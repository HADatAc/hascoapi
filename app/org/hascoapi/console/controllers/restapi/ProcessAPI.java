package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.hascoapi.entity.pojo.Process;
import org.hascoapi.entity.pojo.ProcessStem;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.HAScOMapper;
import org.hascoapi.vocabularies.VSTOI;

import org.hascoapi.simulation.WorkflowExecution;
import play.mvc.Controller;
import play.mvc.Result;

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

    /*
        Metodo que será utilizado para executar o simulador

    */


    public Result executeworkflow(String uri, String duration) {
        /*
        1- Metodo deve buscar qual process é baseado no uri
        2- Depois deve buscar qual a TopTask deste process
        3- Depois verificar se a task é abstrata ou executável, verificando se tem subtasks
            3.1- Verifica o hasTemporalDependency e soma o tempo que deve ser executada
        4- Caso tenha subtasks deve ir nelas e ver se teem subtasks.
            4.1- Verifica se tem hasSuperTask para definir ordem
            4.2- Verifica se teem hasTemporalDependency, se tiver, aloca parte do tempo da Supertask
            4.3- Se tiver mais de um subtask, verifica o hasTemporalDependency e depois aloca o tempo restante da Supertask
        5- Caso nao tenha subtasks deve executar esta task
        6- Caso tenha mais de uma subtask deve ter uma ordem de execução
        7- depois de executar deve retornar como executado e verificar se tem outra subtask para executar, caso tenha executa.
        8- retorna uma mensagem opc-ua de que terminou o workflow
         */
        if (uri == null || uri.equals("")) {
            System.out.println("[ERROR] No uri has been provided.");
            return null;
        }
        WorkflowExecution processexec =  new WorkflowExecution();
        Float durationfloat = Float.parseFloat(duration);
        processexec.execute(uri,durationfloat);



        return ok();

    }



}
