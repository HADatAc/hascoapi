package org.sirapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
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
        } else if (elementType.equals("instrumenttype")) {
            return InstrumentType.class;
        } else if (elementType.equals("detectorstemtype")) {
            return DetectorStemType.class;
        } else if (elementType.equals("entity")) {
            return Entity.class;
        } else if (elementType.equals("attribute")) {
            return Attribute.class;
        } else if (elementType.equals("unit")) {
            return Unit.class;
        } else if (elementType.equals("agent")) {
            return Agent.class;
        } 
        return null;
    }

    public Result createElement(String elementType, String json) {
        if (json == null || json.equals("")) {
            return ok(ApiUtil.createResponse("No json content has been provided.", false));
        }
        if (elementType == null || elementType.isEmpty()) {
            return ok(ApiUtil.createResponse("No elementType has been provided", false));
        }
        Class clazz = getElementClass(elementType);
        if (clazz == null) {
            return ok(ApiUtil.createResponse("No valid elementType has been provided", false));
        }
        boolean success = true;
        String message = "";
        ObjectMapper objectMapper = new ObjectMapper();
        if (clazz == Instrument.class) {
            Instrument object;
            try {
                object = (Instrument)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                success = false;
                message = e.getMessage();
            }
        } else if (clazz == DetectorStem.class) {
            try {
                DetectorStem object;
                object = (DetectorStem)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                success = false;
                message = e.getMessage();
            }
        } else if (clazz == Detector.class) {
            try {
                Detector object;
                object = (Detector)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                success = false;
                message = e.getMessage();
            }
        } else if (clazz == DetectorSlot.class) {
            try {
                DetectorSlot object;
                object = (DetectorSlot)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                success = false;
                message = e.getMessage();
            }
        } else if (clazz == Codebook.class) {
            try {
                Codebook object;
                object = (Codebook)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                success = false;
                message = e.getMessage();
            }
        } else if (clazz == ResponseOption.class) {
            try {
                ResponseOption object;
                object = (ResponseOption)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                success = false;
                message = e.getMessage();
            }
        } else if (clazz == ResponseOptionSlot.class) {
            try {
                ResponseOptionSlot object;
                object = (ResponseOptionSlot)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                success = false;
                message = e.getMessage();
            }
        } else if (clazz == SemanticVariable.class) {
            try {
                SemanticVariable object;
                object = (SemanticVariable)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                success = false;
                message = e.getMessage();
            }
        } else if (clazz == InstrumentType.class) {
            try {
                InstrumentType object;
                object = (InstrumentType)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                success = false;
                message = e.getMessage();
            }
        } else if (clazz == DetectorStemType.class) {
            try {
                DetectorStemType object;
                object = (DetectorStemType)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                success = false;
                message = e.getMessage();
            }
        } else if (clazz == Entity.class) {
            try {
                Entity object;
                object = (Entity)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                success = false;
                message = e.getMessage();
            }
        } else if (clazz == Attribute.class) {
            try {
                Attribute object;
                object = (Attribute)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                success = false;
                message = e.getMessage();
            }
        } else if (clazz == Unit.class) {
            try {
                Unit object;
                object = (Unit)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                success = false;
                message = e.getMessage();
            }
        } else if (clazz == Agent.class) {
            try {
                Agent object;
                object = (Agent)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                success = false;
                message = e.getMessage();
            }
        } 
        if (!success) {
            return ok(ApiUtil.createResponse("Error processing JSON: " + message, false));
        }
        return ok(ApiUtil.createResponse("Element has been saved", true));
    }

    public Result deleteElement(String elementType, String uri) {
        if (uri == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No uri has been provided.", false));
        }
        if (elementType == null || elementType.isEmpty()) {
            return ok(ApiUtil.createResponse("No elementType has been provided", false));
        }
        Class clazz = getElementClass(elementType);
        if (clazz == null) {
            return ok(ApiUtil.createResponse("No valid elementType has been provided", false));
        }
        if (clazz == Instrument.class) {
            Instrument object = Instrument.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == DetectorStem.class) {
            DetectorStem object = DetectorStem.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == Detector.class) {
            Detector object = Detector.findDetector(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == DetectorSlot.class) {
            DetectorSlot object = DetectorSlot.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == Codebook.class) {
            Codebook object = Codebook.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == ResponseOption.class) {
            ResponseOption object = ResponseOption.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == ResponseOptionSlot.class) {
            ResponseOptionSlot object = ResponseOptionSlot.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == SemanticVariable.class) {
            SemanticVariable object = SemanticVariable.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == InstrumentType.class) {
            InstrumentType object = InstrumentType.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == DetectorStemType.class) {
            DetectorStemType object = DetectorStemType.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == Entity.class) {
            Entity object = Entity.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == Attribute.class) {
            Attribute object = Attribute.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == Unit.class) {
            Unit object = Unit.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == Agent.class) {
            Agent object = Agent.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } 
        return ok(ApiUtil.createResponse("Element with URI [" + uri + "] has been deleted", true));
    }

    public Result getElementsWithPage(String elementType, int pageSize, int offset) {
        if (elementType == null || elementType.isEmpty()) {
            return ok(ApiUtil.createResponse("No elementType has been provided", false));
        }
        if (pageSize <=0) {
            return ok(ApiUtil.createResponse("Page size needs to be greated than zero", false));
        }
        if (offset < 0) {
            return ok(ApiUtil.createResponse("Offset needs to be igual or greater than zero", false));
        }
        Class clazz = getElementClass(elementType);
        if (clazz == null) {        
            return ok(ApiUtil.createResponse("[" + elementType + "] is not a valid elementType", false));
        }
        List<Object> results = (List<Object>)GenericFind.findWithPages(clazz,pageSize,offset);
        if (results != null && results.size() >= 0) {
            ObjectMapper mapper = HAScOMapper.getFilteredByClass("essential", clazz);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
        return ok(ApiUtil.createResponse("method getElements() failed to retrieve elements", false));    
    }

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

    public Result getElementsByKeywordWithPage(String elementType, String keyword, int pageSize, int offset) {
        if (keyword.equals("_")) {
            keyword = "";
        }
        if (elementType.equals("entity")) {
            GenericFind<Entity> query = new GenericFind<Entity>();
            List<Entity> results = query.findByKeywordWithPages(Entity.class,keyword, pageSize, offset);
            return EntityAPI.getEntities(results);
        }  else if (elementType.equals("attribute")) {
            GenericFind<Attribute> query = new GenericFind<Attribute>();
            List<Attribute> results = query.findByKeywordWithPages(Attribute.class,keyword, pageSize, offset);
            return AttributeAPI.getAttributes(results);
        }  else if (elementType.equals("unit")) {
            GenericFind<Unit> query = new GenericFind<Unit>();
            List<Unit> results = query.findByKeywordWithPages(Unit.class,keyword, pageSize, offset);
            return UnitAPI.getUnits(results);
        } 
        return ok("No valid element type.");
    }

    public Result getTotalElementsByKeyword(String elementType, String keyword) {
        if (keyword.equals("_")) {
            keyword = "";
        }
        if (elementType == null || elementType.isEmpty()) {
            return ok(ApiUtil.createResponse("No elementType has been provided", false));
        }
        Class clazz = getElementClass(elementType);
        if (clazz == null) {        
            return ok(ApiUtil.createResponse("[" + elementType + "] is not a valid elementType", false));
        }
        int totalElements = GenericFind.findTotalByKeyword(clazz, keyword);
        if (totalElements >= 0) {
            String totalElementsJSON = "{\"total\":" + totalElements + "}";
            return ok(ApiUtil.createResponse(totalElementsJSON, true));
        }
        return ok(ApiUtil.createResponse("querymethod getTotalElementsByKeyword() failed to retrieve total number of element", false));
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
