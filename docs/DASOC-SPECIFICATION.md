# DA-SOC SPECIFICATION — CONSOLIDATED UNDERSTANDING

**Version:** 1.0  
**Date:** 2026-03-25  
**Status:** CANONICAL REFERENCE  
**Supersedes:** All previous informal DA-SOC documentation

---

## 1. What a DA-SOC Is

**DA-SOC** stands for **Data Acquisition — Study Object Collection**.

A DA-SOC is a **CSV file** that enriches existing StudyObject instances within a StudyObjectCollection (SOC) by adding **extra scope properties** that were not captured during the original DSG ingestion. Unlike DSG files (which create new SOC instances), DA-SOC files **extend** existing instances with additional properties that do not fit within the DSG schema constraints.

### Key Characteristics

1. **CSV Format Only** — DA-SOC files are always CSV (comma-separated values), not Excel workbooks. This constraint simplifies parsing and ensures a flat, tabular structure where:
   - Column 0 is always `originalID` (the lookup key)
   - Columns 1..N are dynamic property URIs
   - Each row corresponds to exactly one existing StudyObject instance

2. **Extends Existing Instances** — A DA-SOC **cannot create new StudyObject instances**. It can only add properties to objects that already exist in the triplestore (ingested via a prior DSG file).

3. **Referential Integrity** — Every `originalID` in a DA-SOC CSV **must** match an existing StudyObject in the target SOC. If a row references a non-existent `originalID`, the row is skipped with a warning (`DASOC_00003`).

4. **File Name Drives SOC Resolution** — The SOC URI is resolved from the file name using the pattern `DA-SOC-{SOCNAME}.csv`, where `{SOCNAME}` is used to query the triplestore for the corresponding StudyObjectCollection URI.

5. **Belongs to Exactly One DSG** — A DA-SOC is always scoped to a single Data Study Generation (DSG) context. It cannot be shared across multiple DSGs. The parent DSG must have already created the SOC instances that the DA-SOC extends.

6. **DataFile Linkage Required** — At upload time, the DA-SOC CSV is external to the HADatAc system. The ingestion pipeline must resolve two critical URIs and store them in the `DataFile` entity:
   - `dasocDataAcquisitionUri` — The Data Acquisition URI (created or reused)
   - `dasocSOCUri` — The StudyObjectCollection URI (resolved from file name)

---

## 2. Global Architectural Principles

### 2.1 Mandatory First Column: `originalID`

**Rule:** Column index 0 **must always** be the `originalID` column.

**Rationale:**
- The `originalID` is the primary lookup key used to match CSV rows to existing StudyObject instances in the triplestore.
- Without a valid `originalID`, the system cannot determine which object to enrich.
- This constraint is **hard-coded** in `AnnotateDASOC.processCSVFile()`:
  ```java
  String originalIdColumn = headers.get(0); // First column is originalID
  ```

**Validation:**
- If the CSV has fewer than 2 columns (no property columns after `originalID`), ingestion fails with error `DASOC_00006`.
- Empty `originalID` values are silently skipped (the row is not processed).

**Consequence of Violation:**
- If `originalID` is missing or placed in a non-zero column, **all rows will fail** to match, resulting in mass `DASOC_00003` warnings and zero successful ingestions.

---

### 2.2 Extra Columns Are Unbounded Scope Extensions

**Rule:** Columns 1..N are **dynamic property URIs** with no upper limit on count.

**Rationale:**
- DA-SOC is designed to handle arbitrary "scope extensions" — additional properties that were not known or captured during DSG ingestion.
- Each column header (from index 1 onward) is interpreted as an **RDF predicate URI** (or a CURIE that will be expanded).
- The system does not pre-validate these URIs against any schema or ontology. The backend assumes the user knows what properties they want to add.

**Processing Logic (from `AnnotateDASOC.processCSVFile()`):**
```java
for (int i = 1; i < headers.size(); i++) {
    String predicateUri = headers.get(i);
    String value = record.get(i).trim();
    
    if (value.isEmpty()) {
        continue; // Skip empty values
    }
    
    // Expand predicate URI if it uses prefixes
    predicateUri = URIUtils.replacePrefixEx(predicateUri);
    
    Property predicate = model.createProperty(predicateUri);
    
    // Determine if value is a URI or literal
    if (URIUtils.isValidURI(value)) {
        Resource object = model.createResource(value);
        model.add(subject, predicate, object);
    } else {
        model.add(subject, predicate, value);
    }
}
```

**Key Observations:**
- **Prefix Expansion:** Column headers like `pharma:altitude_m` are expanded to full URIs using `URIUtils.replacePrefixEx()`.
- **Type Detection:** If a cell value looks like a URI (passes `URIUtils.isValidURI()`), it becomes an RDF resource. Otherwise, it becomes a literal.
- **Empty Cells Skipped:** Empty cells do not generate triples (no null/empty literal pollution).

**Example:**
Given a CSV with columns:
```
originalID, pharma:altitude_m, pharma:floor, pharma:zone_code
```
Each row generates triples of the form:
```turtle
<http://.../LIBRARY-L0> pharma:altitude_m "15.0" .
<http://.../LIBRARY-L0> pharma:floor "0" .
<http://.../LIBRARY-L0> pharma:zone_code "ZONE-A" .
```

---

### 2.3 All `originalID` Values Must Exist in the Parent DSG

**Rule:** A DA-SOC row is **rejected** if its `originalID` does not match an existing StudyObject in the target SOC.

**Rationale:**
- DA-SOC is not a SOC creation mechanism. It is a **post-DSG enrichment** mechanism.
- If an `originalID` does not exist, the system cannot determine which RDF resource to extend.

**Enforcement (from `AnnotateDASOC.buildOriginalIdMap()`):**
1. When processing starts, the system queries the triplestore for **all StudyObject instances** in the target SOC:
   ```sparql
   SELECT ?objUri ?originalId WHERE {
       ?objUri hasco:isMemberOf <{socUri}> .
       ?objUri hasco:originalID ?originalId .
   }
   ```
2. This builds a **lookup map**: `originalID → objectURI`.
3. During CSV processing, if `originalIdToUriMap.get(originalId)` returns `null`, the row is skipped with warning `DASOC_00003`.

**Error Code:**
- `DASOC_00003`: *"Original ID '%s' not found in SOC at row %d"*

**Consequence:**
- Rows with invalid `originalID` values **do not fail ingestion** — they are simply **skipped** with a warning logged to `DataFile.logger`.
- This is a **soft error** (ingestion continues for valid rows).

---

### 2.4 File Name Drives SOC Resolution

**Rule:** The file name **must** follow the pattern `DA-SOC-{SOCNAME}.csv`, where `{SOCNAME}` is used to resolve the SOC URI.

**Rationale:**
- DA-SOC files are external to HADatAc at upload time. The system needs a convention to automatically determine which SOC to extend.
- The file name embeds the SOC identity, eliminating the need for users to manually provide the SOC URI.

**Resolution Logic (from `IngestionWorker.ingest()`):**
```java
if (FilenameUtils.getBaseName(fileName).startsWith("DA-SOC-")) {
    String baseName = FilenameUtils.getBaseName(fileName);
    String socName = baseName.substring(7); // "DA-SOC-ENTERPRISE" -> "ENTERPRISE"
    
    // Try to find SOC by querying the triplestore
    String socUri = findSOCByName(socName);
    if (socUri != null && !socUri.isEmpty()) {
        dataFile.setDasocSOCUri(socUri);
    } else {
        // If not found, construct expected SOC URI using naming convention
        String kbPrefix = ConfigProp.getKbPrefix();
        socUri = kbPrefix + "SOC-" + socName;
        dataFile.setDasocSOCUri(socUri);
    }
    dataFile.save();
}
```

**SPARQL Query Used (from `IngestionWorker.findSOCByName()`):**
```sparql
SELECT ?socUri WHERE {
    ?socUri a hasco:StudyObjectCollection .
    { ?socUri rdfs:label ?label . FILTER(CONTAINS(UCASE(?label), UCASE("{socName}"))) }
    UNION
    { FILTER(CONTAINS(UCASE(STR(?socUri)), UCASE("SOC-{socName}"))) }
} LIMIT 1
```

**Fallback Behavior:**
- If the SPARQL query returns no results, the system **constructs** a SOC URI using the knowledge base prefix:
  ```
  {kbPrefix} + "SOC-" + {socName}
  ```
  Example: `http://hadatac.org/ont/arrowhead/SOC-LOCATION`

**Examples:**
| File Name                | Extracted SOC Name | Expected SOC URI (if found)                              |
|--------------------------|--------------------|---------------------------------------------------------|
| `DA-SOC-LOCATION.csv`    | `LOCATION`         | `http://hadatac.org/.../SOC-LOCATION`                   |
| `DA-SOC-ENTERPRISE.csv`  | `ENTERPRISE`       | `http://hadatac.org/.../SOC-ENTERPRISE`                 |
| `DA-SOC-API.csv`         | `API`              | `http://hadatac.org/.../SOC-API`                        |

**Edge Case:**
- If a file is named `DA-SOC-.csv` (empty SOC name), `socName` becomes an empty string, and fallback resolution will produce an invalid URI. ⚠️ **NOT EXPLICITLY VALIDATED** — requires defensive check.

---

### 2.5 One DA-SOC Belongs to Exactly One DSG

**Rule:** A DA-SOC file is scoped to a **single DSG context**. It cannot enrich SOC instances from multiple DSGs simultaneously.

**Rationale:**
- DA-SOC extends SOC instances created by a specific DSG file.
- SOC instances are partitioned by study/DSG context. A DA-SOC must respect this boundary.

**Enforcement:**
- The SOC URI resolved from the file name is **study-scoped**. When the system queries for SOC objects (`hasco:isMemberOf <socUri>`), it retrieves objects from a single study context.
- There is **no cross-DSG contamination** because each DSG creates SOC instances with unique URIs tied to its study.

**Cardinality:**
- **1 DA-SOC → 1 SOC → 1 DSG**
- ⚠️ **Multiple DA-SOCs per DSG are allowed** (see Section 6.4), but each DA-SOC targets exactly one SOC within that DSG.

---

## 3. DA-SOC File Structure

### 3.1 File Naming Convention

**Pattern:**
```
DA-SOC-{SOCNAME}.csv
```

**Components:**
- `DA-SOC-` — **Required prefix** that triggers DA-SOC ingestion routing.
- `{SOCNAME}` — **SOC identifier** extracted and used to resolve the SOC URI.
- `.csv` — **Required extension** (Excel workbooks are not supported).

**Examples:**
- `DA-SOC-LOCATION.csv` → SOC name: `LOCATION`
- `DA-SOC-ENTERPRISE.csv` → SOC name: `ENTERPRISE`
- `DA-SOC-API.csv` → SOC name: `API`

**Invalid Names:**
- `DASOC-LOCATION.csv` — Missing hyphen after `DA`, will not trigger DA-SOC routing.
- `DA-SOC-LOCATION.xlsx` — Excel format, will fail at CSV parsing stage.
- `DA-LOCATION.csv` — Missing `SOC` component, will be rejected as general DA (not supported).

---

### 3.2 Column Structure

**Header Row:** **Required** — the first row is always treated as column headers.

**Column Layout:**

| Column Index | Name / Purpose       | Required | Description                                                                 |
|--------------|----------------------|----------|-----------------------------------------------------------------------------|
| 0            | `originalID`         | ✅ Yes    | Lookup key to match existing StudyObject instances in the SOC               |
| 1..N         | Property URIs        | ✅ Yes    | Dynamic property columns (RDF predicates). Headers are URIs or CURIEs.      |

**Minimum Column Count:** **2** (Column 0 + at least 1 property column)
- Validation: If `headers.size() < 2`, ingestion fails with error `DASOC_00006`.

---

### 3.3 Data Types

**Column 0 (`originalID`):**
- **Type:** String literal
- **Constraint:** Must match `hasco:originalID` property of an existing StudyObject in the target SOC.
- **Empty Values:** Silently skipped (row not processed).

**Columns 1..N (Property Values):**
- **Type:** Automatically detected:
  - **URI** — If `URIUtils.isValidURI(value)` returns `true`, the value becomes an RDF resource.
  - **Literal** — Otherwise, the value becomes an RDF literal (string).
- **Empty Values:** Silently skipped (no triple generated for that cell).

**Encoding:** UTF-8 (standard for CSV files in HADatAc ecosystem).

---

### 3.4 Example: LTE-PIAGET-WEATHER-STATION Study

**Study Context:**
- Study URI: `http://hadatac.org/ont/arrowhead/LTE-PIAGET-WEATHER-STATION`
- SOC: `SOC-LOCATION` (50 instances: weather station locations)
- DSG File: `DSG-LTE-PIAGET-LOCATION.xlsx` (already ingested, created 50 StudyObject instances)

**DA-SOC File: `DA-SOC-LOCATION.csv`**

| originalID       | pharma:altitude_m | pharma:floor | pharma:zone_code |
|------------------|-------------------|--------------|------------------|
| LIBRARY-L0       | 15.0              | 0            | ZONE-A           |
| SCIENCE-ROOF     | 25.5              | 4            | ZONE-B           |
| CAFETERIA-EAST   | 12.0              | 1            | ZONE-A           |
| ADMIN-BASEMENT   | 8.5               | -1           | ZONE-C           |

**Interpretation:**
- **4 rows** (excluding header) will be processed.
- **3 extra properties** per object: `pharma:altitude_m`, `pharma:floor`, `pharma:zone_code`.
- Each row looks up the corresponding StudyObject by `originalID` and adds 3 triples to it.

**Resulting Triples (for row 1):**
```turtle
<http://hadatac.org/ont/arrowhead/LIBRARY-L0> pharma:altitude_m "15.0" .
<http://hadatac.org/ont/arrowhead/LIBRARY-L0> pharma:floor "0" .
<http://hadatac.org/ont/arrowhead/LIBRARY-L0> pharma:zone_code "ZONE-A" .
<http://hadatac.org/ont/arrowhead/LIBRARY-L0> hasco:hasTimestamp "2026-03-25T14:32:15.123Z" .
```

**Total Triples Generated:**
- 4 rows × (3 properties + 1 timestamp) = **16 triples**

---

## 4. Ingestion Pipeline

DA-SOC ingestion flows through a **6-stage pipeline** orchestrated by `IngestionWorker`, `AnnotateDASOC`, and `DASOCGenerator`. Each stage has specific responsibilities and failure modes.

---

### 4.1 File Detection (IngestionWorker)

**Location:** `IngestionWorker.getGeneratorChain()`

**Trigger:** File name starts with `DA-SOC-`

**Logic:**
```java
String fileName = FilenameUtils.getBaseName(dataFile.getFilename());

if (fileName.startsWith("DA-SOC-")) {
    System.out.println("[INGESTION PATH] ✅ Matched DA-SOC-* pattern, routing to AnnotateDASOC");
    dataFile.getLogger().println("Processing as DASOC (Data Acquisition - Study Object Collection)");
    chain = AnnotateDASOC.exec(dataFile);
} else if (fileName.startsWith("DA-")) {
    // REJECT: General DA ingestion is not working at this time
    System.out.println("IngestionWorker: ERROR - General DA ingestion not supported");
    dataFile.getLogger().printException("ERROR: General DA ingestion is not supported. Only DA-SOC-* files can be ingested.");
    dataFile.setFileStatus(DataFile.UNPROCESSED);
    dataFile.save();
    return null;
}
```

**Key Decision:**
- DA-SOC files are **separated from general DA files** at this stage.
- General DA files (e.g., `DA-MEASUREMENTS.csv`) are **explicitly rejected** with an error message.
- Only `DA-SOC-*` files are routed to `AnnotateDASOC`.

**Failure Mode:**
- If the file is named `DA-LOCATION.csv` (missing `SOC`), it will be rejected as unsupported general DA.

---

### 4.2 SOC Resolution (IngestionWorker.findSOCByName)

**Location:** `IngestionWorker.ingest()`

**Timing:** Before routing to `AnnotateDASOC`, during `DataFile` setup.

**Steps:**
1. **Extract SOC Name from File Name:**
   ```java
   String baseName = FilenameUtils.getBaseName(fileName); // "DA-SOC-LOCATION.csv" -> "DA-SOC-LOCATION"
   String socName = baseName.substring(7); // "DA-SOC-LOCATION" -> "LOCATION"
   ```

2. **Query Triplestore for Matching SOC:**
   ```sparql
   SELECT ?socUri WHERE {
       ?socUri a hasco:StudyObjectCollection .
       { ?socUri rdfs:label ?label . FILTER(CONTAINS(UCASE(?label), UCASE("LOCATION"))) }
       UNION
       { FILTER(CONTAINS(UCASE(STR(?socUri)), UCASE("SOC-LOCATION"))) }
   } LIMIT 1
   ```

3. **Fallback to Constructed URI:**
   ```java
   if (socUri == null || socUri.isEmpty()) {
       String kbPrefix = ConfigProp.getKbPrefix(); // e.g., "http://hadatac.org/ont/arrowhead/"
       socUri = kbPrefix + "SOC-" + socName;       // "http://hadatac.org/ont/arrowhead/SOC-LOCATION"
       dataFile.setDasocSOCUri(socUri);
       dataFile.getLogger().println("⚠️ SOC not found in triplestore, using convention-based URI: " + socUri);
   }
   ```

4. **Store SOC URI in DataFile:**
   ```java
   dataFile.setDasocSOCUri(socUri);
   dataFile.save();
   ```

**Logging:**
- **Found in triplestore:** `"✅ Auto-detected SOC URI: <uri>"`
- **Constructed fallback:** `"⚠️ SOC not found in triplestore, using convention-based URI: <uri>"`
- **Total failure:** `"[WARNING] IngestionWorker: Could not determine SOC URI for {socName}"`

**Failure Mode:**
- If `kbPrefix` is `null` or empty, and the SOC is not found in the triplestore, the SOC URI will be incomplete → ingestion will fail at validation stage with `DASOC_00001`.

---

### 4.3 DataFile Linkage

**Location:** `IngestionWorker.ingest()` (SOC linkage), `AnnotateDASOC.processDASOC()` (DA linkage)

**Purpose:** Establish bidirectional traceability between the uploaded CSV file, the Data Acquisition record, and the target SOC.

**Fields Set on DataFile:**

| Field                      | Set By                    | Value Source                                      |
|----------------------------|---------------------------|---------------------------------------------------|
| `dasocSOCUri`              | `IngestionWorker.ingest()` | Resolved from file name via `findSOCByName()`     |
| `dasocDataAcquisitionUri`  | `AnnotateDASOC` (implicit) | Constructed as `{kbPrefix}DA-{timestamp}` or user-provided |

**DA URI Resolution (from `AnnotateDASOC.processDASOC()`):**
```java
DA da = DA.find(daUri);
if (da == null) {
    dataFile.getLogger().println(String.format("DA <%s> not found, creating new DA record", daUri));
    
    da = new DA();
    da.setUri(daUri);
    da.setLabel(dataFile.getFilename());
    da.setComment("Data Acquisition for " + dataFile.getFilename());
    da.setHascoTypeUri("http://hadatac.org/ont/hasco/DataAcquisition");
    da.setTypeUri("http://hadatac.org/ont/hasco/DataAcquisition");
    da.setHasSIRManagerEmail(dataFile.getHasSIRManagerEmail());
    da.setHasDataFileUri(dataFile.getUri());
    da.setHasStatus("UNPROCESSED");
    
    da.save();
} else {
    dataFile.getLogger().println(String.format("Found existing DA: <%s>", daUri));
}
```

**Key Observation:**
- If a DA record with the same URI already exists, it is **reused** (not overwritten).
- If the DA does not exist, a **new DA record is created** with minimal metadata.
- ⚠️ **NOT CONFIRMED:** The exact source of `daUri` in the current pipeline. It appears to be constructed implicitly or passed from upstream. Requires clarification.

**Traceability Chain:**
```
DataFile.uri
    ↓
DataFile.dasocDataAcquisitionUri → DA.uri → DA.hasDataFileUri → DataFile.uri (circular reference)
    ↓
DataFile.dasocSOCUri → SOC.uri → StudyObject instances (via hasco:isMemberOf)
```

---

### 4.4 Annotation (AnnotateDASOC)

**Location:** `AnnotateDASOC.exec(DataFile)` → `AnnotateDASOC.processDASOC()`

**Purpose:** Parse the CSV file, validate rows, and generate RDF triples for each valid row.

**Steps:**

#### Step 4.4.1: Build originalID → Object URI Map

**Method:** `AnnotateDASOC.buildOriginalIdMap(socUri, dataFile)`

**SPARQL Query:**
```sparql
SELECT ?objUri ?originalId WHERE {
    ?objUri hasco:isMemberOf <{socUri}> .
    ?objUri hasco:originalID ?originalId .
}
```

**Map Construction:**
```java
Map<String, String> map = new HashMap<>();
while (results.hasNext()) {
    QuerySolution soln = results.next();
    String objUri = soln.get("objUri").toString();
    String originalId = soln.get("originalId").toString();
    
    // Validate URI format
    if (!objUri.startsWith("http://") && !objUri.startsWith("https://")) {
        dataFile.getLogger().printWarningByIdWithArgs("DASOC_00004", objUri);
        continue;
    }
    
    // Check for duplicate originalIDs
    if (map.containsKey(originalId)) {
        dataFile.getLogger().printWarningByIdWithArgs("DASOC_00005", originalId);
        continue;
    }
    
    map.put(originalId, objUri);
}
```

**Validation:**
- **Empty Map:** If `map.isEmpty()`, ingestion fails with error `DASOC_00002` (*"No objects found in StudyObjectCollection"*).
- **Invalid URIs:** Objects with malformed URIs are skipped with warning `DASOC_00004`.
- **Duplicate originalIDs:** Only the first occurrence is kept; duplicates trigger warning `DASOC_00005`.

**Logging:**
```
✅ (1) Found related SOC: <{socUri}>
✅ (2) Created originalID<->URI map with {N} elements
```

---

#### Step 4.4.2: Parse CSV File

**Method:** `AnnotateDASOC.processCSVFile(file, dataFile, daUri, originalIdToUriMap)`

**CSV Parser Configuration:**
```java
CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
    .withFirstRecordAsHeader()         // Row 0 becomes headers
    .withIgnoreHeaderCase()            // Case-insensitive headers
    .withTrim()                        // Trim whitespace
    .withAllowMissingColumnNames(true) // Handle unnamed columns gracefully
);
```

**Header Validation:**
```java
Map<String, Integer> headerMap = csvParser.getHeaderMap();
List<String> headers = new ArrayList<>(headerMap.keySet());

if (headers.isEmpty() || headers.size() < 2) {
    dataFile.getLogger().printExceptionById("DASOC_00006");
    throw new Exception(ErrorDictionary.getInstance().getTable().get("DASOC_00006").getDetail());
}
```

**Row Processing Loop:**
```java
for (CSVRecord record : csvParser) {
    totalCSVRows++;
    
    try {
        String originalId = record.get(0).trim(); // Column 0
        
        if (originalId.isEmpty()) {
            skippedRows++;
            continue; // Skip empty originalID
        }
        
        // Look up object URI
        String objectUri = originalIdToUriMap.get(originalId);
        if (objectUri == null) {
            dataFile.getLogger().printWarningByIdWithArgs("DASOC_00003", originalId, record.getRecordNumber());
            skippedRows++;
            continue;
        }
        
        // Create RDF resource
        Resource subject = model.createResource(objectUri);
        
        // Add properties from columns 1..N
        for (int i = 1; i < headers.size(); i++) {
            String predicateUri = headers.get(i);
            String value = record.get(i).trim();
            
            if (value.isEmpty()) {
                continue; // Skip empty values
            }
            
            predicateUri = URIUtils.replacePrefixEx(predicateUri);
            Property predicate = model.createProperty(predicateUri);
            
            if (URIUtils.isValidURI(value)) {
                Resource object = model.createResource(value);
                model.add(subject, predicate, object);
            } else {
                model.add(subject, predicate, value);
            }
        }
        
        // Add timestamp
        Property timestampProp = model.createProperty(TIMESTAMP_PREDICATE);
        model.add(subject, timestampProp, timestamp);
        
        totalRows++;
        objectsCreated++;
        currentBatchCount++;
        
        // Save batch every 10,000 rows
        if (currentBatchCount >= BATCH_SIZE) {
            saveModelToTriplestore(model, daUri + "-dasoc", dataFile);
            model = ModelFactory.createDefaultModel();
            addNamespacePrefixes(model);
            currentBatchCount = 0;
        }
        
    } catch (Exception e) {
        errorRows++;
        dataFile.getLogger().printWarningByIdWithArgs("DASOC_00007", record.getRecordNumber(), e.getMessage());
    }
}

// Save remaining records
if (currentBatchCount > 0) {
    saveModelToTriplestore(model, daUri + "-dasoc", dataFile);
}
```

**Counters Tracked:**
- `totalCSVRows` — Total data rows in CSV (excluding header)
- `objectsCreated` — Successfully processed rows (triples generated)
- `skippedRows` — Rows with empty or invalid `originalID`
- `errorRows` — Rows that threw exceptions during processing

**Logging:**
```
✅ (3) Total rows in DA-SOC-{NAME}.csv: {totalCSVRows} (excluding header)
✅ (4) Number of objects created: {objectsCreated}
Rows skipped (empty/invalid originalID): {skippedRows}
Rows with errors: {errorRows}
```

---

#### Step 4.4.3: Property Count Formula

**Base SOC Fields:** 5 (fixed properties from DSG ingestion)
- `rdf:type`
- `hasco:isMemberOf`
- `hasco:originalID`
- `hasco:label`
- `rdfs:comment`

**DA-SOC Extra Properties:** N (dynamic, from CSV columns 1..N)

**Timestamp:** 1 (`hasco:hasTimestamp`)

**Total Properties per Object:**
```
Total = 5 (base) + N (extra columns) + 1 (timestamp) = 6 + N
```

**Example:**
For the `DA-SOC-LOCATION.csv` with 3 extra columns:
```
Total = 5 + 3 + 1 = 9 properties per object
```

---

### 4.5 Generation (DASOCGenerator)

**Location:** `DASOCGenerator.createRows()`

**Purpose:** Wrap `AnnotateDASOC.processDASOC()` in a `BaseGenerator`-compatible interface so DA-SOC can flow through `GeneratorChain.generate()`.

**Delegation Pattern:**
```java
@Override
public void createRows() throws Exception {
    System.out.println("\n=== [INGESTION PATH] DASOCGenerator.createRows() ===");
    
    // Delegate to AnnotateDASOC processing logic
    AnnotateDASOC.IngestionResult result = AnnotateDASOC.processDASOC(dataFile, csvFile, daUri, socUri);
    
    if (!result.isSuccess()) {
        String errorMsg = "DASOC processing failed: " + result.getErrorMessage();
        dataFile.getLogger().printException(errorMsg);
        throw new Exception(errorMsg);
    }
    
    dataFile.getLogger().println(String.format("✅ DASOC processing completed: %d rows processed", result.getRowCount()));
}
```

**Override Behavior:**
- `initMapping()` — No-op (DA-SOC does not use template mapping)
- `createObject()` — Returns `null` (DA-SOC does not create individual objects per row)
- `createRow()` — Returns `null` (DA-SOC uses batch processing)
- `createObjects()` — Skipped (all object creation handled in `processDASOC()`)

**Batch Size:** **10,000 rows** (`BATCH_SIZE` constant in `AnnotateDASOC`)

**Named Graph:**
- All DA-SOC triples are stored in a **separate named graph**: `{daUri}-dasoc`
- Example: If `daUri = http://hadatac.org/ont/arrowhead/DA-123456`, triples go to:
  ```
  http://hadatac.org/ont/arrowhead/DA-123456-dasoc
  ```
- **Rationale:** Prevents `DA.save()` from deleting DA-SOC triples (DA entity management operates on the main DA graph, not the `-dasoc` suffix graph).

**Triplestore Insertion:**
```java
private static void saveModelToTriplestore(Model model, String namedGraphUri, DataFile dataFile) {
    // Serialize Jena model to Turtle format
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    RDFDataMgr.write(baos, model, RDFFormat.TURTLE);
    byte[] modelBytes = baos.toByteArray();
    
    // Use GSPClient to POST to named graph
    String gspEndpoint = CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_GRAPH);
    GSPClient gspClient = new GSPClient(gspEndpoint);
    
    gspClient.postInputStream(
        () -> new ByteArrayInputStream(modelBytes),
        "text/turtle",
        namedGraphUri
    );
    
    dataFile.getLogger().println(String.format("Inserted %d triples into graph <%s>", model.size(), namedGraphUri));
}
```

**Endpoint Used:** `SPARQL_GRAPH` (Graph Store Protocol endpoint: `/store/data`), **not** `SPARQL_QUERY`.

---

### 4.6 Pipeline Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│ User Uploads: DA-SOC-LOCATION.csv                                   │
└─────────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────────┐
│ Stage 1: File Detection (IngestionWorker.getGeneratorChain)         │
│  • Check: fileName.startsWith("DA-SOC-") ?                          │
│  • YES → Route to AnnotateDASOC                                     │
│  • NO  → Reject as unsupported DA                                   │
└─────────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────────┐
│ Stage 2: SOC Resolution (IngestionWorker.ingest)                    │
│  • Extract socName from file name: "LOCATION"                       │
│  • Query triplestore: findSOCByName("LOCATION")                     │
│  • Fallback: Construct URI if not found                             │
│  • Store in DataFile.dasocSOCUri                                    │
└─────────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────────┐
│ Stage 3: DataFile Linkage (AnnotateDASOC.processDASOC)              │
│  • Find or create DA record                                         │
│  • Link DA ↔ DataFile ↔ SOC                                        │
└─────────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────────┐
│ Stage 4: Build originalID Map (buildOriginalIdMap)                  │
│  • Query: SELECT ?objUri ?originalId WHERE {                        │
│      ?objUri hasco:isMemberOf <socUri> .                            │
│      ?objUri hasco:originalID ?originalId .                         │
│    }                                                                 │
│  • Validate URIs, check for duplicates                              │
│  • Result: Map<originalID, objectURI>                               │
└─────────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────────┐
│ Stage 5: CSV Processing (processCSVFile)                            │
│  • Parse CSV with header row                                        │
│  • For each row:                                                    │
│      1. Look up objectURI by originalID                             │
│      2. Generate triples for columns 1..N                           │
│      3. Add timestamp triple                                        │
│  • Batch write every 10,000 rows                                    │
└─────────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────────┐
│ Stage 6: Triplestore Insertion (saveModelToTriplestore)             │
│  • Serialize model to Turtle                                        │
│  • POST to named graph: {daUri}-dasoc                               │
│  • Log: "Inserted {N} triples into graph <{uri}>"                   │
└─────────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────────┐
│ Result: DataFile status → PROCESSED                                 │
│         Study objects enriched with extra properties                │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 5. Validation Rules and Error Codes

DA-SOC ingestion uses the **Error Dictionary** pattern (`error_dictionary.json`) to standardize error reporting. All error codes are prefixed with `DASOC_` to distinguish them from other file types (DSG, DP2, INS, etc.).

### 5.1 Error Code Reference Table

| Code         | Condition                                                                 | Consequence                          | Severity |
|--------------|---------------------------------------------------------------------------|--------------------------------------|----------|
| `DASOC_00001` | StudyObjectCollection URI could not be resolved or does not exist in triplestore | Ingestion fails (no objects to enrich) | **FATAL** |
| `DASOC_00002` | SOC exists but contains **zero StudyObject instances** (empty collection) | Ingestion fails (no originalID map built) | **FATAL** |
| `DASOC_00003` | `originalID` in CSV row does not match any object in SOC                  | **Row skipped** (logged as warning)  | **WARNING** |
| `DASOC_00004` | Object URI retrieved from triplestore is malformed (does not start with `http://` or `https://`) | Object excluded from originalID map | **WARNING** |
| `DASOC_00005` | Duplicate `originalID` found in SOC (same `originalID` on multiple objects) | First occurrence kept, duplicates skipped | **WARNING** |
| `DASOC_00006` | CSV file has **fewer than 2 columns** (missing property columns)          | Ingestion fails (CSV structure invalid) | **FATAL** |
| `DASOC_00007` | Exception thrown while processing a specific row (e.g., CSV parse error) | **Row skipped** (logged as warning)  | **WARNING** |
| `DASOC_00008` | URL decoding failed for DA URI or SOC URI                                 | Ingestion fails (cannot proceed without valid URIs) | **FATAL** |

---

### 5.2 Error Handling Strategy

**Fatal Errors (Ingestion Stops):**
- `DASOC_00001`, `DASOC_00002`, `DASOC_00006`, `DASOC_00008`
- These errors indicate **structural problems** that prevent any meaningful processing.
- When a fatal error occurs, `IngestionResult.success = false` and the error message is returned to the caller.
- The `DataFile` status remains `UNPROCESSED` or is set to `ERROR`.

**Warning Errors (Row Skipped, Ingestion Continues):**
- `DASOC_00003`, `DASOC_00004`, `DASOC_00005`, `DASOC_00007`
- These errors indicate **row-level problems** that do not invalidate the entire file.
- Processing continues for valid rows.
- The final `IngestionResult` includes counters for `skippedRows` and `errorRows`.

**Example Scenario:**
- CSV has 100 rows.
- 5 rows have invalid `originalID` → 5 × `DASOC_00003` warnings logged.
- 95 rows processed successfully.
- Final result: `success = true`, `rowCount = 95`, log shows 5 warnings.

---

### 5.3 Error Messages (from `error_dictionary.json`)

```json
{
  "DASOC": {
    "file type": "DASOC",
    "content": [
      {
        "id": "DASOC_00001",
        "detail": "StudyObjectCollection <%s> not found",
        "solution": "Ensure the SOC has been properly ingested before attempting DASOC ingestion"
      },
      {
        "id": "DASOC_00002",
        "detail": "No objects found in StudyObjectCollection <%s>",
        "solution": "Verify the SOC contains study objects with originalID values"
      },
      {
        "id": "DASOC_00003",
        "detail": "Original ID '%s' not found in SOC at row %d",
        "solution": "Check that the originalID in the CSV matches objects in the SOC"
      },
      {
        "id": "DASOC_00004",
        "detail": "Invalid object URI found: %s",
        "solution": "Ensure all object URIs in the SOC are properly formatted (must start with http:// or https://)"
      },
      {
        "id": "DASOC_00005",
        "detail": "Duplicate originalID found: '%s' (keeping first occurrence)",
        "solution": "Remove duplicate originalID values from the StudyObjectCollection"
      },
      {
        "id": "DASOC_00006",
        "detail": "CSV file must have at least 2 columns (originalID + at least one property)",
        "solution": "Add property columns to the CSV file. First column should be originalID, remaining columns should be property URIs"
      },
      {
        "id": "DASOC_00007",
        "detail": "Error processing row %d: %s",
        "solution": "Check the CSV format and data values at the identified row"
      },
      {
        "id": "DASOC_00008",
        "detail": "URL decoding error for URI: %s",
        "solution": "Ensure the URI is properly URL-encoded"
      }
    ]
  }
}
```

---

## 6. Modeling Rules Derived from the Codebase

These rules are **extracted directly** from `AnnotateDASOC.java`, `DASOCGenerator.java`, and `IngestionWorker.java`. They represent the **behavioral contract** of the DA-SOC subsystem.

---

### 6.1 DA-SOC Does Not Create New SOC Instances

**Rule:** A DA-SOC file **cannot introduce new StudyObject instances** into a SOC.

**Evidence:**
- The originalID → objectURI map is built **entirely from existing triplestore data** (SPARQL query in `buildOriginalIdMap()`).
- If an `originalID` in the CSV does not exist in the map, the row is **skipped** with warning `DASOC_00003`.
- There is **no code path** that creates new StudyObject entities during DA-SOC ingestion.

**Consequence:**
- DA-SOC is strictly a **post-DSG enrichment** mechanism.
- To add new objects to a SOC, you must **re-ingest the DSG** with additional rows.

---

### 6.2 DA-SOC Enriches Existing StudyObjects with Extra Triples

**Rule:** DA-SOC adds RDF triples to existing StudyObject resources. It does **not replace or delete** existing triples.

**Evidence:**
- The CSV processing loop (in `processCSVFile()`) uses `model.add(subject, predicate, object)` to **append** triples to the RDF model.
- There is **no DELETE operation** or graph clearing before insertion.
- Each batch is written to the triplestore using **Graph Store Protocol POST**, which **adds** triples to the named graph (does not overwrite).

**Consequence:**
- If you ingest the same DA-SOC file twice, the triples will be **duplicated** (unless the triplestore has duplicate suppression enabled).
- ⚠️ **NOT CONFIRMED:** Whether the triplestore automatically deduplicates identical triples or allows duplicates. Requires testing or documentation review.

---

### 6.3 Total Property Count = 5 Base Fields + N Extra Columns + 1 Timestamp

**Rule:** After DA-SOC ingestion, each enriched StudyObject has:
```
Total Properties = 5 (base from DSG) + N (DA-SOC columns 1..N) + 1 (timestamp)
```

**Base Properties (from DSG):**
1. `rdf:type` → `hasco:StudyObject`
2. `hasco:isMemberOf` → SOC URI
3. `hasco:originalID` → Original identifier
4. `hasco:label` → Display name
5. `rdfs:comment` → Description

**DA-SOC Extra Properties:**
- Columns 1..N from the CSV file
- Each becomes an RDF predicate-object pair

**Timestamp Property:**
- `hasco:hasTimestamp` → ISO 8601 timestamp (when DA-SOC was ingested)
- Example: `"2026-03-25T14:32:15.123Z"`

**Example Calculation:**
- CSV has 3 extra columns (`altitude_m`, `floor`, `zone_code`)
- Total = 5 + 3 + 1 = **9 properties**

---

### 6.4 Multiple DA-SOCs per DSG Are Allowed

**Rule:** A single DSG can have **multiple DA-SOC files**, each targeting different SOCs within the same study.

**Evidence:**
- There is **no uniqueness constraint** enforced on DA-SOC file names or SOC URIs.
- The ingestion pipeline processes each DA-SOC file independently.
- Each DA-SOC creates triples in a **separate named graph** (`{daUri}-dasoc`), preventing cross-contamination.

**Example:**
- Study: `LTE-PIAGET-WEATHER-STATION`
- DSG File: `DSG-LTE-PIAGET-LOCATION.xlsx` (creates 2 SOCs: `SOC-LOCATION`, `SOC-SENSOR`)
- DA-SOC File 1: `DA-SOC-LOCATION.csv` → Enriches `SOC-LOCATION` with geospatial properties
- DA-SOC File 2: `DA-SOC-SENSOR.csv` → Enriches `SOC-SENSOR` with calibration data

**Consequence:**
- You can compose complex enrichment scenarios by uploading multiple DA-SOC files in sequence.
- Each file must target a **different SOC** (or the same SOC with non-overlapping properties).

---

### 6.5 One DA-SOC per SOC per Ingestion Cycle ⚠️ CLARIFICATION NEEDED

**Rule:** ⚠️ **NOT EXPLICITLY ENFORCED** — The codebase does not prevent multiple DA-SOC files from targeting the same SOC simultaneously.

**Evidence:**
- No uniqueness checks on `dasocSOCUri` during ingestion.
- No locking mechanism to prevent concurrent DA-SOC ingestion for the same SOC.

**Potential Issue:**
- If two users upload `DA-SOC-LOCATION.csv` with different properties at the same time, both will succeed and **add triples concurrently** to the same objects.
- This may or may not be desirable depending on use case.

**Recommendation:**
- Clarify business rule: Should multiple DA-SOC files be allowed to target the same SOC?
- If NO, add validation in `IngestionWorker` to reject duplicate SOC targets.
- If YES, document expected behavior (triples are additive, no conflict resolution).

---

### 6.6 Named Graph Behavior

**Rule:** All DA-SOC triples are stored in a **named graph** with URI: `{daUri}-dasoc`

**Evidence:**
```java
saveModelToTriplestore(model, daUri + "-dasoc", dataFile);
```

**Rationale:**
- Isolates DA-SOC triples from the main DA graph.
- Prevents `DA.save()` (which operates on the DA entity graph) from accidentally deleting DA-SOC triples.
- Allows for **selective graph deletion** if needed (e.g., `DELETE FROM <{daUri}-dasoc>` to remove all DA-SOC enrichments without affecting the base SOC instances).

**Example:**
- `daUri` = `http://hadatac.org/ont/arrowhead/DA-123456`
- DA-SOC named graph = `http://hadatac.org/ont/arrowhead/DA-123456-dasoc`

**Consequence:**
- SPARQL queries must explicitly query the DA-SOC named graph to retrieve enriched properties:
  ```sparql
  SELECT ?obj ?altitude WHERE {
      GRAPH <http://hadatac.org/ont/arrowhead/DA-123456-dasoc> {
          ?obj pharma:altitude_m ?altitude .
      }
  }
  ```
- Or use `FROM <graph>` clause to merge graphs in a single query.

---

## 7. Example: Application to LTE-PIAGET-WEATHER-STATION Study

### 7.1 Study Context

**Study:**
- URI: `http://hadatac.org/ont/arrowhead/LTE-PIAGET-WEATHER-STATION`
- Purpose: Monitor environmental conditions across a university campus

**DSG File:** `DSG-LTE-PIAGET-LOCATION.xlsx`
- Ingested on: 2026-03-10
- Created SOC: `http://hadatac.org/ont/arrowhead/SOC-LOCATION`
- SOC contains **50 instances** (weather station locations)

**Sample SOC Instances (from DSG):**

| Object URI                                      | originalID       | label                | comment              |
|------------------------------------------------|------------------|----------------------|----------------------|
| `http://hadatac.org/.../LIBRARY-L0`            | `LIBRARY-L0`     | Library Level 0      | Main library floor   |
| `http://hadatac.org/.../SCIENCE-ROOF`          | `SCIENCE-ROOF`   | Science Building Roof| Outdoor sensor       |
| `http://hadatac.org/.../CAFETERIA-EAST`        | `CAFETERIA-EAST` | Cafeteria East Wing  | Indoor sensor        |
| `http://hadatac.org/.../ADMIN-BASEMENT`        | `ADMIN-BASEMENT` | Admin Building B1    | Basement monitoring  |

**Problem:**
- The DSG captured basic location metadata (ID, label, comment).
- But it **did not capture** geospatial details like altitude, floor number, or zone codes.
- The research team now wants to add these properties **without re-ingesting the entire DSG**.

**Solution:** Use DA-SOC to enrich the existing instances.

---

### 7.2 DA-SOC File: `DA-SOC-LOCATION.csv`

**File Name:** `DA-SOC-LOCATION.csv`

**Content:**
```csv
originalID,pharma:altitude_m,pharma:floor,pharma:zone_code
LIBRARY-L0,15.0,0,ZONE-A
SCIENCE-ROOF,25.5,4,ZONE-B
CAFETERIA-EAST,12.0,1,ZONE-A
ADMIN-BASEMENT,8.5,-1,ZONE-C
```

**Column Definitions:**
- **Column 0:** `originalID` — Matches `hasco:originalID` property in SOC instances
- **Column 1:** `pharma:altitude_m` — Altitude in meters (literal)
- **Column 2:** `pharma:floor` — Floor number (literal, can be negative for basements)
- **Column 3:** `pharma:zone_code` — Security/administrative zone identifier (literal)

**Namespace Context:**
- `pharma:` prefix resolves to `http://hadatac.org/ont/pharma#` (configured in HADatAc namespace registry)

---

### 7.3 Ingestion Pipeline Execution

#### Step 1: File Detection
```
[INGESTION PATH] DataFile: DA-SOC-LOCATION.csv
[INGESTION PATH] Base filename: DA-SOC-LOCATION
[INGESTION PATH] Checking if starts with 'DA-SOC-': true
[INGESTION PATH] ✅ Matched DA-SOC-* pattern, routing to AnnotateDASOC
```

#### Step 2: SOC Resolution
```
[INGESTION PATH] Detected DASOC file pattern: DA-SOC-*
[INGESTION PATH] Extracting SOC name: LOCATION
IngestionWorker.findSOCByName(): Querying for SOC with name: LOCATION
IngestionWorker.findSOCByName(): Found SOC URI: http://hadatac.org/ont/arrowhead/SOC-LOCATION
[INGESTION PATH] Auto-detected SOC URI from triplestore: http://hadatac.org/ont/arrowhead/SOC-LOCATION
DataFile.dasocSOCUri set to: http://hadatac.org/ont/arrowhead/SOC-LOCATION
```

#### Step 3: DataFile Linkage
```
AnnotateDASOC.processDASOC() called
DA URI: http://hadatac.org/ont/arrowhead/DA-202603251432
SOC URI: http://hadatac.org/ont/arrowhead/SOC-LOCATION

DA <http://hadatac.org/ont/arrowhead/DA-202603251432> not found, creating new DA record
✅ Created DA record: <http://hadatac.org/ont/arrowhead/DA-202603251432>
```

#### Step 4: Build originalID Map
```
SPARQL Query Executed:
  SELECT ?objUri ?originalId WHERE {
      ?objUri hasco:isMemberOf <http://hadatac.org/ont/arrowhead/SOC-LOCATION> .
      ?objUri hasco:originalID ?originalId .
  }

Results:
  - LIBRARY-L0 → http://hadatac.org/ont/arrowhead/LIBRARY-L0
  - SCIENCE-ROOF → http://hadatac.org/ont/arrowhead/SCIENCE-ROOF
  - CAFETERIA-EAST → http://hadatac.org/ont/arrowhead/CAFETERIA-EAST
  - ADMIN-BASEMENT → http://hadatac.org/ont/arrowhead/ADMIN-BASEMENT
  - (46 more...)

✅ (1) Found related SOC: <http://hadatac.org/ont/arrowhead/SOC-LOCATION>
✅ (2) Created originalID<->URI map with 50 elements
```

#### Step 5: CSV Processing

**Row 1: LIBRARY-L0**
```
originalID: LIBRARY-L0
Object URI: http://hadatac.org/ont/arrowhead/LIBRARY-L0

Triples Generated:
  <http://hadatac.org/.../LIBRARY-L0> pharma:altitude_m "15.0" .
  <http://hadatac.org/.../LIBRARY-L0> pharma:floor "0" .
  <http://hadatac.org/.../LIBRARY-L0> pharma:zone_code "ZONE-A" .
  <http://hadatac.org/.../LIBRARY-L0> hasco:hasTimestamp "2026-03-25T14:32:15.123Z" .
```

**Row 2: SCIENCE-ROOF**
```
originalID: SCIENCE-ROOF
Object URI: http://hadatac.org/ont/arrowhead/SCIENCE-ROOF

Triples Generated:
  <http://hadatac.org/.../SCIENCE-ROOF> pharma:altitude_m "25.5" .
  <http://hadatac.org/.../SCIENCE-ROOF> pharma:floor "4" .
  <http://hadatac.org/.../SCIENCE-ROOF> pharma:zone_code "ZONE-B" .
  <http://hadatac.org/.../SCIENCE-ROOF> hasco:hasTimestamp "2026-03-25T14:32:15.123Z" .
```

**Row 3: CAFETERIA-EAST**
```
originalID: CAFETERIA-EAST
Object URI: http://hadatac.org/ont/arrowhead/CAFETERIA-EAST

Triples Generated:
  <http://hadatac.org/.../CAFETERIA-EAST> pharma:altitude_m "12.0" .
  <http://hadatac.org/.../CAFETERIA-EAST> pharma:floor "1" .
  <http://hadatac.org/.../CAFETERIA-EAST> pharma:zone_code "ZONE-A" .
  <http://hadatac.org/.../CAFETERIA-EAST> hasco:hasTimestamp "2026-03-25T14:32:15.123Z" .
```

**Row 4: ADMIN-BASEMENT**
```
originalID: ADMIN-BASEMENT
Object URI: http://hadatac.org/ont/arrowhead/ADMIN-BASEMENT

Triples Generated:
  <http://hadatac.org/.../ADMIN-BASEMENT> pharma:altitude_m "8.5" .
  <http://hadatac.org/.../ADMIN-BASEMENT> pharma:floor "-1" .
  <http://hadatac.org/.../ADMIN-BASEMENT> pharma:zone_code "ZONE-C" .
  <http://hadatac.org/.../ADMIN-BASEMENT> hasco:hasTimestamp "2026-03-25T14:32:15.123Z" .
```

#### Step 6: Triplestore Insertion
```
[DASOC] saveModelToTriplestore() called
[DASOC]   Model size: 16 triples
[DASOC]   Target graph: http://hadatac.org/ont/arrowhead/DA-202603251432-dasoc
[DASOC]   Serialized to 1024 bytes
[DASOC]   GSP endpoint: http://localhost:3030/store/data
[DASOC]   Calling GSPClient.postInputStream()...
[DASOC]   ✅ GSPClient.postInputStream() completed successfully
Inserted 16 triples into graph <http://hadatac.org/ont/arrowhead/DA-202603251432-dasoc>
```

#### Final Summary
```
=== DASOC Ingestion Summary ===
✅ (3) Total rows in DA-SOC-LOCATION.csv: 4 (excluding header)
✅ (4) Number of objects created: 4
Rows skipped (empty/invalid originalID): 0
Rows with errors: 0

Processing complete - 4 objects successfully enriched from 4 CSV rows
DataFile status set to PROCESSED
```

---

### 7.4 Resulting RDF Graph

**Named Graph:** `http://hadatac.org/ont/arrowhead/DA-202603251432-dasoc`

**Complete Triples for LIBRARY-L0:**
```turtle
@prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .
@prefix hasco: <http://hadatac.org/ont/hasco#> .
@prefix pharma: <http://hadatac.org/ont/pharma#> .

# Base properties (from DSG ingestion, stored in main study graph)
<http://hadatac.org/ont/arrowhead/LIBRARY-L0>
    rdf:type hasco:StudyObject ;
    hasco:isMemberOf <http://hadatac.org/ont/arrowhead/SOC-LOCATION> ;
    hasco:originalID "LIBRARY-L0" ;
    hasco:label "Library Level 0" ;
    rdfs:comment "Main library floor" .

# DA-SOC enrichment properties (stored in DA-202603251432-dasoc graph)
<http://hadatac.org/ont/arrowhead/LIBRARY-L0>
    pharma:altitude_m "15.0" ;
    pharma:floor "0" ;
    pharma:zone_code "ZONE-A" ;
    hasco:hasTimestamp "2026-03-25T14:32:15.123Z" .
```

**Total Properties for LIBRARY-L0:**
- 5 base (from DSG)
- 3 extra (from DA-SOC)
- 1 timestamp (from DA-SOC)
- **Total: 9 properties**

---

### 7.5 SPARQL Query to Retrieve Enriched Data

**Query:** Retrieve all locations with their altitude and floor:
```sparql
PREFIX hasco: <http://hadatac.org/ont/hasco#>
PREFIX pharma: <http://hadatac.org/ont/pharma#>

SELECT ?location ?label ?altitude ?floor ?zone
WHERE {
    # Base properties from study graph
    ?location hasco:isMemberOf <http://hadatac.org/ont/arrowhead/SOC-LOCATION> .
    ?location hasco:label ?label .
    
    # Enriched properties from DA-SOC graph
    GRAPH <http://hadatac.org/ont/arrowhead/DA-202603251432-dasoc> {
        ?location pharma:altitude_m ?altitude .
        ?location pharma:floor ?floor .
        ?location pharma:zone_code ?zone .
    }
}
ORDER BY ?label
```

**Results:**
| location                                         | label                 | altitude | floor | zone   |
|-------------------------------------------------|----------------------|----------|-------|--------|
| `http://hadatac.org/.../ADMIN-BASEMENT`         | Admin Building B1    | 8.5      | -1    | ZONE-C |
| `http://hadatac.org/.../CAFETERIA-EAST`         | Cafeteria East Wing  | 12.0     | 1     | ZONE-A |
| `http://hadatac.org/.../LIBRARY-L0`             | Library Level 0      | 15.0     | 0     | ZONE-A |
| `http://hadatac.org/.../SCIENCE-ROOF`           | Science Building Roof| 25.5     | 4     | ZONE-B |

---

## 8. How to Use This Specification Going Forward

This specification serves **four primary audiences**, each with distinct use cases:

---

### 8.1 For Data Curators (Creating DA-SOC Files)

**Use This Spec To:**
1. **Understand File Structure** — Consult Section 3 for CSV format rules, column layout, and naming conventions.
2. **Validate File Before Upload** — Check that:
   - File name matches `DA-SOC-{SOCNAME}.csv` pattern
   - Column 0 is `originalID`
   - All `originalID` values exist in the target SOC (query triplestore first)
   - At least 1 property column is present (columns 1..N)
3. **Troubleshoot Ingestion Failures** — Consult Section 5 (Error Codes) to interpret warnings/errors in the ingestion log.
4. **Plan Multi-File Enrichment** — Use Section 6.4 to understand how multiple DA-SOC files can target different SOCs within the same study.

**Example Workflow:**
```
Step 1: Query triplestore to list all originalID values in SOC-LOCATION
Step 2: Create CSV with those originalIDs in column 0
Step 3: Add property columns (e.g., pharma:altitude_m, pharma:floor)
Step 4: Upload file via HADatAc UI
Step 5: Check ingestion log for DASOC_00003 warnings (invalid originalIDs)
Step 6: Fix invalid rows and re-upload if needed
```

---

### 8.2 For Backend Developers (Extending AnnotateDASOC)

**Use This Spec To:**
1. **Understand Current Behavior** — Section 4 (Ingestion Pipeline) documents the exact flow, method calls, and SPARQL queries used.
2. **Add New Validation Rules** — Consult Section 5 to add new error codes following the established pattern.
3. **Modify CSV Processing Logic** — Section 4.4 shows how rows are parsed, validated, and converted to RDF triples.
4. **Debug Named Graph Issues** — Section 6.6 explains why triples go to `{daUri}-dasoc` graph (prevents deletion by `DA.save()`).

**Common Extension Points:**
- **Add URI validation for column headers:** Modify `processCSVFile()` to reject headers that are not valid URIs or CURIEs.
- **Add duplicate row detection:** Hash each row and reject duplicates before processing.
- **Add property cardinality constraints:** Prevent multiple DA-SOCs from overwriting the same property on the same object.

---

### 8.3 For Frontend Developers (Integrating DA-SOC Upload)

**Use This Spec To:**
1. **Understand File Requirements** — Section 3 defines what makes a valid DA-SOC file (for client-side validation).
2. **Design Upload UI** — Show users the expected file name pattern and column structure.
3. **Display Ingestion Results** — Parse the `IngestionResult` object (Section 4.4) to show row counts, errors, and warnings.
4. **Explain Error Codes** — Use Section 5 to generate user-friendly error messages in the UI.

**Example UI Flow:**
```
1. User selects SOC from dropdown → UI suggests file name: DA-SOC-{SOCNAME}.csv
2. User uploads CSV → Frontend validates:
   - File extension is .csv
   - File name starts with DA-SOC-
3. Backend processes file → Returns IngestionResult
4. Frontend displays:
   - ✅ Success: 95 rows processed
   - ⚠️ 5 rows skipped (invalid originalID)
   - Link to full ingestion log
```

---

### 8.4 For Ontology Architects (Understanding DA-SOC's Role in the Knowledge Graph)

**Use This Spec To:**
1. **Understand Relationship to DSG** — Section 1 and 2.3 clarify that DA-SOC **extends** DSG-created instances, not replaces them.
2. **Design Property Schemas** — Section 2.2 explains how DA-SOC columns become RDF predicates (no schema enforcement at ingestion time).
3. **Plan Named Graph Strategy** — Section 6.6 shows how DA-SOC triples are isolated in separate graphs for provenance tracking.
4. **Calculate Property Counts** — Section 6.3 provides the formula for total properties per object after enrichment.

**Modeling Considerations:**
- **Should DA-SOC properties be required or optional?** → DA-SOC always adds optional properties (no `sh:minCount` enforcement).
- **Should DA-SOC properties have datatypes?** → Current implementation treats all values as literals unless they look like URIs. Consider adding explicit datatype hints in column headers (e.g., `pharma:altitude_m^^xsd:float`).
- **Should DA-SOC support multi-valued properties?** → Not currently supported. If a CSV has two rows with the same `originalID`, both rows add triples to the same object, but there's no array/list handling.

---

## Open Questions

The following ambiguities were identified during code inspection and require clarification from the backend development team before this specification can be considered **fully canonical**:

---

### Q1: DA URI Source and Construction

**Question:** Where does the `daUri` parameter come from in the `AnnotateDASOC.exec(DataFile)` call?

**Observation:**
- The current code extracts `daUri` from `DataFile.getDasocDataAcquisitionUri()`.
- But there is no clear code path showing **where** this field is initially populated.
- In `AnnotateDASOC.processDASOC()`, if the DA does not exist, a new one is created with the provided `daUri`.

**Implications:**
- If `daUri` is `null` or empty, ingestion fails immediately.
- If `daUri` is auto-generated (e.g., `{kbPrefix}DA-{timestamp}`), document the generation logic.
- If `daUri` is user-provided (via API or UI), document the expected format and validation rules.

**Recommendation:**
- Add code comments in `IngestionWorker.ingest()` showing where `dasocDataAcquisitionUri` is set.
- Add a section to this spec documenting DA URI construction rules.

---

### Q2: Duplicate Triple Handling

**Question:** If the same DA-SOC file is ingested twice, are triples deduplicated or duplicated?

**Observation:**
- The code uses `model.add()` which appends triples.
- The triplestore POST operation does not appear to check for existing triples before insertion.

**Test Needed:**
1. Ingest `DA-SOC-LOCATION.csv` (4 rows, 16 triples).
2. Ingest the same file again.
3. Query the named graph — are there 16 or 32 triples?

**Implications:**
- If triples are duplicated, users need to be warned not to re-ingest the same file.
- If triples are deduplicated (by the triplestore), document this behavior.
- Consider adding a "replace mode" that deletes the DA-SOC named graph before re-ingestion.

**Recommendation:**
- Test triple deduplication behavior with Fuseki.
- Document findings in Section 6.2.

---

### Q3: Empty SOC Name Handling

**Question:** What happens if a file is named `DA-SOC-.csv` (empty SOC name)?

**Observation:**
- `socName = baseName.substring(7)` would produce an empty string.
- `findSOCByName("")` would query for SOCs with empty labels or URIs containing `"SOC-"` (likely matches many SOCs).
- Fallback construction: `kbPrefix + "SOC-"` produces an invalid URI.

**Test Needed:**
1. Upload a file named `DA-SOC-.csv`.
2. Check whether ingestion fails gracefully or produces corrupt data.

**Recommendation:**
- Add validation in `IngestionWorker.ingest()` to reject files where `socName.isEmpty()`.
- Return error: `"Invalid file name: SOC name cannot be empty"`

---

### Q4: Multiple DA-SOC Files Targeting Same SOC

**Question:** Is it allowed (or recommended) to upload multiple DA-SOC files that target the same SOC?

**Scenario:**
- User uploads `DA-SOC-LOCATION-PART1.csv` (adds altitude)
- User uploads `DA-SOC-LOCATION-PART2.csv` (adds floor and zone)

**Observation:**
- Both files would resolve to the same `SOC-LOCATION` URI.
- Both would add triples to the same objects.
- No conflict detection or merging logic exists.

**Implications:**
- If allowed, document that properties are **additive** (no replacement).
- If not allowed, add uniqueness validation.

**Recommendation:**
- Clarify business rule and document in Section 6.5.

---

### Q5: API Endpoint for DA-SOC Ingestion

**Question:** Is there a dedicated REST API endpoint for DA-SOC ingestion, or does it reuse the general file upload endpoint?

**Observation:**
- No DA-SOC-specific routes found in `conf/routes`.
- Likely uses the standard file upload endpoint: `/hascoapi/api/upload`.

**Implications:**
- Users may need to know how to trigger DA-SOC ingestion via API (vs. UI).
- Frontend needs to know which endpoint to call.

**Recommendation:**
- Document the API endpoint in this spec (add new Section 9: API Reference).
- Show example `curl` command for DA-SOC upload.

---

### Q6: Column Header URI Validation

**Question:** Are column headers (columns 1..N) validated as URIs or CURIEs before processing?

**Observation:**
- Code calls `URIUtils.replacePrefixEx(predicateUri)` to expand prefixes.
- But if a header is `"invalid header with spaces"`, it would still become a predicate URI.

**Implication:**
- Users could accidentally create malformed predicates in the triplestore.

**Recommendation:**
- Add validation in `processCSVFile()` to reject headers that are not valid URIs or CURIEs.
- Return error: `DASOC_00009: Invalid property URI in column {N}: "{header}"`

---

## Appendix: Glossary

| Term        | Definition                                                                                   |
|-------------|----------------------------------------------------------------------------------------------|
| **DA**      | Data Acquisition — a record linking uploaded files to studies and data processing workflows. |
| **DA-SOC**  | Data Acquisition — Study Object Collection — a CSV file that enriches existing SOC instances. |
| **DSG**     | Data Study Generation — an Excel workbook that defines study structure and creates SOC instances. |
| **SOC**     | StudyObjectCollection — a named set of StudyObject instances (e.g., locations, sensors).    |
| **originalID** | A unique identifier for a StudyObject within its SOC (user-defined, stored in `hasco:originalID`). |
| **CURIE**   | Compact URI — a prefixed URI like `pharma:altitude_m` (expands to full URI via namespace registry). |
| **Named Graph** | A URI-identified subset of triples in the triplestore (RDF quad: `<subject> <predicate> <object> <graph>`). |
| **GSP**     | Graph Store Protocol — a RESTful API for adding/removing RDF graphs (used by `GSPClient`). |
| **Triplestore** | A database that stores RDF triples (subject-predicate-object statements). HADatAc uses Apache Jena Fuseki. |

---

**End of Specification**

---

**Version History:**
- **v1.0** (2026-03-25): Initial release based on codebase inspection of Kaykael branch (commit: `bc85bd0`).

**Maintainer:** HADatAc Development Team  
**Contact:** [GitHub Issues](https://github.com/paulopinheiro1234/hadatac/issues)

