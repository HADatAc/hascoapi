package org.sirapi.console.controllers.restapi;

import org.sirapi.entity.pojo.*;
import org.sirapi.utils.ApiUtil;
import org.sirapi.vocabularies.VSTOI;
import play.mvc.Controller;
import play.mvc.Result;
import java.util.List;

public class SIRElementAPI extends Controller {

    public Result getElementsAll(String elementType) {
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
