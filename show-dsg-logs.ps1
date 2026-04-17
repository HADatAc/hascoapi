# Script para buscar e exibir logs do ultimo DSG ingerido
$baseUrl = "http://localhost:9000"
$token = "qwertyuiopasdfghjklzxcvbnm123456"

Write-Host "`n========================================"
Write-Host "LOGS DA ULTIMA INGESTAO DSG"
Write-Host "========================================`n"

# Busca o DataFile mais recente do tipo DSG
Write-Host "Buscando ultimo DSG ingerido..." -ForegroundColor Yellow

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/hascoapi/api/datafile/draft" `
        -Method GET `
        -Headers @{"Authorization" = "Bearer $token"}

    if ($response.isSuccessful -and $response.body) {
        # Filtra apenas DSG files
        $dsgFiles = $response.body | Where-Object { $_.filename -like "DSG-*" } | Sort-Object -Property lastProcessTime -Descending

        if ($dsgFiles.Count -gt 0) {
            $latestDSG = $dsgFiles[0]
            Write-Host "`nUltimo DSG: $($latestDSG.filename)" -ForegroundColor Green
            Write-Host "Status: $($latestDSG.fileStatus)" -ForegroundColor Cyan
            Write-Host "Processado em: $($latestDSG.lastProcessTime)" -ForegroundColor Cyan
            Write-Host "URI: $($latestDSG.uri)" -ForegroundColor Gray

            # Busca os logs desse DataFile
            Write-Host "`n--- LOGS ---`n" -ForegroundColor Yellow

            $logUri = [System.Uri]::EscapeDataString($latestDSG.uri)
            $logResponse = Invoke-RestMethod -Uri "$baseUrl/hascoapi/api/datafile/log/$logUri" `
                -Method GET `
                -Headers @{"Authorization" = "Bearer $token"}

            if ($logResponse.isSuccessful -and $logResponse.body) {
                # Filtra apenas as linhas relevantes para VSTOI
                $logLines = $logResponse.body -split "`n"
                $inVstoiSection = $false

                foreach ($line in $logLines) {
                    if ($line -match "INSTRUMENT INFERENCE") {
                        $inVstoiSection = $true
                        Write-Host $line -ForegroundColor Cyan
                    } elseif ($inVstoiSection) {
                        if ($line -match "COMPLETE" -or $line -match "^$") {
                            Write-Host $line -ForegroundColor Cyan
                            if ($line -match "COMPLETE") {
                                $inVstoiSection = $false
                            }
                        } elseif ($line -match "VSTOI DETECTION|Total SOCs|Processed|Created") {
                            Write-Host $line -ForegroundColor White
                        } elseif ($line -match "ERROR") {
                            Write-Host $line -ForegroundColor Red
                        } else {
                            Write-Host $line -ForegroundColor Gray
                        }
                    }
                }
            } else {
                Write-Host "Nao foi possivel obter os logs" -ForegroundColor Red
            }

        } else {
            Write-Host "Nenhum arquivo DSG encontrado" -ForegroundColor Yellow
        }
    } else {
        Write-Host "Erro ao buscar DataFiles" -ForegroundColor Red
    }

} catch {
    Write-Host "Erro: $_" -ForegroundColor Red
}

Write-Host "`n========================================`n"

