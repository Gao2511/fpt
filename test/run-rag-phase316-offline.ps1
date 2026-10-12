param([string]$AntHome=$env:ANT_HOME)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
if(!$AntHome){$AntHome='D:/Subject FPT/PRJ301/NetBeans-13/netbeans/extide/ant'}
& "$AntHome/bin/ant.bat" -f "$root/build-portable.xml" compile
if($LASTEXITCODE -ne 0){throw 'Compile failed'}
$classes="$root/.build/phase316-offline-classes"
New-Item -ItemType Directory -Force -Path $classes | Out-Null
$deps="$root/.build/web/WEB-INF/classes;$root/lib/runtime/*;$root/lib/compile-only/*"
& javac -encoding UTF-8 --release 8 -cp $deps -d $classes "$PSScriptRoot/rag/RagPhase316OfflineTest.java" "$PSScriptRoot/rag/RagAdminKbTest.java" "$PSScriptRoot/cms/FakeDatabase.java"
if($LASTEXITCODE -ne 0){throw 'Test compile failed'}
& java '-Dfile.encoding=UTF-8' -cp "$classes;$deps" rag.RagPhase316OfflineTest
if($LASTEXITCODE -ne 0){throw 'Phase316 offline failed'}
# Admin suite installs a JDBC fixture, deregisters all real drivers and opens no socket.
# Supply explicit synthetic config so DBUtils never consults a production environment/fallback.
$fixture=@{DB_HOST='127.0.0.1';DB_PORT='1';DB_NAME='fpt_rag_test';DB_USER='offline_fixture';DB_PASSWORD='offline_fixture_only';DB_SSLMODE='disable';RAG_ENVIRONMENT='development';RAG_ENABLED='false'}
$previous=@{}
try {
    foreach($key in $fixture.Keys){$previous[$key]=[Environment]::GetEnvironmentVariable($key,'Process');[Environment]::SetEnvironmentVariable($key,$fixture[$key],'Process')}
    & java '-Dfile.encoding=UTF-8' -cp "$classes;$deps" rag.RagAdminKbTest
    if($LASTEXITCODE -ne 0){throw 'Admin KB offline regression failed'}
} finally {foreach($key in $previous.Keys){[Environment]::SetEnvironmentVariable($key,$previous[$key],'Process')}}
