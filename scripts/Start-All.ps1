param(
  [ValidateRange(1024,65535)][int]$Port = 4173,
  [switch]$NoBrowser,
  [switch]$Foreground
)
$ErrorActionPreference = 'Stop'
$Root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$Node = Get-Command node -ErrorAction SilentlyContinue
if (-not $Node) { throw 'Node.js 20+ is required. Install Node.js LTS then reopen PowerShell.' }
$NodeMajor = [int]((& node --version).TrimStart('v').Split('.')[0])
if ($NodeMajor -lt 20) { throw "Node.js 20+ required; found $NodeMajor" }
$env:PORT = "$Port"
$env:HOST = '127.0.0.1'
$env:ELECTION_DATA_MODE = 'demo'
$Url = "http://127.0.0.1:$Port"
Write-Host 'Election Signal Phase 0 — DEMO MODE ONLY' -ForegroundColor Yellow
Write-Host "UI: $Url" -ForegroundColor Cyan
Write-Host 'No Docker, database, credentials or internet connection required.'
if ($Foreground) {
  if (-not $NoBrowser) { Start-Process $Url }
  Push-Location $Root
  try { & node 'src/server.mjs' } finally { Pop-Location }
  exit
}
$PidPath = Join-Path $Root '.runtime/server.pid'
New-Item -ItemType Directory -Force -Path (Join-Path $Root '.runtime') | Out-Null
if (Test-Path $PidPath) {
  $Saved = Get-Content $PidPath -ErrorAction SilentlyContinue | Select-Object -First 1
  if ($Saved -and (Get-Process -Id ([int]$Saved) -ErrorAction SilentlyContinue)) {
    Write-Host 'A previous server PID exists. Run scripts/Stop-All.ps1 first.' -ForegroundColor Yellow
    exit 1
  }
  Remove-Item $PidPath -Force
}
$Process = Start-Process -FilePath $Node.Source -ArgumentList @('src/server.mjs') -WorkingDirectory $Root -PassThru -WindowStyle Hidden
Set-Content -Path $PidPath -Value "$($Process.Id)" -Encoding ascii
$Ready = $false
for ($i=0; $i -lt 20; $i++) {
  Start-Sleep -Milliseconds 350
  try {
    $Health = Invoke-RestMethod -Uri "$Url/api/v1/health" -TimeoutSec 2
    if ($Health.service -eq 'election-signal-phase0' -and $Health.dataMode -eq 'DEMO') { $Ready = $true; break }
  } catch { }
}
if (-not $Ready) {
  Write-Warning "Server did not become ready at $Url. Check port availability and run 'node src/server.mjs' in a terminal for logs."
  exit 1
}
Write-Host "Ready — PID $($Process.Id)" -ForegroundColor Green
if (-not $NoBrowser) { Start-Process $Url }
