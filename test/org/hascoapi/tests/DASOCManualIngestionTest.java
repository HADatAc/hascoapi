package org.hascoapi.tests;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.StudyObject;
import org.hascoapi.entity.pojo.StudyObjectCollection;
import org.hascoapi.ingestion.AnnotateDASOC;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.ConfigProp;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Manual test for DA-SOC ingestion
 *
 * This test manually ingests the DA-SOC-LOCATION.csv file that enriches
 * the SOC-LOCATION instances with additional properties (altitude, floor, zone, etc.)
 *
 * Prerequisites:
 * - Triplestore must be running (Fuseki on localhost:3030)
 *
 * This test will:
 * 1. Create SOC-LOCATION with 50 StudyObject instances
 * 2. Ingest DA-SOC-LOCATION.csv to enrich those objects with 5 extra properties
 *
 * To run this test:
 * 1. Start Fuseki: docker-compose up fuseki
 * 2. Run: sbt "testOnly org.hascoapi.tests.DASOCManualIngestionTest"
 */
public class DASOCManualIngestionTest {

    private static final String DASOC_CSV_FILE_PATH = "test/resources/da/DA-SOC-LOCATION.csv";
    private static final String SOC_BASE_CSV_FILE_PATH = "test/resources/da/SOC-LOCATION-base.csv";
    private static final String SOC_NAME = "LOCATION";
    private static final String TEST_EMAIL = "test@hascoapi.org";

    private String socUri;
    private String kbPrefix;

    @Before
    public void setUp() {
        System.out.println("\n========================================");
        System.out.println("DA-SOC MANUAL INGESTION TEST - SETUP");
        System.out.println("========================================\n");

        // Verify configuration
        kbPrefix = ConfigProp.getKbPrefix();
        System.out.println("Knowledge Base Prefix: " + kbPrefix);

        // Construct SOC URI
        socUri = kbPrefix + "SOC-" + SOC_NAME;

        // Verify CSV files exist
        File dasocCsvFile = new File(DASOC_CSV_FILE_PATH);
        if (!dasocCsvFile.exists()) {
            fail("DA-SOC CSV file not found: " + DASOC_CSV_FILE_PATH);
        }
        System.out.println("✅ DA-SOC CSV file found: " + DASOC_CSV_FILE_PATH);
        System.out.println("   File size: " + dasocCsvFile.length() + " bytes");

        File socBaseCsvFile = new File(SOC_BASE_CSV_FILE_PATH);
        if (!socBaseCsvFile.exists()) {
            fail("SOC base CSV file not found: " + SOC_BASE_CSV_FILE_PATH);
        }
        System.out.println("✅ SOC base CSV file found: " + SOC_BASE_CSV_FILE_PATH);
        System.out.println("   File size: " + socBaseCsvFile.length() + " bytes");

        // Create SOC-LOCATION with 50 objects
        createSOCLocation();
    }

    /**
     * Create SOC-LOCATION and populate it with 50 StudyObject instances
     */
    private void createSOCLocation() {
        System.out.println("\n========================================");
        System.out.println("CREATING SOC-LOCATION");
        System.out.println("========================================\n");

        try {
            // Check if SOC already exists
            StudyObjectCollection existingSOC = StudyObjectCollection.find(socUri);
            if (existingSOC != null) {
                System.out.println("⚠️  SOC-LOCATION already exists, deleting it first...");
                existingSOC.delete();
            }

            // Create new SOC
            StudyObjectCollection soc = new StudyObjectCollection();
            soc.setUri(socUri);
            soc.setLabel("Location Collection");
            soc.setComment("Test location collection for DA-SOC ingestion test");
            soc.setTypeUri("http://hadatac.org/ont/hasco#StudyObjectCollection");
            soc.setHascoTypeUri("http://hadatac.org/ont/hasco#StudyObjectCollection");
            soc.setHasSIRManagerEmail(TEST_EMAIL);
            soc.save();

            System.out.println("✅ Created SOC: " + socUri);

            // Read base CSV and create StudyObjects
            File socBaseCsv = new File(SOC_BASE_CSV_FILE_PATH);
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(socBaseCsv));

            String line = reader.readLine(); // Skip header
            int objectCount = 0;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    String originalID = parts[0].trim();
                    String label = parts[1].trim();
                    String comment = parts[2].trim();

                    // Create StudyObject
                    StudyObject obj = new StudyObject();
                    String objUri = kbPrefix + originalID;
                    obj.setUri(objUri);
                    obj.setOriginalId(originalID);
                    obj.setLabel(label);
                    obj.setComment(comment);
                    obj.setIsMemberOfUri(socUri);
                    obj.setTypeUri("http://hadatac.org/ont/hasco#StudyObject");
                    obj.setHascoTypeUri("http://hadatac.org/ont/hasco#StudyObject");
                    obj.setHasSIRManagerEmail(TEST_EMAIL);
                    obj.save();

                    objectCount++;
                }
            }

            reader.close();

            System.out.println("✅ Created " + objectCount + " StudyObject instances");

            // Verify objects were created
            String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT (COUNT(?obj) as ?count) WHERE { " +
                "  ?obj hasco:isMemberOf <" + socUri + "> . " +
                "}";

            ResultSetRewindable results = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY),
                queryString);

            if (results.hasNext()) {
                QuerySolution soln = results.next();
                int count = soln.getLiteral("count").getInt();
                System.out.println("✅ Verified: " + count + " objects in triplestore");
                assertEquals("Should have 50 objects", 50, count);
            }

        } catch (Exception e) {
            fail("Failed to create SOC-LOCATION: " + e.getMessage());
        }
    }

    @Test
    public void testDASOCIngestion() {
        System.out.println("\n========================================");
        System.out.println("STARTING DA-SOC INGESTION");
        System.out.println("========================================\n");

        try {
            // Step 1: Construct URIs
            String kbPrefix = ConfigProp.getKbPrefix();
            String socUri = kbPrefix + "SOC-" + SOC_NAME;
            String daUri = kbPrefix + "DA-TEST-" + System.currentTimeMillis();

            System.out.println("Step 1: Constructed URIs");
            System.out.println("  SOC URI: " + socUri);
            System.out.println("  DA URI:  " + daUri);

            // Step 2: Create DataFile entity
            String dataFileId = "DFL-TEST-" + System.currentTimeMillis();
            DataFile dataFile = DataFile.create(
                dataFileId,
                "DA-SOC-LOCATION.csv",
                TEST_EMAIL,
                DataFile.UNPROCESSED
            );

            String dataFileUri = kbPrefix + dataFileId;
            dataFile.setUri(dataFileUri);
            dataFile.setDasocSOCUri(socUri);
            dataFile.setDasocDataAcquisitionUri(daUri);
            dataFile.save();

            System.out.println("\nStep 2: Created DataFile entity");
            System.out.println("  DataFile URI: " + dataFileUri);
            System.out.println("  DASOC SOC URI: " + socUri);
            System.out.println("  DASOC DA URI: " + daUri);

            // Step 3: Get CSV file
            File csvFile = new File(DASOC_CSV_FILE_PATH);
            System.out.println("\nStep 3: Loading CSV file");
            System.out.println("  File path: " + csvFile.getAbsolutePath());
            System.out.println("  File exists: " + csvFile.exists());
            System.out.println("  File size: " + csvFile.length() + " bytes");

            // Step 4: Execute DASOC ingestion
            System.out.println("\nStep 4: Calling AnnotateDASOC.processDASOC()");
            System.out.println("  This will:");
            System.out.println("  - Load all StudyObjects from SOC-LOCATION");
            System.out.println("  - Build originalID → objectURI map");
            System.out.println("  - Process CSV rows (50 expected)");
            System.out.println("  - Add 5 properties per object (altitude_m, floor, zone_code, area_m2, orientation)");
            System.out.println("  - Save triples to graph: " + daUri + "-dasoc");
            System.out.println("\nProcessing...\n");

            AnnotateDASOC.IngestionResult result = AnnotateDASOC.processDASOC(
                dataFile,
                csvFile,
                daUri,
                socUri
            );

            // Step 5: Verify results
            System.out.println("\n========================================");
            System.out.println("INGESTION RESULTS");
            System.out.println("========================================\n");
            System.out.println(result.toString());

            assertTrue("Ingestion should succeed", result.isSuccess());
            assertTrue("Should process at least 1 row", result.getRowCount() > 0);

            System.out.println("\n✅ TEST PASSED!");
            System.out.println("   Rows processed: " + result.getRowCount());
            System.out.println("   Success: " + result.isSuccess());

            // Step 6: Display ingestion log
            System.out.println("\n========================================");
            System.out.println("INGESTION LOG");
            System.out.println("========================================\n");
            System.out.println(dataFile.getLogger().getLog());

        } catch (Exception e) {
            System.err.println("\n❌ TEST FAILED!");
            System.err.println("   Error: " + e.getMessage());
            e.printStackTrace();
            fail("DASOC ingestion failed: " + e.getMessage());
        }
    }

    @After
    public void tearDown() {
        System.out.println("\n========================================");
        System.out.println("DA-SOC MANUAL INGESTION TEST - COMPLETE");
        System.out.println("========================================\n");
    }
}

