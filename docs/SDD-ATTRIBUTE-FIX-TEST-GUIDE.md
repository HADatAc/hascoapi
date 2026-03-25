# SDD Attribute Fix - Quick Test Guide

## What Was Fixed

The `SDDAttributeGenerator` was not creating SDDAttributes during ingestion due to a **field shadowing bug**. 

The fix removed the shadowing `Templates templates;` field declaration and the redundant initialization, allowing `initMapping()` to access the parent class's properly initialized `templates` field.

## How to Test

### Step 1: Verify the Fix in Logs

When you **re-ingest** an SDD file, look for these changes in the logs:

#### BEFORE FIX (BAD):
```
[ERROR] SDDAttributeGenerator.initMapping(): Templates is null after BaseGenerator initialization
[SDDAttributeGenerator] Template mapping for AttributeType: 'null'
[SDDAttributeGenerator] Processing record #1:
[SDDAttributeGenerator]   attribute (from column 'null')=''
[SDDAttributeGenerator]   -> SKIPPED (empty attribute)
Created 0 rows
```

#### AFTER FIX (GOOD):
```
[SDDAttributeGenerator] Template mapping for AttributeType: 'Attribute'
[SDDAttributeGenerator] Processing record #1:
[SDDAttributeGenerator]   label='Timestamp'
[SDDAttributeGenerator]   attribute='time:inXSDDateTime'
[SDDAttributeGenerator]   -> Creating row for attribute
Created 6 rows
```

### Step 2: Check Generated SDD File

Generate an SDD file and check the **Dictionary Mapping** sheet:

#### BEFORE FIX (BAD):
Only 3 rows (SDDObjects only):
```
Column          Attribute  attributeOf  Unit  Time  Entity                                              Role  Relation          inRelationTo   wasDerivedFrom  wasGeneratedBy
??weather                                           http://purl.obolibrary.org/obo/ENVO_01001079
??observation                                       http://www.w3.org/2006/time#Interval
??instant                                           http://www.w3.org/2006/time#Instant                       http://www.w3.org/2006/time#inside  ??observation
```

#### AFTER FIX (GOOD):
9 rows (6 SDDAttributes + 3 SDDObjects):
```
Column          Attribute             attributeOf  Unit                 Time      Entity                                              Role  Relation          inRelationTo   wasDerivedFrom  wasGeneratedBy
Timestamp       time:inXSDDateTime    ??instant
ESP32_Chip_ID   hasco:originalID      ??observation
Temperature     envo:09200001         ??weather    unit:DEG_C           ??instant
air_quality     envo:01000432         ??weather    unit:MicroGM-PER-M3  ??instant
humidity        pato:0015009          ??weather    unit:PERCENT         ??instant
Pressure_Baro   envo:09200011         ??weather    unit:PA              ??instant
??weather                                                                         http://purl.obolibrary.org/obo/ENVO_01001079
??observation                                                                     http://www.w3.org/2006/time#Interval
??instant                                                                         http://www.w3.org/2006/time#Instant                       http://www.w3.org/2006/time#inside  ??observation
```

### Step 3: Verify in Triplestore

Query the triplestore to check SDDAttributes were created:

```sparql
PREFIX hasco: <http://hadatac.org/ont/hasco/>

SELECT (COUNT(?attr) AS ?count) 
WHERE {
  ?attr a hasco:SDDAttribute .
  ?attr hasco:partOfSchema <https://hadatac.org/ont/hadatac#/SDDICT1773851870316631> .
}
```

- **BEFORE FIX**: count = 0
- **AFTER FIX**: count = 6

## Quick Commands

### Uningest existing SDD (if needed):
```bash
# Use your uningest script or API call
curl -X DELETE "http://localhost:9000/api/sdd/uningest?uri=https://hadatac.org/ont/hadatac#/SDD1773851870316631"
```

### Re-ingest SDD:
```bash
# Upload and ingest your SDD file through the UI or API
```

### Generate SDD:
```bash
curl -X POST "http://localhost:9000/api/sdd/generate?status=DRAFT&filename=test-output.xlsx"
```

## Success Criteria

✅ **Fix is working if you see:**
1. Log shows `Template mapping for AttributeType: 'Attribute'` (not 'null')
2. Log shows `Created 6 rows` for SDDAttributeGenerator (not 0)
3. Generated SDD has 9 rows in Dictionary Mapping (not 3)
4. Triplestore contains 6 SDDAttributes (not 0)
5. Round-trip works: ingest → generate → ingest produces same result

## Troubleshooting

If the fix doesn't work:

1. **Check if you recompiled** - The Java code needs to be recompiled
2. **Check if you restarted** - The application needs to be restarted to load new code
3. **Check the triplestore** - Old data might interfere; uningest first
4. **Check the logs** - Look for the specific error messages mentioned above

## Files Changed

- ✅ `app/org/hascoapi/ingestion/SDDAttributeGenerator.java`
  - Removed `Templates templates;` field declaration (line 31)
  - Removed `this.templates = new Templates(templateFile);` (line 50)
  - Removed `import org.hascoapi.utils.Templates;` (line 20)

No other files need to be changed.

## Date Applied

2026-03-19

