# Automated Test Script - Summary

## ✅ Complete Testing Solution

I've created comprehensive automated test scripts that build, run, and validate the entire SLE Demo system.

## 📁 Files Created

1. **test-demo.sh** - Unix/Linux/macOS test script
2. **test-demo.bat** - Windows test script  
3. **TESTING.md** - Complete testing documentation

## 🎯 What the Test Does

### Automated Workflow

```
1. Check Prerequisites (Java 17+, Maven 3.8.5+)
   ↓
2. Build Parent SLE API
   ↓
3. Build Demo Project
   ↓
4. Start Ground Station (background)
   ↓
5. Start Spacecraft (background)
   ↓
6. Start MOC (background)
   ↓
7. Monitor for 30 seconds
   ↓
8. Collect Statistics
   ↓
9. Validate Results
   ↓
10. Generate Report
   ↓
11. Cleanup & Exit
```

## 📊 Test Validation

The script validates 5 key criteria:

### ✓ Test 1: Minimum Frames
- MOC receives ≥ 20 frames in 30 seconds
- Expected: ~28-30 frames (1 Hz rate)

### ✓ Test 2: No Errors
- Zero errors in all component logs
- Checks for exceptions and error messages

### ✓ Test 3: Data Flow Consistency
- Frames sent = frames received at each stage
- Validates: Spacecraft → Ground Station → MOC

### ✓ Test 4: Success Rate
- (MOC frames / Spacecraft frames) × 100
- Must be ≥ 95%

### ✓ Test 5: Process Stability
- All processes still running after test
- No crashes or unexpected terminations

## 🚀 How to Use

### Quick Test
```bash
cd demo
./test-demo.sh    # Unix/Linux/macOS
# OR
test-demo.bat     # Windows
```

### Expected Output
```
================================================================================
                    SLE DEMO - AUTOMATED TEST SCRIPT
================================================================================

[1/7] Checking prerequisites...
✓ Java version: java version "17.0.9"
✓ Maven version: Apache Maven 3.8.5

[2/7] Building parent SLE Java API...
✓ Parent SLE API built successfully

[3/7] Building demo...
✓ Demo built successfully

[4/7] Starting Ground Station...
✓ Ground Station started successfully

[5/7] Starting Spacecraft Simulator...
✓ Spacecraft started and connected

[6/7] Starting MOC Client...
✓ MOC started and connected

[7/7] Running test for 30 seconds...
[██████████████████████████████████████████████████] 100% | Frames: 28

================================================================================
                           TEST RESULTS
================================================================================

Frame Counts:
  Spacecraft Sent:        28
  Ground Station RX:      28
  Ground Station TX:      28
  MOC Received:           28

Performance:
  Success Rate:           100%
  Frame Rate:             0.93 frames/sec

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
```

## 📝 Log Files

All output is captured in `test-logs/`:

- **groundstation.log** - Ground Station output
- **spacecraft.log** - Spacecraft Simulator output
- **moc.log** - MOC Client output

## 🎨 Features

### Real-time Progress
- Live progress bar (Unix/Linux/macOS)
- Frame count updates
- Time remaining display

### Comprehensive Reporting
- Frame counts at each stage
- Error detection
- Success rate calculation
- Performance metrics
- Sample telemetry display

### Automatic Cleanup
- Stops all background processes
- Preserves log files for analysis
- Returns proper exit codes

### CI/CD Ready
- Exit code 0 = success
- Exit code 1 = failure
- Structured output
- Log file artifacts

## 🔧 Configuration

Edit the script to customize:

```bash
TEST_DURATION=30  # Test duration in seconds
MIN_FRAMES=20     # Minimum expected frames
LOG_DIR="test-logs"  # Log directory
```

## 🎓 Use Cases

### 1. Development Testing
Run after code changes to ensure nothing broke:
```bash
./test-demo.sh
```

### 2. Pre-Commit Validation
Add to git pre-commit hook:
```bash
#!/bin/bash
cd demo && ./test-demo.sh
```

### 3. CI/CD Pipeline
Integrate with GitHub Actions, Jenkins, etc.:
```yaml
- name: Test SLE Demo
  run: cd demo && ./test-demo.sh
```

### 4. Regression Testing
Run regularly to catch regressions:
```bash
# Cron job: Run daily at 2 AM
0 2 * * * cd /path/to/demo && ./test-demo.sh
```

### 5. Performance Benchmarking
Increase duration for performance testing:
```bash
# Edit script: TEST_DURATION=300 (5 minutes)
./test-demo.sh
```

## 🐛 Troubleshooting

### Test Fails
1. Check log files in `test-logs/`
2. Look for error messages
3. Verify prerequisites (Java 17+, Maven 3.8.5+)
4. Ensure ports 5555 and 5556 are available

### Processes Don't Stop
```bash
# Unix/Linux/macOS
pkill -f "sle-demo"

# Windows
taskkill /F /FI "WINDOWTITLE eq java*sle-demo*"
```

### Ports Already in Use
```bash
# Unix/Linux/macOS
lsof -i :5555
lsof -i :5556

# Windows
netstat -ano | findstr :5555
netstat -ano | findstr :5556
```

## 📈 Performance Metrics

Expected on modern hardware:

| Metric | Value |
|--------|-------|
| Total Test Time | 60-90 seconds |
| Build Time | 30-60 seconds |
| Test Duration | 30 seconds |
| Frames Expected | 28-30 |
| Success Rate | 95-100% |
| CPU Usage | < 5% |
| Memory Usage | ~30 MB |

## ✨ Benefits

### For Developers
- ✅ Quick validation after changes
- ✅ Automated testing workflow
- ✅ Detailed error reporting
- ✅ No manual setup required

### For QA
- ✅ Repeatable test procedure
- ✅ Consistent validation criteria
- ✅ Comprehensive reporting
- ✅ Log file preservation

### For CI/CD
- ✅ Scriptable execution
- ✅ Proper exit codes
- ✅ Artifact generation
- ✅ Fast execution

### For Users
- ✅ Easy to run
- ✅ Clear output
- ✅ Self-contained
- ✅ Cross-platform

## 🎉 Summary

The automated test script provides:

- **Complete automation** - Build, run, validate, report
- **Comprehensive validation** - 5 test criteria
- **Real-time monitoring** - Progress bar and live updates
- **Detailed reporting** - Statistics and sample data
- **Log preservation** - All output captured
- **Automatic cleanup** - No manual intervention needed
- **CI/CD ready** - Proper exit codes and artifacts
- **Cross-platform** - Unix and Windows versions

## 🚀 Next Steps

1. **Run the test**: `./test-demo.sh`
2. **Check results**: Look for "ALL TESTS PASSED"
3. **Review logs**: Check `test-logs/` if needed
4. **Integrate**: Add to your workflow

The test script ensures your SLE Demo is working correctly every time! ✅
