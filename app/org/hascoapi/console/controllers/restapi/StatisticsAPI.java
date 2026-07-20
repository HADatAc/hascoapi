package org.hascoapi.console.controllers.restapi;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import play.mvc.Controller;
import play.mvc.Result;

/**
 * API Controller for statistics queries
 */
public class StatisticsAPI extends Controller {

    /**
     * Get count of instruments (subclasses of vstoi:Instrument + 1)
     * GET /hascoapi/api/statistics/instruments/count
     */
    public Result getInstrumentsCount() {
        int count = countInstruments();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = mapper.createObjectNode();
        result.put("total", count);
        return ok(ApiUtil.createResponse(result, true));
    }

    /**
     * Get count of clinical procedures (NCIT procedure classes)
     * GET /hascoapi/api/statistics/procedures/count
     */
    public Result getClinicalProceduresCount() {
        int count = countClinicalProcedures();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = mapper.createObjectNode();
        result.put("total", count);
        return ok(ApiUtil.createResponse(result, true));
    }

    /**
     * Get count of anatomical structures (UBERON classes)
     * GET /hascoapi/api/statistics/anatomy/count
     */
    public Result getAnatomicalStructuresCount() {
        int count = countAnatomicalStructures();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = mapper.createObjectNode();
        result.put("total", count);
        return ok(ApiUtil.createResponse(result, true));
    }

    /**
     * Get count of medical devices (NCIT device classes)
     * GET /hascoapi/api/statistics/medical-devices/count
     */
    public Result getMedicalDevicesCount() {
        int count = countMedicalDevices();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = mapper.createObjectNode();
        result.put("total", count);
        return ok(ApiUtil.createResponse(result, true));
    }

    /**
     * Count instruments: number of subclasses of vstoi:Instrument + 1
     * Fixed to avoid counting vstoi:Instrument multiple times across different graphs
     */
    private int countInstruments() {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT (COUNT(DISTINCT ?class) as ?count) WHERE { " +
                "   ?class rdfs:subClassOf+ vstoi:Instrument . " +  // Changed from * to + to exclude vstoi:Instrument itself
                "} ";

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        if (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            int subclasses = soln.getLiteral("count").getInt();
            // Add 1 to include vstoi:Instrument itself in the count
            return subclasses + 1;
        }
        return 0;
    }

    /**
     * Count clinical procedures from NCIT ontology
     * Counts classes that are subclasses of pmsr:MedicalSimulationProcessStem
     */
    private int countClinicalProcedures() {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT (COUNT(DISTINCT ?class) as ?count) WHERE { " +
                "   { " +
                "     ?class rdfs:subClassOf* <http://pmsr.net/ont/pmsr#MedicalSimulationProcessStem> . " +
                "   } UNION { " +
                "     ?class a owl:Class . " +
                "     ?class rdfs:subClassOf* <http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#C18020> . " +
                "   } " +
                "} ";

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        if (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            return soln.getLiteral("count").getInt();
        }
        return 0;
    }

    /**
     * Count anatomical structures from UBERON ontology
     * Counts all UBERON classes
     */
    private int countAnatomicalStructures() {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT (COUNT(DISTINCT ?class) as ?count) WHERE { " +
                "   ?class a owl:Class . " +
                "   FILTER(STRSTARTS(STR(?class), \"http://purl.obolibrary.org/obo/UBERON_\")) " +
                "} ";

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        if (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            return soln.getLiteral("count").getInt();
        }
        return 0;
    }

    /**
     * Count medical devices from NCIT ontology
     * Counts classes that are subclasses of NCIT_C97325 (Manufactured Object)
     */
    private int countMedicalDevices() {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT (COUNT(DISTINCT ?class) as ?count) WHERE { " +
                "   ?class rdfs:subClassOf* <http://purl.obolibrary.org/obo/NCIT_C97325> . " +
                "} ";

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        if (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            return soln.getLiteral("count").getInt();
        }
        return 0;
    }
}
