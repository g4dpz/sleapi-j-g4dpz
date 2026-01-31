@echo off
REM Run Ground Station Server

echo Starting Ground Station Server...
echo Press Ctrl+C to stop
echo.

cd /d "%~dp0"

if exist "target\sle-demo-1.0.0-groundstation.jar" (
    java -jar target\sle-demo-1.0.0-groundstation.jar
) else (
    echo JAR file not found. Building...
    call mvn clean package
    java -jar target\sle-demo-1.0.0-groundstation.jar
)

pause
