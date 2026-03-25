# SDD Generation Issue - Root Cause Analysis

**Date:** March 19, 2026  
**Issue:** Generated SDD files have empty Dictionary Mapping sheet (only SDDObjects, no SDDAttributes)

## Summary

The SDD generation is working correctly, but the generated files are empty because the **ingestion process is not creating SDDAttribute entities**. The generator can only output what exists in the triplestore.

## Expected vs. Actual Behavior

### Expected (based on original SDD-WS.xlsx file)

Dictionary Mapping should contain **9 rows total**:

**6 SDDAttribute rows** (data variables with measurements):
- `Timestamp` → `time:inXSDDateTime` → `??instant`
- `ESP32_Chip_ID` → `hasco:originalID` → `??observation`
- `Temperature` → `envo:09200001` → `??weather` → `unit:DEG_C` → `??instant`
- `air_quality` → `envo:01000432` → `??weather` → `unit:MicroGM-PER-M3` → `??instant`
- `humidity` → `pato:0015009` → `??weather` → `unit:PERCENT` → `??instant`
- `Pressure_Baro` → `envo:09200011` → `??weather` → `unit:PA` → `??instant`

**3 SDDObject rows** (contextual entities):
- `??weather` → → → → → `envo:01001079`
- `??observation` → → → → → `time:Interval`
- `??instant` → → → → → `time:Instant` → → `time:inside` → `??observation`

### Actual (what's in the triplestore)

**0 SDDAttribute entities** created  
**3 SDDObject entities** created  

## Root Cause

### Ingestion Log Evidence

From the ingestion logs (element URI: `https://hadatac.org/ont/hadatac#/SDD1773851870316631`):

```
[SDDAttributeGenerator] createRows() START - total records: 9
[SDDAttributeGenerator] createRows() END - total rows created: 0
Ô£ô Created 0 objects

[SDDObjectGenerator] createRows() START - total records: 9
Ô£ô Created 3 objects
```

### Why SDDAttributes Were Not Created

The `SDDAttributeGenerator.createRows()` method has this logic:

```java
String attr = getAttribute(record);  // Gets value from "Attribute" column

if (attr == null || attr.equals("")) {
    // SKIP this row - don't create SDDAttribute
    continue;
} else {
    // Create SDDAttribute
    rows.add(createRow(record, ++rowNumber));
}
```

**The problem:** For ALL 9 rows, `getAttribute(record)` returned null or empty string, causing all rows to be skipped.

### Why getAttribute() Returns Empty

`getAttribute()` reads from:
- Template config: `[SDDA]` section → `AttributeType=Attribute`
- Excel column: The column named `Attribute`

**Possible causes:**
1. Column header mismatch (Excel has "Attribute" but code expects something else)
2. Column is empty in the uploaded file (user error)
3. Excel parser not reading the column correctly
4. Template configuration mismatch

## Generation Behavior (Current)

The generator (`SDDGen.java`) queries the triplestore for:

1. **SDDAttributes** with `partOfSchema = SDDICT...`
2. **SDDObjects** with `partOfSchema = SDDICT...`

Since 0 SDDAttributes exist, the Dictionary Mapping sheet only shows the 3 SDDObjects:
- `??weather` → `envo:01001079`
- `??observation` → `time:Interval`
- `??instant` → `time:Instant` → `time:inside` → `??observation`

**This is correct behavior** - the generator outputs what's in the triplestore.

## Solution Path

### Option 1: Fix the Ingestion (Recommended)

Investigate and fix `SDDAttributeGenerator` to properly create SDDAttribute entities:

1. **Debug the column mapping:**
   - Add logging to show what `getAttribute(record)` returns for each row
   - Verify template.conf `[SDDA]` section has `AttributeType=Attribute`
   - Check if Excel parser is correctly reading the "Attribute" column

2. **Test with the original SDD-WS.xlsx file:**
   - Re-ingest the file with detailed logging
   - Verify that rows with non-empty "Attribute" column create SDDAttributes
   - Verify that rows with empty "Attribute" column create SDDObjects

3. **Expected outcome after fix:**
   - Ingestion should create 6 SDDAttributes + 3 SDDObjects = 9 entities total
   - Generation should then output all 9 rows in Dictionary Mapping

### Option 2: Manual Data Correction (Workaround)

Manually create the missing SDDAttribute entities in the triplestore using SPARQL INSERT, then regenerate.

### Option 3: Template-Based Generation (Alternative)

Modify `SDDGen` to generate a "template" SDD with placeholder attributes when no SDDAttributes are found. This would give users a starting point to fill in manually.

## Verification Steps

After fixing ingestion, verify with these steps:

1. Delete existing SDD and DataFile from triplestore
2. Re-upload SDD-WS.xlsx
3. Check ingestion logs for:
   ```
   [SDDAttributeGenerator] Created 6 objects
   [SDDObjectGenerator] Created 3 objects
   ```
4. Query triplestore:
   ```sparql
   SELECT (COUNT(?attr) AS ?count) WHERE {
     ?attr a hasco:SDDAttribute .
     ?attr hasco:partOfSchema <...SDDICT...> .
   }
   # Expected: 6
   ```
5. Generate new SDD file
6. Verify Dictionary Mapping has 9 rows (6 attributes + 3 objects)

## Current Workaround

Until ingestion is fixed, users must:
1. Generate the SDD file (will be mostly empty)
2. Manually fill in the Dictionary Mapping sheet with their data variables
3. Re-upload and ingest the manually-filled SDD

This defeats the purpose of generation, but preserves the ingestion→generation→re-ingestion roundtrip.

## Related Files

- **Ingestion:** `app/org/hascoapi/ingestion/SDDAttributeGenerator.java` (line 264-295)
- **Generation:** `app/org/hascoapi/transform/mt/sdd/SDDGen.java` (line 287-350)
- **Dictionary Mapping:** `app/org/hascoapi/transform/mt/sdd/SDDDictionaryMapping.java`
- **Template:** `conf/template.conf` [SDDA] section

## Recommendation

**Priority: HIGH** - This is a critical bug that prevents the SDD workflow from functioning correctly.

**Action:** Focus on fixing `SDDAttributeGenerator` to properly detect and process rows with Attribute values. The generation side is working correctly and does not need changes.

