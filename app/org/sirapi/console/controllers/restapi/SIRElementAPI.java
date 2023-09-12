package org.sirapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.sirapi.entity.pojo.*;
import org.sirapi.utils.ApiUtil;
import org.sirapi.utils.HAScOMapper;
import org.sirapi.vocabularies.VSTOI;
import play.mvc.Controller;
import play.mvc.Result;
import java.util.List;
import java.util.ArrayList;

public class SIRElementAPI extends Controller {

    public static Class getElementClass(String elementType) {
        
        if (elementType.equals("instrument")) {
            return Instrument.class;
        } else if (elementType.equals("detectorstem")) {
            return DetectorStem.class;
        } else if (elementType.equals("detector")) {
            return Detector.class;
        } else if (elementType.equals("detectorslot")) {
            return DetectorSlot.class;
        } else if (elementType.equals("codebook")) {
            return Codebook.class;
        } else if (elementType.equals("responseoption")) {
            return ResponseOption.class;
        } else if (elementType.equals("responseoptionslot")) {
            return ResponseOptionSlot.class;
        } else if (elementType.equals("semanticvariable")) {
            return SemanticVariable.class;
        } else if (elementType.equals("entity")) {
            return Entity.class;
        } else if (elementType.equals("attribute")) {
            return Attribute.class;
        } else if (elementType.equals("unit")) {
            return Unit.class;
        }
        return null;
    }

    public static Class getSubclassClass(String elementType) {
        
        if (elementType.equals("instrument")) {
            return InstrumentType.class;
        } else if (elementType.equals("detectorstem")) {
            return DetectorStemType.class;
        } 
        return null;
    }

    public Result getElements(String elementType) {
        if (elementType == null || elementType.isEmpty()) {
            return ok(ApiUtil.createResponse("No elementType has been provided", false));
        }
        Class clazz = getElementClass(elementType);
        if (clazz == null) {        
            return ok(ApiUtil.createResponse("[" + elementType + "] is not a valid elementType", false));
        }
        List<Object> results = (List<Object>)GenericFind.findWithPages(clazz,12,0);
        if (results != null && results.size() >= 0) {
            ObjectMapper mapper = HAScOMapper.getFilteredByClass("essential", clazz);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
        return ok(ApiUtil.createResponse("method getElements() failed to retrieve elements", false));    }

    public Result getSubclasses(String elementType) {
        if (elementType == null || elementType.isEmpty()) {
            return ok(ApiUtil.createResponse("No elementType has been provided", false));
        }
        Class clazz = getSubclassClass(elementType);
        if (clazz == null) {        
            return ok(ApiUtil.createResponse("[" + elementType + "] is not a superclass", false));
        }
        List<Object> results = (List<Object>)GenericFind.findSubclassesWithPages(clazz,12,0);
        if (results != null && results.size() >= 0) {
            ObjectMapper mapper = HAScOMapper.getFilteredByClass("essential", clazz);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
        return ok(ApiUtil.createResponse("method getSubclasses() failed to retrieve subclasses", false));    }

    public Result getTotalElements(String elementType) {
        if (elementType == null || elementType.isEmpty()) {
            return ok(ApiUtil.createResponse("No elementType has been provided", false));
        }
        Class clazz = getElementClass(elementType);
        if (clazz == null) {        
            return ok(ApiUtil.createResponse("[" + elementType + "] is not a valid elementType", false));
        }
        int totalElements = GenericFind.findTotal(clazz);
        if (totalElements >= 0) {
            String totalElementsJSON = "{\"total\":" + totalElements + "}";
            return ok(ApiUtil.createResponse(totalElementsJSON, true));
        }
        return ok(ApiUtil.createResponse("querymethod getTotalElements() failed to retrieve total number of element", false));
    }
        
    public static int getNumberElements(String elementType) {
        if (elementType == null || elementType.isEmpty()) {
            return -1   ;
        }
        Class clazz = getElementClass(elementType);
        if (clazz == null) {        
            return -1;
        }
        return GenericFind.findTotal(clazz);
    }

    public Result getElementsWithPages(String elementType, int pageSize, int offset) {
        if (elementType.equals("instrumenttype")) {
            return InstrumentTypeAPI.getInstrumentTypes();
        } else if (elementType.equals("instrument")) {
            List<Instrument> results = Instrument.find();
            return InstrumentAPI.getInstruments(results);
        } else if (elementType.equals("detectorstemtype")) {
            return DetectorStemTypeAPI.getDetectorStemTypes();
        } else if (elementType.equals("detectorstem")) {
            List<DetectorStem> results = DetectorStem.find();
            return DetectorStemAPI.getDetectorStems(results);
        } else if (elementType.equals("detector")) {
            List<Detector> results = Detector.findDetectors();
            return DetectorAPI.getDetectors(results);
        } else if (elementType.equals("detectorslot")) {
            List<DetectorSlot> results = DetectorSlot.find();
          /*  return DetectorAPI.getDetectorSlots(results); */ 
        } else if (elementType.equals("codebook")) {
            List<Codebook> results = Codebook.find();
            return CodebookAPI.getCodebooks(results);
        } else if (elementType.equals("responseoption")) {
            List<ResponseOption> results = ResponseOption.find();
            return ResponseOptionAPI.getResponseOptions(results);
        } else if (elementType.equals("responseoptionslot")) {
            List<ResponseOptionSlot> results = ResponseOptionSlot.find();
            return ResponseOptionAPI.getResponseOptionSlots(results);
        }
        return ok("No valid element type.");
    }

    public Result getElementsAll2(String elementType) {
        if (elementType.equals("instrumenttype")) {
            return InstrumentTypeAPI.getInstrumentTypes();
        } else if (elementType.equals("instrument")) {
            List<Instrument> results = Instrument.find();
            return InstrumentAPI.getInstruments(results);
        } else if (elementType.equals("detectorstemtype")) {
            return DetectorStemTypeAPI.getDetectorStemTypes();
        } else if (elementType.equals("detectorstem")) {
            List<DetectorStem> results = DetectorStem.find();
            return DetectorStemAPI.getDetectorStems(results);
        } else if (elementType.equals("detector")) {
            List<Detector> results = Detector.findDetectors();
            return DetectorAPI.getDetectors(results);
        } else if (elementType.equals("detectorslot")) {
            List<DetectorSlot> results = DetectorSlot.find();
            return DetectorAPI.getDetectorSlots(results);
        } else if (elementType.equals("codebook")) {
            List<Codebook> results = Codebook.find();
            return CodebookAPI.getCodebooks(results);
        } else if (elementType.equals("responseoption")) {
            List<ResponseOption> results = ResponseOption.find();
            return ResponseOptionAPI.getResponseOptions(results);
        } else if (elementType.equals("responseoptionslot")) {
            List<ResponseOptionSlot> results = ResponseOptionSlot.find();
            return ResponseOptionAPI.getResponseOptionSlots(results);
        }
        return ok("No valid element type.");
    }

    public Result getElementsByKeywordWithPages(String elementType, String keyword, int pageSize, int offset) {
        if (keyword.equals("_")) {
            keyword = "";
        }
        if (elementType.equals("instrument")) {
            GenericFind<Instrument> query = new GenericFind<Instrument>();
            List<Instrument> results = query.findByKeywordWithPages(Instrument.class,keyword, pageSize, offset);
            return InstrumentAPI.getInstruments(results);
        } /* else if (elementType.equals("detectorstem")) {
            List<DetectorStem> results = DetectorStem.findByKeywordAndLanguageWithPages(keyword, language, pageSize, offset);
            return DetectorStemAPI.getDetectorStems(results);
        } else if (elementType.equals("detector")) {
            List<Detector> results = Detector.findDetectorsByKeywordAndLanguageWithPages(keyword, language, pageSize, offset);
            return DetectorAPI.getDetectors(results);
        } else if (elementType.equals("codebook")) {
            List<Codebook> results = Codebook.findByKeywordAndLanguageWithPages(keyword, language, pageSize, offset);
            return CodebookAPI.getCodebooks(results);
        } else if (elementType.equals("responseoption")) {
            List<ResponseOption> results = ResponseOption.findByKeywordAndLanguageWithPages(keyword, language, pageSize, offset);
            return ResponseOptionAPI.getResponseOptions(results);
        }*/
        return ok("No valid element type.");
    }

    public Result getElementsByKeywordAndLanguage(String elementType, String keyword, String language, int pageSize, int offset) {
        if (keyword.equals("_")) {
            keyword = "";
        }
        if (language.equals("_")) {
            language = "";
        }
        if (elementType.equals("instrument")) {
            List<Instrument> results = Instrument.findByKeywordAndLanguageWithPages(keyword, language, pageSize, offset);
            return InstrumentAPI.getInstruments(results);
        } else if (elementType.equals("detectorstem")) {
            List<DetectorStem> results = DetectorStem.findByKeywordAndLanguageWithPages(keyword, language, pageSize, offset);
            return DetectorStemAPI.getDetectorStems(results);
        } else if (elementType.equals("detector")) {
            List<Detector> results = Detector.findDetectorsByKeywordAndLanguageWithPages(keyword, language, pageSize, offset);
            return DetectorAPI.getDetectors(results);
        } else if (elementType.equals("codebook")) {
            List<Codebook> results = Codebook.findByKeywordAndLanguageWithPages(keyword, language, pageSize, offset);
            return CodebookAPI.getCodebooks(results);
        } else if (elementType.equals("responseoption")) {
            List<ResponseOption> results = ResponseOption.findByKeywordAndLanguageWithPages(keyword, language, pageSize, offset);
            return ResponseOptionAPI.getResponseOptions(results);
        }
        return ok("No valid element type.");
    }

    public Result getTotalElementsByKeywordAndLanguage(String elementType, String keyword, String language){
        if (keyword.equals("_")) {
            keyword = "";
        }
        if (language.equals("_")) {
            language = "";
        }
        if (elementType.equals("instrument")) {
            int totalInstruments = Instrument.findTotalByKeywordAndLanguage(keyword, language);
            String totalInstrumentsJSON = "{\"total\":" + totalInstruments + "}";
            return ok(ApiUtil.createResponse(totalInstrumentsJSON, true));
        } else if (elementType.equals("detectorstem")) {
            int totalDetectorStems = DetectorStem.findTotalByKeywordAndLanguage(keyword, language);
            String totalDetectorStemsJSON = "{\"total\":" + totalDetectorStems + "}";
            return ok(ApiUtil.createResponse(totalDetectorStemsJSON, true));
        } else if (elementType.equals("detector")) {
            int totalDetectors = Detector.findTotalDetectorsByKeywordAndLanguage(keyword, language);
            String totalDetectorsJSON = "{\"total\":" + totalDetectors + "}";
            return ok(ApiUtil.createResponse(totalDetectorsJSON, true));
        } else if (elementType.equals("codebook")) {
            int totalCodebooks = Codebook.findTotalByKeywordAndLanguage(keyword, language);
            String totalCodebooksJSON = "{\"total\":" + totalCodebooks + "}";
            return ok(ApiUtil.createResponse(totalCodebooksJSON, true));
        } else if (elementType.equals("responseoption")) {
            int totalResponseOptions = ResponseOption.findTotalByKeywordAndLanguage(keyword, language);
            String totalResponseOptionsJSON = "{\"total\":" + totalResponseOptions + "}";
            return ok(ApiUtil.createResponse(totalResponseOptionsJSON, true));
        }
        return ok("No valid element type.");
    }

    public Result getElementsByManagerEmail(String elementType, String managerEmail, int pageSize, int offset) {
        if (elementType.equals("instrument")) {
            List<Instrument> results = Instrument.findByManagerEmailWithPages(managerEmail, pageSize, offset);
            return InstrumentAPI.getInstruments(results);
        } else if (elementType.equals("detectorStem")) {
            List<DetectorStem> results = DetectorStem.findByManagerEmailWithPages(managerEmail, pageSize, offset);
            return DetectorStemAPI.getDetectorStems(results);
        } else if (elementType.equals("detector")) {
            List<Detector> results = Detector.findDetectorsByManagerEmailWithPages(managerEmail, pageSize, offset);
            return DetectorAPI.getDetectors(results);
        } else if (elementType.equals("codebook")) {
           List<Codebook> results = Codebook.findByManagerEmailWithPages(managerEmail, pageSize, offset);
           return CodebookAPI.getCodebooks(results);
        } else if (elementType.equals("responseoption")) {
            List<ResponseOption> results = ResponseOption.findByManagerEmailWithPages(managerEmail, pageSize, offset);
            return ResponseOptionAPI.getResponseOptions(results);
        }
        return ok("No valid element type.");
    }

    public Result getTotalElementsByManagerEmail(String elementType, String managerEmail){
        if (elementType.equals("instrument")) {
            int totalInstruments = Instrument.findTotalByManagerEmail(managerEmail);
            String totalInstrumentsJSON = "{\"total\":" + totalInstruments + "}";
            return ok(ApiUtil.createResponse(totalInstrumentsJSON, true));
        } else if (elementType.equals("detectorstem")) {
            int totalDetectorStems = Detector.findTotalByManagerEmail(managerEmail);
            String totalDetectorStemsJSON = "{\"total\":" + totalDetectorStems + "}";
            return ok(ApiUtil.createResponse(totalDetectorStemsJSON, true));
        } else if (elementType.equals("detector")) {
            int totalDetectors = Detector.findTotalByManagerEmail(managerEmail);
            String totalDetectorsJSON = "{\"total\":" + totalDetectors + "}";
            return ok(ApiUtil.createResponse(totalDetectorsJSON, true));
        } else if (elementType.equals("codebook")) {
            int totalCodebooks = Codebook.findTotalByManagerEmail(managerEmail);
            String totalCodebooksJSON = "{\"total\":" + totalCodebooks + "}";
            return ok(ApiUtil.createResponse(totalCodebooksJSON, true));
        } else if (elementType.equals("responseoption")) {
            int totalResponseOptions = ResponseOption.findTotalByManagerEmail(managerEmail);
            String totalResponseOptionsJSON = "{\"total\":" + totalResponseOptions + "}";
            return ok(ApiUtil.createResponse(totalResponseOptionsJSON, true));
        }
        return ok("No valid element type.");
    }

    public Result getElementsByKeyword(String elementType, String keyword) {
        if (keyword.equals("_")) {
            keyword = "";
        }
        if (elementType.equals("instrument")) {
            List<Instrument> results = Instrument.findByKeyword(keyword);
            return InstrumentAPI.getInstruments(results);
        } else if (elementType.equals("detectorstem")) {
            List<DetectorStem> results = DetectorStem.findByKeyword(keyword);
            return DetectorStemAPI.getDetectorStems(results);
        } else if (elementType.equals("detector")) {
            List<Detector> results = Detector.findDetectorsByKeyword(keyword);
            return DetectorAPI.getDetectors(results);
        } else if (elementType.equals("codebook")) {
            List<Codebook> results = Codebook.findByKeyword(keyword);
            return CodebookAPI.getCodebooks(results);
        } else if (elementType.equals("responseoption")) {
            List<ResponseOption> results = ResponseOption.findByKeyword(keyword);
            return ResponseOptionAPI.getResponseOptions(results);
        }
        return ok("No valid element type.");
    }

    public Result getElementsByLanguage(String elementType, String language) {
        if (language.equals("_")) {
            language = "";
        }
        if (elementType.equals("instrument")) {
            List<Instrument> results = Instrument.findByLanguage(language);
            return InstrumentAPI.getInstruments(results);
        } else if (elementType.equals("detectorstem")) {
            List<DetectorStem> results = DetectorStem.findByLanguage(language);
            return DetectorStemAPI.getDetectorStems(results);
        } else if (elementType.equals("detector")) {
            List<Detector> results = Detector.findDetectorsByLanguage(language);
            return DetectorAPI.getDetectors(results);
        } else if (elementType.equals("codebook")) {
            List<Codebook> results = Codebook.findByLanguage(language);
            return CodebookAPI.getCodebooks(results);
        } else if (elementType.equals("responseoption")) {
            List<ResponseOption> results = ResponseOption.findByLanguage(language);
            return ResponseOptionAPI.getResponseOptions(results);
        }
        return ok("No valid element type.");
    }

    public Result usage(String elementUri){
        HADatAcThing object = URIPage.objectFromUri(elementUri);
        if (object == null || object.getHascoTypeUri() == null) {
            return ok("No valid element type.");
        }
        String elementType = object.getHascoTypeUri();
        //System.out.println("SIREelementAPI: element type is " + elementType);
        if (elementType.equals(VSTOI.DETECTOR)) {
            List<DetectorSlot> results = Detector.usage(elementUri);
            //System.out.println("SIREelementAPI: Results is " + results.size());
            return DetectorAPI.getDetectorSlots(results);
        } //else if (elementType.equals("detector")) {
        //    int totalDetectors = Detector.findTotalByManagerEmail(managerEmail);
        //    String totalDetectorsJSON = "{\"total\":" + totalDetectors + "}";
        //    return ok(ApiUtil.createResponse(totalDetectorsJSON, true));
        //}
        return ok("No valid element type.");
    }

    public Result derivation(String elementUri){
        HADatAcThing object = URIPage.objectFromUri(elementUri);
        if (object == null || object.getHascoTypeUri() == null) {
            return ok("No valid element type.");
        }
        String elementType = object.getHascoTypeUri();
        //System.out.println("SIREelementAPI: element type is " + elementType);
        if (elementType.equals(VSTOI.DETECTOR)) {
            List<Detector> results = Detector.derivationDetector(elementUri);
            //System.out.println("SIREelementAPI: Results is " + results.size());
            return DetectorAPI.getDetectors(results);
        } //else if (elementType.equals("detector")) {
        //    int totalDetectors = Detector.findTotalByManagerEmail(managerEmail);
        //    String totalDetectorsJSON = "{\"total\":" + totalDetectors + "}";
        //    return ok(ApiUtil.createResponse(totalDetectorsJSON, true));
        //}
        return ok("No valid element type.");
    }

}
