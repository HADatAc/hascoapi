package org.hascoapi.ingestion;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.DataFile;

import java.util.*;
import java.util.stream.Collectors;

public class AnnotateWKF extends BaseAnnotator {

    public static GeneratorChain exec(DataFile dataFile, String templateFile, String status) {
        System.out.println("\n========== AnnotateWKF.exec() START ==========");
        System.out.println("DataFile URI: " + dataFile.getUri());
        System.out.println("DataFile Filename: " + dataFile.getFilename());
        System.out.println("Template File: " + templateFile);
        System.out.println("Status: " + status);

        dataFile.getLogger().addLine("Processing WKF meta-template...");

        // Load catalog with sheet validation
        System.out.println("→ Loading catalog...");
        Map<String, String> mapCatalog = loadCatalog(dataFile, Constants.MT_WKF);

        if (mapCatalog == null) {
            System.err.println("❌ Failed to load catalog - mapCatalog is null");
            dataFile.getLogger().printExceptionById("WKF_00020");
            return null;
        }

        System.out.println("✓ Catalog loaded successfully with " + mapCatalog.size() + " sheets:");
        for (Map.Entry<String, String> entry : mapCatalog.entrySet()) {
            System.out.println("  - Sheet: [" + entry.getKey() + "] → URI: [" + entry.getValue() + "]");
        }

        // MEDIUM PRIORITY FIX: Validate InfoSheet structure
        System.out.println("→ Validating InfoSheet structure...");
        if (!validateInfoSheetStructure(dataFile, mapCatalog)) {
            System.err.println("❌ InfoSheet structure validation failed");
            dataFile.getLogger().printExceptionById("WKF_00001");
            return null;
        }
        System.out.println("✓ InfoSheet structure validated successfully");

        // Generate namespace and messages
        System.out.println("→ Generating namespaces...");
        boolean okNS = IngestionWorker.nameSpaceGen(dataFile, mapCatalog, templateFile);
        System.out.println("→ Generating messages...");
        boolean okMsg = IngestionWorker.messageGen(dataFile, mapCatalog, templateFile, status);

        if (!okNS) {
            System.err.println("❌ Namespace generation failed");
            dataFile.getLogger().printExceptionById("WKF_00005");
            return null;
        }

        if (!okMsg) {
            System.err.println("❌ Message generation failed");
            dataFile.getLogger().printExceptionById("WKF_00006");
            return null;
        }

        System.out.println("✓ Namespaces and messages generated successfully");
        dataFile.getLogger().addLine("WKF: Namespaces and messages generated successfully");

        // Build the generator chain for WKF sheets
        System.out.println("→ Building generator chain...");
        GeneratorChain chain = new GeneratorChain();
        int generatorCount = 0;

        for (String sheet : mapCatalog.keySet()) {
            System.out.println("  Processing sheet: [" + sheet + "]");

            if ("InfoSheet".equalsIgnoreCase(sheet) || "Namespaces".equalsIgnoreCase(sheet)) {
                System.out.println("    → Skipping metadata sheet");
                continue; // Skip metadata sheets
            }

            if ("ProcessStems".equalsIgnoreCase(sheet)) {
                System.out.println("    → Adding ProcessStem generator");
                addCustomGeneratorIfSheetExists(dataFile, mapCatalog, sheet, status, chain,
                        (df, st) -> new WKFGenerator("processstem", df, st));
                generatorCount++;

            } else if ("Processes".equalsIgnoreCase(sheet)) {
                System.out.println("    → Adding Process generator");
                addCustomGeneratorIfSheetExists(dataFile, mapCatalog, sheet, status, chain,
                        (df, st) -> new WKFGenerator("process", df, st));
                generatorCount++;

            } else if ("Tasks".equalsIgnoreCase(sheet)) {
                System.out.println("    → Adding Task generator");
                addCustomGeneratorIfSheetExists(dataFile, mapCatalog, sheet, status, chain,
                        (df, st) -> new WKFGenerator("task", df, st));
                generatorCount++;

            } else if ("RequiredInstruments".equalsIgnoreCase(sheet)) {
                System.out.println("    → Adding RequiredInstrument generator");
                addCustomGeneratorIfSheetExists(dataFile, mapCatalog, sheet, status, chain,
                        (df, st) -> new WKFGenerator("requiredinstrument", df, st));
                generatorCount++;

            } else {
                // Unknown sheet - log warning but continue
                System.out.println("    ⚠️ Unknown sheet, logging warning");
                dataFile.getLogger().printWarningByIdWithArgs("WKF_00008", sheet);
            }
        }

        System.out.println("✓ Generator chain built with " + generatorCount + " generators");

        // Set the named graph URI so data is stored in the DataFile's graph
        chain.setNamedGraphUri(dataFile.getUri());
        System.out.println("✓ Named graph URI set to: " + dataFile.getUri());

        // Validate that at least one generator was added
        chain.setDataFile(dataFile);
        if (!chain.isValid()) {
            System.err.println("❌ Generator chain is invalid");
            dataFile.getLogger().printExceptionById("WKF_00007");
            return null;
        }

        System.out.println("✓ WKF: Generator chain validated successfully");

        // CRITICAL PRIORITY FIX: Validate temporal dependencies form a DAG (no cycles)
        System.out.println("→ Validating temporal dependency DAG...");
        if (!validateTemporalDependencyDAG(dataFile, mapCatalog)) {
            System.err.println("❌ Temporal dependency DAG validation failed - circular dependencies detected");
            dataFile.getLogger().printExceptionById("WKF_00004");
            return null;
        }
        System.out.println("✓ Temporal dependencies form a valid DAG (no cycles)");

        // CRITICAL PRIORITY FIX: Validate task hierarchy has no cycles
        System.out.println("→ Validating task hierarchy...");
        if (!validateTaskHierarchy(dataFile, mapCatalog)) {
            System.err.println("❌ Task hierarchy validation failed - circular references detected");
            dataFile.getLogger().printExceptionById("WKF_00003");
            return null;
        }
        System.out.println("✓ Task hierarchy validated successfully (no cycles)");

        // HIGH PRIORITY FIX: Validate reference integrity
        System.out.println("→ Validating reference integrity...");
        if (!validateReferenceIntegrity(dataFile, mapCatalog)) {
            System.err.println("❌ Reference integrity validation failed");
            // Don't return null - just warn, as some references might be to external resources
            dataFile.getLogger().printWarning("WKF reference integrity issues detected - check logs");
        }
        System.out.println("✓ Reference integrity validation completed");

        // EDUCATIONAL ENHANCEMENT: Validate educational properties
        System.out.println("→ Validating educational properties...");
        validateEducationalProperties(dataFile, mapCatalog);
        System.out.println("✓ Educational properties validation completed");

        // EDUCATIONAL ENHANCEMENT: Validate objective consistency
        System.out.println("→ Validating objective consistency...");
        validateObjectiveConsistency(dataFile, mapCatalog);
        System.out.println("✓ Objective consistency validation completed");

        System.out.println("========== AnnotateWKF.exec() END (SUCCESS) ==========\n");
        return chain;
    }

    /**
     * MEDIUM PRIORITY FIX: Validate InfoSheet structure according to WKF-SPEC-V1
     * Requirements:
     * - Exactly 6 data rows (excluding header)
     * - hasVersion must be numeric (no dates or text)
     * - No prohibited fields (hasWorkflowID, label, comment, versionNumber)
     */
    private static boolean validateInfoSheetStructure(DataFile dataFile, Map<String, String> catalog) {
        RecordFile infoSheet = dataFile.getRecordFile();
        
        // Check row count (should be exactly 6 data rows)
        if (infoSheet.getRecords().size() != 6) {
            System.err.println("[WKF Validation] InfoSheet must have exactly 6 data rows, found " + infoSheet.getRecords().size());
            dataFile.getLogger().printWarning("InfoSheet has " + infoSheet.getRecords().size() + " rows, expected 6. This may indicate malformed structure.");
            // Don't fail - just warn, as MTSheet validation already checks required fields
        }
        
        // Validate hasVersion is numeric
        String version = catalog.get("hasVersion");
        if (version != null && !version.matches("^\\d+(\\.\\d+)*$")) {
            System.err.println("[WKF Validation] hasVersion must be numeric (e.g., '1.0'), found: " + version);
            dataFile.getLogger().printWarning("hasVersion should be numeric (e.g., '1.0'), found: " + version);
            // Don't fail - just warn, as non-numeric versions can still work
        }
        
        // Check for prohibited fields
        String[] prohibited = {"hasWorkflowID", "label", "comment", "versionNumber"};
        for (String field : prohibited) {
            if (catalog.containsKey(field)) {
                System.err.println("[WKF Validation] InfoSheet contains prohibited field: " + field);
                dataFile.getLogger().printWarning("InfoSheet contains prohibited field: " + field + ". This field should be removed per WKF-SPEC-V1.");
                // Don't fail - just warn, as extra fields don't break processing
            }
        }
        
        return true; // Warnings only, don't fail ingestion
    }

    /**
     * CRITICAL PRIORITY FIX: Validate temporal dependencies form a DAG (Directed Acyclic Graph)
     * Requirements:
     * - No circular dependencies in vstoi:hasTemporalDependency
     * - Uses DFS (Depth-First Search) cycle detection
     */
    private static boolean validateTemporalDependencyDAG(DataFile dataFile, Map<String, String> catalog) {
        String tasksSheet = catalog.get("Tasks");
        if (tasksSheet == null || tasksSheet.trim().isEmpty()) {
            // No Tasks sheet, no temporal dependencies to validate
            return true;
        }
        
        try {
            RecordFile tasks = new SpreadsheetRecordFile(dataFile.getFile(), tasksSheet.replace("#", ""));
            if (!tasks.isValid() || tasks.getRecords().isEmpty()) {
                return true; // No tasks, no dependencies
            }
            
            // Build dependency graph: task -> list of predecessors (tasks it depends on via "after")
            Map<String, List<String>> graph = new HashMap<>();
            Map<String, String> taskURIs = new HashMap<>();
            
            for (Record rec : tasks.getRecords()) {
                String uri = rec.getValueByColumnName("hasURI");
                String tempDep = rec.getValueByColumnName("vstoi:hasTemporalDependency");
                
                if (uri != null && !uri.trim().isEmpty()) {
                    taskURIs.put(uri, uri);
                    
                    // Parse temporal dependency - only "after" creates directed edge
                    if (tempDep != null && tempDep.trim().startsWith("after ")) {
                        String predecessor = tempDep.substring(6).trim();
                        graph.computeIfAbsent(uri, k -> new ArrayList<>()).add(predecessor);
                    }
                }
            }
            
            // Detect cycles using DFS
            Set<String> visited = new HashSet<>();
            Set<String> recStack = new HashSet<>();
            
            for (String task : graph.keySet()) {
                if (hasCycleDFS(task, graph, visited, recStack, dataFile)) {
                    return false; // Cycle detected
                }
            }
            
            return true; // No cycles - valid DAG
            
        } catch (Exception e) {
            System.err.println("[WKF Validation] Error validating temporal dependencies: " + e.getMessage());
            e.printStackTrace();
            // Don't fail on exception - log and continue
            dataFile.getLogger().printWarning("Could not validate temporal dependencies: " + e.getMessage());
            return true;
        }
    }

    /**
     * DFS-based cycle detection helper for temporal dependency DAG
     */
    private static boolean hasCycleDFS(String task, Map<String, List<String>> graph,
                                       Set<String> visited, Set<String> recStack,
                                       DataFile dataFile) {
        if (recStack.contains(task)) {
            // Cycle detected!
            System.err.println("[WKF Validation] CYCLE DETECTED in temporal dependencies involving task: " + task);
            dataFile.getLogger().printExceptionByIdWithArgs("WKF_00018", task);
            return true;
        }
        
        if (visited.contains(task)) {
            return false; // Already processed this branch
        }
        
        visited.add(task);
        recStack.add(task);
        
        for (String neighbor : graph.getOrDefault(task, Collections.emptyList())) {
            if (hasCycleDFS(neighbor, graph, visited, recStack, dataFile)) {
                return true;
            }
        }
        
        recStack.remove(task);
        return false;
    }

    /**
     * CRITICAL PRIORITY FIX: Validate task hierarchy has no circular references
     * Requirements:
     * - No cycles in vstoi:hasSupertask / vstoi:hasSubtask relationships
     * - Tasks form a proper tree structure
     */
    private static boolean validateTaskHierarchy(DataFile dataFile, Map<String, String> catalog) {
        String tasksSheet = catalog.get("Tasks");
        if (tasksSheet == null || tasksSheet.trim().isEmpty()) {
            return true; // No Tasks sheet
        }
        
        try {
            RecordFile tasks = new SpreadsheetRecordFile(dataFile.getFile(), tasksSheet.replace("#", ""));
            if (!tasks.isValid() || tasks.getRecords().isEmpty()) {
                return true;
            }
            
            // Build hierarchy graph: child -> parent (via hasSupertask)
            Map<String, String> hierarchy = new HashMap<>();
            
            for (Record rec : tasks.getRecords()) {
                String taskUri = rec.getValueByColumnName("hasURI");
                String supertaskUri = rec.getValueByColumnName("vstoi:hasSupertask");
                
                if (taskUri != null && !taskUri.trim().isEmpty()) {
                    if (supertaskUri != null && !supertaskUri.trim().isEmpty()) {
                        hierarchy.put(taskUri, supertaskUri);
                    }
                }
            }
            
            // Detect cycles: for each task, follow parent chain
            Set<String> visited = new HashSet<>();
            for (String task : hierarchy.keySet()) {
                if (!visited.contains(task)) {
                    if (hasHierarchyCycle(task, hierarchy, visited, new HashSet<>(), dataFile)) {
                        return false; // Cycle detected
                    }
                }
            }
            
            return true; // No cycles
            
        } catch (Exception e) {
            System.err.println("[WKF Validation] Error validating task hierarchy: " + e.getMessage());
            e.printStackTrace();
            dataFile.getLogger().printWarning("Could not validate task hierarchy: " + e.getMessage());
            return true;
        }
    }

    /**
     * Cycle detection helper for task hierarchy
     */
    private static boolean hasHierarchyCycle(String task, Map<String, String> hierarchy,
                                            Set<String> visited, Set<String> recStack,
                                            DataFile dataFile) {
        if (recStack.contains(task)) {
            // Cycle detected!
            System.err.println("[WKF Validation] CYCLE DETECTED in task hierarchy involving task: " + task);
            dataFile.getLogger().printExceptionByIdWithArgs("WKF_00018", task);
            return true;
        }
        
        if (visited.contains(task)) {
            return false;
        }
        
        visited.add(task);
        recStack.add(task);
        
        String parent = hierarchy.get(task);
        if (parent != null && !parent.trim().isEmpty()) {
            if (hasHierarchyCycle(parent, hierarchy, visited, recStack, dataFile)) {
                return true;
            }
        }
        
        recStack.remove(task);
        return false;
    }

    /**
     * HIGH PRIORITY FIX: Validate reference integrity across sheets
     * Checks:
     * - Process -> ProcessStem (prov:wasDerivedFrom)
     * - Process -> Task (vstoi:hasTopTask)
     * - Task -> Task (vstoi:hasSupertask, vstoi:hasSubtask)
     * - Task -> RequiredInstrument (vstoi:hasRequiredInstrument)
     * - RequiredInstrument -> Instrument (vstoi:usesInstrument) - warns only
     */
    private static boolean validateReferenceIntegrity(DataFile dataFile, Map<String, String> catalog) {
        try {
            // Collect all URIs by type
            Map<String, Set<String>> urisByType = new HashMap<>();
            urisByType.put("ProcessStem", collectURIs(dataFile, catalog, "ProcessStems"));
            urisByType.put("Process", collectURIs(dataFile, catalog, "Processes"));
            urisByType.put("Task", collectURIs(dataFile, catalog, "Tasks"));
            urisByType.put("RequiredInstrument", collectURIs(dataFile, catalog, "RequiredInstruments"));
            
            boolean allValid = true;
            
            // Validate Process references
            String processSheet = catalog.get("Processes");
            if (processSheet != null && !processSheet.trim().isEmpty()) {
                RecordFile processes = new SpreadsheetRecordFile(dataFile.getFile(), processSheet.replace("#", ""));
                if (processes.isValid()) {
                    for (Record rec : processes.getRecords()) {
                        String processUri = rec.getValueByColumnName("hasURI");
                        
                        // Check prov:wasDerivedFrom -> ProcessStem
                        String stemRef = rec.getValueByColumnName("prov:wasDerivedFrom");
                        if (stemRef != null && !stemRef.trim().isEmpty()) {
                            if (!urisByType.get("ProcessStem").contains(stemRef)) {
                                System.err.println("[WKF Validation] Process " + processUri + " references non-existent ProcessStem: " + stemRef);
                                dataFile.getLogger().printWarningByIdWithArgs("WKF_00002", processUri, stemRef);
                                allValid = false;
                            }
                        }
                        
                        // Check vstoi:hasTopTask -> Task
                        String topTask = rec.getValueByColumnName("vstoi:hasTopTask");
                        if (topTask != null && !topTask.trim().isEmpty()) {
                            if (!urisByType.get("Task").contains(topTask)) {
                                System.err.println("[WKF Validation] Process " + processUri + " references non-existent top task: " + topTask);
                                dataFile.getLogger().printExceptionByIdWithArgs("WKF_00013", processUri, topTask);
                                allValid = false;
                            }
                        }
                    }
                }
            }
            
            // Validate Task references
            String taskSheet = catalog.get("Tasks");
            if (taskSheet != null && !taskSheet.trim().isEmpty()) {
                RecordFile tasks = new SpreadsheetRecordFile(dataFile.getFile(), taskSheet.replace("#", ""));
                if (tasks.isValid()) {
                    for (Record rec : tasks.getRecords()) {
                        String taskUri = rec.getValueByColumnName("hasURI");
                        
                        // Check vstoi:hasSupertask -> Task
                        String supertask = rec.getValueByColumnName("vstoi:hasSupertask");
                        if (supertask != null && !supertask.trim().isEmpty()) {
                            if (!urisByType.get("Task").contains(supertask)) {
                                System.err.println("[WKF Validation] Task " + taskUri + " references non-existent supertask: " + supertask);
                                dataFile.getLogger().printExceptionByIdWithArgs("WKF_00014", taskUri, supertask);
                                allValid = false;
                            }
                        }
                        
                        // Check vstoi:hasSubtask -> Tasks (semicolon-separated)
                        String subtasks = rec.getValueByColumnName("vstoi:hasSubtask");
                        if (subtasks != null && !subtasks.trim().isEmpty()) {
                            String[] subtaskList = subtasks.split(";");
                            for (String subtask : subtaskList) {
                                String cleanSubtask = subtask.trim();
                                if (!cleanSubtask.isEmpty() && !urisByType.get("Task").contains(cleanSubtask)) {
                                    System.err.println("[WKF Validation] Task " + taskUri + " references non-existent subtask: " + cleanSubtask);
                                    dataFile.getLogger().printExceptionByIdWithArgs("WKF_00015", taskUri, cleanSubtask);
                                    allValid = false;
                                }
                            }
                        }
                        
                        // Check vstoi:hasRequiredInstrument -> RequiredInstruments
                        String instruments = rec.getValueByColumnName("vstoi:hasRequiredInstrument");
                        if (instruments != null && !instruments.trim().isEmpty()) {
                            String[] instrumentList = instruments.split(";");
                            for (String instrument : instrumentList) {
                                String cleanInst = instrument.trim();
                                if (!cleanInst.isEmpty() && !urisByType.get("RequiredInstrument").contains(cleanInst)) {
                                    System.err.println("[WKF Validation] Task " + taskUri + " references non-existent required instrument: " + cleanInst);
                                    dataFile.getLogger().printExceptionByIdWithArgs("WKF_00017", taskUri, cleanInst);
                                    allValid = false;
                                }
                            }
                        }
                    }
                }
            }
            
            // Note: vstoi:usesInstrument references to external INS templates are NOT validated
            // (they may be in separate files/graphs)
            
            return allValid; // Return false only if internal references are broken
            
        } catch (Exception e) {
            System.err.println("[WKF Validation] Error validating reference integrity: " + e.getMessage());
            e.printStackTrace();
            dataFile.getLogger().printWarning("Could not validate reference integrity: " + e.getMessage());
            return true; // Don't fail on exception
        }
    }

    /**
     * Helper method to collect all URIs from a sheet
     */
    private static Set<String> collectURIs(DataFile dataFile, Map<String, String> catalog, String sheetKey) {
        Set<String> uris = new HashSet<>();
        String sheetName = catalog.get(sheetKey);
        
        if (sheetName != null && !sheetName.trim().isEmpty()) {
            try {
                RecordFile sheet = new SpreadsheetRecordFile(dataFile.getFile(), sheetName.replace("#", ""));
                if (sheet.isValid()) {
                    for (Record rec : sheet.getRecords()) {
                        String uri = rec.getValueByColumnName("hasURI");
                        if (uri != null && !uri.trim().isEmpty()) {
                            uris.add(uri.trim());
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[WKF Validation] Error collecting URIs from " + sheetKey + ": " + e.getMessage());
            }
        }
        
        return uris;
    }

    /**
     * EDUCATIONAL ENHANCEMENT: Validate educational properties for training/simulation workflows
     * Requirements (Conditional):
     * - Educational workflows should have learning objectives
     * - Educational workflows should have critical actions
     * - Educational workflows should have debriefing focus
     * 
     * Checks Process.hasco:hascoType to determine if workflow is educational
     */
    private static void validateEducationalProperties(DataFile dataFile, Map<String, String> catalog) {
        String processSheet = catalog.get("Processes");
        if (processSheet == null || processSheet.trim().isEmpty()) {
            return; // No Processes sheet
        }
        
        try {
            RecordFile processes = new SpreadsheetRecordFile(dataFile.getFile(), processSheet.replace("#", ""));
            if (!processes.isValid() || processes.getRecords().isEmpty()) {
                return;
            }
            
            for (Record rec : processes.getRecords()) {
                String processUri = rec.getValueByColumnName("hasURI");
                String hascoType = rec.getValueByColumnName("hasco:hascoType");
                
                // Check if this is an educational workflow
                if (hascoType != null && isEducationalWorkflow(hascoType)) {
                    String learningObjectives = rec.getValueByColumnName("vstoi:hasLearningObjectives");
                    String criticalActions = rec.getValueByColumnName("vstoi:hasCriticalActions");
                    String debriefingFocus = rec.getValueByColumnName("vstoi:hasDebriefingFocus");
                    
                    // Warn if educational workflow is missing educational properties
                    if (learningObjectives == null || learningObjectives.trim().isEmpty()) {
                        System.out.println("[WKF Validation] WARNING: Educational process " + processUri + " is missing learning objectives (vstoi:hasLearningObjectives)");
                        dataFile.getLogger().printWarning("Educational process " + processUri + " should define learning objectives for INACSL compliance");
                    }
                    
                    if (criticalActions == null || criticalActions.trim().isEmpty()) {
                        System.out.println("[WKF Validation] WARNING: Educational process " + processUri + " is missing critical actions (vstoi:hasCriticalActions)");
                        dataFile.getLogger().printWarning("Educational process " + processUri + " should define critical actions for assessment");
                    }
                    
                    if (debriefingFocus == null || debriefingFocus.trim().isEmpty()) {
                        System.out.println("[WKF Validation] INFO: Educational process " + processUri + " is missing debriefing focus (vstoi:hasDebriefingFocus)");
                        dataFile.getLogger().printWarning("Educational process " + processUri + " should define debriefing topics for structured reflection");
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("[WKF Validation] Error validating educational properties: " + e.getMessage());
            // Don't fail on exception - this is optional validation
        }
    }

    /**
     * Helper method to determine if a workflow is educational/training based on hascoType
     */
    private static boolean isEducationalWorkflow(String hascoType) {
        if (hascoType == null) {
            return false;
        }
        
        String lowerType = hascoType.toLowerCase();
        return lowerType.contains("training") || 
               lowerType.contains("simulation") || 
               lowerType.contains("educational") ||
               lowerType.contains("learning") ||
               lowerType.contains("teaching") ||
               lowerType.contains("assessment") ||
               lowerType.contains("clinical") && lowerType.contains("scenario");
    }

    /**
     * EDUCATIONAL ENHANCEMENT: Validate objective consistency between Process and Tasks
     * Requirements:
     * - All Process objectives should be supported by at least one Task
     * - All Task objectives should match Process objectives (exact string match)
     * - Warn about orphaned objectives or tasks
     */
    private static void validateObjectiveConsistency(DataFile dataFile, Map<String, String> catalog) {
        String processSheet = catalog.get("Processes");
        String taskSheet = catalog.get("Tasks");
        
        if (processSheet == null || taskSheet == null) {
            return; // Can't validate without both sheets
        }
        
        try {
            RecordFile processes = new SpreadsheetRecordFile(dataFile.getFile(), processSheet.replace("#", ""));
            RecordFile tasks = new SpreadsheetRecordFile(dataFile.getFile(), taskSheet.replace("#", ""));
            
            if (!processes.isValid() || !tasks.isValid()) {
                return;
            }
            
            // For each process with learning objectives
            for (Record processRec : processes.getRecords()) {
                String processUri = processRec.getValueByColumnName("hasURI");
                String learningObjectivesStr = processRec.getValueByColumnName("vstoi:hasLearningObjectives");
                
                if (learningObjectivesStr == null || learningObjectivesStr.trim().isEmpty()) {
                    continue; // No objectives to validate
                }
                
                // Parse process objectives
                Set<String> processObjectives = parseObjectives(learningObjectivesStr);
                Set<String> taskObjectives = new HashSet<>();
                
                // Collect all objectives from tasks
                for (Record taskRec : tasks.getRecords()) {
                    String taskObjectiveStr = taskRec.getValueByColumnName("vstoi:supportsObjective");
                    if (taskObjectiveStr != null && !taskObjectiveStr.trim().isEmpty()) {
                        taskObjectives.addAll(parseObjectives(taskObjectiveStr));
                    }
                }
                
                // Check for process objectives not mapped to any task
                Set<String> unmappedObjectives = new HashSet<>(processObjectives);
                unmappedObjectives.removeAll(taskObjectives);
                
                if (!unmappedObjectives.isEmpty()) {
                    System.out.println("[WKF Validation] WARNING: Process " + processUri + " has objectives not mapped to any task:");
                    for (String objective : unmappedObjectives) {
                        System.out.println("  - " + objective);
                        dataFile.getLogger().printWarning("Process objective not mapped to task: " + objective);
                    }
                }
                
                // Check for task objectives not defined in process
                Set<String> undefinedObjectives = new HashSet<>(taskObjectives);
                undefinedObjectives.removeAll(processObjectives);
                
                if (!undefinedObjectives.isEmpty()) {
                    System.out.println("[WKF Validation] WARNING: Tasks reference objectives not defined in Process " + processUri + ":");
                    for (String objective : undefinedObjectives) {
                        System.out.println("  - " + objective);
                        dataFile.getLogger().printWarning("Task references undefined objective: " + objective);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("[WKF Validation] Error validating objective consistency: " + e.getMessage());
            // Don't fail on exception - this is optional validation
        }
    }

    /**
     * Helper method to parse semicolon-separated objectives into a Set
     */
    private static Set<String> parseObjectives(String objectivesStr) {
        Set<String> objectives = new HashSet<>();
        if (objectivesStr == null || objectivesStr.trim().isEmpty()) {
            return objectives;
        }
        
        String[] parts = objectivesStr.split(";");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                objectives.add(trimmed);
            }
        }
        
        return objectives;
    }
}
