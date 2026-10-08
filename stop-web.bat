@echo off
chcp 65001 > nul
echo Dang dung Tomcat server...
set "TOMCAT_DIR=D:\Subject FPT\PRJ301\apache-tomcat-9.0.118-windows-x64\apache-tomcat-9.0.118"
set "CATALINA_HOME=%TOMCAT_DIR%"
call "%TOMCAT_DIR%\bin\shutdown.bat"
powershell -nop -c "Get-Process -Name java -ErrorAction SilentlyContinue | Where-Object { $_.Path -like '*Tomcat*' -or $_.Path -like '*Subject FPT*' } | Stop-Process -Force"
echo Da dung server Tomcat hoan toan.
