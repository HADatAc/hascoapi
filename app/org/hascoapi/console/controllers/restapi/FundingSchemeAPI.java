package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.FundingScheme;
import org.hascoapi.entity.pojo.Organization;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.HAScOMapper;
import org.hascoapi.vocabularies.SCHEMA;
import play.mvc.Controller;
import play.mvc.Result;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class FundingSchemeAPI extends Controller {

    public static Result getFundingSchemes(List<FundingScheme> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No Funding Scheme has been found", false));
        } else {
            ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL,SCHEMA.FUNDING_SCHEME);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

    public Result findFunds(String uri, int pageSize, int offset) {
        List<FundingScheme> results = FundingScheme.findFunds(uri, pageSize, offset);
        return FundingSchemeAPI.getFundingSchemes(results);
       
    }

    public Result findTotalFunds(String uri) {
        int totalElements = FundingScheme.findTotalFunds(uri);
        if (totalElements >= 0) {
            String totalElementsJSON = "{\"total\":" + totalElements + "}";
            return ok(ApiUtil.createResponse(totalElementsJSON, true));
        }     
        return ok(ApiUtil.createResponse("Query method findTotalFunds() failed to retrieve total number of element", false));   
    }




}
