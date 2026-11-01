[CmdletBinding()]
param(
  [Parameter(Mandatory)][ValidateScript({Test-Path -LiteralPath $_ -PathType Leaf})][string]$DistrictCsv,
  [Parameter(Mandatory)][uri]$SourceUrl,
  [Parameter(Mandatory)][ValidateScript({Test-Path -LiteralPath $_ -PathType Leaf})][string]$SourceMetadata
)
$ErrorActionPreference='Stop'
if(-not $SourceUrl.IsAbsoluteUri -or $SourceUrl.Scheme -ne 'https'){throw 'SourceUrl must be an absolute HTTPS URL.'}
$metadata=Get-Content -Raw -LiteralPath $SourceMetadata | ConvertFrom-Json
if($metadata.providerId -ne 'CENSUS_BAS_2026_CD119' -or $metadata.geographyVersion -ne 'CENSUS_BAS_2026_CD119' -or $metadata.rightsStatus -ne 'APPROVED' -or $metadata.legalReviewer -eq $null -or [string]::IsNullOrWhiteSpace([string]$metadata.legalReviewer)){
  throw 'Registry publication requires an explicit human rights review and the correct 119th Congress version.'
}
$required=@('state','state_fips','district_code','geoid','display_name','source_feature_id')
$rows=Import-Csv -LiteralPath $DistrictCsv
if(-not $rows -or $rows.Count -ne 435){throw 'The voting-seat district registry must contain exactly 435 rows; DC and territories are excluded.'}
foreach($name in $required){if($name -notin $rows[0].PSObject.Properties.Name){throw "Missing column $name"}}
$keys=[Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
$states=[Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
foreach($row in $rows){
  if($row.state -notmatch '^[A-Z]{2}$' -or $row.state_fips -notmatch '^\d{2}$' -or $row.district_code -notmatch '^\d{2}$' -or $row.geoid -notmatch '^\d{4}$' -or $row.geoid.Substring(0,2) -ne $row.state_fips){throw "Invalid or width-losing state/district code: $($row | ConvertTo-Json -Compress)"}
  if([string]::IsNullOrWhiteSpace($row.display_name) -or [string]::IsNullOrWhiteSpace($row.source_feature_id)){throw 'Every district requires a source feature and official name.'}
  if(-not $keys.Add("$($row.state)|$($row.district_code)")){throw "Duplicate district $($row.state)-$($row.district_code)"}
  [void]$states.Add($row.state)
}
if($states.Count -ne 50){throw "Expected 50 voting states, received $($states.Count)."}
$sha=(Get-FileHash -LiteralPath $DistrictCsv -Algorithm SHA256).Hash.ToLowerInvariant()
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\.runtime\registry-staging'))
New-Item -ItemType Directory -Path $root -Force | Out-Null
$destination=Join-Path $root "2026-CD119-$sha.csv"
Copy-Item -LiteralPath (Resolve-Path -LiteralPath $DistrictCsv).Path -Destination $destination
$manifest=[ordered]@{providerId='CENSUS_BAS_2026_CD119';geographyVersion='CENSUS_BAS_2026_CD119';sourceUrl=$SourceUrl.AbsoluteUri;sourceFileSha256=$sha;rightsStatus='APPROVED_BY_OPERATOR';districtRows=$rows.Count;states=$states.Count;stagedFile=[IO.Path]::GetFileName($destination);stageStatus='VALIDATED_STAGED_NOT_PUBLISHED';stagedAtUtc=[DateTimeOffset]::UtcNow.ToString('O')}
$manifest | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $root "2026-CD119-$sha.manifest.json") -Encoding utf8
Write-Host "Validated and staged $($rows.Count) voting districts with SHA-256 $sha. Database publication remains a separate reviewed operation."
