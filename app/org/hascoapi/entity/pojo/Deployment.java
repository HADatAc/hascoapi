package org.hascoapi.entity.pojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.annotations.PropertyField;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.State;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.PROV;
import org.hascoapi.vocabularies.RDF;
import org.hascoapi.vocabularies.RDFS;
import org.hascoapi.vocabularies.VSTOI;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;


public class Deployment extends HADatAcThing {

    /* 
    public static String INDENT1 = "     ";
    public static String INSERT_LINE1 = "INSERT DATA {  ";
    public static String DELETE_LINE1 = "DELETE WHERE {  ";
    public static String LINE3 = INDENT1 + "a         vstoi:Deployment;  ";
    public static String DELETE_LINE3 = INDENT1 + " ?p ?o . ";
    public static String LINE3_LEGACY = INDENT1 + "a         vstoi:Deployment;  ";
    public static String PLATFORM_PREDICATE =     INDENT1 + "vstoi:hasPlatform        ";
    public static String INSTRUMENT_PREDICATE =   INDENT1 + "hasco:hasInstrument    ";
    public static String DETECTOR_PREDICATE =     INDENT1 + "hasco:hasDetector      ";
    public static String START_TIME_PREDICATE =   INDENT1 + "prov:startedAtTime		  ";
    public static String END_TIME_PREDICATE =     INDENT1 + "prov:endedAtTime		  ";
    public static String TIME_XMLS =   "\"^^<http://www.w3.org/2001/XMLSchema#dateTime> .";
    public static String LINE_LAST = "}  ";
    */

    @PropertyField(uri="prov:startedAtTime")
    private String startedAt;

    @PropertyField(uri="prov:endedAtTime")
    private String endedAt;

    @PropertyField(uri="vstoi:isLegacy")
    private String isLegacy;

    @PropertyField(uri="hasco:hasInstrument")
    private String instrumentUri;

    @PropertyField(uri="vstoi:hasPlatform")
    private String platformUri;

    @PropertyField(uri="hasco:hasDetector")
    private List<String> detectorUri;

    @PropertyField(uri = "vstoi:hasVersion")
    private String hasVersion;

    @PropertyField(uri = "vstoi:hasSIRManagerEmail")
    private String hasSIRManagerEmail;

    //
    //  CACHE
    //

    private static Map<String, Deployment> DPLCache;

    private static Map<String, Deployment> getCache() {
        if (DPLCache == null) {
            DPLCache = new HashMap<String, Deployment>(); 
        }
        return DPLCache;
    }

    public static void resetCache() {
        DPLCache = null;
    }

    //
    //  CONSTRUCT
    //

    public Deployment() {
        startedAt = null;
        endedAt = null;
        instrumentUri = null;
        platformUri = null;
        isLegacy = "F";
        detectorUri = new ArrayList<String>();
        Deployment.getCache();
    }

    //
    //  GETS/SETS
    //

    public String getIsLegacy() {
        return this.isLegacy;
    }
    public void setIsLegacy(String isLegacy) {
        if (isLegacy == null && 
            (!isLegacy.equals("T") || !isLegacy.equals("F"))) {
            System.out.println("[ERROR] Deployment.java: isLegacy needs to be 'T' or 'F'.");
        }
        this.isLegacy = isLegacy;
    }

    public String getStartedAt() {
        //DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
        //return formatter.withZone(DateTimeZone.UTC).print(startedAt);
        return startedAt;
    }
    //public String getStartedAtXsd() {
        //DateTimeFormatter formatter = ISODateTimeFormat.dateTimeNoMillis();
        //return formatter.withZone(DateTimeZone.UTC).print(startedAt);
    //}
    public void setStartedAt(String startedAt) {
        //DateTimeFormatter formatter = DateTimeFormat.forPattern("EEE MMM dd HH:mm:ss zzz yyyy");
        //this.startedAt = formatter.parseDateTime(startedAt);
        this.startedAt = startedAt;
    }
    public void setStartedAtXsd(DateTime startedAtRaw) {
        DateTimeFormatter formatterNoMillis = ISODateTimeFormat.dateTimeNoMillis();
        this.startedAt = startedAtRaw.toString(formatterNoMillis);
    }
    public void setStartedAtXsdWithMillis(String startedAtString) {
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        DateTime startedAtRaw = DateTime.parse(startedAtString, formatter);
        DateTimeFormatter formatterISO = ISODateTimeFormat.dateTime();
        this.startedAt = startedAtRaw.toString(formatterISO);
    }

    public String getEndedAt() {
        //DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
        //return formatter.withZone(DateTimeZone.UTC).print(endedAt);
        return endedAt;
    }
    public void setEndedAt(String endedAt) {
        //DateTimeFormatter formatter = DateTimeFormat.forPattern("EEE MMM dd HH:mm:ss zzz yyyy");
        //this.endedAt = formatter.parseDateTime(endedAt);
        this.endedAt = endedAt;
    }
    public void setEndedAtXsd(String endedAtString) {
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        DateTime endedAtRaw = DateTime.parse(endedAtString, formatter);
        DateTimeFormatter formatterNoMillis = ISODateTimeFormat.dateTimeNoMillis();
        this.endedAt = endedAtRaw.toString(formatterNoMillis);
    }
    public void setEndedAtXsdWithMillis(String endedAtString) {
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        DateTime endedAtRaw = DateTime.parse(endedAtString, formatter);
        DateTimeFormatter formatterISO = ISODateTimeFormat.dateTime();
        this.endedAt = endedAtRaw.toString(formatterISO);
    }

    public String getInstrumentUri() {
        return instrumentUri;
    }
    public Instrument getInstrument() {
        if (instrumentUri == null || instrumentUri.isEmpty()) {
            return null;
        }
        return Instrument.find(instrumentUri);
    }
    public void setInstrumentUri(String instrumentUri) {
        this.instrumentUri = instrumentUri;
    }

    public String getPlatformUri() {
        return platformUri;
    }
    public Platform getPlatform() {
        if (platformUri == null || platformUri.isEmpty()) {
            return null;
        }
        return Platform.find(platformUri);
    }
    public void setPlatformUri(String platformUri) {
        this.platformUri = platformUri;
    }

    public List<String> getDetectorUri() {
        return detectorUri;
    }
    public List<Detector> getDetector() {
        List<Detector> detectors = new ArrayList<Detector>();
        if (detectorUri != null && detectorUri.size() > 0) {
            for (String detUri : detectorUri) {
                if (detUri != null) {
                    Detector det = Detector.findDetector(detUri);
                    if (det != null) {
                        detectors.add(det);
                    }
                }
            }
        }
        return detectors;
    }
    public void addDetectorUri(String detectorUri) {
        if (detectorUri != null) {
            if (!detectorUri.contains(detectorUri)) {
                this.detectorUri.add(detectorUri);
            }
        }
    }

    public String getHasVersion() {
        return hasVersion;
    }
    public void setHasVersion(String hasVersion) {
        this.hasVersion = hasVersion;
    }

    public String getHasSIRManagerEmail() {
        return hasSIRManagerEmail;
    }
    public void setHasSIRManagerEmail(String hasSIRManagerEmail) {
        this.hasSIRManagerEmail = hasSIRManagerEmail;
    }

    /* 
    public void saveEndedAtTime() {
        String insert = "";
        if (this.getEndedAt() != null) {
            insert += NameSpaces.getInstance().printSparqlNameSpaceList();
            insert += INSERT_LINE1;
            insert += "<" + this.getUri() + ">  ";
            insert += END_TIME_PREDICATE + "\"" + this.getEndedAt() + TIME_XMLS + "  ";
            insert += LINE_LAST;
            UpdateRequest request = UpdateFactory.create(insert);
            UpdateProcessor processor = UpdateExecutionFactory.createRemote(request,
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_UPDATE)); 
            processor.execute();
        }
    }
    */

    public void close(String endedAt) {
        setEndedAtXsd(endedAt);
        List<Stream> list = Stream.find(this, true);
        if (!list.isEmpty()) {
            Stream dc = list.get(0);
            dc.close(endedAt);
        }
        //saveEndedAtTime();
        save();
    }

    public static Deployment create(String uri) {
        Deployment deployment = new Deployment();
        deployment.setUri(uri);
        
        return deployment;
    }

    public static Deployment createLegacy(String uri) {
        Deployment deployment = new Deployment();
        deployment.setUri(uri);
        deployment.setIsLegacy("T");

        return deployment;
    }

    public static Deployment find(String deployment_uri) {
        if (Deployment.getCache().get(deployment_uri) != null) {
            return Deployment.getCache().get(deployment_uri);
        }

        //System.out.println("Current URI for FIND DEPLOYMENT: " + deployment_uri);

        Deployment deployment = null;
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
        if (deployment_uri.startsWith("http")) {
            queryString += "DESCRIBE <" + deployment_uri + ">";
        } else {
            queryString += "DESCRIBE " + deployment_uri;
        }
        // System.out.println("FIND DEPLOYMENT (queryString): " + queryString);
        
        Model model = SPARQLUtils.describe(CollectionUtil.getCollectionPath(
                CollectionUtil.Collection.SPARQL_QUERY), queryString);

        StmtIterator stmtIterator = model.listStatements();
        if (!model.isEmpty()) {
            deployment = new Deployment();
            deployment.setUri(deployment_uri);
        }

        while (stmtIterator.hasNext()) {
            Statement statement = stmtIterator.next();
            RDFNode object = statement.getObject();
            String str = URIUtils.objectRDFToString(object);
            if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
                deployment.setLabel(str);
            } else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
                deployment.setTypeUri(str);
            } else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
                deployment.setHascoTypeUri(str);
            } else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
                deployment.setComment(str);
            } else if (statement.getPredicate().getURI().equals(HASCO.HAS_INSTRUMENT)) {
                deployment.setInstrumentUri(str);
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_PLATFORM)) {
                deployment.setPlatformUri(str);;
            } else if (statement.getPredicate().getURI().equals(HASCO.HAS_DETECTOR)) {
                deployment.addDetectorUri(str);
            } else if (statement.getPredicate().getURI().equals(PROV.STARTED_AT_TIME)) {
                deployment.setStartedAtXsdWithMillis(str);
            } else if (statement.getPredicate().getURI().equals(PROV.ENDED_AT_TIME)) {
                deployment.setEndedAtXsdWithMillis(str);
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_VERSION)) {
                deployment.setHasVersion(str);
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
                deployment.setHasSIRManagerEmail(str);
            }
        }
        
        Deployment.getCache().put(deployment_uri, deployment);
        return deployment;
    }

    public static List<Deployment> find(State state) {
        List<Deployment> deployments = new ArrayList<Deployment>();
        String queryString = "";
        if (state.getCurrent() == State.ACTIVE) { 
            queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
            		"SELECT ?uri WHERE { " + 
                    "   ?uri a vstoi:Deployment . " + 
                    "   FILTER NOT EXISTS { ?uri prov:endedAtTime ?enddatetime . } " + 
                    "} " + 
                    "ORDER BY DESC(?datetime) ";
        } else {
            if (state.getCurrent() == State.CLOSED) {
                queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                        "SELECT ?uri WHERE { " + 
                        "   ?uri a vstoi:Deployment . " + 
                        "   ?uri prov:startedAtTime ?startdatetime .  " + 
                        "   ?uri prov:endedAtTime ?enddatetime .  " + 
                        "} " +
                        "ORDER BY DESC(?datetime) ";
            } else {
                if (state.getCurrent() == State.ALL) {
                    queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                            "SELECT ?uri WHERE { " + 
                            "   ?uri a vstoi:Deployment . " + 
                            "} " +
                            "ORDER BY DESC(?datetime) ";
                } else {
                    System.out.println("Deployment.java: no valid state specified.");
                    return null;
                }
            }
        }
        
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        Deployment dep = null;
        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            if (soln != null && soln.getResource("uri").getURI()!= null) { 
                dep = Deployment.find(soln.getResource("uri").getURI()); 
            }
            deployments.add(dep);
        }

        return deployments;
    }

    public static List<Deployment> findWithPages(State state, int pageSize, int offset) {
        List<Deployment> deployments = new ArrayList<Deployment>();
        String queryString = "";
        if (state.getCurrent() == State.ACTIVE) { 
            queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
            		"SELECT ?uri WHERE { " + 
                    "   ?uri a vstoi:Deployment . " + 
                    "   FILTER NOT EXISTS { ?uri prov:endedAtTime ?enddatetime . } " + 
                    "} " + 
                    " ORDER BY DESC(?datetime) " +
            		" LIMIT " + pageSize + 
            		" OFFSET " + offset;
        } else {
            if (state.getCurrent() == State.CLOSED) {
                queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                        "SELECT ?uri WHERE { " + 
                        "   ?uri a vstoi:Deployment . " + 
                        "   ?uri prov:startedAtTime ?startdatetime .  " + 
                        "   ?uri prov:endedAtTime ?enddatetime .  " + 
                        "} " +
                        " ORDER BY DESC(?datetime) " +
                        " LIMIT " + pageSize + 
                        " OFFSET " + offset;
            } else {
                if (state.getCurrent() == State.ALL) {
                    queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                            "SELECT ?uri WHERE { " + 
                            "   ?uri a vstoi:Deployment . " + 
                            "} " +
                            " ORDER BY DESC(?datetime) " +
                            " LIMIT " + pageSize + 
                            " OFFSET " + offset;
                } else {
                    System.out.println("Deployment.java: no valid state specified.");
                    return null;
                }
            }
        }
        
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        Deployment dep = null;
        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            if (soln != null && soln.getResource("uri").getURI()!= null) { 
                dep = Deployment.find(soln.getResource("uri").getURI()); 
            }
            deployments.add(dep);
        }

        return deployments;
    }

    public static int getNumberDeployments(State state) {
        String query = "";
        if (state.getCurrent() == State.ACTIVE) { 
            query = NameSpaces.getInstance().printSparqlNameSpaceList() +
            		"SELECT (count(?uri) as ?tot) WHERE { " + 
                    "   ?uri a vstoi:Deployment . " + 
                    "   FILTER NOT EXISTS { ?uri prov:endedAtTime ?enddatetime . } " + 
                    "} "; 
        } else {
            if (state.getCurrent() == State.CLOSED) {
                query = NameSpaces.getInstance().printSparqlNameSpaceList() +
                        "SELECT (count(?uri) as ?tot) WHERE { " + 
                        "   ?uri a vstoi:Deployment . " + 
                        "   ?uri prov:startedAtTime ?startdatetime .  " + 
                        "   ?uri prov:endedAtTime ?enddatetime .  " + 
                        "} ";
            } else {
                if (state.getCurrent() == State.ALL) {
                    query = NameSpaces.getInstance().printSparqlNameSpaceList() +
                            "SELECT (count(?uri) as ?tot) WHERE { " + 
                            "   ?uri a vstoi:Deployment . " + 
                            "} ";
                } else {
                    System.out.println("Deployment.java: no valid state specified.");
                    return 0;
                }
            }
        }
                
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

    public static List<Deployment> findWithGeoReference() {
        List<Deployment> deployments = new ArrayList<Deployment>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?platModel rdfs:subClassOf* vstoi:Platform . " + 
                " ?plat a ?platModel ." +
                " ?plat hasco:hasFirstCoordinate ?lat . " +
                " ?plat hasco:hasSecondCoordinate ?lon . " +
                " ?plat hasco:hasFirstCoordinateCharacteristic <" + Platform.LAT + "> . " +
                " ?plat hasco:hasSecondCoordinateCharacteristic <" + Platform.LONG + "> . " +
                " ?uri vstoi:hasPlatform ?plat . " +
                "} ";

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            Deployment deployment = find(soln.getResource("uri").getURI());
            deployments.add(deployment);
        }			

        return deployments;
    }
    
    public static List<Deployment> findByPlatformAndStatus(String plat_uri, State state) {
    	if (plat_uri == null) {
    		return null;
    	}
        List<Deployment> deployments = new ArrayList<Deployment>();
    	String p_uri = plat_uri;
    	if (plat_uri.startsWith("http")) {
    		p_uri = "<" + plat_uri + ">"; 
    	}
        String queryString = "";
        if (state.getCurrent() == State.ACTIVE) { 
            queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
            		"SELECT ?uri WHERE { " + 
                    "   ?uri a vstoi:Deployment . " + 
                    "   ?uri vstoi:hasPlatform " + p_uri + " . " + 
                    "   FILTER NOT EXISTS { ?uri prov:endedAtTime ?enddatetime . } " + 
                    "} " + 
                    "ORDER BY DESC(?datetime) ";
        } else {
            if (state.getCurrent() == State.CLOSED) {
                queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                        "SELECT ?uri WHERE { " + 
                        "   ?uri a vstoi:Deployment . " + 
                        "   ?uri vstoi:hasPlatform " + p_uri + " . " + 
                        "   ?uri prov:startedAtTime ?startdatetime .  " + 
                        "   ?uri prov:endedAtTime ?enddatetime .  " + 
                        "} " +
                        "ORDER BY DESC(?datetime) ";
            } else {
                if (state.getCurrent() == State.ALL) {
                    queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                            "SELECT ?uri WHERE { " + 
                            "   ?uri a vstoi:Deployment . " + 
                            "   ?uri vstoi:hasPlatform " + p_uri + " . " + 
                            "} " +
                            "ORDER BY DESC(?datetime) ";
                } else {
                    System.out.println("Deployment.java: no valid state specified.");
                    return null;
                }
            }
        }
                
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        Deployment dep = null;
        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            if (soln != null && soln.getResource("uri").getURI()!= null) { 
                dep = Deployment.find(soln.getResource("uri").getURI()); 
            }
            deployments.add(dep);
        }

        return deployments;
    }

    public static List<Deployment> findByReferenceLayoutAndStatus(String plat_uri, State state) {
    	if (plat_uri == null) {
    		return null;
    	}
        List<Deployment> deployments = new ArrayList<Deployment>();
    	String p_uri = plat_uri;
    	if (plat_uri.startsWith("http")) {
    		p_uri = "<" + plat_uri + ">"; 
    	}
        String queryString = "";
        if (state.getCurrent() == State.ACTIVE) { 
            queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
            		"SELECT ?uri WHERE { " + 
                    "   ?uri a vstoi:Deployment . " + 
                    "   ?uri vstoi:hasPlatform ?plt . " + 
                    "   ?plt hasco:hasReferenceLayout " + p_uri + "  . " + 
                    "   FILTER NOT EXISTS { ?uri prov:endedAtTime ?enddatetime . } " + 
                    "} " + 
                    "ORDER BY DESC(?datetime) ";
        } else {
            if (state.getCurrent() == State.CLOSED) {
                queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                        "SELECT ?uri WHERE { " + 
                        "   ?uri a vstoi:Deployment . " + 
                        "   ?uri vstoi:hasPlatform ?plt . " + 
                        "   ?plt hasco:hasReferenceLayout " + p_uri + "  . " + 
                        "   ?uri prov:startedAtTime ?startdatetime .  " + 
                        "   ?uri prov:endedAtTime ?enddatetime .  " + 
                        "} " +
                        "ORDER BY DESC(?datetime) ";
            } else {
                if (state.getCurrent() == State.ALL) {
                    queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                            "SELECT ?uri WHERE { " + 
                            "   ?uri a vstoi:Deployment . " + 
                            "   ?uri vstoi:hasPlatform ?plt . " + 
                            "   ?plt hasco:hasReferenceLayout " + p_uri + "  . " + 
                            "} " +
                            "ORDER BY DESC(?datetime) ";
                } else {
                    System.out.println("Deployment.java: no valid state specified.");
                    return null;
                }
            }
        }
                
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        Deployment dep = null;
        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            if (soln != null && soln.getResource("uri").getURI()!= null) { 
                dep = Deployment.find(soln.getResource("uri").getURI()); 
            }
            deployments.add(dep);
        }

        return deployments;
    }

    @Override
    public void save() {
        System.out.println("Saving platform [" + uri + "]");
        saveToTripleStore();
        resetCache();
    }

    @Override
    public void delete() {
        deleteFromTripleStore();
        resetCache();
    }

    /* 
    @Override
    public boolean saveToTripleStore() {
        String insert = "";
        insert += NameSpaces.getInstance().printSparqlNameSpaceList();
        insert += INSERT_LINE1;
        
        if (!getNamedGraph().isEmpty()) {
            insert += " GRAPH <" + getNamedGraph() + "> { ";
        }
        
        insert += "<" + this.getUri() + ">  ";
        if (this.isLegacy()) {
            insert += LINE3_LEGACY;
        } else {
            insert += LINE3;
        }
        insert += PLATFORM_PREDICATE + "<" + this.getPlatformUri() + "> ;   ";
        insert += INSTRUMENT_PREDICATE + "<" + this.getInstrumentUri() + "> ;   ";
        Iterator<String> i = this.detectorUri.iterator();
        while (i.hasNext()) {
            insert += DETECTOR_PREDICATE + "<" + i.next() + "> ;   ";
        }
        insert += START_TIME_PREDICATE + "\"" + this.getStartedAt() + TIME_XMLS + "  ";
        if (this.endedAt != null) {
            insert += END_TIME_PREDICATE + "\"" + this.getEndedAt() + TIME_XMLS + "  ";
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
    */

}
