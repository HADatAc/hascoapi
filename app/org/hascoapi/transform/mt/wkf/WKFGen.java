package org.hascoapi.transform.mt.wkf;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.hascoapi.entity.pojo.WKF;
import org.hascoapi.entity.pojo.GenericFindWithStatus;
import org.hascoapi.entity.pojo.NameSpace;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/*
WKFGen builds an Excel workbook for workflows (WKF):
genByStatus queries WKFs by status,
create initializes the workbook (InfoSheet + ProcessStems/Processes/Tasks/RequiredInstruments + Namespaces),
ProcessStems/Processes/Tasks/RequiredInstruments rows are added per WKF,
InfoSheet references are set (first WKF URI/label and first namespace URI),
Namespaces is populated from the helper map or in-memory namespaces, and save writes and closes the file.
*/

public class WKFGen {

    public static final String INFOSHEET                = "InfoSheet";
    public static final String NAMESPACES               = "Namespaces";
    public static final String PROCESSSTEMS             = "ProcessStems";
    public static final String PROCESSES                = "Processes";
    public static final String TASKS                    = "Tasks";
    public static final String REQUIREDINSTRUMENTS      = "RequiredInstruments";

    public static final int PAGESIZE                    = 20000;
    public static final int OFFSET                      = 0;

    public static String genByStatus(String status, String filename, String mediaFolder, String verifyUri) {
        return genByStatus(status, filename, mediaFolder, verifyUri, null);
    }

    public static String genByStatus(String status, String filename, String mediaFolder, String verifyUri, String excludeDataFileUri) {
        System.out.println("[WKFGen] genByStatus START status=" + status + ", filename=" + filename);
        if (excludeDataFileUri != null && !excludeDataFileUri.trim().isEmpty()) {
            System.out.println("[WKFGen] Will exclude WKF with DataFile URI: " + excludeDataFileUri);
        }
        WKFGenHelper helper = new WKFGenHelper();
        List<WKF> wkfs = null;

        final String ns = org.hascoapi.utils.NameSpaces.getInstance().printSparqlNameSpaceList();

        try {
            // 1) Buscar todos os WKFs por tipo
            String diagQuery = ns
                    + " SELECT ?uri WHERE { "
                    + "   ?wkfType rdfs:subClassOf* hasco:WKF . "
                    + "   ?uri a ?wkfType . "
                    + " }";
            System.out.println("[WKFGen] Diagnostic SPARQL (all WKFs):\n" + diagQuery);
            java.util.List<WKF> allWkfs = org.hascoapi.entity.pojo.GenericFind.findByQuery(WKF.class, diagQuery);
            System.out.println("[WKFGen] Diagnostic: total WKFs found=" + (allWkfs == null ? 0 : allWkfs.size()));

            String requestedStatus = status == null ? "" : status.trim();
            String draftStatus = org.hascoapi.vocabularies.VSTOI.DRAFT;

            // Normalize the excludeDataFileUri for comparison
            String normalizedExcludeDataFileUri = null;
            if (excludeDataFileUri != null && !excludeDataFileUri.trim().isEmpty()) {
                normalizedExcludeDataFileUri = excludeDataFileUri.trim().toLowerCase();
            }

            // Normalize/deduplicate WKFs by canonical URI
            java.util.Map<String, WKF> byCanonicalUri = new java.util.LinkedHashMap<>();

            if (allWkfs != null) {
                for (WKF w : allWkfs) {
                    if (w == null || w.getUri() == null) {
                        continue;
                    }

                    String canonicalUri = canonicalizeWkfUri(w.getUri());
                    if (canonicalUri == null || canonicalUri.isEmpty()) {
                        System.out.println("  [WKFGen] WKF diag: uri=" + w.getUri() + " (INVALID/UNUSABLE URI) -> skipped");
                        continue;
                    }

                    // Apply status filter using effective status
                    // IMPORTANT: WKF.getHasStatus() returns short form ("DRAFT")
                    // but frontend sends full URI ("http://hadatac.org/ont/vstoi#Draft")
                    // We need to normalize both for comparison
                    String rawStatus = w.getHasStatus();
                    String effectiveStatus = (rawStatus == null || rawStatus.isEmpty()) ? draftStatus : rawStatus;

                    // Normalize effective status to full URI if it's in short form
                    if (effectiveStatus != null && !effectiveStatus.contains("://")) {
                        // It's a short form like "DRAFT", convert to full URI
                        effectiveStatus = org.hascoapi.vocabularies.VSTOI.VSTOI + effectiveStatus;
                    }

                    System.out.println("  [WKFGen] WKF diag: uri=" + w.getUri()
                            + " (canonical=" + canonicalUri + ")"
                            + ", label=" + w.getLabel()
                            + ", rawStatus=" + rawStatus
                            + ", effectiveStatus=" + effectiveStatus
                            + ", requestedStatus=" + requestedStatus);

                    boolean include;
                    if (requestedStatus.isEmpty()) {
                        include = true;
                    } else {
                        // Compare normalized URIs (CASE-INSENSITIVE to handle DRAFT vs Draft)
                        include = effectiveStatus.equalsIgnoreCase(requestedStatus);
                    }
                    if (!include) {
                        System.out.println("    → Skipping " + w.getUri() + " (status mismatch)");
                        continue;
                    }

                    // CRITICAL FIX: Exclude the WKF that matches the DataFile URI being generated
                    // This prevents self-reference when generating a WKF from the just-created metadata
                    if (normalizedExcludeDataFileUri != null && w.getHasDataFileUri() != null) {
                        String wkfDataFileUri = w.getHasDataFileUri().trim().toLowerCase();
                        if (wkfDataFileUri.equals(normalizedExcludeDataFileUri)) {
                            System.out.println("    → Skipping " + w.getUri() + " (matches excludeDataFileUri: " + excludeDataFileUri + ")");
                            continue;
                        }
                    }

                    // Check if the WKF's named graph has actual content.
                    // An empty graph (or only metadata triples) means it's a freshly-created WKF for this generation request.
                    if (w.getHasDataFileUri() != null && !w.getHasDataFileUri().trim().isEmpty()) {
                        try {
                            String dataFileUri = w.getHasDataFileUri();
                            System.out.println("    [WKFGen] Checking graph content for WKF " + w.getUri());
                            System.out.println("    [WKFGen]   DataFile URI: " + dataFileUri);

                            int tripleCount = graphTripleCount(ns, dataFileUri);
                            System.out.println("    [WKFGen]   Graph triple count: " + tripleCount);

                            // A WKF that was just created will have only metadata triples (~10-15 triples)
                            // A WKF that was ingested will have many more (50+ triples from tasks, processes, etc.)
                            if (tripleCount < 20) {
                                System.out.println("    → Skipping " + w.getUri() + " (graph has only " + tripleCount + " triples, likely fresh creation)");
                                continue;
                            } else {
                                System.out.println("    → Including " + w.getUri() + " (graph has " + tripleCount + " triples)");
                            }
                        } catch (Exception ex) {
                            System.out.println("    → Error checking graph for " + w.getUri() + ": " + ex.getMessage());
                            ex.printStackTrace();
                            continue;
                        }
                    } else {
                        System.out.println("    → Skipping " + w.getUri() + " (no DataFile URI)");
                        continue;
                    }

                    // Prefer http(s) variant if any duplicates map to same canonical
                    WKF existing = byCanonicalUri.get(canonicalUri);
                    if (existing == null) {
                        byCanonicalUri.put(canonicalUri, w);
                    } else {
                        String existingUri = existing.getUri() == null ? "" : existing.getUri();
                        if (!existingUri.startsWith("http") && canonicalUri.startsWith("http")) {
                            byCanonicalUri.put(canonicalUri, w);
                        }
                    }
                }
            }

            wkfs = new java.util.ArrayList<>(byCanonicalUri.values());
            System.out.println("[WKFGen] Filtered+normalized WKFs by status; count=" + (wkfs == null ? 0 : wkfs.size()));
        } catch (Throwable t) {
            System.err.println("[WKFGen] ERROR fetching WKFs: " + t.getMessage());
            t.printStackTrace();
            return "FAILURE: fetching WKFs - " + t.getMessage();
        }

        try {
            helper.workbook = WKFGen.create(filename, wkfs);
            if (helper.workbook == null) {
                System.err.println("[WKFGen] ERROR: workbook creation returned null");
                return "FAILURE: workbook creation returned null";
            }
            System.out.println("[WKFGen] Workbook created");
        } catch (Throwable t) {
            System.err.println("[WKFGen] ERROR creating workbook: " + t.getMessage());
            t.printStackTrace();
            return "FAILURE: creating workbook - " + t.getMessage();
        }

        if (wkfs != null && !wkfs.isEmpty()) {
            System.out.println("[WKFGen] Iterating WKFs to populate sheets");
            int idx = 0;
            for (WKF wkf : wkfs) {
                idx++;
                if (wkf == null) {
                    System.out.println("[WKFGen] WARN: wkf[" + idx + "] is null, skipping");
                    continue;
                }
                System.out.println("[WKFGen] Processing wkf[" + idx + "] uri=" + wkf.getUri() + ", label=" + wkf.getLabel());
                try {
                    helper = WKFProcessStems.addByWkf(helper, wkf);
                    System.out.println("[WKFGen] ProcessStems added for wkf[" + idx + "]");
                } catch (Throwable t) {
                    System.err.println("[WKFGen] ERROR adding ProcessStems for wkf uri=" + wkf.getUri() + ": " + t.getMessage());
                    t.printStackTrace();
                }
                try {
                    helper = WKFProcesses.addByWkf(helper, wkf);
                    System.out.println("[WKFGen] Processes added for wkf[" + idx + "]");
                } catch (Throwable t) {
                    System.err.println("[WKFGen] ERROR adding Processes for wkf uri=" + wkf.getUri() + ": " + t.getMessage());
                    t.printStackTrace();
                }
                try {
                    helper = WKFTasks.addByWkf(helper, wkf);
                    System.out.println("[WKFGen] Tasks added for wkf[" + idx + "]");
                } catch (Throwable t) {
                    System.err.println("[WKFGen] ERROR adding Tasks for wkf uri=" + wkf.getUri() + ": " + t.getMessage());
                    t.printStackTrace();
                }
                try {
                    helper = WKFRequiredInstruments.addByWkf(helper, wkf);
                    System.out.println("[WKFGen] RequiredInstruments added for wkf[" + idx + "]");
                } catch (Throwable t) {
                    System.err.println("[WKFGen] ERROR adding RequiredInstruments for wkf uri=" + wkf.getUri() + ": " + t.getMessage());
                    t.printStackTrace();
                }
            }
        } else {
            System.out.println("[WKFGen] No WKFs found; sheet population skipped");
        }

        // After populating the workbook, keep only the namespaces that are actually referenced
        try {
            pruneUnusedNamespaces(helper.workbook);
        } catch (Throwable t) {
            System.err.println("[WKFGen] WARN: failed to prune unused namespaces: " + t.getMessage());
            t.printStackTrace();
        }

        String saveResult;
        try {
            saveResult = WKFGen.save(helper, filename);
            System.out.println("[WKFGen] Save result=" + saveResult);
        } catch (Throwable t) {
            System.err.println("[WKFGen] ERROR saving workbook: " + t.getMessage());
            t.printStackTrace();
            return "FAILURE: saving workbook - " + t.getMessage();
        }
        System.out.println("[WKFGen] genByStatus END");
        return saveResult;
    }

    public static String genByWkf(WKF wkf, String filename, String mediaFolder, String verifyUri) {
        if (wkf == null) {
            System.err.println("[WKFGen] ERROR: wkf is null");
            return "FAILURE: wkf is null";
        }
        System.out.println("[WKFGen] genByWkf START filename=" + filename + ", wkfUri=" + wkf.getUri());
        WKFGenHelper helper = new WKFGenHelper();
        try {
            java.util.List<WKF> wkfs = new java.util.ArrayList<>();
            wkfs.add(wkf);
            helper.workbook = WKFGen.create(filename, wkfs);
            if (helper.workbook == null) {
                System.err.println("[WKFGen] ERROR: workbook creation returned null");
                return "FAILURE: workbook creation returned null";
            }
            System.out.println("[WKFGen] Workbook created for single wkf uri=" + wkf.getUri());
        } catch (Throwable t) {
            System.err.println("[WKFGen] ERROR creating workbook: " + t.getMessage());
            t.printStackTrace();
            return "FAILURE: creating workbook - " + t.getMessage();
        }

        try {
            helper = WKFProcessStems.addByWkf(helper, wkf);
            System.out.println("[WKFGen] ProcessStems added");
        } catch (Throwable t) {
            System.err.println("[WKFGen] ERROR adding ProcessStems: " + t.getMessage());
            t.printStackTrace();
        }
        try {
            helper = WKFProcesses.addByWkf(helper, wkf);
            System.out.println("[WKFGen] Processes added");
        } catch (Throwable t) {
            System.err.println("[WKFGen] ERROR adding Processes: " + t.getMessage());
            t.printStackTrace();
        }
        try {
            helper = WKFTasks.addByWkf(helper, wkf);
            System.out.println("[WKFGen] Tasks added");
        } catch (Throwable t) {
            System.err.println("[WKFGen] ERROR adding Tasks: " + t.getMessage());
            t.printStackTrace();
        }
        try {
            helper = WKFRequiredInstruments.addByWkf(helper, wkf);
            System.out.println("[WKFGen] RequiredInstruments added");
        } catch (Throwable t) {
            System.err.println("[WKFGen] ERROR adding RequiredInstruments: " + t.getMessage());
            t.printStackTrace();
        }

        // After populating the workbook, keep only the namespaces that are actually referenced
        try {
            pruneUnusedNamespaces(helper.workbook);
        } catch (Throwable t) {
            System.err.println("[WKFGen] WARN: failed to prune unused namespaces: " + t.getMessage());
            t.printStackTrace();
        }

        String saveResult;
        try {
            saveResult = WKFGen.save(helper, filename);
            System.out.println("[WKFGen] Save result=" + saveResult);
        } catch (Throwable t) {
            System.err.println("[WKFGen] ERROR saving workbook: " + t.getMessage());
            t.printStackTrace();
            return "FAILURE: saving workbook - " + t.getMessage();
        }
        System.out.println("[WKFGen] genByWkf END");
        return saveResult;
    }

    public static String genByManager(String useremail, String status, String filename, String mediaFolder, String verifyUri) {
        System.out.println("[WKFGen] genByManager START status=" + status + ", useremail=" + useremail + ", filename=" + filename);
        WKFGenHelper helper = new WKFGenHelper();
        java.util.List<WKF> wkfs = null;
        boolean withCurrent = false; // retrieve just the elements of the requested status
        try {
            GenericFindWithStatus<WKF> wkfQuery = new GenericFindWithStatus<>();
            System.out.println("[WKFGen] Querying WKFs by manager with pageSize=" + PAGESIZE + ", offset=" + OFFSET);
            wkfs = (java.util.List<WKF>) (java.util.List<?>) wkfQuery.findByStatusManagerEmailWithPages(WKF.class, status, useremail, withCurrent, PAGESIZE, OFFSET);
            System.out.println("[WKFGen] Retrieved WKFs count=" + (wkfs == null ? 0 : wkfs.size()));
        } catch (Throwable t) {
            System.err.println("[WKFGen] ERROR fetching WKFs by manager: " + t.getMessage());
            t.printStackTrace();
            return "FAILURE: fetching WKFs by manager - " + t.getMessage();
        }

        try {
            helper.workbook = WKFGen.create(filename, wkfs);
            if (helper.workbook == null) {
                System.err.println("[WKFGen] ERROR: workbook creation returned null");
                return "FAILURE: workbook creation returned null";
            }
            System.out.println("[WKFGen] Workbook created");
        } catch (Throwable t) {
            System.err.println("[WKFGen] ERROR creating workbook: " + t.getMessage());
            t.printStackTrace();
            return "FAILURE: creating workbook - " + t.getMessage();
        }

        if (wkfs != null && !wkfs.isEmpty()) {
            System.out.println("[WKFGen] Iterating WKFs to populate sheets");
            int idx = 0;
            for (WKF wkf : wkfs) {
                idx++;
                if (wkf == null) {
                    System.out.println("[WKFGen] WARN: wkf[" + idx + "] is null, skipping");
                    continue;
                }
                System.out.println("[WKFGen] Processing wkf[" + idx + "] uri=" + wkf.getUri() + ", label=" + wkf.getLabel());
                try {
                    helper = WKFProcessStems.addByWkf(helper, wkf);
                    System.out.println("[WKFGen] ProcessStems added for wkf[" + idx + "]");
                } catch (Throwable t) {
                    System.err.println("[WKFGen] ERROR adding ProcessStems for wkf uri=" + wkf.getUri() + ": " + t.getMessage());
                    t.printStackTrace();
                }
                try {
                    helper = WKFProcesses.addByWkf(helper, wkf);
                    System.out.println("[WKFGen] Processes added for wkf[" + idx + "]");
                } catch (Throwable t) {
                    System.err.println("[WKFGen] ERROR adding Processes for wkf uri=" + wkf.getUri() + ": " + t.getMessage());
                    t.printStackTrace();
                }
                try {
                    helper = WKFTasks.addByWkf(helper, wkf);
                    System.out.println("[WKFGen] Tasks added for wkf[" + idx + "]");
                } catch (Throwable t) {
                    System.err.println("[WKFGen] ERROR adding Tasks for wkf uri=" + wkf.getUri() + ": " + t.getMessage());
                    t.printStackTrace();
                }
                try {
                    helper = WKFRequiredInstruments.addByWkf(helper, wkf);
                    System.out.println("[WKFGen] RequiredInstruments added for wkf[" + idx + "]");
                } catch (Throwable t) {
                    System.err.println("[WKFGen] ERROR adding RequiredInstruments for wkf uri=" + wkf.getUri() + ": " + t.getMessage());
                    t.printStackTrace();
                }
            }
        } else {
            System.out.println("[WKFGen] No WKFs found for manager/status; sheet population skipped");
        }

        // After populating the workbook, keep only the namespaces that are actually referenced
        try {
            pruneUnusedNamespaces(helper.workbook);
        } catch (Throwable t) {
            System.err.println("[WKFGen] WARN: failed to prune unused namespaces: " + t.getMessage());
            t.printStackTrace();
        }

        String saveResult;
        try {
            saveResult = WKFGen.save(helper, filename);
            System.out.println("[WKFGen] Save result=" + saveResult);
        } catch (Throwable t) {
            System.err.println("[WKFGen] ERROR saving workbook: " + t.getMessage());
            t.printStackTrace();
            return "FAILURE: saving workbook - " + t.getMessage();
        }
        System.out.println("[WKFGen] genByManager END");
        return saveResult;
    }

    public static Workbook create(String filename, List<WKF> wkfs) {
        // Create a new workbook
        Workbook workbook = new XSSFWorkbook();

        // Create 'InfoSheet'
        Sheet infoSheet = workbook.createSheet(WKFGen.INFOSHEET);

        // Header for InfoSheet
        Row isHeaderRow = infoSheet.createRow(0);
        isHeaderRow.createCell(0).setCellValue("Attribute");
        isHeaderRow.createCell(1).setCellValue("Value");

        // Dependency rows
        Row dataRow1 = infoSheet.createRow(1);
        dataRow1.createCell(0).setCellValue("hasDependencies");
        dataRow1.createCell(1).setCellValue("#" + WKFGen.NAMESPACES);

        Row dataRow2 = infoSheet.createRow(2);
        dataRow2.createCell(0).setCellValue("ProcessStems");
        dataRow2.createCell(1).setCellValue("#" + WKFGen.PROCESSSTEMS);

        Row dataRow3 = infoSheet.createRow(3);
        dataRow3.createCell(0).setCellValue("Processes");
        dataRow3.createCell(1).setCellValue("#" + WKFGen.PROCESSES);

        Row dataRow4 = infoSheet.createRow(4);
        dataRow4.createCell(0).setCellValue("Tasks");
        dataRow4.createCell(1).setCellValue("#" + WKFGen.TASKS);

        Row dataRow5 = infoSheet.createRow(5);
        dataRow5.createCell(0).setCellValue("RequiredInstruments");
        dataRow5.createCell(1).setCellValue("#" + WKFGen.REQUIREDINSTRUMENTS);

        Row dataRow6 = infoSheet.createRow(6);
        dataRow6.createCell(0).setCellValue("hasVersion");
        // Get version from the first WKF if available
        String versionValue = "1"; // default
        if (wkfs != null && !wkfs.isEmpty() && wkfs.get(0) != null) {
            String wkfVersion = wkfs.get(0).getHasVersion();
            if (wkfVersion != null && !wkfVersion.trim().isEmpty()) {
                versionValue = wkfVersion;
            }
        }
        dataRow6.createCell(1).setCellValue(versionValue);

        // Create sheet named 'Namespaces'
        Sheet nsSheet = workbook.createSheet(WKFGen.NAMESPACES);
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
        Map<String, NameSpace> nsMap = WKFGenHelper.getNamespaces();
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
        Sheet processItemsSheet = workbook.createSheet(WKFGen.PROCESSSTEMS);
        Sheet processesSheet = workbook.createSheet(WKFGen.PROCESSES);
        Sheet tasksSheet = workbook.createSheet(WKFGen.TASKS);
        Sheet requiredInstrumentsSheet = workbook.createSheet(WKFGen.REQUIREDINSTRUMENTS);

        // Initialize ProcessStems headers
        Row processItemsHeaderRow = processItemsSheet.createRow(0);
        processItemsHeaderRow.createCell(0).setCellValue("hasURI");
        processItemsHeaderRow.createCell(1).setCellValue("rdf:type");
        processItemsHeaderRow.createCell(2).setCellValue("hasco:hascoType");
        processItemsHeaderRow.createCell(3).setCellValue("rdfs:label");
        processItemsHeaderRow.createCell(4).setCellValue("rdfs:comment");
        processItemsHeaderRow.createCell(5).setCellValue("vstoi:hasStatus");
        processItemsHeaderRow.createCell(6).setCellValue("vstoi:hasContent");
        processItemsHeaderRow.createCell(7).setCellValue("vstoi:hasLanguage");
        processItemsHeaderRow.createCell(8).setCellValue("vstoi:hasVersion");
        processItemsHeaderRow.createCell(9).setCellValue("prov:wasDerivedFrom");
        processItemsHeaderRow.createCell(10).setCellValue("prov:wasGeneratedBy");
        processItemsHeaderRow.createCell(11).setCellValue("vstoi:hasReviewNote");
        processItemsHeaderRow.createCell(12).setCellValue("vstoi:hasSIRManagerEmail");
        processItemsHeaderRow.createCell(13).setCellValue("vstoi:hasEditorEmail");
        processItemsHeaderRow.createCell(14).setCellValue("hasco:hasImage");
        processItemsHeaderRow.createCell(15).setCellValue("hasco:hasWebDocument");

        // Initialize Processes headers
        Row processesHeaderRow = processesSheet.createRow(0);
        processesHeaderRow.createCell(0).setCellValue("hasURI");
        processesHeaderRow.createCell(1).setCellValue("rdf:type");
        processesHeaderRow.createCell(2).setCellValue("hasco:hascoType");
        processesHeaderRow.createCell(3).setCellValue("rdfs:label");
        processesHeaderRow.createCell(4).setCellValue("rdfs:comment");
        processesHeaderRow.createCell(5).setCellValue("vstoi:hasStatus");
        processesHeaderRow.createCell(6).setCellValue("vstoi:hasLanguage");
        processesHeaderRow.createCell(7).setCellValue("vstoi:hasVersion");
        processesHeaderRow.createCell(8).setCellValue("prov:wasDerivedFrom");
        processesHeaderRow.createCell(9).setCellValue("vstoi:hasReviewNote");
        processesHeaderRow.createCell(10).setCellValue("vstoi:hasSIRManagerEmail");
        processesHeaderRow.createCell(11).setCellValue("vstoi:hasEditorEmail");
        processesHeaderRow.createCell(12).setCellValue("vstoi:hasTopTask");
        processesHeaderRow.createCell(13).setCellValue("hasco:hasImage");
        processesHeaderRow.createCell(14).setCellValue("hasco:hasWebDocument");

        // Initialize Tasks headers
        Row tasksHeaderRow = tasksSheet.createRow(0);
        tasksHeaderRow.createCell(0).setCellValue("hasURI");
        tasksHeaderRow.createCell(1).setCellValue("rdf:type");
        tasksHeaderRow.createCell(2).setCellValue("hasco:hascoType");
        tasksHeaderRow.createCell(3).setCellValue("rdfs:label");
        tasksHeaderRow.createCell(4).setCellValue("rdfs:comment");
        tasksHeaderRow.createCell(5).setCellValue("vstoi:hasStatus");
        tasksHeaderRow.createCell(6).setCellValue("vstoi:hasLanguage");
        tasksHeaderRow.createCell(7).setCellValue("vstoi:hasVersion");
        tasksHeaderRow.createCell(8).setCellValue("prov:wasDerivedFrom");
        tasksHeaderRow.createCell(9).setCellValue("vstoi:hasReviewNote");
        tasksHeaderRow.createCell(10).setCellValue("vstoi:hasSIRManagerEmail");
        tasksHeaderRow.createCell(11).setCellValue("vstoi:hasEditorEmail");
        tasksHeaderRow.createCell(12).setCellValue("vstoi:hasSupertask");
        tasksHeaderRow.createCell(13).setCellValue("vstoi:hasSubtask");
        tasksHeaderRow.createCell(14).setCellValue("vstoi:hasTemporalDependency");
        tasksHeaderRow.createCell(15).setCellValue("vstoi:hasRequiredInstrument");
        tasksHeaderRow.createCell(16).setCellValue("hasco:hasImage");
        tasksHeaderRow.createCell(17).setCellValue("hasco:hasWebDocument");

        // Initialize RequiredInstruments headers
        Row requiredInstrumentsHeaderRow = requiredInstrumentsSheet.createRow(0);
        requiredInstrumentsHeaderRow.createCell(0).setCellValue("hasURI");
        requiredInstrumentsHeaderRow.createCell(1).setCellValue("rdf:type");
        requiredInstrumentsHeaderRow.createCell(2).setCellValue("hasco:hascoType");
        requiredInstrumentsHeaderRow.createCell(3).setCellValue("rdfs:label");
        requiredInstrumentsHeaderRow.createCell(4).setCellValue("rdfs:comment");
        requiredInstrumentsHeaderRow.createCell(5).setCellValue("vstoi:usesInstrument");
        requiredInstrumentsHeaderRow.createCell(6).setCellValue("vstoi:hasRequiredComponent");
        requiredInstrumentsHeaderRow.createCell(7).setCellValue("hasco:hasImage");
        requiredInstrumentsHeaderRow.createCell(8).setCellValue("hasco:hasWebDocument");

        return workbook;
    }

    private static String safe(String v) { return v == null ? "" : v; }

    public static String save(WKFGenHelper helper, String filename) {
        System.out.println("\n========== WKFGen.save() START ==========");
        System.out.println("  Input filename: [" + filename + "]");

        String basePath;
        try {
            basePath = org.hascoapi.utils.ConfigProp.getPathIngestion();
            System.out.println("  ConfigProp.getPathIngestion(): [" + basePath + "]");
        } catch (Throwable t) {
            System.err.println("[WKFGen] ERROR reading ingestion path: " + t.getMessage());
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
        System.out.println("[WKFGen] Saving workbook to: " + outputFile.getAbsolutePath());
        try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
            if (helper == null || helper.workbook == null) {
                System.err.println("[WKFGen] ERROR: helper or workbook is null");
                System.out.println("========== WKFGen.save() END (FAILURE) ==========\n");
                return "FAILURE: helper or workbook is null";
            }
            helper.workbook.write(fileOut);
            System.out.println("✅ [WKFGen] WKF workbook saved successfully!");
            System.out.println("  File size: " + outputFile.length() + " bytes");
            System.out.println("========== WKFGen.save() END (SUCCESS) ==========\n");
        } catch (IOException e) {
            resp = "FAILURE: Error writing workbook - " + e.getMessage();
            System.err.println("[WKFGen] " + resp);
            e.printStackTrace();
            System.out.println("========== WKFGen.save() END (FAILURE) ==========\n");
        } finally {
            try {
                if (helper != null && helper.workbook != null) {
                    helper.workbook.close();
                }
            } catch (IOException ioe) {
                System.err.println("[WKFGen] WARN: error closing workbook: " + ioe.getMessage());
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

        // Always keep some common base prefixes
        usedPrefixes.add("rdf");
        usedPrefixes.add("rdfs");
        usedPrefixes.add("owl");
        usedPrefixes.add("xsd");
        usedPrefixes.add("vstoi");
        usedPrefixes.add("prov");
        usedPrefixes.add("hasco");

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
            System.out.println("[WKFGen] Namespaces pruning: no unused rows detected");
            return;
        }

        // Remove from bottom to top to keep indexes stable
        java.util.Collections.sort(rowsToRemove);
        java.util.Collections.reverse(rowsToRemove);
        for (int rowIndex : rowsToRemove) {
            removeRow(nsSheet, rowIndex);
        }

        System.out.println("[WKFGen] Namespaces pruning: keptPrefixes=" + usedPrefixes + " removedRows=" + rowsToRemove.size());
    }

    private static java.util.Set<String> collectUsedPrefixes(Workbook workbook) {
        java.util.Set<String> usedPrefixes = new java.util.HashSet<>();

        // Scan all sheets except InfoSheet and Namespaces
        for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
            Sheet sheet = workbook.getSheetAt(s);
            String sheetName = sheet.getSheetName();
            if (INFOSHEET.equalsIgnoreCase(sheetName) || NAMESPACES.equalsIgnoreCase(sheetName)) {
                continue;
            }

            // Scan all cells in the sheet
            for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                for (int c = 0; c < row.getLastCellNum(); c++) {
                    Cell cell = row.getCell(c);
                    String cellValue = getCellString(cell);
                    if (cellValue != null && !cellValue.isEmpty()) {
                        // Extract prefixes from URIs like "vstoi:ProcessStem"
                        if (cellValue.contains(":")) {
                            String[] parts = cellValue.split(":");
                            if (parts.length > 0) {
                                String prefix = parts[0].trim().toLowerCase();
                                // Only add if it looks like a prefix (no http://)
                                if (!prefix.startsWith("http") && !prefix.startsWith("https")) {
                                    usedPrefixes.add(prefix);
                                }
                            }
                        }
                    }
                }
            }
        }

        return usedPrefixes;
    }

    private static String getCellString(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    private static void removeRow(Sheet sheet, int rowIndex) {
        int lastRowNum = sheet.getLastRowNum();
        if (rowIndex >= 0 && rowIndex < lastRowNum) {
            sheet.shiftRows(rowIndex + 1, lastRowNum, -1);
        }
        if (rowIndex == lastRowNum) {
            Row removingRow = sheet.getRow(rowIndex);
            if (removingRow != null) {
                sheet.removeRow(removingRow);
            }
        }
    }

    private static String canonicalizeWkfUri(String uri) {
        if (uri == null || uri.trim().isEmpty()) {
            return null;
        }
        String normalized = uri.trim();
        // Remove trailing slashes
        while (normalized.endsWith("/") || normalized.endsWith("#")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        // Convert to lowercase for comparison
        normalized = normalized.toLowerCase();
        return normalized;
    }

    /**
     * Counts the number of triples in a named graph.
     * Used to distinguish between freshly created WKFs (few triples) and ingested WKFs (many triples).
     *
     * @param ns The SPARQL namespace prefix declarations
     * @param graphUri The URI of the named graph to check
     * @return The number of triples in the graph, or -1 if an error occurs
     */
    private static int graphTripleCount(String ns, String graphUri) {
        try {
            String q = ns + " SELECT (COUNT(*) AS ?tot) WHERE { GRAPH <" + graphUri + "> { ?s ?p ?o } }";
            System.out.println("    [WKFGen] Triple count query: " + q);
            org.apache.jena.query.ResultSetRewindable rs = org.hascoapi.utils.SPARQLUtils.select(
                    org.hascoapi.utils.CollectionUtil.getCollectionPath(org.hascoapi.utils.CollectionUtil.Collection.SPARQL_QUERY), q);
            if (rs != null && rs.hasNext()) {
                org.apache.jena.query.QuerySolution soln = rs.next();
                int count = soln.getLiteral("tot").getInt();
                System.out.println("    [WKFGen] Triple count result: " + count);
                return count;
            } else {
                System.out.println("    [WKFGen] Triple count query returned no results");
                return 0;
            }
        } catch (Exception e) {
            System.err.println("    [WKFGen] ERROR in graphTripleCount: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }
}
