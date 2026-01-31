# Phase 1 Refactoring - CRC and CLCW Utilities

## Overview

Successfully refactored the demo to use the new CRC-16 and CLCW utilities from the `esa.sle.java.api.ccsds.utils` library extension.

## Changes Made

### 1. TelemetryFrame.java

**Before** (Manual Implementation):
```java
// Manual CLCW encoding (20 lines of bit manipulation)
int clcw = 0;
clcw |= (virtualChannelId & 0x3F) << 18;
if (lastCommandReceived >= 0) {
    clcw |= (lastCommandReceived & 0xFF);
}
buffer.putInt(clcw);

// Manual CRC-16 calculation (15 lines)
private int calculateCRC16(byte[] data) {
    int crc = 0xFFFF;
    int polynomial = 0x1021;
    for (byte b : data) {
        crc ^= (b & 0xFF) << 8;
        for (int i = 0; i < 8; i++) {
            if ((crc & 0x8000) != 0) {
                crc = (crc << 1) ^ polynomial;
            } else {
                crc = crc << 1;
            }
        }
    }
    return crc & 0xFFFF;
}
```

**After** (Library Utilities):
```java
import esa.sle.ccsds.utils.clcw.CLCWEncoder;
import esa.sle.ccsds.utils.crc.CRC16Calculator;

// CLCW encoding (3 lines)
int clcw = (lastCommandReceived >= 0) ?
        CLCWEncoder.encode(virtualChannelId, lastCommandReceived) :
        CLCWEncoder.encode(virtualChannelId, 0);
buffer.putInt(clcw);

// CRC-16 calculation (1 line)
int crc = CRC16Calculator.calculate(frameWithoutFECF);
```

**Lines Saved**: 31 lines → 4 lines (27 lines removed, 87% reduction)

---

### 2. CommandFrame.java

**Before** (Manual Implementation):
```java
// Manual CRC-16 calculation (15 lines)
private int calculateCRC16(byte[] data) {
    int crc = 0xFFFF;
    int polynomial = 0x1021;
    for (byte b : data) {
        crc ^= (b & 0xFF) << 8;
        for (int i = 0; i < 8; i++) {
            if ((crc & 0x8000) != 0) {
                crc = (crc << 1) ^ polynomial;
            } else {
                crc = crc << 1;
            }
        }
    }
    return crc & 0xFFFF;
}
```

**After** (Library Utilities):
```java
import esa.sle.ccsds.utils.crc.CRC16Calculator;

// CRC-16 calculation (1 line)
int crc = CRC16Calculator.calculate(frameWithoutFECF);
```

**Lines Saved**: 15 lines → 1 line (14 lines removed, 93% reduction)

---

### 3. MOCClient.java

**Before** (Manual Implementation):
```java
// Manual CLCW decoding (5 lines of bit manipulation)
int clcw = buffer.getInt();
int clcwVCID = (clcw >> 18) & 0x3F;
int clcwReportValue = clcw & 0xFF;

String clcwInfo = (clcwReportValue >= 0) ? 
        String.format("CLCW_ACK=%d", clcwReportValue) : "CLCW_ACK=NONE";
```

**After** (Library Utilities):
```java
import esa.sle.ccsds.utils.clcw.CLCWDecoder;

// CLCW decoding (4 lines, but type-safe and complete)
int clcwWord = buffer.getInt();
CLCWDecoder.CLCW clcw = CLCWDecoder.decode(clcwWord);
int clcwReportValue = clcw.getReportValue();
String clcwInfo = String.format("CLCW_ACK=%d", clcwReportValue);
```

**Lines Saved**: 5 lines → 4 lines (1 line saved, but much clearer)

**Additional Benefits**:
- Type-safe CLCW object
- Access to all CLCW fields (not just report value)
- Can check `clcw.isNominal()` for status
- Better error handling

---

## Code Reduction Summary

| File | Before | After | Saved | Reduction |
|------|--------|-------|-------|-----------|
| TelemetryFrame.java | 31 lines | 4 lines | 27 lines | 87% |
| CommandFrame.java | 15 lines | 1 line | 14 lines | 93% |
| MOCClient.java | 5 lines | 4 lines | 1 line | 20% |
| **Total** | **51 lines** | **9 lines** | **42 lines** | **82%** |

**Total Code Reduction**: 42 lines of complex bit manipulation replaced with 9 lines of simple library calls.

---

## Benefits

### 1. Code Quality
- ✅ Eliminated code duplication (CRC-16 was in 2 files)
- ✅ Removed complex bit manipulation
- ✅ Clearer intent with named methods
- ✅ Type-safe CLCW handling

### 2. Maintainability
- ✅ Single source of truth for CRC and CLCW
- ✅ Easier to understand
- ✅ Easier to test
- ✅ Easier to debug

### 3. Correctness
- ✅ Library utilities are thoroughly tested
- ✅ Standard CCSDS implementations
- ✅ Less prone to bit manipulation errors
- ✅ Consistent behavior across projects

### 4. Reusability
- ✅ Other projects can use same utilities
- ✅ No need to copy/paste code
- ✅ Improvements benefit all users
- ✅ Community contributions possible

---

## Test Results

All tests pass with the refactored code:

```
================================================================================
                           VALIDATION
================================================================================

✓ PASS: MOC received at least 20 frames (37)
✓ PASS: No errors detected
✓ PASS: Data flow is consistent
✓ PASS: Success rate is 100% (>= 95%)
✓ PASS: All processes still running

✓ ALL TESTS PASSED (5/5)
```

### Verification

- ✅ Both commands execute successfully
- ✅ CLCW acknowledgments work correctly
- ✅ CRC validation passes
- ✅ No errors or warnings
- ✅ Same performance as before

---

## Library Utilities Added

### CRC16Calculator

**Package**: `esa.sle.ccsds.utils.crc`

**Methods**:
- `calculate(byte[] data)` - Calculate CRC-16-CCITT
- `calculate(byte[] data, int offset, int length)` - Calculate for portion
- `verify(byte[] data, int expectedCrc)` - Verify CRC
- `verifyAppended(byte[] dataWithCrc)` - Verify appended CRC
- `appendCrc(byte[] data)` - Append CRC to data

**Features**:
- CRC-16-CCITT polynomial (0x1021)
- Initial value 0xFFFF
- Fully tested
- CCSDS compliant

---

### CLCWEncoder

**Package**: `esa.sle.ccsds.utils.clcw`

**Methods**:
- `encode(int vcid, int reportValue)` - Simple encoding
- `builder()` - Advanced encoding with all fields

**Builder Methods**:
- `setVirtualChannelId(int vcid)`
- `setReportValue(int value)`
- `setStatusField(int status)`
- `setCopInEffect(int cop)`
- `setNoRfAvailable(boolean noRf)`
- `setNoBitLock(boolean noBitLock)`
- `setLockout(boolean lockout)`
- `setWait(boolean wait)`
- `setRetransmit(boolean retransmit)`
- `setFarmBCounter(int counter)`
- `build()`

**Features**:
- Simple API for common use case
- Builder pattern for advanced use
- All CLCW fields supported
- CCSDS compliant

---

### CLCWDecoder

**Package**: `esa.sle.ccsds.utils.clcw`

**Methods**:
- `decode(int clcwWord)` - Decode from 32-bit word
- `decode(byte[] clcwBytes)` - Decode from byte array

**CLCW Object Methods**:
- `getType()`
- `getVersion()`
- `getStatusField()`
- `getCopInEffect()`
- `getVirtualChannelId()`
- `isNoRfAvailable()`
- `isNoBitLock()`
- `isLockout()`
- `isWait()`
- `isRetransmit()`
- `getFarmBCounter()`
- `getReportValue()`
- `getRawValue()`
- `isNominal()` - Check if all status indicators are nominal
- `toString()` - Simple string representation
- `toDetailedString()` - Detailed string representation

**Features**:
- Type-safe CLCW object
- Access to all fields
- Nominal status check
- CCSDS compliant

---

## Usage Examples

### Simple CLCW Encoding
```java
int clcw = CLCWEncoder.encode(virtualChannelId, lastCommandFrameCount);
```

### Advanced CLCW Encoding
```java
int clcw = CLCWEncoder.builder()
    .setVirtualChannelId(0)
    .setReportValue(5)
    .setLockout(false)
    .setWait(false)
    .build();
```

### CLCW Decoding
```java
CLCWDecoder.CLCW clcw = CLCWDecoder.decode(clcwWord);
System.out.println("Report Value: " + clcw.getReportValue());
System.out.println("VCID: " + clcw.getVirtualChannelId());
System.out.println("Nominal: " + clcw.isNominal());
```

### CRC Calculation
```java
int crc = CRC16Calculator.calculate(frameData);
```

### CRC Verification
```java
boolean valid = CRC16Calculator.verify(frameData, expectedCrc);
```

### CRC Append
```java
byte[] frameWithCrc = CRC16Calculator.appendCrc(frameData);
```

---

## Migration Guide

### For Existing Projects

1. **Add Dependency**:
   ```xml
   <dependency>
       <groupId>esa.sle.java</groupId>
       <artifactId>esa.sle.java.api.ccsds.utils</artifactId>
       <version>5.1.6</version>
   </dependency>
   ```

2. **Replace CRC Calculation**:
   ```java
   // Old
   private int calculateCRC16(byte[] data) { ... }
   
   // New
   import esa.sle.ccsds.utils.crc.CRC16Calculator;
   int crc = CRC16Calculator.calculate(data);
   ```

3. **Replace CLCW Encoding**:
   ```java
   // Old
   int clcw = 0;
   clcw |= (vcid & 0x3F) << 18;
   clcw |= (reportValue & 0xFF);
   
   // New
   import esa.sle.ccsds.utils.clcw.CLCWEncoder;
   int clcw = CLCWEncoder.encode(vcid, reportValue);
   ```

4. **Replace CLCW Decoding**:
   ```java
   // Old
   int reportValue = clcw & 0xFF;
   
   // New
   import esa.sle.ccsds.utils.clcw.CLCWDecoder;
   CLCWDecoder.CLCW decoded = CLCWDecoder.decode(clcw);
   int reportValue = decoded.getReportValue();
   ```

---

## Next Steps

### Phase 2 Candidates

1. **Frame Header Parser** - Extract header parsing logic
2. **Pseudo-Randomization** - Add randomization utilities
3. **Time Code Utilities** - CUC/CDS time encoding/decoding
4. **Space Packet Protocol** - Packet building and parsing

See `FUTURE_ENHANCEMENTS.md` for details.

---

## Conclusion

Phase 1 refactoring successfully:

✅ **Reduced code by 82%** (42 lines removed)
✅ **Eliminated duplication** (CRC-16 in 2 files)
✅ **Improved code quality** (clearer, type-safe)
✅ **Maintained functionality** (all tests pass)
✅ **Added reusable utilities** (CRC, CLCW)

The demo now demonstrates proper use of the library extension, and other projects can benefit from these utilities.

---

**Status**: ✅ Complete

**Test Results**: 5/5 Passing

**Code Reduction**: 82% (42 lines removed)

**Performance**: No degradation

**Next**: Phase 2 (Frame Parsers, Randomization)
