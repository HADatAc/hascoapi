package org.sirapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import org.sirapi.Constants;
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
        // System.out.println("ResponseOption <" + responseOption.getUri() + "> has been
        // CREATED.");
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
            testResponseOption1.setHasSIRManagerEmail("me@example.com");
            testResponseOption1.setNamedGraph(Constants.TEST_KB);
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
            testResponseOption2.setHasSIRManagerEmail("me@example.com");
            testResponseOption2.setNamedGraph(Constants.TEST_KB);
            testResponseOption2.save();

            return ok(ApiUtil.createResponse("Test ResponseOptions 1 and 2 have been CREATED.", true));
        }
    }

    public Result createResponseOption(String json) {
        if (json == null || json.equals("")) {
            return ok(ApiUtil.createResponse("No json content has been provided.", false));
        }
        // System.out.println("(createResponseOption) Value of json: [" + json + "]");
        ObjectMapper objectMapper = new ObjectMapper();
        ResponseOption newResponseOption;
        try {
            newResponseOption = objectMapper.readValue(json, ResponseOption.class);
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

    public Result deleteResponseOptionsForTesting() {
        ResponseOption test1 = ResponseOption.find(TEST_RESPONSE_OPTION1_URI);
        ResponseOption test2 = ResponseOption.find(TEST_RESPONSE_OPTION2_URI);
        if (test1 == null) {
            return ok(ApiUtil.createResponse("There is no Test ResponseOption 1 to be deleted.", false));
        } else if (test2 == null) {
            return ok(ApiUtil.createResponse("There is no Test ResponseOption 2 to be deleted.", false));
        } else {
            test1.setNamedGraph(Constants.TEST_KB);
            test1.delete();
            test2.setNamedGraph(Constants.TEST_KB);
            test2.delete();
            return ok(ApiUtil.createResponse("Test ResponseOptions 1 and 2 have been DELETED.", true));
        }
    }

    public Result deleteResponseOption(String uri) {
        if (uri == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No ResponseOption URI has been provided.", false));
        }
        ResponseOption responseOption = ResponseOption.find(uri);
        if (responseOption == null) {
            return ok(
                    ApiUtil.createResponse("There is no ResponseOption with URI <" + uri + "> to be deleted.", false));
        } else {
            return deleteResponseOptionResult(responseOption);
        }
    }

    /** 
    public Result getAllResponseOptions() {
        ObjectMapper mapper = new ObjectMapper();

        List<ResponseOption> results = ResponseOption.find();
        if (results == null) {
            return notFound(ApiUtil.createResponse("No ResponseOption has been found", false));
        } else {
            SimpleFilterProvider filterProvider = new SimpleFilterProvider();
            filterProvider.addFilter("responseOptionFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "comment",
                            "hasContent", "hasLanguage", "hasVersion", "hasSIRManagerEmail"));
            mapper.setFilterProvider(filterProvider);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }
    */

    public static Result getResponseOptions(List<ResponseOption> results) {
        if (results == null) {
            return ok(ApiUtil.createResponse("No response option has been found", false));
        } else {
            ObjectMapper mapper = new ObjectMapper();
            SimpleFilterProvider filterProvider = new SimpleFilterProvider();
            filterProvider.addFilter("responseOptionFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "comment", "hasContent", "hasLanguage", "hasVersion",
                            "hasSIRManagerEmail"));
            mapper.setFilterProvider(filterProvider);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

    public Result attach(String uri, String responseOptionSlotUri) {
        if (uri == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No response option URI has been provided.", false));
        }
        ResponseOption responseOption = ResponseOption.find(uri);
        if (responseOption == null) {
            return ok(ApiUtil.createResponse("There is no detector with URI <" + uri + "> to be attached.", false));
        }
        if (responseOptionSlotUri == null || responseOptionSlotUri.equals("")) {
            return ok(ApiUtil.createResponse("No ResponseOptionSlot URI has been provided.", false));
        }
        ResponseOptionSlot responseOptionSlot = ResponseOptionSlot.find(responseOptionSlotUri);
        if (responseOptionSlot == null) {
            return ok(ApiUtil.createResponse("There is no ResponseOptionSlot with uri <" + responseOptionSlotUri + ">.",
                    false));
        }
        if (ResponseOption.attach(responseOptionSlot, responseOption)) {
            return ok(ApiUtil.createResponse("ResponseOption <" + uri
                    + "> successfully attached to ResponseOptionSlot <" + responseOptionSlotUri + ">.", true));
        }
        return ok(ApiUtil.createResponse("ResponseOption <" + uri + "> failed to associate with ResponseOptionSlot  <"
                + responseOptionSlotUri + ">.", false));
    }

    public Result attachForTesting() {
        Codebook testCodebook = Codebook.find(TEST_CODEBOOK_URI);
        if (testCodebook == null) {
            return ok(ApiUtil.createResponse("Create test codebook before attaching response options.", false));
        }
        if (testCodebook.getResponseOptionSlots() == null) {
            return ok(ApiUtil.createResponse(
                    "Create response option slots for test codebook before attaching response options.", false));
        }
        ResponseOptionSlot slot1 = ResponseOptionSlot.find(TEST_RESPONSE_OPTION_SLOT1_URI);
        ResponseOptionSlot slot2 = ResponseOptionSlot.find(TEST_RESPONSE_OPTION_SLOT2_URI);
        if (slot1 == null || slot2 == null) {
            return ok(ApiUtil.createResponse(
                    "Either Test ResponseOption Slot 1 or 2 is unavailable to allow the detectorSlot of ResponseOptions 1 and 2 to test codebook.",
                    false));
        }
        if (slot1.getHasResponseOption() != null) {
            return ok(ApiUtil.createResponse("Test ResponseOption Slot 1 already has an attached Response Option", false));
        }
        if (slot2.getHasResponseOption() != null) {
            return ok(ApiUtil.createResponse("Test ResponseOption Slot 2 already has an attached Response Option", false));
        }
        ResponseOption test1 = ResponseOption.find(TEST_RESPONSE_OPTION1_URI);
        ResponseOption test2 = ResponseOption.find(TEST_RESPONSE_OPTION2_URI);
        if (test1 == null || test2 == null) {
            return ok(ApiUtil.createResponse(
                    "Either Test Response Option 1 or 2 is unavailable to be attached to test codebook.", false));
        } else {
            slot1.setNamedGraph(Constants.TEST_KB);
            slot2.setNamedGraph(Constants.TEST_KB);
            test1.setNamedGraph(Constants.TEST_KB);
            test2.setNamedGraph(Constants.TEST_KB);
            boolean done = ResponseOption.attach(slot1, test1);
            if (!done) {
                return ok(ApiUtil.createResponse(
                        "The detectorSlot of Test Response Option 1 to Test ResponseOptionSlot1 HAS FAILED.", false));
            } else {
                done = ResponseOption.attach(slot2, test2);
                if (!done) {
                    return ok(ApiUtil.createResponse(
                            "The detectorSlot of Test Response Option 2 to Test ResponseOptionSlot2 HAS FAILED.", false));
                }
            }
        }
        return ok(ApiUtil.createResponse(
                "Test Response Options 1 and 2 have been ATTACHED to Test ResponseOption Slots 1 and 2.", true));
    }

    public Result detach(String responseOptionSlotUri) {
        if (responseOptionSlotUri == null || responseOptionSlotUri.equals("")) {
            return ok(ApiUtil.createResponse("No detectorSlot URI has been provided.", false));
        }
        ResponseOptionSlot responseOptionSlot = ResponseOptionSlot.find(responseOptionSlotUri);
        if (responseOptionSlot == null) {
            return ok(ApiUtil.createResponse("There is no ResponseOption Slot with URI <" + responseOptionSlotUri + ">.",
                    false));
        }
        if (ResponseOption.detach(responseOptionSlot)) {
            return ok(ApiUtil.createResponse(
                    "No Response Option is associated with ResponseOption Slot <" + responseOptionSlotUri + ">.", true));
        }
        return ok(ApiUtil.createResponse(
                "A Response Option has failed to be removed from ResponseOption Slot <" + responseOptionSlotUri + ">.",
                false));
    }

    public Result detachForTesting() {
        Codebook testCodebook = Codebook.find(TEST_CODEBOOK_URI);
        if (testCodebook == null) {
            return ok(ApiUtil.createResponse("There is no test codebook to have their response options detached.",
                    false));
        }
        if (testCodebook.getResponseOptionSlots() == null) {
            return ok(ApiUtil.createResponse("Test codebook has no ResponseOptionSlots for Response Options.", false));
        }
        ResponseOption test1 = ResponseOption.find(TEST_RESPONSE_OPTION1_URI);
        ResponseOption test2 = ResponseOption.find(TEST_RESPONSE_OPTION2_URI);
        ResponseOptionSlot slot1 = ResponseOptionSlot.find(TEST_RESPONSE_OPTION_SLOT1_URI);
        ResponseOptionSlot slot2 = ResponseOptionSlot.find(TEST_RESPONSE_OPTION_SLOT2_URI);
        if (test1 == null) {
            return ok(ApiUtil.createResponse(
                    "There is no Test Response Option 1 to be detached from test code book slot.", false));
        } else if (test2 == null) {
            return ok(ApiUtil.createResponse(
                    "There is no Test Response Option 2 to be detached from test code book slot.", false));
        } else if (slot1 == null) {
            return ok(ApiUtil.createResponse(
                    "There is no Test Response Option Slot 1 in test code book.", false));
        } else if (slot2 == null) {
            return ok(ApiUtil.createResponse(
                    "There is no Test Response Option Slot 2 in test code book.", false));
        } else if (slot1.getResponseOption() == null) {
            return ok(ApiUtil.createResponse(
                    "There is no Test Response Option to be detached from Slot 1.", false));
        } else if (slot2.getResponseOption() == null) {
            return ok(ApiUtil.createResponse(
                    "There is no Test Response Option to be detached from Slot 2.", false));
        } else {
            slot1.setNamedGraph(Constants.TEST_KB);
            slot2.setNamedGraph(Constants.TEST_KB);
            boolean done = ResponseOption.detach(slot1);
            if (done) {
                done = ResponseOption.detach(slot2);
            }
            if (done) {
                return ok(ApiUtil.createResponse(
                        "Test Response Options 1 and 2 have been DETACHED from Test ResponseOption Slot 1.", true));
            }
        }
        return ok(ApiUtil.createResponse("The detachment of Test Detectors 1 and 2 from Test Instrument HAS FAILED.",
                false));
    }

    public Result getAllResponseOptionSlots() {
        List<ResponseOptionSlot> results = ResponseOptionSlot.find();
        return getResponseOptionSlots(results);
    }

    public Result getResponseOptionSlotsByCodebook(String codebookUri) {
        List<ResponseOptionSlot> results = ResponseOptionSlot.findByCodebook(codebookUri);
        return getResponseOptionSlots(results);
    }

    public static Result getResponseOptionSlots(List<ResponseOptionSlot> results) {
        if (results == null) {
            return ok(ApiUtil.createResponse("No response option slot has been found", false));
        } else {
            ObjectMapper mapper = new ObjectMapper();
            SimpleFilterProvider filterProvider = new SimpleFilterProvider();
            filterProvider.addFilter("ResponseOptionSlotFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "comment", "hasPriority", "hasResponseOption"));
            mapper.setFilterProvider(filterProvider);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

}
