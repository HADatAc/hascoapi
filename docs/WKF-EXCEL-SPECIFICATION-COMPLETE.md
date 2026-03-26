# Especificação Completa do Metadata Template WKF (Workflow)

## 📋 Índice
1. [Visão Geral](#visão-geral)
2. [Estrutura do Arquivo Excel](#estrutura-do-arquivo-excel)
3. [Sheets Obrigatórias](#sheets-obrigatórias)
4. [Definição de Cada Sheet](#definição-de-cada-sheet)
5. [Relacionamentos entre Entidades](#relacionamentos-entre-entidades)
6. [Exemplo Completo](#exemplo-completo)
7. [Validações](#validações)
8. [Como Criar um WKF do Zero](#como-criar-um-wkf-do-zero)

---

## Visão Geral

O **WKF (Workflow)** é um Metadata Template que descreve processos de trabalho estruturados, incluindo:
- **ProcessStems**: Templates/definições de processos
- **Processes**: Instâncias de processos
- **Tasks**: Tarefas que compõem os processos
- **RequiredInstruments**: Instrumentos necessários para executar as tarefas

### Propósito
O WKF permite documentar e padronizar workflows científicos, garantindo que todos os passos de um procedimento sejam registrados semanticamente.

---

## Estrutura do Arquivo Excel

### Nome do Arquivo
Formato: `WKF-{nome-descritivo}.xlsx`

Exemplos:
- `WKF-Data-Collection-Protocol.xlsx`
- `WKF-Sample-Analysis-Workflow.xlsx`
- `WKF-Field-Study-Process.xlsx`

### Sheets Obrigatórias (em ordem)

1. **InfoSheet** - Metadados do arquivo
2. **Namespaces** - Definição de prefixos e namespaces
3. **ProcessStems** - Definições/templates de processos
4. **Processes** - Instâncias de processos
5. **Tasks** - Tarefas individuais
6. **RequiredInstruments** - Instrumentos necessários

---

## Definição de Cada Sheet

### 1. InfoSheet

Define as dependências e metadados do arquivo WKF.

**Estrutura:**

| Attribute | Value |
|-----------|-------|
| hasDependencies | #Namespaces |
| ProcessStems | #ProcessStems |
| Processes | #Processes |
| Tasks | #Tasks |
| RequiredInstruments | #RequiredInstruments |
| hasVersion | 1 |

**Regras:**
- ✅ O prefixo `#` indica referência a outra sheet
- ✅ `hasVersion` deve ser um número inteiro (padrão: 1)
- ✅ Todas as sheets referenciadas devem existir no arquivo

---

### 2. Namespaces

Define os prefixos usados no arquivo para abreviar URIs.

**Estrutura:**

| hasPrefix | hasNameSpace | hasFormat | hasSource |
|-----------|--------------|-----------|-----------|
| hasco | http://hadatac.org/ont/hasco# | text/turtle | http://hadatac.org/ont/hasco# |
| vstoi | http://hadatac.org/ont/vstoi# | text/turtle | http://hadatac.org/ont/vstoi# |
| prov | http://www.w3.org/ns/prov# | text/turtle | http://www.w3.org/ns/prov# |
| rdfs | http://www.w3.org/2000/01/rdf-schema# | text/turtle | http://www.w3.org/2000/01/rdf-schema# |
| rdf | http://www.w3.org/1999/02/22-rdf-syntax-ns# | text/turtle | http://www.w3.org/1999/02/22-rdf-syntax-ns# |
| owl | http://www.w3.org/2002/07/owl# | text/turtle | http://www.w3.org/2002/07/owl# |
| xsd | http://www.w3.org/2001/XMLSchema# | text/turtle | http://www.w3.org/2001/XMLSchema# |

**Namespaces Obrigatórios:**
- ✅ `hasco` - Ontologia HASCO
- ✅ `vstoi` - Vocabulário VSTOI (status, version, etc.)
- ✅ `prov` - Proveniência W3C
- ✅ `rdfs` - RDF Schema
- ✅ `rdf` - RDF
- ✅ `owl` - OWL Ontology
- ✅ `xsd` - XML Schema Datatypes

**Adicione conforme necessário:**
- Custom ontologies
- Domain-specific vocabularies

---

### 3. ProcessStems

Define templates/classes de processos reutilizáveis.

**Estrutura (16 colunas):**

| Coluna | Propriedade | Tipo | Obrigatório | Descrição | Exemplo |
|--------|-------------|------|-------------|-----------|---------|
| 1 | hasURI | URI | ✅ SIM | URI único do ProcessStem | `#PST001` ou `http://example.org/process-stems/PST001` |
| 2 | rdf:type | URI | ✅ SIM | Tipo RDF | `vstoi:ProcessStem` |
| 3 | hasco:hascoType | URI | ✅ SIM | Tipo HASCO | `vstoi:ProcessStem` |
| 4 | rdfs:label | String | ✅ SIM | Nome legível | `Weather Monitoring Process` |
| 5 | rdfs:comment | String | ❌ NÃO | Descrição detalhada | `Standard procedure for monitoring weather conditions` |
| 6 | vstoi:hasStatus | URI | ✅ SIM | Status | `vstoi:Draft` ou `vstoi:Current` |
| 7 | vstoi:hasContent | String | ❌ NÃO | Conteúdo descritivo | Texto livre |
| 8 | vstoi:hasLanguage | String | ❌ NÃO | Idioma | `en`, `pt`, `es` |
| 9 | vstoi:hasVersion | String | ❌ NÃO | Versão | `1.0`, `2.1` |
| 10 | prov:wasDerivedFrom | URI | ❌ NÃO | Derivado de | URI de ProcessStem anterior |
| 11 | prov:wasGeneratedBy | URI | ❌ NÃO | Gerado por | URI de agente/sistema |
| 12 | vstoi:hasReviewNote | String | ❌ NÃO | Notas de revisão | Comentários do revisor |
| 13 | vstoi:hasSIRManagerEmail | Email | ✅ SIM | Email do gestor | `admin@example.com` |
| 14 | vstoi:hasEditorEmail | Email | ❌ NÃO | Email do editor | `editor@example.com` |
| 15 | hasco:hasImage | URI | ❌ NÃO | Imagem ilustrativa | URL da imagem |
| 16 | hasco:hasWebDocument | URI | ❌ NÃO | Documentação web | URL do documento |

**Exemplo de Linha:**

```
#PST001 | vstoi:ProcessStem | vstoi:ProcessStem | Weather Monitoring Process | Standard procedure for monitoring weather conditions in field studies | vstoi:Draft | | en | 1.0 | | | | admin@example.com | | | https://example.org/docs/weather-monitoring
```

**Regras:**
- ✅ Cada ProcessStem deve ter URI única
- ✅ `vstoi:hasStatus` valores aceitos: `vstoi:Draft`, `vstoi:Current`, `vstoi:Retired`
- ✅ URIs podem ser absolutas (`http://...`) ou relativas (`#PST001`)
- ✅ Se usar URI relativa, o sistema gerará: `{namespace}/PST001`

---

### 4. Processes

Define instâncias concretas de processos baseadas em ProcessStems.

**Estrutura (15 colunas):**

| Coluna | Propriedade | Tipo | Obrigatório | Descrição | Exemplo |
|--------|-------------|------|-------------|-----------|---------|
| 1 | hasURI | URI | ✅ SIM | URI único do Process | `#PROC001` |
| 2 | rdf:type | URI | ✅ SIM | Tipo RDF | `vstoi:Process` |
| 3 | hasco:hascoType | URI | ✅ SIM | Tipo HASCO | `vstoi:Process` |
| 4 | rdfs:label | String | ✅ SIM | Nome do processo | `Daily Weather Monitoring - Site A` |
| 5 | rdfs:comment | String | ❌ NÃO | Descrição | `Daily weather monitoring at research site A` |
| 6 | vstoi:hasStatus | URI | ✅ SIM | Status | `vstoi:Draft` |
| 7 | vstoi:hasLanguage | String | ❌ NÃO | Idioma | `en` |
| 8 | vstoi:hasVersion | String | ❌ NÃO | Versão | `1.0` |
| 9 | prov:wasDerivedFrom | URI | ❌ NÃO | Derivado de | URI ProcessStem ou Process |
| 10 | vstoi:hasReviewNote | String | ❌ NÃO | Notas de revisão | |
| 11 | vstoi:hasSIRManagerEmail | Email | ✅ SIM | Email do gestor | `admin@example.com` |
| 12 | vstoi:hasEditorEmail | Email | ❌ NÃO | Email do editor | |
| 13 | vstoi:hasTopTask | URI | ✅ SIM | Tarefa principal | `#TASK001` |
| 14 | hasco:hasImage | URI | ❌ NÃO | Imagem | |
| 15 | hasco:hasWebDocument | URI | ❌ NÃO | Documentação | |

**Relacionamento Importante:**
- ✅ **`vstoi:belongsTo`**: Embora não apareça como coluna no Excel, o Process está relacionado a um ProcessStem através da propriedade `vstoi:belongsTo` (definida internamente durante a ingestão)
- ✅ **`vstoi:hasTopTask`**: Aponta para a Task principal/inicial do processo

**Exemplo:**

```
#PROC001 | vstoi:Process | vstoi:Process | Daily Weather Monitoring - Site A | Daily weather monitoring at research site A | vstoi:Draft | en | 1.0 | #PST001 | | admin@example.com | | #TASK001 | |
```

---

### 5. Tasks

Define tarefas individuais que compõem os processos.

**Estrutura (18 colunas):**

| Coluna | Propriedade | Tipo | Obrigatório | Descrição | Exemplo |
|--------|-------------|------|-------------|-----------|---------|
| 1 | hasURI | URI | ✅ SIM | URI única da Task | `#TASK001` |
| 2 | rdf:type | URI | ✅ SIM | Tipo RDF | `vstoi:Task` |
| 3 | hasco:hascoType | URI | ✅ SIM | Tipo HASCO | `vstoi:Task` |
| 4 | rdfs:label | String | ✅ SIM | Nome da tarefa | `Collect Temperature Data` |
| 5 | rdfs:comment | String | ❌ NÃO | Descrição | `Use thermometer to record temperature every hour` |
| 6 | vstoi:hasStatus | URI | ✅ SIM | Status | `vstoi:Draft` |
| 7 | vstoi:hasLanguage | String | ❌ NÃO | Idioma | `en` |
| 8 | vstoi:hasVersion | String | ❌ NÃO | Versão | `1.0` |
| 9 | prov:wasDerivedFrom | URI | ❌ NÃO | Derivado de | URI de Task anterior |
| 10 | vstoi:hasReviewNote | String | ❌ NÃO | Notas de revisão | |
| 11 | vstoi:hasSIRManagerEmail | Email | ✅ SIM | Email do gestor | `admin@example.com` |
| 12 | vstoi:hasEditorEmail | Email | ❌ NÃO | Email do editor | |
| 13 | vstoi:hasSupertask | URI | ❌ NÃO | Tarefa pai (hierarquia) | `#TASK_PARENT` |
| 14 | vstoi:hasSubtask | URI(s) | ❌ NÃO | Subtarefas (sep. por `\|`) | `#TASK002 \| #TASK003` |
| 15 | vstoi:hasTemporalDependency | String | ❌ NÃO | Dependência temporal | `after #TASK001` |
| 16 | vstoi:hasRequiredInstrument | URI(s) | ❌ NÃO | Instrumentos (sep. por `\|`) | `#INS001 \| #INS002` |
| 17 | hasco:hasImage | URI | ❌ NÃO | Imagem | |
| 18 | hasco:hasWebDocument | URI | ❌ NÃO | Documentação | |

**Relacionamentos Importantes:**
- ✅ **`vstoi:belongsTo`**: A Task pertence a um ProcessStem (definido internamente)
- ✅ **`vstoi:hasSupertask`**: Define hierarquia (tarefa pai)
- ✅ **`vstoi:hasSubtask`**: Lista de subtarefas (separadas por ` | `)
- ✅ **`vstoi:hasRequiredInstrument`**: Instrumentos necessários (separados por ` | `)

**Exemplo:**

```
#TASK001 | vstoi:Task | vstoi:Task | Collect Temperature Data | Use thermometer to record temperature every hour | vstoi:Draft | en | 1.0 | | | admin@example.com | | | #TASK002 | #TASK003 | after initialization | #INS_THERMOMETER | |
```

---

### 6. RequiredInstruments

Define os instrumentos necessários para executar as tarefas.

**Estrutura (5 colunas):**

| Coluna | Propriedade | Tipo | Obrigatório | Descrição | Exemplo |
|--------|-------------|------|-------------|-----------|---------|
| 1 | hasURI | URI | ✅ SIM | URI do RequiredInstrument | `#REQINS001` |
| 2 | vstoi:usesInstrument | URI | ✅ SIM | URI do Instrument | `#INS_THERMOMETER` |
| 3 | vstoi:isRelatedToTask | URI | ❌ NÃO | URI da Task relacionada | `#TASK001` |
| 4 | rdfs:comment | String | ❌ NÃO | Comentários | `Digital thermometer with 0.1°C precision` |
| 5 | vstoi:hasInstrumentConfig | String | ❌ NÃO | Configuração do instrumento | JSON ou texto com configurações |

**Exemplo:**

```
#REQINS001 | #INS_THERMOMETER | #TASK001 | Digital thermometer with 0.1°C precision | {"range": "-40 to 80°C", "precision": "0.1°C"}
```

**Notas:**
- Os Instruments referenciados (`#INS_THERMOMETER`) devem existir em um INS (Instrument Metadata Template) separado
- Esta sheet apenas relaciona quais instrumentos são necessários para quais tarefas

---

## Relacionamentos entre Entidades

### Diagrama de Relacionamentos

```
ProcessStem (#PST001)
    │
    ├─── belongsTo ───┐
    │                 │
    │              Process (#PROC001)
    │                 │
    │                 └─── hasTopTask ──→ Task (#TASK001)
    │
    ├─── belongsTo ───┐
    │                 │
    │              Task (#TASK001)
    │                 ├─── hasSubtask ──→ Task (#TASK002)
    │                 ├─── hasSubtask ──→ Task (#TASK003)
    │                 └─── hasRequiredInstrument ──→ RequiredInstrument (#REQINS001)
    │                                                    │
    │                                                    └─── usesInstrument ──→ Instrument (#INS_THERMOMETER)
    │
    └─── belongsTo ───┐
                      │
                   Task (#TASK002)
                      └─── hasSupertask ──→ Task (#TASK001)
```

### Regras de Relacionamento

1. **ProcessStem → Process**:
   - Um ProcessStem pode ter ZERO ou MUITOS Processes
   - Cada Process **deve** `belongsTo` um ProcessStem (implícito)

2. **ProcessStem → Task**:
   - Um ProcessStem pode ter ZERO ou MUITAS Tasks
   - Cada Task **deve** `belongsTo` um ProcessStem (implícito)

3. **Process → Task (via hasTopTask)**:
   - Cada Process **deve** ter UMA Task principal (`vstoi:hasTopTask`)
   - Esta é a Task inicial do workflow

4. **Task → Task (hierarquia)**:
   - Task pode ter `hasSupertask` (tarefa pai)
   - Task pode ter múltiplas `hasSubtask` (subtarefas)
   - Separar múltiplas com ` | ` (pipe com espaços)

5. **Task → Instrument**:
   - Task pode referenciar múltiplos `hasRequiredInstrument`
   - Separar com ` | `
   - Cada RequiredInstrument aponta para um Instrument real

---

## Exemplo Completo

### Cenário: "Weather Monitoring Workflow"

Um workflow para monitoramento meteorológico com múltiplas tarefas.

#### InfoSheet
```
Attribute                | Value
------------------------|------------------
hasDependencies         | #Namespaces
ProcessStems            | #ProcessStems
Processes               | #Processes
Tasks                   | #Tasks
RequiredInstruments     | #RequiredInstruments
hasVersion              | 1
```

#### Namespaces
```
hasPrefix | hasNameSpace                              | hasFormat    | hasSource
----------|-------------------------------------------|--------------|-------------------------------------------
hasco     | http://hadatac.org/ont/hasco#            | text/turtle  | http://hadatac.org/ont/hasco#
vstoi     | http://hadatac.org/ont/vstoi#            | text/turtle  | http://hadatac.org/ont/vstoi#
prov      | http://www.w3.org/ns/prov#               | text/turtle  | http://www.w3.org/ns/prov#
rdfs      | http://www.w3.org/2000/01/rdf-schema#    | text/turtle  | http://www.w3.org/2000/01/rdf-schema#
rdf       | http://www.w3.org/1999/02/22-rdf-syntax-ns# | text/turtle | http://www.w3.org/1999/02/22-rdf-syntax-ns#
owl       | http://www.w3.org/2002/07/owl#           | text/turtle  | http://www.w3.org/2002/07/owl#
xsd       | http://www.w3.org/2001/XMLSchema#        | text/turtle  | http://www.w3.org/2001/XMLSchema#
```

#### ProcessStems
```
hasURI   | rdf:type          | hasco:hascoType   | rdfs:label                    | rdfs:comment                                      | vstoi:hasStatus | vstoi:hasContent | vstoi:hasLanguage | vstoi:hasVersion | prov:wasDerivedFrom | prov:wasGeneratedBy | vstoi:hasReviewNote | vstoi:hasSIRManagerEmail | vstoi:hasEditorEmail | hasco:hasImage | hasco:hasWebDocument
---------|-------------------|-------------------|-------------------------------|--------------------------------------------------|-----------------|------------------|-------------------|------------------|--------------------|--------------------|--------------------|-----------------------|---------------------|---------------|---------------------
#PST001  | vstoi:ProcessStem | vstoi:ProcessStem | Weather Monitoring Process    | Standard procedure for monitoring weather data    | vstoi:Draft     |                  | en                | 1.0              |                    |                    |                    | admin@example.com        |                     |               | https://docs.example.org/weather
```

#### Processes
```
hasURI    | rdf:type       | hasco:hascoType | rdfs:label                          | rdfs:comment                              | vstoi:hasStatus | vstoi:hasLanguage | vstoi:hasVersion | prov:wasDerivedFrom | vstoi:hasReviewNote | vstoi:hasSIRManagerEmail | vstoi:hasEditorEmail | vstoi:hasTopTask | hasco:hasImage | hasco:hasWebDocument
----------|----------------|-----------------|-------------------------------------|------------------------------------------|-----------------|-------------------|------------------|--------------------|--------------------|-------------------------|---------------------|-----------------|---------------|---------------------
#PROC001  | vstoi:Process  | vstoi:Process   | Daily Weather Monitoring - Site A   | Daily weather monitoring at site A        | vstoi:Draft     | en                | 1.0              | #PST001            |                    | admin@example.com        |                     | #TASK001        |               |
```

#### Tasks
```
hasURI    | rdf:type     | hasco:hascoType | rdfs:label                  | rdfs:comment                                    | vstoi:hasStatus | vstoi:hasLanguage | vstoi:hasVersion | prov:wasDerivedFrom | vstoi:hasReviewNote | vstoi:hasSIRManagerEmail | vstoi:hasEditorEmail | vstoi:hasSupertask | vstoi:hasSubtask        | vstoi:hasTemporalDependency | vstoi:hasRequiredInstrument | hasco:hasImage | hasco:hasWebDocument
----------|--------------|-----------------|-----------------------------|-------------------------------------------------|-----------------|-------------------|------------------|--------------------|--------------------|-------------------------|---------------------|-------------------|------------------------|----------------------------|----------------------------|---------------|---------------------
#TASK001  | vstoi:Task   | vstoi:Task      | Setup Equipment             | Prepare and calibrate monitoring instruments     | vstoi:Draft     | en                | 1.0              |                    |                    | admin@example.com        |                     |                   | #TASK002 \| #TASK003   |                            | #INS_THERM \| #INS_HYGRO   |               |
#TASK002  | vstoi:Task   | vstoi:Task      | Collect Temperature Data    | Record temperature readings every hour           | vstoi:Draft     | en                | 1.0              |                    |                    | admin@example.com        |                     | #TASK001          |                        | after #TASK001             | #INS_THERM                 |               |
#TASK003  | vstoi:Task   | vstoi:Task      | Collect Humidity Data       | Record humidity readings every hour              | vstoi:Draft     | en                | 1.0              |                    |                    | admin@example.com        |                     | #TASK001          |                        | after #TASK001             | #INS_HYGRO                 |               |
#TASK004  | vstoi:Task   | vstoi:Task      | Data Quality Check          | Verify and validate collected data               | vstoi:Draft     | en                | 1.0              |                    |                    | admin@example.com        |                     |                   |                        | after #TASK002 and #TASK003|                            |               |
```

#### RequiredInstruments
```
hasURI      | vstoi:usesInstrument | vstoi:isRelatedToTask | rdfs:comment                          | vstoi:hasInstrumentConfig
------------|---------------------|----------------------|--------------------------------------|-----------------------------
#REQINS001  | #INS_THERM          | #TASK001             | Digital thermometer, 0.1°C precision | {"range":"-40 to 80°C"}
#REQINS002  | #INS_HYGRO          | #TASK001             | Digital hygrometer, 1% precision     | {"range":"0 to 100%"}
```

---

## Validações

### Validações Automáticas Durante Ingestão

1. **InfoSheet**:
   - ✅ Todas as sheets referenciadas existem no arquivo
   - ✅ `hasVersion` é um número válido

2. **Namespaces**:
   - ✅ Namespaces obrigatórios estão presentes (hasco, vstoi, prov, rdfs, rdf, owl, xsd)
   - ✅ Cada prefixo é único
   - ✅ URIs são válidas

3. **ProcessStems**:
   - ✅ URIs são únicas
   - ✅ Campos obrigatórios preenchidos: `hasURI`, `rdf:type`, `hasco:hascoType`, `rdfs:label`, `vstoi:hasStatus`, `vstoi:hasSIRManagerEmail`
   - ✅ `vstoi:hasStatus` tem valor válido (Draft/Current/Retired)
   - ✅ Email tem formato válido

4. **Processes**:
   - ✅ URIs são únicas
   - ✅ Campos obrigatórios preenchidos
   - ✅ `vstoi:hasTopTask` aponta para uma Task existente
   - ✅ Process tem relação com ProcessStem (via `belongsTo`)

5. **Tasks**:
   - ✅ URIs são únicas
   - ✅ Campos obrigatórios preenchidos
   - ✅ `vstoi:hasSupertask` (se presente) aponta para Task existente
   - ✅ `vstoi:hasSubtask` (se presente) aponta para Tasks existentes
   - ✅ Não há ciclos de dependência (Task não pode ser supertask de si mesma)
   - ✅ Task tem relação com ProcessStem

6. **RequiredInstruments**:
   - ✅ URIs são únicas
   - ✅ `vstoi:usesInstrument` é obrigatório
   - ✅ Instrument referenciado existe (em INS template separado)

### Erros Comuns

| Erro | Causa | Solução |
|------|-------|---------|
| `WKF_00001` | Sheet obrigatória faltando | Adicionar todas as 6 sheets |
| `WKF_00002` | ProcessStem sem URI | Preencher coluna `hasURI` |
| `WKF_00003` | InfoSheet com sheets extras/faltando | Verificar lista de dependências |
| `WKF_00004` | Sheet `{name}` faltando | Criar a sheet faltante |
| `WKF_00010` | Process sem campo obrigatório | Preencher todos campos obrigatórios |
| `WKF_00011` | Task sem campo obrigatório | Preencher todos campos obrigatórios |
| `WKF_00012` | RequiredInstrument sem `usesInstrument` | Adicionar referência ao instrumento |
| `WKF_00013` | Process referencia Task inexistente | Verificar URI em `vstoi:hasTopTask` |

---

## Como Criar um WKF do Zero

### Passo 1: Criar Arquivo Excel

1. Abra Excel ou LibreOffice Calc
2. Salve como: `WKF-{seu-nome-descritivo}.xlsx`
3. Formato: **Excel Workbook (.xlsx)**

### Passo 2: Criar Sheet "InfoSheet"

1. Criar nova sheet chamada exatamente `InfoSheet`
2. Adicionar header na linha 1:
   - Coluna A: `Attribute`
   - Coluna B: `Value`
3. Adicionar linhas de dados:
   ```
   Row 2: hasDependencies | #Namespaces
   Row 3: ProcessStems | #ProcessStems
   Row 4: Processes | #Processes
   Row 5: Tasks | #Tasks
   Row 6: RequiredInstruments | #RequiredInstruments
   Row 7: hasVersion | 1
   ```

### Passo 3: Criar Sheet "Namespaces"

1. Criar sheet `Namespaces`
2. Copiar header:
   ```
   hasPrefix | hasNameSpace | hasFormat | hasSource
   ```
3. Copiar namespaces obrigatórios (mínimo):
   ```
   hasco | http://hadatac.org/ont/hasco# | text/turtle | http://hadatac.org/ont/hasco#
   vstoi | http://hadatac.org/ont/vstoi# | text/turtle | http://hadatac.org/ont/vstoi#
   prov  | http://www.w3.org/ns/prov# | text/turtle | http://www.w3.org/ns/prov#
   rdfs  | http://www.w3.org/2000/01/rdf-schema# | text/turtle | http://www.w3.org/2000/01/rdf-schema#
   rdf   | http://www.w3.org/1999/02/22-rdf-syntax-ns# | text/turtle | http://www.w3.org/1999/02/22-rdf-syntax-ns#
   owl   | http://www.w3.org/2002/07/owl# | text/turtle | http://www.w3.org/2002/07/owl#
   xsd   | http://www.w3.org/2001/XMLSchema# | text/turtle | http://www.w3.org/2001/XMLSchema#
   ```

### Passo 4: Criar Sheet "ProcessStems"

1. Criar sheet `ProcessStems`
2. Copiar header (16 colunas):
   ```
   hasURI | rdf:type | hasco:hascoType | rdfs:label | rdfs:comment | vstoi:hasStatus | vstoi:hasContent | vstoi:hasLanguage | vstoi:hasVersion | prov:wasDerivedFrom | prov:wasGeneratedBy | vstoi:hasReviewNote | vstoi:hasSIRManagerEmail | vstoi:hasEditorEmail | hasco:hasImage | hasco:hasWebDocument
   ```
3. Adicionar seus ProcessStems:
   ```
   #PST001 | vstoi:ProcessStem | vstoi:ProcessStem | Meu Processo | Descrição do processo | vstoi:Draft | | en | 1.0 | | | | admin@example.com | | |
   ```

### Passo 5: Criar Sheet "Processes"

1. Criar sheet `Processes`
2. Copiar header (15 colunas):
   ```
   hasURI | rdf:type | hasco:hascoType | rdfs:label | rdfs:comment | vstoi:hasStatus | vstoi:hasLanguage | vstoi:hasVersion | prov:wasDerivedFrom | vstoi:hasReviewNote | vstoi:hasSIRManagerEmail | vstoi:hasEditorEmail | vstoi:hasTopTask | hasco:hasImage | hasco:hasWebDocument
   ```
3. Adicionar Process vinculado ao ProcessStem:
   ```
   #PROC001 | vstoi:Process | vstoi:Process | Minha Instância de Processo | Descrição | vstoi:Draft | en | 1.0 | #PST001 | | admin@example.com | | #TASK001 | |
   ```

### Passo 6: Criar Sheet "Tasks"

1. Criar sheet `Tasks`
2. Copiar header (18 colunas):
   ```
   hasURI | rdf:type | hasco:hascoType | rdfs:label | rdfs:comment | vstoi:hasStatus | vstoi:hasLanguage | vstoi:hasVersion | prov:wasDerivedFrom | vstoi:hasReviewNote | vstoi:hasSIRManagerEmail | vstoi:hasEditorEmail | vstoi:hasSupertask | vstoi:hasSubtask | vstoi:hasTemporalDependency | vstoi:hasRequiredInstrument | hasco:hasImage | hasco:hasWebDocument
   ```
3. Adicionar Tasks:
   ```
   #TASK001 | vstoi:Task | vstoi:Task | Minha Tarefa 1 | Descrição | vstoi:Draft | en | 1.0 | | | admin@example.com | | | #TASK002 | | | |
   #TASK002 | vstoi:Task | vstoi:Task | Minha Tarefa 2 | Descrição | vstoi:Draft | en | 1.0 | | | admin@example.com | | #TASK001 | | after #TASK001 | | |
   ```

### Passo 7: Criar Sheet "RequiredInstruments"

1. Criar sheet `RequiredInstruments`
2. Copiar header (5 colunas):
   ```
   hasURI | vstoi:usesInstrument | vstoi:isRelatedToTask | rdfs:comment | vstoi:hasInstrumentConfig
   ```
3. Adicionar instrumentos necessários (se houver):
   ```
   #REQINS001 | #INS_MEU_INSTRUMENTO | #TASK001 | Descrição do instrumento | configurações
   ```
4. **NOTA**: Se não houver instrumentos, deixar a sheet com apenas o header

### Passo 8: Validar

Verificar:
- ✅ Todas as 6 sheets existem e com nomes corretos
- ✅ Headers estão corretos em cada sheet
- ✅ Campos obrigatórios preenchidos
- ✅ URIs únicas (sem duplicatas)
- ✅ Referências válidas (`#TASK001` existe antes de ser referenciado)
- ✅ Email no formato válido
- ✅ Status usa valores válidos (`vstoi:Draft`, `vstoi:Current`, ou `vstoi:Retired`)

### Passo 9: Upload

1. Fazer upload do arquivo WKF no sistema HADatAc
2. Sistema irá validar automaticamente
3. Se houver erros, corrigir e fazer novo upload

---

## Dicas e Boas Práticas

### URIs

✅ **BOM**:
```
#PST001
#PROC_WEATHER_MONITORING_2024
#TASK_COLLECT_TEMP
```

❌ **RUIM**:
```
PST001 (faltando #)
#PST 001 (espaço no meio)
#PST-001-@#$ (caracteres especiais problemáticos)
```

### Labels

✅ **BOM** (descritivo, claro):
```
Weather Monitoring Process - Field Site A
Collect Temperature Data Using Digital Thermometer
Data Quality Verification and Validation
```

❌ **RUIM** (vago, curto demais):
```
Process 1
Task
Data
```

### Hierarquia de Tasks

Para criar hierarquia clara:

```
TASK001 (pai)
  ├─ TASK002 (filho, hasSupertask = #TASK001)
  └─ TASK003 (filho, hasSupertask = #TASK001)
      └─ TASK004 (neto, hasSupertask = #TASK003)
```

Excel:
```
#TASK001 | ... | | #TASK002 | #TASK003 | ...
#TASK002 | ... | #TASK001 | | ...
#TASK003 | ... | #TASK001 | #TASK004 | ...
#TASK004 | ... | #TASK003 | | ...
```

### Múltiplos Valores

Quando uma coluna aceita múltiplos valores (como `hasSubtask` ou `hasRequiredInstrument`), separar com ` | ` (pipe com espaços):

```
#TASK001 | #TASK002 | #TASK003
#INS_THERM | #INS_HYGRO | #INS_BARO
```

---

## Diferenças vs. Versões Anteriores

### Mudanças Recentes (2026-03)

| Aspecto | Antes | Agora |
|---------|-------|-------|
| Geração por ProcessStem | ❌ Não suportado | ✅ Totalmente suportado |
| Busca de Processes/Tasks relacionados | ❌ Não buscava | ✅ Busca via `vstoi:belongsTo` |
| Download de arquivos gerados | ❌ Apenas `resources/{DFL}/` | ✅ Fallback para root `var/` |
| Retorno do método `save()` | `"SUCCESS"` | `filename` (ex: `"WKF-teste.xlsx"`) |
| Estrutura Excel | Mesma | **SEM MUDANÇAS NA ESTRUTURA** |

**IMPORTANTE**: A estrutura do Excel WKF **NÃO MUDOU**. As alterações foram apenas no backend para melhorar a geração e download.

---

## Ferramentas e Recursos

### Template Vazio

Para facilitar, você pode criar um template base com todas as sheets e headers pré-configurados.

### Validação

O sistema HADatAc valida automaticamente durante o upload e fornece mensagens de erro específicas.

### Documentação Adicional

- Ontologia VSTOI: http://hadatac.org/ont/vstoi#
- Ontologia HASCO: http://hadatac.org/ont/hasco#
- PROV-O: https://www.w3.org/TR/prov-o/

---

## Suporte

Em caso de dúvidas ou erros durante a criação/ingestão do WKF:

1. Verificar logs do sistema para mensagens de erro específicas
2. Consultar a seção de Validações deste documento
3. Verificar se todas as sheets obrigatórias existem e estão nomeadas corretamente
4. Confirmar que todos os campos obrigatórios estão preenchidos
5. Validar que todas as referências (URIs) apontam para entidades existentes

---

**Versão do Documento**: 1.0  
**Data**: 2026-03-26  
**Status**: Completo e Atualizado

