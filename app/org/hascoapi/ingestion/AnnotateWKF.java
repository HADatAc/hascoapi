package org.hascoapi.ingestion;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.Study;
import org.hascoapi.entity.pojo.StudyObjectCollection;
import org.hascoapi.entity.pojo.StudyRole;
import org.hascoapi.entity.pojo.VirtualColumn;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.utils.Utils;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.VSTOI;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class AnnotateWKF extends BaseAnnotator {

    public static GeneratorChain exec(DataFile dataFile, String templateFile, String status) {
        System.out.println("\n========== AnnotateWKF.exec() START ==========");
        System.out.println("DataFile URI: " + dataFile.getUri());
        System.out.println("DataFile Filename: " + dataFile.getFilename());
        System.out.println("Template File: " + templateFile);
        System.out.println("Status: " + status);

        dataFile.getLogger().addLine("Processing WKF meta-template...");

        // Load catalog with sheet validation
        System.out.println("→ Loading catalog...");
        Map<String, String> mapCatalog = loadCatalog(dataFile, Constants.MT_WKF);

        if (mapCatalog == null) {
            System.err.println("❌ Failed to load catalog - mapCatalog is null");
            dataFile.getLogger().printExceptionById("WKF_00020");
            return null;
        }

        System.out.println("✓ Catalog loaded successfully with " + mapCatalog.size() + " sheets:");
        for (Map.Entry<String, String> entry : mapCatalog.entrySet()) {
            System.out.println("  - Sheet: [" + entry.getKey() + "] → URI: [" + entry.getValue() + "]");
        }

        // MEDIUM PRIORITY FIX: Validate InfoSheet structure
        System.out.println("→ Validating InfoSheet structure...");
        if (!validateInfoSheetStructure(dataFile, mapCatalog)) {
            System.err.println("❌ InfoSheet structure validation failed");
            dataFile.getLogger().printExceptionById("WKF_00001");
            return null;
        }
        System.out.println("✓ InfoSheet structure validated successfully");

        // HIGH PRIORITY FIX: Validate STD sheet semantics
        System.out.println("→ Validating STD sheet semantics...");
        if (!validateStdSheetSemantics(dataFile, mapCatalog)) {
            System.err.println("❌ STD sheet semantic validation failed");
            dataFile.getLogger().printException("WKF STD sheet semantic validation failed");
            return null;
        }
        System.out.println("✓ STD sheet semantics validated successfully");

        // HIGH PRIORITY FIX: Validate Namespaces semantics
        System.out.println("→ Validating Namespaces semantics...");
        if (!validateNamespaceSheetSemantics(dataFile, mapCatalog)) {
            System.err.println("❌ Namespaces semantic validation failed");
            dataFile.getLogger().printException("WKF Namespaces semantic validation failed");
            return null;
        }
        System.out.println("✓ Namespaces semantics validated successfully");

        // Generate namespace and messages
        System.out.println("→ Generating namespaces...");
        boolean okNS = IngestionWorker.nameSpaceGen(dataFile, mapCatalog, templateFile, Constants.MT_WKF);
        System.out.println("→ Generating messages...");
        boolean okMsg = IngestionWorker.messageGen(dataFile, mapCatalog, templateFile, status);

        if (!okNS) {
            System.err.println("❌ Namespace generation failed");
            dataFile.getLogger().printExceptionById("WKF_00005");
            return null;
        }

        if (!okMsg) {
            System.err.println("❌ Message generation failed");
            dataFile.getLogger().printExceptionById("WKF_00006");
            return null;
        }

        System.out.println("✓ Namespaces and messages generated successfully");
        dataFile.getLogger().addLine("WKF: Namespaces and messages generated successfully");

        // Build the generator chain for WKF sheets
        System.out.println("→ Building generator chain...");
        GeneratorChain chain = new GeneratorChain();
        int generatorCount = 0;

        for (String sheet : mapCatalog.keySet()) {
            System.out.println("  Processing sheet: [" + sheet + "]");

            if ("InfoSheet".equalsIgnoreCase(sheet) ||
                "Namespaces".equalsIgnoreCase(sheet) ||
                "hasStudyDescription".equalsIgnoreCase(sheet) ||
                "hasDependencies".equalsIgnoreCase(sheet) ||
                "hasVersion".equalsIgnoreCase(sheet)) {
                System.out.println("    → Skipping metadata sheet");
                continue; // Skip metadata sheets
            }

            if ("ProcessStems".equalsIgnoreCase(sheet)) {
                System.out.println("    → Adding ProcessStem generator");
                addCustomGeneratorIfSheetExists(dataFile, mapCatalog, sheet, status, chain,
                        (df, st) -> new WKFGenerator("processstem", df, st));
                generatorCount++;

            } else if ("Processes".equalsIgnoreCase(sheet)) {
                System.out.println("    → Adding Process generator");
                addCustomGeneratorIfSheetExists(dataFile, mapCatalog, sheet, status, chain,
                        (df, st) -> new WKFGenerator("process", df, st));
                generatorCount++;

            } else if ("Tasks".equalsIgnoreCase(sheet)) {
                System.out.println("    → Adding Task generator");
                addCustomGeneratorIfSheetExists(dataFile, mapCatalog, sheet, status, chain,
                        (df, st) -> new WKFGenerator("task", df, st));
                generatorCount++;

            } else {
                // Unknown sheet - log warning but continue
                System.out.println("    ⚠️ Unknown sheet, logging warning");
                dataFile.getLogger().printWarningByIdWithArgs("WKF_00008", sheet);
            }
        }

        System.out.println("✓ Generator chain built with " + generatorCount + " generators");

        // Set the named graph URI so data is stored in the DataFile's graph
        chain.setNamedGraphUri(dataFile.getUri());
        System.out.println("✓ Named graph URI set to: " + dataFile.getUri());

        // Validate that at least one generator was added
        chain.setDataFile(dataFile);
        if (!chain.isValid()) {
            System.err.println("❌ Generator chain is invalid");
            dataFile.getLogger().printExceptionById("WKF_00007");
            return null;
        }

        System.out.println("✓ WKF: Generator chain validated successfully");

        // HIGH PRIORITY FIX: Strict task typing validation (Tasks columns B and C)
        System.out.println("→ Validating task typing semantics...");
        if (!validateTaskTypingSemantics(dataFile, mapCatalog)) {
            System.err.println("❌ Task typing semantic validation failed");
            dataFile.getLogger().printException("WKF Task typing semantic validation failed");
            return null;
        }
        System.out.println("✓ Task typing semantics validated successfully");

        // CRITICAL PRIORITY FIX: Validate temporal dependencies form a DAG (no cycles)
        System.out.println("→ Validating temporal dependency DAG...");
        if (!validateTemporalDependencyDAG(dataFile, mapCatalog)) {
            System.err.println("❌ Temporal dependency DAG validation failed - circular dependencies detected");
            dataFile.getLogger().printExceptionById("WKF_00004");
            return null;
        }
        System.out.println("✓ Temporal dependencies form a valid DAG (no cycles)");

        // CRITICAL PRIORITY FIX: Validate task hierarchy has no cycles
        System.out.println("→ Validating task hierarchy...");
        if (!validateTaskHierarchy(dataFile, mapCatalog)) {
            System.err.println("❌ Task hierarchy validation failed - circular references detected");
            dataFile.getLogger().printExceptionById("WKF_00003");
            return null;
        }
        System.out.println("✓ Task hierarchy validated successfully (no cycles)");

        // CRITICAL PRIORITY FIX: enforce unique top-level task and Process top-task coherence
        System.out.println("→ Validating top-level task semantics...");
        if (!validateTopLevelTaskSemantics(dataFile, mapCatalog)) {
            System.err.println("❌ Top-level task semantics validation failed");
            dataFile.getLogger().printException("WKF top-level task semantic validation failed");
            return null;
        }
        System.out.println("✓ Top-level task semantics validated successfully");

        // HIGH PRIORITY FIX: Validate reference integrity
        System.out.println("→ Validating reference integrity...");
        if (!validateReferenceIntegrity(dataFile, mapCatalog)) {
            System.err.println("❌ Reference integrity validation failed");
            // Don't return null - just warn, as some references might be to external resources
            dataFile.getLogger().printWarning("WKF reference integrity issues detected - check logs");
        }
        System.out.println("✓ Reference integrity validation completed");

        // EDUCATIONAL ENHANCEMENT: Validate educational properties
        System.out.println("→ Validating educational properties...");
        validateEducationalProperties(dataFile, mapCatalog);
        System.out.println("✓ Educational properties validation completed");

        // EDUCATIONAL ENHANCEMENT: Validate objective consistency
        System.out.println("→ Validating objective consistency...");
        validateObjectiveConsistency(dataFile, mapCatalog);
        System.out.println("✓ Objective consistency validation completed");

        System.out.println("========== AnnotateWKF.exec() END (SUCCESS) ==========\n");
        return chain;
    }

    /**
     * WKF-specific post-processing after successful generator-chain commit.
     * Creates ProcessBasedStudy entities from WKF Process entities and ensures
     * default SOC placeholders expected for workflow-derived studies.
     */
    public static void postProcessAfterIngestion(DataFile dataFile) {
        generateProcessBasedStudies(dataFile);
    }

    /**
     * Generate ProcessBasedStudy entities from Process entities created during WKF ingestion.
     */
    private static void generateProcessBasedStudies(DataFile dataFile) {
        dataFile.getLogger().println("\n========== WKF Post-Processing: Creating ProcessBasedStudy Entities ==========");

        try {
            Map<String, String> stdMetadata = extractStdMetadata(dataFile);

            String strictQuery =
                "PREFIX hasco: <http://hadatac.org/ont/hasco/> " +
                "PREFIX vstoi: <http://hadatac.org/ont/vstoi#> " +
                "SELECT DISTINCT ?processUri WHERE { " +
                "  ?processUri a vstoi:Process . " +
                "  ?processUri hasco:hasDataFile <" + dataFile.getUri() + "> . " +
                "}";

            ResultSetRewindable results = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), strictQuery);

            if (results == null || !results.hasNext()) {
                dataFile.getLogger().println("  No Process found with rdf:type vstoi:Process; trying hasco:hascoType fallback...");

                String fallbackQuery =
                    "PREFIX hasco: <http://hadatac.org/ont/hasco/> " +
                    "PREFIX vstoi: <http://hadatac.org/ont/vstoi#> " +
                    "SELECT DISTINCT ?processUri WHERE { " +
                    "  ?processUri hasco:hascoType vstoi:Process . " +
                    "  ?processUri hasco:hasDataFile <" + dataFile.getUri() + "> . " +
                    "}";

                results = SPARQLUtils.select(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), fallbackQuery);
            }

            if (results == null || !results.hasNext()) {
                dataFile.getLogger().println("  No Process entities found in WKF file - skipping ProcessBasedStudy generation");
                return;
            }

            int processCount = 0;
            int studyCount = 0;

            while (results.hasNext()) {
                QuerySolution solution = results.next();
                String processUri = solution.getResource("processUri").getURI();
                processCount++;

                dataFile.getLogger().println("  Processing: " + processUri);

                try {
                    ProcessBasedStudyGenerator generator = new ProcessBasedStudyGenerator(
                        dataFile,
                        processUri,
                        dataFile.getHasSIRManagerEmail(),
                        null,
                        stdMetadata
                    );

                    if (!generator.validateProcess()) {
                        dataFile.getLogger().println("    Process validation failed, skipping: " + processUri);
                        continue;
                    }

                    Map<String, Object> studyRow = generator.createRowFromProcess();
                    if (studyRow == null) {
                        dataFile.getLogger().println("    Failed to generate study row, skipping: " + processUri);
                        continue;
                    }

                    String studyUri = (String) studyRow.get("hasURI");
                    dataFile.getLogger().println("    Generated ProcessBasedStudy: " + studyUri);

                    generator.setNamedGraphUri(dataFile.getUri());
                    generator.getRows().add(studyRow);
                    generator.createObjects();

                    boolean committed = generator.commitRowsToTripleStore(generator.getRows());
                    if (committed) {
                        dataFile.getLogger().println("    Committed study to triplestore: " + studyUri);
                        ensureDefaultWkfStudyStructures(dataFile, studyUri);
                        studyCount++;
                    } else {
                        dataFile.getLogger().println("    Failed to commit study: " + studyUri);
                    }

                } catch (Exception e) {
                    dataFile.getLogger().println("    Error generating study for Process " + processUri + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

            dataFile.getLogger().println("========== ProcessBasedStudy Generation Complete ==========");
            dataFile.getLogger().println("  Processed " + processCount + " Process entities");
            dataFile.getLogger().println("  Created " + studyCount + " ProcessBasedStudy entities");
            dataFile.getLogger().println("=============================================================\n");

        } catch (Exception e) {
            dataFile.getLogger().println("ERROR in ProcessBasedStudy generation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Ensure workflow-derived studies have a default participant placeholder.
     */
    private static void ensureDefaultWkfStudyStructures(DataFile dataFile, String studyUri) {
        if (studyUri == null || studyUri.trim().isEmpty()) {
            return;
        }

        final String socLabel = "SOC-STUDENTS";

        try {
            Study study = Study.find(studyUri);
            if (study == null) {
                dataFile.getLogger().println("    Could not load Study for default WKF structure creation: " + studyUri);
                return;
            }

            List<StudyObjectCollection> existingSocs = StudyObjectCollection.findStudyObjectCollectionsByStudyFlexible(studyUri);
            if (existingSocs != null) {
                for (StudyObjectCollection existingSoc : existingSocs) {
                    if (existingSoc != null && socLabel.equalsIgnoreCase(existingSoc.getLabel())) {
                        dataFile.getLogger().println("    Default " + socLabel + " already exists for study: " + studyUri);
                        return;
                    }
                }
            }

            String managerEmail = dataFile.getHasSIRManagerEmail() == null ? "" : dataFile.getHasSIRManagerEmail();

            StudyRole studentRole = new StudyRole();
            studentRole.setUri(Utils.uriGen("studyrole"));
            studentRole.setTypeUri(HASCO.STUDY_ROLE);
            studentRole.setHascoTypeUri(HASCO.STUDY_ROLE);
            studentRole.setLabel("Student");
            studentRole.setComment("Default study role auto-created from WKF ingestion.");
            studentRole.setIsMemberOfUri(studyUri);
            studentRole.setHasSIRManagerEmail(managerEmail);
            studentRole.setNamedGraph(dataFile.getUri());
            studentRole.save();

            VirtualColumn studentsVc = VirtualColumn.find(studyUri, socLabel);
            if (studentsVc == null) {
                studentsVc = new VirtualColumn(studyUri, "Student", socLabel, managerEmail);
                studentsVc.setNamedGraph(dataFile.getUri());
                studentsVc.save();
            }

            StudyObjectCollection studentsSoc = new StudyObjectCollection();
            studentsSoc.setUri(Utils.uriGen("studyobjectcollection"));
            studentsSoc.setTypeUri(HASCO.STUDY_OBJECT_COLLECTION);
            studentsSoc.setHascoTypeUri(HASCO.STUDY_OBJECT_COLLECTION);
            studentsSoc.setLabel(socLabel);
            studentsSoc.setComment("Default placeholder collection auto-created from WKF ingestion for student participants.");
            studentsSoc.setIsMemberOfUri(studyUri);
            studentsSoc.setVirtualColumnUri(studentsVc.getUri());
            studentsSoc.setRoleUri(studentRole.getUri());
            studentsSoc.setHasSIRManagerEmail(managerEmail);
            studentsSoc.setNamedGraph(dataFile.getUri());
            studentsSoc.save();

            dataFile.getLogger().println("    Created default " + socLabel + " structure for study: " + studyUri);
        } catch (Exception e) {
            dataFile.getLogger().println("    Failed to create default WKF study structures for " + studyUri + ": " + e.getMessage());
        }
    }

    /**
     * Validate InfoSheet structure according to WKF-SPEC-V3.
     */
    private static boolean validateInfoSheetStructure(DataFile dataFile, Map<String, String> catalog) {
        final String[] expectedKeys = {
            "hasDependencies",
            "hasStudyDescription",
            "ProcessStems",
            "Processes",
            "Tasks",
            "hasVersion"
        };
        final String[] expectedValues = {
            "#Namespaces",
            "#STD",
            "#ProcessStems",
            "#Processes",
            "#Tasks",
            null
        };

        boolean valid = true;
        RecordFile infoSheet = dataFile.getRecordFile();
        if (infoSheet == null || !infoSheet.isValid()) {
            dataFile.getLogger().printException("InfoSheet is missing or invalid");
            return false;
        }

        int rows = infoSheet.getRecords() == null ? 0 : infoSheet.getRecords().size();
        if (rows != 6) {
            System.err.println("[WKF Validation] InfoSheet must have exactly 6 data rows, found " + rows);
            dataFile.getLogger().printException("InfoSheet must have exactly 6 data rows");
            valid = false;
        }

        try (FileInputStream fis = new FileInputStream(dataFile.getFile());
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet info = workbook.getSheet("InfoSheet");
            if (info == null) {
                dataFile.getLogger().printException("InfoSheet sheet not found in workbook");
                return false;
            }

            DataFormatter formatter = new DataFormatter();
            Row header = info.getRow(0);
            String h1 = header == null ? "" : formatter.formatCellValue(header.getCell(0)).trim();
            String h2 = header == null ? "" : formatter.formatCellValue(header.getCell(1)).trim();
            if (!"Attribute".equals(h1) || !"Value".equals(h2)) {
                dataFile.getLogger().printException("InfoSheet header must be exactly 'Attribute | Value'");
                valid = false;
            }

            Set<String> seenKeys = new HashSet<>();
            int maxRows = info.getLastRowNum();
            for (int rowIdx = 1; rowIdx <= maxRows; rowIdx++) {
                Row row = info.getRow(rowIdx);
                if (row == null) {
                    continue;
                }
                String key = formatter.formatCellValue(row.getCell(0)).trim();
                if (key.isEmpty()) {
                    continue;
                }
                String value = formatter.formatCellValue(row.getCell(1)).trim();
                seenKeys.add(key);

                if ("hasDependencies".equals(key) && !"#Namespaces".equals(value)) {
                    dataFile.getLogger().printException("InfoSheet hasDependencies must point to #Namespaces and found '" + value + "'");
                    valid = false;
                }
                if ("ProcessStems".equals(key) && !"#ProcessStems".equals(value)) {
                    dataFile.getLogger().printException("InfoSheet ProcessStems must point to #ProcessStems and found '" + value + "'");
                    valid = false;
                }
                if ("Processes".equals(key) && !"#Processes".equals(value)) {
                    dataFile.getLogger().printException("InfoSheet Processes must point to #Processes and found '" + value + "'");
                    valid = false;
                }
                if ("Tasks".equals(key) && !"#Tasks".equals(value)) {
                    dataFile.getLogger().printException("InfoSheet Tasks must point to #Tasks and found '" + value + "'");
                    valid = false;
                }
                if ("hasStudyDescription".equals(key) && !value.isEmpty() && !"#STD".equals(value)) {
                    dataFile.getLogger().printException("InfoSheet hasStudyDescription must point to #STD when provided and found '" + value + "'");
                    valid = false;
                }
            }

            for (String required : expectedKeys) {
                if (!seenKeys.contains(required)) {
                    dataFile.getLogger().printException("InfoSheet is missing required key '" + required + "'");
                    valid = false;
                }
            }

        } catch (Exception e) {
            dataFile.getLogger().printException("Error validating InfoSheet workbook structure: " + e.getMessage());
            return false;
        }

        String version = safeValue(catalog.get("hasVersion"));
        if (!version.matches("^\\d+(\\.\\d+)*$")) {
            dataFile.getLogger().printException("InfoSheet hasVersion must match ^\\d+(\\.\\d+)*$: " + version);
            valid = false;
        }

        String[] prohibited = {"hasWorkflowID", "label", "comment", "versionNumber"};
        for (String field : prohibited) {
            if (catalog.containsKey(field)) {
                dataFile.getLogger().printException("InfoSheet contains prohibited field: " + field);
                valid = false;
            }
        }

        return valid;
    }

    private static Map<String, String> extractStdMetadata(DataFile dataFile) {
        Map<String, String> stdMetadata = new HashMap<>();

        try {
            Map<String, String> catalog = loadCatalog(dataFile, Constants.MT_WKF);
            if (catalog == null || catalog.isEmpty()) {
                return stdMetadata;
            }

            String stdPointer = catalog.get("hasStudyDescription");
            if (stdPointer == null || stdPointer.trim().isEmpty()) {
                populateStdMetadataFromWkfComment(dataFile, stdMetadata);
                return stdMetadata;
            }

            String stdSheetName = stdPointer.trim().replace("#", "");
            if (stdSheetName.isEmpty()) {
                return stdMetadata;
            }

            try (FileInputStream fis = new FileInputStream(dataFile.getFile());
                 Workbook workbook = new XSSFWorkbook(fis)) {

                Sheet stdSheet = workbook.getSheet(stdSheetName);
                if (stdSheet == null) {
                    populateStdMetadataFromWkfComment(dataFile, stdMetadata);
                    return stdMetadata;
                }

                DataFormatter formatter = new DataFormatter();
                Row headerRow = stdSheet.getRow(1);
                if (headerRow == null) {
                    headerRow = stdSheet.getRow(0);
                }
                Row dataRow = firstNonEmptyDataRow(stdSheet, 2);
                if (dataRow == null) {
                    dataRow = firstNonEmptyDataRow(stdSheet, 1);
                }

                if (headerRow == null || dataRow == null) {
                    populateStdMetadataFromWkfComment(dataFile, stdMetadata);
                    return stdMetadata;
                }

                for (int col = 0; col <= headerRow.getLastCellNum(); col++) {
                    String header = formatter.formatCellValue(headerRow.getCell(col)).trim();
                    if (header.isEmpty()) {
                        continue;
                    }
                    String value = formatter.formatCellValue(dataRow.getCell(col)).trim();
                    if (!value.isEmpty()) {
                        stdMetadata.put(header, value);
                    }
                }

                if (!stdMetadata.containsKey("hasStudyDescription") || stdMetadata.get("hasStudyDescription").trim().isEmpty()) {
                    populateStdMetadataFromWkfComment(dataFile, stdMetadata);
                }
            }

        } catch (Exception e) {
            dataFile.getLogger().printWarning("Unable to extract STD metadata during WKF post-processing: " + e.getMessage());
            populateStdMetadataFromWkfComment(dataFile, stdMetadata);
        }

        return stdMetadata;
    }

    private static void populateStdMetadataFromWkfComment(DataFile dataFile, Map<String, String> stdMetadata) {
        String wkfComment = resolveWkfCommentByDataFile(dataFile);
        if (wkfComment == null || wkfComment.trim().isEmpty()) {
            return;
        }

        String cleaned = wkfComment.trim();
        stdMetadata.putIfAbsent("hasStudyDescription", cleaned);
        if (!stdMetadata.containsKey("Specific Aims") || stdMetadata.get("Specific Aims").trim().isEmpty()) {
            stdMetadata.put("Specific Aims", cleaned);
        }
    }

    private static String resolveWkfCommentByDataFile(DataFile dataFile) {
        if (dataFile == null || dataFile.getUri() == null || dataFile.getUri().trim().isEmpty()) {
            return "";
        }

        try {
            String dfUri = dataFile.getUri().trim();
            String query = NameSpaces.getInstance().printSparqlNameSpaceList()
                + "SELECT DISTINCT ?comment WHERE { "
                + "  ?wkf hasco:hasDataFile <" + dfUri + "> . "
                + "  { ?wkf a hasco:WKF . } UNION { ?wkf hasco:hascoType hasco:WKF . } "
                + "  ?wkf rdfs:comment ?comment . "
                + "} LIMIT 1";

            ResultSetRewindable results = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), query);
            if (results != null && results.hasNext()) {
                QuerySolution sol = results.next();
                if (sol != null && sol.get("comment") != null) {
                    return sol.get("comment").toString();
                }
            }
        } catch (Exception e) {
            dataFile.getLogger().printWarning("WKF comment fallback lookup failed: " + e.getMessage());
        }

        return "";
    }

    /**
     * Strict WKF v1.2.2 validation for STD sheet semantics.
     */
    private static boolean validateStdSheetSemantics(DataFile dataFile, Map<String, String> catalog) {
        String stdPointer = catalog.get("hasStudyDescription");
        if (stdPointer == null || stdPointer.trim().isEmpty()) {
            System.err.println("[WKF Validation] hasStudyDescription is required and must point to #STD");
            dataFile.getLogger().printException("hasStudyDescription is required and must point to #STD");
            return false;
        }

        String stdSheetName = stdPointer.trim().replace("#", "");
        if (stdSheetName.isEmpty()) {
            System.err.println("[WKF Validation] Invalid hasStudyDescription pointer: " + stdPointer);
            dataFile.getLogger().printException("Invalid hasStudyDescription pointer: " + stdPointer);
            return false;
        }

        try (FileInputStream fis = new FileInputStream(dataFile.getFile());
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet stdSheet = workbook.getSheet(stdSheetName);
            if (stdSheet == null) {
                System.err.println("[WKF Validation] STD sheet not found: " + stdSheetName);
                dataFile.getLogger().printException("STD sheet not found: " + stdSheetName);
                return false;
            }

            DataFormatter formatter = new DataFormatter();
            Row headerRow = stdSheet.getRow(1);
            int headerRowIndex = 1;
            if (headerRow == null) {
                System.err.println("[WKF Validation] STD sheet must have effective headers on row 2");
                dataFile.getLogger().printException("STD sheet must have effective headers on row 2");
                return false;
            }

            List<String> requiredHeaders = Arrays.asList(
                "hasURI",
                "hasco:hasProcess",
                "Study ID",
                "Title",
                "Specific Aims",
                "Significance",
                "Institution",
                "Principal Investigator",
                "Email",
                "Start Date",
                "End Date",
                "vstoi:hasLearningObjectives",
                "vstoi:hasCriticalActions",
                "vstoi:hasDebriefingFocus"
            );

            Map<String, Integer> headerIndex = new HashMap<>();
            for (int col = 0; col <= headerRow.getLastCellNum(); col++) {
                String header = formatter.formatCellValue(headerRow.getCell(col)).trim();
                if (!header.isEmpty()) {
                    headerIndex.put(header, col);
                }
            }

            boolean valid = true;
            for (String requiredHeader : requiredHeaders) {
                if (!headerIndex.containsKey(requiredHeader)) {
                    System.err.println("[WKF Validation] STD sheet missing required column: " + requiredHeader);
                    dataFile.getLogger().printException("STD sheet missing required column: " + requiredHeader);
                    valid = false;
                }
            }
            if (!valid) {
                return false;
            }

            List<Row> nonEmptyRows = new ArrayList<>();
            Set<String> stdProcessUris = new HashSet<>();
            for (int rowIndex = Math.max(headerRowIndex + 1, 2); rowIndex <= stdSheet.getLastRowNum(); rowIndex++) {
                Row row = stdSheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                boolean hasAnyValue = false;
                for (String requiredHeader : requiredHeaders) {
                    int col = headerIndex.get(requiredHeader);
                    String value = formatter.formatCellValue(row.getCell(col)).trim();
                    if (!value.isEmpty()) {
                        hasAnyValue = true;
                        break;
                    }
                }
                if (hasAnyValue) {
                    nonEmptyRows.add(row);
                }
            }

            if (nonEmptyRows.isEmpty()) {
                System.err.println("[WKF Validation] STD sheet must have at least one non-empty data row");
                dataFile.getLogger().printException("STD sheet must have at least one non-empty data row");
                return false;
            }

            for (Row stdRow : nonEmptyRows) {
                int stdRowNumber = stdRow.getRowNum() + 1;

                String pbsUri = formatter.formatCellValue(stdRow.getCell(headerIndex.get("hasURI"))).trim();
                if (pbsUri.isEmpty() || !URIUtils.isValidURI(pbsUri)) {
                    dataFile.getLogger().printException("STD row " + stdRowNumber + " must define valid hasURI");
                    valid = false;
                }

                String processUri = formatter.formatCellValue(stdRow.getCell(headerIndex.get("hasco:hasProcess"))).trim();
                if (processUri.isEmpty() || !URIUtils.isValidURI(processUri)) {
                    dataFile.getLogger().printException("STD row " + stdRowNumber + " must define valid hasco:hasProcess");
                    valid = false;
                } else {
                    stdProcessUris.add(processUri);
                }

                String studyId = formatter.formatCellValue(stdRow.getCell(headerIndex.get("Study ID"))).trim();
                if (studyId.isEmpty()) {
                    dataFile.getLogger().printException("STD row " + stdRowNumber + " must define Study ID");
                    valid = false;
                } else if (!studyId.startsWith("STD-")) {
                    dataFile.getLogger().printWarning("STD row " + stdRowNumber + " Study ID should start with STD-: " + studyId);
                }

                String title = formatter.formatCellValue(stdRow.getCell(headerIndex.get("Title"))).trim();
                if (title.isEmpty()) {
                    dataFile.getLogger().printException("STD row " + stdRowNumber + " must define Title");
                    valid = false;
                }

                String institutionValue = formatter.formatCellValue(stdRow.getCell(headerIndex.get("Institution"))).trim();
                if (institutionValue.isEmpty() || !URIUtils.isValidURI(institutionValue)) {
                    dataFile.getLogger().printException("STD row " + stdRowNumber + " must define valid Institution URI");
                    valid = false;
                }

                String piValue = formatter.formatCellValue(stdRow.getCell(headerIndex.get("Principal Investigator"))).trim();
                if (piValue.isEmpty() || !URIUtils.isValidURI(piValue)) {
                    dataFile.getLogger().printException("STD row " + stdRowNumber + " must define valid Principal Investigator URI");
                    valid = false;
                }

                String emailValue = formatter.formatCellValue(stdRow.getCell(headerIndex.get("Email"))).trim();
                if (!emailValue.isEmpty() && !emailValue.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                    dataFile.getLogger().printException("STD row " + stdRowNumber + " has invalid Email format: " + emailValue);
                    valid = false;
                }

                String startDateValue = formatter.formatCellValue(stdRow.getCell(headerIndex.get("Start Date"))).trim();
                if (!startDateValue.isEmpty() && !isValidDateValue(startDateValue)) {
                    dataFile.getLogger().printException("STD row " + stdRowNumber + " has invalid Start Date format: " + startDateValue);
                    valid = false;
                }

                String endDateValue = formatter.formatCellValue(stdRow.getCell(headerIndex.get("End Date"))).trim();
                if (!endDateValue.isEmpty() && !isValidDateValue(endDateValue)) {
                    dataFile.getLogger().printException("STD row " + stdRowNumber + " has invalid End Date format: " + endDateValue);
                    valid = false;
                }
            }

            String processSheetName = catalog.get("Processes");
            if (processSheetName != null && !processSheetName.trim().isEmpty()) {
                Sheet processSheet = workbook.getSheet(processSheetName.replace("#", ""));
                if (processSheet != null) {
                    for (int rowIndex = 1; rowIndex <= processSheet.getLastRowNum(); rowIndex++) {
                        Row processRow = processSheet.getRow(rowIndex);
                        if (processRow == null) {
                            continue;
                        }
                        String processUri = formatter.formatCellValue(processRow.getCell(0)).trim();
                        if (processUri.isEmpty()) {
                            continue;
                        }
                        if (!stdProcessUris.contains(processUri)) {
                            dataFile.getLogger().printException("Process row " + (rowIndex + 1)
                                + " URI is not linked by any STD hasco:hasProcess: " + processUri);
                            valid = false;
                        }
                    }
                }
            }

            return valid;
        } catch (Exception e) {
            System.err.println("[WKF Validation] Error validating STD semantics: " + e.getMessage());
            dataFile.getLogger().printException("Error validating STD semantics: " + e.getMessage());
            return false;
        }
    }

    /**
     * Strict WKF v1.2.1 validation for Tasks typing:
     * - Column C (hasco:hascoType) MUST be vstoi:Task
     * - Column B (rdf:type) MUST be vstoi:Task or subclass of vstoi:Task
     */
    private static boolean validateTaskTypingSemantics(DataFile dataFile, Map<String, String> catalog) {
        String tasksSheet = catalog.get("Tasks");
        if (tasksSheet == null || tasksSheet.trim().isEmpty()) {
            return true;
        }

        try {
            RecordFile tasks = new SpreadsheetRecordFile(dataFile.getFile(), tasksSheet.replace("#", ""));
            if (!tasks.isValid() || tasks.getRecords() == null || tasks.getRecords().isEmpty()) {
                return true;
            }

            boolean valid = true;
            int rowNumber = 2; // Tasks sheet header is expected at row 1
            Map<String, Boolean> taskSubclassCache = new HashMap<>();

            for (Record rec : tasks.getRecords()) {
                String taskUri = safeValue(rec.getValueByColumnName("hasURI"));
                String hascoType = safeValue(rec.getValueByColumnName("hasco:hascoType"));
                String rdfType = safeValue(rec.getValueByColumnName("rdf:type"));

                // Skip structurally empty rows
                if (taskUri.isEmpty() && hascoType.isEmpty() && rdfType.isEmpty()) {
                    rowNumber++;
                    continue;
                }

                boolean hascoTypeIsArchetype = isTaskArchetypeValue(hascoType);
                boolean hascoTypeIsSubclass = !hascoTypeIsArchetype
                    && !hascoType.isEmpty()
                    && isTaskTypeOrSubclass(hascoType, taskSubclassCache, dataFile);

                if (!hascoTypeIsArchetype) {
                    if (hascoTypeIsSubclass) {
                        dataFile.getLogger().printWarning("Tasks row " + rowNumber
                            + " uses legacy hasco:hascoType subclass value '" + hascoType
                            + "'. It will be normalized to vstoi:Task during ingestion.");
                    } else {
                        System.err.println("[WKF Validation] Tasks row " + rowNumber + " has invalid hasco:hascoType (must be vstoi:Task or subclass): " + hascoType);
                        dataFile.getLogger().printException("Tasks row " + rowNumber + " has invalid hasco:hascoType (must be vstoi:Task or subclass): " + hascoType);
                        valid = false;
                    }
                }

                if (rdfType.isEmpty()) {
                    if (hascoTypeIsSubclass) {
                        dataFile.getLogger().printWarning("Tasks row " + rowNumber
                            + " has empty rdf:type but legacy hasco:hascoType subclass is present; rdf:type will be inferred during ingestion.");
                    } else {
                        System.err.println("[WKF Validation] Tasks row " + rowNumber + " has empty rdf:type");
                        dataFile.getLogger().printException("Tasks row " + rowNumber + " has empty rdf:type");
                        valid = false;
                    }
                } else if (!isTaskTypeOrSubclass(rdfType, taskSubclassCache, dataFile)) {
                    System.err.println("[WKF Validation] Tasks row " + rowNumber + " has rdf:type not compatible with vstoi:Task: " + rdfType);
                    dataFile.getLogger().printException("Tasks row " + rowNumber + " has rdf:type not compatible with vstoi:Task: " + rdfType);
                    valid = false;
                }

                rowNumber++;
            }

            return valid;
        } catch (Exception e) {
            System.err.println("[WKF Validation] Error validating task typing semantics: " + e.getMessage());
            dataFile.getLogger().printException("Error validating task typing semantics: " + e.getMessage());
            return false;
        }
    }

    private static String safeValue(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isTaskArchetypeValue(String value) {
        String v = safeValue(value);
        return VSTOI.TASK.equals(v) || "vstoi:Task".equals(v);
    }

    private static boolean isTaskTypeOrSubclass(String taskType,
                                                Map<String, Boolean> cache,
                                                DataFile dataFile) {
        String value = safeValue(taskType);
        if (value.isEmpty()) {
            return false;
        }

        boolean lexicalTaskLike = value.endsWith("Task")
            || value.contains(":Task")
            || value.contains("#Task");

        if (isTaskArchetypeValue(value)) {
            return true;
        }

        if (cache.containsKey(value)) {
            return cache.get(value);
        }

        try {
            String candidateToken = toSparqlResource(value);
            String taskToken = toSparqlResource(VSTOI.TASK);
            String query = NameSpaces.getInstance().printSparqlNameSpaceList()
                    + "SELECT ?candidate WHERE { "
                    + "  VALUES ?candidate { " + candidateToken + " } "
                    + "  ?candidate rdfs:subClassOf* " + taskToken + " . "
                    + "} LIMIT 1";

            ResultSetRewindable rs = SPARQLUtils.select(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), query);
            boolean isSubclass = rs != null && rs.hasNext();
            if (!isSubclass && lexicalTaskLike) {
                dataFile.getLogger().printWarning("Task type '" + value
                    + "' has no explicit rdfs:subClassOf* vstoi:Task assertion in KG; accepting by lexical task-like fallback.");
                cache.put(value, true);
                return true;
            }
            cache.put(value, isSubclass);
            return isSubclass;
        } catch (Exception e) {
            // If subclass lookup cannot be evaluated, keep strict lexical guard.
            boolean lexicalFallback = lexicalTaskLike;
            dataFile.getLogger().printWarning("Task type subclass lookup failed for '" + value + "'; using lexical fallback=" + lexicalFallback + ". Reason: " + e.getMessage());
            cache.put(value, lexicalFallback);
            return lexicalFallback;
        }
    }

    private static String toSparqlResource(String value) {
        String v = safeValue(value);
        if (v.startsWith("<") && v.endsWith(">")) {
            return v;
        }
        if (URIUtils.isValidURI(v) && (v.startsWith("http://") || v.startsWith("https://"))) {
            return "<" + v + ">";
        }
        return v;
    }

    private static boolean isValidDateValue(String value) {
        String v = safeValue(value);
        if (v.isEmpty()) {
            return true;
        }

        // Accept ISO and common workbook date forms used in WKF templates.
        String[] formats = {
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ssXXX"
        };

        for (String format : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                sdf.setLenient(false);
                sdf.parse(v);
                return true;
            } catch (Exception ignored) {
            }
        }

        return false;
    }

    /**
     * Validate Namespaces sheet against WKF-SPEC-V2 v1.2.2 minimum required mappings.
     */
    private static boolean validateNamespaceSheetSemantics(DataFile dataFile, Map<String, String> catalog) {
        // InfoSheet stores Namespaces pointer under key "hasDependencies" as per WKF-SPEC-V2.
        String namespaceSheetName = catalog.get("Namespaces");
        if (namespaceSheetName == null || namespaceSheetName.trim().isEmpty()) {
            namespaceSheetName = catalog.get("hasDependencies");
        }
        if (namespaceSheetName == null || namespaceSheetName.trim().isEmpty()) {
            dataFile.getLogger().printException("Missing Namespaces pointer in InfoSheet");
            return false;
        }

        try {
            RecordFile namespaces = new SpreadsheetRecordFile(dataFile.getFile(), namespaceSheetName.replace("#", ""));
            if (!namespaces.isValid() || namespaces.getRecords() == null) {
                dataFile.getLogger().printException("Namespaces sheet is missing or invalid");
                return false;
            }

            Map<String, String> required = new LinkedHashMap<>();
            required.put("hasco", "http://hadatac.org/ont/hasco#");
            required.put("vstoi", "http://hadatac.org/ont/vstoi#");
            required.put("prov", "http://www.w3.org/ns/prov#");
            required.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
            required.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
            required.put("owl", "http://www.w3.org/2002/07/owl#");
            required.put("xsd", "http://www.w3.org/2001/XMLSchema#");
            required.put("pmsr", "https://pmsr.net/ont/");

            Map<String, String> found = new HashMap<>();
            for (Record rec : namespaces.getRecords()) {
                String prefix = firstNonBlank(rec,
                    "prefix", "hasPrefix", "Prefix", "hasprefix");
                String uri = firstNonBlank(rec,
                    "namespace", "hasNameSpace", "Namespace", "hasnamespace");

                if (prefix.isEmpty() && uri.isEmpty()) {
                    continue;
                }

                if (!prefix.isEmpty()) {
                    found.put(prefix, uri);
                }
            }

            boolean valid = true;
            for (Map.Entry<String, String> entry : required.entrySet()) {
                String got = safeValue(found.get(entry.getKey()));
                if (!entry.getValue().equals(got)) {
                    dataFile.getLogger().printException("Namespaces entry must be '" + entry.getKey()
                        + " -> " + entry.getValue() + "' and found '" + got + "'");
                    valid = false;
                }
            }
            return valid;
        } catch (Exception e) {
            dataFile.getLogger().printException("Error validating Namespaces semantics: " + e.getMessage());
            return false;
        }
    }

    private static String firstNonBlank(Record rec, String... keys) {
        if (rec == null || keys == null) {
            return "";
        }
        for (String key : keys) {
            try {
                String value = rec.getValueByColumnName(key);
                if (value != null && !value.trim().isEmpty()) {
                    return value.trim();
                }
            } catch (Exception ignored) {
            }
        }
        return "";
    }

    /**
     * Enforce unique top-level task and Process.vstoi:hasTopTask coherence.
     */
    private static boolean validateTopLevelTaskSemantics(DataFile dataFile, Map<String, String> catalog) {
        String tasksSheet = catalog.get("Tasks");
        if (tasksSheet == null || tasksSheet.trim().isEmpty()) {
            return true;
        }

        try {
            RecordFile tasks = new SpreadsheetRecordFile(dataFile.getFile(), tasksSheet.replace("#", ""));
            if (!tasks.isValid() || tasks.getRecords() == null || tasks.getRecords().isEmpty()) {
                return true;
            }

            Set<String> allTasks = new HashSet<>();
            Set<String> topLevelTasks = new HashSet<>();
            Map<String, String> parentByTask = new HashMap<>();

            for (Record rec : tasks.getRecords()) {
                String taskUri = safeValue(rec.getValueByColumnName("hasURI"));
                String supertask = safeValue(rec.getValueByColumnName("vstoi:hasSupertask"));
                if (taskUri.isEmpty()) {
                    continue;
                }
                allTasks.add(taskUri);
                if (supertask.isEmpty()) {
                    topLevelTasks.add(taskUri);
                } else {
                    parentByTask.put(taskUri, supertask);
                }
            }

            if (topLevelTasks.size() != 1) {
                dataFile.getLogger().printException("WKF must have exactly one top-level task, found " + topLevelTasks.size());
                return false;
            }

            String root = topLevelTasks.iterator().next();
            for (String task : allTasks) {
                if (task.equals(root)) {
                    continue;
                }

                String cursor = task;
                Set<String> guard = new HashSet<>();
                boolean reachesRoot = false;
                while (!cursor.isEmpty() && !guard.contains(cursor)) {
                    guard.add(cursor);
                    String parent = safeValue(parentByTask.get(cursor));
                    if (parent.isEmpty()) {
                        break;
                    }
                    if (parent.equals(root)) {
                        reachesRoot = true;
                        break;
                    }
                    cursor = parent;
                }

                if (!reachesRoot) {
                    dataFile.getLogger().printException("Task is not a descendant of unique top-level task: " + task);
                    return false;
                }
            }

            String processSheet = catalog.get("Processes");
            if (processSheet != null && !processSheet.trim().isEmpty()) {
                RecordFile processes = new SpreadsheetRecordFile(dataFile.getFile(), processSheet.replace("#", ""));
                if (processes.isValid() && processes.getRecords() != null) {
                    for (Record rec : processes.getRecords()) {
                        String processUri = safeValue(rec.getValueByColumnName("hasURI"));
                        String topTask = safeValue(rec.getValueByColumnName("vstoi:hasTopTask"));
                        if (processUri.isEmpty()) {
                            continue;
                        }
                        if (!root.equals(topTask)) {
                            dataFile.getLogger().printException("Process " + processUri
                                + " must reference unique top-level task " + root + " in vstoi:hasTopTask");
                            return false;
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            dataFile.getLogger().printException("Error validating top-level task semantics: " + e.getMessage());
            return false;
        }
    }

    private static Row firstNonEmptyDataRow(Sheet sheet, int startRowIndex) {
        for (int rowIndex = Math.max(startRowIndex, 0); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            for (int col = row.getFirstCellNum(); col <= row.getLastCellNum(); col++) {
                if (col < 0) {
                    continue;
                }
                Cell cell = row.getCell(col);
                if (cell != null && !cell.toString().trim().isEmpty()) {
                    return row;
                }
            }
        }
        return null;
    }

    /**
     * CRITICAL PRIORITY FIX: Validate temporal dependencies form a DAG (Directed Acyclic Graph)
     * Requirements:
     * - No circular dependencies in vstoi:hasTemporalDependency
     * - Uses DFS (Depth-First Search) cycle detection
     */
    private static boolean validateTemporalDependencyDAG(DataFile dataFile, Map<String, String> catalog) {
        String tasksSheet = catalog.get("Tasks");
        if (tasksSheet == null || tasksSheet.trim().isEmpty()) {
            // No Tasks sheet, no temporal dependencies to validate
            return true;
        }
        
        try {
            RecordFile tasks = new SpreadsheetRecordFile(dataFile.getFile(), tasksSheet.replace("#", ""));
            if (!tasks.isValid() || tasks.getRecords().isEmpty()) {
                return true; // No tasks, no dependencies
            }
            
            // Build dependency graph: task -> list of predecessors (tasks it depends on via "after")
            Map<String, List<String>> graph = new HashMap<>();
            Map<String, String> taskURIs = new HashMap<>();
            
            for (Record rec : tasks.getRecords()) {
                String uri = rec.getValueByColumnName("hasURI");
                String tempDep = rec.getValueByColumnName("vstoi:hasTemporalDependency");
                
                if (uri != null && !uri.trim().isEmpty()) {
                    taskURIs.put(uri, uri);
                    
                    // Parse temporal dependency - only "after" creates directed edge
                    if (tempDep != null && tempDep.trim().startsWith("after ")) {
                        String predecessor = tempDep.substring(6).trim();
                        graph.computeIfAbsent(uri, k -> new ArrayList<>()).add(predecessor);
                    }
                }
            }
            
            // Detect cycles using DFS
            Set<String> visited = new HashSet<>();
            Set<String> recStack = new HashSet<>();
            
            for (String task : graph.keySet()) {
                if (hasCycleDFS(task, graph, visited, recStack, dataFile)) {
                    return false; // Cycle detected
                }
            }
            
            return true; // No cycles - valid DAG
            
        } catch (Exception e) {
            System.err.println("[WKF Validation] Error validating temporal dependencies: " + e.getMessage());
            e.printStackTrace();
            // Don't fail on exception - log and continue
            dataFile.getLogger().printWarning("Could not validate temporal dependencies: " + e.getMessage());
            return true;
        }
    }

    /**
     * DFS-based cycle detection helper for temporal dependency DAG
     */
    private static boolean hasCycleDFS(String task, Map<String, List<String>> graph,
                                       Set<String> visited, Set<String> recStack,
                                       DataFile dataFile) {
        if (recStack.contains(task)) {
            // Cycle detected!
            System.err.println("[WKF Validation] CYCLE DETECTED in temporal dependencies involving task: " + task);
            dataFile.getLogger().printExceptionByIdWithArgs("WKF_00018", task);
            return true;
        }
        
        if (visited.contains(task)) {
            return false; // Already processed this branch
        }
        
        visited.add(task);
        recStack.add(task);
        
        for (String neighbor : graph.getOrDefault(task, Collections.emptyList())) {
            if (hasCycleDFS(neighbor, graph, visited, recStack, dataFile)) {
                return true;
            }
        }
        
        recStack.remove(task);
        return false;
    }

    /**
     * CRITICAL PRIORITY FIX: Validate task hierarchy has no circular references
     * Requirements:
     * - No cycles in vstoi:hasSupertask / vstoi:hasSubtask relationships
     * - Tasks form a proper tree structure
     */
    private static boolean validateTaskHierarchy(DataFile dataFile, Map<String, String> catalog) {
        String tasksSheet = catalog.get("Tasks");
        if (tasksSheet == null || tasksSheet.trim().isEmpty()) {
            return true; // No Tasks sheet
        }
        
        try {
            RecordFile tasks = new SpreadsheetRecordFile(dataFile.getFile(), tasksSheet.replace("#", ""));
            if (!tasks.isValid() || tasks.getRecords().isEmpty()) {
                return true;
            }
            
            // Build hierarchy graph: child -> parent (via hasSupertask)
            Map<String, String> hierarchy = new HashMap<>();
            
            for (Record rec : tasks.getRecords()) {
                String taskUri = rec.getValueByColumnName("hasURI");
                String supertaskUri = rec.getValueByColumnName("vstoi:hasSupertask");
                
                if (taskUri != null && !taskUri.trim().isEmpty()) {
                    if (supertaskUri != null && !supertaskUri.trim().isEmpty()) {
                        hierarchy.put(taskUri, supertaskUri);
                    }
                }
            }
            
            // Detect cycles: for each task, follow parent chain
            Set<String> visited = new HashSet<>();
            for (String task : hierarchy.keySet()) {
                if (!visited.contains(task)) {
                    if (hasHierarchyCycle(task, hierarchy, visited, new HashSet<>(), dataFile)) {
                        return false; // Cycle detected
                    }
                }
            }
            
            return true; // No cycles
            
        } catch (Exception e) {
            System.err.println("[WKF Validation] Error validating task hierarchy: " + e.getMessage());
            e.printStackTrace();
            dataFile.getLogger().printWarning("Could not validate task hierarchy: " + e.getMessage());
            return true;
        }
    }

    /**
     * Cycle detection helper for task hierarchy
     */
    private static boolean hasHierarchyCycle(String task, Map<String, String> hierarchy,
                                            Set<String> visited, Set<String> recStack,
                                            DataFile dataFile) {
        if (recStack.contains(task)) {
            // Cycle detected!
            System.err.println("[WKF Validation] CYCLE DETECTED in task hierarchy involving task: " + task);
            dataFile.getLogger().printExceptionByIdWithArgs("WKF_00018", task);
            return true;
        }
        
        if (visited.contains(task)) {
            return false;
        }
        
        visited.add(task);
        recStack.add(task);
        
        String parent = hierarchy.get(task);
        if (parent != null && !parent.trim().isEmpty()) {
            if (hasHierarchyCycle(parent, hierarchy, visited, recStack, dataFile)) {
                return true;
            }
        }
        
        recStack.remove(task);
        return false;
    }

    /**
     * HIGH PRIORITY FIX: Validate reference integrity across sheets
     * Checks:
     * - Process -> ProcessStem (prov:wasDerivedFrom)
     * - Process -> Task (vstoi:hasTopTask)
     * - Task -> Task (vstoi:hasSupertask, vstoi:hasSubtask)
     * - Task -> ComponentInstance (vstoi:usesComponentInstance) URI format
     * Task type restriction (when property is populated):
     * - rdf:type must be vstoi:AutomatedTask or vstoi:InteractionTask
     */
    private static boolean validateReferenceIntegrity(DataFile dataFile, Map<String, String> catalog) {
        try {
            // Collect all URIs by type
            Map<String, Set<String>> urisByType = new HashMap<>();
            urisByType.put("ProcessStem", collectURIs(dataFile, catalog, "ProcessStems"));
            urisByType.put("Process", collectURIs(dataFile, catalog, "Processes"));
            urisByType.put("Task", collectURIs(dataFile, catalog, "Tasks"));
            
            boolean allValid = true;
            
            // Validate Process references
            String processSheet = catalog.get("Processes");
            if (processSheet != null && !processSheet.trim().isEmpty()) {
                RecordFile processes = new SpreadsheetRecordFile(dataFile.getFile(), processSheet.replace("#", ""));
                if (processes.isValid()) {
                    for (Record rec : processes.getRecords()) {
                        String processUri = rec.getValueByColumnName("hasURI");
                        
                        // Check prov:wasDerivedFrom -> ProcessStem
                        String stemRef = rec.getValueByColumnName("prov:wasDerivedFrom");
                        if (stemRef != null && !stemRef.trim().isEmpty()) {
                            if (!urisByType.get("ProcessStem").contains(stemRef)) {
                                System.err.println("[WKF Validation] Process " + processUri + " references non-existent ProcessStem: " + stemRef);
                                dataFile.getLogger().printWarningByIdWithArgs("WKF_00002", processUri, stemRef);
                                allValid = false;
                            }
                        }
                        
                        // Check vstoi:hasTopTask -> Task
                        String topTask = rec.getValueByColumnName("vstoi:hasTopTask");
                        if (topTask != null && !topTask.trim().isEmpty()) {
                            if (!urisByType.get("Task").contains(topTask)) {
                                System.err.println("[WKF Validation] Process " + processUri + " references non-existent top task: " + topTask);
                                dataFile.getLogger().printExceptionByIdWithArgs("WKF_00013", processUri, topTask);
                                allValid = false;
                            }
                        }
                    }
                }
            }
            
            // Validate Task references
            String taskSheet = catalog.get("Tasks");
            if (taskSheet != null && !taskSheet.trim().isEmpty()) {
                RecordFile tasks = new SpreadsheetRecordFile(dataFile.getFile(), taskSheet.replace("#", ""));
                if (tasks.isValid()) {
                    for (Record rec : tasks.getRecords()) {
                        String taskUri = rec.getValueByColumnName("hasURI");
                        
                        // Check vstoi:hasSupertask -> Task
                        String supertask = rec.getValueByColumnName("vstoi:hasSupertask");
                        if (supertask != null && !supertask.trim().isEmpty()) {
                            if (!urisByType.get("Task").contains(supertask)) {
                                System.err.println("[WKF Validation] Task " + taskUri + " references non-existent supertask: " + supertask);
                                dataFile.getLogger().printExceptionByIdWithArgs("WKF_00014", taskUri, supertask);
                                allValid = false;
                            }
                        }
                        
                        // Check vstoi:hasSubtask -> Tasks (semicolon-separated)
                        String subtasks = rec.getValueByColumnName("vstoi:hasSubtask");
                        if (subtasks != null && !subtasks.trim().isEmpty()) {
                            String[] subtaskList = subtasks.split(";");
                            for (String subtask : subtaskList) {
                                String cleanSubtask = subtask.trim();
                                if (!cleanSubtask.isEmpty() && !urisByType.get("Task").contains(cleanSubtask)) {
                                    System.err.println("[WKF Validation] Task " + taskUri + " references non-existent subtask: " + cleanSubtask);
                                    dataFile.getLogger().printExceptionByIdWithArgs("WKF_00015", taskUri, cleanSubtask);
                                    allValid = false;
                                }
                            }
                        }
                        
                        // WKF-SPEC-V3: validate usesComponentInstance by URI format and task type only.
                        String componentInstances = rec.getValueByColumnName("vstoi:usesComponentInstance");
                        if (componentInstances != null && !componentInstances.trim().isEmpty()) {
                            String taskType = safeValue(rec.getValueByColumnName("rdf:type"));
                            if (!isTaskTypeAllowedForComponentInstances(taskType)) {
                                System.err.println("[WKF Validation] Task " + taskUri
                                    + " uses vstoi:usesComponentInstance but rdf:type is not vstoi:AutomatedTask or vstoi:InteractionTask: " + taskType);
                                dataFile.getLogger().printException("Task " + taskUri
                                    + " uses vstoi:usesComponentInstance but rdf:type is not vstoi:AutomatedTask or vstoi:InteractionTask: " + taskType);
                                allValid = false;
                            }

                            String[] componentInstanceList = componentInstances.split("[;|]");
                            for (String componentInstance : componentInstanceList) {
                                String cleanUri = componentInstance.trim();
                                if (cleanUri.isEmpty()) {
                                    continue;
                                }
                                if (!isValidHttpUri(cleanUri)) {
                                    System.err.println("[WKF Validation] Task " + taskUri + " has non-URI vstoi:usesComponentInstance value: " + cleanUri);
                                    dataFile.getLogger().printException("Task " + taskUri + " has non-URI vstoi:usesComponentInstance value: " + cleanUri);
                                    allValid = false;
                                }
                            }
                        }
                    }
                }
            }

            return allValid; // Return false only if internal references are broken
            
        } catch (Exception e) {
            System.err.println("[WKF Validation] Error validating reference integrity: " + e.getMessage());
            e.printStackTrace();
            dataFile.getLogger().printWarning("Could not validate reference integrity: " + e.getMessage());
            return true; // Don't fail on exception
        }
    }

    /**
     * Helper method to collect all URIs from a sheet
     */
    private static Set<String> collectURIs(DataFile dataFile, Map<String, String> catalog, String sheetKey) {
        Set<String> uris = new HashSet<>();
        String sheetName = catalog.get(sheetKey);
        
        if (sheetName != null && !sheetName.trim().isEmpty()) {
            try {
                RecordFile sheet = new SpreadsheetRecordFile(dataFile.getFile(), sheetName.replace("#", ""));
                if (sheet.isValid()) {
                    for (Record rec : sheet.getRecords()) {
                        String uri = rec.getValueByColumnName("hasURI");
                        if (uri != null && !uri.trim().isEmpty()) {
                            uris.add(uri.trim());
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[WKF Validation] Error collecting URIs from " + sheetKey + ": " + e.getMessage());
            }
        }
        
        return uris;
    }

    private static boolean isValidHttpUri(String value) {
        String v = safeValue(value);
        return URIUtils.isValidURI(v) && (v.startsWith("http://") || v.startsWith("https://"));
    }

    private static boolean isTaskTypeAllowedForComponentInstances(String taskType) {
        String tt = safeValue(taskType);
        if (tt.isEmpty()) {
            return false;
        }

        return "vstoi:AutomatedTask".equals(tt)
            || "vstoi:InteractionTask".equals(tt)
            || VSTOI.AUTOMATED_TASK.equals(tt)
            || VSTOI.INTERACTION_TASK.equals(tt);
    }

    /**
     * EDUCATIONAL ENHANCEMENT: Validate educational properties for training/simulation workflows
     * Requirements (Conditional):
     * - Educational workflows should have learning objectives
     * - Educational workflows should have critical actions
     * - Educational workflows should have debriefing focus
     * 
     * Checks Process.hasco:hascoType to determine if workflow is educational
     */
    private static void validateEducationalProperties(DataFile dataFile, Map<String, String> catalog) {
        String processSheet = catalog.get("Processes");
        if (processSheet == null || processSheet.trim().isEmpty()) {
            return; // No Processes sheet
        }

        Map<String, Record> pbsByProcess = loadProcessBasedStudiesByProcess(dataFile, catalog);
        
        try {
            RecordFile processes = new SpreadsheetRecordFile(dataFile.getFile(), processSheet.replace("#", ""));
            if (!processes.isValid() || processes.getRecords().isEmpty()) {
                return;
            }
            
            for (Record rec : processes.getRecords()) {
                String processUri = rec.getValueByColumnName("hasURI");
                String hascoType = rec.getValueByColumnName("hasco:hascoType");
                
                // Check if this is an educational workflow
                if (hascoType != null && isEducationalWorkflow(hascoType)) {
                    Record pbs = pbsByProcess.get(processUri);
                    String learningObjectives = pbs != null ? pbs.getValueByColumnName("vstoi:hasLearningObjectives") : null;
                    String criticalActions = pbs != null ? pbs.getValueByColumnName("vstoi:hasCriticalActions") : null;
                    String debriefingFocus = pbs != null ? pbs.getValueByColumnName("vstoi:hasDebriefingFocus") : null;
                    
                    // Warn if educational workflow is missing educational properties
                    if (learningObjectives == null || learningObjectives.trim().isEmpty()) {
                        System.out.println("[WKF Validation] WARNING: Educational process " + processUri + " is missing learning objectives in ProcessBasedStudy (vstoi:hasLearningObjectives)");
                        dataFile.getLogger().printWarning("Educational process " + processUri + " should define learning objectives in ProcessBasedStudy for INACSL compliance");
                    }
                    
                    if (criticalActions == null || criticalActions.trim().isEmpty()) {
                        System.out.println("[WKF Validation] WARNING: Educational process " + processUri + " is missing critical actions in ProcessBasedStudy (vstoi:hasCriticalActions)");
                        dataFile.getLogger().printWarning("Educational process " + processUri + " should define critical actions in ProcessBasedStudy for assessment");
                    }
                    
                    if (debriefingFocus == null || debriefingFocus.trim().isEmpty()) {
                        System.out.println("[WKF Validation] INFO: Educational process " + processUri + " is missing debriefing focus in ProcessBasedStudy (vstoi:hasDebriefingFocus)");
                        dataFile.getLogger().printWarning("Educational process " + processUri + " should define debriefing topics in ProcessBasedStudy for structured reflection");
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("[WKF Validation] Error validating educational properties: " + e.getMessage());
            // Don't fail on exception - this is optional validation
        }
    }

    /**
     * Helper method to determine if a workflow is educational/training based on hascoType
     */
    private static boolean isEducationalWorkflow(String hascoType) {
        if (hascoType == null) {
            return false;
        }
        
        String lowerType = hascoType.toLowerCase();
        return lowerType.contains("training") || 
               lowerType.contains("simulation") || 
               lowerType.contains("educational") ||
               lowerType.contains("learning") ||
               lowerType.contains("teaching") ||
               lowerType.contains("assessment") ||
               lowerType.contains("clinical") && lowerType.contains("scenario");
    }

    /**
     * EDUCATIONAL ENHANCEMENT: Validate objective consistency between Process and Tasks
     * Requirements:
     * - All Process objectives should be supported by at least one Task
     * - All Task objectives should match Process objectives (exact string match)
     * - Warn about orphaned objectives or tasks
     */
    private static void validateObjectiveConsistency(DataFile dataFile, Map<String, String> catalog) {
        String processSheet = catalog.get("Processes");
        String taskSheet = catalog.get("Tasks");
        
        if (processSheet == null || taskSheet == null) {
            return; // Can't validate without both sheets
        }

        Map<String, Record> pbsByProcess = loadProcessBasedStudiesByProcess(dataFile, catalog);
        
        try {
            RecordFile processes = new SpreadsheetRecordFile(dataFile.getFile(), processSheet.replace("#", ""));
            RecordFile tasks = new SpreadsheetRecordFile(dataFile.getFile(), taskSheet.replace("#", ""));
            
            if (!processes.isValid() || !tasks.isValid()) {
                return;
            }
            
            // For each process with learning objectives
            for (Record processRec : processes.getRecords()) {
                String processUri = processRec.getValueByColumnName("hasURI");
                Record pbs = pbsByProcess.get(processUri);
                String learningObjectivesStr = pbs != null ? pbs.getValueByColumnName("vstoi:hasLearningObjectives") : null;
                
                if (learningObjectivesStr == null || learningObjectivesStr.trim().isEmpty()) {
                    continue; // No objectives to validate
                }
                
                // Parse process objectives
                Set<String> processObjectives = parseObjectives(learningObjectivesStr);
                Set<String> taskObjectives = new HashSet<>();
                
                // Collect all objectives from tasks
                for (Record taskRec : tasks.getRecords()) {
                    String taskObjectiveStr = taskRec.getValueByColumnName("vstoi:supportsObjective");
                    if (taskObjectiveStr != null && !taskObjectiveStr.trim().isEmpty()) {
                        taskObjectives.addAll(parseObjectives(taskObjectiveStr));
                    }
                }
                
                // Check for process objectives not mapped to any task
                Set<String> unmappedObjectives = new HashSet<>(processObjectives);
                unmappedObjectives.removeAll(taskObjectives);
                
                if (!unmappedObjectives.isEmpty()) {
                    System.out.println("[WKF Validation] WARNING: ProcessBasedStudy objectives for process " + processUri + " are not mapped to any task:");
                    for (String objective : unmappedObjectives) {
                        System.out.println("  - " + objective);
                        dataFile.getLogger().printWarning("ProcessBasedStudy objective not mapped to task: " + objective);
                    }
                }
                
                // Check for task objectives not defined in process
                Set<String> undefinedObjectives = new HashSet<>(taskObjectives);
                undefinedObjectives.removeAll(processObjectives);
                
                if (!undefinedObjectives.isEmpty()) {
                    System.out.println("[WKF Validation] WARNING: Tasks reference objectives not defined in ProcessBasedStudy for process " + processUri + ":");
                    for (String objective : undefinedObjectives) {
                        System.out.println("  - " + objective);
                        dataFile.getLogger().printWarning("Task references objective undefined in ProcessBasedStudy: " + objective);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("[WKF Validation] Error validating objective consistency: " + e.getMessage());
            // Don't fail on exception - this is optional validation
        }
    }

    /**
     * Helper method to parse semicolon-separated objectives into a Set
     */
    private static Set<String> parseObjectives(String objectivesStr) {
        Set<String> objectives = new HashSet<>();
        if (objectivesStr == null || objectivesStr.trim().isEmpty()) {
            return objectives;
        }
        
        String[] parts = objectivesStr.split(";");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                objectives.add(trimmed);
            }
        }
        
        return objectives;
    }

    /**
     * Index ProcessBasedStudy rows by related process URI.
     */
    private static Map<String, Record> loadProcessBasedStudiesByProcess(DataFile dataFile, Map<String, String> catalog) {
        Map<String, Record> byProcess = new HashMap<>();
        String pbsSheet = catalog.get("ProcessBasedStudies");

        if (pbsSheet == null || pbsSheet.trim().isEmpty()) {
            pbsSheet = catalog.get("ProcessBasedStudy");
        }

        if (pbsSheet == null || pbsSheet.trim().isEmpty()) {
            return byProcess;
        }

        try {
            RecordFile pbsRows = new SpreadsheetRecordFile(dataFile.getFile(), pbsSheet.replace("#", ""));
            if (!pbsRows.isValid()) {
                return byProcess;
            }

            for (Record rec : pbsRows.getRecords()) {
                String processUri = rec.getValueByColumnName("hasco:hasProcess");
                if (processUri != null && !processUri.trim().isEmpty()) {
                    byProcess.put(processUri.trim(), rec);
                }
            }
        } catch (Exception e) {
            System.err.println("[WKF Validation] Error loading ProcessBasedStudies sheet: " + e.getMessage());
        }

        return byProcess;
    }
}
