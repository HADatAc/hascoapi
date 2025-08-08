package config;

import org.junit.jupiter.api.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FusekiConnectionTest {

    private HttpClient client;

    @BeforeAll
    public void setup() {
        client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    @Test
    public void testHascoapiVersionEndpoint() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI("http://localhost:9000/hascoapi/version"))
            .GET()
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "API /hascoapi/version should return 200 OK");

        String body = response.body();
        System.out.println("Response body:\n" + body);

        // Verifica se a resposta contém a versão esperada ou partes do HTML
        assertTrue(body.contains("0.8") || body.toLowerCase().contains("version"),
            "Response should contain version information");
    }

    @Test
    public void testHascoapiRepoQueryTest() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI("http://localhost:9000/hascoapi/api/repo/queryTest"))
            .GET()
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "API /hascoapi/api/repo/queryTest should return 200 OK");

        String body = response.body();
        System.out.println("Response body:\n" + body);

        assertTrue(body.contains("http") || body.contains("result"),
            "Response should contain some triple or query result");
    }
}
