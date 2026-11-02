-- Rights must be explicit for each use; an enabled source alone is not permission to archive or republish.
CREATE TABLE source_usage_permissions (
  provider_id text PRIMARY KEY REFERENCES source_providers(provider_id),
  archive_allowed boolean NOT NULL DEFAULT false,
  display_allowed boolean NOT NULL DEFAULT false,
  api_redistribution_allowed boolean NOT NULL DEFAULT false,
  terms_url text NOT NULL CHECK (terms_url ~ '^https://[^/ ]+'),
  reviewed_at timestamptz NOT NULL,
  evidence_note text NOT NULL CHECK (length(trim(evidence_note))>0)
);
