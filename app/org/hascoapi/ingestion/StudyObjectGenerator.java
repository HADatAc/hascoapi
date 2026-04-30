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
    
    // Lista de StudyObjects que precisam de camada VSTOI adicional
    // Salvamos essas entidades VSTOI no postprocess(), DEPOIS do BaseGenerator salvar os StudyObjects
    private List<StudyObject> vstoiObjectsToEnrich = new ArrayList<>();

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
        String originalID = rec.getValueByColumnName(mapCol.get("originalID"));
        // Sanitiza o identificador para uso em URI: trim, colapsa whitespace e troca espaços por underscore
        String localId = "";
        if (originalID != null) {
            localId = originalID.trim().replaceAll("\\s+", " ");
            localId = localId.replace(' ', '_');
        }
        return Utils.uriPlainGen("studyobject", localId, this.namespace, this.soc_reference);
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
        if (URIUtils.isValidURI(auxstr)) {
            return "";
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
		                returnedValue.replaceAll("(?<=^\\d+)\\.0*$", ""),
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
                        rec.getValueByColumnName(mapCol.get("timeScopeID")).replaceAll("(?<=^\\d+)\\.0*$", ""),
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
                        rec.getValueByColumnName(mapCol.get("spaceScopeID")).replaceAll("(?<=^\\d+)\\.0*$", ""),
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
    	if (getOriginalID(record) == null || getOriginalID(record).isEmpty()) {
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
			getOriginalID(record), 
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
                // É um tipo VSTOI, então precisamos criar AMBAS as camadas com o MESMO URI:
                // 
                // CAMADA 1 - StudyObject (hasco:hascoType = hasco:StudyObject)
                //   - Aparece em "Object Collections" 
                //   - DAs conseguem encontrar via originalID
                //   - Query genérica de StudyObjects retorna esses objetos
                //
                // CAMADA 2 - Instrument/Component (hasco:hascoType = vstoi:Instrument)
                //   - Aparece em "/sir/select/instrument"
                //   - Query específica de Instruments retorna esses objetos
                //   - Tem propriedades específicas de Instrument
                //
                // IMPORTANTE: O mesmo URI terá DOIS valores para hasco:hascoType:
                //   pmsr:INS123 hasco:hascoType hasco:StudyObject .
                //   pmsr:INS123 hasco:hascoType vstoi:Instrument .
                //
                // SOLUÇÃO: Armazenamos o StudyObject para enriquecer no postprocess()
                // Isso garante que o BaseGenerator salve o StudyObject primeiro,
                // e DEPOIS adicionamos as propriedades do Instrument sem deletar
                vstoiObjectsToEnrich.add(studyObject);
            }
        }
        
        // Retorna o StudyObject para o BaseGenerator salvar e contar
        return studyObject;
    }
    
    /**
     * Cria entidade vstoi (Instrument, Component, ComponentStem, ContainerSlot) 
     * se o StudyObject tiver um tipo vstoi
     * @return A entidade VSTOI criada, ou null se não for tipo VSTOI
     */
    private HADatAcThing createVstoiEntityIfApplicable(StudyObject studyObject) {
        String typeUri = studyObject.getTypeUri();
        
        if (typeUri == null || typeUri.isEmpty()) {
            return null;
        }
        
        // Detecta tipo vstoi
        String vstoiType = detectVstoiType(typeUri);
        
        if (vstoiType == null) {
            return null; // Não é tipo vstoi
        }
        
        try {
            if (VSTOI.INSTRUMENT.equals(vstoiType)) {
                return createInstrumentFromStudyObject(studyObject);
            } else if (VSTOI.COMPONENT.equals(vstoiType)) {
                return createComponentFromStudyObject(studyObject);
            } else if (VSTOI.COMPONENT_STEM.equals(vstoiType)) {
                return createComponentStemFromStudyObject(studyObject);
            } else if (VSTOI.CONTAINER_SLOT.equals(vstoiType)) {
                return createContainerSlotFromStudyObject(studyObject);
            } else if (VSTOI.CODEBOOK.equals(vstoiType)) {
                return createCodebookFromStudyObject(studyObject);
            } else if (VSTOI.RESPONSE_OPTION.equals(vstoiType)) {
                return createResponseOptionFromStudyObject(studyObject);
            } else if (VSTOI.ANNOTATION_STEM.equals(vstoiType)) {
                return createAnnotationStemFromStudyObject(studyObject);
            }
        } catch (Exception e) {
            dataFile.getLogger().println("Warning: Failed to create vstoi entity for " + studyObject.getUri() + ": " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Gera uma URI separada para a instância VSTOI a partir da URI do StudyObject
     * Exemplo:
     *   pmsr:OBJ_instrumentcollection_INS1739301009974715
     *   -> pmsr:INST-INS1739301009974715
     * 
     * @param studyObjectUri URI do StudyObject
     * @param prefix Prefixo do tipo VSTOI (INST, COMP, CSTEM, etc.)
     * @return URI para a instância VSTOI
     */
    private String generateVSTOIUri(String studyObjectUri, String prefix) {
        String uri = studyObjectUri;
        
        if (uri.contains("OBJ_")) {
            // Extrair a parte após OBJ_
            String suffix = uri.substring(uri.indexOf("OBJ_") + 4);
            
            // Remover o prefixo de coleção (instrumentcollection_, componentcollection_, etc.)
            if (suffix.contains("_")) {
                suffix = suffix.substring(suffix.indexOf("_") + 1);
            }
            
            // Construir nova URI
            String namespace = uri.substring(0, uri.indexOf("OBJ_"));
            uri = namespace + prefix + "-" + suffix;
        }
        
        return uri;
    }
    
    /**
     * Adiciona um link VSTOI no StudyObject apontando para a instância especializada
     * Exemplo: <studyObject> vstoi:hasInstrument <instrumentInstance>
     * 
     * @param studyObjectUri URI do StudyObject
     * @param vstoiInstanceUri URI da instância VSTOI
     * @param property Propriedade de link (vstoi:hasInstrument, vstoi:hasComponent, etc.)
     */
    private void addVSTOILinkToStudyObject(String studyObjectUri, String vstoiInstanceUri, String property) {
        try {
            String insert = NameSpaces.getInstance().printSparqlNameSpaceList();
            insert += "INSERT DATA { \n";
            insert += "  GRAPH <" + getNamedGraphUri() + "> { \n";
            insert += "    <" + studyObjectUri + "> " + property + " <" + vstoiInstanceUri + "> . \n";
            insert += "  } \n";
            insert += "}";
            
            UpdateRequest request = UpdateFactory.create(insert);
            UpdateProcessor processor = UpdateExecutionFactory.createRemote(
                    request, CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_UPDATE));
            processor.execute();
            
            dataFile.getLogger().println("    [VSTOI-LINK] Added " + property + " link to VSTOI instance");
        } catch (Exception e) {
            dataFile.getLogger().printWarning("    [ERROR] Failed to add VSTOI link: " + e.getMessage());
        }
    }
    
    /**
     * Cria e salva entidade VSTOI SEM deletar o StudyObject existente.
     * AGORA cria uma instância VSTOI com URI SEPARADA e adiciona link no StudyObject.
     * 
     * Arquitetura Dual-Layer:
     * 1. StudyObject (URI original) - camada base para DAs
     * 2. VSTOI Instance (URI gerada) - camada especializada com propriedades específicas
     * 3. Link: StudyObject --vstoi:has[Type]--> VSTOI Instance
     */
    private void createVstoiEntityWithoutDeletingStudyObject(StudyObject studyObject) {
        String typeUri = studyObject.getTypeUri();
        
        if (typeUri == null || typeUri.isEmpty()) {
            return;
        }
        
        // Detecta tipo vstoi
        String vstoiType = detectVstoiType(typeUri);
        
        if (vstoiType == null) {
            return; // Não é tipo vstoi
        }
        
        try {
            if (VSTOI.INSTRUMENT.equals(vstoiType)) {
                createInstrumentFromStudyObjectWithoutDelete(studyObject);
            } else if (VSTOI.COMPONENT.equals(vstoiType)) {
                createComponentFromStudyObjectWithoutDelete(studyObject);
            } else if (VSTOI.COMPONENT_STEM.equals(vstoiType)) {
                createComponentStemFromStudyObjectWithoutDelete(studyObject);
            } else if (VSTOI.CONTAINER_SLOT.equals(vstoiType)) {
                createContainerSlotFromStudyObjectWithoutDelete(studyObject);
            } else if (VSTOI.CODEBOOK.equals(vstoiType)) {
                createCodebookFromStudyObjectWithoutDelete(studyObject);
            } else if (VSTOI.RESPONSE_OPTION.equals(vstoiType)) {
                createResponseOptionFromStudyObjectWithoutDelete(studyObject);
            } else if (VSTOI.ANNOTATION_STEM.equals(vstoiType)) {
                createAnnotationStemFromStudyObjectWithoutDelete(studyObject);
            }
        } catch (Exception e) {
            dataFile.getLogger().println("Warning: Failed to create vstoi entity for " + studyObject.getUri() + ": " + e.getMessage());
        }
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
        if (typeUri.contains("ComponentStem")) return VSTOI.COMPONENT_STEM;
        if (typeUri.contains("ContainerSlot")) return VSTOI.CONTAINER_SLOT;
        if (typeUri.contains("Questionnaire")) return VSTOI.INSTRUMENT;
        if (typeUri.contains("PhysicalInstrument")) return VSTOI.INSTRUMENT;
        if (typeUri.contains("SimulationModel")) return VSTOI.INSTRUMENT;
        if (typeUri.contains("Codebook")) return VSTOI.CODEBOOK;
        if (typeUri.contains("ResponseOption")) return VSTOI.RESPONSE_OPTION;
        if (typeUri.contains("AnnotationStem")) return VSTOI.ANNOTATION_STEM;
        
        return null;
    }
    
    /**
     * Cria um Instrument a partir de um StudyObject
     * @return O Instrument criado
     */
    private Instrument createInstrumentFromStudyObject(StudyObject so) {
        Instrument instrument = new Instrument();
        
        instrument.setUri(so.getUri());
        instrument.setTypeUri(so.getTypeUri());
        instrument.setHascoTypeUri(VSTOI.INSTRUMENT);
        instrument.setLabel(so.getLabel());
        instrument.setComment(so.getComment());
        instrument.setNamedGraph(getNamedGraphUri());
        instrument.setHasSIRManagerEmail(so.getHasSIRManagerEmail());
        
        // DEBUG: Log what we're about to save
        dataFile.getLogger().println("  [DEBUG] Creating Instrument:");
        dataFile.getLogger().println("    - URI: " + instrument.getUri());
        dataFile.getLogger().println("    - TypeUri (rdf:type): " + instrument.getTypeUri());
        dataFile.getLogger().println("    - HascoTypeUri (hasco:hascoType): " + instrument.getHascoTypeUri());
        dataFile.getLogger().println("    - VSTOI.INSTRUMENT constant: " + VSTOI.INSTRUMENT);
        dataFile.getLogger().println("    - Label: " + instrument.getLabel());
        dataFile.getLogger().println("    - ManagerEmail: " + instrument.getHasSIRManagerEmail());
        
        // Salva imediatamente
        instrument.save();
        
        dataFile.getLogger().println("  Created Instrument: " + instrument.getLabel());
        
        return instrument;
    }
    
    /**
     * Cria um Instrument COM URI SEPARADA sem deletar o StudyObject existente.
     * Implementa arquitetura dual-layer verdadeira:
     * 1. StudyObject mantém sua URI original
     * 2. Instrument criado com URI diferente (INST-xxx)
     * 3. Link vstoi:hasInstrument conecta os dois
     */
    private void createInstrumentFromStudyObjectWithoutDelete(StudyObject so) {
        // Gerar URI separada para a instância VSTOI
        String vstoiUri = generateVSTOIUri(so.getUri(), "INST");
        
        Instrument instrument = new Instrument();
        instrument.setUri(vstoiUri);  // ✅ URI DIFERENTE!
        instrument.setTypeUri(VSTOI.INSTRUMENT);
        instrument.setHascoTypeUri(VSTOI.INSTRUMENT);
        instrument.setLabel(so.getLabel());
        instrument.setComment(so.getComment());
        instrument.setNamedGraph(getNamedGraphUri());
        instrument.setHasSIRManagerEmail(so.getHasSIRManagerEmail());
        
        // Salvar a instância VSTOI
        instrument.saveToTripleStore(true, false);
        
        // Adicionar link no StudyObject
        addVSTOILinkToStudyObject(so.getUri(), vstoiUri, "vstoi:hasInstrument");
        
        dataFile.getLogger().println("  Created Instrument (dual-layer): " + instrument.getLabel());
        dataFile.getLogger().println("    StudyObject URI: " + so.getUri());
        dataFile.getLogger().println("    VSTOI Instance URI: " + vstoiUri);
    }
    
    /**
     * Cria um Component a partir de um StudyObject
     * @return O Component criado
     */
    private Component createComponentFromStudyObject(StudyObject so) {
        Component component = new Component();
        
        component.setUri(so.getUri());
        component.setTypeUri(so.getTypeUri());
        component.setHascoTypeUri(VSTOI.COMPONENT);
        component.setLabel(so.getLabel());
        component.setComment(so.getComment());
        component.setNamedGraph(getNamedGraphUri());
        component.setHasSIRManagerEmail(so.getHasSIRManagerEmail());
        
        // Salva imediatamente
        component.save();
        
        dataFile.getLogger().println("  Created Component: " + component.getLabel());
        
        return component;
    }
    
    /**
     * Cria um Component COM URI SEPARADA sem deletar o StudyObject existente
     */
    private void createComponentFromStudyObjectWithoutDelete(StudyObject so) {
        String vstoiUri = generateVSTOIUri(so.getUri(), "COMP");
        
        Component component = new Component();
        component.setUri(vstoiUri);  // ✅ URI DIFERENTE!
        component.setTypeUri(VSTOI.COMPONENT);
        component.setHascoTypeUri(VSTOI.COMPONENT);
        component.setLabel(so.getLabel());
        component.setComment(so.getComment());
        component.setNamedGraph(getNamedGraphUri());
        component.setHasSIRManagerEmail(so.getHasSIRManagerEmail());
        
        component.saveToTripleStore(true, false);
        addVSTOILinkToStudyObject(so.getUri(), vstoiUri, "vstoi:hasComponent");
        
        dataFile.getLogger().println("  Created Component (dual-layer): " + component.getLabel());
    }
    
    /**
     * Cria um ComponentStem a partir de um StudyObject
     * @return O ComponentStem criado
     */
    private ComponentStem createComponentStemFromStudyObject(StudyObject so) {
        ComponentStem stem = new ComponentStem();
        
        stem.setUri(so.getUri());
        stem.setTypeUri(so.getTypeUri());
        stem.setHascoTypeUri(VSTOI.COMPONENT_STEM);
        stem.setLabel(so.getLabel());
        stem.setComment(so.getComment());
        stem.setNamedGraph(getNamedGraphUri());
        stem.setHasSIRManagerEmail(so.getHasSIRManagerEmail());
        
        // Salva imediatamente
        stem.save();
        
        dataFile.getLogger().println("  Created ComponentStem: " + stem.getLabel());
        
        return stem;
    }
    
    /**
     * Cria um ComponentStem COM URI SEPARADA sem deletar o StudyObject existente
     */
    private void createComponentStemFromStudyObjectWithoutDelete(StudyObject so) {
        String vstoiUri = generateVSTOIUri(so.getUri(), "CSTEM");
        
        ComponentStem componentStem = new ComponentStem();
        componentStem.setUri(vstoiUri);  // ✅ URI DIFERENTE!
        componentStem.setTypeUri(VSTOI.COMPONENT_STEM);
        componentStem.setHascoTypeUri(VSTOI.COMPONENT_STEM);
        componentStem.setLabel(so.getLabel());
        componentStem.setComment(so.getComment());
        componentStem.setNamedGraph(getNamedGraphUri());
        componentStem.setHasSIRManagerEmail(so.getHasSIRManagerEmail());
        
        componentStem.saveToTripleStore(true, false);
        addVSTOILinkToStudyObject(so.getUri(), vstoiUri, "vstoi:hasComponentStem");
        
        dataFile.getLogger().println("  Created ComponentStem (dual-layer): " + componentStem.getLabel());
    }
    
    /**
     * Cria um ContainerSlot a partir de um StudyObject
     * @return O ContainerSlot criado
     */
    private ContainerSlot createContainerSlotFromStudyObject(StudyObject so) {
        ContainerSlot slot = new ContainerSlot();
        
        slot.setUri(so.getUri());
        slot.setTypeUri(so.getTypeUri());
        slot.setHascoTypeUri(VSTOI.CONTAINER_SLOT);
        slot.setLabel(so.getLabel());
        slot.setComment(so.getComment());
        slot.setNamedGraph(getNamedGraphUri());
        // Note: ContainerSlot não tem setHasSIRManagerEmail()
        
        // Salva imediatamente
        slot.save();
        
        dataFile.getLogger().println("  Created ContainerSlot: " + slot.getLabel());
        
        return slot;
    }
    
    /**
     * Cria um ContainerSlot COM URI SEPARADA sem deletar o StudyObject existente
     */
    private void createContainerSlotFromStudyObjectWithoutDelete(StudyObject so) {
        String vstoiUri = generateVSTOIUri(so.getUri(), "CTSLOT");
        
        ContainerSlot containerSlot = new ContainerSlot();
        containerSlot.setUri(vstoiUri);  // ✅ URI DIFERENTE!
        containerSlot.setTypeUri(VSTOI.CONTAINER_SLOT);
        containerSlot.setHascoTypeUri(VSTOI.CONTAINER_SLOT);
        containerSlot.setLabel(so.getLabel());
        containerSlot.setComment(so.getComment());
        containerSlot.setNamedGraph(getNamedGraphUri());
        // Note: ContainerSlot does not have setHasSIRManagerEmail()
        
        containerSlot.saveToTripleStore(true, false);
        addVSTOILinkToStudyObject(so.getUri(), vstoiUri, "vstoi:hasContainerSlot");
        
        dataFile.getLogger().println("  Created ContainerSlot (dual-layer): " + containerSlot.getLabel());
    }
    
    /**
     * Cria um Codebook a partir de um StudyObject
     * @return O Codebook criado
     */
    private Codebook createCodebookFromStudyObject(StudyObject so) {
        Codebook codebook = new Codebook();
        
        codebook.setUri(so.getUri());
        codebook.setTypeUri(so.getTypeUri());
        codebook.setHascoTypeUri(VSTOI.CODEBOOK);
        codebook.setLabel(so.getLabel());
        codebook.setComment(so.getComment());
        codebook.setNamedGraph(getNamedGraphUri());
        codebook.setHasSIRManagerEmail(so.getHasSIRManagerEmail());
        
        // Salva imediatamente
        codebook.save();
        
        dataFile.getLogger().println("  Created Codebook: " + codebook.getLabel());
        
        return codebook;
    }
    
    /**
     * Cria um Codebook SEM deletar o StudyObject existente
     */
    private void createCodebookFromStudyObjectWithoutDelete(StudyObject so) {
        String vstoiUri = generateVSTOIUri(so.getUri(), "CB");
        
        Codebook codebook = new Codebook();
        codebook.setUri(vstoiUri);  // ✅ URI DIFERENTE!
        codebook.setTypeUri(VSTOI.CODEBOOK);
        codebook.setHascoTypeUri(VSTOI.CODEBOOK);
        codebook.setLabel(so.getLabel());
        codebook.setComment(so.getComment());
        codebook.setNamedGraph(getNamedGraphUri());
        codebook.setHasSIRManagerEmail(so.getHasSIRManagerEmail());
        
        codebook.saveToTripleStore(true, false);
        addVSTOILinkToStudyObject(so.getUri(), vstoiUri, "vstoi:hasCodebook");
        
        dataFile.getLogger().println("  Created Codebook (dual-layer): " + codebook.getLabel());
    }
    
    /**
     * Cria um ResponseOption a partir de um StudyObject
     * @return O ResponseOption criado
     */
    private ResponseOption createResponseOptionFromStudyObject(StudyObject so) {
        ResponseOption responseOption = new ResponseOption();
        
        responseOption.setUri(so.getUri());
        responseOption.setTypeUri(so.getTypeUri());
        responseOption.setHascoTypeUri(VSTOI.RESPONSE_OPTION);
        responseOption.setLabel(so.getLabel());
        responseOption.setComment(so.getComment());
        responseOption.setNamedGraph(getNamedGraphUri());
        responseOption.setHasSIRManagerEmail(so.getHasSIRManagerEmail());
        
        // Salva imediatamente
        responseOption.save();
        
        dataFile.getLogger().println("  Created ResponseOption: " + responseOption.getLabel());
        
        return responseOption;
    }
    
    /**
     * Cria um ResponseOption SEM deletar o StudyObject existente
     */
    private void createResponseOptionFromStudyObjectWithoutDelete(StudyObject so) {
        String vstoiUri = generateVSTOIUri(so.getUri(), "ROPT");
        
        ResponseOption responseOption = new ResponseOption();
        responseOption.setUri(vstoiUri);  // ✅ URI DIFERENTE!
        responseOption.setTypeUri(VSTOI.RESPONSE_OPTION);
        responseOption.setHascoTypeUri(VSTOI.RESPONSE_OPTION);
        responseOption.setLabel(so.getLabel());
        responseOption.setComment(so.getComment());
        responseOption.setNamedGraph(getNamedGraphUri());
        responseOption.setHasSIRManagerEmail(so.getHasSIRManagerEmail());
        
        responseOption.saveToTripleStore(true, false);
        addVSTOILinkToStudyObject(so.getUri(), vstoiUri, "vstoi:hasResponseOption");
        
        dataFile.getLogger().println("  Created ResponseOption (dual-layer): " + responseOption.getLabel());
    }
    
    /**
     * Cria um AnnotationStem a partir de um StudyObject
     * @return O AnnotationStem criado
     */
    private AnnotationStem createAnnotationStemFromStudyObject(StudyObject so) {
        AnnotationStem annotationStem = new AnnotationStem();
        
        annotationStem.setUri(so.getUri());
        annotationStem.setTypeUri(so.getTypeUri());
        annotationStem.setHascoTypeUri(VSTOI.ANNOTATION_STEM);
        annotationStem.setLabel(so.getLabel());
        annotationStem.setComment(so.getComment());
        annotationStem.setNamedGraph(getNamedGraphUri());
        annotationStem.setHasSIRManagerEmail(so.getHasSIRManagerEmail());
        
        // Salva imediatamente
        annotationStem.save();
        
        dataFile.getLogger().println("  Created AnnotationStem: " + annotationStem.getLabel());
        
        return annotationStem;
    }
    
    /**
     * Cria um AnnotationStem COM URI SEPARADA sem deletar o StudyObject existente
     */
    private void createAnnotationStemFromStudyObjectWithoutDelete(StudyObject so) {
        String vstoiUri = generateVSTOIUri(so.getUri(), "ASTEM");
        
        AnnotationStem annotationStem = new AnnotationStem();
        annotationStem.setUri(vstoiUri);  // ✅ URI DIFERENTE!
        annotationStem.setTypeUri(VSTOI.ANNOTATION_STEM);
        annotationStem.setHascoTypeUri(VSTOI.ANNOTATION_STEM);
        annotationStem.setLabel(so.getLabel());
        annotationStem.setComment(so.getComment());
        annotationStem.setNamedGraph(getNamedGraphUri());
        annotationStem.setHasSIRManagerEmail(so.getHasSIRManagerEmail());
        
        annotationStem.saveToTripleStore(true, false);
        addVSTOILinkToStudyObject(so.getUri(), vstoiUri, "vstoi:hasAnnotationStem");
        
        dataFile.getLogger().println("  Created AnnotationStem (dual-layer): " + annotationStem.getLabel());
    }

    @Override
    public Map<String, Object> createRow(Record rec, int rowNumber) throws Exception {
        if (getOriginalID(rec).length() > 0) {
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
        // NÃO fazer nada aqui - o enrichment acontece no commitObjectsToTripleStore()
    }
    
    @Override
    public boolean commitObjectsToTripleStore(List<HADatAcThing> objects) {
        dataFile.getLogger().println("[COMMIT DEBUG] Starting commitObjectsToTripleStore");
        dataFile.getLogger().println("[COMMIT DEBUG] Objects to commit: " + objects.size());
        dataFile.getLogger().println("[COMMIT DEBUG] VSTOI objects to enrich: " + vstoiObjectsToEnrich.size());
        
        // Primeiro, deleta e salva os StudyObjects normalmente (comportamento padrão do BaseGenerator)
        dataFile.getLogger().println("[COMMIT DEBUG] Calling super.commitObjectsToTripleStore()...");
        boolean result = super.commitObjectsToTripleStore(objects);
        dataFile.getLogger().println("[COMMIT DEBUG] super.commitObjectsToTripleStore() completed with result: " + result);
        
        // AGORA, após os StudyObjects estarem salvos, adicionamos a camada VSTOI
        // aos objetos que precisam (sem deletar os StudyObjects)
        if (!vstoiObjectsToEnrich.isEmpty()) {
            dataFile.getLogger().println("[POST-COMMIT] Enriching " + vstoiObjectsToEnrich.size() + " VSTOI objects with specialized properties...");
            
            int count = 0;
            for (StudyObject studyObject : vstoiObjectsToEnrich) {
                count++;
                dataFile.getLogger().println("[POST-COMMIT] Enriching object " + count + "/" + vstoiObjectsToEnrich.size() + ": " + studyObject.getLabel());
                createVstoiEntityWithoutDeletingStudyObject(studyObject);
            }
            
            dataFile.getLogger().println("[POST-COMMIT] VSTOI enrichment completed.");
            
            // Limpa a lista para evitar reprocessamento
            vstoiObjectsToEnrich.clear();
        } else {
            dataFile.getLogger().println("[POST-COMMIT] No VSTOI objects to enrich (list is empty)");
        }
        
        dataFile.getLogger().println("[COMMIT DEBUG] commitObjectsToTripleStore finished");
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
