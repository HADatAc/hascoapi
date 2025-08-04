package org.hascoapi.ingestion;

import java.util.Map;

import org.hascoapi.entity.pojo.DataFile;

public class AnnotateINS extends BaseAnnotator {

    public static GeneratorChain exec(DataFile dataFile, String templateFile, String status) {
        System.out.println("Processing INS meta-template ...");

        Map<String, String> mapCatalog = loadCatalog(dataFile);
        if (mapCatalog == null) {
            return null;
        }

        IngestionWorker.nameSpaceGen(dataFile, mapCatalog, templateFile);
        IngestionWorker.annotationGen(dataFile, mapCatalog, templateFile, status);

        GeneratorChain chain = new GeneratorChain();

        // Sheet generators
        addCustomGeneratorIfSheetExists(dataFile, mapCatalog, "ResponseOptions", status, chain,
                (df, st) -> new INSGenerator("responseoption", df, st));
        addCustomGeneratorIfSheetExists(dataFile, mapCatalog, "CodeBooks", status, chain,
                (df, st) -> new INSGenerator("codebook", df, st));
        addCustomGeneratorIfSheetExists(dataFile, mapCatalog, "CodeBookSlots", status, chain,
                new CodeBookSlotGeneratorFactory());
        addCustomGeneratorIfSheetExists(dataFile, mapCatalog, "ActuatorStems", status, chain,
                (df, st) -> new INSGenerator("actuatorstem", df, st));
        addCustomGeneratorIfSheetExists(dataFile, mapCatalog, "Actuators", status, chain,
                new ActuatorGeneratorFactory());
        addCustomGeneratorIfSheetExists(dataFile, mapCatalog, "DetectorStems", status, chain,
                (df, st) -> new INSGenerator("detectorstem", df, st));
        addCustomGeneratorIfSheetExists(dataFile, mapCatalog, "Detectors", status, chain,
                new DetectorGeneratorFactory());
        addCustomGeneratorIfSheetExists(dataFile, mapCatalog, "SlotElements", status, chain,
                (df, st) -> new INSGenerator("slotelement", df, st));
        addCustomGeneratorIfSheetExists(dataFile, mapCatalog, "Instruments", status, chain,
                (df, st) -> new INSGenerator("instrument", df, st));

        return chain;
    }

    // Factories for complex generators
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

