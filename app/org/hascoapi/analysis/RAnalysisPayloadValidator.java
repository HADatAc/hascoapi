package org.hascoapi.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.libs.Json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RAnalysisPayloadValidator {

    public static final class ValidationError {
        private final String field;
        private final String message;

        public ValidationError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public String getMessage() {
            return message;
        }

        public ObjectNode toJson() {
            ObjectNode node = Json.newObject();
            node.put("field", field);
            node.put("message", message);
            return node;
        }
    }

    public static final class ValidationResult {
        private final List<ValidationError> errors;

        public ValidationResult(List<ValidationError> errors) {
            this.errors = errors == null ? Collections.emptyList() : errors;
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public List<ValidationError> getErrors() {
            return errors;
        }

        public ArrayNode toJsonDetails() {
            ArrayNode arrayNode = Json.newArray();
            for (ValidationError error : errors) {
                arrayNode.add(error.toJson());
            }
            return arrayNode;
        }
    }

    private RAnalysisPayloadValidator() {
    }

    public static ValidationResult validate(JsonNode payload) {
        List<ValidationError> errors = new ArrayList<>();

        if (payload == null || payload.isMissingNode() || payload.isNull()) {
            errors.add(new ValidationError("body", "JSON body is required"));
            return new ValidationResult(errors);
        }

        requireText(payload, "studyUri", errors);
        requireText(payload, "processUri", errors);
        requireText(payload, "requestedAt", errors);

        JsonNode tool = requireObject(payload, "tool", errors);
        if (tool != null) {
            requireText(tool, "toolUri", errors, "tool.toolUri");
            requireText(tool, "language", errors, "tool.language");
            requireText(tool, "entrypoint", errors, "tool.entrypoint");

            String language = text(tool, "language");
            if (!language.isEmpty() && !"r".equalsIgnoreCase(language.trim())) {
                errors.add(new ValidationError("tool.language", "Expected language R"));
            }
        }

        requireObject(payload, "associations", errors);
        JsonNode arguments = requireObject(payload, "arguments", errors);
        if (arguments != null) {
            JsonNode rscriptArgs = arguments.path("rscriptArgs");
            if (rscriptArgs.isMissingNode() || rscriptArgs.isNull() || !rscriptArgs.isArray()) {
                errors.add(new ValidationError("arguments.rscriptArgs", "Required array with at least one input argument"));
            } else {
                int nonEmptyArgs = 0;
                for (JsonNode arg : rscriptArgs) {
                    if (arg != null && arg.isTextual() && !arg.asText("").trim().isEmpty()) {
                        nonEmptyArgs++;
                    }
                }
                if (nonEmptyArgs == 0) {
                    errors.add(new ValidationError("arguments.rscriptArgs", "Provide at least one non-empty argument (for example dataset URL/path)"));
                }
            }
        }

        JsonNode requestedBy = requireObject(payload, "requestedBy", errors);
        if (requestedBy != null) {
            String uid = text(requestedBy, "uid");
            String identifier = text(requestedBy, "identifier");
            if (uid.isEmpty() && identifier.isEmpty()) {
                errors.add(new ValidationError("requestedBy", "Either requestedBy.uid or requestedBy.identifier must be provided"));
            }
        }

        return new ValidationResult(errors);
    }

    private static JsonNode requireObject(JsonNode root, String field, List<ValidationError> errors) {
        JsonNode node = root.path(field);
        if (node.isMissingNode() || node.isNull() || !node.isObject()) {
            errors.add(new ValidationError(field, "Required object field"));
            return null;
        }
        return node;
    }

    private static JsonNode requireObject(JsonNode root, String field, List<ValidationError> errors, String fullPath) {
        JsonNode node = root.path(field);
        if (node.isMissingNode() || node.isNull() || !node.isObject()) {
            errors.add(new ValidationError(fullPath, "Required object field"));
            return null;
        }
        return node;
    }

    private static void requireText(JsonNode root, String field, List<ValidationError> errors) {
        requireText(root, field, errors, field);
    }

    private static void requireText(JsonNode root, String field, List<ValidationError> errors, String fullPath) {
        String value = text(root, field);
        if (value.isEmpty()) {
            errors.add(new ValidationError(fullPath, "Required non-empty string"));
        }
    }

    private static String text(JsonNode root, String field) {
        JsonNode node = root.path(field);
        if (node.isMissingNode() || node.isNull()) {
            return "";
        }
        return node.asText("").trim();
    }
}
