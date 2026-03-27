package org.hascoapi.ingestion;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.DataFile;

import java.util.Map;

public class AnnotateWKF extends BaseAnnotator {

    public static GeneratorChain exec(DataFile dataFile, String templateFile, String status) {
        System.out.println("\n========== AnnotateWKF.exec() START ==========");
        System.out.println("DataFile URI: " + dataFile.getUri());
        System.out.println("DataFile Filename: " + dataFile.getFilename());
        System.out.println("Template File: " + templateFile);
        System.out.println("Status: " + status);

        dataFile.getLogger().addLine("Processing WKF meta-template...");

        // Load catalog with sheet validation
        System.out.println("→ Loading catalog...");
        Map<String, String> mapCatalog = loadCatalog(dataFile, Constants.MT_WKF);

        if (mapCatalog == null) {
            System.err.println("❌ Failed to load catalog - mapCatalog is null");
            dataFile.getLogger().printExceptionById("WKF_00020");
            return null;
        }

        System.out.println("✓ Catalog loaded successfully with " + mapCatalog.size() + " sheets:");
        for (Map.Entry<String, String> entry : mapCatalog.entrySet()) {
            System.out.println("  - Sheet: [" + entry.getKey() + "] → URI: [" + entry.getValue() + "]");
        }

        // Generate namespace and messages
        System.out.println("→ Generating namespaces...");
        boolean okNS = IngestionWorker.nameSpaceGen(dataFile, mapCatalog, templateFile);
        System.out.println("→ Generating messages...");
        boolean okMsg = IngestionWorker.messageGen(dataFile, mapCatalog, templateFile, status);

        if (!okNS) {
            System.err.println("❌ Namespace generation failed");
            dataFile.getLogger().printExceptionById("WKF_00005");
            return null;
        }

        if (!okMsg) {
            System.err.println("❌ Message generation failed");
            dataFile.getLogger().printExceptionById("WKF_00006");
            return null;
        }

        System.out.println("✓ Namespaces and messages generated successfully");
        dataFile.getLogger().addLine("WKF: Namespaces and messages generated successfully");

        // Build the generator chain for WKF sheets
        System.out.println("→ Building generator chain...");
        GeneratorChain chain = new GeneratorChain();
        int generatorCount = 0;

        for (String sheet : mapCatalog.keySet()) {
            System.out.println("  Processing sheet: [" + sheet + "]");

            if ("InfoSheet".equalsIgnoreCase(sheet) || "Namespaces".equalsIgnoreCase(sheet)) {
                System.out.println("    → Skipping metadata sheet");
                continue; // Skip metadata sheets
            }

            if ("ProcessStems".equalsIgnoreCase(sheet)) {
                System.out.println("    → Adding ProcessStem generator");
                addCustomGeneratorIfSheetExists(dataFile, mapCatalog, sheet, status, chain,
                        (df, st) -> new WKFGenerator("processstem", df, st));
                generatorCount++;

            } else if ("Processes".equalsIgnoreCase(sheet)) {
                System.out.println("    → Adding Process generator");
                addCustomGeneratorIfSheetExists(dataFile, mapCatalog, sheet, status, chain,
                        (df, st) -> new WKFGenerator("process", df, st));
                generatorCount++;

            } else if ("Tasks".equalsIgnoreCase(sheet)) {
                System.out.println("    → Adding Task generator");
                addCustomGeneratorIfSheetExists(dataFile, mapCatalog, sheet, status, chain,
                        (df, st) -> new WKFGenerator("task", df, st));
                generatorCount++;

            } else if ("RequiredInstruments".equalsIgnoreCase(sheet)) {
                System.out.println("    → Adding RequiredInstrument generator");
                addCustomGeneratorIfSheetExists(dataFile, mapCatalog, sheet, status, chain,
                        (df, st) -> new WKFGenerator("requiredinstrument", df, st));
                generatorCount++;

            } else {
                // Unknown sheet - log warning but continue
                System.out.println("    ⚠️ Unknown sheet, logging warning");
                dataFile.getLogger().printWarningByIdWithArgs("WKF_00008", sheet);
            }
        }

        System.out.println("✓ Generator chain built with " + generatorCount + " generators");

        // Set the named graph URI so data is stored in the DataFile's graph
        chain.setNamedGraphUri(dataFile.getUri());
        System.out.println("✓ Named graph URI set to: " + dataFile.getUri());

        // Validate that at least one generator was added
        chain.setDataFile(dataFile);
        if (!chain.isValid()) {
            System.err.println("❌ Generator chain is invalid");
            dataFile.getLogger().printExceptionById("WKF_00007");
            return null;
        }

        System.out.println("✓ WKF: Generator chain validated successfully");
        System.out.println("========== AnnotateWKF.exec() END (SUCCESS) ==========\n");
        return chain;
    }
}
