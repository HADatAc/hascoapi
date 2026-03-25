# Script para executar testes de setup e verificar total de elementos
Write-Host "`n========== EXECUTANDO TESTES DE SETUP DE DADOS ==========" -ForegroundColor Cyan

# 1. Executar testes de LinkedSOC
Write-Host "`n[1/2] Executando testes LinkedSOCElementsSetupTest..." -ForegroundColor Yellow
sbt "testOnly org.hascoapi.tests.LinkedSOCElementsSetupTest"

if ($LASTEXITCODE -ne 0) {
    Write-Host "⚠ LinkedSOCElementsSetupTest falhou!" -ForegroundColor Red
} else {
    Write-Host "✓ LinkedSOCElementsSetupTest passou!" -ForegroundColor Green
}

# 2. Executar testes de OrphanSOC
Write-Host "`n[2/2] Executando testes OrphanSOCElementsSetupTest..." -ForegroundColor Yellow
sbt "testOnly org.hascoapi.tests.OrphanSOCElementsSetupTest"

if ($LASTEXITCODE -ne 0) {
    Write-Host "⚠ OrphanSOCElementsSetupTest falhou!" -ForegroundColor Red
} else {
    Write-Host "✓ OrphanSOCElementsSetupTest passou!" -ForegroundColor Green
}

Write-Host "`n========== TESTES COMPLETOS ==========" -ForegroundColor Cyan
Write-Host "`nAgora você deve ter:" -ForegroundColor White
Write-Host "  - 50 elementos LINKED (com named graph)" -ForegroundColor Green
Write-Host "  - 50 elementos ORPHAN (sem named graph)" -ForegroundColor Yellow
Write-Host "  - Total: 100 elementos no banco de dados" -ForegroundColor Cyan
Write-Host "`nVerifique no frontend em: http://localhost:9000/hadatac/soc" -ForegroundColor White

