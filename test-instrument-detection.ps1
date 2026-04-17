# =====================================================
# Script de Teste: Detecção de Tipos de Instruments
# =====================================================
# Este script verifica em detalhes por que a detecção
# de tipos vstoi não está funcionando
# =====================================================

$baseUrl = "http://localhost:9000"
$token = "qwertyuiopasdfghjklzxcvbnm123456"

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "TESTE DE DETECÇÃO DE TIPOS VSTOI" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# PASSO 1: Verificar servidor
Write-Host "Passo 1: Verificando servidor..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "$baseUrl/hascoapi/version" -Method GET
    Write-Host "  ✅ Servidor rodando: $health" -ForegroundColor Green
} catch {
    Write-Host "  ❌ Servidor não está respondendo!" -ForegroundColor Red
    exit
}

# PASSO 2: Listar SOCs
Write-Host "`nPasso 2: Listando SOCs..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/hascoapi/api/studyobjectcollection/elements/10/0" `
        -Method GET `
        -Headers @{"Authorization" = "Bearer $token"}

    if ($response.isSuccessful -and $response.body) {
        Write-Host "  ✅ Encontrados $($response.body.Count) SOCs:" -ForegroundColor Green
        foreach ($soc in $response.body) {
            Write-Host "    - $($soc.label) (URI: $($soc.uri))" -ForegroundColor Gray
        }
    } else {
        Write-Host "  ⚠️ Nenhum SOC encontrado" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  ❌ Erro ao listar SOCs: $_" -ForegroundColor Red
}

# PASSO 3: Query SPARQL direta para verificar rdf:type dos StudyObjects
Write-Host "`nPasso 3: Consultando tipos RDF dos StudyObjects (via SPARQL)..." -ForegroundColor Yellow
$sparqlQuery = @"
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX hasco: <http://hadatac.org/ont/hasco#>
PREFIX vstoi: <http://hadatac.org/ont/vstoi#>

SELECT ?so ?label ?type ?hascoType (COUNT(?so) as ?count) WHERE {
  ?so a ?type .
  ?so hasco:hascoType hasco:StudyObject .
  OPTIONAL { ?so rdfs:label ?label }
  OPTIONAL { ?so hasco:hascoType ?hascoType }
}
GROUP BY ?type
ORDER BY ?type
LIMIT 20
"@

try {
    $sparqlResponse = Invoke-RestMethod -Uri "http://localhost:3030/HASCO/query" `
        -Method POST `
        -Headers @{"Content-Type" = "application/x-www-form-urlencoded"} `
        -Body "query=$([System.Web.HttpUtility]::UrlEncode($sparqlQuery))"

    Write-Host "  ✅ Tipos encontrados:" -ForegroundColor Green
    if ($sparqlResponse.results.bindings) {
        foreach ($binding in $sparqlResponse.results.bindings) {
            $type = if ($binding.type.value) { $binding.type.value } else { "N/A" }
            $count = if ($binding.count.value) { $binding.count.value } else { "0" }
            Write-Host "    - $type ($count objetos)" -ForegroundColor Gray
        }
    } else {
        Write-Host "    ⚠️ Nenhum resultado" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  ⚠️ Não foi possível consultar Fuseki diretamente (erro esperado se HttpUtility não disponível)" -ForegroundColor Yellow
    Write-Host "  Execute esta query manualmente em http://localhost:3030/HASCO/sparql:" -ForegroundColor Yellow
    Write-Host $sparqlQuery -ForegroundColor Gray
}

# PASSO 4: Verificar Instruments
Write-Host "`nPasso 4: Verificando Instruments criados..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/hascoapi/api/instrument/elements/10/0" `
        -Method GET `
        -Headers @{"Authorization" = "Bearer $token"}

    if ($response.isSuccessful -and $response.body -and $response.body.Count -gt 0) {
        Write-Host "  ✅ Encontrados $($response.body.Count) Instruments:" -ForegroundColor Green
        foreach ($inst in $response.body | Select-Object -First 5) {
            Write-Host "    - $($inst.label)" -ForegroundColor Gray
        }
    } else {
        Write-Host "  ❌ Nenhum Instrument encontrado!" -ForegroundColor Red
        Write-Host "  Resposta: $($response | ConvertTo-Json -Depth 3)" -ForegroundColor Red
    }
} catch {
    Write-Host "  ❌ Erro ao verificar Instruments: $_" -ForegroundColor Red
}

# PASSO 5: Verificar total
Write-Host "`nPasso 5: Verificando total de Instruments..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/hascoapi/api/instrument/elements/total" `
        -Method GET `
        -Headers @{"Authorization" = "Bearer $token"}

    if ($response.isSuccessful) {
        Write-Host "  Total: $($response.body)" -ForegroundColor Green
    } else {
        Write-Host "  ❌ Falha: $($response.body)" -ForegroundColor Red
    }
} catch {
    Write-Host "  ❌ Erro: $_" -ForegroundColor Red
}

# RESUMO E PRÓXIMOS PASSOS
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "PRÓXIMOS PASSOS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "1. Verifique os LOGS do backend (console onde rodou 'sbt run')" -ForegroundColor White
Write-Host "   Procure por:" -ForegroundColor White
Write-Host "   - '=== INSTRUMENT INFERENCE ENGINE ==='" -ForegroundColor Gray
Write-Host "   - 'Processing SOC: ...' (deve mostrar TODAS as 4 SOCs)" -ForegroundColor Gray
Write-Host "   - 'typeUri (normalized): ...' (deve ser URI completa)" -ForegroundColor Gray
Write-Host "   - 'Equals INSTRUMENT? true/false'" -ForegroundColor Gray
Write-Host "`n2. Se 'typeUri (normalized)' for 'vstoi:Instrument':" -ForegroundColor White
Write-Host "   → O problema é que URIUtils.replacePrefixEx() não está expandindo" -ForegroundColor Yellow
Write-Host "`n3. Se 'typeUri (normalized)' for 'http://hadatac.org/ont/vstoi#Instrument':" -ForegroundColor White
Write-Host "   → O problema é outra comparação de string" -ForegroundColor Yellow
Write-Host "`n4. Se não aparecer nenhum log 'Processing SOC':" -ForegroundColor White
Write-Host "   → O postprocess() não está sendo chamado ou SOCs não existem" -ForegroundColor Yellow
Write-Host "========================================`n" -ForegroundColor Cyan

