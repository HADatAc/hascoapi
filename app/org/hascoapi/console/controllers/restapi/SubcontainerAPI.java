package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import java.util.ArrayList;
import java.util.List;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.Container;
import org.hascoapi.entity.pojo.Instrument;
import org.hascoapi.entity.pojo.Subcontainer;
import org.hascoapi.entity.pojo.SlotOperations;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.HAScOMapper;
import org.hascoapi.vocabularies.VSTOI;
import play.mvc.Controller;
import play.mvc.Result;

import static org.hascoapi.Constants.*;

public class SubcontainerAPI extends Controller {

    /** 
     *   MAINTAINING SUBCONTAINERS
     */

    private Result createSubcontainerResult(Subcontainer subcontainer) {
        if (subcontainer.saveAndAttach()) {
            return ok(ApiUtil.createResponse("Subcontainer <" + subcontainer.getUri() + "> has been CREATED.", true));
        } else {
            return ok(ApiUtil.createResponse("Subcontainer <" + subcontainer.getUri() + "> FAILED to be created.", false));
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

    /** 
    private Result deleteSubcontainerResult(Subcontainer subcontainer) {
        String uri = subcontainer.getUri();
        if (subcontainer.deleteAndDetach()) {
           return ok(ApiUtil.createResponse("Subcontainer <" + uri + "> has been DELETED.", true));
        } else {
           return ok(ApiUtil.createResponse("Subcontainer <" + uri + "> FAILED to be deleted.", false));
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
    */

    /** 
     *   TESTING SUBCONTAINERS
     */

    public Result createSubcontainerForTesting() {
        Instrument testInstrument = Instrument.find(TEST_INSTRUMENT_URI);
        Subcontainer testSubcontainer1 = Subcontainer.find(TEST_SUBCONTAINER1_URI);
        Subcontainer testSubcontainer2 = Subcontainer.find(TEST_SUBCONTAINER2_URI);
        if (testInstrument == null) {
            return ok(ApiUtil.createResponse("Test instrument <" + TEST_INSTRUMENT_URI + "> is required before the subcontainer can be created.", false));
        } else if (testSubcontainer1 != null) {
            return ok(ApiUtil.createResponse("Test subcontainer <" + TEST_SUBCONTAINER1_URI + "> already exists.", false));
        } else if (testSubcontainer2 != null) {
            return ok(ApiUtil.createResponse("Test subcontainer <" + TEST_SUBCONTAINER2_URI + "> already exists.", false));
        } else {

            // Insert list of subcontainers in the reverse order

            testSubcontainer2 = new Subcontainer();
            testSubcontainer2.setUri(TEST_SUBCONTAINER2_URI);
            testSubcontainer2.setBelongsTo(TEST_INSTRUMENT_URI);
            testSubcontainer2.setLabel("Test Subcontainer 2");
            testSubcontainer2.setTypeUri(VSTOI.SUBCONTAINER);
            testSubcontainer2.setHascoTypeUri(VSTOI.SUBCONTAINER);
            testSubcontainer2.setHasShortName("SUBCONTAINER TEST 2");
            testSubcontainer2.setComment("Test subcontainer 2 to be added to the main Test Instrument.");
            testSubcontainer2.setHasSIRManagerEmail("me@example.com");
            testSubcontainer2.setNamedGraph(Constants.TEST_KB);
            if (!testSubcontainer2.saveAndAttach()) {
                return ok(ApiUtil.createResponse("Failed to create Subcontainers 2.", false));
            }

            testSubcontainer1 = new Subcontainer();
            testSubcontainer1.setUri(TEST_SUBCONTAINER1_URI);
            testSubcontainer1.setBelongsTo(TEST_INSTRUMENT_URI);
            testSubcontainer1.setLabel("Test Subcontainer 1");
            testSubcontainer1.setTypeUri(VSTOI.SUBCONTAINER);
            testSubcontainer1.setHascoTypeUri(VSTOI.SUBCONTAINER);
            testSubcontainer1.setHasShortName("SUBCONTAINER TEST 1");
            testSubcontainer1.setComment("Test subcontainer 1 to be added to the main Test Instrument.");
            testSubcontainer1.setHasSIRManagerEmail("me@example.com");
            testSubcontainer1.setNamedGraph(Constants.TEST_KB);
            if (testSubcontainer1.saveAndAttach()) {
                return ok(ApiUtil.createResponse("Subcontainers 1 and 2 has been CREATED.", true));
            } else {
                return ok(ApiUtil.createResponse("Failed to create Subcontainers 1.", false));
            }
        }
    }

    public Result deleteSubcontainerForTesting(){
        Subcontainer testSubcontainer1 = Subcontainer.find(TEST_SUBCONTAINER1_URI);
        Subcontainer testSubcontainer2 = Subcontainer.find(TEST_SUBCONTAINER2_URI);
        String msg = "";
        if (testSubcontainer1 == null) {
            msg += "No Test subcontainer 1 to be deleted. ";
        } else {
            SlotOperations.deleteSlotElement(TEST_SUBCONTAINER1_URI);
            //testSubcontainer1.setNamedGraph(Constants.TEST_KB);
            //testSubcontainer1.deleteAndDetach();
        } 
        if (testSubcontainer2 == null) {
            msg += "No Test subcontainer 2 to be deleted. ";
        } else {
            SlotOperations.deleteSlotElement(TEST_SUBCONTAINER2_URI);
            //testSubcontainer2.setNamedGraph(Constants.TEST_KB);
            //testSubcontainer2.deleteAndDetach();
        }
        if (msg.isEmpty()) {
            return ok(ApiUtil.createResponse("Subcontainers 1 and 2 has been DELETED.", true));
        } else {
            return ok(ApiUtil.createResponse(msg, false));
        }
    }

    /**
     *   QUERYING SUBCONTAINER
     */
 
    public static Result getSubcontainers(List<Subcontainer> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No subcontainer has been found", false));
        } else {
            ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL,VSTOI.SUBCONTAINER);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

}
