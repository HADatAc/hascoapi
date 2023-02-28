package org.sirapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.sirapi.entity.pojo.DetectorType;
import org.sirapi.utils.ApiUtil;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.List;

public class DetectorTypeAPI extends Controller {

    public Result getAllDetectorTypes(){
        ObjectMapper mapper = new ObjectMapper();

        List<DetectorType> results = DetectorType.find();
        if (results == null) {
            return notFound(ApiUtil.createResponse("No detector type has been found", false));
        } else {
            SimpleFilterProvider filterProvider = new SimpleFilterProvider();
            filterProvider.addFilter("detectorTypeFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "comment", "superUri"));
            mapper.setFilterProvider(filterProvider);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

}
