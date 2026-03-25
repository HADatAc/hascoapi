package org.hascoapi.ingestion;

import java.lang.String;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.events.Namespace;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.hascoapi.entity.pojo.Stream;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.DOI;
import org.hascoapi.entity.pojo.DP2;
//import org.hascoapi.entity.pojo.DPL;
import org.hascoapi.entity.pojo.SDD;
import org.hascoapi.entity.pojo.SDDAttribute;
import org.hascoapi.entity.pojo.SDDObject;
import org.hascoapi.entity.pojo.SSDSheet;
import org.hascoapi.entity.pojo.Study;
import org.hascoapi.entity.pojo.StudyObjectCollection;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.ConfigProp;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.vocabularies.VSTOI;


public class IngestionWorker {

    public static void ingest(DataFile dataFile, File file, String templateFile, String status) {

        System.out.println("Processing file with filename: " + dataFile.getFilename());
        System.out.println("Processing file with Datafile URI: " + dataFile.getUri());
        System.out.println("Processing file with status: " + dataFile.getFileStatus());

        // DP2 status rule: if the API didn't provide a status, default to DRAFT for DP2 ingestion.
        final String fileNameForRule = (dataFile.getFilename() == null ? "" : dataFile.getFilename());
        String effectiveStatus = status;
        if (fileNameForRule.startsWith("DP2-") || fileNameForRule.contains("/DP2-") || fileNameForRule.contains("\\DP2-")) {
            if (effectiveStatus == null || effectiveStatus.trim().isEmpty()) {
                effectiveStatus = VSTOI.DRAFT;
            }
            System.out.println("[DP2 STATUS] providedStatus=" + status + " effectiveStatus=" + effectiveStatus);
        }

        dataFile.setLastProcessTime(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));
        dataFile.getLogger().resetLog();
        dataFile.save();

        String fileName = dataFile.getFilename();

        dataFile.getLogger().println(String.format("Processing file: %s", fileName));

        // file is rejected if it has an invalid extension
        RecordFile recordFile = null;
        if (fileName.endsWith(".csv")) {
            System.out.println("IngestionWorker: Creating CSVRecordFile for: " + fileName);
            System.out.println("IngestionWorker: File object passed to CSVRecordFile - Path: " + (file != null ? file.getAbsolutePath() : "NULL"));
            System.out.println("IngestionWorker: File exists: " + (file != null && file.exists()));
            System.out.println("IngestionWorker: File readable: " + (file != null && file.canRead()));
            System.out.println("IngestionWorker: File size: " + (file != null && file.exists() ? file.length() + " bytes" : "N/A"));
            recordFile = new CSVRecordFile(file);
            System.out.println("IngestionWorker: CSVRecordFile created, checking validity...");
        } else if (fileName.endsWith(".xlsx")) {
            recordFile = new SpreadsheetRecordFile(file,dataFile.getFilename(),"InfoSheet");
        } else {
            dataFile.getLogger().printExceptionByIdWithArgs("GBL_00003", fileName);
           // System.out.println("[ERROR] IngestionWorker: invalid file extension.");
            return;
        }

        System.out.println("\n=== [INGESTION PATH] IngestionWorker.ingest() ===");
        System.out.println("[INGESTION PATH] DataFile: " + dataFile.getFilename());
        System.out.println("[INGESTION PATH] DataFile URI: " + dataFile.getUri());
        System.out.println("[INGESTION PATH] RecordFile isValid: " + (recordFile != null ? recordFile.isValid() : "recordFile is NULL"));
        
        if (!recordFile.isValid()) {
            System.out.println("[ERROR] RecordFile.isValid() returned FALSE - aborting ingestion");
            dataFile.getLogger().printExceptionById("GBL_00005");
            //System.out.println("[ERROR] IngestionWorker: No InfoSheet in provided file.");
            return;
        }
        
        System.out.println("✅ RecordFile is valid, continuing with ingestion");

        dataFile.setRecordFile(recordFile);

        // Auto-detect SOC for DASOC files (DA-SOC-* pattern)
        if (FilenameUtils.getBaseName(fileName).startsWith("DA-SOC-")) {
            String baseName = FilenameUtils.getBaseName(fileName);
            String socName = baseName.substring(7); // "DA-SOC-ENTERPRISE" -> "ENTERPRISE"
            
            System.out.println("[INGESTION PATH] Detected DASOC file pattern: DA-SOC-*");
            System.out.println("[INGESTION PATH] Extracting SOC name: " + socName);
            dataFile.getLogger().println("DASOC file detected - SOC name from filename: " + socName);
            
            // Try to find SOC by querying for objects with matching originalID pattern
            String socUri = findSOCByName(socName);
            if (socUri != null && !socUri.isEmpty()) {
                dataFile.setDasocSOCUri(socUri);
                dataFile.getLogger().println("✅ Auto-detected SOC URI: " + socUri);
                System.out.println("[INGESTION PATH] Auto-detected SOC URI from triplestore: " + socUri);
            } else {
                // If not found, construct expected SOC URI using naming convention
                String kbPrefix = ConfigProp.getKbPrefix();
                if (kbPrefix != null && !kbPrefix.isEmpty()) {
                    socUri = kbPrefix + "SOC-" + socName;
                    dataFile.setDasocSOCUri(socUri);
                    dataFile.getLogger().println("⚠️  SOC not found in triplestore, using convention-based URI: " + socUri);
                    System.out.println("[INGESTION PATH] Using convention-based SOC URI: " + socUri);
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

            System.out.println("IngestionWorker: studyUri is [" + studyUri + "]");

        }

        boolean bSucceed = false;
        System.out.println("\n=== About to call getGeneratorChain() ===");
        System.out.println("DataFile filename: " + dataFile.getFilename());
        System.out.println("StudyUri: " + (studyUri != null ? studyUri : "NULL"));

        GeneratorChain chain = getGeneratorChain(dataFile, studyUri, templateFile, effectiveStatus);

        System.out.println("\n=== After getGeneratorChain() ===");
        System.out.println("Chain is: " + (chain != null ? "NOT NULL" : "NULL"));

        // If no chain was produced, log and throw exception to fail fast (as requested)
        if (chain == null) {
            String msg = "IngestionWorker: No generator chain produced. Aborting ingestion gracefully.";
            dataFile.getLogger().println(msg);
            System.out.println(msg);
            throw new RuntimeException(msg);
        }

        System.out.println("Chain is valid: " + chain.isValid());

        // Only set study URI if a chain was produced
        if (studyUri == null || studyUri.isEmpty()) {
            chain.setStudyUri("");
        } else {
            chain.setStudyUri(studyUri);
        }

        if (chain != null) {
            try {
                System.out.println("IngestionWorker: chain.generate() STARTED.");
                bSucceed = chain.generate();
                System.out.println("IngestionWorker: chain.generate() ENDED. Response: [" + bSucceed + "]");
                chain.disposeChain();
            } catch (Exception e) {
                System.out.println("IngestionWorker: ERROR during chain.generate()");
                e.printStackTrace();
                dataFile.getLogger().println("ERROR during ingestion: " + e.getMessage());
                bSucceed = false;
            }
        }

        if (bSucceed) {

            try {
                // if chain includes PVGenerator, executes PVGenerator.generateOthers()
                if (chain.getPV()) {
                    PVGenerator.generateOthers(chain.getCodebookFile(), chain.getSddName(), ConfigProp.getKbPrefix());
                }

                dataFile.setFileStatus(DataFile.PROCESSED);
                dataFile.setCompletionTime(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));
                dataFile.setStudyUri(chain.getStudyUri());
                dataFile.save();
                System.out.println("IngestionWorker: DataFile status set to PROCESSED");
            } catch (Exception e) {
                System.out.println("IngestionWorker: ERROR during finalization");
                e.printStackTrace();
            }

        } else {
            System.out.println("IngestionWorker: Ingestion FAILED. DataFile status remains: " + dataFile.getFileStatus());
        }

        //if (dataFile.getFileStatus().equals(DataFile.PROCESSED_STD)) {
        //    System.out.println("================> REINVOKING DSG for SSD processing");
        //    System.out.println("  DataFile Status: [" + dataFile.getFileStatus() + "]");
        //    IngestionWorker.ingest(dataFile, file, templateFile, status);
        //}
    }

    public static GeneratorChain getGeneratorChain(DataFile dataFile, String studyUri, String templateFile, String status) {
        System.out.println("\n=== [INGESTION PATH] IngestionWorker.getGeneratorChain() ===");
        System.out.println("[INGESTION PATH] Determining generator for file: " + dataFile.getFilename());
        System.out.println("[INGESTION PATH] Full filename: " + dataFile.getFilename());
        
        GeneratorChain chain = null;
        String fileName = FilenameUtils.getBaseName(dataFile.getFilename());
        System.out.println("[INGESTION PATH] Base filename (without extension): " + fileName);
        System.out.println("[INGESTION PATH] Checking if starts with 'DA-SOC-': " + fileName.startsWith("DA-SOC-"));

        // Check for DA-SOC files BEFORE general DA files
        // DA-SOC files are DASOC (Data Acquisition - Study Object Collection) links
        // They extend SOC content with properties that don't fit in DSG files
        if (fileName.startsWith("DA-SOC-")) {
            System.out.println("[INGESTION PATH] ✅ Matched DA-SOC-* pattern, routing to AnnotateDASOC");
            System.out.println("[INGESTION PATH] Calling AnnotateDASOC.exec(dataFile)");
            dataFile.getLogger().println("Processing as DASOC (Data Acquisition - Study Object Collection)");
            chain = AnnotateDASOC.exec(dataFile);

        } else if (fileName.startsWith("DA-")) {
            // REJECT: General DA ingestion is not working at this time
            // NOTE: DA-SOC is not yet an official element type in hascoapi
            // Only DA-SOC-* files are supported through AnnotateDASOC
            System.out.println("IngestionWorker: ERROR - General DA ingestion not supported");
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
                System.out.println("IngestionWorker: verifying SSD referenced sheets before annotation.");
                if (!verifySheetsInSSD(dataFile)) {
                    dataFile.getLogger().printExceptionById("DSG_00022");
                    System.out.println("IngestionWorker: SSD verification failed. Aborting SSD annotation.");
                    return null;
                }
                chain = AnnotateSSD.exec(dataFile, studyUri, templateFile, status);
            }

        } else if (fileName.startsWith("DP2-")) {
            chain = AnnotateDP2.exec(dataFile, templateFile, status);

        } else if (fileName.startsWith("INS-")) {
            chain = AnnotateINS.exec(dataFile, templateFile, status);

        } else if (fileName.startsWith("KGR-")) {
            chain = AnnotateKGR.exec(dataFile, templateFile, status);

        } else if (fileName.startsWith("STR-")) {
            chain = AnnotateSTR.exec(dataFile, templateFile);

        } else if (fileName.startsWith("SDD-")) {
            chain = AnnotateSDD.exec(dataFile, templateFile);

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

        while (System.currentTimeMillis() < deadline) {
            attempt++;
            try {
                Study s = Study.find(studyUri);
                if (s != null) {
                    System.out.println("IngestionWorker: Study visible after STD. attempts=" + attempt + " uri=" + studyUri);
                    return true;
                }
            } catch (Exception e) {
                // ignore and retry; transient Fuseki hiccups can happen right after startup
            }

            System.out.println("IngestionWorker: waiting for Study visibility (attempt " + attempt + ") uri=" + studyUri + " sleepMs=" + sleepMs);
            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return false;
            }
            sleepMs = Math.min(1000, sleepMs * 2);
        }

        System.out.println("IngestionWorker: TIMEOUT waiting for Study visibility. uri=" + studyUri + " timeoutMs=" + timeoutMs);
        return false;
    }

    // Java
    private static boolean verifySheetsInSSD(DataFile dataFile) {
        System.out.println("SSD verification: starting referenced sheets check.");
        SpreadsheetRecordFile ssdSheet = new SpreadsheetRecordFile(
                dataFile.getFile(), dataFile.getFilename(), "SSD");
        if (ssdSheet == null || !ssdSheet.isValid() || ssdSheet.getRecords() == null || ssdSheet.getRecords().isEmpty()) {
            dataFile.getLogger().printExceptionByIdWithArgs("GBL_00014", "SSD verification: SSD sheet");
            System.out.println("SSD verification: sheet 'SSD' not found or empty.");
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
                System.out.println("SSD verification: referenced sheet '" + referencedSheet + "' does not exist or is unreadable. Aborting.");
                return false; // stop immediately on first missing/unreadable sheet
            }
        }

        System.out.println("SSD verification: completed. Status: OK");
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
        String sheetName = mapCatalog.get("hasDependencies");
        if (sheetName != null) {
            System.out.print("Extracting NameSpace sheet from spreadsheet... ");
            nameSpaceRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), sheetName.replace("#",""));
            if (nameSpaceRecordFile == null) {
                dataFile.getLogger().printWarning("GBL_00009");
                //System.out.println("[WARNING] NameSpaceGenerator: nameSpaceRecordFile is NULL.");
            } else if (nameSpaceRecordFile.getRecords() == null) {
                dataFile.getLogger().printWarning("GBL_00010");
                //System.out.println("[WARNING] NameSpaceGenerator: nameSpaceRecordFile.getRecords() is NULL.");
            } else {
                System.out.println("nameSpaceRecordFile has [" + nameSpaceRecordFile.getRecords().size() + "] rows");
                dataFile.setRecordFile(nameSpaceRecordFile);

                GeneratorChain chain = new GeneratorChain();
                chain.setNamedGraphUri(dataFile.getUri());
                chain.addGenerator(new NameSpaceGenerator(dataFile,templateFile));
                boolean isSuccess = false;
                if (chain != null) {
                    isSuccess = chain.generate();
                }
                if (isSuccess) {
                    System.out.println("Done extracting NameSpace sheet. ");
                } else {
                    System.out.println("Failed to extract NameSpace sheet. ");
                }
                return isSuccess;
            }
        } else {
            dataFile.getLogger().printWarning("GBL_00011");
            //System.out.println("[WARNING] NameSpaceGenerator: could not find any sheet inside of Metadata Template called [hasDependencies].");
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
            System.out.print("Extracting [AnnotationStems] sheet from spreadsheet... ");
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
            System.out.print("Extracting [Annotations] sheet from spreadsheet... ");
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

        if (isSuccess) {
            System.out.println("Done extracting annotationStem and annotation sheets. ");
        } else {
            dataFile.getLogger().printWarningByIdWithArgs("GBL_00016", "annotationStem and/or annotation");
            System.out.println("Failed to extract annotationStem and/or annotation sheets. ");
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
     * Queries triplestore for SOCs with labels or URIs containing the given name.
     * 
     * @param socName The SOC name extracted from filename (e.g., "ENTERPRISE", "API", "PRODUCT")
     * @return SOC URI if found, null otherwise
     */
    public static String findSOCByName(String socName) {
        if (socName == null || socName.isEmpty()) {
            return null;
        }
        
        try {
            String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT ?socUri WHERE { " +
                "  ?socUri a hasco:StudyObjectCollection . " +
                "  { ?socUri rdfs:label ?label . FILTER(CONTAINS(UCASE(?label), UCASE(\"" + socName + "\"))) } " +
                "  UNION " +
                "  { FILTER(CONTAINS(UCASE(STR(?socUri)), UCASE(\"SOC-" + socName + "\"))) } " +
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
                        studyKG = record.getValueByColumnIndex(1);
                    }
                }
                if (record.getValueByColumnIndex(0).equals("hasStudyURI")) {
                    if (record.getValueByColumnIndex(1) != null){
                        studyUri = record.getValueByColumnIndex(1);
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

        String finalStudyUri = studyKG + ":" + Constants.PREFIX_STUDY + "-" + trimmedStudyUri;
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
}
