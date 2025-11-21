package org.hascoapi.entity.pojo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonFilter;
import org.apache.commons.text.WordUtils;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.engine.http.QueryExceptionHTTP;
import org.hascoapi.annotations.PropertyField;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.FirstLabel;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.RDF;
import org.hascoapi.vocabularies.RDFS;
import org.hascoapi.vocabularies.SIO;
import org.hascoapi.vocabularies.VSTOI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonFilter("platformFilter")
public class Platform extends HADatAcClass implements Comparable<Platform> {

    private static final Logger log = LoggerFactory.getLogger(Platform.class);
    public static String LAT = SIO.LATITUDE;
	public static String LONG = SIO.LONGITUDE;
	
    private String location;


    @PropertyField(uri="hasco:hasVersion")
    private String hasVersion;

    @PropertyField(uri="vstoi:hasSIRManagerEmail")
    private String hasSIRManagerEmail;

    public Platform(String uri,
            String typeUri,
            String label,
            String comment) {
        this.uri = uri;
        this.typeUri = typeUri;
        this.label = label;
        this.comment = comment;
    }

    public Platform() {
        this.uri = "";
        this.typeUri = "";
        this.label = "";
        this.comment = "";
        this.location = "";
    }

    public String getLocation() {
        return location;
    }
    public void setLocation(String location) {
        this.location = location;
    }



    public String getHasVersion() {
        return this.hasVersion;
    }
    public void setHasVersion(String hasVersion) {
        this.hasVersion = hasVersion;
    }

    public String getHasSIRManagerEmail() {
        return this.hasSIRManagerEmail;
    }
    public void setHasSIRManagerEmail(String hasSIRManagerEmail) {
        this.hasSIRManagerEmail = hasSIRManagerEmail;
    }

    public List<Platform> getImmediateSubPlatforms() {
        List<Platform> subPlatforms = new ArrayList<Platform>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?uri hasco:partOf <" + uri + "> . " + 
                "} ";

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            Platform platform = find(soln.getResource("uri").getURI());
            subPlatforms.add(platform);
        }			

        if (subPlatforms.size() > 1) {
        	java.util.Collections.sort((List<Platform>) subPlatforms);
        }
        
        return subPlatforms;
    }

    
    public String getTypeLabel() {
    	PlatformType pltType = PlatformType.find(getTypeUri());
    	if (pltType == null || pltType.getLabel() == null) {
    		return "";
    	}
    	return pltType.getLabel();
    }

    
    @Override
    public boolean equals(Object o) {
        if((o instanceof Platform) && (((Platform)o).getUri().equals(this.getUri()))) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return getUri().hashCode();
    }
    
    public static Platform find(String uri) {
 
    	//System.out.println("Platform.find <" + uri + ">");
    	
    	Platform platform = null;
        Statement statement;
        RDFNode object;

        String queryString = "DESCRIBE <" + uri + ">";
        Model model = SPARQLUtils.describe(CollectionUtil.getCollectionPath(
                CollectionUtil.Collection.SPARQL_QUERY), queryString);

        platform = new Platform();
        StmtIterator stmtIterator = model.listStatements();

        while (stmtIterator.hasNext()) {
            statement = stmtIterator.next();
            object = statement.getObject();
            String str = URIUtils.objectRDFToString(object);
            if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
                platform.setLabel(str);
            } else if (statement.getPredicate().getURI().equals(RDFS.SUBCLASS_OF)) {
                platform.setSuperUri(str);
            } else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
                platform.setHascoTypeUri(str);
            } else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
                platform.setComment(str);
            } else if (statement.getPredicate().getURI().equals(HASCO.HAS_IMAGE)) {
                platform.setHasImageUri(str);
            } else if (statement.getPredicate().getURI().equals(HASCO.HAS_WEB_DOCUMENT)) {
                platform.setHasWebDocument(str);
            } else if (statement.getPredicate().getURI().equals(HASCO.HAS_VERSION)) {
                platform.setHasVersion(str);
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
                platform.setHasSIRManagerEmail(str);
            }
        }

        platform.setUri(uri);

    	//System.out.println("AFTER Platform.find <" + platform + ">");

        return platform;
    }

    public static int getNumberPlatforms() {
        String query = "";
        query += NameSpaces.getInstance().printSparqlNameSpaceList();
        query += " select (count(?uri) as ?tot) where { " + 
                " ?uri hasco:hascoType <" + VSTOI.PLATFORM + "> . " +
                //" ?platModel rdfs:subClassOf* vstoi:Platform . " + 
                //" ?uri a ?platModel ." + 
                "}";

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

    public static List<Platform> findWithPages(int pageSize, int offset) {
        List<Platform> platforms = new ArrayList<Platform>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() + 
        		"SELECT ?uri WHERE { " + 
                " ?uri hasco:hascoType <" + VSTOI.PLATFORM + "> . " +
                //" ?platModel rdfs:subClassOf* vstoi:Platform . " + 
                //" ?uri a ?platModel . } " + 
                " LIMIT " + pageSize + 
                " OFFSET " + offset;

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            if (soln != null && soln.getResource("uri").getURI() != null) {
                Platform platform = Platform.find(soln.getResource("uri").getURI());
                platforms.add(platform);
            }
        }
        return platforms;
    }

    public static List<Platform> find() {
        List<Platform> platforms = new ArrayList<Platform>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?uri hasco:hascoType <" + VSTOI.PLATFORM + "> . " +
                //" ?platModel rdfs:subClassOf* vstoi:Platform . " + 
                //" ?uri a ?platModel ." + 
                "} ";

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            Platform platform = find(soln.getResource("uri").getURI());
            platforms.add(platform);
        }			

        java.util.Collections.sort((List<Platform>) platforms);

        return platforms;
    }

    public static List<Platform> findWithGeoReferenceAndDeployment() {
        List<Platform> platforms = new ArrayList<Platform>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                //" ?platModel rdfs:subClassOf* vstoi:Platform . " + 
                //" ?uri a ?platModel ." +
                " ?uri hasco:hascoType <" + VSTOI.PLATFORM + "> . " +
                " ?uri hasco:hasFirstCoordinate ?lat . " +
                " ?uri hasco:hasSecondCoordinate ?lon . " +
                " ?uri hasco:hasFirstCoordinateCharacteristic <" + LAT + "> . " +
                " ?uri hasco:hasSecondCoordinateCharacteristic <" + LONG + "> . " +
                "} ";

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            Platform platform = find(soln.getResource("uri").getURI());
            platforms.add(platform);
        }			

        java.util.Collections.sort((List<Platform>) platforms);

        return platforms;
    }

    @Override
    public int compareTo(Platform another) {
        return this.getLabel().compareTo(another.getLabel());
    }

    @Override
    public void save() {
        System.out.println("Saving platform [" + uri + "]");
        saveToTripleStore();
    }

    @Override
    public void delete() {
        deleteFromTripleStore();
    }


}
