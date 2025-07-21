package org.hascoapi.ingestion.opcua;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.hascoapi.entity.pojo.Stream;

import java.util.concurrent.CompletableFuture;

public class OpcUaReader {

    private OpcUaClient client;
    private String endpoint;
    private boolean connected;

    public OpcUaReader(Stream stream) throws Exception {
        String ip = stream.getMessageIP();      // Ex: 52.211.230.190
        String port = stream.getMessagePort();  // Ex: 4840
        this.endpoint = "opc.tcp://" + ip + ":" + port;

        client = OpcUaClient.create(endpoint);
        client.connect().get();
        connected = true;
    }

    public boolean isConnected() {
        return connected;
    }

    public void disconnect() throws Exception {
        if (client != null && connected) {
            client.disconnect().get();
            connected = false;
        }
    }

    public String readValue(String nodeIdString) throws Exception {
        NodeId nodeId = parseNodeId(nodeIdString);
        CompletableFuture<DataValue> valueFuture = client.readValue(0, null, nodeId);
        DataValue value = valueFuture.get();

        if (value == null || value.getValue() == null) {
            return null;
        }

        Variant variant = value.getValue();
        return variant.getValue().toString();
    }

    public static NodeId parseNodeId(String nodeIdStr) {
        // Ex: ns=1;s=E86BEAF89F34_temperature_dht11_C
        if (nodeIdStr.startsWith("ns=")) {
            return NodeId.parse(nodeIdStr);
        }
        return new NodeId(2, nodeIdStr); // default namespace
    }
}
