# SDD Generation Debugging - Empty Sheets Issue

## Problem Summary
When generating an SDD file, all data sheets (Dictionary Mapping, Codebook, Timeline) are empty, even though the SDD was successfully ingested with data.

## Root Cause Analysis

### What's Happening
1. **Ingestion Phase (Working)**:
   - SDD file is uploaded and ingested
   - S

DDObjects (like `??weather`, `??observation`, `??instant`) ARE created - count = 3
   - SDDAttributes (like Temperature, humidity, etc.) are NOT created - count = 0

2. **Generation Phase (Failing)**:
   - System finds the SDD with 3 entities (SDDObjects only)
   - Queries for SDDAttributes returns 0 results
   - Queries for SDDObjects could potentially work, but sheets remain empty

### Why SDDAttributes Aren't Being Created During Ingestion

The SDDAttributeGenerator's `createRows()` method has this logic:
```java
String attr = getAttribute(record);
if (attr == null || attr.equals("")) {
    System.out.println("[SDDAttributeGenerator]   -> SKIPPED (empty attribute)");
    continue;
}
```

This means:
- If the "Attribute" column is empty, the row is skipped
- Only rows with non-empty "Attribute" values become SDDAttributes
- Rows without "Attribute" values (the `??entity` rows) are handled differently

### Expected Behavior

For the Weather Station SDD file:

**SDDAttributes (6 expected)**:
- Timestamp → time:inXSDDateTime
- ESP32_Chip_ID → hasco:originalID  
- Temperature → envo:09200001
- air_quality → envo:01000432
- humidity → pato:0015009
- Pressure_Baro → envo:09200011

**SDDObjects (3 expected)**:
- ??weather → envo:01001079
- ??observation → time:Interval
- ??instant → time:Instant

### Current Behavior

**SDDAttributes**: 0 (WRONG - should be 6)
**SDDObjects**: 3 (CORRECT)

## Debug Steps Added

Added logging to check if ANY SDDAttributes exist in the triple store:
```java
String debugQuery = ... + " SELECT DISTINCT ?uri ?schema WHERE { "
                + "   ?uri a hasco:SDDAttribute . "
                + "   OPTIONAL { ?uri hasco:partOfSchema ?schema } "
                + " } LIMIT 10";
```

## Next Steps

1. **Test with Debug Logging**:
   - Ingest a fresh SDD file
   - Check the logs for:
     - `[SDDAttributeGenerator] Processing record: label='...', attribute='...'`
     - `[SDDAttributeGenerator]   -> Creating SDDAttribute row` (should see 6 times)
     - `[SDDAttributeGenerator] createRows() END - total rows created: X` (should be 6)

2. **If SDDAttributes Are Created But Not Queried**:
   - Issue is in the SPARQL query (wrong schema URI)
   - Check that `hasco:partOfSchema` is being set correctly

3. **If SDDAttributes Are NOT Created**:
   - Issue is in SDDAttributeGenerator reading the Excel file
   - The "Attribute" column values are not being read correctly
   - Need to check how Records are created from the Dictionary Mapping sheet

## Potential Fixes

### If Issue Is in Query (Schema URI Mismatch)
- Verify that during ingestion, SDDAttributes get `hasco:partOfSchema` = the SDDICT URI
- The SDDICT URI should be created from the DataFile URI: `DFL123` → `SDDICT123`

### If Issue Is in Record Reading
- Check how the Dictionary Mapping sheet is parsed
- Verify that the "Attribute" column is correctly identified
- May need to add logging in the SpreadsheetRecordFile parsing

## Test Command

```powershell
# Restart the server to pick up new logging
cd C:\Users\kaell\Desktop\Project\hascoapi
sbt run

# In another terminal, test SDD ingestion
# Upload WKF-WeatherStation.xlsx via frontend
# Check logs for [SDDAttributeGenerator] messages
```

## Expected Log Output (Successful Case)

```
[SDDAttributeGenerator] createRows() START - total records: 9
[SDDAttributeGenerator] Processing record: label='Timestamp', attribute='time:inXSDDateTime'
[SDDAttributeGenerator]   -> Creating SDDAttribute row
[SDDAttributeGenerator]   -> Row created with URI: ...
[SDDAttributeGenerator]   -> Rows list now has 1 rows
...
[SDDAttributeGenerator] createRows() END - total rows created: 6
```

## Actual Log Output (Current Failure)

```
[SDDAttributeGenerator] createRows() END - total rows created: 0
```

This confirms NO SDDAttributes are being created during ingestion.

