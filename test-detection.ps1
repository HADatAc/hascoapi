# Script de Teste: Detecao de Tipos de Instruments
# Este script verifica se a deteccao de tipos vstoi esta funcionando

$baseUrl = "http://localhost:9000"
$token = "qwertyuiopasdfghjklzxcvbnm123456"

Write-Host "`n========================================"
Write-Host "TESTE DE DETECCAO DE TIPOS VSTOI"
Write-Host "========================================`n"

# PASSO 1: Verificar servidor
Write-Host "Passo 1: Verificando servidor..."
try {
    $health = Invoke-RestMethod -Uri "$baseUrl/hascoapi/version" -Method GET
    Write-Host "  OK Servidor rodando: $health" -ForegroundColor Green
} catch {
    Write-Host "  ERRO Servidor nao esta respondendo!" -ForegroundColor Red
    exit
}

# PASSO 2: Listar SOCs
Write-Host "`nPasso 2: Listando SOCs..."
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/hascoapi/api/studyobjectcollection/elements/10/0" `
        -Method GET `
        -Headers @{"Authorization" = "Bearer $token"}

    if ($response.isSuccessful -and $response.body) {
        Write-Host "  OK Encontrados $($response.body.Count) SOCs:" -ForegroundColor Green
        foreach ($soc in $response.body) {
            Write-Host "    - $($soc.label)"
        }
    } else {
        Write-Host "  AVISO Nenhum SOC encontrado" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  ERRO ao listar SOCs" -ForegroundColor Red
}

# PASSO 3: Verificar Instruments
Write-Host "`nPasso 3: Verificando Instruments criados..."
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/hascoapi/api/instrument/elements/10/0" `
        -Method GET `
        -Headers @{"Authorization" = "Bearer $token"}

    if ($response.isSuccessful -and $response.body -and $response.body.Count -gt 0) {
        Write-Host "  OK Encontrados $($response.body.Count) Instruments:" -ForegroundColor Green
        foreach ($inst in $response.body | Select-Object -First 5) {
            Write-Host "    - $($inst.label)"
        }
    } else {
        Write-Host "  ERRO Nenhum Instrument encontrado!" -ForegroundColor Red
    }
} catch {
    Write-Host "  ERRO ao verificar Instruments" -ForegroundColor Red
}

# PASSO 4: Verificar total
Write-Host "`nPasso 4: Verificando total de Instruments..."
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/hascoapi/api/instrument/elements/total" `
        -Method GET `
        -Headers @{"Authorization" = "Bearer $token"}

    if ($response.isSuccessful) {
        Write-Host "  Total: $($response.body)" -ForegroundColor Green
    } else {
        Write-Host "  ERRO Falha ao obter total" -ForegroundColor Red
    }
} catch {
    Write-Host "  ERRO ao verificar total" -ForegroundColor Red
}

# PASSO 5: Verificar Components
Write-Host "`nPasso 5: Verificando Components..."
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/hascoapi/api/component/elements/10/0" `
        -Method GET `
        -Headers @{"Authorization" = "Bearer $token"}

    if ($response.isSuccessful -and $response.body -and $response.body.Count -gt 0) {
        Write-Host "  OK Encontrados $($response.body.Count) Components:" -ForegroundColor Green
        foreach ($comp in $response.body | Select-Object -First 5) {
            Write-Host "    - $($comp.label)"
        }
    } else {
        Write-Host "  ERRO Nenhum Component encontrado!" -ForegroundColor Red
    }
} catch {
    Write-Host "  ERRO ao verificar Components" -ForegroundColor Red
}

# PASSO 6: Verificar ComponentStems
Write-Host "`nPasso 6: Verificando ComponentStems..."
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/hascoapi/api/componentstem/elements/10/0" `
        -Method GET `
        -Headers @{"Authorization" = "Bearer $token"}

    if ($response.isSuccessful -and $response.body -and $response.body.Count -gt 0) {
        Write-Host "  OK Encontrados $($response.body.Count) ComponentStems:" -ForegroundColor Green
        foreach ($stem in $response.body | Select-Object -First 5) {
            Write-Host "    - $($stem.label)"
        }
    } else {
        Write-Host "  ERRO Nenhum ComponentStem encontrado!" -ForegroundColor Red
    }
} catch {
    Write-Host "  ERRO ao verificar ComponentStems" -ForegroundColor Red
}

Write-Host "`n========================================`n"

