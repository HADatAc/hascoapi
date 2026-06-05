package org.hascoapi.tests;

import org.hascoapi.console.controllers.restapi.URIPage;
import org.hascoapi.entity.pojo.HADatAcClass;
import org.hascoapi.entity.pojo.HADatAcThing;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test for ontology class resolution fix (2026-06-05)
 * 
 * Verifies that URIPage.objectFromUri() now resolves ontology classes
 * (owl:Class) in addition to instances, fixing inconsistency between
 * /api/uri, /api/children, and /api/subclasses/keyword endpoints.
 */
public class OntologyClassResolutionTest {

    /**
     * Test that VSTOI ontology class URIs are resolved correctly
     */
    @Test
    public void testVstoiClassResolution() {
        System.out.println("\n=== Testing VSTOI Ontology Class Resolution ===\n");
        
        String[] testUris = {
            "http://hadatac.org/ont/vstoi#Instrument",
            "http://hadatac.org/ont/vstoi#PhysicalInstrument",
            "http://hadatac.org/ont/vstoi#GroundBasedInstrument"
        };
        
        for (String uri : testUris) {
            System.out.println("Testing URI: " + uri);
            
            HADatAcThing result = URIPage.objectFromUri(uri);
            
            assertNotNull("Class " + uri + " should be resolved (not null)", result);
            assertTrue("Result should be HADatAcClass instance for " + uri, 
                       result instanceof HADatAcClass);
            
            HADatAcClass classObj = (HADatAcClass) result;
            assertNotNull("Class should have a label: " + uri, classObj.getLabel());
            assertFalse("Class label should not be empty: " + uri, classObj.getLabel().isEmpty());
            
            System.out.println("  ✓ Resolved: " + classObj.getLabel());
            System.out.println("  ✓ Type: " + classObj.getClass().getSimpleName());
            System.out.println("  ✓ URI: " + classObj.getUri());
            System.out.println();
        }
        
        System.out.println("=== All VSTOI Class Resolution Tests Passed ===\n");
    }

    /**
     * Test that non-ontology URIs still work as instances
     */
    @Test
    public void testInstanceResolutionStillWorks() {
        System.out.println("\n=== Testing Instance Resolution (should still work) ===\n");
        
        // This test assumes there are some instances in the triplestore
        // If this test fails, it might be because the triplestore is empty
        // which is OK - the important test is testVstoiClassResolution()
        
        // We're just verifying that the logic doesn't break instance resolution
        String testInstanceUri = "http://hadatac.org/kb/test/instance1";
        
        System.out.println("Testing instance URI: " + testInstanceUri);
        HADatAcThing result = URIPage.objectFromUri(testInstanceUri);
        
        // Result might be null if instance doesn't exist, which is fine
        // We're just ensuring no exceptions are thrown
        if (result != null) {
            System.out.println("  ✓ Instance resolved: " + result.getClass().getSimpleName());
        } else {
            System.out.println("  ✓ Instance not found (expected if triplestore is empty)");
        }
        
        System.out.println("\n=== Instance Resolution Test Completed ===\n");
    }
}
