# Automatic Namespace Detection and Registration

## Feature
Automatically detects unknown namespaces from full URIs, creates prefixes, and adds them to the Namespaces sheet during SDD generation.

## Problem Solved
When ontology URIs appear in the data (e.g., in Codebook Class column or Dictionary Mapping), they were appearing as full URIs instead of prefixed form:
- ❌ Before: `http://purl.obolibrary.org/obo/NCIT_C46110`
- ✅ After: `ncit:C46110`

And the namespace is automatically added to the Namespaces sheet.

---

## Implementation

### Core Logic: SDDGenHelper.registerAndConvertFullUri()

Located in: `app/org/hascoapi/transform/mt/sdd/SDDGenHelper.java`

#### Algorithm

```java
public String registerAndConvertFullUri(String fullUri) {
    // 1. Check if already prefixed (e.g., "ncit:C46110")
    if (contains ":" && !startsWith "http") {
        registerPrefixFromUri(fullUri);
        return fullUri;
    }
    
    // 2. Check if namespace already registered globally
    String prefixed = URIUtils.replaceNameSpaceEx(fullUri);
    if (prefixed != fullUri) {
        registerPrefixFromUri(prefixed);
        return prefixed;  // e.g., "time:Interval"
    }
    
    // 3. Auto-detect namespace pattern and create prefix
    
    // Pattern A: Hash separator (e.g., http://www.w3.org/2006/time#Interval)
    if (contains "#") {
        namespaceUri = before "#" + "#"  // http://www.w3.org/2006/time#
        localName = after "#"             // Interval
        prefix = last meaningful path part  // time
        → Result: "time:Interval"
    }
    
    // Pattern B: Underscore separator (e.g., http://purl.obolibrary.org/obo/NCIT_C46110)
    else if (contains "_") {
        namespaceUri = before last "_" + "_"  // http://purl.obolibrary.org/obo/NCIT_
        localName = after last "_"            // C46110
        prefix = part between "/" and "_"    // NCIT → ncit (lowercase)
        → Result: "ncit:C46110"
    }
    
    // Pattern C: Slash separator fallback
    else if (contains "/") {
        namespaceUri = before last "/" + "/"
        localName = after last "/"
        prefix = last meaningful path part
    }
    
    // 4. Register and return
    if (namespace detected successfully) {
        create NameSpace(prefix, namespaceUri);
        add to helper.namespaces collection;
        return prefix + ":" + localName;
    }
    
    // 5. Fallback: return as-is if can't parse
    return fullUri;
}
```

### Usage in Generation

All URI fields in sheets now use this method:

#### SDDCodebook.add()
```java
String classUri = pv.getHasClass();  // http://purl.obolibrary.org/obo/NCIT_C46110
String classShort = helper.registerAndConvertFullUri(classUri);  // ncit:C46110
row.createCell(col).setCellValue(classShort);
```

#### SDDTimeline.add()
```java
String type = obj.getEntity();  // http://www.w3.org/2006/time#Interval
String typeShort = helper.registerAndConvertFullUri(type);  // time:Interval
row.createCell(col).setCellValue(typeShort);

String unit = obj.getHasUnit();  // http://qudt.org/vocab/unit/DAY
String unitShort = helper.registerAndConvertFullUri(unit);  // unit:DAY
row.createCell(col).setCellValue(unitShort);
```

#### SDDDictionaryMapping.add() and addObject()
```java
String attribute = attr.getAttribute();  // http://purl.obolibrary.org/obo/NCIT_C25299
String attrShort = helper.registerAndConvertFullUri(attribute);  // ncit:C25299
row.createCell(col).setCellValue(attrShort);

// Same for: Unit, Time, Relation, InRelationTo, WasDerivedFrom, Entity, Role
```

---

## Examples

### Example 1: NCIT Ontology
**Input**: `http://purl.obolibrary.org/obo/NCIT_C46110`

**Detection**:
- Contains `_`: ✅
- Namespace URI: `http://purl.obolibrary.org/obo/NCIT_`
- Local name: `C46110`
- Prefix part: `NCIT` → `ncit` (lowercase)

**Output**: `ncit:C46110`

**Namespaces sheet gets**:
```
hasPrefix | hasNameSpace
ncit      | http://purl.obolibrary.org/obo/NCIT_
```

### Example 2: Time Ontology
**Input**: `http://www.w3.org/2006/time#Interval`

**Detection**:
- Contains `#`: ✅
- Namespace URI: `http://www.w3.org/2006/time#`
- Local name: `Interval`
- Prefix: `time` (extracted from path)

**Output**: `time:Interval`

**Namespaces sheet gets**:
```
hasPrefix | hasNameSpace
time      | http://www.w3.org/2006/time#
```

### Example 3: Already Registered
**Input**: `http://hadatac.org/ont/hasco/originalID`

**Detection**:
- Global namespace check: ✅ Found as `hasco:originalID`
- Already in global registry

**Output**: `hasco:originalID`

**Namespaces sheet gets**: (from existing namespaces, not auto-created)

---

## Files Modified

1. **SDDGenHelper.java**
   - Added `registerAndConvertFullUri()` method

2. **SDDCodebook.java**
   - Updated Class column to use `registerAndConvertFullUri()`

3. **SDDTimeline.java**
   - Updated Type column to use `registerAndConvertFullUri()`
   - Updated Unit column to use `registerAndConvertFullUri()`

4. **SDDDictionaryMapping.java**
   - Updated Attribute column to use `registerAndConvertFullUri()`
   - Updated attributeOf column to use `registerAndConvertFullUri()`
   - Updated Unit column to use `registerAndConvertFullUri()`
   - Updated Time column to use `registerAndConvertFullUri()`
   - Updated Relation column to use `registerAndConvertFullUri()`
   - Updated inRelationTo column to use `registerAndConvertFullUri()`
   - Updated wasDerivedFrom column to use `registerAndConvertFullUri()`
   - Updated Entity column (in addObject) to use `registerAndConvertFullUri()`
   - Updated Role column (in addObject) to use `registerAndConvertFullUri()`

---

## Benefits

1. **Cleaner Output**: All URIs appear in prefixed form (ncit:C46110 instead of full URL)
2. **Complete Namespaces Sheet**: All required namespaces are automatically collected
3. **Roundtrip Compatible**: Generated SDD can be re-uploaded without namespace errors
4. **Ontology Discovery**: New ontologies are automatically discovered and documented

---

## Testing

### What to Look For

**In Generation Logs:**
```
[SDDGenHelper] Auto-registered namespace: ncit → http://purl.obolibrary.org/obo/NCIT_
[SDDGenHelper] Converted URI: http://purl.obolibrary.org/obo/NCIT_C46110 → ncit:C46110
```

**In Generated SDD:**

**Codebook Sheet:**
```
Column | Code | Label  | Class
Gender | 1    | Male   | ncit:C46109  ← Prefixed!
Gender | 2    | Female | ncit:C46110  ← Prefixed!
```

**Namespaces Sheet:**
```
hasPrefix | hasNameSpace
ncit      | http://purl.obolibrary.org/obo/NCIT_  ← Auto-added!
time      | http://www.w3.org/2006/time#           ← Auto-added if needed!
unit      | http://qudt.org/vocab/unit/            ← Auto-added if needed!
```

---

## Edge Cases Handled

1. **Already prefixed input**: Returns as-is
2. **Already registered globally**: Uses global namespace
3. **URI with # separator**: Extracts namespace correctly
4. **URI with _ separator**: Handles OBO pattern (NCIT_, ENVO_, etc.)
5. **URI with / separator only**: Fallback extraction
6. **Unparseable URI**: Returns original (warning logged)

---

## Compilation Status
✅ **SUCCESSFUL**: Compiled at 16:04:37

## Date
2026-03-20

