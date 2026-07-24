package org.hascoapi.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.util.*;

/**
 * Extract Piaget organization URIs from KGR-PEOPLE.xlsx
 */
public class ExtractPiagetOrgs {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java ExtractPiagetOrgs <KGR-PEOPLE.xlsx>");
            System.exit(1);
        }

        try (FileInputStream fis = new FileInputStream(args[0]);
             Workbook workbook = new XSSFWorkbook(fis)) {

            System.out.println("Searching for Piaget organizations in: " + args[0]);
            System.out.println();

            // Try to find the People sheet
            Sheet sheet = null;
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet s = workbook.getSheetAt(i);
                String name = s.getSheetName();
                if (name.equalsIgnoreCase("People") || name.equalsIgnoreCase("Person") || name.equalsIgnoreCase("Persons")) {
                    sheet = s;
                    System.out.println("Found sheet: " + name);
                    break;
                }
            }
            
            if (sheet == null) {
                System.out.println("People sheet not found. Available sheets:");
                for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                    System.out.println("  " + workbook.getSheetAt(i).getSheetName());
                }
                return;
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                System.out.println("No header row found in sheet: " + sheet.getSheetName());
                return;
            }
            
            int orgCol = findColumn(headerRow, "foaf:member");
            
            if (orgCol == -1) {
                System.out.println("Column 'foaf:member' not found. Available columns:");
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                    Cell cell = headerRow.getCell(i);
                    if (cell != null) {
                        System.out.println("  [" + i + "] " + getCellValue(cell));
                    }
                }
                return;
            }

            Set<String> piagetOrgs = new TreeSet<>();
            
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Cell orgCell = row.getCell(orgCol);
                if (orgCell != null) {
                    String orgUri = getCellValue(orgCell);
                    if (orgUri.contains("PIAGET") || orgUri.toLowerCase().contains("piaget")) {
                        piagetOrgs.add(orgUri);
                    }
                }
            }

            System.out.println("Found " + piagetOrgs.size() + " Piaget organization URIs:");
            System.out.println();
            for (String org : piagetOrgs) {
                System.out.println(org);
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
