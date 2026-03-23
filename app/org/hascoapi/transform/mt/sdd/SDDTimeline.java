package org.hascoapi.transform.mt.sdd;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.hascoapi.entity.pojo.SDDObject;
import org.hascoapi.utils.URIUtils;

/**
 * Handles the Timeline sheet for SDD generation.
 *
 * The Timeline defines the temporal context of the data collection,
 * often linking to time-related ontologies.
 *
 * Timeline objects must have hasco:hasRole hasco:TimeRole to be included here.
 * Virtual objects (??instant, ??observation, etc.) belong in Dictionary Mapping, NOT Timeline.
 *
 * Timeline supports Start, End, and Unit fields for temporal modeling.
 */
public class SDDTimeline {

    /**
     * Set headers for the Timeline sheet.
     *
     * Columns: Name, Label, Type, Start, End, Unit
     *
     * This matches the original SDD template structure.
     */
    public static void setHeaders(Sheet sheet) {
        if (sheet == null) {
            return;
        }

        Row headerRow = sheet.createRow(0);
        int col = 0;

        headerRow.createCell(col++).setCellValue("Name");
        headerRow.createCell(col++).setCellValue("Label");
        headerRow.createCell(col++).setCellValue("Type");
        headerRow.createCell(col++).setCellValue("Start");
        headerRow.createCell(col++).setCellValue("End");
        headerRow.createCell(col++).setCellValue("Unit");
    }

    /**
     * Add a row to the Timeline sheet from an SDDObject.
     *
     * Timeline objects must have hasco:hasRole hasco:TimeRole.
     * Retrieves Start, End, and Unit values from SDDObject properties.
     */
    public static void add(Sheet sheet, SDDGenHelper helper, SDDObject obj) {
        if (sheet == null || obj == null) {
            return;
        }

        System.out.println("[SDDTimeline] Adding timeline object: " + obj.getUri());

        // Find next available row
        int rowNum = sheet.getLastRowNum() + 1;
        Row row = sheet.createRow(rowNum);

        int col = 0;

        // Name - the identifier/variable name
        String name = obj.getLabel();
        if (name != null && !name.isEmpty()) {
            row.createCell(col).setCellValue(name);
        }
        col++;

        // Label - human-readable description (stored in rdfs:comment)
        String label = obj.getComment();
        if (label != null && !label.isEmpty()) {
            row.createCell(col).setCellValue(label);
        }
        col++;

        // Type - the ontological class/type - USE PREFIXED FORM
        String type = obj.getEntity();
        if (type != null && !type.isEmpty()) {
            String typeShort;
            if (helper != null) {
                // Auto-detect and register namespace if it's a full URI
                typeShort = helper.registerAndConvertFullUri(type);
            } else {
                typeShort = URIUtils.replaceNameSpaceEx(type);
            }
            row.createCell(col).setCellValue(typeShort);
        }
        col++;

        // Start - temporal start value
        String start = obj.getHasStart();
        if (start != null && !start.isEmpty()) {
            row.createCell(col).setCellValue(start);
        }
        col++;

        // End - temporal end value
        String end = obj.getHasEnd();
        if (end != null && !end.isEmpty()) {
            row.createCell(col).setCellValue(end);
        }
        col++;

        // Unit - temporal unit - USE PREFIXED FORM
        String unit = obj.getHasUnit();
        if (unit != null && !unit.isEmpty()) {
            String unitShort;
            if (helper != null) {
                // Auto-detect and register namespace if it's a full URI
                unitShort = helper.registerAndConvertFullUri(unit);
            } else {
                unitShort = URIUtils.replaceNameSpaceEx(unit);
            }
            row.createCell(col).setCellValue(unitShort);
        }
        col++;

        System.out.println("[SDDTimeline] Timeline object added at row " + rowNum);
    }
}

