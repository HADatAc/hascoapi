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
     *
     * @param dataFile     The input data file to annotate.
     * @param studyUri     The study URI.
     * @param templateFile The SSD meta-template reference (unused).
     * @param status       Optional status string for logging.
     * @return A GeneratorChain representing the annotation pipeline or null if failure occurs.
     */
    public static GeneratorChain exec(DataFile dataFile, String studyUri, String templateFile, String status) {
        System.out.println("Processing DGS's SSD meta-template ...");

        Map<String, String> mapCatalog = loadCatalog(dataFile);
        if (mapCatalog == null) {
            return null;
        }

        String namespace =  mapCatalog.get("hasStudyKG");
        System.out.println("AnnotateSSD: namespace value is [" + namespace + "]");

        RecordFile ssdRecordFile = extractSSDRecordFile(dataFile, mapCatalog.get("hasEntityDesign"));
        if (ssdRecordFile == null || ssdRecordFile.getRecords().isEmpty()) {
            System.out.println("[WARNING] SSD sheet is empty or invalid.");
            dataFile.getLogger().println("[WARNING] SSD sheet is empty or invalid.");
            return new SSDGeneratorChain(); // Return empty chain to allow fallback
        }

        dataFile.setRecordFile(ssdRecordFile);
        SSDSheet ssd = new SSDSheet(dataFile);
        mapCatalog = ssd.getCatalog();
        Map<String, List<String>> mapContent = ssd.getMapContent();

        SSDGeneratorChain chain = new SSDGeneratorChain();
        chain.setNamedGraphUri(dataFile.getUri());

        if (!validateSSDStructure(dataFile, ssdRecordFile, studyUri, chain, namespace)) {
            return null;
        }

        Study study = Study.find(studyUri);
        if (study == null) {
            dataFile.getLogger().printExceptionByIdWithArgs("SSD_00005", studyUri);
            return null;
        }

        chain.setStudyUri(URIUtils.replacePrefixEx(studyUri));
        dataFile.getLogger().println("SSD ingestion: The study URI [" + studyUri + "] is in the triple store.");
        System.out.println("IngestionWorker: Pre-processing StudyObjectGenerator. Study ID: " + study.getId());

        for (String sheetKey : mapCatalog.keySet()) {
            addStudyObjectGenerator(sheetKey, mapCatalog, mapContent, dataFile, chain, study, namespace);
        }

        System.out.println("SSD Processing: Completed GeneratorChain.");
        return chain;
    }

    private static RecordFile extractSSDRecordFile(DataFile dataFile, String sheetNameRaw) {
        if (!dataFile.getFilename().endsWith(".xlsx")) {
            System.err.println("[ERROR] IngestionWorker: DSG file must have a .xlsx extension.");
            return null;
        }

        if (sheetNameRaw == null || sheetNameRaw.isEmpty()) {
            System.err.println("[ERROR] IngestionWorker: Missing sheet name for [hasEntityDesign].");
            return null;
        }

        try {
            String sheetName = sheetNameRaw.replace("#", "");
            System.out.print("Extracting SSD sheet... ");
            RecordFile ssdRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), sheetName);

            if (ssdRecordFile == null || ssdRecordFile.getRecords() == null) {
                System.err.println("[ERROR] Failed to load SSD sheet.");
                return null;
            }

            System.out.println("[" + ssdRecordFile.getRecords().size() + "] rows extracted.");
            return ssdRecordFile;

        } catch (Exception e) {
            System.err.println("[ERROR] Exception during SSD sheet extraction: " + e.getMessage());
            return null;
        }
    }

    /* 
     *  Verifies if the SSD contains exactly one SOC that is of type SubjectGroup.  
     */
    private static boolean validateSSDStructure(DataFile dataFile, RecordFile ssdRecordFile, String studyUri, SSDGeneratorChain chain, String namespace) {
        if (!ssdRecordFile.isValid()) {
            dataFile.getLogger().printException("SSD sheet is invalid.");
            return false;
        }

        System.out.println("SSD Processing: Adding VirtualColumnGenerator.");
        VirtualColumnGenerator vcgen = new VirtualColumnGenerator(dataFile);
        vcgen.setStudyUri(studyUri);
        chain.addGenerator(vcgen);

        System.out.println("SSD Processing: Adding SSDGenerator.");
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
            System.err.println("[ERROR] SSD Processing: No SubjectGroup found.");
            dataFile.getLogger().printExceptionById("SSD_00006");
            return false;
        }

        if (subjectGroupCount > 1) {
            System.err.println("[ERROR] SSD Processing: Multiple SubjectGroups found.");
            dataFile.getLogger().printExceptionById("SSD_00007");
            return false;
        }

        return true;
    }

    private static void addStudyObjectGenerator(
            String key,
            Map<String, String> catalog,
            Map<String, List<String>> content,
            DataFile dataFile,
            SSDGeneratorChain chain,
            Study study,
            String namespace
    ) {
        String sheetName = catalog.get(key);
        if (sheetName == null || sheetName.isEmpty()) return;

        try {
            System.out.println("Pre-processing SOC [" + sheetName + "]");
            RecordFile sheet = new SpreadsheetRecordFile(dataFile.getFile(), sheetName.replace("#", ""));

            DataFile clonedFile = (DataFile) dataFile.clone();
            clonedFile.setRecordFile(sheet);

            List<String> headers = content.get(key);
            if (headers == null) {
                dataFile.getLogger().printException("No content for key [" + key + "]");
                return;
            }

            System.out.println("Adding StudyObjectGenerator...");
            chain.addGenerator(new StudyObjectGenerator(clonedFile, headers, content, chain.getStudyUri(), study.getId(), namespace));

        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
    }
}
