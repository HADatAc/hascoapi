package org.hascoapi.ingestion;

import java.util.Map;

import org.hascoapi.entity.pojo.DataFile;

public class AnnotateSTD extends BaseAnnotator {

    public static GeneratorChain exec(DataFile dataFile, String studyUri, String templateFile) {
        dataFile.getLogger().println("Processing DSG's STD meta-template ...");

        Map<String, String> mapCatalog = loadCatalog(dataFile, "STD");
        if (mapCatalog == null) {
            dataFile.getLogger().printExceptionById("DSG_00017");
            return null;
        }
        
        // Debug: Print all mapCatalog entries
        System.out.println("AnnotateSTD.exec(): mapCatalog contents:");
        for (Map.Entry<String, String> entry : mapCatalog.entrySet()) {
            System.out.println("  " + entry.getKey() + " = " + entry.getValue());
        }
        System.out.println("AnnotateSTD.exec(): hasStudyKG from catalog: " + mapCatalog.get("hasStudyKG"));
        System.out.println("AnnotateSTD.exec(): hasVariableDesign from catalog: " + mapCatalog.get("hasVariableDesign"));
        System.out.println("AnnotateSTD.exec(): hasVersion from catalog: " + mapCatalog.get("hasVersion"));

        // Check file extension
        if (!dataFile.getFilename().endsWith(".xlsx")) {
            dataFile.getLogger().printExceptionByIdWithArgs("DSG_00018", dataFile.getFilename());
            return null;
        }

        // Generate namespace
        IngestionWorker.nameSpaceGen(dataFile, mapCatalog, templateFile);
        dataFile.getLogger().println("Namespace generation completed.");

        GeneratorChain chain = new GeneratorChain();
        chain.setNamedGraphUri(dataFile.getUri());
        dataFile.getLogger().println("Named graph URI set: " + dataFile.getUri());

        // Load hasStudyDescription sheet
        String sheetKey = "hasStudyDescription";
        String sheetName = mapCatalog.get(sheetKey);
        if (sheetName == null || sheetName.trim().isEmpty()) {
            dataFile.getLogger().printExceptionById("DSG_00019");
            return null;
        }

        try {
            dataFile.getLogger().println("Loading sheet [" + sheetName + "] from STD file ...");
            RecordFile studyRecordFile = new SpreadsheetRecordFile(
                    dataFile.getFile(),
                    dataFile.getFilename(),
                    sheetName.replace("#", "")
            );

            if (studyRecordFile == null || studyRecordFile.getRecords() == null) {
                dataFile.getLogger().printExceptionById("DSG_00020");
                return null;
            }

            dataFile.getLogger().println(
                    "STD: Loaded studyRecordFile with [" + studyRecordFile.getRecords().size() + "] rows."
            );
            dataFile.setRecordFile(studyRecordFile);

            // Add generators
            dataFile.getLogger().println("Adding AgentGenerator and StudyGenerator to chain...");
            chain.addGenerator(new AgentGenerator(dataFile, studyUri, templateFile));
            chain.addGenerator(new StudyGenerator(dataFile, studyUri, templateFile));

            // Pass InfoSheet metadata to chain for StudyGenerator
            String hasStudyKG = mapCatalog.get("hasStudyKG");
            String hasVariableDesign = mapCatalog.get("hasVariableDesign");
            String hasVersion = mapCatalog.get("hasVersion");
            
            System.out.println("AnnotateSTD.exec(): Calling setInfoSheetMetadata with:");
            System.out.println("  hasStudyKG: " + hasStudyKG);
            System.out.println("  hasVariableDesign: " + hasVariableDesign);
            System.out.println("  hasVersion: " + hasVersion);
            
            chain.setInfoSheetMetadata(hasStudyKG, hasVariableDesign, hasVersion);

        } catch (Exception e) {
            dataFile.getLogger().printExceptionByIdWithArgs("DSG_00021", e.getMessage());
            return null;
        }

        dataFile.getLogger().println("STD processing completed successfully.");
        return chain;
    }
}
