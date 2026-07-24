package org.hascoapi.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;

public class ListPiagetOrgs {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java ListPiagetOrgs <KGR-INSTITUTOS-URI.xlsx>");
            System.exit(1);
        }

        try (FileInputStream fis = new FileInputStream(args[0]);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet orgSheet = workbook.getSheet("Organizations");
            if (orgSheet == null) {
                System.err.println("Organizations sheet not found");
                System.exit(1);
            }

            Row headerRow = orgSheet.getRow(0);
            int uriCol = -1, labelCol = -1;
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                String header = getCellValue(headerRow.getCell(i));
                if (header.equals("hasURI")) uriCol = i;
                if (header.equals("rdfs:label")) labelCol = i;
            }

            System.out.println("PIAGET Organizations in KGR-INSTITUTOS-URI.xlsx:\n");

            for (int i = 1; i <= orgSheet.getLastRowNum(); i++) {
                Row row = orgSheet.getRow(i);
                if (row == null) continue;

                String label = getCellValue(row.getCell(labelCol));
                if (label.toUpperCase().contains("PIAGET")) {
                    String uri = getCellValue(row.getCell(uriCol));
                    System.out.println("Label: " + label);
                    System.out.println("URI:   " + uri);
                    System.out.println();
                }
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC: return String.valueOf(cell.getNumericCellValue());
            default: return "";
        }
    }
}
