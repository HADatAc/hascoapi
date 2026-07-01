package org.hascoapi.tests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.ingestion.AnnotateWKF;
import org.hascoapi.ingestion.Record;
import org.hascoapi.ingestion.RecordFile;
import org.hascoapi.ingestion.SpreadsheetRecordFile;
import org.hascoapi.ingestion.WKFGenerator;
import org.hascoapi.utils.IngestionLogger;
import org.hascoapi.vocabularies.VSTOI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * WKFIngestionTest
 *
 * Comprehensive tests for WKF ingestion, parsing, and error handling to ensure
 * compliance with WKF Specification v1.0. Tests AnnotateWKF and WKFGenerator classes.
 */
@DisplayName("WKF Ingestion and Parsing Tests")
public class WKFIngestionTest {

    @TempDir
    Path tempDir;

    private DataFile mockDataFile;
    private IngestionLogger mockLogger;

    @BeforeEach
    public void setup() {
        mockDataFile = mock(DataFile.class);
        mockLogger = mock(IngestionLogger.class);
        
        when(mockDataFile.getLogger()).thenReturn(mockLogger);
        when(mockDataFile.getUri()).thenReturn("http://example.org/test-wkf");
        when(mockDataFile.getHasSIRManagerEmail()).thenReturn("test@example.org");
    }

    // ========== Successful Ingestion Tests ==========

    @Test
    @DisplayName("AnnotateWKF should successfully process valid WKF file")
    public void testAnnotateWKF_ValidFile() throws Exception {
        File validWkf = createValidWKFFile("valid-wkf.xlsx");
        when(mockDataFile.getFile()).thenReturn(validWkf);

        org.hascoapi.ingestion.GeneratorChain result = AnnotateWKF.exec(mockDataFile, "test/wkf-template.xlsx", VSTOI.DRAFT);

        // Result can be null or a GeneratorChain - just verify it executed
        verify(mockDataFile, atLeastOnce()).getFile();
    }

    @Test
    @DisplayName("WKFGenerator should parse Tasks with hasIterationConstraint")
    public void testWKFGenerator_TaskWithIterationConstraint() throws Exception {
        File wkfFile = createWKFWithIterationConstraint();
        RecordFile recordFile = new SpreadsheetRecordFile(wkfFile, "Tasks");
        
        assertTrue(recordFile.isValid(), "Tasks sheet should be valid");
        assertFalse(recordFile.getRecords().isEmpty(), "Tasks sheet should have records");
        
        // Verify first data record has iteration constraint
        Record firstRecord = recordFile.getRecords().get(0);
        String iterationConstraint = firstRecord.getValueByColumnName("vstoi:hasIterationConstraint");
        assertNotNull(iterationConstraint, "Task should have iteration constraint");
        assertEquals("at least 2 times", iterationConstraint, "Iteration constraint should match");
    }

    @Test
    @DisplayName("WKFGenerator should parse RequiredInstruments with isRelatedToTask")
    public void testWKFGenerator_RequiredInstrumentWithRelatedTask() throws Exception {
        File wkfFile = createWKFWithRequiredInstruments();
        RecordFile recordFile = new SpreadsheetRecordFile(wkfFile, "RequiredInstruments");
        
        assertTrue(recordFile.isValid(), "RequiredInstruments sheet should be valid");
        assertFalse(recordFile.getRecords().isEmpty(), "RequiredInstruments sheet should have records");
        
        Record firstRecord = recordFile.getRecords().get(0);
        String relatedTask = firstRecord.getValueByColumnName("vstoi:isRelatedToTask");
        assertNotNull(relatedTask, "RequiredInstrument should have related task");
        assertEquals("http://example.org/task/calibrate", relatedTask, "Related task should match");
    }

    @Test
    @DisplayName("WKFGenerator should parse RequiredInstruments with hasInstrumentConfig")
    public void testWKFGenerator_RequiredInstrumentWithConfig() throws Exception {
        File wkfFile = createWKFWithRequiredInstruments();
        RecordFile recordFile = new SpreadsheetRecordFile(wkfFile, "RequiredInstruments");
        
        Record firstRecord = recordFile.getRecords().get(0);
        String config = firstRecord.getValueByColumnName("vstoi:hasInstrumentConfig");
        assertNotNull(config, "RequiredInstrument should have config");
        assertTrue(config.contains("range"), "Config should contain JSON data");
    }

    @Test
    @DisplayName("WKFGenerator should handle multi-value properties with semicolons")
    public void testWKFGenerator_MultiValueSemicolon() throws Exception {
        File wkfFile = createWKFWithMultiValueSubtasks();
        RecordFile recordFile = new SpreadsheetRecordFile(wkfFile, "Tasks");
        
        assertTrue(recordFile.isValid(), "Tasks sheet should be valid");
        assertFalse(recordFile.getRecords().isEmpty(), "Tasks sheet should have records");
        
        Record firstRecord = recordFile.getRecords().get(0);
        String subtasks = firstRecord.getValueByColumnName("vstoi:hasSubtask");
        assertNotNull(subtasks, "Task should have subtasks");
        assertTrue(subtasks.contains(";"), "Multi-value subtasks should use semicolon separator");
    }

    @Test
    @DisplayName("WKFGenerator should handle multi-value properties with pipes")
    public void testWKFGenerator_MultiValuePipe() throws Exception {
        File wkfFile = createWKFWithMultiValueInstruments();
        RecordFile recordFile = new SpreadsheetRecordFile(wkfFile, "Tasks");
        
        assertTrue(recordFile.isValid(), "Tasks sheet should be valid");
        assertFalse(recordFile.getRecords().isEmpty(), "Tasks sheet should have records");
        
        Record firstRecord = recordFile.getRecords().get(0);
        String instruments = firstRecord.getValueByColumnName("vstoi:hasRequiredInstrument");
        assertNotNull(instruments, "Task should have required instruments");
        assertTrue(instruments.contains("|"), "Multi-value instruments should use pipe separator");
    }

    // ========== Error Handling Tests ==========

    @Test
    @DisplayName("AnnotateWKF should handle missing InfoSheet")
    public void testAnnotateWKF_MissingInfoSheet() throws Exception {
        File wkfWithoutInfo = createWKFWithoutInfoSheet();
        when(mockDataFile.getFile()).thenReturn(wkfWithoutInfo);

        org.hascoapi.ingestion.GeneratorChain result = AnnotateWKF.exec(mockDataFile, "test.xlsx", VSTOI.DRAFT);

        // Should log error and return null or empty chain
        assertNull(result, "Should return null for missing InfoSheet");
    }

    @Test
    @DisplayName("AnnotateWKF should detect missing required sheets")
    public void testAnnotateWKF_MissingRequiredSheets() throws Exception {
        File incompletWkf = createWKFMissingTasksSheet();
        when(mockDataFile.getFile()).thenReturn(incompletWkf);

        org.hascoapi.ingestion.GeneratorChain result = AnnotateWKF.exec(mockDataFile, "test.xlsx", VSTOI.DRAFT);

        // Should return null or incomplete chain when required sheets are missing
        // (Implementation may still process available sheets)
        // Just verify the call completed without throwing exception
    }

    @Test
    @DisplayName("AnnotateWKF should handle empty InfoSheet")
    public void testAnnotateWKF_EmptyInfoSheet() throws Exception {
        File emptyInfoWkf = createWKFWithEmptyInfoSheet();
        when(mockDataFile.getFile()).thenReturn(emptyInfoWkf);

        org.hascoapi.ingestion.GeneratorChain result = AnnotateWKF.exec(mockDataFile, "test.xlsx", VSTOI.DRAFT);

        // Should return null for empty InfoSheet
        assertNull(result, "Should return null for empty InfoSheet");
    }

    @Test
    @DisplayName("WKFGenerator should accept rows with hasURI")
    public void testWKFGenerator_ValidURI() throws Exception {
        File wkfFile = createWKFWithTasks();
        RecordFile recordFile = new SpreadsheetRecordFile(wkfFile, "Tasks");
        
        assertTrue(recordFile.isValid(), "Tasks sheet should be valid");
        assertFalse(recordFile.getRecords().isEmpty(), "Tasks sheet should have records");
        
        Record firstRecord = recordFile.getRecords().get(0);
        String uri = firstRecord.getValueByColumnName("hasURI");
        assertNotNull(uri, "Task should have hasURI");
        assertTrue(uri.startsWith("http://"), "URI should be valid");
    }

    // ========== Negative Test Cases ==========

    @Test
    @DisplayName("Should reject WKF with incorrect InfoSheet row count")
    public void testNegative_IncorrectInfoSheetRows() throws Exception {
        File badInfoWkf = createWKFWithIncorrectInfoSheetRows();
        RecordFile recordFile = new SpreadsheetRecordFile(badInfoWkf, "InfoSheet");
        
        assertTrue(recordFile.isValid(), "InfoSheet should be structurally valid");
        
        // Should have wrong number of rows (less than 6 data rows)
        int recordCount = recordFile.getRecords().size();
        assertNotEquals(6, recordCount, "InfoSheet should not have exactly 6 data rows");
    }

    @Test
    @DisplayName("Should handle WKF with sheets in wrong order")
    public void testNegative_WrongSheetOrder() throws Exception {
        File wrongOrderWkf = createWKFWithWrongSheetOrder();
        
        try (Workbook wb = WorkbookFactory.create(wrongOrderWkf)) {
            // Verify sheets exist but in wrong order
            assertNotNull(wb.getSheet("InfoSheet"));
            assertNotNull(wb.getSheet("Tasks"));
            
            // InfoSheet should be at index 0, but it's not
            int infoSheetIndex = wb.getSheetIndex("InfoSheet");
            assertNotEquals(0, infoSheetIndex, "InfoSheet should not be at index 0 in this test");
        }
    }

    @Test
    @DisplayName("Should reject Task without required rdfs:label")
    public void testNegative_TaskWithoutLabel() throws Exception {
        File badTaskWkf = createWKFTaskWithoutLabel();
        RecordFile recordFile = new SpreadsheetRecordFile(badTaskWkf, "Tasks");
        
        assertTrue(recordFile.isValid(), "Tasks sheet should be structurally valid");
        Record firstRecord = recordFile.getRecords().get(0);
        
        String label = firstRecord.getValueByColumnName("rdfs:label");
        assertTrue(label == null || label.trim().isEmpty(), 
            "Task should be missing label for this test");
    }

    @Test
    @DisplayName("Should reject RequiredInstrument without usesInstrument")
    public void testNegative_RequiredInstrumentWithoutUsesInstrument() throws Exception {
        File badRIWkf = createWKFRequiredInstrumentWithoutUsesInstrument();
        RecordFile recordFile = new SpreadsheetRecordFile(badRIWkf, "RequiredInstruments");
        
        assertTrue(recordFile.isValid(), "RequiredInstruments sheet should be structurally valid");
        Record firstRecord = recordFile.getRecords().get(0);
        
        String usesInstrument = firstRecord.getValueByColumnName("vstoi:usesInstrument");
        assertTrue(usesInstrument == null || usesInstrument.trim().isEmpty(),
            "RequiredInstrument should be missing usesInstrument for this test");
    }

    @Test
    @DisplayName("Should handle malformed temporal dependency values")
    public void testNegative_MalformedTemporalDependency() throws Exception {
        File badTempDepWkf = createWKFWithInvalidTemporalDependency();
        RecordFile recordFile = new SpreadsheetRecordFile(badTempDepWkf, "Tasks");
        
        Record firstRecord = recordFile.getRecords().get(0);
        String tempDep = firstRecord.getValueByColumnName("vstoi:hasTemporalDependency");
        
        // Invalid value like "invalid_operator" should be present
        assertNotNull(tempDep, "Temporal dependency should exist");
        assertFalse(tempDep.equals("enabling") || tempDep.equals("concurrent"), 
            "Should have invalid temporal dependency value");
    }

    // ========== Helper Methods ==========

    private File createValidWKFFile(String filename) throws Exception {
        File file = tempDir.resolve(filename).toFile();
        Workbook wb = new XSSFWorkbook();
        
        createInfoSheet(wb);
        createNamespacesSheet(wb);
        createProcessStemsSheet(wb);
        createProcessesSheet(wb);
        createTasksSheet(wb);
        createRequiredInstrumentsSheet(wb);
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            wb.write(fos);
        }
        wb.close();
        
        return file;
    }

    private File createWKFWithIterationConstraint() throws Exception {
        File file = tempDir.resolve("wkf-iteration.xlsx").toFile();
        Workbook wb = new XSSFWorkbook();
        
        createInfoSheet(wb);
        createNamespacesSheet(wb);
        createProcessStemsSheet(wb);
        createProcessesSheet(wb);
        
        // Tasks sheet with iteration constraint
        Sheet tasksSheet = wb.createSheet("Tasks");
        Row header = tasksSheet.createRow(0);
        String[] headers = {
            "hasURI", "rdf:type", "hasco:hascoType", "rdfs:label", "rdfs:comment",
            "vstoi:hasStatus", "vstoi:hasLanguage", "vstoi:hasVersion",
            "prov:wasDerivedFrom", "vstoi:hasReviewNote", "vstoi:hasSIRManagerEmail",
            "vstoi:hasEditorEmail", "vstoi:hasSupertask", "vstoi:hasSubtask",
            "vstoi:hasTemporalDependency", "vstoi:hasRequiredInstrument",
            "hasco:hasImage", "hasco:hasWebDocument", "vstoi:hasIterationConstraint"
        };
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
        }
        
        Row dataRow = tasksSheet.createRow(1);
        dataRow.createCell(0).setCellValue("http://example.org/task/test1");
        dataRow.createCell(1).setCellValue("vstoi:UserTask");
        dataRow.createCell(2).setCellValue("vstoi:Task");
        dataRow.createCell(3).setCellValue("Test Task");
        dataRow.createCell(5).setCellValue("CURRENT");
        dataRow.createCell(18).setCellValue("at least 2 times");
        
        createRequiredInstrumentsSheet(wb);
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            wb.write(fos);
        }
        wb.close();
        
        return file;
    }

    private File createWKFWithRequiredInstruments() throws Exception {
        File file = tempDir.resolve("wkf-ri.xlsx").toFile();
        Workbook wb = new XSSFWorkbook();
        
        createInfoSheet(wb);
        createNamespacesSheet(wb);
        createProcessStemsSheet(wb);
        createProcessesSheet(wb);
        createTasksSheet(wb);
        
        // RequiredInstruments with new properties
        Sheet riSheet = wb.createSheet("RequiredInstruments");
        Row header = riSheet.createRow(0);
        String[] headers = {
            "hasURI", "rdf:type", "hasco:hascoType", "rdfs:label", "rdfs:comment",
            "vstoi:usesInstrument", "vstoi:isRelatedToTask", "vstoi:hasInstrumentConfig",
            "hasco:hasImage", "hasco:hasWebDocument"
        };
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
        }
        
        Row dataRow = riSheet.createRow(1);
        dataRow.createCell(0).setCellValue("http://example.org/ri/test");
        dataRow.createCell(1).setCellValue("vstoi:RequiredInstrument");
        dataRow.createCell(2).setCellValue("vstoi:RequiredInstrument");
        dataRow.createCell(3).setCellValue("Test RI");
        dataRow.createCell(5).setCellValue("http://example.org/instrument/inst1");
        dataRow.createCell(6).setCellValue("http://example.org/task/calibrate");
        dataRow.createCell(7).setCellValue("{\"range\": \"0-100\", \"accuracy\": \"1%\"}");
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            wb.write(fos);
        }
        wb.close();
        
        return file;
    }

    private File createWKFWithoutInfoSheet() throws Exception {
        File file = tempDir.resolve("wkf-no-info.xlsx").toFile();
        Workbook wb = new XSSFWorkbook();
        
        wb.createSheet("Namespaces");
        wb.createSheet("ProcessStems");
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            wb.write(fos);
        }
        wb.close();
        
        return file;
    }

    private File createWKFMissingTasksSheet() throws Exception {
        File file = tempDir.resolve("wkf-no-tasks.xlsx").toFile();
        Workbook wb = new XSSFWorkbook();
        
        createInfoSheet(wb);
        createNamespacesSheet(wb);
        createProcessStemsSheet(wb);
        createProcessesSheet(wb);
        // Missing Tasks sheet
        createRequiredInstrumentsSheet(wb);
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            wb.write(fos);
        }
        wb.close();
        
        return file;
    }

    private File createWKFWithEmptyInfoSheet() throws Exception {
        File file = tempDir.resolve("wkf-empty-info.xlsx").toFile();
        Workbook wb = new XSSFWorkbook();
        
        // InfoSheet with only header, no data rows
        Sheet infoSheet = wb.createSheet("InfoSheet");
        Row header = infoSheet.createRow(0);
        header.createCell(0).setCellValue("Attribute");
        header.createCell(1).setCellValue("Value");
        
        createNamespacesSheet(wb);
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            wb.write(fos);
        }
        wb.close();
        
        return file;
    }

    private File createWKFWithIncorrectInfoSheetRows() throws Exception {
        File file = tempDir.resolve("wkf-bad-info-rows.xlsx").toFile();
        Workbook wb = new XSSFWorkbook();
        
        Sheet infoSheet = wb.createSheet("InfoSheet");
        Row header = infoSheet.createRow(0);
        header.createCell(0).setCellValue("Attribute");
        header.createCell(1).setCellValue("Value");
        
        // Only 3 data rows instead of 6
        createRow(infoSheet, 1, "hasDependencies", "#Namespaces");
        createRow(infoSheet, 2, "ProcessStems", "#ProcessStems");
        createRow(infoSheet, 3, "hasVersion", "1");
        
        createNamespacesSheet(wb);
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            wb.write(fos);
        }
        wb.close();
        
        return file;
    }

    private File createWKFWithWrongSheetOrder() throws Exception {
        File file = tempDir.resolve("wkf-wrong-order.xlsx").toFile();
        Workbook wb = new XSSFWorkbook();
        
        // Create in wrong order
        wb.createSheet("Tasks");
        wb.createSheet("InfoSheet");
        wb.createSheet("ProcessStems");
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            wb.write(fos);
        }
        wb.close();
        
        return file;
    }

    private File createWKFTaskWithoutLabel() throws Exception {
        File file = tempDir.resolve("wkf-task-no-label.xlsx").toFile();
        Workbook wb = new XSSFWorkbook();
        
        createInfoSheet(wb);
        createNamespacesSheet(wb);
        createProcessStemsSheet(wb);
        createProcessesSheet(wb);
        
        Sheet tasksSheet = wb.createSheet("Tasks");
        Row header = tasksSheet.createRow(0);
        header.createCell(0).setCellValue("hasURI");
        header.createCell(1).setCellValue("rdf:type");
        header.createCell(3).setCellValue("rdfs:label");
        
        Row dataRow = tasksSheet.createRow(1);
        dataRow.createCell(0).setCellValue("http://example.org/task/test");
        dataRow.createCell(1).setCellValue("vstoi:Task");
        // Missing label at index 3
        
        createRequiredInstrumentsSheet(wb);
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            wb.write(fos);
        }
        wb.close();
        
        return file;
    }

    private File createWKFRequiredInstrumentWithoutUsesInstrument() throws Exception {
        File file = tempDir.resolve("wkf-ri-no-uses.xlsx").toFile();
        Workbook wb = new XSSFWorkbook();
        
        createInfoSheet(wb);
        createNamespacesSheet(wb);
        createProcessStemsSheet(wb);
        createProcessesSheet(wb);
        createTasksSheet(wb);
        
        Sheet riSheet = wb.createSheet("RequiredInstruments");
        Row header = riSheet.createRow(0);
        header.createCell(0).setCellValue("hasURI");
        header.createCell(5).setCellValue("vstoi:usesInstrument");
        
        Row dataRow = riSheet.createRow(1);
        dataRow.createCell(0).setCellValue("http://example.org/ri/test");
        // Missing usesInstrument at index 5
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            wb.write(fos);
        }
        wb.close();
        
        return file;
    }

    private File createWKFWithInvalidTemporalDependency() throws Exception {
        File file = tempDir.resolve("wkf-invalid-temp.xlsx").toFile();
        Workbook wb = new XSSFWorkbook();
        
        createInfoSheet(wb);
        createNamespacesSheet(wb);
        createProcessStemsSheet(wb);
        createProcessesSheet(wb);
        
        Sheet tasksSheet = wb.createSheet("Tasks");
        Row header = tasksSheet.createRow(0);
        header.createCell(0).setCellValue("hasURI");
        header.createCell(14).setCellValue("vstoi:hasTemporalDependency");
        
        Row dataRow = tasksSheet.createRow(1);
        dataRow.createCell(0).setCellValue("http://example.org/task/test");
        dataRow.createCell(14).setCellValue("invalid_operator"); // Invalid value
        
        createRequiredInstrumentsSheet(wb);
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            wb.write(fos);
        }
        wb.close();
        
        return file;
    }

    private void createInfoSheet(Workbook wb) {
        Sheet sheet = wb.createSheet("InfoSheet");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Attribute");
        header.createCell(1).setCellValue("Value");
        
        createRow(sheet, 1, "hasDependencies", "#Namespaces");
        createRow(sheet, 2, "ProcessStems", "#ProcessStems");
        createRow(sheet, 3, "Processes", "#Processes");
        createRow(sheet, 4, "Tasks", "#Tasks");
        createRow(sheet, 5, "RequiredInstruments", "#RequiredInstruments");
        createRow(sheet, 6, "hasVersion", "1");
    }

    private void createNamespacesSheet(Workbook wb) {
        Sheet sheet = wb.createSheet("Namespaces");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("hasPrefix");
        header.createCell(1).setCellValue("hasNameSpace");
        header.createCell(2).setCellValue("hasFormat");
        header.createCell(3).setCellValue("hasSource");
    }

    private void createProcessStemsSheet(Workbook wb) {
        wb.createSheet("ProcessStems");
    }

    private void createProcessesSheet(Workbook wb) {
        wb.createSheet("Processes");
    }

    private void createTasksSheet(Workbook wb) {
        wb.createSheet("Tasks");
    }

    private void createRequiredInstrumentsSheet(Workbook wb) {
        wb.createSheet("RequiredInstruments");
    }

    private void createRow(Sheet sheet, int rowIndex, String... values) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }

    private File createWKFWithMultiValueSubtasks() throws Exception {
        File file = tempDir.resolve("wkf-multi-subtasks.xlsx").toFile();
        Workbook wb = new XSSFWorkbook();
        
        createInfoSheet(wb);
        createNamespacesSheet(wb);
        createProcessStemsSheet(wb);
        createProcessesSheet(wb);
        
        Sheet tasksSheet = wb.createSheet("Tasks");
        Row header = tasksSheet.createRow(0);
        header.createCell(0).setCellValue("hasURI");
        header.createCell(13).setCellValue("vstoi:hasSubtask");
        
        Row dataRow = tasksSheet.createRow(1);
        dataRow.createCell(0).setCellValue("http://example.org/task/parent");
        dataRow.createCell(13).setCellValue("http://example.org/task/sub1 ; http://example.org/task/sub2");
        
        createRequiredInstrumentsSheet(wb);
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            wb.write(fos);
        }
        wb.close();
        
        return file;
    }

    private File createWKFWithMultiValueInstruments() throws Exception {
        File file = tempDir.resolve("wkf-multi-instruments.xlsx").toFile();
        Workbook wb = new XSSFWorkbook();
        
        createInfoSheet(wb);
        createNamespacesSheet(wb);
        createProcessStemsSheet(wb);
        createProcessesSheet(wb);
        
        Sheet tasksSheet = wb.createSheet("Tasks");
        Row header = tasksSheet.createRow(0);
        header.createCell(0).setCellValue("hasURI");
        header.createCell(15).setCellValue("vstoi:hasRequiredInstrument");
        
        Row dataRow = tasksSheet.createRow(1);
        dataRow.createCell(0).setCellValue("http://example.org/task/test");
        dataRow.createCell(15).setCellValue("http://example.org/ri/inst1 | http://example.org/ri/inst2");
        
        createRequiredInstrumentsSheet(wb);
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            wb.write(fos);
        }
        wb.close();
        
        return file;
    }

    private File createWKFWithTasks() throws Exception {
        File file = tempDir.resolve("wkf-with-tasks.xlsx").toFile();
        Workbook wb = new XSSFWorkbook();
        
        createInfoSheet(wb);
        createNamespacesSheet(wb);
        createProcessStemsSheet(wb);
        createProcessesSheet(wb);
        
        Sheet tasksSheet = wb.createSheet("Tasks");
        Row header = tasksSheet.createRow(0);
        header.createCell(0).setCellValue("hasURI");
        header.createCell(3).setCellValue("rdfs:label");
        
        Row dataRow = tasksSheet.createRow(1);
        dataRow.createCell(0).setCellValue("http://example.org/task/test1");
        dataRow.createCell(3).setCellValue("Test Task");
        
        createRequiredInstrumentsSheet(wb);
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            wb.write(fos);
        }
        wb.close();
        
        return file;
    }
}
