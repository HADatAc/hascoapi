# VSTOI-Based DSG Implementation - Complete Technical Documentation

**Author**: Kaell (with AI assistance)  
**Date**: April 2026  
**Branch**: INStoDSG  
**Status**: Production Ready ✅

---

## Executive Summary

This document describes the complete implementation of the **VSTOI-based DSG (Data Semantics Generator) architecture** that replaces the legacy INS (Instrument) file system in HADatAc. The new system provides a unified, scalable approach to managing all seven VSTOI entity types through DSG files and their enrichment via DA-SOC (Data Acquisition - Study Object Collection) files.

### Key Achievements

- ✅ **Eliminated INS file dependency** - All VSTOI entities now managed through DSG
- ✅ **Unified ingestion pipeline** - Single workflow for all 7 VSTOI entity types
- ✅ **Dual-layer architecture** - Clean separation between study management (StudyObject) and domain entities (VSTOI)
- ✅ **Flexible enrichment** - Properties can be added/modified via DA-SOC files without re-ingesting the base DSG
- ✅ **Production validated** - System operational with 200+ VSTOI instances across multiple types

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [VSTOI Entity Types](#vstoi-entity-types)
3. [DSG File Structure](#dsg-file-structure)
4. [DA-SOC Enrichment](#da-soc-enrichment)
   - 4.1 [DA-SOC Ingestion Order](#da-soc-ingestion-order)
   - 4.2 [URI Transformation and Prefix Mapping](#uri-transformation-and-prefix-mapping)
5. [Implementation Details](#implementation-details)
6. [Critical Fixes Applied](#critical-fixes-applied)
7. [Testing and Validation](#testing-and-validation)
8. [Troubleshooting Guide](#troubleshooting-guide)
9. [Future Enhancements](#future-enhancements)

---

## Architecture Overview

### Dual-Layer System

The implementation uses a **two-layer architecture** that separates concerns between study management and domain-specific entities:

#### Layer 1: Study/Knowledge Graph Layer
- **Entity**: `hasco:StudyObject`
- **URI Pattern**: `pmsr:OBJ_{type}collection_{originalID}`
- **Purpose**: Standard HADatAc study management, provenance tracking, collection membership
- **Properties**: originalID, rdf:type, label, comment, isMemberOf (SOC reference)

#### Layer 2: VSTOI Application Layer
- **Entities**: `vstoi:Instrument`, `vstoi:Component`, `vstoi:Codebook`, etc.
- **URI Pattern**: `pmsr:{prefix}-{originalID}` (e.g., `pmsr:INST-INS001`, `pmsr:CB-CBK001`)
- **Purpose**: Domain-specific properties, relationships, and semantics
- **Properties**: Type-specific properties (hasLanguage, hasVersion, hasComponentStem, etc.)

#### Linking Between Layers

```turtle
# Layer 1: StudyObject
pmsr:OBJ_instrumentcollection_INS1739301009974715
  a hasco:StudyObject ;
  hasco:originalID "INS1739301009974715" ;
  hasco:isMemberOf pmsr:OCL_SOC-INSTRUMENT-PMSR ;
  vstoi:hasInstrument pmsr:INST-INS1739301009974715 .

# Layer 2: VSTOI Instance
pmsr:INST-INS1739301009974715
  a vstoi:Instrument ;
  rdfs:label "ARTEC LEO Scanner" ;
  vstoi:hasShortName "LaerdalNursingAnne" ;
  vstoi:hasLanguage "en" ;
  vstoi:hasVersion "2.0" .
```

### Benefits of This Architecture

1. **Separation of Concerns**: Study management logic doesn't pollute domain models
2. **Flexible Querying**: Can query by study context OR by domain properties
3. **Independent Evolution**: VSTOI ontology can evolve without breaking study structures
4. **Backward Compatibility**: Existing StudyObject queries continue to work
5. **Enrichment Without Re-ingestion**: DA-SOC files can add properties to VSTOI instances without touching the base DSG

---

## VSTOI Entity Types

All seven VSTOI entity types are fully supported:

| Entity Type | Ontology Class | URI Prefix | Production Count | Use Case |
|-------------|----------------|------------|------------------|----------|
| **Instrument** | `vstoi:Instrument` | `INST-` | 71+ | Physical instruments, questionnaires, simulation models |
| **Component** | `vstoi:Component` | `COMP-` | 16+ | Instrument sub-parts, sensors, detectors |
| **ComponentStem** | `vstoi:ComponentStem` | `CSTEM-` | 83+ | Reusable component templates |
| **ContainerSlot** | `vstoi:ContainerSlot` | `CSLOT-` | 12+ | Ordered slots in instruments |
| **Codebook** | `vstoi:Codebook` | `CB-` | 12+ | Controlled vocabularies, value sets |
| **ResponseOption** | `vstoi:ResponseOption` | `ROPT-` | - | Individual options within codebooks |
| **AnnotationStem** | `vstoi:AnnotationStem` | `ASTEM-` | - | Reusable annotation templates |

### Entity Relationships

```
Instrument
├── hasFirst → ContainerSlot (head of ordered list)
├── hasComponent → Component (direct child)
└── rdfs:subClassOf → Instrument (inheritance)

ContainerSlot
├── hasComponent → Component
├── hasNext → ContainerSlot
├── hasPrevious → ContainerSlot
├── hasPriority → Integer
└── belongsTo → Instrument

Component
├── hasComponentStem → ComponentStem
├── hasCodebook → Codebook
└── hasDetector → Detector (subclass of Component)

Codebook
└── hasResponseOption → ResponseOption

ComponentStem
└── hasCodebook → Codebook
```

---

## DSG File Structure

### File Format

DSG files are Excel workbooks (.xlsx) containing multiple worksheets that define:
- Study metadata (STD sheet)
- Semantic data dictionaries (SSD sheet)
- Study object collections (SOC-* sheets)

### SOC Worksheet Structure for VSTOI Entities

Each SOC worksheet represents a collection of entities of the same type. The worksheet name pattern is:

```
SOC-{ENTITY-TYPE}-{STUDY-NAME}
```

**Examples**:
- `SOC-INSTRUMENT-PMSR`
- `SOC-COMPONENT-STEM-PMSR`
- `SOC-CODEBOOK-PMSR`

### Required Columns

| Column Name | Description | Example Value |
|-------------|-------------|---------------|
| `originalID` | Unique identifier within the study | `INS1739301009974715` |
| `rdf:type` | VSTOI ontology class | `vstoi:Instrument` |
| `label` | Human-readable name | `"ARTEC LEO Scanner"` |
| `comment` | Optional description | `"3D scanning device"` |
| `scopeID` | Domain scope (optional) | - |
| `timeScopeID` | Time scope (optional) | - |
| `spaceScopeID` | Space scope (optional) | - |

### Example DSG SOC Worksheet

```csv
originalID,rdf:type,scopeID,timeScopeID,spaceScopeID,label,comment
INS1739301009974715,vstoi:Instrument,,,,"ARTEC LEO Scanner","3D scanning device"
COMP1739302145123456,vstoi:Component,,,,"Chest Inflator","Pneumatic chest simulator"
CBK1738096258564815,vstoi:Codebook,,,,"Response Scale","1-5 Likert scale"
```

### Ingestion Process

1. **Parse DSG file**: Read STD, SSD, and all SOC-* worksheets
2. **Create study structure**: Generate Study, StudyObjectCollections
3. **Process each SOC row**:
   - Create `StudyObject` with base properties
   - Detect VSTOI type from `rdf:type` column
   - Auto-generate VSTOI instance (Instrument, Component, etc.)
   - Link StudyObject to VSTOI instance via `vstoi:has{Type}` property
4. **Commit to triplestore**: Save both layers in named graph

**Key Innovation**: The system automatically creates **two URIs per entity**:
- One for the StudyObject (Layer 1)
- One for the VSTOI instance (Layer 2)

This happens transparently during DSG ingestion without user intervention.

---

## DA-SOC Enrichment

### Purpose

DA-SOC files allow you to **add or modify properties** on existing VSTOI instances without re-ingesting the base DSG. This is crucial for:
- Adding extended metadata after initial creation
- Establishing relationships between entities
- Updating versioning/status information
- Linking components to stems and codebooks

⚠️ **IMPORTANT**: DA-SOC files **must be ingested in the correct order** due to dependencies between entity types. See [DA-INGESTION-ORDER.md](DA-INGESTION-ORDER.md) for detailed documentation on the required ingestion sequence and dependency graph.

### Quick Order Reference

1. **Independent entities** (no dependencies):
   - DA-SOC-CODEBOOK
   - DA-SOC-RESPONSE-OPTION
   - DA-SOC-COMPONENTSTEM

2. **DA-SOC-COMPONENT** (depends on Codebooks + ComponentStems)

3. **DA-SOC-SLOTELEMENT** (depends on Components)

4. **DA-SOC-INSTRUMENT** (depends on SlotElements)


### File Naming Convention

```
DA-SOC-{ENTITY-TYPE}-{STUDY-NAME}_{VERSION}.csv
```

**Examples**:
- `DA-SOC-INSTRUMENT-PMSR_1.csv`
- `DA-SOC-CODEBOOK_2.csv`
- `DA-SOC-COMPONENT-STEM-PMSR_1.csv`

### CSV Structure

DA-SOC files are CSV files where:
- **First column**: `originalID` (matches the ID from DSG)
- **Remaining columns**: Property URIs (full URIs or prefixed)

**Example**:
```csv
originalID,vstoi:hasLanguage,vstoi:hasVersion,vstoi:hasStatus,rdfs:subClassOf
INS1739301009974715,en,2.0,PUBLISHED,pmsr:/INS1739287159797795
```

### CSV Best Practices

**⚠️ CRITICAL**: Proper CSV formatting is essential to avoid ingestion errors and UI warnings.

#### Value Type Guidelines

DA-SOC CSV values fall into two categories:

1. **Literal Values** (plain strings/numbers):
   - `hasLanguage`, `hasVersion`, `hasStatus`, `hasShortName`, `hasContent`, `hasSerialNumber`, `hasPriority`
   - Format: Plain value without namespace prefix
   - ✅ Correct: `en`, `2.0`, `PUBLISHED`, `1`
   - ❌ Incorrect: `pmsr:/en`, `pmsr:/2.0`, `pmsr:/PUBLISHED`

2. **URI References** (links to other entities):
   - `hasCodebook`, `hasComponentStem`, `hasComponent`, `belongsTo`, `hasFirst`, `hasNext`, `hasPrevious`, `rdfs:subClassOf`, `hasMaker`, `isAttributeOf`
   - Format: Namespace prefix + originalID
   - ✅ Correct: `pmsr:/CBK001`, `pmsr:/COM002`, `uberon:0001004`
   - ❌ Incorrect: `CBK001` (missing namespace), `https://pmsr.net/ont/CBK001` (full URI not needed)

#### Example DA-SOC-COMPONENT.csv (Correct Format)

```csv
originalID,rdf:type,vstoi:hasComponentStem,vstoi:hasCodebook,vstoi:isAttributeOf,hasco:hasWebDocument
COM1738097990641815,pmsr:/CSM1738097871592315,pmsr:/CSM1738097871592315,pmsr:/CBK1738096258564815,uberon:0001004,
COM1738098129989165,pmsr:/CSM1738098066919465,pmsr:/CSM1738098066919465,pmsr:/CBK1738096048734595,uberon:0001007,
```

#### Example DA-SOC-CODEBOOK.csv (Correct Format)

```csv
originalID,rdf:type,vstoi:hasLanguage,vstoi:hasVersion
CBK1738096258564815,vstoi:Codebook,en,1
CBK1738190767525383,vstoi:Codebook,en,1
```
**Note**: `hasLanguage` and `hasVersion` are literal values (no `pmsr:/` prefix).

#### Common Formatting Mistakes

| Mistake | Problem | Correct Format |
|---------|---------|----------------|
| `pmsr:/en` for language | Treated as URI, causes warnings | `en` |
| `CBK001` for codebook | No namespace, fails to resolve | `pmsr:/CBK001` |
| `http://full-uri.com/CBK001` | Full URI, parsing issues | `pmsr:/CBK001` |
| Extra spaces in headers | Column not recognized | Remove spaces |
| BOM character at start | First column not found | Save as UTF-8 without BOM |

### Supported Properties by Entity Type

#### Instrument
- `vstoi:hasShortName` - Short identifier
- `vstoi:hasLanguage` - Language code (en, pt, etc.)
- `vstoi:hasVersion` - Version number
- `vstoi:hasMaker` - Manufacturer URI
- `hasco:hasWebDocument` - Documentation URL
- `vstoi:hasFirst` - First ContainerSlot URI
- `hasco:hasImage` - Image URL
- `rdfs:subClassOf` - Parent instrument URI

#### Component
- `vstoi:hasComponentStem` - ComponentStem URI
- `vstoi:hasCodebook` - Codebook URI
- `vstoi:isAttributeOf` - External ontology reference (e.g., UBERON anatomy terms)
- `vstoi:hasLanguage`
- `vstoi:hasVersion`
- `hasco:hasWebDocument`

#### ComponentStem
- `vstoi:hasContent` - Template content
- `vstoi:hasLanguage`
- `vstoi:hasVersion`
- `vstoi:hasStatus`
- `rdfs:subClassOf` - Parent ComponentStem URI (for inheritance hierarchies)
- `hasco:hasMaker` - Manufacturer/creator URI
- `hasco:hasImage` - Image URL

#### ContainerSlot
- `vstoi:belongsTo` - Parent instrument URI
- `vstoi:hasComponent` - Child component URI
- `vstoi:hasNext` - Next slot URI
- `vstoi:hasPrevious` - Previous slot URI
- `vstoi:hasPriority` - Integer order

#### Codebook
- `vstoi:hasLanguage`
- `vstoi:hasVersion`
- `vstoi:hasStatus`
- `vstoi:hasSerialNumber`
- `vstoi:hasReviewNote`

#### ResponseOption
- `vstoi:hasContent` - Option text
- `vstoi:hasLanguage`
- `vstoi:hasStatus`
- `rdfs:label`

#### AnnotationStem
- `vstoi:hasContent` - Annotation template
- `vstoi:hasLanguage`
- `vstoi:hasVersion`
- `vstoi:hasStatus`

### Enrichment Process

1. **Upload DA-SOC file** via UI
2. **System discovers study context**: Queries triplestore to find which study the originalIDs belong to
3. **Resolve VSTOI instance URIs**: For each originalID, lookup the corresponding VSTOI instance URI (not the StudyObject URI)
4. **Add triples to graph**: Create RDF triples with VSTOI instance as subject
5. **Enrich POJO objects**: Update in-memory POJO objects with new properties and save back to triplestore

**Critical Fix**: The system was initially attempting to enrich StudyObject URIs instead of VSTOI instance URIs. This was fixed by adding a resolution step using `getVSTOIInstanceUri()` that follows the linking property (`vstoi:hasInstrument`, etc.) to find the correct target.

### DA-SOC Ingestion Order

**⚠️ IMPORTANT**: DA-SOC files **must be ingested in the correct order** to ensure all dependencies are resolved properly. Ingesting files out of order will result in broken references and missing relationships in the UI.

#### Recommended Ingestion Order

1. **Codebook** (`DA-SOC-CODEBOOK*.csv`) - **First Priority**
   - No dependencies on other VSTOI entities
   - Contains controlled vocabularies referenced by other entities
   - Example: Response options value sets, unit definitions

2. **ResponseOption** (`DA-SOC-RESPONSE-OPTION*.csv`)
   - May reference Codebooks (optional)
   - Should be ingested after Codebooks if relationships exist

3. **ComponentStem** (`DA-SOC-COMPONENT-STEM*.csv`)
   - No dependencies on Components or Instruments
   - Templates referenced by Components
   - May have parent-child relationships (via `rdfs:subClassOf`)

4. **Component** (`DA-SOC-COMPONENT*.csv`)
   - **Depends on**: ComponentStems (via `vstoi:hasComponentStem`)
   - **Depends on**: Codebooks (via `vstoi:hasCodebook`)
   - Must be ingested **after** ComponentStems and Codebooks

5. **ContainerSlot** (`DA-SOC-CONTAINER-SLOT*.csv` or `DA-SOC-SLOT-ELEMENT*.csv`)
   - **Depends on**: Components (via `vstoi:hasComponent`)
   - **Depends on**: Instruments (via `vstoi:belongsTo`)
   - May self-reference (via `vstoi:hasNext`, `vstoi:hasPrevious`)
   - Should be ingested **after** Components

6. **Instrument** (`DA-SOC-INSTRUMENT*.csv`) - **Last Priority**
   - **Depends on**: ContainerSlots (via `vstoi:hasFirst`)
   - May have parent-child relationships (via `rdfs:subClassOf`)
   - Must be ingested **last** to ensure all ContainerSlots exist

#### Dependency Graph

```
┌─────────────────────────────────────────────────────────┐
│                  DA-SOC Ingestion Flow                  │
└─────────────────────────────────────────────────────────┘

                    1. Codebook
                         ↓
                    2. ResponseOption
                         ↓
                    3. ComponentStem
                         ↓
    ┌────────────────────┴────────────────────┐
    │                                         │
    ↓                                         ↓
4. Component ─────────────────────→    5. ContainerSlot
    │  (hasCodebook)                         │
    │  (hasComponentStem)                    │  (hasComponent)
    │                                        │  (belongsTo)
    └────────────────────┬───────────────────┘
                         ↓
                    6. Instrument
                       (hasFirst → ContainerSlot)
```

#### What Happens If Ingested Out of Order?

If DA-SOC files are ingested in the wrong order, the following issues may occur:

1. **Broken References**: UI will show "None Provided" or "(Unknown type: )" for dependent properties
   - Component without ComponentStem: `hasComponentStem` will be null
   - Component without Codebook: `hasCodebook` will be null
   - Instrument without ContainerSlots: `SlotElements` table will be empty

2. **Failed URI Resolution**: Reference URIs won't match existing entity URIs
   - System logs will show: `[DEBUG-CSTEM-FIND] No results found for URI`
   - `find()` methods will return null

3. **Missing Relationships**: Entity relationships won't be established in the triplestore
   - Navigation between related entities will fail
   - SPARQL queries for relationships will return empty results

#### Recovery from Wrong Order

If you've ingested DA-SOC files in the wrong order:

1. **Uningest all DA-SOC files** in reverse order (Instrument → ContainerSlot → Component → ComponentStem → ResponseOption → Codebook)
2. **Wait for uningest to complete** (check DataFile status)
3. **Re-ingest in correct order** following the sequence above

### URI Transformation and Prefix Mapping

**⚠️ CRITICAL**: DA-SOC CSV files contain originalIDs with abbreviated formats (e.g., `pmsr:/CBK001`), but VSTOI instances are stored with full prefixed URIs (e.g., `https://pmsr.net/ont/CB-CBK001`). The system automatically transforms these during enrichment.

#### Transformation Rules

The `convertToVstoiUri()` method in `AnnotateDASOC.java` applies the following transformations:

| CSV Value Format | Entity Type | VSTOI URI Format | Transformation |
|------------------|-------------|------------------|----------------|
| `pmsr:/CBK{id}` | Codebook | `pmsr:CB-CBK{id}` | Add `CB-` prefix |
| `pmsr:/CSM{id}` | ComponentStem | `pmsr:CSTEM-CSM{id}` | Add `CSTEM-` prefix |
| `pmsr:/COM{id}` | Component | `pmsr:COMP-COM{id}` | Add `COMP-` prefix |
| `pmsr:/ROP{id}` | ResponseOption | `pmsr:ROPT-ROP{id}` | Add `ROPT-` prefix |
| `pmsr:/INS{id}` | Instrument | `pmsr:INST-INS{id}` | Add `INST-` prefix |
| `pmsr:/CTS{id}` | ContainerSlot | `pmsr:CTSLOT-CTS{id}` | Add `CTSLOT-` prefix |
| `pmsr:/AST{id}` | AnnotationStem | `pmsr:ASTEM-AST{id}` | Add `ASTEM-` prefix |

#### Example Transformation

**DA-SOC CSV Content**:
```csv
originalID,vstoi:hasComponentStem,vstoi:hasCodebook,vstoi:isAttributeOf
COM1738097990641815,pmsr:/CSM1738097871592315,pmsr:/CBK1738096258564815,uberon:0001004
```

**System Processing**:
1. Finds Component with URI: `https://pmsr.net/ont/COMP-COM1738097990641815`
2. Transforms `pmsr:/CSM1738097871592315` → `https://pmsr.net/ont/CSTEM-CSM1738097871592315`
3. Transforms `pmsr:/CBK1738096258564815` → `https://pmsr.net/ont/CB-CBK1738096258564815`
4. Sets properties on Component POJO
5. Saves to triplestore with full URIs

**Debug Output**:
```
[DEBUG-URI-TRANSFORM] ComponentStem: pmsr:/CSM1738097871592315 -> https://pmsr.net/ont/CSTEM-CSM1738097871592315
[DEBUG-URI-TRANSFORM] Codebook: pmsr:/CBK1738096258564815 -> https://pmsr.net/ont/CB-CBK1738096258564815
```

This transformation ensures that:
- ✅ CSV files remain human-readable with short IDs
- ✅ References resolve correctly to existing VSTOI instances
- ✅ `find()` methods successfully locate target entities
- ✅ UI displays relationships correctly

---

## Implementation Details

### Files Modified

#### 1. StudyObjectGenerator.java
**Location**: `app/org/hascoapi/ingestion/StudyObjectGenerator.java`

**Changes**:
- Added detection for Codebook, ResponseOption, AnnotationStem types
- Created factory methods for each VSTOI type
- Implemented dual-URI generation (StudyObject + VSTOI instance)
- Added linking logic between layers

**Key Methods**:
```java
private String detectVstoiType(String typeUri)
private void createVstoiEntityIfApplicable(StudyObject so)
private void createInstrumentFromStudyObject(StudyObject so)
private void createCodebookFromStudyObject(StudyObject so)
// ... 7 create methods total
```

#### 2. AnnotateDASOC.java
**Location**: `app/org/hascoapi/ingestion/AnnotateDASOC.java`

**Changes**:
- Enhanced type detection for all 7 VSTOI types
- Created enrichment methods for each type
- Fixed CSV column access (was using index, now uses column name)
- Added VSTOI instance URI resolution
- Comprehensive debug logging

**Key Methods**:
```java
private static void enrichVstoiEntity(String vstoiInstanceUri, ...)
private static void enrichInstrument(String uri, ...)
private static void enrichCodebook(String uri, ...)
private static String getVSTOIInstanceUri(String studyObjectUri, ...)
private static String detectVstoiTypeByUri(String uri)
// ... 7 enrichment methods total
```

#### 3. IngestionAPI.java
**Location**: `app/org/hascoapi/console/controllers/restapi/IngestionAPI.java`

**Changes**:
- Added DA (DataAcquisition) type recognition in `uningestMetadataTemplate()`
- Implemented DA uningest handler
- Allows removing DA-SOC enrichments without affecting base entities

**Key Addition**:
```java
else if (mtType.equals(HASCO.DATA_ACQUISITION)) {
    // Handle DA uningest: remove enrichment triples but preserve VSTOI instances
}
```

### Workflow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    DSG INGESTION                            │
└─────────────────────────────────────────────────────────────┘
                            │
                            ↓
              ┌─────────────────────────────┐
              │  Upload DSG-STUDY.xlsx      │
              └─────────────────────────────┘
                            │
                            ↓
              ┌─────────────────────────────┐
              │  Parse Worksheets:          │
              │  - STD (Study metadata)     │
              │  - SSD (Semantic schema)    │
              │  - SOC-* (Entity data)      │
              └─────────────────────────────┘
                            │
                            ↓
              ┌─────────────────────────────┐
              │  For each SOC row:          │
              │  1. Create StudyObject      │
              │  2. Detect VSTOI type       │
              │  3. Create VSTOI instance   │
              │  4. Link Layer 1 ←→ Layer 2 │
              └─────────────────────────────┘
                            │
                            ↓
              ┌─────────────────────────────┐
              │  Commit to Triplestore      │
              │  Named Graph: DFL{id}       │
              └─────────────────────────────┘
                            │
                            ↓
              Result: 200+ VSTOI instances created
                      with base properties


┌─────────────────────────────────────────────────────────────┐
│                  DA-SOC ENRICHMENT                          │
└─────────────────────────────────────────────────────────────┘
                            │
                            ↓
              ┌─────────────────────────────┐
              │  Upload DA-SOC-*.csv        │
              └─────────────────────────────┘
                            │
                            ↓
              ┌─────────────────────────────┐
              │  Discover Study Context     │
              │  (Query by originalID)      │
              └─────────────────────────────┘
                            │
                            ↓
              ┌─────────────────────────────┐
              │  For each CSV row:          │
              │  1. Lookup StudyObject URI  │
              │  2. Resolve VSTOI URI       │
              │  3. Add property triples    │
              │  4. Enrich POJO object      │
              │  5. Save to triplestore     │
              └─────────────────────────────┘
                            │
                            ↓
              ┌─────────────────────────────┐
              │  Commit Extended Properties │
              │  Same Named Graph: DFL{id}  │
              └─────────────────────────────┘
                            │
                            ↓
              Result: Enriched entities with
                      relationships & metadata
```

---

## Critical Fixes Applied

### Fix #1: DA Uningest Not Supported
**Problem**: DataAcquisition files could not be uningest from the system, causing accumulation of stale data.

**Solution**: Added DA type recognition and uningest handler in `IngestionAPI.java` that:
- Removes DA-specific triples from the named graph
- Preserves VSTOI instances and their base properties
- Resets DataFile status to UNPROCESSED for re-ingestion

### Fix #2: Wrong URI Being Enriched
**Problem**: `enrichVstoiEntity()` was receiving StudyObject URIs (e.g., `pmsr:OBJ_instrumentcollection_INS...`) instead of VSTOI instance URIs (e.g., `pmsr:INST-INS...`), causing `find()` methods to return null.

**Solution**: Added `getVSTOIInstanceUri()` resolution step that:
- Queries the triplestore for linking properties (`vstoi:hasInstrument`, etc.)
- Follows the link from StudyObject to VSTOI instance
- Passes the correct URI to enrichment functions

**Code**:
```java
// Before enrichment, resolve VSTOI instance URI
String vstoiInstanceUri = getVSTOIInstanceUri(objectUri, dataFile);
if (vstoiInstanceUri != null && !vstoiInstanceUri.isEmpty()) {
    enrichVstoiEntity(vstoiInstanceUri, properties, dataFile);
}
```

### Fix #3: Wrong CSV Access Method
**Problem**: Properties map was being built using **index-based CSV access** (`record.get(i)`) instead of **column name-based access** (`record.get(columnName)`), resulting in empty values.

**Root Cause**: Apache Commons CSV with `.withFirstRecordAsHeader()` creates a column name index, requiring access by name rather than position.

**Solution**:
```java
// BEFORE (wrong)
String value = record.get(i).trim();

// AFTER (correct)
String columnName = headers.get(i);
String value = record.get(columnName).trim();
```

This fix was critical because:
- RDF triple creation worked (it was using column name access)
- But POJO enrichment failed (it was using index access)
- Result: Triples existed in triplestore, but POJOs showed empty properties in UI

### Fix #4: URI Prefix Mismatch Between DA-SOC References and VSTOI Instances

**Problem**: DA-SOC CSV files contained abbreviated originalID references (e.g., `pmsr:/CBK1738096258564815`), but VSTOI instances were created with prefixed URIs (e.g., `https://pmsr.net/ont/CB-CBK1738096258564815`). When enrichment methods tried to resolve these references using `find()`, they returned null because the URIs didn't match.

**Manifestation**:
- Components saved `hasCodebook = https://pmsr.net/ont/CBK1738096258564815` (missing `CB-` prefix)
- Codebook stored as `https://pmsr.net/ont/CB-CBK1738096258564815`
- `Codebook.find()` returned null → UI showed "None Provided"
- Same issue affected: ComponentStems (`CSTEM-`), ContainerSlots (`CTSLOT-`), Instruments (`INST-`)

**Root Cause**: The `URIUtils.replacePrefixEx()` method only expanded namespace prefixes (e.g., `pmsr:/` → `https://pmsr.net/ont/`) but didn't add the entity type prefixes (`CB-`, `CSTEM-`, etc.) that `StudyObjectGenerator` uses when creating VSTOI instances.

**Solution**: Created `convertToVstoiUri()` helper method that:
1. Expands namespace prefixes
2. Detects entity type from originalID pattern (CBK, CSM, COM, etc.)
3. Adds appropriate entity prefix (CB-, CSTEM-, COMP-, etc.)

**Code**:
```java
private static String convertToVstoiUri(String value) {
    String expandedUri = URIUtils.replacePrefixEx(value);
    
    if (expandedUri.contains("#CBK")) {
        expandedUri = expandedUri.replace("#CBK", "#CB-CBK");
    } else if (expandedUri.contains("#CSM")) {
        expandedUri = expandedUri.replace("#CSM", "#CSTEM-CSM");
    } else if (expandedUri.contains("#COM")) {
        expandedUri = expandedUri.replace("#COM", "#COMP-COM");
    }
    // ... additional mappings for all entity types
    
    return expandedUri;
}

// Usage in enrichComponent()
component.setHasCodebook(convertToVstoiUri(value));
component.setHasComponentStem(convertToVstoiUri(value));
```

**Impact**: This fix ensures that all inter-entity references resolve correctly, enabling:
- ✅ Components display their Codebooks and ComponentStems in UI
- ✅ Instruments display their ContainerSlots in SlotElements table
- ✅ Navigation between related entities works properly
- ✅ SPARQL queries using relationship properties return correct results

### Fix #5: Literal Values Being Treated as URIs

**Problem**: When viewing entities (especially Codebooks) in the UI, the system was generating warnings about literal values:
```
[WARNING] URIPage.objectFromUri(): No generic instance found for uri [https://pmsr.net/ont/en]
[WARNING] URIPage.objectFromUri(): No generic instance found for uri [https://pmsr.net/ont/1]
```

**Manifestation**:
- DA-SOC CSV files contained literal values (language codes, version numbers) with namespace prefixes: `pmsr:/en`, `pmsr:/1`
- These were being saved to the triplestore with full URIs: `https://pmsr.net/ont/en`, `https://pmsr.net/ont/1`
- When displaying entities in UI, the system tried to resolve these as entity URIs instead of literal strings
- Result: Continuous warnings in logs, confusion about what properties are entities vs. literals

**Root Cause**: DA-SOC CSV files were incorrectly formatted with namespace prefixes on literal values. The enrichment code was accepting these values as-is, causing them to be stored as URIs in the triplestore.

**Solution**: Created `cleanLiteralValue()` helper method that strips namespace prefixes from literal values:
```java
private static String cleanLiteralValue(String value) {
    if (value == null || value.isEmpty()) {
        return value;
    }
    
    // Check if value starts with a namespace prefix pattern (prefix:/ or prefix:#)
    if (value.contains(":/")) {
        String[] parts = value.split(":/");
        return parts[parts.length - 1]; // Extract value after prefix
    } else if (value.contains("#")) {
        String[] parts = value.split("#");
        return parts[parts.length - 1];
    }
    
    return value;
}

// Applied to all literal properties across all enrichment methods
codebook.setHasLanguage(cleanLiteralValue(value));  // "pmsr:/en" → "en"
codebook.setHasVersion(cleanLiteralValue(value));   // "pmsr:/1" → "1"
```

**Properties Cleaned Across All Entity Types**:

| Entity Type | Literal Properties Cleaned |
|-------------|---------------------------|
| Instrument | `hasLanguage`, `hasVersion` |
| Component | `hasLanguage`, `hasVersion` |
| ComponentStem | `hasLanguage`, `hasVersion` |
| Codebook | `hasLanguage`, `hasVersion`, `hasStatus` |
| ResponseOption | `hasLanguage`, `hasStatus` |
| AnnotationStem | `hasLanguage`, `hasVersion`, `hasStatus` |

**Impact**: This fix ensures that:
- ✅ Literal values are stored as proper strings, not URIs
- ✅ No warnings when viewing entities in UI
- ✅ Properties display correctly as simple text values
- ✅ Clear distinction between entity references (URIs) and data values (literals)
- ✅ SPARQL queries can properly filter by literal values

**Note**: While this fix prevents the issue during ingestion, **DA-SOC CSV files should be corrected** to contain plain literal values (`en`, `1`) instead of prefixed values (`pmsr:/en`, `pmsr:/1`). The cleaning logic provides backward compatibility for incorrectly formatted files.

---

## Testing and Validation

### Test Dataset

**Study**: PMSR-Simulators-1  
**DSG File**: `DSG-PMSR-Simulators-1.xlsx`  
**DA-SOC Files**:
- `DA-SOC-INSTRUMENT-PMSR_1.csv`
- `DA-SOC-COMPONENT-PMSR_1.csv`
- `DA-SOC-COMPONENT-STEM-PMSR_1.csv`
- `DA-SOC-CONTAINER-SLOT-PMSR_1.csv`
- `DA-SOC-CODEBOOK_2.csv`

### Production Metrics

| Metric | Count | Status |
|--------|-------|--------|
| Total VSTOI instances | 209 | ✅ Created |
| Instruments | 71 | ✅ Enriched |
| Components | 16 | ✅ Enriched |
| ComponentStems | 83 | ✅ Enriched |
| ContainerSlots | 12 | ✅ Enriched |
| Codebooks | 12 | ✅ Enriched |
| ResponseOptions | 4 | ✅ Created |
| AnnotationStems | 11 | ✅ Created |

### Validation Queries

#### Check VSTOI Instances Exist
```sparql
PREFIX vstoi: <http://hadatac.org/ont/vstoi#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?type (COUNT(?instance) AS ?count) WHERE {
  ?instance a ?type .
  FILTER(STRSTARTS(STR(?type), "http://hadatac.org/ont/vstoi#"))
}
GROUP BY ?type
ORDER BY DESC(?count)
```

#### Check Properties Were Enriched
```sparql
PREFIX pmsr: <https://pmsr.net/ont/>
PREFIX vstoi: <http://hadatac.org/ont/vstoi#>

SELECT ?instrument ?label ?language ?version ?status WHERE {
  ?instrument a vstoi:Instrument .
  OPTIONAL { ?instrument rdfs:label ?label }
  OPTIONAL { ?instrument vstoi:hasLanguage ?language }
  OPTIONAL { ?instrument vstoi:hasVersion ?version }
  OPTIONAL { ?instrument vstoi:hasStatus ?status }
}
LIMIT 10
```

#### Verify Dual-Layer Linking
```sparql
PREFIX hasco: <http://hadatac.org/ont/hasco#>
PREFIX vstoi: <http://hadatac.org/ont/vstoi#>

SELECT ?studyObject ?vstoiInstance WHERE {
  ?studyObject a hasco:StudyObject ;
               vstoi:hasInstrument ?vstoiInstance .
  ?vstoiInstance a vstoi:Instrument .
}
LIMIT 10
```

### UI Validation

After successful ingestion and enrichment, the following should be verified in the HADatAc UI:

1. **Study Page** (`/sir/studies`)
   - Shows 6 Object Collections
   - Shows total count of 209 Objects

2. **Instruments Page** (`/sir/select/instrument/1/9`)
   - Lists 71 instruments
   - Shows Language, Version columns populated
   - Instrument detail pages show all properties

3. **Components Page**
   - Lists 16 components
   - Shows ComponentStem and Codebook links

4. **Codebooks Page** (`/codebook`)
   - Lists 12 codebooks
   - Shows Language, Version, Status columns populated

---

## Troubleshooting Guide

### Common Issues and Solutions

#### Issue 1: UI Shows "None Provided" for Codebook/ComponentStem in Components

**Symptoms**:
- Component detail page displays "None Provided" for Codebook
- Component detail page displays "None Provided" for ComponentStem
- DA-SOC ingestion logs show successful save
- No error messages in application logs

**Cause**: DA-SOC files were ingested in wrong order. Components were enriched before Codebooks and ComponentStems existed.

**Solution**:
1. Uningest all DA-SOC files (in reverse order)
2. Re-ingest in correct order: Codebook → ComponentStem → Component

**Verification**:
```
[DEBUG-COMPONENT-UI] hasCodebook value: https://pmsr.net/ont/CB-CBK...
[DEBUG-CODEBOOK-FIND] Results found! Creating new Codebook instance
```

#### Issue 2: Instruments Show Empty SlotElements Table

**Symptoms**:
- Instrument detail page shows "Slots Elements" table with message "(Unknown type: )"
- No slots appear in the table
- ContainerSlots exist in the system

**Cause**: Either:
- DA-SOC files ingested in wrong order (ContainerSlots before Instruments)
- `vstoi:hasFirst` property not set on Instrument
- ContainerSlots not linked via `vstoi:hasNext` chain

**Solution**:
1. Check Instrument enrichment: `[DEBUG] Instrument MODIFIED - saving changes`
2. Verify `hasFirst` property is present in DA-SOC-INSTRUMENT CSV
3. Uningest and re-ingest: ContainerSlot → Instrument (in that order)

**Debug Logging**:
```
[DEBUG-SLOTS] Container.hasFirst = https://pmsr.net/ont/CTSLOT-CTS...
[DEBUG-SLOTS] Found 5 slot elements
```

#### Issue 3: "No results found for URI" in Logs

**Symptoms**:
- Debug logs show: `[DEBUG-CSTEM-FIND] No results found for URI: https://pmsr.net/ont/CSM...`
- Entity exists in triplestore with slightly different URI
- UI shows null/empty references

**Cause**: URI prefix mismatch. DA-SOC references use abbreviated format without entity type prefix.

**Solution**: This should be automatically handled by `convertToVstoiUri()`. If still occurring:
1. Check DA-SOC CSV format - ensure originalIDs match DSG format
2. Verify `convertToVstoiUri()` is being called in enrichment methods
3. Look for debug messages: `[DEBUG-URI-TRANSFORM] Codebook: pmsr:/CBK... -> ...`

**Verification Query**:
```sparql
# Check what URI format is actually stored
SELECT ?uri ?label WHERE {
  ?uri a vstoi:Codebook ;
       rdfs:label ?label .
} LIMIT 10
```

#### Issue 4: DA-SOC Ingestion Shows "No originalIDs found"

**Symptoms**:
- DA-SOC ingestion completes but reports 0 objects enriched
- Log message: "No StudyObjects found with originalIDs in Study"

**Cause**: Either:
- DSG file not ingested yet
- originalIDs in DA-SOC don't match originalIDs in DSG
- Wrong study context detected

**Solution**:
1. Verify DSG ingestion completed successfully
2. Compare originalID values in DSG SOC worksheet vs. DA-SOC CSV
3. Check study detection: `[DASOC] Study discovered (Strategy 2): ...`
4. Ensure originalID column in DA-SOC CSV is exactly named `originalID` (case-sensitive)

#### Issue 5: Properties Not Appearing After DA-SOC Ingestion

**Symptoms**:
- DA-SOC ingestion completes without errors
- POJO entities still show empty properties in UI
- Logs show: `Properties collected: 0`

**Cause**: CSV column headers don't match expected property URIs.

**Solution**:
1. Check CSV headers match property URI format: `vstoi:hasLanguage` not `hasLanguage`
2. Remove BOM (Byte Order Mark) if using UTF-8: `\uFEFF` at start of file
3. Verify no extra spaces in column headers
4. Check debug logs: `[ENRICH-TRACE] Property: vstoi:hasLanguage → ... = en`

**CSV Header Format**:
```csv
originalID,vstoi:hasLanguage,vstoi:hasVersion,vstoi:hasCodebook
COM001,en,1.0,pmsr:/CBK001
```

#### Issue 6: Circular Dependency During Ingestion

**Symptoms**:
- Ingestion hangs or times out
- Logs show repeated queries for same entities

**Cause**: Circular references in entity relationships (e.g., ComponentStem parent-child loops).

**Solution**:
1. Review DA-SOC CSV for circular `rdfs:subClassOf` references
2. Ensure ContainerSlot `hasNext` chains are acyclic
3. Break circular dependencies in CSV data

#### Issue 7: Changes Not Reflected in UI After Re-ingestion

**Symptoms**:
- Re-ingested DA-SOC with updated values
- UI still shows old values

**Cause**: Browser cache or entity cache not refreshed.

**Solution**:
1. Hard refresh browser (Ctrl+F5 / Cmd+Shift+R)
2. Check entity was actually modified: `[DEBUG-COMPONENT] Component MODIFIED - saving changes`
3. Query triplestore directly to verify values:
   ```sparql
   SELECT * WHERE {
     <https://pmsr.net/ont/COMP-COM001> ?p ?o .
   }
   ```

#### Issue 8: Warnings About Literal Values Being Treated as URIs

**Symptoms**:
- Console logs show repeated warnings when viewing entities:
  ```
  [WARNING] No generic instance found for uri [https://pmsr.net/ont/en]
  [WARNING] No generic instance found for uri [https://pmsr.net/ont/1]
  ```
- Warnings appear especially when viewing Codebooks, Instruments, or Components
- Same entity shows warnings multiple times

**Cause**: DA-SOC CSV files contain literal values (language, version, status) with namespace prefixes:
- CSV has: `pmsr:/en` instead of `en`
- CSV has: `pmsr:/1` instead of `1`
- System interprets these as entity URIs and tries to resolve them

**Solution**:
1. **Fix CSV files** (recommended): Remove namespace prefixes from literal values
   - Before: `vstoi:hasLanguage,vstoi:hasVersion` → `pmsr:/en,pmsr:/1`
   - After: `vstoi:hasLanguage,vstoi:hasVersion` → `en,1`

2. **Use automatic cleaning** (already implemented): The `cleanLiteralValue()` method automatically strips prefixes during ingestion
   - Re-ingest DA-SOC files after code update
   - Warnings will disappear on next page load

3. **Verify in triplestore**:
   ```sparql
   SELECT ?codebook ?language ?version WHERE {
     ?codebook a vstoi:Codebook ;
               vstoi:hasLanguage ?language ;
               vstoi:hasVersion ?version .
   } LIMIT 5
   ```
   - Language should show: `"en"` (string literal)
   - NOT: `<https://pmsr.net/ont/en>` (URI)

**Prevention**: Always use plain values for literal properties in DA-SOC CSVs:
- ✅ Correct: `en`, `1`, `PUBLISHED`, `2.0`
- ❌ Incorrect: `pmsr:/en`, `pmsr:/1`, `pmsr:/PUBLISHED`

### Debug Logging Reference

Key debug messages to look for during troubleshooting:

| Log Message | Location | Meaning |
|-------------|----------|---------|
| `[DEBUG-URI-TRANSFORM]` | AnnotateDASOC | URI transformation being applied |
| `[DEBUG-LITERAL-CLEAN]` | AnnotateDASOC | Literal value cleaning (removing namespace prefixes) |
| `[DEBUG-COMPONENT-UI]` | Component.java | UI retrieving component properties |
| `[DEBUG-CODEBOOK-FIND]` | Codebook.java | Codebook lookup in triplestore |
| `[DEBUG-CSTEM-FIND]` | ComponentStem.java | ComponentStem lookup |
| `[DEBUG-SLOTS]` | Container.java | SlotElements retrieval for instruments |
| `[ENRICH-TRACE]` | AnnotateDASOC | Property collection from CSV |
| `[DEBUG-COMPONENT]` | AnnotateDASOC | Component enrichment process |

### Recovery Procedures

#### Complete Reset (Nuclear Option)

If the system is in an inconsistent state and you need to start fresh:

1. **Backup triplestore** (if needed)
2. **Uningest all DA-SOC files** (newest to oldest)
3. **Uningest DSG file**
4. **Clear browser cache**
5. **Re-ingest DSG file**
6. **Wait for processing to complete**
7. **Re-ingest DA-SOC files in correct order**

#### Partial Reset (Fix Specific Entity Type)

To fix issues with a specific entity type without affecting others:

1. **Uningest only the affected DA-SOC file(s)**
2. **Verify entities still exist**: Check UI or run SPARQL query
3. **Fix DA-SOC CSV data**
4. **Re-ingest in correct dependency order**

---

## Future Enhancements

### Potential Improvements

1. **Batch Validation**: Add pre-ingestion validation for DA-SOC files to check:
   - All originalIDs exist in the study
   - Property URIs are valid
   - Value formats match expected types

2. **Conflict Resolution**: Handle cases where DA-SOC files contain conflicting property values:
   - Last-write-wins strategy
   - Versioned property history
   - Warning logs for conflicts

3. **Property Schema Validation**: Validate property values against ontology constraints:
   - Data type validation (integer, string, URI)
   - Cardinality constraints (single-valued vs. multi-valued)
   - Required property checks

4. **UI Enhancements**:
   - Visual indicator showing which properties came from DSG vs. DA-SOC
   - Ability to edit properties directly in UI (generates DA-SOC update)
   - Diff view showing property changes across versions

5. **Performance Optimization**:
   - Batch SPARQL queries for large DA-SOC files
   - Caching of frequently accessed entities
   - Parallel processing for independent enrichment operations

6. **API Extensions**:
   - RESTful endpoints for programmatic DA-SOC submission
   - Webhooks for post-ingestion notifications
   - GraphQL interface for flexible entity queries

---

## Conclusion

The VSTOI-based DSG implementation represents a significant architectural improvement over the legacy INS file system. By leveraging the dual-layer architecture and flexible enrichment via DA-SOC files, the system provides:

- **Unified management** of all VSTOI entity types
- **Scalability** for large instrument inventories
- **Flexibility** to add/modify properties without re-ingestion
- **Clean separation** between study management and domain semantics
- **Production-ready** solution validated with 200+ real entities

The implementation is complete, tested, and operational in production environments. All seven VSTOI entity types are fully supported with comprehensive property enrichment capabilities.

**Next steps for users**:
1. Prepare DSG files with SOC-* worksheets for your VSTOI entities
2. Ingest DSG file to create base entities (StudyObjects + VSTOI instances)
3. Prepare DA-SOC CSV files to enrich entities with extended properties and relationships
4. **⚠️ IMPORTANT**: Ingest DA-SOC files in the correct dependency order (see [DA-SOC Ingestion Order](#da-soc-ingestion-order)):
   - Codebook → ResponseOption → ComponentStem → Component → ContainerSlot → Instrument
5. Query and use your enriched VSTOI instances via API or UI
6. If issues occur, consult the [Troubleshooting Guide](#troubleshooting-guide)

For technical support or questions, refer to the HADatAc documentation or contact the development team.

---

**Document Version**: 1.2  
**Last Updated**: April 30, 2026  
**Author**: Kaell  
**Contributors**: AI Assistant (GitHub Copilot)  
**Changelog**:
- v1.2 (2026-04-30): Added Fix #5 (literal values treated as URIs), ContainerSlot CTSLOT prefix fix, rdfs:subClassOf URI transformation, Issue #8 in troubleshooting
- v1.1 (2026-04-30): Added DA-SOC ingestion order section, URI transformation details, troubleshooting guide, Fix #4 (URI prefix mismatch)
- v1.0 (2026-04-30): Initial complete documentation

