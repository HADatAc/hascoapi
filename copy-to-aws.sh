#!/bin/bash
# Copy necessary files to AWS VM for deployment (us-east-1)

set -e

NEW_IP="34.230.25.112"
SSH_KEY="~/.ssh/graxiom_us.pem"

echo "=== Copying deployment files to AWS VM ($NEW_IP) ==="

# Ensure directory exists with proper permissions
ssh -i $SSH_KEY ubuntu@$NEW_IP "mkdir -p ~/hascoapi && chmod 755 ~/hascoapi"

# Copy Dockerfiles
scp -i $SSH_KEY Dockerfile.deps ubuntu@$NEW_IP:~/hascoapi/
scp -i $SSH_KEY Dockerfile.with-deps ubuntu@$NEW_IP:~/hascoapi/

# Copy deployment instructions
scp -i $SSH_KEY DEPLOYMENT-STEPS.md ubuntu@$NEW_IP:~/hascoapi/

echo ""
echo "✓ Files copied successfully"
echo ""
echo "Next steps:"
echo "1. ssh -i $SSH_KEY ubuntu@$NEW_IP"
echo "2. cd ~/hascoapi"
echo "3. Follow DEPLOYMENT-STEPS.md"
echo ""
