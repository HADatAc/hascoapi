package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.DP2;
import org.hascoapi.transform.Renderings;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.HAScOMapper;
import org.hascoapi.vocabularies.SIO;
import org.hascoapi.vocabularies.HASCO;
import play.mvc.Controller;
import play.mvc.Result;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class DP2API extends Controller {

    private Result createDP2Result(DP2 dp2) {
        System.out.println("\n========== DP2API.createDP2Result() START ==========");
        System.out.println("DP2 URI: " + dp2.getUri());
        System.out.println("DP2 Label: " + dp2.getLabel());
        System.out.println("DataFile URI: " + dp2.getHasDataFileUri());
        System.out.println("Status: " + dp2.getHasStatus());

        dp2.save();
        System.out.println("✓ DP2 saved to triple store");

        // Suggest generation URL for debugging
        String datafileUri = dp2.getHasDataFileUri();
        String status = dp2.getHasStatus() != null ? dp2.getHasStatus() : "DRAFT";
        String filename = dp2.getLabel() != null ? dp2.getLabel() + ".xlsx" : "dp2.xlsx";
        String generationUrl = "/api/mt/gen/perstatus/dp2/" +
                              java.net.URLEncoder.encode(datafileUri, java.nio.charset.StandardCharsets.UTF_8) +
                              "/" + status + "/" + filename + "/null/null";

        System.out.println("\n⚠️  DP2 metadata created successfully!");
        System.out.println("    To generate the Excel file, the front-end should call:");
        System.out.println("    POST " + generationUrl);
        System.out.println("    Or: GET " + generationUrl);

        // TODO: Uncomment to automatically generate Excel file after DP2 creation
        // System.out.println("\n🔄 Auto-generating Excel file...");
        // try {
        //     org.hascoapi.transform.mt.dp2.DP2Gen.genByStatus(datafileUri, status, filename, null, null);
        //     System.out.println("✓ Excel file generated automatically");
        // } catch (Exception e) {
        //     System.out.println("✗ Auto-generation failed: " + e.getMessage());
        // }

        System.out.println("========== DP2API.createDP2Result() END ==========\n");

        return ok(ApiUtil.createResponse("DP2 <" + dp2.getUri() + "> has been CREATED.", true));
    }

    public Result createDP2(String json) {
        System.out.println("\n========== DP2API.createDP2() START ==========");
        if (json == null || json.equals("")) {
            System.out.println("✗ No JSON content provided");
            return ok(ApiUtil.createResponse("No json content has been provided.", false));
        }
        System.out.println("Received JSON: " + json);

        ObjectMapper objectMapper = new ObjectMapper();
        DP2 newDP2;
        try {
            newDP2  = objectMapper.readValue(json, DP2.class);
            System.out.println("✓ JSON parsed successfully");
        } catch (Exception e) {
            System.out.println("✗ Failed to parse JSON: " + e.getMessage());
            return ok(ApiUtil.createResponse("Failed to parse json.", false));
        }
        return createDP2Result(newDP2);
    }

    public static Result getDP2s(List<DP2> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No DP2 has been found", false));
        } else {
            //for (DP2 dp2: results) {
            //    System.out.println(dp2.getLabel() + "  [" + dp2.getHasDataFile().getFileStatus() + "]");
            //}
            ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL,HASCO.DP2);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

}
