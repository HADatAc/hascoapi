package org.hascoapi.transform.mt.dsg;

import org.apache.poi.ss.usermodel.*;
import org.hascoapi.entity.pojo.SemanticDataDictionary;
import org.hascoapi.entity.pojo.Study;
import org.hascoapi.entity.pojo.StudyObjectCollection;
import org.hascoapi.utils.URIUtils;

public class DSGSSD {

    public static DSGGenHelper add(DSGGenHelper helper, SemanticDataDictionary sdd) {
        Sheet sheet = helper.workbook.getSheet(DSGGen.SSD);
        if (sheet == null) {
            sheet = helper.workbook.createSheet(DSGGen.SSD);
            // Headers aligned with SSDGenerator.initMapping
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("sheet");
            headerRow.createCell(1).setCellValue("hasURI");
            headerRow.createCell(2).setCellValue("type");
            headerRow.createCell(3).setCellValue("hasSOCReference");
            headerRow.createCell(4).setCellValue("hasRoleLabel");
            headerRow.createCell(5).setCellValue("label");
            headerRow.createCell(6).setCellValue("hasScope");
            headerRow.createCell(7).setCellValue("hasSpaceScope");
            headerRow.createCell(8).setCellValue("hasTimeScope");
            headerRow.createCell(9).setCellValue("hasGroup");
        }
        return helper;
    }

    public static DSGGenHelper addByStudy(DSGGenHelper helper, Study study) {
        // Ensure SSD sheet exists and has headers
        add(helper, null);
        Sheet ssdSheet = helper.workbook.getSheet(DSGGen.SSD);

        // Fetch SOCs for the study
        java.util.List<StudyObjectCollection> socs = StudyObjectCollection.findStudyObjectCollectionsByStudy(study.getUri());
        if (socs == null || socs.isEmpty()) {
            return helper;
        }

        for (StudyObjectCollection soc : socs) {
            // Derive SSD row values according to ingestion mapping
            String sheetName = deriveSheetName(soc);
            String hasURI = deriveHasURI(soc);
            String type = safe(soc.getTypeUri());
            String hasSOCReference = safe(soc.getSOCReference());
            String roleLabel = safe(soc.getRoleUri());
            String label = safe(soc.getLabel());
            String hasScope = safe(soc.getHasScopeUri());
            String hasSpaceScope = joinList(soc.getSpaceScopeUris());
            String hasTimeScope = joinList(soc.getTimeScopeUris());
            String hasGroup = joinList(soc.getGroupUris());

            // Append SSD row
            int rowNum = ssdSheet.getLastRowNum() + 1;
            Row row = ssdSheet.createRow(rowNum);
            row.createCell(0).setCellValue(sheetName); // sheet
            row.createCell(1).setCellValue(hasURI);    // hasURI
            row.createCell(2).setCellValue(type);      // type
            row.createCell(3).setCellValue(hasSOCReference); // hasSOCReference
            row.createCell(4).setCellValue(roleLabel);       // hasRoleLabel
            row.createCell(5).setCellValue(label);           // label
            row.createCell(6).setCellValue(hasScope);        // hasScope
            row.createCell(7).setCellValue(hasSpaceScope);   // hasSpaceScope
            row.createCell(8).setCellValue(hasTimeScope);    // hasTimeScope
            row.createCell(9).setCellValue(hasGroup);        // hasGroup

            // If sheet name provided, create the SOC sheet and populate details
            if (sheetName != null && !sheetName.trim().isEmpty()) {
                Sheet socSheet = helper.workbook.getSheet(sheetName);
                if (socSheet == null) {
                    socSheet = helper.workbook.createSheet(sheetName);
                    Row h = socSheet.createRow(0);
                    h.createCell(0).setCellValue("originalID");
                    h.createCell(1).setCellValue("rdf:type");
                    h.createCell(2).setCellValue("scopeID");
                    h.createCell(3).setCellValue("timeScopeID");
                    h.createCell(4).setCellValue("spaceScopeID");
                }
                int r = socSheet.getLastRowNum() + 1;
                Row sr = socSheet.createRow(r);
                sr.createCell(0).setCellValue(hasSOCReference); // originalID from reference
                sr.createCell(1).setCellValue(type);             // rdf:type
                sr.createCell(2).setCellValue(hasScope);         // scopeID
                sr.createCell(3).setCellValue(hasTimeScope);     // timeScopeID
                sr.createCell(4).setCellValue(hasSpaceScope);    // spaceScopeID
            }
        }
        return helper;
    }

    private static String deriveSheetName(StudyObjectCollection soc) {
        // Prefer a concise, stable sheet name based on SOC reference or label
        if (soc.getSOCReference() != null && !soc.getSOCReference().isEmpty()) {
            return soc.getSOCReference();
        }
        if (soc.getLabel() != null && !soc.getLabel().isEmpty()) {
            return sanitizeSheetName(soc.getLabel());
        }
        // Fallback: use last segment of URI
        String uri = soc.getUri();
        if (uri != null) {
            int idx = uri.lastIndexOf('/');
            if (idx >= 0 && idx + 1 < uri.length()) {
                return sanitizeSheetName(uri.substring(idx + 1));
            }
            return sanitizeSheetName(uri);
        }
        return "SOC";
    }

    private static String deriveHasURI(StudyObjectCollection soc) {
        // Map to SSD hasURI input: ingestion expects a plain ID that SSDGenerator turns into a URI
        // Use the last path segment or a label-like identifier
        String ref = soc.getSOCReference();
        if (ref != null && !ref.isEmpty()) {
            return ref;
        }
        String uri = soc.getUri();
        if (uri != null) {
            int idx = Math.max(uri.lastIndexOf('#'), uri.lastIndexOf('/'));
            if (idx >= 0 && idx + 1 < uri.length()) {
                return uri.substring(idx + 1);
            }
            return uri;
        }
        return sanitizeSheetName(soc.getLabel());
    }

    private static String sanitizeSheetName(String name) {
        if (name == null) return "";
        return name.replace(" ", "-").replace("_", "-");
    }

    private static String joinList(java.util.List<String> list) {
        if (list == null || list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            String item = list.get(i);
            if (item == null) continue;
            sb.append(URIUtils.replacePrefixEx(item));
            if (i < list.size() - 1) sb.append(",");
        }
        return sb.toString();
    }

    private static String safe(String val) { return val == null ? "" : val; }
}
