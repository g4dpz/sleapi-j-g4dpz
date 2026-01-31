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

### Pseudo-Randomization

Pseudo-randomization for spectral shaping of CCSDS data streams as specified in CCSDS 131.0-B-3.

**Classes:**
- `PseudoRandomizer` - Apply/remove pseudo-randomization

**Features:**
- CCSDS polynomial: 1 + x^3 + x^5 + x^7 + x^8
- Self-synchronizing scrambler
- Randomize/derandomize operations
- In-place processing option
- Sequence generation

**Usage Example:**
```java
import esa.sle.ccsds.utils.randomization.PseudoRandomizer;

// Randomize data (creates new array)
byte[] randomized = PseudoRandomizer.randomize(frameData);

// Derandomize data (self-synchronizing, same operation)
byte[] original = PseudoRandomizer.derandomize(randomized);

// In-place processing (no memory allocation)
PseudoRandomizer.randomizeInPlace(frameData);
PseudoRandomizer.derandomizeInPlace(frameData);

// Generate pseudo-random sequence
byte[] sequence = PseudoRandomizer.generateSequence(1024);
```

### Frame Header Parser

Frame header parsing utilities for CCSDS Transfer Frames as specified in CCSDS 732.0-B-3.

**Classes:**
- `FrameHeaderParser` - Parse CCSDS frame headers
- `FrameHeader` - Immutable header object with all fields

**Features:**
- Parse complete frame header (6 bytes)
- Extract all header fields
- Quick extraction methods (SCID, VCID, frame count)
- Frame validation
- Support for TM and TC frames

**Usage Example:**
```java
import esa.sle.ccsds.utils.frames.*;

// Parse complete header
FrameHeader header = FrameHeaderParser.parse(frameData);
System.out.println("SCID: " + header.getSpacecraftId());
System.out.println("VCID: " + header.getVirtualChannelId());
System.out.println("Frame Count: " + header.getFrameCount());
System.out.println("OCF Present: " + header.isOcfPresent());
System.out.println("Type: " + (header.isCommandFrame() ? "CMD" : "TM"));

// Quick extraction (without full parsing)
int scid = FrameHeaderParser.extractSpacecraftId(frameData);
int vcid = FrameHeaderParser.extractVirtualChannelId(frameData);
int count = FrameHeaderParser.extractFrameCount(frameData);

// Validate frame
boolean valid = FrameHeaderParser.isValidFrame(frameData);
```

### CRC (Cyclic Redundancy Check)

CRC-16-CCITT calculation for Frame Error Control Field (FECF) as specified in CCSDS 131.0-B-3.

**Classes:**
- `CRC16Calculator` - Calculate and verify CRC-16-CCITT (polynomial 0x1021)

**Features:**
- CRC-16-CCITT with polynomial 0x1021
- Initial value 0xFFFF
- Calculate CRC for data
- Verify CRC against expected value
- Append CRC to data
- Verify appended CRC

**Usage Example:**
```java
import esa.sle.ccsds.utils.crc.CRC16Calculator;

// Calculate CRC for frame data
byte[] frameData = ...; // Frame without FECF
int crc = CRC16Calculator.calculate(frameData);

// Verify CRC
boolean valid = CRC16Calculator.verify(frameData, expectedCrc);

// Append CRC to data
byte[] frameWithCrc = CRC16Calculator.appendCrc(frameData);

// Verify appended CRC
boolean valid = CRC16Calculator.verifyAppended(frameWithCrc);
```

### CLCW (Communications Link Control Word)

CLCW encoding/decoding for command acknowledgment in the Operational Control Field (OCF) as specified in CCSDS 232.0-B-3.

**Classes:**
- `CLCWEncoder` - Build CLCW for OCF
- `CLCWDecoder` - Decode CLCW from OCF

**Features:**
- Simple encoding with VCID and Report Value
- Advanced encoding with all CLCW fields
- Decode all CLCW fields
- Check nominal status
- Builder pattern for complex CLCW

**Usage Example:**
```java
import esa.sle.ccsds.utils.clcw.*;

// Simple encoding (most common use case)
int clcw = CLCWEncoder.encode(virtualChannelId, lastCommandFrameCount);

// Advanced encoding with all fields
int clcw = CLCWEncoder.builder()
    .setVirtualChannelId(0)
    .setReportValue(5)
    .setLockout(false)
    .setWait(false)
    .build();

// Decode CLCW
CLCWDecoder.CLCW decoded = CLCWDecoder.decode(clcwWord);
System.out.println("Report Value: " + decoded.getReportValue());
System.out.println("VCID: " + decoded.getVirtualChannelId());
System.out.println("Nominal: " + decoded.isNominal());
```

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
3. **Frame Parsers** - Helper classes for parsing CCSDS frame headers
4. **Randomization** - Pseudo-randomization for spectral shaping
5. **Reed-Solomon** - Forward error correction encoding/decoding
6. **Time Code Utilities** - CUC and CDS time code encoding/decoding
7. **Space Packet Protocol** - CCSDS Space Packet building and parsing
