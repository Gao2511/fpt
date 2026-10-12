param([string]$AntHome=$env:ANT_HOME,[switch]$Test)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
& "$PSScriptRoot/bootstrap-libs.ps1" -VerifyOnly
if($AntHome){$ant=Join-Path $AntHome 'bin/ant.bat'}else{$ant=(Get-Command ant -ErrorAction Stop).Source}
$targets=@('war');if($Test){$targets+= 'test'}
& $ant -f (Join-Path $root 'build-portable.xml') @targets
if($LASTEXITCODE -ne 0){throw 'Portable Ant build/tests failed'}
