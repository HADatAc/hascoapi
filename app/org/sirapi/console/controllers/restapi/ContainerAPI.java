package org.sirapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

import org.sirapi.entity.fhir.Questionnaire;
import org.sirapi.entity.pojo.Container;
import org.sirapi.entity.pojo.Instrument;
import org.sirapi.entity.pojo.SubContainer;
import org.sirapi.transform.Renderings;
import org.sirapi.utils.ApiUtil;
import org.sirapi.vocabularies.VSTOI;
import play.mvc.Controller;
import play.mvc.Result;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.sirapi.Constants.TEST_INSTRUMENT_URI;
import static org.sirapi.Constants.TEST_INSTRUMENT_TOT_DETECTOR_SLOTS;

public class ContainerAPI extends Controller {

    public Result createDetectorSlots(String containerUri, String totDetectorSlots) {
        if (containerUri == null || containerUri.equals("")) {
            return ok(ApiUtil.createResponse("No instrument URI or subContainer URI has been provided.", false));
        }
        Container container = (Container)Instrument.find(containerUri);
        if (container == null) {
            container = (Container)SubContainer.find(containerUri);
            if (container == null) {
                return ok(ApiUtil.createResponse("No instrument or subContainer with provided URI has been found.", false));
            }
        }
        if (container.getDetectorSlots() != null) {
            return ok(ApiUtil.createResponse("Instrument/SubContainer already has detectorSlots. Delete existing detectorSlots before creating new detectorSlots", false));
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
        if (container.createDetectorSlots(total)) {
            return ok(ApiUtil.createResponse("A total of " + total + " detectorSlots have been created for instrument/subContainer <" + containerUri + ">.", true));
        } else {
            return ok(ApiUtil.createResponse("Method failed to create detectorSlots for instrument <" + containerUri + ">.", false));
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

}
