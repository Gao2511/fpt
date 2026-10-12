# Test-only helpers. Loading this file never reads credentials, sends HTTP or writes files.
function Get-RagTestApiKey {
    $value = [Environment]::GetEnvironmentVariable('RAG_EMBEDDING_API_KEY', 'Process')
    if ([string]::IsNullOrWhiteSpace($value)) { throw 'RAG_EMBEDDING_API_KEY is unavailable; no request sent' }
    if ($value -cne $value.Trim() -or $value -match '[\r\n]') { throw 'Test key contains invalid whitespace; no request sent' }
    # Never fall back to global GEMINI_API_KEY, GOOGLE_API_KEY or database settings.
    return $value
}

function Read-RagBoundedText {
    param([Parameter(Mandatory)][IO.Stream]$Stream,
        [Threading.CancellationToken]$CancellationToken=[Threading.CancellationToken]::None)
    $buffer=[byte[]]::new(4096)
    $output=[IO.MemoryStream]::new()
    try {
        while (($count=$Stream.ReadAsync($buffer,0,$buffer.Length,$CancellationToken).GetAwaiter().GetResult()) -gt 0) {
            if ($output.Length + $count -gt 65536) { return $null }
            $output.Write($buffer,0,$count)
        }
        return [Text.Encoding]::UTF8.GetString($output.ToArray())
    } finally { $output.Dispose() }
}

function Get-RagSafeProviderError {
    param([int]$HttpStatus, [AllowNull()][string]$Body)
    $safe = [ordered]@{
        providerErrorCode = $null
        providerStatus = $null
        providerReasons = @()
        providerErrorParsed = $false
        providerFieldsRedacted = $false
        errorKind = 'HTTP_ERROR'
        safeErrorMessage = 'Provider từ chối request; chưa xác định nguyên nhân cụ thể.'
    }
    switch ($HttpStatus) {
        400 { $safe.errorKind='CONFIGURATION'; $safe.safeErrorMessage='Request hoặc cấu hình xác thực không hợp lệ.' }
        401 { $safe.errorKind='AUTHENTICATION'; $safe.safeErrorMessage='Request bị từ chối xác thực; chưa đủ dữ liệu để xác định nguyên nhân của key.' }
        403 { $safe.errorKind='PERMISSION'; $safe.safeErrorMessage='Request bị từ chối do quyền hoặc hạn chế truy cập.' }
        404 { $safe.errorKind='MODEL_UNAVAILABLE'; $safe.safeErrorMessage='Không tìm thấy model hoặc tài nguyên có thể truy cập.' }
        429 { $safe.errorKind='RATE_LIMIT'; $safe.safeErrorMessage='Provider báo giới hạn request hoặc quota.' }
        503 { $safe.errorKind='PROVIDER'; $safe.safeErrorMessage='Provider tạm thời không sẵn sàng.' }
    }
    if ([string]::IsNullOrWhiteSpace($Body) -or $Body.Length -gt 65536) { return [pscustomobject]$safe }
    try {
        $json = ConvertFrom-Json -InputObject $Body -Depth 32 -ErrorAction Stop
        if ($null -eq $json.error -or $json.error -isnot [pscustomobject]) { return [pscustomobject]$safe }
        $errorObject = $json.error
        $safe.providerErrorParsed = $true
        # Raw message, metadata, domains, headers and unknown strings are always discarded.
        $safe.providerFieldsRedacted = $true
        $number = 0
        if ([int]::TryParse([string]$errorObject.code, [ref]$number) -and $number -ge 100 -and $number -le 599) { $safe.providerErrorCode = $number }
        $allowedStatuses = @('UNAUTHENTICATED','PERMISSION_DENIED','INVALID_ARGUMENT','NOT_FOUND',
            'RESOURCE_EXHAUSTED','UNAVAILABLE','INTERNAL','DEADLINE_EXCEEDED','FAILED_PRECONDITION','UNKNOWN')
        if ($allowedStatuses -ccontains $errorObject.status) { $safe.providerStatus = [string]$errorObject.status }
        $reasonMessages = @{
            API_KEY_INVALID='Google báo key không hợp lệ.'
            API_KEY_EXPIRED='Google báo key đã hết hạn.'
            API_KEY_NOT_FOUND='Google báo không tìm thấy key.'
            API_KEY_SERVICE_BLOCKED='Google báo restriction của key chặn dịch vụ.'
            API_KEY_IP_ADDRESS_BLOCKED='Google báo restriction địa chỉ IP chặn request.'
            API_KEY_HTTP_REFERRER_BLOCKED='Google báo restriction HTTP referrer chặn request.'
            API_KEY_ANDROID_APP_BLOCKED='Google báo restriction ứng dụng Android chặn request.'
            API_KEY_IOS_APP_BLOCKED='Google báo restriction ứng dụng iOS chặn request.'
            CREDENTIALS_MISSING='Google báo request thiếu credential xác thực.'
            ACCESS_TOKEN_EXPIRED='Google báo credential xác thực đã hết hạn.'
            ACCESS_TOKEN_SCOPE_INSUFFICIENT='Google báo credential không đủ scope.'
            SERVICE_DISABLED='Google báo dịch vụ API chưa được bật.'
            IAM_PERMISSION_DENIED='Google báo quyền IAM không đủ.'
            BILLING_DISABLED='Google báo billing chưa được bật.'
            RATE_LIMIT_EXCEEDED='Google báo giới hạn request đã vượt mức.'
            QUOTA_EXCEEDED='Google báo quota đã vượt mức.'
        }
        $reasons = @()
        foreach ($detail in @($errorObject.details) | Select-Object -First 10) {
            if ($detail.'@type' -ceq 'type.googleapis.com/google.rpc.ErrorInfo' -and
                @($reasonMessages.Keys) -ccontains $detail.reason -and $reasons -cnotcontains $detail.reason) { $reasons += [string]$detail.reason }
        }
        $safe.providerReasons = $reasons
        if ($reasons.Count -eq 1) { $safe.safeErrorMessage = $reasonMessages[$reasons[0]] }
        # Multiple reasons remain explicit codes; do not guess which one caused the failure.
    } catch { $safe.providerErrorParsed = $false }
    return [pscustomobject]$safe
}

function Invoke-RagSingleMetadataProbe {
    param([Parameter(Mandatory)][string]$TestApiKey, [Parameter(Mandatory)][scriptblock]$Transport)
    $result = [ordered]@{
        checkedAtUtc=[DateTime]::UtcNow.ToString('o')
        requestedModel='gemini-embedding-001'
        keySource='RAG_EMBEDDING_API_KEY'
        metadataGetAttempts=0
        embeddingPostRequests=0
        chatGenerationRequests=0
        httpStatus=$null
        latencyMs=$null
        expectedModelNameReturned=$false
        supportsEmbedContent=$false
        inputTokenLimit=$null
        outputTokenLimit=$null
        modelAccessVerified=$false
        errorKind=$null
        providerErrorCode=$null
        providerStatus=$null
        providerReasons=@()
        providerErrorParsed=$false
        providerFieldsRedacted=$false
        safeErrorMessage=$null
        pricingVerified=$false
        quotaVerified=$false
    }
    $request = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::Get,
        'https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001')
    $timer = [Diagnostics.Stopwatch]::StartNew()
    try {
        $request.Headers.Add('x-goog-api-key', $TestApiKey)
        $result.metadataGetAttempts = 1
        # ONE transport invocation. The real transport must have no retry or redirect.
        $response = & $Transport $request
        $result.httpStatus = [int]$response.status
        if ($result.httpStatus -ge 200 -and $result.httpStatus -lt 300) {
            try {
                if ([string]$response.body -eq '' -or $response.body.Length -gt 65536) { throw 'Invalid metadata' }
                $metadata = ConvertFrom-Json -InputObject $response.body -Depth 32 -ErrorAction Stop
                $result.expectedModelNameReturned = $metadata.name -ceq 'models/gemini-embedding-001'
                $result.supportsEmbedContent = @($metadata.supportedGenerationMethods) -ccontains 'embedContent'
                foreach ($field in @('inputTokenLimit','outputTokenLimit')) {
                    $number=0
                    if ([int]::TryParse([string]$metadata.$field,[ref]$number) -and $number -gt 0) { $result[$field]=$number }
                }
                $result.modelAccessVerified=$result.expectedModelNameReturned -and $result.supportsEmbedContent
                if (!$result.modelAccessVerified) { $result.errorKind='UNEXPECTED_METADATA'; $result.safeErrorMessage='Response không xác nhận model embedding được yêu cầu.' }
            } catch { $result.errorKind='INVALID_METADATA'; $result.safeErrorMessage='Provider trả metadata không hợp lệ.' }
        } else {
            $diagnostic=Get-RagSafeProviderError -HttpStatus $result.httpStatus -Body $response.body
            foreach ($field in @('errorKind','providerErrorCode','providerStatus','providerReasons',
                'providerErrorParsed','providerFieldsRedacted','safeErrorMessage')) { $result[$field]=$diagnostic.$field }
        }
    } catch { $result.errorKind='TRANSPORT_OR_TIMEOUT'; $result.safeErrorMessage='Không nhận được response hợp lệ trong giới hạn probe.' }
    finally { $timer.Stop(); $result.latencyMs=[math]::Round($timer.Elapsed.TotalMilliseconds,1); $request.Dispose() }
    return [pscustomobject]$result
}
