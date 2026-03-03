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
     * inRelationTo, wasDerivedFrom, wasGeneratedBy, hasPosition
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
        headerRow.createCell(col++).setCellValue("hasPosition");
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
            helper.registerPrefixFromUri(column);
        }
        col++;

        // Attribute - the ontological property URI
        String attribute = attr.getAttributeUri();
        if (attribute != null && !attribute.isEmpty()) {
            String attrShort = URIUtils.replacePrefixEx(attribute);
            row.createCell(col).setCellValue(attrShort);
            helper.registerPrefixFromUri(attrShort);
        }
        col++;

        // attributeOf - the entity this attribute describes
        String attributeOf = attr.getAttributeOf();
        if (attributeOf != null && !attributeOf.isEmpty()) {
            String attrOfShort = URIUtils.replacePrefixEx(attributeOf);
            row.createCell(col).setCellValue(attrOfShort);
            helper.registerPrefixFromUri(attrOfShort);
        }
        col++;

        // Unit - the unit of measure
        String unit = attr.getUnit();
        if (unit != null && !unit.isEmpty()) {
            String unitShort = URIUtils.replacePrefixEx(unit);
            row.createCell(col).setCellValue(unitShort);
            helper.registerPrefixFromUri(unitShort);
        }
        col++;

        // Time - the time entity
        String time = attr.getTime();
        if (time != null && !time.isEmpty()) {
            String timeShort = URIUtils.replacePrefixEx(time);
            row.createCell(col).setCellValue(timeShort);
            helper.registerPrefixFromUri(timeShort);
        }
        col++;

        // Entity - the ontological class of the entity
        String entity = attr.getEntity();
        if (entity != null && !entity.isEmpty()) {
            String entityShort = URIUtils.replacePrefixEx(entity);
            row.createCell(col).setCellValue(entityShort);
            helper.registerPrefixFromUri(entityShort);
        }
        col++;

        // Role - the role of the entity
        String role = attr.getRole();
        if (role != null && !role.isEmpty()) {
            String roleShort = URIUtils.replacePrefixEx(role);
            row.createCell(col).setCellValue(roleShort);
            helper.registerPrefixFromUri(roleShort);
        }
        col++;

        // Relation - the relationship to other entities
        String relation = attr.getRelation();
        if (relation != null && !relation.isEmpty()) {
            String relationShort = URIUtils.replacePrefixEx(relation);
            row.createCell(col).setCellValue(relationShort);
            helper.registerPrefixFromUri(relationShort);
        }
        col++;

        // inRelationTo - the target of the relation
        String inRelationTo = attr.getInRelationTo();
        if (inRelationTo != null && !inRelationTo.isEmpty()) {
            String inRelShort = URIUtils.replacePrefixEx(inRelationTo);
            row.createCell(col).setCellValue(inRelShort);
            helper.registerPrefixFromUri(inRelShort);
        }
        col++;

        // wasDerivedFrom - provenance
        String wasDerivedFrom = attr.getWasDerivedFrom();
        if (wasDerivedFrom != null && !wasDerivedFrom.isEmpty()) {
            String derivedShort = URIUtils.replacePrefixEx(wasDerivedFrom);
            row.createCell(col).setCellValue(derivedShort);
            helper.registerPrefixFromUri(derivedShort);
        }
        col++;

        // wasGeneratedBy - provenance
        String wasGeneratedBy = attr.getWasGeneratedBy();
        if (wasGeneratedBy != null && !wasGeneratedBy.isEmpty()) {
            String generatedShort = URIUtils.replacePrefixEx(wasGeneratedBy);
            row.createCell(col).setCellValue(generatedShort);
            helper.registerPrefixFromUri(generatedShort);
        }
        col++;

        // hasPosition - the position in the data file
        String hasPosition = attr.getHasPosition();
        if (hasPosition != null && !hasPosition.isEmpty()) {
            row.createCell(col).setCellValue(hasPosition);
        }
        col++;
    }
}

