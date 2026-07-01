package org.hascoapi.tests;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.hascoapi.entity.pojo.Task;
import org.hascoapi.entity.pojo.RequiredInstrument;
import org.hascoapi.entity.pojo.ProcessStem;
import org.hascoapi.entity.pojo.Process;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.vocabularies.VSTOI;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.RDF;
import org.hascoapi.vocabularies.RDFS;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * WKFPersistenceTest
 *
 * Tests for RDF/SPARQL persistence validation to ensure WKF entities with new
 * CTT properties are correctly stored and retrieved from the triplestore per
 * WKF Specification v1.0.
 * 
 * NOTE: These tests require a running Fuseki instance and will be skipped if
 * Fuseki is not available. They validate entity save/find round-trips.
 */
@DisplayName("WKF RDF/SPARQL Persistence Tests")
public class WKFPersistenceTest {

    private static final String TEST_GRAPH_URI = "http://example.org/test/wkf-persistence";
    private static boolean fusekiAvailable = false;

    @BeforeAll
    public static void checkFusekiAvailability() {
        try {
            String triplestore = CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY);
            fusekiAvailable = triplestore != null && !triplestore.isEmpty();
            
            if (fusekiAvailable) {
                // Test connection
                String testQuery = "SELECT * WHERE { ?s ?p ?o } LIMIT 1";
                ResultSet rs = SPARQLUtils.select(triplestore, testQuery);
                fusekiAvailable = (rs != null);
            }
        } catch (Exception e) {
            fusekiAvailable = false;
        }
        
        if (!fusekiAvailable) {
            System.out.println("[WKFPersistenceTest] Fuseki not available - tests will be skipped");
        }
    }

    // ========== Task Persistence Tests ==========

    @Test
    @DisplayName("Task with hasIterationConstraint should round-trip correctly")
    public void testTask_IterationConstraintRoundTrip() {
        assumeTrue(fusekiAvailable, "Fuseki triplestore not available");
        
        // Create Task with iteration constraint
        Task task = new Task();
        task.setUri("http://example.org/task/test-iter-roundtrip");
        task.setLabel("Test Task with Iteration");
        task.setTypeUri(VSTOI.TASK);
        task.setHascoTypeUri(VSTOI.TASK);
        task.setHasStatus(VSTOI.CURRENT);
        task.setHasIterationConstraint("at least 3 times");
        task.setNamedGraph(TEST_GRAPH_URI);
        
        // Save to triplestore
        task.save();
        
        // Retrieve from triplestore
        Task retrieved = Task.find(task.getUri());
        
        assertNotNull(retrieved, "Should retrieve task from triplestore");
        assertEquals(task.getLabel(), retrieved.getLabel(), "Label should match");
        assertEquals(task.getHasIterationConstraint(), retrieved.getHasIterationConstraint(),
            "Iteration constraint should match after round-trip");
        
        // Cleanup
        retrieved.delete();
    }

    @Test
    @DisplayName("Task.find() should retrieve hasIterationConstraint from RDF")
    public void testTask_FindRetrievesIterationConstraint() {
        assumeTrue(fusekiAvailable, "Fuseki triplestore not available");
        
        // Create and save Task with iteration constraint
        Task task = new Task();
        String taskUri = "http://example.org/task/test-find-iter";
        task.setUri(taskUri);
        task.setLabel("Findable Task");
        task.setTypeUri(VSTOI.TASK);
        task.setHascoTypeUri(VSTOI.TASK);
        task.setHasIterationConstraint("until completion");
        task.setNamedGraph(TEST_GRAPH_URI);
        task.save();
        
        // Use Task.find() to retrieve
        Task retrieved = Task.find(taskUri);
        
        assertNotNull(retrieved, "Task.find() should return task");
        assertNotNull(retrieved.getHasIterationConstraint(), "Task should have iteration constraint");
        assertEquals("until completion", retrieved.getHasIterationConstraint(),
            "Retrieved iteration constraint should match");
        
        // Cleanup
        retrieved.delete();
    }

    @Test
    @DisplayName("Task with temporal dependency should persist correctly")
    public void testTask_TemporalDependencyPersists() {
        assumeTrue(fusekiAvailable, "Fuseki triplestore not available");
        
        String[] temporalDeps = {"enabling", "concurrent", "choice"};
        
        for (String tempDep : temporalDeps) {
            Task task = new Task();
            task.setUri("http://example.org/task/temp-" + tempDep);
            task.setLabel("Task with " + tempDep);
            task.setTypeUri(VSTOI.TASK);
            task.setHascoTypeUri(VSTOI.TASK);
            task.setHasTemporalDependency(tempDep);
            task.setNamedGraph(TEST_GRAPH_URI);
            task.save();
            
            // Retrieve and verify
            Task retrieved = Task.find(task.getUri());
            assertNotNull(retrieved, "Should find task for " + tempDep);
            assertEquals(tempDep, retrieved.getHasTemporalDependency(),
                "Temporal dependency should match: " + tempDep);
            
            retrieved.delete();
        }
    }

    // ========== RequiredInstrument Persistence Tests ==========

    @Test
    @DisplayName("RequiredInstrument with isRelatedToTask should persist to RDF")
    public void testRequiredInstrument_RelatedToTaskPersists() {
        assumeTrue(fusekiAvailable, "Fuseki triplestore not available");
        
        RequiredInstrument ri = new RequiredInstrument();
        ri.setUri("http://example.org/ri/test-related");
        ri.setLabel("Test RI");
        ri.setUsesInstrument("http://example.org/instrument/inst1");
        ri.setIsRelatedToTask("http://example.org/task/calibrate");
        ri.setNamedGraph(TEST_GRAPH_URI);
        ri.save();
        
        // Retrieve and verify
        RequiredInstrument retrieved = RequiredInstrument.find(ri.getUri());
        assertNotNull(retrieved, "Should retrieve RequiredInstrument");
        assertEquals("http://example.org/task/calibrate", retrieved.getIsRelatedToTask(),
            "Related task should match");
        
        retrieved.delete();
    }

    @Test
    @DisplayName("RequiredInstrument with hasInstrumentConfig should persist to RDF")
    public void testRequiredInstrument_ConfigPersists() {
        assumeTrue(fusekiAvailable, "Fuseki triplestore not available");
        
        String config = "{\"range\": \"0-100\", \"accuracy\": \"1%\"}";
        RequiredInstrument ri = new RequiredInstrument();
        ri.setUri("http://example.org/ri/test-config");
        ri.setLabel("Test RI with Config");
        ri.setUsesInstrument("http://example.org/instrument/inst2");
        ri.setHasInstrumentConfig(config);
        ri.setNamedGraph(TEST_GRAPH_URI);
        ri.save();
        
        // Retrieve and verify
        RequiredInstrument retrieved = RequiredInstrument.find(ri.getUri());
        assertNotNull(retrieved, "Should retrieve RequiredInstrument");
        assertNotNull(retrieved.getHasInstrumentConfig(), "Should have config");
        assertTrue(retrieved.getHasInstrumentConfig().contains("range"),
            "Config should contain range: " + retrieved.getHasInstrumentConfig());
        
        retrieved.delete();
    }

    @Test
    @DisplayName("RequiredInstrument.find() should retrieve isRelatedToTask and hasInstrumentConfig")
    public void testRequiredInstrument_FindRetrievesNewProperties() {
        assumeTrue(fusekiAvailable, "Fuseki triplestore not available");
        
        String riUri = "http://example.org/ri/test-find-all";
        String taskUri = "http://example.org/task/test-task";
        String config = "{\"temp\": \"25C\"}";
        
        RequiredInstrument ri = new RequiredInstrument();
        ri.setUri(riUri);
        ri.setLabel("Complete RI");
        ri.setUsesInstrument("http://example.org/instrument/inst3");
        ri.setIsRelatedToTask(taskUri);
        ri.setHasInstrumentConfig(config);
        ri.setNamedGraph(TEST_GRAPH_URI);
        ri.save();
        
        // Retrieve
        RequiredInstrument retrieved = RequiredInstrument.find(riUri);
        
        assertNotNull(retrieved, "RequiredInstrument.find() should return entity");
        assertNotNull(retrieved.getIsRelatedToTask(), "Should have isRelatedToTask");
        assertEquals(taskUri, retrieved.getIsRelatedToTask(), "Related task should match");
        
        assertNotNull(retrieved.getHasInstrumentConfig(), "Should have hasInstrumentConfig");
        assertTrue(retrieved.getHasInstrumentConfig().contains("temp"), 
            "Config should match: " + retrieved.getHasInstrumentConfig());
        
        retrieved.delete();
    }

    @Test
    @DisplayName("RequiredInstrument with all properties should round-trip correctly")
    public void testRequiredInstrument_RoundTrip() {
        assumeTrue(fusekiAvailable, "Fuseki triplestore not available");
        
        // Create RequiredInstrument with all properties
        RequiredInstrument original = new RequiredInstrument();
        original.setUri("http://example.org/ri/roundtrip");
        original.setLabel("Test RI");
        original.setUsesInstrument("http://example.org/instrument/inst1");
        original.setIsRelatedToTask("http://example.org/task/task1");
        original.setHasInstrumentConfig("{\"key\": \"value\"}");
        original.setNamedGraph(TEST_GRAPH_URI);
        
        // Save to triplestore
        original.save();
        
        // Retrieve from triplestore
        RequiredInstrument retrieved = RequiredInstrument.find(original.getUri());
        
        assertNotNull(retrieved, "Should retrieve RequiredInstrument");
        assertEquals(original.getLabel(), retrieved.getLabel(), "Label should match");
        assertEquals(original.getUsesInstrument(), retrieved.getUsesInstrument(), 
            "UsesInstrument should match");
        assertEquals(original.getIsRelatedToTask(), retrieved.getIsRelatedToTask(),
            "IsRelatedToTask should match");
        assertNotNull(retrieved.getHasInstrumentConfig(), "Should have config");
        // JSON quotes may be escaped in RDF - just check key content is present
        assertTrue(retrieved.getHasInstrumentConfig().contains("key") && 
                   retrieved.getHasInstrumentConfig().contains("value"),
            "HasInstrumentConfig should contain key and value");
        
        retrieved.delete();
    }

    // ========== Simplified Helper Methods ==========

    // No model creation helpers needed - tests use entity save/find methods directly
}
