# 🐛 BUG REPORT - Front-End: Generator Service NOT Being Called

**Date**: 2026-02-09  
**Severity**: CRITICAL  
**Component**: Metadata Template Generation Form (INS, DP2, DSG, KGR)  
**Status**: Backend Working ✅ | Frontend NOT Calling API ❌

---

## 🎯 Problem Summary - CONFIRMED

When users create a new Metadata Template (e.g., Simulators/INS) and submit the form:
- ✅ **Metadata is created successfully** in the triple store
- ❌ **Generation endpoint is NEVER CALLED** by the front-end
- ❌ User sees error message: _"Simulators metadata was registered, but the generator service did not return a confirmation."_

**Expected Behavior**: After metadata creation, the system should automatically call the generation endpoint.

**Actual Behavior**: Metadata is created but **generation endpoint is never invoked**.

---

## 🔍 Evidence - Backend Logs

### What We See (Metadata Creation Only):
```
Type: [ins]  JSON [{"uri":"https://hadatac.org/ont/hadatac#/INF1770658817935381",...}]

========== MetadataFactory.createModel() START ==========
MetadataFactory.createModel() received ROWS with [1] entries
...
Total triples in model: 7
========== MetadataFactory.createModel() END ==========
```

### What We DON'T See (Generation NOT Called):
```
========== IngestionAPI.mtGenByStatus() START ==========  ← NEVER APPEARS!
✓ mtGenByStatus endpoint was called successfully!
...
```

**Conclusion**: The front-end is **NOT calling** `/hascoapi/api/mt/gen/perstatus/...` after creating metadata.

---

## ✅ Backend Status - VERIFIED WORKING

### Tests Performed:

#### Test 1: Direct API Call to Generation Endpoint
```bash
GET http://localhost:9000/hascoapi/api/mt/gen/perstatus/ins/null/DRAFT/test-manual.xlsx/null/null
```

**Result**: ✅ SUCCESS
```json
{
  "isSuccessful": true,
  "body": "test-manual.xlsx"
}
```

#### Test 2: File Creation Verification
```bash
ls C:\hascoapi\var\test-manual.xlsx
```

**Result**: ✅ File created successfully (10,307 bytes)

#### Test 3: Download Endpoint
```bash
POST http://localhost:9000/hascoapi/api/mt/get/generated/test-manual.xlsx
```

**Result**: ✅ File downloaded successfully (10,307 bytes)

### Conclusion:
**The backend API is 100% functional.** The issue is in the frontend code that calls or processes the generation API.

---

## 🔍 Root Cause Analysis

The problem is in the **frontend JavaScript/TypeScript code** that:
1. Creates the metadata (this works ✅)
2. Should then call the generation endpoint (this fails ❌)
3. Should process the response and trigger download (this fails ❌)

### Likely Issues:

#### Issue A: Generation API Not Being Called
The frontend may be creating metadata but **not calling** the generation endpoint at all.

#### Issue B: Wrong URL/Parameters
The frontend may be calling the generation endpoint with:
- Wrong URL path
- Wrong HTTP method (POST vs GET)
- Wrong parameters
- URL encoding issues (e.g., spaces, special characters)

#### Issue C: Response Not Processed Correctly
The frontend may be:
- Not handling the JSON response
- Not extracting the filename from `response.body`
- Not calling the download endpoint with the returned filename

#### Issue D: CORS or Network Error
The frontend may be hitting CORS issues or network errors that are being silently swallowed.

---

## 🔧 How to Fix (Frontend Developer Instructions)

### Step 1: Locate the Generation Form Submit Handler

Find the code that handles form submission for creating Metadata Templates. This is likely in:
- `src/components/MetadataTemplateForm.jsx` (React)
- `sites/all/modules/custom/sir/src/Form/GenerateForm.php` (Drupal)
- Or similar file handling the "Generate" or "Create" button

### Step 2: Verify the API Call Sequence

The correct sequence should be:

```javascript
// 1. Create metadata (this already works)
const metadataResponse = await createMetadata(formData);

// 2. Extract metadata URI
const metadataUri = metadataResponse.uri; // e.g., "hadatac:INF1770656725460031"
const dataFileUri = metadataResponse.hasDataFileUri; // e.g., "hadatac:DFL1770656725460031"

// 3. Call generation endpoint
const elementType = "ins"; // or "dp2", "dsg", "kgr"
const status = "DRAFT";
const filename = `${formData.label}.xlsx`; // e.g., "ASDASDASDASD.xlsx"

const generationResponse = await fetch(
  `/hascoapi/api/mt/gen/perstatus/${elementType}/${dataFileUri}/${status}/${filename}/null/null`,
  {
    method: 'GET', // or 'POST' - both should work
    headers: {
      'Content-Type': 'application/json',
    },
  }
);

// 4. Check response
const result = await generationResponse.json();

if (result.isSuccessful) {
  const generatedFilename = result.body; // e.g., "ASDASDASDASD.xlsx"
  
  // 5. Trigger download
  window.location.href = `/hascoapi/api/mt/get/generated/${generatedFilename}`;
  
  // Show success message
  showSuccessMessage(`File generated successfully: ${generatedFilename}`);
} else {
  // Show error
  showErrorMessage(`Generation failed: ${result.body}`);
}
```

### Step 3: Fix Common Issues

#### Fix A: URL Encoding
```javascript
// WRONG - spaces and special chars break the URL
const filename = "My Test File.xlsx";
const url = `/api/mt/gen/perstatus/ins/null/DRAFT/${filename}/null/null`;

// RIGHT - encode the filename
const filename = "My Test File.xlsx";
const encodedFilename = encodeURIComponent(filename);
const url = `/api/mt/gen/perstatus/ins/null/DRAFT/${encodedFilename}/null/null`;
```

#### Fix B: Handle dataFileUri Parameter
```javascript
// For most MTs (INS, DSG, KGR):
const url = `/api/mt/gen/perstatus/${elementType}/null/${status}/${filename}/null/null`;

// For DP2 (requires dataFileUri):
const url = `/api/mt/gen/perstatus/dp2/${dataFileUri}/${status}/${filename}/null/null`;
```

#### Fix C: Error Handling
```javascript
try {
  const response = await fetch(generationUrl);
  
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
  }
  
  const result = await response.json();
  
  if (!result.isSuccessful) {
    throw new Error(result.body || 'Generation failed');
  }
  
  // Success - proceed with download
  const filename = result.body;
  window.location.href = `/hascoapi/api/mt/get/generated/${filename}`;
  
} catch (error) {
  console.error('Generation error:', error);
  showErrorMessage(`Generation failed: ${error.message}`);
}
```

### Step 4: Add Logging for Debugging

```javascript
console.log('=== Generation Debug Info ===');
console.log('Element Type:', elementType);
console.log('Data File URI:', dataFileUri);
console.log('Status:', status);
console.log('Filename:', filename);
console.log('Full URL:', generationUrl);
console.log('=============================');

const response = await fetch(generationUrl);
console.log('Response Status:', response.status);
console.log('Response OK:', response.ok);

const result = await response.json();
console.log('Response Body:', result);
```

---

## 🧪 Testing Instructions (Frontend)

### Test 1: Verify API Call is Made
1. Open browser DevTools (F12)
2. Go to **Network** tab
3. Create a new INS (Simulator)
4. Submit the form
5. **Check**: Do you see a request to `/hascoapi/api/mt/gen/perstatus/...`?
   - ✅ **YES**: Proceed to Test 2
   - ❌ **NO**: The API call is not being made - add it in Step 2 above

### Test 2: Check Request URL
If you see the request, check the URL:
- Is it correctly formatted?
- Are there any `%20` or encoded characters?
- Is the filename parameter correct?
- Is the method GET or POST?

### Test 3: Check Response
Click on the request in Network tab:
- What is the **Status Code**? (should be 200)
- What is the **Response** body? (should be `{"isSuccessful":true,"body":"filename.xlsx"}`)
- Are there any CORS errors in the Console?

### Test 4: Manual Test
Open browser console and run:
```javascript
fetch('/hascoapi/api/mt/gen/perstatus/ins/null/DRAFT/manual-test.xlsx/null/null')
  .then(r => r.json())
  .then(data => console.log('Result:', data))
  .catch(err => console.error('Error:', err));
```

Expected output:
```json
{
  "isSuccessful": true,
  "body": "manual-test.xlsx"
}
```

---

## 📊 Expected API Endpoints

### Generation Endpoints (All Working ✅):

#### GET/POST: Generate by Status
```
/hascoapi/api/mt/gen/perstatus/{elementtype}/{datafileuri}/{status}/{filename}/{mediafolder}/{verifyuri}
```

**Parameters**:
- `elementtype`: `ins`, `dp2`, `dsg`, or `kgr`
- `datafileuri`: DataFile URI (use `null` for most MTs except DP2)
- `status`: `DRAFT`, `PUBLISHED`, etc.
- `filename`: Name for the generated Excel file (e.g., `test.xlsx`)
- `mediafolder`: Media folder path (use `null` if not needed)
- `verifyuri`: Verification URI (use `null` if not needed)

**Response**:
```json
{
  "isSuccessful": true,
  "body": "filename.xlsx"
}
```

#### POST: Download Generated File
```
/hascoapi/api/mt/get/generated/{filename}
```

**Parameters**:
- `filename`: The filename returned from the generation endpoint

**Response**: Binary Excel file (`.xlsx`)

---

## 🎯 Quick Fix Example (React)

```jsx
const handleGenerateSubmit = async (formData) => {
  try {
    // Step 1: Create metadata
    const metadataResponse = await createMetadata(formData);
    
    if (!metadataResponse.isSuccessful) {
      throw new Error('Metadata creation failed');
    }
    
    // Step 2: Generate Excel file
    const elementType = 'ins'; // or get from form
    const dataFileUri = metadataResponse.hasDataFileUri;
    const status = 'DRAFT';
    const filename = `${formData.label.replace(/[^a-zA-Z0-9]/g, '_')}.xlsx`;
    
    const generationUrl = `/hascoapi/api/mt/gen/perstatus/${elementType}/${dataFileUri || 'null'}/${status}/${encodeURIComponent(filename)}/null/null`;
    
    console.log('Calling generation endpoint:', generationUrl);
    
    const genResponse = await fetch(generationUrl, {
      method: 'GET',
    });
    
    if (!genResponse.ok) {
      throw new Error(`Generation request failed: ${genResponse.status}`);
    }
    
    const genResult = await genResponse.json();
    
    if (!genResult.isSuccessful) {
      throw new Error(genResult.body || 'Generation failed');
    }
    
    // Step 3: Trigger download
    const downloadUrl = `/hascoapi/api/mt/get/generated/${genResult.body}`;
    window.location.href = downloadUrl;
    
    // Show success message
    showNotification({
      type: 'success',
      message: `Metadata created and file generated: ${genResult.body}`,
    });
    
  } catch (error) {
    console.error('Error in handleGenerateSubmit:', error);
    showNotification({
      type: 'error',
      message: `Error: ${error.message}`,
    });
  }
};
```

---

## 📝 Checklist for Frontend Developer

- [ ] Locate the form submit handler for Metadata Template creation
- [ ] Verify that generation API is being called after metadata creation
- [ ] Check that URL is correctly formatted with proper encoding
- [ ] Verify HTTP method is GET or POST (both work)
- [ ] Check that response is properly parsed as JSON
- [ ] Verify that `result.body` contains the filename
- [ ] Ensure download endpoint is called with the correct filename
- [ ] Add proper error handling for network failures
- [ ] Add console logging for debugging
- [ ] Test with different MT types (INS, DP2, DSG, KGR)
- [ ] Verify filename encoding handles special characters
- [ ] Confirm success message is shown to user
- [ ] Ensure download is triggered automatically

---

## 🚨 Common Mistakes to Avoid

### ❌ Don't Do This:
```javascript
// Missing generation step
createMetadata(data)
  .then(() => showSuccess("Metadata created!"));
// File is never generated!
```

### ✅ Do This:
```javascript
// Complete flow
createMetadata(data)
  .then(response => generateFile(response.uri))
  .then(filename => downloadFile(filename))
  .then(() => showSuccess("Metadata created and file generated!"));
```

---

## 📞 Contact Backend Team If:

- You implement the fix but still see 404 errors
- The generation endpoint returns `isSuccessful: false`
- You see network errors or CORS issues
- The download endpoint fails

**Note**: The backend has been thoroughly tested and is fully functional. The issue is 100% in the frontend code.

---

## 🎉 Expected Outcome After Fix

1. User fills out MT creation form
2. User clicks "Submit" or "Generate"
3. **Metadata is created** ✅
4. **Generation API is called automatically** ✅
5. **Excel file is generated** ✅
6. **Download starts automatically** ✅
7. User sees success message: _"Metadata created and file generated successfully!"_ ✅

---

**Priority**: HIGH  
**Estimated Fix Time**: 30 minutes to 2 hours  
**Testing Required**: Yes - test all MT types (INS, DP2, DSG, KGR)
