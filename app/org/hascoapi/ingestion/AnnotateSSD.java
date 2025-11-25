package org.hascoapi.ingestion;

import java.util.List;
import java.util.Map;

import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.SSDSheet;
import org.hascoapi.entity.pojo.Study;
import org.hascoapi.utils.URIUtils;

public class AnnotateSSD extends BaseAnnotator {

    /**
     * Processes an SSD (Semantic Study Design) file and produces a generator chain.
     */
    public static GeneratorChain exec(DataFile dataFile, String studyUri, String templateFile, String status) {
        dataFile.getLogger().println("Processing DSG's SSD meta-template ...");

        Map<String, String> mapCatalog = loadCatalog(dataFile, "SSD");
        if (mapCatalog == null) {
            // loadCatalog already registered the specific DSG error.
            return null;
        }

        String namespace = mapCatalog.get("hasStudyKG");
        dataFile.getLogger().println("AnnotateSSD: namespace value is [" + namespace + "]");

        RecordFile ssdRecordFile = extractSSDRecordFile(dataFile, mapCatalog.get("hasEntityDesign"));
        if (ssdRecordFile == null || ssdRecordFile.getRecords().isEmpty()) {
            // SSD sheet empty / invalid -> use DSG warning
            dataFile.getLogger().printWarningById("DSG_00013");
            return new SSDGeneratorChain(); // Return empty chain to allow fallback
        }

        dataFile.setRecordFile(ssdRecordFile);
        SSDSheet ssd = new SSDSheet(dataFile);
        mapCatalog = ssd.getCatalog();
        Map<String, List<String>> mapContent = ssd.getMapContent();
        Map<String, String> mapReferences = ssd.getMapReferences();

        SSDGeneratorChain chain = new SSDGeneratorChain();
        chain.setNamedGraphUri(dataFile.getUri());

        if (!validateSSDStructure(dataFile, ssdRecordFile, studyUri, chain, namespace)) {
            return null;
        }

        Study study = Study.find(studyUri);
        if (study == null) {
            // Use DSG error for study not found (argument = studyUri)
            dataFile.getLogger().printExceptionByIdWithArgs("DSG_00010", studyUri);
            return null;
        }

        //chain.setStudyUri(URIUtils.replacePrefixEx(studyUri));
        chain.setStudyUri(studyUri);
        dataFile.getLogger().println("DSG ingestion: The study URI [" + studyUri + "] is in the triple store.");
        dataFile.getLogger().println("AnnotateSSD: Pre-processing StudyObjectGenerator. Study URI: " + study.getUri());

        dataFile.getLogger().println("AnnotateSSD: Catalog size: " + mapCatalog.size());
        for (String sheetKey : mapCatalog.keySet()) {
            addStudyObjectGenerator(sheetKey, mapCatalog, mapContent, mapReferences, dataFile, chain, study, namespace);
        }

        dataFile.getLogger().println("SSD Processing: Completed GeneratorChain.");
        return chain;
    }

    private static RecordFile extractSSDRecordFile(DataFile dataFile, String sheetNameRaw) {
        if (!dataFile.getFilename().endsWith(".xlsx")) {
            // Use DSG exception with filename as argument
            dataFile.getLogger().printExceptionByIdWithArgs("DSG_00006", dataFile.getFilename());
            return null;
        }

        if (sheetNameRaw == null || sheetNameRaw.isEmpty()) {
            // Missing sheet name for hasEntityDesign
            dataFile.getLogger().printExceptionById("DSG_00007");
            return null;
        }

        try {
            String sheetName = sheetNameRaw.replace("#", "");
            dataFile.getLogger().println("Extracting SSD sheet...");
            RecordFile ssdRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), sheetName);

            if (ssdRecordFile == null || ssdRecordFile.getRecords() == null) {
                // Failed to load sheet
                dataFile.getLogger().printExceptionById("DSG_00008");
                return null;
            }

            dataFile.getLogger().println("[" + ssdRecordFile.getRecords().size() + "] rows extracted.");
            return ssdRecordFile;

        } catch (Exception e) {
            // Exception during extraction -> include message as argument
            dataFile.getLogger().printExceptionByIdWithArgs("DSG_00009", e.getMessage());
            return null;
        }
    }

    /*
     *  Verifies if the SSD contains exactly one SOC that is of type SubjectGroup.
     */
    private static boolean validateSSDStructure(DataFile dataFile, RecordFile ssdRecordFile, String studyUri, SSDGeneratorChain chain, String namespace) {
        if (!ssdRecordFile.isValid()) {
            dataFile.getLogger().printExceptionById("DSG_00014");
            return false;
        }

        dataFile.getLogger().println("SSD Processing: Adding VirtualColumnGenerator.");
        VirtualColumnGenerator vcgen = new VirtualColumnGenerator(dataFile);
        vcgen.setStudyUri(studyUri);
        chain.addGenerator(vcgen);

        dataFile.getLogger().println("SSD Processing: Adding SSDGenerator.");
        SSDGenerator socgen = new SSDGenerator(dataFile, namespace);
        socgen.setStudyUri(studyUri);
        chain.addGenerator(socgen);

        int subjectGroupCount = 0;
        for (Record record : ssdRecordFile.getRecords()) {
            String socType = record.getValueByColumnIndex(2);
            if (socType != null && socType.contains("SubjectGroup")) {
                subjectGroupCount++;
            }
        }

        if (subjectGroupCount == 0) {
            // Use DSG error for missing SubjectGroup
            dataFile.getLogger().printExceptionById("DSG_00011");
            return false;
        }

        if (subjectGroupCount > 1) {
            // Use DSG error for multiple SubjectGroups
            dataFile.getLogger().printExceptionById("DSG_00012");
            return false;
        }

        return true;
    }

    private static void addStudyObjectGenerator(
            String key,
            Map<String, String> catalog,
            Map<String, List<String>> content,
            Map<String, String> references,
            DataFile dataFile,
            SSDGeneratorChain chain,
            Study study,
            String namespace
    ) {
        String sheetName = catalog.get(key);
        if (sheetName == null || sheetName.isEmpty()) return;

        try {
            dataFile.getLogger().println("Pre-processing SOC [" + sheetName + "]");
            RecordFile sheet = new SpreadsheetRecordFile(dataFile.getFile(), sheetName.replace("#", ""));

            DataFile clonedFile = (DataFile) dataFile.clone();
            clonedFile.setRecordFile(sheet);

            List<String> headers = content.get(key);
            if (headers == null) {
                dataFile.getLogger().printExceptionByIdWithArgs("DSG_00015", key);
                return;
            }

            dataFile.getLogger().println("Adding StudyObjectGenerator...");
            chain.addGenerator(new StudyObjectGenerator(clonedFile, headers, content, references, chain.getStudyUri(), study.getId(), namespace));

        } catch (CloneNotSupportedException e) {
            dataFile.getLogger().printExceptionByIdWithArgs("DSG_00016", e.getMessage());
        }
    }
}