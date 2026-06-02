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
        }

        // Always enforce original SSD header layout (12 cols, no "source" column)
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            headerRow = sheet.createRow(0);
        }
        headerRow.createCell(0).setCellValue("sheet");
        headerRow.createCell(1).setCellValue("hasURI");
        headerRow.createCell(2).setCellValue("type");
        headerRow.createCell(3).setCellValue("hasSOCReference");
        headerRow.createCell(4).setCellValue("comment");
        headerRow.createCell(5).setCellValue("label");
        headerRow.createCell(6).setCellValue("definition");
        headerRow.createCell(7).setCellValue("role");
        headerRow.createCell(8).setCellValue("groundingLabel");
        headerRow.createCell(9).setCellValue("hasScope");
        headerRow.createCell(10).setCellValue("hasTimeScope");
        headerRow.createCell(11).setCellValue("hasSpaceScope");
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
        java.util.List<StudyObjectCollection> socs = org.hascoapi.entity.pojo.StudyObjectCollection
                .findStudyObjectCollectionsByStudyFlexible(study.getUri());
        int socCount = (socs == null) ? 0 : socs.size();
        System.out.println("[DSGSSD] Study uri=" + study.getUri() + "; found SOC count=" + socCount);
        if (socs == null || socs.isEmpty()) {
            return helper;
        }

        // Regeneration should be deterministic: clear previous rows (keep header).
        // IMPORTANT: clear only after confirming this study has SOC content; otherwise
        // a later empty study call could wipe rows generated for the target DSG study.
        clearSheetDataRows(ssdSheet);

        // Build a set of existing keys to avoid duplicates across previous runs.
        // IMPORTANT: do NOT key on abbreviated hasURI, because it may collapse distinct SOCs (e.g., different OCL_* URIs).
        // We key on the stable SOC URI tail that is written in the hasURI column.
        java.util.HashSet<String> existingKeys = new java.util.HashSet<>();
        for (int r = 1; r <= ssdSheet.getLastRowNum(); r++) {
            Row row = ssdSheet.getRow(r);
            if (row == null) continue;
            String sheetCell0 = getCellString(row.getCell(0));
            String hasUri0 = getCellString(row.getCell(1));
            if (!hasUri0.isEmpty()) {
                existingKeys.add(sheetCell0 + "::" + hasUri0);
            }
        }

        // De-duplicate SOCs by canonical URI before writing any rows.
        // The flexible finder can return duplicates across graphs; those duplicates were causing
        // legitimate SOCs to be skipped later due to repeated keys.
        java.util.LinkedHashMap<String, StudyObjectCollection> uniqueSocsByUri = new java.util.LinkedHashMap<>();
        for (StudyObjectCollection soc : socs) {
            if (soc == null) continue;
            String fullUri = safe(org.hascoapi.utils.URIUtils.replacePrefixEx(soc.getUri()));
            if (fullUri.isEmpty()) {
                continue;
            }
            // keep the first occurrence to preserve stable ordering
            uniqueSocsByUri.putIfAbsent(fullUri, soc);
        }
        if (uniqueSocsByUri.size() != socCount) {
            System.out.println("[DSGSSD] NOTE: SOC list contained duplicates; uniqueByUri=" + uniqueSocsByUri.size() + " original=" + socCount);
        }

        // Deterministic SOC ordering helps keep generated DSG stable and aligned with expected templates.
        java.util.List<StudyObjectCollection> orderedSocs = new java.util.ArrayList<>(uniqueSocsByUri.values());
        orderedSocs.sort((a, b) -> {
            String aKey = deriveHasURI(a);
            String bKey = deriveHasURI(b);
            int rankCmp = Integer.compare(socSortRank(aKey), socSortRank(bKey));
            if (rankCmp != 0) return rankCmp;
            return safe(aKey).compareToIgnoreCase(safe(bKey));
        });

        // Track keys added in this call to avoid adding same SOC twice for this study
        java.util.HashSet<String> seenKeys = new java.util.HashSet<>();

        // Fallback scope for SOC rows when hasScope is missing in triplestore.
        String defaultInstrumentScope = "";
        for (StudyObjectCollection candidate : orderedSocs) {
            String candidateHasUri = deriveHasURI(candidate);
            if (socSortRank(candidateHasUri) == 1) {
                defaultInstrumentScope = candidateHasUri;
                break;
            }
        }

        for (StudyObjectCollection soc : orderedSocs) {
            if (soc == null) {
                continue;
            }

            String rawSheetName = deriveSheetName(soc);
            String sheetCell = rawSheetName.isEmpty() ? "" : ("#" + rawSheetName);

            // hasURI value written to the sheet should be a stable, human-ish identifier.
            // We use the URI tail (after stripping any OCL_ prefix), NOT an abbreviated prefix form.
            String hasURIValue = deriveHasURI(soc);

            // DEDUP KEY: based on sheet + hasURIValue to keep consistent with what we store in the sheet.
            // This prevents false duplicates when different SOCs collapse to the same abbreviated prefix.
            String key = sheetCell + "::" + hasURIValue;

            if (existingKeys.contains(key)) {
                System.out.println("[DSGSSD] Skipping SSD duplicate already in sheet key=" + key);
                continue;
            }
            if (seenKeys.contains(key)) {
                System.out.println("[DSGSSD] Skipping SSD duplicate in this run key=" + key);
                continue;
            }
            seenKeys.add(key);

            String type = URIUtils.replaceNameSpaceEx(safe(soc.getTypeUri()));
            String hasSOCReference = safe(soc.getSOCReference());
            String comment = safe(soc.getComment());
            String label = safe(soc.getLabel());
            String definition = ""; // not available on StudyObjectCollection
            String role = URIUtils.replaceNameSpaceEx(safe(soc.getRoleUri()));
            String groundingLabel = safe(getGroundingLabelSafe(soc));
            String hasScope = URIUtils.replaceNameSpaceEx(deriveHasUriFromSocUri(soc.getHasScopeUri()));
            if (hasScope.isEmpty() && !defaultInstrumentScope.isEmpty()) {
                hasScope = defaultInstrumentScope;
            }
            String hasTimeScope = URIUtils.replaceNameSpaceEx(deriveHasUriList(soc.getTimeScopeUris()));
            String hasSpaceScope = URIUtils.replaceNameSpaceEx(deriveHasUriList(soc.getSpaceScopeUris()));

            int rowNum = ssdSheet.getLastRowNum() + 1;
            Row row = ssdSheet.createRow(rowNum);
            row.createCell(0).setCellValue(sheetCell);
            row.createCell(1).setCellValue(hasURIValue);
            row.createCell(2).setCellValue(type);
            row.createCell(3).setCellValue(hasSOCReference);
            row.createCell(4).setCellValue(comment);
            row.createCell(5).setCellValue(label);
            row.createCell(6).setCellValue(definition);
            row.createCell(7).setCellValue(role);
            row.createCell(8).setCellValue(groundingLabel);
            row.createCell(9).setCellValue(hasScope);
            row.createCell(10).setCellValue(hasTimeScope);
            row.createCell(11).setCellValue(hasSpaceScope);
            System.out.println("[DSGSSD] Added SSD row for SOC uri=" + safe(soc.getUri()) + ", key=" + key);

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
                    h.createCell(5).setCellValue("label");
                    h.createCell(6).setCellValue("comment");
                } else {
                    clearSheetDataRows(socSheet);
                }

                // Populate SOC objects (deduplicate by originalID)
                java.util.HashSet<String> seenOriginalIds = new java.util.HashSet<>();

                // First check if objectUris is populated
                java.util.List<String> objectUris = soc.getObjectUris();
                int uriCount = (objectUris == null) ? 0 : objectUris.size();
                System.out.println("[DSGSSD] SOC=" + rawSheetName + " (uri=" + safe(soc.getUri()) + "); objectUris count=" + uriCount);

                if (objectUris != null && !objectUris.isEmpty()) {
                    System.out.println("[DSGSSD] Object URIs for SOC " + rawSheetName + ":");
                    for (String objUri : objectUris) {
                        System.out.println("[DSGSSD]   - " + objUri);
                    }
                }

                java.util.List<StudyObject> objects = soc.getObjects();
                int objCount = (objects == null) ? 0 : objects.size();
                System.out.println("[DSGSSD] SOC=" + rawSheetName + "; resolved objects count=" + objCount);

                if (objects != null) {
                    // Keep stable row ordering in generated DSG.
                    java.util.List<StudyObject> orderedObjects = new java.util.ArrayList<>(objects);
                    orderedObjects.sort((o1, o2) -> resolveOriginalIdForExport(o1).compareToIgnoreCase(resolveOriginalIdForExport(o2)));

                    for (StudyObject obj : orderedObjects) {
                        if (obj == null) {
                            System.out.println("[DSGSSD] WARNING: null object in list for SOC " + rawSheetName);
                            continue;
                        }
                        String originalId = resolveOriginalIdForExport(obj);
                        System.out.println("[DSGSSD] Processing object: uri=" + safe(obj.getUri()) + ", originalId=" + originalId);

                        if (seenOriginalIds.contains(originalId)) {
                            System.out.println("[DSGSSD] Skipping duplicate originalId: " + originalId);
                            continue;
                        }
                        seenOriginalIds.add(originalId);
                        int r = socSheet.getLastRowNum() + 1;
                        Row sr = socSheet.createRow(r);
                        sr.createCell(0).setCellValue(originalId);
                        sr.createCell(1).setCellValue(URIUtils.replaceNameSpaceEx(URIUtils.replacePrefixEx(safe(obj.getTypeUri()))));
                        
                        // CRITICAL: Load scopes from triplestore explicitly
                        // The DESCRIBE query in find() doesn't include named graphs, so scopes aren't loaded
                        String scopeLookupUri = resolveScopeLookupUri(obj, originalId);
                        java.util.List<String> scopeUris = StudyObject.retrieveScopeUris(scopeLookupUri);
                        java.util.List<String> timeScopeUris = StudyObject.retrieveTimeScopeUris(scopeLookupUri);
                        java.util.List<String> spaceScopeUris = StudyObject.retrieveSpaceScopeUris(scopeLookupUri);

                        if ((spaceScopeUris == null || spaceScopeUris.isEmpty()) && isComponentType(obj)) {
                            String attributeLookupUri = resolveComponentAttributeLookupUri(scopeLookupUri, originalId);
                            java.util.List<String> attributeUris = StudyObject.retrieveAttributeUris(attributeLookupUri);
                            if ((attributeUris == null || attributeUris.isEmpty()) && !attributeLookupUri.equals(scopeLookupUri)) {
                                attributeUris = StudyObject.retrieveAttributeUris(scopeLookupUri);
                            }
                            if (attributeUris != null && !attributeUris.isEmpty()) {
                                spaceScopeUris = attributeUris;
                            }
                        }

                        // Map object scope URIs back to exported original IDs.
                        String scopeIds = joinOriginalIds(scopeUris);
                        String timeScopeIds = joinOriginalIds(timeScopeUris);
                        String spaceScopeIds = joinOriginalIds(spaceScopeUris);
                        sr.createCell(2).setCellValue(scopeIds);
                        sr.createCell(3).setCellValue(timeScopeIds);
                        sr.createCell(4).setCellValue(spaceScopeIds);
                        sr.createCell(5).setCellValue(safe(obj.getLabel()));
                        sr.createCell(6).setCellValue(safe(obj.getComment()));
                        System.out.println("[DSGSSD] Added row for object originalId=" + originalId);
                    }
                } else {
                    System.out.println("[DSGSSD] WARNING: No objects returned from soc.getObjects() for SOC " + rawSheetName);
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

    private static void clearSheetDataRows(Sheet sheet) {
        if (sheet == null) {
            return;
        }
        int lastRow = sheet.getLastRowNum();
        for (int r = lastRow; r >= 1; r--) {
            Row row = sheet.getRow(r);
            if (row != null) {
                sheet.removeRow(row);
            }
        }
    }

    private static String getCellString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:  return safe(cell.getStringCellValue());
            case NUMERIC: return String.valueOf((long)cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            default:      return "";
        }
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
        // Prefer SOC URI tail to preserve canonical naming like SOC-INSTRUMENT-PMSR.
        String hasUri = deriveHasURI(soc);
        if (!hasUri.isEmpty()) {
            return normalizeForSheet(hasUri);
        }
        // Fallback to label/groundingLabel when URI is unavailable.
        String label = safe(soc.getLabel());
        if (!label.trim().isEmpty()) {
            return "SOC-" + normalizeForSheet(label);
        }
        String gl = safe(getGroundingLabelSafe(soc));
        if (!gl.trim().isEmpty()) {
            return "SOC-" + normalizeForSheet(gl);
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
        java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>();
        for (String u : scopeUris) {
            if (u == null || u.isEmpty()) continue;
            String full = URIUtils.replacePrefixEx(u);
            // Retrieve originalId from triplestore (searches all named graphs)
            String original = org.hascoapi.entity.pojo.StudyObject.retrieveOriginalId(full);
            original = normalizeExportScopeId(full, original);
            if (original != null && !original.trim().isEmpty()) {
                ids.add(original.trim());
            }
        }
        if (ids.isEmpty()) {
            return "";
        }
        return String.join(",", ids);
    }

    private static String resolveScopeLookupUri(StudyObject obj, String originalId) {
        String objUri = URIUtils.replacePrefixEx(safe(obj == null ? null : obj.getUri()));
        String cleanOriginalId = safe(originalId).trim();

        // Most robust path: if originalId is already a full/prefixed URI, use it directly.
        String expandedOriginal = URIUtils.replacePrefixEx(cleanOriginalId);
        if (expandedOriginal != null && (expandedOriginal.startsWith("http://") || expandedOriginal.startsWith("https://"))) {
            return expandedOriginal;
        }

        // SOC object lists may return synthetic OBJ_* URIs. Convert to real local ID URI when possible.
        if (!objUri.isEmpty() && objUri.contains("#OBJ_") && !cleanOriginalId.isEmpty()) {
            int idx = objUri.lastIndexOf('#');
            if (idx >= 0 && idx + 1 < objUri.length()) {
                return objUri.substring(0, idx + 1) + cleanOriginalId;
            }
        }

        return objUri;
    }

    private static String normalizeExportScopeId(String fullUri, String originalId) {
        String normalized = normalizeSyntheticObjId(safe(originalId));
        if (!normalized.isEmpty()) {
            return normalized;
        }

        String local = normalizeSyntheticObjId(lastSegment(fullUri));
        if (looksLikeLocalOriginalId(local)) {
            return local;
        }

        String abbreviated = URIUtils.replaceNameSpaceEx(safe(fullUri));
        if (!abbreviated.isEmpty()) {
            return abbreviated;
        }

        return local;
    }

    private static String normalizeSyntheticObjId(String value) {
        String v = safe(value).trim();
        if (!v.startsWith("OBJ_")) {
            return v;
        }
        int idx = v.lastIndexOf('_');
        if (idx >= 0 && idx + 1 < v.length()) {
            return v.substring(idx + 1);
        }
        return v;
    }

    private static boolean looksLikeLocalOriginalId(String value) {
        String v = safe(value);
        if (v.isEmpty()) return false;
        if (v.contains("/CTS/")) return true;
        return v.matches("^(INS|CMP|CST|SLT|COD|ROP|VCO|SOC|STD|STUDY).*");
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

    private static int socSortRank(String hasUri) {
        String u = safe(hasUri).toUpperCase();
        if (u.contains("SOC-INSTRUMENT")) return 1;
        if (u.contains("SOC-COMPONENT-STEM") || u.contains("SOC-COMPONENTSTEM")) return 2;
        if (u.contains("SOC-COMPONENT")) return 3;
        if (u.contains("SOC-SLOT-ELEMENT") || u.contains("SOC-SLOTELEMENT") || u.contains("SOC-CONTAINER-SLOT")) return 4;
        if (u.contains("SOC-CODEBOOK")) return 5;
        if (u.contains("SOC-RESPONSE-OPTION") || u.contains("SOC-RESPONSEOPTION")) return 6;
        return 99;
    }

    private static String resolveOriginalIdForExport(StudyObject obj) {
        if (obj == null) return "";
        String originalId = safe(obj.getOriginalId());
        if (!originalId.isEmpty()) {
            return originalId;
        }
        String fallback = lastSegment(safe(obj.getUri()));
        if (!fallback.isEmpty()) {
            System.out.println("[DSGSSD] WARNING: missing originalID for uri=" + safe(obj.getUri()) + "; using URI tail=" + fallback);
            return fallback;
        }
        return "";
    }

    private static boolean isComponentType(StudyObject obj) {
        if (obj == null) {
            return false;
        }
        String typeUri = URIUtils.replacePrefixEx(safe(obj.getTypeUri())).toLowerCase();
        return typeUri.contains("#component");
    }

    private static String resolveComponentAttributeLookupUri(String lookupUri, String originalId) {
        String expandedLookup = URIUtils.replacePrefixEx(safe(lookupUri));
        String cleanOriginal = safe(originalId).trim();

        if (expandedLookup.contains("#OBJ_componentcollection_")) {
            return expandedLookup;
        }
        if (cleanOriginal.isEmpty()) {
            return expandedLookup;
        }

        if ((cleanOriginal.startsWith("COM") || cleanOriginal.startsWith("CSM")) && expandedLookup.contains("#")) {
            int idx = expandedLookup.lastIndexOf('#');
            if (idx >= 0 && idx + 1 < expandedLookup.length()) {
                return expandedLookup.substring(0, idx + 1) + "OBJ_componentcollection_" + cleanOriginal;
            }
        }

        return expandedLookup;
    }

    private static String safe(String val) { return val == null ? "" : val; }
}
