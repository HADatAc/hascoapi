# WKF Ingestion Compliance Review
**Date:** July 3, 2026  
**Specification:** WKF-SPEC-V1.md (v1.0, June 30, 2026)  
**Implementation:** HASCOAPI WKF Ingestion System

---

## Executive Summary

This document reviews the HASCOAPI WKF ingestion implementation against the official WKF Specification v1.0. The review identifies areas of **full compliance**, **partial compliance**, and **non-compliance**, with specific recommendations for achieving full specification compliance.

**Overall Assessment:** ⚠️ **Partial Compliance**

The implementation successfully handles the core WKF ingestion workflow and processes the required sheets correctly. However, several **critical validation requirements** from the WKF-SPEC-V1 are **not implemented**, which may allow invalid WKF files to be ingested and could lead to data quality issues.

---

## Compliance Matrix

| Category | Spec Requirement | Implementation Status | Priority | Notes |
|----------|-----------------|----------------------|----------|-------|
| **InfoSheet Structure** | Exactly 7 rows (1 header + 6 data) | ❌ Not Validated | HIGH | No row count validation |
| **InfoSheet Columns** | Exactly 2 columns (Attribute, Value) | ❌ Not Validated | MEDIUM | No column count validation |
| **InfoSheet Fields** | Required fields with # prefix | ✅ Validated | - | MTSheet.java validates keys |
| **Prohibited Fields** | No hasWorkflowID, label, comment, versionNumber | ❌ Not Validated | MEDIUM | Allows extra fields |
| **hasVersion Format** | Must be numeric (no dates/text) | ❌ Not Validated | MEDIUM | Accepts any string value |
| **Sheet Ordering** | Specific sheet order (0-5) | ❌ Not Validated | LOW | Order not enforced |
| **Sheet Presence** | All 6 sheets required | ✅ Validated | - | MTSheet.java validates presence |
| **RDF Type Constraints** | Specific types per entity | ⚠️ Partially Validated | HIGH | Sets hascoType but doesn't validate |
| **CTT Task Types** | UserTask, ApplicationTask, etc. | ❌ Not Validated | HIGH | All tasks use generic VSTOI.TASK |
| **URI Patterns** | /PST/, /PROC/, /TSK/, /RIN/ | ❌ Not Validated | MEDIUM | No URI pattern validation |
| **Temporal Dependencies** | DAG validation, valid operators | ❌ Not Validated | **CRITICAL** | No DAG or operator validation |
| **Task Hierarchy** | No circular references | ❌ Not Validated | **CRITICAL** | No cycle detection |
| **Reference Integrity** | Valid cross-sheet references | ❌ Not Validated | HIGH | No validation of URIs |
| **Multi-Value Properties** | Semicolon/pipe separation | ✅ Implemented | - | WKFGenerator splits values |
| **Required Properties** | Per-entity required fields | ❌ Not Validated | HIGH | No completeness checks |

---

## Detailed Findings

### 1. InfoSheet Validation ⚠️ Partial Compliance

#### Spec Requirements (Section 5.1):
```
1. Exactly 2 columns: Attribute | Value
2. Exactly 6 rows after header (7 total rows)
3. Required fields with # prefix
4. Prohibited fields: hasWorkflowID, label, comment, versionNumber
5. hasVersion must be numeric (e.g., "1.0", not "2026-06-30")
```

#### Implementation Status:

**✅ COMPLIANT:**
- Required field validation via `MTSheet.getSheetsForType(MT_WKF)`:
  ```java
  Arrays.asList(
      "hasDependencies",
      "ProcessStems",
      "Processes", 
      "Tasks",
      "RequiredInstruments",
      "hasVersion"
  )
  ```
- [BaseAnnotator.java](app/org/hascoapi/ingestion/BaseAnnotator.java#L13-L80) validates field presence

**❌ NON-COMPLIANT:**
- ❌ No validation that InfoSheet has exactly 7 rows
- ❌ No validation that InfoSheet has exactly 2 columns
- ❌ No validation that hasVersion is numeric
- ❌ No validation against prohibited fields
- ❌ No validation that field values use # prefix (e.g., #Namespaces, not Namespaces)

**Impact:** The implementation may accept malformed WKF files with:
- Extra rows/fields beyond the required 6
- Non-numeric version numbers (e.g., dates or text)
- Missing # prefix in sheet pointers
- Prohibited legacy fields from earlier spec versions

**Recommendation:**
Add validation in [AnnotateWKF.java](app/org/hascoapi/ingestion/AnnotateWKF.java) after loading catalog:
```java
// After loadCatalog call
if (mapCatalog != null) {
    validateInfoSheetStructure(dataFile, mapCatalog);
}

private static boolean validateInfoSheetStructure(DataFile dataFile, Map<String, String> catalog) {
    RecordFile infoSheet = dataFile.getRecordFile();
    
    // Validate row count (should be exactly 6 data rows + 1 header = 7 total)
    if (infoSheet.getRecords().size() != 6) {
        dataFile.getLogger().printExceptionByIdWithArgs("WKF_00001", 
            "InfoSheet must have exactly 6 data rows, found " + infoSheet.getRecords().size());
        return false;
    }
    
    // Validate hasVersion is numeric
    String version = catalog.get("hasVersion");
    if (version != null && !version.matches("^\\d+(\\.\\d+)*$")) {
        dataFile.getLogger().printExceptionByIdWithArgs("WKF_00001", 
            "hasVersion must be numeric (e.g., '1.0'), found: " + version);
        return false;
    }
    
    // Check for prohibited fields
    String[] prohibited = {"hasWorkflowID", "label", "comment", "versionNumber"};
    for (String field : prohibited) {
        if (catalog.containsKey(field)) {
            dataFile.getLogger().printExceptionByIdWithArgs("WKF_00001",
                "InfoSheet contains prohibited field: " + field);
            return false;
        }
    }
    
    return true;
}
```

---

### 2. Sheet Ordering ⚠️ Not Validated

#### Spec Requirements (Section 3.3):
```
1. InfoSheet must be sheet index 0
2. Namespaces must be sheet index 1
3. ProcessStems must be sheet index 2
4. Processes must be sheet index 3
5. Tasks must be sheet index 4
6. RequiredInstruments must be sheet index 5
```

#### Implementation Status:

**❌ NON-COMPLIANT:**
- No validation of sheet ordering in the workbook
- Implementation uses sheet names, not indices

**Impact:** Minimal - Apache POI and the implementation access sheets by name, not by index, so ordering doesn't affect processing. However, it violates the spec and may cause issues with external validation tools.

**Recommendation:** **LOW PRIORITY**  
Consider adding a warning (not an error) if sheets are out of order, for compliance with external validators.

---

### 3. CTT Task Type Classification ❌ Non-Compliant

#### Spec Requirements (Section 1.6, Sheet 4.5):
Tasks must use **CTT task type taxonomy** via `hasco:hascoType`:
- `vstoi:UserTask` - Cognitive/motor tasks by user
- `vstoi:ApplicationTask` / `vstoi:SystemTask` - Automated system tasks  
- `vstoi:InteractionTask` - User-system collaboration
- `vstoi:AbstractTask` - High-level decomposable tasks

#### Implementation Status:

**❌ NON-COMPLIANT:**
[WKFGenerator.java](app/org/hascoapi/ingestion/WKFGenerator.java#L58-L75):
```java
} else if (elementType.equals("task")) {
    row.put("hasco:hascoType", VSTOI.TASK);  // ❌ Generic type only
}
```

All tasks are assigned the same generic `VSTOI.TASK` type, ignoring the CTT classification that should come from the Excel file.

**Impact:** **HIGH**
- Loss of semantic richness required by spec
- Cannot distinguish between user cognitive tasks, system automated tasks, and interaction tasks
- Violates CTT theoretical foundation of WKF
- Applications cannot use task type for workflow execution logic

**Recommendation:** **HIGH PRIORITY**  
The `hasco:hascoType` should be **read from the Excel file** (column C in Tasks sheet), not hardcoded:

```java
} else if (elementType.equals("task")) {
    // Read hascoType from Excel - don't override it
    // row.put("hasco:hascoType", VSTOI.TASK);  // REMOVE THIS LINE
    
    // The hascoType is already in the row from the Excel file
    // Just validate it if present
    if (row.containsKey("hasco:hascoType")) {
        String taskType = row.get("hasco:hascoType").toString();
        if (!isValidCTTTaskType(taskType)) {
            dataFile.getLogger().printWarningByIdWithArgs("WKF_00009",
                row.get("hasURI").toString(), taskType);
        }
    }
}

private static boolean isValidCTTTaskType(String taskType) {
    return taskType.equals("vstoi:UserTask") ||
           taskType.equals("vstoi:ApplicationTask") ||
           taskType.equals("vstoi:SystemTask") ||
           taskType.equals("vstoi:InteractionTask") ||
           taskType.equals("vstoi:AbstractTask") ||
           taskType.startsWith("pmsr:") ||  // Domain-specific extensions
           taskType.startsWith("vstoi:");    // Other VSTOI task types
}
```

---

### 4. Temporal Dependency Validation ❌ Critical Non-Compliance

#### Spec Requirements (Section 10):

**Temporal Operators (CTT-based):**
- `after <URI>` - Sequential execution (CTT enabling >>)
- `before <URI>` - Reverse ordering  
- `parallel <URI>` - Concurrent execution (CTT |||)
- `choice <URI1>; <URI2>` - Exclusive alternatives (CTT [])
- `independent <URI1>; <URI2>` - Any order, all must complete (CTT |=|)
- `disables <URI>` - Permanent disabling (CTT [>)
- `interrupts <URI>` - Temporary suspension (CTT |>)

**Critical Requirement:** 
> "All temporal dependencies MUST form a Directed Acyclic Graph (DAG)"

**No cycles allowed** - workflow would be unexecutable.

#### Implementation Status:

**❌ CRITICAL NON-COMPLIANCE:**
- ❌ No validation of temporal operators
- ❌ No DAG cycle detection
- ❌ No validation of temporal dependency syntax
- ❌ Dependencies are stored as raw strings without validation

**Impact:** **CRITICAL**
- Invalid workflows can be ingested
- Circular dependencies would cause infinite loops in workflow execution
- Invalid operator syntax accepted without error
- Referenced task URIs not validated

**Example of Accepted Invalid WKF:**
```
Task A: vstoi:hasTemporalDependency = "after TaskB_URI"
Task B: vstoi:hasTemporalDependency = "after TaskA_URI"
```
This circular dependency violates the DAG requirement but would be accepted.

**Recommendation:** **CRITICAL PRIORITY**  
Add validation in [AnnotateWKF.java](app/org/hascoapi/ingestion/AnnotateWKF.java):

```java
// After building the generator chain, before returning
if (chain != null && chain.isValid()) {
    // Validate temporal dependencies form a DAG
    if (!validateTemporalDAG(dataFile, mapCatalog)) {
        dataFile.getLogger().printExceptionById("WKF_00019");
        return null;
    }
}

private static boolean validateTemporalDAG(DataFile dataFile, Map<String, String> catalog) {
    // Load Tasks sheet
    String tasksSheet = catalog.get("Tasks");
    if (tasksSheet == null) return true;
    
    RecordFile tasks = new SpreadsheetRecordFile(
        dataFile.getFile(), 
        tasksSheet.replace("#", "")
    );
    
    // Build dependency graph
    Map<String, List<String>> graph = new HashMap<>();
    Map<String, String> taskURIs = new HashMap<>();
    
    for (Record rec : tasks.getRecords()) {
        String uri = rec.getValueByColumnName("hasURI");
        String tempDep = rec.getValueByColumnName("vstoi:hasTemporalDependency");
        
        if (uri != null && !uri.isEmpty()) {
            taskURIs.put(uri, uri);
            
            if (tempDep != null && tempDep.startsWith("after ")) {
                String predecessor = tempDep.substring(6).trim();
                graph.computeIfAbsent(uri, k -> new ArrayList<>()).add(predecessor);
            }
        }
    }
    
    // Detect cycles using DFS
    Set<String> visited = new HashSet<>();
    Set<String> recStack = new HashSet<>();
    
    for (String task : graph.keySet()) {
        if (hasCycle(task, graph, visited, recStack, dataFile)) {
            return false;
        }
    }
    
    return true;
}

private static boolean hasCycle(String task, Map<String, List<String>> graph,
                                Set<String> visited, Set<String> recStack,
                                DataFile dataFile) {
    if (recStack.contains(task)) {
        dataFile.getLogger().printExceptionByIdWithArgs("WKF_00018", task);
        return true;
    }
    
    if (visited.contains(task)) {
        return false;
    }
    
    visited.add(task);
    recStack.add(task);
    
    for (String neighbor : graph.getOrDefault(task, Collections.emptyList())) {
        if (hasCycle(neighbor, graph, visited, recStack, dataFile)) {
            return true;
        }
    }
    
    recStack.remove(task);
    return false;
}
```

---

### 5. Task Hierarchy Validation ❌ Non-Compliant

#### Spec Requirements (Section 5.5):
- Tasks form a **tree structure** via `vstoi:hasSupertask` and `vstoi:hasSubtask`
- **No circular references** in parent-child relationships
- Root task has no supertask and is referenced by Process's `vstoi:hasTopTask`

#### Implementation Status:

**❌ NON-COMPLIANT:**
- ❌ No validation of task hierarchy cycles
- ❌ No validation that hasSupertask and hasSubtask are consistent
- ❌ No validation that hasTopTask references a valid root task

**Impact:** **HIGH**
- Circular task hierarchies would cause infinite loops
- Inconsistent parent-child relationships lead to data quality issues

**Recommendation:** **HIGH PRIORITY**  
Add task hierarchy validation similar to temporal DAG validation.

---

### 6. Reference Integrity ❌ Non-Compliant

#### Spec Requirements:
Cross-sheet references must be valid:
- Process → ProcessStem (`prov:wasDerivedFrom`)
- Process → Task (`vstoi:hasTopTask`)
- Task → Task (`vstoi:hasSupertask`, `vstoi:hasSubtask`)
- Task → RequiredInstrument (`vstoi:hasRequiredInstrument`)
- RequiredInstrument → Instrument (`vstoi:usesInstrument`)

#### Implementation Status:

**❌ NON-COMPLIANT:**
- No validation that referenced URIs exist
- External instrument references not verified
- Broken references accepted without error

**Impact:** **HIGH**
- Broken workflows ingested
- Queries fail at runtime when following broken references

**Recommendation:** **HIGH PRIORITY**  
Add reference validation pass after ingestion.

---

### 7. Multi-Value Properties ✅ Compliant

#### Spec Requirements:
Multi-value properties use semicolon or pipe separation:
- `vstoi:hasSubtask`
- `vstoi:hasRequiredInstrument`

#### Implementation Status:

**✅ COMPLIANT:**
[WKFGenerator.java](app/org/hascoapi/ingestion/WKFGenerator.java#L100-L145):
```java
private void splitMultiValueProperty(Map<String, Object> row, String propertyKey) {
    // Handles both ; and | separators
    // URL-decodes values
    // Creates List for MetadataFactory
}
```

**Status:** ✅ Working correctly

---

### 8. URI Pattern Validation ⚠️ Not Validated

#### Spec Requirements (Section 7):
```
http://<domain>/ont/<namespace>#/<WKF_ID>/<TYPE_CODE>/<SEQ_ID>

Type Codes:
- PST  - ProcessStem
- PROC - Process
- TSK  - Task
- RIN  - RequiredInstrument
```

#### Implementation Status:

**❌ NON-COMPLIANT:**
- No validation of URI structure
- No enforcement of type codes

**Impact:** LOW  
URIs are used as-is; pattern violations don't break processing but reduce data quality.

**Recommendation:** **LOW PRIORITY**  
Add URI pattern validation as a warning (not error).

---

## Error Code Coverage

### Spec-Defined Error Codes vs Implementation

| Error Code | Spec Description | Implemented in error_dictionary.json | Used in Code |
|-----------|------------------|-------------------------------------|--------------|
| WKF_00001 | InfoSheet structure error | ✅ Yes | ⚠️ Partially |
| WKF_00002 | ProcessStem reference missing | ✅ Yes | ❌ No |
| WKF_00003 | Task hierarchy circular ref | ✅ Yes | ❌ No |
| WKF_00004 | Temporal dependency DAG violation | ✅ Yes | ❌ No |
| WKF_00005 | Missing top task reference | ✅ Yes | ⚠️ Partially |
| WKF_00006 | Invalid instrument reference | ✅ Yes | ❌ No |
| WKF_00007 | No valid sheets | ✅ Yes | ✅ Yes |
| WKF_00008 | Unknown sheet | ✅ Yes | ✅ Yes |
| WKF_00009-00020 | Additional errors | ✅ Yes | ⚠️ Partially |

**Finding:** Error codes are defined but **validation logic is missing** for most critical errors.

---

## Priority Recommendations

### CRITICAL (Must Fix)

1. **Temporal Dependency DAG Validation**
   - Implement cycle detection
   - Validate operator syntax
   - Error: WKF_00004, WKF_00019
   - **Risk:** Unexecutable workflows with circular dependencies

2. **Task Hierarchy Validation**
   - Implement circular reference detection
   - Error: WKF_00003, WKF_00018
   - **Risk:** Infinite loops in task traversal

### HIGH (Should Fix)

3. **CTT Task Type Preservation**
   - Stop overriding hasco:hascoType with generic VSTOI.TASK
   - Read and validate CTT task types from Excel
   - **Risk:** Loss of semantic classification required by spec

4. **Reference Integrity Validation**
   - Validate cross-sheet URI references
   - Errors: WKF_00002, WKF_00005, WKF_00006, WKF_00013-00017
   - **Risk:** Broken references, runtime query failures

5. **Required Property Validation**
   - Validate each entity has required properties per spec Section 12.1
   - Errors: WKF_00009, WKF_00010, WKF_00011, WKF_00012
   - **Risk:** Incomplete data, missing critical information

### MEDIUM (Nice to Have)

6. **InfoSheet Structure Validation**
   - Validate row/column counts
   - Validate hasVersion is numeric
   - Check for prohibited fields
   - Error: WKF_00001
   - **Risk:** Malformed InfoSheets accepted

7. **URI Pattern Validation**
   - Validate URI follows spec pattern
   - Validate type codes (PST, PROC, TSK, RIN)
   - Error: WKF_00003
   - **Risk:** Inconsistent URI structure

### LOW (Optional)

8. **Sheet Ordering Validation**
   - Add warning for out-of-order sheets
   - Error: WKF_00002
   - **Risk:** Minimal - implementation uses names, not indices

---

## Testing Coverage

### Existing Tests
- ✅ [WKFIngestionTest.java](test/org/hascoapi/tests/WKFIngestionTest.java) - Basic ingestion tests
- ✅ Multi-value property splitting
- ✅ Required instrument parsing
- ✅ Iteration constraint parsing

### Missing Tests (Per Spec)
- ❌ Temporal dependency DAG validation
- ❌ Task hierarchy cycle detection
- ❌ CTT task type classification
- ❌ Reference integrity checks
- ❌ InfoSheet structure validation
- ❌ URI pattern validation

---

## Conclusion

The HASCOAPI WKF ingestion implementation successfully handles the **core workflow** of processing WKF files and extracting data from the required sheets. The implementation correctly:
- ✅ Validates presence of required sheets
- ✅ Handles multi-value properties with semicolons/pipes
- ✅ Processes ProcessStems, Processes, Tasks, and RequiredInstruments
- ✅ Stores data in the knowledge graph

However, several **critical validation requirements** from WKF-SPEC-V1 are **not implemented**, particularly:
- ❌ **Temporal dependency DAG validation** (CRITICAL)
- ❌ **Task hierarchy cycle detection** (CRITICAL)
- ❌ **CTT task type preservation** (HIGH)
- ❌ **Reference integrity validation** (HIGH)
- ❌ **InfoSheet structure validation** (MEDIUM)

**Overall Assessment:** The implementation provides a **working foundation** but requires significant validation enhancements to achieve **full compliance** with WKF-SPEC-V1.

**Recommended Action:** Implement CRITICAL and HIGH priority recommendations to prevent invalid WKF files from being ingested and to preserve semantic richness required by the CTT-based specification.

---

## References

- WKF Specification v1.0: `/Users/pp3223/git/wkf/WKF-SPEC-V1.md`
- HASCOAPI Implementation: `/Users/pp3223/git/hascoapi/`
- Error Dictionary: [conf/error_dictionary.json](conf/error_dictionary.json)
- MTSheet Definition: [app/org/hascoapi/utils/MTSheet.java](app/org/hascoapi/utils/MTSheet.java)
- AnnotateWKF: [app/org/hascoapi/ingestion/AnnotateWKF.java](app/org/hascoapi/ingestion/AnnotateWKF.java)
- WKFGenerator: [app/org/hascoapi/ingestion/WKFGenerator.java](app/org/hascoapi/ingestion/WKFGenerator.java)
