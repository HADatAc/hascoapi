package org.sirapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.sirapi.entity.pojo.DetectorStemType;
import org.sirapi.utils.ApiUtil;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.List;

public class DetectorStemTypeAPI extends Controller {

    public Result getAllDetectorStemTypes(){
        ObjectMapper mapper = new ObjectMapper();

        List<DetectorStemType> results = DetectorStemType.find();
        if (results == null) {
            return notFound(ApiUtil.createResponse("No detector stem type has been found", false));
        } else {
            SimpleFilterProvider filterProvider = new SimpleFilterProvider();
            filterProvider.addFilter("detectorStemTypeFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "comment", "superUri"));
            mapper.setFilterProvider(filterProvider);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

}
