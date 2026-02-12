# DP2 Generation Backend Test

## Quick Test to Prove Backend Works

This test proves the DP2 generation backend is working correctly, independent of frontend.

### Prerequisites

1. Backend running on `http://localhost:9000`
2. JWT token for authentication
3. An existing DP2 with ingested data

### Step 1: Get JWT Token

```powershell
# Login and get token (adjust credentials as needed)
$loginUrl = "http://localhost:9000/hascoapi/api/login"
$credentials = @{
    email = "admin@example.com"
    password = "yourpassword"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri $loginUrl -Method Post -Body $credentials -ContentType "application/json"
$token = $response.token
Write-Host "Token: $token"
```

### Step 2: Find an Existing DP2

```powershell
# List DP2s
$listUrl = "http://localhost:9000/hascoapi/api/dp2/list"
$headers = @{ "Authorization" = "Bearer $token" }

$dp2List = Invoke-RestMethod -Uri $listUrl -Method Get -Headers $headers
$dp2List | ConvertTo-Json -Depth 5
```

Pick a DP2 that has `hasDataFileUri` set. Example:
```json
{
  "uri": "https://hadatac.org/ont/hadatac#/DP21770907845634971",
  "hasDataFileUri": "https://hadatac.org/ont/hadatac#/DFL1770907845634971",
  "hasStatus": "DRAFT"
}
```

### Step 3: Call Generation Endpoint Directly

```powershell
# Set your parameters
$dataFileUri = "https://hadatac.org/ont/hadatac#/DFL1770907845634971"
$encodedUri = [System.Web.HttpUtility]::UrlEncode($dataFileUri)
$filename = "test-dp2-generation.xlsx"
$status = "DRAFT"

# Call generation endpoint
$genUrl = "http://localhost:9000/hascoapi/api/mt/gen/perstatus/dp2/$encodedUri/$status/$filename/null/null"
Write-Host "Calling: $genUrl"

$headers = @{ "Authorization" = "Bearer $token" }

$genResponse = Invoke-RestMethod -Uri $genUrl -Method Post -Headers $headers
Write-Host "Generation response:"
$genResponse | ConvertTo-Json
```

### Expected Backend Logs

You should see in the backend console:

```
========== IngestionAPI.mtGenByStatus() START ==========
Parameters:
  elementtype: dp2
  datafileuri: https://hadatac.org/ont/hadatac#/DFL1770907845634971
  status: DRAFT
  filename: test-dp2-generation.xlsx

========== DP2Gen.genByStatus() START ==========
Input parameters:
  dataFileUri: https://hadatac.org/ont/hadatac#/DFL1770907845634971
  status: DRAFT
  filename: test-dp2-generation.xlsx
  basePath (from ConfigProp): C:\hascoapi\var\
  outFilename (final): C:\hascoapi\var\test-dp2-generation.xlsx

DP2Gen.genByStatus: dp2 MT candidates found=1
  [DP2Gen] candidate uri=... status(eff)=DRAFT dataFile=...
DP2Gen.genByStatus: resolved dp2=... hasDataFile=... hasStatus=DRAFT

[DP2Gen] graph(https) tripleCount=7
[DP2Gen] graph(https) counts deployments=0 platforms=0 ...

========== DP2Gen.save() START ==========
  filename parameter: C:\hascoapi\var\test-dp2-generation.xlsx
  ✓ helper and workbook are valid
  → Saving namespaces...
  ✓ Namespaces saved
  Output file absolute path: C:\hascoapi\var\test-dp2-generation.xlsx
  → Writing workbook to file...
  ✓ File written successfully
  File size: 8192 bytes
  File exists: true
========== DP2Gen.save() END (SUCCESS) ==========

✓ Generation completed successfully
  Final filename for response: [test-dp2-generation.xlsx]
========== IngestionAPI.mtGenByStatus() END (SUCCESS) ==========
```

### Step 4: Verify File Was Created

```powershell
# Check if file exists
$filePath = "C:\hascoapi\var\test-dp2-generation.xlsx"
Test-Path $filePath
# Should return: True

# Check file size
(Get-Item $filePath).Length
# Should return: some number > 0
```

### Step 5: Download the File via API

```powershell
# Download the generated file
$downloadUrl = "http://localhost:9000/hascoapi/api/mt/get/generated/$filename"
$outputPath = "C:\temp\downloaded-test.xlsx"

Invoke-RestMethod -Uri $downloadUrl -Method Post -Headers $headers -OutFile $outputPath

Write-Host "File downloaded to: $outputPath"
Write-Host "File size: $((Get-Item $outputPath).Length) bytes"

# Open the file
Start-Process $outputPath
```

## Alternative: cURL Test (Windows Git Bash)

```bash
# Get token
TOKEN=$(curl -X POST "http://localhost:9000/hascoapi/api/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"yourpassword"}' \
  | jq -r '.token')

echo "Token: $TOKEN"

# Set parameters
DATAFILE_URI="https://hadatac.org/ont/hadatac#/DFL1770907845634971"
ENCODED_URI=$(echo "$DATAFILE_URI" | jq -sRr @uri)
FILENAME="test-dp2-generation.xlsx"

# Generate file
curl -X POST "http://localhost:9000/hascoapi/api/mt/gen/perstatus/dp2/$ENCODED_URI/DRAFT/$FILENAME/null/null" \
  -H "Authorization: Bearer $TOKEN" \
  -v

# Check backend logs for generation output

# Download file
curl -X POST "http://localhost:9000/hascoapi/api/mt/get/generated/$FILENAME" \
  -H "Authorization: Bearer $TOKEN" \
  -o test-downloaded.xlsx

# Verify file
ls -lh test-downloaded.xlsx
```

## Expected Results

### ✅ Success Indicators

1. **Generation Response**:
   ```json
   {
     "message": "test-dp2-generation.xlsx",
     "isSuccessful": true
   }
   ```

2. **Backend Logs**: Show complete generation flow (no errors)

3. **File Created**: File exists at `C:\hascoapi\var\test-dp2-generation.xlsx`

4. **File Size**: > 0 bytes (typically 8-15 KB for empty DP2)

5. **Download Works**: File can be downloaded and opened in Excel

### ❌ Failure Indicators

1. **404 Response**: Generation endpoint not found
   - **Fix**: Check routes configuration, restart backend

2. **No Logs**: Backend doesn't show `DP2Gen.genByStatus()` logs
   - **Issue**: Endpoint not being called or JWT auth failing

3. **File Not Created**: File doesn't exist at expected path
   - **Fix**: Check ConfigProp.getPathIngestion() value
   - **Fix**: Check directory permissions

4. **Empty Response**: API returns empty string
   - **Fix**: Check DP2 has valid hasDataFileUri
   - **Fix**: Check DP2 has ingested data

## Troubleshooting

### Issue: 401 Unauthorized

```
Response: 401 Unauthorized
```

**Fix**: Regenerate JWT token (may have expired)

### Issue: 404 Not Found

```
Response: 404 Not Found
```

**Possible Causes**:
1. Backend not running
2. Routes not loaded (restart backend)
3. Wrong URL (check spelling)

### Issue: Generation Returns Empty String

```json
{
  "message": "",
  "isSuccessful": true
}
```

**Possible Causes**:
1. DP2 has no hasDataFileUri
2. No DP2 found with given status
3. DataFile URI mismatch (http vs https)

**Debug**: Check backend logs for:
```
DP2Gen.genByStatus: no DP2 MT found for status=DRAFT
```

### Issue: File Not Found After Generation

```
[ERROR] IngestionAPI.mtGetGenerated(): File not found
```

**Possible Causes**:
1. Wrong path in ConfigProp.getPathIngestion()
2. Directory doesn't exist
3. Permission issue

**Fix**: Check `conf/template.conf`:
```
hascoapi.paths.ingestion="C:/hascoapi/var/"
```

## Conclusion

If this test **succeeds**, it proves:
- ✅ Backend generation logic works correctly
- ✅ Routes are configured properly
- ✅ File storage is working
- ✅ Download API works

**Therefore, the issue is 100% in the frontend not calling the generation endpoint.**

Provide the `DP2-GENERATION-MISSING-FRONTEND-CALL.md` document to the frontend team.

---

**Date**: 2026-02-12  
**Purpose**: Prove backend DP2 generation is working  
**Result Expected**: Backend works → Frontend needs fix
