package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.ContainerSlot;
import org.hascoapi.entity.pojo.ActuatorStem;
import org.hascoapi.entity.pojo.Instrument;
import org.hascoapi.entity.pojo.SemanticVariable;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.HAScOMapper;
import org.hascoapi.vocabularies.VSTOI;

import play.mvc.Controller;
import play.mvc.Result;
import static org.hascoapi.Constants.*;
import java.util.List;


public class ActuatorStemAPI extends Controller {

    private Result createActuatorStemResult(ActuatorStem actuatorStem) {
        actuatorStem.save();
        return ok(ApiUtil.createResponse("ActuatorStem <" + actuatorStem.getUri() + "> has been CREATED.", true));
    }

    public Result createActuatorStem(String json) {
        if (json == null || json.equals("")) {
            return ok(ApiUtil.createResponse("No json content has been provided.", false));
        }
        //System.out.println("(CreateActuatorStem) Value of json: [" + json + "]");
        ObjectMapper objectMapper = new ObjectMapper();
        ActuatorStem newActuatorStem;
        try {
            //convert json string to Instrument instance
            newActuatorStem  = objectMapper.readValue(json, ActuatorStem.class);
        } catch (Exception e) {
            //System.out.println("(createActuator) Failed to parse json.");
            return ok(ApiUtil.createResponse("Failed to parse json.", false));
        }
        return createActuatorStemResult(newActuatorStem);
    }

    private Result deleteActuatorStemResult(ActuatorStem actuatorStem) {
        String uri = actuatorStem.getUri();
        actuatorStem.delete();
        return ok(ApiUtil.createResponse("Actuator Stem <" + uri + "> has been DELETED.", true));
    }

    public Result deleteActuatorStem(String uri){
        if (uri == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No actuator setm URI has been provided.", false));
        }
        ActuatorStem actuatorStem = ActuatorStem.find(uri);
        if (actuatorStem == null) {
            return ok(ApiUtil.createResponse("There is no actuator stem with URI <" + uri + "> to be deleted.", false));
        } else {
            return deleteActuatorStemResult(actuatorStem);
        }
    }

    public Result getActuatorStemsByInstrument(String instrumentUri){
        List<ActuatorStem> results = ActuatorStem.findByInstrument(instrumentUri);
        return getActuatorStems(results);
    }

    public static Result getActuatorStems(List<ActuatorStem> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No actuator stem has been found", false));
        } else {
            ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL,VSTOI.ACTUATOR_STEM);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

}
