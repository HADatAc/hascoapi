# Script para limpar TODOS os dados de teste antes de começar
Write-Host "`n========== LIMPANDO DADOS DE TESTE ==========" -ForegroundColor Cyan

Write-Host "`nExecutando limpeza de dados..." -ForegroundColor Yellow
sbt "runMain org.hascoapi.tests.CleanupTestData"

Write-Host "`n✓ Limpeza completa!" -ForegroundColor Green
Write-Host "`nAgora você pode executar os testes com dados limpos:" -ForegroundColor White
Write-Host "  .\run-setup-tests.ps1" -ForegroundColor Cyan

