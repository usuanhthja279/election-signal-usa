# Phase 2 prototype update

`web/index.html` now layers Phase 2 source and registry statuses onto the approved V7 visual design. The dark shell, map framing, cards, global mode tabs and responsive layout remain in use. An exact pre-update copy is preserved at `reference/prototypes/election_signal_v7_approved.html`; reference screenshots were not changed.

The added `web/phase2-layer.js` prototype layer provides URL-backed mode/year/office/state/provider/poll-view state, reload and browser history handling, neutral reference geography when there are no verified results, and explicit `NOT_IMPORTED`, `NOT_CONNECTED`, `NOT_PUBLISHED`, `UNAVAILABLE`, `REFERENCE_ONLY`, and `MODEL_UNAVAILABLE` states. House district selection is not offered without verified registry metadata. The national Overview does not show model probabilities, and individual-race pages display the model as unavailable.

Every demo fixture in the original V7 Live Results screens remains visibly identified as fictional. No provider, historical result, race registry, survey, 2026 boundary, or reporting measure is represented as verified or connected. Historical county maps and modeled sample percentages have been replaced in the Phase 2 lens views by neutral geometry and unavailable states.

## Review routes

Run the existing Phase 0 prototype server with `Start-All.ps1` and open `http://127.0.0.1:4173/`. The ordinary URL opens the V7-inspired Live Results design with the Phase 2 status strip. The other global contexts can be reviewed with:

- `/?mode=history&year=2022&office=senate`
- `/?mode=polls&year=2026&office=house&pollView=surveys`
- `/?mode=exit&year=2026&office=governors`
- `/?mode=live&year=2000&office=house&state=TX&page=race` (year normalizes to 2026; unsupported district context is discarded)

The updated prototype is a static review artifact, not a connected Phase 2 application. `node --check web/phase2-layer.js` and `node --check` for the V7 inline JavaScript block passed. `npm run check` passed (11/11). Installed Chrome rendered desktop and mobile screens with no browser runtime errors and no horizontal overflow at 390 px. Browser exercises covered history deep links, refresh, invalid district removal, live-year normalization to 2026, office switching while retaining state/year, poll-view selection, Back navigation, and keyboard activation of a map state while retaining the selected year.

Review captures (new files; the V7 reference screenshots are unchanged):

- [Live Overview at 1440 px](screenshots/phase2-prototype/overview-1440.png)
- [History 2022 at 1440 px](screenshots/phase2-prototype/history-2022-1440.png)
- [Polls at 390 px](screenshots/phase2-prototype/polls-390.png)

These checks confirm the prototype interactions and layout at the captured sizes; they do not certify full V7 parity or source/data integration.

## V14 React application adaptation

The attached `web/election_signal_v14_house_county_package.zip` contains a complete multi-view prototype, not only the county screen. Its shared Election Signal / The Desk shell and editorial visual system now carry across the React application's national, office, state, race, county, historical, polls, exit-poll, and source views. The four global mode tabs, status notice, compact navigation rail, keyboard state finder, theme switch, page title, filters, maps, cards, and tables remain inside the same application shell.

| V14 prototype view | React application view |
| --- | --- |
| National live overview | `NationalOverview` |
| Office desk | `NationalOfficePage` |
| State dashboard | `StateElectionDashboard` |
| Senate/Governor race and House district detail | `RaceDetailView` / `HouseDistrictPage` |
| Historical overview and state results | `HistoryDashboard` and mode-aware state dashboard |
| Polls and exit surveys | Mode-aware overview, filters, and unavailable status panels |
| County explorer | County map and county list within the selected state |
| County × district intersection preview | `HouseCountyDistrictPreview` (schematic only) |
| Sources and methodology | `SourceDashboard` and source status panel |

The V14 HTML's replay controls are intentionally not represented as a live feed. The React app retains no connected provider or validated time-series feed; fixture-based replay must remain visibly fictional. County-section polygons in the supplied prototype remain schematic and do not name official districts, divide county votes, or claim 2026 geography.

The frontend canonicalizes the mode, year, office, state, district, provider, and poll view in the URL before scoped requests. It now also withholds district-scoped requests until the selected district is confirmed by registry metadata. Direct House county selection uses a semantic deep link and opens the inspector immediately. This establishes UI seams for later work while leaving Phase 3 polling/forecast integrations unimplemented.

New V14 review captures are separate from the original V7 reference screenshots:

- [Live overview · desktop 1440 px](screenshots/phase2/desktop-live.png)
- [House county/district inspector · desktop 1440 px](screenshots/phase2/desktop-v14-county-districts.png)
- [House county/district inspector · mobile 390 px](screenshots/phase2/mobile-v14-county-districts.png)

Focused Playwright review (PowerShell): `$env:ELECTION_E2E_PORT='3001'; $env:CI='1'; npm run test:e2e -- --workers=1 --reporter=line --grep 'V14|House county'` passed 6/6 across desktop (1440 px), tablet (768 px), and mobile (390 px). A broader run exercised 27 non-skipped tests across the same three viewports; 26 passed, and one existing demo-label assertion needed a more precise locator after the V14 banner added adjacent status text. The corrected global-mode and demo-label flows then passed 6/6 across those viewports. The three PostgreSQL-only local-profile tests are skipped unless `ELECTION_E2E_LOCAL=1`. These UI checks do not constitute Phase 2 sign-off.
