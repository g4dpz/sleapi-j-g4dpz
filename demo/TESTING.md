# SLE Demo - Testing Guide

## Automated Testing

The demo includes comprehensive automated test scripts that build, run, and validate all components.

## Test Scripts

### Unix/Linux/macOS
```bash
./test-demo.sh
```

### Windows
```cmd
test-demo.bat
```

## What the Test Does

The automated test script performs the following steps:

### 1. **Prerequisites Check** ✓
- Verifies Java 17+ is installed
- Verifies Maven 3.8.5+ is installed
- Displays versions

### 2. **Build Parent SLE API** 🔨
- Cleans and builds the parent SLE Java API
- Skips tests for faster build
- Validates build success

### 3. **Build Demo** 🔨
- Cleans and builds the demo project
- Creates three JAR files:
  - `sle-demo-1.0.0-groundstation.jar`
  - `sle-demo-1.0.0-spacecraft.jar`
  - `sle-demo-1.0.0-moc.jar`
- Verifies all JARs exist

### 4. **Start Ground Station** 🚀
- Launches Ground Station in background
- Waits for startup (3 seconds)
- Verifies it's listening on ports 5555 and 5556
- Logs output to `test-logs/groundstation.log`

### 5. **Start Spacecraft** 🛰️
- Launches Spacecraft Simulator in background
- Waits for startup (3 seconds)
- Verifies connection to Ground Station
- Logs output to `test-logs/spacecraft.log`

### 6. **Start MOC** 🖥️
- Launches MOC Client in background
- Waits for startup (3 seconds)
- Verifies connection to Ground Station
- Logs output to `test-logs/moc.log`

### 7. **Monitor & Validate** 📊
- Runs for 30 seconds (configurable)
- Shows real-time progress bar
- Counts frames received
- Monitors for errors
- Collects statistics

### 8. **Analyze Results** 📈
- Counts frames at each stage
- Calculates success rate
- Checks for errors
- Validates data flow
- Generates report

### 9. **Cleanup** 🧹
- Stops all background processes
- Preserves log files
- Returns exit code (0 = success, 1 = failure)

## Test Output

### Successful Test Run

```
================================================================================
                    SLE DEMO - AUTOMATED TEST SCRIPT
================================================================================

This script will:
  1. Build the parent SLE API and demo
  2. Start all three components (Ground Station, Spacecraft, MOC)
  3. Run for 30 seconds
  4. Validate results
  5. Generate test report

Test Duration: 30 seconds
Expected Frames: At least 20
Log Directory: test-logs

================================================================================

[1/7] Checking prerequisites...

✓ Java version: java version "17.0.9"
✓ Maven version: Apache Maven 3.8.5

[2/7] Building parent SLE Java API...

✓ Parent SLE API built successfully

[3/7] Building demo...

✓ Demo built successfully

✓ JAR files created:
  target/sle-demo-1.0.0-groundstation.jar (1.2M)
  target/sle-demo-1.0.0-spacecraft.jar (1.2M)
  target/sle-demo-1.0.0-moc.jar (1.2M)

[4/7] Starting Ground Station...

Ground Station PID: 12345
✓ Ground Station started successfully

[5/7] Starting Spacecraft Simulator...

Spacecraft PID: 12346
✓ Spacecraft started and connected

[6/7] Starting MOC Client...

MOC PID: 12347
✓ MOC started and connected

[7/7] Running test for 30 seconds...

Monitoring telemetry flow...

[██████████████████████████████████████████████████] 100% | Frames: 28 | Time: 30/30s

================================================================================
                           TEST RESULTS
================================================================================

Frame Counts:
  Spacecraft Sent:        28
  Ground Station RX:      28
  Ground Station TX:      28
  MOC Received:           28

Error Counts:
  Spacecraft Errors:      0
  Ground Station Errors:  0
  MOC Errors:             0

Performance:
  Success Rate:           100%
  Frame Rate:             0.93 frames/sec
  Data Rate:              1.04 KB/sec

Sample Telemetry (last 3 frames):
  [MOC] Frame #26 | SCID=185 VCID=0 Count=26 | Temp=45.3°C Volt=28.5V Curr=3.2A Alt=450km Att=125° | Solar=✓ Ant=✓ Sys=OK
  [MOC] Frame #27 | SCID=185 VCID=0 Count=27 | Temp=46.1°C Volt=28.7V Curr=3.1A Alt=451km Att=126° | Solar=✓ Ant=✓ Sys=OK
  [MOC] Frame #28 | SCID=185 VCID=0 Count=28 | Temp=44.8°C Volt=28.3V Curr=3.3A Alt=449km Att=124° | Solar=✓ Ant=✓ Sys=OK

================================================================================
                           VALIDATION
================================================================================

✓ PASS: MOC received at least 20 frames (28)
✓ PASS: No errors detected
✓ PASS: Data flow is consistent
✓ PASS: Success rate is 100% (>= 95%)
✓ PASS: All processes still running

================================================================================
                           SUMMARY
================================================================================

✓ ALL TESTS PASSED (5/5)

The SLE Demo is working correctly!

Log files:
  Ground Station: test-logs/groundstation.log
  MOC:            test-logs/moc.log
  Spacecraft:     test-logs/spacecraft.log

================================================================================
```

## Test Validation Criteria

The test validates the following:

### Test 1: Minimum Frames ✓
- **Criteria**: MOC receives at least 20 frames in 30 seconds
- **Expected**: ~28-30 frames (1 Hz rate)
- **Pass**: ≥ 20 frames
- **Fail**: < 20 frames

### Test 2: No Errors ✓
- **Criteria**: No errors in any component logs
- **Expected**: 0 errors
- **Pass**: 0 errors
- **Fail**: > 0 errors

### Test 3: Data Flow Consistency ✓
- **Criteria**: Frames sent = frames received at each stage
- **Expected**: SC_SENT = GS_RX = GS_TX = MOC_RX
- **Pass**: All equal
- **Warn**: Minor discrepancies

### Test 4: Success Rate ✓
- **Criteria**: (MOC frames / Spacecraft frames) × 100
- **Expected**: 100%
- **Pass**: ≥ 95%
- **Fail**: < 95%

### Test 5: Process Stability ✓
- **Criteria**: All processes still running after test
- **Expected**: All 3 processes alive
- **Pass**: All running
- **Fail**: Any crashed

## Configuration

You can modify test parameters by editing the script:

### Unix/Linux/macOS (`test-demo.sh`)
```bash
TEST_DURATION=30  # Test duration in seconds
MIN_FRAMES=20     # Minimum expected frames
LOG_DIR="test-logs"  # Log directory
```

### Windows (`test-demo.bat`)
```batch
set TEST_DURATION=30
set MIN_FRAMES=20
set LOG_DIR=test-logs
```

## Log Files

After running the test, check the logs in `test-logs/`:

### groundstation.log
```
[GROUND STATION] Listening for spacecraft on port 5555
[GROUND STATION] SLE RAF service listening on port 5556
[GROUND STATION] Spacecraft connected: /127.0.0.1:xxxxx
[GROUND STATION] MOC connected: /127.0.0.1:xxxxx
[GROUND STATION] Received frame #1 from spacecraft (Queue: 1)
[GROUND STATION] Forwarded frame #1 to MOC (Queue: 0)
```

### spacecraft.log
```
[SPACECRAFT] Connected to Ground Station
[SPACECRAFT] Sent: TM Frame [SCID=185, VCID=0, Count=0, Size=1115 bytes, Time=...]
[SPACECRAFT] Sent: TM Frame [SCID=185, VCID=0, Count=1, Size=1115 bytes, Time=...]
```

### moc.log
```
[MOC] Connected to Ground Station
[MOC] Receiving telemetry frames...
[MOC] Frame #1 | SCID=185 VCID=0 Count=0 | Temp=45.3°C Volt=28.5V Curr=3.2A Alt=450km Att=125° | Solar=✓ Ant=✓ Sys=OK
```

## Troubleshooting

### Test Fails: "Java not found"
**Solution**: Install Java 17 or higher
```bash
java -version  # Check version
```

### Test Fails: "Maven not found"
**Solution**: Install Maven 3.8.5 or higher
```bash
mvn -version  # Check version
```

### Test Fails: "Ground Station failed to start"
**Solution**: 
- Check if ports 5555 and 5556 are available
- Check `test-logs/groundstation.log` for errors
- Try: `lsof -i :5555` (Unix) or `netstat -ano | findstr :5555` (Windows)

### Test Fails: "Spacecraft not connected"
**Solution**:
- Ensure Ground Station started successfully
- Check `test-logs/spacecraft.log` for connection errors
- Verify Ground Station is listening on port 5555

### Test Fails: "MOC not connected"
**Solution**:
- Ensure Ground Station started successfully
- Check `test-logs/moc.log` for connection errors
- Verify Ground Station is listening on port 5556

### Test Fails: "Insufficient frames"
**Solution**:
- Increase `TEST_DURATION` to allow more frames
- Check if processes crashed (look for errors in logs)
- Verify network connectivity (localhost)

### Test Hangs
**Solution**:
- Press Ctrl+C to stop
- Manually kill processes:
  - Unix: `pkill -f "sle-demo"`
  - Windows: `taskkill /F /FI "WINDOWTITLE eq java*sle-demo*"`

## Manual Testing

If you prefer manual testing:

### Step 1: Build
```bash
cd demo
./build-all.sh  # or build-all.bat on Windows
```

### Step 2: Run (3 terminals)
```bash
# Terminal 1
./run-groundstation.sh

# Terminal 2
./run-spacecraft.sh

# Terminal 3
./run-moc.sh
```

### Step 3: Observe
- Watch frames flowing in MOC terminal
- Check statistics every 10 frames
- Press Ctrl+C to stop

### Step 4: Validate
- Did MOC receive frames?
- Were there any errors?
- Did all components stay running?

## Continuous Integration

The test script is CI/CD ready:

### GitHub Actions Example
```yaml
name: SLE Demo Test

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run Demo Test
        run: |
          cd demo
          chmod +x test-demo.sh
          ./test-demo.sh
      - name: Upload Logs
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: test-logs
          path: demo/test-logs/
```

## Performance Benchmarks

Expected performance on modern hardware:

| Metric | Value |
|--------|-------|
| Build Time | 30-60 seconds |
| Startup Time | 5-10 seconds |
| Frame Rate | 0.9-1.0 Hz |
| Success Rate | 95-100% |
| CPU Usage | < 5% total |
| Memory Usage | ~30 MB total |

## Exit Codes

The test script returns:
- **0**: All tests passed
- **1**: One or more tests failed

Use in scripts:
```bash
./test-demo.sh
if [ $? -eq 0 ]; then
    echo "Success!"
else
    echo "Failed!"
fi
```

## Best Practices

1. **Run test before committing** - Ensure changes don't break functionality
2. **Check logs on failure** - Logs contain detailed error information
3. **Increase test duration for stability** - Longer tests catch intermittent issues
4. **Clean environment** - Close other applications using ports 5555/5556
5. **Monitor resources** - Ensure sufficient CPU and memory available

## Summary

The automated test script provides:
- ✅ Complete build validation
- ✅ Automated component startup
- ✅ Real-time monitoring
- ✅ Comprehensive validation
- ✅ Detailed reporting
- ✅ Easy troubleshooting
- ✅ CI/CD ready

Run it before every release to ensure quality! 🚀
