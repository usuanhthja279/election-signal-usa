package com.electionsignal;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("demo")
class DemoApiIntegrationTests {
    @LocalServerPort int port;
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).build(), HttpResponse.BodyHandlers.ofString());
    }
    @SuppressWarnings("unchecked")
    private Map<String,Object> json(String path) throws Exception { return mapper.readValue(get(path).body(), Map.class); }
    @Test void fixtureAndStatusContracts() throws Exception {
        var races = json("/api/v1/demo/races?state=TX&office=house");
        assertEquals("DEMO", races.get("dataMode")); assertEquals(true, races.get("isTestData")); assertEquals(1, races.get("total"));
        assertEquals(0, json("/api/v1/demo/races?state=TX&office=house&district=08").get("total"));
        assertEquals(400, get("/api/v1/polls?state=TX&office=senate&district=07").statusCode());
        assertEquals("NOT_IMPORTED", json("/api/v1/history/results?year=2000").get("status"));
        assertEquals("NOT_CONNECTED", json("/api/v1/polls").get("status"));
        assertEquals("NOT_CONNECTED", json("/api/v1/forecasts/published").get("status"));
        assertEquals("NOT_PUBLISHED", json("/api/v1/exit-polls").get("status"));
        assertEquals(2000, json("/api/v1/exit-polls?year=2000").get("year"));
        assertEquals(400, get("/api/v1/exit-polls?year=2001").statusCode());
        assertNull(json("/api/v1/models/races/TX-HOUSE-07-2026-DEMO").get("probabilities"));
        assertEquals(400, get("/api/v1/context?mode=invalid").statusCode());
        assertEquals("INVALID_PARAMETER", json("/api/v1/context?mode=invalid").get("error"));
        assertEquals(400, get("/api/v1/history/results?year=2019").statusCode());
        assertEquals(404, get("/api/v1/demo/races/unknown").statusCode());
        assertEquals("NOT_FOUND", json("/api/v1/demo/races/unknown").get("error"));
        var counties = (Map<?,?>) json("/api/v1/geography/states/TX/counties").get("data");
        assertEquals(254, counties.get("countyCount"));
        assertTrue(get("/api/v1/events").body().contains("\"resultsStream\":false"));
    }
}
