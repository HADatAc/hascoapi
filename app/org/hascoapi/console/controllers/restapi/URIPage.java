package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.hascoapi.Constants;
import org.hascoapi.RepositoryInstance;
import org.hascoapi.entity.pojo.*;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.HAScOMapper;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.Utils;
import org.hascoapi.vocabularies.FOAF;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.OWL;
import org.hascoapi.vocabularies.SCHEMA;
import org.hascoapi.vocabularies.SIO;
import org.hascoapi.vocabularies.VSTOI;
import org.apache.jena.query.ResultSetRewindable;
import play.mvc.Controller;
import play.mvc.Result;

public class URIPage extends Controller {

    private static final long MISSING_URI_WARNING_WINDOW_MS = 60_000L;
    private static final Map<String, Long> missingUriWarningLastSeen = new ConcurrentHashMap<>();

    private Result apiError(int statusCode, String code, String message) {
        ObjectNode response = new ObjectMapper().createObjectNode();
        response.put("isSuccessful", false);
        response.put("code", code);
        response.put("body", message == null ? "" : message);
        return status(statusCode, response);
    }

    public Result getUri(String uri) {

        //System.out.println("URIPage.getUri() with uri [" + uri + "]");
        if (!uri.startsWith("http://") && !uri.startsWith("https://")) {
            return ok(ApiUtil.createResponse("[" + uri + "] is an invalid URI", false));
        }

        // Determine upfront whether this URI belongs to a Task family element.
        // This pre-resolution is advisory only; keep endpoint resilient on
        // transient triplestore read failures (for example EOF on HTTP stream).
        GenericInstance preResolved = null;
        try {
            preResolved = findGenericInstanceWithRetry(uri);
        } catch (Throwable ignored) {
            preResolved = null;
        }
        boolean isTaskUri = false;
        if (preResolved != null) {
            String preHascoType = preResolved.getHascoTypeUri() == null ? "" : preResolved.getHascoTypeUri();
            String preType = preResolved.getTypeUri() == null ? "" : preResolved.getTypeUri();
            isTaskUri = isTaskType(preHascoType, preType);
        }

        // Handle URI sets (multiple URIs separated by semicolons)
        // Example: "https://example.org/TSK1;https://example.org/TSK2;https://example.org/TSK3"
        // This is common in WKF where a Process may have multiple Tasks
        if (uri.contains(";")) {
            System.out.println("[INFO] URIPage.getUri(): Detected URI set with semicolons, splitting...");
            String[] uris = uri.split(";");
            if (uris.length > 0) {
                String firstUri = uris[0].trim();
                System.out.println("[INFO] URIPage.getUri(): Using first URI from set: " + firstUri);
                System.out.println("[INFO] URIPage.getUri(): Total URIs in set: " + uris.length);
                uri = firstUri; // Use the first URI for the query
            }
        }

        HADatAcThing finalResult = URIPage.objectFromUri(uri);
        if (finalResult == null) {
            if (isTaskUri) {
                return apiError(404, "URI_NOT_FOUND", "No element with URI [" + uri + "] has been found");
            }
            return ok(ApiUtil.createResponse("Uri [" + uri + "] returned no object from the knowledge graph", false));
        }

        String hascoTypeUri = finalResult.getHascoTypeUri();
        if (hascoTypeUri == null || hascoTypeUri.equals("")) {
            String typeUri = finalResult.getTypeUri();
            if (typeUri == null || typeUri.equals("")) {
                if (isTaskUri) {
                    return apiError(404, "TYPE_NOT_RESOLVED", "No type-specific instance found for uri [" + uri + "]");
                }
                return ok(ApiUtil.createResponse("No type-specific instance found for uri [" + uri + "]", false));
            }
        }

        return processResult(finalResult, finalResult.getHascoTypeUri(), uri);

    }

    private static GenericInstance findGenericInstanceWithRetry(String uri) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                return GenericInstance.find(uri);
            } catch (RuntimeException ex) {
                last = ex;
                if (attempt < 2) {
                    try {
                        Thread.sleep(80L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw ex;
                    }
                }
            }
        }
        if (last != null) {
            throw last;
        }
        return null;
    }

    public Result uriGen(String elementType) {
        if (elementType == null) {
            return ok(ApiUtil.createResponse("No elementType has been provided.", false));
        }
        String repoUri = RepositoryInstance.getInstance().getBaseURL();
        if (repoUri == null || repoUri.isEmpty()) {
            return ok(ApiUtil.createResponse("Repository's base URL needs to be setup before URIs can be generated.", false));
        }

        String shortPrefix = Utils.shortPrefix(elementType);
        if (shortPrefix == null) {
            return ok(ApiUtil.createResponse("Cannot generate URI for elementType [" + elementType + "]", false));
        }

        if (!repoUri.endsWith("/")) {
            repoUri += "/";
        }

        String newUri = Utils.uriGen(repoUri, shortPrefix);
        String newUriJSON = "{\"uri\":" + newUri + "}";

        return ok(ApiUtil.createResponse(newUriJSON, true));
    }
    
    private static String getCurrentUserId() {
        // Implement this method to return the current user's ID
        // For example, if using Spring Security, you might do:
        // return SecurityContextHolder.getContext().getAuthentication().getName();
        return "12345";  // Placeholder implementation
    }
    
    private static int convertEmailToNumber(String email) {
        try {
            // Create a MessageDigest instance for MD5
            MessageDigest md = MessageDigest.getInstance("MD5");

            // Digest the email bytes
            byte[] hashBytes = md.digest(email.getBytes(StandardCharsets.UTF_8));

            // Convert the hash bytes to a positive integer
            int hashInt = Math.abs(bytesToInt(hashBytes));

            // Map the integer to a 5-digit number (range 10000 to 99999)
            int fiveDigitNumber = 10000 + (hashInt % 90000);

            return fiveDigitNumber;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }

    private static int bytesToInt(byte[] bytes) {
        int result = 0;
        for (int i = 0; i < 4; i++) {
            result <<= 8;
            result |= (bytes[i] & 0xFF);
        }
        return result;
    }

    public static HADatAcThing objectFromUri(String uri) {
        // System.out.println("URIPage.objectFromUri(): URI [" + uri + "]");
        uri = normalizeLookupUri(uri);
        
        // FIXED: Try to resolve ontology classes (owl:Class) instead of skipping them
        if (isOntologyClass(uri)) {
            // System.out.println("[DEBUG] URIPage.objectFromUri(): Detected ontology class URI [" + uri + "], attempting to resolve as HADatAcClass");
            HADatAcClass classObj = HADatAcClass.find(uri);
            if (classObj != null) {
                // System.out.println("[DEBUG] URIPage.objectFromUri(): Successfully resolved class [" + uri + "] with label [" + classObj.getLabel() + "]");
                return classObj;
            } else {
                System.out.println("[DEBUG] URIPage.objectFromUri(): Could not resolve class [" + uri + "], returning null");
                return null;
            }
        }
        
        String typeUri = "";
        try {

            /*
             * Now uses GenericInstance to process URI against TripleStore content
             */

            Object finalResult = null;
            GenericInstance result = GenericInstance.find(uri);

            if (result == null) {
                HADatAcClass classObj = resolveOntologyClass(uri);
                if (classObj != null) {
                    return classObj;
                }
                if (isPmsrComponentInstanceUri(uri)) {
                    ComponentInstance componentInstance = ComponentInstance.find(uri);
                    if (componentInstance != null) {
                        return componentInstance;
                    }
                }
                NameSpace ns = NameSpaces.getInstance().getNamespacesByUri().get(uri);

                if (ns == null) {
                    if (shouldLogMissingGenericInstanceWarning(uri)) {
                        System.out.println("[WARNING] URIPage.objectFromUri(): No generic instance found for uri [" + uri + "]");
                    }
                    return null;
                }

                return (HADatAcThing)ns;

            }

            String hascoTypeUri = result.getHascoTypeUri() == null ? "" : result.getHascoTypeUri();
            String resultTypeUri = result.getTypeUri() == null ? "" : result.getTypeUri();

            //System.out.println("URIPage.objectFromUri(): HASCO TYPE [" + result.getHascoTypeUri() + "]");

            /*
             * if (result.getHascoTypeUri() == null || result.getHascoTypeUri().isEmpty()) {
             * System.out.println("inside getUri(): typeUri [" + result.getTypeUri() + "]");
             * if (!result.getTypeUri().equals("http://www.w3.org/2002/07/owl#Class")) {
             * return notFound(ApiUtil.createResponse("No valid HASCO type found for uri ["
             * + uri + "]", false));
             * }
             * }
             */

            if (hascoTypeUri.equals(VSTOI.ANNOTATION)) {
                finalResult = Annotation.find(uri);
            } else if (hascoTypeUri.equals(HASCO.ANALYTICAL_TOOL)) {
                finalResult = AnalyticalTool.find(uri);
            } else if (hascoTypeUri.equals(VSTOI.ANNOTATION_STEM)) {
                finalResult = AnnotationStem.find(uri);
            } else if (hascoTypeUri.equals(SIO.ATTRIBUTE)) {
                finalResult = Attribute.find(uri);
            } else if (hascoTypeUri.equals(VSTOI.CODEBOOK)) {
                finalResult = Codebook.find(uri);            
            } else if (hascoTypeUri.equals(VSTOI.CODEBOOK_SLOT)) {
                finalResult = CodebookSlot.find(uri);
            } else if (hascoTypeUri.equals(VSTOI.COMPONENT)) {
                finalResult = Component.find(uri);
            } else if (hascoTypeUri.equals(HASCO.COMPONENT_INSTANCE)) {
                finalResult = ComponentInstance.find(uri);
            } else if (hascoTypeUri.equals(VSTOI.COMPONENT_INSTANCE)) {
                finalResult = ComponentInstance.find(uri);
            } else if (hascoTypeUri.equals(VSTOI.COMPONENT_STEM)) {
                finalResult = ComponentStem.find(uri);
            } else if (hascoTypeUri.equals(VSTOI.CONTAINER_SLOT)) {
                finalResult = ContainerSlot.find(uri);
            } else if (hascoTypeUri.equals(VSTOI.SLOT_ELEMENT)) {
                SlotElement slotElement = SlotOperations.findSlotElement(uri);
                if (slotElement instanceof HADatAcThing) {
                    finalResult = (HADatAcThing) slotElement;
                } else {
                    // Keep abstract SlotElement resolvable even when it has no concrete subtype materialized.
                    finalResult = result;
                }
            } else if (hascoTypeUri.equals(HASCO.DATA_ACQUISITION)) {
                finalResult = DA.find(uri);
            } else if (hascoTypeUri.equals(HASCO.DATAFILE)) {
                finalResult = DataFile.find(uri);
            } else if (hascoTypeUri.equals(HASCO.DD)) {
                finalResult = DD.find(uri);
            } else if (hascoTypeUri.equals(VSTOI.DEPLOYMENT)) {
                finalResult = Deployment.find(uri);
            } else if (hascoTypeUri.equals(HASCO.DP2)) {
                finalResult = DP2.find(uri);
            } else if (hascoTypeUri.equals(HASCO.DSG)) {
                finalResult = DSG.find(uri);
            } else if (hascoTypeUri.equals(SIO.ENTITY)) {
                finalResult = Entity.find(uri);
            } else if (hascoTypeUri.equals(SCHEMA.FUNDING_SCHEME)) {
                finalResult = FundingScheme.find(uri);
            } else if (hascoTypeUri.equals(HASCO.INS)) {
                finalResult = INS.find(uri);
            } else if (hascoTypeUri.equals(VSTOI.INSTRUMENT)) {
                finalResult = Instrument.find(uri);
            } else if (hascoTypeUri.equals(HASCO.INSTRUMENT_INSTANCE)) {
                finalResult = InstrumentInstance.find(uri);
            } else if (hascoTypeUri.equals(VSTOI.INSTRUMENT_INSTANCE)) {
                finalResult = InstrumentInstance.find(uri);
            } else if (hascoTypeUri.equals(HASCO.KGR)) {
                finalResult = KGR.find(uri);
            } else if (hascoTypeUri.equals(HASCO.ONTOLOGY)) {
                finalResult = NameSpaces.getInstance().getNamespaces().get(uri);
            } else if (hascoTypeUri.equals(SCHEMA.ORGANIZATION)) {
                finalResult = Organization.find(uri);
            } else if (hascoTypeUri.equals(SCHEMA.PERSON)) {
                finalResult = Person.find(uri);
            } else if (hascoTypeUri.equals(SCHEMA.PLACE)) {
                finalResult = Place.find(uri);
            } else if (hascoTypeUri.equals(VSTOI.PLATFORM)) {
                finalResult = Platform.find(uri);
            } else if (hascoTypeUri.equals(HASCO.PLATFORM_INSTANCE)) {
                finalResult = PlatformInstance.find(uri);
            } else if (hascoTypeUri.equals(VSTOI.PLATFORM_INSTANCE)) {
                finalResult = PlatformInstance.find(uri);
            } else if (hascoTypeUri.equals(HASCO.POSSIBLE_VALUE)) {
                finalResult = PossibleValue.find(uri);
            } else if (hascoTypeUri.equals(SCHEMA.POSTAL_ADDRESS)) {
                finalResult = PostalAddress.find(uri);
            } else if (hascoTypeUri.equals(VSTOI.PROCESS)) {
                finalResult = org.hascoapi.entity.pojo.Process.find(uri);
            } else if (hascoTypeUri.equals(VSTOI.PROCESS_STEM)) {
                finalResult = ProcessStem.find(uri);
            } else if (hascoTypeUri.equals(SCHEMA.PROJECT)) {
                finalResult = Project.find(uri);
            } else if (hascoTypeUri.equals(VSTOI.REQUIRED_COMPONENT)) {
                finalResult = RequiredComponent.find(uri);
            } else if (hascoTypeUri.equals(VSTOI.REQUIRED_INSTRUMENT)) {
                finalResult = RequiredInstrument.find(uri);
            } else if (hascoTypeUri.equals(VSTOI.RESPONSE_OPTION)) {
                finalResult = ResponseOption.find(uri);
            } else if (hascoTypeUri.equals(HASCO.SDD)) {
                finalResult = SDD.find(uri);
            } else if (hascoTypeUri.equals(HASCO.SDD_ATTRIBUTE)) {
                finalResult = SDDAttribute.find(uri);
            } else if (hascoTypeUri.equals(HASCO.SDD_OBJECT)) {
                finalResult = SDDObject.find(uri);
            } else if (hascoTypeUri.equals(HASCO.SEMANTIC_DATA_DICTIONARY)) {
                finalResult = SemanticDataDictionary.find(uri);
            } else if (hascoTypeUri.equals(HASCO.SEMANTIC_VARIABLE)) {
                finalResult = SemanticVariable.find(uri);
            } else if (hascoTypeUri.equals(HASCO.STR)) {
                finalResult = STR.find(uri);
            } else if (hascoTypeUri.equals(HASCO.STREAM)) {
                finalResult = Stream.find(uri);
            } else if (hascoTypeUri.equals(HASCO.STREAM_TOPIC)) {
                finalResult = StreamTopic.find(uri);
            } else if (hascoTypeUri.equals(HASCO.REPOSITORY)) {
                finalResult = RepositoryInstance.getInstance();
            } else if (hascoTypeUri.equals(HASCO.STUDY)) {
                finalResult = Study.find(uri);
            } else if (hascoTypeUri.equals(HASCO.PROCESS_BASED_STUDY)) {
                finalResult = ProcessBasedStudy.find(uri);
            } else if (hascoTypeUri.equals(HASCO.STUDY_OBJECT)) {
                finalResult = StudyObject.find(uri);
            } else if (hascoTypeUri.equals(HASCO.STUDY_OBJECT_COLLECTION)) {
                finalResult = StudyObjectCollection.find(uri);
            } else if (hascoTypeUri.equals(HASCO.STUDY_ROLE)) {
                finalResult = StudyRole.find(uri);
            } else if (hascoTypeUri.equals(HASCO.WKF)) {
                finalResult = WKF.find(uri);
            } else if (hascoTypeUri.equals(HASCO.WKF_NAMESPACE)) {
                finalResult = WKFNamespace.find(uri);
            } else if (hascoTypeUri.equals(VSTOI.SUBCONTAINER)) {
                finalResult = Subcontainer.find(uri);
            } else if (isTaskType(hascoTypeUri, resultTypeUri)) {
                finalResult = Task.find(uri);
            } else if (hascoTypeUri.equals(SIO.UNIT)) {
                finalResult = Unit.find(uri);
            } else if (hascoTypeUri.equals(HASCO.VIRTUAL_COLUMN)) {
                finalResult = VirtualColumn.find(uri);
            } else if (resultTypeUri.equals(OWL.CLASS)) {
                finalResult = HADatAcClass.find(uri);
            } else {
                finalResult = result;
            }
            return (HADatAcThing) finalResult;

        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve URI from triplestore: " + uri, e);
        }
    }

    private static boolean isPmsrComponentInstanceUri(String uri) {
        if (uri == null) {
            return false;
        }
        String value = uri.trim();
        return value.startsWith("https://pmsr.net/ont/CPI") || value.startsWith("http://pmsr.net/ont/CPI");
    }

    private Result processResult(Object result, String typeResult, String uri) {
        ObjectMapper mapper = HAScOMapper.getFiltered("full",typeResult);

        //System.out.println("[RestAPI] generating JSON for following object: " + uri + " and typeResult: " + typeResult);
        JsonNode jsonObject = null;
        try {
            ObjectNode obj = mapper.convertValue(result, ObjectNode.class);
            jsonObject = mapper.convertValue(obj, JsonNode.class);
            //System.out.println(org.hascoapi.console.controllers.restapi.URIPage.prettyPrintJsonString(jsonObject));
        } catch (Exception e) {
            e.printStackTrace();
            return ok(ApiUtil.createResponse("Error processing the json object for URI [" + uri + "]", false));
        }
        return ok(ApiUtil.createResponse(jsonObject, true));
    }

    public static String prettyPrintJsonString(JsonNode jsonNode) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Object json = mapper.readValue(jsonNode.toString(), Object.class);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Determines whether an instance type should be resolved as Task.
     * Supports exact known VSTOI task types and ontology subclass expansion.
     */
    private static boolean isTaskType(String hascoTypeUri, String rdfTypeUri) {
        if (isKnownTaskType(hascoTypeUri) || isKnownTaskType(rdfTypeUri)) {
            return true;
        }

        // Fallback heuristic for domain-specific extensions (e.g., custom *Task classes).
        if (looksLikeTaskType(hascoTypeUri) || looksLikeTaskType(rdfTypeUri)) {
            return true;
        }

        // Final fallback: ask the ontology if either type is a subclass of vstoi:Task.
        return isSubclassOfTask(hascoTypeUri) || isSubclassOfTask(rdfTypeUri);
    }

    private static boolean isKnownTaskType(String typeUri) {
        if (typeUri == null || typeUri.isEmpty()) {
            return false;
        }
        return typeUri.equals(VSTOI.TASK)
                || typeUri.equals(VSTOI.ABSTRACT_TASK)
                || typeUri.equals(VSTOI.APPLICATION_TASK)
                || typeUri.equals(VSTOI.INTERACTIVE_TASK)
                || typeUri.equals(VSTOI.USER_TASK);
    }

    private static boolean looksLikeTaskType(String typeUri) {
        if (typeUri == null || typeUri.isEmpty()) {
            return false;
        }
        int hash = typeUri.lastIndexOf('#');
        int slash = typeUri.lastIndexOf('/');
        int idx = Math.max(hash, slash);
        String local = idx >= 0 && idx < typeUri.length() - 1 ? typeUri.substring(idx + 1) : typeUri;
        return local.endsWith("Task");
    }

    private static boolean isSubclassOfTask(String typeUri) {
        if (typeUri == null || typeUri.isEmpty() || !typeUri.startsWith("http")) {
            return false;
        }
        String query = NameSpaces.getInstance().printSparqlNameSpaceList()
                + "SELECT ?cls WHERE { "
                + "<" + typeUri + "> rdfs:subClassOf* vstoi:Task . "
                + "BIND(<" + typeUri + "> AS ?cls) "
                + "} LIMIT 1";
        try {
            ResultSetRewindable rs = SPARQLUtils.select(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY),
                    query);
            return rs != null && rs.hasNext();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if a URI represents an ontology class or literal value rather than an instance.
     * Returns true for:
     * - Language literals (pmsr#en, pmsr#pt, etc.)
     * - Version numbers (pmsr#1, pmsr#2, etc.)
     * - VSTOI ontology classes
     */
    /**
     * Determines if a URI represents an ontology class (owl:Class) or a literal/version identifier.
     * 
     * This method identifies:
     * - VSTOI ontology classes (e.g., vstoi#Instrument, vstoi#PhysicalInstrument, vstoi#Component)
     * - Language literals (e.g., URIs ending with /en, #en)
     * - Version literals (e.g., pmsr#1, pmsr#2 - pure numeric fragments only)
     * 
     * Note: This method is used to determine whether to resolve a URI as an ontology class
     * (via HADatAcClass.find()) or as an instance (via GenericInstance.find()).
     * 
     * @param uri The URI to check
     * @return true if the URI represents an ontology class or literal, false otherwise
     */
    private static boolean isOntologyClass(String uri) {
        if (uri == null || uri.isEmpty()) {
            return false;
        }
        
        // VSTOI ontology classes (not instances)
        // Match any URI in the vstoi namespace with a fragment identifier (class name after #)
        // Examples: vstoi#Instrument, vstoi#PhysicalInstrument, vstoi#GroundBasedInstrument
        if (uri.contains("vstoi#")) {
            String fragment = uri.substring(uri.indexOf("vstoi#") + 6); // Extract after "vstoi#"
            
            // Check if fragment looks like a class name (starts with uppercase, contains letters)
            // This covers all VSTOI class names: Instrument, PhysicalInstrument, ComponentStem, etc.
            if (!fragment.isEmpty() && 
                Character.isUpperCase(fragment.charAt(0)) && 
                fragment.matches("^[A-Z][a-zA-Z]*$")) {
                return true;
            }
        }
        
        // Also check for other common ontology patterns (HASCO, SCHEMA, etc.)
        // Match patterns like hasco#Something, schema#Something
        if (uri.matches(".*[/#](hasco|schema|sio|prov|foaf|dcterms|skos)#[A-Z][a-zA-Z]*$")) {
            return true;
        }
        
        // Language literals: Common language codes
        if (uri.endsWith("/en") || uri.endsWith("#en") ||
            uri.endsWith("/pt") || uri.endsWith("#pt") ||
            uri.endsWith("/es") || uri.endsWith("#es") ||
            uri.endsWith("/fr") || uri.endsWith("#fr") ||
            uri.endsWith("/de") || uri.endsWith("#de") ||
            uri.endsWith("/ja") || uri.endsWith("#ja") ||
            uri.endsWith("/zh") || uri.endsWith("#zh")) {
            return true;
        }
        
        // Version literals: only treat pure numeric fragment identifiers as non-instance.
        // Example: pmsr#1, pmsr#2 (but NOT pmsr#WKF.../PROC/0001).
        int hashIndex = uri.lastIndexOf('#');
        if (hashIndex >= 0 && hashIndex < uri.length() - 1) {
            String fragment = uri.substring(hashIndex + 1);
            if (!fragment.contains("/") && fragment.matches("^\\d+$")) {
                return true;
            }
        }
        
        return false;
    }

    private static HADatAcClass resolveOntologyClass(String uri) {
        if (uri == null || uri.isEmpty()) {
            return null;
        }

        HADatAcClass direct = HADatAcClass.find(uri);
        if (direct != null) {
            return direct;
        }

        // Compatibility aliases seen in legacy data and older mapping files.
        String[] candidates = new String[] {
                uri.replace("http://pmsr.net/ont/pmsr#", "https://pmsr.net/ont/"),
                uri.replace("https://pmsr.net/ont/pmsr#", "https://pmsr.net/ont/"),
                uri.replace("http://pmsr.net/ont/", "https://pmsr.net/ont/"),
                uri.replace("https://pmsr.net/ont/pmsr#", "http://pmsr.net/ont/pmsr#"),
                uri.replace("https://hadatac.org/ont/", "http://hadatac.org/ont/"),
                uri.replace("http://hadatac.org/ont/", "https://hadatac.org/ont/")
        };

        for (String candidate : candidates) {
            if (candidate == null || candidate.isEmpty() || candidate.equals(uri)) {
                continue;
            }
            HADatAcClass alt = HADatAcClass.find(candidate);
            if (alt != null) {
                return alt;
            }
        }

        return null;
    }

    private static String normalizeLookupUri(String uri) {
        if (uri == null) {
            return null;
        }

        String normalized = uri.trim();
        if (normalized.isEmpty()) {
            return normalized;
        }

        if (normalized.startsWith("http://pmsr.net/ont/pmsr#")) {
            return "https://pmsr.net/ont/" + normalized.substring("http://pmsr.net/ont/pmsr#".length());
        }

        if (normalized.startsWith("https://pmsr.net/ont/pmsr#")) {
            return "https://pmsr.net/ont/" + normalized.substring("https://pmsr.net/ont/pmsr#".length());
        }

        return normalized;
    }

    private static boolean shouldLogMissingGenericInstanceWarning(String uri) {
        if (uri == null || uri.isEmpty()) {
            return false;
        }

        // Process URIs are often probed before WKF post-processing materializes Process/Study.
        // Avoid warning floods for known probe patterns.
        if (looksLikeWkfProcessUri(uri)) {
            return false;
        }

        long now = System.currentTimeMillis();
        Long previous = missingUriWarningLastSeen.put(uri, now);
        return previous == null || (now - previous) >= MISSING_URI_WARNING_WINDOW_MS;
    }

    private static boolean looksLikeWkfProcessUri(String uri) {
        return uri.contains("/WKF") && uri.contains("/PROC/");
    }

}
