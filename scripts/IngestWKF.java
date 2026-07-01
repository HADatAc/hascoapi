import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.WKF;
import org.hascoapi.entity.pojo.ProcessStem;
import org.hascoapi.entity.pojo.Process;
import org.hascoapi.entity.pojo.Task;
import org.hascoapi.entity.pojo.RequiredInstrument;
import org.hascoapi.ingestion.IngestionWorker;
import org.hascoapi.utils.ConfigProp;
import org.hascoapi.utils.IngestionLogger;
import org.hascoapi.vocabularies.VSTOI;

import java.io.File;
import java.util.List;

/**
 * Simple WKF Ingestion Script
 * 
 * Usage: Run this from sbt console or as standalone app
 */
public class IngestWKF {
    
    public static void main(String[] args) {
        String wkfFilePath = "/Users/pp3223/git/wkf/wkf/WKF-ASPIRACAO_SECRECOES_PSMR.xlsx";
        
        if (args.length > 0) {
            wkfFilePath = args[0];
        }
        
        System.out.println("========================================");
        System.out.println("WKF Ingestion Script");
        System.out.println("========================================");
        System.out.println("File: " + wkfFilePath);
        System.out.println("");
        
        File wkfFile = new File(wkfFilePath);
        if (!wkfFile.exists()) {
            System.err.println("ERROR: File not found: " + wkfFilePath);
            System.exit(1);
        }
        
        try {
            // Step 1: Create DataFile for ingestion
            System.out.println("[Step 1] Creating DataFile...");
            long timestamp = System.currentTimeMillis();
            String dataFileUri = "http://example.org/datafile/wkf-aspiracao-" + timestamp;
            
            DataFile dataFile = new DataFile();
            dataFile.setUri(dataFileUri);
            dataFile.setNamedGraph(dataFileUri);
            dataFile.setFilename(wkfFile.getName());
            dataFile.setHasSIRManagerEmail("test@example.com");
            dataFile.setFileStatus(DataFile.UNPROCESSED);
            
            // Create logger
            IngestionLogger logger = new IngestionLogger();
            logger.resetLog();
            dataFile.setLogger(logger);
            
            // Save DataFile to KG
            dataFile.save();
            System.out.println("DataFile created: " + dataFileUri);
            
            // Step 2: Ingest the WKF file
            System.out.println("\n[Step 2] Ingesting WKF file...");
            String templateFile = ConfigProp.getPathUnproc() + wkfFile.getName();
            
            // Copy file to unprocessed folder
            java.nio.file.Files.copy(
                wkfFile.toPath(),
                new File(templateFile).toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );
            
            // Trigger ingestion
            IngestionWorker.ingest(dataFile, templateFile, VSTOI.DRAFT, Constants.MT_WKF);
            
            System.out.println("\nIngestion Status: " + dataFile.getFileStatus());
            System.out.println("\nIngestion Log:");
            System.out.println(logger.printLog());
            
            // Step 3: Retrieve and display ingested entities
            System.out.println("\n========================================");
            System.out.println("Retrieving Ingested Entities");
            System.out.println("========================================");
            
            // Get ProcessStems
            System.out.println("\n--- ProcessStems ---");
            List<ProcessStem> processStems = ProcessStem.findByNamedGraph(dataFileUri);
            if (processStems != null && !processStems.isEmpty()) {
                for (ProcessStem ps : processStems) {
                    System.out.println("  - " + ps.getLabel() + " [" + ps.getUri() + "]");
                }
            } else {
                System.out.println("  (none found)");
            }
            
            // Get Processes
            System.out.println("\n--- Processes ---");
            List<Process> processes = Process.findByNamedGraph(dataFileUri);
            if (processes != null && !processes.isEmpty()) {
                for (Process proc : processes) {
                    System.out.println("  - " + proc.getLabel() + " [top task: " + proc.getHasTopTask() + "]");
                }
            } else {
                System.out.println("  (none found)");
            }
            
            // Get Tasks
            System.out.println("\n--- Tasks ---");
            List<Task> tasks = Task.findByNamedGraph(dataFileUri);
            if (tasks != null && !tasks.isEmpty()) {
                for (Task task : tasks) {
                    System.out.println("  - " + task.getLabel());
                    if (task.getHasIterationConstraint() != null) {
                        System.out.println("    Iteration: " + task.getHasIterationConstraint());
                    }
                    if (task.getHasTemporalDependency() != null) {
                        System.out.println("    Temporal: " + task.getHasTemporalDependency());
                    }
                }
            } else {
                System.out.println("  (none found)");
            }
            
            // Get RequiredInstruments
            System.out.println("\n--- RequiredInstruments ---");
            List<RequiredInstrument> instruments = RequiredInstrument.findByNamedGraph(dataFileUri);
            if (instruments != null && !instruments.isEmpty()) {
                for (RequiredInstrument ri : instruments) {
                    System.out.println("  - " + ri.getLabel());
                    if (ri.getIsRelatedToTask() != null) {
                        System.out.println("    Related to task: " + ri.getIsRelatedToTask());
                    }
                    if (ri.getHasInstrumentConfig() != null) {
                        System.out.println("    Config: " + ri.getHasInstrumentConfig());
                    }
                }
            } else {
                System.out.println("  (none found)");
            }
            
            System.out.println("\n========================================");
            System.out.println("Ingestion Complete!");
            System.out.println("========================================");
            System.out.println("DataFile URI: " + dataFileUri);
            System.out.println("Named Graph: " + dataFileUri);
            
        } catch (Exception e) {
            System.err.println("ERROR during ingestion:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
