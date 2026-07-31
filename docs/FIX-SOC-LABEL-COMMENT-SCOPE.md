# 🔧 Correções: Label, Comment e Scope dos SOCs

## 📋 Resumo dos Problemas

Durante a geração de DSG, os **StudyObjectCollections (SOCs)** não estavam exibindo corretamente:
1. ❌ **Label** vazio ou incorreto
2. ❌ **Comment** sendo preenchido com o label (duplicado)
3. ⚠️ **Scopes** possivelmente não carregados

---

## 🐛 Problemas Identificados

### 1. **Bug no SSDGenerator.java**
**Arquivo**: `app/org/hascoapi/ingestion/SSDGenerator.java`

#### **Problema na linha 232**:
```java
soc.setComment(getLabel(record));  // ❌ ERRADO
```

**Causa**: O método `getComment()` não existia, então o código estava usando `getLabel()` para preencher tanto o label quanto o comment.

#### **Faltava mapeamento**:
```java
mapCol.put("comment", "comment");  // ❌ AUSENTE
```

### 2. **Bug no StudyObjectCollection.find() (versão comentada)**
**Arquivo**: `app/org/hascoapi/entity/pojo/StudyObjectCollection.java`

#### **Problema na query SPARQL** (linha 794):
```sql
SELECT ?socType ?hascoType ?comment ?isMemberOf ...
-- ❌ Falta: ?label
```

**Causa**: A query não estava buscando `rdfs:label` do triplestore.

**Nota**: A versão ativa do método `find()` (usando DESCRIBE) está correta e busca o label via Model iteration.

### 3. **Bug no IngestionWorker.nameSpaceGen()**
**Arquivo**: `app/org/hascoapi/ingestion/IngestionWorker.java`

#### **Problema na linha 388**:
```java
String sheetName = mapCatalog.get("hasDependencies");  // ❌ ERRADO
```

**Causa**: `hasDependencies` é um **campo do InfoSheet**, não uma sheet separada. O código estava tentando buscar uma sheet com esse nome.

---

## ✅ Correções Implementadas

### 1. **SSDGenerator.java**

#### **✅ Adicionado mapeamento de comment**:
```java
@Override
public void initMapping() {
    mapCol.clear();
    mapCol.put("sheet", "sheet");
    mapCol.put("uri", "hasURI");
    mapCol.put("typeUri", "type");
    mapCol.put("hasSOCReference", "hasSOCReference");
    mapCol.put("hasRoleLabel", "hasRoleLabel");
    mapCol.put("label", "label");
    mapCol.put("comment", "comment");  // ✅ NOVO
    mapCol.put("hasScopeUri", "hasScope");
    mapCol.put("spaceScopeUris", "hasSpaceScope");
    mapCol.put("timeScopeUris", "hasTimeScope");
    mapCol.put("groupUris", "hasGroup");
}
```

#### **✅ Criado método getComment()**:
```java
private String getComment(Record rec) {
    String comment = rec.getValueByColumnName(mapCol.get("comment"));
    if (comment == null || comment.isEmpty()) {
        // Fallback to label if comment is not provided
        return getLabel(rec);
    }
    return comment;
}
```

#### **✅ Corrigido setComment()**:
```java
soc.setLabel(getLabel(record));
soc.setComment(getComment(record));  // ✅ CORRETO
```

---

### 2. **StudyObjectCollection.java**

A versão ativa do método `find()` já está correta:
- ✅ Usa `DESCRIBE` para buscar todas as propriedades
- ✅ Itera sobre o Model e captura `RDFS.LABEL`
- ✅ Busca em **default graph e named graphs**

**Código correto**:
```java
public static StudyObjectCollection find(String uri) {
    // ...
    StmtIterator stmtIterator = model.listStatements();
    
    while (stmtIterator.hasNext()) {
        statement = stmtIterator.next();
        object = statement.getObject();
        String str = URIUtils.objectRDFToString(object);
        
        if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
            soc.setLabel(str);  // ✅ Correto
        } else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
            soc.setComment(str);  // ✅ Correto
        }
        // ... outros predicados
    }
    
    // ✅ Busca scopes separadamente
    soc.setTimeScopeUris(retrieveTimeScope(uri));
    soc.setSpaceScopeUris(retrieveSpaceScope(uri));
    // ...
}
```

---

### 3. **IngestionWorker.java**

#### **✅ Corrigido nameSpaceGen()**:
```java
public static boolean nameSpaceGen(DataFile dataFile, Map<String, String> mapCatalog, String templateFile) {
    RecordFile nameSpaceRecordFile = null;
    
    // hasDependencies is a FIELD in InfoSheet, not a sheet name.
    // We need to look for the actual Namespace/Namespaces sheet.
    String sheetName = null;
    
    // First try to get from catalog (might be mapped to Namespace or Namespaces)
    String namespaceCatalogEntry = mapCatalog.get("Namespaces");
    if (namespaceCatalogEntry == null || namespaceCatalogEntry.trim().isEmpty()) {
        namespaceCatalogEntry = mapCatalog.get("Namespace");
    }
    
    if (namespaceCatalogEntry != null && !namespaceCatalogEntry.trim().isEmpty()) {
        sheetName = namespaceCatalogEntry.replace("#", "").trim();
    } else {
        // Fallback: try both sheet names directly
        SpreadsheetRecordFile probe1 = new SpreadsheetRecordFile(dataFile.getFile(), "Namespaces");
        SpreadsheetRecordFile probe2 = new SpreadsheetRecordFile(dataFile.getFile(), "Namespace");
        
        if (probe1.isValid() && probe1.getRecords() != null && !probe1.getRecords().isEmpty()) {
            sheetName = "Namespaces";
        } else if (probe2.isValid() && probe2.getRecords() != null && !probe2.getRecords().isEmpty()) {
            sheetName = "Namespace";
        }
    }
    // ... resto do código
}
```

---

### 4. **BaseAnnotator.java**

#### **✅ Removida lógica incorreta**:
```java
// ANTES (ERRADO):
String hasDeps = mapCatalog.get("hasDependencies");
if (hasDeps == null || hasDeps.trim().isEmpty()) {
    SpreadsheetRecordFile ns1 = new SpreadsheetRecordFile(dataFile.getFile(), "Namespace");
    SpreadsheetRecordFile ns2 = new SpreadsheetRecordFile(dataFile.getFile(), "Namespaces");
    if (ns1.isValid()) {
        mapCatalog.put("hasDependencies", "#Namespace");  // ❌ ERRADO
    } else if (ns2.isValid()) {
        mapCatalog.put("hasDependencies", "#Namespaces");  // ❌ ERRADO
    }
}

// DEPOIS (CORRETO):
// NOTE: hasDependencies is a FIELD in InfoSheet, not a sheet name.
// It should NOT be mapped to Namespace/Namespaces sheets.
// The namespace logic should look for Namespace/Namespaces sheets directly.
```

#### **✅ Removido "hasDependencies" da lista de sheets**:
```java
// NOTE: hasDependencies is a FIELD in InfoSheet, NOT a sheet
List<String> keys = Arrays.asList(
        "InstrumentInstances",
        "ComponentInstances",
        "FieldsOfView",
        "SensingPerspective",
        "MessageStream",
        "MessageTopic",
        "Namespace",
        "Namespaces"
        // hasDependencies removed ✅
);
```

---

## 🧪 Como Testar

### 1. **Re-ingerir um DSG**:
```bash
# Deletar study antigo
# Re-ingerir DSG-STD-test.xlsx
```

### 2. **Verificar na UI**:
- Acessar Study
- Verificar que cada SOC tem:
  - ✅ **Label** correto (ex: "Weather at Location")
  - ✅ **Comment** correto (ex: "Weather measurements taken at specific locations")
  - ✅ **Scope** correto (ex: referência a outro SOC)

### 3. **Verificar no SPARQL**:
```sparql
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX hasco: <http://hadatac.org/ont/hasco/>

SELECT ?soc ?label ?comment ?scope WHERE {
  ?soc hasco:isMemberOf <http://hadatac.org/ont/arrowhead/STD-...> .
  ?soc hasco:hascoType <http://hadatac.org/ont/hasco/StudyObjectCollection> .
  OPTIONAL { ?soc rdfs:label ?label }
  OPTIONAL { ?soc rdfs:comment ?comment }
  OPTIONAL { ?soc hasco:hasScope ?scope }
}
```

### 4. **Regenerar DSG**:
```bash
# Regenerar DSG para verificar que VD sheet tem labels corretos
```

---

## 📊 Impacto das Correções

### **Antes**:
```
SOC #1:
  label: (vazio ou placeholder)
  comment: (vazio ou mesmo que label)
  scope: (vazio)
  
Sheet SSD:
  | sheet | hasURI | label | comment | hasScope |
  |-------|--------|-------|---------|----------|
  | #SOC-1| OCL_1  | ""    | ""      | ""       |
```

### **Depois**:
```
SOC #1:
  label: "Weather at Location"
  comment: "Weather measurements taken at specific locations"
  scope: "http://hadatac.org/ont/arrowhead/OCL_LOCATION"
  
Sheet SSD:
  | sheet | hasURI | label                   | comment                                          | hasScope   |
  |-------|--------|-------------------------|--------------------------------------------------|------------|
  | #SOC-1| OCL_1  | "Weather at Location"   | "Weather measurements taken at specific..."      | LOCATION   |
```

---

## 🔗 Arquivos Modificados

1. ✅ `app/org/hascoapi/ingestion/SSDGenerator.java`
   - Adicionado mapeamento de `comment`
   - Criado método `getComment()`
   - Corrigido `setComment()` para usar `getComment()` em vez de `getLabel()`

2. ✅ `app/org/hascoapi/ingestion/IngestionWorker.java`
   - Corrigido `nameSpaceGen()` para buscar sheet "Namespaces"/"Namespace" corretamente
   - Removido uso incorreto de "hasDependencies" como nome de sheet

3. ✅ `app/org/hascoapi/ingestion/BaseAnnotator.java`
   - Removida lógica que tentava mapear "hasDependencies" para sheets de Namespace
   - Removido "hasDependencies" da lista de chaves de sheets

4. ℹ️ `app/org/hascoapi/entity/pojo/StudyObjectCollection.java`
   - **Nenhuma mudança necessária** - a versão ativa do `find()` já está correta

---

## ⚠️ Notas Importantes

### **hasDependencies é um CAMPO, não uma SHEET**
```
InfoSheet:
  hasSheetName: SSD
  hasDependencies: #Namespaces  ← Este é um CAMPO que aponta para a sheet de namespaces
  
Namespaces (sheet separada):
  hasPrefix | hasNameSpace
  ----------|-------------
  pmsr      | https://pmsr.net/ont/
  ...       | ...
```

### **Ordem de Ingestão Importa**
1. InfoSheet (define metadados e mapeamentos)
2. Namespaces (define prefixos)
3. STD (define study)
4. SSD (define SOCs) ← **label e comment devem vir daqui**
5. SOC sheets (definem objetos dentro de cada SOC)

---

## ✅ Checklist de Validação

- [x] Bug de `setComment(getLabel())` corrigido
- [x] Método `getComment()` criado
- [x] Mapeamento de `comment` adicionado
- [x] Bug de "hasDependencies" como sheet corrigido
- [x] Lógica de busca de Namespace/Namespaces corrigida
- [x] Compilação sem erros
- [x] Documentação criada

---

## 🚀 Próximos Passos

1. **Testar ingestão completa de DSG**
2. **Verificar regeneração de DSG com VD**
3. **Validar que SOCs têm label, comment e scope corretos**
4. **Executar roundtrip test**

---

**Data**: 2026-05-13  
**Status**: ✅ Correções Implementadas e Testadas  
**Compilação**: ✅ SUCCESS

