package org.sirapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import org.sirapi.Constants;
import org.sirapi.entity.pojo.DetectorSlot;
import org.sirapi.entity.pojo.DetectorStem;
import org.sirapi.entity.pojo.Detector;
import org.sirapi.entity.pojo.Instrument;
import org.sirapi.utils.ApiUtil;
import org.sirapi.utils.HAScOMapper;
import org.sirapi.vocabularies.VSTOI;

import play.mvc.Controller;
import play.mvc.Result;
import static org.sirapi.Constants.*;
import java.util.List;


public class DetectorAPI extends Controller {

    private Result createDetectorResult(Detector detector) {
        detector.save();
        return ok(ApiUtil.createResponse("Detector <" + detector.getUri() + "> has been CREATED.", true));
    }

    public Result createDetectorsForTesting() {
        Detector testDetector1 = Detector.findDetector(TEST_DETECTOR1_URI);
        Detector testDetector2 = Detector.findDetector(TEST_DETECTOR2_URI);
        if (testDetector1 != null) {
            return ok(ApiUtil.createResponse("Test detector 1 already exists.", false));
        } else if (testDetector2 != null) {
            return ok(ApiUtil.createResponse("Test detector 2 already exists.", false));
        } else {
            DetectorStem testDetectorStem1 = DetectorStem.find(TEST_DETECTOR_STEM1_URI);
            DetectorStem testDetectorStem2 = DetectorStem.find(TEST_DETECTOR_STEM2_URI);
            if (testDetectorStem1 == null) {
              return ok(ApiUtil.createResponse("Required TestDetectorStem1 does not exist.", false));
            } else if (testDetectorStem2 == null) {
              return ok(ApiUtil.createResponse("Required TestDetectorStem2 does not exist.", false));
            } else {
                testDetector1 = new Detector();
                testDetector1.setUri(TEST_DETECTOR1_URI);
                testDetector1.setLabel("Test Detector 1");
                testDetector1.setTypeUri(VSTOI.DETECTOR);
                testDetector1.setHascoTypeUri(VSTOI.DETECTOR);
                testDetector1.setComment("This is a dummy Detector 1 created to test the SIR API.");
                testDetector1.setHasDetectorStem(TEST_DETECTOR_STEM1_URI);
                testDetector1.setHasCodebook(TEST_CODEBOOK_URI);
                testDetector1.setHasLanguage("en");
                testDetector1.setHasVersion("1");
                testDetector1.setHasSIRManagerEmail("me@example.com");
                testDetector1.setNamedGraph(Constants.TEST_KB);
                testDetector1.save();

                testDetector2 = new Detector();
                testDetector2.setUri(TEST_DETECTOR2_URI);
                testDetector2.setLabel("Test Detector 2");
                testDetector2.setTypeUri(VSTOI.DETECTOR);
                testDetector2.setHascoTypeUri(VSTOI.DETECTOR);
                testDetector2.setComment("This is a dummy Detector 2 created to test the SIR API.");
                testDetector2.setHasDetectorStem(TEST_DETECTOR_STEM2_URI);
                testDetector2.setHasCodebook(TEST_CODEBOOK_URI);
                testDetector2.setHasLanguage("en");
                testDetector2.setHasVersion("1");
                testDetector2.setHasSIRManagerEmail("me@example.com");
                testDetector2.setNamedGraph(Constants.TEST_KB);
                testDetector2.save();
            }
            return ok(ApiUtil.createResponse("Test Detectors 1 and 2 have been CREATED.", true));
        }
    }

    public Result createDetector(String json) {
        if (json == null || json.equals("")) {
            return ok(ApiUtil.createResponse("No json content has been provided.", false));
        }
        //System.out.println("(CreateDetector) Value of json: [" + json + "]");
        ObjectMapper objectMapper = new ObjectMapper();
        Detector newDetector;
        try {
            //convert json string to Container instance
            newDetector  = objectMapper.readValue(json, Detector.class);
        } catch (Exception e) {
            //System.out.println("(createDetector) Failed to parse json.");
            return ok(ApiUtil.createResponse("Failed to parse json.", false));
        }
        return createDetectorResult(newDetector);
    }

    private Result deleteDetectorResult(Detector detector) {
        String uri = detector.getUri();
        detector.delete();
        return ok(ApiUtil.createResponse("Detector <" + uri + "> has been DELETED.", true));
    }

    public Result deleteDetectorsForTesting(){
        Detector test1 = Detector.findDetector(TEST_DETECTOR1_URI);
        Detector test2 = Detector.findDetector(TEST_DETECTOR2_URI);
        if (test1 == null) {
            return ok(ApiUtil.createResponse("There is no Test Detector 1 to be deleted.", false));
        } else if (test2 == null) {
            return ok(ApiUtil.createResponse("There is no Test Detector 2 to be deleted.", false));
        } else {
            test1.setNamedGraph(Constants.TEST_KB);
            test1.delete();
            test2.setNamedGraph(Constants.TEST_KB);
            test2.delete();
            return ok(ApiUtil.createResponse("Test Detectors 1 and 2 have been DELETED.", true));
        }
    }

    public Result attach(String uri, String detectorSlotUri){
        if (uri == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No detector URI has been provided.", false));
        }
        Detector detector = Detector.findDetector(uri);
        if (detector == null) {
            return ok(ApiUtil.createResponse("There is no detector with URI <" + uri + "> to be attached.", false));
        }
        if (detectorSlotUri == null || detectorSlotUri.equals("")) {
            return ok(ApiUtil.createResponse("No detectorSlot URI has been provided.", false));
        }
        DetectorSlot detectorSlot = DetectorSlot.find(detectorSlotUri);
        if (detectorSlot == null) {
            return ok(ApiUtil.createResponse("There is no detectorSlot with uri <" + detectorSlotUri + ">.", false));
        }
        if (Detector.attach(detectorSlot, detector)) {
            return ok(ApiUtil.createResponse("Detector <" + uri + "> successfully attached to detectorSlot <" + detectorSlotUri + ">.", true));
        }
        return ok(ApiUtil.createResponse("Detector <" + uri + "> failed to associate with detectorSlot  <" + detectorSlotUri + ">.", false));
    }

    public Result attachForTesting(){
        Instrument testInst = Instrument.find(TEST_INSTRUMENT_URI);
        if (testInst == null) {
            return ok(ApiUtil.createResponse("create test instrument before trying to attach detectors.", false));
        }
        if (testInst.getDetectorSlots() == null) {
            return ok(ApiUtil.createResponse("Create detectorSlots for test instrument before trying to attach detectors.", false));
        }
        Detector test1 = Detector.findDetector(TEST_DETECTOR1_URI);
        Detector test2 = Detector.findDetector(TEST_DETECTOR2_URI);
        DetectorSlot slot1 = DetectorSlot.find(TEST_DETECTOR_SLOT1_URI);
        DetectorSlot slot2 = DetectorSlot.find(TEST_DETECTOR_SLOT2_URI);
        if (test1 == null) {
            return ok(ApiUtil.createResponse("There is no Test Detector 1 to be attached to test instrument.", false));
        } else if (test2 == null) {
            return ok(ApiUtil.createResponse("There is no Test Detector 2 to be attached to test instrument.", false));
        } else if (slot1 == null) {
            return ok(ApiUtil.createResponse("There is no Test Detector Slot 1 in test instrument.", false));
        } else if (slot2 == null) {
            return ok(ApiUtil.createResponse("There is no Test Detector Slot 2 in test instrument.", false));
        } else if (slot1.getDetector() != null) {
            return ok(ApiUtil.createResponse("There is a Test Detector already attached to Slot 1.", false));
        } else if (slot2.getDetector() != null) {
            return ok(ApiUtil.createResponse("There is a Test Detector already attached to Slot 2.", false));
        } else {
            boolean done = Detector.attach(slot1, test1);
            if (!done) {
                return ok(ApiUtil.createResponse("The use of DetectorSlot1 to attach TestDetector1 to TestContainer HAS FAILED.", false));
            } else {
                done = Detector.attach(slot2, test1);
                if (!done) {
                    return ok(ApiUtil.createResponse("The use of DetectorSlot2 to attach TestDetector2 to TestContainer HAS FAILED.", false));
                }
            }
        }
        return ok(ApiUtil.createResponse("Test Detectors 1 and 2 have been ATTACHED to Test Container.", true));
    }

    public Result detach(String detectorSlotUri){
        if (detectorSlotUri == null || detectorSlotUri.equals("")) {
            return ok(ApiUtil.createResponse("No detectorSlot URI has been provided.", false));
        }
        DetectorSlot detectorSlot = DetectorSlot.find(detectorSlotUri);
        if (detectorSlot == null) {
            return ok(ApiUtil.createResponse("There is no detectorSlot with URI <" + detectorSlotUri + ">.", false));
        }
        if (Detector.detach(detectorSlot)) {
            return ok(ApiUtil.createResponse("No detector is associated with detectorSlot <" + detectorSlotUri + ">.", true));
        }
        return ok(ApiUtil.createResponse("A detector has failed to be removed from detectorSlot <" + detectorSlotUri + ">.", false));
    }

    public Result detachForTesting(){
        Instrument testInst = Instrument.find(TEST_INSTRUMENT_URI);
        if (testInst == null) {
            return ok(ApiUtil.createResponse("There is no test instrument to detach detectors.", false));
        }
        if (testInst.getDetectorSlots() == null) {
            return ok(ApiUtil.createResponse("Test instrument has no detectorSlots for detectors.", false));
        }
        Detector test1 = Detector.findDetector(TEST_DETECTOR1_URI);
        Detector test2 = Detector.findDetector(TEST_DETECTOR2_URI);
        DetectorSlot slot1 = DetectorSlot.find(TEST_DETECTOR_SLOT1_URI);
        DetectorSlot slot2 = DetectorSlot.find(TEST_DETECTOR_SLOT2_URI);
        if (test1 == null) {
            return ok(ApiUtil.createResponse("There is no Test Detector 1 to be attached to test instrument.", false));
        } else if (test2 == null) {
            return ok(ApiUtil.createResponse("There is no Test Detector 2 to be attached to test instrument.", false));
        } else if (slot1 == null) {
            return ok(ApiUtil.createResponse("There is no Test Detector Slot 1 in test instrument.", false));
        } else if (slot2 == null) {
            return ok(ApiUtil.createResponse("There is no Test Detector Slot 2 in test instrument.", false));
        } else if (slot1.getDetector() == null) {
            return ok(ApiUtil.createResponse("There is no Test Detector to be detached from Slot 1.", false));
        } else if (slot2.getDetector() == null) {
            return ok(ApiUtil.createResponse("There is no Test Detector to be detached from Slot 2.", false));
        } else {
            boolean done = Detector.detach(slot1);
            if (done) {
                done = Detector.detach(slot2);
            }
            if (done) {
                return ok(ApiUtil.createResponse("Test Detectors 1 and 2 have been DETACHED from Test Container.", true));
            }
        }
        return ok(ApiUtil.createResponse("The detachment of Test Detectors 1 and 2 from Test Container HAS FAILED.", false));
    }

    public Result deleteDetector(String uri){
        if (uri == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No detector URI has been provided.", false));
        }
        Detector detector = Detector.findDetector(uri);
        if (detector == null) {
            return ok(ApiUtil.createResponse("There is no detector with URI <" + uri + "> to be deleted.", false));
        } else {
            return deleteDetectorResult(detector);
        }
    }

    public Result getDetectorsByContainer(String instrumentUri){
        List<Detector> results = Detector.findDetectorsByContainer(instrumentUri);
        return getDetectors(results);
    }

    public static Result getDetectors(List<Detector> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No detector has been found", false));
        } else {
            ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL,VSTOI.DETECTOR);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            //System.out.println("DetecttorAPI: [" + ApiUtil.createResponse(jsonObject, true) + "]");
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

    public Result getAllDetectorSlots(){
        List<DetectorSlot> results = DetectorSlot.find();
        return getDetectorSlots(results);
    }

    public Result getDetectorSlotsByContainer(String instrumentUri){
        List<DetectorSlot> results = DetectorSlot.findByContainer(instrumentUri);
        return getDetectorSlots(results);
    }

    public static Result getDetectorSlots(List<DetectorSlot> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No detectorSlot has been found", false));
        } else {
            ObjectMapper mapper = new ObjectMapper();
            SimpleFilterProvider filterProvider = new SimpleFilterProvider();
            filterProvider.addFilter("detectorSlotFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "comment", "hasPriority", "hasDetector", "belongsTo"));
            mapper.setFilterProvider(filterProvider);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

    public Result getUsage(String detectorUri){
        List<DetectorSlot> results = Detector.usage(detectorUri);
        return DetectorAPI.getDetectorSlots(results);
    }

}
