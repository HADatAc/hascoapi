package org.hascoapi.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Utility to convert multiple CSV files into a single Excel workbook.
 * Each CSV becomes a separate sheet.
 */
public class CsvToExcel {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java CsvToExcel <output.xlsx> <sheet1:file1.csv> <sheet2:file2.csv> ...");
            System.err.println("Example: java CsvToExcel output.xlsx \"PlatformInstances:data1.csv\" \"Deployments:data2.csv\"");
            System.exit(1);
        }

        String outputFile = args[0];
        
        try (Workbook workbook = new XSSFWorkbook()) {
            
            // Process each CSV file
            for (int i = 1; i < args.length; i++) {
                String[] parts = args[i].split(":", 2);
                if (parts.length != 2) {
                    System.err.println("Invalid argument format: " + args[i]);
                    System.err.println("Expected format: SheetName:file.csv");
                    continue;
                }
                
                String sheetName = parts[0];
                String csvFile = parts[1];
                
                System.out.println("Processing: " + csvFile + " -> " + sheetName);
                
                Sheet sheet = workbook.createSheet(sheetName);
                loadCsvIntoSheet(csvFile, sheet);
                
                System.out.println("  ✓ Added " + (sheet.getLastRowNum() + 1) + " rows");
            }
            
            // Save the workbook
            try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
                workbook.write(fileOut);
                System.out.println("\n✅ Created " + outputFile);
                System.out.println("   File size: " + new File(outputFile).length() + " bytes");
            }
            
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void loadCsvIntoSheet(String csvFile, Sheet sheet) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(csvFile), StandardCharsets.UTF_8))) {
            
            String line;
            int rowNum = 0;
            
            while ((line = reader.readLine()) != null) {
                Row row = sheet.createRow(rowNum++);
                String[] values = parseCsvLine(line);
                
                for (int i = 0; i < values.length; i++) {
                    Cell cell = row.createCell(i);
                    cell.setCellValue(values[i]);
                }
            }
            
            // Auto-size columns (limited to first 10 for performance)
            int maxCols = Math.min(10, sheet.getRow(0) != null ? sheet.getRow(0).getLastCellNum() : 0);
            for (int i = 0; i < maxCols; i++) {
                sheet.autoSizeColumn(i);
            }
        }
    }
    
    /**
     * Simple CSV parser that handles quoted values.
     * Supports: "value", "value with, comma", "value with "" quote"
     */
    private static String[] parseCsvLine(String line) {
        if (line == null || line.isEmpty()) {
            return new String[0];
        }
        
        java.util.List<String> result = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // Escaped quote: ""
                    current.append('"');
                    i++; // Skip next quote
                } else {
                    // Toggle quote mode
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                // End of field
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        
        // Add last field
        result.add(current.toString());
        
        return result.toArray(new String[0]);
    }
}
