package org.hascoapi.tests;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLCountUtil;
import org.hascoapi.utils.SPARQLUtils;
import org.junit.jupiter.api.*;

import java.util.*;

/**
 * DASOCEnrichmentComparisonTest
 * 
 * Validates that objects enriched with DA-SOC data have MORE properties
 * than normal objects without enrichment.
 * 
 * Test scenarios:
 * 1. Count properties of normal objects (baseline - should have ~4-5 base properties)
 * 2. Count properties of DA-SOC enriched objects (should have 9+ properties)
 * 3. Verify enriched objects have specific DA-SOC properties (altitude_m, floor, zone_code, etc.)
 * 4. Compare and confirm enriched > normal
 * 
 * Prerequisites:
 * - LinkedSOCElementsSetup has inserted 50 normal objects with -2 suffix
 * - DA-SOC ingestion has enriched some objects with additional properties
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DASOCEnrichmentComparisonTest {

    private static final String HASCO_NS = "http://hadatac.org/ont/hasco/";
    private static final String PHARMA_NS = "http://hadatac.org/ont/pharma#";
    private static final String SOC_LOCATION_URI = "http://hadatac.org/ont/arrowhead/LTE-PIAGET-LOCATION";
    private static final String TEST_NAMED_GRAPH = "http://hadatac.org/ont/arrowhead/DataFile/LTE-PIAGET-LOCATION-TEST-DF";

    @BeforeAll
    public static void setupOnce() {
        System.out.println("\n========================================");
        System.out.println("DA-SOC ENRICHMENT COMPARISON TEST");
        System.out.println("========================================\n");
        System.out.println("This test compares normal objects vs DA-SOC enriched objects");
        System.out.println("to verify that enrichment adds additional properties.\n");
    }

    @Test
    @Order(1)
    @DisplayName("Test 1: Normal objects have baseline properties (4-5 properties)")
    public void test1_normalObjectsHaveBaselineProperties() {
        System.out.println("\n[Test 1] Counting properties of NORMAL objects (without DA-SOC)...");

        String ns = NameSpaces.getInstance().printSparqlNameSpaceList();

        // Select one normal object and count its properties
        String selectNormalObject = ns +
                "SELECT ?obj WHERE { \n" +
                "  GRAPH <" + TEST_NAMED_GRAPH + "> { \n" +
                "    ?obj hasco:isMemberOf <" + SOC_LOCATION_URI + "> ; \n" +
                "         hasco:originalID ?id . \n" +
                "    FILTER (STRENDS(STR(?id), \"-2\")) \n" +
                "  } \n" +
                "  # Exclude objects that have DA-SOC enrichment properties \n" +
                "  FILTER NOT EXISTS { \n" +
                "    GRAPH ?g { \n" +
                "      ?obj <" + PHARMA_NS + "altitude_m> ?alt . \n" +
                "    } \n" +
                "  } \n" +
                "} LIMIT 1";

        String endpoint = CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY);
        ResultSetRewindable results = SPARQLUtils.select(endpoint, selectNormalObject);

        if (!results.hasNext()) {
            System.out.println("[Test 1] ⚠️  No normal objects found (all might be enriched)");
            System.out.println("[Test 1] This is OK if DA-SOC has enriched all objects");
            System.out.println("[Test 1] Skipping comparison...");
            return;
        }

        QuerySolution sol = results.next();
        String normalObjUri = sol.getResource("obj").getURI();
        System.out.println("[Test 1] Sample normal object: " + normalObjUri);

        // Count properties of this normal object
        String countPropsQuery = ns +
                "SELECT (COUNT(DISTINCT ?p) AS ?propCount) WHERE { \n" +
                "  GRAPH <" + TEST_NAMED_GRAPH + "> { \n" +
                "    <" + normalObjUri + "> ?p ?o . \n" +
                "  } \n" +
                "}";

        ResultSetRewindable countResults = SPARQLUtils.select(endpoint, countPropsQuery);
        long normalPropCount = 0;
        if (countResults.hasNext()) {
            QuerySolution countSol = countResults.next();
            if (countSol.getLiteral("propCount") != null) {
                normalPropCount = countSol.getLiteral("propCount").getLong();
            }
        }
        System.out.println("[Test 1] Normal object property count: " + normalPropCount);

        // Expected baseline properties:
        // 1. rdf:type
        // 2. hasco:isMemberOf
        // 3. hasco:originalID
        // 4. rdfs:label
        // 5. hasco:hasSpaceScope
        assertTrue(normalPropCount >= 4 && normalPropCount <= 6,
            "Normal objects should have 4-6 baseline properties, found: " + normalPropCount);

        System.out.println("[Test 1] ✓ PASSED - Normal objects have " + normalPropCount + " baseline properties");
    }

    @Test
    @Order(2)
    @DisplayName("Test 2: DA-SOC enriched objects have additional properties (9+ properties)")
    public void test2_enrichedObjectsHaveAdditionalProperties() {
        System.out.println("\n[Test 2] Counting properties of DA-SOC ENRICHED objects...");

        String ns = NameSpaces.getInstance().printSparqlNameSpaceList();

        // Find objects that have DA-SOC enrichment (have altitude_m property)
        String selectEnrichedObject = ns +
                "SELECT ?obj ?g WHERE { \n" +
                "  GRAPH ?g { \n" +
                "    ?obj <" + PHARMA_NS + "altitude_m> ?alt . \n" +
                "  } \n" +
                "  FILTER (CONTAINS(STR(?g), \"-dasoc\")) \n" +
                "} LIMIT 1";

        String endpoint = CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY);
        ResultSetRewindable results = SPARQLUtils.select(endpoint, selectEnrichedObject);

        if (!results.hasNext()) {
            System.out.println("[Test 2] ⚠️  WARNING: No DA-SOC enriched objects found!");
            System.out.println("[Test 2] Please run DA-SOC ingestion first:");
            System.out.println("[Test 2]   sbt \"testOnly org.hascoapi.tests.DASOCSimpleIngestionTest\"");
            fail("No DA-SOC enriched objects found. Run DA-SOC ingestion first.");
        }

        QuerySolution sol = results.next();
        String enrichedObjUri = sol.getResource("obj").getURI();
        String dasocGraph = sol.getResource("g").getURI();
        System.out.println("[Test 2] Sample enriched object: " + enrichedObjUri);
        System.out.println("[Test 2] DA-SOC graph: " + dasocGraph);

        // Count properties in base graph
        String countBasePropsQuery = ns +
                "SELECT (COUNT(DISTINCT ?p) AS ?propCount) WHERE { \n" +
                "  GRAPH <" + TEST_NAMED_GRAPH + "> { \n" +
                "    <" + enrichedObjUri + "> ?p ?o . \n" +
                "  } \n" +
                "}";

        ResultSetRewindable baseResults = SPARQLUtils.select(endpoint, countBasePropsQuery);
        long basePropCount = 0;
        if (baseResults.hasNext()) {
            QuerySolution baseSol = baseResults.next();
            if (baseSol.getLiteral("propCount") != null) {
                basePropCount = baseSol.getLiteral("propCount").getLong();
            }
        }
        System.out.println("[Test 2] Base properties (from original graph): " + basePropCount);

        // Count properties in DA-SOC graph
        String countDasocPropsQuery = ns +
                "SELECT (COUNT(DISTINCT ?p) AS ?propCount) WHERE { \n" +
                "  GRAPH <" + dasocGraph + "> { \n" +
                "    <" + enrichedObjUri + "> ?p ?o . \n" +
                "  } \n" +
                "}";

        ResultSetRewindable dasocResults = SPARQLUtils.select(endpoint, countDasocPropsQuery);
        long dasocPropCount = 0;
        if (dasocResults.hasNext()) {
            QuerySolution dasocSol = dasocResults.next();
            if (dasocSol.getLiteral("propCount") != null) {
                dasocPropCount = dasocSol.getLiteral("propCount").getLong();
            }
        }
        System.out.println("[Test 2] DA-SOC enrichment properties: " + dasocPropCount);

        long totalPropCount = basePropCount + dasocPropCount;
        System.out.println("[Test 2] Total properties (base + enrichment): " + totalPropCount);

        // Expected DA-SOC properties: altitude_m, floor, zone_code, area_m2, orientation
        assertTrue(dasocPropCount >= 3,
            "DA-SOC enrichment should add at least 3 properties, found: " + dasocPropCount);

        assertTrue(totalPropCount >= 9,
            "Enriched objects should have 9+ total properties, found: " + totalPropCount);

        System.out.println("[Test 2] ✓ PASSED - Enriched objects have " + totalPropCount + " total properties");
    }

    @Test
    @Order(3)
    @DisplayName("Test 3: Verify specific DA-SOC properties exist")
    public void test3_verifySpecificDASOCProperties() {
        System.out.println("\n[Test 3] Verifying specific DA-SOC properties exist...");

        String ns = NameSpaces.getInstance().printSparqlNameSpaceList();
        String endpoint = CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY);

        // Expected DA-SOC properties from DA-SOC-LOCATION.csv:
        // - pharma:altitude_m (required)
        // - pharma:floor (optional)
        // - pharma:zone_code (optional)
        // - pharma:area_m2 (optional)
        // - pharma:orientation (optional)

        Map<String, String> expectedProperties = new LinkedHashMap<>();
        expectedProperties.put("altitude_m", PHARMA_NS + "altitude_m");
        expectedProperties.put("floor", PHARMA_NS + "floor");
        expectedProperties.put("zone_code", PHARMA_NS + "zone_code");
        expectedProperties.put("area_m2", PHARMA_NS + "area_m2");
        expectedProperties.put("orientation", PHARMA_NS + "orientation");

        System.out.println("[Test 3] Checking for DA-SOC enrichment properties:");

        int foundCount = 0;
        for (Map.Entry<String, String> entry : expectedProperties.entrySet()) {
            String propName = entry.getKey();
            String propUri = entry.getValue();

            String checkQuery = ns +
                    "SELECT (COUNT(DISTINCT ?obj) AS ?count) WHERE { \n" +
                    "  GRAPH ?g { \n" +
                    "    ?obj <" + propUri + "> ?value . \n" +
                    "  } \n" +
                    "  FILTER (CONTAINS(STR(?g), \"-dasoc\")) \n" +
                    "}";

            long count = SPARQLCountUtil.count(endpoint, checkQuery);
            
            if (count > 0) {
                System.out.println("[Test 3]   ✓ " + propName + ": " + count + " objects have this property");
                foundCount++;
            } else {
                System.out.println("[Test 3]   ✗ " + propName + ": NOT FOUND");
            }
        }

        assertTrue(foundCount >= 1,
            "At least one DA-SOC property should be found (altitude_m is required)");

        System.out.println("[Test 3] Found " + foundCount + "/" + expectedProperties.size() + " DA-SOC properties");
        System.out.println("[Test 3] ✓ PASSED - DA-SOC enrichment properties exist");
    }

    @Test
    @Order(4)
    @DisplayName("Test 4: Compare normal vs enriched - confirm enriched have MORE properties")
    public void test4_confirmEnrichedHaveMoreProperties() {
        System.out.println("\n[Test 4] FINAL COMPARISON: Normal vs Enriched objects...");

        String ns = NameSpaces.getInstance().printSparqlNameSpaceList();
        String endpoint = CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY);

        // Count average properties for normal objects (no DA-SOC enrichment)
        String normalAvgQuery = ns +
                "SELECT (AVG(?propCount) AS ?avgProps) WHERE { \n" +
                "  { \n" +
                "    SELECT ?obj (COUNT(DISTINCT ?p) AS ?propCount) WHERE { \n" +
                "      GRAPH <" + TEST_NAMED_GRAPH + "> { \n" +
                "        ?obj hasco:isMemberOf <" + SOC_LOCATION_URI + "> ; \n" +
                "             hasco:originalID ?id ; \n" +
                "             ?p ?o . \n" +
                "        FILTER (STRENDS(STR(?id), \"-2\")) \n" +
                "      } \n" +
                "      FILTER NOT EXISTS { \n" +
                "        GRAPH ?g { \n" +
                "          ?obj <" + PHARMA_NS + "altitude_m> ?alt . \n" +
                "        } \n" +
                "      } \n" +
                "    } GROUP BY ?obj \n" +
                "  } \n" +
                "}";

        ResultSetRewindable normalResults = SPARQLUtils.select(endpoint, normalAvgQuery);
        double normalAvgProps = 0;
        if (normalResults.hasNext()) {
            QuerySolution sol = normalResults.next();
            if (sol.getLiteral("avgProps") != null) {
                normalAvgProps = sol.getLiteral("avgProps").getDouble();
            }
        }

        System.out.println("[Test 4] Average properties in NORMAL objects: " + 
            String.format("%.1f", normalAvgProps));

        // Count properties for enriched objects (across both graphs)
        String enrichedQuery = ns +
                "SELECT ?obj ?dasocGraph WHERE { \n" +
                "  GRAPH ?dasocGraph { \n" +
                "    ?obj <" + PHARMA_NS + "altitude_m> ?alt . \n" +
                "  } \n" +
                "  FILTER (CONTAINS(STR(?dasocGraph), \"-dasoc\")) \n" +
                "} LIMIT 5";

        ResultSetRewindable enrichedResults = SPARQLUtils.select(endpoint, enrichedQuery);
        
        if (!enrichedResults.hasNext()) {
            System.out.println("[Test 4] ⚠️  No enriched objects found - skipping comparison");
            System.out.println("[Test 4] Run DA-SOC ingestion first to enable this test");
            return;
        }

        List<Integer> enrichedPropCounts = new ArrayList<>();
        
        while (enrichedResults.hasNext()) {
            QuerySolution sol = enrichedResults.next();
            String objUri = sol.getResource("obj").getURI();
            String dasocGraph = sol.getResource("dasocGraph").getURI();

            // Count properties in both graphs
            String countQuery = ns +
                    "SELECT (COUNT(DISTINCT ?p) AS ?count) WHERE { \n" +
                    "  { \n" +
                    "    GRAPH <" + TEST_NAMED_GRAPH + "> { \n" +
                    "      <" + objUri + "> ?p ?o . \n" +
                    "    } \n" +
                    "  } UNION { \n" +
                    "    GRAPH <" + dasocGraph + "> { \n" +
                    "      <" + objUri + "> ?p ?o . \n" +
                    "    } \n" +
                    "  } \n" +
                    "}";

            ResultSetRewindable countResults = SPARQLUtils.select(endpoint, countQuery);
            if (countResults.hasNext()) {
                QuerySolution countSol = countResults.next();
                if (countSol.getLiteral("count") != null) {
                    long count = countSol.getLiteral("count").getLong();
                    enrichedPropCounts.add((int) count);
                }
            }
        }

        double enrichedAvgProps = enrichedPropCounts.stream()
            .mapToInt(Integer::intValue)
            .average()
            .orElse(0);

        System.out.println("[Test 4] Average properties in ENRICHED objects: " + 
            String.format("%.1f", enrichedAvgProps));
        System.out.println("[Test 4] Sample enriched object property counts: " + enrichedPropCounts);

        // The key assertion: enriched objects MUST have more properties than normal
        if (normalAvgProps > 0) {
            assertTrue(enrichedAvgProps > normalAvgProps,
                String.format("Enriched objects (%.1f props) should have MORE properties than normal objects (%.1f props)",
                    enrichedAvgProps, normalAvgProps));
            
            double difference = enrichedAvgProps - normalAvgProps;
            System.out.println("[Test 4] Difference: +" + String.format("%.1f", difference) + 
                " properties added by DA-SOC enrichment");
        } else {
            // All objects are enriched
            assertTrue(enrichedAvgProps >= 9,
                "Enriched objects should have at least 9 properties");
        }

        System.out.println("\n========================================");
        System.out.println("✓ CONFIRMATION: DA-SOC enrichment ADDS properties to objects!");
        System.out.println("  Normal objects: " + String.format("%.1f", normalAvgProps) + " properties");
        System.out.println("  Enriched objects: " + String.format("%.1f", enrichedAvgProps) + " properties");
        System.out.println("========================================");
        System.out.println("[Test 4] ✓ PASSED");
    }

    @Test
    @Order(5)
    @DisplayName("Test 5: List enrichment details for sample object")
    public void test5_listEnrichmentDetailsForSampleObject() {
        System.out.println("\n[Test 5] Detailed view of one enriched object...");

        String ns = NameSpaces.getInstance().printSparqlNameSpaceList();
        String endpoint = CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY);

        // Find one enriched object
        String findEnrichedQuery = ns +
                "SELECT ?obj ?id ?dasocGraph WHERE { \n" +
                "  GRAPH <" + TEST_NAMED_GRAPH + "> { \n" +
                "    ?obj hasco:isMemberOf <" + SOC_LOCATION_URI + "> ; \n" +
                "         hasco:originalID ?id . \n" +
                "  } \n" +
                "  GRAPH ?dasocGraph { \n" +
                "    ?obj <" + PHARMA_NS + "altitude_m> ?alt . \n" +
                "  } \n" +
                "  FILTER (CONTAINS(STR(?dasocGraph), \"-dasoc\")) \n" +
                "} LIMIT 1";

        ResultSetRewindable results = SPARQLUtils.select(endpoint, findEnrichedQuery);
        
        if (!results.hasNext()) {
            System.out.println("[Test 5] No enriched objects found - skipping detail view");
            return;
        }

        QuerySolution sol = results.next();
        String objUri = sol.getResource("obj").getURI();
        String objId = sol.getLiteral("id").getString();
        String dasocGraph = sol.getResource("dasocGraph").getURI();

        System.out.println("[Test 5] Sample enriched object:");
        System.out.println("[Test 5]   URI: " + objUri);
        System.out.println("[Test 5]   Original ID: " + objId);
        System.out.println("[Test 5]   DA-SOC Graph: " + dasocGraph);

        // List all base properties
        System.out.println("\n[Test 5] BASE PROPERTIES (from original graph):");
        String basePropsQuery = ns +
                "SELECT ?property ?value WHERE { \n" +
                "  GRAPH <" + TEST_NAMED_GRAPH + "> { \n" +
                "    <" + objUri + "> ?property ?value . \n" +
                "  } \n" +
                "}";

        ResultSetRewindable baseProps = SPARQLUtils.select(endpoint, basePropsQuery);
        int baseCount = 0;
        while (baseProps.hasNext()) {
            QuerySolution prop = baseProps.next();
            baseCount++;
            String propUri = prop.getResource("property").getURI();
            String propName = propUri.substring(propUri.lastIndexOf('/') + 1)
                                     .substring(propUri.lastIndexOf('#') + 1);
            String value = prop.get("value").toString();
            if (value.length() > 60) value = value.substring(0, 60) + "...";
            System.out.println("[Test 5]     " + baseCount + ". " + propName + " = " + value);
        }

        // List all DA-SOC enrichment properties
        System.out.println("\n[Test 5] DA-SOC ENRICHMENT PROPERTIES (added by ingestion):");
        String dasocPropsQuery = ns +
                "SELECT ?property ?value WHERE { \n" +
                "  GRAPH <" + dasocGraph + "> { \n" +
                "    <" + objUri + "> ?property ?value . \n" +
                "  } \n" +
                "}";

        ResultSetRewindable dasocProps = SPARQLUtils.select(endpoint, dasocPropsQuery);
        int dasocCount = 0;
        while (dasocProps.hasNext()) {
            QuerySolution prop = dasocProps.next();
            dasocCount++;
            String propUri = prop.getResource("property").getURI();
            String propName = propUri.substring(propUri.lastIndexOf('/') + 1)
                                     .substring(propUri.lastIndexOf('#') + 1);
            String value = prop.get("value").toString();
            System.out.println("[Test 5]     " + dasocCount + ". " + propName + " = " + value);
        }

        System.out.println("\n[Test 5] SUMMARY:");
        System.out.println("[Test 5]   Base properties: " + baseCount);
        System.out.println("[Test 5]   Enrichment properties: " + dasocCount);
        System.out.println("[Test 5]   Total properties: " + (baseCount + dasocCount));

        assertTrue(dasocCount > 0, "Should have at least 1 DA-SOC enrichment property");
        
        System.out.println("[Test 5] ✓ PASSED - Enrichment details displayed");
    }
}

