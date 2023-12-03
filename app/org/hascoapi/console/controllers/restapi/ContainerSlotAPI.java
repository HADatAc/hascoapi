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
import org.hascoapi.entity.pojo.ContainerSlot;
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

public class ContainerSlotAPI extends Controller {

    /** 
     *   MAINTAINING CONTAINERS SLOTS
     */

    public Result createContainerSlots(String containerUri, String totContainerSlots) {
        if (containerUri == null || containerUri.isEmpty()) {
            return ok(ApiUtil.createResponse("Cannot create container slots without providing a container URI.", false));
        }
        Container container = Instrument.find(containerUri);
        if (container == null) {
            container = Subcontainer.find(containerUri);
        }
        return createContainerSlots(container, totContainerSlots);
    }

    public Result createContainerSlots(Container container, String totContainerSlots) {
        if (container == null) {
            return ok(ApiUtil.createResponse("Cannot create container slots on null container", false));
        }
        if (totContainerSlots == null || totContainerSlots.equals("")) {
            return ok(ApiUtil.createResponse("No total numbers of containerSlots to be created has been provided.", false));
        }
        int total = 0;
        try {
            total = Integer.parseInt(totContainerSlots);
        } catch (Exception e) {
            return ok(ApiUtil.createResponse("totContainerSlots is not a valid number of containerSlots.", false));
        }
        if (total <= 0) {
            return ok(ApiUtil.createResponse("Total numbers of containerSlots need to be greated than zero.", false));
        }
        if (container.createContainerSlots(total)) {
            return ok(ApiUtil.createResponse("A total of " + total + " containerSlots have been created for instrument/subContainer <" + container.getUri() + ">.", true));
        } else {
            return ok(ApiUtil.createResponse("Method failed to create containerSlots for instrument <" + container.getUri() + ">.", false));
        }
    }

    public Result deleteContainerSlots(String containerUri) {
        if (containerUri == null || containerUri.isEmpty()) {
            return ok(ApiUtil.createResponse("Cannot delete container slots without providing a container URI.", false));
        }
        Container container = Instrument.find(containerUri);
        if (container == null) {
            container = Subcontainer.find(containerUri);
        }
        return deleteContainerSlots(container);
    }

    public Result deleteContainerSlots(Container container) {
        if (container == null) {
            return ok(ApiUtil.createResponse("No container with provided URI has been found.", false));
        }
        if (container.getContainerSlots() == null) {
            return ok(ApiUtil.createResponse("Container has no containerSlot to be deleted.", false));
        }
        container.deleteContainerSlots();
        return ok(ApiUtil.createResponse("ContainerSlots for Container <" + container.getUri() + "> have been deleted.", true));
    }

    /** 
     *   TESTING CONTAINER SLOTS
     */

    public Result createContainerSlotsForTesting() {

        // VERIFY IF TEST INSTRUMENT AND TEST SUBCONTAINER EXIST
        Instrument testInstrument = Instrument.find(TEST_INSTRUMENT_URI);
        if (testInstrument == null) {
            return ok(ApiUtil.createResponse("Test instrument <" + TEST_INSTRUMENT_URI + "> needs to exist before its containerSlots can be created.", false));
        } 
        Subcontainer testSubcontainer = Subcontainer.find(TEST_SUBCONTAINER_URI);
        if (testSubcontainer == null) {
            return ok(ApiUtil.createResponse("Test subcontainer <" + TEST_SUBCONTAINER_URI + "> needs to exist before its containerSlots can be created.", false));
        } 
        
        // CREATE CONTAINER SLOTS
        if (testInstrument.getContainerSlots() != null && testInstrument.getContainerSlots().size() > 0) {
            return ok(ApiUtil.createResponse("Test instrument <" + TEST_INSTRUMENT_URI + "> already has containerSlots.", false));
        } 
        if (testSubcontainer.getContainerSlots() != null && testSubcontainer.getContainerSlots().size() > 0) {
            return ok(ApiUtil.createResponse("Test subcontainer <" + TEST_SUBCONTAINER_URI + "> already has containerSlots.", false));
        } 

        testInstrument.setNamedGraph(Constants.TEST_KB);
        testInstrument.createContainerSlots(TEST_INSTRUMENT_TOT_CONTAINER_SLOTS);
        testSubcontainer.setNamedGraph(Constants.TEST_KB);
        testSubcontainer.createContainerSlots(TEST_SUBCONTAINER_TOT_CONTAINER_SLOTS);
        
        return ok(ApiUtil.createResponse("Required containerSlots for testing containers have been created.", false));

    }

    public Result deleteContainerSlotsForTesting() {

        // VERIFY IF TEST INSTRUMENT AND TEST SUBCONTAINER EXIST 
        Instrument testInstrument = Instrument.find(TEST_INSTRUMENT_URI);
        if (testInstrument == null) {
            return ok(ApiUtil.createResponse("Test instrument <" + TEST_INSTRUMENT_URI + "> needs to exist before its containerSlots can be deleted.", false));
        } 
        Subcontainer testSubcontainer = Subcontainer.find(TEST_SUBCONTAINER_URI);
        if (testSubcontainer == null) {
            return ok(ApiUtil.createResponse("Test subcontainer <" + TEST_SUBCONTAINER_URI + "> needs to exist before its containerSlots can be deleted.", false));
        }
        
        // DELETE EXISTING CONTAINER SLOTS 
        if (testInstrument.getContainerSlots() == null || testInstrument.getContainerSlots().size() == 0) {
            return ok(ApiUtil.createResponse("Test instrument <" + TEST_INSTRUMENT_URI + "> has no containerSlots to be deleted.", false));
        } 
        if (testSubcontainer.getContainerSlots() == null || testSubcontainer.getContainerSlots().size() == 0) {
            return ok(ApiUtil.createResponse("Test subcontainer <" + TEST_SUBCONTAINER_URI + "> has no containerSlots to be deleted.", false));
        } 
        
        testInstrument.setNamedGraph(Constants.TEST_KB);
        testInstrument.deleteContainerSlots();
        testSubcontainer.setNamedGraph(Constants.TEST_KB);
        testSubcontainer.deleteContainerSlots();
        
        return ok(ApiUtil.createResponse("Existing containerSlots for testing containers have been deleted.", false));

    }

    /** 
     *   QUERYING CONTAINERS SLOTS
     */

    public Result getAllContainerSlots(){
        List<ContainerSlot> results = ContainerSlot.find();
        return getContainerSlots(results);
    }

    public Result getContainerSlotsByContainer(String containerUri){
        List<ContainerSlot> results = ContainerSlot.findByContainer(containerUri);
        return getContainerSlots(results);
    }

    public static Result getContainerSlots(List<ContainerSlot> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No containerSlot has been found", false));
        } else {
            ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL,VSTOI.CONTAINER_SLOT);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

}
