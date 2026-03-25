# Roundtrip Test Technical Architecture

## Overview

The **HascoRoundtripTest** is a comprehensive validation framework designed to ensure the bidirectional integrity of HASCO Machine-Readable Templates (MTs). It validates that metadata can be faithfully ingested into the triplestore, regenerated from the triplestore back into Excel format, and re-ingested to produce an identical or equivalent semantic graph.

## Core Concept: The Roundtrip Validation Pattern

The roundtrip test implements a **deterministic 3-phase lifecycle** for each metadata template type:

```
┌─────────────┐         ┌─────────────┐         ┌─────────────┐
│   Phase 1   │────────▶│   Phase 2   │────────▶│   Phase 3   │
│   INGEST    │         │  REGENERATE │         │  REINGEST   │
└─────────────┘         └─────────────┘         └─────────────┘
     │                        │                        │
     ▼                        ▼                        ▼
Excel → RDF           RDF → Excel           Excel → RDF (verify)
```

### Why This Matters

This pattern validates:
1. **Semantic preservation**: No information is lost during ingestion
2. **Generation fidelity**: The regeneration logic can reconstruct the original structure
3. **Determinism**: The same input produces the same output consistently
4. **Idempotence**: Multiple ingestion cycles don't corrupt or duplicate data

## Phase 1: Ingest (Excel → Triplestore)

### Purpose
Convert structured Excel metadata into RDF triples and store them in the triplestore.

### Process Flow

```
Excel File (.xlsx)
     │
     ├─▶ SpreadsheetRecordFile (parsing)
     │
     ├─▶ IngestionWorker.ingest()
     │        │
     │        ├─▶ AnnotateXX.exec() (validation & catalog loading)
     │        │
     │        └─▶ GeneratorChain (transformation)
     │                 │
     │                 ├─▶ BaseGenerator.createRows()
     │                 ├─▶ BaseGenerator.createObjects()
     │                 └─▶ BaseGenerator.commitRowsToTripleStore()
     │
     └─▶ Named Graph in Triplestore
```

### Technical Components

#### 1. DataFile Mocking
```java
DataFile df = mockDataFileFor(excel);
```
- Creates a test double with essential methods stubbed
- Tracks ingestion state (fileStatus, studyUri, recordFile)
- Enables testing without database dependencies

#### 2. Ingestion Execution
```java
IngestionWorker.ingest(df, excel, TEMPLATE_GENERIC, status)
```
- **Parameters**:
  - `df`: DataFile mock representing the upload
  - `excel`: Physical .xlsx file to process
  - `TEMPLATE_GENERIC`: Configuration template path
  - `status`: Initial semantic status (e.g., "Draft")

#### 3. Named Graph Storage
Each ingested file creates a unique named graph in the triplestore:
- **Graph URI**: Derived from DataFile.getUri()
- **Example**: `http://example.org/DF-INS-PMSR-Simulators.xlsx`
- All RDF triples for this ingestion are scoped to this graph

### Enhanced Analysis Features

After ingestion, the test performs **comprehensive graph analysis**:

#### A. Entity Counts by Type
Queries the triplestore to count entities grouped by `rdf:type`:

```sparql
SELECT ?type (COUNT(DISTINCT ?entity) AS ?count)
WHERE {
  GRAPH <namedGraphUri> {
    ?entity rdf:type ?type .
  }
}
GROUP BY ?type ORDER BY DESC(?count)
```

**Example Output**:
```
[ENTITY COUNTS BY TYPE]
  vstoi:InstrumentInstance: 71
  vstoi:ComponentInstance: 16
  vstoi:Deployment: 71
  vstoi:Platform: 1
  vstoi:PlatformInstance: 1
```

#### B. Relationship Completeness
Identifies **dangling references** - object URIs that don't exist as subjects:

```sparql
SELECT ?predicate (COUNT(DISTINCT ?object) AS ?danglingCount)
WHERE {
  GRAPH <namedGraphUri> {
    ?subject ?predicate ?object .
    FILTER(isURI(?object))
    FILTER NOT EXISTS {
      GRAPH <namedGraphUri> { ?object ?p ?o }
    }
  }
}
GROUP BY ?predicate ORDER BY DESC(?danglingCount)
```

**Purpose**: Ensures referential integrity within the named graph.

#### C. Property Value Statistics
Analyzes property usage patterns:

```sparql
SELECT ?property 
       (COUNT(*) AS ?valueCount) 
       (COUNT(DISTINCT ?value) AS ?distinctValues)
WHERE {
  GRAPH <namedGraphUri> {
    ?subject ?property ?value .
    FILTER(?property != rdf:type)
  }
}
GROUP BY ?property ORDER BY DESC(?valueCount)
```

**Example Output**:
```
[PROPERTY VALUE STATISTICS]
  hasco:hascoType: 160 values (5 distinct)
  hasco:hasDataFile: 160 values (1 distinct)
  rdfs:label: 159 values (71 distinct)
```

### Validation Artifacts

The test generates **Turtle (.ttl) files** for inspection:
- **Location**: `test/resources/generated/`
- **Naming**: `{MTType}_ingested_original.ttl`
- **Purpose**: Manual verification, debugging, archival

## Phase 2: Regenerate (Triplestore → Excel)

### Purpose
Query the triplestore and reconstruct an Excel workbook that represents the same metadata.

### Process Flow

```
Triplestore (RDF)
     │
     ├─▶ XXGen.genByStatus(status, filename, ...)
     │        │
     │        ├─▶ Query entities by type & status
     │        │        │
     │        │        └─▶ SPARQL SELECT (find matching entities)
     │        │
     │        ├─▶ XXGenHelper (Apache POI workbook creation)
     │        │        │
     │        │        ├─▶ createSheet(sheetName)
     │        │        ├─▶ addRow(rowData)
     │        │        └─▶ save(outputPath)
     │        │
     │        └─▶ Generated Excel (.xlsx)
     │
     └─▶ test/resources/generated/{MT}_regenerated.xlsx
```

### Technical Components

#### 1. Generation by Status
```java
String result = INSGen.genByStatus(VSTOI.DRAFT, regeneratedFilename, null, null);
```

**Status-based filtering**:
- Queries entities with matching `vstoi:hasStatus` or equivalent
- Falls back to `null` status if not explicitly set (often treated as Draft)
- Uses SPARQL `FILTER` to scope results

#### 2. Sheet Population Logic

Each MT type implements custom sheet generators:

| Sheet Type | Populated From | Key Properties |
|------------|----------------|----------------|
| **InfoSheet** | Template metadata | `hasDependencies`, `hasVersion` |
| **Namespaces** | `NameSpace` entities | `hasPrefix`, `hasNameSpace` |
| **Deployments** | `vstoi:Deployment` | `hasURI`, `rdfs:label`, deployment links |
| **InstrumentInstances** | `vstoi:InstrumentInstance` | `hasURI`, `rdf:type`, `vstoi:hasStatus` |
| **ComponentInstances** | `vstoi:ComponentInstance` | `hasURI`, `rdf:type`, `vstoi:hasSerialNumber` |

#### 3. Structural Validation
After generation, the test validates workbook structure:

```java
assertDoesNotThrow(() -> {
    try (Workbook wb = WorkbookFactory.create(new FileInputStream(out))) {
        assertNotNull(wb.getSheet("InfoSheet"));
        assertNotNull(wb.getSheet("Namespaces"));
        // ... validate required sheets exist
    }
});
```

### Re-ingestion for Analysis

The regenerated file is **immediately re-ingested**:
```java
DataFile regeneratedDf = mockDataFileFor(generatedCopy);
IngestionWorker.ingest(regeneratedDf, generatedCopy, TEMPLATE_GENERIC, status);
dumpAndLogTtl("INS_ingested_regenerated", regeneratedDf);
```

This creates a **second named graph** for comparison against the original.

## Phase 3: Reset & Deterministic Re-ingest

### Purpose
Verify that the system produces **identical or semantically equivalent results** when starting from a clean state.

### Process Flow

```
1. Delete original named graph
         │
         ▼
2. Delete regenerated named graph
         │
         ▼
3. Re-ingest ORIGINAL file → new graph A
         │
         ▼
4. Regenerate from graph A → new Excel
         │
         ▼
5. Ingest regenerated file → new graph B
         │
         ▼
6. Compare graph A vs graph B
```

### Graph Cleanup

```java
deleteNamedGraphBestEffort(originalGraphUri);
deleteNamedGraphBestEffort(regeneratedGraphUri);
```

Uses SPARQL UPDATE to remove all triples:
```sparql
WITH <namedGraphUri>
DELETE { ?s ?p ?o }
WHERE { ?s ?p ?o }
```

### NEW: Comprehensive Graph Comparison

The test now performs **deep graph analysis** comparing original vs regenerated:

#### 1. Triple Count Comparison
```
[GRAPH COMPARISON] INS - Original vs Regenerated
==========================================
Original graph:     http://example.org/DF-INS-PMSR-Simulators.xlsx
Regenerated graph:  http://example.org/DF-INS-PMSR-Simulators-regenerated.xlsx

Triple counts:
  Original:    609
  Regenerated: 609
  Difference:  0
```

#### 2. Entity Count Comparison by Type
```
[ENTITY COUNT COMPARISON BY TYPE]
  ✓ vstoi:InstrumentInstance: 71 -> 71
  ✓ vstoi:ComponentInstance: 16 -> 16
  ✓ vstoi:Deployment: 71 -> 71
  - vstoi:Platform: 1 -> 0
  + vstoi:CustomType: 0 -> 2
```

**Symbols**:
- `✓` Exact match
- `-` Entity count decreased (potential data loss)
- `+` Entity count increased (additional data or metadata)

#### 3. Missing Triples Detection

Identifies triples present in original but absent in regenerated:

```sparql
SELECT ?s ?p ?o
WHERE {
  GRAPH <originalGraph> { ?s ?p ?o }
  FILTER NOT EXISTS {
    GRAPH <regeneratedGraph> { ?s ?p ?o }
  }
}
LIMIT 100
```

**Example Output**:
```
[MISSING TRIPLES] (in original but NOT in regenerated)
  Found 3 missing triples (showing first 10):
    - <pmsr:PMSR_Simulation_Platform> <hasco:hasMaker> ""
    - <pmsr:INI1739301009974715> <hasco:hasWebDocument> ""
    - <pmsr:DPL1739301009974715> <vstoi:hasDeploymentTime> ""
```

#### 4. Extra Triples Detection

Identifies triples present in regenerated but absent in original:

```
[EXTRA TRIPLES] (in regenerated but NOT in original)
  Found 5 extra triples (showing first 10):
    + <pmsr:INI1739301009974715> <hasco:hasGenerationTimestamp> "2026-03-17T13:45:00"^^xsd:dateTime
    + <pmsr:DPL1739301009974715> <hasco:hasDataFileVersion> "2"
```

**Interpretation**:
- **Acceptable extras**: Generation timestamps, version numbers, auto-generated IDs
- **Problematic extras**: Duplicate entities, spurious relationships

#### 5. Property Value Equality

Checks if the same subject-property pairs have different values:

```sparql
SELECT ?property (COUNT(*) AS ?diffCount)
WHERE {
  {
    GRAPH <graph1> { ?s ?property ?v1 }
    GRAPH <graph2> { ?s ?property ?v2 }
    FILTER(?v1 != ?v2)
  }
}
GROUP BY ?property ORDER BY DESC(?diffCount)
```

**Example Output**:
```
[PROPERTY VALUE EQUALITY]
  DIFFERENT VALUES for vstoi:hasSerialNumber: 3 instances
  DIFFERENT VALUES for hasco:hasDataFile: 160 instances
```

**Interpretation**:
- `hasco:hasDataFile` differences are expected (different named graphs)
- Other property differences may indicate transformation errors

## Test Execution Strategy

### Parameterized Testing

The test uses JUnit 5's `@ParameterizedTest` to run the same logic for multiple MT types:

```java
@ParameterizedTest
@ValueSource(strings = {"INS", "DP2", "WKF"})
public void step3_allMTs_reset_and_deterministic_reingest(String mtName) {
    MTType type = MTType.valueOf(mtName);
    step3_reset_and_deterministic_reingest(type);
}
```

**Benefits**:
- Single test logic for all MT types
- Parallel execution capability
- Clear pass/fail per MT type

### Test Ordering

Tests execute in **lexicographic method name order** (via `@TestMethodOrder`):

1. `step1_allMTs_ingest(INS)` → `step1_allMTs_ingest(DP2)` → `step1_allMTs_ingest(WKF)`
2. `step2_allMTs_regenerate_and_compare(INS)` → ...
3. `step3_allMTs_reset_and_deterministic_reingest(INS)` → ...

This ensures **ingest completes before regeneration** is attempted.

### Cleanup

```java
@Test
public void zzz_cleanup_triplestore_ingestions() {
    // Runs LAST (alphabetically)
    // Deletes test data from triplestore
    // Keeps generated .xlsx and .ttl files for inspection
}
```

## Key Technical Decisions

### 1. Named Graph Scoping

Each test ingestion uses a **unique named graph** derived from the filename:
- **Isolation**: Tests don't interfere with each other
- **Reproducibility**: Can re-run tests without full triplestore reset
- **Debugging**: Can inspect graphs independently in Fuseki UI

### 2. Mock DataFile Pattern

Instead of using real database persistence:
- Uses Mockito to stub DataFile behavior
- Tracks state in `AtomicReference` holders
- Enables fast, repeatable testing without I/O overhead

### 3. Comparison Tolerance

The test **does not require exact triple equality**:
- Allows for acceptable differences (timestamps, auto-IDs)
- Focuses on **semantic equivalence** via entity/property counts
- Reports discrepancies for manual review

### 4. Fail-Fast vs. Comprehensive Reporting

Each phase uses `assertDoesNotThrow()` to:
- Continue execution to gather maximum diagnostic information
- Report all issues in a single test run
- Generate all artifacts (.ttl, .xlsx) even if validation fails

## Interpreting Test Results

### Success Indicators

✅ **All phases complete without exceptions**
✅ **Entity counts match between original and regenerated**
✅ **No missing triples (or only acceptable ones like empty strings)**
✅ **Extra triples are limited to metadata (timestamps, versions)**
✅ **Property value differences are explained (e.g., hasDataFile graph URIs)**

### Failure Patterns

#### Pattern 1: Missing Entities
```
[ENTITY COUNT COMPARISON BY TYPE]
  - vstoi:InstrumentInstance: 71 -> 65
```
**Cause**: Generator logic failed to query or populate all entities  
**Fix**: Check SPARQL query filters, verify status handling

#### Pattern 2: Dangling References
```
[RELATIONSHIP COMPLETENESS]
  DANGLING REFS via hasco:hasComponentOf: 10
```
**Cause**: Related entities not ingested or deleted prematurely  
**Fix**: Verify cascade ingestion, check foreign key semantics

#### Pattern 3: Property Value Corruption
```
[PROPERTY VALUE EQUALITY]
  DIFFERENT VALUES for vstoi:hasStatus: 71 instances
```
**Cause**: Transformation logic altered values during regeneration  
**Fix**: Check property mapping in XXGen classes, verify SPARQL result handling

## Future Enhancements

### Planned Features

1. **Exact Triple Equality Mode**: Flag to require 100% match for regression testing
2. **Graph Diffing Tool**: Visual HTML report showing side-by-side comparison
3. **Performance Benchmarks**: Track ingestion/generation time over commits
4. **Schema Validation**: Verify against SHACL/OWL constraints
5. **Concurrent Test Execution**: Parallel phase execution for faster CI

### Extension Points

New MT types can be added by:
1. Adding to `MTType` enum
2. Implementing `getMtExcel(type)` case
3. Adding `step1_ingest(type)` logic
4. Implementing `XXGen.genByStatus()` if not yet present
5. Adding type to `@ValueSource` arrays

## Conclusion

The roundtrip test provides **quantitative validation** of metadata template processing through:
- **Entity count tracking** ensures no data loss
- **Relationship completeness** verifies referential integrity
- **Property value comparison** detects transformation errors
- **Missing/extra triple detection** identifies regression

This comprehensive approach guarantees that the HASCO system can reliably:
1. Parse and understand metadata templates
2. Store them semantically in RDF
3. Regenerate equivalent templates on demand
4. Maintain data integrity across transformation cycles

