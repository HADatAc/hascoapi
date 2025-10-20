package org.hascoapi.ingestion.mqtt;

import org.eclipse.paho.client.mqttv3.*;

import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class MqttExposeManager {

    private static MqttExposeManager instance;

    private final Map<String, MqttClient> exposeClients = new HashMap<>();
    private final int qos = 0;
    private final Set<String> exposingTopics = new HashSet<>();


    private MqttExposeManager() {}

    public static synchronized MqttExposeManager getInstance() {
        if (instance == null) {
            instance = new MqttExposeManager();
        }
        return instance;
    }

    public boolean startExpose(String topicUri, String topicLabel, String brokerUrl) {
        try {
            String clientId = "Expose-" + topicLabel + "-" + System.currentTimeMillis();
            MqttClient client = new MqttClient(brokerUrl, clientId, null);
    
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            client.connect(options);
    
            exposeClients.put(topicUri, client);
            exposingTopics.add(topicUri);
    
            System.out.println("[ExposeManager] Started exposing topic " + topicLabel + " to " + brokerUrl);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    

    public boolean stopExpose(String topicUri) {
        try {
            MqttClient client = exposeClients.get(topicUri);
            if (client != null && client.isConnected()) {
                client.disconnect();
                exposeClients.remove(topicUri);
            }
            exposingTopics.remove(topicUri);
            System.out.println("[ExposeManager] Stopped exposing topic " + topicUri);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean isExposing(String topicUri) {
        return exposingTopics.contains(topicUri);
    }

    public void publish(String topicUri, String topicLabel, String originalMessage, String recordUri) {
        if (isExposing(topicUri)) {
            MqttClient client = exposeClients.get(topicUri);
            if (client != null && client.isConnected()) {
                try {
                    JSONObject wrappedMessage = new JSONObject();
                    wrappedMessage.put("recordUri", recordUri);
                    wrappedMessage.put("sourceTopicUri", topicUri);
    
                    try {
                        JSONParser parser = new JSONParser();
                        Object parsed = parser.parse(originalMessage);
                        wrappedMessage.put("originalMessage", parsed);
                    } catch (org.json.simple.parser.ParseException e) {
                        wrappedMessage.put("originalMessage", originalMessage);
                    }
    
                    MqttMessage mqttMessage = new MqttMessage(wrappedMessage.toJSONString().getBytes());
                    mqttMessage.setQos(qos);
                    client.publish(topicLabel, mqttMessage);
    
                    System.out.println("[ExposeManager] Republishing message to expose broker: " + topicLabel);
                    System.out.println("[ExposeManager] recordUri: " + recordUri);
                } catch (Exception e) {
                    System.err.println("[ExposeManager] Error publishing message for topic: " + topicLabel);
                    e.printStackTrace();
                }
            } else {
                System.out.println("[ExposeManager] No active expose client for topic: " + topicLabel);
            }
        }
    }
    
}
