param([switch]$Postgres)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
$evidence=Join-Path $root ('.build/file-kb-'+[guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $evidence | Out-Null
$deps="$root/.build/web/WEB-INF/classes;$root/lib/runtime/*;$root/lib/compile-only/*"
& javac -encoding UTF-8 --release 8 -cp $deps -d $evidence "$PSScriptRoot/rag/FileKnowledgeOfflineTest.java" "$PSScriptRoot/rag/FileKnowledgePostgresTest.java"
if($LASTEXITCODE -ne 0){throw 'File-KB test compilation failed'}
if($Postgres){& java '-Dfile.encoding=UTF-8' -cp "$evidence;$deps" rag.FileKnowledgePostgresTest}
else{& java '-Dfile.encoding=UTF-8' -cp "$evidence;$deps" rag.FileKnowledgeOfflineTest "$evidence/knowledge"}
if($LASTEXITCODE -ne 0){throw 'File-KB checks failed'}
Write-Output "Evidence: $evidence"
