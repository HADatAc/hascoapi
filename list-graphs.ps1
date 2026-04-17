# Script para listar todos os named graphs e contar triples
$query = "SELECT ?g (COUNT(*) as ?count) WHERE { GRAPH ?g { ?s ?p ?o } } GROUP BY ?g ORDER BY DESC(?count)"
$body = "query=" + [System.Uri]::EscapeDataString($query)
$response = Invoke-RestMethod -Uri "http://localhost:3030/store/query" -Method Post -Body $body -ContentType "application/x-www-form-urlencoded"

Write-Host "`n" -NoNewline
Write-Host ("=" * 80) -ForegroundColor Cyan
Write-Host "NAMED GRAPHS NO FUSEKI" -ForegroundColor Cyan
Write-Host ("=" * 80) -ForegroundColor Cyan
Write-Host ""

if ($response.results.bindings.Count -gt 0) {
    foreach ($item in $response.results.bindings) {
        $graphUri = $item.g.value
        $count = $item.count.value
        Write-Host "Graph: $graphUri" -ForegroundColor Yellow
        Write-Host "  Triples: $count" -ForegroundColor White
        Write-Host ""
    }
} else {
    Write-Host "Nenhum named graph encontrado!" -ForegroundColor Red
}

Write-Host ("=" * 80) -ForegroundColor Cyan
Write-Host ""

