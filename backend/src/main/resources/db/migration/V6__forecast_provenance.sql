-- Preserve date-only source precision; do not manufacture publication instants.
ALTER TABLE published_forecast_snapshots ALTER COLUMN published_at DROP NOT NULL;
ALTER TABLE published_forecast_snapshots ADD COLUMN source_date date;
ALTER TABLE published_forecast_snapshots ADD COLUMN candidate_name text;
ALTER TABLE published_forecast_snapshots ADD COLUMN source_party text;
ALTER TABLE published_forecast_snapshots ADD COLUMN source_key text;
ALTER TABLE published_forecast_snapshots ADD CONSTRAINT forecast_source_time CHECK (published_at IS NOT NULL OR source_date IS NOT NULL);
CREATE TABLE polling_forecast_archives (
  sha256 char(64) PRIMARY KEY CHECK (sha256 ~ '^[0-9a-f]{64}$'),
  provider_id text NOT NULL REFERENCES polling_providers,
  source_url text NOT NULL CHECK (source_url ~ '^https://[^/ ]+'),
  retrieved_at timestamptz NOT NULL,
  payload bytea NOT NULL CHECK (octet_length(payload) <= 5242880)
);
ALTER TABLE published_forecast_snapshots ADD COLUMN archive_sha256 char(64) REFERENCES polling_forecast_archives(sha256);
