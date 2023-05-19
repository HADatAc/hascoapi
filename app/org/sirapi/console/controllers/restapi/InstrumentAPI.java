package org.sirapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.sirapi.annotations.PropertyField;
import org.sirapi.entity.pojo.Instrument;
import org.sirapi.utils.ApiUtil;
import org.sirapi.vocabularies.VSTOI;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.List;

import static org.sirapi.Constants.TEST_INSTRUMENT_URI;
import static org.sirapi.Constants.TEST_INSTRUMENT_TOT_ATTACHMENTS;

public class InstrumentAPI extends Controller {

    private Result createInstrumentResult(Instrument inst) {
        inst.save();
        return ok(ApiUtil.createResponse("Instrument <" + inst.getUri() + "> has been CREATED.", true));
    }

    public Result createInstrumentForTesting() {
        Instrument testInstrument = Instrument.find(TEST_INSTRUMENT_URI);
        if (testInstrument != null) {
            return ok(ApiUtil.createResponse("Test instrument <" + TEST_INSTRUMENT_URI + "> already exists.", false));
        } else {
            testInstrument = new Instrument();
            testInstrument.setUri(TEST_INSTRUMENT_URI);
            testInstrument.setLabel("Test Instrument");
            testInstrument.setTypeUri(VSTOI.QUESTIONNAIRE);
            testInstrument.setHascoTypeUri(VSTOI.INSTRUMENT);
            testInstrument.setHasShortName("TEST");
            testInstrument.setHasInstruction("Please put a circle around the word that shows how often each of these things happens to you. There are no right or wrong answers. ");
            testInstrument.setHasLanguage("en"); // ISO 639-1
            testInstrument.setComment("This is a dummy instrument created to test the SIR API.");
            testInstrument.setHasVersion("1");
            testInstrument.setHasSIRMaintainerEmail("me@example.com");
            return createInstrumentResult(testInstrument);
        }
    }

    public Result createInstrument(String json) {
        if (json == null || json.equals("")) {
            return ok(ApiUtil.createResponse("No json content has been provided.", false));
        }
        //System.out.println("(InstrumentAPI) Value of json in createInstrument: [" + json + "]");
        ObjectMapper objectMapper = new ObjectMapper();
        Instrument newInst;
        try {
            //convert json string to Instrument instance
            newInst  = objectMapper.readValue(json, Instrument.class);
        } catch (Exception e) {
            //System.out.println("(InstrumentAPI) Failed to parse json for [" + json + "]");
            return ok(ApiUtil.createResponse("Failed to parse json.", false));
        }
        return createInstrumentResult(newInst);
    }

    public Result createAttachments(String instrumentUri, String totAttachments) {
        if (instrumentUri == null || instrumentUri.equals("")) {
            return ok(ApiUtil.createResponse("No instrument URI has been provided.", false));
        }
        Instrument instrument = Instrument.find(instrumentUri);
        if (instrument == null) {
            return ok(ApiUtil.createResponse("No instrument with provided URI has been found.", false));
        }
        if (instrument.getAttachments() != null) {
            return ok(ApiUtil.createResponse("Instrument already has attachments. Delete existing attachments before creating new attachments", false));
        }
        if (totAttachments == null || totAttachments.equals("")) {
            return ok(ApiUtil.createResponse("No total numbers of attachments to be created has been provided.", false));
        }
        int total = 0;
        try {
            total = Integer.parseInt(totAttachments);
        } catch (Exception e) {
            return ok(ApiUtil.createResponse("totAttachments is not a valid number of attachments.", false));
        }
        if (total <= 0) {
            return ok(ApiUtil.createResponse("Total numbers of attachments need to be greated than zero.", false));
        }
        if (instrument.createAttachments(total)) {
            return ok(ApiUtil.createResponse("A total of " + total + " attachments have been created for instrument <" + instrumentUri + ">.", true));
        } else {
            return ok(ApiUtil.createResponse("Method failed to create attachments for instrument <" + instrumentUri + ">.", false));
        }
    }

    public Result createAttachmentsForTesting() {
        Instrument testInstrument = Instrument.find(TEST_INSTRUMENT_URI);
        if (testInstrument == null) {
            return ok(ApiUtil.createResponse("Test instrument <" + TEST_INSTRUMENT_URI + "> needs to exist before its attachments can be created.", false));
        } else if (testInstrument.getAttachments() != null && testInstrument.getAttachments().size() > 0) {
            return ok(ApiUtil.createResponse("Test instrument <" + TEST_INSTRUMENT_URI + "> already has attachments.", false));
        } else {
            return createAttachments(testInstrument.getUri(), TEST_INSTRUMENT_TOT_ATTACHMENTS);
        }
    }

    private Result deleteInstrumentResult(Instrument inst) {
        String uri = inst.getUri();
        inst.delete();
        return ok(ApiUtil.createResponse("Instrument <" + uri + "> has been DELETED.", true));
    }

    public Result deleteInstrumentForTesting(){
        Instrument test;
        test = Instrument.find(TEST_INSTRUMENT_URI);
        if (test == null) {
            return ok(ApiUtil.createResponse("There is no Test instrument to be deleted.", false));
        } else {
            return deleteInstrumentResult(test);
        }
    }

    public Result deleteInstrument(String uri){
        if (uri == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No instrument URI has been provided.", false));
        }
        Instrument inst = Instrument.find(uri);
        if (inst == null) {
            return ok(ApiUtil.createResponse("There is no instrument with URI <" + uri + "> to be deleted.", false));
        } else {
            return deleteInstrumentResult(inst);
        }
    }

    public Result deleteAttachmentsForTesting() {
        Instrument testInstrument = Instrument.find(TEST_INSTRUMENT_URI);
        if (testInstrument == null) {
            return ok(ApiUtil.createResponse("Test instrument <" + TEST_INSTRUMENT_URI + "> needs to exist before its attachments can be deleted.", false));
        } else if (testInstrument.getAttachments() == null || testInstrument.getAttachments().size() == 0) {
            return ok(ApiUtil.createResponse("Test instrument <" + TEST_INSTRUMENT_URI + "> has no attachments to be deleted.", false));
        } else {
            return deleteAttachments(testInstrument.getUri());
        }
    }

    public Result deleteAttachments(String instrumentUri) {
        if (instrumentUri == null || instrumentUri.equals("")) {
            return ok(ApiUtil.createResponse("No instrument URI has been provided.", false));
        }
        Instrument instrument = Instrument.find(instrumentUri);
        if (instrument == null) {
            return ok(ApiUtil.createResponse("No instrument with provided URI has been found.", false));
        }
        if (instrument.getAttachments() == null) {
            return ok(ApiUtil.createResponse("Instrument has no attachment to be deleted.", false));
        }
        instrument.deleteAttachments();
        return ok(ApiUtil.createResponse("Attachments for Instrument <" + instrument.getUri() + "> have been deleted.", true));
    }

    public Result getAllInstruments(){
        List<Instrument> results = Instrument.find();
        return getInstruments(results);
    }

    public Result getInstrumentsByLanguage(String language){
        List<Instrument> results = Instrument.findByLanguage(language);
        return getInstruments(results);
    }

    public Result getInstrumentsByKeyword(String keyword){
        List<Instrument> results = Instrument.findByKeyword(keyword);
        return getInstruments(results);
    }

    public Result getInstrumentsByKeywordAndLanguage(String keyword, String language){
        List<Instrument> results = Instrument.findByKeywordAndLanguage(keyword, language);
        return getInstruments(results);
    }

    public Result getInstrumentsByMaintainerEmail(String maintainerEmail){
        List<Instrument> results = Instrument.findByMaintainerEmail(maintainerEmail);
        return getInstruments(results);
    }

    private Result getInstruments(List<Instrument> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No instrument has been found", false));
        } else {
            ObjectMapper mapper = new ObjectMapper();
            SimpleFilterProvider filterProvider = new SimpleFilterProvider();
            filterProvider.addFilter("instrumentFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "hasShortName", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "comment", "hasSerialNumber", "hasImage",
                            "hasLanguage", "hasVersion", "hasInstruction", "hasSIRMaintainerEmail"));
            mapper.setFilterProvider(filterProvider);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

    public Result toTextPlain(String uri) {
        if (uri  == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No URI has been provided", false));
        }
        String instrumentText = Instrument.toString(uri, 80);
        if (instrumentText == null || instrumentText.equals("")) {
            return ok(ApiUtil.createResponse("No instrument has been found", false));
        } else {
            return ok(instrumentText).as("text/plain");
        }
    }

    public Result toTextHTML(String uri) {
        if (uri  == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No URI has been provided", false));
        }
        String instrumentText = Instrument.toHTML(uri, 80);
        if (instrumentText == null || instrumentText.equals("")) {
            return ok(ApiUtil.createResponse("No instrument has been found", false));
        } else {
            return ok(instrumentText).as("text/html");
        }
    }


}
