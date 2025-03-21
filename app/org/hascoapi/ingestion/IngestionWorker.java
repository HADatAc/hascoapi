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
        System.out.println("Processing file with URI: " + dataFile.getUri());

        String studyUri = "";
        if (dataFile.getFilename().contains("DSG-")) {
            studyUri = dataFile.getUri().replace("DF","ST");
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
            System.out.println("[ERROR] IngestionWorker: invalid file extension.");
            return;
        }

        if (!recordFile.isValid()) {
            dataFile.getLogger().printExceptionById("SDD_00001");
            System.out.println("[ERROR] IngestionWorker: No InfoSheet in provided file.");
            return;
        }

        dataFile.setRecordFile(recordFile);

        boolean bSucceed = false;
        GeneratorChain chain = getGeneratorChain(dataFile, templateFile, status);
        if (studyUri == null || studyUri.isEmpty()) {
            chain.setStudyUri("");
        } else {
            chain.setStudyUri(studyUri);
        }

        if (chain != null) {
            System.out.println("IngestionWorker: chain.generate() STARTED.");
            bSucceed = chain.generate();
            System.out.println("IngestionWorker: chain.generate() ENDED. Response: [" + bSucceed + "]");
        }

        if (bSucceed) {

            // if chain includes PVGenerator, executes PVGenerator.generateOthers()
            if (chain.getPV()) {
                PVGenerator.generateOthers(chain.getCodebookFile(), chain.getSddName(), ConfigProp.getKbPrefix());
            }

            if (dataFile.getFileStatus().equals(DataFile.WORKING_STD)) {
                dataFile.setFileStatus(DataFile.PROCESSED_STD);
            } else {
                dataFile.setFileStatus(DataFile.PROCESSED);
            }
            dataFile.setCompletionTime(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));
            dataFile.setStudyUri(chain.getStudyUri());
            dataFile.save();

        }

        if (dataFile.getFileStatus().equals(DataFile.PROCESSED_STD)) {
            System.out.println("================> REINVOKING DSG for SSD processing");
            System.out.println("  DataFile Status: [" + dataFile.getFileStatus() + "]");
            IngestionWorker.ingest(dataFile, file, templateFile, status);
        }
    }

    public static GeneratorChain getGeneratorChain(DataFile dataFile, String templateFile, String status) {
        GeneratorChain chain = null;
        String fileName = FilenameUtils.getBaseName(dataFile.getFilename());

        //if (fileName.startsWith("DA-")) {
        //    chain = annotateDAFile(dataFile);
        //
        //} else

        if (fileName.startsWith("DSG-") &&
            dataFile.getFileStatus().equals(DataFile.WORKING_STD)) {
            chain = annotateSTDFile(dataFile, templateFile);

        } else if (fileName.startsWith("DSG-") &&
            dataFile.getFileStatus().equals(DataFile.PROCESSED_STD)) {
            chain = annotateSSDFile(dataFile, templateFile);

        } else if (fileName.startsWith("DP2-")) {
            chain = annotateDP2File(dataFile, templateFile);

        } else if (fileName.startsWith("INS-")) {
            chain = AnnotateINS.exec(dataFile, templateFile, status);

        } else if (fileName.startsWith("KGR-")) {
            chain = AnnotateKGR.exec(dataFile, templateFile, status);

        } else if (fileName.startsWith("STR-")) {
            chain = AnnotateSTR.exec(dataFile, templateFile);

        } else if (fileName.startsWith("SDD-")) {
            chain = AnnotateSDD.exec(dataFile, templateFile);

        } else if (fileName.startsWith("DOI-")) {
            chain = annotateDOIFile(dataFile);

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

    /*===========================================================================================*
     *                                  METADATA TEMPLATE ANNOTATORS                             *
     *===========================================================================================*/

    /****************************
     *    DSG                   *
     ****************************/

    public static GeneratorChain annotateSTDFile(DataFile dataFile, String templateFile) {

        Map<String, String> mapCatalog = new HashMap<String, String>();
        for (Record record : dataFile.getRecordFile().getRecords()) {
            mapCatalog.put(record.getValueByColumnIndex(0), record.getValueByColumnIndex(1));
            //System.out.println(record.getValueByColumnIndex(0) + ":" + record.getValueByColumnIndex(1));
        }

        if (dataFile.getFilename().endsWith(".xlsx")) {
            nameSpaceGen(dataFile, mapCatalog, templateFile);
        } else {
            System.out.println("[ERROR] StudyGenerator: DSG file needs to have suffix [.xlsx].");
            return null;
        }

        GeneratorChain chain = new GeneratorChain();
        chain.setNamedGraphUri(dataFile.getUri());
        chain.addGenerator(new NameSpaceGenerator(dataFile,templateFile));

        RecordFile studyRecordFile = null;

        if (mapCatalog.get("hasStudyDescription") != null) {
            System.out.print("Extracting STD sheet from spreadsheet... ");
            studyRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), mapCatalog.get("hasStudyDescription"));
            if (studyRecordFile == null) {
                System.out.println("[ERROR] StudyGenerator: studyRecordFile is NULL.");
                return null;
            } else if (studyRecordFile.getRecords() == null) {
                System.out.println("[ERROR] StudyGenerator: studyRecordFile.getRecords() is NULL.");
                return null;
            } else{
                System.out.println("studyRecordFile has [" + studyRecordFile.getRecords().size() + "] rows");
            }
            dataFile.setRecordFile(studyRecordFile);
            System.out.print("Done extracting STD sheet. ");
        } else {
            System.out.println("[ERROR] StudyGenerator: could not find any sheet inside of DSG called [hasStudyDescription].");
            return null;
        }

        chain.addGenerator(new AgentGenerator(dataFile,null,templateFile));
        chain.addGenerator(new StudyGenerator(dataFile,null,templateFile));

        return chain;
    }

    /****************************
     *    SSD                   *
     ****************************/

     public static GeneratorChain annotateSSDFile(DataFile dataFile, String templateFile) {
        String studyUri = dataFile.getUri().replaceAll("DF", "ST");
        System.out.println("Processing SSD file of " + studyUri + "...");

        Map<String, String> mapCatalog = new HashMap<String, String>();
        for (Record record : dataFile.getRecordFile().getRecords()) {
            mapCatalog.put(record.getValueByColumnIndex(0), record.getValueByColumnIndex(1));
            //System.out.println(record.getValueByColumnIndex(0) + ":" + record.getValueByColumnIndex(1));
        }

        RecordFile ssdRecordFile = null;

        if (dataFile.getFilename().endsWith(".xlsx")) {

            if (mapCatalog.get("hasEntityDesign") != null) {
                System.out.print("Extracting SSD sheet from spreadsheet... ");
                ssdRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), mapCatalog.get("hasEntityDesign"));
                if (ssdRecordFile == null) {
                    System.out.println("[ERROR] IngestionWorker: ssdRecordFile is NULL.");
                    return null;
                } else if (ssdRecordFile.getRecords() == null) {
                    System.out.println("[ERROR] IngestionWorker: ssdRecordFile.getRecords() is NULL.");
                    return null;
                } else{
                    System.out.println("ssdRecordFile has [" + ssdRecordFile.getRecords().size() + "] rows");
                }
                dataFile.setRecordFile(ssdRecordFile);
                System.out.print("Done extracting SSD sheet. ");
            } else {
                System.out.println("[ERROR] IngestionWorker: could not find any sheet inside of DSG called [hasEntityDesign].");
                return null;
            }
        } else {
            System.out.println("[ERROR] IngestionWorker: DSG file needs to have suffix [.xlsx].");
            return null;
        }

        // THERE IS AN SSD SHEET, BUT IT IS EMPTY. STOP EXECUTION OF THE CHAIN WITH A WARNING
        if (ssdRecordFile.getRecords().size() < 1) {
            System.out.println("[WARNING] IngestionWorker: SSD sheet is empty.");
            dataFile.getLogger().println("[WARNING] IngestionWorker: SSD sheet is empty.");
            return new SSDGeneratorChain();
        }

        SSDSheet ssd = new SSDSheet(dataFile);
        //Map<String, String> mapCatalog = ssd.getCatalog();
        mapCatalog = ssd.getCatalog();
        Map<String, List<String>> mapContent = ssd.getMapContent();

        //RecordFile SSDsheet = new SpreadsheetRecordFile(dataFile.getFile(), "SSD");
        //dataFile.setRecordFile(SSDsheet);

        SSDGeneratorChain chain = new SSDGeneratorChain();
        chain.setNamedGraphUri(dataFile.getUri());

        Study study = null;

        //if (SSDsheet.isValid()) {
        if (ssdRecordFile.isValid()) {

            System.out.println("SSD Processing: adding VirtualColumnGenerator");
            VirtualColumnGenerator vcgen = new VirtualColumnGenerator(dataFile);
            vcgen.setStudyUri(studyUri);
            chain.addGenerator(vcgen);
            //System.out.println("added VirtualColumnGenerator for " + dataFile.getAbsolutePath());

            System.out.println("SSD Processing: adding SSDGenerator");
            SSDGenerator socgen = new SSDGenerator(dataFile);
            socgen.setStudyUri(studyUri);
            chain.addGenerator(socgen);
            //System.out.println("added SSDGenerator for " + dataFile.getAbsolutePath());

            //String studyUri = socgen.getStudyUri();
            if (studyUri == null || studyUri.isEmpty()) {
                return null;
            } else {
                chain.setStudyUri(studyUri);
                study = Study.find(studyUri);
                if (study != null) {
                    System.out.println("SSD Processing: Found study [" + study.getUri() + "]");
                    dataFile.getLogger().println("SSD ingestion: The study uri :" + studyUri + " is in the TripleStore.");
                    socgen.setStudyUri(studyUri);
                } else {
                    dataFile.getLogger().printExceptionByIdWithArgs("SSD_00005", studyUri);
                    return null;
                }
            }

            System.out.println("SSD Processing: SubjectGroup verification.");
            // check the rule for hasco:SubjectGroup, there should be one and only such type
            int subjectGroupCount = 0;
            for (Record record : dataFile.getRecordFile().getRecords()) {
                //String socName = record.getValueByColumnIndex(1);
                String socType = record.getValueByColumnIndex(2);
                if (socType.contains("SubjectGroup")) {
                    subjectGroupCount++;
                }
            }

            if ( subjectGroupCount == 0 ) {
                System.out.println("[ERROR] SSD Processing: NO SubjectGroup.");
                dataFile.getLogger().printExceptionById("SSD_00006");
                return null;
            }
            if ( subjectGroupCount > 1 ) {
                System.out.println("SSD Processing: More than one SubjectGroup.");
                dataFile.getLogger().printExceptionById("SSD_00007");
                return null;
            }

            chain.setNamedGraphUri(dataFile.getUri());
            chain.setDataFile(dataFile);

        } else {
            //chain.setInvalid();
            dataFile.getLogger().printException("Cannot locate SSD's sheet ");
        }

        System.out.println("IngestionWorker: pre-processing StudyObjectGenerator. Study Id is  " + study.getId());
        String study_uri = chain.getStudyUri();
        for (String i : mapCatalog.keySet()) {
            if (mapCatalog.get(i) != null && !mapCatalog.get(i).isEmpty()) {
                try {
                    System.out.println("Pre-processing SOC [" + mapCatalog.get(i) + "]");
                    RecordFile SOsheet = new SpreadsheetRecordFile(dataFile.getFile(), mapCatalog.get(i).replace("#", ""));
                    DataFile dataFileForSheet = (DataFile)dataFile.clone();
                    dataFileForSheet.setRecordFile(SOsheet);
                    if (mapContent == null || mapContent.get(i) == null) {
                        dataFile.getLogger().printException("No value for MapContent with index [" + i + "]");
                    } else {
                        System.out.println("SSD Processing: adding StudyObjectGenerator");
                        chain.addGenerator(new StudyObjectGenerator(dataFileForSheet, mapContent.get(i), mapContent, study_uri, study.getId()));
                    }
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }
        }

        System.out.println("SSD Processing: Completed GeneratorChain()");
        return chain;
    }

    /****************************
     *    DOI                   *
     ****************************/

    public static GeneratorChain annotateDOIFile(DataFile dataFile) {
        System.out.println("Processing DOI file ...");
        RecordFile recordFile = new SpreadsheetRecordFile(dataFile.getFile(), "InfoSheet");
        if (!recordFile.isValid()) {
            dataFile.getLogger().printExceptionById("DOI_00001");
            return null;
        } else {
            dataFile.setRecordFile(recordFile);
        }

        DOI doi = new DOI(dataFile);
        Map<String, String> mapCatalog = doi.getCatalog();
        GeneratorChain chain = new GeneratorChain();

        String studyId = doi.getStudyId();
        if (studyId == null || studyId.isEmpty()) {
            dataFile.getLogger().printExceptionByIdWithArgs("DOI_00002", studyId);
            return null;
        } else {
            Study study = Study.findById(studyId);
            if (study != null) {
                chain.setStudyUri(study.getUri());
                dataFile.getLogger().println("DOI ingestion: Found study id [" + studyId + "]");
            } else {
                dataFile.getLogger().printExceptionByIdWithArgs("DOI_00003", studyId);
                return null;
            }
        }

        chain.setDataFile(dataFile);

        String doiVersion = doi.getVersion();
        if (doiVersion != null && !doiVersion.isEmpty()) {
            dataFile.getLogger().println("DOI ingestion: version is [" + doiVersion + "]");
        } else {
            dataFile.getLogger().printExceptionById("DOI_00004");
            return null;
        }

        if (mapCatalog.get("Filenames") == null) {
            dataFile.getLogger().printExceptionById("DOI_00005");
            return null;
        }

        String sheetName = mapCatalog.get("Filenames").replace("#", "");
        RecordFile sheet = new SpreadsheetRecordFile(dataFile.getFile(), sheetName);

        try {
        	DataFile dataFileForSheet = (DataFile)dataFile.clone();
        	dataFileForSheet.setRecordFile(sheet);
        	chain.addGenerator(new DOIGenerator(dataFileForSheet));
        } catch (CloneNotSupportedException e) {
        	e.printStackTrace();
            dataFile.getLogger().printExceptionById("DOI_00006");
            return null;
        }

        return chain;
    }

    /****************************
     *    DP2                   *
     ****************************/

    public static GeneratorChain annotateDP2File(DataFile dataFile, String templateFile) {
        RecordFile recordFile = new SpreadsheetRecordFile(dataFile.getFile(), "InfoSheet");
        if (!recordFile.isValid()) {
            dataFile.getLogger().printExceptionById("DPL_00001");
            return null;
        } else {
            dataFile.setRecordFile(recordFile);
        }

        Map<String, String> mapCatalog = new HashMap<String, String>();
        System.out.println("InfoSheet: ");
        for (Record record : dataFile.getRecordFile().getRecords()) {
            mapCatalog.put(record.getValueByColumnIndex(0), record.getValueByColumnIndex(1));
            System.out.println("  (" + record.getValueByColumnIndex(0) + ") : " + record.getValueByColumnIndex(1));
        }

        // hasDependencies
        nameSpaceGen(dataFile, mapCatalog,templateFile);

        //MessageStream
        //MessageTopic
        messageGen(dataFile, mapCatalog,templateFile);

        //Instruments
        //Detectors
        //SensingPerspective
        deployInstancesGen(dataFile, mapCatalog, templateFile);

        // Deployments
        // PlatformModels
        // Platforms
        // FieldsOfView
        GeneratorChain chain = new GeneratorChain();
        RecordFile sheet = null;

        try {

            chain.setNamedGraphUri(dataFile.getUri());

            // PlatformModels
            String platformModelsSheet = mapCatalog.get("PlatformModels");
            if (platformModelsSheet == null) {
                System.out.println("[WARNING] 'PlatformModels' sheet is missing.");
                dataFile.getLogger().println("[WARNING] 'PlatformModels' sheet is missing.");
            } else {
                platformModelsSheet.replace("#", "");
                sheet = new SpreadsheetRecordFile(dataFile.getFile(), platformModelsSheet);
                try {
                    DataFile dataFileForSheet = (DataFile)dataFile.clone();
                    dataFileForSheet.setRecordFile(sheet);
                    DP2Generator platformModelsGen = new DP2Generator("platform",dataFileForSheet);
                    platformModelsGen.setNamedGraphUri(dataFileForSheet.getUri());
                    chain.addGenerator(platformModelsGen);
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }

            // Platforms
            String platformsSheet = mapCatalog.get("Platforms");
            if (platformsSheet == null) {
                System.out.println("[WARNING] 'Platforms' sheet is missing.");
                dataFile.getLogger().println("[WARNING] 'Platforms' sheet is missing.");
            } else {
                platformsSheet.replace("#", "");
                sheet = new SpreadsheetRecordFile(dataFile.getFile(), platformsSheet);
                try {
                    DataFile dataFileForSheet = (DataFile)dataFile.clone();
                    dataFileForSheet.setRecordFile(sheet);
                    DP2Generator platformsGen = new DP2Generator("platforminstance",dataFileForSheet);
                    platformsGen.setNamedGraphUri(dataFileForSheet.getUri());
                    chain.addGenerator(platformsGen);
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }

            // FieldsOfView
            String fieldsOfViewSheet = mapCatalog.get("FieldsOfView");
            if (fieldsOfViewSheet == null) {
                System.out.println("[WARNING] 'FieldsOfView' sheet is missing.");
                dataFile.getLogger().println("[WARNING] 'FieldsOfView' sheet is missing.");
            } else {
                fieldsOfViewSheet.replace("#", "");
                sheet = new SpreadsheetRecordFile(dataFile.getFile(), fieldsOfViewSheet);
                try {
                    DataFile dataFileForSheet = (DataFile)dataFile.clone();
                    dataFileForSheet.setRecordFile(sheet);
                    DP2Generator fieldsOfViewGen = new DP2Generator("fieldofview",dataFileForSheet);
                    fieldsOfViewGen.setNamedGraphUri(dataFileForSheet.getUri());
                    chain.addGenerator(fieldsOfViewGen);
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }

            // Deployments
            String deploymentsSheet = mapCatalog.get("Deployments");
            if (deploymentsSheet == null) {
                System.out.println("[WARNING] 'Deployments' sheet is missing.");
                dataFile.getLogger().println("[WARNING] 'Deployments' sheet is missing.");
            } else {
                deploymentsSheet.replace("#", "");
                sheet = new SpreadsheetRecordFile(dataFile.getFile(), deploymentsSheet);
                try {
                    DataFile dataFileForSheet = (DataFile)dataFile.clone();
                    dataFileForSheet.setRecordFile(sheet);
                    DP2Generator deploymentsGen = new DP2Generator("deployment",dataFileForSheet);
                    deploymentsGen.setNamedGraphUri(dataFileForSheet.getUri());
                    chain.addGenerator(deploymentsGen);
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return chain;

    }

    /****************************
     *    DA                    *
     ****************************/

    /*
    public static GeneratorChain annotateDAFile(DataFile dataFile) {
        System.out.println("Processing DA file " + dataFile.getFilename());

        GeneratorChain chain = new GeneratorChain();

        STR str = null;
        String str_uri = null;
        String deployment_uri = null;
        String schema_uri = null;
        String study_uri = null;

        if (dataFile != null) {
            str_uri = URIUtils.replacePrefixEx(dataFile.getDataAcquisitionUri());
            str = STR.findByUri(str_uri);
            if (str != null) {
                if (!str.isComplete()) {
                    dataFile.getLogger().printWarningByIdWithArgs("DA_00003", str_uri);
                    chain.setInvalid();
                    return chain;
                } else {
                    dataFile.getLogger().println(String.format("STR <%s> has been located", str_uri));
                }
                study_uri = str.getStudy().getUri();
                deployment_uri = str.getDeploymentUri();
                schema_uri = str.getSchemaUri();
            } else {
                dataFile.getLogger().printWarningByIdWithArgs("DA_00004", str_uri);
                chain.setInvalid();
                return chain;
            }
        }

        if (study_uri == null || study_uri.isEmpty()) {
            dataFile.getLogger().printExceptionByIdWithArgs("DA_00008", str_uri);
            chain.setInvalid();
            return chain;
        } else {
            try {
                study_uri = URLDecoder.decode(study_uri, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                dataFile.getLogger().printException(String.format("URL decoding error for study uri <%s>", study_uri));
                chain.setInvalid();
                return chain;
            }
            dataFile.getLogger().println(String.format("Study <%s> specified for data acquisition <%s>", study_uri, str_uri));
        }

        if (schema_uri == null || schema_uri.isEmpty()) {
            dataFile.getLogger().printExceptionByIdWithArgs("DA_00005", str_uri);
            chain.setInvalid();
            return chain;
        } else {
            dataFile.getLogger().println(String.format("Schema <%s> specified for data acquisition: <%s>", schema_uri, str_uri));
        }

        if (deployment_uri == null || deployment_uri.isEmpty()) {
            dataFile.getLogger().printExceptionByIdWithArgs("DA_00006", str_uri);
            chain.setInvalid();
            return chain;
        } else {
            try {
                deployment_uri = URLDecoder.decode(deployment_uri, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                dataFile.getLogger().printException(String.format("URL decoding error for deployment uri <%s>", deployment_uri));
                chain.setInvalid();
                return chain;
            }
            dataFile.getLogger().println(String.format("Deployment <%s> specified for data acquisition <%s>", deployment_uri, str_uri));
        }

        if (str != null) {
            dataFile.setStudyUri(str.getStudy().getUri());
            // TODO
            //dataFile.setDatasetUri(DataFactory.getNextDatasetURI(str.getUri()));
            str.addDatasetUri(dataFile.getDatasetUri());

            SDD schema = SDD.find(str.getSchemaUri());
            if (schema == null) {
                dataFile.getLogger().printExceptionByIdWithArgs("DA_00007", str.getSchemaUri());
                chain.setInvalid();
                return chain;
            }

            if (!str.hasCellScope()) {
            	// Need to be fixed here by getting codeMap and codebook from sparql query
            	DASOInstanceGenerator dasoInstanceGen = new DASOInstanceGenerator(
            			dataFile, str, dataFile.getFileName());
            	chain.addGenerator(dasoInstanceGen);
            	chain.addGenerator(new MeasurementGenerator(MeasurementGenerator.FILEMODE, dataFile, str, schema, dasoInstanceGen));
            } else {
                chain.addGenerator(new MeasurementGenerator(MeasurementGenerator.FILEMODE, dataFile, str, schema, null));
            }
            chain.setNamedGraphUri(URIUtils.replacePrefixEx(dataFile.getDataAcquisitionUri()));
        }

        return chain;
    }
    */

    public static boolean nameSpaceGen(DataFile dataFile, Map<String, String> mapCatalog, String templateFile) {
        RecordFile nameSpaceRecordFile = null;
        if (mapCatalog.get("hasDependencies") != null) {
            System.out.print("Extracting NameSpace sheet from spreadsheet... ");
            nameSpaceRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), mapCatalog.get("hasDependencies"));
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
        if (mapCatalog.get("AnnotationStems") != null) {
            System.out.print("Extracting [AnnotationStems] sheet from spreadsheet... ");
            annotationStemRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), mapCatalog.get("AnnotationStems"));
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
        if (mapCatalog.get("Annotations") != null) {
            System.out.print("Extracting [Annotations] sheet from spreadsheet... ");
            annotationRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), mapCatalog.get("Annotations"));
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
        if (mapCatalog.get("MessageStream") != null) {
            System.out.print("Extracting [MessageStream] sheet from spreadsheet... ");
            messageStreamRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), mapCatalog.get("MessageStream"));
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
        if (mapCatalog.get("MessageTopic") != null) {
            System.out.print("Extracting [MessageTopic] sheet from spreadsheet... ");
            messageTopicRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), mapCatalog.get("MessageTopic"));
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
        if (mapCatalog.get("Instruments") != null) {
            System.out.print("Extracting [Instruments] sheet from spreadsheet... ");
            instrumentsRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), mapCatalog.get("Instruments"));
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
        if (mapCatalog.get("Detectors") != null) {
            System.out.print("Extracting [Detectors] sheet from spreadsheet... ");
            detectorsRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), mapCatalog.get("Detectors"));
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
        if (mapCatalog.get("SensingPerspective") != null) {
            System.out.print("Extracting [SensingPerspective] sheet from spreadsheet... ");
            sensingPerspectiveRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), mapCatalog.get("SensingPerspective"));
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

}
