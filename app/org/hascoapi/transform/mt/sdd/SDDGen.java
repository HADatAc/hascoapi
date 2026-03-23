package org.hascoapi.transform.mt.sdd;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.hascoapi.entity.pojo.*;
import org.hascoapi.utils.ConfigProp;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * SDDGen generates SDD (Semantic Data Dictionary) Excel templates.
 *
 * The SDD is the mechanism by which raw data columns are transformed into semantically meaningful,
 * machine-readable concepts. It ensures that the meaning of every data point is explicitly defined
 * and linked to controlled vocabularies (ontologies).
 *
 * The SDD defines:
 * - Variables: The columns or fields in the raw data file (e.g., Timestamp, Temperature).
 * - Semantic Annotation: The ontological terms (URIs) that define the meaning of each variable.
 * - Unit of Measure: The unit associated with the variable's value (e.g., unit:DEG_C).
 * - Contextual Entities: Defines the entities that contextualize the data.
 */
public class SDDGen {

    public static final String INFOSHEET            = "InfoSheet";
    public static final String NAMESPACES           = "Namespaces";
    public static final String DICTIONARY_MAPPING   = "Dictionary Mapping";
    public static final String CODEBOOK             = "Codebook";
    public static final String TIMELINE             = "Timeline";

    public static final int PAGESIZE                = 20000;
    public static final int OFFSET                  = 0;

    /**
     * Generate an SDD template by status.
     *
     * @param dataFileUri The DataFile URI for which to generate the SDD (ALSO used to EXCLUDE the just-created SDD)
     * @param status The status filter (e.g., "Draft", "Processed")
     * @param filename Output filename
     * @param mediaFolder Media folder path
     * @param excludeDataFileUri The DataFile URI to exclude (typically the one being generated)
     * @return Absolute path to the generated file, or null on error
     */
    public static String genByStatus(String dataFileUri, String status, String filename, String mediaFolder, String excludeDataFileUri) {
        System.out.println("\n========== SDDGen.genByStatus() START ==========");
        System.out.println("Input parameters:");
        System.out.println("  dataFileUri: " + dataFileUri);
        System.out.println("  status: " + status);
        System.out.println("  filename: " + filename);
        System.out.println("  mediaFolder: " + mediaFolder);
        if (excludeDataFileUri != null && !excludeDataFileUri.trim().isEmpty()) {
            System.out.println("  excludeDataFileUri: " + excludeDataFileUri);
        }

        SDDGenHelper helper = new SDDGenHelper();

        try {
            // Get base path from configuration (same convention as other generators)
            String basePath = ConfigProp.getPathIngestion();
            if (basePath == null || basePath.isEmpty()) {
                throw new IllegalStateException("ConfigProp.getPathIngestion() returned empty.");
            }

            // Normalize path separators
            basePath = basePath.replace("/", java.io.File.separator)
                    .replace("\\\\", java.io.File.separator);

            // Ensure trailing separator
            if (!basePath.endsWith(java.io.File.separator)) {
                basePath += java.io.File.separator;
            }

            String outFilename = basePath + filename;
            java.io.File outFile = new java.io.File(outFilename);
            String absolutePath = outFile.getAbsolutePath();

            System.out.println("  basePath (from ConfigProp): " + basePath);
            System.out.println("  outFilename (final): " + outFilename);
            System.out.println("  outFilename (absolute): " + absolutePath);


            System.out.println("[SDDGen] Searching for existing SDDs with status=" + status);

            // Normalize the excludeDataFileUri for comparison
            String normalizedExcludeDataFileUri = null;
            if (excludeDataFileUri != null && !excludeDataFileUri.trim().isEmpty()) {
                normalizedExcludeDataFileUri = excludeDataFileUri.trim().toLowerCase();
                System.out.println("[SDDGen] Will exclude SDD with DataFile URI: " + excludeDataFileUri);
            }

            // Normalize status for comparison
            // Default to DRAFT if not provided
            String normalizedRequestedStatus = "DRAFT";
            if (status != null && !status.trim().isEmpty()) {
                normalizedRequestedStatus = status.trim().toUpperCase();
                // Extract just the status part after the last # or /
                if (normalizedRequestedStatus.contains("#")) {
                    normalizedRequestedStatus = normalizedRequestedStatus.substring(normalizedRequestedStatus.lastIndexOf("#") + 1);
                } else if (normalizedRequestedStatus.contains("/")) {
                    normalizedRequestedStatus = normalizedRequestedStatus.substring(normalizedRequestedStatus.lastIndexOf("/") + 1);
                }
            }

            // Find all SDDs
            String queryAllSdds = org.hascoapi.utils.NameSpaces.getInstance().printSparqlNameSpaceList()
                    + " SELECT ?uri WHERE { "
                    + "   ?sddType rdfs:subClassOf* hasco:SDD . "
                    + "   ?uri a ?sddType . "
                    + " }";

            List<SDD> allSdds = org.hascoapi.entity.pojo.GenericFind.findByQuery(SDD.class, queryAllSdds);

            System.out.println("[SDDGen] Found " + (allSdds == null ? 0 : allSdds.size()) + " total SDDs");

            SDD targetSdd = null;

            if (allSdds != null) {
                for (SDD candidate : allSdds) {
                    if (candidate == null) continue;

                    String candidateDataFileUri = candidate.getHasDataFileUri();
                    if (candidateDataFileUri == null) continue;

                    String normalizedCandidateUri = candidateDataFileUri.trim().toLowerCase();

                    // CRITICAL FIX: Exclude the SDD that matches the DataFile URI being generated
                    if (normalizedExcludeDataFileUri != null) {
                        if (normalizedCandidateUri.equals(normalizedExcludeDataFileUri)) {
                            System.out.println("  [SDDGen] SDD diag: uri=" + candidate.getUri()
                                    + ", label=" + candidate.getLabel()
                                    + " -> EXCLUDED (matches excludeDataFileUri)");
                            continue;
                        }
                    }

                    // Check status match
                    // For SDD, if status is null or empty, treat it as DRAFT (default status)
                    String candidateStatus = candidate.getHasStatus();
                    String effectiveStatus = "DRAFT"; // Default when null or empty
                    if (candidateStatus != null && !candidateStatus.trim().isEmpty()) {
                        effectiveStatus = candidateStatus.trim().toUpperCase();
                        if (effectiveStatus.contains("#")) {
                            effectiveStatus = effectiveStatus.substring(effectiveStatus.lastIndexOf("#") + 1);
                        } else if (effectiveStatus.contains("/")) {
                            effectiveStatus = effectiveStatus.substring(effectiveStatus.lastIndexOf("/") + 1);
                        }
                    }

                    // Accept DRAFT, CURRENT, or the requested status
                    // Always treat null/empty as DRAFT
                    boolean statusMatches = effectiveStatus.equals("DRAFT") ||
                                           effectiveStatus.equals("CURRENT") ||
                                           effectiveStatus.equals(normalizedRequestedStatus);

                    if (!statusMatches) {
                        System.out.println("  [SDDGen] SDD diag: uri=" + candidate.getUri()
                                + ", label=" + candidate.getLabel()
                                + " -> skipped (status mismatch: " + effectiveStatus + " vs " + normalizedRequestedStatus + ")");
                        continue;
                    }

                    // Check if the SDD has actual content (SDDAttributes or SDDObjects)
                    // This is the real test of a complete SDD
                    // An SDD without attributes/objects is NOT a valid source for generation
                    try {
                        int entityCount = countSDDAttributes(candidate);

                        if (entityCount <= 0) {
                            System.out.println("  [SDDGen] SDD diag: uri=" + candidate.getUri()
                                    + ", label=" + candidate.getLabel()
                                    + " -> skipped (has " + entityCount + " entities, not a valid source)");
                            continue;
                        } else {
                            System.out.println("  [SDDGen] SDD diag: uri=" + candidate.getUri()
                                    + ", label=" + candidate.getLabel()
                                    + ", status=" + effectiveStatus
                                    + " -> SELECTED (has " + entityCount + " entities: attributes + objects)");
                        }
                    } catch (Exception ex) {
                        System.out.println("  [SDDGen] SDD diag: uri=" + candidate.getUri()
                                + ", label=" + candidate.getLabel()
                                + " -> skipped (error checking entities: " + ex.getMessage() + ")");
                        continue;
                    }

                    targetSdd = candidate;
                    break;
                }
            }

            if (targetSdd == null) {
                System.out.println("[SDDGen] WARNING: No SDD found for DataFile " + dataFileUri);
                System.out.println("[SDDGen] Generating empty SDD template.");
            } else {
                System.out.println("[SDDGen] Using SDD: " + targetSdd.getUri());
                System.out.println("[SDDGen] SDD Label: " + targetSdd.getLabel());
                System.out.println("[SDDGen] SDD uriId (getIdLabel): '" + targetSdd.getIdLabel() + "'");
                System.out.println("[SDDGen] SDD hasDataFileUri: " + targetSdd.getHasDataFileUri());
                System.out.println("[SDDGen] DEBUG: Calling SDD.find() again to verify field population...");
                SDD debugSdd = SDD.find(targetSdd.getUri());
                if (debugSdd != null) {
                    System.out.println("[SDDGen] DEBUG after SDD.find():");
                    System.out.println("[SDDGen]   Label: " + debugSdd.getLabel());
                    System.out.println("[SDDGen]   IdLabel: '" + debugSdd.getIdLabel() + "'");
                    System.out.println("[SDDGen]   HasDataFileUri: " + debugSdd.getHasDataFileUri());
                }
            }

            // Create workbook
            // Priority: 1) SDD's uriId (idLabel), 2) SDD's label, 3) extract from filename
            String sddId = null;
            if (targetSdd != null) {
                // Try uriId first
                if (targetSdd.getIdLabel() != null && !targetSdd.getIdLabel().trim().isEmpty()) {
                    sddId = targetSdd.getIdLabel().trim();
                    System.out.println("[SDDGen] Using SDD uriId: " + sddId);
                }
                // Fall back to label
                else if (targetSdd.getLabel() != null && !targetSdd.getLabel().trim().isEmpty()) {
                    sddId = targetSdd.getLabel().trim();
                    System.out.println("[SDDGen] Using SDD label as ID: " + sddId);
                }
            }

            // Last resort: extract from filename
            if (sddId == null || sddId.isEmpty()) {
                sddId = extractSddId(filename);
                System.out.println("[SDDGen] Using extracted ID from filename: " + sddId);
            }

            helper.workbook = create(sddId);

            // Save namespaces
            saveNamespaces(helper);

            // If we found an SDD, populate data
            if (targetSdd != null) {
                try {
                    addDictionaryMappingData(helper, targetSdd);
                } catch (Exception e) {
                    System.out.println("[SDDGen] ERROR while adding Dictionary Mapping data:");
                    System.out.println("[SDDGen]   " + e.getClass().getName() + ": " + e.getMessage());
                    System.out.println("[SDDGen]   This may be due to malformed URIs in the triplestore.");
                    System.out.println("[SDDGen]   Continuing with partial data...");
                }
                try {
                    addCodebookData(helper, targetSdd);
                } catch (Exception e) {
                    System.out.println("[SDDGen] ERROR while adding Codebook data:");
                    System.out.println("[SDDGen]   " + e.getClass().getName() + ": " + e.getMessage());
                    System.out.println("[SDDGen]   Continuing with partial data...");
                }
                try {
                    addTimelineData(helper, targetSdd);
                } catch (Exception e) {
                    System.out.println("[SDDGen] ERROR while adding Timeline data:");
                    System.out.println("[SDDGen]   " + e.getClass().getName() + ": " + e.getMessage());
                    System.out.println("[SDDGen]   Continuing with partial data...");
                }
            }

            // Save workbook
            String result = save(helper, absolutePath);
            System.out.println("  Save result: " + result);

            System.out.println("========== SDDGen.genByStatus() END ==========\n");
            return result;

        } catch (Exception e) {
            System.out.println("  ❌ EXCEPTION during generation:");
            System.out.println("     Exception type: " + e.getClass().getName());
            System.out.println("     Message: " + e.getMessage());
            e.printStackTrace();
            System.out.println("========== SDDGen.genByStatus() END (EXCEPTION) ==========\n");
            return null;
        }
    }

    /**
     * Create the basic workbook structure for SDD.
     */
    public static Workbook create(String sddId) {
        Workbook workbook = new XSSFWorkbook();

        // Create InfoSheet
        Sheet infoSheet = workbook.createSheet(INFOSHEET);

        // InfoSheet headers - with extra empty columns to match original format
        Row isHeaderRow = infoSheet.createRow(0);
        isHeaderRow.createCell(0).setCellValue("Attribute");
        isHeaderRow.createCell(1).setCellValue("Value");
        // Add empty cells for columns 2-7 to match original format
        for (int i = 2; i <= 7; i++) {
            isHeaderRow.createCell(i).setCellValue("");
        }

        // InfoSheet data rows
        int rowNum = 1;

        Row row1 = infoSheet.createRow(rowNum++);
        row1.createCell(0).setCellValue("SDD_ID");
        row1.createCell(1).setCellValue(sddId != null ? sddId : "SDD-UNKNOWN");

        Row row2 = infoSheet.createRow(rowNum++);
        row2.createCell(0).setCellValue("hasDependencies");
        row2.createCell(1).setCellValue("#" + NAMESPACES);

        Row row3 = infoSheet.createRow(rowNum++);
        row3.createCell(0).setCellValue("Data_Dictionary");
        row3.createCell(1).setCellValue("#" + DICTIONARY_MAPPING);

        Row row4 = infoSheet.createRow(rowNum++);
        row4.createCell(0).setCellValue("Codebook");
        row4.createCell(1).setCellValue("#" + CODEBOOK);

        Row row5 = infoSheet.createRow(rowNum++);
        row5.createCell(0).setCellValue("Code_Mappings");

        Row row6 = infoSheet.createRow(rowNum++);
        row6.createCell(0).setCellValue("Imports");

        Row row7 = infoSheet.createRow(rowNum++);
        row7.createCell(0).setCellValue("Timeline");
        row7.createCell(1).setCellValue("#" + TIMELINE);

        Row row8 = infoSheet.createRow(rowNum++);
        row8.createCell(0).setCellValue("Version");
        row8.createCell(1).setCellValue("1"); // String value to match original

        // Add one empty row after Version
        infoSheet.createRow(rowNum++);

        // Create other sheets
        workbook.createSheet(NAMESPACES);
        workbook.createSheet(DICTIONARY_MAPPING);
        workbook.createSheet(CODEBOOK);
        workbook.createSheet(TIMELINE);

        // Set headers for Dictionary Mapping
        SDDDictionaryMapping.setHeaders(workbook.getSheet(DICTIONARY_MAPPING));

        // Set headers for Codebook
        SDDCodebook.setHeaders(workbook.getSheet(CODEBOOK));

        // Set headers for Timeline
        SDDTimeline.setHeaders(workbook.getSheet(TIMELINE));

        return workbook;
    }

    /**
     * Extract SDD ID from filename.
     */
    private static String extractSddId(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "SDD-" + System.currentTimeMillis();
        }

        String baseName = filename;
        if (baseName.toLowerCase().endsWith(".xlsx")) {
            baseName = baseName.substring(0, baseName.length() - 5);
        }

        // Remove "SDD-" prefix if present
        if (baseName.toUpperCase().startsWith("SDD-")) {
            return baseName.substring(4);
        }

        return baseName;
    }

    /**
     * Add Dictionary Mapping data from the triple store.
     */
    private static void addDictionaryMappingData(SDDGenHelper helper, SDD sdd) {
        System.out.println("[SDDGen] Adding Dictionary Mapping data...");

        // Query for SDD Attributes associated with this SDD
        // CRITICAL: SDDAttributes are stored with partOfSchema = SDDICT..., not SDD...
        // The SDDICT URI is created from the DataFile URI, not the SDD URI
        // We need to get the DataFile URI from the SDD and then create the SDDICT URI
        String sddDataFileUri = sdd.getHasDataFileUri();

        if (sddDataFileUri == null || sddDataFileUri.isEmpty()) {
            System.out.println("[SDDGen] ERROR: SDD has no DataFile URI, cannot query attributes");
            return;
        }

        // The schema URI (SDDICT) is created by replacing DFL with SDDICT in the DataFile URI
        String schemaUri = sddDataFileUri.replace("DFL", "SDDICT");

        System.out.println("[SDDGen] SDD URI: " + sdd.getUri());
        System.out.println("[SDDGen] DataFile URI: " + sddDataFileUri);
        System.out.println("[SDDGen] Schema URI (SDDICT) for query: " + schemaUri);

        // Build SPARQL query to get all SDD attributes
        String query = org.hascoapi.utils.NameSpaces.getInstance().printSparqlNameSpaceList()
                + " SELECT DISTINCT ?uri WHERE { "
                + "   ?uri a hasco:SDDAttribute . "
                + "   ?uri hasco:partOfSchema <" + schemaUri + "> . "
                + " } ORDER BY ?uri LIMIT " + PAGESIZE + " OFFSET " + OFFSET;

        System.out.println("[SDDGen] Dictionary Mapping query: " + query);

        List<SDDAttribute> attributes = org.hascoapi.entity.pojo.GenericFind.findByQuery(
            SDDAttribute.class, query);

        System.out.println("[SDDGen] Found " + (attributes == null ? 0 : attributes.size()) + " attributes");

        // DEBUG: Let's check what's actually in the triplestore
        String debugQuery1 = org.hascoapi.utils.NameSpaces.getInstance().printSparqlNameSpaceList()
                + " SELECT DISTINCT ?uri ?label ?schema WHERE { "
                + "   ?uri a hasco:SDDAttribute . "
                + "   OPTIONAL { ?uri rdfs:label ?label } "
                + "   OPTIONAL { ?uri hasco:partOfSchema ?schema } "
                + " } LIMIT 10";
        System.out.println("[SDDGen] DEBUG: Checking for ANY SDDAttributes in system with their schema URIs:");
        List<SDDAttribute> debugAttrs = org.hascoapi.entity.pojo.GenericFind.findByQuery(
            SDDAttribute.class, debugQuery1);
        System.out.println("[SDDGen] DEBUG: Found " + (debugAttrs == null ? 0 : debugAttrs.size()) + " total SDDAttributes in system");
        if (debugAttrs != null && !debugAttrs.isEmpty()) {
            for (int i = 0; i < Math.min(10, debugAttrs.size()); i++) {
                SDDAttribute da = debugAttrs.get(i);
                System.out.println("[SDDGen] DEBUG:   SDDAttribute #" + (i+1) + ": uri=" + da.getUri() + ", label=" + da.getLabel() + ", partOfSchema=" + da.getPartOfSchema());
            }
        }

        // DEBUG: Check the specific schema we're querying for
        String debugQuery2 = org.hascoapi.utils.NameSpaces.getInstance().printSparqlNameSpaceList()
                + " SELECT (COUNT(?uri) AS ?count) WHERE { "
                + "   ?uri a hasco:SDDAttribute . "
                + "   ?uri hasco:partOfSchema <" + schemaUri + "> . "
                + " }";
        System.out.println("[SDDGen] DEBUG: Counting SDDAttributes for schema " + schemaUri + ":");
        System.out.println("[SDDGen] DEBUG Query: " + debugQuery2);

        if (attributes != null && !attributes.isEmpty()) {
            Sheet sheet = helper.workbook.getSheet(DICTIONARY_MAPPING);
            for (SDDAttribute attr : attributes) {
                if (attr != null) {
                    SDDDictionaryMapping.add(sheet, helper, attr);
                }
            }
        }

        // Also query for SDDObjects (like ??weather, ??observation, ??instant)
        // These also appear in the Dictionary Mapping sheet
        String objectQuery = org.hascoapi.utils.NameSpaces.getInstance().printSparqlNameSpaceList()
                + " SELECT DISTINCT ?uri WHERE { "
                + "   ?uri a hasco:SDDObject . "
                + "   ?uri hasco:partOfSchema <" + schemaUri + "> . "
                + " } ORDER BY ?uri LIMIT " + PAGESIZE + " OFFSET " + OFFSET;

        System.out.println("[SDDGen] Dictionary Mapping SDDObject query: " + objectQuery);

        List<org.hascoapi.entity.pojo.SDDObject> objects = org.hascoapi.entity.pojo.GenericFind.findByQuery(
            org.hascoapi.entity.pojo.SDDObject.class, objectQuery);

        System.out.println("[SDDGen] Found " + (objects == null ? 0 : objects.size()) + " SDDObjects");

        if (objects != null && !objects.isEmpty()) {
            System.out.println("[SDDGen] Adding " + objects.size() + " SDDObjects to Dictionary Mapping sheet...");
            Sheet sheet = helper.workbook.getSheet(DICTIONARY_MAPPING);
            int objIndex = 0;
            for (org.hascoapi.entity.pojo.SDDObject obj : objects) {
                objIndex++;
                if (obj != null) {
                    System.out.println("[SDDGen] Processing SDDObject #" + objIndex + ": " + obj.getUri());
                    System.out.println("[SDDGen]   Label: " + obj.getLabel());
                    System.out.println("[SDDGen]   Entity: " + obj.getEntity());
                    System.out.println("[SDDGen]   Role: " + obj.getRole());
                    System.out.println("[SDDGen]   Relation: " + obj.getRelation());
                    System.out.println("[SDDGen]   InRelationTo: " + obj.getInRelationTo());
                    System.out.println("[SDDGen]   ListPosition: " + obj.getListPosition());
                    // Add SDDObject to Dictionary Mapping sheet
                    // SDDObjects appear as rows with NO Attribute column, only Entity column
                    SDDDictionaryMapping.addObject(sheet, helper, obj);
                    System.out.println("[SDDGen] SDDObject #" + objIndex + " added successfully");
                } else {
                    System.out.println("[SDDGen] WARNING: SDDObject #" + objIndex + " is null, skipping");
                }
            }
        } else {
            System.out.println("[SDDGen] No SDDObjects to add (list is null or empty)");
        }
    }

    /**
     * Add Codebook data from the triple store.
     */
    private static void addCodebookData(SDDGenHelper helper, SDD sdd) {
        System.out.println("[SDDGen] Adding Codebook data...");

        // Query for PossibleValues associated with this SDD
        // CRITICAL: PossibleValues are linked to attributes which have partOfSchema = SDDICT...
        // The SDDICT URI is created from the DataFile URI
        String sddDataFileUri = sdd.getHasDataFileUri();

        if (sddDataFileUri == null || sddDataFileUri.isEmpty()) {
            System.out.println("[SDDGen] ERROR: SDD has no DataFile URI, cannot query possible values");
            return;
        }

        String schemaUri = sddDataFileUri.replace("DFL", "SDDICT");

        // DEBUG: First check if there are ANY PossibleValues in the system
        String debugQuery = org.hascoapi.utils.NameSpaces.getInstance().printSparqlNameSpaceList()
                + " SELECT DISTINCT ?uri ?attr ?schema WHERE { "
                + "   ?uri a hasco:PossibleValue . "
                + "   OPTIONAL { ?uri hasco:isPossibleValueOf ?attr } "
                + "   OPTIONAL { ?attr hasco:partOfSchema ?schema } "
                + " } LIMIT 20";

        System.out.println("[SDDGen] DEBUG: Checking for ANY PossibleValues in system:");
        System.out.println("[SDDGen] DEBUG Query: " + debugQuery);

        org.apache.jena.query.ResultSetRewindable debugRs = org.hascoapi.utils.SPARQLUtils.select(
                org.hascoapi.utils.CollectionUtil.getCollectionPath(
                        org.hascoapi.utils.CollectionUtil.Collection.SPARQL_QUERY), debugQuery);

        int debugCount = 0;
        if (debugRs != null) {
            while (debugRs.hasNext()) {
                debugCount++;
                org.apache.jena.query.QuerySolution soln = debugRs.next();
                String pvUri = soln.get("uri") != null ? soln.get("uri").toString() : "null";
                String attrUri = soln.get("attr") != null ? soln.get("attr").toString() : "null";
                String schema = soln.get("schema") != null ? soln.get("schema").toString() : "null";
                System.out.println("[SDDGen] DEBUG:   PossibleValue #" + debugCount + ": uri=" + pvUri + ", attr=" + attrUri + ", schema=" + schema);
            }
        }
        System.out.println("[SDDGen] DEBUG: Found " + debugCount + " total PossibleValues in system");

        // Now do the actual query
        String query = org.hascoapi.utils.NameSpaces.getInstance().printSparqlNameSpaceList()
                + " SELECT DISTINCT ?uri WHERE { "
                + "   ?uri a hasco:PossibleValue . "
                + "   ?uri hasco:isPossibleValueOf ?attr . "
                + "   ?attr hasco:partOfSchema <" + schemaUri + "> . "
                + " } ORDER BY ?uri LIMIT " + PAGESIZE + " OFFSET " + OFFSET;

        System.out.println("[SDDGen] Codebook query: " + query);

        List<PossibleValue> possibleValues = org.hascoapi.entity.pojo.GenericFind.findByQuery(
            PossibleValue.class, query);

        System.out.println("[SDDGen] Found " + (possibleValues == null ? 0 : possibleValues.size()) +
            " possible values");

        if (possibleValues != null && !possibleValues.isEmpty()) {
            System.out.println("[SDDGen] Adding " + possibleValues.size() + " PossibleValues to Codebook sheet...");
            Sheet sheet = helper.workbook.getSheet(CODEBOOK);
            int pvCount = 0;
            for (PossibleValue pv : possibleValues) {
                if (pv != null) {
                    pvCount++;
                    System.out.println("[SDDGen] Processing PossibleValue #" + pvCount + ": " + pv.getUri());
                    System.out.println("[SDDGen]   Variable: " + pv.getHasVariable());
                    System.out.println("[SDDGen]   Code: " + pv.getHasCode());
                    System.out.println("[SDDGen]   CodeLabel: " + pv.getHasCodeLabel());
                    System.out.println("[SDDGen]   Class: " + pv.getHasClass());
                    System.out.println("[SDDGen]   IsPossibleValueOf: " + pv.getIsPossibleValueOf());
                    SDDCodebook.add(sheet, helper, pv);
                    System.out.println("[SDDGen] PossibleValue #" + pvCount + " added successfully");
                }
            }
        } else {
            System.out.println("[SDDGen] No PossibleValues found - Codebook sheet will remain empty (only headers)");
        }
    }

    /**
     * Add Timeline data from the triple store.
     *
     * The Timeline sheet is for EXPLICIT timeline definitions only.
     * Virtual objects (??instant, ??observation, etc.) are handled in Dictionary Mapping, NOT Timeline.
     *
     * Timeline objects must have hasco:hasRole hasco:TimeRole to be included here.
     * This is a rare case - most SDDs have an empty Timeline sheet.
     */
    private static void addTimelineData(SDDGenHelper helper, SDD sdd) {
        System.out.println("[SDDGen] Adding Timeline data...");

        // CRITICAL: SDDObjects are stored with partOfSchema = SDDICT..., not SDD...
        String sddDataFileUri = sdd.getHasDataFileUri();

        if (sddDataFileUri == null || sddDataFileUri.isEmpty()) {
            System.out.println("[SDDGen] ERROR: SDD has no DataFile URI, cannot query timeline objects");
            return;
        }

        String schemaUri = sddDataFileUri.replace("DFL", "SDDICT");

        // Query ONLY for SDDObjects that explicitly have hasco:hasRole hasco:TimeRole
        // Virtual objects (??instant, ??observation, etc.) belong in Dictionary Mapping, NOT Timeline
        // The Timeline sheet is typically empty in most SDDs
        String query = org.hascoapi.utils.NameSpaces.getInstance().printSparqlNameSpaceList()
                + " SELECT DISTINCT ?uri WHERE { "
                + "   ?uri a hasco:SDDObject . "
                + "   ?uri hasco:partOfSchema <" + schemaUri + "> . "
                + "   ?uri hasco:hasRole hasco:TimeRole . "
                + " } ORDER BY ?uri LIMIT " + PAGESIZE + " OFFSET " + OFFSET;

        System.out.println("[SDDGen] Timeline query: " + query);

        List<SDDObject> timelineObjects = org.hascoapi.entity.pojo.GenericFind.findByQuery(
            SDDObject.class, query);

        System.out.println("[SDDGen] Found " + (timelineObjects == null ? 0 : timelineObjects.size()) +
            " timeline objects (with hasco:TimeRole)");

        if (timelineObjects != null && !timelineObjects.isEmpty()) {
            System.out.println("[SDDGen] Adding " + timelineObjects.size() + " timeline objects to Timeline sheet...");
            Sheet sheet = helper.workbook.getSheet(TIMELINE);
            int objCount = 0;
            for (SDDObject obj : timelineObjects) {
                if (obj != null) {
                    objCount++;
                    System.out.println("[SDDGen] Processing Timeline object #" + objCount + ": " + obj.getUri());
                    System.out.println("[SDDGen]   Label: " + obj.getLabel());
                    System.out.println("[SDDGen]   Comment: " + obj.getComment());
                    System.out.println("[SDDGen]   Entity: " + obj.getEntity());
                    System.out.println("[SDDGen]   Role: " + obj.getRole());
                    System.out.println("[SDDGen]   InRelationTo: " + obj.getInRelationTo());
                    System.out.println("[SDDGen]   HasStart: " + obj.getHasStart());
                    System.out.println("[SDDGen]   HasEnd: " + obj.getHasEnd());
                    System.out.println("[SDDGen]   HasUnit: " + obj.getHasUnit());
                    SDDTimeline.add(sheet, helper, obj);
                    System.out.println("[SDDGen] Timeline object #" + objCount + " added successfully");
                }
            }
        } else {
            System.out.println("[SDDGen] No timeline objects with hasco:TimeRole found - Timeline sheet will be empty (only headers)");
            System.out.println("[SDDGen] NOTE: This is CORRECT and expected for most SDDs.");
            System.out.println("[SDDGen] The original SDD-WS template also has an empty Timeline sheet.");
            System.out.println("[SDDGen] Virtual objects (??instant, ??observation, etc.) belong in Dictionary Mapping, NOT Timeline.");
        }
    }

    /**
     * Save namespaces to the Namespaces sheet.
     */
    public static void saveNamespaces(SDDGenHelper helper) {
        if (helper == null || helper.workbook == null) {
            System.out.println("✅ helper and workbook are valid");
            return;
        }

        System.out.println("✅ Saving namespaces...");

        Sheet namespacesSheet = helper.workbook.getSheet(NAMESPACES);

        // Namespaces sheet schema - with header column named exactly as in original
        Row row = namespacesSheet.createRow(0);
        row.createCell(0).setCellValue("hasPrefix");
        row.createCell(1).setCellValue("hasNameSpace");
        row.createCell(2).setCellValue("hasFormat");
        row.createCell(3).setCellValue("hasSource");

        // Required prefixes for SDD - matching the original file EXACTLY
        int rowIndex = 1;
        // Note: Empty strings for format/source will create empty cells (not cells with "")
        rowIndex = writeNamespaceRow(namespacesSheet, rowIndex, "hasco", "http://hadatac.org/ont/hasco#", "", "");
        rowIndex = writeNamespaceRow(namespacesSheet, rowIndex, "envo", "http://purl.obolibrary.org/obo/ENVO_", "application/rdf+xml", "http://purl.obolibrary.org/obo/envo.owl");
        rowIndex = writeNamespaceRow(namespacesSheet, rowIndex, "pato", "http://purl.obolibrary.org/obo/PATO_", "application/rdf+xml", "http://purl.obolibrary.org/obo/pato.owl");
        rowIndex = writeNamespaceRow(namespacesSheet, rowIndex, "unit", "http://qudt.org/vocab/unit/", "text/turtle", "http://qudt.org/vocab/unit/");
        rowIndex = writeNamespaceRow(namespacesSheet, rowIndex, "time", "http://www.w3.org/2006/time#", "", "");
        rowIndex = writeNamespaceRow(namespacesSheet, rowIndex, "sio", "http://semanticscience.org/resource/", "", "");
        rowIndex = writeNamespaceRow(namespacesSheet, rowIndex, "ncit", "http://purl.obolibrary.org/obo/NCIT_", "application/rdf+xml", "http://purl.obolibrary.org/obo/ncit.owl");
        rowIndex = writeNamespaceRow(namespacesSheet, rowIndex, "obo", "http://purl.obolibrary.org/obo/", "", "");

        // Add collected namespaces (avoid duplicates)
        java.util.Set<String> seenPrefixes = new java.util.HashSet<>();
        seenPrefixes.add("hasco");
        seenPrefixes.add("envo");
        seenPrefixes.add("pato");
        seenPrefixes.add("unit");
        seenPrefixes.add("time");
        seenPrefixes.add("sio");
        seenPrefixes.add("ncit");
        seenPrefixes.add("obo");
        // Don't automatically exclude these - only add if explicitly used
        // seenPrefixes.add("vstoi");

        for (NameSpace namespace : helper.namespaces.values()) {
            if (namespace == null) continue;
            String prefix = namespace.getLabel();
            String nsUri = namespace.getUri();
            if (prefix == null) continue;
            if (seenPrefixes.contains(prefix)) continue;
            seenPrefixes.add(prefix);

            rowIndex = writeNamespaceRow(namespacesSheet, rowIndex, prefix, nsUri, "text/turtle", nsUri);
        }

        System.out.println("✅ Namespaces saved");
    }

    private static int writeNamespaceRow(Sheet sheet, int rowIndex, String prefix, String nsUri,
                                        String format, String source) {
        Row newRow = sheet.createRow(rowIndex);
        newRow.createCell(0).setCellValue(prefix == null ? "" : prefix);
        newRow.createCell(1).setCellValue(nsUri == null ? "" : nsUri);
        if (format != null && !format.isEmpty()) {
            newRow.createCell(2).setCellValue(format);
        }
        if (source != null && !source.isEmpty()) {
            newRow.createCell(3).setCellValue(source);
        }
        return rowIndex + 1;
    }

    /**
     * Count the number of SDDAttributes and SDDObjects for an SDD.
     * This is the definitive test of whether an SDD has been fully ingested and populated.
     * An SDD without attributes/objects is just metadata and not suitable for use as a template source.
     *
     * CRITICAL: An SDD can have either SDDAttributes (for data variables) or SDDObjects (for contextual entities).
     * We need to count BOTH to determine if the SDD is valid.
     */
    private static int countSDDAttributes(SDD sdd) {
        try {
            if (sdd == null) {
                return 0;
            }

            // CRITICAL: SDDAttributes and SDDObjects are stored with partOfSchema = SDDICT..., not SDD...
            // The SDDICT URI is created from the DataFile URI
            String sddDataFileUri = sdd.getHasDataFileUri();

            if (sddDataFileUri == null || sddDataFileUri.isEmpty()) {
                System.out.println("    [SDDGen] countSDDAttributes: SDD has no DataFile URI");
                return 0;
            }

            String schemaUri = sddDataFileUri.replace("DFL", "SDDICT");

            String ns = org.hascoapi.utils.NameSpaces.getInstance().printSparqlNameSpaceList();

            // Count both SDDAttributes and SDDObjects
            // An SDD is valid if it has either attributes (data variables) or objects (contextual entities)
            String q = ns + " SELECT (COUNT(DISTINCT ?entity) AS ?count) WHERE { "
                    + "   { "
                    + "     ?entity a hasco:SDDAttribute . "
                    + "     ?entity hasco:partOfSchema <" + schemaUri + "> . "
                    + "   } UNION { "
                    + "     ?entity a hasco:SDDObject . "
                    + "     ?entity hasco:partOfSchema <" + schemaUri + "> . "
                    + "   } "
                    + " }";

            System.out.println("    [SDDGen] countSDDAttributes query for schema " + schemaUri + ":");
            System.out.println("    " + q);

            org.apache.jena.query.ResultSetRewindable rs = org.hascoapi.utils.SPARQLUtils.select(
                    org.hascoapi.utils.CollectionUtil.getCollectionPath(org.hascoapi.utils.CollectionUtil.Collection.SPARQL_QUERY), q);

            if (rs != null && rs.hasNext()) {
                org.apache.jena.query.QuerySolution soln = rs.next();
                int count = soln.getLiteral("count").getInt();
                System.out.println("    [SDDGen] Found " + count + " total entities (attributes + objects) for schema " + schemaUri);
                return count;
            } else {
                System.out.println("    [SDDGen] Query returned no results for schema " + schemaUri);
                return 0;
            }
        } catch (Exception e) {
            System.err.println("    [SDDGen] ERROR in countSDDAttributes: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Save the workbook to file.
     */
    public static String save(SDDGenHelper helper, String filename) {
        System.out.println("\n========== SDDGen.save() START ==========");
        System.out.println("  filename parameter: " + filename);

        if (helper == null || helper.workbook == null) {
            System.out.println("  ❌ helper or workbook is null");
            System.out.println("========== SDDGen.save() END (FAILURE) ==========\n");
            return null;
        }

        System.out.println("  ✅ helper and workbook are valid");

        try {
            // Ensure parent directory exists
            java.io.File file = new java.io.File(filename);
            String absolutePath = file.getAbsolutePath();
            java.io.File parentDir = file.getParentFile();

            System.out.println("  Output file absolute path: " + absolutePath);
            System.out.println("  Parent directory: " + (parentDir == null ? "null" : parentDir.getAbsolutePath()));

            if (parentDir != null && !parentDir.exists()) {
                System.out.println("  Parent directory does not exist, creating...");
                parentDir.mkdirs();
            }

            System.out.println("  Parent directory exists: " + (parentDir != null && parentDir.exists()));

            System.out.println("  ✅ Writing workbook to file...");
            FileOutputStream fileOut = new FileOutputStream(file);
            helper.workbook.write(fileOut);
            fileOut.close();
            helper.workbook.close();

            System.out.println("  ✅ File written successfully");
            System.out.println("  File size: " + file.length() + " bytes");
            System.out.println("  File exists: " + file.exists());
            System.out.println("  File can read: " + file.canRead());

            System.out.println("========== SDDGen.save() END (SUCCESS) ==========\n");
            return absolutePath;

        } catch (IOException e) {
            System.out.println("  ❌ IOException: " + e.getMessage());
            e.printStackTrace();
            System.out.println("========== SDDGen.save() END (EXCEPTION) ==========\n");
            return null;
        }
    }
}
