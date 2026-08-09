package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.hascoapi.analysis.RAnalysisPayloadValidator;
import org.hascoapi.utils.ConfigProp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class RAnalysisAPI extends Controller {

    private static final Logger LOGGER = LoggerFactory.getLogger(RAnalysisAPI.class);
    private static final ConcurrentMap<String, ObjectNode> ENGINE_RUN_CACHE = new ConcurrentHashMap<>();
    private static final int ENGINE_RUN_CACHE_MAX = 500;

    public Result engineHealth(Http.Request request) {
        String runId = generateRunId();
        String engineBaseUrl = resolveEngineBaseUrl();
        try {
            Result authResult = validateAuthIfRequired(request, runId);
            if (authResult != null) {
                return authResult;
            }

            HttpResult engine = callREngine("GET", "/health", null, resolveEngineTimeoutSeconds(30));
            if (!engine.is2xx()) {
                ObjectNode details = Json.newObject();
                details.put("runId", runId);
                details.put("engineBaseUrl", engineBaseUrl);
                details.put("engineEndpoint", engineBaseUrl + "/health");
                details.put("status", engine.statusCode);
                details.put("body", summarizeOutput(engine.body));
                return status(502, errorResponse("r_engine_unavailable", "R engine health check failed", details));
            }

            ObjectNode body = Json.newObject();
            body.put("runId", runId);
            body.put("status", "ok");
            body.set("engine", parseJsonOrText(engine.body));

            ObjectNode success = Json.newObject();
            success.put("isSuccessful", true);
            success.set("body", body);
            return ok(success);
        } catch (Exception e) {
            ObjectNode details = Json.newObject();
            details.put("runId", runId);
            details.put("engineBaseUrl", engineBaseUrl);
            details.put("engineEndpoint", engineBaseUrl + "/health");
            details.put("message", e.getMessage() == null ? "Unexpected server error" : e.getMessage());
            return internalServerError(errorResponse("r_engine_unavailable", "Unable to reach R engine", details));
        }
    }

    public Result runViaEngine(Http.Request request) {
        String runId = generateRunId();
        Instant startedAt = Instant.now();
        String engineBaseUrl = resolveEngineBaseUrl();

        try {
            Result authResult = validateAuthIfRequired(request, runId);
            if (authResult != null) {
                return authResult;
            }

            JsonNode payload = request.body().asJson();
            if (payload == null || payload.isNull()) {
                ArrayNode details = Json.newArray();
                details.add(errorDetail("body", "Expecting JSON body"));
                return badRequest(errorResponse("invalid_payload", "Malformed or missing JSON payload", details));
            }

            String code = firstNonEmpty(
                    text(payload, "code"),
                    text(payload.path("tool"), "inlineCode")
            );
            if (code.isEmpty()) {
                ArrayNode details = Json.newArray();
                details.add(errorDetail("code", "Required non-empty R code string"));
                return badRequest(errorResponse("invalid_payload", "Payload validation failed", details));
            }

            String requestedJobId = firstNonEmpty(text(payload, "jobId"), text(payload, "job_id"));
            String inputCsvText = firstNonEmpty(text(payload, "inputCsvText"), text(payload, "input_csv_text"));
            String inputCsvBase64 = firstNonEmpty(text(payload, "inputCsvBase64"), text(payload, "input_csv_base64"));

            ObjectNode enginePayload = Json.newObject();
            enginePayload.put("code", code);
            if (!requestedJobId.isEmpty()) {
                enginePayload.put("job_id", requestedJobId);
            }
            if (!inputCsvText.isEmpty()) {
                enginePayload.put("input_csv_text", inputCsvText);
            }
            if (!inputCsvBase64.isEmpty()) {
                enginePayload.put("input_csv_base64", inputCsvBase64);
            }

            int timeoutSeconds = resolveEngineTimeoutSeconds(resolveTimeoutSeconds(payload));
            HttpResult engine = callREngine("POST", "/run", enginePayload, timeoutSeconds);
            if (!engine.is2xx()) {
                ObjectNode details = Json.newObject();
                details.put("runId", runId);
                details.put("engineBaseUrl", engineBaseUrl);
                details.put("engineEndpoint", engineBaseUrl + "/run");
                details.put("status", engine.statusCode);
                details.put("body", summarizeOutput(engine.body));
                return status(502, errorResponse("r_engine_execution_failed", "R engine rejected execution request", details));
            }

            JsonNode engineBodyNode = parseJsonOrText(engine.body);
            String jobId = text(engineBodyNode, "job_id");
            if (jobId.isEmpty()) {
                jobId = requestedJobId.isEmpty() ? runId : requestedJobId;
            }

            ObjectNode record = Json.newObject();
            record.put("runId", runId);
            record.put("jobId", jobId);
            record.put("startedAt", startedAt.toString());
            record.put("finishedAt", Instant.now().toString());
            record.put("status", text(engineBodyNode, "ok").equalsIgnoreCase("true") || engineBodyNode.path("ok").asBoolean(false) ? "completed" : "failed");
            record.set("engineResponse", engineBodyNode);

            cacheEngineRun(jobId, record);

            ObjectNode body = Json.newObject();
            body.put("runId", runId);
            body.put("jobId", jobId);
            body.put("status", record.path("status").asText("completed"));
            body.set("engine", engineBodyNode);
            body.set("artifacts", buildArtifactList(jobId, engineBodyNode.path("artifacts")));

            ObjectNode success = Json.newObject();
            success.put("isSuccessful", true);
            success.set("body", body);
            return ok(success);

        } catch (Exception e) {
            ObjectNode details = Json.newObject();
            details.put("runId", runId);
            details.put("engineBaseUrl", engineBaseUrl);
            details.put("engineEndpoint", engineBaseUrl + "/run");
            details.put("exception", e.getClass().getSimpleName());
            details.put("message", e.getMessage() == null ? "Unexpected server error" : e.getMessage());
            if (e.getCause() != null) {
                details.put("causeException", e.getCause().getClass().getSimpleName());
                details.put("causeMessage", e.getCause().getMessage() == null ? "" : e.getCause().getMessage());
            }
            return internalServerError(errorResponse("r_engine_execution_failed", "Unexpected runtime failure", details));
        }
    }

    public Result getEngineRun(String jobId, Http.Request request) {
        String runId = generateRunId();
        try {
            Result authResult = validateAuthIfRequired(request, runId);
            if (authResult != null) {
                return authResult;
            }

            ObjectNode record = ENGINE_RUN_CACHE.get(jobId);
            if (record == null) {
                ObjectNode details = Json.newObject();
                details.put("runId", runId);
                details.put("jobId", jobId);
                return notFound(errorResponse("job_not_found", "No R execution record found for jobId", details));
            }

            ObjectNode success = Json.newObject();
            success.put("isSuccessful", true);
            success.set("body", record);
            return ok(success);
        } catch (Exception e) {
            ObjectNode details = Json.newObject();
            details.put("runId", runId);
            details.put("jobId", jobId);
            details.put("message", e.getMessage() == null ? "Unexpected server error" : e.getMessage());
            return internalServerError(errorResponse("internal_server_error", "Failed to retrieve R execution record", details));
        }
    }

    public Result getEngineArtifacts(String jobId, Http.Request request) {
        String runId = generateRunId();
        try {
            Result authResult = validateAuthIfRequired(request, runId);
            if (authResult != null) {
                return authResult;
            }

            ObjectNode record = ENGINE_RUN_CACHE.get(jobId);
            if (record == null) {
                ObjectNode details = Json.newObject();
                details.put("runId", runId);
                details.put("jobId", jobId);
                return notFound(errorResponse("job_not_found", "No R execution record found for jobId", details));
            }

            JsonNode artifactsNode = record.path("engineResponse").path("artifacts");
            ObjectNode body = Json.newObject();
            body.put("jobId", jobId);
            body.set("artifacts", buildArtifactList(jobId, artifactsNode));

            ObjectNode success = Json.newObject();
            success.put("isSuccessful", true);
            success.set("body", body);
            return ok(success);
        } catch (Exception e) {
            ObjectNode details = Json.newObject();
            details.put("runId", runId);
            details.put("jobId", jobId);
            details.put("message", e.getMessage() == null ? "Unexpected server error" : e.getMessage());
            return internalServerError(errorResponse("internal_server_error", "Failed to retrieve R artifacts", details));
        }
    }

    public Result downloadEngineArtifact(String jobId, String artifactPath, Http.Request request) {
        String runId = generateRunId();
        try {
            Result authResult = validateAuthIfRequired(request, runId);
            if (authResult != null) {
                return authResult;
            }

            String cleanArtifactPath = sanitizeArtifactPath(artifactPath);
            if (cleanArtifactPath.isEmpty()) {
                ArrayNode details = Json.newArray();
                details.add(errorDetail("artifactPath", "Invalid artifact path"));
                return badRequest(errorResponse("invalid_payload", "Artifact path is invalid", details));
            }

            ObjectNode record = ENGINE_RUN_CACHE.get(jobId);
            if (record == null) {
                ObjectNode details = Json.newObject();
                details.put("runId", runId);
                details.put("jobId", jobId);
                return notFound(errorResponse("job_not_found", "No R execution record found for jobId", details));
            }

            if (!artifactExistsInRecord(record, cleanArtifactPath)) {
                ObjectNode details = Json.newObject();
                details.put("runId", runId);
                details.put("jobId", jobId);
                details.put("artifactPath", cleanArtifactPath);
                return notFound(errorResponse("artifact_not_found", "Artifact is not registered for this job", details));
            }

            Path root = resolveEngineArtifactRoot();
            if (root == null) {
                ObjectNode details = Json.newObject();
                details.put("runId", runId);
                details.put("jobId", jobId);
                return badRequest(errorResponse("artifact_root_not_configured", "Artifact root is not configured. Set hascoapi.r_engine.artifact_root or R_ENGINE_ARTIFACT_ROOT.", details));
            }

            Path resolved = root.resolve(jobId).resolve(cleanArtifactPath).normalize();
            Path allowedRoot = root.resolve(jobId).normalize();
            if (!resolved.startsWith(allowedRoot) || !Files.exists(resolved) || Files.isDirectory(resolved)) {
                ObjectNode details = Json.newObject();
                details.put("runId", runId);
                details.put("jobId", jobId);
                details.put("artifactPath", cleanArtifactPath);
                return notFound(errorResponse("artifact_not_found", "Artifact file does not exist", details));
            }

            String contentType = Files.probeContentType(resolved);
            if (contentType == null || contentType.isEmpty()) {
                contentType = "application/octet-stream";
            }
            return ok(Files.newInputStream(resolved)).as(contentType);

        } catch (Exception e) {
            ObjectNode details = Json.newObject();
            details.put("runId", runId);
            details.put("jobId", jobId);
            details.put("message", e.getMessage() == null ? "Unexpected server error" : e.getMessage());
            return internalServerError(errorResponse("internal_server_error", "Failed to download artifact", details));
        }
    }

    public Result validate(Http.Request request) {
        String runId = generateRunId();

        try {
            Result authResult = validateAuthIfRequired(request, runId);
            if (authResult != null) {
                return authResult;
            }

            JsonNode payload = request.body().asJson();
            if (payload == null) {
                ArrayNode details = Json.newArray();
                details.add(errorDetail("body", "Expecting JSON body"));
                LOGGER.warn("RAnalysis validate failed runId={} details={}", runId, details);
                return badRequest(errorResponse("invalid_payload", "Malformed or missing JSON payload", details));
            }

            RAnalysisPayloadValidator.ValidationResult validation = RAnalysisPayloadValidator.validate(payload);
            if (!validation.isValid()) {
                ArrayNode details = validation.toJsonDetails();
                LOGGER.warn("RAnalysis validate failed runId={} details={}", runId, details);
                return badRequest(errorResponse("invalid_payload", "Payload validation failed", details));
            }

            ObjectNode body = Json.newObject();
            body.put("runId", runId);
            body.put("status", "validated");
            body.put("validatedAt", Instant.now().toString());

            ObjectNode success = Json.newObject();
            success.put("isSuccessful", true);
            success.set("body", body);

            return ok(success);
        } catch (Exception e) {
            LOGGER.error("RAnalysis validate unhandled failure runId={} message={}", runId, e.getMessage(), e);
            ObjectNode details = Json.newObject();
            details.put("runId", runId);
            details.put("exception", e.getClass().getSimpleName());
            details.put("message", e.getMessage() == null ? "Unexpected server error" : e.getMessage());
            return internalServerError(errorResponse("internal_server_error", "Unexpected runtime failure", details));
        }
    }

    public Result execute(Http.Request request) {
        String runId = generateRunId();
        long startedMs = System.currentTimeMillis();
        Instant startedAt = Instant.now();

        try {
            Result authResult = validateAuthIfRequired(request, runId);
            if (authResult != null) {
                return authResult;
            }

            JsonNode payload = request.body().asJson();
            if (payload == null) {
                ArrayNode details = Json.newArray();
                details.add(errorDetail("body", "Expecting JSON body"));
                LOGGER.warn("RAnalysis execute validation failed runId={} details={}", runId, details);
                return badRequest(errorResponse("invalid_payload", "Malformed or missing JSON payload", details));
            }

            RAnalysisPayloadValidator.ValidationResult validation = RAnalysisPayloadValidator.validate(payload);
            if (!validation.isValid()) {
                ArrayNode details = validation.toJsonDetails();
                LOGGER.warn("RAnalysis execute validation failed runId={} details={}", runId, details);
                return badRequest(errorResponse("invalid_payload", "Payload validation failed", details));
            }

            String studyUri = text(payload, "studyUri");
            String processUri = text(payload, "processUri");
            JsonNode toolNode = payload.path("tool");
            String toolUri = text(toolNode, "toolUri");

            LOGGER.info("RAnalysis execute started runId={} studyUri={} processUri={} toolUri={}", runId, studyUri, processUri, toolUri);

            int timeoutSeconds = resolveTimeoutSeconds(payload);
            ExecutionResult executionResult = executeRAnalysis(payload, request, timeoutSeconds, runId);

            long durationMs = System.currentTimeMillis() - startedMs;
            Instant finishedAt = Instant.now();

            if (executionResult.timedOut) {
                ObjectNode details = Json.newObject();
                details.put("runId", runId);
                details.put("timeoutSeconds", timeoutSeconds);
                details.put("stdoutSummary", summarizeOutput(executionResult.stdout));
                details.put("stderrSummary", summarizeOutput(executionResult.stderr));

                LOGGER.error("RAnalysis execute timeout runId={} studyUri={} processUri={} toolUri={} durationMs={}",
                        runId, studyUri, processUri, toolUri, durationMs);
                return status(504, errorResponse("execution_timeout", "Execution exceeded timeout window", details));
            }

            if (!executionResult.success) {
                String stderrLower = executionResult.stderr == null ? "" : executionResult.stderr.toLowerCase();
                boolean runtimeMissing = executionResult.exitCode == -1 &&
                        (stderrLower.contains("cannot run program") || stderrLower.contains("failed to start rscript"));

                ObjectNode details = Json.newObject();
                details.put("runId", runId);
                details.put("exitCode", executionResult.exitCode);
                details.put("stdoutSummary", summarizeOutput(executionResult.stdout));
                details.put("stderrSummary", summarizeOutput(executionResult.stderr));

                LOGGER.error("RAnalysis execution failed runId={} studyUri={} processUri={} toolUri={} durationMs={} exitCode={}",
                        runId, studyUri, processUri, toolUri, durationMs, executionResult.exitCode);
                if (runtimeMissing) {
                    return internalServerError(errorResponse("r_runtime_unavailable", "R runtime is unavailable (Rscript not found)", details));
                }
                return internalServerError(errorResponse("r_execution_failed", "Rscript process failed", details));
            }

            ObjectNode responseBody = Json.newObject();
            responseBody.put("runId", runId);
            responseBody.put("status", "completed");
            responseBody.put("startedAt", startedAt.toString());
            responseBody.put("finishedAt", finishedAt.toString());
            responseBody.put("durationMs", durationMs);

            ObjectNode engine = Json.newObject();
            engine.put("name", executionResult.engineName);
            engine.put("version", executionResult.engineVersion);
            responseBody.set("engine", engine);

            ObjectNode input = Json.newObject();
            input.put("studyUri", studyUri);
            input.put("processUri", processUri);
            input.put("toolUri", toolUri);
            responseBody.set("input", input);

            responseBody.set("outputs", Json.newArray());
            responseBody.set("summary", buildSummary(payload.path("associations")));

            ArrayNode logs = Json.newArray();
            logs.add("Execution finished successfully");
            if (!executionResult.stdout.isEmpty()) {
                logs.add("stdout: " + summarizeOutput(executionResult.stdout));
            }
            if (!executionResult.stderr.isEmpty()) {
                logs.add("stderr: " + summarizeOutput(executionResult.stderr));
            }
            responseBody.set("logs", logs);

            ObjectNode success = Json.newObject();
            success.put("isSuccessful", true);
            success.set("body", responseBody);

            LOGGER.info("RAnalysis execute completed runId={} studyUri={} processUri={} toolUri={} durationMs={} status=completed",
                    runId, studyUri, processUri, toolUri, durationMs);

            return ok(success);

        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startedMs;
            LOGGER.error("RAnalysis execute unhandled failure runId={} durationMs={} message={}", runId, durationMs, e.getMessage(), e);

            ObjectNode details = Json.newObject();
            details.put("runId", runId);
            details.put("exception", e.getClass().getSimpleName());
            details.put("message", e.getMessage() == null ? "Unexpected server error" : e.getMessage());

            return internalServerError(errorResponse("r_execution_failed", "Unexpected runtime failure", details));
        }
    }

    protected Result validateAuthIfRequired(Http.Request request, String runId) {
        if (!isAuthRequired()) {
            return null;
        }

        String authHeader = request.getHeaders().get("Authorization").orElse("").trim();
        if (authHeader.isEmpty()) {
            LOGGER.warn("RAnalysis unauthorized runId={} reason=missing_authorization", runId);
            return unauthorized(errorResponse("unauthorized", "Missing bearer token", (JsonNode) null));
        }

        if (!authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            LOGGER.warn("RAnalysis forbidden runId={} reason=invalid_authorization_scheme", runId);
            return forbidden(errorResponse("forbidden", "Invalid authorization scheme", (JsonNode) null));
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            LOGGER.warn("RAnalysis unauthorized runId={} reason=empty_bearer_token", runId);
            return unauthorized(errorResponse("unauthorized", "Empty bearer token", (JsonNode) null));
        }

        if (!validateJwtToken(token)) {
            LOGGER.warn("RAnalysis forbidden runId={} reason=invalid_or_expired_token", runId);
            return forbidden(errorResponse("forbidden", "Invalid or expired bearer token", (JsonNode) null));
        }

        return null;
    }

    protected boolean isAuthRequired() {
        Config config = ConfigFactory.load();
        return config.hasPath("hascoapi.r_analysis.require_auth") &&
                config.getBoolean("hascoapi.r_analysis.require_auth");
    }

    protected boolean validateJwtToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }

            String signedData = parts[0] + "." + parts[1];
            byte[] headerBytes = Base64.getUrlDecoder().decode(parts[0]);
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            byte[] providedSignature = Base64.getUrlDecoder().decode(parts[2]);

            JsonNode header = Json.parse(new String(headerBytes, StandardCharsets.UTF_8));
            JsonNode payload = Json.parse(new String(payloadBytes, StandardCharsets.UTF_8));

            String alg = header.path("alg").asText("").trim();
            String jcaAlgorithm = jwtToJcaAlgorithm(alg);
            if (jcaAlgorithm == null) {
                return false;
            }

            String secret = ConfigProp.getJWTSecret();
            if (secret == null || secret.trim().isEmpty()) {
                return false;
            }

            Mac mac = Mac.getInstance(jcaAlgorithm);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), jcaAlgorithm));
            byte[] expectedSignature = mac.doFinal(signedData.getBytes(StandardCharsets.US_ASCII));

            if (!MessageDigest.isEqual(expectedSignature, providedSignature)) {
                return false;
            }

            long now = Instant.now().getEpochSecond();
            JsonNode expNode = payload.path("exp");
            if (!expNode.isMissingNode() && expNode.canConvertToLong()) {
                if (now >= expNode.asLong()) {
                    return false;
                }
            }

            JsonNode nbfNode = payload.path("nbf");
            if (!nbfNode.isMissingNode() && nbfNode.canConvertToLong()) {
                if (now < nbfNode.asLong()) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String jwtToJcaAlgorithm(String jwtAlgorithm) {
        if (jwtAlgorithm == null) {
            return null;
        }

        switch (jwtAlgorithm.toUpperCase()) {
            case "HS256":
                return "HmacSHA256";
            case "HS384":
                return "HmacSHA384";
            case "HS512":
                return "HmacSHA512";
            default:
                return null;
        }
    }

    private static void cacheEngineRun(String jobId, ObjectNode record) {
        if (jobId == null || jobId.trim().isEmpty() || record == null) {
            return;
        }

        if (ENGINE_RUN_CACHE.size() >= ENGINE_RUN_CACHE_MAX) {
            String firstKey = ENGINE_RUN_CACHE.keySet().stream().findFirst().orElse(null);
            if (firstKey != null) {
                ENGINE_RUN_CACHE.remove(firstKey);
            }
        }

        ENGINE_RUN_CACHE.put(jobId, record);
    }

    private static boolean artifactExistsInRecord(ObjectNode record, String artifactPath) {
        JsonNode artifacts = record.path("engineResponse").path("artifacts");
        if (!artifacts.isArray()) {
            return false;
        }
        for (JsonNode artifact : artifacts) {
            if (artifact != null && artifact.isTextual() && artifact.asText("").trim().equals(artifactPath)) {
                return true;
            }
        }
        return false;
    }

    private static String sanitizeArtifactPath(String artifactPath) {
        if (artifactPath == null) {
            return "";
        }
        String value = artifactPath.trim();
        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        if (value.isEmpty() || value.contains("..") || value.contains("\\0")) {
            return "";
        }
        return value;
    }

    private static Path resolveEngineArtifactRoot() {
        String envRoot = System.getenv("R_ENGINE_ARTIFACT_ROOT");
        if (envRoot != null && !envRoot.trim().isEmpty()) {
            return Paths.get(envRoot.trim());
        }

        Config config = ConfigFactory.load();
        if (config.hasPath("hascoapi.r_engine.artifact_root")) {
            String confRoot = config.getString("hascoapi.r_engine.artifact_root");
            if (confRoot != null && !confRoot.trim().isEmpty()) {
                return Paths.get(confRoot.trim());
            }
        }
        return null;
    }

    private static ArrayNode buildArtifactList(String jobId, JsonNode artifactsNode) {
        ArrayNode artifacts = Json.newArray();
        if (artifactsNode != null && artifactsNode.isArray()) {
            for (JsonNode artifact : artifactsNode) {
                if (artifact != null && artifact.isTextual()) {
                    String artifactPath = artifact.asText("").trim();
                    if (artifactPath.isEmpty()) {
                        continue;
                    }
                    ObjectNode node = Json.newObject();
                    node.put("path", artifactPath);
                    node.put("downloadUrl", "/hascoapi/api/r-analysis/engine/jobs/" + jobId + "/artifacts/download/" + artifactPath);
                    artifacts.add(node);
                }
            }
        }
        return artifacts;
    }

    private static int resolveEngineTimeoutSeconds(int fallback) {
        int timeout = fallback;
        String envTimeout = System.getenv("R_ENGINE_TIMEOUT_SECONDS");
        if (envTimeout != null && !envTimeout.trim().isEmpty()) {
            try {
                timeout = Integer.parseInt(envTimeout.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        Config config = ConfigFactory.load();
        if (config.hasPath("hascoapi.r_engine.timeout_seconds")) {
            timeout = config.getInt("hascoapi.r_engine.timeout_seconds");
        }

        if (timeout < 1) {
            return 60;
        }
        if (timeout > 3600) {
            return 3600;
        }
        return timeout;
    }

    private static String resolveEngineBaseUrl() {
        String env = System.getenv("R_ENGINE_URL");
        if (env != null && !env.trim().isEmpty()) {
            return trimTrailingSlash(env.trim());
        }

        Config config = ConfigFactory.load();
        if (config.hasPath("hascoapi.r_engine.base_url")) {
            String conf = config.getString("hascoapi.r_engine.base_url");
            if (conf != null && !conf.trim().isEmpty()) {
                return trimTrailingSlash(conf.trim());
            }
        }

        return "http://localhost:3031";
    }

    private static String trimTrailingSlash(String value) {
        String v = value;
        while (v.endsWith("/")) {
            v = v.substring(0, v.length() - 1);
        }
        return v;
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private static JsonNode parseJsonOrText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Json.newObject();
        }
        try {
            return Json.parse(value);
        } catch (Exception ignored) {
            ObjectNode node = Json.newObject();
            node.put("raw", value);
            return node;
        }
    }

    private static HttpResult callREngine(String method, String path, JsonNode body, int timeoutSeconds) throws IOException, InterruptedException {
        String endpoint = resolveEngineBaseUrl() + (path.startsWith("/") ? path : "/" + path);
        HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds));

        if ("GET".equalsIgnoreCase(method)) {
            builder.GET();
        } else {
            String content = body == null ? "{}" : body.toString();
            builder.header("Content-Type", "application/json");
            builder.POST(HttpRequest.BodyPublishers.ofString(content, StandardCharsets.UTF_8));
        }

        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new HttpResult(response.statusCode(), response.body() == null ? "" : response.body());
    }

    protected static final class HttpResult {
        private final int statusCode;
        private final String body;

        private HttpResult(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        private boolean is2xx() {
            return statusCode >= 200 && statusCode < 300;
        }
    }

    private static ObjectNode errorDetail(String field, String message) {
        ObjectNode node = Json.newObject();
        node.put("field", field);
        node.put("message", message);
        return node;
    }

    private static ObjectNode errorResponse(String code, String message, JsonNode details) {
        ObjectNode root = Json.newObject();
        root.put("isSuccessful", false);

        ObjectNode error = Json.newObject();
        error.put("code", code);
        error.put("message", message);
        if (details != null && !details.isNull()) {
            error.set("details", details);
        }

        root.set("error", error);
        return root;
    }

    private static String text(JsonNode node, String field) {
        JsonNode valueNode = node.path(field);
        if (valueNode.isMissingNode() || valueNode.isNull()) {
            return "";
        }
        return valueNode.asText("").trim();
    }

    private static String summarizeOutput(String output) {
        if (output == null) {
            return "";
        }
        String normalized = output.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 400) {
            return normalized;
        }
        return normalized.substring(0, 400) + "...";
    }

    private static String generateRunId() {
        return "RA-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static int resolveTimeoutSeconds(JsonNode payload) {
        int timeout = 60;

        JsonNode fromPayload = payload.path("arguments").path("timeoutSeconds");
        if (fromPayload != null && fromPayload.isInt()) {
            timeout = fromPayload.asInt(timeout);
        }

        String envTimeout = System.getenv("R_ANALYSIS_TIMEOUT_SECONDS");
        if (envTimeout != null && !envTimeout.trim().isEmpty()) {
            try {
                timeout = Integer.parseInt(envTimeout.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        Config config = ConfigFactory.load();
        if (config.hasPath("hascoapi.r_analysis.timeout_seconds")) {
            timeout = config.getInt("hascoapi.r_analysis.timeout_seconds");
        }

        if (timeout < 1) {
            timeout = 60;
        }
        if (timeout > 3600) {
            timeout = 3600;
        }

        return timeout;
    }

    private static ObjectNode buildSummary(JsonNode associations) {
        ObjectNode summary = Json.newObject();

        JsonNode counts = associations.path("counts");
        int datasets = intValue(counts, "datasets", associations.path("datasets").isArray() ? associations.path("datasets").size() : 0);
        int variables = intValue(counts, "variables", associations.path("variables").isArray() ? associations.path("variables").size() : 0);
        int images = intValue(counts, "images", associations.path("images").isArray() ? associations.path("images").size() : 0);

        summary.put("datasets", datasets);
        summary.put("variables", variables);
        summary.put("images", images);
        summary.put("totalAssociations", datasets + variables + images);

        return summary;
    }

    private static int intValue(JsonNode node, String field, int defaultValue) {
        JsonNode valueNode = node.path(field);
        if (valueNode.isInt()) {
            return valueNode.asInt();
        }
        if (valueNode.isTextual()) {
            try {
                return Integer.parseInt(valueNode.asText().trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    protected ExecutionResult executeRAnalysis(JsonNode payload, Http.Request request, int timeoutSeconds, String runId)
            throws IOException, InterruptedException, ExecutionException, TimeoutException {

        JsonNode toolNode = payload.path("tool");
        Path scriptPath = resolveScriptPath(toolNode, request, runId);

        List<String> command = new ArrayList<>();
        String rscriptBin = System.getenv("R_SCRIPT_BIN");
        if (rscriptBin == null || rscriptBin.trim().isEmpty()) {
            rscriptBin = "Rscript";
        }
        command.add(rscriptBin);
        command.add(scriptPath.toAbsolutePath().toString());

        JsonNode argsNode = payload.path("arguments").path("rscriptArgs");
        if (argsNode.isArray()) {
            for (JsonNode arg : argsNode) {
                if (arg.isTextual() && !arg.asText().trim().isEmpty()) {
                    command.add(arg.asText().trim());
                }
            }
        }

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        if (scriptPath.getParent() != null && Files.exists(scriptPath.getParent())) {
            processBuilder.directory(scriptPath.getParent().toFile());
        }

        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            ExecutionResult failed = new ExecutionResult();
            failed.success = false;
            failed.exitCode = -1;
            failed.stderr = "Failed to start Rscript process: " + e.getMessage();
            failed.engineName = rscriptBin;
            failed.engineVersion = "unknown";
            return failed;
        }

        ExecutorService streamExecutor = Executors.newFixedThreadPool(2);
        CompletableFuture<String> stdoutFuture = CompletableFuture.supplyAsync(() -> readStream(process.getInputStream()), streamExecutor);
        CompletableFuture<String> stderrFuture = CompletableFuture.supplyAsync(() -> readStream(process.getErrorStream()), streamExecutor);

        ExecutionResult result = new ExecutionResult();
        result.engineName = rscriptBin;
        result.engineVersion = "unknown";

        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            result.timedOut = true;
            result.success = false;
            result.exitCode = -1;
            result.stdout = getFutureValue(stdoutFuture, 2);
            result.stderr = getFutureValue(stderrFuture, 2);
            streamExecutor.shutdownNow();
            return result;
        }

        result.exitCode = process.exitValue();
        result.stdout = getFutureValue(stdoutFuture, 2);
        result.stderr = getFutureValue(stderrFuture, 2);
        result.success = result.exitCode == 0;

        streamExecutor.shutdownNow();

        LOGGER.info("RAnalysis process finished runId={} exitCode={} timeoutSeconds={}", runId, result.exitCode, timeoutSeconds);

        return result;
    }

    private static String getFutureValue(CompletableFuture<String> future, int timeoutSeconds) {
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            return "";
        }
    }

    private static Path resolveScriptPath(JsonNode toolNode, Http.Request request, String runId) throws IOException {
        String inlineCode = firstNonEmpty(
                text(toolNode, "inlineCode"),
                text(toolNode, "sourceCode")
        );
        if (!inlineCode.isEmpty()) {
            String inlineName = firstNonEmpty(text(toolNode, "sourceFilename"), text(toolNode, "artifactFilename"));
            return materializeInlineScript(inlineCode, inlineName, runId);
        }

        String entrypoint = text(toolNode, "entrypoint");
        String artifactUri = text(toolNode, "artifactUri");
        String artifactFilename = text(toolNode, "artifactFilename");

        if (!entrypoint.isEmpty()) {
            try {
                Path entryPath = Paths.get(entrypoint);
                if (Files.exists(entryPath)) {
                    return entryPath;
                }
            } catch (Exception ignored) {
            }
        }

        if (!artifactUri.isEmpty()) {
            if (artifactUri.startsWith("http://") || artifactUri.startsWith("https://")) {
                return downloadArtifact(artifactUri, artifactFilename, request.getHeaders().get("Authorization").orElse(""));
            }

            if (artifactUri.startsWith("file://")) {
                Path fileUriPath = Paths.get(URI.create(artifactUri));
                if (Files.exists(fileUriPath)) {
                    return fileUriPath;
                }
            }

            try {
                Path artifactPath = Paths.get(artifactUri);
                if (Files.exists(artifactPath)) {
                    if (!entrypoint.isEmpty() && Files.isDirectory(artifactPath)) {
                        Path nested = artifactPath.resolve(entrypoint);
                        if (Files.exists(nested)) {
                            return nested;
                        }
                    }
                    return artifactPath;
                }
            } catch (Exception ignored) {
            }
        }

        throw new IOException("Could not resolve R script from tool.entrypoint/artifactUri");
    }

    private static Path materializeInlineScript(String inlineCode, String sourceFilename, String runId) throws IOException {
        String safeRunId = (runId == null || runId.trim().isEmpty()) ? generateRunId() : runId.trim();
        String baseName = sourceFilename == null ? "script.R" : sourceFilename.trim();
        if (baseName.isEmpty()) {
            baseName = "script.R";
        }

        baseName = Paths.get(baseName).getFileName().toString();
        if (!baseName.toLowerCase().endsWith(".r")) {
            baseName = baseName + ".R";
        }

        Path sourceRoot = resolveEngineArtifactRoot();
        if (sourceRoot == null) {
            sourceRoot = Paths.get(System.getProperty("java.io.tmpdir"), "hascoapi-r-analysis");
        }

        Path runRoot = sourceRoot.resolve("source-code").resolve(safeRunId).normalize();
        Files.createDirectories(runRoot);

        Path scriptPath = runRoot.resolve(baseName).normalize();
        if (!scriptPath.startsWith(runRoot)) {
            throw new IOException("Invalid inline source filename");
        }

        Files.writeString(scriptPath, inlineCode, StandardCharsets.UTF_8);
        return scriptPath;
    }

    private static Path downloadArtifact(String artifactUri, String artifactFilename, String authorizationHeader) throws IOException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(artifactUri))
                .GET()
                .timeout(java.time.Duration.ofSeconds(30));

        if (authorizationHeader != null && !authorizationHeader.trim().isEmpty()) {
            builder.header("Authorization", authorizationHeader.trim());
        }

        HttpRequest request = builder.build();

        HttpResponse<InputStream> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Artifact download interrupted", e);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Artifact download failed with status " + response.statusCode());
        }

        String suffix = ".R";
        if (artifactFilename != null && artifactFilename.toLowerCase().endsWith(".r")) {
            suffix = artifactFilename.substring(artifactFilename.length() - 2);
        }
        Path tempFile = Files.createTempFile("r-analysis-", suffix);

        try (InputStream inputStream = response.body()) {
            Files.copy(inputStream, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        return tempFile;
    }

    private static String readStream(InputStream stream) {
        try (InputStream inputStream = stream) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    protected static ExecutionResult successExecution(String engineName, String engineVersion, String stdout, String stderr) {
        ExecutionResult result = new ExecutionResult();
        result.success = true;
        result.exitCode = 0;
        result.engineName = engineName == null ? "Rscript" : engineName;
        result.engineVersion = engineVersion == null ? "unknown" : engineVersion;
        result.stdout = stdout == null ? "" : stdout;
        result.stderr = stderr == null ? "" : stderr;
        return result;
    }

    protected static ExecutionResult failureExecution(int exitCode, String engineName, String engineVersion, String stdout, String stderr) {
        ExecutionResult result = new ExecutionResult();
        result.success = false;
        result.exitCode = exitCode;
        result.engineName = engineName == null ? "Rscript" : engineName;
        result.engineVersion = engineVersion == null ? "unknown" : engineVersion;
        result.stdout = stdout == null ? "" : stdout;
        result.stderr = stderr == null ? "" : stderr;
        return result;
    }

    protected static ExecutionResult timeoutExecution(String engineName, String engineVersion, String stdout, String stderr) {
        ExecutionResult result = new ExecutionResult();
        result.success = false;
        result.timedOut = true;
        result.exitCode = -1;
        result.engineName = engineName == null ? "Rscript" : engineName;
        result.engineVersion = engineVersion == null ? "unknown" : engineVersion;
        result.stdout = stdout == null ? "" : stdout;
        result.stderr = stderr == null ? "" : stderr;
        return result;
    }

    protected static final class ExecutionResult {
        private boolean success;
        private boolean timedOut;
        private int exitCode;
        private String stdout = "";
        private String stderr = "";
        private String engineName;
        private String engineVersion;
    }
}
