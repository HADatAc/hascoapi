# WKF Upload Testing - Manual Instructions

**Date:** 2026-07-17  
**Purpose:** Step-by-step instructions for testing WKF upload and ProcessBasedStudy auto-generation  
**Status:** READY TO EXECUTE  

---

## Test Setup Complete ✅

- ✅ hascoapi running on http://localhost:9001
- ✅ Drupal running on http://localhost:8080
- ✅ Test user 'test' with admin privileges (password: test1234)
- ✅ Study Review Queue fixed (HTTP 200, handles empty database)
- ✅ Sample WKF file available: `/Users/pp3223/git/wkf-dev/wkf/correct/WKF-ASPIRACAO_SECRECOES_PSMR_0001_CTT.xlsx`

---

## Pre-Test Verification

Before uploading, verify the system is ready:

```bash
# 1. Check hascoapi is running
curl -s "http://localhost:9001/hascoapi/api/processbasedstudy/elements/total" | jq
# Expected: {"isSuccessful":true,"body":"{\"total\":0}"}

# 2. Check current Process count
curl -s "http://localhost:9001/hascoapi/api/process/elements/total" | jq

# 3. Verify Drupal is accessible
curl -s -o /dev/null -w "%{http_code}\n" "http://localhost:8080"
# Expected: 200
```

---

## Manual Test Procedure

### Step 1: Login to Drupal

1. Open browser to: http://localhost:8080/user/login
2. Username: `test`
3. Password: `test1234`
4. Click "Log in"

**Expected:** Successful login, redirected to user dashboard

---

### Step 2: Navigate to Workflow Upload

**Option A - Via Menu:**
1. Click "Manage" in main menu
2. Click "Workflows" submenu
3. Click "Upload Workflow"

**Option B - Direct URL:**
- Navigate to: http://localhost:8080/std/manage/addworkflow/upload

**Expected:** Workflow upload form appears

---

### Step 3: Upload WKF File

1. **Select File:**
   - Click "Choose File" or file picker
   - Navigate to: `/Users/pp3223/git/wkf-dev/wkf/correct/`
   - Select: `WKF-ASPIRACAO_SECRECOES_PSMR_0001_CTT.xlsx`

2. **Form Fields:** (Most should auto-populate from file)
   - Verify fields are populated from InfoSheet
   - Check that no errors appear

3. **Submit:**
   - Click "Upload" or "Submit" button
   - Wait for processing (may take 10-30 seconds)

**Expected Results:**
- ✅ Green success message appears
- ✅ "Workflow uploaded successfully" or similar
- ✅ No error messages
- ✅ Redirected to workflow list or workflow details

**If Errors Occur:**
- Screenshot the error message
- Check browser console (F12) for JavaScript errors
- Check Drupal error log: `/opt/homebrew/var/log/httpd/error_log`

---

### Step 4: Verify Workflow in List

1. Navigate to: http://localhost:8080/std/manage/workflows (or similar)
2. Look for uploaded workflow in the list

**Expected:**
- ✅ Workflow "WKF-ASPIRACAO_SECRECOES_PSMR_0001_CTT" appears
- ✅ Status shows "Active" or "Current"
- ✅ Can click to view details

---

## Automated Verification After Upload

Once upload completes, run these checks:

```bash
#!/bin/bash

echo "=== WKF Upload Verification ==="
echo ""

# 1. Check Process creation
echo "1. Checking Process count..."
PROCESS_COUNT=$(curl -s "http://localhost:9001/hascoapi/api/process/elements/total" | jq -r '.body' | jq -r '.total')
echo "   Process count: $PROCESS_COUNT"
if [ "$PROCESS_COUNT" -gt "0" ]; then
  echo "   ✅ Process entities created"
else
  echo "   ❌ No Process entities found"
fi

echo ""

# 2. Get Process details
echo "2. Process details:"
curl -s "http://localhost:9001/hascoapi/api/process/elements/10/0" | jq -r '.body[0] | {uri, label, processUri: .uri}'

echo ""

# 3. Check ProcessBasedStudy auto-generation
echo "3. Checking ProcessBasedStudy auto-generation..."
PBS_COUNT=$(curl -s "http://localhost:9001/hascoapi/api/processbasedstudy/elements/total" | jq -r '.body' | jq -r '.total')
echo "   ProcessBasedStudy count: $PBS_COUNT"
if [ "$PBS_COUNT" -gt "0" ]; then
  echo "   ✅ ProcessBasedStudy auto-generated"
else
  echo "   ❌ ProcessBasedStudy NOT created (check logs)"
fi

echo ""

# 4. Get ProcessBasedStudy details
echo "4. ProcessBasedStudy details:"
curl -s "http://localhost:9001/hascoapi/api/processbasedstudy/elements/10/0" | jq -r '.body[0] | {
  uri,
  studyID,
  studyTitle,
  processUri,
  specificAims,
  significance,
  institution,
  principalInvestigator
}'

echo ""

# 5. Check Study Review Queue
echo "5. Testing Study Review Queue..."
COOKIE_JAR="/tmp/verify_test.txt"
rm -f $COOKIE_JAR

# Login
LOGIN_PAGE=$(curl -s -c $COOKIE_JAR "http://localhost:8080/user/login")
FORM_BUILD_ID=$(echo "$LOGIN_PAGE" | grep -o 'name="form_build_id" value="[^"]*"' | sed 's/.*value="\([^"]*\)".*/\1/')
FORM_TOKEN=$(echo "$LOGIN_PAGE" | grep -o 'name="form_token" value="[^"]*"' | sed 's/.*value="\([^"]*\)".*/\1/')

curl -s -b $COOKIE_JAR -c $COOKIE_JAR -L \
  -X POST "http://localhost:8080/user/login" \
  -d "name=test" \
  -d "pass=test1234" \
  -d "form_build_id=$FORM_BUILD_ID" \
  -d "form_token=$FORM_TOKEN" \
  -d "form_id=user_login_form" \
  -d "op=Log in" > /dev/null

STATUS=$(curl -s -b $COOKIE_JAR -o /dev/null -w "%{http_code}" "http://localhost:8080/std/review/processbasedstudies")
echo "   Review Queue Status: HTTP $STATUS"
if [ "$STATUS" = "200" ]; then
  echo "   ✅ Review Queue accessible"
  
  # Check if study appears in queue
  QUEUE_HTML=$(curl -s -b $COOKIE_JAR "http://localhost:8080/std/review/processbasedstudies")
  if echo "$QUEUE_HTML" | grep -q "STD-"; then
    echo "   ✅ Study appears in Review Queue"
  else
    echo "   ⚠️  Check if study needs review (may have complete metadata)"
  fi
else
  echo "   ❌ Review Queue error"
fi

echo ""
echo "=== Verification Complete ==="
```

Save this script to `/tmp/verify_wkf_upload.sh` and run:

```bash
chmod +x /tmp/verify_wkf_upload.sh
/tmp/verify_wkf_upload.sh
```

---

## Expected Test Results

### After Successful Upload

**1. Process Entity Created:**
```json
{
  "uri": "http://pmsr.net/ont/pmsr#/WKF-ASPIRACAO_SECRECOES_PSMR_0001_CTT/PROC/0001",
  "label": "Aspiração de Secreções PSMR",
  "hasVersion": "0001"
}
```

**2. ProcessBasedStudy Auto-Generated:**
```json
{
  "uri": "http://pmsr.net/ont/pmsr#STD-ASPIRACAO_SECRECOES_PSMR_0001_CTT",
  "studyID": "STD-ASPIRACAO_SECRECOES_PSMR_0001_CTT",
  "studyTitle": "Aspiração de Secreções PSMR",
  "processUri": "http://pmsr.net/ont/pmsr#/WKF-ASPIRACAO_SECRECOES_PSMR_0001_CTT/PROC/0001",
  "specificAims": "Clinical simulation study",
  "significance": "Clinical simulation study",
  "institution": "PMSR Repository",
  "principalInvestigator": "test"
}
```

**3. Study Review Queue:**
- Study appears with status "Needs Review"
- Auto-generated metadata flagged
- Edit button functional
- DSG download button visible

---

## UI Testing Checklist

### Study Review Queue Tests

1. **Navigate to Review Queue:**
   - URL: http://localhost:8080/std/review/processbasedstudies
   - ✅ Page loads without errors
   - ✅ Study appears in table
   - ✅ Shows study ID (STD-xxx)
   - ✅ Shows process name
   - ✅ Shows status/flags

2. **Edit Study Metadata:**
   - Click "Edit" button on study row
   - ✅ Redirected to Edit ProcessBasedStudy form
   - ✅ Form pre-populated with current values
   - ✅ All 11 fields visible and editable
   - Update at least 2 fields:
     - Specific Aims: Add detailed objectives
     - Significance: Add clinical relevance
   - Click "Save"
   - ✅ Success message appears
   - ✅ Redirected to study list
   - ✅ Changes persisted (verify via API or re-open form)

3. **Download DSG:**
   - From Review Queue or Study list
   - Click "Download DSG" button
   - ✅ Excel file downloads
   - ✅ Filename: `DSG_STD-ASPIRACAO_SECRECOES_PSMR_0001_CTT.xlsx`
   - Open file:
     - ✅ InfoSheet present with study metadata
     - ✅ StudyDesign sheet present
     - ✅ Variables sheet present
     - ✅ Data looks correct

---

## Add ProcessBasedStudy Form Test

**Direct Test of Form (without WKF upload):**

1. Navigate to: http://localhost:8080/std/manage/addprocessbasedstudy

2. **Test Form Validation:**
   - Leave Process URI empty → Submit
   - ✅ Error: "Process URI is required"
   
   - Enter invalid Process URI → Submit
   - ✅ Error: "Process does not exist" or validation error

   - Enter valid Process URI (from previous upload)
   - Fill optional fields
   - Submit
   - ✅ Success message
   - ✅ Study created with specified metadata

3. **Test Auto-Generation:**
   - Enter only Process URI
   - Leave all metadata fields empty
   - Submit
   - ✅ Study created with auto-generated metadata
   - ✅ Verify via API that defaults were applied

---

## Troubleshooting

### Upload Fails

**Error: "Invalid file format"**
- WKF file may be corrupted
- Try different WKF file from `/Users/pp3223/git/wkf-dev/wkf/`

**Error: "Missing required sheet"**
- WKF file doesn't conform to spec
- Check file has: InfoSheet, Namespaces, Processes, Tasks

**Error: "Duplicate URI"**
- Workflow already uploaded
- Clean database or use different WKF file

### ProcessBasedStudy Not Created

**Check hascoapi logs:**
```bash
cd /Users/pp3223/git/hascoapi
tail -100 logs/application.log | grep -i "ProcessBasedStudy\|ingestion"
```

**Manually trigger generation:**
```bash
# Get Process URI from upload
PROCESS_URI="http://pmsr.net/ont/pmsr%23/WKF-ASPIRACAO_SECRECOES_PSMR_0001_CTT/PROC/0001"

# Trigger manual generation (if endpoint exists)
curl -X POST "http://localhost:9001/hascoapi/api/processbasedstudy/generate/${PROCESS_URI}"
```

### Study Not in Review Queue

**Possible reasons:**
- Study has complete metadata (not auto-generated)
- Review logic needs adjustment
- Check controller logic in StudyReviewQueueController.php

**Verify study exists:**
```bash
curl -s "http://localhost:9001/hascoapi/api/processbasedstudy/elements/10/0" | jq
```

---

## Success Criteria Summary

✅ **PASS** if all of these are true:
1. WKF file uploads without errors
2. Process entity created in hascoapi
3. ProcessBasedStudy auto-generated
4. Study ID derived correctly (STD- prefix)
5. Study appears in Review Queue OR is viewable in study list
6. Edit form accessible and functional
7. DSG generation produces valid Excel file

⚠️ **PARTIAL** if:
- Upload works but some features have minor issues
- Manual workarounds needed
- Documentation gaps

❌ **FAIL** if:
- Upload errors out
- Process not created
- ProcessBasedStudy not auto-generated
- Critical features broken

---

## Next Actions After Testing

**If Tests Pass:**
1. Mark Phase 5 complete ✅
2. Update FINAL_COMPLETE_TEST_REPORT.md
3. Create staging deployment plan
4. Document any UI/UX improvements needed

**If Tests Fail:**
1. Document specific failures
2. Identify root causes
3. Fix blocking issues
4. Retest
5. Update test documentation

---

**Testing Instructions Created:** 2026-07-17  
**Ready for Execution:** YES  
**Estimated Time:** 15-20 minutes  
**Requires:** Browser, terminal access, test user credentials  
