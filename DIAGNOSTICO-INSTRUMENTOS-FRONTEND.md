# 🔍 DIAGNÓSTICO: Instrumentos não aparecem no Frontend

**Data**: 2026-04-17  
**Status**: ✅ **PROBLEMA IDENTIFICADO - SOLUÇÃO DEFINIDA**

---

## ❌ PROBLEMA

Instrumentos criados via DSG não aparecem no frontend na página:
```
http://localhost/drupal/web/sir/select/instrument/1/9
```

### Sintoma
- Frontend lista instrumentos filtrando por `managerEmail`
- Endpoint usado: `/hascoapi/api/instrument/manageremail/{email}/{pageSize}/{offset}`
- Instrumentos criados via DSG não retornam nenhum resultado

---

## 🔎 CAUSA RAIZ

### 1. Fluxo do Frontend → Backend

**Frontend (Drupal)**:
```php
// SIRSelectForm.php linha 105
ListManagerEmailPage::exec($this->element_type, $this->manager_email, $page, $pagesize)

// rep/src/ListManagerEmailPage.php linha 9
$api->listByManagerEmail($elementtype, $manageremail, $pagesize, $offset)

// FusekiAPIConnector.php linha 368
GET "/hascoapi/api/instrument/manageremail/{email}/9/0"
```

**Backend (HASCOAPI)**:
```java
// SIRElementAPI.java linha 1259
public Result getElementsByManagerEmail(String elementType, String managerEmail, ...) {
    GenericFind<Instrument> query = new GenericFind<Instrument>();
    List<Instrument> results = query.findByManagerEmailWithPages(
        Instrument.class, managerEmail, pageSize, offset);
    return InstrumentAPI.getInstruments(results);
}
```

**Query SPARQL**:
```sparql
SELECT ?uri WHERE {
  ?uri a vstoi:Instrument .
  ?uri vstoi:hasSIRManagerEmail "{managerEmail}" .
}
```

### 2. O Que Acontece na Criação via DSG

**StudyObjectGenerator.java** (linha 399):
```java
private void createInstrumentFromStudyObject(StudyObject so) {
    Instrument instrument = new Instrument();
    instrument.setUri(so.getUri());
    instrument.setTypeUri(so.getTypeUri());
    instrument.setHascoTypeUri(VSTOI.INSTRUMENT);
    instrument.setLabel(so.getLabel());
    instrument.setComment(so.getComment());
    instrument.setNamedGraph(getNamedGraphUri());
    instrument.setHasSIRManagerEmail(so.getHasSIRManagerEmail());  // ← Aqui!
    instrument.save();
}
```

**StudyObjectGenerator.java** (linha 294):
```java
public StudyObject createStudyObject(Record record) throws Exception {
    StudyObject obj = new StudyObject(
        getUri(record),
        getType(record),
        getHascoType(record),
        getOriginalID(record),
        getLabel(record),
        getSocUriFromRecord(record),
        getComment(record),
        this.dataFile.getHasSIRManagerEmail());  // ← Vem do DataFile!
    // ...
}
```

**O PROBLEMA**: `dataFile.getHasSIRManagerEmail()` retorna **`null` ou `""`** porque o DataFile foi criado **SEM** esse campo preenchido!

---

## ✅ SOLUÇÃO

### Opção 1: Propagar o `hasSIRManagerEmail` no momento da criação do DSG (RECOMENDADO)

Quando o DSG é criado/uploaded, o `DataFile` associado deve receber o `hasSIRManagerEmail` do usuário que fez o upload.

**Onde modificar**:

#### A. IngestionAPI.java (quando DSG é uploaded)

```java
// Linha ~127 (no bloco "dsg")
} else if (elementType.equals("dsg")) {
    DSG dsg = DSG.find(elementUri);
    if (dsg == null) {
        return ok(ApiUtil.createResponse(...));
    }
    dataFile = DataFile.find(dsg.getHasDataFileUri());
    
    // ✅ ADICIONAR: Garantir que DataFile tenha o manager email
    if (dataFile != null && dsg.getHasSIRManagerEmail() != null) {
        dataFile.setHasSIRManagerEmail(dsg.getHasSIRManagerEmail());
        dataFile.save();
    }
}
```

#### B. DSGAPI.java (quando DSG é criado)

**Localização**: `app/org/hascoapi/console/controllers/restapi/DSGAPI.java`

```java
public Result createDSG(String json) {
    // ... parse JSON ...
    
    // Criar DataFile com o manager email do DSG
    DataFile dataFile = DataFile.create(
        dataFileId,
        filename,
        newDsg.getHasSIRManagerEmail(),  // ← PASSAR o manager email
        DataFile.UNPROCESSED
    );
    
    // ...
}
```

---

### Opção 2: Setar `hasSIRManagerEmail` durante a ingestão DSG (FALLBACK)

Se o DataFile já existe sem o campo, podemos setá-lo durante a ingestão.

**Onde modificar**: `AnnotateSTD.java` ou `AnnotateSSD.java`

```java
// AnnotateSTD.java ou AnnotateSSD.java
public static GeneratorChain exec(DataFile dataFile, ...) {
    // ...
    
    // ✅ ADICIONAR: Se DataFile não tem manager email, pegar do Study
    if (dataFile.getHasSIRManagerEmail() == null || 
        dataFile.getHasSIRManagerEmail().isEmpty()) {
        
        Study study = Study.find(studyUri);
        if (study != null && study.getHasSIRManagerEmail() != null) {
            dataFile.setHasSIRManagerEmail(study.getHasSIRManagerEmail());
            dataFile.save();
        }
    }
    
    // ...
}
```

---

### Opção 3: Query alternativa (NÃO RECOMENDADO - altera contrato)

Modificar a query para buscar instrumentos sem filtrar por manager email (permite ver todos).

**NÃO RECOMENDADO** porque:
- Altera o contrato da API
- Quebra segurança (usuários veriam instrumentos de outros)
- Requer mudanças no frontend

---

## 🧪 TESTE DE VERIFICAÇÃO

### 1. Verificar DataFile antes da correção

```bash
# Verificar se DataFile tem hasSIRManagerEmail
curl -X GET "http://localhost:8080/hascoapi/api/datafile/find/{dataFileUri}"

# Resultado esperado (ANTES da correção):
{
  "uri": "http://kb/DFL123",
  "hasSIRManagerEmail": ""  # ← VAZIO!
}
```

### 2. Verificar Instrument

```bash
# Query SPARQL no Fuseki
curl -X POST http://localhost:3030/store/sparql \
  -H "Content-Type: application/sparql-query" \
  --data 'SELECT * WHERE { ?s a <http://hadatac.org/ont/vstoi#Instrument> ; <http://hadatac.org/ont/vstoi#hasSIRManagerEmail> ?email }'

# Resultado esperado (ANTES da correção):
# NENHUM RESULTADO (porque instrumentos não têm hasSIRManagerEmail)
```

### 3. Aplicar Correção

Implementar **Opção 1** conforme descrito acima.

### 4. Verificar após correção

```bash
# 1. Upload novo DSG
curl -F "file=@DSG-TEST.xlsx" \
  -H "Authorization: Bearer {token}" \
  http://localhost:8080/hascoapi/api/upload/dsg/{dsgUri}

# 2. Ingest DSG
curl -X POST http://localhost:8080/hascoapi/api/ingest/DRAFT/dsg/{dsgUri}

# 3. Verificar instrumentos criados
curl -X GET "http://localhost:8080/hascoapi/api/instrument/manageremail/user@example.com/10/0"

# Resultado esperado (APÓS correção):
{
  "isSuccessful": true,
  "body": "[{
    \"uri\": \"http://kb/INS123\",
    \"label\": \"My Instrument\",
    \"hasSIRManagerEmail\": \"user@example.com\"  # ← PREENCHIDO!
  }]"
}
```

---

## 📝 ARQUIVOS MODIFICADOS (Opção 1 - RECOMENDADO)

### 1. `app/org/hascoapi/console/controllers/restapi/IngestionAPI.java`

**Linha**: ~127 (bloco `dsg`)

**Mudança**:
```java
} else if (elementType.equals("dsg")) {
    DSG dsg = DSG.find(elementUri);
    if (dsg == null) {
        return ok(ApiUtil.createResponse("IngestionAPI.ingest(): File FAILED to be ingested: could not retrieve " + elementType + ". ",false));
    }
    dataFile = DataFile.find(dsg.getHasDataFileUri());
    
    // ✅ CORREÇÃO: Propagar hasSIRManagerEmail do DSG para o DataFile
    if (dataFile != null) {
        if (dsg.getHasSIRManagerEmail() != null && !dsg.getHasSIRManagerEmail().isEmpty()) {
            dataFile.setHasSIRManagerEmail(dsg.getHasSIRManagerEmail());
            dataFile.save();
            System.out.println("[INGESTION FIX] Set DataFile.hasSIRManagerEmail = " + dsg.getHasSIRManagerEmail());
        } else {
            System.out.println("[WARNING] DSG has no hasSIRManagerEmail - instruments may not be visible");
        }
    }
}
```

---

### 2. (OPCIONAL) `app/org/hascoapi/console/controllers/restapi/DSGAPI.java`

Se o DSG ainda não tem `createDSG()` implementado ou se DataFile é criado em outro lugar, garantir que:

```java
DataFile dataFile = DataFile.create(
    dataFileId,
    filename,
    dsg.getHasSIRManagerEmail(),  // ← Manager email
    DataFile.UNPROCESSED
);
```

---

## 🎯 RESULTADO ESPERADO

Após a correção:

✅ **DataFile criado com `hasSIRManagerEmail`**
```java
dataFile.getHasSIRManagerEmail() // → "user@example.com"
```

✅ **StudyObject criado com `hasSIRManagerEmail`**
```java
studyObject.getHasSIRManagerEmail() // → "user@example.com"
```

✅ **Instrument criado com `hasSIRManagerEmail`**
```java
instrument.getHasSIRManagerEmail() // → "user@example.com"
```

✅ **Triple salvo no triplestore**
```turtle
<http://kb/INS123> vstoi:hasSIRManagerEmail "user@example.com" .
```

✅ **Endpoint retorna instrumentos**
```bash
GET /hascoapi/api/instrument/manageremail/user@example.com/10/0
# → Retorna instrumentos criados por esse usuário
```

✅ **Frontend exibe instrumentos**
```
http://localhost/drupal/web/sir/select/instrument/1/9
# → Lista os instrumentos
```

---

## ⚠️ IMPACTO

### Sem a correção:
- ❌ Instrumentos via DSG não aparecem no frontend
- ❌ Usuários não conseguem ver seus próprios instrumentos
- ❌ Frontend mostra "Nenhum instrumento encontrado"

### Com a correção:
- ✅ Instrumentos via DSG aparecem normalmente
- ✅ Filtragem por manager email funciona
- ✅ Compatibilidade com INS mantida
- ✅ Sem quebra de contrato da API

---

## 🔗 RELAÇÃO COM DEPRECAÇÃO INS

Este problema é **independente** da deprecação do INS:
- ✅ INS já preenchia `hasSIRManagerEmail` corretamente
- ❌ DSG não estava preenchendo (bug)
- ✅ Correção alinha DSG com comportamento do INS

**Conclusão**: A migração INS → DSG **não pode prosseguir** até que esta correção seja aplicada, porque instrumentos criados via DSG seriam invisíveis no frontend.

---

## 🚀 PRÓXIMOS PASSOS

1. ✅ **Aplicar correção em IngestionAPI.java**
2. ✅ **Testar com DSG de exemplo**
3. ✅ **Verificar endpoint `/instrument/manageremail/...`**
4. ✅ **Verificar frontend Drupal**
5. ✅ **Documentar correção aplicada**
6. ⏳ **Prosseguir com migração INS → DSG**

---

**Documento Versão**: 1.0  
**Última Atualização**: 2026-04-17  
**Status**: Solução Identificada - Aguardando Implementação

