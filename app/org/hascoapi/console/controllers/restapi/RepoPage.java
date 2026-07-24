package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import org.hascoapi.Constants;
import org.hascoapi.RepositoryInstance;
import org.hascoapi.console.controllers.ontologies.LoadOnt;
import org.hascoapi.entity.pojo.HADatAcClass;
import org.hascoapi.entity.pojo.NameSpace;
import org.hascoapi.entity.pojo.Repository;
import org.hascoapi.entity.pojo.Table;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.ConfigProp;
import org.hascoapi.utils.HAScOMapper;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.vocabularies.HASCO;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;

import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.BodyParser;
import com.typesafe.config.ConfigFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RepoPage extends Controller {

    public Result getRepository() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            // get the list of variables in that study
            // serialize the Study object first as ObjectNode
            //   as JsonNode is immutable and meant to be read-only
            ObjectNode obj = mapper.convertValue(RepositoryInstance.getInstance(), ObjectNode.class);
            JsonNode jsonObject = mapper.convertValue(obj, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        } catch (Exception e) {
            e.printStackTrace();
            return badRequest(ApiUtil.createResponse("Error parsing class " + Repository.className, false));
        }
    }

    public Result updateLabel(String label){
        if (label == null || label.equals("")) {
            return ok(ApiUtil.createResponse("No (name) has been provided.", false));
        }
        RepositoryInstance.getInstance().setLabel(label);
        RepositoryInstance.getInstance().save();
        return ok(ApiUtil.createResponse("Repository's (name) has been UPDATED.", true));
    }

    public Result updateTitle(String title){
        if (title == null || title.equals("")) {
            return ok(ApiUtil.createResponse("No (title) has been provided.", false));
        }
        RepositoryInstance.getInstance().setTitle(title);
        RepositoryInstance.getInstance().save();
        return ok(ApiUtil.createResponse("Repository's (title) has been UPDATED.", true));
    }

    public Result updateURL(String url){
        if (url == null || url.equals("")) {
            return ok(ApiUtil.createResponse("No (domainURL) has been provided.", false));
        }
        RepositoryInstance.getInstance().setHasDomainURL(url);
        RepositoryInstance.getInstance().save();
        return ok(ApiUtil.createResponse("Repository's (domainURL) has been UPDATED.", true));
    }

    public Result updateDescription(String description){
        if (description == null || description.equals("")) {
            return ok(ApiUtil.createResponse("No (description) has been provided.", false));
        }
        RepositoryInstance.getInstance().setComment(description);
        RepositoryInstance.getInstance().save();
        return ok(ApiUtil.createResponse("Repository's (description) has been UPDATED.", true));
    }

    public Result updateDefaultNamespace(String prefix, String url, String sourceMime, String source) {
        if (sourceMime.equals("_")) { 
            sourceMime = "";
        }
        if (source.equals("_")) { 
            source = "";
        }
        //System.out.println("updateDefaultNamespace:");
        //System.out.println("    - Namespace prefix: [" + prefix + "]");
        //System.out.println("    - Namespace url: [" + url + "]");
        //System.out.println("    - Namespace mime: [" + sourceMime + "]");
        //System.out.println("    - Namespace source: [" + source + "]");
        if (prefix == null || prefix.equals("")) {
            return ok(ApiUtil.createResponse("No (prefix) has been provided.", false));
        }
        if (url == null || url.equals("")) {
            return ok(ApiUtil.createResponse("No (url) has been provided.", false));
        }
        if (sourceMime == null) {
            sourceMime = "";
        }
        if (source == null) {
            source = "";
        }
        RepositoryInstance.getInstance().setHasDefaultNamespacePrefix(prefix);
        System.out.println("RepoPage: default namespace prefix is [" + prefix + "]");
        RepositoryInstance.getInstance().setHasDefaultNamespaceURL(url);
        RepositoryInstance.getInstance().setHasDefaultNamespaceSourceMime(sourceMime);
        RepositoryInstance.getInstance().setHasDefaultNamespaceSource(source);
        RepositoryInstance.getInstance().save();
        NameSpaces.getInstance().updateLocalNamespace();
        return ok(ApiUtil.createResponse("Repository's local namespace has been UPDATED.", true));
    }

    public Result updateNamespace(String abbreviation, String url){
        if (abbreviation == null || abbreviation.equals("")) {
            return ok(ApiUtil.createResponse("No (abbreviation) has been provided.", false));
        }
        if (url == null || url.equals("")) {
            return ok(ApiUtil.createResponse("No (url) has been provided.", false));
        }
        RepositoryInstance.getInstance().setHasNamespaceAbbreviation(abbreviation);
        RepositoryInstance.getInstance().setHasNamespaceURL(url);
        RepositoryInstance.getInstance().save();
        NameSpaces.getInstance().resetNameSpaces();;
        return ok(ApiUtil.createResponse("Repository's local namespace has been UPDATED.", true));
    }

    public Result createNamespace(String json){
        if (json == null || json.equals("")) {
            return ok(ApiUtil.createResponse("No JSON has been provided.", false));
        }
        if (RepositoryInstance.getInstance().newNamespace(json)) {
            RepositoryInstance.getInstance().save();
            //NameSpaces.getInstance().resetNameSpaces();
            return ok(ApiUtil.createResponse("New namespace has been added to the repository.", true));
        } else {
            return ok(ApiUtil.createResponse("Failed to add new namespace into the repository.", false));
        }
    }

    public Result resetNamespaces(){
        if (RepositoryInstance.getInstance().resetNamespaces()) {
            NameSpaces.getInstance().resetNameSpaces();;
            return ok(ApiUtil.createResponse("Namespaces have been reset.", true));
        } else {
            return ok(ApiUtil.createResponse("Failed to reset namespaces.", false));
        }
    }

    public Result deleteSelectedNamespace(String abbreviation){
        if (abbreviation == null || abbreviation.equals("")) {
            return ok(ApiUtil.createResponse("No Namespace's ABBREVIATION has been provided.", false));
        }
        String response = NameSpace.deleteNamespace(abbreviation);
        NameSpaces.getInstance().resetNameSpaces();;
        if (response.isEmpty()) {
            return ok(ApiUtil.createResponse("Namespace [" + abbreviation + "] has been DELETED.", true));
        } else {
            return ok(ApiUtil.createResponse("Namespace [" + abbreviation + "] has NOT been DELETED. Reason: " + response, false));
        }
    }

    public Result deleteNamespace(){
        String prefix = RepositoryInstance.getInstance().getHasDefaultNamespacePrefix();
        String url = RepositoryInstance.getInstance().getHasDefaultNamespaceURL();
        String mime = RepositoryInstance.getInstance().getHasDefaultNamespaceSourceMime();
        String source = RepositoryInstance.getInstance().getHasDefaultNamespaceSource();
        if (prefix == null || prefix.equals("") || 
            url == null    || url.equals("") ||
            mime == null   || mime.equals("") ||
            source == null || source.equals("")) {
            return ok(ApiUtil.createResponse("There is no default namespace to be deleted.", false));
        }
        RepositoryInstance.getInstance().setHasDefaultNamespacePrefix("");
        RepositoryInstance.getInstance().setHasDefaultNamespaceURL("");
        RepositoryInstance.getInstance().setHasDefaultNamespaceSourceMime("");
        RepositoryInstance.getInstance().setHasDefaultNamespaceSource("");
        RepositoryInstance.getInstance().save();
        NameSpaces.getInstance().deleteLocalNamespace();
        return ok(ApiUtil.createResponse("Repository's local namespace has been DELETED.", true));
    }

    private Long manageTriples(String oper, String kb) {
        LoadOnt.playLoadOntologiesAsync(oper, kb);
        return 0L;
    }
    
    /**
     * Handles application ontology upload, save permanently, delete old triples, and ingest new ones.
     */
    public Result ingestAppOntology(Http.Request request) {
        File tempFile = request.body().asRaw().asFile();
        if (tempFile == null) {
            return ok(ApiUtil.createResponse(
                "[ERROR] RepoPage.ingestAppOntology(): No file has been provided for ingestion.", false
            ));
        }

        String basePath = ConfigProp.getPathAppOntology();
        if (basePath == null || basePath.trim().isEmpty()) {
            System.err.println("[ERROR] RepoPage.ingestAppOntology(): Invalid file storage path from ConfigProp.getPathAppOntology()");
            return internalServerError(ApiUtil.createResponse(
                "[ERROR] RepoPage.ingestAppOntology(): Invalid file storage path.", false
            ));
        }

        String filename = tempFile.getName();
        Path permanentPath = Paths.get(basePath, filename);

        // Run asynchronously to avoid blocking the HTTP thread
        CompletableFuture.runAsync(() -> {
            try {
                // Save file permanently
                DataFileAPI.saveFile(tempFile, permanentPath);

                NameSpace appOntology = NameSpaces.getInstance().getAppOntology();
                appOntology.setSourceMime("text/turtle");

                System.out.println("ingestAppOntology: filename=[" + permanentPath + "]");
                System.out.println("ingestAppOntology: appOntologyURI=[" + appOntology.getUri() + "]");

                // Remove triples from the same named graph
                appOntology.deleteTriples();

                // Load triples from the newly saved local file
                appOntology.loadTriples(permanentPath.toString(), false);

                System.out.println("[INFO] Ontology ingestion complete for file: " + permanentPath);

            } catch (Exception e) {
                System.err.println("[ERROR] Failed during ontology ingestion: " + e.getMessage());
                e.printStackTrace();
            }
        });

        return ok(ApiUtil.createResponse("Ontology upload and ingestion in progress.", true));
    }

    /**
     * Handles arbitrary namespace ontology ingestion with auto-registration.
     * If the namespace doesn't exist, creates it dynamically following NameSpaceGenerator protocol.
     * Accepts a namespace abbreviation and URI as parameters and TTL content as request body.
     * The abbreviation MUST be provided and will be used exactly as given - NOT derived from URI.
     * 
     * @param request HTTP request containing TTL file as raw body
     * @param abbreviation The namespace abbreviation (e.g., pmsr, uberon, ncit)
     * @param namespaceUri URL-encoded namespace URI (e.g., http://pmsr.net/ont/pmsr)
     * @return Result indicating success or failure
     */
    @BodyParser.Of(BodyParser.Raw.class)
    public Result ingestNamespaceOntology(Http.Request request, String abbreviation, String namespaceUri) {
        File tempFile = request.body().asRaw().asFile();
        if (tempFile == null) {
            return ok(ApiUtil.createResponse(
                "[ERROR] RepoPage.ingestNamespaceOntology(): No file has been provided for ingestion.", false
            ));
        }

        // Decode the namespace URI and abbreviation
        final String decodedUri = java.net.URLDecoder.decode(namespaceUri, java.nio.charset.StandardCharsets.UTF_8);
        final String decodedAbbrev = java.net.URLDecoder.decode(abbreviation, java.nio.charset.StandardCharsets.UTF_8);
        System.out.println("ingestNamespaceOntology: abbreviation=[" + decodedAbbrev + "], namespaceUri=[" + decodedUri + "]");
        System.out.println("ingestNamespaceOntology: File size: " + tempFile.length() + " bytes");
        
        // Determine MIME type from file name
        String tempFileName = tempFile.getName();
        final String sourceMime;
        
        if (tempFileName.endsWith(".owl")) {
            sourceMime = "application/rdf+xml";
        } else if (tempFileName.endsWith(".ttl")) {
            sourceMime = "text/turtle";
        } else {
            // Default to turtle
            sourceMime = "text/turtle";
        }
        
        System.out.println("ingestNamespaceOntology: Detected MIME type: " + sourceMime + " from temp file: " + tempFileName);
        
        // Copy temp file to avoid Play Framework cleanup before async processing completes
        File workingFile = null;
        try {
            workingFile = File.createTempFile("ontology_ingest_", tempFileName.endsWith(".owl") ? ".owl" : ".ttl");
            workingFile.deleteOnExit();
            java.nio.file.Files.copy(tempFile.toPath(), workingFile.toPath(), 
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            System.out.println("ingestNamespaceOntology: Copied temp file to: " + workingFile.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to copy temp file: " + e.getMessage());
            e.printStackTrace();
            return badRequest(ApiUtil.createResponse(
                "[ERROR] Failed to prepare file for ingestion: " + e.getMessage(), false
            ));
        }
        
        // Create file:// URI from working file path for named graph and source
        final String fileUri = workingFile.toURI().toString();
        System.out.println("ingestNamespaceOntology: Working file path: " + workingFile.getAbsolutePath());
        System.out.println("ingestNamespaceOntology: File URI (named graph & source): " + fileUri);
        
        final File fileToIngest = workingFile;
        
        // Find or auto-register the namespace
        final NameSpace namespace;
        NameSpace existingByUri = NameSpaces.getInstance().getNamespacesByUri().get(decodedUri);
        NameSpace existingByLabel = NameSpaces.getInstance().getNamespaces().get(decodedAbbrev);
        
        if (existingByUri != null) {
            // Found namespace by URI - verify abbreviation matches
            if (!existingByUri.getLabel().equals(decodedAbbrev)) {
                return badRequest(ApiUtil.createResponse(
                    "[ERROR] Namespace URI conflict: URI '" + decodedUri + "' exists with abbreviation '" + 
                    existingByUri.getLabel() + "' but trying to ingest with abbreviation '" + decodedAbbrev + "'. " +
                    "The namespace abbreviation must match. " +
                    "Please delete the incorrect namespace or use the correct abbreviation.", false
                ));
            }
            namespace = existingByUri;
            System.out.println("ingestNamespaceOntology: Found existing namespace by URI: " + namespace.getLabel());
            System.out.println("ingestNamespaceOntology: Will update existing namespace with new file URI");
            
        } else if (existingByLabel != null) {
            // Found namespace by label but different URI - delete and recreate (immutable pattern)
            System.out.println("ingestNamespaceOntology: Found existing namespace '" + decodedAbbrev + "' with URI '" + 
                existingByLabel.getUri() + "' but new URI is '" + decodedUri + "' - deleting old namespace");
            
            // Delete old namespace following delete-then-recreate pattern
            String deleteResponse = NameSpace.deleteNamespace(decodedAbbrev);
            if (!deleteResponse.isEmpty()) {
                System.err.println("[ERROR] Failed to delete conflicting namespace: " + deleteResponse);
                return badRequest(ApiUtil.createResponse(
                    "[ERROR] Cannot update namespace '" + decodedAbbrev + "': " + deleteResponse, false
                ));
            }
            
            // Refresh namespace cache after deletion
            NameSpaces.getInstance().resetNameSpaces();
            System.out.println("ingestNamespaceOntology: Deleted old namespace, will create new one");
            
            // Fall through to create new namespace
            NameSpace newNamespace = new NameSpace();
            newNamespace.setNamedGraph(fileUri);
            newNamespace.setLabel(decodedAbbrev);
            newNamespace.setUri(decodedUri);
            newNamespace.setTypeUri(HASCO.ONTOLOGY);
            newNamespace.setHascoTypeUri(HASCO.ONTOLOGY);
            newNamespace.setSourceMime(sourceMime);
            newNamespace.setSource(fileUri);
            newNamespace.setComment("Auto-registered via namespace ontology ingestion");
            newNamespace.setPriority(100);
            newNamespace.setPermanent(false);
            
            NameSpaces.getInstance().addNamespace(newNamespace);
            
            try {
                newNamespace.save();
                System.out.println("ingestNamespaceOntology: Successfully recreated namespace: " + decodedAbbrev);
            } catch (Exception e) {
                System.err.println("[ERROR] Failed to recreate namespace: " + e.getMessage());
                e.printStackTrace();
                NameSpaces.getInstance().getNamespaces().remove(decodedAbbrev);
                NameSpaces.getInstance().getNamespacesByUri().remove(decodedUri);
                return badRequest(ApiUtil.createResponse(
                    "[ERROR] Failed to recreate namespace: " + e.getMessage(), false
                ));
            }
            
            namespace = newNamespace;
            
        } else {
            // Namespace doesn't exist - auto-register it with the provided abbreviation (NOT derived)
            System.out.println("ingestNamespaceOntology: Auto-registering new namespace: " + decodedAbbrev + " -> " + decodedUri);
            
            NameSpace newNamespace = new NameSpace();
            newNamespace.setNamedGraph(fileUri);  // Use file:// URI as named graph
            newNamespace.setLabel(decodedAbbrev);
            newNamespace.setUri(decodedUri);
            newNamespace.setTypeUri(HASCO.ONTOLOGY);
            newNamespace.setHascoTypeUri(HASCO.ONTOLOGY);
            newNamespace.setSourceMime(sourceMime);  // Set based on file extension
            newNamespace.setSource(fileUri);  // Use file:// URI as source
            newNamespace.setComment("Auto-registered via namespace ontology ingestion");
            newNamespace.setPriority(100);
            newNamespace.setPermanent(false);
            
            // Add to in-memory cache BEFORE saving (required by NameSpaceGenerator protocol)
            NameSpaces.getInstance().addNamespace(newNamespace);
            
            // Save to triplestore
            try {
                newNamespace.save();
                System.out.println("ingestNamespaceOntology: Successfully auto-registered namespace: " + decodedAbbrev);
            } catch (Exception e) {
                System.err.println("[ERROR] Failed to auto-register namespace: " + e.getMessage());
                e.printStackTrace();
                // Remove from cache on failure
                NameSpaces.getInstance().getNamespaces().remove(decodedAbbrev);
                NameSpaces.getInstance().getNamespacesByUri().remove(decodedUri);
                return badRequest(ApiUtil.createResponse(
                    "[ERROR] Failed to auto-register namespace: " + e.getMessage(), false
                ));
            }
            
            namespace = newNamespace;
        }

        // Update namespace metadata with file URI and MIME type
        namespace.setSourceMime(sourceMime);
        namespace.setSource(fileUri);
        namespace.setNamedGraph(fileUri);
        
        // Save metadata updates to triplestore
        try {
            namespace.save();
            System.out.println("ingestNamespaceOntology: Updated namespace metadata (source, MIME, named graph)");
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to save namespace metadata: " + e.getMessage());
            e.printStackTrace();
            return badRequest(ApiUtil.createResponse(
                "[ERROR] Failed to save namespace metadata: " + e.getMessage(), false
            ));
        }

        // Run asynchronously to avoid blocking the HTTP thread
        CompletableFuture.runAsync(() -> {
            try {
                System.out.println("ingestNamespaceOntology: Starting ingestion for namespace: " + namespace.getLabel());

                // Remove existing triples from the same named graph
                namespace.deleteTriples();
                System.out.println("ingestNamespaceOntology: Deleted existing triples for namespace: " + namespace.getLabel());

                // Load triples from the temp file
                namespace.loadTriples(fileToIngest.getAbsolutePath(), false);

                // CRITICAL: Scan for and delete any auto-created namespaces
                // hascoapi must NOT create namespaces from TTL content (owl:Ontology + rdfs:label)
                // We only allow the explicitly requested namespace
                System.out.println("ingestNamespaceOntology: Scanning for unwanted auto-created namespaces...");
                java.util.List<NameSpace> allNamespaces = NameSpace.find();
                java.util.List<String> unwantedNamespaces = new java.util.ArrayList<>();
                
                for (NameSpace ns : allNamespaces) {
                    // Check if this namespace points to our graph URI but has wrong abbreviation
                    if (ns.getUri().equals(decodedUri) && !ns.getLabel().equals(decodedAbbrev)) {
                        System.out.println("[WARNING] Found unwanted namespace: '" + ns.getLabel() + "' -> " + ns.getUri());
                        System.out.println("[WARNING] This was auto-created from TTL metadata (rdfs:label)");
                        unwantedNamespaces.add(ns.getLabel());
                        
                        // Delete from cache
                        NameSpaces.getInstance().getNamespaces().remove(ns.getLabel());
                        NameSpaces.getInstance().getNamespacesByUri().remove(ns.getUri(), ns);
                        
                        // Delete from triplestore
                        ns.delete();
                        System.out.println("[INFO] Deleted unwanted namespace: " + ns.getLabel());
                    }
                }
                
                if (!unwantedNamespaces.isEmpty()) {
                    System.out.println("[ERROR] POLICY VIOLATION: hascoapi auto-created namespaces from TTL content!");
                    System.out.println("[ERROR] Deleted unwanted namespaces: " + String.join(", ", unwantedNamespaces));
                    System.out.println("[ERROR] ROOT CAUSE: hascoapi must be fixed to NOT derive namespaces from owl:Ontology + rdfs:label");
                }

                // Update triple count to reflect loaded data
                namespace.setNumberOfLoadedTriples();
                namespace.save();
                
                System.out.println("[INFO] Namespace ontology ingestion complete for: " + namespace.getLabel() + " (" + decodedUri + ")");
                System.out.println("ingestNamespaceOntology: Triple count updated: " + namespace.getNumberOfLoadedTriples() + " triples");
                System.out.println("ingestNamespaceOntology: Named graph: " + fileUri);

                // Clean up the temp file after ingestion
                try {
                    fileToIngest.delete();
                    System.out.println("ingestNamespaceOntology: Cleaned up temp file: " + fileToIngest.getAbsolutePath());
                } catch (Exception e) {
                    System.err.println("[WARNING] Failed to delete temp file: " + e.getMessage());
                }

            } catch (Exception e) {
                System.err.println("[ERROR] Failed during namespace ontology ingestion: " + e.getMessage());
                e.printStackTrace();
            }
        });

        return ok(ApiUtil.createResponse("Ontology upload and ingestion in progress for namespace: " + namespace.getLabel(), true));
    }

    public Result loadOntologies(){
        String kb = ConfigFactory.load().getString("hascoapi.repository.triplestore");
        CompletableFuture<Long> completableFuture = CompletableFuture.supplyAsync(() -> manageTriples("load", kb));
        //while (!completableFuture.isDone()) {
        //    System.out.println("CompletableFuture is not finished yet...");
        //}
        //long result = completableFuture.get();
        return ok(ApiUtil.createResponse("Repository's ontologies has been requested to be LOADED.", true));
    }

    public Result deleteOntologies(){
        String kb = ConfigFactory.load().getString("hascoapi.repository.triplestore");
        CompletableFuture<Long> completableFuture = CompletableFuture.supplyAsync(() -> manageTriples("delete", kb));
        //while (!completableFuture.isDone()) {
        //    System.out.println("CompletableFuture is not finished yet...");
        //}
        //long result = completableFuture.get();
        return ok(ApiUtil.createResponse("Repository's ontologies have been requested to be DELETED.", true));
    }

    public Result getLanguages() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            // get the list of variables in that study
            // serialize the Study object first as ObjectNode
            //   as JsonNode is immutable and meant to be read-only
            //List<Table> table = Table.findLanguage();
            //for (Table entry: table) {
            //    System.out.println(entry.getCode());
            // }
            //System.out.println("inside getLanguages");
            ArrayNode array = mapper.convertValue(Table.findLanguage(), ArrayNode.class);
            JsonNode jsonObject = mapper.convertValue(array, JsonNode.class);
            //System.out.println("inside getLanguages [" + jsonObject + "]");
            return ok(ApiUtil.createResponse(jsonObject, true));
        } catch (Exception e) {
            e.printStackTrace();
            return badRequest(ApiUtil.createResponse("Error retrieving languages", false));
        }
    }

    public Result getGenerationActivities() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            // get the list of variables in that study
            // serialize the Study object first as ObjectNode
            //   as JsonNode is immutable and meant to be read-only
            //List<Table> table = Table.find();
            //for (Table entry: table) {
            //    System.out.println(entry.getCode());
            //}
            ArrayNode array = mapper.convertValue(Table.findGenerationActivity(), ArrayNode.class);
            JsonNode jsonObject = mapper.convertValue(array, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        } catch (Exception e) {
            e.printStackTrace();
            return badRequest(ApiUtil.createResponse("Error retrieving generation activities", false));
        }
    }

    public Result getInformants() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            // get the list of variables in that study
            // serialize the Study object first as ObjectNode
            //   as JsonNode is immutable and meant to be read-only
            //List<Table> table = Table.find();
            //for (Table entry: table) {
            //    System.out.println(entry.getCode());
            //}
            ArrayNode array = mapper.convertValue(Table.findInformant(), ArrayNode.class);
            JsonNode jsonObject = mapper.convertValue(array, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        } catch (Exception e) {
            e.printStackTrace();
            return badRequest(ApiUtil.createResponse("Error retrieving informants", false));
        }
    }

    public Result getNamespaces() {
        ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL, HASCO.HASCO_CLASS);
        //ObjectMapper mapper = new ObjectMapper();
        try {
            ArrayNode array = mapper.convertValue(NameSpaces.getInstance().getOrderedNamespacesAsList(), ArrayNode.class);
            JsonNode jsonObject = mapper.convertValue(array, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        } catch (Exception e) {
            e.printStackTrace();
            return badRequest(ApiUtil.createResponse("Error retrieving namespaces", false));
        }
    }

    public Result getTopClasses(String uri) {
        System.out.println("RepoPage.getTopClasses:uri=[" + uri + "]");
        if (uri == null || uri.isEmpty()) {
            return ok(ApiUtil.createResponse("No Namespace's uri has been provided.", false));
        }
        NameSpace nameSpace = NameSpaces.getInstance().getNamespacesByUri().get(uri);
        if (nameSpace == null) {
            nameSpace = NameSpace.find(uri);
        }

        if (nameSpace == null) {
            return ok(ApiUtil.createResponse("Could not retreive any namespace with the uri that has been provided.", false));
        }
 
        List<HADatAcClass> topClasses = nameSpace.getTopclasses();
        if (topClasses == null) {
            topClasses = new ArrayList<HADatAcClass>();
        }
        ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL,HASCO.HASCO_CLASS);
        JsonNode jsonObject = mapper.convertValue(topClasses, JsonNode.class);
        return ok(ApiUtil.createResponse(jsonObject, true));
    }

    public Result getInstrumentPositions() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            ArrayNode array = mapper.convertValue(Table.findInstrumentPosition(), ArrayNode.class);
            JsonNode jsonObject = mapper.convertValue(array, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        } catch (Exception e) {
            e.printStackTrace();
            return badRequest(ApiUtil.createResponse("Error retrieving Instrument Positions", false));
        }
    }

    public Result getSubcontainerPositions() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            ArrayNode array = mapper.convertValue(Table.findSubcontainerPosition(), ArrayNode.class);
            JsonNode jsonObject = mapper.convertValue(array, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        } catch (Exception e) {
            e.printStackTrace();
            return badRequest(ApiUtil.createResponse("Error retrieving Subcontainer Positions", false));
        }
    }

    /**
     * Get all ontology URIs from the triplestore
     * Queries for all resources of type owl:Ontology
     */
    public Result getOntologies() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT DISTINCT ?ontology WHERE { \n" +
                "  ?ontology a <http://www.w3.org/2002/07/owl#Ontology> . \n" +
                "} ORDER BY ?ontology";

            ResultSetRewindable results = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), 
                queryString);

            ArrayNode ontologies = mapper.createArrayNode();
            while (results.hasNext()) {
                QuerySolution soln = results.next();
                if (soln != null && soln.getResource("ontology") != null) {
                    String ontologyUri = soln.getResource("ontology").getURI();
                    ObjectNode ontology = mapper.createObjectNode();
                    ontology.put("uri", ontologyUri);
                    ontologies.add(ontology);
                }
            }

            return ok(ApiUtil.createResponse(ontologies, true));
        } catch (Exception e) {
            e.printStackTrace();
            return badRequest(ApiUtil.createResponse("Error retrieving ontologies: " + e.getMessage(), false));
        }
    }

}
