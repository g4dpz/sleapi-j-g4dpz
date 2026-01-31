# Phase 3 Proposal: Frame Builders and CLTU Stream Reader

## Overview

After completing Phases 1 and 2, there are still opportunities to refactor CCSDS-related code from the demo into reusable library utilities. This document proposes Phase 3 enhancements.

## Analysis Date

January 31, 2026

## CCSDS Standards Analysis

Before proposing refactoring, we must verify which operations are defined by CCSDS standards vs demo-specific implementations.

### CCSDS Standards Review

**Relevant Standards:**
- **CCSDS 732.0-B-3**: AOS Space Data Link Protocol (TM frames)
- **CCSDS 232.0-B-3**: TC Space Data Link Protocol (TC frames)
- **CCSDS 231.0-B-3**: TC Synchronization and Channel Coding (CLTU)

**What IS Standardized:**
- ✅ Frame header structure (6 bytes) - CCSDS 732.0-B-3 / 232.0-B-3
- ✅ OCF structure (4 bytes) - CCSDS 732.0-B-3
- ✅ FECF (CRC-16) - CCSDS 732.0-B-3 / 232.0-B-3
- ✅ CLTU structure (start/tail sequences) - CCSDS 231.0-B-3
- ✅ Frame sizes and field positions - CCSDS standards

**What is NOT Standardized:**
- ❌ Frame payload content (mission-specific)
- ❌ Data field interpretation (application-specific)
- ❌ Specific frame sizes (mission-configurable)
- ❌ Stream reading patterns (implementation-specific)

### Conclusion

**Frame Builders**: ✅ **CCSDS STANDARD** - Frame structure is fully defined by CCSDS 732.0-B-3 and 232.0-B-3
**CLTU Stream Reader**: ⚠️ **PARTIALLY STANDARD** - CLTU format is standard, but stream reading is implementation-specific
**Frame Data Extractor**: ✅ **CCSDS STANDARD** - Field positions are defined by CCSDS standards

## Identified Opportunities (CCSDS Standard Only)

### 1. Frame Builder Utilities (HIGH PRIORITY) ⭐⭐⭐ ✅ CCSDS STANDARD

**Current Problem**: Frame construction logic is embedded in `TelemetryFrame.java` and `CommandFrame.java` with significant complexity.

**CCSDS Standards**: 
- CCSDS 732.0-B-3 Section 4.1.2 (TM Transfer Frame Structure)
- CCSDS 232.0-B-3 Section 4.1.2 (TC Transfer Frame Structure)

**Code Analysis**:
- `TelemetryFrame.buildFrame()`: ~80 lines of frame construction
- `CommandFrame.buildFrame()`: ~60 lines of frame construction
- Both manually construct headers, data fields, OCF, and FECF
- Duplication of frame structure knowledge

**Proposed Solution**:
```java
package esa.sle.ccsds.utils.frames;

public class TelemetryFrameBuilder {
    /**
     * Build complete CCSDS TM Transfer Frame
     */
    public static byte[] build(TelemetryFrameConfig config);
    
    public static class TelemetryFrameConfig {
        private int spacecraftId;
        private int virtualChannelId;
        private int frameCount;
        private byte[] data;
        private Integer clcw;  // Optional OCF
        private int frameSize = 1115;
        
        // Builder methods...
    }
}

public class CommandFrameBuilder {
    /**
     * Build complete CCSDS TC Forward Frame
     */
    public static byte[] build(CommandFrameConfig config);
    
    public static class CommandFrameConfig {
        private int spacecraftId;
        private int virtualChannelId;
        private int frameCount;
        private byte[] data;
        private int frameSize = 1115;
        
        // Builder methods...
    }
}
```

**Benefits**:
- Eliminates ~140 lines of frame construction code from demo
- Single source of truth for frame structure
- Reusable across projects
- Easier to maintain and test
- Consistent frame format

**Effort**: Medium (4-5 hours)
**Impact**: High (significant code reduction, improved maintainability)
**CCSDS Compliance**: ✅ YES - Frame structure fully defined by CCSDS 732.0-B-3 and 232.0-B-3

---

### 2. CLTU Stream Reader (REMOVED - NOT PURE CCSDS STANDARD) ❌

**Reason for Removal**: While CLTU format (start/tail sequences) is defined by CCSDS 231.0-B-3, the stream reading pattern is implementation-specific and not part of the CCSDS standard. The standard defines the CLTU structure but not how to read it from a stream.

**Already Implemented**: We have `CLTUEncoder` and `CLTUDecoder` which handle the CCSDS-standard parts.

---

### 3. Frame Data Extractor (LOW-MEDIUM PRIORITY) ⭐ ✅ CCSDS STANDARD

**Current Problem**: Frame data extraction logic is scattered across multiple files.

**CCSDS Standards**:
- CCSDS 732.0-B-3 Section 4.1.2 (TM Transfer Frame Structure)
- CCSDS 232.0-B-3 Section 4.1.2 (TC Transfer Frame Structure)
- Field positions and sizes are standardized

**Code Analysis**:
```java
// In MOCClient.java
int dataStart = 6;
int dataEnd = frameData.length - 6;
byte[] dataField = new byte[dataEnd - dataStart];
System.arraycopy(frameData, dataStart, dataField, 0, dataField.length);

// In CommandFrame.java (constructor)
ByteBuffer buffer = ByteBuffer.wrap(frameData);
buffer.position(HEADER_SIZE);
int dataLength = frameData.length - HEADER_SIZE - FECF_SIZE;
byte[] commandBytes = new byte[dataLength];
buffer.get(commandBytes);
```

**Proposed Solution**:
```java
package esa.sle.ccsds.utils.frames;

public class FrameDataExtractor {
    /**
     * Extract data field from TM frame (excludes header, OCF, FECF)
     */
    public static byte[] extractTelemetryData(byte[] frameData);
    
    /**
     * Extract data field from TC frame (excludes header, FECF)
     */
    public static byte[] extractCommandData(byte[] frameData);
    
    /**
     * Extract OCF from TM frame
     */
    public static byte[] extractOCF(byte[] frameData);
    
    /**
     * Extract FECF from frame
     */
    public static int extractFECF(byte[] frameData);
}
```

**Benefits**:
- Standardized data extraction
- Handles different frame types
- Clear API
- Reduces error-prone manual calculations

**Effort**: Low (2-3 hours)
**Impact**: Low-Medium (code clarity, reduced errors)
**CCSDS Compliance**: ✅ YES - Field positions defined by CCSDS 732.0-B-3 and 232.0-B-3

---

## Recommended Implementation Order (CCSDS Standard Only)

### Phase 3.1: Frame Builders (HIGH PRIORITY) ✅ CCSDS STANDARD
1. **TelemetryFrameBuilder** - Build complete TM frames per CCSDS 732.0-B-3
2. **CommandFrameBuilder** - Build complete TC frames per CCSDS 232.0-B-3
3. Update demo to use builders
4. Remove ~140 lines from demo

**Effort**: 4-5 hours
**Value**: High (significant code reduction)
**CCSDS Standard**: CCSDS 732.0-B-3, CCSDS 232.0-B-3

### Phase 3.2: Frame Data Extractor (OPTIONAL) ✅ CCSDS STANDARD
1. **FrameDataExtractor** - Extract frame components per CCSDS standards
2. Update demo to use extractor
3. Remove ~30 lines from demo

**Effort**: 2-3 hours
**Value**: Low-Medium (code clarity)
**CCSDS Standard**: CCSDS 732.0-B-3, CCSDS 232.0-B-3

**Note**: CLTU Stream Reader removed as it's implementation-specific, not CCSDS standard.

---

## Code Savings Estimate (CCSDS Standard Only)

### Current Demo Code
- Frame construction: ~140 lines (TelemetryFrame + CommandFrame) ✅ CCSDS STANDARD
- ~~CLTU reading: ~120 lines~~ ❌ NOT CCSDS STANDARD (removed)
- Data extraction: ~30 lines (various files) ✅ CCSDS STANDARD

**Total CCSDS Standard Code**: ~170 lines

### After Phase 3 (CCSDS Standard Only)
- Frame building: `TelemetryFrameBuilder.build(config)`
- Data extraction: `FrameDataExtractor.extractTelemetryData(frame)`

**Total**: ~5 lines of simple method calls

**Savings**: ~165 lines per project + improved maintainability

---

## Benefits Summary

### For Demo
- **280 lines removed** (96% reduction in CCSDS code)
- Cleaner, more focused on SLE protocol
- Easier to understand and maintain
- Better separation of concerns

### For Library Users
- Complete frame building utilities
- Stream-based CLTU reading
- Standard data extraction
- Production-ready implementations

### For Projects
- Faster development
- Fewer bugs
- Standard compliance
- Reusable components

---

## Decision Points (CCSDS Standard Only)

### Should We Implement Phase 3.1 (Frame Builders)?

**Pros**:
- High value (140 lines removed)
- Significant simplification
- Reusable across projects
- Completes the CCSDS toolkit
- ✅ **CCSDS 732.0-B-3 and 232.0-B-3 compliant**

**Cons**:
- Medium effort (4-5 hours)
- Demo already works
- Adds more library code

**Recommendation**: ✅ **YES** - High value, CCSDS standard, completes the toolkit

### Should We Implement Phase 3.2 (Frame Data Extractor)?

**Pros**:
- Standardizes extraction
- Reduces errors
- Clear API
- ✅ **CCSDS 732.0-B-3 and 232.0-B-3 compliant**

**Cons**:
- Low effort but low impact
- Simple operations
- May be overkill

**Recommendation**: ⚠️ **OPTIONAL** - Nice to have, CCSDS standard, low priority

### CLTU Stream Reader - REMOVED

**Decision**: ❌ **NO** - Not a CCSDS standard function. While CLTU format is standardized, stream reading patterns are implementation-specific and should remain in application code.

---

## Comparison: Before vs After Phase 3

### Before Phase 3

```java
// TelemetryFrame.java - 80 lines of frame construction
private byte[] buildFrame(byte[] payload) {
    ByteBuffer buffer = ByteBuffer.allocate(FRAME_SIZE);
    
    // Primary Header (6 bytes)
    int ocfFlag = 1;
    int word1 = (0 << 14) | ((spacecraftId & 0x3FF) << 4) | 
                ((virtualChannelId & 0x7) << 1) | ocfFlag;
    buffer.putShort((short) word1);
    buffer.put((byte) (frameCount >> 8));
    buffer.put((byte) (frameCount & 0xFF));
    buffer.putShort((short) 0x4000);
    
    // Data field...
    // OCF...
    // FECF...
    
    return buffer.array();
}
```

### After Phase 3

```java
// TelemetryFrame.java - 5 lines
private byte[] buildFrame(byte[] payload) {
    return TelemetryFrameBuilder.builder()
        .setSpacecraftId(spacecraftId)
        .setVirtualChannelId(virtualChannelId)
        .setFrameCount(frameCount)
        .setData(payload)
        .setCLCW(clcw)
        .build();
}
```

**Reduction**: 80 lines → 5 lines (94% reduction)

---

## Integration with Existing Utilities

### Frame Builders Use Existing Utilities

```java
// TelemetryFrameBuilder internally uses:
- CRC16Calculator.calculate() for FECF
- CLCWEncoder.encode() for OCF
- FrameHeaderParser constants for structure

// CommandFrameBuilder internally uses:
- CRC16Calculator.calculate() for FECF
- FrameHeaderParser constants for structure
```

### Complete Uplink/Downlink Example

```java
// Spacecraft: Build and send telemetry
byte[] frame = TelemetryFrameBuilder.builder()
    .setSpacecraftId(185)
    .setVirtualChannelId(0)
    .setFrameCount(frameCount++)
    .setData(telemetryData)
    .setCLCW(CLCWEncoder.encode(vcid, lastCmd))
    .build();
transmit(frame);

// Ground: Receive and parse
FrameHeader header = FrameHeaderParser.parse(receivedFrame);
byte[] data = FrameDataExtractor.extractTelemetryData(receivedFrame);
CLCWDecoder.CLCW clcw = CLCWDecoder.decode(
    FrameDataExtractor.extractOCF(receivedFrame));

// Ground: Build and send command
byte[] cmdFrame = CommandFrameBuilder.builder()
    .setSpacecraftId(185)
    .setVirtualChannelId(0)
    .setFrameCount(cmdCount++)
    .setData(commandData)
    .build();
byte[] cltu = CLTUEncoder.encode(cmdFrame);
transmit(cltu);

// Spacecraft: Receive command
byte[] cltu = CLTUStreamReader.readCLTU(inputStream);
byte[] cmdFrame = CLTUDecoder.decode(cltu);
FrameHeader header = FrameHeaderParser.parse(cmdFrame);
byte[] command = FrameDataExtractor.extractCommandData(cmdFrame);
```

---

## Testing Strategy

### Unit Tests
- Frame builders: All field combinations
- CLTU stream reader: Various stream conditions
- Data extractor: Different frame types
- Round-trip: Build → Parse → Extract

### Integration Tests
- Update demo to use Phase 3 utilities
- Verify all tests still pass
- Performance comparison

### Edge Cases
- Minimum/maximum frame sizes
- Empty data fields
- Stream timeouts
- Malformed CLTUs

---

## Documentation Requirements

1. **Library README**: Add Frame Builders section
2. **Class Javadoc**: Complete API documentation
3. **Usage Examples**: Common patterns
4. **Migration Guide**: Update demo code
5. **Phase 3 Summary**: Document changes

---

## Timeline Estimate

### Phase 3.1: Frame Builders
- Design: 1 hour
- Implementation: 2-3 hours
- Testing: 1 hour
- Documentation: 1 hour
- **Total**: 5-6 hours

### Phase 3.2: CLTU Stream Reader
- Design: 0.5 hours
- Implementation: 2-2.5 hours
- Testing: 0.5 hours
- Documentation: 0.5 hours
- **Total**: 3.5-4 hours

### Phase 3.3: Frame Data Extractor
- Design: 0.5 hours
- Implementation: 1-1.5 hours
- Testing: 0.5 hours
- Documentation: 0.5 hours
- **Total**: 2.5-3 hours

### Complete Phase 3
**Total Effort**: 11-13 hours (~1.5-2 days)

---

## Success Criteria

### Phase 3.1 Success
- ✅ TelemetryFrameBuilder implemented
- ✅ CommandFrameBuilder implemented
- ✅ Demo updated to use builders
- ✅ ~140 lines removed from demo
- ✅ All tests passing
- ✅ Documentation complete

### Phase 3.2 Success
- ✅ CLTUStreamReader implemented
- ✅ Demo updated to use reader
- ✅ ~120 lines removed from demo
- ✅ All tests passing
- ✅ Documentation complete

### Phase 3.3 Success
- ✅ FrameDataExtractor implemented
- ✅ Demo updated to use extractor
- ✅ ~30 lines removed from demo
- ✅ All tests passing
- ✅ Documentation complete

---

## Conclusion (CCSDS Standard Only)

Phase 3 offers opportunities to refactor **CCSDS-standardized** code from the demo into reusable library utilities:

**High Priority** ✅ CCSDS STANDARD:
- Frame Builders (140 lines removed, high value)
  - CCSDS 732.0-B-3 (TM Transfer Frame Structure)
  - CCSDS 232.0-B-3 (TC Transfer Frame Structure)

**Optional** ✅ CCSDS STANDARD:
- Frame Data Extractor (30 lines removed, code clarity)
  - CCSDS 732.0-B-3 and 232.0-B-3 (Field positions)

**Removed** ❌ NOT CCSDS STANDARD:
- CLTU Stream Reader - Implementation-specific, not part of CCSDS standard

**Total Potential Savings**: ~170 lines of CCSDS-standard code

After Phase 3, the demo would have all CCSDS-standardized physical/link layer operations handled by the library extension, while keeping implementation-specific code (like stream reading) in the demo where it belongs.

---

**Status**: Proposal (CCSDS Standard Only)

**Next Step**: Decision on Phase 3.1 implementation

**Owner**: SLE Java API Team
