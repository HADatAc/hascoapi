package org.hascoapi.console.controllers.errorhandlers;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.http.HttpErrorHandler;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;

import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Custom error handler that ensures JSON endpoints never return HTML error pages.
 * Intercepts Play Framework errors and converts them to structured JSON responses.
 */
@Singleton
public class JsonErrorHandler implements HttpErrorHandler {
    
    private static final ObjectMapper mapper = new ObjectMapper();
    
    @Override
    public CompletionStage<Result> onClientError(Http.RequestHeader request, int statusCode, String message) {
        // Check if this is a JSON parsing error for our R-analysis endpoint
        if (request.path().contains("/api/r-analysis/execute") || 
            request.contentType().orElse("").contains("application/json")) {
            
            JsonNode errorResponse = buildJsonError(
                statusCode,
                "invalid_payload",
                "Request processing failed",
                message
            );
            
            return CompletableFuture.completedFuture(
                Results.status(statusCode, errorResponse)
                    .as("application/json")
            );
        }
        
        // For non-JSON endpoints, use default behavior
        return CompletableFuture.completedFuture(
            Results.status(statusCode, message)
        );
    }
    
    @Override
    public CompletionStage<Result> onServerError(Http.RequestHeader request, Throwable exception) {
        // Check if this is a JSON endpoint
        if (request.path().contains("/api/r-analysis/execute") || 
            request.contentType().orElse("").contains("application/json")) {
            
            JsonNode errorResponse = buildJsonError(
                500,
                "internal_server_error",
                "Unexpected server error",
                exception.getMessage()
            );
            
            return CompletableFuture.completedFuture(
                Results.internalServerError(errorResponse)
                    .as("application/json")
            );
        }
        
        // For non-JSON endpoints, use default behavior
        return CompletableFuture.completedFuture(
            Results.internalServerError("Internal server error: " + exception.getMessage())
        );
    }
    
    private JsonNode buildJsonError(int statusCode, String code, String message, String details) {
        ObjectNode response = mapper.createObjectNode();
        response.put("isSuccessful", false);
        
        ObjectNode error = mapper.createObjectNode();
        error.put("code", code);
        error.put("message", message);
        
        if (details != null && !details.isEmpty()) {
            // Check if details contain JSON parsing error info
            if (details.contains("JsonParseException") || details.contains("decoding json")) {
                error.put("details", "Invalid JSON format in request body");
            } else {
                error.put("details", details);
            }
        }
        
        response.set("error", error);
        return response;
    }
}
