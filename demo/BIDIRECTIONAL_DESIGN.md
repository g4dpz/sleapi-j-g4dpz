# Bidirectional Communication Design

## Overview
This document describes how to implement bidirectional communication between MOC and Spacecraft using forward (command) frames.

## Current System (Implemented)
```
Spacecraft --TM frames--> Ground Station --RAF--> MOC
  (Port 5555)                                (Port 5556)
```

## Target System (Bidirectional)
```
Spacecraft <--TC frames-- Ground Station <--FSP-- MOC
  (Port 5557)                                (Port 5558)
     |                                            |
     +--------TM frames--> Ground Station --RAF--+
       (Port 5555)                          (Port 5556)
```

## Components Created

### 1. CommandFrame.java ✅
**Location**: `demo/src/main/java/esa/sle/demo/common/CommandFrame.java`

**Purpose**: Represents CCSDS AOS Forward Frame for telecommands

**Structure**:
```
[0-1]   Primary Header: Version+SCID+VCID
[2-3]   Frame Counts
[4-5]   Data Field Status (0x8000 = command frame)
[6-1112] Command String Data
[1113-1114] FECF (CRC-16)
```

**Key Methods**:
- `CommandFrame(scid, vcid, count, command)` - Create command frame
- `CommandFrame(byte[] data)` - Parse received command frame
- `getCommand()` - Extract command string
- `getData()` - Get raw frame bytes

## Implementation Steps

### Step 1: Update Ground Station Server

Add four server threads instead of two:

```java
// DOWNLINK (Spacecraft → Ground Station → MOC)
Thread spacecraftDownlinkThread = new Thread(this::handleSpacecraftDownlink);
Thread mocRafThread = new Thread(this::handleMocRaf);

// UPLINK (MOC → Ground Station → Spacecraft)
Thread mocFspThread = new Thread(this::handleMocFsp);
Thread spacecraftUplinkThread = new Thread(this::handleSpacecraftUplink);
```

**New Ports**:
- 5557: Spacecraft Uplink (TC frames to spacecraft)
- 5558: MOC FSP Service (TC frames from MOC)

**New Buffers**:
```java
private final BlockingQueue<byte[]> telemetryBuffer = new LinkedBlockingQueue<>(1000);
private final BlockingQueue<byte[]> commandBuffer = new LinkedBlockingQueue<>(1000);
```

### Step 2: Update MOC Client

Add command sending capability:

```java
public class MOCClient {
    private Socket rafSocket;      // Existing - receive TM
    private Socket fspSocket;      // New - send TC
    
    // Send command to spacecraft
    public void sendCommand(String command) {
        CommandFrame frame = new CommandFrame(
            SPACECRAFT_ID,
            VIRTUAL_CHANNEL_ID,
            commandCount++,
            command
        );
        
        fspSocket.getOutputStream().write(frame.getData());
        System.out.println("[MOC] Sent command: " + command);
    }
}
```

**Example Commands**:
- `"DEPLOY_SOLAR_PANELS"`
- `"ACTIVATE_ANTENNA"`
- `"SET_POWER_MODE:LOW"`
- `"REQUEST_STATUS"`

### Step 3: Update Spacecraft Simulator

Add command receiver and executor:

```java
public class SpacecraftSimulator {
    private Socket downlinkSocket;  // Existing - send TM
    private Socket uplinkSocket;    // New - receive TC
    
    // Receive and execute commands
    private void receiveCommands() {
        while (running.get()) {
            byte[] frameData = readFrame(uplinkSocket);
            CommandFrame cmdFrame = new CommandFrame(frameData);
            String command = cmdFrame.getCommand();
            
            System.out.println("[SPACECRAFT] Received command: " + command);
            executeCommand(command);
            sendAcknowledgment(command);
        }
    }
    
    private void executeCommand(String command) {
        switch (command) {
            case "DEPLOY_SOLAR_PANELS":
                solarPanelsDeployed = true;
                System.out.println("[SPACECRAFT] Solar panels deployed");
                break;
            case "ACTIVATE_ANTENNA":
                antennaActive = true;
                System.out.println("[SPACECRAFT] Antenna activated");
                break;
            case "REQUEST_STATUS":
                sendStatusReport();
                break;
            default:
                System.out.println("[SPACECRAFT] Unknown command: " + command);
        }
    }
    
    private void sendAcknowledgment(String command) {
        String ack = "ACK:" + command + ":SUCCESS";
        TelemetryFrame ackFrame = new TelemetryFrame(
            SPACECRAFT_ID,
            VIRTUAL_CHANNEL_ID,
            frameCount++,
            ack.getBytes()
        );
        downlinkSocket.getOutputStream().write(ackFrame.getData());
    }
}
```

## Data Flow Examples

### Example 1: Deploy Solar Panels

```
1. MOC creates command:
   CommandFrame("DEPLOY_SOLAR_PANELS")
   
2. MOC sends via FSP (port 5558):
   0B 91 | 00 00 | 80 00 | 44 45 50 4C 4F 59 5F 53 4F 4C 41 52 ... | CRC

3. Ground Station receives and forwards to Spacecraft (port 5557)

4. Spacecraft receives, parses, and executes:
   - Deploys solar panels
   - Updates internal state
   
5. Spacecraft sends ACK via TM:
   "ACK:DEPLOY_SOLAR_PANELS:SUCCESS"
   
6. MOC receives ACK via RAF (port 5556)
```

### Example 2: Request Status

```
1. MOC: CommandFrame("REQUEST_STATUS")
2. Ground Station forwards to Spacecraft
3. Spacecraft executes and sends detailed status via TM:
   "STATUS:SOLAR=ON,ANTENNA=ON,POWER=NOMINAL,TEMP=25C"
4. MOC receives status via RAF
```

## Security Considerations

### Command Authentication
```java
// Add HMAC to command frames
String commandWithAuth = command + "|HMAC:" + calculateHMAC(command, secretKey);
CommandFrame secureFrame = new CommandFrame(scid, vcid, count, commandWithAuth);

// Spacecraft verifies
String[] parts = receivedCommand.split("\\|HMAC:");
if (verifyHMAC(parts[0], parts[1], secretKey)) {
    executeCommand(parts[0]);
} else {
    System.err.println("[SPACECRAFT] Command authentication failed!");
}
```

### Command Sequence Numbers
```java
// Prevent replay attacks
private int lastCommandSequence = 0;

if (cmdFrame.getFrameCount() <= lastCommandSequence) {
    System.err.println("[SPACECRAFT] Duplicate/old command rejected");
    return;
}
lastCommandSequence = cmdFrame.getFrameCount();
```

## Testing

### Manual Test Sequence
```bash
# Terminal 1: Start Ground Station
java -jar target/sle-demo-1.0.0-groundstation.jar

# Terminal 2: Start Spacecraft
java -jar target/sle-demo-1.0.0-spacecraft.jar

# Terminal 3: Start MOC
java -jar target/sle-demo-1.0.0-moc.jar

# In MOC terminal, type commands:
> DEPLOY_SOLAR_PANELS
> ACTIVATE_ANTENNA
> REQUEST_STATUS
```

### Automated Test
Add to `test-demo.sh`:
```bash
# Send test commands
echo "DEPLOY_SOLAR_PANELS" | nc localhost 5558
sleep 2
echo "REQUEST_STATUS" | nc localhost 5558
sleep 2

# Verify acknowledgments in MOC log
grep "ACK:DEPLOY_SOLAR_PANELS" test-logs/moc.log
```

## Frame Format Comparison

### Telemetry Frame (Downlink)
```
0B 91 | 00 00 | 40 00 | [TM data...] | 00 00 00 00 | CRC
  ^       ^       ^         ^              ^          ^
  |       |       |         |              |          FECF
  |       |       |         |              OCF (CLCW)
  |       |       |         Data Field (1103 bytes)
  |       |       Data Field Status (sync flag)
  |       Frame Counts
  Header (SCID, VCID, OCF flag=1)
```

### Command Frame (Uplink)
```
0B 91 | 00 00 | 80 00 | [TC command...] | CRC
  ^       ^       ^         ^               ^
  |       |       |         |               FECF
  |       |       |         Command Data (1111 bytes)
  |       |       Data Field Status (command flag)
  |       Frame Counts
  Header (SCID, VCID)
```

## Benefits

1. **Full Duplex Communication**: MOC can send commands and receive telemetry simultaneously
2. **Standard Compliance**: Uses CCSDS AOS forward frames
3. **Acknowledgments**: Spacecraft confirms command execution
4. **Error Detection**: CRC-16 on all frames
5. **Buffering**: Ground Station queues both directions
6. **Realistic**: Mirrors actual ground-to-space operations

## Next Steps

To fully implement:
1. ✅ Create CommandFrame class
2. ⏳ Update GroundStationServer with 4 threads and 2 buffers
3. ⏳ Update MOCClient with FSP socket and sendCommand()
4. ⏳ Update SpacecraftSimulator with uplink receiver and command executor
5. ⏳ Add command acknowledgment mechanism
6. ⏳ Update test scripts to test bidirectional flow
7. ⏳ Add security/authentication layer

## File Changes Required

- ✅ `CommandFrame.java` - Created
- ⏳ `GroundStationServer.java` - Add uplink handling
- ⏳ `MOCClient.java` - Add command sending
- ⏳ `SpacecraftSimulator.java` - Add command receiving
- ⏳ `test-demo.sh` - Add command testing
- ⏳ `ARCHITECTURE.md` - Update with bidirectional flow

---

**Status**: Design complete, CommandFrame implemented, ready for full integration.
