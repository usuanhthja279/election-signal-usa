-- Original provider response bodies, including compressed files, after explicit archive approval.
CREATE TABLE raw_source_archives (
    archive_id bigserial PRIMARY KEY,
    provider_id text NOT NULL REFERENCES source_providers(provider_id),
    source_url text NOT NULL CHECK (source_url ~ '^https://[^/ ]+'),
    media_type text NOT NULL,
    source_as_of timestamptz,
    fetched_at timestamptz NOT NULL DEFAULT now(),
    content_sha256 char(64) NOT NULL CHECK (content_sha256 ~ '^[0-9a-f]{64}$'),
    response_status integer NOT NULL CHECK (response_status = 200),
    raw_bytes bytea NOT NULL CHECK (octet_length(raw_bytes) BETWEEN 1 AND 8388608),
    is_test_data boolean NOT NULL DEFAULT false CHECK (is_test_data = false),
    UNIQUE (provider_id,source_url,content_sha256)
);
CREATE TRIGGER raw_source_archives_append_only BEFORE UPDATE OR DELETE ON raw_source_archives
    FOR EACH ROW EXECUTE FUNCTION reject_phase2_mutation();
