#!/bin/bash
# Deploy HASCOAPI (devinfra branch) to AWS using kaykael/hascoapi-dependencies

set -e

# Configuration
AWS_IP="34.254.221.79"
SSH_KEY="$HOME/.ssh/graxiom_core.pem"
REMOTE_USER="ubuntu"
REMOTE_DIR="/var/data/hascoapi"

echo "=== HASCOAPI DevInfra Deployment to AWS ==="
echo "Target: $REMOTE_USER@$AWS_IP"
echo "Using dependencies: kaykael/hascoapi-dependencies"
echo ""

# Check if SSH key exists
if [ ! -f "$SSH_KEY" ]; then
    echo "Error: SSH key not found at $SSH_KEY"
    exit 1
fi

# Create Dockerfile that uses the dependencies container
cat > Dockerfile.aws-devinfra << 'DOCKERFILE_EOF'
# Multi-stage build - using dependencies container for build tools only
# hadatac-libs/ contains the actual JARs needed (aopalliance, etc.)

# Stage 1: Build application
FROM kaykael/hascoapi-dependencies:latest AS builder

WORKDIR /hascoapi

# Copy everything including hadatac-libs which has the actual JARs
COPY . .

# Create lib/ directory and symlink hadatac-libs to it (SBT default unmanagedBase)
RUN mkdir -p lib && cp hadatac-libs/*.jar lib/

# Build the application - SBT will use lib/ for unmanaged dependencies
RUN sbt playUpdateSecret && sbt dist

# Unzip the distribution
RUN cd target/universal/ && \
    unzip -q hascoapi-10.0.1-SNAPSHOT.zip

# Stage 2: Runtime image (minimal, production-ready)
FROM eclipse-temurin:11-jre

WORKDIR /hascoapi

# Copy the complete built application from builder stage
COPY --from=builder /hascoapi/target/universal/hascoapi-10.0.1-SNAPSHOT /hascoapi

# Copy configuration files (overwrite defaults)
COPY ./conf/hascoapi-docker.conf /hascoapi/conf/hascoapi.conf
COPY ./docker-entrypoint.sh /hascoapi/docker-entrypoint.sh
COPY ./load-pharma-ontology.sh /hascoapi/load-pharma-ontology.sh

# Copy pharma ontology
RUN mkdir -p /var/hascoapi/app_ontology
COPY ./app_ontology/pharma.owl /var/hascoapi/app_ontology/pharma.owl

# Make scripts executable
RUN chmod +x /hascoapi/docker-entrypoint.sh && \
    chmod +x /hascoapi/load-pharma-ontology.sh

EXPOSE 9000

ENTRYPOINT [ "/hascoapi/docker-entrypoint.sh" ]
DOCKERFILE_EOF

# Create docker-compose file for AWS deployment
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
            JAVA_OPTS: "-Xms128m -Xmx2560m"
            FUSEKI_URL: "http://fuseki:3030"
        volumes:
            - /var/data/hascoapi/logs:/hascoapi/logs
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

echo "Step 1: Testing SSH connection..."
ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no -o ConnectTimeout=10 "$REMOTE_USER@$AWS_IP" "echo 'SSH connection successful'"

echo ""
echo "Step 2: Creating remote directory..."
ssh -i "$SSH_KEY" "$REMOTE_USER@$AWS_IP" "sudo mkdir -p $REMOTE_DIR && sudo chown -R ubuntu:ubuntu /var/data"

echo ""
echo "Step 3: Copying files to AWS server..."
# Create a temporary directory with only the files we need
TEMP_DIR=$(mktemp -d)
trap "rm -rf $TEMP_DIR" EXIT

# Copy necessary files
rsync -avz --progress -e "ssh -i $SSH_KEY" \
    --exclude='.git' \
    --exclude='target' \
    --exclude='logs' \
    --exclude='node_modules' \
    --exclude='*.log' \
    ./ "$REMOTE_USER@$AWS_IP:$REMOTE_DIR/"

echo ""
echo "Step 4: Building and deploying on AWS server..."
ssh -i "$SSH_KEY" "$REMOTE_USER@$AWS_IP" << 'REMOTE_COMMANDS'
set -e

cd /var/data/hascoapi

echo "Cleaning up old containers and images..."
sudo docker-compose -f docker-compose-aws-devinfra.yml down || true
sudo docker system prune -af --volumes || true

echo ""
echo "Installing Docker if not present..."
if ! command -v docker &> /dev/null; then
    sudo apt-get update
    sudo apt-get install -y docker.io docker-compose
    sudo systemctl start docker
    sudo systemctl enable docker
    sudo usermod -aG docker ubuntu
fi

echo ""
echo "Pulling dependencies container..."
sudo docker pull kaykael/hascoapi-dependencies:latest

echo ""
echo "Building all services..."
sudo docker-compose -f docker-compose-aws-devinfra.yml build

echo ""
echo "Starting all services..."
sudo docker-compose -f docker-compose-aws-devinfra.yml up -d

echo ""
echo "Waiting for services to start..."
sleep 10

echo ""
echo "=== Deployment Status ==="
sudo docker-compose -f docker-compose-aws-devinfra.yml ps

echo ""
echo "=== Container Logs (last 20 lines) ==="
sudo docker logs hascoapi --tail 20

REMOTE_COMMANDS

echo ""
echo "=== Deployment Complete ==="
echo ""
echo "Services:"
echo "  - HASCOAPI: http://$AWS_IP:9001"
echo "  - Fuseki: http://$AWS_IP:3030"
echo "  - YASGUI: http://$AWS_IP:8888"
echo ""
echo "To check logs:"
echo "  ssh -i $SSH_KEY $REMOTE_USER@$AWS_IP 'sudo docker logs -f hascoapi'"
echo ""
echo "To restart services:"
echo "  ssh -i $SSH_KEY $REMOTE_USER@$AWS_IP 'cd $REMOTE_DIR && sudo docker-compose -f docker-compose-aws-devinfra.yml restart'"
echo ""
