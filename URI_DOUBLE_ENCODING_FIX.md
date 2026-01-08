# URI Double-Encoding Fix

## Problem

When attempting to regenerate DSG files from the triplestore, the application was throwing the following error:

```
Bad IRI: '%3C%3Cahead:STD-LTE-PIAGET-WEATHER-STATION%3E%3E': <%3C%3Cahead:STD-LTE-PIAGET-WEATHER-STATION%3E%3E> Code: 0/ILLEGAL_CHARACTER in SCHEME
```

This error indicated that URIs were being double-wrapped with angle brackets (`<<URI>>`), which then got URL-encoded as `%3C%3C...%3E%3E`, creating invalid SPARQL queries.

## Root Cause

The issue occurred in entity `find()` methods (e.g., `Study.find()`, `StudyObject.find()`, etc.) where SPARQL DESCRIBE queries are constructed:

```java
String queryString = "DESCRIBE <" + uri + ">";
```

If the `uri` parameter already contained angle brackets (e.g., `<ahead:STD-LTE-PIAGET-WEATHER-STATION>`), this would create:
```sparql
DESCRIBE <<ahead:STD-LTE-PIAGET-WEATHER-STATION>>
```

This is invalid SPARQL syntax and causes the query parser to fail.

## Solution

### 1. Added URI Utility Method

Added a new helper method to `URIUtils.java`:

```java
/**
 * Strip leading and trailing angle brackets from a URI string.
 * This prevents double-encoding when constructing SPARQL queries.
 * 
 * @param uri The URI string that may contain angle brackets
 * @return The URI without leading/trailing angle brackets
 */
public static String stripAngleBrackets(String uri) {
    if (uri == null || uri.isEmpty()) {
        return uri;
    }
    return uri.replaceAll("^<+|>+$", "");
}
```

This method uses a regex pattern `^<+|>+$` to strip any number of leading or trailing angle brackets.

### 2. Updated Entity Find Methods

Updated the `find()` methods in the following entity classes to use `stripAngleBrackets()` before constructing DESCRIBE queries:

**Core entities:**
- `Study.java`
- `StudyObject.java`
- `StudyObjectCollection.java`

**MT-related entities:**
- `DSG.java`
- `INS.java`
- `STR.java`
- `SDD.java`
- `DA.java`
- `DD.java`
- `DP2.java`
- `KGR.java`
- `StudyRole.java`

**Example fix in Study.java:**

```java
public static Study find(String uri) {
    if (uri == null || uri.isEmpty()) {
        System.out.println("[ERROR] No valid URI provided to retrieve Study object: " + uri);
        return null;
    }

    Study study = null;
    Statement statement;
    RDFNode object;
    
    // Strip any existing angle brackets to prevent double-encoding
    String cleanUri = URIUtils.stripAngleBrackets(uri);
    String queryString = "DESCRIBE <" + cleanUri + ">";
    Model model = SPARQLUtils.describe(CollectionUtil.getCollectionPath(
            CollectionUtil.Collection.SPARQL_QUERY), queryString);
    
    // ... rest of method
}
```

## Testing

The fix was validated with the `HascoRoundtripTest` which performs DSG ingestion. The test now passes successfully:

```
[info] Test completed - DSG ingestion workflow executed. Final status: PROCESSED
[info] Passed: Total 3, Failed 0, Errors 0, Passed 3, Canceled 20
[success] Total time: 9 s
```

## Impact

This fix ensures that:
1. DSG generation from the triplestore now works correctly
2. All entity `find()` methods are resilient to URIs that may already have angle brackets
3. SPARQL queries are always well-formed regardless of how URIs are stored or passed around
4. The system is more robust to different URI formats (full URIs, prefixed URIs, with/without brackets)

## Files Modified

1. `app/org/hascoapi/utils/URIUtils.java` - Added `stripAngleBrackets()` method
2. `app/org/hascoapi/entity/pojo/Study.java` - Updated `find()` method
3. `app/org/hascoapi/entity/pojo/StudyObject.java` - Updated `find()` method
4. `app/org/hascoapi/entity/pojo/StudyObjectCollection.java` - Updated `find()` method
5. Multiple other entity POJOs (DSG, INS, STR, SDD, DA, DD, DP2, KGR, StudyRole) - Updated `find()` methods

## Date

2026-01-08

