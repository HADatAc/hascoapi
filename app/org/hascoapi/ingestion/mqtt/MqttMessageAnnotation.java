package org.hascoapi.ingestion.mqtt;

import java.lang.String;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

import java.util.concurrent.ConcurrentHashMap;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;

import org.hascoapi.ingestion.ValueGenerator;
import org.hascoapi.entity.pojo.Stream;
import org.hascoapi.entity.pojo.StreamTopic;
import org.hascoapi.entity.pojo.Study;
import org.hascoapi.entity.pojo.DA;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.vocabularies.HASCO;

public class MqttMessageAnnotation {

    public static final ConcurrentHashMap<String, FileWriter> topicWriters = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, Integer> topicFileIndexes = new ConcurrentHashMap<>();

    public MqttMessageAnnotation() {}

    /* TIAGO */
    public static void startRecordingMessageStreamTopic(StreamTopic streamTopic) {
        if (streamTopic == null || !streamTopic.getHasTopicStatus().equals(HASCO.SUSPENDED)) {
            return;
        }
        if (streamTopic.getStreamUri() == null || streamTopic.getStreamUri().isEmpty()) {
            return;
        }
        Stream stream = Stream.find(streamTopic.getStreamUri()); 
        if (stream == null) {
            return;
        }
        streamTopic.getMessageLogger().resetLog();
        streamTopic.getMessageLogger().println(String.format("Start recording message stream: %s", streamTopic.getLabel()));

        String topicUri = streamTopic.getUri();
        int index = topicFileIndexes.getOrDefault(topicUri, 0);
        topicFileIndexes.put(topicUri, index + 1);
    
        // Gerar nome seguro para o arquivo
        String safeFileNameBase = stream.getMessageName().replaceAll("[^a-zA-Z0-9_\\-]", "_");
        String fileName = String.format("DA-%s_%d.json", safeFileNameBase, index);
    
        // Criar o DataFile novo, sempre sequencial
        Date date = new Date();
        DataFile archive = DataFile.create(fileName, "" , "", DataFile.PROCESSED);
        archive.setSubmissionTime(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(date));
        archive.save();
    
        stream.setMessageArchiveId(archive.getId());
        stream.getMessageLogger().println(String.format("Creating archive datafile %s with id %s", fileName, archive.getId()));
        stream.save();
    
        // Criar diretório para gravação, se não existir
        String directoryPath = "/var/hascoapi/stream/files";
        new File(directoryPath).mkdirs();
    
        // Full path para gravação (deve ser igual ao nome do DataFile)
        String fullPath = directoryPath + File.separator + fileName;
    
        // Criar FileWriter e armazenar para gravação
        try {
            FileWriter writer = new FileWriter(fullPath, true);
            topicWriters.put(topicUri, writer);
            streamTopic.getMessageLogger().println("Gravação iniciada no ficheiro: " + fullPath);
        } catch (IOException e) {
            streamTopic.getMessageLogger().printException("Erro ao criar ficheiro de gravação: " + e.getMessage());
            return;
        }

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

        DA da = new DA();
        da.setLabel("Data Acquisition for " + fileName);
        da.setHasDataFileUri(archive.getId());

        Study study = stream.getStudy();
        if (study != null) {
            da.setIsMemberOfUri(study.getUri());
        } else {
            streamTopic.getMessageLogger().println("Warning: No Study linked to Stream for DA creation.");
        }

        da.save();
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
        System.out.println("Unsubscribing message stream: " + streamTopic.getLabel());
        streamTopic.getMessageLogger().resetLog();
        streamTopic.getMessageLogger().println(String.format("Unsubscribing message stream [%s]", streamTopic.getLabel()));
        if (!MqttMessageWorker.getInstance().containsExecutor(streamTopic)) {
            streamTopic.getMessageLogger().printWarning("CurrentClient for the following stream is missing [" + streamTopic.getUri() + "]");
        } else {
            MqttMessageWorker.getInstance().stopStream(streamTopic);
        }

        String topicUri = streamTopic.getUri();

        FileWriter writer = topicWriters.get(topicUri);
        if (writer != null) {
            try {
                writer.flush();
                writer.close();
                topicWriters.remove(topicUri);
                streamTopic.getMessageLogger().println("Ficheiro de gravação fechado com sucesso.");
            } catch (IOException e) {
                streamTopic.getMessageLogger().printException("Erro ao fechar ficheiro de gravação: " + e.getMessage());
            }
        } else {
            streamTopic.getMessageLogger().println("Nenhum ficheiro ativo de gravação encontrado para este tópico.");
        }


        streamTopic.setHasTopicStatus(HASCO.SUSPENDED);
        streamTopic.getMessageLogger().println(String.format("Suspended processing of message stream [%s]", streamTopic.getUri()));
        streamTopic.save();
    }

    /* 
    public static void closeMessageStream(StreamTopic streamTopic) {
        if (streamTopic == null || streamTopic.getHasTopicStatus().equals(HASCO.INACTIVE)) {
            return;
        }
        System.out.println("closing message stream: " + streamTopic.getLabel());
        streamTopic.getMessageLogger().resetLog();
        streamTopic.getMessageLogger().println(String.format("Closing message stream [%s]", streamTopic.getLabel()));
        if (!MqttMessageWorker.getInstance().containsExecutor(streamTopic)) {
            streamTopic.getMessageLogger().printWarning("CurrentClient for the following stream is missing [" + streamTopic.getUri() + "]");
        } else {
            MqttMessageWorker.getInstance().stopStream(streamTopic);
        }
        streamTopic.setHasTopicStatus(HASCO.INACTIVE);
        DateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        String endTime = isoFormat.format(new Date());
        //streamTopic.setEndedAtXsdWithMillis(endTime);
        streamTopic.getMessageLogger().println(String.format("Closed message stream [%s]", streamTopic.getUri()));
        streamTopic.save();
        MqttMessageWorker.getInstance().listStreamTopics().remove(streamTopic);
    }
    */

}
