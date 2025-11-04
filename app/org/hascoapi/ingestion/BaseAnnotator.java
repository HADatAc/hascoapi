package org.hascoapi.ingestion;

import java.util.*;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.utils.MetadataSheetsCatalog;

public abstract class BaseAnnotator {

    /**
     * Loads and validates the InfoSheet, builds the mapCatalog, and sets it on the dataFile.
     * Returns null if the InfoSheet is invalid, empty, or has missing/unexpected sheet keys.
     */
    protected static Map<String, String> loadCatalog(DataFile dataFile, String metadataType) {
        RecordFile recordFile = new SpreadsheetRecordFile(dataFile.getFile(), "InfoSheet");
        if (!recordFile.isValid()) {
            // Missing InfoSheet → match with "Missing InfoSheet" errors from dictionary
            dataFile.getLogger().printExceptionById("DOI_00001");
            return null;
        }

        if (recordFile.getRecords().isEmpty()) {
            String msg = "[ERROR] InfoSheet has no records.";
            System.out.println(msg);
            // Equivalent to "Unknown headers" or malformed InfoSheet
            dataFile.getLogger().printExceptionById("DPL_00002");
            dataFile.getLogger().println(msg);
            return null;
        }

        dataFile.setRecordFile(recordFile);
        Map<String, String> mapCatalog = new HashMap<>();

        // Build catalog map from InfoSheet
        for (Record record : recordFile.getRecords()) {
            String key = record.getValueByColumnIndex(0);
            String value = record.getValueByColumnIndex(1);
            if (key != null && !key.trim().isEmpty()) {
                mapCatalog.put(key.trim(), value != null ? value.trim() : "");
                System.out.println(key + " : " + value);
            }
        }

        // Validate sheet keys; return null if any errors found
        boolean valid = validateSheetKeys(dataFile, mapCatalog, metadataType);
        if (!valid) {
            String msg = "[ERROR] InfoSheet validation failed for metadata type: " + metadataType;
            System.out.println(msg);
            // Log a more general “unknown headers / wrong structure” type of issue
            dataFile.getLogger().printExceptionById("DPL_00002");
            dataFile.getLogger().println(msg);
            return null;
        }

        return mapCatalog;
    }

    /**
     * Validates the sheet keys in mapCatalog against the expected list
     * from MetadataSheetsCatalog for the given metadata type.
     *
     * @return true if valid, false if missing or unexpected sheets are found.
     */
    private static boolean validateSheetKeys(DataFile dataFile, Map<String, String> mapCatalog, String metadataType) {
        List<String> expectedSheets = MetadataSheetsCatalog.getSheetsForType(metadataType);
        Set<String> providedSheets = mapCatalog.keySet();

        boolean isValid = true;

        // Missing expected sheets
        for (String required : expectedSheets) {
            if (!providedSheets.contains(required)) {
                String msg = "[ERROR] Missing required sheet key: '" + required + "' for type " + metadataType;
                System.out.println(msg);
                // “Missing InfoSheet / required sheet” → consistent with STR_00005–STR_00006
                dataFile.getLogger().printExceptionById("STR_00005");
                dataFile.getLogger().println(msg);
                isValid = false;
            }
        }

        // Extra sheets not expected for this metadata type
        for (String extra : providedSheets) {
            if (!expectedSheets.contains(extra)) {
                String msg = "[ERROR] Unexpected sheet key found: '" + extra + "' for type " + metadataType;
                System.out.println(msg);
                // "Unknown headers" fits this case
                dataFile.getLogger().printExceptionById("DPL_00002");
                dataFile.getLogger().println(msg);
                isValid = false;
            }
        }

        return isValid;
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
