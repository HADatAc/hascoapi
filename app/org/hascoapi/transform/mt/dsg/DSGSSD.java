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
            headerRow.createCell(4).setCellValue("comment");
            headerRow.createCell(5).setCellValue("label");
            headerRow.createCell(6).setCellValue("definition");
            headerRow.createCell(7).setCellValue("groundingLabel");
            headerRow.createCell(8).setCellValue("hasScope");
            headerRow.createCell(9).setCellValue("hasTimeScope");
            headerRow.createCell(10).setCellValue("hasSpaceScope");
            headerRow.createCell(11).setCellValue("source");
        }
        return helper;
    }

    public static DSGGenHelper addByStudy(DSGGenHelper helper, Study study) {
        if (study == null || study.getUri() == null || study.getUri().isEmpty()) {
            System.out.println("[DSGSSD] WARN: addByStudy called with null/empty study URI; skipping");
            return helper;
        }
        // Ensure SSD sheet exists and has headers
        add(helper, null);
        Sheet ssdSheet = helper.workbook.getSheet(DSGGen.SSD);

        // Fetch SOCs for the study (flexible: search in any named graph)
        java.util.List<StudyObjectCollection> socs = org.hascoapi.entity.pojo.StudyObjectCollection.findStudyObjectCollectionsByStudyFlexible(study.getUri());
        int socCount = (socs == null) ? 0 : socs.size();
        System.out.println("[DSGSSD] Study uri=" + study.getUri() + "; found SOC count=" + socCount);
        if (socs == null || socs.isEmpty()) {
            return helper;
        }

        java.util.HashSet<String> seenHasUri = new java.util.HashSet<>();

        for (StudyObjectCollection soc : socs) {
            if (soc == null) { continue; }
            // Derive SSD row values according to requested header order
            String rawSheetName = deriveSheetName(soc);
            String sheetCell = rawSheetName.isEmpty() ? "" : ("#" + rawSheetName);
            String hasURI = deriveHasURI(soc);
            if (seenHasUri.contains(hasURI)) {
                System.out.println("[DSGSSD] Skipping duplicate SOC hasURI=" + hasURI);
                continue;
            }
            seenHasUri.add(hasURI);

            String type = URIUtils.replacePrefixEx(safe(soc.getTypeUri()));
            String hasSOCReference = safe(soc.getSOCReference());
            String comment = safe(soc.getComment());
            String label = safe(soc.getLabel());
            String definition = ""; // not available on StudyObjectCollection
            String groundingLabel = safe(getGroundingLabelSafe(soc));
            String hasScope = deriveHasUriFromSocUri(soc.getHasScopeUri());
            String hasTimeScope = deriveHasUriList(soc.getTimeScopeUris());
            String hasSpaceScope = deriveHasUriList(soc.getSpaceScopeUris());
            String source = ""; // not available on StudyObjectCollection

            // Append SSD row per new header order
            int rowNum = ssdSheet.getLastRowNum() + 1;
            Row row = ssdSheet.createRow(rowNum);
            row.createCell(0).setCellValue(sheetCell);          // sheet (with '#')
            row.createCell(1).setCellValue(hasURI);             // hasURI
            row.createCell(2).setCellValue(type);               // type
            row.createCell(3).setCellValue(hasSOCReference);    // hasSOCReference
            row.createCell(4).setCellValue(comment);            // comment
            row.createCell(5).setCellValue(label);              // label
            row.createCell(6).setCellValue(definition);         // definition
            row.createCell(7).setCellValue(groundingLabel);     // groundingLabel
            row.createCell(8).setCellValue(hasScope);           // hasScope
            row.createCell(9).setCellValue(hasTimeScope);       // hasTimeScope
            row.createCell(10).setCellValue(hasSpaceScope);     // hasSpaceScope
            row.createCell(11).setCellValue(source);            // source
            System.out.println("[DSGSSD] Added SSD row for SOC uri=" + safe(soc.getUri()) + ", sheetCell=" + sheetCell + ", hasURI=" + hasURI);

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

                // Populate SOC objects (deduplicate by originalID)
                java.util.HashSet<String> seenOriginalIds = new java.util.HashSet<>();
                java.util.List<StudyObject> objects = soc.getObjects();
                int objCount = (objects == null) ? 0 : objects.size();
                System.out.println("[DSGSSD] SOC=" + rawSheetName + "; objects count=" + objCount);
                if (objects != null) {
                    for (StudyObject obj : objects) {
                        if (obj == null) { continue; }
                        String originalId = safe(obj.getOriginalId());
                        if (!originalId.isEmpty() && seenOriginalIds.contains(originalId)) {
                            continue;
                        }
                        seenOriginalIds.add(originalId);
                        int r = socSheet.getLastRowNum() + 1;
                        Row sr = socSheet.createRow(r);
                        sr.createCell(0).setCellValue(originalId);
                        sr.createCell(1).setCellValue(URIUtils.replacePrefixEx(safe(obj.getTypeUri())));
                        // Map object scopes to originalIDs
                        sr.createCell(2).setCellValue(joinOriginalIds(obj.getScopeUris()));
                        sr.createCell(3).setCellValue(joinOriginalIds(obj.getTimeScopeUris()));
                        sr.createCell(4).setCellValue(joinOriginalIds(obj.getSpaceScopeUris()));
                    }
                }
            }
        }

        // Optional: autosize SSD columns for readability
        try {
            for (int c = 0; c <= 11; c++) {
                ssdSheet.autoSizeColumn(c);
            }
        } catch (Throwable t) {
            // ignore sizing issues
        }
        return helper;
    }

    private static String getGroundingLabelSafe(StudyObjectCollection soc) {
        try {
            String gl = soc.getGroundingLabel();
            return gl == null ? "" : gl;
        } catch (Throwable t) {
            return "";
        }
    }

    private static String deriveSheetName(StudyObjectCollection soc) {
        // Prefer the SSD label to produce names like SOC-WEATHER-AT-LOCATION
        String label = safe(soc.getLabel());
        if (label != null && !label.trim().isEmpty()) {
            return ("SOC-" + normalizeForSheet(label));
        }
        // Fallback to groundingLabel, then URI tail
        String gl = safe(getGroundingLabelSafe(soc));
        if (gl != null && !gl.trim().isEmpty()) {
            return ("SOC-" + normalizeForSheet(gl));
        }
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

    private static String deriveHasURI(StudyObjectCollection soc) {
        // Always derive from URI tail to avoid placeholders, strip common OCL_ prefix
        String uri = soc.getUri();
        if (uri != null && !uri.isEmpty()) {
            String tail = lastSegment(uri);
            if (tail.startsWith("OCL_")) {
                tail = tail.substring(4);
            }
            return tail;
        }
        // Fallback to sanitized label
        return sanitizeSheetName(soc.getLabel());
    }

    private static String deriveHasUriFromSocUri(String socUri) {
        if (socUri == null || socUri.isEmpty()) return "";
        String full = URIUtils.replacePrefixEx(socUri);
        String tail = lastSegment(full);
        if (tail.startsWith("OCL_")) tail = tail.substring(4);
        return tail;
    }

    private static String deriveHasUriList(java.util.List<String> uris) {
        if (uris == null || uris.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String u : uris) {
            if (u == null || u.isEmpty()) continue;
            String full = URIUtils.replacePrefixEx(u);
            String tail = lastSegment(full);
            if (tail.startsWith("OCL_")) tail = tail.substring(4);
            if (sb.length() > 0) sb.append(",");
            sb.append(tail);
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
