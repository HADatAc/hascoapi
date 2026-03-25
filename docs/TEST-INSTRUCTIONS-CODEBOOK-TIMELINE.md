# FINAL TEST INSTRUCTIONS - Codebook & Timeline

## ✅ Compilation Status
**COMPLETED**: `sbt clean compile` executed successfully at 14:36:11

---

## Next Steps

### 1. Restart the Server
```powershell
# Stop current server (Ctrl+C if running in terminal)
# Then start:
cd C:\Users\kaell\Desktop\Project\hascoapi
sbt run
```

### 2. Clear Old Data from Triple Store

**Option A: Via UI**
- Navigate to Data Files management
- Delete the old SDD DataFile with URI ending in `...DFL1773938255113971`

**Option B: Via API** (if available)
```bash
curl -X DELETE "http://localhost:9000/api/datafile/delete?uri=https://hadatac.org/ont/hadatac#/DFL1773938255113971"
```

### 3. Re-Upload SDD-health.xlsx

Upload via the UI and watch for these log entries:

#### Expected Ingestion Logs
```
✅ [SDDAttributeGenerator] -> Stored label->URI mapping: 'Gender' -> 'https://hadatac.org/ont/hadatac#/SDDATT.../6'
✅ [AnnotateSDD] Passing labelToUriMap to PVGenerator with 6 entries
✅ [PVGenerator] Linked PossibleValue for column 'Gender' to SDDAttribute (via mapAttrObj): ...SDDATT.../6

✅ [TimelineGenerator] Created Timeline object: ??observation_period with TimeRole
✅ [TimelineGenerator]   hasStart: 2026-03-01T00:00:00Z
✅ [TimelineGenerator]   hasEnd: 2026-03-31T23:59:59Z
✅ [TimelineGenerator]   hasUnit: http://qudt.org/vocab/unit/DAY
✅ 12 triple(s) have been committed to triple store
```

### 4. Generate SDD

Request generation and watch for these log entries:

#### Expected Generation Logs
```
✅ [SDDGen] Found 2 possible values
✅ [SDDCodebook] PossibleValue row added successfully at row 1
✅ [SDDCodebook] PossibleValue row added successfully at row 2

✅ [SDDGen] Found 1 timeline objects (with hasco:TimeRole)
✅ [SDDGen]   Label: ??observation_period
✅ [SDDGen]   Comment: Patient Observation Period
✅ [SDDGen]   HasStart: 2026-03-01T00:00:00Z
✅ [SDDGen]   HasEnd: 2026-03-31T23:59:59Z
✅ [SDDGen]   HasUnit: http://qudt.org/vocab/unit/DAY
✅ [SDDTimeline] Timeline object added at row 1
```

### 5. Verify Generated File

Download the generated SDD file and verify:

#### Codebook Sheet
```
Column | Code | Label  | Class
Gender | 1    | Male   | ncit:C46109
Gender | 2    | Female | ncit:C46110
```
✅ 2 data rows with all 4 columns

#### Timeline Sheet
```
Name                  | Label                           | Type          | Start                  | End                    | Unit
??observation_period  | Patient Observation Period      | time:Interval | 2026-03-01T00:00:00Z  | 2026-03-31T23:59:59Z  | unit:DAY
```
✅ 1 data row with all 6 columns

---

## Troubleshooting

### If Codebook is still empty:
- Check log: `[AnnotateSDD] Passing labelToUriMap to PVGenerator with X entries`
- If X = 0, the SDDAttributeGenerator wasn't captured properly
- Verify you restarted the server after recompiling

### If Timeline is still empty:
- Check log: `[SDDGen] Found X timeline objects (with hasco:TimeRole)`
- If X = 0, the TimelineGenerator didn't run or didn't create the object with TimeRole
- Verify the Timeline sheet exists in the uploaded file

### If Timeline Name/Label/Type are correct but Start/End/Unit are empty:
- Check generation logs for: `[SDDGen]   HasStart: ...` 
- If null/empty, the properties weren't saved during ingestion
- If populated in log but not in file, issue is in SDDTimeline.add()
- Verify SDDObject.find() loaded the properties (check if HASCO constants exist)

---

## Files Changed Summary

| File | Change | Purpose |
|------|--------|---------|
| TimelineGenerator.java | NEW | Ingest Timeline sheet data |
| AnnotateSDD.java | Modified | Add generators to chain, pass labelToUriMap |
| SDDAttributeGenerator.java | Modified | Build labelToUriMap for PVGenerator |
| SDDObject.java | Modified | Add hasStart/End/Unit fields + loading |
| HASCO.java | Modified | Add HAS_START, HAS_END constants |
| SDDTimeline.java | Modified | Populate all 6 Timeline columns |
| SDDGen.java | Modified | Add debug logging |

---

## Success Criteria

- ✅ Codebook sheet: 2 rows (Gender: Male, Female)
- ✅ Timeline sheet: 1 row with 6 columns all filled
- ✅ Roundtrip test: Upload → Ingest → Generate → Re-upload should work identically

---

## Date
2026-03-20 14:36

## Next Action
**RESTART THE SERVER** and perform test upload!

