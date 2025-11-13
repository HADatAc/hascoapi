package org.hascoapi.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.MessageTopic;
import org.hascoapi.entity.pojo.Stream;
import org.hascoapi.entity.pojo.StreamTopic;
import org.hascoapi.utils.Feedback;

public class IngestionLogger {

    private String log = "";
    private DataFile parent = null;
    private Stream stream = null;
    private StreamTopic streamTopic = null;
    private MessageTopic topic = null;

    public IngestionLogger(DataFile parent) {
        this.parent = parent;
    }

    public IngestionLogger(Stream stream) {
        this.stream = stream;
    }

    public IngestionLogger(StreamTopic streamTopic) {
        this.streamTopic = streamTopic;
    }

    public IngestionLogger(MessageTopic topic) {
        this.topic = topic;
    }

    public IngestionLogger(DataFile parent, String log) {
        this.parent = parent;
        this.log = log;
    }

    public IngestionLogger(Stream stream, String log) {
        this.stream = stream;
        this.log = log;
    }

    public IngestionLogger(MessageTopic topic, String log) {
        this.topic = topic;
        this.log = log;
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
        if (parent != null &&
                parent.getNamedGraph() != null &&
                !parent.getNamedGraph().isEmpty()) {
            parent.save();
        }
    }

    public void resetLog() {
        log = "";
        if (parent != null) {
            parent.save();
        }
    }

    public void addLine(String newLine) {
        log += (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(new Date()) + " " + newLine;
        if (parent != null) {
            parent.save();
        }
        if (stream != null) {
            stream.save();
        }
        if (streamTopic != null) {
            streamTopic.save();
        }
        if (topic != null) {
            topic.save();
        }
    }

    /**
     * Logs an exception using an error ID from the ErrorDictionary.
     */
    public void printExceptionById(String id) {
        System.out.println("ExceptioById with Id=[" + id + "]");
        printException(id + ": " + ErrorDictionary.getDetailById(id));
    }

    /**
     * Logs an exception with arguments replacing placeholders in the error message template.
     * Example: printExceptionByIdWithArgs("STR_00005", "ResponseOptions", "INS")
     */
    public void printExceptionByIdWithArgs(String id, Object... args) {
        String template = ErrorDictionary.getDetailById(id);
        if (template == null) {
            printException(id + ": [Unknown error ID]");
            return;
        }

        String formatted = String.format(template, args);
        printException(id + ": " + formatted);
    }

    public void printException(Exception exception) {
        System.out.println(exception.getMessage());
        addLine(Feedback.println(Feedback.WEB, "[ERROR] " + exception.getMessage()));
    }

    public void printException(String message) {
        addLine(Feedback.println(Feedback.WEB, "[ERROR] " + message));
    }

    public void printWarningById(String id) {
        printWarning(id + ": " + ErrorDictionary.getDetailById(id));
    }

    /**
     * Logs a warning with arguments replacing placeholders in the warning message template.
     * Example: printWarningByIdWithArgs("GBL_00003", "Deployments")
     */
    public void printWarningByIdWithArgs(String id, Object... args) {
        System.out.println(id + ": " + ErrorDictionary.getDetailById(id));
        String template = ErrorDictionary.getDetailById(id);
        if (template == null) {
            printWarning(id + ": [Unknown warning ID]");
            return;
        }

        String formatted = String.format(template, args);
        printWarning(id + ": " + formatted);
    }

    public void printWarning(String message) {
        System.out.println(message);
        addLine(Feedback.println(Feedback.WEB, "[WARNING] " + message));
    }

    public void println(String message) {
        System.out.println(message);
        addLine(Feedback.println(Feedback.WEB, "[LOG] " + message));
    }
}
