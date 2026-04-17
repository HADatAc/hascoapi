# Script rápido para verificar objetos SOC-LOCATION
$query = "PREFIX hasco: <http://hadatac.org/ont/hasco#> PREFIX pharma: <http://hadatac.org/ont/pharma#> SELECT (COUNT(?obj) as ?count) WHERE { ?obj hasco:isMemberOf ?soc . ?obj pharma:altitude_m ?alt . FILTER(CONTAINS(STR(?soc), 'SOC-LOCATION')) }"

$body = "query=" + [System.Uri]::EscapeDataString($query)
$response = Invoke-RestMethod -Uri "http://localhost:3030/store/query" -Method Post -Body $body -ContentType "application/x-www-form-urlencoded"

$count = $response.results.bindings[0].count.value
Write-Host "Objetos SOC-LOCATION com propriedades extras do DA-SOC: $count" -ForegroundColor $(if ($count -eq "50") { "Green" } elseif ($count -eq "0") { "Red" } else { "Yellow" })

