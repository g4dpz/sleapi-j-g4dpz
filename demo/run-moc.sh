#!/bin/bash
# Run MOC Client

echo "Starting MOC Client..."
echo "Press Ctrl+C to stop"
echo ""

cd "$(dirname "$0")"

if [ -f "target/sle-demo-1.0.0-moc.jar" ]; then
    java -jar target/sle-demo-1.0.0-moc.jar
else
    echo "JAR file not found. Building..."
    mvn clean package
    java -jar target/sle-demo-1.0.0-moc.jar
fi
