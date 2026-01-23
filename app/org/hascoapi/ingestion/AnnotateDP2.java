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
        boolean okMsg = IngestionWorker.messageGen(dataFile, mapCatalog, templateFile);
        boolean okDeploy = IngestionWorker.deployInstancesGen(dataFile, mapCatalog, templateFile);

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
                        (df, st) -> new DP2Generator("platform", df));

            } else if ("PlatformInstances".equalsIgnoreCase(sheet)) {
                // PlatformInstances = platforminstance
                addCustomGeneratorIfSheetExists(dataFile, mapCatalog, sheet, status, chain,
                        (df, st) -> new DP2Generator("platforminstance", df));

            } else if ("FieldsOfView".equalsIgnoreCase(sheet)) {
                addCustomGeneratorIfSheetExists(dataFile, mapCatalog, sheet, status, chain,
                        (df, st) -> new DP2Generator("fieldofview", df));

            } else if ("Deployments".equalsIgnoreCase(sheet)) {
                addCustomGeneratorIfSheetExists(dataFile, mapCatalog, sheet, status, chain,
                        (df, st) -> new DP2Generator("deployment", df));

            } else if ("InstrumentInstances".equalsIgnoreCase(sheet)) {
                addCustomGeneratorIfSheetExists(dataFile, mapCatalog, sheet, status, chain,
                        (df, st) -> new DP2Generator("instrumentinstance", df));

            } else if ("ComponentInstances".equalsIgnoreCase(sheet)) {
                addCustomGeneratorIfSheetExists(dataFile, mapCatalog, sheet, status, chain,
                        (df, st) -> new DP2Generator("componentinstance", df));

            } else if ("SensingPerspective".equalsIgnoreCase(sheet)) {
                addCustomGeneratorIfSheetExists(dataFile, mapCatalog, sheet, status, chain,
                        (df, st) -> new DP2Generator("sensingperspective", df));
            }
        }

        return chain;
    }

    private static boolean validateDP2Instances(DataFile dataFile, Map<String, String> mapCatalog) {
        Map<String, String> validations = new HashMap<>();
        validations.put("PlatformInstances", "hasPlatform"); //hasPlatform
        validations.put("InstrumentInstances", "hasInstrument"); //hasInstrument
        validations.put("ComponentInstances", "hasInstrument"); //hasInstrument

        String sparqlService = "http://0.0.0.0:3030/store/sparql";

        boolean allValid = true;

        for (Map.Entry<String, String> entry : validations.entrySet()) {
            String sheetKey = entry.getKey();
            String property = entry.getValue();
            String sheetName = mapCatalog.get(sheetKey);

            System.out.println("SPARQL Endpoint: " + sparqlService + "\tSheet: " + sheetName + "\tProperty: " + property);

            if (sheetName == null) continue;

            RecordFile sheet = new SpreadsheetRecordFile(dataFile.getFile(), sheetName.replace("#", ""));
            if (!sheet.isValid()) continue;

            Set<String> urisToCheck = new HashSet<>();
            for (Record record : sheet.getRecords()) {
                String refUri = record.getValueByColumnName(property);
                if (refUri != null && !refUri.trim().isEmpty()) {
                    refUri = refUri.trim();
                    if (!refUri.startsWith("<")) {
                        refUri = "<" + refUri + ">";
                    }
                    refUri = refUri.replaceAll("\\s+", "");
                    urisToCheck.add(refUri);
                }
            }

            if (urisToCheck.isEmpty()) {
                System.out.println("[INFO] No URIs found in sheet " + sheetKey + " to validate.");
                continue;
            }

            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("SELECT ?uri WHERE { VALUES ?uri { ");
            for (String uri : urisToCheck) {
                queryBuilder.append(uri).append(" ");
            }
            queryBuilder.append("} ?uri ?p ?o . }");

            String queryString = queryBuilder.toString();
            System.out.println("Doing the query");
            System.out.println("Query: " + queryString);

            try {
                ResultSetRewindable results = SPARQLUtils.select(sparqlService, queryString);
                System.out.println("Results obtained, checking each URI...");

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
                        System.out.println("No results found for " + uri);
                        dataFile.getLogger().printWarningByIdWithArgs(
                                "DP2_00007",
                                String.format("Reference %s in sheet %s does not exist in the repository.", uri, sheetKey)
                        );
                        allValid = false;
                    }
                }

            } catch (Exception e) {
                System.out.println("[ERROR] Exception during SPARQL query execution: " + e.getMessage());
                e.printStackTrace();
                allValid = false;
            }
        }
        System.out.println("All valid: " + allValid);
        return allValid;
    }








}
