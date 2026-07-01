#!/usr/bin/env bash
#
# cleanup-vm-universal.sh - Universal VM Cleanup Script
#
# Cleans up any ESS, INFRA, or KG component from VM root partition
# by reading configuration from infra-config.json.
#
# Usage from component directory:
#   ./cleanup-vm-universal.sh /path/to/infra-config.local.json
#
# Reference: dev/docs/COPILOT-RULES.md (Cloud Deployment Architecture)
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

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Auto-detect component ID from directory name
COMPONENT_ID="$(basename "$SCRIPT_DIR")"

# Parse arguments
if [ $# -lt 1 ]; then
  error "infra-config.local.json path is required.\n\nUsage: $0 /path/to/infra-config.local.json"
fi

CONFIG_FILE="$1"

if [ ! -f "$CONFIG_FILE" ]; then
  error "Configuration file not found: $CONFIG_FILE"
fi

if ! command -v jq >/dev/null 2>&1; then
  error "jq is required but was not found in PATH. Install jq and retry."
fi

if ! jq empty "$CONFIG_FILE" 2>/dev/null; then
  error "Configuration file contains invalid JSON: $CONFIG_FILE"
fi

success "Configuration file found and valid: $CONFIG_FILE"
info "Component: $COMPONENT_ID (auto-detected from directory)"

# Find the system that contains this component
SYSTEM_ID=$(jq -r --arg cid "$COMPONENT_ID" '
  .systems[] | 
  select(.components[]? | .componentId == $cid) | 
  .systemId
' "$CONFIG_FILE" | head -n 1)

if [ -z "$SYSTEM_ID" ]; then
  error "Component '$COMPONENT_ID' not found in infra-config.\nMake sure you're running this from a component directory."
fi

# Extract VM host
DEPLOYMENT_JQ='.systems[] | select(.systemId=="'$SYSTEM_ID'") | .components[] | select(.componentId=="'$COMPONENT_ID'") | .deployment'
VM_HOST=$(jq -r "$DEPLOYMENT_JQ | .vmHost // empty" "$CONFIG_FILE")
VM_NAME=$(jq -r "$DEPLOYMENT_JQ | .vmName // empty" "$CONFIG_FILE")

if [ -z "$VM_HOST" ]; then
  error "Missing deployment.vmHost for $COMPONENT_ID in infra-config"
fi

# SSH configuration
SSH_USER="ubuntu"
SSH_KEY="$HOME/.ssh/graxiom_core.pem"

echo "========================================="
echo "Cleaning up $COMPONENT_ID from VM root partition"
echo "VM: $VM_NAME ($VM_HOST)"
echo "========================================="
echo ""

# Verify SSH connection
info "Verifying SSH connection to $VM_HOST..."
if ! ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no -o ConnectTimeout=10 "$SSH_USER@$VM_HOST" "echo 'OK'" >/dev/null 2>&1; then
  error "SSH connection failed to $SSH_USER@$VM_HOST"
fi
success "SSH connection verified"

# Determine docker compose command
info "Detecting docker compose command..."
DOCKER_COMPOSE_CMD=$(ssh -i "$SSH_KEY" "$SSH_USER@$VM_HOST" 'if command -v "docker-compose" >/dev/null 2>&1; then echo "docker-compose"; else echo "docker compose"; fi')
info "Using: $DOCKER_COMPOSE_CMD"

# Stop containers for this component
info "Stopping $COMPONENT_ID containers..."
ssh -i "$SSH_KEY" "$SSH_USER@$VM_HOST" << STOP_EOF
# Stop containers if they exist
$DOCKER_COMPOSE_CMD -p $COMPONENT_ID down --remove-orphans 2>/dev/null || true

# Also try stopping individual containers by name pattern
docker ps -a --filter "name=$COMPONENT_ID" --format "{{.Names}}" | xargs -r docker stop 2>/dev/null || true
docker ps -a --filter "name=$COMPONENT_ID" --format "{{.Names}}" | xargs -r docker rm 2>/dev/null || true
STOP_EOF
success "Containers stopped"

# Find and remove component directories outside /var/data
info "Searching for $COMPONENT_ID installations outside /var/data..."
ssh -i "$SSH_KEY" "$SSH_USER@$VM_HOST" << CLEANUP_EOF
# Common locations where components might have been deployed
CLEANUP_DIRS=(
  "/opt/$COMPONENT_ID"
  "/home/ubuntu/$COMPONENT_ID"
  "/tmp/$COMPONENT_ID"
  "/usr/local/$COMPONENT_ID"
)

echo "Checking for $COMPONENT_ID installations..."
for dir in "\${CLEANUP_DIRS[@]}"; do
  if [ -d "\$dir" ]; then
    echo "  Found: \$dir"
    sudo rm -rf "\$dir"
    echo "  ✓ Removed: \$dir"
  fi
done

# Remove any stray infra-config files outside /var/data
echo "Checking for stray config files..."
find /home/ubuntu /opt /tmp -name "infra-config*.json" -type f 2>/dev/null | while read file; do
  if [[ "\$file" != "/var/data/infra-config.local.json" ]]; then
    echo "  Found: \$file"
    sudo rm -f "\$file"
    echo "  ✓ Removed: \$file"
  fi
done

echo "Cleanup complete for $COMPONENT_ID"
CLEANUP_EOF

success "Root partition cleaned up"

# Check disk space
info "Checking disk space on VM..."
ssh -i "$SSH_KEY" "$SSH_USER@$VM_HOST" "df -h /"

echo ""
success "Cleanup completed successfully!"
echo ""
info "The VM root partition has been cleaned."
info "All $COMPONENT_ID files should now only be under /var/data/"
echo ""
