# Phase 2 implementation report

## Status and ledger

Branch: `feature/phase2-election-data`, based on Phase 1 commit `d2114ea7ba2f2f92f4bf865345b961eb74534aec`. Phase 2 is in progress and is not signed off. Phase 3 has not started. This ledger records verified implementation, not the target architecture as if it were complete.

| Workstream | Status | Evidence / remaining |
|---|---|---|
| 2.0 context | Implemented, test pending | URL context normalization and unit checks; integration review pending |
| 2.1 CI | Defined | Workflow added; GitHub run not yet observed |
| 2.2 registry | Foundation implemented | Migrations, API, staging validator; no verified contests/districts imported |
| 2.3 live ingestion | Framework implemented, disconnected | Provider contract, gated append-only archive and bounded scheduler; no authorized adapter/feed verified; no SSE yet |
| 2.4 history | Validation/staging foundation | No historical results have been downloaded, licensed, checksummed or published |
| 2.5 geography | Metadata foundation | Census vintage metadata and schema; no geometry/crosswalk import |
| 2.6 UI | Partial | Context and history API presentation; full V7 parity, map/result integration remain |
| 2.7 tests/security | Partial | Existing checks plus new unit/API/browser tests; full suite and PostgreSQL verification pending |

## Audit baseline

The branch began clean at the stated Phase 1 commit. Existing documentation described the preview and API but did not provide a completed Phase 2 registry, verified source coverage, or history import. Phase 2 work introduced URL year normalization, registry and historical schema/API, a no-rights-no-write recorder gate, provider scheduler abstractions, history staging validation, CI and Windows smoke scripts. No real election provider is represented as connected. The V7 prototype and reference screenshots are preserved.

## Key changes

- V3 adds jurisdiction, election/office/race, district and Senate-seat metadata, source publication, historical contest/result staging, geography versions, external race mappings and append-only observations/revisions.
- V4 seeds candidate source inventory entries disabled pending legal/source verification.
- Local profile registry endpoints return explicit `NOT_IMPORTED` for absent race/district datasets. History results require certified status and approved redistribution rights.
- Recorder refuses test fixtures, unsuccessful responses, oversized payloads, invalid status and unapproved provider rights; retains bytes and checksum and appends candidate revisions.
- Polling coordinator bounds concurrency and overlapping calls, applies retry/backoff and shuts down cleanly. It is not wired to an enabled provider.
- Context supports live/polls 2026, even historical cycles 2000–2024 and source-supported survey years.
- Scripts validate and stage input; they do not silently publish unreviewed data.

## Data sources and permissions

See [LIVE_SOURCE_INVENTORY.md](LIVE_SOURCE_INVENTORY.md), [history_sources.csv](history_sources.csv), and [GEOGRAPHY_VERSIONING.md](GEOGRAPHY_VERSIONING.md). Inventory candidates are not integrations. civicAPI exact usable election routes and terms for this use were not verified in a successful adapter test. MEDSL 2022/2024 datasets were not downloaded or imported; their file-level license, checksums and coverage remain unverified. Census 119th district metadata is recorded, but boundary files are not loaded. No commercial credentials or live feeds are configured.

## Verification record

Exact commands/results are maintained in [PHASE_2_TEST_RESULTS.md](PHASE_2_TEST_RESULTS.md). Do not interpret CI YAML as a passing GitHub run. PostgreSQL tests must use the disposable-cluster workflow; an existing user database is out of scope.

## Acceptance assessment

Not complete. URL/context fixes and supporting tests exist, but full browser acceptance, V7 parity, complete API errors/envelopes, source-grounded race registry, enabled provider or SSE, verified 2022/2024 imports, authoritative boundary/crosswalks, PostgreSQL migrations/integration, and GitHub CI execution need successful evidence. National overview must continue to omit internal win probabilities. Do not claim external coverage, county results, or district allocation without validated sources/geography.

## Windows startup

Use `Start-Phase1.ps1` and `Stop-Phase1.ps1` for the existing preview/API; Phase 2 does not replace V7. `scripts/Test-Phase2Windows.ps1` checks prerequisites and script/process ownership. Collector/replay command scripts remain a Phase 2 gap and are not claimed available.

## Phase 3 readiness

Not ready. First obtain documented provider rights and adapter fixtures, finish PostgreSQL integration, import verified historical releases with checksums, source and validate 2026 registry and boundaries, add SSE/recovery and complete responsive/accessibility acceptance. See [PHASE_3_READINESS.md](PHASE_3_READINESS.md).
