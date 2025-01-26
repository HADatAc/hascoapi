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
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.HAScOMapper;
import org.hascoapi.vocabularies.VSTOI;

import play.mvc.Controller;
import play.mvc.Result;
import static org.hascoapi.Constants.*;
import java.util.List;


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

    public Result addInstrument(String processUri, String instrumentUri){
        System.out.println("ProcessAPI.java: adding instrument [" + instrumentUri + "] to process [" + processUri + "]");
        if (processUri == null || processUri.equals("")) {
            return ok(ApiUtil.createResponse("No processURI has been provided.", false));
        }
        if (instrumentUri == null || instrumentUri.equals("")) {
            return ok(ApiUtil.createResponse("No instrumentURI has been provided.", false));
        }
        Process process = Process.find(processUri);
        if (process == null) {
            return ok(ApiUtil.createResponse("Process with URI <" + processUri + "> could not be found.", false));
        } 
        Instrument instrument = Instrument.find(instrumentUri);
        if (instrument == null) {
            return ok(ApiUtil.createResponse("Instrument with URI <" + instrumentUri + "> could not be found.", false));
        } 
        boolean resp = process.addInstrumentUri(instrumentUri);
        if (resp) {
            return ok(ApiUtil.createResponse("Instrument <" + instrumentUri + "> added to process <" + processUri + ">.", true));
        } else {
            return ok(ApiUtil.createResponse("Failed to add instrument <" + instrumentUri + "> to process <" + processUri + ">.", false));
        }
    }

    public Result removeInstrument(String processUri, String instrumentUri){
        if (processUri == null || processUri.equals("")) {
            return ok(ApiUtil.createResponse("No processURI has been provided.", false));
        }
        if (instrumentUri == null || instrumentUri.equals("")) {
            return ok(ApiUtil.createResponse("No instrumentURI has been provided.", false));
        }
        Process process = Process.find(processUri);
        if (process == null) {
            return ok(ApiUtil.createResponse("Process with URI <" + processUri + "> could not be found.", false));
        } 
        Instrument instrument = Instrument.find(instrumentUri);
        if (instrument == null) {
            return ok(ApiUtil.createResponse("Instrument with URI <" + instrumentUri + "> could not be found.", false));
        } 
        boolean resp = process.removeInstrumentUri(instrumentUri);
        if (resp) {
            return ok(ApiUtil.createResponse("Instrument <" + instrumentUri + "> removed from process <" + processUri + ">.", true));
        } else {
            return ok(ApiUtil.createResponse("Failed to remove instrument <" + instrumentUri + "> from process <" + processUri + ">.", false));
        }
    }

    public Result addDetector(String processUri, String detectorUri){
        if (processUri == null || processUri.equals("")) {
            return ok(ApiUtil.createResponse("No processURI has been provided.", false));
        }
        if (detectorUri == null || detectorUri.equals("")) {
            return ok(ApiUtil.createResponse("No detectorURI has been provided.", false));
        }
        Process process = Process.find(processUri);
        if (process == null) {
            return ok(ApiUtil.createResponse("Process with URI <" + processUri + "> could not be found.", false));
        } 
        Detector detector = Detector.find(detectorUri);
        if (detector == null) {
            return ok(ApiUtil.createResponse("Detector with URI <" + detectorUri + "> could not be found.", false));
        } 
        boolean resp = process.addDetectorUri(detectorUri);
        if (resp) {
            return ok(ApiUtil.createResponse("Detector <" + detectorUri + "> added to process <" + processUri + ">.", true));
        } else {
            return ok(ApiUtil.createResponse("Failed to add detector <" + detectorUri + "> to process <" + processUri + ">.", false));
        }
    }

    public Result removeDetector(String processUri, String detectorUri){
        if (processUri == null || processUri.equals("")) {
            return ok(ApiUtil.createResponse("No processURI has been provided.", false));
        }
        if (detectorUri == null || detectorUri.equals("")) {
            return ok(ApiUtil.createResponse("No detectorURI has been provided.", false));
        }
        Process process = Process.find(processUri);
        if (process == null) {
            return ok(ApiUtil.createResponse("Process with URI <" + processUri + "> could not be found.", false));
        } 
        Detector detector = Detector.find(detectorUri);
        if (detector == null) {
            return ok(ApiUtil.createResponse("Detector with URI <" + detectorUri + "> could not be found.", false));
        } 
        boolean resp = process.removeDetectorUri(detectorUri);
        if (resp) {
            return ok(ApiUtil.createResponse("Detector <" + detectorUri + "> removed from process <" + processUri + ">.", true));
        } else {
            return ok(ApiUtil.createResponse("Failed to reomve detector <" + detectorUri + "> from process <" + processUri + ">.", false));
        }
    }

}
