param([string]$TomcatHome='D:\Subject FPT\PRJ301\apache-tomcat-9.0.118-windows-x64\apache-tomcat-9.0.118',[switch]$Browser)
$ErrorActionPreference='Stop'
$repoRoot=Split-Path -Parent $PSScriptRoot
$classes=Join-Path $env:TEMP ('fpt-cms-tests-'+[guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $classes | Out-Null
$deps=(Join-Path $repoRoot 'build/web/WEB-INF/lib/*')+';'+(Join-Path $repoRoot 'lib/runtime/*')+';'+$TomcatHome+'/lib/*;'+$TomcatHome+'/bin/tomcat-juli.jar'
$sources=@(Get-ChildItem -LiteralPath (Join-Path $repoRoot 'src/java'),(Join-Path $repoRoot 'test') -Recurse -Filter '*.java' | Where-Object { $_.FullName -notmatch '[\\/]test[\\/]rag[\\/]live[\\/]' -and $_.Name -ne 'TestController.java' } | ForEach-Object FullName)
& javac -encoding UTF-8 --release 8 -cp $deps -d $classes $sources
if($LASTEXITCODE-ne 0){throw 'CMS compilation failed'}
$env:DB_HOST='127.0.0.1';$env:DB_PORT='1';$env:DB_NAME='offline_fixture';$env:DB_USER='FAKE_TEST_USER';$env:DB_PASSWORD='FAKE_TEST_PASSWORD';$env:AI_ENCRYPTION_KEY='FAKE_OFFLINE_TEST_ENCRYPTION_KEY';$env:RAG_ENABLED='false'
foreach($suite in @('cms.CmsSecurityTest','cms.CmsBehaviorTest','cms.LocalDatabaseTest','cms.EditorLayoutTest')){& java '-Dfile.encoding=UTF-8' '-DAI_ENCRYPTION_KEY=FAKE_OFFLINE_TEST_ENCRYPTION_KEY' -cp "$classes;$deps" $suite;if($LASTEXITCODE-ne 0){throw "Failed: $suite"}}
if($Browser){$env:FPT_CMS_TEST_CLASSPATH="$classes;$deps";& node (Join-Path $repoRoot 'test-e2e/cms-browser-test.cjs');if($LASTEXITCODE-ne 0){throw 'CMS browser tests failed'}}
Write-Output 'CMS tests passed using isolated JDBC fixtures. No production database or email/API calls.'
