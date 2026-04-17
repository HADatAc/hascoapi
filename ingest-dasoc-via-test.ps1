# ============================================================================
# SCRIPT: Copiar DA-SOC para o diretório do DSG e executar teste
# ============================================================================
# Este script copia o DA-SOC-LOCATION.csv para o diretório do DSG
# e executa o teste de ingestão completo
# ============================================================================

Write-Host ""
Write-Host ("=" * 80) -ForegroundColor Cyan
Write-Host "PREPARANDO INGESTÃO DO DA-SOC-LOCATION.CSV" -ForegroundColor Cyan
Write-Host ("=" * 80) -ForegroundColor Cyan
Write-Host ""

# Passo 1: Localizar o arquivo fonte
$sourceFile = "test\resources\da\DA-SOC-LOCATION.csv"
if (!(Test-Path $sourceFile)) {
    Write-Host "ERRO: Arquivo não encontrado: $sourceFile" -ForegroundColor Red
    exit 1
}

Write-Host "Passo 1: Arquivo fonte encontrado" -ForegroundColor Green
Write-Host "  Caminho: $sourceFile" -ForegroundColor White
Write-Host "  Tamanho: $((Get-Item $sourceFile).Length) bytes" -ForegroundColor White
Write-Host ""

# Passo 2: Encontrar o diretório do DSG mais recente
$resourcesDir = "C:\hascoapi\var\resources"
if (!(Test-Path $resourcesDir)) {
    Write-Host "ERRO: Diretório de recursos não encontrado: $resourcesDir" -ForegroundColor Red
    exit 1
}

# Procurar pelo diretório DFL mais recente
$dsgDirs = Get-ChildItem -Path $resourcesDir -Directory -Filter "DFL*" | Sort-Object LastWriteTime -Descending
if ($dsgDirs.Count -eq 0) {
    Write-Host "ERRO: Nenhum diretório DFL encontrado em $resourcesDir" -ForegroundColor Red
    exit 1
}

$targetDir = $dsgDirs[0].FullName
Write-Host "Passo 2: Diretório DSG mais recente encontrado" -ForegroundColor Green
Write-Host "  Diretório: $targetDir" -ForegroundColor White
Write-Host ""

# Passo 3: Copiar o arquivo DA-SOC
$targetFile = Join-Path $targetDir "DA-SOC-LOCATION.csv"
Copy-Item -Path $sourceFile -Destination $targetFile -Force

if (Test-Path $targetFile) {
    Write-Host "Passo 3: Arquivo DA-SOC copiado com sucesso" -ForegroundColor Green
    Write-Host "  Destino: $targetFile" -ForegroundColor White
    Write-Host "  Tamanho: $((Get-Item $targetFile).Length) bytes" -ForegroundColor White
    Write-Host ""
} else {
    Write-Host "ERRO: Falha ao copiar arquivo" -ForegroundColor Red
    exit 1
}

# Passo 4: Executar o teste SBT
Write-Host ""
Write-Host ("=" * 80) -ForegroundColor Cyan
Write-Host "EXECUTANDO TESTE DE INGESTÃO DO DA-SOC" -ForegroundColor Cyan
Write-Host ("=" * 80) -ForegroundColor Cyan
Write-Host ""

Write-Host "Aguarde... Este processo pode levar alguns minutos." -ForegroundColor Yellow
Write-Host ""

# Executar o teste SBT
sbt "testOnly org.hascoapi.tests.DASOCManualIngestionTest"

Write-Host ""
Write-Host ("=" * 80) -ForegroundColor Cyan
Write-Host "PROCESSO COMPLETO" -ForegroundColor Cyan
Write-Host ("=" * 80) -ForegroundColor Cyan
Write-Host ""
Write-Host "Agora execute o script de verificação:" -ForegroundColor Yellow
Write-Host "  .\check-dasoc-quick.ps1" -ForegroundColor White
Write-Host ""

