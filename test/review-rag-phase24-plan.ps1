# Offline plan/evidence review ONLY. Child runner uses injected fake transport;
# no database, Docker, production settings or live request switches are used.
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
$plan=Get-Content -Raw -LiteralPath "$root/test/rag/fixtures/phase24-live-plan.json" | ConvertFrom-Json
if($plan.approved -or $plan.executionAuthorized -or $plan.productionRagEnabled){throw 'Plan must remain unapproved and production RAG off'}
$budget=$plan.requestBudget
$sum=$budget.documentEmbeddingPosts+$budget.queryEmbeddingPosts+$budget.chatGenerationPosts
if($sum -ne 14 -or $budget.maximumExternalRequests -ne 14 -or $budget.documentEmbeddingPosts -ne 2 -or $budget.queryEmbeddingPosts -ne 6 -or $budget.chatGenerationPosts -ne 6){throw 'Request budget mismatch'}
foreach($name in @('metadataGets','countTokensRequests','retries','redirects')){if($budget.$name -ne 0){throw 'Unexpected extra request allowance'}}
if($plan.documents.Count -ne 2 -or $plan.questions.Count -ne 6 -or @($plan.documents | Where-Object { !$_.approvedForLive -or !$_.approvedForIsolatedTestOnly -or $_.productionPublicationApproved -or $_.expectedChunks -ne 1 }).Count){throw 'Corpus must have two one-chunk documents approved for isolated test only and never for production publication'}
if($plan.embeddingModel -cne 'gemini-embedding-001' -or $plan.dimensions -ne 768 -or $plan.embeddingCompatibility.documentTaskType -cne 'RETRIEVAL_DOCUMENT' -or $plan.embeddingCompatibility.queryTaskType -cne 'RETRIEVAL_QUERY'){throw 'Embedding space/task mismatch'}
if($plan.cost.paidRequestsAllowed -or $plan.cost.maximumApprovedPaidCostUsd -ne 0){throw 'No paid requests are approved'}
if($plan.database.host -cne '127.0.0.1' -or $plan.database.name -cne 'fpt_rag_test' -or !$plan.database.requireNewDedicatedContainer -or $plan.database.productionAllowed){throw 'Database isolation plan mismatch'}

# Snapshot existing evidence BEFORE either rerun, including both old API result/lock pairs.
$before=@{}
foreach($folder in @('.build/rag-embedding-probes','.build/rag-auth-probes')) {
    if(Test-Path -LiteralPath "$root/$folder"){
        foreach($file in Get-ChildItem -LiteralPath "$root/$folder" -File){
            $before[$file.FullName]=@{sha256=(Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash;modifiedUtc=$file.LastWriteTimeUtc.ToString('o')}
        }
    }
}
$runPaths=@()
for($i=0;$i -lt 2;$i++){
    $output=@(& "$PSScriptRoot/rag-single-embedding-tests.ps1" -PlanId 'phase23-corrected-payload-20261011-v2')
    $marker=@($output | Where-Object { $_ -is [string] -and $_.StartsWith('Sanitized mock result: ') })
    if($marker.Count -ne 1){throw 'Mock runner must publish exactly one unique result path'}
    $path=$marker[0].Substring('Sanitized mock result: '.Length)
    $directory=[IO.Path]::GetFullPath("$root/.build/rag-embedding-probes")
    if([IO.Path]::GetDirectoryName([IO.Path]::GetFullPath($path)) -cne $directory -or [IO.Path]::GetFileName($path) -notmatch '^phase23-corrected-payload-20261011-v2\.mocks\.[a-f0-9]{32}\.json$' -or $before.ContainsKey($path) -or $runPaths -contains $path){throw 'Mock output path reused previous evidence'}
    $result=Get-Content -Raw -LiteralPath $path | ConvertFrom-Json
    if(!$result.mockOnly -or $result.realApiRequests -ne 0 -or $result.passed -ne 29){throw 'Unexpected mock test result'}
    $runPaths+=$path
    # Protect the first rerun as well as original artifacts on the next rerun.
    $file=Get-Item -LiteralPath $path
    $before[$path]=@{sha256=(Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash;modifiedUtc=$file.LastWriteTimeUtc.ToString('o')}
}
$preserved=@()
foreach($path in $before.Keys){
    if(!(Test-Path -LiteralPath $path)){throw 'Existing evidence was removed'}
    $file=Get-Item -LiteralPath $path
    $ok=(Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash -ceq $before[$path].sha256 -and $file.LastWriteTimeUtc.ToString('o') -ceq $before[$path].modifiedUtc
    if(!$ok){throw 'Existing artifact content or modification time changed'}
    $preserved+=[pscustomobject]@{path=$path;sha256=$before[$path].sha256;modifiedUtc=$before[$path].modifiedUtc;unchanged=$true}
}
$phase24Lock=Join-Path $root ('.build/phase24-live/'+$plan.planId+'.request.lock')
$report=[pscustomobject]@{checkedAtUtc=[DateTime]::UtcNow.ToString('o');planId=$plan.planId;approved=$false;executionAuthorized=$false;liveRunnerImplemented=$plan.runnerImplemented;realApiRequests=0;databaseAccesses=0;requestBudgetTotal=$sum;mockReruns=2;mockTestsPerRerun=29;newMockReports=$runPaths;preservedArtifacts=$preserved;phase24ExecutionLockExists=(Test-Path -LiteralPath $phase24Lock);evidenceScope='Current pre-review snapshot; does not restore or validate original bytes of the previously overwritten legacy v2 mock report'}
$resultPath=Join-Path $root ('.build/phase24-plan-review-'+[guid]::NewGuid().ToString('N')+'.json')
$stream=[IO.File]::Open($resultPath,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::None)
try{$bytes=[Text.UTF8Encoding]::new($false).GetBytes(($report | ConvertTo-Json -Depth 8));$stream.Write($bytes,0,$bytes.Length)}finally{$stream.Dispose()}
Write-Output 'PASS budget 2+6+6=14, no extra requests, model/task/dimensions and isolation plan'
Write-Output ('PASS mock runner rerun twice: 29/29 each, distinct new reports, '+$preserved.Count+' artifacts unchanged')
Write-Output ('Phase 2.4 live runner implemented: '+$plan.runnerImplemented+'; approved=false; live API requests=0')
Write-Output "Sanitized review result: $resultPath"
