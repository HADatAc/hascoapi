# HASCOAPI AWS Deployment - 3 Step Process

## Prerequisites
- AWS VM with new IP: 63.35.15.7 (fresh Maven rate limit)
- Docker Hub account: paulopinheiro1234
- SSH key: ~/.ssh/graxiom_core.pem

---

## STEP 1: Build Dependency Cache Image (One-Time, ~10-15 minutes)

**Goal:** Download all Maven dependencies once and cache them in a Docker image.

### On AWS VM (63.35.15.7):

```bash
# 1. SSH into AWS VM
ssh -i ~/.ssh/graxiom_core.pem ubuntu@63.35.15.7

# 2. Navigate to hascoapi directory
cd ~/hascoapi

# 3. Build the dependency cache image
# This will download ~200 JAR files from Maven Central
sudo docker build -f Dockerfile.deps -t paulopinheiro1234/hascoapi-deps:latest .

# Expected time: 10-15 minutes
# Expected size: ~1.5-2GB
# Watch for: "sbt update compile test:compile" completing successfully
```

**Success indicators:**
- No HTTP 429 errors
- Build completes with "Successfully built..."
- Image appears in `sudo docker images`

---

## STEP 2: Push Dependency Image to Docker Hub (Manual)

**Goal:** Make the dependency cache available for future builds.

### On AWS VM (continue from Step 1):

```bash
# 1. Login to Docker Hub
sudo docker login -u paulopinheiro1234
# Enter password: Abacaxi1357!

# 2. Push dependency image to Docker Hub
sudo docker push paulopinheiro1234/hascoapi-deps:latest

# Expected time: 5-10 minutes (uploading ~1.5-2GB)

# 3. Logout (security)
sudo docker logout

# 4. Verify image on Docker Hub
# Visit: https://hub.docker.com/r/paulopinheiro1234/hascoapi-deps/tags
```

**Success indicators:**
- Push completes without errors
- Image visible on Docker Hub web interface
- Tag shows "latest"

---

## STEP 3: Build HASCOAPI Using Dependency Cache & Deploy

**Goal:** Build your custom HASCOAPI quickly using cached dependencies, deploy to AWS.

### 3A. Build HASCOAPI Image

On AWS VM (continue):

```bash
# 1. Build HASCOAPI using the dependency cache
# This build will be FAST (30-60 seconds) because dependencies are cached
sudo docker build -f Dockerfile.with-deps -t paulopinheiro1234/hascoapi:latest .

# Expected time: 30-60 seconds
# No Maven downloads needed!
```

### 3B. Update docker-compose.yml

```bash
# Edit docker-compose.yml to use your built image instead of building
nano docker-compose.yml

# Change the hascoapi service from:
#   build: .
# To:
#   image: paulopinheiro1234/hascoapi:latest

# Save and exit (Ctrl+X, Y, Enter)
```

### 3C. Deploy the Application

```bash
# 1. Start all services
sudo docker-compose up -d

# 2. Check status
sudo docker-compose ps

# 3. View logs
sudo docker-compose logs -f hascoapi

# 4. Test the application
curl http://localhost:9001
# Or visit http://63.35.15.7:9001 in browser
```

**Success indicators:**
- All 3 containers running (hascoapi, fuseki, yasgui)
- HASCOAPI responds to HTTP requests
- No errors in logs

---

## Troubleshooting

### If Step 1 fails with HTTP 429 errors:
- Wait 30 minutes and retry
- New IP should have fresh rate limit quota

### If Step 1 runs out of memory:
- Check available memory: `free -h`
- May need larger AWS instance type

### If Step 3B deployment fails:
- Check logs: `sudo docker-compose logs hascoapi`
- Verify image exists: `sudo docker images | grep hascoapi`
- Ensure ports 9001, 3030, 8888 are open in AWS security group

---

## Future Updates

After this initial setup, updating HASCOAPI is fast:

```bash
# On AWS VM:
cd ~/hascoapi
git pull
sudo docker build -f Dockerfile.with-deps -t paulopinheiro1234/hascoapi:latest .
sudo docker-compose restart hascoapi
```

Build time: 30-60 seconds (dependencies already cached!)

---

## Files Overview

- `Dockerfile.deps` - Builds dependency cache (Step 1)
- `Dockerfile.with-deps` - Builds HASCOAPI using cached deps (Step 3)
- `docker-compose.yml` - Orchestrates all services (Step 3)

---

## Cleanup (Optional)

To save disk space after successful deployment:

```bash
# Remove intermediate build images
sudo docker image prune -a

# Keep only:
# - paulopinheiro1234/hascoapi-deps:latest
# - paulopinheiro1234/hascoapi:latest
# - hansidm/fuseki
# - hansidm/yasgui
```
