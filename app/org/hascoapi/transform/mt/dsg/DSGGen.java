package org.hascoapi.transform.mt.dsg;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.hascoapi.entity.pojo.Study;
import org.hascoapi.entity.pojo.GenericFindWithStatus;
import org.hascoapi.entity.pojo.NameSpace;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hascoapi.utils.URIUtils;

/*
DSGGen builds an Excel workbook for studies:
genByStatus queries studies by status,
create initializes the workbook (InfoSheet + SSD/STD/VD + Namespaces),
STD/SSD rows are added per study,
InfoSheet references are set (first study URI/title and first namespace URI),
Namespaces is populated from the helper map or in-memory namespaces, and save writes and closes the file.
*/


public class DSGGen {

    public static final String INFOSHEET            = "InfoSheet";
    public static final String NAMESPACES           = "Namespaces";
    public static final String SSD                  = "SSD";
    public static final String STD                  = "STD";
    public static final String VD                   = "VD";

    public static final int PAGESIZE                = 20000;
    public static final int OFFSET                  = 0;

    public static String genByStatus(String status, String filename, String mediaFolder, String verifyUri) {
        System.out.println("[DSGGen] genByStatus START status=" + status + ", filename=" + filename);
        DSGGenHelper helper = new DSGGenHelper();
        List<Study> studies = null;
        try {
            // 1) Buscar todos os estudos por tipo
            String diagQuery = org.hascoapi.utils.NameSpaces.getInstance().printSparqlNameSpaceList()
                    + " SELECT ?uri WHERE { "
                    + "   ?studyType rdfs:subClassOf* hasco:Study . "
                    + "   ?uri a ?studyType . "
                    + " }";
            System.out.println("[DSGGen] Diagnostic SPARQL (all studies):\n" + diagQuery);
            java.util.List<Study> allStudies = org.hascoapi.entity.pojo.GenericFind.findByQuery(Study.class, diagQuery);
            System.out.println("[DSGGen] Diagnostic: total studies found=" + (allStudies == null ? 0 : allStudies.size()));

            String requestedStatus = status == null ? "" : status.trim();
            String draftStatus = org.hascoapi.vocabularies.VSTOI.DRAFT;

            // Normalize/deduplicate studies by canonical URI (skip malformed IRIs)
            java.util.Map<String, Study> byCanonicalUri = new java.util.LinkedHashMap<>();

            if (allStudies != null) {
                for (Study s : allStudies) {
                    if (s == null || s.getUri() == null) {
                        continue;
                    }

                    String canonicalUri = canonicalizeStudyUri(s.getUri());
                    if (canonicalUri == null || canonicalUri.isEmpty()) {
                        System.out.println("  [DSGGen] Study diag: uri=" + s.getUri() + " (INVALID/UNUSABLE URI) -> skipped");
                        continue;
                    }

                    // Apply status filter using effective status
                    String rawStatus = s.getHasStatus();
                    String effectiveStatus = (rawStatus == null || rawStatus.isEmpty()) ? draftStatus : rawStatus;

                    System.out.println("  [DSGGen] Study diag: uri=" + s.getUri()
                            + " (canonical=" + canonicalUri + ")"
                            + ", title=" + s.getTitle()
                            + ", rawStatus=" + rawStatus
                            + ", effectiveStatus=" + effectiveStatus);

                    boolean include;
                    if (requestedStatus.isEmpty()) {
                        include = true;
                    } else {
                        include = effectiveStatus.equals(requestedStatus);
                    }
                    if (!include) {
                        continue;
                    }

                    // Prefer http(s) variant if any duplicates map to same canonical
                    Study existing = byCanonicalUri.get(canonicalUri);
                    if (existing == null) {
                        byCanonicalUri.put(canonicalUri, s);
                    } else {
                        String existingUri = existing.getUri() == null ? "" : existing.getUri();
                        if (!existingUri.startsWith("http") && canonicalUri.startsWith("http")) {
                            byCanonicalUri.put(canonicalUri, s);
                        }
                    }
                }
            }

            studies = new java.util.ArrayList<>(byCanonicalUri.values());
            System.out.println("[DSGGen] Filtered+normalized studies by status; count=" + (studies == null ? 0 : studies.size()));
        } catch (Throwable t) {
            System.err.println("[DSGGen] ERROR fetching studies: " + t.getMessage());
            t.printStackTrace();
            return "FAILURE: fetching studies - " + t.getMessage();
        }

        try {
            helper.workbook = DSGGen.create(filename, studies);
            if (helper.workbook == null) {
                System.err.println("[DSGGen] ERROR: workbook creation returned null");
                return "FAILURE: workbook creation returned null";
            }
            System.out.println("[DSGGen] Workbook created");
        } catch (Throwable t) {
            System.err.println("[DSGGen] ERROR creating workbook: " + t.getMessage());
            t.printStackTrace();
            return "FAILURE: creating workbook - " + t.getMessage();
        }

        if (studies != null && !studies.isEmpty()) {
            System.out.println("[DSGGen] Iterating studies to populate STD/SSD");
            int idx = 0;
            for (Study study: studies) {
                idx++;
                if (study == null) {
                    System.out.println("[DSGGen] WARN: study[" + idx + "] is null, skipping");
                    continue;
                }
                System.out.println("[DSGGen] Processing study[" + idx + "] uri=" + study.getUri() + ", title=" + study.getTitle());
                try {
                    helper = DSGSTD.add(helper, study);
                    System.out.println("[DSGGen] STD added for study[" + idx + "]");
                } catch (Throwable t) {
                    System.err.println("[DSGGen] ERROR adding STD for study uri=" + study.getUri() + ": " + t.getMessage());
                    t.printStackTrace();
                }
                try {
                    helper = DSGSSD.addByStudy(helper, study);
                    System.out.println("[DSGGen] SSD added for study[" + idx + "]");
                } catch (Throwable t) {
                    System.err.println("[DSGGen] ERROR adding SSD for study uri=" + study.getUri() + ": " + t.getMessage());
                    t.printStackTrace();
                }
            }
        } else {
            System.out.println("[DSGGen] No studies found; STD/SSD population skipped");
        }

        // After populating the workbook, keep only the namespaces that are actually referenced.
        try {
            pruneUnusedNamespaces(helper.workbook);
        } catch (Throwable t) {
            System.err.println("[DSGGen] WARN: failed to prune unused namespaces: " + t.getMessage());
            t.printStackTrace();
        }

        String saveResult;
        try {
            saveResult = DSGGen.save(helper, filename);
            System.out.println("[DSGGen] Save result=" + saveResult);
        } catch (Throwable t) {
            System.err.println("[DSGGen] ERROR saving workbook: " + t.getMessage());
            t.printStackTrace();
            return "FAILURE: saving workbook - " + t.getMessage();
        }
        System.out.println("[DSGGen] genByStatus END");
        return saveResult;
    }

    public static String genByStudy(Study study, String filename, String mediaFolder, String verifyUri) {
        if (study == null) {
            System.err.println("[DSGGen] ERROR: study is null");
            return "FAILURE: study is null";
        }
        System.out.println("[DSGGen] genByStudy START filename=" + filename + ", studyUri=" + study.getUri());
        DSGGenHelper helper = new DSGGenHelper();
        try {
            java.util.List<Study> studies = new java.util.ArrayList<>();
            studies.add(study);
            helper.workbook = DSGGen.create(filename, studies);
            if (helper.workbook == null) {
                System.err.println("[DSGGen] ERROR: workbook creation returned null");
                return "FAILURE: workbook creation returned null";
            }
            System.out.println("[DSGGen] Workbook created for single study uri=" + study.getUri());
        } catch (Throwable t) {
            System.err.println("[DSGGen] ERROR creating workbook: " + t.getMessage());
            t.printStackTrace();
            return "FAILURE: creating workbook - " + t.getMessage();
        }

        try {
            helper = DSGSTD.add(helper, study);
            System.out.println("[DSGGen] STD added");
        } catch (Throwable t) {
            System.err.println("[DSGGen] ERROR adding STD: " + t.getMessage());
            t.printStackTrace();
        }
        try {
            helper = DSGSSD.addByStudy(helper, study);
            System.out.println("[DSGGen] SSD added");
        } catch (Throwable t) {
            System.err.println("[DSGGen] ERROR adding SSD: " + t.getMessage());
            t.printStackTrace();
        }

        // After populating the workbook, keep only the namespaces that are actually referenced.
        try {
            pruneUnusedNamespaces(helper.workbook);
        } catch (Throwable t) {
            System.err.println("[DSGGen] WARN: failed to prune unused namespaces: " + t.getMessage());
            t.printStackTrace();
        }

        String saveResult;
        try {
            saveResult = DSGGen.save(helper, filename);
            System.out.println("[DSGGen] Save result=" + saveResult);
        } catch (Throwable t) {
            System.err.println("[DSGGen] ERROR saving workbook: " + t.getMessage());
            t.printStackTrace();
            return "FAILURE: saving workbook - " + t.getMessage();
        }
        System.out.println("[DSGGen] genByStudy END");
        return saveResult;
    }

    public static String genByManager(String useremail, String status, String filename, String mediaFolder, String verifyUri, boolean generateDASOCs) {
        System.out.println("[DSGGen] genByManager START status=" + status + ", useremail=" + useremail + ", filename=" + filename + ", generateDASOCs=" + generateDASOCs);
        DSGGenHelper helper = new DSGGenHelper();
        java.util.List<Study> studies = null;
        boolean withCurrent = false; // retrieve just the elements of the requested status
        try {
            GenericFindWithStatus<Study> studyQuery = new GenericFindWithStatus<>();
            System.out.println("[DSGGen] Querying studies by manager with pageSize=" + PAGESIZE + ", offset=" + OFFSET);
            studies = (java.util.List<Study>) (java.util.List<?>) studyQuery.findByStatusManagerEmailWithPages(Study.class, status, useremail, withCurrent, PAGESIZE, OFFSET);
            System.out.println("[DSGGen] Retrieved studies count=" + (studies == null ? 0 : studies.size()));
        } catch (Throwable t) {
            System.err.println("[DSGGen] ERROR fetching studies by manager: " + t.getMessage());
            t.printStackTrace();
            return "FAILURE: fetching studies by manager - " + t.getMessage();
        }

        try {
            helper.workbook = DSGGen.create(filename, studies);
            if (helper.workbook == null) {
                System.err.println("[DSGGen] ERROR: workbook creation returned null");
                return "FAILURE: workbook creation returned null";
            }
            System.out.println("[DSGGen] Workbook created");
        } catch (Throwable t) {
            System.err.println("[DSGGen] ERROR creating workbook: " + t.getMessage());
            t.printStackTrace();
            return "FAILURE: creating workbook - " + t.getMessage();
        }

        if (studies != null && !studies.isEmpty()) {
            System.out.println("[DSGGen] Iterating studies to populate STD/SSD");
            int idx = 0;
            for (Study study : studies) {
                idx++;
                if (study == null) {
                    System.out.println("[DSGGen] WARN: study[" + idx + "] is null, skipping");
                    continue;
                }
                System.out.println("[DSGGen] Processing study[" + idx + "] uri=" + study.getUri() + ", title=" + study.getTitle());
                try {
                    helper = DSGSTD.add(helper, study);
                    System.out.println("[DSGGen] STD added for study[" + idx + "]");
                } catch (Throwable t) {
                    System.err.println("[DSGGen] ERROR adding STD for study uri=" + study.getUri() + ": " + t.getMessage());
                    t.printStackTrace();
                }
                try {
                    helper = DSGSSD.addByStudy(helper, study);
                    System.out.println("[DSGGen] SSD added for study[" + idx + "]");
                } catch (Throwable t) {
                    System.err.println("[DSGGen] ERROR adding SSD for study uri=" + study.getUri() + ": " + t.getMessage());
                    t.printStackTrace();
                }
            }
        } else {
            System.out.println("[DSGGen] No studies found for manager/status; STD/SSD population skipped");
        }

        // After populating the workbook, keep only the namespaces that are actually referenced.
        try {
            pruneUnusedNamespaces(helper.workbook);
        } catch (Throwable t) {
            System.err.println("[DSGGen] WARN: failed to prune unused namespaces: " + t.getMessage());
            t.printStackTrace();
        }

        String saveResult;
        try {
            saveResult = DSGGen.save(helper, filename);
            System.out.println("[DSGGen] Save result=" + saveResult);
        } catch (Throwable t) {
            System.err.println("[DSGGen] ERROR saving workbook: " + t.getMessage());
            t.printStackTrace();
            return "FAILURE: saving workbook - " + t.getMessage();
        }
        
        // Generate DA-SOC files if requested by frontend
        if (generateDASOCs && studies != null && !studies.isEmpty()) {
            System.out.println("\n[DSGGen] Frontend requested DA-SOC generation - processing...");
            try {
                String dasocResult = generateDASOCsForStudies(studies, filename);
                System.out.println("[DSGGen] DA-SOC generation result: " + dasocResult);
                saveResult += " | DA-SOC: " + dasocResult;
            } catch (Throwable t) {
                System.err.println("[DSGGen] WARNING: DA-SOC generation failed: " + t.getMessage());
                t.printStackTrace();
                saveResult += " | DA-SOC: FAILED - " + t.getMessage();
            }
        } else if (!generateDASOCs) {
            System.out.println("[DSGGen] DA-SOC generation not requested by frontend - skipping");
        }
        
        System.out.println("[DSGGen] genByManager END");
        return saveResult;
    }

    public static Workbook create(String filename, List<Study> studies) {
        // Create a new workbook
        Workbook workbook = new XSSFWorkbook();

        // Create 'InfoSheet'
        Sheet infoSheet = workbook.createSheet(DSGGen.INFOSHEET);

        // Header for InfoSheet
        Row isHeaderRow = infoSheet.createRow(0);
        isHeaderRow.createCell(0).setCellValue("Attribute");
        isHeaderRow.createCell(1).setCellValue("Value");

        // Dependency rows
        Row dataRow1 = infoSheet.createRow(1);
        dataRow1.createCell(0).setCellValue("hasDependencies");
        dataRow1.createCell(1).setCellValue("#" + DSGGen.NAMESPACES);

        Row dataRow2 = infoSheet.createRow(2);
        dataRow2.createCell(0).setCellValue("hasStudyURI");
        String studyUri = "";
        if (studies != null && !studies.isEmpty() && studies.get(0) != null) {
            Study first = studies.get(0);
            studyUri = first.getUri() != null && !first.getUri().isEmpty() ? first.getUri() : safe(first.getTitle());
        }
        // Abreviar URI com prefixo, se aplicável
        dataRow2.createCell(1).setCellValue(URIUtils.replaceNameSpaceEx(studyUri));

        Row dataRow3 = infoSheet.createRow(3);
        dataRow3.createCell(0).setCellValue("hasStudyKG");
        // Use namespace prefix (label) instead of URI
        Map<String, NameSpace> nsMap = DSGGenHelper.getNamespaces();
        String firstNamespacePrefix = "";
        if (nsMap != null && !nsMap.isEmpty()) {
            NameSpace ns = nsMap.values().iterator().next();
            if (ns != null) {
                String label = ns.getLabel();
                firstNamespacePrefix = (label != null && !label.isEmpty()) ? label : safe(ns.toString());
            }
        } else {
            List<NameSpace> inMem = NameSpace.findInMemory();
            if (inMem != null && !inMem.isEmpty()) {
                NameSpace ns = inMem.get(0);
                if (ns != null) {
                    String label = ns.getLabel();
                    firstNamespacePrefix = (label != null && !label.isEmpty()) ? label : safe(ns.toString());
                }
            }
        }
        dataRow3.createCell(1).setCellValue(firstNamespacePrefix);

        Row dataRow4 = infoSheet.createRow(4);
        dataRow4.createCell(0).setCellValue("hasStudyDescription");
        dataRow4.createCell(1).setCellValue("#" + DSGGen.STD);

        Row dataRow5 = infoSheet.createRow(5);
        dataRow5.createCell(0).setCellValue("hasEntityDesign");
        dataRow5.createCell(1).setCellValue("#" + DSGGen.SSD);

        Row dataRow6 = infoSheet.createRow(6);
        dataRow6.createCell(0).setCellValue("hasVariableDesign");
        dataRow6.createCell(1).setCellValue("#" + DSGGen.VD);

        Row dataRow7 = infoSheet.createRow(7);
        dataRow7.createCell(0).setCellValue("hasVersion");
        dataRow7.createCell(1).setCellValue("1"); // Placeholder version

        // Create sheet named 'Namespaces'
        Sheet nsSheet = workbook.createSheet(DSGGen.NAMESPACES);
        String[] nsHeaders = { "hasPrefix", "hasNameSpace", "hasFormat", "hasSource" };

        // Header row
        Row nsHeaderRow = nsSheet.createRow(0);
        for (int i = 0; i < nsHeaders.length; i++) {
            nsHeaderRow.createCell(i).setCellValue(nsHeaders[i]);
        }
        for (int i = 0; i < nsHeaders.length; i++) {
            nsSheet.autoSizeColumn(i);
        }

        // Populate namespace rows: prefer helper map, else in-memory ordered namespaces
        int nsRowNum = 1;
        if (nsMap != null && !nsMap.isEmpty()) {
            for (NameSpace ns : nsMap.values()) {
                Row row = nsSheet.createRow(nsRowNum++);
                row.createCell(0).setCellValue(safe(ns.getLabel()));       // hasPrefix
                row.createCell(1).setCellValue(safe(ns.getUri()));         // hasNameSpace
                row.createCell(2).setCellValue(safe(ns.getSourceMime()));  // hasFormat
                row.createCell(3).setCellValue(safe(ns.getSource()));      // hasSource
            }
        } else {
            List<NameSpace> inMem = NameSpace.findInMemory();
            if (inMem != null) {
                for (NameSpace ns : inMem) {
                    Row row = nsSheet.createRow(nsRowNum++);
                    row.createCell(0).setCellValue(safe(ns.getLabel()));
                    row.createCell(1).setCellValue(safe(ns.getUri()));
                    row.createCell(2).setCellValue(safe(ns.getSourceMime()));
                    row.createCell(3).setCellValue(safe(ns.getSource()));
                }
            }
        }

        // Create data sheets
        Sheet ssdSheet = workbook.createSheet(DSGGen.SSD);
        Sheet stdSheet = workbook.createSheet(DSGGen.STD);
        Sheet vdSheet = workbook.createSheet(DSGGen.VD);

        // Initialize STD headers
        Row stdHeaderRow = stdSheet.createRow(0);
        stdHeaderRow.createCell(0).setCellValue("Study ID");
        stdHeaderRow.createCell(1).setCellValue("Title");
        stdHeaderRow.createCell(2).setCellValue("Specific Aims");
        stdHeaderRow.createCell(3).setCellValue("Significance");
        stdHeaderRow.createCell(4).setCellValue("Institution");
        stdHeaderRow.createCell(5).setCellValue("Principal Investigator");
        stdHeaderRow.createCell(6).setCellValue("PI Address");
        stdHeaderRow.createCell(7).setCellValue("PI City");
        stdHeaderRow.createCell(8).setCellValue("PI State");
        stdHeaderRow.createCell(9).setCellValue("PI Zip Code");
        stdHeaderRow.createCell(10).setCellValue("Email");
        stdHeaderRow.createCell(11).setCellValue("PI Phone");
        stdHeaderRow.createCell(12).setCellValue("Co-PI 1 First Name");
        stdHeaderRow.createCell(13).setCellValue("Co-PI 1 Last Name");
        stdHeaderRow.createCell(14).setCellValue("Co-PI 1 Email");
        stdHeaderRow.createCell(15).setCellValue("Co-PI 2 First Name");
        stdHeaderRow.createCell(16).setCellValue("Co-PI 2 Last Name");
        stdHeaderRow.createCell(17).setCellValue("Co-PI 2 Email");
        stdHeaderRow.createCell(18).setCellValue("Contact First Name");
        stdHeaderRow.createCell(19).setCellValue("Contact Last Name");
        stdHeaderRow.createCell(20).setCellValue("Contact Email");
        stdHeaderRow.createCell(21).setCellValue("Project Created Date");
        stdHeaderRow.createCell(22).setCellValue("Project Last Updated Date");
        stdHeaderRow.createCell(23).setCellValue("DC Access?");

        // Initialize SSD headers
        Row ssdHeaderRow = ssdSheet.createRow(0);
        ssdHeaderRow.createCell(0).setCellValue("sheet");
        ssdHeaderRow.createCell(1).setCellValue("hasURI");
        ssdHeaderRow.createCell(2).setCellValue("type");
        ssdHeaderRow.createCell(3).setCellValue("hasSOCReference");
        ssdHeaderRow.createCell(4).setCellValue("comment");
        ssdHeaderRow.createCell(5).setCellValue("label");
        ssdHeaderRow.createCell(6).setCellValue("definition");
        ssdHeaderRow.createCell(7).setCellValue("groundingLabel");
        ssdHeaderRow.createCell(8).setCellValue("hasScope");
        ssdHeaderRow.createCell(9).setCellValue("hasTimeScope");
        ssdHeaderRow.createCell(10).setCellValue("hasSpaceScope");
        ssdHeaderRow.createCell(11).setCellValue("source");

        // Initialize VD headers (basic variable design template)
        Row vdHeaderRow = vdSheet.createRow(0);
        vdHeaderRow.createCell(0).setCellValue("sheet");
        vdHeaderRow.createCell(1).setCellValue("hasURI");
        vdHeaderRow.createCell(2).setCellValue("variableLabel");
        vdHeaderRow.createCell(3).setCellValue("variableDefinition");
        vdHeaderRow.createCell(4).setCellValue("hasUnit");
        vdHeaderRow.createCell(5).setCellValue("hasCodebook");
        vdHeaderRow.createCell(6).setCellValue("hasAttribute");
        vdHeaderRow.createCell(7).setCellValue("hasAttributeOf");
        vdHeaderRow.createCell(8).setCellValue("hasScale");
        vdHeaderRow.createCell(9).setCellValue("isAbout");

        return workbook;
    }

    private static String safe(String v) { return v == null ? "" : v; }

    public static String save(DSGGenHelper helper, String filename) {
        System.out.println("\n========== DSGGen.save() START ==========");
        System.out.println("  Input filename: [" + filename + "]");

        String basePath;
        try {
            basePath = org.hascoapi.utils.ConfigProp.getPathIngestion();
            System.out.println("  ConfigProp.getPathIngestion(): [" + basePath + "]");
        } catch (Throwable t) {
            System.err.println("[DSGGen] ERROR reading ingestion path: " + t.getMessage());
            t.printStackTrace();
            basePath = ""; // fallback to current working directory
        }

        // Normalize the path for Windows
        if (basePath != null && !basePath.isEmpty()) {
            basePath = basePath.replace("/", java.io.File.separator);
            if (!basePath.endsWith(java.io.File.separator)) {
                basePath += java.io.File.separator;
            }
            System.out.println("  Normalized basePath: [" + basePath + "]");
        } else {
            basePath = "";
        }

        String pathString = basePath + filename;
        System.out.println("  Full pathString: [" + pathString + "]");

        // Convert to absolute path and ensure directory exists
        java.io.File outputFile = new java.io.File(pathString);
        System.out.println("  Absolute path: [" + outputFile.getAbsolutePath() + "]");

        if (outputFile.getParentFile() != null && !outputFile.getParentFile().exists()) {
            System.out.println("  Creating parent directory...");
            outputFile.getParentFile().mkdirs();
        }

        String resp = "SUCCESS";
        System.out.println("[DSGGen] Saving workbook to: " + outputFile.getAbsolutePath());
        try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
            if (helper == null || helper.workbook == null) {
                System.err.println("[DSGGen] ERROR: helper or workbook is null");
                System.out.println("========== DSGGen.save() END (FAILURE) ==========\n");
                return "FAILURE: helper or workbook is null";
            }
            helper.workbook.write(fileOut);
            System.out.println("✅ [DSGGen] DSG workbook saved successfully!");
            System.out.println("  File size: " + outputFile.length() + " bytes");
            System.out.println("========== DSGGen.save() END (SUCCESS) ==========\n");
        } catch (IOException e) {
            resp = "FAILURE: Error writing workbook - " + e.getMessage();
            System.err.println("[DSGGen] " + resp);
            e.printStackTrace();
            System.out.println("========== DSGGen.save() END (FAILURE) ==========\n");
        } finally {
            try {
                if (helper != null && helper.workbook != null) {
                    helper.workbook.close();
                }
            } catch (IOException ioe) {
                System.err.println("[DSGGen] WARN: error closing workbook: " + ioe.getMessage());
            }
        }
        return resp;
    }

    private static void pruneUnusedNamespaces(Workbook workbook) {
        if (workbook == null) {
            return;
        }

        Sheet nsSheet = workbook.getSheet(NAMESPACES);
        if (nsSheet == null) {
            return;
        }

        java.util.Set<String> usedPrefixes = collectUsedPrefixes(workbook);

        // Always keep some common base prefixes (even if not explicitly used) to reduce surprises.
        usedPrefixes.add("rdf");
        usedPrefixes.add("rdfs");
        usedPrefixes.add("owl");
        usedPrefixes.add("xsd");

        // Namespaces sheet columns: 0=hasPrefix, 1=hasNameSpace, 2=hasFormat, 3=hasSource
        // We delete rows whose prefix isn't used.
        java.util.List<Integer> rowsToRemove = new java.util.ArrayList<>();
        int last = nsSheet.getLastRowNum();
        for (int r = 1; r <= last; r++) {
            Row row = nsSheet.getRow(r);
            if (row == null) {
                continue;
            }
            Cell prefixCell = row.getCell(0);
            String prefix = getCellString(prefixCell);
            if (prefix == null || prefix.trim().isEmpty()) {
                rowsToRemove.add(r);
                continue;
            }
            String normPrefix = prefix.trim();
            if (normPrefix.endsWith(":")) {
                normPrefix = normPrefix.substring(0, normPrefix.length() - 1);
            }
            normPrefix = normPrefix.trim().toLowerCase();

            if (!usedPrefixes.contains(normPrefix)) {
                rowsToRemove.add(r);
            }
        }

        if (rowsToRemove.isEmpty()) {
            System.out.println("[DSGGen] Namespaces pruning: no unused rows detected");
            return;
        }

        // Remove from bottom to top to keep indexes stable
        java.util.Collections.sort(rowsToRemove);
        java.util.Collections.reverse(rowsToRemove);
        for (int rowIndex : rowsToRemove) {
            removeRow(nsSheet, rowIndex);
        }

        System.out.println("[DSGGen] Namespaces pruning: keptPrefixes=" + usedPrefixes + " removedRows=" + rowsToRemove.size());
    }

    private static java.util.Set<String> collectUsedPrefixes(Workbook workbook) {
        java.util.Set<String> used = new java.util.HashSet<>();
        if (workbook == null) {
            return used;
        }

        for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
            Sheet sheet = workbook.getSheetAt(s);
            if (sheet == null) continue;
            String sheetName = sheet.getSheetName();
            // Skip Namespaces sheet itself to avoid self-inclusion.
            if (NAMESPACES.equals(sheetName)) {
                continue;
            }

            for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                short lastCell = row.getLastCellNum();
                if (lastCell < 0) continue;

                for (int c = 0; c < lastCell; c++) {
                    Cell cell = row.getCell(c);
                    if (cell == null) continue;
                    String v = getCellString(cell);
                    if (v == null || v.isEmpty()) continue;

                    // Find occurrences of prefix-like patterns, e.g., "ahead:STD-...", "hasco:Study".
                    // We keep this simple: scan for token chars before ':' and add them.
                    extractPrefixesFromText(v, used);
                }
            }
        }

        // Some DSG generators write full URIs. In those cases, also infer common prefixes by URI patterns.
        // Example: http://hadatac.org/ont/arrowhead/ -> ahead
        //          http://hadatac.org/ont/hasco/ -> hasco
        //          http://purl.obolibrary.org/obo/PATO_ -> pato
        //          http://hadatac.org/ont/vstoi# -> vstoi
        // NOTE: this inference is best-effort.
        // (We do not remove any prefix that is explicitly used by prefix: patterns.)
        // ...could be added here if needed.

        return used;
    }

    private static void extractPrefixesFromText(String text, java.util.Set<String> used) {
        if (text == null || text.isEmpty() || used == null) {
            return;
        }
        // Tokenize by whitespace and common punctuation.
        // We intentionally keep ':' inside tokens so we can detect prefix:localName patterns.
        String[] tokens = text.split("[\\s\\(\\)\\[\\]\\{\\}\\\"'\\,;]+"
        );
        for (String t : tokens) {
            if (t == null) continue;
            int idx = t.indexOf(':');
            if (idx <= 0) continue;
            String prefix = t.substring(0, idx).trim();
            if (prefix.isEmpty()) continue;
            // exclude URL schemes like http:
            if (prefix.equalsIgnoreCase("http") || prefix.equalsIgnoreCase("https")) continue;
            // ensure prefix looks like a namespace prefix
            if (!prefix.matches("[A-Za-z_][A-Za-z0-9_\\-]*")) continue;
            used.add(prefix.toLowerCase());
        }
    }

    private static String getCellString(Cell cell) {
        if (cell == null) return "";
        try {
            CellType type = cell.getCellType();
            if (type == CellType.FORMULA) {
                type = cell.getCachedFormulaResultType();
            }
            switch (type) {
                case STRING:
                    return cell.getStringCellValue() == null ? "" : cell.getStringCellValue().trim();
                case NUMERIC:
                    double d = cell.getNumericCellValue();
                    long l = (long) d;
                    return (d == l) ? Long.toString(l) : Double.toString(d);
                case BOOLEAN:
                    return Boolean.toString(cell.getBooleanCellValue());
                default:
                    return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    private static void removeRow(Sheet sheet, int rowIndex) {
        if (sheet == null) return;
        int lastRowNum = sheet.getLastRowNum();
        if (rowIndex < 0 || rowIndex > lastRowNum) return;

        Row row = sheet.getRow(rowIndex);
        if (row != null) {
            sheet.removeRow(row);
        }

        // Shift rows up to fill the gap
        if (rowIndex < lastRowNum) {
            sheet.shiftRows(rowIndex + 1, lastRowNum, -1);
        }
    }

    /**
     * Attempts to convert various polluted URI representations into a canonical, safe URI.
     * Handles cases like:
     *  - "%3Cahead:STD-...%3E" (URL-encoded angle brackets)
     *  - "%3C%3Cahead:STD-...%3E%3E" (double angle brackets)
     *  - already prefixed URIs like "ahead:STD-..."
     * Returns null when the value can't be made into a usable URI.
     */
    private static String canonicalizeStudyUri(String uri) {
        if (uri == null) {
            return null;
        }
        String s = uri.trim();
        if (s.isEmpty()) {
            return null;
        }

        // Best-effort URL-decode (handles %3C, %3E, etc). Do it twice to handle double-encoding.
        for (int i = 0; i < 2; i++) {
            if (s.contains("%")) {
                try {
                    s = java.net.URLDecoder.decode(s, java.nio.charset.StandardCharsets.UTF_8.name());
                } catch (Exception e) {
                    // ignore and proceed
                }
            }
        }

        // Strip surrounding angle brackets if present (one or many)
        while (s.startsWith("<") && s.endsWith(">") && s.length() > 2) {
            s = s.substring(1, s.length() - 1).trim();
        }

        // Fix previous bug patterns like "#/" -> "#"
        s = s.replace("#/", "#");

        // If it is a prefixed URI, expand to full URI (after namespaces are loaded)
        try {
            String expanded = URIUtils.replacePrefixEx(s);
            if (expanded != null && !expanded.isEmpty()) {
                s = expanded;
            }
        } catch (Exception e) {
            // ignore
        }

        // Accept either full http(s) URIs or known-prefix-like forms (for envs without namespaces loaded)
        if (s.startsWith("http://") || s.startsWith("https://")) {
            return s;
        }
        // If still looks like prefixed, keep it (some code uses it), but ensure it isn't still bracketed/encoded
        // NOTE: '-' inside a character class doesn't need escaping if placed at the end or in its own group.
        if (s.matches("^[A-Za-z_][A-Za-z0-9_-]*:.*")) {
            return s;
        }

        return null;
    }

    /**
     * Generate DA-SOC CSV files for all SOCs associated with the given studies.
     * This method:
     * 1. Queries the triplestore for all SOCs linked to the studies
     * 2. For each SOC, retrieves all StudyObjects and their properties
     * 3. Generates a DA-SOC-{SOCNAME}.csv file with enrichment properties
     * 
     * @param studies List of studies to generate DA-SOCs for
     * @param dsgFilename The DSG filename (used to determine output directory)
     * @return Result string indicating success/failure
     */
    private static String generateDASOCsForStudies(java.util.List<Study> studies, String dsgFilename) {
        System.out.println("\n========== DA-SOC GENERATION START ==========");
        
        if (studies == null || studies.isEmpty()) {
            System.out.println("[DA-SOC GEN] No studies provided, skipping");
            return "SKIPPED: No studies";
        }
        
        int totalSOCsProcessed = 0;
        int totalFilesGenerated = 0;
        int totalErrors = 0;
        
        try {
            // Get output directory from DSG filename
            String basePath = org.hascoapi.utils.ConfigProp.getPathIngestion();
            if (basePath != null && !basePath.isEmpty()) {
                basePath = basePath.replace("/", java.io.File.separator);
                if (!basePath.endsWith(java.io.File.separator)) {
                    basePath += java.io.File.separator;
                }
            } else {
                basePath = "";
            }
            
            java.io.File outputDir = new java.io.File(basePath);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            
            System.out.println("[DA-SOC GEN] Output directory: " + outputDir.getAbsolutePath());
            
            // For each study, find all associated SOCs
            for (Study study : studies) {
                if (study == null || study.getUri() == null) {
                    continue;
                }
                
                System.out.println("\n[DA-SOC GEN] Processing study: " + study.getUri());
                
                try {
                    // Query for all SOCs associated with this study
                    String queryString = org.hascoapi.utils.NameSpaces.getInstance().printSparqlNameSpaceList() +
                        "SELECT DISTINCT ?socUri ?socLabel WHERE { " +
                        "  ?obj hasco:isMemberOf ?socUri . " +
                        "  ?socUri a hasco:StudyObjectCollection . " +
                        "  OPTIONAL { ?socUri rdfs:label ?socLabel . } " +
                        "} ORDER BY ?socUri";
                    
                    org.apache.jena.query.ResultSetRewindable results = org.hascoapi.utils.SPARQLUtils.select(
                        org.hascoapi.utils.CollectionUtil.getCollectionPath(
                            org.hascoapi.utils.CollectionUtil.Collection.SPARQL_QUERY),
                        queryString);
                    
                    // Process each SOC
                    while (results.hasNext()) {
                        org.apache.jena.query.QuerySolution soln = results.next();
                        String socUri = soln.get("socUri").toString();
                        String socLabel = soln.get("socLabel") != null ? soln.get("socLabel").toString() : "";
                        
                        System.out.println("[DA-SOC GEN] Found SOC: " + socUri + " (label: " + socLabel + ")");
                        totalSOCsProcessed++;
                        
                        try {
                            // Generate DA-SOC CSV for this SOC
                            String socName = extractSOCNameFromURI(socUri);
                            if (socName == null || socName.isEmpty()) {
                                System.out.println("[DA-SOC GEN] Could not extract SOC name from URI: " + socUri);
                                totalErrors++;
                                continue;
                            }
                            
                            String dasocFilename = "DA-SOC-" + socName + ".csv";
                            java.io.File dasocFile = new java.io.File(outputDir, dasocFilename);
                            
                            System.out.println("[DA-SOC GEN] Generating: " + dasocFilename);
                            
                            boolean generated = generateDASOCFile(socUri, socName, dasocFile);
                            if (generated) {
                                totalFilesGenerated++;
                                System.out.println("[DA-SOC GEN] ✅ Generated: " + dasocFilename);
                            } else {
                                totalErrors++;
                                System.out.println("[DA-SOC GEN] ❌ Failed to generate: " + dasocFilename);
                            }
                            
                        } catch (Exception e) {
                            System.err.println("[DA-SOC GEN] ERROR generating DA-SOC for SOC " + socUri + ": " + e.getMessage());
                            e.printStackTrace();
                            totalErrors++;
                        }
                    }
                    
                } catch (Exception e) {
                    System.err.println("[DA-SOC GEN] ERROR querying SOCs for study " + study.getUri() + ": " + e.getMessage());
                    e.printStackTrace();
                    totalErrors++;
                }
            }
            
            System.out.println("\n========== DA-SOC GENERATION SUMMARY ==========");
            System.out.println("SOCs processed: " + totalSOCsProcessed);
            System.out.println("Files generated: " + totalFilesGenerated);
            System.out.println("Errors: " + totalErrors);
            System.out.println("========== DA-SOC GENERATION END ==========\n");
            
            if (totalFilesGenerated > 0) {
                return "SUCCESS: " + totalFilesGenerated + " DA-SOC file(s) generated";
            } else if (totalSOCsProcessed == 0) {
                return "NO SOCs FOUND";
            } else {
                return "FAILURE: " + totalErrors + " error(s)";
            }
            
        } catch (Exception e) {
            System.err.println("[DA-SOC GEN] FATAL ERROR: " + e.getMessage());
            e.printStackTrace();
            return "FAILURE: " + e.getMessage();
        }
    }
    
    /**
     * Extract SOC name from SOC URI.
     * Examples:
     *   http://hadatac.org/kb/default/SOC-LOCATION -> LOCATION
     *   hadatac:SOC-ENTERPRISE -> ENTERPRISE
     */
    private static String extractSOCNameFromURI(String socUri) {
        if (socUri == null || socUri.isEmpty()) {
            return null;
        }
        
        // Try to find "SOC-" pattern
        int socIndex = socUri.indexOf("SOC-");
        if (socIndex == -1) {
            // Try uppercase
            socIndex = socUri.indexOf("soc-");
        }
        
        if (socIndex != -1) {
            String afterSOC = socUri.substring(socIndex + 4); // Skip "SOC-"
            // Remove any trailing fragments (#, >, etc)
            afterSOC = afterSOC.replaceAll("[>#/].*$", "");
            return afterSOC.trim();
        }
        
        // Fallback: use last segment of URI
        String[] parts = socUri.split("[/#]");
        if (parts.length > 0) {
            String last = parts[parts.length - 1];
            if (last.startsWith("SOC-")) {
                return last.substring(4);
            }
            return last;
        }
        
        return null;
    }
    
    /**
     * Generate a single DA-SOC CSV file for a given SOC.
     * 
     * @param socUri URI of the StudyObjectCollection
     * @param socName Name of the SOC (extracted from URI)
     * @param outputFile File to write the CSV to
     * @return true if successful, false otherwise
     */
    private static boolean generateDASOCFile(String socUri, String socName, java.io.File outputFile) {
        try {
            System.out.println("[DA-SOC GEN] Querying objects for SOC: " + socUri);
            
            // Query for all objects in this SOC with their existing properties
            String queryString = org.hascoapi.utils.NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT DISTINCT ?obj ?originalId ?prop ?value WHERE { " +
                "  ?obj hasco:isMemberOf <" + socUri + "> . " +
                "  ?obj hasco:originalID ?originalId . " +
                "  OPTIONAL { " +
                "    ?obj ?prop ?value . " +
                "    FILTER(?prop != rdf:type && ?prop != hasco:isMemberOf && " +
                "           ?prop != hasco:originalID && ?prop != rdfs:label && " +
                "           ?prop != rdfs:comment && ?prop != hasco:hasTimestamp && " +
                "           ?prop != vstoi:hasSIRManagerEmail && ?prop != hasco:hascoType) " +
                "  } " +
                "} ORDER BY ?obj ?prop";
            
            org.apache.jena.query.ResultSetRewindable results = org.hascoapi.utils.SPARQLUtils.select(
                org.hascoapi.utils.CollectionUtil.getCollectionPath(
                    org.hascoapi.utils.CollectionUtil.Collection.SPARQL_QUERY),
                queryString);
            
            // Build data structure: originalID -> properties
            java.util.Map<String, java.util.Map<String, String>> objectsData = new java.util.LinkedHashMap<>();
            java.util.Set<String> allPropertyUris = new java.util.LinkedHashSet<>();
            
            while (results.hasNext()) {
                org.apache.jena.query.QuerySolution soln = results.next();
                String originalId = soln.get("originalId").toString();
                
                objectsData.putIfAbsent(originalId, new java.util.LinkedHashMap<>());
                
                if (soln.get("prop") != null && soln.get("value") != null) {
                    String prop = soln.get("prop").toString();
                    String value = soln.get("value").toString();
                    
                    // Store property
                    objectsData.get(originalId).put(prop, value);
                    allPropertyUris.add(prop);
                }
            }
            
            System.out.println("[DA-SOC GEN] Found " + objectsData.size() + " objects");
            System.out.println("[DA-SOC GEN] Found " + allPropertyUris.size() + " unique properties");
            
            if (objectsData.isEmpty()) {
                System.out.println("[DA-SOC GEN] No objects found in SOC, skipping file generation");
                return false;
            }
            
            // If no extra properties beyond the base 5, skip generation
            if (allPropertyUris.isEmpty()) {
                System.out.println("[DA-SOC GEN] No enrichment properties found, skipping file generation");
                return false;
            }
            
            // Convert property URIs to CURIEs if possible
            java.util.List<String> propertyHeaders = new java.util.ArrayList<>();
            for (String propUri : allPropertyUris) {
                String curie = org.hascoapi.utils.URIUtils.replaceNameSpaceEx(propUri);
                propertyHeaders.add(curie);
            }
            
            // Write CSV file
            try (java.io.PrintWriter writer = new java.io.PrintWriter(outputFile, "UTF-8")) {
                // Write header row
                writer.print("originalID");
                for (String header : propertyHeaders) {
                    writer.print("," + header);
                }
                writer.println();
                
                // Write data rows
                for (java.util.Map.Entry<String, java.util.Map<String, String>> entry : objectsData.entrySet()) {
                    writer.print(escapeCSV(entry.getKey()));
                    
                    java.util.Map<String, String> props = entry.getValue();
                    int propIndex = 0;
                    for (String propUri : allPropertyUris) {
                        String value = props.get(propUri);
                        writer.print(",");
                        if (value != null && !value.isEmpty()) {
                            writer.print(escapeCSV(value));
                        }
                        propIndex++;
                    }
                    writer.println();
                }
                
                System.out.println("[DA-SOC GEN] ✅ File written: " + outputFile.getName() + 
                    " (" + objectsData.size() + " rows, " + propertyHeaders.size() + " properties)");
                return true;
                
            } catch (IOException e) {
                System.err.println("[DA-SOC GEN] ERROR writing CSV file: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("[DA-SOC GEN] ERROR generating DA-SOC file: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Escape CSV value (handle commas, quotes, newlines)
     */
    private static String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        
        // If value contains comma, quote, or newline, wrap in quotes and escape internal quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        
        return value;
    }
}
