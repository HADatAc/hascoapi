#!/bin/bash
# Deployment script with detailed logging and progress tracking

set -e

# Configuration
AWS_IP="34.254.221.79"
SSH_KEY="$HOME/.ssh/graxiom_core.pem"
REMOTE_USER="ubuntu"
REMOTE_DIR="/var/data/hascoapi"
LOG_FILE="deployment-$(date +%Y%m%d-%H%M%S).log"

# Color codes for terminal output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging function
log() {
    local level=$1
    shift
    local message="$@"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    case $level in
        INFO)
            echo -e "${BLUE}[INFO]${NC} $message" | tee -a "$LOG_FILE"
            ;;
        SUCCESS)
            echo -e "${GREEN}[SUCCESS]${NC} $message" | tee -a "$LOG_FILE"
            ;;
        WARNING)
            echo -e "${YELLOW}[WARNING]${NC} $message" | tee -a "$LOG_FILE"
            ;;
        ERROR)
            echo -e "${RED}[ERROR]${NC} $message" | tee -a "$LOG_FILE"
            ;;
        STEP)
            echo -e "\n${GREEN}========================================${NC}" | tee -a "$LOG_FILE"
            echo -e "${GREEN}$message${NC}" | tee -a "$LOG_FILE"
            echo -e "${GREEN}========================================${NC}\n" | tee -a "$LOG_FILE"
            ;;
    esac
    echo "[$timestamp] [$level] $message" >> "$LOG_FILE"
}

# Error handler
error_exit() {
    log ERROR "$1"
    log ERROR "Deployment failed. Check $LOG_FILE for details."
    exit 1
}

# Main deployment
log STEP "HASCOAPI Deployment Started"
log INFO "Target: $REMOTE_USER@$AWS_IP"
log INFO "Log file: $LOG_FILE"

# Step 1: Verify SSH connection
log STEP "Step 1/8: Verifying SSH Connection"
if ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no -o ConnectTimeout=10 "$REMOTE_USER@$AWS_IP" "echo 'Connection OK'" >> "$LOG_FILE" 2>&1; then
    log SUCCESS "SSH connection verified"
else
    error_exit "SSH connection failed"
fi

# Step 2: Create Dockerfile
log STEP "Step 2/8: Creating Dockerfile"
cat > Dockerfile.aws-devinfra << 'DOCKERFILE_EOF'
# Multi-stage build - using dependencies container for build tools only
# hadatac-libs/ contains the actual JARs needed (aopalliance, etc.)

FROM kaykael/hascoapi-dependencies:latest AS builder

WORKDIR /hascoapi

# Copy everything including hadatac-libs which has the actual JARs
COPY . .

# Create lib/ directory and copy hadatac-libs to it (SBT default unmanagedBase)
RUN mkdir -p lib && cp hadatac-libs/*.jar lib/

# Build the application - SBT will use lib/ for unmanaged dependencies
RUN sbt playUpdateSecret && sbt dist

# Unzip the distribution (overwrite without prompting)
RUN cd target/universal/ && unzip -qo hascoapi-10.0.1-SNAPSHOT.zip

# Stage 2: Runtime image
FROM eclipse-temurin:11-jre

WORKDIR /hascoapi

COPY --from=builder /hascoapi/target/universal/hascoapi-10.0.1-SNAPSHOT /hascoapi
COPY ./conf/hascoapi-docker.conf /hascoapi/conf/hascoapi.conf
COPY ./docker-entrypoint.sh /hascoapi/docker-entrypoint.sh

RUN chmod +x /hascoapi/docker-entrypoint.sh

EXPOSE 9000

ENTRYPOINT [ "/hascoapi/docker-entrypoint.sh" ]
DOCKERFILE_EOF
log SUCCESS "Dockerfile created"

# Step 3: Create docker-compose file
log STEP "Step 3/8: Creating docker-compose configuration"
cat > docker-compose-aws-devinfra.yml << 'COMPOSE_EOF'
version: '3'

services:
    fuseki:
        build:
            context: ./fuseki
            dockerfile: Dockerfile
        container_name: hascoapi_fuseki
        ports:
            - "3030:3030"
        networks:
            hascoapi:
                aliases:
                    - fuseki
        volumes:
            - /var/data/hascoapi/fuseki-data:/fuseki/databases
        environment:
            - TMPDIR=/var/data/tmp
        tmpfs:
            - /tmp:size=100M,exec
        restart: always

    fuseki-yasgui:
        build:
            context: ./fuseki-yasgui
            dockerfile: Dockerfile
        container_name: hascoapi_fuseki_yasgui
        ports:
            - "8888:8888"
        networks:
            hascoapi:
                aliases:
                    - fuseki-yasgui
        environment:
            DEFAULT_SPARQL_ENDPOINT: "http://localhost:3030/store/sparql"
            TMPDIR: /var/data/tmp
        tmpfs:
            - /tmp:size=100M,exec
        depends_on:
            - fuseki
        restart: always
    
    hascoapi:
        build:
            context: .
            dockerfile: Dockerfile.aws-devinfra
        container_name: hascoapi
        stdin_open: true    
        tty: true
        ports:
            - "9001:9000"
        environment:
            JAVA_OPTS: "-Xms128m -Xmx2560m -Djava.io.tmpdir=/var/data/tmp"
            FUSEKI_URL: "http://fuseki:3030"
            TMPDIR: /var/data/tmp
        volumes:
            - /var/data/hascoapi/logs:/hascoapi/logs
            - /var/data/tmp:/var/data/tmp
        tmpfs:
            - /tmp:size=500M,exec
        depends_on:
            - fuseki
        networks:
            hascoapi:
                aliases:
                    - hascoapi
        restart: always

networks:
    hascoapi:
COMPOSE_EOF
log SUCCESS "docker-compose configuration created"

# Step 4: Create remote directory
log STEP "Step 4/8: Setting up remote directory"
if ssh -i "$SSH_KEY" "$REMOTE_USER@$AWS_IP" "sudo mkdir -p $REMOTE_DIR && sudo chown -R ubuntu:ubuntu /var/data" >> "$LOG_FILE" 2>&1; then
    log SUCCESS "Remote directory configured"
else
    error_exit "Failed to create remote directory"
fi

# Step 5: Copy files to AWS
log STEP "Step 5/8: Copying files to AWS server (this may take a few minutes)"
log INFO "Transferring source code..."
if rsync -avz --progress \
    --exclude='.git' \
    --exclude='target' \
    --exclude='logs' \
    --exclude='node_modules' \
    --exclude='*.log' \
    -e "ssh -i $SSH_KEY" \
    ./ "$REMOTE_USER@$AWS_IP:$REMOTE_DIR/" >> "$LOG_FILE" 2>&1; then
    log SUCCESS "Files transferred successfully"
else
    error_exit "File transfer failed"
fi

# Step 6: Build areating tmp directory on /var/data..."
sudo mkdir -p /var/data/tmp
sudo chmod 1777 /var/data/tmp

echo "[$(date)] Cnd deploy on AWS
log STEP "Step 6/8: Building on AWS server (this will take several minutes)"
log INFO "Pulling dependencies container..."
log INFO "Building HASCOAPI with hadatac-libs..."
log INFO "Building Fuseki..."
log INFO "You can monitor progress in: $LOG_FILE"

ssh -i "$SSH_KEY" "$REMOTE_USER@$AWS_IP" bash -s >> "$LOG_FILE" 2>&1 << 'REMOTE_COMMANDS'
set -e
cd /var/data/hascoapi

echo "[$(date)] Cleaning up old containers..."
sudo docker-compose -f docker-compose-aws-devinfra.yml down || true
sudo docker system prune -af --volumes || true

echo "[$(date)] Pulling dependencies container..."
sudo docker pull kaykael/hascoapi-dependencies:latest

echo "[$(date)] Building all services..."
sudo docker-compose -f docker-compose-aws-devinfra.yml build

echo "[$(date)] Starting services..."
sudo docker-compose -f docker-compose-aws-devinfra.yml up -d

echo "[$(date)] Waiting for services to initialize..."
sleep 15

echo "[$(date)] Deployment complete"
REMOTE_COMMANDS

if [ $? -eq 0 ]; then
    log SUCCESS "Build completed successfully"
else
    error_exit "Build failed on AWS server"
fi

# Step 7: Verify deployment
log STEP "Step 7/8: Verifying deployment"
sleep 5

CONTAINER_STATUS=$(ssh -i "$SSH_KEY" "$REMOTE_USER@$AWS_IP" "cd $REMOTE_DIR && sudo docker-compose -f docker-compose-aws-devinfra.yml ps" 2>&1)
echo "$CONTAINER_STATUS" >> "$LOG_FILE"
log INFO "Container status:"
echo "$CONTAINER_STATUS"

# Step 8: Check logs
log STEP "Step 8/8: Checking application logs"
HASCOAPI_LOGS=$(ssh -i "$SSH_KEY" "$REMOTE_USER@$AWS_IP" "sudo docker logs hascoapi --tail 30" 2>&1)
echo "$HASCOAPI_LOGS" >> "$LOG_FILE"

if echo "$HASCOAPI_LOGS" | grep -q "ClassNotFoundException\|Error\|Exception"; then
    log WARNING "Detected errors in application logs:"
    echo "$HASCOAPI_LOGS" | grep -i "error\|exception\|classnotfound" | head -10
else
    log SUCCESS "No critical errors detected in startup logs"
fi

# Final summary
log STEP "Deployment Summary"
log INFO "Services deployed:"
log INFO "  - HASCOAPI: http://$AWS_IP:9001"
log INFO "  - Fuseki: http://$AWS_IP:3030"
log INFO "  - YASGUI: http://$AWS_IP:8888"
log INFO ""
log INFO "Full deployment log saved to: $LOG_FILE"
log INFO ""
log INFO "Useful commands:"
log INFO "  Check logs:    ssh -i $SSH_KEY $REMOTE_USER@$AWS_IP 'sudo docker logs -f hascoapi'"
log INFO "  Restart:       ssh -i $SSH_KEY $REMOTE_USER@$AWS_IP 'cd $REMOTE_DIR && sudo docker-compose -f docker-compose-aws-devinfra.yml restart'"
log INFO "  Stop:          ssh -i $SSH_KEY $REMOTE_USER@$AWS_IP 'cd $REMOTE_DIR && sudo docker-compose -f docker-compose-aws-devinfra.yml down'"
log INFO ""
log SUCCESS "Deployment process completed!"
