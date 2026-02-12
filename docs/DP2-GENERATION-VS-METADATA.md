# DP2 Excel Generation - Missing Step Identified

## Problem Summary

You're seeing the DP2 **metadata being created successfully** in the triple store:
```
Type: [dp2] JSON [{"uri":"...DP21770637034846641",...}]
Total triples in model: 7
```

But the **Excel file with sheets (Deployments, Platforms, etc.) is NOT being generated**.

## Root Cause

The DP2 generation process has **TWO separate steps**:

### Step 1: Create DP2 Metadata (✅ Working)
- POST to `/hascoapi/api/dp2/create` or similar
- Creates DP2 entity in triple store
- Creates 7 triples (label, hasDataFile, hasVersion, etc.)
- **This is what you're seeing in the logs**

### Step 2: Generate Excel File (❌ NOT happening)
- POST/GET to `/api/mt/gen/perstatus/dp2/{datafileuri}/{status}/{filename}/null/null`
- Queries triple store for Deployments, Platforms, InstrumentInstances, etc.
- Creates Excel workbook with multiple sheets
- **This is NOT being called!**

## What You're Missing

The Excel file with sheets like:
- InfoSheet (with hasDependencies)
- Deployments
- Platforms  
- PlatformInstances
- FieldsOfView
- InstrumentInstances
- ComponentInstances
- SensingPerspective

These are **NOT part of the DP2 metadata**. They are the **generated output** from querying the triple store.

## Why the Excel is Empty/Missing

When you generate a DP2 Excel file, the system:
1. Takes the DP2 metadata you created
2. Uses `hasDataFileUri` to scope the query
3. Queries the triple store for:
   - All Deployments linked to that DataFile
   - All Platforms linked to that DataFile
   - All InstrumentInstances linked to that DataFile
   - etc.
4. Populates each sheet with the found data

**If you haven't ingested any Deployments, Platforms, etc., the Excel will be empty except for headers!**

## Solution

### Option 1: Generate Empty Template (for filling in manually)

Call the generation endpoint to get an Excel template:

```bash
# Get the values from your DP2
datafileuri="https://hadatac.org/ont/hadatac#/DFL1770637034846641"
status="DRAFT"
filename="Asdasdasd.xlsx"

# URL encode the datafileuri
encoded=$(echo "$datafileuri" | jq -sRr @uri)

# Generate the Excel file
curl -X POST "http://localhost:9000/api/mt/gen/perstatus/dp2/$encoded/$status/$filename/null/null"
```

**Expected result**: Excel file with all sheets, but only headers (no data rows)

### Option 2: Ingest Data First, Then Generate

1. **Upload and ingest DP2 Excel file** with data (Deployments, Platforms, etc.)
2. **Then generate** to export it back to Excel

**Workflow**:
```
1. Create DP2 Excel manually (with Deployments, Platforms, etc.)
2. Upload via API: POST /hascoapi/api/mt/upload
3. System ingests all sheets → creates entities in triple store
4. Generate: POST /api/mt/gen/perstatus/dp2/...
5. System queries triple store → creates Excel with all data
```

## What the Logs Should Show

After calling the generation endpoint, you should see:

```
========== IngestionAPI.mtGenByStatus() START ==========
Parameters:
  elementtype: dp2
  datafileuri: https://hadatac.org/ont/hadatac#/DFL1770637034846641
  status: DRAFT
  filename: Asdasdasd.xlsx

========== DP2Gen.genByStatus() START ==========
Input parameters:
  dataFileUri: https://hadatac.org/ont/hadatac#/DFL1770637034846641
  ...
  
Querying triple store for Deployments...
Found 0 deployments  (or Found N deployments)

Querying triple store for Platforms...
Found 0 platforms

[etc.]

========== DP2Gen.save() START ==========
✓ File written successfully
File size: 15234 bytes
```

## Front-End Issue

The Drupal front-end should automatically call the generation endpoint after creating the DP2 metadata, but it seems to be:
1. **Not calling it at all**, OR
2. **Calling it but getting 404** (which we fixed with the new routes)

Check the Drupal logs/code for where it calls the generation.

## Test Manually

### 1. Create DP2 Metadata (you've done this)
✅ Already working

### 2. Call Generation Endpoint
```bash
# Using the DP2 you just created
curl -X POST "http://localhost:9000/api/mt/gen/perstatus/dp2/https%3A%2F%2Fhadatac.org%2Font%2Fhadatac%23%2FDFL1770637034846641/DRAFT/Asdasdasd.xlsx/null/null"
```

### 3. Check Console for Logs
You should see:
```
========== IngestionAPI.mtGenByStatus() START ==========
```

If you DON'T see this, the endpoint still isn't being reached.

### 4. Download the File
```bash
curl -X POST "http://localhost:9000/api/mt/get/generated/Asdasdasd.xlsx" -o Asdasdasd.xlsx
```

Open the Excel file - you should see:
- InfoSheet with hasDependencies, Deployments, Platforms, etc.
- All the other sheets (empty except for headers if no data was ingested)

## Summary

**What You Have**:
- ✅ DP2 metadata created in triple store
- ✅ DataFile created in triple store

**What You're Missing**:
- ❌ Calling the generation endpoint
- ❌ The Excel file with sheets

**Next Steps**:
1. Call the generation endpoint manually (see command above)
2. Check if you see the generation logs
3. Check if the Excel file is created
4. Fix the front-end to automatically call generation after creating DP2

---

**Key Point**: Creating DP2 metadata ≠ Generating Excel file

They are **two separate API calls**!
