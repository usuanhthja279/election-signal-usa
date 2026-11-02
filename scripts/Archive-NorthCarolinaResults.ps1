param()
$ErrorActionPreference='Stop'
if(-not $env:ELECTION_DB_URL -or -not $env:ELECTION_DB_USER -or -not $env:ELECTION_DB_PASSWORD){throw 'Set ELECTION_DB_URL, ELECTION_DB_USER, and ELECTION_DB_PASSWORD for the intended local database.'}
$root=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
if(-not $env:JAVA_HOME){$jdk=Get-ChildItem (Join-Path $root '.runtime/jdk') -Directory -ErrorAction SilentlyContinue | Select-Object -First 1;if($jdk){$env:JAVA_HOME=$jdk.FullName}}
$java=if($env:JAVA_HOME){Join-Path $env:JAVA_HOME 'bin/java.exe'}else{(Get-Command java -ErrorAction Stop).Source}
Push-Location (Join-Path $root 'backend')
try {
  & .\mvnw.cmd -q '-Delection.build.directory=../.runtime/nc-archive-build' -DskipTests package
  if($LASTEXITCODE -ne 0){throw 'Backend build failed.'}
  & $java -jar '../.runtime/nc-archive-build/backend-0.0.1-SNAPSHOT.jar' --spring.profiles.active=local --spring.main.web-application-type=none --election.nc-results-archive=true
  if($LASTEXITCODE -ne 0){throw 'North Carolina archive failed; check source approval and connectivity.'}
} finally {Pop-Location}
