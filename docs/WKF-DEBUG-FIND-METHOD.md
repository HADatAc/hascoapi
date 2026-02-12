# WKF - Debug Final: Problema de Conversão URI → Objeto

## Data: 2026-02-10 - DEBUG FINAL

## Problema Identificado

O backend encontra **4 WKFs no Fuseki** (logs mostram as 4 URIs), mas o front-end recebe um **erro HTTP** (GuzzleHttp\Exception\RequestException).

### Logs do Backend (Sucessos)
```
=== DEBUG findMTInstancesByManagerEmailWithPages ===
  Class: class org.hascoapi.entity.pojo.WKF
  hascoTypeStr: hasco:WKF
  Results found: 4

=== DEBUG findByQuery ===
  Found URI #1: https://hadatac.org/ont/hadatac#/WKF1770736689677451
  Found URI #2: https://hadatac.org/ont/hadatac#/WKF1770736690549481
  Found URI #3: https://hadatac.org/ont/hadatac#/WKF1770737045277511
  Found URI #4: https://hadatac.org/ont/hadatac#/WKF1770737311132511
  Total results found: 4
```

### Problema Crítico

O método `GenericFind.findByQuery()` encontra 4 URIs, mas para cada URI ele chama:
```java
T element = findElement(clazz, uri);  // Linha 1043
```

Que por sua vez chama:
```java
} else if (clazz == WKF.class) {
    return (T)WKF.find(uri);  // Linha 1166-1167
}
```

O método `WKF.find(uri)` está **retornando null** para todas as 4 URIs, resultando em uma **lista vazia** sendo retornada ao `WKFAPI.getWKFs()`.

## Hipótese do Problema

O método `WKF.find(uri)` usa uma query `DESCRIBE` para buscar os triples do WKF:

```java
String queryString = "DESCRIBE <" + cleanUri + ">";
Model model = SPARQLUtils.describe(..., queryString);
```

Há duas possibilidades:

### 1. Named Graph Problem
Os WKFs podem estar armazenados em um **named graph** específico, mas a query `DESCRIBE` está buscando no **default graph**. Isso faria a query retornar 0 triples, resultando em `WKF.find()` retornando `null`.

### 2. Namespace Problem
Os triples do WKF podem estar usando prefixos diferentes do esperado, fazendo com que o método `WKF.find()` não reconheça os predicados e não preencha os campos do objeto.

## Solução Implementada: Logs de Debug

Adicionei logs detalhados no método `WKF.find()` para diagnosticar exatamente o que está acontecendo:

### Logs Adicionados

```java
public static WKF find(String uri) {
    System.out.println("=== DEBUG WKF.find() ===");
    System.out.println("  Input URI: " + uri);
    System.out.println("  Clean URI: " + cleanUri);
    System.out.println("  DESCRIBE Query: " + queryString);
    System.out.println("  Model has statements: " + stmtIterator.hasNext());
    
    // Se model está vazio
    if (!stmtIterator.hasNext()) {
        System.out.println("  No statements found in model, returning null");
        return null;
    }
    
    // Para cada statement processado
    while (stmtIterator.hasNext()) {
        System.out.println("  Statement #X: " + predicate + " = " + value);
    }
    
    System.out.println("  Total statements processed: " + statementCount);
    System.out.println("  WKF Label: " + wkf.getLabel());
    System.out.println("  WKF Status: " + wkf.getHasStatus());
    System.out.println("  WKF returning object: SUCCESS/NULL");
    System.out.println("=== END DEBUG WKF.find() ===");
}
```

## Próximos Passos para Diagnóstico

1. **Reiniciar servidor**: `sbt run`

2. **Acessar listagem de WKFs**: `http://localhost/drupal/web/rep/select/mt/wkf/table/1/9/none`

3. **Verificar logs** no console:

### Cenário 1: Model está vazio
```
=== DEBUG WKF.find() ===
  Input URI: https://hadatac.org/ont/hadatac#/WKF1770736689677451
  Clean URI: https://hadatac.org/ont/hadatac#/WKF1770736689677451
  DESCRIBE Query: DESCRIBE <https://hadatac.org/ont/hadatac#/WKF1770736689677451>
  Model has statements: false
  No statements found in model, returning null
=== END DEBUG WKF.find() ===
```

**Solução**: Modificar a query para buscar no named graph correto:
```java
String queryString = "DESCRIBE <" + cleanUri + "> FROM <https://hadatac.org/ont/hadatac#/DFL...>";
```

### Cenário 2: Model tem statements, mas não são reconhecidos
```
=== DEBUG WKF.find() ===
  Input URI: https://hadatac.org/ont/hadatac#/WKF1770736689677451
  Model has statements: true
  Statement #1: http://www.w3.org/1999/02/22-rdf-syntax-ns#type = http://hadatac.org/ont/hasco/WKF
  Statement #2: http://hadatac.org/ont/hasco/hascoType = http://hadatac.org/ont/hasco/WKF
  Statement #3: http://www.w3.org/2000/01/rdf-schema#label = 1235
  Statement #4: http://hadatac.org/ont/hasco/hasDataFile = https://hadatac.org/ont/hadatac#/DFL...
  Statement #5: http://hadatac.org/ont/vstoi#hasStatus = DRAFT
  Statement #6: http://hadatac.org/ont/vstoi#hasSIRManagerEmail = admin@example.com
  Total statements processed: 6
  WKF Label: 1235
  WKF Status: DRAFT
  WKF returning object: SUCCESS
=== END DEBUG WKF.find() ===
```

**Solução**: Neste caso, o `WKF.find()` está funcionando! O problema seria em outro lugar (ex: serialização JSON).

### Cenário 3: Model tem statements, mas predicados não batem
```
=== DEBUG WKF.find() ===
  Model has statements: true
  Statement #1: http://UNEXPECTED_NAMESPACE/type = ...
  Statement #2: http://UNEXPECTED_NAMESPACE/hascoType = ...
  Total statements processed: 6
  WKF Label: null  ← Não preencheu!
  WKF Status: null  ← Não preencheu!
  WKF returning object: SUCCESS (mas vazio)
=== END DEBUG WKF.find() ===
```

**Solução**: Adicionar os predicados corretos no método `WKF.find()`.

## Comparação com INS (que funciona)

Verificar como o `INS.find()` funciona e comparar com `WKF.find()` para identificar diferenças.

## Possíveis Soluções (após diagnóstico)

### Se Named Graph é o problema:
```java
// Opção 1: Buscar no named graph específico
String namedGraph = wkf.getHasDataFileUri();  // Usar o DataFile URI como named graph
String queryString = "DESCRIBE <" + cleanUri + "> FROM <" + namedGraph + ">";

// Opção 2: Usar SPARQL UPDATE para mover para default graph
String updateQuery = "INSERT { <" + uri + "> ?p ?o } WHERE { GRAPH <...> { <" + uri + "> ?p ?o } }";
```

### Se Predicados estão diferentes:
```java
// Adicionar logs para ver quais predicados estão vindo
System.out.println("  Predicate URI: " + statement.getPredicate().getURI());
System.out.println("  Expected RDFS.label: " + RDFS.label.getURI());
System.out.println("  Match: " + statement.getPredicate().getURI().equals(RDFS.label.getURI()));
```

## Documentos Relacionados

- `WKF-COMPLETE-INTEGRATION-FINAL.md` - Integração completa do WKF no SIRElementAPI
- `WKF-FINAL-FIX-STATUS-DEFAULT.md` - Correção do status padrão DRAFT
- `WKF-DEBUG-LISTING-ISSUE.md` - Problema de listagem inicial

---

**Última atualização**: 2026-02-10  
**Status**: 🔍 DIAGNÓSTICO EM ANDAMENTO - Aguardando logs do `WKF.find()`
