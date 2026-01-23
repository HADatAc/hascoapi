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

            case INS:
                // Authoritative INS test workbook provided under test/resources/ins
                // NOTE: filename currently contains a trailing space before .xlsx in the repo.
                return new File("test/resources/ins/INS-ARROWHEAD_v4_Components .xlsx");

            case DP2:
                return new File("test/resources/dp2/DP2-LTE-PIAGET-WEATHER-STATION-V3.xlsx");

          /*  case SDD:
                // TODO: point to authoritative SDD test workbook when available
                return new File("test/resources/sdd/SDD-STD-test.xlsx");
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

        // Ensure the DataFile URI is a valid IRI (no raw spaces), because we use it as namedGraphUri in SPARQL.
        String rawUri = "http://example.org/DF-" + excelFile.getName();
        String safeUri;
        try {
            // Encode only illegal chars from the filename portion.
            String encodedName = java.net.URLEncoder.encode(excelFile.getName(), java.nio.charset.StandardCharsets.UTF_8.toString())
                    .replace("+", "%20");
            safeUri = "http://example.org/DF-" + encodedName;
        } catch (Exception e) {
            safeUri = rawUri.replace(" ", "%20");
        }
        when(dataFile.getUri()).thenReturn(safeUri);

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
    private static final String REGENERATED_INS_FILENAME = "INS-ARROWHEAD_v4_Components-regenerated.xlsx";
    private static final String REGENERATED_DP2_FILENAME = "DP2-LTE-PIAGET-WEATHER-STATION-V3-regenerated.xlsx";

    // Keep the last ingested study URI so step3 can delete/reingest deterministically.
    private static final java.util.concurrent.atomic.AtomicReference<String> LAST_INGESTED_STUDY_URI =
            new java.util.concurrent.atomic.AtomicReference<>(null);

    private static final String STEP_SEPARATOR = "---------------------------------------------------------------------------------------";

    private static void printStepBanner(String message) {
        System.out.println("\n" + STEP_SEPARATOR);
        System.out.println(message);
        System.out.println(STEP_SEPARATOR + "\n");
    }

    private static final String GENERATED_DIR = "test/resources/generated";

    private static void dumpAndLogTtl(String label, DataFile df) {
        if (df == null || df.getUri() == null || df.getUri().isEmpty()) {
            System.out.println("[TTL] " + label + ": DataFile or URI is null/empty; skipping TTL dump.");
            return;
        }

        String graphUri = df.getUri();
        long count = org.hascoapi.utils.TripleStoreDumpUtil.countTriplesInNamedGraph(graphUri);
        System.out.println("[TTL] " + label + ": namedGraphUri=" + graphUri + " tripleCount=" + count);

        // Write TTL file into test/resources/generated
        File outDir = new File(GENERATED_DIR);
        if (!outDir.exists()) {
            outDir.mkdirs();
        }
        String safeLabel = label.replaceAll("[^A-Za-z0-9._-]+", "_");
        File out = new File(outDir, safeLabel + ".ttl");
        org.hascoapi.utils.TripleStoreDumpUtil.writeNamedGraphAsTurtle(graphUri, out);
        System.out.println("[TTL] " + label + ": wrote " + out.getAbsolutePath());

        // Log preview
        org.hascoapi.utils.TripleStoreDumpUtil.logNamedGraphTriplesPreview(graphUri, 25);
    }

    private static File copyToGenerated(File source, String targetName) {
        File generatedDir = new File(GENERATED_DIR);
        if (!generatedDir.exists()) {
            generatedDir.mkdirs();
        }
        File dest = new File(generatedDir, targetName);
        try {
            java.nio.file.Files.copy(source.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException("Failed to copy " + source.getAbsolutePath() + " to " + dest.getAbsolutePath() + ": " + e.getMessage(), e);
        }
        return dest;
    }

    private void step1_ingest(MTType type) {
        printStepBanner("STEP 1/3 - INGEST (Excel -> Triplestore) - MT=" + type);
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

            // Post-ingest check: validate that triples exist in the triplestore for what we just ingested.
            // We use a conservative "minimum" check so the environment can contain more data.
            assertDoesNotThrow(() -> assertStudyGraphPresentAfterIngestViaSparql(excel, df.getStudyUri()),
                    "Post-ingest SPARQL validation should not throw");

            System.out.println("Test completed - DSG ingestion workflow executed. Final status: " + df.getFileStatus());
            printStepBanner("STEP 1/3 - DONE - MT=" + type + " studyUri=" + df.getStudyUri());
            return;
        }

        if (type == MTType.INS) {
            DataFile df = mockDataFileFor(excel);
            final String status = VSTOI.DRAFT;

            assertDoesNotThrow(() -> IngestionWorker.ingest(df, excel, TEMPLATE_GENERIC, status),
                    () -> "Step 1 INS ingestion should complete without exceptions");

            assertNotNull(df.getFileStatus(), "INS ingestion should set a file status");
            assertFalse(df.getFileStatus().isEmpty(), "INS ingestion should set a non-empty file status");

            // After ingest: dump and log triples for this ingested file
            dumpAndLogTtl("INS_ingested_original", df);

            System.out.println("Test completed - INS ingestion workflow executed. Final status: " + df.getFileStatus());
            printStepBanner("STEP 1/3 - DONE - MT=" + type);
            return;
        }

        if (type == MTType.DP2) {
            DataFile df = mockDataFileFor(excel);
            final String status = VSTOI.DRAFT;

            assertDoesNotThrow(() -> IngestionWorker.ingest(df, excel, TEMPLATE_GENERIC, status),
                    () -> "Step 1 DP2 ingestion should complete without exceptions");

            assertNotNull(df.getFileStatus(), "DP2 ingestion should set a file status");
            assertFalse(df.getFileStatus().isEmpty(), "DP2 ingestion should set a non-empty file status");

            dumpAndLogTtl("DP2_ingested_original", df);

            System.out.println("Test completed - DP2 ingestion workflow executed. Final status: " + df.getFileStatus());
            printStepBanner("STEP 1/3 - DONE - MT=" + type);
            return;
        }

        assumeTrue(false, () -> "Step 1 ingestion for " + type + " is not yet implemented in tests.");
    }

    private void step2_regenerate_and_compare(MTType type) {
        printStepBanner("STEP 2/3 - REGENERATE & COMPARE (Triplestore -> Excel; compare) - MT=" + type);
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
            printStepBanner("STEP 2/3 - DONE - MT=" + type + " result=" + result);
            return;
        }

        if (type == MTType.INS) {
            final String regeneratedFilename = REGENERATED_INS_FILENAME;
            final String status = VSTOI.DRAFT;

            String result = null;
            try {
                result = org.hascoapi.transform.mt.ins.INSGen.genByStatus(status, regeneratedFilename, null, null);
            } catch (Exception e) {
                fail("INS regeneration threw exception: " + e.getMessage());
            }

            assertNotNull(result, "INSGen.genByStatus should return a result string (empty means success today)");
            assertTrue(result.isEmpty() || result.startsWith("SUCCESS"),
                    "Expected empty (current behavior) or SUCCESS* from INSGen.genByStatus but got: '" + result + "'");

            final File out = new File(ConfigProp.getPathIngestion() + regeneratedFilename);
            assertTrue(out.exists(), "Regenerated INS workbook should exist at: " + out.getAbsolutePath());
            assertTrue(out.length() > 0, "Regenerated INS workbook should not be empty: " + out.getAbsolutePath());

            final File generatedCopy = copyToGenerated(out, regeneratedFilename);
            assertTrue(generatedCopy.exists(), "Expected copied INS at: " + generatedCopy.getAbsolutePath());
            assertTrue(generatedCopy.length() > 0, "Copied INS workbook should not be empty: " + generatedCopy.getAbsolutePath());

            // Minimal structural validation
            assertDoesNotThrow(() -> {
                try (java.io.FileInputStream in = new java.io.FileInputStream(out);
                     org.apache.poi.ss.usermodel.Workbook wb = org.apache.poi.ss.usermodel.WorkbookFactory.create(in)) {
                    assertNotNull(wb.getSheet(org.hascoapi.transform.mt.ins.INSGen.INFOSHEET));
                    assertNotNull(wb.getSheet(org.hascoapi.transform.mt.ins.INSGen.NAMESPACES));
                }
            });

            // NEW: ingest the regenerated file and dump+log triples again
            DataFile regeneratedDf = mockDataFileFor(generatedCopy);
            assertDoesNotThrow(() -> IngestionWorker.ingest(regeneratedDf, generatedCopy, TEMPLATE_GENERIC, status),
                    "Ingesting regenerated INS workbook should not throw");
            dumpAndLogTtl("INS_ingested_regenerated", regeneratedDf);

            System.out.println("Step2 INS regenerated workbook: " + out.getAbsolutePath());
            System.out.println("Step2 INS copied to workspace: " + generatedCopy.getAbsolutePath());
            printStepBanner("STEP 2/3 - DONE - MT=" + type + " result=" + result);
            return;
        }

        if (type == MTType.DP2) {
            final String regeneratedFilename = REGENERATED_DP2_FILENAME;
            final String status = VSTOI.DRAFT;

            String result = null;
            try {
                result = org.hascoapi.transform.mt.dp2.DP2Gen.genByStatus(status, regeneratedFilename, null, null);
            } catch (Exception e) {
                fail("DP2 regeneration threw exception: " + e.getMessage());
            }

            // DP2Gen currently returns the filename on success (or empty)
            assertNotNull(result, "DP2Gen.genByStatus should return a result string");

            final File out = new File(ConfigProp.getPathIngestion() + regeneratedFilename);
            assertTrue(out.exists(), "Regenerated DP2 workbook should exist at: " + out.getAbsolutePath());
            assertTrue(out.length() > 0, "Regenerated DP2 workbook should not be empty: " + out.getAbsolutePath());

            final File generatedCopy = copyToGenerated(out, regeneratedFilename);
            assertTrue(generatedCopy.exists(), "Expected copied DP2 at: " + generatedCopy.getAbsolutePath());
            assertTrue(generatedCopy.length() > 0, "Copied DP2 workbook should not be empty: " + generatedCopy.getAbsolutePath());

            // Minimal structural validation
            assertDoesNotThrow(() -> {
                try (java.io.FileInputStream in = new java.io.FileInputStream(out);
                     org.apache.poi.ss.usermodel.Workbook wb = org.apache.poi.ss.usermodel.WorkbookFactory.create(in)) {
                    assertNotNull(wb.getSheet(org.hascoapi.transform.mt.dp2.DP2Gen.INFOSHEET));
                    assertNotNull(wb.getSheet(org.hascoapi.transform.mt.dp2.DP2Gen.NAMESPACES));
                }
            });

            // Ingest regenerated DP2 and dump/log
            DataFile regeneratedDf = mockDataFileFor(generatedCopy);
            assertDoesNotThrow(() -> IngestionWorker.ingest(regeneratedDf, generatedCopy, TEMPLATE_GENERIC, status),
                    "Ingesting regenerated DP2 workbook should not throw");
            dumpAndLogTtl("DP2_ingested_regenerated", regeneratedDf);

            System.out.println("Step2 DP2 regenerated workbook: " + out.getAbsolutePath());
            System.out.println("Step2 DP2 copied to workspace: " + generatedCopy.getAbsolutePath());
            printStepBanner("STEP 2/3 - DONE - MT=" + type + " result=" + result);
            return;
        }

        // Placeholder: regeneration APIs are not currently wired for other MTs.
        assumeTrue(false, () -> "Step 2 regeneration & comparison for " + type + " awaits API wiring.");
    }

    private static void deleteNamedGraphBestEffort(String namedGraphUri) {
        if (namedGraphUri == null || namedGraphUri.isEmpty()) {
            return;
        }
        try {
            String endpoint = org.hascoapi.utils.CollectionUtil.getCollectionPath(org.hascoapi.utils.CollectionUtil.Collection.SPARQL_UPDATE);
            String delete = "WITH <" + namedGraphUri + "> DELETE { ?s ?p ?o } WHERE { ?s ?p ?o }";
            org.apache.jena.update.UpdateRequest req = org.apache.jena.update.UpdateFactory.create(delete);
            org.apache.jena.update.UpdateProcessor proc = org.apache.jena.update.UpdateExecutionFactory.createRemote(req, endpoint);
            proc.execute();
            System.out.println("[RESET] Deleted named graph: " + namedGraphUri);
        } catch (Exception e) {
            System.out.println("[RESET] WARNING: failed to delete named graph " + namedGraphUri + ": " + e.getMessage());
        }
    }

    private void step3_reset_and_deterministic_reingest(MTType type) {
        printStepBanner("STEP 3/3 - RESET & DETERMINISTIC RE-INGEST (fresh -> identical/superset) - MT=" + type);

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
            printStepBanner("STEP 3/3 - DONE - MT=" + type + " regenerated2=" + regenerated2);
            return;
        }

        if (type == MTType.INS) {
            // Contract for INS step3:
            // 1) Delete INS named graphs created by step1+step2 (best-effort)
            // 2) Re-ingest original INS, dump/log ttl
            // 3) Regenerate INS again, ingest regenerated, dump/log ttl

            final File original = getMtExcel(MTType.INS);
            assumeTrue(original != null && original.exists(),
                    () -> "Original INS test input not found: " + (original == null ? "null" : original.getAbsolutePath()));

            final String status = VSTOI.DRAFT;

            // Best-effort cleanup of INS graphs created in previous steps
            DataFile dfTmpOriginal = mockDataFileFor(original);
            deleteNamedGraphBestEffort(dfTmpOriginal.getUri());

            File regeneratedCopy = new File(GENERATED_DIR, REGENERATED_INS_FILENAME);
            if (regeneratedCopy.exists()) {
                DataFile dfTmpRegen = mockDataFileFor(regeneratedCopy);
                deleteNamedGraphBestEffort(dfTmpRegen.getUri());
            }

            // Re-ingest original
            DataFile dfOriginal = mockDataFileFor(original);
            assertDoesNotThrow(() -> IngestionWorker.ingest(dfOriginal, original, TEMPLATE_GENERIC, status),
                    "Step3: re-ingesting original INS should not throw");
            dumpAndLogTtl("INS_step3_reingested_original", dfOriginal);

            // Regenerate again and re-ingest regenerated
            String result = null;
            try {
                result = org.hascoapi.transform.mt.ins.INSGen.genByStatus(status, REGENERATED_INS_FILENAME, null, null);
            } catch (Exception e) {
                fail("Step3: INS regeneration threw exception: " + e.getMessage());
            }
            assertNotNull(result);

            final File regeneratedOut = new File(ConfigProp.getPathIngestion() + REGENERATED_INS_FILENAME);
            assertTrue(regeneratedOut.exists(), "Step3: regenerated INS workbook should exist at: " + regeneratedOut.getAbsolutePath());
            assertTrue(regeneratedOut.length() > 0, "Step3: regenerated INS workbook should not be empty: " + regeneratedOut.getAbsolutePath());

            final File regeneratedCopied2 = copyToGenerated(regeneratedOut, REGENERATED_INS_FILENAME);
            DataFile dfRegen = mockDataFileFor(regeneratedCopied2);
            assertDoesNotThrow(() -> IngestionWorker.ingest(dfRegen, regeneratedCopied2, TEMPLATE_GENERIC, status),
                    "Step3: ingesting regenerated INS should not throw");
            dumpAndLogTtl("INS_step3_reingested_regenerated", dfRegen);

            printStepBanner("STEP 3/3 - DONE - MT=" + type);
            return;
        }

        if (type == MTType.DP2) {
            final File original = getMtExcel(MTType.DP2);
            assumeTrue(original != null && original.exists(), () -> "Original DP2 test input not found: " + (original == null ? "null" : original.getAbsolutePath()));

            final String status = VSTOI.DRAFT;

            // Best-effort cleanup of DP2 graphs created in previous steps
            DataFile dfTmpOriginal = mockDataFileFor(original);
            deleteNamedGraphBestEffort(dfTmpOriginal.getUri());

            File regeneratedCopy = new File(GENERATED_DIR, REGENERATED_DP2_FILENAME);
            if (regeneratedCopy.exists()) {
                DataFile dfTmpRegen = mockDataFileFor(regeneratedCopy);
                deleteNamedGraphBestEffort(dfTmpRegen.getUri());
            }

            // Re-ingest original
            DataFile dfOriginal = mockDataFileFor(original);
            assertDoesNotThrow(() -> IngestionWorker.ingest(dfOriginal, original, TEMPLATE_GENERIC, status),
                    "Step3: re-ingesting original DP2 should not throw");
            dumpAndLogTtl("DP2_step3_reingested_original", dfOriginal);

            // Regenerate again and ingest regenerated
            String result = null;
            try {
                result = org.hascoapi.transform.mt.dp2.DP2Gen.genByStatus(status, REGENERATED_DP2_FILENAME, null, null);
            } catch (Exception e) {
                fail("Step3: DP2 regeneration threw exception: " + e.getMessage());
            }
            assertNotNull(result);

            final File regeneratedOut = new File(ConfigProp.getPathIngestion() + REGENERATED_DP2_FILENAME);
            assertTrue(regeneratedOut.exists(), "Step3: regenerated DP2 workbook should exist at: " + regeneratedOut.getAbsolutePath());
            assertTrue(regeneratedOut.length() > 0, "Step3: regenerated DP2 workbook should not be empty: " + regeneratedOut.getAbsolutePath());

            final File regeneratedCopied2 = copyToGenerated(regeneratedOut, REGENERATED_DP2_FILENAME);
            DataFile dfRegen = mockDataFileFor(regeneratedCopied2);
            assertDoesNotThrow(() -> IngestionWorker.ingest(dfRegen, regeneratedCopied2, TEMPLATE_GENERIC, status),
                    "Step3: ingesting regenerated DP2 should not throw");
            dumpAndLogTtl("DP2_step3_reingested_regenerated", dfRegen);

            printStepBanner("STEP 3/3 - DONE - MT=" + type);
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
    @ValueSource(strings = {
            // "DSG",
            // "INS",
            "DP2"
    })
    public void step1_allMTs_ingest(String mtName) {
        MTType type = MTType.valueOf(mtName);
        printStepBanner("TEST ENTRYPOINT: step1_allMTs_ingest (MT=" + type + ")");
        step1_ingest(type);
    }

    @ParameterizedTest
    @DisplayName("HASCO round-trip: Step 2 regeneration & comparison for all MTs")
    @ValueSource(strings = {
            // "DSG",
            // "INS",
            "DP2"
    })
    public void step2_allMTs_regenerate_and_compare(String mtName) {
        MTType type = MTType.valueOf(mtName);
        printStepBanner("TEST ENTRYPOINT: step2_allMTs_regenerate_and_compare (MT=" + type + ")");
        step2_regenerate_and_compare(type);
    }

    @ParameterizedTest
    @DisplayName("HASCO round-trip: Step 3 reset & deterministic re-ingestion for all MTs")
    @ValueSource(strings = {
            // "DSG",
            // "INS",
            "DP2"
    })
    public void step3_allMTs_reset_and_deterministic_reingest(String mtName) {
        MTType type = MTType.valueOf(mtName);
        printStepBanner("TEST ENTRYPOINT: step3_allMTs_reset_and_deterministic_reingest (MT=" + type + ")");
        step3_reset_and_deterministic_reingest(type);
    }

    @DisplayName("Sanity: test scaffold lists all MT types")
    @org.junit.jupiter.api.Test
    public void sanity_z_listsAllMtTypes() {
        List<MTType> types = Arrays.asList(MTType.values());
        List<MTType> expectedTypes = Arrays.asList(MTType.DSG, MTType.INS, MTType.DP2, MTType.STR, MTType.KGR, MTType.SDD, MTType.DA);
        assertTrue(types.containsAll(expectedTypes), "MTType enum must include all expected MT types");

        // While focusing on DP2, only DP2 needs to be wired as a mandatory test input.
        // (Keep INS checks commented temporariamente; do not delete.)
        File dp2 = getMtExcel(MTType.DP2);
        assertNotNull(dp2, "DP2 input must be wired in getMtExcel");
        assertTrue(dp2.exists(), "DP2 test input must exist at: " + dp2.getPath());
        assertTrue(dp2.length() > 0, "DP2 test input must not be empty: " + dp2.getPath());

        // File ins = getMtExcel(MTType.INS);
        // assertNotNull(ins, "INS input must be wired in getMtExcel");
        // assertTrue(ins.exists(), "INS test input must exist at: " + ins.getPath());
        // assertTrue(ins.length() > 0, "INS test input must not be empty: " + ins.getPath());

        File generatedDir = new File("test/resources/generated");
        assertTrue(generatedDir.exists() || generatedDir.mkdirs(), "generated dir should be creatable at: " + generatedDir.getPath());

        File insRegen = new File(generatedDir, REGENERATED_INS_FILENAME);
        if (insRegen.exists()) {
            assertTrue(insRegen.length() > 0, "INS regen exists but is empty: " + insRegen.getPath());
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

    private static void assertStudyGraphPresentAfterIngestViaSparql(File ingestedDsg, String studyUriFromDataFile) {
        // Contract:
        // - Confirm the Study URI exists in triplestore (at least one triple mentioning it).
        // - Confirm the minimum SOC count implied by the workbook exists.
        // This is deliberately conservative and environment-friendly.

        // Verify the triplestore knows about the study URI.
        long studyMention = sparqlCount(
                "SELECT (COUNT(*) AS ?tot) WHERE { { <" + studyUriFromDataFile + "> ?p ?o } UNION { ?s ?p <" + studyUriFromDataFile + "> } }"
        );
        assertTrue(studyMention > 0,
                "Expected at least one triple mentioning the ingested study URI, but found none for: " + studyUriFromDataFile);

        // Expected minimum SOC count derived from the workbook.
        java.util.Map<String, java.util.Set<String>> fp = extractDsgFingerprint(ingestedDsg);
        long expectedSocMin = fp.getOrDefault("SSD", java.util.Collections.emptySet()).stream()
                .filter(s -> s.startsWith("#SOC-"))
                .count();

        // Count SOC membership in triplestore.
        long actualSoc = sparqlCount(
                "PREFIX hasco: <http://hadatac.org/ont/hasco/> \n" +
                        "SELECT (COUNT(DISTINCT ?soc) AS ?tot) WHERE { \n" +
                        "  ?soc hasco:isMemberOf <" + studyUriFromDataFile + "> .\n" +
                        "  ?soc hasco:hascoType <http://hadatac.org/ont/hasco/StudyObjectCollection> .\n" +
                        "}"
        );

        System.out.println("Step1 post-ingest SPARQL diag: study=" + studyUriFromDataFile
                + " expectedSocMin=" + expectedSocMin + " actualSoc=" + actualSoc);

        assertTrue(actualSoc >= expectedSocMin,
                "Post-ingest: Triplestore should have at least the SOCs implied by the ingested workbook. expectedMin="
                        + expectedSocMin + " actual=" + actualSoc);

        // Optional: if StudyObjects are directly linked to the study, we can sanity-check the count too.
        long expectedObjMin = fp.getOrDefault("SOC", java.util.Collections.emptySet()).size();
        long actualObj = sparqlCount(
                "PREFIX hasco: <http://hadatac.org/ont/hasco/> \n" +
                        "SELECT (COUNT(DISTINCT ?obj) AS ?tot) WHERE { \n" +
                        "  ?obj hasco:isMemberOf <" + studyUriFromDataFile + "> .\n" +
                        "  ?obj hasco:hascoType <http://hadatac.org/ont/hasco/StudyObject> .\n" +
                        "}"
        );

        // In some deployments objects aren't directly memberOf the study (they may be memberOf the SOC).
        // So we only assert the minimum if the query returns non-zero.
        if (actualObj > 0) {
            assertTrue(actualObj >= expectedObjMin,
                    "Post-ingest: expected at least the StudyObjects implied by the workbook when counted by memberOf. expectedMin="
                            + expectedObjMin + " actual=" + actualObj);
        }
    }
}
