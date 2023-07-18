package org.sirapi.console.controllers.restapi;

import org.sirapi.entity.pojo.*;
import org.sirapi.utils.ApiUtil;
import org.sirapi.vocabularies.VSTOI;
import play.mvc.Controller;
import play.mvc.Result;
import java.util.List;

public class SIRElementAPI extends Controller {

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
        } else if (elementType.equals("detector")) {
            List<Detector> results = Detector.findByKeywordAndLanguageWithPages(keyword, language, pageSize, offset);
            return DetectorAPI.getDetectors(results);
        } else if (elementType.equals("experience")) {
            List<Experience> results = Experience.findByKeywordAndLanguageWithPages(keyword, language, pageSize, offset);
            return ExperienceAPI.getExperiences(results);
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
        } else if (elementType.equals("detector")) {
            int totalDetectors = Detector.findTotalByKeywordAndLanguage(keyword, language);
            String totalDetectorsJSON = "{\"total\":" + totalDetectors + "}";
            return ok(ApiUtil.createResponse(totalDetectorsJSON, true));
        } else if (elementType.equals("experience")) {
            int totalExperiences = Experience.findTotalByKeywordAndLanguage(keyword, language);
            String totalExperiencesJSON = "{\"total\":" + totalExperiences + "}";
            return ok(ApiUtil.createResponse(totalExperiencesJSON, true));
        } else if (elementType.equals("responseoption")) {
            int totalResponseOptions = ResponseOption.findTotalByKeywordAndLanguage(keyword, language);
            String totalResponseOptionsJSON = "{\"total\":" + totalResponseOptions + "}";
            return ok(ApiUtil.createResponse(totalResponseOptionsJSON, true));
        }
        return ok("No valid element type.");
    }

    public Result getElementsByMaintainerEmail(String elementType, String maintainerEmail, int pageSize, int offset) {
        if (elementType.equals("instrument")) {
            List<Instrument> results = Instrument.findByMaintainerEmailWithPages(maintainerEmail, pageSize, offset);
            return InstrumentAPI.getInstruments(results);
        } else if (elementType.equals("detector")) {
            List<Detector> results = Detector.findByMaintainerEmailWithPages(maintainerEmail, pageSize, offset);
            return DetectorAPI.getDetectors(results);
        } else if (elementType.equals("experience")) {
           List<Experience> results = Experience.findByMaintainerEmailWithPages(maintainerEmail, pageSize, offset);
           return ExperienceAPI.getExperiences(results);
        } else if (elementType.equals("responseoption")) {
            List<ResponseOption> results = ResponseOption.findByMaintainerEmailWithPages(maintainerEmail, pageSize, offset);
            return ResponseOptionAPI.getResponseOptions(results);
        }
        return ok("No valid element type.");
    }

    public Result getTotalElementsByMaintainerEmail(String elementType, String maintainerEmail){
        if (elementType.equals("instrument")) {
            int totalInstruments = Instrument.findTotalByMaintainerEmail(maintainerEmail);
            String totalInstrumentsJSON = "{\"total\":" + totalInstruments + "}";
            return ok(ApiUtil.createResponse(totalInstrumentsJSON, true));
        } else if (elementType.equals("detector")) {
            int totalDetectors = Detector.findTotalByMaintainerEmail(maintainerEmail);
            String totalDetectorsJSON = "{\"total\":" + totalDetectors + "}";
            return ok(ApiUtil.createResponse(totalDetectorsJSON, true));
        } else if (elementType.equals("experience")) {
            int totalExperiences = Experience.findTotalByMaintainerEmail(maintainerEmail);
            String totalExperiencesJSON = "{\"total\":" + totalExperiences + "}";
            return ok(ApiUtil.createResponse(totalExperiencesJSON, true));
        } else if (elementType.equals("responseoption")) {
            int totalResponseOptions = ResponseOption.findTotalByMaintainerEmail(maintainerEmail);
            String totalResponseOptionsJSON = "{\"total\":" + totalResponseOptions + "}";
            return ok(ApiUtil.createResponse(totalResponseOptionsJSON, true));
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
            List<Attachment> results = Detector.usage(elementUri);
            //System.out.println("SIREelementAPI: Results is " + results.size());
            return DetectorAPI.getAttachments(results);
        } //else if (elementType.equals("detector")) {
        //    int totalDetectors = Detector.findTotalByMaintainerEmail(maintainerEmail);
        //    String totalDetectorsJSON = "{\"total\":" + totalDetectors + "}";
        //    return ok(ApiUtil.createResponse(totalDetectorsJSON, true));
        //}
        return ok("No valid element type.");
    }

}
