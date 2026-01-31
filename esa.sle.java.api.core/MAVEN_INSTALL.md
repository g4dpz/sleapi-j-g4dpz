# Installing SLE API Core as a Maven Dependency

## Problem

The main project uses Tycho (Eclipse plugin build system) which creates Eclipse-specific metadata. This makes it difficult for standard Maven projects to use the SLE API Core as a dependency.

## Solution

We provide a standalone Maven POM (`pom-standalone.xml`) that builds the SLE API Core as a standard Maven artifact with proper metadata.

## Installation

### Quick Install

Run the installation script:

```bash
cd esa.sle.java.api.core
./install-maven.sh
```

This will:
1. Generate ASN.1 sources from CCSDS specifications
2. Build the SLE API Core with standard Maven
3. Install to your local Maven repository (`~/.m2/repository/`)
4. Create proper Maven metadata (POM file)
5. Generate source and javadoc JARs

### Manual Install

If you prefer to install manually:

```bash
cd esa.sle.java.api.core
mvn -f pom-standalone.xml clean install
```

## Verification

After installation, verify the files exist:

```bash
ls -lh ~/.m2/repository/esa/sle/java/esa.sle.java.api.core/5.1.6/
```

You should see:
- `esa.sle.java.api.core-5.1.6.jar` (compiled classes, ~2.6 MB)
- `esa.sle.java.api.core-5.1.6-sources.jar` (source code)
- `esa.sle.java.api.core-5.1.6-javadoc.jar` (documentation)
- `esa.sle.java.api.core-5.1.6.pom` (Maven metadata)

## Usage in Your Project

Add this dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>esa.sle.java</groupId>
    <artifactId>esa.sle.java.api.core</artifactId>
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
    <artifactId>my-sle-project</artifactId>
    <version>1.0.0</version>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- SLE API Core -->
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
</project>
```

## What's Included

The SLE API Core library provides complete implementations of CCSDS SLE services:

### Return Services (Downlink)
- **RAF** (Return All Frames) - CCSDS 911.1-B-4
- **RCF** (Return Channel Frames) - CCSDS 911.2-B-3
- **ROCF** (Return Operational Control Fields) - CCSDS 911.5-B-2

### Forward Services (Uplink)
- **CLTU** (Forward CLTU) - CCSDS 912.1-B-4
- **FSP** (Forward Space Packet) - CCSDS 912.3-B-3

### Core Features
- Complete SLE protocol implementation
- ASN.1 encoding/decoding (using jasn1)
- Service instance management
- User and provider roles
- Authentication and security
- State management
- Event handling

## Dependencies

The core library has two runtime dependencies:

```xml
<dependency>
    <groupId>com.beanit</groupId>
    <artifactId>jasn1-compiler</artifactId>
    <version>1.11.2</version>
</dependency>

<dependency>
    <groupId>com.beanit</groupId>
    <artifactId>jasn1</artifactId>
    <version>1.11.2</version>
</dependency>
```

These are automatically included when you add the SLE API Core dependency.

## Example Usage

### RAF Service (Receive Telemetry)

```java
import esa.sle.impl.api.apiop.rafop.*;
import esa.sle.impl.ifs.gen.*;

// Create RAF service instance
ISLE_ServiceInform serviceInform = ...; // Your service inform handler
ISLE_SII serviceInstanceId = ...; // Service instance identifier

// Configure and bind
// Start receiving frames
// Process RAF-TRANSFER-DATA operations
```

### FSP Service (Send Commands)

```java
import esa.sle.impl.api.apiop.fspop.*;

// Create FSP service instance
// Configure and bind
// Send space packets via FSP-TRANSFER-DATA
```

## Build Process

The standalone Maven build performs these steps:

1. **Clean**: Remove any previously generated sources
2. **Copy Libraries**: Copy jasn1 JARs to `lib/` directory
3. **Generate ASN.1 Sources**: Compile CCSDS ASN.1 specifications to Java
   - Common types and PDUs
   - RAF, RCF, ROCF, CLTU, FSP modules
   - Credentials and bind types
4. **Add Sources**: Add generated sources to build path
5. **Compile**: Compile all Java sources (manual + generated)
6. **Package**: Create JAR with manifest
7. **Attach Sources**: Create sources JAR
8. **Attach Javadoc**: Generate and package javadoc
9. **Install**: Install all artifacts to local Maven repository

## Differences from Tycho Build

| Aspect | Tycho Build | Standalone Maven Build |
|--------|-------------|------------------------|
| Packaging | `eclipse-plugin` | `jar` |
| POM | Minimal consumer POM | Full Maven POM |
| Dependencies | Eclipse P2 metadata | Standard Maven dependencies |
| Usage | Eclipse plugins only | Any Maven project |
| Sources JAR | Not included | Included |
| Javadoc JAR | Not included | Included |
| ASN.1 Generation | Via Tycho | Via exec-maven-plugin |

## Troubleshooting

### Dependency Not Found

If Maven can't find the dependency, ensure you've run the installation script:

```bash
cd esa.sle.java.api.core
./install-maven.sh
```

### ASN.1 Generation Fails

If ASN.1 source generation fails, ensure:
- Java 17 or higher is installed
- jasn1 dependencies are available in Maven Central
- ASN.1 specification files exist in `src/main/java/esa/sle/impl/ifs/asn1/`

### Compilation Errors

If compilation fails after ASN.1 generation:
- Check that generated sources are in `src/main/generated/`
- Verify Java version is 17 or higher
- Ensure all dependencies are resolved

### Wrong POM Metadata

If you see Eclipse-specific metadata, you may have installed with the parent Tycho build. Run the standalone installation:

```bash
mvn -f pom-standalone.xml clean install
```

## Notes

- The standalone Maven build is independent of the Tycho build
- Both builds produce the same compiled classes
- The standalone build adds proper Maven metadata for dependency resolution
- ASN.1 sources are generated during build (not checked into version control)
- Requires Java 17 or higher
- Total build time: ~15-20 seconds (includes ASN.1 generation)

## See Also

- [CCSDS Utilities Maven Install](../esa.sle.java.api.ccsds.utils/MAVEN_INSTALL.md) - Physical layer utilities
- [Demo Application](../demo/README.md) - Complete usage example
- [Main README](../README.md) - Project overview
