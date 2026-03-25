#!/bin/bash

echo "========================================="
echo "DP2 Generation Manual Test"
echo "========================================="
echo ""

# Configuration
BASE_URL="http://localhost:9000"
DATAFILE_URI="https://hadatac.org/ont/hadatac#/DFL1770637034846641"
STATUS="DRAFT"
FILENAME="Asdasdasd.xlsx"

# URL encode the datafile URI
ENCODED_URI=$(echo -n "$DATAFILE_URI" | jq -sRr @uri)

# Generation endpoint
GEN_URL="${BASE_URL}/api/mt/gen/perstatus/dp2/${ENCODED_URI}/${STATUS}/${FILENAME}/null/null"

echo "Testing DP2 Generation..."
echo "DataFile URI: $DATAFILE_URI"
echo "Status: $STATUS"
echo "Filename: $FILENAME"
echo ""
echo "Generation URL:"
echo "$GEN_URL"
echo ""
echo "Calling generation endpoint..."
echo ""

# Call generation
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$GEN_URL")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

echo "HTTP Status: $HTTP_CODE"
echo "Response:"
echo "$BODY"
echo ""

if [ "$HTTP_CODE" = "200" ]; then
    echo "✓ Generation endpoint responded successfully!"
    echo ""
    echo "Now check the server console for generation logs."
    echo "You should see:"
    echo "  ========== IngestionAPI.mtGenByStatus() START =========="
    echo "  ========== DP2Gen.genByStatus() START =========="
    echo ""
    echo "To download the file:"
    echo "  curl -X POST \"${BASE_URL}/api/mt/get/generated/${FILENAME}\" -o ${FILENAME}"
else
    echo "✗ Generation failed!"
    echo ""
    if [ "$HTTP_CODE" = "404" ]; then
        echo "404 means the endpoint was not found."
        echo "Did you restart the application after adding the new routes?"
    fi
fi

echo ""
echo "========================================="
