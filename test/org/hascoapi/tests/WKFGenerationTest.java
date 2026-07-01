package org.hascoapi.tests;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.poi.ss.usermodel.*;
import org.hascoapi.transform.mt.wkf.WKFGen;
import org.hascoapi.entity.pojo.WKF;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * WKFGenerationTest
 *
 * Tests for WKF Excel workbook generation to ensure compliance with
 * WKF Specification v1.0, including correct sheet structure, headers,
 * and column ordering.
 */
@DisplayName("WKF Workbook Generation Tests")
public class WKFGenerationTest {

    @Test
    @DisplayName("Generated workbook should have exactly 6 sheets in correct order")
    public void testWorkbookSheetStructure() {
        List<WKF> emptyWkfs = new ArrayList<>();
        Workbook workbook = WKFGen.create("test.xlsx", emptyWkfs);
        
        assertNotNull(workbook, "Workbook should be created");
        assertEquals(6, workbook.getNumberOfSheets(),
            "Workbook should have exactly 6 sheets");
        
        // Verify sheet order per WKF spec section 3.3
        assertEquals("InfoSheet", workbook.getSheetName(0),
            "Sheet 0 should be InfoSheet");
        assertEquals("Namespaces", workbook.getSheetName(1),
            "Sheet 1 should be Namespaces");
        assertEquals("ProcessStems", workbook.getSheetName(2),
            "Sheet 2 should be ProcessStems");
        assertEquals("Processes", workbook.getSheetName(3),
            "Sheet 3 should be Processes");
        assertEquals("Tasks", workbook.getSheetName(4),
            "Sheet 4 should be Tasks");
        assertEquals("RequiredInstruments", workbook.getSheetName(5),
            "Sheet 5 should be RequiredInstruments");
    }

    @Test
    @DisplayName("InfoSheet should have exactly 7 rows with correct structure")
    public void testInfoSheetStructure() {
        List<WKF> emptyWkfs = new ArrayList<>();
        Workbook workbook = WKFGen.create("test.xlsx", emptyWkfs);
        
        Sheet infoSheet = workbook.getSheet("InfoSheet");
        assertNotNull(infoSheet, "InfoSheet should exist");
        
        // WKF spec section 3.2: InfoSheet has exactly 7 rows (1 header + 6 data)
        assertEquals(6, infoSheet.getLastRowNum(),
            "InfoSheet should have 7 rows (index 0-6)");
        
        // Verify header row
        Row headerRow = infoSheet.getRow(0);
        assertNotNull(headerRow, "InfoSheet header row should exist");
        assertEquals("Attribute", getCellValue(headerRow, 0),
            "InfoSheet column A header should be 'Attribute'");
        assertEquals("Value", getCellValue(headerRow, 1),
            "InfoSheet column B header should be 'Value'");
        
        // Verify data rows per WKF spec section 3.2
        assertEquals("hasDependencies", getCellValue(infoSheet.getRow(1), 0),
            "Row 1 should be hasDependencies");
        assertEquals("ProcessStems", getCellValue(infoSheet.getRow(2), 0),
            "Row 2 should be ProcessStems");
        assertEquals("Processes", getCellValue(infoSheet.getRow(3), 0),
            "Row 3 should be Processes");
        assertEquals("Tasks", getCellValue(infoSheet.getRow(4), 0),
            "Row 4 should be Tasks");
        assertEquals("RequiredInstruments", getCellValue(infoSheet.getRow(5), 0),
            "Row 5 should be RequiredInstruments");
        assertEquals("hasVersion", getCellValue(infoSheet.getRow(6), 0),
            "Row 6 should be hasVersion");
    }

    @Test
    @DisplayName("InfoSheet should reference correct sheet names")
    public void testInfoSheetReferences() {
        List<WKF> emptyWkfs = new ArrayList<>();
        Workbook workbook = WKFGen.create("test.xlsx", emptyWkfs);
        
        Sheet infoSheet = workbook.getSheet("InfoSheet");
        
        // Verify sheet references (column B values)
        assertEquals("#Namespaces", getCellValue(infoSheet.getRow(1), 1),
            "hasDependencies should reference #Namespaces");
        assertEquals("#ProcessStems", getCellValue(infoSheet.getRow(2), 1),
            "ProcessStems should reference #ProcessStems");
        assertEquals("#Processes", getCellValue(infoSheet.getRow(3), 1),
            "Processes should reference #Processes");
        assertEquals("#Tasks", getCellValue(infoSheet.getRow(4), 1),
            "Tasks should reference #Tasks");
        assertEquals("#RequiredInstruments", getCellValue(infoSheet.getRow(5), 1),
            "RequiredInstruments should reference #RequiredInstruments");
    }

    @Test
    @DisplayName("Namespaces sheet should have correct 4-column header")
    public void testNamespacesSheetHeader() {
        List<WKF> emptyWkfs = new ArrayList<>();
        Workbook workbook = WKFGen.create("test.xlsx", emptyWkfs);
        
        Sheet nsSheet = workbook.getSheet("Namespaces");
        assertNotNull(nsSheet, "Namespaces sheet should exist");
        
        Row headerRow = nsSheet.getRow(0);
        assertNotNull(headerRow, "Namespaces header row should exist");
        
        // WKF spec section 3.4: Namespaces has 4 columns
        assertEquals("hasPrefix", getCellValue(headerRow, 0),
            "Column A should be hasPrefix");
        assertEquals("hasNameSpace", getCellValue(headerRow, 1),
            "Column B should be hasNameSpace");
        assertEquals("hasFormat", getCellValue(headerRow, 2),
            "Column C should be hasFormat");
        assertEquals("hasSource", getCellValue(headerRow, 3),
            "Column D should be hasSource");
    }

    @Test
    @DisplayName("ProcessStems sheet should have correct 16-column header")
    public void testProcessStemsSheetHeader() {
        List<WKF> emptyWkfs = new ArrayList<>();
        Workbook workbook = WKFGen.create("test.xlsx", emptyWkfs);
        
        Sheet sheet = workbook.getSheet("ProcessStems");
        assertNotNull(sheet, "ProcessStems sheet should exist");
        
        Row headerRow = sheet.getRow(0);
        assertNotNull(headerRow, "ProcessStems header row should exist");
        
        // WKF spec section 4.3: ProcessStems has 16 columns
        String[] expectedHeaders = {
            "hasURI", "rdf:type", "hasco:hascoType", "rdfs:label", "rdfs:comment",
            "vstoi:hasStatus", "vstoi:hasContent", "vstoi:hasLanguage", "vstoi:hasVersion",
            "prov:wasDerivedFrom", "prov:wasGeneratedBy", "vstoi:hasReviewNote",
            "vstoi:hasSIRManagerEmail", "vstoi:hasEditorEmail", "hasco:hasImage",
            "hasco:hasWebDocument"
        };
        
        for (int i = 0; i < expectedHeaders.length; i++) {
            assertEquals(expectedHeaders[i], getCellValue(headerRow, i),
                "ProcessStems column " + i + " should be " + expectedHeaders[i]);
        }
    }

    @Test
    @DisplayName("Processes sheet should have correct 15-column header")
    public void testProcessesSheetHeader() {
        List<WKF> emptyWkfs = new ArrayList<>();
        Workbook workbook = WKFGen.create("test.xlsx", emptyWkfs);
        
        Sheet sheet = workbook.getSheet("Processes");
        assertNotNull(sheet, "Processes sheet should exist");
        
        Row headerRow = sheet.getRow(0);
        assertNotNull(headerRow, "Processes header row should exist");
        
        // WKF spec section 4.4: Processes has 15 columns
        String[] expectedHeaders = {
            "hasURI", "rdf:type", "hasco:hascoType", "rdfs:label", "rdfs:comment",
            "vstoi:hasStatus", "vstoi:hasLanguage", "vstoi:hasVersion",
            "prov:wasDerivedFrom", "vstoi:hasReviewNote", "vstoi:hasSIRManagerEmail",
            "vstoi:hasEditorEmail", "vstoi:hasTopTask", "hasco:hasImage",
            "hasco:hasWebDocument"
        };
        
        for (int i = 0; i < expectedHeaders.length; i++) {
            assertEquals(expectedHeaders[i], getCellValue(headerRow, i),
                "Processes column " + i + " should be " + expectedHeaders[i]);
        }
    }

    @Test
    @DisplayName("Tasks sheet should have correct 19-column header including hasIterationConstraint")
    public void testTasksSheetHeader() {
        List<WKF> emptyWkfs = new ArrayList<>();
        Workbook workbook = WKFGen.create("test.xlsx", emptyWkfs);
        
        Sheet sheet = workbook.getSheet("Tasks");
        assertNotNull(sheet, "Tasks sheet should exist");
        
        Row headerRow = sheet.getRow(0);
        assertNotNull(headerRow, "Tasks header row should exist");
        
        // WKF spec section 4.5: Tasks has 19 columns (including new hasIterationConstraint)
        String[] expectedHeaders = {
            "hasURI", "rdf:type", "hasco:hascoType", "rdfs:label", "rdfs:comment",
            "vstoi:hasStatus", "vstoi:hasLanguage", "vstoi:hasVersion",
            "prov:wasDerivedFrom", "vstoi:hasReviewNote", "vstoi:hasSIRManagerEmail",
            "vstoi:hasEditorEmail", "vstoi:hasSupertask", "vstoi:hasSubtask",
            "vstoi:hasTemporalDependency", "vstoi:hasRequiredInstrument",
            "hasco:hasImage", "hasco:hasWebDocument", "vstoi:hasIterationConstraint"
        };
        
        for (int i = 0; i < expectedHeaders.length; i++) {
            assertEquals(expectedHeaders[i], getCellValue(headerRow, i),
                "Tasks column " + i + " should be " + expectedHeaders[i]);
        }
        
        // Explicitly verify the new column 18 (S)
        assertEquals("vstoi:hasIterationConstraint", getCellValue(headerRow, 18),
            "Tasks column 18 (S) should be vstoi:hasIterationConstraint");
    }

    @Test
    @DisplayName("RequiredInstruments sheet should have correct 10-column header with isRelatedToTask and hasInstrumentConfig")
    public void testRequiredInstrumentsSheetHeader() {
        List<WKF> emptyWkfs = new ArrayList<>();
        Workbook workbook = WKFGen.create("test.xlsx", emptyWkfs);
        
        Sheet sheet = workbook.getSheet("RequiredInstruments");
        assertNotNull(sheet, "RequiredInstruments sheet should exist");
        
        Row headerRow = sheet.getRow(0);
        assertNotNull(headerRow, "RequiredInstruments header row should exist");
        
        // WKF spec section 4.6: RequiredInstruments has 10 columns
        String[] expectedHeaders = {
            "hasURI", "rdf:type", "hasco:hascoType", "rdfs:label", "rdfs:comment",
            "vstoi:usesInstrument", "vstoi:isRelatedToTask", "vstoi:hasInstrumentConfig",
            "hasco:hasImage", "hasco:hasWebDocument"
        };
        
        for (int i = 0; i < expectedHeaders.length; i++) {
            assertEquals(expectedHeaders[i], getCellValue(headerRow, i),
                "RequiredInstruments column " + i + " should be " + expectedHeaders[i]);
        }
        
        // Explicitly verify the new columns 6 (G) and 7 (H)
        assertEquals("vstoi:isRelatedToTask", getCellValue(headerRow, 6),
            "RequiredInstruments column 6 (G) should be vstoi:isRelatedToTask");
        assertEquals("vstoi:hasInstrumentConfig", getCellValue(headerRow, 7),
            "RequiredInstruments column 7 (H) should be vstoi:hasInstrumentConfig");
    }

    @Test
    @DisplayName("RequiredInstruments sheet should NOT have hasRequiredComponent column")
    public void testRequiredInstrumentsNoHasRequiredComponent() {
        List<WKF> emptyWkfs = new ArrayList<>();
        Workbook workbook = WKFGen.create("test.xlsx", emptyWkfs);
        
        Sheet sheet = workbook.getSheet("RequiredInstruments");
        Row headerRow = sheet.getRow(0);
        
        // Verify that hasRequiredComponent is NOT in the headers (replaced by isRelatedToTask/hasInstrumentConfig)
        for (int i = 0; i < 10; i++) {
            String cellValue = getCellValue(headerRow, i);
            assertNotEquals("vstoi:hasRequiredComponent", cellValue,
                "RequiredInstruments should not have vstoi:hasRequiredComponent column (non-spec-compliant)");
        }
    }

    @Test
    @DisplayName("All data sheets should have only header row initially")
    public void testEmptyDataSheets() {
        List<WKF> emptyWkfs = new ArrayList<>();
        Workbook workbook = WKFGen.create("test.xlsx", emptyWkfs);
        
        // Data sheets should have only header row when no data provided
        assertEquals(0, workbook.getSheet("ProcessStems").getLastRowNum(),
            "ProcessStems should have only header row");
        assertEquals(0, workbook.getSheet("Processes").getLastRowNum(),
            "Processes should have only header row");
        assertEquals(0, workbook.getSheet("Tasks").getLastRowNum(),
            "Tasks should have only header row");
        assertEquals(0, workbook.getSheet("RequiredInstruments").getLastRowNum(),
            "RequiredInstruments should have only header row");
    }

    @Test
    @DisplayName("InfoSheet should default to version 1 when no WKF provided")
    public void testDefaultVersion() {
        List<WKF> emptyWkfs = new ArrayList<>();
        Workbook workbook = WKFGen.create("test.xlsx", emptyWkfs);
        
        Sheet infoSheet = workbook.getSheet("InfoSheet");
        Row versionRow = infoSheet.getRow(6); // hasVersion is row 6
        
        assertEquals("1", getCellValue(versionRow, 1),
            "Default version should be 1 when no WKF provided");
    }

    // Helper method to safely get cell value as string
    private String getCellValue(Row row, int cellIndex) {
        if (row == null) return null;
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}
