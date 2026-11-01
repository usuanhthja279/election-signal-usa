# CODEX PHASE 5 — 2026 live election results ingestion

Read `AGENTS.md`, `docs/CODEX_MASTER_PROMPT.md`, `docs/DATA_CONTRACTS.md`, `docs/DATA_SOURCES.md`, and `docs/MODELING_RESEARCH_SPEC.md`. Implement only Phase 5, prioritizing safe archival and factual display.

## Goal

Real-time results for actual House, Senate and Governor contests with accurate county/district granularity where the licensed feed provides it. National Overview contains **reported results only**; source race calls are separate from current leads.

## Steps

1. Audit official 2026 election registry (regular/special races, Senate 2 seats per state, governor election availability, 435 House districts, candidates, multi-party ballots, caucus status). Use source-backed registry, not invented `2026` fixtures.
2. Implement provider adapter(s) starting with one **permitted, tested, legally authorized** feed (AP or suitable alternative). Verify real-vs-test flags, event time, county/district coverage, reporting metric definitions, revisions, rate limits and attribution.
3. On every fetch: capture raw payload, checksum, upstream time, ingest time, provider race ID, request metadata and license; archive append-only.
4. Map to canonical races, candidates and FIPS geographies; stage/validate observations and reject mismatched state, duplicate candidate IDs, invalid negative counts except correctly represented revisions, missing race fields and stale updates. Never substitute an absent count with 0.
5. Compute idempotent result snapshots and materialized current race state. Preserve vote-count corrections, race-call retractions, status changes, certification as separate typed events.
6. Implement county-level race results where actually available. House results require district-specific geography even when source provides county aggregates. Keep county and district layers distinct.
7. Publish small changed-race events via SSE with event IDs and reconnect snapshot behavior; centralize provider polling so browser users never individually query upstream.
8. Make source health and freshness visible. `No reports`, `not on ballot`, `provider missing`, `stale`, `counting`, `called`, `certified` are different states.
9. On national Overview, map hover should show state's live Senate/Governor vote shares, type-specific counting percentage and House districts reporting / called / uncalled counts, plus source-as-of time. **No win probabilities on Overview**.
10. Use load tests with unmistakably marked simulated fixtures; no synthetic results in production.

## Acceptance

- Raw snapshots allow complete replay at timestamp; a count can decrease on correction without being lost.
- Real provider sample reconciles against verified source; no mistaken rehearsal results.
- House District 07 own reported results distinct from Senate/Governor statewide races.
- Source outage shows stale snapshot and source time, not invented substitute.
- Each number has source attribution, correct metric type and freshness; source calls only appear when publisher called a race.
- Native Windows startup works without Docker. Unit, integration, replay, E2E and error-injection tests pass.

Report coverage by office/state/county, cost/license limitations, screenshots and source health. Stop for review.
