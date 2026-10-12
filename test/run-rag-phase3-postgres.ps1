# Explicit opt-in Phase 3 PostgreSQL integration suite. Zero Gemini API calls.
param()
$ErrorActionPreference='Stop'
if($env:RAG_TEST_DATABASE_APPROVED -ne 'true' -or $env:RAG_ENVIRONMENT -ne 'development' -or $env:DB_HOST -ne '127.0.0.1' -or $env:DB_NAME -ne 'fpt_rag_test'){throw 'Require a fresh approved loopback fpt_rag_test database; existing data is preserved'}
$root=Split-Path -Parent $PSScriptRoot
$deps="$root/.build/web/WEB-INF/classes;$root/lib/runtime/*;$root/lib/compile-only/*"
$classes=Join-Path $root '.build/phase3-postgres-test-classes'
New-Item -ItemType Directory -Path $classes -Force | Out-Null
& javac -encoding UTF-8 --release 8 -cp $deps -d $classes "$PSScriptRoot/rag/RagPhase3PostgresTest.java"
if($LASTEXITCODE -ne 0){throw 'Phase 3 PostgreSQL test compilation failed'}
& java '-Dfile.encoding=UTF-8' -cp "$classes;$deps" rag.RagPhase3PostgresTest
if($LASTEXITCODE -ne 0){throw 'Phase 3 PostgreSQL integration test failed'}
