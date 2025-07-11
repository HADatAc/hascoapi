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

    public Result deleteWithTasks(String uri) {
        //System.out.println("Delete element => Type: [" + elementType + "]  URI [" + uri + "]");
        if (uri == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No uri has been provided.", false));
        }
        Task task = Task.find(uri);
        if (task == null) {
            return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
        }
        Task.deleteWithSubtasks(task);
        return ok(ApiUtil.createResponse("PROCESS with URI [" + uri + "] has been deleted along with its TASKS", true));
    }

    public Result setRequiredInstruments(Http.Request request) {
        // Get the JSON body from the request
        JsonNode json = request.body().asJson();
        System.out.println(json);

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

        List<RequiredInstrument> riList = new ArrayList<RequiredInstrument>();

        for (JsonNode node : requiredInstrumentNode) {
            RequiredInstrument riTmp = new RequiredInstrument();

            riTmp.setUsesInstrument(node.path("instrumentUri").asText());
            
            if (riTmp.getUsesInstrument().isEmpty()) {
                return badRequest("Each requiredInstrument entry must have an instrumentUri");
            }

            System.out.println("   instrument uri: <" + riTmp.getUsesInstrument() + ">");

            // Extract components
            JsonNode requiredComponentsNode = node.path("requiredComponents");

            if (requiredComponentsNode.isArray()) {
                for (JsonNode requiredComponentNode : requiredComponentsNode) {
                    RequiredComponent rcTmp = new RequiredComponent();

                    String requiredComponentUri = node.path("requireComponentUri").asText();
                    
                    if (requiredComponentUri.isEmpty()) {
                        return badRequest("Each requiredComponent entry must have a requiredComponentUri");
                    }

                    System.out.println("   required component uri: <" + requiredComponentUri + ">");

                    rcTmp.setUri(requiredComponentUri);

                    rcTmp.setUsesComponent(node.path("componentUri").asText());
            
                    if (rcTmp.getUsesComponent().isEmpty()) {
                        return badRequest("Each requiredComponent entry must have a componentUri");
                    }
        
                    System.out.println("   component uri: <" + rcTmp.getUsesComponent() + ">");

                    rcTmp.setHasContainerSlot(node.path("containerSlotUri").asText());
            
                    if (rcTmp.getHasContainerSlot().isEmpty()) {
                        return badRequest("Each requiredComponent entry must have a containerSlotUri");
                    }
        
                    System.out.println("   container slot uri: <" + rcTmp.getHasContainerSlot() + ">");

                    rcTmp.save();
                    riTmp.addHasRequiredComponent(rcTmp.getUri());
                }
            }

            riList.add(riTmp);
        }

        System.out.println("Total instruments: <" + riList.size() + ">");


        if (riList.isEmpty()) {
            return badRequest("List of instruments is empty");
        }

        // Save instruments to task
        int aux = 0;
        List<String> listRi = new ArrayList<String>();
        for (RequiredInstrument ri : riList) {
            
            ri.setUri(taskuri +
                "/" + Constants.PREFIX_REQUIRED_INSTRUMENT + 
                "/" + aux++);    
            try {
                listRi.add(ri.getUri());
                ri.setTypeUri(VSTOI.REQUIRED_INSTRUMENT);
                ri.setHascoTypeUri(VSTOI.REQUIRED_INSTRUMENT);
                ri.save();
            } catch (Exception e) {
                e.printStackTrace();
                return badRequest("Error processing Required Instrumentation ");
            }
        }
        System.out.println("TaskAPI: will add required instrumentation");
        task.setHasRequiredInstrumentUris(listRi);
        System.out.println("TaskAPI: added required instrumentation");

        System.out.println("taskAPI: will save process");
        try {
            task.save();
        } catch (Exception e) {
            e.printStackTrace();
            return internalServerError("Error saving task: " + e.getMessage());
        }
        System.out.println("TaskAPI: saved task");

        return ok("Received required instrument(s) for taskuri: [" + taskuri + "].");
    }

}
