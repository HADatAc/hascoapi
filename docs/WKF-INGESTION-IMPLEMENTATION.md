# WKF (Workflow) MT - Implementação de Ingestão Completa

## Resumo
Implementação completa da infraestrutura de ingestão para o novo Metadata Template **WKF (Workflow)**, seguindo o padrão dos MTs existentes (DP2, DSG, INS, KGR, SDD, STR).

## Arquivos Criados

### 1. Entidade POJO
**Arquivo**: `app/org/hascoapi/entity/pojo/WKF.java`
- Classe que representa o WKF no sistema
- Métodos: `find(String uri)`, getters/setters
- Estende `MetadataTemplate`
- Similar a `DP2.java`, `DSG.java`, etc.

### 2. Generator
**Arquivo**: `app/org/hascoapi/ingestion/WKFGenerator.java`
- Processa as linhas do Excel durante a ingestão
- Suporta 4 tipos de elementos:
  - `processstem` (ProcessStem)
  - `process` (Process)
  - `task` (Task)
  - `requiredinstrument` (RequiredInstrument)
- Adiciona metadata automática (hascoType, status, emails, etc.)

### 3. Annotator
**Arquivo**: `app/org/hascoapi/ingestion/AnnotateWKF.java`
- Orquestra a ingestão das sheets do WKF
- Valida o catálogo de sheets
- Constrói a cadeia de generators
- Sheets suportadas:
  - InfoSheet (metadata)
  - Namespaces
  - ProcessStems
  - Processes
  - Tasks
  - RequiredInstruments

### 4. API Controller
**Arquivo**: `app/org/hascoapi/console/controllers/restapi/WKFAPI.java`
- Endpoint para criar WKF via JSON
- Endpoint para listar WKFs
- Debug logging completo
- Similar a `DP2API.java`

## Arquivos Modificados

### 1. Constants.java
**Adicionado**:
```java
public static final String MT_WKF = "WKF";
public static final String PREFIX_WKF = "WKF";
```

### 2. HASCO.java (vocabulário)
**Adicionado**:
```java
public static final String WKF = "http://hadatac.org/ont/hasco/WKF";
```

### 3. MTSheet.java
**Adicionado**:
```java
METADATA_SHEETS.put(Constants.MT_WKF, Arrays.asList(
    "hasDependencies",
    "ProcessStems",
    "Processes",
    "Tasks",
    "RequiredInstruments"
));
```

### 4. Utils.java
**Adicionado** case no método `shortPrefix()`:
```java
case "wkf":
    shortPrefix = Constants.PREFIX_WKF;
    break;
```

### 5. IngestionAPI.java
**Modificações**:
- Import de `WKF`
- Adicionado `"wkf"` na validação de `elementType`
- Adicionado case para `WKF` em `ingest()`
- Adicionado case para `WKF` em `uningestMetadataTemplate()`

### 6. IngestionWorker.java
**Adicionado**:
```java
} else if (fileName.startsWith("WKF-")) {
    chain = AnnotateWKF.exec(dataFile, templateFile, status);
```

## Estrutura do MT WKF

### Sheets Obrigatórias:
1. **InfoSheet** - Metadata do arquivo
2. **Namespaces** - Prefixos e URIs

### Sheets de Dados:
1. **ProcessStems** - Templates/definições de workflows
2. **Processes** - Instâncias de workflows
3. **Tasks** - Tarefas do workflow
4. **RequiredInstruments** - Instrumentos requeridos por tasks

### Relacionamentos:
- `Process` → `vstoi:hasTopTask` → `Task`
- `Task` → `vstoi:hasSupertask` → `Task` (hierarquia)
- `Task` → `vstoi:hasSubtask` → `Task[]` (filhos)
- `Task` → `vstoi:hasRequiredInstrument` → `RequiredInstrument[]`
- `RequiredInstrument` → `vstoi:usesInstrument` → `Instrument` (já ingerido)

## Vocabulário Utilizado

### Classes (tipos):
- `vstoi:ProcessStem` - Template do workflow
- `vstoi:Process` - Instância do workflow
- `vstoi:Task` - Tarefa
- `vstoi:RequiredInstrument` - Instrumento requerido

### Propriedades comuns (todas as entidades):
- `rdfs:label`
- `rdfs:comment`
- `rdf:type`
- `hasco:hascoType`
- `hasco:hasImage`
- `hasco:hasWebDocument`
- `vstoi:hasStatus`
- `vstoi:hasLanguage`
- `vstoi:hasVersion`
- `vstoi:hasReviewNote`
- `prov:wasDerivedFrom`
- `vstoi:hasSIRManagerEmail`
- `vstoi:hasEditorEmail`

### Propriedades específicas:
#### ProcessStem:
- `vstoi:hasContent`
- `prov:wasGeneratedBy`

#### Process:
- `vstoi:hasTopTask`

#### Task:
- `vstoi:hasSupertask`
- `vstoi:hasSubtask` (multi-valor)
- `vstoi:hasTemporalDependency` (código/vocabulário)
- `vstoi:hasRequiredInstrument` (multi-valor)

#### RequiredInstrument:
- `vstoi:usesInstrument`
- `vstoi:hasRequiredComponent` (multi-valor, opcional)

## URIs Exemplo

### Formato CURIE (para Excel):
- ProcessStem: `hadatac:PST1770629000000001`
- Process: `hadatac:PC01770629000000002`
- Task: `hadatac:TSK1770629000000100`
- RequiredInstrument: `hadatac:RIN1770629000000200`

### Prefixos utilizados:
- `hadatac:` → `https://hadatac.org/ont/hadatac#/`
- `vstoi:` → `http://hadatac.org/ont/vstoi#`
- `hasco:` → `http://hadatac.org/ont/hasco/`
- `prov:` → `http://www.w3.org/ns/prov#`
- `rdfs:` → `http://www.w3.org/2000/01/rdf-schema#`
- `rdf:` → `http://www.w3.org/1999/02/22-rdf-syntax-ns#`

## Fluxo de Ingestão

### 1. Upload do arquivo
- Nome do arquivo deve começar com `WKF-` (ex: `WKF-MyWorkflow.xlsx`)
- POST `/hascoapi/api/ingest/DRAFT/wkf/{wkfUri}`

### 2. Processamento
1. `IngestionWorker` detecta prefixo `WKF-`
2. Chama `AnnotateWKF.exec()`
3. `AnnotateWKF` valida sheets no InfoSheet
4. Gera namespaces e messages
5. Cria chain de generators:
   - `WKFGenerator("processstem")` para ProcessStems
   - `WKFGenerator("process")` para Processes
   - `WKFGenerator("task")` para Tasks
   - `WKFGenerator("requiredinstrument")` para RequiredInstruments
6. Cada generator processa as linhas e gera RDF
7. RDF é inserido no triple store

### 3. Resultado
- Todas as entidades criadas no triple store
- DataFile marcado como PROCESSED
- Log disponível via `/hascoapi/api/ingestion/{dataFileUri}/log`

## Teste da Implementação

### Criar um WKF Excel simples:
1. Sheet `InfoSheet`:
   ```
   Attribute           | Value
   hasDependencies     | #Namespaces
   ```

2. Sheet `Namespaces`:
   ```
   hasPrefix | hasNameSpace
   hadatac   | https://hadatac.org/ont/hadatac#/
   vstoi     | http://hadatac.org/ont/vstoi#
   ```

3. Sheet `ProcessStems`:
   ```
   uri                        | rdfs:label      | ...
   hadatac:PST1770629000001   | My Workflow     | ...
   ```

4. Sheet `Processes`:
   ```
   uri                        | rdfs:label        | vstoi:hasTopTask
   hadatac:PC01770629000002   | Workflow Run 1    | hadatac:TSK1770629000100
   ```

5. Sheet `Tasks`:
   ```
   uri                        | rdfs:label  | vstoi:hasSupertask
   hadatac:TSK1770629000100   | Task 1      |
   ```

6. Sheet `RequiredInstruments`:
   ```
   uri                        | vstoi:usesInstrument
   hadatac:RIN1770629000200   | hadatac:INS1234567890
   ```

### Upload via API:
```bash
curl -X POST "http://localhost:9000/hascoapi/api/ingest/DRAFT/wkf/hadatac:WKF1770629000001" \
  -F "file=@WKF-MyWorkflow.xlsx"
```

### Verificar ingestão:
- Consultar SPARQL para ver as entidades criadas
- Verificar log: `GET /hascoapi/api/ingestion/{dataFileUri}/log`

## Status da Implementação

### ✅ Completo:
- [x] Entidade POJO (WKF.java)
- [x] Generator (WKFGenerator.java)
- [x] Annotator (AnnotateWKF.java)
- [x] API Controller (WKFAPI.java)
- [x] Constantes (MT_WKF, PREFIX_WKF)
- [x] Vocabulário (HASCO.WKF)
- [x] MTSheet configuração
- [x] Utils.shortPrefix()
- [x] IngestionAPI integração
- [x] IngestionWorker integração

### ⚠️ Warnings (não-bloqueantes):
- Classes marcadas como "never used" (normal até integração completa com UI)
- Alguns imports não utilizados

### 🔧 Próximos Passos (opcional):
1. Criar rota no `conf/routes` para WKFAPI (se necessário)
2. Criar gerador de Excel (WKFGen.java) - similar a DP2Gen.java
3. Criar UI no front-end para criar/gerenciar WKFs
4. Adicionar validações específicas de negócio
5. Criar testes unitários

## Compilação

✅ **Código compila sem erros**
- Apenas warnings de "unused" (esperado nesta fase)
- Pronto para testes de ingestão

---

**Data**: 2026-02-09
**Status**: ✅ Implementação completa da ingestão WKF
