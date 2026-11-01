# Live County-Level Statistical Modeling Research Specification

This is a research/engineering design for later phases. **Phase 0 contains no predictive model and no real win probabilities.** Do not implement numeric candidate-win estimates until calibration and data-quality gates pass.

## Objective

Estimate the distribution of *unreported votes* in one legally defined election contest (Senate statewide, Governor statewide, House congressional district) using live reported votes, documented reporting completeness, and historical geographical patterns; expose uncertainty and abstain where information is insufficient. Do not use modeled outputs as publisher race calls.

## Historical inputs

- **2022 and 2024** are the primary historical contextual years requested. Compare 2022 midterm vs 2024 general by office and electorate, and record whether a race with the same ballot office actually existed.
- Senate 2026 regular seats were generally last elected in 2020; prior same-seat results may be a more relevant additional reference. Some 2026 races are special elections.
- For House 2026, changed district boundaries can make naive 2022/2024 county-vs-district comparisons invalid. Implement official precinct-to-district crosswalk or abstain.
- Include turnout, certified candidate-level results, third-party votes, geographic FIPS, office and boundary vintage; never assign a whole county to one district if split.
- Archived reporting snapshots and ballot types are distinct data from final 2022/2024 results; no batch order can be inferred simply from certified final totals.

## Descriptive metric

For county `c`, year `y`, and an appropriate ballot office:

`p[c,y] = D[c,y] / (D[c,y] + R[c,y])` only if both candidate vote totals and a comparable two-party contest exist.

`margin[c,y] = 2 * p[c,y] - 1`.

`observed_shift = reported_2026_margin - historically_adjusted_baseline_margin`.

These are **two-party descriptive metrics**, not final forecasts; preserve independent/third-party counts in actual total-vote accounting. A county reporting 20% of expected ballots is not a random sample of its eventual result.

## Data audit before model run

1. Canonical race ID and office verified, source type live and not rehearsal/test.
2. Candidate IDs and reported totals internally consistent; no unexpected `null→0` coercion.
3. Source time not excessively stale; legal county/district geography available.
4. Reporting-progress type known (`expected vote`, `precincts reporting`, `ballots tabulated`, etc.). If not trustworthy, abstain or widen uncertainty appropriately.
5. Unexpected negative deltas are recorded as corrections, not dropped.
6. Ballot type/mail/election day reporting mix handled where published, and missing otherwise.
7. Sufficient observed geographic coverage; data source integrity/health green.

## Staged development

**Step A: deterministic baseline model** using historical county (office-aware) share with shrinkage, state effects, boundary normalization. Validate on held-out election years. Avoid arbitrary hard-coded weights.

**Step B: turnout/remaining vote model** estimates eventual votes per reporting unit and outstanding vote distribution, with confidence/credible ranges. Do not use precincts-reporting percent as votes-counted percent.

**Step C: conditional remaining-vote model** accounts for source composition and temporal reporting order when archived batch data is available. If not available, explicitly increase uncertainty and do not claim learned batch patterns.

**Step D: correlated Monte Carlo** sample statewide and county-level residuals jointly, preserve total-vote accounting, generate final-margin distribution and only then derived probabilities if justified by rigorous validation.

**Step E: calibration** use historical timestamped batches and actual completed certified results. Track margin MAE, log loss/Brier score for eventual win events (if probabilities published), reliability diagram, interval coverage, county-size strata, region and office type. Verify performance under low reporting percentages; use an abstention threshold when calibration cannot be demonstrated.

**Step F: operational model** store every input snapshot ID, feature version, model version, output distribution, as-of time, uncertainty, explanation, warning or abstention status. Replay a historical snapshot deterministically; do not overwrite forecasts.

## API status example

```json
{
  "raceId": "US-2026-HOUSE-TX-07",
  "modelStatus": "ABSTAINED",
  "modelVersion": null,
  "inputAsOf": null,
  "probabilities": null,
  "reason": "No validated model/data source is connected",
  "isTestData": false
}
```

If the model eventually provides a distribution, display it **only on individual race pages** alongside but separately from votes, and label it estimated chance, model name/version/as-of and key limitations. National overview remains reported-results-only. Never display a race call unless a named publisher actually made one.

## Suggested Python packages later

Python 3.12+, pandas/Polars, NumPy, SciPy, scikit-learn, statsmodels or PyMC if needed, pytest, Pydantic. Optional FastAPI serving model runs, but a background process plus database persistence may be enough initially. GPU is optional; start with reproducible CPU baselines before heavy models.
