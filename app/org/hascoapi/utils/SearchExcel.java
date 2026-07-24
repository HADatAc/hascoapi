package org.hascoapi.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;

public class SearchExcel {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java SearchExcel <file.xlsx> <search_term>");
            System.exit(1);
        }

        String searchTerm = args[1].toLowerCase();

        try (FileInputStream fis = new FileInputStream(args[0]);
             Workbook workbook = new XSSFWorkbook(fis)) {

            System.out.println("Searching for: " + searchTerm);
            System.out.println("File: " + args[0]);
            System.out.println();

            boolean found = false;

            for (int sheetIdx = 0; sheetIdx < workbook.getNumberOfSheets(); sheetIdx++) {
                Sheet sheet = workbook.getSheetAt(sheetIdx);
                
                for (int rowIdx = 0; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                    Row row = sheet.getRow(rowIdx);
                    if (row == null) continue;

                    for (int cellIdx = 0; cellIdx < row.getLastCellNum(); cellIdx++) {
                        Cell cell = row.getCell(cellIdx);
                        if (cell == null) continue;

                        String value = getCellValue(cell).toLowerCase();
                        if (value.contains(searchTerm)) {
                            found = true;
                            System.out.println("Found in sheet: " + sheet.getSheetName());
                            System.out.println("  Row " + (rowIdx + 1) + ", Column " + (cellIdx + 1));
                            
                            // Print the entire row
                            Row headerRow = sheet.getRow(0);
                            System.out.println("  Full row:");
                            for (int i = 0; i < row.getLastCellNum(); i++) {
                                Cell c = row.getCell(i);
                                if (c != null) {
                                    String colName = "";
                                    if (headerRow != null) {
                                        Cell headerCell = headerRow.getCell(i);
                                        if (headerCell != null) {
                                            colName = getCellValue(headerCell) + ": ";
                                        }
                                    }
                                    System.out.println("    " + colName + getCellValue(c));
                                }
                            }
                            System.out.println();
                        }
                    }
                }
            }

            if (!found) {
                System.out.println("No matches found for: " + searchTerm);
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
