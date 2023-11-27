package org.sirapi.entity.pojo;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

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

public class SDDAttribute extends HADatAcThing {

    public static String INDENT1 = "     ";
    public static String INSERT_LINE1 = "INSERT DATA {  ";
    public static String DELETE_LINE1 = "DELETE WHERE {  ";
    public static String LINE3 = INDENT1 + "a         hasco:SDDAttribute;  ";
    public static String DELETE_LINE3 = " ?p ?o . ";
    public static String LINE_LAST = "}  ";
    public static String PREFIX = "SDDA-";

    private static Map<String, SDDAttribute> SDDACache;

    private String uri;
    private String localName;
    private String label;
    private String partOfSchema;
    private String position;
    private int    positionInt;

    /* 
       tempPositionInt is set every time a new csv file is loaded. tempPositionIn = -1 indicates that the attribute is not valid for the given cvs
         - because an original position is out of range for the csv
         - because there is no original position and the given localName does not match any of the labels in the CSV
     */

    private int    tempPositionInt;
    private String entity;
    private String entityLabel;
    private List<String> attributes;
    private List<String> attributeLabels;
    private String unit;
    private String unitLabel;
    private String sddeUri;
    private String sddoUri;
    private Map<String, String> relations = new HashMap<String, String>();
    private boolean isMeta;
    private SDD sdd;
    private String socUri;

    private static Map<String, SDDAttribute> getCache() {
	if (SDDACache == null) {
	    SDDACache = new HashMap<String, SDDAttribute>(); 
	}
	return SDDACache;
    }

    public static void resetCache() {
	SDDACache = null;
    }

    public SDDAttribute(String uri, String partOfSchema) {
        this.uri = uri;
        this.partOfSchema = partOfSchema;
        this.localName = "";
        this.label = "";
        this.position = "";
        this.positionInt = -1;
        this.setEntity("");
        this.setAttributes(Arrays.asList(""));
        this.setUnit("");
        this.sddeUri = "";
        this.sddoUri = "";
        this.isMeta = false;
	SDDAttribute.getCache();
    }

    public SDDAttribute(String uri, 
            String localName, 
            String label,
            String partOfSchema,
            String position, 
            String entity, 
            List<String> attributes, 
            String unit, 
            String sddeUri, 
            String sddoUri) {
        this.uri = uri;
        this.localName = localName;
        this.label = label;
        this.partOfSchema = partOfSchema;
        this.position = position;
        try {
            if (position != null && !position.equals("")) {
                positionInt = Integer.parseInt(position);
            } else {
                positionInt = -1;
            }
        } catch (Exception e) {
            positionInt = -1;
        }
        this.setEntity(entity);
        this.setAttributes(attributes);
        this.setUnit(unit);
        this.sddeUri = sddeUri;
        this.sddoUri = sddoUri;
	SDDAttribute.getCache();
    }

    public String getUri() {
        if (uri == null) {
            return "";
        } else {
            return uri;
        }
    }

    public String getUriNamespace() {
        return URIUtils.replaceNameSpaceEx(uri.replace("<","").replace(">",""));
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getLocalName() {
        return localName;
    }

    public void setLocalName(String localName) {
        this.localName = localName;
    }

    public void setSDD(SDD sdd) {
        this.sdd = sdd;
    }

    public String getLabel() {
        if (label == null) {
            return "";
        } else {
            return label;
        }
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getPartOfSchema() {
        if (partOfSchema == null) {
            return "";
        } else {
            return partOfSchema;
        }
    }

    public void setPartOfSchema(String partOfSchema) {
        this.partOfSchema = partOfSchema;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public int getPositionInt() {
        return positionInt;
    }

    public int getTempPositionInt() {
        return tempPositionInt;
    }

    public void setTempPositionInt(int tempPositionInt) {
        this.tempPositionInt = tempPositionInt;
    }

    public String getEntity() {
        if (entity == null) {
            return "";
        } else {
            return entity;
        }
    }

    public String getEntityNamespace() {
        if (entity == "") {
            return "";
        }
        return URIUtils.replaceNameSpaceEx(entity.replace("<","").replace(">",""));
    }

    public void setEntity(String entity) {
        this.entity = entity;
        if (entity == null || entity.equals("")) {
            this.entityLabel = "";
        } else {
            this.entityLabel = FirstLabel.getPrettyLabel(entity);
        }
    }

    public String getEntityLabel() {
        if (entityLabel.equals("")) {
            return URIUtils.replaceNameSpaceEx(entity);
        }
        return entityLabel;
    }

    public String getEntityViewLabel() {
        if (isMeta) {
            return "";
        }
        if (sddoUri != null && !sddoUri.equals("") && getObject() != null) {
            return "[" + getObject().getEntityLabel() + "]";
        }
        if (sddoUri == null || sddoUri.equals("")) {
            if (sdd != null && (!sdd.getIdLabel().equals("") || !sdd.getOriginalIdLabel().equals(""))) {
                return "[inferred from DefaultObject]";
            }
            return "";
        } else {
            return getEntityLabel();
        }
    }

    public String getAnnotatedEntity() {
        String annotation;
        if (entityLabel.equals("")) {
            if (entity == null || entity.equals("")) {
                return "";
            }
            annotation = URIUtils.replaceNameSpaceEx(entity);
        } else {
            annotation = entityLabel;
        }
        if (!getEntityNamespace().equals("")) {
            annotation += " [" + getEntityNamespace() + "]";
        } 
        return annotation;
    }

    public List<String> getAttributes() {
        if (attributes == null) {
            return new ArrayList<String>();
        } else {
            return attributes;
        }
    }
    
    public String getAttributeString() {
        if (attributes == null) {
            return "";
        }
        
        return String.join("; ", attributes);
    }
    
    public String getReversedAttributeString() {
        if (attributes == null) {
            return "";
        }
        
        // Remove duplicates
        List<String> uniqueAttributes = new ArrayList<String>();
        for (String attrib : attributes) {
            if (!uniqueAttributes.contains(attrib)) {
                uniqueAttributes.add(attrib);
            }
        }

        String result = "";
        for (String attrib : uniqueAttributes) {
            if (result.equals("")) {
                result = attrib;
            } else {
                result = attrib + "; " + result;
            }
        }
        
        return result;
    }

    public List<String> getAttributeNamespace() {
        if (attributes == Arrays.asList("")) {
            return attributes;
        }
        List<String> answer = new ArrayList<String>();
        for (String attr : attributes) {
            answer.add(URIUtils.replaceNameSpaceEx(attr.replace("<","").replace(">","")));
        }
        return answer;
    }

    public void setAttributes(List<String> attributes) {
        this.attributes = attributes;
        if (attributes == null || attributes.size() < 1 ) {
            this.attributeLabels = Arrays.asList("");
        } else {
            List<String> answer = new ArrayList<String>();
            for (String attr : attributes) {
                if (FirstLabel.getPrettyLabel(attr).equals("")) {
                    answer.add(attr);
                } else {
                    answer.add(FirstLabel.getPrettyLabel(attr));
                }
            }
            this.attributeLabels = answer;
        }

        this.isMeta = true;

        for (String attr : attributes) {
            if (!SDD.METASDDA.contains(URIUtils.replaceNameSpaceEx(attr))) {
                this.isMeta = false;
            }
        }      
    }

    public List<String> getAttributeLabels() {
        return attributeLabels;
    }

    public String getConcatAttributeLabel() {
        return String.join(" ", attributeLabels);
    }

    public List<String> getAnnotatedAttribute() {
        List<String> annotation;
        if (attributeLabels.equals(Arrays.asList(""))) {
            if (attributes == null || attributes.equals(Arrays.asList(""))) {
                return Arrays.asList("");
            }
            annotation = Arrays.asList("");
        } else {
            annotation = attributeLabels;
        }
        if (!getAttributeNamespace().equals(Arrays.asList(""))) {
            for (String anno : annotation) {
                anno += " [" + URIUtils.replaceNameSpaceEx(anno.replace("<","").replace(">","")) + "]";	
            }
        }
        return annotation;
    }

    public String getInRelationToUri() {
        String inRelationToUri = "";
        for (String key : relations.keySet()) {
            inRelationToUri = relations.get(key);
            break;
        }
        return inRelationToUri;
    }

    public String getInRelationToLabel() {
        String inRelationTo = getInRelationToUri();
        if (inRelationTo == null || inRelationTo.equals("")) {
            return "";
        } else {
            return FirstLabel.getPrettyLabel(inRelationTo);
        }
    }

    public String getInRelationToUri(String relationUri) {
        //System.out.println("[SDDA] relations: " + relations);
        if (relations.containsKey(relationUri)) {
            return relations.get(relationUri);
        }

        return "";
    }

    public void addRelation(String relationUri, String inRelationToUri) {
        relations.put(relationUri, inRelationToUri);
    }

    public String getUnit() {
        if (unit == null) {
            return "";
        } else {
            return unit;
        }
    }

    public String getUnitNamespace() {
        if (unit == "") {
            return "";
        }
        return URIUtils.replaceNameSpaceEx(unit.replace("<","").replace(">",""));
    }

    public void setUnit(String unit) {
        this.unit = unit;
        if (unit == null || unit.equals("")) {
            this.unitLabel = "";
        } else {
            this.unitLabel = FirstLabel.getPrettyLabel(unit);
        }
    }

    public String getUnitLabel() {
        if (unitLabel.equals("")) {
            return URIUtils.replaceNameSpaceEx(unit);
        }
        return unitLabel;
    }

    public String getAnnotatedUnit() {
        String annotation;
        if (unitLabel.equals("")) {
            if (unit == null || unit.equals("")) {
                return "";
            }
            annotation = URIUtils.replaceNameSpaceEx(unit);
        } else {
            annotation = unitLabel;
        }
        if (!getUnitNamespace().equals("")) {
            annotation += " [" + getUnitNamespace() + "]";
        } 
        return annotation;
    }

    public String getObjectUri() {
        return sddoUri;
    }

    public void setObjectUri(String sddoUri) {
        this.sddoUri = sddoUri;
    }

    public SDDObject getObject() {
        if (sddoUri == null || sddoUri.equals("")) {
            return null;
        }
        return SDDObject.find(sddoUri);
    }

    public String getObjectNamespace() {
        if (sddoUri == null || sddoUri.equals("")) {
            return "";
        }
        return URIUtils.replaceNameSpaceEx(sddoUri.replace("<","").replace(">",""));
    }

    public String getObjectViewLabel() {
        /*
        if (attributes.equals(URIUtils.replaceNameSpaceEx("hasco:originalID"))) {
            return "[DefaultObject]";
        }
        if (isMeta) {
            return "";
        }
         */
        if (sddoUri == null || sddoUri.equals("")) {
            if (sdd != null && (!sdd.getIdLabel().equals("") || !sdd.getOriginalIdLabel().equals(""))) {
                return "[DefaultObject]";
            }
            return "";
        } else {
            SDDObject sddo = SDDObject.find(sddoUri);
            if (sddo == null || sddo.getLabel() == null || sddo.getLabel().equals("")) {
                return sddoUri;
            }
            return sddo.getLabel();
        }
    }

    public String getEventUri() {
        return sddeUri;
    }

    public void setEventUri(String sddeUri) {
        this.sddeUri = sddeUri;
    }
    
    /*
    public SDDEvent getEvent() {
        if (sddeUri == null || sddeUri.equals("")) {
            return null;
        }
        return SDDEvent.find(sddeUri);
	}*/
    
    public SDDObject getEvent() {
        if (sddeUri == null || sddeUri.equals("")) {
            return null;
        }
        return SDDObject.find(sddeUri);
    }
    
    public String getEventNamespace() {
        if (sddeUri == null || sddeUri.equals("")) {
            return "";
        }
        return URIUtils.replaceNameSpaceEx(sddeUri.replace("<","").replace(">",""));
    }

    public String getEventViewLabel() {
        if (isMeta) {
            return "";
        }
        if (sddeUri == null || sddeUri.equals("")) {
            if (sdd != null && !sdd.getTimestampLabel().equals("")) {
                return "[value at label " + sdd.getTimestampLabel() + "]";
            }
            return "";
        } else {
            //SDDEvent sdde = SDDEvent.find(sddeUri);
            SDDObject sdde = SDDObject.find(sddeUri);
            if (sdde == null || sdde.getLabel() == null || sdde.getLabel().equals("")) {
                return sddeUri;
            }
            return sdde.getLabel();
        }
    }

    public static int getNumberSDDAs() {
        String query = "";
        query += NameSpaces.getInstance().printSparqlNameSpaceList();
        query += "select distinct (COUNT(?x) AS ?tot) where {" + 
                " ?x a <http://sirapi.org/ont/hasco/SDDAttribute> } ";

        //System.out.println("Study query: " + query);

        try {
            ResultSetRewindable resultsrw = SPARQLUtils.select(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), query);

            if (resultsrw.hasNext()) {
                QuerySolution soln = resultsrw.next();
                return Integer.parseInt(soln.getLiteral("tot").getString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static SDDAttribute find(String sdda_uri) {

        // debug
        if ( sdda_uri.contains("zvalue-bwt-gage-sex-d")) {
            int x = 1;
        }
        if ( sdda_uri.contains("ZBFA")) {
            int x = 1;
        }
        if ( sdda_uri.contains("ZHFA")) {
            int x = 1;
        }
        // end of debug

        if (SDDAttribute.getCache().get(sdda_uri) != null) {
            return SDDAttribute.getCache().get(sdda_uri);
        }
        SDDAttribute sdda = null;
        //System.out.println("Looking for data acquisition schema attribute with URI <" + sdda_uri + ">");

        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() + 
                "SELECT ?partOfSchema ?hasEntity ?hasAttribute " + 
                " ?hasUnit ?hasSDDO ?hasSDDE ?hasSource ?isPIConfirmed ?relation ?inRelationTo ?label WHERE { \n" + 
                "    <" + sdda_uri + "> a hasco:SDDAttribute . \n" + 
                "    <" + sdda_uri + "> hasco:partOfSchema ?partOfSchema . \n" + 
                "    OPTIONAL { <" + sdda_uri + "> hasco:hasEntity ?hasEntity } . \n" + 
                "    OPTIONAL { <" + sdda_uri + "> hasco:hasAttribute/rdf:rest*/rdf:first ?hasAttribute } . \n" +
                "    OPTIONAL { <" + sdda_uri + "> hasco:hasUnit ?hasUnit } . \n" + 
                "    OPTIONAL { <" + sdda_uri + "> hasco:hasEvent ?hasSDDE } . \n" + 
                "    OPTIONAL { <" + sdda_uri + "> hasco:isAttributeOf ?hasSDDO } . \n" + 
                "    OPTIONAL { <" + sdda_uri + "> hasco:hasSource ?hasSource } . \n" + 
                "    OPTIONAL { <" + sdda_uri + "> hasco:isPIConfirmed ?isPIConfirmed } . \n" + 
                "    OPTIONAL { <" + sdda_uri + "> hasco:Relation ?relation . <" + sdda_uri + "> ?relation ?inRelationTo . } . \n" + 
                "    OPTIONAL { <" + sdda_uri + "> rdfs:label ?label } . \n" +
                "}";

        //System.out.println("SDDAttribute find() queryString: \n" + queryString);

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        if (!resultsrw.hasNext()) {
            System.out.println("[WARNING] SDDAttribute. Could not find SDDA with URI: <" + sdda_uri + ">");
            return sdda;
        }

        String localNameStr = "";
        String labelStr = "";
        String partOfSchemaStr = "";
        String positionStr = "";
        String entityStr = "";
        String attributeStr = "";
        List<String> attributeList = new ArrayList<String>();
        String unitStr = "";
        String sddoUriStr = "";
        String sddeUriStr = "";
        String inRelationToUri = "";
        String relationUri = "";

        Map<String,String> relationMap = new HashMap<>();
        while (resultsrw.hasNext()) {        	
            QuerySolution soln = resultsrw.next();

            /*
             *  The label should be the exact value in the SDD, e.g., cannot be altered be something like
             *  FirstLabel.getPrettyLabel(sdda_uri) since that would prevent the matching of the label with 
             *  the column header of the data acquisition file/message
             */
            labelStr = soln.get("label").toString();

            if (soln.get("partOfSchema") != null) {
                partOfSchemaStr = soln.get("partOfSchema").toString();
            }
            if (soln.get("hasEntity") != null) {
                entityStr = soln.get("hasEntity").toString();
            }
            if (soln.get("hasAttribute") != null) {
                attributeList.add(soln.get("hasAttribute").toString());
            }
            if (soln.get("hasUnit") != null) {
                unitStr = soln.get("hasUnit").toString();
            }
            if (soln.get("hasSDDO") != null) {
                sddoUriStr = soln.get("hasSDDO").toString();
            }
            if (soln.get("hasSDDE") != null) {
                sddeUriStr = soln.get("hasSDDE").toString();
            }
            if (soln.get("inRelationTo") != null) {
                inRelationToUri = soln.get("inRelationTo").toString();
            }
            if (soln.get("relation") != null) {
                relationUri = soln.get("relation").toString();
            }

            if ( relationUri != null && relationUri.length() > 0 && inRelationToUri != null && inRelationToUri.length() > 0 ) {
                relationMap.put(relationUri, inRelationToUri);
                relationUri = "";
                inRelationToUri = "";
            }

        }

        // debug
        if ( sdda_uri.contains("zvalue-bwt-gage-sex-d")) {
            int x = 1;
        }
        if ( sdda_uri.contains("ZBFA")) {
            int x = 1;
        }
        if ( sdda_uri.contains("ZHFA")) {
            int x = 1;
        }
        // end of debug

        sdda = new SDDAttribute(
                sdda_uri,
                localNameStr,
                labelStr,
                partOfSchemaStr,
                positionStr,
                entityStr,
                attributeList,
                unitStr,
                sddeUriStr,
                sddoUriStr);

        for ( Map.Entry<String, String> entry : relationMap.entrySet() ) {
            sdda.addRelation(entry.getKey(), entry.getValue());
        }

	    SDDAttribute.getCache().put(sdda_uri,sdda);
        return sdda;
    }

    // Given a study URI, 
    // returns a list of SDDA's
    // (we need to go study -> data acqusition(s) -> data acqusition schema(s) -> data acquisition schema attributes)
    public static List<SDDAttribute> findByStudy(String studyUri){
        //System.out.println("Looking for data acquisition schema attributes from study " + studyUri);
        if (studyUri.startsWith("http")) {
            studyUri = "<" + studyUri + ">";
        }
        List<SDDAttribute> attributes = new ArrayList<SDDAttribute>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() + 
                "SELECT ?uri ?hasEntity ?schemaUri ?attrUri" + 
                " ?hasUnit ?hasSDDO ?hasSDDE ?hasSource ?isPIConfirmed WHERE { " + 
                "    ?da hasco:isDataAcquisitionOf " + studyUri + " .  " +
                "    ?da hasco:hasSchema ?schemaUri .  "+
                "    ?uri hasco:partOfSchema ?schemaUri .  " +
                "    ?uri a hasco:SDDAttribute . " + 
                "    ?uri hasco:hasAttribute ?attrUri . " +
                "} ";
        //System.out.println("[SDDA] query string = \n" + queryString);

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        if (!resultsrw.hasNext()) {
            System.out.println("[WARNING] SDDAttribute. Could not find SDDA's with attribute: " + studyUri);
            return attributes;
        }

        String uriStr = "";

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            if (soln != null) {

                try {
                    if (soln.getResource("uri") != null && soln.getResource("uri").getURI() != null) {
                        uriStr = soln.getResource("uri").getURI();
                        SDDAttribute attr = find(uriStr);
                        attributes.add(attr);
                    }
                } catch (Exception e1) {
                    System.out.println("[ERROR] SDDAttribute. URI: " + uriStr);
                }
            }
        }
        attributes.sort(Comparator.comparing(SDDAttribute::getPositionInt));

        return attributes;
    }

    public static List<String> findUriBySchema(String schemaUri) {
        //System.out.println("Looking for data acquisition schema attribute URIs for <" + schemaUri + ">");

        List<String> attributeUris = new ArrayList<String>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() + 
                "SELECT ?uri  WHERE { \n" + 
                " ?uri a hasco:SDDAttribute . \n" + 
                " ?uri hasco:partOfSchema <" + schemaUri + "> . \n" + 
                "} ";

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        if (!resultsrw.hasNext()) {
            System.out.println("[WARNING] SDDAttribute. Could not find attributes for schema: <" + schemaUri + ">");
            return attributeUris;
        }

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            try {
                if (soln.getResource("uri") != null && soln.getResource("uri").getURI() != null) {
                    String uri = soln.getResource("uri").getURI();
                    attributeUris.add(uri);
                }
            } catch (Exception e1) {
                System.out.println("[ERROR] SDDAttribute.findBySchema() URI: <" + schemaUri + ">");
                e1.printStackTrace();
            }
        }
        return attributeUris;
    }

    public static List<SDDAttribute> findBySchema(String schemaUri) {
        //System.out.println("Looking for data acquisition schema attributes for <" + schemaUri + ">");

        List<SDDAttribute> attributes = new ArrayList<SDDAttribute>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() + 
                "SELECT ?uri ?label WHERE { \n" + 
                " ?uri a hasco:SDDAttribute . \n" + 
                " ?uri hasco:partOfSchema <" + schemaUri + "> . \n" + 
                " ?uri rdfs:label ?label . \n" + 
                " } " + 
                " ORDER BY ?label";

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        if (!resultsrw.hasNext()) {
            System.out.println("[WARNING] SDDAttribute. Could not find attributes for schema: <" + schemaUri + ">");
            return attributes;
        }

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            try {
                if (soln.getResource("uri") != null && soln.getResource("uri").getURI() != null) {
                    String uri = soln.getResource("uri").getURI();
                    SDDAttribute attr = find(uri);
                    attributes.add(attr);
                }
            } catch (Exception e1) {
                System.out.println("[ERROR] SDDAttribute.findBySchema() URI: <" + schemaUri + ">");
                e1.printStackTrace();
            }
        }
        attributes.sort(Comparator.comparing(SDDAttribute::getPositionInt));

        return attributes;
    }

    @Override
    public void save() {
        saveToTripleStore();
    }

    @Override
    public boolean saveToTripleStore() {
        deleteFromTripleStore();

        if (uri == null || uri.equals("")) {
            System.out.println("[ERROR] Trying to save SDDA without assigning an URI");
            return false;
        }
        if (partOfSchema == null || partOfSchema.equals("")) {
            System.out.println("[ERROR] Trying to save SDDA without assigning SDD's URI");
            return false;
        }

        String insert = "";
        insert += NameSpaces.getInstance().printSparqlNameSpaceList();
        insert += INSERT_LINE1;

        if (!getNamedGraph().isEmpty()) {
            insert += " GRAPH <" + getNamedGraph() + "> { ";
        }

        insert += this.getUri() + " a hasco:SDDAttribute . ";
        insert += this.getUri() + " rdfs:label  \"" + label + "\" . ";

        if (partOfSchema.startsWith("http")) {
            insert += this.getUri() + " hasco:partOfSchema <" + partOfSchema + "> .  "; 
        } else {
            insert += this.getUri() + " hasco:partOfSchema " + partOfSchema + " .  "; 
        }

        if (entity.startsWith("http")) {
            insert += this.getUri() + " hasco:hasEntity <" + entity + "> .  ";
        } else {
            insert += this.getUri() + " hasco:hasEntity " + entity + " .  ";
        }

        for (String attribute : attributes) {
            if (attribute.startsWith("http")) {
                insert += this.getUri() + " hasco:hasAttribute <" + attribute + "> .  ";
            } else {
                insert += this.getUri() + " hasco:hasAttribute " + attribute + " . ";
            }
        }

        if (unit.startsWith("http")) {
            insert += this.getUri() + " hasco:hasUnit <" + unit + "> .  ";
        } else {
            insert += this.getUri() + " hasco:hasUnit " + unit + " .  ";
        }

        if (sddeUri != null && !sddeUri.equals("")) {
            if (sddeUri.startsWith("http")) {
                insert += this.getUri() + " hasco:hasEvent <" + sddeUri + "> .  ";
            } else {
                insert += this.getUri() + " hasco:hasEvent " + sddeUri + " .  ";
            }
        }
        if (sddoUri != null && !sddoUri.equals("")) {
            if (sddoUri.startsWith("http")) {
                insert += this.getUri() + " hasco:isAttributeOf <" + sddoUri + "> .  ";
            } else {
                insert += this.getUri() + " hasco:isAttributeOf " + sddoUri + " .  ";
            }
        }

        insert += LINE_LAST;

        try {
            UpdateRequest request = UpdateFactory.create(insert);
            UpdateProcessor processor = UpdateExecutionFactory.createRemote(
                    request, CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_UPDATE));
            processor.execute();
        } catch (QueryParseException e) {
            System.out.println("[ERROR] QueryParseException due to update query: " + insert);
            throw e;
        }

        return true;
    }

    @Override
    public void deleteFromTripleStore() {
        super.deleteFromTripleStore();
        SDDAttribute.resetCache();
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


