# System Architecture

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         SPACE SEGMENT                                   │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                    SPACECRAFT SIMULATOR                          │  │
│  │                                                                  │  │
│  │  ┌────────────┐    ┌──────────────┐    ┌─────────────────┐    │  │
│  │  │  Sensors   │───▶│   Telemetry  │───▶│  Frame Builder  │    │  │
│  │  │ Generator  │    │   Processor  │    │   (CCSDS TM)    │    │  │
│  │  └────────────┘    └──────────────┘    └─────────────────┘    │  │
│  │        │                                         │              │  │
│  │        │ Temperature, Voltage, Current,          │              │  │
│  │        │ Attitude, Altitude, Status              │              │  │
│  │        │                                         ▼              │  │
│  │        │                              ┌─────────────────┐      │  │
│  │        └─────────────────────────────▶│  TCP Transmit   │      │  │
│  │                                       │   Port: 5555    │      │  │
│  │                                       └─────────────────┘      │  │
│  └──────────────────────────────────────────────┬───────────────────  │
└─────────────────────────────────────────────────┼───────────────────────┘
                                                   │
                                                   │ TCP/IP
                                                   │ 1115 bytes/frame
                                                   │ 1 Hz
                                                   ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        GROUND SEGMENT                                   │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                  GROUND STATION SERVER                           │  │
│  │                                                                  │  │
│  │  ┌─────────────────┐         ┌──────────────┐                  │  │
│  │  │ Spacecraft RX   │────────▶│ Frame Queue  │                  │  │
│  │  │  Port: 5555     │         │ (1000 frames)│                  │  │
│  │  └─────────────────┘         └──────────────┘                  │  │
│  │                                      │                          │  │
│  │                                      │ FIFO                     │  │
│  │                                      ▼                          │  │
│  │                              ┌──────────────┐                  │  │
│  │                              │ SLE RAF      │                  │  │
│  │                              │ Service      │                  │  │
│  │                              │ Port: 5556   │                  │  │
│  │                              └──────────────┘                  │  │
│  └──────────────────────────────────────┬───────────────────────────  │
└─────────────────────────────────────────┼───────────────────────────────┘
                                           │
                                           │ SLE RAF
                                           │ (Simplified)
                                           ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    MISSION OPERATIONS CENTER                            │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                        MOC CLIENT                                │  │
│  │                                                                  │  │
│  │  ┌─────────────────┐         ┌──────────────┐                  │  │
│  │  │  SLE RAF RX     │────────▶│ Frame Parser │                  │  │
│  │  │  Port: 5556     │         │   (Header)   │                  │  │
│  │  └─────────────────┘         └──────────────┘                  │  │
│  │                                      │                          │  │
│  │                                      ▼                          │  │
│  │                              ┌──────────────┐                  │  │
│  │                              │  Telemetry   │                  │  │
│  │                              │   Decoder    │                  │  │
│  │                              └──────────────┘                  │  │
│  │                                      │                          │  │
│  │                                      ▼                          │  │
│  │                              ┌──────────────┐                  │  │
│  │                              │   Display    │                  │  │
│  │                              │  Statistics  │                  │  │
│  │                              └──────────────┘                  │  │
│  └──────────────────────────────────────────────────────────────────  │
└─────────────────────────────────────────────────────────────────────────┘
```

## Component Details

### Spacecraft Simulator

```
┌─────────────────────────────────────────┐
│      SpacecraftSimulator.java           │
├─────────────────────────────────────────┤
│ - Spacecraft ID: 185                    │
│ - Virtual Channel: 0                    │
│ - Frame Rate: 1 Hz                      │
├─────────────────────────────────────────┤
│ Methods:                                │
│ • start()          - Main loop          │
│ • generateFrame()  - Create TM frame    │
│ • generateTelemetryData() - Sensors     │
│ • stop()           - Shutdown           │
└─────────────────────────────────────────┘
```

### Ground Station Server

```
┌─────────────────────────────────────────┐
│     GroundStationServer.java            │
├─────────────────────────────────────────┤
│ Threads:                                │
│ 1. Spacecraft Receiver (Port 5555)     │
│ 2. SLE Service Provider (Port 5556)    │
├─────────────────────────────────────────┤
│ Data Structure:                         │
│ • BlockingQueue<byte[]> frameQueue      │
│   - Capacity: 1000 frames               │
│   - Thread-safe FIFO                    │
├─────────────────────────────────────────┤
│ Methods:                                │
│ • start()                - Init server  │
│ • receiveFromSpacecraft() - RX thread   │
│ • serveSLE()             - TX thread    │
│ • stop()                 - Shutdown     │
└─────────────────────────────────────────┘
```

### MOC Client

```
┌─────────────────────────────────────────┐
│          MOCClient.java                 │
├─────────────────────────────────────────┤
│ Statistics:                             │
│ • Frames Received                       │
│ • Data Volume (KB/MB)                   │
│ • Average Frame Size                    │
├─────────────────────────────────────────┤
│ Methods:                                │
│ • start()          - Connect & receive  │
│ • processFrame()   - Parse & display    │
│ • printStatistics() - Show summary      │
│ • stop()           - Shutdown           │
└─────────────────────────────────────────┘
```

## Data Flow Sequence

```
Time │ Spacecraft          │ Ground Station      │ MOC
─────┼─────────────────────┼─────────────────────┼──────────────────
T+0s │ Generate Frame #0   │                     │
     │ Send to GS ────────▶│ Receive Frame #0    │
     │                     │ Queue Frame         │
     │                     │ Forward to MOC ────▶│ Receive Frame #0
     │                     │                     │ Parse Header
     │                     │                     │ Decode Telemetry
     │                     │                     │ Display Data
─────┼─────────────────────┼─────────────────────┼──────────────────
T+1s │ Generate Frame #1   │                     │
     │ Send to GS ────────▶│ Receive Frame #1    │
     │                     │ Queue Frame         │
     │                     │ Forward to MOC ────▶│ Receive Frame #1
     │                     │                     │ Parse & Display
─────┼─────────────────────┼─────────────────────┼──────────────────
T+2s │ Generate Frame #2   │                     │
     │ Send to GS ────────▶│ Receive Frame #2    │
     │                     │ Queue Frame         │
     │                     │ Forward to MOC ────▶│ Receive Frame #2
     │                     │                     │ Parse & Display
```

## Frame Structure

```
┌────────────────────────────────────────────────────────────────┐
│                    CCSDS TM Transfer Frame                     │
│                        (1115 bytes)                            │
├────────────────────────────────────────────────────────────────┤
│ PRIMARY HEADER (6 bytes)                                       │
├────────────────────────────────────────────────────────────────┤
│ Byte 0-1: Version (2b) | SCID (10b) | VCID (3b) | OCF (1b)   │
│ Byte 2:   Master Channel Frame Count (8b)                     │
│ Byte 3:   Virtual Channel Frame Count (8b)                    │
│ Byte 4-5: Transfer Frame Data Field Status (16b)              │
├────────────────────────────────────────────────────────────────┤
│ DATA FIELD (1109 bytes)                                        │
├────────────────────────────────────────────────────────────────┤
│ Byte 6-13:   Timestamp (long, 8 bytes)                        │
│ Byte 14-17:  Temperature (float, 4 bytes)                     │
│ Byte 18-21:  Voltage (float, 4 bytes)                         │
│ Byte 22-25:  Current (float, 4 bytes)                         │
│ Byte 26-29:  Attitude (int, 4 bytes)                          │
│ Byte 30-33:  Altitude (int, 4 bytes)                          │
│ Byte 34:     Solar Panels Status (1 byte)                     │
│ Byte 35:     Antenna Status (1 byte)                          │
│ Byte 36:     System Status (1 byte)                           │
│ Byte 37-1114: Additional data / padding                       │
└────────────────────────────────────────────────────────────────┘
```

## Network Topology

```
┌──────────────┐
│  Spacecraft  │
│  Simulator   │
│              │
│ 127.0.0.1    │
└──────┬───────┘
       │
       │ TCP Connection
       │ Port 5555
       │
       ▼
┌──────────────┐
│   Ground     │
│   Station    │
│   Server     │
│              │
│ 127.0.0.1    │
│ Port 5555 RX │
│ Port 5556 TX │
└──────┬───────┘
       │
       │ TCP Connection
       │ Port 5556
       │
       ▼
┌──────────────┐
│     MOC      │
│    Client    │
│              │
│ 127.0.0.1    │
└──────────────┘
```

## Thread Model

### Spacecraft Simulator
```
Main Thread
  └─▶ Frame Generation Loop
       ├─▶ Generate Telemetry Data
       ├─▶ Build CCSDS Frame
       ├─▶ Send to Ground Station
       └─▶ Sleep 1 second
```

### Ground Station Server
```
Main Thread
  ├─▶ Spacecraft Receiver Thread
  │    └─▶ Accept Connection Loop
  │         ├─▶ Read Frame (1115 bytes)
  │         ├─▶ Queue Frame
  │         └─▶ Repeat
  │
  └─▶ SLE Service Thread
       └─▶ Accept Connection Loop
            ├─▶ Dequeue Frame (blocking)
            ├─▶ Send to MOC
            └─▶ Repeat
```

### MOC Client
```
Main Thread
  └─▶ Frame Reception Loop
       ├─▶ Read Frame (1115 bytes)
       ├─▶ Parse Header
       ├─▶ Decode Telemetry
       ├─▶ Display Data
       ├─▶ Update Statistics
       └─▶ Repeat
```

## Error Handling

```
┌─────────────────┐
│ Connection Lost │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Log Error       │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Wait 5 seconds  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Retry Connect   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Success?        │
├─────────────────┤
│ Yes │ No        │
│  │  └───────────┼─▶ Repeat
│  ▼              │
│ Resume          │
└─────────────────┘
```

## Scalability Considerations

### Current Design
- Single spacecraft
- Single ground station
- Single MOC
- 1 Hz frame rate
- ~1.1 KB/s throughput

### Potential Scaling
- Multiple spacecraft (different ports)
- Multiple ground stations (load balancing)
- Multiple MOCs (broadcast)
- Higher frame rates (up to 1000 Hz)
- Larger frames (up to 8920 bytes)

## Performance Metrics

| Component | CPU | Memory | Network |
|-----------|-----|--------|---------|
| Spacecraft | <1% | ~10 MB | 1.1 KB/s TX |
| Ground Station | <1% | ~10 MB | 1.1 KB/s RX + 1.1 KB/s TX |
| MOC | <1% | ~10 MB | 1.1 KB/s RX |

## Future Architecture Enhancements

1. **Add SLE Protocol Layer**
   ```
   MOC ←→ [SLE Protocol] ←→ Ground Station
   ```

2. **Add Authentication**
   ```
   MOC ←→ [ISP1 Credentials] ←→ Ground Station
   ```

3. **Add Multiple Services**
   ```
   MOC ←→ [RAF/RCF/ROCF] ←→ Ground Station
   ```

4. **Add Redundancy**
   ```
           ┌─ Ground Station 1 ─┐
   MOC ←──┤                     ├──→ Spacecraft
           └─ Ground Station 2 ─┘
   ```

This architecture provides a solid foundation for understanding and extending SLE-based telemetry systems.
