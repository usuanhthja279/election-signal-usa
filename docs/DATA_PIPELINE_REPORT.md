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

## Continuation: 2026-11-02

- Added migration V10 and `RawSourceArchive` for append-only, SHA-256-deduplicated original ZIP, JSON, CSV or TSV bytes, with an 8 MiB limit. The exact registered HTTPS URL, provider enabled status, verified license and separate archive permission must all match. Fictional test inputs, non-200 responses and unapproved providers are rejected. Archive-only authorization does not authorize display or API redistribution. The service has no public write endpoint.
- `scripts/Archive-NorthCarolinaResults.ps1` is a manual, one-shot command. It requires explicit local DB configuration and an independently reviewed enabled provider record `NC_2026_OFFICIAL` whose registered URL exactly matches the official 2026 ZIP, plus `source_usage_permissions.archive_allowed=true`. It checks permission before making the request, parses and validates the file before recording, and does not publish its contents. The user must review terms and evidence themselves; no approval is preconfigured. This command and parser do not enable a scheduler, map races or establish real election-night operation. It may migrate the explicitly configured database; test it against a disposable DB first.
- Corrected JDBC handling of the source timestamp when persisting candidate revisions. The archive tests use fictional bytes exclusively in a disposable database.
- Direct Harvard Dataverse metadata lookup for MIT House V15 found file ID 13592823 and a 2026-03-09 release; the requested original download was refused because a guestbook response is required. No guestbook was bypassed and no House records were imported. Dataset metadata: https://doi.org/10.7910/DVN/IG0UN2
- Actual Polling Forecast House, Senate and Governor outputs were fetched and imported twice into a disposable PostgreSQL database with `ELECTION_VERIFY_POLLINGFORECAST=true`; the integration test verified idempotency and scoped API availability. This does not import underlying pollster records or modify the user's database. The publisher describes its own outputs as CC BY 4.0 with attribution: https://www.pollingforecast.com/us/data
- `scripts/Test-Phase3.ps1` passed frontend typecheck and 13 unit tests. `npm run test:e2e -- history-source.spec.ts phase3-polling.spec.ts --workers=1` passed 18 browser tests at desktop, mobile and tablet, with synthetic test responses. A separate disposable native PostgreSQL run applied V1–V10 and passed 29 backend tests including actual forecasts, with none skipped. `Archive-NorthCarolinaResults.ps1` passed PowerShell syntax parsing.
- No real source was granted archival or publishing permission; the manual collector has not yet archived an official feed. Canonical mappings, event publishing, live race data, House history and broader historic geography remain incomplete. Nor do the focused browser tests establish full UI parity.
- Final recheck after adding the manual command: disposable PostgreSQL/Flyway V1–V10 and live forecast test passed 30 tests with none skipped. The revised North Carolina preflight again validated 141,466 rows across 100 counties and found zero nonzero totals; it did not archive or publish the file.
