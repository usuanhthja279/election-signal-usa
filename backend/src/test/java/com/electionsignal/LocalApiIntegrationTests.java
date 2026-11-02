package com.electionsignal;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import java.net.http.*;
import java.sql.Connection;
import java.sql.SQLException;
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
        try(Connection connection=database.getConnection(); var query=connection.prepareStatement("SELECT count(*) FROM flyway_schema_history WHERE success = true AND version IN ('1','2','3','4')");var rows=query.executeQuery()) { assertTrue(rows.next()); assertEquals(4,rows.getInt(1)); }
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
        assertEquals("UNAVAILABLE_NOT_TRAINED",json("/api/v1/models/races/TX-HOUSE-07-2026-DEMO").get("status"));
        assertEquals("BLOCKED",((Map<?,?>)json("/api/v1/ops/readiness").get("data")).get("overallStatus"));
        assertEquals("HISTORICAL_CERTIFIED",json("/api/v1/history/coverage").get("dataMode"));
        assertEquals("AVAILABLE",json("/api/v1/sources/inventory").get("status"));
        var registry=json("/api/v1/elections/2026/registry");
        assertEquals("US-2026-GENERAL",((Map<?,?>)registry.get("data")).get("electionId"));
        assertEquals("NOT_IMPORTED",((Map<?,?>)registry.get("data")).get("registryStatus"));
        var states=json("/api/v1/elections/2026/states");
        assertEquals(51,((java.util.List<?>)states.get("data")).size());
        assertEquals("NOT_IMPORTED",json("/api/v1/elections/2026/house/districts?state=TX").get("status"));
        assertEquals("NOT_IMPORTED",json("/api/v1/elections/2026/races?state=TX&office=HOUSE").get("status"));
        assertEquals(400,get("/api/v1/elections/2026/states/ZZ/races").statusCode());
        assertEquals(400,get("/api/v1/elections/2026/races?limit=501").statusCode());
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

    @Test void pollingReadsRequireApprovedRightsAndHonorScopeAndCutoff() throws Exception {
        // Synthetic records exist only in the explicitly disposable test database and are removed afterwards.
        try(var c=database.getConnection();var q=c.createStatement()) {
            q.executeUpdate("INSERT INTO polling_providers(provider_id,display_name,website_url) VALUES ('test-p3','Synthetic provider','https://example.invalid')");
            q.executeUpdate("INSERT INTO polling_sources(source_id,provider_id,publication_url,license_id,attribution,rights_evidence_url,review_status,redistribution_allowed,reviewed_at) VALUES ('test-approved','test-p3','https://example.invalid/poll','TEST','Synthetic fixture','https://example.invalid/rights','APPROVED',true,now()),('test-denied','test-p3','https://example.invalid/private','TEST','Synthetic fixture','https://example.invalid/rights','UNREVIEWED',false,null)");
            q.executeUpdate("INSERT INTO pollsters(pollster_id,display_name) VALUES ('test-pollster','Synthetic pollster')");
            q.executeUpdate("INSERT INTO poll_surveys(survey_id,pollster_id,election_year,office,state_code,field_start,field_end,question_identity,matchup_identity) VALUES ('test-survey','test-pollster',2026,'SENATE','TX','2026-10-01','2026-10-03','q1','a-b'),('test-private','test-pollster',2026,'SENATE','TX','2026-10-01','2026-10-03','q2','a-b')");
            q.executeUpdate("INSERT INTO poll_publications(survey_id,source_id,provider_poll_id,provider_revision,published_at,fetched_at,raw_sha256) VALUES ('test-survey','test-approved','one','v1','2026-10-05T12:00:00Z',now(),repeat('a',64)),('test-private','test-denied','two','v1','2026-10-05T12:00:00Z',now(),repeat('b',64))");
            q.executeUpdate("INSERT INTO poll_answers(publication_id,choice_id,choice_label,candidate_id,response_percent) SELECT publication_id,'a','Synthetic candidate','test-candidate',0 FROM poll_publications WHERE survey_id='test-survey'");
            var result=json("/api/v1/polls?year=2026&state=TX&office=senate&provider=test-p3");
            assertEquals("AVAILABLE",result.get("status"));var data=(java.util.List<?>)result.get("data");assertEquals(1,data.size());var record=(Map<?,?>)data.get(0);assertEquals("test-survey",record.get("pollId"));assertNull(record.get("sampleSize"));assertEquals(0,((Number)((Map<?,?>)((java.util.List<?>)record.get("answers")).get(0)).get("percent")).intValue());
            assertEquals(0,((java.util.List<?>)json("/api/v1/polls?year=2026&state=TX&office=senate&asOf=2026-10-04T00:00:00Z").get("data")).size());
            assertEquals(0,((java.util.List<?>)json("/api/v1/polls?year=2026&state=GA&office=senate").get("data")).size());
            q.executeUpdate("INSERT INTO poll_average_series(series_id,source_id,election_year,office,state_code,race_id) VALUES ('test-average','test-approved',2026,'SENATE','TX','test-race')");
            q.executeUpdate("INSERT INTO poll_average_points(series_id,published_at,candidate_id,share,included_poll_count,fetched_at) VALUES ('test-average','2026-10-11T12:00:00Z','test-candidate',48.5,null,now()),('test-average','2026-10-25T12:00:00Z','test-candidate',50.1,5,now())");
            var averages=json("/api/v1/polls/averages?year=2026&state=TX&office=senate&asOf=2026-10-20T00:00:00Z");assertEquals("POLL",averages.get("dataMode"));var points=(java.util.List<?>)averages.get("data");assertEquals(1,points.size());assertNull(((Map<?,?>)points.get(0)).get("includedPollCount"));
            q.executeUpdate("INSERT INTO published_forecast_models(model_id,source_id,model_name) VALUES ('test-model','test-approved','Synthetic model'),('test-private-model','test-denied','Private model')");
            q.executeUpdate("INSERT INTO published_forecast_snapshots(forecast_id,model_id,election_year,office,state_code,race_id,candidate_id,published_at,fetched_at,win_probability) VALUES ('test-old','test-model',2026,'SENATE','TX','test-race','test-candidate','2026-10-10T12:00:00Z',now(),0.65),('test-new','test-model',2026,'SENATE','TX','test-race','test-candidate','2026-10-25T12:00:00Z',now(),0.70),('test-secret','test-private-model',2026,'SENATE','TX','test-race','test-candidate','2026-10-10T12:00:00Z',now(),0.80)");
            var forecasts=json("/api/v1/forecasts/published?year=2026&state=TX&office=senate&asOf=2026-10-20T00:00:00Z");assertEquals("PUBLISHED_FORECAST",forecasts.get("dataMode"));var snapshots=(java.util.List<?>)forecasts.get("data");assertEquals(1,snapshots.size());assertEquals("test-old",((Map<?,?>)snapshots.get(0)).get("forecastId"));
            assertEquals(400,get("/api/v1/polls?pageSize=101").statusCode());
            assertThrows(SQLException.class,()->q.executeUpdate("INSERT INTO poll_surveys(survey_id,pollster_id,election_year,office,question_identity,matchup_identity,is_test_data) VALUES ('bad','test-pollster',2026,'SENATE','q','m',true)"));
        } finally {
            try(var c=database.getConnection();var q=c.createStatement()) {q.executeUpdate("DELETE FROM poll_average_points WHERE series_id='test-average'");q.executeUpdate("DELETE FROM poll_average_series WHERE series_id='test-average'");q.executeUpdate("DELETE FROM published_forecast_snapshots WHERE forecast_id IN ('test-old','test-new','test-secret')");q.executeUpdate("DELETE FROM published_forecast_models WHERE model_id IN ('test-model','test-private-model')");q.executeUpdate("DELETE FROM poll_answers WHERE publication_id IN (SELECT publication_id FROM poll_publications WHERE source_id IN ('test-approved','test-denied'))");q.executeUpdate("DELETE FROM poll_publications WHERE source_id IN ('test-approved','test-denied')");q.executeUpdate("DELETE FROM poll_surveys WHERE survey_id IN ('test-survey','test-private')");q.executeUpdate("DELETE FROM pollsters WHERE pollster_id='test-pollster'");q.executeUpdate("DELETE FROM polling_sources WHERE source_id IN ('test-approved','test-denied')");q.executeUpdate("DELETE FROM polling_providers WHERE provider_id='test-p3'");}
        }
    }
}
