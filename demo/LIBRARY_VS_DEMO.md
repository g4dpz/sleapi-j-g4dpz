# SLE Java API Library vs Demo Implementation

## What the Library Provides

The SLE Java API library implements the **CCSDS Space Link Extension (SLE) services** at the protocol level:

### 1. SLE Services (Protocol Layer)
- **RAF** (Return All Frames) - Downlink telemetry service
- **RCF** (Return Channel Frames) - Filtered downlink service  
- **ROCF** (Return Operational Control Field) - OCF-only service
- **CLTU** (Command Link Transmission Unit) - Uplink command service
- **FSP** (Forward Space Packet) - Packet-based uplink service

### 2. SLE Operations
Each service provides operations like:
- `BIND` / `UNBIND` - Establish/terminate service connection
- `START` / `STOP` - Begin/end data transfer
- `TRANSFER-DATA` - Send/receive data
- `GET-PARAMETER` - Query service parameters
- `STATUS-REPORT` - Get service status
- `ASYNC-NOTIFY` / `SYNC-NOTIFY` - Event notifications

### 3. ASN.1 Encoding/Decoding
- Encodes SLE operations into ASN.1 PDUs
- Decodes received ASN.1 PDUs into operation objects
- Handles all SLE protocol versions (v1-v5)

### 4. Service Instance Management
- Configuration management
- State machines for each service type
- Parameter validation
- Credentials and authentication

### 5. What the Library Does NOT Provide

The library operates at the **SLE protocol layer**, not the **physical/link layer**:

❌ **Physical CLTU Encoding** - Start sequence, BCH code blocks, tail sequence
❌ **Frame Generation** - Creating CCSDS TM/TC frames
❌ **CLCW Encoding/Decoding** - Command Link Control Word in OCF
❌ **CRC Calculation** - Frame Error Control Field
❌ **Spacecraft/Ground Station Simulation** - End-to-end system
❌ **Socket/Network Communication** - TCP/IP connections
❌ **Data Generation** - Telemetry data, commands, etc.

## What Our Demo Implements

Our demo fills the gaps by implementing the **physical/link layer** and **application layer**:

### 1. Physical CLTU Encoding (`CLTU.java`) ✓ NEW
```
┌─────────────┬────────────────────┬─────────────┐
│ Start: EB90 │ Code Blocks (BCH)  │ Tail: C5... │
└─────────────┴────────────────────┴─────────────┘
```
- Start sequence (0xEB90)
- BCH(63,56) error detection code blocks
- Tail sequence (0xC5C5C5C5C5C5C5)
- **This is NOT in the library** - we implemented it

### 2. CCSDS Frame Generation
**TelemetryFrame.java** - Creates 1115-byte TM frames:
- Primary header (6 bytes)
- Data field (1103 bytes)
- OCF with CLCW (4 bytes)
- FECF/CRC-16 (2 bytes)
- **This is NOT in the library** - we implemented it

**CommandFrame.java** - Creates 1115-byte TC frames:
- Primary header (6 bytes)
- Command data (1107 bytes)
- FECF/CRC-16 (2 bytes)
- **This is NOT in the library** - we implemented it

### 3. CLCW Implementation
- Encodes CLCW in OCF of telemetry frames
- Tracks last command frame count
- Provides command acknowledgment
- **This is NOT in the library** - we implemented it

### 4. End-to-End System Simulation
**SpacecraftSimulator.java**:
- Generates telemetry frames
- Receives and decodes CLTUs
- Executes commands
- Maintains spacecraft state

**GroundStationServer.java**:
- Simulates SLE services (RAF, FSP)
- Buffers and forwards frames
- Multi-threaded socket server

**MOCClient.java**:
- Receives telemetry via RAF
- Sends commands via FSP (wrapped in CLTU)
- Decodes CLCW acknowledgments
- Displays telemetry data

**All of this is NOT in the library** - we implemented it

## Why We Needed to Implement These

The SLE Java API library is designed to be used by:
- **Ground stations** that already have physical layer equipment
- **MOCs** that connect to existing ground station infrastructure
- **Test harnesses** that simulate SLE protocol behavior

The library assumes you have:
- Hardware or software that generates/receives CCSDS frames
- Physical layer encoding/decoding (CLTU, randomization, FEC)
- RF equipment or simulators

Our demo needed to simulate the **entire end-to-end system** including:
- Spacecraft generating frames
- Physical CLTU encoding for uplink
- Ground station forwarding data
- MOC processing telemetry and sending commands

## Could the Library Generate These?

### What Could Be Added to the Library:

1. **CLTU Physical Encoding** ✓ Good candidate
   - Standard CCSDS format
   - Reusable across missions
   - Well-defined specification

2. **CCSDS Frame Builders** ✓ Good candidate
   - Standard frame formats
   - Common across missions
   - Helper utilities would be useful

3. **CLCW Encoding/Decoding** ✓ Good candidate
   - Standard CCSDS format
   - Part of TC protocol
   - Useful for many users

### What Should Stay Application-Specific:

1. **Spacecraft Simulation** ❌ Too application-specific
   - Each mission has unique behavior
   - State management varies widely
   - Command execution is mission-specific

2. **Telemetry Data Generation** ❌ Too application-specific
   - Sensor data varies by spacecraft
   - Format depends on mission requirements
   - Not part of SLE standard

3. **Network Communication** ❌ Deployment-specific
   - Socket vs shared memory vs message queue
   - Network topology varies
   - Security requirements differ

## Summary Table

| Component | In Library? | In Demo? | Should Be in Library? |
|-----------|-------------|----------|----------------------|
| SLE Protocol (BIND, START, etc.) | ✓ Yes | ✗ No | Already there |
| ASN.1 Encoding/Decoding | ✓ Yes | ✗ No | Already there |
| Physical CLTU Encoding | ✗ No | ✓ Yes | ✓ Good candidate |
| CCSDS Frame Builders | ✗ No | ✓ Yes | ✓ Good candidate |
| CLCW Encoding/Decoding | ✗ No | ✓ Yes | ✓ Good candidate |
| CRC-16 Calculation | ✗ No | ✓ Yes | ✓ Good candidate |
| Spacecraft Simulator | ✗ No | ✓ Yes | ✗ Too specific |
| Ground Station Simulator | ✗ No | ✓ Yes | ✗ Too specific |
| MOC Client | ✗ No | ✓ Yes | ✗ Too specific |
| Telemetry Data Generation | ✗ No | ✓ Yes | ✗ Too specific |

## Conclusion

Our demo implementation is **complementary** to the SLE Java API library:

- **Library**: Handles SLE protocol, ASN.1 encoding, service management
- **Demo**: Handles physical layer, frame generation, end-to-end simulation

Some components we implemented (CLTU encoding, frame builders, CLCW) could potentially be added to the library as utility classes, but the simulation components (spacecraft, ground station, MOC) are intentionally application-specific and demonstrate how to use the library in a complete system.

The demo shows how to **integrate** the SLE library into a working space communication system, filling in the physical and application layers that the library intentionally leaves to the user.
