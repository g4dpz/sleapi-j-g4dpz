@echo off
REM Automated test script for SLE Demo (Windows)
REM Builds, runs, and validates all components

setlocal enabledelayedexpansion

REM Configuration
set TEST_DURATION=30
set MIN_FRAMES=20
set LOG_DIR=test-logs

echo ================================================================================
echo                    SLE DEMO - AUTOMATED TEST SCRIPT
echo ================================================================================
echo.
echo This script will:
echo   1. Build the parent SLE API and demo
echo   2. Start all three components (Ground Station, Spacecraft, MOC)
echo   3. Run for %TEST_DURATION% seconds
echo   4. Validate results
echo   5. Generate test report
echo.
echo Test Duration: %TEST_DURATION% seconds
echo Expected Frames: At least %MIN_FRAMES%
echo Log Directory: %LOG_DIR%
echo.
echo ================================================================================
echo.

REM Create log directory
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"
del /Q "%LOG_DIR%\*.log" 2>nul

REM Step 1: Check prerequisites
echo [1/7] Checking prerequisites...
echo.

where java >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java not found
    pause
    exit /b 1
)

where mvn >nul 2>&1
if errorlevel 1 (
    echo ERROR: Maven not found
    pause
    exit /b 1
)

java -version 2>&1 | findstr /C:"version"
mvn -version 2>&1 | findstr /C:"Apache Maven"
echo.

REM Step 2: Build parent SLE API
echo [2/7] Building parent SLE Java API...
echo.

cd ..
call mvn clean install -DskipTests -q
if errorlevel 1 (
    echo ERROR: Failed to build parent SLE API
    pause
    exit /b 1
)
echo SUCCESS: Parent SLE API built successfully
echo.

REM Step 3: Build demo
echo [3/7] Building demo...
echo.

cd demo
call mvn clean package -q
if errorlevel 1 (
    echo ERROR: Failed to build demo
    pause
    exit /b 1
)
echo SUCCESS: Demo built successfully
echo.

REM Verify JAR files
if not exist "target\sle-demo-1.0.0-groundstation.jar" (
    echo ERROR: Ground Station JAR not found
    pause
    exit /b 1
)
if not exist "target\sle-demo-1.0.0-spacecraft.jar" (
    echo ERROR: Spacecraft JAR not found
    pause
    exit /b 1
)
if not exist "target\sle-demo-1.0.0-moc.jar" (
    echo ERROR: MOC JAR not found
    pause
    exit /b 1
)

echo JAR files created:
dir /B target\sle-demo-*.jar
echo.

REM Step 4: Start Ground Station
echo [4/7] Starting Ground Station...
echo.

start /B java -jar target\sle-demo-1.0.0-groundstation.jar > "%LOG_DIR%\groundstation.log" 2>&1
timeout /t 3 /nobreak >nul

REM Check if Ground Station started
findstr /C:"Listening for spacecraft" "%LOG_DIR%\groundstation.log" >nul
if errorlevel 1 (
    echo ERROR: Ground Station failed to start
    type "%LOG_DIR%\groundstation.log"
    goto cleanup
)
echo SUCCESS: Ground Station started
echo.

REM Step 5: Start Spacecraft
echo [5/7] Starting Spacecraft Simulator...
echo.

start /B java -jar target\sle-demo-1.0.0-spacecraft.jar > "%LOG_DIR%\spacecraft.log" 2>&1
timeout /t 3 /nobreak >nul

REM Check if Spacecraft connected
findstr /C:"Connected to Ground Station" "%LOG_DIR%\spacecraft.log" >nul
if errorlevel 1 (
    echo ERROR: Spacecraft failed to connect
    type "%LOG_DIR%\spacecraft.log"
    goto cleanup
)
echo SUCCESS: Spacecraft started and connected
echo.

REM Step 6: Start MOC
echo [6/7] Starting MOC Client...
echo.

start /B java -jar target\sle-demo-1.0.0-moc.jar > "%LOG_DIR%\moc.log" 2>&1
timeout /t 3 /nobreak >nul

REM Check if MOC connected
findstr /C:"Connected to Ground Station" "%LOG_DIR%\moc.log" >nul
if errorlevel 1 (
    echo ERROR: MOC failed to connect
    type "%LOG_DIR%\moc.log"
    goto cleanup
)
echo SUCCESS: MOC started and connected
echo.

REM Step 7: Monitor
echo [7/7] Running test for %TEST_DURATION% seconds...
echo.
echo Monitoring telemetry flow...
echo.

REM Wait for test duration
for /L %%i in (1,1,%TEST_DURATION%) do (
    REM Count frames
    for /f %%a in ('findstr /C:"Frame #" "%LOG_DIR%\moc.log" 2^>nul ^| find /C /V ""') do set FRAMES=%%a
    if not defined FRAMES set FRAMES=0
    
    echo Time: %%i/%TEST_DURATION%s ^| Frames: !FRAMES!
    timeout /t 1 /nobreak >nul
)

echo.
echo.

REM Analyze results
echo ================================================================================
echo                           TEST RESULTS
echo ================================================================================
echo.

REM Count frames
for /f %%a in ('findstr /C:"Sent: TM Frame" "%LOG_DIR%\spacecraft.log" 2^>nul ^| find /C /V ""') do set SC_FRAMES=%%a
for /f %%a in ('findstr /C:"Received frame #" "%LOG_DIR%\groundstation.log" 2^>nul ^| find /C /V ""') do set GS_RX_FRAMES=%%a
for /f %%a in ('findstr /C:"Forwarded frame #" "%LOG_DIR%\groundstation.log" 2^>nul ^| find /C /V ""') do set GS_TX_FRAMES=%%a
for /f %%a in ('findstr /C:"Frame #" "%LOG_DIR%\moc.log" 2^>nul ^| find /C /V ""') do set MOC_FRAMES=%%a

if not defined SC_FRAMES set SC_FRAMES=0
if not defined GS_RX_FRAMES set GS_RX_FRAMES=0
if not defined GS_TX_FRAMES set GS_TX_FRAMES=0
if not defined MOC_FRAMES set MOC_FRAMES=0

echo Frame Counts:
echo   Spacecraft Sent:        %SC_FRAMES%
echo   Ground Station RX:      %GS_RX_FRAMES%
echo   Ground Station TX:      %GS_TX_FRAMES%
echo   MOC Received:           %MOC_FRAMES%
echo.

REM Check for errors
for /f %%a in ('findstr /I /C:"error" /C:"exception" "%LOG_DIR%\spacecraft.log" 2^>nul ^| find /C /V ""') do set SC_ERRORS=%%a
for /f %%a in ('findstr /I /C:"error" /C:"exception" "%LOG_DIR%\groundstation.log" 2^>nul ^| find /C /V ""') do set GS_ERRORS=%%a
for /f %%a in ('findstr /I /C:"error" /C:"exception" "%LOG_DIR%\moc.log" 2^>nul ^| find /C /V ""') do set MOC_ERRORS=%%a

if not defined SC_ERRORS set SC_ERRORS=0
if not defined GS_ERRORS set GS_ERRORS=0
if not defined MOC_ERRORS set MOC_ERRORS=0

echo Error Counts:
echo   Spacecraft Errors:      %SC_ERRORS%
echo   Ground Station Errors:  %GS_ERRORS%
echo   MOC Errors:             %MOC_ERRORS%
echo.

REM Calculate success rate
if %SC_FRAMES% GTR 0 (
    set /a SUCCESS_RATE=%MOC_FRAMES% * 100 / %SC_FRAMES%
) else (
    set SUCCESS_RATE=0
)

echo Performance:
echo   Success Rate:           %SUCCESS_RATE%%%
echo.

REM Sample telemetry
echo Sample Telemetry (last 3 frames):
powershell -Command "Get-Content '%LOG_DIR%\moc.log' | Select-String 'Frame #' | Select-Object -Last 3"
echo.

REM Validation
echo ================================================================================
echo                           VALIDATION
echo ================================================================================
echo.

set PASSED=0
set FAILED=0

REM Test 1: Minimum frames
if %MOC_FRAMES% GEQ %MIN_FRAMES% (
    echo [PASS] MOC received at least %MIN_FRAMES% frames ^(%MOC_FRAMES%^)
    set /a PASSED+=1
) else (
    echo [FAIL] MOC received only %MOC_FRAMES% frames ^(expected at least %MIN_FRAMES%^)
    set /a FAILED+=1
)

REM Test 2: No errors
set /a TOTAL_ERRORS=%SC_ERRORS% + %GS_ERRORS% + %MOC_ERRORS%
if %TOTAL_ERRORS% EQU 0 (
    echo [PASS] No errors detected
    set /a PASSED+=1
) else (
    echo [FAIL] %TOTAL_ERRORS% errors detected
    set /a FAILED+=1
)

REM Test 3: Success rate
if %SUCCESS_RATE% GEQ 95 (
    echo [PASS] Success rate is %SUCCESS_RATE%%% ^(^>= 95%%^)
    set /a PASSED+=1
) else (
    echo [FAIL] Success rate is %SUCCESS_RATE%%% ^(expected ^>= 95%%^)
    set /a FAILED+=1
)

echo.
echo ================================================================================
echo                           SUMMARY
echo ================================================================================
echo.

set /a TOTAL_TESTS=%PASSED% + %FAILED%
if %FAILED% EQU 0 (
    echo [SUCCESS] ALL TESTS PASSED ^(%PASSED%/%TOTAL_TESTS%^)
    echo.
    echo The SLE Demo is working correctly!
    set EXIT_CODE=0
) else (
    echo [FAILURE] SOME TESTS FAILED ^(%FAILED%/%TOTAL_TESTS% failed, %PASSED% passed^)
    echo.
    echo Please check the logs in %LOG_DIR%\ for details
    set EXIT_CODE=1
)

echo.
echo Log files:
echo   Ground Station: %LOG_DIR%\groundstation.log
echo   Spacecraft:     %LOG_DIR%\spacecraft.log
echo   MOC:            %LOG_DIR%\moc.log
echo.
echo ================================================================================

:cleanup
echo.
echo Cleaning up...
taskkill /F /FI "WINDOWTITLE eq java*sle-demo*" >nul 2>&1
timeout /t 2 /nobreak >nul
echo Cleanup complete
echo.

pause
exit /b %EXIT_CODE%
