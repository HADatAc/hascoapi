# Quick Test Guide - DP2 Generation

## What Was Fixed
The DP2 generation now properly handles file paths on both Windows and Linux, automatically creates directories, and provides detailed debug output.

## Test Now

### 1. Run DP2 Generation
Use your existing test case - generate a DP2 file through the UI or API.

### 2. Watch Console Output
Look for these success indicators:

```
========== DP2Gen.genByStatus() START ==========
✓ basePath (normalized): [your path]
✓ outFilename (absolute): [full path to file]

========== DP2Gen.save() START ==========
✓ File written successfully
✓ File exists: true
========== DP2Gen.save() END (SUCCESS) ==========
```

### 3. Verify File Exists
**Windows (PowerShell)**:
```powershell
# Check where ConfigProp.getPathIngestion() points
# Default might be: C:\var\hascoapi\

dir C:\var\hascoapi\*.xlsx
# or
dir C:\hascoapi\ingestion\*.xlsx
```

**Linux**:
```bash
ls -la /var/hascoapi/*.xlsx
```

### 4. Try to Download
Through UI or API:
```bash
curl -X POST http://localhost:9000/api/mt/get/generated/yourfile.xlsx -o downloaded.xlsx
```

## Expected Results

### ✅ Success:
```
- Console shows "✓ File written successfully"
- File exists on disk
- File size > 0 bytes
- Download works
```

### ❌ Still Failing?

Check debug output for:

1. **Wrong Path**:
   ```
   basePath (from ConfigProp): /var/hascoapi/
   ```
   → Update `conf/application.conf`:
   ```conf
   hasco.ingestion_path = "C:/your/path/"  # Windows
   ```

2. **Permission Denied**:
   ```
   IOException: Access is denied
   ```
   → Grant write permissions to the directory

3. **Directory Not Created**:
   ```
   Parent directory created: false
   ```
   → Check parent directory permissions

## Quick Fix

If you need to change the ingestion path:

1. Edit `conf/application.conf`:
   ```conf
   hasco.ingestion_path = "C:/hascoapi/generated/"  # Windows
   # or
   hasco.ingestion_path = "/var/hascoapi/"  # Linux
   ```

2. Restart the application

3. Test again

## Report Results

When reporting, include:
1. Full console output from generation
2. File path from debug output
3. Whether file exists on disk
4. Download error (if any)

---

**All code changes compile successfully** ✅  
**Ready for testing** ✅
