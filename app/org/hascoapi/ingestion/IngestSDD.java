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

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
//import org.hascoapi.data.api.DataFactory;
import org.hascoapi.entity.pojo.DataFile;
//import org.hascoapi.entity.pojo.DataAcquisitionSchema;
import org.hascoapi.entity.pojo.SDDAttribute;
import org.hascoapi.entity.pojo.SDDObject;
import org.hascoapi.entity.pojo.SDD;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.ConfigProp;
import org.hascoapi.utils.NameSpaces;

import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;

public class IngestSDD {

    public static void exec(SDD sdd, DataFile dataFile, File file) {

        System.out.println("IngestSDD.exec(): Step 1 of 5 = Processing file: " + dataFile.getFilename());

        String fileName = dataFile.getFilename();

        System.out.println("IngestSDD.exec() Step 2 of 5. [" + dataFile.getLastProcessTime() + "]");

        // file is rejected if it already exists in the folder of processed files
        //if (dataFile.getLastProcessTime() != null) {
        //    dataFile.getLogger().printExceptionByIdWithArgs("GBL_00002", fileName);
        //    return;
        //}

        System.out.println("IngestSDD.exec() Step 3 of 5");

        dataFile.getLogger().println(String.format("Processing file: %s", fileName));

        // file is rejected if it has an invalid extension
        RecordFile recordFile = null;
        if (fileName.endsWith(".csv")) {
            recordFile = new CSVRecordFile(file);
        } else if (fileName.endsWith(".xlsx")) {
            recordFile = new SpreadsheetRecordFile(file);
        } else {
            dataFile.getLogger().printExceptionByIdWithArgs("GBL_00003", fileName);
            return;
        }
        
        System.out.println("IngestSDD.exec() Step 4 of 5");

        //dataFile.setRecordFile(recordFile);

        boolean bSucceed = false;
        GeneratorChain chain = null;

        if (fileName.startsWith("SDD-")) {
            System.out.println("IngestSDD.exec(): Step 5 of 5 - calling IngestSDD.buildChain()");
            chain = buildChain(sdd, dataFile,file);           
        } 

        if (chain != null) {
            bSucceed = chain.generate();
        }

        if (bSucceed) {
            // if chain includes PVGenerator, executes PVGenerator.generateOthers()
            if (chain.getPV()) {
                
                PVGenerator.generateOthers(chain.getCodebookFile(), chain.getSddName(), ConfigProp.getKbPrefix());
            }            	
            dataFile.setFileStatus(DataFile.PROCESSED);
            dataFile.setCompletionTime(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));
            dataFile.save();
            return;
        } 
        return;
    }

    /****************************
     *    SDD                   *
     ****************************/    
    
    public static GeneratorChain buildChain(SDD sdd, DataFile dataFile, File file) {
        System.out.println("IngestSDD.buildChain(): Processing SDD file ...");
        
        RecordFile recordFile = new SpreadsheetRecordFile(file, "InfoSheet");
        if (!recordFile.isValid()) {
            dataFile.getLogger().printExceptionById("SDD_00001");
            return null;
        } 
        dataFile.setRecordFile(recordFile);
        Map<String, String> mapCatalog = sdd.getCatalog();
        for (Record record : recordFile.getRecords()) {
            mapCatalog.put(record.getValueByColumnIndex(0), record.getValueByColumnIndex(1));
            System.out.println(record.getValueByColumnIndex(0) + ":" + record.getValueByColumnIndex(1));
        }

        System.out.println("IngestSDD.buildChain(): Build chain 1 of 10");

        String sddName = sdd.getLabel();
        String sddVersion = sdd.getHasVersion();
        if (sddName == "") {
            dataFile.getLogger().printExceptionById("SDD_00003");
            return null;
        }
        if (sddVersion == "") {
            dataFile.getLogger().printExceptionById("SDD_00018");
            return null;
        }

        System.out.println("IngestSDD.buildChain(): Build chain 2  of 10");

        RecordFile codeMappingRecordFile = null;
        RecordFile dictionaryRecordFile = null;
        RecordFile codeBookRecordFile = null;
        RecordFile timelineRecordFile = null;

        File codeMappingFile = null;

        String fileName = dataFile.getFilename();

        if (fileName.endsWith(".csv")) {
            String prefix = "sddtmp/" + fileName.replace(".csv", "");
            File dictionaryFile = sdd.downloadFile(mapCatalog.get("Data_Dictionary"), prefix + "-dd.csv");
            File codeBookFile = sdd.downloadFile(mapCatalog.get("Codebook"), prefix + "-codebook.csv");
            File timelineFile = sdd.downloadFile(mapCatalog.get("Timeline"), prefix + "-timeline.csv");
            codeMappingFile = sdd.downloadFile(mapCatalog.get("Code_Mappings"), prefix + "-code-mappings.csv");

            dictionaryRecordFile = new CSVRecordFile(dictionaryFile);
            codeBookRecordFile = new CSVRecordFile(codeBookFile);
            timelineRecordFile = new CSVRecordFile(timelineFile);
        } else if (fileName.endsWith(".xlsx")) {
            //codeMappingFile = sdd.downloadFile(mapCatalog.get("Code_Mappings"), 
            //        "sddtmp/" + fileName.replace(".xlsx", "") + "-code-mappings.csv");

            if (mapCatalog.get("Codebook") != null) { 
                System.out.println("Processing codebook...");
                codeBookRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), mapCatalog.get("Codebook").replace("#", ""));
            }
            
            if (mapCatalog.get("Data_Dictionary") != null) {
                System.out.println("Processing data dictionary...");
                dictionaryRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), mapCatalog.get("Data_Dictionary").replace("#", ""));
            }
            
            if (mapCatalog.get("Timeline") != null) {
                System.out.println("Processing timeline...");
                timelineRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), mapCatalog.get("Timeline").replace("#", ""));
            }
        }

        System.out.println("IngestSDD.buildChain(): Build chain 3 of 10");

        /** 

        if (null != codeMappingFile) {
            codeMappingRecordFile = new CSVRecordFile(codeMappingFile);
            if (!sdd.readCodeMapping(codeMappingRecordFile)) {
                dataFile.getLogger().printWarningById("SDD_00016");
            } else {
                dataFile.getLogger().println(String.format("Codemappings: " + sdd.getCodeMapping().get("U"), fileName));
            }
        } else {
            dataFile.getLogger().printWarningById("SDD_00017");
        }
        */

        System.out.println("IngestSDD.buildChain(): Build chain 4 of 10");

        if (!sdd.readDataDictionary(dictionaryRecordFile, dataFile)) {
            dataFile.getLogger().printExceptionById("SDD_00004");
            return null;
        }

        System.out.println("IngestSDD.buildChain(): Build chain 5 of 10");

        if (codeBookRecordFile == null || !sdd.readCodebook(codeBookRecordFile)) {
            dataFile.getLogger().printWarningById("SDD_00005");
        }

        System.out.println("IngestSDD.buildChain(): Build chain 6 of 10");

        if (timelineRecordFile == null || !sdd.readTimeline(timelineRecordFile)) {
            dataFile.getLogger().printWarningById("SDD_00006");
        }

        System.out.println("IngestSDD.buildChain(): Build chain 7 of 10");

        GeneratorChain chain = new GeneratorChain();
        chain.setPV(true);
        
        if (dictionaryRecordFile != null && dictionaryRecordFile.isValid()) {
            DataFile dictionaryFile;
            try {
                dictionaryFile = (DataFile)dataFile.clone();
                dictionaryFile.setRecordFile(dictionaryRecordFile);
                chain.addGenerator(new SDDAttributeGenerator(dictionaryFile, sddName, sdd.getCodeMapping(), sdd.readDDforEAmerge(dictionaryRecordFile)));
                chain.addGenerator(new SDDObjectGenerator(dictionaryFile, sddName, sdd.getCodeMapping()));
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("IngestSDD.buildChain(): Build chain 8 of 10");

        // codebook needs to be processed after data dictionary because codebook relies on 
        // data dictionary's attributes (SDDAs) to group codes for categorical variables
        
        if (codeBookRecordFile != null && codeBookRecordFile.isValid()) {
            DataFile codeBookFile;
            try {
                codeBookFile = (DataFile)dataFile.clone();
                codeBookFile.setRecordFile(codeBookRecordFile);
                chain.setCodebookFile(codeBookFile);
                chain.setSddName(URIUtils.replacePrefixEx(ConfigProp.getKbPrefix() + "SDD-" + sddName));
                chain.addGenerator(new PVGenerator(codeBookFile, sddName, sdd.getMapAttrObj(), sdd.getCodeMapping()));
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("IngestSDD.buildChain(): Build chain 9 of 10");

        GeneralGenerator generalGenerator = new GeneralGenerator(dataFile, "SDD");
        String sddUri = ConfigProp.getKbPrefix() + "SDD-" + sddName;
        Map<String, Object> row = new HashMap<String, Object>();
        row.put("hasURI", sddUri);
        dataFile.getLogger().println("This SDD is assigned with uri: " + sddUri + " and is of type hasco:SDD");
        row.put("a", "hasco:SDD");
        row.put("rdfs:label", "SDD-" + sddName);
        row.put("rdfs:comment", "");
        row.put("hasco:hasVersion", sddVersion);
        generalGenerator.addRow(row);
        chain.addGenerator(generalGenerator);
        chain.setNamedGraphUri(URIUtils.replacePrefixEx(sddUri));

        System.out.println("IngestSDD.buildChain(): Build chain 10 of 10");

        return chain;
    }
}
