# Phase 2 test results

This file records only commands actually run in the current work session. Update it after each verified run; do not infer a pass from older report entries.

| Check | Command | Result |
|---|---|---|
| Frontend context unit suite | `cd frontend; npm test` | Passed: 9/9 |
| Frontend TypeScript | `cd frontend; npm run typecheck` | Passed |
| Frontend production build | `cd frontend; npm run build` | Passed (Next.js static production build) |
| Playwright discovery | `cd frontend; npm exec playwright test -- --list` | Passed: 24 tests discovered across desktop/tablet/mobile |
| Playwright execution | `cd frontend; npm exec playwright test` | Failed to execute acceptance assertions: 21 failed, 3 skipped because browser navigation to `127.0.0.1:3000` returned `ERR_NETWORK_ACCESS_DENIED`; screenshots/assertions are not verified. A subsequent single-test retry also could not establish the expected local web server; port 3000 was occupied by an unknown process and was left untouched. |
| Backend compile | `cd backend; .\mvnw.cmd -o -B test-compile` | Passed: production and test Java sources compile |
| Phase 2 backend unit tests | `cd backend; .\mvnw.cmd -o -B -DforkCount=0 '-Dtest=Phase2FoundationTests,PollingCoordinatorTests' test` with project JDK 21 and local Maven cache | Passed: 7 tests, 0 failures/errors |
| Full backend suite | `cd backend; .\mvnw.cmd -o -B test` | Compiled; Spring demo context started, then stalled during Mockito self-attach/Windows pipe operation. Forked and in-process attempts did not complete. |
| GitHub Actions | GitHub workflow | Not run/observed; no CI result link available |

## Environment

Project JDK is `.runtime/jdk/jdk-21.0.13+9`. Maven uses an ignored project-local cache at `.runtime/m2-repository` to avoid writing into the protected user Maven cache. PostgreSQL verification must use the repository's disposable native workflow and must not point to a user database.

The test attempts emitted a portable Git `test.exe` MSYS named-object access warning. This was not treated as the root cause of frontend tests; their reported failure is loopback network access. The backend runner reported a Java attach pipe error after Mockito initialized and did not complete. These are environment blockers, not passing tests.
