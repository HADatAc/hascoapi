# WKF Specification Compliance Updates

**Date**: 2025
**Branch**: DEV_V0.9.5
**Specification**: WKF-SPEC-V1.md

## Overview

This document summarizes the code changes made to bring HASCOAPI's WKF implementation into full compliance with the WKF Specification v1.0. The WKF specification defines workflow metadata templates based on ConcurTaskTree (CTT) hierarchical task modeling notation.

## Issues Identified

### 1. Missing CTT Task Properties
**Issue**: The Tasks sheet was missing the `vstoi:hasIterationConstraint` column required by the WKF spec (section 4.5, column S).

**Impact**: CTT iteration operators (e.g., "at least 3 times", "until condition") could not be captured for Tasks.

### 2. Non-Compliant RequiredInstruments Sheet
**Issue**: The RequiredInstruments sheet had non-standard column `vstoi:hasRequiredComponent` instead of the spec-required columns:
- `vstoi:isRelatedToTask` (column G) - reference to associated Task
- `vstoi:hasInstrumentConfig` (column H) - configuration parameters

**Impact**: Instrument-to-task relationships and instrument configurations could not be properly stored or retrieved.

## Changes Made

### 1. Task Entity (Task.java)

#### Added Properties
```java
@PropertyField(uri="vstoi:hasIterationConstraint")
private String hasIterationConstraint;
```

#### Added Methods
- `getHasIterationConstraint()`: Returns the CTT iteration constraint string
- `setHasIterationConstraint(String)`: Sets the iteration constraint

#### Updated SPARQL Parsing
Modified `find(String uri)` method to parse `vstoi:hasIterationConstraint` from RDF triples.

### 2. RequiredInstrument Entity (RequiredInstrument.java)

#### Added Properties
```java
@PropertyField(uri="vstoi:isRelatedToTask")
protected String isRelatedToTask;

@PropertyField(uri="vstoi:hasInstrumentConfig")
protected String hasInstrumentConfig;
```

**Note**: Kept `vstoi:hasRequiredComponent` for backward compatibility with existing data, but it's not part of the WKF spec.

#### Added Methods
- `getIsRelatedToTask()`: Returns URI of related Task
- `setIsRelatedToTask(String)`: Sets related Task URI
- `getHasInstrumentConfig()`: Returns instrument configuration string (JSON/key-value)
- `setHasInstrumentConfig(String)`: Sets instrument configuration

#### Updated SPARQL Parsing
Modified `find(String uri)` method to parse `vstoi:isRelatedToTask` and `vstoi:hasInstrumentConfig` from RDF triples.

### 3. VSTOI Vocabulary (VSTOI.java)

#### Added Constants
```java
public static final String HAS_ITERATION_CONSTRAINT = VSTOI + "hasIterationConstraint";
public static final String HAS_INSTRUMENT_CONFIG    = VSTOI + "hasInstrumentConfig";
public static final String IS_RELATED_TO_TASK       = VSTOI + "isRelatedToTask";
```

### 4. WKF Workbook Generation (WKFGen.java)

#### Updated Tasks Sheet Headers
Added column 18: `vstoi:hasIterationConstraint`

**Complete Tasks Sheet Header (19 columns)**:
1. hasURI
2. rdf:type
3. hasco:hascoType
4. rdfs:label
5. rdfs:comment
6. vstoi:hasStatus
7. vstoi:hasLanguage
8. vstoi:hasVersion
9. prov:wasDerivedFrom
10. vstoi:hasReviewNote
11. vstoi:hasSIRManagerEmail
12. vstoi:hasEditorEmail
13. vstoi:hasSupertask
14. vstoi:hasSubtask
15. vstoi:hasTemporalDependency
16. vstoi:hasRequiredInstrument
17. hasco:hasImage
18. hasco:hasWebDocument
19. **vstoi:hasIterationConstraint** *(NEW)*

#### Updated RequiredInstruments Sheet Headers
Replaced `vstoi:hasRequiredComponent` with spec-compliant columns.

**Complete RequiredInstruments Sheet Header (10 columns)**:
1. hasURI
2. rdf:type
3. hasco:hascoType
4. rdfs:label
5. rdfs:comment
6. vstoi:usesInstrument
7. **vstoi:isRelatedToTask** *(CHANGED from vstoi:hasRequiredComponent)*
8. **vstoi:hasInstrumentConfig** *(NEW)*
9. hasco:hasImage
10. hasco:hasWebDocument

### 5. Tasks Sheet Row Population (WKFTasks.java)

#### Updated Row Creation
Both `addByWkf()` and `addTask()` methods now populate column 18 with iteration constraint:
```java
row.createCell(18).setCellValue(safe(t.getHasIterationConstraint()));
```

### 6. RequiredInstruments Sheet Row Population (WKFRequiredInstruments.java)

#### Updated Row Creation
Both `addByWkf()` and `addRequiredInstrument()` methods now populate:
- Column 6: `vstoi:isRelatedToTask` (replaces hasRequiredComponent)
- Column 7: `vstoi:hasInstrumentConfig` (new)

```java
row.createCell(6).setCellValue(URIUtils.replaceNameSpaceEx(safe(ri.getIsRelatedToTask())));
row.createCell(7).setCellValue(safe(ri.getHasInstrumentConfig()));
```

#### Removed Code
Deleted unused `joinUriList()` method (no longer needed for hasRequiredComponent).

### 7. WKF Ingestion (WKFGenerator.java)

**No changes required**. The WKFGenerator already handles all properties generically by copying Excel column headers to the row map. New single-value properties are automatically ingested without special handling.

## Compliance Status

### ✅ Fully Compliant Sections

1. **InfoSheet Structure** (Section 3.2)
   - 7 rows (1 header + 6 data rows)
   - Correct field order: hasDependencies, ProcessStems, Processes, Tasks, RequiredInstruments, hasVersion

2. **Sheet Ordering** (Section 3.3)
   - Index 0: InfoSheet
   - Index 1: Namespaces
   - Index 2: ProcessStems
   - Index 3: Processes
   - Index 4: Tasks
   - Index 5: RequiredInstruments

3. **ProcessStems Sheet** (Section 4.3)
   - All 16 columns match spec

4. **Processes Sheet** (Section 4.4)
   - All 15 columns match spec

5. **Tasks Sheet** (Section 4.5)
   - All 19 columns match spec (including new vstoi:hasIterationConstraint)

6. **RequiredInstruments Sheet** (Section 4.6)
   - All 10 columns match spec (fixed vstoi:isRelatedToTask and vstoi:hasInstrumentConfig)

### ⚠️ Notes on Backward Compatibility

**RequiredInstrument POJO** retains the `vstoi:hasRequiredComponent` property for backward compatibility with existing data, even though it's not in the WKF spec. This property:
- Is still parsed from RDF during `find()`
- Is NOT written to generated Excel files (spec-compliant)
- Can be used by legacy code if needed

## Testing Recommendations

### 1. Unit Tests
- Test Task entity with hasIterationConstraint property
- Test RequiredInstrument entity with isRelatedToTask and hasInstrumentConfig properties
- Verify SPARQL parsing for new properties

### 2. Integration Tests
- Generate WKF Excel file and verify column headers match spec
- Ingest WKF Excel file with new columns and verify data persists
- Round-trip test: generate → ingest → generate → compare

### 3. Backward Compatibility Tests
- Verify existing WKF data (without new fields) still loads correctly
- Ensure null/empty values for new fields don't break existing functionality

## Files Modified

1. `/Users/pp3223/git/hascoapi/app/org/hascoapi/entity/pojo/Task.java`
2. `/Users/pp3223/git/hascoapi/app/org/hascoapi/entity/pojo/RequiredInstrument.java`
3. `/Users/pp3223/git/hascoapi/app/org/hascoapi/vocabularies/VSTOI.java`
4. `/Users/pp3223/git/hascoapi/app/org/hascoapi/transform/mt/wkf/WKFGen.java`
5. `/Users/pp3223/git/hascoapi/app/org/hascoapi/transform/mt/wkf/WKFTasks.java`
6. `/Users/pp3223/git/hascoapi/app/org/hascoapi/transform/mt/wkf/WKFRequiredInstruments.java`

## Next Steps

1. **Build and Deploy**: Compile changes and deploy to test environment
2. **Generate Sample WKF**: Create a test WKF file to verify all columns appear correctly
3. **Ingest Sample WKF**: Upload the test file to verify new properties are stored in RDF
4. **Query Verification**: Run SPARQL queries to confirm new triples are created
5. **Update Documentation**: Ensure API documentation reflects new properties
6. **Update UI Forms**: If applicable, add form fields for new properties in workflow editor

## References

- **WKF Specification**: `/Users/pp3223/git/wkf/WKF-SPEC-V1.md`
- **CTT Notation**: ConcurTaskTree hierarchical task modeling
- **VSTOI Ontology**: `http://hadatac.org/ont/vstoi#`
- **HASCO Ontology**: Human-Aware Science Ontology
- **PROV Ontology**: W3C Provenance Ontology
