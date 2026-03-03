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
 */
public class SDDTimeline {

    /**
     * Set headers for the Timeline sheet.
     *
     * Columns: Name, Label, Entity, Role, inRelationTo, Relation
     */
    public static void setHeaders(Sheet sheet) {
        if (sheet == null) {
            return;
        }

        Row headerRow = sheet.createRow(0);
        int col = 0;

        headerRow.createCell(col++).setCellValue("Name");
        headerRow.createCell(col++).setCellValue("Label");
        headerRow.createCell(col++).setCellValue("Entity");
        headerRow.createCell(col++).setCellValue("Role");
        headerRow.createCell(col++).setCellValue("inRelationTo");
        headerRow.createCell(col++).setCellValue("Relation");
    }

    /**
     * Add a row to the Timeline sheet from an SDDObject.
     */
    public static void add(Sheet sheet, SDDGenHelper helper, SDDObject obj) {
        if (sheet == null || obj == null) {
            return;
        }

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

        // Label - human-readable label
        String label = obj.getLabel();
        if (label != null && !label.isEmpty()) {
            row.createCell(col).setCellValue(label);
        }
        col++;

        // Entity - the ontological class
        String entity = obj.getEntity();
        if (entity != null && !entity.isEmpty()) {
            String entityShort = URIUtils.replacePrefixEx(entity);
            row.createCell(col).setCellValue(entityShort);
            helper.registerPrefixFromUri(entityShort);
        }
        col++;

        // Role - the role of the entity
        String role = obj.getRole();
        if (role != null && !role.isEmpty()) {
            String roleShort = URIUtils.replacePrefixEx(role);
            row.createCell(col).setCellValue(roleShort);
            helper.registerPrefixFromUri(roleShort);
        }
        col++;

        // inRelationTo - the target of the relation
        String inRelationTo = obj.getInRelationTo();
        if (inRelationTo != null && !inRelationTo.isEmpty()) {
            String inRelShort = URIUtils.replacePrefixEx(inRelationTo);
            row.createCell(col).setCellValue(inRelShort);
            helper.registerPrefixFromUri(inRelShort);
        }
        col++;

        // Relation - the relationship to other entities
        String relation = obj.getRelation();
        if (relation != null && !relation.isEmpty()) {
            String relationShort = URIUtils.replacePrefixEx(relation);
            row.createCell(col).setCellValue(relationShort);
            helper.registerPrefixFromUri(relationShort);
        }
        col++;
    }
}

