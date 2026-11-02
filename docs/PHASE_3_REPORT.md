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

## Polling and forecast journey continuation (2026-11-02)

- A House poll or forecast request now validates its state/office/district syntax without requiring the unrelated, currently incomplete 2026 election registry. Results still come only from matching, approved survey or publisher records. The UI preserves a valid district deep link and labels a provider district when the registry is unavailable; this does not verify district boundaries or canonical race identity.
- Added previous/next controls for polling and forecast result pages so records beyond the first 50 are reachable. Page state is session-local and resets when the query scope changes; page position is not URL-backed. No data or provider was enabled by these UI changes.
- Read-only inspection of the running local API showed 35 Senate forecast rows and no connected individual surveys at the time of the check. The current running preview may use an older backend build until restarted; the changes in this continuation were tested separately.
- VoteHub's documented API (https://votehub.com/polls/api/) states CC BY 4.0. The documented `GET /polls?poll_type=us-senator&from_date=2026-10-20` returned a top-level array rather than the documentation's illustrated `polls` envelope. One Georgia Senate row was checked against its original ActiVote publication (https://www.activote.net/polls/2026/ga-u-s-senate/); dates, sample and reported shares matched. VoteHub is an aggregator, and this does not establish each original pollster's rights or complete race identity. The existing project rule requires individually reviewed survey publications, so no VoteHub poll is imported or exposed by this work. Do not transfer the Polling Forecast model license to third-party poll figures.
- `Test-Postgres.ps1` with the actual Polling Forecast source enabled passed 31 tests with 2 unrelated historical file tests skipped. The Phase 3 frontend typecheck and 13 unit tests passed. Browser tests for scoped House forecast deep links and pagination use fictional responses; see the following test entry for final results.

Remaining: individually approved real survey publications and a source-specific importer; broader provider and race reconciliation, automatic forecast refresh and reliable monitoring; complete polling/forecast browser and accessibility coverage. Do not claim the complete poll journey is connected until a rights-approved real survey is imported and shown.

Final browser run: `ELECTION_E2E_PORT=3002`, `ELECTION_E2E_REUSE_SERVER=1`, `npm run test:e2e -- poll-journey.spec.ts --workers=1`: 6 passed across desktop, mobile and tablet. The tests use fictional API responses and cover deep-link refresh, exact district scope, paging and scope-change reset.
