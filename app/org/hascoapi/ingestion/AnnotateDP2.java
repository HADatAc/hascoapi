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
            // dataFile.getLogger().printExceptionById("DP2_00001"); // "DP2 InfoSheet validation failed"
            return null;
        }

        // Generate namespace, messages, and deploy instances
        IngestionWorker.nameSpaceGen(dataFile, mapCatalog, templateFile);
        IngestionWorker.messageGen(dataFile, mapCatalog, templateFile);
        IngestionWorker.deployInstancesGen(dataFile, mapCatalog, templateFile);

        // Validate DP2 instance references
        if (!validateDP2Instances(dataFile, mapCatalog)) {
            System.out.println("Erro aqui");
            dataFile.getLogger().printWarningById("DP2_00006"); // “One or more DP2 instance references are invalid”
            return null;
        }
        GeneratorChain chain = new GeneratorChain();

        // Get all sheet names for DP2 from MTSheet
        List<String> dp2Sheets = MTSheet.getSheetsForType(Constants.MT_DP2);

        for (String sheet : dp2Sheets) {
            if ("Platforms".equalsIgnoreCase(sheet)) {
                addCustomGeneratorIfSheetExists(dataFile, mapCatalog, sheet, status, chain,
                        (df, st) -> new DP2Generator("platform", df));

            } else if ("PlatformInstances".equalsIgnoreCase(sheet)) {
                addCustomGeneratorIfSheetExists(dataFile, mapCatalog, sheet, status, chain,
                        (df, st) -> new DP2Generator("platforminstance", df));
                /*
                Adicionar linha de código que caso exista um plataform instance, este deve ser referente a um plataform existente.
                 */
                System.out.println("log teste");
            } else if ("FieldsOfView".equalsIgnoreCase(sheet)) {
                addCustomGeneratorIfSheetExists(dataFile, mapCatalog, sheet, status, chain,
                        (df, st) -> new DP2Generator("fieldofview", df));

            } else if ("Deployments".equalsIgnoreCase(sheet)) {
                addCustomGeneratorIfSheetExists(dataFile, mapCatalog, sheet, status, chain,
                        (df, st) -> new DP2Generator("deployment", df));
            }
        }

        return chain;
    }

    private static boolean validateDP2Instances(DataFile dataFile, Map<String, String> mapCatalog) {
        Map<String, String> validations = new HashMap<>();
        validations.put("PlatformInstances", "a"); //hasPlatform
        validations.put("InstrumentInstances", "a"); //hasInstrument
        validations.put("ComponentInstances", "a"); //hasInstrument

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
