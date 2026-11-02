# Data pipeline implementation ledger

Branch `feature/data-foundation-and-phase3` starts from `feature/history-forecast-live-data` at `441a780`, which contains the reviewed V14 base and the existing historical and forecast work. This is a partial implementation; no live feed is connected or displayed.

## Implemented

- Added North Carolina precinct ZIP validation and bounded streaming TSV parser. The explicit `scripts/Test-NorthCarolinaResultsSource.ps1` preflight fetches the official file without saving or publishing it. The source supplied all-zero pre-election rows when tested, and exact geographic and candidate mappings remain unverified.
- Added an Arizona documented-format parser. It preserves local versus state scope and unknown versus zero values, and does not manufacture calls. It has only been tested on fictional inputs because the published mock file returned 404.
- Added migration V9 for separate archive/display/API reuse permissions. The existing JSON observation recorder now requires explicit archive permission, verifies raw bytes against the submitted JSON, and retains duplicate/correction behavior. Permission flags for future display and redistribution must be enforced by future delivery code. No provider permissions were granted.
- Fixed polling coordination to respect Retry-After even when it exceeds the normal retry cap, and added per-provider minimum-cadence enforcement. No new scheduled source is activated.
- Source, rights and coverage findings: `DATA_SOURCE_VERIFICATION_MATRIX.md`, `INGESTION_SOURCE_RIGHTS.md`, and `DATA_COVERAGE_MATRIX.md`.

## Executed checks (2026-11-02)

- `./scripts/Test-Phase3.ps1`: frontend typecheck and 13 unit tests passed before changes. No frontend files were changed.
- With local JDK 21, local Maven repository and `ELECTION_VERIFY_MEDSL_PATH` pointing to the existing runtime MIT Senate file: `./scripts/Test-Postgres.ps1 -PgBin .runtime/pgcore/pgsql/bin -Port 55464` passed on a newly created disposable PostgreSQL 17.11 cluster: 28 backend tests, 0 failures, 0 errors, 1 skipped (opt-in live forecast verification). Flyway applied migrations V1–V9. Existing user databases were not modified.
- `./scripts/Test-NorthCarolinaResultsSource.ps1`: initially failed under restricted networking (Java loopback/DNS); succeeded with network access. Its result was 141,466 rows, 100 county names, zero nonzero total rows. This verifies this particular pre-election package, not election-night behavior or redistribution rights.
- `git diff --check` passed, with line-ending warnings. Maven emitted remote repository metadata 401 warnings while resolving from the existing local cache; the test build passed.
- No new browser, performance, CI or production election-night integration run was conducted for this backend-only change. Do not infer those passes from prior reports.

## Remaining

A rights-cleared official live source must still be activated, mapped to canonical races, archived in its actual format (the existing observation archive accepts JSON, whereas NC distributes ZIP), and safely delivered through scoped REST/SSE with freshness, corrections and replay. Verify real election-night updates, source provenance and reuse rights before publishing. Florida and Arizona access remain unverified, and Open America requires permission before repackaging. Further historical House/county imports, geography, pollster rights, survey ingestion, coverage dashboards, browser regression and source-specific verification remain incomplete. No Phase 3 or overall data-pipeline acceptance is claimed.
