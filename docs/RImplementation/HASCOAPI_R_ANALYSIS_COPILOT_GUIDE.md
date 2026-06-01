# Copilot Guide For API Teammate

This file provides ready-to-paste prompts for your teammate to use in Copilot while implementing:
- POST /hascoapi/api/r-analysis/execute

Use these prompts in sequence.

## Prompt 1: Discover current API wiring
Paste in teammate Copilot chat:

I need to implement POST /hascoapi/api/r-analysis/execute in this hascoapi codebase.
Please do a read-only discovery and return:
1) where routes are declared,
2) where controllers/actions are declared,
3) where JSON request validation patterns exist,
4) where auth middleware/guards are applied,
5) one existing endpoint with best-practice error JSON we should mirror.
Return file paths + line references and a minimal implementation plan.

## Prompt 2: Implement endpoint with strict JSON contract
Paste in teammate Copilot chat:

Implement POST /hascoapi/api/r-analysis/execute with this behavior:
- Accept JSON body containing studyUri, processUri, tool object, associations, arguments, requestedAt, requestedBy.
- Validate required fields and tool.language == "R" (case-insensitive accepted).
- On validation errors, return HTTP 400 JSON:
  { "isSuccessful": false, "error": { "code": "invalid_payload", "message": "...", "details": [...] } }
- On success, return HTTP 200 JSON:
  { "isSuccessful": true, "body": { "runId": "...", "status": "completed", "outputs": [...], "summary": {...} } }
- Never return HTML error pages from this endpoint.
- Keep existing project style and logging conventions.
Also add/update route wiring and unit/integration tests.

## Prompt 3: Add resilient error mapping
Paste in teammate Copilot chat:

Harden POST /hascoapi/api/r-analysis/execute error handling:
- Map auth failures to 401/403 JSON.
- Map malformed JSON to 400 JSON.
- Map runtime execution failure to 500 JSON with error.code = "r_execution_failed".
- Map timeout to 504 JSON with error.code = "execution_timeout".
- Ensure all error responses are JSON, not HTML.
Add tests for all these paths.

## Prompt 4: Add structured logs and traceability
Paste in teammate Copilot chat:

Add structured logging to POST /hascoapi/api/r-analysis/execute:
- Generate runId per request.
- Log runId, studyUri, processUri, toolUri, durationMs, status.
- Log validation failures with field-level details.
- Log runtime stderr/stdout summary safely.
Do not log secrets/tokens.
Keep logs aligned with existing logger style in this repo.

## Prompt 5: Contract compatibility check with Drupal
Paste in teammate Copilot chat:

Validate compatibility of POST /hascoapi/api/r-analysis/execute with Drupal CTT expectations:
- Request body fields from Drupal must be accepted without breaking.
- Response JSON must be parseable by a generic proxy client.
- Endpoint path must be exactly /hascoapi/api/r-analysis/execute.
Return a checklist and any mismatches with concrete code edits.

## Prompt 6: Generate curl tests
Paste in teammate Copilot chat:

Create runnable curl examples for POST /hascoapi/api/r-analysis/execute:
1) valid payload expected 200,
2) invalid payload expected 400,
3) auth failure expected 401/403 (if auth enabled).
Also provide sample payload.json files and expected response snippets.

## Prompt 7: Final PR checklist prompt
Paste in teammate Copilot chat:

Before finalizing, run a PR-quality self-review for POST /hascoapi/api/r-analysis/execute:
- route registered and reachable,
- validation complete,
- all error paths return JSON,
- tests added and passing,
- logging/observability included,
- no breaking changes to existing routes.
Return a concise checklist with pass/fail and required fixes.

## What teammate should send back to us
Ask the teammate to provide:
- PR link/commit hash
- exact endpoint URL and auth requirement
- sample 200 response
- sample 400 response
- proof tests passed

Then we will run:
- [run_full_aspiracao_pipeline.ps1](run_full_aspiracao_pipeline.ps1)
- and confirm [evidence/aspiracao_pipeline_report_latest.json](evidence/aspiracao_pipeline_report_latest.json) has rExecute=true.
