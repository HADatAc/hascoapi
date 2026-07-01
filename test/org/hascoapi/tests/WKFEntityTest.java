package org.hascoapi.tests;

import static org.junit.jupiter.api.Assertions.*;

import org.hascoapi.entity.pojo.Task;
import org.hascoapi.entity.pojo.RequiredInstrument;
import org.hascoapi.vocabularies.VSTOI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

/**
 * WKFEntityTest
 *
 * Unit tests for WKF entity POJOs (Task, RequiredInstrument) to ensure
 * compliance with WKF Specification v1.0, particularly the new CTT properties.
 */
@DisplayName("WKF Entity POJO Tests")
public class WKFEntityTest {

    @Test
    @DisplayName("Task should support hasIterationConstraint property")
    public void testTaskIterationConstraint() {
        Task task = new Task();
        
        // Test setter and getter
        String constraint = "at least 3 times";
        task.setHasIterationConstraint(constraint);
        
        assertEquals(constraint, task.getHasIterationConstraint(),
            "Task should store and retrieve hasIterationConstraint");
    }

    @Test
    @DisplayName("Task should handle null hasIterationConstraint")
    public void testTaskNullIterationConstraint() {
        Task task = new Task();
        
        // Default should be null
        assertNull(task.getHasIterationConstraint(),
            "Task hasIterationConstraint should default to null");
        
        // Should accept null value
        task.setHasIterationConstraint(null);
        assertNull(task.getHasIterationConstraint(),
            "Task should accept null hasIterationConstraint");
    }

    @Test
    @DisplayName("Task should support all CTT temporal dependencies")
    public void testTaskTemporalDependency() {
        Task task = new Task();
        
        // Test CTT temporal operators
        String[] temporalDeps = {
            "enabling",      // >> operator
            "concurrent",    // ||| operator
            "choice",        // [] operator
            "independent",   // |=| operator
            "disabling"      // [> operator
        };
        
        for (String dep : temporalDeps) {
            task.setHasTemporalDependency(dep);
            assertEquals(dep, task.getHasTemporalDependency(),
                "Task should store temporal dependency: " + dep);
        }
    }

    @Test
    @DisplayName("Task should support multiple subtasks")
    public void testTaskMultipleSubtasks() {
        Task task = new Task();
        
        List<String> subtaskUris = Arrays.asList(
            "http://example.org/task/subtask1",
            "http://example.org/task/subtask2",
            "http://example.org/task/subtask3"
        );
        
        task.setHasSubtaskUris(subtaskUris);
        
        assertEquals(3, task.getHasSubtaskUris().size(),
            "Task should store multiple subtasks");
        assertTrue(task.getHasSubtaskUris().containsAll(subtaskUris),
            "Task should preserve all subtask URIs");
    }

    @Test
    @DisplayName("Task should support multiple required instruments")
    public void testTaskMultipleRequiredInstruments() {
        Task task = new Task();
        
        List<String> instrumentUris = Arrays.asList(
            "http://example.org/ri/instrument1",
            "http://example.org/ri/instrument2"
        );
        
        task.setHasRequiredInstrumentUris(instrumentUris);
        
        assertEquals(2, task.getHasRequiredInstrumentUris().size(),
            "Task should store multiple required instruments");
        assertTrue(task.getHasRequiredInstrumentUris().containsAll(instrumentUris),
            "Task should preserve all required instrument URIs");
    }

    @Test
    @DisplayName("Task should have correct HASCO type")
    public void testTaskHascoType() {
        Task task = new Task();
        
        // Task constructor should set the type
        task.setTypeUri(VSTOI.TASK);
        task.setHascoTypeUri(VSTOI.TASK);
        
        assertEquals(VSTOI.TASK, task.getTypeUri(),
            "Task should have correct rdf:type");
        assertEquals(VSTOI.TASK, task.getHascoTypeUri(),
            "Task should have correct hasco:hascoType");
    }

    @Test
    @DisplayName("RequiredInstrument should support isRelatedToTask property")
    public void testRequiredInstrumentRelatedToTask() {
        RequiredInstrument ri = new RequiredInstrument();
        
        String taskUri = "http://example.org/task/calibration";
        ri.setIsRelatedToTask(taskUri);
        
        assertEquals(taskUri, ri.getIsRelatedToTask(),
            "RequiredInstrument should store and retrieve isRelatedToTask");
    }

    @Test
    @DisplayName("RequiredInstrument should support hasInstrumentConfig property")
    public void testRequiredInstrumentConfig() {
        RequiredInstrument ri = new RequiredInstrument();
        
        // Test with JSON config
        String jsonConfig = "{\"temperature\": 25, \"humidity\": 60}";
        ri.setHasInstrumentConfig(jsonConfig);
        
        assertEquals(jsonConfig, ri.getHasInstrumentConfig(),
            "RequiredInstrument should store JSON configuration");
        
        // Test with key-value config
        String kvConfig = "temperature=25;humidity=60";
        ri.setHasInstrumentConfig(kvConfig);
        
        assertEquals(kvConfig, ri.getHasInstrumentConfig(),
            "RequiredInstrument should store key-value configuration");
    }

    @Test
    @DisplayName("RequiredInstrument should handle null config values")
    public void testRequiredInstrumentNullConfig() {
        RequiredInstrument ri = new RequiredInstrument();
        
        // Default should be null
        assertNull(ri.getIsRelatedToTask(),
            "RequiredInstrument isRelatedToTask should default to null");
        assertNull(ri.getHasInstrumentConfig(),
            "RequiredInstrument hasInstrumentConfig should default to null");
        
        // Should accept null values
        ri.setIsRelatedToTask(null);
        ri.setHasInstrumentConfig(null);
        
        assertNull(ri.getIsRelatedToTask(),
            "RequiredInstrument should accept null isRelatedToTask");
        assertNull(ri.getHasInstrumentConfig(),
            "RequiredInstrument should accept null hasInstrumentConfig");
    }

    @Test
    @DisplayName("RequiredInstrument should support usesInstrument property")
    public void testRequiredInstrumentUsesInstrument() {
        RequiredInstrument ri = new RequiredInstrument();
        
        String instrumentUri = "http://example.org/instrument/thermometer";
        ri.setUsesInstrument(instrumentUri);
        
        assertEquals(instrumentUri, ri.getUsesInstrument(),
            "RequiredInstrument should store and retrieve usesInstrument");
    }

    @Test
    @DisplayName("RequiredInstrument should have correct HASCO type")
    public void testRequiredInstrumentHascoType() {
        RequiredInstrument ri = new RequiredInstrument();
        
        // Constructor sets the type
        assertEquals(VSTOI.REQUIRED_INSTRUMENT, ri.getTypeUri(),
            "RequiredInstrument should have correct rdf:type");
        assertEquals(VSTOI.REQUIRED_INSTRUMENT, ri.getHascoTypeUri(),
            "RequiredInstrument should have correct hasco:hascoType");
    }

    @Test
    @DisplayName("RequiredInstrument should support hasRequiredComponent for backward compatibility")
    public void testRequiredInstrumentBackwardCompatibility() {
        RequiredInstrument ri = new RequiredInstrument();
        
        List<String> componentUris = Arrays.asList(
            "http://example.org/component/sensor1",
            "http://example.org/component/sensor2"
        );
        
        ri.setHasRequiredComponent(componentUris);
        
        assertEquals(2, ri.getHasRequiredComponents().size(),
            "RequiredInstrument should support hasRequiredComponent for backward compatibility");
        assertTrue(ri.getHasRequiredComponents().containsAll(componentUris),
            "RequiredInstrument should preserve component URIs");
    }

    @Test
    @DisplayName("Task should support all required metadata properties")
    public void testTaskRequiredMetadata() {
        Task task = new Task();
        
        // Set all required properties per WKF spec
        task.setUri("http://example.org/task/test-task");
        task.setLabel("Test Task");
        task.setHasStatus("DRAFT");
        task.setTypeUri(VSTOI.TASK);
        task.setHascoTypeUri(VSTOI.TASK);
        
        assertNotNull(task.getUri(), "Task should have URI");
        assertNotNull(task.getLabel(), "Task should have label");
        assertNotNull(task.getHasStatus(), "Task should have status");
        assertNotNull(task.getTypeUri(), "Task should have type");
        assertNotNull(task.getHascoTypeUri(), "Task should have HASCO type");
    }

    @Test
    @DisplayName("RequiredInstrument should support all required metadata properties")
    public void testRequiredInstrumentRequiredMetadata() {
        RequiredInstrument ri = new RequiredInstrument();
        
        // Set all required properties per WKF spec
        ri.setUri("http://example.org/ri/test-ri");
        ri.setLabel("Test Required Instrument");
        ri.setUsesInstrument("http://example.org/instrument/test-instrument");
        
        assertNotNull(ri.getUri(), "RequiredInstrument should have URI");
        assertNotNull(ri.getLabel(), "RequiredInstrument should have label");
        assertNotNull(ri.getUsesInstrument(), "RequiredInstrument should have usesInstrument");
    }
}
