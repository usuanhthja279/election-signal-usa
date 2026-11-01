# API and Canonical Data Contracts

## Conventions

- Read-only under `/api/v1/` during Phase 0; JSON responses always have explicit `dataMode`, `isTestData`, `status` and `source` when representing data.
- Typed `data_mode`/`dataMode`: `LIVE_REPORTED`, `HISTORICAL_CERTIFIED`, `POLL`, `PUBLISHED_FORECAST`, `EXIT_SURVEY`, `INTERNAL_MODEL`, `REFERENCE_GEOGRAPHY`, `DEMO`. Reference geography does not represent reported results.
- `sourceAsOf` (upstream observation time), `fetchedAt` (ingestion time), and `publishedAt` (original publication) are not interchangeable.
- ISO-8601 UTC for stored instants; display local timezone with a selector and label.
- Null means unknown or unavailable; never substitute 0 or an invented rate. Amounts include unit definitions.

## Working endpoints (already implemented)

| Method / route | Current behavior |
|---|---|
| GET `/api/v1/health` | Status, demo mode and all provider/model connection flags |
| GET `/api/v1/meta` | Modes, election cycle, historical years |
| GET `/api/v1/sources/health` | Source registry, disconnected by default |
| GET `/api/v1/context?mode=history&year=2022&office=senate&state=TX` | Validated global context and availability |
| GET `/api/v1/demo/races?office=house&state=TX` | Fictional candidate totals |
| GET `/api/v1/demo/races/TX-HOUSE-07-2026-DEMO` | One fictional race |
| GET `/api/v1/demo/states/TX/summary` | Fictional races for a state |
| GET `/api/v1/geography/manifest` | Geographic reference counts and caveats |
| GET `/api/v1/geography/states/TX/counties` | Reference county shapes; geometry not production verified |
| GET `/api/v1/history/results?year=2000` | `NOT_IMPORTED`, empty array |
| GET `/api/v1/polls` | `NOT_CONNECTED`, empty array |
| GET `/api/v1/forecasts/published` | `NOT_CONNECTED`, empty array |
| GET `/api/v1/exit-polls` | `NOT_PUBLISHED`, empty array |
| GET `/api/v1/models/races/TX-SEN-2026-DEMO` | `UNAVAILABLE_NOT_TRAINED`, null probabilities |
| GET `/api/v1/events` | SSE handshake + keepalive; explicitly not a vote stream |

All API endpoints are explorable through `/api-explorer.html`.

## Phase 1+ normalized race contract

```json
{
  "raceId":"US-2026-SEN-TX-REGULAR",
  "electionId":"US-2026-GENERAL",
  "office":"SENATE",
  "state":"TX",
  "district":null,
  "electionType":"REGULAR",
  "status":"COUNTING",
  "source":{"providerId":"publisher-id","sourceRaceId":"vendor-race-id","asOf":"2026-11-04T01:00:00Z","ingestedAt":"2026-11-04T01:00:03Z","licenseId":"rights-record"},
  "reporting":{"kind":"ESTIMATED_VOTE_PERCENT","value":72.4,"isEstimate":true},
  "candidates":[{"candidateId":"canonical-candidate-id","displayName":"Name from verified feed","party":"DEM","votes":null}],
  "currentLeaderCandidateId":null,
  "publishedCall":null,
  "certification":null,
  "isTestData":false
}
```

**Important:** This JSON is a *shape example*, not an actual 2026 result; candidate votes are null and all timestamp values are illustrative. The server may omit unavailable optional fields, but it must not replace null with zero.

## Distinct data types

**Historical result**: `{year,office,state,geoId,geographyVersion,candidateId,party,votes,certifyingAuthority,sourceUrl,dataVersion}`. Support year 2000–2024, with no-contest and missing-source as separate statuses. For House, store district boundaries per year.

**Opinion poll**: `{pollId,pollster,sponsor,fieldStart,fieldEnd,publishedAt,sampleN,population:LV|RV|ADULTS,methodology,office,state,district,questionVariant,candidateShares,sourceUrl,licenseId}`. Never equate a poll share to win probability; prevent duplicate surveys from multiple outlets.

**Published forecast**: `{provider,modelName,modelVersion,asOf,raceId,probabilityDistribution,uncertaintyMethod,sourceUrl,licenseId}`. Only display if a valid license permits republication and the outcome is actually published.

**Exit-poll/voter survey**: `{provider,surveyName,fieldDates,population,questionText,subgroup,sampleN,weightingMethod,responseShares,suppressedCells,releaseTime,sourceUrl,licenseId}`. No inference of county vote count from exit survey.

**Internal live model**: `{raceId,modelVersion,runId,asOfInput,runAt,status:VALIDATED|ABSTAINED,expectedFinalVoteDistribution,winProbabilityDistribution,calibrationVersion,uncertaintyInterval,explanations,missingInputs,sourceRevisionIds}`. Suppress probabilities when source or model quality gates fail.

## API error patterns

- HTTP 400 `INVALID_PARAMETER` for unknown mode/state/office or year outside 2000–2024 historical years.
- HTTP 404 `NOT_FOUND` for nonexistent race IDs.
- Normal HTTP 200 `NOT_IMPORTED`, `NOT_CONNECTED`, `NOT_PUBLISHED`, `UNAVAILABLE_NOT_TRAINED` for valid endpoints whose external data simply do not exist yet.
- HTTP 503 `SOURCE_STALE` / `SOURCE_UNAVAILABLE` can be used in future for provider outages, but retain the last snapshot and its timestamp separately; never replace old count with fabricated current numbers.

## Production transport

Provide REST `/api/v1/...` for snapshots and SSE for incremental updates. An SSE payload should have stable `eventId`, `raceId`, `sourceRevisionId`, `eventType`, `dataMode`, `emittedAt`, plus a minimal affected-resource patch. The client should fetch a full snapshot on reconnect when replay cursor is unavailable.
