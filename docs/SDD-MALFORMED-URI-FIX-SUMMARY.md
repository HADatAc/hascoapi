# Resumo da Correção: URIs Malformadas no SDD

## Problema

O sistema estava falhando ao tentar gerar um arquivo Excel a partir de um SDD armazenado no triplestore. O erro era:

```
org.apache.jena.query.QueryException: Bad IRI: 'http://qudt.org/vocab/unit/MicroGM-PER-M3%A'
```

## Causa

O campo Unit no arquivo Excel original tinha um espaço ou newline após o valor:
```
unit:MicroGM-PER-M3 
```

Quando convertido para URI completa, resultou em:
```
http://qudt.org/vocab/unit/MicroGM-PER-M3%A
```

O `%A` é uma codificação percentual incompleta/inválida, violando o RFC 3986.

## Solução Implementada

### Camadas de Proteção Adicionadas:

1. **Validação de URI (URIUtils.java)**
   - Novo método `isWellFormedURI()` que valida sintaxe RFC 3986
   - Usa `java.net.URI` para validação rigorosa

2. **Sanitização na Ingestão (SDDAttributeGenerator.java)**
   - `getUnit()` agora valida URIs após conversão
   - Rejeita URIs malformadas com warning ao invés de armazená-las
   - Retorna string vazia para URIs inválidas

3. **Validação na Recuperação (FirstLabel.java)**
   - Valida URI ANTES de inserir em query SPARQL
   - Previne QueryException por URIs malformadas
   - Retorna label vazia ao invés de falhar

4. **Tratamento de Exceções (SDDAttribute.java)**
   - Já existia try-catch em setUnit(), setEntity(), setAttribute()
   - Garante que exceções sejam capturadas graciosamente

5. **Proteção na Geração (SDDGen.java)**
   - Try-catch ao redor de addDictionaryMappingData()
   - Try-catch ao redor de addCodebookData()
   - Try-catch ao redor de addTimelineData()
   - Sistema continua com dados parciais ao invés de falhar completamente

## Comportamento Atual

### Antes da Correção:
- ❌ Ingestão aceita URIs malformadas
- ❌ Geração falha completamente com QueryException
- ❌ Nenhum arquivo Excel é gerado

### Depois da Correção:
- ✅ Ingestão rejeita URIs malformadas (warning no log)
- ✅ Geração tenta recuperar dados parciais
- ✅ Se houver URIs malformadas já no triplestore, gera aviso mas não falha
- ✅ Arquivo Excel pode ser gerado com dados disponíveis

## Para Usuários

### Se você tem um SDD com problemas no triplestore:

1. **Opção Recomendada**: Delete e reingira
   - Acesse a interface web
   - Delete o SDD problemático
   - Corrija o arquivo Excel (remova espaços extras)
   - Faça upload e ingestão novamente

2. **Opção Avançada**: Correção manual via SPARQL UPDATE no Fuseki

3. **Nova Ingestão**: Com as correções, novos SDDs não terão este problema

## Arquivos Modificados

1. `app/org/hascoapi/utils/URIUtils.java` - Novo método de validação
2. `app/org/hascoapi/utils/FirstLabel.java` - Validação antes de queries
3. `app/org/hascoapi/ingestion/SDDAttributeGenerator.java` - Validação na ingestão
4. `app/org/hascoapi/transform/mt/sdd/SDDGen.java` - Try-catch na geração
5. `docs/FIX-MALFORMED-URI-ISSUE.md` - Documentação detalhada

## Teste

✅ Compilação bem-sucedida (sbt compile)
✅ Sem erros de compilação
✅ Apenas warnings de estilo (não afetam funcionalidade)

## Próximos Passos

1. Reinicie o servidor HADatAc
2. Teste com o arquivo Excel problemático
3. Verifique os logs para warnings sobre URIs malformadas
4. Se necessário, delete e reingira o SDD

