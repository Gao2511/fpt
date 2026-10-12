# PREPARATION ONLY until a separately reviewed plan has approved=true, current model/tier/quota and test keys.
param([switch]$ApprovedLivePlan,[string]$PlanPath)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
if($PSVersionTable.PSVersion.Major -lt 7){throw 'PowerShell 7 required'}
if(!$ApprovedLivePlan){throw 'Separate explicit live authorization is required; no API or DB action'}
if(!$PlanPath){$PlanPath=Join-Path $root 'test/rag/fixtures/phase24-live-plan.json'}
if($env:RAG_ENVIRONMENT -cne 'development' -or $env:RAG_ENABLED -cne 'false'){throw 'Require development and RAG_ENABLED=false'}
$classes=Join-Path $root '.build/phase241-test-classes'
if(!(Test-Path -LiteralPath "$classes/rag/live/LiveRagRunner.class")){throw 'Compile the runner offline first'}
$classpath="$classes;$root/.build/web/WEB-INF/classes;$root/lib/runtime/*;$root/lib/compile-only/*"
# One frozen local snapshot is used for both preflight and execution; the original plan is not modified.
$snapshotDir=Join-Path $root '.build/phase241-plan-snapshots'
New-Item -ItemType Directory -Path $snapshotDir -Force | Out-Null
$snapshot=Join-Path $snapshotDir ([guid]::NewGuid().ToString('N')+'.json')
$snapshotHandle=[IO.File]::Open($snapshot,[IO.FileMode]::CreateNew,[IO.FileAccess]::ReadWrite,[IO.FileShare]::Read)
try {
    $planBytes=[IO.File]::ReadAllBytes((Resolve-Path -LiteralPath $PlanPath).Path)
    $snapshotHandle.Write($planBytes);$snapshotHandle.Flush($true)
# Java preflight validates the immutable plan and only the two named test environment keys; no network/DB.
& java '-Dfile.encoding=UTF-8' -cp $classpath rag.live.LiveRagRunner --preflight $snapshot
if($LASTEXITCODE -ne 0){throw 'Preflight rejected live execution; no provider or DB action'}
$plan=[Text.Encoding]::UTF8.GetString($planBytes) | ConvertFrom-Json
$orchestration=Join-Path $root '.build/phase24-orchestration'
New-Item -ItemType Directory -Path $orchestration -Force | Out-Null
$lock=[IO.File]::Open((Join-Path $orchestration ($plan.planId+'.lock')),[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::None)
$database=$null;$old=@{}
try {
    $lock.Write([Text.Encoding]::UTF8.GetBytes('claimed; no resume; no production fallback'));$lock.Flush($true)
    . "$PSScriptRoot/rag-live-database.ps1"
    $database=New-RagLiveTestDatabase -Root $root -PlanId $plan.planId
    $old=Set-RagLiveDatabaseEnvironment -Database $database
    & java '-Dfile.encoding=UTF-8' -cp $classpath rag.live.LiveRagRunner --execute-approved $snapshot "$root/.build/phase24-live"
    if($LASTEXITCODE -ne 0){throw 'Live runner halted; retain locks/evidence, do not repeat this plan'}
} finally {
    foreach($name in $old.Keys){[Environment]::SetEnvironmentVariable($name,$old[$name],'Process')}
    if($database){try{Invoke-RagDocker -DockerArgs @('stop',$database.Name) | Out-Null;Write-Output "Test container preserved (stopped): $($database.Name)"}catch{Write-Warning 'Inspect the new test container manually; not removed'}}
    $lock.Dispose()
}
} finally {$snapshotHandle.Dispose()}
