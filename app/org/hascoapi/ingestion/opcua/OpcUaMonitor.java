package org.hascoapi.ingestion.opcua;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OpcUaMonitor {

    // Conjunto de URIs de objetos monitorados (ex: objetos OPC UA)
    private final Set<String> objectURIs;

    // Contador de mensagens por objeto URI
    private final Map<String, Integer> messageCountMap;

    // Último valor recebido por objeto (opcional, pode ser útil para debug)
    private final Map<String, String> latestValueMap;

    public OpcUaMonitor() {
        this.objectURIs = ConcurrentHashMap.newKeySet();
        this.messageCountMap = new ConcurrentHashMap<>();
        this.latestValueMap = new ConcurrentHashMap<>();
    }

    /** Adiciona objeto para monitoramento */
    public void addObject(String objectUri) {
        objectURIs.add(objectUri);
        messageCountMap.putIfAbsent(objectUri, 0);
    }

    /** Remove objeto do monitoramento */
    public void removeObject(String objectUri) {
        objectURIs.remove(objectUri);
        messageCountMap.remove(objectUri);
        latestValueMap.remove(objectUri);
    }

    /** Lista todos os objetos monitorados */
    public List<String> listObjects() {
        return new ArrayList<>(objectURIs);
    }

    /** Atualiza o valor mais recente e incrementa o contador */
    public void updateLatestValue(String objectUri, String value) {
        latestValueMap.put(objectUri, value);
        messageCountMap.compute(objectUri, (k, v) -> (v == null) ? 1 : v + 1);
    }

    /** Retorna o número de mensagens recebidas */
    public int getMessageCount(String objectUri) {
        return messageCountMap.getOrDefault(objectUri, 0);
    }

    /** Retorna o último valor recebido */
    public String getLatestValue(String objectUri) {
        return latestValueMap.getOrDefault(objectUri, null);
    }

    /** Zera todos os contadores */
    public void resetAll() {
        messageCountMap.clear();
        latestValueMap.clear();
        for (String uri : objectURIs) {
            messageCountMap.put(uri, 0);
        }
    }
}
