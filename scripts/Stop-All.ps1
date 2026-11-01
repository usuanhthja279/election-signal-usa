$ErrorActionPreference = 'Stop'
$Root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$PidPath = Join-Path $Root '.runtime/server.pid'
if (-not (Test-Path $PidPath)) { Write-Host 'No Phase 0 server PID file. Nothing to stop.'; exit 0 }
$Saved = Get-Content $PidPath | Select-Object -First 1
if ($Saved -notmatch '^\d+$') { throw 'Invalid PID file. Remove it manually after verifying your running processes.' }
$Target = Get-CimInstance -ClassName Win32_Process -Filter "ProcessId = $Saved" -ErrorAction SilentlyContinue
if (-not $Target) { Remove-Item $PidPath -Force; Write-Host 'Server process already stopped.'; exit 0 }
if ($Target.Name -notmatch '^node(\.exe)?$' -or $Target.CommandLine -notmatch 'src[/\\]server\.mjs') {
  throw "PID $Saved does not look like the Election Signal server. Refusing to stop an unrelated process."
}
Stop-Process -Id ([int]$Saved) -Force
Remove-Item $PidPath -Force
Write-Host "Stopped Election Signal Phase 0 (PID $Saved)." -ForegroundColor Green
