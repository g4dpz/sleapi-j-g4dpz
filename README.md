# SLE Java API

The SLE Java API package provides a complete implementation of CCSDS Space Link Extension (SLE) services for spacecraft ground systems.

## Overview

This repository contains:
- **SLE API Core**: Implementation of CCSDS SLE transfer services
- **CCSDS Utilities Library**: Physical and link layer utilities for CCSDS protocols
- **Demo Application**: Bidirectional spacecraft communication demonstration

## SLE Transfer Services

The SLE API supports the following transfer services:
- **RAF** (Return All Frames) - CCSDS 911.1-B-4
- **RCF** (Return Channel Frames) - CCSDS 911.2-B-3
- **ROCF** (Return Operational Control Fields) - CCSDS 911.5-B-2
- **CLTU** (Forward CLTU) - CCSDS 912.1-B-4
- **FSP** (Forward Space Packet) - CCSDS 912.3-B-3

## CCSDS Utilities Library

The `esa.sle.java.api.ccsds.utils` module provides CCSDS-compliant utilities:

### Physical Layer
- **CLTU Encoding/Decoding** - TC Synchronization and Channel Coding (CCSDS 231.0-B-3)
- **Pseudo-Randomization** - Spectral shaping for RF transmission (CCSDS 131.0-B-3)

### Link Layer
- **CRC-16 Calculator** - Frame Error Control Field (CCSDS 131.0-B-3)
- **CLCW Encoder/Decoder** - Command Link Control Word (CCSDS 232.0-B-3)
- **Frame Header Parser** - Parse TM/TC frame headers (CCSDS 732.0-B-3, 232.0-B-3)
- **Frame Builders** - Build complete TM/TC frames (CCSDS 732.0-B-3, 232.0-B-3)

### Time Representation
- **CUC Time Codes** - CCSDS Unsegmented Time Code (CCSDS 301.0-B-4)
- **CDS Time Codes** - CCSDS Day Segmented Time Code (CCSDS 301.0-B-4)

### Application Layer
- **Space Packet Protocol** - Build and parse CCSDS Space Packets (CCSDS 133.0-B-2)

See [CCSDS Utilities README](esa.sle.java.api.ccsds.utils/README.md) for detailed documentation.

## Demo Application

The demo application demonstrates bidirectional spacecraft communication:
- **Spacecraft Simulator**: Sends telemetry frames, receives and executes commands
- **Ground Station**: Forwards data between spacecraft and MOC
- **Mission Operations Center (MOC)**: Receives telemetry, sends commands

See [Demo README](demo/README.md) for usage instructions.

## Prerequisites

- **Java**: OpenJDK 17 or later
- **Maven**: 3.8.5 or later
- **OS**: Linux, macOS, or Windows

## Build

### Standard Maven Build (Recommended)

Build the entire project using standard Maven:

```bash
mvn clean install
```

Build without tests:

```bash
mvn clean install -DskipTests
```

Or use the convenience script:

```bash
./build-maven.sh
```

This builds:
1. CCSDS Utilities Library
2. SLE API Core
3. Demo Application

All artifacts are installed to your local Maven repository (`~/.m2/repository`).

### Legacy Tycho Build

For Eclipse plugin development, the legacy Tycho build is still available:

```bash
mvn -f pom-tycho.xml clean install
```

## Quick Start

1. **Build the project**:
   ```bash
   mvn clean install
   ```

2. **Run the demo**:
   ```bash
   cd demo
   ./test-demo.sh  # Linux/macOS
   # or
   test-demo.bat   # Windows
   ```

3. **View results**: The demo runs for 30 seconds and displays:
   - Telemetry frame flow
   - Automated command execution
   - CLCW acknowledgments
   - Test results and statistics

## Project Structure

```
sleapi-j-g4dpz/
├── esa.sle.java.api.core/          # SLE API core implementation
├── esa.sle.java.api.ccsds.utils/   # CCSDS utilities library
├── demo/                            # Demo application
├── esa.sle.java.api.core.test/     # Core tests
├── esa.sle.java.api.core.test.harness/ # Test harness
└── esa.sle.java.api.feature/       # Eclipse feature
```

## Documentation

- [Demo README](demo/README.md) - Demo application usage
- [Demo Architecture](demo/ARCHITECTURE.md) - System design
- [Demo Testing](demo/TESTING.md) - Testing guide
- [CCSDS Utilities](esa.sle.java.api.ccsds.utils/README.md) - Utilities documentation
- [CCSDS Standards Audit](esa.sle.java.api.ccsds.utils/CCSDS_STANDARDS_AUDIT.md) - Compliance verification

## Standards Compliance

This implementation follows CCSDS Blue Book standards:
- CCSDS 910.4-B-3: Cross Support Reference Model
- CCSDS 911.x: Return Link Services (RAF, RCF, ROCF)
- CCSDS 912.x: Forward Link Services (CLTU, FSP)
- CCSDS 131.0-B-3: TM Synchronization and Channel Coding
- CCSDS 231.0-B-3: TC Synchronization and Channel Coding
- CCSDS 232.0-B-3: TC Space Data Link Protocol
- CCSDS 301.0-B-4: Time Code Formats
- CCSDS 732.0-B-3: AOS Space Data Link Protocol
- CCSDS 133.0-B-2: Space Packet Protocol

## License

See [LICENSE](LICENSE) file for details.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidelines.

## Version

5.1.6

## Authors

ESA SLE Java API Team

