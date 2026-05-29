package org.hascoapi.ingestion;

import java.lang.String;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.HADatAcThing;
import org.hascoapi.entity.pojo.Instrument;
import org.hascoapi.entity.pojo.Component;
import org.hascoapi.entity.pojo.ComponentStem;
import org.hascoapi.entity.pojo.ContainerSlot;
import org.hascoapi.entity.pojo.Codebook;
import org.hascoapi.entity.pojo.ResponseOption;
import org.hascoapi.entity.pojo.AnnotationStem;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.VSTOI;

/**
 * SIRObjectGenerator - Creates SIR entities directly from DSG SOC sheets
 * 
 * This generator replaces the dual-layer approach where SIR objects were created
 * as both StudyObjects AND specialized SIR entities. Now SIR objects (Instrument,
 * Component, etc.) are created ONLY as pure SIR entities.
 * 
 * Supported SIR types:
 * - vstoi:Instrument
 * - vstoi:Component
 * - vstoi:ComponentStem
 * - vstoi:ContainerSlot
 * - vstoi:Codebook
 * - vstoi:ResponseOption
 * - vstoi:AnnotationStem
 */
public class SIRObjectGenerator extends BaseGenerator {

    private String namespace;
    private String socUri;  // The SOC this generator is processing

    public SIRObjectGenerator(DataFile dataFile, String namespace, String socUri) {
        super(dataFile);
        this.namespace = namespace;
        this.socUri = socUri;
    }

    @Override
    public void initMapping() {
        mapCol.clear();
        mapCol.put("originalID", "originalID");
        mapCol.put("uri", "hasURI");
        mapCol.put("typeUri", "rdf:type");  // ✅ CORRIGIDO: buscar de "rdf:type" em vez de "type"
        mapCol.put("label", "label");
        mapCol.put("comment", "comment");
        mapCol.put("isMemberOf", "isMemberOf");
        mapCol.put("scopeUri", "hasScope");
        mapCol.put("timeScopeUri", "hasTimeScope");
        mapCol.put("spaceScopeUri", "hasSpaceScope");
        mapCol.put("roleLabel", "hasRole");
    }

    private String getUri(Record rec) {
        String uriValue = rec.getValueByColumnName(mapCol.get("uri"));
        if (uriValue != null) {
            String trimmedUriValue = uriValue.trim();
            if (!trimmedUriValue.isEmpty()) {
                // If hasURI already provides a full URI/CURIE, preserve it.
                if (URIUtils.isValidURI(trimmedUriValue)) {
                    return URIUtils.replacePrefixEx(trimmedUriValue);
                }
                return buildNativeUriFromIdentifier(trimmedUriValue);
            }
        }

        // Fallback: build from originalID, preserving URI-valued IDs.
        String originalId = getOriginalID(rec);
        if (originalId.isEmpty()) {
            return null;
        }
        if (URIUtils.isValidURI(originalId)) {
            return URIUtils.replacePrefixEx(originalId);
        }
        return buildNativeUriFromIdentifier(originalId);
    }

    private String buildNativeUriFromIdentifier(String identifier) {
        if (identifier == null) {
            return null;
        }
        String trimmed = identifier.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (URIUtils.isValidURI(trimmed)) {
            return URIUtils.replacePrefixEx(trimmed);
        }
        if (isObjectSyntheticId(trimmed)) {
            throw new IllegalStateException("SIR ingest cannot use synthetic StudyObject identifier: " + trimmed);
        }

        String ns = namespace == null ? "" : namespace.trim();
        if (ns.isEmpty()) {
            return trimmed;
        }
        if (ns.startsWith("http://") || ns.startsWith("https://")) {
            if (ns.endsWith("#") || ns.endsWith("/")) {
                return ns + trimmed;
            }
            return ns + "#" + trimmed;
        }
        if (ns.endsWith(":")) {
            return URIUtils.replacePrefixEx(ns + trimmed);
        }
        return URIUtils.replacePrefixEx(ns + ":" + trimmed);
    }

    private boolean isObjectSyntheticId(String value) {
        if (value == null) {
            return false;
        }
        String upper = value.toUpperCase();
        return upper.contains("OBJ-") || upper.contains("OBJ_");
    }

    private String getOriginalID(Record rec) {
        String originalId = rec.getValueByColumnName(mapCol.get("originalID"));
        if (originalId == null) {
            return "";
        }
        originalId = originalId.trim();
        if (originalId.isEmpty()) {
            return "";
        }
        // Keep URI-valued originalID unchanged to preserve SIR identity fidelity.
        if (URIUtils.isValidURI(originalId)) {
            return originalId;
        }
        return originalId.replaceAll("\\s+", "");
    }

    private String getTypeUri(Record rec) {
        return rec.getValueByColumnName(mapCol.get("typeUri"));
    }

    private String getLabel(Record rec) {
        return rec.getValueByColumnName(mapCol.get("label"));
    }

    private String getComment(Record rec) {
        String comment = rec.getValueByColumnName(mapCol.get("comment"));
        if (comment == null || comment.isEmpty()) {
            return getLabel(rec);
        }
        return comment;
    }

    private String getIsMemberOf(Record rec) {
        String memberOf = rec.getValueByColumnName(mapCol.get("isMemberOf"));
        if (memberOf == null || memberOf.isEmpty()) {
            return socUri; // Default to the SOC being processed
        }
        return URIUtils.replacePrefixEx(memberOf);
    }

    private String getScopeUri(Record rec) {
        String scope = rec.getValueByColumnName(mapCol.get("scopeUri"));
        if (scope == null || scope.isEmpty()) {
            return null;
        }
        return URIUtils.replacePrefixEx(scope);
    }

    private String getTimeScopeUri(Record rec) {
        String timeScope = rec.getValueByColumnName(mapCol.get("timeScopeUri"));
        if (timeScope == null || timeScope.isEmpty()) {
            return null;
        }
        return URIUtils.replacePrefixEx(timeScope);
    }

    private String getSpaceScopeUri(Record rec) {
        String spaceScope = rec.getValueByColumnName(mapCol.get("spaceScopeUri"));
        if (spaceScope == null || spaceScope.isEmpty()) {
            return null;
        }
        return URIUtils.replacePrefixEx(spaceScope);
    }

    /**
     * Detecta o tipo SIR do typeUri
     */
    private String detectSIRType(String typeUri) {
        if (typeUri == null || typeUri.isEmpty()) {
            return null;
        }
        
        String normalizedType = URIUtils.replacePrefixEx(typeUri).toLowerCase();
        
        if (normalizedType.contains("vstoi") || normalizedType.contains("http://hadatac.org/ont/vstoi#")) {
            if (normalizedType.contains("instrument")) {
                return "Instrument";
            } else if (normalizedType.contains("componentstem")) {
                return "ComponentStem";
            } else if (normalizedType.contains("component")) {
                return "Component";
            } else if (normalizedType.contains("responseoption")) {
                return "ResponseOption";
            } else if (normalizedType.contains("codebook")) {
                return "Codebook";
            } else if (normalizedType.contains("containerslot") || normalizedType.contains("slotelement")) {
                return "ContainerSlot";
            } else if (normalizedType.contains("annotationstem")) {
                return "AnnotationStem";
            }
        }
        
        return null;
    }

    /**
     * Cria uma entidade SIR a partir de um registro
     */
    private HADatAcThing createSIREntity(Record rec) throws Exception {
        String uri = getUri(rec);
        String typeUri = getTypeUri(rec);
        String originalID = getOriginalID(rec);
        String label = getLabel(rec);
        String comment = getComment(rec);
        String isMemberOf = getIsMemberOf(rec);
        String scopeUri = getScopeUri(rec);
        String timeScopeUri = getTimeScopeUri(rec);
        String spaceScopeUri = getSpaceScopeUri(rec);

        if (uri == null || uri.isEmpty()) {
            logger.println("[SIR-GEN] Skipping record - no URI");
            return null;
        }

        if (isObjectSyntheticId(uri) || isObjectSyntheticId(originalID)) {
            throw new IllegalStateException("Blocked SIR ingest with synthetic OBJ identifier: " + uri);
        }

        if (typeUri == null || typeUri.isEmpty()) {
            logger.println("[SIR-GEN] Skipping record - no type: " + uri);
            return null;
        }

        String sirType = detectSIRType(typeUri);
        if (sirType == null) {
            logger.println("[SIR-GEN] Skipping record - not a SIR type: " + typeUri);
            return null;
        }

        logger.println("[SIR-GEN] Creating " + sirType + ": " + label + " (URI: " + uri + ")");
        System.out.println("[SIR-GEN] Creating " + sirType + ": originalID=" + originalID + ", URI=" + uri);

        HADatAcThing entity = null;

        // Create the appropriate SIR entity based on type
        switch (sirType) {
            case "Instrument": {
                Instrument instrument = new Instrument();
                instrument.setUri(uri);
                instrument.setTypeUri(URIUtils.replacePrefixEx(typeUri));
                instrument.setHascoTypeUri(VSTOI.INSTRUMENT);
                instrument.setLabel(label);
                instrument.setComment(comment);
                instrument.setNamedGraph(getNamedGraphUri());
                instrument.setHasSIRManagerEmail(this.dataFile.getHasSIRManagerEmail());
                entity = instrument;
                break;
            }
            case "Component": {
                Component component = new Component();
                component.setUri(uri);
                component.setTypeUri(URIUtils.replacePrefixEx(typeUri));
                component.setHascoTypeUri(VSTOI.COMPONENT);
                component.setLabel(label);
                component.setComment(comment);
                component.setNamedGraph(getNamedGraphUri());
                component.setHasSIRManagerEmail(this.dataFile.getHasSIRManagerEmail());
                entity = component;
                break;
            }
            case "ComponentStem": {
                ComponentStem componentStem = new ComponentStem();
                componentStem.setUri(uri);
                componentStem.setTypeUri(URIUtils.replacePrefixEx(typeUri));
                componentStem.setHascoTypeUri(VSTOI.COMPONENT_STEM);
                componentStem.setLabel(label);
                componentStem.setComment(comment);
                componentStem.setNamedGraph(getNamedGraphUri());
                componentStem.setHasSIRManagerEmail(this.dataFile.getHasSIRManagerEmail());
                entity = componentStem;
                break;
            }
            case "ContainerSlot": {
                ContainerSlot containerSlot = new ContainerSlot();
                containerSlot.setUri(uri);
                containerSlot.setTypeUri(URIUtils.replacePrefixEx(typeUri));
                containerSlot.setHascoTypeUri(VSTOI.CONTAINER_SLOT);
                containerSlot.setLabel(label);
                containerSlot.setComment(comment);
                containerSlot.setNamedGraph(getNamedGraphUri());
                // ContainerSlot doesn't have setHasSIRManagerEmail method
                entity = containerSlot;
                break;
            }
            case "Codebook": {
                Codebook codebook = new Codebook();
                codebook.setUri(uri);
                codebook.setTypeUri(URIUtils.replacePrefixEx(typeUri));
                codebook.setHascoTypeUri(VSTOI.CODEBOOK);
                codebook.setLabel(label);
                codebook.setComment(comment);
                codebook.setNamedGraph(getNamedGraphUri());
                codebook.setHasSIRManagerEmail(this.dataFile.getHasSIRManagerEmail());
                entity = codebook;
                break;
            }
            case "ResponseOption": {
                ResponseOption responseOption = new ResponseOption();
                responseOption.setUri(uri);
                responseOption.setTypeUri(URIUtils.replacePrefixEx(typeUri));
                responseOption.setHascoTypeUri(VSTOI.RESPONSE_OPTION);
                responseOption.setLabel(label);
                responseOption.setComment(comment);
                responseOption.setNamedGraph(getNamedGraphUri());
                responseOption.setHasSIRManagerEmail(this.dataFile.getHasSIRManagerEmail());
                entity = responseOption;
                break;
            }
            case "AnnotationStem": {
                AnnotationStem annotationStem = new AnnotationStem();
                annotationStem.setUri(uri);
                annotationStem.setTypeUri(URIUtils.replacePrefixEx(typeUri));
                annotationStem.setHascoTypeUri(VSTOI.ANNOTATION_STEM);
                annotationStem.setLabel(label);
                annotationStem.setComment(comment);
                annotationStem.setNamedGraph(getNamedGraphUri());
                annotationStem.setHasSIRManagerEmail(this.dataFile.getHasSIRManagerEmail());
                entity = annotationStem;
                break;
            }
        }

        // Save the entity first
        if (entity != null) {
            // Defensive cleanup: remove old dual-layer legacy instance URIs for the same originalID.
            cleanupLegacyDuplicateSirInstances(originalID, uri);
            entity.saveToTripleStore(true, false);
            
            // Add hasco:originalID and hasco:isMemberOf via SPARQL INSERT
            // These properties are not part of the POJO model but are needed for DASOC to work
            if (originalID != null && !originalID.isEmpty()) {
                addRDFProperty(uri, "hasco:originalID", originalID, false);
            }
            if (isMemberOf != null && !isMemberOf.isEmpty()) {
                addRDFProperty(uri, "hasco:isMemberOf", isMemberOf, true);
            }
            if (scopeUri != null && !scopeUri.isEmpty()) {
                addRDFProperty(uri, "hasco:hasScope", scopeUri, true);
            }
            if (timeScopeUri != null && !timeScopeUri.isEmpty()) {
                addRDFProperty(uri, "hasco:hasTimeScope", timeScopeUri, true);
            }
            if (spaceScopeUri != null && !spaceScopeUri.isEmpty()) {
                addRDFProperty(uri, "hasco:hasSpaceScope", spaceScopeUri, true);
            }
            if (("Instrument".equals(sirType) || "ComponentStem".equals(sirType)) &&
                    scopeUri != null && !scopeUri.isEmpty()) {
                addRDFProperty(uri, "rdfs:subClassOf", scopeUri, true);
            }
        }

        logger.println("[SIR-GEN] ✅ Created " + sirType + ": " + label);
        return entity;
    }

    /**
     * Removes legacy dual-layer SIR instance resources (e.g., INST-INS..., COMP-COM...)
     * that share the same originalID as the canonical native URI.
     */
    private void cleanupLegacyDuplicateSirInstances(String originalID, String canonicalUri) {
        if (originalID == null || originalID.trim().isEmpty() || canonicalUri == null || canonicalUri.trim().isEmpty()) {
            return;
        }
        try {
            String escapedOriginalId = originalID.trim().replace("\\", "\\\\").replace("\"", "\\\"");
            String ns = org.hascoapi.utils.NameSpaces.getInstance().printSparqlNameSpaceList();
            String select = ns +
                    "SELECT DISTINCT ?dup WHERE { " +
                    "  { ?dup hasco:originalID \"" + escapedOriginalId + "\" . } " +
                    "  UNION { GRAPH ?g { ?dup hasco:originalID \"" + escapedOriginalId + "\" . } } " +
                    "  FILTER(str(?dup) != \"" + canonicalUri + "\") " +
                    "  FILTER( " +
                    "    CONTAINS(str(?dup), \"#INST-INS\") || CONTAINS(str(?dup), \"/INST-INS\") || " +
                    "    CONTAINS(str(?dup), \"#COMP-COM\") || CONTAINS(str(?dup), \"/COMP-COM\") || " +
                    "    CONTAINS(str(?dup), \"#CSTEM-CSM\") || CONTAINS(str(?dup), \"/CSTEM-CSM\") || " +
                    "    CONTAINS(str(?dup), \"#CB-CBK\") || CONTAINS(str(?dup), \"/CB-CBK\") || " +
                    "    CONTAINS(str(?dup), \"#ROPT-ROP\") || CONTAINS(str(?dup), \"/ROPT-ROP\") || " +
                    "    CONTAINS(str(?dup), \"#CTSLOT-CTS\") || CONTAINS(str(?dup), \"/CTSLOT-CTS\") || " +
                    "    CONTAINS(str(?dup), \"#ASTEM-ASM\") || CONTAINS(str(?dup), \"/ASTEM-ASM\") " +
                    "  ) " +
                    "}";

            org.apache.jena.query.ResultSet rs = org.hascoapi.utils.SPARQLUtils.select(
                    org.hascoapi.utils.CollectionUtil.getCollectionPath(
                            org.hascoapi.utils.CollectionUtil.Collection.SPARQL_QUERY),
                    select);

            Set<String> duplicates = new HashSet<String>();
            while (rs != null && rs.hasNext()) {
                org.apache.jena.query.QuerySolution soln = rs.next();
                if (soln.get("dup") != null) {
                    String dup = soln.get("dup").toString();
                    if (dup != null && !dup.trim().isEmpty()) {
                        duplicates.add(dup.trim());
                    }
                }
            }

            for (String dupUri : duplicates) {
                deleteResourceFromAllGraphs(dupUri);
                logger.println("[SIR-GEN] Removed legacy duplicate SIR URI: " + dupUri + " (originalID=" + originalID + ")");
            }
        } catch (Exception e) {
            logger.println("[SIR-GEN] WARNING: Failed duplicate cleanup for originalID=" + originalID + ": " + e.getMessage());
        }
    }

    /**
     * Deletes all outgoing and incoming triples for a resource in both default and named graphs.
     */
    private void deleteResourceFromAllGraphs(String uri) {
        if (uri == null || uri.trim().isEmpty()) {
            return;
        }
        String target = uri.trim();
        String update = org.hascoapi.utils.NameSpaces.getInstance().printSparqlNameSpaceList() +
                "DELETE { " +
                "  <" + target + "> ?p ?o . " +
                "  ?s ?pin <" + target + "> . " +
                "} WHERE { " +
                "  { <" + target + "> ?p ?o . } UNION { ?s ?pin <" + target + "> . } " +
                "} ; " +
                "DELETE { GRAPH ?g { " +
                "  <" + target + "> ?p2 ?o2 . " +
                "  ?s2 ?pin2 <" + target + "> . " +
                "} } WHERE { GRAPH ?g { " +
                "  { <" + target + "> ?p2 ?o2 . } UNION { ?s2 ?pin2 <" + target + "> . } " +
                "} }";

        org.apache.jena.update.UpdateRequest request = org.apache.jena.update.UpdateFactory.create(update);
        org.apache.jena.update.UpdateProcessor processor = org.apache.jena.update.UpdateExecutionFactory.createRemote(
                request,
                org.hascoapi.utils.CollectionUtil.getCollectionPath(
                        org.hascoapi.utils.CollectionUtil.Collection.SPARQL_UPDATE));
        processor.execute();
    }

    /**
     * Add an RDF property via SPARQL INSERT
     * @param subjectUri Subject URI
     * @param property Property (e.g., "hasco:originalID")
     * @param value Value (literal or URI)
     * @param isUri true if value is a URI, false if it's a literal
     */
    private void addRDFProperty(String subjectUri, String property, String value, boolean isUri) {
        try {
            String valueStr = isUri ? "<" + value + ">" : "\"" + value + "\"";
            String graphUri = getNamedGraphUri();
            
            String updateQuery;
            if (graphUri != null && !graphUri.isEmpty()) {
                updateQuery = org.hascoapi.utils.NameSpaces.getInstance().printSparqlNameSpaceList() +
                        "INSERT DATA { GRAPH <" + graphUri + "> { " +
                        "<" + subjectUri + "> " + property + " " + valueStr + " . " +
                        "} }";
            } else {
                updateQuery = org.hascoapi.utils.NameSpaces.getInstance().printSparqlNameSpaceList() +
                        "INSERT DATA { " +
                        "<" + subjectUri + "> " + property + " " + valueStr + " . " +
                        "}";
            }
            
            // Execute the update using UpdateRequest and UpdateProcessor
            org.apache.jena.update.UpdateRequest request = org.apache.jena.update.UpdateFactory.create(updateQuery);
            org.apache.jena.update.UpdateProcessor processor = org.apache.jena.update.UpdateExecutionFactory.createRemote(
                    request, 
                    org.hascoapi.utils.CollectionUtil.getCollectionPath(
                        org.hascoapi.utils.CollectionUtil.Collection.SPARQL_UPDATE));
            processor.execute();
                
        } catch (Exception e) {
            logger.println("[SIR-GEN] WARNING: Failed to add RDF property " + property + ": " + e.getMessage());
        }
    }

    @Override
    public void preprocess() throws Exception {
        // No preprocessing needed
    }

    @Override
    public HADatAcThing createObject(Record rec, int rowNumber, String selector) throws Exception {
        return createSIREntity(rec);
    }

    @Override
    public String getErrorMsg(Exception e) {
        return "Error in SIRObjectGenerator: " + e.getMessage();
    }

    @Override
    public String getTableName() {
        return "SIR Objects";
    }
}

