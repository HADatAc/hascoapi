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

    public Result setRequiredInstrumentation(Http.Request request) {
        // Get the JSON body from the request
        JsonNode json = request.body().asJson();

        if (json == null) {
            return badRequest("Expecting JSON data");
        }

        // Extract the "processuri" from the JSON body
        String processuri = json.path("processuri").asText();

        if (processuri.isEmpty()) {
            return badRequest("Missing parameter: processuri");
        }

        System.out.println("ProcessAPI: will read process");
        Process process = Process.find(processuri);
        System.out.println("ProcessAPI: read process");

        if (process == null) {
            return ok(ApiUtil.createResponse("Process with URI <" + processuri + "> could not be found.", false));
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

        // Save instruments to process
        int aux = 0;
        List<String> listRequiredInstrumentationUri = new ArrayList<String>();
        for (String instrumentUri : instrumentUris) {
            
            String rinUri = processuri.replaceAll(
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
        System.out.println("ProcessAPI: will add required instrumentation");
        process.setHasRequiredInstrumentationUris(listRequiredInstrumentationUri);
        System.out.println("ProcessAPI: added required instrumentation");

        System.out.println("ProcessAPI: will save process");
        try {
            process.save();
        } catch (Exception e) {
            e.printStackTrace();
            return internalServerError("Error saving process: " + e.getMessage());
        }
        System.out.println("ProcessAPI: saved process");

        return ok("Received processuri: " + processuri + ", instrumenturis: " + instrumentUris);
    }

}
