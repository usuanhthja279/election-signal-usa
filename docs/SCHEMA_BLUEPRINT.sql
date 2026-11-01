-- FUTURE POSTGRESQL/FLYWAY SCHEMA BLUEPRINT (not applied in Phase 0).
-- Codex Phase 1+ must review and split into versioned migrations.
-- Never import DEMO fixture data into a production result table.
CREATE TABLE source_providers (
  provider_id text PRIMARY KEY,
  display_name text NOT NULL,
  source_url text,
  category text NOT NULL,
  license_id text,
  use_rights_verified boolean NOT NULL DEFAULT false,
  enabled boolean NOT NULL DEFAULT false,
  coverage_json jsonb NOT NULL DEFAULT '{}'::jsonb,
  last_success_at timestamptz,
  last_error_at timestamptz
);
CREATE TABLE elections (
  election_id text PRIMARY KEY,
  country text NOT NULL DEFAULT 'US',
  cycle_year integer NOT NULL,
  date_local date NOT NULL,
  election_kind text NOT NULL,
  local_timezone text NOT NULL DEFAULT 'America/New_York'
);
CREATE TABLE geographical_units (
  geo_id text PRIMARY KEY,
  geo_type text NOT NULL CHECK (geo_type IN ('STATE','COUNTY','PRECINCT','DISTRICT')),
  state_fips char(2),
  county_fips char(5),
  official_name text NOT NULL
);
CREATE TABLE boundary_versions (
  boundary_version_id text PRIMARY KEY,
  effective_election_year integer,
  geometry_source_url text NOT NULL,
  source_revision text,
  verified_for_use boolean NOT NULL DEFAULT false
);
CREATE TABLE geo_shapes (
  geo_id text NOT NULL REFERENCES geographical_units(geo_id),
  boundary_version_id text NOT NULL REFERENCES boundary_versions(boundary_version_id),
  shape_data jsonb NOT NULL,
  PRIMARY KEY (geo_id, boundary_version_id)
);
CREATE TABLE races (
  race_id text PRIMARY KEY,
  election_id text NOT NULL REFERENCES elections(election_id),
  office text NOT NULL CHECK (office IN ('HOUSE','SENATE','GOVERNOR','PRESIDENT','OTHER')),
  state_code char(2) NOT NULL,
  district_code text,
  seat_class text,
  election_subtype text NOT NULL DEFAULT 'REGULAR',
  race_status text NOT NULL DEFAULT 'NOT_REPORTING',
  UNIQUE (election_id, office, state_code, district_code, seat_class, election_subtype)
);
CREATE TABLE candidates (
  candidate_id text PRIMARY KEY,
  canonical_name text NOT NULL,
  party_code text,
  external_ids jsonb NOT NULL DEFAULT '{}'::jsonb
);
CREATE TABLE race_candidates (
  race_id text NOT NULL REFERENCES races(race_id),
  candidate_id text NOT NULL REFERENCES candidates(candidate_id),
  ballot_order integer,
  PRIMARY KEY (race_id,candidate_id)
);
CREATE TABLE source_observations (
  observation_id bigserial PRIMARY KEY,
  provider_id text NOT NULL REFERENCES source_providers(provider_id),
  source_race_id text NOT NULL,
  race_id text REFERENCES races(race_id),
  source_as_of timestamptz,
  fetched_at timestamptz NOT NULL,
  content_sha256 char(64) NOT NULL,
  is_test_data boolean NOT NULL,
  raw_payload jsonb NOT NULL,
  UNIQUE (provider_id, source_race_id, content_sha256)
);
CREATE TABLE candidate_result_revisions (
  revision_id bigserial PRIMARY KEY,
  observation_id bigint NOT NULL REFERENCES source_observations(observation_id),
  race_id text NOT NULL REFERENCES races(race_id),
  candidate_id text NOT NULL REFERENCES candidates(candidate_id),
  geo_id text REFERENCES geographical_units(geo_id),
  total_votes bigint,
  reporting_kind text,
  reporting_value numeric(7,3),
  source_as_of timestamptz,
  ingested_at timestamptz NOT NULL DEFAULT now(),
  CHECK (total_votes IS NULL OR total_votes >= 0),
  CHECK (reporting_value IS NULL OR (reporting_value >= 0 AND reporting_value <= 100))
);
CREATE TABLE race_calls (
  call_id bigserial PRIMARY KEY,
  race_id text NOT NULL REFERENCES races(race_id),
  provider_id text NOT NULL REFERENCES source_providers(provider_id),
  winning_candidate_id text REFERENCES candidates(candidate_id),
  called_at timestamptz NOT NULL,
  withdrawn_at timestamptz,
  reason text,
  source_observation_id bigint REFERENCES source_observations(observation_id)
);
CREATE TABLE historical_results (
  historical_result_id bigserial PRIMARY KEY,
  election_id text NOT NULL REFERENCES elections(election_id),
  race_id text NOT NULL REFERENCES races(race_id),
  candidate_id text NOT NULL REFERENCES candidates(candidate_id),
  geo_id text REFERENCES geographical_units(geo_id),
  boundary_version_id text REFERENCES boundary_versions(boundary_version_id),
  votes bigint,
  import_source_url text NOT NULL,
  import_sha256 char(64) NOT NULL,
  certified boolean NOT NULL DEFAULT false,
  CHECK (votes IS NULL OR votes >= 0)
);
CREATE TABLE opinion_polls (
  poll_id text PRIMARY KEY,
  provider_id text NOT NULL REFERENCES source_providers(provider_id),
  original_url text NOT NULL,
  pollster text NOT NULL,
  sponsor text,
  field_start date,
  field_end date,
  published_at timestamptz,
  sample_n integer,
  population text,
  office text,
  state_code char(2),
  district_code text,
  question_variant text,
  results jsonb NOT NULL,
  underlying_survey_key text NOT NULL,
  license_id text
);
CREATE TABLE published_forecasts (
  provider_forecast_id text PRIMARY KEY,
  provider_id text NOT NULL REFERENCES source_providers(provider_id),
  race_id text REFERENCES races(race_id),
  published_at timestamptz NOT NULL,
  model_name text NOT NULL,
  model_version text,
  forecast_json jsonb NOT NULL,
  original_url text NOT NULL
);
CREATE TABLE voter_surveys (
  survey_id text PRIMARY KEY,
  provider_id text NOT NULL REFERENCES source_providers(provider_id),
  release_at timestamptz,
  embargo_until timestamptz,
  population text,
  methods jsonb NOT NULL DEFAULT '{}'::jsonb,
  crosstabs jsonb NOT NULL,
  license_id text
);
CREATE TABLE model_runs (
  model_run_id text PRIMARY KEY,
  race_id text NOT NULL REFERENCES races(race_id),
  model_name text NOT NULL,
  model_version text NOT NULL,
  run_at timestamptz NOT NULL,
  inputs_as_of timestamptz,
  status text NOT NULL CHECK(status IN ('VALIDATED','ABSTAINED','RESEARCH_ONLY')),
  inputs_json jsonb NOT NULL,
  outputs_json jsonb,
  calibration_ref text,
  abstention_reason text,
  is_test_data boolean NOT NULL DEFAULT false
);
CREATE INDEX idx_result_race_time ON candidate_result_revisions(race_id,source_as_of DESC);
CREATE INDEX idx_obs_provider_fetched ON source_observations(provider_id,fetched_at DESC);
CREATE INDEX idx_hist_election_geo ON historical_results(election_id,geo_id);
CREATE INDEX idx_poll_survey_key ON opinion_polls(underlying_survey_key);
