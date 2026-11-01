[CmdletBinding()]
param(
    [Parameter(Mandatory)][ValidateSet(2000,2002,2004,2006,2008,2010,2012,2014,2016,2018,2020,2022,2024)][int]$Year,
    [Parameter(Mandatory)][ValidateSet('HOUSE','SENATE','GOVERNOR')][string]$Office,
    [Parameter(Mandatory)][ValidateScript({ Test-Path -LiteralPath $_ -PathType Leaf })][string]$SourceFile,
    [Parameter(Mandatory)][uri]$SourceUrl,
    [string]$LicenseId = 'UNVERIFIED',
    [Parameter(Mandatory)][string]$GeographyVersionId
)

$ErrorActionPreference = 'Stop'
if (-not $SourceUrl.IsAbsoluteUri -or $SourceUrl.Scheme -ne 'https' -or [string]::IsNullOrWhiteSpace($SourceUrl.Host)) {
    throw 'SourceUrl must be an absolute HTTPS URL for the cited primary source.'
}
if ([string]::IsNullOrWhiteSpace($GeographyVersionId)) { throw 'GeographyVersionId is required.' }
$requiredHeaders = @(
    'contest_id','candidate_id','candidate_name','party_code','state_fips','county_fips',
    'geo_type','geo_id','district_code','geography_version_id','vote_method','votes',
    'certification_status','source_as_of'
)
$resolvedSource = (Resolve-Path -LiteralPath $SourceFile).Path
$table = Import-Csv -LiteralPath $resolvedSource
if (-not $table -or $table.Count -eq 0) { throw 'Source file has no result rows.' }
$actualHeaders = @($table[0].PSObject.Properties.Name)
foreach ($header in $requiredHeaders) {
    if ($header -notin $actualHeaders) { throw "Missing normalized CSV column: $header" }
}

$seen = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
$rowNumber = 1
foreach ($row in $table) {
    $rowNumber++
    foreach ($field in @('contest_id','candidate_id','candidate_name','geo_type','geo_id','geography_version_id','vote_method','certification_status')) {
        if ([string]::IsNullOrWhiteSpace([string]$row.$field)) { throw "Row $rowNumber has an empty required field: $field" }
    }
    if ($row.geography_version_id -ne $GeographyVersionId) { throw "Row $rowNumber uses a different geography version." }
    if ($row.certification_status -ne 'CERTIFIED') { throw "Row $rowNumber is not marked CERTIFIED; only certified results can be staged." }
    if ($row.geo_type -notin @('STATE','COUNTY','PRECINCT','DISTRICT')) { throw "Row $rowNumber has an unsupported geo_type." }
    if ($row.state_fips -and $row.state_fips -notmatch '^\d{2}$') { throw "Row $rowNumber has malformed state FIPS." }
    if ($row.county_fips -and $row.county_fips -notmatch '^\d{5}$') { throw "Row $rowNumber has malformed county FIPS." }
    if ($row.votes -and $row.votes -notin @('*','\N','NA')) {
        $voteCount = 0L
        if (-not [long]::TryParse([string]$row.votes,[ref]$voteCount) -or $voteCount -lt 0) { throw "Row $rowNumber has invalid votes. Use blank for unknown; never replace it with 0." }
    }
    if ($Office -eq 'HOUSE' -and ($row.geo_type -ne 'DISTRICT' -or $row.district_code -notmatch '^\d{1,2}$')) {
        throw "Row $rowNumber is not a district result. County and precinct data require a separately validated district crosswalk before staging."
    }
    if ($row.source_as_of) {
        $parsed = [DateTimeOffset]::MinValue
        if (-not [DateTimeOffset]::TryParse([string]$row.source_as_of,[ref]$parsed)) { throw "Row $rowNumber has an invalid source_as_of timestamp." }
    }
    $identity = @($row.contest_id,$row.candidate_id,$row.geo_id,$row.geography_version_id,$row.vote_method) -join '|'
    if (-not $seen.Add($identity)) { throw "Duplicate contest/candidate/geography/method row at CSV line $rowNumber." }
}

$sha = (Get-FileHash -LiteralPath $resolvedSource -Algorithm SHA256).Hash.ToLowerInvariant()
$stageRoot = Join-Path $PSScriptRoot '..\.runtime\history-staging'
$stageRoot = [System.IO.Path]::GetFullPath($stageRoot)
New-Item -ItemType Directory -Path $stageRoot -Force | Out-Null
$stageFile = Join-Path $stageRoot "$Year-$Office-$sha.csv"
Copy-Item -LiteralPath $resolvedSource -Destination $stageFile -Force
$manifest = [ordered]@{
    year = $Year
    office = $Office
    sourceUrl = $SourceUrl.AbsoluteUri
    licenseId = $LicenseId
    rightsStatus = 'UNVERIFIED'
    importStatus = 'STAGED_NOT_IMPORTED'
    sourceFileSha256 = $sha
    geographyVersionId = $GeographyVersionId
    rows = $table.Count
    distinctContests = @($table.contest_id | Sort-Object -Unique).Count
    stagedAtUtc = [DateTimeOffset]::UtcNow.ToString('O')
    stagedFile = [System.IO.Path]::GetFileName($stageFile)
    caveat = 'Staging validates normalized structure only. Rights approval and an authorized database import are still required.'
}
$manifestPath = Join-Path $stageRoot "$Year-$Office-$sha.manifest.json"
$manifest | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $manifestPath -Encoding UTF8
Write-Host "Validated and staged $($table.Count) rows; no database import was performed."
Write-Host "Manifest: $manifestPath"
