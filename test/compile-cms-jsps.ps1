param(
    [string]$TomcatHome='D:/Subject FPT/PRJ301/apache-tomcat-9.0.118-windows-x64/apache-tomcat-9.0.118',
    [string]$AntJar='D:/Subject FPT/PRJ301/NetBeans-13/netbeans/extide/ant/lib/ant.jar'
)
$ErrorActionPreference='Stop'
$repoRoot=Split-Path -Parent $PSScriptRoot
$webRoot=Join-Path $repoRoot 'build/web'
$output=Join-Path $env:TEMP ('fpt-cms-jsps-'+[guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $output | Out-Null
$jars=@(Get-ChildItem -LiteralPath "$TomcatHome/lib","$webRoot/WEB-INF/lib" -Filter '*.jar' | ForEach-Object FullName)
$classpath=($jars+"$TomcatHome/bin/tomcat-juli.jar"+$AntJar+"$webRoot/WEB-INF/classes") -join ';'
$pages=@('view/admin/content.jsp','view/admin/settings.jsp','view/admin/dashboard.jsp','view/admin/customer-list.jsp','view/admin/customer-detail.jsp','view/admin/customer-edit.jsp','view/admin/package-list.jsp','view/admin/package-form.jsp','view/admin/user-list.jsp','view/admin/user-change-password.jsp','view/admin/email-log.jsp','view/admin/admin-header.jsp','view/home.jsp','view/contact.jsp','view/customer/package-detail.jsp','view/customer/customer-header.jsp','view/customer/profile.jsp')
$diagnostics=& java -cp $classpath org.apache.jasper.JspC -uriroot $webRoot -d $output -compile -classpath $classpath @pages 2>&1
$code=$LASTEXITCODE
$diagnostics | ForEach-Object {Write-Output "$_"}
# Jasper's CLI may return exit 0 even when a page fails; require its diagnostic result.
if($code-ne 0 -or (($diagnostics -join "`n") -notmatch 'Generation completed with \[0\] errors')){throw 'CMS JSP compilation failed'}
Write-Output "PASS compiled $($pages.Count) CMS/public JSPs"
