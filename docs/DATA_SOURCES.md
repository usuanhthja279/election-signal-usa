# Data Source Research and Provider Integration Checklist

Research notes as of **2026-11-01**, not a claim that any provider is already connected. Every provider needs coverage validation, terms of use/license confirmation, schema inspection and a safe import test before publication.

## Live results

1. **Associated Press Elections API**: https://developer.ap.org/ap-elections-api/index.html — commercial authenticated provider; reference API documents vote-count changes, race calls and an incremental-update workflow. Authentication: https://developer.ap.org/ap-elections-api/docs/Getting_Started.htm. Reporting field reference: https://developer.ap.org/ap-elections-api/docs/Elections_Response_Data_Elements.htm. API keys and commercial terms required.
2. **Official state/local election administrators**: FEC explains administration and certification are decentralized: https://www.fec.gov/introduction-campaign-finance/election-results-and-voting-information/. State sites have different files/formats, may not offer a unified machine API.
3. **Alternate commercial feed**: DDHQ or others subject to legal access, verified data granularity, call policy and redistribution rights.
4. **Free/community providers**: may be evaluated experimentally (e.g. civicAPI/Open America). Do not treat nationwide completeness, county-level availability, refresh frequency or licensing as guaranteed until verified with real 2026 responses and explicit terms.

## Historical 2000–2024 results

- MIT Election Lab: https://electionlab.mit.edu/data. Datasets vary by year, office and granularity. Each requires checks for missing or uncontested races, verified source, license and downloadable schema.
- MIT 2024 official precinct data: https://github.com/MEDSL/2024-elections-official. Large datasets should be downloaded once to local staging, checked, then imported; not repeatedly fetched in public HTTP requests.
- FEC final federal results: https://www.fec.gov/introduction-campaign-finance/election-results-and-voting-information/ for reference/cross-check.
- Census TIGERweb boundary services: https://tigerweb.geo.census.gov/tigerwebmain/TIGERweb_main.html. Need exact 2026 election-appropriate congressional districts; don't reuse a historical district shape without verification.

## Opinion polls and forecasts

- Individual pollster publications are authoritative for survey field dates, sampled population (LV/RV/adults), method, sponsor and sample N.
- Poll aggregators/outlets (DDHQ, Polling Forecast, others) may list poll records or published forecasts. Investigate public APIs, licensing, deduplication and allowed redistribution; don't scrape websites contrary to terms.
- FiveThirtyEight historical data repository: https://github.com/fivethirtyeight/data — check if the particular dataset exists and covers the requested years before importing.
- Published forecasts must retain provider model name/version/as-of and not be confused with original survey percentages.

## Exit polls / voter surveys

- AP VoteCast / voter survey commercial data: https://www.ap.org/elections/solutions/services/ap-vote-count/.
- Roper archived exit polls: https://ropercenter.cornell.edu/exit-polls/us-national-election-day-exit-polls.
- Special licensing, embargo, weighting, geography, questionnaire and suppression handling. Exit polls are surveys, not official counted votes; county-level cross tabs may not exist.

## Every adapter must record

`provider_id`, `feed_type`, `source_url`, `license_id`, `legal_review_status`, `allowed_redistribution`, `credentials_ref` (never credential value), `enabled`, `poll_interval`, `coverage_offices`, `coverage_geographies`, `rate_limits`, `last_success`, `last_error`, `as_of`, `fetched_at`, `schema_version`.

## Source acceptance gate

Before a source is enabled, retain documented evidence of: authenticated/anonymous access actually working; source data not being a rehearsal/test fixture; accurate vote totals relative to official samples; county/district granularity; field definitions for reporting percentage; license restrictions; rate limits; call/certification semantics; resilience to negative revisions; and replay capability. If any gate is missing, leave it disabled and show a truthful empty/limited-coverage status.
