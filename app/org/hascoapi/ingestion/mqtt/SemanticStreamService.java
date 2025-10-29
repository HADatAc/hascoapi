package org.hascoapi.ingestion.mqtt;

import org.hascoapi.entity.pojo.*;
import org.hascoapi.ingestion.mqtt.MqttExposeManager;
import org.hascoapi.utils.Utils;
import org.hascoapi.vocabularies.HASCO;
import java.time.Instant;

public class SemanticStreamService {

    private static SemanticStreamService instance;

    private SemanticStreamService() {}

    public static synchronized SemanticStreamService getInstance() {
        if (instance == null) {
            instance = new SemanticStreamService();
        }
        return instance;
    }

    public boolean startExpose(StreamTopic topic, String brokerIp, String brokerPort) {
        try {
            // 1. Encontrar ou criar o SemanticStream
            String streamUri = topic.getStreamUri();
            Stream stream = Stream.find(streamUri);
            SemanticStream semStream = SemanticStream.findByStream(streamUri);
            if (semStream == null) {
                semStream = new SemanticStream();
                semStream.setUri(Utils.uriGen("semanticstream"));
                semStream.setLabel("Semantic mirror of " + stream.getLabel());
                semStream.setHasStream(stream.getUri());
                semStream.setHasMessageProtocol("MQTT");
                semStream.setPort(brokerPort);
                semStream.setHasSemanticMessageBroker(SemanticMessageBroker.findOrCreate(brokerIp));
                semStream.save();
            }

            // 2. Criar SemanticStreamTopic
            SemanticStreamTopic semTopic = new SemanticStreamTopic();
            semTopic.setUri(Utils.uriGen("semanticstreamtopic"));
            semTopic.setLabel("Expose topic for " + topic.getLabel());
            semTopic.setStartedAt(Instant.now().toString());
            semTopic.setHasSemanticStream(semStream.getUri());
            semTopic.save();

            // 3. Iniciar expose MQTT
            String brokerUrl = "tcp://" + brokerIp + ":" + brokerPort;
            String deploymentUri = topic.getDeploymentUri();
            String safeDeployment = deploymentUri != null ? deploymentUri.replaceAll("[^a-zA-Z0-9_]", "_") : "unknown";
            String exposedTopicLabel = "expose/" + safeDeployment + "/" + topic.getLabel();

            return MqttExposeManager.getInstance().startExpose(topic.getUri(), exposedTopicLabel, brokerUrl);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean stopExpose(StreamTopic topic) {
        try {
            // 1. Apagar SemanticStreamTopic correspondente
            SemanticStreamTopic semTopic = SemanticStreamTopic.findByStream(topic.getUri());
            if (semTopic != null) {
                semTopic.delete();
            }

            // 2. Parar expose MQTT
            return MqttExposeManager.getInstance().stopExpose(topic.getUri());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isExposing(StreamTopic topic) {
        return SemanticStreamTopic.findByStream(topic.getUri()) != null;
    }
}
