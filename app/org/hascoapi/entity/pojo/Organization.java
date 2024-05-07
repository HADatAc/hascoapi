package org.hascoapi.entity.pojo;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFilter;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.vocabularies.RDF;
import org.hascoapi.vocabularies.RDFS;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.VSTOI;
import org.hascoapi.vocabularies.FOAF;

@JsonFilter("organizationFilter")
public class Organization extends Agent {

    public static List<Organization> find() {
        String query =
            " SELECT ?uri WHERE { " +
            " ?uri a foaf:Organization ." +
            "} ";
        return findManyByQuery(query);
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
                "        FILTER (?id=\"" + originalID + "\"^^xsd:string)  . " +
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
                "        FILTER (?email=\"" + email + "\"^^xsd:string)  . " +
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
        Statement statement;
        RDFNode object;
        String queryString;

        if (uri.startsWith("<")) {
            queryString = "DESCRIBE " + uri + " ";
        } else {
            queryString = "DESCRIBE <" + uri + ">";
        }

        Model model = SPARQLUtils.describe(CollectionUtil.getCollectionPath(
                CollectionUtil.Collection.SPARQL_QUERY), queryString);

        organization = new Organization();
        StmtIterator stmtIterator = model.listStatements();

        while (stmtIterator.hasNext()) {
            statement = stmtIterator.next();
            object = statement.getObject();
            String str = URIUtils.objectRDFToString(object);
            if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
                organization.setLabel(str);
            } else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
                organization.setTypeUri(str);
            } else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
                organization.setComment(str);
            } else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
                organization.setHascoTypeUri(str);
            } else if (statement.getPredicate().getURI().equals(FOAF.NAME)) {
                organization.setName(str);
            } else if (statement.getPredicate().getURI().equals(FOAF.FAMILY_NAME)) {
                organization.setFamilyName(str);
            } else if (statement.getPredicate().getURI().equals(FOAF.GIVEN_NAME)) {
                organization.setGivenName(str);
            } else if (statement.getPredicate().getURI().equals(FOAF.MBOX)) {
                organization.setMbox(str);
            } else if (statement.getPredicate().getURI().equals(FOAF.MEMBER)) {
                organization.setMember(str);
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
                organization.setHasSIRManagerEmail(str);
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
