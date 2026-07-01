// WKF Ingestion Script for sbt console
// Paste this into sbt console: sbt console

import org.hascoapi.Constants
import org.hascoapi.entity.pojo._
import org.hascoapi.ingestion._
import org.hascoapi.utils._
import org.hascoapi.vocabularies.VSTOI
import java.io.File

def ingestAndRetrieveWKF(wkfFilePath: String = "/Users/pp3223/git/wkf/wkf/WKF-ASPIRACAO_SECRECOES_PSMR.xlsx"): Unit = {
  println("========================================")
  println("WKF Ingestion and Retrieval")
  println("========================================")
  println(s"File: $wkfFilePath")
  println("")
  
  val wkfFile = new File(wkfFilePath)
  if (!wkfFile.exists()) {
    println(s"ERROR: File not found: $wkfFilePath")
    return
  }
  
  val timestamp = System.currentTimeMillis()
  val dataFileUri = s"http://example.org/datafile/wkf-aspiracao-$timestamp"
  
  println("[Step 1] Creating DataFile...")
  val dataFile = new DataFile()
  dataFile.setUri(dataFileUri)
  dataFile.setNamedGraph(dataFileUri)
  dataFile.setFilename(wkfFile.getName)
  dataFile.setHasSIRManagerEmail("test@example.com")
  dataFile.setFileStatus(DataFile.UNPROCESSED)
  
  val logger = new IngestionLogger(dataFile)
  dataFile.setLogger(logger)
  dataFile.save()
  println(s"DataFile created: $dataFileUri")
  
  println("\n[Step 2] Ingesting WKF file...")
  val templateFile = wkfFile
  
  try {
    IngestionWorker.ingest(dataFile, templateFile, VSTOI.DRAFT, Constants.MT_WKF)
    
    println(s"\nIngestion Status: ${dataFile.getFileStatus}")
    
    // Step 3: Retrieve entities
    println("\n========================================")
    println("Retrieving Ingested Entities from KG")
    println("========================================")
    
    // ProcessStems
    println("\n--- ProcessStems ---")
    val processStems = ProcessStem.findByNamedGraph(dataFileUri)
    if (processStems != null && !processStems.isEmpty) {
      processStems.forEach(ps => 
        println(s"  - ${ps.getLabel} [${ps.getUri}]")
      )
    } else {
      println("  (none found)")
    }
    
    // Processes
    println("\n--- Processes ---")
    val processes = org.hascoapi.entity.pojo.Process.findByNamedGraph(dataFileUri)
    if (processes != null && !processes.isEmpty) {
      processes.forEach(proc => 
        println(s"  - ${proc.getLabel} [top: ${proc.getHasTopTask}]")
      )
    } else {
      println("  (none found)")
    }
    
    // Tasks
    println("\n--- Tasks ---")
    val tasks = Task.findByNamedGraph(dataFileUri)
    if (tasks != null && !tasks.isEmpty) {
      tasks.forEach { task =>
        println(s"  - ${task.getLabel}")
        Option(task.getHasIterationConstraint).foreach(iter => 
          println(s"    Iteration: $iter")
        )
        Option(task.getHasTemporalDependency).foreach(temp => 
          println(s"    Temporal: $temp")
        )
      }
    } else {
      println("  (none found)")
    }
    
    // RequiredInstruments
    println("\n--- RequiredInstruments ---")
    val instruments = RequiredInstrument.findByNamedGraph(dataFileUri)
    if (instruments != null && !instruments.isEmpty) {
      instruments.forEach { ri =>
        println(s"  - ${ri.getLabel}")
        Option(ri.getIsRelatedToTask).foreach(task => 
          println(s"    Related to task: $task")
        )
        Option(ri.getHasInstrumentConfig).foreach(config => 
          println(s"    Config: $config")
        )
      }
    } else {
      println("  (none found)")
    }
    
    println("\n========================================")
    println("Ingestion Complete!")
    println("========================================")
    println(s"DataFile URI: $dataFileUri")
    println(s"Named Graph: $dataFileUri")
    
  } catch {
    case e: Exception =>
      println(s"ERROR during ingestion: ${e.getMessage}")
      e.printStackTrace()
  }
}

// Run the ingestion
println("To ingest WKF, call: ingestAndRetrieveWKF()")
println("Or provide a custom path: ingestAndRetrieveWKF(\"/path/to/file.xlsx\")")
