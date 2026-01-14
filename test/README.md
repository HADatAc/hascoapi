# Tests

This project uses **sbt** to run JUnit tests (JUnit Jupiter).

The main test suite is the **round-trip scaffold**:

- `test/org/hascoapi/tests/HascoRoundtripTest.java`

It validates that Machine-Readable Templates (MTs) can be **ingested** into the triplestore and then **regenerated** back to Excel deterministically.

---

## Quick start: run the round-trip test

From the repository root:

```bash
sbt -no-colors --allow-empty 'testOnly org.hascoapi.tests.HascoRoundtripTest -- -v'
```

### Running with `sudo` (only if your environment needs it)

Some environments require elevated permissions (for example, when the configured ingestion output path is under `/var/hascoapi`). In that case:

```bash
sudo sbt -no-colors --allow-empty 'testOnly org.hascoapi.tests.HascoRoundtripTest -- -v'
```

---

## What the round-trip test does

`HascoRoundtripTest` is organized as a 3-step lifecycle per MT type:

1. **Step 1 — Ingest**: Excel → triplestore
2. **Step 2 — Regenerate & compare**: triplestore → Excel (+ comparison)
3. **Step 3 — Reset & deterministic re-ingest**: delete ingested data → re-ingest → regenerate again (comparison)

At the moment, only **DSG** is fully wired end-to-end (other MTs are listed but intentionally skipped until their authoritative test workbooks and regen/reset APIs are implemented).

### DSG input workbook

The authoritative DSG workbook used by Step 1 is:

- `test/resources/dsg/DSG-STD-test.xlsx`

### Generated artifacts (Excel outputs)

During Step 2 and Step 3, the generated DSG Excel outputs are copied into the repository for easy download/diffing:

- `test/resources/generated/DSG-STD-test-regenerated.xlsx`
- `test/resources/generated/DSG-STD-test-regenerated-step3.xlsx`

> Note: The generator also writes to the configured ingestion directory (commonly `/var/hascoapi/`). The test then copies the file into `test/resources/generated/`.

### Comparisons ("superset" semantics)

The comparison logic is **not strict equality**. The regenerated workbook must contain **everything that was ingested**, but it is allowed to contain **more**.

This is intentional because the triplestore may already contain additional metadata.

### Triplestore validation

For DSG, Step 3 also performs a lightweight SPARQL-based validation to ensure that key graph elements implied by the workbook (e.g., minimum SOC count) exist in the triplestore.

### Cleanup (runs last)

The test class contains a final cleanup test that runs last (by method name ordering):

- `zzz_cleanup_triplestore_ingestions()`

It performs a **best-effort scoped delete** of what was ingested by the test (currently the known DSG study), and it **does not delete** any generated Excel artifacts under `test/resources/generated/`.

---

## Sanity checks

There is also a scaffold sanity test:

- `sanity_z_listsAllMtTypes()`

It checks:

- All expected MT types exist in the `MTType` enum
- The DSG input workbook exists and is non-empty
- Other MT inputs are intentionally not wired yet (expected `null`)
- The `test/resources/generated/` folder exists (or can be created)
- If DSG generated artifacts exist, they are non-empty

---

## Other tests in this folder

You may also find unit-style tests for specific generators/ingestion behavior, for example:

- `IngestionWorkerDSGTest.java`
- `StudyGeneratorTest.java`
- `StudyObjectGeneratorTest.java`
- `SSDGeneratorTest.java`

Run all tests with:

```bash
sbt -no-colors test
```

---

## Troubleshooting

### 1) "No generator chain produced. Aborting ingestion gracefully."

This usually means the ingestion pipeline could not find a usable generator chain for the provided workbook/template combination (mapping mismatch, missing required sheets, or a configuration mismatch).

For the round-trip scaffold, ensure:

- The input file exists: `test/resources/dsg/DSG-STD-test.xlsx`
- The template used by the test exists: `conf/template.generic.conf`

### 2) Permission errors writing output

If generation tries to write to a protected ingestion directory (often `/var/hascoapi/`), run the test with `sudo`:

```bash
sudo sbt -no-colors --allow-empty 'testOnly org.hascoapi.tests.HascoRoundtripTest -- -v'
```

### 3) Triplestore not running / SPARQL failures

The DSG regeneration and SPARQL checks depend on a reachable triplestore configured by the application (SPARQL query/update endpoints).

If endpoints are down, Step 2/3 will fail. Start your docker-compose stack (Fuseki, etc.) as you normally do for HASCOAPI development.

