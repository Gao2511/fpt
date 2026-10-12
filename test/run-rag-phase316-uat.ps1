# Phase 3.16: Full Local/Offline UAT Runner (PostgreSQL 17 + pgvector 0.8.2 + Live Tomcat 9.0.118 HTTP Server)
# Zero real Gemini API calls; zero Supabase production connections.
param([string]$Image='pgvector/pgvector:0.8.2-pg17@sha256:feb68f4f15446397d8cac7f4fe48fe4586de83160d1fc48b46283312d1a33966')
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
. "$PSScriptRoot/rag-docker-command.ps1"

$ant = "D:\Subject FPT\PRJ301\NetBeans-13\netbeans\extide\ant\bin\ant.bat"
$tomcatHome = "D:\Subject FPT\PRJ301\apache-tomcat-9.0.118-windows-x64\apache-tomcat-9.0.118"

& $ant -f "$root/build-portable.xml" war | Out-Host
if ($LASTEXITCODE -ne 0) { throw "Portable WAR build failed" }
$cp = "$root/.build/web/WEB-INF/classes;$root/.build/test-classes;$root/lib/runtime/*;$root/lib/compile-only/*"
New-Item -ItemType Directory -Force -Path "$root/.build/test-classes" | Out-Null
& javac -encoding UTF-8 --release 8 -cp $cp -d "$root/.build/test-classes" "$root/test/rag/RagPhase316UatTest.java" "$root/test/rag/RagPhase315UatTest.java" "$root/test/ai/ConsultationBehaviorTest.java"
if ($LASTEXITCODE -ne 0) { throw "RagPhase316UatTest compilation failed" }

Invoke-RagDocker -DockerArgs @('info','--format','{{.ServerVersion}}') -TimeoutSeconds 10 | Out-Null
$name = 'fpt-rag-uat316-' + [guid]::NewGuid().ToString('N').Substring(0,12)
$dir = Join-Path $root ".local/rag-tests/$name"
New-Item -ItemType Directory -Path $dir -Force | Out-Null

$random = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($random)
$rng.Dispose()
$password = [Convert]::ToBase64String($random)
$envFile = Join-Path $dir 'postgres.env'
[IO.File]::WriteAllText($envFile, "POSTGRES_USER=fpt_rag_user`nPOSTGRES_DB=fpt_rag_test`nPOSTGRES_PASSWORD=$password`n", [Text.UTF8Encoding]::new($false))

$httpPort = 18088
$shutdownPort = 18005
$catalinaBase = Join-Path ([System.IO.Path]::GetTempPath()) ('fpt-tomcat-uat316-' + [guid]::NewGuid().ToString('N').Substring(0,8))
$tomcatProc = $null
$containerCreated = $false

try {
    Invoke-RagDocker -DockerArgs @('run','--detach','--name',$name,'--label','com.fpt.rag.test=true','--env-file',$envFile,'-p','127.0.0.1::5432',$Image) -TimeoutSeconds 60 | Out-Null
    $containerCreated = $true
    $portText = Invoke-RagDocker -DockerArgs @('port',$name,'5432/tcp')
    if ($portText -notmatch '^127\.0\.0\.1:(\d+)$') { throw 'Expected loopback-only database port' }
    $pgPort = $Matches[1]

    $ready = $false
    $readyDeadline = [DateTime]::UtcNow.AddSeconds(30)
    for ($i = 0; $i -lt 40 -and [DateTime]::UtcNow -lt $readyDeadline; $i++) {
        try {
            Invoke-RagDocker -DockerArgs @('exec',$name,'pg_isready','-h','127.0.0.1','-U','fpt_rag_user','-d','fpt_rag_test') -TimeoutSeconds 5 | Out-Null
            $ready = $true
            break
        } catch {}
        Start-Sleep -Milliseconds 500
    }
    if (-not $ready) { throw 'Test database did not become ready' }

    Invoke-RagDocker -DockerArgs @('exec',$name,'psql','-U','fpt_rag_user','-d','fpt_rag_test','--set','ON_ERROR_STOP=1','-c','CREATE EXTENSION vector;') | Out-Null
    foreach ($migration in @('002-rag-mvp.sql','003-rag-source-policy.sql','004-admin-knowledge-base.sql','005-knowledge-section-import.sql','006-rag-gemini-runtime.sql')) {
        Invoke-RagDocker -DockerArgs @('cp',"$root/database/migrations/$migration","${name}:/tmp/$migration") | Out-Null
        Invoke-RagDocker -DockerArgs @('exec',$name,'psql','-U','fpt_rag_user','-d','fpt_rag_test','--set','ON_ERROR_STOP=1','-f',"/tmp/$migration") | Out-Null
    }

    # Configure isolated environment variables
    $env:RAG_ENVIRONMENT = 'development'
    $env:RAG_TEST_DATABASE_APPROVED = 'true'
    $env:RAG_ENABLED = 'false'
    $env:RAG_CALLS_APPROVED = 'false'
    $env:RAG_MODEL_VERIFIED = 'false'
    $env:DB_HOST = '127.0.0.1'
    $env:DB_PORT = $pgPort
    $env:DB_NAME = 'fpt_rag_test'
    $env:DB_USER = 'fpt_rag_user'
    $env:DB_PASSWORD = $password
    $env:DB_SSLMODE = 'disable'

    # Prepare isolated Tomcat 9 CATALINA_BASE and deploy ROOT.war
    foreach ($sub in @('conf', 'logs', 'temp', 'webapps', 'work')) {
        New-Item -ItemType Directory -Force -Path (Join-Path $catalinaBase $sub) | Out-Null
    }
    Get-ChildItem -LiteralPath "$tomcatHome/conf" -File | Copy-Item -Destination (Join-Path $catalinaBase 'conf')
    $serverXmlPath = Join-Path $catalinaBase 'conf/server.xml'
    $serverXml = Get-Content -Raw $serverXmlPath
    $serverXml = $serverXml -replace '<Server port="[0-9]+"', '<Server port="-1"'
    $serverXml = $serverXml -replace 'port="808[0-9]"', "port=`"$httpPort`" address=`"127.0.0.1`""
    Set-Content -Path $serverXmlPath -Value $serverXml -Encoding UTF8

    Copy-Item "$root/.build/dist/fpt-sale-rag.war" (Join-Path $catalinaBase 'webapps/ROOT.war') -Force

    $listener=[Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback,$httpPort)
    try{$listener.Start()}finally{$listener.Stop()}
    $uploadRoot=Join-Path $catalinaBase 'uploads'
    $env:CATALINA_OPTS='-Dfpt.kb.upload.dir="'+$uploadRoot+'"'
    $env:RAG_EMBEDDING_CALLS_APPROVED='false'
    $env:CATALINA_HOME = $tomcatHome
    $env:CATALINA_BASE = $catalinaBase
    $tomcatOut = Join-Path $catalinaBase 'logs/tomcat-stdout.log'
    $tomcatErr = Join-Path $catalinaBase 'logs/tomcat-stderr.log'
    $tomcatProc = Start-Process -FilePath "$tomcatHome/bin/catalina.bat" -ArgumentList 'run' `
        -RedirectStandardOutput $tomcatOut -RedirectStandardError $tomcatErr -PassThru -WindowStyle Hidden

    $httpReady = $false
    for ($i = 0; $i -lt 45; $i++) {
        try {
            $resp = Invoke-WebRequest -Uri "http://127.0.0.1:$httpPort/login" -UseBasicParsing -MaximumRedirection 0 -TimeoutSec 2 -ErrorAction Stop
            if ($resp.StatusCode -eq 200) { $httpReady = $true; break }
        } catch {
            if ($_.Exception.Response -and [int]$_.Exception.Response.StatusCode -eq 200) { $httpReady = $true; break }
        }
        Start-Sleep -Seconds 1
    }
    if (-not $httpReady) {
        if (Test-Path $tomcatErr) { Get-Content $tomcatErr -Tail 40 | Out-Host }
        throw "Isolated Tomcat 9 server did not become ready on http://127.0.0.1:$httpPort"
    }
    Write-Host "Isolated Tomcat 9 ready at http://127.0.0.1:$httpPort (PostgreSQL 17.10 port $pgPort)"

    # Compile & execute RagPhase316UatTest against PostgreSQL + live Tomcat HTTP
    $uatLog=Join-Path $catalinaBase 'logs/phase316-test.log'
    $uatStream=[IO.File]::Open($uatLog,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write)
    $uatWriter=[IO.StreamWriter]::new($uatStream,[Text.UTF8Encoding]::new($false))
    try {
        & java '-Dfile.encoding=UTF-8' -cp $cp rag.RagPhase316UatTest "http://127.0.0.1:$httpPort" $uploadRoot *>&1 | ForEach-Object {$uatWriter.WriteLine($_); Write-Output $_}
    } finally {$uatWriter.Dispose()}
    if ($LASTEXITCODE -ne 0) { throw "RagPhase316UatTest execution failed" }

} finally {
    Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" -ErrorAction SilentlyContinue |
        Where-Object { $_.CommandLine -like "*$catalinaBase*" } |
        ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
    if($tomcatProc -and -not $tomcatProc.HasExited){Stop-Process -Id $tomcatProc.Id -Force -ErrorAction SilentlyContinue}
    if($containerCreated){Invoke-RagDocker -DockerArgs @('stop',$name) -TimeoutSeconds 15 | Out-Null}
    Write-Output "Preserved isolated container: $name; Tomcat evidence: $catalinaBase; private config retained under ignored .local/rag-tests"
}
