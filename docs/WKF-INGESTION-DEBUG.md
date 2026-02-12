# WKF Ingestion Path Debug

## Problema Identificado

O WKF não está conseguindo fazer ingestion porque o arquivo não está sendo encontrado no path correto.

## Logs de Erro

```
IngestionAPI.ingest(): Looking for file at path: C:\hascoapi\var\WKF-WeatherStation.xlsx
[ERROR] IngestionAPI.ingest(): Uploaded file not found at: C:\hascoapi\var\WKF-WeatherStation.xlsx
```

## Path Esperado

O path correto deveria ser:
```
C:\hascoapi\var\resources\DFL{id}\WKF-WeatherStation.xlsx
```

## Alterações Feitas

Adicionei logs de debug em `IngestionAPI.java` para rastrear a construção do path:

1. **Linha ~151** - Primeiro check de arquivo pré-uploadado
2. **Linha ~231** - Segundo check quando o arquivo não foi encontrado

### Logs Adicionados:

```java
System.out.println("[DEBUG-FIRST] IngestionAPI.ingest(): basePath = " + basePath);
System.out.println("[DEBUG-FIRST] IngestionAPI.ingest(): Constants.RESOURCE_FOLDER = " + Constants.RESOURCE_FOLDER);
System.out.println("[DEBUG-FIRST] IngestionAPI.ingest(): uriTerm = " + uriTerm);
System.out.println("[DEBUG-FIRST] IngestionAPI.ingest(): dataFile.getFilename() = " + dataFile.getFilename());
```

E:

```java
System.out.println("[DEBUG] IngestionAPI.ingest(): basePath = " + basePath);
System.out.println("[DEBUG] IngestionAPI.ingest(): Constants.RESOURCE_FOLDER = " + Constants.RESOURCE_FOLDER);
System.out.println("[DEBUG] IngestionAPI.ingest(): uriTerm = " + uriTerm);
System.out.println("[DEBUG] IngestionAPI.ingest(): dataFile.getFilename() = " + dataFile.getFilename());
```

## Próximos Passos

1. Reiniciar a aplicação
2. Criar um novo WKF
3. Verificar os logs de debug para identificar onde o path está sendo construído incorretamente
4. Comparar com INS (que funciona) para encontrar a diferença

## Código de Construção do Path

O path é construído usando:
```java
Path uploadedFilePath = Paths.get(basePath, Constants.RESOURCE_FOLDER, uriTerm, dataFile.getFilename());
```

Onde:
- `basePath` = configuração `hascoapi.paths.ingestion` (geralmente `C:\hascoapi\var`)
- `Constants.RESOURCE_FOLDER` = `"resources/"`
- `uriTerm` = último segmento do URI do DataFile (ex: `DFL1770739867864311`)
- `dataFile.getFilename()` = nome do arquivo (ex: `WKF-WeatherStation.xlsx`)

## Data

2026-02-10
