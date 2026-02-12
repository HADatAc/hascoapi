package org.hascoapi.transform.mt.wkf;

import java.util.Map;
import java.util.HashMap;
import org.hascoapi.entity.pojo.NameSpace;
import org.hascoapi.entity.pojo.ProcessStem;
import org.hascoapi.entity.pojo.Process;
import org.hascoapi.entity.pojo.Task;
import org.hascoapi.entity.pojo.RequiredInstrument;
import org.apache.poi.ss.usermodel.*;

public class WKFGenHelper {

    public static Map<String,NameSpace> namespaces;
    public Map<String,ProcessStem> processStems;
    public Map<String,Process> processes;
    public Map<String,Task> tasks;
    public Map<String,RequiredInstrument> requiredInstruments;
    public Workbook workbook;

    public WKFGenHelper() {
        namespaces = new HashMap<String,NameSpace>();
        processStems = new HashMap<String,ProcessStem>();
        processes = new HashMap<String,Process>();
        tasks = new HashMap<String,Task>();
        requiredInstruments = new HashMap<String,RequiredInstrument>();
        workbook = null;
    }

    public void addNamespace(NameSpace namespace) {
        if (namespace == null || namespace.getUri() == null) {
            return;
        }
        if (!namespaces.containsKey(namespace.getUri())) {
            namespaces.put(namespace.getUri(),namespace);
        }
    }

    public static Map<String, NameSpace> getNamespaces() {
        return namespaces;
    }

    public void setNamespaces(Map<String, NameSpace> namespaces) {
        this.namespaces = namespaces;
    }

    public Map<String, ProcessStem> getProcessStems() {
        return processStems;
    }

    public void setProcessStems(Map<String, ProcessStem> processStems) {
        this.processStems = processStems;
    }

    public Map<String, Process> getProcesses() {
        return processes;
    }

    public void setProcesses(Map<String, Process> processes) {
        this.processes = processes;
    }

    public Map<String, Task> getTasks() {
        return tasks;
    }

    public void setTasks(Map<String, Task> tasks) {
        this.tasks = tasks;
    }

    public Map<String, RequiredInstrument> getRequiredInstruments() {
        return requiredInstruments;
    }

    public void setRequiredInstruments(Map<String, RequiredInstrument> requiredInstruments) {
        this.requiredInstruments = requiredInstruments;
    }

    public Workbook getWorkbook() {
        return workbook;
    }

    public void setWorkbook(Workbook workbook) {
        this.workbook = workbook;
    }
}
