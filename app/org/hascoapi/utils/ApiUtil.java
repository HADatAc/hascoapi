package org.hascoapi.utils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;
import play.libs.Json;

public class ApiUtil {
    public static ObjectNode createResponse(Object response, boolean ok) {
        ObjectNode result = null;
        try {
            result = Json.newObject();
            result.put("isSuccessful", ok);

            // IMPORTANT: Never put null in the body field
            if (response == null) {
                result.put("body", ""); // Use empty string instead of null
            } else if (response instanceof String) {
                String strResponse = (String) response;
                // Also check for "null" string
                if ("null".equals(strResponse)) {
                    result.put("body", ""); // Replace "null" string with empty
                } else {
                    result.put("body", strResponse);
                }
            } else {
                result.set("body", (JsonNode) response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // If exception occurs, create a safe fallback response
            result = Json.newObject();
            result.put("isSuccessful", false);
            result.put("body", "Error creating response: " + e.getMessage());
        }
        return result;
    }
}
