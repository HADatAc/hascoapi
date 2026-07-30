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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class RepoPage extends Controller {

    private static final String NS_APPROVAL_REQUIRED = "hascoapi.namespace.approval.required";
    private static final String NS_APPROVAL_TOKEN = "hascoapi.namespace.approval.token";
    private static final String NS_APPROVAL_HEADER = "X-Namespace-Approval";
    private static final String NS_CHANGE_ID_HEADER = "X-Change-Id";
    private static final String NS_COMPONENT_HEADER = "X-Namespace-Component";
    private static final Set<String> ALLOWED_NS_COMPONENTS = new HashSet<>(Arrays.asList(
        "pmsr-config-bootstrap",
        "pmsr-ingest-ontologies"
    ));

    /**
     * Blocks namespace mutations unless an explicit approval token is provided.
     * Returns null when approved, otherwise a Result explaining the block.
     */
    private Result requireNamespaceApproval(String action, Http.Request req) {
        boolean required = true;
        try {
            if (ConfigFactory.load().hasPath(NS_APPROVAL_REQUIRED)) {
                required = ConfigFactory.load().getBoolean(NS_APPROVAL_REQUIRED);
            }
        } catch (Exception e) {
            required = true;
        }

        if (!required) {
            return null;
        }

        String expectedToken = "";
        try {
            if (ConfigFactory.load().hasPath(NS_APPROVAL_TOKEN)) {
                expectedToken = ConfigFactory.load().getString(NS_APPROVAL_TOKEN);
            }
        } catch (Exception e) {
            expectedToken = "";
        }

        if (expectedToken == null || expectedToken.trim().isEmpty()) {
            System.err.println("[SECURITY] Namespace mutation blocked: approval token is not configured. action=" + action);
            return forbidden(ApiUtil.createResponse(
                "Namespace mutation blocked: approval token is not configured.", false
            ));
        }

        String providedToken = req.getHeaders().get(NS_APPROVAL_HEADER).orElse("");
        String changeId = req.getHeaders().get(NS_CHANGE_ID_HEADER).orElse("");

        if (providedToken.trim().isEmpty() || changeId.trim().isEmpty()) {
            System.err.println("[SECURITY] Namespace mutation blocked: missing approval headers. action=" + action);
            return forbidden(ApiUtil.createResponse(
                "Namespace mutation blocked: missing required approval headers ("
                    + NS_APPROVAL_HEADER + ", " + NS_CHANGE_ID_HEADER + ").",
                false
            ));
        }

        if (!secureEquals(expectedToken, providedToken)) {
            System.err.println("[SECURITY] Namespace mutation blocked: invalid approval token. action=" + action + ", changeId=" + changeId);
            return forbidden(ApiUtil.createResponse(
                "Namespace mutation blocked: invalid approval token.", false
            ));
        }

        String component = req.getHeaders().get(NS_COMPONENT_HEADER).orElse("").trim().toLowerCase();
        if (component.isEmpty() || !ALLOWED_NS_COMPONENTS.contains(component)) {
            System.err.println("[SECURITY] Namespace mutation blocked: unauthorized component. action="
                + action + ", component=" + component + ", changeId=" + changeId);
            return forbidden(ApiUtil.createResponse(
                "Namespace mutation blocked: unauthorized component. Allowed components are pmsr-config-bootstrap and pmsr-ingest-ontologies.",
                false
            ));
        }

        return null;
    }

    private boolean secureEquals(String a, String b) {
        byte[] left = a.getBytes(StandardCharsets.UTF_8);
        byte[] right = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(left, right);
    }

    private void auditNamespaceMutation(String action, Http.Request req, String details) {
        String changeId = req.getHeaders().get(NS_CHANGE_ID_HEADER).orElse("-");
        String component = req.getHeaders().get(NS_COMPONENT_HEADER).orElse("-");
        System.out.println("[AUDIT] namespaceMutation action=" + action
            + " component=" + component + " changeId=" + changeId + " details=" + details);
    }

    private String validateNamespaceLabel(String label) {
        if (label == null || label.trim().isEmpty()) {
            return "label is required";
        }
        String normalized = label.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z][a-z0-9-]*")) {
            return "label must match [a-z][a-z0-9-]*";
        }
        if (normalized.startsWith("ns_http___")) {
            return "label pattern ns_http___* is blocked by policy";
        }
        return null;
    }

    private String validateNamespaceUri(String uri) {
        if (uri == null || uri.trim().isEmpty()) {
            return "uri is required";
        }
        String normalized = URIUtils.normalizeNamespaceBase(uri.trim());
        if (!(normalized.startsWith("http://") || normalized.startsWith("https://"))) {
            return "uri must start with http:// or https://";
        }
        if (!(normalized.endsWith("/") || normalized.endsWith("#") || normalized.endsWith("_"))) {
            return "uri must end with '/', '#', or '_'";
        }
        return null;
    }

    private String validateNamespacePair(String label, String uri, String currentLabel) {
        String labelError = validateNamespaceLabel(label);
        if (labelError != null) {
            return labelError;
        }
        String uriError = validateNamespaceUri(uri);
        if (uriError != null) {
            return uriError;
        }

        String normalizedLabel = label.trim().toLowerCase(Locale.ROOT);
        String normalizedUri = URIUtils.normalizeNamespaceBase(uri.trim());

        NameSpace sameLabel = NameSpaces.getInstance().getNamespaces().get(normalizedLabel);
        if (sameLabel != null && (currentLabel == null || !sameLabel.getLabel().equals(currentLabel))) {
            return "label already exists";
        }

        NameSpace sameUri = NameSpaces.getInstance().getNamespacesByUri().get(normalizedUri);
        if (sameUri != null && (currentLabel == null || !sameUri.getLabel().equals(currentLabel))) {
            return "uri already exists for label '" + sameUri.getLabel() + "'";
        }

        return null;
    }

    private Result blockNamespaceMutation(String action) {
        System.err.println("[SECURITY] Namespace mutation endpoint is disabled. action=" + action);
        return forbidden(ApiUtil.createResponse(
            "Namespace mutation is disabled for this endpoint. Only ingestNamespaceOntology is currently allowed.", false
        ));
    }

    private Result blockOntologyMutation(String action) {
        System.err.println("[SECURITY] Ontology mutation endpoint is disabled. action=" + action);
        return forbidden(ApiUtil.createResponse(
            "Ontology mutation is disabled for this endpoint. Manage Ontologies is read-only.", false
        ));
    }

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

    public Result updateDefaultNamespace(Http.Request request, String prefix, String url, String sourceMime, String source) {
        Result approval = requireNamespaceApproval("updateDefaultNamespace", request);
        if (approval != null) {
            return approval;
        }

        if (prefix == null || prefix.trim().isEmpty() || url == null || url.trim().isEmpty()) {
            return badRequest(ApiUtil.createResponse(
                "Default namespace update blocked: prefix and url are required.", false
            ));
        }

        String safeMime = (sourceMime == null || "_".equals(sourceMime)) ? "" : sourceMime;
        String safeSource = (source == null || "_".equals(source)) ? "" : source;

        RepositoryInstance.getInstance().setHasDefaultNamespacePrefix(prefix);
        RepositoryInstance.getInstance().setHasDefaultNamespaceURL(url);
        RepositoryInstance.getInstance().setHasDefaultNamespaceSourceMime(safeMime);
        RepositoryInstance.getInstance().setHasDefaultNamespaceSource(safeSource);
        RepositoryInstance.getInstance().save();

        return ok(ApiUtil.createResponse("Repository default namespace has been UPDATED.", true));
    }

    public Result updateNamespace(Http.Request request, String abbreviation, String url){
        Result approval = requireNamespaceApproval("updateNamespace", request);
        if (approval != null) {
            return approval;
        }

        if (abbreviation == null || abbreviation.trim().isEmpty() || url == null || url.trim().isEmpty()) {
            return badRequest(ApiUtil.createResponse("Namespace update blocked: abbreviation and url are required.", false));
        }

        NameSpace ns = NameSpaces.getInstance().getNamespaces().get(abbreviation);
        if (ns == null) {
            return badRequest(ApiUtil.createResponse("Could not find namespace with abbreviation [" + abbreviation + "]", false));
        }

        String pairError = validateNamespacePair(ns.getLabel(), url, ns.getLabel());
        if (pairError != null) {
            return badRequest(ApiUtil.createResponse("Namespace update blocked: " + pairError + ".", false));
        }

        String oldUri = ns.getUri();

        ns.setUri(url);
        ns.setNamedGraph(url);
        ns.save();
        NameSpaces.getInstance().resetNameSpaces();
        auditNamespaceMutation("updateNamespace", request,
            "label=" + ns.getLabel() + " oldUri=" + oldUri + " newUri=" + ns.getUri());
        return ok(ApiUtil.createResponse("Namespace [" + abbreviation + "] has been UPDATED.", true));
    }

    public Result createNamespace(Http.Request request, String json){
        Result approval = requireNamespaceApproval("createNamespace", request);
        if (approval != null) {
            return approval;
        }

        if (json == null || json.trim().isEmpty()) {
            return badRequest(ApiUtil.createResponse("No namespace JSON payload has been provided.", false));
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(json);

            String label = node.hasNonNull("label") ? node.get("label").asText() : "";
            String uri = node.hasNonNull("uri") ? node.get("uri").asText() : "";
            String source = node.hasNonNull("source") ? node.get("source").asText() : "";
            String sourceMime = node.hasNonNull("sourceMime") ? node.get("sourceMime").asText() : "";

            label = label.trim().toLowerCase(Locale.ROOT);
            uri = URIUtils.normalizeNamespaceBase(uri.trim());

            if (label.trim().isEmpty() || uri.trim().isEmpty()) {
                return badRequest(ApiUtil.createResponse("Namespace creation blocked: label and uri are required.", false));
            }

            String pairError = validateNamespacePair(label, uri, null);
            if (pairError != null) {
                return badRequest(ApiUtil.createResponse("Namespace creation blocked: " + pairError + ".", false));
            }

            if (NameSpaces.getInstance().getNamespaces().containsKey(label)) {
                return badRequest(ApiUtil.createResponse("Namespace [" + label + "] already exists.", false));
            }

            NameSpace ns = new NameSpace();
            ns.setLabel(label);
            ns.setUri(uri);
            ns.setNamedGraph(uri);
            ns.setTypeUri(HASCO.ONTOLOGY);
            ns.setHascoTypeUri(HASCO.ONTOLOGY);
            ns.setSource(source);
            ns.setSourceMime(sourceMime);
            ns.setPriority(100);
            ns.setPermanent(false);

            NameSpaces.getInstance().addNamespace(ns);
            ns.save();
            NameSpaces.getInstance().resetNameSpaces();
            auditNamespaceMutation("createNamespace", request,
                "label=" + label + " uri=" + uri + " sourceMime=" + sourceMime);
            return ok(ApiUtil.createResponse("Namespace [" + label + "] has been CREATED.", true));
        } catch (Exception e) {
            e.printStackTrace();
            return badRequest(ApiUtil.createResponse("Failed to create namespace: " + e.getMessage(), false));
        }
    }

    public Result resetNamespaces(Http.Request request){
        Result approval = requireNamespaceApproval("resetNamespaces", request);
        if (approval != null) {
            return approval;
        }
        NameSpaces.getInstance().resetNameSpaces();
        auditNamespaceMutation("resetNamespaces", request, "cacheReset=true");
        return ok(ApiUtil.createResponse("Namespace cache has been RESET.", true));
    }

    public Result deleteSelectedNamespace(Http.Request request, String abbreviation){
        Result approval = requireNamespaceApproval("deleteSelectedNamespace", request);
        if (approval != null) {
            return approval;
        }

        if (abbreviation == null || abbreviation.trim().isEmpty()) {
            return badRequest(ApiUtil.createResponse("No namespace abbreviation has been provided.", false));
        }

        String response = NameSpace.deleteNamespace(abbreviation);
        if (!response.isEmpty()) {
            return badRequest(ApiUtil.createResponse(response, false));
        }
        NameSpaces.getInstance().resetNameSpaces();
        auditNamespaceMutation("deleteSelectedNamespace", request, "label=" + abbreviation);
        return ok(ApiUtil.createResponse("Namespace [" + abbreviation + "] has been DELETED.", true));
    }

    public Result deleteNamespace(Http.Request request){
        Result approval = requireNamespaceApproval("deleteNamespace", request);
        if (approval != null) {
            return approval;
        }

        NameSpace.deleteAll();
        NameSpaces.getInstance().resetNameSpaces();
        auditNamespaceMutation("deleteNamespace", request, "scope=all");
        return ok(ApiUtil.createResponse("Namespace table delete has been requested.", true));
    }

    private Long manageTriples(String oper, String kb) {
        LoadOnt.playLoadOntologiesAsync(oper, kb);
        return 0L;
    }
    
    /**
     * Handles application ontology upload, save permanently, delete old triples, and ingest new ones.
     */
    public Result ingestAppOntology(Http.Request request) {
        return blockOntologyMutation("ingestAppOntology");
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
        Result approval = requireNamespaceApproval("ingestNamespaceOntology", request);
        if (approval != null) {
            return approval;
        }
        final String auditChangeId = request.getHeaders().get(NS_CHANGE_ID_HEADER).orElse("-");
        final String auditComponent = request.getHeaders().get(NS_COMPONENT_HEADER).orElse("-");
        File tempFile = request.body().asRaw().asFile();
        if (tempFile == null) {
            return ok(ApiUtil.createResponse(
                "[ERROR] RepoPage.ingestNamespaceOntology(): No file has been provided for ingestion.", false
            ));
        }

        // Decode and normalize namespace URI.
        final String decodedUri = URIUtils.normalizeNamespaceBase(
            java.net.URLDecoder.decode(namespaceUri, java.nio.charset.StandardCharsets.UTF_8)
        );
        final String decodedAbbrev = java.net.URLDecoder.decode(abbreviation, java.nio.charset.StandardCharsets.UTF_8).trim().toLowerCase(Locale.ROOT);

        String pairError = validateNamespacePair(decodedAbbrev, decodedUri, decodedAbbrev);
        if (pairError != null && !pairError.equals("label already exists")) {
            return badRequest(ApiUtil.createResponse(
                "Namespace ontology ingestion blocked: " + pairError + ".", false
            ));
        }
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
        
        // Create file:// URI for local source tracking.
        final String fileUri = workingFile.toURI().toString();
        System.out.println("ingestNamespaceOntology: Working file path: " + workingFile.getAbsolutePath());
        System.out.println("ingestNamespaceOntology: File URI (source): " + fileUri);
        System.out.println("ingestNamespaceOntology: Target named graph: " + decodedUri);
        
        final File fileToIngest = workingFile;
        
        // Find or auto-register the namespace
        final NameSpace namespace;
        NameSpace existingByLabel = NameSpaces.getInstance().getNamespaces().get(decodedAbbrev);
        NameSpace existingByUri = NameSpaces.getInstance().getNamespacesByUri().get(decodedUri);
        
        if (existingByLabel != null) {
            // REQUIRED behavior: if abbreviation already exists (e.g., pmsr), treat as update-in-place.
            // Keep the exact label and ingest into this namespace graph.
            namespace = existingByLabel;
            System.out.println("ingestNamespaceOntology: Found existing namespace by label: " + namespace.getLabel());
            if (!decodedUri.equals(existingByLabel.getUri())) {
                System.out.println("[WARNING] Requested URI differs from existing namespace URI. Keeping existing URI for label '" + namespace.getLabel() + "'.");
                System.out.println("[WARNING] Existing URI: " + existingByLabel.getUri() + " | Requested URI: " + decodedUri);
            }
            System.out.println("ingestNamespaceOntology: Will update existing namespace with new file URI and MIME");

        } else if (existingByUri != null) {
            // URI exists but with a different label. Replace it so requested abbreviation is preserved exactly.
            String conflictingLabel = existingByUri.getLabel();
            System.out.println("[WARNING] URI already exists with conflicting abbreviation: " + conflictingLabel +
                ". Replacing with requested abbreviation: " + decodedAbbrev);

            String deleteResponse = NameSpace.deleteNamespace(conflictingLabel);
            if (!deleteResponse.isEmpty()) {
                System.err.println("[ERROR] Failed to delete conflicting namespace: " + deleteResponse);
                return badRequest(ApiUtil.createResponse(
                    "[ERROR] Cannot replace conflicting namespace '" + conflictingLabel + "': " + deleteResponse, false
                ));
            }

            NameSpaces.getInstance().resetNameSpaces();

            NameSpace newNamespace = new NameSpace();
            newNamespace.setNamedGraph(decodedUri);
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
                System.out.println("ingestNamespaceOntology: Successfully replaced conflicting namespace with: " + decodedAbbrev);
            } catch (Exception e) {
                System.err.println("[ERROR] Failed to create replacement namespace: " + e.getMessage());
                e.printStackTrace();
                NameSpaces.getInstance().getNamespaces().remove(decodedAbbrev);
                NameSpaces.getInstance().getNamespacesByUri().remove(decodedUri);
                return badRequest(ApiUtil.createResponse(
                    "[ERROR] Failed to create replacement namespace: " + e.getMessage(), false
                ));
            }

            namespace = newNamespace;

        } else {
            // Namespace doesn't exist - auto-register it with the provided abbreviation (NOT derived)
            System.out.println("ingestNamespaceOntology: Auto-registering new namespace: " + decodedAbbrev + " -> " + decodedUri);
            
            NameSpace newNamespace = new NameSpace();
            newNamespace.setNamedGraph(decodedUri);
            newNamespace.setLabel(decodedAbbrev);
            newNamespace.setUri(decodedUri);
            newNamespace.setTypeUri(HASCO.ONTOLOGY);
            newNamespace.setHascoTypeUri(HASCO.ONTOLOGY);
            newNamespace.setSourceMime(sourceMime);  // Set based on file extension
            newNamespace.setSource(fileUri);
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
        namespace.setNamedGraph(namespace.getUri());
        
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
                    if (ns.getUri().equals(namespace.getUri()) && !ns.getLabel().equals(decodedAbbrev)) {
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
                System.out.println("ingestNamespaceOntology: Named graph: " + namespace.getUri());
                System.out.println("[AUDIT] namespaceMutation action=ingestNamespaceOntology component="
                    + auditComponent + " changeId=" + auditChangeId + " details=label=" + namespace.getLabel()
                    + " uri=" + namespace.getUri() + " triples=" + namespace.getNumberOfLoadedTriples());

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
        manageTriples("load", "ontology");
        return ok(ApiUtil.createResponse("Ontology loading process has started.", true));
    }

    public Result deleteOntologies(){
        return blockOntologyMutation("deleteOntologies");
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
