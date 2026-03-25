package org.hascoapi.tests;

import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLCountUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * OrphanSOCElementsSetup
 *
 * Test utility class that inserts 50 SOC elements WITHOUT a named graph context,
 * simulating "orphan" elements that were inserted directly into Blazegraph's DEFAULT GRAPH.
 *
 * These elements are invisible to the standard SOC generation code because they lack
 * the named graph context that the current WHERE GRAPH ?g { ... } queries require.
 *
 * This is the "hidden elements" setup for orphan recovery feature tests:
 * - 50 elements WITHOUT named graph context (invisible to current generation code)
 * - Used together with LinkedSOCElementsSetup (50 elements WITH named graph context)
 * - Combined: 100 total elements (50 visible, 50 hidden) for testing recovery logic
 *
 * Usage pattern:
 *   @BeforeEach
 *     OrphanSOCElementsSetup.insertOrphanElements();
 *
 *   @AfterEach
 *     OrphanSOCElementsSetup.deleteOrphanElements();
 *
 * Key implementation details:
 * - Single batch INSERT DATA statement (NO GRAPH clause → lands in default graph)
 * - Uses DELETE WHERE for cleanup (targets default graph specifically)
 * - Independent of LinkedSOCElementsSetup (can be called in any order)
 * - Uses "extra-" prefix for originalIDs to differentiate from linked elements
 *
 * Verification query (paste into Blazegraph UI):
 *   # Count orphan elements (in default graph only)
 *   SELECT (COUNT(?e) AS ?c) WHERE {
 *     ?e <http://hadatac.org/ont/hasco/isMemberOf> <http://hadatac.org/ont/arrowhead/LTE-PIAGET-LOCATION> .
 *     FILTER NOT EXISTS {
 *       GRAPH ?g {
 *         ?e <http://hadatac.org/ont/hasco/isMemberOf> <http://hadatac.org/ont/arrowhead/LTE-PIAGET-LOCATION>
 *       }
 *     }
 *   }
 *   Expected: 50
 */
public class OrphanSOCElementsSetup {

    // Constants (must match LinkedSOCElementsSetup for consistency)
    public static final String AHEAD_NS           = "http://hadatac.org/ont/arrowhead/";
    public static final String HASCO_NS           = "http://hadatac.org/ont/hasco/";
    public static final String PATO_NS            = "http://purl.obolibrary.org/obo/PATO_";
    public static final String RDFS_NS            = "http://www.w3.org/2000/01/rdf-schema#";
    public static final String RDF_NS             = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    public static final String SOC_LOCATION_URI   = AHEAD_NS + "LTE-PIAGET-LOCATION";
    public static final int    ORPHAN_COUNT       = 50;

    // 10 zones × 5 levels = 50 elements
    // Using same zones and levels as LinkedSOCElementsSetup for consistency
    public static final String[] ZONES  = {
        "LIBRARY","SCIENCE","ARTS","SPORTS","ADMIN",
        "CAFE","MAIN","ANNEX-A","ANNEX-B","GREENHOUSE"
    };
    public static final String[] LEVELS = {"L0","L1","L2","L3","ROOF"};

    /**
     * Inserts 50 SOC elements into the DEFAULT GRAPH (no named graph context).
     * These elements will be invisible to standard generation code that queries via GRAPH ?g { ... }.
     *
     * Pattern: Single batch insert, NO GRAPH clause in INSERT DATA.
     * Uses "extra-" prefix for originalIDs to differentiate from linked elements.
     *
     * @throws RuntimeException if insertion fails
     */
    public static void insertOrphanElements() {
        System.out.println("\n========== OrphanSOCElementsSetup.insertOrphanElements() START ==========");
        System.out.println("[OrphanSOCElementsSetup] Inserting " + ORPHAN_COUNT + " orphan SOC elements into DEFAULT GRAPH");
        System.out.println("[OrphanSOCElementsSetup] SOC URI: " + SOC_LOCATION_URI);
        System.out.println("[OrphanSOCElementsSetup] These elements will have NO named graph context");

        // Build single INSERT statement WITHOUT GRAPH clause (inserts into default graph)
        StringBuilder insert = new StringBuilder();
        insert.append(NameSpaces.getInstance().printSparqlNameSpaceList());
        insert.append("\n");
        insert.append("INSERT {\n");

        // Generate triples for all 50 orphan elements
        // Use "extra-" prefix to differentiate from linked elements
        for (String zone : ZONES) {
            for (String level : LEVELS) {
                String originalId = "extra-" + zone + "-" + level;
                String elementUri = SOC_LOCATION_URI + "/" + originalId;
                String locationTypeUri = getLocationTypeUri(zone, level);

                insert.append("  <").append(elementUri).append("> \n");
                insert.append("      <").append(RDF_NS).append("type> <").append(PATO_NS).append("0000140> ; \n");
                insert.append("      <").append(HASCO_NS).append("isMemberOf> <").append(SOC_LOCATION_URI).append("> ; \n");
                insert.append("      <").append(HASCO_NS).append("originalID> \"").append(originalId).append("\" ; \n");
                insert.append("      <").append(HASCO_NS).append("hasSpaceScope> <").append(locationTypeUri).append("> ; \n");
                insert.append("      <").append(RDFS_NS).append("label> \"Extra Location ").append(originalId).append("\" . \n");
                insert.append("\n");
            }
        }

        insert.append("} WHERE {}\n");

        String insertQuery = insert.toString();

        // Execute the single batch insert (into default graph)
        try {
            String endpoint = CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_UPDATE);
            UpdateRequest request = UpdateFactory.create(insertQuery);
            UpdateProcessor processor = UpdateExecutionFactory.createRemote(request, endpoint);
            processor.execute();
            System.out.println("[OrphanSOCElementsSetup] ✓ Successfully inserted " + ORPHAN_COUNT + " orphan elements in a single batch");
        } catch (Exception e) {
            System.err.println("[OrphanSOCElementsSetup] ERROR: Failed to insert orphan elements: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to insert orphan SOC elements", e);
        }

        // Verify insertion count
        try {
            long count = countOrphanElements();
            System.out.println("[OrphanSOCElementsSetup] Verification: found " + count + " orphan elements");
            if (count != ORPHAN_COUNT) {
                System.err.println("[OrphanSOCElementsSetup] WARNING: Expected " + ORPHAN_COUNT + " but found " + count);
            }
        } catch (Exception e) {
            System.err.println("[OrphanSOCElementsSetup] WARNING: Verification count failed: " + e.getMessage());
        }

        System.out.println("========== OrphanSOCElementsSetup.insertOrphanElements() END ==========\n");
    }

    /**
     * Removes all 50 orphan elements from the default graph.
     * Uses DELETE WHERE targeting elements by their "extra-" prefix pattern.
     *
     * Pattern: Follows BaseGenerator deletion patterns, targets default graph specifically.
     *
     * @throws RuntimeException if deletion fails
     */
    public static void deleteOrphanElements() {
        System.out.println("\n========== OrphanSOCElementsSetup.deleteOrphanElements() START ==========");
        System.out.println("[OrphanSOCElementsSetup] Deleting orphan elements from DEFAULT GRAPH");

        // Delete by pattern: all elements with originalID starting with "extra-"
        StringBuilder delete = new StringBuilder();
        delete.append(NameSpaces.getInstance().printSparqlNameSpaceList());
        delete.append("\n");
        delete.append("DELETE {\n");
        delete.append("  ?e ?p ?o . \n");
        delete.append("} WHERE {\n");
        delete.append("  ?e <").append(HASCO_NS).append("isMemberOf> <").append(SOC_LOCATION_URI).append("> ; \n");
        delete.append("     <").append(HASCO_NS).append("originalID> ?id ; \n");
        delete.append("     ?p ?o . \n");
        delete.append("  FILTER (STRSTARTS(STR(?id), \"extra-\")) \n");
        delete.append("}\n");

        String deleteQuery = delete.toString();

        try {
            String endpoint = CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_UPDATE);
            UpdateRequest request = UpdateFactory.create(deleteQuery);
            UpdateProcessor processor = UpdateExecutionFactory.createRemote(request, endpoint);
            processor.execute();
            System.out.println("[OrphanSOCElementsSetup] ✓ Successfully deleted orphan elements");
        } catch (Exception e) {
            System.err.println("[OrphanSOCElementsSetup] ERROR: Failed to delete orphan elements: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to delete orphan SOC elements", e);
        }

        // Verify deletion
        try {
            long count = countOrphanElements();
            System.out.println("[OrphanSOCElementsSetup] Verification: found " + count + " orphan elements after deletion");
            if (count != 0) {
                System.err.println("[OrphanSOCElementsSetup] WARNING: Expected 0 but found " + count + " after deletion");
            }
        } catch (Exception e) {
            System.out.println("[OrphanSOCElementsSetup] Verification: no orphan elements found (as expected)");
        }

        System.out.println("========== OrphanSOCElementsSetup.deleteOrphanElements() END ==========\n");
    }

    /**
     * Returns the list of expected orphan originalIDs.
     * All IDs have "extra-" prefix to differentiate from linked elements.
     * Useful for test assertions.
     *
     * @return List of 50 originalIDs in format "extra-{ZONE}-{LEVEL}"
     */
    public static List<String> expectedOrphanOriginalIDs() {
        List<String> ids = new ArrayList<>(ORPHAN_COUNT);
        for (String zone : ZONES) {
            for (String level : LEVELS) {
                ids.add("extra-" + zone + "-" + level);
            }
        }
        return ids;
    }

    /**
     * Count orphan elements (elements in default graph without named graph context).
     *
     * @return Number of orphan elements found
     */
    private static long countOrphanElements() {
        String queryString = "SELECT (COUNT(*) AS ?tot) WHERE { \n" +
                "  ?e <" + HASCO_NS + "isMemberOf> <" + SOC_LOCATION_URI + "> ; \n" +
                "     <" + HASCO_NS + "originalID> ?id . \n" +
                "  FILTER (STRSTARTS(STR(?id), \"extra-\")) \n" +
                "  FILTER NOT EXISTS { \n" +
                "    GRAPH ?g { \n" +
                "      ?e <" + HASCO_NS + "isMemberOf> <" + SOC_LOCATION_URI + "> \n" +
                "    } \n" +
                "  } \n" +
                "}";

        String endpoint = CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY);
        return SPARQLCountUtil.count(endpoint, queryString);
    }

    /**
     * Determine location type URI based on zone and level.
     * Logic matches LinkedSOCElementsSetup for consistency.
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
     * Verify that orphan elements exist in the default graph.
     * Used internally by tests.
     *
     * @return true if exactly ORPHAN_COUNT elements found in default graph, false otherwise
     */
    public static boolean verifyOrphanElementsExist() {
        try {
            long count = countOrphanElements();
            return count == ORPHAN_COUNT;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if any orphan elements exist.
     *
     * @return true if at least one orphan element exists, false otherwise
     */
    public static boolean orphanElementsExist() {
        try {
            long count = countOrphanElements();
            return count > 0;
        } catch (Exception e) {
            return false;
        }
    }
}

