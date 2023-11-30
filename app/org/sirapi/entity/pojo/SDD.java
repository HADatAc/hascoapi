package org.sirapi.entity.pojo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.sirapi.utils.CollectionUtil;
import org.sirapi.utils.NameSpaces;
import org.sirapi.utils.FirstLabel;
import org.sirapi.utils.URIUtils;
import org.sirapi.utils.SPARQLUtils;
import org.sirapi.entity.pojo.SDDAttribute;
import org.sirapi.entity.pojo.SDDObject;

public class SDD extends HADatAcThing {
    public static String INDENT1 = "     ";
    public static String INSERT_LINE1 = "INSERT DATA {  ";
    public static String DELETE_LINE1 = "DELETE WHERE {  ";
    public static String LINE3 = INDENT1 + "a         hasco:SDD;  ";
    public static String DELETE_LINE3 = INDENT1 + " ?p ?o . ";
    public static String LINE_LAST = "}  ";
    public static String PREFIX = "SDD-";
    public static List<String> METASDDA = Arrays.asList(
            "hasco:TimeStamp", 
            "sio:SIO_000418", 
            "hasco:namedTime", 
            "hasco:originalID", 
            "hasco:uriId", 
            "hasco:hasMetaEntity", 
            "hasco:hasMetaEntityURI", 
            "hasco:hasMetaAttribute", 
            "hasco:hasMetaAttributeURI", 
            "hasco:hasMetaUnit", 
            "hasco:hasMetaUnitURI", 
            "sio:SIO_000668",
            "hasco:hasLOD",
            "hasco:hasCalibration",
            "hasco:hasElevation",
            "hasco:hasLocation",
            "hasco:isGroupMember",
            "hasco:matchesWith");

    private static Map<String, SDD> SDDCache;
    private List<SDDAttribute> attributesCache = new ArrayList<SDDAttribute>();
    private List<SDDObject> objectsCache = new ArrayList<SDDObject>();
    private Map<String, Map<String, String>> possibleValuesCache = new HashMap<String, Map<String, String>>();
    
    private String uri = "";
    private String label = "";
    private String version = "";
    private String timestampLabel = "";
    private String timeInstantLabel = "";
    private String namedTimeLabel = "";
    private String idLabel = "";
    private String originalIdLabel = "";
    private String elevationLabel = "";
    private String entityLabel = "";
    private String unitLabel = "";
    private String inRelationToLabel = "";
    private String lodLabel = "";
    private String groupLabel = "";
    private String matchingLabel = "";
    
    private List<String> attributes = new ArrayList<String>();
    private List<String> objects = new ArrayList<String>();
    private List<String> events = new ArrayList<String>();
    private boolean isRefreshed = false;

    private static Map<String, SDD> getCache() {
        if (SDDCache == null) {
            SDDCache = new HashMap<String, SDD>(); 
        } 
        return SDDCache;
    }

    public static void resetCache() {
        SDDAttribute.resetCache();
        SDDObject.resetCache();
        SDDCache = null;
    }

    public void resetAttributesCache() {
        attributesCache = null;
    }

    public void resetObjectsCache() {
        objectsCache = null;
    }


    public SDD() {
        SDD.getCache();
    }

    public SDD(String uri, String label) {
        this.uri = uri;
        this.label = label;
        isRefreshed = false;
        SDD.getCache();
        getAttributes();
        getObjects();
    }

    public String getUri() {
        return uri;
    }

    public String getUriNamespace() {
        return URIUtils.replaceNameSpaceEx(uri);
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getTimestampLabel() {
        return timestampLabel;
    }

    public void setTimestampLabel(String timestampLabel) {
        this.timestampLabel = timestampLabel;
    }

    public String getTimeInstantLabel() {
        return timeInstantLabel;
    }

    public void setTimeInstantLabel(String timeInstantLabel) {
        this.timeInstantLabel = timeInstantLabel;
    }

    public String getNamedTimeLabel() {
        return namedTimeLabel;
    }

    public void setNamedTimeLabel(String namedTimeLabel) {
        this.namedTimeLabel = namedTimeLabel;
    }

    public String getIdLabel() {
        return idLabel;
    }

    public void setIdLabel(String idLabel) {
        this.idLabel = idLabel;
    }

    public String getOriginalIdLabel() {
        return originalIdLabel;
    }

    public void setOriginalIdLabel(String originalIdLabel) {
        this.originalIdLabel = originalIdLabel;
    }

    public String getLODLabel() {
        return lodLabel;
    }

    public void setLODLabel(String lodLabel) {
        this.lodLabel = lodLabel;
    }

    public String getGroupLabel() {
        return groupLabel;
    }

    public void setGroupLabel(String groupLabel) {
        this.groupLabel = groupLabel;
    }

    public String getMatchingLabel() {
        return matchingLabel;
    }

    public void setMatchingLabel(String matchingLabel) {
        this.matchingLabel = matchingLabel;
    }

    public String getElevationLabel() {
        return elevationLabel;
    }

    public void setElevationLabel(String elevationLabel) {
        this.elevationLabel = elevationLabel;
    }

    public String getEntityLabel() {
        return entityLabel;
    }

    public void setEntityLabel(String entityLabel) {
        this.entityLabel = entityLabel;
    }

    public String getUnitLabel() {
        return unitLabel;
    }

    public void setUnitLabel(String unitLabel) {
        this.unitLabel = unitLabel;
    }

    public String getInRelationToLabel() {
        return inRelationToLabel;
    }

    public void setInRelationToLabel(String inRelationToLabel) {
        this.inRelationToLabel = inRelationToLabel;
    }

    public int getTotalSDDA() {
        if (attributes == null) {
            return -1;
        }
        return attributes.size();
    }

    public int getTotalSDDE() {
        if (events == null) {
            return -1;
        }
        return events.size();
    }

    public int getTotalSDDO() {
        if (objects == null) {
            return -1;
        }
        return objects.size();
    }

    public List<SDDAttribute> getAttributes() {
        if (attributesCache == null || attributesCache.isEmpty()) {
            attributesCache = SDDAttribute.findBySchema(getUri());
        }
        return attributesCache;
    }

    public void setAttributes(List<String> attributes) {
        if (attributes == null) {
            System.out.println("[WARNING] No SDDObject for " + uri + " is defined in the knowledge base. ");
        } else {
            this.attributes = attributes;
            if (!isRefreshed) {
                refreshAttributes();
            }
        }
    }

    public void refreshAttributes() {
        List<SDDAttribute> attributeList = SDDAttribute.findBySchema(this.getUri());
        if (attributes == null) {
            System.out.println("[ERROR] No SDDAttribute for " + uri + " is defined in the knowledge base. ");
        } else {
            for (SDDAttribute sdda : attributeList) {
                sdda.setSDD(this);

                if (sdda.getAttributes().contains(URIUtils.replacePrefixEx("hasco:TimeStamp"))) {
                    setTimestampLabel(sdda.getLabel());
                    //System.out.println("[OK] SDD TimeStampLabel: " + sdda.getLabel());
                }
                if (sdda.getAttributes().contains(URIUtils.replacePrefixEx("sio:SIO_000418"))) {
                    setTimeInstantLabel(sdda.getLabel());
                    //System.out.println("[OK] SDD TimeInstantLabel: " + sdda.getLabel());
                }
                if (sdda.getAttributes().contains(URIUtils.replacePrefixEx("hasco:namedTime"))) {
                    setNamedTimeLabel(sdda.getLabel());
                    //System.out.println("[OK] SDD NamedTimeLabel: " + sdda.getLabel());
                }
                if (sdda.getAttributes().contains(URIUtils.replacePrefixEx("hasco:uriId"))) {
                    setIdLabel(sdda.getLabel());
                    //System.out.println("[OK] SDD IdLabel: " + sdda.getLabel());
                }
                if (sdda.getAttributes().contains(URIUtils.replacePrefixEx("hasco:LevelOfDetection"))) {
                    setLODLabel(sdda.getLabel());
                    //System.out.println("[OK] SDD LODLabel: " + sdda.getLabel());
                }
                if (sdda.getAttributes().contains(URIUtils.replacePrefixEx("hasco:isGroupMember"))) {
                    setGroupLabel(sdda.getLabel());
                    //System.out.println("[OK] SDD GroupLabel: " + sdda.getLabel());
                }
                if (sdda.getAttributes().contains(URIUtils.replacePrefixEx("hasco:matchesWith"))) {
                    setMatchingLabel(sdda.getLabel());
                    //System.out.println("[OK] SDD MatchingLabel: " + sdda.getLabel());
                }
                if (sdda.getAttributes().contains(URIUtils.replacePrefixEx("hasco:originalID")) 
                        || sdda.getAttributes().equals(URIUtils.replacePrefixEx("sio:SIO_000115")) 
                        || Entity.getSubclasses(URIUtils.replacePrefixEx("hasco:originalID")).contains(sdda.getAttributes())) { 
                    setOriginalIdLabel(sdda.getLabel());
                    //System.out.println("[OK] SDD IdLabel: " + sdda.getLabel());
                }
                if (sdda.getAttributes().contains(URIUtils.replacePrefixEx("hasco:hasEntity"))) {
                    setEntityLabel(sdda.getLabel());
                    //System.out.println("[OK] SDD EntityLabel: " + sdda.getLabel());
                }
                if (!sdda.getInRelationToUri(URIUtils.replacePrefixEx("sio:SIO_000221")).isEmpty()) {
                    String uri = sdda.getInRelationToUri(URIUtils.replacePrefixEx("sio:SIO_000221"));
                    SDDObject sddoUnit = SDDObject.find(uri);
                    if (sddoUnit != null) {
                        setUnitLabel(sddoUnit.getLabel());
                    } else {
                        SDDAttribute sddaUnit = SDDAttribute.find(uri);
                        if (sddaUnit != null) {
                            setUnitLabel(sddaUnit.getLabel());
                        }
                    }
                }
                if (sdda.getAttributes().contains(URIUtils.replacePrefixEx("sio:SIO_000668"))) {
                    setInRelationToLabel(sdda.getLabel());
                }
            }
        }
    }

    public List<SDDObject> getObjects() {
        if (objectsCache == null || objectsCache.isEmpty()) {
            objectsCache = SDDObject.findBySchema(getUri());
        }
        return objectsCache;
    }

    public void setObjects(List<String> objects) {
        if (objects == null) {
            System.out.println("[WARNING] No SDDObject for " + uri + " is defined in the knowledge base. ");
        } else {
            this.objects = objects;
        }
    }

    public SDDObject getObject(String sddoUri) {
        for (String sddo : objects) {
            if (sddo.equals(sddoUri)) {
                return SDDObject.find(sddo);
            }
        }
        return null;
    }

    public SDDObject getEvent(String sddeUri) {
        return SDDObject.find(sddeUri);
    }

    public List<String> defineTemporaryPositions(List<String> csvHeaders) {
        List<String> unknownHeaders = new ArrayList<String>(csvHeaders);
        List<SDDAttribute> listDasa = getAttributes();
        
        // Assign SDDA positions by label matching
        if (listDasa != null && listDasa.size() > 0) {
            // reset temporary positions
            for (SDDAttribute sdda : listDasa) {
                sdda.setTempPositionInt(-1);
            }

            for (int i = 0; i < csvHeaders.size(); i++) {
                for (SDDAttribute sdda : listDasa) {
                    if (csvHeaders.get(i).equalsIgnoreCase(sdda.getLabel())) {
                        sdda.setTempPositionInt(i);
                        unknownHeaders.remove(csvHeaders.get(i));
                    }
                }
            }
        }
        
        // Assign SDDO positions by label matching
        List<SDDObject> listDaso = getObjects();
        if (listDaso != null && listDaso.size() > 0) {
            // reset temporary positions
            for (SDDObject sddo : listDaso) {
                sddo.setTempPositionInt(-1);
            }

            for (int i = 0; i < csvHeaders.size(); i++) {
                for (SDDObject sddo : listDaso) {
                    if (csvHeaders.get(i).equalsIgnoreCase(sddo.getLabel())) {
                        sddo.setTempPositionInt(i);
                        unknownHeaders.remove(csvHeaders.get(i));
                    }
                }
            }
        }

        return unknownHeaders;
    }

    public int tempPositionOfLabel(String label) {
        if (label == null || label.equals("")) {
            return -1;
        }

        int position = -1;
        for (SDDAttribute sdda : getAttributes()) {
            if (sdda.getLabel().equalsIgnoreCase(label)) {
                position = sdda.getTempPositionInt();
                break;
            }
        }

        if (position != -1) {
            return position;
        }

        for (SDDObject sddo : getObjects()) {
            if (sddo.getLabel().equalsIgnoreCase(label)) {
                position = sddo.getTempPositionInt();
                break;
            }
        }

        return position;
    }

    public static SDD find(String sddUri) {
        if (SDD.getCache().get(sddUri) != null) {
        	
            SDD sdd = SDD.getCache().get(sddUri);
            sdd.getAttributes();
            return sdd;
        }

        //System.out.println("Looking for data acquisition sdd " + sddUri);

        if (sddUri == null || sddUri.equals("")) {
            System.out.println("[ERROR] SDD URI blank or null.");
            return null;
        }

        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() + 
                "SELECT ?version WHERE { " + 
                "   <" + sddUri + "> a hasco:SDD . " +
                "   <" + sddUri + "> hasco:hasVersion ?version . } ";

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        SDD sdd = new SDD();
        sdd.setUri(sddUri);
        sdd.setLabel(FirstLabel.getLabel(sddUri));

        if (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            if (soln == null) {
                System.out.println("[WARNING] SDD. Could not find sdd for uri: <" + sddUri + ">");
                return null;
            }
            if (soln.get("version") != null) {
                sdd.setVersion(soln.get("version").toString());
            }
        } else {
            System.out.println("[WARNING] SDD. Could not find sdd for uri: <" + sddUri + ">");
        	return null;
        }

        sdd.setAttributes(SDDAttribute.findUriBySchema(sddUri));
        sdd.setObjects(SDDObject.findUriBySchema(sddUri));

        sdd.getAttributes();
        sdd.getObjects();
        SDD.getCache().put(sddUri,sdd);
        return sdd;
    }

    public static List<SDD> findAll() {
        List<SDD> sdds = new ArrayList<SDD>();

        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() + 
                "SELECT ?uri WHERE { " + 
                "   ?uri a hasco:SDD . } ";

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            if (soln != null && soln.getResource("uri").getURI() != null) { 
                SDD sdd = SDD.find(soln.getResource("uri").getURI());
                sdds.add(sdd);
            }
        }

        return sdds;
    }

    public static Map<String, String> findAllUrisByLabel(String sddUri) {
        Map<String, String> resp = new HashMap<String, String>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList()
                + " SELECT ?sddo_or_sdda ?label WHERE { "
                + " ?sddo_or_sdda rdfs:label ?label . "
                + " ?sddo_or_sdda hasco:partOfSchema <" + sddUri + "> . "
                + " }";

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        String uriStr = "";
        String labelStr = "";
        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();

            if (soln.get("sddo_or_sdda") != null) {
                uriStr = soln.get("sddo_or_sdda").toString();
                if (soln.get("label") != null) {
                    labelStr = soln.get("label").toString();
                    if (uriStr != null && labelStr != null) {
                        resp.put(labelStr, uriStr);
                    }
                }
            }
        }
        return resp;
    }

    public static String findByLabel(String sddUri, String label) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList()
                + " SELECT ?sddo_or_sdda ?label WHERE { "
                + " ?sddo_or_sdda rdfs:label ?label . "
                + " ?sddo_or_sdda hasco:partOfSchema <" + sddUri + "> . "
                + " FILTER regex(str(?label), \"" + label + "\" ) "
                + " }";

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        if (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            return soln.get("sddo_or_sdda").toString();
        }

        return "";
    }

    public static SDD create(String uri) {
        SDD sdd = new SDD();
        sdd.setUri(uri);
        return sdd;
    }

    @Override
    public boolean saveToTripleStore() {
        // SAVING SDD's SDDAs
        for (SDDAttribute sdda : SDDAttribute.findBySchema(this.getUri())) {
            sdda.saveToTripleStore();
        }

        // SAVING SDD ITSELF
        String insert = "";
        insert += NameSpaces.getInstance().printSparqlNameSpaceList();
        insert += INSERT_LINE1;

        if (!getNamedGraph().isEmpty()) {
            insert += " GRAPH <" + getNamedGraph() + "> { ";
        }

        insert += this.getUri() + " a hasco:SDD . ";
        insert += this.getUri() + " rdfs:label  \"" + this.getLabel() + "\" . ";

        if (!getNamedGraph().isEmpty()) {
            insert += " } ";
        }

        if (!getNamedGraph().isEmpty()) {
            insert += " } ";
        }

        if (!getNamedGraph().isEmpty()) {
            insert += " } ";
        }

        insert += LINE_LAST;

        try {
            UpdateRequest request = UpdateFactory.create(insert);
            UpdateProcessor processor = UpdateExecutionFactory.createRemote(
                    request, CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_UPDATE));
            processor.execute();
        } catch (QueryParseException e) {
            System.out.println("QueryParseException due to update query: " + insert);
            throw e;
        }

        return true;
    }

    @Override
    public void deleteFromTripleStore() {
        super.deleteFromTripleStore();
        SDD.resetCache();
    }

    @Override
    public boolean saveToSolr() {
        return false;
    }

    @Override
    public int deleteFromSolr() {
        return 0;
    }
}
