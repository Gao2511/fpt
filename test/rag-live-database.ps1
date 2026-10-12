# Only creates a NEW pinned local container. Never reads production database settings.
. "$PSScriptRoot/rag-docker-command.ps1"
function New-RagLiveTestDatabase {
    param([string]$Root,[string]$PlanId)
    $image='pgvector/pgvector:0.8.2-pg17@sha256:feb68f4f15446397d8cac7f4fe48fe4586de83160d1fc48b46283312d1a33966'
    Invoke-RagDocker -DockerArgs @('info','--format','{{.ServerVersion}}') -TimeoutSeconds 10 | Out-Null
    Invoke-RagDocker -DockerArgs @('image','inspect','--format','{{.Id}}',$image) -TimeoutSeconds 10 | Out-Null
    $name='fpt-rag-live-test-'+[guid]::NewGuid().ToString('N').Substring(0,12)
    $nonce=[guid]::NewGuid().ToString('N')
    $dir=Join-Path $Root ".local/rag-tests/$name"
    New-Item -ItemType Directory -Path $dir | Out-Null
    $password=[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
    $envFile=Join-Path $dir 'postgres.env'
    $file=[IO.File]::Open($envFile,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::None)
    try{$bytes=[Text.Encoding]::UTF8.GetBytes("POSTGRES_USER=fpt_rag_user`nPOSTGRES_DB=fpt_rag_test`nPOSTGRES_PASSWORD=$password`n");$file.Write($bytes);$file.Flush($true)}finally{$file.Dispose()}
    $created=$false
    try {
        $id=Invoke-RagDocker -DockerArgs @('run','--detach','--name',$name,'--label','com.fpt.rag.test=true','--label',"com.fpt.rag.plan=$PlanId",'--label',"com.fpt.rag.nonce=$nonce",'--env-file',$envFile,'-p','127.0.0.1::5432',$image) -TimeoutSeconds 60
        $created=$true
        # Do not inspect/print Config.Env, which contains the test database password.
        $identity=Invoke-RagDocker -DockerArgs @('inspect','--format','{{.Id}}|{{index .Config.Labels "com.fpt.rag.test"}}|{{index .Config.Labels "com.fpt.rag.plan"}}|{{index .Config.Labels "com.fpt.rag.nonce"}}|{{.State.Running}}',$name)
        if($identity -cne "$id|true|$PlanId|$nonce|true"){throw 'New test container identity not verified'}
        $portText=Invoke-RagDocker -DockerArgs @('port',$name,'5432/tcp')
        if($portText -notmatch '^127\.0\.0\.1:(\d+)$'){throw 'Database must be loopback only'}
        $port=$Matches[1]
        $ready=$false
        for($i=0;$i -lt 40;$i++){
            try{Invoke-RagDocker -DockerArgs @('exec',$name,'pg_isready','-h','127.0.0.1','-U','fpt_rag_user','-d','fpt_rag_test') -TimeoutSeconds 3 | Out-Null;$ready=$true;break}catch{}
            Start-Sleep -Milliseconds 500
        }
        if(!$ready){throw 'New isolated test DB not ready'}
        Invoke-RagDocker -DockerArgs @('exec',$name,'psql','-U','fpt_rag_user','-d','fpt_rag_test','--set','ON_ERROR_STOP=1','-c','CREATE EXTENSION vector;') | Out-Null
        foreach($migration in @('002-rag-mvp.sql','003-rag-source-policy.sql')){
            Invoke-RagDocker -DockerArgs @('cp',"$Root/database/migrations/$migration","${name}:/tmp/$migration") | Out-Null
            Invoke-RagDocker -DockerArgs @('exec',$name,'psql','-U','fpt_rag_user','-d','fpt_rag_test','--set','ON_ERROR_STOP=1','-f',"/tmp/$migration") | Out-Null
        }
        Invoke-RagDocker -DockerArgs @('exec',$name,'psql','-U','fpt_rag_user','-d','fpt_rag_test','--set','ON_ERROR_STOP=1','-c',"CREATE TABLE phase241_test_identity(nonce text NOT NULL); INSERT INTO phase241_test_identity VALUES ('$nonce');") | Out-Null
        # Returned only to the caller's variable, never serialized or printed.
        return @{Name=$name;Port=$port;Password=$password;Nonce=$nonce}
    } catch {
        if($created){try{Invoke-RagDocker -DockerArgs @('stop',$name) | Out-Null}catch{}}
        throw 'Isolated database preparation failed; no Gemini calls. Container and private config retained.'
    }
}
function Set-RagLiveDatabaseEnvironment {
    param([hashtable]$Database)
    $values=@{RAG_TEST_DB_HOST='127.0.0.1';RAG_TEST_DB_NAME='fpt_rag_test';RAG_TEST_DB_PORT=$Database.Port;RAG_TEST_DB_PASSWORD=$Database.Password;RAG_TEST_DB_NONCE=$Database.Nonce;RAG_TEST_CONTAINER_VERIFIED='true'}
    $old=@{}
    foreach($name in $values.Keys){$old[$name]=[Environment]::GetEnvironmentVariable($name,'Process');[Environment]::SetEnvironmentVariable($name,$values[$name],'Process')}
    return $old
}
