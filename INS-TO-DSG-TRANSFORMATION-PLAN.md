# INS-TO-DSG TRANSFORMATION PLAN
**Complete Architectural Transformation: Eliminating INS and Migrating to DSG-based Instrument Management**

---

## EXECUTIVE SUMMARY

### Current State
The system currently uses **two parallel metadata templates** for managing instrument data:
- **INS (Instrument Namespace Specification)**: Excel-based ontology describing instruments, components, stems, slots, codebooks, response options, and annotations
- **DSG (Data Set Generator)**: Excel-based metadata template describing scientific studies with Study Object Collections (SOCs)

### Target State
**Eliminate INS entirely** and consolidate all instrument management into the DSG framework by:
1. **SOCs represent VSTOI entities**: Instruments, Components, ComponentStems, ContainerSlots, Codebooks, ResponseOptions, AnnotationStems
2. **DA-SOCs handle extended properties**: Properties that don't fit in standard SOC schema
3. **All existing API endpoints remain unchanged**: Internal implementation changes only - no contract modifications

### Critical Constraint
⚠️ **API CONTRACTS MUST NOT CHANGE**
- All existing endpoints in `conf/routes` must continue working
- Response formats must remain identical
- Only internal implementation logic changes

---

## TABLE OF CONTENTS
1. [Current Architecture Analysis](#1-current-architecture-analysis)
2. [Target Architecture Design](#2-target-architecture-design)
3. [Transformation Strategy](#3-transformation-strategy)
4. [File-by-File Change Plan](#4-file-by-file-change-plan)
5. [API Endpoint Mapping](#5-api-endpoint-mapping)
6. [Data Migration Strategy](#6-data-migration-strategy)
7. [Testing Strategy](#7-testing-strategy)
8. [Risks and Mitigation](#8-risks-and-mitigation)
9. [Implementation Phases](#9-implementation-phases)
10. [Rollback Plan](#10-rollback-plan)

---

## 1. CURRENT ARCHITECTURE ANALYSIS

### 1.1 INS System Components

#### 1.1.1 INS File Structure (Excel Workbook)
```
INS-EXAMPLE.xlsx
├── InfoSheet          (Metadata control)
├── Namespaces         (Ontology prefixes)
├── Instruments        (Instrument classes)
├── SlotElements       (Container slots ordering)
├── ComponentStems     (Reusable component templates)
├── Components         (Concrete component instances)
├── CodeBooks          (Discrete response value sets)
├── CodeBookSlots      (Options within codebooks)
├── ResponseOptions    (Individual response choices)
├── Annotations        (Metadata annotations)
└── AnnotationStems    (Reusable annotation templates)
```

**URI Naming Pattern**:
- Instruments: `ahead:INS<hash>`
- Components: `ahead:DTC<hash>` or `ahead:DET-<name>`
- ComponentStems: `ahead:DSM<hash>`
- ContainerSlots: `ahead:INS<hash>/CTS/0001`
- No underscores in URI segments

#### 1.1.2 INS Ingestion Pipeline
```
IngestionWorker.ingest()
  ↓ (file: INS-*.xlsx)
IngestionWorker.getGeneratorChain()
  ↓
AnnotateINS.exec()
  ↓
GeneratorChain [
  - INSGenerator (instruments)
  - INSGenerator (slotelement)
  - INSGenerator (componentstem)
  - ComponentGenerator (component)
  - INSGenerator (codebook)
  - CodeBookSlotGenerator (codebookslot)
  - INSGenerator (responseoption)
]
  ↓
Each generator creates POJOs:
  - Instrument.save()
  - Component.save()
  - ComponentStem.save()
  - ContainerSlot.save()
  - Codebook.save()
  - ResponseOption.save()
```

**Key Files**:
- `app/org/hascoapi/ingestion/AnnotateINS.java` - INS ingestion orchestrator
- `app/org/hascoapi/ingestion/INSGenerator.java` - Generic INS row processor
- `app/org/hascoapi/ingestion/ComponentGenerator.java` - Component-specific processor
- `app/org/hascoapi/ingestion/CodeBookSlotGenerator.java` - CodeBookSlot processor
- `app/org/hascoapi/transform/mt/ins/INSGen.java` - INS file generator (export)
- `app/org/hascoapi/transform/mt/ins/INSInstrument.java` - Instrument export helper
- `app/org/hascoapi/transform/mt/ins/INSComponent.java` - Component export helper
- `app/org/hascoapi/transform/mt/ins/INSCodebook.java` - Codebook export helper

#### 1.1.3 INS API Endpoints
```
Routes (conf/routes):

# Generic retrieval
GET  /hascoapi/api/:elementType/elements/:pageSize/:offset
  → SIRElementAPI.getElementsWithPage()
    → GenericFind.findInstancesWithPages(Instrument.class, ...)

# Instrument-specific
GET  /hascoapi/api/instrument/components/:instrumentUri
  → InstrumentAPI.retrieveInstrumentComponents()
    → InstrumentTraversal.retrieveInstrumentComponents()

GET  /hascoapi/api/instrument/containerslots/:instrumentUri
  → InstrumentAPI.retrieveInstrumentContainerSlots()

# Rendering
GET  /hascoapi/api/instrument/totext/plain/:uri
GET  /hascoapi/api/instrument/totext/html/:uri
GET  /hascoapi/api/instrument/tordf/:uri
GET  /hascoapi/api/instrument/tofhir/:uri

# Create/Delete
POST /hascoapi/api/:elementType/create/:json
  → SIRElementAPI.createElement()
    → InstrumentAPI.createInstrument() if elementType == "instrument"

# Export
GET  /hascoapi/api/mt/gen/perelement/:elementtype/:datafileuri/...
  → IngestionAPI.mtGenByElement()
    → INSGen.genByInstrument() if elementtype == "ins"
```

**Key Observation**: API endpoints are **generic and element-type driven**, not INS-specific. They route based on `elementType` parameter.

---

### 1.2 DSG System Components

#### 1.2.1 DSG File Structure (Excel Workbook)
```
DSG-STUDY-NAME.xlsx
├── InfoSheet          (Metadata control + Study URI)
├── Namespaces         (Ontology prefixes)
├── SSD                (Study Object Collection definitions)
├── STD                (Study administrative metadata)
├── VD                 (Variable Design - optional)
└── SOC-*              (One worksheet per SOC with instances)
    ├── SOC-LOCATION
    ├── SOC-WEATHER-AT-LOCATION
    └── SOC-LOCATION-TYPE
```

**SOC Structure** (e.g., SOC-LOCATION worksheet):
```
| originalID    | rdf:type       | scopeID | timeScopeID | spaceScopeID |
|---------------|----------------|---------|-------------|--------------|
| H-outdoor     | pato:0000140   |         |             | outdoor      |
| C-indoor      | pato:0000140   |         |             | indoor       |
```

#### 1.2.2 DSG Ingestion Pipeline
```
IngestionWorker.ingest()
  ↓ (file: DSG-*.xlsx)
IngestionWorker.getGeneratorChain()
  ↓
AnnotateDSG.exec()
  ↓
GeneratorChain [
  - SSDGenerator (creates StudyObjectCollections)
  - StudyObjectGenerator (creates StudyObjects from SOC-* sheets)
]
  ↓
SSDGenerator.createObjectCollection()
  → StudyObjectCollection.save()
  
StudyObjectGenerator.createRow()
  → StudyObject.save()
  → createVstoiEntityIfApplicable()  ← RECENT ADDITION
      ↓
      if (typeUri matches vstoi pattern):
        - createInstrumentFromStudyObject()
        - createComponentFromStudyObject()
        - createComponentStemFromStudyObject()
        - createContainerSlotFromStudyObject()
```

**Key Files**:
- `app/org/hascoapi/ingestion/AnnotateDSG.java` - DSG ingestion orchestrator
- `app/org/hascoapi/ingestion/SSDGenerator.java` - SOC definition processor
- `app/org/hascoapi/ingestion/StudyObjectGenerator.java` - SOC instance processor (with VSTOI entity creation)
- `app/org/hascoapi/transform/mt/dsg/DSGGen.java` - DSG file generator (export)
- `app/org/hascoapi/transform/mt/dsg/DSGSSD.java` - SSD export helper
- `app/org/hascoapi/transform/mt/dsg/DSGSOC.java` - SOC export helper

---

### 1.3 Recent VSTOI Integration (Bridge Implementation)

**Status**: ✅ **Partially Implemented** (2026-03)

During DSG ingestion, `StudyObjectGenerator.java` now detects VSTOI types and auto-creates entities:

```java
// In StudyObjectGenerator.createRow()
StudyObject studyObject = new StudyObject(...);
studyObject.save();

// NEW: Detect and create VSTOI entities
createVstoiEntityIfApplicable(studyObject);

private void createVstoiEntityIfApplicable(StudyObject studyObject) {
    String typeUri = studyObject.getTypeUri();
    String vstoiType = detectVstoiType(typeUri);
    
    if (VSTOI.INSTRUMENT.equals(vstoiType)) {
        createInstrumentFromStudyObject(studyObject);
    } else if (VSTOI.COMPONENT.equals(vstoiType)) {
        createComponentFromStudyObject(studyObject);
    } else if (VSTOI.COMPONENT_STEM.equals(vstoiType)) {
        createComponentStemFromStudyObject(studyObject);
    } else if (VSTOI.CONTAINER_SLOT.equals(vstoiType)) {
        createContainerSlotFromStudyObject(studyObject);
    }
}

private String detectVstoiType(String typeUri) {
    if (VSTOI.INSTRUMENT.equals(typeUri)) return VSTOI.INSTRUMENT;
    if (VSTOI.COMPONENT.equals(typeUri)) return VSTOI.COMPONENT;
    if (typeUri.contains("Detector")) return VSTOI.COMPONENT;
    if (typeUri.contains("ComponentStem")) return VSTOI.COMPONENT_STEM;
    if (typeUri.contains("ContainerSlot")) return VSTOI.CONTAINER_SLOT;
    return null;
}
```

**Example DSG → VSTOI Mapping**:
```
DSG File:
  SOC-EQUIPMENT-MODULE (71 instances) → 71 Instruments created
  SOC-CONTROL-MODULE (16 instances)   → 16 Components created
  SOC-COMPONENT-STEM (4 instances)    → 4 ComponentStems created
  SOC-SLOT-ELEMENT (83 instances)     → 83 ContainerSlots created
```

**Limitations of Current Bridge**:
- ✅ Creates basic VSTOI entities (uri, typeUri, label, comment)
- ❌ Does not handle INS-specific relationships (vstoi:hasComponentStem, vstoi:hasFirst, vstoi:hasNext)
- ❌ Does not handle CodeBooks, ResponseOptions, AnnotationStems
- ❌ Does not handle extended properties (hasLanguage, hasVersion, hasStatus, etc.)

---

### 1.4 DA-SOC System (Extended Properties)

**Purpose**: Enrich existing StudyObject instances with properties not captured in DSG schema.

**Example**:
```csv
DA-SOC-LOCATION.csv

originalID,pharma:altitude_m,pharma:floor,pharma:zone_code
LIBRARY-L0,15.0,0,ZONE-A
SCIENCE-ROOF,25.5,4,ZONE-B
```

**Ingestion Result**:
```turtle
<.../LIBRARY-L0>
    # Base properties from DSG
    rdf:type              hasco:StudyObject ;
    hasco:isMemberOf      <.../SOC-LOCATION> ;
    hasco:originalID      "LIBRARY-L0" ;
    hasco:label           "Library Level 0" ;
    
    # Extended properties from DA-SOC (stored in named graph)
    pharma:altitude_m     "15.0" ;
    pharma:floor          "0" ;
    pharma:zone_code      "ZONE-A" ;
    hasco:hasTimestamp    "2026-04-17T10:30:00Z" .
```

**Key Files**:
- `app/org/hascoapi/ingestion/AnnotateDASOC.java` - DA-SOC processor
- `app/org/hascoapi/ingestion/DASOCGenerator.java` - DA-SOC generator wrapper
- Recently added: `enrichVstoiEntity()` method to update VSTOI POJOs

---

## 2. TARGET ARCHITECTURE DESIGN

### 2.1 Conceptual Model

**Current (INS + DSG)**:
```
INS File (Excel) → INS Ingestion → Instrument/Component POJOs
DSG File (Excel) → DSG Ingestion → Study/SOC/StudyObject POJOs
                                     ↓ (bridge)
                                  Instrument/Component POJOs
```

**Target (DSG Only)**:
```
DSG File (Excel) → DSG Ingestion → Study/SOC/StudyObject POJOs
                                     ↓ (enhanced)
                                  Instrument/Component POJOs
                                     ↓
DA-SOC Files (CSV) → DA-SOC Ingestion → Enrichment of VSTOI POJOs
```

### 2.2 SOC-to-VSTOI Entity Mapping

| VSTOI Entity      | SOC Naming Pattern      | rdf:type Example                    | Base Properties from DSG |
|-------------------|-------------------------|-------------------------------------|--------------------------|
| Instrument        | SOC-INSTRUMENT-*        | vstoi:Instrument, vstoi:Questionnaire | uri, label, comment, typeUri |
| Component         | SOC-COMPONENT-*         | vstoi:Component, vstoi:Detector     | uri, label, comment, typeUri |
| ComponentStem     | SOC-COMPONENT-STEM-*    | vstoi:ComponentStem                 | uri, label, comment, typeUri |
| ContainerSlot     | SOC-SLOT-ELEMENT-*      | vstoi:ContainerSlot                 | uri, label, comment, typeUri |
| Codebook          | SOC-CODEBOOK-*          | vstoi:Codebook                      | uri, label, comment, typeUri |
| ResponseOption    | SOC-RESPONSE-OPTION-*   | vstoi:ResponseOption                | uri, label, comment, typeUri |
| AnnotationStem    | SOC-ANNOTATION-STEM-*   | vstoi:AnnotationStem                | uri, label, comment, typeUri |

**Extended Properties via DA-SOC**:
```
DA-SOC-INSTRUMENT-<NAME>.csv columns:
  - vstoi:hasShortName
  - vstoi:hasLanguage
  - vstoi:hasVersion
  - vstoi:hasFirst (first ContainerSlot URI)
  - hasco:hasImage
  - vstoi:hasStatus

DA-SOC-COMPONENT-<NAME>.csv columns:
  - vstoi:hasComponentStem (URI reference)
  - vstoi:hasCodebook (URI reference)
  - vstoi:isAttributeOf (URI reference)
  - vstoi:hasLanguage
  - vstoi:hasVersion

DA-SOC-SLOT-ELEMENT-<NAME>.csv columns:
  - vstoi:belongsTo (Instrument URI)
  - vstoi:hasComponent (Component URI)
  - vstoi:hasNext (next slot URI)
  - vstoi:hasPrevious (previous slot URI)
  - vstoi:hasPriority (ordering number)
```

### 2.3 Relationship Modeling

**INS Approach** (Excel sheet relationships):
```
Instruments sheet:
  hasURI: ahead:INS1234
  vstoi:hasFirst: ahead:INS1234/CTS/0001

SlotElements sheet:
  hasURI: ahead:INS1234/CTS/0001
  vstoi:belongsTo: ahead:INS1234
  vstoi:hasComponent: ahead:DTC5678
  vstoi:hasNext: ahead:INS1234/CTS/0002

Components sheet:
  hasURI: ahead:DTC5678
  vstoi:hasComponentStem: ahead:DSM9999
```

**DSG + DA-SOC Approach**:
```
DSG File:
  SSD sheet:
    SOC-INSTRUMENT-ARROWHEAD
    SOC-SLOT-ELEMENT-ARROWHEAD
    SOC-COMPONENT-ARROWHEAD
    SOC-COMPONENT-STEM-ARROWHEAD
  
  SOC-INSTRUMENT-ARROWHEAD sheet:
    originalID: INS1234
    rdf:type: vstoi:Instrument
    
  SOC-SLOT-ELEMENT-ARROWHEAD sheet:
    originalID: CTS-0001
    rdf:type: vstoi:ContainerSlot
    
  SOC-COMPONENT-ARROWHEAD sheet:
    originalID: DTC5678
    rdf:type: vstoi:Component

DA-SOC-INSTRUMENT-ARROWHEAD.csv:
  originalID,vstoi:hasFirst
  INS1234,http://kb/INS1234/CTS/0001

DA-SOC-SLOT-ELEMENT-ARROWHEAD.csv:
  originalID,vstoi:belongsTo,vstoi:hasComponent,vstoi:hasNext
  CTS-0001,http://kb/INS1234,http://kb/DTC5678,http://kb/INS1234/CTS/0002

DA-SOC-COMPONENT-ARROWHEAD.csv:
  originalID,vstoi:hasComponentStem
  DTC5678,http://kb/DSM9999
```

### 2.4 API Response Strategy

**Key Principle**: API endpoints must return **identical JSON** regardless of whether data came from INS or DSG.

**Current Instrument API Response** (from INS):
```json
{
  "isSuccessful": true,
  "body": [
    {
      "uri": "http://kb/INS1234",
      "typeUri": "http://vstoi#Instrument",
      "hascoTypeUri": "http://vstoi#Instrument",
      "label": "Arrowhead Components V4",
      "comment": "Example instrument",
      "hasShortName": "ARROW-V4",
      "hasLanguage": "en",
      "hasVersion": "4.0",
      "hasFirst": "http://kb/INS1234/CTS/0001",
      "hasStatus": "DRAFT"
    }
  ]
}
```

**Target Instrument API Response** (from DSG + DA-SOC):
```json
{
  "isSuccessful": true,
  "body": [
    {
      "uri": "http://kb/INS1234",              // ← from StudyObject.uri
      "typeUri": "http://vstoi#Instrument",    // ← from StudyObject.typeUri
      "hascoTypeUri": "http://vstoi#Instrument", // ← from Instrument POJO
      "label": "Arrowhead Components V4",      // ← from StudyObject.label
      "comment": "Example instrument",         // ← from StudyObject.comment
      "hasShortName": "ARROW-V4",              // ← from DA-SOC enrichment
      "hasLanguage": "en",                     // ← from DA-SOC enrichment
      "hasVersion": "4.0",                     // ← from DA-SOC enrichment
      "hasFirst": "http://kb/INS1234/CTS/0001", // ← from DA-SOC enrichment
      "hasStatus": "DRAFT"                     // ← from DA-SOC enrichment
    }
  ]
}
```

**Implementation Strategy**:
- `Instrument.find(uri)` queries both:
  1. Main triplestore (for StudyObject base properties)
  2. DA-SOC named graph (for extended properties)
- POJO fields populated from both sources
- JSON serialization produces identical output

---

## 3. TRANSFORMATION STRATEGY

### 3.1 Core Principles

1. **Zero API Contract Changes**: All existing endpoints continue working
2. **Gradual Migration**: System supports both INS and DSG during transition
3. **Backward Compatibility**: Old INS files can still be ingested (with deprecation warning)
4. **Data Preservation**: No data loss - all INS data convertible to DSG + DA-SOC
5. **Performance Neutral**: Query performance must not degrade

### 3.2 High-Level Approach

**Phase 1: Enhanced DSG Ingestion** (Expand VSTOI entity creation)
- ✅ Already done: Basic Instrument/Component/ComponentStem/ContainerSlot creation
- ⬜ Add: Codebook, ResponseOption, AnnotationStem detection and creation
- ⬜ Add: Relationship property handling (hasComponentStem, hasNext, hasPrevious, etc.)

**Phase 2: DA-SOC VSTOI Enrichment** (Expand property coverage)
- ✅ Already done: Basic property enrichment in `AnnotateDASOC.java`
- ⬜ Add: Comprehensive property mapping for all VSTOI types
- ⬜ Add: Validation for URI references

**Phase 3: POJO Query Enhancement** (Unified data retrieval)
- ⬜ Modify `Instrument.find()`, `Component.find()`, etc. to query:
  1. Base StudyObject triples
  2. DA-SOC named graph triples
  3. Merge into single POJO
- ⬜ Ensure `GenericFind.findInstancesWithPages()` works with DSG-sourced data

**Phase 4: Export/Generation Update** (DSG file creation)
- ⬜ Modify `DSGGen.java` to export VSTOI-typed SOCs
- ⬜ Modify `DSGGen.generateDASOCsForStudies()` to export DA-SOC files for VSTOI properties
- ⬜ Add VSTOI-specific SOC naming conventions

**Phase 5: INS Deprecation** (Redirect to DSG)
- ⬜ Add warning message when INS file uploaded
- ⬜ Auto-convert INS → DSG + DA-SOC (temporary converter)
- ⬜ Update documentation

**Phase 6: INS Removal** (Clean up)
- ⬜ Remove `AnnotateINS.java`, `INSGenerator.java`, `INSGen.java`
- ⬜ Remove INS-specific routes (if any exist)
- ⬜ Archive old test files

---

### 3.3 Critical Decision Points

#### Decision 1: URI Namespace Consistency
**Question**: Should DSG-created VSTOI entities use the same URI patterns as INS?

**Options**:
- **A**: Keep INS patterns (`ahead:INS<hash>`, `ahead:DTC<hash>`)
- **B**: Use StudyObject URI patterns (`http://kb/study/SOC-INSTRUMENT-X/INS-001`)

**Recommendation**: **Option A** - Maintain INS URI patterns for backward compatibility and user expectations.

**Implementation**:
```java
// In StudyObjectGenerator.createInstrumentFromStudyObject()
String instrumentUri = studyObject.getUri(); 
// ✅ If DSG uses pattern: http://kb/ahead/INS1234
// ✅ This automatically matches INS pattern

// Alternative: Generate new URI with INS pattern
String instrumentUri = URIUtils.generateInstrumentUri(); // ahead:INS<timestamp>
```

#### Decision 2: DA-SOC File Naming for VSTOI
**Question**: How to name DA-SOC files for VSTOI-specific properties?

**Options**:
- **A**: Generic naming: `DA-SOC-INSTRUMENT.csv`, `DA-SOC-COMPONENT.csv`
- **B**: Study-specific: `DA-SOC-INSTRUMENT-ARROWHEAD.csv`
- **C**: SOC-specific: `DA-SOC-{SOC-NAME}.csv` (current pattern)

**Recommendation**: **Option C** - Maintain current DA-SOC naming pattern.

**Rationale**:
- Already implemented in `AnnotateDASOC.java`
- Allows multiple instruments in same study to have different DA-SOC files
- Example: Study has 3 instrument SOCs → 3 DA-SOC files

#### Decision 3: Relationship Storage
**Question**: Where to store relationships (hasComponentStem, hasNext, etc.)?

**Options**:
- **A**: In DA-SOC named graph (as extended properties)
- **B**: In main triplestore (as StudyObject properties)
- **C**: In separate VSTOI-specific graph

**Recommendation**: **Option A** - Store in DA-SOC named graph.

**Rationale**:
- Consistent with current DA-SOC architecture
- Allows selective deletion via `DROP GRAPH <daUri>-dasoc`
- Separates base SOC structure from VSTOI-specific semantics

#### Decision 4: INS Conversion Strategy
**Question**: Should we auto-convert uploaded INS files to DSG?

**Options**:
- **A**: Reject INS files immediately
- **B**: Auto-convert INS → DSG + DA-SOC on upload
- **C**: Accept both indefinitely

**Recommendation**: **Option B** with deprecation timeline.

**Implementation**:
```java
// In IngestionWorker.getGeneratorChain()
if (fileName.startsWith("INS-")) {
    dataFile.getLogger().println("⚠️ WARNING: INS format deprecated. Auto-converting to DSG.");
    
    // Convert INS to DSG + DA-SOC
    ConversionResult result = INStoDSGConverter.convert(dataFile);
    
    // Ingest converted files
    ingest(result.dsgFile);
    for (File dasocFile : result.dasocFiles) {
        ingest(dasocFile);
    }
    
    return null; // Conversion handled
}
```

---

## 4. FILE-BY-FILE CHANGE PLAN

### 4.1 Core Ingestion Files

#### File: `app/org/hascoapi/ingestion/StudyObjectGenerator.java`
**Status**: ⚠️ **Partially Modified** (basic VSTOI creation exists)

**Current Functionality**:
- Creates StudyObject from SOC-* worksheet rows
- Detects VSTOI types (Instrument, Component, ComponentStem, ContainerSlot)
- Creates basic VSTOI POJOs with uri, label, comment, typeUri

**Required Changes**:

**Change 1: Expand VSTOI Type Detection**
```java
// CURRENT
private String detectVstoiType(String typeUri) {
    if (VSTOI.INSTRUMENT.equals(typeUri)) return VSTOI.INSTRUMENT;
    if (VSTOI.COMPONENT.equals(typeUri)) return VSTOI.COMPONENT;
    if (typeUri.contains("Detector")) return VSTOI.COMPONENT;
    // ...
}

// TARGET: Add missing types
private String detectVstoiType(String typeUri) {
    // Direct matches
    if (VSTOI.INSTRUMENT.equals(typeUri)) return VSTOI.INSTRUMENT;
    if (VSTOI.COMPONENT.equals(typeUri)) return VSTOI.COMPONENT;
    if (VSTOI.COMPONENT_STEM.equals(typeUri)) return VSTOI.COMPONENT_STEM;
    if (VSTOI.CONTAINER_SLOT.equals(typeUri)) return VSTOI.CONTAINER_SLOT;
    if (VSTOI.CODEBOOK.equals(typeUri)) return VSTOI.CODEBOOK;               // ← NEW
    if (VSTOI.RESPONSE_OPTION.equals(typeUri)) return VSTOI.RESPONSE_OPTION; // ← NEW
    if (VSTOI.ANNOTATION_STEM.equals(typeUri)) return VSTOI.ANNOTATION_STEM; // ← NEW
    
    // Subclass matches
    if (typeUri.contains("Questionnaire")) return VSTOI.INSTRUMENT;
    if (typeUri.contains("Detector")) return VSTOI.COMPONENT;
    if (typeUri.contains("ComponentStem")) return VSTOI.COMPONENT_STEM;
    // ...
}
```

**Change 2: Add Creation Methods for New Types**
```java
// ← NEW METHOD
private void createCodebookFromStudyObject(StudyObject studyObject) {
    Codebook codebook = new Codebook();
    codebook.setUri(studyObject.getUri());
    codebook.setTypeUri(studyObject.getTypeUri());
    codebook.setHascoTypeUri(VSTOI.CODEBOOK);
    codebook.setLabel(studyObject.getLabel());
    codebook.setComment(studyObject.getComment());
    codebook.setNamedGraph(studyObject.getNamedGraph());
    codebook.setHasSIRManagerEmail(dataFile.getHasSIRManagerEmail());
    codebook.save();
}

// ← NEW METHOD
private void createResponseOptionFromStudyObject(StudyObject studyObject) {
    ResponseOption responseOption = new ResponseOption();
    responseOption.setUri(studyObject.getUri());
    responseOption.setTypeUri(studyObject.getTypeUri());
    responseOption.setHascoTypeUri(VSTOI.RESPONSE_OPTION);
    responseOption.setLabel(studyObject.getLabel());
    responseOption.setComment(studyObject.getComment());
    responseOption.setNamedGraph(studyObject.getNamedGraph());
    responseOption.setHasSIRManagerEmail(dataFile.getHasSIRManagerEmail());
    responseOption.save();
}

// ← NEW METHOD
private void createAnnotationStemFromStudyObject(StudyObject studyObject) {
    AnnotationStem annotationStem = new AnnotationStem();
    annotationStem.setUri(studyObject.getUri());
    annotationStem.setTypeUri(studyObject.getTypeUri());
    annotationStem.setHascoTypeUri(VSTOI.ANNOTATION_STEM);
    annotationStem.setLabel(studyObject.getLabel());
    annotationStem.setComment(studyObject.getComment());
    annotationStem.setNamedGraph(studyObject.getNamedGraph());
    annotationStem.setHasSIRManagerEmail(dataFile.getHasSIRManagerEmail());
    annotationStem.save();
}
```

**Change 3: Update Dispatcher**
```java
private void createVstoiEntityIfApplicable(StudyObject studyObject) {
    String vstoiType = detectVstoiType(studyObject.getTypeUri());
    if (vstoiType == null) return;
    
    try {
        if (VSTOI.INSTRUMENT.equals(vstoiType)) {
            createInstrumentFromStudyObject(studyObject);
        } else if (VSTOI.COMPONENT.equals(vstoiType)) {
            createComponentFromStudyObject(studyObject);
        } else if (VSTOI.COMPONENT_STEM.equals(vstoiType)) {
            createComponentStemFromStudyObject(studyObject);
        } else if (VSTOI.CONTAINER_SLOT.equals(vstoiType)) {
            createContainerSlotFromStudyObject(studyObject);
        } else if (VSTOI.CODEBOOK.equals(vstoiType)) {              // ← NEW
            createCodebookFromStudyObject(studyObject);
        } else if (VSTOI.RESPONSE_OPTION.equals(vstoiType)) {       // ← NEW
            createResponseOptionFromStudyObject(studyObject);
        } else if (VSTOI.ANNOTATION_STEM.equals(vstoiType)) {       // ← NEW
            createAnnotationStemFromStudyObject(studyObject);
        }
    } catch (Exception e) {
        dataFile.getLogger().printException("Error creating VSTOI entity: " + e.getMessage());
    }
}
```

---

#### File: `app/org/hascoapi/ingestion/AnnotateDASOC.java`
**Status**: ⚠️ **Partially Modified** (basic enrichment exists)

**Current Functionality**:
- Enriches StudyObject instances with extra properties
- Has `enrichVstoiEntity()` method that updates Instrument/Component/ComponentStem/ContainerSlot POJOs

**Required Changes**:

**Change 1: Expand Property Mapping**
```java
// CURRENT: enrichInstrument() has limited properties
private static void enrichInstrument(String objectUri, Map<String, String> properties, DataFile dataFile) {
    Instrument instrument = Instrument.find(objectUri);
    if (instrument == null) return;
    
    for (Map.Entry<String, String> entry : properties.entrySet()) {
        String predicate = entry.getKey();
        String value = entry.getValue();
        
        if (predicate.equals(VSTOI.HAS_SHORT_NAME)) {
            instrument.setHasShortName(value);
        } else if (predicate.equals(VSTOI.HAS_LANGUAGE)) {
            instrument.setHasLanguage(value);
        }
        // ... limited set
    }
    instrument.save();
}

// TARGET: Add comprehensive property support
private static void enrichInstrument(String objectUri, Map<String, String> properties, DataFile dataFile) {
    Instrument instrument = Instrument.find(objectUri);
    if (instrument == null) return;
    
    for (Map.Entry<String, String> entry : properties.entrySet()) {
        String predicate = entry.getKey();
        String value = entry.getValue();
        
        // Basic metadata
        if (predicate.equals(VSTOI.HAS_SHORT_NAME)) {
            instrument.setHasShortName(value);
        } else if (predicate.equals(VSTOI.HAS_LANGUAGE)) {
            instrument.setHasLanguage(value);
        } else if (predicate.equals(VSTOI.HAS_VERSION)) {
            instrument.setHasVersion(value);
        } else if (predicate.equals(VSTOI.HAS_STATUS)) {
            instrument.setHasStatus(value);
        } else if (predicate.equals(RDFS.COMMENT)) {
            instrument.setComment(value);
        }
        
        // Structural relationships
        else if (predicate.equals(VSTOI.HAS_FIRST)) {                    // ← NEW
            instrument.setHasFirst(URIUtils.replacePrefixEx(value));
        } else if (predicate.equals(HASCO.HAS_IMAGE)) {                  // ← NEW
            instrument.setHasImageUri(value);
        } else if (predicate.equals(HASCO.HAS_WEB_DOCUMENT)) {           // ← NEW
            instrument.setHasWebDocument(value);
        } else if (predicate.equals(VSTOI.HAS_MAKER)) {                  // ← NEW
            instrument.setHasMakerUri(value);
        }
        
        // Operational parameters (from INS spec)
        else if (predicate.equals(VSTOI.MIN_OPERATING_TEMPERATURE)) {    // ← NEW
            instrument.setMinOperatingTemperature(value);
        } else if (predicate.equals(VSTOI.MAX_OPERATING_TEMPERATURE)) {  // ← NEW
            instrument.setMaxOperatingTemperature(value);
        } else if (predicate.equals(VSTOI.MAX_LOGGED_MEASUREMENTS)) {    // ← NEW
            instrument.setMaxLoggedMeasurements(value);
        }
    }
    
    instrument.save();
}
```

**Change 2: Add Enrichment for New Types**
```java
// ← NEW METHOD
private static void enrichCodebook(String objectUri, Map<String, String> properties, DataFile dataFile) {
    Codebook codebook = Codebook.find(objectUri);
    if (codebook == null) return;
    
    for (Map.Entry<String, String> entry : properties.entrySet()) {
        String predicate = entry.getKey();
        String value = entry.getValue();
        
        if (predicate.equals(VSTOI.HAS_CONTENT)) {
            codebook.setHasContent(value);
        } else if (predicate.equals(VSTOI.HAS_LANGUAGE)) {
            codebook.setHasLanguage(value);
        } else if (predicate.equals(VSTOI.HAS_VERSION)) {
            codebook.setHasVersion(value);
        } else if (predicate.equals(VSTOI.HAS_FIRST)) {  // First CodebookSlot
            codebook.setHasFirst(URIUtils.replacePrefixEx(value));
        }
    }
    
    codebook.save();
}

// ← NEW METHOD
private static void enrichResponseOption(String objectUri, Map<String, String> properties, DataFile dataFile) {
    ResponseOption responseOption = ResponseOption.find(objectUri);
    if (responseOption == null) return;
    
    for (Map.Entry<String, String> entry : properties.entrySet()) {
        String predicate = entry.getKey();
        String value = entry.getValue();
        
        if (predicate.equals(VSTOI.HAS_CONTENT)) {
            responseOption.setHasContent(value);
        } else if (predicate.equals(VSTOI.HAS_LANGUAGE)) {
            responseOption.setHasLanguage(value);
        }
    }
    
    responseOption.save();
}
```

**Change 3: Update Dispatcher**
```java
private static void enrichVstoiEntity(String objectUri, Map<String, String> properties, DataFile dataFile) {
    try {
        String vstoiType = detectVstoiTypeFromTriplestore(objectUri);
        
        if (VSTOI.INSTRUMENT.equals(vstoiType)) {
            enrichInstrument(objectUri, properties, dataFile);
        } else if (VSTOI.COMPONENT.equals(vstoiType)) {
            enrichComponent(objectUri, properties, dataFile);
        } else if (VSTOI.COMPONENT_STEM.equals(vstoiType)) {
            enrichComponentStem(objectUri, properties, dataFile);
        } else if (VSTOI.CONTAINER_SLOT.equals(vstoiType)) {
            enrichContainerSlot(objectUri, properties, dataFile);
        } else if (VSTOI.CODEBOOK.equals(vstoiType)) {              // ← NEW
            enrichCodebook(objectUri, properties, dataFile);
        } else if (VSTOI.RESPONSE_OPTION.equals(vstoiType)) {       // ← NEW
            enrichResponseOption(objectUri, properties, dataFile);
        } else if (VSTOI.ANNOTATION_STEM.equals(vstoiType)) {       // ← NEW
            enrichAnnotationStem(objectUri, properties, dataFile);
        }
    } catch (Exception e) {
        // Silently skip - entity may not exist yet
    }
}
```

---

### 4.2 POJO Entity Files

#### File: `app/org/hascoapi/entity/pojo/Instrument.java`
**Status**: ✅ **No Changes Required**

**Rationale**: 
- POJO already has all required fields
- `find()` method already uses SPARQL SELECT to retrieve properties
- JSON serialization already configured via `@JsonFilter`

**Verification Needed**:
- Ensure `find()` queries both main graph and DA-SOC named graphs
- Test JSON output matches INS-sourced instruments

**Potential Enhancement**:
```java
// Current find() method queries single graph
public static Instrument find(String uri) {
    String queryString = "SELECT DISTINCT ?graph ?p ?o WHERE { GRAPH ?graph { <" + uri + "> ?p ?o } }";
    // ...
}

// Enhanced find() to include DA-SOC graphs
public static Instrument find(String uri) {
    // Query 1: Main graph (from StudyObject/DSG)
    String queryMain = "SELECT ?p ?o WHERE { <" + uri + "> ?p ?o }";
    
    // Query 2: DA-SOC graphs (extended properties)
    String queryDaSOC = "SELECT ?p ?o ?graph WHERE { " +
        "GRAPH ?graph { <" + uri + "> ?p ?o } " +
        "FILTER(CONTAINS(STR(?graph), '-dasoc')) " +
    "}";
    
    // Merge results
    // ...
}
```

**Decision**: ⬜ **Defer enhancement** - Current `find()` should work if DA-SOC triples are in default graph.

---

#### Files: `app/org/hascoapi/entity/pojo/Component.java`, `ComponentStem.java`, `ContainerSlot.java`, `Codebook.java`, `ResponseOption.java`, `AnnotationStem.java`
**Status**: ✅ **No Changes Required** (same rationale as Instrument.java)

---

### 4.3 Export/Generation Files

#### File: `app/org/hascoapi/transform/mt/dsg/DSGGen.java`
**Status**: ⬜ **Major Modifications Required**

**Current Functionality**:
- Generates DSG Excel files from Study entities
- Exports SOCs and StudyObjects
- Has `generateDASOCsForStudies()` method (creates DA-SOC CSVs for existing SOCs)

**Required Changes**:

**Change 1: VSTOI-Aware SOC Export**
```java
// CURRENT: Exports all SOCs generically
private static DSGGenHelper addSOCs(DSGGenHelper helper, Study study) {
    List<StudyObjectCollection> socs = StudyObjectCollection.findByStudyUri(study.getUri());
    
    for (StudyObjectCollection soc : socs) {
        helper = DSGSSD.add(helper, soc);  // Adds to SSD sheet
        helper = DSGSOC.add(helper, soc);  // Creates SOC-* worksheet
    }
}

// TARGET: Detect VSTOI SOCs and export with extended properties
private static DSGGenHelper addSOCs(DSGGenHelper helper, Study study) {
    List<StudyObjectCollection> socs = StudyObjectCollection.findByStudyUri(study.getUri());
    
    for (StudyObjectCollection soc : socs) {
        // Check if SOC contains VSTOI entities
        boolean isVstoiSOC = detectVstoiSOC(soc);
        
        if (isVstoiSOC) {
            // Export as VSTOI-typed SOC with base properties only
            helper = DSGSSD.add(helper, soc);
            helper = DSGSOCVstoi.add(helper, soc);  // ← NEW: VSTOI-aware export
        } else {
            // Export as regular SOC
            helper = DSGSSD.add(helper, soc);
            helper = DSGSOC.add(helper, soc);
        }
    }
}

// ← NEW HELPER METHOD
private static boolean detectVstoiSOC(StudyObjectCollection soc) {
    // Query first StudyObject in SOC to check typeUri
    String query = "SELECT ?type WHERE { " +
        "?obj hasco:isMemberOf <" + soc.getUri() + "> . " +
        "?obj rdf:type ?type . " +
    "} LIMIT 1";
    
    ResultSet results = SPARQLUtils.select(..., query);
    if (results.hasNext()) {
        String typeUri = results.next().get("type").toString();
        return isVstoiType(typeUri);
    }
    return false;
}
```

**Change 2: Enhanced DA-SOC Generation**
```java
// CURRENT: Exports DA-SOC for any SOC with additional properties
private static String generateDASOCsForStudies(List<Study> studies, String dsgFilename) {
    for (Study study : studies) {
        // Query SOCs
        String queryString = "SELECT DISTINCT ?socUri ?socLabel WHERE { " +
            "?obj hasco:isMemberOf ?socUri . " +
            "?socUri a hasco:StudyObjectCollection . " +
        "}";
        
        // For each SOC, check if StudyObjects have extra properties
        // If yes, export DA-SOC-<SOCNAME>.csv
    }
}

// TARGET: Export VSTOI-specific DA-SOC files with relationship properties
private static String generateDASOCsForStudies(List<Study> studies, String dsgFilename) {
    for (Study study : studies) {
        List<StudyObjectCollection> socs = StudyObjectCollection.findByStudyUri(study.getUri());
        
        for (StudyObjectCollection soc : socs) {
            String vstoiType = detectSOCVstoiType(soc);
            
            if (vstoiType != null) {
                // Export VSTOI-specific DA-SOC with extended properties
                if (VSTOI.INSTRUMENT.equals(vstoiType)) {
                    exportInstrumentDASOC(soc, dsgFilename);
                } else if (VSTOI.COMPONENT.equals(vstoiType)) {
                    exportComponentDASOC(soc, dsgFilename);
                } else if (VSTOI.CONTAINER_SLOT.equals(vstoiType)) {
                    exportContainerSlotDASOC(soc, dsgFilename);
                }
                // ... other types
            } else {
                // Export generic DA-SOC if extra properties exist
                exportGenericDASOC(soc, dsgFilename);
            }
        }
    }
}

// ← NEW HELPER METHOD
private static void exportInstrumentDASOC(StudyObjectCollection soc, String dsgFilename) {
    String socName = extractSOCNameFromURI(soc.getUri());
    String filename = "DA-SOC-" + socName + ".csv";
    
    // Query Instrument-specific properties
    String query = "SELECT ?originalId ?hasFirst ?hasShortName ?hasLanguage ?hasVersion " +
        "WHERE { " +
        "  ?obj hasco:isMemberOf <" + soc.getUri() + "> . " +
        "  ?obj hasco:originalID ?originalId . " +
        "  ?instrument a vstoi:Instrument ; hasco:originalID ?originalId . " +
        "  OPTIONAL { ?instrument vstoi:hasFirst ?hasFirst } " +
        "  OPTIONAL { ?instrument vstoi:hasShortName ?hasShortName } " +
        "  OPTIONAL { ?instrument vstoi:hasLanguage ?hasLanguage } " +
        "  OPTIONAL { ?instrument vstoi:hasVersion ?hasVersion } " +
        "}";
    
    // Write to CSV
    ResultSet results = SPARQLUtils.select(..., query);
    writeDASOCFile(filename, results, Arrays.asList(
        "originalID", "vstoi:hasFirst", "vstoi:hasShortName", "vstoi:hasLanguage", "vstoi:hasVersion"
    ));
}
```

---

#### File: `app/org/hascoapi/transform/mt/dsg/DSGSOC.java`
**Status**: ⬜ **Minor Modifications**

**Required Changes**:
- Add comment indicating VSTOI properties should be exported via DA-SOC
- Optionally: Create separate `DSGSOCVstoi.java` for VSTOI-specific SOC export

```java
// ← NEW FILE: DSGSOCVstoi.java
public class DSGSOCVstoi {
    /**
     * Exports VSTOI-typed SOC worksheet with base properties only.
     * Extended VSTOI properties (hasFirst, hasComponentStem, etc.) are
     * exported via DA-SOC files generated by DSGGen.generateDASOCsForStudies().
     */
    public static DSGGenHelper add(DSGGenHelper helper, StudyObjectCollection soc) {
        // Similar to DSGSOC.add(), but only exports:
        // - originalID
        // - rdf:type
        // - rdfs:label
        // - rdfs:comment
        // (no vstoi:* properties)
    }
}
```

---

### 4.4 Ingestion Routing Files

#### File: `app/org/hascoapi/ingestion/IngestionWorker.java`
**Status**: ⬜ **Minor Modifications**

**Current INS Detection**:
```java
public static GeneratorChain getGeneratorChain(...) {
    String fileName = FilenameUtils.getBaseName(dataFile.getFilename());
    
    if (fileName.startsWith("INS-")) {
        chain = AnnotateINS.exec(dataFile, templateFile, status);
    } else if (fileName.startsWith("DSG-")) {
        chain = AnnotateDSG.exec(dataFile, templateFile, status);
    }
    // ...
}
```

**Target (Deprecation with Auto-Conversion)**:
```java
public static GeneratorChain getGeneratorChain(...) {
    String fileName = FilenameUtils.getBaseName(dataFile.getFilename());
    
    if (fileName.startsWith("INS-")) {
        // ⚠️ DEPRECATED: Auto-convert to DSG
        dataFile.getLogger().println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        dataFile.getLogger().println("⚠️  WARNING: INS format deprecated");
        dataFile.getLogger().println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        dataFile.getLogger().println("Converting to DSG + DA-SOC format...");
        
        try {
            // Convert INS to DSG + DA-SOC files
            INSConverter.ConversionResult conversion = INSConverter.convert(dataFile);
            
            // Ingest converted DSG
            DataFile dsgDataFile = new DataFile(...);
            dsgDataFile.setFile(conversion.dsgFile);
            chain = AnnotateDSG.exec(dsgDataFile, templateFile, status);
            
            // Queue DA-SOC files for ingestion
            for (File dasocFile : conversion.dasocFiles) {
                DataFile dasocDataFile = new DataFile(...);
                dasocDataFile.setFile(dasocFile);
                // Ingest in separate call or add to chain
            }
            
            dataFile.getLogger().println("✅ Conversion successful");
            return chain;
            
        } catch (Exception e) {
            dataFile.getLogger().printException("INS conversion failed: " + e.getMessage());
            dataFile.getLogger().println("Attempting direct INS ingestion (deprecated)...");
            chain = AnnotateINS.exec(dataFile, templateFile, status);  // Fallback
        }
        
    } else if (fileName.startsWith("DSG-")) {
        chain = AnnotateDSG.exec(dataFile, templateFile, status);
    }
    // ...
}
```

---

#### File: `app/org/hascoapi/console/controllers/restapi/IngestionAPI.java`
**Status**: ✅ **No Changes Required**

**Rationale**:
- Route handling is generic: `/hascoapi/api/ingest/:status/:elementType/:elementUri`
- File type detection happens in `IngestionWorker.getGeneratorChain()`
- No INS-specific endpoints exist

---

### 4.5 Query/Retrieval Files

#### File: `app/org/hascoapi/entity/pojo/GenericFind.java`
**Status**: ⚠️ **Minor Verification Required**

**Current Functionality**:
- `findInstancesWithPages(Instrument.class, ...)` queries for Instrument POJOs
- Uses SPARQL query to find entities by class type

**Required Verification**:
- Ensure query matches both INS-sourced and DSG-sourced Instruments
- Test pagination with mixed-source data

**Potential Issue**:
```java
// Current query pattern
String queryString = 
    "SELECT ?uri WHERE { " +
    "  ?uri a vstoi:Instrument . " +
    "  ?uri hasco:hascoType vstoi:Instrument . " +
    "}";
```

**Solution**:
- DSG-created Instruments already have `hasco:hascoType vstoi:Instrument` (set in `StudyObjectGenerator.createInstrumentFromStudyObject()`)
- No query changes needed

---

#### File: `app/org/hascoapi/console/controllers/restapi/InstrumentAPI.java`
**Status**: ✅ **No Changes Required**

**Endpoints**:
- `GET /hascoapi/api/instrument/components/:instrumentUri` → `retrieveInstrumentComponents()`
- `GET /hascoapi/api/instrument/containerslots/:instrumentUri` → `retrieveInstrumentContainerSlots()`

**Current Implementation**:
```java
public Result retrieveInstrumentComponents(String uri) {
    Instrument instr = Instrument.find(uri);
    List<String> components = InstrumentTraversal.retrieveInstrumentComponents(uri);
    // ...
}
```

**Verification**:
- `InstrumentTraversal.retrieveInstrumentComponents()` uses SPARQL to query `vstoi:hasComponent` relationships
- Relationships stored in DA-SOC named graph should be queryable
- Test with DSG-created Instruments

---

### 4.6 Converter (New Component)

#### File: `app/org/hascoapi/utils/INSConverter.java` ← **NEW**
**Status**: ⬜ **Create New File**

**Purpose**: Convert uploaded INS files to DSG + DA-SOC format

**Interface**:
```java
public class INSConverter {
    
    public static class ConversionResult {
        public File dsgFile;
        public List<File> dasocFiles;
        public String studyUri;
        public List<String> warnings;
    }
    
    /**
     * Convert INS file to DSG + DA-SOC files
     * 
     * @param insDataFile Original INS DataFile
     * @return Conversion result with DSG and DA-SOC files
     */
    public static ConversionResult convert(DataFile insDataFile) throws Exception {
        // Parse INS file
        INSData insData = parseINS(insDataFile.getFile());
        
        // Create Study from INS metadata
        Study study = createStudyFromINS(insData);
        
        // Create SOCs for each VSTOI type
        List<StudyObjectCollection> socs = createSOCsFromINS(insData, study);
        
        // Generate DSG file
        File dsgFile = generateDSGFile(study, socs, insData);
        
        // Generate DA-SOC files for extended properties
        List<File> dasocFiles = generateDASOCFiles(socs, insData);
        
        return new ConversionResult(dsgFile, dasocFiles, study.getUri());
    }
    
    // Helper methods
    private static INSData parseINS(File insFile) { ... }
    private static Study createStudyFromINS(INSData data) { ... }
    private static List<StudyObjectCollection> createSOCsFromINS(INSData data, Study study) { ... }
    private static File generateDSGFile(Study study, List<StudyObjectCollection> socs, INSData data) { ... }
    private static List<File> generateDASOCFiles(List<StudyObjectCollection> socs, INSData data) { ... }
}
```

**Example Conversion Flow**:
```
INS File: INS-ARROWHEAD-V4.xlsx
├── Instruments sheet (1 row)
├── SlotElements sheet (6 rows)
├── ComponentStems sheet (6 rows)
├── Components sheet (6 rows)

↓ CONVERT ↓

DSG File: DSG-ARROWHEAD-V4.xlsx
├── InfoSheet (hasStudyURI = arrowhead:STUDY-ARROWHEAD-V4)
├── Namespaces
├── SSD
│   ├── Row: SOC-INSTRUMENT-ARROWHEAD, hasco:InstrumentCollection
│   ├── Row: SOC-SLOT-ELEMENT-ARROWHEAD, hasco:ContainerSlotCollection
│   ├── Row: SOC-COMPONENT-STEM-ARROWHEAD, hasco:ComponentStemCollection
│   └── Row: SOC-COMPONENT-ARROWHEAD, hasco:ComponentCollection
├── STD (Study metadata)
├── VD (empty)
├── SOC-INSTRUMENT-ARROWHEAD
│   └── Row: originalID=INS-001, rdf:type=vstoi:Instrument, rdfs:label="Arrowhead V4"
├── SOC-SLOT-ELEMENT-ARROWHEAD
│   ├── Row: originalID=CTS-0001, rdf:type=vstoi:ContainerSlot
│   └── ... (5 more rows)
├── SOC-COMPONENT-STEM-ARROWHEAD
│   └── ... (6 rows)
└── SOC-COMPONENT-ARROWHEAD
    └── ... (6 rows)

DA-SOC Files:
├── DA-SOC-INSTRUMENT-ARROWHEAD.csv
│   └── originalID,vstoi:hasFirst,vstoi:hasShortName,vstoi:hasLanguage
│       INS-001,ahead:INS-001/CTS/0001,ARROW-V4,en
│
├── DA-SOC-SLOT-ELEMENT-ARROWHEAD.csv
│   └── originalID,vstoi:belongsTo,vstoi:hasComponent,vstoi:hasNext,vstoi:hasPrevious
│       CTS-0001,ahead:INS-001,ahead:DTC-001,ahead:INS-001/CTS/0002,
│       CTS-0002,ahead:INS-001,ahead:DTC-002,ahead:INS-001/CTS/0003,ahead:INS-001/CTS/0001
│
├── DA-SOC-COMPONENT-ARROWHEAD.csv
│   └── originalID,vstoi:hasComponentStem,vstoi:hasCodebook
│       DTC-001,ahead:DSM-001,
│
└── DA-SOC-COMPONENT-STEM-ARROWHEAD.csv
    └── originalID,vstoi:hasContent,vstoi:hasLanguage,vstoi:hasVersion
        DSM-001,"Material Weight Sensor",en,1.0
```

---

## 5. API ENDPOINT MAPPING

### 5.1 Endpoints That Will Continue Working Unchanged

| Endpoint Pattern | Controller Method | Data Source Change | Test Required |
|------------------|-------------------|-------------------|---------------|
| `GET /hascoapi/api/:elementType/elements/:pageSize/:offset` | `SIRElementAPI.getElementsWithPage()` | INS → DSG+DA-SOC | ✅ Yes |
| `POST /hascoapi/api/:elementType/create/:json` | `SIRElementAPI.createElement()` | Direct POJO save (no change) | ✅ Yes |
| `POST /hascoapi/api/:elementType/delete/:uri` | `SIRElementAPI.deleteElement()` | Direct POJO delete (no change) | ✅ Yes |
| `GET /hascoapi/api/instrument/components/:instrumentUri` | `InstrumentAPI.retrieveInstrumentComponents()` | Queries `vstoi:hasComponent` (DA-SOC graph) | ✅ Yes |
| `GET /hascoapi/api/instrument/containerslots/:instrumentUri` | `InstrumentAPI.retrieveInstrumentContainerSlots()` | Queries `vstoi:hasContainerSlot` (DA-SOC graph) | ✅ Yes |
| `GET /hascoapi/api/instrument/totext/plain/:uri` | `InstrumentAPI.toTextPlain()` | Uses `Instrument.find()` (POJO) | ⚠️ Verify |
| `GET /hascoapi/api/instrument/tordf/:uri` | `InstrumentAPI.toRDF()` | Serializes POJO to RDF | ⚠️ Verify |
| `GET /hascoapi/api/instrument/tofhir/:uri` | `InstrumentAPI.toFHIR()` | Converts POJO to FHIR | ⚠️ Verify |

### 5.2 Endpoints That Require Testing

**Priority 1: Core Retrieval**
```bash
# Test: Get all instruments (mixed INS + DSG sources)
curl http://localhost:9000/hascoapi/api/instrument/elements/10/0

# Expected: JSON array with instruments from both sources
# Verify: 
#  - All required fields present (uri, label, hasShortName, hasFirst, etc.)
#  - No duplicate entries
#  - Pagination works correctly
```

**Priority 2: Relationship Queries**
```bash
# Test: Get components of a DSG-created instrument
curl http://localhost:9000/hascoapi/api/instrument/components/http%3A%2F%2Fkb%2FINS-001

# Expected: Array of component URIs
# Verify: Relationships from DA-SOC graph are retrieved
```

**Priority 3: Export/Generation**
```bash
# Test: Generate DSG from study containing VSTOI entities
curl -X POST http://localhost:9000/hascoapi/api/mt/gen/perelement/dsg/...?generateDASOCs=true

# Expected: DSG file + DA-SOC files created
# Verify: 
#  - SOCs created for each VSTOI type
#  - DA-SOC files contain relationship properties
#  - Round-trip: ingest generated files → same data structure
```

---

## 6. DATA MIGRATION STRATEGY

### 6.1 Migration Scenarios

**Scenario A: Fresh Installation**
- No existing INS data
- ✅ Use DSG + DA-SOC exclusively
- No migration needed

**Scenario B: Existing INS Data + New DSG Workflow**
- Existing INS-created instruments in triplestore
- New instruments via DSG
- ✅ Coexistence supported (both work via same API endpoints)
- No migration required (gradual transition)

**Scenario C: Convert All Existing INS Data to DSG**
- Want to eliminate INS entirely
- Need to migrate historical data
- ⬜ **Migration script required**

### 6.2 Migration Script Design

**File**: `scripts/migrate-ins-to-dsg.sh` ← **NEW**

```bash
#!/bin/bash
# Migrate existing INS data to DSG + DA-SOC format

# Step 1: Export all INS-created entities to INS files
curl -X POST http://localhost:9000/hascoapi/api/mt/gen/perstatus/ins/.../DRAFT/...

# Step 2: For each generated INS file, convert to DSG
for insFile in /tmp/hascoapi/generated/INS-*.xlsx; do
    echo "Converting $insFile..."
    
    # Upload INS file (auto-converts to DSG via IngestionWorker)
    curl -X POST \
        -F "file=@$insFile" \
        http://localhost:9000/hascoapi/api/ingest/DRAFT/ins/...
done

# Step 3: Verify migration
curl http://localhost:9000/hascoapi/api/instrument/elements/1000/0 > instruments-after.json
diff instruments-before.json instruments-after.json

# Step 4: Delete old INS-sourced triples (if desired)
# WARNING: Only do this after verification
# curl http://localhost:9000/hascoapi/api/cleanup/ins-sourced-data
```

### 6.3 Rollback Strategy

**If migration fails:**
1. Re-ingest original INS files
2. Delete DSG-created entities via:
   ```sparql
   DELETE WHERE {
     GRAPH ?g {
       ?s ?p ?o .
       FILTER(CONTAINS(STR(?g), "DSG-STUDY-NAME"))
     }
   }
   ```
3. Revert code changes via git

---

## 7. TESTING STRATEGY

### 7.1 Unit Tests

**Test Suite**: `test/org/hascoapi/tests/INStoDSGTransformationTest.java` ← **NEW**

```java
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class INStoDSGTransformationTest {
    
    @Test
    @Order(1)
    public void test01_IngestDSGWithInstrumentSOC() {
        // Ingest DSG with SOC-INSTRUMENT-TEST
        // Verify Instrument POJO created
    }
    
    @Test
    @Order(2)
    public void test02_IngestDASOCWithInstrumentProperties() {
        // Ingest DA-SOC-INSTRUMENT-TEST.csv
        // Verify Instrument POJO enriched with hasFirst, hasShortName
    }
    
    @Test
    @Order(3)
    public void test03_RetrieveInstrumentViaAPI() {
        // GET /hascoapi/api/instrument/elements/1/0
        // Verify JSON contains all expected fields
    }
    
    @Test
    @Order(4)
    public void test04_ExportDSGWithInstruments() {
        // Generate DSG from study
        // Verify DSG contains SOC-INSTRUMENT-*
        // Verify DA-SOC files generated
    }
    
    @Test
    @Order(5)
    public void test05_RoundTripIntegrity() {
        // Ingest DSG + DA-SOC → Export DSG + DA-SOC → Re-ingest
        // Verify no data loss
    }
    
    @Test
    @Order(6)
    public void test06_INSConversion() {
        // Upload INS file
        // Verify auto-conversion to DSG + DA-SOC
        // Verify same instrument data accessible via API
    }
}
```

### 7.2 Integration Tests

**Test Matrix**:

| Test Case | INS File | DSG File | Expected Outcome |
|-----------|----------|----------|------------------|
| T1: Pure INS | INS-EXAMPLE.xlsx | - | ✅ Instruments created via AnnotateINS (deprecated path) |
| T2: Pure DSG | - | DSG-STUDY.xlsx + DA-SOC-INSTRUMENT.csv | ✅ Instruments created via DSG + enriched via DA-SOC |
| T3: Mixed Data | INS-OLD.xlsx (existing) | DSG-NEW.xlsx (new) | ✅ Both accessible via same API |
| T4: INS Auto-Convert | INS-NEW.xlsx | - | ✅ Auto-converted to DSG + DA-SOC, then ingested |
| T5: Round-Trip | - | DSG export → Re-ingest | ✅ Identical data structure |

### 7.3 Performance Tests

**Metrics to Track**:
- Query response time: `GET /hascoapi/api/instrument/elements/100/0`
  - **Baseline (INS)**: ?ms
  - **Target (DSG+DA-SOC)**: ≤ baseline
- Ingestion time: INS file vs DSG+DA-SOC files
  - **Baseline (INS)**: ?seconds
  - **Target (DSG+DA-SOC)**: ≤ 1.5x baseline

**Test Dataset**:
- 1000 instruments
- 5000 components
- 500 componentstems
- 10000 containerslots

---

## 8. RISKS AND MITIGATION

### 8.1 Risk Matrix

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| **R1**: API response format changes break frontend | Medium | High | - Comprehensive integration tests<br>- JSON schema validation<br>- Gradual rollout |
| **R2**: DA-SOC graph queries slow | Low | Medium | - Index SPARQL predicates<br>- Cache frequently accessed relationships |
| **R3**: INS conversion fails for edge cases | Medium | Medium | - Extensive conversion testing<br>- Fallback to manual conversion<br>- Detailed error logging |
| **R4**: Mixed INS+DSG data causes duplicates | Low | High | - URI consistency enforcement<br>- Deduplication queries<br>- Clear migration guide |
| **R5**: Users continue uploading INS files | High | Low | - Clear deprecation warnings<br>- Auto-conversion feature<br>- Documentation updates |
| **R6**: Data loss during migration | Low | Critical | - Mandatory backup before migration<br>- Rollback script tested<br>- Dry-run mode |

### 8.2 Mitigation Details

**R1 Mitigation: API Compatibility Testing**
```java
// Create API contract test
@Test
public void testInstrumentAPIResponseSchema() {
    // Ingest INS-created instrument
    Instrument insInstrument = ...;
    String insResponse = GET("/api/instrument/elements/1/0");
    
    // Ingest DSG-created instrument
    Instrument dsgInstrument = ...;
    String dsgResponse = GET("/api/instrument/elements/1/0");
    
    // Verify schema match
    assertJsonSchemaMatch(insResponse, dsgResponse);
}
```

**R2 Mitigation: Query Optimization**
```sparql
# Current: Queries DA-SOC graph separately
SELECT ?hasFirst WHERE {
  GRAPH ?g {
    <instrumentUri> vstoi:hasFirst ?hasFirst .
    FILTER(CONTAINS(STR(?g), "-dasoc"))
  }
}

# Optimized: Union query
SELECT ?hasFirst WHERE {
  {
    <instrumentUri> vstoi:hasFirst ?hasFirst .
  } UNION {
    GRAPH ?g {
      <instrumentUri> vstoi:hasFirst ?hasFirst .
    }
  }
}
```

**R6 Mitigation: Backup Script**
```bash
# Pre-migration backup
curl http://localhost:3030/store/data \
    -X POST \
    --data-urlencode 'query=CONSTRUCT WHERE { ?s ?p ?o }' \
    -H 'Accept: application/n-triples' \
    > backup-$(date +%Y%m%d).nt
```

---

## 9. IMPLEMENTATION PHASES

### Phase 1: Foundation (Week 1-2)
**Goal**: Extend DSG ingestion to support all VSTOI types

**Tasks**:
- [ ] 1.1: Update `StudyObjectGenerator.java` - Add Codebook, ResponseOption, AnnotationStem detection
- [ ] 1.2: Update `AnnotateDASOC.java` - Add enrichment methods for new types
- [ ] 1.3: Create unit tests for VSTOI entity creation
- [ ] 1.4: Test DSG ingestion with all VSTOI types

**Deliverables**:
- ✅ All 7 VSTOI types auto-created from DSG
- ✅ All tests pass

**Validation**:
```bash
# Upload DSG with all VSTOI types
curl -F "file=@DSG-COMPREHENSIVE.xlsx" http://localhost:9000/hascoapi/api/ingest/DRAFT/dsg/...

# Verify entities created
curl http://localhost:9000/hascoapi/api/instrument/elements/10/0   # Instruments
curl http://localhost:9000/hascoapi/api/component/elements/10/0    # Components
curl http://localhost:9000/hascoapi/api/codebook/elements/10/0     # Codebooks
```

---

### Phase 2: Enrichment (Week 3-4)
**Goal**: Support comprehensive property enrichment via DA-SOC

**Tasks**:
- [ ] 2.1: Expand `AnnotateDASOC.enrichInstrument()` - Add all INS properties
- [ ] 2.2: Add relationship validation (hasFirst → ContainerSlot URI must exist)
- [ ] 2.3: Create DA-SOC templates for each VSTOI type
- [ ] 2.4: Test enrichment round-trip (DSG → enrich → export → re-ingest)

**Deliverables**:
- ✅ All INS properties supported via DA-SOC
- ✅ Relationship integrity validated
- ✅ Export/import tested

**Validation**:
```bash
# Upload DA-SOC with relationships
curl -F "file=@DA-SOC-INSTRUMENT-TEST.csv" http://localhost:9000/hascoapi/api/ingest/DRAFT/da/...

# Verify enrichment
curl http://localhost:9000/hascoapi/api/:elementType/elements/1/0 | jq '.body[0].hasFirst'
# Expected: "http://kb/INS-001/CTS/0001"
```

---

### Phase 3: Export Enhancement (Week 5)
**Goal**: Generate DSG + DA-SOC files from existing instruments

**Tasks**:
- [ ] 3.1: Update `DSGGen.java` - Add VSTOI SOC detection
- [ ] 3.2: Create `DSGSOCVstoi.java` - Export VSTOI-typed SOCs
- [ ] 3.3: Update `DSGGen.generateDASOCsForStudies()` - Export VSTOI-specific DA-SOCs
- [ ] 3.4: Test export with existing INS-created instruments

**Deliverables**:
- ✅ DSG export generates VSTOI-typed SOCs
- ✅ DA-SOC files generated with relationship properties
- ✅ Round-trip tested (export → re-ingest → identical data)

**Validation**:
```bash
# Export DSG from study with instruments
curl http://localhost:9000/hascoapi/api/mt/gen/perelement/dsg/.../generateDASOCs=true

# Verify files created
ls /tmp/hascoapi/generated/
# Expected:
#   DSG-STUDY-NAME.xlsx
#   DA-SOC-INSTRUMENT-NAME.csv
#   DA-SOC-SLOT-ELEMENT-NAME.csv
```

---

### Phase 4: INS Converter (Week 6)
**Goal**: Auto-convert uploaded INS files to DSG

**Tasks**:
- [ ] 4.1: Create `INSConverter.java` - Parse INS and generate DSG + DA-SOC
- [ ] 4.2: Update `IngestionWorker.java` - Add INS auto-conversion
- [ ] 4.3: Add deprecation warning to logs
- [ ] 4.4: Test conversion with real INS files

**Deliverables**:
- ✅ INS files auto-converted on upload
- ✅ Deprecation warning displayed
- ✅ Conversion tested with 10+ INS files

**Validation**:
```bash
# Upload INS file
curl -F "file=@INS-ARROWHEAD-V4.xlsx" http://localhost:9000/hascoapi/api/ingest/DRAFT/ins/...

# Check logs for warning
tail -f hascoapi.log | grep "INS format deprecated"

# Verify DSG created
curl http://localhost:9000/hascoapi/api/instrument/elements/10/0
```

---

### Phase 5: API Testing & Verification (Week 7)
**Goal**: Ensure all API endpoints work with DSG-sourced data

**Tasks**:
- [ ] 5.1: Test all instrument endpoints (create, read, update, delete)
- [ ] 5.2: Test relationship queries (components, containerslots)
- [ ] 5.3: Test rendering endpoints (toText, toRDF, toFHIR)
- [ ] 5.4: Test pagination and filtering
- [ ] 5.5: Performance benchmarking

**Deliverables**:
- ✅ All API tests pass
- ✅ Response formats identical (INS vs DSG)
- ✅ Performance within acceptable range

**Test Suite**: `test/org/hascoapi/tests/INStoDSGAPITest.java`

---

### Phase 6: Migration & Deprecation (Week 8)
**Goal**: Migrate existing data and prepare for INS removal

**Tasks**:
- [ ] 6.1: Create migration script (`scripts/migrate-ins-to-dsg.sh`)
- [ ] 6.2: Run migration on test environment
- [ ] 6.3: Verify data integrity post-migration
- [ ] 6.4: Update documentation (user guide, API docs)
- [ ] 6.5: Add deprecation notices to INS-related code

**Deliverables**:
- ✅ Migration script tested
- ✅ Documentation updated
- ✅ Deprecation plan communicated

---

### Phase 7: INS Removal (Week 9+)
**Goal**: Remove INS code from codebase

**Tasks** (Only after 3+ months of deprecation):
- [ ] 7.1: Remove `AnnotateINS.java`
- [ ] 7.2: Remove `INSGenerator.java`, `ComponentGenerator.java`, etc.
- [ ] 7.3: Remove `INSGen.java`, `INSInstrument.java`, etc.
- [ ] 7.4: Remove INS test files
- [ ] 7.5: Remove INS routes (if any)
- [ ] 7.6: Update Constants (remove INS-related constants)

**Deliverables**:
- ✅ INS code removed
- ✅ All tests pass
- ✅ System runs without INS dependencies

---

## 10. ROLLBACK PLAN

### 10.1 Rollback Triggers
- **Trigger A**: Critical API endpoint regression (>5% failure rate)
- **Trigger B**: Performance degradation (>50% slower queries)
- **Trigger C**: Data corruption detected
- **Trigger D**: User-reported blocking issues (>10 reports)

### 10.2 Rollback Procedure

**Step 1: Stop Ingestion**
```bash
# Disable file uploads
touch /var/hascoapi/MAINTENANCE_MODE
```

**Step 2: Revert Code**
```bash
# Git rollback
git checkout <previous-stable-commit>
mvn clean compile
sbt clean compile
```

**Step 3: Restore Data (if corrupted)**
```bash
# Restore triplestore from backup
curl -X PUT http://localhost:3030/store/data \
    -H "Content-Type: application/n-triples" \
    --data-binary @backup-YYYYMMDD.nt
```

**Step 4: Verify**
```bash
# Test all endpoints
./scripts/run-api-tests.sh
```

**Step 5: Resume Operations**
```bash
rm /var/hascoapi/MAINTENANCE_MODE
```

---

## 11. SUCCESS CRITERIA

### 11.1 Functional Criteria
- [ ] ✅ All 7 VSTOI types (Instrument, Component, ComponentStem, ContainerSlot, Codebook, ResponseOption, AnnotationStem) created from DSG
- [ ] ✅ All INS properties supported via DA-SOC
- [ ] ✅ All API endpoints return identical JSON (INS vs DSG sources)
- [ ] ✅ DSG export generates VSTOI-typed SOCs + DA-SOC files
- [ ] ✅ INS files auto-convert to DSG + DA-SOC
- [ ] ✅ Round-trip tested (DSG → export → re-ingest → identical)

### 11.2 Performance Criteria
- [ ] ✅ Query response time ≤ 1.2x baseline
- [ ] ✅ Ingestion time ≤ 1.5x baseline
- [ ] ✅ No memory leaks detected
- [ ] ✅ Triplestore size increase ≤ 20% (due to DA-SOC graphs)

### 11.3 Quality Criteria
- [ ] ✅ Code coverage ≥ 80% for new/modified files
- [ ] ✅ All tests pass (unit, integration, performance)
- [ ] ✅ Documentation complete (user guide, API docs, developer guide)
- [ ] ✅ Zero API contract changes (backward compatible)

---

## 12. NEXT STEPS

### Immediate Actions
1. **Review this document** with team
2. **Confirm architectural decisions** (Section 3.3)
3. **Approve Phase 1 plan** (Section 9)
4. **Set up test environment** with sample DSG + DA-SOC files
5. **Create GitHub issues** for each task

### Questions to Resolve
1. **Q1**: Should we maintain INS URI patterns (`ahead:INS<hash>`) or create new DSG-specific patterns?
2. **Q2**: Timeline for INS deprecation - immediate warning or grace period?
3. **Q3**: Should we create a migration tool or accept coexistence indefinitely?
4. **Q4**: Performance benchmarking - what are acceptable thresholds?
5. **Q5**: Do we need backward compatibility with existing INS files, or can we reject them after a certain date?

---

## APPENDICES

### Appendix A: File Inventory

**Files to Modify**:
1. `app/org/hascoapi/ingestion/StudyObjectGenerator.java`
2. `app/org/hascoapi/ingestion/AnnotateDASOC.java`
3. `app/org/hascoapi/ingestion/IngestionWorker.java`
4. `app/org/hascoapi/transform/mt/dsg/DSGGen.java`
5. `app/org/hascoapi/transform/mt/dsg/DSGSOC.java`

**Files to Create**:
1. `app/org/hascoapi/utils/INSConverter.java`
2. `app/org/hascoapi/transform/mt/dsg/DSGSOCVstoi.java`
3. `test/org/hascoapi/tests/INStoDSGTransformationTest.java`
4. `test/org/hascoapi/tests/INStoDSGAPITest.java`
5. `scripts/migrate-ins-to-dsg.sh`

**Files to Remove** (Phase 7):
1. `app/org/hascoapi/ingestion/AnnotateINS.java`
2. `app/org/hascoapi/ingestion/INSGenerator.java`
3. `app/org/hascoapi/ingestion/ComponentGenerator.java`
4. `app/org/hascoapi/ingestion/CodeBookSlotGenerator.java`
5. `app/org/hascoapi/transform/mt/ins/INSGen.java`
6. `app/org/hascoapi/transform/mt/ins/INSInstrument.java`
7. `app/org/hascoapi/transform/mt/ins/INSComponent.java`
8. `app/org/hascoapi/transform/mt/ins/INSCodebook.java`
9. (and all other `INS*.java` files in transform/mt/ins/)

---

### Appendix B: Sample DSG File Structure (VSTOI)

```
DSG-ARROWHEAD-V4.xlsx

InfoSheet:
  Attribute           | Value
  hasStudyURI         | arrowhead:STUDY-ARROWHEAD-V4
  hasStudyKG          | ahead
  hasDependencies     | #Namespaces
  hasStudyDescription | #STD
  hasEntityDesign     | #SSD
  hasVariableDesign   | #VD

SSD:
  sheet                         | hasURI                           | type                           | label
  #SOC-INSTRUMENT-ARROWHEAD     | ahead:SOC-INSTRUMENT-ARROWHEAD   | hasco:InstrumentCollection     | Arrowhead Instruments
  #SOC-SLOT-ELEMENT-ARROWHEAD   | ahead:SOC-SLOT-ELEMENT-ARROWHEAD | hasco:ContainerSlotCollection  | Arrowhead Slots
  #SOC-COMPONENT-ARROWHEAD      | ahead:SOC-COMPONENT-ARROWHEAD    | hasco:ComponentCollection      | Arrowhead Components
  #SOC-COMPONENT-STEM-ARROWHEAD | ahead:SOC-COMPONENT-STEM-ARROWHEAD | hasco:ComponentStemCollection | Arrowhead Component Stems

SOC-INSTRUMENT-ARROWHEAD:
  originalID  | rdf:type           | rdfs:label         | rdfs:comment
  INS-001     | vstoi:Instrument   | Arrowhead V4       | Instrument for Arrowhead components

SOC-SLOT-ELEMENT-ARROWHEAD:
  originalID  | rdf:type             | rdfs:label                  | rdfs:comment
  CTS-0001    | vstoi:ContainerSlot  | Slot 1 - Weight Sensor      | First container slot
  CTS-0002    | vstoi:ContainerSlot  | Slot 2 - Airflow Sensor     | Second container slot
  ...

SOC-COMPONENT-ARROWHEAD:
  originalID  | rdf:type          | rdfs:label                         | rdfs:comment
  DTC-001     | vstoi:Detector    | Detector - Material Weight Sensor  | Weight sensor component
  DTC-002     | vstoi:Detector    | Detector - Airflow Sensor          | Airflow sensor component
  ...

SOC-COMPONENT-STEM-ARROWHEAD:
  originalID  | rdf:type              | rdfs:label                    | rdfs:comment
  DSM-001     | vstoi:ComponentStem   | Material Weight Sensor Stem   | Stem for weight sensors
  DSM-002     | vstoi:ComponentStem   | Airflow Sensor Stem           | Stem for airflow sensors
  ...
```

### Appendix C: Sample DA-SOC Files (VSTOI)

**DA-SOC-INSTRUMENT-ARROWHEAD.csv**:
```csv
originalID,vstoi:hasFirst,vstoi:hasShortName,vstoi:hasLanguage,vstoi:hasVersion,vstoi:hasStatus,hasco:hasImage
INS-001,ahead:INS-001/CTS/0001,ARROW-V4,en,4.0,DRAFT,http://images.example.com/arrowhead-v4.png
```

**DA-SOC-SLOT-ELEMENT-ARROWHEAD.csv**:
```csv
originalID,vstoi:belongsTo,vstoi:hasComponent,vstoi:hasNext,vstoi:hasPrevious,vstoi:hasPriority
CTS-0001,ahead:INS-001,ahead:DTC-001,ahead:INS-001/CTS/0002,,1
CTS-0002,ahead:INS-001,ahead:DTC-002,ahead:INS-001/CTS/0003,ahead:INS-001/CTS/0001,2
CTS-0003,ahead:INS-001,ahead:DTC-003,ahead:INS-001/CTS/0004,ahead:INS-001/CTS/0002,3
...
```

**DA-SOC-COMPONENT-ARROWHEAD.csv**:
```csv
originalID,vstoi:hasComponentStem,vstoi:hasCodebook,vstoi:isAttributeOf,vstoi:hasLanguage,vstoi:hasVersion
DTC-001,ahead:DSM-001,,ahead:INS-001,en,1.0
DTC-002,ahead:DSM-002,,ahead:INS-001,en,1.0
...
```

**DA-SOC-COMPONENT-STEM-ARROWHEAD.csv**:
```csv
originalID,vstoi:hasContent,vstoi:hasLanguage,vstoi:hasVersion
DSM-001,Material Weight Sensor (Load Cell / Bench Scale),en,1.0
DSM-002,Airflow Sensor,en,1.0
...
```

---

**END OF TRANSFORMATION PLAN**

---

**Document Version**: 1.0  
**Date**: 2026-04-17  
**Author**: GitHub Copilot  
**Status**: DRAFT - Awaiting Review  
**Next Review Date**: TBD

