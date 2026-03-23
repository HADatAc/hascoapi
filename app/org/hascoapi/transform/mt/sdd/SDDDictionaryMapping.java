package org.hascoapi.transform.mt.sdd;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.hascoapi.entity.pojo.SDDAttribute;
import org.hascoapi.utils.URIUtils;

/**
 * Handles the Dictionary Mapping sheet for SDD generation.
 *
 * The Dictionary Mapping sheet is the core of the SDD, mapping raw data columns
 * to semantic concepts and defining contextual entities.
 */
public class SDDDictionaryMapping {

    /**
     * Set headers for the Dictionary Mapping sheet.
     *
     * Columns: Column, Attribute, attributeOf, Unit, Time, Entity, Role, Relation,
     * inRelationTo, wasDerivedFrom, wasGeneratedBy
     */
    public static void setHeaders(Sheet sheet) {
        if (sheet == null) {
            return;
        }

        Row headerRow = sheet.createRow(0);
        int col = 0;

        headerRow.createCell(col++).setCellValue("Column");
        headerRow.createCell(col++).setCellValue("Attribute");
        headerRow.createCell(col++).setCellValue("attributeOf");
        headerRow.createCell(col++).setCellValue("Unit");
        headerRow.createCell(col++).setCellValue("Time");
        headerRow.createCell(col++).setCellValue("Entity");
        headerRow.createCell(col++).setCellValue("Role");
        headerRow.createCell(col++).setCellValue("Relation");
        headerRow.createCell(col++).setCellValue("inRelationTo");
        headerRow.createCell(col++).setCellValue("wasDerivedFrom");
        headerRow.createCell(col++).setCellValue("wasGeneratedBy");
        // hasPosition removed - not in original template
    }

    /**
     * Add a row to the Dictionary Mapping sheet from an SDDAttribute.
     */
    public static void add(Sheet sheet, SDDGenHelper helper, SDDAttribute attr) {
        if (sheet == null || attr == null) {
            return;
        }

        // Find next available row
        int rowNum = sheet.getLastRowNum() + 1;
        Row row = sheet.createRow(rowNum);

        int col = 0;

        // Column - the label of the attribute
        String column = attr.getLabel();
        if (column != null && !column.isEmpty()) {
            row.createCell(col).setCellValue(column);
        }
        col++;

        // Attribute - hasco:hasAttribute - USE PREFIXED FORM
        String attribute = attr.getAttribute();
        if (attribute != null && !attribute.isEmpty()) {
            String attrShort;
            if (helper != null) {
                attrShort = helper.registerAndConvertFullUri(attribute);
            } else {
                attrShort = URIUtils.replaceNameSpaceEx(attribute);
            }
            row.createCell(col).setCellValue(attrShort);
        }
        col++;

        // attributeOf - SDDAttribute points to an SDDObject via hasco:isVariableOf
        // In practice, this is the contextual placeholder (e.g., ??weather). We'll use the object URI.
        String attributeOf = attr.getObjectUri();
        if (attributeOf != null && !attributeOf.isEmpty()) {
            String attrOfShort;
            if (helper != null) {
                attrOfShort = helper.registerAndConvertFullUri(attributeOf);
            } else {
                attrOfShort = URIUtils.replaceNameSpaceEx(attributeOf);
            }
            row.createCell(col).setCellValue(attrOfShort);
        }
        col++;

        // Unit - USE PREFIXED FORM
        String unit = attr.getUnit();
        if (unit != null && !unit.isEmpty()) {
            String unitShort;
            if (helper != null) {
                unitShort = helper.registerAndConvertFullUri(unit);
            } else {
                unitShort = URIUtils.replaceNameSpaceEx(unit);
            }
            row.createCell(col).setCellValue(unitShort);
        }
        col++;

        // Time - not explicitly modeled as a separate field in SDDAttribute.
        // Use hasco:hasEvent (event URI) as the best approximation - USE PREFIXED FORM
        String time = attr.getEventUri();
        if (time != null && !time.isEmpty()) {
            String timeShort;
            if (helper != null) {
                timeShort = helper.registerAndConvertFullUri(time);
            } else {
                timeShort = URIUtils.replaceNameSpaceEx(time);
            }
            row.createCell(col).setCellValue(timeShort);
        }
        col++;

        // Entity - LEAVE EMPTY for SDDAttributes (only populated for SDDObjects)
        col++;

        // Role - not represented in SDDAttribute; leave blank
        col++;

        // Relation - USE PREFIXED FORM
        String relation = attr.getRelation();
        if (relation != null && !relation.isEmpty()) {
            String relationShort;
            if (helper != null) {
                relationShort = helper.registerAndConvertFullUri(relation);
            } else {
                relationShort = URIUtils.replaceNameSpaceEx(relation);
            }
            row.createCell(col).setCellValue(relationShort);
        }
        col++;

        // inRelationTo - USE PREFIXED FORM
        String inRelationTo = attr.getInRelationTo();
        if (inRelationTo != null && !inRelationTo.isEmpty()) {
            String inRelShort;
            if (helper != null) {
                inRelShort = helper.registerAndConvertFullUri(inRelationTo);
            } else {
                inRelShort = URIUtils.replaceNameSpaceEx(inRelationTo);
            }
            row.createCell(col).setCellValue(inRelShort);
        }
        col++;

        // wasDerivedFrom - USE PREFIXED FORM
        String wasDerivedFrom = attr.getWasDerivedFrom();
        if (wasDerivedFrom != null && !wasDerivedFrom.isEmpty()) {
            String derivedShort;
            if (helper != null) {
                derivedShort = helper.registerAndConvertFullUri(wasDerivedFrom);
            } else {
                derivedShort = URIUtils.replaceNameSpaceEx(wasDerivedFrom);
            }
            row.createCell(col).setCellValue(derivedShort);
        }
        col++;

        // wasGeneratedBy - not in SDDAttribute; leave blank
        col++;
    }

    /**
     * Add a row to the Dictionary Mapping sheet from an SDDObject.
     * SDDObjects appear as rows with NO Attribute column (they define entities, not measurements).
     */
    public static void addObject(Sheet sheet, SDDGenHelper helper, org.hascoapi.entity.pojo.SDDObject obj) {
        if (sheet == null || obj == null) {
            System.out.println("[SDDDictionaryMapping] ERROR: Cannot add object - sheet or obj is null");
            return;
        }

        System.out.println("[SDDDictionaryMapping] Adding SDDObject:");
        System.out.println("  URI: " + obj.getUri());
        System.out.println("  Label: " + obj.getLabel());
        System.out.println("  Entity: " + obj.getEntity());
        System.out.println("  Role: " + obj.getRole());
        System.out.println("  Relation: " + obj.getRelation());
        System.out.println("  InRelationTo: " + obj.getInRelationTo());

        // Find next available row
        int rowNum = sheet.getLastRowNum() + 1;
        Row row = sheet.createRow(rowNum);

        int col = 0;

        // Column - the label of the object (e.g., ??weather, ??observation, ??instant)
        String column = obj.getLabel();
        if (column != null && !column.isEmpty()) {
            row.createCell(col).setCellValue(column);
            System.out.println("  Added Column: " + column);
        }
        col++;

        // Attribute - EMPTY for SDDObjects
        col++;

        // attributeOf - EMPTY for SDDObjects
        col++;

        // Unit - EMPTY for SDDObjects
        col++;

        // Time - EMPTY for SDDObjects
        col++;

        // Entity - hasco:hasEntity (e.g., envo:01001079 for weather)
        String entity = obj.getEntity();
        if (entity != null && !entity.isEmpty()) {
            String entityShort;
            if (helper != null) {
                entityShort = helper.registerAndConvertFullUri(entity);
            } else {
                entityShort = URIUtils.replaceNameSpaceEx(entity);
            }
            row.createCell(col).setCellValue(entityShort);
            System.out.println("  Added Entity: " + entityShort);
        }
        col++;

        // Role - hasco:hasRole (e.g., hasco:TimeRole)
        String role = obj.getRole();
        if (role != null && !role.isEmpty()) {
            String roleShort;
            if (helper != null) {
                roleShort = helper.registerAndConvertFullUri(role);
            } else {
                roleShort = URIUtils.replaceNameSpaceEx(role);
            }
            row.createCell(col).setCellValue(roleShort);
            System.out.println("  Added Role: " + roleShort);
        }
        col++;

        // Relation - hasco:hasRelation
        String relation = obj.getRelation();
        if (relation != null && !relation.isEmpty()) {
            String relationShort;
            if (helper != null) {
                relationShort = helper.registerAndConvertFullUri(relation);
            } else {
                relationShort = URIUtils.replaceNameSpaceEx(relation);
            }
            row.createCell(col).setCellValue(relationShort);
            System.out.println("  Added Relation: " + relationShort);
        }
        col++;

        // inRelationTo - hasco:inRelationTo
        String inRelationTo = obj.getInRelationTo();
        if (inRelationTo != null && !inRelationTo.isEmpty()) {
            String inRelShort;
            if (helper != null) {
                inRelShort = helper.registerAndConvertFullUri(inRelationTo);
            } else {
                inRelShort = URIUtils.replaceNameSpaceEx(inRelationTo);
            }
            row.createCell(col).setCellValue(inRelShort);
            System.out.println("  Added InRelationTo: " + inRelShort);
        }
        col++;

        // wasDerivedFrom - EMPTY for SDDObjects
        col++;

        // wasGeneratedBy - EMPTY for SDDObjects
        col++;

        System.out.println("[SDDDictionaryMapping] SDDObject row added successfully at row " + rowNum);
    }
}
