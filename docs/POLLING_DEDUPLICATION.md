# Poll reconciliation

`PollDeduplicationService` compares pollster, sponsor, office, geography, field dates, population, matchup, questionnaire and publication identity. An exact publication identity plus matching attributes is `EXACT_MATCH`; matching core attributes with different publication identities is `PROBABLE_DUPLICATE`; differing populations, matchups or question versions are `DIFFERENT_QUESTIONNAIRE`. Missing evidence is `REVIEW_REQUIRED`. Different scope or fieldwork is `INDEPENDENT_POLL`.

The comparison service alone does not merge records or write review decisions. The manual import path described below applies it; any other database writer must enforce the same invariants.

The manual licensed survey importer now applies this comparison transactionally, rejects ambiguous or multiple matches and differing answer identities, and links matched syndicated publications to the same survey. The public API lists their individually approved citations. A comparison cannot establish truth of operator-supplied identifiers.
