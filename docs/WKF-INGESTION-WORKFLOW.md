# WKF Ingestion Workflow - Complete Guide

## Problem Summary

The WKF ingestion is failing with the error:
```
[ERROR] IngestionAPI.ingest(): File not found or empty after 20 attempts at: C:\hascoapi\var\resources\DFL1774522348842261\WKF-WeatherStation (1).xlsx
```

**Root Cause:** The file upload step is missing from the workflow. The ingestion endpoint is being called without first uploading the physical file to the server.

---

## Correct Workflow

### Step 1: Create WKF Metadata
**Endpoint:** `POST /hascoapi/api/wkf/create/{json}`

**Purpose:** Creates the WKF and DataFile metadata records in the triplestore.

**Result:**
- WKF URI created (e.g., `https://hadatac.org/ont/hadatac#/WKF1774522348842261`)
- DataFile URI created (e.g., `https://hadatac.org/ont/hadatac#/DFL1774522348842261`)
- Status: `UNPROCESSED`

---

### Step 2: Upload the Physical File
**Endpoint:** `POST /hascoapi/api/uploadFile/{elementUri}/{filename}`

**Parameters:**
- `elementUri`: The WKF URI (e.g., `https://hadatac.org/ont/hadatac#/WKF1774522348842261`)
- `filename`: The filename (e.g., `WKF-WeatherStation (1).xlsx`)

**Request:**
- Content-Type: `multipart/form-data`
- Body: The file as `file` field in multipart form data

**Purpose:** Saves the physical file to the server at:
```
C:\hascoapi\var\resources\DFL1774522348842261\WKF-WeatherStation (1).xlsx
```

**Result:**
- Physical file saved on server
- Returns: `{"message": "File uploaded and saved successfully.", "success": true}`

---

### Step 3: Trigger Ingestion
**Endpoint:** `POST /hascoapi/api/ingest/{status}/{elementType}/{elementUri}`

**Parameters:**
- `status`: Status URI (e.g., `http://hadatac.org/ont/vstoi#Draft`)
- `elementType`: `wkf`
- `elementUri`: The WKF URI (e.g., `https://hadatac.org/ont/hadatac#/WKF1774522348842261`)

**Request:**
- Content-Type: `application/json` (optional, can be empty body)

**Purpose:** Reads the uploaded file and ingests the WKF content into the triplestore.

**Result:**
- File status changed to `WORKING`, then `PROCESSED` or `UNPROCESSED` (if error)
- WKF content ingested into named graph
- Returns: `{"message": "File submitted for ingestion. Check file's log for ingestion status", "success": true}`

---

## Current Problem

**What's happening:**
1. ✅ Step 1 (Create metadata) - **WORKING**
2. ❌ Step 2 (Upload file) - **MISSING**
3. ❌ Step 3 (Ingest) - **FAILING** (because file doesn't exist)

**Fix:** The frontend MUST call Step 2 (upload) before calling Step 3 (ingest).

---

## Example Frontend Code (JavaScript/React)

```javascript
// Step 1: Create WKF metadata
const createWKF = async (wkfData) => {
  const response = await fetch('/hascoapi/api/wkf/create/' + encodeURIComponent(JSON.stringify(wkfData)), {
    method: 'POST'
  });
  const result = await response.json();
  return result.uri; // WKF URI
};

// Step 2: Upload the file
const uploadFile = async (wkfUri, file) => {
  const formData = new FormData();
  formData.append('file', file);
  
  const response = await fetch(`/hascoapi/api/uploadFile/${encodeURIComponent(wkfUri)}/${encodeURIComponent(file.name)}`, {
    method: 'POST',
    body: formData
  });
  
  const result = await response.json();
  if (!result.success) {
    throw new Error('File upload failed: ' + result.message);
  }
};

// Step 3: Trigger ingestion
const ingestWKF = async (wkfUri, status = 'http://hadatac.org/ont/vstoi#Draft') => {
  const response = await fetch(`/hascoapi/api/ingest/${encodeURIComponent(status)}/wkf/${encodeURIComponent(wkfUri)}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({})
  });
  
  const result = await response.json();
  return result;
};

// Complete workflow
const completeWKFIngestion = async (wkfData, file) => {
  try {
    // Step 1
    const wkfUri = await createWKF(wkfData);
    console.log('WKF created:', wkfUri);
    
    // Step 2 (CRITICAL - currently missing)
    await uploadFile(wkfUri, file);
    console.log('File uploaded successfully');
    
    // Step 3
    const result = await ingestWKF(wkfUri);
    console.log('Ingestion result:', result);
    
    return { success: true, wkfUri };
  } catch (error) {
    console.error('WKF ingestion failed:', error);
    return { success: false, error: error.message };
  }
};
```

---

## Path Logic (Already Fixed)

The backend now correctly handles both absolute and relative file paths:

### Relative Path (Standard)
- Filename: `WKF-WeatherStation.xlsx`
- Stored at: `C:\hascoapi\var\resources\DFL1774522348842261\WKF-WeatherStation.xlsx`

### Absolute Path (Windows)
- Filename: `C:/xampp/htdocs/drupal/sites/hascorepo/resources/DFL1774518626166601/WKF-WeatherStation.xlsx`
- Stored at: (same as filename, already absolute)

The fix checks if the path is absolute before prepending the base path.

---

## Testing the Fix

### Test with cURL

```bash
# Step 1: Create WKF (replace with your actual JSON)
curl -X POST "http://localhost:9000/hascoapi/api/wkf/create/%7B%22label%22%3A%22Test%22%7D"

# Step 2: Upload file
curl -X POST "http://localhost:9000/hascoapi/api/uploadFile/https%3A%2F%2Fhadatac.org%2Font%2Fhadatac%23%2FWKF1774522348842261/WKF-Test.xlsx" \
  -F "file=@/path/to/WKF-Test.xlsx"

# Step 3: Ingest
curl -X POST "http://localhost:9000/hascoapi/api/ingest/http%3A%2F%2Fhadatac.org%2Font%2Fvstoi%23Draft/wkf/https%3A%2F%2Fhadatac.org%2Font%2Fhadatac%23%2FWKF1774522348842261" \
  -H "Content-Type: application/json"
```

---

## Backend Improvements Made

1. ✅ **Path handling fixed** - Absolute vs relative paths now work correctly
2. ✅ **Synchronous upload** - File is saved immediately (not async)
3. ✅ **Better logging** - Clear [ABSOLUTE] vs [RELATIVE] indicators
4. ✅ **Error messages** - Descriptive errors for missing files

---

## Recommended Frontend Fix

Add the missing upload step between WKF creation and ingestion:

```diff
  // Create WKF
  const wkfUri = await createWKF(data);
  
+ // Upload the file BEFORE calling ingest
+ await uploadFile(wkfUri, selectedFile);
  
  // Trigger ingestion
  await ingestWKF(wkfUri);
```

---

## Summary

| Step | Status | Action Required |
|------|--------|-----------------|
| Backend path fix | ✅ Complete | None |
| Upload endpoint | ✅ Working | None |
| Ingestion endpoint | ✅ Working | None |
| Frontend workflow | ❌ Broken | **Add upload step** |

**Action:** Update the frontend to call `/hascoapi/api/uploadFile/{wkfUri}/{filename}` before calling the ingestion endpoint.

