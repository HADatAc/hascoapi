package org.sirapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.sirapi.entity.pojo.Annotation;
import org.sirapi.entity.pojo.AnnotationStem;
import org.sirapi.utils.ApiUtil;
import org.sirapi.utils.HAScOMapper;
import org.sirapi.vocabularies.VSTOI;
import play.mvc.Controller;
import play.mvc.Result;
import static org.sirapi.Constants.*;
import java.util.List;


public class AnnotationStemAPI extends Controller {

    private Result createAnnotationStemResult(AnnotationStem annotationStem) {
        annotationStem.save();
        return ok(ApiUtil.createResponse("AnnotationStem <" + annotationStem.getUri() + "> has been CREATED.", true));
    }

    public Result createAnnotationStemsForTesting() {
        AnnotationStem testAnnotationStem1 = AnnotationStem.find(TEST_ANNOTATION_STEM1_URI);
        AnnotationStem testAnnotationStem2 = AnnotationStem.find(TEST_ANNOTATION_STEM2_URI);
        if (testAnnotationStem1 != null) {
            return ok(ApiUtil.createResponse("TestAnnotationStem1 already exists.", false));
        } else if (testAnnotationStem2 != null) {
            return ok(ApiUtil.createResponse("TestAnnotationStem2 already exists.", false));
        } else {
            testAnnotationStem1 = new AnnotationStem();
            testAnnotationStem1.setUri(TEST_ANNOTATION_STEM1_URI);
            testAnnotationStem1.setLabel("Test Annotation Stem 1");
            testAnnotationStem1.setTypeUri(VSTOI.ANNOTATION_STEM);
            testAnnotationStem1.setHascoTypeUri(VSTOI.ANNOTATION_STEM);
            testAnnotationStem1.setComment("This is a dummy Annotation Stem 1 created to test the SIR API.");
            testAnnotationStem1.setHasContent("Considering your feelings during the last two weeks, select the best answer for each question.");
            testAnnotationStem1.setHasLanguage("en"); // ISO 639-1
            testAnnotationStem1.setHasVersion("1");
            testAnnotationStem1.setHasSIRManagerEmail("me@example.com");
            testAnnotationStem1.save();

            testAnnotationStem2 = new AnnotationStem();
            testAnnotationStem2.setUri(TEST_ANNOTATION_STEM2_URI);
            testAnnotationStem2.setLabel("Test Annotation Stem 2");
            testAnnotationStem2.setTypeUri(VSTOI.ANNOTATION_STEM);
            testAnnotationStem2.setHascoTypeUri(VSTOI.ANNOTATION_STEM);
            testAnnotationStem2.setComment("This is a dummy Annotation Stem 2 created to test the SIR API.");
            testAnnotationStem2.setHasContent("Page");
            testAnnotationStem2.setHasLanguage("en"); // ISO 639-1
            testAnnotationStem2.setHasVersion("1");
            testAnnotationStem2.setHasSIRManagerEmail("me@example.com");
            testAnnotationStem2.save();
            return ok(ApiUtil.createResponse("Test Annotation Stems 1 and 2 have been CREATED.", true));
        }
    }

    public Result createAnnotationStem(String json) {
        if (json == null || json.equals("")) {
            return ok(ApiUtil.createResponse("No json content has been provided.", false));
        }
        //System.out.println("(CreateAnnotationStem) Value of json: [" + json + "]");
        ObjectMapper objectMapper = new ObjectMapper();
        AnnotationStem newAnnotationStem;
        try {
            //convert json string to Instrument instance
            newAnnotationStem  = objectMapper.readValue(json, AnnotationStem.class);
        } catch (Exception e) {
            //System.out.println("(createDetector) Failed to parse json.");
            return ok(ApiUtil.createResponse("Failed to parse json.", false));
        }
        return createAnnotationStemResult(newAnnotationStem);
    }

    private Result deleteAnnotationStemResult(AnnotationStem annotationStem) {
        String uri = annotationStem.getUri();
        annotationStem.delete();
        return ok(ApiUtil.createResponse("Annotation Stem <" + uri + "> has been DELETED.", true));
    }

    public Result deleteAnnotationStemsForTesting(){
        AnnotationStem test1 = AnnotationStem.find(TEST_ANNOTATION_STEM1_URI);
        AnnotationStem test2 = AnnotationStem.find(TEST_ANNOTATION_STEM2_URI);
        if (test1 == null) {
            return ok(ApiUtil.createResponse("There is no Test Annotation Stem 1 to be deleted.", false));
        } else if (test2 == null) {
            return ok(ApiUtil.createResponse("There is no Test Annotation Stem 2 to be deleted.", false));
        } else {
            test1.delete();
            test2.delete();
            return ok(ApiUtil.createResponse("Test Annotation Stems 1 and 2 have been DELETED.", true));
        }
    }

    public Result deleteAnnotationStem(String uri){
        if (uri == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No annotation stem URI has been provided.", false));
        }
        AnnotationStem annotationStem = AnnotationStem.find(uri);
        if (annotationStem == null) {
            return ok(ApiUtil.createResponse("There is no annotation stem with URI <" + uri + "> to be deleted.", false));
        } else {
            return deleteAnnotationStemResult(annotationStem);
        }
    }

    public static Result getAnnotationStems(List<AnnotationStem> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No annotation stem has been found", false));
        } else {
            ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL,VSTOI.ANNOTATION_STEM);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            //System.out.println("DetecttorAPI: [" + ApiUtil.createResponse(jsonObject, true) + "]");
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

    public Result getUsage(String annotationStemUri){
        List<Annotation> results = AnnotationStem.usage(annotationStemUri);
        return AnnotationAPI.getAnnotations(results);
    }


}
