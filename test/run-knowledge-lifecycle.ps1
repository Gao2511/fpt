param([switch]$Postgres)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
$evidence=Join-Path $root ('.build/kb57-'+[guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $evidence | Out-Null
$deps="$root/.build/web/WEB-INF/classes;$root/lib/runtime/*;$root/lib/compile-only/*"
& javac -encoding UTF-8 --release 8 -cp $deps -d $evidence "$PSScriptRoot/ai/ConsultationBehaviorTest.java" "$PSScriptRoot/rag/KnowledgeLifecycleOfflineTest.java" "$PSScriptRoot/rag/KnowledgeLifecyclePostgresTest.java"
if($LASTEXITCODE -ne 0){throw 'KB57 test compilation failed'}
if($Postgres){& java '-Dfile.encoding=UTF-8' -cp "$evidence;$deps" rag.KnowledgeLifecyclePostgresTest "$evidence/uploads"}
else {& java '-Dfile.encoding=UTF-8' -cp "$evidence;$deps" rag.KnowledgeLifecycleOfflineTest $evidence}
if($LASTEXITCODE -ne 0){throw 'KB57 safety checks failed'}
Write-Output "Evidence: $evidence"
