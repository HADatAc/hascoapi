// package org.hascoapi.ingestion.opcua;

// import java.util.Map;
// import java.util.concurrent.ConcurrentHashMap;
// import java.util.concurrent.atomic.AtomicReference;

// public class OpcUaMonitor {

//     private final Map<String, StreamMonitor> streamMonitors = new ConcurrentHashMap<>();

//     public void registerStream(String streamUri) {
//         streamMonitors.putIfAbsent(streamUri, new StreamTopicMonitor(streamUri));
//     }

//     public void removeStream(String streamUri) {
//         streamMonitors.remove(streamUri);
//     }

//     public void updateLatestValue(String streamUri, String value) {
//         StreamMonitor monitor = streamMonitors.get(streamUri);
//         if (monitor != null) {
//             monitor.setLatestValue(value);
//         }
//     }

//     public String getLatestValue(String streamUri) {
//         StreamMonitor monitor = streamMonitors.get(streamUri);
//         return monitor != null ? monitor.getLatestValue() : "Stream not found or not active";
//     }

//     private static class StreamMonitor {
//         private final String streamUri;
//         private final AtomicReference<String> latestValue = new AtomicReference<>("No data yet");

//         public StreamMonitor(String streamUri) {
//             this.streamUri = streamUri;
//         }

//         public void setLatestValue(String value) {
//             latestValue.set(value);
//         }

//         public String getLatestValue() {
//             return latestValue.get();
//         }
//     }
// }
