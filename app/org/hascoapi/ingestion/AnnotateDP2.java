package org.hascoapi.ingestion;

import java.util.Map;

import org.hascoapi.entity.pojo.DataFile;

public class AnnotateDP2 extends BaseAnnotator {

    public static GeneratorChain exec(DataFile dataFile, String templateFile, String status) {
        System.out.println("Processing DP2 meta-template ...");

        Map<String, String> mapCatalog = loadCatalog(dataFile);
        if (mapCatalog == null) {
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

