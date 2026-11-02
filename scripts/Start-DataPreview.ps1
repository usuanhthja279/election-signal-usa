param([ValidateRange(1024,65535)][int]$Port=55460,[switch]$NoBrowser,[switch]$SkipForecastRefresh)
$ErrorActionPreference='Stop'
$root=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$base=Join-Path $root '.runtime/data-preview'
$data=Join-Path $base 'postgres'
$secretPath=Join-Path $base 'password.dpapi'
$source=Join-Path $root '.runtime/history-sources/senate-v8-original.csv'
$pg=Join-Path $root '.runtime/pgcore/pgsql/bin'
if(-not (Test-Path -LiteralPath $source -PathType Leaf)){throw 'Download the verified MIT Senate V8 original CSV first; see docs/HISTORY_IMPORT_GUIDE.md.'}
if(-not (Test-Path -LiteralPath (Join-Path $pg 'initdb.exe'))){throw 'Project-local native PostgreSQL binaries were not found.'}
if(-not $env:JAVA_HOME){$jdk=Get-ChildItem (Join-Path $root '.runtime/jdk') -Directory | Select-Object -First 1;if($jdk){$env:JAVA_HOME=$jdk.FullName}}
if(-not $env:JAVA_HOME){throw 'Java 21 is required.'}
$java=Join-Path $env:JAVA_HOME 'bin/java.exe'
New-Item -ItemType Directory -Path $base -Force | Out-Null
$newCluster=-not (Test-Path -LiteralPath (Join-Path $data 'PG_VERSION'))
if($newCluster){
  if((Test-Path -LiteralPath $data) -or (Test-Path -LiteralPath $secretPath)){throw 'Incomplete preview database exists; refusing to overwrite it.'}
  if(Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue){throw "Port $Port is already in use."}
  $password=[guid]::NewGuid().ToString('N')+[guid]::NewGuid().ToString('N')
  $secure=ConvertTo-SecureString $password -AsPlainText -Force
  $secure | ConvertFrom-SecureString | Set-Content -LiteralPath $secretPath -Encoding ascii
  $pwFile=Join-Path $base 'initdb-password.tmp'
  try {Set-Content -LiteralPath $pwFile -Value $password -NoNewline -Encoding ascii;& (Join-Path $pg 'initdb.exe') -D $data -U election_preview --auth=scram-sha-256 "--pwfile=$pwFile" --encoding=UTF8 --no-locale;if($LASTEXITCODE -ne 0){throw 'Database initialization failed.'}} finally {if(Test-Path -LiteralPath $pwFile){Remove-Item -LiteralPath $pwFile -Force}}
} else {
  if(-not (Test-Path -LiteralPath $secretPath -PathType Leaf)){throw 'Preview database password is missing; refusing to alter the cluster.'}
  $secure=(Get-Content -LiteralPath $secretPath -Raw).Trim() | ConvertTo-SecureString
  $ptr=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
  try {$password=[Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)} finally {[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)}
}
& (Join-Path $pg 'pg_ctl.exe') -D $data status *> $null
if($LASTEXITCODE -ne 0){
  if(Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue){throw "Port $Port is already in use."}
  & (Join-Path $pg 'pg_ctl.exe') -D $data -l (Join-Path $base 'postgres.log') -o "-p $Port -h 127.0.0.1" -w start
  if($LASTEXITCODE -ne 0){throw 'Preview PostgreSQL failed to start.'}
}
$pidFile=Join-Path $data 'postmaster.pid'
if(-not (Test-Path -LiteralPath $pidFile)){throw 'Preview database ownership record is unavailable.'}
$pidLines=Get-Content -LiteralPath $pidFile
if($pidLines.Count -lt 4 -or $pidLines[3].Trim() -ne [string]$Port){throw 'The preview cluster is running on a different port; refusing to connect.'}
$env:PGPASSWORD=$password
$env:ELECTION_DB_URL="jdbc:postgresql://127.0.0.1:$Port/election_preview"
$env:ELECTION_DB_USER='election_preview'
$env:ELECTION_DB_PASSWORD=$password
if($newCluster){& (Join-Path $pg 'createdb.exe') -h 127.0.0.1 -p $Port -U election_preview election_preview;if($LASTEXITCODE -ne 0){throw 'Preview database creation failed.'}}
$jar=Join-Path $root '.runtime/history-import-build/backend-0.0.1-SNAPSHOT.jar'
Push-Location (Join-Path $root 'backend')
try {
  & .\mvnw.cmd -q '-Delection.build.directory=../.runtime/history-import-build' -DskipTests package
  if($LASTEXITCODE -ne 0){throw 'Application build failed.'}
  & $java -jar $jar --spring.profiles.active=local --spring.main.web-application-type=none "--election.medsl-senate-import=$source"
  if($LASTEXITCODE -ne 0){throw 'MIT history import failed.'}
  if(-not $SkipForecastRefresh){foreach($dataset in @('senate','governors','house')){& $java -jar $jar --spring.profiles.active=local --spring.main.web-application-type=none "--election.forecast-import=$dataset";if($LASTEXITCODE -ne 0){Write-Warning "Forecast dataset $dataset could not be refreshed; existing snapshots remain available."}}}
} finally {Pop-Location}
if(Test-Path -LiteralPath (Join-Path $root '.runtime/phase3-processes.json')){& (Join-Path $PSScriptRoot 'Stop-Phase3.ps1')}
if(Test-Path -LiteralPath (Join-Path $root '.runtime/phase1-processes.json')){throw 'A different preview is running. Stop it using its own launcher before starting this one.'}
& (Join-Path $PSScriptRoot 'Start-Phase3.ps1') -Profile local -NoBrowser:$NoBrowser
Copy-Item -LiteralPath (Join-Path $root '.runtime/phase3-processes.json') -Destination (Join-Path $base 'app-owner.json') -Force
Write-Host 'Evidence preview is running with a dedicated localhost database. Live results and exit surveys remain disconnected.'
