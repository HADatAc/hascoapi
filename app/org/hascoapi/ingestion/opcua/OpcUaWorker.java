package org.hascoapi.ingestion.opcua;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OpcUaWorker {

    private static OpcUaWorker single_instance = null;

    // Mapa de executores por objeto OPC UA (URI)
    private final Map<String, ExecutorService> executorsMap;

    // Monitor de objetos OPC UA
    private final OpcUaMonitor monitor;

    private OpcUaWorker() {
        this.executorsMap = new HashMap<>();
        this.monitor = new OpcUaMonitor();
    }

    /** Singleton instance */
    public static synchronized OpcUaWorker getInstance() {
        if (single_instance == null) {
            single_instance = new OpcUaWorker();
        }
        return single_instance;
    }

    /** Adiciona um objeto OPC UA para monitoramento */
    public boolean addObjectToWorker(String objectUri) {
        if (executorsMap.containsKey(objectUri)) {
            return false;
        }

        monitor.addObject(objectUri);

        // Inicia uma assinatura assíncrona com executor dedicado
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executorsMap.put(objectUri, executor);

        executor.submit(() -> OpcUaObjectsAsyncSubscribe.exec(objectUri, monitor));

        return true;
    }

    /** Remove objeto do worker */
    public boolean removeObjectFromWorker(String objectUri) {
        if (!executorsMap.containsKey(objectUri)) {
            return false;
        }

        monitor.removeObject(objectUri);

        ExecutorService executor = executorsMap.remove(objectUri);
        executor.shutdownNow();

        return true;
    }

    /** Verifica se objeto já está em execução */
    public boolean containsObject(String objectUri) {
        return executorsMap.containsKey(objectUri);
    }

    /** Lista de objetos sendo monitorados */
    public List<String> listActiveObjects() {
        return new ArrayList<>(executorsMap.keySet());
    }

    /** Acesso ao monitor */
    public OpcUaMonitor getMonitor() {
        return monitor;
    }

    /** Parar todos os objetos monitorados */
    public void stopAll() {
        for (ExecutorService executor : executorsMap.values()) {
            executor.shutdownNow();
        }
        executorsMap.clear();
        monitor.resetAll();
    }
}
