package org.hascoapi.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;

/**
 * Enhances DP2-PIAGET-V2.xlsx by:
 * 1. Following URI conventions from DP2-PMSR-V2.xlsx (using namespaces instead of full URLs)
 * 2. Updating Deployments to match new URIs
 * 3. Using Organization URIs from KGR-FACULDADES-URI.xlsx
 */
public class EnhanceDP2Piaget {

    private static Map<String, String> orgUriMap = new HashMap<>();
    private static Map<String, String> platformOldToNew = new HashMap<>();
    private static Map<String, String> simulatorOldToNew = new HashMap<>();
    private static String platformUriPattern = "";
    private static String instrumentUriPattern = "";

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: java EnhanceDP2Piaget <DP2-PMSR-V2.xlsx> <KGR-FACULDADES-URI.xlsx> <DP2-PIAGET-V2.xlsx>");
            System.exit(1);
        }

        String pmsr = args[0];
        String kgr = args[1];
        String piaget = args[2];

        try {
            System.out.println("Step 1: Reading DP2-PMSR-V2.xlsx to understand URI patterns...");
            analyzeUriPatterns(pmsr);
            
            // Set defaults if patterns weren't found
            if (platformUriPattern.isEmpty()) {
                platformUriPattern = "pmsr:PLATFORM_";
                System.out.println("  Using default platform pattern: " + platformUriPattern + "XXXX");
            }
            if (instrumentUriPattern.isEmpty()) {
                instrumentUriPattern = "pmsr:SIMULATOR_";
                System.out.println("  Using default instrument pattern: " + instrumentUriPattern + "XXXX");
            }

            System.out.println("\nStep 2: Reading KGR-FACULDADES-URI.xlsx for Organization URIs...");
            loadOrganizationUris(kgr);

            System.out.println("\nStep 3: Updating DP2-PIAGET-V2.xlsx...");
            updatePiagetFile(piaget);

            System.out.println("\n✅ Successfully enhanced DP2-PIAGET-V2.xlsx");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void analyzeUriPatterns(String pmsr) throws IOException {
        try (FileInputStream fis = new FileInputStream(pmsr);
             Workbook workbook = new XSSFWorkbook(fis)) {

            // Check PlatformInstances sheet
            Sheet platformSheet = workbook.getSheet("PlatformInstances");
            if (platformSheet != null && platformSheet.getLastRowNum() > 0) {
                Row firstDataRow = platformSheet.getRow(1);
                if (firstDataRow != null) {
                    Cell uriCell = firstDataRow.getCell(0);
                    if (uriCell != null) {
                        String uri = getCellValue(uriCell);
                        System.out.println("  Sample Platform URI: " + uri);
                        // Extract pattern (e.g., pmsr:PLATFORM_xxx or pmsr:PlatformInstance_xxx)
                        if (uri.contains(":") && uri.contains("_")) {
                            int lastUnderscore = uri.lastIndexOf("_");
                            if (lastUnderscore > 0) {
                                String prefix = uri.substring(0, lastUnderscore);
                                platformUriPattern = prefix + "_";
                                System.out.println("  Platform URI pattern: " + platformUriPattern + "XXXX");
                            }
                        } else if (uri.contains(":")) {
                            // Fallback: use namespace prefix
                            String namespace = uri.substring(0, uri.indexOf(":") + 1);
                            platformUriPattern = namespace + "PLATFORM_";
                            System.out.println("  Platform URI pattern (fallback): " + platformUriPattern + "XXXX");
                        }
                    }
                }
            }

            // Check InstrumentInstances or PhysicalMedicalSimulators
            Sheet instrumentSheet = workbook.getSheet("InstrumentInstances");
            if (instrumentSheet == null) {
                instrumentSheet = workbook.getSheet("PhysicalMedicalSimulators");
            }
            if (instrumentSheet != null && instrumentSheet.getLastRowNum() > 0) {
                Row firstDataRow = instrumentSheet.getRow(1);
                if (firstDataRow != null) {
                    Cell uriCell = firstDataRow.getCell(0);
                    if (uriCell != null) {
                        String uri = getCellValue(uriCell);
                        System.out.println("  Sample Instrument URI: " + uri);
                        if (uri.contains(":") && uri.contains("_")) {
                            int lastUnderscore = uri.lastIndexOf("_");
                            if (lastUnderscore > 0) {
                                String prefix = uri.substring(0, lastUnderscore);
                                instrumentUriPattern = prefix + "_";
                                System.out.println("  Instrument URI pattern: " + instrumentUriPattern + "XXXX");
                            }
                        } else if (uri.contains(":")) {
                            // Fallback: use namespace prefix
                            String namespace = uri.substring(0, uri.indexOf(":") + 1);
                            instrumentUriPattern = namespace + "SIMULATOR_";
                            System.out.println("  Instrument URI pattern (fallback): " + instrumentUriPattern + "XXXX");
                        }
                    }
                }
            }
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
                        orgUriMap.put(label.toLowerCase(), uri);
                        System.out.println("  " + label + " → " + uri);
                    }
                }
            }
        }
    }

    private static void updatePiagetFile(String piaget) throws IOException {
        File inputFile = new File(piaget);
        File outputFile = new File(piaget + ".tmp");

        try (FileInputStream fis = new FileInputStream(inputFile);
             Workbook workbook = new XSSFWorkbook(fis)) {

            // Update PlatformInstances
            Sheet platformSheet = workbook.getSheet("PlatformInstances");
            if (platformSheet != null) {
                System.out.println("\n  Updating PlatformInstances...");
                updatePlatformInstances(platformSheet);
            }

            // Update PhysicalMedicalSimulators (InstrumentInstances)
            Sheet simSheet = workbook.getSheet("PhysicalMedicalSimulators");
            if (simSheet != null) {
                System.out.println("\n  Updating PhysicalMedicalSimulators...");
                updateSimulators(simSheet);
            }

            // Update Deployments
            Sheet deploySheet = workbook.getSheet("Deployments");
            if (deploySheet != null) {
                System.out.println("\n  Updating Deployments...");
                updateDeployments(deploySheet);
            }

            // Save to temp file first
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                workbook.write(fos);
            }
        }

        // Replace original with updated version
        if (inputFile.delete()) {
            if (outputFile.renameTo(inputFile)) {
                System.out.println("\n  File updated successfully");
            } else {
                System.err.println("  Error: Could not rename temp file");
            }
        } else {
            System.err.println("  Error: Could not delete original file");
        }
    }

    private static void updatePlatformInstances(Sheet sheet) {
        Row headerRow = sheet.getRow(0);
        int uriCol = findColumn(headerRow, "hasURI");
        int labelCol = findColumn(headerRow, "rdfs:label");
        int partOfCol = findColumn(headerRow, "hasco:partOf");

        if (uriCol == -1 || labelCol == -1) {
            System.out.println("    Warning: Required columns not found");
            return;
        }

        int counter = 1;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            Cell uriCell = row.getCell(uriCol);
            Cell labelCell = row.getCell(labelCol);

            if (uriCell != null && labelCell != null) {
                String oldUri = getCellValue(uriCell);
                String label = getCellValue(labelCell);

                if (!oldUri.isEmpty()) {
                    // Generate new URI following PMSR pattern
                    String newUri = platformUriPattern + String.format("%04d", counter++);
                    platformOldToNew.put(oldUri, newUri);

                    uriCell.setCellValue(newUri);
                    System.out.println("    " + oldUri + " → " + newUri);

                    // Try to assign organization based on label
                    if (partOfCol != -1) {
                        String orgUri = findOrganizationForPlatform(label);
                        if (orgUri != null) {
                            Cell partOfCell = row.getCell(partOfCol);
                            if (partOfCell == null) {
                                partOfCell = row.createCell(partOfCol);
                            }
                            partOfCell.setCellValue(orgUri);
                            System.out.println("      Assigned to: " + orgUri);
                        }
                    }
                }
            }
        }
    }

    private static void updateSimulators(Sheet sheet) {
        Row headerRow = sheet.getRow(0);
        int uriCol = findColumn(headerRow, "hasURI");
        int labelCol = findColumn(headerRow, "rdfs:label");
        int partOfCol = findColumn(headerRow, "hasco:partOf");

        if (uriCol == -1 || labelCol == -1) {
            System.out.println("    Warning: Required columns not found");
            return;
        }

        int counter = 1;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            Cell uriCell = row.getCell(uriCol);
            Cell labelCell = row.getCell(labelCol);

            if (uriCell != null && labelCell != null) {
                String oldUri = getCellValue(uriCell);
                String label = getCellValue(labelCell);

                if (!oldUri.isEmpty()) {
                    // Generate new URI following PMSR pattern
                    String newUri = instrumentUriPattern + String.format("%04d", counter++);
                    simulatorOldToNew.put(oldUri, newUri);

                    uriCell.setCellValue(newUri);
                    System.out.println("    " + oldUri + " → " + newUri);

                    // Try to assign organization based on label
                    if (partOfCol != -1) {
                        String orgUri = findOrganizationForSimulator(label);
                        if (orgUri != null) {
                            Cell partOfCell = row.getCell(partOfCol);
                            if (partOfCell == null) {
                                partOfCell = row.createCell(partOfCol);
                            }
                            partOfCell.setCellValue(orgUri);
                            System.out.println("      Assigned to: " + orgUri);
                        }
                    }
                }
            }
        }
    }

    private static void updateDeployments(Sheet sheet) {
        Row headerRow = sheet.getRow(0);
        int uriCol = findColumn(headerRow, "hasURI");
        int platformCol = findColumn(headerRow, "platform_uri");
        int simulatorCol = findColumn(headerRow, "simulator_uri");

        if (uriCol == -1) {
            System.out.println("    Warning: hasURI column not found");
            return;
        }

        int counter = 1;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            // Update deployment URI
            Cell uriCell = row.getCell(uriCol);
            if (uriCell != null) {
                String oldUri = getCellValue(uriCell);
                if (!oldUri.isEmpty() && oldUri.startsWith("http")) {
                    String newUri = "pmsr:DEPLOYMENT_" + String.format("%04d", counter++);
                    uriCell.setCellValue(newUri);
                    System.out.println("    " + oldUri + " → " + newUri);
                }
            }

            // Update platform reference
            if (platformCol != -1) {
                Cell platformCell = row.getCell(platformCol);
                if (platformCell != null) {
                    String oldPlatformUri = getCellValue(platformCell);
                    String newPlatformUri = platformOldToNew.get(oldPlatformUri);
                    if (newPlatformUri != null) {
                        platformCell.setCellValue(newPlatformUri);
                        System.out.println("      Platform: " + oldPlatformUri + " → " + newPlatformUri);
                    }
                }
            }

            // Update simulator reference
            if (simulatorCol != -1) {
                Cell simulatorCell = row.getCell(simulatorCol);
                if (simulatorCell != null) {
                    String oldSimulatorUri = getCellValue(simulatorCell);
                    String newSimulatorUri = simulatorOldToNew.get(oldSimulatorUri);
                    if (newSimulatorUri != null) {
                        simulatorCell.setCellValue(newSimulatorUri);
                        System.out.println("      Simulator: " + oldSimulatorUri + " → " + newSimulatorUri);
                    }
                }
            }
        }
    }

    private static String findOrganizationForPlatform(String platformLabel) {
        platformLabel = platformLabel.toLowerCase();
        
        // Match based on campus/location keywords
        if (platformLabel.contains("silves")) {
            return findOrgByKeyword("silves");
        } else if (platformLabel.contains("gaia")) {
            return findOrgByKeyword("gaia");
        } else if (platformLabel.contains("viseu")) {
            return findOrgByKeyword("viseu");
        }
        
        return null;
    }

    private static String findOrganizationForSimulator(String simulatorLabel) {
        // Simulators inherit organization from their deployment location
        // This will be handled via Deployments sheet
        return null;
    }

    private static String findOrgByKeyword(String keyword) {
        for (Map.Entry<String, String> entry : orgUriMap.entrySet()) {
            if (entry.getKey().contains(keyword)) {
                return entry.getValue();
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
