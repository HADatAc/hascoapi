# Check what was actually saved

$fusekiUrl = "http://localhost:3030/store/sparql"

Write-Host "`n=== CHECK WHAT'S IN THE DATAFILE GRAPH ===" -ForegroundColor Cyan

# Get the latest DataFile graph
$queryGraph = @"
PREFIX hadatac: <https://hadatac.org/ont/hadatac#>

SELECT ?graph WHERE {
  GRAPH ?graph {
    ?df a hadatac:DataFile .
    FILTER(CONTAINS(STR(?graph), "DFL"))
  }
}
ORDER BY DESC(?graph)
LIMIT 1
"@

try {
    $response = Invoke-RestMethod -Uri $fusekiUrl -Method Post -Body @{query=$queryGraph} -ContentType "application/x-www-form-urlencoded"
    if ($response.results.bindings.Count -gt 0) {
        $graphUri = $response.results.bindings[0].graph.value
        Write-Host "Latest DataFile graph: $graphUri" -ForegroundColor Green

        # Now check what's IN that graph
        $queryContents = @"
PREFIX hasco: <http://hadatac.org/ont/hasco#>
PREFIX vstoi: <http://hadatac.org/ont/vstoi#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?s ?type (COUNT(*) AS ?tripleCount)
WHERE {
  GRAPH <$graphUri> {
    ?s a ?type .
  }
}
GROUP BY ?s ?type
ORDER BY DESC(?tripleCount)
LIMIT 20
"@

        $response2 = Invoke-RestMethod -Uri $fusekiUrl -Method Post -Body @{query=$queryContents} -ContentType "application/x-www-form-urlencoded"
        Write-Host "`nObjects in graph:" -ForegroundColor Cyan
        foreach ($binding in $response2.results.bindings) {
            $uri = $binding.s.value
            $type = $binding.type.value
            $count = $binding.tripleCount.value
            Write-Host "  $uri" -ForegroundColor White
            Write-Host "    Type: $type" -ForegroundColor Yellow
            Write-Host "    Triples: $count" -ForegroundColor Gray
        }
    } else {
        Write-Host "No DataFile graphs found!" -ForegroundColor Red
    }
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
}

Write-Host "`n=== DONE ===" -ForegroundColor Cyan

