package org.hascoapi.ingestion.mqtt;

import java.lang.String;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

import org.hascoapi.ingestion.ValueGenerator;
import org.hascoapi.entity.pojo.Stream;
import org.hascoapi.entity.pojo.StreamTopic;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.vocabularies.HASCO;

public class MqttMessageAnnotation {

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
        DataFile archive;
        if (stream.getMessageArchiveId() == null || stream.getMessageArchiveId().isEmpty()) {
            Date date = new Date();
            String fileName = "DA-" + stream.getMessageName().replaceAll("/","_").replaceAll(".", "_") + ".json";
            archive = DataFile.create(fileName, "" , "", DataFile.PROCESSED);
            archive.setSubmissionTime(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(date));
            archive.save();
            stream.setMessageArchiveId(archive.getId());
            stream.getMessageLogger().println(String.format("Creating archive datafile " + fileName + " with id " + archive.getId()));
            stream.save();
        } else {
            archive = DataFile.findById(stream.getMessageArchiveId());
            streamTopic.getMessageLogger().println("Reusing archive datafile with id " + stream.getMessageArchiveId());
        }

        streamTopic.getMessageLogger().println(String.format("Message stream <%s> has labels <%s>", stream.getLabel(), streamTopic.getHeaders().toString()));

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
