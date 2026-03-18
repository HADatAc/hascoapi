package org.hascoapi.ingestion;

import java.io.*;
import java.lang.String;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.vocabulary.RDF;
import org.hascoapi.entity.pojo.DA;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.NameSpace;
import org.hascoapi.entity.pojo.StudyObject;
import org.hascoapi.entity.pojo.StudyObjectCollection;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.ErrorDictionary;
import org.hascoapi.utils.GSPClient;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.URIUtils;

/**
 * AnnotateDASOC - Data Acquisition Study Object Collection Annotator
 * 
 * Ingests CSV files to add properties to existing StudyObjects in a StudyObjectCollection.
 * CSV format: First column = originalID (for object lookup), remaining columns = property URIs
 * 
 * Follows patterns from AnnotateDA.java:
 * - URL decoding for URI parameters
 * - Validation with early returns
 * - Dual logging (console + DataFile logger)
 * - Counter tracking for rows processed/skipped/errors
 * - ErrorDictionary usage for standardized error messages
 * 
 * Error Codes (defined in error_dictionary.json):
 * - DASOC_00001: StudyObjectCollection not found
 * - DASOC_00002: No objects found in SOC
 * - DASOC_00003: Original ID not found in SOC at row
 * - DASOC_00004: Invalid object URI format
 * - DASOC_00005: Duplicate originalID found
 * - DASOC_00006: CSV must have at least 2 columns
 * - DASOC_00007: Error processing row
 * - DASOC_00008: URL decoding error
 */
public class AnnotateDASOC {

    private static final int BATCH_SIZE = 10000;
    private static final String TIMESTAMP_PREDICATE = "http://hadatac.org/ont/hasco/hasTimestamp";

    /**
     * IngestionWorker-compatible exec method that returns GeneratorChain.
     * Extracts DASOC parameters from DataFile properties.
     * 
     * @param dataFile DataFile containing DASOC CSV and metadata
     * @return GeneratorChain that will process DASOC when generate() is called
     */
    public static GeneratorChain exec(DataFile dataFile) {
        System.out.println("\n=== AnnotateDASOC.exec(DataFile) CALLED ===");
        System.out.println("Filename: " + dataFile.getFilename());
        System.out.println("DataFile URI: " + dataFile.getUri());
        System.out.println("DataFile.getRecordFile(): " + (dataFile.getRecordFile() != null ? "NOT NULL" : "NULL"));
        
        GeneratorChain chain = new GeneratorChain();
        chain.setNamedGraphUri(dataFile.getUri());
        
        // Extract DASOC-specific parameters from DataFile
        String daUri = dataFile.getDasocDataAcquisitionUri();
        String socUri = dataFile.getDasocSOCUri();
        
        System.out.println("Extracted DA URI from DataFile: " + (daUri != null ? daUri : "NULL"));
        System.out.println("Extracted SOC URI from DataFile: " + (socUri != null ? socUri : "NULL"));
        
        if (daUri == null || daUri.isEmpty()) {
            System.out.println("[ERROR] DA URI is NULL or empty - marking chain as invalid");
            dataFile.getLogger().printException("DASOC DataAcquisition URI not set in DataFile");
            chain.setInvalid();
            return chain;
        }
        
        if (socUri == null || socUri.isEmpty()) {
            System.out.println("[ERROR] SOC URI is NULL or empty - marking chain as invalid");
            dataFile.getLogger().printException("DASOC SOC URI not set in DataFile");
            chain.setInvalid();
            return chain;
        }
        
        File file = dataFile.getFile();
        System.out.println("DataFile.getFile() returned: " + (file != null ? file.getAbsolutePath() : "NULL"));
        
        if (file == null || !file.exists()) {
            System.out.println("[ERROR] File is NULL or does not exist - marking chain as invalid");
            dataFile.getLogger().printException("DASOC file not found: " + dataFile.getFilename());
            chain.setInvalid();
            return chain;
        }
        
        System.out.println("✅ All validations passed - creating DASOCGenerator");
        // Use DASOCGenerator to handle processing
        try {
            chain.addGenerator(new DASOCGenerator(dataFile, daUri, socUri, file));
            System.out.println("✅ DASOCGenerator added to chain - returning chain");
        } catch (Exception e) {
            System.out.println("[ERROR] Exception while creating DASOCGenerator: " + e.getMessage());
            e.printStackTrace();
            dataFile.getLogger().printException("Failed to create DASOCGenerator: " + e.getMessage());
            chain.setInvalid();
        }
        
        return chain;
    }

    /**
     * Legacy exec method for direct API calls (maintains backwards compatibility).
     * Used by IngestionAPI when called directly (not through IngestionWorker).
     * 
     * @param dataFile DataFile object
     * @param file Physical CSV file
     * @param daUri Data Acquisition URI
     * @param socUri StudyObjectCollection URI
     * @return IngestionResult with success status
     */
    public static IngestionResult exec(DataFile dataFile, File file, String daUri, String socUri) {
        System.out.println("AnnotateDASOC.exec(legacy) called for: " + dataFile.getFilename());
        return processDASOC(dataFile, file, daUri, socUri);
    }

    /**
     * Core DASOC processing logic (called by both exec methods and DASOCGenerator).
     * 
     * @param dataFile DataFile object
     * @param file Physical CSV file
     * @param daUri Data Acquisition URI
     * @param socUri StudyObjectCollection URI
     * @return IngestionResult with success status
     */
    public static IngestionResult processDASOC(DataFile dataFile, File file, String daUri, String socUri) {
        System.out.println("\n=== [INGESTION PATH] AnnotateDASOC.processDASOC() ===");
        System.out.println("✅ (1) Code reached AnnotateDASOC");
        System.out.println("[INGESTION PATH] Starting DASOC processing");
        System.out.println("[INGESTION PATH] File: " + dataFile.getFilename());
        System.out.println("[INGESTION PATH] DA URI: " + daUri);
        System.out.println("[INGESTION PATH] SOC URI: " + socUri);

        IngestionResult result = new IngestionResult();
        result.setSuccess(false);
        result.setRowCount(0);
        result.setErrorMessage("");

        // Validate inputs
        if (dataFile == null) {
            result.setErrorMessage("DataFile is null");
            System.err.println("[ERROR] DASOC ingestion: DataFile is null");
            return result;
        }

        if (daUri == null || daUri.isEmpty()) {
            result.setErrorMessage("Data Acquisition URI is required");
            dataFile.getLogger().printException("Data Acquisition URI is required");
            return result;
        }

        if (socUri == null || socUri.isEmpty()) {
            result.setErrorMessage("StudyObjectCollection URI is required");
            dataFile.getLogger().printException("StudyObjectCollection URI is required");
            return result;
        }

        try {
            // Decode URIs
            daUri = URLDecoder.decode(daUri, "UTF-8");
            socUri = URLDecoder.decode(socUri, "UTF-8");
            socUri = URIUtils.replacePrefixEx(socUri);

            dataFile.getLogger().println(String.format("DASOC ingestion for DA: <%s>, SOC: <%s>", daUri, socUri));

            // Step 1: Ensure DA exists - create if necessary
            DA da = DA.find(daUri);
            if (da == null) {
                dataFile.getLogger().println(String.format("DA <%s> not found, creating new DA record", daUri));
                
                da = new DA();
                da.setUri(daUri);
                da.setLabel(dataFile.getFilename());
                da.setComment("Data Acquisition for " + dataFile.getFilename());
                da.setHascoTypeUri("http://hadatac.org/ont/hasco/DataAcquisition");
                da.setTypeUri("http://hadatac.org/ont/hasco/DataAcquisition");
                da.setHasSIRManagerEmail(dataFile.getHasSIRManagerEmail());
                da.setHasDataFileUri(dataFile.getUri());
                da.setHasStatus("UNPROCESSED"); // Will be updated after DASOC ingestion
                
                // Save DA to triplestore
                da.save();
                dataFile.getLogger().println(String.format("✅ Created DA record: <%s>", daUri));
                System.out.println("Created new DA record: " + daUri);
            } else {
                dataFile.getLogger().println(String.format("Found existing DA: <%s>", daUri));
                System.out.println("Using existing DA record: " + daUri);
            }

            dataFile.getLogger().println(String.format("Loading StudyObjectCollection: <%s>", socUri));

            // Step 2: Load SOC and create originalID -> URI map
            Map<String, String> originalIdToUriMap = buildOriginalIdMap(socUri, dataFile);
            if (originalIdToUriMap == null || originalIdToUriMap.isEmpty()) {
                result.setErrorMessage("Failed to load StudyObjectCollection or no objects found in SOC");
                dataFile.getLogger().printExceptionByIdWithArgs("DASOC_00002", socUri);
                return result;
            }

            // Log map creation success
            dataFile.getLogger().println(String.format("✅ Successfully loaded SOC: <%s>", socUri));
            dataFile.getLogger().println(String.format("✅ Created originalID<->URI map with %d elements", originalIdToUriMap.size()));
            System.out.println(String.format("[DASOC] originalID map created: %d elements", originalIdToUriMap.size()));

            // Step 2-7: Process CSV file and create RDF model
            int rowCount = processCSVFile(file, dataFile, daUri, originalIdToUriMap);
            
            if (rowCount > 0) {
                result.setSuccess(true);
                result.setRowCount(rowCount);
                dataFile.getLogger().println(String.format("✅ Successfully ingested %d rows", rowCount));
            } else {
                result.setErrorMessage("No data rows were processed");
                dataFile.getLogger().printWarning("No data rows were processed from CSV file");
            }

        } catch (UnsupportedEncodingException e) {
            result.setErrorMessage("URL decoding error: " + e.getMessage());
            dataFile.getLogger().printExceptionByIdWithArgs("DASOC_00008", e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            result.setErrorMessage("Ingestion failed: " + e.getMessage());
            dataFile.getLogger().printException("DASOC ingestion failed: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Build a map of originalID -> object URI for all objects in the SOC
     */
    private static Map<String, String> buildOriginalIdMap(String socUri, DataFile dataFile) {
        Map<String, String> map = new HashMap<>();

        try {
            // Load the SOC
            StudyObjectCollection soc = StudyObjectCollection.find(socUri);
            if (soc == null) {
                dataFile.getLogger().printExceptionByIdWithArgs("DASOC_00001", socUri);
                System.out.println("[DASOC] ERROR: Could not find SOC: " + socUri);
                return map;
            }
            
            // Log successful SOC discovery
            dataFile.getLogger().println(String.format("✅ (1) Found related SOC: <%s>", socUri));
            System.out.println("[DASOC] (1) Found related SOC: " + socUri);

            // Query for all study objects in this SOC with their originalIDs
            String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                    "SELECT ?objUri ?originalId WHERE { " +
                    "  ?objUri hasco:isMemberOf <" + socUri + "> . " +
                    "  ?objUri hasco:originalID ?originalId . " +
                    "}";

            org.apache.jena.query.ResultSet results = SPARQLUtils.select(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY),
                    queryString);

            while (results.hasNext()) {
                org.apache.jena.query.QuerySolution soln = results.next();
                if (soln.get("objUri") != null && soln.get("originalId") != null) {
                    String objUri = soln.get("objUri").toString();
                    String originalId = soln.get("originalId").toString();
                    
                    // Skip if objUri is not a valid URI
                    if (!objUri.startsWith("http://") && !objUri.startsWith("https://")) {
                        dataFile.getLogger().printWarningByIdWithArgs("DASOC_00004", objUri);
                        continue;
                    }
                    
                    // Check for duplicate originalIDs
                    if (map.containsKey(originalId)) {
                        dataFile.getLogger().printWarningByIdWithArgs("DASOC_00005", originalId);
                        continue;
                    }
                    
                    map.put(originalId, objUri);
                }
            }
            
            // Log map creation success
            dataFile.getLogger().println(String.format("✅ (2) Created originalID<->URI map with %d elements", map.size()));
            System.out.println(String.format("[DASOC] (2) originalID map created: %d elements", map.size()));
            System.out.println(String.format("✅ (2) Code had access to original SOCs and the mapping between originalID and object URIs (%d mappings)", map.size()));

        } catch (Exception e) {
            dataFile.getLogger().printException("Error building originalID map: " + e.getMessage());
            e.printStackTrace();
        }

        return map;
    }

    /**
     * Process CSV file and create RDF triples
     */
    private static int processCSVFile(File file, DataFile dataFile, String daUri, Map<String, String> originalIdToUriMap) 
            throws Exception {
        
        int totalRows = 0;
        int currentBatchCount = 0;
        int skippedRows = 0;
        int errorRows = 0;
        int totalCSVRows = 0; // Total rows in CSV (including header)
        int objectsCreated = 0; // Successfully processed objects
        Model model = ModelFactory.createDefaultModel();
        
        // Add namespace prefixes
        addNamespacePrefixes(model);

        // Get timestamp for all triples
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(new Date());

        // Validate file exists
        if (!file.exists()) {
            throw new Exception("File not found: " + file.getAbsolutePath());
        }

        try (Reader reader = new FileReader(file);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim()
                     .withAllowMissingColumnNames(true))) {
            
            System.out.println("✅ (3) Code was able to process the CSV file: " + file.getName());

            Map<String, Integer> headerMap = csvParser.getHeaderMap();
            List<String> headers = new ArrayList<>(headerMap.keySet());

            if (headers.isEmpty() || headers.size() < 2) {
                dataFile.getLogger().printExceptionById("DASOC_00006");
                throw new Exception(ErrorDictionary.getInstance().getTable().get("DASOC_00006").getDetail());
            }

            String originalIdColumn = headers.get(0); // First column is originalID
            dataFile.getLogger().println(String.format("Using column '%s' for originalID", originalIdColumn));
            dataFile.getLogger().println(String.format("Found %d property columns", headers.size() - 1));
            
            // Log all property column headers for debugging
            for (int i = 1; i < headers.size(); i++) {
                dataFile.getLogger().println(String.format("  Property column %d: %s", i, headers.get(i)));
            }

            // Process each row
            for (CSVRecord record : csvParser) {
                totalCSVRows++; // Count all CSV data rows
                
                try {
                    String originalId = record.get(0).trim(); // First column
                    
                    if (originalId.isEmpty()) {
                        skippedRows++;
                        continue; // Skip empty originalID
                    }

                    // Look up object URI
                    String objectUri = originalIdToUriMap.get(originalId);
                    if (objectUri == null) {
                        dataFile.getLogger().printWarningByIdWithArgs("DASOC_00003", 
                            originalId, record.getRecordNumber());
                        skippedRows++;
                        continue;
                    }

                    // Create resource for the object
                    Resource subject = model.createResource(objectUri);

                    // Add properties from remaining columns
                    for (int i = 1; i < headers.size(); i++) {
                        String predicateUri = headers.get(i);
                        String value = record.get(i).trim();

                        if (value.isEmpty()) {
                            continue; // Skip empty values
                        }

                        // Expand predicate URI if it uses prefixes
                        predicateUri = URIUtils.replacePrefixEx(predicateUri);

                        // Create property
                        Property predicate = model.createProperty(predicateUri);
                        
                        // Determine if value is a URI or literal
                        if (URIUtils.isValidURI(value)) {
                            // Value is a URI - create resource
                            Resource object = model.createResource(value);
                            model.add(subject, predicate, object);
                        } else {
                            // Value is a literal
                            model.add(subject, predicate, value);
                        }
                    }

                    // Add timestamp to each object
                    Property timestampProp = model.createProperty(TIMESTAMP_PREDICATE);
                    model.add(subject, timestampProp, timestamp);

                    totalRows++;
                    objectsCreated++; // Count successfully created objects
                    currentBatchCount++;

                    // Save batch every BATCH_SIZE rows
                    if (currentBatchCount >= BATCH_SIZE) {
                        // Store DASOC data in separate graph to prevent DA.save() from deleting it
                        saveModelToTriplestore(model, daUri + "-dasoc", dataFile);
                        dataFile.getLogger().println(String.format("Saved batch of %d rows (total: %d)", 
                            currentBatchCount, totalRows));
                        
                        // Reset for next batch
                        model = ModelFactory.createDefaultModel();
                        addNamespacePrefixes(model);
                        currentBatchCount = 0;
                    }

                } catch (Exception e) {
                    errorRows++;
                    dataFile.getLogger().printWarningByIdWithArgs("DASOC_00007", 
                        record.getRecordNumber(), e.getMessage());
                }
            }

            // Save remaining records
            if (currentBatchCount > 0) {
                // Store DASOC data in separate graph to prevent DA.save() from deleting it
                saveModelToTriplestore(model, daUri + "-dasoc", dataFile);
                dataFile.getLogger().println(String.format("Saved final batch of %d rows (total: %d)", 
                    currentBatchCount, totalRows));
            }
            
            // Log summary with all requested metrics
            dataFile.getLogger().println(String.format("\n=== DASOC Ingestion Summary ==="));
            dataFile.getLogger().println(String.format("✅ (3) Total rows in DA-SOC-API.csv: %d (excluding header)", totalCSVRows));
            dataFile.getLogger().println(String.format("✅ (4) Number of objects created: %d", objectsCreated));
            if (skippedRows > 0) {
                dataFile.getLogger().println(String.format("Rows skipped (empty/invalid originalID): %d", skippedRows));
            }
            if (errorRows > 0) {
                dataFile.getLogger().println(String.format("Rows with errors: %d", errorRows));
            }
            
            // Console output for monitoring
            System.out.println(String.format("[DASOC] (3) Total CSV rows: %d", totalCSVRows));
            System.out.println(String.format("✅ (4) How many objects were submitted: %d", objectsCreated));
            System.out.println(String.format("[DASOC] Processing complete - %d objects successfully created from %d CSV rows", objectsCreated, totalCSVRows));

        } catch (IOException e) {
            throw new Exception("Error reading CSV file: " + e.getMessage(), e);
        }

        return totalRows;
    }

    /**
     * Add common namespace prefixes to the model
     */
    private static void addNamespacePrefixes(Model model) {
        model.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        model.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        model.setNsPrefix("hasco", "http://hadatac.org/ont/hasco#");
        
        // Add pharma namespace if it exists
        NameSpace pharmaNamespace = NameSpaces.getInstance().getNamespaces().get("pharma");
        if (pharmaNamespace != null && pharmaNamespace.getUri() != null) {
            model.setNsPrefix("pharma", pharmaNamespace.getUri());
        }
    }

    /**
     * Save RDF model to triplestore in named graph
     */
    private static void saveModelToTriplestore(Model model, String namedGraphUri, DataFile dataFile) {
        try {
            System.out.println("\n[DASOC] saveModelToTriplestore() called");
            System.out.println("[DASOC]   Model size: " + model.size() + " triples");
            System.out.println("[DASOC]   Target graph: " + namedGraphUri);
            
            // Serialize Jena model to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            RDFDataMgr.write(baos, model, RDFFormat.TURTLE);
            byte[] modelBytes = baos.toByteArray();
            
            System.out.println("[DASOC]   Serialized to " + modelBytes.length + " bytes");
            
            // Use GSPClient to post to named graph - requires SPARQL_GRAPH endpoint (/store/data), not SPARQL_QUERY
            String gspEndpoint = CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_GRAPH);
            System.out.println("[DASOC]   GSP endpoint: " + gspEndpoint);
            
            GSPClient gspClient = new GSPClient(gspEndpoint);
            
            System.out.println("[DASOC]   Calling GSPClient.postInputStream()...");
            gspClient.postInputStream(
                () -> new ByteArrayInputStream(modelBytes),
                "text/turtle",
                namedGraphUri
            );
            
            System.out.println("[DASOC]   ✅ GSPClient.postInputStream() completed successfully");
            dataFile.getLogger().println(String.format("Inserted %d triples into graph <%s>", 
                model.size(), namedGraphUri));

        } catch (Exception e) {
            System.err.println("[DASOC] ✗ ERROR in saveModelToTriplestore:");
            System.err.println("[DASOC]   Exception: " + e.getClass().getName());
            System.err.println("[DASOC]   Message: " + e.getMessage());
            e.printStackTrace();
            dataFile.getLogger().printException("Error saving to triplestore: " + e.getMessage());
        }
    }

    /**
     * Result class for ingestion operation
     */
    public static class IngestionResult {
        private boolean success;
        private int rowCount;
        private String errorMessage;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public int getRowCount() {
            return rowCount;
        }

        public void setRowCount(int rowCount) {
            this.rowCount = rowCount;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        @Override
        public String toString() {
            if (success) {
                return String.format("✅ DASOC Ingestion successful: %d rows processed", rowCount);
            } else {
                return String.format("❌ DASOC Ingestion failed: %s", errorMessage);
            }
        }
    }
}
