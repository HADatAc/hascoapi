# HASCOAPI Docker Hub Deployment Guide

## Overview
Deploy HASCOAPI images to Docker Hub for public access. Your private VM can then pull these images without any credentials.

## Quick Start

### 1. Prerequisites
- Docker installed locally
- Docker Hub account (free): https://hub.docker.com/signup
- HASCOAPI images built locally

### 2. Push to Docker Hub

Run the deployment script:
```bash
./deploy-to-dockerhub.sh
```

You'll be prompted for:
- Docker Hub username
- Docker Hub password (hidden input)

The script will:
- Login securely (credentials not stored)
- Tag your images
- Push to Docker Hub
- Logout automatically

### 3. Deploy on Private VM

On your private VM, create a `docker-compose.yml`:

```yaml
version: '3'

services:
    fuseki:
        image: YOUR_USERNAME/hascoapi-fuseki:latest
        container_name: hascoapi_fuseki
        ports:
            - "3030:3030"
        volumes:
            - hascoapi-fuseki-data:/fuseki/databases
        restart: always

    fuseki-yasgui:
        image: YOUR_USERNAME/hascoapi-fuseki-yasgui:latest
        container_name: hascoapi_fuseki_yasgui
        ports:
            - "8888:8888"
        environment:
            DEFAULT_SPARQL_ENDPOINT: "http://localhost:3030/store/sparql"
        depends_on:
            - fuseki
        restart: always
    
    hascoapi:
        image: YOUR_USERNAME/hascoapi:latest
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
        restart: always

volumes:
    hascoapi-fuseki-data:
```

Replace `YOUR_USERNAME` with your Docker Hub username, then:

```bash
# Pull images (NO credentials needed - public images)
docker-compose pull

# Start services
docker-compose up -d

# Verify
docker-compose ps
docker logs hascoapi-hascoapi-1
```

## Updating Images

### On your Mac:
```bash
# Rebuild if needed
docker-compose build

# Push new version
./deploy-to-dockerhub.sh
```

### On your VM:
```bash
# Pull latest
docker-compose pull

# Restart with new images
docker-compose up -d
```

## Security

- ✅ Credentials prompted interactively (never stored)
- ✅ Password input hidden
- ✅ Auto-logout after push
- ✅ VM requires NO credentials (public images)
- ✅ Your data stays private on your VM

## Cost

**FREE** - Docker Hub allows unlimited public repositories

## Troubleshooting

### "repository does not exist"
Make sure repositories are public in Docker Hub:
- Go to https://hub.docker.com/repositories
- Click on each repository → Settings
- Set visibility to "Public"

### Can't pull on VM
Check image name matches your username:
```bash
docker pull YOUR_USERNAME/hascoapi:latest
```

### Build fails
Check existing images first:
```bash
docker images | grep hascoapi
```
