# CODEX PHASE 1 — Production foundation and V7 UI conversion

**Copy this prompt into Codex** after extracting the Phase 0 ZIP and opening this repository. Start by reading `AGENTS.md` and `docs/CODEX_MASTER_PROMPT.md` in full. **Implement only Phase 1.**

## Goal

Replace the temporary Node/inline-HTML implementation with a robust native Windows project **without losing any approved V7 UI interactions**. Do NOT connect real election feeds or publish real predictions during this phase.

## Step-by-step tasks

1. **Audit baseline**: inspect `web/index.html`, `reference/election_signal_phase0_v7_original.html`, source fixtures, API explorer, screenshots and existing Node tests. Run `npm run check`; retain reports. Create `docs/phase1-baseline.md` mapping each existing V7 page and interaction.
2. **Repo modules**: create `frontend/` Next.js App Router + TypeScript (strict), and `backend/` Spring Boot Java 21 Maven with wrapper. One Spring Boot monolith, organized packages `registry`, `results`, `history`, `polls`, `exitsurveys`, `models`, `geography`, `sources`, `api`, `events`, `config`. Do not add Kafka, Redis, Docker or microservices.
3. **Native database**: add PostgreSQL config profiles `local` and `demo`; Flyway migrations for source registry, elections, offices, races, candidates, geographies and sample snapshot schema (reference `docs/SCHEMA_BLUEPRINT.sql`). Document native Windows PostgreSQL installation and creation of database/user. If DB is unavailable during initial setup, a clearly labeled demo mode with dedicated fixtures may run, but never quietly write test data into production tables.
4. **Server API parity**: implement Java REST endpoints compatible with existing `/api/v1/health`, `/meta`, `/sources/health`, `/context`, `/demo/races`, `/geography/manifest`, `/geography/states/{state}/counties`, `/history/results`, `/polls`, `/forecasts/published`, `/exit-polls`, `/models/races/{raceId}`. Preserve status and type semantics; intentionally return NOT_CONNECTED/NOT_IMPORTED/UNAVAILABLE until sources truly exist. Add Spring validation and problem details.
5. **Convert V7 UI** into small reusable components: `ElectionShell`, `GlobalModeTabs`, `NationalOverview`, `NationalOfficePage`, `StateElectionDashboard`, `HouseDistrictPage`, `CountyMap`, `CountyTooltip`, `CandidateVotes`, `RaceModelPanel`, `PollingDashboard`, `ExitSurveyDashboard`, `HistoricalYearPicker`, `SourceStatus`. Reuse V7 visual palette, spacing, information hierarchy, map shapes and hover behavior. Keep original HTML under `reference/` unchanged.
6. **URL-backed context**: support canonical parameters `mode`, `year`, `office`, `state`, `district`, `provider`, `pollView`. Build typed query parsing and navigation helpers. Ensure browser back, refresh and deep links preserve mode/year/provider across national, office, state, district and county views. Context changes must not reuse stale data from the previous mode.
7. **Typed API client** with status-aware discriminated unions. `available`, `not_connected`, `not_imported`, `not_on_ballot`, `not_reporting`, `stale`, `error`, `demo` must render separately. Sample data must be behind explicit `DEMO_MODE` and visually labeled. No unsupported numeric defaults.
8. **Windows scripts**: update root `Start-All.ps1` to start PostgreSQL health (if enabled), Spring Boot API and Next.js, capture PIDs, display localhost URLs, check port conflicts, and provide `Stop-All.ps1` that only stops project-owned processes. `Test-All.ps1` should run unit/integration and E2E tests. Keep no-container startup.
9. **Test and review**: unit tests for mode parsing and state/office filtering; API MockMvc tests for every status; Playwright E2E at desktop/mobile for V7 interactions; accessibility checks for keyboard focus and accessible maps. Compare screenshots against `reference/screenshots/`.

## Must-pass acceptance

- Overview TX state click opens all-election state page; Senate/Governor/House clicks preserve office. House TX district 07 opens its own race page.
- All 4 global modes persist across state/office/back navigation; history year 2000 remains 2000; poll provider remains selected.
- National live Overview contains zero model win probabilities; model panels exist only on individual race pages and show *unavailable* unless validated.
- Demo fixture numbers cannot appear under `LIVE_REPORTED` or `HISTORICAL_CERTIFIED` metadata.
- Source types and reporting percent types are explicitly labeled. Not-on-ballot never renders as 0 votes.
- Original V7 design remains recognizable and mobile works.
- Java and Next start natively from PowerShell, no Docker; tests pass; paths/scripts documented.

## Deliverable

Working source and a `docs/PHASE_1_REPORT.md` containing exact run/test commands, screenshots and a source-status matrix. Stop after Phase 1 for review.
