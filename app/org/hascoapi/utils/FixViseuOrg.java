package org.hascoapi.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;

public class FixViseuOrg {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java FixViseuOrg <DP2-PIAGET.xlsx>");
            System.exit(1);
        }

        String piaget = args[0];
        File inputFile = new File(piaget);
        File outputFile = new File(piaget + ".tmp");

        try (FileInputStream fis = new FileInputStream(inputFile);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet platformSheet = workbook.getSheet("PlatformInstances");
            if (platformSheet == null) {
                System.out.println("PlatformInstances sheet not found");
                return;
            }

            Row headerRow = platformSheet.getRow(0);
            int uriCol = -1, labelCol = -1, partOfCol = -1;
            
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    String value = cell.getStringCellValue();
                    if (value.equalsIgnoreCase("hasURI")) uriCol = i;
                    if (value.equalsIgnoreCase("rdfs:label")) labelCol = i;
                    if (value.equalsIgnoreCase("hasco:partOf")) partOfCol = i;
                }
            }

            System.out.println("Fixing Viseu campus organization assignment...\n");

            int fixed = 0;
            for (int i = 1; i <= platformSheet.getLastRowNum(); i++) {
                Row row = platformSheet.getRow(i);
                if (row == null) continue;

                Cell partOfCell = row.getCell(partOfCol);
                if (partOfCell != null) {
                    String orgUri = partOfCell.getStringCellValue();
                    // Remove IGOS-UCP-VISEU assignment (ORG...612140)
                    if (orgUri.contains("612140")) {
                        Cell uriCell = row.getCell(uriCol);
                        Cell labelCell = row.getCell(labelCol);
                        String platformUri = uriCell != null ? uriCell.getStringCellValue() : "";
                        String label = labelCell != null ? labelCell.getStringCellValue() : "";
                        
                        partOfCell.setCellValue("");  // Clear incorrect assignment
                        System.out.println("Removed incorrect assignment:");
                        System.out.println("  Platform: " + platformUri);
                        System.out.println("  Label: " + label);
                        System.out.println("  Old: " + orgUri);
                        System.out.println("  New: (empty - PIAGET-VISEU not found in KGR)");
                        fixed++;
                    }
                }
            }

            // Save
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                workbook.write(fos);
            }

            // Replace original
            if (inputFile.delete()) {
                if (outputFile.renameTo(inputFile)) {
                    System.out.println("\n✅ Fixed " + fixed + " platform(s)");
                    System.out.println("\nNote: PIAGET-VISEU organization not found in KGR-FACULDADES-URI.xlsx");
                    System.out.println("Viseu platform organization left empty until added to KGR");
                }
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
