#!/bin/bash

# Script to run multiple instances of the Spring Boot application for cache synchronization testing

echo "🚀 Starting Redis and Multiple Spring Boot Instances..."
echo "=================================================================================="

# Build and start services with multi-instance profile
echo "📦 Building Docker images and starting services..."
docker compose --profile multi-instance up --build -d

# Wait for services to start
echo "⏳ Waiting for services to start..."
sleep 10

# Check if services are running
echo "🔍 Checking service status..."
docker compose --profile multi-instance ps

echo ""
echo "✅ Multi-instance setup completed!"
echo "=================================================================================="
echo "🌐 Application instances are running on:"
echo "   - Main Instance:    http://localhost:8080"
echo "   - Instance 2:       http://localhost:8081" 
echo "   - Instance 3:       http://localhost:8082"
echo "   - Instance 4:       http://localhost:8083"
echo ""
echo "📊 Redis Dashboard:    redis://localhost:6379"
echo ""
echo "📋 Useful commands:"
echo "   - View all logs:           docker compose --profile multi-instance logs -f"
echo "   - View specific instance:  docker compose --profile multi-instance logs -f app"
echo "   - Stop all instances:      docker compose --profile multi-instance down"
echo "   - Monitor Redis:           docker exec -it \$(docker compose ps -q redis) redis-cli monitor"
echo ""
echo "🔄 The instances will automatically:"
echo "   - Create random products every 10-15 seconds"
echo "   - Create catalogs every 30-45 seconds"
echo "   - Delete items every 20-25 seconds"
echo "   - Access cached items every 8 seconds"
echo "   - Log cache statistics every 30 seconds"
echo ""
echo "📈 Watch the logs to see cache synchronization between instances!"
echo "=================================================================================="