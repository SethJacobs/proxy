#!/bin/bash

echo "🚀 Building Funnel Proxy Manager..."

# Clean and build
mvn clean package -DskipTests

if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    echo ""
    echo "To run the application:"
    echo "  java -jar target/funnel-proxy-1.0.0.jar"
    echo ""
    echo "Or with Docker:"
    echo "  docker build -t funnel-proxy ."
    echo "  docker run -p 80:80 -v \$(pwd)/data:/app/data funnel-proxy"
    echo ""
    echo "Access the admin dashboard at: http://localhost/admin"
else
    echo "❌ Build failed!"
    exit 1
fi