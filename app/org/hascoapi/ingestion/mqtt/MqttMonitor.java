package org.hascoapi.ingestion.mqtt;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class MqttMonitor {

    private final ExecutorService asyncStreamTopicExecutor = Executors.newCachedThreadPool();
    private final Map<String, StreamTopicMonitor> streamTopicMonitors = new ConcurrentHashMap<>();

    public void addStreamTopic(String streamTopicUri) {
        StreamTopicMonitor monitor = new StreamTopicMonitor(streamTopicUri);
        streamTopicMonitors.put(streamTopicUri, monitor);
        monitor.start();
    }

    public void stopStreamTopic(String streamTopicUri) {
        StreamTopicMonitor monitor = streamTopicMonitors.remove(streamTopicUri);
        if (monitor != null) {
            monitor.stop();
        }
    }

    // This is the sync access point: consults latest value for a given stream
    public String getLatestValue(String streamTopicUri) {
        StreamTopicMonitor monitor = streamTopicMonitors.get(streamTopicUri);
        return monitor != null ? monitor.getLatestValue() : "Stream topic not found";
    }


    public void updateLatestValue(String streamTopicUri, String message) {
        StreamTopicMonitor monitor = streamTopicMonitors.get(streamTopicUri);
        if (monitor != null) {
            monitor.setLatestValue(message);
        }
    }
    
    public void shutdown() {
        for (StreamTopicMonitor monitor : streamTopicMonitors.values()) {
            monitor.stop();
        }
        asyncStreamTopicExecutor.shutdownNow();
    }    

    // This is the sync access point: consults latest value for a given stream
    //public void setLatestValue(String streamTopicUri, String message) {
    //    StreamTopicMonitor monitor = streamTopicMonitors.get(streamTopicUri);
    //    monitor.start(message);
    //}

    // Inner class representing a single stream
    private class StreamTopicMonitor {
        private final String streamTopicUri;
        private final AtomicReference<String> latestValue = new AtomicReference<>("No data yet");
        private Future<?> dataProducerTask;

        StreamTopicMonitor(String streamTopicUri) {
            this.streamTopicUri = streamTopicUri;
        }

        void setLatestValue(String message) {
            latestValue.set(message);
        }
        
        String getLatestValue() {
            return latestValue.get();
        }

        void start() {
            // Stop existing task if running
            if (dataProducerTask != null && !dataProducerTask.isDone()) {
                dataProducerTask.cancel(true);
            }
        
            dataProducerTask = asyncStreamTopicExecutor.submit(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        TimeUnit.SECONDS.sleep((long) (Math.random() * 60));
                        //System.out.printf("[%s] Updated value: %s%n", streamTopicUri, latestValue.get());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        void stop() {
            if (dataProducerTask != null) {
                dataProducerTask.cancel(true);
            }
        }

    }

    /* 
    // Test/demo
    public static void main(String[] args) throws InterruptedException {
        MultiStreamService service = new MultiStreamService();
        service.addStream("stream1");
        service.addStream("stream2");

        // Periodically consult the latest values synchronously
        for (int i = 0; i < 10; i++) {
            TimeUnit.SECONDS.sleep(10);
            System.out.println("[SYNC CHECK] stream1 = " + service.getLatestValue("stream1"));
            System.out.println("[SYNC CHECK] stream2 = " + service.getLatestValue("stream2"));
        }

        service.stopStream("stream1");
        service.stopStream("stream2");
    }
        */

}
