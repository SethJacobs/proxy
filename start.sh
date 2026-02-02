#!/bin/bash

echo "🚀 Starting Funnel Proxy Manager..."

# Check if jar exists
if [ ! -f "target/funnel-proxy-1.0.0.jar" ]; then
    echo "📦 Building application first..."
    ./build.sh
    if [ $? -ne 0 ]; then
        echo "❌ Build failed, cannot start application"
        exit 1
    fi
fi

# Create data directory if it doesn't exist
mkdir -p data

echo "🌐 Starting server on port 80..."
echo "📊 Admin dashboard: http://localhost/admin"
echo "🗄️  Database console: http://localhost/h2-console"
echo ""
echo "Press Ctrl+C to stop the server"
echo ""

# Start the application
java -jar target/funnel-proxy-1.0.0.jar