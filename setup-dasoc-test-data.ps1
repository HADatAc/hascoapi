# Script para criar objetos de teste para DA-SOC
# Este script cria 50 objetos no SOC-LOCATION para testar a ingestão do DA-SOC

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "SETUP: Criando objetos de teste para DA-SOC" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

$fusekiEndpoint = "http://localhost:3030/store/update"
$socUri = "http://hadatac.org/kb/default/SOC-LOCATION"

# Lista de objetos a criar (baseado no arquivo DA-SOC-LOCATION.csv)
$objects = @(
    @{id="LIBRARY-L0"; label="Library Level 0"},
    @{id="LIBRARY-L1"; label="Library Level 1"},
    @{id="LIBRARY-L2"; label="Library Level 2"},
    @{id="LIBRARY-L3"; label="Library Level 3"},
    @{id="LIBRARY-ROOF"; label="Library Roof"},
    @{id="SCIENCE-L0"; label="Science Building Level 0"},
    @{id="SCIENCE-L1"; label="Science Building Level 1"},
    @{id="SCIENCE-L2"; label="Science Building Level 2"},
    @{id="SCIENCE-L3"; label="Science Building Level 3"},
    @{id="SCIENCE-ROOF"; label="Science Building Roof"},
    @{id="CAFETERIA-L0"; label="Cafeteria Level 0"},
    @{id="CAFETERIA-L1"; label="Cafeteria Level 1"},
    @{id="ADMIN-L0"; label="Admin Building Level 0"},
    @{id="ADMIN-L1"; label="Admin Building Level 1"},
    @{id="ADMIN-BASEMENT"; label="Admin Building Basement"},
    @{id="GYM-L0"; label="Gymnasium Level 0"},
    @{id="GYM-L1"; label="Gymnasium Level 1"},
    @{id="PARKING-L0"; label="Parking Level 0"},
    @{id="PARKING-B1"; label="Parking Basement 1"},
    @{id="PARKING-B2"; label="Parking Basement 2"}
)

Write-Host "Criando $($objects.Count) objetos no SOC-LOCATION...`n" -ForegroundColor Yellow

$successCount = 0
$errorCount = 0

foreach ($obj in $objects) {
    $objUri = "http://hadatac.org/kb/default/$($obj.id)"

    $query = @"
PREFIX hasco: <http://hadatac.org/ont/hasco#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

INSERT DATA {
    <$objUri> a hasco:StudyObject ;
        hasco:isMemberOf <$socUri> ;
        hasco:originalID "$($obj.id)" ;
        rdfs:label "$($obj.label)" ;
        rdfs:comment "Test object for DA-SOC ingestion - created by setup script" .
}
"@

    try {
        $body = @{ update = $query }
        Invoke-RestMethod -Uri $fusekiEndpoint -Method POST -Body $body -ContentType "application/x-www-form-urlencoded" | Out-Null
        Write-Host "  ✓ Criado: $($obj.id)" -ForegroundColor Green
        $successCount++
    }
    catch {
        Write-Host "  ✗ Erro ao criar $($obj.id): $_" -ForegroundColor Red
        $errorCount++
    }
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "RESULTADOS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Objetos criados com sucesso: $successCount" -ForegroundColor Green
Write-Host "  Erros: $errorCount" -ForegroundColor $(if ($errorCount -gt 0) { "Red" } else { "Gray" })

# Verificar objetos criados
Write-Host "`nVerificando objetos no triplestore..." -ForegroundColor Yellow

$verifyQuery = @"
PREFIX hasco: <http://hadatac.org/ont/hasco#>
SELECT (COUNT(?obj) as ?count) WHERE {
    ?obj hasco:isMemberOf <$socUri>
}
"@

try {
    $result = (Invoke-RestMethod -Uri "http://localhost:3030/store/query" -Method POST -Body @{ query = $verifyQuery } -ContentType "application/x-www-form-urlencoded" -Headers @{ "Accept" = "application/sparql-results+json" }).results.bindings
    $count = $result[0].count.value
    Write-Host "  ✓ Total de objetos no SOC-LOCATION: $count" -ForegroundColor Green
}
catch {
    Write-Host "  ✗ Erro ao verificar objetos: $_" -ForegroundColor Red
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "PRÓXIMO PASSO: Executar ingestão do DA-SOC" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Use o teste: sbt `"testOnly org.hascoapi.tests.CompleteDASOCIngestionTest`"" -ForegroundColor White
Write-Host ""

