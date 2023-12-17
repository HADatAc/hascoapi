package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.hascoapi.entity.pojo.*;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.HAScOMapper;
import org.hascoapi.vocabularies.VSTOI;
import play.mvc.Controller;
import play.mvc.Result;
import java.util.List;
import java.util.ArrayList;

public class SIRElementAPI extends Controller {

    /**
     *   CREATE ELEMENT
     */

    public Result createElement(String elementType, String json) {
        //System.out.println("Type: [" + elementType + "  JSON [" + json + "]");
        if (json == null || json.equals("")) {
            return ok(ApiUtil.createResponse("No json content has been provided.", false));
        }
        if (elementType == null || elementType.isEmpty()) {
            return ok(ApiUtil.createResponse("No elementType has been provided", false));
        }
        Class clazz = GenericFind.getElementClass(elementType);
        //System.out.println("Clazz: [" + clazz + "]");
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
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == Subcontainer.class) {
            try {
                Subcontainer object;
                object = (Subcontainer)objectMapper.readValue(json, clazz);
                //System.out.println("SIRElementAPI.create(Subcontainer): JSON=[" + json + "]");
                object.saveAsSlot();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == DetectorStem.class) {
            try {
                DetectorStem object;
                object = (DetectorStem)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == Detector.class) {
            try {
                Detector object;
                object = (Detector)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == ContainerSlot.class) {
            // NOTE: Use ContainerSlot.createContainerSlots(container,totContainerSlots) to create container slots
        } else if (clazz == Codebook.class) {
            try {
                Codebook object;
                object = (Codebook)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == ResponseOption.class) {
            try {
                ResponseOption object;
                object = (ResponseOption)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == CodebookSlot.class) {
            try {
                CodebookSlot object;
                object = (CodebookSlot)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == AnnotationStem.class) {
            try {
                AnnotationStem object;
                object = (AnnotationStem)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == Annotation.class) {
            try {
                Annotation object;
                object = (Annotation)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == SemanticVariable.class) {
            try {
                SemanticVariable object;
                object = (SemanticVariable)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == InstrumentType.class) {
            try {
                InstrumentType object;
                object = (InstrumentType)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == DetectorStemType.class) {
            try {
                DetectorStemType object;
                object = (DetectorStemType)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == Entity.class) {
            try {
                Entity object;
                object = (Entity)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == Attribute.class) {
            try {
                Attribute object;
                object = (Attribute)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == Unit.class) {
            try {
                Unit object;
                object = (Unit)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == Agent.class) {
            try {
                Agent object;
                object = (Agent)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } else if (clazz == SDD.class) {
            try {
                SDD object;
                object = (SDD)objectMapper.readValue(json, clazz);
                object.save();
            } catch (JsonProcessingException e) {
                message = e.getMessage();
                return ok(ApiUtil.createResponse("Following error parsing JSON for " + clazz + ": " + e.getMessage(), false));
            }
        } 
        if (!success) {
            return ok(ApiUtil.createResponse("Error processing JSON: " + message, false));
        }
        return ok(ApiUtil.createResponse("Element has been saved", true));
    }

    /**
     *   DELETE ELEMENT
     */

    public Result deleteElement(String elementType, String uri) {
        //System.out.println("Delete element => Type: [" + elementType + "]  URI [" + uri + "]");
        if (uri == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No uri has been provided.", false));
        }
        if (elementType == null || elementType.isEmpty()) {
            return ok(ApiUtil.createResponse("No elementType has been provided", false));
        }
        Class clazz = GenericFind.getElementClass(elementType);
        if (clazz == null) {
            return ok(ApiUtil.createResponse("No valid elementType has been provided", false));
        }
        if (clazz == Instrument.class) {
            Instrument object = Instrument.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == Subcontainer.class) {
            //Subcontainer object = Subcontainer.find(uri);
            if (!SlotOperations.deleteSlotElement(uri)) {
                return ok(ApiUtil.createResponse("Failed to delete element with URI [" + uri + "]", false));
            }
            //object.deleteAndDetach();
        } else if (clazz == SlotElement.class) {
            //Subcontainer object = Subcontainer.find(uri);
            if (!SlotOperations.deleteSlotElement(uri)) {
                return ok(ApiUtil.createResponse("Failed to delete element with URI [" + uri + "]", false));
            }
            //object.deleteAndDetach();
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
        } else if (clazz == ContainerSlot.class) {
            //ContainerSlot object = ContainerSlot.find(uri);
            if (!SlotOperations.deleteSlotElement(uri)) {
                return ok(ApiUtil.createResponse("Failed to delete element with URI [" + uri + "]", false));
            }
            //object.delete();
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
        } else if (clazz == CodebookSlot.class) {
            CodebookSlot object = CodebookSlot.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == AnnotationStem.class) {
            AnnotationStem object = AnnotationStem.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } else if (clazz == Annotation.class) {
            Annotation object = Annotation.find(uri);
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
        } else if (clazz == SDD.class) {
            SDD object = SDD.find(uri);
            if (object == null) {
                return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
            }
            object.delete();
        } 
        return ok(ApiUtil.createResponse("Element with URI [" + uri + "] has been deleted", true));
    }

    /**
     *   GET ELEMENTS WITH PAGE
     */

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
        Class clazz = GenericFind.getElementClass(elementType);
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
        Class clazz = GenericFind.getElementClass(elementType);
        if (clazz == null) {        
            return ok(ApiUtil.createResponse("[" + elementType + "] is not a valid elementType", false));
        }
        int totalElements = GenericFind.findTotal(clazz);
        if (totalElements >= 0) {
            String totalElementsJSON = "{\"total\":" + totalElements + "}";
            return ok(ApiUtil.createResponse(totalElementsJSON, true));
        }
        return ok(ApiUtil.createResponse("query method getTotalElements() failed to retrieve total number of element", false));
    }
    
    /**
     *   GET ELEMENTS BY KEYWORD WITH PAGE
     */

    public Result getElementsByKeywordWithPage(String elementType, String keyword, int pageSize, int offset) {
        if (keyword.equals("_")) {
            keyword = "";
        }
        if (elementType.equals("instrument")) {
            GenericFind<Instrument> query = new GenericFind<Instrument>();
            List<Instrument> results = query.findByKeywordWithPages(Instrument.class,keyword, pageSize, offset);
            return InstrumentAPI.getInstruments(results);
        } else if (elementType.equals("detectorstem")) {
            GenericFind<DetectorStem> query = new GenericFind<DetectorStem>();
            List<DetectorStem> results = query.findByKeywordWithPages(DetectorStem.class,keyword, pageSize, offset);
            return DetectorStemAPI.getDetectorStems(results);
        } else if (elementType.equals("detector")) {
            GenericFind<Detector> query = new GenericFind<Detector>();
            List<Detector> results = query.findByKeywordWithPages(Detector.class,keyword, pageSize, offset);
            return DetectorAPI.getDetectors(results);
        } else if (elementType.equals("codebook")) {
            GenericFind<Codebook> query = new GenericFind<Codebook>();
            List<Codebook> results = query.findByKeywordWithPages(Codebook.class,keyword, pageSize, offset);
            return CodebookAPI.getCodebooks(results);
        } else if (elementType.equals("responseoption")) {
            GenericFind<ResponseOption> query = new GenericFind<ResponseOption>();
            List<ResponseOption> results = query.findByKeywordWithPages(ResponseOption.class,keyword, pageSize, offset);
            return ResponseOptionAPI.getResponseOptions(results);
        } else if (elementType.equals("annotationstem")) {
            GenericFind<AnnotationStem> query = new GenericFind<AnnotationStem>();
            List<AnnotationStem> results = query.findByKeywordWithPages(AnnotationStem.class,keyword, pageSize, offset);
            return AnnotationStemAPI.getAnnotationStems(results);
        } else if (elementType.equals("annotation")) {
            GenericFind<Annotation> query = new GenericFind<Annotation>();
            List<Annotation> results = query.findByKeywordWithPages(Annotation.class,keyword, pageSize, offset);
            return AnnotationAPI.getAnnotations(results);
        } else if (elementType.equals("semanticvariable")) {
            GenericFind<SemanticVariable> query = new GenericFind<SemanticVariable>();
            List<SemanticVariable> results = query.findByKeywordWithPages(SemanticVariable.class,keyword, pageSize, offset);
            return SemanticVariableAPI.getSemanticVariables(results);
        } else if (elementType.equals("entity")) {
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
        }  else if (elementType.equals("sdd")) {
            GenericFind<SDD> query = new GenericFind<SDD>();
            List<SDD> results = query.findByKeywordWithPages(SDD.class,keyword, pageSize, offset);
            return SDDAPI.getSDDs(results);
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
        Class clazz = GenericFind.getElementClass(elementType);
        if (clazz == null) {        
            return ok(ApiUtil.createResponse("[" + elementType + "] is not a valid elementType", false));
        }
        int totalElements = GenericFind.findTotalByKeyword(clazz, keyword);
        if (totalElements >= 0) {
            String totalElementsJSON = "{\"total\":" + totalElements + "}";
            return ok(ApiUtil.createResponse(totalElementsJSON, true));
        }
        return ok(ApiUtil.createResponse("query method getTotalElementsByKeyword() failed to retrieve total number of [" + elementType + "]", false));
    }
        
    /**
     *   GET ELEMENTS BY KEYWORD AND LANGUAGE WITH PAGE
     */
                
    public Result getElementsByKeywordAndLanguageWithPage(String elementType, String keyword, String language, int pageSize, int offset) {
        if (keyword.equals("_")) {
            keyword = "";
        }
        if (language.equals("_")) {
            language = "";
        }
        if (elementType.equals("instrument")) {
            GenericFind<Instrument> query = new GenericFind<Instrument>();
            List<Instrument> results = query.findByKeywordAndLanguageWithPages(Instrument.class, keyword, language, pageSize, offset);
            return InstrumentAPI.getInstruments(results);
        } else if (elementType.equals("detectorstem")) {
            GenericFind<DetectorStem> query = new GenericFind<DetectorStem>();
            List<DetectorStem> results = query.findByKeywordAndLanguageWithPages(DetectorStem.class, keyword, language, pageSize, offset);
            return DetectorStemAPI.getDetectorStems(results);
        } else if (elementType.equals("detector")) {
            GenericFind<Detector> query = new GenericFind<Detector>();
            List<Detector> results = query.findByKeywordAndLanguageWithPages(Detector.class, keyword, language, pageSize, offset);
            return DetectorAPI.getDetectors(results);
        } else if (elementType.equals("codebook")) {
            GenericFind<Codebook> query = new GenericFind<Codebook>();
            List<Codebook> results = query.findByKeywordAndLanguageWithPages(Codebook.class, keyword, language, pageSize, offset);
            return CodebookAPI.getCodebooks(results);
        } else if (elementType.equals("responseoption")) {
            GenericFind<ResponseOption> query = new GenericFind<ResponseOption>();
            List<ResponseOption> results = query.findByKeywordAndLanguageWithPages(ResponseOption.class, keyword, language, pageSize, offset);
            return ResponseOptionAPI.getResponseOptions(results);
        } else if (elementType.equals("annotationstem")) {
            GenericFind<AnnotationStem> query = new GenericFind<AnnotationStem>();
            List<AnnotationStem> results = query.findByKeywordAndLanguageWithPages(AnnotationStem.class, keyword, language, pageSize, offset);
            return AnnotationStemAPI.getAnnotationStems(results);
        } else if (elementType.equals("annotation")) {
            GenericFind<Annotation> query = new GenericFind<Annotation>();
            List<Annotation> results = query.findByKeywordAndLanguageWithPages(Annotation.class, keyword, language, pageSize, offset);
            return AnnotationAPI.getAnnotations(results);
        } else if (elementType.equals("semanticvariable")) {
            GenericFind<SemanticVariable> query = new GenericFind<SemanticVariable>();
            List<SemanticVariable> results = query.findByKeywordAndLanguageWithPages(SemanticVariable.class, keyword, language, pageSize, offset);
            return SemanticVariableAPI.getSemanticVariables(results);
        } else if (elementType.equals("entity")) {
            GenericFind<Entity> query = new GenericFind<Entity>();
            List<Entity> results = query.findByKeywordAndLanguageWithPages(Entity.class, keyword, language, pageSize, offset);
            return EntityAPI.getEntities(results);
        }  else if (elementType.equals("attribute")) {
            GenericFind<Attribute> query = new GenericFind<Attribute>();
            List<Attribute> results = query.findByKeywordAndLanguageWithPages(Attribute.class, keyword, language, pageSize, offset);
            return AttributeAPI.getAttributes(results);
        }  else if (elementType.equals("unit")) {
            GenericFind<Unit> query = new GenericFind<Unit>();
            List<Unit> results = query.findByKeywordAndLanguageWithPages(Unit.class, keyword, language, pageSize, offset);
            return UnitAPI.getUnits(results);
        }  else if (elementType.equals("sdd")) {
            GenericFind<SDD> query = new GenericFind<SDD>();
            List<SDD> results = query.findByKeywordAndLanguageWithPages(Unit.class, keyword, language, pageSize, offset);
            return SDDAPI.getSDDs(results);
        } 
        return ok("No valid element type.");
    }

    public Result getTotalElementsByKeywordAndLanguage(String elementType, String keyword, String language) {
        if (keyword.equals("_")) {
            keyword = "";
        }
        if (language.equals("_")) {
            language = "";
        }
        if (elementType == null || elementType.isEmpty()) {
            return ok(ApiUtil.createResponse("No elementType has been provided", false));
        }
        Class clazz = GenericFind.getElementClass(elementType);
        if (clazz == null) {        
            return ok(ApiUtil.createResponse("[" + elementType + "] is not a valid elementType", false));
        }
        int totalElements = GenericFind.findTotalByKeywordAndLanguage(clazz, keyword, language);
        if (totalElements >= 0) {
            String totalElementsJSON = "{\"total\":" + totalElements + "}";
            return ok(ApiUtil.createResponse(totalElementsJSON, true));
        }
        return ok(ApiUtil.createResponse("query method getTotalElementsByKeyword() failed to retrieve total number of [" + elementType + "]", false));
    }
        
    /** 
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
    */

    /**
     *   GET ELEMENTS BY MANAGER EMAIL WITH PAGE
     */

    public Result getElementsByManagerEmail(String elementType, String managerEmail, int pageSize, int offset) {
        if (managerEmail == null || managerEmail.isEmpty()) {
            return ok(ApiUtil.createResponse("No Manager Email has been provided", false));
        }
        if (elementType.equals("semanticvariable")) {
            GenericFind<SemanticVariable> query = new GenericFind<SemanticVariable>();
            List<SemanticVariable> results = query.findByManagerEmailWithPages(SemanticVariable.class, managerEmail, pageSize, offset);
            return SemanticVariableAPI.getSemanticVariables(results);
        } else if (elementType.equals("instrument")) {
            GenericFind<Instrument> query = new GenericFind<Instrument>();
            List<Instrument> results = query.findByManagerEmailWithPages(Instrument.class, managerEmail, pageSize, offset);
            return InstrumentAPI.getInstruments(results);
        }  else if (elementType.equals("detectorstem")) {
            GenericFind<DetectorStem> query = new GenericFind<DetectorStem>();
            List<DetectorStem> results = query.findByManagerEmailWithPages(DetectorStem.class, managerEmail, pageSize, offset);
            return DetectorStemAPI.getDetectorStems(results);
        }  else if (elementType.equals("detector")) {
            GenericFind<Detector> query = new GenericFind<Detector>();
            List<Detector> results = query.findByManagerEmailWithPages(Detector.class, managerEmail, pageSize, offset);
            return DetectorAPI.getDetectors(results);
        }  else if (elementType.equals("codebook")) {
            GenericFind<Codebook> query = new GenericFind<Codebook>();
            List<Codebook> results = query.findByManagerEmailWithPages(Codebook.class, managerEmail, pageSize, offset);
            return CodebookAPI.getCodebooks(results);
        }  else if (elementType.equals("responseoption")) {
            GenericFind<ResponseOption> query = new GenericFind<ResponseOption>();
            List<ResponseOption> results = query.findByManagerEmailWithPages(ResponseOption.class, managerEmail, pageSize, offset);
            return ResponseOptionAPI.getResponseOptions(results);
        }  else if (elementType.equals("annotationstem")) {
            GenericFind<AnnotationStem> query = new GenericFind<AnnotationStem>();
            List<AnnotationStem> results = query.findByManagerEmailWithPages(AnnotationStem.class, managerEmail, pageSize, offset);
            return AnnotationStemAPI.getAnnotationStems(results);
        }  else if (elementType.equals("annotation")) {
            GenericFind<Annotation> query = new GenericFind<Annotation>();
            List<Annotation> results = query.findByManagerEmailWithPages(Annotation.class, managerEmail, pageSize, offset);
            return AnnotationAPI.getAnnotations(results);
        }  else if (elementType.equals("sdd")) {
            GenericFind<SDD> query = new GenericFind<SDD>();
            List<SDD> results = query.findByManagerEmailWithPages(SDD.class, managerEmail, pageSize, offset);
            return SDDAPI.getSDDs(results);
        } 
        return ok("No valid element type.");

    }

    public Result getTotalElementsByManagerEmail(String elementType, String managerEmail){
        //System.out.println("SIRElementAPI: getTotalElementsByManagerEmail");
        if (elementType == null || elementType.isEmpty()) {
            return ok(ApiUtil.createResponse("No elementType has been provided", false));
        }
        Class clazz = GenericFind.getElementClass(elementType);
        if (clazz == null) {        
            return ok(ApiUtil.createResponse("[" + elementType + "] is not a valid elementType", false));
        }
        int totalElements = GenericFind.findTotalByManagerEmail(clazz, managerEmail);
        if (totalElements >= 0) {
            String totalElementsJSON = "{\"total\":" + totalElements + "}";
            return ok(ApiUtil.createResponse(totalElementsJSON, true));
        }
        return ok(ApiUtil.createResponse("query method getTotalElements() failed to retrieve total number of element", false));

    }

    public Result usage(String elementUri){
        HADatAcThing object = URIPage.objectFromUri(elementUri);
        if (object == null || object.getHascoTypeUri() == null) {
            return ok("No valid element type.");
        }
        String elementType = object.getHascoTypeUri();
        //System.out.println("SIREelementAPI: element type is " + elementType);
        if (elementType.equals(VSTOI.DETECTOR)) {
            List<ContainerSlot> results = Detector.usage(elementUri);
            //System.out.println("SIREelementAPI: Results is " + results.size());
            return ContainerSlotAPI.getContainerSlots(results);
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
