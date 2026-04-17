# ============================================================================
# SCRIPT: Consultar Scopes Extras do DA-SOC (Versão Simplificada)
# ============================================================================

$FUSEKI_ENDPOINT = "http://localhost:3030/store/query"

Write-Host ""
Write-Host ("=" * 80) -ForegroundColor Cyan
Write-Host "CONSULTAR SCOPES EXTRAS DO DA-SOC-LOCATION" -ForegroundColor Cyan
Write-Host ("=" * 80) -ForegroundColor Cyan
Write-Host ""

# ----------------------------------------------------------------------------
# QUERY 1: Buscar todos os 50 objetos com scopes extras
# ----------------------------------------------------------------------------
Write-Host "QUERY 1: Buscar TODOS os objetos com scopes extras" -ForegroundColor Yellow
Write-Host ("-" * 80) -ForegroundColor Yellow
Write-Host ""

$query1 = "PREFIX hasco: <http://hadatac.org/ont/hasco#> PREFIX pharma: <http://hadatac.org/ont/pharma#> SELECT ?originalID ?altitude ?floor ?zone ?area ?orientation WHERE { ?objUri hasco:isMemberOf ?socUri . ?objUri hasco:originalID ?originalID . ?objUri pharma:altitude_m ?altitude . ?objUri pharma:floor ?floor . ?objUri pharma:zone_code ?zone . ?objUri pharma:area_m2 ?area . ?objUri pharma:orientation ?orientation . FILTER(CONTAINS(STR(?socUri), `"SOC-LOCATION`")) } ORDER BY ?originalID"

try {
    $body = "query=" + [System.Uri]::EscapeDataString($query1)
    $response1 = Invoke-RestMethod -Uri $FUSEKI_ENDPOINT -Method Post -Body $body -ContentType "application/x-www-form-urlencoded"

    $count = $response1.results.bindings.Count
    Write-Host "Total de objetos encontrados: $count" -ForegroundColor Green
    Write-Host ""

    if ($count -gt 0) {
        Write-Host "Primeiros 10 objetos:" -ForegroundColor White
        Write-Host ""

        $first10 = $response1.results.bindings | Select-Object -First 10
        foreach ($obj in $first10) {
            $id = $obj.originalID.value
            $alt = $obj.altitude.value
            $floor = $obj.floor.value
            $zone = $obj.zone.value
            $area = $obj.area.value
            $orient = $obj.orientation.value

            Write-Host "  * $id" -ForegroundColor White
            Write-Host "    - Altitude: $alt m" -ForegroundColor Gray
            Write-Host "    - Andar: $floor" -ForegroundColor Gray
            Write-Host "    - Zona: $zone" -ForegroundColor Gray
            Write-Host "    - Area: $area m2" -ForegroundColor Gray
            Write-Host "    - Orientacao: $orient" -ForegroundColor Gray
            Write-Host ""
        }
    } else {
        Write-Host "ATENCAO: Nenhum objeto encontrado com scopes extras!" -ForegroundColor Red
        Write-Host "Isso pode significar que o DA-SOC-LOCATION.csv ainda nao foi ingerido." -ForegroundColor Red
        Write-Host ""
    }
} catch {
    Write-Host "Erro ao executar query: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# ----------------------------------------------------------------------------
# QUERY 2: Estatísticas por ZONA
# ----------------------------------------------------------------------------
Write-Host ""
Write-Host "QUERY 2: Estatisticas por ZONA" -ForegroundColor Yellow
Write-Host ("-" * 80) -ForegroundColor Yellow
Write-Host ""

$query2 = "PREFIX hasco: <http://hadatac.org/ont/hasco#> PREFIX pharma: <http://hadatac.org/ont/pharma#> SELECT ?zone (COUNT(?objUri) AS ?count) WHERE { ?objUri hasco:isMemberOf ?socUri . ?objUri pharma:zone_code ?zone . FILTER(CONTAINS(STR(?socUri), `"SOC-LOCATION`")) } GROUP BY ?zone ORDER BY ?zone"

try {
    $body = "query=" + [System.Uri]::EscapeDataString($query2)
    $response2 = Invoke-RestMethod -Uri $FUSEKI_ENDPOINT -Method Post -Body $body -ContentType "application/x-www-form-urlencoded"

    if ($response2.results.bindings.Count -gt 0) {
        foreach ($item in $response2.results.bindings) {
            $zone = $item.zone.value
            $count = $item.count.value
            Write-Host "  $zone : $count objetos" -ForegroundColor White
        }
    } else {
        Write-Host "  Nenhum resultado" -ForegroundColor Red
    }
    Write-Host ""
} catch {
    Write-Host "Erro ao executar query: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# ----------------------------------------------------------------------------
# QUERY 3: Estatísticas por ANDAR
# ----------------------------------------------------------------------------
Write-Host ""
Write-Host "QUERY 3: Estatisticas por ANDAR" -ForegroundColor Yellow
Write-Host ("-" * 80) -ForegroundColor Yellow
Write-Host ""

$query3 = "PREFIX hasco: <http://hadatac.org/ont/hasco#> PREFIX pharma: <http://hadatac.org/ont/pharma#> SELECT ?floor (COUNT(?objUri) AS ?count) WHERE { ?objUri hasco:isMemberOf ?socUri . ?objUri pharma:floor ?floor . FILTER(CONTAINS(STR(?socUri), `"SOC-LOCATION`")) } GROUP BY ?floor ORDER BY ?floor"

try {
    $body = "query=" + [System.Uri]::EscapeDataString($query3)
    $response3 = Invoke-RestMethod -Uri $FUSEKI_ENDPOINT -Method Post -Body $body -ContentType "application/x-www-form-urlencoded"

    if ($response3.results.bindings.Count -gt 0) {
        foreach ($item in $response3.results.bindings) {
            $floor = $item.floor.value
            $count = $item.count.value
            Write-Host "  Andar $floor : $count objetos" -ForegroundColor White
        }
    } else {
        Write-Host "  Nenhum resultado" -ForegroundColor Red
    }
    Write-Host ""
} catch {
    Write-Host "Erro ao executar query: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# ----------------------------------------------------------------------------
# QUERY 4: Verificar se existem objetos SOC-LOCATION (base)
# ----------------------------------------------------------------------------
Write-Host ""
Write-Host "QUERY 4: Verificar objetos SOC-LOCATION (base do DSG)" -ForegroundColor Yellow
Write-Host ("-" * 80) -ForegroundColor Yellow
Write-Host ""

$query4 = "PREFIX hasco: <http://hadatac.org/ont/hasco#> SELECT (COUNT(?objUri) AS ?count) WHERE { ?objUri hasco:isMemberOf ?socUri . ?objUri hasco:originalID ?originalID . FILTER(CONTAINS(STR(?socUri), `"SOC-LOCATION`")) }"

try {
    $body = "query=" + [System.Uri]::EscapeDataString($query4)
    $response4 = Invoke-RestMethod -Uri $FUSEKI_ENDPOINT -Method Post -Body $body -ContentType "application/x-www-form-urlencoded"

    if ($response4.results.bindings.Count -gt 0) {
        $totalBase = $response4.results.bindings[0].count.value
        Write-Host "  Total de objetos SOC-LOCATION (base): $totalBase" -ForegroundColor Green

        if ($totalBase -eq "0") {
            Write-Host ""
            Write-Host "  ATENCAO: O DSG ainda nao foi ingerido!" -ForegroundColor Red
            Write-Host "  Voce precisa primeiro ingerir o DSG que cria os 50 objetos SOC-LOCATION" -ForegroundColor Red
        } elseif ($count -eq 0) {
            Write-Host ""
            Write-Host "  ATENCAO: O DSG foi ingerido mas o DA-SOC ainda nao!" -ForegroundColor Yellow
            Write-Host "  Os 50 objetos existem mas ainda nao tem as propriedades extras" -ForegroundColor Yellow
            Write-Host "  Voce precisa ingerir o arquivo DA-SOC-LOCATION.csv" -ForegroundColor Yellow
        } else {
            Write-Host "  DA-SOC ingerido com sucesso!" -ForegroundColor Green
        }
    }
    Write-Host ""
} catch {
    Write-Host "Erro ao executar query: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

Write-Host ""
Write-Host ("=" * 80) -ForegroundColor Cyan
Write-Host "CONSULTA COMPLETA" -ForegroundColor Cyan
Write-Host ("=" * 80) -ForegroundColor Cyan
Write-Host ""

