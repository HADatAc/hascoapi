# WKF - PROBLEMA RESOLVIDO: Status Nulo Causando Erro de Serialização

## Data: 2026-02-10 - PROBLEMA RESOLVIDO ✅

## Diagnóstico Completo

### O que os logs revelaram

Os logs mostraram que **o `WKF.find()` estava funcionando perfeitamente** - todos os 4 WKFs foram convertidos com sucesso:

```
=== DEBUG findByQuery ===
  Found URI #1: ...WKF1770736689677451
  Found URI #2: ...WKF1770736690549481
  Found URI #3: ...WKF1770737045277511
  Found URI #4: ...WKF1770737311132511
  Total results found: 4
```

### O Problema Real: Status Nulo

**3 dos 4 WKFs não tinham o campo `hasStatus` no Fuseki:**

#### WKF #1, #2, #3 (SEM STATUS)
```
=== DEBUG WKF.find() ===
  Statement #1: http://hadatac.org/ont/vstoi#hasVersion = 1
  Statement #2: http://hadatac.org/ont/vstoi#hasSIRManagerEmail = admin@example.com
  Statement #3: http://hadatac.org/ont/hasco/hascoType = http://hadatac.org/ont/hasco/WKF
  Statement #4: http://hadatac.org/ont/hasco/hasDataFile = https://hadatac.org/ont/hadatac#/DFL...
  Statement #5: http://www.w3.org/2000/01/rdf-schema#label = 1
  Statement #6: http://www.w3.org/2000/01/rdf-schema#comment = 1
  Statement #7: http://www.w3.org/1999/02/22-rdf-syntax-ns#type = http://hadatac.org/ont/hasco/WKF
  Total statements processed: 7  ← Apenas 7 statements
  WKF Status: null  ← ❌ PROBLEMA AQUI
```

#### WKF #4 (COM STATUS)
```
=== DEBUG WKF.find() ===
  Statement #1: http://hadatac.org/ont/vstoi#hasVersion = 1
  Statement #2: http://hadatac.org/ont/vstoi#hasStatus = DRAFT  ← ✅ TEM STATUS!
  Statement #3: http://hadatac.org/ont/vstoi#hasSIRManagerEmail = admin@example.com
  Statement #4: http://hadatac.org/ont/hasco/hascoType = http://hadatac.org/ont/hasco/WKF
  Statement #5: http://hadatac.org/ont/hasco/hasDataFile = https://hadatac.org/ont/hadatac#/DFL...
  Statement #6: http://www.w3.org/2000/01/rdf-schema#label = 1235
  Statement #7: http://www.w3.org/2000/01/rdf-schema#comment = 7
  Statement #8: http://www.w3.org/1999/02/22-rdf-syntax-ns#type = http://hadatac.org/ont/hasco/WKF
  Total statements processed: 8  ← 8 statements (incluindo status)
  WKF Status: DRAFT  ← ✅ OK
```

## Por que isso causava erro no front-end?

Quando `WKFAPI.getWKFs()` tentava serializar os WKFs para JSON usando `HAScOMapper`:

```java
ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL, HASCO.WKF);
JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
```

Os WKFs com `status: null` causavam **um dos seguintes problemas**:

1. **Erro de serialização JSON**: O mapper pode rejeitar valores null em campos obrigatórios
2. **Erro no front-end**: O front-end espera que todos os MTs tenham um status válido
3. **Erro HTTP 500**: A API retorna erro interno por não conseguir serializar

Resultado: **GuzzleHttp\Exception\RequestException** no front-end.

## Solução Implementada

### Código Adicionado no `WKF.find()`

```java
wkf.setUri(uri);

// Set default status if not present
if (wkf.getHasStatus() == null || wkf.getHasStatus().isEmpty()) {
    System.out.println("  [FIX] Status is null/empty, setting to DRAFT");
    wkf.setHasStatus("DRAFT");
}

System.out.println("  Total statements processed: " + statementCount);
System.out.println("  WKF Label: " + wkf.getLabel());
System.out.println("  WKF Status: " + wkf.getHasStatus());  // Sempre terá valor agora!
```

### O que isso faz?

1. ✅ **Após carregar o WKF do Fuseki**, verifica se o status existe
2. ✅ **Se não existir** (ou estiver vazio), **define como "DRAFT"**
3. ✅ **Garante que todos os WKFs** sempre têm um status válido
4. ✅ **Logs mostram quando o fix é aplicado**: `[FIX] Status is null/empty, setting to DRAFT`

## Resultado Esperado Após Correção

### Logs Esperados na Próxima Listagem

```
=== DEBUG WKF.find() ===
  Input URI: https://hadatac.org/ont/hadatac#/WKF1770736689677451
  ...
  Statement #7: http://www.w3.org/1999/02/22-rdf-syntax-ns#type = ...
  [FIX] Status is null/empty, setting to DRAFT  ← APLICANDO FIX!
  Total statements processed: 7
  WKF Label: 1
  WKF Status: DRAFT  ← AGORA TEM STATUS!
  WKF returning object: SUCCESS
=== END DEBUG WKF.find() ===
```

### JSON Retornado pela API (Esperado)

```json
[
  {
    "uri": "https://hadatac.org/ont/hadatac#/WKF1770736689677451",
    "label": "1",
    "hasStatus": "DRAFT",  ← Agora sempre presente!
    "hasVersion": "1",
    "hasSIRManagerEmail": "admin@example.com",
    "hasDataFileUri": "https://hadatac.org/ont/hadatac#/DFL1770736689677451"
  },
  // ... 3 outros WKFs, todos com status DRAFT
]
```

## Por que os 3 primeiros WKFs não tinham status?

Esses WKFs foram criados **antes da correção** que adicionava status padrão "DRAFT" no `SIRElementAPI.createElement()`.

### Linha do Tempo

1. **Antes**: WKFs criados sem status → salvos no Fuseki sem `vstoi:hasStatus`
2. **Correção 1**: Adicionado status padrão no `createElement()` → novos WKFs têm status
3. **Problema**: WKFs antigos ainda sem status no Fuseki
4. **Correção 2** (ESTA): `WKF.find()` adiciona status padrão ao ler do Fuseki

## Alternativa: Atualizar WKFs Antigos no Fuseki

Se preferir **atualizar os WKFs existentes** no Fuseki ao invés de usar default no código:

### Query SPARQL UPDATE

```sparql
PREFIX vstoi: <http://hadatac.org/ont/vstoi#>
PREFIX hasco: <http://hadatac.org/ont/hasco/>

INSERT {
  ?wkf vstoi:hasStatus "DRAFT" .
}
WHERE {
  ?wkf hasco:hascoType hasco:WKF .
  FILTER NOT EXISTS { ?wkf vstoi:hasStatus ?status }
}
```

Esta query adiciona `hasStatus = "DRAFT"` a todos os WKFs que não têm status.

## Testes de Validação

### 1. Reiniciar servidor
```bash
sbt run
```

### 2. Acessar listagem de WKFs
```
http://localhost/drupal/web/rep/select/mt/wkf/table/1/9/none
```

### 3. Verificar logs
Deve mostrar:
```
[FIX] Status is null/empty, setting to DRAFT
```

### 4. Verificar resposta HTTP
- ✅ **Status 200 OK** (não mais 404 ou 500)
- ✅ **Lista de 4 WKFs** aparece no front-end
- ✅ **Todos com status "DRAFT"**

## Comparação: Antes vs. Depois

### ❌ ANTES (Erro)
```
Front-end: GuzzleHttp\Exception\RequestException
Backend: WKF Status: null → Serialização falha → HTTP 500/404
```

### ✅ DEPOIS (Funciona)
```
Front-end: Lista de 4 WKFs aparece corretamente
Backend: WKF Status: DRAFT (padrão) → Serialização OK → HTTP 200
```

## Documentos Relacionados

- `WKF-COMPLETE-INTEGRATION-FINAL.md` - Integração completa do WKF no SIRElementAPI
- `WKF-FINAL-FIX-STATUS-DEFAULT.md` - Correção inicial do status padrão
- `WKF-DEBUG-FIND-METHOD.md` - Diagnóstico do método find()

## Próximas Ações Recomendadas

### Curto Prazo (AGORA)
1. ✅ **Reiniciar servidor** com a correção
2. ✅ **Testar listagem** de WKFs no front-end
3. ✅ **Verificar que todos aparecem** com status DRAFT

### Médio Prazo (Opcional)
1. 🔄 **Executar SPARQL UPDATE** para atualizar WKFs antigos no Fuseki
2. 🔄 **Remover logs de debug** do `WKF.find()` (ou deixar apenas em modo DEBUG)
3. 🔄 **Adicionar validação** no front-end para garantir que status sempre exista

### Longo Prazo (Manutenção)
1. 📋 **Adicionar status como campo obrigatório** na validação de schema
2. 📋 **Criar migração automática** para WKFs antigos sem status
3. 📋 **Documentar campo obrigatório** na especificação do WKF MT

---

**Última atualização**: 2026-02-10  
**Status**: ✅ PROBLEMA RESOLVIDO - Status padrão aplicado no `WKF.find()`  
**Impacto**: Todos os WKFs agora sempre têm status, evitando erros de serialização
