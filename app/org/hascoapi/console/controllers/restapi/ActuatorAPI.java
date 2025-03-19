package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.ContainerSlot;
import org.hascoapi.entity.pojo.ActuatorStem;
import org.hascoapi.entity.pojo.Actuator;
import org.hascoapi.entity.pojo.Instrument;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.HAScOMapper;
import org.hascoapi.vocabularies.VSTOI;

import play.mvc.Controller;
import play.mvc.Result;
import static org.hascoapi.Constants.*;
import java.util.List;


public class ActuatorAPI extends Controller {

    /** 
     *   MAINTAINING ACTUATORS
     */

    private Result createActuatorResult(Actuator actuator) {
        actuator.save();
        return ok(ApiUtil.createResponse("Actuator <" + actuator.getUri() + "> has been CREATED.", true));
    }

    public Result createActuator(String json) {
        if (json == null || json.equals("")) {
            return ok(ApiUtil.createResponse("No json content has been provided.", false));
        }
        //System.out.println("(CreateActuator) Value of json: [" + json + "]");
        ObjectMapper objectMapper = new ObjectMapper();
        Actuator newActuator;
        try {
            //convert json string to Container instance
            newActuator  = objectMapper.readValue(json, Actuator.class);
        } catch (Exception e) {
            //System.out.println("(createActuator) Failed to parse json.");
            return ok(ApiUtil.createResponse("Failed to parse json.", false));
        }
        return createActuatorResult(newActuator);
    }

    private Result deleteActuatorResult(Actuator actuator) {
        String uri = actuator.getUri();
        actuator.delete();
        return ok(ApiUtil.createResponse("Actuator <" + uri + "> has been DELETED.", true));
    }

    public Result deleteActuator(String uri){
        if (uri == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No actuator URI has been provided.", false));
        }
        Actuator actuator = Actuator.find(uri);
        if (actuator == null) {
            return ok(ApiUtil.createResponse("There is no actuator with URI <" + uri + "> to be deleted.", false));
        } else {
            return deleteActuatorResult(actuator);
        }
    }

    /*** 
     *  QUERYING ACTUATORS
     */


    public Result getActuatorsByContainer(String instrumentUri){
        List<Actuator> results = Actuator.findActuatorsByContainer(instrumentUri);
        return getActuators(results);
    }

    public static Result getActuators(List<Actuator> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No actuator has been found", false));
        } else {
            ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL,VSTOI.ACTUATOR);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            //System.out.println("DetecttorAPI: [" + ApiUtil.createResponse(jsonObject, true) + "]");
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

    public Result getUsage(String actuatorUri){
        List<ContainerSlot> results = Actuator.usage(actuatorUri);
        return ContainerSlotAPI.getContainerSlots(results);
    }

}
