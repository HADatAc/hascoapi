# WKF MT v1.2.1 Migration TODO (hascoapi)

## Scope
Align hascoapi WKF ingestion, generation, POJOs, and APIs with WKF-SPEC-V2 v1.2.1.

## Completed In This Iteration
- [x] Update WKF InfoSheet expected keys to include hasStudyDescription.
- [x] Update WKF InfoSheet validation to expect 7 data rows and validate hasStudyDescription -> #STD.
- [x] Skip hasStudyDescription key as metadata pointer in AnnotateWKF generator-chain assembly.
- [x] Enforce Task typing semantics in ingestion:
  - hasco:hascoType fixed to vstoi:Task
  - rdf:type allowed to carry task subclass
  - backward-compatible migration path from legacy templates
- [x] Add STD metadata extraction in post-processing and feed it into ProcessBasedStudy generation.
- [x] Make ProcessBasedStudyGenerator consume STD metadata as canonical source, with fallback auto-generation.
- [x] Include educational STD fields on generated ProcessBasedStudy triples:
  - vstoi:hasLearningObjectives
  - vstoi:hasCriticalActions
  - vstoi:hasDebriefingFocus
- [x] Update WKF workbook generation skeleton:
  - InfoSheet includes hasStudyDescription -> #STD
  - STD sheet is created with v1.2.1 headers
- [x] Update WKF Tasks/RequiredInstruments export queries to support task subclasses and hasco-type driven discovery.

## Remaining High Priority
- [x] Populate STD data rows during WKF generation from ProcessBasedStudy (not only headers/skeleton).
- [x] Validate STD sheet semantics at ingestion level (required columns, URI/email/date formats, row cardinality).
- [x] Add strict validation for Tasks sheet column C:
  - hasco:hascoType MUST equal vstoi:Task for all task rows.
- [x] Add strict validation for Tasks sheet column B:
  - rdf:type MUST be vstoi:Task or subclass of vstoi:Task.
- [ ] Ensure ProcessBasedStudy ingestion output always links:
  - hasco:hasProcess -> Process URI
  - ownership fields PI/Institution by URI where provided in STD.

## Remaining Medium Priority
- [ ] Review Process POJO legacy study metadata comments and deprecate process-level study fields in favor of STD source.
- [ ] Review ProcessBasedStudyAPI payload contracts to ensure STD-originated metadata serialization is complete and stable.
- [ ] Review WKFAPI generation endpoints to guarantee generated workbook ordering: InfoSheet, Namespaces, STD, ProcessStems, Processes, Tasks, RequiredInstruments.
- [ ] Add compatibility warning path for legacy WKF files missing STD.

## Tests To Add / Update
- [ ] Ingestion test: valid v1.2.1 WKF with STD creates ProcessBasedStudy with STD metadata.
- [ ] Ingestion test: legacy task typing in column C is normalized correctly.
- [ ] Validation test: hasStudyDescription != #STD triggers warning/error behavior as intended.
- [ ] Generation test: exported WKF includes STD sheet and v1.2.1 InfoSheet rows.
- [ ] Roundtrip test: ingest -> generate preserves task typing semantics and required links.

## Files Already Changed
- app/org/hascoapi/utils/MTSheet.java
- app/org/hascoapi/ingestion/AnnotateWKF.java
- app/org/hascoapi/ingestion/WKFGenerator.java
- app/org/hascoapi/ingestion/ProcessBasedStudyGenerator.java
- app/org/hascoapi/transform/mt/wkf/WKFGen.java
- app/org/hascoapi/transform/mt/wkf/WKFTasks.java
- app/org/hascoapi/transform/mt/wkf/WKFRequiredInstruments.java
