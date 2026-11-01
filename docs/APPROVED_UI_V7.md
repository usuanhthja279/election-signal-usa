# Election Signal — Phase 0 V7 UI Specification

**Scope:** reviewable offline HTML prototype. The production app is not implemented. All red/blue historical vote shares, 2026 vote counts, chances, results, and candidate fixtures inside the HTML are synthetic. Geographic outlines are historical reference data and need 2026 boundary verification.

## Product decision: one global data context

The header carries four mutually exclusive lenses:

1. **Live Results** — latest *reported ballots* only on Overview; drill-down to per-race statistical win-chance model (once validated), separately labeled from results.
2. **Past Results** — verified historic results for years 2000, 2002, ... 2024. Year selection persists across country, office, state, county, and race pages. Certain offices/states/years have no election or no accessible data.
3. **Polls & Forecasts** — 2026 pre-election opinion surveys and distinct *published forecasts*. Provider choice persists across pages, and any forecast must retain provider attribution and model timestamp. Distinguish primary survey, aggregation, and forecast. No ballot counting is associated with polling.
4. **Exit Polls** — Election Day/verified-voter survey results. Distinct from pre-election polls and actual election returns. 2026 should display “Not yet published” until a permitted release is available. Historical exit polls available subject to restrictions.

**Global state future production URL contract:** `?mode=live|history|polls|exit&year=YYYY&office=house|senate|governors&state=TX&district=07&provider=ddhq&view=surveys|forecasts`.

### Routing

| From | Action | Result |
|---|---|---|
| Live Overview | Click a state | State dashboard showing all scheduled election offices; reported votes, source times, no forecasts on national Overview |
| Historical Overview | Choose 2004, click a state | State's **2004** data, not 2026 |
| Historical state | Open a county | Historical county data for the same year and office, where valid |
| Polls Overview | Select outlet, click a state | State polling/forecast records filtered to same outlet and view type |
| Polls House | Open a district | Only polling for that exact district; no extrapolated state survey to districts |
| Exit Polls | Click state | Actual available state-level survey findings, not county voting totals |
| Any lens | Switch House/Senate/Governors | Preserve lens, year and outlet; update office-specific availability |
| Any lens | Switch global lens | Preserve state/office where meaningful, prevent invalid county survey views |

### Main-page visual layout

- Dark navy election terminal, left navigation, bright primary surface and four segmented global tabs in sticky header.
- Summary KPIs reflect **only** the selected lens. No stale live-results cards in historical/polling/exit views.
- Map: clickable real state boundaries. In historical mode, map shading illustrates imported verified results; polling and exit modes use neutral geography unless actual source-defined coverage is supplied.
- Office selector and state explorer: contextual data, same active lens.
- Historical county maps: current reference shapes are demonstrational only; implement versioned polygon boundaries and district crosswalks for real history.
- Forecasting stays off national Live Overview. Individual Senate/Governor/House race pages have clearly marked reported totals, model estimate, uncertainty, provenance, run time, as-of input time.

## Historical data requirements

- Elections 2000–2024 by 2-year increments for federal legislative offices. President occurs every 4 years, governors and senators are state-specific cycles.
- MIT Election Lab is a high-value source for historical House, Senate, precinct and county data: https://electionlab.mit.edu/data
- Election Data API at https://electiondata.dev/datasets/ exposes 2000–2024 county presidential, historical district House and statewide Senate data. Confirm terms and data completeness before reuse.
- County lines, precinct boundaries and congressional districts change; data and map versions must not be joined solely by textual county name.
- Keep unavailable vs uncontested vs not-on-ballot separate. Third party/independents must remain in candidate results.

## Poll source candidates and validation

1. Polling Forecast (https://www.pollingforecast.com/us/data): published `/api/us/polls` (race, pollster, kind, since) and `/api/us/{house|senate|governors}` sources, row-level license metadata. Check origin, freshness and whether the permission actually covers your use.
2. DDHQ 2026 polling averages and documentation (https://decisiondeskhq.substack.com/p/2026-election-polling-averages): verify authenticated API agreement and allowed display; do not guess credentials or scrape hidden endpoints.
3. FiveThirtyEight published CSV repository (https://github.com/fivethirtyeight/data/blob/master/polls/README.md): useful historical archive; verify that each current CSV actually contains 2026 before using.
4. RealClearPolling (https://www.realclearpolling.com/): human-readable listings; separate permissions for automated ingestion or redistribution.
5. Pollster-origin releases: primary cross-check source for sponsor, field dates and methodology.

**Poll normalization contract:**

```
poll_id, provider, provider_record_id, original_url, published_at,
field_start, field_end, pollster, sponsor, state, office, district,
race_id, population (LV/RV/adults), sample_size, methodology,
margin_of_error, response_options[], candidate_shares[],
ballot_test_variant, source_revision, license, fetched_at
```

**Deduplication:** one underlying survey can appear at several aggregators; deduplicate by pollster, sponsor, field dates, sample, population, questionnaire/ballot test; preserve citations to all aggregators without treating them as independent polls. Poll percentages do not equal win probabilities. Published models carry a separate model-output schema.

**Refresh:** poll for new records only as permitted by each provider; begin with a periodic job (e.g. 30–60 minutes for licensed APIs, subject to terms), HTTP conditional requests when supported, backoff on rate limiting, and manual importer for original releases. Separate `fetched_at`, `original_published_at`, and fieldwork dates. Trigger UI events only on real changes.

## Exit poll source requirements

- AP The Voter Poll / AP VoteCast: https://www.ap.org/elections/solutions/services/ap-vote-count/ ; https://developer.ap.org/ap-elections-api/docs/AP_VoteCast.htm
- Roper archive: https://ropercenter.cornell.edu/exit-polls/us-national-election-day-exit-polls
- Live 2026 release contingent on actual election, subscription rights and embargo restrictions. Do not show fabricated results or advertise coverage that isn't available.
- Store survey methodology, population, questions, weighting, subgroup sample N, release time and permissions; respect sparse crosstabs and suppression policies.
- Do not convert survey crosstabs into county-by-county vote estimates.

## Live modeling references

- Primary 2026 modeled historical context: 2022 midterm and 2024 general election results. For Senate contests, prior same-seat 2020 result may add necessary comparable office history.
- Separate county observed margin vs historical baseline from estimated **remaining ballots**; account for differing ballot types, count order, missing counties and boundary changes.
- Do not report win chance without a calibrated, validated statistical model and sufficient live data. The Phase 0 probability panel is synthetic.

## Cross-view API contract (future)

```
GET /api/v1/context?mode=live|history|polls|exit&year=YYYY&office=...
GET /api/v1/history/races?year=2022&office=senate&state=TX
GET /api/v1/history/counties?year=2024&office=president&state=PA
GET /api/v1/polls?cycle=2026&office=senate&state=TX&provider=...
GET /api/v1/polls/metadata/sources
GET /api/v1/forecasts/published?cycle=2026&raceId=...
GET /api/v1/exit-polls?cycle=2026&state=TX&office=governors
GET /api/v1/live/races?cycle=2026&office=house&state=TX
GET /api/v1/live/models?raceId=...  # validated only
GET /api/v1/sources/health
```

Responses contain `data_mode`, `data_status`, `provider`, `source_as_of`, `fetched_at`, `license`, `attribution`, `coverage_scope`, and `is_test_data`.

## Acceptance tests

- Header tabs work on desktop and mobile and update every page's data context.
- Changing historic year to 2000 persists after opening TX, changing office, opening county, and returning to map.
- Polling view retains source and poll/forecast choice when selecting state and district.
- Exit Polls 2026 page displays unavailable before publication, no fictitious 2026 survey findings.
- No county polls created when source only has statewide survey.
- Real recorded results, historic results, poll opinions, exit poll crosstabs, outlet forecasts and own model output are distinct types.
- History can render boundaries for relevant historical year and guard against redistricting mixing.
- House district detail is separate from county totals.
- All survey records link to original pollster or poll publisher and have dates and population.
- No Docker. Windows PowerShell launcher later starts Next.js, Spring Boot, Python forecast service and local PostgreSQL.

## Status of supplied V7 HTML

Fully interactive **UI concept only**. No network calls, actual historical election returns, current polls, exit poll data or live forecasts have been imported. JS state is for design review; production should use URL-backed state and proper TypeScript components.
