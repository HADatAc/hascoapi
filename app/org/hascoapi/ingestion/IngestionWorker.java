package org.hascoapi.ingestion;

import java.lang.String;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.Study;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.ConfigProp;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.vocabularies.VSTOI;


public class IngestionWorker {

    public static void ingest(DataFile dataFile, File file, String templateFile, String status) {

        // DP2 status rule: if the API didn't provide a status, default to DRAFT for DP2 ingestion.
        final String fileNameForRule = (dataFile.getFilename() == null ? "" : dataFile.getFilename());
        String effectiveStatus = status;
        if (fileNameForRule.startsWith("DP2-") || fileNameForRule.contains("/DP2-") || fileNameForRule.contains("\\DP2-")) {
            if (effectiveStatus == null || effectiveStatus.trim().isEmpty()) {
                effectiveStatus = VSTOI.DRAFT;
            }
        }

        dataFile.setLastProcessTime(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));
        dataFile.getLogger().resetLog();
        dataFile.save();

        String fileName = dataFile.getFilename();

        dataFile.getLogger().println(String.format("Processing file: %s", fileName));

        // file is rejected if it has an invalid extension
        RecordFile recordFile = null;
        if (fileName.endsWith(".csv")) {
            recordFile = new CSVRecordFile(file);
        } else if (fileName.endsWith(".xlsx")) {
            recordFile = new SpreadsheetRecordFile(file,dataFile.getFilename(),"InfoSheet");
        } else {
            dataFile.getLogger().printExceptionByIdWithArgs("GBL_00003", fileName);
            return;
        }

        if (!recordFile.isValid()) {
            dataFile.getLogger().printExceptionById("GBL_00005");
            return;
        }

        dataFile.setRecordFile(recordFile);

        // Auto-detect DASOC files (DA-SOC-* pattern)
        // Study URI will be discovered automatically from originalIDs in the CSV
        if (FilenameUtils.getBaseName(fileName).startsWith("DA-SOC-")) {
            String baseName = FilenameUtils.getBaseName(fileName);
            String socName = baseName.substring(7); // "DA-SOC-EQUIPMENT-MODULE" -> "EQUIPMENT-MODULE"

            dataFile.getLogger().println("DASOC file detected - SOC name from filename: " + socName);
            dataFile.getLogger().println("Study URI will be discovered from originalIDs in the CSV file");

            // Store SOC name for reference (used in logs only, not for processing)
            // The actual Study URI is discovered by tracing originalID -> Object -> Collection -> Study
            dataFile.save();
            // Try to find SOC by querying for objects with matching originalID pattern
            String socUri = findSOCByName(socName);
            if (socUri != null && !socUri.isEmpty()) {
                dataFile.setDasocSOCUri(socUri);
                dataFile.getLogger().println("✅ Auto-detected SOC URI: " + socUri);
                System.out.println("[INGESTION PATH] Auto-detected SOC URI from triplestore: " + socUri);
            } else {
                // If not found,construct expected SOC URI using OCL_ naming convention (standard for DSG-ingested SOCs)
                String kbPrefix = ConfigProp.getKbPrefix();
                if (kbPrefix != null && !kbPrefix.isEmpty()) {
                    socUri = kbPrefix + "OCL_" + socName;
                    dataFile.setDasocSOCUri(socUri);
                    dataFile.getLogger().println("⚠️  SOC not found in triplestore, using OCL_ convention-based URI: " + socUri);
                    System.out.println("[INGESTION PATH] Using OCL_ convention-based SOC URI: " + socUri);
                } else {
                    dataFile.getLogger().printWarning("Could not auto-detect SOC URI for: " + socName);
                    System.out.println("[WARNING] IngestionWorker: Could not determine SOC URI for " + socName);
                }
            }
            dataFile.save(); // Save SOC URI to DataFile
        }

        // Setting study URI from dataFile
        String studyUri = "";
        if (dataFile.getFilename().contains("DSG-")) {
            // Getting study URI from InfoSheet
            studyUri = getStudyUri(dataFile);

            // Getting study URI from InfoSheet
            // NOTE: do NOT use '==' for string emptiness checks; that can be non-deterministic.
            if (studyUri == null || studyUri.isEmpty()) {
                studyUri = dataFile.getUri().replace(Constants.PREFIX_DATAFILE, Constants.PREFIX_STUDY);
            }

        }

        boolean bSucceed = false;

        GeneratorChain chain = getGeneratorChain(dataFile, studyUri, templateFile, effectiveStatus);

        // If no chain was produced, log and throw exception to fail fast (as requested)
        if (chain == null) {
            String msg = "No generator chain produced. Aborting ingestion.";
            dataFile.getLogger().println(msg);
            throw new RuntimeException(msg);
        }

        // Only set study URI if a chain was produced
        if (studyUri == null || studyUri.isEmpty()) {
            chain.setStudyUri("");
        } else {
            chain.setStudyUri(studyUri);
        }

        try {
            bSucceed = chain.generate();
            chain.disposeChain();
        } catch (Exception e) {
            dataFile.getLogger().println("ERROR during ingestion: " + e.getMessage());
            e.printStackTrace();
            bSucceed = false;
        }

        if (bSucceed) {

            try {
                // if chain includes PVGenerator, executes PVGenerator.generateOthers()
                if (chain.getPV()) {
                    PVGenerator.generateOthers(chain.getCodebookFile(), chain.getSddName(), ConfigProp.getKbPrefix());
                }

                // WKF post-processing: Create ProcessBasedStudy entities from Process entities
                String fileNameBase = FilenameUtils.getBaseName(dataFile.getFilename());
                if (fileNameBase.startsWith("WKF-")) {
                    AnnotateWKF.postProcessAfterIngestion(dataFile);
                }

                dataFile.setFileStatus(DataFile.PROCESSED);
                dataFile.setCompletionTime(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));
                dataFile.setStudyUri(chain.getStudyUri());
                dataFile.save();
            } catch (Exception e) {
                dataFile.getLogger().println("ERROR during finalization: " + e.getMessage());
                e.printStackTrace();
            }

        }
    }

    public static GeneratorChain getGeneratorChain(DataFile dataFile, String studyUri, String templateFile, String status) {
        GeneratorChain chain = null;
        String fileName = FilenameUtils.getBaseName(dataFile.getFilename());

        // Check for DA-SOC files BEFORE general DA files
        // DA-SOC files are DASOC (Data Acquisition - Study Object Collection) links
        // They extend SOC content with properties that don't fit in DSG files
        if (fileName.startsWith("DA-SOC-")) {
            dataFile.getLogger().println("Processing as DASOC (Data Acquisition - Study Object Collection)");
            chain = AnnotateDASOC.exec(dataFile);

        } else if (fileName.startsWith("DA-")) {
            // REJECT: General DA ingestion is not working at this time
            // NOTE: DA-SOC is not yet an official element type in hascoapi
            // Only DA-SOC-* files are supported through AnnotateDASOC
            dataFile.getLogger().printException("ERROR: General DA ingestion is not supported. Only DA-SOC-* files can be ingested.");
            dataFile.setFileStatus(DataFile.UNPROCESSED);
            dataFile.save();
            return null; // No chain to process

        } else if (fileName.startsWith("DSG-")) {
            boolean bSucceed = false;

            chain = AnnotateSTD.exec(dataFile, studyUri, templateFile);
            if (chain != null) {
                bSucceed = chain.generate();
                chain.disposeChain();
            }
            if (bSucceed) {
                // Barrier: on a fresh triplestore, the Study created by STD may not be
                // immediately readable by SSD (Study.find). Wait deterministically.
                if (!waitForStudyVisible(studyUri, 15000 /*ms*/)) {
                    dataFile.getLogger().println("DSG ingestion: Study not visible after STD commit; aborting SSD phase. studyUri=" + studyUri);
                    dataFile.getLogger().printExceptionByIdWithArgs("DSG_00010", studyUri);
                    return null;
                }

                // Verify all referenced sheets in SSD before executing SSD annotation
                if (!verifySheetsInSSD(dataFile)) {
                    dataFile.getLogger().printExceptionById("DSG_00022");
                    return null;
                }
                chain = AnnotateSSD.exec(dataFile, studyUri, templateFile, status);

                // After successful SSD ingestion, automatically process DA-SOC files
                if (chain != null && chain.isValid()) {
                    processAssociatedDASOCFiles(dataFile, dataFile.getFile(), studyUri);
                }
            }

        } else if (fileName.startsWith("DP2-")) {
            chain = AnnotateDP2.exec(dataFile, templateFile, status);

        } else if (fileName.startsWith("INS-")) {
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // ⚠️  INS FORMAT IS DEPRECATED - USE DSG + DA-SOC INSTEAD
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            dataFile.getLogger().println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            dataFile.getLogger().println("⚠️  DEPRECATION WARNING: INS format is deprecated");
            dataFile.getLogger().println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            dataFile.getLogger().println("");
            dataFile.getLogger().println("The INS (Instrument Namespace Specification) file format");
            dataFile.getLogger().println("is deprecated and will be removed in a future release.");
            dataFile.getLogger().println("");
            dataFile.getLogger().println("Please migrate to the DSG + DA-SOC approach:");
            dataFile.getLogger().println("");
            dataFile.getLogger().println("NEW WORKFLOW:");
            dataFile.getLogger().println("  1. Create a DSG file with SOCs for VSTOI entities:");
            dataFile.getLogger().println("     - SOC-INSTRUMENT-<name>");
            dataFile.getLogger().println("     - SOC-COMPONENT-<name>");
            dataFile.getLogger().println("     - SOC-COMPONENT-STEM-<name>");
            dataFile.getLogger().println("     - SOC-SLOT-ELEMENT-<name>");
            dataFile.getLogger().println("     - SOC-CODEBOOK-<name>");
            dataFile.getLogger().println("     - SOC-RESPONSE-OPTION-<name>");
            dataFile.getLogger().println("");
            dataFile.getLogger().println("  2. In each SOC worksheet, include columns:");
            dataFile.getLogger().println("     - originalID (unique identifier)");
            dataFile.getLogger().println("     - rdf:type (e.g., vstoi:Instrument)");
            dataFile.getLogger().println("     - rdfs:label (human-readable name)");
            dataFile.getLogger().println("     - rdfs:comment (description)");
            dataFile.getLogger().println("");
            dataFile.getLogger().println("  3. Create DA-SOC files for extended properties:");
            dataFile.getLogger().println("     - DA-SOC-INSTRUMENT-<name>.csv");
            dataFile.getLogger().println("     - DA-SOC-COMPONENT-<name>.csv");
            dataFile.getLogger().println("     - etc.");
            dataFile.getLogger().println("");
            dataFile.getLogger().println("  4. In DA-SOC files, include VSTOI-specific properties:");
            dataFile.getLogger().println("     - vstoi:hasFirst, vstoi:hasShortName, vstoi:hasLanguage");
            dataFile.getLogger().println("     - vstoi:hasComponentStem, vstoi:hasCodebook");
            dataFile.getLogger().println("     - vstoi:belongsTo, vstoi:hasNext, vstoi:hasPrevious");
            dataFile.getLogger().println("");
            dataFile.getLogger().println("BENEFITS:");
            dataFile.getLogger().println("  ✓ Unified data management (same framework for all studies)");
            dataFile.getLogger().println("  ✓ Better version control (CSV files vs Excel sheets)");
            dataFile.getLogger().println("  ✓ Easier collaboration (separate files for different aspects)");
            dataFile.getLogger().println("  ✓ More flexible property extension (via DA-SOC)");
            dataFile.getLogger().println("");
            dataFile.getLogger().println("For migration assistance, see:");
            dataFile.getLogger().println("  docs/INS-TO-DSG-TRANSFORMATION-PLAN.md");
            dataFile.getLogger().println("");
            dataFile.getLogger().println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            dataFile.getLogger().println("");
            dataFile.getLogger().println("Proceeding with INS ingestion (legacy mode)...");
            dataFile.getLogger().println("");
            
            chain = AnnotateINS.exec(dataFile, templateFile, status);

        } else if (fileName.startsWith("KGR-")) {
            chain = AnnotateKGR.exec(dataFile, templateFile, status);

        } else if (fileName.startsWith("STR-")) {
            chain = AnnotateSTR.exec(dataFile, templateFile);

        } else if (fileName.startsWith("SDD-")) {
            chain = AnnotateSDD.exec(dataFile, templateFile);

        } else if (fileName.startsWith("WKF_")) {
            dataFile.getLogger().printException("ERROR: Invalid WKF filename prefix 'WKF_'. Use 'WKF-' (hyphen). Ingestion rejected.");
            dataFile.setFileStatus(DataFile.UNPROCESSED);
            dataFile.save();
            return null;

        } else if (fileName.startsWith("WKF-")) {
            chain = AnnotateWKF.exec(dataFile, templateFile, status);

        } else if (fileName.startsWith("DOI-")) {
            chain = AnnotateDOI.exec(dataFile);

        } else {
            dataFile.getLogger().printExceptionById("GBL_00001");
            return null;
        }

        return chain;
    }

    /**
     * Wait until the Study created by STD is visible in the triplestore.
     * This prevents flaky first-run DSG ingestion where SSD runs before the
     * triplestore makes the new Study readable.
     */
    private static boolean waitForStudyVisible(String studyUri, long timeoutMs) {
        if (studyUri == null || studyUri.trim().isEmpty()) {
            return false;
        }
        long deadline = System.currentTimeMillis() + timeoutMs;
        long sleepMs = 150;
        int attempt = 0;

        // Expand CURIE to full URI if necessary
        String expandedStudyUri = studyUri;
        if (studyUri != null && !studyUri.startsWith("http://") && !studyUri.startsWith("https://")) {
            expandedStudyUri = URIUtils.replacePrefixEx(studyUri);
        }

        while (System.currentTimeMillis() < deadline) {
            attempt++;
            try {
                Study s = Study.find(expandedStudyUri);
                if (s != null) {
                    return true;
                }
            } catch (Exception e) {
                // ignore and retry; transient Fuseki hiccups can happen right after startup
            }

            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return false;
            }
            sleepMs = Math.min(1000, sleepMs * 2);
        }

        return false;
    }

    private static boolean verifySheetsInSSD(DataFile dataFile) {
        SpreadsheetRecordFile ssdSheet = new SpreadsheetRecordFile(
                dataFile.getFile(), dataFile.getFilename(), "SSD");
        if (ssdSheet == null || !ssdSheet.isValid() || ssdSheet.getRecords() == null || ssdSheet.getRecords().isEmpty()) {
            dataFile.getLogger().printExceptionByIdWithArgs("GBL_00014", "SSD verification: SSD sheet");
            return false;
        }

        for (Record r : ssdSheet.getRecords()) {
            String referencedSheet = r.getValueByColumnIndex(0);
            if (referencedSheet == null || referencedSheet.trim().isEmpty()) {
                continue;
            }
            String sheetName = referencedSheet.replace("#", "").trim();
            SpreadsheetRecordFile ref = new SpreadsheetRecordFile(
                    dataFile.getFile(), dataFile.getFilename(), sheetName);

            if (ref == null || !ref.isValid() || ref.getRecords() == null || ref.getRecords().isEmpty()) {
                dataFile.getLogger().printExceptionByIdWithArgs("GBL_00015", referencedSheet);
                return false; // stop immediately on first missing/unreadable sheet
            }
        }

        return true;
    }

    /*
     * Move any file that isMediaFile() into a media folder in processed files.
     * At the moment, no other kind of processing is performed by this code.
     */
    /*
    public static void processMediaFile(DataFile dataFile, File file) {
    	//Move the file to the folder for processed files
        String new_path = ConfigProp.getPathMedia();

        File file = new File(dataFile.getAbsolutePath());

        File destFolder = new File(new_path);
        if (!destFolder.exists()) {
            destFolder.mkdirs();
        }

        dataFile.setFileStatus(DataFile.PROCESSED);
        dataFile.setCompletionTime(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));
        dataFile.setDir(ConfigProp.MEDIA_FOLDER);
        dataFile.setStudyUri("");
        dataFile.save();

        file.renameTo(new File(destFolder + "/" + dataFile.getStorageFileName()));
        file.delete();
    }
    */

    /*========================================================================*
     *                       METADATA TEMPLATE ANNOTATORS                     *
     *========================================================================*/

    public static boolean nameSpaceGen(DataFile dataFile, Map<String, String> mapCatalog, String templateFile) {
        RecordFile nameSpaceRecordFile = null;
        
        // hasDependencies is a FIELD in InfoSheet, not a sheet name.
        // We need to look for the actual Namespace/Namespaces sheet.
        String sheetName = null;
        
        // First try to get from catalog (might be mapped to Namespace or Namespaces)
        String namespaceCatalogEntry = mapCatalog.get("Namespaces");
        if (namespaceCatalogEntry == null || namespaceCatalogEntry.trim().isEmpty()) {
            namespaceCatalogEntry = mapCatalog.get("Namespace");
        }
        
        if (namespaceCatalogEntry != null && !namespaceCatalogEntry.trim().isEmpty()) {
            sheetName = namespaceCatalogEntry.replace("#", "").trim();
        } else {
            // Fallback: try both sheet names directly
            SpreadsheetRecordFile probe1 = new SpreadsheetRecordFile(dataFile.getFile(), "Namespaces");
            SpreadsheetRecordFile probe2 = new SpreadsheetRecordFile(dataFile.getFile(), "Namespace");
            
            if (probe1.isValid() && probe1.getRecords() != null && !probe1.getRecords().isEmpty()) {
                sheetName = "Namespaces";
            } else if (probe2.isValid() && probe2.getRecords() != null && !probe2.getRecords().isEmpty()) {
                sheetName = "Namespace";
            }
        }
        
        if (sheetName != null) {
            nameSpaceRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), sheetName);
            if (nameSpaceRecordFile == null) {
                dataFile.getLogger().printWarning("GBL_00009");
            } else if (nameSpaceRecordFile.getRecords() == null) {
                dataFile.getLogger().printWarning("GBL_00010");
            } else {
                dataFile.getLogger().println("Namespace generation completed using sheet: " + sheetName);
                dataFile.setRecordFile(nameSpaceRecordFile);

                GeneratorChain chain = new GeneratorChain();
                chain.setNamedGraphUri(dataFile.getUri());
                chain.addGenerator(new NameSpaceGenerator(dataFile,templateFile));
                boolean isSuccess = false;
                if (chain != null) {
                    isSuccess = chain.generate();
                }
                return isSuccess;
            }
        } else {
            dataFile.getLogger().printWarning("GBL_00011: Namespace/Namespaces sheet not found");
        }
        return false;
    }

    public static boolean annotationGen(DataFile dataFile, Map<String, String> mapCatalog, String templateFile, String status) {
        RecordFile annotationStemRecordFile = null;
        RecordFile annotationRecordFile = null;
        DataFile annotationStemDataFile;
        DataFile annotationDataFile;
        try {
            annotationStemDataFile = (DataFile)dataFile.clone();
            annotationDataFile = (DataFile)dataFile.clone();
        } catch (Exception e) {
            dataFile.getLogger().printExceptionByIdWithArgs("GBL_00012", e.getMessage());
            return false;
        }

        boolean hasAnyAnnotationSheet = false;

        String sheetName = mapCatalog.get("AnnotationStems");
        if (sheetName != null) {
            annotationStemRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), sheetName.replace("#", ""));

            // SpreadsheetRecordFile is never null, but may be invalid when the sheet doesn't exist.
            if (annotationStemRecordFile == null || !annotationStemRecordFile.isValid() || annotationStemRecordFile.getRecords() == null) {
                // Treat missing/invalid sheet as absent (skip), not as a hard failure.
                annotationStemRecordFile = null;
            } else if (annotationStemRecordFile.getRecords().isEmpty()) {
                // Empty sheet: treat as absent.
                annotationStemRecordFile = null;
            } else {
                annotationStemDataFile.setRecordFile(annotationStemRecordFile);
                hasAnyAnnotationSheet = true;
            }
        }

        sheetName = mapCatalog.get("Annotations");
        if (sheetName != null) {
            annotationRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), sheetName.replace("#", ""));

            if (annotationRecordFile == null || !annotationRecordFile.isValid() || annotationRecordFile.getRecords() == null) {
                annotationRecordFile = null;
            } else if (annotationRecordFile.getRecords().isEmpty()) {
                annotationRecordFile = null;
            } else {
                annotationDataFile.setRecordFile(annotationRecordFile);
                hasAnyAnnotationSheet = true;
            }
        }

        // If the template doesn't include annotation sheets (or the workbook doesn't provide them),
        // skip annotation generation gracefully.
        if (!hasAnyAnnotationSheet) {
            dataFile.getLogger().println("annotationGen(): no AnnotationStems/Annotations sheet configured; skipping annotation generation.");
            return true;
        }

        GeneratorChain chain = new GeneratorChain();
        chain.setNamedGraphUri(dataFile.getUri());

        if (annotationStemRecordFile != null) {
            INSGenerator annotationStemGen = new INSGenerator("annotationstem", annotationStemDataFile, status);
            annotationStemGen.setNamedGraphUri(dataFile.getUri());
            chain.addGenerator(annotationStemGen);
        }
        if (annotationRecordFile != null) {
            INSGenerator annotationGen = new INSGenerator("annotation", annotationDataFile, status);
            annotationGen.setNamedGraphUri(dataFile.getUri());
            chain.addGenerator(annotationGen);
        }

        boolean isSuccess = false;
        isSuccess = chain.generate();

        if (!isSuccess) {
            dataFile.getLogger().printWarningByIdWithArgs("GBL_00016", "annotationStem and/or annotation");
        }
        return isSuccess;
    }

    public static boolean messageGen(DataFile dataFile, Map<String, String> mapCatalog, String templateFile, String status) {
        RecordFile messageStreamRecordFile = null;
        RecordFile messageTopicRecordFile = null;
        DataFile messageStreamDataFile = cloneDataFileSafe(dataFile);
        DataFile messageTopicDataFile = cloneDataFileSafe(dataFile);

        String sheetName = mapCatalog.get("MessageStream");
        if (sheetName != null) {
            System.out.print("Extracting [MessageStream] sheet from spreadsheet... ");
            messageStreamRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), sheetName.replace("#", ""));
            if (messageStreamRecordFile == null || !messageStreamRecordFile.isValid() || messageStreamRecordFile.getRecords() == null) {
                dataFile.getLogger().printWarningByIdWithArgs("GBL_00014", "messageGen(): messageStream");
                messageStreamRecordFile = null;
            } else {
                messageStreamDataFile.setRecordFile(messageStreamRecordFile);
            }
        }

        sheetName = mapCatalog.get("MessageTopic");
        if (sheetName != null) {
            System.out.print("Extracting [MessageTopic] sheet from spreadsheet... ");
            messageTopicRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), sheetName.replace("#", ""));
            if (messageTopicRecordFile == null || !messageTopicRecordFile.isValid() || messageTopicRecordFile.getRecords() == null) {
                dataFile.getLogger().printWarningByIdWithArgs("GBL_00014", "messageGen(): messageTopic");
                messageTopicRecordFile = null;
            } else {
                messageTopicDataFile.setRecordFile(messageTopicRecordFile);
            }
        }

        DP2Generator messageStreamGen = null;
        DP2Generator messageTopicGen = null;

        if (messageStreamRecordFile != null) {
            messageStreamGen = new DP2Generator("messagestream", messageStreamDataFile, status);
            messageStreamGen.setNamedGraphUri(dataFile.getUri());
        }

        if (messageTopicRecordFile != null) {
            messageTopicGen = new DP2Generator("messagetopic", messageTopicDataFile, status);
            messageTopicGen.setNamedGraphUri(dataFile.getUri());
        }

        // If the workbook doesn't define any of these sheets, there's nothing to do here.
        if (messageStreamGen == null && messageTopicGen == null) {
            System.out.println("No DP2 message sheets found (MessageStream/MessageTopic); skipping messageGen.");
            return true;
        }

        GeneratorChain chain = new GeneratorChain();
        chain.setNamedGraphUri(dataFile.getUri());
        if (messageStreamGen != null) {
            chain.addGenerator(messageStreamGen);
        }
        if (messageTopicGen != null) {
            chain.addGenerator(messageTopicGen);
        }

        boolean isSuccess = false;
        if (chain != null) {
            isSuccess = chain.generate();
        }
        if (isSuccess) {
            System.out.println("Done extracting messageStream and messageTopic sheets. ");
        } else {
            dataFile.getLogger().printWarningByIdWithArgs("GBL_00016", "messageStream and/or messageTopic");
            System.out.println("Failed to extract messageStream and/or messageTopic sheets. ");
        }
        return isSuccess;
    }

    // Backward-compatible overload
    public static boolean messageGen(DataFile dataFile, Map<String, String> mapCatalog, String templateFile) {
        return messageGen(dataFile, mapCatalog, templateFile, null);
    }

    public static boolean deployInstancesGen(DataFile dataFile, Map<String, String> mapCatalog, String templateFile, String status) {
        RecordFile instrumentsRecordFile = null;
        RecordFile componentsRecordFile = null;
        RecordFile sensingPerspectiveRecordFile = null;
        DataFile instrumentsDataFile;
        DataFile componentsDataFile;
        DataFile sensingPerspectiveDataFile;

        try {
            // Use safe clone to prevent NPE in tests where DataFile.clone() might return null
            instrumentsDataFile = cloneDataFileSafe(dataFile);
            componentsDataFile = cloneDataFileSafe(dataFile);
            sensingPerspectiveDataFile = cloneDataFileSafe(dataFile);
        } catch (Exception e) {
            dataFile.getLogger().printExceptionByIdWithArgs("GBL_00012", e.getMessage());
            return false;
        }

        boolean hasAnyInstanceSheet = false;

        // Resolve DP2 instance sheet names defensively (catalog may be wrong)
        java.util.function.Function<String, String> resolveSheet = (key) -> {
            String fromCatalog = mapCatalog != null ? mapCatalog.get(key) : null;
            if (fromCatalog != null && !fromCatalog.trim().isEmpty()) {
                return fromCatalog.replace("#", "").trim();
            }
            return key;
        };

        String resolvedInstrumentInstances = resolveSheet.apply("InstrumentInstances");
        System.out.print("Extracting [InstrumentInstances] sheet from spreadsheet... ");
        instrumentsRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), resolvedInstrumentInstances);
        if (instrumentsRecordFile == null || !instrumentsRecordFile.isValid() || instrumentsRecordFile.getRecords() == null || instrumentsRecordFile.getRecords().isEmpty()) {
            instrumentsRecordFile = null;
        } else {
            instrumentsDataFile.setRecordFile(instrumentsRecordFile);
            hasAnyInstanceSheet = true;
        }

        String resolvedComponentInstances = resolveSheet.apply("ComponentInstances");
        System.out.print("Extracting [ComponentInstances] sheet from spreadsheet... ");
        componentsRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), resolvedComponentInstances);
        if (componentsRecordFile == null || !componentsRecordFile.isValid() || componentsRecordFile.getRecords() == null || componentsRecordFile.getRecords().isEmpty()) {
            componentsRecordFile = null;
        } else {
            componentsDataFile.setRecordFile(componentsRecordFile);
            hasAnyInstanceSheet = true;
        }

        String resolvedSensingPerspective = resolveSheet.apply("SensingPerspective");
        System.out.print("Extracting [SensingPerspective] sheet from spreadsheet... ");
        sensingPerspectiveRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), resolvedSensingPerspective);
        if (sensingPerspectiveRecordFile == null || !sensingPerspectiveRecordFile.isValid() || sensingPerspectiveRecordFile.getRecords() == null || sensingPerspectiveRecordFile.getRecords().isEmpty()) {
            sensingPerspectiveRecordFile = null;
        } else {
            sensingPerspectiveDataFile.setRecordFile(sensingPerspectiveRecordFile);
            hasAnyInstanceSheet = true;
        }

        // If no instance sheets were found, exit gracefully (like annotationGen)
        if (!hasAnyInstanceSheet) {
            System.out.println("No DP2 instance sheets found (InstrumentInstances/ComponentInstances/SensingPerspective); skipping deployInstancesGen.");
            return true;
        }

        DP2Generator instrumentsGen = null;
        DP2Generator detectorsGen = null;
        DP2Generator sensingPerspectiveGen = null;

        if (instrumentsRecordFile != null) {
            instrumentsGen = new DP2Generator("instrumentinstance", instrumentsDataFile, status);
            instrumentsGen.setNamedGraphUri(dataFile.getUri());
        } else {
            instrumentsGen = null;
        }

        if (componentsRecordFile != null) {
            detectorsGen = new DP2Generator("componentinstance", componentsDataFile, status);
            detectorsGen.setNamedGraphUri(dataFile.getUri());
        } else {
            detectorsGen = null;
        }

        if (sensingPerspectiveRecordFile != null) {
            sensingPerspectiveGen = new DP2Generator("sensingperspective", sensingPerspectiveDataFile, status);
            sensingPerspectiveGen.setNamedGraphUri(dataFile.getUri());
        } else {
            sensingPerspectiveGen = null;
        }

        GeneratorChain chain = new GeneratorChain();
        chain.setNamedGraphUri(dataFile.getUri());
        if (instrumentsGen != null) chain.addGenerator(instrumentsGen);
        if (detectorsGen != null) chain.addGenerator(detectorsGen);
        if (sensingPerspectiveGen != null) chain.addGenerator(sensingPerspectiveGen);

        boolean isSuccess = chain.generate();
        if (isSuccess) {
            System.out.println("Done extracting instruments, components and sensingPerspective sheets. ");
        } else {
            dataFile.getLogger().printWarningByIdWithArgs("GBL_00016", "instruments and/or detectors and/or sensingPerspective");
            System.out.println("Failed to extract instruments and/or detectors and/or sensingPerspective sheets. ");
        }
        return isSuccess;
    }

    // Backward-compatible overload
    public static boolean deployInstancesGen(DataFile dataFile, Map<String, String> mapCatalog, String templateFile) {
        return deployInstancesGen(dataFile, mapCatalog, templateFile, null);
    }

    /**
     * Find StudyObjectCollection URI by matching SOC name pattern.
     * Queries triplestore for SOCs with URIs exactly matching the SOC name.
     * Uses REGEX to ensure exact segment matching (e.g., "UNIT" won't match "BUSINESS-UNIT").
     *
     * @param socName The SOC name extracted from filename (e.g., "ENTERPRISE", "API", "PRODUCT")
     * @return SOC URI if found, null otherwise
     */
    public static String findSOCByName(String socName) {
        if (socName == null || socName.isEmpty()) {
            return null;
        }

        try {
            // Use REGEX to match exact URI endings (not substrings)
            // This prevents "UNIT" from matching "BUSINESS-UNIT"
            String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT ?socUri WHERE { " +
                "  ?socUri a hasco:StudyObjectCollection . " +
                "  FILTER( " +
                "    REGEX(STR(?socUri), \"[/#]SOC-" + socName + "$\", \"i\") || " +
                "    REGEX(STR(?socUri), \"[/#]OCL_" + socName + "$\", \"i\") " +
                "  ) " +
                "} LIMIT 1";

            System.out.println("IngestionWorker.findSOCByName(): Querying for SOC with name: " + socName);

            ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

            if (resultsrw.hasNext()) {
                QuerySolution soln = resultsrw.next();
                if (soln != null && soln.getResource("socUri") != null) {
                    String socUri = soln.getResource("socUri").getURI();
                    System.out.println("IngestionWorker.findSOCByName(): Found SOC URI: " + socUri);
                    return socUri;
                }
            }

            System.out.println("IngestionWorker.findSOCByName(): No SOC found for name: " + socName);
            return null;

        } catch (Exception e) {
            System.err.println("[ERROR] IngestionWorker.findSOCByName(): " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static String getStudyUri(DataFile dataFile) {
        String studyUri = "";
        String studyKG = "";
        if (dataFile.getRecordFile() != null) {
            for (Record record : dataFile.getRecordFile().getRecords()) {
                if (record.getValueByColumnIndex(0).equals("hasStudyKG")) {
                    if (record.getValueByColumnIndex(1) != null){
                        studyKG = record.getValueByColumnIndex(1).trim();
                    }
                }
                if (record.getValueByColumnIndex(0).equals("hasStudyURI")) {
                    if (record.getValueByColumnIndex(1) != null){
                        studyUri = record.getValueByColumnIndex(1).trim();
                    }
                }
            }
        }

        if (studyUri == null || studyUri.isEmpty()) {
            dataFile.getLogger().printWarningById("GBL_00017");
            System.out.println("IngestionWorker: failed to build studyUri - missing hasStudyURI portion of the URI in the InfoSheet");
            return null;
        }

        // If InfoSheet already provides a full URI, use it as-is.
        // This avoids accidental double-prefixing like "ahead:STD-http://...".
        String trimmedStudyUri = studyUri.trim();
        if (trimmedStudyUri.startsWith("http://") || trimmedStudyUri.startsWith("https://")) {
            return trimmedStudyUri.replace("#/", "#");
        }

        // If InfoSheet provides a compact URI (CURIE) like "ahead:EOL-AVL-SNL", treat it as final.
        // This prevents generating "ahead:STD-ahead:EOL-AVL-SNL".
        if (trimmedStudyUri.contains(":")) {
            String resolved = URIUtils.replacePrefixEx(trimmedStudyUri);
            return resolved.replace("#/", "#");
        }

        if (studyKG == null || studyKG.isEmpty()) {
            dataFile.getLogger().printWarningById("GBL_00018");
            System.out.println("IngestionWorker: failed to build studyUri - missing hasStudyKG portion of the URI in the InfoSheet");
            return null;
        }

        // Per DSG-SPEC-V5, hasStudyURI must already contain STD- prefix in InfoSheet
        // If it already starts with STD-, use as-is without adding another prefix
        String finalStudyUri;
        if (trimmedStudyUri.startsWith("STD-")) {
            finalStudyUri = studyKG + ":" + trimmedStudyUri;
        } else {
            // Legacy fallback: add STD- prefix if missing (for backward compatibility)
            finalStudyUri = studyKG + ":" + Constants.PREFIX_STUDY + "-" + trimmedStudyUri;
        }
        finalStudyUri = URIUtils.replacePrefixEx(finalStudyUri);

        finalStudyUri = finalStudyUri.replace("#/","#");

        return finalStudyUri;
    }

    private static DataFile cloneDataFileSafe(DataFile source) {
        try {
            DataFile cloned = (DataFile)source.clone();
            if (cloned != null) {
                return cloned;
            }
        } catch (Exception e) {
            // Log warning if needed, or silently fall back
        }

        // Fallback for tests or if clone fails unexpectedly
        DataFile df = new DataFile(source.getId(), source.getFilename());
        df.setUri(source.getUri());
        df.setFileStatus(source.getFileStatus());
        df.setStudyUri(source.getStudyUri());
        df.setHasSIRManagerEmail(source.getHasSIRManagerEmail());

        // Minimal copy of other fields if critical
        if (source.getLogger() != null) {
             // IngestionLogger usually created fresh in constructor or clone override
             // but here we ensure it's set
        }

        return df;
    }

    /**
     * Process DA-SOC files associated with a DSG file.
     * This method searches for DA-SOC-*.csv files in the same directory as the DSG file
     * and automatically ingests them after successful DSG ingestion.
     *
     * Uses study-based approach: passes the Study URI from DSG to each DA-SOC file.
     * The actual object discovery happens via originalID matching in AnnotateDASOC.
     *
     * @param dsgDataFile The DataFile object for the DSG file
     * @param dsgFile The physical DSG file
     * @param studyUri The Study URI from the DSG ingestion
     */
    private static void processAssociatedDASOCFiles(DataFile dsgDataFile, File dsgFile, String studyUri) {
        try {
            System.out.println("\n=== [DA-SOC AUTO-PROCESSING] Searching for DA-SOC files ===");

            // Get the directory where the DSG file is located
            File dsgDirectory = dsgFile.getParentFile();
            if (dsgDirectory == null || !dsgDirectory.exists() || !dsgDirectory.isDirectory()) {
                System.out.println("[DA-SOC AUTO-PROCESSING] No parent directory found for DSG file");
                return;
            }

            System.out.println("[DA-SOC AUTO-PROCESSING] DSG directory: " + dsgDirectory.getAbsolutePath());
            System.out.println("[DA-SOC AUTO-PROCESSING] Study URI from DSG: " + studyUri);

            // Find all DA-SOC-*.csv files in the same directory
            File[] dasocFiles = dsgDirectory.listFiles((dir, name) ->
                name.startsWith("DA-SOC-") && name.endsWith(".csv")
            );

            if (dasocFiles == null || dasocFiles.length == 0) {
                System.out.println("[DA-SOC AUTO-PROCESSING] No DA-SOC files found in directory");
                return;
            }

            System.out.println("[DA-SOC AUTO-PROCESSING] Found " + dasocFiles.length + " DA-SOC file(s)");

            // Process each DA-SOC file
            int successCount = 0;
            int failCount = 0;

            for (File dasocFile : dasocFiles) {
                System.out.println("\n[DA-SOC AUTO-PROCESSING] Processing: " + dasocFile.getName());

                try {
                    // Extract SOC name from filename (DA-SOC-{SOCNAME}.csv -> {SOCNAME})
                    String baseName = FilenameUtils.getBaseName(dasocFile.getName());
                    String socName = baseName.substring(7); // Remove "DA-SOC-" prefix

                    System.out.println("[DA-SOC AUTO-PROCESSING] Collection name from filename: " + socName);

                    // Create DA URI
                    String kbPrefix = ConfigProp.getKbPrefix();
                    if (kbPrefix == null || kbPrefix.isEmpty()) {
                        System.out.println("[DA-SOC AUTO-PROCESSING] ERROR: Could not determine KB prefix");
                        failCount++;
                        continue;
                    }

                    String daUri = kbPrefix + "DA-" + socName + "-" + System.currentTimeMillis();

                    // Create DataFile for DA-SOC
                    String dataFileId = "DFL" + System.currentTimeMillis();
                    DataFile dasocDataFile = new DataFile(dataFileId, dasocFile.getName());

                    String dataFileUri = kbPrefix + dataFileId;
                    dasocDataFile.setUri(dataFileUri);
                    dasocDataFile.setHasSIRManagerEmail(dsgDataFile.getHasSIRManagerEmail());
                    dasocDataFile.setFileStatus(DataFile.UNPROCESSED);

                    // CRITICAL: Pass Study URI from DSG to DA-SOC DataFile
                    // This allows AnnotateDASOC to use study-based approach
                    dasocDataFile.setStudyUri(studyUri);

                    // Store DA URI in DataFile metadata
                    dasocDataFile.setDasocDataAcquisitionUri(daUri);

                    System.out.println("[DA-SOC AUTO-PROCESSING] Created DataFile: " + dataFileUri);
                    System.out.println("[DA-SOC AUTO-PROCESSING] DA URI: " + daUri);
                    System.out.println("[DA-SOC AUTO-PROCESSING] Study URI: " + studyUri);

                    // Save DataFile to triplestore
                    dasocDataFile.save();

                    // Copy file to permanent location
                    String basePath = ConfigProp.getPathIngestion();
                    String targetDir = basePath + "/" + Constants.RESOURCE_FOLDER + "/" + dataFileId;
                    File targetDirectory = new File(targetDir);
                    if (!targetDirectory.exists()) {
                        targetDirectory.mkdirs();
                    }

                    File targetFile = new File(targetDir + "/" + dasocFile.getName());
                    java.nio.file.Files.copy(
                        dasocFile.toPath(),
                        targetFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                    );

                    System.out.println("[DA-SOC AUTO-PROCESSING] File copied to: " + targetFile.getAbsolutePath());

                    // Process DA-SOC using standard ingestion flow
                    // This will call AnnotateDASOC.exec() which uses study-based approach
                    try {
                        ingest(dasocDataFile, targetFile, "template.conf", null);

                        // Check if ingestion was successful
                        if (DataFile.PROCESSED.equals(dasocDataFile.getFileStatus())) {
                            System.out.println("[DA-SOC AUTO-PROCESSING] ✅ SUCCESS: " + dasocFile.getName());
                            successCount++;
                        } else {
                            System.out.println("[DA-SOC AUTO-PROCESSING] ❌ FAILED: " + dasocFile.getName() +
                                " (status: " + dasocDataFile.getFileStatus() + ")");
                            failCount++;
                        }
                    } catch (Exception e) {
                        System.err.println("[DA-SOC AUTO-PROCESSING] ❌ EXCEPTION during ingestion: " + e.getMessage());
                        e.printStackTrace();
                        dasocDataFile.setFileStatus(DataFile.ERROR);
                        dasocDataFile.save();
                        failCount++;
                    }

                } catch (Exception e) {
                    System.err.println("[DA-SOC AUTO-PROCESSING] ERROR processing " + dasocFile.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                    failCount++;
                }
            }

            // Log summary
            System.out.println("\n=== [DA-SOC AUTO-PROCESSING] Summary ===");
            System.out.println("Total DA-SOC files found: " + dasocFiles.length);
            System.out.println("Successfully processed: " + successCount);
            System.out.println("Failed: " + failCount);

            // Add summary to DSG DataFile log
            dsgDataFile.getLogger().println("\n=== DA-SOC Auto-Processing ===");
            dsgDataFile.getLogger().println("Found " + dasocFiles.length + " DA-SOC file(s) in directory");
            dsgDataFile.getLogger().println("Successfully processed: " + successCount);
            if (failCount > 0) {
                dsgDataFile.getLogger().println("Failed: " + failCount);
            }

        } catch (Exception e) {
            System.err.println("[DA-SOC AUTO-PROCESSING] ERROR: " + e.getMessage());
            e.printStackTrace();
            dsgDataFile.getLogger().printException("DA-SOC auto-processing error: " + e.getMessage());
        }
    }
}

