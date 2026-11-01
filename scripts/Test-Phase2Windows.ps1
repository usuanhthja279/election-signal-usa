$ErrorActionPreference='Stop'
$scripts=@('Start-Phase1.ps1','Stop-Phase1.ps1','scripts/Import-History.ps1','scripts/Validate-History.ps1','scripts/History-Coverage.ps1','scripts/Import-ElectionRegistry.ps1')
foreach($relative in $scripts){
  $path=Join-Path $PSScriptRoot $relative
  $tokens=$null;$errors=$null
  [System.Management.Automation.Language.Parser]::ParseFile($path,[ref]$tokens,[ref]$errors) | Out-Null
  if($errors.Count){throw ('PowerShell syntax error in ' + $relative + ': ' + ($errors -join '; '))}
}
$node=Get-Command node -ErrorAction Stop
$nodeVersion=[version]((& $node.Source --version).TrimStart('v'))
if($nodeVersion -lt [version]'20.9'){throw "Node.js 20.9+ is required; found $nodeVersion"}
$java=Get-Command java -ErrorAction Stop
$javaOutput=& $java.Source -version 2>&1 | Out-String
if($javaOutput -notmatch 'version ""?21\.'){throw "Java 21 is required; found $javaOutput"}
$stopText=Get-Content -Raw -LiteralPath (Join-Path $PSScriptRoot 'Stop-Phase1.ps1')
if($stopText -notmatch 'StartTime\.ToUniversalTime\(\)\.Ticks' -or $stopText -notmatch 'CommandLine -notmatch' -or $stopText -notmatch 'Stop-Process'){
  throw 'Stop-Phase1 process-ownership checks are missing.'
}
Write-Host "Windows Phase 2 smoke prerequisites pass: Node $nodeVersion and Java 21."
