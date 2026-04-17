# ============================================================================
# SCRIPT: Ingestão Manual do DA-SOC-LOCATION.csv
# ============================================================================
# Este script faz a ingestão manual do DA-SOC-LOCATION.csv via API
# ============================================================================

$HASCOAPI_URL = "http://localhost:9000"
$AUTH_TOKEN = "your-auth-token-here" # Substitua com token válido

Write-Host ""
Write-Host ("=" * 80) -ForegroundColor Cyan
Write-Host "INGESTÃO MANUAL DO DA-SOC-LOCATION.CSV" -ForegroundColor Cyan
Write-Host ("=" * 80) -ForegroundColor Cyan
Write-Host ""

# Passo 1: Verificar arquivo
$csvFile = "test\resources\da\DA-SOC-LOCATION.csv"
if (!(Test-Path $csvFile)) {
    Write-Host "ERRO: Arquivo não encontrado: $csvFile" -ForegroundColor Red
    exit 1
}

$fileInfo = Get-Item $csvFile
Write-Host "Passo 1: Arquivo encontrado" -ForegroundColor Green
Write-Host "  Caminho: $($fileInfo.FullName)" -ForegroundColor White
Write-Host "  Tamanho: $($fileInfo.Length) bytes" -ForegroundColor White
Write-Host ""

# Passo 2: Criar DataFile URI
$timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$dataFileUri = "http://hadatac.org/kb/default/DA-TEST-$timestamp"
$filename = "DA-SOC-LOCATION.csv"

Write-Host "Passo 2: URIs criados" -ForegroundColor Green
Write-Host "  DataFile URI: $dataFileUri" -ForegroundColor White
Write-Host "  Filename: $filename" -ForegroundColor White
Write-Host ""

# Passo 3: Upload do arquivo
Write-Host "Passo 3: Fazendo upload do arquivo..." -ForegroundColor Yellow

# Ler o arquivo como bytes
$fileBytes = [System.IO.File]::ReadAllBytes($fileInfo.FullName)

# Fazer a requisição de upload
$uploadUrl = "$HASCOAPI_URL/hascoapi/api/uploadFile/$([Uri]::EscapeDataString($dataFileUri))/$([Uri]::EscapeDataString($filename))"

Write-Host "  URL: $uploadUrl" -ForegroundColor Gray

try {
    $headers = @{
        "Content-Type" = "text/csv"
    }

    $uploadResponse = Invoke-RestMethod -Uri $uploadUrl -Method POST -Body $fileBytes -Headers $headers -ContentType "text/csv"

    Write-Host "  Upload bem-sucedido!" -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host "  ERRO no upload: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    exit 1
}

# Passo 4: Criar elemento DataFile no triplestore (via API ou diretamente)
Write-Host "Passo 4: O arquivo foi salvo no servidor" -ForegroundColor Green
Write-Host "  Localização: C:\hascoapi\var\resources\DA-TEST-$timestamp\DA-SOC-LOCATION.csv" -ForegroundColor White
Write-Host ""

# Passo 5: Ingerir via API
Write-Host "Passo 5: Solicitando ingestão do DA-SOC..." -ForegroundColor Yellow

$elementUri = $dataFileUri
$ingestUrl = "$HASCOAPI_URL/hascoapi/api/ingest/da/$([Uri]::EscapeDataString($elementUri))"

Write-Host "  URL: $ingestUrl" -ForegroundColor Gray

try {
    $ingestResponse = Invoke-RestMethod -Uri $ingestUrl -Method POST -ContentType "application/json"

    Write-Host "  Ingestão solicitada com sucesso!" -ForegroundColor Green
    Write-Host "  Resposta: $($ingestResponse | ConvertTo-Json)" -ForegroundColor White
    Write-Host ""
} catch {
    Write-Host "  ERRO na ingestão: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    exit 1
}

Write-Host ""
Write-Host ("=" * 80) -ForegroundColor Cyan
Write-Host "INGESTÃO COMPLETA" -ForegroundColor Cyan
Write-Host ("=" * 80) -ForegroundColor Cyan
Write-Host ""
Write-Host "Agora execute o script de consulta para verificar os resultados:" -ForegroundColor Yellow
Write-Host "  .\query-dasoc-simple.ps1" -ForegroundColor White
Write-Host ""

