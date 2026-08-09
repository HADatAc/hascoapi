package org.hascoapi.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.util.*;

/**
 * Enhances DP2-PIAGET-V2.xlsx by:
 * 1. Converting simple column names to DP2 standard format (hasURI, rdfs:label, etc.)
 * 2. Converting full URLs to namespace prefixes (pmsr:)
 * 3. Adding organization assignments from KGR-FACULDADES-URI.xlsx
 */
public class EnhanceDP2PiagetV2 {

    private static Map<String, String> orgUriMap = new HashMap<>();
    
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java EnhanceDP2PiagetV2 <KGR-FACULDADES-URI.xlsx> <DP2-PIAGET-V2.xlsx>");
            System.exit(1);
        }

        String kgr = args[0];
        String piaget = args[1];

        try {
            System.out.println("Step 1: Reading KGR-FACULDADES-URI.xlsx for Organization URIs...");
            loadOrganizationUris(kgr);

            System.out.println("\nStep 2: Enhancing DP2-PIAGET-V2.xlsx...");
            enhancePiagetFile(piaget);

            System.out.println("\n✅ Successfully enhanced DP2-PIAGET-V2.xlsx");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void loadOrganizationUris(String kgr) throws IOException {
        try (FileInputStream fis = new FileInputStream(kgr);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet orgSheet = workbook.getSheet("Organizations");
            if (orgSheet == null) {
                System.out.println("  Warning: No 'Organizations' sheet found");
                return;
            }

            Row headerRow = orgSheet.getRow(0);
            int uriCol = findColumn(headerRow, "hasURI");
            int labelCol = findColumn(headerRow, "rdfs:label");

            if (uriCol == -1 || labelCol == -1) {
                System.out.println("  Warning: Required columns not found");
                return;
            }

            for (int i = 1; i <= orgSheet.getLastRowNum(); i++) {
                Row row = orgSheet.getRow(i);
                if (row == null) continue;

                Cell uriCell = row.getCell(uriCol);
                Cell labelCell = row.getCell(labelCol);

                if (uriCell != null && labelCell != null) {
                    String uri = getCellValue(uriCell);
                    String label = getCellValue(labelCell);
                    if (!uri.isEmpty() && !label.isEmpty()) {
                        String key = label.toLowerCase().trim();
                        orgUriMap.put(key, uri);
                        // Also store common keywords
                        if (key.contains("silves")) orgUriMap.put("silves", uri);
                        if (key.contains("gaia")) orgUriMap.put("gaia", uri);
                        if (key.contains("viseu")) orgUriMap.put("viseu", uri);
                    }
                }
            }
            
            System.out.println("  Loaded " + orgUriMap.size() + " organization mappings");
        }
    }

    private static void enhancePiagetFile(String piaget) throws IOException {
        File inputFile = new File(piaget);
        File backupFile = new File(piaget + ".backup");
        File outputFile = new File(piaget + ".tmp");

        // Backup original
        if (!backupFile.exists()) {
            java.nio.file.Files.copy(inputFile.toPath(), backupFile.toPath());
            System.out.println("  Created backup: " + backupFile.getName());
        }

        try (FileInputStream fis = new FileInputStream(inputFile);
             Workbook workbook = new XSSFWorkbook(fis)) {

            // Process each sheet
            processSheet(workbook, "PlatformInstances", true);
            processSheet(workbook, "PhysicalMedicalSimulators", false);
            processDeployments(workbook);

            // Save to temp file
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                workbook.write(fos);
            }
        }

        // Replace original
        if (inputFile.delete()) {
            if (outputFile.renameTo(inputFile)) {
                System.out.println("  File updated successfully");
            }
        }
    }

    private static void processSheet(Workbook workbook, String sheetName, boolean isPlatform) {
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            System.out.println("  Warning: Sheet '" + sheetName + "' not found");
            return;
        }

        System.out.println("\n  Processing " + sheetName + "...");

        Row headerRow = sheet.getRow(0);
        if (headerRow == null) return;

        // Find current columns
        int uriCol = findColumn(headerRow, "uri");
        int labelCol = findColumn(headerRow, "label");
        int commentCol = findColumn(headerRow, "comment");
        int campusCol = findColumn(headerRow, "campus");

        if (uriCol == -1 || labelCol == -1) {
            System.out.println("    Error: Required columns not found");
            return;
        }

        // Update header to DP2 standard format
        headerRow.getCell(uriCol).setCellValue("hasURI");
        headerRow.getCell(labelCol).setCellValue("rdfs:label");
        if (commentCol != -1) {
            headerRow.getCell(commentCol).setCellValue("rdfs:comment");
        }

        // Add new columns if needed
        int typeCol = headerRow.getLastCellNum();
        headerRow.createCell(typeCol).setCellValue("a");
        
        int partOfCol = typeCol + 1;
        headerRow.createCell(partOfCol).setCellValue("hasco:partOf");

        // Process data rows
        int counter = 1;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            Cell uriCell = row.getCell(uriCol);
            Cell labelCell = row.getCell(labelCol);
            Cell campusCell = campusCol != -1 ? row.getCell(campusCol) : null;

            if (uriCell != null) {
                String oldUri = getCellValue(uriCell);
                String label = labelCell != null ? getCellValue(labelCell) : "";
                String campus = campusCell != null ? getCellValue(campusCell) : "";

                if (!oldUri.isEmpty() && oldUri.startsWith("http")) {
                    // Convert to namespace format
                    String newUri;
                    if (isPlatform) {
                        newUri = "pmsr:PlatformInstance_" + String.format("%04d", counter++);
                    } else {
                        newUri = "pmsr:Simulator_" + String.format("%04d", counter++);
                    }
                    
                    uriCell.setCellValue(newUri);
                    System.out.println("    " + oldUri);
                    System.out.println("      → " + newUri);

                    // Add type
                    Cell typeCell = row.createCell(typeCol);
                    if (isPlatform) {
                        typeCell.setCellValue("vstoi:PlatformInstance");
                    } else {
                        typeCell.setCellValue("vstoi:InstrumentInstance");
                    }

                    // Add organization
                    String orgUri = findOrganization(label, campus);
                    if (orgUri != null) {
                        Cell partOfCell = row.createCell(partOfCol);
                        partOfCell.setCellValue(orgUri);
                        System.out.println("      Org: " + orgUri);
                    }
                }
            }
        }
    }

    private static void processDeployments(Workbook workbook) {
        Sheet sheet = workbook.getSheet("Deployments");
        if (sheet == null) return;

        System.out.println("\n  Processing Deployments...");

        Row headerRow = sheet.getRow(0);
        if (headerRow == null) return;

        // Find columns
        int uriCol = findColumn(headerRow, "uri");
        int simCol = findColumn(headerRow, "simulator_uri");
        int platCol = findColumn(headerRow, "platform_uri");
        int dateCol = findColumn(headerRow, "deployment_date");

        if (uriCol == -1) return;

        // Update headers
        headerRow.getCell(uriCol).setCellValue("hasURI");
        if (simCol != -1) headerRow.getCell(simCol).setCellValue("vstoi:hasInstrumentInstance");
        if (platCol != -1) headerRow.getCell(platCol).setCellValue("vstoi:hasPlatformInstance");
        if (dateCol != -1) headerRow.getCell(dateCol).setCellValue("vstoi:hasDeploymentDate");

        // Add type column
        int typeCol = headerRow.getLastCellNum();
        headerRow.createCell(typeCol).setCellValue("a");

        // Process rows
        int counter = 1;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            Cell uriCell = row.getCell(uriCol);
            if (uriCell != null) {
                String oldUri = getCellValue(uriCell);
                if (!oldUri.isEmpty() && oldUri.startsWith("http")) {
                    String newUri = "pmsr:Deployment_" + String.format("%04d", counter++);
                    uriCell.setCellValue(newUri);
                    System.out.println("    " + oldUri + " → " + newUri);
                }
            }

            // Update platform reference
            if (platCol != -1) {
                Cell platCell = row.getCell(platCol);
                if (platCell != null) {
                    String oldPlatUri = getCellValue(platCell);
                    if (oldPlatUri.startsWith("http")) {
                        String platNum = extractNumber(oldPlatUri);
                        String newPlatUri = "pmsr:PlatformInstance_" + platNum;
                        platCell.setCellValue(newPlatUri);
                        System.out.println("      Platform: " + newPlatUri);
                    }
                }
            }

            // Update simulator reference
            if (simCol != -1) {
                Cell simCell = row.getCell(simCol);
                if (simCell != null) {
                    String oldSimUri = getCellValue(simCell);
                    if (oldSimUri.startsWith("http")) {
                        String simNum = extractNumber(oldSimUri);
                        String newSimUri = "pmsr:Simulator_" + simNum;
                        simCell.setCellValue(newSimUri);
                        System.out.println("      Simulator: " + newSimUri);
                    }
                }
            }

            // Add type
            Cell typeCell = row.createCell(typeCol);
            typeCell.setCellValue("vstoi:Deployment");
        }
    }

    private static String extractNumber(String uri) {
        // Extract the last numeric part
        String[] parts = uri.split("/");
        String lastPart = parts[parts.length - 1];
        return String.format("%04d", Integer.parseInt(lastPart));
    }

    private static String findOrganization(String label, String campus) {
        label = label.toLowerCase();
        campus = campus.toLowerCase();

        // Try campus first
        if (!campus.isEmpty()) {
            String org = orgUriMap.get(campus);
            if (org != null) return org;
        }

        // Try label keywords
        for (String key : Arrays.asList("silves", "gaia", "viseu")) {
            if (label.contains(key) || campus.contains(key)) {
                String org = orgUriMap.get(key);
                if (org != null) return org;
            }
        }

        return null;
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
