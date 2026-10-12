# Option B: operator supplies a NEW EMPTY isolated local fpt_rag_test DB and process env privately.
# Never load .local/runtime.json. Use a dedicated disposable test DB role, not the website DB.
param()
$ErrorActionPreference='Stop'
if($env:RAG_TEST_DATABASE_APPROVED-ne 'true' -or $env:RAG_ENVIRONMENT-ne 'development' -or $env:DB_HOST-ne '127.0.0.1' -or $env:DB_NAME-ne 'fpt_rag_test'){throw 'Require approved empty isolated loopback fpt_rag_test and explicit test environment'}
$root=Split-Path -Parent $PSScriptRoot
$deps="$root/.build/web/WEB-INF/classes;$root/lib/runtime/*;$root/lib/compile-only/*"
$classes=Join-Path $root '.build/local-test-setup'
New-Item -ItemType Directory -Force -Path $classes | Out-Null
& javac -encoding UTF-8 --release 8 -cp $deps -d $classes "$root/test/rag/RagLocalDatabaseSetup.java"
if($LASTEXITCODE-ne 0){throw 'Test setup compilation failed'}
& java '-Dfile.encoding=UTF-8' -cp "$classes;$deps" rag.RagLocalDatabaseSetup $root
if($LASTEXITCODE-ne 0){throw 'Empty isolated DB setup failed/refused; no test executed'}
& "$PSScriptRoot/run-rag-postgres-tests.ps1"
