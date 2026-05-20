package org.hascoapi.ingestion;

import java.lang.String;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
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
import org.hascoapi.utils.Utils;
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
        mapCol.put("typeUri", "type");
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
        if (uriValue == null || uriValue.isEmpty()) {
            // Auto-generate URI from originalID
            String originalId = getOriginalID(rec);
            if (originalId != null && !originalId.isEmpty()) {
                return Utils.uriPlainGen("studyobject", originalId, namespace);
            }
            return null;
        }
        return Utils.uriPlainGen("studyobject", uriValue, namespace);
    }

    private String getOriginalID(Record rec) {
        return rec.getValueByColumnName(mapCol.get("originalID"));
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

        if (uri == null || uri.isEmpty()) {
            logger.println("[SIR-GEN] Skipping record - no URI");
            return null;
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
        if ("Instrument".equals(sirType)) {
            Instrument instrument = new Instrument();
            instrument.setUri(uri);
            instrument.setTypeUri(URIUtils.replacePrefixEx(typeUri));
            instrument.setHascoTypeUri(VSTOI.INSTRUMENT);
            instrument.setLabel(label);
            instrument.setComment(comment);
            instrument.setOriginalId(originalID);
            instrument.setIsMemberOf(isMemberOf);
            instrument.setNamedGraph(getNamedGraphUri());
            instrument.setHasSIRManagerEmail(this.dataFile.getHasSIRManagerEmail());
            entity = instrument;

        } else if ("Component".equals(sirType)) {
            Component component = new Component();
            component.setUri(uri);
            component.setTypeUri(URIUtils.replacePrefixEx(typeUri));
            component.setHascoTypeUri(VSTOI.COMPONENT);
            component.setLabel(label);
            component.setComment(comment);
            component.setOriginalId(originalID);
            component.setIsMemberOf(isMemberOf);
            component.setNamedGraph(getNamedGraphUri());
            component.setHasSIRManagerEmail(this.dataFile.getHasSIRManagerEmail());
            entity = component;

        } else if ("ComponentStem".equals(sirType)) {
            ComponentStem componentStem = new ComponentStem();
            componentStem.setUri(uri);
            componentStem.setTypeUri(URIUtils.replacePrefixEx(typeUri));
            componentStem.setHascoTypeUri(VSTOI.COMPONENT_STEM);
            componentStem.setLabel(label);
            componentStem.setComment(comment);
            componentStem.setNamedGraph(getNamedGraphUri());
            componentStem.setHasSIRManagerEmail(this.dataFile.getHasSIRManagerEmail());
            entity = componentStem;

        } else if ("ContainerSlot".equals(sirType)) {
            ContainerSlot containerSlot = new ContainerSlot();
            containerSlot.setUri(uri);
            containerSlot.setTypeUri(URIUtils.replacePrefixEx(typeUri));
            containerSlot.setHascoTypeUri(VSTOI.CONTAINER_SLOT);
            containerSlot.setLabel(label);
            containerSlot.setComment(comment);
            containerSlot.setOriginalId(originalID);
            containerSlot.setIsMemberOf(isMemberOf);
            containerSlot.setNamedGraph(getNamedGraphUri());
            containerSlot.setHasSIRManagerEmail(this.dataFile.getHasSIRManagerEmail());
            entity = containerSlot;

        } else if ("Codebook".equals(sirType)) {
            Codebook codebook = new Codebook();
            codebook.setUri(uri);
            codebook.setTypeUri(URIUtils.replacePrefixEx(typeUri));
            codebook.setHascoTypeUri(VSTOI.CODEBOOK);
            codebook.setLabel(label);
            codebook.setComment(comment);
            codebook.setOriginalId(originalID);
            codebook.setIsMemberOf(isMemberOf);
            codebook.setNamedGraph(getNamedGraphUri());
            codebook.setHasSIRManagerEmail(this.dataFile.getHasSIRManagerEmail());
            entity = codebook;

        } else if ("ResponseOption".equals(sirType)) {
            ResponseOption responseOption = new ResponseOption();
            responseOption.setUri(uri);
            responseOption.setTypeUri(URIUtils.replacePrefixEx(typeUri));
            responseOption.setHascoTypeUri(VSTOI.RESPONSE_OPTION);
            responseOption.setLabel(label);
            responseOption.setComment(comment);
            responseOption.setOriginalId(originalID);
            responseOption.setIsMemberOf(isMemberOf);
            responseOption.setNamedGraph(getNamedGraphUri());
            responseOption.setHasSIRManagerEmail(this.dataFile.getHasSIRManagerEmail());
            entity = responseOption;

        } else if ("AnnotationStem".equals(sirType)) {
            AnnotationStem annotationStem = new AnnotationStem();
            annotationStem.setUri(uri);
            annotationStem.setTypeUri(URIUtils.replacePrefixEx(typeUri));
            annotationStem.setHascoTypeUri(VSTOI.ANNOTATION_STEM);
            annotationStem.setLabel(label);
            annotationStem.setComment(comment);
            annotationStem.setOriginalId(originalID);
            annotationStem.setIsMemberOf(isMemberOf);
            annotationStem.setNamedGraph(getNamedGraphUri());
            annotationStem.setHasSIRManagerEmail(this.dataFile.getHasSIRManagerEmail());
            entity = annotationStem;
        }

        logger.println("[SIR-GEN] ✅ Created " + sirType + ": " + label);
        return entity;
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

