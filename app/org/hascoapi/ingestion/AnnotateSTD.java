package org.hascoapi.ingestion;

import java.util.Map;

import org.hascoapi.entity.pojo.DataFile;

public class AnnotateSTD extends BaseAnnotator {

    public static GeneratorChain exec(DataFile dataFile, String studyUri, String templateFile) {
        System.out.println("Processing DGS's STD meta-template ...");

        Map<String, String> mapCatalog = loadCatalog(dataFile);
        if (mapCatalog == null) {
            System.out.println("[ERROR] STD: Failed to load InfoSheet.");
            return null;
        }
    
        // Check file extension
        if (!dataFile.getFilename().endsWith(".xlsx")) {
            System.out.println("[ERROR] STD: File must have .xlsx extension.");
            return null;
        }

        // Generate namespace
        IngestionWorker.nameSpaceGen(dataFile, mapCatalog, templateFile);

        GeneratorChain chain = new GeneratorChain();
        chain.setNamedGraphUri(dataFile.getUri());

        // Load hasStudyDescription sheet
        String sheetKey = "hasStudyDescription";
        String sheetName = mapCatalog.get(sheetKey);
        if (sheetName == null || sheetName.trim().isEmpty()) {
            System.out.println("[ERROR] STD: Missing 'hasStudyDescription' sheet in catalog.");
            return null;
        }

        RecordFile studyRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), sheetName.replace("#", ""));
        if (studyRecordFile == null || studyRecordFile.getRecords() == null) {
            System.out.println("[ERROR] STD: Failed to load or parse study description sheet.");
            return null;
        }

        System.out.println("STD: Loaded studyRecordFile with [" + studyRecordFile.getRecords().size() + "] rows.");
        dataFile.setRecordFile(studyRecordFile);

        // Add generators
        chain.addGenerator(new AgentGenerator(dataFile, studyUri, templateFile));
        chain.addGenerator(new StudyGenerator(dataFile, studyUri, templateFile));

        return chain;
    }
}
