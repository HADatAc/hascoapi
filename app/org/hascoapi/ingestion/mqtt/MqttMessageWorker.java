package org.hascoapi.ingestion.mqtt;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.hascoapi.utils.StreamTopicStore;
import org.hascoapi.ingestion.JSONRecord;
import org.hascoapi.ingestion.ValueGenerator;
import org.hascoapi.ingestion.Record;
import org.hascoapi.entity.pojo.StreamTopic;

public class MqttMessageWorker {

    private static MqttMessageWorker single_instance = null;

    // public variables
    final private List<StreamTopic> streamTopics;
    final private Map<String,ExecutorService> executorsMap;
    final private Map<String,MqttAsyncClient> clientsMap;
    final private Map<String,ValueGenerator> streamGenMap;

    private MqttMessageWorker() {
        streamTopics = new ArrayList<StreamTopic>();
        executorsMap = new HashMap<String,ExecutorService>();
        clientsMap = new HashMap<String,MqttAsyncClient>();
        streamGenMap = new HashMap<String,ValueGenerator>();
    }

    // static method to create instance of Singleton class
    public static MqttMessageWorker getInstance()
    {
        if (single_instance == null)
            single_instance = new MqttMessageWorker();

        return single_instance;
    }

    public List<StreamTopic> listStreamTopics() {
        return streamTopics;
    }

    //public boolean addStreamTopic(StreamTopic streamTopic) {
    public boolean addStreamTopicToWorker() {
        StreamTopic streamTopic = StreamTopic.find("http://cienciapt.org/kg/STP1750214669419661");
        if (streamTopics.contains(streamTopic)) {
            return false;
        }
        streamTopics.add(streamTopic);
        return true;
    }

    //public boolean deleteStreamTopic(StreamTopic streamTopic) {
    public boolean removeStreamTopicFromWorker() {
        StreamTopic streamTopic = StreamTopic.find("http://cienciapt.org/kg/STP1750214669419661");
        if (!streamTopics.contains(streamTopic)) {
            return false;
        }
        streamTopics.remove(streamTopic);
        return true;
    }

    public ExecutorService getExecutor(String streamTopicUri) {
        return executorsMap.get(streamTopicUri);
    }

    public void addExecutor(StreamTopic streamTopic, ExecutorService executor) {
        this.executorsMap.put(streamTopic.getUri(), executor);
    }

    public boolean containsExecutor(StreamTopic streamTopic) {
        if (executorsMap == null || streamTopic == null || streamTopic.getUri() == null) {
            return false;
        }
        return executorsMap.containsKey(streamTopic.getUri());
    }


    public MqttAsyncClient getClient(String streamTopicUri) {
        return clientsMap.get(streamTopicUri);
    }

    public void addClient(StreamTopic streamTopic, MqttAsyncClient client) {
        this.clientsMap.put(streamTopic.getUri(), client);
    }

    public ValueGenerator getStreamGenerator(String streamTopicUri) {
        return streamGenMap.get(streamTopicUri);
    }

    public void addStreamGenerator(String streamTopicUri, ValueGenerator streamGen) {
        this.streamGenMap.put(streamTopicUri, streamGen);
    }

    public static Record processMessage(String streamTopicUri, String topicStr, String message, int currentRow) {
        System.out.println("TopicStr: [" + topicStr + "]   Message: [" + message + "]");

        StreamTopic streamTopic = StreamTopicStore.getInstance().findCachedByUri(streamTopicUri);
        ValueGenerator generator = MqttMessageWorker.getInstance().getStreamGenerator(streamTopicUri);
        Record record = new JSONRecord(message, streamTopic.getHeaders());
        if (generator == null) {
            System.out.println("MessageWorker: stream generator is missing in processMessage");
        } else {
            try {
                generator.createObject(record, currentRow, topicStr);
                //generator.postprocess();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return record;
    }

    public void stopStream(String streamUri) {

        StreamTopic streamTopic = StreamTopicStore.getInstance().findCachedByUri(streamUri);
        streamTopic.getMessageLogger().println("MessageWorker: stopping stream " + streamTopic.getUri());
        try {
            if (clientsMap != null && streamTopic != null && clientsMap.get(streamTopic.getUri()) != null) {
                clientsMap.get(streamTopic.getUri()).unsubscribe(streamTopic.getLabel() + "/#");
                clientsMap.get(streamTopic.getUri()).disconnectForcibly();
                clientsMap.put(streamTopic.getUri(),null);
                streamTopic.getMessageLogger().println("Unsubscribed mqtt stream [" + streamTopic.getUri() + "]");
            }
        } catch (MqttException e) {
            if (executorsMap != null && streamTopic != null && executorsMap.get(streamTopic.getUri()) != null) {
                executorsMap.get(streamTopic.getUri()).shutdownNow();
                executorsMap.put(streamTopic.getUri(),null);
                streamTopic.getMessageLogger().println("Stopped stream thread [" + streamTopic.getUri() + "]");
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (clientsMap != null && streamTopic != null && streamTopic.getUri() != null) {
            clientsMap.remove(streamTopic.getUri());
            streamTopic.getMessageLogger().println("Removed stream MQTT client");
        }
        if (executorsMap != null && streamTopic != null && streamTopic.getUri() != null) {
            executorsMap.remove(streamTopic.getUri());
            streamTopic.getMessageLogger().println("Removed service executor");
        }
        MqttMessageWorker.getInstance().streamGenMap.remove(streamTopic.getUri());
        streamTopic.getMessageLogger().println("Removed value generator");
    }

}
