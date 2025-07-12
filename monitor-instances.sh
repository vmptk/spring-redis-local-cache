#!/bin/bash

# Script to monitor cache synchronization between instances

echo "📊 Cache Synchronization Monitor"
echo "=================================================================================="

# Function to check if services are running
check_services() {
    echo "🔍 Checking service status..."
    docker compose --profile multi-instance ps
    echo ""
}

# Function to show logs from all instances
show_logs() {
    echo "📋 Showing logs from all instances (press Ctrl+C to stop)..."
    docker compose --profile multi-instance logs -f --tail=50
}

# Function to monitor Redis commands
monitor_redis() {
    echo "🔍 Monitoring Redis commands (press Ctrl+C to stop)..."
    docker exec -it $(docker compose ps -q redis) redis-cli monitor
}

# Function to check Redis keys
check_redis_keys() {
    echo "🔑 Current Redis keys:"
    docker exec -it $(docker compose ps -q redis) redis-cli --scan --pattern "*" | head -20
    echo ""
    echo "📊 Redis info:"
    docker exec -it $(docker compose ps -q redis) redis-cli info stats | grep -E "(connected_clients|total_commands_processed|keyspace_hits|keyspace_misses)"
    echo ""
}

# Function to test cache endpoints
test_endpoints() {
    echo "🧪 Testing cache endpoints..."
    
    echo "Getting detailed cache statistics from each instance..."
    echo "================================================================================"
    
    echo "📊 Instance 1 (8080) - Cache Summary:"
    curl -s http://localhost:8080/api/cache/summary 2>/dev/null || echo "Instance not responding"
    echo ""
    echo "📊 Instance 2 (8081) - Cache Summary:"
    curl -s http://localhost:8081/api/cache/summary 2>/dev/null || echo "Instance not responding"
    echo ""
    echo "📊 Instance 3 (8082) - Cache Summary:"
    curl -s http://localhost:8082/api/cache/summary 2>/dev/null || echo "Instance not responding"
    echo ""
    echo "📊 Instance 4 (8083) - Cache Summary:"
    curl -s http://localhost:8083/api/cache/summary 2>/dev/null || echo "Instance not responding"
    echo ""
    
    echo "Getting detailed JSON metrics from Instance 1..."
    echo "📈 Detailed metrics (JSON):"
    curl -s http://localhost:8080/api/cache/metrics | jq '.' 2>/dev/null || curl -s http://localhost:8080/api/cache/metrics
    echo ""
}

# Function to stop all instances
stop_instances() {
    echo "🛑 Stopping all instances..."
    docker compose --profile multi-instance down
    echo "✅ All instances stopped"
}

# Main menu
while true; do
    echo "Choose an option:"
    echo "1) Check service status"
    echo "2) Show application logs"
    echo "3) Monitor Redis commands"
    echo "4) Check Redis keys and stats"
    echo "5) Test cache endpoints"
    echo "6) Stop all instances"
    echo "7) Exit"
    echo ""
    read -p "Enter your choice (1-7): " choice
    
    case $choice in
        1)
            check_services
            ;;
        2)
            show_logs
            ;;
        3)
            monitor_redis
            ;;
        4)
            check_redis_keys
            ;;
        5)
            test_endpoints
            ;;
        6)
            stop_instances
            break
            ;;
        7)
            echo "👋 Goodbye!"
            break
            ;;
        *)
            echo "❌ Invalid option. Please try again."
            ;;
    esac
    echo ""
done