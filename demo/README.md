# SLE Demo: Spacecraft to MOC Telemetry Transfer

This demonstration shows a complete end-to-end telemetry transfer system using the SLE (Space Link Extension) Java API.

## System Architecture

```
┌─────────────────┐         ┌──────────────────┐         ┌─────────────┐
│   SPACECRAFT    │         │  GROUND STATION  │         │     MOC     │
│   SIMULATOR     │────────▶│     SERVER       │────────▶│   CLIENT    │
│                 │  TCP    │                  │  SLE    │             │
│  Generates TM   │  :5555  │  Receives &      │  RAF    │  Receives & │
│  Frames         │         │  Forwards Frames │  :5556  │  Processes  │
└─────────────────┘         └──────────────────┘         └─────────────┘
```

## Components

### 1. Spacecraft Simulator
- **Purpose**: Simulates a spacecraft generating telemetry frames
- **Function**: 
  - Generates CCSDS-compliant telemetry frames
  - Includes simulated sensor data (temperature, voltage, current, attitude, altitude)
  - Sends frames to Ground Station at 1 Hz
- **Port**: Connects to Ground Station on port 5555

### 2. Ground Station Server
- **Purpose**: Acts as the ground station receiving spacecraft data
- **Function**:
  - Receives telemetry frames from spacecraft
  - Buffers frames in a queue
  - Provides SLE RAF (Return All Frames) service to MOC
  - Forwards frames to connected MOC clients
- **Ports**: 
  - 5555 (Spacecraft receiver)
  - 5556 (SLE RAF service)

### 3. MOC Client
- **Purpose**: Mission Operations Center client
- **Function**:
  - Connects to Ground Station via SLE RAF service
  - Receives and processes telemetry frames
  - Decodes frame headers and telemetry data
  - Displays real-time telemetry information
  - Tracks statistics (frames received, data volume)
- **Port**: Connects to Ground Station on port 5556

## Telemetry Frame Format

The system uses simplified CCSDS Telemetry Transfer Frames:

```
┌──────────────────────────────────────────────────────────┐
│ Primary Header (6 bytes)                                 │
├──────────────────────────────────────────────────────────┤
│ - Version (2 bits)                                       │
│ - Spacecraft ID (10 bits)                                │
│ - Virtual Channel ID (3 bits)                            │
│ - OCF Flag (1 bit)                                       │
│ - Master Channel Frame Count (8 bits)                    │
│ - Virtual Channel Frame Count (8 bits)                   │
│ - Transfer Frame Data Field Status (16 bits)             │
├──────────────────────────────────────────────────────────┤
│ Data Field (1109 bytes)                                  │
│ - Timestamp (8 bytes)                                    │
│ - Temperature (4 bytes float)                            │
│ - Voltage (4 bytes float)                                │
│ - Current (4 bytes float)                                │
│ - Attitude (4 bytes int)                                 │
│ - Altitude (4 bytes int)                                 │
│ - Status flags (3 bytes)                                 │
│ - Additional data / padding                              │
└──────────────────────────────────────────────────────────┘
Total: 1115 bytes
```

## Building the Demo

### Prerequisites
- Java 17 or higher
- Maven 3.8.5 or higher
- SLE Java API Core library (built from parent project)

### Build Steps

1. **Build the parent SLE API project first:**
   ```bash
   cd ..
   mvn clean install
   ```

2. **Build the demo:**
   ```bash
   cd demo
   mvn clean package
   ```

This will create three JAR files in `target/`:
- `sle-demo-1.0.0-spacecraft.jar` - Spacecraft Simulator
- `sle-demo-1.0.0-groundstation.jar` - Ground Station Server
- `sle-demo-1.0.0-moc.jar` - MOC Client

## Running the Demo

You need to run all three components in separate terminal windows.

### Automated Testing (Recommended)

The easiest way to test the demo is using the automated test script:

**Unix/Linux/macOS:**
```bash
cd demo
./test-demo.sh
```

**Windows:**
```cmd
cd demo
test-demo.bat
```

This will:
1. Build everything
2. Start all three components
3. Run for 30 seconds
4. Validate results
5. Generate a test report

See [TESTING.md](TESTING.md) for details.

### Manual Testing

### Option 1: Using Maven (Development)

**Terminal 1 - Ground Station:**
```bash
mvn exec:java -Dexec.mainClass="esa.sle.demo.groundstation.GroundStationServer"
```

**Terminal 2 - Spacecraft:**
```bash
mvn exec:java -Dexec.mainClass="esa.sle.demo.spacecraft.SpacecraftSimulator"
```

**Terminal 3 - MOC:**
```bash
mvn exec:java -Dexec.mainClass="esa.sle.demo.moc.MOCClient"
```

### Option 2: Using JAR Files (Production)

**Terminal 1 - Ground Station:**
```bash
java -jar target/sle-demo-1.0.0-groundstation.jar
```

**Terminal 2 - Spacecraft:**
```bash
java -jar target/sle-demo-1.0.0-spacecraft.jar
```

**Terminal 3 - MOC:**
```bash
java -jar target/sle-demo-1.0.0-moc.jar
```

### Option 3: Using Run Scripts

**Unix/Linux/macOS:**
```bash
# Make scripts executable
chmod +x run-*.sh

# Terminal 1
./run-groundstation.sh

# Terminal 2
./run-spacecraft.sh

# Terminal 3
./run-moc.sh
```

**Windows:**
```cmd
REM Terminal 1
run-groundstation.bat

REM Terminal 2
run-spacecraft.bat

REM Terminal 3
run-moc.bat
```

## Startup Sequence

1. **Start Ground Station first** - It must be running to accept connections
2. **Start Spacecraft** - It will connect to the Ground Station
3. **Start MOC** - It will connect to the Ground Station and start receiving frames

## Expected Output

### Ground Station
```
================================================================================
GROUND STATION SERVER
================================================================================
Spacecraft Port: 5555
SLE Service Port: 5556
Frame Queue Size: 1000
================================================================================

[GROUND STATION] Listening for spacecraft on port 5555
[GROUND STATION] SLE RAF service listening on port 5556
[GROUND STATION] Spacecraft connected: /127.0.0.1:xxxxx
[GROUND STATION] MOC connected: /127.0.0.1:xxxxx
[GROUND STATION] Received frame #1 from spacecraft (Queue: 1)
[GROUND STATION] Forwarded frame #1 to MOC (Queue: 0)
```

### Spacecraft
```
================================================================================
SPACECRAFT SIMULATOR
================================================================================
Spacecraft ID: 185
Virtual Channel: 0
Ground Station: localhost:5555
Frame Rate: 1000 ms
================================================================================

[SPACECRAFT] Connected to Ground Station
[SPACECRAFT] Sent: TM Frame [SCID=185, VCID=0, Count=0, Size=1115 bytes, Time=...]
[SPACECRAFT] Sent: TM Frame [SCID=185, VCID=0, Count=1, Size=1115 bytes, Time=...]
```

### MOC
```
================================================================================
MISSION OPERATIONS CENTER (MOC) CLIENT
================================================================================
Ground Station: localhost:5556
Service: RAF (Return All Frames)
================================================================================

[MOC] Connected to Ground Station
[MOC] Receiving telemetry frames...
--------------------------------------------------------------------------------
[MOC] Frame #1 | SCID=185 VCID=0 Count=0 | Temp=45.3°C Volt=28.5V Curr=3.2A Alt=450km Att=125° | Solar=✓ Ant=✓ Sys=OK | Time=2026-01-30 22:15:30.123
[MOC] Frame #2 | SCID=185 VCID=0 Count=1 | Temp=46.1°C Volt=28.7V Curr=3.1A Alt=451km Att=126° | Solar=✓ Ant=✓ Sys=OK | Time=2026-01-30 22:15:31.125
```

## Stopping the Demo

Press `Ctrl+C` in each terminal window to stop the components gracefully. The MOC will display session statistics on shutdown.

## Configuration

You can modify the following parameters in the source code:

### Spacecraft Simulator
- `SPACECRAFT_ID` - Spacecraft identifier (default: 185)
- `VIRTUAL_CHANNEL_ID` - Virtual channel (default: 0)
- `FRAME_RATE_MS` - Frame generation rate (default: 1000ms = 1 Hz)
- `GROUND_STATION_HOST` - Ground station hostname (default: localhost)
- `GROUND_STATION_PORT` - Ground station port (default: 5555)

### Ground Station Server
- `SPACECRAFT_PORT` - Port for spacecraft connection (default: 5555)
- `SLE_PORT` - Port for SLE RAF service (default: 5556)
- Frame queue size (default: 1000 frames)

### MOC Client
- `GROUND_STATION_HOST` - Ground station hostname (default: localhost)
- `GROUND_STATION_PORT` - SLE service port (default: 5556)

## Troubleshooting

### "Connection refused" errors
- Ensure the Ground Station is started first
- Check that ports 5555 and 5556 are not in use by other applications
- Verify firewall settings allow local connections

### No frames received at MOC
- Check that all three components are running
- Verify the Spacecraft successfully connected to Ground Station
- Check Ground Station logs for errors

### Frame queue full warnings
- The MOC is not consuming frames fast enough
- Increase the frame queue size in Ground Station
- Reduce the spacecraft frame rate

## Architecture Notes

This is a **simplified demonstration** for educational purposes. A production SLE system would include:

- Full SLE protocol implementation (BIND, START, STOP, UNBIND operations)
- Authentication and security (credentials, hash functions)
- Service instance identifiers (SII)
- Quality of service parameters
- Error handling and diagnostics
- Frame quality indicators
- Time correlation
- Multiple virtual channels
- ASN.1 encoding/decoding
- TML (Transport Mapping Layer) protocol

## Next Steps

To extend this demo:

1. **Add SLE Protocol**: Implement full SLE RAF operations using the SLE Java API
2. **Add RCF Service**: Filter frames by virtual channel
3. **Add CLTU Service**: Implement forward link (MOC to Spacecraft)
4. **Add Authentication**: Implement SLE credentials and security
5. **Add Configuration Files**: Use external configuration for parameters
6. **Add Logging**: Implement proper logging framework
7. **Add Metrics**: Track performance metrics and statistics
8. **Add GUI**: Create graphical interface for monitoring

## References

- CCSDS 911.1 - Space Link Extension - Return All Frames Service
- CCSDS 132.0 - TM Space Data Link Protocol
- SLE Java API Documentation

## License

This demonstration code is provided as-is for educational purposes.
