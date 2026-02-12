# 🔍 Diagnóstico: Erro 404 nos Generates

## ❌ Problema Identificado

O erro **NÃO é 404 de rota**. As rotas estão funcionando corretamente.

### Evidências:
1. ✅ Health check funciona: `{"isSuccessful":true,"body":"DP2 generation routes are active"}`
2. ✅ Rotas estão definidas no `conf/routes` (linhas 178-205)
3. ✅ Métodos existem em `IngestionAPI.java`
4. ✅ Código compila sem erros

### O Problema Real:
**O erro 404 acontece quando o front-end tenta baixar o arquivo gerado, porque o arquivo não existe.**

Do log anterior:
```
[ERROR] IngestionAPI.mtGetGenerated(): File not found - \var\hascoapi\sadsdasdas.xlsx
```

Isso significa:
- ✅ A rota `/api/mt/gen/perstatus/...` **foi chamada**
- ✅ O método `mtGenByStatus()` **executou**
- ❌ A geração **falhou** ou **não aconteceu**
- ❌ O arquivo Excel **não foi criado**
- ❌ Quando o front chama `/api/mt/get/generated/sadsdasdas.xlsx`, retorna **404 (arquivo não encontrado)**

---

## 🔎 Análise do Fluxo Atual

### Para DP2:
```
Front-end POST /api/mt/gen/perstatus/dp2/...
    ↓
IngestionAPI.mtGenByStatus()
    ↓
DP2Gen.genByStatus()  ✅ (implementado)
    ↓
Gera arquivo Excel
    ↓
Salva em /var/hascoapi/filename.xlsx
    ↓
Front-end GET /api/mt/get/generated/filename.xlsx
    ↓
✅ Arquivo existe, download funciona
```

### Para WKF (problema):
```
Front-end POST /api/mt/gen/perstatus/wkf/...
    ↓
IngestionAPI.mtGenByStatus()
    ↓
❌ ERRO: "invalid elementtype=[wkf]"
    ↓
Retorna erro, não gera arquivo
    ↓
Front-end GET /api/mt/get/generated/filename.xlsx
    ↓
❌ 404: Arquivo não existe
```

---

## 🐛 Root Cause

### No método `mtGenByStatus()`:

```java
switch (elementtype) {
    case "ins":
        generationResult = INSGen.genByStatus(...);
        break;
    case "dp2":
        generationResult = DP2Gen.genByStatus(...);
        break;
    case "dsg":
        generationResult = DSGGen.genByStatus(...);
        break;
    case "kgr":
        generationResult = KGRGen.genByStatus(...);
        break;
    default:
        // ❌ WKF cai aqui!
        return ok(ApiUtil.createResponse(
            "[ERROR] IngestionAPI.mtGenByStatus() invalid elementtype=[" + elementtype + "]", 
            false));
}
```

**WKF não está no switch!** Por isso retorna erro e não gera o arquivo.

### Solução Necessária:
1. Criar `WKFGen.java` (gerador de Excel para WKF)
2. Adicionar `case "wkf"` no switch

---

## ✅ Solução Imediata

### 1. Adicionar case WKF no switch (temporário - retorna mensagem clara):

```java
switch (elementtype) {
    case "ins":
        generationResult = INSGen.genByStatus(status,filename,mediaFolder,verifyUri);
        break;
    case "dp2":
        generationResult = DP2Gen.genByStatus(datafileuri, status, filename, mediaFolder, verifyUri);
        break;
    case "dsg":
        generationResult = DSGGen.genByStatus(status,filename,mediaFolder,verifyUri);
        break;
    case "kgr":
        generationResult = KGRGen.genByStatus(status,filename,mediaFolder,verifyUri);
        break;
    case "wkf":
        // TODO: Implementar WKFGen.genByStatus()
        return ok(ApiUtil.createResponse(
            "WKF generation not implemented yet. Please implement WKFGen.java", 
            false));
    default:
        return ok(ApiUtil.createResponse(
            "[ERROR] IngestionAPI.mtGenByStatus() invalid elementtype=[" + elementtype + "]", 
            false));
}
```

### 2. Implementar `WKFGen.java` (completo):

Similar a `DP2Gen.java`, precisa:
- `genByStatus(String datafileuri, String status, String filename, String mediaFolder, String verifyUri)`
- `create()` - cria o Workbook
- Métodos para preencher cada sheet:
  - `WKFProcessStems.add()`
  - `WKFProcesses.add()`
  - `WKFTasks.add()`
  - `WKFRequiredInstruments.add()`
- `save()` - salva o arquivo Excel

---

## 🔧 Para Outros MTs (INS, DSG, KGR)

Se **esses também estão retornando 404**, o problema pode ser:

### 1. **Path de salvamento incorreto**
Verificar se `ConfigProp.getPathIngestion()` está retornando o caminho correto.

### 2. **Permissões de escrita**
O servidor pode não ter permissão para escrever em `/var/hascoapi/`.

### 3. **Consultas SPARQL falhando**
Os generators fazem consultas ao triple store. Se o triple store estiver vazio ou inacessível, a geração falha.

### Debug:
```java
System.out.println("✓ Generation started for elementtype: " + elementtype);
String generationResult = DP2Gen.genByStatus(...);
System.out.println("✓ Generation result: " + generationResult);
if (generationResult == null || generationResult.isEmpty()) {
    System.out.println("❌ Generation returned null/empty - file not created!");
}
```

---

## 📋 Checklist de Verificação

Para cada MT que está retornando 404:

- [ ] Verificar se o case existe no switch de `mtGenByStatus()`
- [ ] Verificar se `XXXGen.genByStatus()` existe e está funcionando
- [ ] Verificar se o path de salvamento está correto
- [ ] Verificar se há dados no triple store para gerar
- [ ] Verificar logs do console para erros de geração
- [ ] Verificar se o arquivo foi criado no filesystem
- [ ] Verificar permissões de escrita no diretório

---

## 🎯 Ação Recomendada

**Imediato**:
1. Adicionar case "wkf" no switch com mensagem clara
2. Verificar logs do console quando chama geração de INS/DSG/KGR
3. Verificar se arquivos estão sendo criados em `/var/hascoapi/`

**Curto Prazo**:
1. Implementar `WKFGen.java` completo
2. Adicionar mais logging nos generators existentes
3. Adicionar validação de permissões de arquivo

**Médio Prazo**:
1. Criar testes unitários para generators
2. Implementar health check que verifica path de escrita
3. Adicionar métricas de geração (tempo, sucesso/falha)

---

**Data**: 2026-02-09  
**Status**: 🔍 Diagnóstico completo  
**Próximo Passo**: Adicionar case WKF e verificar logs dos outros MTs
