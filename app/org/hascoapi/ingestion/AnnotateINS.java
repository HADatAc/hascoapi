package org.hascoapi.ingestion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hascoapi.entity.pojo.DataFile;

public class AnnotateINS {

    public static GeneratorChain exec(DataFile dataFile, String templateFile, String status) {
        RecordFile recordFile = new SpreadsheetRecordFile(dataFile.getFile(), "InfoSheet");
        if (!recordFile.isValid()) {
            dataFile.getLogger().printExceptionById("DPL_00001");
            return null;
        }

        if (recordFile.getRecords().isEmpty()) {
            System.out.println("[ERROR] InfoSheet has no records.");
            dataFile.getLogger().println("[ERROR] InfoSheet has no records.");
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

        IngestionWorker.nameSpaceGen(dataFile, mapCatalog, templateFile);
        IngestionWorker.annotationGen(dataFile, mapCatalog, templateFile, status);

        GeneratorChain chain = new GeneratorChain();

        addGeneratorIfSheetExists(dataFile, mapCatalog, "ResponseOptions", status, chain, "responseoption");
        addGeneratorIfSheetExists(dataFile, mapCatalog, "CodeBooks", status, chain, "codebook");
        addGeneratorIfSheetExists(dataFile, mapCatalog, "CodeBookSlots", status, chain, new CodeBookSlotGeneratorFactory());
        addGeneratorIfSheetExists(dataFile, mapCatalog, "ActuatorStems", status, chain, "actuatorstem");
        addGeneratorIfSheetExists(dataFile, mapCatalog, "Actuators", status, chain, new ActuatorGeneratorFactory());
        addGeneratorIfSheetExists(dataFile, mapCatalog, "DetectorStems", status, chain, "detectorstem");
        addGeneratorIfSheetExists(dataFile, mapCatalog, "Detectors", status, chain, new DetectorGeneratorFactory());
        addGeneratorIfSheetExists(dataFile, mapCatalog, "SlotElements", status, chain, "slotelement");
        addGeneratorIfSheetExists(dataFile, mapCatalog, "Instruments", status, chain, "instrument");

        return chain;
    }

    private static void addGeneratorIfSheetExists(DataFile dataFile, Map<String, String> mapCatalog, String sheetKey,
                                                  String status, GeneratorChain chain, String type) {
        String sheetName = mapCatalog.get(sheetKey);
        if (sheetName == null) {
            warnSheetMissing(dataFile, sheetKey);
            return;
        }

        RecordFile sheet = new SpreadsheetRecordFile(dataFile.getFile(), sheetName.replace("#", ""));
        try {
            DataFile clonedFile = (DataFile) dataFile.clone();
            clonedFile.setRecordFile(sheet);
            INSGenerator gen = new INSGenerator(type, clonedFile, status);
            gen.setNamedGraphUri(clonedFile.getUri());
            chain.addGenerator(gen);
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
    }

    private static void addGeneratorIfSheetExists(DataFile dataFile, Map<String, String> mapCatalog, String sheetKey,
                                                  String status, GeneratorChain chain, GeneratorFactory factory) {
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

    private static void warnSheetMissing(DataFile dataFile, String sheetKey) {
        String msg = "[WARNING] '" + sheetKey + "' sheet is missing.";
        System.out.println(msg);
        dataFile.getLogger().println(msg);
    }

    // Factory interfaces for generator creation
    interface GeneratorFactory {
        BaseGenerator create(DataFile dataFile, String status);
    }

    static class CodeBookSlotGeneratorFactory implements GeneratorFactory {
        public BaseGenerator create(DataFile dataFile, String status) {
            return new CodeBookSlotGenerator(dataFile);
        }
    }

    static class ActuatorGeneratorFactory implements GeneratorFactory {
        public BaseGenerator create(DataFile dataFile, String status) {
            return new ActuatorGenerator(dataFile, status);
        }
    }

    static class DetectorGeneratorFactory implements GeneratorFactory {
        public BaseGenerator create(DataFile dataFile, String status) {
            return new DetectorGenerator(dataFile, status);
        }
    }
}
