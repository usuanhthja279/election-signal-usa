# Historical coverage

The dedicated local data preview imports MIT Election Data and Science Lab’s CC0 U.S. Senate statewide release V8 for 2022 and 2024. The original CSV was downloaded on 2026-11-02; its MD5 `0f8b51cf0f77a2ef0bff992f7a64d1b4` matches Harvard Dataverse metadata, and its SHA-256 is `6f745db1b4a0026ad837e74428f9ed6f3f77fa51eb6f858b58ebdbc186fdb3bd`. The importer checks the pinned SHA-256, validates contest totals, and stores the original bytes and attribution.

Coverage is 65 general-election contests and 302 candidate-party rows across both years; rows flagged unofficial and runoff stages are excluded. These are statewide Senate results reported as official by the source, not separately verified certifications. Regular and special contests remain distinct. County, House and Governor returns, excluded contests and all other selectable years remain unavailable. No historical result has been inferred from a different office or geography.

The Harvard House release requires a guestbook response in this environment and has not been downloaded. The original Phase 0 demo remains fictional. See `history_sources.csv` for source records and `HISTORY_IMPORT_GUIDE.md` for operations.
