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
     * Accepts a namespace URI as parameter and TTL content as request body.
     * 
     * @param request HTTP request containing TTL file as raw body
     * @param namespaceUri URL-encoded namespace URI (e.g., http://pmsr.net/ont/pmsr)
     * @return Result indicating success or failure
     */
    @BodyParser.Of(BodyParser.Raw.class)
    public Result ingestNamespaceOntology(Http.Request request, String namespaceUri) {
        File tempFile = request.body().asRaw().asFile();
        if (tempFile == null) {
            return ok(ApiUtil.createResponse(
                "[ERROR] RepoPage.ingestNamespaceOntology(): No file has been provided for ingestion.", false
            ));
        }

        // Decode the namespace URI
        String decodedUri = java.net.URLDecoder.decode(namespaceUri, java.nio.charset.StandardCharsets.UTF_8);
        System.out.println("ingestNamespaceOntology: namespaceUri=[" + decodedUri + "]");
        System.out.println("ingestNamespaceOntology: File size: " + tempFile.length() + " bytes");

        // Derive label from URI for lookup/registration
        String derivedLabel = deriveNamespaceLabel(decodedUri);
        
        // Find or create the namespace (must be final for lambda)
        final NameSpace namespace;
        NameSpace existingNamespace = NameSpaces.getInstance().getNamespacesByUri().get(decodedUri);
        
        if (existingNamespace == null) {
            // Check if a namespace with this label already exists (might have wrong URI)
            NameSpace existingByLabel = NameSpaces.getInstance().getNamespaces().get(derivedLabel);
            
            if (existingByLabel != null) {
                // Found namespace by label but different URI - update it
                System.out.println("ingestNamespaceOntology: Found existing namespace by label '" + derivedLabel + 
                    "' with different URI (old: " + existingByLabel.getUri() + ", new: " + decodedUri + ") - updating");
                
                // Update the URI and other properties
                existingByLabel.setUri(decodedUri);
                existingByLabel.setSourceMime("text/turtle");
                existingByLabel.setSource(""); // No remote source, manual upload
                existingByLabel.setComment("Updated via namespace ontology ingestion");
                
                try {
                    existingByLabel.save();
                    System.out.println("ingestNamespaceOntology: Successfully updated namespace: " + derivedLabel);
                } catch (Exception e) {
                    System.err.println("[ERROR] Failed to update namespace: " + e.getMessage());
                    e.printStackTrace();
                    return badRequest(ApiUtil.createResponse(
                        "[ERROR] Failed to update namespace: " + e.getMessage(), false
                    ));
                }
                
                namespace = existingByLabel;
            } else {
                // No existing namespace - create new one
                System.out.println("ingestNamespaceOntology: Namespace not found, auto-registering: " + decodedUri);
                
                // Auto-register namespace following NameSpaceGenerator protocol
                NameSpace newNamespace = new NameSpace();
                newNamespace.setNamedGraph(Constants.DEFAULT_REPOSITORY);
                newNamespace.setLabel(derivedLabel);
                newNamespace.setUri(decodedUri);
                newNamespace.setTypeUri(HASCO.ONTOLOGY);
                newNamespace.setHascoTypeUri(HASCO.ONTOLOGY);
                newNamespace.setSourceMime("text/turtle");
                newNamespace.setSource(""); // No remote source, manual upload
                newNamespace.setComment("Auto-registered via namespace ontology ingestion");
                newNamespace.setPriority(100);
                newNamespace.setPermanent(false);
                
                // Add to in-memory cache BEFORE saving (required by NameSpaceGenerator protocol)
                NameSpaces.getInstance().addNamespace(newNamespace);
                
                // Save to triplestore
                try {
                    newNamespace.save();
                    System.out.println("ingestNamespaceOntology: Successfully registered namespace: " + derivedLabel);
                } catch (Exception e) {
                    System.err.println("[ERROR] Failed to register namespace: " + e.getMessage());
                    e.printStackTrace();
                    return badRequest(ApiUtil.createResponse(
                        "[ERROR] Failed to register namespace: " + e.getMessage(), false
                    ));
                }
                
                namespace = newNamespace;
            }
        } else {
            // Found by URI - update MIME type to ensure it's correct
            System.out.println("ingestNamespaceOntology: Found existing namespace by URI: " + decodedUri);
            existingNamespace.setSourceMime("text/turtle");
            try {
                existingNamespace.save();
            } catch (Exception e) {
                System.err.println("[ERROR] Failed to update namespace MIME type: " + e.getMessage());
            }
            namespace = existingNamespace;
        }

        // Ensure source MIME type is correct
        namespace.setSourceMime("text/turtle");

        // Copy temp file to a permanent location before async processing
        // (Play Framework cleans up temp files after request completes)
        File permanentFile = null;
        try {
            permanentFile = File.createTempFile("ontology_ingest_", ".ttl");
            permanentFile.deleteOnExit();
            java.nio.file.Files.copy(tempFile.toPath(), permanentFile.toPath(), 
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            System.out.println("ingestNamespaceOntology: Copied temp file to: " + permanentFile.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to copy temp file: " + e.getMessage());
            e.printStackTrace();
            return badRequest(ApiUtil.createResponse(
                "[ERROR] Failed to prepare file for ingestion: " + e.getMessage(), false
            ));
        }

        final File fileToIngest = permanentFile;

        // Run asynchronously to avoid blocking the HTTP thread
        CompletableFuture.runAsync(() -> {
            try {
                System.out.println("ingestNamespaceOntology: Starting ingestion for namespace: " + namespace.getLabel());

                // Remove existing triples from the same named graph
                namespace.deleteTriples();
                System.out.println("ingestNamespaceOntology: Deleted existing triples for namespace: " + namespace.getLabel());

                // Load triples from the uploaded file directly (no permanent storage needed for external ontologies)
                namespace.loadTriples(fileToIngest.getAbsolutePath(), false);

                // Update triple count to reflect loaded data
                namespace.setNumberOfLoadedTriples();
                namespace.save();
                
                System.out.println("[INFO] Namespace ontology ingestion complete for: " + namespace.getLabel() + " (" + decodedUri + ")");
                System.out.println("ingestNamespaceOntology: Triple count updated: " + namespace.getNumberOfLoadedTriples() + " triples");

                // Clean up the permanent temp file after ingestion
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

    /**
     * Derive a namespace label from its URI.
     * Extracts the last meaningful segment from the URI.
     * Examples:
     *   http://pmsr.net/ont/pmsr -> pmsr
     *   http://purl.obolibrary.org/obo/uberon.owl -> uberon
     *   http://purl.obolibrary.org/obo/ncit.owl -> ncit
     */
    private String deriveNamespaceLabel(String uri) {
        if (uri == null || uri.isEmpty()) {
            return "unknown";
        }
        
        // Remove trailing slashes and anchors
        String cleaned = uri.replaceAll("[/#]+$", "");
        
        // Get last segment
        String[] segments = cleaned.split("[/#]");
        String lastSegment = segments[segments.length - 1];
        
        // Remove .owl, .ttl, .rdf extensions
        lastSegment = lastSegment.replaceAll("\\.(owl|ttl|rdf)$", "");
        
        // Convert to lowercase for consistency with NameSpaceGenerator requirements
        return lastSegment.toLowerCase();
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
