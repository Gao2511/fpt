param(
    [string]$TomcatHome=$env:CATALINA_HOME,
    [string]$AntJar,
    [string]$WebRoot
)
$ErrorActionPreference='Stop'
$repoRoot=Split-Path -Parent $PSScriptRoot
if(!$TomcatHome){throw 'Specify -TomcatHome (Tomcat 9) or CATALINA_HOME'}
if(!$AntJar){if(!$env:ANT_HOME){throw 'Specify -AntJar or ANT_HOME'};$AntJar=Join-Path $env:ANT_HOME 'lib/ant.jar'}
if(!$WebRoot){$WebRoot=Join-Path $repoRoot '.build/web'}
$webRoot=$WebRoot
$output=Join-Path $env:TEMP ('fpt-cms-jsps-'+[guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $output | Out-Null
$jars=@(Get-ChildItem -LiteralPath "$TomcatHome/lib","$webRoot/WEB-INF/lib" -Filter '*.jar' | ForEach-Object FullName)
$classpath=($jars+"$TomcatHome/bin/tomcat-juli.jar"+$AntJar+"$webRoot/WEB-INF/classes") -join ';'
$pages=@(Get-ChildItem -LiteralPath (Join-Path $webRoot 'view') -Recurse -File -Filter '*.jsp' | Sort-Object FullName | ForEach-Object {
    [IO.Path]::GetRelativePath($webRoot,$_.FullName).Replace('\','/')
})
if($pages.Count-eq 0){throw 'No view JSPs found to compile'}
$diagnostics=& java -cp $classpath org.apache.jasper.JspC -uriroot $webRoot -d $output -compile -classpath $classpath @pages 2>&1
$code=$LASTEXITCODE
$diagnostics | ForEach-Object {Write-Output "$_"}
# Jasper's CLI may return exit 0 even when a page fails; require its diagnostic result.
if($code-ne 0 -or (($diagnostics -join "`n") -notmatch 'Generation completed with \[0\] errors')){throw 'CMS JSP compilation failed'}
Write-Output "PASS compiled $($pages.Count) CMS/public JSPs"
