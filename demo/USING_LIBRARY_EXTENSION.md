# Using the Library Extension in the Demo

## Overview

The demo currently has its own CLTU implementation in `demo/src/main/java/esa/sle/demo/common/CLTU.java`. 

Now that we've added CLTU utilities to the library (`esa.sle.java.api.ccsds.utils`), the demo can use the library implementation instead.

## Migration Steps

### 1. Add Dependency

Update `demo/pom.xml` to depend on the new utilities module:

```xml
<dependencies>
    <!-- Add CCSDS utilities dependency -->
    <dependency>
        <groupId>esa.sle.java</groupId>
        <artifactId>esa.sle.java.api.ccsds.utils</artifactId>
        <version>5.1.6</version>
    </dependency>
</dependencies>
```

### 2. Update Imports

**MOCClient.java** - Change:
```java
// Old
import esa.sle.demo.common.CLTU;

// New
import esa.sle.ccsds.utils.cltu.CLTUEncoder;
import esa.sle.ccsds.utils.cltu.CLTUException;
```

**SpacecraftSimulator.java** - Change:
```java
// Old
import esa.sle.demo.common.CLTU;

// New
import esa.sle.ccsds.utils.cltu.CLTUDecoder;
import esa.sle.ccsds.utils.cltu.CLTUException;
```

### 3. Update Code

**MOCClient.java** - sendCommand() method:
```java
// Old
CLTU cltu = new CLTU(cmdFrame.getData());

// New
byte[] cltu = CLTUEncoder.encode(cmdFrame.getData());
```

```java
// Old
commandOutput.write(cltu.getData());
System.out.printf("[FSP] CLTU: %d bytes, %d code blocks%n", 
        cltu.getSize(), cltu.getCodeBlockCount());

// New
commandOutput.write(cltu);
int codeBlocks = CLTUEncoder.getCodeBlockCount(cmdFrame.getData().length);
System.out.printf("[FSP] CLTU: %d bytes, %d code blocks%n", 
        cltu.length, codeBlocks);
```

**SpacecraftSimulator.java** - handleUplink() method:
```java
// Old
try {
    byte[] commandFrameData = CLTU.extractCommandData(cltuData);
    // ...
} catch (CLTU.CLTUException e) {
    System.err.println("[UPLINK] CLTU decode error: " + e.getMessage());
}

// New
try {
    byte[] commandFrameData = CLTUDecoder.decode(cltuData);
    // ...
} catch (CLTUException e) {
    System.err.println("[UPLINK] CLTU decode error: " + e.getMessage());
}
```

### 4. Remove Old Implementation

Delete `demo/src/main/java/esa/sle/demo/common/CLTU.java` since we're now using the library version.

### 5. Rebuild and Test

```bash
# Rebuild demo with library dependency
cd demo
mvn clean package

# Run test
bash test-demo.sh
```

## Benefits of Using Library Extension

### Before (Demo Implementation)
- ❌ Code duplication (every project reimplements CLTU)
- ❌ No shared improvements/bug fixes
- ❌ Each project maintains their own version
- ❌ Inconsistent implementations across projects

### After (Library Extension)
- ✅ Single, standard implementation
- ✅ Shared improvements benefit all users
- ✅ Library team maintains the code
- ✅ Consistent across all projects using the library

## API Comparison

### Old Demo API
```java
// Create CLTU
CLTU cltu = new CLTU(commandData);
byte[] cltuBytes = cltu.getData();
int size = cltu.getSize();
int blocks = cltu.getCodeBlockCount();

// Decode CLTU
byte[] commandData = CLTU.extractCommandData(cltuBytes);
```

### New Library API
```java
// Encode CLTU
byte[] cltuBytes = CLTUEncoder.encode(commandData);
int size = cltuBytes.length;
int blocks = CLTUEncoder.getCodeBlockCount(commandData.length);

// Decode CLTU
byte[] commandData = CLTUDecoder.decode(cltuBytes);
```

**Differences:**
- Library uses static methods (no object creation)
- Slightly more explicit method names
- Same functionality, cleaner API

## Future: Using More Library Utilities

As the library extension grows, the demo can use more utilities:

### Frame Builders (Future)
```java
// Instead of custom TelemetryFrame class
import esa.sle.ccsds.utils.frames.TelemetryFrameBuilder;

TelemetryFrameBuilder builder = new TelemetryFrameBuilder()
    .setSpacecraftId(185)
    .setVirtualChannelId(0)
    .setFrameCount(frameCount)
    .setData(telemetryData)
    .setCLCW(clcwValue);
byte[] frame = builder.build();
```

### CLCW Utilities (Future)
```java
// Instead of manual CLCW encoding
import esa.sle.ccsds.utils.clcw.CLCWEncoder;

int clcw = CLCWEncoder.encode()
    .setVirtualChannelId(vcid)
    .setReportValue(lastCommandFrameCount)
    .build();
```

### CRC Utilities (Future)
```java
// Instead of custom CRC calculation
import esa.sle.ccsds.utils.crc.CRC16Calculator;

int crc = CRC16Calculator.calculate(frameData);
```

## Recommendation

**For the demo**: Keep the current implementation to show a complete, self-contained example.

**For real projects**: Use the library extension to benefit from:
- Standard implementations
- Shared maintenance
- Community improvements
- Consistent behavior

## Conclusion

The library extension provides the same CLTU functionality as the demo's custom implementation, but with the benefits of:
- Being part of the official library
- Shared across all users
- Maintained by the library team
- Following CCSDS standards exactly

Projects can choose to use the library extension or implement their own, depending on their specific requirements.
