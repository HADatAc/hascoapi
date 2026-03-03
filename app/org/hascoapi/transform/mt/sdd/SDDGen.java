package org.hascoapi.transform.mt.sdd;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

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
     * @param dataFileUri The DataFile URI for which to generate the SDD
     * @param status The status filter (e.g., "Draft", "Processed")
     * @param filename Output filename
     * @param mediaFolder Media folder path
     * @return Absolute path to the generated file, or null on error
     */
    public static String genByStatus(String dataFileUri, String status, String filename, String mediaFolder) {
        System.out.println("\n========== SDDGen.genByStatus() START ==========");
        System.out.println("Input parameters:");
        System.out.println("  dataFileUri: " + dataFileUri);
        System.out.println("  status: " + status);
        System.out.println("  filename: " + filename);
        System.out.println("  mediaFolder: " + mediaFolder);

        SDDGenHelper helper = new SDDGenHelper();

        try {
            // Get base path from configuration
            String basePath = ConfigProp.getPathIngestion();
            if (basePath == null || basePath.isEmpty()) {
                basePath = ConfigProp.getPathUnproc();
            }

            // Normalize path separators
            basePath = basePath.replace("/", java.io.File.separator)
                               .replace("\\", java.io.File.separator);

            // Ensure trailing separator
            if (!basePath.endsWith(java.io.File.separator)) {
                basePath += java.io.File.separator;
            }

            // Extract base filename
            String baseName = filename;
            if (baseName.toLowerCase().endsWith(".xlsx")) {
                baseName = baseName.substring(0, baseName.length() - 5);
            }

            // Construct output filename
            String outFilename = basePath + baseName + ".xlsx";
            java.io.File outFile = new java.io.File(outFilename);
            String absolutePath = outFile.getAbsolutePath();

            System.out.println("  basePath (from ConfigProp): " + basePath);
            System.out.println("  basePath (normalized): " + basePath);
            System.out.println("  outFilename (final): " + outFilename);
            System.out.println("  outFilename (absolute): " + absolutePath);

            // Find SDD by DataFile URI
            SemanticDataDictionary sdd = null;
            List<SemanticDataDictionary> sddCandidates = SemanticDataDictionary.findAll();

            System.out.println("SDDGen.genByStatus: sdd MT candidates found=" +
                (sddCandidates == null ? 0 : sddCandidates.size()));

            if (sddCandidates != null && !sddCandidates.isEmpty()) {
                for (SemanticDataDictionary candidate : sddCandidates) {
                    String candidateDataFileUri = candidate.getHasDataFileUri();
                    System.out.println("  [SDDGen] candidate uri=" + candidate.getUri() +
                        " dataFile=" + candidateDataFileUri +
                        " status(raw)=" + candidate.getHasStatus());

                    if (candidateDataFileUri != null && candidateDataFileUri.equals(dataFileUri)) {
                        sdd = candidate;
                        System.out.println("    ✅ Including " + candidate.getUri() +
                            " (matches dataFileUri)");
                        break;
                    }
                }
            }

            if (sdd == null) {
                System.out.println("[SDDGen] WARNING: No SDD found for dataFileUri=" + dataFileUri);
                System.out.println("[SDDGen] Generating empty template");
            } else {
                System.out.println("SDDGen.genByStatus: resolved sdd=" + sdd.getUri() +
                    " hasDataFile=" + sdd.getHasDataFileUri());
            }

            // Create workbook
            helper.workbook = create(filename);

            // Add data if SDD was found
            if (sdd != null) {
                // Query and add Dictionary Mapping entries
                addDictionaryMappingData(helper, sdd);

                // Query and add Codebook entries (if any)
                addCodebookData(helper, sdd);

                // Query and add Timeline entries (if any)
                addTimelineData(helper, sdd);
            }

            System.out.println("\n✅ All data added to workbook, calling save()...");
            System.out.println("  Output filename: " + absolutePath);

            // Save namespaces
            saveNamespaces(helper);

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
    public static Workbook create(String filename) {
        Workbook workbook = new XSSFWorkbook();

        // Create InfoSheet
        Sheet infoSheet = workbook.createSheet(INFOSHEET);

        // InfoSheet headers
        Row isHeaderRow = infoSheet.createRow(0);
        isHeaderRow.createCell(0).setCellValue("Attribute");
        isHeaderRow.createCell(1).setCellValue("Value");

        // InfoSheet data rows
        int rowNum = 1;

        Row row1 = infoSheet.createRow(rowNum++);
        row1.createCell(0).setCellValue("SDD_ID");
        row1.createCell(1).setCellValue(extractSddId(filename));

        Row row2 = infoSheet.createRow(rowNum++);
        row2.createCell(0).setCellValue("Version");
        row2.createCell(1).setCellValue("1.0");

        Row row3 = infoSheet.createRow(rowNum++);
        row3.createCell(0).setCellValue("hasDependencies");
        row3.createCell(1).setCellValue("#" + NAMESPACES);

        Row row4 = infoSheet.createRow(rowNum++);
        row4.createCell(0).setCellValue("Data_Dictionary");
        row4.createCell(1).setCellValue("#" + DICTIONARY_MAPPING);

        Row row5 = infoSheet.createRow(rowNum++);
        row5.createCell(0).setCellValue("Codebook");
        row5.createCell(1).setCellValue("#" + CODEBOOK);

        Row row6 = infoSheet.createRow(rowNum++);
        row6.createCell(0).setCellValue("Timeline");
        row6.createCell(1).setCellValue("#" + TIMELINE);

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
    private static void addDictionaryMappingData(SDDGenHelper helper, SemanticDataDictionary sdd) {
        System.out.println("[SDDGen] Adding Dictionary Mapping data...");

        // Query for SDD Attributes associated with this SDD
        String sddUri = sdd.getUri();

        // Build SPARQL query to get all SDD attributes
        String query = org.hascoapi.utils.NameSpaces.getInstance().printSparqlNameSpaceList()
                + " SELECT DISTINCT ?uri WHERE { "
                + "   ?uri a hasco:SDDAttribute . "
                + "   ?uri hasco:partOfSchema <" + sddUri + "> . "
                + " } ORDER BY ?uri LIMIT " + PAGESIZE + " OFFSET " + OFFSET;

        System.out.println("[SDDGen] Dictionary Mapping query: " + query);

        List<SDDAttribute> attributes = org.hascoapi.entity.pojo.GenericFind.findByQuery(
            SDDAttribute.class, query);

        System.out.println("[SDDGen] Found " + (attributes == null ? 0 : attributes.size()) + " attributes");

        if (attributes != null && !attributes.isEmpty()) {
            Sheet sheet = helper.workbook.getSheet(DICTIONARY_MAPPING);
            for (SDDAttribute attr : attributes) {
                if (attr != null) {
                    SDDDictionaryMapping.add(sheet, helper, attr);
                }
            }
        }
    }

    /**
     * Add Codebook data from the triple store.
     */
    private static void addCodebookData(SDDGenHelper helper, SemanticDataDictionary sdd) {
        System.out.println("[SDDGen] Adding Codebook data...");

        // Query for PossibleValues associated with this SDD
        String sddUri = sdd.getUri();

        String query = org.hascoapi.utils.NameSpaces.getInstance().printSparqlNameSpaceList()
                + " SELECT DISTINCT ?uri WHERE { "
                + "   ?uri a hasco:PossibleValue . "
                + "   ?uri hasco:isPossibleValueOf ?attr . "
                + "   ?attr hasco:partOfSchema <" + sddUri + "> . "
                + " } ORDER BY ?uri LIMIT " + PAGESIZE + " OFFSET " + OFFSET;

        System.out.println("[SDDGen] Codebook query: " + query);

        List<PossibleValue> possibleValues = org.hascoapi.entity.pojo.GenericFind.findByQuery(
            PossibleValue.class, query);

        System.out.println("[SDDGen] Found " + (possibleValues == null ? 0 : possibleValues.size()) +
            " possible values");

        if (possibleValues != null && !possibleValues.isEmpty()) {
            Sheet sheet = helper.workbook.getSheet(CODEBOOK);
            for (PossibleValue pv : possibleValues) {
                if (pv != null) {
                    SDDCodebook.add(sheet, helper, pv);
                }
            }
        }
    }

    /**
     * Add Timeline data from the triple store.
     */
    private static void addTimelineData(SDDGenHelper helper, SemanticDataDictionary sdd) {
        System.out.println("[SDDGen] Adding Timeline data...");

        // Timeline is typically defined through SDDObject entities with Time annotation
        String sddUri = sdd.getUri();

        String query = org.hascoapi.utils.NameSpaces.getInstance().printSparqlNameSpaceList()
                + " SELECT DISTINCT ?uri WHERE { "
                + "   ?uri a hasco:SDDObject . "
                + "   ?uri hasco:partOfSchema <" + sddUri + "> . "
                + "   ?uri hasco:hasRole hasco:TimeRole . "
                + " } ORDER BY ?uri LIMIT " + PAGESIZE + " OFFSET " + OFFSET;

        System.out.println("[SDDGen] Timeline query: " + query);

        List<SDDObject> timelineObjects = org.hascoapi.entity.pojo.GenericFind.findByQuery(
            SDDObject.class, query);

        System.out.println("[SDDGen] Found " + (timelineObjects == null ? 0 : timelineObjects.size()) +
            " timeline objects");

        if (timelineObjects != null && !timelineObjects.isEmpty()) {
            Sheet sheet = helper.workbook.getSheet(TIMELINE);
            for (SDDObject obj : timelineObjects) {
                if (obj != null) {
                    SDDTimeline.add(sheet, helper, obj);
                }
            }
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

        // Namespaces sheet schema
        Row row = namespacesSheet.createRow(0);
        row.createCell(0).setCellValue("hasPrefix");
        row.createCell(1).setCellValue("hasNameSpace");
        row.createCell(2).setCellValue("hasFormat");
        row.createCell(3).setCellValue("hasSource");

        // Required prefixes for SDD
        int rowIndex = 1;
        rowIndex = writeNamespaceRow(namespacesSheet, rowIndex, "hasco", "http://hadatac.org/ont/hasco/", "text/turtle", "http://hadatac.org/ont/hasco/");
        rowIndex = writeNamespaceRow(namespacesSheet, rowIndex, "vstoi", "http://hadatac.org/ont/vstoi#", "text/turtle", "http://hadatac.org/ont/vstoi#");
        rowIndex = writeNamespaceRow(namespacesSheet, rowIndex, "sio", "http://semanticscience.org/resource/", "text/turtle", "http://semanticscience.org/resource/");
        rowIndex = writeNamespaceRow(namespacesSheet, rowIndex, "unit", "http://qudt.org/vocab/unit/", "text/turtle", "http://qudt.org/vocab/unit/");

        // Add collected namespaces (avoid duplicates)
        java.util.Set<String> seenPrefixes = new java.util.HashSet<>();
        seenPrefixes.add("hasco");
        seenPrefixes.add("vstoi");
        seenPrefixes.add("sio");
        seenPrefixes.add("unit");

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
        newRow.createCell(2).setCellValue(format == null ? "" : format);
        newRow.createCell(3).setCellValue(source == null ? "" : source);
        return rowIndex + 1;
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

