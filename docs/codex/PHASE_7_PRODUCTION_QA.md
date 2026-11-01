# CODEX PHASE 7 — QA, resilience and release readiness

Read all root and docs agent instructions. Implement only Phase 7 on a working, validated foundation.

- Threat-model the public election web app, enforce source-rights controls, no credentials in browser, rate-limit public APIs, appropriate TLS, caching and CORS.
- Load test source refresh and high concurrent viewer traffic using explicitly synthetic replay fixtures. Browser clients must not directly poll licensed vendors.
- Verify offline source behavior, stale data, negative count corrections, call retractions, election status changes, certification updates, Senate tie/caucus arithmetic and House redistricting.
- Implement dashboards for provider freshness, source failures, API errors, ingestion lag, geography version, model abstentions, audit log integrity, and election-night operational runbooks.
- Run keyboard/screen-reader/mobile accessibility tests and screenshot comparisons against V7 UI reference; ensure tooltip accessibility and mobile county list fallback.
- Validate backups, database migrations, archive and replay, historical data coverage notes and license/attribution pages.
- Run pen tests/security linting and fix issues; include audit of sample fixtures so none can appear in a production result feed.
- Provide Windows-native dev/QA scripts and an explicitly documented production deployment design; do not introduce Docker without user approval.
- Publish a final launch gate report that includes actual connected sources, purchased rights, state/office coverage, all test results, release blockers and a plain-English explanation of remaining uncertainty.

Do not call the platform ready for public election-night reporting without an authorized feed, verified boundaries, reliable testing and a documented publication policy.
