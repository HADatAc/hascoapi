package org.hascoapi.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hascoapi.console.controllers.restapi.RAnalysisAPI;
import org.hascoapi.utils.ConfigProp;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class RAnalysisAPITest {

    private enum Mode {
        SUCCESS,
        FAILURE,
        TIMEOUT
    }

    private static class StubRAnalysisAPI extends RAnalysisAPI {
        private final Mode mode;
        private final boolean authRequired;

        StubRAnalysisAPI(Mode mode, boolean authRequired) {
            this.mode = mode;
            this.authRequired = authRequired;
        }

        @Override
        protected boolean isAuthRequired() {
            return authRequired;
        }

        @Override
        protected ExecutionResult executeRAnalysis(JsonNode payload, Http.Request request, int timeoutSeconds, String runId) {
            if (mode == Mode.TIMEOUT) {
                return timeoutExecution("Rscript", "test", "", "Timed out");
            }
            if (mode == Mode.FAILURE) {
                return failureExecution(1, "Rscript", "test", "", "Execution failed");
            }
            return successExecution("Rscript", "test", "ok", "");
        }

        boolean validateTokenForTest(String token) {
            return validateJwtToken(token);
        }
    }

    @Test
    public void executeReturns200ForValidPayload() {
        StubRAnalysisAPI api = new StubRAnalysisAPI(Mode.SUCCESS, false);

        Result result = api.execute(jsonRequest(validPayload(), null));

        Assertions.assertEquals(200, result.status());
        JsonNode response = Json.parse(Helpers.contentAsString(result));
        Assertions.assertTrue(response.path("isSuccessful").asBoolean(false));
        Assertions.assertFalse(response.path("body").path("runId").asText("").isEmpty());
        Assertions.assertEquals("completed", response.path("body").path("status").asText());
    }

    @Test
    public void validateReturns200ForValidPayload() {
        StubRAnalysisAPI api = new StubRAnalysisAPI(Mode.SUCCESS, false);

        Result result = api.validate(jsonRequest(validPayload(), null));

        Assertions.assertEquals(200, result.status());
        JsonNode response = Json.parse(Helpers.contentAsString(result));
        Assertions.assertTrue(response.path("isSuccessful").asBoolean(false));
        Assertions.assertEquals("validated", response.path("body").path("status").asText());
    }

    @Test
    public void executeReturns400ForInvalidPayload() {
        StubRAnalysisAPI api = new StubRAnalysisAPI(Mode.SUCCESS, false);

        ObjectNode invalid = Json.newObject();
        invalid.put("studyUri", "https://example.org/STD1");

        Result result = api.execute(jsonRequest(invalid, null));

        Assertions.assertEquals(400, result.status());
        JsonNode response = Json.parse(Helpers.contentAsString(result));
        Assertions.assertFalse(response.path("isSuccessful").asBoolean(true));
        Assertions.assertEquals("invalid_payload", response.path("error").path("code").asText());
    }

    @Test
    public void executeReturns400WhenRscriptArgsMissing() {
        StubRAnalysisAPI api = new StubRAnalysisAPI(Mode.SUCCESS, false);

        ObjectNode invalid = validPayload();
        ((ObjectNode) invalid.path("arguments")).remove("rscriptArgs");

        Result result = api.execute(jsonRequest(invalid, null));

        Assertions.assertEquals(400, result.status());
        JsonNode response = Json.parse(Helpers.contentAsString(result));
        Assertions.assertEquals("invalid_payload", response.path("error").path("code").asText());
    }

    @Test
    public void executeReturns400ForMalformedJsonBody() {
        StubRAnalysisAPI api = new StubRAnalysisAPI(Mode.SUCCESS, false);

        Http.Request request = new Http.RequestBuilder()
                .method("POST")
                .uri("/hascoapi/api/r-analysis/execute")
                .header("Content-Type", "application/json")
                .bodyText("{not valid json")
                .build();

        Result result = api.execute(request);

        Assertions.assertEquals(400, result.status());
        JsonNode response = Json.parse(Helpers.contentAsString(result));
        Assertions.assertEquals("invalid_payload", response.path("error").path("code").asText());
    }

    @Test
    public void executeReturns401WhenAuthRequiredAndMissingHeader() {
        StubRAnalysisAPI api = new StubRAnalysisAPI(Mode.SUCCESS, true);

        Result result = api.execute(jsonRequest(validPayload(), null));

        Assertions.assertEquals(401, result.status());
        JsonNode response = Json.parse(Helpers.contentAsString(result));
        Assertions.assertEquals("unauthorized", response.path("error").path("code").asText());
    }

    @Test
    public void executeReturns403WhenAuthSchemeIsInvalid() {
        StubRAnalysisAPI api = new StubRAnalysisAPI(Mode.SUCCESS, true);

        Result result = api.execute(jsonRequest(validPayload(), "Basic abc"));

        Assertions.assertEquals(403, result.status());
        JsonNode response = Json.parse(Helpers.contentAsString(result));
        Assertions.assertEquals("forbidden", response.path("error").path("code").asText());
    }

    @Test
    public void executeReturns403WhenBearerTokenIsInvalid() {
        StubRAnalysisAPI api = new StubRAnalysisAPI(Mode.SUCCESS, true);

        Result result = api.execute(jsonRequest(validPayload(), "Bearer abc.def.ghi"));

        Assertions.assertEquals(403, result.status());
        JsonNode response = Json.parse(Helpers.contentAsString(result));
        Assertions.assertEquals("forbidden", response.path("error").path("code").asText());
    }

    @Test
    public void executeReturns500WhenExecutionFails() {
        StubRAnalysisAPI api = new StubRAnalysisAPI(Mode.FAILURE, false);

        Result result = api.execute(jsonRequest(validPayload(), null));

        Assertions.assertEquals(500, result.status());
        JsonNode response = Json.parse(Helpers.contentAsString(result));
        Assertions.assertEquals("r_execution_failed", response.path("error").path("code").asText());
    }

    @Test
    public void executeReturns504WhenExecutionTimesOut() {
        StubRAnalysisAPI api = new StubRAnalysisAPI(Mode.TIMEOUT, false);

        Result result = api.execute(jsonRequest(validPayload(), null));

        Assertions.assertEquals(504, result.status());
        JsonNode response = Json.parse(Helpers.contentAsString(result));
        Assertions.assertEquals("execution_timeout", response.path("error").path("code").asText());
    }

    @Test
    public void jwtValidationAcceptsValidAndRejectsExpiredToken() throws Exception {
        StubRAnalysisAPI api = new StubRAnalysisAPI(Mode.SUCCESS, false);

        String validToken = createHs256Token(300);
        String expiredToken = createHs256Token(-300);

        Assertions.assertTrue(api.validateTokenForTest(validToken));
        Assertions.assertFalse(api.validateTokenForTest(expiredToken));
    }

    private static Http.Request jsonRequest(JsonNode payload, String authorizationHeader) {
        Http.RequestBuilder builder = new Http.RequestBuilder()
                .method("POST")
                .uri("/hascoapi/api/r-analysis/execute")
                .bodyJson(payload);

        if (authorizationHeader != null && !authorizationHeader.trim().isEmpty()) {
            builder.header("Authorization", authorizationHeader);
        }

        return builder.build();
    }

    private static ObjectNode validPayload() {
        ObjectNode payload = Json.newObject();
        payload.put("studyUri", "https://example.org/STD1");
        payload.put("processUri", "https://example.org/PROC1");
        payload.put("requestedAt", "2026-06-01T10:00:00Z");

        ObjectNode tool = Json.newObject();
        tool.put("toolUri", "https://example.org/tool/R1");
        tool.put("language", "R");
        tool.put("entrypoint", "analysis.R");
        payload.set("tool", tool);

        payload.set("associations", Json.newObject());
        ObjectNode arguments = Json.newObject();
        arguments.set("rscriptArgs", Json.newArray().add("https://example.org/input.csv"));
        payload.set("arguments", arguments);

        ObjectNode requestedBy = Json.newObject();
        requestedBy.put("identifier", "automation@local.pmsr");
        payload.set("requestedBy", requestedBy);

        return payload;
    }

    private static String createHs256Token(long expOffsetSeconds) throws Exception {
        long now = Instant.now().getEpochSecond();
        long exp = now + expOffsetSeconds;

        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson = "{\"sub\":\"test-user\",\"iat\":" + now + ",\"exp\":" + exp + "}";

        String encodedHeader = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

        String signingInput = encodedHeader + "." + encodedPayload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(ConfigProp.getJWTSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signature = mac.doFinal(signingInput.getBytes(StandardCharsets.US_ASCII));

        String encodedSignature = Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        return signingInput + "." + encodedSignature;
    }
}
