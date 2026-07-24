package org.hascoapi.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Prints InstrumentInstances rows with URI, serial, owner and inferred org group.
 * Usage:
 *   runMain org.hascoapi.utils.ReportInstrumentInstanceSerials <dp2.xlsx>
 */
public class ReportInstrumentInstanceSerials {

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: runMain org.hascoapi.utils.ReportInstrumentInstanceSerials <dp2.xlsx>");
            System.exit(1);
        }

        try (Workbook wb = new XSSFWorkbook(new FileInputStream(args[0]))) {
            Sheet s = wb.getSheet("InstrumentInstances");
            if (s == null) {
                System.err.println("InstrumentInstances sheet not found.");
                System.exit(1);
            }

            Map<String, String> ownerToCode = new LinkedHashMap<>();
            String[] codes = new String[] {"08", "09", "10"};
            int idx = 0;

            System.out.println("row|hasURI|serial|owner|ownerCode|label");
            for (int r = 1; r <= s.getLastRowNum(); r++) {
                Row row = s.getRow(r);
                if (row == null) {
                    continue;
                }

                String uri = str(row.getCell(0));
                if (uri.isEmpty()) {
                    continue;
                }
                String serial = str(row.getCell(3));
                String owner = str(row.getCell(6));
                String label = str(row.getCell(2));

                if (!owner.isEmpty() && !ownerToCode.containsKey(owner)) {
                    ownerToCode.put(owner, idx < codes.length ? codes[idx++] : "08");
                }
                String code = ownerToCode.getOrDefault(owner, "");

                System.out.println((r + 1) + "|" + uri + "|" + serial + "|" + owner + "|" + code + "|" + label);
            }

            System.out.println("\nowner-map:");
            for (Map.Entry<String, String> e : ownerToCode.entrySet()) {
                System.out.println(e.getValue() + " <- " + e.getKey());
            }
        }
    }

    private static String str(Cell c) {
        if (c == null) return "";
        if (c.getCellType() == CellType.STRING) return c.getStringCellValue();
        if (c.getCellType() == CellType.NUMERIC) {
            double n = c.getNumericCellValue();
            long ln = (long) n;
            if (Math.abs(n - ln) < 1e-7) return String.valueOf(ln);
            return String.valueOf(n);
        }
        if (c.getCellType() == CellType.BOOLEAN) return String.valueOf(c.getBooleanCellValue());
        return "";
    }
}