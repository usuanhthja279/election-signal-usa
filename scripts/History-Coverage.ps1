[CmdletBinding()]
param([uri]$ApiBase=[uri]'http://127.0.0.1:8080')
$ErrorActionPreference='Stop'
$url=[uri]::new($ApiBase,'/api/v1/history/coverage')
$response=Invoke-RestMethod -Uri $url -TimeoutSec 10
if($response.isTestData -ne $false -or $response.dataMode -ne 'HISTORICAL_CERTIFIED'){throw 'Unexpected history coverage contract.'}
$response.data | Sort-Object year,office | Format-Table year,office,status,contests,resultRows,geographyTypes -AutoSize
