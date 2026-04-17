# Script para verificar entidades VSTOI via API do hascoapi
$baseUrl = "http://localhost:9000"
$token = "qwertyuiopasdfghjklzxcvbnm123456"

Write-Host "`n========================================"
Write-Host "VERIFICACAO DE INGESTAO VSTOI VIA API"
Write-Host "========================================`n"

$endpoints = @(
    @{Name="Instruments"; Url="/hascoapi/api/instrument/elements/total"; Expected=71}
    @{Name="Components"; Url="/hascoapi/api/component/elements/total"; Expected=16}
    @{Name="ComponentStems"; Url="/hascoapi/api/componentstem/elements/total"; Expected=4}
    @{Name="ContainerSlots"; Url="/hascoapi/api/containerslot/elements/total"; Expected=83}
)

$totalFound = 0
$successCount = 0

foreach ($endpoint in $endpoints) {
    Write-Host "Verificando $($endpoint.Name)..." -ForegroundColor Yellow

    try {
        $response = Invoke-RestMethod -Uri "$baseUrl$($endpoint.Url)" `
            -Method GET `
            -Headers @{"Authorization" = "Bearer $token"} `
            -ErrorAction Stop

        if ($response.isSuccessful) {
            $count = 0

            # Tenta extrair o total de diferentes formatos de resposta
            if ($response.body -is [string]) {
                try {
                    $bodyObj = $response.body | ConvertFrom-Json
                    $count = $bodyObj.total
                } catch {
                    # Se nao for JSON, pode ser um numero direto
                    if ($response.body -match '\d+') {
                        $count = [int]$matches[0]
                    }
                }
            } elseif ($response.body.total) {
                $count = $response.body.total
            } elseif ($response.body -is [array]) {
                $count = $response.body.Count
            } else {
                $count = $response.body
            }

            $totalFound += $count

            if ($count -eq $endpoint.Expected) {
                Write-Host "  OK $count (esperado: $($endpoint.Expected))" -ForegroundColor Green
                $successCount++
            } elseif ($count -gt 0) {
                Write-Host "  AVISO $count (esperado: $($endpoint.Expected))" -ForegroundColor Yellow
            } else {
                Write-Host "  ERRO 0 entidades encontradas!" -ForegroundColor Red
            }
        } else {
            Write-Host "  ERRO Resposta nao bem-sucedida: $($response.body)" -ForegroundColor Red
        }

    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($statusCode -eq 404) {
            Write-Host "  ERRO Endpoint nao existe (404)" -ForegroundColor Red
        } else {
            Write-Host "  ERRO $_" -ForegroundColor Red
        }
    }
}

Write-Host "`n========================================"
Write-Host "RESUMO"
Write-Host "========================================`n"

Write-Host "Total de entidades encontradas: $totalFound" -ForegroundColor Cyan
Write-Host "Endpoints com sucesso: $successCount / $($endpoints.Count)" -ForegroundColor Cyan

if ($totalFound -eq 174 -and $successCount -eq 4) {
    Write-Host "`nSUCESSO! Todas as entidades VSTOI foram ingeridas corretamente!" -ForegroundColor Green
} elseif ($totalFound -gt 0) {
    Write-Host "`nPARCIAL: Algumas entidades foram encontradas, mas faltam outras." -ForegroundColor Yellow
    Write-Host "Verifique os logs da ingestao do DSG." -ForegroundColor Yellow
} else {
    Write-Host "`nERRO: Nenhuma entidade VSTOI encontrada!" -ForegroundColor Red
    Write-Host "`nPossiveis causas:" -ForegroundColor Yellow
    Write-Host "  1. DSG ainda nao foi ingerido" -ForegroundColor Gray
    Write-Host "  2. Deteccao de tipos nao funcionou" -ForegroundColor Gray
    Write-Host "  3. Endpoints de API nao existem" -ForegroundColor Gray
    Write-Host "  4. Problema na criacao das entidades" -ForegroundColor Gray
}

# Tenta listar alguns exemplos
Write-Host "`n========================================"
Write-Host "EXEMPLOS"
Write-Host "========================================`n"

Write-Host "Instruments (primeiros 5):" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/hascoapi/api/instrument/elements/5/0" `
        -Method GET `
        -Headers @{"Authorization" = "Bearer $token"}

    if ($response.isSuccessful -and $response.body) {
        foreach ($item in $response.body | Select-Object -First 5) {
            Write-Host "  - $($item.label)" -ForegroundColor Gray
        }
    } else {
        Write-Host "  (Nenhum encontrado)" -ForegroundColor Gray
    }
} catch {
    Write-Host "  (Erro ao buscar exemplos)" -ForegroundColor Gray
}

Write-Host "`n========================================`n"

