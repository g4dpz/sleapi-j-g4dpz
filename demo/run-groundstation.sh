#!/bin/bash
# Run Ground Station Server

echo "Starting Ground Station Server..."
echo "Press Ctrl+C to stop"
echo ""

cd "$(dirname "$0")"

if [ -f "target/sle-demo-1.0.0-groundstation.jar" ]; then
    java -jar target/sle-demo-1.0.0-groundstation.jar
else
    echo "JAR file not found. Building..."
    mvn clean package
    java -jar target/sle-demo-1.0.0-groundstation.jar
fi
