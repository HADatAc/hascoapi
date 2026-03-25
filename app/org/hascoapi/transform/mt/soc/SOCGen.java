package org.hascoapi.transform.mt.soc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hascoapi.entity.pojo.StudyObject;
import org.hascoapi.entity.pojo.StudyObjectCollection;
import org.hascoapi.utils.ConfigProp;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.utils.CollectionUtil;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;

/**
 * SOCGen - StudyObjectCollection Generator
 *
 * Generates Excel workbooks containing StudyObjectCollection (SOC) data retrieved from the triplestore.
 *
 * Key features:
 * - Queries SOCs from BOTH named graphs AND default graph (to capture orphan elements)
 * - Generates InfoSheet with metadata
 * - Generates Namespaces sheet with referenced ontologies
 * - Generates individual SOC sheets with StudyObject members
 * - Supports filtering by study URI
 * - Handles duplicate detection and canonical URI normalization
 *
 * Pattern follows: DP2Gen, WKFGen, SDDGen conventions
 */
public class SOCGen {

    public static final String INFOSHEET = "InfoSheet";
    public static final String NAMESPACES = "Namespaces";
    public static final int PAGESIZE = 20000;
    public static final int OFFSET = 0;

    /**
     * Generate SOC workbook for a specific study
     *
     * @param studyUri URI of the study whose SOCs to export
     * @param filename Output filename
     * @param mediaFolder Media folder (currently unused but kept for API consistency)
     * @param verifyUri Verify URI format (currently unused but kept for API consistency)
     * @return Path to generated file, or error message starting with "FAILURE:"
     */
    public static String genByStudy(String studyUri, String filename, String mediaFolder, String verifyUri) {
        System.out.println("\n========== SOCGen.genByStudy() START ==========");
        System.out.println("Input parameters:");
        System.out.println("  studyUri: " + studyUri);
        System.out.println("  filename: " + filename);
        System.out.println("  mediaFolder: " + mediaFolder);

        if (studyUri == null || studyUri.trim().isEmpty()) {
            System.err.println("[SOCGen] ERROR: studyUri is required");
            return "FAILURE: studyUri is required";
        }

        String normalizedStudyUri = URIUtils.replacePrefixEx(studyUri.trim());
        System.out.println("  normalized studyUri: " + normalizedStudyUri);

        SOCGenHelper helper = new SOCGenHelper();
        List<StudyObjectCollection> socs = null;

        // Step 1: Fetch ALL SOCs for the study (from both named graphs and default graph)
        try {
            System.out.println("[SOCGen] Fetching SOCs for study: " + normalizedStudyUri);
            socs = fetchSOCsForStudy(normalizedStudyUri);
            System.out.println("[SOCGen] Found " + (socs == null ? 0 : socs.size()) + " SOCs");
        } catch (Throwable t) {
            System.err.println("[SOCGen] ERROR fetching SOCs: " + t.getMessage());
            t.printStackTrace();
            return "FAILURE: fetching SOCs - " + t.getMessage();
        }

        // Step 2: Create workbook structure
        try {
            helper.workbook = create(filename, socs);
            if (helper.workbook == null) {
                System.err.println("[SOCGen] ERROR: workbook creation returned null");
                return "FAILURE: workbook creation returned null";
            }
            System.out.println("[SOCGen] Workbook created");
        } catch (Throwable t) {
            System.err.println("[SOCGen] ERROR creating workbook: " + t.getMessage());
            t.printStackTrace();
            return "FAILURE: creating workbook - " + t.getMessage();
        }

        // Step 3: Populate SOC sheets with members
        if (socs != null && !socs.isEmpty()) {
            System.out.println("[SOCGen] Populating " + socs.size() + " SOC sheets");
            int idx = 0;
            for (StudyObjectCollection soc : socs) {
                idx++;
                if (soc == null) {
                    System.out.println("[SOCGen] WARN: soc[" + idx + "] is null, skipping");
                    continue;
                }
                System.out.println("[SOCGen] Processing soc[" + idx + "] uri=" + soc.getUri() + ", label=" + soc.getLabel());
                try {
                    helper = populateSOCSheet(helper, soc);
                    System.out.println("[SOCGen] SOC sheet populated for soc[" + idx + "]");
                } catch (Throwable t) {
                    System.err.println("[SOCGen] ERROR populating SOC sheet for soc uri=" + soc.getUri() + ": " + t.getMessage());
                    t.printStackTrace();
                }
            }
        } else {
            System.out.println("[SOCGen] No SOCs found; sheet population skipped");
        }

        // Step 4: Prune unused namespaces
        try {
            pruneUnusedNamespaces(helper.workbook);
        } catch (Throwable t) {
            System.err.println("[SOCGen] WARN: failed to prune unused namespaces: " + t.getMessage());
            t.printStackTrace();
        }

        // Step 5: Save workbook
        String saveResult;
        try {
            saveResult = save(helper, filename);
            System.out.println("[SOCGen] Save result=" + saveResult);
        } catch (Throwable t) {
            System.err.println("[SOCGen] ERROR saving workbook: " + t.getMessage());
            t.printStackTrace();
            return "FAILURE: saving workbook - " + t.getMessage();
        }

        System.out.println("========== SOCGen.genByStudy() END ==========\n");
        return saveResult;
    }

    /**
     * Fetch all SOCs for a given study from BOTH named graphs and default graph.
     * This is critical for finding "orphan" SOCs that were inserted without a named graph context.
     */
    private static List<StudyObjectCollection> fetchSOCsForStudy(String studyUri) {
        String ns = NameSpaces.getInstance().printSparqlNameSpaceList();

        // Query pattern: use UNION to search BOTH default graph and all named graphs
        // This matches the pattern in StudyObjectCollection.findStudyObjectCollectionsByStudyFlexible()
        String query = ns +
                "SELECT DISTINCT ?uri WHERE { \n" +
                "  { \n" +
                "    ?socType rdfs:subClassOf* hasco:StudyObjectCollection . \n" +
                "    ?uri a ?socType . \n" +
                "    ?uri hasco:isMemberOf <" + studyUri + "> . \n" +
                "  } UNION { \n" +
                "    GRAPH ?g { \n" +
                "      ?socType rdfs:subClassOf* hasco:StudyObjectCollection . \n" +
                "      ?uri a ?socType . \n" +
                "      ?uri hasco:isMemberOf <" + studyUri + "> . \n" +
                "    } \n" +
                "  } \n" +
                "} ORDER BY ?uri";

        System.out.println("[SOCGen] SOC query:\n" + query);

        // Use GenericFind to fetch and hydrate SOC objects
        List<StudyObjectCollection> allSocs = org.hascoapi.entity.pojo.GenericFind.findByQuery(
                StudyObjectCollection.class, query);

        System.out.println("[SOCGen] Raw query returned " + (allSocs == null ? 0 : allSocs.size()) + " SOCs");

        if (allSocs == null || allSocs.isEmpty()) {
            return new ArrayList<>();
        }

        // Deduplicate by canonical URI (some SOCs may appear in multiple graphs)
        Map<String, StudyObjectCollection> uniqueSocs = new LinkedHashMap<>();
        for (StudyObjectCollection soc : allSocs) {
            if (soc == null || soc.getUri() == null) continue;

            String canonical = canonicalizeUri(soc.getUri());
            if (canonical == null || canonical.isEmpty()) {
                System.out.println("[SOCGen] Skipping SOC with invalid URI: " + soc.getUri());
                continue;
            }

            uniqueSocs.putIfAbsent(canonical, soc);
        }

        System.out.println("[SOCGen] After deduplication: " + uniqueSocs.size() + " unique SOCs");
        return new ArrayList<>(uniqueSocs.values());
    }

    /**
     * Fetch all member StudyObjects for a given SOC from BOTH named graphs and default graph.
     */
    private static List<StudyObject> fetchMembersForSOC(String socUri) {
        if (socUri == null || socUri.isEmpty()) {
            return new ArrayList<>();
        }

        String ns = NameSpaces.getInstance().printSparqlNameSpaceList();

        // Query pattern: UNION to search both default graph and named graphs
        String query = ns +
                "SELECT DISTINCT ?uri WHERE { \n" +
                "  { \n" +
                "    ?uri hasco:isMemberOf <" + socUri + "> . \n" +
                "    FILTER NOT EXISTS { ?uri a ?socType . ?socType rdfs:subClassOf* hasco:StudyObjectCollection . } \n" +
                "  } UNION { \n" +
                "    GRAPH ?g { \n" +
                "      ?uri hasco:isMemberOf <" + socUri + "> . \n" +
                "      FILTER NOT EXISTS { ?uri a ?socType . ?socType rdfs:subClassOf* hasco:StudyObjectCollection . } \n" +
                "    } \n" +
                "  } \n" +
                "} ORDER BY ?uri LIMIT " + PAGESIZE + " OFFSET " + OFFSET;

        System.out.println("[SOCGen] Members query for SOC " + socUri + ":\n" + query);

        // Use GenericFind to fetch StudyObject instances
        List<StudyObject> members = org.hascoapi.entity.pojo.GenericFind.findByQuery(
                StudyObject.class, query);

        System.out.println("[SOCGen] Found " + (members == null ? 0 : members.size()) + " members for SOC " + socUri);

        if (members == null) {
            return new ArrayList<>();
        }

        // Deduplicate members by canonical URI
        Map<String, StudyObject> uniqueMembers = new LinkedHashMap<>();
        for (StudyObject obj : members) {
            if (obj == null || obj.getUri() == null) continue;
            String canonical = canonicalizeUri(obj.getUri());
            if (canonical != null && !canonical.isEmpty()) {
                uniqueMembers.putIfAbsent(canonical, obj);
            }
        }

        System.out.println("[SOCGen] After deduplication: " + uniqueMembers.size() + " unique members");
        return new ArrayList<>(uniqueMembers.values());
    }

    /**
     * Create the workbook structure with InfoSheet and Namespaces
     */
    private static Workbook create(String filename, List<StudyObjectCollection> socs) {
        Workbook wb = new XSSFWorkbook();

        // Create InfoSheet
        Sheet infoSheet = wb.createSheet(INFOSHEET);
        Row r0 = infoSheet.createRow(0);
        r0.createCell(0).setCellValue("Attribute");
        r0.createCell(1).setCellValue("Value");

        // Add basic metadata
        int rowIdx = 1;

        Row r1 = infoSheet.createRow(rowIdx++);
        r1.createCell(0).setCellValue("SOC_Count");
        r1.createCell(1).setCellValue(socs == null ? 0 : socs.size());

        Row r2 = infoSheet.createRow(rowIdx++);
        r2.createCell(0).setCellValue("hasDependencies");
        r2.createCell(1).setCellValue("#Namespaces");

        // Add sheet references for each SOC
        if (socs != null && !socs.isEmpty()) {
            for (StudyObjectCollection soc : socs) {
                if (soc == null) continue;
                String sheetName = deriveSheetName(soc);
                if (!sheetName.isEmpty()) {
                    Row r = infoSheet.createRow(rowIdx++);
                    r.createCell(0).setCellValue(sheetName);
                    r.createCell(1).setCellValue("#" + sheetName);
                }
            }
        }

        // Create Namespaces sheet with headers
        Sheet nsSheet = wb.createSheet(NAMESPACES);
        Row nsHeader = nsSheet.createRow(0);
        nsHeader.createCell(0).setCellValue("hasPrefix");
        nsHeader.createCell(1).setCellValue("hasNameSpace");
        nsHeader.createCell(2).setCellValue("hasFormat");
        nsHeader.createCell(3).setCellValue("hasSource");

        // Populate with commonly used namespaces
        populateCommonNamespaces(nsSheet);

        System.out.println("[SOCGen] Workbook structure created");
        return wb;
    }

    /**
     * Populate a SOC sheet with its member StudyObjects
     */
    private static SOCGenHelper populateSOCSheet(SOCGenHelper helper, StudyObjectCollection soc) {
        String sheetName = deriveSheetName(soc);
        if (sheetName.isEmpty()) {
            System.out.println("[SOCGen] WARNING: Cannot derive sheet name for SOC " + soc.getUri());
            return helper;
        }

        System.out.println("[SOCGen] Creating/updating sheet: " + sheetName);

        Sheet sheet = helper.workbook.getSheet(sheetName);
        if (sheet == null) {
            sheet = helper.workbook.createSheet(sheetName);
            // Create headers matching SSD generator pattern
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("originalID");
            headerRow.createCell(1).setCellValue("rdf:type");
            headerRow.createCell(2).setCellValue("scopeID");
            headerRow.createCell(3).setCellValue("timeScopeID");
            headerRow.createCell(4).setCellValue("spaceScopeID");
        }

        // Fetch members from both default graph and named graphs
        List<StudyObject> members = fetchMembersForSOC(soc.getUri());
        System.out.println("[SOCGen] SOC " + sheetName + " has " + members.size() + " members");

        // Track seen originalIDs to avoid duplicates
        java.util.HashSet<String> seenOriginalIds = new java.util.HashSet<>();

        // Check existing rows in sheet
        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String existingId = getCellString(row.getCell(0));
            if (!existingId.isEmpty()) {
                seenOriginalIds.add(existingId);
            }
        }

        // Add member rows
        int addedCount = 0;
        for (StudyObject obj : members) {
            if (obj == null) continue;

            String originalId = safe(obj.getOriginalId());
            if (originalId.isEmpty()) {
                // Generate originalID from URI if missing
                originalId = lastSegment(obj.getUri());
            }

            if (seenOriginalIds.contains(originalId)) {
                System.out.println("[SOCGen] Skipping duplicate member originalId: " + originalId);
                continue;
            }
            seenOriginalIds.add(originalId);

            int rowNum = sheet.getLastRowNum() + 1;
            Row row = sheet.createRow(rowNum);

            row.createCell(0).setCellValue(originalId);
            row.createCell(1).setCellValue(URIUtils.replaceNameSpaceEx(URIUtils.replacePrefixEx(safe(obj.getTypeUri()))));
            row.createCell(2).setCellValue(joinOriginalIds(obj.getScopeUris()));
            row.createCell(3).setCellValue(joinOriginalIds(obj.getTimeScopeUris()));
            row.createCell(4).setCellValue(joinOriginalIds(obj.getSpaceScopeUris()));

            addedCount++;
            System.out.println("[SOCGen] Added member: originalId=" + originalId + ", type=" + obj.getTypeUri());
        }

        System.out.println("[SOCGen] Added " + addedCount + " members to sheet " + sheetName);

        // Auto-size columns for readability
        try {
            for (int c = 0; c <= 4; c++) {
                sheet.autoSizeColumn(c);
            }
        } catch (Throwable t) {
            // Ignore sizing issues
        }

        return helper;
    }

    /**
     * Save the workbook to disk
     */
    private static String save(SOCGenHelper helper, String filename) throws IOException {
        System.out.println("\n========== SOCGen.save() START ==========");
        System.out.println("  filename parameter: " + filename);

        if (helper == null || helper.workbook == null) {
            System.err.println("[SOCGen] ERROR: helper or workbook is null");
            throw new IOException("Helper or workbook is null");
        }
        System.out.println("  ✓ helper and workbook are valid");

        String basePath = ConfigProp.getPathIngestion();
        if (basePath == null || basePath.trim().isEmpty()) {
            basePath = "C:/hascoapi/var/";
        }

        // Normalize path separators
        basePath = basePath.replace("\\", "/");
        if (!basePath.endsWith("/")) {
            basePath += "/";
        }

        Path path = Paths.get(basePath + filename).toAbsolutePath();
        String absolutePath = path.toString();
        System.out.println("  Output file absolute path: " + absolutePath);

        File parentDir = path.getParent().toFile();
        System.out.println("  Parent directory: " + parentDir.getAbsolutePath());
        System.out.println("  Parent directory exists: " + parentDir.exists());

        if (!parentDir.exists()) {
            System.out.println("  Creating parent directory...");
            parentDir.mkdirs();
        }

        System.out.println("  ✓ Writing workbook to file...");
        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            helper.workbook.write(fos);
        }

        System.out.println("  ✓ File written successfully");
        File savedFile = path.toFile();
        System.out.println("  File size: " + savedFile.length() + " bytes");
        System.out.println("  File exists: " + savedFile.exists());
        System.out.println("  File can read: " + savedFile.canRead());

        System.out.println("========== SOCGen.save() END (SUCCESS) ==========\n");
        return absolutePath;
    }

    /**
     * Populate common namespaces that are typically used in SOC files
     */
    private static void populateCommonNamespaces(Sheet nsSheet) {
        addNamespace(nsSheet, "hasco", "http://hadatac.org/ont/hasco/", "text/turtle", "http://hadatac.org/ont/hasco/");
        addNamespace(nsSheet, "rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#", "text/turtle", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        addNamespace(nsSheet, "rdfs", "http://www.w3.org/2000/01/rdf-schema#", "text/turtle", "http://www.w3.org/2000/01/rdf-schema#");
        addNamespace(nsSheet, "vstoi", "http://hadatac.org/ont/vstoi#", "text/turtle", "http://hadatac.org/ont/vstoi#");
        addNamespace(nsSheet, "owl", "http://www.w3.org/2002/07/owl#", "text/turtle", "http://www.w3.org/2002/07/owl#");
        addNamespace(nsSheet, "xsd", "http://www.w3.org/2001/XMLSchema#", "text/turtle", "http://www.w3.org/2001/XMLSchema#");
        addNamespace(nsSheet, "prov", "http://www.w3.org/ns/prov#", "text/turtle", "http://www.w3.org/ns/prov#");
    }

    private static void addNamespace(Sheet sheet, String prefix, String namespace, String format, String source) {
        int rowNum = sheet.getLastRowNum() + 1;
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(prefix);
        row.createCell(1).setCellValue(namespace);
        row.createCell(2).setCellValue(format);
        row.createCell(3).setCellValue(source);
    }

    /**
     * Prune namespaces that are not actually used in the workbook
     */
    private static void pruneUnusedNamespaces(Workbook wb) {
        Sheet nsSheet = wb.getSheet(NAMESPACES);
        if (nsSheet == null) return;

        // Collect all text from all sheets except Namespaces
        StringBuilder allContent = new StringBuilder();
        for (int s = 0; s < wb.getNumberOfSheets(); s++) {
            Sheet sheet = wb.getSheetAt(s);
            if (sheet.getSheetName().equals(NAMESPACES)) continue;

            for (Row row : sheet) {
                for (Cell cell : row) {
                    if (cell.getCellType() == CellType.STRING) {
                        allContent.append(cell.getStringCellValue()).append(" ");
                    }
                }
            }
        }
        String content = allContent.toString();

        // Keep track of prefixes to keep
        java.util.HashSet<String> keptPrefixes = new java.util.HashSet<>();
        // Always keep core ontology prefixes
        keptPrefixes.add("hasco");
        keptPrefixes.add("rdf");
        keptPrefixes.add("rdfs");
        keptPrefixes.add("vstoi");
        keptPrefixes.add("owl");
        keptPrefixes.add("xsd");
        keptPrefixes.add("prov");

        // Check which prefixes are actually used
        int lastRow = nsSheet.getLastRowNum();
        for (int r = lastRow; r >= 1; r--) {
            Row row = nsSheet.getRow(r);
            if (row == null) continue;

            Cell prefixCell = row.getCell(0);
            if (prefixCell == null) continue;

            String prefix = getCellString(prefixCell);
            if (prefix.isEmpty()) continue;

            // Check if this prefix is used anywhere in the content
            if (content.contains(prefix + ":") || keptPrefixes.contains(prefix)) {
                keptPrefixes.add(prefix);
            } else {
                // Remove this row
                nsSheet.removeRow(row);
            }
        }

        // Compact the sheet (remove gaps from deleted rows)
        int writeRow = 1;
        for (int readRow = 1; readRow <= lastRow; readRow++) {
            Row row = nsSheet.getRow(readRow);
            if (row != null && writeRow != readRow) {
                nsSheet.removeRow(nsSheet.getRow(writeRow));
                Row newRow = nsSheet.createRow(writeRow);
                copyRow(row, newRow);
                nsSheet.removeRow(row);
            }
            if (row != null) {
                writeRow++;
            }
        }

        System.out.println("[SOCGen] Namespaces pruning: keptPrefixes=" + keptPrefixes + " removedRows=" + (lastRow - (writeRow - 1)));
    }

    private static void copyRow(Row source, Row target) {
        for (int c = 0; c < source.getLastCellNum(); c++) {
            Cell sourceCell = source.getCell(c);
            if (sourceCell == null) continue;
            Cell targetCell = target.createCell(c);
            switch (sourceCell.getCellType()) {
                case STRING:
                    targetCell.setCellValue(sourceCell.getStringCellValue());
                    break;
                case NUMERIC:
                    targetCell.setCellValue(sourceCell.getNumericCellValue());
                    break;
                case BOOLEAN:
                    targetCell.setCellValue(sourceCell.getBooleanCellValue());
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Derive a sheet name from SOC properties
     */
    private static String deriveSheetName(StudyObjectCollection soc) {
        // Prefer the SOC label
        String label = safe(soc.getLabel());
        if (label != null && !label.trim().isEmpty()) {
            return "SOC-" + normalizeForSheet(label);
        }

        // Fallback to grounding label
        String gl = safe(soc.getGroundingLabel());
        if (gl != null && !gl.trim().isEmpty()) {
            return "SOC-" + normalizeForSheet(gl);
        }

        // Last resort: URI tail
        String tail = lastSegment(soc.getUri());
        if (tail != null && !tail.isEmpty()) {
            return "SOC-" + normalizeForSheet(tail);
        }

        return "SOC-UNNAMED";
    }

    private static String normalizeForSheet(String s) {
        if (s == null) return "";
        // Excel sheet names: max 31 chars, no special chars
        String normalized = s.trim()
                .replaceAll("\\s+", "-")
                .replace("_", "-")
                .toUpperCase()
                .replaceAll("[\\[\\]\\*\\?/\\\\]", "");
        if (normalized.length() > 31) {
            normalized = normalized.substring(0, 31);
        }
        return normalized;
    }

    /**
     * Join original IDs from a list of scope URIs
     */
    private static String joinOriginalIds(List<String> scopeUris) {
        if (scopeUris == null || scopeUris.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (String uri : scopeUris) {
            if (uri == null || uri.isEmpty()) continue;

            String full = URIUtils.replacePrefixEx(uri);
            StudyObject scopeObj = StudyObject.find(full);
            String originalId = scopeObj != null ? scopeObj.getOriginalId() : lastSegment(full);
            if (originalId == null) originalId = lastSegment(full);

            if (sb.length() > 0) sb.append(",");
            sb.append(originalId);
        }
        return sb.toString();
    }

    /**
     * Canonicalize URI for deduplication (lowercase, strip angle brackets)
     */
    private static String canonicalizeUri(String uri) {
        if (uri == null || uri.isEmpty()) return null;
        try {
            String clean = URIUtils.stripAngleBrackets(uri).trim().toLowerCase();
            if (clean.isEmpty()) return null;
            return clean;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract last segment from URI (after # or /)
     */
    private static String lastSegment(String uri) {
        if (uri == null) return "";
        int idx = Math.max(uri.lastIndexOf('#'), uri.lastIndexOf('/'));
        if (idx >= 0 && idx + 1 < uri.length()) {
            return uri.substring(idx + 1);
        }
        return uri;
    }

    /**
     * Safe string accessor (null -> empty string)
     */
    private static String safe(String val) {
        return val == null ? "" : val;
    }

    /**
     * Get cell value as string
     */
    private static String getCellString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return safe(cell.getStringCellValue());
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }
}

