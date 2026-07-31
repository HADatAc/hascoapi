package org.hascoapi.ingestion;

import java.io.*;
import java.lang.String;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

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
import org.hascoapi.entity.pojo.Codebook;
import org.hascoapi.entity.pojo.ResponseOption;
import org.hascoapi.entity.pojo.AnnotationStem;
import org.hascoapi.RepositoryInstance;
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
    private static final Map<String, String> DASOC_DEFAULT_PREDICATES = createDasocDefaultPredicates();

    private static Map<String, String> createDasocDefaultPredicates() {
        Map<String, String> map = new HashMap<>();
        map.put("containedinplace", "http://schema.org/containedInPlace");
        map.put("sameas", "http://schema.org/sameAs");
        map.put("alternatename", "http://schema.org/alternateName");
        map.put("url", "http://schema.org/url");
        map.put("name", "http://schema.org/name");
        map.put("description", "http://schema.org/description");
        map.put("latitude", "http://schema.org/latitude");
        map.put("longitude", "http://schema.org/longitude");
        return map;
    }

    private static String normalizePredicateHeader(String predicateHeader) {
        if (predicateHeader == null) {
            return "";
        }

        String trimmed = predicateHeader.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        String expanded = URIUtils.replacePrefixEx(trimmed);
        if (expanded != null && (expanded.startsWith("http://") || expanded.startsWith("https://"))) {
            return expanded;
        }

        // If the header is a bare token, map common DA-SOC columns to schema.org predicates.
        if (!trimmed.contains(":")) {
            String mapped = DASOC_DEFAULT_PREDICATES.get(trimmed.toLowerCase());
            if (mapped != null) {
                return mapped;
            }

            // For unprefixed headers, use the repository default namespace (e.g., pharma:).
            // This prevents Jena from resolving bare names against a localhost base URI.
            String defaultNamespace = null;
            if (RepositoryInstance.getInstance() != null) {
                defaultNamespace = RepositoryInstance.getInstance().getHasDefaultNamespaceURL();
            }
            if (defaultNamespace != null && !defaultNamespace.trim().isEmpty()) {
                String ns = defaultNamespace.trim();
                if (!ns.endsWith("/") && !ns.endsWith("#")) {
                    ns = ns + "/";
                }
                return ns + trimmed;
            }
        }

        return expanded == null ? trimmed : expanded;
    }

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

                // Discovery fallback order:
                // 1) DA -> hasco:isMemberOf -> Study
                // 2) SOC (explicit or inferred) -> hasco:isMemberOf -> Study
                // 3) originalID tracing from CSV rows
                studyUri = discoverStudyUri(dataFile, daUri, socUri);

                if (studyUri != null && !studyUri.isEmpty()) {
                    dataFile.setStudyUri(studyUri);
                    dataFile.save();
                    dataFile.getLogger().println("Persisted discovered Study URI into DataFile: " + studyUri);
                }
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
                dataFile.getLogger().println(String.format("✅ Successfully ingested %d rows", rowCount));

                // Update DataFile status to PROCESSED
                dataFile.setFileStatus(DataFile.PROCESSED);
                dataFile.save();
                System.out.println("[DASOC] DataFile status set to PROCESSED");
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
     * 
     * NOW SUPPORTS PURE SIR OBJECTS: Also searches for SIR entities (Instrument, Component, etc.)
     * that were created without a StudyObject layer (new SIR-only approach)
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
                    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                    // 🔍 NEW: Search for PURE SIR objects (no StudyObject layer)
                    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                    // SIR objects created with the new SIR-only approach have originalID
                    // and are directly instances of vstoi:Instrument, vstoi:Component, etc.
                    // They don't have an intermediate StudyObject, so we search them directly
                    "  UNION \n" +
                    "  { \n" +
                    "    ?objUri hasco:originalID ?originalId . \n" +
                    "    ?objUri rdf:type ?type . \n" +
                    "    FILTER( \n" +
                    "      ?type = vstoi:Instrument || \n" +
                    "      ?type = vstoi:Component || \n" +
                    "      ?type = vstoi:ComponentStem || \n" +
                    "      ?type = vstoi:ContainerSlot || \n" +
                    "      ?type = vstoi:Codebook || \n" +
                    "      ?type = vstoi:ResponseOption || \n" +
                    "      ?type = vstoi:AnnotationStem \n" +
                    "    ) \n" +
                    "  } \n" +
                    "  UNION \n" +
                    "  { GRAPH ?g3 { \n" +
                    "      ?objUri hasco:originalID ?originalId . \n" +
                    "      ?objUri rdf:type ?type . \n" +
                    "      FILTER( \n" +
                    "        ?type = vstoi:Instrument || \n" +
                    "        ?type = vstoi:Component || \n" +
                    "        ?type = vstoi:ComponentStem || \n" +
                    "        ?type = vstoi:ContainerSlot || \n" +
                    "        ?type = vstoi:Codebook || \n" +
                    "        ?type = vstoi:ResponseOption || \n" +
                    "        ?type = vstoi:AnnotationStem \n" +
                    "      ) \n" +
                    "    } \n" +
                    "  } \n" +
                    "}";

            org.apache.jena.query.ResultSet results = SPARQLUtils.select(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY),
                    queryString);

            int count = 0;
            int sirCount = 0;
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

                    // Check for duplicate originalIDs and prefer canonical native URI shape.
                    if (map.containsKey(originalId)) {
                        String currentUri = map.get(originalId);
                        if (shouldReplaceWithCanonicalUri(currentUri, objUri)) {
                            map.put(originalId, objUri);
                            dataFile.getLogger().println("Duplicate originalID resolved to canonical URI: " + originalId +
                                    " [" + currentUri + " -> " + objUri + "]");
                        } else {
                            dataFile.getLogger().printWarning("Duplicate originalID found, keeping existing URI: " +
                                    originalId + " [kept=" + currentUri + ", ignored=" + objUri + "]");
                        }
                        continue;
                    }

                    map.put(originalId, objUri);
                    count++;
                    
                    // Count SIR objects for logging
                    if (objUri.contains("INST-") || objUri.contains("COMP-") || 
                        objUri.contains("ROPT-") || objUri.contains("CDBK-")) {
                        sirCount++;
                    }
                }
            }

            dataFile.getLogger().println(String.format("✅ Found %d objects with originalIDs in Study (%d SIR objects)", count, sirCount));
            System.out.println(String.format("[DASOC] Built originalID map: %d elements from Study (%d SIR)", count, sirCount));

        } catch (Exception e) {
            dataFile.getLogger().printException("Error building originalID map from Study: " + e.getMessage());
            e.printStackTrace();
        }

        return map;
    }

    private static boolean shouldReplaceWithCanonicalUri(String currentUri, String candidateUri) {
        if (candidateUri == null || candidateUri.isEmpty()) {
            return false;
        }
        if (currentUri == null || currentUri.isEmpty()) {
            return true;
        }
        boolean currentLegacy = isLegacySirInstanceUri(currentUri);
        boolean candidateLegacy = isLegacySirInstanceUri(candidateUri);
        if (currentLegacy && !candidateLegacy) {
            return true;
        }
        if (!currentLegacy && candidateLegacy) {
            return false;
        }
        // Stable fallback: keep existing mapping.
        return false;
    }

    private static boolean isLegacySirInstanceUri(String uri) {
        if (uri == null) {
            return false;
        }
        String upper = uri.toUpperCase();
        return upper.contains("INST-INS") ||
                upper.contains("COMP-COM") ||
                upper.contains("CSTEM-CSM") ||
                upper.contains("CB-CBK") ||
                upper.contains("ROPT-ROP") ||
                upper.contains("CTSLOT-CTS") ||
                upper.contains("ASTEM-ASM");
    }

    /**
     * Process CSV file and add properties to matched objects (Study-based approach)
     */
    private static int processCSVFileWithStudy(File file, DataFile dataFile, String daUri, String studyUri,
                                                Map<String, String> originalIdToUriMap) throws Exception {

        int totalRows = 0;
        int skippedRows = 0;
        int errorRows = 0;
        Set<String> unresolvedPredicateHeaders = new HashSet<>();
        Model model = ModelFactory.createDefaultModel();

        // Add namespace prefixes
        addNamespacePrefixes(model);

        // Get timestamp for all triples
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(new Date());

        // Validate file exists
        if (!file.exists()) {
            throw new Exception("File not found: " + file.getAbsolutePath());
        }

        // CRITICAL FIX: Pre-read first line to clean empty column headers
        // Apache Commons CSV throws IllegalArgumentException for empty header names
        // even with withAllowMissingColumnNames(true), so we need to replace them first
        String firstLine = null;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            firstLine = br.readLine();
        }
        
        if (firstLine == null || firstLine.trim().isEmpty()) {
            throw new Exception("CSV file is empty or has no header row");
        }
        
        // Split header line and replace empty columns with placeholders
        String[] rawHeaders = firstLine.split(",", -1); // -1 keeps trailing empty strings
        int emptyCount = 0;
        for (int i = 0; i < rawHeaders.length; i++) {
            if (rawHeaders[i] == null || rawHeaders[i].trim().isEmpty()) {
                rawHeaders[i] = "EMPTY_COL_" + i;
                emptyCount++;
            }
        }
        
        if (emptyCount > 0) {
            dataFile.getLogger().println(String.format("Warning: Replaced %d empty column headers with placeholders", emptyCount));
            System.out.println(String.format("[DASOC] Cleaned %d empty column headers", emptyCount));
        }

        // Parse CSV with cleaned headers
        try (BufferedReader br = new BufferedReader(new FileReader(file));
             CSVParser csvParser = CSVFormat.DEFAULT
                     .withHeader(rawHeaders) // Use our cleaned headers
                     .withSkipHeaderRecord(true) // Skip the original header line
                     .withAllowMissingColumnNames(true)
                     .parse(br)) {

            // Get headers (now cleaned)
            Map<String, Integer> headerMap = csvParser.getHeaderMap();
            // Keep header order exactly as in CSV and filter out our placeholder columns
            List<String> headers = headerMap.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .filter(key -> key != null && !key.startsWith("EMPTY_COL_")) // Filter out placeholder headers
                    .collect(Collectors.toList());

            // Remove BOM from the first header when present.
            if (!headers.isEmpty()) {
                String firstHeader = headers.get(0);
                if (firstHeader.startsWith("\uFEFF") || firstHeader.startsWith("﻿")) {
                    String cleaned = firstHeader.replace("\uFEFF", "").replace("﻿", "").trim();
                    headers.set(0, cleaned);
                    dataFile.getLogger().println("Warning: BOM detected in CSV header, removed automatically");
                }
            }

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

                    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                    // 🔍 DETECT OBJECT TYPE: Check if this is a pure SIR object or dual-layer
                    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                    // NEW APPROACH: SIR objects can be pure (no StudyObject layer)
                    // OLD APPROACH: SIR objects had dual-layer (StudyObject + VSTOI link)
                    // We need to handle both cases for backward compatibility
                    
                    boolean isPureSIRObject = isPureSIRObject(objectUri, dataFile);
                    boolean hasDualLayerVSTOI = false;
                    String vstoiInstanceUri = null;
                    
                    if (!isPureSIRObject) {
                        // Check if this has a VSTOI instance via dual-layer approach
                        hasDualLayerVSTOI = isVSTOIInstrument(objectUri, dataFile);
                        if (hasDualLayerVSTOI) {
                            vstoiInstanceUri = getInstrumentInstanceUri(objectUri, dataFile);
                        }
                    }
                    
                    Resource objectResource;
                    if (isPureSIRObject) {
                        // Pure SIR object - add properties directly to it
                        objectResource = model.createResource(objectUri);
                        dataFile.getLogger().println(String.format(
                            "  [SIR-PURE] Adding properties to pure SIR object: %s", originalId));
                    } else if (hasDualLayerVSTOI && vstoiInstanceUri != null && !vstoiInstanceUri.isEmpty()) {
                        // Dual-layer VSTOI - add properties to the specialized instance
                        objectResource = model.createResource(vstoiInstanceUri);
                        dataFile.getLogger().println(String.format(
                            "  [SIR-DUAL] Adding properties to dual-layer VSTOI instance: %s", originalId));
                    } else {
                        // Regular StudyObject - add properties to it
                        objectResource = model.createResource(objectUri);
                    }

                    // Add properties from remaining columns
                    boolean hasProperties = false;
                    for (int i = 1; i < headers.size(); i++) {
                        String propertyUri = headers.get(i);
                        String value = record.get(propertyUri).trim();

                        if (!value.isEmpty() && !propertyUri.isEmpty()) {
                            String expandedPropertyUri = normalizePredicateHeader(propertyUri);
                            if (expandedPropertyUri == null || expandedPropertyUri.isEmpty()) {
                                expandedPropertyUri = propertyUri;
                            }

                            // Debug visibility: capture headers that did not expand into absolute URIs.
                            if (!expandedPropertyUri.startsWith("http://") && !expandedPropertyUri.startsWith("https://")) {
                                unresolvedPredicateHeaders.add(propertyUri + " -> " + expandedPropertyUri);
                            }

                            Property property = model.createProperty(expandedPropertyUri);
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
                        
                        // For dual-layer VSTOI, also add DA link to the base StudyObject
                        if (hasDualLayerVSTOI && !objectResource.getURI().equals(objectUri)) {
                            Resource studyObjectResource = model.createResource(objectUri);
                            studyObjectResource.addProperty(hasDAProperty, model.createResource(daUri));
                        }

                        // ===== ENRICH POJO OBJECTS =====
                        // After adding RDF triples, also enrich the POJO objects
                        // This updates the in-memory Java objects that are used by the UI
                        if (isPureSIRObject || hasDualLayerVSTOI) {
                            // Determine which URI to enrich
                            String enrichUri = isPureSIRObject ? objectUri : vstoiInstanceUri;
                            
                            if (enrichUri != null && !enrichUri.isEmpty()) {
                                // Collect properties for enrichment
                                Map<String, String> propertiesMap = new HashMap<>();
                                for (int i = 1; i < headers.size(); i++) {
                                    String predicateUri = normalizePredicateHeader(headers.get(i));
                                    String columnName = headers.get(i);
                                    String value = record.get(columnName).trim();
                                    if (!value.isEmpty()) {
                                        propertiesMap.put(predicateUri, value);
                                    }
                                }

                                System.out.println("[ENRICH-TRACE] Row " + record.getRecordNumber() + ": originalId=" + originalId);
                                System.out.println("[ENRICH-TRACE]   Object URI: " + objectUri);
                                System.out.println("[ENRICH-TRACE]   Target URI (SIR): " + enrichUri);
                                System.out.println("[ENRICH-TRACE]   Is Pure SIR: " + isPureSIRObject);
                                System.out.println("[ENRICH-TRACE]   Properties collected: " + propertiesMap.size());
                                System.out.println("[ENRICH-TRACE]   Calling enrichVstoiEntity()");

                                // Enrich the SIR POJO instance
                                enrichVstoiEntity(enrichUri, propertiesMap, dataFile);
                            }
                        }
                        // ===== END ENRICH POJO OBJECTS =====

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

            if (!unresolvedPredicateHeaders.isEmpty()) {
                String unresolvedList = String.join(", ", unresolvedPredicateHeaders);
                dataFile.getLogger().printWarning(
                    "[STEP4->STEP5] Unresolved/non-URI predicate headers detected: " + unresolvedList);
                System.out.println(
                    "[STEP4->STEP5][WARNING] Unresolved/non-URI predicate headers: " + unresolvedList);
            }

            // Debug bridge between Step 4 (rows processed/model built) and Step 5 (graph write).
            long modelTripleCount = model.size();
            int beforeGraphCount = getGraphTripleCount(namedGraphUri, dataFile);
            String namespaceSummary = summarizePredicateNamespaces(model);

            dataFile.getLogger().println(String.format(
                "[STEP4->STEP5] Pre-write diagnostics: rowsProcessed=%d, modelTriples=%d, targetGraph=<%s>, graphTriplesBefore=%d",
                totalRows, modelTripleCount, namedGraphUri, beforeGraphCount));
            dataFile.getLogger().println("[STEP4->STEP5] Predicate namespace summary: " + namespaceSummary);

            System.out.println(String.format(
                "[STEP4->STEP5] rowsProcessed=%d modelTriples=%d targetGraph=%s graphTriplesBefore=%d",
                totalRows, modelTripleCount, namedGraphUri, beforeGraphCount));
            System.out.println("[STEP4->STEP5] predicateNamespaces=" + namespaceSummary);

            saveModelToTriplestore(model, namedGraphUri, dataFile);

            int afterGraphCount = getGraphTripleCount(namedGraphUri, dataFile);
            int delta = (beforeGraphCount >= 0 && afterGraphCount >= 0) ? (afterGraphCount - beforeGraphCount) : -1;

            dataFile.getLogger().println(String.format(
                "[STEP4->STEP5] Post-write diagnostics: graphTriplesAfter=%d, delta=%d",
                afterGraphCount, delta));
            System.out.println(String.format(
                "[STEP4->STEP5] graphTriplesAfter=%d delta=%d",
                afterGraphCount, delta));

            if (beforeGraphCount >= 0 && afterGraphCount >= 0 && modelTripleCount > 0 && delta <= 0) {
                dataFile.getLogger().printWarning(
                    "[STEP4->STEP5] Graph triple count did not increase after write. Check graph URI and predicate namespaces.");
                System.out.println(
                    "[STEP4->STEP5][WARNING] Graph triple count did not increase after write.");
            }

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
            
            // CRITICAL FIX: headerMap.keySet() has no guaranteed order - must sort by column index
            // ALSO: Filter out columns with empty/null header names (trailing commas in CSV)
            List<String> headers = headerMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .filter(key -> key != null && !key.trim().isEmpty()) // Filter out empty headers
                .collect(Collectors.toList());

            // DEBUG: Log raw headers to see if there's BOM or encoding issues
            System.out.println("[DEBUG] Raw headers count: " + headers.size());
            System.out.println("[DEBUG] Original headerMap size (before filtering): " + headerMap.size());
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
                    String originalId = record.get(originalIdColumn).trim(); // Access by column name
                    
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
                        String value = record.get(predicateUri).trim();  // Access by column name, not index

                        if (value.isEmpty()) {
                            continue; // Skip empty values
                        }

                        // Normalize predicate URI (prefix expansion + bare-token default namespace fallback)
                        predicateUri = normalizePredicateHeader(predicateUri);

                        // Create property
                        Property predicate = model.createProperty(predicateUri);
                        
                        // Determine if value is a URI or literal
                        // First try to expand it in case it uses prefixes
                        String expandedValue = URIUtils.replacePrefixEx(value);

                        if (URIUtils.isValidURI(expandedValue)) {
                            // Value is a URI - create resource with expanded URI
                            Resource object = model.createResource(expandedValue);
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
                        String predicateUri = normalizePredicateHeader(headers.get(i));
                        String columnName = headers.get(i);  // ✅ FIX: Use column name
                        String value = record.get(columnName).trim();  // ✅ FIX: Access by column name
                        if (!value.isEmpty()) {
                            properties.put(predicateUri, value);
                            System.out.println("[ENRICH-TRACE]     Property: " + columnName + " → " + predicateUri + " = " + value);
                        }
                    }

                    System.out.println("[ENRICH-TRACE] Row " + record.getRecordNumber() + ": originalId=" + originalId);
                    System.out.println("[ENRICH-TRACE]   StudyObject URI: " + objectUri);
                    System.out.println("[ENRICH-TRACE]   Properties collected: " + properties.size());

                    // ✅ CRITICAL FIX: Buscar URI da instância VSTOI (não o StudyObject!)
                    // Exemplo: pmsr:OBJ_instrumentcollection_INS... → pmsr:INST-INS...
                    String vstoiInstanceUri = getVSTOIInstanceUri(objectUri, dataFile);
                    System.out.println("[ENRICH-TRACE]   VSTOI Instance URI: " + vstoiInstanceUri);
                    
                    if (vstoiInstanceUri != null && !vstoiInstanceUri.isEmpty()) {
                        System.out.println("[ENRICH-TRACE]   Calling enrichVstoiEntity() with VSTOI URI");
                        // Enriquecer usando a URI da instância VSTOI
                        enrichVstoiEntity(vstoiInstanceUri, properties, dataFile);
                    } else {
                        System.out.println("[ENRICH-TRACE]   No VSTOI instance found, using StudyObject URI");
                        // Fallback: se não tem instância VSTOI, enriquecer o StudyObject
                        enrichVstoiEntity(objectUri, properties, dataFile);
                    }
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

    private static int getGraphTripleCount(String graphUri, DataFile dataFile) {
        try {
            String queryString = "SELECT (COUNT(*) AS ?n) WHERE { GRAPH <" + graphUri + "> { ?s ?p ?o } }";
            org.apache.jena.query.ResultSet results = SPARQLUtils.select(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY),
                    queryString);

            if (results.hasNext()) {
                org.apache.jena.query.QuerySolution soln = results.next();
                if (soln.get("n") != null && soln.get("n").isLiteral()) {
                    // Use lexical form instead of toString() to avoid values like
                    // "746^^http://www.w3.org/2001/XMLSchema#integer".
                    String lexical = soln.getLiteral("n").getLexicalForm();
                    return Integer.parseInt(lexical);
                }
            }
        } catch (Exception e) {
            dataFile.getLogger().printWarning("[STEP4->STEP5] Could not query graph triple count: " + e.getMessage());
            System.out.println("[STEP4->STEP5][WARNING] Could not query graph triple count: " + e.getMessage());
        }
        return -1;
    }

    private static String summarizePredicateNamespaces(Model model) {
        try {
            Map<String, Integer> counts = new HashMap<>();
            StmtIterator stmtIterator = model.listStatements();
            while (stmtIterator.hasNext()) {
                Statement st = stmtIterator.nextStatement();
                String predUri = st.getPredicate().getURI();
                String ns = extractNamespace(predUri);
                counts.put(ns, counts.getOrDefault(ns, 0) + 1);
            }

            return counts.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(", "));
        } catch (Exception e) {
            return "namespace-summary-error=" + e.getMessage();
        }
    }

    private static String extractNamespace(String uri) {
        if (uri == null || uri.isEmpty()) {
            return "<empty>";
        }
        int hashIdx = uri.lastIndexOf('#');
        int slashIdx = uri.lastIndexOf('/');
        int idx = Math.max(hashIdx, slashIdx);
        if (idx >= 0) {
            return uri.substring(0, idx + 1);
        }
        return uri;
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
     * @param vstoiInstanceUri URI da instância VSTOI (ex: pmsr:INST-INS..., pmsr:COMP-...)
     * @param properties Mapa de propriedades do CSV (predicateUri -> value)
     * @param dataFile DataFile para logging
     */
    private static void enrichVstoiEntity(String vstoiInstanceUri, Map<String, String> properties, DataFile dataFile) {
        if (properties.isEmpty()) {
            System.out.println("[DEBUG-ENRICH] enrichVstoiEntity() - NO properties to enrich for: " + vstoiInstanceUri);
            return; // Nada para enriquecer
        }

        System.out.println("[DEBUG-ENRICH] enrichVstoiEntity() called for: " + vstoiInstanceUri);
        System.out.println("[DEBUG-ENRICH] Properties count: " + properties.size());

        try {
            // Detecta o tipo VSTOI pelo URI (ex: INST-INS → Instrument, COMP- → Component)
            String vstoiType = detectVstoiTypeByUri(vstoiInstanceUri);
            
            if (vstoiType == null) {
                // Fallback: buscar no triplestore
                System.out.println("[DEBUG-ENRICH] Could not detect type from URI, querying triplestore...");
                vstoiType = detectVstoiTypeFromTriplestore(vstoiInstanceUri, dataFile);
            }

            System.out.println("[DEBUG-ENRICH] Detected VSTOI type: " + vstoiType);

            if (vstoiType == null) {
                System.out.println("[DEBUG-ENRICH] NOT a VSTOI type, skipping enrichment");
                return; // Não é tipo vstoi, skip
            }

            // Enriquece a entidade apropriada
            if (VSTOI.INSTRUMENT.equals(vstoiType)) {
                System.out.println("[DEBUG-ENRICH] Calling enrichInstrument()...");
                enrichInstrument(vstoiInstanceUri, properties, dataFile);
            } else if (VSTOI.COMPONENT.equals(vstoiType)) {
                System.out.println("[DEBUG-ENRICH] Calling enrichComponent()...");
                enrichComponent(vstoiInstanceUri, properties, dataFile);
            } else if (VSTOI.COMPONENT_STEM.equals(vstoiType)) {
                System.out.println("[DEBUG-ENRICH] Calling enrichComponentStem()...");
                enrichComponentStem(vstoiInstanceUri, properties, dataFile);
            } else if (VSTOI.CONTAINER_SLOT.equals(vstoiType)) {
                System.out.println("[DEBUG-ENRICH] Calling enrichContainerSlot()...");
                enrichContainerSlot(vstoiInstanceUri, properties, dataFile);
            } else if (VSTOI.CODEBOOK.equals(vstoiType)) {
                System.out.println("[DEBUG-ENRICH] Calling enrichCodebook()...");
                enrichCodebook(vstoiInstanceUri, properties, dataFile);
            } else if (VSTOI.RESPONSE_OPTION.equals(vstoiType)) {
                System.out.println("[DEBUG-ENRICH] Calling enrichResponseOption()...");
                enrichResponseOption(vstoiInstanceUri, properties, dataFile);
            } else if (VSTOI.ANNOTATION_STEM.equals(vstoiType)) {
                System.out.println("[DEBUG-ENRICH] Calling enrichAnnotationStem()...");
                enrichAnnotationStem(vstoiInstanceUri, properties, dataFile);
            }

        } catch (Exception e) {
            // Log errors for debugging
            System.out.println("[DEBUG-ENRICH] Exception in enrichVstoiEntity: " + e.getMessage());
            e.printStackTrace();
            dataFile.getLogger().println("Warning: Could not enrich vstoi entity " + vstoiInstanceUri + ": " + e.getMessage());
        }
    }

    /**
     * Detecta o tipo VSTOI pela estrutura do URI
     * Ex: pmsr:INST-INS... → VSTOI.INSTRUMENT
     *     pmsr:COMP-... → VSTOI.COMPONENT
     *     pmsr:CSTEM-... → VSTOI.COMPONENT_STEM
     */
    private static String detectVstoiTypeByUri(String uri) {
        if (uri.contains("INST-INS") || uri.contains("/INST-")) return VSTOI.INSTRUMENT;
        if (uri.contains("COMP-") || uri.contains("/COMP-")) return VSTOI.COMPONENT;
        if (uri.contains("CSTEM-") || uri.contains("ComponentStem")) return VSTOI.COMPONENT_STEM;
        if (uri.contains("CSLOT-") || uri.contains("ContainerSlot")) return VSTOI.CONTAINER_SLOT;
        if (uri.contains("CBK-") || uri.contains("Codebook")) return VSTOI.CODEBOOK;
        if (uri.contains("ROPT-") || uri.contains("ResponseOption")) return VSTOI.RESPONSE_OPTION;
        if (uri.contains("ASTEM-") || uri.contains("AnnotationStem")) return VSTOI.ANNOTATION_STEM;
        return null;
    }

    /**
     * Detecta o tipo VSTOI consultando o triplestore
     */
    private static String detectVstoiTypeFromTriplestore(String uri, DataFile dataFile) {
        try {
            String ns = NameSpaces.getInstance().printSparqlNameSpaceList();
            String queryString = ns +
                "SELECT ?type WHERE { \n" +
                "  { <" + uri + "> a ?type . } \n" +
                "  UNION \n" +
                "  { GRAPH ?g { <" + uri + "> a ?type . } } \n" +
                "} LIMIT 1";

            org.apache.jena.query.ResultSet results = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY),
                queryString);

            if (results.hasNext()) {
                org.apache.jena.query.QuerySolution soln = results.next();
                if (soln.get("type") != null) {
                    String typeUri = soln.get("type").toString();
                    return detectVstoiType(typeUri);
                }
            }
        } catch (Exception e) {
            System.out.println("[DEBUG-ENRICH] Error detecting type from triplestore: " + e.getMessage());
        }
        return null;
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
        if (VSTOI.CODEBOOK.equals(typeUri)) return VSTOI.CODEBOOK;
        if (VSTOI.RESPONSE_OPTION.equals(typeUri)) return VSTOI.RESPONSE_OPTION;
        if (VSTOI.ANNOTATION_STEM.equals(typeUri)) return VSTOI.ANNOTATION_STEM;

        // Verificação por substring (subclasses)
        if (typeUri.contains("Detector")) return VSTOI.COMPONENT;
        if (typeUri.contains("ComponentStem")) return VSTOI.COMPONENT_STEM;
        if (typeUri.contains("ContainerSlot")) return VSTOI.CONTAINER_SLOT;
        if (typeUri.contains("Questionnaire")) return VSTOI.INSTRUMENT;
        if (typeUri.contains("PhysicalInstrument")) return VSTOI.INSTRUMENT;
        if (typeUri.contains("SimulationModel")) return VSTOI.INSTRUMENT;
        if (typeUri.contains("Codebook")) return VSTOI.CODEBOOK;
        if (typeUri.contains("ResponseOption")) return VSTOI.RESPONSE_OPTION;
        if (typeUri.contains("AnnotationStem")) return VSTOI.ANNOTATION_STEM;

        return null;
    }

    /**
     * Enriquece um Instrument com propriedades do DA-SOC
     */
    private static void enrichInstrument(String uri, Map<String, String> properties, DataFile dataFile) {
        System.out.println("[DEBUG] enrichInstrument() called for URI: " + uri);
        System.out.println("[DEBUG] Properties to enrich: " + properties.size());
        
        Instrument instrument = Instrument.find(uri);
        if (instrument == null) {
            System.out.println("[DEBUG] Instrument.find() returned NULL for URI: " + uri);
            dataFile.getLogger().println("  Warning: Could not find Instrument for enrichment: " + uri);
            return;
        }

        System.out.println("[DEBUG] Instrument found! Label: " + instrument.getLabel());
        boolean modified = false;

        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String prop = entry.getKey();
            String value = entry.getValue();
            System.out.println("[DEBUG] Processing property: " + prop + " = " + value);

            // Mapeia propriedades para setters do Instrument
            if (prop.endsWith("hasShortName") || prop.contains("hasShortName")) {
                instrument.setHasShortName(value);
                modified = true;
            } else if (prop.endsWith("hasLanguage") || prop.contains("hasLanguage")) {
                instrument.setHasLanguage(cleanLiteralValue(value));
                modified = true;
            } else if (prop.endsWith("hasVersion") || prop.contains("hasVersion")) {
                instrument.setHasVersion(cleanLiteralValue(value));
                modified = true;
            } else if (prop.endsWith("hasMaker") || prop.contains("hasMaker")) {
                instrument.setHasMakerUri(URIUtils.replacePrefixEx(value));
                modified = true;
            } else if (prop.endsWith("hasWebDocument") || prop.contains("hasWebDocument")) {
                instrument.setHasWebDocument(value);
                modified = true;
            } else if (prop.endsWith("hasFirst") || prop.contains("hasFirst")) {
                // hasFirst references the first ContainerSlot (CTSLOT-CTS...)
                instrument.setHasFirst(convertToVstoiUri(value));
                modified = true;
            } else if (prop.endsWith("hasImage") || prop.contains("hasImage")) {
                instrument.setHasImageUri(value);
                modified = true;
            } else if (prop.endsWith("subClassOf") || prop.contains("subClassOf")) {
                // subClassOf references parent Instrument (INST-INS...)
                instrument.setSuperUri(convertToVstoiUri(value));
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
            System.out.println("[DEBUG] Instrument MODIFIED - saving changes for: " + instrument.getUri());
            instrument.save();
            dataFile.getLogger().println("  Enriched Instrument: " + instrument.getLabel());
        } else {
            System.out.println("[DEBUG] Instrument NOT modified (no matching properties) for: " + uri);
        }
    }
    
    /**
     * Cleans literal values by removing namespace prefixes that shouldn't be there.
     * For example: "pmsr:/en" -> "en", "pmsr:/1" -> "1"
     * 
     * @param value The value to clean
     * @return The cleaned literal value
     */
    private static String cleanLiteralValue(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        // Check if value starts with a namespace prefix pattern (prefix:/ or prefix:#)
        if (value.contains(":/") || value.contains(":#")) {
            // Extract just the value after the last separator
            if (value.contains(":/")) {
                String[] parts = value.split(":/");
                String cleanValue = parts[parts.length - 1];
                System.out.println("[DEBUG-LITERAL-CLEAN] Cleaned '" + value + "' -> '" + cleanValue + "'");
                return cleanValue;
            } else if (value.contains("#")) {
                String[] parts = value.split("#");
                String cleanValue = parts[parts.length - 1];
                System.out.println("[DEBUG-LITERAL-CLEAN] Cleaned '" + value + "' -> '" + cleanValue + "'");
                return cleanValue;
            }
        }
        
        // If no namespace prefix found, return as is
        return value;
    }

    /**
     * Helper method to convert originalID-based references to canonical VSTOI URIs
     * Examples:
     *   pmsr:/CBK1738096258564815 -> https://pmsr.net/ont/CBK1738096258564815
     *   pmsr:/CSM1738097871592315 -> https://pmsr.net/ont/CSM1738097871592315
     */
    private static String convertToVstoiUri(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        // Canonical single-layer model: URI = namespace + originalID (no INST-/COMP-/CSTEM- rewrites).
        return URIUtils.replacePrefixEx(value);
    }

    private static boolean uriExists(String uri) {
        if (uri == null || uri.isEmpty()) {
            return false;
        }
        try {
            String ns = NameSpaces.getInstance().printSparqlNameSpaceList();
            String queryString = ns +
                "SELECT ?p WHERE { { <" + uri + "> ?p ?o . } UNION { GRAPH ?g { <" + uri + "> ?p ?o . } } } LIMIT 1";
            org.apache.jena.query.ResultSet results = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY),
                queryString);
            return results != null && results.hasNext();
        } catch (Exception e) {
            // best-effort resolution; caller handles fallback
        }
        return false;
    }

    /**
     * Enriquece um Component com propriedades do DA-SOC
     */
    private static void enrichComponent(String uri, Map<String, String> properties, DataFile dataFile) {
        System.out.println("[DEBUG-COMPONENT] enrichComponent() called for URI: " + uri);
        System.out.println("[DEBUG-COMPONENT] Properties to enrich: " + properties.size());
        
        Component component = Component.find(uri);
        if (component == null) {
            System.out.println("[DEBUG-COMPONENT] Component.find() returned NULL for URI: " + uri);
            return;
        }

        System.out.println("[DEBUG-COMPONENT] Component found! Label: " + component.getLabel());
        boolean modified = false;

        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String prop = entry.getKey();
            String value = entry.getValue();
            System.out.println("[DEBUG-COMPONENT] Processing property: " + prop + " = " + value);

            if (prop.endsWith("hasComponentStem") || prop.contains("hasComponentStem")) {
                System.out.println("[DEBUG-COMPONENT] MATCHED hasComponentStem! Original value: " + value);
                String vstoiUri = convertToVstoiUri(value);
                System.out.println("[DEBUG-COMPONENT] Converted to VSTOI URI: " + vstoiUri);
                component.setHasComponentStem(vstoiUri);
                modified = true;
            } else if (prop.endsWith("hasCodebook") || prop.contains("hasCodebook")) {
                System.out.println("[DEBUG-COMPONENT] MATCHED hasCodebook! Original value: " + value);
                String vstoiUri = convertToVstoiUri(value);
                System.out.println("[DEBUG-COMPONENT] Converted to VSTOI URI: " + vstoiUri);
                component.setHasCodebook(vstoiUri);
                modified = true;
            } else if (prop.endsWith("isAttributeOf") || prop.contains("isAttributeOf")) {
                System.out.println("[DEBUG-COMPONENT] MATCHED isAttributeOf! Setting value: " + value);
                component.setIsAttributeOf(URIUtils.replacePrefixEx(value));
                modified = true;
            } else if (prop.endsWith("hasLanguage") || prop.contains("hasLanguage")) {
                component.setHasLanguage(cleanLiteralValue(value));
                modified = true;
            } else if (prop.endsWith("hasVersion") || prop.contains("hasVersion")) {
                component.setHasVersion(cleanLiteralValue(value));
                modified = true;
            } else if (prop.endsWith("hasWebDocument") || prop.contains("hasWebDocument")) {
                component.setHasWebDocument(value);
                modified = true;
            }
        }

        if (modified) {
            System.out.println("[DEBUG-COMPONENT] Component MODIFIED - saving changes for: " + component.getUri());
            component.save();
            
            // ✅ CRITICAL FIX: Re-fetch component to verify it was saved correctly
            Component savedComponent = Component.find(component.getUri());
            if (savedComponent != null) {
                String cbInfo = (savedComponent.getHasCodebook() != null && !savedComponent.getHasCodebook().isEmpty()) 
                    ? savedComponent.getHasCodebook() 
                    : "EMPTY";
                System.out.println("[DEBUG-COMPONENT] Component SAVED! Codebook after save: " + cbInfo);
                dataFile.getLogger().println("  Enriched Component: " + savedComponent.getLabel() + "  -- CB:" + cbInfo);
            } else {
                System.out.println("[DEBUG-COMPONENT] WARNING: Could not re-fetch component after save!");
                dataFile.getLogger().println("  Enriched Component: " + component.getLabel() + "  -- CB:NOTFOUND");
            }
        } else {
            System.out.println("[DEBUG-COMPONENT] Component NOT modified (no matching properties) for: " + uri);
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

            if (prop.endsWith("subClassOf") || prop.contains("subClassOf")) {
                // subClassOf references parent ComponentStem (CSTEM-CSM...)
                stem.setSuperUri(convertToVstoiUri(value));
                modified = true;
            } else if (prop.endsWith("hasContent") || prop.contains("hasContent")) {
                stem.setHasContent(value);
                modified = true;
            } else if (prop.endsWith("hasLanguage") || prop.contains("hasLanguage")) {
                stem.setHasLanguage(cleanLiteralValue(value));
                modified = true;
            } else if (prop.endsWith("hasVersion") || prop.contains("hasVersion")) {
                stem.setHasVersion(cleanLiteralValue(value));
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
                // belongsTo references Instrument (INST-INS...)
                slot.setBelongsTo(convertToVstoiUri(value));
                modified = true;
            } else if (prop.endsWith("hasComponent") || prop.contains("hasComponent")) {
                // hasComponent references Component (COMP-COM...)
                slot.setHasComponent(convertToVstoiUri(value));
                modified = true;
            } else if (prop.endsWith("hasNext") || prop.contains("hasNext")) {
                // hasNext references another ContainerSlot (CTSLOT-CTS...)
                slot.setHasNext(convertToVstoiUri(value));
                modified = true;
            } else if (prop.endsWith("hasPrevious") || prop.contains("hasPrevious")) {
                // hasPrevious references another ContainerSlot (CTSLOT-CTS...)
                slot.setHasPrevious(convertToVstoiUri(value));
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
     * Enriquece um Codebook com propriedades do DA-SOC
     */
    private static void enrichCodebook(String uri, Map<String, String> properties, DataFile dataFile) {
        System.out.println("[DEBUG-CODEBOOK] enrichCodebook() called for URI: " + uri);
        System.out.println("[DEBUG-CODEBOOK] Properties to enrich: " + properties.size());
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            System.out.println("[DEBUG-CODEBOOK]   Property: " + entry.getKey() + " = " + entry.getValue());
        }
        
        Codebook codebook = Codebook.find(uri);
        if (codebook == null) {
            System.out.println("[DEBUG-CODEBOOK] Codebook.find() returned NULL for URI: " + uri);
            dataFile.getLogger().println("  Warning: Could not find Codebook for enrichment: " + uri);
            return;
        }

        System.out.println("[DEBUG-CODEBOOK] Codebook found! Label: " + codebook.getLabel());
        boolean modified = false;

        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String prop = entry.getKey();
            String value = entry.getValue();
            System.out.println("[DEBUG-CODEBOOK] Processing property: " + prop + " = " + value);

            if (prop.endsWith("hasLanguage") || prop.contains("hasLanguage")) {
                // Clean literal value - remove namespace prefix if present
                String cleanValue = cleanLiteralValue(value);
                System.out.println("[DEBUG-CODEBOOK] Setting hasLanguage to: " + cleanValue);
                codebook.setHasLanguage(cleanValue);
                modified = true;
            } else if (prop.endsWith("hasVersion") || prop.contains("hasVersion")) {
                // Clean literal value - remove namespace prefix if present
                String cleanValue = cleanLiteralValue(value);
                System.out.println("[DEBUG-CODEBOOK] Setting hasVersion to: " + cleanValue);
                codebook.setHasVersion(cleanValue);
                modified = true;
            } else if (prop.endsWith("hasStatus") || prop.contains("hasStatus")) {
                // Clean literal value - remove namespace prefix if present
                String cleanValue = cleanLiteralValue(value);
                System.out.println("[DEBUG-CODEBOOK] Setting hasStatus to: " + cleanValue);
                codebook.setHasStatus(cleanValue);
                modified = true;
            } else if (prop.endsWith("hasSerialNumber") || prop.contains("hasSerialNumber")) {
                System.out.println("[DEBUG-CODEBOOK] Setting hasSerialNumber to: " + value);
                codebook.setSerialNumber(value);
                modified = true;
            } else if (prop.endsWith("hasReviewNote") || prop.contains("hasReviewNote")) {
                System.out.println("[DEBUG-CODEBOOK] Setting hasReviewNote to: " + value);
                codebook.setHasReviewNote(value);
                modified = true;
            } else {
                System.out.println("[DEBUG-CODEBOOK] Property not matched: " + prop);
            }
        }

        if (modified) {
            System.out.println("[DEBUG-CODEBOOK] Codebook MODIFIED - saving changes for: " + codebook.getUri());
            codebook.save();
            dataFile.getLogger().println("  Enriched Codebook: " + codebook.getLabel());
            System.out.println("[DEBUG-CODEBOOK] Codebook saved successfully");
        } else {
            System.out.println("[DEBUG-CODEBOOK] Codebook NOT modified (no matching properties) for: " + uri);
        }
    }
    
    /**
     * Enriquece um ResponseOption com propriedades do DA-SOC
     */
    private static void enrichResponseOption(String uri, Map<String, String> properties, DataFile dataFile) {
        ResponseOption responseOption = ResponseOption.find(uri);
        if (responseOption == null) {
            return;
        }

        boolean modified = false;

        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String prop = entry.getKey();
            String value = entry.getValue();

            if (prop.endsWith("hasContent") || prop.contains("hasContent")) {
                responseOption.setHasContent(value);
                modified = true;
            } else if (prop.endsWith("hasLanguage") || prop.contains("hasLanguage")) {
                responseOption.setHasLanguage(cleanLiteralValue(value));
                modified = true;
            } else if (prop.endsWith("hasStatus") || prop.contains("hasStatus")) {
                responseOption.setHasStatus(cleanLiteralValue(value));
                modified = true;
            } else if (prop.endsWith("hasLabel") || prop.contains("hasLabel")) {
                responseOption.setLabel(value);
                modified = true;
            }
        }

        if (modified) {
            responseOption.save();
            dataFile.getLogger().println("  Enriched ResponseOption: " + responseOption.getLabel());
        }
    }
    
    /**
     * Enriquece um AnnotationStem com propriedades do DA-SOC
     */
    private static void enrichAnnotationStem(String uri, Map<String, String> properties, DataFile dataFile) {
        AnnotationStem annotationStem = AnnotationStem.find(uri);
        if (annotationStem == null) {
            return;
        }

        boolean modified = false;

        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String prop = entry.getKey();
            String value = entry.getValue();

            if (prop.endsWith("hasContent") || prop.contains("hasContent")) {
                annotationStem.setHasContent(value);
                modified = true;
            } else if (prop.endsWith("hasLanguage") || prop.contains("hasLanguage")) {
                annotationStem.setHasLanguage(cleanLiteralValue(value));
                modified = true;
            } else if (prop.endsWith("hasVersion") || prop.contains("hasVersion")) {
                annotationStem.setHasVersion(cleanLiteralValue(value));
                modified = true;
            } else if (prop.endsWith("hasStatus") || prop.contains("hasStatus")) {
                annotationStem.setHasStatus(cleanLiteralValue(value));
                modified = true;
            }
        }

        if (modified) {
            annotationStem.save();
            dataFile.getLogger().println("  Enriched AnnotationStem: " + annotationStem.getLabel());
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
    private static String discoverStudyUri(DataFile dataFile, String daUri, String socUri) {
        try {
            // Strategy 0a: DA -> Study
            String studyFromDA = discoverStudyUriFromDA(dataFile, daUri);
            if (studyFromDA != null && !studyFromDA.isEmpty()) {
                return studyFromDA;
            }

            // Strategy 0a.1: DataFile -> hasco:hasStudy
            String studyFromDataFile = discoverStudyUriFromDataFileMetadata(dataFile);
            if (studyFromDataFile != null && !studyFromDataFile.isEmpty()) {
                return studyFromDataFile;
            }

            // Strategy 0b: SOC -> Study (explicit/inferred SOC URI)
            String studyFromSOC = discoverStudyUriFromSOC(dataFile, socUri);
            if (studyFromSOC != null && !studyFromSOC.isEmpty()) {
                return studyFromSOC;
            }

            // Strategy 1+: legacy/original approach from CSV originalIDs
            return discoverStudyUriFromOriginalIds(dataFile);
        } catch (Exception e) {
            dataFile.getLogger().printWarning("Error discovering Study URI: " + e.getMessage());
            System.out.println("[DASOC] Error discovering Study (combined strategy): " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static String discoverStudyUriFromDA(DataFile dataFile, String daUri) {
        if (daUri == null || daUri.isEmpty()) {
            return null;
        }

        try {
            String decodedDaUri = URLDecoder.decode(daUri, "UTF-8");
            String fullDaUri = URIUtils.replacePrefixEx(decodedDaUri);

            // Fast path: use DA POJO lookup first
            DA da = DA.find(fullDaUri);
            if (da != null && da.getIsMemberOfUri() != null && !da.getIsMemberOfUri().isEmpty()) {
                String studyUri = URIUtils.replacePrefixEx(da.getIsMemberOfUri());
                dataFile.getLogger().println("✅ Discovered Study URI from DA record: " + studyUri);
                System.out.println("[DASOC] ✅ Study discovered from DA: " + studyUri);
                return studyUri;
            }

            // Fallback: query triplestore directly (default + named graphs)
            String q = NameSpaces.getInstance().printSparqlNameSpaceList() +
                    "SELECT ?study WHERE { \n" +
                    "  { <" + fullDaUri + "> hasco:isMemberOf ?study . } \n" +
                    "  UNION \n" +
                    "  { GRAPH ?g { <" + fullDaUri + "> hasco:isMemberOf ?study . } } \n" +
                    "} LIMIT 1";

            org.apache.jena.query.ResultSet rs = SPARQLUtils.select(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), q);
            if (rs.hasNext()) {
                org.apache.jena.query.QuerySolution soln = rs.next();
                String studyUri = soln.get("study") != null ? soln.get("study").toString() : null;
                if (studyUri != null && !studyUri.isEmpty()) {
                    dataFile.getLogger().println("✅ Discovered Study URI from DA query: " + studyUri);
                    System.out.println("[DASOC] ✅ Study discovered from DA query: " + studyUri);
                    return studyUri;
                }
            }
        } catch (Exception e) {
            dataFile.getLogger().printWarning("DA-based Study discovery failed: " + e.getMessage());
            System.out.println("[DASOC] DA-based Study discovery failed: " + e.getMessage());
        }

        return null;
    }

    private static String discoverStudyUriFromDataFileMetadata(DataFile dataFile) {
        try {
            if (dataFile == null || dataFile.getUri() == null || dataFile.getUri().isEmpty()) {
                return null;
            }

            String dataFileUri = URIUtils.replacePrefixEx(dataFile.getUri());
            String q = NameSpaces.getInstance().printSparqlNameSpaceList() +
                    "SELECT ?study WHERE { \n" +
                    "  { <" + dataFileUri + "> hasco:hasStudy ?study . } \n" +
                    "  UNION \n" +
                    "  { GRAPH ?g { <" + dataFileUri + "> hasco:hasStudy ?study . } } \n" +
                    "} LIMIT 1";

            org.apache.jena.query.ResultSet rs = SPARQLUtils.select(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), q);
            if (rs.hasNext()) {
                org.apache.jena.query.QuerySolution soln = rs.next();
                String studyUri = soln.get("study") != null ? soln.get("study").toString() : null;
                if (studyUri != null && !studyUri.isEmpty()) {
                    dataFile.getLogger().println("✅ Discovered Study URI from DataFile metadata: " + studyUri);
                    System.out.println("[DASOC] ✅ Study discovered from DataFile metadata: " + studyUri);
                    return studyUri;
                }
            }
        } catch (Exception e) {
            dataFile.getLogger().printWarning("DataFile-metadata Study discovery failed: " + e.getMessage());
            System.out.println("[DASOC] DataFile-metadata Study discovery failed: " + e.getMessage());
        }
        return null;
    }

    private static String discoverStudyUriFromSOC(DataFile dataFile, String socUri) {
        java.util.LinkedHashSet<String> candidates = new java.util.LinkedHashSet<>();

        if (socUri != null && !socUri.isEmpty()) {
            candidates.add(socUri);
        }
        if (dataFile.getDasocSOCUri() != null && !dataFile.getDasocSOCUri().isEmpty()) {
            candidates.add(dataFile.getDasocSOCUri());
        }

        // Best-effort inference from filename: DA-SOC-{SOCNAME}.csv
        String filename = dataFile.getFilename();
        if (filename != null && filename.startsWith("DA-SOC-")) {
            String base = filename;
            int dot = filename.lastIndexOf('.');
            if (dot > 0) {
                base = filename.substring(0, dot);
            }
            String socName = base.substring("DA-SOC-".length());
            if (!socName.isEmpty()) {
                String detected = IngestionWorker.findSOCByName(socName);
                if (detected != null && !detected.isEmpty()) {
                    candidates.add(detected);
                }
                if (socName.contains("_")) {
                    String detectedHyphen = IngestionWorker.findSOCByName(socName.replace("_", "-"));
                    if (detectedHyphen != null && !detectedHyphen.isEmpty()) {
                        candidates.add(detectedHyphen);
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        for (String candidate : candidates) {
            try {
                String fullSocUri = URIUtils.replacePrefixEx(candidate);
                if (fullSocUri == null || fullSocUri.isEmpty()) {
                    continue;
                }

                // Direct SOC URI lookup
                String qDirect = NameSpaces.getInstance().printSparqlNameSpaceList() +
                        "SELECT ?study WHERE { \n" +
                        "  { <" + fullSocUri + "> hasco:isMemberOf ?study . } \n" +
                        "  UNION \n" +
                        "  { GRAPH ?g { <" + fullSocUri + "> hasco:isMemberOf ?study . } } \n" +
                        "} LIMIT 1";

                org.apache.jena.query.ResultSet rsDirect = SPARQLUtils.select(
                        CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), qDirect);
                if (rsDirect.hasNext()) {
                    org.apache.jena.query.QuerySolution soln = rsDirect.next();
                    String studyUri = soln.get("study") != null ? soln.get("study").toString() : null;
                    if (studyUri != null && !studyUri.isEmpty()) {
                        dataFile.getLogger().println("✅ Discovered Study URI from SOC: " + studyUri + " (SOC=" + fullSocUri + ")");
                        System.out.println("[DASOC] ✅ Study discovered from SOC " + fullSocUri + ": " + studyUri);
                        return studyUri;
                    }
                }

                // Tail-based fallback (when prefix/base differs but URI tail is stable)
                String tail = extractUriTail(fullSocUri);
                if (tail != null && !tail.isEmpty()) {
                    String qTail = NameSpaces.getInstance().printSparqlNameSpaceList() +
                            "SELECT ?soc ?study WHERE { \n" +
                            "  { ?soc hasco:isMemberOf ?study . } \n" +
                            "  UNION \n" +
                            "  { GRAPH ?g { ?soc hasco:isMemberOf ?study . } } \n" +
                            "  FILTER(STRENDS(STR(?soc), \"#" + tail + "\") || STRENDS(STR(?soc), \"/" + tail + "\")) \n" +
                            "} LIMIT 1";

                    org.apache.jena.query.ResultSet rsTail = SPARQLUtils.select(
                            CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), qTail);
                    if (rsTail.hasNext()) {
                        org.apache.jena.query.QuerySolution soln = rsTail.next();
                        String studyUri = soln.get("study") != null ? soln.get("study").toString() : null;
                        String resolvedSoc = soln.get("soc") != null ? soln.get("soc").toString() : null;
                        if (studyUri != null && !studyUri.isEmpty()) {
                            dataFile.getLogger().println("✅ Discovered Study URI by SOC tail: " + studyUri + " (SOC=" + resolvedSoc + ")");
                            System.out.println("[DASOC] ✅ Study discovered by SOC tail " + tail + ": " + studyUri + " (SOC=" + resolvedSoc + ")");
                            return studyUri;
                        }
                    }
                }
            } catch (Exception e) {
                dataFile.getLogger().printWarning("SOC-based Study discovery failed for candidate " + candidate + ": " + e.getMessage());
                System.out.println("[DASOC] SOC-based Study discovery failed for " + candidate + ": " + e.getMessage());
            }
        }

        return null;
    }

    private static String extractUriTail(String uri) {
        if (uri == null || uri.isEmpty()) {
            return "";
        }
        int hash = uri.lastIndexOf('#');
        int slash = uri.lastIndexOf('/');
        int idx = Math.max(hash, slash);
        if (idx >= 0 && idx + 1 < uri.length()) {
            return uri.substring(idx + 1);
        }
        return uri;
    }

    private static String discoverStudyUriFromOriginalIds(DataFile dataFile) {
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

    /**
     * Check if an object is a pure SIR object (without StudyObject layer)
     * Pure SIR objects are directly instances of vstoi:Instrument, vstoi:Component, etc.
     * and have hasco:originalID property
     */
    private static boolean isPureSIRObject(String objectUri, DataFile dataFile) {
        try {
            String ns = NameSpaces.getInstance().printSparqlNameSpaceList();
            String queryString = ns +
                    "SELECT ?type WHERE { \n" +
                    "  { \n" +
                    "    <" + objectUri + "> rdf:type ?type . \n" +
                    "    FILTER( \n" +
                    "      ?type = vstoi:Instrument || \n" +
                    "      ?type = vstoi:Component || \n" +
                    "      ?type = vstoi:ComponentStem || \n" +
                    "      ?type = vstoi:ContainerSlot || \n" +
                    "      ?type = vstoi:Codebook || \n" +
                    "      ?type = vstoi:ResponseOption || \n" +
                    "      ?type = vstoi:AnnotationStem \n" +
                    "    ) \n" +
                    "  } \n" +
                    "  UNION \n" +
                    "  { GRAPH ?g { \n" +
                    "      <" + objectUri + "> rdf:type ?type . \n" +
                    "      FILTER( \n" +
                    "        ?type = vstoi:Instrument || \n" +
                    "        ?type = vstoi:Component || \n" +
                    "        ?type = vstoi:ComponentStem || \n" +
                    "        ?type = vstoi:ContainerSlot || \n" +
                    "        ?type = vstoi:Codebook || \n" +
                    "        ?type = vstoi:ResponseOption || \n" +
                    "        ?type = vstoi:AnnotationStem \n" +
                    "      ) \n" +
                    "    } \n" +
                    "  } \n" +
                    "} LIMIT 1";

            org.apache.jena.query.ResultSet results = SPARQLUtils.select(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY),
                    queryString);

            boolean result = results.hasNext();

            if (result) {
                System.out.println("[DASOC] Detected pure SIR object: " + objectUri);
            }

            return result;

        } catch (Exception e) {
            dataFile.getLogger().printWarning("Error checking if object is pure SIR: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if a StudyObject has a VSTOI specialized instance
     * This checks for any of the VSTOI type properties:
     * - vstoi:hasInstrument → Instrument
     * - vstoi:hasComponentStem → ComponentStem
     * - vstoi:hasComponent → Component
     * - vstoi:hasResponseOption → ResponseOption
     * - vstoi:hasCodebook → Codebook
     * - vstoi:hasContainerSlot → ContainerSlot
     */
    private static boolean isVSTOIInstrument(String studyObjectUri, DataFile dataFile) {
        return getVSTOIInstanceUri(studyObjectUri, dataFile) != null;
    }

    /**
     * Get the VSTOI specialized instance URI for a StudyObject
     * This checks all VSTOI type properties and returns the first match found.
     * 
     * Supported VSTOI types:
     * - vstoi:hasInstrument → Instrument
     * - vstoi:hasComponentStem → ComponentStem
     * - vstoi:hasComponent → Component
     * - vstoi:hasResponseOption → ResponseOption
     * - vstoi:hasCodebook → Codebook
     * - vstoi:hasContainerSlot → ContainerSlot
     * - vstoi:hasDetector → Detector
     * - vstoi:hasPlatform → Platform
     */
    private static String getInstrumentInstanceUri(String studyObjectUri, DataFile dataFile) {
        return getVSTOIInstanceUri(studyObjectUri, dataFile);
    }

    /**
     * Generic method to get any VSTOI specialized instance URI for a StudyObject
     * Checks all known VSTOI linking properties in order
     */
    private static String getVSTOIInstanceUri(String studyObjectUri, DataFile dataFile) {
        // List of all VSTOI linking properties to check
        String[] vstoiProperties = {
            "vstoi:hasInstrument",
            "vstoi:hasComponentStem",
            "vstoi:hasComponent",
            "vstoi:hasResponseOption",
            "vstoi:hasCodebook",
            "vstoi:hasContainerSlot",
            "vstoi:hasDetector",
            "vstoi:hasPlatform"
        };

        try {
            String ns = NameSpaces.getInstance().printSparqlNameSpaceList();
            
            // Build a UNION query checking all VSTOI properties
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append(ns);
            queryBuilder.append("SELECT ?vstoiInstance ?property WHERE { \n");
            
            for (int i = 0; i < vstoiProperties.length; i++) {
                if (i > 0) {
                    queryBuilder.append("  UNION \n");
                }
                queryBuilder.append("  { \n");
                queryBuilder.append("    { \n");
                queryBuilder.append("      <").append(studyObjectUri).append("> ").append(vstoiProperties[i]).append(" ?vstoiInstance . \n");
                queryBuilder.append("      BIND(\"").append(vstoiProperties[i]).append("\" AS ?property) \n");
                queryBuilder.append("    } \n");
                queryBuilder.append("    UNION \n");
                queryBuilder.append("    { GRAPH ?g { \n");
                queryBuilder.append("        <").append(studyObjectUri).append("> ").append(vstoiProperties[i]).append(" ?vstoiInstance . \n");
                queryBuilder.append("        BIND(\"").append(vstoiProperties[i]).append("\" AS ?property) \n");
                queryBuilder.append("      } \n");
                queryBuilder.append("    } \n");
                queryBuilder.append("  } \n");
            }
            
            queryBuilder.append("} LIMIT 1");

            org.apache.jena.query.ResultSet results = SPARQLUtils.select(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY),
                    queryBuilder.toString());

            if (results.hasNext()) {
                org.apache.jena.query.QuerySolution soln = results.next();
                if (soln.get("vstoiInstance") != null) {
                    String vstoiInstanceUri = soln.get("vstoiInstance").toString();
                    String propertyUsed = soln.get("property") != null ? soln.get("property").toString() : "unknown";
                    
                    // Log what type of VSTOI instance was found
                    String vstoiType = propertyUsed.replace("vstoi:has", "");
                    dataFile.getLogger().println(String.format(
                        "  [VSTOI-%s] Found specialized instance for StudyObject", vstoiType));
                    
                    return vstoiInstanceUri;
                }
            }

        } catch (Exception e) {
            dataFile.getLogger().printWarning("Error getting VSTOI instance URI: " + e.getMessage());
        }

        return null;
    }
}
