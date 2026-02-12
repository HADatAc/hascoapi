# DP2 Ingestion Debugging Guide

## Overview
This guide provides a comprehensive approach to debugging DP2 (Deployment Plan) metadata template ingestion issues in the HADatAc/HASCO API system.

## Problem Description
The DP2 ingestion appears to correctly process the first row (headers), but subsequent content rows are not being ingested properly. This could be due to:
1. Excel parsing issues (SpreadsheetRecordFile)
2. Record-to-Row conversion issues (DP2Generator.createRow)
3. RDF triple generation issues (MetadataFactory.createModel)
4. Triple store commit issues (commitRowsToTripleStore)
5. SPARQL validation issues (validateDP2Instances)

## DP2 Ingestion Flow

```
1. AnnotateDP2.exec()
   ├─> IngestionWorker.nameSpaceGen()
   ├─> IngestionWorker.messageGen()
   ├─> IngestionWorker.deployInstancesGen()
   │   └─> Generates: InstrumentInstances, ComponentInstances, SensingPerspective
   ├─> validateDP2Instances()
   │   └─> Validates references exist in triple store
   └─> Creates GeneratorChain with DP2Generators for each sheet:
       ├─> Platforms
       ├─> PlatformInstances
       ├─> FieldsOfView
       ├─> Deployments
       ├─> InstrumentInstances (again, main processing)
       ├─> ComponentInstances (again, main processing)
       └─> SensingPerspective (again, main processing)

2. GeneratorChain.generate()
   For each generator:
   ├─> BaseGenerator.preprocess()
   ├─> BaseGenerator.createRows()
   │   └─> For each Record:
   │       └─> DP2Generator.createRow() → Map<String, Object>
   ├─> BaseGenerator.createObjects()
   └─> BaseGenerator.postprocess()

3. GeneratorChain.generate() [commit phase]
   For each generator:
   ├─> BaseGenerator.commitRowsToTripleStore()
   │   ├─> MetadataFactory.createModel()
   │   │   └─> For each row: creates RDF triples
   │   └─> MetadataFactory.commitModelToTripleStore()
   │       └─> GSPClient.postModel()
   └─> BaseGenerator.commitObjectsToTripleStore()
```

## Critical Debugging Points

### 1. Excel File Parsing (SpreadsheetRecordFile)
**Location**: `SpreadsheetRecordFile.SheetHandler`

**What to check**:
- Are headers correctly parsed?
- Are all data rows being detected?
- Are empty rows being skipped correctly?
- Are cell values being extracted correctly?

**Debug Output** (now enabled):
```
[SheetHandler] Headers parsed: [hasURI, a, rdfs:label, ...]
[SheetHandler] Starting data row 2
[SheetHandler] Row 2 data: [ahead:INI-AVL-TESTER-00001, ahead:BatteryTester, ...]
[SheetHandler] ✓ Added record #1
```

### 2. Record Processing (BaseGenerator.createRows)
**Location**: `BaseGenerator.createRows()`

**What to check**:
- How many records are being processed?
- Are all records being passed to createRow()?
- Are any records being skipped as duplicates?
- Are null rows being returned?

**Debug Output** (now enabled):
```
========== BaseGenerator.createRows() START ==========
Generator type: DP2Generator
Element type: instrumentinstance
Number of records: 5

--- Processing record #1 ---
✓ Valid row created and added to rows list (total valid rows: 1)
```

### 3. Row Creation (DP2Generator.createRow)
**Location**: `DP2Generator.createRow()`

**What to check**:
- Are all headers being processed?
- Are cell values being extracted correctly?
- Is hasURI being found/set?
- Are metadata fields being added?
- Is the row passing validation?

**Debug Output** (now enabled):
```
========== DP2Generator.createRow() START ==========
ElementType: [instrumentinstance]
RowNumber: 1
Record size: 10
File headers: [hasURI, a, rdfs:label, vstoi:hasSerialNumber, ...]

  Processing header [hasURI] -> value [ahead:INI-AVL-TESTER-00001]
    ✓ Added to row: [hasURI] = [ahead:INI-AVL-TESTER-00001]
  ...
  
✓ Row VALID - returning row with 15 properties
========== DP2Generator.createRow() END (VALID) ==========
```

### 4. RDF Model Creation (MetadataFactory.createModel)
**Location**: `MetadataFactory.createModel()`

**What to check**:
- How many rows are being processed?
- Are all row properties being converted to triples?
- Is the named graph URI correct?
- Are URIs being expanded correctly?
- Are literals being handled correctly?

**Debug Output** (now enabled):
```
========== MetadataFactory.createModel() START ==========
MetadataFactory.createModel() received ROWS with [5] entries
MetadataFactory.createModel() received namedGraphUri [http://...]
Named graph IRI: http://...

--- Processing Row 1 ---
Subject URI: ahead:INI-AVL-TESTER-00001
Subject IRI (expanded): http://hadatac.org/ont/hasco/INI-AVL-TESTER-00001

  Property 1: [a]
    Predicate: rdf:type
    Raw value: ahead:BatteryTester
    ✓ Adding IRI triple: <...> <rdf:type> <...>
  ...

========== MetadataFactory.createModel() SUMMARY ==========
Total rows processed: 5
Total triples in model: 75
```

### 5. Triple Store Commit
**Location**: `BaseGenerator.commitRowsToTripleStore()`

**What to check**:
- Is the model being created correctly?
- Is the commit succeeding?
- How many triples are being committed?

**Debug Output** (now enabled):
```
========== BaseGenerator.commitRowsToTripleStore() START ==========
Generator type: DP2Generator
Element type: instrumentinstance
Number of rows to commit: 5
Named graph URI: http://...

Creating RDF model from rows...
[MetadataFactory output...]
Committing model to triple store...
✓ Successfully committed 75 triples
```

## Common Issues and Solutions

### Issue 1: No rows being created
**Symptoms**: `Total valid rows created: 0`

**Possible causes**:
1. Records not being read from Excel
2. All rows failing hasURI validation
3. Records being filtered out as duplicates

**Debug steps**:
1. Check SpreadsheetRecordFile output - are records being created?
2. Check DP2Generator.createRow - is hasURI present in each record?
3. Check for duplicate detection logic

### Issue 2: Rows created but no triples committed
**Symptoms**: `Total valid rows created: 5` but `Total triples in model: 0`

**Possible causes**:
1. Named graph URI not set
2. Row properties not being recognized as valid predicates
3. URIUtils.replacePrefixEx failing

**Debug steps**:
1. Check MetadataFactory row processing - are properties being converted?
2. Verify namespace prefixes are defined
3. Check URIUtils expansion

### Issue 3: First row works, subsequent rows fail
**Symptoms**: Only the first data row is ingested

**Possible causes**:
1. Excel parsing stopping after first data row
2. Duplicate detection incorrectly flagging rows
3. hasURI validation failing for subsequent rows

**Debug steps**:
1. Check SpreadsheetRecordFile - are all rows being parsed?
2. Check duplicate detection logic in createRows()
3. Verify hasURI format in all rows

### Issue 4: SPARQL validation failing
**Symptoms**: `validateDP2Instances() END - All valid: false`

**Possible causes**:
1. Referenced entities (Platforms, Instruments) not yet in triple store
2. URI format mismatch (with/without < >)
3. SPARQL endpoint not accessible

**Debug steps**:
1. Check validation output - which URIs are not found?
2. Manually query triple store for those URIs
3. Verify ingestion order (INS before DP2)

## Debugging Workflow

### Step 1: Run Ingestion with Debug Output
```bash
# Run the ingestion
# All debug output will now be printed to console and logs
```

### Step 2: Analyze SpreadsheetRecordFile Output
Look for:
```
[SheetHandler] Headers parsed: [...]
[SheetHandler] ✓ Added record #N
```
Expected: One record per data row (excluding header)

### Step 3: Analyze BaseGenerator.createRows Output
Look for:
```
========== BaseGenerator.createRows() START ==========
Number of records: N
✓ Valid row created and added to rows list (total valid rows: N)
```
Expected: Valid rows = Number of records

### Step 4: Analyze DP2Generator.createRow Output
For each record, look for:
```
========== DP2Generator.createRow() START ==========
Processing header [hasURI] -> value [...]
✓ Row VALID - returning row with N properties
```
Expected: Valid return for each row with hasURI

### Step 5: Analyze MetadataFactory.createModel Output
Look for:
```
========== MetadataFactory.createModel() START ==========
Total rows processed: N
Total triples in model: M
```
Expected: Rows processed = rows from step 3, triples > 0

### Step 6: Analyze Commit Output
Look for:
```
✓ Successfully committed M triples
```
Expected: Triples committed > 0

## SPARQL Queries for Verification

### Check if InstrumentInstances were ingested
```sparql
PREFIX vstoi: <http://hadatac.org/ont/vstoi#>
PREFIX hasco: <http://hadatac.org/ont/hasco#>

SELECT ?uri ?label ?type ?serialNumber
WHERE {
  GRAPH <YOUR_NAMED_GRAPH> {
    ?uri a vstoi:InstrumentInstance ;
         rdfs:label ?label ;
         vstoi:hasSerialNumber ?serialNumber .
    OPTIONAL { ?uri a ?type }
  }
}
```

### Check if Deployments were ingested
```sparql
PREFIX vstoi: <http://hadatac.org/ont/vstoi#>

SELECT ?deployment ?platform ?instrument ?label
WHERE {
  GRAPH <YOUR_NAMED_GRAPH> {
    ?deployment a vstoi:Deployment ;
                vstoi:hasPlatformInstance ?platform ;
                vstoi:hasInstrumentInstance ?instrument ;
                rdfs:label ?label .
  }
}
```

### Check if validation references exist
```sparql
# Check if Instrument Models exist (for InstrumentInstances validation)
PREFIX vstoi: <http://hadatac.org/ont/vstoi#>

SELECT ?instrument
WHERE {
  ?instrument a vstoi:Instrument .
}
```

## Next Steps

1. **Collect full debug output** - Run a DP2 ingestion and capture all console output
2. **Identify the failure point** - Use the workflow above to find where the process breaks
3. **Compare first row vs subsequent rows** - Look for differences in processing
4. **Verify prerequisites** - Ensure INS template was ingested first (for validation)
5. **Check Excel file format** - Verify the spreadsheet structure matches expectations

## Additional Tools

### Enable verbose logging in application.conf
```
# Add to conf/application.conf
logger.org.hascoapi.ingestion=DEBUG
logger.org.hascoapi.utils=DEBUG
```

### Query triple store directly
```bash
# Access Fuseki web interface
http://localhost:3030

# Or use curl
curl -X POST http://localhost:3030/store/sparql \
  --data-urlencode "query=SELECT * WHERE { ?s ?p ?o } LIMIT 10"
```

## Contact Points

If debugging reveals an issue:
1. **Excel parsing issue** → Check SpreadsheetRecordFile.java
2. **Row creation issue** → Check DP2Generator.java
3. **RDF generation issue** → Check MetadataFactory.java
4. **Commit issue** → Check GSPClient.java and Fuseki configuration
5. **Validation issue** → Check AnnotateDP2.java validateDP2Instances()

---

**Last Updated**: 2026-02-09
**Version**: 1.0 (Enhanced with comprehensive debug logging)
