package org.sirapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.sirapi.entity.pojo.Attachment;
import org.sirapi.entity.pojo.Detector;
import org.sirapi.entity.pojo.Instrument;
import org.sirapi.utils.ApiUtil;
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
        Detector testDetector1 = Detector.find(TEST_DETECTOR1_URI);
        Detector testDetector2 = Detector.find(TEST_DETECTOR2_URI);
        if (testDetector1 != null) {
            return ok(ApiUtil.createResponse("Test detector 1 already exists.", false));
        } else if (testDetector2 != null) {
            return ok(ApiUtil.createResponse("Test detector 2 already exists.", false));
        } else {
            testDetector1 = new Detector();
            testDetector1.setUri(TEST_DETECTOR1_URI);
            testDetector1.setLabel("Test Detector 1");
            testDetector1.setTypeUri(VSTOI.DETECTOR);
            testDetector1.setHascoTypeUri(VSTOI.DETECTOR);
            testDetector1.setComment("This is a dummy Detector 1 created to test the SIR API.");
            testDetector1.setHasContent("During the last 2 weeks, have you lost appetite?");
            testDetector1.setHasCodebook(TEST_CODEBOOK_URI);
            testDetector1.setHasLanguage("en"); // ISO 639-1
            testDetector1.setHasVersion("1");
            testDetector1.setHasSIRManagerEmail("me@example.com");
            testDetector1.save();
            testDetector2 = new Detector();
            testDetector2.setUri(TEST_DETECTOR2_URI);
            testDetector2.setLabel("Test Detector 2");
            testDetector2.setTypeUri(VSTOI.DETECTOR);
            testDetector2.setHascoTypeUri(VSTOI.DETECTOR);
            testDetector2.setComment("This is a dummy Detector 2 created to test the SIR API.");
            testDetector2.setHasContent("During the last 2 weeks, have you gain appetite?");
            testDetector2.setHasCodebook(TEST_CODEBOOK_URI);
            testDetector2.setHasLanguage("en"); // ISO 639-1
            testDetector2.setHasVersion("1");
            testDetector2.setHasSIRManagerEmail("me@example.com");
            testDetector2.save();
            return ok(ApiUtil.createResponse("Test Detectors 1 and 2 have been CREATED.", true));
        }
    }

    public Result createDetector(String json) {
        if (json == null || json.equals("")) {
            return ok(ApiUtil.createResponse("No json content has been provided.", false));
        }
        System.out.println("(CreateDetector) Value of json: [" + json + "]");
        ObjectMapper objectMapper = new ObjectMapper();
        Detector newDetector;
        try {
            //convert json string to Instrument instance
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
        Detector test1 = Detector.find(TEST_DETECTOR1_URI);
        Detector test2 = Detector.find(TEST_DETECTOR2_URI);
        if (test1 == null) {
            return ok(ApiUtil.createResponse("There is no Test Detector 1 to be deleted.", false));
        } else if (test2 == null) {
            return ok(ApiUtil.createResponse("There is no Test Detector 2 to be deleted.", false));
        } else {
            test1.delete();
            test2.delete();
            return ok(ApiUtil.createResponse("Test Detectors 1 and 2 have been DELETED.", true));
        }
    }

    public Result attach(String uri, String attachmentUri){
        if (uri == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No detector URI has been provided.", false));
        }
        Detector detector = Detector.find(uri);
        if (detector == null) {
            return ok(ApiUtil.createResponse("There is no detector with URI <" + uri + "> to be attached.", false));
        }
        if (attachmentUri == null || attachmentUri.equals("")) {
            return ok(ApiUtil.createResponse("No attachment URI has been provided.", false));
        }
        Attachment attachment = Attachment.find(attachmentUri);
        if (attachment == null) {
            return ok(ApiUtil.createResponse("There is no attachment with uri <" + attachmentUri + ">.", false));
        }
        if (Detector.attach(attachmentUri, uri)) {
            return ok(ApiUtil.createResponse("Detector <" + uri + "> successfully attached to attachment <" + attachmentUri + ">.", true));
        }
        return ok(ApiUtil.createResponse("Detector <" + uri + "> failed to associate with attachment  <" + attachmentUri + ">.", false));
    }

    public Result attachForTesting(){
        Instrument testInst = Instrument.find(TEST_INSTRUMENT_URI);
        if (testInst == null) {
            return ok(ApiUtil.createResponse("create test instrument before trying to attach detectors.", false));
        }
        if (testInst.getAttachments() == null) {
            return ok(ApiUtil.createResponse("Create attachments for test instrument before trying to attach detectors.", false));
        }
        Detector test1 = Detector.find(TEST_DETECTOR1_URI);
        Detector test2 = Detector.find(TEST_DETECTOR2_URI);
        if (test1 == null) {
            return ok(ApiUtil.createResponse("There is no Test Detector 1 to be attached to test instrument.", false));
        } else if (test2 == null) {
            return ok(ApiUtil.createResponse("There is no Test Detector 2 to be attached to test instrument.", false));
        } else {
            boolean done = Detector.attach(TEST_ATTACHMENT1_URI, TEST_DETECTOR1_URI);
            if (!done) {
                return ok(ApiUtil.createResponse("The attachment of Test Detector 1 to Test Instrument HAS FAILED.", false));
            } else {
                done = Detector.attach(TEST_ATTACHMENT2_URI, TEST_DETECTOR2_URI);
                if (!done) {
                    return ok(ApiUtil.createResponse("The attachment of Test Detector 2 to Test Instrument HAS FAILED.", false));
                }
            }
        }
        return ok(ApiUtil.createResponse("Test Detectors 1 and 2 have been ATTACHED to Test Instrument.", true));
    }

    public Result detach(String attachmentUri){
        if (attachmentUri == null || attachmentUri.equals("")) {
            return ok(ApiUtil.createResponse("No attachment URI has been provided.", false));
        }
        Attachment attachment = Attachment.find(attachmentUri);
        if (attachment == null) {
            return ok(ApiUtil.createResponse("There is no attachment with URI <" + attachmentUri + ">.", false));
        }
        if (Detector.detach(attachmentUri)) {
            return ok(ApiUtil.createResponse("No detector is associated with attachment <" + attachmentUri + ">.", true));
        }
        return ok(ApiUtil.createResponse("A detector has failed to be removed from attachment <" + attachmentUri + ">.", false));
    }

    public Result detachForTesting(){
        Instrument testInst = Instrument.find(TEST_INSTRUMENT_URI);
        if (testInst == null) {
            return ok(ApiUtil.createResponse("There is no test instrument to detach detectors.", false));
        }
        if (testInst.getAttachments() == null) {
            return ok(ApiUtil.createResponse("Test instrument has no attachments for detectors.", false));
        }
        Detector test1 = Detector.find(TEST_DETECTOR1_URI);
        Detector test2 = Detector.find(TEST_DETECTOR2_URI);
        if (test1 == null) {
            return ok(ApiUtil.createResponse("There is no Test Detector 1 to be detached from test instrument.", false));
        } else if (test2 == null) {
            return ok(ApiUtil.createResponse("There is no Test Detector 2 to be detached from test instrument.", false));
        } else {
            boolean done = Detector.detach(TEST_ATTACHMENT1_URI);
            if (done) {
                done = Detector.detach(TEST_ATTACHMENT2_URI);
            }
            if (done) {
                return ok(ApiUtil.createResponse("Test Detectors 1 and 2 have been DETACHED from Test Instrument.", true));
            }
        }
        return ok(ApiUtil.createResponse("The detachment of Test Detectors 1 and 2 from Test Instrument HAS FAILED.", false));
    }

    public Result deleteDetector(String uri){
        if (uri == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No detector URI has been provided.", false));
        }
        Detector detector = Detector.find(uri);
        if (detector == null) {
            return ok(ApiUtil.createResponse("There is no detector with URI <" + uri + "> to be deleted.", false));
        } else {
            return deleteDetectorResult(detector);
        }
    }

    public Result getDetectorsByLanguage(String language){
        List<Detector> results = Detector.findByLanguage(language);
        return getDetectors(results);
    }

    public Result getDetectorsByKeyword(String keyword){
        List<Detector> results = Detector.findByKeyword(keyword);
        return getDetectors(results);
    }

    public Result getDetectorsByManagerEmail(String managerEmail){
        List<Detector> results = Detector.findByManagerEmail(managerEmail);
        return getDetectors(results);
    }

    public Result getDetectorsByInstrument(String instrumentUri){
        List<Detector> results = Detector.findByInstrument(instrumentUri);
        return getDetectors(results);
    }

    public Result getAllDetectors(){
        List<Detector> results = Detector.find();
        return getDetectors(results);
    }

    public static Result getDetectors(List<Detector> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No detector has been found", false));
        } else {
            ObjectMapper mapper = new ObjectMapper();
            SimpleFilterProvider filterProvider = new SimpleFilterProvider();
            filterProvider.addFilter("detectorFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "comment", "hasContent", "hasSerialNumber", "hasLanguage","hasCodebook",
                            "hasVersion", "wasDerivedFrom", "wasGeneratedBy", "hasSIRManagerEmail"));
            mapper.setFilterProvider(filterProvider);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

    public Result getAllAttachments(){
        List<Attachment> results = Attachment.find();
        return getAttachments(results);
    }

    public Result getAttachmentsByInstrument(String instrumentUri){
        List<Attachment> results = Attachment.findByInstrument(instrumentUri);
        return getAttachments(results);
    }

    public static Result getAttachments(List<Attachment> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No attachment has been found", false));
        } else {
            ObjectMapper mapper = new ObjectMapper();
            SimpleFilterProvider filterProvider = new SimpleFilterProvider();
            filterProvider.addFilter("attachmentFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "comment", "hasPriority", "hasDetector", "belongsTo"));
            mapper.setFilterProvider(filterProvider);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

    public Result getUsage(String detectorUri){
        List<Attachment> results = Detector.usage(detectorUri);
        return DetectorAPI.getAttachments(results);
    }

}
