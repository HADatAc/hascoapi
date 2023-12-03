package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hascoapi.entity.pojo.*;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.HAScOMapper;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.SIO;
import org.hascoapi.vocabularies.VSTOI;
import play.mvc.Controller;
import play.mvc.Result;

public class URIPage extends Controller {

    public Result getUri(String uri) {

        if (!uri.startsWith("http://") && !uri.startsWith("https://")) {
            return ok(ApiUtil.createResponse("[" + uri + "] is an invalid URI", false));
        }

        HADatAcThing finalResult = URIPage.objectFromUri(uri);
        if (finalResult == null) {
            return ok(ApiUtil.createResponse("Uri [" + uri + "] returned no object from the knowledge graph", false));
        }

        String typeUri = finalResult.getHascoTypeUri();
        if (typeUri == null || typeUri.equals("")) {
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
            //System.out.println("URIPage.objectFromUri() [1]: URI [" + uri + "]");

            if (result == null) {
                System.out.println("No generic instance found for uri [" + uri + "]");
                return null;
            }
            //System.out.println("URIPage.objectFromUri() [1]: URI [" + uri + "]");

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
            } else if (result.getHascoTypeUri().equals(VSTOI.CONTAINER_SLOT)) {
                finalResult = ContainerSlot.find(uri);
            } else if (result.getHascoTypeUri().equals(VSTOI.DETECTOR_STEM)) {
                finalResult = DetectorStem.find(uri);
            } else if (result.getHascoTypeUri().equals(VSTOI.DETECTOR)) {
                finalResult = Detector.findDetector(uri);
            } else if (result.getHascoTypeUri().equals(VSTOI.CODEBOOK)) {
                finalResult = Codebook.find(uri);            
            } else if (result.getHascoTypeUri().equals(VSTOI.CODEBOOK_SLOT)) {
                finalResult = CodebookSlot.find(uri);
            } else if (result.getHascoTypeUri().equals(VSTOI.RESPONSE_OPTION)) {
                finalResult = ResponseOption.find(uri);
            } else if (result.getHascoTypeUri().equals(VSTOI.ANNOTATION_STEM)) {
                finalResult = AnnotationStem.find(uri);
            } else if (result.getHascoTypeUri().equals(VSTOI.ANNOTATION)) {
                finalResult = Annotation.find(uri);
            } else if (result.getHascoTypeUri().equals(HASCO.SEMANTIC_VARIABLE)) {
                finalResult = SemanticVariable.find(uri);
            } else if (result.getHascoTypeUri().equals(SIO.ENTITY)) {
                finalResult = Entity.find(uri);
            } else if (result.getHascoTypeUri().equals(SIO.ATTRIBUTE)) {
                finalResult = Attribute.find(uri);
            } else if (result.getHascoTypeUri().equals(SIO.UNIT)) {
                finalResult = Unit.find(uri);
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

        //System.out.println("[RestAPI] generating JSON for following object: " + uri);
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
