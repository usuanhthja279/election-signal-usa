$ErrorActionPreference = 'Stop'
$PidFile = Join-Path $PSScriptRoot '.runtime\phase1-processes.json'
if (-not (Test-Path $PidFile)) { Write-Host 'No Phase 1 process record.'; exit 0 }
$Records = @(Get-Content -Raw -LiteralPath $PidFile | ConvertFrom-Json)
foreach ($Record in $Records) {
  $Process = Get-Process -Id ([int]$Record.pid) -ErrorAction SilentlyContinue
  if (-not $Process) { continue }
  if ($Process.StartTime.ToUniversalTime().Ticks -ne [long]$Record.started) { throw "PID $($Record.pid) has been reused; refusing to stop it." }
  $Info = Get-CimInstance Win32_Process -Filter "ProcessId = $([int]$Record.pid)"
  if ($Info.CommandLine -notmatch 'backend-0\.0\.1-SNAPSHOT\.jar|node_modules[\\/]next[\\/]dist[\\/]bin[\\/]next') { throw "PID $($Record.pid) does not match the project; refusing to stop it." }
  Stop-Process -Id $Process.Id -Force
  Wait-Process -Id $Process.Id -Timeout 5 -ErrorAction SilentlyContinue
  if (Get-Process -Id $Process.Id -ErrorAction SilentlyContinue) {
    # The verified launcher may have spawned a same-project child that owns the listener.
    & "$env:SystemRoot\System32\taskkill.exe" /PID $Process.Id /T /F | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "Failed to stop verified Phase 1 process tree for PID $($Record.pid)." }
    Wait-Process -Id $Process.Id -Timeout 5 -ErrorAction SilentlyContinue
  }
  if (Get-Process -Id $Process.Id -ErrorAction SilentlyContinue) { throw "PID $($Record.pid) is still running; retaining the ownership record." }
}
Remove-Item -LiteralPath $PidFile -Force
Write-Host 'Phase 1 processes stopped.'
