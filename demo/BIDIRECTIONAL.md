# Bidirectional Communication - User Guide

## Overview

The SLE Demo now supports **full bidirectional communication** between MOC and Spacecraft:

- **Downlink (TM)**: Spacecraft → Ground Station → MOC (telemetry frames via RAF)
- **Uplink (TC)**: MOC → Ground Station → Spacecraft (command frames via FSP)

## Architecture

```
┌─────────────┐                    ┌──────────────────┐                    ┌─────────┐
│             │  TM (Port 5555)    │                  │  RAF (Port 5556)   │         │
│  Spacecraft ├───────────────────>│  Ground Station  ├───────────────────>│   MOC   │
│             │                    │                  │                    │         │
│             │  TC (Port 5557)    │                  │  FSP (Port 5558)   │         │
│             │<───────────────────┤                  │<───────────────────┤         │
└─────────────┘                    └──────────────────┘                    └─────────┘
```

## Components

### 1. Ground Station Server (4 threads, 2 buffers)

**Ports:**
- 5555: Spacecraft Downlink (receive TM)
- 5556: MOC RAF Service (forward TM)
- 5557: Spacecraft Uplink (forward TC)
- 5558: MOC FSP Service (receive TC)

**Buffers:**
- Telemetry Buffer: 1000 frames
- Command Buffer: 1000 frames

### 2. Spacecraft Simulator (2 threads)

**Capabilities:**
- Sends telemetry frames every 1 second
- Receives and executes commands
- Maintains internal state (solar panels, antenna, power mode)
- Reports state in telemetry messages

**State Variables:**
- `solarPanelsDeployed`: boolean
- `antennaActive`: boolean
- `powerMode`: "LOW" | "NOMINAL" | "HIGH"

### 3. MOC Client (2 threads)

**Capabilities:**
- Receives telemetry via RAF service
- Sends commands via FSP service
- Interactive command input from console
- Displays frame structure and statistics

## Available Commands

| Command | Description |
|---------|-------------|
| `DEPLOY_SOLAR_PANELS` | Deploy solar panels |
| `STOW_SOLAR_PANELS` | Stow solar panels |
| `ACTIVATE_ANTENNA` | Activate antenna |
| `DEACTIVATE_ANTENNA` | Deactivate antenna |
| `SET_POWER_MODE:LOW` | Set power mode to LOW |
| `SET_POWER_MODE:NOMINAL` | Set power mode to NOMINAL |
| `SET_POWER_MODE:HIGH` | Set power mode to HIGH |
| `REQUEST_STATUS` | Request status report |

## Quick Start

### Option 1: Interactive Test (Recommended)

```bash
cd demo
./test-bidirectional.sh
```

This will:
1. Start Ground Station
2. Start Spacecraft
3. Start MOC (interactive)
4. Allow you to type commands

**Example Session:**
```
[RAF] Frame #1 | SCID=185 VCID=0 Count=0 | TM Frame #0 | Time: ... | Solar: STOWED | Antenna: INACTIVE | Power: NOMINAL | Commands RX: 0

> DEPLOY_SOLAR_PANELS
[FSP] Sent command #1: DEPLOY_SOLAR_PANELS

[RAF] Frame #2 | SCID=185 VCID=0 Count=1 | TM Frame #1 | Time: ... | Solar: DEPLOYED | Antenna: INACTIVE | Power: NOMINAL | Commands RX: 1

> ACTIVATE_ANTENNA
[FSP] Sent command #2: ACTIVATE_ANTENNA

[RAF] Frame #3 | SCID=185 VCID=0 Count=2 | TM Frame #2 | Time: ... | Solar: DEPLOYED | Antenna: ACTIVE | Power: NOMINAL | Commands RX: 2
```

### Option 2: Manual Start (3 terminals)

**Terminal 1 - Ground Station:**
```bash
cd demo
java -jar target/sle-demo-1.0.0-groundstation.jar
```

**Terminal 2 - Spacecraft:**
```bash
cd demo
java -jar target/sle-demo-1.0.0-spacecraft.jar
```

**Terminal 3 - MOC:**
```bash
cd demo
java -jar target/sle-demo-1.0.0-moc.jar
```

Then type commands in Terminal 3.

## Frame Formats

### Telemetry Frame (TM)
```
0B 91 | 00 00 | 40 00 | [Message: "TM Frame #X | ..."] | 00 00 00 00 | CRC
  ^       ^       ^                    ^                      ^          ^
  |       |       |                    |                      |          FECF
  |       |       |                    |                      OCF
  |       |       |                    Data (1103 bytes)
  |       |       Data Field Status
  |       Frame Counts
  Header (SCID, VCID, OCF=1)
```

### Command Frame (TC)
```
0B 91 | 00 00 | 80 00 | [Command: "DEPLOY_SOLAR_PANELS"] | CRC
  ^       ^       ^                    ^                     ^
  |       |       |                    |                     FECF
  |       |       |                    Command (1111 bytes)
  |       |       Data Field Status (0x8000 = command)
  |       Frame Counts
  Header (SCID, VCID)
```

## Data Flow Example

### Scenario: Deploy Solar Panels

**Step 1:** MOC sends command
```
[MOC FSP] → CommandFrame("DEPLOY_SOLAR_PANELS")
```

**Step 2:** Ground Station receives and queues
```
[GS FSP] RX TC frame #1 from MOC (Queue: 1)
```

**Step 3:** Ground Station forwards to Spacecraft
```
[GS UPLINK] TX TC frame #1 to spacecraft (Queue: 0)
```

**Step 4:** Spacecraft receives and executes
```
[SPACECRAFT UPLINK] Received command #1: DEPLOY_SOLAR_PANELS
[SPACECRAFT] Executing: DEPLOY_SOLAR_PANELS
[SPACECRAFT] ✓ Solar panels deployed
```

**Step 5:** Spacecraft sends updated telemetry
```
[SPACECRAFT DOWNLINK] Sent TM frame #5
Message: "TM Frame #5 | Solar: DEPLOYED | Antenna: INACTIVE | Commands RX: 1"
```

**Step 6:** Ground Station relays to MOC
```
[GS DOWNLINK] RX TM frame #5 (Queue: 1)
[GS RAF] TX TM frame #5 to MOC (Queue: 0)
```

**Step 7:** MOC displays result
```
[MOC RAF] Frame #5 | SCID=185 VCID=0 Count=4 | TM Frame #5 | Solar: DEPLOYED | ...
```

## Monitoring

### Ground Station Logs
```bash
tail -f test-logs/groundstation-bidir.log
```

Shows:
- `[DOWNLINK]` - TM frames from spacecraft
- `[RAF]` - TM frames to MOC
- `[FSP]` - TC frames from MOC
- `[UPLINK]` - TC frames to spacecraft

### Spacecraft Logs
```bash
tail -f test-logs/spacecraft-bidir.log
```

Shows:
- `[DOWNLINK]` - TM frames sent
- `[UPLINK]` - TC frames received
- `[SPACECRAFT]` - Command execution

## Testing Sequence

Try this sequence to test all commands:

```bash
# 1. Deploy solar panels
DEPLOY_SOLAR_PANELS

# 2. Activate antenna
ACTIVATE_ANTENNA

# 3. Set power mode to high
SET_POWER_MODE:HIGH

# 4. Request status
REQUEST_STATUS

# 5. Set power mode back to nominal
SET_POWER_MODE:NOMINAL

# 6. Stow solar panels
STOW_SOLAR_PANELS

# 7. Deactivate antenna
DEACTIVATE_ANTENNA
```

Watch the telemetry frames to see the spacecraft state change after each command!

## Statistics

The MOC displays statistics every 10 frames:
```
[RAF] Statistics: 10 frames RX, 3 commands TX, 10.89 KB
```

On shutdown, full statistics are shown:
```
MOC SESSION STATISTICS
Total Frames Received: 45
Total Commands Sent: 7
Total Data Volume: 49.07 KB (0.05 MB)
```

## Troubleshooting

### Commands not executing
- Check Ground Station logs for FSP connection
- Verify Spacecraft uplink connection
- Ensure command spelling is exact (case-sensitive)

### No telemetry received
- Check Ground Station downlink connection
- Verify Spacecraft is running
- Check RAF service connection

### Connection errors
- Ensure all ports are available (5555-5558)
- Start components in order: Ground Station → Spacecraft → MOC
- Wait 2-3 seconds between starting each component

## Advanced Usage

### Automated Command Sequence

Create a command file `commands.txt`:
```
DEPLOY_SOLAR_PANELS
ACTIVATE_ANTENNA
SET_POWER_MODE:HIGH
REQUEST_STATUS
```

Pipe to MOC:
```bash
cat commands.txt | java -jar target/sle-demo-1.0.0-moc.jar
```

### Command Rate Limiting

Add delays between commands:
```bash
echo "DEPLOY_SOLAR_PANELS" | java -jar target/sle-demo-1.0.0-moc.jar &
sleep 5
echo "ACTIVATE_ANTENNA" | java -jar target/sle-demo-1.0.0-moc.jar &
```

## Security Considerations

**Note:** This demo does not implement authentication. In a production system, you would add:

1. **HMAC Authentication**: Sign commands with shared secret
2. **Sequence Numbers**: Prevent replay attacks
3. **Encryption**: Protect command content
4. **Access Control**: Restrict who can send commands

See `BIDIRECTIONAL_DESIGN.md` for security implementation details.

## Summary

✅ **Implemented:**
- Full bidirectional communication
- 4-port Ground Station (downlink + uplink)
- Command execution on Spacecraft
- Interactive MOC command interface
- State tracking and reporting
- CCSDS-compliant frame formats

🎯 **Benefits:**
- Realistic ground-to-space operations
- Real-time command and control
- State feedback via telemetry
- Standard SLE protocol compliance
- Easy to extend with new commands

---

**Ready to test!** Run `./test-bidirectional.sh` and start sending commands!
