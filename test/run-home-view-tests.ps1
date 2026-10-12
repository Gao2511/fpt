param([Parameter(Mandatory)][string]$TomcatHome)
$ErrorActionPreference='Stop'
$repoRoot=Split-Path -Parent $PSScriptRoot
$webRoot=Join-Path $repoRoot '.build/web'
if(!(Test-Path "$webRoot/WEB-INF/classes/controller/HomeServlet.class")){throw 'Run ant -f build-portable.xml war first'}
$work=Join-Path $repoRoot ('.build/home-view-test-'+[guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path "$work/classes" -Force | Out-Null
$jars=@(Get-ChildItem -LiteralPath "$TomcatHome/lib","$webRoot/WEB-INF/lib" -Filter '*.jar' | ForEach-Object FullName)
$classpath=($jars+"$TomcatHome/bin/tomcat-juli.jar"+"$webRoot/WEB-INF/classes"+"$work/classes") -join ';'
& javac -encoding UTF-8 --release 8 -cp $classpath -d "$work/classes" "$repoRoot/test/cms/HomeViewTest.java"
if($LASTEXITCODE-ne 0){throw 'Home view test compilation failed'}
# Synthetic servlet data only. No production servlet/filter mappings or DAO/AI requests.
$env:RAG_ENABLED='false'
& java '-Dfile.encoding=UTF-8' -cp $classpath cms.HomeViewTest $webRoot "$work/evidence" 2>&1 | Tee-Object -FilePath "$work/render.log"
if($LASTEXITCODE-ne 0){throw 'Home JSP render test failed'}
& node "$repoRoot/test/cms/home-view-scripts.test.cjs" "$work/evidence/anonymous.html" | Tee-Object -FilePath "$work/scripts.log"
if($LASTEXITCODE-ne 0){throw 'Rendered JavaScript test failed'}
Write-Output "EVIDENCE=$work"
