package org.hascoapi.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.ingestion.ComponentDeploymentGenerator;
import org.hascoapi.ingestion.Record;
import org.hascoapi.ingestion.RecordFile;
import org.hascoapi.utils.IngestionLogger;
import org.junit.jupiter.api.Test;

public class ComponentDeploymentGeneratorTest {

    private static class TestRecord implements Record {
        private final Map<String, String> values = new HashMap<String, String>();

        TestRecord with(String col, String val) {
            values.put(col, val);
            return this;
        }

        @Override
        public String getValueByColumnName(String colomnName) {
            return values.get(colomnName);
        }

        @Override
        public String getValueByColumnIndex(int index) {
            return null;
        }

        @Override
        public int size() {
            return values.size();
        }
    }

    private static class TestRecordFile implements RecordFile {
        private final List<Record> records;
        private final List<String> headers;

        TestRecordFile(List<Record> records, List<String> headers) {
            this.records = records;
            this.headers = headers;
        }

        @Override
        public List<Record> getRecords() {
            return records;
        }

        @Override
        public List<String> getHeaders() {
            return headers;
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
        public boolean isValid() {
            return true;
        }

        @Override
        public int getNumberOfSheets() {
            return 1;
        }

        @Override
        public int getNumberOfRows() {
            return records.size();
        }

        @Override
        public String getSheetName() {
            return "ComponentDeployments";
        }
    }

    private static class TestDataFile extends DataFile {
        private final String uri;
        private final RecordFile recordFile;
        private final IngestionLogger logger;

        TestDataFile(String uri, RecordFile recordFile) {
            this.uri = uri;
            this.recordFile = recordFile;
            this.logger = new IngestionLogger((DataFile) null);
        }

        @Override
        public String getUri() {
            return uri;
        }

        @Override
        public RecordFile getRecordFile() {
            return recordFile;
        }

        @Override
        public IngestionLogger getLogger() {
            return logger;
        }
    }

    @Test
    public void createRows_createsTwoDerivedRows_whenValidColumns() throws Exception {
        List<Record> records = new ArrayList<Record>();
        records.add(new TestRecord()
                .with("Deployment URI", "https://pmsr.net/ont/DPL-001")
                .with("Instrument Slot URI", "https://pmsr.net/ont/INS-001/CTS/0001")
                .with("Component Instance URI", "https://pmsr.net/ont/CPI-001"));

        RecordFile rf = new TestRecordFile(records, List.of("Deployment URI", "Instrument Slot URI", "Component Instance URI"));
        DataFile df = new TestDataFile("https://pmsr.net/ont/DFL-TEST", rf);

        ComponentDeploymentGenerator gen = new ComponentDeploymentGenerator(df);
        gen.createRows();

        List<Map<String, Object>> rows = gen.getRows();
        assertEquals(2, rows.size(), "Each valid input row should generate two triples (deployment and slot)");

        Map<String, Object> depRow = rows.get(0);
        assertEquals("https://pmsr.net/ont/DPL-001", depRow.get("hasURI"));
        assertEquals("https://pmsr.net/ont/CPI-001", depRow.get("vstoi:hasComponentInstance"));
        assertEquals("https://pmsr.net/ont/DFL-TEST", depRow.get("hasco:hasDataFile"));

        Map<String, Object> slotRow = rows.get(1);
        assertEquals("https://pmsr.net/ont/INS-001/CTS/0001", slotRow.get("hasURI"));
        assertEquals("https://pmsr.net/ont/CPI-001", slotRow.get("vstoi:hasComponentInstance"));
        assertEquals("https://pmsr.net/ont/DFL-TEST", slotRow.get("hasco:hasDataFile"));
    }

    @Test
    public void createRows_skipsRow_whenMissingRequiredValues() throws Exception {
        List<Record> records = new ArrayList<Record>();
        records.add(new TestRecord()
                .with("Deployment URI", "https://pmsr.net/ont/DPL-001")
                .with("Instrument Slot URI", "")
                .with("Component Instance URI", "https://pmsr.net/ont/CPI-001"));

        RecordFile rf = new TestRecordFile(records, List.of("Deployment URI", "Instrument Slot URI", "Component Instance URI"));
        DataFile df = new TestDataFile("https://pmsr.net/ont/DFL-TEST", rf);

        ComponentDeploymentGenerator gen = new ComponentDeploymentGenerator(df);
        gen.createRows();

        assertTrue(gen.getRows().isEmpty(), "Rows with missing required fields must be skipped");
    }

    @Test
    public void createRows_acceptsAliasColumns() throws Exception {
        List<Record> records = new ArrayList<Record>();
        records.add(new TestRecord()
                .with("deploymentUri", "https://pmsr.net/ont/DPL-ALIAS")
                .with("instrumentSlotUri", "https://pmsr.net/ont/INS-ALIAS/CTS/0002")
                .with("componentInstanceUri", "https://pmsr.net/ont/CPI-ALIAS"));

        RecordFile rf = new TestRecordFile(records, List.of("deploymentUri", "instrumentSlotUri", "componentInstanceUri"));
        DataFile df = new TestDataFile("https://pmsr.net/ont/DFL-TEST", rf);

        ComponentDeploymentGenerator gen = new ComponentDeploymentGenerator(df);
        gen.createRows();

        assertEquals(2, gen.getRows().size(), "Alias columns should also be accepted");
    }
}
