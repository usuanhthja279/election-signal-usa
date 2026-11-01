$ErrorActionPreference = 'Stop'
$Root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Push-Location $Root
try { & node --test; if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE } }
finally { Pop-Location }
