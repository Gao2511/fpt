# Deterministic mock-only tests. No HttpClient, SendAsync, JDBC or provider calls.
param()
$ErrorActionPreference='Stop'
. "$PSScriptRoot/rag-auth-diagnostics.ps1"
$script:passed=0
$script:mockResults=@()
$script:fakeKey='FAKE_OFFLINE_SECRET_SENTINEL'
function Assert-Check([bool]$condition) { if (!$condition) { throw 'Offline auth assertion failed' } }
function Test-Case([string]$name,[scriptblock]$body) { & $body; $script:passed++; Write-Output "PASS $name" }
function Assert-NoLeak($result) {
    $json=$result | ConvertTo-Json -Depth 8
    foreach ($marker in @($script:fakeKey,'PRIVATE_PROJECT_SENTINEL','PRIVATE_EMAIL_SENTINEL','PRIVATE_URL_SENTINEL')) {
        Assert-Check (!$json.Contains($marker))
    }
}
function Mock-Probe([int]$status,[string]$body) {
    $counter=[pscustomobject]@{calls=0}
    $fakeKey=$script:fakeKey
    $mock = {
        param($request)
        $counter.calls++
        Assert-Check ($request.Method.Method -ceq 'GET')
        Assert-Check ($request.RequestUri.AbsoluteUri -ceq 'https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001')
        Assert-Check ($null -eq $request.Content)
        Assert-Check (!$request.Headers.Contains('Authorization'))
        Assert-Check (@($request.Headers.GetValues('x-goog-api-key'))[0] -ceq $fakeKey)
        [pscustomobject]@{status=$status;body=$body}
    }
    $result=Invoke-RagSingleMetadataProbe -TestApiKey $script:fakeKey -Transport $mock
    Assert-Check ($counter.calls -eq 1 -and $result.metadataGetAttempts -eq 1)
    Assert-Check ($result.embeddingPostRequests -eq 0 -and $result.chatGenerationRequests -eq 0)
    Assert-NoLeak $result
    $script:mockResults+= $result
    return $result
}
function Error-Body([int]$code,[string]$status,[string]$reason) {
    @{error=@{code=$code;status=$status;message="RAW $script:fakeKey PRIVATE_EMAIL_SENTINEL";
        details=@(@{'@type'='type.googleapis.com/google.rpc.ErrorInfo';reason=$reason;
            domain='PRIVATE_PROJECT_SENTINEL';metadata=@{key=$script:fakeKey;url='PRIVATE_URL_SENTINEL'}})}} | ConvertTo-Json -Depth 8
}
$names=@('RAG_EMBEDDING_API_KEY','GEMINI_API_KEY','GOOGLE_API_KEY')
$old=@{}
foreach ($name in $names) { $old[$name]=[Environment]::GetEnvironmentVariable($name,'Process') }
try {
    Test-Case 'approval switch required before any live client' {
        $rejected=$false
        try { & "$PSScriptRoot/check-rag-model-access.ps1" } catch { $rejected=$_.Exception.Message -ceq 'Explicit single metadata GET approval is required' }
        Assert-Check $rejected
    }
    Test-Case 'missing dedicated key never falls back to global keys' {
        [Environment]::SetEnvironmentVariable('RAG_EMBEDDING_API_KEY',$null,'Process')
        [Environment]::SetEnvironmentVariable('GEMINI_API_KEY','FAKE_GLOBAL_GEMINI','Process')
        [Environment]::SetEnvironmentVariable('GOOGLE_API_KEY','FAKE_GLOBAL_GOOGLE','Process')
        $rejected=$false
        try { $unused=Get-RagTestApiKey } catch { $rejected=$_.Exception.Message -ceq 'RAG_EMBEDDING_API_KEY is unavailable; no request sent' }
        Assert-Check $rejected
    }
    Test-Case 'dedicated key resolves independently of both global variables' {
        [Environment]::SetEnvironmentVariable('RAG_EMBEDDING_API_KEY',$script:fakeKey,'Process')
        $resolved=Get-RagTestApiKey
        Assert-Check ($resolved -ceq $script:fakeKey)
    }
    Test-Case 'whitespace or CRLF key rejected without revealing value' {
        foreach ($value in @(" $script:fakeKey", "$script:fakeKey`n")) {
            [Environment]::SetEnvironmentVariable('RAG_EMBEDDING_API_KEY',$value,'Process')
            $rejected=$false
            try { $unused=Get-RagTestApiKey } catch { $rejected=$_.Exception.Message -ceq 'Test key contains invalid whitespace; no request sent' }
            Assert-Check $rejected
        }
    }
    Test-Case 'valid metadata confirms model with exactly one GET' {
        $r=Mock-Probe 200 '{"name":"models/gemini-embedding-001","supportedGenerationMethods":["embedContent"],"inputTokenLimit":2048}'
        Assert-Check ($r.modelAccessVerified -and $r.inputTokenLimit -eq 2048 -and !$r.pricingVerified -and !$r.quotaVerified)
    }
    Test-Case 'wrong metadata model rejected' {
        $r=Mock-Probe 200 '{"name":"models/other","supportedGenerationMethods":["embedContent"]}'
        Assert-Check (!$r.modelAccessVerified -and $r.errorKind -ceq 'UNEXPECTED_METADATA')
    }
    Test-Case 'missing embedContent method rejected' {
        $r=Mock-Probe 200 '{"name":"models/gemini-embedding-001","supportedGenerationMethods":["generateContent"]}'
        Assert-Check (!$r.modelAccessVerified)
    }
    Test-Case '401 stores status and allowlisted provider reason without raw secret' {
        $r=Mock-Probe 401 (Error-Body 401 'UNAUTHENTICATED' 'API_KEY_INVALID')
        Assert-Check ($r.httpStatus -eq 401 -and $r.providerErrorCode -eq 401 -and $r.providerStatus -ceq 'UNAUTHENTICATED' -and $r.providerReasons[0] -ceq 'API_KEY_INVALID')
        Assert-Check ($r.safeErrorMessage -ceq 'Google báo key không hợp lệ.' -and $r.providerFieldsRedacted)
    }
    Test-Case '403 restriction and IAM reasons kept distinct' {
        foreach ($reason in @('API_KEY_IP_ADDRESS_BLOCKED','API_KEY_SERVICE_BLOCKED','IAM_PERMISSION_DENIED')) {
            $r=Mock-Probe 403 (Error-Body 403 'PERMISSION_DENIED' $reason)
            Assert-Check ($r.providerReasons[0] -ceq $reason -and $r.httpStatus -eq 403)
        }
    }
    Test-Case 'unknown provider strings containing secret are discarded' {
        $r=Mock-Probe 401 (Error-Body 401 $script:fakeKey $script:fakeKey)
        Assert-Check ($null -eq $r.providerStatus -and $r.providerReasons.Count -eq 0)
    }
    Test-Case 'ErrorInfo type must match before reason is accepted' {
        $body=@{error=@{status='UNAUTHENTICATED';details=@(@{'@type'='unknown';reason='API_KEY_INVALID'})}}|ConvertTo-Json -Depth 6
        $r=Mock-Probe 401 $body
        Assert-Check ($r.providerReasons.Count -eq 0)
    }
    Test-Case 'malformed error JSON has safe HTTP fallback' {
        $r=Mock-Probe 401 ("broken $script:fakeKey")
        Assert-Check (!$r.providerErrorParsed -and $r.httpStatus -eq 401 -and $r.errorKind -ceq 'AUTHENTICATION')
    }
    Test-Case 'empty error body preserves HTTP status' {
        $r=Mock-Probe 401 ''
        Assert-Check ($r.httpStatus -eq 401 -and !$r.providerErrorParsed)
    }
    Test-Case 'oversized error body ignored' {
        $r=Mock-Probe 401 ('x'*65537)
        Assert-Check (!$r.providerErrorParsed -and $r.httpStatus -eq 401)
    }
    Test-Case 'quota error is not retried' {
        $r=Mock-Probe 429 (Error-Body 429 'RESOURCE_EXHAUSTED' 'QUOTA_EXCEEDED')
        Assert-Check ($r.errorKind -ceq 'RATE_LIMIT' -and $r.providerReasons[0] -ceq 'QUOTA_EXCEEDED')
    }
    Test-Case 'outage error is not retried' {
        $r=Mock-Probe 503 (Error-Body 503 'UNAVAILABLE' '')
        Assert-Check ($r.httpStatus -eq 503 -and !$r.modelAccessVerified)
    }
    Test-Case 'redirect response does not cause another transport invocation' {
        $r=Mock-Probe 302 ''
        Assert-Check ($r.httpStatus -eq 302 -and !$r.modelAccessVerified)
    }
    Test-Case 'network exception text containing secret is suppressed' {
        $counter=[pscustomobject]@{calls=0}
        $r=Invoke-RagSingleMetadataProbe -TestApiKey $script:fakeKey -Transport { param($request); $counter.calls++; throw "failure $script:fakeKey" }
        Assert-Check ($counter.calls -eq 1 -and $r.errorKind -ceq 'TRANSPORT_OR_TIMEOUT' -and $null -eq $r.httpStatus)
        Assert-NoLeak $r
    }
    Test-Case 'malformed success metadata rejected without leaking body' {
        $r=Mock-Probe 200 ("broken $script:fakeKey")
        Assert-Check (!$r.modelAccessVerified -and $r.errorKind -ceq 'INVALID_METADATA')
    }
    Test-Case 'multiple reasons do not imply a single root cause' {
        $body=@{error=@{code=403;status='PERMISSION_DENIED';details=@(
            @{'@type'='type.googleapis.com/google.rpc.ErrorInfo';reason='API_KEY_SERVICE_BLOCKED'},
            @{'@type'='type.googleapis.com/google.rpc.ErrorInfo';reason='IAM_PERMISSION_DENIED'})}}|ConvertTo-Json -Depth 6
        $r=Mock-Probe 403 $body
        Assert-Check ($r.providerReasons.Count -eq 2 -and $r.safeErrorMessage -ceq 'Request bị từ chối do quyền hoặc hạn chế truy cập.')
    }
    Test-Case 'actual live wrapper has one SendAsync and disables redirects' {
        $source=Get-Content -LiteralPath "$PSScriptRoot/check-rag-model-access.ps1" -Raw
        Assert-Check (([regex]::Matches($source,'\$client\.SendAsync\(')).Count -eq 1)
        Assert-Check ($source.Contains('$handler.AllowAutoRedirect = $false'))
        Assert-Check (!$source.Contains('GEMINI_API_KEY') -and !$source.Contains('GOOGLE_API_KEY'))
    }
    Test-Case 'bounded body reader preserves UTF8 text in memory only' {
        $content='Lỗi xác thực tiếng Việt.'
        $stream=[IO.MemoryStream]::new([Text.Encoding]::UTF8.GetBytes($content))
        try { $read=Read-RagBoundedText -Stream $stream; Assert-Check ($read -ceq $content) } finally { $stream.Dispose() }
    }
    Test-Case 'body byte limit rejects multibyte payload beyond 64KiB' {
        $stream=[IO.MemoryStream]::new([Text.Encoding]::UTF8.GetBytes(('ế'*30000)))
        try { $read=Read-RagBoundedText -Stream $stream; Assert-Check ($null -eq $read) } finally { $stream.Dispose() }
    }
    Test-Case 'body read respects cancellation deadline' {
        $stream=[IO.MemoryStream]::new([Text.Encoding]::UTF8.GetBytes('body'))
        $cancel=[Threading.CancellationTokenSource]::new()
        $cancel.Cancel()
        $rejected=$false
        try { $unused=Read-RagBoundedText -Stream $stream -CancellationToken $cancel.Token } catch { $rejected=$true }
        finally { $stream.Dispose(); $cancel.Dispose() }
        Assert-Check $rejected
    }
    Test-Case 'persisted mock diagnostics contain no secret sentinel' {
        $root=Split-Path -Parent $PSScriptRoot
        $artifact=Join-Path $root '.build/phase23-auth-mock-tests.json'
        New-Item -ItemType Directory -Force -Path (Split-Path -Parent $artifact) | Out-Null
        $report=[pscustomobject]@{mockOnly=$true;realApiRequests=0;results=$script:mockResults}
        Assert-NoLeak $report
        [IO.File]::WriteAllText($artifact,($report|ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false))
        Assert-Check (!(Get-Content -LiteralPath $artifact -Raw).Contains($script:fakeKey))
    }
    Write-Output "AuthRecoveryTests: $script:passed PASS / 0 FAIL; real API requests=0"
} finally {
    foreach ($name in $names) { [Environment]::SetEnvironmentVariable($name,$old[$name],'Process') }
}
