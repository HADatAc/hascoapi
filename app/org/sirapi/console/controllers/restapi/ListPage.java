package org.sirapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.sirapi.entity.pojo.Instrument;
import org.sirapi.utils.ApiUtil;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.List;

public class ListPage extends Controller {

    private final static int MAX_PAGE_SIZE = 80;

    private Result getInstruments(int offset, int pageSize) {
        if (pageSize < 1) {
            pageSize = MAX_PAGE_SIZE;
            System.out.println("[ListPage] Resetting page size");
        }
        ObjectMapper mapper = new ObjectMapper();
        List<Instrument> results = Instrument.findWithPages(pageSize, offset);
        if (results == null) {
            return notFound(ApiUtil.createResponse("No instrument has been found", false));
        } else {
            SimpleFilterProvider filterProvider = new SimpleFilterProvider();
            filterProvider.addFilter("instrumentFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri", "hascoTypeLabel", "comment"));
            mapper.setFilterProvider(filterProvider);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

}
