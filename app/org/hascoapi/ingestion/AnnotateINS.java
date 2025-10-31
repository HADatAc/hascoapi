package org.hascoapi.ingestion;

import java.util.Map;
import org.hascoapi.entity.pojo.DataFile;

public class AnnotateINS extends BaseAnnotator {

    public static GeneratorChain exec(DataFile dataFile, String templateFile, String status) {
        System.out.println("Processing INS meta-template ...");

        // Load catalog with sheet validation
        Map<String, String> mapCatalog = loadCatalog(dataFile, "INS");
        if (mapCatalog == null) {
            System.out.println("[ERROR] INS InfoSheet validation failed. Aborting annotation.");
            return null; // Abort if InfoSheet is invalid or has missing/extra sheets
        }

        // Generate basic entities
        IngestionWorker.nameSpaceGen(dataFile, mapCatalog, templateFile);
        IngestionWorker.annotationGen(dataFile, mapCatalog, templateFile, status);

        // Build generator chain for custom sheets
        GeneratorChain chain = new GeneratorChain();

        addCustomGeneratorIfSheetExists(dataFile, mapCatalog, "ResponseOptions", status, chain,
                (df, st) -> new INSGenerator("responseoption", df, st));
        addCustomGeneratorIfSheetExists(dataFile, mapCatalog, "CodeBooks", status, chain,
                (df, st) -> new INSGenerator("codebook", df, st));
        addCustomGeneratorIfSheetExists(dataFile, mapCatalog, "CodeBookSlots", status, chain,
                new CodeBookSlotGeneratorFactory());
        addCustomGeneratorIfSheetExists(dataFile, mapCatalog, "ComponentStems", status, chain,
                (df, st) -> new INSGenerator("componentstem", df, st));
        addCustomGeneratorIfSheetExists(dataFile, mapCatalog, "Components", status, chain,
                new ComponentGeneratorFactory());
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

    static class ComponentGeneratorFactory implements GeneratorFactory {
        public BaseGenerator create(DataFile dataFile, String status) {
            return new ComponentGenerator(dataFile, status);
        }
    }
}
