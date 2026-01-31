# Installing CCSDS Utilities as a Maven Dependency

## Problem

The main project uses Tycho (Eclipse plugin build system) which creates Eclipse-specific metadata. This makes it difficult for standard Maven projects to use the CCSDS utilities as a dependency.

## Solution

We provide a standalone Maven POM (`pom-standalone.xml`) that builds the CCSDS utilities as a standard Maven artifact with proper metadata.

## Installation

### Quick Install

Run the installation script:

```bash
cd esa.sle.java.api.ccsds.utils
./install-maven.sh
```

This will:
1. Build the CCSDS utilities with standard Maven
2. Install to your local Maven repository (`~/.m2/repository/`)
3. Create proper Maven metadata (POM file)
4. Generate source and javadoc JARs

### Manual Install

If you prefer to install manually:

```bash
cd esa.sle.java.api.ccsds.utils
mvn -f pom-standalone.xml clean install
```

## Verification

After installation, verify the files exist:

```bash
ls -lh ~/.m2/repository/esa/sle/java/esa.sle.java.api.ccsds.utils/5.1.6/
```

You should see:
- `esa.sle.java.api.ccsds.utils-5.1.6.jar` (compiled classes)
- `esa.sle.java.api.ccsds.utils-5.1.6-sources.jar` (source code)
- `esa.sle.java.api.ccsds.utils-5.1.6-javadoc.jar` (documentation)
- `esa.sle.java.api.ccsds.utils-5.1.6.pom` (Maven metadata)

## Usage in Your Project

Add this dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>esa.sle.java</groupId>
    <artifactId>esa.sle.java.api.ccsds.utils</artifactId>
    <version>5.1.6</version>
</dependency>
```

### Complete Example

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-project</artifactId>
    <version>1.0.0</version>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- CCSDS Utilities -->
        <dependency>
            <groupId>esa.sle.java</groupId>
            <artifactId>esa.sle.java.api.ccsds.utils</artifactId>
            <version>5.1.6</version>
        </dependency>
    </dependencies>
</project>
```

## What's Included

The CCSDS utilities library provides 18 utility classes:

### Physical Layer
- **CLTUEncoder/Decoder** - CLTU encoding and decoding (CCSDS 231.0-B-3)
- **BCHEncoder** - BCH error detection
- **PseudoRandomizer** - Spectral shaping (CCSDS 131.0-B-3)

### Link Layer
- **CRC16Calculator** - Frame Error Control Field (CCSDS 131.0-B-3)
- **CLCWEncoder/Decoder** - Command Link Control Word (CCSDS 232.0-B-3)
- **FrameHeaderParser** - Parse TM/TC frame headers (CCSDS 732.0-B-3, 232.0-B-3)
- **FrameHeader** - Immutable frame header object
- **TelemetryFrameBuilder** - Build complete TM frames (CCSDS 732.0-B-3)
- **CommandFrameBuilder** - Build complete TC frames (CCSDS 232.0-B-3)

### Time Representation
- **CUCTimeEncoder/Decoder** - CCSDS Unsegmented Time Code (CCSDS 301.0-B-4)
- **CDSTimeEncoder/Decoder** - CCSDS Day Segmented Time Code (CCSDS 301.0-B-4)

### Application Layer
- **SpacePacketBuilder** - Build CCSDS Space Packets (CCSDS 133.0-B-2)
- **SpacePacketParser** - Parse CCSDS Space Packets (CCSDS 133.0-B-2)

## Example Usage

```java
import esa.sle.ccsds.utils.cltu.CLTUEncoder;
import esa.sle.ccsds.utils.frames.TelemetryFrameBuilder;
import esa.sle.ccsds.utils.clcw.CLCWEncoder;

// Build a telemetry frame
byte[] data = "Hello from spacecraft".getBytes();
int clcw = CLCWEncoder.encode(0, 42);

byte[] tmFrame = TelemetryFrameBuilder.builder()
    .setSpacecraftId(185)
    .setVirtualChannelId(0)
    .setFrameCount(10)
    .setData(data)
    .setOcf(clcw)
    .build();

// Encode command in CLTU
byte[] commandFrame = ...; // Your command frame
byte[] cltu = CLTUEncoder.encode(commandFrame);
```

## Differences from Tycho Build

| Aspect | Tycho Build | Standalone Maven Build |
|--------|-------------|------------------------|
| Packaging | `eclipse-plugin` | `jar` |
| POM | Minimal consumer POM | Full Maven POM |
| Dependencies | Eclipse P2 metadata | Standard Maven dependencies |
| Usage | Eclipse plugins only | Any Maven project |
| Sources JAR | Not included | Included |
| Javadoc JAR | Not included | Included |

## Troubleshooting

### Dependency Not Found

If Maven can't find the dependency, ensure you've run the installation script:

```bash
cd esa.sle.java.api.ccsds.utils
./install-maven.sh
```

### Wrong POM Metadata

If you see Eclipse-specific metadata, you may have installed with the parent Tycho build. Run the standalone installation:

```bash
mvn -f pom-standalone.xml clean install
```

### Version Mismatch

Ensure the version in your dependency matches the installed version (5.1.6).

## Notes

- The standalone Maven build is independent of the Tycho build
- Both builds produce the same compiled classes
- The standalone build adds proper Maven metadata for dependency resolution
- No external dependencies required - this is a standalone library
- Requires Java 17 or higher

## See Also

- [CCSDS Utilities README](README.md) - Complete documentation
- [Demo Application](../demo/README.md) - Usage examples
