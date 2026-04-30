# 📋 DSG TRANSFORMATION GUIDE - VSTOI ENTITY STRUCTURE

**Document Version**: 1.0  
**Date**: 2026-04-21  
**Purpose**: Transform existing DSG file to support VSTOI entities (Instruments, Components, etc.)  
**Target Audience**: Perplexity.ai / DSG Transformation Tool / Data Curators  
**Status**: ✅ CANONICAL REFERENCE

---

## 🎯 OBJECTIVE

Transform an existing DSG file from the **traditional study object structure** to the **new VSTOI-based structure** where instruments, components, component stems, container slots, codebooks, response options, and annotation stems are represented as **Study Object Collections (SOCs)** instead of separate INS files.

---

## 📊 WHAT CHANGED

### Old System (Deprecated - Before April 2026)
- **INS files** (Excel) contained instruments and components
- **DSG files** only contained study subjects, locations, etc.
- Separate metadata systems
- Instruments managed separately from studies

### New System (Current - April 2026)
- **No INS files** - everything is DSG + DA-SOC
- **DSG files** now contain VSTOI entities as SOCs
- **DA-SOC files** (CSV) provide extended properties
- Unified metadata framework
- Instruments integrated with study management

---

## 🏗️ NEW DSG STRUCTURE

### Required Worksheets

Every DSG file must have these worksheets **in this exact order**:

1. **InfoSheet** - Metadata control
2. **Namespaces** - Ontology prefixes
3. **SSD** - SOC definitions (**THIS IS WHERE YOU DEFINE VSTOI SOCS**)
4. **STD** - Study administrative data
5. **VD** - Variable design (optional, can be empty)
6. **SOC-*** - One worksheet per SOC defined in SSD

---

## 📝 DETAILED WORKSHEET SPECIFICATIONS

### 1️⃣ InfoSheet Worksheet

**Purpose**: Central metadata control for the entire DSG file

**Columns**: `Attribute` | `Value`

**Required Rows**:
```
Attribute           | Value
hasStudyURI         | {namespace}:STUDY-{YOUR-STUDY-NAME}
hasStudyKG          | {namespace-prefix}
hasDependencies     | #Namespaces
hasStudyDescription | #STD
hasEntityDesign     | #SSD
hasVariableDesign   | #VD
```

**Example**:
```
Attribute           | Value
hasStudyURI         | pmsr:STUDY-ARROWHEAD-V4
hasStudyKG          | pmsr
hasDependencies     | #Namespaces
hasStudyDescription | #STD
hasEntityDesign     | #SSD
hasVariableDesign   | #VD
```

**Rules**:
- `hasStudyURI` must be globally unique
- `hasStudyKG` is your namespace prefix
- Values starting with `#` reference worksheet names

---

### 2️⃣ Namespaces Worksheet

**Purpose**: Define ontology prefixes used throughout the file

**Columns**: `hasPrefix` | `hasNameSpace` | `hasFormat` | `hasSource`

**Standard VSTOI Ontologies** (always include these):
```csv
hasPrefix,hasNameSpace,hasFormat,hasSource
vstoi,http://hadatac.org/ont/vstoi#,text/turtle,http://hadatac.org/ont/vstoi
hasco,http://hadatac.org/ont/hasco#,text/turtle,http://hadatac.org/ont/hasco
ncit,http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#,application/rdf+xml,http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl
uberon,http://purl.obolibrary.org/obo/UBERON_,application/rdf+xml,http://purl.obolibrary.org/obo/uberon.owl
rdfs,http://www.w3.org/2000/01/rdf-schema#,text/turtle,http://www.w3.org/2000/01/rdf-schema
rdf,http://www.w3.org/1999/02/22-rdf-syntax-ns#,text/turtle,http://www.w3.org/1999/02/22-rdf-syntax-ns
```

**Add your custom namespace**:
```csv
pmsr,http://hadatac.org/ont/pmsr#,text/turtle,http://hadatac.org/ont/pmsr
```

---

### 3️⃣ SSD Worksheet (CRITICAL - SOC Definitions)

**Purpose**: Define all Study Object Collections (this is where VSTOI entities are declared)

**Columns**: `sheet` | `hasURI` | `type` | `hasSOCReference` | `comment` | `label` | `definition` | `hasSpaceScope` | `hasTimeScope`

**VSTOI SOC Types** (use these exact values):

| VSTOI Entity Type | SOC Type to Use | Example Sheet Name | Example Label |
|-------------------|-----------------|-------------------|---------------|
| Instrument | `hasco:InstrumentCollection` | `#SOC-INSTRUMENT-MEDICAL` | Medical Instruments |
| Component | `hasco:ComponentCollection` | `#SOC-COMPONENT-MEDICAL` | Medical Components |
| ComponentStem | `hasco:ComponentStemCollection` | `#SOC-COMPONENT-STEM-MEDICAL` | Component Stems |
| ContainerSlot | `hasco:ContainerSlotCollection` | `#SOC-SLOT-ELEMENT-MEDICAL` | Container Slots |
| Codebook | `hasco:CodebookCollection` | `#SOC-CODEBOOK-SCALES` | Response Scales |
| ResponseOption | `hasco:ResponseOptionCollection` | `#SOC-RESPONSE-OPTION-SCALES` | Response Options |
| AnnotationStem | `hasco:AnnotationStemCollection` | `#SOC-ANNOTATION-STEM-QUALITY` | Annotation Templates |

**Example SSD Worksheet**:
```csv
sheet,hasURI,type,label,comment
#SOC-INSTRUMENT-MEDICAL,pmsr:/SOC-INSTRUMENT-MEDICAL,hasco:InstrumentCollection,Medical Instruments,Medical simulation instruments
#SOC-COMPONENT-MEDICAL,pmsr:/SOC-COMPONENT-MEDICAL,hasco:ComponentCollection,Medical Components,Components for medical instruments
#SOC-COMPONENT-STEM-MEDICAL,pmsr:/SOC-COMPONENT-STEM-MEDICAL,hasco:ComponentStemCollection,Component Stems,Reusable component templates
#SOC-SLOT-ELEMENT-MEDICAL,pmsr:/SOC-SLOT-ELEMENT-MEDICAL,hasco:ContainerSlotCollection,Container Slots,Instrument structure slots
#SOC-CODEBOOK-SCALES,pmsr:/SOC-CODEBOOK-SCALES,hasco:CodebookCollection,Response Scales,Response value sets
#SOC-RESPONSE-OPTION-SCALES,pmsr:/SOC-RESPONSE-OPTION-SCALES,hasco:ResponseOptionCollection,Response Options,Individual response choices
```

**CRITICAL RULES**:
- ✅ `sheet` column MUST start with `#SOC-`
- ✅ `hasURI` should use your namespace prefix
- ✅ `type` must be one of the 7 collection types above
- ✅ Worksheet name must match `sheet` column (without `#` prefix)
- ❌ Do NOT use `hasco:StudyObjectCollection` for VSTOI entities

---

### 4️⃣ STD Worksheet (Study Administrative Data)

**Purpose**: Study metadata for administrative purposes

**Columns**: `Study ID` | `Title` | `Specific Aims` | `Significance` | `Institution` | `Principal Investigator` | `Email` | etc.

**Minimum Required**:
```csv
Study ID,Title,Principal Investigator,Email
STUDY-ARROWHEAD-V4,Arrowhead Medical Instruments,Dr. John Doe,user@example.com
```

**Optional Columns**:
- `Specific Aims`
- `Significance`
- `Institution`
- `Approved IRB ID`
- `Start Date`
- `End Date`

---

### 5️⃣ VD Worksheet (Variable Design)

**Purpose**: Variable design (can be empty for instrument-focused DSGs)

**Minimum Structure**:
```csv
variableName,variableLabel
```

**Note**: This worksheet can be completely empty for VSTOI-focused DSGs.

---

### 6️⃣ SOC Worksheets (ONE PER VSTOI TYPE)

Each SOC defined in SSD must have a corresponding worksheet with base properties only.

---

#### 🔹 SOC-INSTRUMENT-{NAME}

**Purpose**: Define instruments/questionnaires/models

**Required Columns**:
- `originalID` ✅ (unique identifier)
- `rdf:type` ✅ (must be VSTOI type URI)
- `rdfs:label` ✅ (human-readable name)
- `rdfs:comment` ❌ (optional description)

**Allowed `rdf:type` values for Instruments**:
- `vstoi:Instrument` ← Most common
- `vstoi:Questionnaire` ← For surveys
- `vstoi:Model` ← For virtual/software instruments
- `ncit:C16830` ← For imaging instruments
- Any URI containing "Instrument" in the path

**Example**:
```csv
originalID,rdf:type,rdfs:label,rdfs:comment
INS1739823398493725,vstoi:Instrument,Laerdal Nursing Anne Mannequin,High-fidelity patient simulator for nursing training
INS1739301009974715,vstoi:Instrument,ARTEC LEO Scanner,3D scanning instrument for anatomical modeling
INS1739398437257165,vstoi:Instrument,Airway Simulator,Airway management training device
```

**⚠️ IMPORTANT - DO NOT Include These Columns** (they go in DA-SOC):
- ❌ `vstoi:hasShortName`
- ❌ `vstoi:hasLanguage`
- ❌ `vstoi:hasVersion`
- ❌ `vstoi:hasFirst`
- ❌ `rdfs:subClassOf`
- ❌ `hasco:hasWebDocument`
- ❌ `hasco:hasImage`
- ❌ `hasco:hasMaker`

---

#### 🔹 SOC-SLOT-ELEMENT-{NAME}

**Purpose**: Define container slots (instrument structure)

**Required Columns**:
- `originalID` ✅
- `rdf:type` ✅
- `rdfs:label` ✅
- `rdfs:comment` ❌ (optional)

**Allowed `rdf:type` values**:
- `vstoi:ContainerSlot` ← Most common
- `vstoi:Subcontainer` ← For nested containers
- Any URI containing "ContainerSlot" or "Slot"

**originalID Pattern**: `{INSTRUMENT-ID}_CTS_{NUMBER}`
- Example: `INS1739823398493725_CTS_0001`
- Example: `INS1739823398493725_CTS_0002`

**Example**:
```csv
originalID,rdf:type,rdfs:label,rdfs:comment
INS1739823398493725_CTS_0001,vstoi:ContainerSlot,Slot 1,First container slot for sensor placement
INS1739823398493725_CTS_0002,vstoi:ContainerSlot,Slot 2,Second container slot
INS1739823398493725_CTS_0003,vstoi:ContainerSlot,Slot 3,Third container slot
INS1739823398493725_CTS_0004,vstoi:ContainerSlot,Slot 4,Fourth container slot
```

**⚠️ IMPORTANT - DO NOT Include Relationship Properties** (they go in DA-SOC):
- ❌ `vstoi:belongsTo`
- ❌ `vstoi:hasComponent`
- ❌ `vstoi:hasNext`
- ❌ `vstoi:hasPrevious`
- ❌ `vstoi:hasPriority`

---

#### 🔹 SOC-COMPONENT-STEM-{NAME}

**Purpose**: Define reusable component templates (stems)

**Required Columns**:
- `originalID` ✅
- `rdf:type` ✅
- `rdfs:label` ✅
- `rdfs:comment` ❌ (optional)

**Allowed `rdf:type` values**:
- `vstoi:ComponentStem` ← Most common
- `vstoi:ItemStem` ← Alternative
- Any URI containing "Stem" or "ComponentStem"

**Example**:
```csv
originalID,rdf:type,rdfs:label,rdfs:comment
CSM1740443161927215,vstoi:ComponentStem,Acceleration Component Stem,Unit of Acceleration sensor template
CSM1740436912687875,vstoi:ComponentStem,Airflow Component Stem,Airflow measurement sensor template
CSM1740440484451525,vstoi:ComponentStem,Anal Canal Component Stem,Anatomical component for examination training
```

**⚠️ IMPORTANT - Extended Properties Go in DA-SOC**:
- ❌ `vstoi:hasContent`
- ❌ `vstoi:hasLanguage`
- ❌ `vstoi:hasVersion`
- ❌ `rdfs:subClassOf`
- ❌ `hasco:hasWebDocument`
- ❌ `hasco:hasImage`

---

#### 🔹 SOC-COMPONENT-{NAME}

**Purpose**: Define concrete components (sensors, detectors, etc.)

**Required Columns**:
- `originalID` ✅
- `rdf:type` ✅
- `rdfs:label` ✅
- `rdfs:comment` ❌ (optional)

**Allowed `rdf:type` values**:
- `vstoi:Component` ← Most common
- `vstoi:Detector` ← For sensors
- Any URI containing "Component" or "Detector"

**Example**:
```csv
originalID,rdf:type,rdfs:label,rdfs:comment
COM1738095221724775,vstoi:Component,Shock Link Monitor,Cardiac monitoring component
COM1738097990641815,vstoi:Component,Chest Inflator,Chest expansion simulator component
COM1740091620192285,vstoi:Component,IV Access,Intravenous access training component
COM1740091527359405,vstoi:Component,Anal Canal,Anatomical examination component
```

**⚠️ IMPORTANT - Extended Properties Go in DA-SOC**:
- ❌ `vstoi:hasComponentStem`
- ❌ `vstoi:hasCodebook`
- ❌ `vstoi:isAttributeOf`
- ❌ `hasco:hascoType`
- ❌ `hasco:hasWebDocument`

---

#### 🔹 SOC-CODEBOOK-{NAME}

**Purpose**: Define response value sets (scales, categories)

**Required Columns**:
- `originalID` ✅
- `rdf:type` ✅
- `rdfs:label` ✅
- `rdfs:comment` ❌ (optional)

**Allowed `rdf:type` values**:
- `vstoi:Codebook`
- Any URI containing "Codebook"

**Example**:
```csv
originalID,rdf:type,rdfs:label,rdfs:comment
CBK-LIKERT-5,vstoi:Codebook,5-Point Likert Scale,Standard satisfaction scale (1-5)
CBK-YES-NO,vstoi:Codebook,Yes/No Binary,Binary choice codebook
CBK-PAIN-SCALE,vstoi:Codebook,Pain Assessment Scale,0-10 pain severity scale
```

---

#### 🔹 SOC-RESPONSE-OPTION-{NAME}

**Purpose**: Define individual response choices

**Required Columns**:
- `originalID` ✅
- `rdf:type` ✅
- `rdfs:label` ✅
- `rdfs:comment` ❌ (optional)

**Allowed `rdf:type` values**:
- `vstoi:ResponseOption`
- Any URI containing "ResponseOption"

**Example**:
```csv
originalID,rdf:type,rdfs:label,rdfs:comment
RO-STRONGLY-AGREE,vstoi:ResponseOption,Strongly Agree,Highest agreement level
RO-AGREE,vstoi:ResponseOption,Agree,Moderate agreement
RO-NEUTRAL,vstoi:ResponseOption,Neutral,Neither agree nor disagree
RO-DISAGREE,vstoi:ResponseOption,Disagree,Moderate disagreement
RO-STRONGLY-DISAGREE,vstoi:ResponseOption,Strongly Disagree,Highest disagreement
```

---

#### 🔹 SOC-ANNOTATION-STEM-{NAME}

**Purpose**: Define annotation/metadata templates

**Required Columns**:
- `originalID` ✅
- `rdf:type` ✅
- `rdfs:label` ✅
- `rdfs:comment` ❌ (optional)

**Allowed `rdf:type` values**:
- `vstoi:AnnotationStem`
- Any URI containing "AnnotationStem"

**Example**:
```csv
originalID,rdf:type,rdfs:label,rdfs:comment
AS-QUALITY-NOTE,vstoi:AnnotationStem,Quality Note Template,Template for quality assessment annotations
AS-CALIBRATION-LOG,vstoi:AnnotationStem,Calibration Log Template,Template for calibration records
```

---

## 🔧 DA-SOC FILES (EXTENDED PROPERTIES)

**Purpose**: Add VSTOI-specific properties that don't fit in the base SOC schema

**File Naming Pattern**: `DA-SOC-{SOC-NAME}.csv`
- Example: `DA-SOC-INSTRUMENT-MEDICAL.csv`
- Example: `DA-SOC-COMPONENT-MEDICAL.csv`

**Column 0**: MUST be `originalID` (matches SOC worksheet)
**Columns 1-N**: Property URIs (prefixed with ontology namespace)

---

### DA-SOC for Instruments

**File**: `DA-SOC-INSTRUMENT-{NAME}.csv`

**Available Properties**:
```csv
originalID,rdfs:subClassOf,vstoi:hasShortName,vstoi:hasLanguage,vstoi:hasVersion,hasco:hasMaker,hasco:hasWebDocument,vstoi:hasFirst,hasco:hasImage,vstoi:maxLoggedMeasurements,vstoi:minOperatingTemperature,vstoi:maxOperatingTemperature,hasco:hasOperatingTemperatureUnit
```

**Example**:
```csv
originalID,rdfs:subClassOf,vstoi:hasShortName,vstoi:hasLanguage,vstoi:hasVersion,hasco:hasWebDocument,vstoi:hasFirst
INS1739301009974715,pmsr:/INS1739300958884615,ARTECLEOScanner,en,1,https://www.artec3d.com/portable-3d-scanners/artec-leo,
INS1739823398493725,pmsr:/INS1739287159797795,NursingAnneMannequin,en,1,https://laerdal.com/br/nursinganne,pmsr:/INS1739823398493725/CTS/0001
```

**Property Descriptions**:

| Property | Type | Description | Example |
|----------|------|-------------|---------|
| `rdfs:subClassOf` | URI | Parent instrument class | `pmsr:/INS1739300958884615` |
| `vstoi:hasShortName` | String | Short name (no spaces) | `ARTECLEOScanner` |
| `vstoi:hasLanguage` | String | ISO 639-1 language code | `en`, `pt`, `es` |
| `vstoi:hasVersion` | String | Version number | `1`, `2.0`, `1.5` |
| `hasco:hasMaker` | URI | Manufacturer URI | `pmsr:/LAERDAL-MEDICAL` |
| `hasco:hasWebDocument` | URL | Documentation URL | `https://laerdal.com/...` |
| `vstoi:hasFirst` | URI | First container slot URI | `pmsr:/INS123/CTS/0001` |
| `hasco:hasImage` | URL | Image URL | `http://example.com/image.png` |
| `vstoi:maxLoggedMeasurements` | Integer | Max measurements | `1000` |
| `vstoi:minOperatingTemperature` | Decimal | Min temperature | `-10.0` |
| `vstoi:maxOperatingTemperature` | Decimal | Max temperature | `40.0` |
| `hasco:hasOperatingTemperatureUnit` | URI | Temperature unit | `unit:DEG_C` |

---

### DA-SOC for Slot Elements

**File**: `DA-SOC-SLOT-ELEMENT-{NAME}.csv`

**Available Properties**:
```csv
originalID,vstoi:belongsTo,vstoi:hasComponent,vstoi:hasNext,vstoi:hasPrevious,vstoi:hasPriority
```

**Example**:
```csv
originalID,vstoi:belongsTo,vstoi:hasComponent,vstoi:hasNext,vstoi:hasPrevious,vstoi:hasPriority
INS1739823398493725_CTS_0001,pmsr:/INS1739823398493725,,pmsr:/INS1739823398493725/CTS/0002,,0001
INS1739823398493725_CTS_0002,pmsr:/INS1739823398493725,,pmsr:/INS1739823398493725/CTS/0003,pmsr:/INS1739823398493725/CTS/0001,0002
INS1739823398493725_CTS_0003,pmsr:/INS1739823398493725,,pmsr:/INS1739823398493725/CTS/0004,pmsr:/INS1739823398493725/CTS/0002,0003
INS1739823398493725_CTS_0004,pmsr:/INS1739823398493725,,,pmsr:/INS1739823398493725/CTS/0003,0004
```

**Property Descriptions**:

| Property | Type | Description | Example |
|----------|------|-------------|---------|
| `vstoi:belongsTo` | URI | Instrument this slot belongs to | `pmsr:/INS1739823398493725` |
| `vstoi:hasComponent` | URI | Component placed in this slot | `pmsr:/COM1738097990641815` |
| `vstoi:hasNext` | URI | Next slot in chain | `pmsr:/INS123/CTS/0002` |
| `vstoi:hasPrevious` | URI | Previous slot in chain | `pmsr:/INS123/CTS/0001` |
| `vstoi:hasPriority` | String | Priority/order number | `0001`, `0002`, `0003` |

**Important Notes**:
- The first slot should NOT have `vstoi:hasPrevious`
- The last slot should NOT have `vstoi:hasNext`
- `vstoi:hasComponent` can be empty if slot is not filled
- Chain forms: first → ... → last

---

### DA-SOC for Component Stems

**File**: `DA-SOC-COMPONENT-STEM-{NAME}.csv`

**Available Properties**:
```csv
originalID,rdfs:subClassOf,vstoi:hasContent,vstoi:hasLanguage,vstoi:hasVersion,hasco:hasMaker,rdfs:comment,hasco:hasImage,hasco:hasWebDocument
```

**Example**:
```csv
originalID,rdfs:subClassOf,vstoi:hasContent,vstoi:hasLanguage,vstoi:hasVersion,rdfs:comment,hasco:hasWebDocument
CSM1740443161927215,pmsr:MotionComponentStem,Acceleration Component Stem,en,1,Unit of Acceleration - A unit of the rate of change of linear or angular velocity with respect to time.,http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#C48450
CSM1740436912687875,pmsr:/CSM1740436692667015,Airflow Component Stem,en,1,The total volume of gas in a specified airway or set of airways at a specified point in time in the respiratory cycle.,http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#C170457
```

**Property Descriptions**:

| Property | Type | Description | Example |
|----------|------|-------------|---------|
| `rdfs:subClassOf` | URI | Parent stem class | `pmsr:MotionComponentStem` |
| `vstoi:hasContent` | String | Textual content/description | `Acceleration Component Stem` |
| `vstoi:hasLanguage` | String | Language code | `en` |
| `vstoi:hasVersion` | String | Version number | `1` |
| `rdfs:comment` | String | Extended description | `Unit of Acceleration...` |
| `hasco:hasWebDocument` | URL | Documentation URL | `http://ncicb.nci.nih.gov/...` |
| `hasco:hasImage` | URL | Image URL | `http://example.com/image.png` |

---

### DA-SOC for Components

**File**: `DA-SOC-COMPONENT-{NAME}.csv`

**Available Properties**:
```csv
originalID,hasco:hascoType,rdf:type,vstoi:hasComponentStem,vstoi:hasCodebook,vstoi:isAttributeOf,hasco:hasWebDocument
```

**Example**:
```csv
originalID,hasco:hascoType,rdf:type,vstoi:hasComponentStem,vstoi:hasCodebook,vstoi:isAttributeOf
COM1738095221724775,vstoi:Component,pmsr:/CSM1738095066920345,pmsr:/CSM1738095066920345,,uberon:0004535
COM1738097990641815,vstoi:Component,pmsr:/CSM1738097871592315,pmsr:/CSM1738097871592315,pmsr:/CBK1738096258564815,uberon:0001004
COM1740091620192285,vstoi:Component,pmsr:/CSM1740090464389005,pmsr:/CSM1740090464389005,,uberon:0009055
```

**Property Descriptions**:

| Property | Type | Description | Example |
|----------|------|-------------|---------|
| `hasco:hascoType` | URI | HASCO type classification | `vstoi:Component` |
| `rdf:type` | URI | RDF type (usually ComponentStem URI) | `pmsr:/CSM1738095066920345` |
| `vstoi:hasComponentStem` | URI | Component stem this is based on | `pmsr:/CSM1738095066920345` |
| `vstoi:hasCodebook` | URI | Codebook for discrete values | `pmsr:/CBK1738096258564815` |
| `vstoi:isAttributeOf` | URI | Anatomical entity (UBERON) | `uberon:0004535` |
| `hasco:hasWebDocument` | URL | Documentation URL | `https://example.com/...` |

---

### DA-SOC for Codebooks

**File**: `DA-SOC-CODEBOOK-{NAME}.csv`

**Available Properties**:
```csv
originalID,vstoi:hasLanguage,vstoi:hasVersion,vstoi:hasStatus,vstoi:hasSerialNumber,vstoi:hasReviewNote
```

**Example**:
```csv
originalID,vstoi:hasLanguage,vstoi:hasVersion,vstoi:hasStatus
CBK-LIKERT-5,en,1.0,PUBLISHED
CBK-YES-NO,en,1.0,DRAFT
CBK-PAIN-SCALE,en,2.0,CURRENT
```

**Property Descriptions**:

| Property | Type | Description | Example |
|----------|------|-------------|---------|
| `vstoi:hasLanguage` | String | Language code | `en`, `pt`, `es` |
| `vstoi:hasVersion` | String | Version number | `1.0`, `2.0` |
| `vstoi:hasStatus` | String | Publication status | `DRAFT`, `CURRENT`, `PUBLISHED` |
| `vstoi:hasSerialNumber` | String | Serial/identifier | `CBK-001` |
| `vstoi:hasReviewNote` | String | Review notes | `Approved by committee` |

---

### DA-SOC for Response Options

**File**: `DA-SOC-RESPONSE-OPTION-{NAME}.csv`

**Available Properties**:
```csv
originalID,vstoi:hasContent,vstoi:hasLanguage,vstoi:hasStatus
```

**Example**:
```csv
originalID,vstoi:hasContent,vstoi:hasLanguage,vstoi:hasStatus
RO-STRONGLY-AGREE,Strongly Agree,en,ACTIVE
RO-AGREE,Agree,en,ACTIVE
RO-NEUTRAL,Neutral,en,ACTIVE
```

**Property Descriptions**:

| Property | Type | Description | Example |
|----------|------|-------------|---------|
| `vstoi:hasContent` | String | Display text for option | `Strongly Agree` |
| `vstoi:hasLanguage` | String | Language code | `en` |
| `vstoi:hasStatus` | String | Active status | `ACTIVE`, `INACTIVE` |

---

### DA-SOC for Annotation Stems

**File**: `DA-SOC-ANNOTATION-STEM-{NAME}.csv`

**Available Properties**:
```csv
originalID,vstoi:hasContent,vstoi:hasLanguage,vstoi:hasVersion,vstoi:hasStatus
```

**Example**:
```csv
originalID,vstoi:hasContent,vstoi:hasLanguage,vstoi:hasVersion
AS-QUALITY-NOTE,Quality assessment annotation template,en,1.0
AS-CALIBRATION-LOG,Calibration log entry template,en,1.0
```

**Property Descriptions**:

| Property | Type | Description | Example |
|----------|------|-------------|---------|
| `vstoi:hasContent` | String | Template description | `Quality assessment...` |
| `vstoi:hasLanguage` | String | Language code | `en` |
| `vstoi:hasVersion` | String | Version number | `1.0` |
| `vstoi:hasStatus` | String | Status | `ACTIVE`, `DEPRECATED` |

---

## ✅ VALIDATION CHECKLIST

Before finalizing the DSG transformation, verify:

### DSG File Structure
- [ ] `InfoSheet` worksheet exists with correct structure
- [ ] `Namespaces` worksheet exists with VSTOI ontologies (vstoi, hasco, ncit, uberon)
- [ ] `SSD` worksheet exists with all SOC definitions
- [ ] `STD` worksheet exists with study metadata
- [ ] `VD` worksheet exists (can be empty)
- [ ] Each SOC defined in `SSD` has a corresponding worksheet

### SOC Worksheets
- [ ] Each SOC worksheet has exactly 4 columns: `originalID`, `rdf:type`, `rdfs:label`, `rdfs:comment`
- [ ] `rdf:type` column contains correct VSTOI types (vstoi:Instrument, vstoi:Component, etc.)
- [ ] NO extended properties in SOC worksheets (all moved to DA-SOC)
- [ ] `originalID` values are unique within each SOC
- [ ] Slot element `originalID` follows pattern: `{INSTRUMENT-ID}_CTS_{NUMBER}`

### SSD Worksheet
- [ ] `sheet` column starts with `#SOC-`
- [ ] `type` column uses correct collection types:
  - `hasco:InstrumentCollection`
  - `hasco:ComponentCollection`
  - `hasco:ComponentStemCollection`
  - `hasco:ContainerSlotCollection`
  - `hasco:CodebookCollection`
  - `hasco:ResponseOptionCollection`
  - `hasco:AnnotationStemCollection`
- [ ] Sheet names match `sheet` column (without `#`)
- [ ] Each SOC has a `hasURI` value

### DA-SOC Files
- [ ] File names match pattern: `DA-SOC-{SOC-NAME}.csv`
- [ ] Column 0 is `originalID`
- [ ] `originalID` values match SOC worksheet exactly
- [ ] Property URIs use correct prefixes (e.g., `vstoi:`, `hasco:`, `rdfs:`)
- [ ] Relationship properties (hasNext, hasPrevious, belongsTo) use full URIs
- [ ] No trailing commas or empty columns

### URI Format
- [ ] Namespace prefix is consistent (e.g., `pmsr:` or `pmsr:/`)
- [ ] Instrument URIs follow pattern: `{prefix}/INS{hash}`
- [ ] Slot URIs follow pattern: `{prefix}/INS{hash}/CTS/{number}`
- [ ] Component Stem URIs follow pattern: `{prefix}/CSM{hash}` or `{prefix}/DSM{hash}`
- [ ] Component URIs follow pattern: `{prefix}/COM{hash}` or `{prefix}/DTC{hash}`
- [ ] Codebook URIs follow pattern: `{prefix}/CBK{hash}`

---

## 🚀 TRANSFORMATION STEPS FOR PERPLEXITY.AI

### Input Data Requirements

Provide Perplexity.ai with the following information:

1. **Your current data tables** in CSV format:
   - Instruments table (with all current columns)
   - Components table (if applicable)
   - Component Stems table (if applicable)
   - Slot Elements table (if applicable)

2. **Configuration**:
   - Namespace prefix (e.g., `pmsr`)
   - Study name (e.g., `ARROWHEAD-V4`)
   - Study title
   - Principal investigator name and email

3. **Optional**:
   - Codebooks table
   - Response Options table
   - Annotation Stems table

### Expected Output

Perplexity.ai should generate:

#### 1. DSG Excel File (`DSG-{STUDY-NAME}.xlsx`)

With worksheets:
- ✅ InfoSheet
- ✅ Namespaces
- ✅ SSD (listing all SOC types needed)
- ✅ STD
- ✅ VD (empty)
- ✅ SOC-INSTRUMENT-{NAME}
- ✅ SOC-COMPONENT-{NAME} (if components exist)
- ✅ SOC-COMPONENT-STEM-{NAME} (if stems exist)
- ✅ SOC-SLOT-ELEMENT-{NAME} (if slots exist)
- ✅ SOC-CODEBOOK-{NAME} (if codebooks exist)
- ✅ SOC-RESPONSE-OPTION-{NAME} (if options exist)
- ✅ SOC-ANNOTATION-STEM-{NAME} (if annotations exist)

#### 2. DA-SOC CSV Files

- ✅ `DA-SOC-INSTRUMENT-{NAME}.csv`
- ✅ `DA-SOC-COMPONENT-{NAME}.csv` (if applicable)
- ✅ `DA-SOC-COMPONENT-STEM-{NAME}.csv` (if applicable)
- ✅ `DA-SOC-SLOT-ELEMENT-{NAME}.csv` (if applicable)
- ✅ `DA-SOC-CODEBOOK-{NAME}.csv` (if applicable)
- ✅ `DA-SOC-RESPONSE-OPTION-{NAME}.csv` (if applicable)
- ✅ `DA-SOC-ANNOTATION-STEM-{NAME}.csv` (if applicable)

### Transformation Rules

**Apply these rules when transforming data**:

#### Rule 1: Split Properties
- **Base properties** (originalID, rdf:type, label, comment) → SOC worksheet
- **Extended properties** (everything else) → DA-SOC CSV file

#### Rule 2: Column Mapping
From your existing Instruments table:
- `originalID` → SOC worksheet (Column A)
- Extract label/comment → SOC worksheet (Columns C, D)
- Determine VSTOI type → SOC worksheet (Column B as `rdf:type`)
- ALL other columns → DA-SOC CSV file

#### Rule 3: URI Generation
- Slot URIs: `{instrument-uri}/CTS/{number}`
  - Example: `pmsr:/INS1739823398493725/CTS/0001`
- Use full URIs in DA-SOC relationship properties
- Use namespace prefix consistently

#### Rule 4: Slot Chaining
If slots exist:
- First slot: `hasNext` points to second, NO `hasPrevious`
- Middle slots: `hasNext` points to next, `hasPrevious` points to previous
- Last slot: NO `hasNext`, `hasPrevious` points to previous
- All slots: `belongsTo` points to parent instrument

---

## 📦 COMPLETE TRANSFORMATION EXAMPLE

### INPUT (Your Current Data)

**Instruments Table**:
```csv
originalID,rdfs:subClassOf,vstoi:hasShortName,vstoi:hasLanguage,vstoi:hasVersion,hasco:hasWebDocument,vstoi:hasFirst,rdfs:label,rdfs:comment
INS1739823398493725,pmsr:/INS1739287159797795,NursingAnneMannequin,en,1,https://laerdal.com/br/nursinganne,pmsr:/INS1739823398493725/CTS/0001,Laerdal Nursing Anne Mannequin,High-fidelity patient simulator
INS1739301009974715,pmsr:/INS1739300958884615,ARTECLEOScanner,en,1,https://www.artec3d.com/portable-3d-scanners/artec-leo,,ARTEC LEO Scanner,3D scanning instrument
```

**Slot Elements Table**:
```csv
originalID,vstoi:belongsTo,vstoi:hasComponent,vstoi:hasNext,vstoi:hasPrevious,vstoi:hasPriority,rdfs:label
INS1739823398493725_CTS_0001,pmsr:/INS1739823398493725,,pmsr:/INS1739823398493725/CTS/0002,,0001,Slot 1
INS1739823398493725_CTS_0002,pmsr:/INS1739823398493725,,pmsr:/INS1739823398493725/CTS/0003,pmsr:/INS1739823398493725/CTS/0001,0002,Slot 2
```

### OUTPUT (DSG Structure)

#### DSG File: `DSG-MEDICAL-INSTRUMENTS.xlsx`

**InfoSheet**:
```
Attribute           | Value
hasStudyURI         | pmsr:STUDY-MEDICAL-INSTRUMENTS
hasStudyKG          | pmsr
hasDependencies     | #Namespaces
hasStudyDescription | #STD
hasEntityDesign     | #SSD
hasVariableDesign   | #VD
```

**Namespaces**:
```csv
hasPrefix,hasNameSpace,hasFormat,hasSource
vstoi,http://hadatac.org/ont/vstoi#,text/turtle,http://hadatac.org/ont/vstoi
hasco,http://hadatac.org/ont/hasco#,text/turtle,http://hadatac.org/ont/hasco
pmsr,http://hadatac.org/ont/pmsr#,text/turtle,http://hadatac.org/ont/pmsr
rdfs,http://www.w3.org/2000/01/rdf-schema#,text/turtle,http://www.w3.org/2000/01/rdf-schema
```

**SSD**:
```csv
sheet,hasURI,type,label
#SOC-INSTRUMENT-MEDICAL,pmsr:/SOC-INSTRUMENT-MEDICAL,hasco:InstrumentCollection,Medical Instruments
#SOC-SLOT-ELEMENT-MEDICAL,pmsr:/SOC-SLOT-ELEMENT-MEDICAL,hasco:ContainerSlotCollection,Container Slots
```

**STD**:
```csv
Study ID,Title,Principal Investigator,Email
STUDY-MEDICAL-INSTRUMENTS,Medical Instruments Collection,Dr. Jane Smith,user@example.com
```

**VD**: (empty)

**SOC-INSTRUMENT-MEDICAL**:
```csv
originalID,rdf:type,rdfs:label,rdfs:comment
INS1739823398493725,vstoi:Instrument,Laerdal Nursing Anne Mannequin,High-fidelity patient simulator
INS1739301009974715,vstoi:Instrument,ARTEC LEO Scanner,3D scanning instrument
```

**SOC-SLOT-ELEMENT-MEDICAL**:
```csv
originalID,rdf:type,rdfs:label,rdfs:comment
INS1739823398493725_CTS_0001,vstoi:ContainerSlot,Slot 1,First container slot
INS1739823398493725_CTS_0002,vstoi:ContainerSlot,Slot 2,Second container slot
```

#### DA-SOC Files

**DA-SOC-INSTRUMENT-MEDICAL.csv**:
```csv
originalID,rdfs:subClassOf,vstoi:hasShortName,vstoi:hasLanguage,vstoi:hasVersion,hasco:hasWebDocument,vstoi:hasFirst
INS1739823398493725,pmsr:/INS1739287159797795,NursingAnneMannequin,en,1,https://laerdal.com/br/nursinganne,pmsr:/INS1739823398493725/CTS/0001
INS1739301009974715,pmsr:/INS1739300958884615,ARTECLEOScanner,en,1,https://www.artec3d.com/portable-3d-scanners/artec-leo,
```

**DA-SOC-SLOT-ELEMENT-MEDICAL.csv**:
```csv
originalID,vstoi:belongsTo,vstoi:hasComponent,vstoi:hasNext,vstoi:hasPrevious,vstoi:hasPriority
INS1739823398493725_CTS_0001,pmsr:/INS1739823398493725,,pmsr:/INS1739823398493725/CTS/0002,,0001
INS1739823398493725_CTS_0002,pmsr:/INS1739823398493725,,pmsr:/INS1739823398493725/CTS/0003,pmsr:/INS1739823398493725/CTS/0001,0002
```

---

## 🎯 SUCCESS CRITERIA

The transformation is complete and correct when:

### Structure Validation
1. ✅ DSG file has all 6 mandatory worksheets in correct order
2. ✅ SSD worksheet defines all needed VSTOI SOCs
3. ✅ Each SOC has a corresponding worksheet
4. ✅ DA-SOC files exist for all SOCs with extended properties

### Content Validation
5. ✅ SOC worksheets have only 4 columns: `originalID`, `rdf:type`, `rdfs:label`, `rdfs:comment`
6. ✅ `rdf:type` values are correct VSTOI types
7. ✅ No relationship/extended properties in SOC worksheets
8. ✅ DA-SOC files have `originalID` as column 0
9. ✅ All `originalID` values in DA-SOC match SOC worksheet

### URI Validation
10. ✅ All URIs use correct namespace prefix
11. ✅ Slot URIs follow pattern: `{instrument-uri}/CTS/{number}`
12. ✅ Relationship properties use full URIs (not just IDs)

### Data Integrity
13. ✅ No data loss during transformation
14. ✅ All properties from original data appear somewhere (SOC or DA-SOC)
15. ✅ Slot chains are complete (first → ... → last)
16. ✅ No orphaned references (all URIs point to existing entities)

---

## 🔴 CRITICAL NOTES

### DO NOT Include in SOC Worksheets
These properties MUST go in DA-SOC files:
- ❌ All `vstoi:has*` properties (except when part of rdf:type)
- ❌ All `rdfs:subClassOf`
- ❌ All relationship properties (hasNext, hasPrevious, belongsTo, hasComponent, hasFirst)
- ❌ All `hasco:has*` properties (hasMaker, hasWebDocument, hasImage, etc.)
- ❌ All content properties (hasContent)
- ❌ All version/language/status properties

### MUST Include in SOC Worksheets
Only these columns are allowed:
- ✅ `originalID` (required - unique identifier)
- ✅ `rdf:type` (required - VSTOI type URI)
- ✅ `rdfs:label` (required - human-readable name)
- ✅ `rdfs:comment` (optional - description)
- ✅ `scopeID`, `timeScopeID`, `spaceScopeID` (only for scope relationships)

### URI Format Rules
- Namespace prefix: Use `pmsr:` or `pmsr:/` consistently
- Instrument URIs: `pmsr:/INS{hash}` (e.g., `pmsr:/INS1739823398493725`)
- Slot URIs: `pmsr:/INS{hash}/CTS/{number}` (e.g., `pmsr:/INS1739823398493725/CTS/0001`)
- Component Stem URIs: `pmsr:/CSM{hash}` or `pmsr:/DSM{hash}`
- Component URIs: `pmsr:/COM{hash}` or `pmsr:/DTC{hash}`
- Codebook URIs: `pmsr:/CBK{hash}`
- Full URIs in relationships (not relative)

### Column Ordering
- SOC worksheets: `originalID` | `rdf:type` | `rdfs:label` | `rdfs:comment`
- DA-SOC files: `originalID` | {property1} | {property2} | ...
- Order in DA-SOC doesn't matter (except column 0 must be originalID)

---

## 📚 REFERENCE DOCUMENTATION

For more information, see:

- **MIGRATION-INS-TO-DSG.md** - Complete migration guide
- **DSG-DASOC-QUICK-REFERENCE.md** - Quick reference for DSG + DA-SOC
- **VSTOI-DSG-COMPLETE-IMPLEMENTATION-SUMMARY.md** - Technical implementation details
- **IMPLEMENTACAO-COMPLETA-PHASES-1-2.md** - Implementation status report

---

## 🆘 TROUBLESHOOTING

### Common Issues

**Issue**: Instruments don't appear in frontend
- **Cause**: `hasSIRManagerEmail` not set
- **Solution**: Ensure STD worksheet has correct PI email

**Issue**: DA-SOC enrichment fails
- **Cause**: `originalID` mismatch between SOC and DA-SOC
- **Solution**: Verify `originalID` values match exactly

**Issue**: Slots not forming chain
- **Cause**: Incorrect `hasNext`/`hasPrevious` URIs
- **Solution**: Verify full URIs, not just IDs

**Issue**: Type detection fails
- **Cause**: Wrong `rdf:type` value
- **Solution**: Use exact VSTOI types (vstoi:Instrument, vstoi:Component, etc.)

**Issue**: Properties not enriching
- **Cause**: Properties in SOC instead of DA-SOC
- **Solution**: Move extended properties to DA-SOC files

---

## 📅 VERSION HISTORY

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-04-21 | Initial transformation guide |

---

**END OF TRANSFORMATION GUIDE**

---

**Document Status**: ✅ CANONICAL REFERENCE  
**Author**: GitHub Copilot  
**Purpose**: Guide DSG transformation from traditional to VSTOI-based structure  
**Audience**: Perplexity.ai, Data Curators, Developers  
**Maintenance**: Keep synchronized with VSTOI implementation changes

