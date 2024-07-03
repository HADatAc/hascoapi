package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.Organization;
import org.hascoapi.entity.pojo.Study;
import org.hascoapi.entity.pojo.StudyObject;
import org.hascoapi.entity.pojo.StudyObjectCollection;
import org.hascoapi.entity.pojo.VirtualColumn;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.HAScOMapper;
import org.hascoapi.vocabularies.HASCO;
import play.mvc.Controller;
import play.mvc.Result;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class StudyAPI extends Controller {

    public static Result getStudies(List<Study> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No Study has been found", false));
        } else {
            ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL,HASCO.STUDY);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

    public Result findSOCs(String uri, int pageSize, int offset) {
        List<StudyObjectCollection> results = Study.findStudyObjectCollections(uri, pageSize, offset);
        return StudyObjectCollectionAPI.getStudyObjectCollections(results);
       
    }

    public Result findTotalSOCs(String uri) {
        int totalElements = Study.findTotalStudyObjectCollections(uri);
        if (totalElements >= 0) {
            String totalElementsJSON = "{\"total\":" + totalElements + "}";
            return ok(ApiUtil.createResponse(totalElementsJSON, true));
        }     
        return ok(ApiUtil.createResponse("Query method findTotalStudyObjectCollections() failed to retrieve total number of element", false));   
    }

    public Result findVCs(String studyuri) {
        List<VirtualColumn> results = VirtualColumn.findVCsByStudy(studyuri);
        return VirtualColumnAPI.getVirtualColumns(results);
       
    }

    public Result findTotalVCs(String studyuri) {
        int totalElements = VirtualColumn.findTotalVCsByStudy(studyuri);
        if (totalElements >= 0) {
            String totalElementsJSON = "{\"total\":" + totalElements + "}";
            return ok(ApiUtil.createResponse(totalElementsJSON, true));
        }     
        return ok(ApiUtil.createResponse("Query method findTotalVCss() failed to retrieve total number of element", false));   
    }

    public Result findTotalSOs(String studyuri) {
        int totalElements = StudyObject.getNumberStudyObjectsByStudy(studyuri);
        if (totalElements >= 0) {
            String totalElementsJSON = "{\"total\":" + totalElements + "}";
            return ok(ApiUtil.createResponse(totalElementsJSON, true));
        }     
        return ok(ApiUtil.createResponse("Query method findTotalSOs() failed to retrieve total number of element", false));   
    }
    
    
}
