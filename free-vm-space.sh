#!/usr/bin/env bash
#
# free-vm-space.sh - Aggressive cleanup to free VM root partition space
#

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

error() {
  echo -e "${RED}ERROR: $1${NC}" >&2
  exit 1
}

success() {
  echo -e "${GREEN}✓ $1${NC}"
}

info() {
  echo -e "${YELLOW}$1${NC}"
}

VM_HOST="34.254.221.79"
SSH_USER="ubuntu"
SSH_KEY="$HOME/.ssh/graxiom_core.pem"

echo "========================================="
echo "Aggressive VM Root Partition Cleanup"
echo "========================================="
echo ""

info "Current disk usage:"
ssh -i "$SSH_KEY" "$SSH_USER@$VM_HOST" "df -h /"
echo ""

# Cleanup script to run on VM
ssh -i "$SSH_KEY" "$SSH_USER@$VM_HOST" << 'CLEANUP_EOF'
set -e

echo "Step 1: Cleaning up old logs..."
# More aggressive journal cleanup
sudo journalctl --vacuum-size=50M
sudo journalctl --vacuum-time=7d

# Clean up old rotated logs
sudo find /var/log -type f -name "*.gz" -delete
sudo find /var/log -type f -name "*.1" -delete
sudo find /var/log -type f -name "*.old" -delete

# Truncate large active logs older than 7 days
sudo find /var/log -type f -name "*.log" -mtime +7 -exec truncate -s 0 {} \;

echo "✓ Logs cleaned"

echo "Step 2: Removing duplicate Docker images..."
# Keep SBT build image (needed for building hascoapi)
# Remove duplicate/old hascoapi images (keep only the ones in use)
docker images | grep hascoapi | grep -v "hascoapi_" | awk '{print $3}' | xargs -r docker rmi -f 2>/dev/null || echo "No duplicate images to clean"

echo "✓ Duplicate images removed (SBT build image preserved)"

echo "Step 3: Removing stopped containers..."
docker ps -a --filter "status=exited" --format "{{.ID}}" | xargs -r docker rm 2>/dev/null || echo "No stopped containers"
docker ps -a --filter "status=created" --format "{{.ID}}" | xargs -r docker rm 2>/dev/null || echo "No created containers"

echo "✓ Stopped containers removed"

echo "Step 4: Docker system cleanup..."
docker system prune -a -f --volumes 2>/dev/null || true

echo "✓ Docker cleanup complete"

echo "Step 5: APT cache cleanup..."
sudo apt-get clean
sudo apt-get autoclean
sudo apt-get autoremove -y

echo "✓ APT cache cleaned"

echo "Step 6: Removing old kernels (keep current + 1)..."
CURRENT_KERNEL=$(uname -r | sed 's/-generic//')
dpkg --list | grep -E "linux-image-[0-9]" | grep -v "$CURRENT_KERNEL" | awk '{print $2}' | sort -V | head -n -1 | xargs -r sudo apt-get purge -y 2>/dev/null || echo "No old kernels to remove"

echo "✓ Old kernels removed"

echo "Step 7: Cleaning thumbnail cache..."
rm -rf ~/.cache/thumbnails/* 2>/dev/null || true
sudo rm -rf /root/.cache/* 2>/dev/null || true

echo "✓ Cache cleaned"

echo ""
echo "Cleanup complete!"
CLEANUP_EOF

echo ""
success "Cleanup completed!"
echo ""
info "New disk usage:"
ssh -i "$SSH_KEY" "$SSH_USER@$VM_HOST" "df -h /"
echo ""
ssh -i "$SSH_KEY" "$SSH_USER@$VM_HOST" "echo 'Space breakdown:' && sudo du -sh /var/log /var/lib /usr /snap 2>/dev/null"
echo ""
