package org.hascoapi.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;

/**
 * Rewrites DP2 URIs for Deployments, PlatformInstances, and InstrumentInstances
 * using the Drupal rep-style iid rule: time() + rand(10000,99999) + uid.
 *
 * Usage:
 *   runMain org.hascoapi.utils.UpdateDp2UrisByRepRule <dp2.xlsx> [uid]
 */
public class UpdateDp2UrisByRepRule {

    private static final String SHEET_DEPLOYMENTS = "Deployments";
    private static final String SHEET_PLATFORM_INSTANCES = "PlatformInstances";
    private static final String SHEET_INSTRUMENT_INSTANCES = "InstrumentInstances";

    private static final String PREFIX_DEPLOYMENT = "DPL";
    private static final String PREFIX_PLATFORM_INSTANCE = "PFI";
    private static final String PREFIX_INSTRUMENT_INSTANCE = "INI";

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Usage: runMain org.hascoapi.utils.UpdateDp2UrisByRepRule <dp2.xlsx> [uid]");
            System.exit(1);
        }

        String xlsxPath = args[0];
        String uid = (args.length == 2) ? args[1] : "1";
        if (!uid.matches("\\d+")) {
            System.err.println("[ERROR] uid must be numeric. Provided: " + uid);
            System.exit(1);
        }

        File inputFile = new File(xlsxPath);
        if (!inputFile.exists()) {
            System.err.println("[ERROR] File not found: " + xlsxPath);
            System.exit(1);
        }

        File backupFile = new File(xlsxPath + ".before-rep-uri-update");
        File tempFile = new File(xlsxPath + ".tmp");

        try {
            copyFile(inputFile, backupFile);

            try (FileInputStream fis = new FileInputStream(inputFile);
                 Workbook workbook = new XSSFWorkbook(fis)) {

                Map<String, String> uriMap = new LinkedHashMap<>();
                Set<String> generatedUris = new HashSet<>();

                int depCount = collectUriMappings(workbook.getSheet(SHEET_DEPLOYMENTS), PREFIX_DEPLOYMENT, uid, uriMap, generatedUris);
                int pfiCount = collectUriMappings(workbook.getSheet(SHEET_PLATFORM_INSTANCES), PREFIX_PLATFORM_INSTANCE, uid, uriMap, generatedUris);
                int iniCount = collectUriMappings(workbook.getSheet(SHEET_INSTRUMENT_INSTANCES), PREFIX_INSTRUMENT_INSTANCE, uid, uriMap, generatedUris);

                int replacements = applyMappings(workbook, uriMap);

                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    workbook.write(fos);
                }
            }

            if (!inputFile.delete()) {
                throw new RuntimeException("Could not remove original file before replace: " + xlsxPath);
            }
            if (!tempFile.renameTo(inputFile)) {
                throw new RuntimeException("Could not rename temp file to original: " + xlsxPath);
            }

            System.out.println("[OK] Updated file: " + xlsxPath);
            System.out.println("[OK] Backup file: " + backupFile.getAbsolutePath());

            // Summary from backup and updated workbook
            try (FileInputStream fis = new FileInputStream(inputFile);
                 Workbook wb = new XSSFWorkbook(fis)) {
                System.out.println("[INFO] Deployments rows: " + countNonEmptyUriRows(wb.getSheet(SHEET_DEPLOYMENTS)));
                System.out.println("[INFO] PlatformInstances rows: " + countNonEmptyUriRows(wb.getSheet(SHEET_PLATFORM_INSTANCES)));
                System.out.println("[INFO] InstrumentInstances rows: " + countNonEmptyUriRows(wb.getSheet(SHEET_INSTRUMENT_INSTANCES)));
            }

        } catch (Exception e) {
            if (tempFile.exists()) {
                // Best effort cleanup.
                tempFile.delete();
            }
            System.err.println("[ERROR] " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static int collectUriMappings(Sheet sheet,
                                          String shortPrefix,
                                          String uid,
                                          Map<String, String> uriMap,
                                          Set<String> generatedUris) {
        if (sheet == null) {
            return 0;
        }

        int updated = 0;
        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }

            Cell cell = row.getCell(0);
            String oldUri = getCellString(cell);
            if (oldUri == null || oldUri.trim().isEmpty()) {
                continue;
            }

            String namespace = extractNamespacePrefix(oldUri);
            String newUri = namespace + shortPrefix + generateRepIid(uid, generatedUris);
            uriMap.put(oldUri, newUri);
            updated++;
        }

        return updated;
    }

    private static int applyMappings(Workbook workbook, Map<String, String> uriMap) {
        int replacements = 0;
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                short lastCellNum = row.getLastCellNum();
                if (lastCellNum < 0) {
                    continue;
                }

                for (int c = 0; c < lastCellNum; c++) {
                    Cell cell = row.getCell(c);
                    if (cell == null || cell.getCellType() != CellType.STRING) {
                        continue;
                    }
                    String value = cell.getStringCellValue();
                    String mapped = uriMap.get(value);
                    if (mapped != null) {
                        cell.setCellValue(mapped);
                        replacements++;
                    }
                }
            }
        }
        return replacements;
    }

    private static String generateRepIid(String uid, Set<String> generatedUris) {
        Random random = new Random();
        String iid;
        do {
            long seconds = System.currentTimeMillis() / 1000L;
            int randomFive = random.nextInt(90000) + 10000;
            iid = String.valueOf(seconds) + randomFive + uid;
        } while (generatedUris.contains(iid));

        generatedUris.add(iid);
        return iid;
    }

    private static String extractNamespacePrefix(String uri) {
        if (uri == null || uri.isEmpty()) {
            return "";
        }

        int colon = uri.indexOf(':');
        if (colon > 0 && !uri.startsWith("http://") && !uri.startsWith("https://")) {
            return uri.substring(0, colon + 1);
        }

        int slash = uri.lastIndexOf('/');
        int hash = uri.lastIndexOf('#');
        int sep = Math.max(slash, hash);
        if (sep >= 0) {
            return uri.substring(0, sep + 1);
        }

        return "";
    }

    private static String getCellString(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((long) cell.getNumericCellValue());
        }
        return null;
    }

    private static int countNonEmptyUriRows(Sheet sheet) {
        if (sheet == null) {
            return 0;
        }
        int count = 0;
        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            Cell cell = row.getCell(0);
            String value = getCellString(cell);
            if (value != null && !value.trim().isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static void copyFile(File source, File target) throws Exception {
        try (FileInputStream fis = new FileInputStream(source);
             FileOutputStream fos = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, read);
            }
        }
    }
}