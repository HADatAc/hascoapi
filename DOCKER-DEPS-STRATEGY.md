# HASCOAPI Docker Dependency Cache Strategy

## Problem
Maven Central rate limiting (~200 req/hour) blocks Docker builds that download 200+ dependencies.

## Solution
Create a dependency cache base image once, push to Docker Hub, reuse forever.

## Workflow

### Step 1: Build and push dependency cache (ONE TIME - wait for rate limit to clear)
```bash
# Wait 12-24 hours for Maven Central rate limit to reset, then:
docker login -u paulopinheiro1234
./build-deps-on-aws.sh
```

This creates and pushes: `paulopinheiro1234/hascoapi-deps:latest` (~1.5GB)

### Step 2: Use the dependency cache for future builds
```bash
# Build using cached dependencies (fast, no Maven downloads!)
docker build -f Dockerfile.with-deps -t paulopinheiro1234/hascoapi:latest .

# Or update docker-compose.yml to use Dockerfile.with-deps
```

## Alternative: Use existing hadatac image
If you don't need custom code changes, just use:
```yaml
services:
  hascoapi:
    image: hadatac/hascoapi:latest
    ports:
      - "9001:9000"
```

## Files Created
- `Dockerfile.deps` - Builds dependency cache base image
- `Dockerfile.with-deps` - Uses cache for fast application builds  
- `build-deps-on-aws.sh` - Script to build and push deps image
- `hadatac-libs/` - 300 extracted JARs from hadatac/hascoapi (reference)

## Timeline
- **Now**: Rate limited, need to wait
- **12-24 hours**: Rate limit clears
- **Then**: Run `build-deps-on-aws.sh` once
- **Future**: All builds use cached deps (30 seconds vs 10+ minutes)
