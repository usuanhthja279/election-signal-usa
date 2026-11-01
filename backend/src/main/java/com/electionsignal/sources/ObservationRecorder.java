package com.electionsignal.sources;

import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Persists a licensed provider's original bytes before appending candidate revisions. */
@Service
@Profile("local")
public class ObservationRecorder {
    private static final int MAX_PAYLOAD_BYTES = 10 * 1024 * 1024;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    public ObservationRecorder(DataSource database, PlatformTransactionManager transactionManager) {
        this.jdbc = new JdbcTemplate(database);
        this.transaction = new TransactionTemplate(transactionManager);
    }

    public record CandidateTotal(String candidateId, String geoId, Long votes,
                                 String reportingKind, Double reportingValue, String reportingUnit) { }
    public record Observation(String providerId, String sourceRaceId, String raceId, Instant sourceAsOf,
                              String contentType, byte[] rawBytes, String rawJson, int responseStatus,
                              String resultStatus, boolean testData, List<CandidateTotal> candidates) { }
    public record Recorded(long observationId, String sha256, boolean duplicate) { }

    public Recorded record(Observation observation) {
        validate(observation);
        return transaction.execute(status -> {
            requireAuthorizedSource(observation.providerId());
            String sha = ObservationFingerprint.sha256(observation.rawBytes());
            String insert = "INSERT INTO source_observations(provider_id,source_race_id,race_id,source_as_of,fetched_at,content_sha256,is_test_data,raw_payload,raw_payload_bytes,content_type,source_revision,provider_response_status,result_status) "
                    + "VALUES (?,?,?,?,?,?,false,?::jsonb,?,?,?,?,?) ON CONFLICT(provider_id,source_race_id,content_sha256) DO NOTHING RETURNING observation_id";
            PreparedStatementCreator creator = connection -> {
                PreparedStatement ps = connection.prepareStatement(insert);
                ps.setString(1,observation.providerId()); ps.setString(2,observation.sourceRaceId()); ps.setString(3,observation.raceId());
                if (observation.sourceAsOf()==null) ps.setNull(4, Types.TIMESTAMP_WITH_TIMEZONE); else ps.setObject(4,observation.sourceAsOf());
                ps.setObject(5,Instant.now()); ps.setString(6,sha); ps.setString(7,observation.rawJson()); ps.setBytes(8,observation.rawBytes());
                ps.setString(9,observation.contentType()); ps.setString(10,sha); ps.setInt(11,observation.responseStatus()); ps.setString(12,observation.resultStatus());
                return ps;
            };
            Long insertedId = jdbc.query(creator, rs -> rs.next() ? rs.getLong(1) : null);
            boolean duplicate = insertedId == null;
            long observationId = duplicate ? jdbc.queryForObject("SELECT observation_id FROM source_observations WHERE provider_id=? AND source_race_id=? AND content_sha256=?",Long.class,observation.providerId(),observation.sourceRaceId(),sha) : insertedId;
            if (!duplicate && observation.raceId()!=null) appendCandidateRevisions(observationId,observation);
            return new Recorded(observationId,sha,duplicate);
        });
    }

    private void requireAuthorizedSource(String providerId) {
        Boolean allowed=jdbc.query("SELECT enabled AND use_rights_verified AND license_id IS NOT NULL FROM source_providers WHERE provider_id=?",
                rs -> rs.next() && rs.getBoolean(1),providerId);
        if (!Boolean.TRUE.equals(allowed)) throw new SecurityException("Provider is missing an enabled, verified reuse license");
    }

    private void appendCandidateRevisions(long observationId,Observation observation) {
        String sql="INSERT INTO candidate_result_revisions(observation_id,race_id,candidate_id,geo_id,total_votes,reporting_kind,reporting_value,source_as_of,reporting_unit,revision_fingerprint) VALUES(?,?,?,?,?,?,?,?,?,?) ON CONFLICT DO NOTHING";
        for (CandidateTotal candidate:observation.candidates()) {
            String fingerprint=ObservationFingerprint.sha256((observation.raceId()+"|"+candidate.candidateId()+"|"+candidate.geoId()+"|"+candidate.votes()+"|"+candidate.reportingKind()+"|"+candidate.reportingValue()).getBytes(StandardCharsets.UTF_8));
            jdbc.update(sql,observationId,observation.raceId(),candidate.candidateId(),candidate.geoId(),candidate.votes(),candidate.reportingKind(),candidate.reportingValue(),observation.sourceAsOf(),candidate.reportingUnit(),fingerprint);
        }
    }

    private void validate(Observation input) {
        if (input==null || input.testData()) throw new IllegalArgumentException("Fictional/replay observations may not enter the local production recorder");
        if (input.providerId()==null || !input.providerId().matches("[A-Za-z0-9_-]{1,80}")) throw new IllegalArgumentException("Invalid provider ID");
        if (input.sourceRaceId()==null || input.sourceRaceId().isBlank() || input.sourceRaceId().length()>200) throw new IllegalArgumentException("Invalid source race ID");
        if (input.rawBytes()==null || input.rawBytes().length==0 || input.rawBytes().length>MAX_PAYLOAD_BYTES) throw new IllegalArgumentException("Raw response must be between 1 byte and 10 MiB");
        if (input.rawJson()==null || input.rawJson().isBlank()) throw new IllegalArgumentException("JSON source payload is required");
        if (input.contentType()==null || !input.contentType().toLowerCase().startsWith("application/json")) throw new IllegalArgumentException("Only JSON provider payloads are supported");
        if (input.responseStatus()<200 || input.responseStatus()>299) throw new IllegalArgumentException("Only successful provider response bodies may be recorded as observations");
        if (!List.of("NOT_ON_BALLOT","NOT_REPORTING","COUNTING","SOURCE_STALE","SOURCE_UNAVAILABLE","PROJECTION_BY_PROVIDER","OFFICIALLY_CERTIFIED").contains(input.resultStatus())) throw new IllegalArgumentException("Invalid provider result status");
        for (CandidateTotal candidate:input.candidates()) {
            if (candidate.candidateId()==null || candidate.candidateId().isBlank()) throw new IllegalArgumentException("Candidate identity is required");
            if (candidate.votes()!=null && candidate.votes()<0) throw new IllegalArgumentException("Vote totals cannot be negative");
            if (candidate.reportingValue()!=null && (candidate.reportingValue()<0 || candidate.reportingValue()>100)) throw new IllegalArgumentException("Reporting value must be 0–100");
            if (candidate.reportingKind()!=null && !List.of("ESTIMATED_VOTE_PERCENT","PRECINCTS_REPORTING_PERCENT","BALLOTS_COUNTED","TURNOUT_PERCENT").contains(candidate.reportingKind())) throw new IllegalArgumentException("Unknown reporting measure");
        }
    }
}
