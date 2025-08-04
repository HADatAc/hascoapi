package org.hascoapi.ingestion;

import java.util.Map;

import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.DOI;
import org.hascoapi.entity.pojo.Study;

public class AnnotateDOI extends BaseAnnotator {

    public static GeneratorChain exec(DataFile dataFile) {
        System.out.println("Processing DOI file ...");

        // Load and validate InfoSheet, build catalog
        Map<String, String> mapCatalog = loadCatalog(dataFile);
        if (mapCatalog == null) {
            dataFile.getLogger().printExceptionById("DOI_00001");
            return null;
        }

        DOI doi = new DOI(dataFile);
        GeneratorChain chain = new GeneratorChain();
        chain.setDataFile(dataFile);

        // Study ID validation
        String studyId = doi.getStudyId();
        if (studyId == null || studyId.isEmpty()) {
            dataFile.getLogger().printExceptionByIdWithArgs("DOI_00002", studyId);
            return null;
        }

        Study study = Study.findById(studyId);
        if (study == null) {
            dataFile.getLogger().printExceptionByIdWithArgs("DOI_00003", studyId);
            return null;
        }
        chain.setStudyUri(study.getUri());
        dataFile.getLogger().println("DOI ingestion: Found study id [" + studyId + "]");

        // Version validation
        String doiVersion = doi.getVersion();
        if (doiVersion != null && !doiVersion.isEmpty()) {
            dataFile.getLogger().println("DOI ingestion: version is [" + doiVersion + "]");
        } else {
            dataFile.getLogger().printExceptionById("DOI_00004");
            return null;
        }

        // Add DOIGenerator if "Filenames" sheet exists
        if (!mapCatalog.containsKey("Filenames")) {
            dataFile.getLogger().printExceptionById("DOI_00005");
            return null;
        }

        addCustomGeneratorIfSheetExists(dataFile, mapCatalog, "Filenames", null, chain,
            (clonedFile, status) -> new DOIGenerator(clonedFile));

        return chain;
    }
}

