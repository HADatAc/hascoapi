package org.hascoapi.simulation;

import org.hascoapi.entity.pojo.Process;
import org.hascoapi.entity.pojo.Task;

import java.util.List;

public class WorkflowExecution {
    /*

1- Metodo deve buscar qual process é baseado no uri
2- Depois deve buscar qual a TopTask deste process
3- Depois verificar se a task é abstrata ou executável, verificando se tem subtasks
    3.1- Verifica o hasTemporalDependency e soma o tempo que deve ser executada
4- Caso tenha subtasks deve ir nelas e ver se teem subtasks.
    4.1- Verifica se tem hasSuperTask a hierarquia, e a sequencia na lista dita a ordem
    4.2- Verifica se teem hasTemporalDependency, se tiver, aloca parte do tempo da Supertask
    4.3- Se tiver mais de um subtask, verifica o hasTemporalDependency e depois aloca o tempo restante da Supertask
5- Caso nao tenha subtasks deve executar esta task
6- Caso tenha mais de uma subtask deve ter uma ordem de execução , que será a ordem da lista.
7- depois de executar deve retornar como executado e verificar se tem outra subtask para executar, caso tenha executa.
8- retorna uma mensagem opc-ua de que terminou o workflow
Usar threads para criar tempo


 */

    private Process process;
    private Task toptask;
    private Float process_duration;



    public void execute(String uri, Float duration) {
        // 1 - Searching process
        if(duration!=null || duration>0 || !duration.isNaN() ) {
            process_duration = duration;
        }else{
            System.out.println("[ERROR] Invalid duration");
            return;
        }

        try {
            Process process = Process.find(uri);
            if (process == null) {
                System.out.println("[ERROR] No process with URI [" + uri + "] has been found");
            }
        }catch( Exception e){
            System.out.println("[ERROR] Could  not find process with URI [" + uri + "]");
            return;
            }

        // 2 - Getting TopTask
        if (process.getHasTopTask() !=null) {
            toptask = process.getHasTopTask();
        }else {
            System.out.println("[ERROR] No Top task has been found");
            return;
        }
        List<String> topsubtaskUris;
        // 3 - Verify if task has subtasks
        if(!toptask.getHasSubtaskUris().isEmpty() || toptask.getHasSubtaskUris() == null) {

            toptask.setHasStatus("In Progress");
            topsubtaskUris = toptask.getHasSubtaskUris();
            executeSubtasks(topsubtaskUris);

        } else{
            //toptask é executável
            //gerar OPC-UA
            System.out.println("No Subtask URIs have been found");
        }



    }

    /**
     * Percorre recursivamente uma lista de subtasks
     */
    private void executeSubtasks(List<String> subtaskUris) {
        for (String subtaskUri : subtaskUris) {
            Task subtask = Task.find(subtaskUri); // supondo que existe um metodo find similar ao de Process

            if (subtask == null) {
                System.out.println("[WARNING] Subtask with URI [" + subtaskUri + "] not founded.");
                continue;
            }

            System.out.println("Verifying Subtask [" + subtask.getUri() + "]...");

            if (subtask.getHasSubtaskUris() != null && !subtask.getHasSubtaskUris().isEmpty()) {
                subtask.setHasStatus("");
                System.out.println("Subtask [" + subtask.getUri() + "] is abstract . Entering Subtasks...");
                // Chamada recursiva
                executeSubtasks(subtask.getHasSubtaskUris());
            } else {
                subtask.setHasStatus("Executable");
                System.out.println("Executing [" + subtask.getUri() + "]");
                // Aqui entra a execução real (ex: chamada OPC-UA)
            }
        }
    }

}
