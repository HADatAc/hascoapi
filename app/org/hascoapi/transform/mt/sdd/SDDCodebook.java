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
     * Columns: Column, Code, CodeLabel, Class
     */
    public static void setHeaders(Sheet sheet) {
        if (sheet == null) {
            return;
        }

        Row headerRow = sheet.createRow(0);
        int col = 0;

        headerRow.createCell(col++).setCellValue("Column");
        headerRow.createCell(col++).setCellValue("Code");
        headerRow.createCell(col++).setCellValue("CodeLabel");
        headerRow.createCell(col++).setCellValue("Class");
    }

    /**
     * Add a row to the Codebook sheet from a PossibleValue.
     */
    public static void add(Sheet sheet, SDDGenHelper helper, PossibleValue pv) {
        if (sheet == null || pv == null) {
            return;
        }

        // Find next available row
        int rowNum = sheet.getLastRowNum() + 1;
        Row row = sheet.createRow(rowNum);

        int col = 0;

        // Column - the variable/column name this code applies to
        String column = pv.getVariableName();
        if (column != null && !column.isEmpty()) {
            row.createCell(col).setCellValue(column);
        }
        col++;

        // Code - the raw value in the data
        String code = pv.getCode();
        if (code != null && !code.isEmpty()) {
            row.createCell(col).setCellValue(code);
        }
        col++;

        // CodeLabel - the human-readable label
        String codeLabel = pv.getCodeLabel();
        if (codeLabel != null && !codeLabel.isEmpty()) {
            row.createCell(col).setCellValue(codeLabel);
        }
        col++;

        // Class - the ontological class this code maps to
        String classUri = pv.getClassUri();
        if (classUri != null && !classUri.isEmpty()) {
            String classShort = URIUtils.replacePrefixEx(classUri);
            row.createCell(col).setCellValue(classShort);
            helper.registerPrefixFromUri(classShort);
        }
        col++;
    }
}

