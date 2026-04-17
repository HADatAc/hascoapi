package org.hascoapi.ingestion;

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

        Map<String, String> mapCatalog = loadCatalog(dataFile, "SSD");
        if (mapCatalog == null) {
            // loadCatalog already registered the specific DSG error.
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
        if (!validateScopeConsistency(dataFile, mapCatalog, mapContent)) {
            // Replace plain log with dictionary-based exception for abort
            dataFile.getLogger().printExceptionById("DSG_00022");
            return null;
        }

        SSDGeneratorChain chain = new SSDGeneratorChain();
        chain.setDataFile(dataFile);
        chain.setNamedGraphUri(dataFile.getUri());

        if (!validateSSDStructure(dataFile, ssdRecordFile, studyUri, chain, namespace, mapCatalog)) {
            return null;
        }

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
            DataFile vcDataFile = (DataFile) dataFile.clone();
            vcDataFile.setRecordFile(ssdRecordFile); // Use same SSD RecordFile
            VirtualColumnGenerator vcgen = new VirtualColumnGenerator(vcDataFile);
            vcgen.setStudyUri(studyUri);
            chain.addGenerator(vcgen);
        } catch (Exception e) {
            dataFile.getLogger().printException("SSD Processing: Failed to create VirtualColumnGenerator: " + e.getMessage());
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
                if (domainSheetName == null || domainSheetName.trim().isEmpty()) {
                    dataFile.getLogger().printExceptionByIdWithArgs("GBL_00006", "hasScope", "DSG");
                    return false;
                }
                SpreadsheetRecordFile ref = new SpreadsheetRecordFile(
                        dataFile.getFile(), dataFile.getFilename(), domainSheetName.replace("#", ""));
                if (ref == null || !ref.isValid() || ref.getRecords() == null) {
                    dataFile.getLogger().printExceptionByIdWithArgs("DSG_00019", "referenced scope sheet '" + domainSheetName + "'");
                    return false;
                }
                for (Record rr : ref.getRecords()) {
                    String oid = rr.getValueByColumnName("originalID");
                    if (oid != null && !oid.trim().isEmpty()) domainOriginals.add(oid.trim());
                }
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
                        dataFile.getLogger().println("SSD scope validation: SOC sheet '" + cleanSheet + "' row originalID='" + originalId + "' is missing scopeID while hasScope is set in SSD.");
                        return false;
                    }
                    if (!domainOriginals.contains(scopeId.trim())) {
                        dataFile.getLogger().println("SSD scope validation: SOC sheet '" + cleanSheet + "' row originalID='" + originalId + "' has scopeID='" + scopeId + "' not found in referenced SOC originalIDs.");
                        return false;
                    }
                }
                if (requiresTime) {
                    String timeScopeId = row.getValueByColumnName("timeScopeID");
                    if (timeScopeId == null || timeScopeId.trim().isEmpty()) {
                        dataFile.getLogger().println("SSD scope validation: SOC sheet '" + cleanSheet + "' row originalID='" + originalId + "' is missing timeScopeID while hasTimeScope is set in SSD.");
                        return false;
                    }
                    if (!timeOriginals.contains(timeScopeId.trim())) {
                        dataFile.getLogger().println("SSD scope validation: SOC sheet '" + cleanSheet + "' row originalID='" + originalId + "' has timeScopeID='" + timeScopeId + "' not found in referenced SOC originalIDs.");
                        return false;
                    }
                }
                if (requiresSpace) {
                    String spaceScopeId = row.getValueByColumnName("spaceScopeID");
                    if (spaceScopeId == null || spaceScopeId.trim().isEmpty()) {
                        // Replace plain log with dictionary-based exception the user requested
                        dataFile.getLogger().printExceptionByIdWithArgs("DSG_00023", cleanSheet, originalId);
                        return false;
                    }
                    if (!spaceOriginals.contains(spaceScopeId.trim())) {
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

            // Restore the original RecordFile
            dataFile.setRecordFile(originalRecordFile);

        } catch (Exception e) {
            System.out.println("[ERROR] Exception preprocessing SOC key=" + key + ": " + e.getMessage());
            e.printStackTrace();
            // Defensive: don't crash the whole ingestion because one SOC couldn't be preprocessed
            dataFile.getLogger().printExceptionByIdWithArgs("DSG_00016", "Exception preprocessing SOC key=" + key + ": " + e.getMessage());
        }
    }
}
