# SDD Generation Template Initialization Fix

## Problem Summary

When generating SDD files from the triplestore data, the Dictionary Mapping sheet was empty (only containing SDDObjects like `??weather`, `??observation`, `??instant`, but missing actual data attributes like `Timestamp`, `ESP32_Chip_ID`, `Temperature`, etc.).

### Root Cause

The `SDDAttributeGenerator` class had a **field shadowing bug** that prevented the `templates` field from being accessible during `initMapping()`:

1. `SDDAttributeGenerator` declared its own `Templates templates;` field (line 31), which **shadowed** the `templates` field inherited from `BaseGenerator`
2. When `super(dataFile, null, templateFile)` was called in the constructor, `BaseGenerator` initialized **its own** `templates` field
3. `BaseGenerator` then called `initMapping()`, which is overridden by `SDDAttributeGenerator`
4. The overridden `initMapping()` tried to access `templates`, but found the **child class's** field which was still null (shadowing!)
5. As a result, `mapCol.get("AttributeType")` returned `null` instead of `"Attribute"`
6. This caused all attribute records to be skipped with the message: `attribute (from column 'null')=''`
7. No SDDAttributes were created in the triplestore
8. When generating SDD files later, only SDDObjects could be found, resulting in an incomplete Dictionary Mapping sheet

**Java Inheritance Note**: In Java, when a child class declares a field with the same name as a parent class field, it creates a **separate field** that shadows the parent's field. The two fields are completely independent - setting one doesn't affect the other.

## Fix Applied

### File: `SDDAttributeGenerator.java`

**Change 1: Removed shadowing field declaration**
- **Line 31**: Removed `Templates templates;` field declaration that was shadowing `BaseGenerator.templates`
- This allows `initMapping()` to access the parent class's `templates` field that was properly initialized by `BaseGenerator`

**Change 2: Removed redundant templates initialization**
- **Line 50**: Removed `this.templates = new Templates(templateFile);` from constructor
- This was redundant since `BaseGenerator` already creates the templates object from the same `templateFile` parameter
- Also would have been problematic since it tried to set a field that no longer exists

**Change 3: Fixed initMapping() error message**
- Changed message from `[WARNING]` to `[ERROR]` for clarity when templates is unexpectedly null

```java
// BEFORE (buggy code):
public class SDDAttributeGenerator extends BaseGenerator {
    // ... other fields ...
    Templates templates;  // ❌ SHADOWS BaseGenerator.templates!
    
    public SDDAttributeGenerator(..., String templateFile) {
        super(dataFile, null, templateFile);  // BaseGenerator sets its own templates
        // ... other initialization ...
        this.templates = new Templates(templateFile);  // ❌ Sets child's templates (too late!)
    }
    
    @Override
    public void initMapping() {
        if (templates == null) {  // ❌ Accesses child's templates (still null!)
            System.out.println("[WARNING] Templates is null...");
            return;
        }
        // ...rest never executes because templates is null
    }
}

// AFTER (fixed code):
public class SDDAttributeGenerator extends BaseGenerator {
    // ... other fields ...
    // Templates templates;  // ✅ REMOVED - use inherited field from BaseGenerator
    
    public SDDAttributeGenerator(..., String templateFile) {
        super(dataFile, null, templateFile);  // BaseGenerator sets templates
        // ... other initialization ...
        // this.templates = new Templates(templateFile);  // ✅ REMOVED - redundant
    }
    
    @Override
    public void initMapping() {
        if (templates == null) {  // ✅ Now accesses parent's templates (properly initialized!)
            System.out.println("[ERROR] Templates is null...");
            return;
        }
        // ...rest of mapping (now works correctly!)
    }
}
```

## Impact

### Before Fix:
- SDDAttributes were NOT created during ingestion
- Generated SDD files had empty Dictionary Mapping (except SDDObjects)
- Data was incomplete and couldn't be round-tripped

### After Fix:
- SDDAttributes ARE created correctly during ingestion
- Generated SDD files will have complete Dictionary Mapping with both:
  - SDDAttributes (data columns like Timestamp, Temperature, etc.)
  - SDDObjects (entities like ??weather, ??observation, ??instant)
- Data can be properly round-tripped (ingest → generate → ingest)

## Testing Required

To verify this fix:

1. **Delete existing SDD data** from triplestore (or uningest the problematic SDD)
2. **Re-ingest** the original SDD file
3. **Check logs** for SDDAttribute creation messages (should no longer skip attributes)
4. **Generate** a new SDD file
5. **Compare** the generated Dictionary Mapping sheet with the original

Expected result: The generated file should match the original file structure with all attributes present.

## Related Files

- `app/org/hascoapi/ingestion/SDDAttributeGenerator.java` (FIXED)
- `app/org/hascoapi/ingestion/BaseGenerator.java` (reference)
- `app/org/hascoapi/utils/Templates.java` (reference)
- `conf/template.conf` (template mappings)

## Date

2026-03-19

