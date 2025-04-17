package org.hascoapi.ingestion;

import java.lang.String;
import java.util.*;

import org.hascoapi.entity.pojo.DataFile;

public class AnnotateINSCleaner {

     public static GeneratorChain exec(DataFile dataFile, String templateFile, String status) {
        RecordFile recordFile = new SpreadsheetRecordFile(dataFile.getFile(), "InfoSheet");
        if (!recordFile.isValid()) {
            dataFile.getLogger().printExceptionById("INSCLR_00001");
            return null;
        } else {
            dataFile.setRecordFile(recordFile);
        }

        Map<String, String> mapCatalog = new HashMap<String, String>();
        for (Record record : dataFile.getRecordFile().getRecords()) {
            mapCatalog.put(record.getValueByColumnIndex(0), record.getValueByColumnIndex(1));
            //System.out.println(record.getValueByColumnIndex(0) + ":" + record.getValueByColumnIndex(1));
        }

        if (dataFile.getFilename().endsWith(".xlsx")) {
            IngestionWorker.nameSpaceGen(dataFile, mapCatalog, templateFile);
        } 

        IngestionWorker.annotationGen(dataFile, mapCatalog, templateFile, status);

        GeneratorChain chain = new GeneratorChain();
        RecordFile sheet = null;

        try {

            chain.setNamedGraphUri(dataFile.getUri());

            String cleanerSheet = mapCatalog.get("Cleaner");
            if (cleanerSheet == null) {
                System.out.println("[WARNING] 'Cleaner' sheet is missing.");
                dataFile.getLogger().println("[WARNING] 'Cleaner' sheet is missing.");
            } else {
                cleanerSheet.replace("#", "");
                sheet = new SpreadsheetRecordFile(dataFile.getFile(), cleanerSheet);
                try {
                    DataFile dataFileForSheet = (DataFile)dataFile.clone();
                    dataFileForSheet.setRecordFile(sheet);
                    INSCleanerGenerator cleanerGen = new INSCleanerGenerator("cleaner",dataFileForSheet, status);
                    cleanerGen.setNamedGraphUri(dataFileForSheet.getUri());
                    chain.addGenerator(cleanerGen);
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return chain;
    }

}
