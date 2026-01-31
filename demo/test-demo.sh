#!/bin/bash
# Automated test script for SLE Demo
# Builds, runs, and validates all components

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
TEST_DURATION=30  # Run test for 30 seconds
MIN_FRAMES=20     # Minimum frames expected in 30 seconds
LOG_DIR="test-logs"

# Cleanup function
cleanup() {
    echo ""
    echo -e "${YELLOW}Cleaning up...${NC}"
    
    # Kill all background processes
    if [ ! -z "$GS_PID" ]; then
        kill $GS_PID 2>/dev/null || true
    fi
    if [ ! -z "$SC_PID" ]; then
        kill $SC_PID 2>/dev/null || true
    fi
    if [ ! -z "$MOC_PID" ]; then
        kill $MOC_PID 2>/dev/null || true
    fi
    
    # Wait a bit for processes to terminate
    sleep 2
    
    # Force kill if still running
    pkill -f "sle-demo.*groundstation" 2>/dev/null || true
    pkill -f "sle-demo.*spacecraft" 2>/dev/null || true
    pkill -f "sle-demo.*moc" 2>/dev/null || true
    
    echo -e "${GREEN}Cleanup complete${NC}"
}

# Set trap to cleanup on exit
trap cleanup EXIT INT TERM

# Print header
echo "================================================================================"
echo "                    SLE DEMO - AUTOMATED TEST SCRIPT"
echo "================================================================================"
echo ""
echo "This script will:"
echo "  1. Build the parent SLE API and demo"
echo "  2. Start all three components (Ground Station, Spacecraft, MOC)"
echo "  3. Run for ${TEST_DURATION} seconds"
echo "  4. Validate results"
echo "  5. Generate test report"
echo ""
echo "Test Duration: ${TEST_DURATION} seconds"
echo "Expected Frames: At least ${MIN_FRAMES}"
echo "Log Directory: ${LOG_DIR}"
echo ""
echo "================================================================================"
echo ""

# Create log directory
mkdir -p "$LOG_DIR"
rm -f "$LOG_DIR"/*.log

# Step 1: Check prerequisites
echo -e "${BLUE}[1/7] Checking prerequisites...${NC}"
echo ""

if ! command -v java &> /dev/null; then
    echo -e "${RED}ERROR: Java not found${NC}"
    exit 1
fi

if ! command -v mvn &> /dev/null; then
    echo -e "${RED}ERROR: Maven not found${NC}"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo -e "${RED}ERROR: Java 17 or higher required (found: $JAVA_VERSION)${NC}"
    exit 1
fi

echo "✓ Java version: $(java -version 2>&1 | head -1)"
echo "✓ Maven version: $(mvn -version | head -1)"
echo ""

# Step 2: Build parent SLE API
echo -e "${BLUE}[2/7] Building parent SLE Java API...${NC}"
echo ""

cd ..
if mvn clean install -DskipTests -q; then
    echo -e "${GREEN}✓ Parent SLE API built successfully${NC}"
else
    echo -e "${RED}✗ Failed to build parent SLE API${NC}"
    exit 1
fi
echo ""

# Step 3: Build demo
echo -e "${BLUE}[3/7] Building demo...${NC}"
echo ""

cd demo
if mvn clean package -q; then
    echo -e "${GREEN}✓ Demo built successfully${NC}"
else
    echo -e "${RED}✗ Failed to build demo${NC}"
    exit 1
fi
echo ""

# Verify JAR files exist
if [ ! -f "target/sle-demo-1.0.0-groundstation.jar" ] || \
   [ ! -f "target/sle-demo-1.0.0-spacecraft.jar" ] || \
   [ ! -f "target/sle-demo-1.0.0-moc.jar" ]; then
    echo -e "${RED}✗ JAR files not found${NC}"
    exit 1
fi

echo "✓ JAR files created:"
ls -lh target/sle-demo-*.jar | awk '{print "  " $9 " (" $5 ")"}'
echo ""

# Step 4: Start Ground Station
echo -e "${BLUE}[4/7] Starting Ground Station...${NC}"
echo ""

java -jar target/sle-demo-1.0.0-groundstation.jar > "$LOG_DIR/groundstation.log" 2>&1 &
GS_PID=$!

echo "Ground Station PID: $GS_PID"
sleep 3

# Check if Ground Station started
if ! ps -p $GS_PID > /dev/null; then
    echo -e "${RED}✗ Ground Station failed to start${NC}"
    cat "$LOG_DIR/groundstation.log"
    exit 1
fi

# Verify Ground Station is listening
if grep -q "Listening for spacecraft" "$LOG_DIR/groundstation.log" && \
   grep -q "SLE RAF service listening" "$LOG_DIR/groundstation.log"; then
    echo -e "${GREEN}✓ Ground Station started successfully${NC}"
else
    echo -e "${RED}✗ Ground Station not ready${NC}"
    cat "$LOG_DIR/groundstation.log"
    exit 1
fi
echo ""

# Step 5: Start Spacecraft
echo -e "${BLUE}[5/7] Starting Spacecraft Simulator...${NC}"
echo ""

java -jar target/sle-demo-1.0.0-spacecraft.jar > "$LOG_DIR/spacecraft.log" 2>&1 &
SC_PID=$!

echo "Spacecraft PID: $SC_PID"
sleep 3

# Check if Spacecraft started and connected
if ! ps -p $SC_PID > /dev/null; then
    echo -e "${RED}✗ Spacecraft failed to start${NC}"
    cat "$LOG_DIR/spacecraft.log"
    exit 1
fi

if grep -q "Connected to Ground Station" "$LOG_DIR/spacecraft.log"; then
    echo -e "${GREEN}✓ Spacecraft started and connected${NC}"
else
    echo -e "${RED}✗ Spacecraft not connected${NC}"
    cat "$LOG_DIR/spacecraft.log"
    exit 1
fi
echo ""

# Step 6: Start MOC
echo -e "${BLUE}[6/7] Starting MOC Client...${NC}"
echo ""

java -jar target/sle-demo-1.0.0-moc.jar > "$LOG_DIR/moc.log" 2>&1 &
MOC_PID=$!

echo "MOC PID: $MOC_PID"
sleep 3

# Check if MOC started and connected
if ! ps -p $MOC_PID > /dev/null; then
    echo -e "${RED}✗ MOC failed to start${NC}"
    cat "$LOG_DIR/moc.log"
    exit 1
fi

if grep -q "Connected to Ground Station" "$LOG_DIR/moc.log"; then
    echo -e "${GREEN}✓ MOC started and connected${NC}"
else
    echo -e "${RED}✗ MOC not connected${NC}"
    cat "$LOG_DIR/moc.log"
    exit 1
fi
echo ""

# Step 7: Monitor and validate
echo -e "${BLUE}[7/7] Running test for ${TEST_DURATION} seconds...${NC}"
echo ""
echo "Monitoring telemetry flow..."
echo ""

# Show live progress
for i in $(seq 1 $TEST_DURATION); do
    # Count frames received by MOC
    FRAMES=$(grep -c "Frame #" "$LOG_DIR/moc.log" 2>/dev/null || echo "0")
    
    # Show progress bar
    PROGRESS=$((i * 100 / TEST_DURATION))
    BAR_LENGTH=50
    FILLED=$((PROGRESS * BAR_LENGTH / 100))
    BAR=$(printf "%${FILLED}s" | tr ' ' '█')
    EMPTY=$(printf "%$((BAR_LENGTH - FILLED))s" | tr ' ' '░')
    
    printf "\r[${BAR}${EMPTY}] ${PROGRESS}%% | Frames: ${FRAMES} | Time: ${i}/${TEST_DURATION}s"
    
    sleep 1
done

echo ""
echo ""

# Analyze results
echo "================================================================================"
echo "                           TEST RESULTS"
echo "================================================================================"
echo ""

# Count frames
SC_FRAMES=$(grep -c "Sent: TM Frame" "$LOG_DIR/spacecraft.log" 2>/dev/null || echo "0")
GS_RX_FRAMES=$(grep -c "Received frame #" "$LOG_DIR/groundstation.log" 2>/dev/null || echo "0")
GS_TX_FRAMES=$(grep -c "Forwarded frame #" "$LOG_DIR/groundstation.log" 2>/dev/null || echo "0")
MOC_FRAMES=$(grep -c "Frame #" "$LOG_DIR/moc.log" 2>/dev/null || echo "0")

# Extract and display CCSDS frame structure from first frame
echo "CCSDS Frame Structure (Frame #1):"
echo ""
sed -n '/^CCSDS TM FRAME #1:/,/^=.*=$/p' "$LOG_DIR/moc.log" 2>/dev/null || echo "  (Frame structure not captured)"
echo ""

echo "Frame Counts:"
echo "  Spacecraft Sent:        $SC_FRAMES"
echo "  Ground Station RX:      $GS_RX_FRAMES"
echo "  Ground Station TX:      $GS_TX_FRAMES"
echo "  MOC Received:           $MOC_FRAMES"
echo ""

# Check for errors (exclude false positives like "Frame Error Control")
SC_ERRORS=$(grep -i "error\|exception" "$LOG_DIR/spacecraft.log" 2>/dev/null | grep -v "Frame Error Control" | grep -c "." || true)
GS_ERRORS=$(grep -i "error\|exception" "$LOG_DIR/groundstation.log" 2>/dev/null | grep -v "Frame Error Control" | grep -c "." || true)
MOC_ERRORS=$(grep -i "error\|exception" "$LOG_DIR/moc.log" 2>/dev/null | grep -v "Frame Error Control" | grep -c "." || true)

# Ensure variables are numeric (strip any whitespace and default to 0)
SC_ERRORS=$(echo "${SC_ERRORS:-0}" | tr -d ' ')
GS_ERRORS=$(echo "${GS_ERRORS:-0}" | tr -d ' ')
MOC_ERRORS=$(echo "${MOC_ERRORS:-0}" | tr -d ' ')

# Final safety check - ensure they're valid numbers
SC_ERRORS=${SC_ERRORS:-0}
GS_ERRORS=${GS_ERRORS:-0}
MOC_ERRORS=${MOC_ERRORS:-0}

echo "Error Counts:"
echo "  Spacecraft Errors:      $SC_ERRORS"
echo "  Ground Station Errors:  $GS_ERRORS"
echo "  MOC Errors:             $MOC_ERRORS"
echo ""

# Calculate success rate
if [ "$SC_FRAMES" -gt 0 ]; then
    SUCCESS_RATE=$((MOC_FRAMES * 100 / SC_FRAMES))
else
    SUCCESS_RATE=0
fi

echo "Performance:"
echo "  Success Rate:           ${SUCCESS_RATE}%"
echo "  Frame Rate:             $(echo "scale=2; $MOC_FRAMES / $TEST_DURATION" | bc) frames/sec"
echo "  Data Rate:              $(echo "scale=2; $MOC_FRAMES * 1.115 / $TEST_DURATION" | bc) KB/sec"
echo ""

# Sample telemetry data
echo "Sample Telemetry (last 3 frames):"
tail -3 "$LOG_DIR/moc.log" | grep "Frame #" | sed 's/^/  /'
echo ""

# Validate results
echo "================================================================================"
echo "                           VALIDATION"
echo "================================================================================"
echo ""

PASSED=0
FAILED=0

# Test 1: Minimum frames received
if [ "$MOC_FRAMES" -ge "$MIN_FRAMES" ]; then
    echo -e "${GREEN}✓ PASS${NC}: MOC received at least $MIN_FRAMES frames ($MOC_FRAMES)"
    ((PASSED++))
else
    echo -e "${RED}✗ FAIL${NC}: MOC received only $MOC_FRAMES frames (expected at least $MIN_FRAMES)"
    ((FAILED++))
fi

# Test 2: No errors
TOTAL_ERRORS=$((${SC_ERRORS:-0} + ${GS_ERRORS:-0} + ${MOC_ERRORS:-0}))
if [ "$TOTAL_ERRORS" -eq 0 ]; then
    echo -e "${GREEN}✓ PASS${NC}: No errors detected"
    ((PASSED++))
else
    echo -e "${RED}✗ FAIL${NC}: $TOTAL_ERRORS errors detected"
    ((FAILED++))
fi

# Test 3: Data flow consistency
if [ "$SC_FRAMES" -eq "$GS_RX_FRAMES" ] && [ "$GS_TX_FRAMES" -eq "$MOC_FRAMES" ]; then
    echo -e "${GREEN}✓ PASS${NC}: Data flow is consistent"
    ((PASSED++))
else
    echo -e "${YELLOW}⚠ WARN${NC}: Data flow inconsistency detected"
fi

# Test 4: Success rate
if [ "$SUCCESS_RATE" -ge 95 ]; then
    echo -e "${GREEN}✓ PASS${NC}: Success rate is ${SUCCESS_RATE}% (>= 95%)"
    ((PASSED++))
else
    echo -e "${RED}✗ FAIL${NC}: Success rate is ${SUCCESS_RATE}% (expected >= 95%)"
    ((FAILED++))
fi

# Test 5: All processes still running
if ps -p $GS_PID > /dev/null && ps -p $SC_PID > /dev/null && ps -p $MOC_PID > /dev/null; then
    echo -e "${GREEN}✓ PASS${NC}: All processes still running"
    ((PASSED++))
else
    echo -e "${RED}✗ FAIL${NC}: One or more processes crashed"
    ((FAILED++))
fi

echo ""
echo "================================================================================"
echo "                           SUMMARY"
echo "================================================================================"
echo ""

TOTAL_TESTS=$((PASSED + FAILED))
if [ "$FAILED" -eq 0 ]; then
    echo -e "${GREEN}✓ ALL TESTS PASSED${NC} ($PASSED/$TOTAL_TESTS)"
    echo ""
    echo "The SLE Demo is working correctly!"
    EXIT_CODE=0
else
    echo -e "${RED}✗ SOME TESTS FAILED${NC} ($FAILED/$TOTAL_TESTS failed, $PASSED passed)"
    echo ""
    echo "Please check the logs in $LOG_DIR/ for details"
    EXIT_CODE=1
fi

echo ""
echo "Log files:"
echo "  Ground Station: $LOG_DIR/groundstation.log"
echo "  Spacecraft:     $LOG_DIR/spacecraft.log"
echo "  MOC:            $LOG_DIR/moc.log"
echo ""
echo "================================================================================"

exit $EXIT_CODE
