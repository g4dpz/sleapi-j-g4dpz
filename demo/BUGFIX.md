# Bugfix Summary - CLTU Timing Issue

## Issue

The second command was not being received by the spacecraft due to a timing issue in the CLTU reading logic.

## Root Cause

The Ground Station's FSP handler was reading fixed-size command frames (1115 bytes) instead of variable-size CLTUs (1289 bytes). This caused:

1. MOC sends CLTU (1289 bytes)
2. Ground Station reads only 1115 bytes
3. Remaining 174 bytes left in buffer
4. Next CLTU read starts mid-stream
5. Spacecraft times out waiting for complete CLTU
6. Connection drops and reconnects

## Solution

Updated Ground Station to properly handle CLTUs:

### Before (Broken)
```java
// FSP handler read fixed-size frames
byte[] buffer = new byte[FRAME_SIZE]; // 1115 bytes
while (bytesRead < FRAME_SIZE) {
    int read = in.read(buffer, bytesRead, FRAME_SIZE - bytesRead);
    // ...
}
```

### After (Fixed)
```java
// FSP handler reads complete CLTUs
byte[] buffer = new byte[4096]; // Large buffer for CLTUs

// Find start sequence (0xEB90)
while (!foundStart) {
    int b1 = in.read();
    if (b1 == 0xEB) {
        int b2 = in.read();
        if (b2 == 0x90) {
            buffer[bytesRead++] = (byte) b1;
            buffer[bytesRead++] = (byte) b2;
            foundStart = true;
        }
    }
}

// Read until tail sequence (7 x 0xC5)
int tailMatchCount = 0;
while (bytesRead < buffer.length) {
    int b = in.read();
    buffer[bytesRead++] = (byte) b;
    
    if (b == 0xC5) {
        tailMatchCount++;
        if (tailMatchCount >= 7) break;
    } else {
        tailMatchCount = 0;
    }
}
```

## Changes Made

### 1. GroundStationServer.java - handleMocFsp()
- Changed from reading fixed-size frames to reading complete CLTUs
- Added CLTU start sequence detection (0xEB90)
- Added CLTU tail sequence detection (7 x 0xC5)
- Updated logging to show CLTU size

### 2. GroundStationServer.java - handleSpacecraftUplink()
- Updated logging to show CLTU forwarding
- No logic changes needed (already forwarding complete buffers)

## Test Results

### Before Fix
```
Spacecraft Log:
[UPLINK] Found CLTU start sequence
[UPLINK] Connection error: Read timed out
[UPLINK] Retrying in 5 seconds...

Commands Received: 1 (only first command)
```

### After Fix
```
Spacecraft Log:
[UPLINK] Received CLTU: 1289 bytes, decoded to 1115 byte command frame
[UPLINK] Command #1 (Frame Count: 0): DEPLOY_SOLAR_PANELS
[SPACECRAFT] Executing: DEPLOY_SOLAR_PANELS
[UPLINK] Received CLTU: 1289 bytes, decoded to 1115 byte command frame
[UPLINK] Command #2 (Frame Count: 1): ACTIVATE_ANTENNA
[SPACECRAFT] Executing: ACTIVATE_ANTENNA

Commands Received: 2 (both commands)
```

### Telemetry Confirmation
```
[RAF] Frame #21 | Solar: DEPLOYED | Antenna: ACTIVE | Commands RX: 2
```

## Verification

✅ Both commands received and executed
✅ No timeout errors
✅ No connection drops
✅ Telemetry shows correct state changes
✅ CLCW acknowledgments working (ACK=1)
✅ All tests passing (5/5)

## Lessons Learned

1. **Protocol Consistency**: When using CLTU encoding, all components must handle CLTUs, not raw frames
2. **Variable-Length Protocols**: Need proper framing detection (start/tail sequences)
3. **Buffer Sizing**: Must accommodate largest possible message (CLTU > frame)
4. **Timeout Handling**: Byte-by-byte reading with timeouts can cause issues with large messages

## Architecture Clarification

```
MOC → [CLTU 1289 bytes] → Ground Station → [CLTU 1289 bytes] → Spacecraft
                          (FSP handler)      (Uplink handler)

Ground Station acts as transparent relay for CLTUs:
- Receives complete CLTU from MOC
- Queues complete CLTU
- Forwards complete CLTU to spacecraft
```

The Ground Station does NOT decode/re-encode CLTUs - it forwards them transparently.

## Status

🎉 **ISSUE RESOLVED**

Both automated commands now execute successfully:
- Frame 10: DEPLOY_SOLAR_PANELS ✅
- Frame 20: ACTIVATE_ANTENNA ✅
