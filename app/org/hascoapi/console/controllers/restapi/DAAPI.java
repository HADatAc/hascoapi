package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.DA;
import org.hascoapi.entity.pojo.Stream;
import org.hascoapi.entity.pojo.StreamTopic;
import org.hascoapi.entity.pojo.Study;
import org.hascoapi.transform.Renderings;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.HAScOMapper;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.vocabularies.SIO;
import org.hascoapi.vocabularies.HASCO;
import play.mvc.Controller;
import play.mvc.Result;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class DAAPI extends Controller {

    private Result createDAResult(DA da) {
        da.save();
        return ok(ApiUtil.createResponse("DA <" + da.getUri() + "> has been CREATED.", true));
    }

    public Result createDA(String json) {
        if (json == null || json.equals("")) {
            return ok(ApiUtil.createResponse("No json content has been provided.", false));
        }
        //System.out.println("(UnitAPI) Value of json in createUnit: [" + json + "]");
        ObjectMapper objectMapper = new ObjectMapper();
        DA newDA;
        try {
            //convert json string to Unit unitance
            newDA  = objectMapper.readValue(json, DA.class);
        } catch (Exception e) {
            //System.out.println("(UnitAPI) Failed to parse json for [" + json + "]");
            return ok(ApiUtil.createResponse("Failed to parse json.", false));
        }
        return createDAResult(newDA);
    }

    public static Result getDAs(List<DA> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No DA has been found", false));
        } else {
            //for (DD dd: results) {
            //    System.out.println(dd.getLabel() + "  [" + dd.getHasDataFile() + "]");
            //}
            ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL,HASCO.DATA_ACQUISITION);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

    public Result findDAsByStudy(String uri, int pagesize, int offset) {
        if (uri == null || uri.isEmpty()) {
            return ok(ApiUtil.createResponse("DAAPI: No study uri has been provided to retrieve data acquisitions", false));
        }
        Study study = Study.find(uri);
        if (study == null) {
            return ok(ApiUtil.createResponse("DAAPI: No study has been retrieved with URI=[" + uri + "]", false));
        }
        List<DA> das = DA.findByStudy(study, pagesize, offset);
        return DAAPI.getDAs(das);
    }

    public Result findTotalDAsByStudy(String uri) {
        if (uri == null || uri.isEmpty()) {
            return ok(ApiUtil.createResponse("DAAPI: No study uri has been provided to retrieve data acquisitions", false));
        }
        Study study = Study.find(uri);
        if (study == null) {
            return ok(ApiUtil.createResponse("DAAPI: No study has been retrieved with URI=[" + uri + "]", false));
        }
        int totalElements = DA.findTotalByStudy(study);
        if (totalElements >= 0) {
            String totalElementsJSON = "{\"total\":" + totalElements + "}";
            return ok(ApiUtil.createResponse(totalElementsJSON, true));
        }     
        return ok(ApiUtil.createResponse("DAAPI: Query method findTotalDAsByStudy() failed to retrieve total number of element", false));   
    }

    public Result findDAsByStream(String uri, int pagesize, int offset) {
        if (uri == null || uri.isEmpty()) {
            return ok(ApiUtil.createResponse("DAAPI: No stream uri has been provided to retrieve data acquisitions", false));
        }
        Stream stream = Stream.find(uri);
        if (stream == null) {
            return ok(ApiUtil.createResponse("DAAPI: No stream has been retrieved with URI=[" + uri + "]", false));
        }
        List<DA> das = DA.findByStream(stream, pagesize, offset);
        return DAAPI.getDAs(das);
    }

    public Result findTotalDAsByStream(String uri) {
        if (uri == null || uri.isEmpty()) {
            return ok(ApiUtil.createResponse("DAAPI: No stream uri has been provided to retrieve data acquisitions", false));
        }
        Stream stream = Stream.find(uri);
        if (stream == null) {
            return ok(ApiUtil.createResponse("DAAPI: No stream has been retrieved with URI=[" + uri + "]", false));
        }
        int totalElements = DA.findTotalByStream(stream);
        if (totalElements >= 0) {
            String totalElementsJSON = "{\"total\":" + totalElements + "}";
            return ok(ApiUtil.createResponse(totalElementsJSON, true));
        }     
        return ok(ApiUtil.createResponse("DAAPI: Query method findTotalDAsByStream() failed to retrieve total number of element", false));   
    }

    public Result findDAsByStreamTopic(String uri, int pagesize, int offset) {
        if (uri == null || uri.isEmpty()) {
            return ok(ApiUtil.createResponse("DAAPI: No stream topic uri has been provided to retrieve data acquisitions", false));
        }
        StreamTopic streamTopic = StreamTopic.find(uri);
        if (streamTopic == null) {
            return ok(ApiUtil.createResponse("DAAPI: No stream topic has been retrieved with URI=[" + uri + "]", false));
        }
        List<DA> das = DA.findByStreamTopic(streamTopic, pagesize, offset);
        return DAAPI.getDAs(das);
    }

    public Result findTotalDAsByStreamTopic(String uri) {
        if (uri == null || uri.isEmpty()) {
            return ok(ApiUtil.createResponse("DAAPI: No stream topic uri has been provided to retrieve data acquisitions", false));
        }
        StreamTopic streamTopic = StreamTopic.find(uri);
        if (streamTopic == null) {
            return ok(ApiUtil.createResponse("DAAPI: No stream topic has been retrieved with URI=[" + uri + "]", false));
        }
        int totalElements = DA.findTotalByStreamTopic(streamTopic);
        if (totalElements >= 0) {
            String totalElementsJSON = "{\"total\":" + totalElements + "}";
            return ok(ApiUtil.createResponse(totalElementsJSON, true));
        }     
        return ok(ApiUtil.createResponse("DAAPI: Query method findTotalDAsByStreamTopic() failed to retrieve total number of element", false));   
    }

    /**
     * Get all DA-SOC DataFiles from the triplestore
     * Queries for DataFiles with filenames starting with "DA-SOC-"
     * Only returns files with PROCESSED or WORKING status (excludes UNPROCESSED/deleted files)
     */
    public Result getDaSocFiles() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT DISTINCT ?uri ?filename ?fileStatus ?daUri \n" +
                "WHERE { \n" +
                "  GRAPH ?uri { \n" +
                "    ?uri hasco:hasFilename ?filename . \n" +
                "    FILTER(STRSTARTS(UCASE(?filename), \"DA-SOC-\")) \n" +
                "    ?uri hasco:hasFileStatus ?fileStatus . \n" +
                "    FILTER(?fileStatus IN (\"PROCESSED\", \"WORKING\")) \n" +
                "  } \n" +
                "  OPTIONAL { \n" +
                "    GRAPH ?g { \n" +
                "      ?daUri hasco:hasDataFile ?uri . \n" +
                "    } \n" +
                "  } \n" +
                "} \n" +
                "ORDER BY ?filename";

            ResultSetRewindable results = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), 
                queryString);

            ArrayNode dataFiles = mapper.createArrayNode();
            while (results.hasNext()) {
                QuerySolution soln = results.next();
                if (soln != null && soln.getResource("uri") != null) {
                    ObjectNode dataFile = mapper.createObjectNode();
                    dataFile.put("uri", soln.getResource("uri").getURI());
                    
                    if (soln.getLiteral("filename") != null) {
                        dataFile.put("filename", soln.getLiteral("filename").getString());
                    }
                    
                    if (soln.getLiteral("fileStatus") != null) {
                        dataFile.put("fileStatus", soln.getLiteral("fileStatus").getString());
                    }
                    
                    if (soln.getResource("daUri") != null) {
                        dataFile.put("daUri", soln.getResource("daUri").getURI());
                    }
                    
                    dataFiles.add(dataFile);
                }
            }

            return ok(ApiUtil.createResponse(dataFiles, true));
        } catch (Exception e) {
            return badRequest(ApiUtil.createResponse("Error retrieving DA-SOC files: " + e.getMessage(), false));
        }
    }

    /**
     * Check if a DASOC graph exists for a given DA URI
     * @param daUri The Data Acquisition URI (the DASOC graph will be {daUri}-dasoc)
     */
    public Result checkDaSocGraph(String daUri) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            if (daUri == null || daUri.isEmpty()) {
                return ok(ApiUtil.createResponse("No DA URI provided", false));
            }

            // DA URIs should already be full URIs from ingestion
            String dasocGraph = daUri + "-dasoc";
            // Use SELECT with LIMIT 1 instead of ASK to check graph existence
            String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT ?s WHERE { \n" +
                "  GRAPH <" + dasocGraph + "> { \n" +
                "    ?s ?p ?o \n" +
                "  } \n" +
                "} LIMIT 1";

            ResultSetRewindable results = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), 
                queryString);

            // Check if there are any results (graph exists and has data)
            boolean exists = results.hasNext();
            
            ObjectNode response = mapper.createObjectNode();
            response.put("daUri", daUri);
            response.put("dasocGraph", dasocGraph);
            response.put("exists", exists);

            return ok(ApiUtil.createResponse(response, true));
        } catch (Exception e) {
            return badRequest(ApiUtil.createResponse("Error checking DASOC graph: " + e.getMessage(), false));
        }
    }

    /**
     * Count additional properties in a DASOC graph (properties beyond the base 5)
     * @param daUri The Data Acquisition URI
     */
    public Result countDaSocProperties(String daUri) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            if (daUri == null || daUri.isEmpty()) {
                return ok(ApiUtil.createResponse("No DA URI provided", false));
            }

            // DA URIs should already be full URIs from ingestion
            String dasocGraph = daUri + "-dasoc";
            // Count distinct predicates in the DASOC graph, excluding ONLY the 5 base SOC properties and metadata
            // Base SOC properties (from SOC sheet): originalID, rdf:type, scopeID, timeScopeID, spaceScopeID
            // rdfs:label and rdfs:comment are DA-SOC contributions and should be counted
            String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT (COUNT(DISTINCT ?p) as ?tot) WHERE { \n" +
                "  GRAPH <" + dasocGraph + "> { \n" +
                "    ?s ?p ?o \n" +
                "    FILTER (?p NOT IN (hasco:originalID, rdf:type, hasco:scopeID, \n" +
                "                       hasco:timeScopeID, hasco:spaceScopeID, \n" +
                "                       hasco:isMemberOf, hasco:hasTimestamp)) \n" +
                "  } \n" +
                "}";

            ResultSetRewindable results = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), 
                queryString);

            int count = 0;
            if (results.hasNext()) {
                QuerySolution soln = results.next();
                if (soln.getLiteral("tot") != null) {
                    count = soln.getLiteral("tot").getInt();
                }
            }
            
            ObjectNode response = mapper.createObjectNode();
            response.put("daUri", daUri);
            response.put("dasocGraph", dasocGraph);
            response.put("additionalProperties", count);

            return ok(ApiUtil.createResponse(response, true));
        } catch (Exception e) {
            return badRequest(ApiUtil.createResponse("Error counting DASOC properties: " + e.getMessage(), false));
        }
    }

}
