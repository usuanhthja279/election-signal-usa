# Poll reconciliation

`PollDeduplicationService` compares pollster, sponsor, office, geography, field dates, population, matchup, questionnaire and publication identity. An exact publication identity plus matching attributes is `EXACT_MATCH`; matching core attributes with different publication identities is `PROBABLE_DUPLICATE`; differing populations, matchups or question versions are `DIFFERENT_QUESTIONNAIRE`. Missing evidence is `REVIEW_REQUIRED`. Different scope or fieldwork is `INDEPENDENT_POLL`.

The service is a comparison foundation. It does not automatically merge records, write review decisions or prevent an operator assigning different canonical IDs to syndicated surveys. Until a reviewed importer and reconciliation workflow exist, do not claim production double-count prevention.
