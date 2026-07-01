package org.hascoapi.tests;

import static org.junit.jupiter.api.Assertions.*;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.*;
import org.hascoapi.ingestion.IngestionWorker;
import org.hascoapi.utils.IngestionLogger;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.vocabularies.VSTOI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

/**
 * Manual WKF Ingestion Test
 * 
 * This test ingests the WKF-ASPIRACAO_SECRECOES_PSMR.xlsx file and
 * retrieves the entities from the knowledge graph using SPARQL queries.
 */
@DisplayName("Manual WKF Ingestion and Retrieval")
public class ManualWKFIngestionTest {

    @Test
    @DisplayName("Ingest WKF-ASPIRACAO_SECRECOES_PSMR and retrieve entities")
    public void testIngestAndRetrieveWKF() throws Exception {
        System.out.println("\n========================================");
        System.out.println("WKF Ingestion and Retrieval Test");
        System.out.println("========================================\n");
        
        // Use the WKF file
        String wkfFilePath = "/Users/pp3223/git/wkf/wkf/WKF-ASPIRACAO_SECRECOES_PSMR.xlsx";
        File wkfFile = new File(wkfFilePath);
        
        assertTrue(wkfFile.exists(), "WKF file must exist: " + wkfFilePath);
        System.out.println("File: " + wkfFile.getName());
        
        // Create DataFile for ingestion
        long timestamp = System.currentTimeMillis();
        String dataFileUri = "http://example.org/datafile/wkf-aspiracao-" + timestamp;
        
        System.out.println("\n[Step 1] Creating DataFile...");
        DataFile dataFile = new DataFile();
        dataFile.setUri(dataFileUri);
        dataFile.setNamedGraph(dataFileUri);
        dataFile.setFilename(wkfFile.getName());
        dataFile.setHasSIRManagerEmail("test@example.com");
        dataFile.setFileStatus(DataFile.UNPROCESSED);
        
        IngestionLogger logger = new IngestionLogger(dataFile);
        dataFile.setLogger(logger);
        dataFile.save();
        
        System.out.println("DataFile created: " + dataFileUri);
        
        // Ingest the WKF file
        System.out.println("\n[Step 2] Ingesting WKF file...");
        IngestionWorker.ingest(dataFile, wkfFile, VSTOI.DRAFT, Constants.MT_WKF);
        
        System.out.println("\nIngestion Status: " + dataFile.getFileStatus());
        assertNotNull(dataFile.getFileStatus(), "File status should be set after ingestion");
        assertFalse(dataFile.getFileStatus().isEmpty(), "File status should not be empty");
        
        // Retrieve entities from Knowledge Graph using SPARQL
        System.out.println("\n========================================");
        System.out.println("Retrieving Ingested Entities from KG");
        System.out.println("========================================");
        
        String namespaces = NameSpaces.getInstance().printSparqlNameSpaceList();
        
        // Get ProcessStems
        System.out.println("\n--- ProcessStems ---");
        String psQuery = namespaces +
            "SELECT ?uri WHERE { " +
            "  GRAPH <" + dataFileUri + "> { " +
            "    ?uri a vstoi:ProcessStem . " +
            "  } " +
            "}";
        List<ProcessStem> processStems = GenericFind.findByQuery(ProcessStem.class, psQuery);
        if (processStems != null && !processStems.isEmpty()) {
            System.out.println("Found " + processStems.size() + " ProcessStem(s):");
            for (ProcessStem ps : processStems) {
                System.out.println("  - " + ps.getLabel() + " [" + ps.getUri() + "]");
            }
        } else {
            System.out.println("  (none found)");
        }
        
        // Get Processes
        System.out.println("\n--- Processes ---");
        String procQuery = namespaces +
            "SELECT ?uri WHERE { " +
            "  GRAPH <" + dataFileUri + "> { " +
            "    ?uri a vstoi:Process . " +
            "  } " +
            "}";
        List<org.hascoapi.entity.pojo.Process> processes = 
            GenericFind.findByQuery(org.hascoapi.entity.pojo.Process.class, procQuery);
        if (processes != null && !processes.isEmpty()) {
            System.out.println("Found " + processes.size() + " Process(es):");
            for (org.hascoapi.entity.pojo.Process proc : processes) {
                System.out.println("  - " + proc.getLabel() + " [" + proc.getUri() + "]");
                System.out.println("    Top Task: " + proc.getHasTopTask());
            }
        } else {
            System.out.println("  (none found)");
        }
        
        // Get Tasks
        System.out.println("\n--- Tasks ---");
        String taskQuery = namespaces +
            "SELECT ?uri WHERE { " +
            "  GRAPH <" + dataFileUri + "> { " +
            "    ?uri a vstoi:Task . " +
            "  } " +
            "}";
        List<Task> tasks = GenericFind.findByQuery(Task.class, taskQuery);
        if (tasks != null && !tasks.isEmpty()) {
            System.out.println("Found " + tasks.size() + " Task(s):");
            for (Task task : tasks) {
                System.out.println("  - " + task.getLabel() + " [" + task.getUri() + "]");
                if (task.getHasIterationConstraint() != null) {
                    System.out.println("    ✓ Iteration Constraint: " + task.getHasIterationConstraint());
                }
                if (task.getHasTemporalDependency() != null) {
                    System.out.println("    ✓ Temporal Dependency: " + task.getHasTemporalDependency());
                }
                if (task.getHasSupertaskUri() != null) {
                    System.out.println("    Supertask: " + task.getHasSupertaskUri());
                }
            }
        } else {
            System.out.println("  (none found)");
        }
        
        // Get RequiredInstruments
        System.out.println("\n--- RequiredInstruments ---");
        String riQuery = namespaces +
            "SELECT ?uri WHERE { " +
            "  GRAPH <" + dataFileUri + "> { " +
            "    ?uri a vstoi:RequiredInstrument . " +
            "  } " +
            "}";
        List<RequiredInstrument> instruments = GenericFind.findByQuery(RequiredInstrument.class, riQuery);
        if (instruments != null && !instruments.isEmpty()) {
            System.out.println("Found " + instruments.size() + " RequiredInstrument(s):");
            for (RequiredInstrument ri : instruments) {
                System.out.println("  - " + ri.getLabel() + " [" + ri.getUri() + "]");
                System.out.println("    Uses Instrument: " + ri.getUsesInstrument());
                if (ri.getIsRelatedToTask() != null) {
                    System.out.println("    ✓ Related to Task: " + ri.getIsRelatedToTask());
                }
                if (ri.getHasInstrumentConfig() != null) {
                    System.out.println("    ✓ Config: " + ri.getHasInstrumentConfig());
                }
            }
        } else {
            System.out.println("  (none found)");
        }
        
        System.out.println("\n========================================");
        System.out.println("SUCCESS!");
        System.out.println("========================================");
        System.out.println("DataFile URI: " + dataFileUri);
        System.out.println("Named Graph: " + dataFileUri);
        System.out.println("\nWKF file successfully ingested!");
        System.out.println("All entities retrieved from knowledge graph!");
        
        // Verify we got some entities
        assertTrue((processStems != null && !processStems.isEmpty()) ||
                   (processes != null && !processes.isEmpty()) ||
                   (tasks != null && !tasks.isEmpty()) ||
                   (instruments != null && !instruments.isEmpty()),
                   "At least some WKF entities should have been ingested");
    }
}
