package org.hascoapi.ingestion;

import java.lang.String;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.HADatAcThing;
import org.hascoapi.entity.pojo.StudyObjectCollection;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.utils.Utils;
import org.hascoapi.vocabularies.HASCO;

public class SSDGenerator extends BaseGenerator {

    String SDDName = ""; //used for reference column uri

    String namespace = "";

    public SSDGenerator(DataFile dataFile, String namespace) {
        super(dataFile);
        //this.SDDName = dataFile.getBaseName().replaceAll("SSD-", "");

        this.namespace = namespace;
        studyUri = dataFile.getStudyUri();
        //if (records.get(0) != null) {
        //    studyUri = URIUtils.replacePrefixEx(getUri(records.get(0)));
        //} else {
        //    studyUri = "";
        //}
    }

    @Override
    public void initMapping() {
        mapCol.clear();
        mapCol.put("sheet", "sheet");
        mapCol.put("uri", "hasURI");
        mapCol.put("typeUri", "type");
        mapCol.put("hasSOCReference", "hasSOCReference");
        mapCol.put("hasRoleLabel", "hasRoleLabel");
        mapCol.put("label", "label");
        mapCol.put("hasScopeUri", "hasScope");
        //mapCol.put("groundingLabel", "groundingLabel");
        mapCol.put("spaceScopeUris", "hasSpaceScope");
        mapCol.put("timeScopeUris", "hasTimeScope");
        mapCol.put("groupUris", "hasGroup");
    }

    private String getUri(Record rec) {
        //System.out.println("SSDGenerator: namespace before getUri() is [" + namespace + "]");
        String newUri = Utils.uriPlainGen(
            "studyobjectcollection", 
            rec.getValueByColumnName(mapCol.get("uri")),
            namespace);
        //System.out.println("SSDGenerator: namespace after getUri() is [" + newUri + "]");
        return newUri;
    }

    private String getTypeUri(Record rec) {
        return rec.getValueByColumnName(mapCol.get("typeUri"));
    }

    private String getLabel(Record rec) {
        return rec.getValueByColumnName(mapCol.get("label"));
    }

    private String getVirtualColumnUri(Record rec) {
        String vcUri= 
            studyUri.replace(Constants.PREFIX_STUDY, Constants.PREFIX_VIRTUAL_COLUMN) + "-" + 
            getSOCReference(rec);
        return vcUri;
    }
    
    private String getSOCReference(Record rec) {
        String ref = rec.getValueByColumnName(mapCol.get("hasSOCReference"));
        if (ref == null || ref.isEmpty()) {
            return "";
        }
        // Clean the reference: remove ??, spaces, and underscores
        return ref.trim().replace("??", "").replace(" ", "").replace("_", "-");
    }

    private String getRoleLabel(Record rec) {
        if (mapCol.get("hasRoleLabel") == null) {
            return "";
        }
        String ref = rec.getValueByColumnName(mapCol.get("hasRoleLabel"));
        if (ref == null) {
            return "";
        }
        return ref.trim().replace(" ", "").replace("_", "-");
    }

    private String getHasScopeUri(Record rec) {
        if (rec.getValueByColumnName(mapCol.get("hasScopeUri")) == null ||
            rec.getValueByColumnName(mapCol.get("hasScopeUri")).isEmpty()) {
            return null;
        }
        return Utils.uriPlainGen(
            "studyobjectcollection", 
            rec.getValueByColumnName(mapCol.get("hasScopeUri")),
            namespace);
    }

    /*private String getGroundingLabel(Record rec) {
        return rec.getValueByColumnName(mapCol.get("groundingLabel"));
    }*/

    private List<String> getSpaceScopeUris(Record rec) {
        if (mapCol.get("spaceScopeUris") == null || rec.getValueByColumnName(mapCol.get("spaceScopeUris")) == null) {
            return new ArrayList<String>();
        }
        //System.out.println("getSpaceScopeUris: getValueByColumnName: [" + rec.getValueByColumnName(mapCol.get("spaceScopeUris")) + "]");
        List<String> ans = Arrays.asList(rec.getValueByColumnName(mapCol.get("spaceScopeUris")).split(","))
                .stream()
                .map(s -> URIUtils.replacePrefixEx(s))
                .collect(Collectors.toList());
        List<String> uris = new ArrayList<String>();
        for (String item : ans) {
            if (item == null || item.isEmpty()) {
                uris.add(null);
            } else {
                uris.add(Utils.uriPlainGen(
                    "studyobjectcollection", 
                    item,
                    namespace));
            }
        }
        return uris;
    }

    private List<String> getTimeScopeUris(Record rec) {
        //System.out.println("getTimeScopeUris:  timeScopeUris is [" + mapCol.get("timeScopeUris") + "]");
        if (mapCol.get("timeScopeUris") == null || rec.getValueByColumnName(mapCol.get("timeScopeUris")) == null) {
            return new ArrayList<String>();
        }
        //System.out.println("getTimeScopeUris: getValueByColumnName: [" + rec.getValueByColumnName(mapCol.get("timeScopeUris")) + "]");
        List<String> ans = Arrays.asList(rec.getValueByColumnName(mapCol.get("timeScopeUris")).split(","))
                .stream()
                .map(s -> URIUtils.replacePrefixEx(s))
                .collect(Collectors.toList());
        List<String> uris = new ArrayList<String>();
        for (String item : ans) {
            if (item == null || item.isEmpty()) {
                uris.add(null);
            } else {
                uris.add(Utils.uriPlainGen(
                    "studyobjectcollection", 
                    item,
                    namespace));
            }
        }
        return uris;
    }

    private List<String> getGroupUris(Record rec) {
        if (mapCol.get("groupUris") == null || rec.getValueByColumnName(mapCol.get("groupUris")) == null) {
            return new ArrayList<String>();
        }
        List<String> ans = Arrays.asList(rec.getValueByColumnName(mapCol.get("groupUris")).split(","))
                .stream()
                .map(s -> URIUtils.replacePrefixEx(s))
                .collect(Collectors.toList());
        return ans;
    }

    public StudyObjectCollection createObjectCollection(Record record) throws Exception {

        if (record.size() <= 0) {  // skip empty records
			return null;
		}

        String uri = this.getUri(record);
    	String typeUri = this.getTypeUri(record);
        String SOCReference = getSOCReference(record);

        System.out.println("SSDGenerator: recordSize=[" + record.size() + "]");
        System.out.println("     uri=[" + uri + "] studyUri=[" + studyUri + "]");
        System.out.println("     typeUri=[" + typeUri + "] SOCReference=[" + SOCReference + "] (cleaned)");

        // Skip empty type and reference
        if (typeUri == null || typeUri.isEmpty()) {
        	return null;
        }
        
        if (this.studyUri == null || this.studyUri.isEmpty()) {
            logger.printExceptionByIdWithArgs("DSG_00006", typeUri);
            return null;
        }
            
        // AUTO-DERIVE SOCReference if not provided
        // VSTOI uses standard hasco:StudyObjectCollection or hasco:ObjectCollection
        // The distinction is made at the instance level (vstoi:Instrument, vstoi:Component, etc.)
        // So we derive SOCReference from the URI for ANY collection that doesn't provide it
        if (SOCReference == null || SOCReference.isEmpty()) {
            // Check if this is a generic ObjectCollection - auto-derive reference
            boolean isObjectCollection = typeUri.contains("ObjectCollection") || 
                                        typeUri.contains("StudyObjectCollection");
            
            if (isObjectCollection) {
                // Auto-derive from URI: extract last segment after OCL_
                if (uri != null && !uri.isEmpty()) {
                    String[] parts = uri.split("[/#]");
                    String lastSegment = parts[parts.length - 1];
                    SOCReference = lastSegment.replace("OCL_", "");
                    logger.println("  [AUTO] Auto-derived SOCReference: " + SOCReference);
                } else {
                    SOCReference = "AUTO-OBJ";
                    logger.println("  [AUTO] Using fallback SOCReference: " + SOCReference);
                }
            } else {
                // For specific typed collections (SubjectGroup, SampleCollection, etc.), SOCReference is REQUIRED
                logger.printExceptionById("DSG_00007");
                return null;
            }
        }

        String scopeUri = getHasScopeUri(record);
        if (scopeUri != null && !scopeUri.isEmpty()) {
            scopeUri = URIUtils.replacePrefixEx(scopeUri);
        }
        //System.out.println("SSDGenerator: [" + scopeUri + "]");

        StudyObjectCollection soc = new StudyObjectCollection();
        soc.setUri(uri);
        soc.setTypeUri(URIUtils.replacePrefixEx(typeUri));
        soc.setHascoTypeUri(URIUtils.replacePrefixEx(HASCO.STUDY_OBJECT_COLLECTION));
        soc.setLabel(getLabel(record));
        soc.setComment(getLabel(record));
        soc.setIsMemberOfUri(studyUri);
        soc.setVirtualColumnUri(getVirtualColumnUri(record));
        soc.setRoleUri(getRoleLabel(record));
        soc.setHasSIRManagerEmail(this.dataFile.getHasSIRManagerEmail());
        // SOCs must always be saved to DEFAULT_REPOSITORY (not to file's named graph)
        soc.setNamedGraph("");
        if (scopeUri != null && !scopeUri.isEmpty()) {
            soc.setHasScopeUri(scopeUri);
        }
        soc.setTimeScopeUris(getTimeScopeUris(record));
        soc.setSpaceScopeUris(getSpaceScopeUris(record));
        soc.setGroupUris(getGroupUris(record));
        soc.setLastCounter("0");
        
        // DEBUG: Log SOC properties before saving
        System.out.println("[SOC DEBUG] Created SOC:");
        System.out.println("  URI: " + soc.getUri());
        System.out.println("  isMemberOf: " + soc.getIsMemberOfUri());
        System.out.println("  label: " + soc.getLabel());
        System.out.println("  studyUri passed: " + studyUri);

        /* 
        StudyObjectCollection soc = new StudyObjectCollection(
                getUri(record),
                URIUtils.replacePrefixEx(typeUri),
                URIUtils.replacePrefixEx(HASCO.STUDY_OBJECT_COLLECTION),
                getLabel(record),
                getLabel(record),
                getStudyUri(),
                getVirtualColumnUri(record),
                getRoleLabel(record),
                this.dataFile.getHasSIRManagerEmail(),
                scopeUri,
                getSpaceScopeUris(record),
                getTimeScopeUris(record),
                getGroupUris(record),
                "0");
        */

        System.out.println("New SOC: uri=[" + soc.getUri() + "] label=[" + soc.getLabel() + "]");

        return soc;
    }   
        
    @Override
    public void preprocess() throws Exception {}

    @Override
    public HADatAcThing createObject(Record rec, int rowNumber, String selector) throws Exception {
            String uri = getUri(rec);
            if (uri == null || uri.isEmpty()) {
                System.out.println("[ERROR] SSDGenerator.createObject(): getUri() returned null/empty for row " + rowNumber);
                return null;
            }
            if (!URIUtils.replacePrefixEx(uri).equals(studyUri)) {
                HADatAcThing obj = createObjectCollection(rec);
                return obj;
            }
        return null;
    }

    @Override
    public String getErrorMsg(Exception e) {
        return "Error in SSDGenerator: " + e.getMessage();
    }

    @Override
    public String getTableName() {
        // TODO Auto-generated method stub
        return null;
    }
}
