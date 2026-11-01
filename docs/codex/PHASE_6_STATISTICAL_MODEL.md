# CODEX PHASE 6 — Research, backtesting and integration of live statistical model

Read `AGENTS.md`, `docs/CODEX_MASTER_PROMPT.md`, `docs/MODELING_RESEARCH_SPEC.md`, `docs/DATA_CONTRACTS.md`. Implement only Phase 6, and do not publish unvalidated probabilities.

## Goal

Analyze whether incomplete reported county/precinct voting patterns differ from office-appropriate **2022 and 2024** baselines, how many ballots might remain, and how sensitive the final race margin is to unreported vote composition. A model may issue a probability distribution only after rigorous backtesting/calibration; otherwise its API must return `ABSTAINED`.

## Plan

1. Build Python 3.12+ isolated package under `modeling/` (native venv), deterministic datasets, versioned seeds and scripts. Reuse verified historical data and 2026 raw snapshots, not UI fixture values.
2. Model candidate-level distributions (not just D/R), including third-party candidates. Design statewide Senate/Governor and district-specific House models separately.
3. Compute two-party descriptive historical comparison for counties only when valid, accounting for 2022 midterm vs 2024 general turnout, office-specific candidacies, missing/uncontested results, geographic changes and Senate prior same-seat 2020 where required.
4. Build vote-remaining/turnout range model from a clearly typed reporting-progress metric. Precinct share is not counted-vote share. Train/report uncertainty and source constraints.
5. Account for systematic reporting-order bias (mail vs early vs in-person and geographical batch sequence) *only* when timestamped history or current source provides adequate fields. Never infer historical batch ordering from final certified totals alone.
6. Use hierarchical/regularized baseline with geographically correlated residuals, then Monte Carlo distribution of plausible uncounted votes. Ensure total ballot accounting remains consistent.
7. Backtest on real archived prior election-night snapshots when available (2018, 2020, 2022, 2024), not just randomly hide counties. Disclose when evaluation data only permit synthetic backtests. Measure vote-margin MAE, interval coverage, log loss/Brier score if probabilities, and reliability by reporting completeness.
8. Define a conservative **abstention policy**: inadequate coverage, unknown metric, stale feed, failed calibration, changed district without crosswalk, anomalous corrections or unreliable historical data => `ABSTAINED` and explanation.
9. Save immutable model run with version, inputs, all source snapshot IDs, timestamp, uncertainties, diagnostics, and explanation. Never label the model output as a published race call.
10. Add individual Senate/Governor/House race panels with estimated margin range, model win chances if validated, updates over time, source and confidence caveats. **Do not put forecast values on national Live Overview.**
11. Test replay reproducibility, source revision corrections, no-data/missing-county cases, early-vote reporting skew and district splits. Add a report describing where model is reliable and where it abstains.

## Acceptance gate

Real win-probability values are displayable only if documented out-of-sample calibration tests pass and required source fields exist for that race. Unknown reliability => `Model unavailable`. No fictional numerical model probabilities in live mode. Exit polls and opinion polls remain distinct from vote counts.

Deliver model code, reproducibility instructions, reports, API DTOs and an audit summary. Stop for review.
