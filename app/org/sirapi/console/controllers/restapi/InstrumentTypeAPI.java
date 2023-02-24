package org.sirapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.sirapi.entity.pojo.InstrumentType;
import org.sirapi.utils.ApiUtil;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.List;

public class InstrumentTypeAPI extends Controller {

    public Result getAllInstrumentTypes(){
        ObjectMapper mapper = new ObjectMapper();

        List<InstrumentType> results = InstrumentType.find();
        if (results == null) {
            return notFound(ApiUtil.createResponse("No instrument type has been found", false));
        } else {
            SimpleFilterProvider filterProvider = new SimpleFilterProvider();
            filterProvider.addFilter("instrumentTypeFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "comment", "superUri"));
            mapper.setFilterProvider(filterProvider);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

}
