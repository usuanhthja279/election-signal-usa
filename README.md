# Election Signal — Working Phase 0 Application

**Status:** runnable local demonstration. **No Docker. No npm dependencies. No database required yet.**

The original Phase 0 application remains the default. Phase 1 has a separate React/Spring preview in `frontend/` and `backend/`; it is presented for review, and does not replace V7. See [the Phase 1 report](docs/PHASE_1_REPORT.md) for acceptance status, exact checks, screenshots and remaining gaps. After installing frontend dependencies with `npm ci` in `frontend/`, try the demo profile with `./Start-Phase1.ps1 -NoBrowser` and stop it with `./Stop-Phase1.ps1`. The preview requires Node.js 20.9+ and Java 21. Its optional, fixture-free PostgreSQL profile is described in [the database setup](docs/POSTGRES_SETUP.md).

This project packages the agreed **V7 election UI** into a locally hosted application, plus a functioning read-only API for schema demonstration, source-status inspection, sample race data, reference county geometry and an SSE status handshake. It includes native Windows PowerShell launchers, automated API tests, docs and Codex implementation prompts for future phases.

> **ELECTION DATA WARNING:** Every 2026 reported count, probability or poll graphic displayed by the V7 browser UI is **fictional design data**. The server deliberately reports `dataMode: DEMO`, `isTestData: true`. No election feeds, verified historical results, opinion polls, exit polls or predictive models are connected. Reference geography is **not verified 2026 House district geometry**.

The V7 prototype now has a Phase 2 review layer: global mode/year URLs, neutral historical geography and explicit source/registry availability states. The original approved V7 page is preserved at `reference/prototypes/election_signal_v7_approved.html`; review the updated prototype and captures in [the Phase 2 prototype notes](docs/PHASE_2_PROTOTYPE.md). This remains a static demo with no verified election source connected.

## Requirements

- Windows 10/11 and **Node.js 20 or newer** (Node.js 22 LTS recommended). PowerShell 5.1+.
- Chrome, Brave, Edge or another modern browser.
- **Not required:** Docker, PostgreSQL, Java, Python, internet, npm install, any API key.
- For later phases (not needed for Phase 0): Java 21, Spring Boot, PostgreSQL/PostGIS, Python 3.12+, Next.js/TypeScript.

## Start on Windows

Extract the entire ZIP to a writable project folder, such as `C:\Projects\election-signal-phase0`. Open PowerShell in that folder:

```powershell
node --version
.\Start-All.ps1
```

Your browser opens **http://127.0.0.1:4173**. The start script launches a background `node` process. If execution policy blocks the script:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Start-All.ps1
```

Or run directly in the foreground (works on other systems, too):

```powershell
node src/server.mjs
```

Stop it with:

```powershell
.\Stop-All.ps1
```

Use `Ctrl+C` if started in a foreground terminal. Change the PowerShell port with `.\Start-All.ps1 -Port 4273` or set `PORT=4273` in your terminal before using `npm start`.

## Main pages

- **Dashboard:** `http://127.0.0.1:4173/` — V7 four-mode UI with clickable US map, House/Senate/Governor office views, state → county/district drilldowns, historical-year control, polls/forecasts and exit-poll placeholders, and clearly marked fictional illustrations.
- **API Explorer:** `http://127.0.0.1:4173/api-explorer.html` — click through the working endpoints and inspect JSON.
- **Health:** `http://127.0.0.1:4173/api/v1/health`.
- **Data Source Status:** `http://127.0.0.1:4173/api/v1/sources/health`.
- **County geometry demo:** `http://127.0.0.1:4173/api/v1/geography/states/TX/counties`.

In the dashboard header, click **Demo API connected** to inspect data availability and navigate to the explorer.

## Tests

```powershell
npm test
npm run check
```

The tests cover read-only API operation, explicit demo labeling, race filtering, historical empty states, polls/exit unavailable states, untrained model abstention, geographic reference counts, input validation and the SSE handshake. All require only Node.js.

## Source tree

```text
election-signal-phase0/
  Start-All.ps1 / Stop-All.ps1     # Windows entrypoints
  package.json                    # node built-in server, zero dependencies
  src/server.mjs                  # local API + static HTTP server
  web/index.html                  # approved V7 clickable UI
  web/assets/app-host.js          # API connection/status panel
  web/api-explorer.html           # interactive working API explorer
  data/demo/                     # explicitly fictional fixtures
  data/geography/                # reference state/county shapes, NOT 2026 districts
  tests/                         # node:test API tests
  scripts/                       # Windows process scripts
  docs/                           # contracts, architecture, safety, roadmap, Codex prompts
  reference/                      # original V7 UI and preview screenshots
```

## Scope matrix

| Capability | Phase 0 state |
|---|---|
| Interactive approved V7 UI | Working in browser; browser fixture data |
| Four global data modes | Working as UI navigation and filters; data not yet imported |
| US state and reference county map interactions | Working prototype UI; backend reference geometry endpoint |
| Native local server, health, read-only APIs | Working Node standard-library implementation |
| Live 2026 vote ingestion | **NOT CONNECTED** |
| Verified historical election data 2000–2024 | **NOT IMPORTED** |
| Published opinion polls or outlet forecasts | **NOT CONNECTED** |
| Licensed exit poll data | **NOT CONNECTED** |
| Real-time statistically validated win probabilities | **NOT TRAINED / UNAVAILABLE** |
| Spring Boot / PostgreSQL / Python services | **Planned, not implemented** |

**Important technical boundary:** The V7 interface is still a large self-contained HTML/CSS/JS prototype driven by **in-browser demo arrays**, while the Node API exposes separately stored sample fixtures. Those two datasets are not synchronized by design in Phase 0. Codex Phase 1 explicitly migrates the UI to typed React components and a unified API-based data store, replacing this temporary dual fixture approach. Do not present the local demo server as live election infrastructure.

## Recommended next steps

1. Read `AGENTS.md`, `docs/PHASE0_COMPLETION.md`, `docs/ARCHITECTURE.md`, `docs/DATA_CONTRACTS.md`, `docs/DATA_SOURCES.md`, and `docs/CODEX_MASTER_PROMPT.md`.
2. Give Codex the **exact prompt** in `docs/codex/PHASE_1_FOUNDATION.md`, then review before starting Phase 2.
3. Subsequent phases have separate Codex prompts and acceptance gates; do not implement everything in one giant unchecked change.

No unpublished election result, provider credential, unlicensed poll data or speculative model probability is shipped in this archive.
