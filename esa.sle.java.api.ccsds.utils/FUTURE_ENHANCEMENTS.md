# Future Enhancements for CCSDS Utils Library

## Overview

This document outlines potential CCSDS functions that could be added to the `esa.sle.java.api.ccsds.utils` library extension based on analysis of the demo implementation and CCSDS standards.

## Currently Implemented

✅ **CLTU (Command Link Transmission Unit)**
- `CLTUEncoder` - Encode command data into CLTU format
- `CLTUDecoder` - Decode CLTU and extract command data
- `BCHEncoder` - BCH(63,56) error detection

## Identified Candidates from Demo Code

### 1. CRC Calculation (HIGH PRIORITY)

**Current State**: Duplicated in both `TelemetryFrame.java` and `CommandFrame.java`

**Proposed Module**: `esa.sle.ccsds.utils.crc`

**Classes**:
```java
package esa.sle.ccsds.utils.crc;

public class CRC16Calculator {
    /**
     * Calculate CRC-16-CCITT (polynomial 0x1021)
     * Used for CCSDS Frame Error Control Field (FECF)
     */
    public static int calculate(byte[] data);
    
    /**
     * Verify CRC-16 of frame data
     */
    public static boolean verify(byte[] frameData, int expectedCrc);
}

public class CRC32Calculator {
    /**
     * Calculate CRC-32 (polynomial 0x04C11DB7)
     * Used for CCSDS packet error control
     */
    public static long calculate(byte[] data);
    
    public static boolean verify(byte[] data, long expectedCrc);
}
```

**Benefits**:
- Eliminates code duplication
- Standard implementation
- Optimized performance
- Consistent across all projects

**CCSDS Reference**: CCSDS 131.0-B-3 (TM Synchronization and Channel Coding)

---

### 2. CLCW (Communications Link Control Word) (HIGH PRIORITY)

**Current State**: Manually encoded/decoded in `TelemetryFrame.java` and `MOCClient.java`

**Proposed Module**: `esa.sle.ccsds.utils.clcw`

**Classes**:
```java
package esa.sle.ccsds.utils.clcw;

public class CLCWEncoder {
    /**
     * Build CLCW (32-bit word) for OCF
     */
    public static int encode(CLCWBuilder builder);
    
    public static class CLCWBuilder {
        private int vcid;
        private int reportValue;
        private boolean lockout;
        private boolean wait;
        private boolean retransmit;
        private int farmBCounter;
        
        public CLCWBuilder setVirtualChannelId(int vcid);
        public CLCWBuilder setReportValue(int value);
        public CLCWBuilder setLockout(boolean lockout);
        public CLCWBuilder setWait(boolean wait);
        public CLCWBuilder setRetransmit(boolean retransmit);
        public CLCWBuilder setFarmBCounter(int counter);
        public int build();
    }
}

public class CLCWDecoder {
    /**
     * Decode CLCW from 32-bit word
     */
    public static CLCW decode(int clcwWord);
    
    public static class CLCW {
        private final int type;
        private final int version;
        private final int statusField;
        private final int copInEffect;
        private final int vcid;
        private final boolean noRfAvailable;
        private final boolean noBitLock;
        private final boolean lockout;
        private final boolean wait;
        private final boolean retransmit;
        private final int farmBCounter;
        private final int reportValue;
        
        // Getters...
    }
}
```

**Benefits**:
- Proper CLCW structure handling
- Type-safe builder pattern
- Clear field extraction
- Supports all CLCW fields (not just report value)

**CCSDS Reference**: CCSDS 232.0-B-3 (TC Space Data Link Protocol)

---

### 3. Frame Builders (MEDIUM PRIORITY)

**Current State**: Frame construction logic embedded in `TelemetryFrame.java` and `CommandFrame.java`

**Proposed Module**: `esa.sle.ccsds.utils.frames`

**Classes**:
```java
package esa.sle.ccsds.utils.frames;

public class TelemetryFrameBuilder {
    /**
     * Build CCSDS AOS Telemetry Transfer Frame
     */
    public static byte[] build(TelemetryFrameConfig config);
    
    public static class TelemetryFrameConfig {
        private int spacecraftId;
        private int virtualChannelId;
        private int frameCount;
        private byte[] payload;
        private Integer clcw;  // Optional OCF
        private boolean includeOcf = true;
        
        // Builder methods...
    }
}

public class CommandFrameBuilder {
    /**
     * Build CCSDS AOS Forward Frame (telecommand)
     */
    public static byte[] build(CommandFrameConfig config);
    
    public static class CommandFrameConfig {
        private int spacecraftId;
        private int virtualChannelId;
        private int frameCount;
        private byte[] commandData;
        
        // Builder methods...
    }
}

public class FrameParser {
    /**
     * Parse CCSDS frame header
     */
    public static FrameHeader parseHeader(byte[] frameData);
    
    public static class FrameHeader {
        private final int version;
        private final int spacecraftId;
        private final int virtualChannelId;
        private final int frameCount;
        private final boolean ocfPresent;
        
        // Getters...
    }
}
```

**Benefits**:
- Reusable frame construction
- Consistent frame format
- Easier to maintain
- Supports multiple frame types

**CCSDS Reference**: CCSDS 732.0-B-3 (AOS Space Data Link Protocol)

---

### 4. Randomization/Derandomization (MEDIUM PRIORITY)

**Current State**: Not implemented in demo

**Proposed Module**: `esa.sle.ccsds.utils.randomization`

**Classes**:
```java
package esa.sle.ccsds.utils.randomization;

public class PseudoRandomizer {
    /**
     * Apply pseudo-randomization for spectral shaping
     * Uses CCSDS standard polynomial: 1 + x^3 + x^5 + x^7 + x^8
     */
    public static byte[] randomize(byte[] data);
    
    /**
     * Remove pseudo-randomization
     */
    public static byte[] derandomize(byte[] data);
}
```

**Benefits**:
- Spectral shaping for RF transmission
- Standard CCSDS implementation
- Improves signal quality

**CCSDS Reference**: CCSDS 131.0-B-3 (TM Synchronization and Channel Coding)

---

### 5. Reed-Solomon Encoding (LOW PRIORITY - Complex)

**Current State**: Not implemented

**Proposed Module**: `esa.sle.ccsds.utils.fec`

**Classes**:
```java
package esa.sle.ccsds.utils.fec;

public class ReedSolomonEncoder {
    /**
     * Reed-Solomon (255,223) encoding for error correction
     */
    public static byte[] encode(byte[] data);
}

public class ReedSolomonDecoder {
    /**
     * Reed-Solomon (255,223) decoding with error correction
     */
    public static byte[] decode(byte[] encodedData) throws FECException;
}
```

**Benefits**:
- Forward error correction
- Improves link reliability
- Standard for deep space missions

**CCSDS Reference**: CCSDS 131.0-B-3 (TM Synchronization and Channel Coding)

**Note**: Complex implementation, may want to use existing library (e.g., Apache Commons Codec)

---

### 6. Turbo Coding (LOW PRIORITY - Very Complex)

**Current State**: Not implemented

**Proposed Module**: `esa.sle.ccsds.utils.turbo`

**Classes**:
```java
package esa.sle.ccsds.utils.turbo;

public class TurboEncoder {
    /**
     * CCSDS Turbo encoding for high-performance error correction
     */
    public static byte[] encode(byte[] data, int rate);
}

public class TurboDecoder {
    /**
     * CCSDS Turbo decoding
     */
    public static byte[] decode(byte[] encodedData) throws FECException;
}
```

**Benefits**:
- Best error correction performance
- Used in modern space missions
- Supports high data rates

**CCSDS Reference**: CCSDS 131.0-B-3 (TM Synchronization and Channel Coding)

**Note**: Very complex, typically requires hardware acceleration

---

### 7. Time Code Utilities (MEDIUM PRIORITY)

**Current State**: Not implemented

**Proposed Module**: `esa.sle.ccsds.utils.time`

**Classes**:
```java
package esa.sle.ccsds.utils.time;

public class CUCTimeEncoder {
    /**
     * CCSDS Unsegmented Time Code (CUC) encoding
     */
    public static byte[] encode(Instant timestamp, int coarseBytes, int fineBytes);
}

public class CUCTimeDecoder {
    /**
     * CCSDS Unsegmented Time Code (CUC) decoding
     */
    public static Instant decode(byte[] cucTime, int coarseBytes, int fineBytes);
}

public class CDSTimeEncoder {
    /**
     * CCSDS Day Segmented Time Code (CDS) encoding
     */
    public static byte[] encode(Instant timestamp);
}

public class CDSTimeDecoder {
    /**
     * CCSDS Day Segmented Time Code (CDS) decoding
     */
    public static Instant decode(byte[] cdsTime);
}
```

**Benefits**:
- Standard time representation
- Compact encoding
- Supports multiple formats

**CCSDS Reference**: CCSDS 301.0-B-4 (Time Code Formats)

---

### 8. Space Packet Protocol (MEDIUM PRIORITY)

**Current State**: Not implemented

**Proposed Module**: `esa.sle.ccsds.utils.packets`

**Classes**:
```java
package esa.sle.ccsds.utils.packets;

public class SpacePacketBuilder {
    /**
     * Build CCSDS Space Packet
     */
    public static byte[] build(SpacePacketConfig config);
    
    public static class SpacePacketConfig {
        private int apid;
        private int sequenceCount;
        private boolean segmentation;
        private byte[] data;
        
        // Builder methods...
    }
}

public class SpacePacketParser {
    /**
     * Parse CCSDS Space Packet
     */
    public static SpacePacket parse(byte[] packetData);
    
    public static class SpacePacket {
        private final int version;
        private final int type;
        private final boolean secondaryHeaderFlag;
        private final int apid;
        private final int sequenceFlags;
        private final int sequenceCount;
        private final int dataLength;
        private final byte[] data;
        
        // Getters...
    }
}
```

**Benefits**:
- Standard packet format
- Application-level data handling
- Multiplexing support

**CCSDS Reference**: CCSDS 133.0-B-2 (Space Packet Protocol)

---

## Recommended Implementation Priority

### Phase 1: High Priority (Immediate Value)
1. **CRC Calculation** - Eliminates duplication, widely used
2. **CLCW Utilities** - Improves command acknowledgment handling

### Phase 2: Medium Priority (Enhanced Functionality)
3. **Frame Builders** - Simplifies frame construction
4. **Randomization** - Improves RF transmission quality
5. **Time Code Utilities** - Standard time representation
6. **Space Packet Protocol** - Application-level data handling

### Phase 3: Low Priority (Advanced Features)
7. **Reed-Solomon** - Error correction (consider existing libraries)
8. **Turbo Coding** - Advanced error correction (very complex)

---

## Implementation Guidelines

### Design Principles
1. **Stateless Utilities**: All classes should be stateless with static methods
2. **No Dependencies**: Avoid dependencies on other SLE modules
3. **Builder Pattern**: Use builders for complex configurations
4. **Immutable Results**: Return immutable objects where possible
5. **Exception Handling**: Use checked exceptions for validation errors
6. **Documentation**: Include CCSDS references in Javadoc

### Testing Requirements
1. Unit tests for all utilities
2. Test vectors from CCSDS standards
3. Performance benchmarks
4. Integration tests with demo

### Documentation Requirements
1. Javadoc with CCSDS references
2. Usage examples
3. Performance characteristics
4. Limitations and assumptions

---

## Usage Example (Future)

```java
// CRC Calculation
import esa.sle.ccsds.utils.crc.CRC16Calculator;
int crc = CRC16Calculator.calculate(frameData);

// CLCW Encoding
import esa.sle.ccsds.utils.clcw.CLCWEncoder;
int clcw = CLCWEncoder.builder()
    .setVirtualChannelId(0)
    .setReportValue(5)
    .build();

// Frame Building
import esa.sle.ccsds.utils.frames.TelemetryFrameBuilder;
byte[] frame = TelemetryFrameBuilder.builder()
    .setSpacecraftId(185)
    .setVirtualChannelId(0)
    .setFrameCount(10)
    .setPayload(data)
    .setCLCW(clcw)
    .build();

// Randomization
import esa.sle.ccsds.utils.randomization.PseudoRandomizer;
byte[] randomized = PseudoRandomizer.randomize(frame);

// Time Encoding
import esa.sle.ccsds.utils.time.CUCTimeEncoder;
byte[] timeCode = CUCTimeEncoder.encode(Instant.now(), 4, 3);
```

---

## Benefits of Library Extension

### For Demo
- Cleaner code
- Less duplication
- Easier maintenance
- Better testing

### For Users
- Standard implementations
- Reusable utilities
- Consistent behavior
- Well-documented

### For Projects
- Faster development
- Fewer bugs
- Standard compliance
- Community contributions

---

## Next Steps

1. **Prioritize**: Decide which utilities to implement first
2. **Design**: Create detailed API designs
3. **Implement**: Develop and test utilities
4. **Refactor**: Update demo to use new utilities
5. **Document**: Create comprehensive documentation
6. **Release**: Version and publish library

---

## CCSDS Standards References

- **CCSDS 131.0-B-3**: TM Synchronization and Channel Coding
- **CCSDS 133.0-B-2**: Space Packet Protocol
- **CCSDS 232.0-B-3**: TC Space Data Link Protocol
- **CCSDS 301.0-B-4**: Time Code Formats
- **CCSDS 732.0-B-3**: AOS Space Data Link Protocol

---

**Status**: Planning Document

**Last Updated**: 2026-01-31

**Maintainer**: SLE Java API Team
