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

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.function.Supplier;

public class AnnotateWKF extends BaseAnnotator {

    private static final String RULE_WKF_NULL_DATAFILE = "WKF-RULE-000";
    private static final String RULE_WKF_CATALOG_LOAD = "WKF-RULE-020";
    private static final String RULE_INFO_SHEET_STRUCTURE = "WKF-RULE-101";
    private static final String RULE_STD_SEMANTICS = "WKF-RULE-102";
    private static final String RULE_NAMESPACES_SEMANTICS = "WKF-RULE-103";
    private static final String RULE_TASK_TYPING = "WKF-RULE-201";
    private static final String RULE_TEMPORAL_DAG = "WKF-RULE-202";
    private static final String RULE_TASK_HIERARCHY_ACYCLIC = "WKF-RULE-203";
    private static final String RULE_TASK_PARENT_CHILD = "WKF-RULE-204";
    private static final String RULE_TOP_LEVEL_TASK = "WKF-RULE-205";
    private static final String RULE_REFERENCE_INTEGRITY = "WKF-RULE-301";

    private static final String SPEC_GENERAL = "WKF-SPEC-V3: General";
    private static final String SPEC_FILE_FORMAT = "WKF-SPEC-V3: File Format";
    private static final String SPEC_INFOSHEET = "WKF-SPEC-V3: InfoSheet";
    private static final String SPEC_STD = "WKF-SPEC-V3: STD";
    private static final String SPEC_NAMESPACES = "WKF-SPEC-V3: Namespaces";
    private static final String SPEC_TASK_TYPING = "WKF-SPEC-V3: Tasks / Typing rules";
    private static final String SPEC_TASK_HIERARCHY = "WKF-SPEC-V3: Tasks / Task hierarchy and cardinality rules";
    private static final String SPEC_CROSS_SHEET = "WKF-SPEC-V3: Cross-Sheet Integrity Requirements";
    private static final String SPEC_PMSR = "WKF-SPEC-V3: PMSR Normative Rules";

    public static class BrokenRule {
        private final String ruleId;
        private final String message;
        private final String specSection;

        public BrokenRule(String ruleId, String message, String specSection) {
            this.ruleId = ruleId;
            this.message = message;
            this.specSection = specSection;
        }

        public String getRuleId() {
            return ruleId;
        }

        public String getMessage() {
            return message;
        }

        public String getSpecSection() {
            return specSection;
        }
    }

    public static class ValidationReport {
        private boolean valid = true;
        private final LinkedHashMap<String, BrokenRule> brokenRules = new LinkedHashMap<>();

        public boolean isValid() {
            return valid;
        }

        public List<String> getBrokenRules() {
            return brokenRules.values().stream()
                .map(BrokenRule::getMessage)
                .collect(Collectors.toList());
        }

        public List<BrokenRule> getBrokenRuleDetails() {
            return new ArrayList<>(brokenRules.values());
        }

        public void addBrokenRule(String ruleId, String message, String specSection) {
            String normalizedRuleId = normalize(ruleId);
            String normalizedMessage = normalize(message);
            String normalizedSpecSection = normalize(specSection);

            if (normalizedRuleId.isEmpty() || normalizedMessage.isEmpty()) {
                return;
            }

            String key = normalizedRuleId + "||" + normalizedMessage;
            if (!brokenRules.containsKey(key)) {
                brokenRules.put(key,
                    new BrokenRule(normalizedRuleId, normalizedMessage, normalizedSpecSection));
            }
            valid = false;
        }

        public boolean hasRuleId(String ruleId) {
            String normalizedRuleId = normalize(ruleId);
            if (normalizedRuleId.isEmpty()) {
                return false;
            }
            for (BrokenRule rule : brokenRules.values()) {
                if (rule != null && normalizedRuleId.equals(normalize(rule.getRuleId()))) {
                    return true;
                }
            }
            return false;
        }

        private String normalize(String value) {
            return value == null ? "" : value.trim();
        }
    }

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

        if (!runWkfValidationPipeline(dataFile, mapCatalog, true, true, null)) {
            return null;
        }

        System.out.println("========== AnnotateWKF.exec() END (SUCCESS) ==========\n");
        return chain;
    }

    /**
     * Validation-only entrypoint for WKF templates.
     * Executes all ingestion-time semantic checks without creating or committing entities.
     */
    public static boolean validateOnly(DataFile dataFile) {
        return validateOnlyWithReport(dataFile).isValid();
    }

    public static ValidationReport validateOnlyWithReport(DataFile dataFile) {
        ValidationReport report = new ValidationReport();
        if (dataFile == null) {
            report.addBrokenRule(
                RULE_WKF_NULL_DATAFILE,
                "WKF validation failed: DataFile is null.",
                SPEC_GENERAL);
            return report;
        }

        File workbook = resolveWorkbookFile(dataFile);
        if (workbook == null || !workbook.exists() || !workbook.canRead()) {
            String filename = dataFile.getFilename() == null ? "" : dataFile.getFilename().trim();
            String suffix = filename.isEmpty() ? "" : (" (filename: " + filename + ")");
            String msg = "WKF validation failed: template workbook file is missing or unreadable for selected WKF" + suffix + ".";
            dataFile.getLogger().printException(msg);
            report.addBrokenRule(
                RULE_WKF_CATALOG_LOAD,
                msg,
                SPEC_FILE_FORMAT);
            return report;
        }

        Map<String, String> mapCatalog = loadCatalog(dataFile, Constants.MT_WKF);
        if (mapCatalog == null) {
            dataFile.getLogger().printExceptionById("WKF_00020");
            report.addBrokenRule(
                RULE_WKF_CATALOG_LOAD,
                "WKF_00020: Failed to load WKF catalog.",
                SPEC_CROSS_SHEET);
            return report;
        }

        runWkfValidationPipeline(dataFile, mapCatalog, false, false, report);
        return report;
    }

    private static boolean runWkfValidationPipeline(DataFile dataFile,
                                                    Map<String, String> mapCatalog,
                                                    boolean failFast,
                                                    boolean verbose,
                                                    ValidationReport report) {
        boolean valid = true;

        valid &= runValidationStep(
            dataFile,
            "InfoSheet structure",
            () -> validateInfoSheetStructure(dataFile, mapCatalog),
            () -> dataFile.getLogger().printExceptionById("WKF_00001"),
            RULE_INFO_SHEET_STRUCTURE,
            "WKF_00001: InfoSheet structure validation failed.",
            SPEC_INFOSHEET,
            report,
            verbose);
        if (failFast && !valid) {
            return false;
        }

        valid &= runValidationStep(
            dataFile,
            "STD sheet semantics",
            () -> validateStdSheetSemantics(dataFile, mapCatalog),
            () -> dataFile.getLogger().printException("WKF STD sheet semantic validation failed"),
            RULE_STD_SEMANTICS,
            "WKF STD sheet semantic validation failed",
            SPEC_STD,
            report,
            verbose);
        if (failFast && !valid) {
            return false;
        }

        valid &= runValidationStep(
            dataFile,
            "Namespaces semantics",
            () -> validateNamespaceSheetSemantics(dataFile, mapCatalog),
            () -> dataFile.getLogger().printException("WKF Namespaces semantic validation failed"),
            RULE_NAMESPACES_SEMANTICS,
            "WKF Namespaces semantic validation failed",
            SPEC_NAMESPACES,
            report,
            verbose);
        if (failFast && !valid) {
            return false;
        }

        valid &= runValidationStep(
            dataFile,
            "task typing semantics",
            () -> validateTaskTypingSemantics(dataFile, mapCatalog, report),
            () -> dataFile.getLogger().printException("WKF Task typing semantic validation failed"),
            RULE_TASK_TYPING,
            "WKF Task typing semantic validation failed",
            SPEC_TASK_TYPING,
            report,
            verbose);
        if (failFast && !valid) {
            return false;
        }

        valid &= runValidationStep(
            dataFile,
            "temporal dependency DAG",
            () -> validateTemporalDependencyDAG(dataFile, mapCatalog),
            () -> dataFile.getLogger().printExceptionById("WKF_00004"),
            RULE_TEMPORAL_DAG,
            "WKF_00004: Temporal dependency DAG validation failed.",
            SPEC_CROSS_SHEET,
            report,
            verbose);
        if (failFast && !valid) {
            return false;
        }

        valid &= runValidationStep(
            dataFile,
            "task hierarchy",
            () -> validateTaskHierarchy(dataFile, mapCatalog),
            () -> dataFile.getLogger().printExceptionById("WKF_00003"),
            RULE_TASK_HIERARCHY_ACYCLIC,
            "WKF_00003: Task hierarchy validation failed.",
            SPEC_CROSS_SHEET,
            report,
            verbose);
        if (failFast && !valid) {
            return false;
        }

        valid &= runValidationStep(
            dataFile,
            "task parent-child semantics",
            () -> validateTaskParentChildSemantics(dataFile, mapCatalog),
            () -> dataFile.getLogger().printException("WKF task parent-child semantic validation failed"),
            RULE_TASK_PARENT_CHILD,
            "WKF task parent-child semantic validation failed",
            SPEC_TASK_HIERARCHY,
            report,
            verbose);
        if (failFast && !valid) {
            return false;
        }

        valid &= runValidationStep(
            dataFile,
            "top-level task semantics",
            () -> validateTopLevelTaskSemantics(dataFile, mapCatalog),
            () -> dataFile.getLogger().printException("WKF top-level task semantic validation failed"),
            RULE_TOP_LEVEL_TASK,
            "WKF top-level task semantic validation failed",
            SPEC_PMSR,
            report,
            verbose);
        if (failFast && !valid) {
            return false;
        }

        valid &= runValidationStep(
            dataFile,
            "reference integrity",
            () -> validateReferenceIntegrity(dataFile, mapCatalog),
            () -> dataFile.getLogger().printException("WKF reference integrity validation failed"),
            RULE_REFERENCE_INTEGRITY,
            "WKF reference integrity validation failed",
            SPEC_CROSS_SHEET,
            report,
            verbose);
        if (failFast && !valid) {
            return false;
        }

        if (verbose) {
            System.out.println("→ Validating educational properties...");
        }
        validateEducationalProperties(dataFile, mapCatalog);
        if (verbose) {
            System.out.println("✓ Educational properties validation completed");
            System.out.println("→ Validating objective consistency...");
        }
        validateObjectiveConsistency(dataFile, mapCatalog);
        if (verbose) {
            System.out.println("✓ Objective consistency validation completed");
        }

        return valid;
    }

    private static boolean runValidationStep(DataFile dataFile,
                                             String label,
                                             Supplier<Boolean> step,
                                             Runnable onFailure,
                                             String ruleId,
                                             String brokenRule,
                                             String specSection,
                                             ValidationReport report,
                                             boolean verbose) {
        if (verbose) {
            System.out.println("→ Validating " + label + "...");
        }

        boolean stepValid = step.get();
        if (!stepValid) {
            if (verbose) {
                System.err.println("❌ " + capitalizeLabel(label) + " validation failed");
            }
            onFailure.run();
            if (report != null && !report.hasRuleId(ruleId)) {
                report.addBrokenRule(ruleId, brokenRule, specSection);
            }
            return false;
        }

        if (verbose) {
            System.out.println("✓ " + capitalizeLabel(label) + " validated successfully");
        }
        return true;
    }

    private static String capitalizeLabel(String label) {
        if (label == null || label.isEmpty()) {
            return "Validation";
        }
        return Character.toUpperCase(label.charAt(0)) + label.substring(1);
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
        if (rows < 5 || rows > 7) {
            System.err.println("[WKF Validation] InfoSheet must have 5 to 7 data rows, found " + rows);
            dataFile.getLogger().printException("InfoSheet must have 5 to 7 data rows");
            valid = false;
        }

        try (FileInputStream fis = new FileInputStream(resolveWorkbookFile(dataFile));
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
                if ("RequiredInstruments".equals(key) && !"#RequiredInstruments".equals(value)) {
                    dataFile.getLogger().printException("InfoSheet RequiredInstruments must point to #RequiredInstruments and found '" + value + "'");
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

              try (FileInputStream fis = new FileInputStream(resolveWorkbookFile(dataFile));
                 Workbook workbook = new XSSFWorkbook(fis)) {

                Sheet stdSheet = workbook.getSheet(stdSheetName);
                if (stdSheet == null) {
                    populateStdMetadataFromWkfComment(dataFile, stdMetadata);
                    return stdMetadata;
                }

                DataFormatter formatter = new DataFormatter();
                List<String> stdRequiredHeaders = Arrays.asList(
                    "hasURI",
                    "hasco:hasProcess",
                    "Study ID",
                    "Title",
                    "Institution",
                    "Principal Investigator",
                    "Email"
                );

                Row headerRow = resolveStdHeaderRow(stdSheet, formatter, stdRequiredHeaders);
                Row dataRow = resolveStdDataRow(stdSheet, formatter, headerRow);

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
            // Some WKF workbooks intentionally omit STD linkage.
            dataFile.getLogger().printWarning("WKF InfoSheet has no hasStudyDescription pointer; skipping STD semantic validation.");
            return true;
        }

        String stdSheetName = stdPointer.trim().replace("#", "");
        if (stdSheetName.isEmpty()) {
            System.err.println("[WKF Validation] Invalid hasStudyDescription pointer: " + stdPointer);
            dataFile.getLogger().printException("Invalid hasStudyDescription pointer: " + stdPointer);
            return false;
        }

        try (FileInputStream fis = new FileInputStream(resolveWorkbookFile(dataFile));
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet stdSheet = workbook.getSheet(stdSheetName);
            if (stdSheet == null) {
                System.err.println("[WKF Validation] STD sheet not found: " + stdSheetName);
                dataFile.getLogger().printException("STD sheet not found: " + stdSheetName);
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

            DataFormatter formatter = new DataFormatter();
            Row headerRow = resolveStdHeaderRow(stdSheet, formatter, requiredHeaders);
            if (headerRow == null) {
                System.err.println("[WKF Validation] STD sheet missing recognizable header row");
                dataFile.getLogger().printException("STD sheet missing recognizable header row");
                return false;
            }
            int headerRowIndex = headerRow.getRowNum();

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
            for (int rowIndex = Math.max(headerRowIndex + 1, 0); rowIndex <= stdSheet.getLastRowNum(); rowIndex++) {
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
                } else if (isTemplatePlaceholderOwnershipUri(institutionValue)) {
                    dataFile.getLogger().printWarning("STD row " + stdRowNumber + " uses template ownership Institution URI: " + institutionValue);
                }

                String piValue = formatter.formatCellValue(stdRow.getCell(headerIndex.get("Principal Investigator"))).trim();
                if (piValue.isEmpty() || !URIUtils.isValidURI(piValue)) {
                    dataFile.getLogger().printException("STD row " + stdRowNumber + " must define valid Principal Investigator URI");
                    valid = false;
                } else if (isTemplatePlaceholderOwnershipUri(piValue)) {
                    dataFile.getLogger().printWarning("STD row " + stdRowNumber + " uses template ownership Principal Investigator URI: " + piValue);
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
     * Strict WKF-SPEC-V3 validation for Tasks typing:
     * - Column C (hasco:hascoType) MUST be exactly vstoi:Task
     * - Column B (rdf:type) MUST resolve to one canonical value:
     *   vstoi:AbstractTask, vstoi:ManualTask, vstoi:AutomatedTask, vstoi:InteractionTask
     *
     * Backward compatibility: legacy aliases (for example vstoi:UserTask)
     * are normalized to canonical values before validation.
     */
    private static boolean validateTaskTypingSemantics(DataFile dataFile,
                                                       Map<String, String> catalog,
                                                       ValidationReport report) {
        String tasksSheet = catalog.get("Tasks");
        if (tasksSheet == null || tasksSheet.trim().isEmpty()) {
            return true;
        }

        try {
            File workbook = resolveWorkbookFile(dataFile);
            RecordFile tasks = new SpreadsheetRecordFile(workbook, tasksSheet.replace("#", ""));
            if (!tasks.isValid() || tasks.getRecords() == null || tasks.getRecords().isEmpty()) {
                return true;
            }

            boolean valid = true;
            int rowNumber = 2; // Tasks sheet header is expected at row 1

            for (Record rec : tasks.getRecords()) {
                String taskUri = safeValue(rec.getValueByColumnName("hasURI"));
                String hascoType = safeValue(rec.getValueByColumnName("hasco:hascoType"));
                String rdfType = safeValue(rec.getValueByColumnName("rdf:type"));

                // Skip structurally empty rows
                if (taskUri.isEmpty() && hascoType.isEmpty() && rdfType.isEmpty()) {
                    rowNumber++;
                    continue;
                }

                if (!isTaskArchetypeValue(hascoType)) {
                    String msg = "Tasks row " + rowNumber
                        + " (hasURI=" + (taskUri.isEmpty() ? "(empty)" : taskUri) + ")"
                        + " has invalid hasco:hascoType (must be exactly vstoi:Task): "
                        + (hascoType.isEmpty() ? "(empty)" : hascoType);
                    System.err.println("[WKF Validation] " + msg);
                    dataFile.getLogger().printException(msg);
                    if (report != null) {
                        report.addBrokenRule(RULE_TASK_TYPING, msg, SPEC_TASK_TYPING);
                    }
                    valid = false;
                }

                if (rdfType.isEmpty()) {
                    String msg = "Tasks row " + rowNumber
                        + " (hasURI=" + (taskUri.isEmpty() ? "(empty)" : taskUri) + ") has empty rdf:type";
                    System.err.println("[WKF Validation] " + msg);
                    dataFile.getLogger().printException(msg);
                    if (report != null) {
                        report.addBrokenRule(RULE_TASK_TYPING, msg, SPEC_TASK_TYPING);
                    }
                    valid = false;
                } else if (!isAllowedConcreteTaskType(rdfType)) {
                    String msg = "Tasks row " + rowNumber
                        + " (hasURI=" + (taskUri.isEmpty() ? "(empty)" : taskUri) + ")"
                        + " has invalid rdf:type. Allowed canonical values: vstoi:AbstractTask, vstoi:ManualTask, vstoi:AutomatedTask, vstoi:InteractionTask. Found: "
                        + rdfType;
                    System.err.println("[WKF Validation] " + msg);
                    dataFile.getLogger().printException(msg);
                    if (report != null) {
                        report.addBrokenRule(RULE_TASK_TYPING, msg, SPEC_TASK_TYPING);
                    }
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
        return VSTOI.TASK.equals(v)
            || "vstoi:Task".equals(v)
            || "http://hadatac.org/ont/vstoi#Task".equals(v);
    }

    private static boolean isAllowedConcreteTaskType(String value) {
        String normalized = normalizeTaskType(value);
        return "vstoi:AbstractTask".equals(normalized)
            || "vstoi:ManualTask".equals(normalized)
            || "vstoi:AutomatedTask".equals(normalized)
            || "vstoi:InteractionTask".equals(normalized)
            || "vstoi:Task".equals(normalized);
    }

    private static String normalizeTaskType(String value) {
        String v = safeValue(value);
        if (v.startsWith("http://hadatac.org/ont/vstoi#")) {
            v = "vstoi:" + v.substring("http://hadatac.org/ont/vstoi#".length());
        }
        if (v.startsWith("https://hadatac.org/ont/vstoi#")) {
            v = "vstoi:" + v.substring("https://hadatac.org/ont/vstoi#".length());
        }

        // Canonicalize legacy aliases to WKF-SPEC-V3 concrete types.
        if ("vstoi:UserTask".equals(v) || "vstoi:InteractiveTask".equals(v)) {
            return "vstoi:InteractionTask";
        }
        if ("vstoi:ApplicationTask".equals(v) || "vstoi:SystemTask".equals(v)) {
            return "vstoi:AutomatedTask";
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
            RecordFile namespaces = new SpreadsheetRecordFile(resolveWorkbookFile(dataFile), namespaceSheetName.replace("#", ""));
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
            RecordFile tasks = new SpreadsheetRecordFile(resolveWorkbookFile(dataFile), tasksSheet.replace("#", ""));
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

            boolean valid = true;
            if (topLevelTasks.size() != 1) {
                dataFile.getLogger().printException("WKF must have exactly one top-level task, found " + topLevelTasks.size());
                valid = false;
            }

            String root = null;
            if (topLevelTasks.size() == 1) {
                root = topLevelTasks.iterator().next();
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
                        valid = false;
                    }
                }
            }

            String processSheet = catalog.get("Processes");
            if (processSheet != null && !processSheet.trim().isEmpty()) {
                RecordFile processes = new SpreadsheetRecordFile(resolveWorkbookFile(dataFile), processSheet.replace("#", ""));
                if (processes.isValid() && processes.getRecords() != null) {
                    for (Record rec : processes.getRecords()) {
                        String processUri = safeValue(rec.getValueByColumnName("hasURI"));
                        String topTask = safeValue(rec.getValueByColumnName("vstoi:hasTopTask"));
                        if (processUri.isEmpty()) {
                            continue;
                        }
                        if (root != null && !root.equals(topTask)) {
                            dataFile.getLogger().printException("Process " + processUri
                                + " must reference unique top-level task " + root + " in vstoi:hasTopTask");
                            valid = false;
                        }
                    }
                }
            }
            return valid;
        } catch (Exception e) {
            dataFile.getLogger().printException("Error validating top-level task semantics: " + e.getMessage());
            return false;
        }
    }

    /**
     * Enforce task hierarchy/cardinality semantics from WKF-SPEC-V3.
     */
    private static boolean validateTaskParentChildSemantics(DataFile dataFile, Map<String, String> catalog) {
        String tasksSheet = catalog.get("Tasks");
        if (tasksSheet == null || tasksSheet.trim().isEmpty()) {
            return true;
        }

        try {
            RecordFile tasks = new SpreadsheetRecordFile(resolveWorkbookFile(dataFile), tasksSheet.replace("#", ""));
            if (!tasks.isValid() || tasks.getRecords() == null || tasks.getRecords().isEmpty()) {
                return true;
            }

            boolean valid = true;
            Set<String> allTasks = new HashSet<>();
            Map<String, String> rdfTypeByTask = new HashMap<>();
            Map<String, String> temporalByTask = new HashMap<>();
            Map<String, String> supertaskByChild = new HashMap<>();
            Map<String, Set<String>> childrenByParentFromSubtask = new HashMap<>();
            Map<String, Set<String>> parentsByChildFromSubtask = new HashMap<>();

            int rowNumber = 2;
            for (Record rec : tasks.getRecords()) {
                String taskUri = safeValue(rec.getValueByColumnName("hasURI"));
                if (taskUri.isEmpty()) {
                    rowNumber++;
                    continue;
                }

                allTasks.add(taskUri);
                rdfTypeByTask.put(taskUri, normalizeTaskType(rec.getValueByColumnName("rdf:type")));
                temporalByTask.put(taskUri, safeValue(rec.getValueByColumnName("vstoi:hasTemporalDependency")));

                String supertask = safeValue(rec.getValueByColumnName("vstoi:hasSupertask"));
                if (!supertask.isEmpty()) {
                    String previous = supertaskByChild.putIfAbsent(taskUri, supertask);
                    if (previous != null && !previous.equals(supertask)) {
                        dataFile.getLogger().printException("Task " + taskUri
                            + " has conflicting vstoi:hasSupertask values: " + previous + " and " + supertask);
                        valid = false;
                    }
                }

                String subtasks = safeValue(rec.getValueByColumnName("vstoi:hasSubtask"));
                if (!subtasks.isEmpty()) {
                    Set<String> seenChildrenInRow = new HashSet<>();
                    for (String token : subtasks.split("[;|]")) {
                        String child = safeValue(token);
                        if (child.isEmpty()) {
                            continue;
                        }
                        if (!seenChildrenInRow.add(child)) {
                            dataFile.getLogger().printException("Task " + taskUri
                                + " lists duplicate child in vstoi:hasSubtask: " + child);
                            valid = false;
                            continue;
                        }

                        childrenByParentFromSubtask.computeIfAbsent(taskUri, k -> new HashSet<>()).add(child);
                        parentsByChildFromSubtask.computeIfAbsent(child, k -> new HashSet<>()).add(taskUri);
                    }
                }

                rowNumber++;
            }

            // Every non-top task MUST have exactly one immediate parent via hasSupertask.
            for (String task : allTasks) {
                String parent = safeValue(supertaskByChild.get(task));
                if (parent.isEmpty()) {
                    continue;
                }
                if (!allTasks.contains(parent)) {
                    dataFile.getLogger().printException("Task " + task
                        + " has vstoi:hasSupertask that does not resolve inside Tasks: " + parent);
                    valid = false;
                }
            }

            // No child can have more than one parent from hasSubtask side.
            for (Map.Entry<String, Set<String>> entry : parentsByChildFromSubtask.entrySet()) {
                if (entry.getValue().size() > 1) {
                    dataFile.getLogger().printException("Task " + entry.getKey()
                        + " is attached to more than one parent in vstoi:hasSubtask: " + entry.getValue());
                    valid = false;
                }
            }

            // Bidirectional consistency: hasSubtask -> hasSupertask
            for (Map.Entry<String, Set<String>> entry : childrenByParentFromSubtask.entrySet()) {
                String parent = entry.getKey();
                for (String child : entry.getValue()) {
                    String supertask = safeValue(supertaskByChild.get(child));
                    if (!parent.equals(supertask)) {
                        dataFile.getLogger().printException("Parent-child mismatch: parent " + parent
                            + " lists child " + child + " in vstoi:hasSubtask, but child has vstoi:hasSupertask="
                            + (supertask.isEmpty() ? "<empty>" : supertask));
                        valid = false;
                    }
                }
            }

            // Bidirectional consistency: hasSupertask -> hasSubtask
            for (Map.Entry<String, String> entry : supertaskByChild.entrySet()) {
                String child = entry.getKey();
                String parent = safeValue(entry.getValue());
                Set<String> listedChildren = childrenByParentFromSubtask.getOrDefault(parent, Collections.emptySet());
                if (!listedChildren.contains(child)) {
                    dataFile.getLogger().printException("Parent-child mismatch: child " + child
                        + " has vstoi:hasSupertask=" + parent
                        + " but parent does not include child in vstoi:hasSubtask");
                    valid = false;
                }
            }

            // Children computed from authoritative hasSupertask relation.
            Map<String, Set<String>> childrenByParentFromSupertask = new HashMap<>();
            for (Map.Entry<String, String> entry : supertaskByChild.entrySet()) {
                String child = entry.getKey();
                String parent = safeValue(entry.getValue());
                if (!parent.isEmpty()) {
                    childrenByParentFromSupertask.computeIfAbsent(parent, k -> new HashSet<>()).add(child);
                }
            }

            // AbstractTask cardinality rules.
            for (String task : allTasks) {
                String taskType = normalizeTaskType(rdfTypeByTask.get(task));
                int childCount = childrenByParentFromSupertask.getOrDefault(task, Collections.emptySet()).size();

                if ("vstoi:AbstractTask".equals(taskType) && childCount < 1) {
                    dataFile.getLogger().printException("Task " + task
                        + " has rdf:type vstoi:AbstractTask but has no child tasks");
                    valid = false;
                }

                String op = extractTemporalOperator(temporalByTask.get(task));
                if ("vstoi:AbstractTask".equals(taskType)
                    && ("parallel".equals(op) || "choice".equals(op) || "independent".equals(op))
                    && childCount < 2) {
                    dataFile.getLogger().printException("Task " + task
                        + " has rdf:type vstoi:AbstractTask and temporal operator " + op
                        + " but has fewer than two child tasks");
                    valid = false;
                }
            }

            return valid;
        } catch (Exception e) {
            dataFile.getLogger().printException("Error validating task parent-child semantics: " + e.getMessage());
            return false;
        }
    }

    private static String extractTemporalOperator(String temporalDependency) {
        String value = safeValue(temporalDependency).toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            return "";
        }

        // If multiple constraints are encoded, inspect only the first tokenized clause.
        String firstClause = safeValue(value.split("[;|]", 2)[0]);
        if (firstClause.isEmpty()) {
            return "";
        }

        if (firstClause.startsWith("after ")) {
            return "after";
        }
        if (firstClause.startsWith("before ")) {
            return "before";
        }

        String[] parts = firstClause.split("\\s+");
        return parts.length == 0 ? "" : parts[0];
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

    private static Row resolveStdHeaderRow(Sheet sheet, DataFormatter formatter, List<String> requiredHeaders) {
        if (sheet == null || formatter == null) {
            return null;
        }

        Set<String> required = new HashSet<>();
        if (requiredHeaders != null) {
            for (String header : requiredHeaders) {
                String norm = normalizeStdHeader(header);
                if (!norm.isEmpty()) {
                    required.add(norm);
                }
            }
        }

        int start = Math.max(sheet.getFirstRowNum(), 0);
        int end = sheet.getLastRowNum();
        for (int rowIndex = start; rowIndex <= end; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            Set<String> tokenSet = new HashSet<>();
            for (int col = row.getFirstCellNum(); col <= row.getLastCellNum(); col++) {
                if (col < 0) {
                    continue;
                }
                String token = normalizeStdHeader(formatter.formatCellValue(row.getCell(col)));
                if (!token.isEmpty()) {
                    tokenSet.add(token);
                }
            }

            if (tokenSet.isEmpty()) {
                continue;
            }

            if (!required.isEmpty() && tokenSet.containsAll(required)) {
                return row;
            }

            // Lenient fallback for corrected and legacy STD layouts.
            if (tokenSet.contains("hasuri") && tokenSet.contains("hasco:hasprocess")
                && tokenSet.contains("studyid") && tokenSet.contains("institution")
                && tokenSet.contains("principalinvestigator")) {
                return row;
            }
        }

        return null;
    }

    private static Row resolveStdDataRow(Sheet sheet, DataFormatter formatter, Row headerRow) {
        if (sheet == null || formatter == null || headerRow == null) {
            return null;
        }

        int start = Math.max(headerRow.getRowNum() + 1, 0);
        Row fallback = firstNonEmptyDataRow(sheet, start);
        if (fallback == null) {
            return null;
        }

        Map<String, Integer> headerIndex = new HashMap<>();
        for (int col = headerRow.getFirstCellNum(); col <= headerRow.getLastCellNum(); col++) {
            if (col < 0) {
                continue;
            }
            String header = normalizeStdHeader(formatter.formatCellValue(headerRow.getCell(col)));
            if (!header.isEmpty()) {
                headerIndex.put(header, col);
            }
        }

        int bestScore = Integer.MIN_VALUE;
        Row bestRow = null;
        for (int rowIndex = start; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            int score = scoreStdDataRow(row, headerIndex, formatter);
            if (score > bestScore) {
                bestScore = score;
                bestRow = row;
            }
        }

        return bestRow != null ? bestRow : fallback;
    }

    private static int scoreStdDataRow(Row row, Map<String, Integer> headerIndex, DataFormatter formatter) {
        if (row == null || headerIndex == null || formatter == null) {
            return Integer.MIN_VALUE;
        }

        boolean hasAnyValue = false;
        for (int col = row.getFirstCellNum(); col <= row.getLastCellNum(); col++) {
            if (col < 0) {
                continue;
            }
            String value = formatter.formatCellValue(row.getCell(col)).trim();
            if (!value.isEmpty()) {
                hasAnyValue = true;
                break;
            }
        }
        if (!hasAnyValue) {
            return Integer.MIN_VALUE;
        }

        int score = 0;
        score += scoreHeaderValue(row, headerIndex.get("hasuri"), formatter, false);
        score += scoreHeaderValue(row, headerIndex.get("studyid"), formatter, false);
        score += scoreHeaderValue(row, headerIndex.get("title"), formatter, false);

        String institution = getHeaderValue(row, headerIndex.get("institution"), formatter);
        if (!institution.isEmpty() && URIUtils.isValidURI(institution)) {
            score += 3;
            if (isTemplatePlaceholderOwnershipUri(institution)) {
                score -= 6;
            }
        }

        String pi = getHeaderValue(row, headerIndex.get("principalinvestigator"), formatter);
        if (!pi.isEmpty() && URIUtils.isValidURI(pi)) {
            score += 3;
            if (isTemplatePlaceholderOwnershipUri(pi)) {
                score -= 6;
            }
        }

        String process = getHeaderValue(row, headerIndex.get("hasco:hasprocess"), formatter);
        if (!process.isEmpty() && URIUtils.isValidURI(process)) {
            score += 2;
        }

        return score;
    }

    private static int scoreHeaderValue(Row row, Integer colIndex, DataFormatter formatter, boolean requireUri) {
        String value = getHeaderValue(row, colIndex, formatter);
        if (value.isEmpty()) {
            return 0;
        }
        if (!requireUri) {
            return 1;
        }
        return URIUtils.isValidURI(value) ? 1 : 0;
    }

    private static String getHeaderValue(Row row, Integer colIndex, DataFormatter formatter) {
        if (row == null || colIndex == null || formatter == null || colIndex < 0) {
            return "";
        }
        return formatter.formatCellValue(row.getCell(colIndex)).trim();
    }

    private static String normalizeStdHeader(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private static boolean isTemplatePlaceholderOwnershipUri(String rawUri) {
        if (rawUri == null || rawUri.trim().isEmpty()) {
            return false;
        }

        String normalized = URIUtils.canonicalizePmsrUri(URIUtils.replacePrefixEx(URIUtils.stripAngleBrackets(rawUri.trim())));
        String lower = normalized == null ? "" : normalized.trim().toLowerCase(Locale.ROOT);

        return lower.equals("https://pmsr.net/ont/org/ess")
            || lower.equals("https://pmsr.net/ont/per/pi-001")
            || lower.equals("pmsr:org/ess")
            || lower.equals("pmsr:per/pi-001");
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
            RecordFile tasks = new SpreadsheetRecordFile(resolveWorkbookFile(dataFile), tasksSheet.replace("#", ""));
            if (!tasks.isValid() || tasks.getRecords().isEmpty()) {
                return true; // No tasks, no dependencies
            }
            
            // Build dependency graph: task -> list of predecessors.
            // "after X"  => task depends on X      => edge task -> X
            // "before X" => X depends on task      => edge X -> task
            Map<String, List<String>> graph = new HashMap<>();
            Map<String, String> taskURIs = new HashMap<>();
            Map<String, String> temporalByTask = new HashMap<>();
            
            for (Record rec : tasks.getRecords()) {
                String uri = rec.getValueByColumnName("hasURI");
                String tempDep = rec.getValueByColumnName("vstoi:hasTemporalDependency");
                
                if (uri != null && !uri.trim().isEmpty()) {
                    String cleanUri = uri.trim();
                    taskURIs.put(cleanUri, cleanUri);
                    temporalByTask.put(cleanUri, safeValue(tempDep));
                }
            }

            boolean valid = true;
            for (Map.Entry<String, String> entry : temporalByTask.entrySet()) {
                String uri = entry.getKey();
                String tempDep = entry.getValue();
                if (tempDep.isEmpty()) {
                    continue;
                }

                for (String clauseRaw : tempDep.split("[;|]")) {
                    String clause = safeValue(clauseRaw);
                    if (clause.isEmpty()) {
                        continue;
                    }

                    String lower = clause.toLowerCase(Locale.ROOT);
                    if (lower.startsWith("after ")) {
                        String predecessor = safeValue(clause.substring(6));
                        if (predecessor.isEmpty()) {
                            continue;
                        }
                        if (!taskURIs.containsKey(predecessor)) {
                            dataFile.getLogger().printException("Task " + uri
                                + " has temporal dependency 'after' that references non-existent task: " + predecessor);
                            valid = false;
                        } else {
                            graph.computeIfAbsent(uri, k -> new ArrayList<>()).add(predecessor);
                        }
                    } else if (lower.startsWith("before ")) {
                        String successor = safeValue(clause.substring(7));
                        if (successor.isEmpty()) {
                            continue;
                        }
                        if (!taskURIs.containsKey(successor)) {
                            dataFile.getLogger().printException("Task " + uri
                                + " has temporal dependency 'before' that references non-existent task: " + successor);
                            valid = false;
                        } else {
                            graph.computeIfAbsent(successor, k -> new ArrayList<>()).add(uri);
                        }
                    }
                }
            }

            // Keep scanning for additional cycle violations even when some
            // dependencies reference missing tasks.
            
            // Detect cycles using DFS; keep scanning to report all cycles.
            Set<String> visited = new HashSet<>();
            Set<String> recStack = new HashSet<>();
            Set<String> cycleReported = new HashSet<>();
            boolean hasCycle = false;
            
            for (String task : graph.keySet()) {
                if (hasCycleDFS(task, graph, visited, recStack, cycleReported, dataFile)) {
                    hasCycle = true;
                }
            }
            
            return !hasCycle; // No cycles - valid DAG
            
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
                                       Set<String> cycleReported,
                                       DataFile dataFile) {
        boolean hasCycle = false;

        if (recStack.contains(task)) {
            // Cycle detected!
            if (!cycleReported.contains(task)) {
                cycleReported.add(task);
                System.err.println("[WKF Validation] CYCLE DETECTED in temporal dependencies involving task: " + task);
                dataFile.getLogger().printExceptionByIdWithArgs("WKF_00018", task);
            }
            return true;
        }
        
        if (visited.contains(task)) {
            return false; // Already processed this branch
        }
        
        visited.add(task);
        recStack.add(task);
        
        for (String neighbor : graph.getOrDefault(task, Collections.emptyList())) {
            if (hasCycleDFS(neighbor, graph, visited, recStack, cycleReported, dataFile)) {
                hasCycle = true;
            }
        }
        
        recStack.remove(task);
        return hasCycle;
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
            RecordFile tasks = new SpreadsheetRecordFile(resolveWorkbookFile(dataFile), tasksSheet.replace("#", ""));
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
            
            // Detect cycles: for each task, follow parent chain and keep scanning.
            Set<String> visited = new HashSet<>();
            Set<String> cycleReported = new HashSet<>();
            boolean hasCycle = false;
            for (String task : hierarchy.keySet()) {
                if (!visited.contains(task)) {
                    if (hasHierarchyCycle(task, hierarchy, visited, new HashSet<>(), cycleReported, dataFile)) {
                        hasCycle = true;
                    }
                }
            }
            
            return !hasCycle; // No cycles
            
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
                                            Set<String> cycleReported,
                                            DataFile dataFile) {
        boolean hasCycle = false;

        if (recStack.contains(task)) {
            // Cycle detected!
            if (!cycleReported.contains(task)) {
                cycleReported.add(task);
                System.err.println("[WKF Validation] CYCLE DETECTED in task hierarchy involving task: " + task);
                dataFile.getLogger().printExceptionByIdWithArgs("WKF_00018", task);
            }
            return true;
        }
        
        if (visited.contains(task)) {
            return false;
        }
        
        visited.add(task);
        recStack.add(task);
        
        String parent = hierarchy.get(task);
        if (parent != null && !parent.trim().isEmpty()) {
            if (hasHierarchyCycle(parent, hierarchy, visited, recStack, cycleReported, dataFile)) {
                hasCycle = true;
            }
        }
        
        recStack.remove(task);
        return hasCycle;
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
                RecordFile processes = new SpreadsheetRecordFile(resolveWorkbookFile(dataFile), processSheet.replace("#", ""));
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
                RecordFile tasks = new SpreadsheetRecordFile(resolveWorkbookFile(dataFile), taskSheet.replace("#", ""));
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
                RecordFile sheet = new SpreadsheetRecordFile(resolveWorkbookFile(dataFile), sheetName.replace("#", ""));
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
        String tt = normalizeTaskType(taskType);
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
            RecordFile processes = new SpreadsheetRecordFile(resolveWorkbookFile(dataFile), processSheet.replace("#", ""));
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
            RecordFile processes = new SpreadsheetRecordFile(resolveWorkbookFile(dataFile), processSheet.replace("#", ""));
            RecordFile tasks = new SpreadsheetRecordFile(resolveWorkbookFile(dataFile), taskSheet.replace("#", ""));
            
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
            RecordFile pbsRows = new SpreadsheetRecordFile(resolveWorkbookFile(dataFile), pbsSheet.replace("#", ""));
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
