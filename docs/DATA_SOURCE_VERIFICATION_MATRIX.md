# Data source verification matrix

Verification date: 2026-11-02. Individual probe times were not captured. Status refers to the work on this branch and is not an assertion of permission to republish.

| Source | Evidence observed | Status and limitations |
| --- | --- | --- |
| [North Carolina official download page](https://er.ncsbe.gov/downloads.html?election_dt=11%2F03%2F2026) and [precinct layout](https://dl.ncsbe.gov/ENRS/layout_results_pct.txt) | Its published 2026 precinct ZIP returned HTTP 200 with 141,466 rows covering 100 county names; all reported totals were zero during the pre-election probe. The package was inspected in memory and was not retained. | SCHEMA_VERIFIED for the observed pre-election file; storage, display, and API reuse rights remain unverified; NOT_CONNECTED. The parser does not infer county FIPS, district assignments, reporting percentages or election calls. Election-night changes and real counting have not been verified. |
| [Arizona Secretary of State](https://azsos.gov/press), [JSON guide](https://azsos.gov/sites/default/files/docs/Arizona%20Media%20JSON%20Guide.pdf) | Official guide describes media JSON. The linked mock file returned HTTP 404; the press page specifies a minimum two-minute download interval. | DISCOVERED; schema-only parser is tested with fictional inputs, not a real payload. Rights and live behavior unverified; NOT_CONNECTED. |
| [Florida Election Watch downloads](https://floridaelectionwatch.gov/Downloads) | HTTP 403 from this environment. | BLOCKED; no bypass attempted, schema and rights unverified. |
| [Open America API](https://openamerica.io/elections/api/) | A 2024 Senate request returned 11 races; the 2026 Senate request returned no races. The 2024 response was stale and is not an election-night replay. | BLOCKED for republication: the provider asks users to obtain permission before repackaging. No provider data is archived or published by this work; no contact has been sent. |
| [civicAPI](https://civicapi.org/api-documentation) | Documentation located; exact 2026 results routes, lineage and coverage were not verified. | DISCOVERED; NOT_CONNECTED. |

The North Carolina package is a publicly reachable pre-election file, not proof that votes are being reported. A zero is not evidence of a completed count. No nationwide live election source is enabled by this work.
