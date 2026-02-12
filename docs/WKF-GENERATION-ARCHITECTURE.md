# WKF Technical Architecture

## Package Overview

**Package**: `org.hascoapi.transform.mt.wkf`  
**Purpose**: Generate Excel workbooks from WKF (Workflow) metadata stored in triple store  
**Pattern**: Based on DSG (Data Study Generator) architecture

## Class Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                         WKFGen                              │
│  (Main Orchestrator)                                        │
├─────────────────────────────────────────────────────────────┤
│ + genByStatus(status, filename) : String                   │
│ + genByWkf(wkf, filename) : String                         │
│ + genByManager(email, status, filename) : String           │
│ + create(filename, wkfs) : Workbook                        │
│ + save(helper, filename) : String                          │
│ - pruneUnusedNamespaces(workbook) : void                   │
│ - collectUsedPrefixes(workbook) : Set<String>              │
│ - canonicalizeWkfUri(uri) : String                         │
└─────────────────────────────────────────────────────────────┘
                          │
                          │ uses
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    WKFGenHelper                             │
│  (State Container)                                          │
├─────────────────────────────────────────────────────────────┤
│ + namespaces : Map<String, NameSpace>  [static]            │
│ + processStems : Map<String, ProcessStem>                  │
│ + processes : Map<String, Process>                         │
│ + tasks : Map<String, Task>                                │
│ + requiredInstruments : Map<String, RequiredInstrument>    │
│ + workbook : Workbook                                      │
└─────────────────────────────────────────────────────────────┘
                          │
                          │ passed to
                          ▼
┌──────────────────┬──────────────────┬──────────────────┬──────────────────┐
│ WKFProcessStems  │  WKFProcesses    │    WKFTasks      │WKFRequiredInstr. │
│                  │                  │                  │                  │
├──────────────────┼──────────────────┼──────────────────┼──────────────────┤
│+ addByWkf()      │+ addByWkf()      │+ addByWkf()      │+ addByWkf()      │
│  : WKFGenHelper  │  : WKFGenHelper  │  : WKFGenHelper  │  : WKFGenHelper  │
└──────────────────┴──────────────────┴──────────────────┴──────────────────┘
```

## Data Flow

```
┌─────────────┐
│   Start     │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────┐
│ 1. Query WKFs from          │
│    Triple Store             │
│    (by status/manager/uri)  │
└──────┬──────────────────────┘
       │
       ▼
┌─────────────────────────────┐
│ 2. Normalize & Deduplicate  │
│    - Canonicalize URIs      │
│    - Apply status filter    │
│    - Remove duplicates      │
└──────┬──────────────────────┘
       │
       ▼
┌─────────────────────────────┐
│ 3. Create Workbook          │
│    - InfoSheet              │
│    - Namespaces             │
│    - ProcessStems (headers) │
│    - Processes (headers)    │
│    - Tasks (headers)        │
│    - RequiredInstruments    │
└──────┬──────────────────────┘
       │
       ▼
┌─────────────────────────────┐
│ 4. Populate Sheets          │
│    For each WKF:            │
│    ├─ ProcessStems.addByWkf│
│    ├─ Processes.addByWkf   │
│    ├─ Tasks.addByWkf        │
│    └─ RequiredInstr.addByWkf│
└──────┬──────────────────────┘
       │
       ▼
┌─────────────────────────────┐
│ 5. Prune Unused Namespaces  │
│    - Scan all cells         │
│    - Extract used prefixes  │
│    - Remove unused rows     │
└──────┬──────────────────────┘
       │
       ▼
┌─────────────────────────────┐
│ 6. Save to File System      │
│    Path: hascoapi.paths     │
│          .ingestion + file  │
└──────┬──────────────────────┘
       │
       ▼
┌─────────────┐
│   Success   │
│   or Error  │
└─────────────┘
```

## Sheet Generation Flow (per WKF)

```
┌─────────────────────┐
│   WKF Instance      │
│   uri: WKF123       │
│   namedGraph: DFL456│
└──────┬──────────────┘
       │
       ├─────────────────────────────────────────┐
       │                                         │
       ▼                                         ▼
┌─────────────────────┐                 ┌─────────────────────┐
│ Get Named Graph     │                 │ Fallback: Use       │
│ from WKF            │                 │ hasDataFileUri      │
└──────┬──────────────┘                 └─────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────────────────┐
│             SPARQL Query (per entity type)                  │
│                                                             │
│  SELECT ?uri WHERE {                                        │
│    GRAPH <namedGraph> {                                     │
│      ?uri a vstoi:ProcessStem .  // or Process, Task, etc. │
│    }                                                        │
│  }                                                          │
└──────┬──────────────────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────────────────┐
│           GenericFind.findByQuery(Class, query)             │
│           Returns: List<Entity>                             │
└──────┬──────────────────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────────────────┐
│        For each entity in list:                             │
│        ┌─────────────────────────────────────────┐          │
│        │ 1. Create new Excel row                 │          │
│        │ 2. Map POJO fields → Columns            │          │
│        │ 3. Apply URI abbreviation               │          │
│        │ 4. Join multi-value lists with " | "    │          │
│        │ 5. Handle null values                   │          │
│        └─────────────────────────────────────────┘          │
└─────────────────────────────────────────────────────────────┘
```

## Entity-Sheet Mapping

| Entity Class          | Sheet Name           | Type Property           | Unique Fields                    |
|-----------------------|----------------------|-------------------------|----------------------------------|
| ProcessStem           | ProcessStems         | vstoi:ProcessStem       | hasContent, wasGeneratedBy       |
| Process               | Processes            | vstoi:Process           | hasTopTask                       |
| Task                  | Tasks                | vstoi:Task              | hasSupertask, hasSubtask,        |
|                       |                      |                         | hasTemporalDependency,           |
|                       |                      |                         | hasRequiredInstrument            |
| RequiredInstrument    | RequiredInstruments  | vstoi:RequiredInstrument| usesInstrument,                  |
|                       |                      |                         | hasRequiredComponent             |

## Common Fields (All Entities)

```java
// All entities inherit from HADatAcThing/HADatAcClass:
- uri                    → hasURI column
- typeUri                → rdf:type column
- hascoTypeUri           → hasco:hascoType column
- label                  → rdfs:label column
- comment                → rdfs:comment column
- hasImageUri            → hasco:hasImage column
- hasWebDocument         → hasco:hasWebDocument column

// SIRElement interface adds:
- hasStatus              → vstoi:hasStatus column
- hasLanguage            → vstoi:hasLanguage column
- hasVersion             → vstoi:hasVersion column
- wasDerivedFrom         → prov:wasDerivedFrom column
- hasReviewNote          → vstoi:hasReviewNote column
- hasSIRManagerEmail     → vstoi:hasSIRManagerEmail column
- hasEditorEmail         → vstoi:hasEditorEmail column
```

## Multi-Value Property Handling

### Tasks.hasSubtask (List<String>)

**Storage in POJO**:
```java
private List<String> hasSubtaskUris = new ArrayList<>();
```

**Query Result**:
```java
[
  "https://hadatac.org/ont/pmsr#/TSK001",
  "https://hadatac.org/ont/pmsr#/TSK002",
  "https://hadatac.org/ont/pmsr#/TSK003"
]
```

**Excel Cell Output**:
```
pmsr:/TSK001 | pmsr:/TSK002 | pmsr:/TSK003
```

**Implementation**:
```java
private static String joinUriList(List<String> uris) {
    if (uris == null || uris.isEmpty()) {
        return "";
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < uris.size(); i++) {
        if (i > 0) {
            sb.append(" | ");
        }
        sb.append(URIUtils.replaceNameSpaceEx(uris.get(i)));
    }
    return sb.toString();
}
```

## URI Abbreviation Strategy

### Input URI
```
http://hadatac.org/ont/vstoi#ProcessStem
```

### Namespace Resolution
```java
// From NameSpace.findInMemory() or WKFGenHelper.namespaces
{
  "vstoi": {
    "uri": "http://hadatac.org/ont/vstoi#",
    "label": "vstoi"
  }
}
```

### Output (Abbreviated)
```
vstoi:ProcessStem
```

### Implementation
```java
URIUtils.replaceNameSpaceEx(uri)
```

## Namespace Pruning Algorithm

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Collect Used Prefixes                                    │
│    - Scan all sheets (except InfoSheet, Namespaces)        │
│    - For each cell containing ":"                           │
│    - Extract prefix before ":"                              │
│    - Add to Set<String> usedPrefixes                        │
└──────┬──────────────────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. Always Keep Core Prefixes                                │
│    usedPrefixes.add("rdf")                                  │
│    usedPrefixes.add("rdfs")                                 │
│    usedPrefixes.add("owl")                                  │
│    usedPrefixes.add("xsd")                                  │
│    usedPrefixes.add("vstoi")                                │
│    usedPrefixes.add("prov")                                 │
│    usedPrefixes.add("hasco")                                │
└──────┬──────────────────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. Identify Rows to Remove from Namespaces Sheet           │
│    For each row (skip header):                              │
│      Get prefix from column 0                               │
│      Normalize: trim(), toLowerCase(), remove trailing ":"  │
│      If NOT in usedPrefixes:                                │
│        Add row index to rowsToRemove list                   │
└──────┬──────────────────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. Remove Rows (bottom to top to maintain indexes)          │
│    Sort rowsToRemove in descending order                    │
│    For each rowIndex:                                       │
│      sheet.shiftRows(rowIndex + 1, lastRow, -1)            │
│      OR sheet.removeRow(row) if last row                    │
└─────────────────────────────────────────────────────────────┘
```

## Error Handling Strategy

### Try-Catch Hierarchy

```java
// Level 1: Main generation method
try {
    genByStatus(...);
} catch (Throwable t) {
    return "FAILURE: " + t.getMessage();
}

// Level 2: Per-WKF processing
for (WKF wkf : wkfs) {
    try {
        addProcessStems(wkf);
    } catch (Throwable t) {
        log error, continue to next sheet
    }
    try {
        addProcesses(wkf);
    } catch (Throwable t) {
        log error, continue to next sheet
    }
    // ... continue for all sheets
}

// Level 3: Per-entity processing
for (ProcessStem ps : processStems) {
    if (ps == null) {
        continue; // Skip null entities
    }
    // Process entity
}
```

### Graceful Degradation

- **Missing Named Graph**: Skip WKF with warning
- **Empty Query Results**: Log info, continue
- **Null Entity**: Skip, continue to next
- **Sheet Population Error**: Log, continue to next sheet
- **Namespace Pruning Error**: Log warning, continue
- **File Save Error**: Return failure status

## Memory Management

### Entity Processing Pattern

```java
// ❌ BAD: Load all into memory
List<ProcessStem> allProcessStems = loadAll();
Map<String, ProcessStem> map = new HashMap<>();
for (ProcessStem ps : allProcessStems) {
    map.put(ps.getUri(), ps);
}

// ✅ GOOD: Stream processing
List<ProcessStem> processStems = query(...);
for (ProcessStem ps : processStems) {
    int rowNum = sheet.getLastRowNum() + 1;
    Row row = sheet.createRow(rowNum);
    // Process immediately, don't store
}
```

### Workbook Management

```java
try (FileOutputStream fileOut = new FileOutputStream(file)) {
    workbook.write(fileOut);
} finally {
    if (workbook != null) {
        workbook.close(); // Release memory
    }
}
```

## File System Integration

### Path Resolution

```java
String basePath = ConfigProp.getPathIngestion();
// Example: "C:/hascoapi/var/"

String filename = "WKF-export.xlsx";

String fullPath = basePath + filename;
// Result: "C:/hascoapi/var/WKF-export.xlsx"
```

### Directory Creation

```java
File outputFile = new File(fullPath);
if (outputFile.getParentFile() != null && 
    !outputFile.getParentFile().exists()) {
    outputFile.getParentFile().mkdirs();
}
```

## Triple Store Query Pattern

### Standard Query Structure

```sparql
PREFIX vstoi: <http://hadatac.org/ont/vstoi#>
PREFIX hasco: <http://hadatac.org/ont/hasco/>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?uri WHERE {
    GRAPH <${namedGraph}> {
        ?uri a ${entityType} .
    }
}
```

### Execution

```java
String queryString = NameSpaces.getInstance().printSparqlNameSpaceList()
    + " SELECT ?uri WHERE { "
    + "   GRAPH <" + namedGraph + "> { "
    + "     ?uri a vstoi:ProcessStem . "
    + "   } "
    + " }";

List<ProcessStem> results = GenericFind.findByQuery(
    ProcessStem.class, 
    queryString
);
```

### Result Handling

```java
// GenericFind.findByQuery internally:
// 1. Executes SPARQL query
// 2. Gets list of URIs
// 3. For each URI, calls Entity.find(uri)
// 4. Entity.find(uri) does DESCRIBE query
// 5. Populates POJO from RDF triples
// 6. Returns list of populated POJOs
```

## Performance Characteristics

| Operation                  | Time Complexity | Notes                           |
|---------------------------|-----------------|---------------------------------|
| Query WKFs by status      | O(n)            | n = total WKFs in triple store |
| Normalize/deduplicate     | O(n)            | n = WKFs found                 |
| Create workbook structure | O(1)            | Fixed number of sheets         |
| Query entities per WKF    | O(m)            | m = entities in named graph    |
| Write entity to row       | O(1)            | Direct cell access             |
| Namespace pruning         | O(s × c)        | s = sheets, c = cells per sheet|
| File write                | O(r)            | r = total rows                 |

**Typical Generation Time**: 2-10 seconds for WKF with 50-200 entities

## Comparison: Generation vs Ingestion

| Aspect              | Ingestion (Excel → RDF)      | Generation (RDF → Excel)        |
|---------------------|------------------------------|---------------------------------|
| Entry Point         | AnnotateWKF.exec()           | WKFGen.genByStatus/genByWkf()   |
| Sheet Processing    | Row-by-row sequential        | SPARQL query → batch populate   |
| Validation          | Extensive (required fields)  | None (assumes valid RDF)        |
| Error Handling      | Strict (stops on error)      | Graceful (continues on error)   |
| Memory Usage        | Moderate (record file cache) | Low (stream processing)         |
| Multi-Value Props   | Parse " | " delimiter        | Join with " | " delimiter        |
| URI Handling        | Expand prefixes to full URI  | Abbreviate full URI to prefix   |
| Named Graph         | Write to WKF's namedGraph    | Read from WKF's namedGraph      |

## Extension Points

### Add New Entity Type

1. **Create POJO** (if not exists):
```java
public class NewEntity extends HADatAcThing implements SIRElement { }
```

2. **Create Generator Class**:
```java
public class WKFNewEntities {
    public static WKFGenHelper addByWkf(WKFGenHelper helper, WKF wkf) {
        // Query, map, populate
    }
}
```

3. **Update WKFGen.create()**:
```java
Sheet newSheet = workbook.createSheet("NewEntities");
Row headerRow = newSheet.createRow(0);
// Add headers
```

4. **Update WKFGen.genByStatus()**:
```java
helper = WKFNewEntities.addByWkf(helper, wkf);
```

### Add Custom Column

1. **Add field to POJO**:
```java
@PropertyField(uri="custom:newField")
private String newField;
```

2. **Add column header** in `create()`:
```java
headerRow.createCell(N).setCellValue("custom:newField");
```

3. **Add cell value** in entity generator:
```java
row.createCell(N).setCellValue(safe(entity.getNewField()));
```

## Dependencies Graph

```
WKFGen
├── WKFGenHelper
├── WKFProcessStems
│   └── ProcessStem (entity)
├── WKFProcesses
│   └── Process (entity)
├── WKFTasks
│   └── Task (entity)
├── WKFRequiredInstruments
│   └── RequiredInstrument (entity)
├── Apache POI
│   ├── XSSFWorkbook
│   ├── Sheet
│   ├── Row
│   └── Cell
└── HAScO Utils
    ├── URIUtils
    ├── NameSpaces
    ├── ConfigProp
    └── GenericFind
```

## Logging Strategy

### Log Levels

- **INFO**: Normal flow (queries, counts, success)
- **WARN**: Recoverable issues (missing graph, empty results)
- **ERROR**: Failures (query errors, file errors)

### Key Log Points

```java
System.out.println("[WKFGen] genByStatus START");
System.out.println("[WKFGen] Diagnostic: total WKFs found=" + count);
System.out.println("[WKFProcessStems] Found " + count + " ProcessStems");
System.out.println("[WKFGen] Workbook created");
System.out.println("✅ [WKFGen] WKF workbook saved successfully!");
System.err.println("[WKFGen] ERROR: " + error.getMessage());
```

## Thread Safety

**Status**: Not thread-safe

**Reasons**:
- Static namespace map in `WKFGenHelper`
- Workbook state in helper instance
- No synchronization mechanisms

**Recommendation**: 
- Use one generation request at a time
- Or create separate helper instances per thread
- Consider future refactoring for concurrent generation

## Summary

The WKF generation architecture provides:

✅ **Modularity**: Separate class per sheet  
✅ **Consistency**: Same structure as DSG generator  
✅ **Robustness**: Comprehensive error handling  
✅ **Efficiency**: Stream processing, memory-conscious  
✅ **Maintainability**: Clear separation of concerns  
✅ **Extensibility**: Easy to add new entity types  
✅ **Compatibility**: Works with existing WKF system  

The design follows SOLID principles and provides a solid foundation for future enhancements.
