-- Discovery metadata only. No providers are enabled and no credentials are stored.
INSERT INTO source_providers(provider_id, display_name, source_url, category, license_id, use_rights_verified, enabled, coverage_json)
VALUES
  ('MEDSL_2022','MIT Election Data + Science Lab 2022 official returns','https://github.com/MEDSL/2022-elections-official','historical_results',NULL,false,false,'{"years":[2022],"geographies":"precinct varies by state","rights":"UNVERIFIED","imported":false}'::jsonb),
  ('MEDSL_2024','MIT Election Data + Science Lab 2024 official returns','https://github.com/MEDSL/2024-elections-official','historical_results',NULL,false,false,'{"years":[2024],"geographies":"precinct varies by state","rights":"UNVERIFIED","imported":false}'::jsonb),
  ('MIT_ELECTION_LAB','MIT Election Lab data catalog','https://electionlab.mit.edu/data','historical_catalog',NULL,false,false,'{"years":"2000-2024 varies by dataset","rights":"UNVERIFIED","imported":false}'::jsonb),
  ('CENSUS_BAS_2026_CD119','U.S. Census Bureau TIGERweb 119th Congressional Districts BAS 2026','https://tigerweb.geo.census.gov/arcgis/rest/services/TIGERweb/Legislative/MapServer/4','geography',NULL,false,false,'{"vintage":"2026-01-01","district":"119th Congress for 2026 election","rights":"UNVERIFIED","imported":false}'::jsonb),
  ('CIVICAPI','civicAPI v2 election results API','https://www.civicapi.org/api-documentation','live_results',NULL,false,false,'{"baseUrl":"https://civicapi.org/api/v2","attributionRequired":true,"apiSchemaVerified":false,"connected":false,"coverage":"provider states worldwide aggregation; exact 2026 coverage unverified"}'::jsonb),
  ('AP_ELECTIONS','Associated Press Elections API','https://developer.ap.org/ap-elections-api/index.html','live_results',NULL,false,false,'{"coverage":"contract-specific","subscription":"not acquired","credentialsRef":null}'::jsonb)
ON CONFLICT (provider_id) DO NOTHING;
