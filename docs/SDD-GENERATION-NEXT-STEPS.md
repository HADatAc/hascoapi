# SDD Dictionary Mapping Generation - FIXED!

**Date:** March 19, 2026  
**Status:** ✅ ROOT CAUSE FOUND AND FIXED

## Problem Summary

The generated SDD files have empty Dictionary Mapping sheets except for 3 SDDObject rows (??weather, ??observation, ??instant). The expected 6 SDDAttribute rows (Timestamp, ESP32_Chip_ID, Temperature, air_quality, humidity, Pressure_Baro) are missing.

## Root Cause ✅ IDENTIFIED

**Configuration Section Name Mismatch in Templates.java**

The `Templates.java` file was looking for configuration section `"SDDA"` but the template configuration file (`template.generic.conf`) defines the section as `"DASA"`.

### Evidence from Logs:

```
[SDDAttributeGenerator] Template mapping for AttributeType: 'null'
[SDDAttributeGenerator]   attribute (from column 'null')=''
```

This `null` value occurs because:
1. `Templates.getATTRIBUTETYPE()` calls `iniConfig.getSection("SDDA")`
2. Section "SDDA" doesn't exist in the config file
3. Returns `null` instead of `"Attribute"`
4. `SDDAttributeGenerator` tries to read column `null` from Excel
5. Gets empty string, skips all 6 SDDAttribute rows

### The Bug:

**File:** `app/org/hascoapi/utils/Templates.java` (lines 156-167)

**Before (WRONG):**
```java
// SDDA, SDDE, SDDO Template (Part of SDD)
public String getLABEL() { return iniConfig.getSection("SDDA").getString("Label"); }
public String getATTRIBUTETYPE() { return iniConfig.getSection("SDDA").getString("AttributeType"); }
// ... all other getters using "SDDA"
```

**After (CORRECT):**
```java
// DASA, DASE, DASO Template (Part of SDD)
public String getLABEL() { return iniConfig.getSection("DASA").getString("Label"); }
public String getATTRIBUTETYPE() { return iniConfig.getSection("DASA").getString("AttributeType"); }
// ... all other getters using "DASA"
```

### Why This Happened:

The config file uses section name `[DASA]` (Dictionary Attribute Semantic Annotation):
```ini
[DASA]
# DASA, DASE, DASO Template (Part of SDD)
Label=Column 
AttributeType=Attribute
AttributeOf=attributeOf
Unit=Unit
Time=Time
Entity=Entity
Role=Role
Relation=Relation
InRelationTo=inRelationTo
WasDerivedFrom=wasDerivedFrom
WasGeneratedBy=wasGeneratedBy
HasPosition=hasPosition
```

But the Java code was looking for `[SDDA]` (likely a typo - SDD Attribute).

## Fix Applied ✅

Changed all occurrences of `"SDDA"` to `"DASA"` in `Templates.java`:
- `getLABEL()`
- `getATTRIBUTETYPE()`
- `getATTTRIBUTEOF()`
- `getUNIT()`
- `getTIME()`
- `getENTITY()`
- `getROLE()`
- `getRELATION()`
- `getINRELATIONTO()`
- `getWASDERIVEDFROM()`
- `getWASGENERATEDBY()`

## Expected Result After Fix

### During Ingestion:
```
[SDDAttributeGenerator] Template mapping for AttributeType: 'Attribute'
[SDDAttributeGenerator] Processing record #1:
[SDDAttributeGenerator]   label='Timestamp'
[SDDAttributeGenerator]   attribute (from column 'Attribute')='time:inXSDDateTime'
[SDDAttributeGenerator]   attribute is null: false
[SDDAttributeGenerator]   attribute is empty: false
[SDDAttributeGenerator]   -> Creating SDDAttribute row
```

### Triple Store Results:
- 6 SDDAttribute entities created (for data columns)
- 3 SDDObject entities created (for ??weather, ??observation, ??instant)
- Total: 9 entities

### During Generation:
- Dictionary Mapping sheet populated with 9 rows
- First 6 rows: Column name + Attribute + attributeOf + Unit + Time
- Last 3 rows: Column name (??xxx) + Entity + Relation + inRelationTo

## Testing Instructions

1. **Recompile the application**
   ```bash
   sbt compile
   ```

2. **Re-ingest the SDD file**
   - Upload `SDD-WS.xlsx` through the UI
   - Check logs for `[SDDAttributeGenerator]` messages
   - Verify `Template mapping for AttributeType: 'Attribute'` (not 'null')
   - Verify "Creating SDDAttribute row" appears 6 times

3. **Generate a new SDD**
   - Use the Generate form
   - Open the generated file
   - Check Dictionary Mapping sheet has 9 rows total

4. **Verify roundtrip test**
   - Original SDD → Ingest → Generate → Should match structure

## Files Modified

- `app/org/hascoapi/utils/Templates.java` - Fixed section name from "SDDA" to "DASA"
- `docs/SDD-GENERATION-NEXT-STEPS.md` - Updated with solution

---

This was a simple but critical typo that prevented the entire SDDAttribute ingestion pipeline from working!

