# ============================================================================
# SCRIPT: Testar Query dos 50 Objetos SOC-LOCATION
# ============================================================================

$FUSEKI_ENDPOINT = "http://localhost:3030/store/query"
$GRAPH_URI = "https://hadatac.org/ont/hadatac#/DFL1775048338616071"

Write-Host ""
Write-Host ("=" * 80) -ForegroundColor Cyan
Write-Host "TESTE: QUERY 50 OBJETOS SOC-LOCATION" -ForegroundColor Cyan
Write-Host ("=" * 80) -ForegroundColor Cyan
Write-Host ""

# ----------------------------------------------------------------------------
# Passo 1: Verificar se o grafo existe
# ----------------------------------------------------------------------------
Write-Host "Passo 1: Verificando se o grafo existe..." -ForegroundColor Yellow
$queryGraphExists = "SELECT (COUNT(*) as ?count) WHERE { GRAPH <$GRAPH_URI> { ?s ?p ?o } }"
$body = "query=" + [System.Uri]::EscapeDataString($queryGraphExists)

try {
    $response = Invoke-RestMethod -Uri $FUSEKI_ENDPOINT -Method Post -Body $body -ContentType "application/x-www-form-urlencoded"
    $count = $response.results.bindings[0].count.value
    Write-Host "  Triples no grafo: $count" -ForegroundColor $(if ($count -eq "0") { "Red" } else { "Green" })
} catch {
    Write-Host "  Erro: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# ----------------------------------------------------------------------------
# Passo 2: Listar todos os grafos disponiveis
# ----------------------------------------------------------------------------
Write-Host "Passo 2: Listando todos os grafos disponiveis..." -ForegroundColor Yellow
$queryListGraphs = "SELECT DISTINCT ?g WHERE { GRAPH ?g { ?s ?p ?o } } LIMIT 20"
$body = "query=" + [System.Uri]::EscapeDataString($queryListGraphs)

try {
    $response = Invoke-RestMethod -Uri $FUSEKI_ENDPOINT -Method Post -Body $body -ContentType "application/x-www-form-urlencoded"
    if ($response.results.bindings.Count -gt 0) {
        Write-Host "  Grafos encontrados:" -ForegroundColor White
        foreach ($item in $response.results.bindings) {
            $graphUri = $item.g.value
            Write-Host "    - $graphUri" -ForegroundColor Gray
        }
    } else {
        Write-Host "  Nenhum grafo encontrado!" -ForegroundColor Red
    }
} catch {
    Write-Host "  Erro: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# ----------------------------------------------------------------------------
# Passo 3: Buscar objetos com tipo pato:0000140 em QUALQUER grafo
# ----------------------------------------------------------------------------
Write-Host "Passo 3: Buscando objetos pato:0000140 em QUALQUER grafo..." -ForegroundColor Yellow
$queryAnyGraph = @"
PREFIX pato: <http://purl.obolibrary.org/obo/PATO_>
PREFIX hasco: <http://hadatac.org/ont/hasco#>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

SELECT ?g ?obj ?originalID
WHERE {
  GRAPH ?g {
    ?obj rdf:type pato:0000140 .
    ?obj hasco:originalID ?originalID .
  }
}
LIMIT 10
"@

$body = "query=" + [System.Uri]::EscapeDataString($queryAnyGraph)

try {
    $response = Invoke-RestMethod -Uri $FUSEKI_ENDPOINT -Method Post -Body $body -ContentType "application/x-www-form-urlencoded"
    $count = $response.results.bindings.Count
    Write-Host "  Objetos encontrados: $count" -ForegroundColor $(if ($count -eq "0") { "Red" } else { "Green" })

    if ($count -gt 0) {
        Write-Host "  Primeiros objetos:" -ForegroundColor White
        foreach ($item in $response.results.bindings) {
            $graph = $item.g.value
            $obj = $item.obj.value
            $id = $item.originalID.value
            Write-Host "    - Graph: $graph" -ForegroundColor Cyan
            Write-Host "      ID: $id" -ForegroundColor White
            Write-Host "      URI: $obj" -ForegroundColor Gray
            Write-Host ""
        }
    }
} catch {
    Write-Host "  Erro: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# ----------------------------------------------------------------------------
# Passo 4: Query original - buscar no grafo especifico
# ----------------------------------------------------------------------------
Write-Host "Passo 4: Executando query original no grafo especifico..." -ForegroundColor Yellow
$queryOriginal = @"
PREFIX hasco: <http://hadatac.org/ont/hasco#>
PREFIX pato: <http://purl.obolibrary.org/obo/PATO_>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?obj ?originalID ?label ?spaceScopeUri
WHERE {
  GRAPH <$GRAPH_URI> {
    ?obj rdf:type pato:0000140 .
    ?obj hasco:originalID ?originalID .
    OPTIONAL { ?obj rdfs:label ?label }
    OPTIONAL { ?obj hasco:hasSpaceScope ?spaceScopeUri }
  }
}
ORDER BY ?originalID
LIMIT 50
"@

$body = "query=" + [System.Uri]::EscapeDataString($queryOriginal)

try {
    $response = Invoke-RestMethod -Uri $FUSEKI_ENDPOINT -Method Post -Body $body -ContentType "application/x-www-form-urlencoded"
    $count = $response.results.bindings.Count
    Write-Host "  Objetos encontrados: $count" -ForegroundColor $(if ($count -eq "0") { "Red" } else { "Green" })

    if ($count -gt 0) {
        Write-Host ""
        Write-Host "  Primeiros 10 objetos:" -ForegroundColor White
        $first10 = $response.results.bindings | Select-Object -First 10
        foreach ($item in $first10) {
            $id = $item.originalID.value
            $label = if ($item.label) { $item.label.value } else { "N/A" }
            $scope = if ($item.spaceScopeUri) { $item.spaceScopeUri.value } else { "N/A" }

            Write-Host "    - $id" -ForegroundColor White
            Write-Host "      Label: $label" -ForegroundColor Gray
            Write-Host "      Space Scope: $scope" -ForegroundColor Gray
            Write-Host ""
        }
    }
} catch {
    Write-Host "  Erro: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

Write-Host ("=" * 80) -ForegroundColor Cyan
Write-Host ""

