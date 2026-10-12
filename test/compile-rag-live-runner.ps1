param([string]$AntHome=$env:ANT_HOME)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
& "$root/tools/bootstrap-libs.ps1" -VerifyOnly
if($AntHome){$ant=Join-Path $AntHome 'bin/ant.bat'}else{$ant=(Get-Command ant -ErrorAction Stop).Source}
& $ant -f "$root/build-portable.xml" compile
if($LASTEXITCODE -ne 0){throw 'Application compile failed'}
$classes=Join-Path $root '.build/phase241-test-classes'
New-Item -ItemType Directory -Path $classes -Force | Out-Null
$deps="$root/.build/web/WEB-INF/classes;$root/lib/runtime/*;$root/lib/compile-only/*"
$sources=@("$root/test/ai/ConsultationBehaviorTest.java")+@(Get-ChildItem "$root/test/rag/live/*.java" | ForEach-Object FullName)
& javac -encoding UTF-8 --release 17 -cp $deps -d $classes @sources
if($LASTEXITCODE -ne 0){throw 'Live runner compile failed'}
Write-Output 'Live runner compiled for Java 17; no provider or database request made.'
