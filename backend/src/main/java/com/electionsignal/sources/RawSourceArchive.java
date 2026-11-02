package com.electionsignal.sources;

import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Archives unchanged source bytes only for explicitly approved, enabled providers. */
@Service
@Profile("local")
public class RawSourceArchive {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    public RawSourceArchive(DataSource db, PlatformTransactionManager manager) {
        jdbc = new JdbcTemplate(db); transaction = new TransactionTemplate(manager);
    }
    public void requireAuthorized(String providerId, String sourceUrl) {
        Boolean allowed = jdbc.query("SELECT p.enabled AND p.use_rights_verified AND p.license_id IS NOT NULL AND u.archive_allowed AND p.source_url=? FROM source_providers p JOIN source_usage_permissions u ON u.provider_id=p.provider_id WHERE p.provider_id=?", rs -> rs.next() && rs.getBoolean(1), sourceUrl, providerId);
        if (!Boolean.TRUE.equals(allowed)) throw new SecurityException("Source URL or archive permission is not approved");
    }
    public record Input(String providerId, String sourceUrl, String mediaType, Instant sourceAsOf,
                        int responseStatus, byte[] bytes, boolean testData) {}
    public record Recorded(long id, String sha256, boolean duplicate) {}
    public Recorded record(Input input) {
        if (input == null || input.testData()) throw new IllegalArgumentException("Test data cannot be archived in local production storage");
        if (input.providerId() == null || !input.providerId().matches("[A-Za-z0-9_-]{1,80}")) throw new IllegalArgumentException("Invalid provider ID");
        if (input.sourceUrl() == null || input.sourceUrl().length() > 2048) throw new IllegalArgumentException("Invalid source URL");
        URI uri;
        try { uri = URI.create(input.sourceUrl()); } catch (IllegalArgumentException e) { throw new IllegalArgumentException("Invalid source URL", e); }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null || uri.getFragment() != null) throw new IllegalArgumentException("Source URL must be HTTPS");
        if (!Set.of("application/zip", "application/json", "text/csv", "text/tab-separated-values").contains(input.mediaType())) throw new IllegalArgumentException("Unsupported media type");
        if (input.responseStatus() != 200 || input.bytes() == null || input.bytes().length == 0 || input.bytes().length > 8 * 1024 * 1024) throw new IllegalArgumentException("Invalid source response");
        return transaction.execute(status -> {
            requireAuthorized(input.providerId(), input.sourceUrl());
            String sha = ObservationFingerprint.sha256(input.bytes());
            Long id = jdbc.query("INSERT INTO raw_source_archives(provider_id,source_url,media_type,source_as_of,content_sha256,response_status,raw_bytes) VALUES (?,?,?,?,?,200,?) ON CONFLICT(provider_id,source_url,content_sha256) DO NOTHING RETURNING archive_id", rs -> rs.next() ? rs.getLong(1) : null, input.providerId(), input.sourceUrl(), input.mediaType(), input.sourceAsOf() == null ? null : OffsetDateTime.ofInstant(input.sourceAsOf(), ZoneOffset.UTC), sha, input.bytes());
            boolean duplicate = id == null;
            if (duplicate) id = jdbc.queryForObject("SELECT archive_id FROM raw_source_archives WHERE provider_id=? AND source_url=? AND content_sha256=?", Long.class, input.providerId(), input.sourceUrl(), sha);
            return new Recorded(id, sha, duplicate);
        });
    }
}
