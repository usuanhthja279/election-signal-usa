$ErrorActionPreference='Stop'
$root=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$marker=Join-Path $root '.runtime/phase3-processes.json'
$owner=Join-Path $root '.runtime/data-preview/app-owner.json'
if(Test-Path -LiteralPath $marker){
  if(-not (Test-Path -LiteralPath $owner) -or (Get-FileHash -LiteralPath $owner -Algorithm SHA256).Hash -ne (Get-FileHash -LiteralPath $marker -Algorithm SHA256).Hash){throw 'The running application is not owned by the data preview; nothing was stopped.'}
  & (Join-Path $PSScriptRoot 'Stop-Phase3.ps1')
  Remove-Item -LiteralPath $owner -Force
}
elseif(Test-Path -LiteralPath (Join-Path $root '.runtime/phase1-processes.json')){throw 'Another application preview is running; the data preview database was not stopped.'}
$data=Join-Path $root '.runtime/data-preview/postgres'
$ctl=Join-Path $root '.runtime/pgcore/pgsql/bin/pg_ctl.exe'
if((Test-Path -LiteralPath (Join-Path $data 'PG_VERSION')) -and (Test-Path -LiteralPath $ctl)){& $ctl -D $data status *> $null;if($LASTEXITCODE -eq 0){& $ctl -D $data -m fast -w stop;if($LASTEXITCODE -ne 0){throw 'Preview database did not stop.'}}}
Write-Host 'Evidence preview stopped. Its data remain in the project runtime directory.'
