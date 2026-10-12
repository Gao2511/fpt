$ErrorActionPreference='Stop'
if($env:RAG_TEST_DATABASE_APPROVED -ne 'true' -or $env:RAG_ENVIRONMENT -ne 'development' -or $env:DB_HOST -ne '127.0.0.1' -or $env:DB_NAME -ne 'fpt_rag_test' -or $env:RAG_ENABLED -ne 'false'){throw 'Approved isolated loopback DB required'}
$root=Split-Path -Parent $PSScriptRoot
$classes=Join-Path $root '.build/phase4-test-classes'
New-Item -ItemType Directory -Force $classes | Out-Null
$deps="$root/.build/web/WEB-INF/classes;$root/lib/runtime/*;$root/lib/compile-only/*"
& javac -encoding UTF-8 --release 8 -cp $deps -d $classes "$root/test/ai/ConsultationBehaviorTest.java" "$PSScriptRoot/rag/RagPhase4RuntimeTest.java" "$PSScriptRoot/rag/RagPhase4PostgresTest.java"
if($LASTEXITCODE -ne 0){throw 'Phase4 PostgreSQL compilation failed'}
& java '-Dfile.encoding=UTF-8' -cp "$classes;$deps" rag.RagPhase4PostgresTest
if($LASTEXITCODE -ne 0){throw 'Phase4 PostgreSQL tests failed'}
