package org.hascoapi.ingestion.opcua;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.stack.core.types.structured.*;
import org.hascoapi.entity.pojo.Stream;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class OpcUaSubscribe {

    private final OpcUaMonitor monitor;
    private final AtomicLong clientHandleCounter = new AtomicLong(1);

    public OpcUaSubscribe(OpcUaMonitor monitor) {
        this.monitor = monitor;
    }

    public void subscribe(Stream stream) throws Exception {
        OpcUaClient client = OpcUaClient.create("opc.tcp://" + stream.getMessageIP() + ":" + stream.getMessagePort());
        client.connect().get();

        UaSubscription subscription = client.getSubscriptionManager()
                .createSubscription(1000.0).get();

        monitor.registerStream(stream.getId());

        for (String nodeIdStr : stream.getMonitoredNodes()) {
            NodeId nodeId = parseNodeId(nodeIdStr);

            ReadValueId readValueId = new ReadValueId(
                nodeId,
                AttributeId.Value.uid(),
                null,
                QualifiedName.NULL_VALUE
            );

            long clientHandle = clientHandleCounter.getAndIncrement();

            MonitoringParameters parameters = new MonitoringParameters(
                Unsigned.uint(clientHandle),
                1000.0, null, Unsigned.uint(10), true
            );

            MonitoredItemCreateRequest request = new MonitoredItemCreateRequest(
                readValueId, MonitoringMode.Reporting, parameters
            );

            UaSubscription.ItemCreationCallback onItemCreated =
                (item, id) -> item.setValueConsumer((monitoredItem, value) -> {
                    Variant variant = value.getValue();
                    if (variant != null) {
                        String result = variant.getValue().toString();
                        System.out.println("📡 Received update for stream " + stream.getId() + ": " + result);
                        monitor.updateLatestValue(stream.getId(), result);
                    }
                });

            subscription.createMonitoredItems(
                TimestampsToReturn.Both, List.of(request), onItemCreated
            ).get();
        }
    }

    private NodeId parseNodeId(String id) {
        if (id.startsWith("ns=")) return NodeId.parse(id);
        return new NodeId(2, id);
    }
}
