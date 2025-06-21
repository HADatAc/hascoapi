package org.hascoapi.ingestion.mqtt;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.hascoapi.ingestion.JSONRecord;
import org.hascoapi.ingestion.ValueGenerator;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.ingestion.Record;
import org.hascoapi.entity.pojo.StreamTopic;

public class MqttMessageWorker {

    private static MqttMessageWorker single_instance = null;

    // public variables
    final private Map<String,StreamTopic> streamTopics;
    final private Map<String,ExecutorService> executorsMap;
    final private Map<String,MqttAsyncClient> clientsMap;
    final private Map<String,ValueGenerator> streamGenMap;
    final private MqttMonitor monitor;

    private MqttMessageWorker() {
        streamTopics = new HashMap<String,StreamTopic>();
        executorsMap = new HashMap<String,ExecutorService>();
        clientsMap = new HashMap<String,MqttAsyncClient>();
        streamGenMap = new HashMap<String,ValueGenerator>();
        monitor = new MqttMonitor();

    }

    // static method to create instance of Singleton class
    public static MqttMessageWorker getInstance()
    {
        if (single_instance == null)
            single_instance = new MqttMessageWorker();

        return single_instance;
    }

    public List<StreamTopic> listStreamTopics() {
        return new ArrayList<StreamTopic>(streamTopics.values());
    }

    //public boolean addStreamTopic(StreamTopic streamTopic) {
    public boolean addStreamTopicToWorker(String topicUri) {
        //topicUri = "http://cienciapt.org/kg/STP1750214669419661";
        StreamTopic streamTopic = StreamTopic.find(topicUri);
        if (streamTopics.containsKey(streamTopic.getUri())) {
            return false;
        }
        streamTopic.setHasTopicStatus(HASCO.SUSPENDED);
        streamTopic.save();
        System.out.println("Subscribing streamTopic [" + streamTopic.getUri() + "]");
        streamTopics.put(streamTopic.getUri(),streamTopic);
        System.out.println("  - creating generator [" + streamTopic.getUri() + "]");
        //ValueGenerator generator = new ValueGenerator(0, null, null, null, null);
        System.out.println("  - creating executor [" + streamTopic.getUri() + "]");
        MqttAsyncSubscribe.exec(streamTopic, null);
        monitor.addStreamTopic(topicUri);
        return true;
    }

    //public boolean deleteStreamTopic(StreamTopic streamTopic) {
    public boolean removeStreamTopicFromWorker(String topicUri) {
        //topicUri = "http://cienciapt.org/kg/STP1750214669419661";
        System.out.println("MqttMessageWorker.removeStreamTopicFromWorker() has been called");
        StreamTopic streamTopic = StreamTopic.find(topicUri);
        if (!streamTopics.containsKey(streamTopic.getUri())) {
            return false;
        }
        monitor.stopStreamTopic(topicUri);
        this.stopStream(streamTopic);
        return true;
    }

    public MqttMonitor getMonitor() {
        return monitor;
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
        System.out.println("Added client " + client.getClientId() + " to topic " + streamTopic.getLabel() + " (" + streamTopic.getUri() + ")");
        this.clientsMap.put(streamTopic.getUri(), client);
        System.out.println("Total number of clients is " + this.clientsMap.size());
        System.out.println("Total number of executors is " + this.executorsMap.size());
    }

    public ValueGenerator getStreamGenerator(String streamTopicUri) {
        return streamGenMap.get(streamTopicUri);
    }

    public void addStreamGenerator(String streamTopicUri, ValueGenerator streamGen) {
        this.streamGenMap.put(streamTopicUri, streamGen);
    }

    public boolean setStatus(String streamTopicUri, String status) {
        StreamTopic streamTopic = streamTopics.get(streamTopicUri);
        if (streamTopic == null) {
            System.out.println("[ERROR] MqttMessageWorker: there is no subscribed stream topic with URI " + streamTopicUri);
            return false;
        }
        if (status.equals(HASCO.INACTIVE)) {
            System.out.println("[ERROR] MqttMessageWorker: Cannot change the status of StreamTopic " + streamTopicUri + " since it is INACTIVE");
            return false;
        } else if (status.equals(HASCO.SUSPENDED)) {
            if (streamTopic.getHasTopicStatus().equals(HASCO.RECORDING) || streamTopic.getHasTopicStatus().equals(HASCO.INGESTING)) {
                if (streamTopic.getHasTopicStatus().equals(HASCO.RECORDING)) {
                    /* TIAGO */
                    MqttMessageAnnotation.stopRecordingMessageStreamTopic(streamTopic);
                }
                streamTopic.setHasTopicStatus(status);
                streamTopic.save();
                System.out.println("MqttMessageWorker: Status of " + streamTopicUri + " has changed to " + status);
                return true;
            } else {
                System.out.println("MqttMessageWorker: StreamTopic " + streamTopicUri + " needs to be RECORDING or INGESTING to be SUSPENDED");
                return false;
            }
        } else if (status.equals(HASCO.RECORDING)) {
            if (streamTopic.getHasTopicStatus().equals(HASCO.SUSPENDED)) {
                /* TIAGO */
                MqttMessageAnnotation.startRecordingMessageStreamTopic(streamTopic);
                streamTopic.setHasTopicStatus(status);
                streamTopic.save();
                System.out.println("MqttMessageWorker: Status of " + streamTopicUri + " has changed to " + status);
                return true;
            } else {
                System.out.println("MqttMessageWorker: StreamTopic " + streamTopicUri + " needs to be SUSPENDED to change to RECORDING");
                return false;
            }
        } else if (status.equals(HASCO.INGESTING)) {
            if (streamTopic.getHasTopicStatus().equals(HASCO.SUSPENDED)) {
                streamTopic.setHasTopicStatus(status);
                streamTopic.save();
                System.out.println("MqttMessageWorker: Status of " + streamTopicUri + " has changed to " + status);
                return true;
            } else {
                System.out.println("MqttMessageWorker: StreamTopic " + streamTopicUri + " needs to be SUSPENDED to change to INGESTING");
                return false;
            }
        } 
        System.out.println("[ERROR] MqttMessageWorker: invalid status " + status);
        return false;
        
    }

    public static Record processMessage(StreamTopic streamTopic, String topicStr, String message, int currentRow) {
        System.out.println("TopicStr: [" + topicStr + "]   Message: [" + message + "]");
        MqttMessageWorker.getInstance().monitor.updateLatestValue(streamTopic.getUri(), message);

        Record record = new JSONRecord(message, streamTopic.getHeaders());

        if (streamTopic.getHasTopicStatus().equals(HASCO.RECORDING)) {
            /* TIAGO */
            /* POR AQUI O CODIGO QUE GRAVA O CONTEUDO NOS ARQUIVOS */
            /* ATUALIZAR O CODIGO EM MQTTMESSAGEANNOTATION PARA CRIAR/GERENCIAR ARQUIVOS GERADOS */
        } else if (streamTopic.getHasTopicStatus().equals(HASCO.INGESTING)) {
            ValueGenerator generator = MqttMessageWorker.getInstance().getStreamGenerator(streamTopic.getUri());
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
        }
        return record;
    }

    protected void stopStream(StreamTopic streamTopic) {

        if (streamTopic == null) {
            System.out.println("[ERROR] MsqqMessageWorker: asking to stop a null stream");
            return;
        }
        streamTopic.getMessageLogger().println("MessageWorker: stopping stream " + streamTopic.getUri());
        System.out.println("MessageWorker: stopping stream " + streamTopic.getUri());
        try {
            if (clientsMap != null && streamTopic != null && clientsMap.get(streamTopic.getUri()) != null) {
                clientsMap.get(streamTopic.getUri()).unsubscribe(streamTopic.getLabel() + "/#");
                clientsMap.get(streamTopic.getUri()).disconnectForcibly();
                clientsMap.put(streamTopic.getUri(),null);
                streamTopic.getMessageLogger().println("Unsubscribed mqtt stream [" + streamTopic.getUri() + "]");
                System.out.println("Unsubscribed mqtt topic [" + streamTopic.getUri() + "]");
            }
            if (executorsMap != null && streamTopic != null && executorsMap.get(streamTopic.getUri()) != null) {
                executorsMap.get(streamTopic.getUri()).shutdownNow();
                executorsMap.put(streamTopic.getUri(),null);
                streamTopic.getMessageLogger().println("Stopped stream thread [" + streamTopic.getUri() + "]");
                System.out.println("Stopped topic thread [" + streamTopic.getUri() + "]");
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (clientsMap != null && streamTopic != null && streamTopic.getUri() != null) {
            clientsMap.remove(streamTopic.getUri());
            streamTopic.getMessageLogger().println("Removed stream MQTT client");
            System.out.println("Removed stream MQTT client");
        }
        if (executorsMap != null && streamTopic != null && streamTopic.getUri() != null) {
            executorsMap.remove(streamTopic.getUri());
            streamTopic.getMessageLogger().println("Removed service executor");
            System.out.println("Removed service executor");
        }
        MqttMessageWorker.getInstance().streamGenMap.remove(streamTopic.getUri());

        // Remove the streamTopic itself
        MqttMessageWorker.getInstance().streamTopics.remove(streamTopic.getUri());
        streamTopic.getMessageLogger().println("Removed value generator");
        streamTopic.setHasTopicStatus(HASCO.INACTIVE);
        streamTopic.save();
        System.out.println("Removed value generator");
        System.out.println("Total number of clients is " + this.clientsMap.size());
        System.out.println("Total number of executors is " + this.executorsMap.size());
    }

}
