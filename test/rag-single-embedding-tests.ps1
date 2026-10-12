# Deterministic mocks and local Java validation ONLY. No provider/DB calls or real request lock.
param([ValidateSet('phase23-single-embedding-20261011-v1','phase23-corrected-payload-20261011-v2')]
    [string]$PlanId='phase23-single-embedding-20261011-v1')
$ErrorActionPreference='Stop'
. "$PSScriptRoot/rag-single-embedding-helpers.ps1"
$root=Split-Path -Parent $PSScriptRoot
$java=(Get-Command java -ErrorAction SilentlyContinue).Source
if (!$java) { $java='C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot\bin\java.exe' }
$payload=Get-RagSingleEmbeddingPayload -Root $root -PlanId $PlanId
$script:passed=0;$script:fakeKey='FAKE_OFFLINE_SECRET_SENTINEL';$script:results=@()
function Assert-Check([bool]$ok) { if (!$ok) { throw 'Single embedding mock assertion failed' } }
function Test-Case([string]$name,[scriptblock]$body) { & $body; $script:passed++;Write-Output "PASS $name" }
function Probe([int]$status,[string]$body) {
    $counter=[pscustomobject]@{calls=0}
    $r=Invoke-RagSingleEmbeddingProbe -TestApiKey $script:fakeKey -Payload $payload -PlanId $PlanId -Transport {
        param($request)
        $counter.calls++
        Assert-Check ($request.Method.Method -ceq 'POST')
        Assert-Check ($request.RequestUri.AbsoluteUri -ceq 'https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent')
        Assert-Check (@($request.Headers.GetValues('x-goog-api-key'))[0] -ceq $script:fakeKey)
        $sent=$request.Content.ReadAsStringAsync().GetAwaiter().GetResult()|ConvertFrom-Json
        Assert-Check ($sent.content.parts[0].text -ceq 'Nhà nhiều tầng WiFi yếu thì sao?' -and $sent.outputDimensionality -eq 768)
        Assert-Check (($sent.outputDimensionality -is [int] -or $sent.outputDimensionality -is [long]) -and $sent.taskType -ceq 'RETRIEVAL_QUERY' -and @($sent.PSObject.Properties).Count -eq 4)
        Assert-Check ((@($sent.PSObject.Properties.Name) -ccontains 'outputDimensionality') -and (@($sent.PSObject.Properties.Name) -ccontains 'taskType'))
        Assert-Check (!(@($sent.PSObject.Properties.Name) -ccontains 'embedContentConfig') -and !(@($sent.PSObject.Properties.Name) -ccontains 'autoTruncate'))
        [pscustomobject]@{status=$status;body=$body}
    } -VectorValidator {param($values); Invoke-RagVectorValidator -Root $root -Java $java -Values $values}
    Assert-Check ($counter.calls -eq 1 -and $r.embeddingPostRequests -eq 1 -and $r.metadataGetRequests -eq 0 -and $r.retryRequests -eq 0 -and $r.databaseWrites -eq 0 -and $r.chatGenerationRequests -eq 0)
    Assert-Check ($r.planId -ceq $PlanId)
    Assert-Check (!(($r|ConvertTo-Json -Depth 8).Contains($script:fakeKey)))
    $script:results+=$r
    return $r
}
$valid=[double[]]::new(768);$valid[0]=3;$valid[1]=4
$withUsage=@{embedding=@{values=$valid};usageMetadata=@{promptTokenCount=18}}|ConvertTo-Json -Compress -Depth 8
Test-Case 'explicit approval required before live wrapper initializes' {
    $blocked=$false
    try { & "$PSScriptRoot/run-rag-single-embedding.ps1" -PlanId $PlanId } catch { $blocked=$_.Exception.Message -ceq 'Explicit single embedding POST approval is required' }
    Assert-Check $blocked
}
Test-Case 'prepared REST payload contains numeric top-level 768 dimensions' { Assert-Check ($payload.outputDimensionality -eq 768 -and ($payload.outputDimensionality -is [int] -or $payload.outputDimensionality -is [long]) -and $payload.taskType -ceq 'RETRIEVAL_QUERY') }
Test-Case 'valid vector uses existing Java normalizer and usage metadata' {
    $r=Probe 200 $withUsage
    Assert-Check ($r.httpStatus -eq 200 -and $r.vectorValid -and $r.returnedDimensions -eq 768 -and [math]::Abs($r.normBefore-5) -lt 0.00001 -and [math]::Abs($r.normAfter-1) -lt 0.00001)
    Assert-Check ($r.tokenUsageAvailable -and $r.inputTokens -eq 18 -and $r.estimatedCostUsd -eq 0)
}
Test-Case 'missing token usage remains unavailable' { $r=Probe 200 (@{embedding=@{values=$valid}}|ConvertTo-Json -Compress -Depth 8);Assert-Check ($r.vectorValid -and !$r.tokenUsageAvailable -and $null -eq $r.inputTokens) }
Test-Case 'wrong vector dimension rejected without padding' { $r=Probe 200 (@{embedding=@{values=@(1,0)}}|ConvertTo-Json -Compress -Depth 8);Assert-Check (!$r.vectorValid -and $r.returnedDimensions -eq 2 -and $r.httpStatus -eq 200) }
Test-Case 'HTTP 200 with default 3072 dimensions is rejected without truncation' {
    $wrong=[double[]]::new(3072);$wrong[0]=1
    $r=Probe 200 (@{embedding=@{values=$wrong}}|ConvertTo-Json -Compress -Depth 8)
    Assert-Check ($r.httpStatus -eq 200 -and !$r.vectorValid -and $r.returnedDimensions -eq 3072 -and $r.errorKind -ceq 'INVALID_EMBEDDING_RESPONSE')
}
Test-Case 'token usage survives an invalid-dimension response' {
    $wrong=[double[]]::new(3072);$wrong[0]=1
    $r=Probe 200 (@{embedding=@{values=$wrong};usageMetadata=@{promptTokenCount=18}}|ConvertTo-Json -Compress -Depth 8)
    Assert-Check (!$r.vectorValid -and $r.returnedDimensions -eq 3072 -and $r.tokenUsageAvailable -and $r.inputTokens -eq 18)
}
Test-Case 'outgoing serialized REST field determines mock response dimensions' {
    $counter=[pscustomobject]@{calls=0}
    $r=Invoke-RagSingleEmbeddingProbe -TestApiKey $script:fakeKey -Payload $payload -PlanId $PlanId -Transport {
        param($request);$counter.calls++
        $sent=$request.Content.ReadAsStringAsync().GetAwaiter().GetResult()|ConvertFrom-Json
        $dimensions=3072
        if ($sent.outputDimensionality -eq 768 -and ($sent.outputDimensionality -is [int] -or $sent.outputDimensionality -is [long])) {$dimensions=768}
        $values=[double[]]::new($dimensions);$values[0]=1
        [pscustomobject]@{status=200;body=(@{embedding=@{values=$values}}|ConvertTo-Json -Compress -Depth 8)}
    } -VectorValidator {param($values);Invoke-RagVectorValidator -Root $root -Java $java -Values $values}
    Assert-Check ($counter.calls -eq 1 -and $r.vectorValid -and $r.returnedDimensions -eq 768)
}
function Reject-Preview($body) {
    $fixtureRoot=Join-Path $root ('.build/mock-payload-'+[guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path "$fixtureRoot/test/rag/fixtures","$fixtureRoot/.build" -Force|Out-Null
    $sourcePaths=Get-RagEmbeddingPlanPaths -Root $root -PlanId $PlanId
    $fixturePaths=Get-RagEmbeddingPlanPaths -Root $fixtureRoot -PlanId $PlanId
    Copy-Item -LiteralPath $sourcePaths.plan -Destination $fixturePaths.plan
    @{planId=$PlanId;requestBody=$body}|ConvertTo-Json -Depth 8|Set-Content -LiteralPath $fixturePaths.preview -Encoding utf8
    $blocked=$false
    try {$null=Get-RagSingleEmbeddingPayload -Root $fixtureRoot -PlanId $PlanId} catch {$blocked=$_.Exception.Message -ceq 'Prepared payload differs from the approved input/config; no request sent'}
    Assert-Check $blocked
}
Test-Case 'stale nested preview fails before transport' {
    Reject-Preview @{model='models/gemini-embedding-001';content=$payload.content;embedContentConfig=@{taskType='RETRIEVAL_QUERY';outputDimensionality=768;autoTruncate=$false}}
}
Test-Case 'string dimensions rejected before transport' {
    Reject-Preview @{model=$payload.model;content=$payload.content;taskType=$payload.taskType;outputDimensionality='768'}
}
Test-Case 'wrong configured dimensions rejected before transport' {
    Reject-Preview @{model=$payload.model;content=$payload.content;taskType=$payload.taskType;outputDimensionality=3072}
}
Test-Case 'incorrect dimension field casing rejected before transport' {
    Reject-Preview @{model=$payload.model;content=$payload.content;taskType=$payload.taskType;outputdimensionality=768}
}
Test-Case 'zero vector rejected' { $r=Probe 200 (@{embedding=@{values=[double[]]::new(768)}}|ConvertTo-Json -Compress -Depth 8);Assert-Check (!$r.vectorValid) }
Test-Case 'numeric strings and secret in values rejected without leaking' {
    $values=[object[]]::new(768);for($i=0;$i -lt 768;$i++){$values[$i]=0};$values[0]=$script:fakeKey
    $r=Probe 200 (@{embedding=@{values=$values}}|ConvertTo-Json -Compress -Depth 8);Assert-Check (!$r.vectorValid)
}
Test-Case 'nonfinite float overflow rejected' {
    $bad='{"embedding":{"values":[1e100,'+((@('0')*767)-join ',')+']}}'
    $r=Probe 200 $bad;Assert-Check (!$r.vectorValid)
}
Test-Case 'empty success body rejected' { $r=Probe 200 '';Assert-Check (!$r.vectorValid -and $r.errorKind -ceq 'INVALID_EMBEDDING_RESPONSE') }
Test-Case 'malformed success body rejected' { $r=Probe 200 'broken';Assert-Check (!$r.vectorValid) }
Test-Case 'missing vector rejected' { $r=Probe 200 '{}';Assert-Check (!$r.vectorValid) }
Test-Case '401 retains only sanitized error codes' {
    $errorJson=@{error=@{code=401;status='UNAUTHENTICATED';message=$script:fakeKey;details=@(@{'@type'='type.googleapis.com/google.rpc.ErrorInfo';reason='API_KEY_INVALID';metadata=@{key=$script:fakeKey}})}}|ConvertTo-Json -Depth 8
    $r=Probe 401 $errorJson;Assert-Check ($r.httpStatus -eq 401 -and $r.providerReasons[0] -ceq 'API_KEY_INVALID' -and !$r.vectorValid)
}
Test-Case 'quota failure makes one attempt' { $r=Probe 429 '';Assert-Check ($r.httpStatus -eq 429 -and $r.errorKind -ceq 'RATE_LIMIT') }
Test-Case 'outage makes one attempt' { $r=Probe 503 '';Assert-Check ($r.httpStatus -eq 503) }
Test-Case 'redirect makes one attempt' { $r=Probe 302 '';Assert-Check ($r.httpStatus -eq 302) }
Test-Case 'transport exception never exposes private text or retries' {
    $counter=[pscustomobject]@{calls=0}
    $r=Invoke-RagSingleEmbeddingProbe -TestApiKey $script:fakeKey -Payload $payload -PlanId $PlanId -Transport {param($request);$counter.calls++;throw $script:fakeKey} -VectorValidator {throw 'Must not run'}
    Assert-Check ($counter.calls -eq 1 -and $r.errorKind -ceq 'TRANSPORT_OR_TIMEOUT' -and $null -eq $r.httpStatus)
    Assert-Check (!(($r|ConvertTo-Json -Depth 8).Contains($script:fakeKey)))
}
Test-Case 'atomic duplicate request guard refuses a second claim' {
    $fixtureRoot=Join-Path $root ('.build/mock-claim-'+[guid]::NewGuid().ToString('N'))
    $paths=Claim-RagEmbeddingExecution -Root $fixtureRoot -PlanId $PlanId
    $before=Get-FileHash -LiteralPath $paths.lock -Algorithm SHA256
    $blocked=$false
    try {$null=Claim-RagEmbeddingExecution -Root $fixtureRoot -PlanId $PlanId}catch{$blocked=$_.Exception.Message -ceq 'This single-POST plan is already claimed; no request sent. Do not remove the lock or rerun.'}
    Assert-Check $blocked
    Assert-Check ((Get-FileHash -LiteralPath $paths.lock -Algorithm SHA256).Hash -ceq $before.Hash)
}
Test-Case 'old and new plans use separate evidence and preview paths' {
    $old=Get-RagEmbeddingPlanPaths -Root $root -PlanId 'phase23-single-embedding-20261011-v1'
    $new=Get-RagEmbeddingPlanPaths -Root $root -PlanId 'phase23-corrected-payload-20261011-v2'
    foreach($field in @('plan','preview','lock','result')) {Assert-Check ($old.$field -cne $new.$field)}
    Assert-Check ($new.lock.EndsWith('phase23-corrected-payload-20261011-v2.request.lock') -and $new.result.EndsWith('phase23-corrected-payload-20261011-v2.result.json'))
}
Test-Case 'unrecognized plan ID cannot select arbitrary evidence paths' {
    $blocked=$false;try {$null=Get-RagEmbeddingPlanPaths -Root $root -PlanId '../other'}catch{$blocked=$true};Assert-Check $blocked
}
Test-Case 'orphan result prevents overwriting evidence even without a lock' {
    $fixtureRoot=Join-Path $root ('.build/mock-result-'+[guid]::NewGuid().ToString('N'))
    $paths=Get-RagEmbeddingPlanPaths -Root $fixtureRoot -PlanId $PlanId
    New-Item -ItemType Directory -Force -Path $paths.directory|Out-Null
    'SAFE_MOCK_EVIDENCE'|Set-Content -LiteralPath $paths.result -Encoding utf8
    $before=Get-FileHash -LiteralPath $paths.result -Algorithm SHA256
    $blocked=$false;try {$null=Claim-RagEmbeddingExecution -Root $fixtureRoot -PlanId $PlanId}catch{$blocked=$true}
    Assert-Check ($blocked -and !(Test-Path -LiteralPath $paths.lock) -and (Get-FileHash -LiteralPath $paths.result -Algorithm SHA256).Hash -ceq $before.Hash)
}
Test-Case 'pending corrected plan can be prepared without granting live approval' {
    $paths=Get-RagEmbeddingPlanPaths -Root $root -PlanId 'phase23-corrected-payload-20261011-v2'
    $plan=Get-Content -LiteralPath $paths.plan -Raw|ConvertFrom-Json
    Assert-Check (!$plan.approved -and $plan.pendingStatus -ceq 'prepared_waiting_explicit_live_approval')
    $body=Get-RagSingleEmbeddingPayload -Root $root -PlanId $paths.planId
    Assert-Check ($body.outputDimensionality -eq 768)
}
Test-Case 'live wrapper has one SendAsync, no redirect and persistent request guard' {
    $source=Get-Content -LiteralPath "$PSScriptRoot/run-rag-single-embedding.ps1" -Raw
    $helper=Get-Content -LiteralPath "$PSScriptRoot/rag-single-embedding-helpers.ps1" -Raw
    Assert-Check (([regex]::Matches($source,'\$client\.SendAsync\(')).Count -eq 1 -and $source.Contains('$handler.AllowAutoRedirect=$false') -and $source.Contains('Claim-RagEmbeddingExecution -Root $root -PlanId $PlanId') -and $helper.Contains('[IO.FileMode]::CreateNew'))
    Assert-Check (!$source.Contains('GEMINI_API_KEY') -and !$source.Contains('GOOGLE_API_KEY'))
}
$report=[pscustomobject]@{planId=$PlanId;mockOnly=$true;realApiRequests=0;passed=$script:passed;results=$script:results}
$directory=Join-Path $root '.build/rag-embedding-probes';New-Item -ItemType Directory -Force -Path $directory|Out-Null
# Re-running mock tests must never overwrite evidence belonging to an earlier live plan.
$reportPath=Join-Path $directory ("$PlanId.mocks."+[guid]::NewGuid().ToString('N')+'.json')
$stream=[IO.File]::Open($reportPath,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::None)
try{$bytes=[Text.UTF8Encoding]::new($false).GetBytes(($report|ConvertTo-Json -Depth 8));$stream.Write($bytes,0,$bytes.Length)}finally{$stream.Dispose()}
Write-Output "Sanitized mock result: $reportPath"
Write-Output "SingleEmbeddingTests: $script:passed PASS / 0 FAIL; real API requests=0"
