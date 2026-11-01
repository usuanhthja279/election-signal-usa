# Phase 2 CI guide

`.github/workflows/phase2-ci.yml` defines frontend, backend, native PostgreSQL, Playwright, and Windows smoke jobs. It uses GitHub-hosted native runners and no container service. Permissions are read-only for repository contents; concurrent superseded runs are canceled. No CI job contacts election providers.

The workflow is defined but has not been verified by a GitHub Actions run. Add the workflow badge using the repository Actions URL after the first successful run; no badge is presented as passing before then.

Local reproduction: frontend `npm ci`, `npm run typecheck`, `npm test`, `npm run build`; backend JDK 21 and `mvnw.cmd verify`; PostgreSQL via `scripts/Test-Postgres.ps1`; browser via `npx playwright install chromium` and `npx playwright test`; Windows smoke via `scripts/Test-Phase2Windows.ps1`. Troubleshoot Java/Node versions, project-local Maven cache, and ephemeral PostgreSQL port conflicts before retrying. Never place provider credentials in workflow variables or logs.
