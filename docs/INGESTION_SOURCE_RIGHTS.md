# Source rights and data boundaries

The source registry's existing enabled, license and rights checks are supplemented by `source_usage_permissions`: archival, UI display and API redistribution are separate permissions. Existing sources are not implicitly granted archival access by the migration. The observation recorder requires an explicit archive permission and verifies that archived UTF-8 bytes match the submitted JSON. Permission for display and API redistribution must also be enforced when a new live delivery path is implemented; this migration alone does not authorize publication.

All permissions default false. A human must document the exact source terms, scope, evidence and review date before enabling any use. A reachable government URL does not by itself establish all archival or redistribution rights. Open America requests permission before repackaging its feed; no permission has been obtained here. No subscriber feed is active. Fictional tests are confined to isolated demo/unit tests or a disposable PostgreSQL cluster, never the user's existing local data.

The North Carolina preflight downloads one official package on explicit invocation, parses it in memory, and prints aggregate validation counts. It does not archive the file or publish records. Arizona uses a documented-schema parser with fabricated test inputs only. Neither tool runs as an automatic collector.

2026-11-02 follow-up: `Archive-NorthCarolinaResults.ps1` adds a manual, permission-gated archival path. It is not enabled or approved by default, and does not expose any results. The append-only archive supports original compressed bytes; display and API permissions remain separate and unenforced by any new publishing path because none has been added.

North Carolina general website terms (https://www.nc.gov/disclaimer-terms-use) describe non-commercial copying and distribution of unchanged non-image information with attribution. They do not establish permission for a commercial product, transformed data, API redistribution, or even their applicability to the specific election-download host. Keep archival and display authorization disabled pending review appropriate to the intended use.
