package org.hascoapi.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExtractDp2UriRules {

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: ExtractDp2UriRules <DP2.xlsx>");
            System.exit(1);
        }

        try (Workbook wb = new XSSFWorkbook(new FileInputStream(args[0]))) {
            analyzeSheet(wb, "Deployments");
            analyzeSheet(wb, "PlatformInstances");
            analyzeSheet(wb, "InstrumentInstances");
        }
    }

    private static void analyzeSheet(Workbook wb, String sheetName) {
        Sheet s = wb.getSheet(sheetName);
        if (s == null) {
            System.out.println("Sheet not found: " + sheetName);
            return;
        }

        int uriCol = findHeaderCol(s, "hasURI");
        if (uriCol < 0) {
            System.out.println("No hasURI column in " + sheetName);
            return;
        }

        List<String> uris = new ArrayList<>();
        for (int r = 1; r <= s.getLastRowNum(); r++) {
            Row row = s.getRow(r);
            if (row == null) continue;
            String v = cellValue(row.getCell(uriCol));
            if (!v.isEmpty()) uris.add(v);
        }

        System.out.println("=== " + sheetName + " ===");
        System.out.println("count=" + uris.size());
        if (uris.isEmpty()) {
            System.out.println();
            return;
        }

        for (int i = 0; i < Math.min(10, uris.size()); i++) {
            System.out.println("sample[" + i + "]=" + uris.get(i));
        }

        // Infer prefix + numeric/epoch suffix patterns
        Pattern p = Pattern.compile("^(.*?)(\\d+)$");
        Map<String, Integer> stemCount = new LinkedHashMap<>();
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        boolean allNumericSuffix = true;
        int width = -1;

        for (String u : uris) {
            Matcher m = p.matcher(u);
            if (!m.find()) {
                allNumericSuffix = false;
                continue;
            }
            String stem = m.group(1);
            String num = m.group(2);
            stemCount.put(stem, stemCount.getOrDefault(stem, 0) + 1);
            try {
                long n = Long.parseLong(num);
                min = Math.min(min, n);
                max = Math.max(max, n);
            } catch (NumberFormatException ignored) {
                allNumericSuffix = false;
            }
            if (width < 0) width = num.length();
            else if (width != num.length()) width = 0;
        }

        System.out.println("stems=");
        for (Map.Entry<String, Integer> e : stemCount.entrySet()) {
            System.out.println("  " + e.getKey() + " -> " + e.getValue());
        }

        if (allNumericSuffix && min != Long.MAX_VALUE) {
            System.out.println("numericSuffix=true");
            System.out.println("suffixMin=" + min + " suffixMax=" + max);
            if (width > 0) System.out.println("fixedWidth=" + width);
            if (width == 0) System.out.println("fixedWidth=no");
        } else {
            System.out.println("numericSuffix=not uniform");
        }

        // show uniqueness
        Set<String> uniq = new HashSet<>(uris);
        System.out.println("unique=" + uniq.size() + " duplicates=" + (uris.size() - uniq.size()));
        System.out.println();
    }

    private static int findHeaderCol(Sheet s, String header) {
        Row h = s.getRow(0);
        if (h == null) return -1;
        for (int c = 0; c < h.getLastCellNum(); c++) {
            if (header.equalsIgnoreCase(cellValue(h.getCell(c)))) return c;
        }
        return -1;
    }

    private static String cellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                double d = cell.getNumericCellValue();
                long l = (long) d;
                return d == l ? String.valueOf(l) : String.valueOf(d);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}
