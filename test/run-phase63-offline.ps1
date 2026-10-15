param([string]$TomcatHome='D:\Subject FPT\PRJ301\apache-tomcat-9.0.118-windows-x64\apache-tomcat-9.0.118')
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
$classes=Join-Path $env:TEMP ('fpt-phase63-'+[guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $classes|Out-Null
try {
    $servlet=Join-Path $TomcatHome 'lib/servlet-api.jar'
    if(!(Test-Path -LiteralPath $servlet)){throw 'Tomcat 9 servlet-api.jar is required'}
    $cp=(Join-Path $root 'build/web/WEB-INF/lib/*')+';'+$servlet
    $sources=@(Get-ChildItem -LiteralPath (Join-Path $root 'src/java') -Recurse -Filter '*.java'|ForEach-Object FullName)
    $sources+=(Join-Path $root 'test/ai/Phase63ConciseUnderstandingTest.java')
    & javac -encoding UTF-8 --release 8 -cp $cp -d $classes $sources
    if($LASTEXITCODE -ne 0){throw 'Phase 6.3 Java compilation failed'}
    Push-Location $root
    try { & java '-Dfile.encoding=UTF-8' -cp "$classes;$cp" ai.Phase63ConciseUnderstandingTest }
    finally { Pop-Location }
    if($LASTEXITCODE -ne 0){throw 'Phase 6.3 acceptance test failed'}
} finally { Remove-Item -LiteralPath $classes -Recurse -Force -ErrorAction SilentlyContinue }
