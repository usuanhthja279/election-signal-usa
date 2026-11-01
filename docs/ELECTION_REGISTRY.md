# 2026 election registry

The local API provides registry, states, races, state-races and House districts endpoints under `/api/v1/elections/2026`. Schema supports regular/special/runoff subtype, ballot status, stable race identifiers, Senate seat class, state/jurisdiction, district and provider mappings. Empty races and districts return `NOT_IMPORTED`; seeded state jurisdiction codes do not claim ballot eligibility. District identifiers are strings to preserve leading zeroes.

`scripts/Import-ElectionRegistry.ps1` validates and stages explicitly reviewed CSV input. It expects a full set of 435 voting House districts and verifies jurisdiction/district identifiers; it does not automatically publish a source file. No authority-derived 2026 race registry is currently imported. API limitations and current counts are returned as metadata. Do not infer statewide House contests or county-to-district results.
