package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hascoapi.entity.pojo.WKF;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.HAScOMapper;
import org.hascoapi.vocabularies.HASCO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.List;

public class WKFAPI extends Controller {

    private static final Logger logger = LoggerFactory.getLogger(WKFAPI.class);

    private Result createWKFResult(WKF wkf) {
        logger.info("Creating WKF: URI={}, Label={}, DataFileURI={}, Status={}",
                wkf.getUri(), wkf.getLabel(), wkf.getHasDataFileUri(), wkf.getHasStatus());

        wkf.save();
        logger.info("WKF saved successfully to triple store: {}", wkf.getUri());

        // Suggest generation URL for debugging
        String datafileUri = wkf.getHasDataFileUri();
        String status = wkf.getHasStatus() != null ? wkf.getHasStatus() : "DRAFT";
        String filename = wkf.getLabel() != null ? wkf.getLabel() + ".xlsx" : "wkf.xlsx";
        String generationUrl = "/api/mt/gen/perstatus/wkf/" +
                              java.net.URLEncoder.encode(datafileUri, java.nio.charset.StandardCharsets.UTF_8) +
                              "/" + status + "/" + filename + "/null/null";

        logger.info("WKF metadata created successfully. To generate Excel file, call: POST {}", generationUrl);

        return ok(ApiUtil.createResponse("WKF <" + wkf.getUri() + "> has been CREATED.", true));
    }

    public Result createWKF(String json) {
        logger.debug("Creating WKF from JSON");

        if (json == null || json.isEmpty()) {
            logger.warn("No JSON content provided for WKF creation");
            return ok(ApiUtil.createResponse("No json content has been provided.", false));
        }

        logger.trace("Received JSON: {}", json);

        ObjectMapper objectMapper = new ObjectMapper();
        WKF newWKF;
        try {
            newWKF = objectMapper.readValue(json, WKF.class);
            logger.debug("JSON parsed successfully for WKF: {}", newWKF.getUri());
        } catch (Exception e) {
            logger.error("Failed to parse JSON for WKF creation", e);
            return ok(ApiUtil.createResponse("Failed to parse json: " + e.getMessage(), false));
        }
        return createWKFResult(newWKF);
    }

    public static Result getWKFs(List<WKF> results) {
        if (results == null) {
            logger.info("No WKFs found");
            return ok(ApiUtil.createResponse("No WKF has been found", false));
        } else {
            logger.info("Returning {} WKF(s)", results.size());

            try {
                ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL, HASCO.WKF);
                JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);


                return ok(ApiUtil.createResponse(jsonObject, true));

            } catch (Exception e) {
                logger.error("Error serializing WKFs", e);
                return internalServerError(ApiUtil.createResponse("Error serializing WKFs: " + e.getMessage(), false));
            }
        }
    }

    private Result deleteWKFResult(WKF wkf) {
        String uri = wkf.getUri();
        String dataFileUri = wkf.getHasDataFileUri();

        logger.info("=== WKFAPI.deleteWKFResult() START ===");
        logger.info("  WKF URI: {}", uri);
        logger.info("  WKF Label: {}", wkf.getLabel());
        logger.info("  WKF DataFile URI: {}", dataFileUri);
        logger.info("  WKF Status: {}", wkf.getHasStatus());

        try {
            wkf.delete();
            logger.info("  ✓ WKF metadata deleted successfully from triplestore");
        } catch (Exception e) {
            logger.error("  ✗ ERROR deleting WKF metadata: {}", e.getMessage(), e);
            return ok(ApiUtil.createResponse("Failed to delete WKF <" + uri + ">: " + e.getMessage(), false));
        }

        logger.info("=== WKFAPI.deleteWKFResult() END ===");
        return ok(ApiUtil.createResponse("WKF <" + uri + "> has been DELETED.", true));
    }

    public Result deleteWKF(String uri) {
        System.out.println("=== WKFAPI.deleteWKF() CALLED ===");
        System.out.println("  URI parameter: " + uri);

        if (uri == null || uri.isEmpty()) {
            System.out.println("  ERROR: URI is null or empty");
            logger.warn("No WKF URI provided for deletion");
            return ok(ApiUtil.createResponse("No WKF URI has been provided.", false));
        }

        System.out.println("  Calling WKF.find() to retrieve WKF...");
        WKF wkf = WKF.find(uri);

        if (wkf == null) {
            System.out.println("  ERROR: WKF.find() returned null");
            logger.warn("WKF not found for deletion: {}", uri);
            return ok(ApiUtil.createResponse("There is no WKF with URI <" + uri + "> to be deleted.", false));
        } else {
            System.out.println("  WKF found successfully");
            System.out.println("  WKF Label: " + wkf.getLabel());
            System.out.println("  WKF DataFileURI: " + wkf.getHasDataFileUri());
            System.out.println("  Calling deleteWKFResult()...");
            return deleteWKFResult(wkf);
        }
    }
}
