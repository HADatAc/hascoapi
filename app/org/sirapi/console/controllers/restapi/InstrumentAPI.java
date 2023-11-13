package org.sirapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

import org.sirapi.entity.fhir.Questionnaire;
import org.sirapi.entity.pojo.Instrument;
import org.sirapi.transform.Renderings;
import org.sirapi.utils.ApiUtil;
import org.sirapi.vocabularies.VSTOI;
import play.mvc.Controller;
import play.mvc.Result;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.sirapi.Constants.TEST_INSTRUMENT_URI;
import static org.sirapi.Constants.TEST_INSTRUMENT_TOT_DETECTOR_SLOTS;

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
            testInstrument.setHasInformant(VSTOI.DEFAULT_INFORMANT);
            testInstrument.setHasShortName("TEST");
            testInstrument.setHasInstruction("Please put a circle around the word that shows how often each of these things happens to you. There are no right or wrong answers. ");
            testInstrument.setHasLanguage(VSTOI.DEFAULT_LANGUAGE); // ISO 639-1
            testInstrument.setComment("This is a dummy instrument created to test the SIR API.");
            testInstrument.setHasVersion("1");
            testInstrument.setHasSIRManagerEmail("me@example.com");
            testInstrument.setHasPageNumber("Page ");
            testInstrument.setHasDateField("Date: ____________ ");
            testInstrument.setHasSubjectIDField("Name/ID: _____________________");
            testInstrument.setHasSubjectRelationshipField("Relationship to Subject: ______________________");
            testInstrument.setHasCopyrightNotice("Copyright (c) 2000 HADatAc.org");

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

    public Result createDetectorSlots(String instrumentUri, String totDetectorSlots) {
        if (instrumentUri == null || instrumentUri.equals("")) {
            return ok(ApiUtil.createResponse("No instrument URI has been provided.", false));
        }
        Instrument instrument = Instrument.find(instrumentUri);
        if (instrument == null) {
            return ok(ApiUtil.createResponse("No instrument with provided URI has been found.", false));
        }
        if (instrument.getDetectorSlots() != null) {
            return ok(ApiUtil.createResponse("Instrument already has detectorSlots. Delete existing detectorSlots before creating new detectorSlots", false));
        }
        if (totDetectorSlots == null || totDetectorSlots.equals("")) {
            return ok(ApiUtil.createResponse("No total numbers of detectorSlots to be created has been provided.", false));
        }
        int total = 0;
        try {
            total = Integer.parseInt(totDetectorSlots);
        } catch (Exception e) {
            return ok(ApiUtil.createResponse("totDetectorSlots is not a valid number of detectorSlots.", false));
        }
        if (total <= 0) {
            return ok(ApiUtil.createResponse("Total numbers of detectorSlots need to be greated than zero.", false));
        }
        if (instrument.createDetectorSlots(total)) {
            return ok(ApiUtil.createResponse("A total of " + total + " detectorSlots have been created for instrument <" + instrumentUri + ">.", true));
        } else {
            return ok(ApiUtil.createResponse("Method failed to create detectorSlots for instrument <" + instrumentUri + ">.", false));
        }
    }

    public Result createDetectorSlotsForTesting() {
        Instrument testInstrument = Instrument.find(TEST_INSTRUMENT_URI);
        if (testInstrument == null) {
            return ok(ApiUtil.createResponse("Test instrument <" + TEST_INSTRUMENT_URI + "> needs to exist before its detectorSlots can be created.", false));
        } else if (testInstrument.getDetectorSlots() != null && testInstrument.getDetectorSlots().size() > 0) {
            return ok(ApiUtil.createResponse("Test instrument <" + TEST_INSTRUMENT_URI + "> already has detectorSlots.", false));
        } else {
            return createDetectorSlots(testInstrument.getUri(), TEST_INSTRUMENT_TOT_DETECTOR_SLOTS);
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

    public Result deleteDetectorSlotsForTesting() {
        Instrument testInstrument = Instrument.find(TEST_INSTRUMENT_URI);
        if (testInstrument == null) {
            return ok(ApiUtil.createResponse("Test instrument <" + TEST_INSTRUMENT_URI + "> needs to exist before its detectorSlots can be deleted.", false));
        } else if (testInstrument.getDetectorSlots() == null || testInstrument.getDetectorSlots().size() == 0) {
            return ok(ApiUtil.createResponse("Test instrument <" + TEST_INSTRUMENT_URI + "> has no detectorSlots to be deleted.", false));
        } else {
            return deleteDetectorSlots(testInstrument.getUri());
        }
    }

    public Result deleteDetectorSlots(String instrumentUri) {
        if (instrumentUri == null || instrumentUri.equals("")) {
            return ok(ApiUtil.createResponse("No instrument URI has been provided.", false));
        }
        Instrument instrument = Instrument.find(instrumentUri);
        if (instrument == null) {
            return ok(ApiUtil.createResponse("No instrument with provided URI has been found.", false));
        }
        if (instrument.getDetectorSlots() == null) {
            return ok(ApiUtil.createResponse("Instrument has no detectorSlot to be deleted.", false));
        }
        instrument.deleteDetectorSlots();
        return ok(ApiUtil.createResponse("DetectorSlots for Instrument <" + instrument.getUri() + "> have been deleted.", true));
    }

    public static Result getInstruments(List<Instrument> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No instrument has been found", false));
        } else {
            ObjectMapper mapper = new ObjectMapper();
            SimpleFilterProvider filterProvider = new SimpleFilterProvider();
            filterProvider.addFilter("instrumentFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "hasShortName", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "comment", "hasSerialNumber", "hasInformant", "hasImage",
                            "hasLanguage", "hasVersion", "hasInstruction", "hasSIRManagerEmail",
                            "hasPageNumber", "hasDateField", "hasSubjectIDField", "hasSubjectRelatioshipField", "hasCopyrightNotice"));
            mapper.setFilterProvider(filterProvider);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

    public Result toTextPlain(String uri) {
        if (uri  == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No URI has been provided", false));
        }
        String instrumentText = Renderings.toString(uri, 80);
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
        String instrumentText = Renderings.toHTML(uri, 80);
        if (instrumentText == null || instrumentText.equals("")) {
            return ok(ApiUtil.createResponse("No instrument has been found", false));
        } else {
            return ok(instrumentText).as("text/html");
        }
    }

    public Result toTextPDF(String uri) {
        if (uri  == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No URI has been provided", false));
        }
        ByteArrayOutputStream instrumentText = Renderings.toPDF(uri, 80);
        if (instrumentText == null || instrumentText.equals("")) {
            return ok(ApiUtil.createResponse("No instrument has been found", false));
        } else {
            return ok(instrumentText.toByteArray()).as("application/pdf");
        }
    }

    public Result toFHIR(String uri) {
        if (uri  == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No URI has been provided", false));
        }
        Instrument instr = Instrument.find(uri);
        if (instr == null) {
            return ok(ApiUtil.createResponse("No instrument instance found for uri [" + uri + "]", false));
        }

        Questionnaire quest = new Questionnaire(instr);

        FhirContext ctx = FhirContext.forR4();
        IParser parser = ctx.newJsonParser();
        String serialized = parser.encodeResourceToString(quest.getFHIRObject());

        return ok(serialized).as("application/json");
    }

    public Result toRDF(String uri) {
        if (uri  == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No URI has been provided", false));
        }
        Instrument instr = Instrument.find(uri);
        if (instr == null) {
            return ok(ApiUtil.createResponse("No instrument instance found for uri [" + uri + "]", false));
        }

        String serialized = instr.printRDF();

        return ok(serialized).as("application/xml");
    }
}
