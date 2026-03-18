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


public class IngestionWorker {

    public static void ingest(DataFile dataFile, File file, String templateFile, String status) {

        System.out.println("Processing file with filename: " + dataFile.getFilename());
        System.out.println("Processing file with Datafile URI: " + dataFile.getUri());
        System.out.println("Processing file with status: " + dataFile.getFileStatus());

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
            if (studyUri == "" || studyUri == null) {
                studyUri = dataFile.getUri().replace(Constants.PREFIX_DATAFILE, Constants.PREFIX_STUDY);
            }

            System.out.println("IngestionWorker: studyUri is [" + studyUri + "]");

        }

        boolean bSucceed = false;
        System.out.println("\n=== About to call getGeneratorChain() ===");
        System.out.println("DataFile filename: " + dataFile.getFilename());
        System.out.println("StudyUri: " + (studyUri != null ? studyUri : "NULL"));
        
        GeneratorChain chain = getGeneratorChain(dataFile, studyUri, templateFile, status);
        
        System.out.println("\n=== After getGeneratorChain() ===");
        System.out.println("Chain is: " + (chain != null ? "NOT NULL" : "NULL"));
        if (chain != null) {
            System.out.println("Chain is valid: " + chain.isValid());
        
            if (studyUri == null || studyUri.isEmpty()) {
                chain.setStudyUri("");
            } else {
                chain.setStudyUri(studyUri);
            }
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

        } else if (fileName.startsWith("DOI-")) {
            chain = AnnotateDOI.exec(dataFile);

        } else {
            dataFile.getLogger().printExceptionById("GBL_00001");
            return null;
        }

        return chain;
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
            //System.out.println("[ERROR] IngestionWorker.annotationGen() - following error cloning dataFile: " + e.getMessage());
            return false;
        }
        String sheetName = mapCatalog.get("AnnotationStems");
        if (sheetName != null) {
            System.out.print("Extracting [AnnotationStems] sheet from spreadsheet... ");
            annotationStemRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), sheetName.replace("#",""));
            if (annotationStemRecordFile == null) {
                dataFile.getLogger().printWarningByIdWithArgs("GBL_00013",sheetName);
                //System.out.println("[WARNING] 'AnnotationStems' sheet is missing.");
                //dataFile.getLogger().println("[WARNING] 'AnnotationStems' sheet is missing.");
                return false;
            } else if (annotationStemRecordFile.getRecords() == null) {
                dataFile.getLogger().printWarningByIdWithArgs("GBL_00014","annotationGen(): annotationStems");
                //System.out.println("[WARNING] annotationGen(): annotationStemRecordFile.getRecords() is NULL.");
                return false;
            }
            annotationStemDataFile.setRecordFile(annotationStemRecordFile);
        }
        sheetName = mapCatalog.get("Annotations");
        if (sheetName != null) {
            System.out.print("Extracting [Annotations] sheet from spreadsheet... ");
            annotationRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), sheetName.replace("#",""));
            if (annotationRecordFile == null) {
                dataFile.getLogger().printWarningByIdWithArgs("GBL_00015",sheetName);
                /*
                System.out.println("[WARNING] 'Annotations' sheet is missing.");
                dataFile.getLogger().println("[WARNING] 'Annotations' sheet is missing.");

                 */
                return false;
            } else if (annotationRecordFile.getRecords() == null) {
                dataFile.getLogger().printWarningByIdWithArgs("GBL_00014","annotationGen(): annotations");
               // System.out.println("[WARNING] annotationGen(): annotationRecordFile.getRecords() is NULL.");
                return false;
            }
            annotationDataFile.setRecordFile(annotationRecordFile);
        }

        INSGenerator annotationStemGen = new INSGenerator("annotationstem",annotationStemDataFile, status);
        annotationStemGen.setNamedGraphUri(dataFile.getUri());
        INSGenerator annotationGen = new INSGenerator("annotation",annotationDataFile, status);
        annotationGen.setNamedGraphUri(dataFile.getUri());

        GeneratorChain chain = new GeneratorChain();
        chain.setNamedGraphUri(dataFile.getUri());
        chain.addGenerator(annotationStemGen);
        chain.addGenerator(annotationGen);
        boolean isSuccess = false;
        if (chain != null) {
            isSuccess = chain.generate();
        }
        if (isSuccess) {
            System.out.println("Done extracting annotationStem and annotation sheets. ");
        } else {
            dataFile.getLogger().printWarningByIdWithArgs("GBL_00016","annotationStem and/or annotation");
            System.out.println("Failed to extract annotationStem and/or annotation sheets. ");
        }
        return isSuccess;
    }

    public static boolean messageGen(DataFile dataFile, Map<String, String> mapCatalog, String templateFile) {
        RecordFile messageStreamRecordFile = null;
        RecordFile messageTopicRecordFile = null;
        DataFile messageStreamDataFile;
        DataFile messageTopicDataFile;
        try {
            messageStreamDataFile = (DataFile)dataFile.clone();
            messageTopicDataFile = (DataFile)dataFile.clone();
        } catch (Exception e) {
            dataFile.getLogger().printExceptionByIdWithArgs("GBL_00012", e.getMessage());
            //System.out.println("[ERROR] IngestionWorker.messageGen() - following error cloning dataFile: " + e.getMessage());
            return false;
        }
        String sheetName = mapCatalog.get("MessageStream");
        if (sheetName != null) {
            System.out.print("Extracting [MessageStream] sheet from spreadsheet... ");
            messageStreamRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), sheetName.replace("#",""));
            if (messageStreamRecordFile == null) {
                dataFile.getLogger().printWarningByIdWithArgs("GBL_00013",sheetName);
                /*
                System.out.println("[WARNING] 'MessageStream' sheet is missing.");
                dataFile.getLogger().println("[WARNING] 'MessageStream' sheet is missing.");

                 */
                return false;
            } else if (messageStreamRecordFile.getRecords() == null) {
                dataFile.getLogger().printWarningByIdWithArgs("GBL_00014","messageGen(): messageStream");
                //System.out.println("[WARNING] messageGen(): MessageStreamRecordFile.getRecords() is NULL.");
                return false;
            }
            messageStreamDataFile.setRecordFile(messageStreamRecordFile);
        }
        sheetName = mapCatalog.get("MessageTopic");
        if (mapCatalog.get("MessageTopic") != null) {
            System.out.print("Extracting [MessageTopic] sheet from spreadsheet... ");
            messageTopicRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), sheetName.replace("#",""));
            if (messageTopicRecordFile == null) {
                dataFile.getLogger().printWarningByIdWithArgs("GBL_00015",sheetName);
                /*
                System.out.println("[WARNING] 'MessageTopic' sheet is missing.");
                dataFile.getLogger().println("[WARNING] 'MessageTopic' sheet is missing.");

                 */
                return false;
            } else if (messageTopicRecordFile.getRecords() == null) {
                dataFile.getLogger().printWarningByIdWithArgs("GBL_00014","messageGen(): message");
                //System.out.println("[WARNING] messageGen(): messageTopicRecordFile.getRecords() is NULL.");
                return false;
            }
            messageTopicDataFile.setRecordFile(messageTopicRecordFile);
        }

        DP2Generator messageStreamGen = new DP2Generator("messagestream",messageStreamDataFile);
        messageStreamGen.setNamedGraphUri(dataFile.getUri());
        DP2Generator messageTopicGen = new DP2Generator("messagetopic",messageTopicDataFile);
        messageTopicGen.setNamedGraphUri(dataFile.getUri());

        GeneratorChain chain = new GeneratorChain();
        chain.setNamedGraphUri(dataFile.getUri());
        chain.addGenerator(messageStreamGen);
        chain.addGenerator(messageTopicGen);
        boolean isSuccess = false;
        if (chain != null) {
            isSuccess = chain.generate();
        }
        if (isSuccess) {
            System.out.println("Done extracting messageStream and messageTopic sheets. ");
        } else {
            dataFile.getLogger().printWarningByIdWithArgs("GBL_00016","messageStream and/or messageTopic");
            System.out.println("Failed to extract messageStream and/or messageTopic sheets. ");
        }
        return isSuccess;
    }

    public static boolean deployInstancesGen(DataFile dataFile, Map<String, String> mapCatalog, String templateFile) {
        RecordFile instrumentsRecordFile = null;
        RecordFile detectorsRecordFile = null;
        RecordFile sensingPerspectiveRecordFile = null;
        DataFile instrumentsDataFile;
        DataFile detectorsDataFile;
        DataFile sensingPerspectiveDataFile;
        try {
            instrumentsDataFile = (DataFile)dataFile.clone();
            detectorsDataFile = (DataFile)dataFile.clone();
            sensingPerspectiveDataFile = (DataFile)dataFile.clone();
        } catch (Exception e) {
            dataFile.getLogger().printExceptionByIdWithArgs("GBL_00012",e.getMessage());
           // System.out.println("[ERROR] IngestionWorker.messageGen() - following error cloning dataFile: " + e.getMessage());
            return false;
        }
        String sheetName = mapCatalog.get("Instruments");
        if (sheetName != null) {
            System.out.print("Extracting [Instruments] sheet from spreadsheet... ");
            instrumentsRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), sheetName.replace("#",""));
            if (instrumentsRecordFile == null) {
                dataFile.getLogger().printWarningByIdWithArgs("GBL_00014",sheetName);
                /*
                System.out.println("[WARNING] 'Instruments' sheet is missing.");
                dataFile.getLogger().println("[WARNING] 'Instruments' sheet is missing.");

                 */
                return false;
            } else if (instrumentsRecordFile.getRecords() == null) {
                dataFile.getLogger().printWarningByIdWithArgs("GBL_00014","deployInstancesGen(): instruments");
                //System.out.println("[WARNING] deployInstancesGen(): instrumentsRecordFile.getRecords() is NULL.");
                return false;
            }
            instrumentsDataFile.setRecordFile(instrumentsRecordFile);
        }
        sheetName = mapCatalog.get("Detectors");
        if (sheetName != null) {
            System.out.print("Extracting [Detectors] sheet from spreadsheet... ");
            detectorsRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), sheetName.replace("#",""));
            if (detectorsRecordFile == null) {
                dataFile.getLogger().printWarningByIdWithArgs("GBL_00015",sheetName);
                /*
                System.out.println("[WARNING] 'Detectors' sheet is missing.");
                dataFile.getLogger().println("[WARNING] 'Detectors' sheet is missing.");

                 */
                return false;
            } else if (detectorsRecordFile.getRecords() == null) {
                dataFile.getLogger().printWarningByIdWithArgs("GBL_00014","deployInstancesGen(): detectors");
                // System.out.println("[WARNING] deployInstancesGen(): detectorsRecordFile.getRecords() is NULL.");
                return false;
            }
            detectorsDataFile.setRecordFile(detectorsRecordFile);
        }
        sheetName = mapCatalog.get("SensingPerspective");
        if (sheetName != null) {
            System.out.print("Extracting [SensingPerspective] sheet from spreadsheet... ");
            sensingPerspectiveRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), sheetName.replace("#",""));
            if (sensingPerspectiveRecordFile == null) {
                dataFile.getLogger().printWarningByIdWithArgs("GBL_00015",sheetName);
                /*
                System.out.println("[WARNING] 'SensingPerspective' sheet is missing.");
                dataFile.getLogger().println("[WARNING] 'SensingPerspective' sheet is missing.");

                 */
                return false;
            } else if (sensingPerspectiveRecordFile.getRecords() == null) {
                dataFile.getLogger().printWarningByIdWithArgs("GBL_00014"," deployInstancesGen(): sensingPerspective");
                System.out.println("[WARNING] deployInstancesGen(): sensingPerspectiveRecordFile.getRecords() is NULL.");
                return false;
            }
            sensingPerspectiveDataFile.setRecordFile(sensingPerspectiveRecordFile);
        }

        DP2Generator instrumentsGen = new DP2Generator("instrumentinstance",instrumentsDataFile);
        instrumentsGen.setNamedGraphUri(dataFile.getUri());
        DP2Generator detectorsGen = new DP2Generator("detectorinstance",detectorsDataFile);
        detectorsGen.setNamedGraphUri(dataFile.getUri());
        DP2Generator sensingPerspectiveGen = new DP2Generator("sensingperspective",sensingPerspectiveDataFile);
        sensingPerspectiveGen.setNamedGraphUri(dataFile.getUri());

        GeneratorChain chain = new GeneratorChain();
        chain.setNamedGraphUri(dataFile.getUri());
        chain.addGenerator(instrumentsGen);
        chain.addGenerator(detectorsGen);
        chain.addGenerator(sensingPerspectiveGen);
        boolean isSuccess = false;
        if (chain != null) {
            isSuccess = chain.generate();
        }
        if (isSuccess) {
            System.out.println("Done extracting instruments, detectors and sensingPerspective sheets. ");
        } else {
            dataFile.getLogger().printWarningByIdWithArgs("GBL_00016","instruments and/or detectors and/or sensingPerspective");
            System.out.println("Failed to extract instruments and/or detectors and/or sensingPerspective sheets. ");
        }
        return isSuccess;
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

        if (studyUri.equals("")) {
            dataFile.getLogger().printWarningById("GBL_00017");
            System.out.println("IngestionWorker: failed to build studyUri - missing hasStudyURI portion of the URI in the InfoSheet");
            return null;
        }

        if (studyKG.equals("")) {
            dataFile.getLogger().printWarningById("GBL_00018");
            System.out.println("IngestionWorker: failed to build studyUri - missing hasStudyKG portion of the URI in the InfoSheet");
            return null;
        }

        String finalStudyUri = studyKG + ":" + Constants.PREFIX_STUDY + "-" + studyUri;
        finalStudyUri = URIUtils.replacePrefixEx(finalStudyUri);

        finalStudyUri = finalStudyUri.replace("#/","#");

        return finalStudyUri;
    }
}
