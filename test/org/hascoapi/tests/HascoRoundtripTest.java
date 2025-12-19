package org.hascoapi.tests;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.Study;
import org.hascoapi.ingestion.IngestionWorker;
import org.hascoapi.utils.IngestionLogger;
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

        // Backing storage for RecordFile set by IngestionWorker
        java.util.concurrent.atomic.AtomicReference<org.hascoapi.ingestion.RecordFile> rfRef =
                new java.util.concurrent.atomic.AtomicReference<>();
        // getRecordFile returns whatever was set
        when(dataFile.getRecordFile()).thenAnswer(inv -> rfRef.get());
        // setRecordFile stores into rfRef
        doAnswer(inv -> { rfRef.set(inv.getArgument(0)); return null; })
                .when(dataFile).setRecordFile(any(org.hascoapi.ingestion.RecordFile.class));

        // Initialize with a valid InfoSheet so early generators can read
        rfRef.set(new org.hascoapi.ingestion.SpreadsheetRecordFile(
                excelFile, excelFile.getName(), "InfoSheet"));
        return dataFile;
    }

    private void step1_ingest(MTType type) {
        File excel = getMtExcel(type);
        assumeTrue(excel != null && excel.exists(),
                () -> "Test input not found for " + type + ": " + (excel == null ? "null" : excel.getAbsolutePath()));

        // For DSG, we have a known ingestion path through IngestionWorker.
        if (type == MTType.DSG) {
            DataFile df = mockDataFileFor(excel);
            // Use the generic template so all STD/SSD mappings are available
            final String templatePath = "conf/template.generic.conf";
            assertDoesNotThrow(() -> IngestionWorker.ingest(df, excel, templatePath, ""),
                    () -> "Step 1 ingestion should complete without exceptions for " + type);
            return;
        }

        // For other MTs, keep the step as a placeholder until the specific ingestion flow is wired.
        // This is a controlled skip marking work-in-progress.
        assumeTrue(false, () -> "Step 1 ingestion for " + type + " is not yet implemented in tests.");
    }

    private void step2_regenerate_and_compare(MTType type) {
        // Placeholder: regeneration APIs are not currently discovered in the repo (no hits for 'regenerate').
        // We mark this step as skipped until the API surface is available.
        assumeTrue(false, () -> "Step 2 regeneration & comparison for " + type + " awaits API wiring.");
    }

    private void step3_reset_and_deterministic_reingest(MTType type) {
        // Placeholder: requires a reset/erase operation for the MT's graphs/named resources.
        // Without a discovered reset API, we skip deterministically.
        assumeTrue(false, () -> "Step 3 reset & deterministic re-ingestion for " + type + " awaits reset API.");
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
    public void sanity_listsAllMtTypes() {
        List<MTType> types = Arrays.asList(MTType.values());
        assertTrue(types.containsAll(Arrays.asList(MTType.DSG, MTType.INS, MTType.DP2, MTType.STR, MTType.KGR, MTType.SDD, MTType.DA)));
    }
}
