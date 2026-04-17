package org.hascoapi.ingestion;

import java.io.*;
import java.lang.String;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
import org.hascoapi.entity.pojo.Study;
import org.hascoapi.entity.pojo.StudyObject;
import org.hascoapi.entity.pojo.StudyObjectCollection;
import org.hascoapi.entity.pojo.Instrument;
import org.hascoapi.entity.pojo.Component;
import org.hascoapi.entity.pojo.ComponentStem;
import org.hascoapi.entity.pojo.ContainerSlot;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.ErrorDictionary;
import org.hascoapi.utils.GSPClient;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.vocabularies.VSTOI;
import org.hascoapi.vocabularies.HASCO;

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
     * NOTE: socUri parameter is now optional - we use Study-based approach instead
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
        String socUri = dataFile.getDasocSOCUri(); // Now optional - used only for backward compatibility
        
        System.out.println("Extracted DA URI from DataFile: " + (daUri != null ? daUri : "NULL"));
        System.out.println("Extracted SOC URI from DataFile (optional): " + (socUri != null ? socUri : "NULL"));
        
        if (daUri == null || daUri.isEmpty()) {
            System.out.println("[ERROR] DA URI is NULL or empty - marking chain as invalid");
            dataFile.getLogger().printException("DASOC DataAcquisition URI not set in DataFile");
            chain.setInvalid();
            return chain;
        }
        
        // SOC URI is now optional - we use Study-based approach
        // if (socUri == null || socUri.isEmpty()) {
        //     System.out.println("[INFO] SOC URI not provided - will use Study-based approach");
        // }
        
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
        System.out.println("[INGESTION PATH] Study-based approach (not using SOC)");

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

        try {
            // Decode URIs
            daUri = URLDecoder.decode(daUri, "UTF-8");

            dataFile.getLogger().println(String.format("DASOC ingestion for DA: <%s>", daUri));

            // Step 1: Get Study URI from DataFile or discover it
            String studyUri = dataFile.getStudyUri();
            
            // If not in DataFile, try to infer from DA metadata
            if (studyUri == null || studyUri.isEmpty()) {
                dataFile.getLogger().println("Study URI not in DataFile, attempting to discover...");
                System.out.println("[DASOC] Attempting to discover Study URI...");
                
                // Try to find study by querying for any object with isMemberOf
                studyUri = discoverStudyUri(dataFile);
            }
            
            if (studyUri == null || studyUri.isEmpty()) {
                result.setErrorMessage("Study URI is required for DASOC ingestion. Please ensure the DataFile has a study associated.");
                dataFile.getLogger().printException("Study URI not found in DataFile and could not be discovered");
                System.out.println("[ERROR] Study URI is NULL - dataFile.getStudyUri() returned: " + dataFile.getStudyUri());
                System.out.println("[ERROR] DataFile properties: hasSIRManagerEmail=" + dataFile.getHasSIRManagerEmail() + ", filename=" + dataFile.getFilename());
                return result;
            }
            
            // Expand study URI if it's a CURIE
            studyUri = URIUtils.replacePrefixEx(studyUri);
            dataFile.getLogger().println(String.format("Using Study: <%s>", studyUri));
            System.out.println("[DASOC] Study URI: " + studyUri);

            // Step 2: Ensure DA exists - create if necessary
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
                da.setIsMemberOfUri(studyUri); // Link DA to Study
                da.setHasStatus("UNPROCESSED");
                
                da.save();
                dataFile.getLogger().println(String.format("✅ Created DA record linked to study: <%s>", studyUri));
                System.out.println("Created new DA record: " + daUri);
            } else {
                dataFile.getLogger().println(String.format("Found existing DA: <%s>", daUri));
                // Update DA to ensure it's linked to study
                if (da.getIsMemberOfUri() == null || da.getIsMemberOfUri().isEmpty()) {
                    da.setIsMemberOfUri(studyUri);
                    da.save();
                    dataFile.getLogger().println(String.format("✅ Linked DA to study: <%s>", studyUri));
                }
                System.out.println("Using existing DA record: " + daUri);
            }

            // Step 3: Build originalID -> URI map for all objects in the Study
            dataFile.getLogger().println(String.format("Building originalID map for Study: <%s>", studyUri));
            Map<String, String> originalIdToUriMap = buildOriginalIdMapFromStudy(studyUri, dataFile);
            
            if (originalIdToUriMap == null || originalIdToUriMap.isEmpty()) {
                result.setErrorMessage("No StudyObjects found in Study with originalIDs");
                dataFile.getLogger().printException("No StudyObjects with originalIDs found in Study: " + studyUri);
                return result;
            }

            dataFile.getLogger().println(String.format("✅ Created originalID<->URI map with %d elements", originalIdToUriMap.size()));
            System.out.println(String.format("[DASOC] originalID map created: %d elements", originalIdToUriMap.size()));

            // Step 4: Process CSV file and add properties to matched objects
            int rowCount = processCSVFileWithStudy(file, dataFile, daUri, studyUri, originalIdToUriMap);
            
            if (rowCount > 0) {
                result.setSuccess(true);
                result.setRowCount(rowCount);
                dataFile.getLogger().println(String.format("✅ Successfully processed %d rows", rowCount));
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
     * Build a map of originalID -> object URI for all objects in the Study (not SOC-specific)
     * This allows DASOC files to add properties to any object in the study by matching originalID
     * 
     * Hierarchy: Object → isMemberOf → Collection → isMemberOf → Study
     */
    private static Map<String, String> buildOriginalIdMapFromStudy(String studyUri, DataFile dataFile) {
        Map<String, String> map = new HashMap<>();

        try {
            dataFile.getLogger().println("Querying all StudyObjects in Study with originalIDs...");
            
            // Query for all study objects in this Study with their originalIDs
            // Objects are members of Collections, and Collections are members of the Study
            // So we need to: Study → Collections → Objects with originalIDs
            // Search in both default graph and named graphs
            String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                    "SELECT DISTINCT ?objUri ?originalId WHERE { \n" +
                    "  { \n" +
                    "    ?collection hasco:isMemberOf <" + studyUri + "> . \n" +
                    "    ?objUri hasco:isMemberOf ?collection . \n" +
                    "    ?objUri hasco:originalID ?originalId . \n" +
                    "  } \n" +
                    "  UNION \n" +
                    "  { GRAPH ?g { \n" +
                    "      ?collection hasco:isMemberOf <" + studyUri + "> . \n" +
                    "    } \n" +
                    "    { \n" +
                    "      ?objUri hasco:isMemberOf ?collection . \n" +
                    "      ?objUri hasco:originalID ?originalId . \n" +
                    "    } \n" +
                    "    UNION \n" +
                    "    { GRAPH ?g2 { \n" +
                    "        ?objUri hasco:isMemberOf ?collection . \n" +
                    "        ?objUri hasco:originalID ?originalId . \n" +
                    "      } \n" +
                    "    } \n" +
                    "  } \n" +
                    "}";

            org.apache.jena.query.ResultSet results = SPARQLUtils.select(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY),
                    queryString);

            int count = 0;
            while (results.hasNext()) {
                org.apache.jena.query.QuerySolution soln = results.next();
                if (soln.get("objUri") != null && soln.get("originalId") != null) {
                    String objUri = soln.get("objUri").toString();
                    String originalId = soln.get("originalId").toString();
                    
                    // Skip if objUri is not a valid URI
                    if (!objUri.startsWith("http://") && !objUri.startsWith("https://")) {
                        dataFile.getLogger().printWarning("Skipping invalid URI: " + objUri);
                        continue;
                    }
                    
                    // Check for duplicate originalIDs
                    if (map.containsKey(originalId)) {
                        dataFile.getLogger().printWarning("Duplicate originalID found, using first occurrence: " + originalId);
                        continue;
                    }
                    
                    map.put(originalId, objUri);
                    count++;
                }
            }
            
            dataFile.getLogger().println(String.format("✅ Found %d StudyObjects with originalIDs in Study", count));
            System.out.println(String.format("[DASOC] Built originalID map: %d elements from Study", count));

        } catch (Exception e) {
            dataFile.getLogger().printException("Error building originalID map from Study: " + e.getMessage());
            e.printStackTrace();
        }

        return map;
    }

    /**
     * Process CSV file and add properties to matched objects (Study-based approach)
     */
    private static int processCSVFileWithStudy(File file, DataFile dataFile, String daUri, String studyUri, 
                                                Map<String, String> originalIdToUriMap) throws Exception {
        
        int totalRows = 0;
        int skippedRows = 0;
        int errorRows = 0;
        Model model = ModelFactory.createDefaultModel();
        
        // Add namespace prefixes
        addNamespacePrefixes(model);

        // Get timestamp for all triples
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(new Date());

        // Validate file exists
        if (!file.exists()) {
            throw new Exception("File not found: " + file.getAbsolutePath());
        }

        // Parse CSV
        try (FileReader reader = new FileReader(file);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            
            // Get headers
            Map<String, Integer> headerMap = csvParser.getHeaderMap();
            List<String> headers = new ArrayList<>(headerMap.keySet());
            
            if (headers.size() < 2) {
                dataFile.getLogger().printException("CSV must have at least 2 columns (originalID + property columns)");
                return 0;
            }
            
            String originalIdColumn = headers.get(0); // First column is always originalID
            dataFile.getLogger().println(String.format("Processing CSV with originalID column: '%s'", originalIdColumn));
            dataFile.getLogger().println(String.format("Property columns: %d", headers.size() - 1));
            
            // Process each row
            for (CSVRecord record : csvParser) {
                try {
                    String originalId = record.get(originalIdColumn).trim();
                    
                    if (originalId.isEmpty()) {
                        skippedRows++;
                        continue;
                    }
                    
                    // Find object URI by originalID
                    String objectUri = originalIdToUriMap.get(originalId);
                    if (objectUri == null) {
                        dataFile.getLogger().printWarning(String.format(
                            "Row %d: originalID '%s' not found in Study", record.getRecordNumber(), originalId));
                        skippedRows++;
                        continue;
                    }
                    
                    // Create resource for the object
                    Resource objectResource = model.createResource(objectUri);
                    
                    // Add properties from remaining columns
                    boolean hasProperties = false;
                    for (int i = 1; i < headers.size(); i++) {
                        String propertyUri = headers.get(i);
                        String value = record.get(propertyUri).trim();
                        
                        if (!value.isEmpty() && !propertyUri.isEmpty()) {
                            Property property = model.createProperty(propertyUri);
                            objectResource.addProperty(property, value);
                            hasProperties = true;
                        }
                    }
                    
                    if (hasProperties) {
                        // Add timestamp
                        Property timestampProp = model.createProperty(TIMESTAMP_PREDICATE);
                        objectResource.addProperty(timestampProp, timestamp);
                        
                        // Link to DA
                        Property hasDAProperty = model.createProperty("http://hadatac.org/ont/hasco/hasDataAcquisition");
                        objectResource.addProperty(hasDAProperty, model.createResource(daUri));
                        
                        totalRows++;
                    } else {
                        skippedRows++;
                    }
                    
                } catch (Exception e) {
                    errorRows++;
                    dataFile.getLogger().printWarning(String.format(
                        "Error processing row %d: %s", record.getRecordNumber(), e.getMessage()));
                }
            }
            
        } catch (Exception e) {
            dataFile.getLogger().printException("Error parsing CSV: " + e.getMessage());
            throw e;
        }

        // Save model to triplestore if we have data
        if (totalRows > 0) {
            String namedGraphUri = dataFile.getUri();
            saveModelToTriplestore(model, namedGraphUri, dataFile);
            
            dataFile.getLogger().println(String.format(
                "✅ Processing complete: %d rows processed, %d skipped, %d errors", 
                totalRows, skippedRows, errorRows));
        }

        return totalRows;
    }

    /**
     * Build a map of originalID -> object URI for all objects in the SOC (OLD METHOD - DEPRECATED)
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
     * Validate that all objects in the SOC belong to the same study
     * and return the study URI.
     * 
     * @param socUri The StudyObjectCollection URI
     * @param originalIdToUriMap Map of originalID -> object URI
     * @param dataFile DataFile for logging
     * @return Study URI if all objects belong to same study, null otherwise
     */
    private static String validateAndGetStudyUri(String socUri, Map<String, String> originalIdToUriMap, DataFile dataFile) {
        try {
            // First, get the study URI from the SOC itself
            StudyObjectCollection soc = StudyObjectCollection.find(socUri);
            if (soc == null) {
                dataFile.getLogger().printException("Cannot validate study: SOC not found: " + socUri);
                return null;
            }
            
            // Get study through isMemberOf relationship
            Study study = soc.getIsMemberOf();
            if (study == null) {
                String socIsMemberOfUri = soc.getIsMemberOfUri();
                dataFile.getLogger().printException("Cannot validate study: SOC has no study reference (isMemberOfUri=" + socIsMemberOfUri + "): " + socUri);
                return null;
            }
            
            String socStudyUri = study.getUri();
            if (socStudyUri == null || socStudyUri.isEmpty()) {
                dataFile.getLogger().printException("Cannot validate study: Study has no URI: " + socUri);
                return null;
            }
            
            dataFile.getLogger().println(String.format("SOC <%s> belongs to study: <%s>", socUri, socStudyUri));
            
            // All objects in the SOC inherit the study from the SOC itself
            // No need to validate each individual object - if they're in this SOC, they belong to this study
            dataFile.getLogger().println(
                String.format("✅ Study validation passed: all %d objects in SOC belong to study <%s>", 
                    originalIdToUriMap.size(), socStudyUri));
            
            return socStudyUri;
            
        } catch (Exception e) {
            dataFile.getLogger().printException("Error validating study: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
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

        try (Reader reader = new InputStreamReader(new FileInputStream(file), java.nio.charset.StandardCharsets.UTF_8);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim()
                     .withAllowMissingColumnNames(true))) {
            
            System.out.println("✅ (3) Code was able to process the CSV file: " + file.getName());

            Map<String, Integer> headerMap = csvParser.getHeaderMap();
            List<String> headers = new ArrayList<>(headerMap.keySet());
            
            // DEBUG: Log raw headers to see if there's BOM or encoding issues
            System.out.println("[DEBUG] Raw headers count: " + headers.size());
            for (int i = 0; i < Math.min(3, headers.size()); i++) {
                String h = headers.get(i);
                System.out.println("[DEBUG] Header[" + i + "]: '" + h + "' (length=" + h.length() + ", first char code=" + (h.length() > 0 ? (int)h.charAt(0) : "N/A") + ")");
            }
            
            // Remove BOM character if present in first header
            if (!headers.isEmpty()) {
                String firstHeader = headers.get(0);
                if (firstHeader.startsWith("\uFEFF") || firstHeader.startsWith("﻿")) {
                    // Remove BOM
                    String cleaned = firstHeader.replace("\uFEFF", "").replace("﻿", "").trim();
                    headers.set(0, cleaned);
                    dataFile.getLogger().println("Warning: BOM detected in CSV header, removed automatically");
                    System.out.println("[DEBUG] Removed BOM from first header: '" + firstHeader + "' -> '" + cleaned + "'");
                }
            }

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

                    // ===== NOVO: Enriquecer entidades vstoi (Instrument, Component, ComponentStem, ContainerSlot) =====
                    // Coleta todas as propriedades desta linha para enriquecer a entidade vstoi correspondente
                    Map<String, String> properties = new HashMap<>();
                    for (int i = 1; i < headers.size(); i++) {
                        String predicateUri = URIUtils.replacePrefixEx(headers.get(i));
                        String value = record.get(i).trim();
                        if (!value.isEmpty()) {
                            properties.put(predicateUri, value);
                        }
                    }
                    
                    // Tenta enriquecer a entidade vstoi correspondente
                    enrichVstoiEntity(objectUri, properties, dataFile);
                    // ===== FIM DO NOVO =====

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
            return String.format("❌ DASOC Ingestion failed: %s", errorMessage);
            }
        }
    }
    
    /**
     * Enriquece uma entidade vstoi (Instrument, Component, ComponentStem, ContainerSlot)
     * com as propriedades do DA-SOC CSV
     * 
     * @param objectUri URI do objeto (StudyObject)
     * @param properties Mapa de propriedades do CSV (predicateUri -> value)
     * @param dataFile DataFile para logging
     */
    private static void enrichVstoiEntity(String objectUri, Map<String, String> properties, DataFile dataFile) {
        if (properties.isEmpty()) {
            return; // Nada para enriquecer
        }
        
        try {
            // Busca o StudyObject para determinar o tipo
            StudyObject so = StudyObject.find(objectUri);
            if (so == null) {
                return; // Objeto não existe, skip
            }
            
            String typeUri = so.getTypeUri();
            if (typeUri == null || typeUri.isEmpty()) {
                return; // Sem tipo, skip
            }
            
            // Detecta qual tipo de entidade vstoi é
            String vstoiType = detectVstoiType(typeUri);
            
            if (vstoiType == null) {
                return; // Não é tipo vstoi, skip
            }
            
            // Enriquece a entidade apropriada
            if (VSTOI.INSTRUMENT.equals(vstoiType)) {
                enrichInstrument(objectUri, properties, dataFile);
            } else if (VSTOI.COMPONENT.equals(vstoiType)) {
                enrichComponent(objectUri, properties, dataFile);
            } else if (VSTOI.COMPONENT_STEM.equals(vstoiType)) {
                enrichComponentStem(objectUri, properties, dataFile);
            } else if (VSTOI.CONTAINER_SLOT.equals(vstoiType)) {
                enrichContainerSlot(objectUri, properties, dataFile);
            }
            
        } catch (Exception e) {
            // Silently skip errors - a entidade pode não existir ainda
            // dataFile.getLogger().println("Warning: Could not enrich vstoi entity " + objectUri + ": " + e.getMessage());
        }
    }
    
    /**
     * Detecta o tipo vstoi de um rdf:type
     */
    private static String detectVstoiType(String typeUri) {
        // Verificação direta
        if (VSTOI.INSTRUMENT.equals(typeUri)) return VSTOI.INSTRUMENT;
        if (VSTOI.COMPONENT.equals(typeUri)) return VSTOI.COMPONENT;
        if (VSTOI.COMPONENT_STEM.equals(typeUri)) return VSTOI.COMPONENT_STEM;
        if (VSTOI.CONTAINER_SLOT.equals(typeUri)) return VSTOI.CONTAINER_SLOT;
        
        // Verificação por substring (subclasses)
        if (typeUri.contains("Detector")) return VSTOI.COMPONENT;
        if (typeUri.contains("ComponentStem")) return VSTOI.COMPONENT_STEM;
        if (typeUri.contains("ContainerSlot")) return VSTOI.CONTAINER_SLOT;
        if (typeUri.contains("Questionnaire")) return VSTOI.INSTRUMENT;
        if (typeUri.contains("PhysicalInstrument")) return VSTOI.INSTRUMENT;
        if (typeUri.contains("SimulationModel")) return VSTOI.INSTRUMENT;
        
        return null;
    }
    
    /**
     * Enriquece um Instrument com propriedades do DA-SOC
     */
    private static void enrichInstrument(String uri, Map<String, String> properties, DataFile dataFile) {
        Instrument instrument = Instrument.find(uri);
        if (instrument == null) {
            return;
        }
        
        boolean modified = false;
        
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String prop = entry.getKey();
            String value = entry.getValue();
            
            // Mapeia propriedades para setters do Instrument
            if (prop.endsWith("hasShortName") || prop.contains("hasShortName")) {
                instrument.setHasShortName(value);
                modified = true;
            } else if (prop.endsWith("hasLanguage") || prop.contains("hasLanguage")) {
                instrument.setHasLanguage(value);
                modified = true;
            } else if (prop.endsWith("hasVersion") || prop.contains("hasVersion")) {
                instrument.setHasVersion(value);
                modified = true;
            } else if (prop.endsWith("hasMaker") || prop.contains("hasMaker")) {
                instrument.setHasMakerUri(URIUtils.replacePrefixEx(value));
                modified = true;
            } else if (prop.endsWith("hasWebDocument") || prop.contains("hasWebDocument")) {
                instrument.setHasWebDocument(value);
                modified = true;
            } else if (prop.endsWith("hasFirst") || prop.contains("hasFirst")) {
                instrument.setHasFirst(URIUtils.replacePrefixEx(value));
                modified = true;
            } else if (prop.endsWith("hasImage") || prop.contains("hasImage")) {
                instrument.setHasImageUri(value);
                modified = true;
            } else if (prop.endsWith("subClassOf") || prop.contains("subClassOf")) {
                instrument.setSuperUri(URIUtils.replacePrefixEx(value));
                modified = true;
            } else if (prop.endsWith("maxLoggedMeasurements") || prop.contains("maxLoggedMeasurements")) {
                // Note: Instrument pode não ter este setter, verificar depois
                modified = true;
            } else if (prop.endsWith("minOperatingTemperature") || prop.contains("minOperatingTemperature")) {
                // Note: Instrument pode não ter este setter, verificar depois
                modified = true;
            } else if (prop.endsWith("maxOperatingTemperature") || prop.contains("maxOperatingTemperature")) {
                // Note: Instrument pode não ter este setter, verificar depois
                modified = true;
            } else if (prop.endsWith("hasOperatingTemperatureUnit") || prop.contains("hasOperatingTemperatureUnit")) {
                // Note: Instrument pode não ter este setter, verificar depois
                modified = true;
            }
        }
        
        if (modified) {
            instrument.save();
            dataFile.getLogger().println("  Enriched Instrument: " + instrument.getLabel());
        }
    }
    
    /**
     * Enriquece um Component com propriedades do DA-SOC
     */
    private static void enrichComponent(String uri, Map<String, String> properties, DataFile dataFile) {
        Component component = Component.find(uri);
        if (component == null) {
            return;
        }
        
        boolean modified = false;
        
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String prop = entry.getKey();
            String value = entry.getValue();
            
            if (prop.endsWith("hasComponentStem") || prop.contains("hasComponentStem")) {
                component.setHasComponentStem(URIUtils.replacePrefixEx(value));
                modified = true;
            } else if (prop.endsWith("hasCodebook") || prop.contains("hasCodebook")) {
                component.setHasCodebook(URIUtils.replacePrefixEx(value));
                modified = true;
            } else if (prop.endsWith("hasLanguage") || prop.contains("hasLanguage")) {
                component.setHasLanguage(value);
                modified = true;
            } else if (prop.endsWith("hasVersion") || prop.contains("hasVersion")) {
                component.setHasVersion(value);
                modified = true;
            } else if (prop.endsWith("hasWebDocument") || prop.contains("hasWebDocument")) {
                component.setHasWebDocument(value);
                modified = true;
            }
        }
        
        if (modified) {
            component.save();
            dataFile.getLogger().println("  Enriched Component: " + component.getLabel());
        }
    }
    
    /**
     * Enriquece um ComponentStem com propriedades do DA-SOC
     */
    private static void enrichComponentStem(String uri, Map<String, String> properties, DataFile dataFile) {
        ComponentStem stem = ComponentStem.find(uri);
        if (stem == null) {
            return;
        }
        
        boolean modified = false;
        
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String prop = entry.getKey();
            String value = entry.getValue();
            
            if (prop.endsWith("hasContent") || prop.contains("hasContent")) {
                stem.setHasContent(value);
                modified = true;
            } else if (prop.endsWith("hasLanguage") || prop.contains("hasLanguage")) {
                stem.setHasLanguage(value);
                modified = true;
            } else if (prop.endsWith("hasVersion") || prop.contains("hasVersion")) {
                stem.setHasVersion(value);
                modified = true;
            } else if (prop.endsWith("hasWebDocument") || prop.contains("hasWebDocument")) {
                stem.setHasWebDocument(value);
                modified = true;
            }
        }
        
        if (modified) {
            stem.save();
            dataFile.getLogger().println("  Enriched ComponentStem: " + stem.getLabel());
        }
    }
    
    /**
     * Enriquece um ContainerSlot com propriedades do DA-SOC
     */
    private static void enrichContainerSlot(String uri, Map<String, String> properties, DataFile dataFile) {
        ContainerSlot slot = ContainerSlot.find(uri);
        if (slot == null) {
            return;
        }
        
        boolean modified = false;
        
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String prop = entry.getKey();
            String value = entry.getValue();
            
            if (prop.endsWith("belongsTo") || prop.contains("belongsTo")) {
                slot.setBelongsTo(URIUtils.replacePrefixEx(value));
                modified = true;
            } else if (prop.endsWith("hasComponent") || prop.contains("hasComponent")) {
                slot.setHasComponent(URIUtils.replacePrefixEx(value));
                modified = true;
            } else if (prop.endsWith("hasNext") || prop.contains("hasNext")) {
                slot.setHasNext(URIUtils.replacePrefixEx(value));
                modified = true;
            } else if (prop.endsWith("hasPrevious") || prop.contains("hasPrevious")) {
                slot.setHasPrevious(URIUtils.replacePrefixEx(value));
                modified = true;
            } else if (prop.endsWith("hasPriority") || prop.contains("hasPriority")) {
                slot.setHasPriority(value);
                modified = true;
            }
        }
        
        if (modified) {
            slot.save();
            dataFile.getLogger().println("  Enriched ContainerSlot: " + slot.getLabel());
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
    
    /**
     * Discover Study URI by following the relationship chain:
     * originalID -> Object -> SOC (StudyObjectCollection) -> Study
     * 
     * This reads a sample of originalIDs from the CSV file and traces back to the Study.
     */
    private static String discoverStudyUri(DataFile dataFile) {
        try {
            File file = dataFile.getFile();
            if (file == null || !file.exists()) {
                dataFile.getLogger().printWarning("Cannot discover Study: DataFile has no physical file");
                return null;
            }
            
            dataFile.getLogger().println("Discovering Study URI by tracing originalID -> Object -> Study");
            System.out.println("[DASOC] Starting Study discovery from CSV originalIDs...");
            
            // Read first few originalIDs from CSV (skip header)
            try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
                 CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                         .withFirstRecordAsHeader()
                         .withIgnoreHeaderCase()
                         .withTrim())) {
                
                // Get first column name (originalID column)
                Map<String, Integer> headerMap = csvParser.getHeaderMap();
                List<String> headers = new ArrayList<>(headerMap.keySet());
                
                if (headers.isEmpty()) {
                    dataFile.getLogger().printWarning("CSV has no headers");
                    return null;
                }
                
                // Clean BOM if present
                String firstHeader = headers.get(0);
                if (firstHeader.startsWith("\uFEFF") || firstHeader.startsWith("﻿")) {
                    firstHeader = firstHeader.replace("\uFEFF", "").replace("﻿", "").trim();
                }
                
                // Try first 10 rows to find a valid originalID
                int rowsChecked = 0;
                for (CSVRecord record : csvParser) {
                    if (rowsChecked >= 10) break; // Limit search
                    rowsChecked++;
                    
                    String originalId = record.get(0).trim();
                    if (originalId.isEmpty()) {
                        continue;
                    }
                    
                    System.out.println("[DASOC] Attempting Study discovery with originalID: " + originalId);
                    
                    // Strategy 1: Find object by originalID, then get its direct Study membership
                    // Objects might be directly linked to Study via hasco:isMemberOf
                    String queryString1 = NameSpaces.getInstance().printSparqlNameSpaceList() +
                            "SELECT ?object ?study WHERE { \n" +
                            "  { \n" +
                            "    ?object hasco:originalID \"" + originalId + "\" . \n" +
                            "    ?object hasco:isMemberOf ?study . \n" +
                            "    ?study a hasco:Study . \n" +
                            "  } \n" +
                            "  UNION \n" +
                            "  { GRAPH ?g { \n" +
                            "      ?object hasco:originalID \"" + originalId + "\" . \n" +
                            "      ?object hasco:isMemberOf ?study . \n" +
                            "      ?study a hasco:Study . \n" +
                            "    } \n" +
                            "  } \n" +
                            "} LIMIT 1";
                    
                    org.apache.jena.query.ResultSet results1 = SPARQLUtils.select(
                            CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY),
                            queryString1);
                    
                    if (results1.hasNext()) {
                        org.apache.jena.query.QuerySolution soln = results1.next();
                        String studyUri = soln.get("study") != null ? soln.get("study").toString() : null;
                        
                        if (studyUri != null && !studyUri.isEmpty()) {
                            dataFile.getLogger().println("✅ Discovered Study URI (direct object→study link):");
                            dataFile.getLogger().println("   originalID: " + originalId);
                            dataFile.getLogger().println("   → Study: " + studyUri);
                            
                            System.out.println("[DASOC] ✅ Study discovered (Strategy 1: direct link):");
                            System.out.println("[DASOC]    originalID: " + originalId);
                            System.out.println("[DASOC]    → Study: " + studyUri);
                            
                            return studyUri;
                        }
                    }
                    
                    // Strategy 2: Find object, then its collection, then collection's study
                    String queryString2 = NameSpaces.getInstance().printSparqlNameSpaceList() +
                            "SELECT ?object ?collection ?study WHERE { \n" +
                            "  { \n" +
                            "    ?object hasco:originalID \"" + originalId + "\" . \n" +
                            "    ?object hasco:isMemberOf ?collection . \n" +
                            "    ?collection hasco:isMemberOf ?study . \n" +
                            "  } \n" +
                            "  UNION \n" +
                            "  { GRAPH ?g { \n" +
                            "      ?object hasco:originalID \"" + originalId + "\" . \n" +
                            "      ?object hasco:isMemberOf ?collection . \n" +
                            "    } \n" +
                            "    { ?collection hasco:isMemberOf ?study . } \n" +
                            "    UNION \n" +
                            "    { GRAPH ?g2 { ?collection hasco:isMemberOf ?study . } } \n" +
                            "  } \n" +
                            "} LIMIT 1";
                    
                    org.apache.jena.query.ResultSet results2 = SPARQLUtils.select(
                            CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY),
                            queryString2);
                    
                    if (results2.hasNext()) {
                        org.apache.jena.query.QuerySolution soln = results2.next();
                        String studyUri = soln.get("study") != null ? soln.get("study").toString() : null;
                        String collectionUri = soln.get("collection") != null ? soln.get("collection").toString() : null;
                        
                        if (studyUri != null && !studyUri.isEmpty()) {
                            dataFile.getLogger().println("✅ Discovered Study URI (via collection chain):");
                            dataFile.getLogger().println("   originalID: " + originalId);
                            dataFile.getLogger().println("   → Collection: " + collectionUri);
                            dataFile.getLogger().println("   → Study: " + studyUri);
                            
                            System.out.println("[DASOC] ✅ Study discovered (Strategy 2: collection chain):");
                            System.out.println("[DASOC]    originalID: " + originalId);
                            System.out.println("[DASOC]    → Collection: " + collectionUri);
                            System.out.println("[DASOC]    → Study: " + studyUri);
                            
                            return studyUri;
                        }
                    }
                    
                    // Strategy 3: Just find ANY object with this originalID and see what we get
                    String debugQuery = NameSpaces.getInstance().printSparqlNameSpaceList() +
                            "SELECT ?object ?p ?o WHERE { \n" +
                            "  { \n" +
                            "    ?object hasco:originalID \"" + originalId + "\" . \n" +
                            "    ?object ?p ?o . \n" +
                            "  } \n" +
                            "  UNION \n" +
                            "  { GRAPH ?g { \n" +
                            "      ?object hasco:originalID \"" + originalId + "\" . \n" +
                            "      ?object ?p ?o . \n" +
                            "    } \n" +
                            "  } \n" +
                            "} LIMIT 10";
                    
                    org.apache.jena.query.ResultSet debugResults = SPARQLUtils.select(
                            CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY),
                            debugQuery);
                    
                    if (debugResults.hasNext()) {
                        System.out.println("[DASOC] Found object with originalID " + originalId + ", examining properties:");
                        while (debugResults.hasNext()) {
                            org.apache.jena.query.QuerySolution soln = debugResults.next();
                            String p = soln.get("p") != null ? soln.get("p").toString() : "null";
                            String o = soln.get("o") != null ? soln.get("o").toString() : "null";
                            System.out.println("[DASOC]    " + p + " -> " + o);
                        }
                    } else {
                        System.out.println("[DASOC] ✗ No object found with originalID: " + originalId);
                    }
                }
                
                dataFile.getLogger().printWarning("Could not discover Study URI from any originalID in CSV");
                System.out.println("[DASOC] ✗ No Study found after checking " + rowsChecked + " originalIDs");
                
            } catch (IOException e) {
                dataFile.getLogger().printWarning("Error reading CSV for Study discovery: " + e.getMessage());
            }
            
        } catch (Exception e) {
            dataFile.getLogger().printWarning("Error discovering Study URI: " + e.getMessage());
            System.out.println("[DASOC] Error discovering Study: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
}
