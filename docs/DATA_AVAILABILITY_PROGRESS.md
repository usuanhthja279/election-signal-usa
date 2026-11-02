# Data availability work — 2026-11-02

## Implemented
- MIT Election Data and Science Lab Senate V8 official-source rows for 2022/2024: pinned original CSV, publisher MD5 verification, SHA-256, source archive, duplicate-safe import, 65 contests/302 candidate-party rows, sum validation and explicit source-reported status. Unofficial rows and runoff stages are excluded. No independent certification, county, House or Governor claims are made.
- Dedicated local PostgreSQL preview startup imports the MIT release and CC BY 4.0 Polling Forecast publisher snapshots, with separate source types and no demo data in local observations. The preview database and encrypted password live under the ignored project `.runtime` folder. Original V7 files and reference screenshots are preserved.
- History UI consumes the source-reported data mode, retains office/state/year context and cites sources. State and office panels show scoped data.
- A read-only California Secretary of State Governor feed preflight fetches and validates source shape. It is not a live collector; the app never publishes its results.

## Limits
- Exit-poll data are not connected. National Election Pool access and redistribution rights remain unverified. AP VoteCast public-use files are a separately defined phone/web voter survey, not an exit poll; reuse terms and weighted analyses require review before any integration.
- Historical House, Governor and county imports remain absent. Harvard’s House release requested a guestbook response and was not downloaded.
- Live ingestion, raw observation storage, mapped races/candidates, source freshness and live UI delivery remain disconnected. The pre-election California feed contains zero precincts reported; successful retrieval is not evidence of election-night readiness or verified licensing.
- Published forecasts in the local preview are attributed external model estimates, not internal probabilities, polling shares, race calls, or reported votes. No unlicensed polling data are imported.

## Verification

- The 2022/2024 source file was retrieved from Harvard Dataverse. The original CSV's MD5 matched `0f8b51cf0f77a2ef0bff992f7a64d1b4`; SHA-256 is recorded in `history_sources.csv`.
- `scripts/Test-Phase3.ps1`: TypeScript and 13 frontend unit checks passed before the historical UI follow-up.
- `scripts/Test-Postgres.ps1 -PgBin .runtime/pgcore/pgsql/bin -Port 55459` with `ELECTION_VERIFY_MEDSL_PATH` set to the verified CSV: 21 tests, zero failures, one forecast-network test skipped; the actual historical file imported, queried and reimported idempotently against a disposable PostgreSQL instance.
- `npm run test:e2e -- history-source.spec.ts --workers=1 --reporter=line`: 3 passed for desktop, mobile and tablet using a clearly fictional browser fixture.
- `scripts/Test-CaliforniaResultsSource.ps1`: public official Governor endpoint responded; result `NOT_REPORTING` and 0% precincts. This was a read-only preflight, not a data import.
- An initial preview launch failed because old build output contained a duplicate Flyway migration; the launcher now builds in an isolated clean directory. A password file newline also caused an initial restart failure and was corrected. See `HISTORY_IMPORT_GUIDE.md` for startup and shutdown.
- Final `scripts/Test-Postgres.ps1 -PgBin .runtime/pgcore/pgsql/bin -Port 55463` with the verified source path: 22 tests, zero failures, one skipped opt-in live-forecast test. The run checked source-reported coverage, health status, raw import and idempotency; database was stopped.
- Final `npm run test:e2e -- history-source.spec.ts phase3-polling.spec.ts --workers=1 --reporter=line` on the local development preview: 18 passed across desktop, tablet and mobile. Browser data were labeled fictional fixtures. The real local preview was separately checked in the in-app browser with actual imported 2024 Texas Senate rows and an attributed Texas forecast.
