package org.hascoapi.tests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.File;

import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.ingestion.IngestionWorker;
import org.hascoapi.utils.IngestionLogger;
import org.junit.jupiter.api.Test;

/**
 * IngestionWorkerDSGTest
 *
 * This test class verifies the full ingestion flow for DSG Excel files by calling
 * {@link org.hascoapi.ingestion.IngestionWorker#ingest(DataFile, File, String, String)}
 * with a real DSG test workbook.
 *
 * Covered scenarios:
 * - ingest_withNonDSGFile_doesNotCreateChain: unknown file names must log GBL_00003 / GBL_00005 and stop.
 * - ingest_withRealDSGStdTestFile_runsWithoutThrowing: ingestion of DSG-STD-test.xlsx must execute end-to-end
 *   without throwing exceptions, exercising NameSpaceGenerator, StudyGenerator, SSDGenerator, etc.
 */
public class IngestionWorkerDSGTest {

    private File getTestDsgFile() {
        return new File("test/resources/dsg/DSG-STD-test.xlsx");
    }

    @Test
    public void ingest_withNonDSGFile_doesNotCreateChain() {
        System.out.println("[IngestionWorkerDSGTest] Running ingest_withNonDSGFile_doesNotCreateChain");
        File fakeFile = new File("test/resources/dsg/UNKNOWN-file.xlsx");
        DataFile dataFile = mock(DataFile.class);
        IngestionLogger logger = mock(IngestionLogger.class);
        when(dataFile.getFilename()).thenReturn("UNKNOWN-file.xlsx");
        when(dataFile.getLogger()).thenReturn(logger);

        assertDoesNotThrow(() -> {
            IngestionWorker.ingest(dataFile, fakeFile, null, "");
        });
        // We expect the logger to capture invalid extension or missing InfoSheet errors
        verify(logger, atLeastOnce()).printExceptionById("GBL_00003");
        System.out.println("[IngestionWorkerDSGTest] Finished ingest_withNonDSGFile_doesNotCreateChain");
    }

    @Test
    public void ingest_withRealDSGStdTestFile_runsWithoutThrowing() {
        System.out.println("[IngestionWorkerDSGTest] Running ingest_withRealDSGStdTestFile_runsWithoutThrowing");
        File dsgFile = getTestDsgFile();
        assertTrue(dsgFile.exists(), "Test DSG Excel file must exist: " + dsgFile.getAbsolutePath());

        DataFile dataFile = mock(DataFile.class);
        IngestionLogger logger = mock(IngestionLogger.class);
        when(dataFile.getFilename()).thenReturn("DSG-STD-test.xlsx");
        when(dataFile.getLogger()).thenReturn(logger);
        when(dataFile.getUri()).thenReturn("http://example.org/DF-TEST");

        assertDoesNotThrow(() -> {
            IngestionWorker.ingest(dataFile, dsgFile, null, "");
        }, "IngestionWorker.ingest should run full DSG ingestion without throwing for DSG-STD-test.xlsx");
        System.out.println("[IngestionWorkerDSGTest] Finished ingest_withRealDSGStdTestFile_runsWithoutThrowing");
    }
}
