# DA-SOC Manual Ingestion Test Script
# This script manually ingests DA-SOC-LOCATION.csv through the HADatAc API

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "DA-SOC MANUAL INGESTION TEST" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Configuration
$baseUrl = "http://localhost:9000"
$apiEndpoint = "$baseUrl/hascoapi/api"
$csvFile = "test\resources\da\DA-SOC-LOCATION.csv"
$fileName = "DA-SOC-LOCATION.csv"

# Check if CSV file exists
if (-Not (Test-Path $csvFile)) {
    Write-Host "❌ ERROR: CSV file not found: $csvFile" -ForegroundColor Red
    exit 1
}

Write-Host "✅ CSV file found: $csvFile" -ForegroundColor Green
Write-Host "   File size: $((Get-Item $csvFile).Length) bytes`n" -ForegroundColor Gray

# Step 1: Check if server is running
Write-Host "Step 1: Checking if HADatAc server is running..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$baseUrl/hascoapi" -Method GET -TimeoutSec 5 -UseBasicParsing
    Write-Host "✅ Server is running (HTTP $($response.StatusCode))`n" -ForegroundColor Green
} catch {
    Write-Host "❌ ERROR: Server is not running or not reachable" -ForegroundColor Red
    Write-Host "   Make sure to start the server with: sbt run`n" -ForegroundColor Yellow
    exit 1
}

# Step 2: Upload the CSV file
Write-Host "Step 2: Uploading DA-SOC-LOCATION.csv..." -ForegroundColor Yellow

# Create multipart form data
$boundary = [System.Guid]::NewGuid().ToString()
$LF = "`r`n"

$fileBytes = [System.IO.File]::ReadAllBytes($csvFile)
$fileEnc = [System.Text.Encoding]::GetEncoding('iso-8859-1').GetString($fileBytes)

$bodyLines = (
    "--$boundary",
    "Content-Disposition: form-data; name=`"file`"; filename=`"$fileName`"",
    "Content-Type: text/csv$LF",
    $fileEnc,
    "--$boundary--$LF"
) -join $LF

try {
    # Upload file using general upload endpoint
    # The system should auto-detect DA-SOC-* pattern and route to AnnotateDASOC
    $uploadResponse = Invoke-WebRequest `
        -Uri "$apiEndpoint/upload" `
        -Method POST `
        -ContentType "multipart/form-data; boundary=$boundary" `
        -Body $bodyLines `
        -UseBasicParsing

    Write-Host "✅ File uploaded successfully" -ForegroundColor Green
    Write-Host "   HTTP Status: $($uploadResponse.StatusCode)" -ForegroundColor Gray
    Write-Host "   Response: $($uploadResponse.Content)`n" -ForegroundColor Gray

} catch {
    Write-Host "❌ ERROR: File upload failed" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "   Response: $($_.Exception.Response.Content)`n" -ForegroundColor Red
    exit 1
}

# Step 3: Check ingestion status
Write-Host "Step 3: Checking ingestion status..." -ForegroundColor Yellow
Write-Host "   (Waiting 3 seconds for ingestion to complete...)" -ForegroundColor Gray
Start-Sleep -Seconds 3

# Parse response to get DataFile URI
try {
    $responseJson = $uploadResponse.Content | ConvertFrom-Json
    $dataFileUri = $responseJson.dataFileUri

    if ($dataFileUri) {
        Write-Host "✅ DataFile URI: $dataFileUri`n" -ForegroundColor Green

        # Get ingestion log
        $logEndpoint = "$apiEndpoint/ingestion/$dataFileUri/log"
        Write-Host "Fetching ingestion log from: $logEndpoint" -ForegroundColor Gray

        $logResponse = Invoke-WebRequest -Uri $logEndpoint -Method GET -UseBasicParsing
        Write-Host "`n========================================" -ForegroundColor Cyan
        Write-Host "INGESTION LOG" -ForegroundColor Cyan
        Write-Host "========================================`n" -ForegroundColor Cyan
        Write-Host $logResponse.Content -ForegroundColor White

    } else {
        Write-Host "⚠️  WARNING: Could not extract DataFile URI from response" -ForegroundColor Yellow
    }
} catch {
    Write-Host "⚠️  WARNING: Could not fetch ingestion log" -ForegroundColor Yellow
    Write-Host "   Error: $($_.Exception.Message)`n" -ForegroundColor Yellow
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "DA-SOC INGESTION TEST COMPLETE" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

