package org.hascoapi.ingestion;

import java.lang.String;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hascoapi.utils.URIUtils;
import org.hascoapi.utils.Utils;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.HADatAcThing;
import org.hascoapi.entity.pojo.StudyObjectCollection;
import org.hascoapi.entity.pojo.StudyObject;


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
        return Utils.uriPlainGen("studyobject",
            rec.getValueByColumnName(mapCol.get("originalID")),
            this.namespace,
            this.soc_reference);
    }

    private String getType(Record rec) {
        return rec.getValueByColumnName(mapCol.get("rdf:type"));
    }

    private String getLabel(Record rec) {
        String originalID = rec.getValueByColumnName(mapCol.get("originalID"));
        if (URIUtils.isValidURI(originalID)) {
            return URIUtils.getBaseName(originalID);
        }
        
        if (getSoc() != null && getSoc().getRoleLabel() != null && !getSoc().getRoleLabel().equals("")) {
    		return getSoc().getRoleLabel() + " " + originalID;
    	}
        
        String auxstr = uriMap.get(soc_type);
        if (auxstr == null) {
            auxstr = "";
        } else {
            auxstr = auxstr.replaceAll("-","");
        }

        if (auxstr.contains("SBJ")) {
            return auxstr + " " + originalID;
        }
        return auxstr + " " + originalID + " - " + study_id;
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
        //System.out.println("StudyObjectGenerator: getOriginalID(2) = [" + auxstr + "]");
        
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
        		String scopeSOCtype = mapContent.get(soc_scope).get(1);
        		if (scopeSOCtype.toLowerCase().contains("SubjectGroup".toLowerCase())) {
                    return Utils.uriPlainGen("studyobject",
                        rec.getValueByColumnName(mapCol.get("scopeID")).replaceAll("(?<=^\\d+)\\.0*$", ""),
                        this.namespace,
                        domain_reference);
                } else {
                    return "";
                }
            } else {
        		System.out.println("[ERROR] StudyObjectGenerator: no mapping for [" + soc_scope + "] in getScopeUri()");
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
        		System.out.println("[ERROR] StudyObjectGenerator: no mapContent for [" + soc_timescope + "] in getTimeScopeUri(). Record is " + rec);
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
        		System.out.println("[ERROR] StudyObjectGenerator: no mapContent for [" + soc_spacescope + "] in getSpaceScopeUri(). Record is " + rec);
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

    	StudyObject obj = new StudyObject(
            getUri(record), 
            getType(record), 
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

        obj.addScopeUri(getScopeUri(record));
        obj.addTimeScopeUri(getTimeScopeUri(record));
        obj.addSpaceScopeUri(getSpaceScopeUri(record));
        
        return obj;
    }

    @Override
    public HADatAcThing createObject(Record rec, int rowNumber, String selector) throws Exception {
        return createStudyObject(rec);
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
