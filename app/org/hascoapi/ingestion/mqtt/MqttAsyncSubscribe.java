package org.hascoapi.ingestion.mqtt;

import java.io.File;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.hascoapi.ingestion.ValueGenerator;
import org.hascoapi.entity.pojo.Stream;
import org.hascoapi.entity.pojo.StreamTopic;

public class MqttAsyncSubscribe implements MqttCallback {

    private MqttAsyncClient      client;
    private Stream               stream;
    private StreamTopic          streamTopic;
    private String 				 brokerUrl;
    private boolean 			 quietMode;
    private MqttConnectOptions 	 conOpt;
    private boolean 			 clean;
    private int                  qos;
    private String               password;
    private String               userName;
    private List<String>         respPayload;
    private String               plainPayload;
    private long                 totalMessages;
    private int					 partialCounter;
    private File                 file;
    private ValueGenerator       gen;

    public String getPlainPayLoad() {
        return plainPayload;
    }

    public String getClientId() {
        if (client == null) {
            return "";
        }
        return client.getClientId();
    }

    public static void exec(StreamTopic streamTopic, ValueGenerator generator) {

        if (streamTopic == null || streamTopic.getStreamUri() == null || streamTopic.getStreamUri().isEmpty()) {
            return;
        }

        // retrieve stream of streamTopic
        Stream stream = Stream.find(streamTopic.getStreamUri());

        // Default settings:
        String broker        = stream.getMessageIP();
        int port             = Integer.parseInt(stream.getMessagePort());
        boolean ssl          = false;
        String password      = null;
        String userName      = null;

        String protocol = "tcp://";

        String url = protocol + broker + ":" + port;

        ExecutorService executor = Executors.newFixedThreadPool(1);

        // Runnable, return void, nothing, submit and run the task async
        executor.submit((Runnable) () -> {

            try {
                new MqttAsyncSubscribe(streamTopic, generator, url, userName, password);
                TimeUnit.MILLISECONDS.sleep(300);
            } catch(MqttException me) {
                // Display full details of any exception that occurs
                streamTopic.getMessageLogger().printException("MQTT Exception: reason : [" + me.getReasonCode() + "]");
                streamTopic.getMessageLogger().printException("MQTT Exception: msg    : [" + me.getMessage() + "]");
                streamTopic.getMessageLogger().printException("MQTT Exception: loc    : [" + me.getLocalizedMessage() + "]");
                streamTopic.getMessageLogger().printException("MQTT Exception: cause  : [" + me.getCause() + "]");
                streamTopic.getMessageLogger().printException("MQTT Exception: excep  : [" + me + "]");
                me.printStackTrace();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        });

        MqttMessageWorker.getInstance().addExecutor(streamTopic, executor);

    }

    public MqttAsyncSubscribe(StreamTopic streamTopic, ValueGenerator generator, String brokerUrl, String userName, String password) throws MqttException {
        this.streamTopic    = streamTopic;
        this.brokerUrl      = brokerUrl;
        this.quietMode      = false;
        this.clean 	        = true;
        this.password       = password;
        this.userName       = userName;
        this.qos            = 0;

        String clientId = UUID.randomUUID().toString();

        if (clientId == null || clientId.isEmpty()) {
            streamTopic.getMessageLogger().printException("AsyncSubscribe: client is null");
            return;
        }
        if (clientId != null && !clientId.isEmpty()) {
            try {
                // Construct the connection options object that contains connection parameters
                // such as cleanSession and LWT
                conOpt = new MqttConnectOptions();
                conOpt.setCleanSession(clean);
                if(password != null ) {
                    conOpt.setPassword(this.password.toCharArray());
                }
                if(userName != null) {
                    conOpt.setUserName(this.userName);
                }

                // Construct an MQTT blocking mode client
                //client = new MqttClient(this.brokerUrl,clientId, dataStore);
                //client = new MqttAsyncClient(this.brokerUrl,clientId);
                client = new MqttAsyncClient(this.brokerUrl,clientId);

                // Set this wrapper as the callback handler
                client.setCallback(this);

                MqttMessageWorker.getInstance().addClient(streamTopic, client);

            } catch (MqttException e) {
                e.printStackTrace();
                streamTopic.getMessageLogger().printException("Unable to set up client: "+e.toString());
                //System.exit(1);
            }
        }

        if (generator == null) {
            System.out.println("[WARNING] MqttAsyncSubscribe: no generator object provided when subscribing streamTopic " + streamTopic.getUri());
        } else {
            MqttMessageWorker.getInstance().addStreamGenerator(streamTopic.getUri(), generator);
            this.gen = generator;
        }

        // Connect to the MQTT server
        IMqttToken token = client.connect(conOpt);
        token.waitForCompletion();
        log("Connected to " + brokerUrl + " with client ID " + client.getClientId());
        System.out.println("Connected to " + brokerUrl + " with client ID " + client.getClientId());

        log("Subscribing to topic \"" + streamTopic.getLabel() + "\" qos " + qos);
        System.out.println("Subscribing to topic \"" + streamTopic.getLabel() + "\" qos " + qos);

        client.subscribe(streamTopic.getLabel() + "/#", qos);

        totalMessages = streamTopic.getTotalReceivedMessages();
        partialCounter = 0;

    }

    /**
     * Utility method to handle logging. If 'quietMode' is set, this method does nothing
     * @param message the message to log
     */
    private void log(String message) {
        if (!quietMode) {
            System.out.println(message);
        }
    }

    /****************************************************************/
    /* Methods to implement the MqttCallback interface              */
    /****************************************************************/

    /**
     * @see MqttCallback#connectionLost(Throwable)
     */
    public void connectionLost(Throwable cause) {
        log("Connection to " + brokerUrl + " lost!" + cause);
    }

    /**
     * @see MqttCallback#deliveryComplete(IMqttDeliveryToken)
     */
    public void deliveryComplete(IMqttDeliveryToken token) {
    }

    /**
     * @see MqttCallback#messageArrived(String, MqttMessage)
     */
    public void messageArrived(String topic, MqttMessage message) throws MqttException {
        String time = new Timestamp(System.currentTimeMillis()).toString();
        plainPayload = new String(message.getPayload());
        String resp = "  Time:\t" +time +
                "  Topic:\t" + topic +
                "  Message:\t" + plainPayload +
                "  QoS:\t" + message.getQos();

        /*
         *    Compute totals. Save ingested content after reading 500 messages
         */
        totalMessages = totalMessages + 1;
        streamTopic.setTotalReceivedMessages(totalMessages);
        partialCounter = partialCounter + 1;
        if (partialCounter >= 300) {
            partialCounter = 0;
            System.out.println("Received " + totalMessages + " messages.");

            try {
                // ************>>>>>>>>>>> TO DO
                //gen.commitObjectsToSolr(gen.getObjects());
            } catch (Exception e) {
                e.printStackTrace();
                gen.getLogger().printException(gen.getErrorMsg(e));
            }

            streamTopic.save();
        }

        //System.out.println(resp);

		/*
		if (file != null) {
			try {
				FileUtils.writeStringToFile(file, resp + "\n", "utf-8", true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		 */

        /*
         *   Ingest message content
         */

        try {
            int temp = (int) totalMessages;
            if (MqttMessageWorker.processMessage(streamTopic, topic, plainPayload, temp) != null) {
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
 

        if (Thread.currentThread().isInterrupted()) {
            //System.out.println("Thread INTERRUPTED");
            MqttAsyncClient client = MqttMessageWorker.getInstance().getClient(streamTopic.getLabel());
            IMqttToken token1 = client.unsubscribe(streamTopic.getLabel());
            token1.waitForCompletion();
            IMqttToken token2 = client.disconnect();
            token2.waitForCompletion();
        } else {
            //System.out.println("Thread not interrrupted");
        }
    }

    /****************************************************************/
    /* End of MqttCallback methods                                  */
    /****************************************************************/

}

