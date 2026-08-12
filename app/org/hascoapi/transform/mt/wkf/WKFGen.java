package org.hascoapi.transform.mt.wkf;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.hascoapi.entity.pojo.ProcessBasedStudy;
import org.hascoapi.entity.pojo.WKF;
import org.hascoapi.entity.pojo.GenericFindWithStatus;
import org.hascoapi.entity.pojo.NameSpace;
import org.hascoapi.entity.pojo.ProcessStem;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hascoapi.utils.URIUtils;

/*
WKFGen builds an Excel workbook for workflows (WKF):
genByStatus queries WKFs by status,
create initializes the workbook (InfoSheet + ProcessStems/Processes/Tasks + Namespaces),
ProcessStems/Processes/Tasks rows are added per WKF,
InfoSheet references are set (first WKF URI/label and first namespace URI),
Namespaces is populated from the helper map or in-memory namespaces, and save writes and closes the file.
*/

public class WKFGen {

    public static final String INFOSHEET                = "InfoSheet";
    public static final String NAMESPACES               = "Namespaces";
    public static final String STD                      = "STD";
    public static final String PROCESSSTEMS             = "ProcessStems";
    public static final String PROCESSES                = "Processes";
    public static final String TASKS                    = "Tasks";
    public static final String REQUIREDINSTRUMENTS      = "RequiredInstruments"; // legacy constant, no longer used by V3 generation

    public static final int PAGESIZE                    = 20000;
    public static final int OFFSET                      = 0;

    public static String genByStatus(String status, String filename, String mediaFolder, String verifyUri) {
        return genByStatus(status, filename, mediaFolder, verifyUri, null);
    }

    public static String genByStatus(String status, String filename, String mediaFolder, String verifyUri, String excludeDataFileUri) {
        System.out.println("\n========== WKFGen.genByStatus() START ==========");
        System.out.println("Input parameters:");
        System.out.println("  status: " + status);
        System.out.println("  filename: " + filename);
        System.out.println("  excludeDataFileUri: " + excludeDataFileUri);

        WKFGenHelper helper = new WKFGenHelper();

        // Create workbook with empty list (no WKF reference needed for status-based generation)
        try {
            java.util.List<WKF> emptyList = new java.util.ArrayList<>();
            helper.workbook = WKFGen.create(filename, emptyList);
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

        // Query ProcessStems by status and add them to the workbook
        try {
            GenericFindWithStatus<ProcessStem> processStemQuery = new GenericFindWithStatus<>();
            List<ProcessStem> processStems = processStemQuery.findByStatusWithPages(ProcessStem.class, status, PAGESIZE, OFFSET);
            if (processStems != null) {
                System.out.println("[WKFGen] Found " + processStems.size() + " ProcessStems with status=" + status);
                for (ProcessStem ps : processStems) {
                    helper = WKFProcessStems.addProcessStem(helper, ps);
                }
            } else {
                System.out.println("[WKFGen] No ProcessStems found with status=" + status);
            }
        } catch (Throwable t) {
            System.err.println("[WKFGen] ERROR querying ProcessStems: " + t.getMessage());
            t.printStackTrace();
        }

        // Query Processes by status and add them to the workbook
        try {
            GenericFindWithStatus<org.hascoapi.entity.pojo.Process> processQuery = new GenericFindWithStatus<>();
            List<org.hascoapi.entity.pojo.Process> processes = processQuery.findByStatusWithPages(org.hascoapi.entity.pojo.Process.class, status, PAGESIZE, OFFSET);
            if (processes != null) {
                System.out.println("[WKFGen] Found " + processes.size() + " Processes with status=" + status);
                for (org.hascoapi.entity.pojo.Process proc : processes) {
                    helper = WKFProcesses.addProcess(helper, proc);
                }
            } else {
                System.out.println("[WKFGen] No Processes found with status=" + status);
            }
        } catch (Throwable t) {
            System.err.println("[WKFGen] ERROR querying Processes: " + t.getMessage());
            t.printStackTrace();
        }

        // Query Tasks by status and add them to the workbook
        List<org.hascoapi.entity.pojo.Task> tasksFound = new java.util.ArrayList<>();
        try {
            GenericFindWithStatus<org.hascoapi.entity.pojo.Task> taskQuery = new GenericFindWithStatus<>();
            List<org.hascoapi.entity.pojo.Task> tasks = taskQuery.findByStatusWithPages(org.hascoapi.entity.pojo.Task.class, status, PAGESIZE, OFFSET);
            if (tasks != null) {
                System.out.println("[WKFGen] Found " + tasks.size() + " Tasks with status=" + status);
                for (org.hascoapi.entity.pojo.Task task : tasks) {
                    helper = WKFTasks.addTask(helper, task);
                    tasksFound.add(task);
                }
            } else {
                System.out.println("[WKFGen] No Tasks found with status=" + status);
            }
        } catch (Throwable t) {
            System.err.println("[WKFGen] ERROR querying Tasks: " + t.getMessage());
            t.printStackTrace();
        }

        // Populate STD row from ProcessBasedStudy metadata when available.
        try {
            populateStdSheetFromProcessBasedStudy(helper.workbook);
        } catch (Throwable t) {
            System.err.println("[WKFGen] WARN: failed to populate STD sheet from ProcessBasedStudy: " + t.getMessage());
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
        System.out.println("========== WKFGen.genByStatus() END ==========\n");
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
        // Populate STD row from ProcessBasedStudy metadata when available.
        try {
            populateStdSheetFromProcessBasedStudy(helper.workbook);
        } catch (Throwable t) {
            System.err.println("[WKFGen] WARN: failed to populate STD sheet from ProcessBasedStudy: " + t.getMessage());
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

    public static String genByProcessStem(ProcessStem processStem, String filename, String mediaFolder, String verifyUri) {
        return genByProcessStem(processStem, filename, mediaFolder, verifyUri, true);
    }

    public static String genByProcessStem(ProcessStem processStem, String filename, String mediaFolder, String verifyUri, boolean includeWorkflowModel) {
        if (processStem == null) {
            System.err.println("[WKFGen] ERROR: processStem is null");
            return "FAILURE: processStem is null";
        }
        System.out.println("[WKFGen] genByProcessStem START filename=" + filename + ", processStemUri=" + processStem.getUri());
        System.out.println("[WKFGen] includeWorkflowModel=" + includeWorkflowModel);

        WKFGenHelper helper = new WKFGenHelper();

        try {
            // Create an empty workbook
            java.util.List<WKF> emptyList = new java.util.ArrayList<>();
            helper.workbook = WKFGen.create(filename, emptyList);
            if (helper.workbook == null) {
                System.err.println("[WKFGen] ERROR: workbook creation returned null");
                return "FAILURE: workbook creation returned null";
            }
            System.out.println("[WKFGen] Workbook created for ProcessStem template");
        } catch (Throwable t) {
            System.err.println("[WKFGen] ERROR creating workbook: " + t.getMessage());
            t.printStackTrace();
            return "FAILURE: creating workbook - " + t.getMessage();
        }

        if (includeWorkflowModel) {
            // Add the ProcessStem to the ProcessStems sheet
            try {
                helper = WKFProcessStems.addProcessStem(helper, processStem);
                System.out.println("[WKFGen] ProcessStem added: " + processStem.getLabel());
            } catch (Throwable t) {
                System.err.println("[WKFGen] ERROR adding ProcessStem: " + t.getMessage());
                t.printStackTrace();
            }
        }

        // Get the named graph from the ProcessStem
        // ProcessStems, Processes, and Tasks are stored in the same named graph
        String namedGraph = processStem.getNamedGraph();
        if (namedGraph == null || namedGraph.isEmpty()) {
            System.out.println("[WKFGen] WARN: ProcessStem has no named graph, trying to infer from URI");
            // Try to infer from URI pattern (some systems use URI as named graph)
            namedGraph = processStem.getUri();
        }

        System.out.println("[WKFGen] Using named graph: " + namedGraph);

        if (includeWorkflowModel) {
            // Query and add all Processes from the same named graph
            try {
                String ns = org.hascoapi.utils.NameSpaces.getInstance().printSparqlNameSpaceList();
                String processQuery = ns
                        + " SELECT ?uri WHERE { "
                        + "   GRAPH <" + namedGraph + "> { "
                        + "     ?uri a vstoi:Process . "
                        + "   } "
                        + " }";

                System.out.println("[WKFGen] Process query: " + processQuery);

                List<org.hascoapi.entity.pojo.Process> processes = org.hascoapi.entity.pojo.GenericFind.findByQuery(
                    org.hascoapi.entity.pojo.Process.class, processQuery);

                if (processes != null && !processes.isEmpty()) {
                    System.out.println("[WKFGen] Found " + processes.size() + " Processes in named graph");
                    for (org.hascoapi.entity.pojo.Process proc : processes) {
                        helper = WKFProcesses.addProcess(helper, proc);
                    }
                } else {
                    System.out.println("[WKFGen] No Processes found in named graph: " + namedGraph);
                }
            } catch (Throwable t) {
                System.err.println("[WKFGen] ERROR querying/adding Processes: " + t.getMessage());
                t.printStackTrace();
            }

            // Query and add all Tasks from the same named graph
            try {
                String ns = org.hascoapi.utils.NameSpaces.getInstance().printSparqlNameSpaceList();
                String taskQuery = ns
                    + " SELECT DISTINCT ?uri WHERE { "
                        + "   GRAPH <" + namedGraph + "> { "
                        + "     { ?uri hasco:hascoType vstoi:Task . } "
                        + "     UNION { ?uri a ?taskType . ?taskType rdfs:subClassOf* vstoi:Task . } "
                        + "   } "
                        + " }";

                System.out.println("[WKFGen] Task query: " + taskQuery);

                List<org.hascoapi.entity.pojo.Task> tasks = org.hascoapi.entity.pojo.GenericFind.findByQuery(
                    org.hascoapi.entity.pojo.Task.class, taskQuery);

                if (tasks != null && !tasks.isEmpty()) {
                    System.out.println("[WKFGen] Found " + tasks.size() + " Tasks in named graph");
                    for (org.hascoapi.entity.pojo.Task task : tasks) {
                        helper = WKFTasks.addTask(helper, task);
                    }
                } else {
                    System.out.println("[WKFGen] No Tasks found in named graph: " + namedGraph);
                }
            } catch (Throwable t) {
                System.err.println("[WKFGen] ERROR querying/adding Tasks: " + t.getMessage());
                t.printStackTrace();
            }

        }

        // Populate STD row from ProcessBasedStudy metadata when available.
        try {
            populateStdSheetFromProcessBasedStudy(helper.workbook);
        } catch (Throwable t) {
            System.err.println("[WKFGen] WARN: failed to populate STD sheet from ProcessBasedStudy: " + t.getMessage());
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
        System.out.println("[WKFGen] genByProcessStem END");
        return saveResult;
    }

    public static String genByProcessBasedStudy(ProcessBasedStudy study, String filename, String mediaFolder, String verifyUri, boolean includeWorkflowModel) {
        if (study == null) {
            System.err.println("[WKFGen] ERROR: ProcessBasedStudy is null");
            return "FAILURE: ProcessBasedStudy is null";
        }

        System.out.println("[WKFGen] genByProcessBasedStudy START filename=" + filename + ", studyUri=" + study.getUri());
        System.out.println("[WKFGen] includeWorkflowModel=" + includeWorkflowModel);

        WKFGenHelper helper = new WKFGenHelper();
        try {
            java.util.List<WKF> emptyList = new java.util.ArrayList<>();
            helper.workbook = WKFGen.create(filename, emptyList);
            if (helper.workbook == null) {
                return "FAILURE: workbook creation returned null";
            }
        } catch (Throwable t) {
            t.printStackTrace();
            return "FAILURE: creating workbook - " + t.getMessage();
        }

        try {
            Sheet stdSheet = helper.workbook.getSheet(STD);
            writeStdDataRow(stdSheet, study);
            System.out.println("[WKFGen] STD row populated from ProcessBasedStudy uri=" + study.getUri());
        } catch (Throwable t) {
            System.err.println("[WKFGen] ERROR populating STD row from ProcessBasedStudy: " + t.getMessage());
            t.printStackTrace();
        }

        if (includeWorkflowModel) {
            String processUri = safe(study.getProcessUri());
            if (!processUri.isEmpty()) {
                try {
                    org.hascoapi.entity.pojo.Process process = org.hascoapi.entity.pojo.Process.find(processUri);
                    if (process != null) {
                        helper = addWorkflowModelFromProcess(helper, process);
                    } else {
                        System.out.println("[WKFGen] WARN: Process not found for URI=" + processUri);
                    }
                } catch (Throwable t) {
                    System.err.println("[WKFGen] ERROR adding workflow model from process: " + t.getMessage());
                    t.printStackTrace();
                }
            }
        }

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
            t.printStackTrace();
            return "FAILURE: saving workbook - " + t.getMessage();
        }

        System.out.println("[WKFGen] genByProcessBasedStudy END");
        return saveResult;
    }

    private static WKFGenHelper addWorkflowModelFromProcess(WKFGenHelper helper, org.hascoapi.entity.pojo.Process process) {
        if (helper == null || process == null) {
            return helper;
        }

        try {
            helper = WKFProcesses.addProcess(helper, process);
        } catch (Throwable t) {
            System.err.println("[WKFGen] ERROR adding Process row: " + t.getMessage());
        }

        try {
            String processStemUri = safe(process.getWasDerivedFrom());
            if (!processStemUri.isEmpty()) {
                ProcessStem processStem = ProcessStem.find(processStemUri);
                if (processStem != null) {
                    helper = WKFProcessStems.addProcessStem(helper, processStem);
                }
            }
        } catch (Throwable t) {
            System.err.println("[WKFGen] ERROR adding ProcessStem row: " + t.getMessage());
        }

        java.util.List<org.hascoapi.entity.pojo.Task> tasks = new java.util.ArrayList<>();
        try {
            String namedGraph = safe(process.getNamedGraph());
            if (namedGraph.isEmpty()) {
                namedGraph = safe(process.getUri());
            }

            String ns = org.hascoapi.utils.NameSpaces.getInstance().printSparqlNameSpaceList();
                String taskQuery = ns
                    + " SELECT DISTINCT ?uri WHERE { "
                    + "   GRAPH <" + namedGraph + "> { "
                    + "     { ?uri hasco:hascoType vstoi:Task . } "
                    + "     UNION { ?uri a ?taskType . ?taskType rdfs:subClassOf* vstoi:Task . } "
                    + "   } "
                    + " }";

                List<org.hascoapi.entity.pojo.Task> queriedTasks = org.hascoapi.entity.pojo.GenericFind.findByQuery(
                    org.hascoapi.entity.pojo.Task.class, taskQuery);
                java.util.Set<String> seenTaskUris = new java.util.LinkedHashSet<>();
            if (queriedTasks != null) {
                for (org.hascoapi.entity.pojo.Task task : queriedTasks) {
                    if (task == null) {
                    continue;
                    }
                    String taskUri = safe(task.getUri());
                    if (taskUri.isEmpty() || !seenTaskUris.add(taskUri)) {
                    continue;
                    }
                    helper = WKFTasks.addTask(helper, task);
                    tasks.add(task);
                }
            }
        } catch (Throwable t) {
            System.err.println("[WKFGen] ERROR adding Task rows: " + t.getMessage());
        }

        return helper;
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
            }
        } else {
            System.out.println("[WKFGen] No WKFs found for manager/status; sheet population skipped");
        }

        // Populate STD row from ProcessBasedStudy metadata when available.
        try {
            populateStdSheetFromProcessBasedStudy(helper.workbook);
        } catch (Throwable t) {
            System.err.println("[WKFGen] WARN: failed to populate STD sheet from ProcessBasedStudy: " + t.getMessage());
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
        dataRow2.createCell(0).setCellValue("hasStudyDescription");
        dataRow2.createCell(1).setCellValue("#" + WKFGen.STD);

        Row dataRow3 = infoSheet.createRow(3);
        dataRow3.createCell(0).setCellValue("ProcessStems");
        dataRow3.createCell(1).setCellValue("#" + WKFGen.PROCESSSTEMS);

        Row dataRow4 = infoSheet.createRow(4);
        dataRow4.createCell(0).setCellValue("Processes");
        dataRow4.createCell(1).setCellValue("#" + WKFGen.PROCESSES);

        Row dataRow5 = infoSheet.createRow(5);
        dataRow5.createCell(0).setCellValue("Tasks");
        dataRow5.createCell(1).setCellValue("#" + WKFGen.TASKS);

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
        String[] nsHeaders = { "prefix", "namespace", "hasFormat", "hasSource" };

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
                String prefix = safe(ns.getLabel());
                row.createCell(0).setCellValue(prefix);       // hasPrefix
                row.createCell(1).setCellValue(normalizeNamespaceUri(prefix, safe(ns.getUri())));         // hasNameSpace
                row.createCell(2).setCellValue(safe(ns.getSourceMime()));  // hasFormat
                row.createCell(3).setCellValue(safe(ns.getSource()));      // hasSource
            }
        } else {
            List<NameSpace> inMem = NameSpace.findInMemory();
            if (inMem != null) {
                for (NameSpace ns : inMem) {
                    Row row = nsSheet.createRow(nsRowNum++);
                    String prefix = safe(ns.getLabel());
                    row.createCell(0).setCellValue(prefix);
                    row.createCell(1).setCellValue(normalizeNamespaceUri(prefix, safe(ns.getUri())));
                    row.createCell(2).setCellValue(safe(ns.getSourceMime()));
                    row.createCell(3).setCellValue(safe(ns.getSource()));
                }
            }
        }

        // Create data sheets
        Sheet stdSheet = workbook.createSheet(WKFGen.STD);
        Sheet processItemsSheet = workbook.createSheet(WKFGen.PROCESSSTEMS);
        Sheet processesSheet = workbook.createSheet(WKFGen.PROCESSES);
        Sheet tasksSheet = workbook.createSheet(WKFGen.TASKS);

        Row stdTitleRow = stdSheet.createRow(0);
        stdTitleRow.createCell(0).setCellValue("Table 1");

        Row stdHeaderRow = stdSheet.createRow(1);
        stdHeaderRow.createCell(1).setCellValue("hasURI");
        stdHeaderRow.createCell(2).setCellValue("hasco:hasProcess");
        stdHeaderRow.createCell(3).setCellValue("Study ID");
        stdHeaderRow.createCell(4).setCellValue("Title");
        stdHeaderRow.createCell(5).setCellValue("Specific Aims");
        stdHeaderRow.createCell(6).setCellValue("Significance");
        stdHeaderRow.createCell(7).setCellValue("Institution");
        stdHeaderRow.createCell(8).setCellValue("Principal Investigator");
        stdHeaderRow.createCell(9).setCellValue("Email");
        stdHeaderRow.createCell(10).setCellValue("Start Date");
        stdHeaderRow.createCell(11).setCellValue("End Date");
        stdHeaderRow.createCell(12).setCellValue("vstoi:hasLearningObjectives");
        stdHeaderRow.createCell(13).setCellValue("vstoi:hasCriticalActions");
        stdHeaderRow.createCell(14).setCellValue("vstoi:hasDebriefingFocus");

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
        tasksHeaderRow.createCell(15).setCellValue("vstoi:usesComponentInstance");
        tasksHeaderRow.createCell(16).setCellValue("hasco:hasImage");
        tasksHeaderRow.createCell(17).setCellValue("hasco:hasWebDocument");
        tasksHeaderRow.createCell(18).setCellValue("vstoi:hasIterationConstraint");
        tasksHeaderRow.createCell(19).setCellValue("vstoi:supportsObjective");

        return workbook;
    }

    private static String safe(String v) { return v == null ? "" : v; }

    private static String normalizeNamespaceUri(String prefix, String uri) {
        String p = safe(prefix).trim().toLowerCase();
        String u = safe(uri).trim();
        // WKF-SPEC-V2 requires the HASCO namespace in hash form.
        if ("hasco".equals(p)) {
            if ("http://hadatac.org/ont/hasco/".equals(u) || "http://hadatac.org/ont/hasco".equals(u)) {
                return "http://hadatac.org/ont/hasco#";
            }
        }
        return u;
    }

    /**
     * Populate STD row using the first Process in the workbook that resolves
     * to a persisted ProcessBasedStudy via hasco:hasProcess.
     */
    private static void populateStdSheetFromProcessBasedStudy(Workbook workbook) {
        if (workbook == null) {
            return;
        }

        Sheet stdSheet = workbook.getSheet(STD);
        Sheet processSheet = workbook.getSheet(PROCESSES);
        if (stdSheet == null || processSheet == null) {
            return;
        }

        DataFormatter formatter = new DataFormatter();

        for (int rowIdx = 1; rowIdx <= processSheet.getLastRowNum(); rowIdx++) {
            Row processRow = processSheet.getRow(rowIdx);
            if (processRow == null) {
                continue;
            }

            String processValue = formatter.formatCellValue(processRow.getCell(0)).trim();
            if (processValue.isEmpty()) {
                continue;
            }

            String processUri = processValue;
            if (!processUri.startsWith("http://") && !processUri.startsWith("https://")) {
                processUri = URIUtils.replacePrefixEx(processUri);
            }

            ProcessBasedStudy pbs = ProcessBasedStudy.findByProcess(processUri);
            if (pbs == null) {
                continue;
            }

            writeStdDataRow(stdSheet, pbs);
            System.out.println("[WKFGen] STD row populated from ProcessBasedStudy: process=" + processUri + ", study=" + pbs.getUri());
            return;
        }

        System.out.println("[WKFGen] STD row not populated: no ProcessBasedStudy found for workbook Processes");
    }

    private static void writeStdDataRow(Sheet stdSheet, ProcessBasedStudy pbs) {
        if (stdSheet == null || pbs == null) {
            return;
        }

        Row row = stdSheet.getRow(2);
        if (row == null) {
            row = stdSheet.createRow(2);
        }

        String studyId = safe(pbs.getStudyID());
        if (studyId.isEmpty()) {
            studyId = safe(pbs.getId());
        }

        row.createCell(1).setCellValue(safe(pbs.getUri()));
        row.createCell(2).setCellValue(safe(pbs.getProcessUri()));
        row.createCell(3).setCellValue(studyId);
        row.createCell(4).setCellValue(nonEmpty(safe(pbs.getStudyTitle()), safe(pbs.getTitle()), safe(pbs.getLabel())));
        row.createCell(5).setCellValue(safe(pbs.getSpecificAims()));
        row.createCell(6).setCellValue(safe(pbs.getSignificance()));

        // WKF v1.2.2 expects URI-form ownership values in STD when provided.
        row.createCell(7).setCellValue(safe(pbs.getInstitutionUri()));
        row.createCell(8).setCellValue(safe(pbs.getPiUri()));

        row.createCell(9).setCellValue(safe(pbs.getContactEmail()));
        row.createCell(10).setCellValue(safe(pbs.getStartDate()));
        row.createCell(11).setCellValue(safe(pbs.getEndDate()));
        row.createCell(12).setCellValue(safe(pbs.getHasLearningObjectives()));
        row.createCell(13).setCellValue(safe(pbs.getHasCriticalActions()));
        row.createCell(14).setCellValue(safe(pbs.getHasDebriefingFocus()));
    }

    private static String nonEmpty(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return "";
    }

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
        String absolutePath = outputFile.getAbsolutePath();
        System.out.println("  Absolute path: [" + absolutePath + "]");

        if (outputFile.getParentFile() != null && !outputFile.getParentFile().exists()) {
            System.out.println("  Creating parent directory...");
            boolean created = outputFile.getParentFile().mkdirs();
            System.out.println("  Parent directory created: " + created);
        }

        System.out.println("[WKFGen] Saving workbook to: " + absolutePath);
        try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
            if (helper == null || helper.workbook == null) {
                System.err.println("[WKFGen] ERROR: helper or workbook is null");
                System.out.println("========== WKFGen.save() END (FAILURE) ==========\n");
                return "FAILURE: helper or workbook is null";
            }
            helper.workbook.write(fileOut);
            fileOut.flush(); // Ensure all data is written to disk
            System.out.println("✅ [WKFGen] WKF workbook saved successfully!");
            System.out.println("  File size: " + outputFile.length() + " bytes");
            System.out.println("  File exists: " + outputFile.exists());
            System.out.println("  File can read: " + outputFile.canRead());
            System.out.println("  Returning filename: [" + filename + "]");
            System.out.println("========== WKFGen.save() END (SUCCESS) ==========\n");
            return filename; // Return the filename on success (like SDDGen does)
        } catch (IOException e) {
            String errorMsg = "FAILURE: Error writing workbook - " + e.getMessage();
            System.err.println("[WKFGen] " + errorMsg);
            e.printStackTrace();
            System.out.println("========== WKFGen.save() END (FAILURE) ==========\n");
            return errorMsg;
        } finally {
            try {
                if (helper != null && helper.workbook != null) {
                    helper.workbook.close();
                }
            } catch (IOException ioe) {
                System.err.println("[WKFGen] WARN: error closing workbook: " + ioe.getMessage());
            }
        }
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
