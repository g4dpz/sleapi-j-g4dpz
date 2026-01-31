#!/bin/bash
# Install CCSDS Utilities as a standard Maven artifact
# This creates a proper Maven-compatible installation that other projects can use

set -e

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "================================================================================"
echo "Installing CCSDS Utilities as Standard Maven Artifact"
echo "================================================================================"
echo ""

# Build with standalone POM
echo "[1/3] Building with standalone Maven POM..."
mvn -f "$SCRIPT_DIR/pom-standalone.xml" clean install

echo ""
echo "[2/3] Verifying installation..."
if [ -f ~/.m2/repository/esa/sle/java/esa.sle.java.api.ccsds.utils/5.1.6/esa.sle.java.api.ccsds.utils-5.1.6.jar ]; then
    echo "✓ JAR installed successfully"
else
    echo "✗ JAR installation failed"
    exit 1
fi

if [ -f ~/.m2/repository/esa/sle/java/esa.sle.java.api.ccsds.utils/5.1.6/esa.sle.java.api.ccsds.utils-5.1.6.pom ]; then
    echo "✓ POM installed successfully"
else
    echo "✗ POM installation failed"
    exit 1
fi

if [ -f ~/.m2/repository/esa/sle/java/esa.sle.java.api.ccsds.utils/5.1.6/esa.sle.java.api.ccsds.utils-5.1.6-sources.jar ]; then
    echo "✓ Sources JAR installed successfully"
else
    echo "✗ Sources JAR installation failed"
    exit 1
fi

echo ""
echo "[3/3] Installation complete!"
echo ""
echo "================================================================================"
echo "CCSDS Utilities installed to local Maven repository"
echo "================================================================================"
echo ""
echo "Location: ~/.m2/repository/esa/sle/java/esa.sle.java.api.ccsds.utils/5.1.6/"
echo ""
echo "Files installed:"
echo "  - esa.sle.java.api.ccsds.utils-5.1.6.jar (compiled classes)"
echo "  - esa.sle.java.api.ccsds.utils-5.1.6-sources.jar (source code)"
echo "  - esa.sle.java.api.ccsds.utils-5.1.6-javadoc.jar (documentation)"
echo "  - esa.sle.java.api.ccsds.utils-5.1.6.pom (Maven metadata)"
echo ""
echo "Use in your project with:"
echo ""
echo "  <dependency>"
echo "      <groupId>esa.sle.java</groupId>"
echo "      <artifactId>esa.sle.java.api.ccsds.utils</artifactId>"
echo "      <version>5.1.6</version>"
echo "  </dependency>"
echo ""
echo "================================================================================"
