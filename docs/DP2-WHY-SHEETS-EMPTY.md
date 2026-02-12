# DP2 - Por que as Sheets Estão Vazias?

## Diagnóstico Completo

### O Que Você Está Vendo
```
Type: [dp2] JSON [...]
Total triples in model: 7
```

✅ Isto está **funcionando corretamente**!
- DataFile criado
- DP2 metadata criado
- 7 triples no triple store

### O Que Você Esperava Ver
Excel com sheets preenchidos:
- hasDependencies ✓ (sempre aparece)
- Deployments ❌ (vazio)
- Platforms ❌ (vazio)
- PlatformInstances ❌ (vazio)
- FieldsOfView ❌ (vazio)
- InstrumentInstances ❌ (vazio)
- ComponentInstances ❌ (vazio)
- SensingPerspective ❌ (vazio)

## Por Que Estão Vazias?

### O DP2 Metadata ≠ Dados do DP2

O DP2 metadata que você criou contém apenas:
- uri
- label
- hasDataFileUri
- hasVersion
- comment
- hasSIRManagerEmail
- type

**MAS NÃO contém**:
- Deployments
- Platforms
- PlatformInstances
- InstrumentInstances
- etc.

Esses dados precisam ser **ingeridos separadamente** de um arquivo Excel DP2!

## Como Funciona a Geração

### Quando você chama:
```
POST /api/mt/gen/perstatus/dp2/{datafileuri}/DRAFT/file.xlsx/null/null
```

O sistema faz:
```java
1. Buscar DP2 metadata pelo datafileuri
2. Consultar triple store por Deployments associados a esse datafile
   → Se não existir nenhum: sheet fica vazia (só headers)
3. Consultar triple store por Platforms associados a esse datafile
   → Se não existir nenhum: sheet fica vazia
4. E assim por diante para cada tipo de entidade
5. Criar Excel com os dados encontrados (ou vazio se não encontrou)
```

## Dois Cenários de Uso

### Cenário 1: Criar Template Vazio (O que você está fazendo)

**Passo 1**: Criar DP2 metadata via API
```json
POST /hascoapi/api/dp2/create/...
{
  "label": "Meu DP2",
  "hasDataFileUri": "...",
  ...
}
```

**Passo 2**: Gerar Excel vazio
```bash
POST /api/mt/gen/perstatus/dp2/.../DRAFT/MeuDP2.xlsx/null/null
```

**Resultado**: Excel com todas as sheets, mas **vazias** (só headers)

**Passo 3**: Preencher manualmente e fazer upload
- Abrir Excel
- Preencher Deployments, Platforms, etc.
- Upload via `/hascoapi/api/ingest/...`
- Agora os dados estarão no triple store

**Passo 4**: Gerar novamente
- Agora o Excel terá dados!

### Cenário 2: Upload de DP2 Completo

**Passo 1**: Criar DP2.xlsx manualmente com dados
- Preencher todas as sheets com dados reais

**Passo 2**: Upload do arquivo
```
POST /hascoapi/api/ingest/DRAFT/dp2/...
```

**Sistema faz**:
- Cria DP2 metadata
- **INGERE todos os Deployments** da sheet → salva no triple store
- **INGERE todos os Platforms** da sheet → salva no triple store
- **INGERE todos os PlatformInstances** da sheet → salva no triple store
- etc.

**Passo 3**: Gerar (exportar) de volta
```
POST /api/mt/gen/perstatus/dp2/.../DRAFT/Export.xlsx/null/null
```

**Resultado**: Excel com **todos os dados** que foram ingeridos

## Seu Problema Específico

Você está no **Cenário 1**, mas esperando resultado do **Cenário 2**.

### O que está acontecendo:
1. ✅ Criar DP2 metadata (via GenerateForm do Drupal)
2. ❌ Front-end **não está chamando** o endpoint de geração
3. ❌ Resultado: Nenhum arquivo Excel é gerado

### O que deveria acontecer:
1. ✅ Criar DP2 metadata
2. ✅ **Front-end chama automaticamente** o endpoint de geração
3. ✅ Excel **vazio** é gerado (template para preencher)
4. Usuário baixa, preenche, e faz upload de novo

## Solução Imediata

### Opção 1: Chamar Geração Manualmente

Após criar o DP2, você verá nos logs:
```
⚠️  DP2 metadata created successfully!
    To generate the Excel file, the front-end should call:
    POST /api/mt/gen/perstatus/dp2/https%3A%2F%2F...DFGL.../DRAFT/Asdasdasd.xlsx/null/null
```

**Copie essa URL e execute**:
```bash
curl -X POST "http://localhost:9000/api/mt/gen/perstatus/dp2/..."
```

Agora você terá o Excel gerado (vazio, mas com todas as sheets).

### Opção 2: Habilitar Geração Automática

Edite `DP2API.java` e **descomente** as linhas:
```java
// System.out.println("\n🔄 Auto-generating Excel file...");
// try {
//     org.hascoapi.transform.mt.dp2.DP2Gen.genByStatus(datafileUri, status, filename, null, null);
//     System.out.println("✓ Excel file generated automatically");
// } catch (Exception e) {
//     System.out.println("✗ Auto-generation failed: " + e.getMessage());
// }
```

Remova os `//` no início de cada linha.

Agora, **sempre que criar um DP2**, o Excel será gerado automaticamente.

### Opção 3: Corrigir o Front-End (Drupal)

O front-end deveria estar fazendo algo como:

```php
// 1. Criar DP2 metadata
$response1 = $client->post('/hascoapi/api/dp2/create/', ['json' => $dp2Data]);

// 2. Se sucesso, chamar geração
if ($response1->isSuccessful()) {
    $generationUrl = "/api/mt/gen/perstatus/dp2/{$datafileUri}/DRAFT/{$filename}/null/null";
    $response2 = $client->post($generationUrl);
}
```

Mas aparentemente está **parando no passo 1**.

## Verificação

### Teste 1: Health Check (já funcionou ✅)
```bash
curl http://localhost:9000/api/mt/gen/health
# Retorna: {"isSuccessful":true,"body":"DP2 generation routes are active"}
```

### Teste 2: Geração Manual
Pegue a URL dos logs do `DP2API.createDP2Result()` e chame:
```bash
curl -X POST "http://localhost:9000/api/mt/gen/perstatus/dp2/https%3A%2F%2Fhadatac.org%2Font%2Fhadatac%23%2FDFL1770637034846641/DRAFT/Asdasdasd.xlsx/null/null"
```

**Você DEVE ver nos logs**:
```
========== IngestionAPI.mtGenByStatus() START ==========
Parameters:
  elementtype: dp2
  datafileuri: https://hadatac.org/ont/hadatac#/DFL1770637034846641
  status: DRAFT
  filename: Asdasdasd.xlsx

========== DP2Gen.genByStatus() START ==========
...
Querying triple store for Deployments...
Found 0 deployments
...

========== DP2Gen.save() START ==========
✓ File written successfully
File size: 12345 bytes
```

### Teste 3: Download do Arquivo
```bash
curl -X POST "http://localhost:9000/api/mt/get/generated/Asdasdasd.xlsx" -o Asdasdasd.xlsx
```

Abra o Excel - deve ter:
- ✅ InfoSheet com hasDependencies
- ✅ Todas as 8 sheets
- ⚠️ **Mas vazias** (sem linhas de dados, só headers)

**Isto é ESPERADO!** Porque você não ingeriu nenhum Deployment/Platform ainda.

## Resumo Final

| Item | Status | Explicação |
|------|--------|------------|
| Rotas | ✅ Funcionando | Health check retorna 200 |
| DP2 Metadata | ✅ Criado | 7 triples no triple store |
| Chamada de Geração | ❌ Não acontece | Front-end não chama automaticamente |
| Excel Gerado | ❌ Não existe | Porque geração não foi chamada |
| Sheets Vazias | ✅ Normal | Triple store não tem Deployments/Platforms/etc. |

**Ação Necessária**:
1. Chamar manualmente o endpoint de geração (teste imediato)
2. Corrigir front-end para chamar automaticamente (solução permanente)
3. OU habilitar geração automática no backend (workaround)

---

**O problema NÃO é nas rotas. O problema é que a geração não está sendo chamada!**
