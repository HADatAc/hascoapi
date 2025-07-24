package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.OpcUaObject;
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

public class OpcUaObjectAPI extends Controller {

    public static Result getOpcUaObjects(List<OpcUaObject> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No OpcUaObject has been found", false));
        } else {
            ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL,HASCO.STREAM_TOPIC);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

    public Result findActive() {
        List<OpcUaObject> activeObjects = OpcUaObjectsWorker.getInstance().listOpcUaObjects();
        return ok(ApiUtil.createResponse(activeObjects, true));
    }
    public Result subscribe(String objectUri) {
        if (objectUri == null || objectUri.isEmpty()) {
            return ok(ApiUtil.createResponse("Invalid objectUri", false));
        }
        if (OpcUaObjectsWorker.getInstance().addObjectToWorker(objectUri)) {
            return ok(ApiUtil.createResponse("Subscribed to OPC UA object", true));
        }
        return ok(ApiUtil.createResponse("Failed to subscribe", false));
    }

    public Result unsubscribe(String objectUri) {
        if (objectUri == null || objectUri.isEmpty()) {
            return ok(ApiUtil.createResponse("Invalid objectUri", false));
        }
        if (OpcUaObjectsWorker.getInstance().removeObjectFromWorker(objectUri)) {
            return ok(ApiUtil.createResponse("Unsubscribed from OPC UA object", true));
        }
        return ok(ApiUtil.createResponse("Failed to unsubscribe", false));
    }

    public Result setStatus(String objectUri, String status) {
        if (objectUri == null || objectUri.isEmpty()) {
            return ok(ApiUtil.createResponse("No valid objectUri has been provided", false));
        }
        if (status == null || status.isEmpty()) {
            return ok(ApiUtil.createResponse("No valid status has been provided", false));
        }
        if (OpcUaObjectsWorker.getInstance().setStatus(objectUri, status)) {
            return ok(ApiUtil.createResponse("OPC UA object " + objectUri + " has set to status " + status, true));
        }     
        return ok(ApiUtil.createResponse("Failed to unsubscribe requested OPC UA object", false));   
    }

    public Result getLatestValue(String objectUri) {
        if (objectUri == null || objectUri.isEmpty()) {
            return ok(ApiUtil.createResponse("Invalid objectUri", false));
        }
        return ok(ApiUtil.createResponse(OpcUaObjectsWorker.getInstance().getMonitor().getLatestValue(objectUri), true));
    }

}