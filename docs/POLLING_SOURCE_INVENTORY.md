# Phase 3 polling source inventory

Verified 2026-11-02. Verification means the listed documentation or endpoint was reachable during this review; it does not mean a provider is integrated or enabled.

| Provider | Documented endpoint / access | Coverage and cadence | Rights / attribution | Status |
|---|---|---|---|---|
| [Polling Forecast](https://www.pollingforecast.com/us/data) | `/api/us/polls`, `/api/us/house`, `/api/us/senate`, `/api/us/governors`, `/api/us/race/{key}`; documentation says no key | US 2026 polls and provider forecasts; site documents 10-minute cache and 120 requests/minute/IP with 429 `Retry-After` | Its own forecast output is described as CC BY 4.0 with attribution; poll figures are separately attributed to pollsters, and some rows are described as CC BY-SA 4.0. Row rights must be reviewed individually. | Documentation and JSON response observed via web retrieval; no application adapter is enabled and no rows imported. Original publication dates may be absent. |
| [Decision Desk HQ](https://polling.decisiondeskhq.com/docs) | Public API documentation describes 2024 polling endpoints; [current API documentation](https://docs.decisiondeskhq.com/reference/get_api-v4-polls-base-data-pollsters) and [pricing](https://votes.decisiondeskhq.com/pricing) describe access options | Current 2026 coverage, cadence and rate limits for an authorized account have not been verified | API access, commercial reuse and redistribution require separate agreement; no credentials or rights supplied | NOT_CONNECTED; no adapter enabled. |

No access terms or figures are inferred from public reachability. Review the current license, attribution and commercial terms again before publication. Never treat the umbrella forecast license as permission to republish third-party poll responses.
