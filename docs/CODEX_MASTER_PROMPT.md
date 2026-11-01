# CODEX MASTER PROMPT — Election Signal Project

Use this file as the reusable header for **every Codex implementation phase**. Attach or reference `AGENTS.md`, `docs/APPROVED_UI_V7.md`, `docs/UI_ACCEPTANCE.md`, `docs/ARCHITECTURE.md`, `docs/DATA_CONTRACTS.md`, `docs/DATA_SOURCES.md`, `docs/MODELING_RESEARCH_SPEC.md`, and `docs/IMPLEMENTATION_ROADMAP.md`.

You are implementing **Election Signal**, a real election-information platform for US midterms with an approved V7 design and a working Phase 0 demo application.

## Read and baseline first

1. Inventory repository. Inspect `web/index.html`, `src/server.mjs`, all `data/` files, `tests/`, the PowerShell scripts and Codex docs.
2. Run `npm run check` and `npm start` with local health checks. Record any existing failures before modifying code. Never delete the existing V7 reference or screenshots.
3. The user explicitly chose **NO DOCKER**. Use native Windows Java 21, Node, PostgreSQL when needed, Python 3.12+ when needed. No Dockerfile, Compose, Kubernetes, Redis, Kafka or cluster unless absolutely necessary and separately approved.
4. Preserve the V7 four global contexts: Live Results, Past Results (2000–2024), Polls & Forecasts, Exit Polls. A mode is a real data type, not just a visual switch.
5. Preserve national-to-state-to-office-to-county/district navigation. All-election State Dashboard on Overview state click; Senate/Governor/House office pages; individual race/detail pages; House districts distinct from county geography.
6. Election data is high-integrity. **Never** present fixture, rehearsal, speculative or simulated numbers as actual votes, polling measurements or certified results. Maintain `isTestData`/`dataMode`, sources, timestamps, revision IDs, and an obvious demo label where needed.
7. National live Overview must show **current reported facts only**, no own win-probability values. An individual Senate/Governor/House race may show a validated model with provenance and uncertainty; otherwise show *Model unavailable*.
8. Prioritize verified 2022 and 2024 county/district returns for 2026 analysis, with same-office/same-seat earlier history when necessary; distinguish estimated votes counted, precincts reporting and turnout. Do not map entire counties onto House districts.
9. Source adapters must explicitly check license and permitted reuse. Do not scrape restricted websites, invent keys or make unreviewed automated requests to sensitive feeds.
10. Work only on the specific **phase** selected below. Do not migrate everything in one uncontrolled implementation.

## Engineering style

- Java 21 Spring Boot modular monolith with versioned REST API and SSE, Next.js + React + TypeScript frontend, PostgreSQL native with Flyway, Python later for model/ETL.
- Minimal dependencies, typed DTOs and schemas, source adapter interfaces, idempotent write operations, immutable raw source snapshots, audit-friendly revisions, validation on every ingestion boundary.
- Use sample data only from explicit fixture directories behind a `DEMO_MODE` setting. Keep real/provider and demo paths isolated so they cannot silently contaminate each other.
- Frontend accessible keyboard navigation, responsive desktop/mobile, full no-data/error/loading/stale states. Keep party colors neutral and data-driven; avoid editorial interpretations.
- Add both unit and integration tests, plus Playwright E2E for core navigation at 1440px and 390px. Never claim test success without actually running the tests.
- Update docs and `AGENTS.md` if architecture materially changes. Include PowerShell commands that can be copied by Windows users.

## Each phase report must include

- Actual implementation summary, affected files, architecture decisions, screenshots, APIs/schemas, test output.
- Which data are actual/verified, which are demo, and which feeds are disconnected.
- Risks/limitations/blockers and what will be required in the next phase.
- No background promises, no invented live outputs, and no surprise data collection or deployment.

**Now read the selected phase prompt and implement exactly that phase.**
