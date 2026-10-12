# Creates ONLY a new disposable local test container; never reads production/local website credentials.
# Requires Docker Desktop already running. Container is stopped in finally, preserved for inspection.
param([string]$Image='pgvector/pgvector:0.8.2-pg17@sha256:feb68f4f15446397d8cac7f4fe48fe4586de83160d1fc48b46283312d1a33966', [switch]$Phase24, [switch]$Phase3, [switch]$Phase4, [switch]$Phase57)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
. "$PSScriptRoot/rag-docker-command.ps1"
# Fail before creating test credentials/container if Docker is unavailable.
Invoke-RagDocker -DockerArgs @('info','--format','{{.ServerVersion}}') -TimeoutSeconds 10 | Out-Null
try{Invoke-RagDocker -DockerArgs @('image','inspect','--format','{{.Id}}',$Image) -TimeoutSeconds 10 | Out-Null}
catch{throw 'Approved pgvector image unavailable or Docker image API unresponsive; recover Docker/pull the pinned image before testing'}
$name='fpt-rag-test-'+[guid]::NewGuid().ToString('N').Substring(0,12)
$dir=Join-Path $root ".local/rag-tests/$name"
New-Item -ItemType Directory -Path $dir -Force | Out-Null
$random=New-Object byte[] 32
$rng=[System.Security.Cryptography.RandomNumberGenerator]::Create();$rng.GetBytes($random);$rng.Dispose()
$password=[Convert]::ToBase64String($random)
$envFile=Join-Path $dir 'postgres.env'
[IO.File]::WriteAllText($envFile,"POSTGRES_USER=fpt_rag_user`nPOSTGRES_DB=fpt_rag_test`nPOSTGRES_PASSWORD=$password`n",[Text.UTF8Encoding]::new($false))
$containerCreated=$false
$old=@{}
try {
    $container=Invoke-RagDocker -DockerArgs @('run','--detach','--name',$name,'--label','com.fpt.rag.test=true','--env-file',$envFile,'-p','127.0.0.1::5432',$Image) -TimeoutSeconds 60
    $containerCreated=$true
    $portText=Invoke-RagDocker -DockerArgs @('port',$name,'5432/tcp')
    if($portText -notmatch '^127\.0\.0\.1:(\d+)$'){throw 'Expected loopback-only database port'}
    $port=$Matches[1]
    $ready=$false
    $readyDeadline=[DateTime]::UtcNow.AddSeconds(30)
    for($i=0;$i -lt 40 -and [DateTime]::UtcNow -lt $readyDeadline;$i++) {
        # TCP becomes ready only after initdb's temporary Unix-socket server exits.
        try {Invoke-RagDocker -DockerArgs @('exec',$name,'pg_isready','-h','127.0.0.1','-U','fpt_rag_user','-d','fpt_rag_test') -TimeoutSeconds 5 | Out-Null;$ready=$true;break}catch{}
        Start-Sleep -Milliseconds 500
    }
    if(!$ready){throw 'Test database did not become ready'}
    # Extension approval applies ONLY to this newly created isolated local container.
    Invoke-RagDocker -DockerArgs @('exec',$name,'psql','-U','fpt_rag_user','-d','fpt_rag_test','--set','ON_ERROR_STOP=1','-c','CREATE EXTENSION vector;')
    foreach($migration in @('002-rag-mvp.sql','003-rag-source-policy.sql','004-admin-knowledge-base.sql','005-knowledge-section-import.sql','006-rag-gemini-runtime.sql')) {
        Invoke-RagDocker -DockerArgs @('cp',"$root/database/migrations/$migration","${name}:/tmp/$migration") | Out-Null
        Invoke-RagDocker -DockerArgs @('exec',$name,'psql','-U','fpt_rag_user','-d','fpt_rag_test','--set','ON_ERROR_STOP=1','-f',"/tmp/$migration")
    }
    if($Phase57){foreach($migration in @('007-online-production-readiness.sql','008-knowledge-publish-lifecycle.sql')){
        Invoke-RagDocker -DockerArgs @('cp',"$root/database/migrations/$migration","${name}:/tmp/$migration") | Out-Null
        Invoke-RagDocker -DockerArgs @('exec',$name,'psql','-U','fpt_rag_user','-d','fpt_rag_test','--set','ON_ERROR_STOP=1','-f',"/tmp/$migration") | Out-Null
    }}
    Invoke-RagDocker -DockerArgs @('exec',$name,'psql','-U','fpt_rag_user','-d','fpt_rag_test','-t','-c',"SELECT version(), extversion FROM pg_extension WHERE extname='vector';")
    $vars=@{DB_HOST='127.0.0.1';DB_PORT=$port;DB_NAME='fpt_rag_test';DB_USER='fpt_rag_user';DB_PASSWORD=$password;DB_SSLMODE='disable';RAG_ENVIRONMENT='development';RAG_TEST_DATABASE_APPROVED='true';RAG_ENABLED='false';RAG_EMBEDDING_CALLS_APPROVED='false'}
    foreach($key in $vars.Keys){$old[$key]=[Environment]::GetEnvironmentVariable($key,'Process');[Environment]::SetEnvironmentVariable($key,$vars[$key],'Process')}
    if($Phase57){ & "$PSScriptRoot/run-knowledge-lifecycle.ps1" -Postgres }
    elseif($Phase4){ & "$PSScriptRoot/run-rag-phase4-postgres.ps1" }
    elseif($Phase3){ & "$PSScriptRoot/run-rag-phase3-postgres.ps1" }
    elseif($Phase24){ & "$PSScriptRoot/run-rag-phase24-e2e.ps1" }
    else { & "$PSScriptRoot/run-rag-postgres-tests.ps1" }
    if($LASTEXITCODE -ne 0){throw 'Integration tests failed'}
} finally {
    foreach($key in $old.Keys){[Environment]::SetEnvironmentVariable($key,$old[$key],'Process')}
    if($containerCreated){try{Invoke-RagDocker -DockerArgs @('stop',$name) | Out-Null;Write-Output "Isolated test container (stopped): $name"}catch{Write-Warning "Could not confirm stopped state; inspect test container $name in Docker Desktop"}}
    else{Write-Output "Test container creation not confirmed; inspect name $name if Docker timed out"}
    Write-Output 'Private test configuration remains in ignored .local/rag-tests; no production database was used.'
}
