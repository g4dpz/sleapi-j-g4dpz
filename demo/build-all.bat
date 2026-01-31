@echo off
REM Build everything needed for the demo

echo ================================================================================
echo SLE Demo - Build Script
echo ================================================================================
echo.

REM Check Java version
echo Checking Java version...
java -version 2>&1 | findstr /C:"version"
echo.

REM Check Maven version
echo Checking Maven version...
mvn -version | findstr /C:"Apache Maven"
echo.

echo ================================================================================
echo Step 1: Building parent SLE Java API
echo ================================================================================
cd ..
call mvn clean install -DskipTests
if errorlevel 1 (
    echo Build failed!
    pause
    exit /b 1
)
echo.

echo ================================================================================
echo Step 2: Building demo
echo ================================================================================
cd demo
call mvn clean package
if errorlevel 1 (
    echo Build failed!
    pause
    exit /b 1
)
echo.

echo ================================================================================
echo Build Complete!
echo ================================================================================
echo.
echo Generated JAR files:
dir /B target\sle-demo-*.jar 2>nul
echo.
echo To run the demo:
echo   Terminal 1: run-groundstation.bat
echo   Terminal 2: run-spacecraft.bat
echo   Terminal 3: run-moc.bat
echo.
echo Or see QUICKSTART.md for detailed instructions
echo ================================================================================
echo.
pause
