package org.hascoapi.transform.mt.dsg;

import org.apache.poi.ss.usermodel.*;
import org.hascoapi.entity.pojo.SemanticDataDictionary;
import org.hascoapi.entity.pojo.Study;
import org.hascoapi.entity.pojo.StudyObject;
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
            String rawSheetName = deriveSheetName(soc);
            String sheetCell = rawSheetName.isEmpty() ? "" : ("#" + rawSheetName);
            String hasURI = deriveHasURI(soc);
            String type = URIUtils.replacePrefixEx(safe(soc.getTypeUri()));
            String hasSOCReference = safe(soc.getSOCReference());
            String roleLabel = ""; // left blank; can be filled from role if needed
            String label = safe(soc.getLabel());
            String hasScope = deriveHasUriFromSocUri(soc.getHasScopeUri());
            String hasSpaceScope = deriveHasUriList(soc.getSpaceScopeUris());
            String hasTimeScope = deriveHasUriList(soc.getTimeScopeUris());
            String hasGroup = deriveHasUriList(soc.getGroupUris());

            // Append SSD row
            int rowNum = ssdSheet.getLastRowNum() + 1;
            Row row = ssdSheet.createRow(rowNum);
            row.createCell(0).setCellValue(sheetCell);      // sheet (with '#')
            row.createCell(1).setCellValue(hasURI);         // hasURI (plain id)
            row.createCell(2).setCellValue(type);           // type (prefixed)
            row.createCell(3).setCellValue(hasSOCReference);// hasSOCReference
            row.createCell(4).setCellValue(roleLabel);      // hasRoleLabel
            row.createCell(5).setCellValue(label);          // label
            row.createCell(6).setCellValue(hasScope);       // hasScope (plain id of referenced SOC)
            row.createCell(7).setCellValue(hasSpaceScope);  // hasSpaceScope (CSV of plain ids)
            row.createCell(8).setCellValue(hasTimeScope);   // hasTimeScope (CSV of plain ids)
            row.createCell(9).setCellValue(hasGroup);       // hasGroup

            // If sheet cell provided, create the SOC sheet and populate details
            if (!rawSheetName.isEmpty()) {
                Sheet socSheet = helper.workbook.getSheet(rawSheetName);
                if (socSheet == null) {
                    socSheet = helper.workbook.createSheet(rawSheetName);
                    Row h = socSheet.createRow(0);
                    h.createCell(0).setCellValue("originalID");
                    h.createCell(1).setCellValue("rdf:type");
                    h.createCell(2).setCellValue("scopeID");
                    h.createCell(3).setCellValue("timeScopeID");
                    h.createCell(4).setCellValue("spaceScopeID");
                }

                // Populate SOC objects
                java.util.List<StudyObject> objects = soc.getObjects();
                if (objects != null) {
                    for (StudyObject obj : objects) {
                        int r = socSheet.getLastRowNum() + 1;
                        Row sr = socSheet.createRow(r);
                        sr.createCell(0).setCellValue(safe(obj.getOriginalId()));
                        sr.createCell(1).setCellValue(URIUtils.replacePrefixEx(safe(obj.getTypeUri())));
                        // Map object scopes to originalIDs
                        sr.createCell(2).setCellValue(joinOriginalIds(obj.getScopeUris()));
                        sr.createCell(3).setCellValue(joinOriginalIds(obj.getTimeScopeUris()));
                        sr.createCell(4).setCellValue(joinOriginalIds(obj.getSpaceScopeUris()));
                    }
                }
            }
        }
        return helper;
    }

    private static String deriveSheetName(StudyObjectCollection soc) {
        // Prefer a human-friendly, stable sheet name following examples: SOC-LOCATION, SOC-LOCATION-TYPE
        // 1) groundingLabel if available (it usually mirrors SSD label), else label
        String label = null;
        try {
            label = soc.getGroundingLabel();
        } catch (Throwable t) {
            // ignore; fallback to label
        }
        if (label == null || label.trim().isEmpty()) {
            label = soc.getLabel();
        }
        if (label != null && !label.trim().isEmpty()) {
            return ("SOC-" + normalizeForSheet(label));
        }
        // 2) Avoid using SOCReference if it looks like a variable placeholder (e.g., ??location)
        String ref = soc.getSOCReference();
        if (ref != null && !ref.trim().isEmpty() && !ref.startsWith("??")) {
            return ("SOC-" + normalizeForSheet(ref));
        }
        // 3) Fallback to URI tail
        String tail = lastSegment(soc.getUri());
        if (tail != null && !tail.isEmpty()) {
            return ("SOC-" + normalizeForSheet(tail));
        }
        return "SOC-UNNAMED";
    }

    private static String normalizeForSheet(String s) {
        if (s == null) return "";
        return s.trim().replaceAll("\\s+", "-").replace("_", "-").toUpperCase();
    }

    private static String deriveHasUriFromSocUri(String socUri) {
        if (socUri == null || socUri.isEmpty()) return "";
        StudyObjectCollection ref = StudyObjectCollection.find(URIUtils.replacePrefixEx(socUri));
        if (ref == null) return "";
        return deriveHasURI(ref);
    }

    private static String deriveHasURI(StudyObjectCollection soc) {
        // Map to SSD hasURI input: ingestion expects a plain ID that SSDGenerator turns into a URI
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

    private static String deriveHasUriList(java.util.List<String> uris) {
        if (uris == null || uris.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String u : uris) {
            if (u == null || u.isEmpty()) continue;
            StudyObjectCollection soc = StudyObjectCollection.find(URIUtils.replacePrefixEx(u));
            if (soc == null) continue;
            if (sb.length() > 0) sb.append(",");
            sb.append(deriveHasURI(soc));
        }
        return sb.toString();
    }

    private static String joinOriginalIds(java.util.List<String> scopeUris) {
        if (scopeUris == null || scopeUris.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String u : scopeUris) {
            if (u == null || u.isEmpty()) continue;
            String full = URIUtils.replacePrefixEx(u);
            org.hascoapi.entity.pojo.StudyObject scopeObj = org.hascoapi.entity.pojo.StudyObject.find(full);
            String original = scopeObj != null ? scopeObj.getOriginalId() : lastSegment(full);
            if (original == null) original = lastSegment(full);
            if (sb.length() > 0) sb.append(",");
            sb.append(original);
        }
        return sb.toString();
    }

    private static String lastSegment(String uri) {
        if (uri == null) return "";
        int idx = Math.max(uri.lastIndexOf('#'), uri.lastIndexOf('/'));
        if (idx >= 0 && idx + 1 < uri.length()) return uri.substring(idx + 1);
        return uri;
    }

    private static String sanitizeSheetName(String name) {
        if (name == null) return "";
        return name.replace(" ", "-").replace("_", "-");
    }

    private static String safe(String val) { return val == null ? "" : val; }
}
