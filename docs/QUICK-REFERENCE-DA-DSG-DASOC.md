# 🚀 Quick Reference: DA-DSG-DASOC Testing

## ⚡ TL;DR - Teste Rápido

### 1️⃣ Importar Collection no Postman
```
File → Import → POSTMAN-DA-DSG-DASOC-Collection.json
```

### 2️⃣ Criar Environment
- Name: `HASCOAPI Local`
- Variable: `baseUrl` = `http://localhost:9000`

### 3️⃣ Executar em Ordem
1. CREATE STUDY
2. CREATE STREAM  
3. CREATE SOC
4. CREATE DSG
5. **GENERATE DSG WITH DASOCs** ⭐
6. VERIFY DAs CREATED
7. CLEANUP

---

## 🔑 Endpoint Principal

```
POST {{baseUrl}}/hascoapi/api/mt/gen/perelement/da/{{dataFileUri}}/{{dsgUri}}/file.xlsx/test_media/true?generateDASOCs=true
                                                                                                          ^^^^^^^^^^^^^^^^^^^^^^
                                                                                                          IMPORTANTE!
```

---

## 📊 Verificação Rápida

### ✅ Sucesso quando você vê:

**Console do Postman:**
```
📊 Total DAs with status DRAFT: 5
🔗 DAs with SOC linked: 5
```

**Response da API:**
```json
{
  "isSuccessful": true,
  "body": [
    {
      "uri": "http://test.example.com/kb/da/DA_001",
      "hasObjectScope": "http://test.example.com/kb/soc/SOC_123",
      ...
    }
  ]
}
```

### ❌ Problema quando:

**Nenhum DA criado:**
```
📊 Total DAs with status DRAFT: 0
```

**DAs sem SOC:**
```
🔗 DAs with SOC linked: 0
⚠️  No DAs with SOC linkage found
```

---

## 🔧 Troubleshooting Rápido

| Problema | Solução |
|----------|---------|
| No DAs found | Adicione `"hasStatus": "DRAFT"` ao criar Study/Stream/DSG |
| DAs without SOC | Verifique `?generateDASOCs=true` está na URL |
| File not found | Crie pasta `C:/hascoapi/var/media/test_media/` |
| 404 Not Found | Use `/hascoapi/api/...` (com prefixo) |

---

## 🎯 Query Parameters

### ✅ CORRETO:
```
/api/mt/gen/perelement/da/uri/dsg/file.xlsx/media/true?generateDASOCs=true
```

### ❌ ERRADO:
```
/api/mt/gen/perelement/da/uri/dsg/file.xlsx/media/true/true
                                                           ^^^^
                                                    (isso é verifyUri!)
```

---

## 📝 Valores Padrão

```javascript
elementtype = "da"          // ou "dsg", "sdd", etc.
status = "DRAFT"            // ou "PUBLISHED", "ARCHIVED"
filename = "DSG-TEST.xlsx"  // qualquer nome .xlsx
mediaFolder = "test_media"  // pasta dentro de {ingestion}/media/
verifyUri = "true"          // pode ser "true" ou uma URI real
generateDASOCs = true       // QUERY PARAMETER!
```

---

## 🎬 One-Liner Test

Se quiser testar rapidamente sem Collection:

```bash
# 1. Create Study
curl -X POST "http://localhost:9000/hascoapi/api/study/create/%7B%22uri%22%3A%22http%3A%2F%2Ftest.com%2Fstudy1%22%2C%22typeUri%22%3A%22http%3A%2F%2Fhadatac.org%2Font%2Fhasco%23Study%22%2C%22hascoTypeUri%22%3A%22http%3A%2F%2Fhadatac.org%2Font%2Fhasco%23Study%22%2C%22label%22%3A%22Test%22%2C%22hasStatus%22%3A%22DRAFT%22%2C%22hasSIRManagerEmail%22%3A%22test%40test.com%22%2C%22namedGraph%22%3A%22http%3A%2F%2Fhadatac.org%2Fkb%2Ftest%22%7D"

# 2. Verify
curl "http://localhost:9000/hascoapi/api/study/status/DRAFT/10/0"
```

---

## 📚 Arquivos Criados

1. ✅ **POSTMAN-DA-DSG-DASOC-TEST-GUIDE.md** - Guia completo detalhado
2. ✅ **POSTMAN-DA-DSG-DASOC-Collection.json** - Collection pronta para importar
3. ✅ **DA-SOC-TEST-TEMPLATE.csv** - Template do arquivo CSV
4. ✅ **generate_da_soc_file.py** - Script para gerar CSV automaticamente
5. ✅ **QUICK-REFERENCE-DA-DSG-DASOC.md** - Este arquivo (referência rápida)

---

## 🎓 Conceitos-Chave

```
Study
  └── Stream ─────┐
  └── SOC         │
                  │
DSG ──────────────┤
                  │
generateDASOCs=true
                  │
                  ↓
           Cria múltiplos DAs
           └── cada DA tem hasObjectScope → SOC
```

---

## ✨ Expected Result

```json
{
  "isSuccessful": true,
  "body": "DSG-TEST-1712505600.xlsx"
}
```

E ao verificar:

```json
{
  "isSuccessful": true,
  "body": [
    {
      "uri": "...",
      "hasObjectScope": "http://test.example.com/kb/soc/SOC_123" ← ISSO!
    }
  ]
}
```

---

**🚀 Pronto para testar! Importe a collection e execute!**

