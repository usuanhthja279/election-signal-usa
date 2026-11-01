# Stepwise Implementation Roadmap

**Phase 0, delivered**: UI reference + native Node demo server, API contracts, scripts, tests, Codex instructions. No real election data.

**Phase 1 — Production frontend + backend foundation**: initialize Next.js TypeScript and Spring Boot Java 21 native Windows, PostgreSQL and Flyway; port design into React; construct canonical registry and modes; typed APIs; demo fixture toggle; reliable startup/stop. Keep existing Node demo as a parity baseline until tests pass.

**Phase 2 — Verified historical data + maps**: import 2000–2024 election years per available offices/geographies, 2022/2024 county/precinct first, FIPS normalization, validate 2026 congressional districts, build county/district historical comparison with coverage metadata.

**Phase 3 — Polls + published forecasts**: identify legal provider(s), configure credentialed adapters and polling methodology/duplicate keys; render original surveys separately from aggregates and model forecasts, preserve provider context and freshness.

**Phase 4 — Exit polls / voter surveys**: optional authorized AP VoteCast or other provider, historical archival permissions, embargo and suppression controls, demographic/issue charts. Show availability limitations, not invented crosstabs.

**Phase 5 — Live results 2026**: source adapters, canonical race registry and full candidate ballots, raw immutable archives, idempotent revisions, county/district granularity where available, source-based leader/call/certification, SSE deltas, monitoring, provider failover. Prioritize early collection and raw source snapshots.

**Phase 6 — Statistical analysis/model**: historical 2022/2024 primary context, same-office previous history where needed, county observed shift, remaining ballots, reporting order, backtests, uncertainty and abstention; model estimates only on individual Senate/Governor/House district races.

**Phase 7 — Operations/reliability**: dashboards/alerts, provider contractual checks, load tests, caching/CDN, audit and replay, accessibility, security, source provenance, release procedure. No container requirement unless later explicitly approved.

## Gate across all phases

At the end of each phase, Codex must provide: tests executed and their output; changed paths; backend/frontend contracts; fixture-vs-real-data declaration; source license review; screenshots at 1440px/390px if UI changed; known limitations and next-phase dependencies. User must be able to run and review the phase independently.
