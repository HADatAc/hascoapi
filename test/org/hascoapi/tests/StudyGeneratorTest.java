package org.hascoapi.tests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.ingestion.Record;
import org.hascoapi.ingestion.StudyGenerator;
import org.hascoapi.utils.IngestionLogger;
import org.junit.jupiter.api.Test;

/**
 * StudyGeneratorTest
 *
 * This test class verifies the DSG STD (Study sheet) ingestion behavior implemented by {@link org.hascoapi.ingestion.StudyGenerator}.
 * It focuses on how rows are created from DSG metadata and how ingestion errors are logged.
 *
 * Covered scenarios:
 * - createRow_returnsNull_andLogs_when_missingUri: missing study URI must log GBL_00028 and skip the row.
 * - createRow_successful_with_validUriAndFields: valid input must generate a complete STD row with URI, title and data file.
 * - createRow_returnsNull_andLogs_when_missingId: missing study ID must log GBL_00029 and skip the row.
 * - createRow_returnsNull_andLogs_when_missingTitle: missing title must log GBL_00030 and skip the row.
 * - createRow_returnsNull_when_recordEmpty: empty DSG line must be ignored without throwing exceptions.
 * - createRow_addsExternalSource_whenPresent: external source column must be mapped to hasco:hasExternalSource.
 * - createRow_logsMissingPiAndInstitution_afterPreprocessuris: when PI and Institution URIs are missing after preprocessuris,
 *   the row is still created but GBL_00032 (PI) and GBL_00033 (Institution) must be logged.
 */
public class StudyGeneratorTest {

    private static class TestLogger extends IngestionLogger {
        public String lastErrorId;

        public TestLogger() {
            super((DataFile) null);
        }

        @Override
        public void printExceptionById(String id) {
            lastErrorId = id;
        }
    }

    private static class FakeRecordFile implements org.hascoapi.ingestion.RecordFile {
        @Override
        public java.util.List<org.hascoapi.ingestion.Record> getRecords() {
            return java.util.Collections.emptyList();
        }

        @Override
        public List<String> getHeaders() {
            return List.of();
        }

        @Override
        public File getFile() {
            return null;
        }

        @Override
        public String getStorageFileName() {
            return "";
        }

        @Override
        public boolean isValid() { return true; }

        @Override
        public int getNumberOfSheets() {
            return 0;
        }

        @Override
        public int getNumberOfRows() {
            return 0;
        }


        @Override
        public String getSheetName() { return "InfoSheet"; }
    }

    private static class SimpleDataFile extends DataFile {
        private final String filename;
        private final String uri;
        private final IngestionLogger logger;
        private final String sirManagerEmail;
        private final org.hascoapi.ingestion.RecordFile recordFile = new FakeRecordFile();

        SimpleDataFile(String filename, String uri, IngestionLogger logger, String sirManagerEmail) {
            this.filename = filename;
            this.uri = uri;
            this.logger = logger;
            this.sirManagerEmail = sirManagerEmail;
        }

        @Override
        public String getFilename() { return filename; }

        @Override
        public String getUri() { return uri; }

        @Override
        public IngestionLogger getLogger() { return logger; }

        @Override
        public String getHasSIRManagerEmail() { return sirManagerEmail; }

        @Override
        public org.hascoapi.ingestion.RecordFile getRecordFile() {
            return recordFile;
        }
    }

    private static class TestRecord implements Record {
        private final Map<String,String> values = new HashMap<>();

        public TestRecord with(String column, String value) {
            values.put(column, value);
            return this;
        }

        @Override
        public int size() {
            return values.size();
        }

        @Override
        public String getValueByColumnName(String name) {
            return values.get(name);
        }

        @Override
        public String getValueByColumnIndex(int index) {
            // Not used in these tests
            return null;
        }
    }

    @Test
    public void createRow_returnsNull_andLogs_when_missingUri() throws Exception {
        System.out.println("[StudyGeneratorTest] Running createRow_returnsNull_andLogs_when_missingUri");
        TestLogger logger = new TestLogger();
        DataFile dataFile = new SimpleDataFile("DSG-test.xlsx", "http://example.org/DF-1", logger, "sir@example.org");
        StudyGenerator gen = new StudyGenerator(dataFile, "", null);

        Map<String, String> mapCol = new HashMap<>();
        mapCol.put("studyID", "studyID");
        mapCol.put("studyTitle", "studyTitle");
        mapCol.put("studyAims", "studyAims");
        mapCol.put("studySignificance", "studySignificance");
        java.lang.reflect.Field f = StudyGenerator.class.getSuperclass().getDeclaredField("mapCol");
        f.setAccessible(true);
        f.set(gen, mapCol);

        Record rec = new TestRecord().with("studyID", "S1");

        Map<String, Object> row = gen.createRow(rec, 1);
        assertNull(row, "Row should be null when URI is missing");
        assertEquals("GBL_00028", logger.lastErrorId, "Error ID for missing URI");
        System.out.println("[StudyGeneratorTest] Finished createRow_returnsNull_andLogs_when_missingUri");
    }

    @Test
    public void createRow_successful_with_validUriAndFields() throws Exception {
        System.out.println("[StudyGeneratorTest] Running createRow_successful_with_validUriAndFields");
        TestLogger logger = new TestLogger();
        DataFile dataFile = new SimpleDataFile("DSG-test.xlsx", "http://example.org/DF-1", logger, "sir@example.org");
        String studyUri = "http://example.org/ST-1";
        StudyGenerator gen = new StudyGenerator(dataFile, studyUri, null);

        Map<String, String> mapCol = new HashMap<>();
        mapCol.put("studyID", "studyID");
        mapCol.put("studyTitle", "studyTitle");
        mapCol.put("studyAims", "studyAims");
        mapCol.put("studySignificance", "studySignificance");
        java.lang.reflect.Field f = StudyGenerator.class.getSuperclass().getDeclaredField("mapCol");
        f.setAccessible(true);
        f.set(gen, mapCol);

        TestRecord rec = new TestRecord()
                .with("studyID", "S1")
                .with("studyTitle", "Study Title")
                .with("studyAims", "Aims")
                .with("studySignificance", "Significance");

        Map<String, Object> row = gen.createRow(rec, 1);
        assertNotNull(row, "Row should not be null for valid URI and fields");
        assertEquals(studyUri, row.get("hasURI"), "Study URI mismatch");
        assertEquals("Study Title", row.get("hasco:hasTitle"), "Study title mismatch");
        assertEquals("http://example.org/DF-1", row.get("hasco:hasDataFile"), "Data file URI mismatch");
        System.out.println("[StudyGeneratorTest] Finished createRow_successful_with_validUriAndFields");
    }

    @Test
    public void createRow_returnsNull_andLogs_when_missingId() throws Exception {
        System.out.println("[StudyGeneratorTest] Running createRow_returnsNull_andLogs_when_missingId");
        TestLogger logger = new TestLogger();
        DataFile dataFile = new SimpleDataFile("DSG-test.xlsx", "http://example.org/DF-1", logger, "sir@example.org");
        StudyGenerator gen = new StudyGenerator(dataFile, "http://example.org/ST-1", null);

        Map<String, String> mapCol = new HashMap<>();
        mapCol.put("studyID", "studyID");
        mapCol.put("studyTitle", "studyTitle");
        mapCol.put("studyAims", "studyAims");
        mapCol.put("studySignificance", "studySignificance");
        java.lang.reflect.Field f = StudyGenerator.class.getSuperclass().getDeclaredField("mapCol");
        f.setAccessible(true);
        f.set(gen, mapCol);

        // ID vazio
        TestRecord rec = new TestRecord()
                .with("studyID", "")
                .with("studyTitle", "Study Title");

        Map<String, Object> row = gen.createRow(rec, 1);
        assertNull(row, "Row should be null when study ID is missing");
        assertEquals("GBL_00029", logger.lastErrorId, "Error ID for missing study ID");
        System.out.println("[StudyGeneratorTest] Finished createRow_returnsNull_andLogs_when_missingId");
    }

    @Test
    public void createRow_returnsNull_andLogs_when_missingTitle() throws Exception {
        System.out.println("[StudyGeneratorTest] Running createRow_returnsNull_andLogs_when_missingTitle");
        TestLogger logger = new TestLogger();
        DataFile dataFile = new SimpleDataFile("DSG-test.xlsx", "http://example.org/DF-1", logger, "sir@example.org");
        StudyGenerator gen = new StudyGenerator(dataFile, "http://example.org/ST-1", null);

        Map<String, String> mapCol = new HashMap<>();
        mapCol.put("studyID", "studyID");
        mapCol.put("studyTitle", "studyTitle");
        mapCol.put("studyAims", "studyAims");
        mapCol.put("studySignificance", "studySignificance");
        java.lang.reflect.Field f = StudyGenerator.class.getSuperclass().getDeclaredField("mapCol");
        f.setAccessible(true);
        f.set(gen, mapCol);

        TestRecord rec = new TestRecord()
                .with("studyID", "S1")
                .with("studyTitle", "");

        Map<String, Object> row = gen.createRow(rec, 1);
        assertNull(row, "Row should be null when study title is missing");
        assertEquals("GBL_00030", logger.lastErrorId, "Error ID for missing study title");
        System.out.println("[StudyGeneratorTest] Finished createRow_returnsNull_andLogs_when_missingTitle");
    }

    @Test
    public void createRow_returnsNull_when_recordEmpty() throws Exception {
        System.out.println("[StudyGeneratorTest] Running createRow_returnsNull_when_recordEmpty");
        TestLogger logger = new TestLogger();
        DataFile dataFile = new SimpleDataFile("DSG-test.xlsx", "http://example.org/DF-1", logger, "sir@example.org");
        StudyGenerator gen = new StudyGenerator(dataFile, "http://example.org/ST-1", null);

        Map<String, String> mapCol = new HashMap<>();
        mapCol.put("studyID", "studyID");
        java.lang.reflect.Field f = StudyGenerator.class.getSuperclass().getDeclaredField("mapCol");
        f.setAccessible(true);
        f.set(gen, mapCol);

        TestRecord rec = new TestRecord(); // size == 0
        Map<String, Object> row = gen.createRow(rec, 1);
        assertNull(row, "Row should be null when record is empty");
        // O logger não é chamado neste caso (bug conhecido), apenas garantimos que não lança exceção
        System.out.println("[StudyGeneratorTest] Finished createRow_returnsNull_when_recordEmpty");
    }

    @Test
    public void createRow_addsExternalSource_whenPresent() throws Exception {
        System.out.println("[StudyGeneratorTest] Running createRow_addsExternalSource_whenPresent");
        TestLogger logger = new TestLogger();
        DataFile dataFile = new SimpleDataFile("DSG-test.xlsx", "http://example.org/DF-1", logger, "sir@example.org");
        String studyUri = "http://example.org/ST-1";
        StudyGenerator gen = new StudyGenerator(dataFile, studyUri, null);

        Map<String, String> mapCol = new HashMap<>();
        mapCol.put("studyID", "studyID");
        mapCol.put("studyTitle", "studyTitle");
        mapCol.put("studyAims", "studyAims");
        mapCol.put("studySignificance", "studySignificance");
        mapCol.put("externalSource", "externalSource");
        java.lang.reflect.Field f = StudyGenerator.class.getSuperclass().getDeclaredField("mapCol");
        f.setAccessible(true);
        f.set(gen, mapCol);

        TestRecord rec = new TestRecord()
                .with("studyID", "S1")
                .with("studyTitle", "Study Title")
                .with("studyAims", "Aims")
                .with("studySignificance", "Significance")
                .with("externalSource", "ext-src");

        Map<String, Object> row = gen.createRow(rec, 1);
        assertNotNull(row, "Row should not be null when external source is present");
        assertEquals("ext-src", row.get("hasco:hasExternalSource"), "External source mismatch");
        System.out.println("[StudyGeneratorTest] Finished createRow_addsExternalSource_whenPresent");
    }

    @Test
    public void createRow_logsMissingPiAndInstitution_afterPreprocessuris() throws Exception {
        System.out.println("[StudyGeneratorTest] Running createRow_logsMissingPiAndInstitution_afterPreprocessuris");
        // Usa Mockito para verificar chamadas de logger
        DataFile dataFile = mock(DataFile.class);
        IngestionLogger mockLogger = mock(IngestionLogger.class);
        when(dataFile.getFilename()).thenReturn("DSG-test.xlsx");
        when(dataFile.getUri()).thenReturn("http://example.org/DF-1");
        when(dataFile.getLogger()).thenReturn(mockLogger);
        when(dataFile.getHasSIRManagerEmail()).thenReturn("sir@example.org");

        StudyGenerator gen = new StudyGenerator(dataFile, "http://example.org/ST-1", null);

        Map<String, String> mapCol = new HashMap<>();
        mapCol.put("studyID", "studyID");
        mapCol.put("studyTitle", "studyTitle");
        mapCol.put("studyAims", "studyAims");
        mapCol.put("studySignificance", "studySignificance");
        java.lang.reflect.Field f = StudyGenerator.class.getSuperclass().getDeclaredField("mapCol");
        f.setAccessible(true);
        f.set(gen, mapCol);

        // preprocessuris com URIs vazias
        Map<String,String> uris = new HashMap<>();
        uris.put("piUri", "");
        uris.put("piInstitutionUri", "");
        uris.put("cpi1Uri", "");
        uris.put("cpi2Uri", "");
        uris.put("contactUri", "");
        gen.preprocessuris(uris);

        TestRecord rec = new TestRecord()
                .with("studyID", "S1")
                .with("studyTitle", "Study Title")
                .with("studyAims", "Aims")
                .with("studySignificance", "Significance");

        Map<String, Object> row = gen.createRow(rec, 1);
        assertNotNull(row, "Row should not be null even if PI and Institution URIs are missing");
        // Deve ter logado falta de PI e de instituição
        verify(mockLogger).printExceptionById("GBL_00032");
        verify(mockLogger).printExceptionById("GBL_00033");
        System.out.println("[StudyGeneratorTest] Finished createRow_logsMissingPiAndInstitution_afterPreprocessuris");
    }
}
