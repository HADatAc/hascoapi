package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.Deployment;
import org.hascoapi.entity.pojo.Organization;
import org.hascoapi.entity.pojo.PlatformInstance;
import org.hascoapi.entity.pojo.PostalAddress;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.HAScOMapper;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.VSTOI;
import play.mvc.Controller;
import play.mvc.Result;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class DeploymentAPI extends Controller {

    public static Result getDeployments(List<Deployment> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No Deployment has been found", false));
        } else {
            ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL,VSTOI.DEPLOYMENT);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

    public Result findCanUpdateWithPages(String state, String userEmail, int pageSize, int offset) {
        if (state == null || state.isEmpty()) {
            return ok(ApiUtil.createResponse("No state has been provided to retrieve deployments", false));
        }
        if (userEmail == null || userEmail.isEmpty()) {
            return ok(ApiUtil.createResponse("No user email has been provided to retrieve deployments", false));
        }
        if (state == null) {
            state = "";
        } else {
            if (!state.equals(HASCO.DRAFT) && 
                !state.equals(HASCO.ACTIVE) &&
                !state.equals(HASCO.CLOSED)) { 
                    return ok(ApiUtil.createResponse("No valid state has been provided to retrieve deployments", false));
                }
        }
        return DeploymentAPI.getDeployments(Deployment.findCanUpdateWithPages(state,userEmail,pageSize,offset));
    }

    public Result findTotalCanUpdateWithPages(String state, String userEmail) {
        if (state == null || state.isEmpty()) {
            return ok(ApiUtil.createResponse("No state has been provided to retrieve deployments", false));
        }
        if (userEmail == null || userEmail.isEmpty()) {
            return ok(ApiUtil.createResponse("No user email has been provided to retrieve deployments", false));
        }
        if (state == null) {
            state = "";
        } else {
            if (!state.equals(HASCO.DRAFT) && 
                !state.equals(HASCO.ACTIVE) &&
                !state.equals(HASCO.CLOSED)) { 
                    return ok(ApiUtil.createResponse("No valid state has been provided to retrieve deployments", false));
                }
        }
        int totalElements = Deployment.findTotalCanUpdateWithPages(state,userEmail);
        if (totalElements >= 0) {
            String totalElementsJSON = "{\"total\":" + totalElements + "}";
            return ok(ApiUtil.createResponse(totalElementsJSON, true));
        }     
        return ok(ApiUtil.createResponse("Query method findTotalCanUpdateWithPages() failed to retrieve total number of element", false));   
    }

    public Result findDeploymentsByPlatformInstanceWithPage(String platforminstanceUri, int pagesize, int offset) {
        System.out.println("DeploymentAPI.findDeploymentsByPlatformInstanceWithPage() with uri=[" + platforminstanceUri + "]");
        if (platforminstanceUri == null || platforminstanceUri.isEmpty()) {
            return ok(ApiUtil.createResponse("No platform instance uri has been provided", false));
        }
        System.out.println(platforminstanceUri);
        List<Deployment> results = Deployment.findByPlaformInstanceWithPage(platforminstanceUri, pagesize, offset);
        return this.getDeployments(results);
    }

    public Result findTotalDeploymentsByPlatformInstance(String platforminstanceUri){
        if (platforminstanceUri == null || platforminstanceUri.isEmpty()) {
            return ok(ApiUtil.createResponse("No platform instance uri has been provided", false));
        }
        int totalElements = totalElements = Deployment.findTotalByPlatformInstance(platforminstanceUri);
        if (totalElements >= 0) {
            String totalElementsJSON = "{\"total\":" + totalElements + "}";
            return ok(ApiUtil.createResponse(totalElementsJSON, true));
        }
        return ok(ApiUtil.createResponse("query method findTotalDeploymentsByPlatforminstance() failed to retrieve total number of element", false));
    }
}
