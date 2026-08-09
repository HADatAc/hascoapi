package org.hascoapi.console.controllers.restapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.Component;
import org.hascoapi.entity.pojo.ComponentInstance;
import org.hascoapi.entity.pojo.Deployment;
import org.hascoapi.entity.fhir.Questionnaire;
import org.hascoapi.entity.pojo.Instrument;
import org.hascoapi.entity.pojo.InstrumentInstance;
import org.hascoapi.entity.pojo.PlatformInstance;
import org.hascoapi.entity.pojo.ContainerSlot;
import org.hascoapi.transform.Renderings;
import org.hascoapi.transform.InstrumentTraversal;
import org.hascoapi.utils.ApiUtil;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.HAScOMapper;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.vocabularies.VSTOI;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.libs.Json;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hascoapi.Constants.TEST_INSTRUMENT_URI;
import static org.hascoapi.Constants.TEST_INSTRUMENT_TOT_CONTAINER_SLOTS;

public class InstrumentAPI extends Controller {

    private Result createInstrumentResult(Instrument inst) {
        inst.save();
        return ok(ApiUtil.createResponse("Instrument <" + inst.getUri() + "> has been CREATED.", true));
    }

    public Result createInstrumentForTesting() {
        Instrument testInstrument = Instrument.find(TEST_INSTRUMENT_URI);
        if (testInstrument != null) {
            return ok(ApiUtil.createResponse("Test instrument <" + TEST_INSTRUMENT_URI + "> already exists.", false));
        } else {
            testInstrument = new Instrument(VSTOI.INSTRUMENT);
            testInstrument.setUri(TEST_INSTRUMENT_URI);
            testInstrument.setLabel("Test Instrument");
            testInstrument.setTypeUri(VSTOI.QUESTIONNAIRE);
            testInstrument.setHascoTypeUri(VSTOI.INSTRUMENT);
            testInstrument.setHasInformant(VSTOI.DEFAULT_INFORMANT);
            testInstrument.setHasShortName("TEST");
            testInstrument.setHasLanguage(VSTOI.DEFAULT_LANGUAGE); // ISO 639-1
            testInstrument.setComment("This is a dummy instrument created to test the SIR API.");
            testInstrument.setHasVersion("1");
            testInstrument.setHasSIRManagerEmail("me@example.com");
            testInstrument.setNamedGraph(Constants.TEST_KB);

            return createInstrumentResult(testInstrument);
        }
    }

    public Result createInstrument(String json) {
        if (json == null || json.equals("")) {
            return ok(ApiUtil.createResponse("No json content has been provided.", false));
        }
        //System.out.println("(InstrumentAPI) Value of json in createInstrument: [" + json + "]");
        ObjectMapper objectMapper = new ObjectMapper();
        Instrument newInst;
        try {
            //convert json string to Instrument instance
            newInst  = objectMapper.readValue(json, Instrument.class);
            
            // Validate required fields
            if (newInst.getLabel() == null || newInst.getLabel().trim().isEmpty()) {
                return ok(ApiUtil.createResponse("Field 'label' is required but was not provided.", false));
            }
            if (newInst.getUri() == null || newInst.getUri().trim().isEmpty()) {
                return ok(ApiUtil.createResponse("Field 'uri' is required but was not provided.", false));
            }
            if (newInst.getNamedGraph() == null || newInst.getNamedGraph().trim().isEmpty()) {
                return ok(ApiUtil.createResponse("Field 'namedGraph' is required but was not provided.", false));
            }
            if (newInst.getHasSIRManagerEmail() == null || newInst.getHasSIRManagerEmail().trim().isEmpty()) {
                return ok(ApiUtil.createResponse("Field 'hasSIRManagerEmail' is required but was not provided.", false));
            }
        } catch (Exception e) {
            //System.out.println("(InstrumentAPI) Failed to parse json for [" + json + "]");
            return ok(ApiUtil.createResponse("Failed to parse json.", false));
        }
        return createInstrumentResult(newInst);
    }

    private Result deleteInstrumentResult(Instrument inst) {
        String uri = inst.getUri();
        inst.delete();
        return ok(ApiUtil.createResponse("Instrument <" + uri + "> has been DELETED.", true));
    }

    public Result deleteInstrumentForTesting(){
        Instrument test;
        test = Instrument.find(TEST_INSTRUMENT_URI);
        if (test == null) {
            return ok(ApiUtil.createResponse("There is no Test instrument to be deleted.", false));
        } else {
            test.setNamedGraph(Constants.TEST_KB);
            return deleteInstrumentResult(test);
        }
    }

    public Result deleteInstrument(String uri){
        if (uri == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No instrument URI has been provided.", false));
        }
        Instrument inst = Instrument.find(uri);
        if (inst == null) {
            return ok(ApiUtil.createResponse("There is no instrument with URI <" + uri + "> to be deleted.", false));
        } else {
            return deleteInstrumentResult(inst);
        }
    }

    public static Result getInstruments(List<Instrument> results){
        if (results == null) {
            return ok(ApiUtil.createResponse("No instrument has been found", false));
        } else {
            ObjectMapper mapper = HAScOMapper.getFiltered(HAScOMapper.FULL,VSTOI.INSTRUMENT);
            JsonNode jsonObject = mapper.convertValue(results, JsonNode.class);
            return ok(ApiUtil.createResponse(jsonObject, true));
        }
    }

    private String normalizeUberonUri(String uberonUri) {
        if (uberonUri == null) {
            return null;
        }

        String trimmed = uberonUri.trim();
        if (trimmed.startsWith("b64:")) {
            String token = trimmed.substring(4);
            try {
                byte[] decoded = Base64.getUrlDecoder().decode(token);
                return new String(decoded, StandardCharsets.UTF_8).trim();
            } catch (IllegalArgumentException e) {
                return "";
            }
        }

        return trimmed;
    }

    public Result findInstrumentsByAnatomy(String uberonUri, Http.Request request) {
        String normalizedUberonUri = normalizeUberonUri(uberonUri);
        if (normalizedUberonUri == null || normalizedUberonUri.isEmpty()) {
            return ok(ApiUtil.createResponse("No UBERON URI has been provided", false));
        }

        String organizationUri = request.getQueryString("organizationUri");
        List<Instrument> results = Instrument.findByAnatomy(normalizedUberonUri, organizationUri);
        return getInstruments(results);
    }

    public Result findTotalInstrumentsByAnatomy(String uberonUri, Http.Request request) {
        String normalizedUberonUri = normalizeUberonUri(uberonUri);
        if (normalizedUberonUri == null || normalizedUberonUri.isEmpty()) {
            return ok(ApiUtil.createResponse("No UBERON URI has been provided", false));
        }

        String organizationUri = request.getQueryString("organizationUri");
        int totalElements = Instrument.findTotalByAnatomy(normalizedUberonUri, organizationUri);
        String totalElementsJSON = "{\"total\":" + totalElements + "}";
        return ok(ApiUtil.createResponse(totalElementsJSON, true));
    }

    public Result toTextPlain(String uri) {
        if (uri  == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No URI has been provided", false));
        }
        String instrumentText = Renderings.toString(uri, 80);
        if (instrumentText == null || instrumentText.equals("")) {
            return ok(ApiUtil.createResponse("No instrument has been found", false));
        } else {
            return ok(instrumentText).as("text/plain");
        }
    }

    public Result toTextHTML(String uri) {
        if (uri  == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No URI has been provided", false));
        }
        String instrumentText = Renderings.toHTML(uri, 80);
        if (instrumentText == null || instrumentText.equals("")) {
            return ok(ApiUtil.createResponse("No instrument has been found", false));
        } else {
            return ok(instrumentText).as("text/html");
        }
    }

    public Result toTextPDF(String uri) {
        if (uri  == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No URI has been provided", false));
        }
        ByteArrayOutputStream instrumentText = Renderings.toPDF(uri, 80);
        if (instrumentText == null || instrumentText.equals("")) {
            return ok(ApiUtil.createResponse("No instrument has been found", false));
        } else {
            return ok(instrumentText.toByteArray()).as("application/pdf");
        }
    }

    public Result toFHIR(String uri) {
        if (uri  == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No URI has been provided", false));
        }
        Instrument instr = Instrument.find(uri);
        if (instr == null) {
            return ok(ApiUtil.createResponse("No instrument instance found for uri [" + uri + "]", false));
        }

        Questionnaire quest = new Questionnaire(instr);

        FhirContext ctx = FhirContext.forR4();
        IParser parser = ctx.newJsonParser();
        String serialized = parser.encodeResourceToString(quest.getFHIRObject());

        return ok(serialized).as("application/json");
    }

    public Result toRDF(String uri) {
        if (uri  == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No URI has been provided", false));
        }
        Instrument instr = Instrument.find(uri);
        if (instr == null) {
            return ok(ApiUtil.createResponse("No instrument instance found for uri [" + uri + "]", false));
        }

        String serialized = instr.printRDF();

        return ok(serialized).as("application/xml");
    }

    public Result updateReviewsRecursive(String uri, String status) {
        //System.out.println("updateReviewsRecursive: [" + uri + "]");
        if (uri  == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No URI has been provided", false));
        }
        if (status  == null || status.equals("")) {
            return ok(ApiUtil.createResponse("No new status value has been provided", false));
        }
        Instrument instr = Instrument.find(uri);
        if (instr == null) {
            return ok(ApiUtil.createResponse("No instrument instance found for uri [" + uri + "]", false));
        }

        int totalElements = InstrumentTraversal.updateStatusRecursive(uri, status);
        if (totalElements >= 0) { 
        String totalElementsJSON = "{\"total\":" + totalElements + "}";
            return ok(ApiUtil.createResponse(totalElementsJSON, true));
        }
        return ok(ApiUtil.createResponse("updataReviewsRecursive() failed to retrieve total number of element", false));
    }

    public Result retrieveInstrumentComponents(String uri) {
        //System.out.println("retrieveInstrumentComponents: [" + uri + "]");
        if (uri  == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No URI has been provided", false));
        }
        Instrument instr = Instrument.find(uri);
        if (instr == null) {
            return ok(ApiUtil.createResponse("No instrument instance found for uri [" + uri + "]", false));
        }

        List<String> components = InstrumentTraversal.retrieveInstrumentComponents(uri);
        if (components.size() >= 0) { 
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                String jsonArray = objectMapper.writeValueAsString(components);
                return ok(ApiUtil.createResponse(jsonArray, true));
            } catch (Exception e) {
                return ok(ApiUtil.createResponse("retrieveInstrumentComponents() failed to retrieve components", false));
            }
        }
        return ok(ApiUtil.createResponse("retrieveInstrumentComponents() failed to retrieve components", false));
    }

    public Result retrieveInstrumentContainerSlots(String uri) {
        //System.out.println("retrieveInstrumentContainerSlots: [" + uri + "]");
        if (uri  == null || uri.equals("")) {
            return ok(ApiUtil.createResponse("No URI has been provided", false));
        }
        Instrument instr = Instrument.find(uri);
        if (instr == null) {
            return ok(ApiUtil.createResponse("No instrument instance found for uri [" + uri + "]", false));
        }

        List<ContainerSlot> containerSlots = InstrumentTraversal.retrieveInstrumentContainerSlots(uri);
        if (containerSlots.size() >= 0) { 
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                String jsonArray = objectMapper.writeValueAsString(containerSlots);
                return ok(ApiUtil.createResponse(jsonArray, true));
            } catch (Exception e) {
                return ok(ApiUtil.createResponse("retrieveInstrumentContainerSlots() failed to retrieve containerSlots", false));
            }
        }
        return ok(ApiUtil.createResponse("retrieveInstrumentContainerSlots() failed to retrieve containerSlots", false));
    }

    private String normalizeFilterUri(String uri) {
        if (uri == null) {
            return "";
        }
        String trimmed = uri.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if (trimmed.startsWith("<") && trimmed.endsWith(">") && trimmed.length() > 2) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        if (trimmed.contains("<") || trimmed.contains(">") || trimmed.contains("\"") || trimmed.contains(" ")) {
            return "";
        }
        return trimmed;
    }

    private Set<String> parseScopeUris(Http.Request request) {
        Set<String> scope = new LinkedHashSet<String>();

        String organizationUri = normalizeFilterUri(request.getQueryString("organizationUri"));
        if (!organizationUri.isEmpty()) {
            scope.add(organizationUri);
        }

        String csvScope = request.getQueryString("organizationScopeUris");
        if (csvScope != null && !csvScope.trim().isEmpty()) {
            String[] values = csvScope.split(",");
            for (String value : values) {
                String normalized = normalizeFilterUri(value);
                if (!normalized.isEmpty()) {
                    scope.add(normalized);
                }
            }
        }

        String scoped = request.getQueryString("organizationScopeUri");
        if (scoped != null && !scoped.trim().isEmpty()) {
            String normalized = normalizeFilterUri(scoped);
            if (!normalized.isEmpty()) {
                scope.add(normalized);
            }
        }

        return scope;
    }

    private ResultSetRewindable selectDeploymentsForScope(Set<String> organizationScopeUris, int pageSize, int offset) {
        StringBuilder query = new StringBuilder();
        query.append(NameSpaces.getInstance().printSparqlNameSpaceList());
        query.append(" SELECT DISTINCT ?deployment ?instrumentInstance ?platformInstance ?org WHERE { ");
        query.append("   ?deployment hasco:hascoType vstoi:Deployment . ");
        query.append("   ?deployment vstoi:hasInstrumentInstance ?instrumentInstance . ");
        query.append("   ?instrumentInstance vstoi:hasOwner ?org . ");
        query.append("   OPTIONAL { ?deployment vstoi:hasPlatformInstance ?platformInstance . } ");

        if (!organizationScopeUris.isEmpty()) {
            query.append("   FILTER(?org IN (");
            int i = 0;
            for (String org : organizationScopeUris) {
                if (i > 0) {
                    query.append(", ");
                }
                query.append("<").append(org).append(">");
                i += 1;
            }
            query.append(")) . ");
        }

        query.append(" } ORDER BY ?deployment ");
        query.append(" LIMIT ").append(pageSize);
        query.append(" OFFSET ").append(offset);

        return SPARQLUtils.select(
            CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY),
            query.toString()
        );
    }

    private String extractUriLocalName(String uri) {
        if (uri == null || uri.trim().isEmpty()) {
            return "";
        }
        String value = uri.trim();
        int hash = value.lastIndexOf('#');
        int slash = value.lastIndexOf('/');
        int idx = Math.max(hash, slash);
        if (idx >= 0 && idx + 1 < value.length()) {
            return value.substring(idx + 1).trim();
        }
        return value;
    }

    private String deriveComponentRole(String componentTypeUri, Map<String, String> typeRoleCache) {
        String normalizedTypeUri = normalizeFilterUri(componentTypeUri);
        if (normalizedTypeUri.isEmpty()) {
            return "";
        }

        if (typeRoleCache.containsKey(normalizedTypeUri)) {
            return typeRoleCache.get(normalizedTypeUri);
        }

        String detectorTypeUri = VSTOI.VSTOI + "Detector";
        String actuatorTypeUri = VSTOI.VSTOI + "Actuator";
        String resolvedRole = "";

        try {
            String query = NameSpaces.getInstance().printSparqlNameSpaceList()
                + " SELECT ?x WHERE { "
                + "   <" + normalizedTypeUri + "> rdfs:subClassOf* <" + detectorTypeUri + "> . "
                + "   BIND(<" + normalizedTypeUri + "> AS ?x) "
                + " } LIMIT 1";
            ResultSetRewindable rs = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY),
                query
            );
            if (rs != null && rs.hasNext()) {
                resolvedRole = "detector";
            }
        } catch (Exception ignored) {}

        if (resolvedRole.isEmpty()) {
            try {
                String query = NameSpaces.getInstance().printSparqlNameSpaceList()
                    + " SELECT ?x WHERE { "
                    + "   <" + normalizedTypeUri + "> rdfs:subClassOf* <" + actuatorTypeUri + "> . "
                    + "   BIND(<" + normalizedTypeUri + "> AS ?x) "
                    + " } LIMIT 1";
                ResultSetRewindable rs = SPARQLUtils.select(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY),
                    query
                );
                if (rs != null && rs.hasNext()) {
                    resolvedRole = "actuator";
                }
            } catch (Exception ignored) {}
        }

        typeRoleCache.put(normalizedTypeUri, resolvedRole);
        return resolvedRole;
    }

    private Map<String, List<String>> selectComponentInstancesByInstrumentLocal() {
        Map<String, List<String>> byInstrumentLocal = new LinkedHashMap<String, List<String>>();

        StringBuilder query = new StringBuilder();
        query.append(NameSpaces.getInstance().printSparqlNameSpaceList());
        query.append(" SELECT DISTINCT ?cpi WHERE { ");
        query.append("   ?cpi hasco:hascoType vstoi:ComponentInstance . ");
        query.append(" } ORDER BY ?cpi ");

        ResultSetRewindable rs = SPARQLUtils.select(
            CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY),
            query.toString()
        );

        while (rs.hasNext()) {
            QuerySolution soln = rs.next();
            if (soln == null || soln.getResource("cpi") == null) {
                continue;
            }

            String cpiUri = normalizeFilterUri(soln.getResource("cpi").getURI());
            if (cpiUri.isEmpty()) {
                continue;
            }

            String local = extractUriLocalName(cpiUri);
            if (local.isEmpty() || !local.startsWith("CPI-")) {
                continue;
            }

            // CPI local pattern expected: CPI-INIxxxx-COMyyyy
            String remainder = local.substring(4);
            int sep = remainder.indexOf("-");
            if (sep <= 0) {
                continue;
            }

            String instrumentLocal = remainder.substring(0, sep).trim();
            if (instrumentLocal.isEmpty() || !instrumentLocal.startsWith("INI")) {
                continue;
            }

            List<String> values = byInstrumentLocal.get(instrumentLocal);
            if (values == null) {
                values = new ArrayList<String>();
                byInstrumentLocal.put(instrumentLocal, values);
            }
            values.add(cpiUri);
        }

        return byInstrumentLocal;
    }

    private String deriveComponentModelUriFromComponentInstanceUri(String componentInstanceUri) {
        String normalized = normalizeFilterUri(componentInstanceUri);
        if (normalized.isEmpty()) {
            return "";
        }

        String local = extractUriLocalName(normalized);
        if (local.isEmpty() || !local.startsWith("CPI-")) {
            return "";
        }

        String remainder = local.substring(4);
        int sep = remainder.indexOf("-");
        if (sep <= 0 || sep + 1 >= remainder.length()) {
            return "";
        }

        String componentLocal = remainder.substring(sep + 1).trim();
        if (componentLocal.isEmpty() || !componentLocal.startsWith("COM")) {
            return "";
        }

        int slash = normalized.lastIndexOf('/');
        if (slash < 0) {
            return "";
        }

        return normalized.substring(0, slash + 1) + componentLocal;
    }

    public Result findOrganizationScopedInstrumentPrefilter(Http.Request request) {
        String studyUri = normalizeFilterUri(request.getQueryString("studyUri"));
        String processUri = normalizeFilterUri(request.getQueryString("processUri"));
        String organizationUri = normalizeFilterUri(request.getQueryString("organizationUri"));

        int pageSize = 400;
        int offset = 0;
        try {
            String pageSizeRaw = request.getQueryString("pageSize");
            if (pageSizeRaw != null && !pageSizeRaw.trim().isEmpty()) {
                pageSize = Math.max(1, Math.min(1000, Integer.parseInt(pageSizeRaw.trim())));
            }
        } catch (Exception ignored) {}
        try {
            String offsetRaw = request.getQueryString("offset");
            if (offsetRaw != null && !offsetRaw.trim().isEmpty()) {
                offset = Math.max(0, Integer.parseInt(offsetRaw.trim()));
            }
        } catch (Exception ignored) {}

        Set<String> organizationScopeUris = parseScopeUris(request);
        if (organizationScopeUris.isEmpty() && !organizationUri.isEmpty()) {
            organizationScopeUris.add(organizationUri);
        }

        ObjectNode payload = Json.newObject();
        payload.put("organizationUri", organizationUri);
        payload.put("studyUri", studyUri);
        payload.put("processUri", processUri);

        ArrayNode scopeNode = Json.newArray();
        for (String scopeUri : organizationScopeUris) {
            scopeNode.add(scopeUri);
        }
        payload.set("organizationScopeUris", scopeNode);

        ArrayNode instrumentsNode = Json.newArray();
        payload.set("instruments", instrumentsNode);

        if (organizationScopeUris.isEmpty()) {
            ObjectNode response = Json.newObject();
            response.put("ok", true);
            response.put("generatedAt", java.time.Instant.now().toString());
            response.put("count", 0);
            response.set("payload", payload);
            return ok(response);
        }

        try {
            ResultSetRewindable results = selectDeploymentsForScope(organizationScopeUris, pageSize, offset);
            Map<String, List<String>> componentInstancesByInstrumentLocal = selectComponentInstancesByInstrumentLocal();

            Map<String, ObjectNode> byInstrument = new LinkedHashMap<String, ObjectNode>();
            Map<String, Set<String>> componentSeen = new LinkedHashMap<String, Set<String>>();
            Map<String, String> componentTypeRoleCache = new LinkedHashMap<String, String>();
            Map<String, ComponentInstance> componentInstanceCache = new LinkedHashMap<String, ComponentInstance>();
            Map<String, Component> componentModelCache = new LinkedHashMap<String, Component>();
            int scopedDeployments = 0;
            int scopedComponents = 0;

            while (results.hasNext()) {
                QuerySolution soln = results.next();
                if (soln == null || soln.getResource("deployment") == null || soln.getResource("instrumentInstance") == null) {
                    continue;
                }

                String instrumentInstanceUri = soln.getResource("instrumentInstance").getURI().trim();
                String platformInstanceUri = soln.getResource("platformInstance") != null
                    ? soln.getResource("platformInstance").getURI().trim()
                    : "";

                scopedDeployments += 1;

                if (instrumentInstanceUri.isEmpty()) {
                    continue;
                }

                InstrumentInstance instrumentInstance = InstrumentInstance.find(instrumentInstanceUri);
                if (instrumentInstance == null) {
                    continue;
                }

                String instrumentUri = normalizeFilterUri(instrumentInstance.getTypeUri());
                if (instrumentUri.isEmpty()) {
                    continue;
                }

                ObjectNode instrumentNode = byInstrument.get(instrumentUri);
                if (instrumentNode == null) {
                    instrumentNode = Json.newObject();
                    instrumentNode.put("uri", instrumentUri);
                    instrumentNode.put("hasURI", instrumentUri);

                    Instrument instrument = Instrument.find(instrumentUri);
                    if (instrument != null) {
                        String label = instrument.getLabel() != null ? instrument.getLabel().trim() : "";
                        String status = instrument.getHasStatus() != null ? instrument.getHasStatus().trim() : "";
                        instrumentNode.put("label", label.isEmpty() ? instrumentUri : label);
                        instrumentNode.put("hasStatus", status);
                    } else {
                        instrumentNode.put("label", instrumentUri);
                        instrumentNode.put("hasStatus", "");
                    }

                    instrumentNode.set("instanceUris", Json.newArray());
                    instrumentNode.set("platforms", Json.newArray());
                    instrumentNode.set("components", Json.newArray());

                    byInstrument.put(instrumentUri, instrumentNode);
                    componentSeen.put(instrumentUri, new LinkedHashSet<String>());
                }

                ArrayNode instanceUrisNode = (ArrayNode) instrumentNode.get("instanceUris");
                boolean hasInstance = false;
                for (JsonNode instanceNode : instanceUrisNode) {
                    if (instrumentInstanceUri.equals(instanceNode.asText())) {
                        hasInstance = true;
                        break;
                    }
                }
                if (!hasInstance) {
                    instanceUrisNode.add(instrumentInstanceUri);
                }

                if (!platformInstanceUri.isEmpty()) {
                    ArrayNode platformsNode = (ArrayNode) instrumentNode.get("platforms");
                    boolean hasPlatform = false;
                    for (JsonNode existingPlatformNode : platformsNode) {
                        if (platformInstanceUri.equals(existingPlatformNode.path("uri").asText())) {
                            hasPlatform = true;
                            break;
                        }
                    }

                    if (!hasPlatform) {
                        ObjectNode platformNode = Json.newObject();
                        platformNode.put("uri", platformInstanceUri);

                        PlatformInstance platformInstance = PlatformInstance.find(platformInstanceUri);
                        String platformLabel = platformInstance != null && platformInstance.getLabel() != null
                            ? platformInstance.getLabel().trim()
                            : "";
                        platformNode.put("label", platformLabel.isEmpty() ? platformInstanceUri : platformLabel);
                        platformsNode.add(platformNode);
                    }
                }

                String instrumentLocal = extractUriLocalName(instrumentInstanceUri);
                List<String> componentUris = componentInstancesByInstrumentLocal.get(instrumentLocal);
                if (componentUris == null) {
                    continue;
                }

                for (String componentUriRaw : componentUris) {
                    String componentUri = normalizeFilterUri(componentUriRaw);
                    if (componentUri.isEmpty()) {
                        continue;
                    }

                    Set<String> seenForInstrument = componentSeen.get(instrumentUri);
                    if (seenForInstrument == null) {
                        seenForInstrument = new LinkedHashSet<String>();
                        componentSeen.put(instrumentUri, seenForInstrument);
                    }
                    if (seenForInstrument.contains(componentUri)) {
                        continue;
                    }
                    seenForInstrument.add(componentUri);

                    ComponentInstance componentInstance = componentInstanceCache.get(componentUri);
                    if (componentInstance == null) {
                        componentInstance = ComponentInstance.find(componentUri);
                        componentInstanceCache.put(componentUri, componentInstance);
                    }

                    String componentLabel = componentUri;
                    String componentStatus = "";
                    String componentModelUri = deriveComponentModelUriFromComponentInstanceUri(componentUri);
                    String componentModelLabel = "";
                    String componentTypeUri = "";
                    String componentTypeLabel = "";
                    String componentRole = "";

                    if (componentInstance != null) {
                        if (componentInstance.getLabel() != null && !componentInstance.getLabel().trim().isEmpty()) {
                            componentLabel = componentInstance.getLabel().trim();
                        }
                        if (componentInstance.getHasStatus() != null) {
                            componentStatus = componentInstance.getHasStatus().trim();
                        }
                        if (componentModelUri.isEmpty()) {
                            componentModelUri = normalizeFilterUri(componentInstance.getTypeUri());
                        }
                    }

                    if (!componentModelUri.isEmpty()) {
                        Component componentModel = componentModelCache.get(componentModelUri);
                        if (!componentModelCache.containsKey(componentModelUri)) {
                            componentModel = Component.find(componentModelUri);
                            componentModelCache.put(componentModelUri, componentModel);
                        }

                        if (componentModel != null) {
                            componentModelLabel = componentModel.getLabel() != null
                                ? componentModel.getLabel().trim()
                                : "";
                            componentTypeUri = normalizeFilterUri(componentModel.getTypeUri());
                        }
                    }

                    if (!componentTypeUri.isEmpty()) {
                        componentTypeLabel = extractUriLocalName(componentTypeUri);
                        componentRole = deriveComponentRole(componentTypeUri, componentTypeRoleCache);
                    }

                    ObjectNode componentNode = Json.newObject();
                    componentNode.put("uri", componentUri);
                    componentNode.put("hasURI", componentUri);
                    componentNode.put("label", componentLabel);
                    componentNode.put("hasStatus", componentStatus);
                    componentNode.put("componentModelUri", componentModelUri);
                    componentNode.put("componentModelLabel", componentModelLabel.isEmpty() ? componentModelUri : componentModelLabel);
                    componentNode.put("componentTypeUri", componentTypeUri);
                    componentNode.put("componentTypeLabel", componentTypeLabel);
                    componentNode.put("componentRole", componentRole);
                    ((ArrayNode) instrumentNode.get("components")).add(componentNode);
                    scopedComponents += 1;
                }
            }

            if (scopedDeployments > 0 && scopedComponents == 0) {
                ObjectNode error = Json.newObject();
                error.put("ok", false);
                error.put("error", "Prefilter inconsistency: deployments found in scope but zero component instances resolved.");
                error.put("deployments", scopedDeployments);
                error.put("components", scopedComponents);
                error.put("organizationUri", organizationUri);
                return internalServerError(error);
            }

            for (Map.Entry<String, ObjectNode> entry : byInstrument.entrySet()) {
                instrumentsNode.add(entry.getValue());
            }

            ObjectNode response = Json.newObject();
            response.put("ok", true);
            response.put("generatedAt", java.time.Instant.now().toString());
            response.put("count", byInstrument.size());
            response.set("payload", payload);
            return ok(response);
        }
        catch (Exception e) {
            ObjectNode error = Json.newObject();
            error.put("ok", false);
            error.put("error", e.getMessage() == null ? "Failed to build instrument prefilter" : e.getMessage());
            return internalServerError(error);
        }
    }

}
