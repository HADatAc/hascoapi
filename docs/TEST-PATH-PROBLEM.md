# 🔍 TESTE DE DIAGNÓSTICO - Execute e Me Envie os Logs

## 📋 O Que Fazer Agora

Você precisa testar a geração e **copiar TODOS os logs do console** para mim.

---

## 🧪 Teste 1: Gerar INS via Front-End

### Passos:
1. Abra o front-end (Drupal/React)
2. Navegue até **Simulators** (INS)
3. Clique em **Generate** ou **Create New**
4. Preencha com qualquer dado de teste (ex: "Test INS")
5. Submeta o formulário

### O Que Observar:
- Mensagem que aparece no front
- **COPIE TODOS OS LOGS** do console do servidor (terminal do SBT)

---

## 📝 Logs Que Preciso Ver

Procure por estas seções no console do servidor:

```
========== IngestionAPI.mtGenByStatus() START ==========
✓ mtGenByStatus endpoint was called successfully!
Parameters:
  elementtype: [...]
  datafileuri: [...]
  status: [...]
  filename: [...]
  mediaFolder: [...]
  verifyUri: [...]
  ConfigProp.getPathIngestion(): [...]
  Path exists: ...
  Path is directory: ...
  Path can write: ...
✓ All required parameters present
→ Calling generator for elementtype: ins
  Calling INSGen.genByStatus()...
  INSGen.genByStatus() returned: [...]
  Generation result: [...]
  ...
========== IngestionAPI.mtGenByStatus() END ==========
```

**E também procure por**:
```
INS workbook save successfully!
```
ou
```
Error occurred while writing the workbook: ...
```

---

## 📊 Informações Específicas Que Preciso

### 1. Path Configuration:
```
ConfigProp.getPathIngestion(): [QUAL É O PATH?]
Path exists: [true ou false?]
Path is directory: [true ou false?]
Path can write: [true ou false?]
```

### 2. Generator Result:
```
INSGen.genByStatus() returned: [O QUE RETORNOU?]
Generation result: [...]
Generation result is null: [true ou false?]
Generation result is empty: [true ou false?]
```

### 3. Exceções:
Se houver algo como:
```
❌ EXCEPTION during generation:
  Exception type: ...
  Message: ...
```

### 4. Arquivo Criado:
Após tentar gerar, execute:
```powershell
Get-ChildItem "C:\hascoapi\var\" -Filter "*.xlsx" | Select-Object Name, Length, LastWriteTime
```

E me diga o resultado.

---

## 🎯 Cenários Possíveis

### Cenário A: Path Ainda Está Errado
```
ConfigProp.getPathIngestion(): [/var/hascoapi/]  ❌ RUIM
Path exists: false
```
**Solução**: Config não foi recarregado, reinicie novamente.

### Cenário B: Path Correto mas Sem Permissão
```
ConfigProp.getPathIngestion(): [C:/hascoapi/var/]  ✅ BOM
Path exists: true
Path is directory: true
Path can write: false  ❌ RUIM
```
**Solução**: Problema de permissões Windows.

### Cenário C: Generator Não Retorna Nada
```
INSGen.genByStatus() returned: []  ❌ STRING VAZIA
Generation result is empty: true
```
**Solução**: Generator não está salvando arquivo ou retornando path.

### Cenário D: Exception Durante Geração
```
❌ EXCEPTION during generation:
  Exception type: java.io.IOException
  Message: Access Denied
```
**Solução**: Problema específico identificado na exception.

### Cenário E: Não Há Dados Para Gerar
```
INS workbook save successfully!
```
Mas arquivo está vazio ou com apenas headers.
**Solução**: Não há Instruments/Components no triple store com status DRAFT.

---

## 🔄 Depois de Coletar os Logs

**ME ENVIE**:

1. ✅ **Logs completos** do console (do início do `mtGenByStatus()` até o fim)
2. ✅ **Mensagem do front-end** (exata)
3. ✅ **Resultado** do comando `Get-ChildItem "C:\hascoapi\var\" -Filter "*.xlsx"`
4. ✅ **Qualquer exception/error** que aparecer em vermelho

Com essas informações, vou identificar **exatamente** onde está o problema!

---

## 💡 Dica Rápida

Se os logs forem muito longos, salve em arquivo:

```powershell
# No terminal onde o SBT está rodando, os logs vão aparecer
# Copie tudo desde "========== IngestionAPI.mtGenByStatus() START"
# até "========== IngestionAPI.mtGenByStatus() END"
```

Ou redirecione a saída ao iniciar o SBT:

```powershell
sbt run 2>&1 | Tee-Object -FilePath "server-logs.txt"
```

Depois tente gerar e me envie o arquivo `server-logs.txt`.

---

**Aguardando seus logs para diagnóstico preciso!** 🔍
