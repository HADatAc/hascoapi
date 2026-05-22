# PORT: `fix_nuno_SDDGen` → `INStoDSG` (HASCOAPI)

Data: **2026-05-16**  
Autor: Nuno + notas geradas a partir do branch `fix_nuno_SDDGen`

## Objetivo

Este documento descreve **exatamente** como portar para o branch **`INStoDSG` (GitHub)** todas as alterações que foram feitas no branch temporário **`fix_nuno_SDDGen`**, para suportar o que o front‑end precisava e que a API não tinha / tinha com bugs.

O foco é **integração segura** (sem “partir” o trabalho existente), usando **cherry-pick** (recomendado) e uma secção de **resolução de conflitos + checklist de validação**.

## Contexto de comparação (referência)

Este guia foi preparado com base nestas referências locais após `git fetch`:

- `origin/INStoDSG` → **`1f8dcae`** (estado no GitHub no momento da comparação)
- `fix_nuno_SDDGen` → **`932adc6`**
- merge-base (`git merge-base origin/INStoDSG fix_nuno_SDDGen`) → **`2fbbf431`**

> Nota: Se entretanto o branch `INStoDSG` no GitHub tiver avançado, repete o `git fetch` e usa o mesmo processo. O importante é cherry-pickar os commits do `fix_nuno_SDDGen` (ou portar manualmente os mesmos deltas).

## Alterações incluídas (escopo)

Confirmado como **obrigatório** para o front‑end:

- WKF geração/ingestão (Excel → TTL)
- SDDGen/KGR ingest + media pipeline
- Downloads/rotas do `DataFileAPI` (inclui hardening contra path traversal)
- Fixes INS namespaces/`INSGen`
- Ajustes em Task/RequiredInstruments URIs
- Testes/ficheiros TTL gerados (roundtrip)

## Método recomendado: cherry-pick (passo a passo)

### 0) Pré‑requisitos (antes de mexer)

1. Working tree limpo:
   - `git status` deve estar limpo (sem alterações por commit).
2. Ter remotes atualizados:
   - `git fetch origin --prune`
3. Garantir que estás mesmo a partir do `INStoDSG` do GitHub:
   - **Recomendado:** trabalhar diretamente a partir de `origin/INStoDSG`.

### 1) Criar um branch de integração

```bash
git fetch origin --prune

# Criar branch novo a partir do INStoDSG do GitHub
git checkout -b port_fix_nuno_SDDGen origin/INStoDSG

# Garantir que tens o branch de origem das alterações
git fetch origin fix_nuno_SDDGen
```

### 2) Cherry-pick dos commits (por ordem)

Aplicar **por ordem** (do mais antigo para o mais recente):

```bash
git cherry-pick \
  71c4aea \
  104b769 \
  18893dd \
  6730035 \
  11ea5cd \
  9bda824 \
  449c340 \
  cac4546 \
  7ec3b0c \
  5192ce0 \
  6bad5c7 \
  cdad66d \
  932adc6
```

Se houver conflitos:

- ver estado: `git status`
- resolver conflitos nos ficheiros assinalados
- marcar como resolvido: `git add <ficheiros>`
- continuar: `git cherry-pick --continue`
- se precisares de abortar: `git cherry-pick --abort`

### 3) Se não quiseres cherry-pick “commit a commit”

Alternativa (range desde o merge-base) — útil quando o histórico é linear:

```bash
# (merge-base conhecido deste port) 2fbbf431...
# aplica todos os commits depois do merge-base até ao topo do fix_nuno_SDDGen
git cherry-pick 2fbbf431..origin/fix_nuno_SDDGen
```

## Inventário por commit (o que cada um traz)

Esta secção serve para:

- perceber **porquê** cada commit existe
- saber **onde** vão ocorrer conflitos
- saber **o que validar** se tiveres de resolver conflitos manualmente

### 71c4aea — WKF ingestion fix (2026-03-26)

Áreas tocadas:

- Upload/ingest: `DataFileAPI`, `IngestionAPI`
- Resolução de URI: `URIPage`, `URIUtils`
- Docs: `docs/ALTERACOES_WKF_INGEST.md`, `docs/WKF-INGESTION-FIX-SUMMARY.md`, `docs/WKF-INGESTION-WORKFLOW.md`, `docs/BACKEND-VERIFICATION-REPORT.md`
- Testes: `HascoRoundtripTest` + TTLs gerados

Pontos críticos (comportamento esperado após integrar):

- Ingestão (`IngestionAPI.ingest`) **procura primeiro** o ficheiro pre‑uploadado em `resources/<DFL...>/<filename>`.
- Se o request for JSON (por ex. body `{}`), **não** deve ser interpretado como upload binário.
- Mensagem de erro quando falta ficheiro orienta o fluxo correto (upload → ingest) e aponta para `docs/WKF-INGESTION-WORKFLOW.md`.
- `URIUtils.uriLastSegment()` passa a suportar URIs com `#` e a remover `/#` finais.
- `URIPage.getUri()` tolera URI sets (separados por `;`), usando o **primeiro URI**.

### 104b769 — fix app conf docker (2026-03-26)

- `conf/application.conf`

Pontos críticos:

- Garantir que `hascoapi.repository.triplestore` aponta para o fuseki do docker (`http://fuseki:3030`).
- Garantir paths de ingestão coerentes com o runtime docker (`hascoapi.paths.ingestion=/var/hascoapi`).

### 18893dd — Debugs logs removed (2026-03-26)

- `DataFileAPI`, `DataFile`
- `AnnotateWKF`, `BaseAnnotator`, `CSVRecordFile`, `MqttMessageWorker`

Pontos críticos:

- Ajustes para reduzir ruído e estabilizar logs do pipeline.

### 6730035 — task edit bug solved (2026-03-26)

- `URIPage`

Ponto crítico:

- Robustez adicional ao resolver URIs, especialmente quando chegam agregados/concatenados.

### 11ea5cd — WKFGen fix (2026-03-26)

- `IngestionAPI`
- WKF Excel generation: `WKFGen`, `WKFProcessStems`, `WKFProcesses`, `WKFTasks`
- `URIUtils`
- Docs WKF (vários ficheiros)

Pontos críticos:

- `WKFGen.genByStatus()` cria workbook sem depender de WKF específico e gera sheets (ProcessStems/Processes/Tasks).
- Começa a preparar o caminho para RequiredInstruments + pruning de Namespaces.

### 9bda824 — WKFGen fixed (2026-03-26)

- `DataFileAPI`
- WKF Excel generation: `WKFGen`, `WKFProcessStems`, `WKFProcesses`, `WKFRequiredInstruments`, `WKFTasks`
- Docs WKF (spec completa + guias)

Pontos críticos:

- `WKFGen.genByStatus()` passa a incluir **RequiredInstruments** referenciados por Tasks.
- No fim do generation, há pruning de namespaces não usados (reduz ruído e inconsistências no workbook).

### 449c340 — Roundtriptest finished (2026-03-27)

- Entidades: `Task`
- Annotators/generators: `AnnotateDP2`, `AnnotateINS`, `AnnotateWKF`, `BaseGenerator`, `DP2Generator`, `SDDAttributeGenerator`, `WKFGenerator`
- Transformers: `INSGen`, `WKFGen`, `WKFRequiredInstruments`
- Utils: `MetadataFactory`
- Docs: `DSG-ROUNDTRIP-TEST-ADDITION.md`, `INS-NAMESPACES-FIX.md`, `TASK-REQUIRED-INSTRUMENT-*.md`, `TEST-ASSERTIONS-FIX.md`, etc.
- Testes: `HascoRoundtripTest`, `test/resources/generated/*.ttl`, `test/resources/dsg/DSG-STD-test.xlsx`

Pontos críticos:

- `WKFGenerator` passa a **splitar multi‑values** (`;` ou `|`) para listas em propriedades como:
  - `vstoi:hasRequiredInstrument`
  - `vstoi:hasSubtask`
  - (e em RequiredInstruments) `vstoi:hasRequiredComponent`
  Isto é necessário para `MetadataFactory` criar múltiplos triplos (em vez de 1 literal concatenado).

- `MetadataFactory.createModel()` passa a aceitar valores que são `List` e criar um triple por valor.

- `INSGen.save()` passa a popular a sheet `Namespaces` a partir de namespaces em memória.

### cac4546 — task (2026-03-27)

- `Task`

Ponto crítico:

- Complementa ajustes em Task (ver secção “Task/RequiredInstruments” abaixo).

### 7ec3b0c — SDD fixes (2026-03-30)

- Docs: `docs/WKF-TASK-URI-CONCATENATION-FIX.md`

### 5192ce0 — Fix KGR ingest + media pipeline (SDDGen) (2026-04-30)

- `DataFileAPI`, `IngestionAPI`
- KGR ingestion: `AnnotateKGR`, `BaseAnnotator`, `KGRGenerator`
- Catalogo sheets: `MTSheet`
- Config: `conf/application.conf`, `docker-compose.yml`
- Doc principal: `docs/SDDGEN-KGR-CORRECAO-PT.md`

Pontos críticos:

- `BaseAnnotator.loadCatalog()` valida InfoSheet contra `MTSheet`.
- `MTSheet` inclui para KGR as chaves `hasMediaFolder` e `verifyUri`.
- `AnnotateKGR` valida `hasMediaFolder` quando o workbook referencia imagens (`hasco:hasImage`) e faz parsing robusto de `verifyUri`.
- `KGRGenerator` tem aliases legacy e tolerância a typo (`ipbejapng` → `IPBEJA.png`).
- `docker-compose.yml` passa a persistir `/var/hascoapi/resources` e `/var/hascoapi/media` por volumes.

### 6bad5c7 — Fix null check for hasAffiliationUri (2026-05-06)

- `Person`

Ponto crítico:

- `Person.getHasAffiliation()` passa a verificar null antes de `.isEmpty()`.

### cdad66d — downloadFile hardening (2026-05-11)

- `DataFileAPI`

Pontos críticos:

- `downloadFile(elementUri, filename)` sanitiza `filename` para basename (mitiga path traversal).
- Fallbacks de procura do ficheiro:
  1) `resources/<uriTerm>/<safeFilename>`
  2) `basePath/<safeFilename>` (ficheiros gerados)
  3) procura recursiva em `media/` (assets sociais)
- Define `Content-Type` baseado em extensão (importante para browsers com `nosniff`).

### 932adc6 — Dockerfiles + logging + dependency reliability (2026-05-12)

- `Dockerfile`, `Dockerfile-development`
- `DataFileAPI`
- `conf/logback.xml`
- `docker-compose-development.yml`

Pontos críticos:

- Dockerfiles:
  - `apt` passa a usar `https://ports.ubuntu.com/...` e limpa `apt lists`.
  - Configura `COURSIER_MIRRORS` para mitigar rate‑limits do Maven Central.
  - `Dockerfile-development` faz cache de deps (copia `build.sbt` + `project/` antes) e tem retries robustos no `sbt update`.

- `logback.xml` mantém `root=WARN` mas coloca `org.hascoapi.console.controllers.restapi.DataFileAPI` em `INFO`.

- `docker-compose-development.yml` monta volumes `hascoapi-resources` e `hascoapi-media`.

## Checklist de integração (pós cherry-pick)

### A) Build/Test (mínimo)

Recomendado correr:

```bash
sbt test
```

Notas importantes (para não perder tempo em problemas de ambiente):

- **Java:** o projeto (Play 2.8 / sbt 1.7.x / Scala 2.12) é estável com **Java 11** (o runtime docker também é Java 11). Se correres com um JDK demasiado recente, podes apanhar erros estranhos de parser/bytecode.
  - Exemplo (macOS): `export JAVA_HOME=$(/usr/libexec/java_home -v 11)`

- **Maven Central rate-limit (HTTP 429):** se o `sbt` falhar a descarregar dependências por 429, usa o mesmo mirror do Coursier que já está nos `Dockerfile*`.
  - Exemplo (bash/zsh):

    ```bash
    export COURSIER_MIRRORS=$(mktemp)
    cat > "$COURSIER_MIRRORS" <<'EOF'
    central.from=https://repo1.maven.org/maven2;https://repo.maven.apache.org/maven2
    central.to=https://maven-central.storage-download.googleapis.com/maven2
    central.type=tree
    EOF
    ```

- **Fuseki obrigatório para testes:** parte dos testes (incluindo roundtrip) faz queries ao triplestore.
  - Se estiveres **fora de docker**, o hostname `fuseki` pode não resolver; nesses casos:
    - corre o Fuseki localmente e garante endpoint acessível (ex.: `http://localhost:3030`), **ou**
    - corre os testes *dentro* do container `hascoapi` na rede do `docker compose`.

Se quiseres isolar:

```bash
sbt "testOnly org.hascoapi.tests.HascoRoundtripTest"
```

### B) Docker (se aplicável)

```bash
docker compose up -d --build
```

Verificar que existem volumes:

- `hascoapi-resources`
- `hascoapi-media`

E que `conf/application.conf` aponta ingestão para `/var/hascoapi`.

### C) Endpoints críticos para o front-end

Validar pelo menos:

- Upload de workbook:
  - `POST /hascoapi/api/uploadFile/:elementUri/:filename`
  - Confirma que o ficheiro fica em `.../resources/<DFL...>/<filename>`

- Ingestão:
  - `POST /hascoapi/api/ingest/:status/:elementType/:elementUri`
  - Confirma que **não** precisa de re‑enviar o ficheiro no body (se já foi uploadado)

- Media:
  - `POST /hascoapi/api/uploadMedia/:folder/:filename`
  - `GET /hascoapi/api/downloadFile/:elementUri/:filename`

  Exemplo (upload ZIP sem multipart, mais robusto em alguns ambientes):

  ```bash
  curl -H "Content-Type: application/octet-stream" \
    --data-binary "@organizations.zip" \
    http://localhost:9000/hascoapi/api/uploadMedia/organizations/organizations.zip
  ```

  Notas:
  - Upload de `.zip` faz extração assíncrona para `/var/hascoapi/media/<folder>/`.
  - A extração normaliza paths para evitar `.../<folder>/<folder>/<ficheiro>` e tem proteção básica contra path traversal dentro do ZIP.

- URI resolver:
  - `GET /hascoapi/api/uri/:uri` (deve tolerar listas separadas por `;`)

## Notas de conflito (onde é mais provável doer)

Se o teu colega tiver alterações próprias nestes ficheiros no `INStoDSG`, é aqui que os conflitos são mais prováveis:

- `app/org/hascoapi/console/controllers/restapi/DataFileAPI.java`
- `app/org/hascoapi/console/controllers/restapi/IngestionAPI.java`
- `app/org/hascoapi/ingestion/BaseAnnotator.java`
- `app/org/hascoapi/utils/MTSheet.java`
- `app/org/hascoapi/ingestion/KGRGenerator.java`
- `app/org/hascoapi/transform/mt/wkf/WKFGen.java` (+ helpers)
- `app/org/hascoapi/ingestion/WKFGenerator.java`
- `app/org/hascoapi/entity/pojo/Task.java`
- `conf/application.conf`, `conf/logback.xml`
- `docker-compose.yml`, `docker-compose-development.yml`, `Dockerfile*`

Regra prática para resolver:

- Para bugs de workflow (upload/ingest/download), preferir a versão do `fix_nuno_SDDGen`.
- Para configurações, manter valores do `INStoDSG` **desde que** não removam:
  - `play.server.max-header-size=64k`
  - volumes para `/var/hascoapi/resources` e `/var/hascoapi/media`
  - `logger DataFileAPI = INFO` no logback

## Documentação de suporte (já no repo)

Quando precisares de detalhe adicional, estes ficheiros já documentam o racional e validações:

- WKF ingest: `docs/WKF-INGESTION-WORKFLOW.md`, `docs/WKF-INGESTION-FIX-SUMMARY.md`, `docs/ALTERACOES_WKF_INGEST.md`
- WKF Excel spec: `docs/WKF-EXCEL-SPECIFICATION-COMPLETE.md`
- KGR/SDDGen: `docs/SDDGEN-KGR-CORRECAO-PT.md`
- Roundtrip: `docs/DSG-ROUNDTRIP-TEST-ADDITION.md`, `docs/TEST-ASSERTIONS-FIX.md`

---

## (Opcional) Artefactos de patch (no mesmo fluxo)

### A) Patch unico (referencia runtime)

No ambiente onde este documento foi criado existe tambem um patch "runtime" (nao versionado):

- `docs/PORT-INStoDSG-from-fix_nuno_SDDGen.runtime.patch`

Serve como referencia/backup textual.

### B) Pacote format-patch (recomendado para envio ao colega)

Se preferires enviar um pacote de patches para o teu colega aplicar no branch dele, usa `git format-patch`.

Gerar pacote no branch de origem das alteracoes:

```bash
git checkout fix_nuno_SDDGen
git fetch origin --prune

mkdir -p /tmp/hascoapi-port-patches
git format-patch --cover-letter -o /tmp/hascoapi-port-patches origin/INStoDSG..fix_nuno_SDDGen

# opcional: empacotar para envio
tar -czf /tmp/hascoapi-port-patches.tgz -C /tmp hascoapi-port-patches
```

Aplicar no branch do teu colega (lado `INStoDSG`):

```bash
git fetch origin --prune
git checkout -b port_fix_nuno_SDDGen origin/INStoDSG

# aplica com tentativa de merge 3-way em caso de divergencias
git am --3way /tmp/hascoapi-port-patches/*.patch
```

Se houver conflitos durante `git am`:

- ver estado: `git status`
- resolver conflitos
- marcar resolvidos: `git add <ficheiros>`
- continuar: `git am --continue`
- abortar tudo (se necessario): `git am --abort`

Validacao rapida apos aplicar:

```bash
git log --oneline origin/INStoDSG..HEAD
```

Deves ver os mesmos 13 commits funcionais deste port (ordem equivalente).
