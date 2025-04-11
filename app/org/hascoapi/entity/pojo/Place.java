package org.hascoapi.entity.pojo;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.hascoapi.annotations.PropertyField;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.utils.Utils;
import org.hascoapi.vocabularies.FOAF;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.RDF;
import org.hascoapi.vocabularies.RDFS;
import org.hascoapi.vocabularies.SCHEMA;
import org.hascoapi.vocabularies.VSTOI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hascoapi.Constants.*;


@JsonFilter("placeFilter")
public class Place extends HADatAcThing implements Comparable<Place> {

	private static final Logger log = LoggerFactory.getLogger(Place.class);

    @PropertyField(uri="vstoi:hasStatus")
    private String hasStatus;
    
	@PropertyField(uri="schema:alternaName")
    protected String hasShortName;

	@PropertyField(uri="foaf:name")
    protected String name;

 	@PropertyField(uri="schema:address")
	private String hasAddress;

	@PropertyField(uri="schema:containedInPlace")
	private String containedInPlace;

	@PropertyField(uri="schema:containsPlace")
	private String containsPlace;

	@PropertyField(uri="schema:identifier")
	private String hasIdentifier;

	@PropertyField(uri="schema:geo")
	private String hasGeo;

	@PropertyField(uri="schema:latitude")
	private String hasLatitude;

	@PropertyField(uri="schema:longitude")
	private String hasLongitude;

	@PropertyField(uri="schema:url")
	private String hasUrl;

	@PropertyField(uri="vstoi:hasSIRManagerEmail")
	private String hasSIRManagerEmail;

	public String getHasStatus() {
		return hasStatus;
	}
	
	public void setHasStatus(String hasStatus) {
		this.hasStatus = hasStatus;
	}
	
	public String getHasShortName() {
		return hasShortName;
	}

	public void setHasShortName(String hasShortName) {
		this.hasShortName = hasShortName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getHasAddress() {
		return hasAddress;
	}

	public void setHasAddress(String hasAddress) {
		this.hasAddress = hasAddress;
	}

	public String getContainedInPlace() {
		return containedInPlace;
	}

	public void setContainedInPlace(String containedInPlace) {
		this.containedInPlace = containedInPlace;
	}

	public String getContainsPlace() {
        return containsPlace;
    }

    public void setContainsPlace(String containsPlace) {
        this.containsPlace = containsPlace;
    }

	public String getHasIdentifier() {
		return hasIdentifier;
	}

	public void setHasIdentifier(String hasIdentifier) {
		this.hasIdentifier = hasIdentifier;
	}

	public String getHasGeo() {
		return hasGeo;
	}

	public void setHasGeo(String hasGeo) {
		this.hasGeo = hasGeo;
	}

	public String getHasLatitude() {
		return hasLatitude;
	}

	public void setHasLatitude(String hasLatitude) {
		this.hasLatitude = hasLatitude;
	}

	public String getHasLongitude() {
		return hasLongitude;
	}

	public void setHasLongitude(String hasLongitude) {
		this.hasLongitude = hasLongitude;
	}
   
	public String getHasUrl() {
		return hasUrl;
	}

	public void setHasUrl(String hasUrl) {
		this.hasUrl = hasUrl;
	}
   
	public String getHasSIRManagerEmail() {
		return hasSIRManagerEmail;
	}

	public void setHasSIRManagerEmail(String hasSIRManagerEmail) {
		this.hasSIRManagerEmail = hasSIRManagerEmail;
	}

    public static Place findByOriginalID(String originalID) {
        if (originalID == null || originalID.isEmpty()) {
            return null;
        }
        String query = 
                "SELECT ?uri " +
                " WHERE {  ?subUri rdfs:subClassOf* schema:Place . " +
                "          ?uri a ?subUri . " +
                "          ?uri hasco:hasOriginalId ?id .  " +
                "        FILTER (?id=\"" + originalID + "\"^^xsd:string)  . " +
                " }";
        return findOneByQuery(query);
    }        

    public static Place findByName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        String query = 
                "SELECT ?uri " +
                " WHERE {  ?subUri rdfs:subClassOf* schema:Place . " +
                "          ?uri a ?subUri . " +
                "          ?uri foaf:name ?id .  " +
                "        FILTER (?id=\"" + name + "\"^^xsd:string)  . " +
                " }";
        return findOneByQuery(query);
    }        

    public static Place findSubclassByName(String subclass, String name) {
        if (name == null || name.isEmpty() ||
			subclass == null || subclass.isEmpty()) {
            return null;
        }
        String query = 
                "SELECT ?uri " +
                " WHERE {  ?subUri rdfs:subClassOf* <" + subclass + "> . " +
                "          ?uri a ?subUri . " +
                "          ?uri foaf:name ?id .  " +
                "        FILTER (?id=\"" + name + "\"^^xsd:string)  . " +
                " }";
        return findOneByQuery(query);
    }        

	private static Place findOneByQuery(String requestedQuery) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() + requestedQuery;
        
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        String uri = null;
        Place place = null;
        if (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            if (soln != null && soln.get("uri") != null) {
                uri = soln.get("uri").toString();
                place = Place.find(uri);
            }
        }

        return place;
    }

    public static int findTotalContainsPlace(String uri) {
        if (uri == null || uri.isEmpty()) {
            return 0;
        }
        String query = NameSpaces.getInstance().printSparqlNameSpaceList() + 
                " SELECT (count(?uri) as ?tot)  " +
                " WHERE {  ?uri schema:containedInPlace <" + uri + "> .  " +
                " }";
        return GenericFind.findTotalByQuery(query);
    }        

    public static List<Place> findContainsPlace(String uri, int pageSize, int offset) {
        if (uri == null || uri.isEmpty()) {
            return new ArrayList<Place>();
        }
        String query = 
                "SELECT ?uri " +
                " WHERE {  ?uri schema:containedInPlace <" + uri + "> .  " +
				"          ?uri rdfs:label ?label . " +
                " } " +
                " ORDER BY ASC(?label) " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;
        return findManyByQuery(query);
    }        

	private static List<Place> findManyByQuery(String queryString) {
        String query = NameSpaces.getInstance().printSparqlNameSpaceList() + queryString;

		List<Place> places = new ArrayList<Place>();
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), query);
        if (!resultsrw.hasNext()) {
            return null;
        }
        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
        	Place place = Place.find(soln.getResource("uri").getURI());
            places.add(place);
        }
        //java.util.Collections.sort((List<Place>) places);
        return places;
    }

	@Override
	public boolean equals(Object o) {
		if((o instanceof Container) && (((Container)o).getUri().equals(this.getUri()))) {
			return true;
		} else {
			return false;
		}
	}

	public static Place find(String uri) {
		//System.out.println("Place.find(): uri = [" + uri + "]");
		Place place;

		// SELECT query used to retrieve named graphs
		String queryString = "SELECT DISTINCT ?graph ?p ?o WHERE { GRAPH ?graph { <" + uri + "> ?p ?o } }";
		ResultSet resultSet = SPARQLUtils.select(CollectionUtil.getCollectionPath(
        	CollectionUtil.Collection.SPARQL_QUERY), queryString);

		if (!resultSet.hasNext()) {
			return null;
		} else {
			place = new Place();
		}

		// Iterate over results
		while (resultSet.hasNext()) {
			QuerySolution qs = resultSet.next();
			
			// Retrieve the named graph URI
			if (qs.contains("graph")) {
				place.setNamedGraph(qs.get("graph").toString());
				//System.out.println("Graph: " + graphURI);
			}
			
			// Retrieve predicate and object (optional)
			if (qs.contains("p") && qs.contains("o")) {
				String predicate = qs.get("p").toString();
				String object = qs.get("o").toString();
				//System.out.println("Predicate: " + predicate + " | Object: " + object); 

				if (uri != null && !uri.isEmpty()) {
					if (predicate.equals(RDFS.LABEL)) {
						place.setLabel(object);
					} else if (predicate.equals(RDF.TYPE)) {
						place.setTypeUri(object); 
					} else if (predicate.equals(RDFS.COMMENT)) {
						place.setComment(object);
					} else if (predicate.equals(HASCO.HASCO_TYPE)) {
						place.setHascoTypeUri(object);
					} else if (predicate.equals(HASCO.HAS_IMAGE)) {
						place.setHasImageUri(object);
					} else if (predicate.equals(HASCO.HAS_WEB_DOCUMENT)) {
						place.setHasWebDocument(object);
					} else if (predicate.equals(VSTOI.HAS_STATUS)) {
						place.setHasStatus(object);				
					} else if (predicate.equals(SCHEMA.ALTERNATE_NAME)) {
						place.setHasShortName(object);
					} else if (predicate.equals(FOAF.NAME)) {
						place.setName(object);
					} else if (predicate.equals(HASCO.HAS_IMAGE)) {
						place.setHasImageUri(object);
					} else if (predicate.equals(SCHEMA.ADDRESS)) {
						place.setHasAddress(object);
					} else if (predicate.equals(SCHEMA.CONTAINED_IN_PLACE)) {
						place.setContainedInPlace(object);
					} else if (predicate.equals(SCHEMA.CONTAINS_PLACE)) {
						place.setContainsPlace(object);
					} else if (predicate.equals(SCHEMA.IDENTIFIER)) {
						place.setHasIdentifier(object);
					} else if (predicate.equals(SCHEMA.GEO)) {
						place.setHasGeo(object);
					} else if (predicate.equals(SCHEMA.LATITUDE)) {
						place.setHasLatitude(object);
					} else if (predicate.equals(SCHEMA.LONGITUDE)) {
						place.setHasLongitude(object);
					} else if (predicate.equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
						place.setHasSIRManagerEmail(object);
					}
				}
			}
		}

		if (place.getHascoTypeUri() == null || place.getHascoTypeUri().isEmpty()) { 
			System.out.println("[ERROR] Place.java: URI [" + uri + "] has no HASCO TYPE.");
			return null;
		} else if (!place.getHascoTypeUri().equals(SCHEMA.PLACE)) {
			System.out.println("[ERROR] Place.java: URI [" + uri + "] HASCO TYPE is not " + SCHEMA.PLACE);
			return null;
		}

		place.setUri(uri);
		
		return place;
	}

	@Override
    public int compareTo(Place another) {
        return this.getLabel().compareTo(another.getLabel());
    }

    @Override public void save() {
		saveToTripleStore();
	}

    @Override public void delete() {
		deleteFromTripleStore();
	}

}
