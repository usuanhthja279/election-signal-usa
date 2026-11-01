# CODEX PHASE 2 — Verified historical 2000–2024 results and geographic versioning

Read `AGENTS.md`, `docs/CODEX_MASTER_PROMPT.md` and `docs/codex/PHASE_1_FOUNDATION.md`. Implement only this Phase 2 on top of verified Phase 1 code; do not alter the approved UI without need.

## Objective

Enable authenticated-to-source but publicly browsable **certified historical results** by election year 2000, 2002, …, 2024 with accurate office, contest, district, county and boundary context. Prioritize high-quality complete **2022/2024** returns that will support county comparisons and later modeling.

## Work plan

1. Audit available official and MIT Election Lab datasets *by office/year/geography*, license and completeness. Build `docs/history_sources.csv` inventory and codebooks; do not claim all 13 election years have equal county coverage.
2. Add schema migrations for historical races, contests, vote methods, candidate results, FIPS geographies, boundary vintage, district crosswalks, source publications and raw source archives. Reuse shared election registry and canonical IDs.
3. Implement CLI `Import-History.ps1 -Year 2022 -Office ... -SourceFile ...` and a Python 3.12+ ETL (or Java batch if simpler). Raw files must be checksum-versioned, staged and validated before committing; write idempotently.
4. Normalize party codes, independents, third-party candidates, candidate totals and special/runoff elections. Distinguish uncontested/not-on-ballot/source missing/not imported.
5. Build state-level and county-level comparisons, especially 2022 versus 2024 **same comparable office**. Presidential-year turnout is not midterm turnout. For Senate 2026 consider prior same-seat 2020 when appropriate.
6. Obtain officially appropriate **2026 congressional district polygons** and versioned historical boundaries. Implement precinct-to-district mapping only if defensible; reject simple whole-county assignments into split districts.
7. Add historical API endpoints with provenance, coverage, downloadable data credits and source-as-of/certification; pagination for large county/precinct tables.
8. Connect Past Results UI to verified back-end data; 2000–2024 year picker propagates to Senate/House/Governor and state views. Missing sections should explain why unavailable.
9. Add validation reports: county sums vs state totals, malformed FIPS, missing votes, duplicate imported rows, negative votes (when present in corrections), boundary-vintage errors.

## Acceptance

- 2022 and 2024 verified returns can be queried with original source links and checksums.
- Selecting 2000 then TX then House does not show 2026 live numbers; if no historical record is available, a truthful unavailable state appears.
- House districts and county maps use correct geography; no unsupported crosswalk.
- Users can compare county reported 2026 numbers only when a suitable baseline and matching boundaries exist; otherwise comparison is explicitly unavailable.
- All tests pass, including re-import idempotency and state/district aggregation.

Report imported dataset list, exact year/office/geographic coverage, known missing data, source rights and test evidence. Stop for review.
