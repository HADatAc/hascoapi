# R Analysis Curl Examples

## 1) Valid payload expected 200

```bash
curl -X POST "http://localhost:9000/hascoapi/api/r-analysis/execute" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d @docs/APIChanges/payloads/r_analysis_valid_payload.json
```

Expected response shape:

```json
{
  "isSuccessful": true,
  "body": {
    "runId": "RA-...",
    "status": "completed",
    "startedAt": "...",
    "finishedAt": "...",
    "durationMs": 1234,
    "outputs": [],
    "summary": {
      "datasets": 0,
      "variables": 0,
      "images": 0,
      "totalAssociations": 0
    }
  }
}
```

## 2) Invalid payload expected 400

```bash
curl -X POST "http://localhost:9000/hascoapi/api/r-analysis/execute" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d @docs/APIChanges/payloads/r_analysis_invalid_payload.json
```

Expected response shape:

```json
{
  "isSuccessful": false,
  "error": {
    "code": "invalid_payload",
    "message": "Payload validation failed",
    "details": [
      {
        "field": "processUri",
        "message": "Required non-empty string"
      }
    ]
  }
}
```

## 3) Auth failure expected 401 or 403 (when auth is enabled)

Enable auth in configuration:

```hocon
hascoapi.r_analysis.require_auth = true
```

Missing token:

```bash
curl -X POST "http://localhost:9000/hascoapi/api/r-analysis/execute" \
  -H "Content-Type: application/json" \
  -d @docs/APIChanges/payloads/r_analysis_valid_payload.json
```

Expected: HTTP 401 with `error.code = "unauthorized"`.

Invalid scheme/token:

```bash
curl -X POST "http://localhost:9000/hascoapi/api/r-analysis/execute" \
  -H "Authorization: Basic abc" \
  -H "Content-Type: application/json" \
  -d @docs/APIChanges/payloads/r_analysis_valid_payload.json
```

Expected: HTTP 403 with `error.code = "forbidden"`.
