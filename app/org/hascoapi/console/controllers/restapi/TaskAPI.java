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
import org.hascoapi.entity.pojo.RequiredComponent;
import org.hascoapi.entity.pojo.RequiredInstrument;
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

    public Result setRequiredComponents(Http.Request request) {
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

        // Extract the "requiredInstrument" array from the JSON body
        JsonNode requiredComponentNode = json.path("requiredComponent");

        if (!requiredComponentNode.isArray()) {
            return badRequest("Missing or invalid parameter: requiredComponent");
        }

        int aux = 0;

        List<RequiredComponent> requiredComponents = new ArrayList<RequiredComponent>();

        for (JsonNode node : requiredComponentNode) {

            // extract component uri
            String componentUri = node.path("componentUri").asText();
            if (componentUri.isEmpty()) {
                return badRequest("Each requiredComponent entry must have a componentUri");
            }
            System.out.println("   component uri: <" + componentUri + ">");

            // extract container slot uri
            String containerSlotUri = node.path("containerSlotUri").asText();
            if (containerSlotUri.isEmpty()) {
                return badRequest("Each requiredComponent entry must have a containerSlotUri");
            }
            System.out.println("   container slot uri: <" + containerSlotUri + ">");

            String reqUri = taskuri + 
                Constants.PREFIX_REQUIRED_COMPONENT + 
                "/" + aux++;    
            try {
                RequiredComponent requiredComponent = new RequiredComponent();
                requiredComponent.setUri(reqUri);
                requiredComponent.setUsesComponent(componentUri);
                requiredComponent.setHasContainerSlot(containerSlotUri);
                requiredComponents.add(requiredComponent);
            } catch (Exception e) {
                e.printStackTrace();
                return badRequest("Error processing Required Component ");
            }
        }

        System.out.println("TaskAPI: will save a total of " + requiredComponents.size() + " required components.");
        for (RequiredComponent requiredComponent : requiredComponents) {
            requiredComponent.save();
        }
        System.out.println("TaskAPI: added required instrumentation");

        System.out.println("taskAPI: will save process");
        try {
            task.save();
        } catch (Exception e) {
            e.printStackTrace();
            return internalServerError("Error saving task: " + e.getMessage());
        }
        System.out.println("TaskAPI: saved task");

        return ok("Saved requiredComponents for taskuri: " + taskuri);
    }

    public Result setRequiredInstruments(Http.Request request) {
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

        // Extract the "requiredInstrument" array from the JSON body
        JsonNode requiredInstrumentNode = json.path("requiredInstrument");

        if (!requiredInstrumentNode.isArray()) {
            return badRequest("Missing or invalid parameter: requiredInstrument");
        }

        List<String> instrumentUris = new ArrayList<>();
        Map<String, List<String>> instrumentComponents = new HashMap<>();

        for (JsonNode node : requiredInstrumentNode) {
            String instrumentUri = node.path("instrumentUri").asText();
            
            if (instrumentUri.isEmpty()) {
                return badRequest("Each requiredInstrument entry must have an instrumentUri");
            }

            System.out.println("   instrument uri: <" + instrumentUri + ">");
            instrumentUris.add(instrumentUri);

            // Extract detectors
            JsonNode requiredComponentsNode = node.path("requiredComponents");
            List<String> requiredComponents = new ArrayList<>();

            if (requiredComponentsNode.isArray()) {
                for (JsonNode requiredComponentNode : requiredComponentsNode) {
                    String requiredComponentUri = node.path("requireComponentUri").asText();
                    
                    if (requiredComponentUri.isEmpty()) {
                        return badRequest("Each requiredComponent entry must have a requiredComponentUri");
                    }

                    System.out.println("   required component uri: <" + requiredComponentUri + ">");

                    requiredComponents.add(requiredComponentUri);
                }
            }

            instrumentComponents.put(instrumentUri, requiredComponents);
        }

        System.out.println("Total instruments: <" + instrumentUris.size() + ">");


        if (instrumentUris.isEmpty()) {
            return badRequest("List of instruments is empty");
        }

        // Save instruments to task
        int aux = 0;
        List<String> listRequiredInstrumentUri = new ArrayList<String>();
        for (String instrumentUri : instrumentUris) {
            
            String rinUri = taskuri +
                Constants.PREFIX_REQUIRED_INSTRUMENT + 
                "/" + aux++;    
            try {
                RequiredInstrument requiredInstrument = new RequiredInstrument();
                requiredInstrument.setUri(rinUri);
                requiredInstrument.setUsesInstrument(instrumentUri);
                requiredInstrument.setHasRequiredComponent(instrumentComponents.get(instrumentUri));
                requiredInstrument.save();
                listRequiredInstrumentUri.add(rinUri);
            } catch (Exception e) {
                e.printStackTrace();
                return badRequest("Error processing Required Instrumentation ");
            }
        }
        System.out.println("TaskAPI: will add required instrumentation");
        task.setHasRequiredInstrumentUris(listRequiredInstrumentUri);
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
