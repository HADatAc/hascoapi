# Quick Test: Codebook and Timeline Sheets

## Codebook Test ✅ WORKING

### Upload Log Shows:
```
[SDDAttributeGenerator] -> Stored label->URI mapping: 'Gender' -> 'https://hadatac.org/ont/hadatac#/SDDATT.../6'
[AnnotateSDD] Passing labelToUriMap to PVGenerator with 6 entries
[PVGenerator] Linked PossibleValue for column 'Gender' to SDDAttribute (via mapAttrObj): https://hadatac.org/ont/hadatac#/SDDATT.../6
```

### Generation Log Shows:
```
[SDDGen] DEBUG:   PossibleValue #1: attr=https://hadatac.org/ont/hadatac#/SDDATT.../6, schema=https://hadatac.org/ont/hadatac#/SDDICT...
[SDDGen] Found 2 possible values
[SDDCodebook] PossibleValue row added successfully at row 1
[SDDCodebook] PossibleValue row added successfully at row 2
```

### Result: ✅ CODEBOOK POPULATED
```
Column | Code | Label  | Class
Gender | 1    | Male   | ncit:C46109
Gender | 2    | Female | ncit:C46110
```

---

## Timeline Test - NEEDS RETEST

### Upload Log Shows:
```
[TimelineGenerator] Created Timeline object: ??observation_period with TimeRole
12 triple(s) have been committed to triple store  ← Timeline object created!
```

### Generation Log Shows:
```
[SDDGen] Found 1 timeline objects (with hasco:TimeRole)
[SDDTimeline] Adding timeline object: https://hadatac.org/ont/hadatac#/SDDOBJ.../timeline/1
[SDDTimeline] Timeline object added at row 1
```

### Current Result (Before Final Fix):
```
Name                  | Label                 | Type          | Start | End | Unit
??observation_period  | ??observation_period  | time:Interval |       |     |
```
❌ Missing: Label description, Start, End, Unit

### Expected Result (After Final Fix):
```
Name                  | Label                           | Type          | Start                  | End                    | Unit
??observation_period  | Patient Observation Period      | time:Interval | 2026-03-01T00:00:00Z  | 2026-03-31T23:59:59Z  | unit:DAY
```
✅ All fields populated

---

## Actions Needed

### To Test Timeline Fix:
1. **Recompile** the application (SDDObject.java and SDDTimeline.java were modified)
2. **Restart** the server
3. **Delete the old SDD** from the triple store (to clear old Timeline object without Start/End/Unit)
4. **Re-upload** the SDD-health.xlsx file
5. **Generate** the SDD again
6. **Verify** Timeline sheet has all 6 columns populated

### Commands (if needed):
```bash
# Delete old SDD
curl -X DELETE "http://localhost:9000/metadata/datafile/https://hadatac.org/ont/hadatac#/DFL1773938255113971"

# Re-upload via UI or API
# Then generate
```

---

## Implementation Complete

### Files Modified:
1. ✅ `TimelineGenerator.java` - Created
2. ✅ `AnnotateSDD.java` - Added TimelineGenerator to chain
3. ✅ `SDDAttributeGenerator.java` - Added labelToUriMap
4. ✅ `SDDObject.java` - Added hasStart, hasEnd, hasUnit fields
5. ✅ `SDDTimeline.java` - Updated to populate all columns

### Both Sheets Should Work:
- ✅ **Codebook**: CONFIRMED WORKING in logs
- 🔄 **Timeline**: Implementation complete, needs retest after recompile

## Date
2026-03-20

