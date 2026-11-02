# Codex Agent Instructions — Election Signal

These instructions apply to the entire repository.

## Mission and nonnegotiable constraints
1. Produce an accurate, neutral US election information and analysis application for 2026 midterms and future cycles.
2. **Windows-first, native processes, PowerShell launchers. NO Docker, Docker Compose, Kubernetes or containers unless user explicitly changes the requirement.**
3. The approved visual and navigation reference is `web/index.html` (V7). Read `docs/APPROVED_UI_V7.md` and `docs/UI_ACCEPTANCE.md` before any frontend redesign.
4. Four **global and mutually exclusive contexts**: Live Results, Past Results (2000–2024), Polls & Forecasts, Exit Polls. Context must propagate into all state, office, county, district, and race pages.
5. National live Overview is **reported results only**. Win-chance models are allowed only on individual Senate/Governor/House district race pages, clearly marked model estimates, never confused with reported vote percentages or publisher race calls.
6. Primary 2026 statistical comparison is office/geography-aware **2022 and 2024** results. Prior same-seat history is needed for some races; never force a bad county-to-district comparison.
7. **All V7 browser figures and `data/demo/` data are fictional.** Keep a visible demo banner, never relabel them as reported or real historical/polling values.
8. If a provider's license, availability, API schema, geographic coverage, or source timestamp is unknown: mark it unverified or unavailable. Do not invent endpoints, credentials, permissions, data or race-call status.
9. Official certification, publisher projection, reported vote lead, polling share, exit-survey share and model win probability are separate typed concepts.
10. Incremental changes: implement only the requested phase, run unit/integration/E2E tests, document outcomes and blockers; don't silently start the next phase.

## Current working baseline
- `node src/server.mjs` serves the V7 frontend and read-only demo APIs on localhost.
- `node --test` is the Phase 0 backend test suite.
- `web/api-explorer.html` demonstrates actual local API integration.
- Third-party feeds are **all disabled** in Phase 0.
- Target Phase 1 stack: Java 21/Spring Boot modular monolith, PostgreSQL native Windows, Next.js/React/TypeScript frontend, Python later for analytical modeling. Do not replace a working baseline until tests prove parity.

## First file to read
`docs/CODEX_MASTER_PROMPT.md` and the selected phase prompt in `docs/codex/`.

## Phase 3 data imports
Polling Forecast's published forecasts and individually licensed survey publications have separate, explicit local-only import paths. Follow `docs/PHASE_3_IMPORT_GUIDE.md`; never apply forecast rights to third-party poll figures, fabricate source timestamps, or import fixtures into non-demo databases. No provider is polled automatically.
