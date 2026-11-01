# Historical import guide

`docs/history_sources.csv` is the source ledger. Obtain the exact release file from a usable authoritative or licensed source and record version, rights, retrieval date, checksum, expected coverage and limitations before staging. Run `scripts/Import-History.ps1` to validate and stage a cited CSV; run `scripts/Validate-History.ps1` and `scripts/History-Coverage.ps1` for validation/reporting. These commands do not publish records. Publication requires reviewed rights and a certified canonical import path; no historical result files are currently imported.

Keep raw releases immutable. Normalize candidate, party, office, election subtype, geography vintage and numeric vote total separately. Preserve zero, null and missing as distinct. Do not allocate county totals into congressional districts or substitute presidential data for another office. Imports must be repeatable and duplicate-safe before database publication is enabled.
