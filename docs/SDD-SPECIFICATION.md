# SDD SPECIFICATION — CONSOLIDATED UNDERSTANDING

## 1. What is an SDD (Semantic Data Dictionary)

The **SDD (Semantic Data Dictionary)** is a crucial metadata template, typically encoded in an Excel spreadsheet, whose primary objective is to provide semantic annotation for the variables within a dataset.

The SDD is the mechanism by which raw data columns are transformed into semantically meaningful, machine-readable concepts. It ensures that the meaning of every data point is explicitly defined and linked to controlled vocabularies (ontologies).

### Key Elements Defined by the SDD

- **Variables**: The columns or fields in the raw data file (e.g., `Timestamp`, `Temperature`)
- **Semantic Annotation**: The ontological terms (URIs) that define the meaning of each variable (e.g., `envo:09200001` for Temperature)
- **Unit of Measure**: The unit associated with the variable's value (e.g., `unit:DEG_C`)
- **Contextual Entities**: Defines the entities that contextualize the data, such as `??instant` (time) and `??weather` (phenomenon) associated with the measurement

---

## 2. Global Architectural Principles

### 2.1. Semantic Annotation of Variables

The core function of the SDD is to link a variable name (e.g., `Temperature`) to a precise ontological concept (e.g., `envo:09200001` for "Air Temperature"). This is achieved by mapping the variable to an **Entity** and an **Attribute** defined by URIs.

### 2.2. Fully Parameterized Variable Names (fPVN)

The SDD is designed to support the creation of **Fully Parameterized Variable Names (fPVN)**. An fPVN is a complete semantic description of a variable, combining the variable itself with its context (Study Object, Unit, etc.).

The structure of the **Dictionary Mapping** sheet, particularly the use of contextual columns like `attributeOf`, `Time`, `Entity`, and `Relation`, is essential for building the fPVN and ensuring that the meaning of the variable is unambiguous and fully traceable within the Knowledge Graph.

---

## 3. SDD Workbook Structure

The SDD template is composed of several worksheets focused on variable definition and annotation.

| Order | Worksheet | Primary Purpose |
|-------|-----------|----------------|
| 1 | **InfoSheet** | Control metadata and template dependencies |
| 2 | **Namespaces** | Definition of external ontology prefixes |
| 3 | **Dictionary Mapping** | The core mapping of data columns to semantic variables and contextual entities |
| 4 | **Codebook** | Defines allowed values for categorical variables (e.g., male/female) |
| 5 | **Timeline** | Defines the temporal context of the data collection |

---

## 4. Worksheet Details

### 4.1. InfoSheet

The InfoSheet contains metadata about the SDD itself:

| Attribute | Value | Purpose |
|-----------|-------|---------|
| `SDD_ID` | e.g., `SDD_WEATHER_STATION` | Unique identifier for this SDD |
| `hasDependencies` | `#Namespaces` | Points to the Namespaces sheet |
| `Data_Dictionary` | `#Dictionary Mapping` | Points to the Dictionary Mapping sheet |
| `Codebook` | `#Codebook` | Points to the Codebook sheet |
| `Timeline` | `#Timeline` | Points to the Timeline sheet |
| `Version` | e.g., `1` | Version number of the SDD |

### 4.2. Namespaces

Defines the ontology prefixes used throughout the template. Example:

| hasPrefix | hasNameSpace | hasFormat | hasSource |
|-----------|--------------|-----------|-----------|
| `hasco` | `http://hadatac.org/ont/hasco#` | | |
| `envo` | `http://purl.obolibrary.org/obo/ENVO_` | `application/rdf+xml` | `http://purl.obolibrary.org/obo/envo.owl` |
| `pato` | `http://purl.obolibrary.org/obo/PATO_` | `application/rdf+xml` | `http://purl.obolibrary.org/obo/pato.owl` |
| `unit` | `http://qudt.org/vocab/unit/` | `text/turtle` | `http://qudt.org/vocab/unit/` |
| `time` | `http://www.w3.org/2006/time#` | | |

### 4.3. Dictionary Mapping (The Core)

This is the **central sheet**, mapping the raw data columns to semantic concepts and defining contextual entities.

#### Column Structure:

| Column | Example Value | Purpose |
|--------|--------------|---------|
| `Column` | `Temperature` | The name of the column in the raw data file |
| `Attribute` | `envo:09200001` | The ontological property (e.g., Air Temperature) |
| `attributeOf` | `??weather` | The entity this attribute describes (a placeholder for the weather phenomenon) |
| `Unit` | `unit:DEG_C` | The unit of measure |
| `Time` | `??instant` | The time entity associated with the measurement |
| `Entity` | `envo:01001079` | The ontological class of the entity being measured (e.g., weather) |
| `Role` | (optional) | Role classification |
| `Relation` | `time:inside` | How entities relate to each other |
| `inRelationTo` | `??observation` | What the relation points to |

#### Example Mapping:

```
Column        | Attribute        | attributeOf | Unit           | Time      | Entity          
--------------|------------------|-------------|----------------|-----------|------------------
Timestamp     | time:inXSDDateTime | ??instant   |                |           |
Temperature   | envo:09200001    | ??weather   | unit:DEG_C     | ??instant |
```

#### Contextual Entities (Placeholders):

The sheet also defines **abstract entities** using `??` prefixes, which are then mapped to ontological classes:

| Placeholder | Ontological Class | Purpose |
|-------------|------------------|---------|
| `??weather` | `envo:01001079` | Defines the weather phenomenon as an entity |
| `??observation` | `time:Interval` | Defines the time interval over which the observation occurred |
| `??instant` | `time:Instant` | Defines a specific point in time, related to the observation interval |

These rows typically have empty `Column` and `Attribute` cells, but populate the `Entity` column:

```
Column        | Attribute | ... | Entity
--------------|-----------|-----|------------------
??weather     |           |     | envo:01001079
??observation |           |     | time:Interval
??instant     |           |     | time:Instant
```

---

### 4.4. Codebook

The Codebook defines **allowed values for categorical variables**. For example, mapping codes to human-readable labels and ontological classes.

#### Column Structure:

| Column | Code | Label | Class |
|--------|------|-------|-------|
| Variable name | Raw value in data | Human-readable meaning | Ontological class URI |

#### Example:

```
Column  | Code | Label  | Class
--------|------|--------|------------------
Gender  | 1    | Male   | ncit:C20197
Gender  | 2    | Female | ncit:C16576
```

**Important Implementation Detail:**

During ingestion, the `PVGenerator` creates `PossibleValue` entities. These must be linked to the corresponding `SDDAttribute` URI via the `hasco:isPossibleValueOf` property. The system queries for the SDDAttribute matching the column name and uses its URI (e.g., `https://hadatac.org/ont/hadatac#/SDDATT{timestamp}/{index}`).

---

### 4.5. Timeline

The Timeline defines the **temporal context** of the data collection, often linking to time-related ontologies.

#### Column Structure:

| Name | Label | Type | Start | End | Unit |
|------|-------|------|-------|-----|------|
| Entity identifier | Human label | Ontological type | Start time | End time | Time unit |

#### Example:

```
Name          | Label       | Type         | Start | End | Unit
--------------|-------------|--------------|-------|-----|------
??instant     | Instant     | time:Instant |       |     |
??observation | Observation | time:Interval|       |     |
```

**Important Implementation Detail:**

Timeline entries are typically virtual `SDDObject` entities (labels starting with `??`) that have:
1. Time-related ontological entities (e.g., `time:Instant`, `time:Interval`)
2. OR are referenced in the `Time` column of SDDAttributes

The generation system identifies these by querying for SDDObjects with:
- `hasco:partOfSchema` pointing to the SDDICT URI
- Entity URIs starting with `http://www.w3.org/2006/time#` OR
- Labels starting with `??` (virtual objects)

---

## 5. SDD Role in the Metadata Ecosystem

The SDD provides the semantic meaning for the actual data values, completing the metadata chain:

- **DSG** → Defines the Study and the Study Objects (the context)
- **DP2** → Defines the Deployment (the provenance)
- **SDD** → Defines the Semantic Variables (the meaning of the data) and links them to the context defined by the DSG and DP2

---

## 6. Technical Implementation Details

### 6.1. URI Patterns

When an SDD is ingested:

- **SDD URI**: `https://hadatac.org/ont/hadatac#/SDD{timestamp}`
- **DataFile URI**: `https://hadatac.org/ont/hadatac#/DFL{timestamp}`
- **Schema URI** (SDDICT): `https://hadatac.org/ont/hadatac#/SDDICT{timestamp}`
- **SDDAttribute URIs**: `https://hadatac.org/ont/hadatac#/SDDATT{timestamp}/{index}`
- **SDDObject URIs**: `https://hadatac.org/ont/hadatac#/SDDOBJ{timestamp}/{index}`
- **PossibleValue URIs**: `https://hadatac.org/ont/hadatac#/PSV{timestamp}/{index}`

### 6.2. Triple Store Structure

All entities are stored with:
- `hasco:partOfSchema` pointing to the SDDICT URI
- Type declarations (`rdf:type`, `hasco:hascoType`)
- Labels (`rdfs:label`)
- Position in sheet (`hasco:listPosition`)

### 6.3. Generation Process

When generating an SDD from the triple store:

1. System queries for SDDs with matching status (e.g., Draft)
2. Counts SDDAttributes + SDDObjects to verify the SDD is fully populated
3. Selects an SDD with entities
4. Queries for all SDDAttributes and SDDObjects
5. Queries for PossibleValues linked to those attributes
6. Queries for Timeline objects (virtual objects with time entities)
7. Builds the Excel workbook with all sheets populated

---

## 7. Common Issues and Solutions

### Issue 1: Codebook Sheet Empty After Generation

**Problem**: PossibleValues not linked correctly to SDDAttributes.

**Solution**: The `PVGenerator` now queries the triple store to find the SDDAttribute URI matching the column name, and links the PossibleValue via `hasco:isPossibleValueOf` using that URI (not just the column name string).

### Issue 2: Timeline Sheet Empty After Generation

**Problem**: Original query required `hasco:hasRole hasco:TimeRole`, which users rarely populate.

**Solution**: Updated query to find virtual objects (`??` prefix) with time-related entities, which is more aligned with actual usage patterns.

### Issue 3: Namespace Expansion in Generated Files

**Problem**: Generated files show full URIs instead of prefixes.

**Expected Behavior**: This is actually correct for the Dictionary Mapping Attribute/Entity columns, as they should show namespace-expanded URIs for attributes but prefixed URIs for contextual entities like `envo:01001079`.

---

## 8. Best Practices

1. **Always define virtual objects** (starting with `??`) for contextual entities
2. **Use standard ontology prefixes** in the Namespaces sheet
3. **Populate the Codebook** for any categorical variables to enable proper value validation
4. **Define Timeline objects** for any time-related virtual entities
5. **Use consistent naming** for column names (avoid special characters except `_` and `-`)

---

## Appendix: Full Example

See the test files:
- `test/resources/SDD-health.xlsx`
- `test/resources/SDD-WS.xlsx`

These demonstrate complete, working SDD templates.

