CREATE TABLE poll_raw_archives (
  sha256 char(64) PRIMARY KEY CHECK (sha256 ~ '^[0-9a-f]{64}$'),
  source_id text NOT NULL REFERENCES polling_sources,
  retrieved_at timestamptz NOT NULL,
  payload bytea NOT NULL CHECK (octet_length(payload) <= 10485760)
);
