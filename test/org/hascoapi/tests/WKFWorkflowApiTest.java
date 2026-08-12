package org.hascoapi.tests;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.JsonNode;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;

import org.apache.jena.query.ResultSetRewindable;
import org.hascoapi.console.controllers.restapi.DataFileAPI;
import org.hascoapi.console.controllers.restapi.ProcessAPI;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.Process;
import org.hascoapi.entity.pojo.Task;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.VSTOI;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@DisplayName("WKF Workflow API Tests")
public class WKFWorkflowApiTest {

    private static boolean fusekiAvailable = false;

    @BeforeAll
    public static void checkFusekiAvailability() {
        try {
            String sparql = CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY);
            ResultSetRewindable rs = SPARQLUtils.select(sparql, "SELECT * WHERE { ?s ?p ?o } LIMIT 1");
            fusekiAvailable = (rs != null);
        } catch (Exception e) {
            fusekiAvailable = false;
        }
    }

    @Test
    @DisplayName("WKF upload endpoint returns clear error when file is missing")
    public void testWkfUploadWithoutFileBody() {
        assumeTrue(fusekiAvailable, "Fuseki not available");

        String ts = String.valueOf(System.currentTimeMillis());
        String dataFileUri = "http://example.org/DFL-WKF-UPLOAD-" + ts;
        String graph = "http://example.org/graph/wkf-upload-" + ts;

        DataFile dataFile = new DataFile();
        dataFile.setUri(dataFileUri);
        dataFile.setNamedGraph(graph);
        dataFile.setTypeUri(HASCO.DATAFILE);
        dataFile.setHascoTypeUri(HASCO.DATAFILE);
        dataFile.setLabel("WKF Upload Test");
        dataFile.setFilename("WKF-UPLOAD-TEST.xlsx");
        dataFile.setFileStatus(DataFile.UNPROCESSED);
        dataFile.setHasSIRManagerEmail("wkf-test@example.org");
        dataFile.save();

        DataFileAPI api = new DataFileAPI();
        Http.Request request = new Http.RequestBuilder()
                .method("POST")
                .uri("/hascoapi/api/uploadFile/" + dataFileUri + "/WKF-UPLOAD-TEST.xlsx")
                .build();

        Result result = api.uploadFile(dataFileUri, "WKF-UPLOAD-TEST.xlsx", request);
        assertEquals(200, result.status());

        JsonNode json = Json.parse(Helpers.contentAsString(result));
        assertFalse(json.path("isSuccessful").asBoolean(true));
        assertTrue(
                json.path("body").asText("").contains("No file has been provided for ingestion."),
                "Expected missing file error body"
        );
    }

    @Test
    @DisplayName("Task.find deduplicates subtasks across multiple named graphs")
    public void testTaskFindDeduplicatesAcrossGraphs() {
        assumeTrue(fusekiAvailable, "Fuseki not available");

        String ts = String.valueOf(System.currentTimeMillis());
        String taskUri = "http://example.org/task/dedup-" + ts;
        String subtaskUri = "http://example.org/task/sub-" + ts;

        Task g1 = new Task();
        g1.setUri(taskUri);
        g1.setNamedGraph("http://example.org/graph/dedup-g1-" + ts);
        g1.setTypeUri(VSTOI.TASK);
        g1.setHascoTypeUri(VSTOI.TASK);
        g1.setLabel("Dedup Root");
        g1.setHasStatus(VSTOI.CURRENT);
        g1.setHasSubtaskUris(Collections.singletonList(subtaskUri));
        g1.save();

        Task g2 = new Task();
        g2.setUri(taskUri);
        g2.setNamedGraph("http://example.org/graph/dedup-g2-" + ts);
        g2.setTypeUri(VSTOI.TASK);
        g2.setHascoTypeUri(VSTOI.TASK);
        g2.setLabel("Dedup Root");
        g2.setHasStatus(VSTOI.CURRENT);
        g2.setHasSubtaskUris(Collections.singletonList(subtaskUri));
        g2.save();

        Task retrieved = Task.find(taskUri);
        assertNotNull(retrieved);

        List<String> subtasks = retrieved.getHasSubtaskUris();
        assertEquals(1, subtasks.size(), "Subtask URI must be deduplicated");
        assertEquals(subtaskUri, subtasks.get(0));
    }

    @Test
    @DisplayName("Task.find deduplicates required instruments across multiple named graphs")
    public void testTaskFindDeduplicatesRequiredInstrumentsAcrossGraphs() {
        assumeTrue(fusekiAvailable, "Fuseki not available");

        String ts = String.valueOf(System.currentTimeMillis());
        String taskUri = "http://example.org/task/dedup-ri-" + ts;
        String requiredInstrumentUri = "http://example.org/ri/" + ts;

        Task g1 = new Task();
        g1.setUri(taskUri);
        g1.setNamedGraph("http://example.org/graph/dedup-ri-g1-" + ts);
        g1.setTypeUri(VSTOI.TASK);
        g1.setHascoTypeUri(VSTOI.TASK);
        g1.setLabel("Dedup RI Root");
        g1.setHasStatus(VSTOI.CURRENT);
        g1.setHasRequiredInstrumentUris(Collections.singletonList(requiredInstrumentUri));
        g1.save();

        Task g2 = new Task();
        g2.setUri(taskUri);
        g2.setNamedGraph("http://example.org/graph/dedup-ri-g2-" + ts);
        g2.setTypeUri(VSTOI.TASK);
        g2.setHascoTypeUri(VSTOI.TASK);
        g2.setLabel("Dedup RI Root");
        g2.setHasStatus(VSTOI.CURRENT);
        g2.setHasRequiredInstrumentUris(Collections.singletonList(requiredInstrumentUri));
        g2.save();

        Task retrieved = Task.find(taskUri);
        assertNotNull(retrieved);

        List<String> reqInst = retrieved.getHasRequiredInstrumentUris();
        assertEquals(1, reqInst.size(), "Required instrument URI must be deduplicated");
        assertEquals(requiredInstrumentUri, reqInst.get(0));
    }

    @Test
    @DisplayName("Task.find normalizes legacy WKF#/ required instrument URI variants")
    public void testTaskFindNormalizesLegacyRequiredInstrumentUris() {
        assumeTrue(fusekiAvailable, "Fuseki not available");

        String ts = String.valueOf(System.currentTimeMillis());
        String taskUri = "https://pmsr.net/ont/WKF-TEST-" + ts + "/TSK/0001";
        String canonicalRi = "https://pmsr.net/ont/WKF-TEST-" + ts + "/RIN/RIN001";
        String legacyRi = "https://pmsr.net/ont/WKF#/WKF-TEST-" + ts + "/RIN/RIN001";

        Task g1 = new Task();
        g1.setUri(taskUri);
        g1.setNamedGraph("http://example.org/graph/dedup-ri-legacy-g1-" + ts);
        g1.setTypeUri(VSTOI.TASK);
        g1.setHascoTypeUri(VSTOI.TASK);
        g1.setLabel("Legacy RI Root");
        g1.setHasStatus(VSTOI.CURRENT);
        g1.setHasRequiredInstrumentUris(Collections.singletonList(canonicalRi));
        g1.save();

        Task g2 = new Task();
        g2.setUri(taskUri);
        g2.setNamedGraph("http://example.org/graph/dedup-ri-legacy-g2-" + ts);
        g2.setTypeUri(VSTOI.TASK);
        g2.setHascoTypeUri(VSTOI.TASK);
        g2.setLabel("Legacy RI Root");
        g2.setHasStatus(VSTOI.CURRENT);
        g2.setHasRequiredInstrumentUris(Collections.singletonList(legacyRi));
        g2.save();

        Task retrieved = Task.find(taskUri);
        assertNotNull(retrieved);

        List<String> reqInst = retrieved.getHasRequiredInstrumentUris();
        assertEquals(1, reqInst.size(), "Legacy and canonical URI variants must collapse to one value");
        assertEquals(canonicalRi, reqInst.get(0));
    }

    @Test
    @DisplayName("Process tasks endpoint returns flat list with stable workflow fields")
    public void testProcessTasksEndpointFlatList() {
        assumeTrue(fusekiAvailable, "Fuseki not available");

        String ts = String.valueOf(System.currentTimeMillis());
        String graph = "http://example.org/graph/process-flat-" + ts;

        String procUri = "http://example.org/process/" + ts;
        String topUri = "http://example.org/task/top-" + ts;
        String t1Uri = "http://example.org/task/t1-" + ts;
        String t2Uri = "http://example.org/task/t2-" + ts;

        Task top = new Task();
        top.setUri(topUri);
        top.setNamedGraph(graph);
        top.setTypeUri(VSTOI.TASK);
        top.setHascoTypeUri(VSTOI.TASK);
        top.setLabel("Top Task");
        top.setHasStatus(VSTOI.CURRENT);
        top.setHasSubtaskUris(Arrays.asList(t1Uri, t2Uri));
        top.save();

        Task t1 = new Task();
        t1.setUri(t1Uri);
        t1.setNamedGraph(graph);
        t1.setTypeUri(VSTOI.TASK);
        t1.setHascoTypeUri(VSTOI.TASK);
        t1.setLabel("Task 1");
        t1.setHasStatus(VSTOI.CURRENT);
        t1.setHasSupertaskUri(topUri);
        t1.save();

        Task t2 = new Task();
        t2.setUri(t2Uri);
        t2.setNamedGraph(graph);
        t2.setTypeUri(VSTOI.TASK);
        t2.setHascoTypeUri(VSTOI.TASK);
        t2.setLabel("Task 2");
        t2.setHasStatus(VSTOI.CURRENT);
        t2.setHasSupertaskUri(topUri);
        t2.save();

        Process p = new Process();
        p.setUri(procUri);
        p.setNamedGraph(graph);
        p.setTypeUri(VSTOI.PROCESS);
        p.setHascoTypeUri(VSTOI.PROCESS);
        p.setLabel("Process Flat");
        p.setHasStatus(VSTOI.CURRENT);
        p.setHasTopTaskUri(topUri);
        p.save();

        ProcessAPI api = new ProcessAPI();
        Result result = api.getTasksByProcess(procUri);
        assertEquals(200, result.status());

        JsonNode json = Json.parse(Helpers.contentAsString(result));
        assertTrue(json.path("isSuccessful").asBoolean(false));

        JsonNode body = json.path("body");
        assertEquals(procUri, body.path("processUri").asText());
        assertEquals(topUri, body.path("topTaskUri").asText());

        JsonNode tasks = body.path("tasks");
        assertTrue(tasks.isArray());
        assertEquals(3, tasks.size());

        for (JsonNode task : tasks) {
            assertTrue(task.has("uri"));
            assertTrue(task.has("hasSupertaskUri"));
            assertTrue(task.has("hasSubtaskUris"));
            assertTrue(task.has("usesComponentInstanceUris"));
        }
    }

    @Test
    @DisplayName("Process tasks endpoint handles task cycles without duplicate nodes")
    public void testProcessTasksEndpointCycleSafe() {
        assumeTrue(fusekiAvailable, "Fuseki not available");

        String ts = String.valueOf(System.currentTimeMillis());
        String graph = "http://example.org/graph/process-cycle-" + ts;

        String procUri = "http://example.org/process/cycle-" + ts;
        String aUri = "http://example.org/task/a-" + ts;
        String bUri = "http://example.org/task/b-" + ts;

        Task a = new Task();
        a.setUri(aUri);
        a.setNamedGraph(graph);
        a.setTypeUri(VSTOI.TASK);
        a.setHascoTypeUri(VSTOI.TASK);
        a.setLabel("A");
        a.setHasStatus(VSTOI.CURRENT);
        a.setHasSubtaskUris(Collections.singletonList(bUri));
        a.save();

        Task b = new Task();
        b.setUri(bUri);
        b.setNamedGraph(graph);
        b.setTypeUri(VSTOI.TASK);
        b.setHascoTypeUri(VSTOI.TASK);
        b.setLabel("B");
        b.setHasStatus(VSTOI.CURRENT);
        b.setHasSubtaskUris(Collections.singletonList(aUri));
        b.setHasSupertaskUri(aUri);
        b.save();

        Process p = new Process();
        p.setUri(procUri);
        p.setNamedGraph(graph);
        p.setTypeUri(VSTOI.PROCESS);
        p.setHascoTypeUri(VSTOI.PROCESS);
        p.setLabel("Process Cycle");
        p.setHasStatus(VSTOI.CURRENT);
        p.setHasTopTaskUri(aUri);
        p.save();

        ProcessAPI api = new ProcessAPI();
        Result result = api.getTasksByProcess(procUri);
        assertEquals(200, result.status());

        JsonNode json = Json.parse(Helpers.contentAsString(result));
        assertTrue(json.path("isSuccessful").asBoolean(false));

        JsonNode tasks = json.path("body").path("tasks");
        assertEquals(2, tasks.size(), "Cycle-safe traversal should include each task once");

        Set<String> uris = new HashSet<String>();
        for (JsonNode t : tasks) {
            uris.add(t.path("uri").asText());
        }
        assertEquals(2, uris.size(), "No duplicate task nodes expected");
        assertTrue(uris.contains(aUri));
        assertTrue(uris.contains(bUri));
    }

    @Test
    @DisplayName("Process tasks endpoint returns a deep hierarchy with one root and consistent links")
    public void testProcessTasksEndpointDeepHierarchyIntegrity() {
        assumeTrue(fusekiAvailable, "Fuseki not available");

        String ts = String.valueOf(System.currentTimeMillis());
        String graph = "http://example.org/graph/process-deep-" + ts;

        String procUri = "http://example.org/process/deep-" + ts;
        String topUri = "http://example.org/task/top-" + ts;
        String aUri = "http://example.org/task/a-" + ts;
        String bUri = "http://example.org/task/b-" + ts;
        String cUri = "http://example.org/task/c-" + ts;
        String dUri = "http://example.org/task/d-" + ts;
        String riUri = "http://example.org/ri/deep-" + ts;

        Task top = new Task();
        top.setUri(topUri);
        top.setNamedGraph(graph);
        top.setTypeUri(VSTOI.TASK);
        top.setHascoTypeUri(VSTOI.TASK);
        top.setLabel("Top");
        top.setHasStatus(VSTOI.CURRENT);
        top.setHasSubtaskUris(Arrays.asList(aUri, bUri));
        top.save();

        Task a = new Task();
        a.setUri(aUri);
        a.setNamedGraph(graph);
        a.setTypeUri(VSTOI.TASK);
        a.setHascoTypeUri(VSTOI.TASK);
        a.setLabel("A");
        a.setHasStatus(VSTOI.CURRENT);
        a.setHasSupertaskUri(topUri);
        a.setHasSubtaskUris(Collections.singletonList(cUri));
        a.save();

        Task b = new Task();
        b.setUri(bUri);
        b.setNamedGraph(graph);
        b.setTypeUri(VSTOI.TASK);
        b.setHascoTypeUri(VSTOI.TASK);
        b.setLabel("B");
        b.setHasStatus(VSTOI.CURRENT);
        b.setHasSupertaskUri(topUri);
        b.save();

        Task c = new Task();
        c.setUri(cUri);
        c.setNamedGraph(graph);
        c.setTypeUri(VSTOI.TASK);
        c.setHascoTypeUri(VSTOI.TASK);
        c.setLabel("C");
        c.setHasStatus(VSTOI.CURRENT);
        c.setHasSupertaskUri(aUri);
        c.setHasSubtaskUris(Collections.singletonList(dUri));
        c.save();

        Task d = new Task();
        d.setUri(dUri);
        d.setNamedGraph(graph);
        d.setTypeUri(VSTOI.TASK);
        d.setHascoTypeUri(VSTOI.TASK);
        d.setLabel("D");
        d.setHasStatus(VSTOI.CURRENT);
        d.setHasSupertaskUri(cUri);
        d.setUsesComponentInstanceUris(Collections.singletonList(riUri));
        d.save();

        Process p = new Process();
        p.setUri(procUri);
        p.setNamedGraph(graph);
        p.setTypeUri(VSTOI.PROCESS);
        p.setHascoTypeUri(VSTOI.PROCESS);
        p.setLabel("Process Deep");
        p.setHasStatus(VSTOI.CURRENT);
        p.setHasTopTaskUri(topUri);
        p.save();

        ProcessAPI api = new ProcessAPI();
        Result result = api.getTasksByProcess(procUri);
        assertEquals(200, result.status());

        JsonNode json = Json.parse(Helpers.contentAsString(result));
        assertTrue(json.path("isSuccessful").asBoolean(false));

        JsonNode tasks = json.path("body").path("tasks");
        assertEquals(5, tasks.size(), "All five tasks in deep hierarchy should be returned");

        Set<String> uris = new HashSet<String>();
        int rootCount = 0;
        for (JsonNode t : tasks) {
            String uri = t.path("uri").asText("");
            uris.add(uri);
            if (t.path("hasSupertaskUri").asText("").isEmpty()) {
                rootCount++;
            }
            for (JsonNode sub : t.path("hasSubtaskUris")) {
                String subUri = sub.asText("");
                assertFalse(subUri.isEmpty());
            }
        }

        assertEquals(5, uris.size(), "Task list should not duplicate task URIs");
        assertEquals(1, rootCount, "Deep hierarchy should have exactly one root task");

        // Ensure deepest node carries required instrument list in stable field.
        JsonNode deepest = null;
        for (JsonNode t : tasks) {
            if (dUri.equals(t.path("uri").asText())) {
                deepest = t;
                break;
            }
        }
        assertNotNull(deepest, "Deepest task must be present");
        assertEquals(1, deepest.path("usesComponentInstanceUris").size());
        assertEquals(riUri, deepest.path("usesComponentInstanceUris").get(0).asText());
    }

    @Test
    @DisplayName("Process tasks endpoint deduplicates repeated subtask links coming from cross-graph duplicates")
    public void testProcessTasksEndpointDeduplicatesCrossGraphSubtaskLinks() {
        assumeTrue(fusekiAvailable, "Fuseki not available");

        String ts = String.valueOf(System.currentTimeMillis());
        String procGraph = "http://example.org/graph/process-cross-" + ts;
        String g1 = "http://example.org/graph/cross-g1-" + ts;
        String g2 = "http://example.org/graph/cross-g2-" + ts;

        String procUri = "http://example.org/process/cross-" + ts;
        String topUri = "http://example.org/task/cross-top-" + ts;
        String childUri = "http://example.org/task/cross-child-" + ts;

        Task top1 = new Task();
        top1.setUri(topUri);
        top1.setNamedGraph(g1);
        top1.setTypeUri(VSTOI.TASK);
        top1.setHascoTypeUri(VSTOI.TASK);
        top1.setLabel("Cross Top");
        top1.setHasStatus(VSTOI.CURRENT);
        top1.setHasSubtaskUris(Collections.singletonList(childUri));
        top1.save();

        Task top2 = new Task();
        top2.setUri(topUri);
        top2.setNamedGraph(g2);
        top2.setTypeUri(VSTOI.TASK);
        top2.setHascoTypeUri(VSTOI.TASK);
        top2.setLabel("Cross Top");
        top2.setHasStatus(VSTOI.CURRENT);
        top2.setHasSubtaskUris(Collections.singletonList(childUri));
        top2.save();

        Task child = new Task();
        child.setUri(childUri);
        child.setNamedGraph(g1);
        child.setTypeUri(VSTOI.TASK);
        child.setHascoTypeUri(VSTOI.TASK);
        child.setLabel("Cross Child");
        child.setHasStatus(VSTOI.CURRENT);
        child.setHasSupertaskUri(topUri);
        child.save();

        Process p = new Process();
        p.setUri(procUri);
        p.setNamedGraph(procGraph);
        p.setTypeUri(VSTOI.PROCESS);
        p.setHascoTypeUri(VSTOI.PROCESS);
        p.setLabel("Process Cross");
        p.setHasStatus(VSTOI.CURRENT);
        p.setHasTopTaskUri(topUri);
        p.save();

        ProcessAPI api = new ProcessAPI();
        Result result = api.getTasksByProcess(procUri);
        assertEquals(200, result.status());

        JsonNode json = Json.parse(Helpers.contentAsString(result));
        assertTrue(json.path("isSuccessful").asBoolean(false));
        JsonNode tasks = json.path("body").path("tasks");
        assertEquals(2, tasks.size(), "Should include only top and child tasks once");

        JsonNode topNode = null;
        for (JsonNode t : tasks) {
            if (topUri.equals(t.path("uri").asText())) {
                topNode = t;
                break;
            }
        }
        assertNotNull(topNode, "Top task must be present");
        assertEquals(1, topNode.path("hasSubtaskUris").size(), "Cross-graph duplicate subtask links must be deduplicated");
        assertEquals(childUri, topNode.path("hasSubtaskUris").get(0).asText());
    }
}
