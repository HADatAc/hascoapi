# Fix: Codebook and Timeline Sheets Empty - PossibleValue Linkage Issue

## Problem Summary
After ingesting an SDD with Codebook and Timeline sheets, when generating the SDD back from the triple store, both sheets contained only headers with no data rows, even though:
1. The Codebook/Timeline sheets were successfully read during ingestion
2. PossibleValue and Timeline entities were created in the triple store
3. The debug logs showed PossibleValues existed but had `schema=null`

## Root Cause Analysis

### The Core Issue
The PossibleValues were not properly linked to their SDDAttribute URIs. During ingestion:

1. **SDDAttributeGenerator** creates SDDAttribute entities with URIs like:
   - `https://hadatac.org/ont/hadatac#/SDDATT1773938255113971/1` for "Patient_ID"
   - `https://hadatac.org/ont/hadatac#/SDDATT1773938255113971/6` for "Gender"

2. **PVGenerator** needs to link PossibleValues to these SDDAttribute URIs via `hasco:isPossibleValueOf` property

3. **THE PROBLEM**: `PVGenerator.getPVvalue()` received `sdd.getMapAttrObj()` which contained:
   ```java
   {
       "Gender" -> "??patient"  // Maps to attributeOf, NOT to SDDAttribute URI!
   }
   ```

4. Since the column name wasn't found in the map, `getPVvalue()` returned just the column name string "Gender"

5. The PossibleValue was created with:
   ```turtle
   <PSV.../1> hasco:isPossibleValueOf "Gender" .  # Wrong! Should be URI
   ```

6. During generation, the query failed:
   ```sparql
   SELECT DISTINCT ?uri WHERE {
       ?uri a hasco:PossibleValue .
       ?uri hasco:isPossibleValueOf ?attr .       # Expects URI, not string
       ?attr hasco:partOfSchema <...SDDICT...> .  # Can't navigate from string
   }
   ```

## Solution Implemented

### Changes to SDDAttributeGenerator

1. **Added `labelToUriMap` field**:
   ```java
   // Map of column label -> SDDAttribute URI (created during createRows)
   private Map<String, String> labelToUriMap = new HashMap<String, String>();
   ```

2. **Populate the map during row creation**:
   ```java
   // In createRows() method, after creating each SDDAttribute:
   String columnLabel = getLabel(record);
   String sddAttUri = (String) newRow.get("hasURI");
   if (columnLabel != null && !columnLabel.isEmpty() && sddAttUri != null) {
       labelToUriMap.put(columnLabel, sddAttUri);
       System.out.println("[SDDAttributeGenerator]   -> Stored label->URI mapping: '" + 
           columnLabel + "' -> '" + sddAttUri + "'");
   }
   ```

3. **Added getter method**:
   ```java
   public Map<String, String> getLabelToUriMap() {
       return labelToUriMap;
   }
   ```

### Changes to AnnotateSDD

Modified the generator chain setup to capture the SDDAttributeGenerator instance and pass its `labelToUriMap` to PVGenerator:

```java
// Create SDDAttributeGenerator and store reference
final SDDAttributeGenerator[] sddAttributeGeneratorRef = new SDDAttributeGenerator[1];

// first Data_Dictionary -> SDDAttributeGenerator
addCustomGeneratorIfSheetExists(dataFile, mapCatalog, "Data_Dictionary", "", chain, (df, status) -> {
    try {
        DataFile clonedFile = (DataFile) df.clone();
        SDD localSdd = new SDD(clonedFile, templateFile);
        localSdd.readDataDictionary(clonedFile.getRecordFile(), clonedFile);
        SDDAttributeGenerator generator = new SDDAttributeGenerator(clonedFile, sddUri, sddId, 
            sdd.getCodeMapping(), localSdd.readDDforEAmerge(clonedFile.getRecordFile()), templateFile);
        sddAttributeGeneratorRef[0] = generator; // Store reference
        return generator;
    } catch (CloneNotSupportedException e) {
        dataFile.getLogger().printExceptionByIdWithArgs("SDD_00020", e.getMessage());
        return null;
    }
});

// Codebook -> PVGenerator (now uses labelToUriMap)
addCustomGeneratorIfSheetExists(dataFile, mapCatalog, "Codebook", "", chain, (df, status) -> {
    try {
        DataFile clonedFile = (DataFile) df.clone();
        chain.setCodebookFile(clonedFile);
        chain.setSddName(URIUtils.replacePrefixEx(sddUri));
        
        // Use the labelToUriMap from SDDAttributeGenerator
        Map<String, String> labelToUriMap = (sddAttributeGeneratorRef[0] != null) 
            ? sddAttributeGeneratorRef[0].getLabelToUriMap() 
            : new HashMap<String, String>();
        
        dataFile.getLogger().println("[AnnotateSDD] Passing labelToUriMap to PVGenerator with " + 
            labelToUriMap.size() + " entries");
        
        return new PVGenerator(clonedFile, sddUri, sddId, labelToUriMap, sdd.getCodeMapping());
    } catch (CloneNotSupportedException e) {
        dataFile.getLogger().printExceptionByIdWithArgs("SDD_00020", e.getMessage());
        return null;
    }
});
```

## How It Works Now

### Ingestion Flow (Fixed)
1. Upload SDD file with Codebook sheet containing:
   ```
   Column | Code | Label  | Class
   Gender | 1    | Male   | ncit:C46109
   Gender | 2    | Female | ncit:C46110
   ```

2. **SDDAttributeGenerator** runs first:
   - Creates SDDAttribute for "Gender" with URI: `https://hadatac.org/ont/hadatac#/SDDATT.../6`
   - Stores mapping: `{"Gender" -> "https://hadatac.org/ont/hadatac#/SDDATT.../6"}`

3. **PVGenerator** runs second:
   - Receives `labelToUriMap` with proper URIs
   - For each Codebook row, calls `getPVvalue("Gender")`
   - Finds "Gender" in map, returns the SDDAttribute URI
   - Creates PossibleValue with:
     ```turtle
     <PSV.../1> a hasco:PossibleValue ;
                hasco:hasVariable "Gender" ;
                hasco:hasCode "1" ;
                hasco:hasCodeLabel "Male" ;
                hasco:hasClass ncit:C46109 ;
                hasco:isPossibleValueOf <SDDATT.../6> .  # ✅ Correct URI!
     ```

### Generation Flow (Now Works)
1. Generate SDD from triple store
2. Query for PossibleValues:
   ```sparql
   SELECT DISTINCT ?uri WHERE {
       ?uri a hasco:PossibleValue .
       ?uri hasco:isPossibleValueOf ?attr .        # ✅ Finds URI link
       ?attr hasco:partOfSchema <...SDDICT...> .   # ✅ Can navigate to schema
   }
   ```

3. Query succeeds, finds 2 PossibleValues
4. Adds them to Codebook sheet via `SDDCodebook.add()`
5. Generated file has populated Codebook sheet!

## Files Modified
- `app/org/hascoapi/ingestion/SDDAttributeGenerator.java`
  - Added `labelToUriMap` field
  - Populate map in `createRows()`
  - Added `getLabelToUriMap()` getter

- `app/org/hascoapi/ingestion/AnnotateSDD.java`
  - Capture SDDAttributeGenerator instance
  - Pass `labelToUriMap` to PVGenerator instead of `sdd.getMapAttrObj()`

## Testing

### Expected Log Output During Ingestion
```
[SDDAttributeGenerator] Processing record #6:
[SDDAttributeGenerator]   label='Gender'
[SDDAttributeGenerator]   -> Creating SDDAttribute row
[SDDAttributeGenerator]   -> Row created with URI: https://hadatac.org/ont/hadatac#/SDDATT.../6
[SDDAttributeGenerator]   -> Stored label->URI mapping: 'Gender' -> 'https://hadatac.org/ont/hadatac#/SDDATT.../6'
...
[AnnotateSDD] Passing labelToUriMap to PVGenerator with 6 entries
...
[PVGenerator] Linked PossibleValue for column 'Gender' to SDDAttribute (via mapAttrObj): https://hadatac.org/ont/hadatac#/SDDATT.../6
```

### Expected Log Output During Generation
```
[SDDGen] Codebook query: SELECT DISTINCT ?uri WHERE { ... }
[SDDGen] Found 2 possible values
[SDDGen] Adding 2 PossibleValues to Codebook sheet...
[SDDGen] Processing PossibleValue #1: https://hadatac.org/ont/hadatac#/PSV.../1
[SDDGen]   Variable: Gender
[SDDGen]   Code: 1
[SDDGen]   CodeLabel: Male
[SDDGen]   Class: http://purl.obolibrary.org/obo/NCIT_C46109
[SDDCodebook] PossibleValue row added successfully at row 2
```

## Timeline Sheet

### Timeline Fix
**Problem**: Timeline sheet was read but data was never persisted to the triple store, so generation couldn't find any timeline objects.

**Solution**: Created `TimelineGenerator` to process the Timeline sheet and create SDDObject entities with `hasco:TimeRole`.

### Changes for Timeline

1. **Created TimelineGenerator.java**:
   ```java
   public class TimelineGenerator extends BaseGenerator {
       // Reads Timeline sheet columns: Name, Label, Type, Start, End, Unit
       // Creates SDDObject with hasco:TimeRole
       
       @Override
       public Map<String, Object> createRow(Record rec, int rowNumber) {
           row.put("hasURI", timelineObjUri);
           row.put("a", "hasco:SDDObject");
           row.put("hasco:hasRole", "hasco:TimeRole");  // CRITICAL!
           row.put("hasco:hasEntity", type);
           row.put("hasco:hasStart", start);
           row.put("hasco:hasEnd", end);
           row.put("hasco:hasUnit", unit);
           // ...
       }
   }
   ```

2. **Added to AnnotateSDD.java**:
   ```java
   // Timeline -> TimelineGenerator
   addCustomGeneratorIfSheetExists(dataFile, mapCatalog, "Timeline", "", chain, (df, status) -> {
       try {
           DataFile clonedFile = (DataFile) df.clone();
           dataFile.getLogger().println("[AnnotateSDD] Creating TimelineGenerator for Timeline sheet");
           return new TimelineGenerator(clonedFile, sddUri, sddId);
       } catch (CloneNotSupportedException e) {
           dataFile.getLogger().printExceptionByIdWithArgs("SDD_00020", e.getMessage());
           return null;
       }
   });
   ```

### How Timeline Works Now

**Ingestion**:
1. Timeline sheet with data like:
   ```
   Name                   | Label                      | Type          | Start                  | End                    | Unit
   ??observation_period   | Patient Observation Period | time:Interval | 2026-03-01T00:00:00Z | 2026-03-31T23:59:59Z | unit:DAY
   ```

2. TimelineGenerator creates SDDObject:
   ```turtle
   <SDDOBJ.../timeline/1> a hasco:SDDObject ;
       rdfs:label "??observation_period" ;
       hasco:hasEntity time:Interval ;
       hasco:hasRole hasco:TimeRole ;  # ← KEY for generation query
       hasco:hasStart "2026-03-01T00:00:00Z" ;
       hasco:hasEnd "2026-03-31T23:59:59Z" ;
       hasco:hasUnit unit:DAY ;
       hasco:partOfSchema <SDDICT...> .
   ```

**Generation**:
1. Query finds objects with `hasco:hasRole hasco:TimeRole`
2. Adds them to Timeline sheet
3. Timeline sheet is populated!

**Note**: Virtual objects like `??instant`, `??observation` (without explicit TimeRole) belong in Dictionary Mapping, NOT Timeline.

## Files Modified

### Codebook Fix
- `app/org/hascoapi/ingestion/SDDAttributeGenerator.java` - Added labelToUriMap
- `app/org/hascoapi/ingestion/AnnotateSDD.java` - Pass labelToUriMap to PVGenerator

### Timeline Fix
- `app/org/hascoapi/ingestion/TimelineGenerator.java` - **NEW FILE** - Generator for Timeline sheet
- `app/org/hascoapi/ingestion/AnnotateSDD.java` - Added TimelineGenerator to chain

## Previous Related Fix

This fix builds on the earlier fix that added `readCodebook()` and `readTimeline()` calls in AnnotateSDD.
- **First fix**: Ensured sheets are read during ingestion
- **Second fix (Codebook)**: Ensures PossibleValues are properly linked to SDDAttributes via URIs
- **Third fix (Timeline)**: Ensures Timeline data is persisted as SDDObjects with TimeRole

## Date
2026-03-20

