$ErrorActionPreference='Stop'
$root=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$marker=Join-Path $root '.runtime/phase3-processes.json'
$phase1=Join-Path $root '.runtime/phase1-processes.json'
if(-not (Test-Path -LiteralPath $marker)){throw 'No Phase 3 ownership marker exists; no process was stopped.'}
if(-not (Test-Path -LiteralPath $phase1)){throw 'The matching launcher record is missing; no process was stopped.'}
if((Get-FileHash -LiteralPath $marker -Algorithm SHA256).Hash -ne (Get-FileHash -LiteralPath $phase1 -Algorithm SHA256).Hash){throw 'The process record has changed; no process was stopped.'}
& (Join-Path $root 'Stop-Phase1.ps1')
if(Test-Path -LiteralPath $phase1){throw 'The launcher record remains; retaining the Phase 3 marker.'}
Remove-Item -LiteralPath $marker -Force
Write-Host 'Phase 3-owned services stopped.'
