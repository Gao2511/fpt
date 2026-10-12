param([string]$AntHome=$env:ANT_HOME)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
& "$PSScriptRoot/compile-rag-live-runner.ps1" -AntHome $AntHome
$classes="$root/.build/phase241-test-classes"
$deps="$root/.build/web/WEB-INF/classes;$root/lib/runtime/*;$root/lib/compile-only/*"
& java '-Dfile.encoding=UTF-8' -cp "$classes;$deps" rag.live.LiveRunnerSafetyTest $root
if($LASTEXITCODE -ne 0){throw 'Offline safety tests failed'}
