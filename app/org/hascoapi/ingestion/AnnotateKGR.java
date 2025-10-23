package org.hascoapi.ingestion;

import java.util.Map;

import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.KGR;

public class AnnotateKGR extends BaseAnnotator {

    public static GeneratorChain exec(DataFile dataFile, String templateFile, String status) {
        System.out.println("AnnotateKGR.exec(): Processing KGR meta-template ...");

        System.out.println("AnnotateKGR.exec(): Build chain 1 of 9 - Reading catalog and template");

        Map<String, String> mapCatalog = loadCatalog(dataFile);
        if (mapCatalog == null) {
            return null;
        }

        KGR kgr = new KGR(dataFile, templateFile);
        kgr.setHasDataFileUri(dataFile.getUri());
        kgr.setHasSIRManagerEmail(dataFile.getHasSIRManagerEmail());

        System.out.println("DataFileUri: [" + dataFile.getUri() + "]");
        System.out.println("DataFileUri: [" + kgr.getHasDataFileUri() + "]");

        IngestionWorker.nameSpaceGen(dataFile, mapCatalog, templateFile);

        // the template is needed to process individual sheets
        kgr.setTemplates(templateFile);

        String hasMediaFolder = mapCatalog.get("hasMediaFolder");

        // verifyUri parameter parsing with error handling
        String rawVerifyUri = mapCatalog.get("verifyUri");
        boolean verifyUri;
        if (rawVerifyUri == null) {
            dataFile.getLogger().printException("KGR file is missing verifyUri parameter.");
            return null;
        }
        rawVerifyUri = rawVerifyUri.toLowerCase();
        if ("true".equals(rawVerifyUri)) {
            verifyUri = true;
        } else if ("false".equals(rawVerifyUri)) {
            verifyUri = false;
        } else {
            dataFile.getLogger().printException("verifyUri parameter in KGR must be `true` or `false`.");
            return null;
        }

        System.out.println("AnnotateKGR.exec(): Build chain 2 of 9 - Creating empty generator chain");
        GeneratorChain chain = new GeneratorChain();

        // Define sheets and corresponding generator types
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
            System.out.println("AnnotateKGR.exec(): Build chain " + aux++ + " of 9 - Adding " + sheetInfo[0] + " generator into chain");
            addKGRGeneratorIfSheetExists(dataFile, mapCatalog, sheetInfo[0], status, chain, sheetInfo[1], hasMediaFolder, verifyUri);
        }

        return chain;
    }

    private static void addKGRGeneratorIfSheetExists(DataFile dataFile, Map<String, String> mapCatalog,
                                                     String sheetKey, String status, GeneratorChain chain,
                                                     String type, String hasMediaFolder, boolean verifyUri) {
        String sheetName = mapCatalog.get(sheetKey);
        if (sheetName == null || sheetName.trim().isEmpty()) {
            warnSheetMissing(dataFile, sheetKey);
            return;
        }

        sheetName = sheetName.replace("#", "").trim();
        RecordFile sheet = new SpreadsheetRecordFile(dataFile.getFile(), sheetName);
        if (!sheet.isValid()) {
            System.out.println("[WARNING] Sheet '" + sheetName + "' is invalid or empty.");
            return;
        }

        try {
            DataFile clonedFile = (DataFile) dataFile.clone();
            clonedFile.setRecordFile(sheet);
            KGRGenerator gen = new KGRGenerator(type, status, clonedFile, hasMediaFolder, verifyUri);
            gen.setNamedGraphUri(clonedFile.getUri());
            chain.addGenerator(gen);
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
    }

}

