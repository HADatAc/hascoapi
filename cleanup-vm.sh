#!/usr/bin/env bash
#
# cleanup-vm.sh - Clean up hascoapi from VM root partition
#
# This script removes any hascoapi files deployed outside of /var/data/
# to free up space on the root partition.
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

# VM configuration
VM_HOST="34.254.221.79"
SSH_USER="ubuntu"
SSH_KEY="$HOME/.ssh/graxiom_core.pem"

echo "========================================="
echo "Cleaning up hascoapi from VM root partition"
echo "========================================="
echo ""

# Verify SSH connection
info "Verifying SSH connection to $VM_HOST..."
if ! ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no -o ConnectTimeout=10 "$SSH_USER@$VM_HOST" "echo 'OK'" >/dev/null 2>&1; then
  error "SSH connection failed to $SSH_USER@$VM_HOST"
fi
success "SSH connection verified"

# Stop all hascoapi containers first
info "Stopping hascoapi containers..."
ssh -i "$SSH_KEY" "$SSH_USER@$VM_HOST" << 'STOP_EOF'
# Stop containers if they exist
docker-compose -p hascoapi down --remove-orphans 2>/dev/null || true
docker stop hascoapi hascoapi_fuseki hascoapi_fuseki_yasgui 2>/dev/null || true
docker rm hascoapi hascoapi_fuseki hascoapi_fuseki_yasgui 2>/dev/null || true
STOP_EOF
success "Containers stopped"

# Find and remove hascoapi directories outside /var/data
info "Searching for hascoapi installations outside /var/data..."
ssh -i "$SSH_KEY" "$SSH_USER@$VM_HOST" << 'CLEANUP_EOF'
# Common locations where hascoapi might have been deployed
CLEANUP_DIRS=(
  "/opt/hascoapi"
  "/home/ubuntu/hascoapi"
  "/tmp/hascoapi"
  "/usr/local/hascoapi"
)

echo "Checking for hascoapi installations..."
for dir in "${CLEANUP_DIRS[@]}"; do
  if [ -d "$dir" ]; then
    echo "  Found: $dir"
    sudo rm -rf "$dir"
    echo "  ✓ Removed: $dir"
  fi
done

# Remove any stray infra-config files outside /var/data
echo "Checking for stray config files..."
find /home/ubuntu /opt /tmp -name "infra-config*.json" -type f 2>/dev/null | while read file; do
  if [[ "$file" != "/var/data/infra-config.local.json" ]]; then
    echo "  Found: $file"
    sudo rm -f "$file"
    echo "  ✓ Removed: $file"
  fi
done

# Clean up Docker build cache and unused volumes to free space
echo "Cleaning Docker build cache..."
docker system prune -f 2>/dev/null || true
docker volume prune -f 2>/dev/null || true

echo "Cleanup complete"
CLEANUP_EOF

success "Root partition cleaned up"

# Check disk space
info "Checking disk space on VM..."
ssh -i "$SSH_KEY" "$SSH_USER@$VM_HOST" "df -h /"

echo ""
success "Cleanup completed successfully!"
echo ""
info "The VM root partition has been cleaned."
info "All hascoapi files should now only be under /var/data/"
echo ""
