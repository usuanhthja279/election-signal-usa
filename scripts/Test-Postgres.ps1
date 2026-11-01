param(
  [Parameter(Mandatory=$true)][string]$PgBin,
  [ValidateRange(1024,65535)][int]$Port = 55432,
  [switch]$WithUi
)
$ErrorActionPreference = 'Stop'
$SavedEnv = @{}
foreach ($Name in @('JAVA_HOME','Path','PGPASSWORD','ELECTION_DB_TEST_URL','ELECTION_DB_TEST_USER','ELECTION_DB_TEST_PASSWORD','ELECTION_DB_URL','ELECTION_DB_USER','ELECTION_DB_PASSWORD','ELECTION_E2E_LOCAL')) { $SavedEnv[$Name] = [Environment]::GetEnvironmentVariable($Name, 'Process') }
$Root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$PgBin = (Resolve-Path -LiteralPath $PgBin).Path
if (-not (Test-Path (Join-Path $PgBin 'initdb.exe'))) { throw 'PgBin must point to PostgreSQL binaries.' }
if (Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue) { throw "Port $Port is already in use." }
if (-not $env:JAVA_HOME) {
  $Jdk = Get-ChildItem (Join-Path $Root '.runtime\jdk') -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
  if ($Jdk) { $env:JAVA_HOME = $Jdk.FullName }
}
if (-not $env:JAVA_HOME) { throw 'Set JAVA_HOME to a JDK 21 installation.' }
$Runtime = Join-Path $Root '.runtime'
New-Item -ItemType Directory -Force -Path $Runtime | Out-Null
$TestRoot = Join-Path $Runtime ("pgtest-" + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $TestRoot | Out-Null
$Data = Join-Path $TestRoot 'data'
$PasswordFile = Join-Path $TestRoot 'pw.txt'
$Password = [guid]::NewGuid().ToString('N')
Set-Content -LiteralPath $PasswordFile -Value $Password -NoNewline -Encoding ascii
$Started = $false
$AppStarted = $false
try {
  & (Join-Path $PgBin 'initdb.exe') -D $Data -U election_test --auth=scram-sha-256 --pwfile=$PasswordFile --encoding=UTF8 --no-locale
  if ($LASTEXITCODE -ne 0) { throw 'initdb failed.' }
  Remove-Item -LiteralPath $PasswordFile -Force
  & (Join-Path $PgBin 'pg_ctl.exe') -D $Data -l (Join-Path $TestRoot 'postgres.log') -o "-p $Port -h 127.0.0.1" -w start
  if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL failed to start.' }
  $Started = $true
  $env:PGPASSWORD = $Password
  & (Join-Path $PgBin 'createdb.exe') -h 127.0.0.1 -p $Port -U election_test election_test
  if ($LASTEXITCODE -ne 0) { throw 'Test database creation failed.' }
  $env:ELECTION_DB_TEST_URL = "jdbc:postgresql://127.0.0.1:$Port/election_test"
  $env:ELECTION_DB_TEST_USER = 'election_test'
  $env:ELECTION_DB_TEST_PASSWORD = $Password
  $env:Path = "$(Join-Path $env:JAVA_HOME 'bin');$env:Path"
  Push-Location (Join-Path $Root 'backend')
  try { & .\mvnw.cmd test; if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL integration tests failed.' } } finally { Pop-Location }
  if ($WithUi) {
    $env:ELECTION_DB_URL = $env:ELECTION_DB_TEST_URL
    $env:ELECTION_DB_USER = $env:ELECTION_DB_TEST_USER
    $env:ELECTION_DB_PASSWORD = $Password
    & (Join-Path $Root 'Start-Phase1.ps1') -Profile local -NoBrowser
    $AppStarted = $true
    $env:ELECTION_E2E_LOCAL = '1'
    Push-Location (Join-Path $Root 'frontend')
    try { & npm.cmd exec playwright test e2e/local.spec.ts; if ($LASTEXITCODE -ne 0) { throw 'Local profile browser tests failed.' } } finally { Pop-Location }
  }
} finally {
  try { if ($AppStarted) { & (Join-Path $Root 'Stop-Phase1.ps1') } }
  finally {
    if ($Started) { & (Join-Path $PgBin 'pg_ctl.exe') -D $Data -m fast -w stop }
    if (Test-Path -LiteralPath $PasswordFile) { Remove-Item -LiteralPath $PasswordFile -Force }
    foreach ($Name in $SavedEnv.Keys) { [Environment]::SetEnvironmentVariable($Name, $SavedEnv[$Name], 'Process') }
  }
}
