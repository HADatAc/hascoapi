package org.hascoapi.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hascoapi.analysis.RAnalysisPayloadValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RAnalysisPayloadValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void validateAcceptsValidPayload() throws Exception {
        String json = "{" +
                "\"studyUri\":\"https://example.org/STD1\"," +
                "\"processUri\":\"https://example.org/PROC1\"," +
                "\"tool\":{" +
                "\"toolUri\":\"https://example.org/tool/R1\"," +
                "\"name\":\"R Tool\"," +
                "\"version\":\"1.0\"," +
                "\"language\":\"R\"," +
                "\"artifactUri\":\"https://example.org/script.R\"," +
                "\"artifactFilename\":\"script.R\"," +
                "\"sourceRepositoryUri\":\"https://example.org/repo\"," +
                "\"entrypoint\":\"script.R\"}," +
                "\"associations\":{" +
                "\"datasets\":[],\"variables\":[],\"images\":[],\"counts\":{\"datasets\":0,\"variables\":0,\"images\":0}} ," +
                "\"arguments\":{}," +
                "\"requestedAt\":\"2026-06-01T10:00:00Z\"," +
                "\"requestedBy\":{\"uid\":\"1\",\"identifier\":\"tester@example.org\"}" +
                "}";

        JsonNode payload = MAPPER.readTree(json);
        RAnalysisPayloadValidator.ValidationResult result = RAnalysisPayloadValidator.validate(payload);

        Assertions.assertTrue(result.isValid());
        Assertions.assertEquals(0, result.getErrors().size());
    }

    @Test
    public void validateRejectsMissingRequiredFields() throws Exception {
        String json = "{" +
                "\"studyUri\":\"\"," +
                "\"tool\":{}," +
                "\"associations\":{}," +
                "\"arguments\":{}," +
                "\"requestedBy\":{}" +
                "}";

        JsonNode payload = MAPPER.readTree(json);
        RAnalysisPayloadValidator.ValidationResult result = RAnalysisPayloadValidator.validate(payload);

        Assertions.assertFalse(result.isValid());
        Assertions.assertTrue(result.getErrors().size() >= 4);
    }

    @Test
    public void validateRejectsNonRLanguage() throws Exception {
        String json = "{" +
                "\"studyUri\":\"https://example.org/STD1\"," +
                "\"processUri\":\"https://example.org/PROC1\"," +
                "\"tool\":{" +
                "\"toolUri\":\"https://example.org/tool/PY1\"," +
                "\"language\":\"Python\"," +
                "\"entrypoint\":\"script.py\"}," +
                "\"associations\":{}," +
                "\"arguments\":{}," +
                "\"requestedAt\":\"2026-06-01T10:00:00Z\"," +
                "\"requestedBy\":{\"identifier\":\"tester@example.org\"}" +
                "}";

        JsonNode payload = MAPPER.readTree(json);
        RAnalysisPayloadValidator.ValidationResult result = RAnalysisPayloadValidator.validate(payload);

        Assertions.assertFalse(result.isValid());
        Assertions.assertTrue(result.getErrors().stream().anyMatch(e -> "tool.language".equals(e.getField())));
    }

    @Test
    public void validateAcceptsLowercaseRLanguage() throws Exception {
        String json = "{" +
                "\"studyUri\":\"https://example.org/STD1\"," +
                "\"processUri\":\"https://example.org/PROC1\"," +
                "\"tool\":{" +
                "\"toolUri\":\"https://example.org/tool/R1\"," +
                "\"language\":\"r\"," +
                "\"entrypoint\":\"script.R\"}," +
                "\"associations\":{}," +
                "\"arguments\":{}," +
                "\"requestedAt\":\"2026-06-01T10:00:00Z\"," +
                "\"requestedBy\":{\"identifier\":\"tester@example.org\"}" +
                "}";

        JsonNode payload = MAPPER.readTree(json);
        RAnalysisPayloadValidator.ValidationResult result = RAnalysisPayloadValidator.validate(payload);

        Assertions.assertTrue(result.isValid());
    }
}
