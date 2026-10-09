param([string]$TomcatHome = 'D:\Subject FPT\PRJ301\apache-tomcat-9.0.118-windows-x64\apache-tomcat-9.0.118')
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$testClasses = Join-Path $env:TEMP ('fpt-chat-tests-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $testClasses | Out-Null
$servletJar = Join-Path $TomcatHome 'lib/servlet-api.jar'
if (!(Test-Path -LiteralPath $servletJar)) { throw 'Specify -TomcatHome for your Tomcat 9 installation.' }
$dependencies = (Join-Path $repoRoot 'build/web/WEB-INF/lib/*') + ';' + $servletJar
$sources = @(Get-ChildItem -LiteralPath (Join-Path $repoRoot 'src/java'),(Join-Path $repoRoot 'test') -Recurse -Filter '*.java' | ForEach-Object FullName)
& javac -encoding UTF-8 --release 8 -cp $dependencies -d $testClasses $sources
if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed' }
foreach ($suite in @('ai.ConsultationBehaviorTest','controller.ChatRegistrationTest','ai.provider.ProviderResponseTest')) {
    & java '-Dfile.encoding=UTF-8' -cp "$testClasses;$dependencies" $suite
    if ($LASTEXITCODE -ne 0) { throw "Test suite failed: $suite" }
}
& node (Join-Path $repoRoot 'test-e2e/chatbot-test.cjs')
if ($LASTEXITCODE -ne 0) { throw 'Chat frontend tests failed' }
Write-Output 'All offline chatbot tests passed. No production database, email or AI API was used.'
