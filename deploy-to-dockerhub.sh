#!/bin/bash
# Deploy HASCOAPI images to Docker Hub (Public & Free)
# Credentials are NOT stored - they are used only for this session

set -e

echo "=== Docker Hub Deployment Script ==="
echo ""

# Ask for username
read -p "Enter your Docker Hub username: " DOCKERHUB_USERNAME

if [ -z "$DOCKERHUB_USERNAME" ]; then
    echo "Error: Username cannot be empty"
    exit 1
fi

# Ask for password (hidden input)
echo "Enter your Docker Hub password (input will be hidden):"
read -s DOCKERHUB_PASSWORD

if [ -z "$DOCKERHUB_PASSWORD" ]; then
    echo "Error: Password cannot be empty"
    exit 1
fi

echo ""
echo "Username: $DOCKERHUB_USERNAME"
echo ""

# Login to Docker Hub using credentials (not stored)
echo "Logging in to Docker Hub..."
echo "$DOCKERHUB_PASSWORD" | docker login --username "$DOCKERHUB_USERNAME" --password-stdin

# Tag images for Docker Hub
echo ""
echo "Tagging images..."
docker tag hascoapi-hascoapi:latest $DOCKERHUB_USERNAME/hascoapi:latest
docker tag hascoapi-fuseki:latest $DOCKERHUB_USERNAME/hascoapi-fuseki:latest
docker tag hascoapi-fuseki-yasgui:latest $DOCKERHUB_USERNAME/hascoapi-fuseki-yasgui:latest

# Push to Docker Hub
echo ""
echo "Pushing to Docker Hub..."
docker push $DOCKERHUB_USERNAME/hascoapi:latest
docker push $DOCKERHUB_USERNAME/hascoapi-fuseki:latest
docker push $DOCKERHUB_USERNAME/hascoapi-fuseki-yasgui:latest

# Logout to clear credentials
echo ""
echo "Logging out from Docker Hub..."
docker logout

# Clear password from memory
unset DOCKERHUB_PASSWORD

echo ""
echo "=== Push Complete ==="
echo "Public images available at:"
echo "  - docker.io/$DOCKERHUB_USERNAME/hascoapi:latest"
echo "  - docker.io/$DOCKERHUB_USERNAME/hascoapi-fuseki:latest"
echo "  - docker.io/$DOCKERHUB_USERNAME/hascoapi-fuseki-yasgui:latest"
echo ""
echo "Anyone can pull with:"
echo "  docker pull $DOCKERHUB_USERNAME/hascoapi:latest"
echo ""
echo "Note: You have been logged out. Credentials were NOT stored."
