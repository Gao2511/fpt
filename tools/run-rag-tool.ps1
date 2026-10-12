param([ValidateSet('check','verify-model','index')][string]$Operation='check',[int]$Limit=1)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
$classes=Join-Path $root '.build/operator-classes'
New-Item -ItemType Directory -Path $classes -Force | Out-Null
$deps="$root/.build/web/WEB-INF/classes;$root/lib/runtime/*"
& javac -encoding UTF-8 --release 8 -cp $deps -d $classes "$PSScriptRoot/java/rag/RagTool.java"
if($LASTEXITCODE -ne 0){throw 'Build operator tool failed; run portable build first'}
& java '-Dfile.encoding=UTF-8' -cp "$classes;$deps" rag.RagTool $Operation $Limit
if($LASTEXITCODE -ne 0){throw 'RAG operator command failed; no raw provider/database response was logged'}
