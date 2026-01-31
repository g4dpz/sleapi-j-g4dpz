@echo off
REM Run MOC Client

echo Starting MOC Client...
echo Press Ctrl+C to stop
echo.

cd /d "%~dp0"

if exist "target\sle-demo-1.0.0-moc.jar" (
    java -jar target\sle-demo-1.0.0-moc.jar
) else (
    echo JAR file not found. Building...
    call mvn clean package
    java -jar target\sle-demo-1.0.0-moc.jar
)

pause
