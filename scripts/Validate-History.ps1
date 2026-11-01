[CmdletBinding()]
param([Parameter(Mandatory)][ValidateScript({Test-Path -LiteralPath $_ -PathType Leaf})][string]$ManifestPath)
$ErrorActionPreference='Stop'
$manifestFile=(Resolve-Path -LiteralPath $ManifestPath).Path
$manifest=Get-Content -Raw -LiteralPath $manifestFile | ConvertFrom-Json
if($manifest.importStatus -ne 'STAGED_NOT_IMPORTED'){throw 'Manifest is not an unmodified staging manifest.'}
$source=Join-Path (Split-Path $manifestFile -Parent) ([string]$manifest.stagedFile)
if(-not(Test-Path -LiteralPath $source -PathType Leaf)){throw 'Staged source file is missing.'}
$hash=(Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash.ToLowerInvariant()
if($hash -ne $manifest.sourceFileSha256){throw 'Staged source checksum does not match its manifest.'}
$rows=Import-Csv -LiteralPath $source
if($rows.Count -ne [int]$manifest.rows){throw 'Staged row count does not match its manifest.'}
[pscustomobject]@{
  Status='VALIDATED_STAGING_ONLY'
  Year=$manifest.year
  Office=$manifest.office
  Rows=$rows.Count
  DistinctContests=@($rows.contest_id | Sort-Object -Unique).Count
  SourceSHA256=$hash
  RightsStatus=$manifest.rightsStatus
  ImportStatus=$manifest.importStatus
  Note='This verifies staging integrity only; rights review and database publication are separate gates.'
} | Format-List
