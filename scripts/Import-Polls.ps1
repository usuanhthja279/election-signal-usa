[CmdletBinding()]
param([Parameter(Mandatory)][ValidateScript({Test-Path -LiteralPath $_ -PathType Leaf})][string]$SourceFile)
$ErrorActionPreference='Stop'
& (Join-Path $PSScriptRoot 'Validate-Polls.ps1') -SourceFile $SourceFile
$root=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$source=(Resolve-Path -LiteralPath $SourceFile).Path
$hash=(Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash.ToLowerInvariant()
$archive=Join-Path $root '.runtime/polling-staging'
New-Item -ItemType Directory -Force -Path $archive | Out-Null
$target=Join-Path $archive "$hash.json"
if(-not (Test-Path -LiteralPath $target)){Copy-Item -LiteralPath $source -Destination $target}
if((Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash.ToLowerInvariant() -ne $hash){throw 'Existing archive checksum does not match.'}
Write-Host "Staged immutable input $hash. No database import or rights approval was performed."
