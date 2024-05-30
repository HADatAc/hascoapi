package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.PostalAddress;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.HAScOMapper;
import org.hascoapi.vocabularies.SCHEMA;
import play.mvc.Controller;
import play.mvc.Result;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class PostalAddressAPI extends Controller {

    public static Result getPostalAddresses(List<PostalAddress> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No PostalAdress has been found", false));
        } else {
            JsonNode jsonObject = null;
            try {
                ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL,SCHEMA.POSTAL_ADDRESS);
                jsonObject = mapper.convertValue(results, JsonNode.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

}
