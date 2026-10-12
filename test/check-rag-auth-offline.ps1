# Offline only. No HttpClient/SendAsync/JDBC; never emits key values, hashes, prefixes or lengths.
param()
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$rows = foreach ($name in @('GEMINI_API_KEY', 'GOOGLE_API_KEY', 'RAG_EMBEDDING_API_KEY')) {
    $value = [Environment]::GetEnvironmentVariable($name, 'Process')
    $present = ![string]::IsNullOrWhiteSpace($value)
    $userValue = [Environment]::GetEnvironmentVariable($name, 'User')
    $machineValue = [Environment]::GetEnvironmentVariable($name, 'Machine')
    [pscustomobject]@{
        name = $name
        processPresent = $present
        userPresent = ![string]::IsNullOrWhiteSpace($userValue)
        machinePresent = ![string]::IsNullOrWhiteSpace($machineValue)
        processMatchesUser = ($present -and $value -ceq $userValue)
        processMatchesMachine = ($present -and $value -ceq $machineValue)
        hasOuterWhitespace = ($present -and $value -cne $value.Trim())
        hasLineBreak = ($present -and $value -match '[\r\n]')
        hasWrappingQuotes = ($present -and ($value.StartsWith('"') -or $value.EndsWith('"') -or $value.StartsWith("'") -or $value.EndsWith("'")))
        obviousPlaceholder = ($present -and $value -match '(?i)^(your[_ -]?(gemini[_ -]?)?(api[_ -]?)?key|replace[_ -]?me|changeme|fake.*|test[_ -]?key|<.*>)$')
        # Shape is heuristic only; it cannot prove validity, account access or key status.
        recognizedKeyShape = ($present -and ($value -cmatch '^AIza[A-Za-z0-9_-]{35}$' -or $value -cmatch '^AQ\.[A-Za-z0-9._-]+$'))
    }
}
$testKeyValue = [Environment]::GetEnvironmentVariable('RAG_EMBEDDING_API_KEY', 'Process')
$headerAccepted = $false
$headerMatches = $false
# Constructing a message is purely in memory. Never create a client or send it.
$message = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::Get,
    'https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001')
try {
    if (![string]::IsNullOrWhiteSpace($testKeyValue)) {
        $message.Headers.Add('x-goog-api-key', $testKeyValue)
        $headerAccepted = $true
        $headers = @($message.Headers.GetValues('x-goog-api-key'))
        $headerMatches = $headers.Count -eq 1 -and $headers[0] -ceq $testKeyValue
    }
} catch {
    # Exception text could contain input; emit only boolean diagnostics.
} finally { $message.Dispose() }
$modelValue = [Environment]::GetEnvironmentVariable('RAG_EMBEDDING_MODEL', 'Process')
$dimensionsValue = [Environment]::GetEnvironmentVariable('RAG_EMBEDDING_DIMENSIONS', 'Process')
$result = [ordered]@{
    checkedAtUtc = [DateTime]::UtcNow.ToString('o')
    offlineOnly = $true
    newApiRequests = 0
    environment = $rows
    metadataProbeKeySource = 'RAG_EMBEDDING_API_KEY'
    metadataProbeTestKeyPresent = ![string]::IsNullOrWhiteSpace($testKeyValue)
    headerAccepted = $headerAccepted
    headerMatchesProcessValue = $headerMatches
    configuredOrDefaultModelMatchesExpected = ($null -eq $modelValue -or $modelValue -ceq 'gemini-embedding-001')
    configuredOrDefaultDimensionsMatchExpected = ($null -eq $dimensionsValue -or $dimensionsValue -ceq '768')
    ragEnabled = ([Environment]::GetEnvironmentVariable('RAG_ENABLED', 'Process') -ieq 'true')
    embeddingCallsApproved = ([Environment]::GetEnvironmentVariable('RAG_EMBEDDING_CALLS_APPROVED', 'Process') -ieq 'true')
    modelVerified = ([Environment]::GetEnvironmentVariable('RAG_EMBEDDING_MODEL_VERIFIED', 'Process') -ieq 'true')
}
$output = Join-Path $root '.build/phase23-auth-offline.json'
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $output) | Out-Null
$safeJson = $result | ConvertTo-Json -Depth 5
[IO.File]::WriteAllText($output, $safeJson, [Text.UTF8Encoding]::new($false))
Write-Output $safeJson
