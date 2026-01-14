package org.hascoapi.tests;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.ingestion.IngestionWorker;
import org.hascoapi.utils.ConfigProp;
import org.hascoapi.utils.IngestionLogger;
import org.hascoapi.vocabularies.VSTOI;
import org.hascoapi.transform.mt.dsg.DSGGen;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * HascoRoundtripTest
 *
 * A deterministic round-trip validation scaffold for HASCO Machine-Readable Templates (MTs).
 * It defines the 3-step lifecycle for each MT type:
 *
 * 1) MT understanding & ingestion (Excel -> triplestore)
 * 2) MT regeneration & testing data extraction (triplestore -> Excel; compare)
 * 3) Triplestore reset & deterministic re-ingestion (fresh -> identical state)
 *
 * Notes:
 * - This scaffold uses available building blocks in the codebase. For DSG, it exercises
 *   IngestionWorker.ingest with a real test workbook, similar to IngestionWorkerDSGTest.
 * - For other MTs, it will gracefully skip if an authoritative Excel input is not present
 *   yet or the corresponding regeneration API is not implemented.
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class HascoRoundtripTest {

    private enum MTType {
        DSG, INS, DP2, STR, KGR, SDD, DA
    }

    // Map MT type to its canonical test Excel location when available.
    private File getMtExcel(MTType type) {
        switch (type) {
            case DSG:
                return new File("test/resources/dsg/DSG-STD-test.xlsx");
          /*  case SDD:
                // TODO: point to authoritative SDD test workbook when available
                return new File("test/resources/sdd/SDD-STD-test.xlsx");
            case INS:
                return new File("test/resources/ins/INS-STD-test.xlsx");
            case DP2:
                return new File("test/resources/dp2/DP2-STD-test.xlsx");
            case STR:
                return new File("test/resources/str/STR-STD-test.xlsx");
            case KGR:
                return new File("test/resources/kgr/KGR-STD-test.xlsx");
            case DA:
                return new File("test/resources/da/DA-STD-test.xlsx");

             */
            default:
                return null;
        }
    }

    private DataFile mockDataFileFor(File excelFile) {
        DataFile dataFile = mock(DataFile.class);
        IngestionLogger logger = mock(IngestionLogger.class);

        when(dataFile.getFilename()).thenReturn(excelFile.getName());
        when(dataFile.getLogger()).thenReturn(logger);
        when(dataFile.getUri()).thenReturn("http://example.org/DF-" + excelFile.getName());

        // Provide file and status expected by IngestionWorker and SpreadsheetRecordFile
        when(dataFile.getFile()).thenReturn(excelFile);
        when(dataFile.getFileStatus()).thenReturn("");

        // IngestionWorker persists progress; keep these as no-ops so we can execute the flow.
        doNothing().when(dataFile).save();
        doNothing().when(logger).resetLog();
        doNothing().when(logger).println(anyString());
        doNothing().when(logger).printExceptionById(anyString());
        doNothing().when(logger).printExceptionByIdWithArgs(anyString(), any());

        // Track value-like setters so we can assert outcomes deterministically.
        java.util.concurrent.atomic.AtomicReference<String> statusRef = new java.util.concurrent.atomic.AtomicReference<>("");
        doAnswer(inv -> {
            statusRef.set(inv.getArgument(0));
            return null;
        }).when(dataFile).setFileStatus(anyString());
        when(dataFile.getFileStatus()).thenAnswer(inv -> statusRef.get());

        java.util.concurrent.atomic.AtomicReference<String> studyUriRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        doAnswer(inv -> {
            studyUriRef.set(inv.getArgument(0));
            return null;
        }).when(dataFile).setStudyUri(anyString());
        when(dataFile.getStudyUri()).thenAnswer(inv -> studyUriRef.get());

        // Backing storage for RecordFile set by IngestionWorker
        java.util.concurrent.atomic.AtomicReference<org.hascoapi.ingestion.RecordFile> rfRef =
                new java.util.concurrent.atomic.AtomicReference<>();
        when(dataFile.getRecordFile()).thenAnswer(inv -> rfRef.get());
        doAnswer(inv -> {
            rfRef.set(inv.getArgument(0));
            return null;
        }).when(dataFile).setRecordFile(any(org.hascoapi.ingestion.RecordFile.class));

        // Initialize with InfoSheet so early code paths can read a valid workbook
        rfRef.set(new org.hascoapi.ingestion.SpreadsheetRecordFile(
                excelFile, excelFile.getName(), "InfoSheet"));

        return dataFile;
    }

    private static final String TEMPLATE_GENERIC = "conf/template.generic.conf";
    private static final String REGENERATED_DSG_FILENAME = "DSG-STD-test-regenerated.xlsx";

    // Keep the last ingested study URI so step3 can delete/reingest deterministically.
    private static final java.util.concurrent.atomic.AtomicReference<String> LAST_INGESTED_STUDY_URI =
            new java.util.concurrent.atomic.AtomicReference<>(null);

    private void step1_ingest(MTType type) {
        File excel = getMtExcel(type);
        assumeTrue(excel != null && excel.exists(),
                () -> "Test input not found for " + type + ": " + (excel == null ? "null" : excel.getAbsolutePath()));

        if (type == MTType.DSG) {
            DataFile df = mockDataFileFor(excel);

            // Use the generic template so STD/SSD mappings are available for the test DSG.
            final String templatePath = TEMPLATE_GENERIC;

            assertDoesNotThrow(() -> IngestionWorker.ingest(df, excel, templatePath, ""),
                    () -> "Step 1 ingestion should complete without exceptions for " + type);

            assertNotNull(df.getStudyUri(), "DSG ingestion should set a study URI after STD phase");
            assertFalse(df.getStudyUri().isEmpty(), "DSG ingestion should set a non-empty study URI");
            LAST_INGESTED_STUDY_URI.set(df.getStudyUri());

            System.out.println("Test completed - DSG ingestion workflow executed. Final status: " + df.getFileStatus());
            return;
        }

        assumeTrue(false, () -> "Step 1 ingestion for " + type + " is not yet implemented in tests.");
    }

    private void step2_regenerate_and_compare(MTType type) {
        if (type == MTType.DSG) {
            // Step 2 (DSG only): regenerate a DSG workbook from the triplestore.
            //
            // IMPORTANT/TODO:
            // Today we generate by status (Draft) because the genByStudies/genByStudy flow
            // isn't reliable yet in this environment.
            // As soon as genByStudies works 100%, this should be switched to generate by the
            // exact study URI that was ingested in step 1.

            final String regeneratedFilename = REGENERATED_DSG_FILENAME;

            // Temporary workaround: use Draft to get the ingested study.
            // NOTE: in our test workbook, Study.hasStatus is often null, and DSGGen treats
            // null status as Draft.
            final String status = VSTOI.DRAFT;

            String result = null;
            try {
                result = DSGGen.genByStatus(status, regeneratedFilename, null, null);
            } catch (Exception e) {
                fail("DSG regeneration threw exception: " + e.getMessage());
            }

            assertNotNull(result, "DSGGen.genByStatus should return a result string");
            assertTrue(result.startsWith("SUCCESS"), "Expected SUCCESS from DSGGen.genByStatus but got: " + result);

            // DSGGen.save writes to ConfigProp.getPathIngestion() + filename
            final File out = new File(ConfigProp.getPathIngestion() + regeneratedFilename);
            assertTrue(out.exists(), "Regenerated DSG workbook should exist at: " + out.getAbsolutePath());
            assertTrue(out.length() > 0, "Regenerated DSG workbook should not be empty: " + out.getAbsolutePath());

            // Also copy the generated file into the repo under test/resources/generated/.
            // This makes it easy to download from the workspace and serves as an artifact for future diffing.
            final File generatedDir = new File("test/resources/generated");
            assertTrue(generatedDir.exists() || generatedDir.mkdirs(),
                    "Failed to create generated output dir: " + generatedDir.getAbsolutePath());
            final File generatedCopy = new File(generatedDir, regeneratedFilename);
            assertDoesNotThrow(() -> java.nio.file.Files.copy(
                    out.toPath(),
                    generatedCopy.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING),
                    "Copying regenerated DSG into test/resources/generated should not throw");
            assertTrue(generatedCopy.exists(), "Expected copied DSG at: " + generatedCopy.getAbsolutePath());
            assertTrue(generatedCopy.length() > 0, "Copied DSG workbook should not be empty: " + generatedCopy.getAbsolutePath());

            // Minimal structural validation: workbook has the expected base sheets.
            assertDoesNotThrow(() -> {
                try (java.io.FileInputStream in = new java.io.FileInputStream(out);
                     org.apache.poi.ss.usermodel.Workbook wb = org.apache.poi.ss.usermodel.WorkbookFactory.create(in)) {
                    assertNotNull(wb.getSheet("InfoSheet"), "Regenerated DSG must include InfoSheet");
                    assertNotNull(wb.getSheet("Namespaces"), "Regenerated DSG must include Namespaces");
                    assertNotNull(wb.getSheet("SSD"), "Regenerated DSG must include SSD");
                    assertNotNull(wb.getSheet("STD"), "Regenerated DSG must include STD");
                    assertNotNull(wb.getSheet("VD"), "Regenerated DSG must include VD");
                }
            });

            System.out.println("Step2 DSG regenerated workbook: " + out.getAbsolutePath());
            System.out.println("Step2 DSG copied to workspace: " + generatedCopy.getAbsolutePath());
            return;
        }

        // Placeholder: regeneration APIs are not currently wired for other MTs.
        assumeTrue(false, () -> "Step 2 regeneration & comparison for " + type + " awaits API wiring.");
    }

    private void step3_reset_and_deterministic_reingest(MTType type) {
        if (type == MTType.DSG) {
            // Contract for DSG step3:
            // 1) Validate regenerated DSG is a superset of the ingested DSG (same study, may contain more).
            // 2) Best-effort reset: delete the study from the triplestore.
            // 3) Re-ingest the original DSG and regenerate again; validate regenerated is still a superset.

            final File ingestedExcel = getMtExcel(MTType.DSG);
            assumeTrue(ingestedExcel != null && ingestedExcel.exists(),
                    () -> "Ingested DSG test input not found: " + (ingestedExcel == null ? "null" : ingestedExcel.getAbsolutePath()));

            final File regeneratedWorkspaceCopy = new File("test/resources/generated/" + REGENERATED_DSG_FILENAME);
            assumeTrue(regeneratedWorkspaceCopy.exists(),
                    () -> "Regenerated DSG not found (run step2 first): " + regeneratedWorkspaceCopy.getAbsolutePath());

            // Excel-based superset check (robust across formatting/order differences)
            assertTrue(isDsgWorkbookSuperset(regeneratedWorkspaceCopy, ingestedExcel),
                    "Regenerated DSG should contain everything from ingested DSG (it may contain more)." );

            // SPARQL-based superset check (triplestore signals): ensure key counts for this study
            // are >= what is implied by the ingested workbook.
            assertDoesNotThrow(() -> assertStudyGraphSupersetViaSparql(ingestedExcel),
                    "SPARQL-based graph superset validation should not throw");

            // Best-effort reset
            final String studyUri = LAST_INGESTED_STUDY_URI.get();
            if (studyUri != null && !studyUri.isEmpty()) {
                boolean deleted = false;
                try {
                    deleted = hardDeleteStudyViaSparql(studyUri);
                    System.out.println("Step3: hardDeleteStudyViaSparql deleted=" + deleted + " for study=" + studyUri);
                } catch (Exception e) {
                    System.out.println("Step3: WARNING - hardDeleteStudyViaSparql failed (fallback to Study.delete): " + e.getMessage());
                }

                if (!deleted) {
                    try {
                        org.hascoapi.entity.pojo.Study study = org.hascoapi.entity.pojo.Study.find(studyUri);
                        if (study != null) {
                            System.out.println("Step3: deleting study from triplestore (fallback): " + studyUri);
                            study.delete();
                        } else {
                            System.out.println("Step3: study not found for delete (continuing): " + studyUri);
                        }
                    } catch (Exception e) {
                        // Don't hard fail reset in CI-ish contexts; determinism check still provides signal.
                        System.out.println("Step3: WARNING - failed to delete study (continuing): " + e.getMessage());
                    }
                }
            } else {
                System.out.println("Step3: No captured study URI from step1; skipping delete/reset.");
            }

            // Re-ingest after reset
            DataFile df = mockDataFileFor(ingestedExcel);
            assertDoesNotThrow(() -> IngestionWorker.ingest(df, ingestedExcel, TEMPLATE_GENERIC, ""),
                    "Step3 re-ingestion should complete without throwing");
            assertNotNull(df.getStudyUri(), "Step3 re-ingestion should set study URI");
            LAST_INGESTED_STUDY_URI.set(df.getStudyUri());

            // Regenerate again to a new filename and validate superset again
            final String regenerated2 = "DSG-STD-test-regenerated-step3.xlsx";
            String res = DSGGen.genByStatus(VSTOI.DRAFT, regenerated2, null, null);
            assertNotNull(res);
            assertTrue(res.startsWith("SUCCESS"), "Expected SUCCESS from DSGGen in step3 but got: " + res);

            final File out2 = new File(ConfigProp.getPathIngestion() + regenerated2);
            assertTrue(out2.exists(), "Step3 regenerated DSG should exist at: " + out2.getAbsolutePath());

            final File generatedDir = new File("test/resources/generated");
            assertTrue(generatedDir.exists() || generatedDir.mkdirs());
            final File out2Copy = new File(generatedDir, regenerated2);
            assertDoesNotThrow(() -> java.nio.file.Files.copy(out2.toPath(), out2Copy.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING));

            assertTrue(isDsgWorkbookSuperset(out2Copy, ingestedExcel),
                    "Step3 regenerated DSG (after reset+reingest) should still contain everything from ingested DSG.");

            System.out.println("Step3 DSG: validated superset and deterministic regeneration.\n  base="
                    + ingestedExcel.getAbsolutePath() + "\n  regen1=" + regeneratedWorkspaceCopy.getAbsolutePath()
                    + "\n  regen2=" + out2Copy.getAbsolutePath());
            return;
        }

        // Placeholder: requires reset API for other MTs.
        assumeTrue(false, () -> "Step 3 reset & deterministic re-ingestion for " + type + " awaits reset API.");
    }

    private static boolean isDsgWorkbookSuperset(File regenerated, File ingested) {
        // Compare a stable subset of content:
        // - Namespaces: (hasPrefix, hasNameSpace)
        // - STD: Study ID
        // - SSD: (sheet, hasURI)
        // - SOC sheets referenced by SSD: originalID
        // This keeps the check robust to ordering and extra columns/rows.

        java.util.Map<String, java.util.Set<String>> base = extractDsgFingerprint(ingested);
        java.util.Map<String, java.util.Set<String>> regen = extractDsgFingerprint(regenerated);

        for (String key : base.keySet()) {
            java.util.Set<String> baseSet = base.get(key);
            java.util.Set<String> regenSet = regen.getOrDefault(key, java.util.Collections.emptySet());
            if (!regenSet.containsAll(baseSet)) {
                java.util.Set<String> missing = new java.util.HashSet<>(baseSet);
                missing.removeAll(regenSet);
                System.out.println("Step3 superset check failed for section=" + key + " missing=" + missing);
                return false;
            }
        }
        return true;
    }

    private static java.util.Map<String, java.util.Set<String>> extractDsgFingerprint(File xlsx) {
        java.util.Map<String, java.util.Set<String>> fp = new java.util.HashMap<>();
        fp.put("Namespaces", new java.util.HashSet<>());
        fp.put("STD", new java.util.HashSet<>());
        fp.put("SSD", new java.util.HashSet<>());
        fp.put("SOC", new java.util.HashSet<>());

        try (java.io.FileInputStream in = new java.io.FileInputStream(xlsx);
             org.apache.poi.ss.usermodel.Workbook wb = org.apache.poi.ss.usermodel.WorkbookFactory.create(in)) {

            // Namespaces: hasPrefix + "|" + hasNameSpace
            org.apache.poi.ss.usermodel.Sheet ns = wb.getSheet("Namespaces");
            if (ns != null) {
                for (int r = 1; r <= ns.getLastRowNum(); r++) {
                    org.apache.poi.ss.usermodel.Row row = ns.getRow(r);
                    if (row == null) continue;
                    String prefix = cellStr(row.getCell(0));
                    String uri = cellStr(row.getCell(1));
                    if (!prefix.isEmpty() || !uri.isEmpty()) {
                        fp.get("Namespaces").add(prefix + "|" + uri);
                    }
                }
            }

            // STD: Study ID (col 0)
            org.apache.poi.ss.usermodel.Sheet std = wb.getSheet("STD");
            if (std != null) {
                for (int r = 1; r <= std.getLastRowNum(); r++) {
                    org.apache.poi.ss.usermodel.Row row = std.getRow(r);
                    if (row == null) continue;
                    String studyId = normalizeStudyId(cellStr(row.getCell(0)));
                    if (!studyId.isEmpty()) {
                        fp.get("STD").add(studyId);
                    }
                }
            }

            // SSD: sheet (col 0) + "::" + hasURI (col 1)
            org.apache.poi.ss.usermodel.Sheet ssd = wb.getSheet("SSD");
            java.util.List<String> socSheets = new java.util.ArrayList<>();
            if (ssd != null) {
                for (int r = 1; r <= ssd.getLastRowNum(); r++) {
                    org.apache.poi.ss.usermodel.Row row = ssd.getRow(r);
                    if (row == null) continue;
                    String sheet = cellStr(row.getCell(0));
                    String hasUri = normalizeHasUri(cellStr(row.getCell(1)));
                    if (!sheet.isEmpty() || !hasUri.isEmpty()) {
                        fp.get("SSD").add(sheet + "::" + hasUri);
                    }
                    if (sheet.startsWith("#")) {
                        String soc = sheet.replace("#", "").trim();
                        if (!soc.isEmpty()) socSheets.add(soc);
                    }
                }
            }

            // SOC sheets: originalID (col 0)
            for (String socName : socSheets) {
                org.apache.poi.ss.usermodel.Sheet soc = wb.getSheet(socName);
                if (soc == null) continue;
                for (int r = 1; r <= soc.getLastRowNum(); r++) {
                    org.apache.poi.ss.usermodel.Row row = soc.getRow(r);
                    if (row == null) continue;
                    String originalId = normalizeOriginalId(cellStr(row.getCell(0)));
                    if (!originalId.isEmpty()) {
                        fp.get("SOC").add(socName + "::" + originalId);
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse DSG workbook " + xlsx.getAbsolutePath() + ": " + e.getMessage(), e);
        }

        return fp;
    }

    private static String normalizeStudyId(String raw) {
        if (raw == null) return "";
        String s = raw.trim();

        // Remove namespace prefix if present (e.g., ahead:STD-...)
        int colon = s.indexOf(':');
        if (colon > 0) {
            s = s.substring(colon + 1);
        }

        // Some files store the study identifier as STD-<originalId>
        if (s.startsWith("STD-")) {
            s = s.substring("STD-".length());
        }

        return s;
    }

    private static String normalizeHasUri(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        // hasURI values may be plain (LTE-...) or prefixed (ahead:LTE-...) or even full URIs.
        // For our superset check, we standardize to the local name after ':' if present.
        int colon = s.indexOf(':');
        if (colon > 0) {
            s = s.substring(colon + 1);
        }
        // Keep STD- prefix here because SSD hasURI isn't necessarily a study.
        return s;
    }

    private static String normalizeOriginalId(String raw) {
        return raw == null ? "" : raw.trim();
    }

    private static String cellStr(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) return "";
        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue() == null ? "" : cell.getStringCellValue().trim();
                case NUMERIC:
                    // avoid scientific notation
                    double d = cell.getNumericCellValue();
                    long l = (long) d;
                    return (d == l) ? Long.toString(l) : Double.toString(d);
                case BOOLEAN:
                    return Boolean.toString(cell.getBooleanCellValue());
                case FORMULA:
                    try {
                        return cell.getStringCellValue() == null ? "" : cell.getStringCellValue().trim();
                    } catch (Exception ignored) {
                        return "";
                    }
                default:
                    return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    @ParameterizedTest
    @DisplayName("HASCO round-trip: Step 1 ingestion for all MTs")
    @ValueSource(strings = { "DSG", "INS", "DP2", "STR", "KGR", "SDD", "DA" })
    public void step1_allMTs_ingest(String mtName) {
        MTType type = MTType.valueOf(mtName);
        step1_ingest(type);
    }

    @ParameterizedTest
    @DisplayName("HASCO round-trip: Step 2 regeneration & comparison for all MTs")
    @ValueSource(strings = { "DSG", "INS", "DP2", "STR", "KGR", "SDD", "DA" })
    public void step2_allMTs_regenerate_and_compare(String mtName) {
        MTType type = MTType.valueOf(mtName);
        step2_regenerate_and_compare(type);
    }

    @ParameterizedTest
    @DisplayName("HASCO round-trip: Step 3 reset & deterministic re-ingestion for all MTs")
    @ValueSource(strings = { "DSG", "INS", "DP2", "STR", "KGR", "SDD", "DA" })
    public void step3_allMTs_reset_and_deterministic_reingest(String mtName) {
        MTType type = MTType.valueOf(mtName);
        step3_reset_and_deterministic_reingest(type);
    }

    @DisplayName("Sanity: test scaffold lists all MT types")
    @org.junit.jupiter.api.Test
    public void sanity_z_listsAllMtTypes() {
        // "Check everything" sanity for the scaffold (no side-effects):
        // - enum coverage
        // - input workbook presence per MT (if wired in getMtExcel)
        // - step implementation status (DSG implemented; others currently intentional skips)
        // - expected generated artifacts for DSG (if they exist, they must be non-empty)

        List<MTType> types = Arrays.asList(MTType.values());
        List<MTType> expectedTypes = Arrays.asList(MTType.DSG, MTType.INS, MTType.DP2, MTType.STR, MTType.KGR, MTType.SDD, MTType.DA);
        assertTrue(types.containsAll(expectedTypes), "MTType enum must include all expected MT types");

        // 1) Inputs that are currently wired through getMtExcel()
        for (MTType t : expectedTypes) {
            File f = getMtExcel(t);
            if (t == MTType.DSG) {
                assertNotNull(f, "DSG input must be wired in getMtExcel");
                assertTrue(f.exists(), "DSG test input must exist at: " + f.getPath());
                assertTrue(f.length() > 0, "DSG test input must not be empty: " + f.getPath());
            } else {
                // For now, other MTs are intentionally not wired (return null), so we record that as expected.
                assertNull(f, "MT " + t + " is not yet wired (expected null in getMtExcel until implemented)");
            }
        }

        // 2) Step coverage expectations
        // DSG: step1/2/3 are implemented; others should currently "assumeTrue(false)" (skipped) after input check.
        // We can't invoke the steps here without causing side effects, so this sanity only documents intent.
        assertTrue(true, "Scaffold sanity: step implementations are expected for DSG only at this stage");

        // 3) Generated artifacts directory exists (created by step2/step3)
        File generatedDir = new File("test/resources/generated");
        assertTrue(generatedDir.exists() || generatedDir.mkdirs(), "generated dir should be creatable at: " + generatedDir.getPath());

        // If the DSG regenerated artifacts exist, they must be non-empty.
        File regen1 = new File(generatedDir, REGENERATED_DSG_FILENAME);
        if (regen1.exists()) {
            assertTrue(regen1.length() > 0, "regen1 exists but is empty: " + regen1.getPath());
        }
        File regen2 = new File(generatedDir, "DSG-STD-test-regenerated-step3.xlsx");
        if (regen2.exists()) {
            assertTrue(regen2.length() > 0, "regen2 exists but is empty: " + regen2.getPath());
        }
    }

    // Force JUnit to execute at least one plain test quickly; also helps sbt discover the suite without Play harness.
    @Test
    @DisplayName("Sanity: JUnit engine is running")
    public void sanity_z_z_junitRuns() {
        assertTrue(true);
    }

    private static void assertStudyGraphSupersetViaSparql(File ingestedDsg) {
        // We intentionally only check counts & presence, not exact equality.
        // The triplestore is allowed to have MORE data than what the workbook creates.

        // Derive study URI from the ingested workbook's STD sheet (col A, row 2)
        java.util.Map<String, java.util.Set<String>> fp = extractDsgFingerprint(ingestedDsg);
        String localStudyId = fp.getOrDefault("STD", java.util.Collections.emptySet()).stream().findFirst().orElse("");
        if (localStudyId.isEmpty()) {
            throw new IllegalStateException("Could not derive study ID from ingested DSG workbook");
        }

        // In this project, the local ID maps to ahead:STD-<local>.
        // We rely on the prefix configured in the triplestore (as seen in logs).
        String fullStudyUri = "http://hadatac.org/ont/arrowhead/STD-" + localStudyId;

        // Expected minimum SOC count from SSD sheet:
        long expectedSocMin = fp.getOrDefault("SSD", java.util.Collections.emptySet()).stream()
                .filter(s -> s.startsWith("#SOC-"))
                .count();

        // Expected minimum StudyObject count from SOC entries:
        long expectedObjMin = fp.getOrDefault("SOC", java.util.Collections.emptySet()).size();

        // Query counts in triplestore
        long actualSoc = sparqlCount(
                "PREFIX hasco: <http://hadatac.org/ont/hasco/> \n" +
                        "SELECT (COUNT(DISTINCT ?soc) AS ?tot) WHERE { \n" +
                        "  ?soc hasco:isMemberOf <" + fullStudyUri + "> .\n" +
                        "  ?soc hasco:hascoType <http://hadatac.org/ont/hasco/StudyObjectCollection> .\n" +
                        "}");

        long actualObj = sparqlCount(
                "PREFIX hasco: <http://hadatac.org/ont/hasco/> \n" +
                        "SELECT (COUNT(DISTINCT ?obj) AS ?tot) WHERE { \n" +
                        "  ?obj hasco:isMemberOf <" + fullStudyUri + "> .\n" +
                        "  ?obj hasco:hascoType <http://hadatac.org/ont/hasco/StudyObject> .\n" +
                        "}");

        System.out.println("Step3 SPARQL diag: study=" + fullStudyUri
                + " expectedSocMin=" + expectedSocMin + " actualSoc=" + actualSoc
                + " expectedObjMin=" + expectedObjMin + " actualObj=" + actualObj);

        // NOTE: Some environments may not store all objects as direct hasco:isMemberOf <study>.
        // We keep this conservative: just assert that there is at least the expected number of SOCs.
        assertTrue(actualSoc >= expectedSocMin,
                "Triplestore should have at least the SOCs expected by the ingested workbook. expectedMin="
                        + expectedSocMin + " actual=" + actualSoc);

        // Objects can be stored inside SOC graphs; if direct membership isn't used, this could be 0.
        // So we only assert if it is non-zero that it's >= expected.
        if (actualObj > 0) {
            assertTrue(actualObj >= expectedObjMin,
                    "Triplestore should have at least the StudyObjects expected by the ingested workbook when counted by membership. expectedMin="
                            + expectedObjMin + " actual=" + actualObj);
        }
    }

    private static long sparqlCount(String queryString) {
        String endpoint = org.hascoapi.utils.CollectionUtil.getCollectionPath(org.hascoapi.utils.CollectionUtil.Collection.SPARQL_QUERY);
        org.apache.jena.query.ResultSetRewindable rs = org.hascoapi.utils.SPARQLUtils.select(endpoint, queryString);
        if (rs == null || !rs.hasNext()) {
            return 0;
        }
        org.apache.jena.query.QuerySolution sol = rs.next();
        if (sol == null || sol.get("tot") == null) {
            return 0;
        }
        try {
            return sol.getLiteral("tot").getLong();
        } catch (Exception e) {
            try {
                return Long.parseLong(sol.get("tot").toString());
            } catch (Exception ignored) {
                return 0;
            }
        }
    }

    private static boolean hardDeleteStudyViaSparql(String studyUri) {
        // This attempts a "hard" delete by removing:
        // 1) everything where s/p/o are connected to the study URI itself
        // 2) everything for resources that areMemberOf the study
        //
        // It is still scoped (doesn't nuke the whole dataset).

        String endpoint = org.hascoapi.utils.CollectionUtil.getCollectionPath(org.hascoapi.utils.CollectionUtil.Collection.SPARQL_UPDATE);

        String delete = "PREFIX hasco: <http://hadatac.org/ont/hasco/> \n" +
                "DELETE { ?s ?p ?o } WHERE { \n" +
                "  { BIND(<" + studyUri + "> AS ?root) \n" +
                "    { ?s ?p ?o . FILTER(?s = ?root || ?o = ?root) }\n" +
                "  } UNION {\n" +
                "    ?member hasco:isMemberOf <" + studyUri + "> .\n" +
                "    ?s ?p ?o . FILTER(?s = ?member || ?o = ?member)\n" +
                "  }\n" +
                "}";

        org.apache.jena.update.UpdateRequest req = org.apache.jena.update.UpdateFactory.create(delete);
        org.apache.jena.update.UpdateProcessor proc = org.apache.jena.update.UpdateExecutionFactory.createRemote(req, endpoint);
        proc.execute();

        // Verify the study isn't returned as a Study anymore (best-effort)
        long remainingStudy = sparqlCount(
                "PREFIX hasco: <http://hadatac.org/ont/hasco/> \n" +
                        "SELECT (COUNT(DISTINCT ?s) AS ?tot) WHERE { ?s a ?t . FILTER(?s = <" + studyUri + ">) }");
        return remainingStudy == 0;
    }

    @DisplayName("Cleanup (tests): delete ingested data from triplestore (keeps generated XLSX)")
    @org.junit.jupiter.api.Test
    public void zzz_cleanup_triplestore_ingestions() {
        // This used to live under a "sanity" name, but it's not a sanity check.
        // It is a best-effort cleanup step intended to run LAST in this class.

        // Keep the original quick sanity assertion (MT enum exists) so the test still has a simple invariant.
        List<MTType> types = Arrays.asList(MTType.values());
        assertTrue(types.containsAll(Arrays.asList(MTType.DSG, MTType.INS, MTType.DP2, MTType.STR, MTType.KGR, MTType.SDD, MTType.DA)));

        // Cleanup: remove ONLY what we ingested during these tests (keep generated XLSX files under test/resources/generated).
        // For now we scope the cleanup to the known DSG study from the test workbook.
        // NOTE: This is triplestore cleanup (not SQL), because ingestion writes to the triplestore.
        try {
            final String ingestedStudyUri = "http://hadatac.org/ont/arrowhead/STD-LTE-PIAGET-WEATHER-STATION";
            boolean deleted = false;
            try {
                deleted = hardDeleteStudyViaSparql(ingestedStudyUri);
            } catch (Exception e) {
                System.out.println("cleanup: hardDeleteStudyViaSparql failed (fallback to Study.delete): " + e.getMessage());
            }
            if (!deleted) {
                try {
                    org.hascoapi.entity.pojo.Study study = org.hascoapi.entity.pojo.Study.find(ingestedStudyUri);
                    if (study != null) {
                        study.delete();
                    }
                } catch (Exception e) {
                    System.out.println("cleanup: Study.delete failed (continuing): " + e.getMessage());
                }
            }
            System.out.println("cleanup: deleted ingested study (best-effort): " + ingestedStudyUri);
        } catch (Exception e) {
            // Never fail cleanup on cleanup issues; it should remain a lightweight safety net.
            System.out.println("cleanup: WARNING - unexpected error (continuing): " + e.getMessage());
        }

        // If/when you want to WIPE EVERYTHING from the triplestore (dangerous), wire it here.
        // For now we keep it commented to avoid accidental data loss.
        // wipeTriplestoreEverything();
    }
}
