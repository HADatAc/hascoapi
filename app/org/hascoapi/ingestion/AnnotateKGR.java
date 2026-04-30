package org.hascoapi.ingestion;

import java.util.Map;
import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.KGR;

public class AnnotateKGR extends BaseAnnotator {

    public static GeneratorChain exec(DataFile dataFile, String templateFile, String status) {
        dataFile.getLogger().println("AnnotateKGR.exec(): Processing KGR meta-template ...");

        Map<String, String> mapCatalog = loadCatalog(dataFile, Constants.MT_KGR);
        if (mapCatalog == null) {
            // loadCatalog already logs the root cause; don't emit a misleading KGR error code.
            dataFile.getLogger().printException("KGR InfoSheet validation failed.");
            return null;
        }

        KGR kgr = new KGR(dataFile, templateFile);
        kgr.setHasDataFileUri(dataFile.getUri());
        kgr.setHasSIRManagerEmail(dataFile.getHasSIRManagerEmail());

        IngestionWorker.nameSpaceGen(dataFile, mapCatalog, templateFile);
        kgr.setTemplates(templateFile);

        // Optional parameter for legacy KGRs, but REQUIRED when the workbook references images.
        String hasMediaFolder = mapCatalog.get("hasMediaFolder");
        hasMediaFolder = hasMediaFolder != null ? hasMediaFolder.trim() : "";
        if (hasMediaFolder.isEmpty() && workbookReferencesImages(dataFile, mapCatalog)) {
            dataFile.getLogger().printException("KGR InfoSheet is missing 'hasMediaFolder' but the workbook references images via 'hasco:hasImage'.");
            return null;
        }

        // Optional parameter: default to false when missing.
        boolean verifyUri = false;
        String rawVerifyUri = mapCatalog.get("verifyUri");
        if (rawVerifyUri != null) {
            rawVerifyUri = rawVerifyUri.trim().toLowerCase();
            if ("true".equals(rawVerifyUri)) {
                verifyUri = true;
            } else if ("false".equals(rawVerifyUri) || rawVerifyUri.isEmpty()) {
                verifyUri = false;
            } else {
                dataFile.getLogger().printWarning("verifyUri parameter in KGR should be `true` or `false`; defaulting to `false`.");
                verifyUri = false;
            }
        }

        GeneratorChain chain = new GeneratorChain();

        String[][] sheets = {
                {"Places", "place"},
                {"PostalAddresses", "postaladdress"},
                {"Organizations", "organization"},
                {"Persons", "person"},
                {"Projects", "project"},
                {"ProjectOrganizations", "projectorganization"},
                {"FundingSchemes", "fundingscheme"}
        };

        int aux = 2;
        for (String[] sheetInfo : sheets) {
            addKGRGeneratorIfSheetExists(dataFile, mapCatalog, sheetInfo[0], status, chain, sheetInfo[1],
                    hasMediaFolder, verifyUri);
        }

        return chain;
    }

    private static boolean workbookReferencesImages(DataFile dataFile, Map<String, String> mapCatalog) {
        if (dataFile == null || dataFile.getFile() == null || mapCatalog == null) {
            return false;
        }

        String[] sheetKeys = new String[] {
                "Places",
                "PostalAddresses",
                "Organizations",
                "Persons",
                "Projects",
                "ProjectOrganizations",
                "FundingSchemes"
        };

        for (String sheetKey : sheetKeys) {
            String sheetName = mapCatalog.get(sheetKey);
            if (sheetName == null || sheetName.trim().isEmpty()) {
                continue;
            }

            RecordFile sheet = new SpreadsheetRecordFile(dataFile.getFile(), sheetName.replace("#", "").trim());
            if (sheet == null || !sheet.isValid() || sheet.getRecords() == null || sheet.getRecords().isEmpty()) {
                continue;
            }

            for (Record record : sheet.getRecords()) {
                try {
                    String img = record.getValueByColumnName("hasco:hasImage");
                    if (img != null && !img.trim().isEmpty()) {
                        return true;
                    }
                } catch (Exception e) {
                    // If the sheet does not have a hasco:hasImage column, ignore.
                }
            }
        }

        return false;
    }

    private static void addKGRGeneratorIfSheetExists(DataFile dataFile, Map<String, String> mapCatalog,
                                                     String sheetKey, String status, GeneratorChain chain,
                                                     String type, String hasMediaFolder, boolean verifyUri) {
        String sheetName = mapCatalog.get(sheetKey);
        if (sheetName == null || sheetName.trim().isEmpty()) {
            dataFile.getLogger().printWarningByIdWithArgs("KGR_00004", sheetKey);
            return;
        }

        RecordFile sheet = new SpreadsheetRecordFile(dataFile.getFile(), sheetName.replace("#", "").trim());
        if (!sheet.isValid()) {
            dataFile.getLogger().printWarningByIdWithArgs("KGR_00005", sheetKey);
            return;
        }

        if (sheet.getRecords() == null || sheet.getRecords().isEmpty()) {
            dataFile.getLogger().println("AnnotateKGR: sheet '" + sheetKey + "' is empty; skipping generator.");
            return;
        }

        try {
            DataFile clonedFile = (DataFile) dataFile.clone();
            clonedFile.setRecordFile(sheet);
            KGRGenerator gen = new KGRGenerator(type, status, clonedFile, hasMediaFolder, verifyUri);
            gen.setNamedGraphUri(clonedFile.getUri());
            chain.addGenerator(gen);
        } catch (CloneNotSupportedException e) {
            dataFile.getLogger().printExceptionByIdWithArgs("GBL_00008", e.getMessage());
        }
    }
}
