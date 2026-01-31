# Quick Start Guide

Get the demo running in 3 minutes!

## Prerequisites

- Java 17+ installed
- Maven 3.8.5+ installed
- 3 terminal windows

## Step 1: Build Everything (2 minutes)

```bash
# Build parent SLE API
cd ..
mvn clean install -DskipTests

# Build demo
cd demo
mvn clean package
```

## Step 2: Run the Demo (30 seconds)

Open 3 terminal windows and run these commands:

### Terminal 1: Ground Station
```bash
cd demo
./run-groundstation.sh    # Unix/Linux/macOS
# OR
run-groundstation.bat     # Windows
```

Wait for: `[GROUND STATION] Listening for spacecraft on port 5555`

### Terminal 2: Spacecraft
```bash
cd demo
./run-spacecraft.sh       # Unix/Linux/macOS
# OR
run-spacecraft.bat        # Windows
```

Wait for: `[SPACECRAFT] Connected to Ground Station`

### Terminal 3: MOC
```bash
cd demo
./run-moc.sh              # Unix/Linux/macOS
# OR
run-moc.bat               # Windows
```

You should see telemetry frames flowing!

## What You'll See

**MOC Terminal:**
```
[MOC] Frame #1 | SCID=185 VCID=0 Count=0 | Temp=45.3°C Volt=28.5V Curr=3.2A Alt=450km Att=125° | Solar=✓ Ant=✓ Sys=OK
[MOC] Frame #2 | SCID=185 VCID=0 Count=1 | Temp=46.1°C Volt=28.7V Curr=3.1A Alt=451km Att=126° | Solar=✓ Ant=✓ Sys=OK
```

## Stop the Demo

Press `Ctrl+C` in each terminal window.

## Troubleshooting

**"Connection refused"**
- Start Ground Station first, wait for it to be ready
- Check ports 5555 and 5556 are not in use

**"Build failure"**
- Ensure parent SLE API is built first: `cd .. && mvn install`
- Check Java 17+ is installed: `java -version`

**No frames appearing**
- Check all 3 components are running
- Look for error messages in Ground Station terminal

## Next Steps

See [README.md](README.md) for:
- Detailed architecture
- Configuration options
- How to extend the demo
- Full documentation

## One-Line Demo (Advanced)

If you want to run all three in background (Unix/Linux/macOS only):

```bash
# Build
cd .. && mvn clean install -DskipTests && cd demo && mvn clean package

# Run (in background)
./run-groundstation.sh > gs.log 2>&1 &
sleep 2
./run-spacecraft.sh > sc.log 2>&1 &
sleep 2
./run-moc.sh

# Stop (when done)
pkill -f "sle-demo"
```

## Demo Video

Watch the telemetry flow in real-time:
1. Ground Station receives frames from Spacecraft
2. Ground Station forwards frames to MOC
3. MOC decodes and displays telemetry data
4. Statistics updated every 10 frames

Enjoy! 🚀
