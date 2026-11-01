-- Phase 2 registry, source provenance and historical result foundations.
-- No results are seeded. A missing registry/import stays explicitly unavailable.

CREATE TABLE jurisdictions (
    state_code char(2) PRIMARY KEY,
    state_fips char(2) NOT NULL UNIQUE,
    display_name text NOT NULL,
    jurisdiction_type text NOT NULL CHECK (jurisdiction_type IN ('STATE','DISTRICT_OF_COLUMBIA')),
    source_url text NOT NULL,
    source_status text NOT NULL DEFAULT 'VERIFIED_CODE' CHECK (source_status IN ('VERIFIED_CODE','UNVERIFIED'))
);

INSERT INTO jurisdictions(state_code,state_fips,display_name,jurisdiction_type,source_url) VALUES
('AL','01','Alabama','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('AK','02','Alaska','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('AZ','04','Arizona','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('AR','05','Arkansas','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('CA','06','California','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('CO','08','Colorado','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('CT','09','Connecticut','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('DE','10','Delaware','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('DC','11','District of Columbia','DISTRICT_OF_COLUMBIA','https://www.census.gov/library/reference/code-lists/ansi.html'),
('FL','12','Florida','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('GA','13','Georgia','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('HI','15','Hawaii','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('ID','16','Idaho','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('IL','17','Illinois','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('IN','18','Indiana','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('IA','19','Iowa','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('KS','20','Kansas','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('KY','21','Kentucky','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('LA','22','Louisiana','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('ME','23','Maine','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('MD','24','Maryland','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('MA','25','Massachusetts','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('MI','26','Michigan','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('MN','27','Minnesota','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('MS','28','Mississippi','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('MO','29','Missouri','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('MT','30','Montana','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('NE','31','Nebraska','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('NV','32','Nevada','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('NH','33','New Hampshire','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('NJ','34','New Jersey','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('NM','35','New Mexico','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('NY','36','New York','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('NC','37','North Carolina','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('ND','38','North Dakota','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('OH','39','Ohio','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('OK','40','Oklahoma','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('OR','41','Oregon','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('PA','42','Pennsylvania','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('RI','44','Rhode Island','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('SC','45','South Carolina','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('SD','46','South Dakota','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('TN','47','Tennessee','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('TX','48','Texas','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('UT','49','Utah','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('VT','50','Vermont','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('VA','51','Virginia','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('WA','53','Washington','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('WV','54','West Virginia','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('WI','55','Wisconsin','STATE','https://www.census.gov/library/reference/code-lists/ansi.html'),
('WY','56','Wyoming','STATE','https://www.census.gov/library/reference/code-lists/ansi.html');

INSERT INTO offices(office_code,display_name) VALUES
('HOUSE','U.S. House'),('SENATE','U.S. Senate'),('GOVERNOR','Governor')
ON CONFLICT (office_code) DO NOTHING;

INSERT INTO elections(election_id,cycle_year,date_local,election_kind)
VALUES ('US-2026-GENERAL',2026,'2026-11-03','GENERAL') ON CONFLICT (election_id) DO NOTHING;

ALTER TABLE races ADD COLUMN ballot_status text NOT NULL DEFAULT 'UNKNOWN'
    CHECK (ballot_status IN ('ON_BALLOT','NOT_ON_BALLOT','UNKNOWN'));
ALTER TABLE races ADD CONSTRAINT races_subtype_allowed
    CHECK (election_subtype IN ('REGULAR','SPECIAL','RUNOFF','PRIMARY','OTHER'));
ALTER TABLE races ADD CONSTRAINT house_race_requires_district
    CHECK (office_code <> 'HOUSE' OR district_code IS NOT NULL);
ALTER TABLE races ADD CONSTRAINT senate_race_requires_seat_identity
    CHECK (office_code <> 'SENATE' OR seat_class IS NOT NULL OR district_code IS NOT NULL);

CREATE TABLE geography_versions (
    geography_version_id text PRIMARY KEY,
    authority text NOT NULL,
    source_url text NOT NULL,
    effective_from date NOT NULL,
    effective_to date,
    retrieved_at timestamptz,
    checksum_sha256 char(64),
    legal_review_status text NOT NULL DEFAULT 'UNVERIFIED'
        CHECK (legal_review_status IN ('UNVERIFIED','APPROVED','REJECTED')),
    notes text NOT NULL DEFAULT '',
    CHECK (effective_to IS NULL OR effective_to >= effective_from),
    CHECK (checksum_sha256 IS NULL OR checksum_sha256 ~ '^[0-9a-f]{64}$')
);

INSERT INTO geography_versions(geography_version_id,authority,source_url,effective_from,legal_review_status,notes)
VALUES ('CENSUS_BAS_2026_CD119','U.S. Census Bureau TIGERweb','https://tigerweb.geo.census.gov/arcgis/rest/services/TIGERweb/Legislative/MapServer/4',
        '2026-01-01','UNVERIFIED','Official 119th Congress boundaries relevant to 2026 elections; geometries and checksum have not yet been imported locally.')
ON CONFLICT (geography_version_id) DO NOTHING;

CREATE TABLE congressional_districts (
    state_code char(2) NOT NULL REFERENCES jurisdictions(state_code),
    district_code char(2) NOT NULL CHECK (district_code ~ '^[0-9]{2}$'),
    geography_version_id text NOT NULL REFERENCES geography_versions(geography_version_id),
    geo_id text NOT NULL UNIQUE,
    display_name text NOT NULL,
    verification_status text NOT NULL CHECK (verification_status IN ('VERIFIED','UNVERIFIED')),
    source_feature_id text NOT NULL,
    PRIMARY KEY (state_code,district_code,geography_version_id)
);

CREATE TABLE senate_seats (
    seat_id text PRIMARY KEY,
    state_code char(2) NOT NULL REFERENCES jurisdictions(state_code),
    seat_class text NOT NULL CHECK (seat_class IN ('CLASS_I','CLASS_II','CLASS_III','SPECIAL')),
    term_start date,
    term_end date,
    source_status text NOT NULL DEFAULT 'UNVERIFIED' CHECK (source_status IN ('VERIFIED','UNVERIFIED')),
    UNIQUE (state_code,seat_class,seat_id)
);

CREATE TABLE source_publications (
    publication_id bigserial PRIMARY KEY,
    provider_id text NOT NULL REFERENCES source_providers(provider_id),
    title text NOT NULL,
    source_url text NOT NULL,
    citation text NOT NULL,
    license_id text,
    legal_review_status text NOT NULL DEFAULT 'UNVERIFIED'
        CHECK (legal_review_status IN ('UNVERIFIED','APPROVED','REJECTED')),
    redistribution_allowed boolean NOT NULL DEFAULT false,
    source_as_of timestamptz,
    retrieved_at timestamptz NOT NULL DEFAULT now(),
    sha256 char(64) NOT NULL CHECK (sha256 ~ '^[0-9a-f]{64}$'),
    content_type text NOT NULL DEFAULT 'application/octet-stream',
    raw_payload_bytes bytea NOT NULL,
    raw_payload jsonb,
    UNIQUE (provider_id,sha256),
    CHECK (legal_review_status <> 'APPROVED' OR (license_id IS NOT NULL AND redistribution_allowed))
);

CREATE TABLE external_race_mappings (
    provider_id text NOT NULL REFERENCES source_providers(provider_id),
    source_election_id text NOT NULL,
    source_race_id text NOT NULL,
    race_id text NOT NULL REFERENCES races(race_id),
    first_seen_at timestamptz NOT NULL DEFAULT now(),
    mapping_status text NOT NULL CHECK (mapping_status IN ('VERIFIED','REVIEW_REQUIRED','REJECTED')),
    PRIMARY KEY (provider_id,source_election_id,source_race_id),
    UNIQUE (provider_id,race_id,source_race_id)
);

CREATE TABLE historical_contests (
    contest_id text PRIMARY KEY,
    election_year integer NOT NULL CHECK (election_year BETWEEN 2000 AND 2024 AND election_year % 2 = 0),
    office_code text NOT NULL REFERENCES offices(office_code),
    state_code char(2) NOT NULL REFERENCES jurisdictions(state_code),
    district_code text,
    seat_class text,
    election_subtype text NOT NULL CHECK (election_subtype IN ('REGULAR','SPECIAL','RUNOFF','PRIMARY','OTHER')),
    result_status text NOT NULL CHECK (result_status IN ('CERTIFIED','UNCONTESTED','NOT_ON_BALLOT','MISSING_SOURCE','NOT_IMPORTED')),
    certification_authority text,
    certification_at timestamptz,
    publication_id bigint NOT NULL REFERENCES source_publications(publication_id),
    CHECK (office_code <> 'HOUSE' OR district_code IS NOT NULL),
    CHECK (office_code <> 'SENATE' OR seat_class IS NOT NULL OR district_code IS NOT NULL)
);
CREATE UNIQUE INDEX historical_contest_identity ON historical_contests
    (election_year,office_code,state_code,COALESCE(district_code,''),COALESCE(seat_class,''),election_subtype,publication_id);

CREATE TABLE historical_result_rows (
    result_row_id bigserial PRIMARY KEY,
    contest_id text NOT NULL REFERENCES historical_contests(contest_id),
    geography_version_id text NOT NULL REFERENCES geography_versions(geography_version_id),
    geo_id text NOT NULL,
    geo_type text NOT NULL CHECK (geo_type IN ('STATE','COUNTY','PRECINCT','DISTRICT')),
    state_fips char(2),
    county_fips char(5),
    jurisdiction_id text,
    candidate_id text NOT NULL,
    candidate_name text NOT NULL,
    party_code text,
    vote_method text NOT NULL DEFAULT 'TOTAL',
    votes bigint CHECK (votes IS NULL OR votes >= 0),
    publication_id bigint NOT NULL REFERENCES source_publications(publication_id),
    UNIQUE (contest_id,geography_version_id,geo_id,candidate_id,vote_method,publication_id)
);
CREATE INDEX historical_result_geo_lookup ON historical_result_rows
    (contest_id,geo_type,state_fips,county_fips,geography_version_id);

CREATE TABLE district_crosswalks (
    crosswalk_id text PRIMARY KEY,
    from_geography_version_id text NOT NULL REFERENCES geography_versions(geography_version_id),
    to_geography_version_id text NOT NULL REFERENCES geography_versions(geography_version_id),
    source_publication_id bigint NOT NULL REFERENCES source_publications(publication_id),
    method text NOT NULL CHECK (method IN ('OFFICIAL_BLOCK_EQUIVALENCY','OFFICIAL_PRECINCT_CROSSWALK','VERIFIED_OVERLAY')),
    verified_at timestamptz NOT NULL,
    status text NOT NULL CHECK (status IN ('VERIFIED','REJECTED'))
);

ALTER TABLE source_observations ADD COLUMN raw_payload_bytes bytea;
ALTER TABLE source_observations ADD COLUMN content_type text NOT NULL DEFAULT 'application/json';
ALTER TABLE source_observations ADD COLUMN source_revision text;
ALTER TABLE source_observations ADD COLUMN provider_response_status integer;
ALTER TABLE source_observations ADD COLUMN provider_error text;
ALTER TABLE source_observations ADD COLUMN result_status text NOT NULL DEFAULT 'COUNTING'
    CHECK (result_status IN ('NOT_ON_BALLOT','NOT_REPORTING','COUNTING','SOURCE_STALE','SOURCE_UNAVAILABLE','PROJECTION_BY_PROVIDER','OFFICIALLY_CERTIFIED'));
ALTER TABLE candidate_result_revisions ADD COLUMN reporting_unit text;
ALTER TABLE candidate_result_revisions ADD COLUMN revision_fingerprint char(64);
ALTER TABLE candidate_result_revisions ADD CONSTRAINT revision_fingerprint_valid
    CHECK (revision_fingerprint IS NULL OR revision_fingerprint ~ '^[0-9a-f]{64}$');
CREATE UNIQUE INDEX candidate_revision_idempotency ON candidate_result_revisions
    (observation_id,race_id,candidate_id,COALESCE(geo_id,''),COALESCE(revision_fingerprint,''));

-- Source evidence and result revisions are never overwritten. A correction is another revision.
CREATE FUNCTION reject_phase2_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'Election evidence is append-only; create a new revision instead'; END;
$$;
CREATE TRIGGER phase2_publication_append_only BEFORE UPDATE OR DELETE ON source_publications
    FOR EACH ROW EXECUTE FUNCTION reject_phase2_mutation();
CREATE TRIGGER phase2_history_append_only BEFORE UPDATE OR DELETE ON historical_result_rows
    FOR EACH ROW EXECUTE FUNCTION reject_phase2_mutation();
CREATE TRIGGER phase2_observation_append_only BEFORE UPDATE OR DELETE ON source_observations
    FOR EACH ROW EXECUTE FUNCTION reject_phase2_mutation();
CREATE TRIGGER phase2_revision_append_only BEFORE UPDATE OR DELETE ON candidate_result_revisions
    FOR EACH ROW EXECUTE FUNCTION reject_phase2_mutation();
