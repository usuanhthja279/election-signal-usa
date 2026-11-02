# Phase 3 implementation ledger

Status: **PARTIAL — not ready for sign-off**. Branch `feature/phase3-polls-forecasts` starts at Phase 2 V14 commit `555862a326e6ad37f023fcc2d54539f0fde01e5c`; Phase 2 is also not signed off. No Phase 4 implementation was started.

## Delivered

- Stabilized Phase 2 CI scripts for native PostgreSQL, Unix Maven wrapper execution, Windows project paths and isolated disposable database builds. Fixed historical coverage envelope status. The prior GitHub run 41393455772 had two passing and three failing jobs; [GitHub run 41402866953](https://github.com/usuanhthja279/election-signal-usa/actions/runs/41402866953) subsequently passed all five jobs for implementation commit e503825.
- Flyway V5 adds source rights, polling survey, publication, answer, reconciliation, published average and forecast tables. Only approved, redistributable source records are returned by local read APIs. Explicit test records are rejected by database constraints; a dishonest false test-data flag cannot be detected automatically.
- Added conservative reconciliation decisions, disabled source adapter interfaces, scoped and paginated read endpoints, URL-backed survey/average/forecast views and source attribution. Browser mocks are fictional and used only in tests.
- Added native PowerShell Phase 3 launch wrappers and a checksum-addressed staging/structural-validation workflow. Staging is **not a database import or a license approval**. At that milestone, no provider was connected and no real polling or forecast rows had been imported. See the continuation below.
- See `PHASE_3_API.md`, `POLLING_DATA_MODEL.md`, `POLLING_DEDUPLICATION.md`, `POLLING_LICENSES.md`, `PHASE_3_SOURCE_INVENTORY.md`, and `PHASE_3_TEST_RESULTS.md` for details.

## Acceptance gaps

Broader provider comparison, source polling scheduling/retry/health metrics, canonical race mapping, production survey source approval, and complete accessibility/responsive acceptance remain outstanding. The continuation below describes the verified forecast and manual survey import paths. An as-of query filters both publication and retrieval times but cannot prove the publisher's underlying model inputs were available at that time. Catalog health reports NOT_CONNECTED, not real-time connectivity. The staging tool cannot establish authenticity or licensing from metadata. Existing Phase 2 issues are not represented as resolved merely by the CI script changes.

Polling Forecast documents a license for its own forecasts while individual polls require separate rights review. DDHQ 2026 access and redistribution permission are unverified. No fictitious credentials, real data, or provider connectivity are claimed. Preserve the existing unrelated prototype files and deletion in the worktree.

Full browser regression: 57 passed and three local-profile browser tests skipped; the separate disposable-database API tests passed. See the test results for the earlier interrupted attempt and limits.



## Continuation — 2026-11-02

A manual, source-specific Polling Forecast adapter now fetches its licensed forecast datasets and imports attributed snapshots to local PostgreSQL. Senate, Governor and House public endpoints were checked directly; live import tests use a disposable database and do not populate the user's database. Poll Forecast owns the forecast output under the documented CC BY 4.0 terms; individual survey rows have different terms and are not fetched or assumed licensed. The adapter is manually invoked, not a scheduled feed. It stores date-only source freshness without fabricating a timestamp, and provider-specific race IDs without claiming a canonical mapping. `scripts/Import-PollingForecast.ps1` is documented in `PHASE_3_IMPORT_GUIDE.md`.

A separate manual single-survey publisher now requires individually approved source metadata, stores the original file and reconciles matching syndications transactionally. Matching publications retain distinct citations; conflicting or ambiguous records are rejected. Fixture integration tests use only disposable PostgreSQL. No real individual polling records have been approved or imported. Operators must review source rights independently; flags are not proof of authenticity. A source polling scheduler, automatic retries, source-change notifications, complete production candidate/race mapping and broad source coverage are still incomplete. Forecast access verification is not a guarantee of election-data correctness.

The polling UI adds URL-backed pollster/date filters and a source-separated published-average chart with a text listing. Chart data are publisher-provided; the app does not fabricate averages. Page-size limits apply. Existing screenshots and the pre-existing unrelated prototype changes remain preserved. See `PHASE_3_TEST_RESULTS.md` for exact tests.
