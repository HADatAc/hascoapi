#!/bin/bash
# Deploy HASCOAPI images to AWS ECR Public

set -e

# Configuration - Public ECR must use us-east-1
AWS_REGION="eu-west-1"
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
ECR_REGISTRY="public.ecr.aws"

# Repository names
REPOS=("hascoapi" "hascoapi-fuseki" "hascoapi-fuseki-yasgui")

echo "=== AWS ECR Public Deployment Script ==="
echo "Region: $AWS_REGION (Public ECR only available in us-east-1)"
echo "Account: $AWS_ACCOUNT_ID"
echo "Registry: $ECR_REGISTRY"
echo ""

# Authenticate Docker to ECR Public
echo "Authenticating to ECR Public..."
aws ecr-public get-login-password --region us-east-1 | docker login --username AWS --password-stdin public.ecr.aws

# Get the registry alias (first time setup may be needed)
echo "Getting ECR Public registry alias..."
REGISTRY_ALIAS=$(aws ecr-public describe-registries --region us-east-1 --query 'registries[0].registryAlias' --output text 2>/dev/null || echo "")

if [ -z "$REGISTRY_ALIAS" ]; then
    echo "ERROR: No ECR Public registry found."
    echo "Please create a public registry first at: https://console.aws.amazon.com/ecr/repositories?region=us-east-1"
    exit 1
fi

echo "Using registry alias: $REGISTRY_ALIAS"
FULL_REGISTRY="$ECR_REGISTRY/$REGISTRY_ALIAS"

# Create ECR public repositories if they don't exist
for repo in "${REPOS[@]}"; do
    echo "Checking repository: $repo"
    aws ecr-public describe-repositories --repository-names $repo --region us-east-1 2>/dev/null || \
        aws ecr-public create-repository \
            --repository-name $repo \
            --region us-east-1 \
            --catalog-data "description=HASCOAPI - Human-Aware Science Ontology API,aboutText=Public repository for $repo,architectures=ARM,ARM 64,x86,x86-64,operatingSystems=Linux"
done

# Tag and push images
echo ""
echo "Tagging and pushing images..."

# Tag local images with ECR public registry
docker tag hascoapi-hascoapi:latest $FULL_REGISTRY/hascoapi:latest
docker tag hascoapi-fuseki:latest $FULL_REGISTRY/hascoapi-fuseki:latest
docker tag hascoapi-fuseki-yasgui:latest $FULL_REGISTRY/hascoapi-fuseki-yasgui:latest

# Push to ECR Public
docker push $FULL_REGISTRY/hascoapi:latest
docker push $FULL_REGISTRY/hascoapi-fuseki:latest
docker push $FULL_REGISTRY/hascoapi-fuseki-yasgui:latest

echo ""
echo "=== Push Complete ==="
echo "Images are now PUBLIC and available at:"
echo "  - $FULL_REGISTRY/hascoapi:latest"
echo "  - $FULL_REGISTRY/hascoapi-fuseki:latest"
echo "  - $FULL_REGISTRY/hascoapi-fuseki-yasgui:latest"
echo ""
echo "Anyone can pull with:"
echo "  docker pull $FULL_REGISTRY/hascoapi:latest"
