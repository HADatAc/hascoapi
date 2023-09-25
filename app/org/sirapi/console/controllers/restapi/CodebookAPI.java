package org.sirapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.sirapi.entity.pojo.Codebook;
import org.sirapi.entity.pojo.Instrument;
import org.sirapi.utils.ApiUtil;
import org.sirapi.vocabularies.VSTOI;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.List;

import static org.sirapi.Constants.*;

public class CodebookAPI extends Controller {

    private Result createCodebookResult(Codebook codebook) {
        codebook.save();
        return ok(ApiUtil.createResponse("Codebook <" + codebook.getUri() + "> has been CREATED.", true));
    }

    public Result createCodebookForTesting() {
        Codebook testCodebook = Codebook.find(TEST_CODEBOOK_URI);
        if (testCodebook != null) {
            return ok(ApiUtil.createResponse("Test Codebook already exists.", false));
        } else {
            testCodebook = new Codebook();
            testCodebook.setUri(TEST_CODEBOOK_URI);
            testCodebook.setLabel("Test Codebook");
            testCodebook.setTypeUri(VSTOI.CODEBOOK);
            testCodebook.setHascoTypeUri(VSTOI.CODEBOOK);
            testCodebook.setHasLanguage("en");
            testCodebook.setHasVersion("1");
            testCodebook.setHasSIRManagerEmail("me@example.com");
            testCodebook.save();
            return ok(ApiUtil.createResponse("Test Codebook been CREATED.", true));
        }
    }

    public Result createCodebook(String json) {
        if (json == null || json.equals("")) {
            return ok(ApiUtil.createResponse("No json content has been provided.", false));
        }
        //System.out.println("[createCodebook] Value of json: [" + json + "]");
        ObjectMapper objectMapper = new ObjectMapper();
        Codebook newCodebook;
        try {
            // convert json string to Instrument instance
            newCodebook = objectMapper.readValue(json, Codebook.class);
        } catch (Exception e) {
            e.printStackTrace();
            return ok(ApiUtil.createResponse("Failed to parse json.", false));
        }
        return createCodebookResult(newCodebook);
    }

    private Result deleteCodebookResult(Codebook codebook) {
        String uri = codebook.getUri();
        codebook.delete();
        return ok(ApiUtil.createResponse("Codebook <" + uri + "> has been DELETED.", true));
    }

    public Result deleteCodebookForTesting() {
        Codebook codebook = Codebook.find(TEST_CODEBOOK_URI);
        if (codebook == null) {
            return ok(ApiUtil.createResponse("There is no Test Codebook to be deleted.", false));
        } else {
            codebook.delete();
            return ok(ApiUtil.createResponse("Test Codebook has been DELETED.", true));
        }
    }

    public Result deleteCodebook(String uri) {
        if (uri == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No codebook URI has been provided.", false));
        }
        Codebook codebook = Codebook.find(uri);
        if (codebook == null) {
            return ok(ApiUtil.createResponse("There is no codebook with URI <" + uri + "> to be deleted.", false));
        } else {
            return deleteCodebookResult(codebook);
        }
    }

    public Result getCodebookByLanguage(String language) {
        List<Codebook> results = Codebook.findByLanguage(language);
        return getCodebooks(results);
    }

    public Result getCodebookByKeyword(String keyword) {
        List<Codebook> results = Codebook.findByKeyword(keyword);
        return getCodebooks(results);
    }

    public Result getCodebookByManagerEmail(String managerEmail) {
        List<Codebook> results = Codebook.findByManagerEmail(managerEmail);
        return getCodebooks(results);
    }

    public static Result getCodebooks(List<Codebook> results) {
        if (results == null) {
            return ok(ApiUtil.createResponse("No codebook has been found", false));
        } else {
            ObjectMapper mapper = new ObjectMapper();
            SimpleFilterProvider filterProvider = new SimpleFilterProvider();
            filterProvider.addFilter("codebookFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "comment", "hasSerialNumber", "hasLanguage", "hasVersion",
                            "hasSIRManagerEmail"));
            mapper.setFilterProvider(filterProvider);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

    public Result getAllCodebooks() {
        ObjectMapper mapper = new ObjectMapper();

        List<Codebook> results = Codebook.find();
        if (results == null) {
            return notFound(ApiUtil.createResponse("No codebook has been found", false));
        } else {
            SimpleFilterProvider filterProvider = new SimpleFilterProvider();
            filterProvider.addFilter("codebookFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "comment", "hasSerialNumber", "hasLanguage", "hasVersion",
                            "hasSIRManagerEmail"));
            mapper.setFilterProvider(filterProvider);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

    public Result createResponseOptionSlots(String codebookUri, String totResponseOptionSlots) {
        if (codebookUri == null || codebookUri.equals("")) {
            return ok(ApiUtil.createResponse("No codebook URI has been provided.", false));
        }
        Codebook codebook = Codebook.find(codebookUri);
        if (codebook == null) {
            return ok(ApiUtil.createResponse("No codebook with provided URI has been found.", false));
        }
        if (codebook.getResponseOptionSlots() != null) {
            return ok(ApiUtil.createResponse(
                    "Codebook already has detectorSlots. Delete existing detectorSlots before creating new detectorSlots",
                    false));
        }
        if (totResponseOptionSlots == null || totResponseOptionSlots.equals("")) {
            return ok(
                    ApiUtil.createResponse("No total numbers of detectorSlots to be created has been provided.", false));
        }
        int total = 0;
        try {
            total = Integer.parseInt(totResponseOptionSlots);
        } catch (Exception e) {
            return ok(ApiUtil.createResponse("totResponseOptionSlots is not a valid number of detectorSlots.", false));
        }
        if (total <= 0) {
            return ok(ApiUtil.createResponse("Total numbers of responseoption slots need to be greater than zero.", false));
        }
        if (codebook.createResponseOptionSlots(total)) {
            return ok(ApiUtil.createResponse(
                    "A total of " + total + " responseoption slots have been created for codebook <" + codebookUri + ">.",
                    true));
        } else {
            return ok(ApiUtil.createResponse(
                    "Method failed to create responseoption slots for codebook <" + codebookUri + ">.", false));
        }
    }

    public Result createResponseOptionSlotsForTesting() {
        Codebook testCodebook = Codebook.find(TEST_CODEBOOK_URI);
        if (testCodebook == null) {
            return ok(ApiUtil.createResponse("Test codebook <" + TEST_CODEBOOK_URI
                    + "> needs to exist before its responseoption slots can be created.", false));
        } else if (testCodebook.getResponseOptionSlots() != null && testCodebook.getResponseOptionSlots().size() > 0) {
            return ok(ApiUtil.createResponse("Test codebook <" + TEST_CODEBOOK_URI + "> already has responseoption slots.",
                    false));
        } else {
            return createResponseOptionSlots(testCodebook.getUri(), TEST_CODEBOOK_TOT_RESPONSE_OPTION_SLOTS);
        }
    }

    public Result deleteResponseOptionSlots(String codebookUri) {
        if (codebookUri == null || codebookUri.equals("")) {
            return ok(ApiUtil.createResponse("No codebook URI has been provided.", false));
        }
        Codebook codebook = Codebook.find(codebookUri);
        if (codebook == null) {
            return ok(ApiUtil.createResponse("No codebook with provided URI has been found.", false));
        }
        if (codebook.getResponseOptionSlots() == null) {
            return ok(ApiUtil.createResponse("Codebook has no responseoption slots to be deleted.", false));
        }
        codebook.deleteResponseOptionSlots();
        return ok(ApiUtil.createResponse(
                "ResponseOptionSlots for Codebook <" + codebook.getUri() + "> have been deleted.", true));
    }

    public Result deleteResponseOptionSlotsForTesting() {
        Codebook testCodebook = Codebook.find(TEST_CODEBOOK_URI);
        if (testCodebook == null) {
            return ok(ApiUtil.createResponse("Test codebook <" + TEST_CODEBOOK_URI
                    + "> needs to exist before its responseoption slots can be deleted.", false));
        } else if (testCodebook.getResponseOptionSlots() == null || testCodebook.getResponseOptionSlots().size() == 0) {
            return ok(ApiUtil.createResponse(
                    "Test codebook <" + TEST_CODEBOOK_URI + "> has no responseoption slots to be deleted.", false));
        } else {
            return deleteResponseOptionSlots(testCodebook.getUri());
        }
    }

}
