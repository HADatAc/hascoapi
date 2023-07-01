package org.sirapi.console.controllers.restapi;

import org.sirapi.entity.pojo.Detector;
import org.sirapi.entity.pojo.Instrument;
import org.sirapi.utils.ApiUtil;
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
        }
        return ok("No valid element type.");
    }

}
