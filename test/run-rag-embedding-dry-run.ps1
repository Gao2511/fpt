# Offline corpus/request inspection only. No live mode, secrets, Docker mutations, JDBC or HTTP calls.
param()
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
$classes=Join-Path $root '.build/phase23-dry-run-classes'
New-Item -ItemType Directory -Force -Path $classes | Out-Null
$deps="$root/.build/web/WEB-INF/classes;$root/lib/runtime/*;$root/lib/compile-only/*"
& javac -encoding UTF-8 --release 8 -cp $deps -d $classes "$PSScriptRoot/rag/RagEmbeddingDryRun.java"
if($LASTEXITCODE-ne 0){throw 'Dry-run compilation failed; build existing source first'}
& java '-Dfile.encoding=UTF-8' -cp "$classes;$deps" rag.RagEmbeddingDryRun "$PSScriptRoot/rag/fixtures/phase23-embedding-plan.json" "$root/.build/phase23-dry-run.json"
if($LASTEXITCODE-ne 0){throw 'Embedding dry-run validation failed'}
