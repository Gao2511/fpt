param([string]$AntHome=$env:ANT_HOME)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
& "$root/tools/bootstrap-libs.ps1" -VerifyOnly
if($AntHome){$ant=Join-Path $AntHome 'bin/ant.bat'}else{$ant=(Get-Command ant -ErrorAction Stop).Source}
& $ant -f "$root/build-portable.xml" compile
if($LASTEXITCODE -ne 0){throw 'Compile failed'}
$classes=Join-Path $root '.build/phase2-test-classes'
New-Item -ItemType Directory -Force -Path $classes | Out-Null
$deps="$root/.build/web/WEB-INF/classes;$root/lib/runtime/*;$root/lib/compile-only/*"
& javac -encoding UTF-8 --release 8 -cp $deps -d $classes "$root/test/ai/ConsultationBehaviorTest.java" "$root/test/rag/RagBehaviorTest.java" "$root/test/rag/RagPhase2CorpusTest.java" "$root/test/rag/RagQualityTest.java" "$root/test/rag/RagSubjectSelectionTest.java"
if($LASTEXITCODE -ne 0){throw 'Compile offline test failed'}
foreach($suite in @('rag.RagBehaviorTest','rag.RagPhase2CorpusTest','rag.RagQualityTest','rag.RagSubjectSelectionTest')){
    & java '-Dfile.encoding=UTF-8' -cp "$classes;$deps" $suite $root
    if($LASTEXITCODE -ne 0){throw "Offline test failed: $suite"}
}
# Every provider/DAO used by these suites is injected and mocked. No credentials are loaded.
