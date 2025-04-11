package org.hascoapi.entity.pojo;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFilter;
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
import org.hascoapi.vocabularies.FOAF;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.RDF;
import org.hascoapi.vocabularies.RDFS;
import org.hascoapi.vocabularies.SCHEMA;
import org.hascoapi.vocabularies.VSTOI;


@JsonFilter("personFilter")
public class Person extends Agent {

    @PropertyField(uri="foaf:familyName")
    protected String familyName;

    @PropertyField(uri="foaf:givenName")
    protected String givenName;

    @PropertyField(uri="schema:jobTitle")
    protected String jobTitle;

    @PropertyField(uri="foaf:member")
    protected String hasAffiliationUri;

    public String getFamilyName() {
        return familyName;
    }
    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getGivenName() {
        return givenName;
    }
    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getJobTitle() {
        return jobTitle;
    }
    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getHasAffiliationUri() {
        return hasAffiliationUri;
    }
    public void setHasAffiliationUri(String hasAffiliationUri) {
        this.hasAffiliationUri = hasAffiliationUri;
    }

    public Organization getHasAffiliation() {
        if (this.getHasAddressUri() == null || this.getHasAffiliationUri().isEmpty()) {
            return null;
        }
        return Organization.find(this.getHasAffiliationUri());
    }

    public static Person findByEmail(String email) {
        if (email == null || email.isEmpty()) {
            return null;
        }
        String query = 
                "SELECT ?uri " +
                " WHERE {  ?subUri rdfs:subClassOf* foaf:Person . " +
                "          ?uri a ?subUri . " +
                "          ?uri foaf:mbox ?email .  " +
                "        FILTER (?email=\"" + email + "\"^^xsd:objecting)  . " +
                " }";
        return findOneByQuery(query);
    }        

    private static Person findOneByQuery(String requestedQuery) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() + requestedQuery;
        
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        String uri = null;
        Person person = null;
        if (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            if (soln != null && soln.get("uri") != null) {
                uri = soln.get("uri").toString();
                person = Person.find(uri);
            }
        }

        return person;
    }

    public static List<Person> find() {
        String query =
            " SELECT ?uri WHERE { " +
            " ?uri a foaf:Person ." +
            "} ";
        return findManyByQuery(query);
    }

    public static List<Person> findManyByQuery(String requestedQuery) {
        List<Person> people = new ArrayList<Person>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() + requestedQuery;

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            String uri = soln.getResource("uri").getURI();
            Person person = Person.find(uri);
            people.add(person);
        }

        java.util.Collections.sort((List<Person>) people);
        return people;
    }

    public static Person find(String uri) {
        Person person = null;

		// Conobjectuct the SELECT query to retrieve named graphs
		String queryString = "SELECT DISTINCT ?graph ?p ?o WHERE { GRAPH ?graph { <" + uri + "> ?p ?o } }";
		ResultSet resultSet = SPARQLUtils.select(CollectionUtil.getCollectionPath(
        	CollectionUtil.Collection.SPARQL_QUERY), queryString);

		if (!resultSet.hasNext()) {
			return null;
		} else {
			person = new Person();
		}

		// Iterate over results
		while (resultSet.hasNext()) {
			QuerySolution qs = resultSet.next();
			
			// Retrieve the named graph URI
			if (qs.contains("graph")) {
				person.setNamedGraph(qs.get("graph").toString());
				//System.out.println("Graph: " + graphURI);
			}
			
			// Retrieve predicate and object (optional)
			if (qs.contains("p") && qs.contains("o")) {
				String predicate = qs.get("p").toString();
				String object = qs.get("o").toString();
				//System.out.println("Predicate: " + predicate + " | Object: " + object);

                if (predicate.equals(RDFS.LABEL)) {
                    person.setLabel(object);
                } else if (predicate.equals(RDF.TYPE)) {
                    person.setTypeUri(object);
                } else if (predicate.equals(RDFS.COMMENT)) {
                    person.setComment(object);
                } else if (predicate.equals(HASCO.HASCO_TYPE)) {
                    person.setHascoTypeUri(object);
                } else if (predicate.equals(HASCO.HAS_IMAGE)) {
                    person.setHasImageUri(object);
                } else if (predicate.equals(HASCO.HAS_WEB_DOCUMENT)) {
                    person.setHasWebDocument(object);
                } else if (predicate.equals(VSTOI.HAS_STATUS)) {
                    person.setHasStatus(object);
                } else if (predicate.equals(SCHEMA.ALTERNATE_NAME)) {
                    person.setHasShortName(object);
                } else if (predicate.equals(FOAF.NAME)) {
                    person.setName(object);
                } else if (predicate.equals(FOAF.FAMILY_NAME)) {
                    person.setFamilyName(object);
                } else if (predicate.equals(FOAF.GIVEN_NAME)) {
                    person.setGivenName(object);
                } else if (predicate.equals(FOAF.MBOX)) {
                    person.setMbox(object);
                } else if (predicate.equals(SCHEMA.TELEPHONE)) {
                    person.setTelephone(object);
                } else if (predicate.equals(FOAF.MEMBER)) {
                    person.setHasAffiliationUri(object);
                } else if (predicate.equals(SCHEMA.ADDRESS)) {
                    person.setHasAddressUri(object);
                } else if (predicate.equals(SCHEMA.JOB_TITLE)) {
                    person.setJobTitle(object);
                } else if (predicate.equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
                    person.setHasSIRManagerEmail(object);
                }
            }
        }

        person.setUri(uri);

        return person;
    }

    @Override
    public void save() {
        saveToTripleStore();
    }

    @Override
    public void delete() {
        deleteFromTripleStore();
    }

}
