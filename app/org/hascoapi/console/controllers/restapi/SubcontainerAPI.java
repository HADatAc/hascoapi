package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

import org.hascoapi.Constants;
import org.hascoapi.entity.fhir.Questionnaire;
import org.hascoapi.entity.pojo.Container;
import org.hascoapi.entity.pojo.Instrument;
import org.hascoapi.entity.pojo.Subcontainer;
import org.hascoapi.transform.Renderings;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.HAScOMapper;
import org.hascoapi.vocabularies.VSTOI;
import play.mvc.Controller;
import play.mvc.Result;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.hascoapi.Constants.*;

public class SubcontainerAPI extends Controller {

    private Result createSubcontainerResult(Subcontainer subcontainer) {
        subcontainer.save();
        return ok(ApiUtil.createResponse("Subcontainer <" + subcontainer.getUri() + "> has been CREATED.", true));
    }

    public Result createSubcontainerForTesting() {
        Instrument testInstrument = Instrument.find(TEST_INSTRUMENT_URI);
        Subcontainer testSubcontainer = Subcontainer.find(TEST_SUBCONTAINER_URI);
        if (testInstrument == null) {
            return ok(ApiUtil.createResponse("Test instrument <" + TEST_INSTRUMENT_URI + "> is required before the subcontainer can be created.", false));
        } else if (testSubcontainer != null) {
            return ok(ApiUtil.createResponse("Test subcontainer <" + TEST_SUBCONTAINER_URI + "> already exists.", false));
        } else {
            testSubcontainer = new Subcontainer();
            testSubcontainer.setUri(TEST_SUBCONTAINER_URI);
            testSubcontainer.setLabel("Test Subcontainer");
            testSubcontainer.setTypeUri(VSTOI.SUBCONTAINER);
            testSubcontainer.setHascoTypeUri(VSTOI.SUBCONTAINER);
            testSubcontainer.setHasShortName("SUBCONTAINER TEST");
            testSubcontainer.setComment("This is a test subcontainer created to be added to the main Test Instrument.");
            testSubcontainer.setHasSIRManagerEmail("me@example.com");
            testSubcontainer.setNamedGraph(Constants.TEST_KB);

            return createSubcontainerResult(testSubcontainer);
        }
    }

    public Result createSubcontainer(String json) {
        if (json == null || json.equals("")) {
            return ok(ApiUtil.createResponse("No json content has been provided.", false));
        }
        //System.out.println("(SubcontainerAPI) Value of json in createSubcontainer: [" + json + "]");
        ObjectMapper objectMapper = new ObjectMapper();
        Subcontainer newInst;
        try {
            //convert json string to Subcontainer subcontainerance
            newInst  = objectMapper.readValue(json, Subcontainer.class);
        } catch (Exception e) {
            //System.out.println("(SubcontainerAPI) Failed to parse json for [" + json + "]");
            return ok(ApiUtil.createResponse("Failed to parse json.", false));
        }
        return createSubcontainerResult(newInst);
    }

    private Result deleteSubcontainerResult(Subcontainer subcontainer) {
        String uri = subcontainer.getUri();
        subcontainer.delete();
        return ok(ApiUtil.createResponse("Subcontainer <" + uri + "> has been DELETED.", true));
    }

    public Result deleteSubcontainerForTesting(){
        Subcontainer test;
        test = Subcontainer.find(TEST_SUBCONTAINER_URI);
        if (test == null) {
            return ok(ApiUtil.createResponse("There is no Test subcontainer to be deleted.", false));
        } else {
            test.setNamedGraph(Constants.TEST_KB);
            return deleteSubcontainerResult(test);
        }
    }

    public Result deleteSubcontainer(String uri){
        if (uri == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No subcontainer URI has been provided.", false));
        }
        Subcontainer subcontainer = Subcontainer.find(uri);
        if (subcontainer == null) {
            return ok(ApiUtil.createResponse("There is no subcontainer with URI <" + uri + "> to be deleted.", false));
        } else {
            return deleteSubcontainerResult(subcontainer);
        }
    }

    public static Result getSubcontainers(List<Subcontainer> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No subcontainer has been found", false));
        } else {
            ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL,VSTOI.SUBCONTAINER);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

    public Result toTextPlain(String uri) {
        if (uri  == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No URI has been provided", false));
        }
        String subcontainerText = Renderings.toString(uri, 80);
        if (subcontainerText == null || subcontainerText.equals("")) {
            return ok(ApiUtil.createResponse("No subcontainer has been found", false));
        } else {
            return ok(subcontainerText).as("text/plain");
        }
    }

    public Result toTextHTML(String uri) {
        if (uri  == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No URI has been provided", false));
        }
        String subcontainerText = Renderings.toHTML(uri, 80);
        if (subcontainerText == null || subcontainerText.equals("")) {
            return ok(ApiUtil.createResponse("No subcontainer has been found", false));
        } else {
            return ok(subcontainerText).as("text/html");
        }
    }

    public Result toTextPDF(String uri) {
        if (uri  == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No URI has been provided", false));
        }
        ByteArrayOutputStream subcontainerText = Renderings.toPDF(uri, 80);
        if (subcontainerText == null || subcontainerText.equals("")) {
            return ok(ApiUtil.createResponse("No subcontainer has been found", false));
        } else {
            return ok(subcontainerText.toByteArray()).as("application/pdf");
        }
    }

    public Result toFHIR(String uri) {
        if (uri  == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No URI has been provided", false));
        }
        Subcontainer subcontainerr = Subcontainer.find(uri);
        if (subcontainerr == null) {
            return ok(ApiUtil.createResponse("No subcontainer subcontainerance found for uri [" + uri + "]", false));
        }

        Questionnaire quest = new Questionnaire(subcontainerr);

        FhirContext ctx = FhirContext.forR4();
        IParser parser = ctx.newJsonParser();
        String serialized = parser.encodeResourceToString(quest.getFHIRObject());

        return ok(serialized).as("application/json");
    }

    public Result toRDF(String uri) {
        if (uri  == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No URI has been provided", false));
        }
        Subcontainer subcontainerr = Subcontainer.find(uri);
        if (subcontainerr == null) {
            return ok(ApiUtil.createResponse("No subcontainer subcontainerance found for uri [" + uri + "]", false));
        }

        String serialized = subcontainerr.printRDF();

        return ok(serialized).as("application/xml");
    }
}
