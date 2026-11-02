param([ValidateSet('demo','local')][string]$Profile='demo',[switch]$NoBrowser)
$ErrorActionPreference='Stop'
$root=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$marker=Join-Path $root '.runtime/phase3-processes.json'
$phase1=Join-Path $root '.runtime/phase1-processes.json'
if(Test-Path -LiteralPath $marker){throw 'A Phase 3 ownership marker already exists. Stop that instance first.'}
if(Test-Path -LiteralPath $phase1){throw 'A Phase 1/2 application is already recorded. Stop it through its own launcher before starting Phase 3.'}
& (Join-Path $root 'Start-Phase1.ps1') -Profile $Profile -NoBrowser:$NoBrowser
if(-not (Test-Path -LiteralPath $phase1)){throw 'Phase 1 launcher did not create an ownership record.'}
Copy-Item -LiteralPath $phase1 -Destination $marker -ErrorAction Stop
Write-Host 'Phase 3 preview started. External polling adapters remain disabled.'
