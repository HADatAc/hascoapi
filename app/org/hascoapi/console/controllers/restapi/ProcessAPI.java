package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.ContainerSlot;
import org.hascoapi.entity.pojo.Detector;
import org.hascoapi.entity.pojo.Instrument;
import org.hascoapi.entity.pojo.Process;
import org.hascoapi.entity.pojo.ProcessStem;
import org.hascoapi.entity.pojo.RequiredInstrumentation;
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
            //System.out.println("(createDetector) Failed to parse json.");
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

}
