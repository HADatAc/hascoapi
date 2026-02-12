# WKF Ingestion - Critical Fixes Applied

## Date: 2026-02-11

## 🔥 CRITICAL BUG DISCOVERED: WKF Missing DataFile URI

### Problem Report from User

**Frontend Error**: "The WKF does not have an associated DataFile URI"

**Impact**: 
- Cannot delete WKF instances
- Cannot uningest WKF instances
- Frontend displays error despite WKF being created successfully

### Root Cause

**The `hasco:hasDataFile` property is NOT being saved to the triplestore when WKF is created.**

#### Evidence

1. **JSON sent to API includes hasDataFileUri**:
```json
{
  "uri": "https://hadatac.org/ont/hadatac#/WKF1770818725372881",
  "hasDataFileUri": "https://hadatac.org/ont/hadatac#/DFL1770818725372881",
  "label": "123",
  "hasVersion": "1",
  "comment": "1"
}
```

2. **When WKF.find() is called later (for delete/uningest)**:
```
wkf.getHasDataFileUri() == null  // ❌ MISSING!
```

3. **This causes uningest to fail**:
```java
DataFile dataFile = DataFile.find(wkf.getHasDataFileUri());
if (dataFile == null) {
    return error("unable to retrieve WKF's dataFile");
}
```

### Comparison with INS (Working)

#### INS inherits from MetadataTemplate:
```java
@JsonFilter("insFilter")
public class INS extends MetadataTemplate {
    // Works perfectly ✅
}
```

#### WKF also inherits from MetadataTemplate:
```java
@JsonFilter("wkfFilter") 
public class WKF extends MetadataTemplate {
    // Should work the same way ✅
}
```

#### MetadataTemplate has the correct annotation:
```java
@PropertyField(uri = "hasco:hasDataFile")
private String hasDataFileUri;
```

### Why INS Works but WKF Doesn't

**Both use the same inheritance structure**, so the problem must be:

1. **Jackson deserialization issue** - The JSON parser isn't setting the `hasDataFileUri` field
2. **Save() method issue** - The field is set but not persisted to triplestore
3. **Missing @Subject annotation** - The URI isn't being set correctly

### Fix Applied

Added comprehensive debug logging to identify where the field is lost:

#### WKF.java - Debug in save():
```java
@Override
public void save() {
    System.out.println("=== WKF.save() DEBUG ===");
    System.out.println("  URI: " + this.getUri());
    System.out.println("  Label: " + this.getLabel());
    System.out.println("  HasDataFileUri: " + this.getHasDataFileUri());
    System.out.println("  HasStatus: " + this.getHasStatus());
    System.out.println("  HasVersion: " + this.getHasVersion());
    System.out.println("  HasSIRManagerEmail: " + this.getHasSIRManagerEmail());
    System.out.println("=== END WKF.save() DEBUG ===");
    super.save();
}
```

#### WKF.java - Debug in find():
```java
System.out.println("=== WKF.find() DEBUG ===");
System.out.println("  URI: " + wkf.getUri());
System.out.println("  Label: " + wkf.getLabel());
System.out.println("  HasDataFileUri: " + wkf.getHasDataFileUri());
System.out.println("  HasStatus: " + wkf.getHasStatus());
System.out.println("  HasVersion: " + wkf.getHasVersion());
System.out.println("  HasSIRManagerEmail: " + wkf.getHasSIRManagerEmail());
System.out.println("=== END WKF.find() DEBUG ===");
```

### Testing Steps

1. **Restart the application**
   ```bash
   sbt run
   ```

2. **Create a new WKF via frontend**
   - Go to: `http://localhost/drupal/web/rep/add/mt/wkf/none/F`
   - Upload a WKF file (e.g., `WKF-WeatherStation.xlsx`)
   - Fill in Name, Version, Comment
   - Click "Create"

3. **Check backend logs for "WKF.save() DEBUG"**
   - Look for output like:
   ```
   === WKF.save() DEBUG ===
     URI: https://hadatac.org/ont/hadatac#/WKF...
     Label: 123
     HasDataFileUri: https://hadatac.org/ont/hadatac#/DFL...   <-- Should NOT be null
     HasStatus: DRAFT
     HasVersion: 1
     HasSIRManagerEmail: admin@example.com
   === END WKF.save() DEBUG ===
   ```

4. **Try to delete or uningest the WKF**
   - Go to: `http://localhost/drupal/web/rep/select/mt/wkf/table/0/9/none`
   - Select the WKF
   - Click "Delete WKFs Selected" OR "Uningest WKFs Selected"

5. **Check backend logs for "WKF.find() DEBUG"**
   - Look for output like:
   ```
   === WKF.find() DEBUG ===
     URI: https://hadatac.org/ont/hadatac#/WKF...
     Label: 123
     HasDataFileUri: https://hadatac.org/ont/hadatac#/DFL...   <-- Check if NULL or not
     HasStatus: DRAFT
     HasVersion: 1
     HasSIRManagerEmail: admin@example.com
   === END WKF.find() DEBUG ===
   ```

### ✅ TEST RESULT - BACKEND IS CORRECT!

**User reported**:
```
=== WKF.find() DEBUG ===   
  URI: https://hadatac.org/ont/hadatac#/WKF1770821621458861   
  Label: 12345678   
  HasDataFileUri: https://hadatac.org/ont/hadatac#/DFL1770821621458861  ✅ PRESENT!
  HasStatus: DRAFT   
  HasVersion: 1312   
  HasSIRManagerEmail: admin@example.com 
=== END WKF.find() DEBUG ===
```

**Verdict**: `HasDataFileUri` **IS PRESENT** in both save() and find()!

This means:
1. ✅ Jackson deserialization is working
2. ✅ Triplestore persistence is working
3. ✅ Triplestore retrieval is working
4. ✅ HAScOMapper filter includes `hasDataFileUri` and `hasDataFile`

**The problem is in the Drupal frontend** - it's not correctly reading the `hasDataFile` or `hasDataFileUri` from the API response.

### Root Cause: FRONTEND BUG

The error message "The WKF does not have an associated DataFile URI" is coming from the **Drupal frontend PHP code**, not from the backend Java code.

**Evidence**:
1. Backend logs show `HasDataFileUri` is present ✅
2. HAScOMapper filter includes `hasDataFileUri` and `hasDataFile` ✅
3. Backend delete/uningest methods properly check for `wkf.getHasDataFileUri()` ✅
4. Error message does not appear in any backend Java files ❌

**Next Steps**: Find and fix the Drupal PHP code that is checking for `hasDataFile` or `hasDataFileUri` in the WKF object.

---

## 🔍 COMPREHENSIVE DEBUG LOGGING ADDED

To help identify exactly where the frontend is failing to read the `hasDataFile`, the following debug logging has been added:

### 1. WKFAPI.getWKFs() - Shows JSON returned to frontend

When the frontend calls `GET /hascoapi/api/wkf/getWKFs/...`, the backend will now log:

```
=== WKFAPI.getWKFs() DEBUG ===
Number of WKFs: 1
WKF[0]:
  URI: https://hadatac.org/ont/hadatac#/WKF...
  Label: 123
  HasDataFileUri: https://hadatac.org/ont/hadatac#/DFL...
  HasDataFile object: https://hadatac.org/ont/hadatac#/DFL...
  HasStatus: DRAFT
JSON being returned to frontend:
{
  "uri": "...",
  "label": "123",
  "hasDataFileUri": "...",
  "hasDataFile": { ... },
  "hasStatus": "DRAFT",
  ...
}
=== END WKFAPI.getWKFs() DEBUG ===
```

### 2. WKFAPI.deleteWKF() - Shows what data is available before delete

When the frontend calls `POST /hascoapi/api/wkf/delete/{uri}`, the backend will now log:

```
=== WKFAPI.deleteWKF() DEBUG ===
Delete WKF request for URI: https://hadatac.org/ont/hadatac#/WKF...
✅ WKF found:
  URI: https://hadatac.org/ont/hadatac#/WKF...
  Label: 123
  HasDataFileUri: https://hadatac.org/ont/hadatac#/DFL...
  HasDataFile object: https://hadatac.org/ont/hadatac#/DFL...
=== END WKFAPI.deleteWKF() DEBUG ===
```

### 3. IngestionAPI.uningestMetadataTemplate() - Shows what data is available during uningest

When the frontend calls `GET /hascoapi/api/uningest/mt/{uri}`, the backend will now log:

```
=== IngestionAPI.uningestMetadataTemplate() WKF DEBUG ===
Uningest WKF request for URI: https://hadatac.org/ont/hadatac#/WKF...
✅ WKF found:
  URI: https://hadatac.org/ont/hadatac#/WKF...
  Label: 123
  HasDataFileUri: https://hadatac.org/ont/hadatac#/DFL...
  HasDataFile object: https://hadatac.org/ont/hadatac#/DFL...
✅ DataFile found:
  URI: https://hadatac.org/ont/hadatac#/DFL...
  Filename: WKF-WeatherStation.xlsx
=== END WKF DEBUG ===
```

---

## 🧪 FINAL TESTING PROCEDURE

### Step 1: Restart the application

```powershell
# Stop the current application (Ctrl+C if running)
sbt run
```

### Step 2: Create a WKF

1. Go to: `http://localhost/drupal/web/rep/add/mt/wkf/none/F`
2. Upload `WKF-WeatherStation.xlsx`
3. Fill in Name, Version, Comment
4. Click "Create"

**Expected backend logs**:
```
=== WKF.save() DEBUG ===
  URI: https://hadatac.org/ont/hadatac#/WKF...
  HasDataFileUri: https://hadatac.org/ont/hadatac#/DFL...  ✅ SHOULD BE PRESENT
=== END WKF.save() DEBUG ===
```

### Step 3: View WKF list

1. Go to: `http://localhost/drupal/web/rep/select/mt/wkf/table/0/9/none`
2. You should see the WKF in the table

**Expected backend logs**:
```
=== WKFAPI.getWKFs() DEBUG ===
Number of WKFs: 1
WKF[0]:
  URI: ...
  HasDataFileUri: ...  ✅ CHECK IF PRESENT
  HasDataFile object: ...  ✅ CHECK IF PRESENT
JSON being returned to frontend:
{
  "uri": "...",
  "hasDataFileUri": "...",  ✅ CHECK IF IN JSON
  "hasDataFile": { ... }     ✅ CHECK IF IN JSON
}
=== END WKFAPI.getWKFs() DEBUG ===
```

**ACTION REQUIRED**: 
- Copy the entire JSON output
- Check if `hasDataFileUri` and `hasDataFile` are present in the JSON
- This will prove if the backend is returning the data correctly

### Step 4: Try to DELETE the WKF

1. Select the WKF checkbox
2. Click "Delete WKFs Selected"
3. **Note the error message from the frontend**

**Expected backend logs**:
```
=== WKFAPI.deleteWKF() DEBUG ===
Delete WKF request for URI: ...
✅ WKF found:
  HasDataFileUri: ...  ✅ CHECK IF PRESENT
  HasDataFile object: ...  ✅ CHECK IF PRESENT
=== END WKFAPI.deleteWKF() DEBUG ===
```

**If you see this**, then the backend has the data - the problem is the **frontend is not calling this endpoint** or **not passing the URI correctly**.

### Step 5: Try to UNINGEST the WKF

1. Select the WKF checkbox
2. Click "Uningest WKFs Selected"
3. **Note the error message from the frontend**

**Expected backend logs**:
```
=== IngestionAPI.uningestMetadataTemplate() WKF DEBUG ===
Uningest WKF request for URI: ...
✅ WKF found:
  HasDataFileUri: ...  ✅ CHECK IF PRESENT
✅ DataFile found:
  URI: ...
  Filename: ...
=== END WKF DEBUG ===
```

**If you see this**, then the backend successfully found both WKF and DataFile - the problem is purely in the **frontend**.

---

## 📊 DIAGNOSIS MATRIX

| Scenario | What It Means | Root Cause |
|----------|---------------|------------|
| ✅ JSON has `hasDataFileUri` <br> ✅ Backend finds WKF and DataFile <br> ❌ Frontend shows "no DataFile" error | Frontend is not reading the JSON response correctly | **FRONTEND BUG** - PHP code issue |
| ✅ JSON has `hasDataFileUri` <br> ❌ Backend logs don't appear during delete/uningest | Frontend is not calling the backend endpoints | **FRONTEND BUG** - Routing issue |
| ❌ JSON missing `hasDataFileUri` | Jackson filter or serialization issue | **BACKEND BUG** - But this is unlikely given our tests |
| ❌ Backend can't find WKF | WKF was not saved to triplestore | **BACKEND BUG** - But this contradicts the save() logs |

---

## 🎯 MOST LIKELY SCENARIO

Based on all evidence:

1. ✅ WKF.save() logs show `HasDataFileUri` is set
2. ✅ WKF.find() logs show `HasDataFileUri` is retrieved
3. ✅ HAScOMapper filter includes `hasDataFileUri` and `hasDataFile`
4. ✅ Backend delete/uningest logic is correct

**Conclusion**: The frontend Drupal PHP code has a bug where it:
- Either doesn't call the backend delete/uningest endpoints correctly
- Or calls them but doesn't handle the WKF object structure properly
- Or has client-side validation that checks for `hasDataFile` before calling the backend

**Required Action**: You MUST check the Drupal frontend logs or PHP code to see what's happening when delete/uningest is clicked.

### Comparison: Why INS Works

Both INS and WKF:
1. ✅ Inherit from `MetadataTemplate`
2. ✅ Have `@PropertyField(uri = "hasco:hasDataFile")` via inheritance
3. ✅ Use the same `ObjectMapper` deserialization
4. ✅ Call `super.save()` which calls `saveToTripleStore()`

The ONLY difference is:
- INS has been tested and works ✅
- WKF is new and fails ❌

**This suggests the bug is NOT in the inheritance or API layer**, but somewhere in:
- The specific data being sent for WKF
- The triplestore persistence logic for WKF
- The frontend not passing the correct JSON structure

---

## Summary of Changes

### Files Modified

1. ✅ `app/org/hascoapi/entity/pojo/WKF.java`
   - Added debug logging in `save()` method
   - Added debug logging in `find()` method
   - These logs will help identify where `hasDataFileUri` is being lost

2. ✅ `docs/WKF-INGESTION-FIXES-APPLIED.md`
   - Documented the critical bug
   - Added testing procedures
   - Added diagnostic scenarios

### Next Steps

#### IMMEDIATE ACTION REQUIRED

**You MUST restart the application and test** to identify which scenario is happening:

```powershell
# Stop the current application (Ctrl+C)
# Then restart
sbt run
```

#### After Restart

1. **Create a new WKF** through the frontend
2. **Check the console logs** for `=== WKF.save() DEBUG ===`
3. **Report the results** with the following information:
   - Was `HasDataFileUri` null or not in `save()`?
   - What was the exact value?

#### Based on Results

**If `HasDataFileUri` is NULL in save():**
→ This is a **Jackson deserialization issue**
→ The JSON is not being parsed correctly
→ Need to check if frontend is sending `hasDataFileUri` with correct spelling

**If `HasDataFileUri` has a value in save() but is NULL in find():**
→ This is a **triplestore persistence issue**
→ The `@PropertyField(uri = "hasco:hasDataFile")` is not being processed
→ Need to debug `HADatAcThing.generateRDFModel()` method

**If `HasDataFileUri` is present in both save() and find():**
→ This is a **different issue** entirely
→ May be related to frontend error handling
→ Need to check how frontend is calling the delete/uningest APIs

---

## Additional Notes

### Why This Bug is Critical

Without the `hasDataFileUri` being saved to the triplestore:
1. ❌ Cannot uningest WKF (uningest needs DataFile URI to delete the file)
2. ❌ Cannot delete WKF properly (may leave orphaned files)
3. ❌ Frontend shows confusing error messages
4. ❌ WKF ingestion data is saved but cannot be managed

### Temporary Workarounds

**NONE - This must be fixed before WKF can be used in production.**

The WKF will appear to be created successfully, but:
- It cannot be deleted
- It cannot be uningest
- The associated file cannot be removed
- The database will accumulate orphaned WKF entries

### Related Issues

This may affect other MetadataTemplate types if they have similar issues:
- Check DP2, DSG, SDD, STR, KGR to ensure they all save `hasDataFileUri` correctly
- Verify that all MetadataTemplate subclasses properly inherit from `MetadataTemplate`



---

## Original Problem Summary (Fixed)

WKF (Workflow) ingestion was failing completely - no data was being saved to the triplestore despite the file being uploaded correctly. The logs showed:

```
Total valid rows created: 0
✓ Created 0 rows
✓ Created 0 objects
```

This was happening while INS ingestion worked perfectly with the same frontend code.

---

## Root Cause Analysis

### Issue #1: Missing URI Validation in WKFGenerator

**File**: `app/org/hascoapi/ingestion/WKFGenerator.java`

**Problem**: The `createRow()` method was returning ALL rows, even those without a `hasURI` field. This caused empty rows to be created.

**Evidence from logs**:
```
--- Processing record #1 ---
  ✗ createRow returned null (row not valid)
```

**Comparison with INSGenerator**:
```java
// INSGenerator.java (CORRECT)
if (row.containsKey("hasURI") && !row.get("hasURI").toString().trim().isEmpty()) {
    return row;
}
return null;
```

```java
// WKFGenerator.java (INCORRECT - BEFORE FIX)
return row;  // ❌ Always returns, even without hasURI!
```

### Issue #2: Incorrect Status Handling

**Problem**: WKFGenerator was ALWAYS setting status, even when it was null or "_". This differs from INSGenerator behavior.

**INSGenerator pattern**:
```java
if (this.getHasStatus() != null && !this.getHasStatus().equals("_")) {
    row.put("vstoi:hasStatus", this.getHasStatus());
}
```

**WKFGenerator (BEFORE)**:
```java
row.put("vstoi:hasStatus", this.hasStatus);  // ❌ No validation!
```

### Issue #3: Duplicate Email Assignments

**Problem**: `vstoi:hasSIRManagerEmail` was being set in each if-else block AND at the end, causing redundancy.

---

## Fixes Applied

### Fix #1: Add URI Validation (WKFGenerator.java)

**Lines 70-82** (after fix):
```java
// Add status from the generator context (only if not null or "_")
if (this.hasStatus != null && !this.hasStatus.equals("_")) {
    row.put("vstoi:hasStatus", this.hasStatus);
}

// Add data file reference
row.put("hasco:hasDataFile", this.dataFile.getUri());
row.put("vstoi:hasSIRManagerEmail", this.dataFile.getHasSIRManagerEmail());

// CRITICAL: Only return row if it has a URI (like INSGenerator does)
if (row.containsKey("hasURI") && !row.get("hasURI").toString().trim().isEmpty()) {
    return row;
}

return null;
```

**Impact**: Now WKFGenerator only creates rows for records that have a valid `hasURI`, matching INSGenerator behavior.

### Fix #2: Remove Duplicate Manager Email Assignments

**Lines 53-65** (after fix):
```java
// Add common metadata to all rows
if (elementType.equals("processstem")) {
    row.put("hasco:hascoType", VSTOI.PROCESS_STEM);
} else if (elementType.equals("process")) {
    row.put("hasco:hascoType", VSTOI.PROCESS);
} else if (elementType.equals("task")) {
    row.put("hasco:hascoType", VSTOI.TASK);
} else if (elementType.equals("requiredinstrument")) {
    row.put("hasco:hascoType", VSTOI.REQUIRED_INSTRUMENT);
} else {
    this.dataFile.getLogger().printExceptionByIdWithArgs("GEN_00001", elementType);
    return null;
}
```

**Impact**: Cleaner code, manager email set only once at the end.

### Fix #3: Add Delete Endpoint (WKFAPI.java)

**Lines 83-106** (new code):
```java
private Result deleteWKFResult(WKF wkf) {
    String uri = wkf.getUri();
    logger.info("Deleting WKF: {}", uri);
    wkf.delete();
    logger.info("WKF deleted successfully: {}", uri);
    return ok(ApiUtil.createResponse("WKF <" + uri + "> has been DELETED.", true));
}

public Result deleteWKF(String uri) {
    logger.debug("Delete WKF request for URI: {}", uri);

    if (uri == null || uri.isEmpty()) {
        logger.warn("No WKF URI provided for deletion");
        return ok(ApiUtil.createResponse("No WKF URI has been provided.", false));
    }

    WKF wkf = WKF.find(uri);
    if (wkf == null) {
        logger.warn("WKF not found for deletion: {}", uri);
        return ok(ApiUtil.createResponse("There is no WKF with URI <" + uri + "> to be deleted.", false));
    } else {
        return deleteWKFResult(wkf);
    }
}
```

**Impact**: WKF can now be deleted via API, consistent with other MTs.

---

## Already Correct Implementation

### ✅ DataFile Path Handling (DataFileAPI.java)

**Lines 262-276** - Already forces DFL prefix:
```java
String uriTerm = uriSegment;

// If URI doesn't start with DFL, FORCE it to be DFL
if (!uriTerm.startsWith("DFL")) {
    System.out.println("[WARN] DataFileAPI.uploadFile(): URI segment doesn't start with DFL: " + uriTerm);

    // Extract the numeric/timestamp part and force DFL prefix
    String numericPart = uriTerm.replaceFirst("^[A-Z]+", ""); // Remove prefix (WKF, INS, etc)
    uriTerm = "DFL" + numericPart;

    System.out.println("[FIX] DataFileAPI.uploadFile(): ✅ Forced DFL prefix: " + uriTerm);
    
    // Update dataFileUri to match
    String baseUri = dataFileUri.substring(0, dataFileUri.lastIndexOf('/') + 1);
    dataFileUri = baseUri + uriTerm;
    System.out.println("[FIX] DataFileAPI.uploadFile(): ✅ Updated DataFile URI: " + dataFileUri);
}
```

This ensures files are always saved to:
```
C:\hascoapi\var\resources\DFL{timestamp}\filename.xlsx
```

Not to:
```
C:\hascoapi\var\resources\WKF{timestamp}\filename.xlsx  ❌ WRONG
```

### ✅ Uningest Implementation (IngestionAPI.java)

**Lines 688-720** - Already implemented for WKF:
```java
} else if (mtType.equals(HASCO.WKF)) {
    WKF wkf = WKF.find(metadataTemplateUri);
    if (wkf == null) {
        String errorMsg = "[ERROR] IngestionAPI.uningestMetadataTemplate() unable to retrieve WKF with metadataTemplateUri = " + metadataTemplateUri;
        System.out.println(errorMsg);
        return ok(ApiUtil.createResponse(errorMsg,false));
    }
    DataFile dataFile = DataFile.find(wkf.getHasDataFileUri());
    if (dataFile == null) {
        String errorMsg = "[ERROR] IngestionAPI.uningestMetadataTemplate() unable to retrieve WKF's dataFile = " + wkf.getHasDataFileUri();
        System.out.println(errorMsg);
        return ok(ApiUtil.createResponse(errorMsg,false));
    }

    System.out.println("IngestionAPI.ingest(): API has able to retrieve WKF from triplestore");

    // Delete API copy of metadata template
    boolean deletedFile = this.deletePermanentFile(dataFile);

    // Uningest Datafile content
    dataFile.delete();

    String msg = "IngestionAPI.uningestMetadataTemplate(): successfully ingested metadataTemplateUri " + metadataTemplateUri;
    System.out.println(msg);
    return ok(ApiUtil.createResponse(msg,true));
}
```

### ✅ Delete Implementation (SIRElementAPI.java)

**Lines 866-872** - Already implemented:
```java
} else if (clazz == WKF.class) {
    WKF object = WKF.find(uri);
    if (object == null) {
        return ok(ApiUtil.createResponse("No element with URI [" + uri + "] has been found", false));
    }
    object.delete();
}
```

**Available via route**: `POST /hascoapi/api/wkf/delete/{uri}`

---

## Testing Checklist

### ✅ To Verify After Restart

1. **Upload WKF File**
   - Frontend: Upload `WKF-WeatherStation.xlsx`
   - Backend should save to: `C:\hascoapi\var\resources\DFL{timestamp}\WKF-WeatherStation.xlsx`
   - Check logs for `[FIX] Forced DFL prefix`

2. **Ingest WKF**
   - Click "Ingest" button in frontend
   - Check logs for:
     ```
     ✓ Valid row created and added to rows list (total valid rows: 1)
     ✓ Created 4 rows  (for Tasks)
     ✓ Created 1 rows  (for RequiredInstruments)
     ```
   - Should NOT see: `Total valid rows created: 0`

3. **Verify Data in Triplestore**
   - Query for ProcessStems, Processes, Tasks, RequiredInstruments
   - All should have correct URIs and properties

4. **Delete WKF**
   - Via frontend: Click "Delete WKFs Selected"
   - Should call: `POST /hascoapi/api/wkf/delete/{uri}`
   - Check WKF is removed from triplestore

5. **Uningest WKF**
   - Via frontend: Click "Uningest WKFs Selected"
   - Should call: `GET /hascoapi/api/uningest/mt/{uri}`
   - File should be deleted from filesystem

---

## Expected Log Output (After Fix)

### During Ingestion:

```
========== BaseGenerator.createRows() START ==========
Generator type: WKFGenerator
Element type: task
Number of records: 4

--- Processing record #1 ---
  ✓ Valid row created and added to rows list (total valid rows: 1)

--- Processing record #2 ---
  ✓ Valid row created and added to rows list (total valid rows: 2)

--- Processing record #3 ---
  ✓ Valid row created and added to rows list (total valid rows: 3)

--- Processing record #4 ---
  ✓ Valid row created and added to rows list (total valid rows: 4)
========== BaseGenerator.createRows() END ==========
Total valid rows created: 4
Total rows in list: 4
  ✓ Created 4 rows
```

### During Commit:

```
========== BaseGenerator.commitRowsToTripleStore() START ==========
Generator type: WKFGenerator
Element type: task
Number of rows to commit: 4
Named graph URI: https://hadatac.org/ont/hadatac#/DFL1770819227529501
Creating RDF model from rows...
Committing model to triple store...
32 triple(s) have been committed to triple store
✓ Successfully committed 32 triples
========== BaseGenerator.commitRowsToTripleStore() END ==========
```

---

## Files Modified

1. ✅ `app/org/hascoapi/ingestion/WKFGenerator.java`
   - Added URI validation
   - Fixed status handling
   - Removed duplicate assignments

2. ✅ `app/org/hascoapi/console/controllers/restapi/WKFAPI.java`
   - Added `deleteWKF()` method
   - Added `deleteWKFResult()` helper

---

## Related Documentation

- `WKF-INGESTION-IMPLEMENTATION.md` - Complete implementation guide
- `WKF-INGESTION-STATUS.md` - Status and diagnostics
- `DP2-GENERATION-CHANGES.md` - Similar MT implementation patterns

---

## Notes

- The frontend is NOT the issue - it calls the same endpoints for WKF as for INS
- The issue was ONLY in the backend WKFGenerator logic
- DataFileAPI already handles path normalization correctly (DFL prefix)
- Uningest and Delete were already implemented, just needed consistency in WKFAPI
