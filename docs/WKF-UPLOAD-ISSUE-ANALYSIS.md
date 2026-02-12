# WKF Upload Issue - Root Cause Analysis

## 🔴 PROBLEMA IDENTIFICADO

O arquivo WKF **NÃO está sendo enviado para o backend** antes da ingestão ser disparada.

## 📊 Evidência dos Logs

```
IngestionAPI.ingest(): Checking for pre-uploaded file at: C:\hascoapi\var\resources\DFL1770747318933181\WKF-WeatherStation.xlsx
IngestionAPI.ingest(): No file found, will wait for uploaded file in filesystem...
IngestionAPI.ingest(): Looking for file at path: C:\hascoapi\var\resources\DFL1770747318933181\WKF-WeatherStation.xlsx
IngestionAPI.ingest(): Waiting for file to be available... (attempt 1/10)
...
IngestionAPI.ingest(): Waiting for file to be available... (attempt 10/10)
[ERROR] IngestionAPI.ingest(): Uploaded file not found
```

**❌ NÃO HÁ NENHUM LOG de `DataFileAPI.uploadFile()` sendo chamado!**

## 🔍 O que deveria acontecer

### Fluxo Correto (INS - que funciona)

1. **Frontend chama:** `POST /hascoapi/api/uploadFile/{DFL_URI}/INS-arquivo.xlsx`
   - Backend recebe o arquivo
   - Log: `=== DataFileAPI.uploadFile() START ===`
   - Salva assíncronamente em: `C:/hascoapi/var/resources/DFL.../INS-arquivo.xlsx`
   
2. **Frontend espera alguns segundos** (para o async terminar)

3. **Frontend chama:** `POST /hascoapi/api/dp2/ingest/{WKF_URI}`
   - Backend procura o arquivo em `C:/hascoapi/var/resources/DFL.../`
   - ✅ Encontra e faz ingest

### Fluxo Atual (WKF - que falha)

1. **Frontend NÃO chama** `uploadFile()` ❌
   - Nenhum arquivo é enviado
   
2. **Frontend chama diretamente:** `POST /hascoapi/api/wkf/ingest/{WKF_URI}`
   - Backend procura o arquivo
   - ❌ Não encontra (porque nunca foi enviado!)
   - Espera 10 segundos
   - ❌ Ainda não existe
   - Retorna erro 404

## ✅ CORREÇÕES APLICADAS NO BACKEND

### 1. Estrutura de Diretórios Corrigida

**Antes:** Salvava em `C:/hascoapi/var/arquivo.xlsx` (errado)  
**Depois:** Salva em `C:/hascoapi/var/resources/{DFL_URI}/arquivo.xlsx` (correto)

### 2. Logs Melhorados

Adicionei logs detalhados em:
- `DataFileAPI.uploadFile()` - Para ver quando o upload é chamado
- `DataFileAPI.saveFile()` - Para ver o que acontece durante o save assíncrono
- `IngestionAPI.ingest()` - Para debug do fluxo de ingestão

### 3. Tempo de Espera Aumentado

- **Antes:** 10 tentativas × 500ms = 5 segundos
- **Depois:** 20 tentativas × 1000ms = 20 segundos

### 4. Validação de Arquivo Vazio

Agora também verifica se `uploadedFile.length() > 0` para evitar ingerir arquivos vazios.

## 🚨 O QUE O FRONTEND PRECISA FAZER

### Opção 1: Chamar uploadFile() antes do ingest (RECOMENDADO)

```javascript
// 1. Upload do arquivo
const uploadResponse = await fetch(
  `/hascoapi/api/uploadFile/${dataFileUri}/${filename}`,
  {
    method: 'POST',
    body: file, // Raw file object
    headers: {
      'Content-Type': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    }
  }
);

// 2. Aguardar async save (2-3 segundos é suficiente)
await new Promise(resolve => setTimeout(resolve, 3000));

// 3. Disparar ingestão
const ingestResponse = await fetch(
  `/hascoapi/api/wkf/ingest/${wkfUri}`,
  { method: 'POST' }
);
```

### Opção 2: Enviar arquivo no corpo do ingest

```javascript
const ingestResponse = await fetch(
  `/hascoapi/api/wkf/ingest/${wkfUri}`,
  {
    method: 'POST',
    body: file,
    headers: {
      'Content-Type': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    }
  }
);
```

## 🎯 COMO VALIDAR A CORREÇÃO

### Logs que você DEVE VER (se o frontend estiver correto):

```
=== DataFileAPI.uploadFile() START ===
DataFileAPI.uploadFile() called with elementUri: https://hadatac.org/ont/hadatac#/DFL...
[DEBUG] DataFileAPI.saveFile() START
[DEBUG] tempFile: C:\...\temp_file.xlsx
[DEBUG] Creating directories: C:\hascoapi\var\resources\DFL...
[SUCCESS] File successfully saved to: C:\hascoapi\var\resources\DFL.../WKF-WeatherStation.xlsx
[DEBUG] DataFileAPI.saveFile() END

== NEW wkf ===========================================================
IngestionAPI.ingest() with elementUri = https://hadatac.org/ont/hadatac#/WKF...
IngestionAPI.ingest(): Checking for pre-uploaded file at: C:\hascoapi\var\resources\DFL.../WKF-WeatherStation.xlsx
IngestionAPI.ingest(): Found pre-uploaded file!
IngestionAPI.ingest(): API has read DataFile from triplestore
```

### Como verificar manualmente:

1. Após fazer upload (e antes de ingest), verifique:
   ```powershell
   dir C:\hascoapi\var\resources -Recurse
   ```
   
2. Você DEVE ver:
   ```
   C:\hascoapi\var\resources\
     DFL1770747318933181\
       WKF-WeatherStation.xlsx
   ```

## 📝 RESUMO

| Item | Status | Observação |
|------|--------|------------|
| Backend salva no lugar correto | ✅ CORRIGIDO | `resources/{DFL}/arquivo.xlsx` |
| Backend procura no lugar correto | ✅ JÁ ESTAVA OK | Mesmo path |
| Backend aguarda arquivo async | ✅ MELHORADO | 20s em vez de 5s |
| **Frontend envia arquivo** | ❌ **PROBLEMA ATUAL** | **Não está chamando uploadFile()** |

## 🔧 PRÓXIMO PASSO

**O frontend precisa ser corrigido para chamar a API de upload antes de disparar a ingestão.**

Caso contrário, o arquivo nunca chegará ao backend e a ingestão sempre falhará com 404.
