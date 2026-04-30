# Correcao KGR no branch SDDGen

## Objetivo

Documentar a regressao encontrada no branch `SDDGen` durante a ingestao de ficheiros KGR, a comparacao com `origin/RELEASE_V0.9.3`, as correcoes aplicadas localmente (KGR + media + hardening) e o resultado da validacao funcional.

## Sintoma observado no SDDGen

Ao ingerir ficheiros KGR, o processamento abortava logo na leitura do `InfoSheet` com erros deste tipo:

- `Unexpected sheet key 'verifyUri'`
- `Unexpected sheet key 'hasMediaFolder'`

Na pratica, a cadeia de geradores nao arrancava para os separadores do KGR e a ingestao falhava antes de processar os registos reais.

## Comparacao com origin/RELEASE_V0.9.3

No `origin/RELEASE_V0.9.3` o ficheiro `app/org/hascoapi/utils/MTSheet.java` nao existia. Ou seja, nao havia uma validacao centralizada do catalogo de folhas por tipo de metadata no caminho KGR.

No `SDDGen`, passou a existir um fluxo generico em `BaseAnnotator.loadCatalog(dataFile, mtType)` que valida as chaves do `InfoSheet` contra a lista declarada em `MTSheet`.

O problema concreto e que a lista de chaves esperadas para `Constants.MT_KGR` foi introduzida sem incluir dois parametros legitimos e usados pelo proprio `AnnotateKGR`:

- `hasMediaFolder`
- `verifyUri`

Isto criou uma regressao funcional: ficheiros KGR validos no release deixaram de ser aceites no `SDDGen`.

## Causa raiz

A causa raiz nao estava no `KGRGenerator`, nem no workbook, nem na API do Drupal.

Estava na combinacao destes dois pontos:

1. `BaseAnnotator.loadCatalog(..., Constants.MT_KGR)` passou a fazer validacao estrita das chaves do `InfoSheet`.
2. `MTSheet` passou a declarar um catalogo KGR incompleto, omitindo `hasMediaFolder` e `verifyUri`.

Em resumo: o `SDDGen` introduziu uma validacao nova, mas a especificacao local para KGR ficou desalinhada com o formato real dos ficheiros.

## Correcao aplicada

Foram feitas varias alteracoes locais.

### 1. Atualizacao do catalogo KGR em MTSheet

Em `app/org/hascoapi/utils/MTSheet.java` foram adicionadas as chaves em falta ao catalogo de `Constants.MT_KGR`:

- `hasMediaFolder`
- `verifyUri`

Isto alinha a validacao generica com o formato real dos ficheiros KGR usados na plataforma.

### 2. Hardenizacao do parsing em AnnotateKGR

Em `app/org/hascoapi/ingestion/AnnotateKGR.java` foi ajustado o tratamento dos parametros KGR para evitar falhas ambiguas:

- `hasMediaFolder` passa a ser validado como obrigatorio e nao vazio
- `verifyUri` passa a ser lido com verificacao de `null`
- `verifyUri` passa a aceitar apenas `true` ou `false`, depois de `trim().toLowerCase()`

Esta parte nao corrige a regressao principal sozinha, mas torna a falha futura mais clara e previsivel se o workbook vier mal preenchido.

### 3. Ajuste local do limite de headers no Play

Em `conf/application.conf` foi adicionado:

`play.server.max-header-size=64k`

Isto resolve localmente o erro observado em `localhost:9000` no ambiente Windows quando o browser enviava headers maiores do que o limite default.

### 4. Reducao de ruido de warnings por folhas vazias (KGR)

Foi ajustado o `AnnotateKGR` para nao criar geradores para folhas KGR vazias.

Isto evita o warning repetido `GBL_00040` causado por geradores criados com `0 records`.

### 5. Hardening do endpoint de ingestao contra corrupcao de workbooks

Foi ajustado o endpoint de ingestao (`IngestionAPI.ingest`) para evitar que requests JSON (por exemplo `POST` com body `{}`) sejam tratados como upload de ficheiro, o que pode sobrescrever/truncar o workbook em `/var/hascoapi/resources/...`.

O comportamento correto e:

- so considerar upload de ficheiro quando o `Content-Type` indica upload real (e.g. `multipart/form-data` ou `application/octet-stream`)
- caso contrario, usar o workbook ja existente em resources

### 6. Fixes de media (countries + organizations)

Durante a validacao ficou claro que muitos `KGR_00021` eram causados por problemas de disponibilizacao/extraçao de media, nao por falhas de RDF.

As correcoes feitas foram:

1) **Persistencia de recursos/media no Docker**

Por defeito, rebuild/restart do container apagava:

- `/var/hascoapi/resources` (workbooks e media copiado por URI)
- `/var/hascoapi/media` (zips extraidos)

Foi atualizado o `docker-compose.yml` para montar volumes dedicados (`hascoapi-resources`, `hascoapi-media`).

2) **Upload de media por octet-stream (Windows/PowerShell 5.1)**

No ambiente local, o parsing de `multipart/form-data` nem sempre devolve `asMultipartFormData()` no Play.

O upload que funcionou de forma consistente foi:

```bash
curl -H "Content-Type: application/octet-stream" \
	--data-binary "@countries.zip" \
	http://localhost:9000/hascoapi/api/uploadMedia/countries/countries.zip

curl -H "Content-Type: application/octet-stream" \
	--data-binary "@organizations.zip" \
	http://localhost:9000/hascoapi/api/uploadMedia/organizations/organizations.zip
```

3) **Extracao de zip com prefixo de pasta duplicado**

Foi corrigido o `DataFileAPI.unzipAndSave()` para evitar extraçoes do tipo:

`/var/hascoapi/media/organizations/organizations/PIAGET.png`

quando o zip tem entradas como `organizations/PIAGET.png`.

O fix “achata” o primeiro diretorio quando coincide com o nome do `foldername` e inclui uma protecao basica contra path traversal.

4) **Content-Type correto ao servir imagens**

O `DataFileAPI.downloadFile()` passou a devolver `image/png`, `image/jpeg`, etc., baseado na extensao.

Isto evita problemas no browser quando existe `X-Content-Type-Options: nosniff` e o upstream devolve `application/octet-stream`.

5) **Aliases de media (legacy + typos)**

Foi mantida tolerancia para casos legacy (countries):

- `gz.png -> ps.png`
- `an.png -> aw.png`

E foi adicionado suporte a um typo real observado no workbook de institutos:

- `IPBEJApng -> IPBEJA.png`

## Validacao local executada

### API / container

Foi feito rebuild e restart do servico:

`docker compose up -d --build hascoapi`

Depois disso:

- o health check respondeu com sucesso
- o endpoint do Play passou a aceitar um header artificial de 9 KB, devolvendo `200`

### Teste funcional KGR end-to-end

Foi executado um teste direto por API, sem depender do UI do Drupal, com o ficheiro:

- `KGR-COUNTRIES-URI.xlsx`

E tambem (para validar logos de organizations):

- `KGR-INSTITUTOS-URI.xlsx`

Fluxo testado:

1. criacao de `DataFile`
2. criacao de `KGR`
3. upload do workbook
4. `POST /hascoapi/api/ingest/UNDER_REVIEW/kgr/{kgrUri}`
5. leitura do log de ingestao do `DataFile`

## Resultado da validacao

O comportamento esperado foi confirmado.

Os erros antigos deixaram de aparecer:

- deixou de aparecer a rejeicao de `verifyUri`
- deixou de aparecer a rejeicao de `hasMediaFolder`
- deixou de aparecer a falha imediata do `InfoSheet`

O processamento avancou para os registos reais do workbook e o log terminou com:

- `2682 triple(s) have been committed to triple store`

Isto valida que a regressao especifica do `SDDGen` ficou corrigida.

### Resultado de media (Countries)

- `DFL` terminou em `PROCESSED`
- `KGR_00021_COUNT=0`
- flags copiados para `/var/hascoapi/resources/<PLC...>/<flag>.png`

### Resultado de media (Institutos / Organizations)

Depois de corrigir a extraçao do zip de organizations e relancar a ingestao do KGR `Institutos`, foi confirmado que:

- o logo `PIAGET.png` foi copiado para `/var/hascoapi/resources/ORG174547834502888794/PIAGET.png`
- o endpoint `downloadFile` devolve `200` e `Content-Type: image/png` para esse logo

Ainda podem existir `KGR_00021` para logos que nao existem no zip fornecido.
Esses warnings indicam **ficheiros realmente ausentes** (nao um bug de ingestao).

## Observacao importante

Durante a mesma validacao apareceram varios erros `KGR_00021` para ficheiros de media em caminhos como:

- `/var/hascoapi/media/countries/af.png`

Isto indica um problema diferente, ja a jusante da regressao principal: os assets de media esperados pelo KGR nao existem dentro do container no caminho configurado.

Esse ponto nao invalida a correcao principal, porque a ingestao prosseguiu e os triples foram efetivamente gravados.

No entanto, se o objetivo for ingestao completa com imagens:

- o media folder tem de existir no runtime (`/var/hascoapi/media/<folder>`)
- o zip tem de conter os ficheiros com os nomes exatamente como aparecem no workbook (Linux e case-sensitive)

Um metodo rapido para levantar a lista de ficheiros em falta e filtrar unicos e extrair os nomes diretamente do `DataFile.log` (ex.: por regex nas linhas `KGR_00021`).

## Conclusao

O problema do `SDDGen` era uma regressao de validacao introduzida pela combinacao de `BaseAnnotator` com `MTSheet`.

O release antigo aceitava estes ficheiros porque nao fazia esta validacao centralizada para KGR. O `SDDGen` passou a faze-la, mas com uma lista incompleta de chaves validas.

A correcao local foi:

- alinhar `MTSheet` com o formato real dos ficheiros KGR
- tornar `AnnotateKGR` mais robusto na leitura dos parametros
- aumentar o limite de headers do Play para o ambiente local Windows

Com estas alteracoes, a ingestao KGR voltou a funcionar localmente no `SDDGen` e o pipeline de media ficou estavel (com persistencia em volumes), permitindo que o UI do Drupal apresente flags/logos quando os ficheiros existem no zip fornecido.