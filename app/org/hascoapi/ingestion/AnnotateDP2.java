package org.hascoapi.ingestion;

import java.util.*;

import org.apache.jena.query.*;
import org.hascoapi.Constants;
import org.hascoapi.RepositoryInstance;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.ConfigProp;
import org.hascoapi.utils.MTSheet;
import org.hascoapi.utils.SPARQLUtils;

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

        // Validate DP2 instance references
        if (!validateDP2Instances(dataFile, mapCatalog)) {
            dataFile.getLogger().printWarningById("DP2_00006");
            return null;
        }
        GeneratorChain chain = new GeneratorChain();

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
        System.out.println("\n========== AnnotateDP2.validateDP2Instances() START ==========");
        Map<String, String> validations = new HashMap<>();
        validations.put("PlatformInstances", "hasPlatform"); //hasPlatform
        validations.put("InstrumentInstances", "hasInstrument"); //hasInstrument
        validations.put("ComponentInstances", "hasInstrument"); //hasInstrument

        String sparqlService = "http://0.0.0.0:3030/store/sparql";
        System.out.println("SPARQL Endpoint: " + sparqlService);

        boolean allValid = true;

        for (Map.Entry<String, String> entry : validations.entrySet()) {
            String sheetKey = entry.getKey();
            String property = entry.getValue();
            String sheetName = mapCatalog.get(sheetKey);

            System.out.println("\n--- Validating Sheet: " + sheetKey + " ---");
            System.out.println("  Property to check: " + property);
            System.out.println("  Sheet name from catalog: " + sheetName);

            if (sheetName == null) {
                System.out.println("  ℹ Sheet not in catalog, skipping");
                continue;
            }

            RecordFile sheet = new SpreadsheetRecordFile(dataFile.getFile(), sheetName.replace("#", ""));
            if (!sheet.isValid()) {
                System.out.println("  ℹ Sheet is not valid, skipping");
                continue;
            }

            System.out.println("  Sheet is valid, processing records...");
            Set<String> urisToCheck = new HashSet<>();
            int recordNum = 0;
            for (Record record : sheet.getRecords()) {
                recordNum++;
                String refUri = record.getValueByColumnName(property);
                System.out.println("  Record " + recordNum + ": [" + property + "] = [" + refUri + "]");
                if (refUri != null && !refUri.trim().isEmpty()) {
                    refUri = refUri.trim();
                    if (!refUri.startsWith("<")) {
                        refUri = "<" + refUri + ">";
                    }
                    refUri = refUri.replaceAll("\\s+", "");
                    urisToCheck.add(refUri);
                    System.out.println("    ✓ Added to validation set: " + refUri);
                } else {
                    System.out.println("    ✗ Skipped (null or empty)");
                }
            }

            if (urisToCheck.isEmpty()) {
                System.out.println("  ℹ No URIs found in sheet " + sheetKey + " to validate.");
                continue;
            }

            System.out.println("  Total URIs to check: " + urisToCheck.size());
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("SELECT ?uri WHERE { VALUES ?uri { ");
            for (String uri : urisToCheck) {
                queryBuilder.append(uri).append(" ");
            }
            queryBuilder.append("} ?uri ?p ?o . }");

            String queryString = queryBuilder.toString();
            System.out.println("\n  Executing SPARQL query:");
            System.out.println("  " + queryString);

            try {
                ResultSetRewindable results = SPARQLUtils.select(sparqlService, queryString);
                System.out.println("  Results obtained, checking each URI...");

                for (String uri : urisToCheck) {
                    boolean found = false;
                    results.reset();
                    while (results.hasNext()) {
                        var qs = results.next();
                        if (qs.contains("uri") && qs.getResource("uri").getURI().equals(uri.substring(1, uri.length() - 1))) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        System.out.println("  ✗ NOT FOUND: " + uri);
                        dataFile.getLogger().printWarningByIdWithArgs(
                                "DP2_00007",
                                String.format("Reference %s in sheet %s does not exist in the repository.", uri, sheetKey)
                        );
                        allValid = false;
                    } else {
                        System.out.println("  ✓ FOUND: " + uri);
                    }
                }

            } catch (Exception e) {
                System.out.println("[ERROR] Exception during SPARQL query execution: " + e.getMessage());
                e.printStackTrace();
                allValid = false;
            }
        }
        System.out.println("\n========== AnnotateDP2.validateDP2Instances() END ==========");
        System.out.println("All valid: " + allValid);
        return allValid;
    }








}
