# PROBLEMA CRÍTICO: Arquitetura Dual-Layer Não Está Sendo Criada

## Data
2026-04-29

## Problema Identificado

O diagnóstico revelou que **não há arquitetura dual-layer** sendo criada durante a ingestão do DSG. Os objetos VSTOI (Instruments, Components, etc.) **não têm instâncias separadas**.

### O Que Está Acontecendo (ERRADO)

```turtle
# MESMA URI para tudo - SEM dual-layer!
pmsr:OBJ_instrumentcollection_INS1739301009974715
    a hasco:StudyObject , vstoi:Instrument ;
    rdfs:label "ARTEC LEO Scanner" ;
    hasco:originalID "INS1739301009974715" ;
    hasco:isMemberOf pmsr:OCL_SOC-INSTRUMENT-PMSR ;
    # ❌ SEM vstoi:hasInstrument
    # ❌ Todas as propriedades no mesmo recurso
    hasco:hasWebDocument <...> ;
    rdfs:comment "..." .
```

### O Que DEVERIA Acontecer (CORRETO)

```turtle
# Camada 1: StudyObject (base)
pmsr:OBJ_instrumentcollection_INS1739301009974715
    a hasco:StudyObject ;
    rdfs:label "ARTEC LEO Scanner" ;
    hasco:originalID "INS1739301009974715" ;
    hasco:isMemberOf pmsr:OCL_SOC-INSTRUMENT-PMSR ;
    # ✅ Link para instância VSTOI
    vstoi:hasInstrument pmsr:INST-INS1739301009974715 .

# Camada 2: Instrument VSTOI (especializada)
pmsr:INST-INS1739301009974715
    a vstoi:Instrument ;
    rdfs:label "ARTEC LEO Scanner" ;
    hasco:hascoType vstoi:Instrument ;
    # ✅ Propriedades específicas aqui
    hasco:hasWebDocument <...> ;
    rdfs:comment "..." .
```

## Causa Raiz

### Arquivo: StudyObjectGenerator.java
### Métodos Problemáticos:

```java
private void createInstrumentFromStudyObjectWithoutDelete(StudyObject so) {
    Instrument instrument = new Instrument();
    
    // ❌ PROBLEMA: Usando a MESMA URI!
    instrument.setUri(so.getUri());  
    
    instrument.setTypeUri(so.getTypeUri());
    instrument.setHascoTypeUri(VSTOI.INSTRUMENT);
    instrument.setLabel(so.getLabel());
    instrument.setComment(so.getComment());
    instrument.setNamedGraph(getNamedGraphUri());
    instrument.setHasSIRManagerEmail(so.getHasSIRManagerEmail());
    
    // ❌ Salva com a mesma URI, sem criar link
    instrument.saveToTripleStore(true, false);
}
```

Mesma lógica errada para:
- `createComponentFromStudyObjectWithoutDelete()`
- `createComponentStemFromStudyObjectWithoutDelete()`
- `createContainerSlotFromStudyObjectWithoutDelete()`
- `createCodebookFromStudyObjectWithoutDelete()`
- `createResponseOptionFromStudyObjectWithoutDelete()`

## Solução Necessária

### 1. Criar URIs Separadas para Instâncias VSTOI

```java
private void createInstrumentFromStudyObjectWithoutDelete(StudyObject so) {
    // ✅ Criar URI DIFERENTE para a instância VSTOI
    String vstoiUri = generateVSTOIUri(so.getUri(), "INST");
    
    Instrument instrument = new Instrument();
    instrument.setUri(vstoiUri);  // ✅ URI separada
    instrument.setTypeUri(VSTOI.INSTRUMENT);
    instrument.setHascoTypeUri(VSTOI.INSTRUMENT);
    instrument.setLabel(so.getLabel());
    instrument.setComment(so.getComment());
    instrument.setNamedGraph(getNamedGraphUri());
    instrument.setHasSIRManagerEmail(so.getHasSIRManagerEmail());
    
    // Salvar a instância VSTOI
    instrument.saveToTripleStore(true, false);
    
    // ✅ Adicionar link no StudyObject
    addVSTOILinkToStudyObject(so.getUri(), vstoiUri, "vstoi:hasInstrument");
}
```

### 2. Método Auxiliar para Gerar URIs

```java
private String generateVSTOIUri(String studyObjectUri, String prefix) {
    // Converter OBJ_xxx para INST-xxx, COMP-xxx, etc.
    // Exemplo:
    // pmsr:OBJ_instrumentcollection_INS1739301009974715
    // → pmsr:INST-INS1739301009974715
    
    String uri = studyObjectUri;
    if (uri.contains("OBJ_")) {
        // Extrair a parte após OBJ_
        String suffix = uri.substring(uri.indexOf("OBJ_") + 4);
        // Remover o prefixo de coleção (instrumentcollection_, etc.)
        if (suffix.contains("_")) {
            suffix = suffix.substring(suffix.indexOf("_") + 1);
        }
        // Construir nova URI
        String namespace = uri.substring(0, uri.indexOf("OBJ_"));
        uri = namespace + prefix + "-" + suffix;
    }
    return uri;
}
```

### 3. Método para Adicionar Link VSTOI

```java
private void addVSTOILinkToStudyObject(String studyObjectUri, String vstoiUri, String property) {
    // Adicionar tripla: <studyObjectUri> <property> <vstoiUri>
    String insert = NameSpaces.getInstance().printSparqlNameSpaceList();
    insert += "INSERT DATA { \n";
    insert += "  GRAPH <" + getNamedGraphUri() + "> { \n";
    insert += "    <" + studyObjectUri + "> " + property + " <" + vstoiUri + "> . \n";
    insert += "  } \n";
    insert += "}";
    
    UpdateRequest request = UpdateFactory.create(insert);
    UpdateProcessor processor = UpdateExecutionFactory.createRemote(
            request, CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_UPDATE));
    processor.execute();
}
```

## Impacto

### Sem a Correção:
- ❌ DA-SOC adiciona propriedades ao StudyObject (lugar errado)
- ❌ Não há separação entre camada base e camada especializada
- ❌ Queries VSTOI não funcionam corretamente
- ❌ Frontend não consegue distinguir propriedades específicas de VSTOI

### Com a Correção:
- ✅ DA-SOC adiciona propriedades à instância VSTOI (lugar certo)
- ✅ Arquitetura dual-layer funcional
- ✅ Queries VSTOI retornam dados corretos
- ✅ Frontend mostra propriedades específicas de cada tipo

## Próximos Passos

1. **Modificar StudyObjectGenerator.java**
   - Implementar `generateVSTOIUri()`
   - Implementar `addVSTOILinkToStudyObject()`
   - Atualizar todos os métodos `create*FromStudyObjectWithoutDelete()`

2. **Testar**
   - Re-ingerir DSG-PMSR-Simulators-fixed.xlsx
   - Verificar que links `vstoi:hasInstrument` são criados
   - Verificar que instâncias VSTOI têm URIs separadas

3. **Re-ingerir DA-SOC**
   - Após arquitetura dual-layer estar correta
   - DA-SOC irá adicionar propriedades às instâncias VSTOI
   - Propriedades aparecerão no lugar certo

## Arquivos a Modificar

- `app/org/hascoapi/ingestion/StudyObjectGenerator.java`
  - Métodos: `createInstrumentFromStudyObjectWithoutDelete()` e similares
  - Adicionar: `generateVSTOIUri()`, `addVSTOILinkToStudyObject()`

## Prioridade

🔴 **CRÍTICO** - Sem isso, toda a arquitetura VSTOI não funciona corretamente

