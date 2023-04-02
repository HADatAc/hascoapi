package org.sirapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.sirapi.entity.pojo.Detector;
import org.sirapi.entity.pojo.ResponseOption;
import org.sirapi.utils.ApiUtil;
import org.sirapi.vocabularies.VSTOI;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.List;

import static org.sirapi.Constants.*;


public class ResponseOptionAPI extends Controller {

    private Result createResponseOptionResult(ResponseOption responseOption) {
        responseOption.save();
        return ok(ApiUtil.createResponse("ResponseOption <" + responseOption.getUri() + "> has been CREATED.", true));
    }

    public Result createResponseOptionsForTesting() {
        ResponseOption testResponseOption1 = ResponseOption.find(TEST_RESPONSE_OPTION1_URI);
        ResponseOption testResponseOption2 = ResponseOption.find(TEST_RESPONSE_OPTION2_URI);
        if (testResponseOption1 != null) {
            return ok(ApiUtil.createResponse("Test responseOption 1 already exists.", false));
        } else if (testResponseOption2 != null) {
            return ok(ApiUtil.createResponse("Test responseOption 2 already exists.", false));
        } else {
            testResponseOption1 = new ResponseOption();
            testResponseOption1.setUri(TEST_RESPONSE_OPTION1_URI);
            testResponseOption1.setLabel("Test ResponseOption 1");
            testResponseOption1.setTypeUri(VSTOI.RESPONSE_OPTION);
            testResponseOption1.setHascoTypeUri(VSTOI.RESPONSE_OPTION);
            testResponseOption1.setComment("This is a dummy ResponseOption 1 created to test the SIR API.");
            testResponseOption1.setHasContent("Never");
            testResponseOption1.setHasPriority("1");
            testResponseOption1.setOfExperience(TEST_EXPERIENCE_URI);
            testResponseOption1.setHasLanguage("en"); // ISO 639-1
            testResponseOption1.setHasSIRMaintainerEmail("me@example.com");
            testResponseOption1.save();
            testResponseOption2 = new ResponseOption();
            testResponseOption2.setUri(TEST_RESPONSE_OPTION2_URI);
            testResponseOption2.setLabel("Test ResponseOption 2");
            testResponseOption2.setTypeUri(VSTOI.RESPONSE_OPTION);
            testResponseOption2.setHascoTypeUri(VSTOI.RESPONSE_OPTION);
            testResponseOption2.setComment("This is a dummy ResponseOption 2 created to test the SIR API.");
            testResponseOption2.setOfExperience(TEST_EXPERIENCE_URI);
            testResponseOption2.setHasContent("Always");
            testResponseOption2.setHasPriority("2");
            testResponseOption2.setHasLanguage("en"); // ISO 639-1
            testResponseOption2.setHasSIRMaintainerEmail("me@example.com");
            testResponseOption2.save();
            return ok(ApiUtil.createResponse("Test ResponseOptions 1 and 2 have been CREATED.", true));
        }
    }

    public Result createResponseOption(String json) {
        if (json == null || json.equals("")) {
            return ok(ApiUtil.createResponse("No json content has been provided.", false));
        }
        //System.out.println("Value of json: [" + json + "]");
        ObjectMapper objectMapper = new ObjectMapper();
        ResponseOption newResponseOption;
        try {
            newResponseOption  = objectMapper.readValue(json, ResponseOption.class);
        } catch (Exception e) {
            return ok(ApiUtil.createResponse("Failed to parse json.", false));
        }
        return createResponseOptionResult(newResponseOption);
    }

    private Result deleteResponseOptionResult(ResponseOption responseOption) {
        String uri = responseOption.getUri();
        responseOption.delete();
        return ok(ApiUtil.createResponse("ResponseOption <" + uri + "> has been DELETED.", true));
    }

    public Result deleteResponseOptionsForTesting(){
        ResponseOption test1 = ResponseOption.find(TEST_RESPONSE_OPTION1_URI);
        ResponseOption test2 = ResponseOption.find(TEST_RESPONSE_OPTION2_URI);
        if (test1 == null) {
            return ok(ApiUtil.createResponse("There is no Test ResponseOption 1 to be deleted.", false));
        } else if (test2 == null) {
            return ok(ApiUtil.createResponse("There is no Test ResponseOption 2 to be deleted.", false));
        } else {
            test1.delete();
            test2.delete();
            return ok(ApiUtil.createResponse("Test ResponseOptions 1 and 2 have been DELETED.", true));
        }
    }

    public Result deleteResponseOption(String uri){
        if (uri == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No ResponseOption URI has been provided.", false));
        }
        ResponseOption responseOption = ResponseOption.find(uri);
        if (responseOption == null) {
            return ok(ApiUtil.createResponse("There is no ResponseOption with URI <" + uri + "> to be deleted.", false));
        } else {
            return deleteResponseOptionResult(responseOption);
        }
    }

    public Result getAllResponseOptions(){
        ObjectMapper mapper = new ObjectMapper();

        List<ResponseOption> results = ResponseOption.find();
        if (results == null) {
            return notFound(ApiUtil.createResponse("No ResponseOption has been found", false));
        } else {
            SimpleFilterProvider filterProvider = new SimpleFilterProvider();
            filterProvider.addFilter("responseOptionFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri", "hascoTypeLabel", "comment", "ofExperience", "hasContent", "hasPriority", "hasLanguage", "hasSIRMaintainerEmail"));
            mapper.setFilterProvider(filterProvider);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

    public Result getResponseOptionsByLanguage(String language){
        List<ResponseOption> results = ResponseOption.findByLanguage(language);
        return getResponseOptions(results);
    }

    public Result getResponseOptionsByKeyword(String keyword){
        List<ResponseOption> results = ResponseOption.findByKeyword(keyword);
        return getResponseOptions(results);
    }

    public Result getResponseOptionsByKeywordAndLanguage(String keyword, String language){
        List<ResponseOption> results = ResponseOption.findByKeywordAndLanguage(keyword, language);
        return getResponseOptions(results);
    }

    public Result getResponseOptionsByMaintainerEmail(String maintainerEmail){
        List<ResponseOption> results = ResponseOption.findByMaintainerEmail(maintainerEmail);
        return getResponseOptions(results);
    }

    public Result getResponseOptionsByExperience(String experienceUri){
        List<ResponseOption> results = ResponseOption.findByExperience(experienceUri);
        return getResponseOptions(results);
    }

    private Result getResponseOptions(List<ResponseOption> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No response option has been found", false));
        } else {
            ObjectMapper mapper = new ObjectMapper();
            SimpleFilterProvider filterProvider = new SimpleFilterProvider();
            filterProvider.addFilter("responseoptionFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "comment", "hasContent", "hasSerialNumber", "hasLanguage", "hasPriority", "hasSIRMaintainerEmail"));
            mapper.setFilterProvider(filterProvider);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

}
