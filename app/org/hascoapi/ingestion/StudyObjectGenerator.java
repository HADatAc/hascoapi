package org.hascoapi.ingestion;

import java.lang.String;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    }

    @Override
    public void initMapping() {
        mapCol.clear();
        mapCol.put("originalID", "originalID");
        mapCol.put("rdf:type", "rdf:type");
        mapCol.put("scopeID", "scopeID");
        mapCol.put("timeScopeID", "timeScopeID");
        mapCol.put("spaceScopeID", "spaceScopeID");
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
        String originalID = rec.getValueByColumnName(mapCol.get("originalID"));

        if (URIUtils.isValidURI(originalID)) {
            return URIUtils.getBaseName(originalID);
        }

        // Versão usada no label: substituir espaços simples por underscore
        String labelId = originalID == null ? "" : originalID.replace(' ', '_');

        if (getSoc() != null && getSoc().getRoleLabel() != null && !getSoc().getRoleLabel().equals("")) {
            return getSoc().getRoleLabel() + " " + labelId;
        }

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
        		// the value returned by getValueByColumnName may be an URI or an original.
        		if (URIUtils.isValidURI(returnedValue)) {
        			// if returned value is an URI, this function returns the URI with expanded namespace
        			return URIUtils.replacePrefixEx(returnedValue);
        		} else {
        			// if returned value is not an URI, this function composes an URI according to SDD convention
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
            createVstoiEntityIfApplicable(studyObject);
        }
        
        return studyObject;
    }
    
    /**
     * Cria entidade vstoi (Instrument, Component, ComponentStem, ContainerSlot) 
     * se o StudyObject tiver um tipo vstoi
     */
    private void createVstoiEntityIfApplicable(StudyObject studyObject) {
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
                createInstrumentFromStudyObject(studyObject);
            } else if (VSTOI.COMPONENT.equals(vstoiType)) {
                createComponentFromStudyObject(studyObject);
            } else if (VSTOI.COMPONENT_STEM.equals(vstoiType)) {
                createComponentStemFromStudyObject(studyObject);
            } else if (VSTOI.CONTAINER_SLOT.equals(vstoiType)) {
                createContainerSlotFromStudyObject(studyObject);
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
        
        // Verificação por substring (subclasses)
        if (typeUri.contains("Detector")) return VSTOI.COMPONENT;
        if (typeUri.contains("ComponentStem")) return VSTOI.COMPONENT_STEM;
        if (typeUri.contains("ContainerSlot")) return VSTOI.CONTAINER_SLOT;
        if (typeUri.contains("Questionnaire")) return VSTOI.INSTRUMENT;
        if (typeUri.contains("PhysicalInstrument")) return VSTOI.INSTRUMENT;
        if (typeUri.contains("SimulationModel")) return VSTOI.INSTRUMENT;
        
        return null;
    }
    
    /**
     * Cria um Instrument a partir de um StudyObject
     */
    private void createInstrumentFromStudyObject(StudyObject so) {
        Instrument instrument = new Instrument();
        
        instrument.setUri(so.getUri());
        instrument.setTypeUri(so.getTypeUri());
        instrument.setHascoTypeUri(VSTOI.INSTRUMENT);
        instrument.setLabel(so.getLabel());
        instrument.setComment(so.getComment());
        instrument.setNamedGraph(getNamedGraphUri());
        instrument.setHasSIRManagerEmail(so.getHasSIRManagerEmail());
        
        // Salva imediatamente
        instrument.save();
        
        dataFile.getLogger().println("  Created Instrument: " + instrument.getLabel());
    }
    
    /**
     * Cria um Component a partir de um StudyObject
     */
    private void createComponentFromStudyObject(StudyObject so) {
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
    }
    
    /**
     * Cria um ComponentStem a partir de um StudyObject
     */
    private void createComponentStemFromStudyObject(StudyObject so) {
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
    }
    
    /**
     * Cria um ContainerSlot a partir de um StudyObject
     */
    private void createContainerSlotFromStudyObject(StudyObject so) {
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
    public String getTableName() {
        return "StudyObject";
    }

    @Override
    public String getErrorMsg(Exception e) {
        return "Error in StudyObjectGenerator: " + e.getMessage();
    }
}
