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
            dataFile.getLogger().printExceptionById("KGR_00001"); // "KGR InfoSheet validation failed"
            return null;
        }

        KGR kgr = new KGR(dataFile, templateFile);
        kgr.setHasDataFileUri(dataFile.getUri());
        kgr.setHasSIRManagerEmail(dataFile.getHasSIRManagerEmail());

        IngestionWorker.nameSpaceGen(dataFile, mapCatalog, templateFile);
        kgr.setTemplates(templateFile);

        String hasMediaFolder = mapCatalog.get("hasMediaFolder");

        // verifyUri parsing with error handling
        String rawVerifyUri = mapCatalog.get("verifyUri");
        boolean verifyUri;
        if (rawVerifyUri == null) {
            dataFile.getLogger().printExceptionById("KGR_00002"); // "Missing verifyUri parameter"
            return null;
        }
        rawVerifyUri = rawVerifyUri.toLowerCase();
        if ("true".equals(rawVerifyUri)) {
            verifyUri = true;
        } else if ("false".equals(rawVerifyUri)) {
            verifyUri = false;
        } else {
            dataFile.getLogger().printExceptionById("KGR_00003"); // "Invalid verifyUri parameter value"
            return null;
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
