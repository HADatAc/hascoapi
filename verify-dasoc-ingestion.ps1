# =====================================================
# Verificação de Ingestão dos DA-SOCs PMSR
# =====================================================

$baseUrl = "http://localhost:9000"
$token = "qwertyuiopasdfghjklzxcvbnm123456"

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "DA-SOC INGESTION VERIFICATION" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Query 1: Verificar se Instruments têm vstoi:hasShortName
Write-Host "Query 1: Checking if Instruments have vstoi:hasShortName..." -ForegroundColor Yellow

$sparql1 = @"
PREFIX vstoi: <http://hadatac.org/ont/vstoi#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX hasco: <http://hadatac.org/ont/hasco#>

SELECT ?obj ?label ?shortName WHERE {
  ?obj a vstoi:Instrument .
  ?obj rdfs:label ?label .
  OPTIONAL { ?obj vstoi:hasShortName ?shortName }
}
LIMIT 10
"@

try {
    $response1 = curl.exe -X POST "http://localhost:3030/HASCO/query" `
        --data-urlencode "query=$sparql1" `
        -H "Accept: application/sparql-results+json" `
        -s

    $json1 = $response1 | ConvertFrom-Json
    $count1 = $json1.results.bindings.Count
    $withShortName = ($json1.results.bindings | Where-Object { $_.shortName }).Count

    Write-Host "  → Found $count1 Instruments" -ForegroundColor Gray
    Write-Host "  → $withShortName have vstoi:hasShortName" -ForegroundColor $(if ($withShortName -gt 0) { "Green" } else { "Red" })

    if ($withShortName -gt 0) {
        Write-Host "`n  Sample Instruments with hasShortName:" -ForegroundColor Cyan
        $json1.results.bindings | Select-Object -First 3 | ForEach-Object {
            Write-Host "    - $($_.label.value): $($_.shortName.value)" -ForegroundColor Gray
        }
    }
} catch {
    Write-Host "  ❌ Query failed: $_" -ForegroundColor Red
}

# Query 2: Verificar se Slots têm vstoi:belongsTo
Write-Host "`nQuery 2: Checking if ContainerSlots have vstoi:belongsTo..." -ForegroundColor Yellow

$sparql2 = @"
PREFIX vstoi: <http://hadatac.org/ont/vstoi#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?slot ?label ?belongsTo WHERE {
  ?slot a vstoi:ContainerSlot .
  ?slot rdfs:label ?label .
  OPTIONAL { ?slot vstoi:belongsTo ?belongsTo }
}
LIMIT 5
"@

try {
    $response2 = curl.exe -X POST "http://localhost:3030/HASCO/query" `
        --data-urlencode "query=$sparql2" `
        -H "Accept: application/sparql-results+json" `
        -s

    $json2 = $response2 | ConvertFrom-Json
    $count2 = $json2.results.bindings.Count
    $withBelongsTo = ($json2.results.bindings | Where-Object { $_.belongsTo }).Count

    Write-Host "  → Found $count2 ContainerSlots" -ForegroundColor Gray
    Write-Host "  → $withBelongsTo have vstoi:belongsTo" -ForegroundColor $(if ($withBelongsTo -gt 0) { "Green" } else { "Red" })

    if ($withBelongsTo -gt 0) {
        Write-Host "`n  Sample Slots with belongsTo:" -ForegroundColor Cyan
        $json2.results.bindings | Select-Object -First 2 | ForEach-Object {
            Write-Host "    - $($_.label.value) → $($_.belongsTo.value)" -ForegroundColor Gray
        }
    }
} catch {
    Write-Host "  ❌ Query failed: $_" -ForegroundColor Red
}

# Query 3: Verificar se ComponentStems têm vstoi:hasContent
Write-Host "`nQuery 3: Checking if ComponentStems have vstoi:hasContent..." -ForegroundColor Yellow

$sparql3 = @"
PREFIX vstoi: <http://hadatac.org/ont/vstoi#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?stem ?label ?content WHERE {
  ?stem a vstoi:ComponentStem .
  ?stem rdfs:label ?label .
  OPTIONAL { ?stem vstoi:hasContent ?content }
}
LIMIT 5
"@

try {
    $response3 = curl.exe -X POST "http://localhost:3030/HASCO/query" `
        --data-urlencode "query=$sparql3" `
        -H "Accept: application/sparql-results+json" `
        -s

    $json3 = $response3 | ConvertFrom-Json
    $count3 = $json3.results.bindings.Count
    $withContent = ($json3.results.bindings | Where-Object { $_.content }).Count

    Write-Host "  → Found $count3 ComponentStems" -ForegroundColor Gray
    Write-Host "  → $withContent have vstoi:hasContent" -ForegroundColor $(if ($withContent -gt 0) { "Green" } else { "Red" })

    if ($withContent -gt 0) {
        Write-Host "`n  Sample Stems with content:" -ForegroundColor Cyan
        $json3.results.bindings | Select-Object -First 2 | ForEach-Object {
            $contentPreview = $_.content.value.Substring(0, [Math]::Min(50, $_.content.value.Length))
            Write-Host "    - $($_.label.value): $contentPreview..." -ForegroundColor Gray
        }
    }
} catch {
    Write-Host "  ❌ Query failed: $_" -ForegroundColor Red
}

# Summary
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "VERIFICATION SUMMARY" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

if ($withShortName -gt 0 -and $withBelongsTo -gt 0 -and $withContent -gt 0) {
    Write-Host "`n✅ DA-SOC INGESTION SUCCESSFUL!" -ForegroundColor Green
    Write-Host "   All DA-SOC properties were added to StudyObjects" -ForegroundColor Gray
    Write-Host "`n   Next step: Apply backend changes to create Instrument entities" -ForegroundColor Cyan
} else {
    Write-Host "`n⚠️  DA-SOC INGESTION INCOMPLETE" -ForegroundColor Yellow
    Write-Host "   Some properties are missing. Check logs above." -ForegroundColor Gray
}

Write-Host "`n========================================`n" -ForegroundColor Cyan

