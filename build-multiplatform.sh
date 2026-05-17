#!/bin/bash
# Rebuild HASCOAPI images for multiple platforms (ARM64 + AMD64)

set -e

echo "=== Multi-Platform Docker Build and Push ==="
echo ""

# Login to Docker Hub
echo "Logging in to Docker Hub..."
echo "Abacaxi1357!" | docker login --username paulopinheiro1234 --password-stdin

# Build and push hascoapi for multiple platforms
echo ""
echo "Building hascoapi for linux/amd64 and linux/arm64..."
docker buildx build --platform linux/amd64,linux/arm64 \
    -t paulopinheiro1234/hascoapi:latest \
    -f Dockerfile \
    --push \
    .

# Build and push hascoapi-fuseki
echo ""
echo "Building hascoapi-fuseki for linux/amd64 and linux/arm64..."
docker buildx build --platform linux/amd64,linux/arm64 \
    -t paulopinheiro1234/hascoapi-fuseki:latest \
    -f fuseki/Dockerfile \
    --push \
    fuseki/

# Build and push hascoapi-fuseki-yasgui
echo ""
echo "Building hascoapi-fuseki-yasgui for linux/amd64 and linux/arm64..."
docker buildx build --platform linux/amd64,linux/arm64 \
    -t paulopinheiro1234/hascoapi-fuseki-yasgui:latest \
    -f fuseki-yasgui/Dockerfile \
    --push \
    fuseki-yasgui/

# Logout
echo ""
echo "Logging out..."
docker logout

echo ""
echo "=== Build Complete ==="
echo "All images now support both AMD64 and ARM64 platforms!"
echo ""
echo "Images available at:"
echo "  - paulopinheiro1234/hascoapi:latest"
echo "  - paulopinheiro1234/hascoapi-fuseki:latest"
echo "  - paulopinheiro1234/hascoapi-fuseki-yasgui:latest"
