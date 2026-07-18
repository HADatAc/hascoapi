# Phase 5: End-to-End Integration Testing

**Date:** 2026-07-17  
**Purpose:** Test complete workflow from WKF upload to ProcessBasedStudy auto-generation  
**Priority:** HIGH  

---

## Test Overview

This phase validates the complete integration of ProcessBasedStudy functionality:
1. Upload Workflow (WKF) file via Drupal UI
2. Verify Process entity creation in hascoapi
3. Verify ProcessBasedStudy auto-generation
4. Verify study metadata propagation
5. Verify DSG generation capability
6. Verify Study Review Queue functionality

---

## Prerequisites

✅ hascoapi server running on http://localhost:9001  
✅ Drupal running on http://localhost:8080  
✅ Test user 'test' with admin privileges  
✅ Sample WKF file available: `/Users/pp3223/git/wkf-dev/wkf/correct/WKF-ASPIRACAO_SECRECOES_PSMR_0001_CTT.xlsx`  
✅ All automated tests passing (409 tests, 100% pass rate)  
✅ Study Review Queue empty database handling fixed  

---

## Test Procedure

### Test 1: Workflow Upload

**Objective:** Upload WKF file and verify Process creation

**Steps:**
1. Open browser to http://localhost:8080
2. Login as user 'test' (password: test1234)
3. Navigate to Manage → Workflows → Upload Workflow
4. Select file: `WKF-ASPIRACAO_SECRECOES_PSMR_0001_CTT.xlsx`
5. Complete upload form
6. Submit workflow

**Expected Results:**
- ✅ Upload successful message
- ✅ Workflow appears in Workflows list
- ✅ Process entities created in hascoapi
- ✅ Tasks created and linked to Process
- ✅ No errors in upload log

**Validation:**
```bash
# Check Process count
curl -s "http://localhost:9001/hascoapi/api/process/elements/total" | jq

# Get Process URIs
curl -s "http://localhost:9001/hascoapi/api/process/elements/10/0" | jq

# Expected: At least 1 Process entity
```

---

### Test 2: ProcessBasedStudy Auto-Generation

**Objective:** Verify automatic ProcessBasedStudy creation from Process

**Steps:**
1. After WKF upload completes
2. Check hascoapi logs for auto-generation message
3. Query ProcessBasedStudy API

**Expected Results:**
- ✅ ProcessBasedStudy automatically created
- ✅ Study URI derived from Process URI (WKF-xxx → STD-xxx)
- ✅ Study metadata fields populated (auto-generated or from WKF)
- ✅ Link to parent Process established

**Validation:**
```bash
# Check ProcessBasedStudy count
curl -s "http://localhost:9001/hascoapi/api/processbasedstudy/elements/total" | jq

# Get ProcessBasedStudy details
curl -s "http://localhost:9001/hascoapi/api/processbasedstudy/elements/10/0" | jq

# Verify study ID format (should be STD-xxx)
curl -s "http://localhost:9001/hascoapi/api/processbasedstudy/elements/10/0" | jq '.[].studyID'

# Expected: Study with ID starting with "STD-"
```

---

### Test 3: Study Metadata Propagation

**Objective:** Verify study metadata fields are correctly populated

**Steps:**
1. Retrieve ProcessBasedStudy via API
2. Check each metadata field
3. Verify auto-generation rules applied correctly

**Expected Results:**
- ✅ studyID: Derived from Process URI (STD-ASPIRACAO_SECRECOES_PSMR_0001)
- ✅ studyTitle: Uses Process label or auto-generated
- ✅ specificAims: Extracted from Process comment or auto-generated
- ✅ significance: Auto-generated default or from WKF
- ✅ institution: From user profile or default
- ✅ principalInvestigator: From user profile or workflow creator
- ✅ contactEmail: From user profile
- ✅ startDate: From workflow creation date
- ✅ endDate: Optional, may be empty

**Validation:**
```bash
# Get specific ProcessBasedStudy
STUDY_URI="http://pmsr.net/ont/pmsr%23STD-ASPIRACAO_SECRECOES_PSMR_0001"
curl -s "http://localhost:9001/hascoapi/api/processbasedstudy/${STUDY_URI}" | jq

# Check required fields
curl -s "http://localhost:9001/hascoapi/api/processbasedstudy/${STUDY_URI}" | jq '{
  studyID,
  studyTitle,
  processUri,
  specificAims,
  significance,
  institution,
  principalInvestigator,
  contactEmail,
  startDate,
  endDate
}'
```

---

### Test 4: Study Review Queue

**Objective:** Verify Study Review Queue displays auto-generated study

**Steps:**
1. Navigate to Drupal: Studies → Review Queue
2. Verify study appears in queue
3. Check study metadata display
4. Test edit link functionality

**Expected Results:**
- ✅ Study appears in review queue (auto-generated metadata triggers review flag)
- ✅ Study details displayed correctly
- ✅ Edit link navigates to Edit ProcessBasedStudy form
- ✅ Form pre-populated with current values
- ✅ Can update study metadata fields
- ✅ Save redirects to study list

**Manual UI Testing:**
- Open: http://localhost:8080/std/review/processbasedstudies
- Verify table shows study with STD-ASPIRACAO_SECRECOES_PSMR_0001
- Click Edit button
- Verify form fields populated
- Update at least one field (e.g., Specific Aims)
- Save and verify update persisted

---

### Test 5: DSG Generation

**Objective:** Verify DSG Excel file generation from ProcessBasedStudy

**Steps:**
1. From Study Review Queue or Study list
2. Click "Download DSG" button
3. Verify Excel file downloads
4. Open file and verify structure

**Expected Results:**
- ✅ DSG Excel file downloads successfully
- ✅ File named: `DSG_STD-ASPIRACAO_SECRECOES_PSMR_0001.xlsx`
- ✅ Contains required sheets:
  - InfoSheet
  - Namespaces
  - Variables
  - CodeBook (optional)
  - StudyDesign
- ✅ InfoSheet contains correct study metadata
- ✅ StudyDesign references Process and Tasks
- ✅ Variables derived from Process structure

**Validation:**
```bash
# Test DSG generation API
STUDY_URI="http://pmsr.net/ont/pmsr%23STD-ASPIRACAO_SECRECOES_PSMR_0001"
curl -s "http://localhost:9001/hascoapi/api/processbasedstudy/dsg/${STUDY_URI}" \
  -o /tmp/test_dsg.xlsx

# Verify file created
ls -lh /tmp/test_dsg.xlsx

# Check file is valid Excel
file /tmp/test_dsg.xlsx
# Expected: Microsoft Excel 2007+
```

---

### Test 6: CTT Editor Integration

**Objective:** Verify CTT editor displays study metadata panel

**Steps:**
1. Navigate to CTT Editor for workflow
2. Verify study metadata panel visible
3. Check metadata fields displayed
4. Test link to ProcessBasedStudy

**Expected Results:**
- ✅ Study metadata panel renders in CTT editor
- ✅ Shows study ID, title, institution, PI
- ✅ Link to Edit ProcessBasedStudy form works
- ✅ Panel updates when study metadata changes

**Manual UI Testing:**
- Open CTT editor for uploaded workflow
- Look for study metadata panel (usually right sidebar)
- Verify study information displayed
- Click edit link, update field, return to editor
- Verify panel reflects updated data

---

## Test Data

### Sample WKF File
**File:** `/Users/pp3223/git/wkf-dev/wkf/correct/WKF-ASPIRACAO_SECRECOES_PSMR_0001_CTT.xlsx`  
**Description:** Secretion aspiration clinical training workflow  
**Structure:**
- InfoSheet: Basic workflow metadata
- Namespaces: Standard RDF prefixes
- ProcessStems: Workflow template
- Processes: 1 main process with tasks
- Tasks: Multiple tasks forming workflow sequence
- RequiredInstruments: Medical equipment references

**Expected Entities:**
- **Process:** `pmsr:WKF-ASPIRACAO_SECRECOES_PSMR_0001/PROC/0001`
- **ProcessBasedStudy:** `pmsr:STD-ASPIRACAO_SECRECOES_PSMR_0001`
- **Tasks:** Multiple vstoi:Task instances

---

## Success Criteria

### Mandatory
✅ WKF upload completes without errors  
✅ Process entities created in triplestore  
✅ ProcessBasedStudy auto-generated  
✅ Study ID derived correctly (STD- prefix)  
✅ Process URI link established  
✅ Study Review Queue displays study  
✅ DSG generation produces valid Excel file  

### Optional (Nice-to-Have)
⚠️ All 9 study metadata fields populated  
⚠️ CTT editor panel displays study info  
⚠️ Edit form validation working  
⚠️ Study metadata enrichment workflow functional  

---

## Known Issues / Limitations

⚠️ **Python openpyxl:** Python 3.14.4 has pyexpat bug, use 3.11/3.12 for automated validation  
⚠️ **WKF Validation:** Manual validation required until Python environment fixed  
⚠️ **Study Metadata:** If WKF file doesn't have columns S-AA, all metadata will be auto-generated  
⚠️ **CTT Editor:** Requires workflow context, may need separate testing  

---

## Troubleshooting

### Issue: WKF Upload Fails
**Symptoms:** Error message during upload  
**Possible Causes:**
- Invalid Excel format
- Missing required sheets
- Invalid URI patterns
- Duplicate URIs

**Solutions:**
1. Validate WKF file structure against spec
2. Check hascoapi logs: `/Users/pp3223/git/hascoapi/logs/`
3. Verify Fuseki triplestore running: http://localhost:3030
4. Check for namespace conflicts

### Issue: ProcessBasedStudy Not Created
**Symptoms:** Process exists but no ProcessBasedStudy  
**Possible Causes:**
- Auto-generation disabled
- IngestionWorker not calling generator
- Invalid Process URI format

**Solutions:**
1. Check hascoapi logs for auto-generation messages
2. Verify ProcessBasedStudyGenerator.generateFromProcess() called
3. Test API endpoint manually:
```bash
PROCESS_URI="http://pmsr.net/ont/pmsr%23/WKF-xxx/PROC/0001"
curl -X POST "http://localhost:9001/hascoapi/api/processbasedstudy/generate/${PROCESS_URI}"
```

### Issue: Study Review Queue Empty
**Symptoms:** No studies in review queue  
**Possible Causes:**
- All study metadata complete (no auto-generated defaults)
- ProcessBasedStudy not created yet
- Empty database handling issue (should be fixed)

**Solutions:**
1. Check ProcessBasedStudy count via API
2. Verify needsMetadataEnrichment() logic
3. Test with known auto-generated study

### Issue: DSG Generation Fails
**Symptoms:** 500 error or empty file  
**Possible Causes:**
- ProcessBasedStudy missing
- Process missing Tasks
- Invalid template structure

**Solutions:**
1. Verify ProcessBasedStudy exists via API
2. Check Process has Tasks via API
3. Review ProcessBasedStudyDSGGen logs
4. Test with simple 1-task workflow first

---

## Test Results Log

### Test Run 1: [DATE/TIME]
**Tester:** [NAME]  
**Environment:** Local development (macOS)  

**Test 1: Workflow Upload**
- Status: [ ] PASS / [ ] FAIL / [ ] BLOCKED
- Notes:

**Test 2: ProcessBasedStudy Auto-Generation**
- Status: [ ] PASS / [ ] FAIL / [ ] BLOCKED
- Notes:

**Test 3: Study Metadata Propagation**
- Status: [ ] PASS / [ ] FAIL / [ ] BLOCKED
- Notes:

**Test 4: Study Review Queue**
- Status: [ ] PASS / [ ] FAIL / [ ] BLOCKED
- Notes:

**Test 5: DSG Generation**
- Status: [ ] PASS / [ ] FAIL / [ ] BLOCKED
- Notes:

**Test 6: CTT Editor Integration**
- Status: [ ] PASS / [ ] FAIL / [ ] BLOCKED
- Notes:

**Overall Result:** [ ] ALL PASS / [ ] PARTIAL / [ ] FAIL  
**Blockers:**  
**Notes:**

---

## Next Steps After Testing

1. **If All Tests Pass:**
   - Update implementation status to "Production Ready"
   - Create deployment checklist
   - Plan staging environment deployment
   - Prepare production rollout

2. **If Issues Found:**
   - Document bugs in separate issues
   - Prioritize fixes (blocker vs. enhancement)
   - Fix critical issues
   - Retest

3. **Documentation Updates:**
   - Update user guide with WKF upload instructions
   - Document study metadata enrichment workflow
   - Create video tutorials for UI operations
   - Update API documentation with examples

---

## Staging Deployment Preparation

Once end-to-end testing passes locally, prepare for staging deployment:

### Pre-Deployment Checklist
- [ ] All 409 automated tests passing
- [ ] End-to-end WKF workflow tested successfully
- [ ] DSG generation validated
- [ ] Study Review Queue functional
- [ ] Documentation complete
- [ ] Database backup completed
- [ ] Rollback plan documented

### Staging Environment Requirements
- [ ] hascoapi deployed and running
- [ ] Drupal instance configured
- [ ] Fuseki triplestore accessible
- [ ] Network connectivity validated
- [ ] SSL certificates if applicable
- [ ] Monitoring/logging configured

### Deployment Steps
1. Backup production database
2. Deploy hascoapi backend (31 commits)
3. Deploy Drupal modules (std, wkf updates)
4. Clear all caches
5. Run smoke tests
6. Execute end-to-end test suite
7. Monitor for 24-48 hours
8. Collect user feedback

---

**Test Report Generated:** 2026-07-17  
**Next Review:** After Test Execution  
**Status:** READY FOR TESTING  
