param([string]$PgBin)
$ErrorActionPreference='Stop'
$root=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
foreach($name in @('Start-Phase3.ps1','Stop-Phase3.ps1','Import-Polls.ps1','Validate-Polls.ps1','Test-Phase3.ps1')){
  $tokens=$null;$errors=$null;[System.Management.Automation.Language.Parser]::ParseFile((Join-Path $PSScriptRoot $name),[ref]$tokens,[ref]$errors)|Out-Null
  if($errors.Count){throw "PowerShell syntax error in $name : $($errors -join '; ')"}
}
Push-Location (Join-Path $root 'frontend')
try { & npm.cmd run typecheck; if($LASTEXITCODE -ne 0){throw 'Frontend typecheck failed.'}; & npm.cmd test; if($LASTEXITCODE -ne 0){throw 'Frontend tests failed.'} } finally {Pop-Location}
if($PgBin){& (Join-Path $PSScriptRoot 'Test-Postgres.ps1') -PgBin $PgBin}
else{Write-Host 'PostgreSQL checks skipped; supply -PgBin to run the disposable database workflow.'}
