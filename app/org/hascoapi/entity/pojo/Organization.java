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

@JsonFilter("organizationFilter")
public class Organization extends Agent {

    @PropertyField(uri="schema:parentOrganization")
    protected String parentOrganizationUri;

    public String getParentOrganizationUri() {
        return parentOrganizationUri;
    }
    public void setParentOrganizationUri(String parentOrganizationUri) {
        this.parentOrganizationUri = parentOrganizationUri;
    }

    public static List<Organization> find() {
        String query =
            " SELECT ?uri WHERE { " +
            " ?uri a foaf:Organization ." +
            "} ";
        return findManyByQuery(query);
    }

    public static int findTotalSubOrganizations(String uri) {
        if (uri == null || uri.isEmpty()) {
            return 0;
        }
        String query = NameSpaces.getInstance().printSparqlNameSpaceList() + 
                " SELECT (count(?uri) as ?tot)  " +
                " WHERE { " +   
                "    ?uri schema:parentOrganization <" + uri + "> .  " +
                " }";
        return GenericFind.findTotalByQuery(query);
    }        

    public static List<Organization> findSubOrganizations(String uri, int pageSize, int offset) {
        if (uri == null || uri.isEmpty()) {
            return new ArrayList<Organization>();
        }
        String query = 
                "SELECT ?uri " +
                " WHERE {  ?uri schema:parentOrganization <" + uri + ">.  " +
				"          ?uri rdfs:label ?label . " +
                " } " +
                " ORDER BY ASC(?label) " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;
        return findManyByQuery(query);
    }        

    public static int findTotalAffiliations(String uri) {
        if (uri == null || uri.isEmpty()) {
            return 0;
        }
        String query = NameSpaces.getInstance().printSparqlNameSpaceList() + 
                " SELECT (count(?uri) as ?tot)  " +
                " WHERE { " +   
                "    ?uri foaf:member <" + uri + "> .  " +
                " }";
        return GenericFind.findTotalByQuery(query);
    }        

    public static List<Person> findAffiliations(String uri, int pageSize, int offset) {
        if (uri == null || uri.isEmpty()) {
            return new ArrayList<Person>();
        }
        String query = 
                "SELECT ?uri " +
                " WHERE {  ?uri foaf:member <" + uri + ">.  " +
				"          ?uri rdfs:label ?label . " +
                " } " +
                " ORDER BY ASC(?label) " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;
        return Person.findManyByQuery(query);
    }        

    private static List<Organization> findManyByQuery(String requestedQuery) {
        List<Organization> organizations = new ArrayList<Organization>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() + requestedQuery;

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            String uri = soln.getResource("uri").getURI();
            Organization organization = Organization.find(uri);
            organizations.add(organization);
        }

        java.util.Collections.sort((List<Organization>) organizations);
        return organizations;
    }

    public static Organization findByOriginalID(String originalID) {
        if (originalID == null || originalID.isEmpty()) {
            return null;
        }
        String query = 
                "SELECT ?uri " +
                " WHERE {  ?subUri rdfs:subClassOf* foaf:Organization . " +
                "          ?uri a ?subUri . " +
                "          ?uri hasco:hasOriginalId ?id .  " +
                "        FILTER (?id=\"" + originalID + "\"^^xsd:objecting)  . " +
                " }";
        return findOneByQuery(query);
    }        

    public static Organization findByEmail(String email) {
        if (email == null || email.isEmpty()) {
            return null;
        }
        String query = 
                "SELECT ?uri " +
                " WHERE {  ?subUri rdfs:subClassOf* foaf:Organization . " +
                "          ?uri a ?subUri . " +
                "          ?uri foaf:mbox ?email .  " +
                "        FILTER (?email=\"" + email + "\"^^xsd:objecting)  . " +
                " }";
        return findOneByQuery(query);
    }        

    public static Organization findByName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        String query = 
                "SELECT ?uri " +
                " WHERE {  ?subUri rdfs:subClassOf* foaf:Organization . " +
                "          ?uri a ?subUri . " +
                "          ?uri foaf:name ?name .  " +
                "        FILTER (?name=\"" + name + "\"^^xsd:objecting)  . " +
                " }";
        return findOneByQuery(query);
    }        

    private static Organization findOneByQuery(String requestedQuery) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() + requestedQuery;
        
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        String uri = null;
        Organization organization = null;
        if (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            if (soln != null && soln.get("uri") != null) {
                uri = soln.get("uri").toString();
                organization = Organization.find(uri);
            }
        }

        return organization;
    }

    public static Organization find(String uri) {
        Organization organization = null;

		// Conobjectuct the SELECT query to retrieve named graphs
		String queryString = "SELECT DISTINCT ?graph ?p ?o WHERE { GRAPH ?graph { <" + uri + "> ?p ?o } }";
		ResultSet resultSet = SPARQLUtils.select(CollectionUtil.getCollectionPath(
        	CollectionUtil.Collection.SPARQL_QUERY), queryString);

		if (!resultSet.hasNext()) {
			return null;
		} else {
			organization = new Organization();
		}

		// Iterate over results
		while (resultSet.hasNext()) {
			QuerySolution qs = resultSet.next();
			
			// Retrieve the named graph URI
			if (qs.contains("graph")) {
				organization.setNamedGraph(qs.get("graph").toString());
				//System.out.println("Graph: " + graphURI);
			}
			
			// Retrieve predicate and object (optional)
			if (qs.contains("p") && qs.contains("o")) {
				String predicate = qs.get("p").toString();
				String object = qs.get("o").toString();
				//System.out.println("Predicate: " + predicate + " | Object: " + object);
            
                if (predicate.equals(RDFS.LABEL)) {
                    organization.setLabel(object);
                } else if (predicate.equals(RDF.TYPE)) {
                    organization.setTypeUri(object);
                } else if (predicate.equals(RDFS.COMMENT)) {
                    organization.setComment(object);
                } else if (predicate.equals(HASCO.HASCO_TYPE)) {
                    organization.setHascoTypeUri(object);
                } else if (predicate.equals(HASCO.HAS_IMAGE)) {
                    organization.setHasImageUri(object);
                } else if (predicate.equals(HASCO.HAS_WEB_DOCUMENT)) {
                    organization.setHasWebDocument(object);
                } else if (predicate.equals(VSTOI.HAS_STATUS)) {
                    organization.setHasStatus(object);
                } else if (predicate.equals(SCHEMA.ALTERNATE_NAME)) {
                    organization.setHasShortName(object);
                } else if (predicate.equals(FOAF.NAME)) {
                    organization.setName(object);
                } else if (predicate.equals(FOAF.MBOX)) {
                    organization.setMbox(object);
                } else if (predicate.equals(SCHEMA.TELEPHONE)) {
                    organization.setTelephone(object);
                } else if (predicate.equals(SCHEMA.PARENT_ORGANIZATION)) {
                    organization.setParentOrganizationUri(object);
                } else if (predicate.equals(SCHEMA.ADDRESS)) {
                    organization.setHasAddressUri(object);
                } else if (predicate.equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
                    organization.setHasSIRManagerEmail(object);
                }
            }
        }

        organization.setUri(uri);

        return organization;
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
