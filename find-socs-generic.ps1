# Find SOCs - Generic approach
$fusekiUrl = "http://localhost:3030/store"

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "Finding SOCs with generic queries..." -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan

# Query 1: Search by URI pattern
$query1 = @"
SELECT DISTINCT ?s ?p ?o ?g WHERE {
  {
    ?s ?p ?o .
    FILTER(CONTAINS(STR(?s), "EQUIPMENT-MODULE") || CONTAINS(STR(?s), "SLOT-ELEMENT") || CONTAINS(STR(?s), "COMPONENT-STEM") || CONTAINS(STR(?s), "CONTROL-MODULE"))
    BIND("DEFAULT" as ?g)
  }
  UNION
  {
    GRAPH ?g {
      ?s ?p ?o .
      FILTER(CONTAINS(STR(?s), "EQUIPMENT-MODULE") || CONTAINS(STR(?s), "SLOT-ELEMENT") || CONTAINS(STR(?s), "COMPONENT-STEM") || CONTAINS(STR(?s), "CONTROL-MODULE"))
    }
  }
}
LIMIT 50
"@

Write-Host "`n1. Searching by URI pattern..." -ForegroundColor Yellow
try {
    $response1 = Invoke-RestMethod -Uri "$fusekiUrl/query" -Method Post -Body @{ query = $query1 }
    if ($response1.results.bindings.Count -gt 0) {
        Write-Host "   ✅ Found $($response1.results.bindings.Count) triples" -ForegroundColor Green
        $grouped = $response1.results.bindings | Group-Object { $_.s.value }
        foreach ($g in $grouped) {
            Write-Host "`n   Subject: $($g.Name)" -ForegroundColor Cyan
            foreach ($b in $g.Group | Select-Object -First 5) {
                Write-Host "      $($b.p.value) -> $($b.o.value)" -ForegroundColor Gray
                if ($b.g) {
                    Write-Host "        (in graph: $($b.g.value))" -ForegroundColor DarkGray
                }
            }
        }
    } else {
        Write-Host "   ❌ No matches found" -ForegroundColor Red
    }
} catch {
    Write-Host "   ❌ Error: $_" -ForegroundColor Red
}

# Query 2: Find all types
$query2 = @"
SELECT DISTINCT ?s ?type ?label ?g WHERE {
  {
    ?s a ?type .
    OPTIONAL { ?s <http://www.w3.org/2000/01/rdf-schema#label> ?label }
    FILTER(CONTAINS(STR(?s), "OCL_") || CONTAINS(STR(?s), "SOC-"))
    BIND("DEFAULT" as ?g)
  }
  UNION
  {
    GRAPH ?g {
      ?s a ?type .
      OPTIONAL { ?s <http://www.w3.org/2000/01/rdf-schema#label> ?label }
      FILTER(CONTAINS(STR(?s), "OCL_") || CONTAINS(STR(?s), "SOC-"))
    }
  }
}
ORDER BY ?s
LIMIT 20
"@

Write-Host "`n2. Finding types of SOC-like objects..." -ForegroundColor Yellow
try {
    $response2 = Invoke-RestMethod -Uri "$fusekiUrl/query" -Method Post -Body @{ query = $query2 }
    if ($response2.results.bindings.Count -gt 0) {
        Write-Host "   ✅ Found $($response2.results.bindings.Count) objects" -ForegroundColor Green
        foreach ($b in $response2.results.bindings) {
            Write-Host "`n   URI: $($b.s.value)" -ForegroundColor Cyan
            Write-Host "      Type: $($b.type.value)" -ForegroundColor Yellow
            if ($b.label) {
                Write-Host "      Label: $($b.label.value)" -ForegroundColor Green
            }
            if ($b.g -and $b.g.value -ne "DEFAULT") {
                Write-Host "      Graph: $($b.g.value)" -ForegroundColor Magenta
            }
        }
    } else {
        Write-Host "   ❌ No objects found" -ForegroundColor Red
    }
} catch {
    Write-Host "   ❌ Error: $_" -ForegroundColor Red
}

# Query 3: Find by hascoType
$query3 = @"
PREFIX hasco: <http://hadatac.org/ont/hasco/>

SELECT DISTINCT ?s ?hascoType ?type ?g WHERE {
  {
    ?s hasco:hascoType ?hascoType .
    OPTIONAL { ?s a ?type }
    FILTER(CONTAINS(STR(?s), "OCL_") || CONTAINS(STR(?s), "SOC-"))
    BIND("DEFAULT" as ?g)
  }
  UNION
  {
    GRAPH ?g {
      ?s hasco:hascoType ?hascoType .
      OPTIONAL { ?s a ?type }
      FILTER(CONTAINS(STR(?s), "OCL_") || CONTAINS(STR(?s), "SOC-"))
    }
  }
}
LIMIT 20
"@

Write-Host "`n3. Finding by hascoType..." -ForegroundColor Yellow
try {
    $response3 = Invoke-RestMethod -Uri "$fusekiUrl/query" -Method Post -Body @{ query = $query3 }
    if ($response3.results.bindings.Count -gt 0) {
        Write-Host "   ✅ Found $($response3.results.bindings.Count) objects with hascoType" -ForegroundColor Green
        foreach ($b in $response3.results.bindings) {
            Write-Host "`n   URI: $($b.s.value)" -ForegroundColor Cyan
            Write-Host "      hascoType: $($b.hascoType.value)" -ForegroundColor Yellow
            if ($b.type) {
                Write-Host "      rdf:type: $($b.type.value)" -ForegroundColor Gray
            }
            if ($b.g -and $b.g.value -ne "DEFAULT") {
                Write-Host "      Graph: $($b.g.value)" -ForegroundColor Magenta
            }
        }
    } else {
        Write-Host "   ❌ No objects with hascoType found" -ForegroundColor Red
    }
} catch {
    Write-Host "   ❌ Error: $_" -ForegroundColor Red
}

Write-Host "`n================================================" -ForegroundColor Cyan

