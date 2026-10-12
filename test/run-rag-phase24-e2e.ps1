# Requires a NEW isolated database. Only injected mock AI providers are used.
param([string]$ReportPath)
$ErrorActionPreference='Stop'
if($env:RAG_TEST_DATABASE_APPROVED -ne 'true' -or $env:RAG_ENVIRONMENT -ne 'development' -or $env:DB_HOST -ne '127.0.0.1' -or $env:DB_NAME -ne 'fpt_rag_test' -or $env:RAG_ENABLED -ne 'false' -or $env:RAG_EMBEDDING_CALLS_APPROVED -ne 'false'){throw 'Require approved isolated loopback DB and disabled live RAG/embedding flags'}
$root=Split-Path -Parent $PSScriptRoot
if(!$ReportPath){$ReportPath=Join-Path $root ('.build/phase24-e2e-'+[guid]::NewGuid().ToString('N')+'.json')}
if(Test-Path -LiteralPath $ReportPath){throw 'Refusing to overwrite previous test evidence'}
$classes=Join-Path $root '.build/phase24-test-classes'
New-Item -ItemType Directory -Path $classes -Force | Out-Null
$deps="$root/.build/web/WEB-INF/classes;$root/lib/runtime/*;$root/lib/compile-only/*"
& javac -encoding UTF-8 --release 8 -cp $deps -d $classes "$root/test/ai/ConsultationBehaviorTest.java" "$root/test/rag/RagEndToEndTest.java"
if($LASTEXITCODE -ne 0){throw 'E2E compilation failed'}
& java '-Dfile.encoding=UTF-8' -cp "$classes;$deps" rag.RagEndToEndTest $ReportPath
if($LASTEXITCODE -ne 0){throw "E2E failed; sanitized report: $ReportPath"}
Write-Output "Sanitized E2E result: $ReportPath"
