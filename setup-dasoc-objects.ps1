# Script para criar objetos de teste para DA-SOC
# Baseado na estrutura real do DSG-LTE-PIAGET-WEATHER-STATION

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "SETUP: Criando objetos do DSG no triplestore" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

$fusekiEndpoint = "http://localhost:3030/store/update"
$fusekiQuery = "http://localhost:3030/store/query"
$socUri = "http://hadatac.org/kb/default/SOC-LOCATION"

# Todos os objetos do DSG
$objectIds = @(
    "LIBRARY-L0", "LIBRARY-L1", "LIBRARY-L2", "LIBRARY-L3", "LIBRARY-ROOF",
    "SCIENCE-L0", "SCIENCE-L1", "SCIENCE-L2", "SCIENCE-L3", "SCIENCE-ROOF",
    "ARTS-L0", "ARTS-L1", "ARTS-L2", "ARTS-L3", "ARTS-ROOF",
    "SPORTS-L0", "SPORTS-L1", "SPORTS-L2", "SPORTS-L3", "SPORTS-ROOF",
    "ADMIN-L0", "ADMIN-L1", "ADMIN-L2", "ADMIN-L3", "ADMIN-ROOF",
    "CAFE-L0", "CAFE-L1", "CAFE-L2", "CAFE-L3", "CAFE-ROOF",
    "MAIN-L0", "MAIN-L1", "MAIN-L2", "MAIN-L3", "MAIN-ROOF",
    "ANNEX-A-L0", "ANNEX-A-L1", "ANNEX-A-L2", "ANNEX-A-L3", "ANNEX-A-ROOF",
    "ANNEX-B-L0", "ANNEX-B-L1", "ANNEX-B-L2", "ANNEX-B-L3", "ANNEX-B-ROOF",
    "GREENHOUSE-L0", "GREENHOUSE-L1", "GREENHOUSE-L2", "GREENHOUSE-L3", "GREENHOUSE-ROOF"
)

Write-Host "Criando $($objectIds.Count) objetos no SOC-LOCATION...`n" -ForegroundColor Yellow

$successCount = 0
$errorCount = 0

foreach ($objId in $objectIds) {
    $objUri = "http://hadatac.org/kb/default/$objId"

    # Criar query usando substituição de variáveis
    $sparqlUpdate = @"
PREFIX hasco: <http://hadatac.org/ont/hasco#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

INSERT DATA {
    <$objUri> a hasco:StudyObject ;
        hasco:isMemberOf <$socUri> ;
        hasco:originalID "$objId" ;
        rdfs:label "$objId" ;
        rdfs:comment "DSG object for LTE-PIAGET study" .
}
"@

    try {
        $body = @{ update = $sparqlUpdate }
        Invoke-RestMethod -Uri $fusekiEndpoint -Method POST -Body $body -ContentType "application/x-www-form-urlencoded" | Out-Null
        Write-Host "  ✓ $objId" -ForegroundColor Green
        $successCount++
    }
    catch {
        Write-Host "  ✗ Erro: $objId - $_" -ForegroundColor Red
        $errorCount++
    }
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "RESULTADOS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Criados: $successCount" -ForegroundColor Green
Write-Host "  Erros: $errorCount" -ForegroundColor $(if ($errorCount -gt 0) { "Red" } else { "Gray" })

# Verificar total
Write-Host "`nVerificando total no triplestore..." -ForegroundColor Yellow

$countQuery = @"
PREFIX hasco: <http://hadatac.org/ont/hasco#>
SELECT (COUNT(?obj) as ?count) WHERE {
    ?obj hasco:isMemberOf <$socUri>
}
"@

try {
    $result = (Invoke-RestMethod -Uri $fusekiQuery -Method POST -Body @{ query = $countQuery } -ContentType "application/x-www-form-urlencoded" -Headers @{ "Accept" = "application/sparql-results+json" }).results.bindings
    $count = $result[0].count.value
    Write-Host "  ✓ Total no SOC-LOCATION: $count" -ForegroundColor Green

    if ([int]$count -ge 50) {
        Write-Host "`n✅ SUCESSO! $count objetos prontos para DA-SOC!`n" -ForegroundColor Green
    }
}
catch {
    Write-Host "  ✗ Erro ao verificar: $_" -ForegroundColor Red
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Próximo passo: Testar DA-SOC" -ForegroundColor Yellow
Write-Host "========================================`n" -ForegroundColor Cyan

