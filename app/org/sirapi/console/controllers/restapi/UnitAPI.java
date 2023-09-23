package org.sirapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

import org.sirapi.entity.pojo.Unit;
import org.sirapi.transform.Renderings;
import org.sirapi.utils.ApiUtil;
import org.sirapi.vocabularies.SIO;
import org.sirapi.vocabularies.VSTOI;
import play.mvc.Controller;
import play.mvc.Result;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.sirapi.Constants.TEST_UNIT_URI;

public class UnitAPI extends Controller {

    private Result createUnitResult(Unit unit) {
        unit.save();
        return ok(ApiUtil.createResponse("Unit <" + unit.getUri() + "> has been CREATED.", true));
    }

    public Result createUnit(String json) {
        if (json == null || json.equals("")) {
            return ok(ApiUtil.createResponse("No json content has been provided.", false));
        }
        //System.out.println("(UnitAPI) Value of json in createUnit: [" + json + "]");
        ObjectMapper objectMapper = new ObjectMapper();
        Unit newInst;
        try {
            //convert json string to Unit unitance
            newInst  = objectMapper.readValue(json, Unit.class);
        } catch (Exception e) {
            //System.out.println("(UnitAPI) Failed to parse json for [" + json + "]");
            return ok(ApiUtil.createResponse("Failed to parse json.", false));
        }
        return createUnitResult(newInst);
    }

    public Result createUnitForTesting() {
        Unit testUnit = Unit.find(TEST_UNIT_URI);
        if (testUnit != null) {
            return ok(ApiUtil.createResponse("Test unit <" + TEST_UNIT_URI + "> already exists.", false));
        } else {
            testUnit = new Unit();
            testUnit.setUri(TEST_UNIT_URI);
            testUnit.setSuperUri(SIO.UNIT);
            testUnit.setLabel("Test Unit");
            testUnit.setTypeUri(TEST_UNIT_URI);
            testUnit.setHascoTypeUri(SIO.UNIT);
            testUnit.setComment("This is a dummy unit created to test the SIR API.");
            //testUnit.setHasSIRManagerEmail("me@example.com");

            return createUnitResult(testUnit);
        }
    }

    private Result deleteUnitResult(Unit unit) {
        String uri = unit.getUri();
        unit.delete();
        return ok(ApiUtil.createResponse("Unit <" + uri + "> has been DELETED.", true));
    }

    public Result deleteUnit(String uri){
        if (uri == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No unit URI has been provided.", false));
        }
        Unit unit = Unit.find(uri);
        if (unit == null) {
            return ok(ApiUtil.createResponse("There is no unit with URI <" + uri + "> to be deleted.", false));
        } else {
            return deleteUnitResult(unit);
        }
    }

    public Result deleteUnitForTesting(){
        Unit test;
        test = Unit.find(TEST_UNIT_URI);
        if (test == null) {
            return ok(ApiUtil.createResponse("There is no Test unit to be deleted.", false));
        } else {
            return deleteUnitResult(test);
        }
    }

}
