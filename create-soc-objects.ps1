# Script para criar objetos de teste para DA-SOC usando arquivo temporário

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "SETUP: Criando objetos do DSG" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

$fusekiEndpoint = "http://localhost:3030/store/update"
$fusekiQuery = "http://localhost:3030/store/query"
$socUri = "http://hadatac.org/kb/default/SOC-LOCATION"

# Todos os 50 objetos do DSG
$objectIds = "LIBRARY-L0", "LIBRARY-L1", "LIBRARY-L2", "LIBRARY-L3", "LIBRARY-ROOF", "SCIENCE-L0", "SCIENCE-L1", "SCIENCE-L2", "SCIENCE-L3", "SCIENCE-ROOF", "ARTS-L0", "ARTS-L1", "ARTS-L2", "ARTS-L3", "ARTS-ROOF", "SPORTS-L0", "SPORTS-L1", "SPORTS-L2", "SPORTS-L3", "SPORTS-ROOF", "ADMIN-L0", "ADMIN-L1", "ADMIN-L2", "ADMIN-L3", "ADMIN-ROOF", "CAFE-L0", "CAFE-L1", "CAFE-L2", "CAFE-L3", "CAFE-ROOF", "MAIN-L0", "MAIN-L1", "MAIN-L2", "MAIN-L3", "MAIN-ROOF", "ANNEX-A-L0", "ANNEX-A-L1", "ANNEX-A-L2", "ANNEX-A-L3", "ANNEX-A-ROOF", "ANNEX-B-L0", "ANNEX-B-L1", "ANNEX-B-L2", "ANNEX-B-L3", "ANNEX-B-ROOF", "GREENHOUSE-L0", "GREENHOUSE-L1", "GREENHOUSE-L2", "GREENHOUSE-L3", "GREENHOUSE-ROOF"

Write-Host "Criando $($objectIds.Count) objetos...`n" -ForegroundColor Yellow

$successCount = 0
$errorCount = 0

foreach ($objId in $objectIds) {
    $objUri = "http://hadatac.org/kb/default/$objId"

    # Criar arquivo SPARQL temporário
    $tempFile = [System.IO.Path]::GetTempFileName()
    $sparql = "PREFIX hasco: " + "<http://hadatac.org/ont/hasco#>" + "`n"
    $sparql += "PREFIX rdfs: " + "<http://www.w3.org/2000/01/rdf-schema#>" + "`n`n"
    $sparql += "INSERT DATA {`n"
    $sparql += "  " + "<$objUri>" + " a hasco:StudyObject ;`n"
    $sparql += "    hasco:isMemberOf " + "<$socUri>" + " ;`n"
    $sparql += "    hasco:originalID `"$objId`" ;`n"
    $sparql += "    rdfs:label `"$objId`" .`n"
    $sparql += "}`n"

    $sparql | Out-File -FilePath $tempFile -Encoding UTF8
    $sparqlContent = Get-Content $tempFile -Raw

    try {
        Invoke-RestMethod -Uri $fusekiEndpoint -Method POST -Body @{ update = $sparqlContent } -ContentType "application/x-www-form-urlencoded" | Out-Null
        Write-Host "  ✓ $objId" -ForegroundColor Green
        $successCount++
    }
    catch {
        Write-Host "  ✗ $objId" -ForegroundColor Red
        $errorCount++
    }
    finally {
        Remove-Item $tempFile -ErrorAction SilentlyContinue
    }
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Criados: $successCount / Erros: $errorCount" -ForegroundColor White
Write-Host "========================================" -ForegroundColor Cyan

# Verificar total
$countSparql = "PREFIX hasco: " + "<http://hadatac.org/ont/hasco#>" + " SELECT (COUNT(?obj) as ?count) WHERE { ?obj hasco:isMemberOf " + "<$socUri>" + " }"

try {
    $result = (Invoke-RestMethod -Uri $fusekiQuery -Method POST -Body @{ query = $countSparql } -ContentType "application/x-www-form-urlencoded").results.bindings
    $count = $result[0].count.value
    Write-Host "`n✅ Total no triplestore: $count objetos`n" -ForegroundColor Green
}
catch {
    Write-Host "`n✗ Erro ao verificar`n" -ForegroundColor Red
}

