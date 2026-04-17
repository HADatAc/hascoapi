package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.DA;
import org.hascoapi.entity.pojo.GenericFind;
import org.hascoapi.entity.pojo.StudyObject;
import org.hascoapi.entity.pojo.StudyObjectCollection;
import org.hascoapi.entity.pojo.StudyRole;
import org.hascoapi.entity.pojo.VirtualColumn;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.HAScOMapper;
import org.hascoapi.vocabularies.HASCO;
import play.mvc.Controller;
import play.mvc.Result;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class StudyObjectAPI extends Controller {

    public static Result getStudyObjects(List<StudyObject> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No Study Object has been found", false));
        } else {
            ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL,HASCO.STUDY_OBJECT);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }             

    public Result getStudyObjectsBySOCWithPage(String socUri, int pageSize, int offset){
        List<StudyObject> results = StudyObject.findByCollectionWithPage(socUri, pageSize, offset);
        return getStudyObjects(results);
    }



    public Result getTotalStudyObjectsBySOC(String socUri){
        int totalElements = StudyObject.getNumberStudyObjectsByCollection(socUri) ;
        if (totalElements >= 0) {
            String totalElementsJSON = "{\"total\":" + totalElements + "}";
            return ok(ApiUtil.createResponse(totalElementsJSON, true));
        }
        return ok(ApiUtil.createResponse("query method getTotalElements() failed to retrieve total number of element", false));
    }

    /**
     * Count additional properties for objects in a SOC beyond the 5 base properties.
     * Base properties loaded during DSG ingestion:
     * - hasco:originalID
     * - rdf:type
     * - hasco:hasObjectScope
     * - hasco:hasTimeObjectScope
     * - hasco:hasSpaceObjectScope
     */
    public Result countAdditionalPropertiesBySOC(String socUri) {
        int additionalProperties = StudyObject.countAdditionalPropertiesByCollection(socUri);
        if (additionalProperties >= 0) {
            String responseJSON = "{\"additionalProperties\":" + additionalProperties + "}";
            return ok(ApiUtil.createResponse(responseJSON, true));
        }
        return ok(ApiUtil.createResponse("query method countAdditionalPropertiesBySOC() failed to count properties", false));
    }

}
