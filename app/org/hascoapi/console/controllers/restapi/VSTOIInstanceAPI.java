package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.ComponentInstance;
import org.hascoapi.entity.pojo.INS;
import org.hascoapi.entity.pojo.InstrumentInstance;
import org.hascoapi.entity.pojo.PlatformInstance;
import org.hascoapi.entity.pojo.StudyObject;
import org.hascoapi.transform.Renderings;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.HAScOMapper;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.VSTOI;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VSTOIInstanceAPI extends Controller {

    public static Result getInstrumentInstances(List<InstrumentInstance> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No InstrumentInstance has been found", false));
        } else {
            ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL,VSTOI.INSTRUMENT_INSTANCE);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

    public static Result getComponentInstances(List<ComponentInstance> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No ComponentInstance has been found", false));
        } else {
            ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL,VSTOI.COMPONENT_INSTANCE);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

    public static Result getPlatformInstances(List<PlatformInstance> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No PlatformInstance has been found", false));
        } else {
            ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL,VSTOI.PLATFORM_INSTANCE);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

    public Result findInstrumentInstancesByAnatomy(String uberonUri, Http.Request request) {
        if (uberonUri == null || uberonUri.trim().isEmpty()) {
            return ok(ApiUtil.createResponse("No UBERON URI has been provided", false));
        }

        String organizationUri = request.getQueryString("organizationUri");
        List<InstrumentInstance> results = InstrumentInstance.findByAnatomy(uberonUri, organizationUri);
        return getInstrumentInstances(results);
    }

    public Result findTotalInstrumentInstancesByAnatomy(String uberonUri, Http.Request request) {
        if (uberonUri == null || uberonUri.trim().isEmpty()) {
            return ok(ApiUtil.createResponse("No UBERON URI has been provided", false));
        }

        String organizationUri = request.getQueryString("organizationUri");
        int totalElements = InstrumentInstance.findTotalByAnatomy(uberonUri, organizationUri);
        String totalElementsJSON = "{\"total\":" + totalElements + "}";
        return ok(ApiUtil.createResponse(totalElementsJSON, true));
    }

    public Result findPlatformInstancesByPlatformWithPage(String platformUri, int pagesize, int offset) {
        if (platformUri == null || platformUri.isEmpty()) {
            return ok(ApiUtil.createResponse("No platform uri has been provided", false));
        }
        System.out.println(platformUri);
        List<PlatformInstance> results = PlatformInstance.findByPlaformWithPage(platformUri, pagesize, offset);
        return this.getPlatformInstances(results);
    }

    public Result findTotalPlatformInstancesByPlatform(String platformUri){
        if (platformUri == null || platformUri.isEmpty()) {
            return ok(ApiUtil.createResponse("No platform uri has been provided", false));
        }
        int totalElements = totalElements = PlatformInstance.findTotalByPlatform(platformUri);
        if (totalElements >= 0) {
            String totalElementsJSON = "{\"total\":" + totalElements + "}";
            return ok(ApiUtil.createResponse(totalElementsJSON, true));
        }
        return ok(ApiUtil.createResponse("query method findTotalPlatformInstancesByPlatform() failed to retrieve total number of element", false));
    }

    /**
     * Retrieve all platform instances and group them by organization URI (hasco:partOf).
     */
    public Result findPlatformInstancesGroupedByOrganization() {
        List<PlatformInstance> allInstances = PlatformInstance.findAll();
        if (allInstances == null) {
            allInstances = new ArrayList<PlatformInstance>();
        }

        Map<String, List<PlatformInstance>> groupedByOrganization = new LinkedHashMap<String, List<PlatformInstance>>();
        for (PlatformInstance platformInstance : allInstances) {
            if (platformInstance == null) {
                continue;
            }

            String organizationUri = platformInstance.getPartOf();
            if (organizationUri == null || organizationUri.trim().isEmpty()) {
                organizationUri = "_unassigned";
            }

            List<PlatformInstance> bucket = groupedByOrganization.get(organizationUri);
            if (bucket == null) {
                bucket = new ArrayList<PlatformInstance>();
                groupedByOrganization.put(organizationUri, bucket);
            }
            bucket.add(platformInstance);
        }

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("total", allInstances.size());
        payload.put("organizations", groupedByOrganization.size());
        payload.put("groups", groupedByOrganization);

        ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL, VSTOI.PLATFORM_INSTANCE);
        JsonNode jsonObject = mapper.convertValue(payload, JsonNode.class);
        return ok(ApiUtil.createResponse(jsonObject, true));
    }

    /**
     * List instrument instances with pagination.
     * Optional query params:
     * - organizationUri (filters by vstoi:hasOwner)
     * - hascoType (defaults to DP2 v3 fixed type behavior when omitted)
     */
    public Result findInstrumentInstancesWithPage(int pageSize, int offset, Http.Request request) {
        if (pageSize < 0 || offset < 0) {
            return ok(ApiUtil.createResponse("Page size and offset must be non-negative", false));
        }

        String organizationUri = request.getQueryString("organizationUri");
        String hascoType = request.getQueryString("hascoType");
        List<InstrumentInstance> results = InstrumentInstance.findWithPageByOwnerAndHascoType(organizationUri, pageSize, offset, hascoType);
        return getInstrumentInstances(results);
    }

    /**
     * Get total instrument instances.
     * Optional query params:
     * - organizationUri (filters by vstoi:hasOwner)
     * - hascoType (defaults to DP2 v3 fixed type behavior when omitted)
     */
    public Result findTotalInstrumentInstances(Http.Request request) {
        String organizationUri = request.getQueryString("organizationUri");
        String hascoType = request.getQueryString("hascoType");
        int totalElements = InstrumentInstance.findTotalByOwnerAndHascoType(organizationUri, hascoType);
        String totalElementsJSON = "{\"total\":" + totalElements + "}";
        return ok(ApiUtil.createResponse(totalElementsJSON, true));
    }

    /**
     * List component instances with pagination.
     * Optional query params:
     * - organizationUri (filters by owner through deployment context)
     * - hascoType (defaults to DP2 v3 fixed type behavior when omitted)
     */
    public Result findComponentInstancesWithPage(int pageSize, int offset, Http.Request request) {
        if (pageSize < 0 || offset < 0) {
            return ok(ApiUtil.createResponse("Page size and offset must be non-negative", false));
        }

        String organizationUri = request.getQueryString("organizationUri");
        String hascoType = request.getQueryString("hascoType");
        List<ComponentInstance> results = ComponentInstance.findWithPageByOwnerAndHascoType(organizationUri, pageSize, offset, hascoType);
        return getComponentInstances(results);
    }

    /**
     * Get total component instances.
     * Optional query params:
     * - organizationUri (filters by owner through deployment context)
     * - hascoType (defaults to DP2 v3 fixed type behavior when omitted)
     */
    public Result findTotalComponentInstances(Http.Request request) {
        String organizationUri = request.getQueryString("organizationUri");
        String hascoType = request.getQueryString("hascoType");
        int totalElements = ComponentInstance.findTotalByOwnerAndHascoType(organizationUri, hascoType);
        String totalElementsJSON = "{\"total\":" + totalElements + "}";
        return ok(ApiUtil.createResponse(totalElementsJSON, true));
    }

}
