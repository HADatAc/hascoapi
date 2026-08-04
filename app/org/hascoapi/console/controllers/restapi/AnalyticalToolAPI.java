package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hascoapi.entity.pojo.AnalyticalTool;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.HAScOMapper;
import org.hascoapi.utils.Utils;
import org.hascoapi.vocabularies.HASCO;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import java.util.List;

public class AnalyticalToolAPI extends Controller {

    public static Result getAnalyticalTools(List<AnalyticalTool> results) {
        if (results == null) {
            return ok(ApiUtil.createResponse("No AnalyticalTool has been found", false));
        }
        ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL, HASCO.ANALYTICAL_TOOL);
        JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
        return ok(ApiUtil.createResponse(jsonObject, true));
    }

    public Result getByProcess(String processUri) {
        if (processUri == null || processUri.trim().isEmpty()) {
            return ok(ApiUtil.createResponse("No process URI has been provided", false));
        }

        List<AnalyticalTool> tools = AnalyticalTool.findByProcessUri(processUri.trim());
        return getAnalyticalTools(tools);
    }

    public Result register(Http.Request request) {
        JsonNode body = request.body().asJson();
        if (body == null || body.isNull()) {
            return badRequest(ApiUtil.createResponse("Expecting JSON data", false));
        }

        ObjectMapper objectMapper = new ObjectMapper();
        AnalyticalTool tool;

        try {
            tool = objectMapper.treeToValue(body, AnalyticalTool.class);
        } catch (Exception e) {
            return ok(ApiUtil.createResponse("Failed to parse AnalyticalTool payload: " + e.getMessage(), false));
        }

        if (tool.getUri() == null || tool.getUri().trim().isEmpty()) {
            tool.setUri(Utils.uriGen("analyticaltool"));
        }
        if (tool.getTypeUri() == null || tool.getTypeUri().trim().isEmpty()) {
            tool.setTypeUri(HASCO.ANALYTICAL_TOOL);
        }
        if (tool.getHascoTypeUri() == null || tool.getHascoTypeUri().trim().isEmpty()) {
            tool.setHascoTypeUri(HASCO.ANALYTICAL_TOOL);
        }

        String processUri = tool.getHasProcessUri() == null ? "" : tool.getHasProcessUri().trim();
        if (processUri.isEmpty()) {
            JsonNode processNode = body.get("processUri");
            if (processNode != null && !processNode.isNull()) {
                processUri = processNode.asText("").trim();
                tool.setHasProcessUri(processUri);
            }
        }

        tool.save();

        if (isHttpUri(processUri)) {
            AnalyticalTool.linkProcessToTool(processUri, tool.getUri());
        }

        ObjectNode resp = objectMapper.createObjectNode();
        resp.put("uri", tool.getUri());
        resp.put("processUri", processUri);
        resp.put("linkedToProcess", isHttpUri(processUri));
        return ok(ApiUtil.createResponse(resp, true));
    }

    public Result linkProcess(String processUri, String toolUri) {
        if (!isHttpUri(processUri) || !isHttpUri(toolUri)) {
            return ok(ApiUtil.createResponse("Both processUri and toolUri must be HTTP(S) URIs", false));
        }

        boolean linked = AnalyticalTool.linkProcessToTool(processUri, toolUri);
        if (!linked) {
            return ok(ApiUtil.createResponse("Failed to create hasco:hasAnalyticalTool relation", false));
        }

        return ok(ApiUtil.createResponse("Process/tool relation has been created", true));
    }

    public Result unlinkProcess(String processUri, String toolUri) {
        if (!isHttpUri(processUri) || !isHttpUri(toolUri)) {
            return ok(ApiUtil.createResponse("Both processUri and toolUri must be HTTP(S) URIs", false));
        }

        boolean unlinked = AnalyticalTool.unlinkProcessFromTool(processUri, toolUri);
        if (!unlinked) {
            return ok(ApiUtil.createResponse("Failed to remove hasco:hasAnalyticalTool relation", false));
        }

        return ok(ApiUtil.createResponse("Process/tool relation has been removed", true));
    }

    private static boolean isHttpUri(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim();
        return normalized.startsWith("http://") || normalized.startsWith("https://");
    }
}
