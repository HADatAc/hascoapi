# ✅ WKF INGESTION - Status e Diagnóstico

**Data**: 2026-02-09  
**MT Type**: WKF (Workflow)  
**Status**: ✅ Backend Implementado | ⏳ Aguardando Teste com Arquivo Real

---

## 🎯 O Que Foi Implementado

### 1. ✅ Estrutura de Classes

#### WKF.java (Entity)
- **Localização**: `app/org/hascoapi/entity/pojo/WKF.java`
- **Status**: ✅ Completo
- **Herança**: Extends `MetadataTemplate`
- **Propriedades**:
  - label, typeUri, hascoTypeUri
  - hasStatus, hasVersion
  - hasDataFileUri, comment
  - hasSIRManagerEmail

#### AnnotateWKF.java (Annotator)
- **Localização**: `app/org/hascoapi/ingestion/AnnotateWKF.java`
- **Status**: ✅ Completo com logging detalhado
- **Função**: Processa o arquivo Excel WKF e cria generators
- **Sheets Suportadas**:
  1. ✅ `ProcessStems` → WKFGenerator("processstem")
  2. ✅ `Processes` → WKFGenerator("process")
  3. ✅ `Tasks` → WKFGenerator("task")
  4. ✅ `RequiredInstruments` → WKFGenerator("requiredinstrument")

#### WKFGenerator.java (Generator)
- **Localização**: `app/org/hascoapi/ingestion/WKFGenerator.java`
- **Status**: ✅ Completo
- **Função**: Gera triples RDF para cada tipo de elemento WKF
- **Tipos Suportados**:
  - `processstem` → vstoi:ProcessStem
  - `process` → vstoi:Process
  - `task` → vstoi:Task
  - `requiredinstrument` → vstoi:RequiredInstrument

---

## 📋 Sheets do WKF MT

Baseado na especificação que você forneceu:

### Sheet 1: InfoSheet (Metadata)
- **Conteúdo**: Dependências entre sheets
- **Campos**: hasDependencies, ProcessStems, Processes, Tasks, RequiredInstruments
- **Status Ingestão**: ✅ Processado automaticamente

### Sheet 2: Namespaces
- **Conteúdo**: Prefixos de ontologias
- **Campos**: hasPrefix, hasNameSpace
- **Status Ingestão**: ✅ Processado por `nameSpaceGen()`

### Sheet 3: ProcessStems
- **Conteúdo**: Process stems (abstratos)
- **Campos**:
  - hasURI (hadatac:PRSXX...)
  - a (vstoi:ProcessStem)
  - rdfs:label
  - vstoi:hasStatus
  - vstoi:hasContent
  - rdfs:comment
- **Status Ingestão**: ✅ Implementado

### Sheet 4: Processes
- **Conteúdo**: Processos específicos
- **Campos**:
  - hasURI (hadatac:PRCXX...)
  - a (vstoi:Process)
  - rdfs:label
  - vstoi:hasTopTask (referência à Task)
  - vstoi:hasStatus
  - rdfs:comment
- **Status Ingestão**: ✅ Implementado

### Sheet 5: Tasks
- **Conteúdo**: Tarefas individuais
- **Campos**:
  - hasURI (hadatac:TSKXX...)
  - a (vstoi:Task)
  - rdfs:label
  - vstoi:hasTemporalDependency (opcional)
  - vstoi:hasStatus
  - rdfs:comment
- **Status Ingestão**: ✅ Implementado

### Sheet 6: RequiredInstruments
- **Conteúdo**: Instrumentos necessários para tasks
- **Campos**:
  - hasURI (hadatac:RIXX...)
  - a (vstoi:RequiredInstrument)
  - vstoi:usesInstrument (referência a Instrument ingerido)
  - rdfs:label
- **Status Ingestão**: ✅ Implementado

---

## 🔧 Integração com IngestionWorker

### ✅ Configurado em IngestionWorker.java

**Linha 220-221**:
```java
} else if (fileName.startsWith("WKF-")) {
    chain = AnnotateWKF.exec(dataFile, templateFile, status);
```

**Funcionamento**:
1. Arquivo com nome começando com `WKF-` é detectado
2. `AnnotateWKF.exec()` é chamado
3. Catalog é carregado e validado
4. Generators são criados para cada sheet
5. GeneratorChain executa e cria triples RDF

---

## 🔍 Análise do Log Fornecido

### O Que Você Mostrou:
```json
Type: [wkf]  JSON [{
  "uri":"https://hadatac.org/ont/hadatac#/WKF1770659395537901",
  "typeUri":"http://hadatac.org/ont/hasco/WKF",
  "hascoTypeUri":"http://hadatac.org/ont/hasco/WKF",
  "label":"asdasdasdasd",
  "hasDataFileUri":"https://hadatac.org/ont/hadatac#/DFL1770659395537901",
  "hasVersion":"1",
  "comment":"1",
  "hasSIRManagerEmail":"admin@example.com"
}]
```

### O Que Isso Significa:
- ✅ **Metadata do WKF foi criado** (1 JSON)
- ❌ **Nenhum log de AnnotateWKF.exec()** apareceu
- ❌ **Sheets não foram processadas** (ProcessStems, Processes, etc.)

### Possíveis Causas:

#### Causa 1: Arquivo Excel Não Foi Enviado
O front-end pode estar criando apenas o metadata sem enviar o arquivo `.xlsx`.

**Verificação**:
```bash
# Procurar arquivos WKF no diretório de upload
Get-ChildItem "C:\hascoapi\var\uploads\" -Filter "WKF-*.xlsx"
```

#### Causa 2: Arquivo Não Tem o Prefixo Correto
O `IngestionWorker` procura por arquivos que começam com `WKF-`.

**Verificação**:
- Nome do arquivo deve ser: `WKF-something.xlsx`
- Não: `asdasdasdasd.xlsx`

#### Causa 3: Ingestão Não Foi Iniciada
Após criar o metadata, é necessário **chamar o endpoint de ingestão**.

**Endpoints Necessários**:
```
1. Criar metadata: POST /hascoapi/api/mt/register
2. Ingerir arquivo: POST /hascoapi/api/ingestion/ingest/:status/:elementType/:elementUri
```

---

## 🧪 Como Testar Corretamente

### Passo 1: Criar Arquivo WKF de Teste

Crie um arquivo Excel com nome `WKF-TEST.xlsx` contendo:

**Sheet 1: InfoSheet**
| hasDependencies | ProcessStems | Processes | Tasks | RequiredInstruments |
|-----------------|--------------|-----------|-------|---------------------|
| #Namespaces | #ProcessStems | #Processes | #Tasks | #RequiredInstruments |

**Sheet 2: Namespaces**
| hasPrefix | hasNameSpace |
|-----------|--------------|
| hadatac | https://hadatac.org/ont/hadatac# |
| vstoi | http://hadatac.org/ont/vstoi# |

**Sheet 3: ProcessStems**
| hasURI | a | rdfs:label | vstoi:hasStatus | vstoi:hasContent | rdfs:comment |
|--------|---|------------|-----------------|------------------|--------------|
| hadatac:PRS001 | vstoi:ProcessStem | Battery Testing Process | DRAFT | Test battery lifecycle | Main testing process |

**Sheet 4: Processes**
| hasURI | a | rdfs:label | vstoi:hasTopTask | vstoi:hasStatus | rdfs:comment |
|--------|---|------------|------------------|-----------------|--------------|
| hadatac:PRC001 | vstoi:Process | Full Battery Test | hadatac:TSK001 | DRAFT | Complete test workflow |

**Sheet 5: Tasks**
| hasURI | a | rdfs:label | vstoi:hasTemporalDependency | vstoi:hasStatus | rdfs:comment |
|--------|---|------------|----------------------------|-----------------|--------------|
| hadatac:TSK001 | vstoi:Task | Charge Battery | | DRAFT | Initial charging phase |
| hadatac:TSK002 | vstoi:Task | Discharge Test | hadatac:TSK001 | DRAFT | Test discharge curve |

**Sheet 6: RequiredInstruments**
| hasURI | a | vstoi:usesInstrument | rdfs:label |
|--------|---|---------------------|------------|
| hadatac:RI001 | vstoi:RequiredInstrument | hadatac:INS-CHARGER-001 | Battery Charger |

### Passo 2: Upload do Arquivo

**Via API**:
```bash
# Upload do arquivo
POST /hascoapi/api/mt/upload
Content-Type: multipart/form-data
file: WKF-TEST.xlsx
```

### Passo 3: Criar Metadata WKF

```bash
POST /hascoapi/api/mt/register
Content-Type: application/json

{
  "type": "wkf",
  "label": "Test Workflow",
  "comment": "Test WKF ingestion",
  "hasVersion": "1",
  "hasSIRManagerEmail": "admin@example.com",
  "hasDataFileUri": "<URI do DataFile retornado no passo 2>"
}
```

### Passo 4: Ingerir WKF

```bash
POST /hascoapi/api/ingestion/ingest/DRAFT/wkf/<WKF URI>
```

### Passo 5: Verificar Logs

Deve aparecer no console do servidor:
```
========== AnnotateWKF.exec() START ==========
DataFile URI: hadatac:DFL...
DataFile Filename: WKF-TEST.xlsx
→ Loading catalog...
✓ Catalog loaded successfully with 6 sheets:
  - Sheet: [InfoSheet] → URI: [...]
  - Sheet: [Namespaces] → URI: [...]
  - Sheet: [ProcessStems] → URI: [...]
  - Sheet: [Processes] → URI: [...]
  - Sheet: [Tasks] → URI: [...]
  - Sheet: [RequiredInstruments] → URI: [...]
→ Generating namespaces...
→ Generating messages...
✓ Namespaces and messages generated successfully
→ Building generator chain...
  Processing sheet: [ProcessStems]
    → Adding ProcessStem generator
  Processing sheet: [Processes]
    → Adding Process generator
  Processing sheet: [Tasks]
    → Adding Task generator
  Processing sheet: [RequiredInstruments]
    → Adding RequiredInstrument generator
✓ Generator chain built with 4 generators
✓ WKF: Generator chain validated successfully
========== AnnotateWKF.exec() END (SUCCESS) ==========
```

### Passo 6: Verificar Dados no Triple Store

```sparql
PREFIX vstoi: <http://hadatac.org/ont/vstoi#>
PREFIX hadatac: <https://hadatac.org/ont/hadatac#>

SELECT ?s ?type ?label WHERE {
  ?s a ?type .
  ?s rdfs:label ?label .
  FILTER(?type IN (vstoi:ProcessStem, vstoi:Process, vstoi:Task, vstoi:RequiredInstrument))
}
```

---

## 📊 Status de Implementação

| Componente | Status | Observação |
|------------|--------|------------|
| **WKF.java** | ✅ | Entity completa |
| **AnnotateWKF.java** | ✅ | Com logging detalhado |
| **WKFGenerator.java** | ✅ | 4 tipos suportados |
| **IngestionWorker** | ✅ | Integração completa |
| **Error Dictionary** | ✅ | 20 códigos de erro |
| **Routes** | ✅ | Endpoints criados |
| **IngestionAPI** | ✅ | Validação e ingest |
| **Teste Real** | ⏳ | Aguardando arquivo Excel |

---

## 🚨 Problema Atual

**O log que você forneceu mostra apenas a criação do metadata, sem ingestão do arquivo.**

### Possíveis Soluções:

#### Solução 1: Verificar se Arquivo Foi Salvo
```powershell
# Procurar arquivos WKF
Get-ChildItem "C:\hascoapi\var\" -Recurse -Filter "*.xlsx" | 
  Where-Object { $_.Name -like "WKF-*" } | 
  Select-Object Name, FullName, Length, LastWriteTime
```

#### Solução 2: Chamar Endpoint de Ingestão Manualmente
```bash
# Usar o URI do WKF que foi criado
POST http://localhost:9000/hascoapi/api/ingestion/ingest/DRAFT/wkf/hadatac:WKF1770659395537901
```

#### Solução 3: Verificar Nome do Arquivo
O arquivo **DEVE** começar com `WKF-`:
- ✅ Correto: `WKF-test.xlsx`, `WKF-workflow1.xlsx`
- ❌ Errado: `asdasdasdasd.xlsx`, `workflow.xlsx`

---

## 🎯 Próximos Passos

1. **Criar arquivo WKF de teste** seguindo o template acima
2. **Upload do arquivo** com nome começando com `WKF-`
3. **Criar metadata WKF** apontando para o DataFile
4. **Chamar endpoint de ingestão**
5. **Verificar logs** no console do servidor
6. **Confirmar triples** no Fuseki

---

## 📞 Se o Problema Persistir

**Me envie os logs completos** do console do servidor quando você:
1. Faz upload do arquivo
2. Cria o metadata WKF
3. Chama a ingestão

Procure especialmente por:
```
========== AnnotateWKF.exec() START ==========
```

Se essa linha **não aparecer**, significa que o arquivo não está sendo processado.

---

**Status**: ✅ **BACKEND 100% IMPLEMENTADO**  
**Próximo**: Testar com arquivo Excel real começando com `WKF-`
