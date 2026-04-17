# Script para verificar tipos RDF dos StudyObjects no triplestore
$baseUrl = "http://localhost:3030"

Write-Host "`n========================================"
Write-Host "VERIFICACAO DE TIPOS RDF NO TRIPLESTORE"
Write-Host "========================================`n"

$sparqlQuery = @"
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX hasco: <http://hadatac.org/ont/hasco#>
PREFIX vstoi: <http://hadatac.org/ont/vstoi#>

SELECT ?type (COUNT(?so) as ?count) WHERE {
  ?so a ?type .
  ?so hasco:hascoType hasco:StudyObject .
}
GROUP BY ?type
ORDER BY DESC(?count)
"@

Write-Host "Query SPARQL:" -ForegroundColor Yellow
Write-Host $sparqlQuery -ForegroundColor Gray

Write-Host "`nExecutando query..." -ForegroundColor Yellow

try {
    $encodedQuery = [System.Web.HttpUtility]::UrlEncode($sparqlQuery)
    $url = "$baseUrl/HASCO/sparql?query=$encodedQuery"

    $response = Invoke-RestMethod -Uri $url -Method GET -Headers @{
        "Accept" = "application/sparql-results+json"
    }

    Write-Host "`nResultados:" -ForegroundColor Green
    Write-Host "============`n" -ForegroundColor Green

    foreach ($binding in $response.results.bindings) {
        $type = $binding.type.value
        $count = $binding.count.value
        Write-Host "  $count x $type"
    }

} catch {
    Write-Host "`nERRO ao executar query: $_" -ForegroundColor Red
    Write-Host "Acesse manualmente: http://localhost:3030/HASCO/sparql" -ForegroundColor Yellow
}

Write-Host "`n========================================`n"

