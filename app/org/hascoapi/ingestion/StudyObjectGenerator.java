package org.hascoapi.ingestion;

import java.lang.String;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.utils.Utils;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.VSTOI;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.HADatAcThing;
import org.hascoapi.entity.pojo.StudyObjectCollection;
import org.hascoapi.entity.pojo.StudyObject;
import org.hascoapi.entity.pojo.Instrument;
import org.hascoapi.entity.pojo.Component;
import org.hascoapi.entity.pojo.ComponentStem;
import org.hascoapi.entity.pojo.ContainerSlot;
import org.hascoapi.entity.pojo.Codebook;
import org.hascoapi.entity.pojo.ResponseOption;
import org.hascoapi.entity.pojo.AnnotationStem;


public class StudyObjectGenerator extends BaseGenerator {

    private static class SirIngestContext {
        String uri;
        String originalId;
        String isMemberOf;
        List<String> scopeUris;
        List<String> timeScopeUris;
        List<String> spaceScopeUris;
        String vstoiType;
    }

    String study_id;
    String file_name;
    String soc_uri;
    String soc_type;
    String soc_scope;
    String soc_timescope;
    String soc_spacescope;
    String soc_reference;
    String grounding_label;
    String domain_reference;
    String time_reference;
    String space_reference;
    String namespace;
    String role;
    private Map<String, StudyObjectCollection> socMap = new HashMap<String, StudyObjectCollection>();
    private List<String> listContent = new ArrayList<String>();
    private Map<String, String> uriMap = new HashMap<String, String>();
    private Map<String, List<String>> mapContent = new HashMap<String, List<String>>();
    private Map<String, String> mapReferences = new HashMap<String, String>();
    private Map<String, SirIngestContext> sirIngestContextByUri = new HashMap<String, SirIngestContext>();

    public StudyObjectGenerator(
            DataFile dataFile, 
            List<String> listContent, 
            Map<String, List<String>> mapContent, 
            Map<String, String> mapReferences, 
            String study_uri, 
            String study_id, 
            String namespace) {
        super(dataFile);
        //System.out.println("We are in StudyObject Generator!");
        //System.out.println("Study URI: " + study_uri);
        this.namespace = namespace;
        file_name = fileName;        
        this.study_id = study_id;
        
        setStudyUri(study_uri);       
        this.listContent = listContent;
        //System.out.println(listContent);
        this.mapContent = mapContent;
        this.mapReferences = mapReferences;

        this.soc_uri = Utils.uriPlainGen(
            "studyobjectcollection", 
            listContent.get(0),
            namespace);

        //System.out.println("oc_uri : " + oc_uri);
        this.soc_type = listContent.get(1);
        //System.out.println("oc_type : " + oc_type);
        this.soc_scope = listContent.get(2);
        //System.out.println("oc_scope : " + oc_scope);
        this.soc_timescope = listContent.get(3);
        //System.out.println("oc_timescope : " + oc_timescope);
        this.soc_spacescope = listContent.get(4);
        //System.out.println("oc_spacescope : " + oc_spacescope);
        this.role = listContent.get(5);
        //System.out.println("role : " + role);
        this.soc_reference = listContent.get(6);
        //listContent.get(7) tmp.add(record.getValueByColumnName("hasSOCReference"));
        this.grounding_label = listContent.get(7);

        uriMap.put("hasco:SubjectGroup", "SBJ-");
        uriMap.put("hasco:SampleCollection", "SPL-");
        uriMap.put("hasco:TimeCollection", "TIME-");
        uriMap.put("hasco:SpaceCollection", "LOC-");
        uriMap.put("hasco:ObjectCollection", "OBJ-");
        
        // VSTOI type mappings for new DSG format
        uriMap.put("hasco:InstrumentCollection", "INS-");
        uriMap.put("hasco:ComponentCollection", "COM-");
        uriMap.put("hasco:ComponentStemCollection", "CSM-");
        uriMap.put("hasco:ContainerSlotCollection", "CTS-");
        uriMap.put("hasco:CodebookCollection", "CBK-");
        uriMap.put("hasco:ResponseOptionCollection", "ROP-");
        uriMap.put("hasco:AnnotationStemCollection", "ASM-");
    }

    @Override
    public void initMapping() {
        mapCol.clear();
        mapCol.put("originalID", "originalID");
        mapCol.put("rdf:type", "rdf:type");
        mapCol.put("scopeID", "scopeID");
        mapCol.put("timeScopeID", "timeScopeID");
        mapCol.put("spaceScopeID", "spaceScopeID");
        mapCol.put("label", "label");
        mapCol.put("rdfs:label", "rdfs:label");
        mapCol.put("comment", "comment");
        mapCol.put("rdfs:comment", "rdfs:comment");
    }

    private String getUri(Record rec) {
        String rdfType = getType(rec);
        if (isVstoiRdfType(rdfType)) {
            String nativeId = rec.getValueByColumnName(mapCol.get("originalID"));
            return buildNativeUriFromIdentifier(nativeId);
        }

        String originalID = rec.getValueByColumnName(mapCol.get("originalID"));
        if (originalID != null) {
            String trimmed = originalID.trim();
            // Preserve explicit URI IDs (SIR URI mode) instead of generating a synthetic URI.
            if (URIUtils.isValidURI(trimmed)) {
                return URIUtils.replacePrefixEx(trimmed);
            }
        }
        // Sanitiza o identificador para uso em URI: trim, colapsa whitespace e troca espaços por underscore
        String localId = "";
        if (originalID != null) {
            localId = originalID.trim().replaceAll("\\s+", " ");
            localId = localId.replace(' ', '_');
        }
        return Utils.uriPlainGen("studyobject", localId, this.namespace, this.soc_reference);
    }

    private boolean isVstoiRdfType(String rdfType) {
        if (rdfType == null || rdfType.trim().isEmpty()) {
            return false;
        }
        String expandedType = URIUtils.replacePrefixEx(rdfType.trim());
        return detectVstoiType(expandedType) != null;
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

    private String normalizeSpreadsheetNumericId(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.matches("^\\d+\\.0+$")) {
            return trimmed.substring(0, trimmed.indexOf('.'));
        }
        return trimmed;
    }

    private String getType(Record rec) {
        return rec.getValueByColumnName(mapCol.get("rdf:type"));
    }

    private String getLabel(Record rec) {
        // Priority 1: Check for explicit label in worksheet (rdfs:label or label column)
        String explicitLabel = rec.getValueByColumnName(mapCol.get("rdfs:label"));
        if (explicitLabel == null || explicitLabel.trim().isEmpty()) {
            explicitLabel = rec.getValueByColumnName(mapCol.get("label"));
        }
        if (explicitLabel != null && !explicitLabel.trim().isEmpty()) {
            return explicitLabel.trim();
        }
        
        // Priority 2: Use originalID as fallback
        String originalID = rec.getValueByColumnName(mapCol.get("originalID"));

        if (URIUtils.isValidURI(originalID)) {
            return URIUtils.getBaseName(originalID);
        }

        // Versão usada no label: substituir espaços simples por underscore
        String labelId = originalID == null ? "" : originalID.replace(' ', '_');

        // Priority 3: Use SOC role label if available
        if (getSoc() != null && getSoc().getRoleLabel() != null && !getSoc().getRoleLabel().equals("")) {
            return getSoc().getRoleLabel() + " " + labelId;
        }

        // Priority 4: Generate label from collection type prefix
        String auxstr = uriMap.get(soc_type);
        if (auxstr == null) {
            auxstr = "";
        } else {
            auxstr = auxstr.replaceAll("-","");
        }

        if (auxstr.contains("SBJ")) {
            return auxstr + " " + labelId;
        }
        return auxstr + " " + labelId + " - " + study_id;
    }

    private String getOriginalID(Record rec) {
        String auxstr = rec.getValueByColumnName(mapCol.get("originalID"));
        //System.out.println("StudyObjectGenerator: getOriginalID(1) = [" + auxstr + "]");
        if (auxstr == null) {
            return "";
        }
        auxstr = auxstr.trim();
        if (auxstr.isEmpty()) {
            return "";
        }
        // Keep URI-valued originalID as-is (trimmed) to preserve SIR identity fidelity.
        if (URIUtils.isValidURI(auxstr)) {
            return auxstr;
        }
        auxstr = auxstr.replaceAll("\\s+","");
        //System.out.println("StudyObjectGenerator: getOriginalID(2) = [" + auxstr + "]");;


    //auxstr = auxstr.replaceAll("(?<=^\\d+)\\.0*$", "");
        //System.out.println("StudyObjectGenerator: getOriginalID(3) = [" + auxstr + "]");
        return auxstr;
    }

    private String getSocUri() {
        return soc_uri;
    }

    
    private StudyObjectCollection getSoc() {
    	if (soc_uri == null || soc_uri.equals("")) {
    		return null;
    	}
    	if (socMap.containsKey(soc_uri)) {
    		return socMap.get(soc_uri);
    	}
    	StudyObjectCollection soc = StudyObjectCollection.find(soc_uri);
    	socMap.put(soc_uri, soc);
    	return soc;
    }
    
    private String getScopeUri(Record rec) {
        if (soc_scope != null && !soc_scope.isEmpty()){
	        if (mapContent.get(soc_scope) != null) {
            	String returnedValue = rec.getValueByColumnName(mapCol.get("scopeID"));
            	if (returnedValue == null) {
            		dataFile.getLogger().println("[WARN] StudyObjectGenerator.getScopeUri(): scopeID is null for SOC [" + soc_uri + "] scopeSOC=[" + soc_scope + "]");
            		return "";
            	}
            	returnedValue = returnedValue.trim();
            	if (returnedValue.isEmpty()) {
            		// no scope assigned if scopeID cell is blank
            		return "";
            	}
            	if (domain_reference == null || domain_reference.isEmpty()) {
            		// best-effort fallback: use referenced SOC's hasSOCReference (index 6)
            		List<String> scopeSocRow = mapContent.get(soc_scope);
            		if (scopeSocRow != null && scopeSocRow.size() > 6 && scopeSocRow.get(6) != null) {
            			domain_reference = scopeSocRow.get(6);
            		}
            	}
	        	// the value returned by getValueByColumnName may be an URI or an original.
	        	if (URIUtils.isValidURI(returnedValue)) {
	        		// if returned value is an URI, this function returns the URI with expanded namespace 
	        		return URIUtils.replacePrefixEx(returnedValue);
	        	} else {
	        		// if returned value is not an URI, this function composes an URI according to SDD convention 
                    return Utils.uriPlainGen("studyobject",
                        normalizeSpreadsheetNumericId(returnedValue),
		                this.namespace,
		                domain_reference);
	        	}
            } else {
                // STO_00001: Missing mapping for soc_scope
                dataFile.getLogger().printExceptionByIdWithArgs("STO_00001", soc_scope);
                return "";
	        }
        } else {
	        return "";
        }
    }

    private String getTimeScopeUri(Record rec) {
        if (soc_timescope != null && soc_timescope.length() > 0){
        	if (mapContent.get(soc_timescope) != null) {
        		//String timeScopeSOCtype = mapContent.get(soc_timescope).get(1);
        		String returnedValue = rec.getValueByColumnName(mapCol.get("timeScopeID"));
        		// the value returned by getValueByColumnName may be ann URI or an original.
        		if (URIUtils.isValidURI(returnedValue)) {
        			// if returned value is ann URI, this function returns the URI with expanded namespace
        			return URIUtils.replacePrefixEx(returnedValue);
        		} else {
        			// if returned value is not ann URI, this function composes ann URI according to SDD convention
                    return Utils.uriPlainGen("studyobject",
                        normalizeSpreadsheetNumericId(rec.getValueByColumnName(mapCol.get("timeScopeID"))),
                        this.namespace,
                        time_reference);
        		}
        	} else {
                // STO_00002: Missing mapContent for soc_timescope
                dataFile.getLogger().printExceptionByIdWithArgs("STO_00002", soc_timescope);
                return "";
        	}
        } else {
            return "";
        }
    }
    
    private String getSpaceScopeUri(Record rec) {
        if (soc_spacescope != null && soc_spacescope.length() > 0){
        	if (mapContent.get(soc_spacescope) != null) {
        		//String spaceScopeSOCtype = mapContent.get(soc_spacescope).get(1);
        		String returnedValue = rec.getValueByColumnName(mapCol.get("spaceScopeID"));
        		// the value returned by getValueByColumnName may be an URI or an original.
        		if (URIUtils.isValidURI(returnedValue)) {
        			// if returned value is an URI, this function returns the URI with expanded namespace
        			return URIUtils.replacePrefixEx(returnedValue);
        		} else {
        			// if returned value is not an URI, this function composes an URI according to SDD convention
                    return Utils.uriPlainGen("studyobject",
                        normalizeSpreadsheetNumericId(rec.getValueByColumnName(mapCol.get("spaceScopeID"))),
                        this.namespace,
                        space_reference);
        		}
        	} else {
                // STO_00003: Missing mapContent for soc_spacescope
                dataFile.getLogger().printExceptionByIdWithArgs("STO_00003", soc_spacescope);
        		return "";
        	}
        } else {
            return "";
        }
    }
    
    public StudyObject createStudyObject(Record record) throws Exception {
        String originalId = getOriginalID(record);
    	if (originalId == null || originalId.isEmpty()) {
    		return null;
    	}

        // Normaliza o tipo RDF para URI completa (se vier como "vstoi:Instrument", vira "http://hadatac.org/ont/vstoi#Instrument")
        String rdfType = getType(record);
        if (rdfType != null && !rdfType.isEmpty()) {
            rdfType = URIUtils.replacePrefixEx(rdfType);
        }

    	StudyObject obj = new StudyObject(
            getUri(record), 
            rdfType,  // Tipo normalizado
            URIUtils.replacePrefixEx(HASCO.STUDY_OBJECT),
        			originalId, 
            getLabel(record), 
			getSocUri(), 
            getLabel(record),
            this.dataFile.getHasSIRManagerEmail());  // hasSIRManagerEmail
        obj.setRoleUri(URIUtils.replacePrefixEx(role));
        
        // Set comment from rdfs:comment or comment column if present
        String comment = record.getValueByColumnName(mapCol.get("rdfs:comment"));
        if (comment == null || comment.trim().isEmpty()) {
            comment = record.getValueByColumnName(mapCol.get("comment"));
        }
        if (comment != null && !comment.trim().isEmpty()) {
            obj.setComment(comment.trim());
        }

        //System.out.println("Domain: [" + getScopeUri(record) + "]  Time: [" + getTimeScopeUri(record) + "] Space: [" + getSpaceScopeUri(record) + "]");

        domain_reference = mapReferences.get(listContent.get(2));
        time_reference = mapReferences.get(listContent.get(3));
        space_reference = mapReferences.get(listContent.get(4)); 
        
        //System.out.println("Domain soc: [" + domain_reference + "]  Time soc: [" + time_reference + "] Space soc: [" + space_reference + "]");

        String scopeUri = getScopeUri(record);
        if (scopeUri != null && !scopeUri.isEmpty()) {
            obj.addScopeUri(scopeUri);
        }
        String timeScopeUri = getTimeScopeUri(record);
        if (timeScopeUri != null && !timeScopeUri.isEmpty()) {
            obj.addTimeScopeUri(timeScopeUri);
        }
        String spaceScopeUri = getSpaceScopeUri(record);
        if (spaceScopeUri != null && !spaceScopeUri.isEmpty()) {
            obj.addSpaceScopeUri(spaceScopeUri);
        }
        
        return obj;
    }

    @Override
    public HADatAcThing createObject(Record rec, int rowNumber, String selector) throws Exception {
        StudyObject studyObject = createStudyObject(rec);
        
        // Se o StudyObject foi criado com sucesso, verifica se é um tipo vstoi e cria a entidade correspondente
        if (studyObject != null && studyObject.getTypeUri() != null) {
            // Detecta se é tipo VSTOI
            String vstoiType = detectVstoiType(studyObject.getTypeUri());
            
            if (vstoiType != null) {
                // ✅ SINGLE LAYER: Se for SIR element, criar APENAS a entidade VSTOI, NÃO o StudyObject
                // 
                // Abandonamos completamente o dual layer:
                // - Se for SIR element → criar APENAS como SIR element (Instrument, Component, etc.)
                // - Se NÃO for SIR element → criar como StudyObject normal
                //
                dataFile.getLogger().println("[SIR ELEMENT DETECTED] Type: " + vstoiType + " - Creating ONLY as SIR element, NOT as StudyObject");

                if (isObjectSyntheticId(studyObject.getUri()) || isObjectSyntheticId(studyObject.getOriginalId())) {
                    throw new IllegalStateException("Blocked SIR ingest with synthetic OBJ identifier: " + studyObject.getUri());
                }
                
                // Criar diretamente a entidade VSTOI e retornar ela (NÃO o StudyObject)
                return createVstoiEntityDirectly(studyObject, vstoiType);
            }
        }
        
        // Se não for SIR element, retorna o StudyObject normal
        return studyObject;
    }
    
    /**
     * Cria DIRETAMENTE uma entidade VSTOI (SIR element) SEM criar StudyObject.
     * Implementa a abordagem single-layer:
     * - Se for SIR element → criar APENAS como SIR element
     * - Não cria StudyObject, não cria dual-layer
     * 
     * @param studyObject Objeto temporário com dados do CSV (usado apenas para transferir propriedades)
     * @param vstoiType Tipo VSTOI detectado
     * @return A entidade VSTOI criada
     */
    private HADatAcThing createVstoiEntityDirectly(StudyObject studyObject, String vstoiType) {
        try {
            HADatAcThing vstoiEntity = null;
            
            if (VSTOI.INSTRUMENT.equals(vstoiType)) {
                vstoiEntity = createInstrumentDirectly(studyObject);
            } else if (VSTOI.COMPONENT.equals(vstoiType)) {
                vstoiEntity = createComponentDirectly(studyObject);
            } else if (VSTOI.COMPONENT_STEM.equals(vstoiType)) {
                vstoiEntity = createComponentStemDirectly(studyObject);
            } else if (VSTOI.CONTAINER_SLOT.equals(vstoiType)) {
                vstoiEntity = createContainerSlotDirectly(studyObject);
            } else if (VSTOI.CODEBOOK.equals(vstoiType)) {
                vstoiEntity = createCodebookDirectly(studyObject);
            } else if (VSTOI.RESPONSE_OPTION.equals(vstoiType)) {
                vstoiEntity = createResponseOptionDirectly(studyObject);
            } else if (VSTOI.ANNOTATION_STEM.equals(vstoiType)) {
                vstoiEntity = createAnnotationStemDirectly(studyObject);
            }

            if (vstoiEntity != null) {
                cacheSirIngestContext(studyObject, vstoiType);
            }
            
            return vstoiEntity;
        } catch (Exception e) {
            dataFile.getLogger().println("Error: Failed to create VSTOI entity directly for " + studyObject.getUri() + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Cria um Instrument DIRETAMENTE (sem StudyObject)
     */
    private Instrument createInstrumentDirectly(StudyObject so) {
        Instrument instrument = new Instrument();
        
        instrument.setUri(so.getUri());
        instrument.setTypeUri(so.getTypeUri());
        instrument.setHascoTypeUri(VSTOI.INSTRUMENT);
        instrument.setLabel(so.getLabel());
        instrument.setComment(so.getComment());
        instrument.setNamedGraph(getNamedGraphUri());
        instrument.setHasSIRManagerEmail(so.getHasSIRManagerEmail());
        
        dataFile.getLogger().println("  [SINGLE-LAYER] Created Instrument: " + instrument.getLabel() + " (URI: " + instrument.getUri() + ")");
        
        return instrument;
    }
    
    /**
     * Cria um Component DIRETAMENTE (sem StudyObject)
     */
    private Component createComponentDirectly(StudyObject so) {
        Component component = new Component();
        
        component.setUri(so.getUri());
        component.setTypeUri(so.getTypeUri());
        component.setHascoTypeUri(VSTOI.COMPONENT);
        component.setLabel(so.getLabel());
        component.setComment(so.getComment());
        component.setNamedGraph(getNamedGraphUri());
        component.setHasSIRManagerEmail(so.getHasSIRManagerEmail());

        dataFile.getLogger().println("  [SINGLE-LAYER] Created Component: " + component.getLabel() + " (URI: " + component.getUri() + ")");
        
        return component;
    }
    
    /**
     * Cria um ComponentStem DIRETAMENTE (sem StudyObject)
     */
    private ComponentStem createComponentStemDirectly(StudyObject so) {
        ComponentStem stem = new ComponentStem();
        
        stem.setUri(so.getUri());
        stem.setTypeUri(so.getTypeUri());
        stem.setHascoTypeUri(VSTOI.COMPONENT_STEM);
        stem.setLabel(so.getLabel());
        stem.setComment(so.getComment());
        stem.setNamedGraph(getNamedGraphUri());
        stem.setHasSIRManagerEmail(so.getHasSIRManagerEmail());
        
        dataFile.getLogger().println("  [SINGLE-LAYER] Created ComponentStem: " + stem.getLabel() + " (URI: " + stem.getUri() + ")");
        
        return stem;
    }
    
    /**
     * Cria um ContainerSlot DIRETAMENTE (sem StudyObject)
     */
    private ContainerSlot createContainerSlotDirectly(StudyObject so) {
        ContainerSlot slot = new ContainerSlot();
        
        slot.setUri(so.getUri());
        slot.setTypeUri(so.getTypeUri());
        slot.setHascoTypeUri(VSTOI.CONTAINER_SLOT);
        slot.setLabel(so.getLabel());
        slot.setComment(so.getComment());
        slot.setNamedGraph(getNamedGraphUri());
        
        dataFile.getLogger().println("  [SINGLE-LAYER] Created ContainerSlot: " + slot.getLabel() + " (URI: " + slot.getUri() + ")");
        
        return slot;
    }
    
    /**
     * Cria um Codebook DIRETAMENTE (sem StudyObject)
     */
    private Codebook createCodebookDirectly(StudyObject so) {
        Codebook codebook = new Codebook();
        
        codebook.setUri(so.getUri());
        codebook.setTypeUri(so.getTypeUri());
        codebook.setHascoTypeUri(VSTOI.CODEBOOK);
        codebook.setLabel(so.getLabel());
        codebook.setComment(so.getComment());
        codebook.setNamedGraph(getNamedGraphUri());
        codebook.setHasSIRManagerEmail(so.getHasSIRManagerEmail());
        
        dataFile.getLogger().println("  [SINGLE-LAYER] Created Codebook: " + codebook.getLabel() + " (URI: " + codebook.getUri() + ")");
        
        return codebook;
    }
    
    /**
     * Cria um ResponseOption DIRETAMENTE (sem StudyObject)
     */
    private ResponseOption createResponseOptionDirectly(StudyObject so) {
        ResponseOption option = new ResponseOption();
        
        option.setUri(so.getUri());
        option.setTypeUri(so.getTypeUri());
        option.setHascoTypeUri(VSTOI.RESPONSE_OPTION);
        option.setLabel(so.getLabel());
        option.setComment(so.getComment());
        option.setNamedGraph(getNamedGraphUri());
        option.setHasSIRManagerEmail(so.getHasSIRManagerEmail());
        
        dataFile.getLogger().println("  [SINGLE-LAYER] Created ResponseOption: " + option.getLabel() + " (URI: " + option.getUri() + ")");
        
        return option;
    }
    
    /**
     * Cria um AnnotationStem (ComponentStem) DIRETAMENTE (sem StudyObject)
     */
    private ComponentStem createAnnotationStemDirectly(StudyObject so) {
        ComponentStem stem = new ComponentStem();
        
        stem.setUri(so.getUri());
        stem.setTypeUri(so.getTypeUri());
        stem.setHascoTypeUri(VSTOI.ANNOTATION_STEM);
        stem.setLabel(so.getLabel());
        stem.setComment(so.getComment());
        stem.setNamedGraph(getNamedGraphUri());
        stem.setHasSIRManagerEmail(so.getHasSIRManagerEmail());
        
        dataFile.getLogger().println("  [SINGLE-LAYER] Created AnnotationStem: " + stem.getLabel() + " (URI: " + stem.getUri() + ")");
        
        return stem;
    }
    
    /**
     * Detecta se um tipo RDF é vstoi e retorna o tipo base
     */
    private String detectVstoiType(String typeUri) {
        // Verificação direta
        if (VSTOI.INSTRUMENT.equals(typeUri)) return VSTOI.INSTRUMENT;
        if (VSTOI.COMPONENT.equals(typeUri)) return VSTOI.COMPONENT;
        if (VSTOI.COMPONENT_STEM.equals(typeUri)) return VSTOI.COMPONENT_STEM;
        if (VSTOI.CONTAINER_SLOT.equals(typeUri)) return VSTOI.CONTAINER_SLOT;
        if (VSTOI.CODEBOOK.equals(typeUri)) return VSTOI.CODEBOOK;
        if (VSTOI.RESPONSE_OPTION.equals(typeUri)) return VSTOI.RESPONSE_OPTION;
        if (VSTOI.ANNOTATION_STEM.equals(typeUri)) return VSTOI.ANNOTATION_STEM;
        
        // Verificação por substring (subclasses)
        if (typeUri.contains("Detector")) return VSTOI.COMPONENT;
        if (typeUri.contains("Questionnaire")) return VSTOI.INSTRUMENT;
        if (typeUri.contains("Codebook")) return VSTOI.CODEBOOK;
        
        return null; // Não é tipo VSTOI
    }

    private void cacheSirIngestContext(StudyObject studyObject, String vstoiType) {
        if (studyObject == null || studyObject.getUri() == null || studyObject.getUri().trim().isEmpty()) {
            return;
        }
        SirIngestContext ctx = new SirIngestContext();
        ctx.uri = studyObject.getUri();
        ctx.originalId = studyObject.getOriginalId();
        ctx.isMemberOf = studyObject.getIsMemberOfUri();
        ctx.scopeUris = new ArrayList<String>(studyObject.getScopeUris());
        ctx.timeScopeUris = new ArrayList<String>(studyObject.getTimeScopeUris());
        ctx.spaceScopeUris = new ArrayList<String>(studyObject.getSpaceScopeUris());
        ctx.vstoiType = vstoiType;
        sirIngestContextByUri.put(ctx.uri, ctx);
    }

    private void addRdfProperty(String subjectUri, String property, String value, boolean isUri) {
        if (subjectUri == null || subjectUri.trim().isEmpty() || value == null || value.trim().isEmpty()) {
            return;
        }
        String cleanValue = value.trim().replace("\"", "\\\"");
        String objectValue = isUri ? "<" + URIUtils.replacePrefixEx(cleanValue) + ">" : "\"" + cleanValue + "\"";
        String graph = getNamedGraphUri();
        String update = NameSpaces.getInstance().printSparqlNameSpaceList();
        if (graph != null && !graph.trim().isEmpty()) {
            update += "INSERT DATA { GRAPH <" + graph + "> { <" + subjectUri + "> " + property + " " + objectValue + " . } }";
        } else {
            update += "INSERT DATA { <" + subjectUri + "> " + property + " " + objectValue + " . }";
        }
        UpdateRequest request = UpdateFactory.create(update);
        UpdateProcessor processor = UpdateExecutionFactory.createRemote(
                request,
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_UPDATE));
        processor.execute();
    }

    private boolean isHierarchyType(String vstoiType) {
        return VSTOI.INSTRUMENT.equals(vstoiType) || VSTOI.COMPONENT_STEM.equals(vstoiType);
    }

    @Override
    public Map<String, Object> createRow(Record rec, int rowNumber) throws Exception {
        String originalId = getOriginalID(rec);
        if (originalId.length() > 0) {
            Map<String, Object> row = new HashMap<String, Object>();
            row.put("hasURI", getUri(rec));
            return row;
        }
        
        return null;
    }

    @Override
    public void preprocess() throws Exception {}

    @Override
    public void postprocess() throws Exception {
        // NÃO fazer nada aqui com a abordagem single-layer
    }
    
    @Override
    public boolean commitObjectsToTripleStore(List<HADatAcThing> objects) {
        dataFile.getLogger().println("[COMMIT] Starting commitObjectsToTripleStore with " + objects.size() + " objects");
        
        // Com a abordagem single-layer, apenas salvamos os objetos normalmente
        // Não há necessidade de enrichment pois os SIR elements já foram criados diretamente
        boolean result = super.commitObjectsToTripleStore(objects);

        if (result) {
            for (SirIngestContext ctx : sirIngestContextByUri.values()) {
                if (isObjectSyntheticId(ctx.uri) || isObjectSyntheticId(ctx.originalId)) {
                    dataFile.getLogger().printException("SIR consistency check failed: synthetic OBJ URI/ID detected for " + ctx.uri);
                    result = false;
                    continue;
                }
                addRdfProperty(ctx.uri, "hasco:originalID", ctx.originalId, false);
                addRdfProperty(ctx.uri, "hasco:isMemberOf", ctx.isMemberOf, true);

                if (ctx.scopeUris != null) {
                    for (String scopeUri : ctx.scopeUris) {
                        addRdfProperty(ctx.uri, "hasco:hasScope", scopeUri, true);
                    }
                }
                if (ctx.timeScopeUris != null) {
                    for (String timeScopeUri : ctx.timeScopeUris) {
                        addRdfProperty(ctx.uri, "hasco:hasTimeScope", timeScopeUri, true);
                    }
                }
                if (ctx.spaceScopeUris != null) {
                    for (String spaceScopeUri : ctx.spaceScopeUris) {
                        addRdfProperty(ctx.uri, "hasco:hasSpaceScope", spaceScopeUri, true);
                    }
                }

                if (isHierarchyType(ctx.vstoiType) && ctx.scopeUris != null && !ctx.scopeUris.isEmpty()) {
                    addRdfProperty(ctx.uri, "rdfs:subClassOf", ctx.scopeUris.get(0), true);
                    if (ctx.scopeUris.size() > 1) {
                        dataFile.getLogger().printWarning("SIR hierarchy consistency: multiple scopes found for " + ctx.uri + ", using first scope as rdfs:subClassOf");
                    }
                }
            }
        }
        
        dataFile.getLogger().println("[COMMIT] Completed with result: " + result);
        
        return result;
    }

    @Override
    public String getTableName() {
        return "StudyObject";
    }

    @Override
    public String getErrorMsg(Exception e) {
        return "Error in StudyObjectGenerator: " + e.getMessage();
    }
}
