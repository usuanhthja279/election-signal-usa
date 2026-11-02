# Phase 3 implementation ledger

Status: **PARTIAL — not ready for sign-off**. Branch `feature/phase3-polls-forecasts` starts at Phase 2 V14 commit `555862a326e6ad37f023fcc2d54539f0fde01e5c`; Phase 2 is also not signed off. No Phase 4 implementation was started.

## Delivered

- Stabilized Phase 2 CI scripts for native PostgreSQL, Unix Maven wrapper execution, Windows project paths and isolated disposable database builds. Fixed historical coverage envelope status. The prior GitHub run 41393455772 had two passing and three failing jobs; these changes have not been verified by a new GitHub run.
- Flyway V5 adds source rights, polling survey, publication, answer, reconciliation, published average and forecast tables. Only approved, redistributable source records are returned by local read APIs. Explicit test records are rejected by database constraints; a dishonest false test-data flag cannot be detected automatically.
- Added conservative reconciliation decisions, disabled source adapter interfaces, scoped and paginated read endpoints, URL-backed survey/average/forecast views and source attribution. Browser mocks are fictional and used only in tests.
- Added native PowerShell Phase 3 launch wrappers and a checksum-addressed staging/structural-validation workflow. Staging is **not a database import or a license approval**. No provider is connected and no real polling or forecast rows have been imported.
- See `PHASE_3_API.md`, `POLLING_DATA_MODEL.md`, `POLLING_DEDUPLICATION.md`, `POLLING_LICENSES.md`, `PHASE_3_SOURCE_INVENTORY.md`, and `PHASE_3_TEST_RESULTS.md` for details.

## Acceptance gaps

A real provider integration with approved rights and end-to-end verification, database publishing/import, automated reconciliation enforcement, provider comparison, full UI filters and charts, collection scheduling/retry/health metrics, and complete accessibility/responsive acceptance remain outstanding. An as-of query filters publication times but cannot prove the publisher's underlying model inputs were available at that time. Catalog health reports NOT_CONNECTED, not real-time connectivity. The staging tool cannot establish authenticity or licensing from metadata. Existing Phase 2 issues are not represented as resolved merely by the CI script changes.

Polling Forecast documents a license for its own forecasts while individual polls require separate rights review. DDHQ 2026 access and redistribution permission are unverified. No fictitious credentials, real data, or provider connectivity are claimed. Preserve the existing unrelated prototype files and deletion in the worktree.

Full browser regression: 57 passed and three local-profile browser tests skipped; the separate disposable-database API tests passed. See the test results for the earlier interrupted attempt and limits.

