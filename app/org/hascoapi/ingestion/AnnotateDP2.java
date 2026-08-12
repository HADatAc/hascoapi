package org.hascoapi.ingestion;

import java.util.*;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.utils.MTSheet;

public class AnnotateDP2 extends BaseAnnotator {

    public static GeneratorChain exec(DataFile dataFile, String templateFile, String status) {
        dataFile.getLogger().println("Processing DP2 meta-template ...");

        // Load catalog with sheet validation
        Map<String, String> mapCatalog = loadCatalog(dataFile, Constants.MT_DP2);
        if (mapCatalog == null) {
            return null;
        }

        // Generate namespace, messages, and deploy instances
        boolean okNS = IngestionWorker.nameSpaceGen(dataFile, mapCatalog, templateFile);
        boolean okMsg = IngestionWorker.messageGen(dataFile, mapCatalog, templateFile, status);
        boolean okDeploy = IngestionWorker.deployInstancesGen(dataFile, mapCatalog, templateFile, status);

        // If any of the pre-generators failed, stop here to avoid downstream NPEs or partial/invalid validation.
        if (!okNS || !okMsg || !okDeploy) {
            dataFile.getLogger().printWarningById("DP2_00006");
            return null;
        }

        // DP2-VERIFY disabled by request: skip verifier execution entirely.
        dataFile.getLogger().println("[DP2-VERIFY-SUMMARY] validation=disabled, continuation_policy=skipped, ingestion_continues=true");
        GeneratorChain chain = new GeneratorChain();

        // Set the named graph URI so data is stored in the DataFile's graph
        chain.setNamedGraphUri(dataFile.getUri());

        // Get all sheet names for DP2 from MTSheet
        List<String> dp2Sheets = MTSheet.getSheetsForType(Constants.MT_DP2);

        for (String sheet : dp2Sheets) {
            if ("Platforms".equalsIgnoreCase(sheet)) {
                // Platforms = platform (types)
                addCustomGeneratorIfSheetExists(dataFile, mapCatalog, sheet, status, chain,
                        (df, st) -> new DP2Generator("platform", df, st));

            } else if ("PlatformInstances".equalsIgnoreCase(sheet)) {
                // PlatformInstances = platforminstance
                addCustomGeneratorIfSheetExists(dataFile, mapCatalog, sheet, status, chain,
                        (df, st) -> new DP2Generator("platforminstance", df, st));

            } else if ("FieldsOfView".equalsIgnoreCase(sheet)) {
                addCustomGeneratorIfSheetExists(dataFile, mapCatalog, sheet, status, chain,
                        (df, st) -> new DP2Generator("fieldofview", df, st));

            } else if ("Deployments".equalsIgnoreCase(sheet)) {
                addCustomGeneratorIfSheetExists(dataFile, mapCatalog, sheet, status, chain,
                        (df, st) -> new DP2Generator("deployment", df, st));

            } else if ("ComponentDeployments".equalsIgnoreCase(sheet)) {
                addCustomGeneratorIfSheetExists(dataFile, mapCatalog, sheet, status, chain,
                        (df, st) -> new ComponentDeploymentGenerator(df));

            } else if ("InstrumentInstances".equalsIgnoreCase(sheet)) {
                addCustomGeneratorIfSheetExists(dataFile, mapCatalog, sheet, status, chain,
                        (df, st) -> new DP2Generator("instrumentinstance", df, st));

            } else if ("ComponentInstances".equalsIgnoreCase(sheet)) {
                addCustomGeneratorIfSheetExists(dataFile, mapCatalog, sheet, status, chain,
                        (df, st) -> new DP2Generator("componentinstance", df, st));

            } else if ("SensingPerspective".equalsIgnoreCase(sheet)) {
                addCustomGeneratorIfSheetExists(dataFile, mapCatalog, sheet, status, chain,
                        (df, st) -> new DP2Generator("sensingperspective", df, st));
            }
        }

        return chain;
    }

    private static boolean validateDP2Instances(DataFile dataFile, Map<String, String> mapCatalog) {
        return new DP2WorkbookVerifier(dataFile, mapCatalog).verify();
    }

}
