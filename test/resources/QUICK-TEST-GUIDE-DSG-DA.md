# Quick Test Guide - DSG + DA Roundtrip

## 🚀 Execução Rápida

### 1. Executar Teste Completo DSG
```bash
cd C:\Users\kaell\Desktop\Project\hascoapi
sbt "testOnly org.hascoapi.tests.HascoRoundtripTest.step1_allMTs_ingest -- -z DSG"
```

### 2. Verificar Logs
Procure por:
```
[DSG+DA] Ingesting related Data Acquisition templates...
[DSG+DA] Ingesting: DA-SOC-CODEBOOK.csv
[DSG+DA] Successfully ingested: DA-SOC-CODEBOOK.csv (status: DRAFT)
...
[DSG+DA] Completed ingestion of all DA templates for study: ...
```

### 3. Verificar Arquivos Gerados
```bash
dir test\resources\generated\DA_*.ttl
```

Deve mostrar:
```
DA_DA-SOC-CODEBOOK.ttl
DA_DA-SOC-COMPONENT-STEM.ttl
DA_DA-SOC-COMPONENT.ttl
DA_DA-SOC-INSTRUMENT-PMSR.ttl
DA_DA-SOC-RESPONSE-OPTION.ttl
DA_DA-SOC-SLOT-ELEMENT.ttl
```

---

## 📊 Ordem de Ingestão dos DAs

Os DAs são ingeridos nesta ordem (respeitando dependências):

1. **DA-SOC-CODEBOOK.csv** → Base (sem dependências)
2. **DA-SOC-RESPONSE-OPTION.csv** → Depende de Codebook
3. **DA-SOC-COMPONENT-STEM.csv** → Base (sem dependências)
4. **DA-SOC-COMPONENT.csv** → Depende de ComponentStem + Codebook
5. **DA-SOC-INSTRUMENT-PMSR.csv** → Depende de Component
6. **DA-SOC-SLOT-ELEMENT.csv** → Depende de Instrument

**⚠️ NUNCA altere esta ordem sem verificar as dependências!**

---

## 🔍 Verificações Essenciais

### Antes de Executar
```bash
# 1. Verificar que os arquivos DA existem
dir test\resources\da\*.csv

# 2. Verificar que o DSG existe
dir test\resources\dsg\DSG-STD-test.xlsx

# 3. Verificar que pasta generated existe
if not exist test\resources\generated mkdir test\resources\generated
```

### Após Execução
```bash
# 1. Verificar TTLs gerados
dir test\resources\generated\DA_*.ttl

# 2. Ver conteúdo de um TTL (exemplo)
type test\resources\generated\DA_DA-SOC-CODEBOOK.ttl | Select-Object -First 20

# 3. Verificar logs no console
# Procurar por: [DSG+DA], [TTL], [DSG VALIDATION]
```

---

## ✅ Checklist de Sucesso

Após executar o teste, verifique que:

- [ ] Console mostra `[DSG+DA] Ingesting related Data Acquisition templates...`
- [ ] Console mostra 6 mensagens `[DSG+DA] Successfully ingested: DA-SOC-XXX.csv`
- [ ] Console mostra `[DSG+DA] Completed ingestion of all DA templates`
- [ ] 6 arquivos TTL foram criados em `test/resources/generated/`
- [ ] Cada TTL tem `tripleCount > 0`
- [ ] Console mostra `[DSG VALIDATION] VD sheet has X rows` onde X > 0
- [ ] Teste termina com `STEP 1/3 - DONE - MT=DSG`
- [ ] Nenhum erro de exceção aparece

---

## 🐛 Troubleshooting Rápido

### Erro: "DA file not found"
```bash
# Verificar que arquivo existe
dir test\resources\da\DA-SOC-CODEBOOK.csv

# Se não existir, verificar localização correta
dir /s DA-SOC-CODEBOOK.csv
```

### Erro: "Failed to ingest DA file"
```bash
# Verificar conteúdo do CSV
type test\resources\da\DA-SOC-CODEBOOK.csv

# Verificar que tem header correto:
# originalID,rdf:type,vstoi:hasContent,vstoi:hasLanguage,vstoi:hasVersion,...
```

### Erro: "VD sheet has 0 rows"
```bash
# Verificar que DSG tem SSD sheet
# Verificar que DSG tem SOC sheets
# Verificar que VirtualColumns foram criados
```

---

## 📝 Comandos Úteis

### Executar apenas Step 1
```bash
sbt "testOnly org.hascoapi.tests.HascoRoundtripTest.step1_allMTs_ingest"
```

### Executar apenas DSG Step 1
```bash
sbt "testOnly org.hascoapi.tests.HascoRoundtripTest.step1_allMTs_ingest -- -z DSG"
```

### Executar Step 2 (Regeneration)
```bash
sbt "testOnly org.hascoapi.tests.HascoRoundtripTest.step2_allMTs_regenerate_and_compare -- -z DSG"
```

### Executar Step 3 (Reset)
```bash
sbt "testOnly org.hascoapi.tests.HascoRoundtripTest.step3_allMTs_reset_and_deterministic_reingest -- -z DSG"
```

### Executar todos os 3 steps para DSG
```bash
sbt "testOnly org.hascoapi.tests.HascoRoundtripTest -- -z DSG"
```

### Limpar arquivos gerados
```bash
del test\resources\generated\DA_*.ttl
del test\resources\generated\DSG-*.xlsx
```

---

## 📊 Logs Esperados (Resumido)

```
[TEST ENTRYPOINT: step1_allMTs_ingest (MT=DSG)]
---------------------------------------------------------------------------------------

Test input: test\resources\dsg\DSG-STD-test.xlsx
Ingesting DSG...
Study URI created: http://hadatac.org/ont/arrowhead/STD-LTE-PIAGET-WEATHER-STATION

[DSG+DA] Ingesting related Data Acquisition templates...
[DSG+DA] Ingesting: DA-SOC-CODEBOOK.csv
[TTL] DA_DA-SOC-CODEBOOK: tripleCount=120
[DSG+DA] Successfully ingested: DA-SOC-CODEBOOK.csv (status: DRAFT)

[DSG+DA] Ingesting: DA-SOC-RESPONSE-OPTION.csv
[TTL] DA_DA-SOC-RESPONSE-OPTION: tripleCount=75
[DSG+DA] Successfully ingested: DA-SOC-RESPONSE-OPTION.csv (status: DRAFT)

[DSG+DA] Ingesting: DA-SOC-COMPONENT-STEM.csv
[TTL] DA_DA-SOC-COMPONENT-STEM: tripleCount=85
[DSG+DA] Successfully ingested: DA-SOC-COMPONENT-STEM.csv (status: DRAFT)

[DSG+DA] Ingesting: DA-SOC-COMPONENT.csv
[TTL] DA_DA-SOC-COMPONENT: tripleCount=250
[DSG+DA] Successfully ingested: DA-SOC-COMPONENT.csv (status: DRAFT)

[DSG+DA] Ingesting: DA-SOC-INSTRUMENT-PMSR.csv
[TTL] DA_DA-SOC-INSTRUMENT-PMSR: tripleCount=180
[DSG+DA] Successfully ingested: DA-SOC-INSTRUMENT-PMSR.csv (status: DRAFT)

[DSG+DA] Ingesting: DA-SOC-SLOT-ELEMENT.csv
[TTL] DA_DA-SOC-SLOT-ELEMENT: tripleCount=95
[DSG+DA] Successfully ingested: DA-SOC-SLOT-ELEMENT.csv (status: DRAFT)

[DSG+DA] Completed ingestion of all DA templates for study: ...

---------------------------------------------------------------------------------------
STEP 1/3 - DONE - MT=DSG studyUri=http://hadatac.org/ont/arrowhead/STD-...
---------------------------------------------------------------------------------------
```

---

**Data**: 2026-05-13  
**Status**: ✅ Pronto para uso

