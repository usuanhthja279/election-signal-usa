-- Registry only. Demo fixtures must not be inserted into this database.
CREATE TABLE source_providers (
    provider_id text PRIMARY KEY,
    display_name text NOT NULL,
    source_url text,
    category text NOT NULL,
    license_id text,
    use_rights_verified boolean NOT NULL DEFAULT false,
    enabled boolean NOT NULL DEFAULT false,
    coverage_json jsonb NOT NULL DEFAULT '{}'::jsonb
);

CREATE TABLE elections (
    election_id text PRIMARY KEY,
    country text NOT NULL DEFAULT 'US',
    cycle_year integer NOT NULL,
    date_local date NOT NULL,
    election_kind text NOT NULL
);

CREATE TABLE offices (
    office_code text PRIMARY KEY,
    display_name text NOT NULL
);

CREATE TABLE geographical_units (
    geo_id text PRIMARY KEY,
    geo_type text NOT NULL CHECK (geo_type IN ('STATE','COUNTY','PRECINCT','DISTRICT')),
    state_fips char(2),
    county_fips char(5),
    official_name text NOT NULL
);

CREATE TABLE races (
    race_id text PRIMARY KEY,
    election_id text NOT NULL REFERENCES elections(election_id),
    office_code text NOT NULL REFERENCES offices(office_code),
    state_code char(2) NOT NULL,
    district_code text,
    seat_class text,
    election_subtype text NOT NULL DEFAULT 'REGULAR',
    race_status text NOT NULL DEFAULT 'NOT_REPORTING'
);

CREATE UNIQUE INDEX races_identity ON races
    (election_id, office_code, state_code, COALESCE(district_code, ''),
     COALESCE(seat_class, ''), election_subtype);

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
    PRIMARY KEY (race_id, candidate_id)
);
