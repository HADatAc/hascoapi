package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import org.hascoapi.Constants;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.hascoapi.entity.pojo.Organization;
import org.hascoapi.entity.pojo.Person;
import org.hascoapi.entity.pojo.Place;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.HAScOMapper;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.vocabularies.HASCO;
import play.mvc.Controller;
import play.mvc.Result;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class OrganizationAPI extends Controller {

    public static Result getOrganizations(List<Organization> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No Organization has been found", false));
        } else {
            ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL,HASCO.STUDY);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

    public Result findSubOrganizations(String uri, int pageSize, int offset) {
        //System.out.println("OrganizationAPI: " + uri);
        List<Organization> results = Organization.findSubOrganizations(uri, pageSize, offset);
        return OrganizationAPI.getOrganizations(results);
       
    }

    public Result findTotalSubOrganizations(String uri) {
        int totalElements = Organization.findTotalSubOrganizations(uri);
        if (totalElements >= 0) {
            String totalElementsJSON = "{\"total\":" + totalElements + "}";
            return ok(ApiUtil.createResponse(totalElementsJSON, true));
        }     
        return ok(ApiUtil.createResponse("Query method findTotalSubOrganizations() failed to retrieve total number of element", false));   
    }

    public Result findAffiliations(String uri, int pageSize, int offset) {
        List<Person> results = Organization.findAffiliations(uri, pageSize, offset);
        return PersonAPI.getPeople(results);
       
    }

    public Result findTotalAffiliations(String uri) {
        int totalElements = Organization.findTotalAffiliations(uri);
        if (totalElements >= 0) {
            String totalElementsJSON = "{\"total\":" + totalElements + "}";
            return ok(ApiUtil.createResponse(totalElementsJSON, true));
        }     
        return ok(ApiUtil.createResponse("Query method findTotalAffiliations() failed to retrieve total number of element", false));   
    }

    /**
     * Get instrument model counts owned by a specific organization.
     *
     * GET /hascoapi/api/organization/instrumentmodels/:uri
     */
    public Result getInstrumentModelCounts(String uri) {
        String ownerUri = normalizeUri(uri);
        if (ownerUri == null || ownerUri.isEmpty()) {
            return ok(ApiUtil.createResponse("No valid organization URI has been provided", false));
        }

        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList()
                + "SELECT ?modelUri (SAMPLE(?label) AS ?modelLabel) (COUNT(DISTINCT ?ii) AS ?count) WHERE { "
                + "  { "
                + "    ?ii hasco:hascoType vstoi:InstrumentInstance . "
                + "    ?ii vstoi:hasOwner <" + ownerUri + "> . "
                + "  } "
                + "  UNION "
                + "  { "
                + "    ?platform hasco:partOf <" + ownerUri + "> . "
                + "    ?deployment hasco:hascoType vstoi:Deployment . "
                + "    ?deployment vstoi:hasPlatformInstance ?platform . "
                + "    ?deployment vstoi:hasInstrumentInstance ?ii . "
                + "    ?ii hasco:hascoType vstoi:InstrumentInstance . "
                + "  } "
                + "  OPTIONAL { ?ii vstoi:hasInstrument ?modelFromVstoi . } "
                + "  OPTIONAL { ?ii hasco:hasInstrument ?modelFromHasco . } "
                + "  OPTIONAL { "
                + "    ?ii rdf:type ?modelFromType . "
                + "    FILTER(?modelFromType NOT IN (vstoi:InstrumentInstance, owl:NamedIndividual)) "
                + "  } "
                + "  BIND(COALESCE(?modelFromVstoi, ?modelFromHasco, ?modelFromType) AS ?modelUri) "
                + "  FILTER(BOUND(?modelUri)) "
                + "  OPTIONAL { ?modelUri rdfs:label ?label . } "
                + "} GROUP BY ?modelUri ORDER BY DESC(?count)";

        ResultSetRewindable results = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY),
                queryString);

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode payload = mapper.createArrayNode();

        while (results.hasNext()) {
            QuerySolution soln = results.next();
            if (soln == null || soln.get("modelUri") == null || soln.get("count") == null) {
                continue;
            }

            String modelUri = soln.get("modelUri").toString();
            String modelLabel = "";
            if (soln.get("modelLabel") != null) {
                modelLabel = soln.get("modelLabel").toString();
            }
            if (modelLabel == null || modelLabel.isEmpty()) {
                modelLabel = labelFromUri(modelUri);
            }

            int count = soln.getLiteral("count").getInt();

            ObjectNode item = mapper.createObjectNode();
            item.put("modelUri", modelUri);
            item.put("modelName", modelLabel == null ? "" : modelLabel);
            item.put("count", count);
            payload.add(item);
        }

        return ok(ApiUtil.createResponse(payload, true));
    }

    private String normalizeUri(String uri) {
        if (uri == null) {
            return "";
        }
        String trimmed = uri.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        if (trimmed.startsWith("<") && trimmed.endsWith(">") && trimmed.length() > 2) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }

        // Defensive guard against malformed values being interpolated in SPARQL.
        if (trimmed.contains("<") || trimmed.contains(">") || trimmed.contains("\"")) {
            return "";
        }

        return trimmed;
    }

    private String labelFromUri(String uri) {
        if (uri == null || uri.isEmpty()) {
            return "";
        }

        String value = uri;
        int hash = value.lastIndexOf('#');
        int slash = value.lastIndexOf('/');
        int split = Math.max(hash, slash);
        if (split >= 0 && split + 1 < value.length()) {
            value = value.substring(split + 1);
        }

        value = value.replace('_', ' ').replace('-', ' ').trim();
        return value;
    }

}
