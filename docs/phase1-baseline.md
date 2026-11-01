# Phase 1 baseline and acceptance checklist

The Phase 0 app and approved `web/index.html` (V7) remain the visual/interaction reference. `Start-All.ps1` continues to launch Phase 0. The React preview and reference screenshots are separate. The baseline audit and completion evidence are documented in [PHASE_1_REPORT.md](PHASE_1_REPORT.md).

Before edits on 2026-11-01: frontend typecheck and six unit checks passed; initial local-server test attempt was denied by sandbox socket permissions, then the approved project workflow passed 11/11 Phase 0 tests. Java 21.0.13 and PostgreSQL 17.11 project binaries existed in `.runtime`. Existing React parity did not cover all V7 navigational screens. No repo git metadata is present in this workspace, so a Git diff/status could not be used as the change ledger.

| V7 screen / behavior | Current checklist |
| --- | --- |
| National Overview | State hover/focus snapshot, map and list selection, no national model probability. Demo values visibly fictional. Tested desktop/mobile. |
| National office pages | House/Senate/Governors selection and state selection retain office. Tested Senate-to-Texas flow. |
| State dashboard | All-election entry, office cards, district selector, reference county navigation. Not-on-ballot / unavailable states are distinct from zero. |
| Race / district | Individual state/office/district route and model-unavailable panel. No county-to-House attribution. |
| County explorer | Reference map, keyboard/pointer selection, search/list, URL selection; no verified result coloring or real result percentages. |
| Past Results | Even years 2000–2024; year stays on state, office and district paths, reload and browser navigation. Actual results are not imported. |
| Polls & Forecasts | Provider and survey/forecast filters persist in URL and API request scope. No licensed provider data. |
| Exit Polls | Separate context/year and unavailable data state. No authorized survey data. |
| Sources / methodology | Explicit disconnected source and interpretation status. |
| Mobile / accessibility | 390px, 768px and desktop 1440px Playwright paths; keyboard tabs/map/county and responsive drawer. Dedicated screen-reader audit and exhaustive state matrix remain open. |
| Local API / database | Separate demo/local profile; fresh disposable PG17.11 and Flyway V1/V2 verified; local profile never loads demo fixtures. |

The current work was compared visually with `reference/screenshots/`. It is a functional structural match, not a pixel-diff pass. V7's fictional aggregate counts and result-colored county map are deliberately not presented as verified facts. See the report for screenshots, exact commands, results and remaining gaps. Phase 1 is presented for review; do not proceed into Phase 2 until the user reviews it.
