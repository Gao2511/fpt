# OFFLINE ONLY: compile existing payload builder/normalizer and test-only helpers.
param([ValidateSet('phase23-single-embedding-20261011-v1','phase23-corrected-payload-20261011-v2')]
    [string]$PlanId='phase23-single-embedding-20261011-v1')
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
$javac=(Get-Command javac -ErrorAction SilentlyContinue).Source
if (!$javac) { $javac='C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot\bin\javac.exe' }
if (!(Test-Path -LiteralPath $javac)) { throw 'JDK 17 javac is required; no request sent' }
$java=Join-Path (Split-Path -Parent $javac) 'java.exe'
$classes=Join-Path $root '.build/phase23-single-probe-classes'
New-Item -ItemType Directory -Force -Path $classes|Out-Null
$source=@('src/java/rag/RagConfig.java','src/java/rag/RagException.java','src/java/rag/EmbeddingService.java',
    'src/java/rag/VectorValues.java','src/java/rag/GeminiEmbeddingService.java',
    'test/rag/RagSingleEmbeddingPlan.java','test/rag/RagEmbeddingVectorCheck.java')|ForEach-Object { Join-Path $root $_ }
& $javac -encoding UTF-8 --release 8 -cp "$root/lib/runtime/*" -d $classes $source
if ($LASTEXITCODE -ne 0) { throw 'Offline preparation compile failed' }
& $java '-Dfile.encoding=UTF-8' -cp "$classes;$root/lib/runtime/*" rag.RagSingleEmbeddingPlan $root $PlanId
if ($LASTEXITCODE -ne 0) { throw 'Offline payload/evidence validation failed' }
Write-Output 'Offline single-embedding preparation: PASS; real API requests=0'
