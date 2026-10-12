param([string]$AntHome=$env:ANT_HOME)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
if(!$AntHome){throw 'Specify -AntHome (existing Ant installation)'}
& (Join-Path $AntHome 'bin/ant.bat') -f "$root/build-portable.xml" compile
if($LASTEXITCODE -ne 0){throw 'Compilation failed'}
$classes=Join-Path $root '.build/phase4-test-classes'
New-Item -ItemType Directory -Force $classes | Out-Null
$deps="$root/.build/web/WEB-INF/classes;$root/lib/runtime/*;$root/lib/compile-only/*"
& javac -encoding UTF-8 --release 8 -cp $deps -d $classes "$root/test/ai/ConsultationBehaviorTest.java" "$PSScriptRoot/rag/RagPhase4RuntimeTest.java"
if($LASTEXITCODE -ne 0){throw 'Phase4 offline test compilation failed'}
& java '-Dfile.encoding=UTF-8' -cp "$classes;$deps" rag.RagPhase4RuntimeTest
if($LASTEXITCODE -ne 0){throw 'Phase4 offline tests failed'}
