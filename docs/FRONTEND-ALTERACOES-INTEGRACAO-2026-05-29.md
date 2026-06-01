# Frontend Integration Changes - 2026-05-29

## Contexto
Durante a integração entre Drupal (módulo rep) e HASCOAPI, apareceram dois sintomas no front:

- erro de rota no grafo (RouteNotFoundException para rep.graph.node)
- erro funcional no describe: "The recovery object is empty or invalid."

Além disso, havia um falso negativo no backend para alguns URIs de workflow/processo que impactava o front ao chamar getUri.

## Alterações feitas no front (Drupal)

### 1) Hardening dos endpoints do grafo no formulário
Arquivo: Drupal módulo rep, formulário de visualização de grafo.

Mudança:

- Foi adicionado fallback para os endpoints usados no drupalSettings:
  - socObjectsEndpoint fallback para /rep/graph/expand
  - nodeInfoEndpoint fallback para /rep/graph/node
- Quando Url::fromRoute(...) falha com RouteNotFoundException, o front não quebra mais a página.
- Em caso de fallback, fica um warning no logger do Drupal em vez de erro fatal.

Objetivo:

- Evitar quebra total da tela quando o registro/cache de rotas estiver inconsistente.
- Permitir que o front continue funcional usando path direto.

Trecho de codigo aplicado:

```php
$socObjectsEndpoint = '/rep/graph/expand';
$nodeInfoEndpoint = '/rep/graph/node';

try {
  $socObjectsEndpoint = Url::fromRoute('rep.graph.expand')->toString();
} catch (\Symfony\Component\Routing\Exception\RouteNotFoundException $e) {
  \Drupal::logger('rep')->warning('Missing route rep.graph.expand; using fallback endpoint path.');
}

try {
  $nodeInfoEndpoint = Url::fromRoute('rep.graph.node')->toString();
} catch (\Symfony\Component\Routing\Exception\RouteNotFoundException $e) {
  \Drupal::logger('rep')->warning('Missing route rep.graph.node; using fallback endpoint path.');
}

$canvas['#attached']['drupalSettings']['rep']['socObjectsEndpoint'] = $socObjectsEndpoint;
$canvas['#attached']['drupalSettings']['rep']['nodeInfoEndpoint'] = $nodeInfoEndpoint;
```

### 2) Correção de contrato no conector getUri
Arquivo: Drupal módulo rep, conector FusekiAPIConnector.

Mudança:

- No fluxo de fallback social do método getUri, o retorno voltou a ser JSON string (envelope legado), em vez de stdClass já decodificado.
- O parseObjectResponse depende desse contrato para desempacotar isSuccessful/body de forma consistente.

Objetivo:

- Eliminar o erro "The recovery object is empty or invalid." em telas como DescribeAssociates.
- Uniformizar o comportamento de getUri entre caminho legado e fallback social.

Trecho de codigo aplicado:

```php
// Keep return type compatible with legacy getUri(): parseObjectResponse expects JSON string envelope.
return $body;
```

## Ajuste backend relacionado (impacta front)

### 3) URIPage - filtro de non-instance
Arquivo: HASCOAPI, URIPage.

Mudança:

- O filtro que tratava "URI terminando com numero" como non-instance foi refinado.
- Agora apenas fragmentos numericos puros (ex.: pmsr#1) sao filtrados.
- URIs validas com caminho apos #, como ...#WKF.../PROC/0001, nao sao mais descartadas.

Objetivo:

- Evitar falso "returned no object from the knowledge graph" para processos/workflows validos.

Trecho de codigo aplicado:

```java
// Version literals: only treat pure numeric fragment identifiers as non-instance.
// Example: pmsr#1, pmsr#2 (but NOT pmsr#WKF.../PROC/0001).
int hashIndex = uri.lastIndexOf('#');
if (hashIndex >= 0 && hashIndex < uri.length() - 1) {
  String fragment = uri.substring(hashIndex + 1);
  if (!fragment.contains("/") && fragment.matches("^\\d+$")) {
    return true;
  }
}
```

## Resultado esperado apos as alteracoes

- O front deixa de cair com RouteNotFoundException no grafo.
- O fluxo de describe deixa de acusar recovery object invalido quando a API retorna envelope valido.
- URIs de processo/workflow passam a resolver corretamente no getUri.

## Checklist de validacao para o front

1. Abrir tela de describe de um workflow/process URI e confirmar que carrega sem mensagem de recovery object invalido.
2. Abrir visualizacao de grafo e confirmar que a tela nao quebra mesmo se houver instabilidade de rota em cache.
3. Verificar no browser network:
   - chamadas para /rep/graph/expand e /rep/graph/node
   - retorno sem erro 500 de rota
4. Validar um URI de processo PMSR (ex.: .../PROC/0001) no fluxo de describe.
5. Confirmar que warnings de fallback (se houver) aparecem apenas no log e nao interrompem a UI.

## Observacoes operacionais

- Se o ambiente Drupal tiver cache antigo, limpar cache apos deploy.
- O ajuste de getUri foi feito para compatibilidade retroativa com parseObjectResponse.
- Mudancas sao de compatibilidade e hardening; nao alteram o contrato funcional esperado do front.
