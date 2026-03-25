# Guia de Testes de Setup de Dados SOC

Este guia explica como usar os testes de setup de dados para criar elementos SOC no banco de dados para testes.

## Visão Geral

O sistema pode adicionar **50 elementos SOC LINKED** aos elementos já existentes no banco:

1. **50 Elementos LINKED** (com named graph context)
   - Inseridos no named graph: `http://hadatac.org/ont/arrowhead/DataFile/LTE-PIAGET-LOCATION-TEST-DF`
   - Visíveis para o código de geração padrão
   - Padrão de originalID: `{ZONE}-{LEVEL}` (ex: `LIBRARY-L0`)
   - **IMPORTANTE:** O teste verifica se já existem 50 elementos. Se sim, NÃO deleta nem adiciona mais.

2. **50 Elementos ORPHAN** (sem named graph context) - EM DESENVOLVIMENTO
   - Inseridos no default graph (sem GRAPH clause)
   - Invisíveis para queries `GRAPH ?g { ... }`
   - Padrão de originalID: `extra-{ZONE}-{LEVEL}` (ex: `extra-LIBRARY-L0`)
   - **NOTA:** Esta funcionalidade está com problemas técnicos no Blazegraph

## Comportamento Atual

### Se você já tem 50 elementos no banco:
- ✅ LinkedSOCElementsSetupTest **detecta** e **NÃO adiciona** mais
- ✅ Mantém seus 50 elementos originais intactos
- ✅ Testes validam os dados existentes

### Se você não tem elementos ou tem menos de 50:
- ✅ LinkedSOCElementsSetupTest **adiciona** elementos até ter 50
- ✅ Não deleta elementos existentes

## Scripts Disponíveis

### 1. Limpar Dados Existentes
```powershell
.\cleanup-test-data.ps1
```
Remove TODOS os dados de teste do banco de dados.

### 2. Executar Teste LinkedSOC (Recomendado)
```powershell
sbt "testOnly org.hascoapi.tests.LinkedSOCElementsSetupTest"
```
Executa o teste que:
- Verifica quantos elementos linked já existem
- Se tem menos de 50, adiciona até completar 50
- Se já tem 50 ou mais, apenas valida os dados existentes
- **NÃO deleta** seus dados originais

### 3. Limpar Apenas Dados de Teste (Se Necessário)
```powershell
.\cleanup-test-data.ps1
```
Remove APENAS os dados de teste (named graph de teste).
**ATENÇÃO:** Isso NÃO afeta seus 50 elementos originais se estiverem em outro named graph.

### 3. Executar Manualmente

#### LinkedSOC (50 elementos com named graph):
```powershell
sbt "testOnly org.hascoapi.tests.LinkedSOCElementsSetupTest"
```

#### OrphanSOC (50 elementos sem named graph):
```powershell
sbt "testOnly org.hascoapi.tests.OrphanSOCElementsSetupTest"
```

## Estrutura dos Dados

### Zonas (10):
- LIBRARY
- SCIENCE
- ARTS
- SPORTS
- ADMIN
- CAFE
- MAIN
- ANNEX-A
- ANNEX-B
- GREENHOUSE

### Níveis (5):
- L0
- L1
- L2
- L3
- ROOF

**Total:** 10 zonas × 5 níveis = 50 elementos por tipo = **100 elementos total**

## Location Types

Os elementos são classificados por tipo de localização:

1. **Outdoor** (10 elementos): Todos os níveis ROOF
2. **Laboratory** (6 elementos): GREENHOUSE ou SCIENCE nos níveis L0/L1/L2
3. **Indoor** (34 elementos): Todos os outros

## Verificação no Frontend

Após executar os testes, acesse:
```
http://localhost:9000/hadatac/soc
```

Você deve ver:
- **OCL_LTE-PIAGET-LOCATION**: 100 elementos (50 linked + 50 orphan)
- **OCL_LTE-PIAGET-LOCATION-TYPE**: 3 tipos (indoor, outdoor, laboratory)

## Queries de Verificação SPARQL

### Contar Elementos Linked:
```sparql
SELECT (COUNT(*) AS ?c) WHERE {
  GRAPH <http://hadatac.org/ont/arrowhead/DataFile/LTE-PIAGET-LOCATION-TEST-DF> {
    ?e <http://hadatac.org/ont/hasco/isMemberOf> 
       <http://hadatac.org/ont/arrowhead/LTE-PIAGET-LOCATION>
  }
}
```
**Esperado:** 50

### Contar Elementos Orphan:
```sparql
SELECT (COUNT(?e) AS ?c) WHERE {
  ?e <http://hadatac.org/ont/hasco/isMemberOf> 
     <http://hadatac.org/ont/arrowhead/LTE-PIAGET-LOCATION> .
  ?e <http://hadatac.org/ont/hasco/originalID> ?id .
  FILTER (STRSTARTS(STR(?id), "extra-"))
  FILTER NOT EXISTS {
    GRAPH ?g {
      ?e <http://hadatac.org/ont/hasco/isMemberOf> 
         <http://hadatac.org/ont/arrowhead/LTE-PIAGET-LOCATION>
    }
  }
}
```
**Esperado:** 50

### Contar TODOS os Elementos:
```sparql
SELECT (COUNT(DISTINCT ?e) AS ?c) WHERE {
  {
    GRAPH ?g {
      ?e <http://hadatac.org/ont/hasco/isMemberOf> 
         <http://hadatac.org/ont/arrowhead/LTE-PIAGET-LOCATION>
    }
  } UNION {
    ?e <http://hadatac.org/ont/hasco/isMemberOf> 
       <http://hadatac.org/ont/arrowhead/LTE-PIAGET-LOCATION> .
    FILTER NOT EXISTS {
      GRAPH ?g {
        ?e <http://hadatac.org/ont/hasco/isMemberOf> 
           <http://hadatac.org/ont/arrowhead/LTE-PIAGET-LOCATION>
      }
    }
  }
}
```
**Esperado:** 100

## Troubleshooting

### Problema: Ainda vejo apenas 50 elementos
**Solução:** Execute a limpeza e rode os testes novamente:
```powershell
.\cleanup-test-data.ps1
.\run-setup-tests.ps1
```

### Problema: Testes falham com erro de DELETE
**Solução:** Isso é normal se não há dados para deletar. Os testes continuarão e inserirão novos dados.

### Problema: OrphanSOCElements não são inseridos
**Solução:** Verifique se o Blazegraph permite INSERT no default graph. Os testes foram ajustados para usar `INSERT { ... } WHERE {}` em vez de `INSERT DATA`.

## Manutenção

Para **manter os dados** no banco após os testes:
- Os testes estão configurados com `@AfterAll` comentado
- Os dados permanecem para verificação no frontend
- Para limpar: execute `.\cleanup-test-data.ps1`

Para **limpar automaticamente** após os testes:
- Descomente o `@AfterAll` em ambos os arquivos de teste
- Os dados serão removidos ao final de cada suite de testes

