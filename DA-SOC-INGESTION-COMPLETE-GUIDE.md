# Correção Aplicada: Extração do Filename do Multipart

## ✅ Problema Identificado

O backend estava usando apenas `request.getQueryString("filename")` (deprecated), mas o PowerShell com `Invoke-RestMethod` NÃO envia query parameters quando usa multipart/form-data.

## ✅ Solução Implementada

### IngestionAPI.java (linha 145-170)

Agora o backend tenta extrair o filename em **3 níveis**:

```java
// NÍVEL 1: Extrair do multipart/form-data (NOVO!)
if (request.body() != null && request.body().asMultipartFormData() != null) {
    play.mvc.Http.MultipartFormData multipart = request.body().asMultipartFormData();
    play.mvc.Http.MultipartFormData.FilePart<Object> filePart = multipart.getFile("file");
    
    if (filePart != null) {
        filename = filePart.getFilename();  // ← ESTE é o filename REAL!
    }
}

// NÍVEL 2: Fallback para query parameter (compatibilidade)
if (filename == null || filename.isEmpty()) {
    filename = request.getQueryString("filename");
}

// NÍVEL 3: Último recurso
if (filename == null || filename.isEmpty()) {
    filename = "DA-SOC-UNKNOWN.csv";
}
```

## 🔄 Resultado Esperado

### Logs ANTES da Correção
```
[INGESTION PATH] Filename from query parameter: DA-SOC-UNKNOWN.csv
[INGESTION PATH] Filename: DA-SOC-UNKNOWN.csv
IngestionAPI.ingest(): Looking for file at path: C:\hascoapi\var\resources\DFL.../DA-SOC-UNKNOWN.csv
[ERROR] File not found or empty after 20 attempts
```

### Logs DEPOIS da Correção
```
[INGESTION PATH] Filename from multipart: DA-SOC-EQUIPMENT-MODULE.csv
[INGESTION PATH] Filename: DA-SOC-EQUIPMENT-MODULE.csv
DASOC file detected - SOC name from filename: EQUIPMENT-MODULE
Auto-detected SOC URI: http://pmsr.net/ont/pmsr#SOC-EQUIPMENT-MODULE
Processing as DASOC (Data Acquisition - Study Object Collection)
... (ingestão procede normalmente)
```

## 🚀 Próximos Passos

### 1. **RECOMPILAR O BACKEND** ⚠️ OBRIGATÓRIO
```bash
# Pare o sbt run atual (Ctrl+C no terminal do backend)
# Depois:
sbt compile
sbt run
```

**IMPORTANTE**: As mudanças estão no código Java, então o backend PRECISA ser recompilado e reiniciado.

### 2. Executar Script de Ingestão
```powershell
.\ingest-dasocs.ps1
```

### 3. Verificar Enriquecimento
```powershell
.\verify-vstoi-api.ps1
```

## 📋 Verificação Manual (Se Necessário)

### Verificar se Instruments foram enriquecidos

**API:**
```
GET http://localhost:9000/hascoapi/api/instrument/elements/5/0
```

**Propriedades Esperadas (do DA-SOC):**
- `hasShortName` ✅
- `hasLanguage` ✅
- `hasVersion` ✅
- `hasWebDocument` ✅
- `superUri` (rdfs:subClassOf) ✅
- `hasFirst` ✅

### Verificar se Components foram enriquecidos

**API:**
```
GET http://localhost:9000/hascoapi/api/component/elements/5/0
```

**Propriedades Esperadas:**
- `hasComponentStem` ✅
- `hasCodebook` ✅

### Verificar se ComponentStems foram enriquecidos

**API:**
```
GET http://localhost:9000/hascoapi/api/componentstem/elements/5/0
```

**Propriedades Esperadas:**
- `hasContent` ✅
- `hasLanguage` ✅
- `hasVersion` ✅

### Verificar se ContainerSlots foram enriquecidos

**API:**
```
GET http://localhost:9000/hascoapi/api/containerslot/elements/5/0
```

**Propriedades Esperadas:**
- `belongsTo` ✅
- `hasComponent` ✅
- `hasNext` ✅
- `hasPrevious` ✅
- `hasPriority` ✅

## ⚡ Status Atual

✅ **Código corrigido** - Extração de filename do multipart implementada
⏳ **Aguardando recompilação** - Backend precisa ser reiniciado
🎯 **Pronto para testar** - Script de ingestão atualizado e pronto

## 🎁 Resultado Final Esperado

Após reiniciar o backend e executar `ingest-dasocs.ps1`:

```
========================================
INGESTAO E VERIFICACAO DE DA-SOCs PMSR
========================================

FASE 1: INGESTAO DOS DA-SOCs
========================================

Ingerindo: DA-SOC-EQUIPMENT-MODULE.csv
  OK Ingestao bem-sucedida!

Ingerindo: DA-SOC-CONTROL-MODULE.csv
  OK Ingestao bem-sucedida!

Ingerindo: DA-SOC-COMPONENT-STEM.csv
  OK Ingestao bem-sucedida!

Ingerindo: DA-SOC-SLOT-ELEMENT.csv
  OK Ingestao bem-sucedida!

========================================

FASE 2: VERIFICACAO VIA API
========================================

Verificando Instruments...
  Total recuperados: 5
  Com hasShortName: 5 ✅

Verificando Components...
  Total recuperados: 5
  Com hasComponentStem: 5 ✅

Verificando ComponentStems...
  Total recuperados: 4
  Com hasContent: 4 ✅

========================================
RESUMO FINAL
========================================

Ingestao dos DA-SOCs:
  Sucesso: 4 / 4 ✅
  Falhas: 0

STATUS: SUCESSO TOTAL ✅
Todos os DA-SOCs foram ingeridos com sucesso!
```

