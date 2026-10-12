param(
    [Parameter(Mandatory)][string]$TomcatHome,
    [string]$War
)
$ErrorActionPreference='Stop'
$repoRoot=Split-Path -Parent $PSScriptRoot
if(!$War){$War=Join-Path $repoRoot 'dist/fpt-sale.war'}
$work=Join-Path $repoRoot ('.build/jstl-runtime-'+[guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path "$work/classes" -Force | Out-Null
& javac -encoding UTF-8 --release 8 -d "$work/classes" "$repoRoot/test/cms/JstlRuntimeTest.java"
if($LASTEXITCODE-ne 0){throw 'JSTL test compilation failed'}
& java '-Dfile.encoding=UTF-8' -cp "$work/classes" cms.JstlRuntimeTest $repoRoot $War $TomcatHome "$work/evidence" 2>&1 | Tee-Object -FilePath "$work/result.log"
if($LASTEXITCODE-ne 0){throw 'JSTL WAR runtime compatibility failed'}
Write-Output "EVIDENCE=$work"
