# DP2 Generation Issue - Missing Frontend Call

## Problem

The frontend is successfully **registering** DP2 metadata, but **NOT calling the generation endpoint** to create the Excel file.

### What's Happening Now (❌ INCOMPLETE)

```
1. ✅ Frontend creates DataFile → POST /hascoapi/api/datafile/create/{json}
2. ✅ Frontend creates DP2 metadata → POST /hascoapi/api/dp2/create/{json}
3. ❌ MISSING: Frontend should call generation endpoint
4. ❌ Frontend tries to download file that doesn't exist → 404 error
```

### Backend Logs Confirm This

```
Type: [datafile]  JSON [{"uri":"https://hadatac.org/ont/hadatac#/DFL1770907845634971",...}]
Type: [dp2]  JSON [{"uri":"https://hadatac.org/ont/hadatac#/DP21770907845634971",...}]
[ERROR] IngestionAPI.mtGetGenerated(): File not found - C:\hascoapi\var\asdasdas.xlsx
```

**Notice**: No generation logs appear (no `========== DP2Gen.genByStatus() START ==========`)

This means the generation endpoint was **never called**.

## Solution

The frontend **MUST** call the generation endpoint after creating the DP2 metadata.

### Required Frontend Flow (✅ COMPLETE)

```
1. Create DataFile
   POST /hascoapi/api/datafile/create/{datafileJson}
   Response: { uri: "DFL...", ... }

2. Create DP2 metadata
   POST /hascoapi/api/dp2/create/{dp2Json}
   Response: { uri: "DP2...", hasDataFileUri: "DFL...", ... }

3. ⭐ CALL GENERATION ENDPOINT (THIS IS MISSING!)
   GET/POST /hascoapi/api/mt/gen/perstatus/dp2/{dataFileUri}/DRAFT/{filename}/null/null
   OR
   GET/POST /api/mt/gen/perstatus/dp2/{dataFileUri}/DRAFT/{filename}/null/null
   
   Response: { message: "asdasdas.xlsx", isSuccessful: true }

4. Download generated file
   POST /hascoapi/api/mt/get/generated/{filename}
   Response: Binary Excel file
```

## Generation Endpoint Details

### Endpoint Options (both work)

**Standard** (recommended):
```
GET  /hascoapi/api/mt/gen/perstatus/:elementtype/:datafileuri/:status/:filename/:mediafolder/:verifyuri
POST /hascoapi/api/mt/gen/perstatus/:elementtype/:datafileuri/:status/:filename/:mediafolder/:verifyuri
```

**Legacy** (for backward compatibility):
```
GET  /api/mt/gen/perstatus/:elementtype/:datafileuri/:status/:filename/:mediafolder/:verifyuri
POST /api/mt/gen/perstatus/:elementtype/:datafileuri/:status/:filename/:mediafolder/:verifyuri
```

### Parameters

| Parameter | Value | Required | Example |
|-----------|-------|----------|---------|
| elementtype | `dp2` | ✅ Yes | `dp2` |
| datafileuri | DataFile URI (URL-encoded) | ✅ Yes | `https%3A%2F%2Fhadatac.org%2Font%2Fhadatac%23%2FDFL1770907845634971` |
| status | DP2 status | ✅ Yes | `DRAFT` or `WORKING` |
| filename | Output filename | ✅ Yes | `asdasdas.xlsx` |
| mediafolder | Media folder (not used) | ❌ No | `null` |
| verifyuri | Verification URI (not used) | ❌ No | `null` |

### Example Request

**JavaScript/PHP**:
```javascript
// After creating DP2 successfully
const dataFileUri = dp2Response.hasDataFileUri; // e.g., "https://hadatac.org/ont/hadatac#/DFL..."
const filename = "asdasdas.xlsx";
const status = "DRAFT";

// URL-encode the dataFileUri
const encodedUri = encodeURIComponent(dataFileUri);

// Call generation endpoint
const genUrl = `/hascoapi/api/mt/gen/perstatus/dp2/${encodedUri}/${status}/${filename}/null/null`;

const genResponse = await fetch(genUrl, {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${jwtToken}`,
    'Content-Type': 'application/json'
  }
});

const result = await genResponse.json();
console.log('Generation result:', result);
// Expected: { message: "asdasdas.xlsx", isSuccessful: true }

// Now can download
const downloadUrl = `/hascoapi/api/mt/get/generated/${filename}`;
// ... download file ...
```

**cURL Example**:
```bash
# Create DataFile
curl -X POST "http://localhost:9000/hascoapi/api/datafile/create/..." \
  -H "Authorization: Bearer $JWT_TOKEN"

# Create DP2
curl -X POST "http://localhost:9000/hascoapi/api/dp2/create/..." \
  -H "Authorization: Bearer $JWT_TOKEN"

# ⭐ GENERATE FILE (THIS IS WHAT'S MISSING!)
DATAFILE_URI="https://hadatac.org/ont/hadatac#/DFL1770907845634971"
ENCODED_URI=$(echo "$DATAFILE_URI" | jq -sRr @uri)

curl -X POST "http://localhost:9000/hascoapi/api/mt/gen/perstatus/dp2/$ENCODED_URI/DRAFT/asdasdas.xlsx/null/null" \
  -H "Authorization: Bearer $JWT_TOKEN"

# Download file
curl -X POST "http://localhost:9000/hascoapi/api/mt/get/generated/asdasdas.xlsx" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -o asdasdas.xlsx
```

## Expected Backend Logs (When Fixed)

When the generation endpoint is called, you should see:

```
Type: [datafile]  JSON [...]
Type: [dp2]  JSON [...]

========== IngestionAPI.mtGenByStatus() START ==========
Parameters:
  elementtype: dp2
  datafileuri: https://hadatac.org/ont/hadatac#/DFL1770907845634971
  status: DRAFT
  filename: asdasdas.xlsx
  mediafolder: null
  verifyuri: null

========== DP2Gen.genByStatus() START ==========
Input parameters:
  dataFileUri: https://hadatac.org/ont/hadatac#/DFL1770907845634971
  status: DRAFT
  filename: asdasdas.xlsx
  baseName (after extraction): asdasdas.xlsx
  basePath (from ConfigProp): C:\hascoapi\var\
  outFilename (final): C:\hascoapi\var\asdasdas.xlsx

[DP2Gen] graph(https) tripleCount=7
[DP2Gen] graph(https) counts deployments=0 platforms=0 ...

========== DP2Gen.save() START ==========
  filename parameter: C:\hascoapi\var\asdasdas.xlsx
  ✓ helper and workbook are valid
  → Saving namespaces...
  ✓ Namespaces saved
  Output file absolute path: C:\hascoapi\var\asdasdas.xlsx
  → Writing workbook to file...
  ✓ File written successfully
  File size: 12345 bytes
========== DP2Gen.save() END (SUCCESS) ==========

✓ Generation completed successfully
========== IngestionAPI.mtGenByStatus() END (SUCCESS) ==========
```

## Frontend Code Location

The generation call is likely missing from:
- Drupal module: `rep`
- Form/Controller handling DP2 creation
- Probably in a file like:
  - `AddMTForm.php` or `GenerateMTForm.php`
  - `REPSelectMTForm.php` (if generation happens after selection)

### Where to Add the Call

Look for code that:
1. ✅ Calls `/hascoapi/api/datafile/create/...` 
2. ✅ Calls `/hascoapi/api/dp2/create/...`
3. ❌ **MISSING**: Should call `/hascoapi/api/mt/gen/perstatus/dp2/...`

Add the generation call **between** steps 2 and 3 (before attempting download).

## Common Frontend Mistakes

### ❌ Mistake 1: Assuming Generation Happens Automatically
```php
// BAD: Creates DP2, then immediately tries to download
$api->elementAdd('dp2', $mtJson);
$file = $api->mtGetGenerated($filename); // ❌ File doesn't exist yet!
```

### ✅ Correct: Explicitly Call Generation
```php
// GOOD: Creates DP2, generates file, then downloads
$api->elementAdd('dp2', $mtJson);

// ⭐ THIS IS THE MISSING CALL
$genResult = $api->mtGenByStatus('dp2', $dataFileUri, 'DRAFT', $filename);

if ($genResult->isSuccessful) {
  $file = $api->mtGetGenerated($filename); // ✅ Now file exists
}
```

### ❌ Mistake 2: Wrong Endpoint
```php
// BAD: Calling wrong endpoint
$api->generateDP2($mtUri); // ❌ This endpoint doesn't exist
```

### ✅ Correct: Use mtGenByStatus
```php
// GOOD: Using correct endpoint
$api->mtGenByStatus('dp2', $dataFileUri, 'DRAFT', $filename);
```

## How Other MTs Work (Reference)

### DSG (Working Example)
```
1. Create DataFile
2. Create DSG metadata
3. ⭐ Call /api/mt/gen/perstatus/dsg/{dataFileUri}/DRAFT/{filename}/null/null
4. Download generated file
```

### INS (Working Example)
```
1. Create DataFile
2. Create INS metadata
3. ⭐ Call /api/mt/gen/perstatus/ins/{dataFileUri}/DRAFT/{filename}/null/null
4. Download generated file
```

### DP2 (Currently Broken)
```
1. Create DataFile
2. Create DP2 metadata
3. ❌ MISSING: Should call /api/mt/gen/perstatus/dp2/{dataFileUri}/DRAFT/{filename}/null/null
4. Try to download → 404 because file was never generated
```

## Testing the Fix

### Backend Test (Works Now)
```bash
# 1. Create test DP2 through UI or API
# 2. Get the DataFile URI from response
# 3. Test generation directly

DATAFILE_URI="https://hadatac.org/ont/hadatac#/DFL1770907845634971"
ENCODED_URI=$(echo "$DATAFILE_URI" | jq -sRr @uri)

curl -X POST "http://localhost:9000/hascoapi/api/mt/gen/perstatus/dp2/$ENCODED_URI/DRAFT/test.xlsx/null/null" \
  -H "Authorization: Bearer $JWT_TOKEN"

# Should see generation logs in backend console
# File should be created at C:\hascoapi\var\test.xlsx
```

### Frontend Test (After Fix)
1. Login to Drupal
2. Navigate to "Add MT" → DP2
3. Fill form and submit
4. Check browser Network tab:
   - ✅ Should see POST to `/datafile/create`
   - ✅ Should see POST to `/dp2/create`
   - ✅ **Should see POST to `/mt/gen/perstatus/dp2/...`** ⭐ THIS IS WHAT TO CHECK
   - ✅ Should see POST to `/mt/get/generated`
5. File should download successfully

## Summary

### Problem
Frontend is creating DP2 metadata but **not calling the generation endpoint**.

### Solution
Add a call to `/api/mt/gen/perstatus/dp2/{dataFileUri}/{status}/{filename}/null/null` **after** creating the DP2 metadata and **before** attempting to download the file.

### Required Change
In the frontend code (Drupal PHP), add:
```php
// After $api->elementAdd('dp2', $mtJson);
$genResult = $api->mtGenByStatus('dp2', $dataFileUri, 'DRAFT', $filename, null, null);
```

### How to Verify Fix Works
Check backend logs for:
```
========== DP2Gen.genByStatus() START ==========
...
✓ File written successfully
========== DP2Gen.save() END (SUCCESS) ==========
```

---

**Status**: Issue Identified  
**Root Cause**: Missing frontend API call  
**Backend**: ✅ Working correctly  
**Frontend**: ❌ Needs to add generation call  
**Date**: 2026-02-12
