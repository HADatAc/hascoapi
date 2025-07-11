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

@JsonFilter("postalAddressFilter")
public class PostalAddress extends HADatAcThing implements Comparable<PostalAddress> {

	private static final Logger log = LoggerFactory.getLogger(PostalAddress.class);

	@PropertyField(uri="vstoi:hasStatus")
	private String hasStatus;

	@PropertyField(uri="schema:streetAddress")
    protected String hasStreetAddress;

 	@PropertyField(uri="schema:addressLocality")
	private String hasAddressLocalityUri;

	@PropertyField(uri="schema:addressRegion")
	private String hasAddressRegionUri;

	@PropertyField(uri="schema:postalCode")
	private String hasPostalCode;

	@PropertyField(uri="schema:addressCountry")
	private String hasAddressCountryUri;

	@PropertyField(uri="vstoi:hasSIRManagerEmail")
	private String hasSIRManagerEmail;

    public String getHasStatus() {
        return hasStatus;
    }

    public void setHasStatus(String hasStatus) {
        this.hasStatus = hasStatus;
    }

	public String getHasStreetAddress() {
		return hasStreetAddress;
	}
	public void setHasStreetAddress(String hasStreetAddress) {
		this.hasStreetAddress = hasStreetAddress;
	}

	public String getHasAddressLocalityUri() {
		return hasAddressLocalityUri;
	}
	public void setHasAddressLocalityUri(String hasAddressLocalityUri) {
		this.hasAddressLocalityUri = hasAddressLocalityUri;
	}
	public Place getHasAddressLocality() {
		if (hasAddressLocalityUri == null || hasAddressLocalityUri.isEmpty()) {
			return null;
		}
		return Place.find(hasAddressLocalityUri);
	}

	public String getHasAddressRegionUri() {
		return hasAddressRegionUri;
	}
	public void setHasAddressRegionUri(String hasAddressRegionUri) {
		this.hasAddressRegionUri = hasAddressRegionUri;
	}
	public Place getHasAddressRegion() {
		if (hasAddressRegionUri == null || hasAddressRegionUri.isEmpty()) {
			return null;
		}
		return Place.find(hasAddressRegionUri);
	}

	public String getHasPostalCode() {
		return hasPostalCode;
	}
	public void setHasPostalCode(String hasPostalCode) {
		this.hasPostalCode = hasPostalCode;
	}

	public String getHasAddressCountryUri() {
		return hasAddressCountryUri;
	}
	public void setHasAddressCountryUri(String hasAddressCountryUri) {
		this.hasAddressCountryUri = hasAddressCountryUri;
	}
	public Place getHasAddressCountry() {
		if (hasAddressCountryUri == null || hasAddressCountryUri.isEmpty()) {
			return null;
		}
		return Place.find(hasAddressCountryUri);
	}

	public String getHasSIRManagerEmail() {
		return hasSIRManagerEmail;
	}
	public void setHasSIRManagerEmail(String hasSIRManagerEmail) {
		this.hasSIRManagerEmail = hasSIRManagerEmail;
	}

    public static PostalAddress findByPostalCode(String postalCode) {
        if (postalCode == null || postalCode.isEmpty()) {
            return null;
        }
        String query = 
                " SELECT ?uri " +
                " WHERE {  ?subUri rdfs:subClassOf* schema:PostalAddress . " +
                "          ?uri a ?subUri . " +
                "          ?uri schema:postalCode ?postalCode .  " +
                "        FILTER (?postalCode=\"" + postalCode + "\"^^xsd:string)  . " +
                " }";
        return findOneByQuery(query);
	}

    public static PostalAddress findByAddress(String street, String postalCode) {
        if (street == null || street.isEmpty() ||
		    postalCode == null || postalCode.isEmpty()) {
            return null;
        }
        String query = 
                "SELECT ?uri " +
                " WHERE {  ?subUri rdfs:subClassOf* schema:PostalAddress . " +
                "          ?uri a ?subUri . " +
                "          ?uri schema:streetAddress ?street .  " +
                "          ?uri schema:postalCode ?postalCode .  " +
                "        FILTER (?street=\"" + street + "\"^^xsd:string)  . " +
                "        FILTER (?postalCode=\"" + postalCode + "\"^^xsd:string)  . " +
                " }";
        return findOneByQuery(query);
    }        

    public static int findTotalContainsPostalAddress(String placeuri) {
        if (placeuri == null || placeuri.isEmpty()) {
            return 0;
        }
		Place place = Place.find(placeuri);
		if (place == null) {
			return 0;
		}
		String searchPredicate = null;
		if (place.getTypeUri().equals(SCHEMA.CITY)) {
			searchPredicate = "schema:addressLocality";
		} else if (place.getTypeUri().equals(SCHEMA.STATE)) {
			searchPredicate = "schema:addressRegion";
		} else if (place.getTypeUri().equals(SCHEMA.COUNTRY)) {
			searchPredicate = "schema:addressCountry";
		}
		if (searchPredicate == null) {
			return 0;
		}
		String query = NameSpaces.getInstance().printSparqlNameSpaceList() + 
			" SELECT (count(DISTINCT ?uri) as ?tot)  " +
			" WHERE {  ?subUri rdfs:subClassOf* schema:PostalAddress . " +
			"          ?uri a ?subUri . " +
			"          ?uri " + searchPredicate + " <" + placeuri + "> . " +
			" } ";
		return GenericFind.findTotalByQuery(query);
    }        

    public static List<PostalAddress> findContainsPostalAddress(String placeuri, int pageSize, int offset) {
        if (placeuri == null || placeuri.isEmpty()) {
            return new ArrayList<PostalAddress>();
        }
		Place place = Place.find(placeuri);
		if (place == null) {
			return new ArrayList<PostalAddress>();
		}
		String searchPredicate = null;
		if (place.getTypeUri().equals(SCHEMA.CITY)) {
			searchPredicate = "schema:addressLocality";
		} else if (place.getTypeUri().equals(SCHEMA.STATE)) {
			searchPredicate = "schema:addressRegion";
		} else if (place.getTypeUri().equals(SCHEMA.COUNTRY)) {
			searchPredicate = "schema:addressCountry";
		}
		if (searchPredicate == null) {
			return new ArrayList<PostalAddress>();
		}
		String query = NameSpaces.getInstance().printSparqlNameSpaceList() + 
				"SELECT DISTINCT ?uri " +
				" WHERE {  ?subUri rdfs:subClassOf* schema:PostalAddress . " +
				"          ?uri a ?subUri . " +
				"          ?uri " + searchPredicate + " <" + placeuri + "> . " +
				"          ?uri rdfs:label ?label . " +
				" } " +
                " ORDER BY ASC(?label) " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;
        return findManyByQuery(query);
    }        

    public static int findTotalContainsElement(String placeuri, String elementtype) {
        if (placeuri == null || placeuri.isEmpty() || elementtype == null || elementtype.isEmpty()) {
            return 0;
        }
		Place place = Place.find(placeuri);
		if (place == null) {
			return 0;
		}
		String elementTypeUri = null;
		if (elementtype.equals("person")) {
			elementTypeUri = SCHEMA.PERSON;
		} else if (elementtype.equals("organization")) {
			elementTypeUri = SCHEMA.ORGANIZATION;
		}
		if (elementTypeUri == null) {
			return 0;
		}
		String searchPredicate = null;
		if (place.getTypeUri().equals(SCHEMA.CITY)) {
			searchPredicate = "schema:addressLocality";
		} else if (place.getTypeUri().equals(SCHEMA.STATE)) {
			searchPredicate = "schema:addressRegion";
		} else if (place.getTypeUri().equals(SCHEMA.COUNTRY)) {
			searchPredicate = "schema:addressCountry";
		}
		if (searchPredicate == null) {
			return 0;
		}
		String query = NameSpaces.getInstance().printSparqlNameSpaceList() + 
			" SELECT (count(?uri) as ?tot)  " +
			" WHERE {  ?subUri rdfs:subClassOf* <" + elementTypeUri + "> . " +
			"          ?uri a ?subUri . " +
			"          ?uri schema:address ?addressuri . " +
			"          ?addressuri " + searchPredicate + " <" + placeuri + "> . " +
			" } ";
		return GenericFind.findTotalByQuery(query);
    }        

    public static <T> List<T> findContainsElement(String placeuri, String elementtype, int pageSize, int offset) {
        if (placeuri == null || placeuri.isEmpty() || elementtype == null || elementtype.isEmpty()) {
            return new ArrayList<T>();
        }
		Place place = Place.find(placeuri);
		if (place == null) {
			return new ArrayList<T>();
		}
		String elementTypeUri = null;
		if (elementtype.equals("person")) {
			elementTypeUri = SCHEMA.PERSON;
		} else if (elementtype.equals("organization")) {
			elementTypeUri = SCHEMA.ORGANIZATION;
		}
		if (elementTypeUri == null) {
			return new ArrayList<T>();
		}
		String searchPredicate = null;
		if (place.getTypeUri().equals(SCHEMA.CITY)) {
			searchPredicate = "schema:addressLocality";
		} else if (place.getTypeUri().equals(SCHEMA.STATE)) {
			searchPredicate = "schema:addressRegion";
		} else if (place.getTypeUri().equals(SCHEMA.COUNTRY)) {
			searchPredicate = "schema:addressCountry";
		}
		if (searchPredicate == null) {
			return new ArrayList<T>();
		}
		String query = NameSpaces.getInstance().printSparqlNameSpaceList() + 
				"SELECT ?uri " +
				" WHERE {  ?subUri rdfs:subClassOf* <" + elementTypeUri + "> . " +
				"          ?uri a ?subUri . " +
				"          ?uri rdfs:label ?label . " +
				"          ?uri schema:address ?addressuri . " +
				"          ?addressuri " + searchPredicate + " <" + placeuri + "> . " +
				" } " +
                " ORDER BY ASC(?label) " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;
		if (elementTypeUri.equals(SCHEMA.PERSON)) {
        	return (List<T>)PostalAddress.findManyElementsByQuery(query, Person.class);
		} else if (elementTypeUri.equals(SCHEMA.ORGANIZATION)) {
        	return (List<T>)PostalAddress.<Organization>findManyElementsByQuery(query, Organization.class);
		}
		return new ArrayList<T>();
    }        

	private static PostalAddress findOneByQuery(String requestedQuery) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() + requestedQuery;
        
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        String uri = null;
        PostalAddress postalAddress = null;
        if (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            if (soln != null && soln.get("uri") != null) {
                uri = soln.get("uri").toString();
				if (uri != null) {
                	postalAddress = PostalAddress.find(uri);
				}
            }
        }

        return postalAddress;
    }

    public static List<PostalAddress> findManyByQuery(String queryString) {
        List<PostalAddress> postalAddresses = new ArrayList<PostalAddress>();
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);
        if (!resultsrw.hasNext()) {
            return null;
        }
        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
			if (soln != null && soln.getResource("uri") != null) {
				PostalAddress postalAddress = PostalAddress.find(soln.getResource("uri").getURI());
				if (postalAddress != null && !postalAddresses.contains(postalAddress)) {
					postalAddresses.add(postalAddress);
				}
			}
        }
        //java.util.Collections.sort((List<Place>) postalAddresss);
        return postalAddresses;
    }

    public static <T> List<T> findManyElementsByQuery(String queryString,  Class<T> clazz) {
        List<T> elements = new ArrayList<T>();
		ResultSetRewindable resultsrw = SPARQLUtils.select(
            CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);
        if (!resultsrw.hasNext()) {
            return null;
        }
        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
			if (soln != null && soln.getResource("uri") != null) {
				if (clazz.equals(Person.class)) {
					T element = (T)Person.find(soln.getResource("uri").getURI());
					if (element != null) {
						elements.add(element);
					}
				} else if (clazz.equals(Organization.class)) {
					T element = (T)Organization.find(soln.getResource("uri").getURI());
					if (element != null) {
						elements.add(element);
					}
				} 
			}
        }
        //java.util.Collections.sort((List<Place>) postalAddresss);
        return elements;
    }

	@Override
	public boolean equals(Object o) {
		if((o instanceof PostalAddress) && 
		   (((PostalAddress)o).getHasStreetAddress().equals(this.getHasStreetAddress())) && 
		   (((PostalAddress)o).getHasPostalCode().equals(this.getHasPostalCode()))) {
			return true;
		} else {
			return false;
		}
	}

	public static PostalAddress find(String uri) {
		PostalAddress postalAddress;

		// Conobjectuct the SELECT query to retrieve named graphs
		String queryString = "SELECT DISTINCT ?graph ?p ?o WHERE { GRAPH ?graph { <" + uri + "> ?p ?o } }";
		ResultSet resultSet = SPARQLUtils.select(CollectionUtil.getCollectionPath(
        	CollectionUtil.Collection.SPARQL_QUERY), queryString);

		if (!resultSet.hasNext()) {
			return null;
		} else {
			postalAddress = new PostalAddress();
		}

		// Iterate over results
		while (resultSet.hasNext()) {
			QuerySolution qs = resultSet.next();
			
			// Retrieve the named graph URI
			if (qs.contains("graph")) {
				postalAddress.setNamedGraph(qs.get("graph").toString());
				//System.out.println("Graph: " + graphURI);
			}
			
			// Retrieve predicate and object (optional)
			if (qs.contains("p") && qs.contains("o")) {
				String predicate = qs.get("p").toString();
				String object = qs.get("o").toString();
				//System.out.println("Predicate: " + predicate + " | Object: " + object); 

				if (predicate.equals(RDFS.LABEL)) {
					postalAddress.setLabel(object);
				} else if (predicate.equals(RDF.TYPE)) {
					postalAddress.setTypeUri(object); 
				} else if (predicate.equals(RDFS.COMMENT)) {
					postalAddress.setComment(object);
				} else if (predicate.equals(HASCO.HASCO_TYPE)) {
					postalAddress.setHascoTypeUri(object);
				} else if (predicate.equals(HASCO.HAS_IMAGE)) {
					postalAddress.setHasImageUri(object);
				} else if (predicate.equals(HASCO.HAS_WEB_DOCUMENT)) {
					postalAddress.setHasWebDocument(object);
				} else if (predicate.equals(VSTOI.HAS_STATUS)) {
					postalAddress.setHasStatus(object);
				} else if (predicate.equals(SCHEMA.STREET_ADDRESS)) {
					postalAddress.setHasStreetAddress(object);
				} else if (predicate.equals(SCHEMA.ADDRESS_LOCALITY)) {
					postalAddress.setHasAddressLocalityUri(object);
				} else if (predicate.equals(SCHEMA.ADDRESS_REGION)) {
					postalAddress.setHasAddressRegionUri(object);
				} else if (predicate.equals(SCHEMA.POSTAL_CODE)) {
					postalAddress.setHasPostalCode(object);
				} else if (predicate.equals(SCHEMA.ADDRESS_COUNTRY)) {
					postalAddress.setHasAddressCountryUri(object);
				} else if (predicate.equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
					postalAddress.setHasSIRManagerEmail(object);
				}
			}
		}

		if (postalAddress.getHascoTypeUri() == null || postalAddress.getHascoTypeUri().isEmpty()) { 
			System.out.println("[ERROR] Place.java: URI [" + uri + "] has no HASCO TYPE.");
			return null;
		} else if (!postalAddress.getHascoTypeUri().equals(SCHEMA.POSTAL_ADDRESS)) {
			System.out.println("[ERROR] Place.java: URI [" + uri + "] HASCO TYPE is not " + SCHEMA.POSTAL_ADDRESS);
			return null;
		}

		postalAddress.setUri(uri);
		
		return postalAddress;
	}

	@Override
    public int compareTo(PostalAddress another) {
		String thisString = this.getHasStreetAddress() + this.getHasPostalCode();
		String anotherString = another.getHasStreetAddress() + another.getHasPostalCode();
        return thisString.compareTo(anotherString);
    }

    @Override public void save() {
		saveToTripleStore();
	}

    @Override public void delete() {
		deleteFromTripleStore();
	}

}
