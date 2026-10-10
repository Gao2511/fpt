$ErrorActionPreference='Stop'
$localRoot=Join-Path $PSScriptRoot '.local'
$base=Join-Path $localRoot 'tomcat'
$tomcatHome='D:\Subject FPT\PRJ301\apache-tomcat-9.0.118-windows-x64\apache-tomcat-9.0.118'
if(Test-Path (Join-Path $localRoot 'tomcat.pid')) {
 & java "-Dcatalina.home=$tomcatHome" "-Dcatalina.base=$base" -classpath "$tomcatHome/bin/bootstrap.jar;$tomcatHome/bin/tomcat-juli.jar" org.apache.catalina.startup.Bootstrap stop
 if($LASTEXITCODE -ne 0){throw 'Tomcat shutdown failed'}
}
& 'C:\Program Files\PostgreSQL\18\bin\pg_ctl.exe' -D (Join-Path $localRoot 'postgres/data') -m fast -w stop
if($LASTEXITCODE -ne 0){throw 'PostgreSQL shutdown failed'}

