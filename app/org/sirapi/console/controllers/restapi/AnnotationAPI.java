package org.sirapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.sirapi.entity.pojo.AnnotationStem;
import org.sirapi.entity.pojo.Annotation;
import org.sirapi.entity.pojo.Instrument;
import org.sirapi.utils.ApiUtil;
import org.sirapi.utils.HAScOMapper;
import org.sirapi.vocabularies.VSTOI;
import play.mvc.Controller;
import play.mvc.Result;
import static org.sirapi.Constants.*;
import java.util.List;


public class AnnotationAPI extends Controller {

    private Result createAnnotationResult(Annotation annotation) {
        annotation.save();
        return ok(ApiUtil.createResponse("Annotation <" + annotation.getUri() + "> has been CREATED.", true));
    }

    public Result createAnnotationsForTesting() {
        Annotation testAnnotation1 = Annotation.find(TEST_ANNOTATION1_URI);
        Annotation testAnnotation2 = Annotation.find(TEST_ANNOTATION2_URI);
        if (testAnnotation1 != null) {
            return ok(ApiUtil.createResponse("Test annotation 1 already exists.", false));
        } else if (testAnnotation2 != null) {
            return ok(ApiUtil.createResponse("Test annotation 2 already exists.", false));
        } else {
            Instrument testInstrument = Instrument.find(TEST_INSTRUMENT_URI);
            AnnotationStem testAnnotationStem1 = AnnotationStem.find(TEST_ANNOTATION_STEM1_URI);
            AnnotationStem testAnnotationStem2 = AnnotationStem.find(TEST_ANNOTATION_STEM2_URI);
            if (testInstrument == null) {
              return ok(ApiUtil.createResponse("Required TestInstrument does not exist.", false));
            } else if (testAnnotationStem1 == null) {
              return ok(ApiUtil.createResponse("Required TestAnnotationStem1 does not exist.", false));
            } else if (testAnnotationStem2 == null) {
              return ok(ApiUtil.createResponse("Required TestAnnotationStem2 does not exist.", false));
            } else {
                testAnnotation1 = new Annotation();
                testAnnotation1.setUri(TEST_ANNOTATION1_URI);
                testAnnotation1.setLabel("Test Annotation 1");
                testAnnotation1.setTypeUri(VSTOI.ANNOTATION);
                testAnnotation1.setHascoTypeUri(VSTOI.ANNOTATION);
                testAnnotation1.setComment("This is a dummy Annotation 1 created to test the SIR API.");
                testAnnotation1.setBelongsTo(TEST_INSTRUMENT_URI);
                testAnnotation1.setHasAnnotationStem(TEST_ANNOTATION_STEM1_URI);
                testAnnotation1.setHasPosition(VSTOI.DEFAULT_PAGE_POSITION);
                testAnnotation1.setHasStyle("");
                testAnnotation1.save();

                testAnnotation2 = new Annotation();
                testAnnotation2.setUri(TEST_ANNOTATION2_URI);
                testAnnotation2.setLabel("Test Annotation 2");
                testAnnotation2.setTypeUri(VSTOI.ANNOTATION);
                testAnnotation2.setHascoTypeUri(VSTOI.ANNOTATION);
                testAnnotation2.setComment("This is a dummy Annotation 2 created to test the SIR API.");
                testAnnotation2.setBelongsTo(TEST_INSTRUMENT_URI);
                testAnnotation2.setHasAnnotationStem(TEST_ANNOTATION_STEM2_URI);
                testAnnotation2.setHasPosition(VSTOI.DEFAULT_PAGE_POSITION);
                testAnnotation2.setHasStyle("");
                testAnnotation2.save();
            }
            return ok(ApiUtil.createResponse("Test Annotations 1 and 2 have been CREATED.", true));
        }
    }

    public Result createAnnotation(String json) {
        if (json == null || json.equals("")) {
            return ok(ApiUtil.createResponse("No json content has been provided.", false));
        }
        //System.out.println("(CreateAnnotation) Value of json: [" + json + "]");
        ObjectMapper objectMapper = new ObjectMapper();
        Annotation newAnnotation;
        try {
            //convert json string to Annotation instance
            newAnnotation  = objectMapper.readValue(json, Annotation.class);
        } catch (Exception e) {
            //System.out.println("(createAnnotation) Failed to parse json.");
            return ok(ApiUtil.createResponse("Failed to parse json.", false));
        }
        return createAnnotationResult(newAnnotation);
    }

    private Result deleteAnnotationResult(Annotation annotation) {
        String uri = annotation.getUri();
        annotation.delete();
        return ok(ApiUtil.createResponse("Annotation <" + uri + "> has been DELETED.", true));
    }

    public Result deleteAnnotationsForTesting(){
        Annotation test1 = Annotation.find(TEST_ANNOTATION1_URI);
        Annotation test2 = Annotation.find(TEST_ANNOTATION2_URI);
        if (test1 == null) {
            return ok(ApiUtil.createResponse("There is no Test Annotation 1 to be deleted.", false));
        } else if (test2 == null) {
            return ok(ApiUtil.createResponse("There is no Test Annotation 2 to be deleted.", false));
        } else {
            test1.delete();
            test2.delete();
            return ok(ApiUtil.createResponse("Test Annotations 1 and 2 have been DELETED.", true));
        }
    }

    public Result getAnnotationsByInstrument(String instrumentUri){
        List<Annotation> results = Annotation.findByInstrument(instrumentUri);
        return getAnnotations(results);
    }

    public static Result getAnnotations(List<Annotation> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No annotation has been found", false));
        } else {
            ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL,VSTOI.ANNOTATION);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            //System.out.println("DetecttorAPI: [" + ApiUtil.createResponse(jsonObject, true) + "]");
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

}
