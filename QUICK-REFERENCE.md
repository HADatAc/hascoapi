# Quick Command Reference - HASCOAPI Deployment

## Copy files to AWS
```bash
chmod +x copy-to-aws.sh
./copy-to-aws.sh
```

## Step 1: Build Dependency Cache (on AWS VM)
```bash
ssh -i ~/.ssh/graxiom_core.pem ubuntu@63.35.15.7
cd ~/hascoapi
sudo docker build -f Dockerfile.deps -t paulopinheiro1234/hascoapi-deps:latest .
```

## Step 2: Push to Docker Hub (on AWS VM)
```bash
sudo docker login -u paulopinheiro1234
# Password: Abacaxi1357!
sudo docker push paulopinheiro1234/hascoapi-deps:latest
sudo docker logout
```

## Step 3: Build & Deploy HASCOAPI (on AWS VM)
```bash
sudo docker build -f Dockerfile.with-deps -t paulopinheiro1234/hascoapi:latest .
sudo docker-compose up -d
sudo docker-compose ps
```

## Test
```bash
curl http://localhost:9001
# Or visit http://63.35.15.7:9001
```

## Monitor
```bash
sudo docker-compose logs -f hascoapi
```

## Verify Dependency Image Size
```bash
sudo docker images | grep hascoapi-deps
# Expected: ~1.5-2GB
```
