# SLE Java API - CCSDS Utilities

This module provides utility classes for CCSDS physical layer encoding/decoding operations that complement the SLE Java API library.

## Overview

The SLE Java API library handles the SLE protocol layer (BIND, START, TRANSFER-DATA, etc.) and ASN.1 encoding/decoding. This utilities module provides the physical and link layer functionality needed for complete CCSDS implementations.

## Components

### CLTU (Command Link Transmission Unit)

Physical layer encoding for uplink commands as specified in CCSDS 231.0-B-3.

**Classes:**
- `CLTUEncoder` - Encode command data into CLTU format
- `CLTUDecoder` - Decode CLTU and extract command data
- `BCHEncoder` - BCH(63,56) error detection code
- `CLTUException` - Exception for CLTU operations

**Features:**
- Start sequence (0xEB90) for synchronization
- Code blocks with 7 data bytes + 1 BCH parity byte
- Tail sequence (0xC5C5C5C5C5C5C5) for framing
- BCH error detection
- Fill byte handling (0x55)

**Usage Example:**
```java
import esa.sle.ccsds.utils.cltu.*;

// Encode command frame into CLTU
byte[] commandFrame = ...; // Your command frame data
byte[] cltu = CLTUEncoder.encode(commandFrame);

// Decode CLTU to extract command frame
try {
    byte[] extractedCommand = CLTUDecoder.decode(cltu);
} catch (CLTUException e) {
    // Handle invalid CLTU or BCH parity error
}

// Calculate CLTU size
int cltuSize = CLTUEncoder.calculateCLTUSize(commandFrame.length);
int codeBlocks = CLTUEncoder.getCodeBlockCount(commandFrame.length);
```

### CCSDS Frames (Future)

Utilities for building and parsing CCSDS Transfer Frames:
- Telemetry Transfer Frames (TM)
- Telecommand Transfer Frames (TC)
- AOS Transfer Frames

### CLCW (Future)

Command Link Control Word encoding/decoding for command acknowledgment in the Operational Control Field (OCF).

### CRC (Future)

CRC-16-CCITT calculation for Frame Error Control Field (FECF).

## Integration with SLE Java API

This module is designed to work alongside the SLE Java API:

1. **Uplink Path (MOC → Spacecraft)**:
   - Application creates command frame
   - `CLTUEncoder` wraps frame in CLTU format
   - SLE CLTU service transmits CLTU via `CLTU-TRANSFER-DATA` operation
   - Ground station forwards CLTU to spacecraft
   - Spacecraft uses `CLTUDecoder` to extract command frame

2. **Downlink Path (Spacecraft → MOC)**:
   - Spacecraft generates telemetry frames
   - Ground station receives frames
   - SLE RAF/RCF service delivers frames via `RAF-TRANSFER-DATA` operation
   - MOC receives and processes frames

## Dependencies

This module has no dependencies on other SLE modules - it's a standalone utility library that can be used independently or with the SLE Java API.

## CCSDS References

- **CCSDS 231.0-B-3**: TC Synchronization and Channel Coding (CLTU)
- **CCSDS 232.0-B-3**: TC Space Data Link Protocol
- **CCSDS 132.0-B-2**: TM Space Data Link Protocol

## Version

5.1.6 - Matches SLE Java API version

## License

Same as SLE Java API parent project.

## Contributing

This module extends the SLE Java API with physical layer utilities. Contributions should:
- Follow CCSDS specifications
- Include unit tests
- Maintain compatibility with Java 17+
- Follow existing code style

## Future Enhancements

1. **Full BCH(63,56) Implementation** - Use proper BCH polynomial for error correction
2. **Frame Builders** - Helper classes for constructing CCSDS frames
3. **CLCW Utilities** - Encode/decode Command Link Control Word
4. **CRC Utilities** - CRC-16-CCITT for FECF
5. **Randomization** - Pseudo-randomization for spectral shaping
6. **Reed-Solomon** - Forward error correction encoding/decoding
