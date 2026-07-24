package org.hascoapi.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.util.*;

/**
 * Search for Piaget organizations in KGR files
 */
public class FindPiagetInOrgs {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java FindPiagetInOrgs <KGR-file.xlsx>");
            System.exit(1);
        }

        try (FileInputStream fis = new FileInputStream(args[0]);
             Workbook workbook = new XSSFWorkbook(fis)) {

            System.out.println("Searching Organizations sheet in: " + args[0]);
            System.out.println();

            Sheet sheet = workbook.getSheet("Organizations");
            if (sheet == null) {
                System.out.println("Organizations sheet not found");
                return;
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                System.out.println("No header row found");
                return;
            }

            // Find columns
            int uriCol = findColumn(headerRow, "hasURI");
            int labelCol = findColumn(headerRow, "rdfs:label");

            if (uriCol == -1 || labelCol == -1) {
                System.out.println("Required columns not found");
                System.out.println("Available columns:");
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                    Cell cell = headerRow.getCell(i);
                    if (cell != null) {
                        System.out.println("  [" + i + "] " + getCellValue(cell));
                    }
                }
                return;
            }

            System.out.println("Searching for PIAGET organizations...");
            System.out.println();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Cell uriCell = row.getCell(uriCol);
                Cell labelCell = row.getCell(labelCol);

                if (uriCell != null && labelCell != null) {
                    String uri = getCellValue(uriCell);
                    String label = getCellValue(labelCell);

                    if (!uri.isEmpty() && !label.isEmpty()) {
                        String labelLower = label.toLowerCase();
                        if (labelLower.contains("piaget") || labelLower.contains("silves") || 
                            labelLower.contains("gaia") || labelLower.contains("viseu")) {
                            System.out.println(label + " → " + uri);
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static int findColumn(Row headerRow, String columnName) {
        if (headerRow == null) return -1;
        
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                String value = getCellValue(cell);
                if (value.equalsIgnoreCase(columnName)) {
                    return i;
                }
            }
        }
        return -1;
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
