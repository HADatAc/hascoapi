package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.HAScOMapper;
import org.hascoapi.vocabularies.VSTOI;
import org.hascoapi.entity.pojo.Task;
import play.mvc.Http;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.List;
import java.util.ArrayList;

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

    public Result setUsesComponentInstances(Http.Request request) {
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

        Task task = Task.find(taskuri);

        if (task == null) {
            return ok(ApiUtil.createResponse("Task with URI <" + taskuri + "> could not be found.", false));
        }

        // Extract the "usesComponentInstance" array from the JSON body
        JsonNode usesComponentInstanceNode = json.path("usesComponentInstance");

        if (!usesComponentInstanceNode.isArray()) {
            return badRequest("Missing or invalid parameter: usesComponentInstance");
        }

        List<String> componentInstanceUris = new ArrayList<String>();
        for (JsonNode node : usesComponentInstanceNode) {
            String componentInstanceUri = node == null ? "" : node.asText("").trim();
            if (componentInstanceUri.isEmpty()) {
                return badRequest("Each usesComponentInstance entry must be a non-empty URI");
            }
            componentInstanceUris.add(componentInstanceUri);
        }

        task.setUsesComponentInstanceUris(componentInstanceUris);

        try {
            task.save();
        } catch (Exception e) {
            e.printStackTrace();
            return internalServerError("Error saving task: " + e.getMessage());
        }

        return ok("Received component instance URI(s) for taskuri: [" + taskuri + "].");
    }

}
