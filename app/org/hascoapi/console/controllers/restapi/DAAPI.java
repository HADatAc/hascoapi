package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.DA;
import org.hascoapi.entity.pojo.Stream;
import org.hascoapi.entity.pojo.StreamTopic;
import org.hascoapi.entity.pojo.Study;
import org.hascoapi.transform.Renderings;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.HAScOMapper;
import org.hascoapi.vocabularies.SIO;
import org.hascoapi.vocabularies.HASCO;
import play.mvc.Controller;
import play.mvc.Result;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class DAAPI extends Controller {

    private Result createDAResult(DA da) {
        da.save();
        return ok(ApiUtil.createResponse("DA <" + da.getUri() + "> has been CREATED.", true));
    }

    public Result createDA(String json) {
        if (json == null || json.equals("")) {
            return ok(ApiUtil.createResponse("No json content has been provided.", false));
        }
        //System.out.println("(UnitAPI) Value of json in createUnit: [" + json + "]");
        ObjectMapper objectMapper = new ObjectMapper();
        DA newDA;
        try {
            //convert json string to Unit unitance
            newDA  = objectMapper.readValue(json, DA.class);
        } catch (Exception e) {
            //System.out.println("(UnitAPI) Failed to parse json for [" + json + "]");
            return ok(ApiUtil.createResponse("Failed to parse json.", false));
        }
        return createDAResult(newDA);
    }

    public static Result getDAs(List<DA> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No DA has been found", false));
        } else {
            //for (DD dd: results) {
            //    System.out.println(dd.getLabel() + "  [" + dd.getHasDataFile() + "]");
            //}
            ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL,HASCO.DATA_ACQUISITION);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

    public Result findDAsByStudy(String uri, int pagesize, int offset) {
        if (uri == null || uri.isEmpty()) {
            return ok(ApiUtil.createResponse("DAAPI: No study uri has been provided to retrieve data acquisitions", false));
        }
        Study study = Study.find(uri);
        if (study == null) {
            return ok(ApiUtil.createResponse("DAAPI: No study has been retrieved with URI=[" + uri + "]", false));
        }
        List<DA> das = DA.findByStudy(study, pagesize, offset);
        return DAAPI.getDAs(das);
    }

    public Result findTotalDAsByStudy(String uri) {
        if (uri == null || uri.isEmpty()) {
            return ok(ApiUtil.createResponse("DAAPI: No study uri has been provided to retrieve data acquisitions", false));
        }
        Study study = Study.find(uri);
        if (study == null) {
            return ok(ApiUtil.createResponse("DAAPI: No study has been retrieved with URI=[" + uri + "]", false));
        }
        int totalElements = DA.findTotalByStudy(study);
        if (totalElements >= 0) {
            String totalElementsJSON = "{\"total\":" + totalElements + "}";
            return ok(ApiUtil.createResponse(totalElementsJSON, true));
        }     
        return ok(ApiUtil.createResponse("DAAPI: Query method findTotalDAsByStudy() failed to retrieve total number of element", false));   
    }

    public Result findDAsByStream(String uri, int pagesize, int offset) {
        if (uri == null || uri.isEmpty()) {
            return ok(ApiUtil.createResponse("DAAPI: No stream uri has been provided to retrieve data acquisitions", false));
        }
        Stream stream = Stream.find(uri);
        if (stream == null) {
            return ok(ApiUtil.createResponse("DAAPI: No stream has been retrieved with URI=[" + uri + "]", false));
        }
        List<DA> das = DA.findByStream(stream, pagesize, offset);
        return DAAPI.getDAs(das);
    }

    public Result findTotalDAsByStream(String uri) {
        if (uri == null || uri.isEmpty()) {
            return ok(ApiUtil.createResponse("DAAPI: No stream uri has been provided to retrieve data acquisitions", false));
        }
        Stream stream = Stream.find(uri);
        if (stream == null) {
            return ok(ApiUtil.createResponse("DAAPI: No stream has been retrieved with URI=[" + uri + "]", false));
        }
        int totalElements = DA.findTotalByStream(stream);
        if (totalElements >= 0) {
            String totalElementsJSON = "{\"total\":" + totalElements + "}";
            return ok(ApiUtil.createResponse(totalElementsJSON, true));
        }     
        return ok(ApiUtil.createResponse("DAAPI: Query method findTotalDAsByStream() failed to retrieve total number of element", false));   
    }

    public Result findDAsByStreamTopic(String uri, int pagesize, int offset) {
        if (uri == null || uri.isEmpty()) {
            return ok(ApiUtil.createResponse("DAAPI: No stream topic uri has been provided to retrieve data acquisitions", false));
        }
        StreamTopic streamTopic = StreamTopic.find(uri);
        if (streamTopic == null) {
            return ok(ApiUtil.createResponse("DAAPI: No stream topic has been retrieved with URI=[" + uri + "]", false));
        }
        List<DA> das = DA.findByStreamTopic(streamTopic, pagesize, offset);
        return DAAPI.getDAs(das);
    }

    public Result findTotalDAsByStreamTopic(String uri) {
        if (uri == null || uri.isEmpty()) {
            return ok(ApiUtil.createResponse("DAAPI: No stream topic uri has been provided to retrieve data acquisitions", false));
        }
        StreamTopic streamTopic = StreamTopic.find(uri);
        if (streamTopic == null) {
            return ok(ApiUtil.createResponse("DAAPI: No stream topic has been retrieved with URI=[" + uri + "]", false));
        }
        int totalElements = DA.findTotalByStreamTopic(streamTopic);
        if (totalElements >= 0) {
            String totalElementsJSON = "{\"total\":" + totalElements + "}";
            return ok(ApiUtil.createResponse(totalElementsJSON, true));
        }     
        return ok(ApiUtil.createResponse("DAAPI: Query method findTotalDAsByStreamTopic() failed to retrieve total number of element", false));   
    }

}
