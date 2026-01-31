@echo off
REM Run Spacecraft Simulator

echo Starting Spacecraft Simulator...
echo Press Ctrl+C to stop
echo.

cd /d "%~dp0"

if exist "target\sle-demo-1.0.0-spacecraft.jar" (
    java -jar target\sle-demo-1.0.0-spacecraft.jar
) else (
    echo JAR file not found. Building...
    call mvn clean package
    java -jar target\sle-demo-1.0.0-spacecraft.jar
)

pause
