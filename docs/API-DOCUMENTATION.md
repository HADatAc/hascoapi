# HADatAc (hascoapi) REST API Documentation

**Version:** 1.0  
**Base URL:** `{{baseUrl}}/hascoapi` (e.g., `http://localhost:9000/hascoapi`)  
**Authentication:** JWT Bearer Token in `Authorization` header  
**Date:** April 6, 2026

---

## Table of Contents
1. [Authentication](#authentication)
2. [Response Format](#response-format)
3. [API Endpoints by Resource](#api-endpoints-by-resource)
   - [Repository & Namespace Management](#repository--namespace-management)
   - [Instruments](#instruments)
   - [Studies](#studies)
   - [Study Objects & Collections](#study-objects--collections)
   - [Data Acquisitions](#data-acquisitions)
   - [Streams & Topics](#streams--topics)
   - [Deployments & Platforms](#deployments--platforms)
   - [Semantic Data Dictionaries (SDD)](#semantic-data-dictionaries-sdd)
   - [Data Specification Grids (DSG)](#data-specification-grids-dsg)
   - [Knowledge Graph Resources (KGR)](#knowledge-graph-resources-kgr)
   - [Data Processing Plans (DP2)](#data-processing-plans-dp2)
   - [Stream Specifications (STR)](#stream-specifications-str)
   - [Workflows (WKF)](#workflows-wkf)
   - [Instruments & Components](#instruments--components)
   - [Codebooks & Response Options](#codebooks--response-options)
   - [Annotations & Semantic Variables](#annotations--semantic-variables)
   - [Organizations & People](#organizations--people)
   - [Places & Addresses](#places--addresses)
   - [Data Files & Blob Management](#data-files--blob-management)
   - [Metadata Template Generation](#metadata-template-generation)
   - [Ingestion](#ingestion)
4. [Postman Test Suite](#postman-test-suite)

---

## Authentication

The API uses JWT Bearer tokens for authentication. Include the token in the `Authorization` header:

```
Authorization: Bearer {{token}}
```

Most endpoints require authentication. Anonymous endpoints are clearly marked.

---

## Response Format

All API responses follow this standard format:

```json
{
  "isSuccessful": true,
  "body": {
    // Response data or message
  }
}
```

**Common HTTP Status Codes:**
- `200 OK` - Request successful
- `201 Created` - Resource created successfully
- `400 Bad Request` - Invalid request parameters
- `401 Unauthorized` - Missing or invalid authentication
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error

---

## API Endpoints by Resource

### Repository & Namespace Management

#### Get Repository Information
- **Method:** GET
- **Path:** `/hascoapi/api/repo`
- **Auth Required:** No
- **Query Parameters:** None
- **Response:** Repository metadata (label, title, URL, description, namespaces)
- **Status Codes:** 200, 404

#### Update Repository Label
- **Method:** GET
- **Path:** `/hascoapi/api/repo/label/{label}`
- **Auth Required:** Yes (Administrator)
- **Path Parameters:** 
  - `label` (string, required) - New repository label
- **Response:** Confirmation message
- **Status Codes:** 200, 400, 401, 403

#### Update Repository Title
- **Method:** GET
- **Path:** `/hascoapi/api/repo/title/{title}`
- **Auth Required:** Yes (Administrator)
- **Path Parameters:** 
  - `title` (string, required) - New repository title
- **Response:** Confirmation message
- **Status Codes:** 200, 400, 401, 403

#### Update Repository URL
- **Method:** GET
- **Path:** `/hascoapi/api/repo/url/{url}`
- **Auth Required:** Yes (Administrator)
- **Path Parameters:** 
  - `url` (string, required) - New repository URL
- **Response:** Confirmation message
- **Status Codes:** 200, 400, 401, 403

#### Update Default Namespace
- **Method:** GET
- **Path:** `/hascoapi/api/repo/namespace/default/{prefix}/{url}/{mime}/{source}`
- **Auth Required:** Yes (Administrator)
- **Path Parameters:** 
  - `prefix` (string, required) - Namespace prefix
  - `url` (string, required) - Namespace URL
  - `mime` (string, required) - MIME type
  - `source` (string, required) - Source identifier
- **Response:** Confirmation message
- **Status Codes:** 200, 400, 401, 403

#### Create Namespace
- **Method:** GET
- **Path:** `/hascoapi/api/repo/namespace/create/{json}`
- **Auth Required:** Yes (Administrator)
- **Path Parameters:** 
  - `json` (string, required) - JSON-encoded namespace definition
- **Request Body Schema:** (in path parameter as JSON string)
  ```json
  {
    "prefix": "string (required)",
    "url": "string (required)",
    "mime": "string (required)",
    "source": "string (required)"
  }
  ```
- **Response:** Confirmation message
- **Status Codes:** 200, 400, 401, 403

#### Delete Namespace
- **Method:** GET
- **Path:** `/hascoapi/api/repo/namespace/delete/{abbreviation}`
- **Auth Required:** Yes (Administrator)
- **Path Parameters:** 
  - `abbreviation` (string, required) - Namespace abbreviation to delete
- **Response:** Confirmation message
- **Status Codes:** 200, 404, 401, 403

#### Get Namespaces
- **Method:** GET
- **Path:** `/hascoapi/api/repo/table/namespaces`
- **Auth Required:** No
- **Response:** Array of namespace objects
- **Status Codes:** 200

#### Load Ontologies
- **Method:** GET
- **Path:** `/hascoapi/api/repo/ont/load`
- **Auth Required:** Yes (Administrator)
- **Response:** Confirmation message
- **Status Codes:** 200, 401, 403, 500

#### Delete Ontologies
- **Method:** GET
- **Path:** `/hascoapi/api/repo/ont/delete`
- **Auth Required:** Yes (Administrator)
- **Response:** Confirmation message
- **Status Codes:** 200, 401, 403, 500

---

### Instruments

#### Get All Instruments
- **Method:** GET
- **Path:** `/hascoapi/api/instrument/elements/{pageSize}/{offset}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `pageSize` (integer, required) - Number of items per page
  - `offset` (integer, required) - Starting index for pagination
- **Response:** Array of Instrument objects with pagination
- **Status Codes:** 200, 401

#### Get Total Instruments Count
- **Method:** GET
- **Path:** `/hascoapi/api/instrument/elements/total`
- **Auth Required:** Yes
- **Response:** `{"total": integer}`
- **Status Codes:** 200, 401

#### Get Instruments by Keyword
- **Method:** GET
- **Path:** `/hascoapi/api/instrument/keyword/{keyword}/{pageSize}/{offset}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `keyword` (string, required) - Search keyword
  - `pageSize` (integer, required)
  - `offset` (integer, required)
- **Response:** Filtered array of Instrument objects
- **Status Codes:** 200, 401

#### Create Instrument
- **Method:** POST/GET
- **Path:** `/hascoapi/api/instrument/create/{json}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `json` (string, required) - URL-encoded JSON instrument definition
- **Request Body Schema:** (in path parameter as JSON string)
  ```json
  {
    "uri": "string (optional, auto-generated if not provided)",
    "typeUri": "string (required)",
    "label": "string (required)",
    "hasShortName": "string (optional)",
    "comment": "string (optional)",
    "hasVersion": "string (optional)",
    "hasLanguage": "string (required, ISO 639-1 code)",
    "hasInformant": "string (required, URI)",
    "hasSIRManagerEmail": "string (required, email)",
    "namedGraph": "string (required)"
  }
  ```
- **Response:** Success message with created instrument URI
- **Status Codes:** 200, 400, 401

#### Delete Instrument
- **Method:** POST/GET
- **Path:** `/hascoapi/api/instrument/delete/{uri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `uri` (string, required) - URI of instrument to delete
- **Response:** Confirmation message
- **Status Codes:** 200, 404, 401

#### Render Instrument as Plain Text
- **Method:** GET
- **Path:** `/hascoapi/api/instrument/totext/plain/{uri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `uri` (string, required) - Instrument URI
- **Response:** Plain text representation
- **Content-Type:** `text/plain`
- **Status Codes:** 200, 404, 401

#### Render Instrument as HTML
- **Method:** GET
- **Path:** `/hascoapi/api/instrument/totext/html/{uri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `uri` (string, required) - Instrument URI
- **Response:** HTML representation
- **Content-Type:** `text/html`
- **Status Codes:** 200, 404, 401

#### Render Instrument as PDF
- **Method:** GET
- **Path:** `/hascoapi/api/instrument/totext/pdf/{uri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `uri` (string, required) - Instrument URI
- **Response:** PDF document
- **Content-Type:** `application/pdf`
- **Status Codes:** 200, 404, 401

#### Get Instrument Components
- **Method:** GET
- **Path:** `/hascoapi/api/instrument/components/{instrumentUri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `instrumentUri` (string, required) - Instrument URI
- **Response:** Array of component objects
- **Status Codes:** 200, 404, 401

#### Get Instrument Container Slots
- **Method:** GET
- **Path:** `/hascoapi/api/instrument/containerslots/{instrumentUri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `instrumentUri` (string, required) - Instrument URI
- **Response:** Array of container slot objects
- **Status Codes:** 200, 404, 401

---

### Studies

#### Get All Studies
- **Method:** GET
- **Path:** `/hascoapi/api/study/elements/{pageSize}/{offset}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `pageSize` (integer, required)
  - `offset` (integer, required)
- **Response:** Array of Study objects
- **Status Codes:** 200, 401

#### Get Total Studies Count
- **Method:** GET
- **Path:** `/hascoapi/api/study/elements/total`
- **Auth Required:** Yes
- **Response:** `{"total": integer}`
- **Status Codes:** 200, 401

#### Get Studies by Manager Email
- **Method:** GET
- **Path:** `/hascoapi/api/study/manageremail/{managerEmail}/{pageSize}/{offset}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `managerEmail` (string, required) - Manager's email address
  - `pageSize` (integer, required)
  - `offset` (integer, required)
- **Response:** Filtered array of Study objects
- **Status Codes:** 200, 401

#### Get Studies by Keyword
- **Method:** GET
- **Path:** `/hascoapi/api/study/keyword/{keyword}/{pageSize}/{offset}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `keyword` (string, required)
  - `pageSize` (integer, required)
  - `offset` (integer, required)
- **Response:** Filtered array of Study objects
- **Status Codes:** 200, 401

#### Create Study
- **Method:** POST/GET
- **Path:** `/hascoapi/api/study/create/{json}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `json` (string, required) - URL-encoded JSON study definition
- **Request Body Schema:** (in path parameter as JSON string)
  ```json
  {
    "uri": "string (optional)",
    "label": "string (required)",
    "title": "string (required)",
    "project": "string (optional, project URI)",
    "comment": "string (optional)",
    "hasSIRManagerEmail": "string (required, email)",
    "permissionUri": "string (optional, default: private)",
    "namedGraph": "string (required)"
  }
  ```
- **Response:** Success message with created study URI
- **Status Codes:** 200, 400, 401

#### Delete Study
- **Method:** POST/GET
- **Path:** `/hascoapi/api/study/delete/{uri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `uri` (string, required) - Study URI
- **Response:** Confirmation message
- **Status Codes:** 200, 404, 401

#### Get Study Object Collections by Study
- **Method:** GET
- **Path:** `/hascoapi/api/study/socs/{uri}/{pageSize}/{offset}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `uri` (string, required) - Study URI
  - `pageSize` (integer, required)
  - `offset` (integer, required)
- **Response:** Array of StudyObjectCollection objects
- **Status Codes:** 200, 404, 401

#### Get Virtual Columns by Study
- **Method:** GET
- **Path:** `/hascoapi/api/study/virtualcolumns/{uri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `uri` (string, required) - Study URI
- **Response:** Array of VirtualColumn objects
- **Status Codes:** 200, 404, 401

#### Get Streams by Study
- **Method:** GET
- **Path:** `/hascoapi/api/study/streams/{uri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `uri` (string, required) - Study URI
- **Response:** Array of Stream objects
- **Status Codes:** 200, 404, 401

---

### Study Objects & Collections

#### Get Study Objects by Collection
- **Method:** GET
- **Path:** `/hascoapi/api/studyobject/bysoc/{socUri}/{pageSize}/{offset}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `socUri` (string, required) - Study Object Collection URI
  - `pageSize` (integer, required)
  - `offset` (integer, required)
- **Response:** Array of StudyObject objects
- **Status Codes:** 200, 404, 401

#### Get Total Study Objects by Collection
- **Method:** GET
- **Path:** `/hascoapi/api/studyobject/bysoc/total/{socUri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `socUri` (string, required) - Study Object Collection URI
- **Response:** `{"total": integer}`
- **Status Codes:** 200, 404, 401

#### Get Study Object Collections by Study
- **Method:** GET
- **Path:** `/hascoapi/api/studyobjectcollection/bystudy/{studyUri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `studyUri` (string, required) - Study URI
- **Response:** Array of StudyObjectCollection objects
- **Status Codes:** 200, 404, 401

#### Create Study Object
- **Method:** POST/GET
- **Path:** `/hascoapi/api/studyobject/create/{json}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `json` (string, required) - URL-encoded JSON
- **Request Body Schema:**
  ```json
  {
    "uri": "string (optional)",
    "label": "string (required)",
    "typeUri": "string (required)",
    "isMemberOf": "string (required, SOC URI)",
    "hasSIRManagerEmail": "string (required)",
    "namedGraph": "string (required)"
  }
  ```
- **Response:** Success message with created URI
- **Status Codes:** 200, 400, 401

#### Delete Study Object
- **Method:** POST/GET
- **Path:** `/hascoapi/api/studyobject/delete/{uri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `uri` (string, required) - StudyObject URI
- **Response:** Confirmation message
- **Status Codes:** 200, 404, 401

---

### Data Acquisitions

#### Get Data Acquisitions by Stream
- **Method:** GET
- **Path:** `/hascoapi/api/dataacquisition/bystream/{uri}/{pageSize}/{offset}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `uri` (string, required) - Stream URI
  - `pageSize` (integer, required)
  - `offset` (integer, required)
- **Response:** Array of DA (DataAcquisition) objects
- **Status Codes:** 200, 404, 401

#### Get Total Data Acquisitions by Stream
- **Method:** GET
- **Path:** `/hascoapi/api/dataacquisition/bystream/total/{uri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `uri` (string, required) - Stream URI
- **Response:** `{"total": integer}`
- **Status Codes:** 200, 404, 401

#### Get Data Acquisitions by Study
- **Method:** GET
- **Path:** `/hascoapi/api/dataacquisition/bystudy/{uri}/{pageSize}/{offset}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `uri` (string, required) - Study URI
  - `pageSize` (integer, required)
  - `offset` (integer, required)
- **Response:** Array of DA objects
- **Status Codes:** 200, 404, 401

#### Create Data Acquisition
- **Method:** POST/GET
- **Path:** `/hascoapi/api/da/create/{json}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `json` (string, required)
- **Request Body Schema:**
  ```json
  {
    "uri": "string (optional)",
    "label": "string (required)",
    "deploymentUri": "string (optional)",
    "hasSIRManagerEmail": "string (required)",
    "namedGraph": "string (required)"
  }
  ```
- **Response:** Success message with created DA URI
- **Status Codes:** 200, 400, 401

#### Delete Data Acquisition
- **Method:** POST/GET
- **Path:** `/hascoapi/api/da/delete/{uri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `uri` (string, required) - DA URI
- **Response:** Confirmation message
- **Status Codes:** 200, 404, 401

---

### Streams & Topics

#### Get Streams by State and Email
- **Method:** GET
- **Path:** `/hascoapi/api/stream/bystateemail/{state}/{email}/{pageSize}/{offset}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `state` (string, required) - One of: DRAFT, ACTIVE, CLOSED, ALL_STATUSES
  - `email` (string, required) - Manager email
  - `pageSize` (integer, required)
  - `offset` (integer, required)
- **Response:** Array of Stream objects
- **Status Codes:** 200, 400, 401

#### Get Total Streams by State and Email
- **Method:** GET
- **Path:** `/hascoapi/api/stream/bystateemail/total/{state}/{email}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `state` (string, required)
  - `email` (string, required)
- **Response:** `{"total": integer}`
- **Status Codes:** 200, 400, 401

#### Get Streams by Study
- **Method:** GET
- **Path:** `/hascoapi/api/stream/bystudy/{studyUri}/{state}/{pageSize}/{offset}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `studyUri` (string, required)
  - `state` (string, required)
  - `pageSize` (integer, required)
  - `offset` (integer, required)
- **Response:** Array of Stream objects
- **Status Codes:** 200, 404, 401

#### Get Subscribed Topics
- **Method:** GET
- **Path:** `/hascoapi/api/topic/subscribed`
- **Auth Required:** Yes
- **Response:** Array of active StreamTopic objects
- **Status Codes:** 200, 401

#### Subscribe to Topic
- **Method:** GET
- **Path:** `/hascoapi/api/topic/subscribe/{topicUri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `topicUri` (string, required) - Topic URI to subscribe to
- **Response:** Confirmation message
- **Status Codes:** 200, 404, 401

#### Unsubscribe from Topic
- **Method:** GET
- **Path:** `/hascoapi/api/topic/unsubscribe/{topicUri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `topicUri` (string, required) - Topic URI to unsubscribe from
- **Response:** Confirmation message
- **Status Codes:** 200, 404, 401

#### Set Topic Status
- **Method:** GET
- **Path:** `/hascoapi/api/topic/setstatus/{topicUri}/{status}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `topicUri` (string, required)
  - `status` (string, required) - New status
- **Response:** Confirmation message
- **Status Codes:** 200, 400, 401

#### Get Latest Topic Value
- **Method:** GET
- **Path:** `/hascoapi/api/topic/latest/{topicUri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `topicUri` (string, required)
- **Response:** Latest value object
- **Status Codes:** 200, 404, 401

---

### Deployments & Platforms

#### Get Deployments by State and Email
- **Method:** GET
- **Path:** `/hascoapi/api/deployment/{state}/{email}/{pageSize}/{offset}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `state` (string, required) - DRAFT, ACTIVE, CLOSED, or ALL_STATUSES
  - `email` (string, required)
  - `pageSize` (integer, required)
  - `offset` (integer, required)
- **Response:** Array of Deployment objects
- **Status Codes:** 200, 400, 401

#### Get Total Deployments
- **Method:** GET
- **Path:** `/hascoapi/api/deployment/total/{state}/{email}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `state` (string, required)
  - `email` (string, required)
- **Response:** `{"total": integer}`
- **Status Codes:** 200, 400, 401

#### Get Deployments by Platform Instance
- **Method:** GET
- **Path:** `/hascoapi/api/deploymentbyplatforminstance/{platforminstanceUri}/{pageSize}/{offset}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `platforminstanceUri` (string, required)
  - `pageSize` (integer, required)
  - `offset` (integer, required)
- **Response:** Array of Deployment objects
- **Status Codes:** 200, 404, 401

#### Get Platform Instances by Platform
- **Method:** GET
- **Path:** `/hascoapi/api/platforminstance/byplatform/{platformUri}/{pageSize}/{offset}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `platformUri` (string, required)
  - `pageSize` (integer, required)
  - `offset` (integer, required)
- **Response:** Array of PlatformInstance objects
- **Status Codes:** 200, 404, 401

#### Create Deployment
- **Method:** POST/GET
- **Path:** `/hascoapi/api/deployment/create/{json}`
- **Auth Required:** Yes
- **Request Body Schema:**
  ```json
  {
    "uri": "string (optional)",
    "label": "string (required)",
    "platform": "string (required, platform URI)",
    "instrument": "string (optional, instrument URI)",
    "startedAt": "string (optional, ISO date)",
    "endedAt": "string (optional, ISO date)",
    "hasSIRManagerEmail": "string (required)",
    "namedGraph": "string (required)"
  }
  ```
- **Response:** Success message
- **Status Codes:** 200, 400, 401

#### Delete Deployment
- **Method:** POST/GET
- **Path:** `/hascoapi/api/deployment/delete/{uri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `uri` (string, required)
- **Response:** Confirmation message
- **Status Codes:** 200, 404, 401

---

### Semantic Data Dictionaries (SDD)

#### Get All SDDs
- **Method:** GET
- **Path:** `/hascoapi/api/sdd/elements/{pageSize}/{offset}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `pageSize` (integer, required)
  - `offset` (integer, required)
- **Response:** Array of SDD objects
- **Status Codes:** 200, 401

#### Get Total SDDs
- **Method:** GET
- **Path:** `/hascoapi/api/sdd/elements/total`
- **Auth Required:** Yes
- **Response:** `{"total": integer}`
- **Status Codes:** 200, 401

#### Create SDD
- **Method:** POST/GET
- **Path:** `/hascoapi/api/sdd/create/{json}`
- **Auth Required:** Yes
- **Request Body Schema:**
  ```json
  {
    "uri": "string (optional)",
    "label": "string (required)",
    "hasDataFileUri": "string (optional)",
    "hasSIRManagerEmail": "string (required)",
    "namedGraph": "string (required)"
  }
  ```
- **Response:** Success message
- **Status Codes:** 200, 400, 401

#### Delete SDD
- **Method:** POST/GET
- **Path:** `/hascoapi/api/sdd/delete/{uri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `uri` (string, required)
- **Response:** Confirmation message
- **Status Codes:** 200, 404, 401

---

### Data Specification Grids (DSG)

#### Get All DSGs
- **Method:** GET
- **Path:** `/hascoapi/api/dsg/elements/{pageSize}/{offset}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `pageSize` (integer, required)
  - `offset` (integer, required)
- **Response:** Array of DSG objects
- **Status Codes:** 200, 401

#### Get Total DSGs
- **Method:** GET
- **Path:** `/hascoapi/api/dsg/elements/total`
- **Auth Required:** Yes
- **Response:** `{"total": integer}`
- **Status Codes:** 200, 401

#### Create DSG
- **Method:** POST/GET
- **Path:** `/hascoapi/api/dsg/create/{json}`
- **Auth Required:** Yes
- **Request Body Schema:**
  ```json
  {
    "uri": "string (optional)",
    "label": "string (required)",
    "hasDataFileUri": "string (optional)",
    "hasSIRManagerEmail": "string (required)",
    "namedGraph": "string (required)"
  }
  ```
- **Response:** Success message
- **Status Codes:** 200, 400, 401

#### Delete DSG
- **Method:** POST/GET
- **Path:** `/hascoapi/api/dsg/delete/{uri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `uri` (string, required)
- **Response:** Confirmation message
- **Status Codes:** 200, 404, 401

---

### Knowledge Graph Resources (KGR)

#### Get All KGRs
- **Method:** GET
- **Path:** `/hascoapi/api/kgr/elements/{pageSize}/{offset}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `pageSize` (integer, required)
  - `offset` (integer, required)
- **Response:** Array of KGR objects
- **Status Codes:** 200, 401

#### Get Total KGRs
- **Method:** GET
- **Path:** `/hascoapi/api/kgr/elements/total`
- **Auth Required:** Yes
- **Response:** `{"total": integer}`
- **Status Codes:** 200, 401

#### Create KGR
- **Method:** POST/GET
- **Path:** `/hascoapi/api/kgr/create/{json}`
- **Auth Required:** Yes
- **Request Body Schema:**
  ```json
  {
    "uri": "string (optional)",
    "label": "string (required)",
    "hasDataFileUri": "string (optional)",
    "hasSIRManagerEmail": "string (required)",
    "namedGraph": "string (required)"
  }
  ```
- **Response:** Success message
- **Status Codes:** 200, 400, 401

#### Delete KGR
- **Method:** POST/GET
- **Path:** `/hascoapi/api/kgr/delete/{uri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `uri` (string, required)
- **Response:** Confirmation message
- **Status Codes:** 200, 404, 401

---

### Data Processing Plans (DP2)

#### Get All DP2s
- **Method:** GET
- **Path:** `/hascoapi/api/dp2/elements/{pageSize}/{offset}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `pageSize` (integer, required)
  - `offset` (integer, required)
- **Response:** Array of DP2 objects
- **Status Codes:** 200, 401

#### Get Total DP2s
- **Method:** GET
- **Path:** `/hascoapi/api/dp2/elements/total`
- **Auth Required:** Yes
- **Response:** `{"total": integer}`
- **Status Codes:** 200, 401

#### Create DP2
- **Method:** POST/GET
- **Path:** `/hascoapi/api/dp2/create/{json}`
- **Auth Required:** Yes
- **Request Body Schema:**
  ```json
  {
    "uri": "string (optional)",
    "label": "string (required)",
    "hasDataFileUri": "string (optional)",
    "hasSIRManagerEmail": "string (required)",
    "namedGraph": "string (required)"
  }
  ```
- **Response:** Success message
- **Status Codes:** 200, 400, 401

#### Delete DP2
- **Method:** POST/GET
- **Path:** `/hascoapi/api/dp2/delete/{uri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `uri` (string, required)
- **Response:** Confirmation message
- **Status Codes:** 200, 404, 401

---

### Stream Specifications (STR)

#### Get All STRs
- **Method:** GET
- **Path:** `/hascoapi/api/str/elements/{pageSize}/{offset}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `pageSize` (integer, required)
  - `offset` (integer, required)
- **Response:** Array of STR objects
- **Status Codes:** 200, 401

#### Get Total STRs
- **Method:** GET
- **Path:** `/hascoapi/api/str/elements/total`
- **Auth Required:** Yes
- **Response:** `{"total": integer}`
- **Status Codes:** 200, 401

#### Create STR
- **Method:** POST/GET
- **Path:** `/hascoapi/api/str/create/{json}`
- **Auth Required:** Yes
- **Request Body Schema:**
  ```json
  {
    "uri": "string (optional)",
    "label": "string (required)",
    "hasDataFileUri": "string (optional)",
    "hasSIRManagerEmail": "string (required)",
    "namedGraph": "string (required)"
  }
  ```
- **Response:** Success message
- **Status Codes:** 200, 400, 401

#### Delete STR
- **Method:** POST/GET
- **Path:** `/hascoapi/api/str/delete/{uri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `uri` (string, required)
- **Response:** Confirmation message
- **Status Codes:** 200, 404, 401

---

### Workflows (WKF)

#### Get All WKFs
- **Method:** GET
- **Path:** `/hascoapi/api/wkf/elements/{pageSize}/{offset}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `pageSize` (integer, required)
  - `offset` (integer, required)
- **Response:** Array of WKF objects
- **Status Codes:** 200, 401

#### Get Total WKFs
- **Method:** GET
- **Path:** `/hascoapi/api/wkf/elements/total`
- **Auth Required:** Yes
- **Response:** `{"total": integer}`
- **Status Codes:** 200, 401

#### Create WKF
- **Method:** POST/GET
- **Path:** `/hascoapi/api/wkf/create/{json}`
- **Auth Required:** Yes
- **Request Body Schema:**
  ```json
  {
    "uri": "string (optional)",
    "label": "string (required)",
    "hasDataFileUri": "string (optional)",
    "hasSIRManagerEmail": "string (required)",
    "namedGraph": "string (required)"
  }
  ```
- **Response:** Success message
- **Status Codes:** 200, 400, 401

#### Delete WKF
- **Method:** POST/GET
- **Path:** `/hascoapi/api/wkf/delete/{uri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `uri` (string, required)
- **Response:** Confirmation message
- **Status Codes:** 200, 404, 401

---

### Instruments & Components

#### Create Container Slots
- **Method:** POST/GET
- **Path:** `/hascoapi/api/slots/container/create/{containerUri}/{totSlots}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `containerUri` (string, required) - Container URI
  - `totSlots` (string, required) - Total number of slots to create
- **Response:** Confirmation message
- **Status Codes:** 200, 400, 401

#### Attach Element to Container Slot
- **Method:** GET
- **Path:** `/hascoapi/api/slots/container/attach/{elementUri}/{containerSlotUri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `elementUri` (string, required) - Element to attach
  - `containerSlotUri` (string, required) - Target slot
- **Response:** Confirmation message
- **Status Codes:** 200, 404, 401

#### Detach from Container Slot
- **Method:** GET
- **Path:** `/hascoapi/api/slots/container/detach/{containerSlotUri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `containerSlotUri` (string, required)
- **Response:** Confirmation message
- **Status Codes:** 200, 404, 401

#### Get Slot Elements by Container
- **Method:** GET
- **Path:** `/hascoapi/api/slotelements/bycontainer/{containerUri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `containerUri` (string, required)
- **Response:** Array of slot element objects
- **Status Codes:** 200, 404, 401

---

### Codebooks & Response Options

#### Create Codebook Slots
- **Method:** POST
- **Path:** `/hascoapi/api/slots/codebook/create/{codebookUri}/{totSlots}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `codebookUri` (string, required)
  - `totSlots` (string, required)
- **Response:** Confirmation message
- **Status Codes:** 200, 400, 401

#### Delete Codebook Slots
- **Method:** POST
- **Path:** `/hascoapi/api/slots/codebook/delete/{codebookUri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `codebookUri` (string, required)
- **Response:** Confirmation message
- **Status Codes:** 200, 404, 401

#### Attach Response Option to Codebook Slot
- **Method:** GET
- **Path:** `/hascoapi/api/slots/codebook/attach/{responseOptionUri}/{codebookSlotUri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `responseOptionUri` (string, required)
  - `codebookSlotUri` (string, required)
- **Response:** Confirmation message
- **Status Codes:** 200, 404, 401

#### Attach with Status
- **Method:** GET
- **Path:** `/hascoapi/api/slots/codebook/attach/status/{responseOptionUri}/{codebookSlotUri}/{statusUri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `responseOptionUri` (string, required)
  - `codebookSlotUri` (string, required)
  - `statusUri` (string, required)
- **Response:** Confirmation message
- **Status Codes:** 200, 404, 401

#### Detach from Codebook Slot
- **Method:** GET
- **Path:** `/hascoapi/api/slots/codebook/detach/{codebookSlotUri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `codebookSlotUri` (string, required)
- **Response:** Confirmation message
- **Status Codes:** 200, 404, 401

#### Get Codebook Slots by Codebook
- **Method:** GET
- **Path:** `/hascoapi/api/slots/bycodebook/{codebookUri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `codebookUri` (string, required)
- **Response:** Array of codebook slot objects
- **Status Codes:** 200, 404, 401

---

### Annotations & Semantic Variables

#### Get All Annotations
- **Method:** GET
- **Path:** `/hascoapi/api/annotation/elements/{pageSize}/{offset}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `pageSize` (integer, required)
  - `offset` (integer, required)
- **Response:** Array of Annotation objects
- **Status Codes:** 200, 401

#### Get Annotation by Container and Position
- **Method:** GET
- **Path:** `/hascoapi/api/annotationsbycontainerposition/{containerUri}/{positionUri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `containerUri` (string, required)
  - `positionUri` (string, required)
- **Response:** Annotation object
- **Status Codes:** 200, 404, 401

#### Create Annotation
- **Method:** POST/GET
- **Path:** `/hascoapi/api/annotation/create/{json}`
- **Auth Required:** Yes
- **Request Body Schema:**
  ```json
  {
    "uri": "string (optional)",
    "label": "string (required)",
    "hasEntity": "string (optional, entity URI)",
    "hasAttribute": "string (optional, attribute URI)",
    "hasUnit": "string (optional, unit URI)",
    "hasInRelationTo": "string (optional, entity URI)",
    "hasSIRManagerEmail": "string (required)",
    "namedGraph": "string (required)"
  }
  ```
- **Response:** Success message
- **Status Codes:** 200, 400, 401

#### Delete Annotation
- **Method:** POST/GET
- **Path:** `/hascoapi/api/annotation/delete/{uri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `uri` (string, required)
- **Response:** Confirmation message
- **Status Codes:** 200, 404, 401

---

### Organizations & People

#### Get All Organizations
- **Method:** GET
- **Path:** `/hascoapi/api/organization/elements/{pageSize}/{offset}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `pageSize` (integer, required)
  - `offset` (integer, required)
- **Response:** Array of Organization objects
- **Status Codes:** 200, 401

#### Get Sub-Organizations
- **Method:** GET
- **Path:** `/hascoapi/api/organization/suborganizations/{uri}/{pageSize}/{offset}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `uri` (string, required) - Parent organization URI
  - `pageSize` (integer, required)
  - `offset` (integer, required)
- **Response:** Array of Organization objects
- **Status Codes:** 200, 404, 401

#### Get Affiliations
- **Method:** GET
- **Path:** `/hascoapi/api/organization/affiliations/{uri}/{pageSize}/{offset}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `uri` (string, required) - Organization URI
  - `pageSize` (integer, required)
  - `offset` (integer, required)
- **Response:** Array of Person objects affiliated with the organization
- **Status Codes:** 200, 404, 401

#### Create Organization
- **Method:** POST/GET
- **Path:** `/hascoapi/api/organization/create/{json}`
- **Auth Required:** Yes
- **Request Body Schema:**
  ```json
  {
    "uri": "string (optional)",
    "label": "string (required)",
    "name": "string (required)",
    "hasURL": "string (optional)",
    "hasSIRManagerEmail": "string (required)",
    "namedGraph": "string (required)"
  }
  ```
- **Response:** Success message
- **Status Codes:** 200, 400, 401

#### Delete Organization
- **Method:** POST/GET
- **Path:** `/hascoapi/api/organization/delete/{uri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `uri` (string, required)
- **Response:** Confirmation message
- **Status Codes:** 200, 404, 401

#### Get All People
- **Method:** GET
- **Path:** `/hascoapi/api/person/elements/{pageSize}/{offset}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `pageSize` (integer, required)
  - `offset` (integer, required)
- **Response:** Array of Person objects
- **Status Codes:** 200, 401

#### Create Person
- **Method:** POST/GET
- **Path:** `/hascoapi/api/person/create/{json}`
- **Auth Required:** Yes
- **Request Body Schema:**
  ```json
  {
    "uri": "string (optional)",
    "label": "string (required)",
    "givenName": "string (required)",
    "familyName": "string (required)",
    "mbox": "string (optional, email)",
    "hasSIRManagerEmail": "string (required)",
    "namedGraph": "string (required)"
  }
  ```
- **Response:** Success message
- **Status Codes:** 200, 400, 401

#### Delete Person
- **Method:** POST/GET
- **Path:** `/hascoapi/api/person/delete/{uri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `uri` (string, required)
- **Response:** Confirmation message
- **Status Codes:** 200, 404, 401

---

### Places & Addresses

#### Get All Places
- **Method:** GET
- **Path:** `/hascoapi/api/place/elements/{pageSize}/{offset}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `pageSize` (integer, required)
  - `offset` (integer, required)
- **Response:** Array of Place objects
- **Status Codes:** 200, 401

#### Get Places Contained by Place
- **Method:** GET
- **Path:** `/hascoapi/api/place/contains/place/{uri}/{pageSize}/{offset}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `uri` (string, required) - Parent place URI
  - `pageSize` (integer, required)
  - `offset` (integer, required)
- **Response:** Array of Place objects
- **Status Codes:** 200, 404, 401

#### Get Postal Addresses by Place
- **Method:** GET
- **Path:** `/hascoapi/api/place/contains/postaladdress/{placeuri}/{pageSize}/{offset}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `placeuri` (string, required)
  - `pageSize` (integer, required)
  - `offset` (integer, required)
- **Response:** Array of PostalAddress objects
- **Status Codes:** 200, 404, 401

#### Create Place
- **Method:** POST/GET
- **Path:** `/hascoapi/api/place/create/{json}`
- **Auth Required:** Yes
- **Request Body Schema:**
  ```json
  {
    "uri": "string (optional)",
    "label": "string (required)",
    "hasSIRManagerEmail": "string (required)",
    "namedGraph": "string (required)"
  }
  ```
- **Response:** Success message
- **Status Codes:** 200, 400, 401

#### Delete Place
- **Method:** POST/GET
- **Path:** `/hascoapi/api/place/delete/{uri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `uri` (string, required)
- **Response:** Confirmation message
- **Status Codes:** 200, 404, 401

---

### Data Files & Blob Management

#### Upload File
- **Method:** POST
- **Path:** `/hascoapi/api/uploadFile/{elementuri}/{filename}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `elementuri` (string, required) - URI of the element (e.g., SDD, DSG, DP2, INS, KGR, STR, WKF)
  - `filename` (string, required) - Name of the file being uploaded
- **Request Body:** Binary file content (multipart/form-data)
- **Response:** Confirmation message with file location
- **Status Codes:** 200, 400, 401, 500

#### Upload Media
- **Method:** POST
- **Path:** `/hascoapi/api/uploadMedia/{foldername}/{filename}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `foldername` (string, required) - Destination folder name
  - `filename` (string, required) - File name
- **Request Body:** Binary file content (multipart/form-data)
- **Response:** Confirmation message with file location
- **Status Codes:** 200, 400, 401, 500

#### Download File
- **Method:** POST
- **Path:** `/hascoapi/api/downloadFile/{elementuri}/{filename}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `elementuri` (string, required) - URI of the element
  - `filename` (string, required) - File name
- **Response:** Binary file content or error message
- **Status Codes:** 200, 404, 401, 500

---

### Metadata Template Generation

#### Health Check (MT Generation)
- **Method:** GET
- **Path:** `/hascoapi/api/mt/gen/health` or `/api/mt/gen/health`
- **Auth Required:** No
- **Response:** `"MT Generation routes are working!"`
- **Status Codes:** 200

#### Generate MT by Status
- **Method:** GET/POST
- **Path:** `/hascoapi/api/mt/gen/perstatus/{elementtype}/{datafileuri}/{status}/{filename}/{mediafolder}/{verifyuri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `elementtype` (string, required) - Type of element (e.g., "study", "instrument", "sdd")
  - `datafileuri` (string, required) - DataFile URI (URL-encoded)
  - `status` (string, required) - Status filter (e.g., "ACTIVE", "DRAFT")
  - `filename` (string, required) - Output filename
  - `mediafolder` (string, required) - Media folder path
  - `verifyuri` (string, required) - Verification URI
- **Query Parameters:**
  - `generateDASOCs` (boolean, optional, default: false) - Generate DA-SOC files
- **Response:** Generated file path or error message
- **Status Codes:** 200, 400, 401

#### Generate MT by Element
- **Method:** GET/POST
- **Path:** `/hascoapi/api/mt/gen/perelement/{elementtype}/{datafileuri}/{elementuri}/{filename}/{mediafolder}/{verifyuri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `elementtype` (string, required)
  - `datafileuri` (string, required)
  - `elementuri` (string, required) - Specific element URI (URL-encoded)
  - `filename` (string, required)
  - `mediafolder` (string, required)
  - `verifyuri` (string, required)
- **Query Parameters:**
  - `generateDASOCs` (boolean, optional, default: false)
- **Response:** Generated file path or error message
- **Status Codes:** 200, 400, 401, 404

#### Generate MT by Manager
- **Method:** GET/POST
- **Path:** `/hascoapi/api/mt/gen/peruser/{elementtype}/{datafileuri}/{useremail}/{status}/{filename}/{mediafolder}/{verifyuri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `elementtype` (string, required)
  - `datafileuri` (string, required)
  - `useremail` (string, required) - Manager email
  - `status` (string, required)
  - `filename` (string, required)
  - `mediafolder` (string, required)
  - `verifyuri` (string, required)
- **Query Parameters:**
  - `generateDASOCs` (boolean, optional, default: false)
- **Response:** Generated file path or error message
- **Status Codes:** 200, 400, 401

#### Get Generated MT File
- **Method:** POST
- **Path:** `/hascoapi/api/mt/get/generated/{filename}` or `/api/mt/get/generated/{filename}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `filename` (string, required) - Name of generated file
- **Response:** Binary file content (Excel .xlsx)
- **Content-Type:** `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- **Status Codes:** 200, 404, 401

---

### Ingestion

#### Ingest File
- **Method:** POST
- **Path:** `/hascoapi/api/ingest/{status}/{elementType}/{elementUri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `status` (string, required) - Processing status
  - `elementType` (string, required) - One of: dp2, dsg, da, ins, kgr, sdd, str, wkf
  - `elementUri` (string, required) - URI of metadata template instance (URL-encoded)
- **Request Body:** Binary file content (multipart/form-data) - CSV or Excel file
- **Response:** Ingestion result with success/failure message
- **Status Codes:** 200, 400, 401, 500
- **Description:** Ingests data from uploaded file according to metadata template. For "da" type, only DA-SOC-* files are supported (filename validation enforced).

#### Uningest Data File
- **Method:** GET
- **Path:** `/hascoapi/api/uningest/{dataFileUri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `dataFileUri` (string, required) - DataFile URI to uningest (URL-encoded)
- **Response:** Confirmation message
- **Status Codes:** 200, 404, 401
- **Description:** Removes ingested data associated with a DataFile from the knowledge graph

#### Uningest Metadata Template
- **Method:** GET
- **Path:** `/hascoapi/api/uningest/mt/{metadataTemplateUri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `metadataTemplateUri` (string, required) - Metadata Template URI (URL-encoded)
- **Response:** Confirmation message
- **Status Codes:** 200, 404, 401

#### Get Ingestion Log
- **Method:** GET
- **Path:** `/hascoapi/api/ingestion/{dataFileUri}/log`
- **Auth Required:** Yes
- **Path Parameters:**
  - `dataFileUri` (string, required) - DataFile URI (URL-encoded)
- **Response:** Ingestion log content
- **Status Codes:** 200, 404, 401

---

### Generic Retrieval Methods

#### Get URI Information
- **Method:** GET
- **Path:** `/hascoapi/api/uri/{uri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `uri` (string, required) - URI to query (URL-encoded)
- **Response:** Object details
- **Status Codes:** 200, 404, 401

#### Generate URI for Element Type
- **Method:** GET
- **Path:** `/hascoapi/api/urigen/{elementtype}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `elementtype` (string, required) - Element type
- **Response:** `{"uri": "generated-uri"}`
- **Status Codes:** 200, 400, 401

#### Get Usage
- **Method:** GET
- **Path:** `/hascoapi/api/usage/{uri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `uri` (string, required) - URI (URL-encoded)
- **Response:** Usage information
- **Status Codes:** 200, 404, 401

#### Get Derivation
- **Method:** GET
- **Path:** `/hascoapi/api/derivation/{uri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `uri` (string, required) - URI (URL-encoded)
- **Response:** Derivation information
- **Status Codes:** 200, 404, 401

#### Get HAScO Type
- **Method:** GET
- **Path:** `/hascoapi/api/hascotype/{uri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `uri` (string, required) - URI (URL-encoded)
- **Response:** `{"hascoType": "type-uri"}`
- **Status Codes:** 200, 404, 401

#### Get Children
- **Method:** GET
- **Path:** `/hascoapi/api/children/{superuri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `superuri` (string, required) - Parent class URI (URL-encoded)
- **Response:** Array of child class URIs
- **Status Codes:** 200, 404, 401

#### Get Superclasses
- **Method:** GET
- **Path:** `/hascoapi/api/superclasses/{uri}`
- **Auth Required:** Yes
- **Path Parameters:**
  - `uri` (string, required) - Class URI (URL-encoded)
- **Response:** Array of superclass URIs
- **Status Codes:** 200, 404, 401

---

## Postman Test Suite

### Prerequisites

Create a Postman environment with these variables:
- `baseUrl`: `http://localhost:9000` (or your server URL)
- `token`: Your JWT authentication token
- `testInstrumentUri`: Will be set dynamically
- `testStudyUri`: Will be set dynamically
- `testSDDUri`: Will be set dynamically

---

### Test Endpoint 1: List All Instruments (GET - Paginated)

**Request:**
```
Method: GET
URL: {{baseUrl}}/hascoapi/api/instrument/elements/10/0
Headers:
  Authorization: Bearer {{token}}
  Content-Type: application/json
```

**Postman Tests (JavaScript):**
```javascript
// Status code validation
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

// Response time check
pm.test("Response time is less than 2000ms", function () {
    pm.expect(pm.response.responseTime).to.be.below(2000);
});

// Response structure validation
pm.test("Response has correct structure", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property('isSuccessful');
    pm.expect(jsonData).to.have.property('body');
});

// Success flag check
pm.test("Response is successful", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.isSuccessful).to.be.true;
});

// Body is array when successful
pm.test("Body is an array", function () {
    var jsonData = pm.response.json();
    if (jsonData.isSuccessful) {
        pm.expect(jsonData.body).to.be.an('array');
    }
});

// Pagination check - should return max 10 items
pm.test("Returns correct page size", function () {
    var jsonData = pm.response.json();
    if (jsonData.isSuccessful && jsonData.body.length > 0) {
        pm.expect(jsonData.body.length).to.be.at.most(10);
    }
});

// Each instrument has required fields
pm.test("Instruments have required fields", function () {
    var jsonData = pm.response.json();
    if (jsonData.isSuccessful && jsonData.body.length > 0) {
        var instrument = jsonData.body[0];
        pm.expect(instrument).to.have.property('uri');
        pm.expect(instrument).to.have.property('label');
        pm.expect(instrument).to.have.property('typeUri');
    }
});
```

**Test Scenarios:**
1. **Happy Path** - Valid authentication, returns paginated instruments
2. **No Authentication** - Missing Bearer token, should return 401
3. **Invalid Page Size** - Negative page size, should handle gracefully
4. **Large Offset** - Offset beyond total count, returns empty array
5. **Invalid Token** - Expired or malformed token, should return 401

---

### Test Endpoint 2: Get Instrument by URI (GET)

**Request:**
```
Method: GET
URL: {{baseUrl}}/hascoapi/api/uri/{{testInstrumentUri}}
Headers:
  Authorization: Bearer {{token}}
  Content-Type: application/json
```

**Postman Tests (JavaScript):**
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response time is acceptable", function () {
    pm.expect(pm.response.responseTime).to.be.below(1500);
});

pm.test("Response has valid structure", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property('isSuccessful');
    pm.expect(jsonData).to.have.property('body');
});

pm.test("Instrument data is returned", function () {
    var jsonData = pm.response.json();
    if (jsonData.isSuccessful) {
        pm.expect(jsonData.body).to.be.an('object');
        pm.expect(jsonData.body).to.have.property('uri');
        pm.expect(jsonData.body.uri).to.equal(pm.environment.get("testInstrumentUri"));
    }
});

pm.test("Instrument has complete metadata", function () {
    var jsonData = pm.response.json();
    if (jsonData.isSuccessful) {
        pm.expect(jsonData.body).to.have.property('label');
        pm.expect(jsonData.body).to.have.property('typeUri');
        pm.expect(jsonData.body).to.have.property('hasSIRManagerEmail');
    }
});

pm.test("Manager email is valid format", function () {
    var jsonData = pm.response.json();
    if (jsonData.isSuccessful && jsonData.body.hasSIRManagerEmail) {
        var emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        pm.expect(jsonData.body.hasSIRManagerEmail).to.match(emailRegex);
    }
});
```

**Test Scenarios:**
1. **Happy Path** - Valid URI returns complete instrument data
2. **Not Found** - Non-existent URI returns 404 or error message
3. **Malformed URI** - Invalid URI format, should handle gracefully
4. **No Authentication** - Missing token returns 401
5. **URL Encoding** - URI with special characters is properly encoded

---

### Test Endpoint 3: Create Instrument (POST)

**Request:**
```
Method: POST
URL: {{baseUrl}}/hascoapi/api/instrument/create/{{instrumentJson}}
Headers:
  Authorization: Bearer {{token}}
  Content-Type: application/json
```

**Pre-request Script:**
```javascript
// Generate unique instrument URI
var timestamp = Date.now();
var instrumentUri = pm.environment.get("baseUrl") + "/kb/INS" + timestamp;

// Create instrument JSON
var instrumentData = {
    "uri": instrumentUri,
    "typeUri": "http://hadatac.org/ont/vstoi#Questionnaire",
    "hascoTypeUri": "http://hadatac.org/ont/vstoi#Instrument",
    "label": "Test Instrument " + timestamp,
    "hasShortName": "TEST" + timestamp,
    "comment": "Automated test instrument created via Postman",
    "hasVersion": "1.0",
    "hasLanguage": "en",
    "hasInformant": "http://hadatac.org/ont/vstoi#SelfReportedInformant",
    "hasSIRManagerEmail": "test@example.com",
    "namedGraph": "http://hadatac.org/kb/test"
};

// URL-encode the JSON
var encodedJson = encodeURIComponent(JSON.stringify(instrumentData));
pm.environment.set("instrumentJson", encodedJson);
pm.environment.set("testInstrumentUri", instrumentUri);
```

**Postman Tests (JavaScript):**
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response time is reasonable", function () {
    pm.expect(pm.response.responseTime).to.be.below(3000);
});

pm.test("Response indicates success", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property('isSuccessful');
    pm.expect(jsonData.isSuccessful).to.be.true;
});

pm.test("Response contains creation confirmation", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.body).to.include("CREATED");
});

pm.test("Created URI is returned", function () {
    var jsonData = pm.response.json();
    var createdUri = pm.environment.get("testInstrumentUri");
    pm.expect(jsonData.body).to.include(createdUri);
});

// Cleanup script - save URI for later deletion
pm.test("Save created instrument URI for cleanup", function () {
    pm.environment.set("lastCreatedInstrument", pm.environment.get("testInstrumentUri"));
});
```

**Test Scenarios:**
1. **Happy Path** - Valid data creates instrument successfully
2. **Missing Required Field** - No label, should return error
3. **Invalid Email Format** - Malformed manager email, should validate
4. **Duplicate URI** - URI already exists, should return conflict error
5. **No Authentication** - Missing token returns 401

---

### Test Endpoint 4: Update Study (conceptual - using create/delete pattern)

**Note:** The API uses create/delete pattern rather than traditional PUT/PATCH. To "update", you would delete and recreate. Here's a conceptual update using element modification:

**Request:**
```
Method: POST
URL: {{baseUrl}}/hascoapi/api/study/create/{{studyJson}}
Headers:
  Authorization: Bearer {{token}}
  Content-Type: application/json
```

**Pre-request Script:**
```javascript
var timestamp = Date.now();
var studyUri = pm.environment.get("baseUrl") + "/kb/STU" + timestamp;

var studyData = {
    "uri": studyUri,
    "label": "Updated Test Study " + timestamp,
    "title": "Updated Study Title",
    "project": pm.environment.get("baseUrl") + "/kb/PRJ001",
    "comment": "This study was updated via API",
    "hasSIRManagerEmail": "manager@example.com",
    "permissionUri": "http://hadatac.org/ont/hasco#PrivatePermission",
    "namedGraph": "http://hadatac.org/kb/test"
};

var encodedJson = encodeURIComponent(JSON.stringify(studyData));
pm.environment.set("studyJson", encodedJson);
pm.environment.set("testStudyUri", studyUri);
```

**Postman Tests (JavaScript):**
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Study created/updated successfully", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.isSuccessful).to.be.true;
});

pm.test("Response time is acceptable", function () {
    pm.expect(pm.response.responseTime).to.be.below(2500);
});

pm.test("Success message contains URI", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.body).to.be.a('string');
    pm.expect(jsonData.body).to.include('STU');
});

pm.test("Verify fields are present in request", function () {
    var studyJson = JSON.parse(decodeURIComponent(pm.environment.get("studyJson")));
    pm.expect(studyJson).to.have.property('label');
    pm.expect(studyJson).to.have.property('title');
    pm.expect(studyJson).to.have.property('hasSIRManagerEmail');
});
```

**Test Scenarios:**
1. **Happy Path** - Valid update data modifies study
2. **Missing Title** - Required field missing, should fail
3. **Invalid Permission URI** - Non-existent permission, should validate
4. **No Authorization** - User doesn't own study, should return 403
5. **Concurrent Update** - Two simultaneous updates, handle race condition

---

### Test Endpoint 5: Delete SDD (DELETE via GET)

**Request:**
```
Method: GET
URL: {{baseUrl}}/hascoapi/api/sdd/delete/{{testSDDUri}}
Headers:
  Authorization: Bearer {{token}}
  Content-Type: application/json
```

**Pre-request Script:**
```javascript
// First create an SDD to delete
var timestamp = Date.now();
var sddUri = pm.environment.get("baseUrl") + "/kb/SDD" + timestamp;
pm.environment.set("testSDDUri", encodeURIComponent(sddUri));
```

**Postman Tests (JavaScript):**
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response time is fast", function () {
    pm.expect(pm.response.responseTime).to.be.below(1000);
});

pm.test("Deletion successful", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.isSuccessful).to.be.true;
});

pm.test("Response confirms deletion", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.body).to.include("DELETED");
});

pm.test("Deleted URI is mentioned", function () {
    var jsonData = pm.response.json();
    var decodedUri = decodeURIComponent(pm.environment.get("testSDDUri"));
    pm.expect(jsonData.body).to.include(decodedUri);
});

// Verify deletion by attempting to retrieve
pm.test("Verify SDD no longer exists", function () {
    pm.sendRequest({
        url: pm.environment.get("baseUrl") + "/hascoapi/api/uri/" + pm.environment.get("testSDDUri"),
        method: 'GET',
        header: {
            'Authorization': 'Bearer ' + pm.environment.get("token")
        }
    }, function (err, response) {
        if (!err) {
            var data = response.json();
            pm.expect(data.isSuccessful).to.be.false;
        }
    });
});
```

**Test Scenarios:**
1. **Happy Path** - Existing SDD deleted successfully
2. **Not Found** - Non-existent SDD URI, should return 404
3. **Already Deleted** - Attempt to delete same SDD twice
4. **No Permission** - User doesn't own SDD, should return 403
5. **Referenced SDD** - SDD in use by other resources, should prevent deletion or cascade

---

### Additional Recommended Tests

#### Get Total Count Endpoint
```
GET {{baseUrl}}/hascoapi/api/instrument/elements/total
```

#### File Upload Endpoint
```
POST {{baseUrl}}/hascoapi/api/uploadFile/{{elementUri}}/{{filename}}
Body: Binary file data (multipart/form-data)
```

#### MT Generation Endpoint
```
POST {{baseUrl}}/hascoapi/api/mt/gen/perstatus/study/{{dataFileUri}}/ACTIVE/output.xlsx/media/true
```

#### Ingestion Endpoint
```
POST {{baseUrl}}/hascoapi/api/ingest/ACTIVE/sdd/{{sddUri}}
Body: CSV/Excel file
```

---

## Common Error Responses

### 400 Bad Request
```json
{
  "isSuccessful": false,
  "body": "No json content has been provided."
}
```

### 401 Unauthorized
```json
{
  "isSuccessful": false,
  "body": "Authentication required"
}
```

### 404 Not Found
```json
{
  "isSuccessful": false,
  "body": "No instrument with URI <uri> has been found"
}
```

### 500 Internal Server Error
```json
{
  "isSuccessful": false,
  "body": "Error processing request: [error details]"
}
```

---

## Notes

1. **URL Encoding**: URIs in path parameters must be URL-encoded
2. **JSON in Path**: Some endpoints accept JSON as a path parameter (must be URL-encoded)
3. **Pagination**: Most list endpoints support `pageSize` and `offset` parameters
4. **Authentication**: JWT token must be in `Authorization: Bearer {token}` header
5. **Element Types**: Common types include: instrument, study, sdd, dsg, dp2, ins, kgr, str, wkf, da, deployment, stream, studyobject, studyobjectcollection
6. **Named Graphs**: All created entities require a `namedGraph` field for RDF storage
7. **Manager Email**: Most entities require `hasSIRManagerEmail` for ownership tracking

---

**End of Documentation**

