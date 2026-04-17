# 🧪 Guia Completo: Teste de DA Ingestion e DSG Generation com DASOCs

## 📋 Índice
1. [Visão Geral](#visão-geral)
2. [Pré-requisitos](#pré-requisitos)
3. [Fluxo de Teste Completo](#fluxo-de-teste-completo)
4. [Endpoints Postman](#endpoints-postman)
5. [Scripts Postman](#scripts-postman)
6. [Arquivos de Teste](#arquivos-de-teste)
7. [Verificação de Resultados](#verificação-de-resultados)
8. [Troubleshooting](#troubleshooting)

---

## 🎯 Visão Geral

Este guia mostra como testar o fluxo completo de:
- **Ingestão de Data Acquisitions (DA)**
- **Geração de DSG (DA Schema Generator)**
- **Criação automática de DAs vinculados a SOCs** (generateDASOCs=true)

### O que é generateDASOCs?

Quando `generateDASOCs=true`, o sistema:
1. ✅ Gera o arquivo DSG (Excel) normalmente
2. ✅ **Cria automaticamente DAs individuais** para cada linha do arquivo
3. ✅ **Vincula cada DA ao seu SOC** através da propriedade `hasObjectScope`
4. ✅ Persiste todas as relações no triplestore

---

## 🔧 Pré-requisitos

### 1. Configurar Environment no Postman

Crie um **Environment** com as seguintes variáveis:

```
baseUrl = http://localhost:9000
```

### 2. Configurar Collection Variables

Crie uma **Collection** e adicione estas variáveis (valores serão preenchidos pelos scripts):

```javascript
studyUri = 
streamUri = 
socUri = 
dsgUri = 
dataFileUri = 
generatedFilename = 
timestamp = 
```

---

## 🔄 Fluxo de Teste Completo

### **Ordem de Execução:**

```mermaid
1. CREATE STUDY (com hasStatus)
   ↓
2. CREATE STREAM (vinculado ao Study)
   ↓
3. CREATE SOC (Study Object Collection)
   ↓
4. CREATE DSG (DA Schema Generator)
   ↓
5. GENERATE DSG MT com generateDASOCs=true ⭐
   ↓
6. VERIFY DAs CREATED (com hasObjectScope vinculado ao SOC)
   ↓
7. CLEANUP (deletar tudo)
```

---

## 📡 Endpoints Postman

### **REQUEST 1: CREATE STUDY**

**Method:** `POST`  
**URL:** 
```
{{baseUrl}}/hascoapi/api/study/create/{{encodedStudyJson}}
```

**Pre-request Script:**
```javascript
const timestamp = Date.now();
const studyUri = `http://test.example.com/kb/study/STUDY_${timestamp}`;

const studyData = {
    "uri": studyUri,
    "typeUri": "http://hadatac.org/ont/hasco#Study",
    "hascoTypeUri": "http://hadatac.org/ont/hasco#Study",
    "label": `Test Study ${timestamp}`,
    "comment": "Study created for DA-DSG-DASOC testing",
    "hasStatus": "DRAFT",
    "hasSIRManagerEmail": "test@example.com",
    "namedGraph": "http://hadatac.org/kb/test"
};

const encodedJson = encodeURIComponent(JSON.stringify(studyData));
pm.collectionVariables.set("encodedStudyJson", encodedJson);
pm.collectionVariables.set("studyUri", studyUri);
pm.collectionVariables.set("timestamp", timestamp);

console.log("📝 Study URI:", studyUri);
```

**Tests:**
```javascript
pm.test("Study created successfully", function() {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();
    pm.expect(jsonData.isSuccessful).to.be.true;
});

console.log("✅ Study created:", pm.collectionVariables.get("studyUri"));
```

---

### **REQUEST 2: CREATE STREAM**

**Method:** `POST`  
**URL:** 
```
{{baseUrl}}/hascoapi/api/stream/create/{{encodedStreamJson}}
```

**Pre-request Script:**
```javascript
const timestamp = pm.collectionVariables.get("timestamp");
const streamUri = `http://test.example.com/kb/stream/STREAM_${timestamp}`;
const studyUri = pm.collectionVariables.get("studyUri");

if (!studyUri) {
    throw new Error("❌ studyUri not found! Execute 'CREATE STUDY' first.");
}

const streamData = {
    "uri": streamUri,
    "typeUri": "http://hadatac.org/ont/hasco#MessageStream",
    "hascoTypeUri": "http://hadatac.org/ont/hasco#Stream",
    "label": `Test Stream ${timestamp}`,
    "comment": "Stream for DA-DSG-DASOC testing",
    "isMemberOf": studyUri,
    "hasStatus": "DRAFT",
    "hasSIRManagerEmail": "test@example.com",
    "namedGraph": "http://hadatac.org/kb/test"
};

const encodedJson = encodeURIComponent(JSON.stringify(streamData));
pm.collectionVariables.set("encodedStreamJson", encodedJson);
pm.collectionVariables.set("streamUri", streamUri);

console.log("📝 Stream URI:", streamUri);
console.log("🔗 Linked to Study:", studyUri);
```

**Tests:**
```javascript
pm.test("Stream created successfully", function() {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();
    pm.expect(jsonData.isSuccessful).to.be.true;
});

console.log("✅ Stream created:", pm.collectionVariables.get("streamUri"));
```

---

### **REQUEST 3: CREATE SOC (Study Object Collection)**

**Method:** `POST`  
**URL:** 
```
{{baseUrl}}/hascoapi/api/studyobjectcollection/create/{{encodedSOCJson}}
```

**Pre-request Script:**
```javascript
const timestamp = pm.collectionVariables.get("timestamp");
const socUri = `http://test.example.com/kb/soc/SOC_${timestamp}`;
const studyUri = pm.collectionVariables.get("studyUri");

if (!studyUri) {
    throw new Error("❌ studyUri not found! Execute 'CREATE STUDY' first.");
}

const socData = {
    "uri": socUri,
    "typeUri": "http://hadatac.org/ont/hasco#StudyObjectCollection",
    "hascoTypeUri": "http://hadatac.org/ont/hasco#StudyObjectCollection",
    "label": `Test SOC ${timestamp}`,
    "comment": "SOC for DASOC linking",
    "isMemberOf": studyUri,
    "hasStatus": "DRAFT",
    "hasSIRManagerEmail": "test@example.com",
    "namedGraph": "http://hadatac.org/kb/test"
};

const encodedJson = encodeURIComponent(JSON.stringify(socData));
pm.collectionVariables.set("encodedSOCJson", encodedJson);
pm.collectionVariables.set("socUri", socUri);

console.log("📝 SOC URI:", socUri);
console.log("🔗 Linked to Study:", studyUri);
```

**Tests:**
```javascript
pm.test("SOC created successfully", function() {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();
    pm.expect(jsonData.isSuccessful).to.be.true;
});

console.log("✅ SOC created:", pm.collectionVariables.get("socUri"));
```

---

### **REQUEST 4: CREATE DSG**

**Method:** `POST`  
**URL:** 
```
{{baseUrl}}/hascoapi/api/dsg/create/{{encodedDSGJson}}
```

**Pre-request Script:**
```javascript
const timestamp = pm.collectionVariables.get("timestamp");
const dsgUri = `http://test.example.com/kb/dsg/DSG_${timestamp}`;
const studyUri = pm.collectionVariables.get("studyUri");

if (!studyUri) {
    throw new Error("❌ studyUri not found! Execute 'CREATE STUDY' first.");
}

const dsgData = {
    "uri": dsgUri,
    "typeUri": "http://hadatac.org/ont/hasco#DASchemaGenerator",
    "hascoTypeUri": "http://hadatac.org/ont/hasco#DSG",
    "label": `Test DSG ${timestamp}`,
    "comment": "DSG for testing DA-SOC generation",
    "isMemberOf": studyUri,
    "hasStatus": "DRAFT",
    "hasSIRManagerEmail": "test@example.com",
    "namedGraph": "http://hadatac.org/kb/test"
};

const encodedJson = encodeURIComponent(JSON.stringify(dsgData));
pm.collectionVariables.set("encodedDSGJson", encodedJson);
pm.collectionVariables.set("dsgUri", dsgUri);

console.log("📝 DSG URI:", dsgUri);
```

**Tests:**
```javascript
pm.test("DSG created successfully", function() {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();
    pm.expect(jsonData.isSuccessful).to.be.true;
});

console.log("✅ DSG created:", pm.collectionVariables.get("dsgUri"));
```

---

### **REQUEST 5: GENERATE DSG WITH DASOCs** ⭐⭐⭐

**Method:** `POST`  
**URL:** 
```
{{baseUrl}}/hascoapi/api/mt/gen/perelement/da/{{dataFileUri}}/{{dsgUri}}/DSG-TEST-{{timestamp}}.xlsx/test_media/true?generateDASOCs=true
```

**Pre-request Script:**
```javascript
const timestamp = pm.collectionVariables.get("timestamp");
const dataFileUri = `http://test.example.com/kb/datafile/DF_DSG_${timestamp}`;
const dsgUri = pm.collectionVariables.get("dsgUri");

if (!dsgUri) {
    throw new Error("❌ dsgUri not found! Execute 'CREATE DSG' first.");
}

pm.collectionVariables.set("dataFileUri", dataFileUri);

console.log("🚀 GENERATING DSG WITH DASOCs");
console.log("📝 DataFile URI:", dataFileUri);
console.log("📝 DSG URI:", dsgUri);
console.log("⚙️  generateDASOCs: TRUE");
console.log("📂 Output file: DSG-TEST-" + timestamp + ".xlsx");
```

**Tests:**
```javascript
pm.test("DSG generated successfully", function() {
    pm.response.to.have.status(200);
    const jsonData = pm.response.json();
    pm.expect(jsonData.isSuccessful).to.be.true;
    
    if (jsonData.body && typeof jsonData.body === 'string') {
        pm.collectionVariables.set("generatedFilename", jsonData.body);
        console.log("📄 Generated file:", jsonData.body);
    }
});

console.log("✅ DSG generated with DASOCs enabled");
console.log("🔍 Next: Verify DAs were created");
```

---

### **REQUEST 6: VERIFY DAs CREATED**

**Method:** `GET`  
**URL:** 
```
{{baseUrl}}/hascoapi/api/da/status/DRAFT/100/0
```

**Tests:**
```javascript
pm.test("Query DAs by status successful", function() {
    pm.response.to.have.status(200);
});

const jsonData = pm.response.json();

if (jsonData.isSuccessful && Array.isArray(jsonData.body)) {
    const totalDAs = jsonData.body.length;
    console.log(`📊 Total DAs with status DRAFT: ${totalDAs}`);
    
    // Filtrar DAs que têm SOC vinculado (DASOC)
    const dasWithSOC = jsonData.body.filter(da => 
        da.hasObjectScope && da.hasObjectScope.length > 0
    );
    
    console.log(`🔗 DAs with SOC linked: ${dasWithSOC.length}`);
    
    if (dasWithSOC.length > 0) {
        console.log("\n📋 DASOC Details:");
        dasWithSOC.forEach((da, idx) => {
            console.log(`  [${idx+1}] ${da.label || da.uri}`);
            console.log(`      URI: ${da.uri}`);
            console.log(`      SOC: ${da.hasObjectScope}`);
            if (da.isDataAcquisitionOf) {
                console.log(`      Stream: ${da.isDataAcquisitionOf}`);
            }
        });
        
        pm.test("DASOCs were generated", function() {
            pm.expect(dasWithSOC.length).to.be.greaterThan(0);
        });
    } else {
        console.log("⚠️  No DAs with SOC linkage found");
        console.log("💡 Check if generateDASOCs parameter was set to true");
    }
    
    // Verificar se os DAs pertencem ao nosso test
    const testStreamUri = pm.collectionVariables.get("streamUri");
    const dasFromTestStream = jsonData.body.filter(da => 
        da.isDataAcquisitionOf === testStreamUri
    );
    
    if (dasFromTestStream.length > 0) {
        console.log(`\n🎯 DAs from our test stream: ${dasFromTestStream.length}`);
    }
    
} else {
    console.log("❌ No DAs found or query failed");
    console.log("Response:", JSON.stringify(jsonData, null, 2));
}
```

---

### **REQUEST 7: VERIFY SPECIFIC DA-SOC LINKAGE**

**Method:** `GET`  
**URL:** 
```
{{baseUrl}}/hascoapi/api/da/elements/100/0
```

**Tests:**
```javascript
const jsonData = pm.response.json();
const socUri = pm.collectionVariables.get("socUri");

if (jsonData.isSuccessful && Array.isArray(jsonData.body)) {
    // Procurar DAs vinculados ao nosso SOC específico
    const dasWithOurSOC = jsonData.body.filter(da => 
        da.hasObjectScope && da.hasObjectScope.includes(socUri)
    );
    
    console.log(`🔗 DAs linked to our SOC (${socUri}):`);
    console.log(`   Total: ${dasWithOurSOC.length}`);
    
    dasWithOurSOC.forEach((da, idx) => {
        console.log(`\n  DA ${idx+1}:`);
        console.log(`    URI: ${da.uri}`);
        console.log(`    Label: ${da.label}`);
        console.log(`    Status: ${da.hasStatus}`);
    });
}
```

---

## 🧹 CLEANUP - DELETE ALL

### **REQUEST 8: DELETE DAs**

**Method:** `GET`  
**URL:** 
```
{{baseUrl}}/hascoapi/api/da/status/DRAFT/1000/0
```

**Tests (para coletar URIs):**
```javascript
const jsonData = pm.response.json();
const streamUri = pm.collectionVariables.get("streamUri");

if (jsonData.isSuccessful && Array.isArray(jsonData.body)) {
    // Filtrar apenas DAs do nosso teste
    const testDAs = jsonData.body.filter(da => 
        da.isDataAcquisitionOf === streamUri
    );
    
    console.log(`🗑️  Found ${testDAs.length} test DAs to delete`);
    
    // Armazenar URIs para deletar
    const daUrisToDelete = testDAs.map(da => da.uri);
    pm.collectionVariables.set("daUrisToDelete", JSON.stringify(daUrisToDelete));
}
```

**REQUEST 8.1: DELETE EACH DA (criar request separado ou usar Runner)**

**Method:** `GET`  
**URL:** 
```
{{baseUrl}}/hascoapi/api/da/delete/{{daUriToDelete}}
```

---

### **REQUEST 9: DELETE DSG**

**Method:** `GET`  
**URL:** 
```
{{baseUrl}}/hascoapi/api/dsg/delete/{{dsgUri}}
```

**Tests:**
```javascript
pm.test("DSG deleted", function() {
    pm.response.to.have.status(200);
});
console.log("✅ DSG deleted");
```

---

### **REQUEST 10: DELETE SOC**

**Method:** `GET`  
**URL:** 
```
{{baseUrl}}/hascoapi/api/studyobjectcollection/delete/{{socUri}}
```

**Tests:**
```javascript
pm.test("SOC deleted", function() {
    pm.response.to.have.status(200);
});
console.log("✅ SOC deleted");
```

---

### **REQUEST 11: DELETE STREAM**

**Method:** `GET`  
**URL:** 
```
{{baseUrl}}/hascoapi/api/stream/delete/{{streamUri}}
```

**Tests:**
```javascript
pm.test("Stream deleted", function() {
    pm.response.to.have.status(200);
});
console.log("✅ Stream deleted");
```

---

### **REQUEST 12: DELETE STUDY**

**Method:** `GET`  
**URL:** 
```
{{baseUrl}}/hascoapi/api/study/delete/{{studyUri}}
```

**Tests:**
```javascript
pm.test("Study deleted", function() {
    pm.response.to.have.status(200);
});
console.log("✅ Study deleted");
console.log("🎉 CLEANUP COMPLETE!");
```

---

## 📝 Arquivos de Teste

### **Arquivo: DA-SOC-TEST.csv**

Crie este arquivo na sua máquina local:

```csv
hasURI,a,rdfs:label,hasco:isMemberOf,hasco:hasSOCReference,hasco:hasMethod,hasco:hasStatus
http://test.example.com/kb/da/DA_001,hasco:DataAcquisition,DA Test 001,STREAM_URI,SOC_URI,http://hadatac.org/ont/hasco#DirectObservation,DRAFT
http://test.example.com/kb/da/DA_002,hasco:DataAcquisition,DA Test 002,STREAM_URI,SOC_URI,http://hadatac.org/ont/hasco#DirectObservation,DRAFT
http://test.example.com/kb/da/DA_003,hasco:DataAcquisition,DA Test 003,STREAM_URI,SOC_URI,http://hadatac.org/ont/hasco#DirectObservation,DRAFT
http://test.example.com/kb/da/DA_004,hasco:DataAcquisition,DA Test 004,STREAM_URI,SOC_URI,http://hadatac.org/ont/hasco#DirectObservation,DRAFT
http://test.example.com/kb/da/DA_005,hasco:DataAcquisition,DA Test 005,STREAM_URI,SOC_URI,http://hadatac.org/ont/hasco#DirectObservation,DRAFT
```

**⚠️ IMPORTANTE:** 
- Substitua `STREAM_URI` pelo valor de `{{streamUri}}`
- Substitua `SOC_URI` pelo valor de `{{socUri}}`
- Você pode pegar esses valores do Console do Postman depois de criar Stream e SOC

**OU** use este script Python para gerar o arquivo automaticamente:

```python
import sys

if len(sys.argv) < 3:
    print("Usage: python generate_da_soc.py <streamUri> <socUri>")
    sys.exit(1)

stream_uri = sys.argv[1]
soc_uri = sys.argv[2]

with open('DA-SOC-TEST.csv', 'w', encoding='utf-8') as f:
    f.write("hasURI,a,rdfs:label,hasco:isMemberOf,hasco:hasSOCReference,hasco:hasMethod,hasco:hasStatus\n")
    for i in range(1, 6):
        da_uri = f"http://test.example.com/kb/da/DA_{i:03d}"
        f.write(f'{da_uri},hasco:DataAcquisition,DA Test {i:03d},{stream_uri},{soc_uri},http://hadatac.org/ont/hasco#DirectObservation,DRAFT\n')

print(f"✅ File 'DA-SOC-TEST.csv' generated with {5} DAs")
print(f"   Stream: {stream_uri}")
print(f"   SOC: {soc_uri}")
```

---

## 🎯 Explicação dos Parâmetros do MT Generation

### **Anatomia da URL:**

```
/hascoapi/api/mt/gen/perelement/da/{dataFileUri}/{elementUri}/{filename}/{mediaFolder}/{verifyUri}?generateDASOCs=true
```

**Parâmetros:**

| Parâmetro | Descrição | Exemplo |
|-----------|-----------|---------|
| `elementtype` | Tipo do elemento | `da`, `dsg`, `sdd`, etc. |
| `dataFileUri` | URI do DataFile que será criado | `http://test.com/datafile/DF_123` |
| `elementUri` | URI do DSG/elemento base | `http://test.com/dsg/DSG_123` |
| `filename` | Nome do arquivo Excel a gerar | `DSG-TEST-123.xlsx` |
| `mediaFolder` | Pasta onde salvar o arquivo | `test_media` |
| `verifyUri` | URI de verificação (pode ser "true") | `true` |
| `generateDASOCs` | **Query param** - Gerar DAs com SOC? | `true` / `false` |

---

## 📊 Verificação de Resultados

### **Método 1: Via API**

**Buscar todos os DAs:**
```
GET {{baseUrl}}/hascoapi/api/da/status/DRAFT/100/0
```

**Buscar DAs por Stream:**
```
GET {{baseUrl}}/hascoapi/api/dataacquisition/bystream/{{streamUri}}/100/0
```

**Buscar total:**
```
GET {{baseUrl}}/hascoapi/api/dataacquisition/bystream/total/{{streamUri}}
```

### **Método 2: Via SPARQL (Fuseki)**

Acesse: `http://localhost:3030/#/dataset/store/query`

**Query 1: DAs com SOC**
```sparql
PREFIX hasco: <http://hadatac.org/ont/hasco#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?da ?label ?stream ?soc
WHERE {
    GRAPH <http://hadatac.org/kb/test> {
        ?da a hasco:DataAcquisition .
        ?da rdfs:label ?label .
        OPTIONAL { ?da hasco:isDataAcquisitionOf ?stream }
        OPTIONAL { ?da hasco:hasObjectScope ?soc }
    }
}
ORDER BY ?da
```

**Query 2: Contar DASOCs**
```sparql
PREFIX hasco: <http://hadatac.org/ont/hasco#>

SELECT (COUNT(?da) as ?total)
WHERE {
    GRAPH <http://hadatac.org/kb/test> {
        ?da a hasco:DataAcquisition .
        ?da hasco:hasObjectScope ?soc .
    }
}
```

---

## 🔍 Logs Esperados

Quando `generateDASOCs=true`, você deve ver nos logs:

```
IngestionAPI.mtGenByElement() with:
  elementtype: [da]
  generateDASOCs: [true]
  
[DSGGen] Generating DSG metadata template...
[DSGGen] Processing DA-SOC linkages...
[DSGGen] Creating individual DAs from DSG...
[DSGGen] Created DA: http://test.example.com/kb/da/DA_001
[DSGGen] Created DA: http://test.example.com/kb/da/DA_002
[DSGGen] Created DA: http://test.example.com/kb/da/DA_003
...
[DSGGen] Total DAs created: 5
```

---

## ❌ Troubleshooting

### **Problema: "No DAs created"**

**Causas possíveis:**
1. ❌ `generateDASOCs` não está como query parameter
2. ❌ Arquivo CSV não está no formato `DA-SOC-*.csv`
3. ❌ Stream ou SOC não existem
4. ❌ Permissões na pasta de media

**Solução:**
```bash
# Verificar se query param está correto
?generateDASOCs=true  ✅
/true                 ❌ (isso é o verifyUri, não o generateDASOCs!)
```

### **Problema: "File not found"**

**Verificar configuração:**
```properties
# application.conf
hascoapi.paths.ingestion="C:/hascoapi/var/"
```

A pasta `C:/hascoapi/var/media/test_media/` deve existir!

### **Problema: "Response vazia []"**

**Causas:**
1. Nenhum DA tem `hasStatus: "DRAFT"`
2. Case-sensitive: use `DRAFT`, não `draft`
3. DAs foram criados em outro namedGraph

**Solução:**
```
# Verificar sem filtro de status
GET {{baseUrl}}/hascoapi/api/da/elements/100/0
```

---

## 📦 Formato do Arquivo DA-SOC

### **Colunas Obrigatórias:**

| Coluna | Descrição | Exemplo |
|--------|-----------|---------|
| `hasURI` | URI do DA | `http://test.com/kb/da/DA_001` |
| `a` | Tipo RDF | `hasco:DataAcquisition` |
| `rdfs:label` | Label do DA | `My Data Acquisition` |
| `hasco:isMemberOf` | URI do Stream | `http://test.com/stream/S1` |
| `hasco:hasSOCReference` | URI do SOC | `http://test.com/soc/SOC1` |
| `hasco:hasMethod` | Método | `http://hadatac.org/ont/hasco#DirectObservation` |
| `hasco:hasStatus` | Status | `DRAFT` |

### **Exemplo Completo:**

```csv
hasURI,a,rdfs:label,hasco:isMemberOf,hasco:hasSOCReference,hasco:hasMethod,hasco:hasStatus,hasco:hasStartedAt
http://test.example.com/kb/da/DA_001,hasco:DataAcquisition,Survey DA 001,http://test.example.com/kb/stream/STREAM_123,http://test.example.com/kb/soc/SOC_123,http://hadatac.org/ont/hasco#DirectObservation,DRAFT,2024-01-01T00:00:00Z
http://test.example.com/kb/da/DA_002,hasco:DataAcquisition,Survey DA 002,http://test.example.com/kb/stream/STREAM_123,http://test.example.com/kb/soc/SOC_123,http://hadatac.org/ont/hasco#DirectObservation,DRAFT,2024-01-02T00:00:00Z
```

---

## 🚀 Quick Test (All-in-One)

### **Script Postman para executar tudo de uma vez:**

Crie um **Collection Runner** ou use este script no **Collection Pre-request**:

```javascript
// Collection Pre-request Script
const timestamp = Date.now();

// Gerar todos os URIs de uma vez
const baseTestUri = "http://test.example.com/kb";
const studyUri = `${baseTestUri}/study/STUDY_${timestamp}`;
const streamUri = `${baseTestUri}/stream/STREAM_${timestamp}`;
const socUri = `${baseTestUri}/soc/SOC_${timestamp}`;
const dsgUri = `${baseTestUri}/dsg/DSG_${timestamp}`;

// Salvar todas as variáveis
pm.collectionVariables.set("timestamp", timestamp);
pm.collectionVariables.set("studyUri", studyUri);
pm.collectionVariables.set("streamUri", streamUri);
pm.collectionVariables.set("socUri", socUri);
pm.collectionVariables.set("dsgUri", dsgUri);

console.log("🎬 TEST SESSION INITIALIZED");
console.log("═══════════════════════════");
console.log("Timestamp:", timestamp);
console.log("Study:", studyUri);
console.log("Stream:", streamUri);
console.log("SOC:", socUri);
console.log("DSG:", dsgUri);
console.log("═══════════════════════════");
```

---

## 📌 Endpoints Alternativos

### **Gerar DSG por Status (todos os DSGs com status DRAFT):**
```
POST {{baseUrl}}/hascoapi/api/mt/gen/perstatus/da/{{dataFileUri}}/DRAFT/DSG-OUTPUT.xlsx/test_media/true?generateDASOCs=true
```

### **Gerar DSG por Manager Email:**
```
POST {{baseUrl}}/hascoapi/api/mt/gen/peruser/da/{{dataFileUri}}/test@example.com/DRAFT/DSG-USER.xlsx/test_media/true?generateDASOCs=true
```

### **Rotas Legacy (sem /hascoapi/):**
```
POST /api/mt/gen/perelement/da/{{dataFileUri}}/{{dsgUri}}/file.xlsx/media/true?generateDASOCs=true
```

---

## ✅ Checklist de Teste

### **Antes de começar:**
- [ ] Servidor rodando (`sbt run`)
- [ ] Fuseki rodando (porta 3030)
- [ ] Environment configurado no Postman
- [ ] Collection variables criadas

### **Execução:**
- [ ] Study criado com `hasStatus: "DRAFT"`
- [ ] Stream criado e vinculado ao Study
- [ ] SOC criado e vinculado ao Study
- [ ] DSG criado
- [ ] MT gerado com `?generateDASOCs=true`
- [ ] Response indica sucesso
- [ ] Arquivo Excel foi gerado

### **Verificação:**
- [ ] Query `GET /api/da/status/DRAFT/100/0` retorna DAs
- [ ] DAs têm propriedade `hasObjectScope` preenchida
- [ ] `hasObjectScope` aponta para o SOC correto
- [ ] Número de DAs = número de linhas no CSV
- [ ] Consulta SPARQL confirma as relações

### **Cleanup:**
- [ ] Todos os DAs deletados
- [ ] DSG deletado
- [ ] SOC deletado
- [ ] Stream deletado
- [ ] Study deletado

---

## 🎓 Conceitos Importantes

### **DA (Data Acquisition)**
- Representa uma aquisição de dados
- Vinculado a um **Stream**
- Pode ter dados brutos ou processados

### **DSG (DA Schema Generator)**
- Template/especificação para gerar múltiplos DAs
- Define estrutura e metadados dos DAs

### **SOC (Study Object Collection)**
- Coleção de objetos de estudo (participantes, amostras, etc.)
- Vinculado a um **Study**

### **DASOC**
- **Link entre DA e SOC**
- Propriedade: `hasco:hasObjectScope`
- Indica quais objetos de estudo foram observados naquele DA

---

## 🔬 Casos de Teste Avançados

### **Teste 1: Múltiplos SOCs**

Crie 2 SOCs e teste um CSV com DAs apontando para SOCs diferentes:

```csv
hasURI,hasco:hasSOCReference
http://test.com/da/DA_001,http://test.com/soc/SOC_A
http://test.com/da/DA_002,http://test.com/soc/SOC_B
http://test.com/da/DA_003,http://test.com/soc/SOC_A
```

### **Teste 2: DA sem SOC**

```csv
hasURI,hasco:hasSOCReference
http://test.com/da/DA_001,
http://test.com/da/DA_002,http://test.com/soc/SOC_A
```

O DA_001 não deve ter `hasObjectScope`.

### **Teste 3: generateDASOCs=false (default)**

Execute sem o query parameter e verifique que **nenhum DA individual** é criado, apenas o arquivo DSG.

---

## 📚 Referências Adicionais

### **Documentação relacionada:**
- `DASOC-SPECIFICATION.md` - Especificação completa do DASOC
- `DASOC-INGESTION-INVESTIGATION-COMPLETE.md` - Investigação de ingestão
- `AUTO-NAMESPACE-TEST-GUIDE.md` - Testes de namespace

### **Código fonte relevante:**
- `IngestionAPI.java` - Métodos `mtGenByElement`, `mtGenByStatus`, `mtGenByManager`
- `DSGGen.java` - Geração de DSG e criação de DAs
- `SOCGen.java` - Geração de SOC templates

---

## 🎉 Resultado Esperado Final

Após executar todo o fluxo com `generateDASOCs=true`:

```json
// GET /api/da/status/DRAFT/100/0

{
  "isSuccessful": true,
  "body": [
    {
      "uri": "http://test.example.com/kb/da/DA_001",
      "label": "DA Test 001",
      "hasStatus": "DRAFT",
      "isDataAcquisitionOf": "http://test.example.com/kb/stream/STREAM_123",
      "hasObjectScope": "http://test.example.com/kb/soc/SOC_123",
      "hasMethod": "http://hadatac.org/ont/hasco#DirectObservation"
    },
    {
      "uri": "http://test.example.com/kb/da/DA_002",
      "label": "DA Test 002",
      "hasStatus": "DRAFT",
      "isDataAcquisitionOf": "http://test.example.com/kb/stream/STREAM_123",
      "hasObjectScope": "http://test.example.com/kb/soc/SOC_123",
      "hasMethod": "http://hadatac.org/ont/hasco#DirectObservation"
    }
    // ... mais DAs
  ]
}
```

**✨ Sucesso quando:**
- `hasObjectScope` está presente e aponta para o SOC correto
- Todos os DAs foram criados automaticamente pelo sistema
- As relações estão persistidas no triplestore

---

## 🆘 Precisa de Ajuda?

Se algo não funcionar:

1. ✅ Verifique os **logs do servidor** (`sbt run`)
2. ✅ Verifique o **Console do Postman** (output dos scripts)
3. ✅ Execute queries SPARQL direto no Fuseki
4. ✅ Verifique se os arquivos foram gerados em `C:/hascoapi/var/media/test_media/`

---

**Criado em:** 2026-04-09  
**Versão:** 1.0  
**Autor:** GitHub Copilot  
**Status:** ✅ Ready for Testing

