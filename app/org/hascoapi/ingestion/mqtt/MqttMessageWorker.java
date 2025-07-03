package org.hascoapi.ingestion.mqtt;

import java.util.HashMap;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.hascoapi.ingestion.CSVRecordFile;
import org.hascoapi.ingestion.JSONRecord;
import org.hascoapi.ingestion.ValueGenerator;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.ingestion.Record;
import org.hascoapi.ingestion.RecordFile;
import org.hascoapi.entity.pojo.DA;
import org.hascoapi.entity.pojo.DataFile;
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
        try {
            StreamTopic streamTopic = StreamTopic.find(topicUri);
            if (!streamTopics.containsKey(streamTopic.getUri())) {
                return false;
            }
            monitor.stopStreamTopic(topicUri);
            this.stopStream(streamTopic);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        System.out.println("[DEBUG] setStatus called with streamTopicUri=" + streamTopicUri + ", status=" + status);
        StreamTopic streamTopic = streamTopics.get(streamTopicUri);
        if (streamTopic == null) {
            System.out.println("[ERROR] MqttMessageWorker: there is no subscribed stream topic with URI " + streamTopicUri);
            return false;
        }
    
        System.out.println("[DEBUG] Current StreamTopic status: " + streamTopic.getHasTopicStatus());
    
        if (status.equals(HASCO.INACTIVE)) {
            System.out.println("[ERROR] MqttMessageWorker: Cannot change the status of StreamTopic " + streamTopicUri + " to INACTIVE");
            return false;
        } else if (status.equals(HASCO.SUSPENDED)) {
            if (streamTopic.getHasTopicStatus().equals(HASCO.RECORDING) || streamTopic.getHasTopicStatus().equals(HASCO.INGESTING)) {
                if (streamTopic.getHasTopicStatus().equals(HASCO.RECORDING)) {
                    System.out.println("[DEBUG] Calling stopRecordingMessageStreamTopic...");
                    MqttMessageAnnotation.stopRecordingMessageStreamTopic(streamTopic);
                }
                streamTopic.setHasTopicStatus(status);
                streamTopic.save();
                System.out.println("[INFO] MqttMessageWorker: Status of " + streamTopicUri + " has changed to " + status);
                return true;
            } else {
                System.out.println("[WARN] MqttMessageWorker: StreamTopic " + streamTopicUri + " is not RECORDING or INGESTING, cannot suspend");
                return false;
            }
        } else if (status.equals(HASCO.RECORDING)) {
            if (streamTopic.getHasTopicStatus().equals(HASCO.SUSPENDED)) {
                System.out.println("[DEBUG] Calling startRecordingMessageStreamTopic...");
                MqttMessageAnnotation.startRecordingMessageStreamTopic(streamTopic);
                // streamTopic.setHasTopicStatus(status);
                // streamTopic.save();
                System.out.println("[INFO] MqttMessageWorker: Status of " + streamTopicUri + " has changed to " + status);
                return true;
            } else {
                System.out.println("[WARN] MqttMessageWorker: StreamTopic " + streamTopicUri + " is not SUSPENDED, cannot change to RECORDING");
                return false;
            }
        } else if (status.equals(HASCO.INGESTING)) {
            if (streamTopic.getHasTopicStatus().equals(HASCO.SUSPENDED)) {
                streamTopic.setHasTopicStatus(status);
                streamTopic.save();
                System.out.println("[INFO] MqttMessageWorker: Status of " + streamTopicUri + " has changed to " + status);
                return true;
            } else {
                System.out.println("[WARN] MqttMessageWorker: StreamTopic " + streamTopicUri + " is not SUSPENDED, cannot change to INGESTING");
                return false;
            }
        }
        System.out.println("[ERROR] MqttMessageWorker: invalid status " + status);
        return false;
    }

    public static Record processMessage(StreamTopic streamTopic, String topicStr, String message, int currentRow) {

        System.out.println("TopicStr: [" + topicStr + "]   Message: [" + message + "]");
        MqttMessageWorker.getInstance().monitor.updateLatestValue(streamTopic.getUri(), message);
        System.out.println("[DEBUG] First Current Headers: " + streamTopic.getHeaders());

        if (streamTopic.getHeaders() == null || streamTopic.getHeaders().isEmpty()) {
            JSONRecord jsonRecord = new JSONRecord(message);
            List<String> headers = jsonRecord.getHeaders();
            streamTopic.setHeaders(headers.toString());
        }
        System.out.println("[DEBUG] Second Current Headers: " + streamTopic.getHeaders());
        Record record = new JSONRecord(message, streamTopic.getHeaders());
    
        String status = streamTopic.getHasTopicStatus();
        System.out.println("[DEBUG] Current topic in processMessage status: " + status);
    
        if (status.equals(HASCO.RECORDING)) {
            String topicUri = streamTopic.getUri();
            System.out.println("[DEBUG] Attempting to record message for topicUri: " + topicUri);
            DataFile df = DataFile.findMostRecentByStreamTopicUri(topicUri);
            if (df != null) {
                String directoryPath = "/var/hascoapi/stream/files";  // teu diretório fixo
                File csvPhysicalFile = new File(directoryPath, df.getFilename());

                if (!csvPhysicalFile.exists()) {
                    System.err.println("[ERROR] Arquivo físico não encontrado: " + csvPhysicalFile.getAbsolutePath());
                    return record;  // ou lança exceção, conforme desejado
                }
                
                CSVRecordFile csvRecordFile = new CSVRecordFile(csvPhysicalFile);
                df.setRecordFile(csvRecordFile);
                try {
                    csvRecordFile.appendRecord(record);
                    System.out.println("[INFO] Record appended to CSV file for topicUri: " + topicUri);

                    // Atualiza DA
                    DA da = DA.findByDataFileUri(df.getUri());
                    if (da != null) {
                        long total = Long.valueOf(da.getHasTotalRecordedMessages());
                        total = total + 1;
                        da.setHasTotalRecordedMessages(Long.toString(total));
                        da.save();
                        DA reloaded = DA.find(da.getUri());
                        System.out.println("Reloaded DA hasTotalRecordedMessages = " + reloaded.getHasTotalRecordedMessages());
                    } else {
                        System.err.println("[WARN] DA not found for topicUri: " + topicUri);
                    }
                } catch (IOException e) {
                    System.err.println("[ERROR] Error writing record to CSV file for topic: " + topicUri);
                    e.printStackTrace();
                }
            } else {
                System.err.println("[WARN] No DataFile found for topicUri: " + topicUri);
            }
        } else if (status.equals(HASCO.INGESTING)) {
            System.out.println("[DEBUG] Topic in INGESTING mode, attempting to generate object...");
            ValueGenerator generator = MqttMessageWorker.getInstance().getStreamGenerator(streamTopic.getUri());
            if (generator == null) {
                System.out.println("[ERROR] MessageWorker: stream generator is missing in processMessage");
            } else {
                try {
                    generator.createObject(record, currentRow, topicStr);
                    System.out.println("[INFO] Object generated successfully.");
                } catch (Exception e) {
                    System.err.println("[ERROR] Exception while creating object:");
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
