package org.sirapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.sirapi.entity.pojo.DetectorSlot;
import org.sirapi.entity.pojo.DetectorStem;
import org.sirapi.entity.pojo.Instrument;
import org.sirapi.utils.ApiUtil;
import org.sirapi.vocabularies.VSTOI;
import play.mvc.Controller;
import play.mvc.Result;
import static org.sirapi.Constants.*;
import java.util.List;


public class DetectorStemAPI extends Controller {

    private Result createDetectorStemResult(DetectorStem detectorStem) {
        detectorStem.save();
        return ok(ApiUtil.createResponse("DetectorStem <" + detectorStem.getUri() + "> has been CREATED.", true));
    }

    public Result createDetectorStemsForTesting() {
        DetectorStem testDetectorStem1 = DetectorStem.find(TEST_DETECTOR_STEM1_URI);
        DetectorStem testDetectorStem2 = DetectorStem.find(TEST_DETECTOR_STEM2_URI);
        if (testDetectorStem1 != null) {
            return ok(ApiUtil.createResponse("Test detector 1 already exists.", false));
        } else if (testDetectorStem2 != null) {
            return ok(ApiUtil.createResponse("Test detector 2 already exists.", false));
        } else {
            testDetectorStem1 = new DetectorStem();
            testDetectorStem1.setUri(TEST_DETECTOR_STEM1_URI);
            testDetectorStem1.setLabel("Test Detector Stem 1");
            testDetectorStem1.setTypeUri(VSTOI.DETECTOR_STEM);
            testDetectorStem1.setHascoTypeUri(VSTOI.DETECTOR_STEM);
            testDetectorStem1.setComment("This is a dummy Detector Stem 1 created to test the SIR API.");
            testDetectorStem1.setHasContent("During the last 2 weeks, have you lost appetite?");
            testDetectorStem1.setHasLanguage("en"); // ISO 639-1
            testDetectorStem1.setHasVersion("1");
            testDetectorStem1.setHasSIRManagerEmail("me@example.com");
            testDetectorStem1.save();
            testDetectorStem2 = new DetectorStem();
            testDetectorStem2.setUri(TEST_DETECTOR_STEM2_URI);
            testDetectorStem2.setLabel("Test Detector Stem 2");
            testDetectorStem2.setTypeUri(VSTOI.DETECTOR_STEM);
            testDetectorStem2.setHascoTypeUri(VSTOI.DETECTOR_STEM);
            testDetectorStem2.setComment("This is a dummy Detector Stem 2 created to test the SIR API.");
            testDetectorStem2.setHasContent("During the last 2 weeks, have you gain appetite?");
            testDetectorStem2.setHasLanguage("en"); // ISO 639-1
            testDetectorStem2.setHasVersion("1");
            testDetectorStem2.setHasSIRManagerEmail("me@example.com");
            testDetectorStem2.save();
            return ok(ApiUtil.createResponse("Test Detector Stems 1 and 2 have been CREATED.", true));
        }
    }

    public Result createDetectorStem(String json) {
        if (json == null || json.equals("")) {
            return ok(ApiUtil.createResponse("No json content has been provided.", false));
        }
        System.out.println("(CreateDetectorStem) Value of json: [" + json + "]");
        ObjectMapper objectMapper = new ObjectMapper();
        DetectorStem newDetectorStem;
        try {
            //convert json string to Instrument instance
            newDetectorStem  = objectMapper.readValue(json, DetectorStem.class);
        } catch (Exception e) {
            //System.out.println("(createDetector) Failed to parse json.");
            return ok(ApiUtil.createResponse("Failed to parse json.", false));
        }
        return createDetectorStemResult(newDetectorStem);
    }

    private Result deleteDetectorStemResult(DetectorStem detectorStem) {
        String uri = detectorStem.getUri();
        detectorStem.delete();
        return ok(ApiUtil.createResponse("Detector Stem <" + uri + "> has been DELETED.", true));
    }

    public Result deleteDetectorStemsForTesting(){
        DetectorStem test1 = DetectorStem.find(TEST_DETECTOR_STEM1_URI);
        DetectorStem test2 = DetectorStem.find(TEST_DETECTOR_STEM2_URI);
        if (test1 == null) {
            return ok(ApiUtil.createResponse("There is no Test Detector Stem 1 to be deleted.", false));
        } else if (test2 == null) {
            return ok(ApiUtil.createResponse("There is no Test Detector Stem 2 to be deleted.", false));
        } else {
            test1.delete();
            test2.delete();
            return ok(ApiUtil.createResponse("Test Detector Stems 1 and 2 have been DELETED.", true));
        }
    }

    public Result deleteDetectorStem(String uri){
        if (uri == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No detector setm URI has been provided.", false));
        }
        DetectorStem detectorStem = DetectorStem.find(uri);
        if (detectorStem == null) {
            return ok(ApiUtil.createResponse("There is no detector stem with URI <" + uri + "> to be deleted.", false));
        } else {
            return deleteDetectorStemResult(detectorStem);
        }
    }

    public Result getDetectorStemsByLanguage(String language){
        List<DetectorStem> results = DetectorStem.findByLanguage(language);
        return getDetectorStems(results);
    }

    public Result getDetectorStemsByKeyword(String keyword){
        List<DetectorStem> results = DetectorStem.findByKeyword(keyword);
        return getDetectorStems(results);
    }

    public Result getDetectorStemsByManagerEmail(String managerEmail){
        List<DetectorStem> results = DetectorStem.findByManagerEmail(managerEmail);
        return getDetectorStems(results);
    }

    public Result getDetectorStemsByInstrument(String instrumentUri){
        List<DetectorStem> results = DetectorStem.findByInstrument(instrumentUri);
        return getDetectorStems(results);
    }

    public Result getAllDetectorStems(){
        List<DetectorStem> results = DetectorStem.find();
        return getDetectorStems(results);
    }

    public static Result getDetectorStems(List<DetectorStem> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No detector stem has been found", false));
        } else {
            ObjectMapper mapper = new ObjectMapper();
            SimpleFilterProvider filterProvider = new SimpleFilterProvider();
            filterProvider.addFilter("detectorStemFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "comment", "hasContent", "hasSerialNumber", "hasLanguage",
                            "hasVersion", "wasDerivedFrom", "wasGeneratedBy", "hasSIRManagerEmail"));
            mapper.setFilterProvider(filterProvider);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

}
