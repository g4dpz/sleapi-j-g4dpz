# CCSDS Command Link Control Word (CLCW) Implementation

## Overview

The demo now implements CCSDS Command Link Control Word (CLCW) in the Operational Control Field (OCF) of telemetry frames to provide command acknowledgment from the spacecraft back to the MOC.

## Implementation Details

### CLCW Structure (32 bits in OCF)

The CLCW is embedded in bytes 1109-1112 of each 1115-byte telemetry frame:

```
Bits 0-7:   Control Word Type, Version, Status Field, COP in Effect
Bits 8-13:  Virtual Channel ID
Bits 14-23: Flags (No RF, No Bit Lock, Lockout, Wait, Retransmit, etc.)
Bits 24-31: Report Value (last command frame count acknowledged)
```

### Components Modified

#### 1. TelemetryFrame.java
- Added `lastCommandReceived` parameter to constructor
- Builds CLCW in OCF with:
  - VCID in bits 8-13
  - Report Value (command frame count) in bits 24-31
- Spacecraft passes the last received command frame count

#### 2. SpacecraftSimulator.java
- Tracks `lastCommandFrameCount` from received commands
- Includes this value when creating telemetry frames
- Logs "CLCW ACK: X" showing the acknowledged command frame count

#### 3. MOCClient.java
- Decodes CLCW from OCF (bytes 1109-1112)
- Extracts Report Value (bits 24-31)
- Displays "CLCW_ACK=X" in telemetry output

## Test Results

From the automated test run:

```
Commands Sent: 2
  [FSP] Sent command #1: DEPLOY_SOLAR_PANELS (frame count 0)
  [FSP] Sent command #2: ACTIVATE_ANTENNA (frame count 1)

Spacecraft Received:
  [UPLINK] Received command #1 (Frame Count: 0): DEPLOY_SOLAR_PANELS
  [UPLINK] Received command #2 (Frame Count: 1): ACTIVATE_ANTENNA

CLCW Acknowledgment:
  All telemetry frames after command reception show: CLCW_ACK=1
  This confirms spacecraft acknowledged command frame count 1 (last command)
```

## Frame Structure with CLCW

```
Byte Range    | Field                          | Size
--------------|--------------------------------|------
0-1           | Header: Version+SCID+VCID+Flags| 2
2-3           | Frame Counts                   | 2
4-5           | Data Field Status              | 2
6-1108        | Data Field                     | 1103
1109-1112     | OCF (contains CLCW)            | 4
1113-1114     | FECF (CRC-16)                  | 2
```

## Benefits

1. **Real-time Acknowledgment**: MOC immediately knows which commands the spacecraft has received
2. **CCSDS Compliant**: Uses standard CCSDS CLCW structure in OCF
3. **Minimal Overhead**: No additional frames needed - acknowledgment is in every telemetry frame
4. **Reliable**: CRC-16 protects entire frame including OCF

## Usage

The CLCW acknowledgment is automatic:
- Spacecraft updates `lastCommandFrameCount` when receiving commands
- Every telemetry frame includes the current CLCW value
- MOC decodes and displays CLCW_ACK in telemetry output

No manual intervention required - the system handles command acknowledgment transparently.
