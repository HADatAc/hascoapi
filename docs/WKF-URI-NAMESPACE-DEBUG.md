# WKF URI Namespace Debug Guide

## Problem Description

When WKF (Workflow) files are ingested and later generated, the URIs for associated elements (Tasks, RequiredInstruments, Processes) are using the default `hadatac#` namespace prefix instead of the project-specific namespace (e.g., `pmsr:`).

### Example of the Problem

**Expected output:**
```
Required Instrument URIs: pmsr:RIN1770629000000200
Subtask URIs: pmsr:TSK1770629000000101; pmsr:TSK1770629000000102
```

**Actual output:**
```
Required Instrument URIs: https://hadatac.org/ont/hadatac#RIN1770629000000200
Subtask URIs: https://hadatac.org/ont/hadatac#TSK1770629000000101; https://hadatac.org/ont/hadatac#TSK1770629000000102
```

## Root Cause

The problem occurs during the **ingestion** phase, NOT during generation. When a WKF Excel file is uploaded:

1. The Excel file contains URIs in the `hasURI` column
2. These URIs are either:
   - **Created by the frontend** with a hardcoded `hadatac#` prefix
   - **Not using the namespace** defined in the Namespaces sheet of the Excel file

3. The `WKFGenerator.createRow()` method simply copies these URIs from the Excel into the triple store
4. Later, when the file is generated again, these incorrect URIs are retrieved and displayed

## Technical Flow

### 1. WKF Ingestion Flow

```
1. User uploads WKF Excel file via frontend
   ↓
2. Frontend/Backend creates WKF + DataFile records
   ↓
3. IngestionAPI.ingest() is called
   ↓
4. AnnotateWKF.exec() processes the file
   ↓
5. WKFGenerator.createRow() reads each row from Excel
   ↓
6. hasURI values are copied AS-IS from Excel
   ↓
7. Data is committed to triple store with these URIs
```

### 2. WKF Generation Flow

```
1. User requests to generate WKF file
   ↓
2. WKFGen.genByStatus() queries triple store
   ↓
3. WKFProcesses, WKFTasks, WKFRequiredInstruments query by WKF URI
   ↓
4. URIUtils.replaceNameSpaceEx() tries to convert full URIs to prefixed form
   ↓
5. Excel file is created with these URIs
```

## Debugging Steps Added

### 1. WKFGenerator Debug Logs

Added logging to `WKFGenerator.createRow()` to track:
- Row number and element type being processed
- hasURI values read from Excel
- Final hasURI values before committing to triple store
- DataFile URI being used

**Location:** `app/org/hascoapi/ingestion/WKFGenerator.java`

**Log output to look for:**
```
[WKFGenerator.createRow] Row #1, elementType=task
[WKFGenerator.createRow] hasURI from Excel: https://hadatac.org/ont/hadatac#TSK1770629000000101
[WKFGenerator.createRow] Final hasURI: https://hadatac.org/ont/hadatac#TSK1770629000000101
[WKFGenerator.createRow] DataFile URI: https://hadatac.org/ont/hadatac#/DFL1770629000000100
[WKFGenerator.createRow] Element type: task
```

## Solution Options

### Option 1: Fix Frontend URI Generation (Recommended)

The frontend should:
1. Read the `Namespaces` sheet from the WKF template
2. Use the first namespace (or project-specific namespace) to create URIs
3. Generate URIs like `pmsr:TSK<timestamp>` instead of `https://hadatac.org/ont/hadatac#TSK<timestamp>`

**Files to check in frontend:**
- WKF template generation code
- Where hasURI values are created for Tasks, RequiredInstruments, Processes, ProcessStems

### Option 2: Backend URI Normalization (Alternative)

The backend `WKFGenerator` could:
1. Read the Namespaces sheet first
2. Detect if URIs use `hadatac#` prefix
3. Replace with the correct project namespace
4. Store normalized URIs in triple store

**Implementation:**
```java
// In WKFGenerator.createRow()
if (row.containsKey("hasURI")) {
    String uri = row.get("hasURI").toString();
    
    // Check if using default hadatac namespace
    if (uri.contains("hadatac.org/ont/hadatac#")) {
        // Get project namespace from Namespaces sheet or DataFile
        String projectNamespace = getProjectNamespace();
        
        // Extract URI suffix (e.g., "TSK1770629000000101")
        String suffix = uri.substring(uri.lastIndexOf("#") + 1);
        
        // Rebuild URI with correct namespace
        String correctedUri = projectNamespace + ":" + suffix;
        row.put("hasURI", correctedUri);
    }
}
```

### Option 3: Template-Based URI Generation

The WKF template itself could include instructions for URI generation:
1. Define a `defaultNamespace` parameter in InfoSheet
2. Backend reads this parameter
3. All generated URIs use this namespace

## Testing the Fix

### 1. Create Test WKF File

Create a WKF Excel file with:
- Namespace sheet defining `pmsr: http://pmsr.net/ont/pmsr#`
- Tasks with URIs like `pmsr:TSK1770629000000101`
- RequiredInstruments with URIs like `pmsr:RIN1770629000000200`

### 2. Ingest the File

```bash
# Upload via frontend or API
POST /hascoapi/api/wkf/create/{wkfJson}
POST /hascoapi/api/uploadFile/{wkfUri}/{filename}
GET /hascoapi/api/ingest/mt/{wkfUri}
```

### 3. Check Logs

Look for:
```
[WKFGenerator.createRow] hasURI from Excel: pmsr:TSK1770629000000101
[WKFGenerator.createRow] Final hasURI: pmsr:TSK1770629000000101
```

### 4. Generate WKF File

```bash
GET /hascoapi/api/mt/dp/{wkfUri}
```

Check that the generated Excel has correct namespace prefixes.

### 5. Verify in Triple Store

Query Fuseki to confirm URIs are stored correctly:
```sparql
PREFIX pmsr: <http://pmsr.net/ont/pmsr#>
PREFIX vstoi: <http://hadatac.org/ont/vstoi#>

SELECT ?task ?label WHERE {
  GRAPH ?g {
    ?task a vstoi:Task .
    OPTIONAL { ?task rdfs:label ?label }
  }
}
```

## Files Modified

1. **WKFGenerator.java**
   - Added debug logging to `createRow()`
   - Tracks hasURI values from Excel
   - Shows final row data before commit

## Next Steps

1. **Run a test ingestion** with debug logs enabled
2. **Examine the logs** to see what hasURI values are coming from Excel
3. **Identify the source** of the `hadatac#` URIs (frontend or template)
4. **Implement the appropriate fix** based on findings

## Related Files

- `app/org/hascoapi/ingestion/WKFGenerator.java` - Ingestion generator for WKF
- `app/org/hascoapi/ingestion/AnnotateWKF.java` - WKF annotation orchestrator
- `app/org/hascoapi/transform/mt/wkf/WKFGen.java` - WKF Excel generation
- `app/org/hascoapi/transform/mt/wkf/WKFTasks.java` - Task sheet generation
- `app/org/hascoapi/transform/mt/wkf/WKFRequiredInstruments.java` - RequiredInstrument sheet generation
- `app/org/hascoapi/utils/URIUtils.java` - URI namespace conversion utilities

## Conclusion

The URI namespace problem in WKF is located in the **ingestion** phase, where URIs from the Excel file are stored as-is without namespace validation or correction. The solution should either:

1. Fix the **frontend** to generate correct namespace URIs
2. Add **backend validation** to normalize URIs during ingestion
3. Use **template-based** namespace configuration

The added debug logs will help identify exactly where the incorrect URIs are coming from.

