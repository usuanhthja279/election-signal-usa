# Native PostgreSQL setup for Phase 1

Phase 1 has no live provider. Use a dedicated PostgreSQL database, not a production election database or a database containing demo fixtures. Install a supported PostgreSQL release natively on Windows from the [PostgreSQL Windows downloads page](https://www.postgresql.org/download/windows/) or use a managed local installation. PostgreSQL 17.11 was used for the disposable migration test. No Docker is required.

With `psql` on PATH and the server running, connect as your PostgreSQL administrator and execute:

```sql
CREATE ROLE election_app LOGIN;
CREATE DATABASE election_signal OWNER election_app;
```

In the `psql` session, set the application role password with `\password election_app`; psql prompts for it without placing it in the SQL history. Then in PowerShell set the application connection for the current process:

```powershell
$env:ELECTION_DB_URL = 'jdbc:postgresql://127.0.0.1:5432/election_signal'
$env:ELECTION_DB_USER = 'election_app'
$secret = Read-Host 'PostgreSQL application password' -AsSecureString
$env:ELECTION_DB_PASSWORD = [System.Net.NetworkCredential]::new('', $secret).Password
.\Start-Phase1.ps1 -Profile local
```

The local profile verifies its connection and Flyway applies the checked-in versioned migrations on startup. It does not load fictional race fixtures into PostgreSQL. No live results, historical imports, surveys or model are connected. Stop the app with `.\Stop-Phase1.ps1`; the existing PostgreSQL service remains under its owner's control. Remove the environment variable from the shell when no longer needed: `Remove-Item Env:ELECTION_DB_PASSWORD`.

To verify the migrations and profile isolation without touching an existing database, point the disposable test script at a PostgreSQL Windows binary `bin` directory:

```powershell
.\scripts\Test-Postgres.ps1 -PgBin 'C:\Program Files\PostgreSQL\17\bin'
```

It creates a separate password-protected cluster under ignored `.runtime`, uses port 55432 by default, runs the Java tests, and stops the cluster. Add `-WithUi` to build and test the real local-profile application in desktop and mobile Chrome; ports 3000 and 8080 must also be free. Pass `-Port` if the PostgreSQL port is in use. The generated test cluster files remain under `.runtime` for inspection; the random password file is removed.
