package org.hascoapi.tests;

import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.ingestion.AnnotateDASOC;
import org.hascoapi.utils.ConfigProp;
import org.junit.Test;

import java.io.File;

/**
 * Simplified DA-SOC Ingestion Test
 * 
 * This test ONLY tests the DA-SOC processing logic without creating SOC objects.
 * It assumes:
 * - SOC-LOCATION already exists in the triplestore with 50 objects
 * - Each object has an originalID matching the CSV (LIBRARY-L0, LIBRARY-L1, etc.)
 * 
 * To run:
 * sbt "testOnly org.hascoapi.tests.DASOCSimpleIngestionTest"
 */
public class DASOCSimpleIngestionTest {

    private static final String DASOC_CSV_FILE_PATH = "test/resources/da/DA-SOC-LOCATION.csv";
    private static final String SOC_NAME = "LOCATION";
    private static final String TEST_EMAIL = "test@hascoapi.org";

    @Test
    public void testDASOCIngestion() {
        System.out.println("\n========================================");
        System.out.println("DA-SOC SIMPLE INGESTION TEST");
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

            // Step 5: Display results
            System.out.println("\n========================================");
            System.out.println("INGESTION RESULTS");
            System.out.println("========================================\n");

            System.out.println("? " + result.toString());

            if (result.isSuccess()) {
                System.out.println("\n✅ DASOC Ingestion completed successfully!");
                System.out.println("   Rows processed: " + result.getRowCount());
            } else {
                System.out.println("\n❌ DASOC Ingestion failed!");
                System.out.println("   Error: " + result.getErrorMessage());
            }

            // Step 6: Display ingestion log
            System.out.println("\n========================================");
            System.out.println("INGESTION LOG");
            System.out.println("========================================\n");
            System.out.println(dataFile.getLogger().getLog());

            System.out.println("\n========================================");
            System.out.println("DA-SOC SIMPLE INGESTION TEST - COMPLETE");
            System.out.println("========================================\n");

        } catch (Exception e) {
            System.err.println("\n❌ TEST FAILED!");
            System.err.println("   Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

