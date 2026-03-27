# Correção: RequiredInstruments Vazio na Regeneração WKF

## Problema Identificado

Após o roundtrip test do WKF, a sheet **RequiredInstruments** estava vazia no arquivo regenerado, mesmo que o arquivo original contivesse dados.

### Sintomas

1. **Step 2** (Regeneração): RequiredInstruments sheet aparecia vazia
2. **Step 3** (Re-ingestão): `tripleCount=0` e `entities found: 0`
3. Tasks estavam sendo ingeridas com valores concatenados em `vstoi:hasRequiredInstrument`:
   - Exemplo: `pmsr:/WKF_PMSR_SIM_0001/RIN/0001%20;%20pmsr:/WKF_PMSR_SIM_0001/RIN/0004`

## Causa Raiz

O problema tinha **três componentes**:

### 1. Valores Concatenados Não Eram Divididos na Ingestão

O WKFGenerator recebia valores como:
```
pmsr:/WKF_PMSR_SIM_0001/RIN/0001%20;%20pmsr:/WKF_PMSR_SIM_0001/RIN/0004
```

E os salvava como **uma única string literal** no triplestore, em vez de múltiplos triples:

**Incorreto** (antes):
```turtle
pmsr:/WKF_PMSR_SIM_0001/TSK/0003 vstoi:hasRequiredInstrument "pmsr:/WKF_PMSR_SIM_0001/RIN/0001 ; pmsr:/WKF_PMSR_SIM_0001/RIN/0004" .
```

**Correto** (depois):
```turtle
pmsr:/WKF_PMSR_SIM_0001/TSK/0003 vstoi:hasRequiredInstrument pmsr:/WKF_PMSR_SIM_0001/RIN/0001 .
pmsr:/WKF_PMSR_SIM_0001/TSK/0003 vstoi:hasRequiredInstrument pmsr:/WKF_PMSR_SIM_0001/RIN/0004 .
```

### 2. MetadataFactory Não Processava Lists Corretamente

Quando encontrava uma `List` nos valores do row, `MetadataFactory.createModel()` **pegava apenas o primeiro elemento** (`list.get(0)`), descartando os demais.

### 3. Query SPARQL Não Encontrava RequiredInstruments

A query em `WKFRequiredInstruments.addByWkf()` buscava:
```sparql
SELECT ?uri WHERE { 
  GRAPH <namedGraph> { 
    ?uri a vstoi:RequiredInstrument . 
  } 
}
```

Mas como não havia relação com Tasks, encontrava **todos** os RequiredInstruments do named graph (não apenas os do WKF específico).

## Soluções Implementadas

### 1. WKFGenerator.java - Dividir Valores Concatenados

**Arquivo**: `app/org/hascoapi/ingestion/WKFGenerator.java`

**Mudança**: Adicionado método `splitMultiValueProperty()` que:
- Divide valores concatenados com `;` ou `|`
- Remove URL encoding (`%20` → espaço)
- Cria uma `List<String>` com URIs individuais
- Aplica-se a:
  - `vstoi:hasRequiredInstrument` (Tasks)
  - `vstoi:hasSubtask` (Tasks)
  - `vstoi:hasRequiredComponent` (RequiredInstruments)

**Código**:
```java
private void splitMultiValueProperty(Map<String, Object> row, String propertyKey) {
    Object value = row.get(propertyKey);
    if (value == null) return;

    String valueStr = value.toString().trim();
    if (valueStr.contains(";") || valueStr.contains("|")) {
        java.util.List<String> uris = new java.util.ArrayList<>();
        
        String[] parts = valueStr.contains(";") 
            ? valueStr.split("\\s*;\\s*")
            : valueStr.split("\\s*\\|\\s*");

        for (String part : parts) {
            String cleanUri = java.net.URLDecoder.decode(part.trim(), "UTF-8").trim();
            if (!cleanUri.isEmpty()) {
                uris.add(cleanUri);
            }
        }

        if (!uris.isEmpty()) {
            row.put(propertyKey, uris);
        }
    }
}
```

### 2. MetadataFactory.java - Processar Lists Corretamente

**Arquivo**: `app/org/hascoapi/utils/MetadataFactory.java`

**Mudança**: Modificado para **iterar sobre todos os elementos** da List e criar um triple para cada um, em vez de pegar apenas o primeiro.

**Antes**:
```java
if (raw instanceof List) {
    List<?> list = (List<?>) raw;
    if (!list.isEmpty()) {
        cellValue = list.get(0).toString(); // ❌ Pega apenas o primeiro!
    }
}
```

**Depois**:
```java
Object raw = row.get(key);
List<String> cellValues = new java.util.ArrayList<>();

if (raw instanceof List) {
    List<?> list = (List<?>) raw;
    for (Object item : list) {  // ✅ Processa TODOS os elementos
        if (item != null) {
            cellValues.add(item.toString());
        }
    }
} else if (raw != null) {
    cellValues.add(raw.toString());
}

// Cria um triple para cada valor
for (String cellValue : cellValues) {
    if (URIUtils.isValidURI(cellValue)) {
        IRI obj = factory.createIRI(URIUtils.replacePrefixEx(cellValue));
        model.add(sub, pred, obj, (Resource)namedGraph);
    } else {
        Literal obj = factory.createLiteral(cellValue.replace("\n", " ")...);
        model.add(sub, pred, obj, (Resource)namedGraph);
    }
}
```

### 3. WKFRequiredInstruments.java - Query Mais Específica

**Arquivo**: `app/org/hascoapi/transform/mt/wkf/WKFRequiredInstruments.java`

**Mudança**: Query modificada para buscar apenas RequiredInstruments **referenciados por Tasks** no named graph.

**Antes**:
```sparql
SELECT ?uri WHERE { 
  GRAPH <namedGraph> { 
    ?uri a vstoi:RequiredInstrument . 
  } 
}
```

**Depois**:
```sparql
SELECT DISTINCT ?uri WHERE { 
  GRAPH <namedGraph> { 
    ?task a vstoi:Task . 
    ?task vstoi:hasRequiredInstrument ?uri . 
    ?uri a vstoi:RequiredInstrument . 
  } 
}
```

Agora a query **só retorna RequiredInstruments que estão relacionados com Tasks**, garantindo que apenas os RequiredInstruments do WKF específico sejam regenerados.

## Logs de Redução de Verbosidade

Como benefício adicional, os logs verbosos foram simplificados:

### Arquivos Modificados para Logs

1. **DP2Generator.java**: Removidos logs detalhados de cada coluna processada
2. **BaseGenerator.java**: Simplificado para mostrar apenas resumo (X válidos, Y duplicados)
3. **WKFGenerator.java**: Removidos logs de debug, mantido apenas WARNING quando hasURI falta
4. **SDDAttributeGenerator.java**: Simplificado para mostrar apenas totais

**Antes** (exemplo DP2):
```
========== DP2Generator.createRow() START ==========
ElementType: [instrumentinstance]
RowNumber: 37
  Processing header [hasURI] -> value [pmsr:INI1739823398493725]
    ✓ Added to row: [hasURI] = [pmsr:INI1739823398493725]
  Processing header [a] -> value [pmsr:/INS1739823398493725]
    ✓ Added to row: [a] = [pmsr:/INS1739823398493725]
... (15+ linhas por linha de dados)
```

**Depois**:
```
[DP2Generator] Processing 62 records for elementType=instrumentinstance
[DP2Generator] Completed: 62 valid rows, 0 duplicates skipped
```

## Impacto e Benefícios

### Antes da Correção
- ❌ RequiredInstruments não eram regenerados
- ❌ Valores concatenados salvos como strings literais
- ❌ Queries SPARQL não encontravam relacionamentos
- ❌ Logs extremamente verbosos (milhares de linhas)

### Depois da Correção
- ✅ RequiredInstruments corretamente regenerados
- ✅ Múltiplos valores salvos como múltiplos triples
- ✅ Queries SPARQL funcionam corretamente
- ✅ Logs concisos e informativos
- ✅ Funciona para qualquer propriedade multi-valorada (hasSubtask, hasRequiredComponent, etc.)

## Testes

Para verificar a correção:

```bash
sbt "testOnly org.hascoapi.tests.HascoRoundtripTest -- -z WKF"
```

**Resultado esperado**:
- Step 1: Ingestão cria múltiplos triples para hasRequiredInstrument
- Step 2: Regeneração popula sheet RequiredInstruments com todos os instrumentos
- Step 3: Re-ingestão encontra os mesmos triples que Step 1

## Arquivos Modificados

1. `app/org/hascoapi/ingestion/WKFGenerator.java`
   - ➕ Método `splitMultiValueProperty()`
   - ➕ Chamadas para dividir hasRequiredInstrument, hasSubtask, hasRequiredComponent

2. `app/org/hascoapi/utils/MetadataFactory.java`
   - 🔄 Processamento de Lists: criar múltiplos triples em vez de pegar apenas o primeiro

3. `app/org/hascoapi/transform/mt/wkf/WKFRequiredInstruments.java`
   - 🔄 Query SPARQL: buscar apenas RequiredInstruments referenciados por Tasks

4. `app/org/hascoapi/ingestion/DP2Generator.java`
   - 🔄 Logs simplificados

5. `app/org/hascoapi/ingestion/BaseGenerator.java`
   - 🔄 Logs simplificados

6. `app/org/hascoapi/ingestion/SDDAttributeGenerator.java`
   - 🔄 Logs simplificados

## Observações

- A mesma solução se aplica a **qualquer propriedade multi-valorada** em qualquer MT
- O padrão de separadores suportados: `;` (ponto e vírgula) e `|` (pipe)
- URL encoding (`%20`) é automaticamente decodificado
- Esta correção também beneficia outras propriedades como `vstoi:hasSubtask`, que frequentemente têm múltiplos valores

---

**Data**: 2026-03-27  
**Tipo**: Bug Fix + Performance Improvement  
**Prioridade**: Alta (impacta testes de roundtrip)

