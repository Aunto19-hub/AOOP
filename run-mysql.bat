@echo off
setlocal
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
set "PATH=%USERPROFILE%\tools\apache-maven-3.9.16\bin;%JAVA_HOME%\bin;%PATH%"
cd /d "%~dp0"

echo ============================================================
echo  Seamline (MySQL)
echo ============================================================
echo Make sure XAMPP's MySQL module is started first
echo (XAMPP Control Panel -^> Start next to MySQL).
echo.

powershell -NoProfile -Command "if (-not (Test-NetConnection -ComputerName localhost -Port 3306 -InformationLevel Quiet -WarningAction SilentlyContinue)) { Write-Host 'MySQL is not reachable on localhost:3306 -- start it in XAMPP first.' -ForegroundColor Yellow }"

start "" cmd /c "timeout /t 15 >nul && start http://localhost:8080"

echo Starting Spring Boot... this window stays open while the app runs.
echo Press Ctrl+C to stop it.
echo.
mvn spring-boot:run -Dspring-boot.run.profiles=mysql

pause
