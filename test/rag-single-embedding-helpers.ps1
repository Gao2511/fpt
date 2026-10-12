# Test-only. Dot-sourcing this file never sends HTTP or reads a key.
. "$PSScriptRoot/rag-auth-diagnostics.ps1"

function Get-RagEmbeddingPlanPaths {
    param([Parameter(Mandatory)][string]$Root,
        [ValidateSet('phase23-single-embedding-20261011-v1','phase23-corrected-payload-20261011-v2')]
        [string]$PlanId='phase23-single-embedding-20261011-v1')
    $fixture='phase23-single-embedding-plan.json';$preview='phase23-single-embedding-preview.json'
    if ($PlanId -ceq 'phase23-corrected-payload-20261011-v2') {
        $fixture='phase23-corrected-embedding-plan.json';$preview='phase23-corrected-embedding-preview.json'
    }
    $directory=Join-Path $Root '.build/rag-embedding-probes'
    [pscustomobject]@{planId=$PlanId;plan=Join-Path $Root "test/rag/fixtures/$fixture"
        preview=Join-Path $Root ".build/$preview";directory=$directory
        lock=Join-Path $directory "$PlanId.request.lock";result=Join-Path $directory "$PlanId.result.json"}
}

function Claim-RagEmbeddingExecution {
    param([Parameter(Mandatory)][string]$Root,[Parameter(Mandatory)][string]$PlanId)
    $paths=Get-RagEmbeddingPlanPaths -Root $Root -PlanId $PlanId
    if (Test-Path -LiteralPath $paths.result) { throw 'This single-POST plan already has evidence; no request sent. Do not rerun.' }
    New-Item -ItemType Directory -Force -Path $paths.directory|Out-Null
    try { $lock=[IO.File]::Open($paths.lock,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::None) }
    catch { throw 'This single-POST plan is already claimed; no request sent. Do not remove the lock or rerun.' }
    $lock.Dispose()
    return $paths
}

function Get-RagSingleEmbeddingPayload {
    param([Parameter(Mandatory)][string]$Root,[string]$PlanId='phase23-single-embedding-20261011-v1')
    $paths=Get-RagEmbeddingPlanPaths -Root $Root -PlanId $PlanId
    $plan=Get-Content -LiteralPath $paths.plan -Raw|ConvertFrom-Json
    # Payload preparation works before approval; live execution requires its explicit CLI approval switch.
    if (!$plan.quotaVerified -or !$plan.projectTierVerified -or $plan.confirmedTier -cne 'Free Tier' -or
        $plan.planId -cne $PlanId -or $plan.ragEnabled -or
        $plan.method -cne 'POST' -or $plan.endpoint -cne 'https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent' -or
        $plan.taskType -cne 'RETRIEVAL_QUERY' -or
        $plan.maximumHttpRequests -ne 1 -or $plan.plannedHttpRequests -ne 1 -or $plan.embeddingPostRequests -ne 1 -or
        $plan.metadataGetRequests -ne 0 -or $plan.retryRequests -ne 0 -or $plan.databaseWrites -ne 0 -or
        $plan.chatGenerationRequests -ne 0 -or $plan.countTokensRequests -ne 0 -or
        $plan.input -cne 'Nhà nhiều tầng WiFi yếu thì sao?' -or $plan.model -cne 'gemini-embedding-001' -or
        $plan.dimensions -ne 768 -or $plan.keyEnvironmentVariable -cne 'RAG_EMBEDDING_API_KEY') {
        throw 'Approved single-POST plan is invalid; no request sent'
    }
    $preview=Get-Content -LiteralPath $paths.preview -Raw|ConvertFrom-Json
    $body=$preview.requestBody
    $rootFields=@($body.PSObject.Properties.Name)
    if ($preview.planId -cne $PlanId -or $body.model -cne 'models/gemini-embedding-001' -or @($body.content.parts).Count -ne 1 -or
        $body.content.parts[0].text -cne $plan.input -or $body.outputDimensionality -ne 768 -or
        !($body.outputDimensionality -is [int] -or $body.outputDimensionality -is [long]) -or
        $body.taskType -cne 'RETRIEVAL_QUERY' -or
        !($rootFields -ccontains 'model') -or !($rootFields -ccontains 'content') -or
        !($rootFields -ccontains 'taskType') -or !($rootFields -ccontains 'outputDimensionality') -or
        @($body.PSObject.Properties).Count -ne 4 -or @($body.content.PSObject.Properties).Count -ne 1 -or
        @($body.content.parts[0].PSObject.Properties).Count -ne 1) {
        throw 'Prepared payload differs from the approved input/config; no request sent'
    }
    return $body
}

function Invoke-RagVectorValidator {
    param([string]$Root,[string]$Java,[AllowNull()]$Values)
    $info=[Diagnostics.ProcessStartInfo]::new()
    $info.FileName=$Java;$info.UseShellExecute=$false;$info.CreateNoWindow=$true
    $info.RedirectStandardInput=$true;$info.RedirectStandardOutput=$true;$info.RedirectStandardError=$true
    $info.StandardInputEncoding=[Text.UTF8Encoding]::new($false)
    foreach($arg in @('-Dfile.encoding=UTF-8','-cp',"$Root/.build/phase23-single-probe-classes;$Root/lib/runtime/*",'rag.RagEmbeddingVectorCheck')) { $info.ArgumentList.Add($arg) }
    $process=[Diagnostics.Process]::new();$process.StartInfo=$info
    try {
        if (!$process.Start()) { throw 'Validator unavailable' }
        $stdout=$process.StandardOutput.ReadToEndAsync();$stderr=$process.StandardError.ReadToEndAsync()
        $process.StandardInput.Write((ConvertTo-Json -InputObject $Values -Compress -Depth 8));$process.StandardInput.Close()
        if (!$process.WaitForExit(10000)) { $process.Kill();throw 'Validator timeout' }
        if ($process.ExitCode -ne 0) { throw 'Validator failed' }
        # Never print stderr or vector input. Java stdout contains only allowlisted metrics.
        return ($stdout.GetAwaiter().GetResult()|ConvertFrom-Json)
    } finally { $process.Dispose() }
}

function Invoke-RagSingleEmbeddingProbe {
    param([Parameter(Mandatory)][string]$TestApiKey,[Parameter(Mandatory)]$Payload,
        [Parameter(Mandatory)][scriptblock]$Transport,[Parameter(Mandatory)][scriptblock]$VectorValidator,
        [ValidateSet('phase23-single-embedding-20261011-v1','phase23-corrected-payload-20261011-v2')]
        [string]$PlanId='phase23-single-embedding-20261011-v1')
    $result=[ordered]@{
        checkedAtUtc=[DateTime]::UtcNow.ToString('o');planId=$PlanId
        model='gemini-embedding-001';keySource='RAG_EMBEDDING_API_KEY'
        metadataGetRequests=0;embeddingPostRequests=0;retryRequests=0;chatGenerationRequests=0;databaseWrites=0
        httpStatus=$null;latencyMs=$null;expectedDimensions=768;returnedDimensions=$null;vectorValid=$false
        normBefore=$null;normAfter=$null;inputTokens=$null;tokenUsageAvailable=$false
        estimatedCostUsd=0;costBasis='Administrator-confirmed Free Tier; billing not measured'
        errorKind=$null;providerErrorCode=$null;providerStatus=$null;providerReasons=@();safeErrorMessage=$null
    }
    $request=[System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::Post,
        'https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent')
    $timer=$null
    try {
        $request.Headers.Add('x-goog-api-key',$TestApiKey)
        $request.Content=[System.Net.Http.StringContent]::new(($Payload|ConvertTo-Json -Compress -Depth 8),[Text.Encoding]::UTF8,'application/json')
        $timer=[Diagnostics.Stopwatch]::StartNew()
        $result.embeddingPostRequests=1
        $response=& $Transport $request
        $timer.Stop();$result.latencyMs=[math]::Round($timer.Elapsed.TotalMilliseconds,1)
        $result.httpStatus=[int]$response.status
        if ($result.httpStatus -ge 200 -and $result.httpStatus -lt 300) {
            try {
                if ([string]::IsNullOrWhiteSpace($response.body) -or $response.body.Length -gt 65536) { throw 'Invalid response' }
                $json=ConvertFrom-Json -InputObject $response.body -Depth 32 -ErrorAction Stop
                $tokens=0
                if ([int]::TryParse([string]$json.usageMetadata.promptTokenCount,[ref]$tokens) -and $tokens -ge 0 -and $tokens -le 2048) {
                    $result.inputTokens=$tokens;$result.tokenUsageAvailable=$true
                }
                $metrics=& $VectorValidator $json.embedding.values
                $result.returnedDimensions=$metrics.returnedDimensions;$result.vectorValid=$metrics.vectorValid
                $result.normBefore=$metrics.normBefore;$result.normAfter=$metrics.normAfter
                if (!$result.vectorValid -or $result.returnedDimensions -ne 768) { throw 'Invalid vector' }
            } catch { $result.vectorValid=$false;$result.errorKind='INVALID_EMBEDDING_RESPONSE';$result.safeErrorMessage='Response embedding không hợp lệ hoặc không xác nhận được vector 768 chiều.' }
        } else {
            $safe=Get-RagSafeProviderError -HttpStatus $result.httpStatus -Body $response.body
            foreach($field in @('errorKind','providerErrorCode','providerStatus','providerReasons','safeErrorMessage')) { $result[$field]=$safe.$field }
        }
    } catch {
        if ($null -ne $timer) { $timer.Stop();$result.latencyMs=[math]::Round($timer.Elapsed.TotalMilliseconds,1) }
        $result.errorKind='TRANSPORT_OR_TIMEOUT';$result.safeErrorMessage='Không nhận được response hợp lệ; không retry.'
    } finally { $request.Dispose() }
    return [pscustomobject]$result
}
