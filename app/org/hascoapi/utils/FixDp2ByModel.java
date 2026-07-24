package org.hascoapi.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

/**
 * Non-destructive DP2 fixer based on DP2-DATA-MODEL.md rules.
 * - Keeps existing/manual sheets.
 * - Normalizes key DP2 sheets and URI relationships.
 */
public class FixDp2ByModel {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java FixDp2ByModel <DP2.xlsx>");
            System.exit(1);
        }

        File xlsx = new File(args[0]);
        File backup = new File(args[0] + ".model-backup");
        File tmp = new File(args[0] + ".tmp");

        try {
            if (!backup.exists()) {
                Files.copy(xlsx.toPath(), backup.toPath());
                System.out.println("Backup created: " + backup.getAbsolutePath());
            }

            try (FileInputStream fis = new FileInputStream(xlsx);
                 Workbook wb = new XSSFWorkbook(fis)) {

                Sheet platformSheet = wb.getSheet("PlatformInstances");
                Sheet instSheet = wb.getSheet("InstrumentInstances");
                if (instSheet == null) {
                    instSheet = wb.getSheet("PhysicalMedicalSimulators");
                    if (instSheet != null) {
                        int idx = wb.getSheetIndex(instSheet);
                        wb.setSheetName(idx, "InstrumentInstances");
                        System.out.println("Renamed sheet PhysicalMedicalSimulators -> InstrumentInstances");
                    }
                }

                Sheet depSheet = wb.getSheet("Deployments");

                if (platformSheet == null || instSheet == null || depSheet == null) {
                    throw new IllegalStateException("Required sheets missing (PlatformInstances, InstrumentInstances, Deployments)");
                }

                // Build platform maps
                Map<String, String> platformToOrg = new HashMap<>();
                Map<String, String> platformToLabel = new HashMap<>();
                int pUriCol = col(platformSheet, "hasURI");
                int pLabelCol = col(platformSheet, "rdfs:label");
                int pOrgCol = col(platformSheet, "hasco:partOf");
                if (pUriCol >= 0) {
                    for (int r = 1; r <= platformSheet.getLastRowNum(); r++) {
                        Row row = platformSheet.getRow(r);
                        if (row == null) continue;
                        String u = get(row, pUriCol);
                        if (u.isEmpty()) continue;
                        if (pOrgCol >= 0) platformToOrg.put(u, get(row, pOrgCol));
                        if (pLabelCol >= 0) platformToLabel.put(u, get(row, pLabelCol));
                    }
                }

                // Normalize InstrumentInstances headers
                normalizeInstrumentHeaders(instSheet);

                int iUriCol = col(instSheet, "hasURI");
                int iLabelCol = col(instSheet, "rdfs:label");
                int iOwnerCol = col(instSheet, "vstoi:hasOwner");
                if (iOwnerCol < 0) {
                    iOwnerCol = ensureColumn(instSheet, "vstoi:hasOwner");
                }

                // Deployment expected columns
                int dUriCol = ensureColumn(depSheet, "hasURI");
                int dACol = ensureColumn(depSheet, "a");
                int dLabelCol = ensureColumn(depSheet, "rdfs:label");
                int dPlatCol = ensureColumn(depSheet, "vstoi:hasPlatformInstance");
                int dInstCol = ensureColumn(depSheet, "vstoi:hasInstrumentInstance");

                // Remap likely old columns
                remapIfPresent(depSheet, "platform_uri", dPlatCol);
                remapIfPresent(depSheet, "simulator_uri", dInstCol);

                // Build instrument labels
                Map<String, String> instToLabel = new HashMap<>();
                Set<String> instrumentUris = new HashSet<>();
                for (int r = 1; r <= instSheet.getLastRowNum(); r++) {
                    Row row = instSheet.getRow(r);
                    if (row == null) continue;
                    String iu = get(row, iUriCol);
                    if (!iu.isEmpty()) {
                        instrumentUris.add(iu);
                        instToLabel.put(iu, get(row, iLabelCol));
                    }
                }

                Set<String> platformUris = new HashSet<>();
                for (int r = 1; r <= platformSheet.getLastRowNum(); r++) {
                    Row row = platformSheet.getRow(r);
                    if (row == null) continue;
                    String pu = get(row, pUriCol);
                    if (!pu.isEmpty()) platformUris.add(pu);
                }

                // Use deployment links to infer owners for instruments
                Map<String, String> instrumentToOwner = new HashMap<>();
                int fixedRefs = 0;
                for (int r = 1; r <= depSheet.getLastRowNum(); r++) {
                    Row row = depSheet.getRow(r);
                    if (row == null) continue;

                    String depUri = get(row, dUriCol);
                    String platUri = get(row, dPlatCol);
                    String instUri = get(row, dInstCol);

                    // Set deployment type
                    set(row, dACol, "vstoi:Deployment");

                    // Set deployment label if missing
                    if (get(row, dLabelCol).isEmpty()) {
                        String ilabel = instToLabel.getOrDefault(instUri, "Instrument");
                        set(row, dLabelCol, "Deployment of " + ilabel);
                    }

                    // Referential integrity checks (basic normalization)
                    if (!platUri.isEmpty() && platformUris.contains(platUri) && !instUri.isEmpty() && instrumentUris.contains(instUri)) {
                        fixedRefs++;
                    }

                    if (!instUri.isEmpty() && !platUri.isEmpty()) {
                        String org = platformToOrg.getOrDefault(platUri, "");
                        if (!org.isEmpty()) {
                            instrumentToOwner.put(instUri, org);
                        }
                    }

                    if (depUri.isEmpty()) {
                        set(row, dUriCol, "pmsr:Deployment_" + String.format("%04d", r));
                    }
                }

                // Set owners and type in InstrumentInstances
                int iACol = col(instSheet, "a");
                for (int r = 1; r <= instSheet.getLastRowNum(); r++) {
                    Row row = instSheet.getRow(r);
                    if (row == null) continue;
                    String iu = get(row, iUriCol);
                    if (iu.isEmpty()) continue;
                    String owner = instrumentToOwner.getOrDefault(iu, get(row, iOwnerCol));
                    if (!owner.isEmpty()) set(row, iOwnerCol, owner);
                    if (iACol >= 0 && get(row, iACol).isEmpty()) {
                        set(row, iACol, "vstoi:InstrumentInstance");
                    }
                }

                // Write
                try (FileOutputStream fos = new FileOutputStream(tmp)) {
                    wb.write(fos);
                }

                if (xlsx.delete() && tmp.renameTo(xlsx)) {
                    System.out.println("Saved fixes to: " + xlsx.getAbsolutePath());
                    System.out.println("Validated deployment references: " + fixedRefs);
                } else {
                    throw new IOException("Failed to replace original workbook");
                }
            }

        } catch (Exception e) {
            System.err.println("Fix failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void normalizeInstrumentHeaders(Sheet sheet) {
        Row h = sheet.getRow(0);
        if (h == null) h = sheet.createRow(0);

        // legacy -> standard
        int oldPartOf = col(sheet, "hasco:partOf");
        if (oldPartOf >= 0) set(h, oldPartOf, "vstoi:hasOwner");

        // keep existing columns; only normalize known header aliases
        renameHeader(sheet, "uri", "hasURI");
        renameHeader(sheet, "label", "rdfs:label");
        renameHeader(sheet, "comment", "rdfs:comment");
        renameHeader(sheet, "serial_number", "vstoi:hasSerialNumber");
    }

    private static void renameHeader(Sheet s, String oldName, String newName) {
        int c = col(s, oldName);
        if (c >= 0) {
            Row h = s.getRow(0);
            set(h, c, newName);
        }
    }

    private static void remapIfPresent(Sheet s, String fromHeader, int toCol) {
        int fromCol = col(s, fromHeader);
        if (fromCol < 0 || fromCol == toCol) return;
        for (int r = 1; r <= s.getLastRowNum(); r++) {
            Row row = s.getRow(r);
            if (row == null) continue;
            String val = get(row, fromCol);
            if (!val.isEmpty() && get(row, toCol).isEmpty()) {
                set(row, toCol, val);
            }
        }
    }

    private static int ensureColumn(Sheet s, String header) {
        int c = col(s, header);
        if (c >= 0) return c;
        Row h = s.getRow(0);
        if (h == null) h = s.createRow(0);
        int n = h.getLastCellNum() < 0 ? 0 : h.getLastCellNum();
        set(h, n, header);
        return n;
    }

    private static int col(Sheet s, String name) {
        Row h = s.getRow(0);
        if (h == null) return -1;
        for (int i = 0; i < h.getLastCellNum(); i++) {
            String v = get(h, i);
            if (name.equalsIgnoreCase(v)) return i;
        }
        return -1;
    }

    private static String get(Row r, int c) {
        if (r == null || c < 0) return "";
        Cell cell = r.getCell(c);
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue().trim();
            case NUMERIC: return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA: return cell.getCellFormula();
            default: return "";
        }
    }

    private static void set(Row r, int c, String v) {
        Cell cell = r.getCell(c);
        if (cell == null) cell = r.createCell(c);
        cell.setCellValue(v == null ? "" : v);
    }
}
