# Phase 3 test results

Local tests were run on Windows on 2026-11-02. GitHub Actions [run 41402866953](https://github.com/usuanhthja279/election-signal-usa/actions/runs/41402866953) passed all five jobs for implementation commit e503825.

- `scripts/Test-Phase3.ps1`: PowerShell script syntax, frontend TypeScript and 12 context/unit tests passed. Its optional PostgreSQL step was run separately below.
- With the project-local JDK and Maven repository configured, `scripts/Test-Postgres.ps1 -PgBin '.runtime/pgcore/pgsql/bin' -Port 55449`: created a disposable database, applied Flyway V1–V5, and passed 15 backend tests including local API, source-rights filtering and reconciliation tests; then stopped the database. No existing database was used. Some Maven metadata repositories returned 401 warnings; the build succeeded.
- `npm run build`: the default output directory encountered Windows EPERM during prerender while an existing preview was present. A production build using a temporary alternate Next output directory succeeded, including TypeScript and prerendering. The temporary Next configuration was used only for this verification.
- `npm run test:e2e -- phase3-polling.spec.ts --workers=1 --reporter=line` with `ELECTION_E2E_PORT=3002`: 9 passed across desktop 1440, tablet 768 and mobile 390. The screenshots in `screenshots/phase3/` show **fictional test mocks**, not actual provider data. They cover state survey, race survey and published forecast layouts. Desktop forecast and mobile race survey screenshots were visually inspected.
- `npm run test:e2e -- phase3-polling.spec.ts --grep 320px --project=mobile --workers=1 --reporter=line`: one narrow-screen reduced-motion and keyboard-focus check passed. This is not exhaustive accessibility coverage.
- `Validate-Polls.ps1` accepted a structural test document with an integer sample size and zero percentage, and rejected the same explicitly test-marked document. This checks validation only; it does not establish rights or import data.
- `Stop-Phase3.ps1` with no Phase 3 ownership marker refused to stop any process. Start/stop of an owned Phase 3 instance was not exercised because an existing user preview was running.
- Full browser suite with `ELECTION_E2E_PORT=3002` and `npm run test:e2e -- --workers=1 --reporter=line`: 57 passed, three local-profile browser tests skipped because the required separate PostgreSQL-backed server was not running. An earlier run had 56 passes, one connection failure and three skips when the preview restarted during cleanup. These automated tests do not establish exhaustive visual or accessibility acceptance.

Earlier attempts: a stale build output introduced a duplicate V3 migration; isolated Maven output fixed it. A subsequent non-isolated clean build was blocked by the running application jar. Other earlier runs exposed the corrected historical envelope defect. Two additional browser startup attempts failed due to an occupied port/Next development lock; these were environment failures and are not counted as test passes. The final database test was rerun after the schema changes.

The last observed GitHub run on the Phase 2 base was [41393455772](https://github.com/usuanhthja279/election-signal-usa/actions/runs/41393455772): two jobs passed and three failed. The newer run linked above passed frontend, backend, native PostgreSQL, Playwright and Windows smoke jobs.





## Continuation verification — 2026-11-02

- `scripts/Test-Phase3.ps1`: frontend TypeScript and 13 unit/context tests passed; PostgreSQL was run separately.
- With project JDK, `ELECTION_VERIFY_POLLINGFORECAST=true`, and isolated Maven output, `scripts/Test-Postgres.ps1 -PgBin '.runtime/pgcore/pgsql/bin' -Port 55455`: PostgreSQL 17.11 applied V1–V7 and 20 tests passed with no failures or skips. The opt-in live test fetched Senate, Governor and House forecast datasets, imported each in the disposable database, queried the API, and verified a second import was idempotent. Synthetic survey tests covered source approval, two-citation reconciliation and conflicting revisions. The temporary database was stopped; no existing user database was changed.
- An earlier database attempt failed because a SQL date serialized as a timezone-shifted instant; the implementation now returns an ISO date string and the final run passed.
- `npm run test:e2e -- phase3-polling.spec.ts --workers=1 --reporter=line` with `ELECTION_E2E_PORT=3002`: 12 passed across desktop/tablet/mobile before the final pollster selector refinement. The updated targeted `--grep '320px|filters persist'` run passed all 6 checks across the three sizes.
- New PowerShell import scripts passed syntax parsing. Their Java service paths were tested directly against disposable PostgreSQL; full standalone script startup with a user-selected database was not tested.
- The full browser run in progress during UI edits saw a 320px overflow, one county navigation timeout, and a stale pollster label in a running test. The overflow and label were corrected; see the final run entry below for the clean rerun. No real polling survey source was imported.
- Clean full browser rerun with `ELECTION_E2E_PORT=3002` and `npm run test:e2e -- --workers=2 --reporter=line`: 63 passed, three local-profile browser checks skipped because the separate local-profile browser server was not running. An earlier run while the UI was being edited finished with 60 passed, three failed and three skipped; the narrow layout and changed filter label were corrected before the clean rerun. The county navigation timeout also passed on rerun.
- The first point-in-time integration rerun exposed test fixture cleanup failing to remove a newly added late forecast; the fixture cleanup was corrected before the final rerun.
- Final `ELECTION_VERIFY_POLLINGFORECAST=true` disposable PostgreSQL run on port 55457: all 20 tests passed, including all three live forecast datasets, syndicated survey reconciliation, distinct questionnaire versions, conflicting revisions, and publication/retrieval cutoffs. Flyway V1–V7 applied successfully and the database was stopped. No existing database was touched.
- The fresh 390px mobile screenshot at `screenshots/phase3-continuation/mobile-polling-fixture.png` was visually inspected; displayed values are fictional browser fixtures.
