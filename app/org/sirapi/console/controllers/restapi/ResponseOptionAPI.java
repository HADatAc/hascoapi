package org.sirapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.sirapi.entity.pojo.*;
import org.sirapi.utils.ApiUtil;
import org.sirapi.vocabularies.VSTOI;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.List;

import static org.sirapi.Constants.*;


public class ResponseOptionAPI extends Controller {

    private Result createResponseOptionResult(ResponseOption responseOption) {
        responseOption.save();
        //System.out.println("ResponseOption <" + responseOption.getUri() + "> has been CREATED.");
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
            testResponseOption1.setHasLanguage("en"); // ISO 639-1
            testResponseOption1.setHasVersion("1");
            testResponseOption1.setHasSIRMaintainerEmail("me@example.com");
            testResponseOption1.save();
            testResponseOption2 = new ResponseOption();
            testResponseOption2.setUri(TEST_RESPONSE_OPTION2_URI);
            testResponseOption2.setLabel("Test ResponseOption 2");
            testResponseOption2.setTypeUri(VSTOI.RESPONSE_OPTION);
            testResponseOption2.setHascoTypeUri(VSTOI.RESPONSE_OPTION);
            testResponseOption2.setComment("This is a dummy ResponseOption 2 created to test the SIR API.");
            testResponseOption2.setHasContent("Always");
            testResponseOption2.setHasLanguage("en"); // ISO 639-1
            testResponseOption2.setHasVersion("1");
            testResponseOption2.setHasSIRMaintainerEmail("me@example.com");
            testResponseOption2.save();
            return ok(ApiUtil.createResponse("Test ResponseOptions 1 and 2 have been CREATED.", true));
        }
    }

    public Result createResponseOption(String json) {
        if (json == null || json.equals("")) {
            return ok(ApiUtil.createResponse("No json content has been provided.", false));
        }
        //System.out.println("(createResponseOption) Value of json: [" + json + "]");
        ObjectMapper objectMapper = new ObjectMapper();
        ResponseOption newResponseOption;
        try {
            newResponseOption  = objectMapper.readValue(json, ResponseOption.class);
        } catch (Exception e) {
            System.out.println("(createResponseOption) failed to parse JSON");
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
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri", "hascoTypeLabel", "comment",
                            "hasContent", "hasLanguage", "hasVersion", "hasSIRMaintainerEmail"));
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

    public Result getResponseOptionsByMaintainerEmail(String maintainerEmail){
        List<ResponseOption> results = ResponseOption.findByMaintainerEmail(maintainerEmail);
        return getResponseOptions(results);
    }

    public static Result getResponseOptions(List<ResponseOption> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No response option has been found", false));
        } else {
            ObjectMapper mapper = new ObjectMapper();
            SimpleFilterProvider filterProvider = new SimpleFilterProvider();
            filterProvider.addFilter("responseOptionFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "comment", "hasContent", "hasLanguage", "hasVersion", "hasSIRMaintainerEmail"));
            mapper.setFilterProvider(filterProvider);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

    public Result attach(String uri, String codebookSlotUri){
        if (uri == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No response option URI has been provided.", false));
        }
        ResponseOption responseOption = ResponseOption.find(uri);
        if (responseOption == null) {
            return ok(ApiUtil.createResponse("There is no detector with URI <" + uri + "> to be attached.", false));
        }
        if (codebookSlotUri == null || codebookSlotUri.equals("")) {
            return ok(ApiUtil.createResponse("No codebookSlot URI has been provided.", false));
        }
        CodebookSlot codebookSlot = CodebookSlot.find(codebookSlotUri);
        if (codebookSlot == null) {
            return ok(ApiUtil.createResponse("There is no CodebookSlot with uri <" + codebookSlotUri + ">.", false));
        }
        if (ResponseOption.attach(codebookSlotUri, uri)) {
            return ok(ApiUtil.createResponse("ResponseOption <" + uri + "> successfully attached to CodebookSlot <" + codebookSlotUri + ">.", true));
        }
        return ok(ApiUtil.createResponse("ResponseOption <" + uri + "> failed to associate with CodebookSlot  <" + codebookSlotUri + ">.", false));
    }

    public Result attachForTesting(){
        Experience testExp = Experience.find(TEST_EXPERIENCE_URI);
        if (testExp == null) {
            return ok(ApiUtil.createResponse("create test experience before trying to attach response options.", false));
        }
        if (testExp.getCodebookSlots() == null) {
            return ok(ApiUtil.createResponse("Create codebook slots for test experiment before trying to attach response options.", false));
        }
        CodebookSlot slot1 = CodebookSlot.find(TEST_CODEBOOK_SLOT1_URI);
        CodebookSlot slot2 = CodebookSlot.find(TEST_CODEBOOK_SLOT2_URI);
        if (slot1 == null || slot2 == null) {
            return ok(ApiUtil.createResponse("Either Test Codebook Slot 1 or 2 is unavailable to allow the attachment of ResponseOptions 1 and 2 to test experience.", false));
        }
        if (slot1.getHasResponseOption() != null) {
            return ok(ApiUtil.createResponse("Test Codebook Slot 1 already has an attached Response Option", false));
        }
        if (slot2.getHasResponseOption() != null) {
            return ok(ApiUtil.createResponse("Test Codebook Slot 2 already has an attached Response Option", false));
        }
        ResponseOption test1 = ResponseOption.find(TEST_RESPONSE_OPTION1_URI);
        ResponseOption test2 = ResponseOption.find(TEST_RESPONSE_OPTION2_URI);
        if (test1 == null || test2 == null) {
            return ok(ApiUtil.createResponse("Either Test Response Option 1 or 2 is unavailable to be attached to test experience.", false));
        } else {
            boolean done = ResponseOption.attach(TEST_CODEBOOK_SLOT1_URI, TEST_RESPONSE_OPTION1_URI);
            if (!done) {
                return ok(ApiUtil.createResponse("The attachment of Test Response Option 1 to Test CodebookSlot1 HAS FAILED.", false));
            } else {
                done = ResponseOption.attach(TEST_CODEBOOK_SLOT2_URI, TEST_RESPONSE_OPTION2_URI);
                if (!done) {
                    return ok(ApiUtil.createResponse("The attachment of Test Response Option 2 to Test CodebookSlot2 HAS FAILED.", false));
                }
            }
        }
        return ok(ApiUtil.createResponse("Test Response Options 1 and 2 have been ATTACHED to Test Codebook Slots 1 and 2.", true));
    }

    public Result detach(String codebookSlotUri){
        if (codebookSlotUri == null || codebookSlotUri.equals("")) {
            return ok(ApiUtil.createResponse("No attachment URI has been provided.", false));
        }
        CodebookSlot codebookSlot = CodebookSlot.find(codebookSlotUri);
        if (codebookSlot == null) {
            return ok(ApiUtil.createResponse("There is no Codebook Slot with URI <" + codebookSlotUri + ">.", false));
        }
        if (ResponseOption.detach(codebookSlotUri)) {
            return ok(ApiUtil.createResponse("No Response Option is associated with Codebook Slot <" + codebookSlotUri + ">.", true));
        }
        return ok(ApiUtil.createResponse("A Response Option has failed to be removed from Codebook Slot <" + codebookSlotUri + ">.", false));
    }

    public Result detachForTesting(){
        Experience testExp = Experience.find(TEST_EXPERIENCE_URI);
        if (testExp == null) {
            return ok(ApiUtil.createResponse("There is no test experience to have their response options detached.", false));
        }
        if (testExp.getCodebookSlots() == null) {
            return ok(ApiUtil.createResponse("Test experience has no CodebookSlots for Response Options.", false));
        }
        ResponseOption test1 = ResponseOption.find(TEST_RESPONSE_OPTION1_URI);
        ResponseOption test2 = ResponseOption.find(TEST_RESPONSE_OPTION2_URI);
        if (test1 == null) {
            return ok(ApiUtil.createResponse("There is no Test Response Option 1 to be detached from test code book slot.", false));
        } else if (test2 == null) {
            return ok(ApiUtil.createResponse("There is no Test Response Option 2 to be detached from test code book slot.", false));
        } else {
            boolean done = ResponseOption.detach(TEST_CODEBOOK_SLOT1_URI);
            if (done) {
                done = ResponseOption.detach(TEST_CODEBOOK_SLOT2_URI);
            }
            if (done) {
                return ok(ApiUtil.createResponse("Test Response Options 1 and 2 have been DETACHED from Test Codebook Slot 1.", true));
            }
        }
        return ok(ApiUtil.createResponse("The detachment of Test Detectors 1 and 2 from Test Instrument HAS FAILED.", false));
    }

    public Result getAllCodebookSlots(){
        List<CodebookSlot> results = CodebookSlot.find();
        return getCodebookSlots(results);
    }

    public Result getCodebookSlotsByExperience(String experienceUri){
        List<CodebookSlot> results = CodebookSlot.findByExperience(experienceUri);
        return getCodebookSlots(results);
    }

    private Result getCodebookSlots(List<CodebookSlot> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No codebook slot has been found", false));
        } else {
            ObjectMapper mapper = new ObjectMapper();
            SimpleFilterProvider filterProvider = new SimpleFilterProvider();
            filterProvider.addFilter("codebookSlotFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "comment", "hasPriority", "hasResponseOption"));
            mapper.setFilterProvider(filterProvider);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }



}
