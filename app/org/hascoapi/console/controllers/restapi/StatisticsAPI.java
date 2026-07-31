package org.hascoapi.console.controllers.restapi;

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

    private static final String HASCO_WORKFLOW_STEM_ENTRY_POINT = "http://hadatac.org/ont/hasco/WorkflowStemEntryPoint";
    private static final String HASCO_ANATOMICAL_PART_ENTRY_POINT = "http://hadatac.org/ont/hasco/AnatomicalPartEntryPoint";
    private static final String HASCO_MEDICAL_DEVICE_ENTRY_POINT = "http://hadatac.org/ont/hasco/MedicalDeviceEntryPoint";
    private static final String VSTOI_INSTRUMENT_ROOT = "http://hadatac.org/ont/vstoi#Instrument";
    private static final String PMSR_PROCESS_STEM_CANONICAL = "https://pmsr.net/ont/MedicalSimulationProcessStem";
    private static final String PMSR_PROCESS_STEM_LEGACY = "https://pmsr.net/ont/MedicalSimulationProcessStem";
    private static final String UBERON_ANATOMICAL_ENTITY = "http://purl.obolibrary.org/obo/UBERON_0001062";
    private static final String NCIT_MANUFACTURED_OBJECT = "http://purl.obolibrary.org/obo/NCIT_C97325";

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
     * Get count of ontologies (named graphs)
     * GET /hascoapi/api/statistics/ontologies/count
     */
    public Result getOntologiesCount() {
        int count = countOntologies();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = mapper.createObjectNode();
        result.put("total", count);
        return ok(ApiUtil.createResponse(result, true));
    }

    /**
     * Get count of classes across all ontologies
     * GET /hascoapi/api/statistics/classes/count
     */
    public Result getClassesCount() {
        int count = countClasses();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = mapper.createObjectNode();
        result.put("total", count);
        return ok(ApiUtil.createResponse(result, true));
    }

    /**
     * Get count of instances across the entire knowledge graph
     * GET /hascoapi/api/statistics/instances/count
     */
    public Result getInstancesCount() {
        int count = countInstances();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = mapper.createObjectNode();
        result.put("total", count);
        return ok(ApiUtil.createResponse(result, true));
    }

    /**
     * Count simulator models as subclasses of vstoi:Instrument.
     */
    private int countInstruments() {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT (COUNT(DISTINCT ?class) as ?count) WHERE { " +
                "   ?class <http://www.w3.org/2000/01/rdf-schema#subClassOf>* <" + VSTOI_INSTRUMENT_ROOT + "> . " +
                "   FILTER(isIRI(?class)) " +
                "} ";

        return runCountQuery(queryString);
    }

    /**
     * Count clinical procedures as descendants of WorkflowStem entry-point roots.
     */
    private int countClinicalProcedures() {
        return countClassesFromEntryPointOrFallback(
                HASCO_WORKFLOW_STEM_ENTRY_POINT,
                PMSR_PROCESS_STEM_CANONICAL,
                PMSR_PROCESS_STEM_LEGACY
        );
    }

    /**
     * Count anatomical structures as descendants of AnatomicalPart entry-point roots.
     */
    private int countAnatomicalStructures() {
        return countClassesFromEntryPointOrFallback(
                HASCO_ANATOMICAL_PART_ENTRY_POINT,
                UBERON_ANATOMICAL_ENTITY
        );
    }

    /**
     * Count medical devices as descendants of MedicalDevice entry-point roots.
     */
    private int countMedicalDevices() {
        return countClassesFromEntryPointOrFallback(
                HASCO_MEDICAL_DEVICE_ENTRY_POINT,
                NCIT_MANUFACTURED_OBJECT
        );
    }

    /**
     * Count classes reachable through rdfs:subClassOf* from mapped entrypoint roots,
     * with explicit fallback roots when mappings are absent.
     */
    private int countClassesFromEntryPointOrFallback(String entryPointUri, String... fallbackRoots) {
        if ((entryPointUri == null || entryPointUri.trim().isEmpty())
                && (fallbackRoots == null || fallbackRoots.length == 0)) {
            return 0;
        }

        String ep = entryPointUri == null ? "" : entryPointUri.replace("\"", "\\\"");
        StringBuilder values = new StringBuilder();
        if (fallbackRoots != null) {
            for (String root : fallbackRoots) {
                if (root != null && !root.trim().isEmpty()) {
                    values.append("<").append(root.replace("\"", "\\\"")).append("> ");
                }
            }
        }

        String rootPattern =
                "{ ?root <http://www.w3.org/2000/01/rdf-schema#subClassOf> <" + ep + "> . }";

        if (values.length() > 0) {
            rootPattern += " UNION { VALUES ?root { " + values.toString() + " } }";
        }

        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT (COUNT(DISTINCT ?class) as ?count) WHERE { " +
                "   " + rootPattern + " " +
                "   ?class <http://www.w3.org/2000/01/rdf-schema#subClassOf>* ?root . " +
                "   FILTER(isIRI(?class)) " +
                "} ";

        return runCountQuery(queryString);
    }

    private int runCountQuery(String queryString) {
        if (queryString == null || queryString.trim().isEmpty()) {
            return 0;
        }

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        if (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            return soln.getLiteral("count").getInt();
        }
        return 0;
    }

    /**
     * Count ontologies using existing NameSpace method
     */
    private int countOntologies() {
        return org.hascoapi.entity.pojo.NameSpace.getNumberOntologies();
    }

    /**
     * Count classes using existing HADatAcClass method
     */
    private int countClasses() {
        return org.hascoapi.entity.pojo.HADatAcClass.getNumberClasses();
    }

    /**
     * Count instances using existing HADatAcThing method
     */
    private int countInstances() {
        return org.hascoapi.entity.pojo.HADatAcThing.getNumberInstances();
    }
}
