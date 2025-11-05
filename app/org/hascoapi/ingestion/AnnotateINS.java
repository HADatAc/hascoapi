package org.hascoapi.ingestion;

import java.util.Map;
import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.DataFile;

public class AnnotateINS extends BaseAnnotator {

    public static GeneratorChain exec(DataFile dataFile, String templateFile, String status) {
        dataFile.getLogger().println("Processing INS meta-template ...");

        // Load catalog with sheet validation
        Map<String, String> mapCatalog = loadCatalog(dataFile, Constants.MT_INS);
        if (mapCatalog == null) {
            dataFile.getLogger().printExceptionById("INS_00001"); // "INS InfoSheet validation failed"
            return null;
        }

        // Namespace and annotation generation
        IngestionWorker.nameSpaceGen(dataFile, mapCatalog, templateFile);
        IngestionWorker.annotationGen(dataFile, mapCatalog, templateFile, status);

        // Build generator chain
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
