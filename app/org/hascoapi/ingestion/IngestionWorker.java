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
            recordFile = new CSVRecordFile(file);
        } else if (fileName.endsWith(".xlsx")) {
            recordFile = new SpreadsheetRecordFile(file,dataFile.getFilename(),"InfoSheet");
        } else {
            dataFile.getLogger().printExceptionByIdWithArgs("GBL_00003", fileName);
            System.out.println("[ERROR] IngestionWorker: invalid file extension.");
            return;
        }

        if (!recordFile.isValid()) {
            dataFile.getLogger().printExceptionById("SDD_00001");
            System.out.println("[ERROR] IngestionWorker: No InfoSheet in provided file.");
            return;
        }

        dataFile.setRecordFile(recordFile);

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
        GeneratorChain chain = getGeneratorChain(dataFile, studyUri, templateFile, status);
        if (studyUri == null || studyUri.isEmpty()) {
            chain.setStudyUri("");
        } else {
            chain.setStudyUri(studyUri);
        }

        if (chain != null) {
            System.out.println("IngestionWorker: chain.generate() STARTED.");
            bSucceed = chain.generate();
            System.out.println("IngestionWorker: chain.generate() ENDED. Response: [" + bSucceed + "]");
            chain.disposeChain();
        }

        if (bSucceed) {

            // if chain includes PVGenerator, executes PVGenerator.generateOthers()
            if (chain.getPV()) {
                PVGenerator.generateOthers(chain.getCodebookFile(), chain.getSddName(), ConfigProp.getKbPrefix());
            }

            dataFile.setFileStatus(DataFile.PROCESSED);
            dataFile.setCompletionTime(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));
            dataFile.setStudyUri(chain.getStudyUri());
            dataFile.save();

        }

        //if (dataFile.getFileStatus().equals(DataFile.PROCESSED_STD)) {
        //    System.out.println("================> REINVOKING DSG for SSD processing");
        //    System.out.println("  DataFile Status: [" + dataFile.getFileStatus() + "]");
        //    IngestionWorker.ingest(dataFile, file, templateFile, status);
        //}
    }

    public static GeneratorChain getGeneratorChain(DataFile dataFile, String studyUri, String templateFile, String status) {
        GeneratorChain chain = null;
        String fileName = FilenameUtils.getBaseName(dataFile.getFilename());

        if (fileName.startsWith("DA-")) {
            chain = AnnotateDA.exec(dataFile);

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
                System.out.println("[WARNING] NameSpaceGenerator: nameSpaceRecordFile is NULL.");
            } else if (nameSpaceRecordFile.getRecords() == null) {
                System.out.println("[WARNING] NameSpaceGenerator: nameSpaceRecordFile.getRecords() is NULL.");
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
            System.out.println("[WARNING] NameSpaceGenerator: could not find any sheet inside of Metadata Template called [hasDependencies].");
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
            System.out.println("[ERROR] IngestionWorker.annotationGen() - following error cloning dataFile: " + e.getMessage());
            return false;
        }
        String sheetName = mapCatalog.get("AnnotationStems");
        if (sheetName != null) {
            System.out.print("Extracting [AnnotationStems] sheet from spreadsheet... ");
            annotationStemRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), sheetName.replace("#",""));
            if (annotationStemRecordFile == null) {
                System.out.println("[WARNING] 'AnnotationStems' sheet is missing.");
                dataFile.getLogger().println("[WARNING] 'AnnotationStems' sheet is missing.");
                return false;
            } else if (annotationStemRecordFile.getRecords() == null) {
                System.out.println("[WARNING] annotationGen(): annotationStemRecordFile.getRecords() is NULL.");
                return false;
            }
            annotationStemDataFile.setRecordFile(annotationStemRecordFile);
        }
        sheetName = mapCatalog.get("Annotations");
        if (sheetName != null) {
            System.out.print("Extracting [Annotations] sheet from spreadsheet... ");
            annotationRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), sheetName.replace("#",""));
            if (annotationRecordFile == null) {
                System.out.println("[WARNING] 'Annotations' sheet is missing.");
                dataFile.getLogger().println("[WARNING] 'Annotations' sheet is missing.");
                return false;
            } else if (annotationRecordFile.getRecords() == null) {
                System.out.println("[WARNING] annotationGen(): annotationRecordFile.getRecords() is NULL.");
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
            System.out.println("[ERROR] IngestionWorker.messageGen() - following error cloning dataFile: " + e.getMessage());
            return false;
        }
        String sheetName = mapCatalog.get("MessageStream");
        if (sheetName != null) {
            System.out.print("Extracting [MessageStream] sheet from spreadsheet... ");
            messageStreamRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), sheetName.replace("#",""));
            if (messageStreamRecordFile == null) {
                System.out.println("[WARNING] 'MessageStream' sheet is missing.");
                dataFile.getLogger().println("[WARNING] 'MessageStream' sheet is missing.");
                return false;
            } else if (messageStreamRecordFile.getRecords() == null) {
                System.out.println("[WARNING] messageGen(): MessageStreamRecordFile.getRecords() is NULL.");
                return false;
            }
            messageStreamDataFile.setRecordFile(messageStreamRecordFile);
        }
        sheetName = mapCatalog.get("MessageTopic");
        if (mapCatalog.get("MessageTopic") != null) {
            System.out.print("Extracting [MessageTopic] sheet from spreadsheet... ");
            messageTopicRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), sheetName.replace("#",""));
            if (messageTopicRecordFile == null) {
                System.out.println("[WARNING] 'MessageTopic' sheet is missing.");
                dataFile.getLogger().println("[WARNING] 'MessageTopic' sheet is missing.");
                return false;
            } else if (messageTopicRecordFile.getRecords() == null) {
                System.out.println("[WARNING] messageGen(): messageTopicRecordFile.getRecords() is NULL.");
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
            System.out.println("[ERROR] IngestionWorker.messageGen() - following error cloning dataFile: " + e.getMessage());
            return false;
        }
        String sheetName = mapCatalog.get("Instruments");
        if (sheetName != null) {
            System.out.print("Extracting [Instruments] sheet from spreadsheet... ");
            instrumentsRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), sheetName.replace("#",""));
            if (instrumentsRecordFile == null) {
                System.out.println("[WARNING] 'Instruments' sheet is missing.");
                dataFile.getLogger().println("[WARNING] 'Instruments' sheet is missing.");
                return false;
            } else if (instrumentsRecordFile.getRecords() == null) {
                System.out.println("[WARNING] deployInstancesGen(): instrumentsRecordFile.getRecords() is NULL.");
                return false;
            }
            instrumentsDataFile.setRecordFile(instrumentsRecordFile);
        }
        sheetName = mapCatalog.get("Detectors");
        if (sheetName != null) {
            System.out.print("Extracting [Detectors] sheet from spreadsheet... ");
            detectorsRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), sheetName.replace("#",""));
            if (detectorsRecordFile == null) {
                System.out.println("[WARNING] 'Detectors' sheet is missing.");
                dataFile.getLogger().println("[WARNING] 'Detectors' sheet is missing.");
                return false;
            } else if (detectorsRecordFile.getRecords() == null) {
                System.out.println("[WARNING] deployInstancesGen(): detectorsRecordFile.getRecords() is NULL.");
                return false;
            }
            detectorsDataFile.setRecordFile(detectorsRecordFile);
        }
        sheetName = mapCatalog.get("SensingPerspective");
        if (sheetName != null) {
            System.out.print("Extracting [SensingPerspective] sheet from spreadsheet... ");
            sensingPerspectiveRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), sheetName.replace("#",""));
            if (sensingPerspectiveRecordFile == null) {
                System.out.println("[WARNING] 'SensingPerspective' sheet is missing.");
                dataFile.getLogger().println("[WARNING] 'SensingPerspective' sheet is missing.");
                return false;
            } else if (sensingPerspectiveRecordFile.getRecords() == null) {
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
            System.out.println("Failed to extract instruments and/or detectors and/or sensingPerspective sheets. ");
        }
        return isSuccess;
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
            System.out.println("IngestionWorker: failed to build studyUri - missing hasStudyURI portion of the URI in the InfoSheet");
            return null;
        }

        if (studyKG.equals("")) {
            System.out.println("IngestionWorker: failed to build studyUri - missing hasStudyKG portion of the URI in the InfoSheet");
            return null;
        }

        String finalStudyUri = studyKG + ":" + Constants.PREFIX_STUDY + "-" + studyUri;
        finalStudyUri = URIUtils.replacePrefixEx(finalStudyUri);

        finalStudyUri = finalStudyUri.replace("#/","#");

        return finalStudyUri;
    }
}
