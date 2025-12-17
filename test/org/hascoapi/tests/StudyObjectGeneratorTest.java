package org.hascoapi.tests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.*;

import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.StudyObject;
import org.hascoapi.ingestion.Record;
import org.hascoapi.ingestion.StudyObjectGenerator;
import org.hascoapi.utils.IngestionLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * StudyObjectGeneratorTest
 *
 * This test class validates how DSG SSD rows are transformed into StudyObject instances
 * by {@link org.hascoapi.ingestion.StudyObjectGenerator}.
 *
 * Covered scenarios:
 * - createStudyObject_returnsNull_when_originalIdEmpty: rows without originalID must be ignored.
 * - createStudyObject_generatesScopesAndUris: a valid row must create a StudyObject with URI and at least one scope.
 * - createStudyObject_setsScopeAndTimeAndSpace_whenIdsPresent: when all scope/time/space IDs are present, corresponding
 *   URI lists must be populated.
 * - createStudyObject_handlesMissingScopeColumnsGracefully: if scope columns are missing from the DSG, a StudyObject is still
 *   created but the scope/time/space lists must be empty.
 */
public class StudyObjectGeneratorTest {

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
        public String getSheetName() {
            return "";
        }

    }

    private static class SimpleDataFile extends DataFile {
        private final String filename;
        private final String sirEmail;
        private final IngestionLogger logger;
        private final org.hascoapi.ingestion.RecordFile recordFile = new FakeRecordFile();

        SimpleDataFile(String filename, String sirEmail, IngestionLogger logger) {
            this.filename = filename;
            this.sirEmail = sirEmail;
            this.logger = logger;
        }

        @Override
        public String getFilename() { return filename; }

        @Override
        public String getHasSIRManagerEmail() { return sirEmail; }

        @Override
        public IngestionLogger getLogger() { return logger; }

        @Override
        public org.hascoapi.ingestion.RecordFile getRecordFile() { return recordFile; }
    }

    private DataFile dataFile;

    @BeforeEach
    public void setup() {
        IngestionLogger logger = mock(IngestionLogger.class);
        dataFile = new SimpleDataFile("DSG-STD-test.xlsx", "sir@example.org", logger);
    }

    @Test
    public void createStudyObject_returnsNull_when_originalIdEmpty() throws Exception {
        System.out.println("[StudyObjectGeneratorTest] Running createStudyObject_returnsNull_when_originalIdEmpty");
        List<String> listContent = Arrays.asList("socRef", "hasco:SubjectGroup", "domain", "time", "space", "roleLabel", "socRef", "grounding");
        Map<String, List<String>> mapContent = new HashMap<>();
        Map<String, String> mapReferences = new HashMap<>();
        StudyObjectGenerator gen = new StudyObjectGenerator(dataFile, listContent, mapContent, mapReferences, "http://kg:ST-1", "S1", "ns");

        Record rec = mock(Record.class);
        when(rec.getValueByColumnName("originalID")).thenReturn("");

        StudyObject obj = gen.createStudyObject(rec);
        assertNull(obj);
        System.out.println("[StudyObjectGeneratorTest] Finished createStudyObject_returnsNull_when_originalIdEmpty");
    }

    @Test
    public void createStudyObject_generatesScopesAndUris() throws Exception {
        System.out.println("[StudyObjectGeneratorTest] Running createStudyObject_generatesScopesAndUris");
        List<String> listContent = Arrays.asList("socRef", "hasco:SubjectGroup", "domainKey", "timeKey", "spaceKey", "roleLabel", "socRef", "grounding");
        Map<String, List<String>> mapContent = new HashMap<>();
        mapContent.put("domainKey", Arrays.asList("sheetName", "hasco:SubjectGroup"));
        mapContent.put("timeKey", Arrays.asList("timeSheet", "hasco:TimeCollection"));
        mapContent.put("spaceKey", Arrays.asList("spaceSheet", "hasco:SpaceCollection"));
        Map<String, String> mapReferences = new HashMap<>();
        mapReferences.put("domainKey", "domainRef");
        mapReferences.put("timeKey", "timeRef");
        mapReferences.put("spaceKey", "spaceRef");

        StudyObjectGenerator gen = new StudyObjectGenerator(dataFile, listContent, mapContent, mapReferences, "http://kg:ST-1", "S1", "ns");

        Map<String,String> mapCol = new HashMap<>();
        mapCol.put("originalID", "originalID");
        mapCol.put("scopeID", "scopeID");
        mapCol.put("timeScopeID", "timeScopeID");
        mapCol.put("spaceScopeID", "spaceScopeID");
        java.lang.reflect.Field f = StudyObjectGenerator.class.getSuperclass().getDeclaredField("mapCol");
        f.setAccessible(true);
        f.set(gen, mapCol);

        Record rec = mock(Record.class);
        when(rec.getValueByColumnName("originalID")).thenReturn("obj1");
        when(rec.getValueByColumnName("scopeID")).thenReturn("10");
        when(rec.getValueByColumnName("timeScopeID")).thenReturn("http://example.org/time/1");
        when(rec.getValueByColumnName("spaceScopeID")).thenReturn("20");

        StudyObject obj = gen.createStudyObject(rec);
        assertNotNull(obj);
        assertNotNull(obj.getUri());
        assertFalse(obj.getScopeUris().isEmpty() && obj.getTimeScopeUris().isEmpty() && obj.getSpaceScopeUris().isEmpty());
        System.out.println("[StudyObjectGeneratorTest] Finished createStudyObject_generatesScopesAndUris");
    }

    @Test
    public void createStudyObject_setsScopeAndTimeAndSpace_whenIdsPresent() throws Exception {
        System.out.println("[StudyObjectGeneratorTest] Running createStudyObject_setsScopeAndTimeAndSpace_whenIdsPresent");
        List<String> listContent = Arrays.asList("socRef", "hasco:SubjectGroup", "domainKey", "timeKey", "spaceKey", "roleLabel", "socRef", "grounding");
        Map<String, List<String>> mapContent = new HashMap<>();
        mapContent.put("domainKey", Arrays.asList("sheetName", "hasco:SubjectGroup"));
        mapContent.put("timeKey", Arrays.asList("timeSheet", "hasco:TimeCollection"));
        mapContent.put("spaceKey", Arrays.asList("spaceSheet", "hasco:SpaceCollection"));
        Map<String, String> mapReferences = new HashMap<>();
        mapReferences.put("domainKey", "domainRef");
        mapReferences.put("timeKey", "timeRef");
        mapReferences.put("spaceKey", "spaceRef");

        StudyObjectGenerator gen = new StudyObjectGenerator(dataFile, listContent, mapContent, mapReferences, "http://kg:ST-1", "S1", "ns");

        Map<String,String> mapCol = new HashMap<>();
        mapCol.put("originalID", "originalID");
        mapCol.put("scopeID", "scopeID");
        mapCol.put("timeScopeID", "timeScopeID");
        mapCol.put("spaceScopeID", "spaceScopeID");
        java.lang.reflect.Field f = StudyObjectGenerator.class.getSuperclass().getDeclaredField("mapCol");
        f.setAccessible(true);
        f.set(gen, mapCol);

        Record rec = mock(Record.class);
        when(rec.getValueByColumnName("originalID")).thenReturn("obj1");
        when(rec.getValueByColumnName("scopeID")).thenReturn("10");
        when(rec.getValueByColumnName("timeScopeID")).thenReturn("11");
        when(rec.getValueByColumnName("spaceScopeID")).thenReturn("12");

        StudyObject obj = gen.createStudyObject(rec);
        assertNotNull(obj);
        assertEquals("obj1", obj.getOriginalId());
        assertFalse(obj.getScopeUris().isEmpty());
        assertFalse(obj.getTimeScopeUris().isEmpty());
        assertFalse(obj.getSpaceScopeUris().isEmpty());
        System.out.println("[StudyObjectGeneratorTest] Finished createStudyObject_setsScopeAndTimeAndSpace_whenIdsPresent");
    }

    @Test
    public void createStudyObject_handlesMissingScopeColumnsGracefully() throws Exception {
        System.out.println("[StudyObjectGeneratorTest] Running createStudyObject_handlesMissingScopeColumnsGracefully");
        List<String> listContent = Arrays.asList("socRef", "hasco:SubjectGroup", "domainKey", "timeKey", "spaceKey", "roleLabel", "socRef", "grounding");
        Map<String, List<String>> mapContent = new HashMap<>();
        Map<String, String> mapReferences = new HashMap<>();

        StudyObjectGenerator gen = new StudyObjectGenerator(dataFile, listContent, mapContent, mapReferences, "http://kg:ST-1", "S1", "ns");

        Map<String,String> mapCol = new HashMap<>();
        mapCol.put("originalID", "originalID");
        // intentionally not putting scope/time/space columns
        java.lang.reflect.Field f = StudyObjectGenerator.class.getSuperclass().getDeclaredField("mapCol");
        f.setAccessible(true);
        f.set(gen, mapCol);

        Record rec = mock(Record.class);
        when(rec.getValueByColumnName("originalID")).thenReturn("obj1");

        StudyObject obj = gen.createStudyObject(rec);
        assertNotNull(obj);
        assertTrue(obj.getScopeUris() == null || obj.getScopeUris().isEmpty());
        assertTrue(obj.getTimeScopeUris() == null || obj.getTimeScopeUris().isEmpty());
        assertTrue(obj.getSpaceScopeUris() == null || obj.getSpaceScopeUris().isEmpty());
        System.out.println("[StudyObjectGeneratorTest] Finished createStudyObject_handlesMissingScopeColumnsGracefully");
    }
}
