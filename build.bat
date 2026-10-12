@echo off
setlocal
set "FPT_ANT_BAT="

rem Prefer explicit configuration, then PATH, then the existing local NetBeans installation.
if defined ANT_HOME if exist "%ANT_HOME%\bin\ant.bat" set "FPT_ANT_BAT=%ANT_HOME%\bin\ant.bat"
if not defined FPT_ANT_BAT for /f "delims=" %%A in ('where ant.bat 2^>nul') do if not defined FPT_ANT_BAT set "FPT_ANT_BAT=%%A"
if not defined FPT_ANT_BAT if exist "D:\Subject FPT\PRJ301\NetBeans-13\netbeans\extide\ant\bin\ant.bat" set "FPT_ANT_BAT=D:\Subject FPT\PRJ301\NetBeans-13\netbeans\extide\ant\bin\ant.bat"

if not defined FPT_ANT_BAT (
    echo [ERROR] Apache Ant was not found. Set ANT_HOME to the Ant directory or add its bin directory to PATH.
    exit /b 1
)

call "%FPT_ANT_BAT%" %*
exit /b %errorlevel%
