package org.hascoapi.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;

public class InspectExcel {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java InspectExcel <file.xlsx>");
            System.exit(1);
        }

        try (FileInputStream fis = new FileInputStream(args[0]);
             Workbook workbook = new XSSFWorkbook(fis)) {

            System.out.println("Inspecting: " + args[0]);
            System.out.println("Number of sheets: " + workbook.getNumberOfSheets());
            System.out.println();

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                System.out.println("Sheet " + i + ": " + sheet.getSheetName());
                System.out.println("  Rows: " + (sheet.getLastRowNum() + 1));

                Row headerRow = sheet.getRow(0);
                if (headerRow != null) {
                    System.out.println("  Columns (" + headerRow.getLastCellNum() + "):");
                    for (int j = 0; j < headerRow.getLastCellNum(); j++) {
                        Cell cell = headerRow.getCell(j);
                        if (cell != null) {
                            System.out.println("    [" + j + "] " + cell.getStringCellValue());
                        }
                    }
                }

                // Show first data row
                if (sheet.getLastRowNum() >= 1) {
                    Row dataRow = sheet.getRow(1);
                    if (dataRow != null) {
                        System.out.println("  Sample data row:");
                        for (int j = 0; j < dataRow.getLastCellNum(); j++) {
                            Cell cell = dataRow.getCell(j);
                            if (cell != null) {
                                String value = getCellValue(cell);
                                if (value.length() > 50) value = value.substring(0, 47) + "...";
                                System.out.println("    [" + j + "] " + value);
                            }
                        }
                    }
                }
                System.out.println();
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC: return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA: return cell.getCellFormula();
            default: return "";
        }
    }
}
