# Executive Brief: Enable Real R Execution in HASCOAPI

## Goal
Enable real execution for R analysis requests coming from Drupal CTT by implementing and exposing:
- POST /hascoapi/api/r-analysis/execute

Current blocker observed in production-like validation:
- Drupal validate-only path passes.
- Real execute fails because upstream returns 404 for POST /hascoapi/api/r-analysis/execute.

## Business impact
Until this endpoint exists and responds with valid JSON:
- R execution in workflow remains non-functional (502 from Drupal side).
- End-to-end synthetic and clinical workflow validation is incomplete.
- Pipeline reports always include 1 blocker (rExecute=false).

## Required outcome
After API work, the following must be true:
- Endpoint exists: POST /hascoapi/api/r-analysis/execute
- Returns JSON for success and error (no HTML error pages)
- Returns 2xx on valid payloads
- Returns structured 4xx/5xx on invalid/failed runs

## Input contract expected by Drupal
Drupal sends JSON with this shape:
- studyUri (string)
- processUri (string)
- tool (object): toolUri, name, version, language=R, artifactUri, artifactFilename, sourceRepositoryUri, entrypoint
- associations (object): datasets, variables, images, counts
- arguments (object)
- requestedAt (ISO datetime)
- requestedBy (uid, identifier)

Reference full contract:
- See [HASCOAPI_R_ANALYSIS_EXECUTE_ENDPOINT_HANDOFF.md](HASCOAPI_R_ANALYSIS_EXECUTE_ENDPOINT_HANDOFF.md)

## Minimum API response contract
Success (200):
- isSuccessful: true
- body: include runId, status, timing, outputs and summary (recommended)

Error (4xx/5xx):
- isSuccessful: false
- error: code, message, optional details

Important:
- Always return JSON in API routes.
- Do not return HTML error pages.

## Acceptance criteria
1. Endpoint route is available and callable at POST /hascoapi/api/r-analysis/execute.
2. Valid request returns 200 JSON.
3. Invalid request returns 400 JSON.
4. Auth failures return 401/403 JSON (if JWT is enforced).
5. Drupal validation script reports:
- R_VALIDATE_STATUS=200
- R_EXECUTE_STATUS=200
- R_EXECUTE_IS_SUCCESSFUL=true
6. Unified runner report shows:
- pipelineSuccess=true
- stageSummary.rExecute=true

## Fast verification from Drupal side
1. Run unified pipeline:
- powershell: ./run_full_aspiracao_pipeline.ps1 -OutputJson modules/custom/evidence/aspiracao_pipeline_report_latest.json
2. Inspect JSON report:
- [evidence/aspiracao_pipeline_report_latest.json](evidence/aspiracao_pipeline_report_latest.json)

Expected after API fix:
- blockers array empty
- rExecute true

## Risk notes
- Biggest integration risk is contract drift (field names/response shape).
- Second risk is non-JSON error handling (HTML fallback pages from framework).
- Third risk is timeout under load; keep execution bounded and observable.

## Recommended implementation sequence
1. Add route + controller method.
2. Validate required fields and language=R.
3. Execute R runtime path (local/container/worker).
4. Return structured JSON success/error.
5. Add API tests for 200/400/401/500 paths.
6. Run Drupal unified pipeline and close blocker.

## Ownership and handoff package
Give the API teammate these files:
- [HASCOAPI_R_ANALYSIS_EXECUTIVE_BRIEF.md](HASCOAPI_R_ANALYSIS_EXECUTIVE_BRIEF.md)
- [HASCOAPI_R_ANALYSIS_EXECUTE_ENDPOINT_HANDOFF.md](HASCOAPI_R_ANALYSIS_EXECUTE_ENDPOINT_HANDOFF.md)
- [HASCOAPI_R_ANALYSIS_COPILOT_GUIDE.md](HASCOAPI_R_ANALYSIS_COPILOT_GUIDE.md)
