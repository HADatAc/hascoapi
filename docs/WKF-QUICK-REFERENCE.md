# WKF - Guia Rápido de Referência

**Versão:** 1.0  
**Última atualização:** 2026-02-12

---

## 🎯 O Que É o WKF?

**WKF (Workflow)** é um Metadata Template que descreve processos e tarefas em workflows científicos.

---

## 📋 Estrutura do Arquivo WKF

### Arquivo Excel (.xlsx) com as seguintes sheets:

1. **InfoSheet** - Metadados gerais do workflow
2. **Namespaces** - Prefixos e URIs de ontologias
3. **Workflows** - Definição dos workflows
4. **Tasks** - Tarefas individuais do workflow
5. **Dependencies** (opcional) - Dependências entre tarefas

---

## 🚀 Operações Disponíveis

### 1. **Criar WKF**

**Frontend:**
```
1. Ir para: Manage > Add Metadata Template
2. Selecionar tipo: wkf
3. Preencher:
   - Name: Nome do workflow
   - Version: Ex: 1.0
   - Comment: Descrição
4. Upload arquivo .xlsx
5. Clicar "Submit"
```

**API:**
```http
POST /hascoapi/api/datafile/create/
Content-Type: application/json
Body: {
  "uri": "DFL{timestamp}",
  "label": "My Workflow",
  "filename": "WKF-MyWorkflow.xlsx",
  "fileStatus": "UNPROCESSED",
  "hasSIRManagerEmail": "user@example.com"
}

POST /hascoapi/api/wkf/create/
Content-Type: application/json
Body: {
  "uri": "WKF{timestamp}",
  "label": "My Workflow",
  "hasDataFileUri": "DFL{timestamp}",
  "hasVersion": "1.0",
  "comment": "Description",
  "hasSIRManagerEmail": "user@example.com"
}

POST /hascoapi/api/uploadFile/{wkfUri}/{filename}
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
Body: [arquivo binário .xlsx]
```

---

### 2. **Listar WKFs**

**Frontend:**
```
1. Ir para: Manage > View Metadata Templates
2. Selecionar tipo: wkf
```

**API:**
```http
GET /hascoapi/api/wkf/
Authorization: Bearer {JWT_TOKEN}

Response:
[
  {
    "uri": "https://hadatac.org/ont/hadatac#/WKF123...",
    "label": "My Workflow",
    "hasStatus": "DRAFT",
    "hasVersion": "1.0",
    "hasDataFileUri": "https://hadatac.org/ont/hadatac#/DFL123..."
  }
]
```

---

### 3. **Fazer Ingestão (Processar)**

**Frontend:**
```
1. Listar WKFs
2. Selecionar WKF(s)
3. Clicar "Ingest WKFs Selected"
```

**API:**
```http
GET /hascoapi/api/ingest/{wkfUri}
Authorization: Bearer {JWT_TOKEN}

Response:
{
  "status": "success",
  "message": "WKF ingested successfully"
}
```

**O que acontece:**
- ✅ Arquivo .xlsx é lido de `resources/DFL{id}/`
- ✅ Conteúdo é parseado (workflows, tasks, etc.)
- ✅ RDF triples são criados e salvos no Fuseki
- ✅ Status do DataFile muda para `PROCESSED`

---

### 4. **Fazer Uningest (Reverter)**

**Frontend:**
```
1. Listar WKFs
2. Selecionar WKF(s) PROCESSED
3. Clicar "Uningest WKFs Selected"
```

**API:**
```http
GET /hascoapi/api/uningest/mt/{wkfUri}
Authorization: Bearer {JWT_TOKEN}

Response:
{
  "status": "success",
  "message": "Uningest completed successfully"
}
```

**O que acontece:**
- ✅ Triples de **conteúdo** são deletados (workflows, tasks, etc.)
- ✅ **WKF e DataFile são PRESERVADOS**
- ✅ Status do DataFile muda para `UNPROCESSED`
- ✅ WKF **continua visível** na lista

---

### 5. **Deletar WKF**

**Frontend:**
```
1. Listar WKFs
2. Selecionar WKF(s)
3. Clicar "Delete WKFs Selected"
```

**API:**
```http
POST /hascoapi/api/wkf/delete/{wkfUri}
Authorization: Bearer {JWT_TOKEN}

Response:
{
  "status": "success",
  "message": "WKF deleted successfully"
}
```

**O que acontece:**
- ✅ DataFile é deletado do Fuseki
- ✅ WKF é deletado do Fuseki
- ✅ WKF **desaparece** da lista
- ⚠️ Arquivo .xlsx permanece em `resources/` (não é deletado automaticamente)

---

## 🗂️ Localização dos Arquivos

### No Sistema de Arquivos:
```
C:\hascoapi\var\resources\
└── DFL{timestamp}\
    └── WKF-MyWorkflow.xlsx
```

**Exemplo:**
```
C:\hascoapi\var\resources\DFL1770818725372881\WKF-WeatherStation.xlsx
```

### No Fuseki (Triple Store):

**Named Graph:** `<https://hadatac.org/ont/hadatac#/DFL{timestamp}>`

**Conteúdo:**
- Metadata do WKF (URI, label, version, status)
- Metadata do DataFile (filename, status)
- Triples de conteúdo (workflows, tasks - apenas se PROCESSED)

---

## 📊 Status do WKF

| Status | Significado |
|--------|-------------|
| `DRAFT` | WKF criado, mas não ingesto |
| `UNPROCESSED` | Arquivo uploaded, aguardando ingestão |
| `PROCESSED` | Conteúdo ingesto no Fuseki |

---

## 🔄 Fluxo de Estados

```
┌──────────┐
│  DRAFT   │ ← Estado inicial após criação
└────┬─────┘
     │ Upload file
     ▼
┌──────────────┐
│ UNPROCESSED  │ ← Arquivo uploaded, pronto para ingestão
└────┬─────────┘
     │ Ingest
     ▼
┌──────────┐
│PROCESSED │ ← Conteúdo no Fuseki
└────┬─────┘
     │ Uningest
     ▼
┌──────────────┐
│ UNPROCESSED  │ ← Pode ser re-ingesto
└──────────────┘

(Delete pode ser feito em qualquer estado)
```

---

## ⚠️ Diferença: Uningest vs Delete

### Uningest:
- ✅ Remove **conteúdo** (workflows, tasks)
- ✅ Preserva **WKF** e **DataFile**
- ✅ WKF **continua na lista**
- ✅ Pode fazer **re-ingest** depois
- Status: `PROCESSED` → `UNPROCESSED`

### Delete:
- ❌ Remove **tudo** (WKF + DataFile + conteúdo)
- ❌ WKF **desaparece da lista**
- ❌ **Não pode reverter**
- Arquivo .xlsx permanece no disco

---

## 🔍 Como Debugar

### 1. Verificar se WKF existe no Fuseki:
```sparql
SELECT * WHERE {
  ?s rdf:type hasco:WKF .
  ?s rdfs:label ?label .
  ?s hasco:hasDataFile ?dataFile .
}
```

### 2. Verificar se arquivo foi uploaded:
```bash
# Verificar diretório
dir C:\hascoapi\var\resources\ /s /b | findstr WKF
```

### 3. Verificar logs da API:
```bash
# Buscar por erros
type logs\application.log | findstr /i "ERROR WKF"

# Buscar por sucessos
type logs\application.log | findstr /i "SUCCESS WKF"
```

### 4. Verificar status do DataFile:
```sparql
SELECT * WHERE {
  <https://hadatac.org/ont/hadatac#/DFL123...> hasco:hasFileStatus ?status .
}
```

---

## 🚨 Problemas Comuns

### 1. "File not found" durante ingestão
**Causa:** Arquivo não foi uploaded corretamente  
**Solução:**
```bash
# Verificar se arquivo existe
dir C:\hascoapi\var\resources\DFL{id}\*.xlsx

# Se não existir, fazer re-upload via API
POST /hascoapi/api/uploadFile/{wkfUri}/{filename}
```

### 2. WKF desapareceu após uningest
**Causa:** Bug antigo (já corrigido)  
**Solução:** Atualizar código para versão atual

### 3. "WKF does not have an associated DataFile URI"
**Causa:** WKF foi criado sem DataFile  
**Solução:**
```java
// Verificar se hasDataFileUri está preenchido
WKF wkf = WKF.find(uri);
System.out.println(wkf.getHasDataFileUri());  // Deve retornar DFL URI
```

### 4. Upload retorna 404
**Causa:** WKF não foi encontrado pelo GenericFind  
**Solução:** Verificar se WKF está registrado em `GenericFind.java`:
```java
} else if (hascoType.equals(HASCO.WKF)) {
    instance = WKF.find(uri);  // ✅ Deve existir
}
```

---

## 📚 Arquivos de Código Relevantes

| Arquivo | Responsabilidade |
|---------|------------------|
| `WKF.java` | Modelo de dados do WKF |
| `WKFAPI.java` | Endpoints REST para WKF |
| `IngestionAPI.java` | Upload, ingest, uningest |
| `GenericFind.java` | Encontrar WKF por URI |
| `WKFGenerator.java` | Parser do arquivo .xlsx |
| `routes` | Rotas HTTP |

---

## 🔐 Autenticação

Todas as operações requerem JWT token:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Como obter token:**
1. Frontend: Automático via Drupal session
2. API direta: POST `/hascoapi/api/authenticate`

---

## 📊 Exemplo Completo

### Cenário: Criar e ingerir um WKF

```bash
# 1. Criar DataFile
curl -X POST http://localhost:9000/hascoapi/api/datafile/create/ \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {TOKEN}" \
  -d '{
    "uri": "https://hadatac.org/ont/hadatac#/DFL1770818725372881",
    "label": "Weather Station Workflow",
    "filename": "WKF-WeatherStation.xlsx",
    "fileStatus": "UNPROCESSED",
    "hasSIRManagerEmail": "admin@example.com"
  }'

# 2. Criar WKF
curl -X POST http://localhost:9000/hascoapi/api/wkf/create/ \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {TOKEN}" \
  -d '{
    "uri": "https://hadatac.org/ont/hadatac#/WKF1770818725372881",
    "label": "Weather Station Workflow",
    "hasDataFileUri": "https://hadatac.org/ont/hadatac#/DFL1770818725372881",
    "hasVersion": "1.0",
    "comment": "Workflow for weather data collection",
    "hasSIRManagerEmail": "admin@example.com"
  }'

# 3. Upload arquivo
curl -X POST http://localhost:9000/hascoapi/api/uploadFile/https%3A%2F%2Fhadatac.org%2Font%2Fhadatac%23%2FWKF1770818725372881/WKF-WeatherStation.xlsx \
  -H "Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" \
  -H "Authorization: Bearer {TOKEN}" \
  --data-binary @WKF-WeatherStation.xlsx

# 4. Verificar arquivo foi salvo
dir C:\hascoapi\var\resources\DFL1770818725372881\WKF-WeatherStation.xlsx

# 5. Fazer ingestão
curl -X GET http://localhost:9000/hascoapi/api/ingest/https%3A%2F%2Fhadatac.org%2Font%2Fhadatac%23%2FWKF1770818725372881 \
  -H "Authorization: Bearer {TOKEN}"

# 6. Verificar status
curl -X GET http://localhost:9000/hascoapi/api/wkf/ \
  -H "Authorization: Bearer {TOKEN}"
```

---

## 📞 Suporte

**Documentação Completa:**
- `WKF-INGESTION-COMPLETE-FIX-FINAL.md` - Solução detalhada dos problemas
- `WKF-TECHNICAL-ARCHITECTURE.md` - Arquitetura e fluxos de dados

**Logs:**
- `logs/application.log` - Logs da API
- Console do Play Framework - Logs em tempo real

**Contato:** Time de Desenvolvimento HADatAc

---

**Última atualização:** 2026-02-12  
**Versão do Sistema:** 2.0 (com WKF completo)
