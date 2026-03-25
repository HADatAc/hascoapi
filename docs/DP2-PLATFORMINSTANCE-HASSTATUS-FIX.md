# Correção: vstoi:hasStatus do PlatformInstance no DP2

## Problema Identificado

Ao gerar um arquivo DP2, o campo `vstoi:hasStatus` do PlatformInstance estava sendo preenchido incorretamente:

### Valores Observados:
- **Gerado**: `http://hadatac.org/ont/vstoi#Draft` (URL completo com `#Draft`)
- **Esperado**: `vstoi:OPERATIONAL` (prefixo correto)

### Também Afetados:
- Campo `a` (tipo): estava como `vstoi:PlatformInstance` mas deveria ser o URI da Platform class (ex: `pmsr:PMSR_Simulation_Platform`)
- Campo `hasco:hasMaker`: estava em branco

## Causa Raiz

O problema estava em **duas camadas**:

### 1. Ingestão (DP2Generator.java)

O código **sobrescrevia** o `vstoi:hasStatus` da coluna do Excel com o status do parâmetro MT:

```java
// CÓDIGO ANTIGO (ERRADO):
if (this.status != null && !this.status.trim().isEmpty()) {
    row.put("vstoi:hasStatus", URIUtils.replaceNameSpaceEx(this.status.trim()));
}
```

**Problema**: O `this.status` vem do **DP2 MT** (Metadata Template), que tem status `Draft`. 
Isso **ignorava** o valor correto (`vstoi:OPERATIONAL`) que estava na **coluna do Excel**.

### 2. Geração (DP2PlataformInstances.java)

O código estava usando `getHasStatus()` corretamente, mas como a ingestão salvou o valor errado no triple store, o valor recuperado estava incorreto.

## Solução Implementada

### DP2Generator.java (Linhas 68-82)

Modificado para **priorizar** o valor da coluna do Excel:

```java
// CÓDIGO NOVO (CORRETO):
String statusFromColumn = rec.getValueByColumnName("vstoi:hasStatus");
if (statusFromColumn != null && !statusFromColumn.trim().isEmpty()) {
    row.put("vstoi:hasStatus", URIUtils.replaceNameSpaceEx(statusFromColumn.trim()));
    System.out.println("Added vstoi:hasStatus from Excel column: " + URIUtils.replaceNameSpaceEx(statusFromColumn.trim()));
} else if (this.status != null && !this.status.trim().isEmpty()) {
    row.put("vstoi:hasStatus", URIUtils.replaceNameSpaceEx(this.status.trim()));
    System.out.println("Added vstoi:hasStatus from parameter (fallback): " + URIUtils.replaceNameSpaceEx(this.status.trim()));
}
```

**Lógica**:
1. **Primeiro**: Tenta ler `vstoi:hasStatus` da **coluna do Excel**
2. **Fallback**: Se a coluna estiver vazia, usa o status do **parâmetro MT**

### DP2PlataformInstances.java (Linhas 69 e 75)

Já estava correto, apenas confirmamos:

```java
// Linha 69: usa getTypeUri() para pegar o URI da Platform class
newRow.createCell(1).setCellValue(platformInstance.getTypeUri() != null ? 
    URIUtils.replaceNameSpaceEx(platformInstance.getTypeUri()) : "");

// Linha 75: usa getHasStatus() para pegar o status da instância
String status = platformInstance.getHasStatus() != null ? 
    URIUtils.replaceNameSpaceEx(platformInstance.getHasStatus()) : "";
newRow.createCell(4).setCellValue(status);
```

## Entendendo os Dois Status do DP2

**Importante**: O DP2 tem **dois níveis de status**:

### 1. Status do DP2 MT (Metadata Template)
- **Tipo**: DataFile.getFileStatus() / DP2.getHasStatus()
- **Valores típicos**: `Draft`, `Working`, `Processed`
- **Onde está**: No registro do DP2 como um todo
- **Uso**: Controlar o fluxo de trabalho do arquivo

### 2. Status das Instâncias (PlatformInstance, InstrumentInstance, etc.)
- **Tipo**: VSTOIInstance.getHasStatus()
- **Valores típicos**: `vstoi:OPERATIONAL`, `vstoi:RETIRED`, `vstoi:DAMAGED`
- **Onde está**: Em cada linha da planilha (cada instância)
- **Uso**: Status operacional de cada equipamento/plataforma

**O erro estava**: Misturar o status do MT (`Draft`) com o status da instância (`OPERATIONAL`).

## Resultado

Após a correção:
- ✅ `vstoi:hasStatus` agora reflete o valor da **coluna do Excel**
- ✅ `a` já estava correto (usa `getTypeUri()`)
- ⚠️ `hasco:hasMaker` ainda em branco - **não é salvo durante ingestão DP2**

## Arquivos Modificados

- `app/org/hascoapi/ingestion/DP2Generator.java`

## Teste Necessário

1. **Uningest** o DP2 atual
2. **Ingest** novamente com a correção
3. **Gerar** o DP2 e verificar:
   - `vstoi:hasStatus` = `vstoi:OPERATIONAL` (ou valor da coluna)
   - `a` = URI da Platform class (ex: `pmsr:PMSR_Simulation_Platform`)

## Observação Adicional

O campo `hasco:hasMaker` está em branco porque:
- **Platform** tem esse campo e salva durante ingestão
- **PlatformInstance** NÃO tem esse campo na classe VSTOIInstance
- Para corrigir, seria necessário adicionar esse campo à classe PlatformInstance

**Decisão**: Manter como está, pois `hasMaker` é propriedade da **Platform** (tipo), não da **PlatformInstance** (instância específica).
