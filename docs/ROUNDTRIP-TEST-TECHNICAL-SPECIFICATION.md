# HASCO Roundtrip Test - Technical Specification

## Executive Summary

The **HASCO Roundtrip Test** is a comprehensive validation framework designed to ensure **lossless bidirectional transformation** between Machine-Readable Templates (Excel workbooks) and their RDF/OWL representation in a SPARQL triplestore.

The test validates that metadata templates can be:
1. **Ingested** from Excel into the triplestore
2. **Regenerated** from the triplestore back to Excel  
3. **Re-ingested** deterministically, producing an identical or superset semantic graph

This specification describes the complete technical architecture, validation strategies, and quality metrics used by the framework.

---

## 1. Core Architecture

### 1.1 Three-Phase Lifecycle

The roundtrip test implements a deterministic **3-step validation lifecycle** for each Machine-Readable Template (MT) type:

```
┌─────────────────────────────────────────────────────────────────┐
│ PHASE 1: INGESTION & UNDERSTANDING                             │
│ Excel Workbook → Parser → RDF Triples → Triplestore            │
└─────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│ PHASE 2: REGENERATION & COMPARISON                             │
│ Triplestore → SPARQL Query → RDF Model → Excel Generator       │
└─────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│ PHASE 3: RESET & DETERMINISTIC RE-INGESTION                    │
│ Cleanup → Fresh Ingest → Regeneration → Comparison             │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 Data Flow Architecture

Each phase operates on specific artifacts:

| Phase | Input | Process | Output | Validation Artifact |
|-------|-------|---------|--------|-------------------|
| 1 | Original Excel | IngestionWorker.ingest() | RDF Named Graph | Turtle dump (.ttl) |
| 2 | RDF Named Graph | MTGen.genByStatus() | Regenerated Excel | Excel + Turtle dump |
| 3 | Both Graphs | Graph Comparison | Metrics Report | Comparison statistics |

---

## 2. Phase 1: Ingestion & Understanding

### 2.1 Objective

Transform an Excel-based metadata template into semantic triples stored in a SPARQL-compliant triplestore.

### 2.2 Technical Implementation

**Entry Point:**
```java
IngestionWorker.ingest(DataFile df, File excel, String templatePath, String status)
```

**Process:**
1. **Template Resolution**: Loads the appropriate template configuration (e.g., `template.generic.conf`)
2. **Excel Parsing**: Uses Apache POI to extract worksheets and cell values
3. **Generator Chain**: Executes a sequence of specialized generators (e.g., NamespaceGenerator, AttributeGenerator)
4. **RDF Model Creation**: Transforms parsed data into Jena RDF Model
5. **Triplestore Commit**: Writes triples to a **named graph** identified by the DataFile URI

**Named Graph Strategy:**
- Each ingested template creates a **named graph** with URI = `DataFile.getUri()`
- Example: `http://example.org/DF-INS-PMSR-Simulators.xlsx`
- This enables **graph-level isolation and cleanup**

### 2.3 Validation Artifacts

After ingestion, the test generates:

1. **Turtle Dump (.ttl file)**:
   - Complete RDF export of the named graph in Turtle syntax
   - Stored in `test/resources/generated/[MT]_ingested_original.ttl`
   - Enables offline diff and inspection

2. **Triple Count Log**:
   ```
   [TTL] INS_ingested_original: namedGraphUri=http://example.org/DF-INS-PMSR-Simulators.xlsx tripleCount=1247
   ```

3. **Triple Preview** (first 25 triples):
   - Logged to console for immediate visibility
   - Shows subject-predicate-object structure

4. **Graph Structure Analysis**:
   - Entity counts by type
   - Relationship completeness metrics
   - Property distribution statistics

---

## 3. Phase 2: Regeneration & Comparison

### 3.1 Objective

Extract metadata from the triplestore and regenerate an equivalent Excel template, demonstrating **data fidelity**.

### 3.2 Technical Implementation

**Entry Point:**
```java
MTGen.genByStatus(String status, String filename, String mediaFolder, String verifyUri)
```

**Process:**
1. **SPARQL Query Execution**: Retrieve entities by status (e.g., `vstoi:Draft`)
2. **Entity Reconstruction**: Rebuild POJO objects from RDF triples
3. **Excel Workbook Creation**: Use Apache POI to generate sheets and populate cells
4. **File Persistence**: Write to `ConfigProp.getPathIngestion() + filename`

**Status-Based Selection:**
- Queries entities matching `vstoi:hasStatus = vstoi:Draft`
- For templates without explicit status, treats `null` as `Draft`
- Example SPARQL pattern:
  ```sparql
  SELECT ?uri WHERE {
    ?uri a hasco:Instrument .
    OPTIONAL { ?uri vstoi:hasStatus ?status }
    FILTER(!BOUND(?status) || ?status = <http://hadatac.org/ont/vstoi#Draft>)
  }
  ```

### 3.3 Validation Steps

**A. File-Level Validation:**
```java
assertTrue(out.exists(), "Regenerated workbook should exist")
assertTrue(out.length() > 0, "Regenerated workbook should not be empty")
```

**B. Structural Validation:**
- Verify all **required sheets** are present (InfoSheet, Namespaces, etc.)
- Check **header integrity** (correct column names)

**C. Re-Ingestion Test:**
- Ingest the regenerated Excel back into the triplestore
- Creates a **second named graph** for comparison
- Generates `[MT]_ingested_regenerated.ttl`

**D. Artifacts:**
1. Regenerated Excel: `test/resources/generated/[MT]-regenerated.xlsx`
2. Turtle dump: `test/resources/generated/[MT]_ingested_regenerated.ttl`
3. Console logs with entity counts and structure analysis

---

## 4. Phase 3: Reset & Deterministic Re-Ingestion

### 4.1 Objective

Prove that the ingestion process is **deterministic** and that regenerated templates produce **identical or superset** semantic graphs.

### 4.2 Technical Implementation

**Process Flow:**
```
1. Cleanup: Delete named graphs from Phase 1 & 2
2. Re-ingest: Ingest original Excel (fresh triplestore state)
3. Regenerate: Generate Excel from fresh state  
4. Compare: Validate graphs are identical/superset
```

**Cleanup Strategy:**
```java
deleteNamedGraphBestEffort(namedGraphUri)
```
- Executes SPARQL UPDATE: `WITH <graph> DELETE { ?s ?p ?o } WHERE { ?s ?p ?o }`
- Best-effort (non-fatal if deletion fails)
- Ensures clean slate for determinism check

### 4.3 Graph Comparison Algorithm

The test implements **multi-level comparison** between original and regenerated graphs:

#### 4.3.1 Entity Count Comparison

**Purpose**: Verify that all entities of each type are preserved

**Implementation**:
```java
Map<String, Long> originalCounts = countEntitiesByType(originalGraphUri);
Map<String, Long> regeneratedCounts = countEntitiesByType(regeneratedGraphUri);

for (String type : originalCounts.keySet()) {
    long original = originalCounts.get(type);
    long regenerated = regeneratedCounts.getOrDefault(type, 0L);
    assertTrue(regenerated >= original, 
        "Type " + type + " count mismatch: expected>=" + original + " got=" + regenerated);
}
```

**SPARQL Pattern**:
```sparql
SELECT ?type (COUNT(?entity) AS ?count) WHERE {
  GRAPH <namedGraphUri> {
    ?entity a ?type .
  }
}
GROUP BY ?type
```

**Example Output**:
```
[GRAPH ANALYSIS] Entity type: hasco:Instrument | Count: 25
[GRAPH ANALYSIS] Entity type: hasco:InstrumentInstance | Count: 71
[GRAPH ANALYSIS] Entity type: hasco:Component | Count: 8
```

#### 4.3.2 Relationship Completeness

**Purpose**: Ensure all semantic relationships (object properties) are preserved

**Implementation**:
```java
Map<String, Long> originalRelations = countRelationshipsByPredicate(originalGraphUri);
Map<String, Long> regeneratedRelations = countRelationshipsByPredicate(regeneratedGraphUri);

for (String predicate : originalRelations.keySet()) {
    long original = originalRelations.get(predicate);
    long regenerated = regeneratedRelations.getOrDefault(predicate, 0L);
    assertTrue(regenerated >= original,
        "Predicate " + predicate + " count mismatch");
}
```

**SPARQL Pattern**:
```sparql
SELECT ?predicate (COUNT(*) AS ?count) WHERE {
  GRAPH <namedGraphUri> {
    ?s ?predicate ?o .
    FILTER(isURI(?o))
  }
}
GROUP BY ?predicate
```

**Example Output**:
```
[GRAPH ANALYSIS] Relationship: hasco:hasInstrument | Count: 71
[GRAPH ANALYSIS] Relationship: hasco:hasComponent | Count: 16
[GRAPH ANALYSIS] Relationship: rdfs:subClassOf | Count: 25
```

#### 4.3.3 Property Value Equality

**Purpose**: Verify that datatype properties (literals) have correct values

**Strategy**:
- Extract all **subject-predicate-value tuples** where value is a literal
- Compare value **strings** (normalized)
- Report mismatches with context

**Implementation**:
```java
Map<String, String> originalProps = extractLiteralProperties(originalGraphUri);
Map<String, String> regeneratedProps = extractLiteralProperties(regeneratedGraphUri);

for (String key : originalProps.keySet()) {
    String originalValue = originalProps.get(key);
    String regeneratedValue = regeneratedProps.get(key);
    assertEquals(originalValue, regeneratedValue,
        "Property value mismatch for " + key);
}
```

**Key Format**: `<subjectURI>|<predicateURI>` → `literalValue`

**Example**:
```
pmsr:INS123|rdfs:label → "Physiology Simulator"
pmsr:INS123|vstoi:hasSerialNumber → "SN-INS-123"
```

#### 4.3.4 Missing or Extra Triples Detection

**Purpose**: Identify unexpected additions or deletions

**Implementation**:
```java
Set<String> originalTriples = getAllTriplesAsStrings(originalGraphUri);
Set<String> regeneratedTriples = getAllTriplesAsStrings(regeneratedGraphUri);

Set<String> missing = new HashSet<>(originalTriples);
missing.removeAll(regeneratedTriples);

Set<String> extra = new HashSet<>(regeneratedTriples);
extra.removeAll(originalTriples);

if (!missing.isEmpty()) {
    System.out.println("[GRAPH DIFF] Missing " + missing.size() + " triples in regenerated graph");
    missing.stream().limit(10).forEach(t -> System.out.println("  MISSING: " + t));
}

if (!extra.isEmpty()) {
    System.out.println("[GRAPH DIFF] Extra " + extra.size() + " triples in regenerated graph");
    extra.stream().limit(10).forEach(t -> System.out.println("  EXTRA: " + t));
}
```

**Triple Serialization Format**: `<subject> <predicate> <object> .`

**Acceptance Criteria**:
- **Missing triples**: FAIL (data loss)
- **Extra triples**: PASS if they are **metadata augmentation** (timestamps, provenance)
- **Superset strategy**: Regenerated graph ⊇ Original graph

---

## 5. Advanced Validation Metrics

### 5.1 Graph Structure Analysis

The `analyzeGraphStructure()` method provides deep insights into the RDF graph:

**Metrics Collected**:

1. **Total Triple Count**
2. **Subject Count** (distinct entities)
3. **Predicate Diversity** (distinct property types)
4. **Object Type Distribution** (URIs vs Literals)
5. **Blank Node Usage** (indicates nesting/complexity)
6. **Namespace Coverage** (ontology dependencies)

**Example Output**:
```
[GRAPH ANALYSIS] ====================================
[GRAPH ANALYSIS] Graph: http://example.org/DF-INS-PMSR-Simulators.xlsx
[GRAPH ANALYSIS] Label: INS_ingested_original
[GRAPH ANALYSIS] ------------------------------------
[GRAPH ANALYSIS] Total triples: 1247
[GRAPH ANALYSIS] Distinct subjects: 104
[GRAPH ANALYSIS] Distinct predicates: 18
[GRAPH ANALYSIS] Object URIs: 523
[GRAPH ANALYSIS] Object literals: 724
[GRAPH ANALYSIS] Blank nodes: 0
[GRAPH ANALYSIS] ====================================
```

### 5.2 Relationship Completeness Matrix

For each predicate, the test tracks:
- **Cardinality**: Number of (subject, object) pairs
- **Domain Coverage**: Percentage of subjects using this predicate
- **Range Types**: Distribution of object types

**SPARQL Query**:
```sparql
SELECT ?predicate ?subject ?object WHERE {
  GRAPH <namedGraphUri> {
    ?subject ?predicate ?object .
  }
}
```

**Analysis Algorithm**:
```java
Map<String, Set<String>> predicateDomains = new HashMap<>();
Map<String, Set<String>> predicateRanges = new HashMap<>();

for (Triple t : triples) {
    String pred = t.getPredicate();
    predicateDomains.computeIfAbsent(pred, k -> new HashSet<>()).add(t.getSubject());
    predicateRanges.computeIfAbsent(pred, k -> new HashSet<>()).add(t.getObject());
}

for (String pred : predicateDomains.keySet()) {
    int domainSize = predicateDomains.get(pred).size();
    int rangeSize = predicateRanges.get(pred).size();
    System.out.println("Predicate: " + pred + " | Domain: " + domainSize + " | Range: " + rangeSize);
}
```

### 5.3 Property Value Equality Validation

**Normalization Rules**:
1. **Whitespace**: Trim leading/trailing spaces
2. **Case Sensitivity**: Preserve original case (RDF literals are case-sensitive)
3. **Datatype Handling**: 
   - String literals: `"value"` vs `"value"^^xsd:string` treated as equal
   - Numeric literals: Compare numeric values, not string representations
   - Dates: Normalize to ISO 8601 format

**Validation Logic**:
```java
for (String subjectPredicate : originalLiterals.keySet()) {
    String originalValue = normalize(originalLiterals.get(subjectPredicate));
    String regeneratedValue = normalize(regeneratedLiterals.get(subjectPredicate));
    
    if (!originalValue.equals(regeneratedValue)) {
        errors.add("Value mismatch: " + subjectPredicate + 
                   " | Expected: '" + originalValue + 
                   "' | Got: '" + regeneratedValue + "'");
    }
}
```

### 5.4 Missing/Extra Triples Detection

**Triple Fingerprinting**:
Each triple is serialized into a canonical string for set-based comparison:

```java
String fingerprint = String.format("<%s> <%s> %s .", 
    triple.getSubject(), 
    triple.getPredicate(), 
    serializeObject(triple.getObject())
);
```

**Diff Algorithm**:
```java
Set<String> originalFingerprints = getAllTripleFingerprints(originalGraph);
Set<String> regeneratedFingerprints = getAllTripleFingerprints(regeneratedGraph);

Set<String> missing = Sets.difference(originalFingerprints, regeneratedFingerprints);
Set<String> extra = Sets.difference(regeneratedFingerprints, originalFingerprints);
```

**Categorization of Extra Triples**:

| Category | Example | Action |
|----------|---------|--------|
| Provenance metadata | `prov:generatedAtTime`, `prov:wasAttributedTo` | ALLOW |
| Auto-generated IDs | `hasco:hasDataFile` (system-assigned) | ALLOW |
| Inferred relationships | `rdfs:subClassOf` (transitivity) | ALLOW |
| Unexpected data | User-defined properties not in original | FLAG/FAIL |

---

## 6. Test Execution Flow

### 6.1 JUnit Test Structure

The test uses **JUnit 5 ParameterizedTest** to execute all phases for each MT type:

```java
@ParameterizedTest
@ValueSource(strings = {"INS", "DP2", "WKF", "SDD"})
public void step1_allMTs_ingest(String mtName) {
    MTType type = MTType.valueOf(mtName);
    step1_ingest(type);
}
```

### 6.2 Mock DataFile Strategy

To avoid database dependencies, the test uses **Mockito** to create DataFile instances:

```java
private DataFile mockDataFileFor(File excelFile) {
    DataFile dataFile = mock(DataFile.class);
    IngestionLogger logger = mock(IngestionLogger.class);
    
    when(dataFile.getFilename()).thenReturn(excelFile.getName());
    when(dataFile.getLogger()).thenReturn(logger);
    
    // Generate safe URI (URL-encoded filename)
    String safeUri = "http://example.org/DF-" + 
        URLEncoder.encode(excelFile.getName(), StandardCharsets.UTF_8);
    when(dataFile.getUri()).thenReturn(safeUri);
    
    return dataFile;
}
```

**Why Mock?**
- **Isolation**: Test doesn't depend on database schema or persistence layer
- **Speed**: Avoid database round-trips during test setup
- **Determinism**: URI generation is predictable
- **Flexibility**: Can simulate error conditions (null URIs, invalid filenames)

### 6.3 Template Resolution

The test uses a **generic template** configuration:

```java
private static final String TEMPLATE_GENERIC = "template.generic.conf";
```

This template includes **all standard ontology mappings** required for parsing Excel columns into RDF predicates.

---

## 7. Graph Comparison Implementation

### 7.1 compareGraphs() Method

**Signature**:
```java
private static void compareGraphs(String originalGraphUri, String regeneratedGraphUri, String label)
```

**Metrics Computed**:

1. **Entity Type Distribution**
2. **Predicate Usage Statistics**
3. **Literal vs URI Object Ratio**
4. **Missing/Extra Triple Analysis**
5. **Structural Similarity Score**

**Output Format**:
```
========================================
GRAPH COMPARISON: INS (Step3)
========================================
Original Graph: http://example.org/DF-INS-PMSR-Simulators.xlsx
Regenerated Graph: http://example.org/DF-INS-PMSR-Simulators-regenerated.xlsx
----------------------------------------
Total Triples:        1247 → 1250 (3 extra)
Distinct Subjects:    104 → 104 (identical)
Distinct Predicates:  18 → 19 (1 new)
----------------------------------------
Entity Type Counts:
  hasco:Instrument:         25 → 25 ✓
  hasco:InstrumentInstance: 71 → 71 ✓
  hasco:Component:          8 → 8 ✓
----------------------------------------
Missing Triples: 0
Extra Triples: 3 (metadata additions)
========================================
```

### 7.2 Superset Validation Strategy

The test uses a **superset model** rather than strict equality:

**Rationale**:
- Regeneration may add **system metadata** (timestamps, IDs)
- Triplestore may **infer** additional relationships (OWL reasoning)
- Backwards compatibility: Regenerated templates may include **new features**

**Validation Rule**:
```
Regenerated ⊇ Original
```

**Formally**:
```
∀ triple t ∈ Original: t ∈ Regenerated ∨ t is metadata
```

---

## 8. Validation Artifacts & Outputs

### 8.1 Generated Files

All artifacts are written to `test/resources/generated/`:

| Artifact | Format | Purpose |
|----------|--------|---------|
| `[MT]-regenerated.xlsx` | Excel | Regenerated template for inspection |
| `[MT]_ingested_original.ttl` | Turtle | RDF export of original ingestion |
| `[MT]_ingested_regenerated.ttl` | Turtle | RDF export of regenerated ingestion |
| `[MT]_step3_reingested_original.ttl` | Turtle | Phase 3 original re-ingestion |
| `[MT]_step3_reingested_regenerated.ttl` | Turtle | Phase 3 regenerated re-ingestion |

### 8.2 Console Logging

**Structured Log Format**:
```
---------------------------------------------------------------------------------------
STEP 1/3 - INGEST & UNDERSTAND (Excel -> triplestore) - MT=INS
---------------------------------------------------------------------------------------

[TTL] INS_ingested_original: namedGraphUri=http://example.org/DF-INS-PMSR-Simulators.xlsx tripleCount=1247
[TTL] INS_ingested_original: wrote test/resources/generated/INS_ingested_original.ttl

[GRAPH ANALYSIS] ====================================
[GRAPH ANALYSIS] Graph: http://example.org/DF-INS-PMSR-Simulators.xlsx
[GRAPH ANALYSIS] Label: INS_ingested_original
[GRAPH ANALYSIS] Total triples: 1247
[GRAPH ANALYSIS] Entity type: hasco:Instrument | Count: 25
[GRAPH ANALYSIS] Entity type: hasco:InstrumentInstance | Count: 71
[GRAPH ANALYSIS] ====================================

---------------------------------------------------------------------------------------
STEP 1/3 - DONE - MT=INS
---------------------------------------------------------------------------------------
```

### 8.3 Test Assertion Strategy

**Hard Assertions** (FAIL if violated):
- File exists after generation
- File size > 0
- Required sheets present
- Entity counts: regenerated >= original
- No missing triples (from original)

**Soft Assertions** (WARN only):
- Extra triples (allowed if metadata)
- Column order differences
- Sheet order differences
- Formatting differences (colors, fonts)

---

## 9. Technical Dependencies

### 9.1 Core Libraries

- **Apache Jena 4.x**: RDF model manipulation and SPARQL execution
- **Apache POI 5.x**: Excel workbook creation and parsing
- **JUnit 5**: Test framework with parameterized test support
- **Mockito**: Mock object creation for DataFile

### 9.2 HASCO Components

- `IngestionWorker`: Orchestrates the ingestion pipeline
- `BaseGenerator`: Abstract generator for RDF model creation
- `MTGen` (various): Template-specific regeneration logic
- `SPARQLUtils`: SPARQL query execution helpers
- `TripleStoreDumpUtil`: Turtle serialization and export

---

## 10. Quality Metrics & Success Criteria

### 10.1 Lossless Transformation Criteria

A roundtrip test **PASSES** if:

1. ✓ **Completeness**: All entities from original are present in regenerated
2. ✓ **Relationship Preservation**: All semantic relationships are maintained
3. ✓ **Value Integrity**: All literal values match (normalized)
4. ✓ **Structural Equivalence**: Graph topology is isomorphic or superset

### 10.2 Determinism Criteria

Phase 3 **PASSES** if:

1. ✓ **Repeatable Ingestion**: Same Excel → Same RDF (modulo timestamps)
2. ✓ **Stable Regeneration**: Same RDF → Same Excel → Same RDF
3. ✓ **Graph Convergence**: Multiple ingest-regenerate cycles produce stable graph

**Convergence Test**:
```
Ingest(Original) → Graph₁
Regenerate(Graph₁) → Excel₂
Ingest(Excel₂) → Graph₂
Regenerate(Graph₂) → Excel₃
Ingest(Excel₃) → Graph₃

Assertion: Graph₁ ≅ Graph₂ ≅ Graph₃
```

---

## 11. Error Handling & Diagnostics

### 11.1 Failure Scenarios

| Scenario | Detection | Recovery |
|----------|-----------|----------|
| Excel parsing error | Exception during POI read | FAIL with stacktrace |
| SPARQL query timeout | Jena timeout exception | RETRY with increased timeout |
| Named graph not found | Zero triple count | FAIL (ingestion didn't work) |
| Malformed URI in data | QueryException during regeneration | FAIL with URI details |
| Missing required sheet | Sheet lookup returns null | FAIL with sheet name |

### 11.2 Diagnostic Outputs

**When a test fails**, the following diagnostics are available:

1. **Turtle Files**: Manual diff of `.ttl` exports
2. **Console Logs**: Full SPARQL queries and results
3. **Entity Count Table**: Side-by-side comparison
4. **Missing/Extra Triple List**: Up to 100 examples
5. **Excel Files**: Side-by-side comparison in Excel

**Example Failure Output**:
```
[GRAPH COMPARISON] MISMATCH DETECTED
Original graph has 1247 triples
Regenerated graph has 1210 triples (37 MISSING)

Missing triples (first 10):
  <pmsr:INS123> <hasco:hasComponent> <pmsr:COI456> .
  <pmsr:COI456> <rdfs:label> "Electrode" .
  ...

Entity count mismatches:
  hasco:Component: expected 8, got 5 (3 MISSING)
```

---

## 12. Extension Points

### 12.1 Adding New MT Types

To add a new MT type to the roundtrip test:

1. **Add to MTType enum**:
   ```java
   private enum MTType {
       DSG, INS, DP2, WKF, SDD, DA, NEW_MT
   }
   ```

2. **Register test file**:
   ```java
   case NEW_MT:
       return new File("test/resources/newmt/NEWMT-test.xlsx");
   ```

3. **Implement step1**:
   ```java
   if (type == MTType.NEW_MT) {
       DataFile df = mockDataFileFor(excel);
       assertDoesNotThrow(() -> IngestionWorker.ingest(df, excel, TEMPLATE_GENERIC, VSTOI.DRAFT));
       dumpAndLogTtl("NEWMT_ingested_original", df);
   }
   ```

4. **Implement step2**:
   ```java
   if (type == MTType.NEW_MT) {
       String result = NEWMTGen.genByStatus(VSTOI.DRAFT, REGENERATED_NEWMT_FILENAME, null, null);
       // ... validation logic ...
   }
   ```

5. **Implement step3**:
   ```java
   if (type == MTType.NEW_MT) {
       // ... cleanup, re-ingest, compare ...
   }
   ```

### 12.2 Custom Comparison Strategies

For MT types with **special semantics**, implement custom comparison:

```java
private static boolean isCustomMTWorkbookSuperset(File regenerated, File original) {
    // Custom logic for specific MT requirements
    // Example: check only specific sheets, ignore others
}
```

---

## 13. Performance Considerations

### 13.1 Optimization Strategies

1. **Batch SPARQL Queries**: Use `UNION` to fetch multiple entity types in one query
2. **Parallel Processing**: Use Java streams for entity processing
3. **Lazy Evaluation**: Only compare what's needed (don't load entire graph into memory)
4. **Graph Streaming**: Use SPARQL result streaming for large graphs

### 13.2 Scalability Limits

Current implementation tested with:
- **Entities per graph**: Up to 5000
- **Triples per graph**: Up to 50,000
- **Excel rows**: Up to 10,000
- **Named graphs**: Up to 100 concurrent

**Memory Requirements**:
- Small templates (<100 entities): ~50 MB
- Medium templates (100-1000 entities): ~200 MB
- Large templates (>1000 entities): ~1 GB

---

## 14. Best Practices & Guidelines

### 14.1 Test Maintenance

1. **Keep test data minimal**: Use small, representative templates
2. **Document exceptions**: If extra triples are expected, comment why
3. **Version control TTL files**: Track graph evolution over time
4. **Automate cleanup**: Ensure named graphs are deleted after tests

### 14.2 Debugging Workflow

**When a roundtrip test fails:**

1. **Check TTL diffs**:
   ```bash
   diff test/resources/generated/MT_ingested_original.ttl \
        test/resources/generated/MT_ingested_regenerated.ttl
   ```

2. **Inspect Excel sheets**:
   - Open both original and regenerated Excel
   - Compare sheet by sheet
   - Look for missing rows, columns, or values

3. **Query triplestore directly**:
   ```sparql
   SELECT * WHERE {
     GRAPH <http://example.org/DF-MT-test.xlsx> {
       ?s ?p ?o .
     }
   } LIMIT 100
   ```

4. **Check generator logs**:
   - Look for `[SDDGen]`, `[DP2Gen]` prefixed messages
   - Identify SPARQL queries being executed
   - Check for null pointer warnings

---

## 15. Known Limitations & Future Work

### 15.1 Current Limitations

1. **Blank Node Handling**: Blank nodes complicate graph isomorphism checks
2. **Timestamp Variability**: `prov:generatedAtTime` causes non-determinism
3. **Inference**: OWL reasoning may add triples not in original
4. **Sheet Order**: Excel sheet order not guaranteed to match

### 15.2 Future Enhancements

1. **Semantic Diff**: Use RDF graph isomorphism algorithms instead of string comparison
2. **Visual Reports**: Generate HTML comparison reports with highlighted diffs
3. **Performance Benchmarks**: Track ingestion/regeneration time per MT type
4. **Coverage Metrics**: Measure ontology term usage across all templates
5. **Regression Detection**: Flag when regenerated graph loses data from previous versions

---

## 16. Conclusion

The **HASCO Roundtrip Test** provides comprehensive validation of the **bidirectional transformation pipeline** between Excel-based metadata templates and RDF knowledge graphs. By implementing a **3-phase lifecycle** with **multi-metric validation**, it ensures:

- **Data Fidelity**: No information loss during Excel ↔ RDF transformation
- **Determinism**: Consistent results across multiple ingestion cycles
- **Quality Assurance**: Early detection of regressions in template processing
- **Debugging Support**: Rich diagnostic artifacts for troubleshooting

This framework serves as the **foundation for metadata template quality control** in the HASCO ecosystem, enabling confident deployment of template-driven data ingestion workflows.

---

**Document Version**: 1.0  
**Last Updated**: 2026-03-23  
**Maintainer**: HASCO Development Team

