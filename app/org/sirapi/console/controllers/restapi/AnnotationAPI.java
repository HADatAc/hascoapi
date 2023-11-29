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
        Annotation testAnnotationInstruction = Annotation.find(TEST_ANNOTATION_INSTRUCTION_URI);
        Annotation testAnnotationPage = Annotation.find(TEST_ANNOTATION_PAGE_URI);
        Annotation testAnnotationDateField = Annotation.find(TEST_ANNOTATION_DATEFIELD_URI);
        Annotation testAnnotationCopyright = Annotation.find(TEST_ANNOTATION_COPYRIGHT_URI);
        if (testAnnotation1 != null) {
            return ok(ApiUtil.createResponse("Test annotation 1 already exists.", false));
        } else if (testAnnotation2 != null) {
            return ok(ApiUtil.createResponse("Test annotation 2 already exists.", false));
        } else if (testAnnotationInstruction != null) {
            return ok(ApiUtil.createResponse("Test annotation instruction already exists.", false));
        } else if (testAnnotationPage != null) {
            return ok(ApiUtil.createResponse("Test annotation page already exists.", false));
        } else if (testAnnotationDateField != null) {
            return ok(ApiUtil.createResponse("Test annotation date field already exists.", false));
        } else if (testAnnotationCopyright != null) {
            return ok(ApiUtil.createResponse("Test annotation copyright already exists.", false));
        } else {
            Instrument testInstrument = Instrument.find(TEST_INSTRUMENT_URI);
            AnnotationStem testAnnotationStem1 = AnnotationStem.find(TEST_ANNOTATION_STEM1_URI);
            AnnotationStem testAnnotationStem2 = AnnotationStem.find(TEST_ANNOTATION_STEM2_URI);
            AnnotationStem testAnnotationStemInstruction = AnnotationStem.find(TEST_ANNOTATION_STEM_INSTRUCTION_URI);
            AnnotationStem testAnnotationStemPage = AnnotationStem.find(TEST_ANNOTATION_STEM_PAGE_URI);
            AnnotationStem testAnnotationStemDateField = AnnotationStem.find(TEST_ANNOTATION_STEM_DATEFIELD_URI);
            AnnotationStem testAnnotationStemCopyright = AnnotationStem.find(TEST_ANNOTATION_STEM_COPYRIGHT_URI);
            if (testInstrument == null) {
              return ok(ApiUtil.createResponse("Required TestInstrument does not exist.", false));
            } else if (testAnnotationStem1 == null) {
              return ok(ApiUtil.createResponse("Required TestAnnotationStem1 does not exist.", false));
            } else if (testAnnotationStem2 == null) {
              return ok(ApiUtil.createResponse("Required TestAnnotationStem2 does not exist.", false));
            } else if (testAnnotationStemInstruction == null) {
              return ok(ApiUtil.createResponse("Required TestAnnotationStemInstruction does not exist.", false));
            } else if (testAnnotationStemPage == null) {
              return ok(ApiUtil.createResponse("Required TestAnnotationStemPage does not exist.", false));
            } else if (testAnnotationStemDateField == null) {
              return ok(ApiUtil.createResponse("Required TestAnnotationStemDateField does not exist.", false));
            } else if (testAnnotationStemCopyright == null) {
              return ok(ApiUtil.createResponse("Required TestAnnotationStemCopyright does not exist.", false));
            } else {
                testAnnotation1 = new Annotation();
                testAnnotation1.setUri(TEST_ANNOTATION1_URI);
                testAnnotation1.setLabel("Test Annotation 1");
                testAnnotation1.setTypeUri(VSTOI.ANNOTATION);
                testAnnotation1.setHascoTypeUri(VSTOI.ANNOTATION);
                testAnnotation1.setComment("This is a dummy Annotation 1 created to test the SIR API.");
                testAnnotation1.setBelongsTo(TEST_INSTRUMENT_URI);
                testAnnotation1.setHasAnnotationStem(TEST_ANNOTATION_STEM1_URI);
                testAnnotation1.setHasPosition(VSTOI.NOT_VISIBLE);
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
                testAnnotation2.setHasPosition(VSTOI.NOT_VISIBLE);
                testAnnotation2.setHasStyle("");
                testAnnotation2.save();

                testAnnotationInstruction = new Annotation();
                testAnnotationInstruction.setUri(TEST_ANNOTATION_INSTRUCTION_URI);
                testAnnotationInstruction.setLabel("Test Annotation Instruction");
                testAnnotationInstruction.setTypeUri(VSTOI.ANNOTATION);
                testAnnotationInstruction.setHascoTypeUri(VSTOI.ANNOTATION);
                testAnnotationInstruction.setComment("This is a dummy Annotation Instruction created to test the SIR API.");
                testAnnotationInstruction.setBelongsTo(TEST_INSTRUMENT_URI);
                testAnnotationInstruction.setHasAnnotationStem(TEST_ANNOTATION_STEM_INSTRUCTION_URI);
                testAnnotationInstruction.setHasPosition(VSTOI.PAGE_BELOW_TOP_LINE);
                testAnnotationInstruction.setHasStyle("");
                testAnnotationInstruction.save();

                testAnnotationPage = new Annotation();
                testAnnotationPage.setUri(TEST_ANNOTATION_PAGE_URI);
                testAnnotationPage.setLabel("Test Annotation Page");
                testAnnotationPage.setTypeUri(VSTOI.ANNOTATION);
                testAnnotationPage.setHascoTypeUri(VSTOI.ANNOTATION);
                testAnnotationPage.setComment("This is a dummy Annotation Page created to test the SIR API.");
                testAnnotationPage.setBelongsTo(TEST_INSTRUMENT_URI);
                testAnnotationPage.setHasAnnotationStem(TEST_ANNOTATION_STEM_PAGE_URI);
                testAnnotationPage.setHasPosition(VSTOI.PAGE_BOTTOM_LEFT);
                testAnnotationPage.setHasStyle("");
                testAnnotationPage.save();

                testAnnotationDateField = new Annotation();
                testAnnotationDateField.setUri(TEST_ANNOTATION_DATEFIELD_URI);
                testAnnotationDateField.setLabel("Test Annotation DateField");
                testAnnotationDateField.setTypeUri(VSTOI.ANNOTATION);
                testAnnotationDateField.setHascoTypeUri(VSTOI.ANNOTATION);
                testAnnotationDateField.setComment("This is a dummy Annotation DateField created to test the SIR API.");
                testAnnotationDateField.setBelongsTo(TEST_INSTRUMENT_URI);
                testAnnotationDateField.setHasAnnotationStem(TEST_ANNOTATION_STEM_DATEFIELD_URI);
                testAnnotationDateField.setHasPosition(VSTOI.PAGE_TOP_RIGHT);
                testAnnotationDateField.setHasStyle("");
                testAnnotationDateField.save();

                testAnnotationCopyright = new Annotation();
                testAnnotationCopyright.setUri(TEST_ANNOTATION_COPYRIGHT_URI);
                testAnnotationCopyright.setLabel("Test Annotation Copyright");
                testAnnotationCopyright.setTypeUri(VSTOI.ANNOTATION);
                testAnnotationCopyright.setHascoTypeUri(VSTOI.ANNOTATION);
                testAnnotationCopyright.setComment("This is a dummy Annotation Copyright created to test the SIR API.");
                testAnnotationCopyright.setBelongsTo(TEST_INSTRUMENT_URI);
                testAnnotationCopyright.setHasAnnotationStem(TEST_ANNOTATION_STEM_COPYRIGHT_URI);
                testAnnotationCopyright.setHasPosition(VSTOI.PAGE_BOTTOM_RIGHT);
                testAnnotationCopyright.setHasStyle("");
                testAnnotationCopyright.save();

            }
            return ok(ApiUtil.createResponse("Test Annotations 1, 2, Instruction, Page, DateField and Copyright have been CREATED.", true));
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
        Annotation testInstruction = Annotation.find(TEST_ANNOTATION_INSTRUCTION_URI);
        Annotation testPage = Annotation.find(TEST_ANNOTATION_PAGE_URI);
        Annotation testDateField = Annotation.find(TEST_ANNOTATION_DATEFIELD_URI);
        Annotation testCopyright = Annotation.find(TEST_ANNOTATION_COPYRIGHT_URI);
        String msg = "";
        if (test1 == null) {
            msg += "There is no Test Annotation 1. ";
        } else {
            test1.delete();
        } 
        if (test2 == null) {
            msg += "There is no Test Annotation 2. ";
        } else {
            test2.delete();
        } 
        if (testInstruction == null) {
            msg += "There is no Test Annotation Instruction. ";
        } else {
            testInstruction.delete();
        }
        if (testPage == null) {
            msg += "There is no Test Annotation Page. ";
        } else {
            testPage.delete();
        }
        if (testDateField == null) {
            msg += "There is no Test Annotation DateField. ";
        } else {
            testDateField.delete();
        }
        if (testCopyright == null) {
            msg += "There is no Test Annotation Copyright. ";
        } else {
            testCopyright.delete();
        }
        if (!msg.isEmpty()) {
            return ok(ApiUtil.createResponse(msg, false));
        } else {
            return ok(ApiUtil.createResponse("Test Annotations 1, 2, Instruction, Page, DateField, and Copyright have been DELETED.", true));
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
