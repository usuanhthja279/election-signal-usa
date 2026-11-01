-- Production observations remain separate from filesystem demo fixtures.
CREATE TABLE source_observations (
    observation_id bigserial PRIMARY KEY,
    provider_id text NOT NULL REFERENCES source_providers(provider_id),
    source_race_id text NOT NULL,
    race_id text REFERENCES races(race_id),
    source_as_of timestamptz,
    fetched_at timestamptz NOT NULL,
    content_sha256 char(64) NOT NULL,
    is_test_data boolean NOT NULL DEFAULT false CHECK (is_test_data = false),
    raw_payload jsonb NOT NULL,
    UNIQUE (provider_id, source_race_id, content_sha256)
);

CREATE TABLE candidate_result_revisions (
    revision_id bigserial PRIMARY KEY,
    observation_id bigint NOT NULL REFERENCES source_observations(observation_id),
    race_id text NOT NULL,
    candidate_id text NOT NULL,
    geo_id text REFERENCES geographical_units(geo_id),
    total_votes bigint CHECK (total_votes IS NULL OR total_votes >= 0),
    reporting_kind text,
    reporting_value numeric(7,3) CHECK (reporting_value IS NULL OR reporting_value BETWEEN 0 AND 100),
    source_as_of timestamptz,
    ingested_at timestamptz NOT NULL DEFAULT now(),
    FOREIGN KEY (race_id, candidate_id) REFERENCES race_candidates(race_id, candidate_id)
);
