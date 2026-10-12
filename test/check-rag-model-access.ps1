# Opt-in, ONE metadata GET only. Test key only; no retry, redirect, embedding or JDBC.
param([switch]$ApprovedSingleMetadataGet)
$ErrorActionPreference = 'Stop'
if (!$ApprovedSingleMetadataGet) { throw 'Explicit single metadata GET approval is required' }
if ([Environment]::GetEnvironmentVariable('RAG_ENVIRONMENT','Process') -cnotin @('development','staging')) {
    throw 'A separate development/staging test process is required; no request sent'
}
if ([Environment]::GetEnvironmentVariable('RAG_ENABLED','Process') -ieq 'true') {
    throw 'Keep RAG_ENABLED=false in the probe process; no request sent'
}
. "$PSScriptRoot/rag-auth-diagnostics.ps1"
$key = Get-RagTestApiKey
$root = Split-Path -Parent $PSScriptRoot
# Preserve historical telemetry; only safe sanitized data is written here.
$directory=Join-Path $root '.build/rag-auth-probes'
New-Item -ItemType Directory -Force -Path $directory | Out-Null
$output=Join-Path $directory (([DateTime]::UtcNow.ToString('yyyyMMddTHHmmssfff'))+'-'+[guid]::NewGuid().ToString('N')+'.json')
$handler = [System.Net.Http.HttpClientHandler]::new()
$handler.AllowAutoRedirect = $false
$handler.UseCookies = $false
$client = [System.Net.Http.HttpClient]::new($handler)
$client.Timeout = [TimeSpan]::FromSeconds(10)
$client.MaxResponseContentBufferSize = 65536
try {
    $result = Invoke-RagSingleMetadataProbe -TestApiKey $key -Transport {
        param($request)
        # One explicit SendAsync; HttpClient does not follow redirects. No fallback/retry.
        $deadline=[Threading.CancellationTokenSource]::new(10000)
        try {
            # Preserve HTTP status even when body is oversized or body reading times out.
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
    }
} finally { $key=$null; $client.Dispose() }
$safeJson=$result | ConvertTo-Json -Depth 5
[IO.File]::WriteAllText($output,$safeJson,[Text.UTF8Encoding]::new($false))
Write-Output $safeJson
Write-Output "Sanitized result: $output"
if (!$result.modelAccessVerified) { exit 1 }
