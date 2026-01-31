#!/bin/bash
# Test bidirectional communication

echo "================================================================================"
echo "                    BIDIRECTIONAL COMMUNICATION TEST"
echo "================================================================================"
echo ""
echo "This test demonstrates:"
echo "  1. Telemetry downlink (Spacecraft → Ground Station → MOC)"
echo "  2. Command uplink (MOC → Ground Station → Spacecraft)"
echo ""
echo "================================================================================"
echo ""

# Create log directory
mkdir -p test-logs

# Start Ground Station
echo "[1/3] Starting Ground Station..."
java -jar target/sle-demo-1.0.0-groundstation.jar > test-logs/groundstation-bidir.log 2>&1 &
GS_PID=$!
sleep 3
echo "  Ground Station PID: $GS_PID"
echo ""

# Start Spacecraft
echo "[2/3] Starting Spacecraft..."
java -jar target/sle-demo-1.0.0-spacecraft.jar > test-logs/spacecraft-bidir.log 2>&1 &
SC_PID=$!
sleep 3
echo "  Spacecraft PID: $SC_PID"
echo ""

# Start MOC
echo "[3/3] Starting MOC..."
echo ""
echo "================================================================================"
echo "MOC CLIENT STARTED - You can now send commands!"
echo "================================================================================"
echo ""
echo "Try these commands:"
echo "  DEPLOY_SOLAR_PANELS"
echo "  ACTIVATE_ANTENNA"
echo "  SET_POWER_MODE:LOW"
echo "  REQUEST_STATUS"
echo ""
echo "Press Ctrl+C to stop all components"
echo "================================================================================"
echo ""

# Run MOC in foreground (interactive)
java -jar target/sle-demo-1.0.0-moc.jar

# Cleanup on exit
echo ""
echo "Cleaning up..."
kill $GS_PID $SC_PID 2>/dev/null
pkill -f "sle-demo" 2>/dev/null
echo "Done!"
