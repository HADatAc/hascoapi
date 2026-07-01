package org.hascoapi.tests;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hascoapi.vocabularies.VSTOI;

import java.io.File;
import java.io.FileOutputStream;

/**
 * WKFTestResourceGenerator
 *
 * Utility class to generate a valid WKF test Excel file compliant with
 * WKF Specification v1.0. This creates a minimal but complete WKF workbook
 * with sample ProcessStem, Process, Tasks, and RequiredInstruments.
 *
 * Run this class to generate: test/resources/wkf/WKF-PMSR-Simulators.xlsx
 */
public class WKFTestResourceGenerator {

    public static void main(String[] args) throws Exception {
        File outputFile = new File("test/resources/wkf/WKF-PMSR-Simulators.xlsx");
        outputFile.getParentFile().mkdirs();
        
        Workbook workbook = createTestWKF();
        
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            workbook.write(fos);
        }
        workbook.close();
        
        System.out.println("Generated WKF test resource: " + outputFile.getAbsolutePath());
    }

    public static Workbook createTestWKF() {
        Workbook workbook = new XSSFWorkbook();

        // Sheet 0: InfoSheet
        createInfoSheet(workbook);

        // Sheet 1: Namespaces
        createNamespacesSheet(workbook);

        // Sheet 2: ProcessStems
        createProcessStemsSheet(workbook);

        // Sheet 3: Processes
        createProcessesSheet(workbook);

        // Sheet 4: Tasks
        createTasksSheet(workbook);

        // Sheet 5: RequiredInstruments
        createRequiredInstrumentsSheet(workbook);

        return workbook;
    }

    private static void createInfoSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("InfoSheet");

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Attribute");
        header.createCell(1).setCellValue("Value");

        createRow(sheet, 1, "hasDependencies", "#Namespaces");
        createRow(sheet, 2, "ProcessStems", "#ProcessStems");
        createRow(sheet, 3, "Processes", "#Processes");
        createRow(sheet, 4, "Tasks", "#Tasks");
        createRow(sheet, 5, "RequiredInstruments", "#RequiredInstruments");
        createRow(sheet, 6, "hasVersion", "1.0");
    }

    private static void createNamespacesSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Namespaces");

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("hasPrefix");
        header.createCell(1).setCellValue("hasNameSpace");
        header.createCell(2).setCellValue("hasFormat");
        header.createCell(3).setCellValue("hasSource");

        // Sample namespaces
        createRow(sheet, 1, "vstoi", "http://hadatac.org/ont/vstoi#", "", "");
        createRow(sheet, 2, "hasco", "http://hadatac.org/ont/hasco#", "", "");
        createRow(sheet, 3, "prov", "http://www.w3.org/ns/prov#", "", "");
    }

    private static void createProcessStemsSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("ProcessStems");

        String[] headers = {
            "hasURI", "rdf:type", "hasco:hascoType", "rdfs:label", "rdfs:comment",
            "vstoi:hasStatus", "vstoi:hasContent", "vstoi:hasLanguage", "vstoi:hasVersion",
            "prov:wasDerivedFrom", "prov:wasGeneratedBy", "vstoi:hasReviewNote",
            "vstoi:hasSIRManagerEmail", "vstoi:hasEditorEmail", "hasco:hasImage",
            "hasco:hasWebDocument"
        };
        createHeaderRow(sheet, headers);

        // Sample ProcessStem
        Row dataRow = sheet.createRow(1);
        dataRow.createCell(0).setCellValue("http://example.org/pst/weather-monitoring");
        dataRow.createCell(1).setCellValue("vstoi:ProcessStem");
        dataRow.createCell(2).setCellValue("vstoi:ProcessStem");
        dataRow.createCell(3).setCellValue("Weather Station Monitoring");
        dataRow.createCell(4).setCellValue("Process template for monitoring weather conditions");
        dataRow.createCell(5).setCellValue("CURRENT");
        dataRow.createCell(6).setCellValue("Detailed monitoring procedure");
        dataRow.createCell(7).setCellValue("en");
        dataRow.createCell(8).setCellValue("1.0");
    }

    private static void createProcessesSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Processes");

        String[] headers = {
            "hasURI", "rdf:type", "hasco:hascoType", "rdfs:label", "rdfs:comment",
            "vstoi:hasStatus", "vstoi:hasLanguage", "vstoi:hasVersion",
            "prov:wasDerivedFrom", "vstoi:hasReviewNote", "vstoi:hasSIRManagerEmail",
            "vstoi:hasEditorEmail", "vstoi:hasTopTask", "hasco:hasImage",
            "hasco:hasWebDocument"
        };
        createHeaderRow(sheet, headers);

        // Sample Process
        Row dataRow = sheet.createRow(1);
        dataRow.createCell(0).setCellValue("http://example.org/proc/weather-2026");
        dataRow.createCell(1).setCellValue("vstoi:Process");
        dataRow.createCell(2).setCellValue("vstoi:Process");
        dataRow.createCell(3).setCellValue("Weather Monitoring 2026");
        dataRow.createCell(4).setCellValue("Weather monitoring process instance for 2026");
        dataRow.createCell(5).setCellValue("CURRENT");
        dataRow.createCell(6).setCellValue("en");
        dataRow.createCell(7).setCellValue("1.0");
        dataRow.createCell(8).setCellValue("http://example.org/pst/weather-monitoring");
        dataRow.createCell(12).setCellValue("http://example.org/task/top-weather-task");
    }

    private static void createTasksSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Tasks");

        String[] headers = {
            "hasURI", "rdf:type", "hasco:hascoType", "rdfs:label", "rdfs:comment",
            "vstoi:hasStatus", "vstoi:hasLanguage", "vstoi:hasVersion",
            "prov:wasDerivedFrom", "vstoi:hasReviewNote", "vstoi:hasSIRManagerEmail",
            "vstoi:hasEditorEmail", "vstoi:hasSupertask", "vstoi:hasSubtask",
            "vstoi:hasTemporalDependency", "vstoi:hasRequiredInstrument",
            "hasco:hasImage", "hasco:hasWebDocument", "vstoi:hasIterationConstraint"
        };
        createHeaderRow(sheet, headers);

        // Top-level task
        Row task1 = sheet.createRow(1);
        task1.createCell(0).setCellValue("http://example.org/task/top-weather-task");
        task1.createCell(1).setCellValue("vstoi:AbstractTask");
        task1.createCell(2).setCellValue("vstoi:Task");
        task1.createCell(3).setCellValue("Monitor Weather");
        task1.createCell(4).setCellValue("Abstract task for weather monitoring");
        task1.createCell(5).setCellValue("CURRENT");
        task1.createCell(6).setCellValue("en");
        task1.createCell(7).setCellValue("1.0");
        task1.createCell(13).setCellValue("http://example.org/task/calibrate | http://example.org/task/measure");
        task1.createCell(14).setCellValue("enabling");

        // Calibration task with iteration constraint
        Row task2 = sheet.createRow(2);
        task2.createCell(0).setCellValue("http://example.org/task/calibrate");
        task2.createCell(1).setCellValue("vstoi:UserTask");
        task2.createCell(2).setCellValue("vstoi:Task");
        task2.createCell(3).setCellValue("Calibrate Sensors");
        task2.createCell(4).setCellValue("Calibrate all weather sensors");
        task2.createCell(5).setCellValue("CURRENT");
        task2.createCell(6).setCellValue("en");
        task2.createCell(7).setCellValue("1.0");
        task2.createCell(12).setCellValue("http://example.org/task/top-weather-task");
        task2.createCell(15).setCellValue("http://example.org/ri/thermometer");
        task2.createCell(18).setCellValue("at least 2 times"); // hasIterationConstraint

        // Measurement task
        Row task3 = sheet.createRow(3);
        task3.createCell(0).setCellValue("http://example.org/task/measure");
        task3.createCell(1).setCellValue("vstoi:ApplicationTask");
        task3.createCell(2).setCellValue("vstoi:Task");
        task3.createCell(3).setCellValue("Record Measurements");
        task3.createCell(4).setCellValue("Record temperature and humidity");
        task3.createCell(5).setCellValue("CURRENT");
        task3.createCell(6).setCellValue("en");
        task3.createCell(7).setCellValue("1.0");
        task3.createCell(12).setCellValue("http://example.org/task/top-weather-task");
        task3.createCell(15).setCellValue("http://example.org/ri/thermometer | http://example.org/ri/hygrometer");
        task3.createCell(18).setCellValue("until end_condition"); // hasIterationConstraint
    }

    private static void createRequiredInstrumentsSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("RequiredInstruments");

        String[] headers = {
            "hasURI", "rdf:type", "hasco:hascoType", "rdfs:label", "rdfs:comment",
            "vstoi:usesInstrument", "vstoi:isRelatedToTask", "vstoi:hasInstrumentConfig",
            "hasco:hasImage", "hasco:hasWebDocument"
        };
        createHeaderRow(sheet, headers);

        // Thermometer required instrument
        Row ri1 = sheet.createRow(1);
        ri1.createCell(0).setCellValue("http://example.org/ri/thermometer");
        ri1.createCell(1).setCellValue("vstoi:RequiredInstrument");
        ri1.createCell(2).setCellValue("vstoi:RequiredInstrument");
        ri1.createCell(3).setCellValue("Digital Thermometer");
        ri1.createCell(4).setCellValue("Required thermometer for temperature measurement");
        ri1.createCell(5).setCellValue("http://example.org/instrument/thermo-001");
        ri1.createCell(6).setCellValue("http://example.org/task/calibrate");
        ri1.createCell(7).setCellValue("{\"range\": \"-40 to 85C\", \"accuracy\": \"0.5C\"}");

        // Hygrometer required instrument
        Row ri2 = sheet.createRow(2);
        ri2.createCell(0).setCellValue("http://example.org/ri/hygrometer");
        ri2.createCell(1).setCellValue("vstoi:RequiredInstrument");
        ri2.createCell(2).setCellValue("vstoi:RequiredInstrument");
        ri2.createCell(3).setCellValue("Digital Hygrometer");
        ri2.createCell(4).setCellValue("Required hygrometer for humidity measurement");
        ri2.createCell(5).setCellValue("http://example.org/instrument/hygro-001");
        ri2.createCell(6).setCellValue("http://example.org/task/measure");
        ri2.createCell(7).setCellValue("{\"range\": \"0-100%\", \"accuracy\": \"2%\"}");
    }

    private static void createHeaderRow(Sheet sheet, String[] headers) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }
    }

    private static void createRow(Sheet sheet, int rowIndex, String... values) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }
}
