// package org.hascoapi.ingestion.opcua;

// import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
// import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
// import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
// import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
// import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
// import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
// import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
// import org.eclipse.milo.opcua.sdk.client.api.subscriptions.MonitoredItemCreateRequest;
// import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
// import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
// import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters;

// import java.util.Collections;
// import java.util.concurrent.CompletableFuture;

// public class OpcUaObjectsAsyncSubscribe {

//     public static void exec(String objectUri, OpcUaMonitor monitor) {
//         try {
//             // Cria um cliente OPC UA
//             OpcUaClient client = OpcUaClient.create(objectUri);
//             client.connect().get();

//             // Exemplo de NodeId a subscrever — idealmente extraído do objectUri
//             // Substituir pelo parsing real de NodeId se necessário
//             NodeId nodeId = parseNodeIdFromUri(objectUri);

//             // Cria uma subscription
//             UaSubscription subscription = client.getSubscriptionManager()
//                     .createSubscription(1000.0).get(); // 1000 ms sampling

//             // Cria monitored item
//             ReadValueId readValueId = new ReadValueId(
//                 nodeId,
//                 org.eclipse.milo.opcua.stack.core.types.builtin.Identifiers.Value,
//                 null,
//                 null
//             );

//             UInteger clientHandle = subscription.getSubscriptionId();
//             MonitoringParameters parameters = new MonitoringParameters(
//                 clientHandle,
//                 1000.0,     // sampling interval
//                 null,       // filter, null = default
//                 UInteger.valueOf(10),   // queue size
//                 true        // discard oldest
//             );

//             MonitoredItemCreateRequest request = new MonitoredItemCreateRequest(
//                 readValueId,
//                 org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode.Reporting,
//                 parameters
//             );

//             UaSubscription.ItemCreationCallback onItemCreated = (item, id) -> {
//                 item.setValueConsumer((UaMonitoredItem monitoredItem, DataValue value) -> {
//                     Variant variant = value.getValue();
//                     String valueStr = variant != null ? variant.getValue().toString() : "null";
//                     monitor.updateLatestValue(objectUri, valueStr);
//                     System.out.println("[OPC UA] Value received from " + objectUri + " = " + valueStr);
//                 });
//             };

//             subscription.createMonitoredItems(
//                 TimestampsToReturn.Both,
//                 Collections.singletonList(request),
//                 onItemCreated
//             ).get();

//         } catch (Exception e) {
//             System.err.println("[ERROR] Failed to subscribe OPC UA object: " + objectUri);
//             e.printStackTrace();
//         }
//     }

//     private static NodeId parseNodeIdFromUri(String objectUri) {
//         // Exemplo: opc.tcp://localhost:4840/ns=2;s=MyDevice.MyValue
//         try {
//             String[] parts = objectUri.split("/ns=");
//             if (parts.length < 2) {
//                 throw new IllegalArgumentException("Invalid OPC UA object URI: " + objectUri);
//             }
//             String[] nodeIdParts = parts[1].split(";");
//             int namespace = Integer.parseInt(nodeIdParts[0]);
//             String identifier = nodeIdParts[1].replace("s=", "");
//             return new NodeId(namespace, identifier);
//         } catch (Exception e) {
//             throw new RuntimeException("Unable to parse NodeId from URI: " + objectUri, e);
//         }
//     }
// }
