# Debug do response de instruments

Write-Host "=== Debug Instruments Response ===" -ForegroundColor Cyan
Write-Host ""

# Endpoint 1: Listar instruments
Write-Host "1. Listando instruments..." -ForegroundColor Yellow
$response1 = Invoke-WebRequest -Method GET `
  -Uri "http://localhost:9000/hascoapi/api/instrument/elements/10/0" `
  -UseBasicParsing

Write-Host "Status: $($response1.StatusCode)" -ForegroundColor Green
Write-Host "`nRaw Content (primeiros 1000 chars):" -ForegroundColor Yellow
Write-Host $response1.Content.Substring(0, [Math]::Min(1000, $response1.Content.Length))

Write-Host "`n`nJSON Parsed:" -ForegroundColor Yellow
$json1 = $response1.Content | ConvertFrom-Json
Write-Host "isSuccessful: $($json1.isSuccessful)"
Write-Host "Body type: $($json1.body.GetType().FullName)"
Write-Host "Body content (raw): [$($json1.body)]"
Write-Host "Body length: $($json1.body.Length)"

Write-Host "`n"
Write-Host "=== Debug Components Response ===" -ForegroundColor Cyan
$response2 = Invoke-WebRequest -Method GET `
  -Uri "http://localhost:9000/hascoapi/api/component/elements/10/0" `
  -UseBasicParsing

Write-Host "Status: $($response2.StatusCode)" -ForegroundColor Green
Write-Host "`nRaw Content:" -ForegroundColor Yellow
Write-Host $response2.Content.Substring(0, [Math]::Min(500, $response2.Content.Length))

