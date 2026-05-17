#!/bin/bash
# Build HASCOAPI entirely on AWS VM (no Docker Hub dependencies)
# Wait 1 hour between attempts to avoid Maven rate limiting

echo "=== HASCOAPI Local Build (All Components) ==="
echo ""
echo "This will build all components locally to avoid:"
echo "  - Platform mismatch issues"
echo "  - Docker Hub dependencies"
echo ""

cd /home/ubuntu/hascoapi

# Use the standard docker-compose.yml
echo "Building all services locally..."
echo "This will take 20-30 minutes..."
echo ""

# Build with retries (Maven rate limit may apply)
MAX_ATTEMPTS=3
ATTEMPT=1

while [ $ATTEMPT -le $MAX_ATTEMPTS ]; do
    echo "Build attempt $ATTEMPT of $MAX_ATTEMPTS..."
    
    if sudo docker-compose build; then
        echo ""
        echo "✓ Build successful!"
        break
    else
        if [ $ATTEMPT -lt $MAX_ATTEMPTS ]; then
            echo ""
            echo "✗ Build failed (likely Maven rate limit HTTP 429)"
            echo "Waiting 30 minutes before retry $((ATTEMPT + 1))..."
            sleep 1800  # Wait 30 minutes
        fi
        ATTEMPT=$((ATTEMPT + 1))
    fi
done

if [ $ATTEMPT -gt $MAX_ATTEMPTS ]; then
    echo ""
    echo "ERROR: Build failed after $MAX_ATTEMPTS attempts"
    echo "Maven rate limit is likely active. Wait 1 hour and run:"
    echo "  cd /home/ubuntu/hascoapi && sudo docker-compose build && sudo docker-compose up -d"
    exit 1
fi

# Start services
echo ""
echo "Starting services..."
sudo docker-compose up -d

echo ""
echo "=== Deployment Complete ==="
echo ""
sudo docker-compose ps
echo ""
echo "Access HASCOAPI at:"
echo "  - http://$(curl -s ifconfig.me):9001 (HASCOAPI)"
echo "  - http://$(curl -s ifconfig.me):3030 (Fuseki)"
echo "  - http://$(curl -s ifconfig.me):8888 (YASGUI)"
