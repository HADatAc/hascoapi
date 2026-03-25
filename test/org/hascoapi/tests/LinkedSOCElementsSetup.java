package org.hascoapi.tests;

import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLCountUtil;
import org.hascoapi.utils.SPARQLUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * LinkedSOCElementsSetup
 *
 * Test utility class that inserts 50 SOC elements WITH a proper named graph context,
 * simulating the behavior of elements ingested through a real DataFile pipeline.
 *
 * These elements are stored inside a named graph (TEST_NAMED_GRAPH), making them
 * visible to the standard SOC generation code which queries via GRAPH ?g { ... }.
 *
 * This is the "baseline" setup for orphan recovery feature tests:
 * - 50 elements WITH named graph context (visible to current generation code)
 * - Used together with OrphanSOCElementsSetup (50 elements WITHOUT named graph context)
 * - Combined: 100 total elements (50 visible, 50 hidden) for testing recovery logic
 *
 * Usage pattern:
 *   @BeforeEach
 *     LinkedSOCElementsSetup.insertLinkedElements();
 *
 *   @AfterEach
 *     LinkedSOCElementsSetup.deleteLinkedElements();
 *
 * Key implementation details:
 * - Single batch INSERT DATA statement (not 50 individual inserts) for performance
 * - Uses DROP GRAPH for cleanup (removes graph metadata completely)
 * - Independent of OrphanSOCElementsSetup (can be called in any order)
 *
 * Verification query (paste into Blazegraph UI):
 *   SELECT (COUNT(?e) AS ?c) WHERE {
 *     GRAPH <http://hadatac.org/ont/arrowhead/DataFile/LTE-PIAGET-LOCATION-TEST-DF> {
 *       ?e <http://hadatac.org/ont/hasco/isMemberOf> <http://hadatac.org/ont/arrowhead/LTE-PIAGET-LOCATION>
 *     }
 *   }
 *   Expected: 50
 */
public class LinkedSOCElementsSetup {

    // Constants
    public static final String AHEAD_NS           = "http://hadatac.org/ont/arrowhead/";
    public static final String HASCO_NS           = "http://hadatac.org/ont/hasco/";
    public static final String PATO_NS            = "http://purl.obolibrary.org/obo/PATO_";
    public static final String RDFS_NS            = "http://www.w3.org/2000/01/rdf-schema#";
    public static final String RDF_NS             = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    public static final String SOC_LOCATION_URI   = AHEAD_NS + "LTE-PIAGET-LOCATION";
    public static final String TEST_NAMED_GRAPH   = AHEAD_NS + "DataFile/LTE-PIAGET-LOCATION-TEST-DF";
    public static final int    LINKED_COUNT       = 50;

    // 10 zones × 5 levels = 50 elements
    public static final String[] ZONES  = {
        "LIBRARY","SCIENCE","ARTS","SPORTS","ADMIN",
        "CAFE","MAIN","ANNEX-A","ANNEX-B","GREENHOUSE"
    };
    public static final String[] LEVELS = {"L0","L1","L2","L3","ROOF"};

    /**
     * Inserts 50 SOC elements into TEST_NAMED_GRAPH in a single batch INSERT DATA statement.
     * Elements are inserted WITH proper named graph context, making them visible to
     * standard generation code that queries via GRAPH ?g { ... }.
     *
     * Pattern: Single batch insert, following HASCO performance conventions.
     *
     * @throws RuntimeException if insertion fails
     */
    public static void insertLinkedElements() {
        System.out.println("\n========== LinkedSOCElementsSetup.insertLinkedElements() START ==========");
        System.out.println("[LinkedSOCElementsSetup] Inserting " + LINKED_COUNT + " linked SOC elements into named graph");
        System.out.println("[LinkedSOCElementsSetup] Named graph: " + TEST_NAMED_GRAPH);
        System.out.println("[LinkedSOCElementsSetup] SOC URI: " + SOC_LOCATION_URI);

        // Build single INSERT DATA statement with GRAPH clause
        StringBuilder insert = new StringBuilder();
        insert.append(NameSpaces.getInstance().printSparqlNameSpaceList());
        insert.append("\n");
        insert.append("INSERT DATA {\n");
        insert.append("  GRAPH <").append(TEST_NAMED_GRAPH).append("> {\n");

        // Generate triples for all 50 elements
        // Using -2 suffix to differentiate from original elements (without suffix or with -1)
        // This allows coexistence: LIBRARY-L0 (original) + LIBRARY-L0-2 (test) = 100 total
        for (String zone : ZONES) {
            for (String level : LEVELS) {
                String originalId = zone + "-" + level + "-2";
                String elementUri = SOC_LOCATION_URI + "/" + originalId;
                String locationTypeUri = getLocationTypeUri(zone, level);

                insert.append("    <").append(elementUri).append("> \n");
                insert.append("        <").append(RDF_NS).append("type> <").append(PATO_NS).append("0000140> ; \n");
                insert.append("        <").append(HASCO_NS).append("isMemberOf> <").append(SOC_LOCATION_URI).append("> ; \n");
                insert.append("        <").append(HASCO_NS).append("originalID> \"").append(originalId).append("\" ; \n");
                insert.append("        <").append(HASCO_NS).append("hasSpaceScope> <").append(locationTypeUri).append("> ; \n");
                insert.append("        <").append(RDFS_NS).append("label> \"Location ").append(originalId).append("\" . \n");
                insert.append("\n");
            }
        }

        insert.append("  }\n");
        insert.append("}\n");

        String insertQuery = insert.toString();

        // Execute the single batch insert
        try {
            String endpoint = CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_UPDATE);
            UpdateRequest request = UpdateFactory.create(insertQuery);
            UpdateProcessor processor = UpdateExecutionFactory.createRemote(request, endpoint);
            processor.execute();
            System.out.println("[LinkedSOCElementsSetup] ✓ Successfully inserted " + LINKED_COUNT + " elements in a single batch");
        } catch (Exception e) {
            System.err.println("[LinkedSOCElementsSetup] ERROR: Failed to insert linked elements: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to insert linked SOC elements", e);
        }

        // Verify insertion count
        try {
            long count = countLinkedElements();
            System.out.println("[LinkedSOCElementsSetup] Verification: found " + count + " elements in named graph");
            if (count != LINKED_COUNT) {
                System.err.println("[LinkedSOCElementsSetup] WARNING: Expected " + LINKED_COUNT + " but found " + count);
            }
        } catch (Exception e) {
            System.err.println("[LinkedSOCElementsSetup] WARNING: Verification count failed: " + e.getMessage());
        }

        System.out.println("========== LinkedSOCElementsSetup.insertLinkedElements() END ==========\n");
    }

    /**
     * Removes all 50 linked elements by dropping the entire test named graph.
     * Uses DROP GRAPH to completely remove graph metadata and contents.
     *
     * Pattern: Follows BaseGenerator.dropGraph() and NameSpace.deleteTriplesByNamedGraph()
     *
     * @throws RuntimeException if deletion fails
     */
    public static void deleteLinkedElements() {
        System.out.println("\n========== LinkedSOCElementsSetup.deleteLinkedElements() START ==========");
        System.out.println("[LinkedSOCElementsSetup] Dropping named graph: " + TEST_NAMED_GRAPH);

        String dropQuery = "DROP GRAPH <" + TEST_NAMED_GRAPH + ">";

        try {
            String endpoint = CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_UPDATE);
            UpdateRequest request = UpdateFactory.create(dropQuery);
            UpdateProcessor processor = UpdateExecutionFactory.createRemote(request, endpoint);
            processor.execute();
            System.out.println("[LinkedSOCElementsSetup] ✓ Successfully dropped named graph");
        } catch (Exception e) {
            System.err.println("[LinkedSOCElementsSetup] ERROR: Failed to drop named graph: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to delete linked SOC elements", e);
        }

        // Verify deletion
        try {
            long count = countLinkedElements();
            System.out.println("[LinkedSOCElementsSetup] Verification: found " + count + " elements after deletion");
            if (count != 0) {
                System.err.println("[LinkedSOCElementsSetup] WARNING: Expected 0 but found " + count + " after deletion");
            }
        } catch (Exception e) {
            // Graph might not exist anymore, which is expected
            System.out.println("[LinkedSOCElementsSetup] Verification: named graph no longer exists (as expected)");
        }

        System.out.println("========== LinkedSOCElementsSetup.deleteLinkedElements() END ==========\n");
    }

    /**
     * Returns the list of expected linked originalIDs.
     * Useful for test assertions.
     *
     * @return List of 50 originalIDs in format "{ZONE}-{LEVEL}-2"
     */
    public static List<String> expectedLinkedOriginalIDs() {
        List<String> ids = new ArrayList<>(LINKED_COUNT);
        for (String zone : ZONES) {
            for (String level : LEVELS) {
                ids.add(zone + "-" + level + "-2");
            }
        }
        return ids;
    }

    /**
     * Count elements in the test named graph.
     *
     * @return Number of elements found, or 0 if graph doesn't exist
     */
    private static long countLinkedElements() {
        String queryString = "SELECT (COUNT(*) AS ?tot) WHERE { \n" +
                "  GRAPH <" + TEST_NAMED_GRAPH + "> { \n" +
                "    ?e <" + HASCO_NS + "isMemberOf> <" + SOC_LOCATION_URI + "> \n" +
                "  } \n" +
                "}";

        String endpoint = CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY);
        return SPARQLCountUtil.count(endpoint, queryString);
    }

    /**
     * Determine location type URI based on zone and level.
     * Logic matches OrphanSOCElementsSetup for consistency.
     *
     * Rules:
     * - ROOF level → outdoor
     * - GREENHOUSE or SCIENCE at L0/L1/L2 → laboratory
     * - All other combinations → indoor
     */
    private static String getLocationTypeUri(String zone, String level) {
        if ("ROOF".equals(level)) {
            return AHEAD_NS + "LTE-PIAGET-LOCATION-TYPE/outdoor";
        }

        if (("GREENHOUSE".equals(zone) || "SCIENCE".equals(zone)) &&
            ("L0".equals(level) || "L1".equals(level) || "L2".equals(level))) {
            return AHEAD_NS + "LTE-PIAGET-LOCATION-TYPE/laboratory";
        }

        return AHEAD_NS + "LTE-PIAGET-LOCATION-TYPE/indoor";
    }

    /**
     * Verify that the named graph exists and contains the expected number of elements.
     * Used internally by tests.
     *
     * @return true if exactly LINKED_COUNT elements found in named graph, false otherwise
     */
    public static boolean verifyLinkedElementsExist() {
        try {
            long count = countLinkedElements();
            return count == LINKED_COUNT;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if any elements exist in the test named graph.
     *
     * @return true if named graph contains at least one element, false otherwise
     */
    public static boolean namedGraphExists() {
        try {
            long count = countLinkedElements();
            return count > 0;
        } catch (Exception e) {
            return false;
        }
    }
}

