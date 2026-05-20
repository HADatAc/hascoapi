# Roundtrip Test: DSG + DA Integration

## 📋 Resumo das Alterações

Este documento descreve as alterações feitas no **HascoRoundtripTest** para integrar a ingestão de **Data Acquisition (DA) templates** com o **Design Study Guide (DSG)**.

---

## ✅ O Que Foi Feito

### 1. **Remoção do INS do Teste**
- ❌ Removido `MTType.INS` do enum
- ❌ Removida toda lógica de ingestão, regeneração e reset de INS
- ✅ Foco agora está no **DSG** que foi reativado

### 2. **Reativação do DSG**
- ✅ DSG foi reativado em todos os testes parametrizados:
  - `step1_allMTs_ingest`
  - `step2_allMTs_regenerate_and_compare`
  - `step3_allMTs_reset_and_deterministic_reingest`

### 3. **Integração DSG + DA**
- ✅ Adicionado método `ingestDAsForDSG(String studyUri)` que ingere automaticamente os DAs após a ingestão do DSG
- ✅ DAs são ingeridos na **ordem de dependência**:
  1. `DA-SOC-CODEBOOK.csv` (base)
  2. `DA-SOC-RESPONSE-OPTION.csv` (depende de Codebook)
  3. `DA-SOC-COMPONENT-STEM.csv` (base)
  4. `DA-SOC-COMPONENT.csv` (depende de ComponentStem e Codebook)
  5. `DA-SOC-INSTRUMENT-PMSR.csv` (depende de Component)
  6. `DA-SOC-SLOT-ELEMENT.csv` (depende de Instrument)

### 4. **Validação Aprimorada do VD (Variable Design)**
- ✅ Adicionado log de contagem de linhas no sheet VD após regeneração
- ✅ Validação que o VD tem pelo menos o header row

---

## 🔄 Fluxo do Teste Completo

### **Step 1: Ingestion**
```
1. Ingerir DSG-STD-test.xlsx
2. Validar que o Study foi criado
3. Ingerir DAs relacionados (6 arquivos CSV)
4. Dump de TTL para cada DA
5. Validação via SPARQL
```

### **Step 2: Regeneration & Comparison**
```
1. Regenerar DSG a partir do triplestore
2. Validar estrutura do workbook regenerado
3. Verificar que VD sheet tem conteúdo
4. Re-ingerir o DSG regenerado
5. Comparar grafos original vs regenerado
```

### **Step 3: Reset & Deterministic Re-ingestion**
```
1. Deletar study do triplestore (best-effort)
2. Re-ingerir DSG original
3. Re-ingerir DAs
4. Regenerar DSG novamente
5. Validar que é superset do original
```

---

## 📂 Arquivos de Teste

### DSG
- **Localização**: `test/resources/dsg/DSG-STD-test.xlsx`
- **Tipo**: Design Study Guide (workbook Excel)
- **Conteúdo**: STD, SSD, SOC sheets, VD sheet

### DAs
- **Localização**: `test/resources/da/`
- **Arquivos**:
  ```
  DA-SOC-CODEBOOK.csv
  DA-SOC-RESPONSE-OPTION.csv
  DA-SOC-COMPONENT-STEM.csv
  DA-SOC-COMPONENT.csv
  DA-SOC-INSTRUMENT-PMSR.csv
  DA-SOC-SLOT-ELEMENT.csv
  ```
- **Formato**: CSV com colunas metadata VSTOI

---

## 🧪 Executando os Testes

### Executar todos os testes do roundtrip:
```bash
sbt "testOnly org.hascoapi.tests.HascoRoundtripTest"
```

### Executar apenas Step 1 (Ingestion):
```bash
sbt "testOnly org.hascoapi.tests.HascoRoundtripTest.step1_allMTs_ingest"
```

### Executar apenas DSG:
```bash
sbt "testOnly org.hascoapi.tests.HascoRoundtripTest.step1_allMTs_ingest -- -z DSG"
```

---

## 🔍 Logs e Debugging

Cada ingestão de DA gera:
1. **Console logs**: `[DSG+DA] Ingesting: <filename>`
2. **TTL dump**: `test/resources/generated/DA_<name>.ttl`
3. **Triple count**: Número de triplas no named graph
4. **Preview**: Primeiras 25 triplas do grafo

### Exemplo de log esperado:
```
[DSG+DA] Ingesting related Data Acquisition templates...
[DSG+DA] Ingesting: DA-SOC-CODEBOOK.csv
[TTL] DA_DA-SOC-CODEBOOK: namedGraphUri=http://example.org/DF-DA-SOC-CODEBOOK.csv tripleCount=120
[DSG+DA] Successfully ingested: DA-SOC-CODEBOOK.csv (status: DRAFT)
[DSG+DA] Ingesting: DA-SOC-RESPONSE-OPTION.csv
...
[DSG+DA] Completed ingestion of all DA templates for study: http://hadatac.org/ont/arrowhead/STD-...
```

---

## ⚠️ Pontos de Atenção

### 1. **Ordem de Ingestão é Crítica**
Os DAs têm dependências entre si:
- `Component` precisa de `ComponentStem` e `Codebook`
- `Instrument` precisa de `Component`
- `SlotElement` precisa de `Instrument`

**Nunca altere a ordem** sem verificar as dependências!

### 2. **Prefixos nos CSVs**
Os DAs devem usar prefixos corretos:
- ✅ `pmsr:CSTEM-CSM123...` (ComponentStem)
- ✅ `pmsr:CB-CBK123...` (Codebook)
- ❌ `pmsr:/CSM123...` (ERRADO - barra antes do ID)

### 3. **Status dos DAs**
Todos os DAs usam `VSTOI.DRAFT` como status padrão.

### 4. **Named Graphs**
Cada DA cria seu próprio named graph:
- Formato: `http://example.org/DF-<filename>`
- Exemplo: `http://example.org/DF-DA-SOC-CODEBOOK.csv`

---

## 📊 Validações Implementadas

### No Step 1:
- ✅ Study URI foi criado
- ✅ Study tem pelo menos o número esperado de SOCs
- ✅ Study tem pelo menos o número esperado de StudyObjects
- ✅ Todos os DAs foram ingeridos sem exceção

### No Step 2:
- ✅ DSG regenerado tem todas as sheets obrigatórias
- ✅ VD sheet tem conteúdo (VirtualColumns foram gerados)
- ✅ Workbook regenerado é superset do original

### No Step 3:
- ✅ Study pode ser deletado e reingerido
- ✅ Regeneração após reset é determinística
- ✅ Grafos original e regenerado são comparáveis

---

## 🚀 Melhorias Futuras

1. **Validação de Enriquecimento**
   - Verificar que Components têm ComponentStems associados
   - Verificar que Instruments têm ContainerSlots
   - Validar relações entre entidades via SPARQL

2. **Testes de Integridade Referencial**
   - Verificar que todos os URIs referenciados existem
   - Validar que prefixos estão corretos

3. **Geração de Relatório**
   - Summary de quantas entidades de cada tipo foram criadas
   - Visualização de dependências entre DAs

---

## 📝 Referências

- **DSG Specification**: `docs/DSGVD-IMPLEMENTATION-COMPLETE.md`
- **VSTOI Specification**: `docs/VSTOI-DSG-IMPLEMENTATION-COMPLETE.md`
- **DA Templates**: `test/resources/da/`

---

## ✅ Checklist de Aprovação

- [x] INS removido do roundtrip test
- [x] DSG reativado em todos os steps
- [x] DAs integrados no fluxo de ingestão
- [x] Validação de VD sheet implementada
- [x] Logs de debug adicionados
- [x] TTL dumps configurados
- [x] Ordem de dependência respeitada
- [x] Erros de compilação corrigidos
- [x] Documentação criada

---

**Data**: 2026-05-13  
**Autor**: GitHub Copilot  
**Versão**: 1.0

