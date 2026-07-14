package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.ContainerSlot;
import org.hascoapi.entity.pojo.Component;
import org.hascoapi.entity.pojo.Instrument;
import org.hascoapi.entity.pojo.Process;
import org.hascoapi.entity.pojo.ProcessStem;
import org.hascoapi.entity.pojo.RequiredInstrument;
import org.hascoapi.entity.pojo.Task;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.HAScOMapper;
import org.hascoapi.vocabularies.VSTOI;

import play.mvc.Http;
import play.mvc.Controller;
import play.mvc.Result;
import static org.hascoapi.Constants.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;


public class ProcessAPI extends Controller {

    private Result createProcessStemResult(ProcessStem processStem) {
        processStem.save();
        return ok(ApiUtil.createResponse("ProcessStem <" + processStem.getUri() + "> has been CREATED.", true));
    }

    public Result createProcessStem(String json) {
        if (json == null || json.equals("")) {
            return ok(ApiUtil.createResponse("No json content has been provided.", false));
        }
        //System.out.println("(CreateProcess) Value of json: [" + json + "]");
        ObjectMapper objectMapper = new ObjectMapper();
        ProcessStem newProcessStem;
        try {
            //convert json string to Instrument instance
            newProcessStem  = objectMapper.readValue(json, ProcessStem.class);
        } catch (Exception e) {
            //System.out.println("(createComponent) Failed to parse json.");
            return ok(ApiUtil.createResponse("Failed to parse json.", false));
        }
        return createProcessStemResult(newProcessStem);
    }

    private Result deleteProcessStemResult(ProcessStem processStem) {
        String uri = processStem.getUri();
        processStem.delete();
        return ok(ApiUtil.createResponse("ProcessStem <" + uri + "> has been DELETED.", true));
    }

    public Result deleteProcessStem(String uri){
        if (uri == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No processStem URI has been provided.", false));
        }
        ProcessStem processStem = ProcessStem.find(uri);
        if (processStem == null) {
            return ok(ApiUtil.createResponse("There is no processStem with URI <" + uri + "> to be deleted.", false));
        } else {
            return deleteProcessStemResult(processStem);
        }
    }

    public static Result getProcesses(List<org.hascoapi.entity.pojo.Process> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No process has been found", false));
        } else {
            ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL,VSTOI.PROCESS);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

    public static Result getProcessStems(List<ProcessStem> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No process stem has been found", false));
        } else {
            ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL,VSTOI.PROCESS_STEM);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

    public Result deleteWithTasks(String uri) {
        //System.out.println("Delete element => Type: [" + elementType + "]  URI [" + uri + "]");
        if (uri == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No uri has been provided.", false));
        }
        Process process = Process.find(uri);
        if (process == null) {
            return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
        }
        process.deleteWithTasks();
        return ok(ApiUtil.createResponse("PROCESS with URI [" + uri + "] has been deleted along with its TASKS", true));
    }

    /**
     * Returns all tasks for a process in one call as a flat list.
     * This avoids N+1 URI walking in clients and keeps field names stable.
     */
    public Result getTasksByProcess(String processUri) {
        if (processUri == null || processUri.trim().isEmpty()) {
            return ok(ApiUtil.createResponse("No process URI has been provided.", false));
        }

        Process process = Process.find(processUri);
        if (process == null) {
            return ok(ApiUtil.createResponse("No process with URI [" + processUri + "] has been found", false));
        }

        String topTaskUri = process.getHasTopTaskUri();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode response = mapper.createObjectNode();
        response.put("processUri", processUri);
        response.put("topTaskUri", topTaskUri == null ? "" : topTaskUri);

        ArrayNode tasksArray = mapper.createArrayNode();
        response.set("tasks", tasksArray);

        if (topTaskUri == null || topTaskUri.trim().isEmpty()) {
            return ok(ApiUtil.createResponse(response, true));
        }

        // Traverse task graph once and avoid cycles/repeated nodes.
        Set<String> visited = new HashSet<String>();
        ArrayDeque<String> queue = new ArrayDeque<String>();
        queue.add(topTaskUri);

        while (!queue.isEmpty()) {
            String taskUri = queue.poll();
            if (taskUri == null || taskUri.trim().isEmpty() || visited.contains(taskUri)) {
                continue;
            }
            visited.add(taskUri);

            Task task = Task.find(taskUri);
            if (task == null) {
                continue;
            }

            ObjectNode taskNode = mapper.createObjectNode();
            taskNode.put("uri", task.getUri() == null ? "" : task.getUri());
            taskNode.put("label", task.getLabel() == null ? "" : task.getLabel());
            taskNode.put("typeUri", task.getTypeUri() == null ? "" : task.getTypeUri());
            taskNode.put("hascoTypeUri", task.getHascoTypeUri() == null ? "" : task.getHascoTypeUri());
            taskNode.put("hasStatus", task.getHasStatus() == null ? "" : task.getHasStatus());
            taskNode.put("hasSupertaskUri", task.getHasSupertaskUri() == null ? "" : task.getHasSupertaskUri());
            taskNode.put("hasTemporalDependency", task.getHasTemporalDependency() == null ? "" : task.getHasTemporalDependency());
            taskNode.put("hasIterationConstraint", task.getHasIterationConstraint() == null ? "" : task.getHasIterationConstraint());
            taskNode.put("supportsObjective", task.getSupportsObjective() == null ? "" : task.getSupportsObjective());

            ArrayNode subtaskUrisNode = mapper.createArrayNode();
            List<String> subtaskUris = task.getHasSubtaskUris();
            if (subtaskUris != null) {
                for (String subUri : subtaskUris) {
                    if (subUri != null && !subUri.trim().isEmpty()) {
                        subtaskUrisNode.add(subUri);
                        if (!visited.contains(subUri)) {
                            queue.add(subUri);
                        }
                    }
                }
            }
            taskNode.set("hasSubtaskUris", subtaskUrisNode);

            ArrayNode reqInstUrisNode = mapper.createArrayNode();
            List<String> reqInstUris = task.getHasRequiredInstrumentUris();
            if (reqInstUris != null) {
                for (String reqUri : reqInstUris) {
                    if (reqUri != null && !reqUri.trim().isEmpty()) {
                        reqInstUrisNode.add(reqUri);
                    }
                }
            }
            taskNode.set("hasRequiredInstrumentUris", reqInstUrisNode);

            tasksArray.add(taskNode);
        }

        return ok(ApiUtil.createResponse(response, true));
    }

}
