#!/bin/bash
# Run Spacecraft Simulator

echo "Starting Spacecraft Simulator..."
echo "Press Ctrl+C to stop"
echo ""

cd "$(dirname "$0")"

if [ -f "target/sle-demo-1.0.0-spacecraft.jar" ]; then
    java -jar target/sle-demo-1.0.0-spacecraft.jar
else
    echo "JAR file not found. Building..."
    mvn clean package
    java -jar target/sle-demo-1.0.0-spacecraft.jar
fi
