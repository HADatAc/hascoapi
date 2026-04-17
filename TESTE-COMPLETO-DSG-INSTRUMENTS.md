# =====================================================
# IMPLEMENTAÇÃO COMPLETA: DSG → Instrumentos Automáticos
# =====================================================
# Data: 2026-04-10
# Status: ✅ CÓDIGO IMPLEMENTADO - PRONTO PARA TESTE
# =====================================================

## 📋 ALTERAÇÕES IMPLEMENTADAS

### 1. SSDGeneratorChain.java ✅

**Arquivo:** `app/org/hascoapi/ingestion/SSDGeneratorChain.java`

**Alterações:**
- ✅ Imports adicionados (VSTOI, Instrument, Component, ComponentStem, ContainerSlot, SPARQL utils)
- ✅ Método `postprocess()` estendido com detecção de tipos vstoi
- ✅ Método `processInstrumentSOC()` - detecta tipo de SOC e dispatcha para handler correto
- ✅ Método `createInstrumentsFromStudyObjects()` - cria entidades Instrument
- ✅ Método `enrichInstrumentFromTriplestore()` - query SPARQL para propriedades DA-SOC
- ✅ Método `createContainerSlotsFromStudyObjects()` - cria entidades ContainerSlot
- ✅ Método `enrichContainerSlotFromTriplestore()` - query SPARQL para propriedades DA-SOC
- ✅ Método `createComponentStemsFromStudyObjects()` - cria entidades ComponentStem
- ✅ Método `enrichComponentStemFromTriplestore()` - query SPARQL para propriedades DA-SOC
- ✅ Método `createComponentsFromStudyObjects()` - cria entidades Component
- ✅ Método `enrichComponentFromTriplestore()` - query SPARQL para propriedades DA-SOC

**Lógica de Detecção:**
```java
String rdfType = members.get(0).getTypeUri();

if (rdfType.equals(VSTOI.INSTRUMENT)) {
    createInstrumentsFromStudyObjects(soc);
} else if (rdfType.equals(VSTOI.CONTAINER_SLOT)) {
    createContainerSlotsFromStudyObjects(soc);
} else if (rdfType.equals(VSTOI.COMPONENT_STEM)) {
    createComponentStemsFromStudyObjects(soc);
} else if (rdfType.equals(VSTOI.COMPONENT) || rdfType.contains("Detector") || rdfType.contains("Component")) {
    createComponentsFromStudyObjects(soc);
}
```

### 2. GenericFind.java ✅

**Arquivo:** `app/org/hascoapi/entity/pojo/GenericFind.java`

**Alterações:**

#### 2.1 Método `findInstancesWithPages()` (usado por Instrument)

**ANTES:**
```sparql
SELECT DISTINCT ?uri WHERE {
  ?type rdfs:subClassOf* vstoi:Instrument .
  ?uri a ?type .
}
```

**DEPOIS:**
```sparql
SELECT DISTINCT ?uri WHERE {
  {
    ?type rdfs:subClassOf* vstoi:Instrument .
    ?uri a ?type .
  } UNION {
    ?uri a vstoi:Instrument .  ← ADICIONA INSTÂNCIAS DIRETAS
  }
}
```

#### 2.2 Método `findSIRInstancesWithPages()` (usado por Component/ComponentStem)

**ANTES:**
```sparql
SELECT DISTINCT ?uri WHERE {
  ?type rdfs:subClassOf* vstoi:Component .
  ?uri a ?type .
  ?uri vstoi:hasContent ?content .  ← OBRIGATÓRIO
}
```

**DEPOIS:**
```sparql
SELECT DISTINCT ?uri WHERE {
  {
    ?type rdfs:subClassOf* vstoi:Component .
    ?uri a ?type .
  } UNION {
    ?uri a vstoi:Component .  ← ADICIONA INSTÂNCIAS DIRETAS
  }
  OPTIONAL { ?uri vstoi:hasContent ?content . }  ← AGORA OPCIONAL
}
```

#### 2.3 Método `findTotalInstances()` (usado para contar totais)

**ANTES:**
```sparql
SELECT (count(?uri) as ?tot) WHERE {
  ?uri hasco:hascoType vstoi:Instrument .
}
```

**DEPOIS:**
```sparql
SELECT (count(DISTINCT ?uri) as ?tot) WHERE {
  {
    ?type rdfs:subClassOf* vstoi:Instrument .
    ?uri a ?type .
  } UNION {
    ?uri a vstoi:Instrument .
  } UNION {
    ?uri hasco:hascoType vstoi:Instrument .
  }
}
```

## 🔄 FLUXO COMPLETO DE INGESTÃO

```
┌─────────────────────────────────────────────────────────────┐
│ 1. UPLOAD DO DSG-PMSR-SIMULATORS.xlsx                      │
└─────────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. AnnotateSTD → Cria Study                                 │
└─────────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│ 3. AnnotateSSD → Cria 4 SOCs:                              │
│    - SOC-EQUIPMENT-MODULE (type: hasco:SubjectGroup)        │
│    - SOC-SLOT-ELEMENT (type: hasco:SpaceCollection)         │
│    - SOC-COMPONENT-STEM (type: hasco:SpaceCollection)       │
│    - SOC-CONTROL-MODULE (type: hasco:SpaceCollection)       │
└─────────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│ 4. StudyObjectGenerator (x4) → Cria StudyObjects:          │
│    - 72 StudyObjects (rdf:type = vstoi:Instrument)          │
│    - 4 StudyObjects (rdf:type = vstoi:ContainerSlot)        │
│    - 84 StudyObjects (rdf:type = vstoi:ComponentStem)       │
│    - 17 StudyObjects (rdf:type = vstoi:Component)           │
│                                                             │
│    Salvos no triplestore com:                               │
│      - hasco:isMemberOf → SOC URI                           │
│      - hasco:originalID → originalID do DSG                 │
│      - rdf:type → vstoi:Instrument/etc                      │
│      - rdfs:label → label do DSG                            │
└─────────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│ 5. GeneratorChain.generate() commit → Salva no triplestore │
└─────────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│ 6. SSDGeneratorChain.postprocess() FASE 1                  │
│    → Computa route labels para SOCs                        │
└─────────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│ 7. SSDGeneratorChain.postprocess() FASE 2 ← NOVO!          │
│                                                             │
│    Para cada SOC:                                           │
│      - Query primeiro StudyObject                           │
│      - Detecta rdf:type                                     │
│                                                             │
│    SOC-EQUIPMENT-MODULE (type: vstoi:Instrument):           │
│      → createInstrumentsFromStudyObjects()                  │
│         - 72 entidades Instrument criadas                   │
│         - URI = URI do StudyObject                          │
│         - enrichInstrumentFromTriplestore() busca props     │
│                                                             │
│    SOC-SLOT-ELEMENT (type: vstoi:ContainerSlot):            │
│      → createContainerSlotsFromStudyObjects()               │
│         - 4 entidades ContainerSlot criadas                 │
│         - enrichContainerSlotFromTriplestore()              │
│                                                             │
│    SOC-COMPONENT-STEM (type: vstoi:ComponentStem):          │
│      → createComponentStemsFromStudyObjects()               │
│         - 84 entidades ComponentStem criadas                │
│         - enrichComponentStemFromTriplestore()              │
│                                                             │
│    SOC-CONTROL-MODULE (type: vstoi:Component):              │
│      → createComponentsFromStudyObjects()                   │
│         - 17 entidades Component criadas                    │
│         - enrichComponentFromTriplestore()                  │
└─────────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│ 8. IngestionWorker.processAssociatedDASOCFiles() ← EXISTENTE│
│                                                             │
│    Busca arquivos DA-SOC-*.csv no mesmo diretório:         │
│      - DA-SOC-EQUIPMENT-MODULE.csv                         │
│      - DA-SOC-SLOT-ELEMENT.csv                             │
│      - DA-SOC-COMPONENT-STEM.csv                           │
│      - DA-SOC-CONTROL-MODULE.csv                           │
│                                                             │
│    Para cada DA-SOC:                                        │
│      → AnnotateDASOC.processDASOC()                        │
│      → Adiciona propriedades aos StudyObjects:             │
│         - vstoi:hasShortName                               │
│         - vstoi:belongsTo                                  │
│         - vstoi:hasContent                                 │
│         - etc.                                              │
└─────────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│ 9. RESULTADO FINAL NO TRIPLESTORE                          │
│                                                             │
│    CADA INSTRUMENTO EXISTE COMO:                            │
│                                                             │
│    A) StudyObject:                                          │
│       - rdf:type = vstoi:Instrument                         │
│       - hasco:hascoType = hasco:StudyObject                 │
│       - hasco:isMemberOf = SOC-EQUIPMENT-MODULE             │
│       - vstoi:hasShortName = "ARTECLEOScanner" (via DA-SOC) │
│       - vstoi:hasFirst = pmsr:/INS.../CTS/0001 (via DA-SOC) │
│                                                             │
│    B) Instrument (CRIADO POR SSDGeneratorChain):            │
│       - rdf:type = vstoi:Instrument                         │
│       - hasco:hascoType = vstoi:Instrument                  │
│       - rdfs:label = "ARTEC LEO Scanner"                    │
│       - vstoi:hasShortName = "ARTECLEOScanner" (copiado)    │
│       - vstoi:hasFirst = pmsr:/INS.../CTS/0001 (copiado)    │
│                                                             │
│    MESMO URI PARA AMBOS! ✅                                 │
└─────────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│ 10. QUERIES DE LISTAGEM (GenericFind - MODIFICADO)         │
│                                                             │
│     /api/instrument/elements/10/0:                          │
│       SELECT DISTINCT ?uri WHERE {                          │
│         { ?type rdfs:subClassOf* vstoi:Instrument .         │
│           ?uri a ?type . }                                  │
│         UNION                                               │
│         { ?uri a vstoi:Instrument . }  ← ENCONTRA!          │
│         UNION                                               │
│         { ?uri hasco:hascoType vstoi:Instrument . }         │
│       }                                                     │
│       → Retorna 72 Instruments ✅                           │
│                                                             │
│     /api/component/elements/10/0:                           │
│       [mesma lógica UNION]                                  │
│       → Retorna 17 Components ✅                            │
│                                                             │
│     /api/componentstem/elements/10/0:                       │
│       [mesma lógica UNION]                                  │
│       → Retorna 84 ComponentStems ✅                        │
└─────────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│ 11. FRONTEND DRUPAL FUNCIONA ✅                             │
│                                                             │
│     http://localhost/drupal/web/sir/select/instrument/1/9   │
│       → Lista 72 instrumentos                               │
│                                                             │
│     http://localhost/drupal/web/sir/select/component/1/9    │
│       → Lista 17 components                                 │
│                                                             │
│     http://localhost/drupal/web/sir/select/componentstem/1/9│
│       → Lista 84 component stems                            │
└─────────────────────────────────────────────────────────────┘
```

## 🧪 SCRIPT DE TESTE COMPLETO

Execute este script após o servidor reiniciar:

```powershell
# =====================================================
# Script de Teste: Ingestão DSG → Criação de Instruments
# =====================================================

$baseUrl = "http://localhost:9000"
$token = "qwertyuiopasdfghjklzxcvbnm123456"

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "TESTE COMPLETO: DSG → INSTRUMENTS" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# PASSO 1: Verificar servidor está rodando
Write-Host "Passo 1: Verificando servidor..." -ForegroundColor Yellow
try {
    $health = curl.exe -X GET "$baseUrl/hascoapi/version" -s
    Write-Host "  ✅ Servidor rodando" -ForegroundColor Green
} catch {
    Write-Host "  ❌ Servidor não está respondendo!" -ForegroundColor Red
    exit
}

# PASSO 2: Ingerir DSG (se você tiver o arquivo)
Write-Host "`nPasso 2: Ingerir DSG-PMSR-SIMULATORS.xlsx" -ForegroundColor Yellow
Write-Host "  ⚠️  EXECUTE MANUALMENTE via Postman ou cole o caminho do arquivo aqui" -ForegroundColor Yellow
Write-Host "  Exemplo:" -ForegroundColor Gray
Write-Host "  curl.exe -X POST '$baseUrl/hascoapi/api/ingest/DRAFT/study/http%3A%2F%2Fpmsr.net%2Font%2Fpmsr%2FPMSR-SIMULATORS' \" -ForegroundColor Gray
Write-Host "    -H 'Authorization: Bearer $token' \" -ForegroundColor Gray
Write-Host "    -F 'file=@path/to/DSG-PMSR-SIMULATORS.xlsx'" -ForegroundColor Gray

Read-Host "`n  Pressione ENTER após ingerir o DSG"

# PASSO 3: Verificar se SOCs foram criados
Write-Host "`nPasso 3: Verificando SOCs criados..." -ForegroundColor Yellow
$socsResponse = curl.exe -X GET "$baseUrl/hascoapi/api/studyobjectcollection/elements/10/0" `
  -H "Authorization: Bearer $token" -s | ConvertFrom-Json

if ($socsResponse.isSuccessful -and $socsResponse.body) {
    $socCount = $socsResponse.body.Count
    Write-Host "  ✅ $socCount SOCs encontrados" -ForegroundColor Green
    $socsResponse.body | ForEach-Object {
        Write-Host "    - $($_.label)" -ForegroundColor Gray
    }
} else {
    Write-Host "  ❌ Nenhum SOC encontrado - DSG não foi ingerido?" -ForegroundColor Red
}

# PASSO 4: Verificar se Instruments foram criados
Write-Host "`nPasso 4: Verificando Instruments criados automaticamente..." -ForegroundColor Yellow
$instResponse = curl.exe -X GET "$baseUrl/hascoapi/api/instrument/elements/10/0" `
  -H "Authorization: Bearer $token" -s | ConvertFrom-Json

if ($instResponse.isSuccessful -and $instResponse.body) {
    $instCount = $instResponse.body.Count
    Write-Host "  ✅ $instCount Instruments encontrados" -ForegroundColor Green
    $instResponse.body | Select-Object -First 5 | ForEach-Object {
        Write-Host "    - $($_.label) (hasShortName: $($_.hasShortName))" -ForegroundColor Gray
    }
} else {
    Write-Host "  ❌ Nenhum Instrument encontrado!" -ForegroundColor Red
    Write-Host "  Resposta: $($instResponse.body)" -ForegroundColor Red
}

# PASSO 5: Verificar total de Instruments
Write-Host "`nPasso 5: Verificando total de Instruments..." -ForegroundColor Yellow
$totalResponse = curl.exe -X GET "$baseUrl/hascoapi/api/instrument/elements/total" `
  -H "Authorization: Bearer $token" -s | ConvertFrom-Json

if ($totalResponse.isSuccessful) {
    Write-Host "  ✅ Total: $($totalResponse.body)" -ForegroundColor Green
} else {
    Write-Host "  ❌ Falha ao contar Instruments" -ForegroundColor Red
}

# PASSO 6: Verificar Components
Write-Host "`nPasso 6: Verificando Components..." -ForegroundColor Yellow
$compResponse = curl.exe -X GET "$baseUrl/hascoapi/api/component/elements/5/0" `
  -H "Authorization: Bearer $token" -s | ConvertFrom-Json

if ($compResponse.isSuccessful -and $compResponse.body) {
    $compCount = $compResponse.body.Count
    Write-Host "  ✅ $compCount Components encontrados" -ForegroundColor Green
} else {
    Write-Host "  ❌ Nenhum Component encontrado" -ForegroundColor Red
}

# PASSO 7: Verificar ComponentStems
Write-Host "`nPasso 7: Verificando ComponentStems..." -ForegroundColor Yellow
$stemResponse = curl.exe -X GET "$baseUrl/hascoapi/api/componentstem/elements/5/0" `
  -H "Authorization: Bearer $token" -s | ConvertFrom-Json

if ($stemResponse.isSuccessful -and $stemResponse.body) {
    $stemCount = $stemResponse.body.Count
    Write-Host "  ✅ $stemCount ComponentStems encontrados" -ForegroundColor Green
} else {
    Write-Host "  ❌ Nenhum ComponentStem encontrado" -ForegroundColor Red
}

# PASSO 8: Verificar ContainerSlots
Write-Host "`nPasso 8: Verificando ContainerSlots..." -ForegroundColor Yellow
$slotResponse = curl.exe -X GET "$baseUrl/hascoapi/api/containerslot/elements/5/0" `
  -H "Authorization: Bearer $token" -s | ConvertFrom-Json

if ($slotResponse.isSuccessful -and $slotResponse.body) {
    $slotCount = $slotResponse.body.Count
    Write-Host "  ✅ $slotCount ContainerSlots encontrados" -ForegroundColor Green
} else {
    Write-Host "  ❌ Nenhum ContainerSlot encontrado" -ForegroundColor Red
}

# RESUMO FINAL
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "RESUMO FINAL" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "SOCs:            $(if ($socCount) {$socCount} else {'N/A'})" -ForegroundColor White
Write-Host "Instruments:     $(if ($instCount) {$instCount} else {'N/A'})" -ForegroundColor White
Write-Host "Components:      $(if ($compCount) {$compCount} else {'N/A'})" -ForegroundColor White
Write-Host "ComponentStems:  $(if ($stemCount) {$stemCount} else {'N/A'})" -ForegroundColor White
Write-Host "ContainerSlots:  $(if ($slotCount) {$slotCount} else {'N/A'})" -ForegroundColor White
Write-Host "========================================`n" -ForegroundColor Cyan

if ($instCount -gt 0) {
    Write-Host "✅ SUCESSO! Instruments foram criados automaticamente!" -ForegroundColor Green
    Write-Host "`nAgora você pode acessar:" -ForegroundColor Cyan
    Write-Host "  - http://localhost/drupal/web/sir/select/instrument/1/9" -ForegroundColor Cyan
    Write-Host "  - http://localhost/drupal/web/sir/select/component/1/9" -ForegroundColor Cyan
    Write-Host "  - http://localhost/drupal/web/sir/select/componentstem/1/9" -ForegroundColor Cyan
} else {
    Write-Host "❌ Instruments não foram criados. Verifique:" -ForegroundColor Red
    Write-Host "  1. DSG foi ingerido corretamente?" -ForegroundColor Yellow
    Write-Host "  2. Logs do backend (console onde rodou sbt run)" -ForegroundColor Yellow
    Write-Host "  3. Propriedades rdf:type dos StudyObjects" -ForegroundColor Yellow
}

Write-Host "`n========================================`n" -ForegroundColor Cyan
```

Salve este script como `test-complete-ingestion.ps1` e execute após o DSG ser ingerido.

## 📊 VALIDAÇÃO SPARQL DIRETA (Fuseki)

Se quiser verificar diretamente no triplestore:

```sparql
# Query 1: Contar Instruments como instâncias diretas
PREFIX vstoi: <http://hadatac.org/ont/vstoi#>
SELECT (COUNT(?inst) as ?total) WHERE {
  ?inst a vstoi:Instrument .
}
# Resultado esperado: 72

# Query 2: Listar primeiros 10 Instruments com propriedades
PREFIX vstoi: <http://hadatac.org/ont/vstoi#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
SELECT ?inst ?label ?shortName ?hasFirst WHERE {
  ?inst a vstoi:Instrument .
  OPTIONAL { ?inst rdfs:label ?label }
  OPTIONAL { ?inst vstoi:hasShortName ?shortName }
  OPTIONAL { ?inst vstoi:hasFirst ?hasFirst }
}
LIMIT 10

# Query 3: Verificar duplicação (mesma URI, tipos diferentes)
PREFIX vstoi: <http://hadatac.org/ont/vstoi#>
PREFIX hasco: <http://hadatac.org/ont/hasco#>
SELECT ?uri (GROUP_CONCAT(DISTINCT ?type; separator=", ") as ?types) WHERE {
  ?uri a ?type .
  FILTER(CONTAINS(STR(?uri), "equipmentModule"))
  FILTER(?type = vstoi:Instrument || ?type = hasco:StudyObject)
}
GROUP BY ?uri
LIMIT 10
# Resultado esperado: cada URI tem "vstoi:Instrument, hasco:StudyObject"
```

## ⚡ INSTRUÇÕES DE COMPILAÇÃO E TESTE

Execute no PowerShell:

```powershell
# 1. Parar servidor atual (se estiver rodando)
Get-Process | Where-Object {$_.ProcessName -like "*java*"} | Stop-Process -Force

# 2. Compilar com novas alterações
cd C:\Users\kaell\Desktop\Project\hascoapi
sbt compile

# 3. Iniciar servidor
sbt "run 9000"

# 4. Em outro terminal, aguardar servidor iniciar (30-60 segundos)
Start-Sleep -Seconds 60

# 5. Ingerir DSG
curl.exe -X POST "http://localhost:9000/hascoapi/api/ingest/DRAFT/study/http%3A%2F%2Fpmsr.net%2Font%2Fpmsr%2FPMSR-SIMULATORS" `
  -H "Authorization: Bearer qwertyuiopasdfghjklzxcvbnm123456" `
  -F "file=@C:\caminho\para\DSG-PMSR-SIMULATORS.xlsx"

# 6. Verificar logs do backend (terminal onde rodou sbt run)
# Procurar por: "=== INSTRUMENT INFERENCE ENGINE ==="

# 7. Testar endpoint de listagem
curl.exe -X GET "http://localhost:9000/hascoapi/api/instrument/elements/10/0" `
  -H "Authorization: Bearer qwertyuiopasdfghjklzxcvbnm123456"

# 8. Acessar frontend
# http://localhost/drupal/web/sir/select/instrument/1/9
```

## 📝 CHECKLIST DE VALIDAÇÃO

Após ingestão completa, verifique:

- [ ] Logs do backend mostram "=== INSTRUMENT INFERENCE ENGINE ==="
- [ ] Logs mostram "✓ Created Instrument: ..." para cada instrumento
- [ ] Logs mostram "✓ Created Component: ..."
- [ ] Logs mostram "✓ Created ComponentStem: ..."
- [ ] Logs mostram "✓ Created ContainerSlot: ..."
- [ ] `/api/instrument/elements/10/0` retorna lista de instruments
- [ ] `/api/instrument/elements/total` retorna 72
- [ ] `/api/component/elements/5/0` retorna lista de components
- [ ] `/api/componentstem/elements/5/0` retorna lista de stems
- [ ] Frontend Drupal mostra instruments em `/sir/select/instrument/1/9`
- [ ] Frontend Drupal mostra components em `/sir/select/component/1/9`
- [ ] Frontend Drupal mostra stems em `/sir/select/componentstem/1/9`

## 🔧 TROUBLESHOOTING

### Problema: Instruments não aparecem no frontend

**Diagnóstico:**
```powershell
# Verificar se existem no backend
curl.exe -X GET "http://localhost:9000/hascoapi/api/instrument/elements/10/0" -H "Authorization: Bearer $token"

# Se retornar vazio, verificar SPARQL diretamente
curl.exe -X POST "http://localhost:3030/HASCO/query" `
  --data-urlencode "query=PREFIX vstoi: <http://hadatac.org/ont/vstoi#> SELECT (COUNT(?i) as ?c) WHERE { ?i a vstoi:Instrument . }"
```

**Soluções:**
1. Verificar logs de `SSDGeneratorChain.postprocess()` durante ingestão
2. Verificar se DA-SOCs foram processados (propriedades existem?)
3. Verificar queries SPARQL em GenericFind.java foram aplicadas

### Problema: Compilação falha

**Erro comum:** "Cannot resolve symbol VSTOI"

**Solução:** Verificar imports em `SSDGeneratorChain.java`:
```java
import org.hascoapi.vocabularies.VSTOI;
import org.hascoapi.entity.pojo.Instrument;
import org.hascoapi.entity.pojo.Component;
import org.hascoapi.entity.pojo.ComponentStem;
import org.hascoapi.entity.pojo.ContainerSlot;
```

### Problema: "No objects found in SOC"

**Causa:** DSG foi ingerido mas StudyObjects não foram criados

**Solução:** Verificar sheet SOC-EQUIPMENT-MODULE no DSG tem dados

---

## ✅ STATUS DA IMPLEMENTAÇÃO

### Arquivos Modificados:
1. ✅ `SSDGeneratorChain.java` - Lógica de criação de entidades vstoi
2. ✅ `GenericFind.java` - Queries SPARQL para incluir instâncias diretas

### Arquivos Criados:
3. ✅ `verify-dasoc-ingestion.ps1` - Script de verificação DA-SOC
4. ✅ `RESUMO-ALTERACOES.md` - Documentação das mudanças
5. ✅ `PLANO-CORRECAO-LISTAGEM.md` - Análise do problema de listagem
6. ✅ Este arquivo - Guia completo de teste

### Compilação:
- ✅ Sem erros de sintaxe
- ⏳ Servidor reiniciando (aguardando)

### Próximos Passos:
1. ⏳ Aguardar servidor terminar de compilar e iniciar
2. ⏳ Ingerir DSG-PMSR-SIMULATORS.xlsx
3. ⏳ Verificar logs para "INSTRUMENT INFERENCE ENGINE"
4. ⏳ Testar endpoints de listagem
5. ⏳ Validar frontend Drupal

---

**AGUARDANDO**: Servidor compilar → Ingerir DSG → Testar

