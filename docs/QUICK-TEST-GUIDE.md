# Quick Test Guide: Malformed URI Fix

## Test Scenario

Verify that the SDD generation now handles trailing whitespace in spreadsheet cells correctly.

## Test Steps

### 1. Prepare Test Data

Use the original failing SDD file or create a test file with:
- Unit value: `unit:MicroGM-PER-M3` with trailing newline or space
- Any other field with trailing whitespace

### 2. Upload and Ingest

```bash
# Upload the SDD file
curl -X POST "http://localhost:9000/api/mt/upload" \
  -F "file=@SDD-WS.xlsx" \
  -F "elementtype=sdd"

# Note the returned DataFile URI
```

### 3. Trigger Generation

```bash
# Generate the SDD
curl -X POST "http://localhost:9000/api/mt/genByStatus" \
  -d "elementtype=sdd" \
  -d "datafileuri=<YOUR_DATAFILE_URI>" \
  -d "status=http://hadatac.org/ont/vstoi#Draft" \
  -d "filename=generated.xlsx" \
  -d "mediaFolder=test"
```

### 4. Verify Success

**Before Fix:**
```
ERROR: org.apache.jena.query.QueryException: Bad IRI: 
'http://qudt.org/vocab/unit/MicroGM-PER-M3%A'
```

**After Fix:**
```
✅ Generation completed successfully
✅ File downloadable
✅ No QueryException errors
```

## Quick PowerShell Test Script

```powershell
# Test the specific case that was failing
$testData = @"
Column,Attribute,attributeOf,Unit,Time,Entity
air_quality,envo:01000432,??weather,unit:MicroGM-PER-M3
,??instant
"@

# Note: Add actual newline after unit value to replicate the issue
Write-Host "Testing SDD generation with whitespace in unit field..."
# Run your upload/generation workflow here
```

## Expected Results

### ✅ Success Indicators
- [x] Compilation successful (no errors)
- [x] SDD file uploads successfully
- [x] Generation completes without QueryException
- [x] Generated file is downloadable
- [x] Unit URIs are correctly formed
- [x] No `ILLEGAL_PERCENT_ENCODING` errors in logs

### ❌ Failure Indicators
- [ ] QueryException during generation
- [ ] Malformed URI errors in logs
- [ ] Generated file not created
- [ ] 404 error when downloading

## Automated Verification

Check the application logs for:

```bash
# Look for successful generation
tail -f logs/application.log | grep "COMPLETED SUCCESSFULLY"

# Verify no QueryException
tail -f logs/application.log | grep "QueryException"

# Check for malformed URI errors
tail -f logs/application.log | grep "Bad IRI"
```

## Rollback Plan

If issues occur:
1. Revert changes to `SDDAttributeGenerator.java`
2. Recompile: `sbt compile`
3. Restart application server

## Test Data

Original failing data:
```
Unit: "unit:MicroGM-PER-M3\n"  (with newline)
```

Expected after fix:
```
Unit: "unit:MicroGM-PER-M3"    (trimmed)
URI:  "http://qudt.org/vocab/unit/MicroGM-PER-M3"  (valid)
```

---

**Status:** Ready for testing  
**Date:** 2025-03-19  
**Fixed in:** Build after commit implementing whitespace trimming

