# WKF Task URI Concatenation Fix

## Problem

When opening a WKF workflow in the frontend, the following error occurs:

```
Bad IRI: 'https://pmsr.net/ont/WKF_PMSR_SIM_0001/RIN/0001%20%7C%20https://pmsr.net/ont/WKF_PMSR_SIM_0001/RIN/0002%20%7C%20https://pmsr.net/ont/WKF_PMSR_SIM_0001/RIN/0003'
Code: 0/ILLEGAL_CHARACTER in FRAGMENT
```

Where:
- `%20` = URL-encoded space character
- `%7C` = URL-encoded pipe character `|`

## Root Cause

The issue occurs in the **roundtrip** workflow:

1. **Initial Excel → Ingestion**: 
   - Excel contains: `pmsr:/RIN/0001 | pmsr:/RIN/0002 | pmsr:/RIN/0003`
   - `WKFGenerator.splitMultiValueProperty()` correctly splits into List
   - `MetadataFactory.createModel()` creates 3 separate triples (CORRECT)

2. **Triplestore → Regeneration**: 
   - Task is retrieved from triplestore with `Task.find(uri)`
   - Each triple is processed correctly
   - Multiple URIs are stored in `hasRequiredInstrumentUris` list

3. **Regeneration → Excel Export**:
   - `WKFTasks.joinUriList()` joins URIs with ` | ` separator
   - Excel cell contains: `pmsr:/RIN/0001 | pmsr:/RIN/0002 | pmsr:/RIN/0003` (CORRECT)

4. **Re-ingestion** (the problem):
   - Excel is re-ingested
   - **BUT** somehow the data in the triplestore ends up as a SINGLE concatenated string instead of multiple triples
   - This could be because the split is not happening OR the storage is incorrect

5. **Frontend API Call**:
   - When frontend calls `URIPage.getUri()` for a Task
   - `Task.find()` retrieves the concatenated string as a SINGLE value
   - `getRequiredInstrument()` tries to use it as a URI
   - Apache Jena rejects the malformed URI containing spaces and pipes

## Investigation Needed

The fix applied to `Task.java` is a **workaround** that splits concatenated URIs when reading from the triplestore. However, the ROOT CAUSE needs to be identified:

**Question**: Why is the data being stored as a single concatenated string in the triplestore instead of multiple triples?

Possible causes:
1. The `splitMultiValueProperty()` is not being called during re-ingestion
2. The `MetadataFactory` is somehow creating a single literal triple instead of multiple URI triples
3. There's a mismatch between how data is written vs. how it's read

## Solution Applied

### File: `app/org/hascoapi/entity/pojo/Task.java`

Modified the `Task.find()` method to split concatenated URIs when reading from the triplestore:

```java
} else if (predicate.equals(VSTOI.HAS_REQUIRED_INSTRUMENT)) {
    System.out.println("[Task.find] Found hasRequiredInstrument: [" + object + "]");
    // Split if the object contains multiple URIs separated by | or ;
    if (object != null && (object.contains("|") || object.contains(";"))) {
        System.out.println("[Task.find] WARN: hasRequiredInstrument contains separators - splitting");
        String[] uriParts;
        if (object.contains("|")) {
            uriParts = object.split("\\s*\\|\\s*");
        } else {
            uriParts = object.split("\\s*;\\s*");
        }
        for (String uriPart : uriParts) {
            if (uriPart != null && !uriPart.trim().isEmpty()) {
                System.out.println("[Task.find]   Adding URI: [" + uriPart.trim() + "]");
                task.addHasRequiredInstrumentUri(uriPart.trim());
            }
        }
    } else {
        task.addHasRequiredInstrumentUri(object);
    }
}
```

The same logic was applied to `VSTOI.HAS_SUBTASK`.

### Benefits

1. **Defensive**: Handles both correct (multiple triples) and incorrect (single concatenated string) data
2. **Backward Compatible**: Works with existing data in the triplestore
3. **Debug Logging**: Identifies when the problem occurs

### Limitations

This is a **workaround** that treats the symptom, not the disease. The proper fix would ensure that:
- Data is always stored as multiple triples (not a concatenated string)
- The ingestion, storage, and retrieval paths are consistent

## Testing

After applying this fix:

1. Restart the backend (sbt clean run)
2. Ingest a WKF with multiple RequiredInstruments
3. Open the workflow in the frontend
4. Check logs for `[Task.find] WARN: hasRequiredInstrument contains separators`
   - If you see this warning, the root cause is still present (data stored incorrectly)
   - If you don't see this warning, the root cause was fixed

## Next Steps

1. ✅ Apply workaround (completed)
2. ⏳ Investigate why data is stored as concatenated string
3. ⏳ Fix the root cause in ingestion/storage layer
4. ⏳ Add integration test to verify correct roundtrip behavior

## Related Files

- `app/org/hascoapi/entity/pojo/Task.java` - Task entity with defensive splitting
- `app/org/hascoapi/ingestion/WKFGenerator.java` - Splits multi-value properties during ingestion
- `app/org/hascoapi/transform/mt/wkf/WKFTasks.java` - Joins URIs with pipes during export
- `app/org/hascoapi/utils/MetadataFactory.java` - Creates RDF triples from row data

