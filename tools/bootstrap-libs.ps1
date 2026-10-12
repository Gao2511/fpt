param([switch]$VerifyOnly)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
foreach($line in Get-Content -LiteralPath (Join-Path $PSScriptRoot 'dependencies.lock')) {
    if($line.StartsWith('#') -or [string]::IsNullOrWhiteSpace($line)){continue}
    $hash,$relative,$url=$line -split '\s+',3
    $path=Join-Path $root $relative
    if(!(Test-Path -LiteralPath $path)) {
        if($VerifyOnly){throw "Missing dependency: $relative"}
        New-Item -ItemType Directory -Path (Split-Path -Parent $path) -Force | Out-Null
        Invoke-WebRequest -Uri $url -OutFile $path -TimeoutSec 60
    }
    if((Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant() -ne $hash){throw "Dependency checksum mismatch: $relative"}
    Write-Output "Verified $relative"
}
