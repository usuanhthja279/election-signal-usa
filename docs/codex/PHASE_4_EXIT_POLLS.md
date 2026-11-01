# CODEX PHASE 4 — Exit Polls / Voter Survey lens

Read `AGENTS.md`, `docs/CODEX_MASTER_PROMPT.md`, `docs/DATA_SOURCES.md`. Implement only Phase 4.

1. Research legal rights to AP VoteCast, Roper archives or other licensed survey data; check embargo and attribution, weighting disclosure, sample-size suppression, permitted chart types and geographical restrictions. Do not collect/show restricted crosstabs without explicit authorization.
2. Define `VoterSurveySourceAdapter` and API DTOs that include exact question wording, field dates, likely population, weighting, sample N, subgroup dimensions, observed response percentages, suppression status, original URL, release timestamp, revision and license ID.
3. Implement an import path for licensed released surveys; maintain raw immutable snapshots and audited revisions.
4. Build neutral, legible `Exit Polls` lens with state/national filters, issue/demographic subgroup charts, source attribution and clear limitations. Where an office or state has no source data, say unavailable.
5. Exit polls are **not** counted ballot results and are not pre-election intention polls. Do not infer or fabricate county votes or district forecasts from exit survey crosstabs.
6. Before a release, 2026 must show NOT_PUBLISHED, not fictional graphs. Respect embargo flags even if source fetched early.
7. Add tests for missing/suppressed cells, small subgroup samples, embargo release time, office/state propagation, no-data cases, and no context contamination.

Report licensed sources, coverage/limitations and tests. Stop for review.
