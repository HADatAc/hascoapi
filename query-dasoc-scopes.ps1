# ============================================================================
# SCRIPT: Consultar Scopes Extras do DA-SOC
# ============================================================================
# Este script executa queries SPARQL para buscar as propriedades extras
# adicionadas aos 50 objetos do SOC-LOCATION pelo DA-SOC-LOCATION.csv
# ============================================================================

$FUSEKI_ENDPOINT = "http://localhost:3030/store/query"
$FUSEKI_UPDATE = "http://localhost:3030/store/update"

Write-Host "`n" -NoNewline
Write-Host "=" * 80 -ForegroundColor Cyan
Write-Host "CONSULTAR SCOPES EXTRAS DO DA-SOC-LOCATION" -ForegroundColor Cyan
Write-Host "=" * 80 -ForegroundColor Cyan
Write-Host ""

# ----------------------------------------------------------------------------
# QUERY 1: Buscar todos os 50 objetos com scopes extras
# ----------------------------------------------------------------------------
Write-Host "`nQUERY 1: Buscar TODOS os objetos com scopes extras" -ForegroundColor Yellow
Write-Host "-" * 80 -ForegroundColor Yellow

$query1 = @"
PREFIX hasco: <http://hadatac.org/ont/hasco#>
PREFIX pharma: <http://hadatac.org/ont/pharma#>

SELECT ?originalID ?altitude ?floor ?zone ?area ?orientation
WHERE {
  ?objUri hasco:isMemberOf ?socUri .
  ?objUri hasco:originalID ?originalID .
  ?objUri pharma:altitude_m ?altitude .
  ?objUri pharma:floor ?floor .
  ?objUri pharma:zone_code ?zone .
  ?objUri pharma:area_m2 ?area .
  ?objUri pharma:orientation ?orientation .

  FILTER(CONTAINS(STR(?socUri), "SOC-LOCATION"))
}
ORDER BY ?originalID
"@

try {
    $response1 = Invoke-RestMethod -Uri $FUSEKI_ENDPOINT `
        -Method Post `
        -Body @{ query = $query1 } `
        -ContentType "application/x-www-form-urlencoded"

    $count = $response1.results.bindings.Count
    Write-Host "✓ Total de objetos encontrados: $count" -ForegroundColor Green
    Write-Host ""

    if ($count -gt 0) {
        Write-Host "Primeiros 10 objetos:" -ForegroundColor White
        $response1.results.bindings | Select-Object -First 10 | ForEach-Object {
            $id = $_.originalID.value
            $alt = $_.altitude.value
            $floor = $_.floor.value
            $zone = $_.zone.value
            $area = $_.area.value
            $orient = $_.orientation.value

            Write-Host "  - $id" -ForegroundColor White
            Write-Host "    Altitude: $alt m | Andar: $floor | Zona: $zone | Área: $area m² | Orientação: $orient" -ForegroundColor Gray
        }
    }
} catch {
    Write-Host "✗ Erro ao executar query: $_" -ForegroundColor Red
}

# ----------------------------------------------------------------------------
# QUERY 2: Estatísticas por ZONA
# ----------------------------------------------------------------------------
Write-Host "`n`nQUERY 2: Estatísticas por ZONA" -ForegroundColor Yellow
Write-Host "-" * 80 -ForegroundColor Yellow

$query2 = @"
PREFIX hasco: <http://hadatac.org/ont/hasco#>
PREFIX pharma: <http://hadatac.org/ont/pharma#>

SELECT ?zone (COUNT(?objUri) AS ?count)
WHERE {
  ?objUri hasco:isMemberOf ?socUri .
  ?objUri pharma:zone_code ?zone .

  FILTER(CONTAINS(STR(?socUri), "SOC-LOCATION"))
}
GROUP BY ?zone
ORDER BY ?zone
"@

try {
    $response2 = Invoke-RestMethod -Uri $FUSEKI_ENDPOINT `
        -Method Post `
        -Body @{ query = $query2 } `
        -ContentType "application/x-www-form-urlencoded"

    Write-Host ""
    $response2.results.bindings | ForEach-Object {
        $zone = $_.zone.value
        $count = $_.count.value
        Write-Host "  $zone : $count objetos" -ForegroundColor White
    }
} catch {
    Write-Host "✗ Erro ao executar query: $_" -ForegroundColor Red
}

# ----------------------------------------------------------------------------
# QUERY 3: Estatísticas por ANDAR
# ----------------------------------------------------------------------------
Write-Host "`n`nQUERY 3: Estatísticas por ANDAR" -ForegroundColor Yellow
Write-Host "-" * 80 -ForegroundColor Yellow

$query3 = @"
PREFIX hasco: <http://hadatac.org/ont/hasco#>
PREFIX pharma: <http://hadatac.org/ont/pharma#>

SELECT ?floor (COUNT(?objUri) AS ?count)
WHERE {
  ?objUri hasco:isMemberOf ?socUri .
  ?objUri pharma:floor ?floor .

  FILTER(CONTAINS(STR(?socUri), "SOC-LOCATION"))
}
GROUP BY ?floor
ORDER BY ?floor
"@

try {
    $response3 = Invoke-RestMethod -Uri $FUSEKI_ENDPOINT `
        -Method Post `
        -Body @{ query = $query3 } `
        -ContentType "application/x-www-form-urlencoded"

    Write-Host ""
    $response3.results.bindings | ForEach-Object {
        $floor = $_.floor.value
        $count = $_.count.value
        Write-Host "  Andar $floor : $count objetos" -ForegroundColor White
    }
} catch {
    Write-Host "✗ Erro ao executar query: $_" -ForegroundColor Red
}

# ----------------------------------------------------------------------------
# QUERY 4: Objetos da ZONE-A
# ----------------------------------------------------------------------------
Write-Host "`n`nQUERY 4: Objetos da ZONE-A" -ForegroundColor Yellow
Write-Host "-" * 80 -ForegroundColor Yellow

$query4 = @"
PREFIX hasco: <http://hadatac.org/ont/hasco#>
PREFIX pharma: <http://hadatac.org/ont/pharma#>

SELECT ?originalID ?floor ?altitude ?area ?orientation
WHERE {
  ?objUri hasco:isMemberOf ?socUri .
  ?objUri hasco:originalID ?originalID .
  ?objUri pharma:zone_code "ZONE-A" .
  ?objUri pharma:floor ?floor .
  ?objUri pharma:altitude_m ?altitude .
  ?objUri pharma:area_m2 ?area .
  ?objUri pharma:orientation ?orientation .

  FILTER(CONTAINS(STR(?socUri), "SOC-LOCATION"))
}
ORDER BY ?floor ?originalID
"@

try {
    $response4 = Invoke-RestMethod -Uri $FUSEKI_ENDPOINT `
        -Method Post `
        -Body @{ query = $query4 } `
        -ContentType "application/x-www-form-urlencoded"

    $count = $response4.results.bindings.Count
    Write-Host "`n✓ Total de objetos na ZONE-A: $count" -ForegroundColor Green
    Write-Host ""

    $response4.results.bindings | ForEach-Object {
        $id = $_.originalID.value
        $floor = $_.floor.value
        $alt = $_.altitude.value
        $area = $_.area.value
        $orient = $_.orientation.value

        Write-Host "  - $id (andar $floor, $alt m, $area m², $orient)" -ForegroundColor White
    }
} catch {
    Write-Host "✗ Erro ao executar query: $_" -ForegroundColor Red
}

# ----------------------------------------------------------------------------
# QUERY 5: Objetos do andar 0 (térreo)
# ----------------------------------------------------------------------------
Write-Host "`n`nQUERY 5: Objetos do andar 0 (térreo)" -ForegroundColor Yellow
Write-Host "-" * 80 -ForegroundColor Yellow

$query5 = @"
PREFIX hasco: <http://hadatac.org/ont/hasco#>
PREFIX pharma: <http://hadatac.org/ont/pharma#>

SELECT ?originalID ?zone ?altitude ?area ?orientation
WHERE {
  ?objUri hasco:isMemberOf ?socUri .
  ?objUri hasco:originalID ?originalID .
  ?objUri pharma:floor "0" .
  ?objUri pharma:zone_code ?zone .
  ?objUri pharma:altitude_m ?altitude .
  ?objUri pharma:area_m2 ?area .
  ?objUri pharma:orientation ?orientation .

  FILTER(CONTAINS(STR(?socUri), "SOC-LOCATION"))
}
ORDER BY ?zone ?originalID
"@

try {
    $response5 = Invoke-RestMethod -Uri $FUSEKI_ENDPOINT `
        -Method Post `
        -Body @{ query = $query5 } `
        -ContentType "application/x-www-form-urlencoded"

    $count = $response5.results.bindings.Count
    Write-Host "`n✓ Total de objetos no andar 0: $count" -ForegroundColor Green
    Write-Host ""

    $response5.results.bindings | ForEach-Object {
        $id = $_.originalID.value
        $zone = $_.zone.value
        $alt = $_.altitude.value
        $area = $_.area.value
        $orient = $_.orientation.value

        Write-Host "  - $id (zona $zone, $alt m, $area m², $orient)" -ForegroundColor White
    }
} catch {
    Write-Host "✗ Erro ao executar query: $_" -ForegroundColor Red
}

# ----------------------------------------------------------------------------
# QUERY 6: Objetos com altitude > 25m
# ----------------------------------------------------------------------------
Write-Host "`n`nQUERY 6: Objetos com altitude > 25m" -ForegroundColor Yellow
Write-Host "-" * 80 -ForegroundColor Yellow

$query6 = @"
PREFIX hasco: <http://hadatac.org/ont/hasco#>
PREFIX pharma: <http://hadatac.org/ont/pharma#>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

SELECT ?originalID ?altitude ?floor ?zone
WHERE {
  ?objUri hasco:isMemberOf ?socUri .
  ?objUri hasco:originalID ?originalID .
  ?objUri pharma:altitude_m ?altitude .
  ?objUri pharma:floor ?floor .
  ?objUri pharma:zone_code ?zone .

  FILTER(CONTAINS(STR(?socUri), "SOC-LOCATION"))
  FILTER(xsd:decimal(?altitude) > 25.0)
}
ORDER BY DESC(?altitude)
"@

try {
    $response6 = Invoke-RestMethod -Uri $FUSEKI_ENDPOINT `
        -Method Post `
        -Body @{ query = $query6 } `
        -ContentType "application/x-www-form-urlencoded"

    $count = $response6.results.bindings.Count
    Write-Host "`n✓ Total de objetos com altitude > 25m: $count" -ForegroundColor Green
    Write-Host ""

    $response6.results.bindings | ForEach-Object {
        $id = $_.originalID.value
        $alt = $_.altitude.value
        $floor = $_.floor.value
        $zone = $_.zone.value

        Write-Host "  - $id ($alt m, andar $floor, zona $zone)" -ForegroundColor White
    }
} catch {
    Write-Host "✗ Erro ao executar query: $_" -ForegroundColor Red
}

Write-Host "`n" -NoNewline
Write-Host "=" * 80 -ForegroundColor Cyan
Write-Host "CONSULTA COMPLETA" -ForegroundColor Cyan
Write-Host "=" * 80 -ForegroundColor Cyan
Write-Host ""

