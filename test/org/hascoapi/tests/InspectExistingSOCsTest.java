package org.hascoapi.tests;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * InspectExistingSOCsTest
 * 
 * Discovers existing SOCs in the triplestore to identify:
 * 1. Which SOCs exist (especially those with "Location" in the name)
 * 2. What originalIDs they contain
 * 3. How many objects each SOC has
 * 
 * This helps us understand what data is available for DA-SOC enrichment.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class InspectExistingSOCsTest {

    @Test
    @Order(1)
    @DisplayName("Test 1: List all SOCs in the database")
    public void test1_listAllSOCs() {
        System.out.println("\n========================================");
        System.out.println("TEST 1: LISTING ALL SOCs");
        System.out.println("========================================\n");

        String ns = NameSpaces.getInstance().printSparqlNameSpaceList();
        String endpoint = CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY);

        // Query to find all SOCs (both StudyObjectCollection and SpaceCollection)
        String socQuery = ns +
                "SELECT DISTINCT ?soc ?label ?type WHERE { \n" +
                "  { ?soc a hasco:StudyObjectCollection } UNION \n" +
                "  { ?soc a hasco:SpaceCollection } \n" +
                "  OPTIONAL { ?soc rdfs:label ?label } \n" +
                "  OPTIONAL { ?soc a ?type } \n" +
                "} ORDER BY ?soc";

        ResultSetRewindable results = SPARQLUtils.select(endpoint, socQuery);
        
        System.out.println("SOCs found in database:");
        System.out.println("─────────────────────────────────────────────────────────");
        
        int socCount = 0;
        List<String> locationSOCs = new ArrayList<>();
        
        while (results.hasNext()) {
            QuerySolution sol = results.next();
            socCount++;
            String socUri = sol.getResource("soc").getURI();
            String label = sol.getLiteral("label") != null ? sol.getLiteral("label").getString() : "N/A";
            
            System.out.println(socCount + ". " + socUri);
            System.out.println("   Label: " + label);
            
            // Check if this SOC is related to Location
            if (socUri.toUpperCase().contains("LOCATION") || label.toUpperCase().contains("LOCATION")) {
                locationSOCs.add(socUri);
                System.out.println("   ✓ CONTAINS 'LOCATION' - CANDIDATE FOR DA-SOC ENRICHMENT");
            }
            System.out.println();
        }

        System.out.println("─────────────────────────────────────────────────────────");
        System.out.println("Total SOCs found: " + socCount);
        System.out.println("Location-related SOCs: " + locationSOCs.size());
        
        if (locationSOCs.size() > 0) {
            System.out.println("\n✓ Found " + locationSOCs.size() + " Location-related SOC(s):");
            for (String soc : locationSOCs) {
                System.out.println("  - " + soc);
            }
        }
        
        assertTrue(socCount > 0, "Should have at least one SOC in the database");
    }

    @Test
    @Order(2)
    @DisplayName("Test 2: Inspect Location SOC objects and originalIDs")
    public void test2_inspectLocationSOCObjects() {
        System.out.println("\n========================================");
        System.out.println("TEST 2: INSPECTING LOCATION SOC OBJECTS");
        System.out.println("========================================\n");

        String ns = NameSpaces.getInstance().printSparqlNameSpaceList();
        String endpoint = CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY);

        // Find Location-related SOCs
        String findLocationSOCQuery = ns +
                "SELECT DISTINCT ?soc ?label WHERE { \n" +
                "  ?soc a hasco:StudyObjectCollection . \n" +
                "  OPTIONAL { ?soc rdfs:label ?label } \n" +
                "  FILTER (CONTAINS(UCASE(STR(?soc)), \"LOCATION\") || \n" +
                "          CONTAINS(UCASE(STR(?label)), \"LOCATION\")) \n" +
                "}";

        ResultSetRewindable socResults = SPARQLUtils.select(endpoint, findLocationSOCQuery);
        
        if (!socResults.hasNext()) {
            System.out.println("⚠️  No Location-related SOCs found!");
            System.out.println("Cannot proceed with DA-SOC enrichment without a Location SOC.");
            return;
        }

        // Inspect each Location SOC
        while (socResults.hasNext()) {
            QuerySolution socSol = socResults.next();
            String socUri = socSol.getResource("soc").getURI();
            String label = socSol.getLiteral("label") != null ? socSol.getLiteral("label").getString() : "N/A";

            System.out.println("Inspecting SOC: " + socUri);
            System.out.println("Label: " + label);
            System.out.println("─────────────────────────────────────────────────────────");

            // Count objects in this SOC
            String countQuery = ns +
                    "SELECT (COUNT(?obj) AS ?count) WHERE { \n" +
                    "  ?obj hasco:isMemberOf <" + socUri + "> . \n" +
                    "}";

            ResultSetRewindable countResults = SPARQLUtils.select(endpoint, countQuery);
            long objectCount = 0;
            if (countResults.hasNext()) {
                QuerySolution countSol = countResults.next();
                if (countSol.getLiteral("count") != null) {
                    objectCount = countSol.getLiteral("count").getLong();
                }
            }

            System.out.println("Total objects in this SOC: " + objectCount);

            if (objectCount == 0) {
                System.out.println("⚠️  This SOC is EMPTY - cannot be enriched\n");
                continue;
            }

            // Get sample originalIDs (first 10)
            String originalIDQuery = ns +
                    "SELECT ?obj ?originalID ?label WHERE { \n" +
                    "  ?obj hasco:isMemberOf <" + socUri + "> . \n" +
                    "  OPTIONAL { ?obj hasco:originalID ?originalID } \n" +
                    "  OPTIONAL { ?obj rdfs:label ?label } \n" +
                    "} LIMIT 10";

            ResultSetRewindable objResults = SPARQLUtils.select(endpoint, originalIDQuery);
            
            System.out.println("\nSample objects (first 10):");
            int objNum = 0;
            Set<String> allOriginalIDs = new HashSet<>();
            
            while (objResults.hasNext()) {
                QuerySolution objSol = objResults.next();
                objNum++;
                String objUri = objSol.getResource("obj").getURI();
                String originalID = objSol.getLiteral("originalID") != null ? 
                    objSol.getLiteral("originalID").getString() : "MISSING";
                String objLabel = objSol.getLiteral("label") != null ? 
                    objSol.getLiteral("label").getString() : "N/A";

                allOriginalIDs.add(originalID);
                
                System.out.println("  " + objNum + ". originalID: " + originalID);
                System.out.println("     Label: " + objLabel);
                System.out.println("     URI: " + objUri);
            }

            // Analyze originalID patterns
            System.out.println("\noriginalID Pattern Analysis:");
            System.out.println("  Unique originalIDs in sample: " + allOriginalIDs.size());
            
            if (allOriginalIDs.contains("MISSING")) {
                System.out.println("  ⚠️  WARNING: Some objects are missing originalID!");
            }

            // Check if any originalID matches DA-SOC-LOCATION.csv pattern
            List<String> csvExpectedIDs = Arrays.asList(
                "LIBRARY-L0", "LIBRARY-L1", "SCIENCE-L0", "SCIENCE-L1",
                "ARTS-L0", "SPORTS-L0", "ADMIN-L0", "CAFE-L0"
            );

            int matchCount = 0;
            for (String id : allOriginalIDs) {
                if (csvExpectedIDs.contains(id)) {
                    matchCount++;
                }
            }

            System.out.println("\nMatch with DA-SOC-LOCATION.csv:");
            System.out.println("  Expected IDs (from CSV): " + csvExpectedIDs.subList(0, 4) + "...");
            System.out.println("  Matching IDs found: " + matchCount + "/" + allOriginalIDs.size());
            
            if (matchCount > 0) {
                System.out.println("  ✓ COMPATIBLE: This SOC can be enriched with DA-SOC-LOCATION.csv");
            } else {
                System.out.println("  ✗ INCOMPATIBLE: originalIDs don't match CSV");
                System.out.println("  Sample actual IDs: " + new ArrayList<>(allOriginalIDs).subList(0, Math.min(3, allOriginalIDs.size())));
            }

            System.out.println("\n");
        }
    }

    @Test
    @Order(3)
    @DisplayName("Test 3: Find best SOC for DA-SOC-LOCATION.csv enrichment")
    public void test3_findBestSOCForEnrichment() {
        System.out.println("\n========================================");
        System.out.println("TEST 3: FINDING BEST SOC FOR ENRICHMENT");
        System.out.println("========================================\n");

        String ns = NameSpaces.getInstance().printSparqlNameSpaceList();
        String endpoint = CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY);

        // Expected originalIDs from DA-SOC-LOCATION.csv
        List<String> expectedIDs = Arrays.asList(
            "LIBRARY-L0", "LIBRARY-L1", "LIBRARY-L2", "LIBRARY-L3", "LIBRARY-ROOF",
            "SCIENCE-L0", "SCIENCE-L1", "SCIENCE-L2", "SCIENCE-L3", "SCIENCE-ROOF",
            "ARTS-L0", "SPORTS-L0", "ADMIN-L0", "CAFE-L0", "MAIN-L0"
        );

        System.out.println("Looking for SOC with these originalIDs:");
        System.out.println("  " + expectedIDs.subList(0, Math.min(5, expectedIDs.size())) + "...");
        System.out.println();

        // Find all SOCs (both StudyObjectCollection and SpaceCollection)
        String allSOCsQuery = ns +
                "SELECT DISTINCT ?soc WHERE { \n" +
                "  { ?soc a hasco:StudyObjectCollection } UNION \n" +
                "  { ?soc a hasco:SpaceCollection } \n" +
                "}";

        ResultSetRewindable socResults = SPARQLUtils.select(endpoint, allSOCsQuery);
        
        String bestSOC = null;
        int bestMatchCount = 0;
        int bestTotalObjects = 0;

        while (socResults.hasNext()) {
            QuerySolution socSol = socResults.next();
            String socUri = socSol.getResource("soc").getURI();

            // Check how many expected IDs exist in this SOC
            String matchQuery = ns +
                    "SELECT (COUNT(?obj) AS ?count) WHERE { \n" +
                    "  ?obj hasco:isMemberOf <" + socUri + "> ; \n" +
                    "       hasco:originalID ?id . \n" +
                    "  FILTER (?id IN (\"LIBRARY-L0\", \"LIBRARY-L1\", \"SCIENCE-L0\", \"SCIENCE-L1\", \"ARTS-L0\")) \n" +
                    "}";

            ResultSetRewindable matchResults = SPARQLUtils.select(endpoint, matchQuery);
            int matchCount = 0;
            if (matchResults.hasNext()) {
                QuerySolution matchSol = matchResults.next();
                if (matchSol.getLiteral("count") != null) {
                    matchCount = matchSol.getLiteral("count").getInt();
                }
            }

            if (matchCount > bestMatchCount) {
                bestMatchCount = matchCount;
                bestSOC = socUri;

                // Count total objects
                String countQuery = ns +
                        "SELECT (COUNT(?obj) AS ?count) WHERE { \n" +
                        "  ?obj hasco:isMemberOf <" + socUri + "> . \n" +
                        "}";
                ResultSetRewindable countResults = SPARQLUtils.select(endpoint, countQuery);
                if (countResults.hasNext()) {
                    QuerySolution countSol = countResults.next();
                    if (countSol.getLiteral("count") != null) {
                        bestTotalObjects = countSol.getLiteral("count").getInt();
                    }
                }
            }
        }

        System.out.println("RESULTS:");
        System.out.println("─────────────────────────────────────────────────────────");
        
        if (bestSOC != null && bestMatchCount > 0) {
            System.out.println("✓ BEST SOC FOUND:");
            System.out.println("  URI: " + bestSOC);
            System.out.println("  Matching originalIDs: " + bestMatchCount + "/" + expectedIDs.size());
            System.out.println("  Total objects in SOC: " + bestTotalObjects);
            System.out.println();
            System.out.println("✓ This SOC can be enriched with DA-SOC-LOCATION.csv");
            System.out.println("  Use this URI in DASOCSimpleIngestionTest");
        } else {
            System.out.println("✗ NO COMPATIBLE SOC FOUND");
            System.out.println();
            System.out.println("RECOMMENDATION:");
            System.out.println("  1. Check if DSG has been ingested");
            System.out.println("  2. Verify the DSG contains a Location SOC");
            System.out.println("  3. Check originalID format in the DSG");
            System.out.println();
            System.out.println("ALTERNATIVE:");
            System.out.println("  Use LinkedSOCElementsSetupTest which creates objects with matching IDs");
            System.out.println("  - Creates 50 objects with IDs: LIBRARY-L0-2, SCIENCE-L0-2, etc.");
            System.out.println("  - Modify DA-SOC-LOCATION.csv to use -2 suffix");
        }
    }
}

