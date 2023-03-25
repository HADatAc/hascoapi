package org.sirapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.sirapi.entity.pojo.*;
import org.sirapi.utils.ApiUtil;
import org.sirapi.utils.HAScOMapper;
import org.sirapi.vocabularies.HASCO;
import org.sirapi.vocabularies.VSTOI;
import play.mvc.Controller;
import play.mvc.Result;

public class URIPage extends Controller {

    public Result getUri(String uri){

        if (!uri.startsWith("http://") && !uri.startsWith("https://")) {
            return ok(ApiUtil.createResponse("[" + uri + "] is an invalid URI", false));
        }

        try {

            /*
             *  Now uses GenericInstance to process URI against TripleStore content
             */

            Object finalResult = null;
            String typeUri = null;
            GenericInstance result = GenericInstance.find(uri);
            System.out.println("inside getUri(): URI [" + uri + "]");

            if (result == null) {
                return ok(ApiUtil.createResponse("No generic instance found for uri [" + uri + "]", false));
            }

            /*
            if (result.getHascoTypeUri() == null || result.getHascoTypeUri().isEmpty()) {
                System.out.println("inside getUri(): typeUri [" + result.getTypeUri() + "]");
                if (!result.getTypeUri().equals("http://www.w3.org/2002/07/owl#Class")) {
                    return notFound(ApiUtil.createResponse("No valid HASCO type found for uri [" + uri + "]", false));
                }
            }
             */

            if (result.getHascoTypeUri().equals(VSTOI.INSTRUMENT)) {
                finalResult = Instrument.find(uri);
                System.out.println("URIPage: object is INSTRUMENT");
                if (finalResult != null) {
                    typeUri = ((Instrument) finalResult).getHascoTypeUri();
                }
            } else if (result.getHascoTypeUri().equals(VSTOI.DETECTOR)) {
                finalResult = Detector.find(uri);
                System.out.println("URIPage: object is DETECTOR");
                if (finalResult != null) {
                    typeUri = ((Detector) finalResult).getHascoTypeUri();
                }
            } else {
                finalResult = result;
                if (finalResult != null) {
                    typeUri = ((GenericInstance) finalResult).getHascoTypeUri();
                }
            }
            if (finalResult == null || typeUri == null || typeUri.equals("")){
                return ok(ApiUtil.createResponse("No type-specific instance found for uri [" + uri + "]", false));
            }

            // list object properties and associated classes

            return processResult(finalResult, result.getHascoTypeUri(), uri);
        } catch (Exception e) {
            e.printStackTrace();
            return badRequest(ApiUtil.createResponse("Error processing URI [" + uri + "]", false));
        }

    }

    private Result processResult(Object result, String typeResult, String uri) {
        ObjectMapper mapper = HAScOMapper.getFiltered(typeResult);

        System.out.println("[RestAPI] generating JSON for following object: " + uri);
        JsonNode jsonObject = null;
        try {
            ObjectNode obj = mapper.convertValue(result, ObjectNode.class);
            jsonObject = mapper.convertValue(obj, JsonNode.class);
            //System.out.println(prettyPrintJsonString(jsonObject));
        } catch (Exception e) {
            e.printStackTrace();
            return ok(ApiUtil.createResponse("Error processing the json object for URI [" + uri + "]", false));
        }
        return ok(ApiUtil.createResponse(jsonObject, true));
    }

    public String prettyPrintJsonString(JsonNode jsonNode) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Object json = mapper.readValue(jsonNode.toString(), Object.class);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

}
