package org.hascoapi.tests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.*;

import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.StudyObjectCollection;
import org.hascoapi.ingestion.Record;
import org.hascoapi.ingestion.SSDGenerator;
import org.hascoapi.utils.IngestionLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * SSDGeneratorTest
 *
 * This test class checks how the SSD (Study Subject Design) sheet of the DSG is ingested by
 * {@link org.hascoapi.ingestion.SSDGenerator} and converted into StudyObjectCollection instances.
 *
 * Covered scenarios:
 * - createObjectCollection_returnsNull_whenEmptyRecord: empty SSD rows (size == 0) must be ignored.
 * - createObjectCollection_returnsNull_whenTypeAndSocReferenceEmpty: rows with both type and SOC reference empty
 *   are considered invalid and ignored.
 * - createObjectCollection_returnsNull_whenSocReferenceMissing: rows with type but missing SOC reference must be ignored
 *   and flagged by the ingestion logger (SSD_00002).
 * - createObjectCollection_createsSoc_whenFieldsValid: a valid SSD row must produce a StudyObjectCollection with label,
 *   membership to the study, SIR manager email and non-empty scope/time/space/group lists.
 */
public class SSDGeneratorTest {

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
        public String getSheetName() { return "SSD"; }
    }

    private static class TestDataFile extends DataFile {
        private final String studyUri;
        private final String sirEmail;
        private final IngestionLogger logger;
        private final org.hascoapi.ingestion.RecordFile recordFile = new FakeRecordFile();

        TestDataFile(String studyUri, String sirEmail, IngestionLogger logger) {
            this.studyUri = studyUri;
            this.sirEmail = sirEmail;
            this.logger = logger;
        }

        @Override
        public String getStudyUri() {
            return studyUri;
        }

        @Override
        public String getHasSIRManagerEmail() {
            return sirEmail;
        }

        @Override
        public IngestionLogger getLogger() {
            return logger;
        }

        @Override
        public org.hascoapi.ingestion.RecordFile getRecordFile() {
            return recordFile;
        }
    }

    private DataFile dataFile;

    @BeforeEach
    public void setup() {
        IngestionLogger logger = mock(IngestionLogger.class);
        dataFile = new TestDataFile("http://example.org/ST-1", "sir@example.org", logger);
    }

    private SSDGenerator newGenerator(String namespace) {
        SSDGenerator gen = new SSDGenerator(dataFile, namespace);
        gen.initMapping();
        return gen;
    }

    private Record recordWith(String uri, String type, String socRef, String scope, String space, String time, String groups) {
        Record rec = mock(Record.class);
        when(rec.size()).thenReturn(1);
        when(rec.getValueByColumnName("hasURI")).thenReturn(uri);
        when(rec.getValueByColumnName("type")).thenReturn(type);
        when(rec.getValueByColumnName("hasSOCReference")).thenReturn(socRef);
        when(rec.getValueByColumnName("hasScope")).thenReturn(scope);
        when(rec.getValueByColumnName("hasSpaceScope")).thenReturn(space);
        when(rec.getValueByColumnName("hasTimeScope")).thenReturn(time);
        when(rec.getValueByColumnName("hasGroup")).thenReturn(groups);
        when(rec.getValueByColumnName("label")).thenReturn("SOC Label");
        return rec;
    }

    @Test
    public void createObjectCollection_returnsNull_whenEmptyRecord() throws Exception {
        System.out.println("[SSDGeneratorTest] Running createObjectCollection_returnsNull_whenEmptyRecord");
        SSDGenerator gen = newGenerator("ns");
        Record rec = mock(Record.class);
        when(rec.size()).thenReturn(0);
        StudyObjectCollection soc = gen.createObjectCollection(rec);
        assertNull(soc);
        System.out.println("[SSDGeneratorTest] Finished createObjectCollection_returnsNull_whenEmptyRecord");
    }

    @Test
    public void createObjectCollection_returnsNull_whenTypeAndSocReferenceEmpty() throws Exception {
        System.out.println("[SSDGeneratorTest] Running createObjectCollection_returnsNull_whenTypeAndSocReferenceEmpty");
        SSDGenerator gen = newGenerator("ns");
        Record rec = recordWith("uri1", "", "", "", "", "", "");
        StudyObjectCollection soc = gen.createObjectCollection(rec);
        assertNull(soc);
        System.out.println("[SSDGeneratorTest] Finished createObjectCollection_returnsNull_whenTypeAndSocReferenceEmpty");
    }

    @Test
    public void createObjectCollection_returnsNull_whenSocReferenceMissing() throws Exception {
        System.out.println("[SSDGeneratorTest] Running createObjectCollection_returnsNull_whenSocReferenceMissing");
        SSDGenerator gen = newGenerator("ns");
        Record rec = recordWith("uri1", "hasco:StudyObjectCollection", "", "", "", "", "");
        StudyObjectCollection soc = gen.createObjectCollection(rec);
        assertNull(soc);
        System.out.println("[SSDGeneratorTest] Finished createObjectCollection_returnsNull_whenSocReferenceMissing");
    }

    @Test
    public void createObjectCollection_createsSoc_whenFieldsValid() throws Exception {
        System.out.println("[SSDGeneratorTest] Running createObjectCollection_createsSoc_whenFieldsValid");
        SSDGenerator gen = newGenerator("ns");
        Record rec = recordWith("URI1", "hasco:StudyObjectCollection", "REF1", "SCOPE1", "S1,S2", "T1", "G1,G2");

        StudyObjectCollection soc = gen.createObjectCollection(rec);
        assertNotNull(soc);
        assertEquals("SOC Label", soc.getLabel());
        assertEquals("http://example.org/ST-1", soc.getIsMemberOfUri());
        assertEquals("sir@example.org", soc.getHasSIRManagerEmail());
        assertNotNull(soc.getTimeScopeUris());
        assertNotNull(soc.getSpaceScopeUris());
        assertNotNull(soc.getGroupUris());
        System.out.println("[SSDGeneratorTest] Finished createObjectCollection_createsSoc_whenFieldsValid");
    }
}
