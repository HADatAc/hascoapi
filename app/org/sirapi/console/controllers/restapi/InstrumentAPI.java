package org.sirapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.sirapi.entity.pojo.Instrument;
import org.sirapi.utils.ApiUtil;
import org.sirapi.vocabularies.VSTOI;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.List;

public class InstrumentAPI extends Controller {

    private final String SIR_KB = "http://hadatac.org/kb/test/";
    private final String TEST_INSTRUMENT_URI = SIR_KB + "TestInstrument";

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
            testInstrument.setComment("This is a dummy instrument created to test the SIR API.");
            return createInstrumentResult(testInstrument);
        }
    }

    public Result createInstrument(String json) {
        if (json == null || json.equals("")) {
            return ok(ApiUtil.createResponse("No json content has been provided.", false));
        }
        System.out.println("Value of json: [" + json + "]");
        ObjectMapper objectMapper = new ObjectMapper();
        Instrument newInst;
        try {
            //convert json string to Instrument instance
            newInst  = objectMapper.readValue(json, Instrument.class);
        } catch (Exception e) {
            return ok(ApiUtil.createResponse("Failed to parse json.", false));
        }
        return createInstrumentResult(newInst);
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

    public Result getAllInstruments(){
        ObjectMapper mapper = new ObjectMapper();

        List<Instrument> results = Instrument.find();
        if (results == null) {
            return notFound(ApiUtil.createResponse("No instrument has been found", false));
        } else {
            //SimpleFilterProvider filterProvider = new SimpleFilterProvider();
            //filterProvider.addFilter("instrumentFilter",
            //        SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri", "hascoTypeLabel", "comment"));
            //mapper.setFilterProvider(filterProvider);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

}
