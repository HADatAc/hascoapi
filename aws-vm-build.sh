#!/bin/bash
# AWS VM Setup Script - Build HASCOAPI locally on VM

echo "=== HASCOAPI AWS VM Build & Deploy ==="
echo ""

# Install dependencies
echo "Installing dependencies..."
sudo apt-get update
sudo apt-get install -y git docker.io docker-compose openjdk-11-jdk

# Start Docker
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -a -G docker ubuntu

# Clone repository (or use existing)
if [ ! -d "/home/ubuntu/hascoapi" ]; then
    echo "Cloning HASCOAPI..."
    cd /home/ubuntu
    git clone https://github.com/HADatAc/hascoapi.git
    cd hascoapi
else
    echo "Using existing HASCOAPI directory..."
    cd /home/ubuntu/hascoapi
    git pull
fi

# Create docker-compose.yml
cat > docker-compose.yml << 'EOF'
version: '3'

services:
    fuseki:
        image: paulopinheiro1234/hascoapi-fuseki:latest
        container_name: hascoapi_fuseki
        ports:
            - "3030:3030"
        networks:
            hascoapi:
                aliases:
                    - fuseki
        volumes:
            - hascoapi-fuseki-data:/fuseki/databases
        restart: always

    fuseki-yasgui:
        image: paulopinheiro1234/hascoapi-fuseki-yasgui:latest
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
            dockerfile: Dockerfile
        stdin_open: true    
        tty: true
        ports:
            - "9001:9001"
        environment:
            JAVA_OPTS: "-Xms128m -Xmx12g"
            FUSEKI_URL: "http://fuseki:3030"
        volumes:
            - /var/log/hascoapi:/hascoapi/logs
        depends_on:
            - fuseki
        networks:
            hascoapi:
                aliases:
                    - hascoapi
        restart: always

volumes:
    hascoapi-fuseki-data:

networks:
    hascoapi:
EOF

# Build and deploy
echo ""
echo "Building HASCOAPI (this will take a while)..."
sudo docker-compose build hascoapi

echo ""
echo "Starting services..."
sudo docker-compose up -d

echo ""
echo "=== Deployment Complete ==="
echo "Check status with: sudo docker-compose ps"
echo "View logs with: sudo docker-compose logs -f"
EOF