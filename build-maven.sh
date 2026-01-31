#!/bin/bash
# Build SLE Java API using standard Maven (no Tycho/Eclipse dependencies)
# This script builds the core library, CCSDS utilities, and demo application

set -e

echo "================================================================================"
echo "Building SLE Java API with Standard Maven"
echo "================================================================================"
echo ""
echo "This build uses standard Maven without Tycho/Eclipse dependencies."
echo "Build order:"
echo "  1. CCSDS Utilities (no dependencies)"
echo "  2. SLE API Core (depends on jasn1)"
echo "  3. Demo Application (depends on core + utilities)"
echo ""
echo "================================================================================"
echo ""

# Build CCSDS Utilities
echo "[1/3] Building CCSDS Utilities..."
echo "----------------------------------------------------------------------"
mvn -f esa.sle.java.api.ccsds.utils/pom-standalone.xml clean install
echo ""

# Build SLE API Core
echo "[2/3] Building SLE API Core..."
echo "----------------------------------------------------------------------"
mvn -f esa.sle.java.api.core/pom-standalone.xml clean install
echo ""

# Build Demo
echo "[3/3] Building Demo Application..."
echo "----------------------------------------------------------------------"
mvn -f demo/pom.xml clean package
echo ""

echo "================================================================================"
echo "Build Complete!"
echo "================================================================================"
echo ""
echo "Artifacts installed to ~/.m2/repository/:"
echo ""
echo "  CCSDS Utilities:"
echo "    esa.sle.java.api.ccsds.utils-5.1.6.jar (33 KB)"
echo ""
echo "  SLE API Core:"
echo "    esa.sle.java.api.core-5.1.6.jar (2.4 MB)"
echo ""
echo "  Demo JARs (in demo/target/):"
echo "    sle-demo-1.0.0-moc.jar"
echo "    sle-demo-1.0.0-groundstation.jar"
echo "    sle-demo-1.0.0-spacecraft.jar"
echo ""
echo "Run the demo:"
echo "  cd demo"
echo "  ./test-demo.sh"
echo ""
echo "================================================================================"
