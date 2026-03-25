# SDD SPECIFICATION — CONSOLIDATED UNDERSTANDING

## 1. What is an SDD (Semantic Data Dictionary)

The **SDD (Semantic Data Dictionary)** is a critical metadata template within the HADatAc ecosystem that serves as the **semantic bridge** between raw data columns and ontologically-grounded, machine-readable concepts.

**Core Purpose:**
- **Transform** raw CSV column names into semantically precise variables linked to controlled vocabularies
- **Define** the complete semantic context of each measurement through ontological terms (URIs)
- **Enable** automated reasoning and data integration across heterogeneous datasets
- **Establish** the foundation for Knowledge Graph population

The SDD ensures that every data point is explicitly defined and traceable to standardized ontological concepts, making the data **FAIR** (Findable, Accessible, Interoperable, Reusable).

---

## 2. Global Architectural Principles

### 2.1. Semantic Annotation of Variables
The SDD's primary function is to create a **binding** between:
- **Column Name** (e.g., `Temperature`) — the raw data label
- **Ontological Attribute** (e.g., `envo:09200001` for "Air Temperature") — the precise semantic concept
- **Contextual Entity** (e.g., `envo:01001079` for "weather phenomenon") — what is being measured
- **Unit of Measure** (e.g., `unit:DEG_C`) — quantitative precision

This binding is stored in the triple store as RDF triples and enables semantic queries and reasoning.

### 2.2. Fully Parameterized Variable Names (fPVN)
The SDD supports the construction of **Fully Parameterized Variable Names (fPVN)**, which are complete semantic descriptions combining:
- The **variable** itself (Attribute)
- The **study object** or phenomenon being measured (Entity, via `attributeOf`)
- The **unit** of measurement
- The **temporal context** (Time)
- **Relationships** to other entities (Relation, inRelationTo)

The fPVN ensures that variable semantics are **unambiguous** and **fully traceable** within the Knowledge Graph.

### 2.3. Dual Entity System: Attributes and Objects

The SDD uses two complementary entity types:

**SDDAttribute** (Actual Measurements):
- Represents **observable** or **measurable** properties
- Has a `Column` name (e.g., `Temperature`)
- Has an `Attribute` URI (e.g., `envo:09200001`)
- Points to a virtual entity via `attributeOf` (e.g., `??weather`)
- Stored in triple store with URI pattern: `SDDATT{timestamp}/{rowNumber}`

**SDDObject** (Virtual/Contextual Entities):
- Represents **abstract entities** or **contextual placeholders**
- Uses `??` prefix convention (e.g., `??weather`, `??instant`, `??observation`)
- Defines the **ontological class** via `Entity` column (e.g., `envo:01001079`)
- Establishes **relationships** between entities (e.g., `??instant` is `time:inside` `??observation`)
- Has **no** Attribute column (entity definition, not a measurement)
- Stored in triple store with URI pattern: `SDDOBJ{timestamp}/{rowNumber}`

This dual system enables the SDD to model complex semantic relationships while maintaining clear separation between observable data and contextual metadata.

---

## 3. SDD Workbook Structure

The SDD template consists of multiple worksheets, each serving a specific semantic or organizational function.

| Order | Worksheet | Primary Purpose |
|-------|-----------|-----------------|
| 1 | **InfoSheet** | Template control metadata, versioning, and dependency declarations |
| 2 | **Namespaces** | Definition of ontology prefixes used throughout the template |
| 3 | **Dictionary Mapping** | **[CORE]** Mapping of data columns to semantic concepts and contextual entities |
| 4 | **Codebook** | Value set definitions for categorical variables (controlled vocabularies) |
| 5 | **Timeline** | Temporal context and structure of data collection |

---

## 4. Worksheet Details

### 4.1. InfoSheet — Template Metadata

**Purpose:** Provides control information about the SDD template itself.

**Key Fields:**

| Attribute | Example Value | Description |
|-----------|---------------|-------------|
| `SDD_ID` | `SDD_WEATHER_STATION` | Unique identifier for this SDD template |
| `Version` | `1` | Template version number |
| `hasDependencies` | `#Namespaces` | Reference to the Namespaces sheet |
| `Data_Dictionary` | `#Dictionary Mapping` | Reference to the core mapping sheet |
| `Codebook` | `#Codebook` | Reference to the codebook (if used) |
| `Timeline` | `#Timeline` | Reference to temporal metadata (if used) |
| `Code_Mappings` | *(empty)* | Reserved for code mapping metadata |
| `Imports` | *(empty)* | Reserved for ontology import declarations |

**Storage:** Metadata is ingested and stored as part of the SDD's triple representation.

---

### 4.2. Namespaces — Ontology Prefix Definitions

**Purpose:** Declares the ontology namespaces (prefixes) used in the Dictionary Mapping sheet.

**Structure:**

| hasPrefix | hasNameSpace | hasFormat | hasSource |
|-----------|--------------|-----------|-----------|
| `hasco` | `http://hadatac.org/ont/hasco/` | *(optional)* | *(optional)* |
| `envo` | `http://purl.obolibrary.org/obo/ENVO_` | `application/rdf+xml` | `http://purl.obolibrary.org/obo/envo.owl` |
| `pato` | `http://purl.obolibrary.org/obo/PATO_` | `application/rdf+xml` | `http://purl.obolibrary.org/obo/pato.owl` |
| `unit` | `http://qudt.org/vocab/unit/` | `text/turtle` | `http://qudt.org/vocab/unit/` |
| `time` | `http://www.w3.org/2006/time#` | *(optional)* | *(optional)* |
| `sio` | `http://semanticscience.org/resource/` | *(optional)* | *(optional)* |

**Important:**
- Prefixes are automatically expanded during ingestion (e.g., `envo:09200001` → `http://purl.obolibrary.org/obo/ENVO_09200001`)
- The system validates that all prefixes used in Dictionary Mapping are declared here
- Prefixes registered here are stored in the triple store and used for URI resolution

---

### 4.3. Dictionary Mapping — The Semantic Core

**Purpose:** This is the **heart of the SDD**, mapping raw data columns to ontological concepts and defining the contextual entities.

#### 4.3.1. Column Definitions

| Column | Description | Example (Attribute Row) | Example (Object Row) |
|--------|-------------|-------------------------|----------------------|
| **Column** | The name of the column in the raw data file OR the placeholder for a virtual entity | `Temperature` | `??weather` |
| **Attribute** | The ontological property/attribute URI | `envo:09200001` | *(empty)* |
| **attributeOf** | The virtual entity this attribute describes (links to an SDDObject) | `??weather` | *(empty)* |
| **Unit** | The unit of measurement | `unit:DEG_C` | *(empty)* |
| **Time** | The temporal entity associated with this measurement | `??instant` | *(empty)* |
| **Entity** | The ontological class of the entity | `envo:01001079` (for attributes) | `envo:01001079` (for objects) |
| **Role** | The role classification (rarely used) | *(empty)* | *(empty)* |
| **Relation** | The relationship predicate linking this entity to another | *(empty)* | `time:inside` |
| **inRelationTo** | The target entity of the relationship | *(empty)* | `??observation` |
| **wasDerivedFrom** | Provenance: source of derived attributes | *(empty)* | *(empty)* |
| **wasGeneratedBy** | Provenance: process that generated this attribute | *(empty)* | *(empty)* |

#### 4.3.2. Row Types

The Dictionary Mapping sheet contains **two types of rows**:

##### **Type 1: SDDAttribute Rows (Measurement Variables)**

These rows define **actual data columns** from the CSV file.

**Example:**

| Column | Attribute | attributeOf | Unit | Time | Entity | Role | Relation | inRelationTo | wasDerivedFrom | wasGeneratedBy |
|--------|-----------|-------------|------|------|--------|------|----------|--------------|----------------|----------------|
| `Temperature` | `envo:09200001` | `??weather` | `unit:DEG_C` | `??instant` | `envo:01001079` | | | | | |

**Semantic Interpretation:**
- There is a data column named **Temperature**
- It measures the property **envo:09200001** (Air Temperature)
- This property is **an attribute of** the entity `??weather`
- The measurement unit is **unit:DEG_C** (degrees Celsius)
- The measurement is associated with the time entity **??instant**
- The entity being measured is of type **envo:01001079** (weather phenomenon)

**Stored as:**
- RDF triples in triple store with URI: `https://hadatac.org/ont/hadatac#/SDDATT{datafileId}/{rowNumber}`
- Linked to schema: `https://hadatac.org/ont/hadatac#/SDDICT{datafileId}`

##### **Type 2: SDDObject Rows (Virtual Entities)**

These rows define **abstract contextual entities** that do not correspond to actual data columns.

**Example:**

| Column | Attribute | attributeOf | Unit | Time | Entity | Role | Relation | inRelationTo | wasDerivedFrom | wasGeneratedBy |
|--------|-----------|-------------|------|------|--------|------|----------|--------------|----------------|----------------|
| `??weather` | | | | | `envo:01001079` | | | | | |
| `??observation` | | | | | `time:Interval` | | | | | |
| `??instant` | | | | | `time:Instant` | | `time:inside` | `??observation` | | |

**Semantic Interpretation:**

1. **`??weather`**
   - Defines a virtual entity (placeholder) named `??weather`
   - This entity is of type **envo:01001079** (weather phenomenon)
   - It serves as the subject for multiple attribute measurements

2. **`??observation`**
   - Defines a temporal interval entity
   - Type: **time:Interval**
   - Represents the overall observation period

3. **`??instant`**
   - Defines a specific time point
   - Type: **time:Instant**
   - Has relationship: **time:inside** pointing to `??observation`
   - Meaning: each instant is contained within the observation interval

**Stored as:**
- RDF triples in triple store with URI: `https://hadatac.org/ont/hadatac#/SDDOBJ{datafileId}/{rowNumber}`
- Linked to schema: `https://hadatac.org/ont/hadatac#/SDDICT{datafileId}`

**Key Characteristic of SDDObject Rows:**
- The **Attribute** column is **ALWAYS EMPTY**
- The **Column** value uses the `??` prefix convention
- They define **what is being measured** (the subject), not the measurement itself

#### 4.3.3. Relationship Graph Example

The Dictionary Mapping creates a semantic graph:

```
??weather (envo:01001079)
  ├─ has attribute → Temperature (envo:09200001) [unit:DEG_C]
  ├─ has attribute → humidity (pato:0015009) [unit:PERCENT]
  └─ has attribute → air_quality (envo:01000432) [unit:MicroGM-PER-M3]

??observation (time:Interval)
  └─ contains → ??instant (time:Instant)
                  └─ when → Temperature was measured
                  └─ when → humidity was measured
                  └─ when → air_quality was measured

??instant
  └─ time:inside → ??observation
```

---

### 4.4. Codebook — Categorical Value Definitions

**Purpose:** Defines the allowed values and their semantic meanings for categorical/coded variables.

**Structure:**

| Column | Code | Label | Class |
|--------|------|-------|-------|
| `gender` | `1` | `Male` | `ncit:C46109` |
| `gender` | `2` | `Female` | `ncit:C46110` |

**Use Case:**
- Ensures that categorical data values (codes) are mapped to standardized ontological terms
- Supports data validation during ingestion
- Enables semantic queries on categorical data (e.g., "find all female participants")

---

### 4.5. Timeline — Temporal Context

**Purpose:** Defines the temporal structure and scope of the data collection.

**Structure:**

| Name | Label | Type | Start | End | Unit |
|------|-------|------|-------|-----|------|
| *(temporal entity definitions)* | | | | | |

**Use Case:**
- Define study phases, observation periods, or temporal sampling strategies
- Link temporal entities to ontological concepts (e.g., W3C Time Ontology)

---

## 5. SDD URI Architecture

The SDD system uses a **hierarchical URI structure** to organize semantic entities:

### 5.1. Core URI Patterns

| Entity Type | URI Pattern | Example |
|-------------|-------------|---------|
| **DataFile** | `{namespace}/DFL{timestamp}` | `https://hadatac.org/ont/hadatac#/DFL1773851870316631` |
| **SDD** | `{namespace}/SDD{timestamp}` | `https://hadatac.org/ont/hadatac#/SDD1773851870316631` |
| **Schema (SDDICT)** | `{namespace}/SDDICT{datafileTimestamp}` | `https://hadatac.org/ont/hadatac#/SDDICT1773851870316631` |
| **SDDAttribute** | `{namespace}/SDDATT{datafileTimestamp}/{rowNumber}` | `https://hadatac.org/ont/hadatac#/SDDATT1773851870316631/1` |
| **SDDObject** | `{namespace}/SDDOBJ{datafileTimestamp}/{rowNumber}` | `https://hadatac.org/ont/hadatac#/SDDOBJ1773851870316631/7` |

### 5.2. URI Relationships

```
DataFile (DFL...)
  └─ hasSchema → SDD (SDD...)
                   └─ hasDataFile → DataFile (DFL...)
                   └─ hasSchema → SDDICT (SDDICT...)
                                    ├─ partOfSchema ← SDDAttribute (SDDATT.../1)
                                    ├─ partOfSchema ← SDDAttribute (SDDATT.../2)
                                    ├─ partOfSchema ← SDDObject (SDDOBJ.../7)
                                    ├─ partOfSchema ← SDDObject (SDDOBJ.../8)
                                    └─ partOfSchema ← SDDObject (SDDOBJ.../9)
```

**Key Insight:**
- The **DataFile timestamp** is the **primary key** for linking all SDD-related entities
- **SDDICT** (Schema URI) is derived from **DFL** (DataFile URI) by replacing the prefix
- All SDDAttributes and SDDObjects reference the **SDDICT** schema URI via `hasco:partOfSchema`

---

## 6. SDD Generation and Ingestion Workflow

### 6.1. Ingestion Process (Excel → Triple Store)

```
1. User uploads SDD Excel file
2. IngestionAPI.ingest() called with elementType="sdd"
3. InfoSheet is parsed → SDD metadata extracted
4. Namespaces sheet is parsed → Prefix mappings registered
5. Dictionary Mapping sheet is parsed:
   - SDDAttributeGenerator processes rows WITH Attribute values
   - SDDObjectGenerator processes rows WITHOUT Attribute values (virtual entities)
6. Codebook sheet is parsed (if present)
7. Timeline sheet is parsed (if present)
8. RDF triples are generated and committed to Fuseki triple store
9. DataFile status updated to PROCESSED
```

### 6.2. Generation Process (Triple Store → Excel)

```
1. User requests SDD generation via /mt/sdd/gen endpoint
2. SDDGen.genByStatus() queries triple store for:
   - SDD with matching status (e.g., DRAFT)
   - Associated DataFile URI
   - SDDICT schema URI (derived from DataFile URI)
3. SDDGen queries for all SDDAttributes and SDDObjects with partOfSchema = SDDICT URI
4. Excel workbook is created:
   - InfoSheet: SDD metadata written
   - Namespaces: Registered prefixes written
   - Dictionary Mapping:
     * SDDAttributes written as rows WITH Attribute column
     * SDDObjects written as rows WITHOUT Attribute column (only Entity)
   - Codebook: Categorical values written (if exist)
   - Timeline: Temporal data written (if exists)
5. Excel file saved to filesystem and returned to user
```

**Critical Implementation Detail:**
- The system uses **URIUtils.replacePrefixEx()** during generation to convert full URIs back to prefixed form
- Example: `http://purl.obolibrary.org/obo/ENVO_09200001` → `envo:09200001`

---

## 7. SDD Role in the Metadata Ecosystem

The SDD **completes the semantic chain** by providing the final layer of meaning for data values:

```
┌─────────────────────────────────────────────────────────────┐
│                    METADATA ECOSYSTEM                        │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  DSG (Data Study Generator)                                 │
│    ↓                                                         │
│    └─ Defines: Study, Participants, Study Objects           │
│                 (the CONTEXT)                                │
│                                                              │
│  DP2 (Deployment Platform to Participants)                  │
│    ↓                                                         │
│    └─ Defines: Deployment, Instruments, Platform            │
│                 (the PROVENANCE)                             │
│                                                              │
│  SDD (Semantic Data Dictionary)                             │
│    ↓                                                         │
│    └─ Defines: Semantic Variables, Units, Entities          │
│                 (the MEANING of the DATA)                    │
│                                                              │
│  DA (Data Acquisition)                                      │
│    ↓                                                         │
│    └─ Uses: SDD to interpret CSV columns                    │
│         Creates: Measurement triples in Knowledge Graph     │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 7.1. Data Flow Example

**Scenario:** Temperature measurement from a weather station

1. **DSG defines:**
   - Study: "Weather Monitoring Study"
   - Study Object: "Weather Station Alpha"

2. **DP2 defines:**
   - Deployment: "Station Alpha deployed at Location X"
   - Instrument: "DHT22 Temperature Sensor"

3. **SDD defines:**
   - Column: `Temperature`
   - Attribute: `envo:09200001` (Air Temperature)
   - Entity: `envo:01001079` (weather phenomenon)
   - Unit: `unit:DEG_C`

4. **DA (Data Acquisition) ingests CSV:**
   ```csv
   Timestamp,Temperature
   2026-03-19T10:00:00Z,23.5
   ```
   
   **Creates triples:**
   ```turtle
   :Measurement_1 a hasco:Measurement ;
     hasco:hasAttribute envo:09200001 ;
     hasco:hasValue "23.5"^^xsd:float ;
     hasco:hasUnit unit:DEG_C ;
     hasco:isMeasurementOf :WeatherStationAlpha ;
     prov:generatedAtTime "2026-03-19T10:00:00Z"^^xsd:dateTime .
   ```

**Result:** The raw number `23.5` is now a **semantically rich measurement** linked to:
- The specific weather station (from DSG)
- The deployment context (from DP2)
- The ontological concept of air temperature (from SDD)
- The unit of measurement (from SDD)
- The temporal context (from SDD)

---

## 8. Advanced SDD Features

### 8.1. Virtual Columns and Derived Attributes

The SDD supports **computed** or **derived** attributes through `wasDerivedFrom`:

**Example:**

| Column | Attribute | attributeOf | wasDerivedFrom |
|--------|-----------|-------------|----------------|
| `BMI` | `ncit:C16358` | `??participant` | `??weight; ??height` |

**Meaning:** BMI is derived from weight and height measurements.

### 8.2. Provenance Tracking

The `wasGeneratedBy` column links measurements to the process/method that generated them:

**Example:**

| Column | Attribute | wasGeneratedBy |
|--------|-----------|----------------|
| `quality_score` | `custom:QualityScore` | `custom:ScoringAlgorithm_v2` |

### 8.3. Multi-Entity Relationships

Complex scenarios can model multiple entities and their relationships:

**Example: Social Network Analysis**

| Column | Attribute | attributeOf | Relation | inRelationTo |
|--------|-----------|-------------|----------|--------------|
| `??person_A` | | | `foaf:knows` | `??person_B` |

---

## 9. Best Practices

### 9.1. Naming Conventions

- **Virtual entities:** Use `??` prefix (e.g., `??weather`, `??participant`)
- **Column names:** Use descriptive, human-readable names (e.g., `Temperature`, `BMI`)
- **Consistency:** Maintain consistent naming across related SDDs

### 9.2. Ontology Selection

- **Prefer established ontologies:** ENVO, PATO, NCIT, OBI, etc.
- **Document custom terms:** If creating local ontology terms, document in study materials
- **Use specific terms:** Choose the most specific ontological class available

### 9.3. Unit Standardization

- **Use QUDT units:** Prefer `unit:` namespace from QUDT ontology
- **Be explicit:** Always specify units, even for dimensionless quantities
- **Validate units:** Ensure unit URIs resolve to valid ontology terms

### 9.4. Template Reusability

- **Create template families:** Develop reusable SDDs for common measurement scenarios
- **Version control:** Track SDD versions in the `Version` field
- **Documentation:** Maintain external documentation for complex SDDs

---

## 10. Common Pitfalls and Troubleshooting

### 10.1. Missing Namespace Declarations

**Problem:** Using `envo:09200001` without declaring `envo` in Namespaces sheet

**Error:** Validation fails with namespace error

**Solution:** Always declare ALL prefixes used in Dictionary Mapping

### 10.2. Inconsistent Virtual Entity Usage

**Problem:** Using `??weather` in `attributeOf` but not defining `??weather` as an SDDObject row

**Error:** Dangling reference, incomplete semantic graph

**Solution:** Every virtual entity referenced in `attributeOf`, `Time`, or `inRelationTo` MUST have a corresponding SDDObject row

### 10.3. Invalid URIs in Unit Column

**Problem:** Using `unit:MicroGM-PER-M3%A` (invalid percent encoding)

**Error:** SPARQL query exception during generation

**Solution:** Ensure all URIs are properly encoded; avoid special characters that break URI syntax

### 10.4. Confusing SDDAttribute and SDDObject Rows

**Problem:** Adding an `Attribute` value to a virtual entity row

**Error:** Semantic inconsistency, entity treated as both object and attribute

**Solution:** 
- **SDDAttribute rows:** Have `Column` + `Attribute` filled
- **SDDObject rows:** Have `Column` + `Entity` filled, `Attribute` is EMPTY

---

## 11. Technical Reference

### 11.1. Key Java Classes

- **SDDGen.java:** Handles generation (triple store → Excel)
- **SDDAttributeGenerator.java:** Ingests SDDAttribute rows (Excel → triple store)
- **SDDObjectGenerator.java:** Ingests SDDObject rows (Excel → triple store)
- **SDDAttribute.java:** POJO representing attribute metadata
- **SDDObject.java:** POJO representing virtual entity metadata
- **SDD.java:** POJO representing the complete SDD template
- **SDDDictionaryMapping.java:** Handles Dictionary Mapping sheet generation

### 11.2. SPARQL Query Patterns

**Find all attributes for an SDD:**
```sparql
PREFIX hasco: <http://hadatac.org/ont/hasco/>

SELECT ?attr WHERE {
  ?attr a hasco:SDDAttribute .
  ?attr hasco:partOfSchema <https://hadatac.org/ont/hadatac#/SDDICT{timestamp}> .
}
```

**Find all virtual objects for an SDD:**
```sparql
PREFIX hasco: <http://hadatac.org/ont/hasco/>

SELECT ?obj WHERE {
  ?obj a hasco:SDDObject .
  ?obj hasco:partOfSchema <https://hadatac.org/ont/hadatac#/SDDICT{timestamp}> .
}
```

### 11.3. RDF Triple Patterns

**SDDAttribute example:**
```turtle
:SDDATT1773851870316631/1 a hasco:SDDAttribute ;
  rdfs:label "Temperature" ;
  hasco:hasAttribute envo:09200001 ;
  hasco:isVariableOf :SDDOBJ1773851870316631/7 ;
  hasco:hasUnit unit:DEG_C ;
  hasco:hasEvent :SDDOBJ1773851870316631/9 ;
  hasco:hasEntity envo:01001079 ;
  hasco:partOfSchema :SDDICT1773851870316631 .
```

**SDDObject example:**
```turtle
:SDDOBJ1773851870316631/7 a hasco:SDDObject ;
  rdfs:label "??weather" ;
  hasco:hasEntity envo:01001079 ;
  hasco:partOfSchema :SDDICT1773851870316631 .
```

---

## 12. Summary

The **SDD (Semantic Data Dictionary)** is the cornerstone of semantic data integration in HADatAc. It:

1. **Transforms** raw data columns into ontologically-grounded concepts
2. **Establishes** the complete semantic context through a dual entity system (Attributes + Objects)
3. **Enables** FAIR data principles through explicit, machine-readable semantics
4. **Integrates** with DSG and DP2 to complete the metadata ecosystem
5. **Powers** the Knowledge Graph with rich, queryable semantic relationships

By carefully designing and maintaining SDDs, researchers ensure their data is not just stored, but **understood**—enabling automated reasoning, data integration, and long-term reusability.

---

**Document Version:** 2.0  
**Last Updated:** 2026-03-19  
**Author:** GitHub Copilot (based on comprehensive codebase analysis)

