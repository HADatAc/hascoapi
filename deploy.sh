#!/usr/bin/env bash
#
# deploy.sh - RDF-HUB-A (HASCOAPI) Deployment Script
#
# Implements the Universal Deployment Rule (Section 4.5)
# Reference: dev/docs/COPILOT-RULES.md
#
# Requirements:
# (a) MUST require a valid infra-config.json instance
# (b) MUST fail clearly if infra-config.json is missing
# (c) MUST use infra-config.json for all configuration parameters
# (d) MUST fail clearly if a required parameter is missing
# (e) MUST complete with clear success or failure outcome
#

set -euo pipefail

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
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

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Check for --stop flag first (can be in any position)
for arg in "$@"; do
    if [ "$arg" = "--stop" ]; then
        echo "Stopping rdf-hub-a (HASCOAPI)..."
        docker-compose -p hascoapi down --remove-orphans 2>&1 | grep -v "^WARN\[" || true
        exit 0
    fi
done

USE_NO_CACHE=false
REBUILD_VOLUME=false
DRY_RUN=false
CONFIG_FILE=""
for arg in "$@"; do
  if [ "$arg" = "--no-cache" ]; then
    USE_NO_CACHE=true
  elif [ "$arg" = "--rebuild-volume" ]; then
    REBUILD_VOLUME=true
  elif [ "$arg" = "--dry-run" ] || [ "$arg" = "--validate-only" ]; then
    DRY_RUN=true
  elif [[ "$arg" == -* ]]; then
    error "Unknown flag: $arg"
  elif [[ "$arg" != -* ]] && [ -z "$CONFIG_FILE" ]; then
    CONFIG_FILE="$arg"
  elif [[ "$arg" != -* ]]; then
    error "Unexpected extra positional argument: $arg"
  fi
done

if [ -z "$CONFIG_FILE" ]; then
  error "infra-config.local.json path is required.\n\nUsage: $0 /path/to/infra-config.local.json [--dry-run] [--no-cache] [--rebuild-volume]"
fi
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

SYSTEM_ID="rdf-hub"
COMPONENT_ID="rdf-hub-a"
COMPONENT_DEPLOYMENT_JQ='.systems[] | select(.systemId=="'$SYSTEM_ID'") | .components[] | select(.componentId=="'$COMPONENT_ID'") | .deployment'

extract_required() {
  local var_name="$1"
  local jq_expr="$2"
  local human_name="$3"
  local value

  value="$(jq -r "$jq_expr // empty" "$CONFIG_FILE")"
  if [ -z "$value" ]; then
    error "Missing required parameter '$human_name' for systemId='$SYSTEM_ID' componentId='$COMPONENT_ID'."
  fi

  export "$var_name=$value"
}

info "Extracting rdf-hub-a configuration parameters..."

extract_required "HOST" "$COMPONENT_DEPLOYMENT_JQ | .host" "deployment.host"
extract_required "PORT" "$COMPONENT_DEPLOYMENT_JQ | .port" "deployment.port"
extract_required "NODE_ENV" "$COMPONENT_DEPLOYMENT_JQ | .nodeEnv" "deployment.nodeEnv"
extract_required "LOG_LEVEL" "$COMPONENT_DEPLOYMENT_JQ | .logLevel" "deployment.logLevel"

success "Configuration extracted successfully"
info "Deployment Configuration:"
info "  Host: $HOST"
info "  Port: $PORT"
info "  Node Environment: $NODE_ENV"
info "  Log Level: $LOG_LEVEL"

if [ "$DRY_RUN" = true ]; then
  success "Dry-run validation passed for rdf-hub-a (HASCOAPI) (no Docker actions executed)."
  exit 0
fi

# Handle volume rebuild if requested
if [ "$REBUILD_VOLUME" = true ]; then
  info "Rebuilding Fuseki volume..."
  docker-compose -p hascoapi down -v --remove-orphans 2>&1 | grep -v "^WARN\[" || true
  docker volume rm hascoapi_hascoapi-fuseki-data 2>/dev/null || true
  success "Fuseki volume removed - will be rebuilt fresh"
fi

# Stop existing containers
info "Stopping existing containers..."
docker-compose -p hascoapi down --remove-orphans 2>&1 | grep -v "^WARN\[" || true

# Build and deploy
if [ "$USE_NO_CACHE" = true ]; then
  info "Building images with --no-cache..."
  docker-compose build --no-cache
else
  info "Building images..."
  docker-compose build
fi

success "Images built successfully"

# Set up environment file for docker-compose
cat > .env.deploy << EOF
HOST=$HOST
PORT=$PORT
NODE_ENV=$NODE_ENV
LOG_LEVEL=$LOG_LEVEL
EOF

info "Starting containers..."
docker-compose -p hascoapi up -d

# Clean up
rm -f .env.deploy

success "RDF-HUB-A (HASCOAPI) deployed successfully"
info "Fuseki is available at: http://localhost:3030"
info "HASCOAPI is available at: http://localhost:$PORT"
info "Fuseki YASGUI is available at: http://localhost:8888"

# Wait for services to be ready
info "Waiting for services to start..."
sleep 5

# Check if containers are running
if docker-compose -p hascoapi ps | grep -q "Up"; then
  success "All services are running"
  docker-compose -p hascoapi ps
else
  error "Some services failed to start. Check logs with: docker-compose -p hascoapi logs"
fi
