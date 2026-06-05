package org.hascoapi.tests;

import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.Study;
import org.hascoapi.ingestion.IngestionWorker;
import org.hascoapi.transform.mt.dsg.DSGGen;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.ConfigProp;
import org.hascoapi.utils.IngestionLogger;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.io.File;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DSGDaZipGenerationTest {

    @Test
    public void genByStudy_withGenerateDASOCs_true_returnsZipContainingDsg() throws Exception {
        Study study = getAnyStudy();
        if (study == null) {
            ingestDsgFixtureToSeedStudy();
            study = getAnyStudy();
        }

        Assumptions.assumeTrue(study != null,
            "No study available in triplestore for DSG generation test");
        assertNotNull(study, "Expected at least one study");

        String dsgFilename = "DSG-DA-ZIP-TEST-" + System.currentTimeMillis() + ".xlsx";
        String result = DSGGen.genByStudy(study, dsgFilename, null, null, true);

        assertNotNull(result, "Generation result should not be null");
        assertFalse(result.toLowerCase().contains("failure"), "Generation should not fail: " + result);
        assertTrue(result.toLowerCase().endsWith(".zip"), "Expected ZIP response when generateDASOCs=true: " + result);

        String basePath = ConfigProp.getPathIngestion();
        File zipFile = new File(basePath, result);
        assertTrue(zipFile.exists(), "Generated ZIP file should exist: " + zipFile.getAbsolutePath());

        try (ZipFile zip = new ZipFile(zipFile)) {
            assertTrue(zip.size() >= 1, "ZIP should contain at least the DSG workbook");
            ZipEntry dsgEntry = zip.getEntry(dsgFilename);
            assertNotNull(dsgEntry, "ZIP should contain generated DSG workbook entry: " + dsgFilename);
        }
    }

    private Study getAnyStudy() {
        String query = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT ?study WHERE { ?study a hasco:Study . } LIMIT 1";
        ResultSetRewindable results = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY),
                query);

        if (!results.hasNext()) {
            return null;
        }

        QuerySolution solution = results.next();
        if (solution.get("study") == null) {
            return null;
        }

        String studyUri = solution.get("study").toString();
        return Study.find(studyUri);
    }

    private void ingestDsgFixtureToSeedStudy() {
        File dsgFixture = new File("test/resources/dsg/DSG-STD-test.xlsx");
        assertTrue(dsgFixture.exists(), "DSG fixture must exist for seeding: " + dsgFixture.getAbsolutePath());

        DataFile dataFile = mock(DataFile.class);
        IngestionLogger logger = mock(IngestionLogger.class);

        when(dataFile.getFilename()).thenReturn("DSG-STD-test.xlsx");
        when(dataFile.getLogger()).thenReturn(logger);
        when(dataFile.getUri()).thenReturn("http://example.org/DF-DSG-ZIP-TEST-" + System.currentTimeMillis());

        IngestionWorker.ingest(dataFile, dsgFixture, null, "");
    }
}
