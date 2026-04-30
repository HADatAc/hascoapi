# 📢 MIGRATING FROM INS TO DSG + DA-SOC

## ⚠️ INS Format is Deprecated

The **INS (Instrument Namespace Specification)** file format is **deprecated** and will be removed in a future release.

Please migrate to the **DSG + DA-SOC** workflow for managing instruments and related VSTOI entities.

---

## 🔄 What Changed?

### Before (INS Approach)
```
INS-ARROWHEAD-V4.xlsx
├── Instruments sheet
├── SlotElements sheet
├── ComponentStems sheet
├── Components sheet
├── CodeBooks sheet
└── ResponseOptions sheet
```

### After (DSG + DA-SOC Approach)
```
DSG-ARROWHEAD-V4.xlsx
├── InfoSheet
├── SSD (Study Object Collection definitions)
├── SOC-INSTRUMENT-ARROWHEAD
├── SOC-SLOT-ELEMENT-ARROWHEAD
├── SOC-COMPONENT-STEM-ARROWHEAD
└── SOC-COMPONENT-ARROWHEAD

DA-SOC-INSTRUMENT-ARROWHEAD.csv
DA-SOC-SLOT-ELEMENT-ARROWHEAD.csv
DA-SOC-COMPONENT-ARROWHEAD.csv
DA-SOC-COMPONENT-STEM-ARROWHEAD.csv
```

---

## ✅ Benefits of DSG + DA-SOC

| Aspect | INS | DSG + DA-SOC |
|--------|-----|--------------|
| **Framework** | Separate from study management | Unified with study framework |
| **Version Control** | Excel sheets (hard to diff) | CSV files (easy to track) |
| **Collaboration** | Single large file | Separate files per aspect |
| **Property Extension** | Limited to predefined columns | Flexible via DA-SOC |
| **Data Management** | Parallel metadata system | Single unified system |
| **API Compatibility** | Same endpoints | Same endpoints (no change) |

---

## 📋 Migration Steps

### Step 1: Create DSG File Structure

Create a new DSG Excel file with the following structure:

#### InfoSheet
```
Attribute           | Value
hasStudyURI         | your-namespace:STUDY-YOUR-INSTRUMENT-NAME
hasStudyKG          | your-namespace
hasDependencies     | #Namespaces
hasStudyDescription | #STD
hasEntityDesign     | #SSD
hasVariableDesign   | #VD
```

#### SSD (Study Object Collection Definitions)
```
sheet                         | hasURI                           | type                              | label
#SOC-INSTRUMENT-NAME          | ns:SOC-INSTRUMENT-NAME           | hasco:InstrumentCollection        | Your Instruments
#SOC-SLOT-ELEMENT-NAME        | ns:SOC-SLOT-ELEMENT-NAME         | hasco:ContainerSlotCollection     | Container Slots
#SOC-COMPONENT-NAME           | ns:SOC-COMPONENT-NAME            | hasco:ComponentCollection         | Components
#SOC-COMPONENT-STEM-NAME      | ns:SOC-COMPONENT-STEM-NAME       | hasco:ComponentStemCollection     | Component Stems
```

Replace `NAME` with your instrument name (e.g., `ARROWHEAD`, `SURVEY-V2`, etc.)

---

### Step 2: Create SOC Worksheets

For each SOC defined in SSD, create a worksheet with **base properties only**:

#### SOC-INSTRUMENT-NAME Worksheet
```csv
originalID   | rdf:type           | rdfs:label              | rdfs:comment
INS-001      | vstoi:Instrument   | My Instrument V1        | Description of instrument
INS-002      | vstoi:Questionnaire| My Survey V2            | Survey questionnaire
```

#### SOC-SLOT-ELEMENT-NAME Worksheet
```csv
originalID   | rdf:type             | rdfs:label        | rdfs:comment
CTS-0001     | vstoi:ContainerSlot  | Slot 1            | First slot
CTS-0002     | vstoi:ContainerSlot  | Slot 2            | Second slot
```

#### SOC-COMPONENT-NAME Worksheet
```csv
originalID   | rdf:type          | rdfs:label                  | rdfs:comment
DTC-001      | vstoi:Detector    | Weight Sensor Component     | Measures weight
DTC-002      | vstoi:Detector    | Airflow Sensor Component    | Measures airflow
```

#### SOC-COMPONENT-STEM-NAME Worksheet
```csv
originalID   | rdf:type              | rdfs:label          | rdfs:comment
DSM-001      | vstoi:ComponentStem   | Weight Sensor Stem  | Template for weight sensors
DSM-002      | vstoi:ComponentStem   | Airflow Stem        | Template for airflow sensors
```

---

### Step 3: Create DA-SOC Files (Extended Properties)

Create CSV files for **VSTOI-specific properties** that don't fit in the base SOC schema:

#### DA-SOC-INSTRUMENT-NAME.csv
```csv
originalID,vstoi:hasFirst,vstoi:hasShortName,vstoi:hasLanguage,vstoi:hasVersion,hasco:hasImage
INS-001,ns:INS-001/CTS/0001,MYINST-V1,en,1.0,http://example.com/image.png
```

**Available Properties:**
- `vstoi:hasFirst` - URI of first container slot
- `vstoi:hasShortName` - Short name/abbreviation
- `vstoi:hasLanguage` - Language code (ISO 639-1, e.g., "en")
- `vstoi:hasVersion` - Version number
- `hasco:hasImage` - Image URL
- `hasco:hasWebDocument` - Documentation URL
- `vstoi:hasMaker` - Manufacturer URI
- `vstoi:minOperatingTemperature` - Min temperature
- `vstoi:maxOperatingTemperature` - Max temperature

#### DA-SOC-SLOT-ELEMENT-NAME.csv
```csv
originalID,vstoi:belongsTo,vstoi:hasComponent,vstoi:hasNext,vstoi:hasPrevious,vstoi:hasPriority
CTS-0001,ns:INS-001,ns:DTC-001,ns:INS-001/CTS/0002,,1
CTS-0002,ns:INS-001,ns:DTC-002,ns:INS-001/CTS/0003,ns:INS-001/CTS/0001,2
```

**Available Properties:**
- `vstoi:belongsTo` - Instrument URI this slot belongs to
- `vstoi:hasComponent` - Component URI in this slot
- `vstoi:hasNext` - URI of next slot
- `vstoi:hasPrevious` - URI of previous slot
- `vstoi:hasPriority` - Ordering priority (integer)

#### DA-SOC-COMPONENT-NAME.csv
```csv
originalID,vstoi:hasComponentStem,vstoi:hasCodebook,vstoi:isAttributeOf,hasco:hasWebDocument
DTC-001,ns:DSM-001,,ns:INS-001,http://example.com/weight-sensor-docs
DTC-002,ns:DSM-002,ns:CBK-001,ns:INS-001,
```

**Available Properties:**
- `vstoi:hasComponentStem` - ComponentStem URI (template)
- `vstoi:hasCodebook` - Codebook URI (for discrete values)
- `vstoi:isAttributeOf` - Parent entity URI
- `hasco:hasWebDocument` - Documentation URL

#### DA-SOC-COMPONENT-STEM-NAME.csv
```csv
originalID,vstoi:hasContent,vstoi:hasLanguage,vstoi:hasVersion,hasco:hasWebDocument
DSM-001,Weight Sensor Template,en,1.0,http://example.com/weight-stem
DSM-002,Airflow Sensor Template,en,1.0,http://example.com/airflow-stem
```

**Available Properties:**
- `vstoi:hasContent` - Content description
- `vstoi:hasLanguage` - Language code
- `vstoi:hasVersion` - Version number
- `hasco:hasWebDocument` - Documentation URL

---

### Step 4: Ingest DSG File

1. **Upload DSG file** to HAScO API:
   ```bash
   # Via web interface or API
   POST /hascoapi/api/ingest/DRAFT/dsg/{dsg-uri}
   ```

2. **Upload DA-SOC files** to HAScO API:
   ```bash
   POST /hascoapi/api/ingest/DRAFT/da/{data-acquisition-uri}
   ```

3. **Verify ingestion** by checking logs and querying API:
   ```bash
   GET /hascoapi/api/instrument/elements/10/0
   ```

---

## 🔍 Verification Checklist

After migration, verify that:

- [ ] All instruments are visible in API: `GET /api/instrument/elements/10/0`
- [ ] Instrument properties are complete (hasShortName, hasFirst, etc.)
- [ ] Components are visible: `GET /api/component/elements/10/0`
- [ ] Component relationships are correct (hasComponentStem, hasCodebook)
- [ ] Container slots are visible: `GET /api/containerslot/elements/10/0`
- [ ] Slot relationships are correct (hasNext, hasPrevious, belongsTo)
- [ ] No data loss compared to original INS file

---

## 📚 Additional Resources

- **Detailed Technical Plan**: `INS-TO-DSG-TRANSFORMATION-PLAN.md`
- **DA-SOC Specification**: `docs/DASOC-SPECIFICATION-v1.1.md`
- **VSTOI Ontology**: VSTOI vocabulary documentation
- **DSG Specification**: DSG file format documentation

---

## 🆘 Need Help?

If you encounter issues during migration:

1. **Check ingestion logs** in the DataFile log viewer
2. **Review error messages** for specific validation failures
3. **Consult documentation** in `docs/` folder
4. **Contact support** with:
   - Original INS file
   - Generated DSG + DA-SOC files
   - Error messages from logs

---

## ⏰ Deprecation Timeline

- **Current**: INS ingestion still works but displays deprecation warnings
- **Future Release**: INS support will be completely removed
- **Recommended Action**: Migrate to DSG + DA-SOC as soon as possible

---

## 📝 Example: Complete Migration

See `examples/INS-TO-DSG-MIGRATION-EXAMPLE/` for a complete example showing:
- Original INS file
- Converted DSG file
- Generated DA-SOC files
- Verification queries

---

**Last Updated**: 2026-04-17  
**Status**: Active Migration Period

