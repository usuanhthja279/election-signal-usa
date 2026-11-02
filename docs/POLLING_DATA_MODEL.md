# Polling data model

Migration `V5__polling_foundation.sql` adds provider/source rights metadata, pollsters, canonical surveys, source publications and answer rows, reconciliation reviews, publisher average series/points, forecast models/snapshots and ingestion run metadata. It seeds no records. Foreign keys and checks preserve nullable sample sizes and percentages, restrict percentages to their appropriate ranges and reject rows explicitly marked as test data. These checks cannot detect a fictional record falsely labeled as real.

Surveys and source publications are separate so a syndicated survey can retain multiple sources without becoming multiple canonical surveys. Each publication and each model points to its own reviewed source. A source must have an approved review, explicit redistribution permission, a license, attribution and review evidence before reads can expose its data. SQL reads use parameterized queries and bounded pages. The read endpoint selects one permitted publication per canonical survey; no automated process currently reconciles or inserts surveys.

Forecasts and averages are separate tables; a probability is a fraction in [0,1], while shares use 0–100 percent. `asOf` filters publication timestamps, not model input timestamps; ingestion must verify source-specific historical inputs before claiming a historical model reconstruction.
