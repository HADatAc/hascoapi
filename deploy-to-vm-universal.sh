#!/usr/bin/env bash
#
# deploy-to-vm-universal.sh - Universal Cloud VM Deployment Orchestrator
#
# Works with ANY ESS, INFRA, or KG component by reading configuration
# from infra-config.json following COPILOT RULES.
#
# Usage from component directory:
#   ./deploy-to-vm-universal.sh /path/to/infra-config.local.json [--no-cache] [--rebuild-volume]
#
# Reference: dev/docs/COPILOT-RULES.md (Cloud Deployment Architecture)
#

set -euo pipefail

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
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

step() {
  echo ""
  echo -e "${BLUE}========================================${NC}"
  echo -e "${BLUE}$1${NC}"
  echo -e "${BLUE}========================================${NC}"
}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Auto-detect component ID from directory name
COMPONENT_ID="$(basename "$SCRIPT_DIR")"

# Parse arguments
if [ $# -lt 1 ]; then
  error "infra-config.local.json path is required.\n\nUsage: $0 /path/to/infra-config.local.json [--no-cache] [--rebuild-volume]"
fi

CONFIG_FILE="$1"
shift

# Collect additional flags
DEPLOY_FLAGS=""
for arg in "$@"; do
  if [ "$arg" = "--no-cache" ] || [ "$arg" = "--rebuild-volume" ]; then
    DEPLOY_FLAGS="$DEPLOY_FLAGS $arg"
  fi
done

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
  error "Component '$COMPONENT_ID' not found in infra-config.\nMake sure you're running this from a component directory and the component exists in infra-config."
fi

success "Found in system: $SYSTEM_ID"

# Extract component deployment configuration
COMPONENT_JQ='.systems[] | select(.systemId=="'$SYSTEM_ID'") | .components[] | select(.componentId=="'$COMPONENT_ID'")'
DEPLOYMENT_JQ="$COMPONENT_JQ | .deployment"

# Extract VM deployment information
VM_HOST=$(jq -r "$DEPLOYMENT_JQ | .vmHost // empty" "$CONFIG_FILE")
VM_NAME=$(jq -r "$DEPLOYMENT_JQ | .vmName // empty" "$CONFIG_FILE")
COMPONENT_TYPE=$(jq -r "$COMPONENT_JQ | .componentType // empty" "$CONFIG_FILE")

if [ -z "$VM_HOST" ]; then
  error "Missing deployment.vmHost for $COMPONENT_ID in infra-config.\nThis component may not be configured for cloud deployment."
fi

if [ -z "$VM_NAME" ]; then
  error "Missing deployment.vmName for $COMPONENT_ID in infra-config"
fi

# SSH configuration (standardized across all VMs)
SSH_USER="ubuntu"
SSH_KEY="$HOME/.ssh/graxiom_core.pem"
REMOTE_DIR="/var/data/$COMPONENT_ID"
OFFICIAL_CLOUD_INFRA_CONFIG="/var/data/infra-config.local.json"

success "VM Deployment Configuration:"
info "  Component: $COMPONENT_ID"
info "  System: $SYSTEM_ID"
info "  Type: $COMPONENT_TYPE"
info "  VM Name: $VM_NAME"
info "  VM Host: $VM_HOST"
info "  Remote Directory: $REMOTE_DIR"
info "  Config Path: $OFFICIAL_CLOUD_INFRA_CONFIG"

# Verify SSH key exists
if [ ! -f "$SSH_KEY" ]; then
  error "SSH key not found: $SSH_KEY"
fi

step "Step 1/5: Verifying SSH Connection to $VM_NAME"
if ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no -o ConnectTimeout=10 "$SSH_USER@$VM_HOST" "echo 'Connection OK'" >/dev/null 2>&1; then
  success "SSH connection verified"
else
  error "SSH connection failed to $SSH_USER@$VM_HOST"
fi

step "Step 2/5: Deploying OFFICIAL-CLOUD-INFRA-CONFIG to VM"
info "Copying $CONFIG_FILE to $VM_HOST:$OFFICIAL_CLOUD_INFRA_CONFIG"

# Ensure /var/data directory exists with correct permissions
info "Setting up /var/data directory on VM..."
ssh -i "$SSH_KEY" "$SSH_USER@$VM_HOST" << 'SETUP_EOF'
sudo mkdir -p /var/data
sudo chown -R ubuntu:ubuntu /var/data
sudo chmod 755 /var/data
SETUP_EOF

# Copy config to VM via temp location first, then move with sudo
info "Uploading configuration file..."
scp -i "$SSH_KEY" "$CONFIG_FILE" "$SSH_USER@$VM_HOST:/tmp/infra-config.local.json"
ssh -i "$SSH_KEY" "$SSH_USER@$VM_HOST" "sudo mv /tmp/infra-config.local.json $OFFICIAL_CLOUD_INFRA_CONFIG && sudo chown ubuntu:ubuntu $OFFICIAL_CLOUD_INFRA_CONFIG"

# Verify config was copied correctly
REMOTE_CONFIG_CHECK=$(ssh -i "$SSH_KEY" "$SSH_USER@$VM_HOST" "cat $OFFICIAL_CLOUD_INFRA_CONFIG | jq -r '.schemaVersion // empty'")
if [ -n "$REMOTE_CONFIG_CHECK" ]; then
  success "OFFICIAL-CLOUD-INFRA-CONFIG deployed to VM"
  info "  Schema version: $REMOTE_CONFIG_CHECK"
else
  error "Failed to verify config on VM"
fi

step "Step 3/5: Copying $COMPONENT_ID Code to VM"
info "Creating remote directory: $REMOTE_DIR"
ssh -i "$SSH_KEY" "$SSH_USER@$VM_HOST" << MKDIR_EOF
sudo mkdir -p $REMOTE_DIR
sudo chown -R ubuntu:ubuntu $REMOTE_DIR
MKDIR_EOF

info "Synchronizing code to VM (excluding node_modules, .git, target)..."
rsync -avz --progress \
  --exclude='node_modules' \
  --exclude='.git' \
  --exclude='target' \
  --exclude='*.log' \
  --exclude='deployment-*.log' \
  --exclude='dist' \
  -e "ssh -i $SSH_KEY" \
  ./ "$SSH_USER@$VM_HOST:$REMOTE_DIR/"

success "Code synchronized to VM"

step "Step 4/5: Stopping Existing Containers on VM"
info "Stopping $COMPONENT_ID containers..."
ssh -i "$SSH_KEY" "$SSH_USER@$VM_HOST" "cd $REMOTE_DIR && ./deploy.sh $OFFICIAL_CLOUD_INFRA_CONFIG --stop" 2>/dev/null || true
success "Existing containers stopped"

step "Step 5/5: Deploying $COMPONENT_ID on VM"
info "Executing deployment on VM..."
info "Deploy flags: $DEPLOY_FLAGS"

# Execute deployment on VM
ssh -i "$SSH_KEY" -t "$SSH_USER@$VM_HOST" << ENDSSH
cd $REMOTE_DIR
echo "========================================="
echo "Deploying $COMPONENT_ID on $VM_NAME"
echo "========================================="
./deploy.sh $OFFICIAL_CLOUD_INFRA_CONFIG $DEPLOY_FLAGS
ENDSSH

success "Deployment completed successfully!"

step "Deployment Summary"
echo ""
success "$COMPONENT_ID has been deployed to $VM_NAME ($VM_HOST)"
echo ""
info "Component directory on VM: $REMOTE_DIR"
echo ""
info "To view logs on VM:"
info "  ssh -i $SSH_KEY $SSH_USER@$VM_HOST 'cd $REMOTE_DIR && docker compose logs -f'"
echo ""
info "To stop services:"
info "  ssh -i $SSH_KEY $SSH_USER@$VM_HOST 'cd $REMOTE_DIR && ./deploy.sh $OFFICIAL_CLOUD_INFRA_CONFIG --stop'"
echo ""
