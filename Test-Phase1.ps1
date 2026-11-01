$ErrorActionPreference = 'Stop'
$Root = $PSScriptRoot
$PidFile = Join-Path $Root '.runtime\phase1-processes.json'
$StartedHere = $false

if (-not $env:JAVA_HOME) {
  $LocalJdk = Get-ChildItem (Join-Path $Root '.runtime\jdk') -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
  if ($LocalJdk) { $env:JAVA_HOME = $LocalJdk.FullName }
}
if (-not $env:JAVA_HOME) { throw 'Set JAVA_HOME to a JDK 21 installation.' }
$env:Path = "$(Join-Path $env:JAVA_HOME 'bin');$env:Path"

if (-not (Test-Path -LiteralPath $PidFile)) {
  $Occupied = @(Get-NetTCPConnection -State Listen -LocalPort 3000,8080 -ErrorAction SilentlyContinue)
  if ($Occupied.Count -gt 0) { throw 'Ports 3000 or 8080 are occupied without a Phase 1 ownership record. Verify the process before testing.' }
  & (Join-Path $Root 'Start-Phase1.ps1') -Profile demo -NoBrowser
  if ($LASTEXITCODE -ne 0) { throw 'Phase 1 demo startup failed.' }
  $StartedHere = $true
}

try {
  Push-Location $Root
  try { & npm.cmd run check; if ($LASTEXITCODE -ne 0) { throw 'Phase 0 checks failed.' } } finally { Pop-Location }
  Push-Location (Join-Path $Root 'backend')
  try { & .\mvnw.cmd test; if ($LASTEXITCODE -ne 0) { throw 'Backend tests failed.' } } finally { Pop-Location }
  Push-Location (Join-Path $Root 'frontend')
  try {
    & npm.cmd run typecheck; if ($LASTEXITCODE -ne 0) { throw 'Type checking failed.' }
    & npm.cmd test; if ($LASTEXITCODE -ne 0) { throw 'Frontend unit tests failed.' }
    & npm.cmd run test:e2e; if ($LASTEXITCODE -ne 0) { throw 'Browser tests failed.' }
  } finally { Pop-Location }
} finally {
  if ($StartedHere) { & (Join-Path $Root 'Stop-Phase1.ps1') }
}
