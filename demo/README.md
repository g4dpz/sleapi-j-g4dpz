# SLE Demo: Bidirectional Spacecraft Communication

This demonstration shows a complete end-to-end bidirectional communication system using the SLE (Space Link Extension) Java API with CCSDS-compliant utilities.

## System Architecture

```
┌─────────────────┐         ┌──────────────────┐         ┌─────────────┐
│   SPACECRAFT    │◄───────▶│  GROUND STATION  │◄───────▶│     MOC     │
│   SIMULATOR     │  TCP    │     SERVER       │  SLE    │   CLIENT    │
│                 │         │                  │         │             │
│  TM: Port 5555  │────────▶│  Downlink: 5555  │────────▶│  RAF: 5556  │
│  TC: Port 5557  │◄────────│  Uplink:   5558  │◄────────│  FSP: 5558  │
│                 │         │                  │         │             │
│  • Sends TM     │         │  • Forwards TM   │         │  • Receives │
│  • Receives TC  │         │  • Forwards TC   │         │    TM       │
│  • Executes CMD │         │  • CLTU decode   │         │  • Sends TC │
└─────────────────┘         └──────────────────┘         └─────────────┘
```

## Features

### Bidirectional Communication
- **Downlink (TM)**: Spacecraft → Ground Station → MOC
- **Uplink (TC)**: MOC → Ground Station → Spacecraft
- **Command Execution**: Automated command sequence with acknowledgments
- **CLCW Support**: Command Link Control Word for acknowledgments

### CCSDS Compliance
- Uses `esa.sle.java.api.ccsds.utils` library for all CCSDS operations
- CLTU encoding/decoding (CCSDS 231.0-B-3)
- Frame building and parsing (CCSDS 732.0-B-3, 232.0-B-3)
- CLCW encoding/decoding (CCSDS 232.0-B-3)
- CRC-16 calculation (CCSDS 131.0-B-3)

## Components

### 1. Spacecraft Simulator
- Generates CCSDS telemetry frames at 1 Hz
- Receives and executes telecommands via CLTU
- Maintains spacecraft state (solar panels, antenna, power mode)
- Sends CLCW acknowledgments in telemetry frames
- **Ports**: 
  - 5555 (TM downlink to Ground Station)
  - 5557 (TC uplink from Ground Station)

### 2. Ground Station Server
- Receives telemetry frames from spacecraft
- Forwards frames to MOC via SLE RAF service
- Receives CLTUs from MOC via SLE FSP service
- Forwards CLTUs to spacecraft
- **Ports**:
  - 5555 (Spacecraft TM receiver)
  - 5556 (SLE RAF service to MOC)
  - 5557 (Spacecraft TC sender)
  - 5558 (SLE FSP service from MOC)

### 3. MOC Client
- Receives telemetry via SLE RAF service
- Sends commands via SLE FSP service
- Automated command sequence:
  - Frame 10: DEPLOY_SOLAR_PANELS
  - Frame 20: ACTIVATE_ANTENNA
- Monitors CLCW acknowledgments
- Displays real-time telemetry and statistics
- **Ports**:
  - 5556 (SLE RAF service)
  - 5558 (SLE FSP service)

## Quick Start

### Automated Testing (Recommended)

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
1. Build the demo
2. Start all three components
3. Run for 30 seconds
4. Execute automated commands
5. Validate results (100% success expected)
6. Generate test report

### Manual Testing

**Terminal 1 - Ground Station:**
```bash
./run-groundstation.sh  # or run-groundstation.bat on Windows
```

**Terminal 2 - Spacecraft:**
```bash
./run-spacecraft.sh  # or run-spacecraft.bat on Windows
```

**Terminal 3 - MOC:**
```bash
./run-moc.sh  # or run-moc.bat on Windows
```

## Expected Output

### Spacecraft
```
[DOWNLINK] Sent TM frame #10 (CLCW ACK: -1)
[UPLINK] Received CLTU: 1289 bytes, decoded to 1115 byte command frame
[UPLINK] Command #1 (Frame Count: 0): DEPLOY_SOLAR_PANELS
[SPACECRAFT] Executing: DEPLOY_SOLAR_PANELS
[SPACECRAFT] ✓ Solar panels deployed
[DOWNLINK] Sent TM frame #11 (CLCW ACK: 0)
```

### Ground Station
```
[DOWNLINK] Received frame #10 from spacecraft (Queue: 1)
[DOWNLINK] Forwarded frame #10 to MOC (Queue: 0)
[UPLINK] Received CLTU from MOC: 1289 bytes
[UPLINK] Forwarded CLTU to spacecraft
```

### MOC
```
[RAF] Frame #10 | SCID=185 VCID=0 Count=9 | CLCW_ACK=-1 | TM Frame #9 | ...
[AUTO] Triggering command at frame 10...
[FSP] Sent command #1: DEPLOY_SOLAR_PANELS
[FSP] CLTU: 1289 bytes, 140 code blocks
[RAF] Frame #11 | SCID=185 VCID=0 Count=10 | CLCW_ACK=0 | TM Frame #10 | Solar: DEPLOYED | ...
```

## Telemetry Frame Format

CCSDS TM Transfer Frame (1115 bytes):
```
[0-1]     Header: Version+SCID+VCID+OCF_Flag
[2-3]     Header: Frame Counts
[4-5]     Header: Data Field Status
[6-1108]  Data Field (1103 bytes)
[1109-1112] OCF - Operational Control Field (CLCW)
[1113-1114] FECF - Frame Error Control (CRC-16)
```

## Command Frame Format

CCSDS TC Transfer Frame wrapped in CLTU (1289 bytes):
```
[0-1]     Start Sequence (0xEB90)
[2-1281]  Code Blocks (140 blocks × 8 bytes + 7 bytes)
[1282-1288] Tail Sequence (0xC5C5C5C5C5C5C5)
```

## Available Commands

- `DEPLOY_SOLAR_PANELS` - Deploy solar panels
- `STOW_SOLAR_PANELS` - Stow solar panels
- `ACTIVATE_ANTENNA` - Activate antenna
- `DEACTIVATE_ANTENNA` - Deactivate antenna
- `SET_POWER_MODE:LOW` - Set power mode to LOW
- `SET_POWER_MODE:NOMINAL` - Set power mode to NOMINAL
- `SET_POWER_MODE:HIGH` - Set power mode to HIGH
- `REQUEST_STATUS` - Request status report

## Building

### Prerequisites
- Java 17 or higher
- Maven 3.8.5 or higher

### Build Steps

```bash
# Build parent project first
cd ..
mvn clean install

# Build demo
cd demo
mvn clean package
```

## Configuration

Default configuration (can be modified in source code):

**Spacecraft:**
- Spacecraft ID: 185
- Virtual Channel: 0
- Frame Rate: 1000ms (1 Hz)

**Ports:**
- 5555: Spacecraft TM → Ground Station
- 5556: Ground Station RAF → MOC
- 5557: Ground Station TC → Spacecraft
- 5558: MOC FSP → Ground Station

## Documentation

- [ARCHITECTURE.md](ARCHITECTURE.md) - Detailed system architecture
- [TESTING.md](TESTING.md) - Testing guide and procedures
- [QUICKSTART.md](QUICKSTART.md) - Quick start guide
- [USING_LIBRARY_EXTENSION.md](USING_LIBRARY_EXTENSION.md) - CCSDS utilities usage
- [CLCW_ACKNOWLEDGMENT.md](CLCW_ACKNOWLEDGMENT.md) - Command acknowledgment flow

## CCSDS Utilities Used

This demo uses the following CCSDS utilities from `esa.sle.java.api.ccsds.utils`:

- **CLTUEncoder/Decoder** - CLTU encoding and decoding
- **TelemetryFrameBuilder** - Build TM frames
- **CommandFrameBuilder** - Build TC frames
- **FrameHeaderParser** - Parse frame headers
- **CLCWEncoder/Decoder** - CLCW handling
- **CRC16Calculator** - Frame error control

See [CCSDS Utilities README](../esa.sle.java.api.ccsds.utils/README.md) for details.

## Troubleshooting

### Connection Issues
- Ensure Ground Station starts first
- Check ports 5555-5558 are available
- Verify firewall settings

### No Commands Executed
- Check MOC is connected to FSP service (port 5558)
- Verify Ground Station forwards CLTUs to spacecraft
- Check spacecraft uplink connection (port 5557)

### CLCW Not Updating
- Verify spacecraft receives commands successfully
- Check telemetry frames include OCF
- Ensure MOC decodes CLCW correctly

## Test Results

Expected test results:
- ✅ 100% success rate
- ✅ All frames received
- ✅ Both commands executed
- ✅ CLCW acknowledgments confirmed
- ✅ No errors detected

## License

This demonstration code is provided as-is for educational purposes.

## References

- CCSDS 911.1-B-4: RAF Service
- CCSDS 912.1-B-4: CLTU Service
- CCSDS 231.0-B-3: TC Synchronization and Channel Coding
- CCSDS 232.0-B-3: TC Space Data Link Protocol
- CCSDS 732.0-B-3: AOS Space Data Link Protocol
- CCSDS 131.0-B-3: TM Synchronization and Channel Coding
