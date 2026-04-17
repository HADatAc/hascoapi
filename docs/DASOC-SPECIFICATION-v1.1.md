# DA-SOC SPECIFICATION
## CONSOLIDATED UNDERSTANDING

**Version:** 1.1  
**Date:** 2026-03-30  
**Status:** CANONICAL REFERENCE  
**Supersedes:** v1.0 (2026-03-25)

---

## Document Updates

### v1.1 (2026-03-30)
- **Updated Section 4.2**: Corrected SOC resolution logic based on actual `IngestionWorker.findSOCByName()` implementation
- **Updated Section 4.3**: Clarified that DA URI is auto-generated or derived from DataFile, not user-provided
- **Updated Section 4.4**: Corrected annotation pipeline to reflect actual CSV processing with Apache Commons CSV
- **Resolved Q1 (Open Questions)**: DA URI construction now documented
- **Resolved Q3 (Open Questions)**: Empty SOC name handling confirmed - no defensive check exists (caveat added)
- **Added Section 9**: Ingestion Entry Points (new) - documents how DA-SOC routing works through IngestionWorker

---

## Table of Contents

1. [What a DA-SOC Is](#1-what-a-da-soc-is)
2. [Global Architectural Principles](#2-global-architectural-principles)
3. [DA-SOC File Structure](#3-da-soc-file-structure)
4. [Ingestion Pipeline](#4-ingestion-pipeline)
5. [Validation Rules and Error Codes](#5-validation-rules-and-error-codes)
6. [Modeling Rules Derived from the Codebase](#6-modeling-rules-derived-from-the-codebase)
7. [Example: LTE-PIAGET-WEATHER-STATION](#7-example-lte-piaget-weather-station)
8. [How to Use This Specification Going Forward](#8-how-to-use-this-specification-going-forward)
9. [Ingestion Entry Points](#9-ingestion-entry-points)
10. [Open Questions](#10-open-questions)
11. [Appendix: Glossary](#appendix-glossary)

---

## 1. What a DA-SOC Is

**DA-SOC** stands for **Data Acquisition — Study Object Collection**.

A DA-SOC is a **CSV file** that enriches existing `StudyObject` instances within a `StudyObjectCollection` (SOC) by adding extra scope properties that were not captured during the original DSG ingestion. Unlike DSG files (which create new SOC instances), DA-SOC files **extend** existing instances with additional properties that do not fit within the DSG schema constraints.

### Key Characteristics

| Characteristic | Description |
|----------------|-------------|
| **CSV Format Only** | DA-SOC files are always CSV (comma-separated values), not Excel workbooks. Column 0 is always `originalID`. Columns 1..N are dynamic property URIs. Each row corresponds to exactly one existing StudyObject instance. |
| **Extends Existing Instances** | A DA-SOC cannot create new StudyObject instances. It can only add properties to objects that already exist in the triplestore (ingested via a prior DSG file). |
| **Referential Integrity** | Every `originalID` in a DA-SOC CSV must match an existing StudyObject in the target SOC. If a row references a non-existent `originalID`, the row is skipped with a warning (`DASOC_00003`). |
| **File Name Drives SOC Resolution** | The SOC URI is resolved from the file name using the pattern `DA-SOC-{SOCNAME}.csv`, where `{SOCNAME}` is used to query the triplestore for the corresponding StudyObjectCollection URI via `IngestionWorker.findSOCByName()`. |
| **Belongs to Exactly One DSG** | A DA-SOC is always scoped to a single DSG context and cannot be shared across multiple DSGs. |
| **DataFile Linkage Required** | At upload time the DA-SOC CSV is external to HADatAc. The pipeline must resolve `dasocDataAcquisitionUri` and `dasocSOCUri` and store them on the DataFile entity. |

---

## 2. Global Architectural Principles

### 2.1  Mandatory First Column: `originalID`

**Rule:** Column index 0 must always be the `originalID` column.

**Rationale:** The `originalID` is the primary lookup key used to match CSV rows to existing StudyObject instances in the triplestore. Without a valid `originalID`, the system cannot determine which object to enrich.

This constraint is hard-coded in `AnnotateDASOC.processCSVFile()`:

```java
String originalIdColumn = headers.get(0); // First column is originalID
```

**Validation:** If the CSV has fewer than 2 columns (no property columns after `originalID`), ingestion fails with error `DASOC_00006`. Empty `originalID` values are silently skipped.

⚠️  **If `originalID` is missing or placed in a non-zero column, all rows will fail to match, resulting in mass `DASOC_00003` warnings and zero successful ingestions.**

---

### 2.2  Extra Columns Are Unbounded Scope Extensions

**Rule:** Columns 1..N are dynamic property URIs with no upper limit on count.

Each column header (from index 1 onward) is interpreted as an **RDF predicate URI** (or a CURIE that will be expanded via `URIUtils.replacePrefixEx()`). The system does not pre-validate these URIs against any schema or ontology.

**Three key behaviors apply:**

1. **Prefix Expansion** — Column headers like `pharma:altitude_m` are expanded to full URIs using the namespace registry.
2. **Type Detection** — If a cell value passes `URIUtils.isValidURI()`, it becomes an **RDF resource**; otherwise it becomes a **string literal**.
3. **Empty Cells Skipped** — Empty cells do not generate triples — no null/empty literal pollution.

---

### 2.3  All `originalID` Values Must Exist in the Parent DSG

**Rule:** A DA-SOC row is rejected if its `originalID` does not match an existing StudyObject in the target SOC.

When processing starts, `AnnotateDASOC.buildOriginalIdMap()` queries the triplestore and builds a lookup map `originalID → objectURI`:

```sparql
SELECT ?objUri ?originalId WHERE {
    ?objUri hasco:isMemberOf <{socUri}> .
    ?objUri hasco:originalID ?originalId .
}
```

During CSV processing, if the map returns `null` for a given `originalID`, the row is skipped with warning `DASOC_00003`. This is a **soft error** — ingestion continues for valid rows.

---

### 2.4  File Name Drives SOC Resolution

**Rule:** The file name must follow the pattern `DA-SOC-{SOCNAME}.csv` where `{SOCNAME}` is used to resolve the SOC URI.

**Implementation:** `IngestionWorker.ingest()` extracts the SOC name:

```java
if (FilenameUtils.getBaseName(fileName).startsWith("DA-SOC-")) {
    String baseName = FilenameUtils.getBaseName(fileName);
    String socName = baseName.substring(7); // "DA-SOC-ENTERPRISE" -> "ENTERPRISE"
    String socUri = findSOCByName(socName);
    // ...
}
```

**`findSOCByName()` resolution logic:**

1. **Query triplestore** for SOCs with matching label or URI:
   ```sparql
   SELECT ?socUri WHERE {
       ?socUri a hasco:StudyObjectCollection .
       { ?socUri rdfs:label ?label . FILTER(CONTAINS(UCASE(?label), UCASE("{socName}"))) }
       UNION
       { FILTER(CONTAINS(UCASE(STR(?socUri)), UCASE("SOC-{socName}"))) }
   } LIMIT 1
   ```

2. **If no result found**, construct fallback URI:
   ```java
   socUri = kbPrefix + "SOC-" + socName
   // Example: http://hadatac.org/ont/arrowhead/SOC-LOCATION
   ```

**Examples:**

| File Name | Extracted SOC Name | Resolved SOC URI |
|-----------|-------------------|------------------|
| `DA-SOC-LOCATION.csv` | `LOCATION` | `.../SOC-LOCATION` |
| `DA-SOC-ENTERPRISE.csv` | `ENTERPRISE` | `.../SOC-ENTERPRISE` |
| `DA-SOC-API.csv` | `API` | `.../SOC-API` |

⚠️  **If a file is named `DA-SOC-.csv` (empty SOC name), `socName` becomes an empty string, leading to an invalid fallback URI. No defensive check currently exists.**

---

### 2.5  One DA-SOC Belongs to Exactly One DSG

**Cardinality:** `1 DA-SOC → 1 SOC → 1 DSG`

The SOC URI resolved from the file name is study-scoped. When the system queries for SOC objects via `hasco:isMemberOf <socUri>`, it retrieves objects from a single study context.

There is **no cross-DSG contamination** because each DSG creates SOC instances with unique URIs tied to its study. **Multiple DA-SOCs per DSG are allowed**, but each targets exactly one SOC.

---

## 3. DA-SOC File Structure

### 3.1  File Naming Convention

**Pattern:** `DA-SOC-{SOCNAME}.csv`

**Components:**
- `DA-SOC-` — Required prefix that triggers DA-SOC ingestion routing
- `{SOCNAME}` — SOC identifier extracted and used to resolve the SOC URI
- `.csv` — Required extension. Excel workbooks are not supported

**Invalid names (will not trigger DA-SOC routing):**
- `DASOC-LOCATION.csv` — Missing hyphen after DA
- `DA-SOC-LOCATION.xlsx` — Excel format
- `DA-LOCATION.csv` — Missing SOC component, rejected as unsupported general DA

---

### 3.2  Column Structure

**Header Row:** Required — the first row is always treated as column headers.

**Minimum column count:** 2 (Column 0 + at least 1 property column). If `headers.size() < 2`, ingestion fails with error `DASOC_00006`.

| Column Index | Name / Purpose | Required | Description |
|--------------|----------------|----------|-------------|
| 0 | `originalID` | ✅ Yes | Lookup key to match existing StudyObject instances in the SOC |
| 1..N | Property URIs | ✅ Yes | Dynamic property columns — headers are RDF predicate URIs or CURIEs |

---

### 3.3  Data Types

| Field | Type | Validation |
|-------|------|------------|
| **Column 0 (`originalID`)** | String literal | Must match `hasco:originalID` of an existing StudyObject. Empty values are silently skipped. |
| **Columns 1..N (Property Values)** | Auto-detected | If `URIUtils.isValidURI(value)` returns true, the value becomes an **RDF resource**; otherwise an **RDF literal** (string). Empty cells are silently skipped. |

**Encoding:** UTF-8

---

### 3.4  Example: LTE-PIAGET-WEATHER-STATION

**Study Context:**
- **Study URI:** `http://hadatac.org/ont/arrowhead/LTE-PIAGET-WEATHER-STATION`
- **SOC:** `SOC-LOCATION` (50 instances)
- **DA-SOC File:** `DA-SOC-LOCATION.csv`

**File Contents:**

```csv
originalID,pharma:altitude_m,pharma:floor,pharma:zone_code
LIBRARY-L0,15.0,0,ZONE-A
SCIENCE-ROOF,25.5,4,ZONE-B
CAFETERIA-EAST,12.0,1,ZONE-A
ADMIN-BASEMENT,8.5,-1,ZONE-C
```

**Result:** Each row looks up the corresponding StudyObject by `originalID` and adds:
- 3 property triples (altitude, floor, zone_code)
- 1 timestamp triple
- **= 4 triples per row × 4 rows = 16 total triples**

---

## 4. Ingestion Pipeline

DA-SOC ingestion flows through a **6-stage pipeline** orchestrated by `IngestionWorker`, `AnnotateDASOC`, and `DASOCGenerator`.

### 4.1  File Detection  (`IngestionWorker`)

**Trigger:** File name starts with `DA-SOC-`

**Code Path:**
```java
// In IngestionWorker.getGeneratorChain()
if (fileName.startsWith("DA-SOC-")) {
    System.out.println("✅ Matched DA-SOC-* pattern, routing to AnnotateDASOC");
    chain = AnnotateDASOC.exec(dataFile);
} else if (fileName.startsWith("DA-")) {
    // REJECT: General DA ingestion is not supported
    dataFile.setFileStatus(DataFile.UNPROCESSED);
    return null;
}
```

**Important:** General DA files (e.g., `DA-MEASUREMENTS.csv`) are explicitly **rejected**. Only `DA-SOC-*` files are routed to `AnnotateDASOC`.

---

### 4.2  SOC Resolution  (`IngestionWorker.findSOCByName`)

**Steps:**

1. **Extract SOC Name** from file name (`DA-SOC-{SOCNAME}.csv` → `{SOCNAME}`)
2. **Query Triplestore** for matching SOC:
   ```sparql
   SELECT ?socUri WHERE {
       ?socUri a hasco:StudyObjectCollection .
       { ?socUri rdfs:label ?label . FILTER(CONTAINS(UCASE(?label), UCASE("{socName}"))) }
       UNION
       { FILTER(CONTAINS(UCASE(STR(?socUri)), UCASE("SOC-{socName}"))) }
   } LIMIT 1
   ```
3. **Fallback to Constructed URI** if no result:
   ```java
   socUri = kbPrefix + "SOC-" + socName
   // Example: http://hadatac.org/ont/arrowhead/SOC-LOCATION
   ```
4. **Store in `DataFile.dasocSOCUri`**

**Logging:**
```
[INGESTION PATH] Detected DASOC file pattern: DA-SOC-*
[INGESTION PATH] Extracting SOC name: LOCATION
IngestionWorker.findSOCByName(): Found SOC URI: .../SOC-LOCATION
[INGESTION PATH] Auto-detected SOC URI from triplestore: .../SOC-LOCATION
```

---

### 4.3  DataFile Linkage

**DA URI Construction:**

The DA URI is **auto-generated** or **derived from DataFile**, not user-provided. The exact construction logic is:

1. **If DA already exists:** Retrieved via `DA.find(daUri)` where `daUri` comes from `DataFile.getDasocDataAcquisitionUri()`
2. **If DA does not exist:** Created by `AnnotateDASOC.processDASOC()`:
   ```java
   da = new DA();
   da.setUri(daUri); // daUri from DataFile
   da.setLabel(dataFile.getFilename());
   da.setComment("Data Acquisition for " + dataFile.getFilename());
   da.setHascoTypeUri("http://hadatac.org/ont/hasco/DataAcquisition");
   da.setTypeUri("http://hadatac.org/ont/hasco/DataAcquisition");
   da.setHasSIRManagerEmail(dataFile.getHasSIRManagerEmail());
   da.setHasDataFileUri(dataFile.getUri());
   da.setHasStatus("UNPROCESSED");
   da.save();
   ```

**DataFile Fields:**

| Field | Set By | Value Source |
|-------|--------|--------------|
| `dasocSOCUri` | `IngestionWorker.ingest()` | Resolved from file name via `findSOCByName()` |
| `dasocDataAcquisitionUri` | `AnnotateDASOC` (implicit) | Constructed or retrieved from DataFile |

---

### 4.4  Annotation  (`AnnotateDASOC`)

The annotation step is composed of three sub-steps:

#### 4.4.1  Build `originalID → Object URI` Map

**Method:** `AnnotateDASOC.buildOriginalIdMap(socUri, dataFile)`

**SPARQL Query:**
```sparql
SELECT ?objUri ?originalId WHERE {
    ?objUri hasco:isMemberOf <{socUri}> .
    ?objUri hasco:originalID ?originalId .
}
```

**Validation:**
- Empty map → `DASOC_00002` (fatal)
- Invalid URIs (not `http://` or `https://`) → `DASOC_00004` (warning, skipped)
- Duplicate `originalIDs` → `DASOC_00005` (warning, first kept)

**Logging:**
```
✅ (1) Found related SOC: <.../SOC-LOCATION>
✅ (2) Created originalID<->URI map with 50 elements
```

---

#### 4.4.2  Parse CSV File

**Method:** `AnnotateDASOC.processCSVFile()`

**CSV Parser Configuration:**
```java
CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
    .withFirstRecordAsHeader()
    .withIgnoreHeaderCase()
    .withTrim()
    .withAllowMissingColumnNames(true));
```

**For each row:**
1. Read `originalID` from column 0
2. Look up `objectURI` in the map (`DASOC_00003` if not found)
3. Generate triples for columns 1..N:
   - Expand predicate URI via `URIUtils.replacePrefixEx()`
   - Detect value type: URI (resource) vs literal (string)
   - Add triple: `<objectURI> <predicateURI> <value>`
4. Add timestamp triple: `<objectURI> hasco:hasTimestamp "ISO8601"`
5. Write to RDF model in **batches of 10,000 rows**

**Counters tracked:**
- `totalCSVRows` — Total data rows in CSV (excluding header)
- `objectsCreated` — Successfully processed objects
- `skippedRows` — Rows with empty/invalid `originalID`
- `errorRows` — Rows that threw exceptions

**Logging:**
```
✅ (3) Total rows in DA-SOC-API.csv: 4 (excluding header)
✅ (4) Number of objects created: 4
```

---

#### 4.4.3  Property Count Formula

**Total Properties per Object = 5 (base from DSG) + N (DA-SOC columns 1..N) + 1 (timestamp)**

| Property Group | Fields | Count |
|----------------|--------|-------|
| **Base (from DSG)** | `rdf:type`, `hasco:isMemberOf`, `hasco:originalID`, `hasco:label`, `rdfs:comment` | 5 |
| **DA-SOC Extra** | Dynamic — columns 1..N from CSV | N |
| **Timestamp** | `hasco:hasTimestamp` | 1 |
| **Total** | — | **6 + N** |

**Example:** 3 extra columns → Total = 5 + 3 + 1 = **9 properties per object**

---

### 4.5  Generation  (`DASOCGenerator`)

**Role:** `DASOCGenerator` wraps `AnnotateDASOC.processDASOC()` in a `BaseGenerator`-compatible interface so DA-SOC can flow through `GeneratorChain.generate()`.

**Override behavior:**
- `initMapping()` — no-op
- `createObject()` — returns `null`
- `createRow()` — returns `null`
- `createRows()` — delegates to `AnnotateDASOC.processDASOC()`

**Named Graph:** All DA-SOC triples are stored in a **separate named graph** with URI:

```
{daUri}-dasoc
```

**Example:** `http://hadatac.org/ont/arrowhead/DA-202603251432-dasoc`

**Why?** This prevents `DA.save()` from accidentally deleting DA-SOC triples.

**Storage Method:** Triples are serialized to Turtle and POSTed via `GSPClient` to the `SPARQL_GRAPH` endpoint (`/store/data`).

---

### 4.6  Pipeline Flow Diagram

```
User Uploads: DA-SOC-LOCATION.csv
    ↓
Stage 1: File Detection
    IngestionWorker.getGeneratorChain()
    startsWith("DA-SOC-") → Route to AnnotateDASOC.exec(dataFile)
    ↓
Stage 2: SOC Resolution
    IngestionWorker.ingest() → findSOCByName("LOCATION")
    Query triplestore OR construct fallback URI
    Store in DataFile.dasocSOCUri
    ↓
Stage 3: DataFile Linkage
    Find or create DA record
    Link DA ↔ DataFile ↔ SOC
    ↓
Stage 4: Build ID Map
    AnnotateDASOC.buildOriginalIdMap()
    SPARQL: SELECT ?objUri ?originalId
    → Map<originalID, URI>
    ↓
Stage 5: CSV Processing
    AnnotateDASOC.processCSVFile()
    For each row: lookup → generate triples → batch write
    ↓
Stage 6: Triplestore POST
    saveModelToTriplestore()
    Serialize to Turtle → POST to named graph {daUri}-dasoc
    ↓
Result: DataFile status → PROCESSED
        Study objects enriched with extra properties
```

---

## 5. Validation Rules and Error Codes

DA-SOC ingestion uses the **Error Dictionary pattern** (`error_dictionary.json`) to standardize error reporting. All codes are prefixed with `DASOC_`.

### 5.1  Error Code Reference

| Code | Condition | Consequence | Severity |
|------|-----------|-------------|----------|
| `DASOC_00001` | StudyObjectCollection URI could not be resolved or does not exist | Ingestion fails — no objects to enrich | **FATAL** |
| `DASOC_00002` | SOC exists but contains zero StudyObject instances | Ingestion fails — no originalID map built | **FATAL** |
| `DASOC_00003` | `originalID` in CSV row does not match any object in SOC | Row skipped (warning logged) | **WARNING** |
| `DASOC_00004` | Object URI from triplestore is malformed (not `http://` or `https://`) | Object excluded from originalID map | **WARNING** |
| `DASOC_00005` | Duplicate `originalID` found in SOC | First occurrence kept, duplicates skipped | **WARNING** |
| `DASOC_00006` | CSV file has fewer than 2 columns | Ingestion fails — CSV structure invalid | **FATAL** |
| `DASOC_00007` | Exception thrown while processing a specific row | Row skipped (warning logged) | **WARNING** |
| `DASOC_00008` | URL decoding failed for DA URI or SOC URI | Ingestion fails — cannot proceed without valid URIs | **FATAL** |

---

### 5.2  Error Handling Strategy

**Fatal Errors (ingestion stops):**
- `DASOC_00001`, `DASOC_00002`, `DASOC_00006`, `DASOC_00008`
- These indicate structural problems that prevent any meaningful processing
- `IngestionResult.success = false`

**Warning Errors (row skipped, ingestion continues):**
- `DASOC_00003`, `DASOC_00004`, `DASOC_00005`, `DASOC_00007`
- The final `IngestionResult` includes counters for `skippedRows` and `errorRows`

**Example scenario:**
- CSV has 100 rows
- 5 rows have invalid `originalID` → 5 × `DASOC_00003` warnings
- 95 rows processed successfully
- **Final result:** `success = true`, `rowCount = 95`

---

## 6. Modeling Rules Derived from the Codebase

These rules are extracted directly from `AnnotateDASOC.java`, `DASOCGenerator.java`, and `IngestionWorker.java`.

### 6.1  DA-SOC Does Not Create New SOC Instances

The `originalID → objectURI` map is built entirely from existing triplestore data via `buildOriginalIdMap()`. If an `originalID` is not in the map, the row is skipped (`DASOC_00003`).

**There is no code path that creates new StudyObject entities during DA-SOC ingestion.**

**Consequence:** To add new objects to a SOC, you must re-ingest the DSG with additional rows.

---

### 6.2  DA-SOC Enriches Existing StudyObjects with Extra Triples

The CSV processing loop uses `model.add(subject, predicate, object)` to append triples. There is no `DELETE` operation or graph clearing before insertion.

Each batch is written via **Graph Store Protocol POST** (additive, does not overwrite).

⚠️  **If the same DA-SOC file is ingested twice, triples may be duplicated.** See [Open Questions Q2](#102-q2-duplicate-triple-handling).

---

### 6.3  Total Property Count = 5 + N + 1

**Formula:** `Total = 5 (base DSG fields) + N (DA-SOC columns 1..N) + 1 (timestamp)`

**Base Properties (from DSG):**
- `rdf:type`
- `hasco:isMemberOf`
- `hasco:originalID`
- `hasco:label`
- `rdfs:comment`

**DA-SOC Extra:** Each column 1..N becomes a predicate-object pair

**Timestamp:** `hasco:hasTimestamp` → ISO 8601 value added to each processed row

---

### 6.4  Multiple DA-SOCs per DSG Are Allowed

There is **no uniqueness constraint** enforced on DA-SOC file names or SOC URIs. The pipeline processes each file independently.

Each DA-SOC creates triples in a **separate named graph** (`{daUri}-dasoc`), preventing cross-contamination.

**Example:** Study `LTE-PIAGET-WEATHER-STATION` can have:
- `DA-SOC-LOCATION.csv` (geospatial properties)
- `DA-SOC-SENSOR.csv` (calibration data)

Both can be ingested independently.

---

### 6.5  Multiple DA-SOCs Targeting the Same SOC

⚠️  **NOT EXPLICITLY ENFORCED** — The codebase does not prevent multiple DA-SOC files from targeting the same SOC.

- No uniqueness checks on `dasocSOCUri`
- No locking mechanism exists
- Triples are **additive**

See [Open Questions Q4](#104-q4-multiple-da-soc-files-targeting-the-same-soc).

---

### 6.6  Named Graph Behavior

**Rule:** All DA-SOC triples are stored in a named graph with URI: `{daUri}-dasoc`

**Rationale:**
- Isolates DA-SOC triples from the main DA graph
- Prevents `DA.save()` from accidentally deleting them
- Allows selective graph deletion (`DROP GRAPH <{daUri}-dasoc>`) to undo enrichments without affecting base SOC instances

**SPARQL Example:**

```sparql
-- Retrieve enriched properties
SELECT ?obj ?altitude WHERE {
    GRAPH <http://hadatac.org/ont/arrowhead/DA-123456-dasoc> {
        ?obj pharma:altitude_m ?altitude .
    }
}
```

---

## 7. Example: LTE-PIAGET-WEATHER-STATION

### 7.1  Study Context

| Property | Value |
|----------|-------|
| **Study URI** | `http://hadatac.org/ont/arrowhead/LTE-PIAGET-WEATHER-STATION` |
| **Purpose** | Monitor environmental conditions across a university campus |
| **DSG File** | `DSG-LTE-PIAGET-WEATHER-STATION-50.xlsx` (already ingested, 50 SOC-LOCATION instances) |
| **SOC URI** | `http://hadatac.org/ont/arrowhead/LTE-PIAGET-LOCATION` |
| **SOC Type** | `hasco:SpaceCollection` |

**Problem:** The DSG captured basic location metadata (ID, label, comment) but did not capture geospatial details like altitude, floor number, or zone codes.

**Solution:** Use DA-SOC to add these properties without re-ingesting the entire DSG.

---

### 7.2  DA-SOC File: `DA-SOC-LOCATION.csv`

```csv
originalID,pharma:altitude_m,pharma:floor,pharma:zone_code
LIBRARY-L0,15.0,0,ZONE-A
SCIENCE-ROOF,25.5,4,ZONE-B
CAFETERIA-EAST,12.0,1,ZONE-A
ADMIN-BASEMENT,8.5,-1,ZONE-C
```

**Note:** `pharma:` prefix resolves to `http://hadatac.org/ont/pharma#` via the HADatAc namespace registry.

---

### 7.3  Ingestion Pipeline Execution (Step-by-Step Log)

```
[INGESTION] DataFile: DA-SOC-LOCATION.csv
[INGESTION] ✅ Matched DA-SOC-* pattern, routing to AnnotateDASOC
[INGESTION] Extracting SOC name: LOCATION
[INGESTION] findSOCByName(): Found SOC URI: .../SOC-LOCATION
[INGESTION] DataFile.dasocSOCUri set to: .../SOC-LOCATION

[DASOC] ✅ (1) Found related SOC: <.../SOC-LOCATION>
[DASOC] ✅ (2) Created originalID<->URI map with 50 elements
[DASOC] ✅ (3) Total rows in DA-SOC-LOCATION.csv: 4 (excluding header)
[DASOC] ✅ (4) Number of objects created: 4
[DASOC] Inserted 16 triples into graph <.../DA-202603251432-dasoc>
[DASOC] DataFile status set to PROCESSED
```

---

### 7.4  Resulting RDF Triples (for `LIBRARY-L0`)

**Base properties** — stored in main study named graph (from DSG):

```turtle
<.../LIBRARY-L0>
    rdf:type              hasco:StudyObject ;
    hasco:isMemberOf      <.../SOC-LOCATION> ;
    hasco:originalID      "LIBRARY-L0" ;
    hasco:label           "Library Level 0" ;
    rdfs:comment          "Main library floor" .
```

**DA-SOC enrichment** — stored in `DA-202603251432-dasoc` named graph:

```turtle
<.../LIBRARY-L0>
    pharma:altitude_m     "15.0" ;
    pharma:floor          "0" ;
    pharma:zone_code      "ZONE-A" ;
    hasco:hasTimestamp    "2026-03-25T14:32:15.123Z" .
```

**Total properties for `LIBRARY-L0`:** 5 (base) + 3 (DA-SOC) + 1 (timestamp) = **9 properties**

---

### 7.5  SPARQL Query to Retrieve Enriched Data

```sparql
PREFIX hasco: <http://hadatac.org/ont/hasco#>
PREFIX pharma: <http://hadatac.org/ont/pharma#>

SELECT ?location ?label ?altitude ?floor ?zone WHERE {
    ?location hasco:isMemberOf <.../SOC-LOCATION> ;
              hasco:label ?label .
    GRAPH <.../DA-202603251432-dasoc> {
        ?location pharma:altitude_m ?altitude ;
                  pharma:floor      ?floor ;
                  pharma:zone_code  ?zone .
    }
} ORDER BY ?label
```

**Results:**

| location | label | altitude | floor | zone |
|----------|-------|----------|-------|------|
| `.../ADMIN-BASEMENT` | Admin Building B1 | 8.5 | -1 | ZONE-C |
| `.../CAFETERIA-EAST` | Cafeteria East Wing | 12.0 | 1 | ZONE-A |
| `.../LIBRARY-L0` | Library Level 0 | 15.0 | 0 | ZONE-A |
| `.../SCIENCE-ROOF` | Science Building Roof | 25.5 | 4 | ZONE-B |

---

## 8. How to Use This Specification Going Forward

### 8.1  For Data Curators (Creating DA-SOC Files)

**Checklist before uploading:**

1. ✅ File name matches `DA-SOC-{SOCNAME}.csv`
2. ✅ Column 0 is `originalID`
3. ✅ All `originalID`s exist in the target SOC
4. ✅ At least 1 property column is present (columns 1..N)
5. ✅ Column headers are valid URIs or CURIEs

**Typical workflow:**

1. Query triplestore for `originalID`s in target SOC
2. Build CSV with extra properties
3. Upload via HADatAc UI
4. Check log for `DASOC_00003` warnings
5. Fix invalid rows and re-upload

---

### 8.2  For Backend Developers (Extending `AnnotateDASOC`)

**Reference sections:**
- **Section 4:** Pipeline flow, method calls, SPARQL queries
- **Section 5:** How to add new error codes
- **Section 6.6:** Named graph strategy

**Potential enhancements:**

1. **Add URI validation for column headers** — Modify `processCSVFile()` to reject headers that are not valid URIs or CURIEs
2. **Add duplicate row detection** — Hash each row and reject duplicates before processing
3. **Add property cardinality constraints** — Prevent multiple DA-SOCs from overwriting the same property

---

### 8.3  For Frontend Developers (Integrating DA-SOC Upload)

**Client-side validation rules:**
- File extension must be `.csv`
- File name must match `DA-SOC-*.csv`
- Column 0 must be named `originalID`

**Parse `IngestionResult` from Section 4.4:**
- `success`: boolean
- `rowCount`: number of successfully processed rows
- `errorMessage`: string (if `success = false`)

**Use Section 5 error codes** to generate user-friendly messages.

---

### 8.4  For Ontology Architects

**Key insights:**

1. **DA-SOC extends DSG-created instances** (Section 1 and 2.3)
2. **Columns become RDF predicates with no schema enforcement** (Section 2.2)
3. **Named graph strategy for provenance tracking** (Section 6.6)

**Considerations:**

- All DA-SOC properties are **optional** (no `sh:minCount` enforcement currently)
- All values are treated as **literals** unless they look like URIs
- **Multi-valued properties:** If a CSV has two rows with the same `originalID`, both add triples (no list/array handling currently)
- Consider adding **datatype hints**: `pharma:altitude_m^^xsd:float`

---

## 9. Ingestion Entry Points

### 9.1  IngestionWorker Entry Point

**DA-SOC ingestion always starts through `IngestionWorker.ingest()`**, which is called by the standard file upload/ingestion API.

**File detection logic:**

```java
// In IngestionWorker.getGeneratorChain()
String fileName = FilenameUtils.getBaseName(dataFile.getFilename());

if (fileName.startsWith("DA-SOC-")) {
    // ✅ DA-SOC file detected → Route to AnnotateDASOC
    dataFile.getLogger().println("Processing as DASOC (Data Acquisition - Study Object Collection)");
    chain = AnnotateDASOC.exec(dataFile);
    
} else if (fileName.startsWith("DA-")) {
    // ❌ General DA ingestion NOT SUPPORTED
    dataFile.getLogger().printException("ERROR: General DA ingestion is not supported. Only DA-SOC-* files can be ingested.");
    dataFile.setFileStatus(DataFile.UNPROCESSED);
    return null;
}
```

**Key behaviors:**

1. **DA-SOC files** (`DA-SOC-*.csv`) are **routed to `AnnotateDASOC.exec(dataFile)`**
2. **General DA files** (`DA-*.csv`) are **explicitly rejected** with error message
3. **SOC resolution** happens **before** `AnnotateDASOC` is called (in `IngestionWorker.ingest()`)

---

### 9.2  AnnotateDASOC Entry Points

**Two entry points exist:**

#### (A) `AnnotateDASOC.exec(DataFile dataFile)` — **IngestionWorker-compatible**

**Used by:** `IngestionWorker.getGeneratorChain()`

**Returns:** `GeneratorChain` (for pipeline integration)

**Behavior:**
1. Extracts `daUri` and `socUri` from `DataFile` properties
2. Validates parameters (marks chain invalid if missing)
3. Creates `DASOCGenerator` and adds to chain
4. Returns chain (processing happens when `chain.generate()` is called)

**Code:**

```java
public static GeneratorChain exec(DataFile dataFile) {
    GeneratorChain chain = new GeneratorChain();
    String daUri = dataFile.getDasocDataAcquisitionUri();
    String socUri = dataFile.getDasocSOCUri();
    
    // Validation...
    chain.addGenerator(new DASOCGenerator(dataFile, daUri, socUri, file));
    return chain;
}
```

---

#### (B) `AnnotateDASOC.exec(DataFile, File, String daUri, String socUri)` — **Legacy/API-compatible**

**Used by:** Direct API calls (not through `IngestionWorker`)

**Returns:** `IngestionResult` (immediate processing)

**Behavior:**
1. Accepts explicit parameters
2. Calls `processDASOC()` directly
3. Returns result synchronously

**Code:**

```java
public static IngestionResult exec(DataFile dataFile, File file, String daUri, String socUri) {
    return processDASOC(dataFile, file, daUri, socUri);
}
```

---

### 9.3  DASOCGenerator Role

**`DASOCGenerator` is a wrapper** that adapts `AnnotateDASOC` to the `GeneratorChain` pattern.

**Key methods:**

| Method | Behavior |
|--------|----------|
| `initMapping()` | No-op (DASOC doesn't use template mapping) |
| `createObject()` | Returns `null` (not used) |
| `createRow()` | Returns `null` (not used) |
| `createRows()` | **Delegates to `AnnotateDASOC.processDASOC()`** |
| `createObjects()` | Skipped (handled internally by `processDASOC()`) |

**Why it exists:**
- Allows DA-SOC to flow through `GeneratorChain.generate()` like other data types
- Maintains consistency with the existing ingestion architecture

---

### 9.4  Routing Summary

```
File Upload → IngestionWorker.ingest()
    ↓
    File name check: DA-SOC-*.csv?
    ↓ YES
    IngestionWorker.findSOCByName() → Store socUri in DataFile
    ↓
    IngestionWorker.getGeneratorChain()
    ↓
    Detect DA-SOC-* → Call AnnotateDASOC.exec(dataFile)
    ↓
    Create DASOCGenerator → Add to GeneratorChain
    ↓
    GeneratorChain.generate() → DASOCGenerator.createRows()
    ↓
    Delegate to AnnotateDASOC.processDASOC()
    ↓
    Build originalID map → Process CSV → Save triples
    ↓
    DataFile status → PROCESSED
```

---

## 10. Open Questions

The following ambiguities were identified during codebase inspection and require clarification before this specification is considered fully canonical.

### 10.1  Q1: DA URI Source and Construction ✅ **RESOLVED**

**Question:** Where does the `daUri` parameter come from in `AnnotateDASOC.exec(DataFile)`?

**Answer (from code inspection):**

The `daUri` is extracted from `DataFile.getDasocDataAcquisitionUri()`. The exact construction logic is:

1. **If DA already exists:** Retrieved via `DA.find(daUri)`
2. **If DA does not exist:** Created by `AnnotateDASOC.processDASOC()`:
   ```java
   da = new DA();
   da.setUri(daUri); // from DataFile
   da.setLabel(dataFile.getFilename());
   // ... set other properties
   da.save();
   ```

**Status:** ✅ Documented in Section 4.3

---

### 10.2  Q2: Duplicate Triple Handling

**Question:** If the same DA-SOC file is ingested twice, are triples deduplicated or duplicated?

**Observation:** The code uses `model.add()` (append) and the GSP POST operation does not check for existing triples.

**Test required:** Ingest file twice and count triples in named graph.

⚠️  **If Fuseki does not deduplicate, users must be warned against re-ingesting the same file.**

**Status:** ⚠️ **UNRESOLVED** — Requires empirical testing

---

### 10.3  Q3: Empty SOC Name Handling ✅ **RESOLVED**

**Question:** A file named `DA-SOC-.csv` produces an empty `socName` string. What happens?

**Answer (from code inspection):**

```java
String socName = baseName.substring(7); // "DA-SOC-" → ""
String socUri = findSOCByName("");
```

`findSOCByName("")` would query for SOCs with empty labels, and the fallback URI construction (`kbPrefix + "SOC-"`) produces an invalid URI like `http://hadatac.org/ont/arrowhead/SOC-`.

**Status:** ⚠️ **CONFIRMED** — No defensive check exists. **Recommendation:** Add validation to reject files where `socName.isEmpty()`.

---

### 10.4  Q4: Multiple DA-SOC Files Targeting the Same SOC

**Question:** Is it allowed to upload multiple DA-SOC files targeting the same SOC (e.g., `DA-SOC-LOCATION-PART1.csv` and `DA-SOC-LOCATION-PART2.csv`)?

**Observation:** Both would resolve to the same SOC URI and add triples to the same objects. No conflict detection or merging logic exists.

⚠️  **Business rule must be clarified:**
- **If allowed:** Document that properties are additive
- **If not:** Add uniqueness validation

**Status:** ⚠️ **UNRESOLVED** — Requires business decision

---

### 10.5  Q5: REST API Endpoint for DA-SOC Upload

**Question:** What is the exact REST API endpoint for DA-SOC file upload?

**Observation:** No DA-SOC-specific routes were confirmed in `conf/routes`. DA-SOC likely uses the standard file upload endpoint.

**Required:** Document the exact endpoint, HTTP method, and required parameters for frontend and API consumers.

**Status:** ⚠️ **UNRESOLVED** — Requires routes file inspection

---

### 10.6  Q6: Column Header URI Validation

**Question:** Are column headers (columns 1..N) validated as valid URIs or CURIEs before processing?

**Observation:** A header like `"invalid header with spaces"` would create a malformed predicate in the triplestore.

**Recommendation:** Add validation to reject non-URI/CURIE headers, using a new error code `DASOC_00009`.

**Status:** ⚠️ **NOT IMPLEMENTED** — Enhancement recommended

---

## Appendix: Glossary

| Term | Definition |
|------|------------|
| **DA** | Data Acquisition — a record linking uploaded files to studies and processing workflows. |
| **DA-SOC** | Data Acquisition — Study Object Collection — a CSV file that enriches existing SOC instances. |
| **DSG** | Data Study Generation — an Excel workbook defining study structure and creating SOC instances. |
| **SOC** | StudyObjectCollection — a named set of StudyObject instances (e.g., locations, sensors). |
| **originalID** | A unique identifier for a StudyObject within its SOC (stored in `hasco:originalID`). |
| **CURIE** | Compact URI — a prefixed URI like `pharma:altitude_m` (expands to full URI via namespace registry). |
| **Named Graph** | A URI-identified subset of triples in the triplestore (RDF quad: subject, predicate, object, graph). |
| **GSP** | Graph Store Protocol — RESTful API for adding/removing RDF graphs (used by `GSPClient`). |
| **Triplestore** | Database storing RDF triples. HADatAc uses Apache Jena Fuseki. |

---

## Document Metadata

**Version:** 1.1 (2026-03-30) — Updated based on actual code implementation in Kaykael branch  
**Previous Version:** v1.0 (2026-03-25) — Initial release based on preliminary codebase inspection  
**Maintainer:** HADatAc Development Team  
**Contact:** GitHub Issues

---

**End of DA-SOC Specification v1.1**

