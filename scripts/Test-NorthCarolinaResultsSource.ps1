param()
$ErrorActionPreference='Stop'
$root=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
if(-not $env:JAVA_HOME){$jdk=Get-ChildItem (Join-Path $root '.runtime/jdk') -Directory -ErrorAction SilentlyContinue | Select-Object -First 1;if($jdk){$env:JAVA_HOME=$jdk.FullName}}
if(-not $env:JAVA_HOME){throw 'Java 21 is required.'}
Push-Location (Join-Path $root 'backend')
try {& .\mvnw.cmd -q '-Delection.build.directory=../.runtime/nc-preflight-build' -DskipTests package;if($LASTEXITCODE -ne 0){throw 'Preflight build failed.'};& (Join-Path $env:JAVA_HOME 'bin/java.exe') -jar '../.runtime/nc-preflight-build/backend-0.0.1-SNAPSHOT.jar' --spring.profiles.active=demo --spring.main.web-application-type=none --election.nc-results-preflight=true;if($LASTEXITCODE -ne 0){throw 'North Carolina source preflight failed.'}} finally {Pop-Location}
