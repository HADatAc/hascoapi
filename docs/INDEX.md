# HADatAc API - Documentation Index

**Last update:** 2026-02-12  
**Version:** 2.0

---

## 📚 Documentation Structure

This directory contains all technical documentation for the HADatAc API project, organized by topic.

---

## 🆕 Main Documents (Updated)

### **WKF (Workflow) - Complete Documentation**

1. **[WKF-INGESTION-COMPLETE-FIX-FINAL.md](./WKF-INGESTION-COMPLETE-FIX-FINAL.md)** ⭐ | **[English Version](./WKF-INGESTION-COMPLETE-FIX-FINAL-EN.md)**
   - **What it is:** COMPLETE documentation of the solution to all WKF ingestion problems
   - **When to use:** To understand ALL the fixes implemented
   - **Content:**
     - 5 problems identified and resolved
     - Before/after code
     - Modified files
     - Validation tests
     - Status: ✅ **PRODUCTION READY**

2. **[WKF-TECHNICAL-ARCHITECTURE.md](./WKF-TECHNICAL-ARCHITECTURE.md)** ⭐ | **[English Version](./WKF-TECHNICAL-ARCHITECTURE-EN.md)**
   - **What it is:** Technical architecture and data flows of WKF
   - **When to use:** To understand HOW the system works
   - **Content:**
     - Architecture diagrams
     - Upload, ingestion, uningest, delete flows
     - Data structure in Fuseki
     - Main classes and responsibilities
     - Validation tests

3. **[WKF-QUICK-REFERENCE.md](./WKF-QUICK-REFERENCE.md)** ⭐
   - **What it is:** Quick reference guide for WKF operations
   - **When to use:** For quick consultations on how to do X
   - **Content:**
     - Available operations (create, list, ingest, uningest, delete)
     - API call examples
     - Common problems and solutions
     - File locations
     - How to debug

---

## 📖 WKF Document History (For Context)

### **Initial Implementation**
- `WKF-COMPLETE-INTEGRATION-FINAL.md` - Complete WKF implementation (initial version)
- `WKF-COMPLETE-INTEGRATION.md` - WKF system integration
- `WKF-COMPLETE-SUMMARY.md` - Implementation summary

### **Problem Debugging**
- `WKF-DEBUG-FIND-METHOD.md` - Debug of find() method
- `WKF-DEBUG-LISTING-ISSUE.md` - Debug of listing problems
- `WKF-FIX-404-SUMMARY.md` - 404 error fixes
- `WKF-FIX-NULL-STATUS-ISSUE.md` - Null status fix

### **Ingestion**
- `WKF-INGESTION-DEBUG.md` - Initial ingestion debug
- `WKF-INGESTION-FILE-NOT-SENT.md` - File not sent problem
- `WKF-INGESTION-FIXES-APPLIED.md` - Applied fixes
- `WKF-INGESTION-IMPLEMENTATION.md` - Ingestion implementation
- `WKF-INGESTION-STATUS.md` - Implementation status

### **Upload**
- `WKF-UPLOAD-ISSUE-ANALYSIS.md` - Upload problem analysis
- `WKF-UPLOAD-MANUAL-TEST.md` - Manual upload tests

### **Others**
- `WKF-ERROR-DICTIONARY-IMPLEMENTATION.md` - Error dictionary implementation
- `WKF-ERROR-DICTIONARY.md` - WKF error dictionary
- `WKF-FIX-FILE-PATH-FINAL.md` - Final path fixes
- `WKF-FIX-JSON-FILTER-MISSING.md` - JSON filter fix
- `WKF-FIX-MULTIPLE-BODY-READERS.md` - Multiple body readers fix
- `WKF-FINAL-FIX-STATUS-DEFAULT.md` - Default status fix

---

## 📄 DP2 Documents (Data Point 2)

### **Generation Problems**
- `DP2-404-FIX.md` - 404 error fix
- `DP2-COMPLETE-SOLUTION.md` - Complete solution
- `DP2-DEBUG-CHANGES-SUMMARY.md` - Debug changes summary
- `DP2-DEBUG-QUICKSTART.md` - Debug quick guide
- `DP2-DEBUGGING-GUIDE.md` - Complete debugging guide
- `DP2-GENERATION-CHANGES.md` - Generation changes
- `DP2-GENERATION-DEBUG.md` - Generation debug
- `DP2-GENERATION-QUICKTEST.md` - Quick tests
- `DP2-GENERATION-VS-METADATA.md` - Generation vs metadata comparison
- `DP2-WHY-SHEETS-EMPTY.md` - Why sheets are empty

---

## 🔧 General Fix Documents

### **Generation Problems (404)**
- `DEBUG-404-GENERATION-ISSUE.md` - 404 error debug in generation
- `FIX-404-ALL-GENERATES-FINAL.md` - Final fix for all generates
- `FIX-404-GENERATION-COMPLETE.md` - Complete generation fix
- `FIX-404-ROOT-CAUSE-WINDOWS-PATH.md` - Root cause: Windows path
- `FIX-ALL-GENERATORS-PATH-NORMALIZATION.md` - Path normalization

### **Upload Problems**
- `UPLOAD-API-FIXES-APPLIED.md` - Upload API fixes applied

### **Null Return Problems**
- `FIX-NULL-RETURN-ISSUE.md` - Null return fix

### **Path Problems**
- `TEST-PATH-PROBLEM.md` - Path problem test

---

## 🎯 Frontend Documents

- `FRONTEND-BUG-REPORT-GENERATOR-CONFIRMATION.md` - Generator confirmation bug report
- `FRONTEND-MUST-CALL-UPLOAD-API.md` - Frontend must call upload API
- `FRONTEND-NOT-CALLING-GENERATION-API.md` - Frontend not calling generation API

---

## 📊 Status Documents

- `COMPLETE-STATUS-AND-NEXT-STEPS.md` - Complete status and next steps

---

## 🔗 API Documents

- `HASCOAPI-ENDPOINT-V2.md` - API endpoints v2 documentation

---

## 🗂️ How to Use This Documentation

### **Scenario 1: I want to understand what was done in WKF**
👉 Read: `WKF-INGESTION-COMPLETE-FIX-FINAL.md`

### **Scenario 2: I want to understand WKF architecture**
👉 Read: `WKF-TECHNICAL-ARCHITECTURE.md`

### **Scenario 3: I need to perform a WKF operation**
👉 Read: `WKF-QUICK-REFERENCE.md`

### **Scenario 4: I'm having a problem with WKF**
👉 Check: `WKF-QUICK-REFERENCE.md` (section "Common Problems")

### **Scenario 5: I want to implement a new MT similar to WKF**
👉 Read: 
1. `WKF-TECHNICAL-ARCHITECTURE.md` (understand pattern)
2. `WKF-INGESTION-COMPLETE-FIX-FINAL.md` (avoid common errors)

### **Scenario 6: I'm having generation problems (404)**
👉 Read: 
1. `FIX-404-ROOT-CAUSE-WINDOWS-PATH.md`
2. `FIX-ALL-GENERATORS-PATH-NORMALIZATION.md`

### **Scenario 7: Problems with DP2**
👉 Read: `DP2-COMPLETE-SOLUTION.md`

---

## 📋 Checklist: Implement New Metadata Template

If you're going to create a new MT (e.g., XYZ), follow this checklist:

### **1. Create Data Model**
- [ ] Create `XYZ.java` extending `MetadataTemplate`
- [ ] Implement specific getters/setters
- [ ] Implement `find(String uri)` method
- [ ] Implement `save()` method
- [ ] Add defaults (e.g., status = "DRAFT")

### **2. Register in System**
- [ ] Add to `GenericFind.java`:
  ```java
  } else if (hascoType.equals(HASCO.XYZ)) {
      instance = XYZ.find(uri);
  }
  ```
- [ ] Add constant in `HASCO.java`:
  ```java
  public static final String XYZ = "http://hadatac.org/ont/hasco/XYZ";
  ```

### **3. Create API**
- [ ] Create `XYZAPI.java`
- [ ] Implement methods:
  - `getAll()` - List all
  - `getOne(String uri)` - Find by URI
  - `createElement()` - Create new
  - `deleteElement(String uri)` - Delete
- [ ] Add JSON filters if needed

### **4. Add Routes**
- [ ] Edit `conf/routes`:
  ```
  GET     /hascoapi/api/xyz/                          @org.hascoapi.console.controllers.restapi.XYZAPI.getAll()
  GET     /hascoapi/api/xyz/:uri                      @org.hascoapi.console.controllers.restapi.XYZAPI.getOne(uri: String)
  POST    /hascoapi/api/xyz/create/:json              @org.hascoapi.console.controllers.restapi.XYZAPI.createElement(json: String)
  POST    /hascoapi/api/xyz/delete/:uri               @org.hascoapi.console.controllers.restapi.XYZAPI.deleteElement(uri: String)
  ```

### **5. Implement Upload**
- [ ] Add handling in `IngestionAPI.uploadFile()`:
  ```java
  } else if (elementType.equals("xyz")) {
      XYZ xyz = XYZ.find(elementUri);
      if (xyz != null && xyz.getHasDataFileUri() != null) {
          dataFileUri = xyz.getHasDataFileUri();
      }
  }
  ```

### **6. Implement Ingestion**
- [ ] Create `XYZGenerator.java` (if needed)
- [ ] Implement .xlsx file parser
- [ ] Add logic to `IngestionAPI.ingest()`

### **7. Implement Uningest**
- [ ] Verify if `uningestMetadataTemplate()` already supports it (usually yes)
- [ ] Test that uningest preserves XYZ and DataFile

### **8. Test**
- [ ] Create XYZ via frontend
- [ ] Upload file
- [ ] Perform ingestion
- [ ] Verify triples in Fuseki
- [ ] Perform uningest
- [ ] Verify that XYZ still appears
- [ ] Perform delete
- [ ] Verify that XYZ disappeared

### **9. Document**
- [ ] Create `XYZ-QUICK-REFERENCE.md`
- [ ] Update `INDEX.md`
- [ ] Add usage examples

---

## 🔑 Important Concepts

### **GenericFind**
- It's the central "router" of the system
- Receives a URI, identifies the type, calls the correct `.find()`
- **CRITICAL:** If a type is not in `GenericFind`, it doesn't exist for the API

### **Named Graphs**
- Each MT has its data saved in a specific Named Graph
- Named Graph URI = DataFile URI
- Uningest deletes content but preserves structure (WKF + DataFile)

### **File Paths**
- Upload must save to: `var/resources/{DFL_URI}/{filename}`
- Ingestion must read from: `var/resources/{DFL_URI}/{filename}`
- **CRITICAL:** Paths must be consistent

### **DataFile Status**
- `UNPROCESSED`: File uploaded, awaiting ingestion
- `PROCESSED`: Content ingested into Fuseki
- `WORKING`: Ingestion in progress

### **JWT Authentication**
- All operations require valid token
- Header: `Authorization: Bearer {token}`

---

## 📊 Documentation Statistics

- **Total documents:** 48
- **WKF documents:** 23 (2 English translations)
- **DP2 documents:** 10
- **General fix documents:** 8
- **Main documents (updated):** 3 ⭐ (with English versions)
- **English translations:** 2 (WKF-TECHNICAL-ARCHITECTURE-EN.md, WKF-INGESTION-COMPLETE-FIX-FINAL-EN.md)

---

## 🚀 Next Steps

### **Future Documentation**
- [ ] Create contribution guide
- [ ] Add sequence diagrams
- [ ] Document structure of each existing MT
- [ ] Create performance tuning guide
- [ ] Add complete troubleshooting guide

### **System Improvements**
- [ ] Implement unit tests
- [ ] Add .xlsx file schema validation
- [ ] Improve error messages
- [ ] Implement API rate limiting
- [ ] Add SPARQL query cache

---

## 📞 Support

**Online Documentation:** `/docs` directory  
**Logs:** `logs/application.log`  
**Contact:** HADatAc Development Team

---

## 📝 How to Contribute to Documentation

1. **Create new document:**
   - Use Markdown template
   - Add to `INDEX.md`
   - Use naming: `{TYPE}-{SUBJECT}-{VERSION}.md`

2. **Update existing document:**
   - Add note at top: "Last update: YYYY-MM-DD"
   - Keep old versions for history

3. **Naming convention:**
   - `WKF-*` - Documents about Workflow
   - `DP2-*` - Documents about Data Point 2
   - `FIX-*` - Documents about fixes
   - `DEBUG-*` - Documents about debugging
   - `FRONTEND-*` - Documents about frontend

---

**Last update:** 2026-02-12  
**Maintained by:** HADatAc Development Team  
**Index Version:** 1.0

