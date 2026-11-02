param([Parameter(Mandatory)][ValidateScript({Test-Path -LiteralPath $_ -PathType Leaf})][string]$SourceFile)
$ErrorActionPreference='Stop'
if(-not $env:ELECTION_DB_URL -or -not $env:ELECTION_DB_USER -or -not $env:ELECTION_DB_PASSWORD){throw 'Set ELECTION_DB_URL, ELECTION_DB_USER, and ELECTION_DB_PASSWORD for the intended local database.'}
$source=(Resolve-Path -LiteralPath $SourceFile).Path
$root=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
if(-not $env:JAVA_HOME){$jdk=Get-ChildItem (Join-Path $root '.runtime/jdk') -Directory -ErrorAction SilentlyContinue | Select-Object -First 1;if($jdk){$env:JAVA_HOME=$jdk.FullName}}
$java=if($env:JAVA_HOME){Join-Path $env:JAVA_HOME 'bin/java.exe'}else{(Get-Command java -ErrorAction Stop).Source}
Push-Location (Join-Path $root 'backend')
try {
  & .\mvnw.cmd -q '-Delection.build.directory=../.runtime/phase3-survey-build' -DskipTests package
  if($LASTEXITCODE -ne 0){throw 'Backend build failed.'}
  & $java -jar '../.runtime/phase3-survey-build/backend-0.0.1-SNAPSHOT.jar' --spring.profiles.active=local --spring.main.web-application-type=none "--election.survey-import=$source"
  if($LASTEXITCODE -ne 0){throw 'Survey import failed. Verify the individual source rights and input.'}
} finally {Pop-Location}
