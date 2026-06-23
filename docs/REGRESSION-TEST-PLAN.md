# HASCOAPI Regression Test Plan (v2)

## 1. Introduction & Goal

This document outlines a multi-phase plan to develop a comprehensive regression test suite for the HASCOAPI. The primary goal is to ensure that new changes do not break existing functionality, enabling confident and rapid development. The test suite will validate the API's behavior, data integrity, and adherence to its documented specifications.

This plan focuses on automated API-level testing, simulating a client interacting with the `hascoapi` endpoints.

## 2. Technology Stack

- **Language**: Java (to align with the existing `hascoapi` project)
- **Test Runner**: JUnit 5
- **API Testing Framework**: REST Assured
- **Build Tool**: SBT (as used by the `hascoapi` project)

This stack will be integrated into the existing `hascoapi` project structure under the `test/` directory.

## 3. Scope of Testing

The regression suite will cover the following areas:

- **Endpoint Contract Testing**: Verify that all API endpoints adhere to the documented request/response structure, including status codes, headers, and JSON schemas.
- **Positive Scenarios**: Test the expected behavior of the API with valid inputs and data.
- **Negative Scenarios**: Test the API's resilience and error-handling capabilities with invalid inputs, malformed data, and incorrect parameters.
- **Workflow Testing**: Validate complex, multi-step business processes, such as the metadata ingestion workflow.
- **Data Integrity**: Ensure that create, update, and delete operations are correctly persisted in the triplestore and that data remains consistent.

Authentication and authorization testing are currently out of scope but can be added in a future phase if required.

## 4. Phased Development Plan

The test suite will be built incrementally in the following phases:

### Phase 1: Foundation and Setup

1.  **Dependency Integration**: Add `JUnit 5` and `REST Assured` dependencies to the `build.sbt` file.
2.  **Configuration**: Set up a dedicated test configuration (`application.test.conf`) to point the API to a separate, ephemeral test database (e.g., an in-memory Fuseki or a temporary Docker container).
3.  **Base Test Class**: Create a `BaseApiTest` class that handles:
    -   Configuration loading.
    -   Setup and teardown logic (e.g., clearing the test database before each test run).
    -   Common REST Assured request specifications (e.g., setting `baseURI`, `port`, and common headers).
4.  **Utility Classes**: Develop helper classes for:
    -   Generating test data (e.g., creating unique URIs, sample JSON payloads).
    -   Common API interactions (e.g., a reusable `createEntity(type, payload)` method).

### Phase 2: Read-Only Endpoint Testing (GET Requests)

This phase focuses on non-destructive tests that validate data retrieval.

1.  **Core Retrieval & Provenance**:
    -   Test `GET /api/uri/:uri` for various entity types.
    -   Test `GET /api/hascotype/:uri`.
    -   Test `GET /api/urigen/:elementtype` to ensure it returns a valid, unique URI format.
    -   Test `GET /api/usage/:uri` to verify the identification of entity dependencies.
    -   Test `GET /api/derivation/:uri` to check for provenance information.
2.  **Hierarchy and Ontology**:
    -   Test `GET /api/children/:superuri` to retrieve direct subclasses.
    -   Test `GET /api/subclasses/keyword/:superuri/:keyword` for searching within a class hierarchy.
    -   Test `GET /api/instances/keyword/:classuri/:keyword` for retrieving class instances.
3.  **Paginated Lists & Search**:
    -   Test `GET /api/:elementType/elements/:pageSize/:offset` for key element types (`study`, `datafile`, `instrument`, etc.).
    -   Verify correct pagination behavior (`pageSize`, `offset`).
    -   Test `GET /api/:elementType/elements/total` and assert its count matches the list endpoint.
    -   Test basic keyword searches: `GET /api/:elementType/keyword/:keyword/...`.
    -   Test advanced search filters, including combinations of `managerEmail`, `status`, `studyUri`, and `socUri`.
4.  **Negative Cases**:
    -   Request non-existent URIs and assert `404 Not Found` or appropriate API error messages.
    -   Use invalid `elementType` and assert failure.

### Phase 3: Write Endpoint Testing (POST, DELETE)

This phase tests the creation and deletion of resources. All tests in this phase must be idempotent, meaning they clean up any data they create.

1.  **Entity Lifecycle**: For each major element type (`study`, `instrument`, `sdd`, `dsg`, etc.):
    -   Create a test that `POST`s a new entity.
    -   Verify the response contains the correct data and a `2xx` status code.
    -   Use a `GET` request to retrieve the entity by its new URI and validate its contents.
    -   `DELETE` the newly created entity.
    -   Verify the delete operation was successful with another `GET` request (expecting a `404` or error).
2.  **Input Validation**:
    -   Send malformed JSON payloads and assert `400 Bad Request`.
    -   Send requests with missing required fields and verify failure.
3.  **Repository Management**:
    -   Test `GET /api/repo/ont/load` to ensure ontologies can be loaded into a clean test database.
    -   Test `GET /api/repo/ont/delete` to ensure ontologies can be cleared.

### Phase 4: Workflow and Business Logic Testing

This phase focuses on validating complex, multi-step operations that are critical to the API's function.

1.  **Metadata Ingestion Workflow**:
    -   Create a test that fully simulates the 3-step ingestion process:
        1.  `POST` to `/api/dsg/create/{json}` to create the `DataFile` entity.
        2.  `POST` to `/api/uploadFile/{elementuri}/{filename}` to upload a sample `.xlsx` file.
        3.  `POST` to `/api/ingest/{status}/{elementType}/{elementUri}` to trigger the ingestion.
    -   Poll the `/api/ingestion/:dataFileUri/log` endpoint to check for successful completion.
    -   Verify that the expected entities (Study, Study Objects, etc.) were created in the triplestore.
    -   Clean up by calling `/api/uningest/:dataFileUri`.
2.  **Instrument Assembly Workflow**:
    -   Create an `Instrument`.
    -   Create `ContainerSlots`.
    -   Create a `Component` (e.g., a question).
    -   Test `GET /api/slots/container/attach/...` to attach the component.
    -   Verify the attachment.
    -   Test `GET /api/slots/container/detach/...` and verify detachment.

## 5. Test Data Management

-   A separate, clean triplestore will be used for each test run.
-   Tests will be responsible for creating their own required data.
-   Sample data files (e.g., `DSG-CORPORATION.xlsx`) will be stored in the `test/resources` directory.

## 6. Execution and CI Integration

-   **Local Execution**: Tests will be runnable locally via the `sbt test` command.
-   **Continuous Integration**: A GitHub Actions workflow will be created in `.github/workflows/regression-tests.yml`. This workflow will trigger on every pull request to the `main` branch, automatically running the full regression suite to prevent merging broken code.
