[CmdletBinding()]
param([Parameter(Mandatory)][ValidateScript({Test-Path -LiteralPath $_ -PathType Leaf})][string]$SourceFile)
$ErrorActionPreference='Stop'
$resolved=(Resolve-Path -LiteralPath $SourceFile).Path
$file=Get-Item -LiteralPath $resolved
if($file.Length -gt 10485760){throw 'Input exceeds 10 MiB.'}
$document=Get-Content -LiteralPath $resolved -Raw | ConvertFrom-Json
if(-not ($document.isTestData -is [bool]) -or $document.isTestData){throw 'Production staging requires isTestData=false; fictional fixtures belong only in tests.'}
foreach($field in @('providerId','sourceId','publicationUrl','licenseId','attribution','rightsEvidenceUrl')){
  if([string]::IsNullOrWhiteSpace([string]$document.$field)){throw "Missing required source field: $field"}
}
foreach($field in @('publicationUrl','rightsEvidenceUrl')){
  $uri=$null
  if(-not [uri]::TryCreate([string]$document.$field,[System.UriKind]::Absolute,[ref]$uri) -or $uri.Scheme -ne 'https'){throw "$field must be an absolute HTTPS URL."}
}
if($document.providerId -notmatch '^[A-Za-z0-9_-]{1,80}$' -or $document.sourceId -notmatch '^[A-Za-z0-9_-]{1,80}$'){throw 'Invalid provider or source ID.'}
if(-not $document.records -or @($document.records).Count -eq 0){throw 'At least one normalized record is required.'}
foreach($record in $document.records){
  if($record.kind -notin @('survey','average','forecast')){throw 'Each record kind must be survey, average or forecast.'}
  if($record.year -lt 2000 -or $record.year -gt 2100){throw 'Invalid election year.'}
  if($record.office -notin @('HOUSE','SENATE','GOVERNOR','GENERIC')){throw 'Invalid office.'}
  if($record.district -and ($record.office -ne 'HOUSE' -or $record.state -notmatch '^[A-Z]{2}$' -or $record.district -notmatch '^\d{2}$')){throw 'A district requires a state and House office.'}
  if($record.sampleSize -ne $null -and (($record.sampleSize -isnot [int] -and $record.sampleSize -isnot [long]) -or $record.sampleSize -le 0)){throw 'Sample size must be a positive integer or null.'}
  if($record.kind -eq 'survey'){
    foreach($field in @('pollster','questionIdentity','matchupIdentity')){if([string]::IsNullOrWhiteSpace([string]$record.$field)){throw "Survey missing $field."}}
    if(-not $record.answers -or @($record.answers).Count -eq 0){throw 'Survey needs at least one answer.'}
    $choices=[System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
    foreach($answer in $record.answers){if([string]::IsNullOrWhiteSpace([string]$answer.choiceId) -or -not $choices.Add([string]$answer.choiceId)){throw 'Survey choice IDs must be present and unique.'};if($null -ne $answer.percent -and ($answer.percent -isnot [ValueType] -or $answer.percent -lt 0 -or $answer.percent -gt 100)){throw 'Response percentages must be between 0 and 100 or null.'}}
  } else {
    $published=[DateTimeOffset]::MinValue
    if(-not [DateTimeOffset]::TryParse([string]$record.publishedAt,[ref]$published)){throw 'Average and forecast records require a publication timestamp.'}
    if([string]::IsNullOrWhiteSpace([string]$record.raceId)){throw 'Average and forecast records require a race ID.'}
    if($record.kind -eq 'average' -and ($null -eq $record.averageShare -or $record.averageShare -lt 0 -or $record.averageShare -gt 100)){throw 'Average share must be between 0 and 100.'}
    if($record.kind -eq 'forecast' -and $null -ne $record.winProbability -and ($record.winProbability -lt 0 -or $record.winProbability -gt 1)){throw 'Forecast probability must be between 0 and 1 or null.'}
  }
}
Write-Host "Validated $(@($document.records).Count) records structurally. Rights and source authenticity still require independent review."
