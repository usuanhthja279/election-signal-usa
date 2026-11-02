-- Phase 3 polling and published forecasts. No provider or observation is seeded.
CREATE TABLE polling_providers (
  provider_id text PRIMARY KEY CHECK (provider_id ~ '^[A-Za-z0-9_-]{1,80}$'),
  display_name text NOT NULL CHECK (length(trim(display_name))>0), website_url text NOT NULL CHECK (website_url ~ '^https://[^/ ]+'),
  enabled boolean NOT NULL DEFAULT false, connection_status text NOT NULL DEFAULT 'NOT_CONNECTED',
  last_attempt timestamptz, last_success timestamptz, last_publication timestamptz,
  consecutive_errors integer NOT NULL DEFAULT 0 CHECK (consecutive_errors >= 0)
);
CREATE TABLE polling_sources (
  source_id text PRIMARY KEY, provider_id text NOT NULL REFERENCES polling_providers,
  publication_url text NOT NULL CHECK (publication_url ~ '^https://[^/ ]+'), methodology_url text CHECK (methodology_url IS NULL OR methodology_url ~ '^https://[^/ ]+'), license_id text NOT NULL CHECK (length(trim(license_id))>0),
  attribution text NOT NULL CHECK (length(trim(attribution))>0), rights_evidence_url text NOT NULL CHECK (rights_evidence_url ~ '^https://[^/ ]+'),
  review_status text NOT NULL DEFAULT 'UNREVIEWED' CHECK (review_status IN ('UNREVIEWED','APPROVED','REJECTED')),
  redistribution_allowed boolean NOT NULL DEFAULT false, reviewed_at timestamptz,
  CHECK (review_status <> 'APPROVED' OR reviewed_at IS NOT NULL)
);
CREATE TABLE pollsters (pollster_id text PRIMARY KEY, display_name text NOT NULL);
CREATE TABLE poll_surveys (
  survey_id text PRIMARY KEY, pollster_id text NOT NULL REFERENCES pollsters,
  sponsor text, election_year integer NOT NULL CHECK (election_year BETWEEN 2000 AND 2100),
  office text NOT NULL CHECK (office IN ('HOUSE','SENATE','GOVERNOR','GENERIC')),
  state_code char(2) REFERENCES jurisdictions, district_code char(2), race_id text,
  field_start date, field_end date, sample_size integer CHECK (sample_size > 0),
  population text, survey_mode text, margin_of_error numeric CHECK (margin_of_error BETWEEN 0 AND 100),
  question_identity text NOT NULL, matchup_identity text NOT NULL,
  is_test_data boolean NOT NULL DEFAULT false CHECK (is_test_data = false),
  CHECK (field_start IS NULL OR field_end IS NULL OR field_start <= field_end),
  CHECK (district_code IS NULL OR (office = 'HOUSE' AND state_code IS NOT NULL AND district_code ~ '^[0-9]{2}$')),
  CHECK (office <> 'GENERIC' OR district_code IS NULL)
);
CREATE TABLE poll_publications (
  publication_id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  survey_id text NOT NULL REFERENCES poll_surveys, source_id text NOT NULL REFERENCES polling_sources,
  provider_poll_id text NOT NULL, provider_revision text NOT NULL, published_at timestamptz,
  fetched_at timestamptz NOT NULL, raw_sha256 char(64) NOT NULL CHECK (raw_sha256 ~ '^[0-9a-f]{64}$'),
  UNIQUE (source_id,provider_poll_id,provider_revision)
);
CREATE TABLE poll_answers (
  publication_id bigint NOT NULL REFERENCES poll_publications, choice_id text NOT NULL,
  choice_label text NOT NULL, candidate_id text, response_percent numeric CHECK (response_percent BETWEEN 0 AND 100),
  PRIMARY KEY(publication_id,choice_id)
);
CREATE TABLE poll_reconciliation_reviews (
  review_id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  left_publication bigint NOT NULL REFERENCES poll_publications, right_publication bigint NOT NULL REFERENCES poll_publications,
  decision text NOT NULL CHECK (decision IN ('EXACT_MATCH','PROBABLE_DUPLICATE','DIFFERENT_QUESTIONNAIRE','INDEPENDENT_POLL','REVIEW_REQUIRED')),
  reviewed_at timestamptz, notes text, CHECK (left_publication <> right_publication), UNIQUE(left_publication,right_publication)
);
CREATE TABLE poll_average_series (
  series_id text PRIMARY KEY, source_id text NOT NULL REFERENCES polling_sources, election_year integer NOT NULL,
  office text NOT NULL CHECK (office IN ('HOUSE','SENATE','GOVERNOR','GENERIC')), state_code char(2) REFERENCES jurisdictions,
  district_code char(2), race_id text, methodology_url text,
  CHECK (district_code IS NULL OR (office='HOUSE' AND state_code IS NOT NULL AND district_code ~ '^[0-9]{2}$'))
);
CREATE TABLE poll_average_points (
  series_id text NOT NULL REFERENCES poll_average_series, published_at timestamptz NOT NULL,
  candidate_id text NOT NULL, share numeric NOT NULL CHECK (share BETWEEN 0 AND 100), included_poll_count integer CHECK (included_poll_count >= 0),
  fetched_at timestamptz NOT NULL, PRIMARY KEY(series_id,published_at,candidate_id)
);
CREATE TABLE published_forecast_models (model_id text PRIMARY KEY, source_id text NOT NULL REFERENCES polling_sources, model_name text NOT NULL, model_version text, methodology_url text);
CREATE TABLE published_forecast_snapshots (
  forecast_id text PRIMARY KEY, model_id text NOT NULL REFERENCES published_forecast_models,
  election_year integer NOT NULL, office text NOT NULL CHECK (office IN ('HOUSE','SENATE','GOVERNOR')),
  state_code char(2) REFERENCES jurisdictions, district_code char(2), race_id text NOT NULL,
  candidate_id text, published_at timestamptz NOT NULL, fetched_at timestamptz NOT NULL,
  expected_share numeric CHECK (expected_share BETWEEN 0 AND 100), win_probability numeric CHECK (win_probability BETWEEN 0 AND 1),
  interval_low numeric CHECK (interval_low BETWEEN 0 AND 100), interval_high numeric CHECK (interval_high BETWEEN 0 AND 100), rating text,
  is_test_data boolean NOT NULL DEFAULT false CHECK (is_test_data = false),
  CHECK (district_code IS NULL OR (office='HOUSE' AND state_code IS NOT NULL AND district_code ~ '^[0-9]{2}$')),
  CHECK (interval_low IS NULL OR interval_high IS NULL OR interval_low <= interval_high)
);
CREATE TABLE polling_ingestion_runs (
  run_id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY, provider_id text NOT NULL REFERENCES polling_providers,
  started_at timestamptz NOT NULL, finished_at timestamptz, result_status text NOT NULL, cursor_value text, error_code text
);
CREATE INDEX poll_scope_idx ON poll_surveys(election_year,office,state_code,district_code,race_id);
CREATE INDEX poll_publication_survey_idx ON poll_publications(survey_id,published_at DESC);
CREATE INDEX forecast_scope_idx ON published_forecast_snapshots(election_year,office,state_code,district_code,race_id,published_at DESC);
