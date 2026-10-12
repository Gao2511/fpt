# ONE approved POST only; no GET/retry/DB/chat/global-key fallback. Requires PowerShell 7.
param([switch]$ApprovedSingleEmbeddingPost,
    [ValidateSet('phase23-single-embedding-20261011-v1','phase23-corrected-payload-20261011-v2')]
    [string]$PlanId='phase23-single-embedding-20261011-v1')
$ErrorActionPreference='Stop'
if (!$ApprovedSingleEmbeddingPost) { throw 'Explicit single embedding POST approval is required' }
if ([Environment]::GetEnvironmentVariable('RAG_ENVIRONMENT','Process') -cnotin @('development','staging') -or
    [Environment]::GetEnvironmentVariable('RAG_ENABLED','Process') -cne 'false') { throw 'A separate test process with RAG_ENABLED=false is required; no request sent' }
. "$PSScriptRoot/rag-single-embedding-helpers.ps1"
$key=Get-RagTestApiKey
$root=Split-Path -Parent $PSScriptRoot
$payload=Get-RagSingleEmbeddingPayload -Root $root -PlanId $PlanId
$java=(Get-Command java -ErrorAction SilentlyContinue).Source
if (!$java) { $java='C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot\bin\java.exe' }
if (!(Test-Path -LiteralPath $java) -or !(Test-Path -LiteralPath "$root/.build/phase23-single-probe-classes/rag/RagEmbeddingVectorCheck.class")) { throw 'Run offline preparation with JDK 17 first; no request sent' }
# Exercise validator locally before consuming approval/request lock.
$localVector=[double[]]::new(768);$localVector[0]=1
$localCheck=Invoke-RagVectorValidator -Root $root -Java $java -Values $localVector
if (!$localCheck.vectorValid) { throw 'Local vector validator unavailable; no request sent' }
$paths=Claim-RagEmbeddingExecution -Root $root -PlanId $PlanId
$output=$paths.result
$handler=[System.Net.Http.HttpClientHandler]::new();$handler.AllowAutoRedirect=$false;$handler.UseCookies=$false
$client=[System.Net.Http.HttpClient]::new($handler);$client.Timeout=[TimeSpan]::FromSeconds(10)
try {
    $result=Invoke-RagSingleEmbeddingProbe -TestApiKey $key -Payload $payload -PlanId $PlanId -Transport {
        param($request)
        $deadline=[Threading.CancellationTokenSource]::new(10000)
        try {
            # Exactly one explicit HTTP invocation; no other endpoints or fallback.
            $response=$client.SendAsync($request,[System.Net.Http.HttpCompletionOption]::ResponseHeadersRead,$deadline.Token).GetAwaiter().GetResult()
            try {
                $body=$null
                try {
                    $stream=$response.Content.ReadAsStreamAsync($deadline.Token).GetAwaiter().GetResult()
                    try { $body=Read-RagBoundedText -Stream $stream -CancellationToken $deadline.Token } finally { $stream.Dispose() }
                } catch { }
                [pscustomobject]@{status=[int]$response.StatusCode;body=$body}
            } finally { $response.Dispose() }
        } finally { $deadline.Dispose() }
    } -VectorValidator { param($values); Invoke-RagVectorValidator -Root $root -Java $java -Values $values }
} finally { $key=$null;$client.Dispose() }
[IO.File]::WriteAllText($output,($result|ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false))
$result|ConvertTo-Json -Depth 8
Write-Output "Sanitized result: $output"
if (!$result.vectorValid) { exit 1 }
