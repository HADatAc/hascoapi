package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.StreamTopic;
import org.hascoapi.entity.pojo.Study;
import org.hascoapi.ingestion.mqtt.MqttMessageWorker;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.HAScOMapper;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.VSTOI;
import play.mvc.Controller;
import play.mvc.Result;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class StreamTopicAPI extends Controller {

    public static Result getStreamTopics(List<StreamTopic> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No StreamTopic has been found", false));
        } else {
            ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL,HASCO.STREAM_TOPIC);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

    public Result findActive() {
        List<StreamTopic> streamTopics = MqttMessageWorker.getInstance().listStreamTopics();
        return getStreamTopics(streamTopics);
    }

    public Result subscribe(String topicUri) {
        if (topicUri == null || topicUri.isEmpty()) {
            return ok(ApiUtil.createResponse("No valid topicUri has been provided", false));
        }
        if (MqttMessageWorker.getInstance().addStreamTopicToWorker(topicUri)) {
            return ok(ApiUtil.createResponse("StreamTopic has been subscribed", true));
        }     
        return ok(ApiUtil.createResponse("Failed to subscribe to StreamTopic", false));   
    }

    public Result unsubscribe(String topicUri) {
        if (topicUri == null || topicUri.isEmpty()) {
            return ok(ApiUtil.createResponse("No valid topicUri has been provided", false));
        }
        if (MqttMessageWorker.getInstance().removeStreamTopicFromWorker(topicUri)) {
            return ok(ApiUtil.createResponse("StreamTopic has been unsubscribed", true));
        }     
        return ok(ApiUtil.createResponse("Failed to unsubscribe requested StreamTopic", false));   
    }

    public Result setStatus(String topicUri, String status) {
        if (topicUri == null || topicUri.isEmpty()) {
            return ok(ApiUtil.createResponse("No valid topicUri has been provided", false));
        }
        if (status == null || status.isEmpty()) {
            return ok(ApiUtil.createResponse("No valid status has been provided", false));
        }
        if (MqttMessageWorker.getInstance().setStatus(topicUri, status)) {
            return ok(ApiUtil.createResponse("StreamTopic " + topicUri + " has set to status " + status, true));
        }     
        return ok(ApiUtil.createResponse("Failed to unsubscribe requested StreamTopic", false));   
    }



}
