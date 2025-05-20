package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.Deployment;
import org.hascoapi.entity.pojo.Stream;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.HAScOMapper;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.VSTOI;
import play.mvc.Controller;
import play.mvc.Result;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class StreamAPI extends Controller {

    public static Result getStreams(List<Stream> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No Stream has been found", false));
        } else {
            ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL,HASCO.STREAM);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

        public Result findCanUpdateByDeploymentWithPages(String state, String userEmail, String deploymentUri, int pageSize, int offset) {
        if (state == null || state.isEmpty()) {
            return ok(ApiUtil.createResponse("No state has been provided to retrieve streams", false));
        }
        if (userEmail == null || userEmail.isEmpty()) {
            return ok(ApiUtil.createResponse("No user email has been provided to retrieve streams", false));
        }
        if (deploymentUri == null || deploymentUri.isEmpty()) {
            return ok(ApiUtil.createResponse("No deploymnent uri has been provided to retrieve streams", false));
        }
        if (state == null) {
            state = "";
        } else {
            if (!state.equals(HASCO.DRAFT) && 
                !state.equals(HASCO.ACTIVE) &&
                !state.equals(HASCO.CLOSED)) { 
                    return ok(ApiUtil.createResponse("No valid state has been provided to retrieve streams", false));
                }
        }
        return StreamAPI.getStreams(Stream.findCanUpdateByDeploymentWithPages(state,userEmail,deploymentUri,pageSize,offset));
    }

    public Result findTotalCanUpdateByDeploymentWithPages(String state, String userEmail, String deploymentUri) {
        if (state == null || state.isEmpty()) {
            return ok(ApiUtil.createResponse("No state has been provided to retrieve streams", false));
        }
        if (userEmail == null || userEmail.isEmpty()) {
            return ok(ApiUtil.createResponse("No user email has been provided to retrieve streams", false));
        }
        if (deploymentUri == null || deploymentUri.isEmpty()) {
            return ok(ApiUtil.createResponse("No deploymnent uri has been provided to retrieve streams", false));
        }
        if (state == null) {
            state = "";
        } else {
            if (!state.equals(HASCO.DRAFT) && 
                !state.equals(HASCO.ACTIVE) &&
                !state.equals(HASCO.CLOSED)) { 
                    return ok(ApiUtil.createResponse("No valid state has been provided to retrieve streams", false));
                }
        }
        int totalElements = Stream.findTotalCanUpdateByDeploymentWithPages(state,userEmail,deploymentUri);
        if (totalElements >= 0) {
            String totalElementsJSON = "{\"total\":" + totalElements + "}";
            return ok(ApiUtil.createResponse(totalElementsJSON, true));
        }     
        return ok(ApiUtil.createResponse("Query method findTotalCanUpdateWithPages() failed to retrieve total number of element", false));   
    }


}
