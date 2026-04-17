# Script Completo: Ingestao e Verificacao de DA-SOCs PMSR
$baseUrl = "http://localhost:9000"
$token = "qwertyuiopasdfghjklzxcvbnm123456"
$daFolder = "C:\Users\kaell\Desktop\Project\hascoapi\test\resources\dapmsr"

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "INGESTAO E VERIFICACAO DE DA-SOCs PMSR" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Lista de DA-SOCs na ordem correta de ingestao
$daFiles = @(
    @{Name="DA-SOC-EQUIPMENT-MODULE.csv"; Uri="http://hadatac.org/kb/DA-PMSR-EQUIPMENT-1"; Description="Instrument properties"}
    @{Name="DA-SOC-CONTROL-MODULE.csv"; Uri="http://hadatac.org/kb/DA-PMSR-CONTROL-1"; Description="Component properties"}
    @{Name="DA-SOC-COMPONENT-STEM.csv"; Uri="http://hadatac.org/kb/DA-PMSR-STEM-1"; Description="ComponentStem properties"}
    @{Name="DA-SOC-SLOT-ELEMENT.csv"; Uri="http://hadatac.org/kb/DA-PMSR-SLOT-1"; Description="ContainerSlot properties"}
)

Write-Host "FASE 1: INGESTAO DOS DA-SOCs" -ForegroundColor Yellow
Write-Host "========================================`n"

$ingestedCount = 0
$failedCount = 0

foreach ($daFile in $daFiles) {
    $filePath = Join-Path $daFolder $daFile.Name
    $encodedUri = [System.Uri]::EscapeDataString($daFile.Uri)

    Write-Host "Ingerindo: $($daFile.Name)" -ForegroundColor Cyan
    Write-Host "  Descricao: $($daFile.Description)" -ForegroundColor Gray
    Write-Host "  URI: $($daFile.Uri)" -ForegroundColor Gray

    if (!(Test-Path $filePath)) {
        Write-Host "  ERRO: Arquivo nao encontrado: $filePath" -ForegroundColor Red
        $failedCount++
        continue
    }

    try {
        # Faz upload do arquivo usando multipart/form-data
        $boundary = [System.Guid]::NewGuid().ToString()
        $LF = "`r`n"

        $fileBytes = [System.IO.File]::ReadAllBytes($filePath)
        $fileContent = [System.Text.Encoding]::GetEncoding("iso-8859-1").GetString($fileBytes)

        $bodyLines = (
            "--$boundary",
            "Content-Disposition: form-data; name=`"file`"; filename=`"$($daFile.Name)`"",
            "Content-Type: text/csv$LF",
            $fileContent,
            "--$boundary--$LF"
        ) -join $LF

        $url = "$baseUrl/hascoapi/api/ingest/DRAFT/da/$encodedUri"

        $response = Invoke-RestMethod -Uri $url `
            -Method POST `
            -ContentType "multipart/form-data; boundary=$boundary" `
            -Body $bodyLines `
            -Headers @{"Authorization" = "Bearer $token"} `
            -ErrorAction Stop

        if ($response.isSuccessful) {
            Write-Host "  OK Ingestao bem-sucedida!" -ForegroundColor Green
            $ingestedCount++
        } else {
            Write-Host "  ERRO: $($response.body)" -ForegroundColor Red
            $failedCount++
        }

    } catch {
        Write-Host "  ERRO: $_" -ForegroundColor Red
        $failedCount++
    }

    Write-Host ""
    Start-Sleep -Milliseconds 500
}

Write-Host "----------------------------------------"
Write-Host "Ingestao Completa: $ingestedCount sucesso, $failedCount falhas" -ForegroundColor Cyan
Write-Host "========================================`n"

# Aguarda processamento
Write-Host "Aguardando processamento..." -ForegroundColor Yellow
Start-Sleep -Seconds 3

Write-Host "`nFASE 2: VERIFICACAO VIA API" -ForegroundColor Yellow
Write-Host "========================================`n"

# Verifica via API se as entidades foram enriquecidas
Write-Host "Verificando Instruments..." -ForegroundColor Cyan
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/hascoapi/api/instrument/elements/5/0" `
        -Method GET `
        -Headers @{"Authorization" = "Bearer $token"}

    if ($response.isSuccessful -and $response.body) {
        $withShortName = ($response.body | Where-Object { $_.hasShortName }).Count
        Write-Host "  Total recuperados: $($response.body.Count)" -ForegroundColor Gray
        Write-Host "  Com hasShortName: $withShortName" -ForegroundColor $(if ($withShortName -gt 0) { "Green" } else { "Yellow" })

        if ($withShortName -gt 0) {
            Write-Host "`n  Exemplos:" -ForegroundColor Gray
            $response.body | Where-Object { $_.hasShortName } | Select-Object -First 2 | ForEach-Object {
                Write-Host "    - $($_.label): hasShortName=$($_.hasShortName)" -ForegroundColor DarkGray
            }
        }
    }
} catch {
    Write-Host "  ERRO: $_" -ForegroundColor Red
}

Write-Host "`nVerificando Components..." -ForegroundColor Cyan
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/hascoapi/api/component/elements/5/0" `
        -Method GET `
        -Headers @{"Authorization" = "Bearer $token"}

    if ($response.isSuccessful -and $response.body) {
        $withStem = ($response.body | Where-Object { $_.hasComponentStem }).Count
        Write-Host "  Total recuperados: $($response.body.Count)" -ForegroundColor Gray
        Write-Host "  Com hasComponentStem: $withStem" -ForegroundColor $(if ($withStem -gt 0) { "Green" } else { "Yellow" })

        if ($withStem -gt 0) {
            Write-Host "`n  Exemplos:" -ForegroundColor Gray
            $response.body | Where-Object { $_.hasComponentStem } | Select-Object -First 2 | ForEach-Object {
                Write-Host "    - $($_.label): hasComponentStem=$($_.hasComponentStem)" -ForegroundColor DarkGray
            }
        }
    }
} catch {
    Write-Host "  ERRO: $_" -ForegroundColor Red
}

Write-Host "`nVerificando ComponentStems..." -ForegroundColor Cyan
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/hascoapi/api/componentstem/elements/5/0" `
        -Method GET `
        -Headers @{"Authorization" = "Bearer $token"}

    if ($response.isSuccessful -and $response.body) {
        $withContent = ($response.body | Where-Object { $_.hasContent }).Count
        Write-Host "  Total recuperados: $($response.body.Count)" -ForegroundColor Gray
        Write-Host "  Com hasContent: $withContent" -ForegroundColor $(if ($withContent -gt 0) { "Green" } else { "Yellow" })

        if ($withContent -gt 0) {
            Write-Host "`n  Exemplos:" -ForegroundColor Gray
            $response.body | Where-Object { $_.hasContent } | Select-Object -First 2 | ForEach-Object {
                $preview = $_.hasContent.Substring(0, [Math]::Min(40, $_.hasContent.Length))
                Write-Host "    - $($_.label): hasContent=$preview..." -ForegroundColor DarkGray
            }
        }
    }
} catch {
    Write-Host "  ERRO: $_" -ForegroundColor Red
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "RESUMO FINAL" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

Write-Host "`nIngestao dos DA-SOCs:" -ForegroundColor White
Write-Host "  Sucesso: $ingestedCount / $($daFiles.Count)" -ForegroundColor $(if ($ingestedCount -eq $daFiles.Count) { "Green" } else { "Yellow" })
Write-Host "  Falhas: $failedCount" -ForegroundColor $(if ($failedCount -eq 0) { "Green" } else { "Red" })

if ($ingestedCount -eq $daFiles.Count) {
    Write-Host "`nSTATUS: SUCESSO TOTAL" -ForegroundColor Green
    Write-Host "Todos os DA-SOCs foram ingeridos com sucesso!" -ForegroundColor Gray
} elseif ($ingestedCount -gt 0) {
    Write-Host "`nSTATUS: SUCESSO PARCIAL" -ForegroundColor Yellow
    Write-Host "Alguns DA-SOCs foram ingeridos, mas houve falhas." -ForegroundColor Gray
} else {
    Write-Host "`nSTATUS: FALHA" -ForegroundColor Red
    Write-Host "Nenhum DA-SOC foi ingerido com sucesso." -ForegroundColor Gray
}

Write-Host "`n========================================`n" -ForegroundColor Cyan

