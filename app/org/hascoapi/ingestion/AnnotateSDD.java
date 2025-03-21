package org.hascoapi.ingestion;

import java.io.File;
import java.lang.String;
import java.util.*;

import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.SDD;
import org.hascoapi.utils.URIUtils;

public class AnnotateSDD {

     public static GeneratorChain exec(DataFile dataFile, String templateFile) {
        System.out.println("Processing SDD file ...");

        RecordFile recordFile = new SpreadsheetRecordFile(dataFile.getFile(), "InfoSheet");
        if (!recordFile.isValid()) {
            dataFile.getLogger().printExceptionById("SDD_00001");
            return null;
        } else {
            dataFile.setRecordFile(recordFile);
        }

        String sddUri = dataFile.getUri().replace("DFL","SDDICT");

        Map<String, String> mapCatalog = new HashMap<String, String>();
        for (Record record : dataFile.getRecordFile().getRecords()) {
            mapCatalog.put(record.getValueByColumnIndex(0), record.getValueByColumnIndex(1));
            //System.out.println(record.getValueByColumnIndex(0) + ":" + record.getValueByColumnIndex(1));
            if (record.getValueByColumnIndex(0).isEmpty() && record.getValueByColumnIndex(1).isEmpty()) {
                break;
            }
        }

        IngestionWorker.nameSpaceGen(dataFile, mapCatalog, templateFile);

        SDD sdd = new SDD(dataFile, templateFile);
        String fileName = dataFile.getFilename();
        if (mapCatalog.get("SDD_ID") == "") {
            dataFile.getLogger().printExceptionById("SDD_00003");
            return null;
        }
        String sddId = mapCatalog.get("SDD_ID");
        if (mapCatalog.get("Version") == "") {
            dataFile.getLogger().printExceptionById("SDD_00018");
            return null;
        }
        String sddVersion = mapCatalog.get("Version");

        RecordFile codeMappingRecordFile = null;
        RecordFile dictionaryRecordFile = null;
        RecordFile codeBookRecordFile = null;
        //RecordFile timelineRecordFile = null;

        File codeMappingFile = null;
        if (fileName.endsWith(".xlsx")) {
            codeMappingFile = sdd.downloadFile(mapCatalog.get("Code_Mappings"),
                    "sddtmp/" + fileName.replace(".xlsx", "") + "-code-mappings.csv");

            if (mapCatalog.get("Codebook") != null) {
                codeBookRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), mapCatalog.get("Codebook").replace("#", ""));
                System.out.println("IngestionWorker: read codeBookRecordFile with " + codeBookRecordFile.getRecords().size() + " records.");
            }

            if (mapCatalog.get("Data_Dictionary") != null) {
                dictionaryRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), mapCatalog.get("Data_Dictionary").replace("#", ""));
                System.out.println("IngestionWorker: read dictionaryRecordFile with " + dictionaryRecordFile.getRecords().size() + " records.");
            }

            //if (mapCatalog.get("Timeline") != null) {
            //    timelineRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), mapCatalog.get("Timeline").replace("#", ""));
            //}
        }

        if (codeMappingFile != null) {
            codeMappingRecordFile = new CSVRecordFile(codeMappingFile);
            if (!sdd.readCodeMapping(codeMappingRecordFile)) {
                dataFile.getLogger().printWarningById("SDD_00016");
            } else {
                dataFile.getLogger().println(String.format("Codemappings: " + sdd.getCodeMapping().get("U"), fileName));
            }
        } else {
            dataFile.getLogger().printWarningById("SDD_00017");
        }

        if (!sdd.readDataDictionary(dictionaryRecordFile, dataFile)) {
            dataFile.getLogger().printExceptionById("SDD_00004");
            //return null;
        }
        if (codeBookRecordFile == null || !sdd.readCodebook(codeBookRecordFile)) {
            dataFile.getLogger().printWarningById("SDD_00005");
        }
        //if (timelineRecordFile == null || !sdd.readTimeline(timelineRecordFile)) {
        //    dataFile.getLogger().printWarningById("SDD_00006");
        //}

        GeneratorChain chain = new GeneratorChain();
        chain.setNamedGraphUri(dataFile.getUri());
        chain.setPV(true);

        System.out.println("DictionaryRecordFile: " + dictionaryRecordFile.isValid());
        if (dictionaryRecordFile != null && dictionaryRecordFile.isValid()) {
            DataFile dictionaryFile;
            try {
                dictionaryFile = (DataFile)dataFile.clone();
                dictionaryFile.setRecordFile(dictionaryRecordFile);
                chain.addGenerator(new SDDAttributeGenerator(dictionaryFile, sddUri, sddId, sdd.getCodeMapping(), sdd.readDDforEAmerge(dictionaryRecordFile), templateFile));
                chain.addGenerator(new SDDObjectGenerator(dictionaryFile, sddUri, sddId, sdd.getCodeMapping()));
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }

        // codebook needs to be processed after data dictionary because codebook relies on
        // data dictionary's attributes (DASAs) to group codes for categorical variables

        System.out.println("CodeBookRecordFile: " + codeBookRecordFile.isValid());
        if (codeBookRecordFile != null && codeBookRecordFile.isValid()) {
            DataFile codeBookFile;
            try {
                codeBookFile = (DataFile)dataFile.clone();
                codeBookFile.setRecordFile(codeBookRecordFile);
                chain.setCodebookFile(codeBookFile);
                chain.setSddName(URIUtils.replacePrefixEx(sddUri));
                chain.addGenerator(new PVGenerator(codeBookFile, sddUri, sddId, sdd.getMapAttrObj(), sdd.getCodeMapping()));
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }

        GeneralGenerator generalGenerator = new GeneralGenerator(dataFile, "SemanticDataDictionary");
        Map<String, Object> row = new HashMap<String, Object>();
        row.put("hasURI", sddUri);
        row.put("a", "hasco:SemanticDataDictionary");
        row.put("hasco:hascoType", "hasco:SemanticDataDictionary");
        row.put("rdfs:label", sddId);
        row.put("rdfs:comment", "Generated from SDD file [" + dataFile.getFilename() + "]");
        row.put("vstoi:hasVersion", sddVersion);
        row.put("vstoi:hasSIRManagerEmail", dataFile.getHasSIRManagerEmail());
        generalGenerator.addRow(row);
        chain.setNamedGraphUri(URIUtils.replacePrefixEx(dataFile.getUri()));
        chain.addGenerator(generalGenerator);
        dataFile.getLogger().println("This SDD is assigned with uri: " + sddUri + " and is of type hasco:SemanticDataDictionary");

        return chain;
     }
}
