#!/bin/bash
# Load pharma ontology into Fuseki at container startup

FUSEKI_URL="${FUSEKI_URL:-http://fuseki:3030}"
PHARMA_OWL="/var/hascoapi/app_ontology/pharma.owl"
GRAPH_URI="https://pharma.graxiom.com/ont/"

echo "[Pharma Ontology] Waiting for Fuseki to be ready..."
until curl -s "${FUSEKI_URL}/$/ping" > /dev/null 2>&1; do
  sleep 2
done

echo "[Pharma Ontology] Fuseki is ready"

if [ -f "$PHARMA_OWL" ]; then
  echo "[Pharma Ontology] Loading pharma.owl into graph ${GRAPH_URI}..."
  
  curl -X POST "${FUSEKI_URL}/store/data?graph=${GRAPH_URI}" \
    --data-binary @"${PHARMA_OWL}" \
    -H "Content-Type: application/rdf+xml" \
    -s -o /dev/null
  
  echo "[Pharma Ontology] ✅ Loaded pharma.owl successfully"
else
  echo "[Pharma Ontology] ⚠️  pharma.owl not found at ${PHARMA_OWL}"
fi
