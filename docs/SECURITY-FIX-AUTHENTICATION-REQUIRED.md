# Fix de Segurança: Autenticação Obrigatória nos Endpoints de API

## Problema Identificado

Foi identificado que endpoints críticos de criação, modificação e remoção de dados **não estavam protegidos por autenticação**. Isso permitia que qualquer pessoa fizesse requisições sem fornecer credenciais, representando uma **grave falha de segurança**.

### Exemplo do Problema
```
GET /hascoapi/api/instrument/create/{{json}}
Authorization: No Auth

Resposta: 
{
    "isSuccessful": false,
    "body": "Field 'label' is required but was not provided."
}
```

O sistema estava processando a requisição e validando os campos mesmo **sem autenticação**, quando deveria retornar **401 Unauthorized** imediatamente.

## Solução Implementada

Adicionamos a anotação `@Secure` do PAC4J aos endpoints críticos, exigindo autenticação via token JWT no header `Authorization: Bearer <token>`.

### Arquivos Modificados

#### 1. SIRElementAPI.java
- **Método `createElement()`**: Endpoint genérico de criação de elementos
- **Método `deleteElement()`**: Endpoint genérico de remoção de elementos

```java
@Secure(clients = "HeaderClient", authorizers = "authenticated")
public Result createElement(String elementType, String json) {
    // ...
}

@Secure(clients = "HeaderClient", authorizers = "authenticated")
public Result deleteElement(String elementType, String uri) {
    // ...
}
```

**Rotas protegidas:**
- `POST /hascoapi/api/:elementType/create/:json`
- `GET /hascoapi/api/:elementType/create/:json` 
- `POST /hascoapi/api/:elementType/delete/:uri`
- `GET /hascoapi/api/:elementType/delete/:uri`

**Tipos de elementos afetados:**
instrument, detector, codebook, annotation, entity, attribute, unit, study, deployment, etc.

#### 2. IngestionAPI.java
- **Método `ingest()`**: Upload e processamento de arquivos de dados
- **Método `uningestDataFile()`**: Remoção de arquivos de dados
- **Método `uningestMetadataTemplate()`**: Remoção de templates de metadados

```java
@Secure(clients = "HeaderClient", authorizers = "authenticated")
public Result ingest(String status, String elementType, String elementUri, Http.Request request) {
    // ...
}

@Secure(clients = "HeaderClient", authorizers = "authenticated")
public Result uningestDataFile(String dataFileUri) {
    // ...
}

@Secure(clients = "HeaderClient", authorizers = "authenticated")
public Result uningestMetadataTemplate(String metadataTemplateUri) {
    // ...
}
```

**Rotas protegidas:**
- `POST /hascoapi/api/ingest/:status/:elementType/:elementUri`
- `GET /hascoapi/api/uningest/:dataFileUri`
- `GET /hascoapi/api/uningest/mt/:metadataTemplateUri`

#### 3. DataFileAPI.java
- **Método `uploadFile()`**: Upload de arquivos para elementos
- **Método `uploadMedia()`**: Upload de arquivos de mídia
- **Método `downloadFile()`**: Download de arquivos

```java
@Secure(clients = "HeaderClient", authorizers = "authenticated")
public Result uploadFile(String elementUri, String filename, Http.Request request) {
    // ...
}

@Secure(clients = "HeaderClient", authorizers = "authenticated")
public Result uploadMedia(String foldername, String filename, Http.Request request) {
    // ...
}

@Secure(clients = "HeaderClient", authorizers = "authenticated")
public Result downloadFile(String elementUri, String filename) {
    // ...
}
```

**Rotas protegidas:**
- `POST /hascoapi/api/uploadFile/:elementuri/:filename`
- `POST /hascoapi/api/uploadMedia/:foldername/:filename`
- `POST /hascoapi/api/downloadFile/:elementuri/:filename`

## Comportamento Após a Correção

### Requisição SEM Autenticação (401 Unauthorized)
```
GET /hascoapi/api/instrument/create/{{json}}
Authorization: No Auth

HTTP/1.1 401 Unauthorized
Content-Type: text/html

<!DOCTYPE html>
<html>
<body>
<h1>Unauthorized operation</h1>
<p>You do not have permission to access this page</p>
<br/>
<a href="/">Home</a>
</body>
</html>
```

### Requisição COM Autenticação Válida (200 OK)
```
GET /hascoapi/api/instrument/create/{{validJson}}
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

HTTP/1.1 200 OK
Content-Type: application/json

{
    "isSuccessful": true,
    "body": "Instrument <uri> has been CREATED."
}
```

## Infraestrutura de Segurança Utilizada

O projeto já possuía o framework **PAC4J** configurado com:
- **SecurityModule**: Configuração de clientes e autorizadores
- **HeaderClient**: Cliente que valida tokens JWT no header `Authorization: Bearer`
- **AuthenticatedAuthorizer**: Verifica se o usuário está autenticado
- **JwtAuthenticator**: Valida a assinatura do token JWT usando secret configurado

**Configuração do JWT Secret:**
```properties
# application.conf
pac4j.jwt.secret="qwertyuiopasdfghjklzxcvbnm123456"
```

## Endpoints que Permanecem Públicos (GET apenas)

Os endpoints de consulta/leitura continuam acessíveis sem autenticação:
- `GET /hascoapi/api/:elementType/elements/:pageSize/:offset` - Listar elementos
- `GET /hascoapi/api/uri/:uri` - Consultar por URI
- `GET /hascoapi/api/instrument/totext/plain/:uri` - Renderizações
- E outros endpoints de consulta

## Testes Necessários

### 1. Testar endpoints protegidos SEM autenticação
Deve retornar **401 Unauthorized**

### 2. Testar endpoints protegidos COM token inválido  
Deve retornar **401 Unauthorized**

### 3. Testar endpoints protegidos COM token válido
Deve retornar **200 OK** (ou código apropriado)

### 4. Testar endpoints de consulta (GET) sem autenticação
Deve continuar funcionando (**200 OK**)

## Impacto

- **Segurança**: Corrige grave vulnerabilidade que permitia operações não autorizadas
- **Compatibilidade**: Clientes precisarão incluir token JWT no header `Authorization: Bearer <token>`
- **Performance**: Overhead mínimo da validação do token JWT

## Próximos Passos Recomendados

1. ✅ Adicionar autenticação nos endpoints de criação e deleção
2. ⚠️ Considerar adicionar autorização por papéis (roles) para operações administrativas
3. ⚠️ Implementar rate limiting para prevenir abuso
4. ⚠️ Adicionar logging de todas as operações de criação/modificação/deleção
5. ⚠️ Revisar outros controllers para identificar endpoints sensíveis

## Data da Implementação

**Data**: 07/04/2026  
**Autor**: GitHub Copilot  
**Revisão**: Pendente

