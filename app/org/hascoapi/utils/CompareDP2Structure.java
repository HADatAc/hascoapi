package org.hascoapi.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;

/**
 * Compare DP2-PMSR.xlsx structure with DP2Gen output
 */
public class CompareDP2Structure {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java CompareDP2Structure <DP2-PMSR.xlsx>");
            System.exit(1);
        }

        try (FileInputStream fis = new FileInputStream(args[0]);
             Workbook workbook = new XSSFWorkbook(fis)) {

            System.out.println("DP2-PMSR.xlsx GOLDEN STANDARD STRUCTURE");
            System.out.println("========================================");
            System.out.println();
            System.out.println("Number of sheets: " + workbook.getNumberOfSheets());
            System.out.println();

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                System.out.println("Sheet " + i + ": \"" + sheet.getSheetName() + "\"");
                
                Row headerRow = sheet.getRow(0);
                if (headerRow != null) {
                    System.out.print("  Columns: ");
                    for (int j = 0; j < headerRow.getLastCellNum(); j++) {
                        Cell cell = headerRow.getCell(j);
                        if (cell != null) {
                            String value = getCellValue(cell);
                            if (!value.isEmpty()) {
                                if (j > 0) System.out.print(", ");
                                System.out.print("\"" + value + "\"");
                            }
                        }
                    }
                    System.out.println();
                }
                
                // Special handling for InfoSheet
                if (sheet.getSheetName().equals("InfoSheet")) {
                    System.out.println("  InfoSheet content:");
                    for (int r = 0; r <= sheet.getLastRowNum() && r < 20; r++) {
                        Row row = sheet.getRow(r);
                        if (row != null) {
                            Cell c1 = row.getCell(0);
                            Cell c2 = row.getCell(1);
                            if (c1 != null && c2 != null) {
                                System.out.println("    " + getCellValue(c1) + " = " + getCellValue(c2));
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
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}
