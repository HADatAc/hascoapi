#!/bin/bash
# Build HASCOAPI dependency cache image and push to Docker Hub
# Run this when Maven Central rate limit has cleared (wait 12-24 hours)

set -e

echo "=== Building hascoapi-deps:latest for AMD64 platform ==="

# Build for AMD64 platform (AWS VM architecture)
docker buildx build \
  --platform linux/amd64 \
  -f Dockerfile.deps \
  -t paulopinheiro1234/hascoapi-deps:latest \
  --push \
  .

echo ""
echo "✓ Image pushed to: paulopinheiro1234/hascoapi-deps:latest"
echo ""
echo "Next: Update Dockerfile to use this base image:"
echo "  FROM paulopinheiro1234/hascoapi-deps:latest"
