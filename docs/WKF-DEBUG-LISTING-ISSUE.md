# Debug do Problema de Listagem do WKF

## Data: 2026-02-10

## Problema Atual

Os **WKFs estão sendo salvos corretamente no Fuseki** (logs mostram "Total triples in model: 7"), mas **não aparecem na listagem** em `http://localhost/drupal/web/rep/select/mt/wkf/table/1/9/none`.

## Logs Adicionados para Diagnóstico

Adicionei **logs de debug completos** em 2 métodos críticos:

### 1. `findMTInstancesByManagerEmailWithPages()`
Este método constrói a query SPARQL para buscar WKFs:

```java
System.out.println("=== DEBUG findMTInstancesByManagerEmailWithPages ===");
System.out.println("  Class: " + clazz);
System.out.println("  hascoTypeStr: " + hascoTypeStr);  // Deve ser "hasco:WKF"
System.out.println("  managerEmail: " + managerEmail);
System.out.println("  pageSize: " + pageSize);
System.out.println("  offset: " + offset);
System.out.println("  SPARQL Query: " + queryString);  // Query completa
System.out.println("  Results found: " + (results != null ? results.size() : "null"));
System.out.println("=== END DEBUG ===");
```

### 2. `findByQuery()`
Este método executa a query no Fuseki e processa os resultados:

```java
System.out.println("=== DEBUG findByQuery ===");
System.out.println("  Class: " + clazz);
System.out.println("  Query: " + queryString);
System.out.println("  ResultSet hasNext: " + resultsrw.hasNext());
// Para cada resultado:
System.out.println("  Found URI #" + count + ": " + uri);
System.out.println("  Total results found: " + list.size());
System.out.println("=== END DEBUG findByQuery ===");
```

## Próximos Passos para Diagnóstico

### 1. Reinicie o Servidor
```bash
cd C:\Users\kaell\Desktop\Project\hascoapi
sbt run
```

### 2. Acesse a Página de Listagem
```
http://localhost/drupal/web/rep/select/mt/wkf/table/1/9/none
```

### 3. Analise os Logs no Console

Você verá algo assim:

#### Cenário A: classNameWithNamespace retorna NULL
```
=== DEBUG findMTInstancesByManagerEmailWithPages ===
  Class: class org.hascoapi.entity.pojo.WKF
  hascoTypeStr: null  ← PROBLEMA: deveria ser "hasco:WKF"
  managerEmail: admin@example.com
  pageSize: 9
  offset: 0
```

**Solução**: O `classNameWithNamespace(WKF.class)` não está funcionando.

#### Cenário B: Query não retorna resultados
```
=== DEBUG findMTInstancesByManagerEmailWithPages ===
  Class: class org.hascoapi.entity.pojo.WKF
  hascoTypeStr: hasco:WKF  ← OK
  managerEmail: admin@example.com
  SPARQL Query: PREFIX hasco: <...> SELECT ?uri WHERE { ... }
  
=== DEBUG findByQuery ===
  ResultSet hasNext: false  ← PROBLEMA: Fuseki não retornou nada
  No results found from SPARQL query
```

**Possíveis causas**:
- Os dados não estão realmente no Fuseki
- O named graph está errado
- A query SPARQL está mal formada

#### Cenário C: Query retorna resultados mas WKF.find() falha
```
=== DEBUG findByQuery ===
  ResultSet hasNext: true
  Found URI #1: https://hadatac.org/ont/hadatac#/WKF1770736689677451
  Found URI #2: https://hadatac.org/ont/hadatac#/WKF1770736690549481
  Total results found: 2  ← OK: 2 URIs encontrados
  
=== DEBUG findMTInstancesByManagerEmailWithPages ===
  Results found: 0  ← PROBLEMA: WKF.find() retornou null para ambos
```

**Solução**: O `WKF.find()` não está conseguindo recuperar os dados do Fuseki.

## Verificação Manual no Fuseki

Enquanto isso, você pode verificar manualmente se os WKFs estão no Fuseki:

### 1. Acesse o Fuseki
```
http://localhost:3030
```

### 2. Execute esta Query
```sparql
PREFIX hasco: <http://hadatac.org/ont/hasco/>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX vstoi: <http://hadatac.org/ont/vstoi#>

SELECT ?uri ?label ?version ?dataFile ?email
WHERE {
  ?uri hasco:hascoType hasco:WKF .
  ?uri rdfs:label ?label .
  OPTIONAL { ?uri vstoi:hasVersion ?version }
  OPTIONAL { ?uri hasco:hasDataFile ?dataFile }
  OPTIONAL { ?uri vstoi:hasSIRManagerEmail ?email }
}
```

### Resultados Esperados
Se os WKFs estão salvos corretamente, você deve ver:
```
uri                                                    | label | version | dataFile                                              | email
------------------------------------------------------ | ----- | ------- | ----------------------------------------------------- | ------------------
https://hadatac.org/ont/hadatac#/WKF1770736689677451  | 1     | 1       | https://hadatac.org/ont/hadatac#/DFL1770736689677451 | admin@example.com
https://hadatac.org/ont/hadatac#/WKF1770736690549481  | 1     | 1       | https://hadatac.org/ont/hadatac#/DFL1770736690549481 | admin@example.com
```

## Possíveis Problemas e Soluções

### Problema 1: Named Graph Incorreto
**Sintoma**: Query manual no Fuseki retorna vazio, mas logs mostram "7 triples"

**Causa**: Os dados estão sendo salvos em um named graph diferente do que a query está buscando.

**Solução**: Verificar em qual named graph os dados estão:
```sparql
SELECT DISTINCT ?g
WHERE {
  GRAPH ?g {
    ?s ?p ?o .
    FILTER(CONTAINS(STR(?s), "WKF"))
  }
}
```

### Problema 2: Fuseki Não Está Atualizando
**Sintoma**: Query manual no Fuseki retorna vazio

**Causa**: O Fuseki pode estar com cache ou não recebeu os dados.

**Solução**: 
1. Reinicie o Fuseki
2. Verifique os logs do Fuseki

### Problema 3: WKF.find() Não Funciona
**Sintoma**: Query retorna URIs, mas `WKF.find()` retorna null

**Causa**: O método `WKF.find()` pode ter um bug ou estar buscando no lugar errado.

**Solução**: Adicionar logs no `WKF.find()` (arquivo `WKF.java`).

## Próxima Ação

**IMPORTANTE**: Após reiniciar o servidor, **copie e cole TODOS os logs** que aparecerem quando você acessar a página de listagem do WKF. Os logs começarão com:

```
=== DEBUG findMTInstancesByManagerEmailWithPages ===
```

Com esses logs, poderemos identificar exatamente onde o problema está ocorrendo.

---

**Checklist**:
- [ ] Servidor reiniciado
- [ ] Página de listagem acessada
- [ ] Logs copiados
- [ ] Query manual no Fuseki executada
- [ ] Resultados compartilhados
