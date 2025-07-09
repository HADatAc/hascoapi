package org.hascoapi.entity.pojo;

import com.fasterxml.jackson.annotation.JsonFilter;

import java.util.ArrayList;
import java.util.List;


import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.hascoapi.annotations.PropertyField;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.IngestionLogger;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.RDF;
import org.hascoapi.vocabularies.RDFS;
import org.hascoapi.vocabularies.VSTOI;

@JsonFilter("daFilter")
public class DA extends MetadataTemplate implements Comparable<DA>  {

    public String className = "hasco:DataAcquisition";

    @PropertyField(uri = "hasco:hasDD")
    private String hasDDUri;

    @PropertyField(uri = "hasco:hasSDD")
    private String hasSDDUri;
    
    @PropertyField(uri = "hasco:isMemberOf")
    private String isMemberOfUri;
    
    @PropertyField(uri="hasco:hasNumberDataPoints")
    private String numberDataPoints;

    @PropertyField(uri="hasco:hasTotalRecordedMessages")
    private String totalRecordedMessages;

    public String getHasDDUri() {
        return hasDDUri;
    }
    public void setHasDDUri(String hasDDUri) {
        this.hasDDUri = hasDDUri;
    }
    public DD getHasDD() {
        if (this.hasDDUri == null) {
            return null;
        }
        return DD.find(hasDDUri);
    }	

    public String getHasSDDUri() {
        return hasSDDUri;
    }
    public void setHasSDDUri(String hasSDDUri) {
        this.hasSDDUri = hasSDDUri;
    }
    public SDD getHasSDD() {
        if (this.hasSDDUri == null) {
            return null;
        }
        return SDD.find(hasSDDUri);
    }	

    public String getIsMemberOfUri() {
        return isMemberOfUri;
    }
    public void setIsMemberOfUri(String isMemberOfUri) {
        this.isMemberOfUri = isMemberOfUri;
    }
    public Study getIsMemberOf() {
        if (this.isMemberOfUri == null) {
            return null;
        }
        return Study.find(isMemberOfUri);
    }	

    public String getHasNumberDataPoints() {
        return numberDataPoints;
    }
    public void setHasNumberDataPoints(String numberDataPoints) {
        this.numberDataPoints = numberDataPoints;
    }

    public String getHasTotalRecordedMessages() {
        return totalRecordedMessages;
    }
    public void setHasTotalRecordedMessages(String totalRecordedMessages) {
        this.totalRecordedMessages = totalRecordedMessages;
    }

    public DA() {
        totalRecordedMessages = "0";
    }

    public static List<DA> findByStudy(Study study, int pageSize, int offset) {
        if (study == null) {
            return new ArrayList<DA>();
        }
        String query = NameSpaces.getInstance().printSparqlNameSpaceList() + 
                " SELECT ?uri " +
                " WHERE {  ?uri hasco:isMemberOf <" + study.getUri() + "> .  " +
				"          ?uri hasco:hascoType <" + HASCO.DATA_ACQUISITION + "> . " +
				"          ?uri rdfs:label ?label . " +
                " } " +
                " ORDER BY ASC(?label) " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;
        //System.out.println(query);
        return DA.findManyByQuery(query);
    }        
    
    public static int findTotalByStudy(Study study) {
        if (study == null) {
            return 0;
        }
        String query = NameSpaces.getInstance().printSparqlNameSpaceList() + 
                " SELECT (count(?uri) as ?tot)  " +
                " WHERE {  ?uri hasco:isMemberOf <" + study.getUri() + "> .  " +
				"          ?uri hasco:hascoType <" + HASCO.DATA_ACQUISITION + "> . " +
                " }";
        return GenericFind.findTotalByQuery(query);
    }        
    
    public static List<DA> findByStream(Stream stream, int pageSize, int offset) {
        if (stream == null) {
            return new ArrayList<DA>();
        }
        String query = NameSpaces.getInstance().printSparqlNameSpaceList() + 
                " SELECT ?uri " +
                " WHERE {  ?uri hasco:hascoType <" + HASCO.DATA_ACQUISITION + "> . " +
				"          ?uri hasco:hasDataFile ?datafile . " +
                "          ?datafile hasco:hasStream <" + stream.getUri() + "> .  " +
                " } " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;
        return DA.findManyByQuery(query);
    }        
    
    public static int findTotalByStream(Stream stream) {
        if (stream == null) {
            return 0;
        }
        String query = NameSpaces.getInstance().printSparqlNameSpaceList() + 
                " SELECT (count(?uri) as ?tot)  " +
                " WHERE {  ?uri hasco:hascoType <" + HASCO.DATA_ACQUISITION + "> . " +
				"          ?uri hasco:hasDataFile ?datafile . " +
                "          ?datafile hasco:hasStream <" + stream.getUri() + "> .  " +
                " }";
        return GenericFind.findTotalByQuery(query);
    }        
    
    public static List<DA> findByStreamTopic(StreamTopic streamTopic, int pageSize, int offset) {
        if (streamTopic == null) {
            return new ArrayList<DA>();
        }
        String query = NameSpaces.getInstance().printSparqlNameSpaceList() + 
                " SELECT ?uri " +
                " WHERE {  ?uri hasco:hascoType <" + HASCO.DATA_ACQUISITION + "> . " +
				"          ?uri hasco:hasDataFile ?datafile . " +
                "          ?datafile hasco:hasStreamTopic <" + streamTopic.getUri() + "> .  " +
                " } " +
                //" ORDER BY ASC(?label) " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;
        return DA.findManyByQuery(query);
    }        
    
    public static int findTotalByStreamTopic(StreamTopic streamTopic) {
        if (streamTopic == null) {
            return 0;
        }
        String query = NameSpaces.getInstance().printSparqlNameSpaceList() + 
                " SELECT (count(?uri) as ?tot)  " +
                " WHERE {  ?uri hasco:hascoType <" + HASCO.DATA_ACQUISITION + "> . " +
				"          ?uri hasco:hasDataFile ?datafile . " +
                "          ?datafile hasco:hasStreamTopic <" + streamTopic.getUri() + "> .  " +
                " }";
        return GenericFind.findTotalByQuery(query);
    }        
    
    public static List<DA> findManyByQuery(String requestedQuery) {
        List<DA> das = new ArrayList<DA>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() + requestedQuery;

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            String uri = null;
            DA da = null;
            if (soln != null && soln.getResource("uri") != null) {
                uri = soln.getResource("uri").getURI();
            }
            if (uri != null) {
                da = DA.find(uri);
            }
            if (da != null) {
                das.add(da);
            }

        }

        if (das != null && !das.contains(null) &&  das.size() > 0) {
            java.util.Collections.sort((List<DA>) das);
        }
        return das;
    }

    public static DA find(String uri) {
            
        if (uri == null || uri.isEmpty()) {
            System.out.println("[ERROR] No valid URI provided to retrieve DA object: " + uri);
            return null;
        }

        DA da = null;
        Statement statement;
        RDFNode object;
        
        String queryString = "DESCRIBE <" + uri + ">";
        Model model = SPARQLUtils.describe(CollectionUtil.getCollectionPath(
                CollectionUtil.Collection.SPARQL_QUERY), queryString);
        
        StmtIterator stmtIterator = model.listStatements();

        if (!stmtIterator.hasNext()) {
            return null;
        } else {
            da = new DA();
        }
        
        while (stmtIterator.hasNext()) {
            statement = stmtIterator.next();
            object = statement.getObject();
            String str = URIUtils.objectRDFToString(object);
            if (uri != null && !uri.isEmpty()) {
                if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
                    da.setLabel(str);
                } else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
                    da.setTypeUri(str); 
                } else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
                    da.setHascoTypeUri(str);
                } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_STATUS)) {
                    da.setHasStatus(str);
                } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_VERSION)) {
                    da.setHasVersion(str);
                } else if (statement.getPredicate().getURI().equals(HASCO.HAS_DATAFILE)) {
                    da.setHasDataFileUri(str);
                } else if (statement.getPredicate().getURI().equals(HASCO.HAS_DD)) {
                    da.setHasDDUri(str);
                } else if (statement.getPredicate().getURI().equals(HASCO.HAS_SDD)) {
                    da.setHasSDDUri(str);
                } else if (statement.getPredicate().getURI().equals(HASCO.IS_MEMBER_OF)) {
                    da.setIsMemberOfUri(str);
                } else if (statement.getPredicate().getURI().equals(HASCO.HAS_NUMBER_DATA_POINTS)) {
                    da.setHasNumberDataPoints(str);
                } else if (statement.getPredicate().getURI().equals(HASCO.HAS_TOTAL_RECORDED_MESSAGES)) {
                    da.setHasTotalRecordedMessages(str);
                } else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
                    da.setComment(str);
                } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
                    da.setHasSIRManagerEmail(str);
                }
            }
        }

        da.setUri(uri);
        
        return da;
    }

    @Override
    public int compareTo(DA another) {
        if (this.getUri() == null || another.getUri() == null) {
            return 0;
        }
        return this.getUri().compareTo(another.getUri());
    }

    private static DA findOneByQuery(String queryString) {
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);
        if (!resultsrw.hasNext()) {
            return null;
        }
        if (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            return find(soln.getResource("uri").getURI());
        }
        return null;
    }

    public static DA findByDataFileUri(String dataFileUri) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
        "SELECT ?uri WHERE { " +
        "  ?uri a ?type . " +
        "  ?type rdfs:subClassOf* hasco:DataAcquisition . " +
        "  ?uri hasco:hasDataFile <" + dataFileUri + "> . " +
        "} LIMIT 1";
    
        return findOneByQuery(queryString);
    }
    
    

}
