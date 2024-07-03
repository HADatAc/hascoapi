package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.StudyObjectCollection;
import org.hascoapi.entity.pojo.VirtualColumn;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.HAScOMapper;
import org.hascoapi.vocabularies.HASCO;
import play.mvc.Controller;
import play.mvc.Result;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class StudyObjectCollectionAPI extends Controller {

    public static Result getStudyObjectCollections(List<StudyObjectCollection> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No Study Object Collection has been found", false));
        } else {
            ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL,HASCO.STUDY_OBJECT_COLLECTION);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

    public Result getSOCsByStudy(String studyUri){
        List<StudyObjectCollection> results = StudyObjectCollection.findStudyObjectCollectionsByStudy(studyUri);
        return getStudyObjectCollections(results);
    }

    public Result findTotalSOCsByStudy(String studyuri) {
        int totalElements = StudyObjectCollection.findTotalStudyObjectCollectionsByStudy(studyuri);
        if (totalElements >= 0) {
            String totalElementsJSON = "{\"total\":" + totalElements + "}";
            return ok(ApiUtil.createResponse(totalElementsJSON, true));
        }     
        return ok(ApiUtil.createResponse("Query method findTotalSOCsByStudy() failed to retrieve total number of SOCs by study", false));   
    }

}
