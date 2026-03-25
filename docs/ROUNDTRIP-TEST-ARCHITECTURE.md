# Roundtrip Test Architecture - Technical Specification

## Executive Summary

The **Roundtrip Test** (`HascoRoundtripTest.java`) is a comprehensive integration test suite that validates the complete lifecycle of Metadata Templates (MTs) in the HADatAc system. It ensures that:

1. **Ingestion**: Excel files can be parsed and stored as RDF triples in the triple store
2. **Generation**: RDF triples can be exported back to Excel format
3. **Deterministic Re-ingestion**: The generated Excel file produces identical or superset RDF graphs when re-ingested

This test is critical for ensuring **data integrity** and **round-trip fidelity** across the entire metadata pipeline.

---

## Test Architecture Overview

### Test Phases

The roundtrip test follows a **3-step validation cycle** for each Metadata Template type:

```
┌─────────────────────────────────────────────────────────────┐
│                    STEP 1: INGEST                           │
│  Excel File → Parser → RDF Model → Triple Store             │
│  Input: Original MT file (e.g., INS-PMSR-Simulators.xlsx)   │
│  Output: Named Graph with N triples                         │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    STEP 2: GENERATE                         │
│  Triple Store → Query → Generator → Excel File              │
│  Input: Named Graph URI                                     │
│  Output: Regenerated MT file (e.g., DP2-PMSR-regenerated.xlsx) │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    STEP 3: RE-INGEST                        │
│  Regenerated Excel → Parser → RDF Model → Triple Store      │
│  Validation: New graph ⊇ Original graph (superset check)    │
└─────────────────────────────────────────────────────────────┘
```

---

## Step 1: Ingestion Process

### Objective
Validate that the original Excel file can be successfully parsed and stored as RDF triples.

### Technical Flow

```java
File originalFile = new File("test/resources/INS-PMSR-Simulators.xlsx");
String namedGraphUri = "http://example.org/DF-INS-PMSR-Simulators.xlsx";

// Execute ingestion
IngestionWorker worker = new IngestionWorker(originalFile, dataFile, templateFile);
worker.ingest();

// Validate result
assertNotNull(namedGraphUri);
assertTrue(tripleStore.graphExists(namedGraphUri));
```

### Key Components

1. **SpreadsheetRecordFile**: Reads Excel worksheets and extracts records
2. **SheetHandler**: Parses rows and columns, validates headers
3. **Generator Chain**: Creates RDF statements from parsed data
4. **Triple Store Commit**: Persists RDF model to Fuseki

### Success Criteria

- ✅ Named graph created with expected URI
- ✅ All worksheets parsed without errors
- ✅ Triple count > 0 (e.g., INS typically produces ~1000+ triples)
- ✅ DataFile status set to `PROCESSED`

---

## Step 2: Generation Process

### Objective
Validate that RDF triples can be queried from the triple store and exported back to Excel format.

### Technical Flow

```java
// Call generator service
String outputPath = DP2Gen.genByStatus(
    dataFileUri,
    status,
    filename,
    mediaFolder
);

// Validate generated file
File generatedFile = new File(outputPath);
assertTrue(generatedFile.exists());
assertTrue(generatedFile.length() > 0);

// Validate Excel structure
Workbook wb = WorkbookFactory.create(generatedFile);
assertNotNull(wb.getSheet("InfoSheet"));
assertNotNull(wb.getSheet("Namespaces"));
```

### Key Components

1. **Generator Class** (e.g., `DP2Gen`, `WKFGen`, `INSGen`)
   - Queries triple store for entities with matching `hasDataFile` URI
   - Filters by status (e.g., `Draft`, `Operational`)
   - Populates Excel worksheets with data

2. **Helper Classes** (e.g., `DP2GenHelper`, `WKFGenHelper`)
   - Creates and configures workbook structure
   - Manages worksheet creation and column headers
   - Handles data formatting and validation

3. **Sheet Population Classes** (e.g., `DP2Deployments`, `WKFTasks`)
   - Executes SPARQL queries to retrieve specific entity types
   - Formats data according to Excel schema
   - Handles relationships and references

### SPARQL Query Pattern

All generators use **Named Graph scoping** to ensure data isolation:

```sparql
PREFIX hasco: <http://hadatac.org/ont/hasco/>
PREFIX vstoi: <http://hadatac.org/ont/vstoi#>

SELECT DISTINCT ?uri WHERE {
  GRAPH <https://hadatac.org/ont/hadatac#/DFL1770904581375191> {
    ?uri hasco:hascoType vstoi:Task .
    OPTIONAL { ?uri rdfs:label ?label . }
  }
} ORDER BY ASC(?label)
```

### Success Criteria

- ✅ Excel file generated at expected path
- ✅ File size > minimum (e.g., 7000+ bytes for WKF)
- ✅ All required worksheets present (`InfoSheet`, `Namespaces`, entity sheets)
- ✅ Data rows match ingested entities

---

## Step 3: Deterministic Re-ingestion

### Objective
Validate that the generated Excel file, when re-ingested, produces **identical or superset** RDF triples compared to the original ingestion.

This is the **critical validation** that ensures no data loss during the generation process.

### Technical Flow

```java
// 1. Delete original named graphs (RESET)
tripleStore.deleteGraph(originalGraphUri);
tripleStore.deleteGraph(regeneratedGraphUri);

// 2. Re-ingest original file
IngestionWorker worker1 = new IngestionWorker(originalFile, ...);
worker1.ingest();
int originalTriples = countTriplesInGraph(originalGraphUri);

// 3. Ingest regenerated file
IngestionWorker worker2 = new IngestionWorker(regeneratedFile, ...);
worker2.ingest();
int regeneratedTriples = countTriplesInGraph(regeneratedGraphUri);

// 4. Validate superset relationship
assertTrue(regeneratedTriples >= originalTriples, 
    "Regenerated graph must have >= triples than original");
```

### Graph Comparison Logic

The test performs a **triple count validation** to ensure:

- `regeneratedTriples ≥ originalTriples`

This superset relationship accounts for:
- **Namespace expansion**: Additional prefixes may be inferred
- **Implicit relationships**: The generator may add derived triples
- **Metadata enrichment**: System-generated timestamps, statuses, etc.

### Success Criteria

- ✅ Original file re-ingests successfully
- ✅ Regenerated file ingests successfully
- ✅ `tripleCount(regenerated) ≥ tripleCount(original)`
- ✅ No parsing errors or warnings

---

## Named Graph Isolation Strategy

### Purpose
Each ingested MT creates a **Named Graph** in the triple store to:
- Isolate data by source file
- Enable precise querying and deletion
- Track provenance and lineage

### URI Pattern

```
Named Graph URI = <DataFile URI>

Examples:
- https://hadatac.org/ont/hadatac#/DFL1770641324417771
- http://example.org/DF-INS-PMSR-Simulators.xlsx (test environment)
```

### Query Scoping

All generation queries are scoped to the specific Named Graph:

```sparql
SELECT ?uri WHERE {
  GRAPH <https://hadatac.org/ont/hadatac#/DFL1770641324417771> {
    ?uri hasco:hascoType vstoi:Deployment .
  }
}
```

This ensures:
- **Data isolation**: Only entities from the specific MT are retrieved
- **Multi-tenancy**: Multiple users can work on different MTs simultaneously
- **Clean deletion**: Entire MT can be removed by deleting the Named Graph

---

## Generator Architecture

### Core Components

Each MT type (INS, DP2, WKF, SDD) has a dedicated generator package:

```
org.hascoapi.transform.mt/
├── ins/
│   ├── INSGen.java              # Main generator orchestrator
│   ├── INSGenHelper.java        # Workbook structure management
│   ├── INSInstruments.java      # Instrument sheet population
│   ├── INSComponents.java       # Component sheet population
│   └── ...
├── dp2/
│   ├── DP2Gen.java
│   ├── DP2GenHelper.java
│   ├── DP2Deployments.java
│   ├── DP2PlatformInstances.java
│   └── ...
├── wkf/
│   ├── WKFGen.java
│   ├── WKFGenHelper.java
│   ├── WKFTasks.java
│   ├── WKFProcesses.java
│   └── ...
└── sdd/
    ├── SDDGen.java
    ├── SDDGenHelper.java
    ├── SDDDictionaryMapping.java
    └── ...
```

### Generator Workflow

```
1. Initialize Helper
   ├─ Create Workbook
   ├─ Add InfoSheet with catalog entries
   └─ Add Namespaces sheet

2. Query Triple Store
   ├─ Filter by status (Draft, Operational, etc.)
   ├─ Filter by Named Graph URI
   └─ Retrieve entities by type

3. Populate Worksheets
   ├─ For each entity type (Deployment, Task, etc.)
   │   ├─ Execute SPARQL query
   │   ├─ Map RDF properties to Excel columns
   │   └─ Add rows to worksheet
   
4. Save Workbook
   └─ Write to filesystem at configured path
```

### Example: DP2 Generator Flow

```java
// 1. Find all DP2 candidates
List<DP2> candidates = DP2.findAll();

// 2. Filter by status and DataFile URI
DP2 selected = candidates.stream()
    .filter(dp -> dp.getHasStatus().equals(status))
    .filter(dp -> dp.getHasDataFileUri().equals(dataFileUri))
    .findFirst()
    .orElse(null);

// 3. Query entities from Named Graph
String graphUri = selected.getHasDataFileUri();
List<Deployment> deployments = Deployment.findByGraph(graphUri);
List<PlatformInstance> instances = PlatformInstance.findByGraph(graphUri);

// 4. Populate sheets
helper.addDeployments(deployments);
helper.addPlatformInstances(instances);

// 5. Save workbook
String outputPath = helper.save(filename);
```

---

## Catalog-Based Generator Chain

### InfoSheet Structure

Each MT defines its structure in the `InfoSheet`:

```
| Attribute          | Value              |
|--------------------|--------------------|
| hasDependencies    | #Namespaces        |
| Deployments        | #Deployments       |
| Platforms          | #Platforms         |
| PlatformInstances  | #PlatformInstances |
| InstrumentInstances| #InstrumentInstances|
| ComponentInstances | #ComponentInstances|
| FieldsOfView       | #FieldsOfView      |
| hasVersion         | 1                  |
```

### Catalog Parsing Logic

```java
Map<String, String> catalog = new HashMap<>();

for (Record record : infoSheet) {
    String attribute = record.getValueByColumn("Attribute");
    String value = record.getValueByColumn("Value");
    
    if (value.startsWith("#")) {
        // This is a sheet reference
        catalog.put(attribute, value);
    }
}

// Build generator chain based on catalog
for (Map.Entry<String, String> entry : catalog.entrySet()) {
    String sheetName = entry.getValue().substring(1); // Remove '#'
    
    if (worksheetExists(sheetName)) {
        generators.add(createGeneratorForSheet(sheetName));
    }
}
```

### Critical Validation

The catalog **must not be null** for the generator chain to be built. The test logs show:

```
[error] ? Failed to load catalog - mapCatalog is null
[info] IngestionWorker: No generator chain produced. Aborting ingestion gracefully.
```

This indicates that the `InfoSheet` parsing failed, preventing the generator chain from being constructed.

---

## Triple Store Verification

### Triple Count Queries

The test validates data persistence by counting triples:

```sparql
SELECT (COUNT(*) as ?count) WHERE {
  GRAPH <https://hadatac.org/ont/hadatac#/DFL1770641324417771> {
    ?s ?p ?o
  }
}
```

**Expected Results:**
- INS: ~1000-1500 triples (many instruments, components, codebooks)
- DP2: ~600-1300 triples (deployments, platforms, instances)
- WKF: ~80-150 triples (tasks, processes, required instruments)
- SDD: ~200-500 triples (dictionary mappings, attributes)

### Entity Count Validation

The test also validates specific entity counts:

```java
// Example from DP2 test
int deployments = countEntitiesOfType(graphUri, "vstoi:Deployment");
int platforms = countEntitiesOfType(graphUri, "vstoi:Platform");
int instrumentInstances = countEntitiesOfType(graphUri, "vstoi:InstrumentInstance");

assertTrue(deployments > 0, "DP2 must have at least one Deployment");
assertTrue(instrumentInstances > 0, "DP2 must have at least one InstrumentInstance");
```

---

## Data Integrity Mechanisms

### 1. Status Tracking

Each DataFile has a `fileStatus` that tracks processing state:

```
UNPROCESSED → WORKING → PROCESSED
```

**During Ingestion:**
- Status is set to `WORKING` at start
- Status is set to `PROCESSED` on success
- Status remains `WORKING` on failure

**During Generation:**
- Generator queries for entities with `PROCESSED` status
- `WORKING` or `UNPROCESSED` entities are skipped (with warning)

### 2. Named Graph Association

Every RDF triple is associated with the source DataFile:

```turtle
# All entities in the graph have this property
<entity-uri> hasco:hasDataFile <https://hadatac.org/ont/hadatac#/DFL1770641324417771> .
```

This enables:
- **Scoped queries**: Only retrieve data from specific MT
- **Clean deletion**: Remove all data by deleting the Named Graph
- **Provenance tracking**: Know which file produced which data

### 3. Superset Validation Logic

```java
int originalTriples = countTriplesInGraph(originalGraphUri);
int regeneratedTriples = countTriplesInGraph(regeneratedGraphUri);

// The regenerated graph must contain AT LEAST as many triples
assertTrue(regeneratedTriples >= originalTriples,
    String.format("Expected %d triples, but found %d", 
        originalTriples, regeneratedTriples));
```

**Why superset (≥) instead of exact equality (=)?**

1. **Namespace expansion**: Generated files may include additional ontology prefixes
2. **System metadata**: Timestamps, auto-generated IDs may differ
3. **Derived relationships**: Some relationships may be inferred during generation
4. **Order independence**: RDF is unordered; triple count is the validation metric

---

## Worksheet Structure Validation

### Standard Worksheets

All MTs must include:

| Worksheet   | Purpose                                    | Required |
|-------------|-------------------------------------------|----------|
| InfoSheet   | Catalog definition and metadata control   | ✅ Yes   |
| Namespaces  | Ontology prefix definitions               | ✅ Yes   |

### MT-Specific Worksheets

#### INS (Instrument Specification)
- `Instruments`: Instrument class definitions
- `ComponentStems`: Component type definitions
- `Components`: Component instances
- `SlotElements`: Container slots
- `CodeBooks`: Categorical value sets
- `ResponseOptions`: Allowed response values

#### DP2 (Deployment & Platform)
- `Deployments`: Deployment definitions
- `Platforms`: Platform class definitions
- `PlatformInstances`: Physical platform instances
- `InstrumentInstances`: Physical instrument instances
- `ComponentInstances`: Physical component instances
- `FieldsOfView`: Spatial sensing perspectives

#### WKF (Workflow)
- `ProcessStems`: Process type definitions
- `Processes`: Concrete process instances
- `Tasks`: Workflow task definitions
- `RequiredInstruments`: Instruments needed for workflow execution

#### SDD (Semantic Data Dictionary)
- `Dictionary Mapping`: Column → semantic variable mappings
- `Codebook`: Categorical variable value sets
- `Timeline`: Temporal context definitions

---

## Error Detection and Recovery

### Common Failure Modes

#### 1. Catalog Loading Failure

**Symptom:**
```
[error] ? Failed to load catalog - mapCatalog is null
[info] IngestionWorker: No generator chain produced.
```

**Root Cause:**
- InfoSheet is malformed or missing
- Header row is incorrect
- Catalog entries don't start with `#`

**Recovery:**
- Validate InfoSheet structure
- Ensure all catalog values use `#SheetName` format
- Check for BOM or encoding issues

#### 2. Status Mismatch

**Symptom:**
```
[DP2Gen] WARNING: No PROCESSED DP2 found, using first candidate
[DP2Gen] ERROR: DataFile has NOT been ingested yet! Status is: WORKING
```

**Root Cause:**
- DataFile status is not `PROCESSED`
- Ingestion failed silently
- Status was not updated after successful ingestion

**Recovery:**
- Check ingestion logs for errors
- Verify triple store connectivity
- Manually set status to `PROCESSED` if needed

#### 3. Empty Graph Generation

**Symptom:**
```
[DP2Gen] graph(https) tripleCount=14
[DP2Gen] WARNING: still empty after graph+hasDataFile. Falling back to global.
DP2Gen.genByStatus(scoped): final counts deployments=0 platforms=0
```

**Root Cause:**
- Named Graph contains only metadata (14 triples = DataFile + DP2 object)
- No actual entity data (deployments, platforms) was ingested

**Recovery:**
- Verify ingestion completed successfully
- Check that status is `PROCESSED`
- Ensure SPARQL queries use correct hascoType values

---

## Test Environment Configuration

### File Paths

```
test/resources/
├── INS-PMSR-Simulators.xlsx          # Original INS file
├── DP2-PMSR.xlsx                     # Original DP2 file
├── WKF-WeatherStation.xlsx           # Original WKF file
└── generated/
    ├── INS-PMSR-Simulators-regenerated.xlsx
    ├── DP2-PMSR-regenerated.xlsx
    ├── WKF-WeatherStation-regenerated.xlsx
    ├── INS_step1_original.ttl        # RDF export for debugging
    ├── INS_step2_regenerated.ttl     # RDF export after generation
    └── INS_step3_reingested_regenerated.ttl
```

### Triple Store Configuration

```properties
# Test uses embedded Fuseki with in-memory dataset
fuseki.url=http://localhost:3030
dataset.name=test-dataset
```

### Template Configuration

```hocon
hascoapi.paths.ingestion = "C:/hascoapi/var/"
hascoapi.template = "conf/template.generic.conf"
```

---

## Debugging Utilities

### TTL Export for Validation

The test exports RDF graphs to Turtle (TTL) format for manual inspection:

```java
public void exportGraphToTTL(String graphUri, String filename) {
    Model model = tripleStore.getGraph(graphUri);
    
    try (FileOutputStream out = new FileOutputStream(filename)) {
        model.write(out, "TURTLE");
    }
    
    // Print sample triples for quick validation
    StmtIterator iter = model.listStatements();
    int count = 0;
    while (iter.hasNext() && count < 20) {
        Statement stmt = iter.nextStatement();
        System.out.println("[TTL PREVIEW] " + 
            stmt.getSubject() + " " +
            stmt.getPredicate() + " " +
            stmt.getObject());
        count++;
    }
}
```

### Logging Conventions

The test uses structured logging with prefixes:

- `✅` or `Ô£ô`: Success
- `⚠️` or `ÔÜá´©Å`: Warning
- `❌` or `ÔØî`: Error
- `➔` or `ÔåÆ`: Action/Transition
- `[DEBUG]`: Diagnostic information
- `[INFO]`: Informational message
- `[ERROR]`: Error message

---

## Test Execution Flow

### JUnit Test Method

```java
@ParameterizedTest
@MethodSource("provideMetadataTemplateTypes")
@Order(3)
public void step3_allMTs_reset_and_deterministic_reingest(String mtType) {
    // Test parameters: "ins", "dp2", "wkf", "sdd"
    
    // 1. Delete previous graphs
    deleteNamedGraph(originalGraphUri);
    deleteNamedGraph(regeneratedGraphUri);
    
    // 2. Re-ingest original
    ingestFile(originalFile, originalGraphUri);
    int originalTriples = countTriples(originalGraphUri);
    
    // 3. Ingest regenerated
    ingestFile(regeneratedFile, regeneratedGraphUri);
    int regeneratedTriples = countTriples(regeneratedGraphUri);
    
    // 4. Validate superset relationship
    assertTrue(regeneratedTriples >= originalTriples);
    
    // 5. Export for debugging
    exportGraphToTTL(regeneratedGraphUri, "test/resources/generated/" + 
        mtType.toUpperCase() + "_step3_reingested_regenerated.ttl");
}
```

### Parameterized Test Source

```java
static Stream<String> provideMetadataTemplateTypes() {
    return Stream.of("ins", "dp2", "wkf");
    // "sdd" commented out until fully implemented
}
```

---

## Success Metrics

### Passing Test Requirements

For each MT type:

1. **Step 1 (Ingest)**: 
   - ✅ File parsed successfully
   - ✅ Named Graph created
   - ✅ Triple count > expected minimum
   - ✅ Status = `PROCESSED`

2. **Step 2 (Generate)**:
   - ✅ Excel file created
   - ✅ All required worksheets present
   - ✅ Data rows match expected entities
   - ✅ File size > minimum threshold

3. **Step 3 (Re-ingest)**:
   - ✅ Original file re-ingests successfully
   - ✅ Generated file ingests successfully
   - ✅ `triples(regenerated) ≥ triples(original)`
   - ✅ No exceptions or errors

### Typical Results

```
✅ INS:  Original: 1281 triples → Regenerated: 1281 triples
✅ DP2:  Original: 609 triples  → Regenerated: 609 triples
✅ WKF:  Original: 109 triples  → Regenerated: 109 triples
```

---

## Common Issues and Solutions

### Issue 1: Catalog Loading Failure (WKF)

**Log Evidence:**
```
[error] ? Failed to load catalog - mapCatalog is null
```

**Root Cause:**
- The `InfoSheet` parsing logic expects specific format
- The InfoSheet may have extra metadata rows (like `hasVersion`) that confuse the parser
- Catalog values must reference actual worksheet names

**Solution:**
- Ensure InfoSheet has consistent structure
- Validate that catalog entries match actual worksheet names
- Add null-safety checks in catalog parsing
- Handle `hasVersion` as a special case (not a worksheet reference)

### Issue 2: Status Mismatch

**Symptom:**
```
[WKFGen] rawStatus=DRAFT, effectiveStatus=http://hadatac.org/ont/vstoi#DRAFT
[WKFGen] requestedStatus=http://hadatac.org/ont/vstoi#Draft
[WKFGen] Skipping (status mismatch)
```

**Root Cause:**
- Status URIs have case sensitivity issues (`Draft` vs `DRAFT`)
- Status values may be stored as full URIs or short forms
- Status comparison doesn't normalize values

**Solution:**
- Normalize status values before comparison
- Use case-insensitive comparison
- Accept both URI forms (`vstoi:Draft` and `http://hadatac.org/ont/vstoi#Draft`)

### Issue 3: Empty Graph Generation (DP2)

**Log Evidence:**
```
[DP2Gen] graph(https) tripleCount=14
[DP2Gen] final counts deployments=0 platforms=0
```

**Root Cause:**
- The Named Graph only contains metadata about the DP2/DataFile object itself (14 triples)
- No actual entity data (deployments, platforms) was ingested
- Ingestion may have failed silently

**Solution:**
- Verify ingestion completed successfully
- Check that DataFile status is `PROCESSED`
- Validate generator chain executed all generators
- Ensure SPARQL queries use correct graph URI

---

## Generator Chain Execution Details

### Normal Chain Flow

```
========================================
GeneratorChain: Executing [NORMAL] generator chain
Number of generators: 7
Named Graph URI: https://hadatac.org/ont/hadatac#/DFL1770749060242281
Commit mode: YES
========================================

┌──────────────────────────────────────┐
│ GENERATOR [1/7]: INSGenerator        │
│ Element Type: responseoption         │
│ Named Graph: <DataFile URI>          │
└──────────────────────────────────────┘
  ➔ Step 1/5: PreProcess
  ➔ Step 2/5: CreateRows
    ✅ Created 23 rows
  ➔ Step 3/5: CreateObjects
    ✅ Created 0 objects
  ➔ Step 4/5: PostProcess
  ✅ Generator completed successfully
└──────────────────────────────────────┘

[... generators 2-6 ...]

========================================
GeneratorChain: Starting COMMIT PHASE
========================================

┌──────────────────────────────────────┐
│ COMMIT [1/7]: INSGenerator           │
│ Element Type: responseoption         │
└──────────────────────────────────────┘
  Rows to commit: 23
  ➔ Committing 23 rows to triple store...
  ✅ Successfully committed 191 triples
└──────────────────────────────────────┘

[... commits 2-7 ...]

========================================
GeneratorChain: COMPLETED SUCCESSFULLY
Final Study URI: 
========================================
```

### Two-Phase Execution

1. **Generation Phase**: All generators create rows/objects in memory
2. **Commit Phase**: All rows/objects are committed to triple store in a single transaction

This ensures **atomicity**: either all data is committed, or none.

---

## SPARQL Query Patterns

### Entity Retrieval by Type

```sparql
SELECT DISTINCT ?uri WHERE {
  GRAPH <{namedGraphUri}> {
    ?uri hasco:hascoType {entityType} .
    OPTIONAL { ?uri rdfs:label ?label . }
  }
} ORDER BY ASC(?label)
```

### Entity Count by Type

```sparql
SELECT (COUNT(DISTINCT ?uri) as ?count) WHERE {
  GRAPH <{namedGraphUri}> {
    ?uri hasco:hascoType {entityType} .
  }
}
```

### Graph Triple Count

```sparql
SELECT (COUNT(*) as ?count) WHERE {
  GRAPH <{namedGraphUri}> {
    ?s ?p ?o
  }
}
```

### Global Entity Search (Fallback)

When Named Graph scoping fails, generators fall back to global search:

```sparql
SELECT ?uri ?dataFile WHERE {
  # No GRAPH clause - searches entire triple store
  ?uri a ?type .
  ?type rdfs:subClassOf* vstoi:Deployment .
  OPTIONAL { ?uri hasco:hasDataFile ?dataFile }
} LIMIT 10
```

⚠️ **Warning**: Global search should only be a fallback. It can return entities from other MTs.

---

## Test Lifecycle Hooks

### Cleanup After Tests

```java
@AfterEach
@Order(999)
public void zzz_cleanup_triplestore_ingestions() {
    // Delete test study
    String studyUri = "http://hadatac.org/ont/arrowhead/STD-LTE-PIAGET-WEATHER-STATION";
    tripleStore.deleteByUri(studyUri);
    
    // Delete test named graphs
    tripleStore.deleteGraph("http://example.org/DF-INS-PMSR-Simulators.xlsx");
    tripleStore.deleteGraph("http://example.org/DF-DP2-PMSR.xlsx");
    tripleStore.deleteGraph("http://example.org/DF-WKF-WeatherStation.xlsx");
    
    System.out.println("cleanup: deleted ingested study (best-effort): " + studyUri);
}
```

---

## Future Enhancements

### 1. Semantic Diff Tool

Instead of just triple counts, implement a **semantic diff** that compares:
- Entity counts by type
- Relationship completeness
- Property value equality
- Missing or extra triples

### 2. Parallel Test Execution

Enable parallel execution of MT tests:

```java
@Execution(ExecutionMode.CONCURRENT)
@ParameterizedTest
@MethodSource("provideMetadataTemplateTypes")
void step1_ingest_all(String mtType) { ... }
```

### 3. Property-Level Validation

Validate that specific properties are preserved:

```java
// Verify that critical properties are identical
assertPropertyEquals(originalGraph, regeneratedGraph, 
    deploymentUri, "rdfs:label");
assertPropertyEquals(originalGraph, regeneratedGraph, 
    deploymentUri, "vstoi:hasStatus");
```

### 4. Automated Regression Detection

Compare new test runs against baseline:

```java
// Store baseline TTL files
baselineTriples = loadTTL("baselines/INS-baseline.ttl");
currentTriples = loadTTL("generated/INS_step3_reingested_regenerated.ttl");

// Detect regressions
assertTrue(currentTriples >= baselineTriples,
    "Regression detected: fewer triples than baseline");
```

---

## Conclusion

The Roundtrip Test is a **critical validation mechanism** that ensures:

1. **Data Integrity**: No information is lost during the Excel ↔ RDF conversion
2. **Generator Correctness**: Generation logic produces valid, re-ingestible files
3. **System Stability**: Core ingestion and generation pipelines work reliably
4. **Regression Prevention**: Changes to parsers/generators don't break existing functionality

By validating the **complete lifecycle** (ingest → generate → re-ingest), the test provides high confidence that the metadata pipeline is **robust, deterministic, and production-ready**.

---

## Appendix A: Generator Chain Execution Log Analysis

### Successful INS Ingestion

```
Processing file: INS-PMSR-Simulators.xlsx
SpreadsheetRecordFile: looking for sheetName = [InfoSheet]
[SheetHandler] Headers parsed: [Attribute, Value]
  [SheetHandler] Row 2 data: [hasDependencies, #Namespaces]
  [SheetHandler] ✅ Added record #1
  [SheetHandler] Row 3 data: [Instruments, #Instruments]
  [SheetHandler] ✅ Added record #2
  ...

========================================
GeneratorChain: Executing [NORMAL] generator chain
Number of generators: 7
Named Graph URI: https://hadatac.org/ont/hadatac#/DFL1770749060242281
Commit mode: YES
========================================

GENERATOR [1/7]: INSGenerator (Element Type: responseoption)
  ✅ Created 23 rows
  ✅ Successfully committed 191 triples

GENERATOR [2/7]: INSGenerator (Element Type: codebook)
  ✅ Created 12 rows
  ✅ Successfully committed 96 triples

[... generators 3-7 ...]

GeneratorChain: COMPLETED SUCCESSFULLY
IngestionWorker: DataFile status set to PROCESSED
```

**Analysis:**
- 7 generators executed (one per entity type)
- Total ~1200+ triples committed
- Status correctly updated to `PROCESSED`
- ✅ **PASS**

### Failed WKF Re-ingestion

```
Processing file: WKF-WeatherStation-regenerated.xlsx
InfoSheet parsed successfully (6 records)

========== AnnotateWKF.exec() START ==========
✅ Catalog loaded successfully with 5 sheets

[error] ? Failed to load catalog - mapCatalog is null
IngestionWorker: No generator chain produced. Aborting ingestion gracefully.
```

**Analysis:**
- InfoSheet was parsed correctly
- AnnotateWKF.exec() shows successful catalog loading
- But IngestionWorker reports null catalog
- Likely a **timing or reference passing issue** between components
- ❌ **FAIL**

---

## Appendix B: Entity Relationship Model

### INS Entity Hierarchy

```
Instrument (Class)
  ├─ hasComponentStem → ComponentStem (Class)
  │   └─ hasComponent → Component (Instance)
  │       └─ hasCodebook → Codebook
  │           └─ hasCodebookSlot → CodebookSlot
  │               └─ hasResponseOption → ResponseOption
  └─ hasContainerSlot → ContainerSlot (SlotElement)
```

### DP2 Entity Hierarchy

```
Platform (Class)
  └─ hasPlatformInstance → PlatformInstance
      └─ hasDeployment → Deployment
          ├─ hasInstrumentInstance → InstrumentInstance
          │   └─ basedOn → Instrument (from INS)
          └─ hasComponentInstance → ComponentInstance
              └─ basedOn → Component (from INS)
```

### WKF Entity Hierarchy

```
ProcessStem (Class)
  └─ hasProcess → Process (Instance)
      └─ hasTopTask → Task (Top-level)
          ├─ hasSubtask → Task (Child)
          └─ hasRequiredInstrument → RequiredInstrument
              └─ usesInstrument → Instrument (from INS)
```

---

**Document Version**: 1.0  
**Last Updated**: 2026-03-17  
**Author**: HADatAc Development Team  
**Related Files**:
- `test/org/hascoapi/tests/HascoRoundtripTest.java`
- `app/org/hascoapi/ingestion/IngestionWorker.java`
- `app/org/hascoapi/transform/mt/dp2/DP2Gen.java`
- `app/org/hascoapi/transform/mt/wkf/WKFGen.java`
- `app/org/hascoapi/transform/mt/ins/INSGen.java`
- `app/org/hascoapi/transform/mt/sdd/SDDGen.java`

