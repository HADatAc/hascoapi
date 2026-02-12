# DP2 Generation Debugging Guide

## Problem Overview
The DP2 generation is failing with the error:
```
[ERROR] IngestionAPI.mtGetGenerated(): File not found - \var\hascoapi\sadsdasdas.xlsx
```

This indicates that the Excel file is either:
1. Not being created at all
2. Being created in the wrong location
3. Being created with the wrong path format (Windows vs Linux)

## DP2 Generation Flow

```
1. User requests DP2 generation (via API or UI)
   ↓
2. IngestionAPI.mtGenByStatus()
   - Receives: elementtype="dp2", status, filename, datafileuri
   - Calls: DP2Gen.genByStatus(datafileuri, status, filename, ...)
   ↓
3. DP2Gen.genByStatus()
   - Normalizes filename to baseName
   - Gets basePath from ConfigProp.getPathIngestion()
   - Builds outFilename = basePath + baseName
   - Creates workbook
   - Queries triple store for DP2 elements
   - Adds elements to workbook
   - Calls: DP2Gen.save(helper, outFilename)
   ↓
4. DP2Gen.save()
   - Creates FileOutputStream
   - Writes workbook to file
   - Returns filename
   ↓
5. User requests download
   - IngestionAPI.mtGetGenerated(filename)
   - Builds path: ConfigProp.getPathIngestion() + filename
   - Serves file for download
```

## Debug Output (Now Enabled)

### In DP2Gen.genByStatus():
```
========== DP2Gen.genByStatus() START ==========
Input parameters:
  dataFileUri: https://hadatac.org/ont/hadatac#/DFL1770627918798681
  status: DRAFT
  filename: sadsdasdas.xlsx
  mediaFolder: null
  baseName (after extraction): sadsdasdas.xlsx
  basePath (from ConfigProp): /var/hascoapi/
  basePath (normalized): C:\var\hascoapi\  (Windows) or /var/hascoapi/ (Linux)
  outFilename (final): C:\var\hascoapi\sadsdasdas.xlsx
  outFilename (absolute): C:\var\hascoapi\sadsdasdas.xlsx
```

### In DP2Gen.save():
```
========== DP2Gen.save() START ==========
  filename parameter: C:\var\hascoapi\sadsdasdas.xlsx
  ✓ helper and workbook are valid
  → Saving namespaces...
  ✓ Namespaces saved
  Output file absolute path: C:\var\hascoapi\sadsdasdas.xlsx
  Output file parent directory: C:\var\hascoapi
  Parent directory exists: false
  Creating parent directory...
  Parent directory created: true
  → Writing workbook to file...
  ✓ File written successfully
  File size: 12345 bytes
  File exists: true
  File can read: true
========== DP2Gen.save() END (SUCCESS) ==========
```

## Common Issues

### Issue 1: Path Separator Mismatch
**Symptom**: 
```
basePath: /var/hascoapi/
outFilename: /var/hascoapi/sadsdasdas.xlsx  (but on Windows this becomes \var\hascoapi\)
```

**Cause**: ConfigProp returns Linux-style path but running on Windows

**Solution**: The code now normalizes path separators:
```java
basePath = basePath.replace("/", java.io.File.separator).replace("\\", java.io.File.separator);
```

### Issue 2: Directory Doesn't Exist
**Symptom**:
```
Parent directory exists: false
IOException: No such file or directory
```

**Cause**: The configured ingestion path doesn't exist on disk

**Solution**: The code now creates the directory:
```java
outputFile.getParentFile().mkdirs();
```

### Issue 3: ConfigProp Returns Wrong Path
**Symptom**:
```
basePath (from ConfigProp): /var/hascoapi/
```
But you're on Windows and the path should be `C:\path\to\hascoapi\`

**Cause**: application.conf has Linux path hardcoded

**Solution**: Check `conf/application.conf`:
```conf
hasco.ingestion_path = "/var/hascoapi/"  # ← Change this!
```

Should be:
```conf
# Windows
hasco.ingestion_path = "C:/hascoapi/ingestion/"

# Or Linux
hasco.ingestion_path = "/var/hascoapi/"
```

### Issue 4: File Created But Not Found
**Symptom**:
```
DP2Gen.save() reports: ✓ File written successfully
But mtGetGenerated() says: File not found
```

**Cause**: Path mismatch between where file is saved and where it's retrieved

**Debug**:
1. Check what `DP2Gen.genByStatus()` returns (should be the full path)
2. Check what `mtGetGenerated()` receives as filename
3. Check how `mtGetGenerated()` builds the path

**Solution**: Ensure both use the same logic:
- `genByStatus` saves to: `ConfigProp.getPathIngestion() + baseName`
- `mtGetGenerated` reads from: `ConfigProp.getPathIngestion() + filename`

## Verification Steps

### Step 1: Check Configuration
```bash
# Check application.conf
grep "ingestion_path" conf/application.conf
```

Expected output should match your OS:
- Windows: `C:/path/to/ingestion/`
- Linux: `/var/hascoapi/`

### Step 2: Run Generation and Capture Logs
Watch for these key indicators:

```
✓ basePath (normalized): [should match your OS path separator]
✓ outFilename (absolute): [should be valid absolute path]
✓ Parent directory created: true
✓ File written successfully
✓ File exists: true
```

### Step 3: Verify File on Disk
After generation, manually check:

**Windows**:
```powershell
dir C:\var\hascoapi\*.xlsx
# or wherever ConfigProp.getPathIngestion() points
```

**Linux**:
```bash
ls -la /var/hascoapi/*.xlsx
```

### Step 4: Test Download
Try to download the generated file through the API:
```bash
curl -X POST http://localhost:9000/api/mt/get/generated/sadsdasdas.xlsx \
  -H "Content-Type: application/json" \
  -o downloaded.xlsx
```

If this fails, check the error message carefully.

## Quick Fix Checklist

- [ ] `conf/application.conf` has correct `hasco.ingestion_path` for your OS
- [ ] The ingestion directory exists and is writable
- [ ] Path separators are normalized (code does this automatically now)
- [ ] Debug output shows file was created successfully
- [ ] File exists on disk at the reported location
- [ ] `mtGetGenerated()` is looking in the same directory

## Expected Working Output

```
========== DP2Gen.genByStatus() START ==========
  basePath (from ConfigProp): C:/hascoapi/ingestion/
  basePath (normalized): C:\hascoapi\ingestion\
  outFilename (final): C:\hascoapi\ingestion\sadsdasdas.xlsx
  
[... queries and data collection ...]

→ All data added to workbook, calling save()...
  
========== DP2Gen.save() START ==========
  Output file absolute path: C:\hascoapi\ingestion\sadsdasdas.xlsx
  Parent directory exists: true
  → Writing workbook to file...
  ✓ File written successfully
  File size: 15234 bytes
  File exists: true
  File can read: true
========== DP2Gen.save() END (SUCCESS) ==========

Save result: C:\hascoapi\ingestion\sadsdasdas.xlsx
========== DP2Gen.genByStatus() END ==========
```

Then when downloading:
```
[IngestionAPI.mtGetGenerated]
  filename: sadsdasdas.xlsx
  basePath: C:\hascoapi\ingestion\
  filePath: C:\hascoapi\ingestion\sadsdasdas.xlsx
  ✓ File exists
  → Serving file for download
```

## Next Steps

1. **Run the generation** - The debug output is now active
2. **Check the logs** - Look for the debug markers above
3. **Verify the path** - Ensure ConfigProp.getPathIngestion() is correct
4. **Check the file** - Manually verify it exists on disk
5. **Test download** - Try to download through the API

---

**Last Updated**: 2026-02-09  
**Related**: DP2-DEBUGGING-GUIDE.md (for ingestion debugging)
