# Script para verificar se entidades VSTOI foram ingeridas no triplestore
$fusekiUrl = "http://localhost:3030/HASCO/sparql"

Write-Host "`n========================================"
Write-Host "VERIFICACAO DE INGESTAO VSTOI"
Write-Host "========================================`n"

# Query para contar entidades por tipo
$sparqlQuery = @"
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX hasco: <http://hadatac.org/ont/hasco#>
PREFIX vstoi: <http://hadatac.org/ont/vstoi#>

SELECT ?hascoType (COUNT(?entity) as ?total) WHERE {
  ?entity hasco:hascoType ?hascoType .
  FILTER(?hascoType IN (
    vstoi:Instrument,
    vstoi:Component,
    vstoi:ComponentStem,
    vstoi:ContainerSlot
  ))
}
GROUP BY ?hascoType
ORDER BY ?hascoType
"@

Write-Host "Consultando Fuseki..." -ForegroundColor Yellow

try {
    $body = @{
        query = $sparqlQuery
    }

    $response = Invoke-RestMethod -Uri $fusekiUrl `
        -Method POST `
        -Headers @{
            "Accept" = "application/sparql-results+json"
            "Content-Type" = "application/x-www-form-urlencoded"
        } `
        -Body $body

    Write-Host "`nResultados:" -ForegroundColor Green
    Write-Host "============`n" -ForegroundColor Green

    $totalEntities = 0
    $expectedCounts = @{
        "http://hadatac.org/ont/vstoi#Instrument" = 71
        "http://hadatac.org/ont/vstoi#Component" = 16
        "http://hadatac.org/ont/vstoi#ComponentStem" = 4
        "http://hadatac.org/ont/vstoi#ContainerSlot" = 83
    }

    if ($response.results.bindings.Count -eq 0) {
        Write-Host "  ERRO: Nenhuma entidade VSTOI encontrada!" -ForegroundColor Red
        Write-Host "  Possibilidades:" -ForegroundColor Yellow
        Write-Host "    1. DSG ainda nao foi ingerido" -ForegroundColor Gray
        Write-Host "    2. Deteccao de tipos nao funcionou" -ForegroundColor Gray
        Write-Host "    3. Problema na criacao das entidades" -ForegroundColor Gray
    } else {
        foreach ($binding in $response.results.bindings) {
            $type = $binding.hascoType.value
            $count = [int]$binding.total.value
            $totalEntities += $count

            $typeName = $type.Split('#')[1]
            $expected = $expectedCounts[$type]

            if ($expected -ne $null) {
                if ($count -eq $expected) {
                    Write-Host "  OK $typeName : $count (esperado: $expected)" -ForegroundColor Green
                } else {
                    Write-Host "  AVISO $typeName : $count (esperado: $expected)" -ForegroundColor Yellow
                }
            } else {
                Write-Host "  $typeName : $count" -ForegroundColor White
            }
        }

        Write-Host "`n  TOTAL: $totalEntities entidades VSTOI" -ForegroundColor Cyan

        if ($totalEntities -eq 174) {
            Write-Host "`n  SUCESSO! Todas as entidades foram ingeridas corretamente!" -ForegroundColor Green
        } elseif ($totalEntities -gt 0) {
            Write-Host "`n  PARCIAL: Algumas entidades foram ingeridas, mas faltam outras." -ForegroundColor Yellow
        }
    }

} catch {
    Write-Host "`nERRO ao consultar Fuseki: $_" -ForegroundColor Red
    Write-Host "`nVerifique se:" -ForegroundColor Yellow
    Write-Host "  1. Fuseki esta rodando (http://localhost:3030)" -ForegroundColor Gray
    Write-Host "  2. Dataset HASCO existe" -ForegroundColor Gray
    Write-Host "  3. URL esta correta: $fusekiUrl" -ForegroundColor Gray
}

Write-Host "`n========================================"
Write-Host "DETALHES ADICIONAIS"
Write-Host "========================================`n"

# Query para ver exemplos de cada tipo
$detailQuery = @"
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX hasco: <http://hadatac.org/ont/hasco#>
PREFIX vstoi: <http://hadatac.org/ont/vstoi#>

SELECT ?entity ?label ?hascoType WHERE {
  ?entity hasco:hascoType ?hascoType .
  OPTIONAL { ?entity rdfs:label ?label }
  FILTER(?hascoType IN (
    vstoi:Instrument,
    vstoi:Component,
    vstoi:ComponentStem,
    vstoi:ContainerSlot
  ))
}
ORDER BY ?hascoType ?label
LIMIT 10
"@

Write-Host "Primeiros 10 exemplos:" -ForegroundColor Yellow

try {
    $body = @{
        query = $detailQuery
    }

    $response = Invoke-RestMethod -Uri $fusekiUrl `
        -Method POST `
        -Headers @{
            "Accept" = "application/sparql-results+json"
            "Content-Type" = "application/x-www-form-urlencoded"
        } `
        -Body $body

    foreach ($binding in $response.results.bindings) {
        $label = if ($binding.label) { $binding.label.value } else { "(sem label)" }
        $type = $binding.hascoType.value.Split('#')[1]
        Write-Host "  [$type] $label" -ForegroundColor Gray
    }

} catch {
    Write-Host "  (Nao foi possivel obter exemplos)" -ForegroundColor Gray
}

Write-Host "`n========================================`n"

