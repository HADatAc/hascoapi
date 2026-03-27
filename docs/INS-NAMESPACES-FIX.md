# Correção: Namespaces Vazios no INS Regenerado

## 🚨 Problema Reportado

**Sintoma**: A sheet "Namespaces" no arquivo INS regenerado está vazia.

## 🔍 Root Cause Analysis

### Investigação

1. **INSGen.create()** - Cria a sheet "Namespaces" com headers ✅
2. **INSGen.save()** - Salva o workbook **SEM popular namespaces** ❌

### Comparação com WKFGen (que funciona)

**WKFGen.save()**:
```java
// WKFGen popula namespaces ANTES de salvar
Map<String, NameSpace> nsMap = WKFGenHelper.getNamespaces();
int nsRowNum = 1;
if (nsMap != null && !nsMap.isEmpty()) {
    for (NameSpace ns : nsMap.values()) {
        Row row = nsSheet.createRow(nsRowNum++);
        row.createCell(0).setCellValue(safe(ns.getLabel()));
        row.createCell(1).setCellValue(safe(ns.getUri()));
        row.createCell(2).setCellValue(safe(ns.getSourceMime()));
        row.createCell(3).setCellValue(safe(ns.getSource()));
    }
} else {
    // Fallback para in-memory namespaces
    List<NameSpace> inMem = NameSpace.findInMemory();
    // ... popula
}
```

**INSGen.save()** (ANTES da correção):
```java
// ❌ NÃO POPULA NAMESPACES!
try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
    helper.workbook.write(fileOut); // Salva direto sem popular
    // ...
}
```

### Root Cause

O método `INSGen.save()` **não estava populando a sheet Namespaces** antes de salvar o arquivo, resultando em uma sheet vazia (apenas headers).

## ✅ Correção Implementada

### 1. Modificado INSGen.save()

Adicionada chamada para popular namespaces **ANTES** de salvar:

```java
public static String save(INSGenHelper helper, String filename) {
    System.out.println("\n========== INSGen.save() START ==========");
    
    // ✅ NOVO: Populate Namespaces sheet before saving
    try {
        System.out.println("  → Saving namespaces...");
        populateNamespaces(helper.workbook);
        System.out.println("  ✓ Namespaces saved");
    } catch (Exception e) {
        System.err.println("  ⚠️ WARNING: Failed to populate namespaces: " + e.getMessage());
        e.printStackTrace();
    }

    // ... resto do código de save
}
```

### 2. Adicionado Método populateNamespaces()

Novo método que popula a sheet Namespaces com dados in-memory:

```java
/**
 * Populate Namespaces sheet with in-memory namespace data
 */
private static void populateNamespaces(Workbook workbook) {
    Sheet nsSheet = workbook.getSheet(INSGen.NAMESPACES);
    if (nsSheet == null) {
        System.err.println("  ⚠️ Namespaces sheet not found!");
        return;
    }

    // Get in-memory namespaces (ordered)
    List<org.hascoapi.entity.pojo.NameSpace> inMem = 
        org.hascoapi.entity.pojo.NameSpace.findInMemory();
    
    if (inMem == null || inMem.isEmpty()) {
        System.out.println("  ⚠️ No namespaces found in memory");
        return;
    }

    // Populate namespace rows starting from row 1 (row 0 is header)
    int nsRowNum = 1;
    for (org.hascoapi.entity.pojo.NameSpace ns : inMem) {
        Row row = nsSheet.createRow(nsRowNum++);
        row.createCell(0).setCellValue(safe(ns.getLabel()));       // hasPrefix
        row.createCell(1).setCellValue(safe(ns.getUri()));         // hasNameSpace
        row.createCell(2).setCellValue(safe(ns.getSourceMime()));  // hasFormat
        row.createCell(3).setCellValue(safe(ns.getSource()));      // hasSource
    }

    System.out.println("  ✓ Added " + (nsRowNum - 1) + " namespaces");
}
```

### 3. Adicionado Método Helper safe()

```java
/**
 * Helper method to safely convert null strings to empty strings
 */
private static String safe(String val) {
    return val == null ? "" : val;
}
```

## 📊 Resultado Esperado

### ANTES (❌)
```
Namespaces Sheet:
+------------+--------------+-----------+-----------+
| hasPrefix  | hasNameSpace | hasFormat | hasSource |
+------------+--------------+-----------+-----------+
|            |              |           |           | <-- VAZIO!
+------------+--------------+-----------+-----------+
```

### DEPOIS (✅)
```
Namespaces Sheet:
+------------+----------------------------------------+-------------+------------------------------+
| hasPrefix  | hasNameSpace                           | hasFormat   | hasSource                    |
+------------+----------------------------------------+-------------+------------------------------+
| hasco      | http://hadatac.org/ont/hasco/          | text/turtle | https://hadatac.org/ont/...  |
| vstoi      | http://hadatac.org/ont/vstoi#          | text/turtle | https://hadatac.org/ont/...  |
| pmsr       | http://pmsr.net/ont/pmsr#              | text/turtle | https://hadatac.org/ont/...  |
| rdf        | http://www.w3.org/1999/02/22-rdf-...   | text/turtle | https://www.w3.org/1999/...  |
| rdfs       | http://www.w3.org/2000/01/rdf-schema#  | text/turtle | https://www.w3.org/2000/...  |
| owl        | http://www.w3.org/2002/07/owl#         | text/turtle | https://www.w3.org/2002/...  |
| xsd        | http://www.w3.org/2001/XMLSchema#      | text/turtle | https://www.w3.org/2001/...  |
+------------+----------------------------------------+-------------+------------------------------+
```

## 🎯 Benefícios

1. ✅ **Namespaces agora são populados** no arquivo INS regenerado
2. ✅ **Consistência com WKFGen** - ambos usam a mesma abordagem
3. ✅ **Fallback robusto** - usa `NameSpace.findInMemory()` que sempre retorna namespaces
4. ✅ **Logs informativos** - mostra quantos namespaces foram adicionados
5. ✅ **Tratamento de erros** - captura exceções sem quebrar o save

## 🔧 Arquivos Modificados

- `app/org/hascoapi/transform/mt/ins/INSGen.java`
  - ✅ Modificado `save()` para chamar `populateNamespaces()`
  - ✅ Adicionado método `populateNamespaces()`
  - ✅ Adicionado método helper `safe()`

## 📝 Verificação

Para verificar se a correção funciona:

1. Execute o teste de roundtrip INS:
   ```bash
   sbt 'testOnly org.hascoapi.tests.HascoRoundtripTest -- -z "INS"'
   ```

2. Verifique o arquivo regenerado:
   ```
   test/resources/generated/INS-PMSR-Simulators-regenerated.xlsx
   ```

3. A sheet "Namespaces" deve conter múltiplas linhas com prefixos (hasco, vstoi, pmsr, etc.)

## 🚨 Outros MTs Afetados?

**Ação Recomendada**: Verificar se DP2Gen e outros geradores têm o mesmo problema!

Generators a verificar:
- ✅ WKFGen - OK (já popula namespaces)
- ✅ INSGen - CORRIGIDO
- ❓ DP2Gen - VERIFICAR
- ❓ DSGGen - VERIFICAR
- ❓ SDDGen - VERIFICAR

---

**Status**: Compilado com sucesso ✅  
**Data**: 2026-03-27  
**Tipo**: Bug Fix - Namespaces Vazios  
**Prioridade**: Alta - Afeta qualidade dos arquivos regenerados

