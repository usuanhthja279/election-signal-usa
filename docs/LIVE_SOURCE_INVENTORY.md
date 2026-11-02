# Live source inventory

Status: **NOT_CONNECTED**. No results provider has passed endpoint, terms, response-schema and adapter verification. The source registry contains disabled candidates only.

| Candidate | Investigation result | Use status |
|---|---|---|
| civicAPI | Public documentation describes election APIs; exact election-cycle endpoints, response fixture, state coverage and redistribution terms were not validated in this repository | Disabled; no adapter |
| State election offices | Jurisdiction-specific systems and terms vary; no uniform verified feed collected | Not connected |
| AP/commercial providers | Requires contract/account and explicit reuse rights | Disabled; no credentials |
| Public historical catalogs | Candidate inventory only; not live results | Not connected |

No source is represented as covering every county or district. Provider polling must honor documented limits and rights before enabling. No scraping or restricted access is used.

## 2026 California official feed discovery

California's Secretary of State lists [2026 election media files](https://media.sos.ca.gov/), including documented JSON and API endpoint inventories. On 2026-11-02 the official `https://media.sos.ca.gov/media/governor.json` returned a JSON response containing candidate entries and zero precincts reported. This is before election night; zeros are not a certification, call, demonstrated live update, or authorization to treat the feed as production-verified. Official terms, timestamp/time-zone interpretation, expected county/office scope, source corrections, identity mapping, rate limits and election-night behavior still need verification before a live adapter can be enabled. The existing live source remains NOT_CONNECTED. No test or rehearsal records are imported.

The read-only `scripts/Test-CaliforniaResultsSource.ps1` now fetches the official Governor JSON with a timeout and size bound and checks its statewide shape. A successful 2026-11-02 run returned `NOT_REPORTING`, zero precincts reporting and the provider's timestamp as unparsed text. It intentionally does not archive or expose candidate vote values: identity mapping, rights review, timestamp semantics and an actual election-night update are not yet verified. `CaliforniaResultsPreflightTests` exercises fictional zero, positive and malformed responses; no fictional data are submitted to an observation table. This is a source preflight, not an enabled results collector.

California's [Election Night Data-Feed Guide](https://cms.cdn.sos.ca.gov/media/1-media-guide-v19.pdf) describes media-client sign-up for its election-night feed. Confirm whether registration applies to the public JSON endpoints before enabling a collector; do not assume that public pre-election reachability grants unattended election-night access. California notes that election-night results are unofficial until the official canvass. The source preflight remains manual and does not collect vote totals.
