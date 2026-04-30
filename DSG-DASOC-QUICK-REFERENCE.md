# DSG + DA-SOC QUICK REFERENCE GUIDE
**VSTOI Entity Management Without INS Files**

---

## 🎯 OVERVIEW

This guide shows how to create and manage VSTOI entities (Instruments, Components, Codebooks, etc.) using **DSG + DA-SOC files only** - **no INS files needed**.

---

## 📋 VSTOI ENTITY TYPES

| Type | Purpose | SOC Naming | Example |
|------|---------|------------|---------|
| **Instrument** | Measurement instruments, questionnaires | `SOC-INSTRUMENT-*` | Medical simulators, surveys |
| **Component** | Parts of instruments | `SOC-COMPONENT-*` | Sensors, detectors |
| **ComponentStem** | Reusable component templates | `SOC-COMPONENT-STEM-*` | Sensor types |
| **ContainerSlot** | Instrument structure (slots) | `SOC-SLOT-ELEMENT-*` | Slot positions |
| **Codebook** | Response value sets | `SOC-CODEBOOK-*` | Likert scales, categories |
| **ResponseOption** | Individual response choices | `SOC-RESPONSE-OPTION-*` | "Agree", "Disagree" |
| **AnnotationStem** | Metadata annotation templates | `SOC-ANNOTATION-STEM-*` | Quality notes |

---

## 📄 FILE STRUCTURE

### DSG File (Excel - Base Properties)

**Worksheet: SSD (SOC Definitions)**
```
sheet                      | hasURI                    | type                           | label
#SOC-INSTRUMENT-MEDICAL    | kb:/SOC-INSTRUMENT-MEDICAL | hasco:InstrumentCollection     | Medical Instruments
#SOC-CODEBOOK-SCALES       | kb:/SOC-CODEBOOK-SCALES    | hasco:CodebookCollection       | Response Scales
```

**Worksheet: SOC-INSTRUMENT-MEDICAL**
```
originalID  | rdf:type           | label                   | comment
INS-001     | vstoi:Instrument   | Nursing Anne Simulator  | Training manikin for nursing
INS-002     | vstoi:Instrument   | Blood Pressure Trainer  | BP measurement trainer
```

**Worksheet: SOC-CODEBOOK-SCALES**
```
originalID  | rdf:type        | label              | comment
CBK-LIKERT5 | vstoi:Codebook  | 5-Point Likert     | Satisfaction scale
CBK-YESNO   | vstoi:Codebook  | Yes/No             | Binary choice
```

### DA-SOC Files (CSV - Extended Properties)

**File: DA-SOC-INSTRUMENT-MEDICAL.csv**
```csv
originalID,vstoi:hasShortName,vstoi:hasLanguage,vstoi:hasVersion,vstoi:hasFirst,hasco:hasWebDocument
INS-001,NursingAnne,en,1,kb:/INS-001/CTS/0001,https://laerdal.com/...
INS-002,BPTrainer,en,1,kb:/INS-002/CTS/0001,https://example.com/...
```

**File: DA-SOC-CODEBOOK-SCALES.csv**
```csv
originalID,vstoi:hasLanguage,vstoi:hasVersion,vstoi:hasStatus
CBK-LIKERT5,en,1.0,PUBLISHED
CBK-YESNO,en,1.0,DRAFT
```

---

## 🔄 WORKFLOW

### Step-by-Step Process

#### **Step 1: Create DSG File**

1. Open Excel
2. Create `InfoSheet`, `Namespaces`, `SSD`, `STD`, `VD` worksheets (standard DSG structure)
3. In `SSD`, define your SOCs:
   ```
   #SOC-INSTRUMENT-MYPROJECT
   #SOC-COMPONENT-MYPROJECT
   #SOC-CODEBOOK-MYPROJECT
   ```
4. Create SOC worksheets with **base properties only**:
   - `originalID` (required)
   - `rdf:type` (required - must be VSTOI type)
   - `label` (required)
   - `comment` (optional)
5. Save as `DSG-MYPROJECT.xlsx`

#### **Step 2: Create DA-SOC Files**

For each SOC, create a corresponding DA-SOC CSV file:

**File naming**: `DA-SOC-{SOC-NAME}.csv`
- Example: `DA-SOC-INSTRUMENT-MYPROJECT.csv`

**Column structure**:
- Column 0: `originalID` (required - matches DSG)
- Columns 1-N: Property URIs (e.g., `vstoi:hasShortName`, `vstoi:hasLanguage`)

**Example**:
```csv
originalID,vstoi:hasShortName,vstoi:hasLanguage,vstoi:hasVersion
INS-001,MyInstrument,en,1.0
INS-002,AnotherInstrument,en,2.0
```

#### **Step 3: Upload DSG File**

```bash
curl -X POST \
  -F "file=@DSG-MYPROJECT.xlsx" \
  "http://localhost:9000/hascoapi/api/ingest/DRAFT/dsg/kb:/STUDY-MYPROJECT"
```

**What happens**:
- ✅ Study created
- ✅ SOCs created
- ✅ StudyObjects created
- ✅ VSTOI POJOs auto-created (Instrument, Component, Codebook, etc.)

#### **Step 4: Upload DA-SOC Files**

```bash
curl -X POST \
  -F "file=@DA-SOC-INSTRUMENT-MYPROJECT.csv" \
  "http://localhost:9000/hascoapi/api/ingest/DRAFT/da/kb:/DA-MYPROJECT-1"

curl -X POST \
  -F "file=@DA-SOC-CODEBOOK-MYPROJECT.csv" \
  "http://localhost:9000/hascoapi/api/ingest/DRAFT/da/kb:/DA-MYPROJECT-2"
```

**What happens**:
- ✅ Matches `originalID` to existing StudyObjects
- ✅ Adds extended properties to triplestore (named graph)
- ✅ Updates VSTOI POJOs with enriched data

#### **Step 5: Query via API**

```bash
# Get all instruments
curl "http://localhost:9000/hascoapi/api/instrument/elements/10/0"

# Get all codebooks
curl "http://localhost:9000/hascoapi/api/codebook/elements/10/0"

# Get specific instrument
curl "http://localhost:9000/hascoapi/api/uri/kb:/INS-001"
```

---

## 📦 PROPERTY QUICK REFERENCE

### Instrument Properties

**SOC Worksheet** (always include):
- `originalID` ✅
- `rdf:type` = `vstoi:Instrument` ✅
- `label` ✅
- `comment` ❌ (optional)

**DA-SOC CSV** (extended - choose what you need):
```csv
originalID,rdfs:subClassOf,vstoi:hasShortName,vstoi:hasLanguage,vstoi:hasVersion,hasco:hasMaker,hasco:hasWebDocument,vstoi:hasFirst,hasco:hasImage
```

### Codebook Properties

**SOC Worksheet**:
- `originalID` ✅
- `rdf:type` = `vstoi:Codebook` ✅
- `label` ✅
- `comment` ❌

**DA-SOC CSV**:
```csv
originalID,vstoi:hasLanguage,vstoi:hasVersion,vstoi:hasStatus,vstoi:hasSerialNumber,vstoi:hasReviewNote
```

### ResponseOption Properties

**SOC Worksheet**:
- `originalID` ✅
- `rdf:type` = `vstoi:ResponseOption` ✅
- `label` ✅
- `comment` ❌

**DA-SOC CSV**:
```csv
originalID,vstoi:hasContent,vstoi:hasLanguage,vstoi:hasStatus
```

### ContainerSlot Properties (Relationships)

**SOC Worksheet**:
- `originalID` ✅ (pattern: `{INSTRUMENT-ID}_CTS_{NUMBER}`)
- `rdf:type` = `vstoi:ContainerSlot` ✅
- `label` ✅
- `comment` ❌

**DA-SOC CSV** (relationships):
```csv
originalID,vstoi:belongsTo,vstoi:hasComponent,vstoi:hasNext,vstoi:hasPrevious,vstoi:hasPriority
INS-001_CTS_0001,kb:/INS-001,kb:/COM-001,kb:/INS-001/CTS/0002,,0001
INS-001_CTS_0002,kb:/INS-001,kb:/COM-002,kb:/INS-001/CTS/0003,kb:/INS-001/CTS/0001,0002
```

---

## 🔗 RELATIONSHIP MODELING

### Instrument → Slots → Components

**DSG Structure**:
```
SOC-INSTRUMENT-X:
  originalID: INS-001, rdf:type: vstoi:Instrument

SOC-SLOT-ELEMENT-X:
  originalID: INS-001_CTS_0001, rdf:type: vstoi:ContainerSlot
  originalID: INS-001_CTS_0002, rdf:type: vstoi:ContainerSlot

SOC-COMPONENT-X:
  originalID: COM-001, rdf:type: vstoi:Component
  originalID: COM-002, rdf:type: vstoi:Component
```

**DA-SOC Relationships**:
```csv
# DA-SOC-INSTRUMENT-X.csv
originalID,vstoi:hasFirst
INS-001,kb:/INS-001/CTS/0001

# DA-SOC-SLOT-ELEMENT-X.csv
originalID,vstoi:belongsTo,vstoi:hasComponent,vstoi:hasNext,vstoi:hasPriority
INS-001_CTS_0001,kb:/INS-001,kb:/COM-001,kb:/INS-001/CTS/0002,0001
INS-001_CTS_0002,kb:/INS-001,kb:/COM-002,,0002

# DA-SOC-COMPONENT-X.csv
originalID,vstoi:hasComponentStem
COM-001,kb:/CSM-001
COM-002,kb:/CSM-002
```

**Result Graph**:
```
INS-001 (Instrument)
  │
  ├─ vstoi:hasFirst ───────> CTS_0001 (ContainerSlot)
  │                            │
  │                            ├─ vstoi:belongsTo ──> INS-001
  │                            ├─ vstoi:hasComponent ──> COM-001 (Component)
  │                            │                          │
  │                            │                          └─ vstoi:hasComponentStem ──> CSM-001
  │                            │
  │                            └─ vstoi:hasNext ──> CTS_0002 (ContainerSlot)
  │                                                  │
  │                                                  ├─ vstoi:belongsTo ──> INS-001
  │                                                  └─ vstoi:hasComponent ──> COM-002
```

---

## 🧪 TESTING EXAMPLES

### Test 1: Create Instrument with Slots

**DSG File**:
```
SOC-INSTRUMENT-TEST:
  originalID: TEST-INS-001, rdf:type: vstoi:Instrument, label: Test Instrument

SOC-SLOT-ELEMENT-TEST:
  originalID: TEST-INS-001_CTS_0001, rdf:type: vstoi:ContainerSlot, label: Slot 1
  originalID: TEST-INS-001_CTS_0002, rdf:type: vstoi:ContainerSlot, label: Slot 2
```

**DA-SOC Files**:
```csv
# DA-SOC-INSTRUMENT-TEST.csv
originalID,vstoi:hasShortName,vstoi:hasFirst
TEST-INS-001,TestInst,kb:/TEST-INS-001/CTS/0001

# DA-SOC-SLOT-ELEMENT-TEST.csv
originalID,vstoi:belongsTo,vstoi:hasNext,vstoi:hasPriority
TEST-INS-001_CTS_0001,kb:/TEST-INS-001,kb:/TEST-INS-001/CTS/0002,0001
TEST-INS-001_CTS_0002,kb:/TEST-INS-001,,0002
```

**Verification**:
```bash
# Check instrument created
curl "http://localhost:9000/hascoapi/api/instrument/elements/1/0" | jq '.body[].uri'

# Check slots
curl "http://localhost:9000/hascoapi/api/instrument/containerslots/kb:/TEST-INS-001"
```

---

### Test 2: Create Codebook with Response Options

**DSG File**:
```
SOC-CODEBOOK-SATISFACTION:
  originalID: CBK-SAT, rdf:type: vstoi:Codebook, label: Satisfaction Scale

SOC-RESPONSE-OPTION-SAT:
  originalID: RO-SAT-1, rdf:type: vstoi:ResponseOption, label: Very Dissatisfied
  originalID: RO-SAT-2, rdf:type: vstoi:ResponseOption, label: Dissatisfied
  originalID: RO-SAT-3, rdf:type: vstoi:ResponseOption, label: Neutral
  originalID: RO-SAT-4, rdf:type: vstoi:ResponseOption, label: Satisfied
  originalID: RO-SAT-5, rdf:type: vstoi:ResponseOption, label: Very Satisfied
```

**DA-SOC Files**:
```csv
# DA-SOC-CODEBOOK-SATISFACTION.csv
originalID,vstoi:hasLanguage,vstoi:hasVersion,vstoi:hasStatus
CBK-SAT,en,1.0,PUBLISHED

# DA-SOC-RESPONSE-OPTION-SAT.csv
originalID,vstoi:hasContent,vstoi:hasLanguage
RO-SAT-1,"1 - Very Dissatisfied",en
RO-SAT-2,"2 - Dissatisfied",en
RO-SAT-3,"3 - Neutral",en
RO-SAT-4,"4 - Satisfied",en
RO-SAT-5,"5 - Very Satisfied",en
```

**Verification**:
```bash
# Check codebook
curl "http://localhost:9000/hascoapi/api/codebook/elements/10/0"

# Check response options
curl "http://localhost:9000/hascoapi/api/responseoption/elements/10/0"
```

---

## ⚡ COMMON PATTERNS

### Pattern 1: Simple Instrument (No Slots)

**DSG**:
```
SOC-INSTRUMENT:
  INS-SIMPLE, vstoi:Instrument, Simple Instrument, Basic instrument
```

**DA-SOC**:
```csv
originalID,vstoi:hasShortName,vstoi:hasLanguage
INS-SIMPLE,SimpleInst,en
```

---

### Pattern 2: Instrument with Component Hierarchy

**DSG**:
```
SOC-INSTRUMENT:        INS-COMPLEX
SOC-SLOT-ELEMENT:      INS-COMPLEX_CTS_0001
SOC-COMPONENT:         COM-SENSOR
SOC-COMPONENT-STEM:    CSM-SENSOR-TYPE
```

**DA-SOC**:
```csv
# DA-SOC-INSTRUMENT.csv
originalID,vstoi:hasFirst
INS-COMPLEX,kb:/INS-COMPLEX/CTS/0001

# DA-SOC-SLOT-ELEMENT.csv
originalID,vstoi:belongsTo,vstoi:hasComponent
INS-COMPLEX_CTS_0001,kb:/INS-COMPLEX,kb:/COM-SENSOR

# DA-SOC-COMPONENT.csv
originalID,vstoi:hasComponentStem
COM-SENSOR,kb:/CSM-SENSOR-TYPE
```

---

### Pattern 3: Questionnaire with Codebook

**DSG**:
```
SOC-INSTRUMENT:        QUEST-001, vstoi:Questionnaire
SOC-SLOT-ELEMENT:      QUEST-001_CTS_0001
SOC-COMPONENT:         COMP-Q1
SOC-CODEBOOK:          CBK-LIKERT
SOC-RESPONSE-OPTION:   RO-1 to RO-5
```

**DA-SOC**:
```csv
# DA-SOC-COMPONENT.csv
originalID,vstoi:hasCodebook
COMP-Q1,kb:/CBK-LIKERT

# DA-SOC-CODEBOOK.csv (relationships to response options handled via CodebookSlots)
originalID,vstoi:hasLanguage,vstoi:hasVersion
CBK-LIKERT,en,1.0
```

---

## 🚨 COMMON MISTAKES

### ❌ Mistake 1: Putting Extended Properties in SOC Worksheet

**Wrong**:
```
SOC-INSTRUMENT:
originalID | rdf:type | label | vstoi:hasShortName | vstoi:hasLanguage
INS-001    | vstoi:Instrument | ... | MyInst | en
```

**Correct**:
```
SOC-INSTRUMENT:
originalID | rdf:type | label
INS-001    | vstoi:Instrument | My Instrument

DA-SOC-INSTRUMENT.csv:
originalID,vstoi:hasShortName,vstoi:hasLanguage
INS-001,MyInst,en
```

---

### ❌ Mistake 2: Wrong SOC Naming Pattern

**Wrong**:
```
SOC-INSTRUMENTS (plural)
SOC-MY-INSTRUMENT (not following pattern)
```

**Correct**:
```
SOC-INSTRUMENT-{STUDY-NAME}
SOC-CODEBOOK-{STUDY-NAME}
```

---

### ❌ Mistake 3: Missing originalID in DA-SOC

**Wrong**:
```csv
vstoi:hasShortName,vstoi:hasLanguage
MyInst,en
```

**Correct**:
```csv
originalID,vstoi:hasShortName,vstoi:hasLanguage
INS-001,MyInst,en
```

---

## 📊 VALIDATION CHECKLIST

### Before Uploading DSG

- [ ] InfoSheet has `hasStudyURI`
- [ ] SSD sheet lists all SOCs
- [ ] Each SOC has worksheet with same name (e.g., `#SOC-INSTRUMENT-X` → `SOC-INSTRUMENT-X` tab)
- [ ] All SOC worksheets have `originalID`, `rdf:type`, `label` columns
- [ ] `rdf:type` column contains VSTOI types (e.g., `vstoi:Instrument`)
- [ ] No extended properties in SOC worksheets (keep them for DA-SOC)

### Before Uploading DA-SOC

- [ ] File name matches `DA-SOC-{SOC-NAME}.csv` pattern
- [ ] Column 0 is `originalID`
- [ ] All `originalID` values exist in corresponding SOC
- [ ] Property column headers are valid URIs or CURIEs (e.g., `vstoi:hasLanguage`)
- [ ] No empty `originalID` cells

---

## 🔧 TROUBLESHOOTING

### Issue 1: "DASOC_00003: originalID not found"

**Cause**: DA-SOC references originalID that doesn't exist in SOC

**Solution**:
1. Check SOC worksheet for matching originalID
2. Verify spelling (case-sensitive!)
3. Ensure DSG was ingested before DA-SOC

**Debug**:
```bash
# Query for originalIDs in SOC
curl -X POST "http://localhost:3030/store" \
  --data-urlencode 'query=
    SELECT ?originalId WHERE {
      ?obj hasco:isMemberOf <YOUR-SOC-URI> .
      ?obj hasco:originalID ?originalId .
    }
  '
```

---

### Issue 2: "No VSTOI entity created"

**Cause**: `rdf:type` in SOC doesn't match VSTOI pattern

**Solution**:
1. Use exact VSTOI type: `vstoi:Instrument`, `vstoi:Codebook`, etc.
2. Check for typos
3. Ensure VSTOI namespace defined in Namespaces sheet

**Valid Types**:
- `vstoi:Instrument`
- `vstoi:Questionnaire` (subclass of Instrument)
- `vstoi:Component`
- `vstoi:Detector` (subclass of Component)
- `vstoi:ComponentStem`
- `vstoi:ContainerSlot`
- `vstoi:Codebook`
- `vstoi:ResponseOption`
- `vstoi:AnnotationStem`

---

### Issue 3: "Property not showing in API response"

**Cause**: Property set in DA-SOC but not reflected in POJO

**Debug Steps**:
1. Check if property exists in POJO class (e.g., `Codebook.java`)
2. Check if enrichment method handles this property
3. Check triplestore directly:
   ```sparql
   SELECT ?p ?o WHERE {
     <YOUR-ENTITY-URI> ?p ?o .
   }
   ```

**Solution**:
- If property exists in POJO: Check enrichment method
- If property doesn't exist: Store in DA-SOC graph only (still queryable via SPARQL)

---

## 📚 COMPLETE EXAMPLES

### Example 1: Medical Simulator Study

**DSG-MEDICAL-SIMULATORS.xlsx**:

```
SSD:
  #SOC-INSTRUMENT-SIMULATORS, kb:/SOC-INSTRUMENT-SIM, hasco:InstrumentCollection
  #SOC-COMPONENT-SENSORS, kb:/SOC-COMPONENT-SEN, hasco:ComponentCollection

SOC-INSTRUMENT-SIMULATORS:
  originalID | rdf:type | label
  SIM-NURSE  | vstoi:Instrument | Nursing Care Simulator
  SIM-BP     | vstoi:Instrument | Blood Pressure Simulator

SOC-COMPONENT-SENSORS:
  originalID | rdf:type | label
  SENS-BP    | vstoi:Component | BP Sensor
  SENS-PULSE | vstoi:Component | Pulse Sensor
```

**DA-SOC-INSTRUMENT-SIMULATORS.csv**:
```csv
originalID,vstoi:hasShortName,vstoi:hasLanguage,vstoi:hasVersion,hasco:hasWebDocument
SIM-NURSE,NursingSim,en,2.0,https://laerdal.com/nursing
SIM-BP,BPSim,en,1.5,https://laerdal.com/bp
```

**DA-SOC-COMPONENT-SENSORS.csv**:
```csv
originalID,vstoi:hasComponentStem,vstoi:hasLanguage
SENS-BP,kb:/CSM-BP-STEM,en
SENS-PULSE,kb:/CSM-PULSE-STEM,en
```

---

## 💡 TIPS & BEST PRACTICES

### Tip 1: Keep SOC Worksheets Minimal
- Only include columns that are **always required**
- Move everything else to DA-SOC
- Makes DSG files easier to maintain

### Tip 2: Split DA-SOC Files by Property Group
Instead of one huge DA-SOC with all properties:
```
DA-SOC-INSTRUMENT-METADATA.csv    (hasShortName, hasLanguage, hasVersion)
DA-SOC-INSTRUMENT-STRUCTURE.csv   (hasFirst, rdfs:subClassOf)
DA-SOC-INSTRUMENT-REFERENCES.csv  (hasWebDocument, hasImage, hasMaker)
```

Benefits:
- Easier to update specific property groups
- Parallel ingestion possible
- Clearer organization

### Tip 3: Use Consistent URI Patterns
Follow INS conventions:
- Instruments: `INS<hash>` or `INS-<name>`
- Components: `COM<hash>` or `COM-<name>`
- ComponentStems: `CSM<hash>` or `CSM-<name>`
- Codebooks: `CBK<hash>` or `CBK-<name>`
- Slots: `{INSTRUMENT-ID}_CTS_{NUMBER}`

### Tip 4: Always Upload DSG Before DA-SOC
```
1. DSG → Creates base entities
2. DA-SOC → Enriches entities
```

If you upload DA-SOC first, you'll get DASOC_00003 errors (originalID not found).

---

## 🔗 RELATED RESOURCES

- **Full Implementation Guide**: `INS-TO-DSG-TRANSFORMATION-PLAN.md`
- **Current Status**: `DSG-DASOC-CURRENT-STATUS.md`
- **Implementation Summary**: `VSTOI-DSG-COMPLETE-IMPLEMENTATION-SUMMARY.md`
- **DA-SOC Specification**: `DA-SOC-SPECIFICATION-CONSOLIDATED-UNDERSTANDING-V2`
- **DSG Specification**: `DSG SPECIFICATION — CONSOLIDATED UNDERSTANDING`

---

## 📞 SUPPORT

For questions or issues:
- Check documentation files above
- Review error logs in DataFile ingestion log
- Query triplestore directly via SPARQL endpoint
- Refer to error dictionary for DASOC_* error codes

---

**Last Updated**: 2026-04-17  
**Version**: 1.0  
**Status**: Production Ready

