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

    public Result getUri(String uri) {

        if (!uri.startsWith("http://") && !uri.startsWith("https://")) {
            return ok(ApiUtil.createResponse("[" + uri + "] is an invalid URI", false));
        }

        HADatAcThing finalResult = URIPage.objectFromUri(uri);
        String typeUri = finalResult.getHascoTypeUri();

        if (finalResult == null || typeUri == null || typeUri.equals("")) {
            return ok(ApiUtil.createResponse("No type-specific instance found for uri [" + uri + "]", false));
        }

        return processResult(finalResult, finalResult.getHascoTypeUri(), uri);

    }

    public static HADatAcThing objectFromUri(String uri) {
        String typeUri = "";
        try {

            /*
             * Now uses GenericInstance to process URI against TripleStore content
             */

            Object finalResult = null;
            GenericInstance result = GenericInstance.find(uri);
            // System.out.println("inside getUri(): URI [" + uri + "]");

            if (result == null) {
                System.out.println("No generic instance found for uri [" + uri + "]");
                return null;
            }

            /*
             * if (result.getHascoTypeUri() == null || result.getHascoTypeUri().isEmpty()) {
             * System.out.println("inside getUri(): typeUri [" + result.getTypeUri() + "]");
             * if (!result.getTypeUri().equals("http://www.w3.org/2002/07/owl#Class")) {
             * return notFound(ApiUtil.createResponse("No valid HASCO type found for uri ["
             * + uri + "]", false));
             * }
             * }
             */

            if (result.getHascoTypeUri().equals(VSTOI.INSTRUMENT)) {
                finalResult = Instrument.find(uri);
            } else if (result.getHascoTypeUri().equals(VSTOI.DETECTOR_SLOT)) {
                finalResult = DetectorSlot.find(uri);
            } else if (result.getHascoTypeUri().equals(VSTOI.DETECTOR_STEM)) {
                finalResult = DetectorStem.find(uri);
            } else if (result.getHascoTypeUri().equals(VSTOI.DETECTOR)) {
                finalResult = Detector.findDetector(uri);
            } else if (result.getHascoTypeUri().equals(VSTOI.CODEBOOK)) {
                finalResult = Codebook.find(uri);
            } else if (result.getHascoTypeUri().equals(VSTOI.RESPONSE_OPTION_SLOT)) {
                finalResult = ResponseOptionSlot.find(uri);
            } else if (result.getHascoTypeUri().equals(VSTOI.RESPONSE_OPTION)) {
                finalResult = ResponseOption.find(uri);
            } else {
                finalResult = result;
            }
            return (HADatAcThing) finalResult;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Result processResult(Object result, String typeResult, String uri) {
        ObjectMapper mapper = HAScOMapper.getFiltered("full",typeResult);

        // System.out.println("[RestAPI] generating JSON for following object: " + uri);
        JsonNode jsonObject = null;
        try {
            ObjectNode obj = mapper.convertValue(result, ObjectNode.class);
            jsonObject = mapper.convertValue(obj, JsonNode.class);
            // System.out.println(prettyPrintJsonString(jsonObject));
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
