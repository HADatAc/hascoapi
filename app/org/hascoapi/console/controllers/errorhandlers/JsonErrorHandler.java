package org.hascoapi.console.controllers.errorhandlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        if (isApiRequest(request)) {
            String code = mapClientErrorCode(statusCode);
            String friendlyMessage = mapClientErrorMessage(statusCode);
            JsonNode errorResponse = buildJsonError(statusCode, code, friendlyMessage, message, request);

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
        if (isApiRequest(request)) {
            Throwable root = rootCause(exception);
            String rootMessage = root == null ? "" : String.valueOf(root.getMessage());
            String rootType = root == null ? "" : root.getClass().getSimpleName();
            boolean triplestoreUnavailable = isTriplestoreConnectivityError(rootType, rootMessage);

            int statusCode = triplestoreUnavailable ? 503 : 500;
            String code = triplestoreUnavailable ? "triplestore_unavailable" : "internal_server_error";
            String message = triplestoreUnavailable
                    ? "Triplestore is unavailable"
                    : "Unexpected server error";
            JsonNode errorResponse = buildJsonError(statusCode, code, message, rootMessage, request);

            return CompletableFuture.completedFuture(
                Results.status(statusCode, errorResponse)
                    .as("application/json")
            );
        }
        
        // For non-JSON endpoints, use default behavior
        return CompletableFuture.completedFuture(
            Results.internalServerError("Internal server error: " + exception.getMessage())
        );
    }

    private boolean isApiRequest(Http.RequestHeader request) {
        String path = request.path() == null ? "" : request.path();
        return path.startsWith("/hascoapi/api/") || path.startsWith("/api/");
    }

    private static String mapClientErrorCode(int statusCode) {
        if (statusCode == 400) return "invalid_payload";
        if (statusCode == 401) return "unauthorized";
        if (statusCode == 403) return "forbidden";
        if (statusCode == 404) return "endpoint_not_found";
        return "client_error";
    }

    private static String mapClientErrorMessage(int statusCode) {
        if (statusCode == 400) return "Invalid request payload";
        if (statusCode == 401) return "Unauthorized";
        if (statusCode == 403) return "Forbidden";
        if (statusCode == 404) return "API endpoint not found";
        return "Request processing failed";
    }

    private static Throwable rootCause(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private static boolean isTriplestoreConnectivityError(String exceptionType, String message) {
        String type = exceptionType == null ? "" : exceptionType.toLowerCase();
        String msg = message == null ? "" : message.toLowerCase();
        return type.contains("connectexception")
                || type.contains("socketexception")
                || type.contains("sockettimeoutexception")
                || type.contains("httptimeoutexception")
                || type.contains("eofexception")
                || type.contains("queryexceptionhttp")
                || type.contains("unresolvedaddressexception")
                || type.contains("unknownhostexception")
                || msg.contains("connectexception")
                || msg.contains("unresolvedaddressexception")
                || msg.contains("eof reached while reading")
                || msg.contains("connection reset")
                || msg.contains("broken pipe")
                || msg.contains("timed out")
                || msg.contains("timeout")
                || msg.contains("connection refused")
                || msg.contains("fuseki");
    }

    private JsonNode buildJsonError(int statusCode, String code, String message, String details, Http.RequestHeader request) {
        ObjectNode response = mapper.createObjectNode();
        response.put("isSuccessful", false);
        
        ObjectNode error = mapper.createObjectNode();
        error.put("code", code);
        error.put("message", message);

        ObjectNode detailsNode = mapper.createObjectNode();
        detailsNode.put("status", statusCode);
        detailsNode.put("path", request.path());
        detailsNode.put("method", request.method());

        if (details != null && !details.isEmpty()) {
            if (details.contains("JsonParseException") || details.contains("decoding json")) {
                detailsNode.put("cause", "Invalid JSON format in request body");
            } else {
                detailsNode.put("cause", details);
            }
        }

        error.set("details", detailsNode);
        
        response.set("error", error);
        return response;
    }
}
