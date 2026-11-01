# CODEX PHASE 3 — Live-updating opinion poll registry and published forecasts

Read `AGENTS.md`, `docs/CODEX_MASTER_PROMPT.md`, `docs/DATA_SOURCES.md`, `docs/DATA_CONTRACTS.md`. Implement only Phase 3 on top of approved earlier phases.

## Objective

Turn the **Polls & Forecasts** global lens into a reliable multi-outlet directory of current published polls, polling averages and **separately labeled** third-party forecasts. No unlicensed scraping and no assumption that opinion-poll percentages are chances of winning.

## Detailed steps

1. Research candidate providers and **confirm access policy, endpoint availability, field scope, geography/office coverage, pricing, commercial reuse rights, attribution and redistributability**. Document provider decisions; do not guess endpoint contracts. If none can be legally integrated, build a manual original-survey upload for licensed data and keep the provider disconnected.
2. Implement `PollingSourceAdapter` and `PublishedForecastAdapter` interfaces, credential settings in `.env` (excluded from git), source health, conditional fetch, polling interval constrained to source limits and exponential backoff on 429/5xx.
3. Model individual poll: pollster, sponsor, field start/end, publication date, likely/registered/adult population, sample N, methodology, margin of error, questionnaire variant, office, candidate choices, poll percentages, original URL, license metadata, provider revision and fetched timestamp.
4. Deduplicate identical underlying surveys republished by different outlets. Keep their citations but do **not** treat a copy as an independent survey. Preserve different ballot-test versions with precise question wording.
5. Keep **individual polls**, **polling averages**, and **published model forecasts** distinct tables and UI tabs, with provider names and last publication time. Show separate elected office and district scope. A statewide poll never becomes House District 07's poll.
6. Preserve the selected outlet/provider and survey/forecast view across national, state, office and race navigation, with no vote-results contamination.
7. Track source freshness and stale/embargo/unavailable indicators. Add job health dashboard and non-destructive reprocessing of changed/corrected polls.
8. Test with permission-cleared archived responses: pagination, duplicate questionnaires, samples, source dates and data mapping. Do not enable unverified network access automatically.

## Acceptance

- Poll entry can be traced to original publication and fieldwork dates; summary never hides survey population.
- A published forecast is explicitly attributed with model/version/time, not described as an internal result.
- Switching to Live Results shows only counted ballots (not polling figures), and back returns same outlet filter.
- Unknown source terms => disabled adapter, explicit NOT_CONNECTED.
- Tests prove duplicate republishing is not double counted.

Deliver code, provider rights inventory, fixtures, API docs and `docs/PHASE_3_REPORT.md`. Stop for review.
