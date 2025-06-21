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
        System.out.println("Stop recording stream topic: " + streamTopic.getLabel());
        /* TIAGO EH AQUI QUE TEM DE FICAR A LOGICA DE PARAR A GRAVACAO, SE EH QUE ALGO TEM DE OCORRER */
        streamTopic.getMessageLogger().resetLog();
        streamTopic.getMessageLogger().println(String.format("Stopped recording of stream topic [%s]", streamTopic.getLabel()));
        streamTopic.save();
    }

}
