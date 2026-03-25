# Complete Timeline Support Implementation

## Issue Resolved
Timeline sheet was being generated with only Name and Type populated, missing Start, End, and Unit values even though they were in the original file.

## Root Cause
1. TimelineGenerator was saving Start, End, Unit as properties in the triple store
2. SDDObject class didn't have fields to hold these values
3. SDDTimeline.add() couldn't retrieve the values during generation

## Solution

### 1. Added Fields to SDDObject.java
```java
// Timeline-specific properties
@PropertyField(uri = "hasco:hasStart")
private String hasStart;

@PropertyField(uri = "hasco:hasEnd")
private String hasEnd;

@PropertyField(uri = "hasco:hasUnit")
private String hasUnit;

// Getters and setters
public String getHasStart() { return hasStart; }
public void setHasStart(String hasStart) { this.hasStart = hasStart; }

public String getHasEnd() { return hasEnd; }
public void setHasEnd(String hasEnd) { this.hasEnd = hasEnd; }

public String getHasUnit() { return hasUnit; }
public void setHasUnit(String hasUnit) { this.hasUnit = hasUnit; }
```

### 2. Updated SDDTimeline.add()
```java
// Start - temporal start value
String start = obj.getHasStart();
if (start != null && !start.isEmpty()) {
    row.createCell(col).setCellValue(start);
}
col++;

// End - temporal end value
String end = obj.getHasEnd();
if (end != null && !end.isEmpty()) {
    row.createCell(col).setCellValue(end);
}
col++;

// Unit - temporal unit - USE PREFIXED FORM
String unit = obj.getHasUnit();
if (unit != null && !unit.isEmpty()) {
    String unitShort = URIUtils.replaceNameSpaceEx(unit);
    row.createCell(col).setCellValue(unitShort);
    if (helper != null) {
        helper.registerPrefixFromUri(unitShort);
    }
}
col++;
```

### 3. Fixed Label vs Name
- **Name** (rdfs:label): Identifier (e.g., `??observation_period`)
- **Label** (rdfs:comment): Human-readable description (e.g., `Patient Observation Period`)

Updated SDDTimeline.add() to use:
```java
String name = obj.getLabel();      // Name column
String label = obj.getComment();   // Label column (description)
```

## Complete Flow

### Ingestion
```
Timeline Sheet:
Name                   | Label                      | Type          | Start                  | End                    | Unit
??observation_period   | Patient Observation Period | time:Interval | 2026-03-01T00:00:00Z  | 2026-03-31T23:59:59Z  | unit:DAY

↓ TimelineGenerator creates ↓

SDDObject:
<SDDOBJ.../timeline/1>
    rdfs:label "??observation_period"
    rdfs:comment "Patient Observation Period"
    hasco:hasEntity time:Interval
    hasco:hasRole hasco:TimeRole
    hasco:hasStart "2026-03-01T00:00:00Z"
    hasco:hasEnd "2026-03-31T23:59:59Z"
    hasco:hasUnit unit:DAY
    hasco:partOfSchema <SDDICT...>
```

### Generation
```
Query: SDDObjects with hasco:TimeRole

↓ SDDObject.find() loads all properties ↓

SDDTimeline.add() writes:
Name                   | Label                      | Type          | Start                  | End                    | Unit
??observation_period   | Patient Observation Period | time:Interval | 2026-03-01T00:00:00Z  | 2026-03-31T23:59:59Z  | unit:DAY
```

## Files Modified
- `app/org/hascoapi/entity/pojo/SDDObject.java` - Added hasStart, hasEnd, hasUnit fields
- `app/org/hascoapi/transform/mt/sdd/SDDTimeline.java` - Updated to populate all Timeline columns
- `app/org/hascoapi/ingestion/TimelineGenerator.java` - Stores temporal properties correctly

## Testing

### Expected Ingestion Log
```
[TimelineGenerator] Created Timeline object: ??observation_period with TimeRole
12 triple(s) have been committed to triple store
```

### Expected Generation Log
```
[SDDGen] Found 1 timeline objects (with hasco:TimeRole)
[SDDTimeline] Adding timeline object: https://hadatac.org/ont/hadatac#/SDDOBJ.../timeline/1
[SDDTimeline] Timeline object added at row 1
```

### Expected Generated Timeline Sheet
All columns populated with original data:
- ✅ Name
- ✅ Label  
- ✅ Type
- ✅ Start
- ✅ End
- ✅ Unit

## Date
2026-03-20

