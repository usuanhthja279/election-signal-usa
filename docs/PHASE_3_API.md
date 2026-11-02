# Phase 3 API status

Local-profile read endpoints use the existing versioned envelope and return only source rows with `review_status=APPROVED` and `redistribution_allowed=true`.

- `GET /api/v1/polls`: canonical surveys and their selected permitted publication; answers, field dates, population, sample and attribution remain distinct. A survey appears once per response, even if it has multiple approved publications.
- `GET /api/v1/polls/{id}`: scoped single survey, or 404 when absent or not publishable.
- `GET /api/v1/polls/averages` and `/polls/trends`: publisher-provided average points; no aggregate is calculated by the application.
- `GET /api/v1/forecasts/published` and `/forecasts/published/{id}`: attributed publisher snapshots, separate from internal model output.
- `GET /api/v1/polls/providers`, `/polls/pollsters`, and `/polling/sources/health`: read-only catalog / health metadata.

List endpoints support `year`, `office`, `state`, `district`, `provider`, `race`, `candidate`, `dateFrom`, `dateTo`, `asOf`, `page` and `pageSize` where applicable. Survey lists additionally accept `pollster` and `population`. `dateFrom`/`dateTo` are UTC publication dates; `asOf` is an ISO UTC instant. Page starts at zero and page size is 1–100. An unavailable source returns an empty list and `NOT_CONNECTED`. Forecast records may include `sourceDate` when the provider supplies a date but no reliable time; those records are excluded from instant-level `asOf` queries. As-of reads require both publication and retrieval to precede the cutoff; survey citations follow the same rule. This prevents presenting later revisions as data the application had already observed. Forecasts and averages reject survey-only filters. Existing validation rejects unverified House district selections.

Known gaps: sort selection, per-source availability/freshness computation, automated scheduling, broad provider integrations and full error/status support are not implemented. See the manual import guide for the source-specific forecast adapter and rights-gated survey publishing path. The demo profile keeps its fictional votes isolated and does not serve real polling records. No public write endpoint exists.
