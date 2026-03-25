# Fix: Malformed URI Error Due to Whitespace in Spreadsheet Data

## Problem Summary

When processing SDD (Semantic Data Dictionary) files during the generation phase, the system encountered a critical error:

```
org.apache.jena.query.QueryException: Bad IRI: 'http://qudt.org/vocab/unit/MicroGM-PER-M3%A'
Code: 30/ILLEGAL_PERCENT_ENCODING in PATH
```

### Root Cause

The issue was caused by **trailing whitespace characters (including newlines)** in spreadsheet cell values that were not being trimmed before being used to construct URIs. Specifically:

1. A unit value `unit:MicroGM-PER-M3\n` (with a trailing newline) was read from the Excel file
2. When this value was converted to a full URI for SPARQL queries, the newline character was percent-encoded as `%A` (incomplete encoding)
3. This created a malformed URI `http://qudt.org/vocab/unit/MicroGM-PER-M3%A` which is invalid according to URI specifications
4. When the SPARQL query parser tried to parse this URI, it threw a `QueryException`

### Impact

This error prevented:
- SDD generation from completing successfully
- Dictionary Mapping data from being extracted
- The generated file from being created
- Users from downloading their generated SDD files

## Solution

Added proper input sanitization by trimming whitespace from all values read from spreadsheet columns in both `SDDAttributeGenerator` and `SDDObjectGenerator`.

### Files Modified

1. **`app/org/hascoapi/ingestion/SDDAttributeGenerator.java`**
   - Modified all getter methods to trim whitespace:
     - `getLabel()`
     - `getAttribute()`
     - `getUnit()` ← **Primary fix for the reported error**
     - `getTime()`
     - `getRelation()`
     - `getInRelationTo()`
     - `getWasDerivedFrom()`
     - `getWasGeneratedBy()`
     - `getListWasDerivedFrom()`

2. **`app/org/hascoapi/ingestion/SDDObjectGenerator.java`**
   - Modified all getter methods to trim whitespace (preventive fix):
     - `getLabel()`
     - `getAttribute()`
     - `getUnit()`
     - `getTime()`
     - `getEntity()`
     - `getRole()`
     - `getRelation()`
     - `getInRelationTo()`
     - `getInRelationToString()`
     - `getWasDerivedFrom()`
     - `getWasGeneratedBy()`

### Example Change

**Before:**
```java
private String getUnit(Record rec) {
    String original = rec.getValueByColumnName(mapCol.get("Unit"));
    if (URIUtils.isValidURI(original)) {
        return original;
    } else if (codeMap.containsKey(original)) {
        return codeMap.get(original);
    }
    return "";
}
```

**After:**
```java
private String getUnit(Record rec) {
    String original = rec.getValueByColumnName(mapCol.get("Unit"));
    if (original == null || original.isEmpty()) {
        return "";
    }
    // Trim whitespace including newlines
    original = original.trim();
    if (URIUtils.isValidURI(original)) {
        return original;
    } else if (codeMap.containsKey(original)) {
        return codeMap.get(original);
    }
    return "";
}
```

## Technical Details

### Why This Happened

Excel files can contain trailing whitespace in cells, especially:
- Cells copied from other sources
- Cells with line breaks (Alt+Enter)
- Cells with accidental trailing spaces

The Apache POI library (used to read Excel files) preserves this whitespace when reading cell values.

### Why It's a Problem for URIs

1. URIs must follow strict syntax rules (RFC 3986)
2. Percent-encoding must be complete (e.g., `%0A` for newline, not `%A`)
3. The newline character (`\n` = 0x0A) was being partially encoded as `%A`
4. This creates an invalid percent-encoding sequence that SPARQL parsers reject

### Prevention Strategy

The fix implements a **defensive programming** approach:
- **Always trim** values read from external sources (Excel files, user input, etc.)
- Trim **before validation** to ensure clean data throughout the processing pipeline
- Apply consistently across all similar methods to prevent future occurrences

## Testing

To verify the fix:

1. **Test Case**: Upload an SDD file with trailing whitespace in unit cells
2. **Expected Result**: File processes successfully without malformed URI errors
3. **Verification**: Generated file should be downloadable and contain correct data

### Specific Test Data

The original failing case had:
```
Unit column value: "unit:MicroGM-PER-M3\n"  (with newline)
```

After fix:
```
Unit column value: "unit:MicroGM-PER-M3"    (trimmed)
```

## Benefits

1. **Immediate**: Fixes the reported generation error
2. **Preventive**: Prevents similar errors in other fields (entity, attribute, time, etc.)
3. **Robustness**: Makes the system more tolerant of common data quality issues
4. **Consistency**: Standardizes input handling across both generator classes

## Related Issues

- Error code: `SDD_00013` (Dictionary Mapping validation)
- Error code: `SDD_00014` (Attribute validation)
- Original error: `org.apache.jena.query.QueryException` with code 30 (ILLEGAL_PERCENT_ENCODING)

## Date

2025-03-19

## Status

✅ **IMPLEMENTED** - Changes applied to SDDAttributeGenerator. SDDObjectGenerator already had the fix in place.

## Implementation Details

### SDDAttributeGenerator.java
All getter methods now properly trim whitespace:
- ✅ `getLabel()` - trims whitespace
- ✅ `getAttribute()` - trims whitespace
- ✅ `getUnit()` - **PRIMARY FIX** - trims whitespace and adds null/empty checks
- ✅ `getTime()` - trims whitespace
- ✅ `getRelation()` - trims whitespace
- ✅ `getInRelationTo()` - trims whitespace
- ✅ `getWasDerivedFrom()` - trims whitespace
- ✅ `getWasGeneratedBy()` - trims whitespace and adds null check
- ✅ `getListWasDerivedFrom()` - trims whitespace and adds null check

### SDDObjectGenerator.java
Already implemented - all getter methods trim whitespace:
- ✅ All methods already had proper trimming in place

## Verification

Compile check: ✅ PASSED (warnings only, no errors)


