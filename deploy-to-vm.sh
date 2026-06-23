#!/usr/bin/env bash
#
# deploy-to-vm.sh - RDF-HUB-A Cloud Deployment Orchestrator
#
# This script orchestrates deployment of rdf-hub-a (HASCOAPI) to a cloud VM
# following the COPILOT RULES for cloud deployment.
#
# Reference: dev/docs/COPILOT-RULES.md (Cloud Deployment Architecture)
#
# Workflow:
# 1. Verify local infra-config.local.json
# 2. Copy infra-config to VM at OFFICIAL-CLOUD-INFRA-CONFIG path
# 3. Copy hascoapi code to VM
# 4. Execute deploy.sh on VM with fresh volumes
#

set -euo pipefail

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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
cd "$SCRIPT_DIR"

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

# Extract VM deployment information from infra-config
SYSTEM_ID="rdf-hub"
COMPONENT_ID="rdf-hub-a"
COMPONENT_DEPLOYMENT_JQ='.systems[] | select(.systemId=="'$SYSTEM_ID'") | .components[] | select(.componentId=="'$COMPONENT_ID'") | .deployment'

VM_HOST=$(jq -r "$COMPONENT_DEPLOYMENT_JQ | .vmHost // empty" "$CONFIG_FILE")
VM_NAME=$(jq -r "$COMPONENT_DEPLOYMENT_JQ | .vmName // empty" "$CONFIG_FILE")

if [ -z "$VM_HOST" ]; then
  error "Missing deployment.vmHost for rdf-hub-a in infra-config"
fi

if [ -z "$VM_NAME" ]; then
  error "Missing deployment.vmName for rdf-hub-a in infra-config"
fi

# SSH configuration
SSH_USER="ubuntu"
SSH_KEY="$HOME/.ssh/graxiom_core.pem"
REMOTE_DIR="/var/data/hascoapi"
OFFICIAL_CLOUD_INFRA_CONFIG="/var/data/infra-config.local.json"

success "VM Deployment Configuration:"
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

step "Step 3/5: Copying HASCOAPI Code to VM"
info "Creating remote directory: $REMOTE_DIR"
ssh -i "$SSH_KEY" "$SSH_USER@$VM_HOST" << 'MKDIR_EOF'
sudo mkdir -p /var/data/hascoapi
sudo chown -R ubuntu:ubuntu /var/data/hascoapi
MKDIR_EOF

info "Synchronizing code to VM (excluding node_modules, .git, target)..."
rsync -avz --progress \
  --exclude='node_modules' \
  --exclude='.git' \
  --exclude='target' \
  --exclude='*.log' \
  --exclude='deployment-*.log' \
  -e "ssh -i $SSH_KEY" \
  ./ "$SSH_USER@$VM_HOST:$REMOTE_DIR/"

success "Code synchronized to VM"

step "Step 4/5: Stopping Existing Containers on VM"
info "Stopping rdf-hub-a containers..."
ssh -i "$SSH_KEY" "$SSH_USER@$VM_HOST" "cd $REMOTE_DIR && ./deploy.sh $OFFICIAL_CLOUD_INFRA_CONFIG --stop" || true
success "Existing containers stopped"

step "Step 5/5: Deploying RDF-HUB-A on VM with Fresh Volumes"
info "Executing deployment on VM..."
info "Deploy flags: $DEPLOY_FLAGS"

# Execute deployment on VM
ssh -i "$SSH_KEY" -t "$SSH_USER@$VM_HOST" << ENDSSH
cd $REMOTE_DIR
echo "========================================="
echo "Deploying RDF-HUB-A (HASCOAPI) on $VM_NAME"
echo "========================================="
./deploy.sh $OFFICIAL_CLOUD_INFRA_CONFIG $DEPLOY_FLAGS
ENDSSH

success "Deployment completed successfully!"

step "Deployment Summary"
echo ""
success "RDF-HUB-A has been deployed to $VM_NAME ($VM_HOST)"
echo ""
info "Services available at:"
info "  Fuseki: http://$VM_HOST:3030"
info "  HASCOAPI: http://$VM_HOST:9001"
info "  Fuseki YASGUI: http://$VM_HOST:8888"
echo ""
info "To view logs on VM:"
info "  ssh -i $SSH_KEY $SSH_USER@$VM_HOST 'cd $REMOTE_DIR && docker compose -p hascoapi logs -f'"
echo ""
info "To stop services:"
info "  ssh -i $SSH_KEY $SSH_USER@$VM_HOST 'cd $REMOTE_DIR && ./deploy.sh $OFFICIAL_CLOUD_INFRA_CONFIG --stop'"
echo ""
