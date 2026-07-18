package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.GenericFind;
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

public class StudyObjectCollectionAPI extends Controller {

    public static Result getStudyObjectCollections(List<StudyObjectCollection> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No Study Object Collection has been found", false));
        } else {
            ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.ESSENTIAL,HASCO.STUDY_OBJECT_COLLECTION);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

    /**
     *   GET ELEMENTS BY MANAGER EMAIL AND SOC WITH PAGE
     */
    public Result getElementsByManagerEmailBySOC(String studyobjectcollectionuri, String elementtype, String manageremail, int pagesize, int offset) {
        if (manageremail == null || manageremail.isEmpty()) {
            return ok(ApiUtil.createResponse("No Manager Email has been provided", false));
        }
        // Use '_' wildcard to get all elements regardless of manager
        if (manageremail.equals("_")) {
            return getElementsBySOC(studyobjectcollectionuri, elementtype, pagesize, offset);
        }
        if (elementtype.equals("studyobject")) {
            GenericFind<StudyObject> query = new GenericFind<StudyObject>();
            List<StudyObject> results = query.findByManagerEmailWithPagesBySOC(StudyObject.class, studyobjectcollectionuri, manageremail, pagesize, offset);
            return StudyObjectAPI.getStudyObjects(results);
        }  
        return ok("[getElementsByManagerEmailByStudy] No valid element type.");
    }

    /**
     *   GET ELEMENTS BY SOC WITH PAGE
     */
    public Result getElementsBySOC(String studyobjectcollectionuri, String elementtype, int pagesize, int offset) {
        if (elementtype.equals("studyobject")) {
            GenericFind<StudyObject> query = new GenericFind<StudyObject>();
            List<StudyObject> results = query.findWithPagesBySOC(StudyObject.class, studyobjectcollectionuri, pagesize, offset);
            return StudyObjectAPI.getStudyObjects(results);
        }  
        return ok("[getElementsByStudy] No valid element type.");
    }

    public Result getTotalElementsByManagerEmailBySOC(String studyobjectcollectionuri, String elementtype, String manageremail){
        //System.out.println("SIRElementAPI: getTotalElementsByManagerEmailByStudy");
        if (elementtype == null || elementtype.isEmpty()) {
            return ok(ApiUtil.createResponse("No elementtype has been provided", false));
        }
        // Use '_' wildcard to get total count regardless of manager
        if (manageremail != null && manageremail.equals("_")) {
            return getTotalElementsBySOC(studyobjectcollectionuri, elementtype);
        }
        Class clazz = GenericFind.getElementClass(elementtype);
        if (clazz == null) {        
            return ok(ApiUtil.createResponse("[" + elementtype + "] is not a valid elementtype", false));
        }
        int totalElements = totalElements = GenericFind.findTotalByManagerEmailBySOC(clazz, studyobjectcollectionuri, manageremail);
        if (totalElements >= 0) {
            String totalElementsJSON = "{\"total\":" + totalElements + "}";
            return ok(ApiUtil.createResponse(totalElementsJSON, true));
        }
        return ok(ApiUtil.createResponse("query method getTotalElementsByManagerEmailBySOC() failed to retrieve total number of element", false));
    }

    public Result getTotalElementsBySOC(String studyobjectcollectionuri, String elementtype){
        //System.out.println("SIRElementAPI: getTotalElementsByManagerEmailByStudy");
        if (elementtype == null || elementtype.isEmpty()) {
            return ok(ApiUtil.createResponse("No elementtype has been provided", false));
        }
        Class clazz = GenericFind.getElementClass(elementtype);
        if (clazz == null) {        
            return ok(ApiUtil.createResponse("[" + elementtype + "] is not a valid elementtype", false));
        }
        int totalElements = totalElements = GenericFind.findTotalBySOC(clazz, studyobjectcollectionuri);
        if (totalElements >= 0) {
            String totalElementsJSON = "{\"total\":" + totalElements + "}";
            return ok(ApiUtil.createResponse(totalElementsJSON, true));
        }
        return ok(ApiUtil.createResponse("query method getTotalElementsBySOC() failed to retrieve total number of element", false));
    }

    public Result getSOCsByStudy(String studyUri){
        List<StudyObjectCollection> results = StudyObjectCollection.findStudyObjectCollectionsByStudyFlexible(studyUri);
        return getStudyObjectCollections(results);
    }

    /**
     *   GET SOCS BY STUDY WITH PAGINATION (no email filter - all SOCs visible to everyone)
     */
    public Result getSOCsByStudyWithPage(String studyUri, int pagesize, int offset) {
        List<StudyObjectCollection> results = StudyObjectCollection.findStudyObjectCollectionsByStudyFlexibleWithPage(studyUri, pagesize, offset);
        return getStudyObjectCollections(results);
    }

    /**
     *   GET TOTAL SOCS BY STUDY (no email filter - all SOCs visible to everyone)
     */
    public Result getTotalSOCsByStudy(String studyUri) {
        int totalElements = StudyObjectCollection.findTotalStudyObjectCollectionsByStudyFlexible(studyUri);
        if (totalElements >= 0) {
            String totalElementsJSON = "{\"total\":" + totalElements + "}";
            return ok(ApiUtil.createResponse(totalElementsJSON, true));
        }
        return ok(ApiUtil.createResponse("Query method getTotalSOCsByStudy() failed to retrieve total number of SOCs", false));
    }

    /**
     *   GET SOCS BY MANAGER EMAIL AND STUDY WITH PAGE (for backward compatibility)
     */
    public Result getSOCsByManagerEmailByStudy(String studyUri, String manageremail, int pagesize, int offset) {
        if (manageremail == null || manageremail.isEmpty() || manageremail.equals("_")) {
            // Use wildcard - return all SOCs for this study with pagination
            return getSOCsByStudyWithPage(studyUri, pagesize, offset);
        }
        // Filter by manager email
        List<StudyObjectCollection> results = StudyObjectCollection.findStudyObjectCollectionsByStudyAndManagerEmailWithPage(studyUri, manageremail, pagesize, offset);
        return getStudyObjectCollections(results);
    }

    /**
     *   GET TOTAL SOCS BY MANAGER EMAIL AND STUDY (for backward compatibility)
     */
    public Result getTotalSOCsByManagerEmailByStudy(String studyUri, String manageremail) {
        if (manageremail == null || manageremail.isEmpty() || manageremail.equals("_")) {
            // Use wildcard - count all SOCs for this study
            return getTotalSOCsByStudy(studyUri);
        }
        // Filter by manager email
        int totalElements = StudyObjectCollection.findTotalStudyObjectCollectionsByStudyAndManagerEmail(studyUri, manageremail);
        if (totalElements >= 0) {
            String totalElementsJSON = "{\"total\":" + totalElements + "}";
            return ok(ApiUtil.createResponse(totalElementsJSON, true));
        }
        return ok(ApiUtil.createResponse("Query method getTotalSOCsByManagerEmailByStudy() failed to retrieve total number of SOCs", false));
    }

    public Result findTotalSOCsByStudy(String studyuri) {
        int totalElements = StudyObjectCollection.findTotalStudyObjectCollectionsByStudyFlexible(studyuri);
        if (totalElements >= 0) {
            String totalElementsJSON = "{\"total\":" + totalElements + "}";
            return ok(ApiUtil.createResponse(totalElementsJSON, true));
        }     
        return ok(ApiUtil.createResponse("Query method findTotalSOCsByStudy() failed to retrieve total number of SOCs by study", false));   
    }

}
