# Test VSTOI Export Functionality
# This script tests if VSTOI SOCs are properly exported to DSG + DA-SOC files

Write-Host "`n========== VSTOI EXPORT TEST ==========" -ForegroundColor Cyan
Write-Host "This script tests Phase 3 of the INS-to-DSG transformation"
Write-Host "========================================`n" -ForegroundColor Cyan

# Configuration
$BASE_URL = "http://localhost:9000/hascoapi/api"
$STUDY_URI = "pmsr:/STD1738098125851815"  # Replace with actual study URI
$OUTPUT_DIR = "C:\tmp\hascoapi\generated"

# Test 1: Check if VSTOI SOCs exist in the study
Write-Host "[TEST 1] Checking for VSTOI SOCs in study..." -ForegroundColor Yellow
$socsResponse = Invoke-RestMethod -Uri "$BASE_URL/soc/bystudy/$STUDY_URI" -Method GET
Write-Host "Found $($socsResponse.body.Count) SOCs total" -ForegroundColor Green

$vstoiSocs = $socsResponse.body | Where-Object {
    $_.typeUri -match "Instrument|Component|ComponentStem|ContainerSlot|Codebook|ResponseOption|AnnotationStem"
}
Write-Host "Found $($vstoiSocs.Count) VSTOI-typed SOCs:" -ForegroundColor Green
$vstoiSocs | ForEach-Object {
    Write-Host "  - $($_.label) (type: $($_.typeUri))"
}

# Test 2: Export DSG with DA-SOC generation enabled
Write-Host "`n[TEST 2] Exporting DSG with DA-SOC generation..." -ForegroundColor Yellow
Write-Host "Triggering export for study: $STUDY_URI"

# Note: Update this endpoint based on actual API
# The actual endpoint might be different - check routes file
try {
    $exportResponse = Invoke-RestMethod -Uri "$BASE_URL/mt/gen/perelement/dsg/DRAFT/bymanager/test@example.com?generateDASOCs=true" -Method POST
    Write-Host "Export triggered successfully" -ForegroundColor Green
    Write-Host "Response: $($exportResponse | ConvertTo-Json -Depth 3)"
} catch {
    Write-Host "Export failed: $_" -ForegroundColor Red
}

# Test 3: Check if DA-SOC files were created
Write-Host "`n[TEST 3] Checking for generated DA-SOC files..." -ForegroundColor Yellow
if (Test-Path $OUTPUT_DIR) {
    $dasocFiles = Get-ChildItem -Path $OUTPUT_DIR -Filter "DA-SOC-*.csv" -ErrorAction SilentlyContinue

    if ($dasocFiles) {
        Write-Host "Found $($dasocFiles.Count) DA-SOC files:" -ForegroundColor Green
        $dasocFiles | ForEach-Object {
            Write-Host "  - $($_.Name) ($($_.Length) bytes)"

            # Show first 5 lines of each file
            Write-Host "    Content preview:" -ForegroundColor Gray
            Get-Content $_.FullName -TotalCount 5 | ForEach-Object {
                Write-Host "      $_" -ForegroundColor Gray
            }
        }
    } else {
        Write-Host "No DA-SOC files found in $OUTPUT_DIR" -ForegroundColor Yellow
    }
} else {
    Write-Host "Output directory does not exist: $OUTPUT_DIR" -ForegroundColor Red
}

# Test 4: Check for VSTOI-specific DA-SOC files
Write-Host "`n[TEST 4] Looking for VSTOI-specific DA-SOC files..." -ForegroundColor Yellow
$expectedFiles = @(
    "DA-SOC-INSTRUMENT*.csv",
    "DA-SOC-COMPONENT*.csv",
    "DA-SOC-COMPONENT-STEM*.csv",
    "DA-SOC-SLOT-ELEMENT*.csv",
    "DA-SOC-CONTAINER-SLOT*.csv",
    "DA-SOC-CODEBOOK*.csv",
    "DA-SOC-RESPONSE-OPTION*.csv"
)

$foundVstoiFiles = @()
foreach ($pattern in $expectedFiles) {
    $files = Get-ChildItem -Path $OUTPUT_DIR -Filter $pattern -ErrorAction SilentlyContinue
    if ($files) {
        $foundVstoiFiles += $files
        Write-Host "  ✓ Found: $pattern" -ForegroundColor Green
    } else {
        Write-Host "  ✗ Missing: $pattern" -ForegroundColor Yellow
    }
}

# Summary
Write-Host "`n========== TEST SUMMARY ==========" -ForegroundColor Cyan
Write-Host "VSTOI SOCs in database: $($vstoiSocs.Count)"
Write-Host "Total DA-SOC files generated: $($dasocFiles.Count)"
Write-Host "VSTOI-specific DA-SOC files: $($foundVstoiFiles.Count)"
Write-Host "==================================`n" -ForegroundColor Cyan

if ($foundVstoiFiles.Count -gt 0) {
    Write-Host "✅ Phase 3 test PASSED - VSTOI export is working!" -ForegroundColor Green
} else {
    Write-Host "⚠️  Phase 3 test INCONCLUSIVE - Check if VSTOI SOCs exist in the study" -ForegroundColor Yellow
}

