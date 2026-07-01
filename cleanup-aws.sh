#!/bin/bash
# Cleanup AWS server and reconfigure Docker to use /var/data

set -e

AWS_IP="34.254.221.79"
SSH_KEY="$HOME/.ssh/graxiom_core.pem"
REMOTE_USER="ubuntu"

echo "=== Cleaning up AWS server and reconfiguring Docker ==="
echo "Target: $REMOTE_USER@$AWS_IP"
echo ""

ssh -i "$SSH_KEY" "$REMOTE_USER@$AWS_IP" << 'REMOTE_COMMANDS'
set -e

echo "Step 1: Checking disk usage..."
df -h

echo ""
echo "Step 2: Stopping all Docker containers..."
sudo docker stop $(sudo docker ps -aq) 2>/dev/null || true

echo ""
echo "Step 3: Removing all Docker containers, images, and volumes..."
sudo docker system prune -af --volumes

echo ""
echo "Step 4: Cleaning up old hascoapi directories..."
sudo rm -rf /home/ubuntu/hascoapi || true
sudo rm -rf /tmp/* || true

echo ""
echo "Step 5: Creating /var/data directories with proper permissions..."
sudo mkdir -p /var/data/hascoapi
sudo mkdir -p /var/data/docker
sudo chown -R ubuntu:ubuntu /var/data

echo ""
echo "Step 6: Reconfiguring Docker to use /var/data/docker..."
sudo systemctl stop docker || true

# Backup existing Docker data
if [ -d "/var/lib/docker" ]; then
    echo "Moving existing Docker data to /var/data/docker..."
    sudo rsync -a /var/lib/docker/ /var/data/docker/ || true
fi

# Create daemon.json to point Docker to new location
sudo mkdir -p /etc/docker
cat << 'EOF' | sudo tee /etc/docker/daemon.json
{
  "data-root": "/var/data/docker",
  "storage-driver": "overlay2"
}
EOF

# Remove old Docker directory
sudo rm -rf /var/lib/docker

echo ""
echo "Step 7: Starting Docker with new configuration..."
sudo systemctl start docker
sudo systemctl enable docker

echo ""
echo "Step 8: Verifying Docker configuration..."
sudo docker info | grep "Docker Root Dir"

echo ""
echo "Step 9: Final disk usage..."
df -h

echo ""
echo "=== Cleanup Complete ==="

REMOTE_COMMANDS

echo ""
echo "Server is cleaned up and Docker is configured to use /var/data/docker"
echo "You can now run the deployment script."
echo ""
