#!/bin/bash
# Script to verify that ontology ingestion doesn't create duplicate triples

echo "=== Checking Triple Counts for PMSR Ontologies ==="
echo ""

# PMSR ontology
echo "1. PMSR Ontology (http://pmsr.net/ont/pmsr):"
curl -s -X POST http://localhost:3030/store/query \
  --data-urlencode "query=SELECT (COUNT(*) as ?count) WHERE { GRAPH <http://pmsr.net/ont/pmsr> { ?s ?p ?o } }" \
  -H "Accept: application/sparql-results+json" | python3 -c "import sys, json; print(f\"   Triples: {json.load(sys.stdin)['results']['bindings'][0]['count']['value']}\")"

echo ""

# UBERON ontology
echo "2. UBERON Ontology (http://purl.obolibrary.org/obo/uberon.owl):"
curl -s -X POST http://localhost:3030/store/query \
  --data-urlencode "query=SELECT (COUNT(*) as ?count) WHERE { GRAPH <http://purl.obolibrary.org/obo/uberon.owl> { ?s ?p ?o } }" \
  -H "Accept: application/sparql-results+json" | python3 -c "import sys, json; print(f\"   Triples: {json.load(sys.stdin)['results']['bindings'][0]['count']['value']}\")"

echo ""

# NCIT ontology
echo "3. NCIT Ontology (http://purl.obolibrary.org/obo/ncit.owl):"
curl -s -X POST http://localhost:3030/store/query \
  --data-urlencode "query=SELECT (COUNT(*) as ?count) WHERE { GRAPH <http://purl.obolibrary.org/obo/ncit.owl> { ?s ?p ?o } }" \
  -H "Accept: application/sparql-results+json" | python3 -c "import sys, json; print(f\"   Triples: {json.load(sys.stdin)['results']['bindings'][0]['count']['value']}\")"

echo ""
echo "=== Expected Triple Counts ==="
echo "PMSR:   ~2,500 triples (555 classes)"
echo "UBERON: ~250,000 triples (5,556 classes)"  
echo "NCIT:   ~65,000 triples (1,460 classes)"
echo ""
echo "If counts are significantly higher than expected, duplicates may exist."
echo "Run this script BEFORE and AFTER re-ingestion to verify counts stay the same."
