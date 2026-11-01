package com.electionsignal;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import java.net.http.*;
import java.sql.Connection;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.ObjectMapper;

/** Runs only against an explicitly supplied disposable PostgreSQL database. */
@EnabledIfEnvironmentVariable(named="ELECTION_DB_TEST_URL",matches=".+")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class LocalApiIntegrationTests {
    @DynamicPropertySource static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url",()->System.getenv("ELECTION_DB_TEST_URL"));
        properties.add("spring.datasource.username",()->System.getenv("ELECTION_DB_TEST_USER"));
        properties.add("spring.datasource.password",()->System.getenv("ELECTION_DB_TEST_PASSWORD"));
    }
    @LocalServerPort int port;
    @Autowired DataSource database;
    private final HttpClient client=HttpClient.newHttpClient();
    private final ObjectMapper mapper=new ObjectMapper();
    private HttpResponse<String> get(String path) throws Exception { return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).build(),HttpResponse.BodyHandlers.ofString()); }
    @SuppressWarnings("unchecked") private Map<String,Object> json(String path) throws Exception { return mapper.readValue(get(path).body(),Map.class); }
    @Test void migrationAndProfileIsolation() throws Exception {
        try(Connection connection=database.getConnection(); var query=connection.prepareStatement("SELECT count(*) FROM flyway_schema_history WHERE success = true AND version IN ('1','2')");var rows=query.executeQuery()) { assertTrue(rows.next()); assertEquals(2,rows.getInt(1)); }
        var health=json("/api/v1/health"); assertEquals("LIVE_REPORTED",health.get("dataMode"));assertEquals(false,health.get("isTestData"));
        assertEquals("NOT_CONNECTED",json("/api/v1/live/races?state=TX&office=house").get("status"));
        assertEquals("NOT_IMPORTED",json("/api/v1/history/results?year=2000").get("status"));
        assertEquals("NOT_CONNECTED",json("/api/v1/polls").get("status"));
        assertEquals(400,get("/api/v1/polls?state=TX&office=senate&district=07").statusCode());
        assertEquals("NOT_CONNECTED",json("/api/v1/forecasts/published").get("status"));
        assertEquals("NOT_PUBLISHED",json("/api/v1/exit-polls").get("status"));
        assertEquals(2000,json("/api/v1/exit-polls?year=2000").get("year"));
        assertEquals(400,get("/api/v1/exit-polls?year=2001").statusCode());
        assertNull(json("/api/v1/models/races/TX-HOUSE-07-2026-DEMO").get("probabilities"));
        assertEquals(404,get("/api/v1/demo/races").statusCode());
        assertEquals(400,get("/api/v1/context?mode=invalid").statusCode());
        assertEquals("REFERENCE_ONLY",json("/api/v1/geography/states/TX/counties").get("status"));
        assertEquals("REFERENCE_GEOGRAPHY",json("/api/v1/geography/states/TX/counties").get("dataMode"));
    }
    @Test void databaseRejectsDemoSourceObservation() throws Exception {
        try(Connection connection=database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try(var insert=connection.prepareStatement("INSERT INTO source_providers(provider_id,display_name,category) VALUES ('test-provider','Test provider','test')")) { insert.executeUpdate(); }
                try(var insert=connection.prepareStatement("INSERT INTO source_observations(provider_id,source_race_id,fetched_at,content_sha256,is_test_data,raw_payload) VALUES ('test-provider','test-race',now(),repeat('0',64),true,'{}'::jsonb)")) {
                    var error=assertThrows(java.sql.SQLException.class,insert::executeUpdate);
                    assertEquals("23514",error.getSQLState());
                }
            } finally { connection.rollback(); }
        }
    }
}
