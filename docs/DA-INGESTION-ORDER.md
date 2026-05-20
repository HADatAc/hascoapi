# DA Ingestion Order for VSTOI Entities

## Overview

Data Acquisition (DA) files that enrich VSTOI entities **MUST** be ingested in the correct order due to dependencies between entity types. This document specifies the required ingestion sequence.

---

## Required Ingestion Order

### 1️⃣ **DA-SOC-CODEBOOK**

**No dependencies** - Can be ingested first

- **Creates:** Codebooks (vstoi:Codebook)
- **Enriches:** Properties like hasShortName, hasLanguage, hasVersion
- **URI Pattern:** `pmsr:CB-CBK{timestamp}`
- **Example:** `DA-SOC-CODEBOOK_1.csv`

---

### 2️⃣ **DA-SOC-RESPONSE-OPTION**

**No dependencies** - Can be ingested first

- **Creates:** ResponseOptions (vstoi:ResponseOption)
- **Enriches:** Properties like hasContent, hasValue
- **URI Pattern:** `pmsr:ROPT-ROP{timestamp}`
- **Example:** `DA-SOC-RESPONSE-OPTION_1.csv`

---

### 3️⃣ **DA-SOC-COMPONENTSTEM**

**No dependencies** - Can be ingested first

- **Creates:** ComponentStems (vstoi:ComponentStem)
- **Enriches:** Properties like hasContent, hasShortName
- **URI Pattern:** `pmsr:CSTEM-CSM{timestamp}`
- **Example:** `DA-SOC-COMPONENTSTEM_1.csv`

---

### 4️⃣ **DA-SOC-COMPONENT**

**⚠️ Depends on:**
- **Codebooks** (property: `vstoi:hasCodebook`)
- **ComponentStems** (property: `vstoi:hasComponentStem`)

- **Creates:** Components (vstoi:Component)
- **Enriches:** Relationships to Codebooks and ComponentStems
- **URI Pattern:** `pmsr:COMP-COM{timestamp}`
- **Example:** `DA-SOC-COMPONENT_1.csv`

**Why order matters:** If Components are ingested before Codebooks/ComponentStems, the `hasCodebook` and `hasComponentStem` references will point to non-existent URIs.

---

### 5️⃣ **DA-SOC-SLOTELEMENT**

**⚠️ Depends on:**
- **Components** (property: `vstoi:hasElement`)

- **Creates:** SlotElements / ContainerSlots (vstoi:ContainerSlot)
- **Enriches:** Properties like hasPriority, hasElement (references to Components)
- **URI Pattern:** `pmsr:SLOT-SLE{timestamp}` or `pmsr:CTSLOT-CTS{timestamp}`
- **Example:** `DA-SOC-SLOTELEMENT_1.csv`

**Why order matters:** SlotElements reference Components via `hasElement`. If SlotElements are created before Components, these links will be broken.

---

### 6️⃣ **DA-SOC-INSTRUMENT**

**⚠️ Depends on:**
- **SlotElements** (property: `vstoi:hasFirst` → `vstoi:hasNext` chain)

- **Creates:** Instruments/Containers (vstoi:Instrument)
- **Enriches:** Properties like hasFirst (entry point to SlotElement chain), hasLanguage, hasVersion
- **URI Pattern:** `pmsr:INST-INS{timestamp}`
- **Example:** `DA-SOC-INSTRUMENT_1.csv`

**Why order matters:** Instruments use `hasFirst` to link to the first SlotElement in the container, which then chains via `hasNext`. If Instruments are created before SlotElements, the navigation chain cannot be established.

---

## Complete Ingestion Workflow

### Step 1: Ingest DSG (Study Object Collections)

```http
POST /sdd/savedsgeditor
Content-Type: multipart/form-data

file: DSG-PMSR-Simulators.xlsx
```

This creates the base Study Object Collections (SOCs) containing:
- Instruments (as StudyObjects with originalID)
- Components (as StudyObjects with originalID)
- ComponentStems (as StudyObjects with originalID)
- Codebooks (as StudyObjects with originalID)
- etc.

---

### Step 2: Ingest DAs in Correct Order

```http
# 1. Independent entities first (no dependencies)
POST /sdd/uploadfile → DA-SOC-CODEBOOK_1.csv
POST /sdd/uploadfile → DA-SOC-RESPONSE-OPTION_1.csv
POST /sdd/uploadfile → DA-SOC-COMPONENTSTEM_1.csv

# 2. Components (depend on Codebooks + ComponentStems)
POST /sdd/uploadfile → DA-SOC-COMPONENT_1.csv

# 3. SlotElements (depend on Components)
POST /sdd/uploadfile → DA-SOC-SLOTELEMENT_1.csv

# 4. Instruments (depend on SlotElements chain)
POST /sdd/uploadfile → DA-SOC-INSTRUMENT_1.csv
```

---

## Dependency Graph

```
┌─────────────────────┐
│  DA-SOC-CODEBOOK    │ (Independent)
└──────────┬──────────┘
           │
           ├───────────────────┐
           │                   │
┌──────────▼──────────┐  ┌────▼──────────────────┐
│ DA-SOC-COMPONENTSTEM│  │ DA-SOC-RESPONSE-OPTION│ (Independent)
└──────────┬──────────┘  └───────────────────────┘
           │
           │
┌──────────▼──────────┐
│  DA-SOC-COMPONENT   │ (Depends on CB + CSTEM)
└──────────┬──────────┘
           │
           │
┌──────────▼──────────┐
│ DA-SOC-SLOTELEMENT  │ (Depends on Components)
└──────────┬──────────┘
           │
           │
┌──────────▼──────────┐
│ DA-SOC-INSTRUMENT   │ (Depends on SlotElement chain)
└─────────────────────┘
```

---

## Consequences of Wrong Order

### ❌ **If Components ingested before Codebooks:**

```csv
# DA-SOC-COMPONENT_1.csv
originalID,vstoi:hasCodebook
COM1738096258564815,pmsr:/CBK1738096258564815
```

Result: `Component.hasCodebook` → `pmsr:CB-CBK1738096258564815` (does not exist yet)

The Component UI will show:
- ❌ Codebook: **(empty or error)**

---

### ❌ **If Instruments ingested before SlotElements:**

```csv
# DA-SOC-INSTRUMENT_1.csv
originalID,vstoi:hasFirst
INS1739301009974715,pmsr:/SLE1739301009974715
```

Result: `Instrument.hasFirst` → `pmsr:SLOT-SLE1739301009974715` (does not exist yet)

The Instrument UI will show:
- ❌ Slots: **(empty table or error)**

---

## Verification Checklist

After ingesting DAs, verify relationships:

### ✅ **Codebooks appear in Components**

```sparql
SELECT ?comp ?cb WHERE {
  ?comp a vstoi:Component .
  ?comp vstoi:hasCodebook ?cb .
}
```

Expected: All Components should have Codebooks if specified in DA-SOC-COMPONENT.

---

### ✅ **Components appear in SlotElements**

```sparql
SELECT ?slot ?comp WHERE {
  ?slot a vstoi:ContainerSlot .
  ?slot vstoi:hasElement ?comp .
}
```

Expected: All SlotElements should reference Components.

---

### ✅ **SlotElements chain correctly in Instruments**

```sparql
SELECT ?instr ?first ?second WHERE {
  ?instr a vstoi:Instrument .
  ?instr vstoi:hasFirst ?first .
  ?first vstoi:hasNext ?second .
}
```

Expected: Instruments should have `hasFirst`, and SlotElements should chain via `hasNext`.

---

## Troubleshooting

### **Problem:** Codebooks don't appear in Component UI

**Cause:** Prefix mismatch (`CBK` in CSV vs `CB-CBK` in triplestore)

**Solution:** Use `convertToVstoiUri()` in AnnotateDASOC.java (already fixed)

---

### **Problem:** Parent types show warning in logs

**Cause:** URIPage tries to resolve `rdfs:subClassOf` URIs as instances

**Solution:** Use `isOntologyClass()` check in URIPage.java (already fixed)

---

### **Problem:** Language/Version return errors

**Cause:** CSV values like `pmsr:/en` are converted to URIs instead of literals

**Solution:** Use `cleanLiteralValue()` in AnnotateDASOC.java (already fixed)

---

## References

- **Implementation:** `AnnotateDASOC.java`
- **URI Prefix Logic:** `StudyObjectGenerator.generateVSTOIUri()`
- **Entity Enrichment:** `AnnotateDASOC.enrichVstoiEntity()`
- **DSG Specification:** `docs/VSTOI-DSG-IMPLEMENTATION-COMPLETE.md`

---

## Summary

✅ **Always ingest DAs in this order:**
1. Codebook, ResponseOption, ComponentStem (independent)
2. Component (depends on 1)
3. SlotElement (depends on 2)
4. Instrument (depends on 3)

✅ **Verify relationships after ingestion**

✅ **Check logs for warnings about missing references**

---

*Last Updated: 2026-05-15*

