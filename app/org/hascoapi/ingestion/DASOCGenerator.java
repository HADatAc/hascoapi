package org.hascoapi.ingestion;

import java.io.File;
import java.util.Map;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.HADatAcThing;

/**
 * DASOCGenerator - Generator wrapper for DASOC (Data Acquisition - Study Object Collection) processing
 * 
 * This generator delegates to AnnotateDASOC for the actual processing logic.
 * It exists to provide GeneratorChain compatibility so DASOC ingestion can flow
 * through IngestionWorker like other data types.
 * 
 * DA-SOC files are a special category of DA that extends SOC content with properties
 * that don't fit inside DSG files.
 */
public class DASOCGenerator extends BaseGenerator {

    private String daUri;
    private String socUri;
    private File csvFile;

    public DASOCGenerator(DataFile dataFile, String daUri, String socUri, File csvFile) {
        super(dataFile, null, null);
        this.daUri = daUri;
        this.socUri = socUri;
        this.csvFile = csvFile;
        this.fileName = dataFile.getFilename();
    }

    @Override
    public void initMapping() {
        // DASOC doesn't use template mapping - processing is handled by AnnotateDASOC
        // This method is required by BaseGenerator but not used for DASOC
    }

    @Override
    public String getTableName() {
        return "DASOC";
    }

    @Override
    public HADatAcThing createObject(Record record, int rowNumber, String selector) throws Exception {
        // DASOC doesn't create individual objects per row in the generator pattern
        // All processing is delegated to AnnotateDASOC
        return null;
    }

    @Override
    public Map<String, Object> createRow(Record record, int rowNumber) throws Exception {
        // DASOC uses batch processing in AnnotateDASOC, not row-by-row generation
        return null;
    }

    /**
     * Override createRows() to delegate all processing to AnnotateDASOC.
     * This is called by GeneratorChain.generate() as part of the standard generator flow.
     */
    @Override
    public void createRows() throws Exception {
        System.out.println("\n=== [INGESTION PATH] DASOCGenerator.createRows() ===");
        System.out.println("[INGESTION PATH] DA URI: " + daUri);
        System.out.println("[INGESTION PATH] SOC URI: " + socUri);
        System.out.println("[INGESTION PATH] CSV File: " + csvFile.getName());
        System.out.println("[INGESTION PATH] Calling AnnotateDASOC.processDASOC()");
        
        dataFile.getLogger().println("Processing DASOC via DASOCGenerator");

        // Delegate to AnnotateDASOC processing logic
        AnnotateDASOC.IngestionResult result = AnnotateDASOC.processDASOC(dataFile, csvFile, daUri, socUri);

        if (!result.isSuccess()) {
            String errorMsg = "DASOC processing failed: " + result.getErrorMessage();
            dataFile.getLogger().printException(errorMsg);
            throw new Exception(errorMsg);
        }

        dataFile.getLogger().println(String.format("✅ DASOC processing completed: %d rows processed", result.getRowCount()));
        System.out.println("[INGESTION PATH] DASOCGenerator.createRows(): DASOC processing succeeded");
        System.out.println("[INGESTION PATH] Rows processed: " + result.getRowCount());
    }
    
    /**
     * Override createObjects() to skip standard object creation.
     * DASOC handles all object creation internally in processDASOC().
     */
    @Override
    public void createObjects() throws Exception {
        // Skip BaseGenerator's createObjects() - DASOC handles this internally
        System.out.println("[INGESTION PATH] DASOCGenerator.createObjects(): Skipping (handled by processDASOC)");
    }

    /**
     * Legacy generate() method for backwards compatibility.
     * Not used by GeneratorChain but kept in case direct calls exist.
     */
    public boolean generate() throws Exception {
        System.out.println("\n=== [INGESTION PATH] DASOCGenerator.generate() (legacy) ===");
        System.out.println("[INGESTION PATH] DA URI: " + daUri);
        System.out.println("[INGESTION PATH] SOC URI: " + socUri);
        System.out.println("[INGESTION PATH] CSV File: " + csvFile.getName());
        System.out.println("[INGESTION PATH] Calling AnnotateDASOC.processDASOC()");
        
        dataFile.getLogger().println("Processing DASOC via DASOCGenerator (legacy generate)");

        // Delegate to AnnotateDASOC processing logic
        AnnotateDASOC.IngestionResult result = AnnotateDASOC.processDASOC(dataFile, csvFile, daUri, socUri);

        if (!result.isSuccess()) {
            String errorMsg = "DASOC processing failed: " + result.getErrorMessage();
            dataFile.getLogger().printException(errorMsg);
            throw new Exception(errorMsg);
        }

        dataFile.getLogger().println(String.format("✅ DASOC processing completed: %d rows processed", result.getRowCount()));
        System.out.println("[INGESTION PATH] DASOCGenerator.generate(): DASOC processing succeeded");
        System.out.println("[INGESTION PATH] Rows processed: " + result.getRowCount());
        return true;
    }
}
