param([ValidateSet('demo','local')][string]$Profile = 'demo', [switch]$NoBrowser)
$ErrorActionPreference = 'Stop'
$Root = $PSScriptRoot
$Runtime = Join-Path $Root '.runtime'
$PidFile = Join-Path $Runtime 'phase1-processes.json'
if (Test-Path $PidFile) { throw 'Phase 1 process record exists. Run Stop-Phase1.ps1 first.' }
$Node = Get-Command node -ErrorAction SilentlyContinue
if (-not $Node) { throw 'Node.js 20.9+ is required.' }
if (-not $env:JAVA_HOME) {
  $LocalJdk = Get-ChildItem (Join-Path $Runtime 'jdk') -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
  if ($LocalJdk) { $env:JAVA_HOME = $LocalJdk.FullName }
}
$Java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\java.exe' } else { (Get-Command java -ErrorAction SilentlyContinue).Source }
if (-not $Java -or -not (Test-Path $Java)) { throw 'Java 21 is required. Set JAVA_HOME to a JDK 21 installation.' }
if ($Profile -eq 'local' -and (-not $env:ELECTION_DB_URL -or -not $env:ELECTION_DB_USER -or -not $env:ELECTION_DB_PASSWORD)) { throw 'For local profile, set ELECTION_DB_URL, ELECTION_DB_USER and ELECTION_DB_PASSWORD.' }
foreach ($Port in @(3000, 8080)) {
  if (Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue) { throw "Port $Port is already in use." }
}
New-Item -ItemType Directory -Force -Path $Runtime | Out-Null
Push-Location (Join-Path $Root 'backend')
try { & .\mvnw.cmd -q '-Delection.build.directory=../.runtime/phase1-build' -DskipTests clean package; if ($LASTEXITCODE -ne 0) { throw 'Backend build failed.' } } finally { Pop-Location }
Push-Location (Join-Path $Root 'frontend')
try { & npm.cmd run build; if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed.' } } finally { Pop-Location }
$Jar = Join-Path $Runtime 'phase1-build\backend-0.0.1-SNAPSHOT.jar'
$Next = Join-Path $Root 'frontend\node_modules\next\dist\bin\next'
$Started = @()
try {
  $Backend = Start-Process -FilePath $Java -ArgumentList @('-jar', "`"$Jar`"", "--spring.profiles.active=$Profile") -WorkingDirectory (Join-Path $Root 'backend') -PassThru -WindowStyle Hidden -RedirectStandardOutput (Join-Path $Runtime 'phase1-backend.log') -RedirectStandardError (Join-Path $Runtime 'phase1-backend-error.log')
  $Started += $Backend
  $Frontend = Start-Process -FilePath $Node.Source -ArgumentList @("`"$Next`"", 'start', '--hostname', '127.0.0.1') -WorkingDirectory (Join-Path $Root 'frontend') -PassThru -WindowStyle Hidden -RedirectStandardOutput (Join-Path $Runtime 'phase1-frontend.log') -RedirectStandardError (Join-Path $Runtime 'phase1-frontend-error.log')
  $Started += $Frontend
  $Records = @(@($Backend, $Frontend) | ForEach-Object { @{ pid = $_.Id; started = $_.StartTime.ToUniversalTime().Ticks } })
  $Records | ConvertTo-Json | Set-Content -Path $PidFile -Encoding UTF8
  $Ready = $false
  for ($i=0; $i -lt 60; $i++) {
    Start-Sleep -Milliseconds 500
    try {
      $Health = Invoke-RestMethod 'http://127.0.0.1:8080/api/v1/health' -TimeoutSec 2
      $Web = Invoke-WebRequest 'http://127.0.0.1:3000/' -UseBasicParsing -TimeoutSec 2
      $ExpectedMode = if ($Profile -eq 'demo') { 'DEMO' } else { 'LIVE_REPORTED' }
      if ($Health.service -eq 'election-signal-phase1' -and $Health.dataMode -eq $ExpectedMode -and $Web.StatusCode -eq 200) { $Ready = $true; break }
    } catch { }
  }
  if (-not $Ready) { throw 'Services did not become ready. Inspect .runtime/phase1-*.log.' }
  if ($Profile -eq 'demo') { Write-Host 'Phase 1 demo: http://127.0.0.1:3000 (fictional data only)' -ForegroundColor Yellow }
  else { Write-Host 'Phase 1 local: http://127.0.0.1:3000 (no election feed connected)' -ForegroundColor Yellow }
  if (-not $NoBrowser) { Start-Process 'http://127.0.0.1:3000' }
} catch {
  foreach ($Process in $Started) { if (-not $Process.HasExited) { Stop-Process -Id $Process.Id -Force } }
  if (Test-Path $PidFile) { Remove-Item -LiteralPath $PidFile -Force }
  throw
}
