package org.hascoapi.tests;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.ingestion.AnnotateDASOC;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;

import static org.junit.jupiter.api.Assertions.*;

public class DASOCIngestionSmokeTest {

    @Test
    public void processDASOC_withExistingContext_ingestsOneRow() throws Exception {
        long nonce = System.currentTimeMillis();
        String base = "http://example.org/dasoc-smoke-" + nonce;

        DasocContext context = queryExistingDasocContext();
        Assumptions.assumeTrue(context != null, "No existing study/SOC/object context available for DASOC smoke test");

        String studyUri = context.studyUri;
        String socUri = context.socUri;
        String objUri = context.objectUri;
        String originalId = context.originalId;

        String daUri = base + "/DA-SMOKE";
        String graphUri = daUri + "-dasoc";
        String dataFileGraphUri = base + "/DFL-SMOKE";
        String propHeader = "pharma:altitude_m";

        File csvFile = File.createTempFile("dasoc-smoke-", ".csv");
        csvFile.deleteOnExit();

        try {
            try (FileWriter writer = new FileWriter(csvFile)) {
                writer.write("originalID," + propHeader + "\n");
                writer.write(originalId + ",12.5\n");
            }

            DataFile dataFile = new DataFile("DFL-SMOKE-" + nonce, csvFile.getName());
            dataFile.setUri(dataFileGraphUri);
            dataFile.setStudyUri(studyUri);
            dataFile.setDasocSOCUri(socUri);
            dataFile.setDasocDataAcquisitionUri(daUri);
            dataFile.setHasSIRManagerEmail("test@hascoapi.org");

            AnnotateDASOC.IngestionResult result = AnnotateDASOC.processDASOC(
                    dataFile,
                    csvFile,
                    daUri,
                    socUri
            );

            assertNotNull(result, "Ingestion result should not be null");
            assertTrue(result.isSuccess(), "Ingestion should succeed: " + result.getErrorMessage());
            assertTrue(result.getRowCount() >= 1, "Expected at least one ingested row");

                String verifyQuery = NameSpaces.getInstance().printSparqlNameSpaceList() +
                    "SELECT (COUNT(?s) AS ?count) WHERE { " +
                        "  GRAPH <" + dataFileGraphUri + "> { " +
                        "    ?s ?p ?o . " +
                    "  } " +
                    "}";

            ResultSetRewindable verifyResults = SPARQLUtils.select(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY),
                    verifyQuery);

                assertTrue(verifyResults.hasNext(), "Expected DASOC graph query result");
                QuerySolution soln = verifyResults.next();
                long count = soln.getLiteral("count").getLong();
                assertTrue(count > 0, "Expected triples in DASOC graph after ingestion");

        } finally {
            cleanupGraph(graphUri);
            cleanupGraph(dataFileGraphUri);
        }
    }

    private DasocContext queryExistingDasocContext() {
        String query = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT ?study ?soc ?obj ?originalId WHERE { " +
                "  ?soc hasco:isMemberOf ?study . " +
                "  ?obj hasco:isMemberOf ?soc . " +
                "  ?obj hasco:originalID ?originalId . " +
                "} LIMIT 1";

        ResultSetRewindable results = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY),
                query);

        if (!results.hasNext()) {
            return null;
        }

        QuerySolution soln = results.next();
        if (soln.get("study") == null || soln.get("soc") == null || soln.get("obj") == null || soln.get("originalId") == null) {
            return null;
        }

        return new DasocContext(
                soln.get("study").toString(),
                soln.get("soc").toString(),
                soln.get("obj").toString(),
                soln.get("originalId").toString()
        );
    }

    private void cleanupGraph(String graphUri) {
        String cleanup = "DELETE WHERE { GRAPH <" + graphUri + "> { ?s ?p ?o . } }";

        executeUpdate(cleanup);
    }

    private void executeUpdate(String updateQuery) {
        UpdateRequest request = UpdateFactory.create(updateQuery);
        UpdateProcessor processor = UpdateExecutionFactory.createRemote(
                request,
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_UPDATE)
        );
        processor.execute();
    }

    private static class DasocContext {
        private final String studyUri;
        private final String socUri;
        private final String objectUri;
        private final String originalId;

        private DasocContext(String studyUri, String socUri, String objectUri, String originalId) {
            this.studyUri = studyUri;
            this.socUri = socUri;
            this.objectUri = objectUri;
            this.originalId = originalId;
        }
    }
}
