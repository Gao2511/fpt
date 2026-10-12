param([string]$TomcatHome='D:/Subject FPT/PRJ301/apache-tomcat-9.0.118-windows-x64/apache-tomcat-9.0.118',[string]$Chrome)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
$evidence=Join-Path $root ('.build/kb57-view-'+[guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $evidence | Out-Null
$web=Join-Path $evidence 'web'
New-Item -ItemType Directory -Path "$web/WEB-INF/lib" -Force | Out-Null
foreach($folder in @('view','assets','js')){Copy-Item -LiteralPath "$root/web/$folder" -Destination $web -Recurse}
Copy-Item -LiteralPath "$root/lib/runtime/jstl-1.2.jar" -Destination "$web/WEB-INF/lib"
# Synthetic app descriptor: do not scan production servlets, filters or opt-in worker listeners.
[IO.File]::WriteAllText("$web/WEB-INF/web.xml",'<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee" version="4.0" metadata-complete="true"></web-app>')
$deps="$root/.build/web/WEB-INF/classes;$root/lib/runtime/*;$TomcatHome/lib/*;$TomcatHome/bin/tomcat-juli.jar"
& javac -encoding UTF-8 --release 8 -cp $deps -d $evidence "$PSScriptRoot/cms/KnowledgeViewServer.java"
if($LASTEXITCODE -ne 0){throw 'View fixture compilation failed'}
$vars=@{KB57_TEST_CLASSPATH="$evidence;$deps";KB57_WEB=$web;KB57_EVIDENCE=$evidence;KB57_CHROME=$Chrome}
$before=@{}
try{foreach($key in $vars.Keys){$before[$key]=[Environment]::GetEnvironmentVariable($key,'Process');[Environment]::SetEnvironmentVariable($key,$vars[$key],'Process')}
 & node "$root/test-e2e/knowledge-lifecycle.cjs"
 if($LASTEXITCODE -ne 0){throw 'KB57 browser safety check failed'}
}finally{foreach($key in $before.Keys){[Environment]::SetEnvironmentVariable($key,$before[$key],'Process')}}
Write-Output "View evidence: $evidence"
