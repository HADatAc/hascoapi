# PowerShell Script: Check PMSR Study Data in Triplestore

$fusekiUrl = "http://localhost:3030/store/sparql"

Write-Host "`n=== 1. CHECK STUDY OBJECT COLLECTIONS ===" -ForegroundColor Cyan

$querySOCs = @"
PREFIX hasco: <http://hadatac.org/ont/hasco#>
PREFIX pmsr: <http://pmsr.net/ont/pmsr#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?soc ?type ?label ?graph
WHERE {
  GRAPH ?graph {
    ?soc a hasco:StudyObjectCollection .
    ?soc hasco:isMemberOf <http://pmsr.net/ont/pmsr#STD-STUDY-PMSR-Simulators-1> .
    OPTIONAL { ?soc a ?type . }
    OPTIONAL { ?soc rdfs:label ?label . }
  }
}
ORDER BY ?label
"@

$body = @{
    query = $querySOCs
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri $fusekiUrl -Method Post -Body $body -ContentType "application/json"
    $count = $response.results.bindings.Count
    Write-Host "Found $count SOCs:" -ForegroundColor Green
    foreach ($binding in $response.results.bindings) {
        Write-Host "  - $($binding.label.value)" -ForegroundColor White
    }
} catch {
    Write-Host "Error querying SOCs: $_" -ForegroundColor Red
}

Write-Host "`n=== 2. CHECK INSTRUMENTS ===" -ForegroundColor Cyan

$queryInstruments = @"
PREFIX vstoi: <http://hadatac.org/ont/vstoi#>
PREFIX hasco: <http://hadatac.org/ont/hasco#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?instrument ?label ?managerEmail ?graph
WHERE {
  GRAPH ?graph {
    ?instrument hasco:hascoType vstoi:Instrument .
    OPTIONAL { ?instrument rdfs:label ?label . }
    OPTIONAL { ?instrument vstoi:hasSIRManagerEmail ?managerEmail . }
  }
}
LIMIT 10
"@

$body = @{
    query = $queryInstruments
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri $fusekiUrl -Method Post -Body $body -ContentType "application/json"
    $count = $response.results.bindings.Count
    Write-Host "Found $count Instruments:" -ForegroundColor Green
    foreach ($binding in $response.results.bindings) {
        $label = $binding.label.value
        $email = if ($binding.managerEmail) { $binding.managerEmail.value } else { "MISSING" }
        $graph = $binding.graph.value
        Write-Host "  - $label" -ForegroundColor White
        Write-Host "    Email: $email" -ForegroundColor Yellow
        Write-Host "    Graph: $graph" -ForegroundColor Gray
    }
} catch {
    Write-Host "Error querying Instruments: $_" -ForegroundColor Red
}

Write-Host "`n=== 3. CHECK STUDY OBJECTS (Instruments as StudyObjects) ===" -ForegroundColor Cyan

$queryStudyObjects = @"
PREFIX hasco: <http://hadatac.org/ont/hasco#>
PREFIX vstoi: <http://hadatac.org/ont/vstoi#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?obj ?type ?label ?soc ?managerEmail ?graph
WHERE {
  GRAPH ?graph {
    ?obj a hasco:StudyObject .
    ?obj a ?type .
    FILTER(CONTAINS(STR(?type), "vstoi"))
    ?obj hasco:isMemberOf ?soc .
    OPTIONAL { ?obj rdfs:label ?label . }
    OPTIONAL { ?obj vstoi:hasSIRManagerEmail ?managerEmail . }
  }
}
LIMIT 10
"@

$body = @{
    query = $queryStudyObjects
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri $fusekiUrl -Method Post -Body $body -ContentType "application/json"
    $count = $response.results.bindings.Count
    Write-Host "Found $count StudyObjects with VSTOI types:" -ForegroundColor Green
    foreach ($binding in $response.results.bindings) {
        $label = $binding.label.value
        $type = $binding.type.value
        $email = if ($binding.managerEmail) { $binding.managerEmail.value } else { "MISSING" }
        Write-Host "  - $label [$type]" -ForegroundColor White
        Write-Host "    Email: $email" -ForegroundColor Yellow
    }
} catch {
    Write-Host "Error querying StudyObjects: $_" -ForegroundColor Red
}

Write-Host "`n=== 4. CHECK EMAIL IN STD ===" -ForegroundColor Cyan

$querySTD = @"
PREFIX hasco: <http://hadatac.org/ont/hasco#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?study ?email ?pi
WHERE {
  GRAPH ?graph {
    ?study a hasco:Study .
    FILTER(CONTAINS(STR(?study), "PMSR"))
    OPTIONAL { ?study hasco:hasPI ?pi . }
    ?pi hasco:hasEmail ?email .
  }
}
"@

$body = @{
    query = $querySTD
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri $fusekiUrl -Method Post -Body $body -ContentType "application/json"
    Write-Host "Study email found:" -ForegroundColor Green
    foreach ($binding in $response.results.bindings) {
        $email = $binding.email.value
        Write-Host "  Email: $email" -ForegroundColor Yellow
    }
} catch {
    Write-Host "Error querying Study: $_" -ForegroundColor Red
}

Write-Host "`n=== DONE ===" -ForegroundColor Cyan

