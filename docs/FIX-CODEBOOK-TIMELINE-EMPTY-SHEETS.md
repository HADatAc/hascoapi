# Fix: Codebook and Timeline Sheets Empty After Generation

## Problem
When generating an SDD file from ingested data, the Codebook and Timeline sheets only contained headers (first line) but no data rows, even when PossibleValues and Timeline objects existed in the triple store.

## Root Cause
The SDD ingestion process had **missing method calls** during file upload:

1. **`readCodebook(RecordFile)`** method existed in `SDD.java` but was **never called** during ingestion
2. **`readTimeline(RecordFile)`** method existed in `SDD.java` but was **never called** during ingestion

These methods are responsible for:
- Reading the Codebook sheet and populating `SDD.codebook` internal data structure
- Reading the Timeline sheet and populating `SDD.timeline` internal data structure

Without calling these methods during ingestion:
- The internal `codebook` map remained empty
- The internal `timeline` map remained empty
- PossibleValue and Timeline entities were created in the triple store but without proper linkage
- During generation, queries couldn't find the data because the internal structures were never populated during ingestion

## Solution
Added calls to `readCodebook()` and `readTimeline()` in `AnnotateSDD.java`, following the same pattern used for `readCodeMapping()`:

### Changes in `AnnotateSDD.java`

```java
// Read Codebook sheet if it exists
if (mapCatalog.containsKey("Codebook") && mapCatalog.get("Codebook") != null && !mapCatalog.get("Codebook").isEmpty()) {
    String codebookSheetName = mapCatalog.get("Codebook").replace("#", "");
    dataFile.getLogger().println("Reading Codebook sheet: " + codebookSheetName);
    RecordFile codebookRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), codebookSheetName);
    if (codebookRecordFile.isValid()) {
        if (sdd.readCodebook(codebookRecordFile)) {
            dataFile.getLogger().println("Codebook data loaded successfully from " + codebookSheetName);
        } else {
            dataFile.getLogger().println("Codebook sheet is empty or invalid");
        }
    } else {
        dataFile.getLogger().printWarningByIdWithArgs("SDD_00019", "Codebook sheet not found: " + codebookSheetName);
    }
}

// Read Timeline sheet if it exists
if (mapCatalog.containsKey("Timeline") && mapCatalog.get("Timeline") != null && !mapCatalog.get("Timeline").isEmpty()) {
    String timelineSheetName = mapCatalog.get("Timeline").replace("#", "");
    dataFile.getLogger().println("Reading Timeline sheet: " + timelineSheetName);
    RecordFile timelineRecordFile = new SpreadsheetRecordFile(dataFile.getFile(), dataFile.getFilename(), timelineSheetName);
    if (timelineRecordFile.isValid()) {
        if (sdd.readTimeline(timelineRecordFile)) {
            dataFile.getLogger().println("Timeline data loaded successfully from " + timelineSheetName);
        } else {
            dataFile.getLogger().println("Timeline sheet is empty or invalid");
        }
    } else {
        dataFile.getLogger().printWarningByIdWithArgs("SDD_00019", "Timeline sheet not found: " + timelineSheetName);
    }
}
```

These calls are placed **before** the generator chain setup, ensuring the internal data structures are populated before PVGenerator and other generators run.

## How It Works

### Ingestion Flow (Now Fixed)
1. Upload SDD file with Codebook and Timeline sheets
2. `AnnotateSDD.exec()` creates an `SDD` object
3. **NEW**: `sdd.readCodebook()` reads the Codebook sheet and populates `SDD.codebook` map
4. **NEW**: `sdd.readTimeline()` reads the Timeline sheet and populates `SDD.timeline` map
5. `PVGenerator` creates PossibleValue entities with proper linkage to SDDAttributes
6. Timeline data is stored with proper structure

### Generation Flow (Benefits from Fix)
1. Generate SDD from triple store
2. `SDDGen.addCodebookData()` queries for PossibleValues linked to the schema
3. Found PossibleValues are added to the Codebook sheet via `SDDCodebook.add()`
4. `SDDGen.addTimelineData()` queries for Timeline objects with `hasco:TimeRole`
5. Found Timeline objects are added to the Timeline sheet via `SDDTimeline.add()`

## Files Modified
- `app/org/hascoapi/ingestion/AnnotateSDD.java` - Added `readCodebook()` and `readTimeline()` calls

## Testing
To verify the fix:

1. **Upload an SDD** with Codebook and Timeline data
2. Check ingestion logs for:
   ```
   Reading Codebook sheet: Codebook
   Codebook data loaded successfully from Codebook
   Reading Timeline sheet: Timeline
   Timeline data loaded successfully from Timeline
   ```
3. **Generate an SDD** from the same data
4. Verify the generated file has:
   - Codebook sheet with multiple data rows (not just headers)
   - Timeline sheet with data rows (if Timeline objects exist with hasco:TimeRole)

## Related Issues
- Similar pattern to how `readCodeMapping()` was already working correctly
- This fix ensures consistency across all SDD sheet reading operations
- The generation code was already correct; only ingestion was missing these calls

## Date
2026-03-20

