package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import org.hascoapi.Constants;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.HAScOMapper;
import org.hascoapi.vocabularies.VSTOI;
import org.hascoapi.entity.pojo.Task;
import org.hascoapi.entity.pojo.RequiredInstrumentation;
import play.mvc.Http;
import play.mvc.Controller;
import play.mvc.Result;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class TaskAPI extends Controller {

    public static Result getTasks(List<Task> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No Task has been found", false));
        } else {
            ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL,VSTOI.TASK);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

    public Result setRequiredInstrumentation(Http.Request request) {
        // Get the JSON body from the request
        JsonNode json = request.body().asJson();

        if (json == null) {
            return badRequest("Expecting JSON data");
        }

        // Extract the "taskuri" from the JSON body
        String taskuri = json.path("taskuri").asText();

        if (taskuri.isEmpty()) {
            return badRequest("Missing parameter: taskuri");
        }

        System.out.println("TaskAPI: will read task");
        Task task = Task.find(taskuri);
        System.out.println("TaskAPI: read task");

        if (task == null) {
            return ok(ApiUtil.createResponse("Task with URI <" + taskuri + "> could not be found.", false));
        } 

        // Extract the "requiredInstrumentation" array from the JSON body
        JsonNode requiredInstrumentationNode = json.path("requiredInstrumentation");

        if (!requiredInstrumentationNode.isArray()) {
            return badRequest("Missing or invalid parameter: requiredInstrumentation");
        }

        List<String> instrumentUris = new ArrayList<>();
        Map<String, List<String>> instrumentDetectors = new HashMap<>();

        for (JsonNode node : requiredInstrumentationNode) {
            String instrumentUri = node.path("instrumentUri").asText();
            
            if (instrumentUri.isEmpty()) {
                return badRequest("Each requiredInstrumentation entry must have an instrumentUri");
            }

            System.out.println("   instrument uri: <" + instrumentUri + ">");
            instrumentUris.add(instrumentUri);

            // Extract detectors
            JsonNode detectorsNode = node.path("detectors");
            List<String> detectors = new ArrayList<>();

            if (detectorsNode.isArray()) {
                for (JsonNode detectorNode : detectorsNode) {
                    detectors.add(detectorNode.asText());
                }
            }

            instrumentDetectors.put(instrumentUri, detectors);
        }

        System.out.println("Total instruments: <" + instrumentUris.size() + ">");
        System.out.println("Instrument-Detectors Mapping: " + instrumentDetectors);


        if (instrumentUris.isEmpty()) {
            return badRequest("List of instruments is empty");
        }

        // Save instruments to task
        int aux = 0;
        List<String> listRequiredInstrumentationUri = new ArrayList<String>();
        for (String instrumentUri : instrumentUris) {
            
            String rinUri = taskuri.replaceAll(
                Constants.PREFIX_PROCESS,
                Constants.PREFIX_REQUIRED_INSTRUMENTATION) + 
                "/" + aux++;    
            try {
                RequiredInstrumentation requiredInstrumentation = new RequiredInstrumentation();
                requiredInstrumentation.setUri(rinUri);
                requiredInstrumentation.setUsesInstrument(instrumentUri);
                requiredInstrumentation.setHasRequiredDetector(instrumentDetectors.get(instrumentUri));
                requiredInstrumentation.save();
                listRequiredInstrumentationUri.add(rinUri);
            } catch (Exception e) {
                e.printStackTrace();
                return badRequest("Error processing Required Instrumentation ");
            }
        }
        System.out.println("TaskAPI: will add required instrumentation");
        task.setHasRequiredInstrumentationUris(listRequiredInstrumentationUri);
        System.out.println("TaskAPI: added required instrumentation");

        System.out.println("taskAPI: will save process");
        try {
            task.save();
        } catch (Exception e) {
            e.printStackTrace();
            return internalServerError("Error saving task: " + e.getMessage());
        }
        System.out.println("TaskAPI: saved task");

        return ok("Received taskuri: " + taskuri + ", instrumenturis: " + instrumentUris);
    }

}
