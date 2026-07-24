package org.hascoapi.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;

/**
 * Fix Piaget organization assignments in DP2-PIAGET.xlsx using URIs from KGR-PEOPLE.xlsx
 */
public class FixPiagetOrgs {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java FixPiagetOrgs <DP2-PIAGET.xlsx>");
            System.exit(1);
        }

        String piaget = args[0];

        try {
            File inputFile = new File(piaget);
            File outputFile = new File(piaget + ".tmp");

            try (FileInputStream fis = new FileInputStream(inputFile);
                 Workbook workbook = new XSSFWorkbook(fis)) {

                System.out.println("Fixing Piaget organization assignments...");
                System.out.println();

                // Organization mapping from KGR-PEOPLE.xlsx
                String silves = "pmsr:ORG174547834502848251";  // IPIAGET-ALMADA (closest to Silves geographically)
                String gaia = "pmsr:ORG174547834502888794";    // IPIAGET-GAIA
                String viseu = "pmsr:ORG174547834502854654";   // ESS-IPIAGET-VISEU

                System.out.println("Organization mappings:");
                System.out.println("  Silves → " + silves + " (IPIAGET-ALMADA)");
                System.out.println("  Gaia → " + gaia + " (IPIAGET-GAIA)");
                System.out.println("  Viseu → " + viseu + " (ESS-IPIAGET-VISEU)");
                System.out.println();

                Sheet platformSheet = workbook.getSheet("PlatformInstances");
                if (platformSheet != null) {
                    fixPlatformOrganizations(platformSheet, silves, gaia, viseu);
                }

                // Save
                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    workbook.write(fos);
                }
            }

            // Replace original
            if (inputFile.delete()) {
                if (outputFile.renameTo(inputFile)) {
                    System.out.println("\n✅ Successfully updated " + piaget);
                }
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void fixPlatformOrganizations(Sheet sheet, String silves, String gaia, String viseu) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) return;

        int partOfCol = findColumn(headerRow, "hasco:partOf");
        int campusCol = findColumn(headerRow, "campus");
        int uriCol = findColumn(headerRow, "hasURI");

        if (partOfCol == -1 || campusCol == -1) {
            System.out.println("  Warning: Required columns not found");
            return;
        }

        System.out.println("Updating PlatformInstances:");
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            Cell campusCell = row.getCell(campusCol);
            Cell partOfCell = row.getCell(partOfCol);
            Cell uriCell = row.getCell(uriCol);

            if (campusCell != null) {
                String campus = getCellValue(campusCell).toLowerCase();
                String uri = uriCell != null ? getCellValue(uriCell) : "";
                String newOrg = null;

                if (campus.contains("silves")) {
                    newOrg = silves;
                } else if (campus.contains("gaia")) {
                    newOrg = gaia;
                } else if (campus.contains("viseu")) {
                    newOrg = viseu;
                }

                if (newOrg != null && partOfCell != null) {
                    String oldOrg = getCellValue(partOfCell);
                    if (!newOrg.equals(oldOrg)) {
                        partOfCell.setCellValue(newOrg);
                        System.out.println("  " + uri + " (" + campus + ")");
                        System.out.println("    " + oldOrg + " → " + newOrg);
                    }
                }
            }
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
