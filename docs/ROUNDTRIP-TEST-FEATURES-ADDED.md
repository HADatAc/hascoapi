# Roundtrip Test Enhancement - Features Added

## Summary

Enhanced the **HascoRoundtripTest** with comprehensive graph analysis features to detect and report differences between original and regenerated metadata templates. These features provide deep insights into data integrity, semantic preservation, and transformation accuracy.

## New Features Added

### 1. Entity Counts by Type

**Purpose**: Track the number of entities grouped by their RDF type to detect data loss or unexpected entity creation.

**Implementation**:
- Queries `rdf:type` for all entities in a named graph
- Groups and counts by type URI
- Displays results sorted by count (most common first)

**Example Output**:
```
[ENTITY COUNTS BY TYPE]
  vstoi:InstrumentInstance: 71
  vstoi:ComponentInstance: 16
  vstoi:Deployment: 71
  vstoi:Platform: 1
  vstoi:PlatformInstance: 1
```

**Key Method**: `getEntityCountsByType(String graphUri)`

**Use Case**: Quickly identify if certain entity types were lost during regeneration (e.g., if Platforms dropped from 1 to 0).

---

### 2. Relationship Completeness

**Purpose**: Identify dangling references - URIs referenced as objects that don't exist as subjects within the same graph.

**Implementation**:
- Queries for object URIs that have no triples where they are the subject
- Groups dangling references by predicate
- Reports count of dangling objects per relationship type

**Example Output**:
```
[RELATIONSHIP COMPLETENESS]
  DANGLING REFS via hasco:hasComponentOf: 10
  DANGLING REFS via vstoi:usesInstrument: 2
```
or
```
[RELATIONSHIP COMPLETENESS]
  ✓ All relationships complete (no dangling references)
```

**Key Method**: `checkRelationshipCompleteness(String graphUri)`

**Use Case**: Detect incomplete ingestion where referenced entities (e.g., instruments, components) weren't properly created or linked.

---

### 3. Property Value Statistics

**Purpose**: Analyze how properties are used across the graph - total uses and distinct value counts.

**Implementation**:
- Counts total property assertions
- Counts distinct values per property
- Shows top 15 most-used properties

**Example Output**:
```
[PROPERTY VALUE STATISTICS]
  hasco:hascoType: 160 values (5 distinct)
  hasco:hasDataFile: 160 values (1 distinct)
  rdfs:label: 159 values (71 distinct)
  vstoi:hasSerialNumber: 71 values (71 distinct)
  vstoi:hasStatus: 160 values (1 distinct)
```

**Key Method**: `getPropertyValueStatistics(String graphUri)`

**Use Case**: Understand data distribution - e.g., if all entities share one `hasDataFile` value (expected) vs. many distinct values (potential bug).

---

### 4. Graph Comparison (Original vs Regenerated)

**Purpose**: Comprehensive comparison of two named graphs to detect missing data, extra data, and value changes.

**Implementation**:
Four-part analysis:

#### a) Triple Count Comparison
```
Triple counts:
  Original:    609
  Regenerated: 609
  Difference:  0
```

#### b) Entity Count Comparison by Type
```
[ENTITY COUNT COMPARISON BY TYPE]
  ✓ vstoi:InstrumentInstance: 71 -> 71
  ✓ vstoi:ComponentInstance: 16 -> 16
  - vstoi:Platform: 1 -> 0
  + vstoi:CustomType: 0 -> 2
```

**Symbols**:
- `✓` = Exact match (good)
- `-` = Count decreased (potential data loss)
- `+` = Count increased (extra data or metadata)

#### c) Missing Triples Detection
```
[MISSING TRIPLES] (in original but NOT in regenerated)
  Found 3 missing triples (showing first 10):
    - <pmsr:PMSR_Simulation_Platform> <hasco:hasMaker> ""
    - <pmsr:INI1739301009974715> <hasco:hasWebDocument> ""
```

Shows specific triples that were lost during the roundtrip.

#### d) Extra Triples Detection
```
[EXTRA TRIPLES] (in regenerated but NOT in original)
  Found 5 extra triples (showing first 10):
    + <pmsr:INI1739301009974715> <hasco:hasGenerationTimestamp> "2026-03-17T13:45:00"
    + <pmsr:DPL1739301009974715> <hasco:hasDataFileVersion> "2"
```

Shows triples added during regeneration (may include acceptable metadata like timestamps).

**Key Method**: `compareGraphs(String originalGraphUri, String regeneratedGraphUri, String mtType)`

**Use Case**: Verify that regenerated files are semantically equivalent to originals, identify transformation bugs.

---

### 5. Property Value Equality

**Purpose**: Detect cases where the same subject-property pair has different values in original vs regenerated.

**Implementation**:
- Joins original and regenerated graphs on subject+property
- Filters for cases where values differ
- Groups and counts differences by property

**Example Output**:
```
[PROPERTY VALUE EQUALITY]
  DIFFERENT VALUES for vstoi:hasSerialNumber: 3 instances
  DIFFERENT VALUES for hasco:hasDataFile: 160 instances
```
or
```
[PROPERTY VALUE EQUALITY]
  ✓ All property values match between original and regenerated
```

**Key Method**: `comparePropertyValues(String graph1Uri, String graph2Uri)`

**Use Case**: Detect subtle bugs where entity structure is preserved but values are corrupted (e.g., serial numbers getting scrambled).

---

## Integration Points

### When These Features Run

1. **After Step 1 (Ingest)**: 
   - `analyzeGraphStructure()` runs automatically
   - Provides baseline understanding of ingested data

2. **After Step 2 (Regenerate)**:
   - Same analysis on regenerated data
   - No comparison yet (waiting for Step 3)

3. **After Step 3 (Reingest)**:
   - **Full comparison executed**: `compareGraphs(originalUri, regeneratedUri, mtType)`
   - All 5 features work together to validate roundtrip integrity

### Test Execution Flow

```
Step 1: Ingest Original
    ↓
  dumpAndLogTtl() → analyzeGraphStructure()
    ↓
  [Entity Counts, Relationships, Property Stats]

Step 2: Regenerate
    ↓
  dumpAndLogTtl() → analyzeGraphStructure()
    ↓
  (Same analysis on regenerated data)

Step 3: Reset & Reingest Both
    ↓
  dumpAndLogTtl() + dumpAndLogTtl()
    ↓
  compareGraphs()
    ↓
  [Triple Counts, Entity Comparison, Missing/Extra Triples, Value Equality]
```

---

## Technical Implementation Details

### Key Methods Added

| Method | Purpose | Returns |
|--------|---------|---------|
| `analyzeGraphStructure()` | Main orchestrator for graph analysis | void (prints report) |
| `getEntityCountsByType()` | Count entities by rdf:type | `Map<String, Long>` |
| `checkRelationshipCompleteness()` | Find dangling references | void (prints report) |
| `getPropertyValueStatistics()` | Analyze property usage | void (prints report) |
| `compareGraphs()` | Compare two graphs | void (prints report) |
| `findMissingTriples()` | Find triples in graph1 not in graph2 | `List<String>` |
| `comparePropertyValues()` | Find value differences | void (prints report) |
| `formatRdfNode()` | Format RDF nodes for display | `String` |
| `shortenUri()` | Abbreviate URIs with prefixes | `String` |

### SPARQL Queries Used

#### Entity Counts
```sparql
SELECT ?type (COUNT(DISTINCT ?entity) AS ?count)
WHERE {
  GRAPH <graphUri> {
    ?entity rdf:type ?type .
  }
}
GROUP BY ?type ORDER BY DESC(?count)
```

#### Dangling References
```sparql
SELECT ?predicate (COUNT(DISTINCT ?object) AS ?danglingCount)
WHERE {
  GRAPH <graphUri> {
    ?subject ?predicate ?object .
    FILTER(isURI(?object))
    FILTER NOT EXISTS {
      GRAPH <graphUri> { ?object ?p ?o }
    }
  }
}
GROUP BY ?predicate ORDER BY DESC(?danglingCount)
```

#### Missing Triples
```sparql
SELECT ?s ?p ?o
WHERE {
  GRAPH <graph1> { ?s ?p ?o }
  FILTER NOT EXISTS {
    GRAPH <graph2> { ?s ?p ?o }
  }
}
LIMIT 100
```

#### Property Value Differences
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

---

## Interpreting Results

### ✅ Good Indicators

- **Entity counts match exactly** between original and regenerated
- **No dangling references** in any graph
- **No missing triples** (or only empty string properties that are optional)
- **Extra triples limited to metadata** (timestamps, auto-generated IDs, version numbers)
- **Property value differences explained** (e.g., `hasco:hasDataFile` differs due to different graph URIs)

### ⚠️ Warning Signs

- **Entity count mismatches**: Some types have fewer entities after regeneration
- **Dangling references present**: Indicates incomplete data or broken links
- **Significant missing triples**: Data was lost during transformation
- **Unexplained property value differences**: Values corrupted or transformed incorrectly

### 🔴 Critical Issues

- **Large triple count difference**: Massive data loss or duplication
- **Core entity types missing entirely**: Generation logic completely failed
- **All relationships dangling**: Graph structure broken
- **Systematic value differences**: Transformation algorithm has fundamental bug

---

## Example Test Output

```
[GRAPH ANALYSIS] INS_ingested_original
==========================================
[ENTITY COUNTS BY TYPE]
  vstoi:InstrumentInstance: 71
  vstoi:ComponentInstance: 16
  vstoi:Deployment: 71
  vstoi:Platform: 1
  vstoi:PlatformInstance: 1

[RELATIONSHIP COMPLETENESS]
  ✓ All relationships complete (no dangling references)

[PROPERTY VALUE STATISTICS]
  hasco:hascoType: 160 values (5 distinct)
  hasco:hasDataFile: 160 values (1 distinct)
  rdfs:label: 159 values (71 distinct)
  vstoi:hasSerialNumber: 87 values (87 distinct)
==========================================

[GRAPH COMPARISON] INS (Step3) - Original vs Regenerated
==========================================
Original graph:     http://example.org/DF-INS-PMSR-Simulators.xlsx
Regenerated graph:  http://example.org/DF-INS-PMSR-Simulators-regenerated.xlsx

Triple counts:
  Original:    609
  Regenerated: 609
  Difference:  0

[ENTITY COUNT COMPARISON BY TYPE]
  ✓ vstoi:InstrumentInstance: 71 -> 71
  ✓ vstoi:ComponentInstance: 16 -> 16
  ✓ vstoi:Deployment: 71 -> 71
  ✓ vstoi:Platform: 1 -> 1
  ✓ vstoi:PlatformInstance: 1 -> 1

[MISSING TRIPLES] (in original but NOT in regenerated)
  ✓ No missing triples - regenerated contains all original data

[EXTRA TRIPLES] (in regenerated but NOT in original)
  ✓ No extra triples - regenerated matches original exactly

[PROPERTY VALUE EQUALITY]
  ✓ All property values match between original and regenerated
==========================================
```

---

## Benefits

1. **Automated Regression Detection**: Immediately catch when code changes break data integrity
2. **Debugging Efficiency**: Pinpoint exact triples/properties that differ, not just "something is wrong"
3. **Confidence in Refactoring**: Know that transformations preserve semantics even after major changes
4. **Documentation**: Test output serves as living documentation of expected graph structure
5. **Quality Metrics**: Track entity counts, property coverage, relationship density over time

---

## Future Enhancements

Possible additions to consider:

1. **Assertion Mode**: Make tests fail if ANY differences detected (strict equality)
2. **Diff HTML Report**: Generate visual side-by-side comparison in browser
3. **Performance Tracking**: Measure SPARQL query time, report slowdowns
4. **Schema Validation**: Verify against SHACL shapes or OWL constraints
5. **Historical Comparison**: Compare current test run against previous baseline
6. **Cardinality Validation**: Check min/max occurrences of properties per entity

---

## Files Modified

- **`test/org/hascoapi/tests/HascoRoundtripTest.java`**:
  - Added `analyzeGraphStructure()`
  - Added `getEntityCountsByType()`
  - Added `checkRelationshipCompleteness()`
  - Added `getPropertyValueStatistics()`
  - Added `compareGraphs()`
  - Added `findMissingTriples()`
  - Added `comparePropertyValues()`
  - Added `formatRdfNode()`
  - Modified `dumpAndLogTtl()` to call `analyzeGraphStructure()`
  - Modified `step3_reset_and_deterministic_reingest()` for INS/DP2/WKF to call `compareGraphs()`

## Files Created

- **`docs/ROUNDTRIP-TEST-TECHNICAL-ARCHITECTURE.md`**: Comprehensive technical documentation of the entire roundtrip test framework

---

## Conclusion

These enhancements transform the roundtrip test from a basic "does it crash?" check into a **quantitative semantic validation framework** that provides actionable insights into metadata template processing quality.

