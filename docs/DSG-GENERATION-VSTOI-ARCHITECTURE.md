# DSG Generation - VSTOI Architecture Implementation

**Author**: AI Assistant with Kaell  
**Date**: 2026-05-20  
**Status**: Implemented ✅

---

## Overview

This document describes the complete redesign of DSG (Data Semantics Generator) generation to align with the new VSTOI-based ingestion architecture. The previous system used a VD (Variable Design) sheet for instruments; the new system uses **SOC (Study Object Collection) worksheets** for all VSTOI entity types.

---

## What Changed

### 🔴 **REMOVED: VD Sheet**

The old architecture had:
- **InfoSheet** → References to Namespaces, STD, SSD, **VD**
- **VD sheet** → Variable design with instrument/component columns
- **Ingestion** → Used VD sheet to create instruments

### 🟢 **NEW: SOC Worksheets**

The new architecture has:
- **InfoSheet** → References to Namespaces, STD, SSD (no VD reference)
- **SSD sheet** → References SOC worksheets via `#SOC-{TYPE}-{STUDY}` format
- **SOC-* worksheets** → One per entity collection, contains originalID, rdf:type, scopeID, timeScopeID, spaceScopeID
- **Ingestion** → Uses SOC worksheets to create StudyObjects + VSTOI instances (dual-layer architecture)

---

## DSG File Structure (New Architecture)

### 1. InfoSheet

```
Attribute               | Value
------------------------|------------------
hasDependencies         | #Namespaces
hasStudyURI             | pmsr:STD-PMSR-001
hasStudyKG              | pmsr
hasStudyDescription     | #STD
hasEntityDesign         | #SSD
hasVersion              | 1
```

**Key Changes**:
- ❌ Removed: `hasVariableDesign` → `#VD`
- ✅ Simplified to 3 core sheets (Namespaces, STD, SSD)

---

### 2. Namespaces Sheet

```csv
hasPrefix,hasNameSpace,hasFormat,hasSource
pmsr,http://pmsr.net/ont/pmsr#,text/turtle,http://pmsr.net/ont/pmsr.ttl
vstoi,http://hadatac.org/ont/vstoi#,text/turtle,http://hadatac.org/ont/vstoi.ttl
hasco,http://hadatac.org/ont/hasco#,text/turtle,http://hadatac.org/ont/hasco.ttl
```

**Features**:
- Auto-pruning: Only namespaces actually used in the workbook are kept
- Dynamic population: Reads from in-memory namespace registry

---

### 3. STD Sheet (Study Description)

```csv
Study ID,Title,Specific Aims,Significance,Institution,Principal Investigator,...
pmsr:STD-PMSR-001,PMSR Simulators Study,Test medical simulators,...,...,...,...
```

**No changes** - Standard study metadata

---

### 4. SSD Sheet (Semantic Schema)

```csv
sheet,hasURI,type,hasSOCReference,comment,label,definition,groundingLabel,hasScope,hasTimeScope,hasSpaceScope,source
#SOC-INSTRUMENT-PMSR,SOC-INSTRUMENT-PMSR,hasco:StudyObjectCollection,INSTRUMENT-PMSR,Medical simulator instruments,Medical Simulators,,,,,
#SOC-COMPONENT-PMSR,SOC-COMPONENT-PMSR,hasco:StudyObjectCollection,COMPONENT-PMSR,Simulator components,Components,,,,,
#SOC-CODEBOOK-PMSR,SOC-CODEBOOK-PMSR,hasco:StudyObjectCollection,CODEBOOK-PMSR,Response scales,Codebooks,,,,,
```

**Key Changes**:
- `sheet` column now references **SOC worksheets** (not VD columns)
- Each SOC row triggers creation of a corresponding **SOC-* worksheet**
- `type` is always `hasco:StudyObjectCollection`
- `hasSOCReference` identifies the SOC type

---

### 5. VD Sheet (Variable Design) - For Future Use

```csv
sheet,hasURI,variableLabel,variableDefinition,hasUnit,hasCodebook,hasAttribute,hasAttributeOf,hasScale,isAbout
```

**Status**: 
- ✅ Sheet is created with headers in all generated DSG files
- ⚠️ **Not actively used** in VSTOI architecture (SOC worksheets are used instead)
- 🔮 Available for future extensions or custom variable design workflows
- 📝 DSGVD class exists and can populate this sheet if needed

**When to use VD sheet**:
- Custom variable design workflows outside VSTOI
- Legacy compatibility requirements
- Future integration with external data dictionary systems

---

### 6. SOC-* Worksheets (VSTOI Primary Method)

#### Example: SOC-INSTRUMENT-PMSR

```csv
originalID,rdf:type,scopeID,timeScopeID,spaceScopeID
INS1739301009974715,vstoi:Instrument,,,
INS1739287159797795,vstoi:Instrument,,,
INS1739287261878295,vstoi:Instrument,,,
```

#### Example: SOC-COMPONENT-PMSR

```csv
originalID,rdf:type,scopeID,timeScopeID,spaceScopeID
COM1738097990641815,vstoi:Component,,,
COM1738098129989165,vstoi:Component,,,
```

#### Example: SOC-CODEBOOK-PMSR

```csv
originalID,rdf:type,scopeID,timeScopeID,spaceScopeID
CBK1738096258564815,vstoi:Codebook,,,
CBK1738190767525383,vstoi:Codebook,,,
```

**SOC Worksheet Columns**:
- `originalID` - Unique identifier (used for DA-SOC enrichment)
- `rdf:type` - VSTOI ontology class (vstoi:Instrument, vstoi:Component, etc.)
- `scopeID` - References to scope objects (comma-separated originalIDs)
- `timeScopeID` - References to time scope objects
- `spaceScopeID` - References to space scope objects

---

## Dual-Layer Architecture

The new ingestion system creates **two entities** for each SOC row:

### Layer 1: Study Management (StudyObject)

```turtle
pmsr:OBJ_instrumentcollection_INS1739301009974715
  a hasco:StudyObject ;
  hasco:originalID "INS1739301009974715" ;
  hasco:isMemberOf pmsr:OCL_SOC-INSTRUMENT-PMSR ;
  rdfs:label "Instrument INS1739301009974715" ;
  rdfs:comment "" ;
  vstoi:hasInstrument pmsr:INST-INS1739301009974715 .
```

**Purpose**: Standard HADatAc study management, provenance tracking, collection membership

### Layer 2: Domain-Specific (VSTOI Instance)

```turtle
pmsr:INST-INS1739301009974715
  a vstoi:Instrument ;
  rdfs:label "ARTEC LEO Scanner" ;
  vstoi:hasShortName "LaerdalNursingAnne" ;
  vstoi:hasLanguage "en" ;
  vstoi:hasVersion "2.0" .
```

**Purpose**: Domain-specific properties, relationships, and semantics

### Linking Between Layers

The StudyObject links to the VSTOI instance via:
- `vstoi:hasInstrument` (for Instruments)
- `vstoi:hasComponent` (for Components)
- `vstoi:hasCodebook` (for Codebooks)
- `vstoi:hasComponentStem` (for ComponentStems)
- `vstoi:hasContainerSlot` (for ContainerSlots)
- `vstoi:hasResponseOption` (for ResponseOptions)
- `vstoi:hasAnnotationStem` (for AnnotationStems)

---

## DSG Generation Process

### 1. Query Studies

```java
// By status
List<Study> studies = GenericFind.findByQuery(Study.class, sparqlQuery);

// By manager
List<Study> studies = studyQuery.findByStatusManagerEmailWithPages(
    Study.class, status, useremail, withCurrent, PAGESIZE, OFFSET);
```

### 2. Create Workbook Structure

```java
Workbook workbook = DSGGen.create(filename, studies);
```

Creates:
- InfoSheet (with 3 main sheet references)
- Namespaces (populated from namespace registry)
- SSD (empty with headers)
- STD (empty with headers)

### 3. Populate STD Sheet

```java
DSGSTD.add(helper, study);
```

Adds one row per study with metadata (title, PI, institution, etc.)

### 4. Populate SSD Sheet + SOC Worksheets

```java
DSGSSD.addByStudy(helper, study);
```

For each Study:
1. Query all StudyObjectCollections for the study
2. For each SOC:
   - Add one row to SSD sheet
   - Create SOC-* worksheet (if not exists)
   - Query all StudyObjects in the SOC
   - For each StudyObject:
     - Add one row to SOC-* worksheet with originalID, rdf:type, scopes

**Key Logic**:

```java
// Derive SOC worksheet name from SOC label
String sheetName = "SOC-" + normalizeForSheet(soc.getLabel());

// Create worksheet with standard columns
Row headerRow = socSheet.createRow(0);
headerRow.createCell(0).setCellValue("originalID");
headerRow.createCell(1).setCellValue("rdf:type");
headerRow.createCell(2).setCellValue("scopeID");
headerRow.createCell(3).setCellValue("timeScopeID");
headerRow.createCell(4).setCellValue("spaceScopeID");

// Populate rows from StudyObjects
List<StudyObject> objects = soc.getObjects();
for (StudyObject obj : objects) {
    Row row = socSheet.createRow(rowNum++);
    row.createCell(0).setCellValue(obj.getOriginalId());
    row.createCell(1).setCellValue(URIUtils.replaceNameSpaceEx(obj.getTypeUri()));
    row.createCell(2).setCellValue(joinOriginalIds(obj.getScopeUris()));
    row.createCell(3).setCellValue(joinOriginalIds(obj.getTimeScopeUris()));
    row.createCell(4).setCellValue(joinOriginalIds(obj.getSpaceScopeUris()));
}
```

### 5. Prune Unused Namespaces

```java
pruneUnusedNamespaces(workbook);
```

Scans all worksheets and removes namespaces that aren't referenced anywhere

### 6. Save Workbook

```java
String result = DSGGen.save(helper, filename);
```

Writes the .xlsx file to the ingestion directory

---

## DA-SOC Generation (Optional)

DSG generation can optionally generate **DA-SOC CSV files** for enrichment properties.

### When to Generate DA-SOCs

When frontend requests DA-SOC generation via `generateDASOCs=true` parameter:

```java
genByManager(useremail, status, filename, mediaFolder, verifyUri, true);
```

### DA-SOC Generation Process

For each Study:
1. Query all StudyObjectCollections for the study
2. For each SOC:
   - Query all StudyObjects in the SOC
   - Resolve VSTOI instance URIs (follow `vstoi:has{Type}` links)
   - Query all properties on VSTOI instances (excluding base properties)
   - Generate `DA-SOC-{SOCNAME}.csv` file

### DA-SOC File Structure

```csv
originalID,vstoi:hasLanguage,vstoi:hasVersion,vstoi:hasShortName,rdfs:subClassOf
INS1739301009974715,en,2.0,LaerdalNursingAnne,pmsr:INST-INS1739287159797795
INS1739287159797795,en,1.0,ArrowheadScanner,
```

**Key Features**:
- First column is always `originalID`
- Remaining columns are enrichment properties (not in base DSG)
- Values are automatically converted to CURIEs for readability
- Empty cells are left blank (not populated with null/empty literals)

### Base Properties Excluded from DA-SOCs

These properties are part of the DSG structure and are NOT included in DA-SOC files:
- `rdf:type`
- `rdfs:label`
- `rdfs:comment`
- `hasco:originalID`
- `hasco:isMemberOf`
- `hasco:hasTimestamp`
- `vstoi:hasSIRManagerEmail`
- `hasco:hascoType`

---

## VSTOI Entity Types Supported

All seven VSTOI entity types are fully supported:

| Entity Type | Ontology Class | URI Prefix | SOC Worksheet Pattern |
|-------------|----------------|------------|----------------------|
| **Instrument** | `vstoi:Instrument` | `INST-` | `SOC-INSTRUMENT-{STUDY}` |
| **Component** | `vstoi:Component` | `COMP-` | `SOC-COMPONENT-{STUDY}` |
| **ComponentStem** | `vstoi:ComponentStem` | `CSTEM-` | `SOC-COMPONENT-STEM-{STUDY}` |
| **ContainerSlot** | `vstoi:ContainerSlot` | `CTSLOT-` | `SOC-CONTAINER-SLOT-{STUDY}` |
| **Codebook** | `vstoi:Codebook` | `CB-` | `SOC-CODEBOOK-{STUDY}` |
| **ResponseOption** | `vstoi:ResponseOption` | `ROPT-` | `SOC-RESPONSE-OPTION-{STUDY}` |
| **AnnotationStem** | `vstoi:AnnotationStem` | `ASTEM-` | `SOC-ANNOTATION-STEM-{STUDY}` |

---

## Workflow Diagram

```
┌────────────────────────────────────────────────────────┐
│              DSG GENERATION WORKFLOW                   │
└────────────────────────────────────────────────────────┘

1. Query Studies
   └─→ By status / By manager / By URI
            │
            ↓
2. Create Workbook Structure
   └─→ InfoSheet + Namespaces + SSD + STD
            │
            ↓
3. Populate STD Sheet
   └─→ One row per study (metadata)
            │
            ↓
4. Populate SSD Sheet + SOC Worksheets
   └─→ For each Study:
         ├─→ Query all SOCs for study
         ├─→ For each SOC:
         │    ├─→ Add row to SSD
         │    ├─→ Create SOC-* worksheet
         │    └─→ Query StudyObjects in SOC
         │         └─→ Add row per object (originalID, type, scopes)
         └─→ Result: SSD references SOC worksheets
            │
            ↓
5. Prune Unused Namespaces
   └─→ Keep only namespaces referenced in workbook
            │
            ↓
6. Save DSG File
   └─→ Write .xlsx to ingestion directory
            │
            ↓
7. Optional: Generate DA-SOCs
   └─→ For each SOC:
         ├─→ Query VSTOI instances
         ├─→ Extract enrichment properties
         └─→ Write DA-SOC-{SOCNAME}.csv
```

---

## Code Changes Summary

### DSGGen.java

**Modified Methods**:
- `create()` - Removed VD sheet creation, simplified InfoSheet references
- `genByStatus()` - Updated workflow
- `genByStudy()` - Updated workflow
- `genByManager()` - Added optional DA-SOC generation
- `generateDASOCsForStudies()` - NEW - Orchestrates DA-SOC generation
- `generateDASOCFile()` - NEW - Generates individual DA-SOC CSV files
- `extractSOCNameFromURI()` - NEW - Extracts SOC name from URI

**Constants**:
- ❌ Removed: `VD` constant
- ✅ Kept: `INFOSHEET`, `NAMESPACES`, `SSD`, `STD`

### DSGSSD.java

**No changes required** - Already implements SOC worksheet generation correctly

### DSGSTD.java

**No changes required** - Already implements STD sheet correctly

---

## Testing Checklist

### ✅ DSG Generation Tests

- [ ] Generate DSG by status → Verify InfoSheet has 3 sheet references (not 4)
- [ ] Generate DSG by manager → Verify SOC worksheets created for all SOC types
- [ ] Generate DSG by study → Verify SSD references match SOC worksheet names
- [ ] Verify namespace pruning → Only used namespaces in final file
- [ ] Verify SOC worksheets have correct columns (originalID, rdf:type, scopeID, timeScopeID, spaceScopeID)

### ✅ DA-SOC Generation Tests

- [ ] Generate DA-SOCs with `generateDASOCs=true` → Verify CSV files created
- [ ] Verify DA-SOC files have `originalID` as first column
- [ ] Verify enrichment properties exclude base properties (rdf:type, rdfs:label, etc.)
- [ ] Verify URI values converted to CURIEs
- [ ] Verify empty cells left blank (not "null" or empty strings)

### ✅ Ingestion Tests

- [ ] Ingest generated DSG → Verify StudyObjects created
- [ ] Ingest generated DSG → Verify VSTOI instances created and linked
- [ ] Ingest generated DA-SOCs → Verify enrichment properties added
- [ ] Verify UI displays VSTOI entities correctly
- [ ] Verify scopes mapped correctly (scopeID → StudyObject originalIDs)

---

## Migration Guide

### For Users

**Old Workflow** (with VD sheet):
1. Generate DSG with VD sheet
2. Edit VD sheet columns manually
3. Ingest DSG

**New Workflow** (with SOC worksheets):
1. Generate DSG with SOC worksheets (auto-populated)
2. Optionally generate DA-SOCs for enrichment
3. Ingest DSG (creates base entities)
4. Ingest DA-SOCs (adds enrichment properties)

### For Developers

**Key Changes**:
- Remove all references to `DSGGen.VD` constant
- SOC worksheets are created dynamically by `DSGSSD.addByStudy()`
- DA-SOC generation is optional, controlled by `generateDASOCs` parameter
- VSTOI instance URIs are resolved via linking properties (`vstoi:has{Type}`)

---

## Future Enhancements

- [ ] Add DA-SOC validation (check dependencies before generation)
- [ ] Add DA-SOC merge utility (combine multiple DA-SOCs into one)
- [ ] Add SOC worksheet templates for empty studies
- [ ] Add bulk DA-SOC generation for multiple studies
- [ ] Add DA-SOC diff utility (compare before/after enrichment)

---

## References

- [VSTOI-DSG-IMPLEMENTATION-COMPLETE.md](VSTOI-DSG-IMPLEMENTATION-COMPLETE.md) - Complete VSTOI architecture documentation
- [DASOC-SPECIFICATION-v1.1.md](DASOC-SPECIFICATION-v1.1.md) - DA-SOC file specification
- [DA-INGESTION-ORDER.md](DA-INGESTION-ORDER.md) - Correct order for DA-SOC ingestion
- [FIX-SOC-LABEL-COMMENT-SCOPE.md](FIX-SOC-LABEL-COMMENT-SCOPE.md) - SOC property fixes

---

## Conclusion

The DSG generation system has been completely redesigned to align with the new VSTOI-based ingestion architecture. The key changes are:

1. **Removed VD sheet** - No longer used for instruments
2. **Added SOC worksheets** - One per entity collection, dynamically created
3. **Dual-layer architecture** - StudyObject + VSTOI instance for each entity
4. **Optional DA-SOC generation** - Extract enrichment properties for existing entities
5. **Namespace pruning** - Only keep namespaces actually used in workbook

This architecture provides a clean separation between study management (Layer 1) and domain-specific entities (Layer 2), enabling flexible enrichment via DA-SOC files without re-ingesting the base DSG.
