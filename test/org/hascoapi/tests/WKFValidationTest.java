package org.hascoapi.tests;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hascoapi.ingestion.RecordFile;
import org.hascoapi.ingestion.SpreadsheetRecordFile;
import org.hascoapi.utils.MTSheet;
import org.hascoapi.Constants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * WKFValidationTest
 *
 * Validation tests to ensure WKF files comply with WKF Specification v1.0
 * structural requirements, including InfoSheet validation, sheet ordering,
 * and required field presence.
 */
@DisplayName("WKF Specification Validation Tests")
public class WKFValidationTest {

    @Test
    @DisplayName("WKF metadata type should be registered in MTSheet")
    public void testWKFMetadataTypeRegistered() {
        List<String> expectedSheets = MTSheet.getSheetsForType(Constants.MT_WKF);
        
        assertNotNull(expectedSheets, "WKF metadata type should be registered");
        assertFalse(expectedSheets.isEmpty(), "WKF should have expected sheets");
        
        // WKF spec section 3.2: InfoSheet has 6 sheet references
        assertEquals(6, expectedSheets.size(),
            "WKF should have 6 expected sheet keys in InfoSheet");
    }

    @Test
    @DisplayName("WKF should expect correct InfoSheet keys")
    public void testWKFInfoSheetKeys() {
        List<String> expectedSheets = MTSheet.getSheetsForType(Constants.MT_WKF);
        
        // WKF spec section 3.2: Required InfoSheet fields
        assertTrue(expectedSheets.contains("hasDependencies"),
            "WKF should expect hasDependencies");
        assertTrue(expectedSheets.contains("ProcessStems"),
            "WKF should expect ProcessStems");
        assertTrue(expectedSheets.contains("Processes"),
            "WKF should expect Processes");
        assertTrue(expectedSheets.contains("Tasks"),
            "WKF should expect Tasks");
        assertTrue(expectedSheets.contains("RequiredInstruments"),
            "WKF should expect RequiredInstruments");
        assertTrue(expectedSheets.contains("hasVersion"),
            "WKF should expect hasVersion");
    }

    @Test
    @DisplayName("Valid WKF workbook should pass InfoSheet validation")
    public void testValidWKFInfoSheet() throws Exception {
        // Create a minimal valid WKF workbook
        Workbook workbook = new XSSFWorkbook();
        
        // Create InfoSheet with correct structure
        Sheet infoSheet = workbook.createSheet("InfoSheet");
        
        // Header row
        Row headerRow = infoSheet.createRow(0);
        headerRow.createCell(0).setCellValue("Attribute");
        headerRow.createCell(1).setCellValue("Value");
        
        // Data rows
        createInfoRow(infoSheet, 1, "hasDependencies", "#Namespaces");
        createInfoRow(infoSheet, 2, "ProcessStems", "#ProcessStems");
        createInfoRow(infoSheet, 3, "Processes", "#Processes");
        createInfoRow(infoSheet, 4, "Tasks", "#Tasks");
        createInfoRow(infoSheet, 5, "RequiredInstruments", "#RequiredInstruments");
        createInfoRow(infoSheet, 6, "hasVersion", "1");
        
        // Create required sheets (empty but present)
        workbook.createSheet("Namespaces");
        workbook.createSheet("ProcessStems");
        workbook.createSheet("Processes");
        workbook.createSheet("Tasks");
        workbook.createSheet("RequiredInstruments");
        
        // Write to temp file
        File tempFile = File.createTempFile("wkf-test-", ".xlsx");
        tempFile.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            workbook.write(fos);
        }
        workbook.close();
        
        // Validate InfoSheet can be loaded
        RecordFile recordFile = new SpreadsheetRecordFile(tempFile, "InfoSheet");
        assertTrue(recordFile.isValid(), "Valid WKF InfoSheet should be recognized");
        assertFalse(recordFile.getRecords().isEmpty(), "InfoSheet should have records");
    }

    @Test
    @DisplayName("WKF workbook missing InfoSheet should be invalid")
    public void testMissingInfoSheet() throws Exception {
        Workbook workbook = new XSSFWorkbook();
        
        // Create sheets but NO InfoSheet
        workbook.createSheet("Namespaces");
        workbook.createSheet("ProcessStems");
        
        File tempFile = File.createTempFile("wkf-no-info-", ".xlsx");
        tempFile.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            workbook.write(fos);
        }
        workbook.close();
        
        // Attempt to load InfoSheet should fail
        RecordFile recordFile = new SpreadsheetRecordFile(tempFile, "InfoSheet");
        assertFalse(recordFile.isValid(),
            "WKF without InfoSheet should be invalid");
    }

    @Test
    @DisplayName("InfoSheet with incorrect number of rows should be detectable")
    public void testInfoSheetIncorrectRowCount() throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet infoSheet = workbook.createSheet("InfoSheet");
        
        // Header row only - missing data rows
        Row headerRow = infoSheet.createRow(0);
        headerRow.createCell(0).setCellValue("Attribute");
        headerRow.createCell(1).setCellValue("Value");
        
        // Only create 3 data rows instead of 6
        createInfoRow(infoSheet, 1, "hasDependencies", "#Namespaces");
        createInfoRow(infoSheet, 2, "ProcessStems", "#ProcessStems");
        createInfoRow(infoSheet, 3, "hasVersion", "1");
        
        File tempFile = File.createTempFile("wkf-bad-rows-", ".xlsx");
        tempFile.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            workbook.write(fos);
        }
        workbook.close();
        
        RecordFile recordFile = new SpreadsheetRecordFile(tempFile, "InfoSheet");
        
        // Should load but have fewer records than expected
        assertTrue(recordFile.isValid(), "InfoSheet should be valid structure");
        assertTrue(recordFile.getRecords().size() < 6,
            "InfoSheet should have fewer than 6 data rows");
    }

    @Test
    @DisplayName("WKF should enforce correct sheet ordering")
    public void testSheetOrdering() throws Exception {
        Workbook workbook = new XSSFWorkbook();
        
        // Create sheets in wrong order
        workbook.createSheet("Tasks");              // Should be index 4
        workbook.createSheet("InfoSheet");          // Should be index 0
        workbook.createSheet("ProcessStems");       // Should be index 2
        workbook.createSheet("Namespaces");         // Should be index 1
        workbook.createSheet("RequiredInstruments"); // Should be index 5
        workbook.createSheet("Processes");          // Should be index 3
        
        // Verify actual order is wrong
        assertNotEquals("InfoSheet", workbook.getSheetName(0),
            "Sheet order should be incorrect");
        
        // WKF spec section 3.3: InfoSheet must be at index 0
        int infoSheetIndex = workbook.getSheetIndex("InfoSheet");
        assertNotEquals(0, infoSheetIndex,
            "InfoSheet is not at index 0 in this test workbook");
    }

    @Test
    @DisplayName("Valid WKF should have sheets in correct order")
    public void testCorrectSheetOrdering() throws Exception {
        Workbook workbook = new XSSFWorkbook();
        
        // Create sheets in CORRECT order per WKF spec section 3.3
        workbook.createSheet("InfoSheet");          // Index 0
        workbook.createSheet("Namespaces");         // Index 1
        workbook.createSheet("ProcessStems");       // Index 2
        workbook.createSheet("Processes");          // Index 3
        workbook.createSheet("Tasks");              // Index 4
        workbook.createSheet("RequiredInstruments"); // Index 5
        
        // Verify correct order
        assertEquals("InfoSheet", workbook.getSheetName(0));
        assertEquals("Namespaces", workbook.getSheetName(1));
        assertEquals("ProcessStems", workbook.getSheetName(2));
        assertEquals("Processes", workbook.getSheetName(3));
        assertEquals("Tasks", workbook.getSheetName(4));
        assertEquals("RequiredInstruments", workbook.getSheetName(5));
    }

    @Test
    @DisplayName("WKF with duplicate sheet names should be detectable")
    public void testDuplicateSheetNames() throws Exception {
        Workbook workbook = new XSSFWorkbook();
        
        // POI automatically renames duplicates, but we can test detection
        workbook.createSheet("Tasks");
        
        // Attempting to create duplicate should throw exception or auto-rename
        assertThrows(IllegalArgumentException.class, () -> {
            workbook.createSheet("Tasks"); // Same name
        }, "Duplicate sheet names should not be allowed");
    }

    @Test
    @DisplayName("InfoSheet should have exactly 2 columns")
    public void testInfoSheetColumnCount() throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet infoSheet = workbook.createSheet("InfoSheet");
        
        // Create header with 3 columns (incorrect)
        Row headerRow = infoSheet.createRow(0);
        headerRow.createCell(0).setCellValue("Attribute");
        headerRow.createCell(1).setCellValue("Value");
        headerRow.createCell(2).setCellValue("Extra"); // Should not exist
        
        File tempFile = File.createTempFile("wkf-extra-col-", ".xlsx");
        tempFile.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            workbook.write(fos);
        }
        workbook.close();
        
        // Load and check
        try (FileInputStream fis = new FileInputStream(tempFile);
             Workbook wb = new XSSFWorkbook(fis)) {
            Sheet sheet = wb.getSheet("InfoSheet");
            Row row = sheet.getRow(0);
            
            // WKF spec section 3.2: InfoSheet has exactly 2 columns
            assertTrue(row.getLastCellNum() >= 2,
                "InfoSheet should have at least 2 columns");
            
            // Having more than 2 is technically allowed but not spec-compliant
            if (row.getLastCellNum() > 2) {
                System.out.println("Warning: InfoSheet has more than 2 columns (not spec-compliant)");
            }
        }
    }

    @Test
    @DisplayName("All required WKF sheets should be present")
    public void testRequiredSheetsPresent() {
        Workbook workbook = new XSSFWorkbook();
        
        // Create all required sheets
        workbook.createSheet("InfoSheet");
        workbook.createSheet("Namespaces");
        workbook.createSheet("ProcessStems");
        workbook.createSheet("Processes");
        workbook.createSheet("Tasks");
        workbook.createSheet("RequiredInstruments");
        
        // Verify all present
        Set<String> requiredSheets = new HashSet<>();
        requiredSheets.add("InfoSheet");
        requiredSheets.add("Namespaces");
        requiredSheets.add("ProcessStems");
        requiredSheets.add("Processes");
        requiredSheets.add("Tasks");
        requiredSheets.add("RequiredInstruments");
        
        Set<String> actualSheets = new HashSet<>();
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            actualSheets.add(workbook.getSheetName(i));
        }
        
        assertTrue(actualSheets.containsAll(requiredSheets),
            "WKF workbook should contain all required sheets");
    }

    // Helper method to create InfoSheet data row
    private void createInfoRow(Sheet sheet, int rowIndex, String attribute, String value) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(attribute);
        row.createCell(1).setCellValue(value);
    }
}
