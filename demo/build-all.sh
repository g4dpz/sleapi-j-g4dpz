#!/bin/bash
# Build everything needed for the demo

set -e  # Exit on error

echo "================================================================================"
echo "SLE Demo - Build Script"
echo "================================================================================"
echo ""

# Check Java version
echo "Checking Java version..."
java -version 2>&1 | head -1
echo ""

# Check Maven version
echo "Checking Maven version..."
mvn -version | head -1
echo ""

echo "================================================================================"
echo "Step 1: Building parent SLE Java API"
echo "================================================================================"
cd ..
mvn clean install -DskipTests
echo ""

echo "================================================================================"
echo "Step 2: Building demo"
echo "================================================================================"
cd demo
mvn clean package
echo ""

echo "================================================================================"
echo "Build Complete!"
echo "================================================================================"
echo ""
echo "Generated JAR files:"
ls -lh target/sle-demo-*.jar 2>/dev/null || echo "No JAR files found"
echo ""
echo "To run the demo:"
echo "  Terminal 1: ./run-groundstation.sh"
echo "  Terminal 2: ./run-spacecraft.sh"
echo "  Terminal 3: ./run-moc.sh"
echo ""
echo "Or see QUICKSTART.md for detailed instructions"
echo "================================================================================"
