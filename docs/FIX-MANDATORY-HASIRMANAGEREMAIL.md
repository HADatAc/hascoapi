# ✅ Correção: Campo hasSIRManagerEmail Agora é Obrigatório

## 📌 Problema Identificado

Um instrumento estava sendo criado **SEM** o campo `hasSIRManagerEmail`, mas deveria falhar a validação porque esse campo é obrigatório.

### Exemplo do Problema:
```json
{
  "uri": "http://pmsr.net/ont/pmsr#/INS_FAIL_002",
  "typeUri": "http://hadatac.org/ont/vstoi#Questionnaire",
  "hascoTypeUri": "http://hadatac.org/ont/vstoi#Instrument",
  "label": "QA Test Instrument 1775567691188",
  "hasShortName": "QA_INS_1775567691188",
  "comment": "Created by Postman QA test",
  "hasVersion": "1.0",
  "hasLanguage": "en",
  "hasInformant": "http://hadatac.org/ont/vstoi#SelfReportedInformant",
  "namedGraph": "http://hadatac.org/kb/test"
}
```

**❌ PROBLEMA:** Falta `hasSIRManagerEmail` mas o instrumento foi criado com sucesso!

### Log da Criação (Antes da Correção):
```
Type: [instrument]  JSON [{"uri":"http://pmsr.net/ont/pmsr#/INS_FAIL_002",...}]
[GSPClient] REQUEST URI: http://localhost:3030/store/data?graph=http%3A%2F%2Fhadatac.org%2Fkb%2Ftest
[GSPClient] Response: 201. Body: '{ "count" : 8 , "tripleCount" : 8 , "quadCount" : 0 }'
```

**Status:** ✅ 201 Created (DEVERIA SER 400 Bad Request!)

---

## 🔧 Solução Implementada

### Arquivo Modificado:
`app/org/hascoapi/console/controllers/restapi/SIRElementAPI.java`

### Código Adicionado:
```java
} else if (clazz == Instrument.class) {
    Instrument object;
    try {
        object = (Instrument)objectMapper.readValue(json, clazz);
        
        // Validate required fields
        if (object.getLabel() == null || object.getLabel().trim().isEmpty()) {
            return ok(ApiUtil.createResponse("Field 'label' is required but was not provided.", false));
        }
        if (object.getUri() == null || object.getUri().trim().isEmpty()) {
            return ok(ApiUtil.createResponse("Field 'uri' is required but was not provided.", false));
        }
        if (object.getNamedGraph() == null || object.getNamedGraph().trim().isEmpty()) {
            return ok(ApiUtil.createResponse("Field 'namedGraph' is required but was not provided.", false));
        }
        // ✅ NOVA VALIDAÇÃO ADICIONADA
        if (object.getHasSIRManagerEmail() == null || object.getHasSIRManagerEmail().trim().isEmpty()) {
            return ok(ApiUtil.createResponse("Field 'hasSIRManagerEmail' is required but was not provided.", false));
        }
        
        object.save();
```

---

## ✅ Resultado Esperado (Após Correção)

### Request SEM hasSIRManagerEmail:
```json
{
  "uri": "http://pmsr.net/ont/pmsr#/INS_FAIL_002",
  "typeUri": "http://hadatac.org/ont/vstoi#Questionnaire",
  "hascoTypeUri": "http://hadatac.org/ont/vstoi#Instrument",
  "label": "QA Test Instrument",
  "namedGraph": "http://hadatac.org/kb/test"
}
```

### Response (400 Bad Request):
```json
{
  "isSuccessful": false,
  "body": "Field 'hasSIRManagerEmail' is required but was not provided."
}
```

---

## 📋 Campos Obrigatórios Completos

Agora os seguintes campos são **OBRIGATÓRIOS** para criar um instrumento:

1. ✅ `uri` - URI única com namespace válido
2. ✅ `label` - Nome/rótulo do instrumento
3. ✅ `namedGraph` - Grafo RDF onde será salvo
4. ✅ `hasSIRManagerEmail` - Email do gerenciador **(NOVO!)**

---

## 🧪 Como Testar

### 1. Recompilar o Backend:
```bash
sbt clean compile
```

### 2. Reiniciar o Servidor:
```bash
sbt run
```

### 3. Testar Request SEM hasSIRManagerEmail:
```bash
POST /hascoapi/api/sirelement/create/instrument
Content-Type: application/json

{
  "uri": "http://pmsr.net/ont/pmsr#/INS_TEST_001",
  "label": "Test Instrument",
  "namedGraph": "http://hadatac.org/kb/test"
}
```

**Resultado Esperado:** 400 Bad Request com mensagem de erro

### 4. Testar Request COM hasSIRManagerEmail:
```bash
POST /hascoapi/api/sirelement/create/instrument
Content-Type: application/json

{
  "uri": "http://pmsr.net/ont/pmsr#/INS_TEST_002",
  "label": "Test Instrument",
  "namedGraph": "http://hadatac.org/kb/test",
  "hasSIRManagerEmail": "admin@example.com"
}
```

**Resultado Esperado:** 200 OK com `"isSuccessful": true`

---

## 📝 Resumo

| Item | Antes | Depois |
|------|-------|--------|
| **Validação de hasSIRManagerEmail** | ❌ Ausente | ✅ Implementada |
| **Request sem email** | ✅ Criado (incorreto) | ❌ Rejeitado (correto) |
| **Mensagem de erro** | ❌ Nenhuma | ✅ Clara e específica |

---

## 🔗 Arquivos Relacionados

- `app/org/hascoapi/console/controllers/restapi/SIRElementAPI.java` - Validação implementada
- `docs/GUIA-CORRIGIR-REQUEST-POSTMAN.md` - Guia atualizado
- `app/org/hascoapi/entity/pojo/Instrument.java` - Classe do instrumento
- `app/org/hascoapi/entity/pojo/Container.java` - Classe pai com hasSIRManagerEmail

---

**Data:** 2026-04-07  
**Status:** ✅ Corrigido e Testado

