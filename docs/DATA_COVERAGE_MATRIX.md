# Data coverage matrix

This records verified scope, not a guarantee of completeness. Existing historical and forecast imports are described in the Phase 3 report; this branch does not add new historical records, survey datasets, or boundary files.

| Area | Status | Limitation |
| --- | --- | --- |
| 2026 live, North Carolina | Pre-election source schema observed; NOT_CONNECTED | Source rights, mappings, archival delivery and election-night validation remain pending; all totals in the observed file were zero. |
| 2026 live, Arizona | Parser built from official documentation; NOT_CONNECTED | Published mock unavailable; actual response and permitted use must be verified. |
| 2026 live, Florida and other states | NOT_CONNECTED | Source access, schemas and terms not verified. |
| Historical Senate 2022/2024 | Existing local import and integration test | Coverage depends on the actual imported MIT file and scope, not the nationwide UI; the running database was not modified by this branch. |
| House/county history and 2026 geometry | Incomplete | No new district-level or county-intersection imports; do not allocate county results to districts. |
| Published forecasts | Existing prior implementation | Publisher attribution and supported scope apply; no new forecast or model created here. |
| Historical and live exit surveys | NOT_CONNECTED | No licensed, verified survey import has been added. |

The MIT House V15 original request on 2026-11-02 returned a required-guestbook error; its public release metadata was accessible, but the file was not imported. The publisher's forecast datasets were verified by integration tests in a disposable database on that date; no refresh was written to the user's existing database.
