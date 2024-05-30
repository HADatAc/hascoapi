package org.hascoapi.entity.pojo;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
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

	@PropertyField(uri="schema:streetAddress")
    protected String hasStreetAddress;

 	@PropertyField(uri="schema:addressLocality")
	private String hasAddressLocality;

	@PropertyField(uri="schema:addressRegion")
	private String hasAddressRegion;

	@PropertyField(uri="schema:postalCode")
	private String hasPostalCode;

	@PropertyField(uri="schema:addressCountry")
	private String hasAddressCountry;

	@PropertyField(uri="vstoi:hasSIRManagerEmail")
	private String hasSIRManagerEmail;

	public String getHasStreetAddress() {
		return hasStreetAddress;
	}
	public void setHasStreetAddress(String hasStreetAddress) {
		this.hasStreetAddress = hasStreetAddress;
	}

	public String getHasAddressLocality() {
		return hasAddressLocality;
	}
	public void setHasAddressLocality(String hasAddressLocality) {
		this.hasAddressLocality = hasAddressLocality;
	}
	public Place getLocality() {
		if (hasAddressLocality == null || hasAddressLocality.isEmpty()) {
			return null;
		}
		return Place.find(hasAddressLocality);
	}

	public String getHasAddressRegion() {
		return hasAddressRegion;
	}
	public void setHasAddressRegion(String hasAddressRegion) {
		this.hasAddressRegion = hasAddressRegion;
	}
	public Place getRegion() {
		if (hasAddressRegion == null || hasAddressRegion.isEmpty()) {
			return null;
		}
		return Place.find(hasAddressRegion);
	}

	public String getHasPostalCode() {
		return hasPostalCode;
	}
	public void setHasPostalCode(String hasPostalCode) {
		this.hasPostalCode = hasPostalCode;
	}

	public String getHasAddressCountry() {
		return hasAddressCountry;
	}
	public void setHasAddressCountry(String hasAddressCountry) {
		this.hasAddressCountry = hasAddressCountry;
	}
	public Place getCountry() {
		if (hasAddressCountry == null || hasAddressCountry.isEmpty()) {
			return null;
		}
		return Place.find(hasAddressCountry);
	}

	public String getHasSIRManagerEmail() {
		return hasSIRManagerEmail;
	}
	public void setHasSIRManagerEmail(String hasSIRManagerEmail) {
		this.hasSIRManagerEmail = hasSIRManagerEmail;
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

    private static List<PostalAddress> findByQuery(String queryString) {
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
				if (postalAddress != null) {
					postalAddresses.add(postalAddress);
				}
			}
        }
        //java.util.Collections.sort((List<Place>) postalAddresss);
        return postalAddresses;
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

	private static String retrieveHASCOTypeUri(String uri) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?type WHERE { " +
                " <" + uri + "> hasco:hascoType ?type ." +
                "} ";
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);
        if (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            return soln.getResource("type").getURI();
        }
		return null;
    }

	public static PostalAddress find(String uri) {
		//System.out.println("PostalAddress.find(): uri = [" + uri + "]");
		PostalAddress postalAddress;
		String hascoTypeUri = retrieveHASCOTypeUri(uri);
		if (hascoTypeUri.equals(SCHEMA.POSTAL_ADDRESS)) {
			postalAddress = new PostalAddress();
		} else {
			return null;
		}

	    Statement statement;
	    RDFNode object;
	    
	    String queryString = "DESCRIBE <" + uri + ">";
	    Model model = SPARQLUtils.describe(CollectionUtil.getCollectionPath(
                CollectionUtil.Collection.SPARQL_QUERY), queryString);
		
		StmtIterator stmtIterator = model.listStatements();

		if (!stmtIterator.hasNext()) {
			return null;
		} 

		while (stmtIterator.hasNext()) {
		    statement = stmtIterator.next();
		    object = statement.getObject();
			String str = URIUtils.objectRDFToString(object);
			if (uri != null && !uri.isEmpty()) {
				if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
					postalAddress.setLabel(str);
				} else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
					postalAddress.setTypeUri(str); 
				} else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
					postalAddress.setComment(str);
				} else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
					postalAddress.setHascoTypeUri(str);
				} else if (statement.getPredicate().getURI().equals(SCHEMA.STREET_ADDRESS)) {
					postalAddress.setHasStreetAddress(str);
				} else if (statement.getPredicate().getURI().equals(SCHEMA.ADDRESS_LOCALITY)) {
					postalAddress.setHasAddressLocality(str);
				} else if (statement.getPredicate().getURI().equals(SCHEMA.ADDRESS_REGION)) {
					postalAddress.setHasAddressRegion(str);
				} else if (statement.getPredicate().getURI().equals(SCHEMA.POSTAL_CODE)) {
					postalAddress.setHasPostalCode(str);
				} else if (statement.getPredicate().getURI().equals(SCHEMA.ADDRESS_COUNTRY)) {
					postalAddress.setHasAddressCountry(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
					postalAddress.setHasSIRManagerEmail(str);
				}
			}
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
