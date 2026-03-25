# Problema: URI Malformada no Triplestore

## Resumo do Problema

O sistema está falhando ao gerar o arquivo Excel a partir do SDD armazenado no triplestore devido a uma URI malformada:

```
http://qudt.org/vocab/unit/MicroGM-PER-M3%A
```

O `%A` é uma codificação percentual inválida (deveria ser `%0A` para representar um newline, mas está incompleta).

## Causa Raiz

No arquivo Excel original, o campo Unit na linha de `air_quality` continha:
```
unit:MicroGM-PER-M3 
```

Note o espaço/newline após "M3". Quando isso foi processado e armazenado no triplestore, criou-se uma URI malformada.

## Solução Implementada

Foram adicionadas múltiplas camadas de proteção:

### 1. Validação Robusta de URI (`URIUtils.java`)

Adicionado novo método `isWellFormedURI()` que valida URIs de acordo com RFC 3986:

```java
public static boolean isWellFormedURI(String uriString) {
    if (uriString == null || uriString.trim().isEmpty()) {
        return false;
    }
    try {
        URI uri = new URI(uriString);
        return true;
    } catch (Exception e) {
        return false;
    }
}
```

### 2. Sanitização na Ingestão (`SDDAttributeGenerator.java`)

Atualizado o método `getUnit()` para:
- Fazer trim de whitespace (já estava implementado)
- Validar URIs completas após conversão de abreviações
- Registrar warnings para URIs malformadas
- Retornar string vazia ao invés de armazenar URI inválida

```java
private String getUnit(Record rec) {
    String original = rec.getValueByColumnName(mapCol.get("Unit"));
    if (original == null || original.isEmpty()) {
        return "";
    }
    // Trim whitespace including newlines to prevent malformed URIs
    original = original.trim();
    
    // First check if it's already a valid URI
    if (URIUtils.isValidURI(original)) {
        // Convert abbreviated URI to full URI
        String fullUri = URIUtils.convertToWholeURI(original);
        // Validate the full URI is well-formed
        if (URIUtils.isWellFormedURI(fullUri)) {
            return original;
        } else {
            System.out.println("[WARNING] SDDAttributeGenerator.getUnit(): Malformed URI after conversion: " + fullUri);
            System.out.println("[WARNING]   Original value: '" + original + "'");
            return "";
        }
    } else if (codeMap.containsKey(original)) {
        // Validate mapped URI
        String mappedUri = codeMap.get(original);
        String fullUri = URIUtils.convertToWholeURI(mappedUri);
        if (URIUtils.isWellFormedURI(fullUri)) {
            return codeMap.get(original);
        } else {
            System.out.println("[WARNING] SDDAttributeGenerator.getUnit(): Malformed URI in codeMap: " + fullUri);
            return "";
        }
    }
    return "";
}
```

### 3. Validação na Recuperação de Labels (`FirstLabel.java`)

Adicionada validação ANTES de inserir URI em query SPARQL:

```java
private static String retrieveLabel(String uri, String namedGraph, boolean useInMemoryModel) {
    if ((uri == null) || (uri.equals(""))) {
        log.warn("an empty URI is given for retrieving it label.");
        return "";
    }

    // Validate URI is well-formed before using in SPARQL query
    String uriToCheck = uri.startsWith("<") ? uri.substring(1, uri.length() - 1) : uri;
    if (!URIUtils.isWellFormedURI(uriToCheck)) {
        log.warn("Malformed URI provided for label retrieval: " + uri);
        log.warn("  This URI has invalid syntax (e.g., improper percent-encoding)");
        return "";
    }
    // ... rest of the method
}
```

### 4. Tratamento de Erro na Recuperação (`SDDAttribute.java`)

Tratamento de exceção já estava implementado:

```java
public void setUnit(String unit) {
    this.unit = unit;
    if (unit == null || unit.equals("")) {
        this.unitLabel = "";
    } else {
        try {
            this.unitLabel = FirstLabel.getPrettyLabel(unit);
        } catch (Exception e) {
            System.out.println("[WARNING] SDDAttribute.setUnit(): Could not get label for unit URI: " + unit);
            System.out.println("[WARNING]   Error: " + e.getMessage());
            this.unitLabel = "";
        }
    }
}
```

Proteção similar já estava em `setEntity()` e `setAttribute()`.

### 5. Proteção na Geração de SDD (`SDDGen.java`)

Adicionado try-catch ao redor das operações de geração para evitar falha completa:

```java
if (targetSdd != null) {
    try {
        addDictionaryMappingData(helper, targetSdd);
    } catch (Exception e) {
        System.out.println("[SDDGen] ERROR while adding Dictionary Mapping data:");
        System.out.println("[SDDGen]   " + e.getClass().getName() + ": " + e.getMessage());
        System.out.println("[SDDGen]   This may be due to malformed URIs in the triplestore.");
        System.out.println("[SDDGen]   Continuing with partial data...");
    }
    // Similar try-catch for Codebook and Timeline
}
```

## Como Corrigir o Problema Atual

Para corrigir o SDD que já está com dados malformados no triplestore:

### Opção 1: Deletar e Reingerir (Recomendado)

1. Abra a interface web do HADatAc
2. Vá até a lista de SDDs
3. Encontre o SDD "SDD_WEATHER_STATION"  
4. Clique em "Delete" para remover completamente do triplestore
5. Certifique-se de que o arquivo Excel não tem espaços extras nos campos (especialmente Unit)
6. Faça upload do arquivo Excel novamente
7. Faça a ingestão novamente

### Opção 2: Correção Manual via SPARQL

Execute a seguinte query SPARQL UPDATE no Fuseki:

```sparql
PREFIX hasco: <http://hadatac.org/ont/hasco/>
PREFIX unit: <http://qudt.org/vocab/unit/>

DELETE {
  ?attr hasco:hasUnit <http://qudt.org/vocab/unit/MicroGM-PER-M3%A> .
}
INSERT {
  ?attr hasco:hasUnit unit:MicroGM-PER-M3 .
}
WHERE {
  ?attr a hasco:SDDAttribute .
  ?attr hasco:hasUnit <http://qudt.org/vocab/unit/MicroGM-PER-M3%A> .
}
```

**NOTA**: Esta query pode falhar se a URI malformada não puder ser parseada. Neste caso, use a Opção 1.

### Opção 3: Limpeza Completa do Named Graph

Se houver muitos problemas no SDD:

```sparql
DROP GRAPH <https://hadatac.org/ont/hadatac#/DFL1773851870316631>
```

Depois reingira o arquivo corrigido.

## Prevenção Futura

Com as correções implementadas:

1. **Na Ingestão**: 
   - Valores são trimmed para remover espaços/newlines
   - URIs são validadas antes de serem armazenadas
   - URIs malformadas são rejeitadas com warning

2. **Na Recuperação**: 
   - URIs são validadas antes de serem usadas em queries SPARQL
   - Exceptions são tratadas graciosamente
   - Logs informativos são gerados para debugging

3. **Na Geração**: 
   - Try-catch evita falha completa do processo
   - Dados parciais podem ser gerados mesmo com alguns problemas
   - Mensagens claras indicam a origem do problema

## Teste

Após corrigir:

1. Compile o projeto: `sbt compile`
2. Reinicie o servidor: `sbt run`
3. Delete o SDD problemático (se existir)
4. Certifique-se que o Excel está limpo (sem espaços extras)
5. Faça upload e ingestão novamente
6. Tente gerar o SDD
7. O sistema deve funcionar sem erros, ou gerar warnings informativos

## Observações

- As mudanças são retrocompatíveis
- SDDs existentes sem problemas continuarão funcionando normalmente
- Novos SDDs estarão protegidos contra URIs malformadas
- Mesmo com URIs malformadas no triplestore, o sistema não crashará completamente
- Logs mais informativos facilitam debugging futuro
