param([switch]$Build)
$ErrorActionPreference='Stop'
$repoRoot=$PSScriptRoot
$localRoot=Join-Path $repoRoot '.local'
$config=Get-Content -LiteralPath (Join-Path $localRoot 'runtime.json') -Raw | ConvertFrom-Json
if($config.DB_HOST -ne '127.0.0.1' -or $config.DB_NAME -ne 'fpt_local' -or $config.DB_PORT -ne '15432'){throw 'Refusing non-local database configuration'}
if($Build){& (Join-Path $repoRoot 'build.bat') "-Ddist.war=$repoRoot/dist/fpt-sale-cms.war" dist;if($LASTEXITCODE -ne 0){throw 'Build failed'}}
$pgBin='C:\Program Files\PostgreSQL\18\bin'
$pgData=Join-Path $localRoot 'postgres/data'
& "$pgBin/pg_ctl.exe" -D $pgData status *> $null
if($LASTEXITCODE -ne 0){& "$pgBin/pg_ctl.exe" -D $pgData -l (Join-Path $localRoot 'postgres/server.log') -w start;if($LASTEXITCODE -ne 0){throw 'Local PostgreSQL startup failed'}}
$base=Join-Path $localRoot 'tomcat'
$tomcatHome='D:\Subject FPT\PRJ301\apache-tomcat-9.0.118-windows-x64\apache-tomcat-9.0.118'
if(Test-Path (Join-Path $localRoot 'tomcat.pid')){
 $runningId=[int](Get-Content (Join-Path $localRoot 'tomcat.pid'))
 if(Get-Process -Id $runningId -ErrorAction SilentlyContinue){Write-Output 'Local Tomcat is already running';exit}
}
Copy-Item -LiteralPath (Join-Path $repoRoot 'dist/fpt-sale-cms.war') -Destination (Join-Path $base 'webapps/fpt-sale.war') -Force
$oldEnv=@{}
$names=@('DB_HOST','DB_PORT','DB_NAME','DB_USER','DB_PASSWORD','DB_SSLMODE','AI_ENCRYPTION_KEY','DEMO_ADMIN_ENABLED','RESEND_API_KEY')
try {
 foreach($name in $names){$oldEnv[$name]=[Environment]::GetEnvironmentVariable($name,'Process');[Environment]::SetEnvironmentVariable($name,[string]$config.$name,'Process')}
 $classpath="$tomcatHome/bin/bootstrap.jar;$tomcatHome/bin/tomcat-juli.jar"
 $args=@('-Dfile.encoding=UTF-8',('-Dcatalina.home="'+$tomcatHome+'"'),('-Dcatalina.base="'+$base+'"'),('-Djava.io.tmpdir="'+$base+'/temp"'),'-classpath',('"'+$classpath+'"'),'org.apache.catalina.startup.Bootstrap','start')
 $server=Start-Process -FilePath (Get-Command java).Source -ArgumentList $args -WindowStyle Hidden -WorkingDirectory $repoRoot -RedirectStandardOutput (Join-Path $base 'logs/stdout.log') -RedirectStandardError (Join-Path $base 'logs/stderr.log') -PassThru
 Set-Content -LiteralPath (Join-Path $localRoot 'tomcat.pid') -Value $server.Id
} finally {foreach($name in $names){[Environment]::SetEnvironmentVariable($name,$oldEnv[$name],'Process')}}
$ready=$false
for($attempt=0;$attempt -lt 30;$attempt++){
 if($server.HasExited){throw 'Local Tomcat exited; inspect .local/tomcat/logs'}
 try {$response=Invoke-WebRequest 'http://127.0.0.1:8085/fpt-sale/login' -UseBasicParsing -TimeoutSec 2;if($response.StatusCode -eq 200){$ready=$true;break}}catch{}
 Start-Sleep -Milliseconds 500
}
if(-not $ready){throw 'Local website did not become ready; inspect .local/tomcat/logs'}
Write-Output 'Local website: http://localhost:8085/fpt-sale/login'


