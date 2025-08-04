package org.hascoapi.ingestion;

import java.util.HashMap;
import java.util.Map;

import org.hascoapi.entity.pojo.DataFile;

public abstract class BaseAnnotator {

    /**
     * Loads and validates the InfoSheet, builds the mapCatalog, and sets it on the dataFile.
     */
    protected static Map<String, String> loadCatalog(DataFile dataFile) {
        RecordFile recordFile = new SpreadsheetRecordFile(dataFile.getFile(), "InfoSheet");
        if (!recordFile.isValid()) {
            dataFile.getLogger().printExceptionById("DPL_00001");
            return null;
        }

        if (recordFile.getRecords().isEmpty()) {
            String msg = "[ERROR] InfoSheet has no records.";
            System.out.println(msg);
            dataFile.getLogger().println(msg);
            return null;
        }

        dataFile.setRecordFile(recordFile);

        Map<String, String> mapCatalog = new HashMap<>();
        for (Record record : recordFile.getRecords()) {
            String key = record.getValueByColumnIndex(0);
            String value = record.getValueByColumnIndex(1);
            if (key != null && !key.trim().isEmpty()) {
                mapCatalog.put(key.trim(), value != null ? value.trim() : "");
                System.out.println(key + " : " + value);
            }
        }

        return mapCatalog;
    }

    /**
     * Adds a generator to the chain if the given sheet key exists in the catalog.
     */
    protected static void addCustomGeneratorIfSheetExists(DataFile dataFile,
                                                          Map<String, String> mapCatalog,
                                                          String sheetKey,
                                                          String status,
                                                          GeneratorChain chain,
                                                          GeneratorFactory factory) {
        String sheetName = mapCatalog.get(sheetKey);
        if (sheetName == null) {
            warnSheetMissing(dataFile, sheetKey);
            return;
        }

        RecordFile sheet = new SpreadsheetRecordFile(dataFile.getFile(), sheetName.replace("#", ""));
        try {
            DataFile clonedFile = (DataFile) dataFile.clone();
            clonedFile.setRecordFile(sheet);

            BaseGenerator generator = factory.create(clonedFile, status);
            generator.setNamedGraphUri(clonedFile.getUri());
            chain.addGenerator(generator);

        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
    }

    public static void warnSheetMissing(DataFile dataFile, String sheetKey) {
        String msg = "[WARNING] '" + sheetKey + "' sheet is missing.";
        System.out.println(msg);
        dataFile.getLogger().println(msg);
    }
}
