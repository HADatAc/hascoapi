# Test WKF Delete and Uningest
# This script tests the WKF delete and uningest endpoints

$baseUrl = "http://localhost:9000/hascoapi/api"

# Test with the WKF URI from the logs
$wkfUri = "https://hadatac.org/ont/hadatac%23/WKF1770821621458861"

Write-Host "===== Testing WKF Delete and Uningest =====" -ForegroundColor Cyan
Write-Host ""

# Test 1: Get WKF to confirm it exists
Write-Host "Test 1: Getting WKF details..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/wkf/admin@example.com/9/0" -Method GET
    Write-Host "SUCCESS: Retrieved WKFs" -ForegroundColor Green
    Write-Host ($response | ConvertTo-Json -Depth 5)
} catch {
    Write-Host "FAILED: Could not retrieve WKFs" -ForegroundColor Red
    Write-Host $_.Exception.Message
}

Write-Host ""
Write-Host "---"
Write-Host ""

# Test 2: Test Uningest
Write-Host "Test 2: Testing Uningest..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/uningest/mt/$wkfUri" -Method GET
    Write-Host "SUCCESS: Uningest completed" -ForegroundColor Green
    Write-Host ($response | ConvertTo-Json -Depth 5)
} catch {
    Write-Host "FAILED: Uningest failed" -ForegroundColor Red
    Write-Host $_.Exception.Message
}

Write-Host ""
Write-Host "---"
Write-Host ""

# Test 3: Test Delete
Write-Host "Test 3: Testing Delete..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/wkf/delete/$wkfUri" -Method POST
    Write-Host "SUCCESS: Delete completed" -ForegroundColor Green
    Write-Host ($response | ConvertTo-Json -Depth 5)
} catch {
    Write-Host "FAILED: Delete failed" -ForegroundColor Red
    Write-Host $_.Exception.Message
}

Write-Host ""
Write-Host "===== Test Complete =====" -ForegroundColor Cyan
