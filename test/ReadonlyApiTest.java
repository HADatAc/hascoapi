package test;

import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import org.hascoapi.vocabularies.HASCO;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ReadonlyApiTest extends BaseApiTest {

    @Test
    public void testApiVersion() {
        given()
            .when()
            .get("/hascoapi/version")
            .then()
            .statusCode(200);
    }

    @Test
    public void testGetTotalDataFiles() {
        given()
            .when()
            .get("/hascoapi/api/datafile/elements/total")
            .then()
            .statusCode(200)
            .body("isSuccessful", equalTo(true));
    }

    @Test
    public void testUriGenerationFailsWithoutBaseUrl() {
        given()
            .when()
            .get("/hascoapi/api/urigen/study")
            .then()
            .statusCode(200)
            .body("isSuccessful", equalTo(false))
            .body("body", equalTo("Repository's base URL needs to be setup before URIs can be generated."));
    }

    @Test
    public void testGetRepositoryMetadata() {
        given()
            .when()
            .get("/hascoapi/api/repo")
            .then()
            .statusCode(200)
            .body("isSuccessful", equalTo(true));
    }

    @Test
    public void testGetChildren() {
        given()
            .when()
            .get("/hascoapi/api/children/" + URLEncoder.encode(HASCO.STUDY, StandardCharsets.UTF_8))
            .then()
            .statusCode(200)
            .body("isSuccessful", equalTo(true));
    }
}
