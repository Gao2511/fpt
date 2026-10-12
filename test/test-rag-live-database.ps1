# Offline provider validation. New isolated PostgreSQL only, no real Gemini transport reachable from this test entry point.
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
. "$PSScriptRoot/rag-live-database.ps1"
$database=$null;$old=@{}
try {
    $database=New-RagLiveTestDatabase -Root $root -PlanId ('offline-phase241-'+[guid]::NewGuid().ToString('N'))
    $old=Set-RagLiveDatabaseEnvironment -Database $database
    foreach($item in @{RAG_ENABLED='false';RAG_ENVIRONMENT='development'}.GetEnumerator()){$old[$item.Key]=[Environment]::GetEnvironmentVariable($item.Key,'Process');[Environment]::SetEnvironmentVariable($item.Key,$item.Value,'Process')}
    $cp="$root/.build/phase241-test-classes;$root/.build/web/WEB-INF/classes;$root/lib/runtime/*;$root/lib/compile-only/*"
    & java '-Dfile.encoding=UTF-8' -cp $cp rag.live.LiveRunnerDatabaseTest $root
    if($LASTEXITCODE -ne 0){throw 'Offline database runner test failed; all resources retained'}
} finally {
    foreach($name in $old.Keys){[Environment]::SetEnvironmentVariable($name,$old[$name],'Process')}
    if($database){try{Invoke-RagDocker -DockerArgs @('stop',$database.Name) | Out-Null;Write-Output "Offline test container preserved (stopped): $($database.Name)"}catch{Write-Warning 'Inspect new offline test container state manually'}}
}
