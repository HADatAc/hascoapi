package org.hascoapi.ingestion.mqtt;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.String;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.modelmbean.DescriptorSupport;

import org.hascoapi.ingestion.CSVRecordFile;
import org.hascoapi.ingestion.ValueGenerator;
import org.hascoapi.utils.Utils;
import org.hascoapi.entity.pojo.Stream;
import org.hascoapi.entity.pojo.StreamTopic;
import org.hascoapi.entity.pojo.Study;
import org.hascoapi.entity.pojo.DA;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.vocabularies.HASCO;

public class MqttMessageAnnotation {

    public static final ConcurrentHashMap<String, FileWriter> topicWriters = new ConcurrentHashMap<>();


    public MqttMessageAnnotation() {}

    /* TIAGO */
    public static void startRecordingMessageStreamTopic(StreamTopic streamTopic) {
        System.out.println("[DEBUG] startRecordingMessageStreamTopic called");

        if (streamTopic == null || !streamTopic.getHasTopicStatus().equals(HASCO.SUSPENDED)) {
            System.out.println("[ERROR] streamTopic is null");
            return;
        }
        if (streamTopic.getStreamUri() == null || streamTopic.getStreamUri().isEmpty()) {
            System.out.println("[ERROR] streamUri is null or empty");
            return;
        }
        Stream stream = Stream.find(streamTopic.getStreamUri()); 
        if (stream == null) {
            System.out.println("[ERROR] Could not find Stream for URI: " + streamTopic.getStreamUri());
            return;
        }
        streamTopic.getMessageLogger().resetLog();
        streamTopic.getMessageLogger().println(String.format("Start recording message stream: %s", streamTopic.getLabel()));
        
        System.out.println("[INFO] Starting to prepare recording for topic: " + streamTopic.getLabel());

        String directoryPath = "/var/hascoapi/stream/files";
        File dir = new File(directoryPath);
        
        String topicUri = streamTopic.getUri();
        String topicLabel = streamTopic.getLabel();

        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                System.out.println("[ERROR] Failed to create directory: " + directoryPath);
                return;
            } else {
                System.out.println("[INFO] Directory created: " + directoryPath);
            }
        }
        
        // Gerar nome seguro base
        String safeFileNameBase = stream.getMessageName().replaceAll("[^a-zA-Z0-9_\\-]", "_");
        String safeTopicLabel = topicLabel.replaceAll("[^a-zA-Z0-9_\\-]", "_");  // sanitizar o nome do tópico

        
        // Encontrar o maior índice já usado
        int maxIndex = -1;
        File[] files = dir.listFiles((d, name) ->
            name.startsWith("DA-" + safeFileNameBase + "_") && name.endsWith(".csv"));
        
        if (files != null) {
            for (File f : files) {
                String name = f.getName();
                try {
                    String indexPart = name.substring(name.lastIndexOf('_') + 1, name.length() - 4); // remove ".csv"
                    int idx = Integer.parseInt(indexPart);
                    if (idx > maxIndex) {
                        maxIndex = idx;
                    }
                } catch (NumberFormatException ignored) {
                    // Ignorar ficheiros que não seguem o padrão
                }
            }
        }
        
        int index = maxIndex + 1;
        String fileName = String.format("DA-%s_%s_%d.csv", safeFileNameBase, safeTopicLabel, index);
        String fullPath = directoryPath + File.separator + fileName;


        String dataFileUri = Utils.uriGen("datafile");
        System.out.println("[DEBUG] Generated DataFile URI: " + dataFileUri);

        String fileId = fileName;

        String studyUri = "";
        if (stream.getStudy() != null) {
            studyUri = stream.getStudy().getUri();
        }
        String streamUri = stream.getUri();

        DataFile archive = new DataFile(fileId, fileName);
        File recordedFile = new File(fullPath);

        if (!recordedFile.exists()) {
            try {
                boolean created = recordedFile.createNewFile();
                if (created) {
                    System.out.println("[INFO] Ficheiro CSV criado: " + recordedFile.getAbsolutePath());
                } else {
                    System.out.println("[WARN] Ficheiro CSV já existia: " + recordedFile.getAbsolutePath());
                }
            } catch (IOException e) {
                System.err.println("[ERROR] Erro ao criar o ficheiro CSV:");
                e.printStackTrace();
                return;
            }
        }
        CSVRecordFile recordFile = new CSVRecordFile(recordedFile);
        
        archive.setTypeUri(HASCO.DATAFILE);
        archive.setHascoTypeUri(HASCO.DATAFILE);
        archive.setUri(dataFileUri);
        archive.setLabel(fileName);
        archive.setFilename(fileName);
        archive.setId(fileId);
        archive.setStudyUri(studyUri);
        archive.setStreamUri(streamUri);
        archive.setNamedGraph(dataFileUri);
        archive.setStreamTopicUri(topicUri);
        archive.setFileStatus(DataFile.UNPROCESSED);
        archive.setRecordFile(recordFile);
        Date date = new Date();
        String formattedDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(date);
        archive.setSubmissionTime(formattedDate);
        archive.save();

        /*
        ValueGenerator gen = new ValueGenerator(ValueGenerator.MSGMODE, null, stream, streamTopic.getSemanticDataDictionary(), null);
        if (!gen.getStudyUri().isEmpty()) {
            gen.setNamedGraphUri(gen.getStudyUri());
        }
        try {
            gen.preprocess();
            streamTopic.getMessageLogger().println("MessageAnnotation : message generator pre-processing completed.");
        } catch (Exception e1) {
            streamTopic.getMessageLogger().printException("Error with ValueGenerator inside MessageAnnotation: " + e1.toString());
            return;
        }
        MqttMessageWorker.getInstance().addStreamGenerator(streamTopic.getUri(), gen);

        try {
            stream.getMessageLogger().println("MessageAnnotation : calling AsyncSubscribe");
            CompletableFuture.runAsync(() -> MqttAsyncSubscribe.exec(streamTopic, gen));
        } catch (Exception e) {
            streamTopic.getMessageLogger().printException("MessageAnnotation: Error executing 'subscribe' inside startMessageStream.");
            e.printStackTrace();
            return;
        }
        */
        String daUri = dataFileUri.replace("DFL", Utils.shortPrefix("da"));
        System.out.println("[DEBUG] DA URI: " + daUri);
        DA da = new DA();
        da.setUri(daUri);
        da.setTypeUri(HASCO.DATA_ACQUISITION);
        da.setHascoTypeUri(HASCO.DATA_ACQUISITION);
        da.setIsMemberOfUri(studyUri);
        da.setLabel(fileName);
        da.setHasDataFileUri(dataFileUri);
        da.setHasVersion("");
        da.setComment("");
        da.save();
        streamTopic.getMessageLogger().println("DataAcquisition object created with URI: " + da.getUri());
        System.out.println("[DEBUG] DA URI FINAL: " + da.getUri());

        // DA da = new DA();
        // da.setLabel("Data Acquisition for " + fileName);
        // da.setHasDataFileUri(archive.getId());

        // Study study = stream.getStudy();
        // if (study != null) {
        //     da.setIsMemberOfUri(study.getUri());
        // } else {
        //     streamTopic.getMessageLogger().println("Warning: No Study linked to Stream for DA creation.");
        // }

        // da.save();
        streamTopic.getMessageLogger().println("DataAcquisition object created with URI: " + da.getUri());

        streamTopic.setHasTopicStatus(HASCO.RECORDING);
        streamTopic.getMessageLogger().println(String.format("Message stream [%s] is active.", streamTopic.getLabel()));
        streamTopic.save();
    }

    /* TIAGO */
    public static void stopRecordingMessageStreamTopic(StreamTopic streamTopic) {
        if (streamTopic == null || !streamTopic.getHasTopicStatus().equals(HASCO.RECORDING)) {
            return;
        }
        System.out.println("Stop recording stream topic: " + streamTopic.getLabel());
        /* TIAGO EH AQUI QUE TEM DE FICAR A LOGICA DE PARAR A GRAVACAO, SE EH QUE ALGO TEM DE OCORRER */

        String topicUri = streamTopic.getUri();
        FileWriter writer = topicWriters.remove(topicUri);
    
        if (writer != null) {
            synchronized (writer) {
                try {
                    writer.flush();
                    writer.close();
                    System.out.println("[INFO] FileWriter closed for topic: " + topicUri);
                } catch (IOException e) {
                    System.err.println("[ERROR] Failed to close FileWriter for topic: " + topicUri);
                    streamTopic.getMessageLogger().printException("Erro ao fechar FileWriter: " + e.getMessage());
                }
            }
        } else {
            System.out.println("[WARN] No FileWriter found for topic: " + topicUri);
        }

        streamTopic.getMessageLogger().resetLog();
        streamTopic.getMessageLogger().println(String.format("Stopped recording of stream topic [%s]", streamTopic.getLabel()));
        streamTopic.save();
    }

}
