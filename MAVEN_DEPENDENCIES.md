# Using SLE Java API as Maven Dependencies

## Overview

The SLE Java API project uses Tycho (Eclipse plugin build system) for its main build, which creates Eclipse-specific metadata. To enable standard Maven projects to use the libraries, we provide standalone Maven builds that generate proper Maven artifacts.

## Quick Start

### Install Both Libraries

```bash
# Install CCSDS Utilities
cd esa.sle.java.api.ccsds.utils
./install-maven.sh
cd ..

# Install SLE API Core
cd esa.sle.java.api.core
./install-maven.sh
cd ..
```

### Use in Your Project

Add to your `pom.xml`:

```xml
<dependencies>
    <!-- SLE API Core (required for SLE services) -->
    <dependency>
        <groupId>esa.sle.java</groupId>
        <artifactId>esa.sle.java.api.core</artifactId>
        <version>5.1.6</version>
    </dependency>
    
    <!-- CCSDS Utilities (optional, for physical layer operations) -->
    <dependency>
        <groupId>esa.sle.java</groupId>
        <artifactId>esa.sle.java.api.ccsds.utils</artifactId>
        <version>5.1.6</version>
    </dependency>
</dependencies>
```

## Libraries

### 1. SLE API Core (`esa.sle.java.api.core`)

**What it provides:**
- Complete CCSDS SLE protocol implementation
- RAF, RCF, ROCF, CLTU, FSP services
- ASN.1 encoding/decoding
- Service instance management
- User and provider roles

**Size:** 2.4 MB (JAR), 1.6 MB (sources), 3.9 MB (javadoc)

**Dependencies:**
- `com.beanit:jasn1-compiler:1.11.2`
- `com.beanit:jasn1:1.11.2`

**Installation:**
```bash
cd esa.sle.java.api.core
./install-maven.sh
```

**Documentation:** [esa.sle.java.api.core/MAVEN_INSTALL.md](esa.sle.java.api.core/MAVEN_INSTALL.md)

### 2. CCSDS Utilities (`esa.sle.java.api.ccsds.utils`)

**What it provides:**
- CLTU encoding/decoding
- Frame builders (TM/TC)
- CLCW encoding/decoding
- CRC-16 calculation
- Time code utilities (CUC/CDS)
- Space packet builder/parser
- Pseudo-randomization
- Frame header parsing

**Size:** 33 KB (JAR), 33 KB (sources), 215 KB (javadoc)

**Dependencies:** None (standalone library)

**Installation:**
```bash
cd esa.sle.java.api.ccsds.utils
./install-maven.sh
```

**Documentation:** [esa.sle.java.api.ccsds.utils/MAVEN_INSTALL.md](esa.sle.java.api.ccsds.utils/MAVEN_INSTALL.md)

## Complete Project Template

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-sle-project</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <name>My SLE Project</name>
    <description>Project using SLE Java API</description>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <maven.compiler.release>17</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <sle.version>5.1.6</sle.version>
    </properties>

    <dependencies>
        <!-- SLE API Core -->
        <dependency>
            <groupId>esa.sle.java</groupId>
            <artifactId>esa.sle.java.api.core</artifactId>
            <version>${sle.version}</version>
        </dependency>
        
        <!-- CCSDS Utilities -->
        <dependency>
            <groupId>esa.sle.java</groupId>
            <artifactId>esa.sle.java.api.ccsds.utils</artifactId>
            <version>${sle.version}</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>17</source>
                    <target>17</target>
                    <release>17</release>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

## Example Usage

### Receive Telemetry (RAF Service)

```java
import esa.sle.ccsds.utils.frames.FrameHeaderParser;
import esa.sle.ccsds.utils.clcw.CLCWDecoder;

// Receive frame via RAF service
byte[] frame = ...; // From RAF-TRANSFER-DATA

// Parse frame header
FrameHeader header = FrameHeaderParser.parse(frame);
System.out.println("SCID: " + header.getSpacecraftId());
System.out.println("VCID: " + header.getVirtualChannelId());

// Extract CLCW from OCF
if (header.isOcfPresent()) {
    int clcw = extractOCF(frame);
    CLCWDecoder.CLCW decoded = CLCWDecoder.decode(clcw);
    System.out.println("Report Value: " + decoded.getReportValue());
}
```

### Send Commands (FSP Service)

```java
import esa.sle.ccsds.utils.frames.CommandFrameBuilder;
import esa.sle.ccsds.utils.cltu.CLTUEncoder;

// Build command frame
byte[] commandData = "DEPLOY_SOLAR_PANELS".getBytes();
byte[] tcFrame = CommandFrameBuilder.builder()
    .setSpacecraftId(185)
    .setVirtualChannelId(0)
    .setFrameCount(42)
    .setData(commandData)
    .build();

// Encode in CLTU
byte[] cltu = CLTUEncoder.encode(tcFrame);

// Send via FSP service
// fspService.transferData(cltu);
```

## Verification

After installation, verify the artifacts:

```bash
# Check CCSDS Utilities
ls -lh ~/.m2/repository/esa/sle/java/esa.sle.java.api.ccsds.utils/5.1.6/

# Check SLE API Core
ls -lh ~/.m2/repository/esa/sle/java/esa.sle.java.api.core/5.1.6/

# Test dependency resolution
mvn dependency:get -Dartifact=esa.sle.java:esa.sle.java.api.ccsds.utils:5.1.6
mvn dependency:get -Dartifact=esa.sle.java:esa.sle.java.api.core:5.1.6
```

## Requirements

- **Java**: 17 or higher
- **Maven**: 3.8.5 or higher
- **OS**: Linux, macOS, or Windows

## Comparison: Tycho vs Standalone Maven

| Feature | Tycho Build | Standalone Maven Build |
|---------|-------------|------------------------|
| **Packaging** | `eclipse-plugin` | `jar` |
| **POM Metadata** | Minimal consumer POM | Complete Maven POM |
| **Dependencies** | Eclipse P2 | Standard Maven |
| **Usage** | Eclipse plugins only | Any Maven project |
| **Sources JAR** | ❌ Not included | ✅ Included |
| **Javadoc JAR** | ❌ Not included | ✅ Included |
| **IDE Integration** | Limited | Full support |
| **Dependency Resolution** | P2 resolver | Maven resolver |

## Troubleshooting

### Dependencies Not Found

Ensure you've installed both libraries:

```bash
cd esa.sle.java.api.ccsds.utils && ./install-maven.sh && cd ..
cd esa.sle.java.api.core && ./install-maven.sh && cd ..
```

### Wrong Metadata

If you see Eclipse-specific metadata, reinstall with standalone POMs:

```bash
mvn -f esa.sle.java.api.ccsds.utils/pom-standalone.xml clean install
mvn -f esa.sle.java.api.core/pom-standalone.xml clean install
```

### Version Conflicts

Ensure both libraries use the same version (5.1.6):

```xml
<properties>
    <sle.version>5.1.6</sle.version>
</properties>
```

### Java Version Issues

Both libraries require Java 17 or higher:

```bash
java -version  # Should show 17 or higher
```

## Build Times

- **CCSDS Utilities**: ~9 seconds
- **SLE API Core**: ~27 seconds (includes ASN.1 generation)
- **Total**: ~36 seconds

## Artifacts Installed

### CCSDS Utilities
```
~/.m2/repository/esa/sle/java/esa.sle.java.api.ccsds.utils/5.1.6/
├── esa.sle.java.api.ccsds.utils-5.1.6.jar (33 KB)
├── esa.sle.java.api.ccsds.utils-5.1.6-sources.jar (33 KB)
├── esa.sle.java.api.ccsds.utils-5.1.6-javadoc.jar (215 KB)
└── esa.sle.java.api.ccsds.utils-5.1.6.pom (3.3 KB)
```

### SLE API Core
```
~/.m2/repository/esa/sle/java/esa.sle.java.api.core/5.1.6/
├── esa.sle.java.api.core-5.1.6.jar (2.4 MB)
├── esa.sle.java.api.core-5.1.6-sources.jar (1.6 MB)
├── esa.sle.java.api.core-5.1.6-javadoc.jar (3.9 MB)
└── esa.sle.java.api.core-5.1.6.pom (12 KB)
```

## See Also

- [Main README](README.md) - Project overview
- [Demo Application](demo/README.md) - Complete usage example
- [CCSDS Utilities README](esa.sle.java.api.ccsds.utils/README.md) - Utilities documentation
- [Core Library Maven Install](esa.sle.java.api.core/MAVEN_INSTALL.md) - Core library details
- [Utilities Maven Install](esa.sle.java.api.ccsds.utils/MAVEN_INSTALL.md) - Utilities details

## Support

For issues with Maven dependencies:
1. Check that installation scripts completed successfully
2. Verify artifacts exist in `~/.m2/repository/`
3. Ensure Java 17+ and Maven 3.8.5+ are installed
4. Review individual MAVEN_INSTALL.md files for detailed troubleshooting

## License

Same as the main SLE Java API project.
