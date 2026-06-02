package org.hascoapi.ingestion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.SSDSheet;
import org.hascoapi.entity.pojo.Study;
import org.hascoapi.utils.URIUtils;

public class AnnotateSSD extends BaseAnnotator {

    /**
     * Processes an SSD (Semantic Study Design) file and produces a generator chain.
     */
    public static GeneratorChain exec(DataFile dataFile, String studyUri, String templateFile, String status) {
        dataFile.getLogger().println("Processing DSG's SSD meta-template ...");
        System.out.println("[ANNOTATESSD] exec() called for file: " + dataFile.getFilename());

        Map<String, String> mapCatalog = loadCatalog(dataFile, "SSD");
        System.out.println("[ANNOTATESSD] loadCatalog result: " + (mapCatalog == null ? "NULL" : mapCatalog.size() + " entries"));
        if (mapCatalog == null) {
            // loadCatalog already registered the specific DSG error.
            System.out.println("[ANNOTATESSD] Returning null - loadCatalog failed");
            return null;
        }

        String namespace = mapCatalog.get("hasStudyKG");
        dataFile.getLogger().println("AnnotateSSD: namespace value is [" + namespace + "]");

        RecordFile ssdRecordFile = extractSSDRecordFile(dataFile, mapCatalog.get("hasEntityDesign"));
        if (ssdRecordFile == null || ssdRecordFile.getRecords().isEmpty()) {
            // SSD sheet empty / invalid -> use DSG warning
            dataFile.getLogger().printWarningById("DSG_00013");
            SSDGeneratorChain emptyChain = new SSDGeneratorChain();
            emptyChain.setDataFile(dataFile);
            return emptyChain; // Return empty chain to allow fallback
        }

        dataFile.setRecordFile(ssdRecordFile);
        SSDSheet ssd = new SSDSheet(dataFile);
        mapCatalog = ssd.getCatalog();
        Map<String, List<String>> mapContent = ssd.getMapContent();
        Map<String, String> mapReferences = ssd.getMapReferences();

        // New validation: if an SSD row declares scopes, enforce that the SOC sheet rows
        // have corresponding scope IDs pointing to originalIDs in the referenced SOC sheet(s)
        System.out.println("[ANNOTATESSD] Calling validateScopeConsistency...");
        if (!validateScopeConsistency(dataFile, mapCatalog, mapContent)) {
            // Replace plain log with dictionary-based exception for abort
            dataFile.getLogger().printExceptionById("DSG_00022");
            System.out.println("[ANNOTATESSD] Returning null - validateScopeConsistency failed");
            return null;
        }
        System.out.println("[ANNOTATESSD] validateScopeConsistency passed");

        SSDGeneratorChain chain = new SSDGeneratorChain();
        chain.setDataFile(dataFile);
        chain.setNamedGraphUri(dataFile.getUri());

        System.out.println("[ANNOTATESSD] Calling validateSSDStructure...");
        if (!validateSSDStructure(dataFile, ssdRecordFile, studyUri, chain, namespace, mapCatalog)) {
            System.out.println("[ANNOTATESSD] Returning null - validateSSDStructure failed");
            return null;
        }
        System.out.println("[ANNOTATESSD] validateSSDStructure passed");

        // Try to find the study with retry logic to allow triplestore to sync
        // Expand CURIE to full URI if necessary
        String expandedStudyUri = studyUri;
        if (studyUri != null && !studyUri.startsWith("http://") && !studyUri.startsWith("https://")) {
            expandedStudyUri = URIUtils.replacePrefixEx(studyUri);
            System.out.println("AnnotateSSD: Expanded studyUri from [" + studyUri + "] to [" + expandedStudyUri + "]");
        }
        
        Study study = null;
        int maxRetries = 5;
        int retryDelay = 500; // milliseconds

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            study = Study.find(expandedStudyUri);
            if (study != null) {
                System.out.println("AnnotateSSD: ✅ Study found on attempt " + (attempt + 1) + " (URI: " + expandedStudyUri + ")");
                break;
            }

            if (attempt < maxRetries - 1) {
                System.out.println("AnnotateSSD: Study not found on attempt " + (attempt + 1) +
                                 ", retrying in " + retryDelay + "ms... (Searching for: " + expandedStudyUri + ")");
                try {
                    Thread.sleep(retryDelay);
                    retryDelay *= 2; // Exponential backoff
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        if (study == null) {
            // Use DSG error for study not found (argument = studyUri)
            System.out.println("AnnotateSSD: ❌ Study not found after " + maxRetries +
                             " attempts. Searched for URI: " + expandedStudyUri);
            System.out.println("AnnotateSSD: Original URI provided: " + studyUri);
            dataFile.getLogger().printExceptionByIdWithArgs("DSG_00010", expandedStudyUri);
            return null;
        }

        chain.setStudyUri(URIUtils.replacePrefixEx(studyUri));
        dataFile.getLogger().println("DSG ingestion: The study URI [" + studyUri + "] is in the triple store.");
        dataFile.getLogger().println("AnnotateSSD: Pre-processing StudyObjectGenerator. Study URI: " + study.getUri());

        dataFile.getLogger().println("AnnotateSSD: Catalog size: " + mapCatalog.size());
        try {
            for (String sheetKey : mapCatalog.keySet()) {
                System.out.println("AnnotateSSD: Processing sheet key: " + sheetKey);
                addStudyObjectGenerator(sheetKey, mapCatalog, mapContent, mapReferences, dataFile, chain, study, namespace);
                System.out.println("AnnotateSSD: Completed processing sheet key: " + sheetKey);
            }
        } catch (Exception e) {
            System.out.println("AnnotateSSD: ERROR during StudyObjectGenerator preprocessing");
            e.printStackTrace();
            dataFile.getLogger().println("ERROR during SSD processing: " + e.getMessage());
            return null;
        }

        dataFile.getLogger().println("SSD Processing: Completed GeneratorChain.");
        return chain;
    }

    private static RecordFile extractSSDRecordFile(DataFile dataFile, String sheetNameRaw) {
        if (!dataFile.getFilename().endsWith(".xlsx")) {
            // Use DSG exception with filename as argument
            dataFile.getLogger().printExceptionByIdWithArgs("DSG_00006", dataFile.getFilename());
            return null;
        }

        if (sheetNameRaw == null || sheetNameRaw.isEmpty()) {
            // Missing sheet name for hasEntityDesign
            dataFile.getLogger().printExceptionById("DSG_00007");
            return null;
        }

        try {
            String sheetName = sheetNameRaw.replace("#", "");
            dataFile.getLogger().println("Extracting SSD sheet...");
            RecordFile ssdRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), sheetName);

            if (ssdRecordFile == null || ssdRecordFile.getRecords() == null) {
                // Failed to load sheet
                dataFile.getLogger().printExceptionById("DSG_00008");
                return null;
            }

            dataFile.getLogger().println("[" + ssdRecordFile.getRecords().size() + "] rows extracted.");
            return ssdRecordFile;

        } catch (Exception e) {
            // Exception during extraction -> include message as argument
            dataFile.getLogger().printExceptionByIdWithArgs("DSG_00009", e.getMessage());
            return null;
        }
    }

    /*
     *  Verifies if the SSD contains exactly one SOC that is of type SubjectGroup.
     *  Creates VirtualColumnGenerator to process Virtual Columns from SSD sheet.
     */
    private static boolean validateSSDStructure(DataFile dataFile, RecordFile ssdRecordFile, String studyUri, SSDGeneratorChain chain, String namespace, Map<String, String> mapCatalog) {
        if (!ssdRecordFile.isValid()) {
            dataFile.getLogger().printExceptionById("DSG_00014");
            return false;
        }

        // Add VirtualColumnGenerator to process VCs from the SSD sheet
        // VirtualColumnGenerator uses the same SSD sheet as SSDGenerator
        // It looks for rows with typeUri and hasSOCReference to create VirtualColumn entities
        try {
            dataFile.getLogger().println("SSD Processing: Adding VirtualColumnGenerator to process Virtual Columns from SSD sheet.");
            System.out.println("[AnnotateSSD DEBUG] Before dataFile.clone()");
            DataFile vcDataFile = null;
            try {
                vcDataFile = (DataFile) dataFile.clone();
            } catch (CloneNotSupportedException e) {
                System.out.println("[AnnotateSSD WARN] Clone not supported, creating new DataFile instance");
            }
            if (vcDataFile == null) {
                // Fallback: create a lightweight copy manually
                vcDataFile = new DataFile();
                vcDataFile.setId(dataFile.getId());
                vcDataFile.setFilename(dataFile.getFilename());
                vcDataFile.setLogger(dataFile.getLogger());
                vcDataFile.setHasSIRManagerEmail(dataFile.getHasSIRManagerEmail());
                System.out.println("[AnnotateSSD DEBUG] Created manual DataFile copy");
            }
            System.out.println("[AnnotateSSD DEBUG] After dataFile.clone/create, vcDataFile=" + (vcDataFile == null ? "null" : "valid"));
            vcDataFile.setRecordFile(ssdRecordFile); // Use same SSD RecordFile
            System.out.println("[AnnotateSSD DEBUG] After setRecordFile");
            VirtualColumnGenerator vcgen = new VirtualColumnGenerator(vcDataFile);
            System.out.println("[AnnotateSSD DEBUG] After VirtualColumnGenerator creation, studyUri=" + studyUri);
            vcgen.setStudyUri(studyUri);
            System.out.println("[AnnotateSSD DEBUG] After vcgen.setStudyUri");
            chain.addGenerator(vcgen);
            System.out.println("[AnnotateSSD DEBUG] VirtualColumnGenerator added successfully");
        } catch (Exception e) {
            dataFile.getLogger().printException("SSD Processing: Failed to create VirtualColumnGenerator: " + e.getMessage());
            System.out.println("[AnnotateSSD ERROR] Exception in VirtualColumnGenerator setup: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
        }

        dataFile.getLogger().println("SSD Processing: Adding SSDGenerator.");
        SSDGenerator socgen = new SSDGenerator(dataFile, namespace);
        socgen.setStudyUri(studyUri);
        chain.addGenerator(socgen);

        int subjectGroupCount = 0;
        for (Record record : ssdRecordFile.getRecords()) {
            String socType = record.getValueByColumnIndex(2);
            if (socType != null && socType.contains("SubjectGroup")) {
                subjectGroupCount++;
            }
        }

        if (subjectGroupCount == 0) {
            // Use DSG error for missing SubjectGroup
            dataFile.getLogger().printExceptionById("DSG_00011");
            return false;
        }

        if (subjectGroupCount > 1) {
            // Use DSG error for multiple SubjectGroups
            dataFile.getLogger().printExceptionById("DSG_00012");
            return false;
        }

        return true;
    }

    private static boolean validateScopeConsistency(
            DataFile dataFile,
            Map<String, String> mapCatalog,
            Map<String, List<String>> mapContent
    ) {
        // mapCatalog: hasURI -> sheetName
        // mapContent: hasURI -> [0:hasURI,1:type,2:hasScope,3:hasTimeScope,4:hasSpaceScope,5:role,6:hasSOCReference,7:grounding]
        for (Map.Entry<String, List<String>> entry : mapContent.entrySet()) {
            String socHasUri = entry.getKey();
            List<String> list = entry.getValue();
            if (list == null || list.size() < 5) continue;

            String sheetName = mapCatalog.get(socHasUri);
            if (sheetName == null || sheetName.trim().isEmpty()) continue; // no sheet to validate
            String cleanSheet = sheetName.replace("#", "");

            // Identify referenced scope SOCs by hasURI
            String hasScopeHasUri = list.get(2) == null ? "" : list.get(2).trim();
            String hasTimeHasUri = list.get(3) == null ? "" : list.get(3).trim();
            String hasSpaceHasUri = list.get(4) == null ? "" : list.get(4).trim();

            // If no scopes declared in SSD for this SOC, skip validation for this sheet
            boolean requiresDomain = !hasScopeHasUri.isEmpty();
            boolean requiresTime = !hasTimeHasUri.isEmpty();
            boolean requiresSpace = !hasSpaceHasUri.isEmpty();
            if (!requiresDomain && !requiresTime && !requiresSpace) {
                continue;
            }

            // Load current SOC sheet
            SpreadsheetRecordFile socSheet = new SpreadsheetRecordFile(
                    dataFile.getFile(), dataFile.getFilename(), cleanSheet);
            if (socSheet == null || !socSheet.isValid() || socSheet.getRecords() == null) {
                dataFile.getLogger().printExceptionByIdWithArgs("DSG_00019", "SOC sheet '" + cleanSheet + "'");
                return false;
            }

            // Build originalID lookups for referenced SOCs
            Set<String> domainOriginals = new HashSet<>();
            Set<String> timeOriginals = new HashSet<>();
            Set<String> spaceOriginals = new HashSet<>();

            if (requiresDomain) {
                String domainSheetName = mapCatalog.get(hasScopeHasUri);
                System.out.println("[VALIDATE-SCOPE] SOC=" + cleanSheet + " has hasScope=" + hasScopeHasUri + ", domainSheetName=" + domainSheetName);
                if (domainSheetName == null || domainSheetName.trim().isEmpty()) {
                    dataFile.getLogger().printExceptionByIdWithArgs("GBL_00006", "hasScope", "DSG");
                    System.out.println("[VALIDATE-SCOPE] FAIL: domainSheetName is null/empty");
                    return false;
                }
                SpreadsheetRecordFile ref = new SpreadsheetRecordFile(
                        dataFile.getFile(), dataFile.getFilename(), domainSheetName.replace("#", ""));
                if (ref == null || !ref.isValid() || ref.getRecords() == null) {
                    dataFile.getLogger().printExceptionByIdWithArgs("DSG_00019", "referenced scope sheet '" + domainSheetName + "'");
                    System.out.println("[VALIDATE-SCOPE] FAIL: ref sheet invalid or no records");
                    return false;
                }
                for (Record rr : ref.getRecords()) {
                    String oid = rr.getValueByColumnName("originalID");
                    if (oid != null && !oid.trim().isEmpty()) domainOriginals.add(oid.trim());
                }
                System.out.println("[VALIDATE-SCOPE] Collected " + domainOriginals.size() + " originalIDs from scope sheet " + domainSheetName);
            }
            if (requiresTime) {
                String timeSheetName = mapCatalog.get(hasTimeHasUri);
                if (timeSheetName == null || timeSheetName.trim().isEmpty()) {
                    dataFile.getLogger().printExceptionByIdWithArgs("GBL_00006", "hasTimeScope", "DSG");
                    return false;
                }
                SpreadsheetRecordFile ref = new SpreadsheetRecordFile(
                        dataFile.getFile(), dataFile.getFilename(), timeSheetName.replace("#", ""));
                if (ref == null || !ref.isValid() || ref.getRecords() == null) {
                    dataFile.getLogger().printExceptionByIdWithArgs("DSG_00019", "referenced time scope sheet '" + timeSheetName + "'");
                    return false;
                }
                for (Record rr : ref.getRecords()) {
                    String oid = rr.getValueByColumnName("originalID");
                    if (oid != null && !oid.trim().isEmpty()) timeOriginals.add(oid.trim());
                }
            }
            if (requiresSpace) {
                String spaceSheetName = mapCatalog.get(hasSpaceHasUri);
                if (spaceSheetName == null || spaceSheetName.trim().isEmpty()) {
                    dataFile.getLogger().printExceptionByIdWithArgs("GBL_00006", "hasSpaceScope", "DSG");
                    return false;
                }
                SpreadsheetRecordFile ref = new SpreadsheetRecordFile(
                        dataFile.getFile(), dataFile.getFilename(), spaceSheetName.replace("#", ""));
                if (ref == null || !ref.isValid() || ref.getRecords() == null) {
                    dataFile.getLogger().printExceptionByIdWithArgs("DSG_00019", "referenced space scope sheet '" + spaceSheetName + "'");
                    return false;
                }
                for (Record rr : ref.getRecords()) {
                    String oid = rr.getValueByColumnName("originalID");
                    if (oid != null && !oid.trim().isEmpty()) spaceOriginals.add(oid.trim());
                }
            }

            // Validate each row in current SOC sheet
            int checkedRows = 0;
            for (Record row : socSheet.getRecords()) {
                String originalId = row.getValueByColumnName("originalID");
                if (originalId == null || originalId.trim().isEmpty()) {
                    // skip blank rows
                    continue;
                }
                if (requiresDomain) {
                    String scopeId = row.getValueByColumnName("scopeID");
                    if (scopeId == null || scopeId.trim().isEmpty()) {
                        System.out.println("[VALIDATE-SCOPE] WARNING: row " + originalId + " has no scopeID but hasScope is set in SSD - allowing anyway");
                        dataFile.getLogger().println("WARNING: SOC sheet '" + cleanSheet + "' row originalID='" + originalId + "' is missing scopeID while hasScope is set in SSD - this may indicate incomplete data.");
                        // Continue instead of failing - allow ingest with warnings
                        continue; // Skip to next row
                    }
                    // Allow URIs/CURIEs (contain ':' or '/' or start with 'http') without validation
                    boolean isUriOrCurie = scopeId.contains(":") || scopeId.contains("/") || scopeId.startsWith("http");
                    if (!isUriOrCurie && !domainOriginals.contains(scopeId.trim())) {
                        System.out.println("[VALIDATE-SCOPE] WARNING: row " + originalId + " scopeID=" + scopeId + " NOT in domainOriginals (size=" + domainOriginals.size() + ") - allowing anyway");
                        dataFile.getLogger().println("WARNING: SOC sheet '" + cleanSheet + "' row originalID='" + originalId + "' has scopeID='" + scopeId + "' not found in referenced SOC originalIDs - this may indicate invalid data.");
                        // Continue instead of failing - allow ingest with warnings
                    }
                    if (isUriOrCurie) {
                        System.out.println("[VALIDATE-SCOPE] Accepting URI/CURIE scope: " + scopeId);
                    }
                }
                if (requiresTime) {
                    String timeScopeId = row.getValueByColumnName("timeScopeID");
                    if (timeScopeId == null || timeScopeId.trim().isEmpty()) {
                        dataFile.getLogger().println("WARNING: SOC sheet '" + cleanSheet + "' row originalID='" + originalId + "' is missing timeScopeID while hasTimeScope is set in SSD - this may indicate incomplete data.");
                        continue; // Skip to next row
                    }
                    boolean isUriOrCurie = timeScopeId.contains(":") || timeScopeId.contains("/") || timeScopeId.startsWith("http");
                    if (!isUriOrCurie && !timeOriginals.contains(timeScopeId.trim())) {
                        dataFile.getLogger().println("WARNING: SOC sheet '" + cleanSheet + "' row originalID='" + originalId + "' has timeScopeID='" + timeScopeId + "' not found in referenced SOC originalIDs.");
                        // Continue with warning
                    }
                }
                if (requiresSpace) {
                    String spaceScopeId = row.getValueByColumnName("spaceScopeID");
                    if (spaceScopeId == null || spaceScopeId.trim().isEmpty()) {
                        dataFile.getLogger().printExceptionByIdWithArgs("DSG_00023", cleanSheet, originalId);
                        dataFile.getLogger().println("WARNING: Allowing missing spaceScopeID - continuing ingest with warnings.");
                        continue; // Skip to next row
                    }
                    boolean isUriOrCurie = spaceScopeId.contains(":") || spaceScopeId.contains("/") || spaceScopeId.startsWith("http");
                    if (!isUriOrCurie && !spaceOriginals.contains(spaceScopeId.trim())) {
                        dataFile.getLogger().printExceptionByIdWithArgs("DSG_00019", "spaceScopeID '" + spaceScopeId + "' not found in referenced SOC originalIDs");
                        return false;
                    }
                }
                // Successful validation for this row
                checkedRows++;
            }
            // Log successful validation summary for this SOC sheet
            dataFile.getLogger().println(
                "SSD scope validation: SOC sheet '" + cleanSheet + "' validated OK. Checked " + checkedRows + " row(s). " +
                (requiresDomain ? "Domain scope OK. " : "") +
                (requiresTime ? "Time scope OK. " : "") +
                (requiresSpace ? "Space scope OK." : "")
            );
        }
        return true;
    }

    private static void addStudyObjectGenerator(
            String key,
            Map<String, String> catalog,
            Map<String, List<String>> content,
            Map<String, String> references,
            DataFile dataFile,
            SSDGeneratorChain chain,
            Study study,
            String namespace
    ) {
        if (key == null || key.trim().isEmpty()) {
            return;
        }
        // These catalog keys are configuration, not SOC sheet keys
        if ("hasStudyKG".equals(key) || "hasEntityDesign".equals(key) || "hasDependencies".equals(key)) {
            return;
        }
        if (catalog == null || dataFile == null || chain == null || study == null) {
            return;
        }
        if (namespace == null || namespace.trim().isEmpty()) {
            dataFile.getLogger().printExceptionByIdWithArgs("DSG_00016", "Missing namespace (hasStudyKG) for key=" + key);
            return;
        }

        String sheetName = catalog.get(key);
        if (sheetName == null || sheetName.isEmpty()) {
            return;
        }

        // Only process SOC sheets in this phase (these are the Study Object Collection sheets)
        // SheetName may have a # prefix (e.g., "#SOC-LOCATION"), so check without the prefix
        String cleanSheetName = sheetName.startsWith("#") ? sheetName.substring(1) : sheetName;
        if (!cleanSheetName.startsWith("SOC-")) {
            return;
        }

        try {
            System.out.println("Pre-processing SOC [" + cleanSheetName + "]");
            dataFile.getLogger().println("Pre-processing SOC [" + sheetName + "]");

            // SpreadsheetRecordFile expects (file, filename, sheetName)
            RecordFile sheet = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), sheetName.replace("#", ""));

            // Store the current RecordFile temporarily
            RecordFile originalRecordFile = dataFile.getRecordFile();

            // Temporarily set the SOC sheet as the current RecordFile
            dataFile.setRecordFile(sheet);

            if (content == null) {
                dataFile.getLogger().printExceptionByIdWithArgs("DSG_00016", "SSD content map is null while processing key=" + key);
                dataFile.setRecordFile(originalRecordFile); // Restore
                return;
            }

            List<String> headers = content.get(key);
            if (headers == null) {
                // Some spreadsheets may key content by sheetName rather than by hasURI; try that as fallback.
                headers = content.get(sheetName);
            }
            if (headers == null) {
                dataFile.getLogger().printExceptionByIdWithArgs("DSG_00015", key);
                dataFile.setRecordFile(originalRecordFile); // Restore
                return;
            }

            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // 🔍 DETECT SIR WORKSHEETS: Check if this is a SIR object collection
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // SIR worksheets contain VSTOI objects (Instrument, Component, etc.)
            // They need to use SIRObjectGenerator instead of StudyObjectGenerator
            
            // Get actual SOC sheet headers to check for scope columns
            List<String> socSheetHeaders = new ArrayList<>();
            if (sheet != null && sheet.isValid()) {
                socSheetHeaders = sheet.getHeaders();
            }
            System.out.println("[SIR-DETECT DEBUG] Sheet " + cleanSheetName + " actual headers: " + socSheetHeaders);
            boolean isSIRWorksheet = detectSIRWorksheet(cleanSheetName, socSheetHeaders);
            
            if (isSIRWorksheet) {
                // For SIR worksheets, use SIRObjectGenerator to create pure SIR entities
                System.out.println("[SIR-DETECT] Detected SIR worksheet: " + cleanSheetName);
                dataFile.getLogger().println("[SIR-DETECT] Detected SIR worksheet - using SIRObjectGenerator: " + cleanSheetName);
                
                // Create a clone of the DataFile for this generator
                DataFile sirDataFile = null;
                try {
                    sirDataFile = (DataFile) dataFile.clone();
                } catch (CloneNotSupportedException e) {
                    System.out.println("[AnnotateSSD WARN] Clone not supported for SIRObjectGenerator, creating new instance");
                }
                if (sirDataFile == null) {
                    sirDataFile = new DataFile();
                    sirDataFile.setId(dataFile.getId());
                    sirDataFile.setFilename(dataFile.getFilename());
                    sirDataFile.setLogger(dataFile.getLogger());
                    sirDataFile.setHasSIRManagerEmail(dataFile.getHasSIRManagerEmail());
                }
                sirDataFile.setRecordFile(sheet);
                
                // Add SIRObjectGenerator instead of StudyObjectGenerator
                String socUri = org.hascoapi.utils.URIUtils.replacePrefixEx(key);
                chain.addGenerator(new SIRObjectGenerator(sirDataFile, namespace, socUri));
                
                dataFile.getLogger().println("✅ Added SIRObjectGenerator for " + cleanSheetName);
            } else {
                // For regular SOC worksheets, use StudyObjectGenerator
                System.out.println("Adding StudyObjectGenerator for SOC [" + cleanSheetName + "]...");
                dataFile.getLogger().println("Adding StudyObjectGenerator...");
                chain.addGenerator(new StudyObjectGenerator(
                        dataFile,
                        headers,
                        content,
                        references,
                        chain.getStudyUri(),
                        study.getId(),
                        namespace));
            }

            // Restore the original RecordFile
            dataFile.setRecordFile(originalRecordFile);

        } catch (Exception e) {
            System.out.println("[ERROR] Exception preprocessing SOC key=" + key + ": " + e.getMessage());
            e.printStackTrace();
            // Defensive: don't crash the whole ingestion because one SOC couldn't be preprocessed
            dataFile.getLogger().printExceptionByIdWithArgs("DSG_00016", "Exception preprocessing SOC key=" + key + ": " + e.getMessage());
        }
    }

    /**
     * Detecta se um worksheet SOC contém objetos SIR (VSTOI)
     * Checa o nome do worksheet e os headers para identificar tipos SIR
     */
    private static boolean detectSIRWorksheet(String cleanSheetName, List<String> headers) {
        // SIR worksheets are ONLY for pure SIR entities WITHOUT study context or scopes.
        // If the sheet has scopeID, timeScopeID, or spaceScopeID columns, it's a StudyObject collection
        // and should use StudyObjectGenerator, NOT SIRObjectGenerator.
        
        System.out.println("[SIR-DETECT DEBUG] detectSIRWorksheet called for sheet: " + cleanSheetName);
        System.out.println("[SIR-DETECT DEBUG] Headers received: " + headers);
        System.out.println("[SIR-DETECT DEBUG] Headers is null? " + (headers == null));
        System.out.println("[SIR-DETECT DEBUG] Headers size: " + (headers == null ? "N/A" : headers.size()));
        
        // Check if headers contain scope columns - if yes, it's NOT a SIR worksheet
        if (headers != null && !headers.isEmpty()) {
            for (int i = 0; i < headers.size(); i++) {
                String header = headers.get(i);
                System.out.println("[SIR-DETECT DEBUG] Checking header[" + i + "]: '" + header + "'");
                if (header != null) {
                    String lowerHeader = header.toLowerCase().trim();
                    System.out.println("[SIR-DETECT DEBUG]   Normalized to: '" + lowerHeader + "'");
                    if (lowerHeader.equals("scopeid") || 
                        lowerHeader.equals("timescopeid") || 
                        lowerHeader.equals("spacescopeid")) {
                        System.out.println("[SIR-DETECT] Sheet " + cleanSheetName + " has scope column '" + header + "' - NOT a SIR worksheet");
                        return false; // Has scopes, so it's a StudyObject collection
                    }
                }
            }
        }
        
        System.out.println("[SIR-DETECT DEBUG] No scope columns found, proceeding to name/type checks");
        
        // Now check if the sheet name or type suggests SIR objects
        // Normalize sheet names to catch both compact and hyphen/space-separated variants.
        String upperSheetName = cleanSheetName.toUpperCase();
        String normalizedSheetName = upperSheetName.replaceAll("[^A-Z0-9]", "");
        if (normalizedSheetName.contains("INSTRUMENT") ||
            normalizedSheetName.contains("COMPONENT") ||
            normalizedSheetName.contains("RESPONSEOPTION") ||
            normalizedSheetName.contains("CODEBOOK") ||
            normalizedSheetName.contains("SLOTELEMENT") ||
            normalizedSheetName.contains("CONTAINERSLOT") ||
            normalizedSheetName.contains("ANNOTATIONSTEM")) {
            System.out.println("[SIR-DETECT] Sheet " + cleanSheetName + " matches SIR name pattern and has NO scope columns - IS a SIR worksheet");
            return true;
        }
        
        // Check if the type column (headers index 1) contains VSTOI types
        if (headers != null && headers.size() > 1) {
            String typeValue = headers.get(1); // type is typically at index 1
            if (typeValue != null) {
                String lowerType = typeValue.toLowerCase();
                String normalizedType = lowerType.replaceAll("[^a-z0-9]", "");
                if (normalizedType.contains("vstoiinstrument") ||
                    normalizedType.contains("vstoicomponent") ||
                    normalizedType.contains("vstoiresponseoption") ||
                    normalizedType.contains("vstoicodebook") ||
                    normalizedType.contains("vstoicontainerslot") ||
                    normalizedType.contains("vstoiannotationstem")) {
                    System.out.println("[SIR-DETECT] Sheet " + cleanSheetName + " has VSTOI type and NO scope columns - IS a SIR worksheet");
                    return true;
                }
            }
        }
        
        System.out.println("[SIR-DETECT] Sheet " + cleanSheetName + " does NOT match SIR criteria - NOT a SIR worksheet");
        return false;
    }
}
