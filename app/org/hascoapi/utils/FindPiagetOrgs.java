package org.hascoapi.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;

public class FindPiagetOrgs {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java FindPiagetOrgs <KGR-FACULDADES-URI.xlsx>");
            System.exit(1);
        }

        try (FileInputStream fis = new FileInputStream(args[0]);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet orgSheet = workbook.getSheet("Organizations");
            if (orgSheet == null) {
                System.out.println("No 'Organizations' sheet found");
                return;
            }

            Row headerRow = orgSheet.getRow(0);
            int uriCol = -1, labelCol = -1;
            
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    String value = cell.getStringCellValue();
                    if (value.equalsIgnoreCase("hasURI")) uriCol = i;
                    if (value.equalsIgnoreCase("rdfs:label")) labelCol = i;
                }
            }

            System.out.println("Searching for PIAGET organizations...\n");

            for (int i = 1; i <= orgSheet.getLastRowNum(); i++) {
                Row row = orgSheet.getRow(i);
                if (row == null) continue;

                Cell labelCell = row.getCell(labelCol);
                if (labelCell != null) {
                    String label = labelCell.getStringCellValue();
                    if (label.toUpperCase().contains("PIAGET")) {
                        Cell uriCell = row.getCell(uriCol);
                        String uri = uriCell != null ? uriCell.getStringCellValue() : "";
                        System.out.println(label + " → " + uri);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
