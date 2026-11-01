# Ingestion architecture

`ElectionResultProvider` is the provider boundary. `ObservationRecorder` validates and persists raw bytes, SHA-256 fingerprint and candidate revisions only for a provider whose database rights record is enabled and verified. Fictional replay/test payloads are rejected by the production recorder. PostgreSQL append-only triggers protect raw observation and history records. Deterministic conflict keys make replay idempotent while preserving subsequent corrections as new observations.

`PollingCoordinator` provides bounded workers, one in-flight poll per provider, exponential jittered backoff, `Retry-After` support and shutdown. It is a foundation, not an active service: no real adapter is registered or scheduled. REST reconstruction exists for current Phase 1 data; Phase 2 compact SSE, reconnect cursor and missed-event recovery are not implemented. Synthetic replay isolation is tested at validation boundary only; a full replay CLI remains outstanding.
