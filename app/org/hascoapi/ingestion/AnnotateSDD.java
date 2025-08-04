package org.hascoapi.ingestion;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.SDD;
import org.hascoapi.utils.URIUtils;

public class AnnotateSDD extends BaseAnnotator {

    public static GeneratorChain exec(DataFile dataFile, String templateFile) {
        System.out.println("Processing SDD meta-template ...");

        // Load the InfoSheet catalog and set it on dataFile
        Map<String, String> mapCatalog = loadCatalog(dataFile);
        if (mapCatalog == null) {
            return null; // loading failed, error already logged
        }

        String sddUri = dataFile.getUri().replace("DFL", "SDDICT");

        // Check required fields in catalog
        if (mapCatalog.getOrDefault("SDD_ID", "").isEmpty()) {
            dataFile.getLogger().printExceptionById("SDD_00003");
            return null;
        }
        String sddId = mapCatalog.get("SDD_ID");

        if (mapCatalog.getOrDefault("Version", "").isEmpty()) {
            dataFile.getLogger().printExceptionById("SDD_00018");
            return null;
        }
        String sddVersion = mapCatalog.get("Version");

        // Create SDD instance early for generator use
        SDD sdd = new SDD(dataFile, templateFile);

        // Download and read Code Mappings if available
        File codeMappingFile = null;
        if (dataFile.getFilename().endsWith(".xlsx") && mapCatalog.get("Code_Mappings") != null) {
            codeMappingFile = sdd.downloadFile(
                    mapCatalog.get("Code_Mappings"),
                    "sddtmp/" + dataFile.getFilename().replace(".xlsx", "") + "-code-mappings.csv");
        }

        if (codeMappingFile != null) {
            RecordFile codeMappingRecordFile = new CSVRecordFile(codeMappingFile);
            if (!sdd.readCodeMapping(codeMappingRecordFile)) {
                dataFile.getLogger().printWarningById("SDD_00016");
            } else {
                dataFile.getLogger().println("Codemappings loaded from " + dataFile.getFilename());
            }
        } else {
            dataFile.getLogger().printWarningById("SDD_00017");
        }

        GeneratorChain chain = new GeneratorChain();
        chain.setNamedGraphUri(dataFile.getUri());
        chain.setPV(true);

        // Add Data Dictionary generators if sheet exists
        addCustomGeneratorIfSheetExists(dataFile, mapCatalog, "Data_Dictionary", "", chain, (df, status) -> {
            try {
                // Clone dataFile to avoid side effects
                DataFile clonedFile = (DataFile) df.clone();
                SDD localSdd = new SDD(clonedFile, templateFile);

                // readDataDictionary returns boolean but not used here for generator creation
                localSdd.readDataDictionary(clonedFile.getRecordFile(), clonedFile);

                return new SDDAttributeGenerator(clonedFile, sddUri, sddId, sdd.getCodeMapping(),
                        localSdd.readDDforEAmerge(clonedFile.getRecordFile()), templateFile);
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
                return null;
            }
        });

        addCustomGeneratorIfSheetExists(dataFile, mapCatalog, "Data_Dictionary", "", chain, (df, status) -> {
            try {
                DataFile clonedFile = (DataFile) df.clone();
                return new SDDObjectGenerator(clonedFile, sddUri, sddId, sdd.getCodeMapping());
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
                return null;
            }
        });

        // Add Codebook generator if sheet exists
        addCustomGeneratorIfSheetExists(dataFile, mapCatalog, "Codebook", "", chain, (df, status) -> {
            try {
                DataFile clonedFile = (DataFile) df.clone();
                chain.setCodebookFile(clonedFile);
                chain.setSddName(URIUtils.replacePrefixEx(sddUri));
                return new PVGenerator(clonedFile, sddUri, sddId, sdd.getMapAttrObj(), sdd.getCodeMapping());
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
                return null;
            }
        });

        // General Generator for SemanticDataDictionary
        GeneralGenerator generalGenerator = new GeneralGenerator(dataFile, "SemanticDataDictionary");
        Map<String, Object> row = new HashMap<>();
        row.put("hasURI", sddUri);
        row.put("a", "hasco:SemanticDataDictionary");
        row.put("hasco:hascoType", "hasco:SemanticDataDictionary");
        row.put("rdfs:label", sddId);
        row.put("rdfs:comment", "Generated from SDD file [" + dataFile.getFilename() + "]");
        row.put("vstoi:hasVersion", sddVersion);
        row.put("vstoi:hasSIRManagerEmail", dataFile.getHasSIRManagerEmail());
        generalGenerator.addRow(row);
        chain.addGenerator(generalGenerator);

        dataFile.getLogger().println("This SDD is assigned with uri: " + sddUri + " and is of type hasco:SemanticDataDictionary");

        return chain;
    }

}
