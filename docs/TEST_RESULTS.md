# Phase 0 Automated Test Evidence

**Environment used for development verification:** Linux container, Node.js 22.16.0, Chromium headless, Python Playwright. Windows PowerShell launchers are provided but could not be executed on Windows in this environment.

## Node API test suite

Command: `npm run check` (runs Node syntax checks + built-in `node --test`).

Results: **11 tests passed, 0 failed**.

Tests covered:

- UI HTML and host script are served.
- Health explicitly declares DEMO and disabled providers/model.
- Demo race filtering by state and office.
- Historical 2000 request returns NOT_IMPORTED, not fabricated votes.
- Unsupported election years rejected.
- Polls, forecasts and exit polls unavailable states.
- Model abstains with null probabilities.
- Reference geography includes 51 states+DC and 254 TX counties, explicitly not verified for 2026 districts.
- Invalid state/office/mode rejected.
- Static file and API missing-route protections.
- SSE status handshake says results stream is not connected.

## Chromium V7 smoke test

Loaded the actual `web/index.html` markup in a headless browser and inserted `web/assets/app-host.js` via script content (browser sandbox prevented direct local-HTTP browsing). Verified:

- JavaScript runtime errors: **0**.
- Four global mode tab controls present.
- Switching **History → Polls → Exit Polls → Live** updated the actual in-page state to the corresponding mode.
- Local-host status button and status modal rendered and opened.

**Limitation:** Chromium test did not validate the Windows PowerShell scripts or the browser's actual same-origin network request to the Node API. Node HTTP integration tests independently exercised all implemented endpoints.

## Data audit

- No real candidate names, licensed poll files, source credentials, verified historical returns or trained model parameters shipped.
- Demo JSON intentionally uses fictional candidate names and vote totals.
- Every API response for fixture data marks `isTestData: true`.
- `web/index.html` was left as the approved V7 visual reference; it contains its own synthetic browser examples, distinctly bannered.
