package org.hascoapi.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

/**
 * Rebuild DP2 workbook using a golden template structure and migrate data from source workbook.
 *
 * Usage:
 *   runMain org.hascoapi.utils.RebuildDp2FromTemplate <template.xlsx> <source.xlsx>
 */
public class RebuildDp2FromTemplate {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: RebuildDp2FromTemplate <template.xlsx> <source.xlsx>");
            System.exit(1);
        }

        File templateFile = new File(args[0]);
        File sourceFile = new File(args[1]);
        File backupFile = new File(args[1] + ".rebuild-backup");
        File tmpFile = new File(args[1] + ".tmp");

        try {
            if (!backupFile.exists()) {
                Files.copy(sourceFile.toPath(), backupFile.toPath());
                System.out.println("Backup created: " + backupFile.getAbsolutePath());
            }

            try (Workbook template = new XSSFWorkbook(new FileInputStream(templateFile));
                 Workbook source = new XSSFWorkbook(new FileInputStream(sourceFile));
                 Workbook out = new XSSFWorkbook()) {

                // 1) Copy template structure exactly.
                //    - InfoSheet: copy full content (must be identical to golden file)
                //    - Other sheets: copy only header row (no template data rows)
                for (int i = 0; i < template.getNumberOfSheets(); i++) {
                    Sheet ts = template.getSheetAt(i);
                    Sheet os = out.createSheet(ts.getSheetName());
                    if ("InfoSheet".equals(ts.getSheetName())) {
                        copySheet(ts, os);
                    } else {
                        copyHeaderOnly(ts, os);
                    }
                }

                // 2) Migrate data into standard sheets
                migratePlatformInstances(source, out.getSheet("PlatformInstances"));
                migrateInstrumentInstances(source, out.getSheet("InstrumentInstances"));
                migrateDeployments(source, out.getSheet("Deployments"));

                // 3) Write output
                try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
                    out.write(fos);
                }
            }

            if (sourceFile.delete() && tmpFile.renameTo(sourceFile)) {
                System.out.println("Rebuild complete: " + sourceFile.getAbsolutePath());
            } else {
                throw new IOException("Could not replace source workbook");
            }

        } catch (Exception e) {
            System.err.println("Rebuild failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void migratePlatformInstances(Workbook source, Sheet dst) {
        Sheet src = source.getSheet("PlatformInstances");
        if (src == null || dst == null) {
            System.out.println("PlatformInstances not migrated (missing sheet)");
            return;
        }

        Map<String, Integer> s = headerMap(src);
        Map<String, Integer> d = headerMap(dst);

        int outRow = firstDataRow(dst);
        int count = 0;
        for (int r = 1; r <= src.getLastRowNum(); r++) {
            Row sr = src.getRow(r);
            if (sr == null) continue;
            String hasUri = value(sr, s, "hasURI", "uri");
            if (hasUri.isEmpty()) continue;

            Row dr = getOrCreateRow(dst, outRow++);
            set(dr, d, "hasURI", hasUri);
            set(dr, d, "a", nz(value(sr, s, "a"), "vstoi:PlatformInstance"));
            set(dr, d, "rdfs:label", value(sr, s, "rdfs:label", "label"));
            set(dr, d, "vstoi:hasSerialNumber", value(sr, s, "vstoi:hasSerialNumber", "serial_number"));
            set(dr, d, "hasco:partOf", value(sr, s, "hasco:partOf"));

            // Optional coordinate columns if present in source
            copyIfPresent(sr, dr, s, d, "hasco:hasFirstCoordinate", "hasco:hasFirstCoordinate");
            copyIfPresent(sr, dr, s, d, "hasco:hasFirstCoordinateUnit", "hasco:hasFirstCoordinateUnit");
            copyIfPresent(sr, dr, s, d, "hasco:hasFirstCoordinateCharacteristic", "hasco:hasFirstCoordinateCharacteristic");
            copyIfPresent(sr, dr, s, d, "hasco:hasSecondCoordinate", "hasco:hasSecondCoordinate");
            copyIfPresent(sr, dr, s, d, "hasco:hasSecondCoordinateUnit", "hasco:hasSecondCoordinateUnit");
            copyIfPresent(sr, dr, s, d, "hasco:hasSecondCoordinateCharacteristic", "hasco:hasSecondCoordinateCharacteristic");
            copyIfPresent(sr, dr, s, d, "hasco:hasThirdCoordinate", "hasco:hasThirdCoordinate");
            copyIfPresent(sr, dr, s, d, "hasco:hasThirdCoordinateUnit", "hasco:hasThirdCoordinateUnit");
            copyIfPresent(sr, dr, s, d, "hasco:hasThirdCoordinateCharacteristic", "hasco:hasThirdCoordinateCharacteristic");

            count++;
        }
        System.out.println("Migrated PlatformInstances: " + count);
    }

    private static void migrateInstrumentInstances(Workbook source, Sheet dst) {
        Sheet src = source.getSheet("InstrumentInstances");
        if (src == null) {
            src = source.getSheet("PhysicalMedicalSimulators");
        }
        if (src == null || dst == null) {
            System.out.println("InstrumentInstances not migrated (missing sheet)");
            return;
        }

        Map<String, Integer> s = headerMap(src);
        Map<String, Integer> d = headerMap(dst);

        int outRow = firstDataRow(dst);
        int count = 0;
        for (int r = 1; r <= src.getLastRowNum(); r++) {
            Row sr = src.getRow(r);
            if (sr == null) continue;
            String hasUri = value(sr, s, "hasURI", "uri");
            if (hasUri.isEmpty()) continue;

            Row dr = getOrCreateRow(dst, outRow++);
            set(dr, d, "hasURI", hasUri);
            set(dr, d, "a", nz(value(sr, s, "a"), "vstoi:InstrumentInstance"));
            set(dr, d, "rdfs:label", value(sr, s, "rdfs:label", "label"));

            // Map source text fields to standard instrument fields
            String serial = value(sr, s, "vstoi:hasSerialNumber", "serial_number");
            String comment = value(sr, s, "skos:definition", "rdfs:comment", "comment");
            String sameAs = value(sr, s, "owl:sameAs", "model");
            String owner = value(sr, s, "vstoi:hasOwner", "hasco:partOf");

            set(dr, d, "vstoi:hasSerialNumber", serial);
            set(dr, d, "skos:definition", comment);
            set(dr, d, "owl:sameAs", sameAs);
            set(dr, d, "vstoi:hasOwner", owner);

            count++;
        }
        System.out.println("Migrated InstrumentInstances: " + count);
    }

    private static void migrateDeployments(Workbook source, Sheet dst) {
        Sheet src = source.getSheet("Deployments");
        if (src == null || dst == null) {
            System.out.println("Deployments not migrated (missing sheet)");
            return;
        }

        Map<String, Integer> s = headerMap(src);
        Map<String, Integer> d = headerMap(dst);

        int outRow = firstDataRow(dst);
        int count = 0;
        for (int r = 1; r <= src.getLastRowNum(); r++) {
            Row sr = src.getRow(r);
            if (sr == null) continue;
            String hasUri = value(sr, s, "hasURI", "uri");
            if (hasUri.isEmpty()) continue;

            Row dr = getOrCreateRow(dst, outRow++);
            set(dr, d, "hasURI", hasUri);
            set(dr, d, "a", nz(value(sr, s, "a"), "vstoi:Deployment"));

            String inst = value(sr, s, "vstoi:hasInstrumentInstance", "simulator_uri");
            String plat = value(sr, s, "vstoi:hasPlatformInstance", "platform_uri");
            String label = value(sr, s, "rdfs:label");
            String started = value(sr, s, "prov:startedAtTime", "vstoi:hasDeploymentDate", "deployment_date");

            if (label.isEmpty()) {
                label = "Deployment";
            }

            set(dr, d, "rdfs:label", label);
            set(dr, d, "vstoi:hasPlatformInstance", plat);
            set(dr, d, "vstoi:hasInstrumentInstance", inst);
            set(dr, d, "vstoi:hasDetectorInstance", value(sr, s, "vstoi:hasDetectorInstance"));
            set(dr, d, "vstoi:designedAtTime", value(sr, s, "vstoi:designedAtTime"));
            set(dr, d, "prov:startedAtTime", started);
            set(dr, d, "prov:endedAtTime", value(sr, s, "prov:endedAtTime"));

            count++;
        }
        System.out.println("Migrated Deployments: " + count);
    }

    private static int firstDataRow(Sheet s) {
        int lr = s.getLastRowNum();
        if (lr < 1) return 1;
        // append after non-empty rows; keep template rows if any
        int row = 1;
        for (int r = 1; r <= lr; r++) {
            Row rr = s.getRow(r);
            if (rr != null && !isEmptyRow(rr)) {
                row = r + 1;
            }
        }
        return row;
    }

    private static boolean isEmptyRow(Row r) {
        if (r == null) return true;
        for (int c = 0; c < r.getLastCellNum(); c++) {
            Cell cell = r.getCell(c);
            if (cell != null && !cellValue(cell).isEmpty()) return false;
        }
        return true;
    }

    private static void copySheet(Sheet src, Sheet dst) {
        for (int r = 0; r <= src.getLastRowNum(); r++) {
            Row sr = src.getRow(r);
            if (sr == null) continue;
            Row dr = dst.createRow(r);
            for (int c = 0; c < sr.getLastCellNum(); c++) {
                Cell sc = sr.getCell(c);
                if (sc == null) continue;
                Cell dc = dr.createCell(c);
                dc.setCellValue(cellValue(sc));
            }
        }
    }

    private static void copyHeaderOnly(Sheet src, Sheet dst) {
        Row sr = src.getRow(0);
        if (sr == null) return;
        Row dr = dst.createRow(0);
        for (int c = 0; c < sr.getLastCellNum(); c++) {
            Cell sc = sr.getCell(c);
            if (sc == null) continue;
            Cell dc = dr.createCell(c);
            dc.setCellValue(cellValue(sc));
        }
    }

    private static String nz(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }

    private static Map<String, Integer> headerMap(Sheet s) {
        Map<String, Integer> map = new HashMap<>();
        Row h = s.getRow(0);
        if (h == null) return map;
        for (int c = 0; c < h.getLastCellNum(); c++) {
            String v = cellValue(h.getCell(c));
            if (!v.isEmpty()) map.put(v.toLowerCase(Locale.ROOT), c);
        }
        return map;
    }

    private static String value(Row r, Map<String, Integer> map, String... keys) {
        for (String k : keys) {
            Integer c = map.get(k.toLowerCase(Locale.ROOT));
            if (c != null) {
                String v = cellValue(r.getCell(c));
                if (!v.isEmpty()) return v;
            }
        }
        return "";
    }

    private static void copyIfPresent(Row sr, Row dr, Map<String, Integer> s, Map<String, Integer> d, String sk, String dk) {
        String v = value(sr, s, sk);
        if (!v.isEmpty()) set(dr, d, dk, v);
    }

    private static void set(Row r, Map<String, Integer> map, String key, String value) {
        Integer c = map.get(key.toLowerCase(Locale.ROOT));
        if (c == null) return;
        Cell cell = r.getCell(c);
        if (cell == null) cell = r.createCell(c);
        cell.setCellValue(value == null ? "" : value);
    }

    private static Row getOrCreateRow(Sheet s, int rowIndex) {
        Row r = s.getRow(rowIndex);
        if (r == null) r = s.createRow(rowIndex);
        return r;
    }

    private static String cellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                double d = cell.getNumericCellValue();
                long l = (long) d;
                return (d == l) ? String.valueOf(l) : String.valueOf(d);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}
