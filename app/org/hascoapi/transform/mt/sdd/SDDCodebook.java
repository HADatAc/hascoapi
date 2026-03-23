package org.hascoapi.transform.mt.sdd;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.hascoapi.entity.pojo.PossibleValue;
import org.hascoapi.utils.URIUtils;

/**
 * Handles the Codebook sheet for SDD generation.
 *
 * The Codebook defines allowed values for categorical variables.
 * For example, mapping 1 to "male" and 2 to "female" for a gender column.
 */
public class SDDCodebook {

    /**
     * Set headers for the Codebook sheet.
     *
     * Columns: Column, Code, Label, Class
     */
    public static void setHeaders(Sheet sheet) {
        if (sheet == null) {
            return;
        }

        Row headerRow = sheet.createRow(0);
        int col = 0;

        headerRow.createCell(col++).setCellValue("Column");
        headerRow.createCell(col++).setCellValue("Code");
        headerRow.createCell(col++).setCellValue("Label"); // Changed from "CodeLabel"
        headerRow.createCell(col++).setCellValue("Class");
    }

    /**
     * Add a row to the Codebook sheet from a PossibleValue.
     */
    public static void add(Sheet sheet, SDDGenHelper helper, PossibleValue pv) {
        if (sheet == null || pv == null) {
            return;
        }

        System.out.println("[SDDCodebook] Adding PossibleValue:");
        System.out.println("  URI: " + pv.getUri());
        System.out.println("  HasVariable: " + pv.getHasVariable());
        System.out.println("  HasCode: " + pv.getHasCode());
        System.out.println("  HasCodeLabel: " + pv.getHasCodeLabel());
        System.out.println("  HasClass: " + pv.getHasClass());

        // Find next available row
        int rowNum = sheet.getLastRowNum() + 1;
        Row row = sheet.createRow(rowNum);

        int col = 0;

        // Column - the variable/column name this code applies to
        String column = pv.getHasVariable();
        if (column != null && !column.isEmpty()) {
            row.createCell(col).setCellValue(column);
        }
        col++;

        // Code - the raw value in the data
        String code = pv.getHasCode();
        if (code != null && !code.isEmpty()) {
            row.createCell(col).setCellValue(code);
        }
        col++;

        // Label - the human-readable label (changed from CodeLabel)
        String codeLabel = pv.getHasCodeLabel();
        if (codeLabel != null && !codeLabel.isEmpty()) {
            row.createCell(col).setCellValue(codeLabel);
        }
        col++;

        // Class - the ontological class this code maps to
        String classUri = pv.getHasClass();
        if (classUri != null && !classUri.isEmpty()) {
            String classShort;
            if (helper != null) {
                // Auto-detect and register namespace if it's a full URI
                classShort = helper.registerAndConvertFullUri(classUri);
            } else {
                classShort = URIUtils.replaceNameSpaceEx(classUri);
            }
            row.createCell(col).setCellValue(classShort);
        }
        col++;

        System.out.println("[SDDCodebook] PossibleValue row added successfully at row " + rowNum);
    }
}
