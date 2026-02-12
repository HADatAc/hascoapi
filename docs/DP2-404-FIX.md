# DP2 Generation 404 Error - Fix Documentation

## Problem
After successfully registering DP2 metadata, the generation request fails with:
```
API request returned the following status code: 404
DP2 metadata was registered, but the generator service did not return a confirmation.
```

## Root Cause
The client code is calling a generation endpoint that either:
1. Doesn't exist
2. Uses the wrong HTTP method (POST vs GET)
3. Has incorrect URL structure

## Solution Applied

### 1. Added POST Support to Generation Routes
**File**: `conf/routes`

**Before**: Only GET methods were supported
```
GET /hascoapi/api/mt/gen/perstatus/...
```

**After**: Both GET and POST are now supported
```
GET  /hascoapi/api/mt/gen/perstatus/:elementtype/:datafileuri/:status/:filename/:mediafolder/:verifyuri
POST /hascoapi/api/mt/gen/perstatus/:elementtype/:datafileuri/:status/:filename/:mediafolder/:verifyuri
```

### 2. Enhanced Debug Logging
**File**: `app/org/hascoapi/console/controllers/restapi/IngestionAPI.java`

**Added**:
- Log HTTP method and full request URI
- Log all input parameters
- Log which generator is being called
- Log generation result
- Return meaningful response with generated filename

**Debug Output**:
```
========== IngestionAPI.mtGenByStatus() START ==========
HTTP Method: POST
Request URI: /hascoapi/api/mt/gen/perstatus/dp2/...
Parameters:
  elementtype: dp2
  datafileuri: https://hadatac.org/ont/hadatac#/DFL...
  status: DRAFT
  filename: ASasa.xlsx
✓ All required parameters present
→ Calling generator for elementtype: dp2
  Calling DP2Gen.genByStatus()...
  
[DP2Gen debug output...]

  Generation result: C:\var\hascoapi\ASasa.xlsx
✓ Generation completed successfully
========== IngestionAPI.mtGenByStatus() END (SUCCESS) ==========
```

### 3. Improved API Response
**Before**: Returned empty string `""`
**After**: Returns meaningful message with filename
```json
{
  "message": "File generated: C:\\var\\hascoapi\\ASasa.xlsx",
  "isSuccessful": true
}
```

## Generation Endpoints

### All Available Routes:

#### 1. Generate by Status (most common for DP2)
```
GET/POST /hascoapi/api/mt/gen/perstatus/:elementtype/:datafileuri/:status/:filename/:mediafolder/:verifyuri

Example:
POST /hascoapi/api/mt/gen/perstatus/dp2/https%3A%2F%2Fhadatac.org%2Font%2Fhadatac%23%2FDFL1770628327888941/DRAFT/ASasa.xlsx/null/null
```

#### 2. Generate by Element
```
GET/POST /hascoapi/api/mt/gen/perelement/:elementtype/:datafileuri/:elementuri/:filename/:mediafolder/:verifyuri
```

#### 3. Generate by User/Manager
```
GET/POST /hascoapi/api/mt/gen/peruser/:elementtype/:datafileuri/:useremail/:status/:filename/:mediafolder/:verifyuri
```

#### 4. Download Generated File
```
POST /hascoapi/api/mt/get/generated/:filename

Example:
POST /hascoapi/api/mt/get/generated/ASasa.xlsx
```

## Testing

### 1. Test Generation Endpoint Directly

**Using curl** (Windows PowerShell):
```powershell
# URL encode the datafileuri
$datafileuri = "https://hadatac.org/ont/hadatac#/DFL1770628327888941"
$encoded = [System.Web.HttpUtility]::UrlEncode($datafileuri)

# Call generation endpoint
curl -X POST "http://localhost:9000/hascoapi/api/mt/gen/perstatus/dp2/$encoded/DRAFT/test.xlsx/null/null"
```

**Using curl** (Linux/Mac):
```bash
# URL encode the datafileuri
datafileuri="https://hadatac.org/ont/hadatac#/DFL1770628327888941"
encoded=$(echo -n "$datafileuri" | jq -sRr @uri)

# Call generation endpoint
curl -X POST "http://localhost:9000/hascoapi/api/mt/gen/perstatus/dp2/$encoded/DRAFT/test.xlsx/null/null"
```

### 2. Check Debug Output
After making the request, check the console for:

```
========== IngestionAPI.mtGenByStatus() START ==========
HTTP Method: POST
Request URI: /hascoapi/api/mt/gen/perstatus/dp2/...
✓ All required parameters present
→ Calling DP2Gen.genByStatus()...
✓ Generation completed successfully
```

### 3. Verify File Created
```powershell
# Windows
dir C:\var\hascoapi\*.xlsx

# Linux
ls -la /var/hascoapi/*.xlsx
```

### 4. Test Download
```bash
curl -X POST http://localhost:9000/hascoapi/api/mt/get/generated/test.xlsx -o downloaded.xlsx
```

## Troubleshooting

### Still Getting 404?

**Check the exact URL being called**:
Look in the debug output for:
```
Request URI: [the full URI]
```

Compare with the routes in `conf/routes`.

**Common Issues**:

1. **URL encoding**: Ensure URIs in parameters are URL-encoded
   - `:` → `%3A`
   - `/` → `%2F`
   - `#` → `%23`

2. **Wrong endpoint**: Client might be calling `/api/mt/generate/...` instead of `/api/mt/gen/...`

3. **Missing parameters**: All 6 parameters are required (use `null` for optional ones)

4. **Wrong base URL**: Ensure using `/hascoapi/api/...` not just `/api/...`

### Debug Client Code

If you have access to the client code that's making the request, add logging:

```javascript
console.log('Generation API URL:', generationUrl);
console.log('HTTP Method:', method);
console.log('Parameters:', parameters);
```

Look for patterns like:
- `/generate/` instead of `/gen/`
- Missing `/hascoapi/` prefix
- Wrong HTTP method
- Wrong parameter order

## Expected Flow

1. **User submits DP2 metadata form**
   - DP2 metadata is created in triple store ✓
   - Success message shown

2. **System calls generation endpoint**
   - Should call: `/hascoapi/api/mt/gen/perstatus/dp2/...`
   - With method: GET or POST (both now supported)
   - With all parameters

3. **DP2Gen.genByStatus() executes**
   - Queries triple store for DP2 elements
   - Creates Excel workbook
   - Saves file to disk
   - Returns filename

4. **User can download file**
   - Calls: `/hascoapi/api/mt/get/generated/filename.xlsx`
   - File is served for download

## Next Steps

1. **Restart Application**: The route changes require restart
2. **Test Generation**: Try generating a DP2 through the UI
3. **Check Logs**: Look for the new debug output
4. **Verify Endpoint**: Ensure the correct URL is being called
5. **Report Back**: Share the "Request URI" from the debug output if still failing

---

**Files Modified**:
- `conf/routes` - Added POST support
- `app/org/hascoapi/console/controllers/restapi/IngestionAPI.java` - Enhanced logging

**Status**: ✅ Ready for testing  
**Requires**: Application restart to load new routes
