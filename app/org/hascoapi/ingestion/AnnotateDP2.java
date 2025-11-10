package org.hascoapi.ingestion;

import java.util.Map;
import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.DataFile;

public class AnnotateDP2 extends BaseAnnotator {

    public static GeneratorChain exec(DataFile dataFile, String templateFile, String status) {
        dataFile.getLogger().println("Processing DP2 meta-template ...");

        // Load catalog with sheet validation
        Map<String, String> mapCatalog = loadCatalog(dataFile, Constants.MT_DP2);
        if (mapCatalog == null) {
          //  dataFile.getLogger().printExceptionById("DP2_00001"); // "DP2 InfoSheet validation failed"
            return null;
        }

        IngestionWorker.nameSpaceGen(dataFile, mapCatalog, templateFile);
        IngestionWorker.messageGen(dataFile, mapCatalog, templateFile);
        IngestionWorker.deployInstancesGen(dataFile, mapCatalog, templateFile);

        GeneratorChain chain = new GeneratorChain();

        addCustomGeneratorIfSheetExists(dataFile, mapCatalog, "PlatformModels", status, chain,
                (df, st) -> new DP2Generator("platform", df));
        addCustomGeneratorIfSheetExists(dataFile, mapCatalog, "Platforms", status, chain,
                (df, st) -> new DP2Generator("platforminstance", df));
        addCustomGeneratorIfSheetExists(dataFile, mapCatalog, "FieldsOfView", status, chain,
                (df, st) -> new DP2Generator("fieldofview", df));
        addCustomGeneratorIfSheetExists(dataFile, mapCatalog, "Deployments", status, chain,
                (df, st) -> new DP2Generator("deployment", df));

        return chain;
    }
}
