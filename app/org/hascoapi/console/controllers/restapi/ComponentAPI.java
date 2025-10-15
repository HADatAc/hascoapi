package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.ContainerSlot;
import org.hascoapi.entity.pojo.ComponentStem;
import org.hascoapi.entity.pojo.Component;
import org.hascoapi.entity.pojo.Instrument;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.HAScOMapper;
import org.hascoapi.vocabularies.VSTOI;

import play.mvc.Controller;
import play.mvc.Result;
import static org.hascoapi.Constants.*;
import java.util.List;


public class ComponentAPI extends Controller {

    /** 
     *   MAINTAINING COMPONENTS
     */

    private Result createComponentResult(Component component) {
        component.save();
        return ok(ApiUtil.createResponse("Component <" + component.getUri() + "> has been CREATED.", true));
    }

    public Result createComponent(String json) {
        if (json == null || json.equals("")) {
            return ok(ApiUtil.createResponse("No json content has been provided.", false));
        }
        //System.out.println("(CreateComponent) Value of json: [" + json + "]");
        ObjectMapper objectMapper = new ObjectMapper();
        Component newComponent;
        try {
            //convert json string to Container instance
            newComponent  = objectMapper.readValue(json, Component.class);
        } catch (Exception e) {
            //System.out.println("(createComponent) Failed to parse json.");
            return ok(ApiUtil.createResponse("Failed to parse json.", false));
        }
        return createComponentResult(newComponent);
    }

    private Result deleteComponentResult(Component component) {
        String uri = component.getUri();
        component.delete();
        return ok(ApiUtil.createResponse("Component <" + uri + "> has been DELETED.", true));
    }

    public Result deleteComponent(String uri){
        if (uri == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No component URI has been provided.", false));
        }
        Component component = Component.find(uri);
        if (component == null) {
            return ok(ApiUtil.createResponse("There is no component with URI <" + uri + "> to be deleted.", false));
        } else {
            return deleteComponentResult(component);
        }
    }

    /** 
     *   TESTING COMPONENTS
     */

    public Result createComponentsForTesting() {
        Component testComponent1 = Component.find(TEST_COMPONENT1_URI);
        Component testComponent2 = Component.find(TEST_COMPONENT2_URI);
        Component testComponent3 = Component.find(TEST_COMPONENT3_URI);
        Component testComponent4 = Component.find(TEST_COMPONENT4_URI);
        if (testComponent1 != null) {
            return ok(ApiUtil.createResponse("Test component 1 already exists.", false));
        } else if (testComponent2 != null) {
            return ok(ApiUtil.createResponse("Test component 2 already exists.", false));
        } else if (testComponent3 != null) {
            return ok(ApiUtil.createResponse("Test component 3 already exists.", false));
        } else if (testComponent4 != null) {
            return ok(ApiUtil.createResponse("Test component 4 already exists.", false));
        } else {
            ComponentStem testComponentStem1 = ComponentStem.find(TEST_COMPONENT_STEM1_URI);
            ComponentStem testComponentStem2 = ComponentStem.find(TEST_COMPONENT_STEM2_URI);
            if (testComponentStem1 == null) {
              return ok(ApiUtil.createResponse("Required TestComponentStem1 does not exist.", false));
            } else if (testComponentStem2 == null) {
              return ok(ApiUtil.createResponse("Required TestComponentStem2 does not exist.", false));
            } else {
                testComponent1 = new Component();
                testComponent1.setUri(TEST_COMPONENT1_URI);
                testComponent1.setLabel("Test Component 1");
                testComponent1.setTypeUri(VSTOI.COMPONENT);
                testComponent1.setHascoTypeUri(VSTOI.COMPONENT);
                testComponent1.setComment("This is a dummy Component 1 created to test the SIR API.");
                testComponent1.setHasComponentStem(TEST_COMPONENT_STEM1_URI);
                testComponent1.setHasCodebook(TEST_CODEBOOK_URI);
                testComponent1.setHasLanguage("en");
                testComponent1.setHasVersion("1");
                testComponent1.setHasSIRManagerEmail("me@example.com");
                testComponent1.setNamedGraph(Constants.TEST_KB);
                testComponent1.save();

                testComponent2 = new Component();
                testComponent2.setUri(TEST_COMPONENT2_URI);
                testComponent2.setLabel("Test Component 2");
                testComponent2.setTypeUri(VSTOI.COMPONENT);
                testComponent2.setHascoTypeUri(VSTOI.COMPONENT);
                testComponent2.setComment("This is a dummy Component 2 created to test the SIR API.");
                testComponent2.setHasComponentStem(TEST_COMPONENT_STEM2_URI);
                testComponent2.setHasCodebook(TEST_CODEBOOK_URI);
                testComponent2.setHasLanguage("en");
                testComponent2.setHasVersion("1");
                testComponent2.setHasSIRManagerEmail("me@example.com");
                testComponent2.setNamedGraph(Constants.TEST_KB);
                testComponent2.save();

                testComponent3 = new Component();
                testComponent3.setUri(TEST_COMPONENT3_URI);
                testComponent3.setLabel("Test Component 3");
                testComponent3.setTypeUri(VSTOI.COMPONENT);
                testComponent3.setHascoTypeUri(VSTOI.COMPONENT);
                testComponent3.setComment("This is a dummy Component 3 created to test the SIR API.");
                testComponent3.setHasComponentStem(TEST_COMPONENT_STEM1_URI);
                testComponent3.setHasCodebook(TEST_CODEBOOK_URI);
                testComponent3.setHasLanguage("en");
                testComponent3.setHasVersion("1");
                testComponent3.setHasSIRManagerEmail("me@example.com");
                testComponent3.setNamedGraph(Constants.TEST_KB);
                testComponent3.save();

                testComponent4 = new Component();
                testComponent4.setUri(TEST_COMPONENT4_URI);
                testComponent4.setLabel("Test Component 4");
                testComponent4.setTypeUri(VSTOI.COMPONENT);
                testComponent4.setHascoTypeUri(VSTOI.COMPONENT);
                testComponent4.setComment("This is a dummy Component 4 created to test the SIR API.");
                testComponent4.setHasComponentStem(TEST_COMPONENT_STEM2_URI);
                testComponent4.setHasCodebook(TEST_CODEBOOK_URI);
                testComponent4.setHasLanguage("en");
                testComponent4.setHasVersion("1");
                testComponent4.setHasSIRManagerEmail("me@example.com");
                testComponent4.setNamedGraph(Constants.TEST_KB);
                testComponent4.save();

            }
            return ok(ApiUtil.createResponse("Test Components 1 and 2 have been CREATED.", true));
        }
    }

    public Result deleteComponentsForTesting(){
        Component test1 = Component.find(TEST_COMPONENT1_URI);
        Component test2 = Component.find(TEST_COMPONENT2_URI);
        Component test3 = Component.find(TEST_COMPONENT3_URI);
        Component test4 = Component.find(TEST_COMPONENT4_URI);
        String msg = "";
        if (test1 == null) {
            msg += "Test Component 1. ";
        } else {
            test1.setNamedGraph(Constants.TEST_KB);
            test1.delete();
        } 
        if (test2 == null) {
            msg += "Test Component 2. ";
        } else {
            test2.setNamedGraph(Constants.TEST_KB);
            test2.delete();
        }
        if (test3 == null) {
            msg += "Test Component 3. ";
        } else {
            test3.setNamedGraph(Constants.TEST_KB);
            test3.delete();
        } 
        if (test4 == null) {
            msg += "Test Component 4. ";
        } else {
            test4.setNamedGraph(Constants.TEST_KB);
            test4.delete();
        }
        if (msg.isEmpty()) {
            return ok(ApiUtil.createResponse("Following components did not exist: " + msg, false));
        } else {
            return ok(ApiUtil.createResponse("Existing Test Components have been DELETED.", true));
        }
    }

    /*** 
     *  QUERYING COMPONENTS
     */


    public Result getComponentsByContainer(String instrumentUri){
        List<Component> results = Component.findComponentsByContainer(instrumentUri);
        return getComponents(results);
    }

    public static Result getComponents(List<Component> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No component has been found", false));
        } else {
            ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL,VSTOI.COMPONENT);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            //System.out.println("DetecttorAPI: [" + ApiUtil.createResponse(jsonObject, true) + "]");
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

    public Result getUsage(String componentUri){
        List<ContainerSlot> results = Component.usage(componentUri);
        return ContainerSlotAPI.getContainerSlots(results);
    }

}
