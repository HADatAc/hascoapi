package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.ContainerSlot;
import org.hascoapi.entity.pojo.ComponentStem;
import org.hascoapi.entity.pojo.Instrument;
import org.hascoapi.entity.pojo.SemanticVariable;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.HAScOMapper;
import org.hascoapi.vocabularies.VSTOI;

import play.mvc.Controller;
import play.mvc.Result;
import static org.hascoapi.Constants.*;
import java.util.List;


public class ComponentStemAPI extends Controller {

    private Result createComponentStemResult(ComponentStem componentStem) {
        componentStem.save();
        return ok(ApiUtil.createResponse("ComponentStem <" + componentStem.getUri() + "> has been CREATED.", true));
    }

    public Result createComponentStemsForTesting() {
        ComponentStem testComponentStem1 = ComponentStem.find(TEST_COMPONENT_STEM1_URI);
        ComponentStem testComponentStem2 = ComponentStem.find(TEST_COMPONENT_STEM2_URI);
        SemanticVariable testSemanticVariable1 = SemanticVariable.find(TEST_SEMANTIC_VARIABLE1_URI);
        SemanticVariable testSemanticVariable2 = SemanticVariable.find(TEST_SEMANTIC_VARIABLE2_URI);
        if (testComponentStem1 != null) {
            return ok(ApiUtil.createResponse("TestComponentStem1 already exists.", false));
        } else if (testComponentStem2 != null) {
            return ok(ApiUtil.createResponse("TestComponentStem2 already exists.", false));
        } else if (testSemanticVariable1 == null || testSemanticVariable2 == null) {
            return ok(ApiUtil.createResponse("Create TestSemanticVariables 1 and 2 before creating TestComponentStems.", false));
        } else {
            testComponentStem1 = new ComponentStem(VSTOI.COMPONENT_STEM);
            testComponentStem1.setUri(TEST_COMPONENT_STEM1_URI);
            testComponentStem1.setLabel("Test Component Stem 1");
            testComponentStem1.setTypeUri(VSTOI.COMPONENT_STEM);
            testComponentStem1.setHascoTypeUri(VSTOI.COMPONENT_STEM);
            testComponentStem1.setComment("This is a dummy Component Stem 1 created to test the SIR API.");
            testComponentStem1.setHasContent("During the last 2 weeks, have you lost appetite?");
            testComponentStem1.setHasLanguage("en"); // ISO 639-1
            testComponentStem1.setHasVersion("1");
            testComponentStem1.setHasSIRManagerEmail("me@example.com");
            testComponentStem1.setIsAssociatedWith(TEST_SEMANTIC_VARIABLE1_URI);
            testComponentStem1.setNamedGraph(Constants.TEST_KB);
            testComponentStem1.save();

            testComponentStem2 = new ComponentStem(VSTOI.COMPONENT_STEM);
            testComponentStem2.setUri(TEST_COMPONENT_STEM2_URI);
            testComponentStem2.setLabel("Test Component Stem 2");
            testComponentStem2.setTypeUri(VSTOI.COMPONENT_STEM);
            testComponentStem2.setHascoTypeUri(VSTOI.COMPONENT_STEM);
            testComponentStem2.setComment("This is a dummy Component Stem 2 created to test the SIR API.");
            testComponentStem2.setHasContent("During the last 2 weeks, have you gain appetite?");
            testComponentStem2.setHasLanguage("en"); // ISO 639-1
            testComponentStem2.setHasVersion("1");
            testComponentStem2.setHasSIRManagerEmail("me@example.com");
            testComponentStem2.setIsAssociatedWith(TEST_SEMANTIC_VARIABLE2_URI);
            testComponentStem2.setNamedGraph(Constants.TEST_KB);
            testComponentStem2.save();
            return ok(ApiUtil.createResponse("Test Component Stems 1 and 2 have been CREATED.", true));
        }
    }

    public Result createComponentStem(String json) {
        if (json == null || json.equals("")) {
            return ok(ApiUtil.createResponse("No json content has been provided.", false));
        }
        //System.out.println("(CreateComponentStem) Value of json: [" + json + "]");
        ObjectMapper objectMapper = new ObjectMapper();
        ComponentStem newComponentStem;
        try {
            //convert json string to Instrument instance
            newComponentStem  = objectMapper.readValue(json, ComponentStem.class);
        } catch (Exception e) {
            //System.out.println("(createComponent) Failed to parse json.");
            return ok(ApiUtil.createResponse("Failed to parse json.", false));
        }
        return createComponentStemResult(newComponentStem);
    }

    private Result deleteComponentStemResult(ComponentStem componentStem) {
        String uri = componentStem.getUri();
        componentStem.delete();
        return ok(ApiUtil.createResponse("Component Stem <" + uri + "> has been DELETED.", true));
    }

    public Result deleteComponentStemsForTesting(){
        ComponentStem test1 = ComponentStem.find(TEST_COMPONENT_STEM1_URI);
        ComponentStem test2 = ComponentStem.find(TEST_COMPONENT_STEM2_URI);
        if (test1 == null) {
            return ok(ApiUtil.createResponse("There is no Test Component Stem 1 to be deleted.", false));
        } else if (test2 == null) {
            return ok(ApiUtil.createResponse("There is no Test Component Stem 2 to be deleted.", false));
        } else {
            test1.setNamedGraph(Constants.TEST_KB);
            test1.delete();
            test2.setNamedGraph(Constants.TEST_KB);
            test2.delete();
            return ok(ApiUtil.createResponse("Test Component Stems 1 and 2 have been DELETED.", true));
        }
    }

    public Result deleteComponentStem(String uri){
        if (uri == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No component setm URI has been provided.", false));
        }
        ComponentStem componentStem = ComponentStem.find(uri);
        if (componentStem == null) {
            return ok(ApiUtil.createResponse("There is no component stem with URI <" + uri + "> to be deleted.", false));
        } else {
            return deleteComponentStemResult(componentStem);
        }
    }

    public Result getComponentStemsByInstrument(String instrumentUri){
        List<ComponentStem> results = ComponentStem.findByInstrument(instrumentUri);
        return getComponentStems(results);
    }

    public static Result getComponentStems(List<ComponentStem> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No component stem has been found", false));
        } else {
            ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL,VSTOI.COMPONENT_STEM);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

}
