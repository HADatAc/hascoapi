# Query para buscar objetos SOC-LOCATION em QUALQUER graph (inclusive default graph)
$query = @"
PREFIX hasco: <http://hadatac.org/ont/hasco#>
SELECT DISTINCT ?g (COUNT(?obj) as ?count)
WHERE {
  {
    GRAPH ?g {
      ?obj hasco:isMemberOf ?soc .
      FILTER(CONTAINS(STR(?soc), "SOC-LOCATION"))
    }
  }
  UNION
  {
    ?obj hasco:isMemberOf ?soc .
    FILTER(CONTAINS(STR(?soc), "SOC-LOCATION"))
    BIND(<urn:x-arq:DefaultGraph> as ?g)
  }
}
GROUP BY ?g
"@

$body = "query=" + [System.Uri]::EscapeDataString($query)
$response = Invoke-RestMethod -Uri "http://localhost:3030/store/query" -Method Post -Body $body -ContentType "application/x-www-form-urlencoded"

Write-Host "`n" -NoNewline
Write-Host ("=" * 80) -ForegroundColor Cyan
Write-Host "BUSCAR OBJETOS SOC-LOCATION EM TODOS OS GRAPHS" -ForegroundColor Cyan
Write-Host ("=" * 80) -ForegroundColor Cyan
Write-Host ""

if ($response.results.bindings.Count -gt 0) {
    foreach ($item in $response.results.bindings) {
        $graph = $item.g.value
        $count = $item.count.value
        Write-Host "Graph: $graph" -ForegroundColor Yellow
        Write-Host "  Objetos SOC-LOCATION: $count" -ForegroundColor White
        Write-Host ""
    }
} else {
    Write-Host "Nenhum objeto SOC-LOCATION encontrado em nenhum graph!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Verificando se ha ALGUM triple no Fuseki..." -ForegroundColor Yellow

    $queryAny = "SELECT (COUNT(*) as ?count) WHERE { ?s ?p ?o }"
    $bodyAny = "query=" + [System.Uri]::EscapeDataString($queryAny)
    $responseAny = Invoke-RestMethod -Uri "http://localhost:3030/store/query" -Method Post -Body $bodyAny -ContentType "application/x-www-form-urlencoded"

    if ($responseAny.results.bindings.Count -gt 0) {
        $totalTriples = $responseAny.results.bindings[0].count.value
        Write-Host "Total de triples no Fuseki: $totalTriples" -ForegroundColor $(if ($totalTriples -eq "0") { "Red" } else { "Green" })
    }
}

Write-Host ("=" * 80) -ForegroundColor Cyan
Write-Host ""

