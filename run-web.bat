@echo off
chcp 65001 > nul
echo ===================================================
echo   [FPT SALE] DANG KHOI DONG SERVER TOMCAT CHO WEB...
echo ===================================================

set "ANT_BAT=D:\Subject FPT\PRJ301\NetBeans-13\netbeans\extide\ant\bin\ant.bat"
set "TOMCAT_DIR=D:\Subject FPT\PRJ301\apache-tomcat-9.0.118-windows-x64\apache-tomcat-9.0.118"
set "CATALINA_HOME=%TOMCAT_DIR%"

echo 1. Dang build WAR (Ant dist)...
call "%ANT_BAT%" dist
if errorlevel 1 (
    echo [LOI] Build WAR that bai!
    pause
    exit /b %errorlevel%
)

echo 2. Dang deploy dist/fpt-sale.war vao Tomcat webapps...
copy /Y "dist\fpt-sale.war" "%TOMCAT_DIR%\webapps\fpt-sale.war" > nul

echo 3. Dang khoi dong Tomcat 9 (Port 8084)...
start "" "%TOMCAT_DIR%\bin\startup.bat"

echo 4. Dang mo trinh duyet http://localhost:8084/fpt-sale/home ...
powershell -nop -c "Start-Sleep -Seconds 3"
start http://localhost:8084/fpt-sale/home

echo ===================================================
echo   [THANH CONG] Website dang chay tai:
echo   http://localhost:8084/fpt-sale/home
echo   (De dung server, hay chay: .\stop-web.bat)
echo ===================================================
