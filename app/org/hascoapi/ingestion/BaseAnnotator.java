package org.hascoapi.ingestion;

import java.util.*;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.utils.MTSheet;

public abstract class BaseAnnotator {

    /**
     * Loads and validates the InfoSheet, builds the mapCatalog, and sets it on the dataFile.
     * Returns null if the InfoSheet is invalid, empty, or has missing/unexpected sheet keys.
     */
    protected static Map<String, String> loadCatalog(DataFile dataFile, String mtType) {
        RecordFile recordFile = new SpreadsheetRecordFile(dataFile.getFile(), "InfoSheet");

        // InfoSheet missing
        if (!recordFile.isValid()) {
            dataFile.getLogger().printExceptionByIdWithArgs("GBL_00005", mtType);
            dataFile.setFileStatus(DataFile.ERROR);
            return null;
        }

        // InfoSheet empty
        if (recordFile.getRecords().isEmpty()) {
            dataFile.getLogger().printExceptionByIdWithArgs("GBL_00004", mtType);
            dataFile.setFileStatus(DataFile.ERROR);
            return null;
        }

        dataFile.setRecordFile(recordFile);
        Map<String, String> mapCatalog = new HashMap<>();

        // Build catalog map from InfoSheet
        for (Record record : recordFile.getRecords()) {
            String key = record.getValueByColumnIndex(0);
            String value = record.getValueByColumnIndex(1);
            if (key == null || key.trim().isEmpty()) {
                continue;
            }
            mapCatalog.put(key.trim(), value != null ? value.trim() : "");
        }

        // DP2: sanitize/repair common InfoSheet mapping issues by preferring actual sheet tabs.
        if (mtType != null && mtType.equalsIgnoreCase(org.hascoapi.Constants.MT_DP2)) {
            sanitizeDp2Catalog(dataFile, mapCatalog);
        }

        // Validate sheet keys; return null if any errors found
        boolean valid = validateSheetKeys(dataFile, mapCatalog, mtType);
        if (!valid) {
            dataFile.getLogger().printExceptionByIdWithArgs("GBL_00004", mtType);
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
    private static boolean validateSheetKeys(DataFile dataFile, Map<String, String> mapCatalog, String mtType) {
        List<String> expectedSheets = MTSheet.getSheetsForType(mtType);
        Set<String> providedSheets = mapCatalog.keySet();

        boolean isValid = true;

        // Missing expected sheets (always an error)
        for (String required : expectedSheets) {
            if (!providedSheets.contains(required)) {
                dataFile.getLogger().printExceptionByIdWithArgs("GBL_00006", required, mtType);
                isValid = false;
            }
        }

        // Extra sheets: for DP2, treat as optional (warning) instead of failing ingestion.
        for (String extra : providedSheets) {
            if (!expectedSheets.contains(extra)) {
                if (mtType != null && mtType.equalsIgnoreCase(org.hascoapi.Constants.MT_DP2)) {
                    // DP2 templates evolve and can include optional tabs; don't fail ingestion.
                    dataFile.getLogger().printWarningByIdWithArgs("GBL_00007", extra, mtType);
                } else {
                    dataFile.getLogger().printExceptionByIdWithArgs("GBL_00007", extra, mtType);
                    isValid = false;
                }
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

        if (sheetName == null || sheetName.trim().isEmpty()) {
            warnSheetMissing(dataFile, sheetKey);
            return;
        }

        String normalized = sheetName.replace("#", "").trim();

        // Prefer the real sheetKey tab when the catalog is miswired.
        // DP2 templates in the wild sometimes contain wrong InfoSheet mappings.
        // If the workbook has a sheet with the same name as the key and it has rows, override.
        if (!normalized.equalsIgnoreCase(sheetKey)) {
            RecordFile keySheetProbe = new SpreadsheetRecordFile(dataFile.getFile(), sheetKey);
            if (keySheetProbe != null && keySheetProbe.isValid() && keySheetProbe.getRecords() != null && !keySheetProbe.getRecords().isEmpty()) {
                normalized = sheetKey;
            }
        }

        // Resolve the sheet
        RecordFile sheet = new SpreadsheetRecordFile(dataFile.getFile(), normalized);
        if (sheet == null || !sheet.isValid()) {
            sheet = new SpreadsheetRecordFile(dataFile.getFile(), sheetKey);
        }

        if (sheet == null || !sheet.isValid() || sheet.getRecords() == null) {
            warnSheetMissing(dataFile, sheetKey);
            return;
        }

        int recordCount = sheet.getRecords().size();

        if (recordCount == 0) {
            dataFile.getLogger().println("addCustomGeneratorIfSheetExists(): sheet '" + sheetKey + "' is empty; skipping.");
            return;
        }

        try {
            DataFile clonedFile = (DataFile) dataFile.clone();
            if (clonedFile == null) {
                dataFile.getLogger().println("addCustomGeneratorIfSheetExists(): failed to clone DataFile for sheet '" + sheetKey + "'; skipping.");
                return;
            }
            clonedFile.setRecordFile(sheet);

            BaseGenerator generator = factory.create(clonedFile, status);
            if (generator == null) {
                dataFile.getLogger().println("addCustomGeneratorIfSheetExists(): generator factory returned null for sheet '" + sheetKey + "'; skipping.");
                return;
            }
            generator.setNamedGraphUri(clonedFile.getUri());
            chain.addGenerator(generator);

        } catch (CloneNotSupportedException e) {
            dataFile.getLogger().println("addCustomGeneratorIfSheetExists(): clone not supported; skipping sheet '" + sheetKey + "'.");
        } catch (Exception e) {
            dataFile.getLogger().println("addCustomGeneratorIfSheetExists(): unexpected error for sheet '" + sheetKey + "': " + e.getMessage());
        }
    }

    /**
     * Logs a warning when a sheet is not found, but not critical.
     */
    public static void warnSheetMissing(DataFile dataFile, String sheetKey) {
        // JSON template example: "Sheet %s was not found in the InfoSheet catalog."
        dataFile.getLogger().printWarningByIdWithArgs("GBL_00006", sheetKey);
    }

    /**
     * DP2 templates are frequently delivered with incorrect InfoSheet mappings.
     * This method repairs core DP2 mappings by preferring real workbook tabs.
     */
    private static void sanitizeDp2Catalog(DataFile dataFile, Map<String, String> mapCatalog) {
        if (dataFile == null || dataFile.getFile() == null || mapCatalog == null) return;

        // Only repair for known DP2 core tabs
        List<String> keys = Arrays.asList(
                "Deployments",
                "Platforms",
                "PlatformInstances",
                "InstrumentInstances",
                "ComponentInstances",
                "FieldsOfView",
                "SensingPerspective",
                "MessageStream",
                "MessageTopic",
                "Namespace",
                "Namespaces",
                "hasDependencies"
        );

        for (String key : keys) {
            if (!mapCatalog.containsKey(key)) continue;
            String raw = mapCatalog.get(key);
            String mapped = raw == null ? "" : raw.replace("#", "").trim();

            // Probe the mapped sheet (if any) and the key sheet.
            SpreadsheetRecordFile mappedProbe = (mapped.isEmpty()) ? null : new SpreadsheetRecordFile(dataFile.getFile(), mapped);
            SpreadsheetRecordFile keyProbe = new SpreadsheetRecordFile(dataFile.getFile(), key);

            boolean mappedOk = mappedProbe != null && mappedProbe.isValid() && mappedProbe.getRecords() != null && !mappedProbe.getRecords().isEmpty();
            boolean keyOk = keyProbe != null && keyProbe.isValid() && keyProbe.getRecords() != null && !keyProbe.getRecords().isEmpty();

            // If mapping is wrong/missing but the key tab exists, fix it.
            if (!mappedOk && keyOk) {
                mapCatalog.put(key, "#" + key);
            }
        }

        // Extra: detect the specific rotation seen in logs and repair it.
        // FieldsOfView -> InstrumentInstances, InstrumentInstances -> ComponentInstances, ComponentInstances -> FieldsOfView
        String fov = norm(mapCatalog.get("FieldsOfView"));
        String ins = norm(mapCatalog.get("InstrumentInstances"));
        String comp = norm(mapCatalog.get("ComponentInstances"));
        if ("InstrumentInstances".equalsIgnoreCase(fov) && "ComponentInstances".equalsIgnoreCase(ins) && "FieldsOfView".equalsIgnoreCase(comp)) {
            mapCatalog.put("FieldsOfView", "#FieldsOfView");
            mapCatalog.put("InstrumentInstances", "#InstrumentInstances");
            mapCatalog.put("ComponentInstances", "#ComponentInstances");
        }

        // Namespace sheet key differences: some DP2 files use 'Namespace' others 'Namespaces' as the actual tab.
        // Make hasDependencies point to whichever exists.
        String hasDeps = mapCatalog.get("hasDependencies");
        if (hasDeps == null || hasDeps.trim().isEmpty()) {
            // If catalog doesn't define it, choose a best-effort default
            SpreadsheetRecordFile ns1 = new SpreadsheetRecordFile(dataFile.getFile(), "Namespace");
            SpreadsheetRecordFile ns2 = new SpreadsheetRecordFile(dataFile.getFile(), "Namespaces");
            if (ns1.isValid()) {
                mapCatalog.put("hasDependencies", "#Namespace");
            } else if (ns2.isValid()) {
                mapCatalog.put("hasDependencies", "#Namespaces");
            }
        }
    }

    private static String norm(String v) {
        return v == null ? "" : v.replace("#", "").trim();
    }
}
